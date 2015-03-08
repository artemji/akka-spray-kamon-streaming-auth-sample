package actors

import java.util.concurrent.Executor

import actors.TrackerActor.{ConnectionError, UnexpectedResponseStatus}
import akka.actor._
import auth.TwitterAuthorization
import model.{TwitterAggregatorStatistics, TrackerStatistics}
import spray.http.Uri

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.duration._

object TwitterAggregatorActor {
  val twitterUri = Uri("https://stream.twitter.com/1.1/statuses/filter.json")

  def props(uri: Uri, ioActor: ActorRef)(implicit twitterAuthorization: TwitterAuthorization): Props =
    Props(new TwitterAggregatorActor(uri, ioActor))

  case class Track(query: String)

  case object CollectStatistics

}

/**
 * Class representing a TwitterAggregatorActor.
 *
 * @param uri Uri to stream
 * @param ioActor IO actor
 * @param twitterAuthorization an authorization logic
 */
class TwitterAggregatorActor(uri: Uri,
                             ioActor: ActorRef)(implicit twitterAuthorization: TwitterAuthorization) extends Actor with ActorLogging {
  implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  var reqNo = 0

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(loggingEnabled = false /* too verbose when connection lost */) {
    case _: ConnectionError => SupervisorStrategy.Restart
    case _: UnexpectedResponseStatus => SupervisorStrategy.Restart
    case e =>
      log.error("Unexpected error from tracker: ", e)
      SupervisorStrategy.Escalate
  }

  override def receive: Receive = {

    case TwitterAggregatorActor.Track(query) =>
      reqNo += 1
      log.info(s"Start tracking tweets for query '$query'")
      context.actorOf(TrackerActor.props(uri, query, ioActor), s"tracker_$reqNo")

    case TwitterAggregatorActor.CollectStatistics =>
      implicit val timeout = Timeout(20 seconds)

      // Collect statistics from each child
      val eventualTwitterAggregatorStatistics = Future.traverse(context.children) {
        child => {
          val eventualTrackerStatistics = (child ? TrackerActor.CollectStatistics).mapTo[TrackerStatistics]
          eventualTrackerStatistics map (ss => TwitterAggregatorStatistics(child.path.name, ss))
        }
      }

      eventualTwitterAggregatorStatistics pipeTo sender

    case TrackerActor.CouldNotAuthenticate =>
      log.error("FATAL: Could not authenticate, shutdown everything")
      context.system.shutdown()

    case other => commonMessageHandlers(other)
  }

  def commonMessageHandlers: Receive = {
    case msg => log.warning(s"Unexpected message at twitter aggregator: $msg")
  }

}
