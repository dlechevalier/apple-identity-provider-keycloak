# Keycloak Compatibility

This matrix tracks which Keycloak versions are verified to work with SPI version **1.17.0**.

Versions from KC 26.6.0 onwards are tested automatically by the
[Integration Test CI](.github/workflows/integration-test.yml) on every PR.
Older versions are verified by compiling and running the unit test suite.

> **Status legend**
> - ✅ Supported — compilation and tests pass
> - ❌ Broken — known incompatibility (see notes)

## Current SPI version: 1.17.0

### Supported versions

| Keycloak | Verification | Notes |
|---|---|---|
| 26.6.1 | ✅ CI — unit + integration tests | |
| 26.6.0 | ✅ CI — unit + integration tests | |
| 26.5.7 | ✅ Unit tests (all patches verified) | No embedded test framework — unit tests only |
| 26.5.5 | ✅ Unit tests | |
| 26.5.3 | ✅ Unit tests | |
| 26.5.1 | ✅ Unit tests | |
| 26.5.0 | ✅ Unit tests | |

### Broken versions

| Keycloak | Status | Notes |
|---|---|---|
| 26.4.x and earlier | ❌ Broken | `UserAuthenticationIdentityProvider` not in KC API — compilation fails |
| 26.3.x | ❌ Broken | Same |
| 26.2.x and earlier | ❌ Broken | Same + `exchangeExternalTokenV1Impl` not in supertype |

## Notes

- **KC 26.6.0+**: Verified automatically by the embedded KC test framework on every PR
  (`dependencyCurrentProject()` API required).
- **KC 26.5.x**: `UserAuthenticationIdentityProvider.AuthenticationCallback` was introduced
  in KC 26.5.0. All patches (26.5.0–26.5.7) pass unit tests. Integration tests are not
  supported for this range (`dependencyCurrentProject()` not available).
- **KC < 26.5.0**: The current SPI code uses `UserAuthenticationIdentityProvider` which does
  not exist before KC 26.5.0. Compilation fails. Previous versions of this SPI (≤ 1.16.0)
  supported older KC versions — see the upstream klausbetz/apple-identity-provider-keycloak
  repository for compatibility details.
