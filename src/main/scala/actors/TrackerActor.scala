package actors

import java.util.concurrent.Executor

import actors.TrackerActor.{ConnectionError, CouldNotAuthenticate, UnexpectedResponseStatus}
import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import auth.TwitterAuthorization
import model.{TrackerStatistics, Tweet, TwitterStatistics}
import spray.client.pipelining._
import spray.http._
import util.TweetsExtractor

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Success

object TrackerActor {

  def props(uri: Uri,
            query: String,
            ioActor: ActorRef)(implicit twitterAuthorization: TwitterAuthorization): Props =
    Props(new TrackerActor(uri, query, ioActor))

  case class SearchResult(tweet: Tweet)

  case object CollectStatistics

  case object CouldNotAuthenticate

  case class ConnectionError(cause: Throwable) extends RuntimeException(cause)

  case class UnexpectedResponseStatus(status: StatusCode) extends RuntimeException(s"Unexpected status code: $status")

}

/**
 * Class representing a TrackerActor.
 *
 * @param uri Uri for streaming
 * @param query query to filter
 * @param ioActor IO actor
 * @param twitterAuthorization an authorization logic
 */
class TrackerActor(uri: Uri,
                   query: String,
                   ioActor: ActorRef)(implicit twitterAuthorization: TwitterAuthorization) extends Actor with ActorLogging {

  private implicit val executor = context.dispatcher.asInstanceOf[Executor with ExecutionContext]

  private val statisticActor = context.actorOf(Props[StatisticActor])

  private val startTime: Double = System.currentTimeMillis / 1000.0

  private def currentTime(): Double = System.currentTimeMillis / 1000.0

  override def preStart(): Unit = {
    val searchRequest = Get(uri.withQuery("track" -> query)) ~> twitterAuthorization.authorize
    sendTo(ioActor).withResponsesReceivedBy(self)(searchRequest)
  }

  override def receive = collectingChunks(partialContent = "")

  def collectingChunks(partialContent: String): Receive = {
    case ChunkedResponseStart(response) =>
      log.info(s"ChunkedResponseStart")

    case MessageChunk(data, _) =>
      val updatedContent = partialContent + new String(data.toByteArray, "UTF-8")
      val (completeMessages, remainingPartialContent) = TweetsExtractor.extractCompleteMessages(updatedContent)

      val tweets = TweetsExtractor.extractTweets(completeMessages)

      tweets foreach {
        case Success(tweet) =>
          log.info(
            s"""
               |Query: $query
                |Tweet user: ${tweet.user}
                |Tweet text: ${tweet.text}
           """.stripMargin)
          statisticActor ! StatisticActor.Process(tweet)
        case _ =>
      }

      context become collectingChunks(remainingPartialContent)

    case ChunkedMessageEnd =>
      log.info(s"ChunkedMessageEnd")

    case TrackerActor.CollectStatistics =>
      log.info(s"Start collecting statistic for query '$query'")

      implicit val timeout = Timeout(20 seconds)

      // Get statistics for this tracker from start time to current time
      val eventualTwitterStatistics = (statisticActor ? StatisticActor.Slice(startTime, currentTime())).mapTo[TwitterStatistics]
      val eventualTrackerStatistics = eventualTwitterStatistics map (ts => TrackerStatistics(query, ts))

      eventualTrackerStatistics pipeTo sender()

    case other => commonMessageHandlers(other)
  }

  def commonMessageHandlers: Receive = {
    case Failure(cause) =>
      // connection broken, let supervisor handle this
      log.warning("Connection was broken!")
      throw new ConnectionError(cause)

    case HttpResponse(status, entity, _, _) if status == StatusCodes.Unauthorized =>
      log.error(s"Could not authenticate - received response with status $status: $entity")
      context.parent ! CouldNotAuthenticate
      context.stop(self)

    case HttpResponse(status, entity, _, _) if status != StatusCodes.OK =>
      throw new UnexpectedResponseStatus(status)

    case msg =>
      log.warning(s"Unexpected message at streamer actor: $msg")
  }

}
