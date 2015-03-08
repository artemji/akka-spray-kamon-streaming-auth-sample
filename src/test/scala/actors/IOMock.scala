package actors

import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import auth.TwitterAuthorization
import spray.can.Http
import spray.http._

import scala.io.Source

trait IOMock {

  val port = 12345
  val uri = Uri(s"http://localhost:$port/")
  val body = Source.fromInputStream(getClass.getResourceAsStream("/tweet.json")).mkString

  implicit val twitterAuthorization = new TwitterAuthorization {
    override def authorize: (HttpRequest) => HttpRequest = identity
  }

  private var _service: Option[ActorRef] = None

  def service(implicit system: ActorSystem): ActorRef = {
    if (_service.isDefined) {
      return _service.get
    }
    _service = Some(system.actorOf(Props(new Service)))
    _service.get
  }

  private class Service extends Actor {

    def receive: Receive = {
      case _: Http.Connected =>
        sender ! Http.Register(self)
      case HttpRequest(HttpMethods.GET, _, _, _, _) =>
        sender ! ChunkedResponseStart(HttpResponse(StatusCodes.OK))
        sender ! MessageChunk(body = body)
        sender ! ChunkedMessageEnd()
    }
  }

}
