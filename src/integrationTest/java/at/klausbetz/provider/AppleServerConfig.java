package at.klausbetz.provider;

import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;

/**
 * Boots the embedded KC server with the Apple identity provider SPI loaded.
 *
 * <p>{@code dependencyCurrentProject()} tells the framework to package this module (from {@code
 * target/classes/}) into the embedded server's {@code providers/} directory. No additional
 * configuration is needed: {@link AppleIdentityProviderFactory} has no {@code init()} method and
 * does not read environment variables or system properties.
 */
public class AppleServerConfig implements KeycloakServerConfig {

  @Override
  public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder server) {
    return server.dependencyCurrentProject();
  }
}
