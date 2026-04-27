# Keycloak Compatibility

This matrix tracks which Keycloak versions are verified to work with each SPI release.

Versions from KC 26.0 onwards are tested automatically by the [Integration Test CI](.github/workflows/integration-test.yml)
on every PR. Older versions are verified manually.

> **Status legend**
> - ✅ Supported — verified by CI or manual testing
> - ⏳ Pending — not yet run against this version
> - ❌ Broken — known incompatibility (see notes)

## Current SPI version: 1.17.0

| Keycloak | Verification | Notes |
|---|---|---|
| 26.6.1 | ⏳ CI (pending first run) | |
| 26.5.0 | ✅ Manual | KC 26.5 uses an older test framework API — unit tests only |
| 26.3.0 | ✅ Manual | |
| 26.2.0 | ✅ Manual | |
| 26.1.0 | ✅ Manual | |
| 26.0.0 | ✅ Manual | |
| 25.0.x | ✅ Manual | |
| 24.0.x | ✅ Manual | |
| 23.0.x | ✅ Manual | |
| 22.0.x | ✅ Manual | Requires Keycloak 22+ for OIDC broker API |
| 21.0.x | ✅ Manual | |
| 20.0.x | ✅ Manual | Requires migration — see UPGRADE.md |

## Notes

- **KC 26.6+**: Verified automatically by the embedded KC test framework on every PR.
- **KC 26.0–26.5**: The `dependencyCurrentProject()` test framework API is not available.
  Compatibility is verified manually.
- **KC < 26**: The KC embedded test framework is not available for these versions.  
  Compatibility is verified manually and tracked in the [README](README.md).
- **KC 22**: Minimum version for current API compatibility (`keycloak-services` OIDC broker).
