# Security & Code Quality Analysis — Apple Identity Provider SPI

> Analysis performed on version `1.17.0` targeting Keycloak `26.5.0`, Java 21.

---

## Table of Contents

1. [Security Vulnerabilities](#1-security-vulnerabilities)
2. [Logic Bug](#2-logic-bug)
3. [Code Quality](#3-code-quality)
4. [Dependencies](#4-dependencies)
5. [Summary Table](#5-summary-table)

---

## 1. Security Vulnerabilities

### 🔴 Critical — Account Takeover via Email-Based Auto-Linking

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 107–128

```java
UserModel existing = this.session.users().getUserByEmail(realm, email);
// ...
FederatedIdentityModel newLink = new FederatedIdentityModel(providerAlias, context.getId(), context.getUsername());
this.session.users().addFederatedIdentity(realm, existing, newLink);
```

`autoLinkIfPossible` automatically links an existing Keycloak account to an Apple account based solely on email matching. An attacker who controls an Apple account registered with a victim's email address (or via Apple's Private Email Relay) can silently take over the corresponding Keycloak account — **without knowing the victim's password**.

This is a well-known OAuth attack pattern known as the "pre-authentication account linking attack". See CVE-2023-39522 and CVE-2022-31074 for similar vulnerabilities in other OAuth providers.

**Aggravating factor:** `tokenExchangeAccountLinkingEnabled` defaults to `true` in `AppleIdentityProviderFactory.java`.

---

### 🔴 Critical — Log Injection via Unsanitized User-Controlled Input

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProviderEndpoint.java`, line 68

```java
logger.warn(error + " for broker login " + appleIdentityProvider.getConfig().getProviderId());
```

The `error` parameter is a `@FormParam` fully controlled by the user, and is directly concatenated into the log message without any sanitisation. An attacker can inject newline characters to forge log entries (CWE-117 — Improper Output Neutralization for Logs).

The same issue exists in `AppleIdentityProvider.java` line 190, where the raw Apple response body is logged:

```java
logger.warn("Error response from apple: status=" + response.getStatus() + ", body=" + response.asString() + " ...");
```

---

### 🟠 High — Race Condition (TOCTOU) on Client Secret Generation

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 175–183

```java
public void prepareClientSecret(String clientId) {
    if (!isValidSecret(getConfig().getClientSecret())) {
        getConfig().setClientSecret(generateJWS(...));
    }
}
```

There is no synchronization around the check-then-act sequence. Under concurrent load, two threads may both observe the secret as expired, both generate a new JWS, and one will overwrite the other. In the best case one request fails; in the worst case an inconsistent secret is persisted.

---

### 🟠 High — Silent Key Generation Failure Propagates Null as Client Secret

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 228–252

```java
} catch (Exception e) {
    logger.error("Unable to generate JWS", e);
}
return null; // ← null returned without throwing
```

`generateJWS` returns `null` on any error. `prepareClientSecret` then calls `getConfig().setClientSecret(null)`. In `generateTokenRequest` (line 225):

```java
.param(OAUTH2_PARAMETER_CLIENT_SECRET, clientSecret.get().orElse(getConfig().getClientSecret()))
```

If the vault also fails to return a value, the token request to Apple is sent with `client_secret=null`, resulting in a cryptic Apple-side error instead of a clear server-side failure.

---

### 🟠 High — Fragile Manual PEM Parsing; Private Key Material in Non-Zeroable String

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 232–238

```java
PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(
    p8Content
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\n", "")
        .replace(" ", "")
));
```

Several issues with this approach:

- Does not handle `\r\n` line endings (Windows-style PEM files).
- Literal `\n` characters (without a preceding backslash) are not stripped.
- A partially attacker-controlled config value could inject malformed content and trigger a swallowed exception.
- The private key is manipulated as an immutable Java `String` — it cannot be explicitly zeroed from memory after use (CWE-312 — Cleartext Storage of Sensitive Information).

A proper PEM parser (e.g., from Bouncy Castle) should be used instead.

---

### 🟡 Medium — Missing Redirect URI Validation

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, line 279

```java
String redirectUri = appRedirectUri != null ? appRedirectUri : Urls.identityProviderAuthnResponse(...);
```

`appRedirectUri` comes directly from the `app_redirect_uri` token exchange request parameter with no validation against a list of allowed URIs. If Apple's server-side validation is misconfigured or bypassed, this could enable an open redirect or an authorization code leak.

---

### 🟡 Medium — Internal Exception Details Exposed in Event

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 169–170

```java
event.detail(Details.REASON, "Unable to extract identity from identity token: " + e.getMessage());
```

The raw Java exception message is included in the Keycloak event, which may be exposed via Admin APIs. This can leak internal implementation details to attackers with admin access.

---

## 2. Logic Bug

### Email Extraction Nested Inside `nameNode != null` Block

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 307–328

```java
if (nameNode != null) {
    // firstName, lastName...
    JsonNode emailNode = profile.get("email"); // ← inside the nameNode block
    if (emailNode != null) {
        appleUser.setEmail(emailNode.asText());
    }
}
```

The email is only extracted when the `name` node is present. If Apple sends `{"email": "foo@bar.com"}` without a `name` field, the email is silently ignored. This bug is even acknowledged in `AppleIdentityProviderTest.java` as a "Known bug" but has never been fixed.

---

## 3. Code Quality

### Exception Swallowing in `handleUserJson`

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, lines 301–303

```java
} catch (Exception e) {
    // do nothing
}
```

All exceptions from `handleUserJson` are silently discarded, including `NullPointerException` and programming errors. At minimum, the exception should be logged at debug level.

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

### Magic Number for Token Expiry

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, line 273

```java
jwt.exp(jwt.getIat() + 86400 * 180);
```

`86400 * 180` (180 days in seconds) is a magic number with no explanation. A named constant such as `CLIENT_SECRET_EXPIRY_SECONDS` would make the intent clear and make the value easier to change.

---

### Manually URL-Encoded Scopes

**File:** `src/main/java/at/klausbetz/provider/AppleIdentityProvider.java`, line 70

```java
return "name%20email";
```

The space is manually percent-encoded. If the framework also encodes this value, the result would be `name%2520email` (double-encoding). Returning `"name email"` and letting the framework handle encoding is the safer approach.

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

| Severity | Issue | File & Lines |
|---|---|---|
| 🔴 Critical | Account takeover via email-based auto-linking | `AppleIdentityProvider.java:107–128` |
| 🔴 Critical | Log injection via unsanitized `@FormParam error` | `AppleIdentityProviderEndpoint.java:68` |
| 🟠 High | TOCTOU race condition on `prepareClientSecret` | `AppleIdentityProvider.java:175–183` |
| 🟠 High | Silent null propagated as client secret | `AppleIdentityProvider.java:228–252` |
| 🟠 High | Fragile PEM parsing; private key in non-zeroable String | `AppleIdentityProvider.java:232–238` |
| 🟡 Medium | Missing redirect URI validation | `AppleIdentityProvider.java:279` |
| 🟡 Medium | Internal exception message exposed in event | `AppleIdentityProvider.java:169–170` |
| 🐛 Bug | Email ignored when `name` node is absent | `AppleIdentityProvider.java:307–328` |
| 🟢 Low | Exception swallowing in `handleUserJson` | `AppleIdentityProvider.java:301–303` |
| 🟢 Low | Magic number `86400 * 180` | `AppleIdentityProvider.java:273` |
| 🟢 Low | Manually URL-encoded scopes | `AppleIdentityProvider.java:70` |
| 🟢 Low | Config mutation in constructor | `AppleIdentityProvider.java:54–60` |
| 🟢 Low | Null used as error sentinel | `AppleIdentityProvider.java:186–198` |
| 🟢 Low | Reflection in tests signals poor testability | `AppleIdentityProviderTest.java` |
