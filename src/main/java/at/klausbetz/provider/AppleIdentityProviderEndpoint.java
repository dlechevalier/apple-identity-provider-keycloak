package at.klausbetz.provider;

import static at.klausbetz.provider.AppleIdentityProvider.OAUTH2_PARAMETER_CODE;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class AppleIdentityProviderEndpoint {

  protected static final Logger logger = Logger.getLogger(AppleIdentityProviderEndpoint.class);

  private static final String OAUTH2_PARAMETER_STATE = "state";
  private static final String OAUTH2_PARAMETER_USER = "user";
  private static final String ACCESS_DENIED = "access_denied";
  private static final String USER_CANCELLED_AUTHORIZE = "user_cancelled_authorize";

  private final AppleIdentityProvider appleIdentityProvider;
  private final RealmModel realm;
  private final UserAuthenticationIdentityProvider.AuthenticationCallback callback;
  private final EventBuilder event;

  protected KeycloakSession session;

  public AppleIdentityProviderEndpoint(
      AppleIdentityProvider appleIdentityProvider,
      RealmModel realm,
      UserAuthenticationIdentityProvider.AuthenticationCallback callback,
      EventBuilder event,
      KeycloakSession session) {
    this.appleIdentityProvider = appleIdentityProvider;
    this.realm = realm;
    this.callback = callback;
    this.event = event;
    this.session = session;
  }

  @POST
  public Response authResponse(
      @FormParam(OAUTH2_PARAMETER_STATE) String state,
      @FormParam(OAUTH2_PARAMETER_CODE) String authorizationCode,
      @FormParam(OAUTH2_PARAMETER_USER) String user,
      @FormParam(OAuth2Constants.ERROR) String error) {
    if (state == null) {
      return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_MISSING_STATE_ERROR);
    }

    IdentityBrokerState idpState = IdentityBrokerState.encoded(state, realm);
    String clientId = idpState.getClientId();
    String tabId = idpState.getTabId();
    if (clientId == null || tabId == null) {
      logger.errorf("Invalid state parameter: %s", state);
      return errorIdentityProviderLogin(Messages.INVALID_REQUEST, Response.Status.BAD_REQUEST);
    }

    AuthenticationSessionModel authSession = this.callback.getAndVerifyAuthenticationSession(state);
    session.getContext().setAuthenticationSession(authSession);
    var context = session.getContext();

    logger.debugf(
        "[tab=%s client=%s] Apple callback received: code=%s error=%s hasUser=%s",
        tabId,
        clientId,
        authorizationCode != null ? "present" : "absent",
        sanitizeForLog(error),
        user != null ? "yes" : "no");

    if (error != null) {
      logger.warnf(
          "[tab=%s client=%s] Apple returned error for broker login %s: %s",
          tabId,
          clientId,
          appleIdentityProvider.getConfig().getProviderId(),
          sanitizeForLog(error));
      if (error.equals(ACCESS_DENIED) || error.equals(USER_CANCELLED_AUTHORIZE)) {
        sendErrorEvent();
        return callback.cancelled(this.appleIdentityProvider.getConfig());
      } else if (error.equals(OAuthErrorException.LOGIN_REQUIRED)
          || error.equals(OAuthErrorException.INTERACTION_REQUIRED)) {
        return callback.error(appleIdentityProvider.getConfig(), error);
      } else {
        return callback.error(
            appleIdentityProvider.getConfig(), Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
      }
    }

    try {
      if (authorizationCode != null) {
        appleIdentityProvider.prepareClientSecret(appleIdentityProvider.getConfig().getClientId());
        BrokeredIdentityContext federatedIdentity =
            appleIdentityProvider.sendTokenRequest(
                authorizationCode,
                appleIdentityProvider.getConfig().getClientId(),
                user,
                authSession,
                Urls.identityProviderAuthnResponse(
                        context.getUri().getBaseUri(),
                        appleIdentityProvider.getConfig().getAlias(),
                        context.getRealm().getName())
                    .toString());
        if (federatedIdentity != null) {
          logger.infof(
              "[tab=%s client=%s] Apple authentication succeeded for subject=%s",
              tabId, clientId, federatedIdentity.getId());
          return callback.authenticated(federatedIdentity);
        }
      } else {
        logger.debugf(
            "[tab=%s client=%s] No authorization code received, skipping token exchange",
            tabId, clientId);
      }
    } catch (WebApplicationException e) {
      return e.getResponse();
    } catch (Exception e) {
      logger.errorf(
          e,
          "[tab=%s client=%s] Failed to complete Apple identity provider OAuth callback",
          tabId,
          clientId);
    }
    return errorIdentityProviderLogin(Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
  }

  private Response errorIdentityProviderLogin(String message) {
    return errorIdentityProviderLogin(message, Response.Status.BAD_GATEWAY);
  }

  private Response errorIdentityProviderLogin(String message, Response.Status status) {
    sendErrorEvent();
    return ErrorPage.error(session, null, status, message);
  }

  private static String sanitizeForLog(String input) {
    if (input == null) return null;
    return input.replaceAll("[\r\n\t]", "_");
  }

  private void sendErrorEvent() {
    event.event(EventType.IDENTITY_PROVIDER_LOGIN);
    event.detail("idp", appleIdentityProvider.getConfig().getProviderId());
    event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
  }
}
