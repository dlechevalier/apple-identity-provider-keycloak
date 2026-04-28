# Security & Code Quality Analysis — Apple Identity Provider SPI

> Analysis performed on version `2.0.0` (fork of klausbetz/apple-identity-provider-keycloak `1.17.0`) targeting Keycloak `26.6.1`, Java 21.
> Last updated: 2026-04-28. Items marked **✅ Fixed** have been resolved in the current codebase.

---

## Table of Contents

1. [Security Vulnerabilities](#1-security-vulnerabilities)
2. [Logic Bug](#2-logic-bug)
3. [Code Quality](#3-code-quality)
4. [Dependencies](#4-dependencies)
5. [Summary Table](#5-summary-table)

---

## 1. Security Vulnerabilities

### 🔴 Critical — Account Takeover via Email-Based Auto-Linking — ⚠️ Partially mitigated

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 107–128

```java
UserModel existing = this.session.users().getUserByEmail(realm, email);
// ...
FederatedIdentityModel newLink = new FederatedIdentityModel(providerAlias, context.getId(), context.getUsername());
this.session.users().addFederatedIdentity(realm, existing, newLink);
```

`autoLinkIfPossible` automatically links an existing Keycloak account to an Apple account based solely on email matching. An attacker who controls an Apple account registered with a victim's email address (or via Apple's Private Email Relay) can silently take over the corresponding Keycloak account — **without knowing the victim's password**.

This is a well-known OAuth attack pattern known as the "pre-authentication account linking attack". See CVE-2023-39522 and CVE-2022-31074 for similar vulnerabilities in other OAuth providers.

**Mitigation applied:** `tokenExchangeAccountLinkingEnabled` now **defaults to `false`** in `AppleIdentityProviderFactory.java`. The feature remains available but must be explicitly enabled by the operator. See [SECURITY.md](SECURITY.md) for deployment guidance.

---

### ~~🔴 Critical~~ — Log Injection via Unsanitized User-Controlled Input — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProviderEndpoint.java`

The `error` `@FormParam` and the Apple response body were concatenated into log messages without sanitization (CWE-117 — Improper Output Neutralization for Logs).

**Fix:** A `sanitizeForLog()` helper (replaces `\r`, `\n`, `\t` with `_`) is now applied to all user-controlled and external inputs before logging, in both `AppleIdentityProviderEndpoint` and `AppleIdentityProvider`.

---

### ~~🟠 High~~ — Race Condition (TOCTOU) on Client Secret Generation — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

The check-then-act sequence in `prepareClientSecret` was not synchronized. Under concurrent load, two threads could both observe the secret as expired and generate a new JWS, with one overwriting the other.

**Fix:** `prepareClientSecret` is now declared `synchronized`.

---

### ~~🟠 High~~ — Silent Key Generation Failure Propagates Null as Client Secret — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

`generateJWS` previously returned `null` on any error, propagating `null` silently as the client secret and producing a cryptic Apple-side failure.

**Fix:** `generateJWS` now throws `IdentityBrokerException("Unable to generate Apple client secret JWT")` on failure, surfacing the error immediately on the server side.

---

### 🟠 High — Fragile Manual PEM Parsing; Private Key Material in Non-Zeroable String — ⚠️ Partially fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

**Partial fix applied:** PEM header/footer stripping now uses `replaceAll("-----[^-]+-----", "")` and `replaceAll("\\s+", "")`, which correctly handles CRLF line endings, bare newlines, and all whitespace variants.

**Still present:** The private key material is manipulated as an immutable Java `String` and cannot be explicitly zeroed from memory after use (CWE-312 — Cleartext Storage of Sensitive Information). A proper PEM parser (e.g., from Bouncy Castle) with a `char[]`/`byte[]` key representation would be the complete fix.

---

### ~~🟡 Medium~~ — Missing Redirect URI Validation — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

`appRedirectUri` from the token exchange request was used without validation, potentially enabling open redirects.

**Fix:** `validateRedirectUri()` now rejects non-absolute URIs and URIs with non-HTTP/HTTPS schemes (`ErrorResponseException` with 400 Bad Request).

---

### ~~🟡 Medium~~ — Internal Exception Details Exposed in Event — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

Raw Java exception messages were included in Keycloak event details, potentially leaking internal implementation information via Admin APIs.

**Fix:** Event details now use fixed generic strings (`"token validation failure"`, `"Failed to extract identity from token"`) with no exception message interpolation.

---

## 2. Logic Bug

### ~~Email Extraction Nested Inside `nameNode != null` Block~~ — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

Email extraction was nested inside the `nameNode != null` block, silently ignoring the email when Apple sends `{"email": "foo@bar.com"}` without a `name` field.

**Fix:** `profile.get("email")` is now called unconditionally outside the `nameNode` block. The test `withEmailButNoName_emailIsParsed()` verifies this behaviour.

---

## 3. Code Quality

### ~~Exception Swallowing in `handleUserJson`~~ — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

All exceptions from `handleUserJson` were silently discarded.

**Fix:** The catch block now calls `logger.debug("Failed to handle user JSON from Apple", e)`, making failures visible in debug logs without disrupting the authentication flow.

---

### Config Mutation in Constructor

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 54–60

```java
config.setAuthorizationUrl(AUTH_URL);
config.setTokenUrl(TOKEN_URL);
// ...
```

The constructor mutates the shared config object with hardcoded values, silently overwriting any existing configuration. This is an unexpected side effect in a constructor and makes the class difficult to test without heavy mocking.

---

### ~~Magic Number for Token Expiry~~ — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

**Fix:** Replaced with the named constant `CLIENT_SECRET_EXPIRY_SECONDS = 86400L * 180` (180 days).

---

### ~~Manually URL-Encoded Scopes~~ — ✅ Fixed

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`

**Fix:** `getDefaultScopes()` now returns `"name email"` (unencoded), delegating percent-encoding to the framework and preventing double-encoding (`name%2520email`).

---

### Null as Error Sentinel

`sendTokenRequest` and `exchangeAuthorizationCode` return `null` to signal failure. This is a Java anti-pattern: callers must remember to null-check the return value. `Optional<BrokeredIdentityContext>` or a dedicated exception would be more explicit and safer.

---

### Poor Testability — Reflection Used to Access Private Methods

Tests use reflection extensively to access private methods (`parseUser`, `handleUserJson`, `isValidSecret`, `generateClientToken`, `autoLinkIfPossible`). This is a signal that the class is too monolithic: either these methods contain enough logic to warrant package-private visibility, or the class should be decomposed into smaller, individually testable units.

---

## 4. Dependencies

| Dependency | Current Version | Notes |
|---|---|---|
| `junit-jupiter` | `5.9.0` (Aug 2022) | 5.11.x available; update recommended |
| `resteasy-core` | `6.2.4.Final` | CVE-2023-0482 fixed in 6.2.3 — version here is safe |
| `mockito-core` | `5.11.0` | Up to date |
| `keycloak-*` | `26.5.0` | Recent |

---

## 5. Summary Table

| Severity | Issue | Status |
|---|---|---|
| 🔴 Critical | Account takeover via email-based auto-linking | ⚠️ Mitigated — disabled by default, opt-in |
| 🔴 Critical | Log injection via unsanitized `@FormParam error` | ✅ Fixed |
| 🟠 High | TOCTOU race condition on `prepareClientSecret` | ✅ Fixed |
| 🟠 High | Silent null propagated as client secret | ✅ Fixed |
| 🟠 High | Fragile PEM parsing; private key in non-zeroable String | ⚠️ Partially fixed (whitespace handling fixed; String in memory remains) |
| 🟡 Medium | Missing redirect URI validation | ✅ Fixed |
| 🟡 Medium | Internal exception message exposed in event | ✅ Fixed |
| 🐛 Bug | Email ignored when `name` node is absent | ✅ Fixed |
| 🟢 Low | Exception swallowing in `handleUserJson` | ✅ Fixed |
| 🟢 Low | Magic number `86400 * 180` | ✅ Fixed |
| 🟢 Low | Manually URL-encoded scopes | ✅ Fixed |
| 🟢 Low | Config mutation in constructor | Open — design issue |
| 🟢 Low | Null used as error sentinel in `sendTokenRequest` | Open — design issue |
| 🟢 Low | Reflection in tests signals poor testability | Open — tests added, decomposition deferred |
