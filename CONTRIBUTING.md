# Contributing

Thank you for your interest in contributing to this project. The sections
below describe the workflow, code style requirements, and testing expectations.

## Workflow

1. Fork the repository and create a branch from `main`.
2. Make your changes (see code style and testing sections below).
3. Open a pull request against `main`. CI must be green before merge.

For non-trivial changes, open an issue first to discuss the approach.

## Code Style

This project uses [Spotless](https://github.com/diffplug/spotless) with
[Google Java Format](https://github.com/google/google-java-format) to enforce
a consistent style.

Run before committing:

```bash
./gradlew spotlessApply
```

CI will fail if `spotlessCheck` finds unformatted code.

## Testing

### Unit Tests

```bash
./gradlew test
```

Coverage is enforced at **65% line and branch** via JaCoCo. If you add new
code paths, add corresponding unit tests.

```bash
./gradlew jacocoTestReport jacocoTestCoverageVerification
```

The HTML report is generated at `build/reports/jacoco/test/html/index.html`.

### Integration Tests

Integration tests use the [Keycloak embedded test framework](https://www.keycloak.org/docs/latest/server_development/#writing-integration-tests)
and require **KC 26.6.0+** (earlier versions do not expose the
`dependencyCurrentProject()` API).

```bash
# Default version (see build.gradle ext.keycloakVersion)
./gradlew integrationTest

# Against a specific KC release
./gradlew integrationTest -PkcVersion=26.6.1
```

Integration tests spin up an embedded Keycloak server and verify:

- The SPI loads and registers without error
- `AppleIdentityProviderFactory` appears in KC's social provider registry
- All four identity provider mappers are registered

See [`COMPATIBILITY.md`](COMPATIBILITY.md) for the list of verified KC
versions.

## Keycloak Compatibility

When adding a dependency or using a Keycloak API, check that it is available
in **KC 22+** (the minimum supported version). For APIs added in later
versions, document the constraint in `COMPATIBILITY.md`.

If you verify compatibility with a new KC version, update the table in
`COMPATIBILITY.md`.

## Security

Please do not open public issues for security vulnerabilities. See
[`SECURITY.md`](SECURITY.md) for the responsible disclosure process.

## Pull Request Checklist

- [ ] `./gradlew spotlessApply` applied (no Spotless diff)
- [ ] `./gradlew test jacocoTestCoverageVerification` passes
- [ ] `./gradlew integrationTest` passes (against the default KC version)
- [ ] `COMPATIBILITY.md` updated if a new KC version was verified
- [ ] `CHANGELOG.md` entry added for user-visible changes
