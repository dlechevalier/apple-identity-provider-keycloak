# Changelog

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [2.0.0] - 2026-04-28

> **Minimum Keycloak version raised to 26.5.0.**
> This version uses `UserAuthenticationIdentityProvider.AuthenticationCallback`,
> introduced in KC 26.5.0. If you are on KC < 26.5, stay on 1.17.0.

### Breaking
- Minimum supported Keycloak version is now **26.5.0**
  (uses `UserAuthenticationIdentityProvider.AuthenticationCallback` from KC 26.5 SPI)

### Security
- Auto-linking of existing accounts by email match now **disabled by default**
  (was enabled by default — potential account takeover vector)
- Sanitize user-controlled `error` parameter and Apple response body before logging (log injection, CWE-117)
- `generateJWS` now throws `IdentityBrokerException` on failure instead of silently returning null
- PEM key parsing now handles CRLF line endings and all whitespace variants
- `prepareClientSecret` is now synchronized to prevent TOCTOU race condition
- `app_redirect_uri` is now validated (absolute URI, HTTP/HTTPS scheme only)
- Internal exception messages removed from Keycloak event details

### Fixed
- Email extraction now works when Apple sends no `name` field in user JSON
- Scopes returned as `name email` (unencoded) instead of `name%20email` to prevent double-encoding

### Added
- Unit tests covering all major flows: token exchange, user parsing, auto-linking, mappers, config, endpoint
- Tests for security fixes: redirect URI validation, JWS failure, auto-linking default
- Integration tests using the KC embedded test framework (KC 26.6.0+)
- Automated CI compatibility matrix (KC 26.6.0+)
- JaCoCo coverage gate (65% line and branch)
- Spotless / Google Java Format enforcement
- SECURITY.md, CONTRIBUTING.md, COMPATIBILITY.md

### Changed
- Bump Keycloak dependencies to 26.6.1 (test/compile scope)
- Bump to Java 21
- Token expiry magic number `86400 * 180` replaced by named constant `CLIENT_SECRET_EXPIRY_SECONDS`
- Swallowed exception in `handleUserJson` now logged at DEBUG level

### Observability
- Added DEBUG/INFO/WARN log entries with `tab` and `client` correlation IDs across the OAuth callback and token exchange flows

## [1.17.0] - 2026-01-15

### Fixed
- Set email from `userJson` when email is missing in the ID token

## [1.16.0] - 2025-10-06
### Added
- Auto-link accounts in token exchange flow
- Custom redirect URI support in token exchange (`app_redirect_uri` parameter)

### Fixed
- Wording of `tokenExchangeLinksAccount` configuration label

### Docs
- Added private relay email documentation
- Updated README

## [1.15.0] - 2025-07-17
### Changed
- Bump Keycloak to 26.3.0

### Fixed
- Eliminated NullPointerException in troubleshooting scenarios

### Docs
- Improved troubleshooting and upgrading guide

## [1.14.0] - 2025-03-29
### Changed
- **Breaking:** Replaced the `p8-Key` field with `Client Secret` to leverage Keycloak Vault support
  (see UPGRADE.md for migration instructions)

### Docs
- Added token exchange configuration guide
- Added Apple Developer Portal configuration guide

## [1.13.0] - 2024-06-18
### Changed
- Bump Keycloak to 25.0.0

### Added
- GitHub release workflow (CI)

## [1.12.0] - 2024-06-08
### Fixed
- `id_token` exchange broken from Keycloak 24.0.3 onwards

## [1.11.0] - 2024-06-03
### Added
- `AppleJsonUserAttributeMapper`: map JSON attributes from Apple token
- `AppleUserSessionNoteMapper`: map claims to user session notes
- Editable display name for the provider in Keycloak UI

## [1.10.0] - 2024-02-26
### Added
- `AppleUserAttributeMapper`: map Apple user attributes to Keycloak user attributes

## [1.9.0] - 2023-12-24
### Added
- Support for Keycloak 23.x

## [1.8.0] - 2023-12-15
### Added
- Fire `IDENTITY_PROVIDER_LOGIN` event when user cancels login

## [1.7.1] - 2023-09-11
### Changed
- Bump Keycloak to 22.0.2

## [1.7.0] - 2023-07-13
### Changed
- Config properties made compatible with new Keycloak Admin UI

## [1.6.0] - 2023-04-23
### Fixed
- Breaking interface change in Keycloak 21.1.0

## [1.5.0] - 2023-04-23
### Changed
- Adjust provider code to refactored Keycloak internals (v21)

## [1.4.1] - 2023-02-22
### Fixed
- Default scopes aligned with Apple documentation
- Handle user consent cancellation properly

## [1.4.0] - 2022-12-09
### Added
- Support for ID token exchange (`urn:ietf:params:oauth:token-type:id_token`)

## [1.3.1] - 2022-12-05
### Changed
- Enhanced description for `p8-Key` configuration field

## [1.3.0] - 2022-11-27
### Fixed
- Error on callback: method not found (#15)

## [1.2.0] - 2022-08-19
### Fixed
- Theme resources not loaded on Keycloak v19
- Bump dependencies to Keycloak 19.0.1

## [1.1.0] - 2022-05-03
### Fixed
- Signing of Apple's `client_secret` token for token exchange
- Token exchange for `app_identifier` values different from the configured Service ID

## [1.0.0] - 2022-04-25
### Added
- Initial release: Apple Sign-in identity provider for Keycloak
- OAuth2 callback endpoint with `form_post` response mode
- JWT client secret generation via ECDSA private key (`.p8`)
- Token exchange support (authorization code flow)
- User data extraction (first name, last name, email)
