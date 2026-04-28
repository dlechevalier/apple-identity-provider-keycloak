# Security Policy

## Supported Versions

Only the latest release is actively maintained. Security fixes are not
backported to older versions.

| Version | Supported |
|---------|-----------|
| Latest  | ✅ Yes    |
| Older   | ❌ No     |

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Please report security issues by email to the maintainer listed in
[`build.gradle`](build.gradle) (`group = 'at.klausbetz'`), or via
[GitHub's private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability)
if enabled on this repository.

Include in your report:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a proof-of-concept (even partial)
- The affected version(s)
- Any mitigations you are aware of

You can expect an acknowledgement within **5 business days** and a resolution
timeline within **30 days** for critical/high issues.

## Known Security Considerations

The following design decisions have security implications that deployers
should be aware of. They are documented here rather than treated as
vulnerabilities because they either reflect upstream Keycloak behaviour or
require a deliberate operator choice to enable.

### Account Auto-Linking (`autoLinkIfPossible`)

The provider supports automatically linking an existing Keycloak account to
an Apple account when their email addresses match. This is **disabled by
default** but can be enabled via the `tokenExchangeAccountLinkingEnabled`
factory setting.

**Risk:** An attacker controlling an Apple account whose registered email
matches a victim's Keycloak account can silently take over that account.
Apple's Private Email Relay increases this risk because relay addresses are
not verified against the real address at the Keycloak level.

**Recommendation:** Do not enable auto-linking unless you have a strong
trust model for Apple's email verification. Consider requiring explicit
user confirmation before linking.

### Client Secret Expiry (180-Day JWS)

The generated Apple client secret (JWS) has a fixed 180-day expiry
(`86400 × 180` seconds). There is no automatic rotation. If the signing
private key is compromised, all client secrets issued with that key remain
valid until expiry.

**Recommendation:** Rotate the `.p8` private key at the Apple Developer
Portal when a compromise is suspected, and restart Keycloak to force
regeneration.

### Log Injection via `error` Form Parameter

The `error` parameter in the Apple callback endpoint is user-controlled.
It is sanitized with `sanitizeForLog()` before any log call — newline, carriage
return, and tab characters are replaced with `_` (fixed in 1.17.0, CWE-117).
The original finding is documented in [`SECURITY_ANALYSIS.md`](SECURITY_ANALYSIS.md).

## Security Analysis

A full security and code quality analysis for version `1.17.0` is available
in [`SECURITY_ANALYSIS.md`](SECURITY_ANALYSIS.md). It covers all known
vulnerabilities, their severity, and affected file/line references.
