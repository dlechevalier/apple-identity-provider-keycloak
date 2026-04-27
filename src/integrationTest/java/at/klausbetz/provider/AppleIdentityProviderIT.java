package at.klausbetz.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.info.SpiInfoRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

/**
 * Integration tests for the Apple Identity Provider SPI.
 *
 * <p>Tests verify that the module:
 *
 * <ol>
 *   <li>Loads and registers in the embedded Keycloak server without error.
 *   <li>Exposes {@link AppleIdentityProviderFactory} in KC's social identity provider registry.
 *   <li>Exposes all four identity provider mappers in KC's mapper registry.
 * </ol>
 *
 * <p>{@link AppleIdentityProviderFactory} has no {@code init()} method and requires no credentials
 * or external services to register. The provider URLs (Apple token endpoint, JWKS endpoint) are set
 * lazily in the constructor when an IDP instance is created — not at factory registration time.
 *
 * <p>Functional callback tests (simulating Apple's form-POST callback and mocking Apple's token
 * endpoint) require the Apple URLs to be configurable at the IDP level. This is tracked as a future
 * improvement.
 *
 * <p>Run with: {@code ./gradlew integrationTest}
 *
 * <p>Run against a specific KC version: {@code ./gradlew integrationTest -PkcVersion=26.6.1}
 */
@KeycloakIntegrationTest(config = AppleServerConfig.class)
public class AppleIdentityProviderIT {

  @InjectAdminClient Keycloak adminClient;

  // -------------------------------------------------------------------------
  // Provider loading
  // -------------------------------------------------------------------------

  @Test
  void providerLoadsWithoutError() {
    // If the SPI factory failed to load, the embedded server would not start and
    // this assertion would never be reached. The master realm always exists.
    assertThat(adminClient.realm("master").toRepresentation())
        .as("Master realm must be accessible — AppleIdentityProviderFactory loaded successfully")
        .isNotNull();
  }

  // -------------------------------------------------------------------------
  // Social provider registration
  // -------------------------------------------------------------------------

  @Test
  void socialProviderIsRegistered() {
    // KC's server info exposes all registered SocialIdentityProviderFactory implementations
    // in getSocialProviders(). Each entry is a map with an "id" key.
    // If the service file is missing or the factory failed to load, "apple" would be absent.
    List<Map<String, String>> socialProviders =
        adminClient.serverInfo().getInfo().getSocialProviders();

    assertThat(socialProviders)
        .as("apple must appear in KC's registered social identity providers")
        .extracting(m -> m.get("id"))
        .contains(AppleIdentityProviderFactory.PROVIDER_ID);
  }

  // -------------------------------------------------------------------------
  // Identity provider mapper registration
  // -------------------------------------------------------------------------

  @Test
  void identityProviderMappersAreRegistered() {
    // KC's server info exposes all registered IdentityProviderMapper implementations
    // under the "identity-provider-mapper" SPI key.
    // If any mapper's service file entry is missing, it would be absent here.
    SpiInfoRepresentation mapperSpi =
        adminClient.serverInfo().getInfo().getProviders().get("identity-provider-mapper");

    assertThat(mapperSpi)
        .as("identity-provider-mapper SPI must be present in server info")
        .isNotNull();

    assertThat(mapperSpi.getProviders())
        .as("All four Apple identity provider mappers must be registered")
        .containsKeys(
            "apple-user-attribute-mapper",
            "apple-json-user-attribute-mapper",
            "apple-claim-user-session-note-mapper",
            "apple-username-template-mapper");
  }
}
