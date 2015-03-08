package auth

import auth.AuthenticationStage.Authenticated
import spray.http.HttpRequest

class PinBasedTwitterAuthorization(consumerCredentials: ConsumerCredentials,
                                   accessToken: AccessToken) extends TwitterAuthorization {

  private val signRequest =
    new OAuthSignatureHelper(authStage = Authenticated(consumerCredentials, accessToken)).signOAuthRequestTransformer

  override def authorize: (HttpRequest) => HttpRequest = signRequest
}
