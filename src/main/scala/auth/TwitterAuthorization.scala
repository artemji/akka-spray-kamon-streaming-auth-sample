package auth

import spray.http.HttpRequest

trait TwitterAuthorization {
  def authorize: HttpRequest => HttpRequest
}
