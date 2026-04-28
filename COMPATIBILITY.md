# Keycloak Compatibility

This matrix tracks which SPI version to use for each Keycloak version.

> **Status legend**
> - ✅ Supported — compilation and tests pass
> - ❌ Broken — known incompatibility

---

## SPI 2.0.0 (current)

**Minimum Keycloak: 26.5.0** — uses `UserAuthenticationIdentityProvider.AuthenticationCallback`
introduced in KC 26.5.0.

Versions from KC 26.6.0 onwards are tested automatically by the
[Integration Test CI](.github/workflows/integration-test.yml) on every PR.

| Keycloak | Verification | Notes |
|---|---|---|
| 26.6.1 | ✅ CI — unit + integration tests | |
| 26.6.0 | ✅ CI — unit + integration tests | |
| 26.5.7 | ✅ Unit tests | No embedded test framework — unit tests only |
| 26.5.5 | ✅ Unit tests | |
| 26.5.3 | ✅ Unit tests | |
| 26.5.1 | ✅ Unit tests | |
| 26.5.0 | ✅ Unit tests | |
| 26.4.x and earlier | ❌ Broken | `UserAuthenticationIdentityProvider` not in KC API |

---

## SPI version history (1.x.x — upstream klausbetz)

If you are on KC < 26.5.0, use the corresponding 1.x release below.
The 1.17.0 release was verified to work on KC 25.x and later KC versions
up to at least KC 26.4.x.

| Keycloak Version | SPI Version |
|---|---|
| `< 17.0.0` | Not tested. Use at your own risk. |
| `17.0.0 – 19.0.3` | `1.2.0` |
| `20.0.0 – 20.0.5` | `1.3.0 – 1.4.1` |
| `21.0.0 – 21.0.2` | `1.5.0` |
| `21.1.0 – 21.1.2` | `1.6.0` |
| `22.0.0 – 22.0.x` | `1.7.0 – 1.8.0` |
| `23.0.0 – 24.0.x` | `1.9.0 – 1.12.0` |
| `25.0.0 – 26.2.x` | `1.13.0 – 1.14.0` |
| `26.3.0 – 26.4.x` | `1.15.0 – 1.16.0` |
| `>= 26.5.0` | `1.17.0` (last 1.x) or `2.0.0` (this fork) |

## Notes

- **SPI 2.0.0 on KC 26.6.0+**: Verified automatically by the embedded KC test framework on
  every PR (`dependencyCurrentProject()` API required).
- **SPI 2.0.0 on KC 26.5.x**: Integration tests not supported
  (`dependencyCurrentProject()` not available); unit tests pass for all patches.
- **SPI 1.17.0 on KC < 26.5.0**: Confirmed working on KC 25.x. For KC < 25, use the
  corresponding 1.x version from the table above.
