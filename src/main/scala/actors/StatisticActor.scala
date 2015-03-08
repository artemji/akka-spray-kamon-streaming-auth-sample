package actors

import akka.actor.{Actor, ActorLogging}
import model.{Tweet, TwitterStatistics}

object StatisticActor {

  case class Process(tweet: Tweet)

  case object TweetsCount

  case class Slice(startTime: Double, endTime: Double)

}

/**
 * Class representing a StatisticActor.
 *
 * It counts a speed of tweets per second and their number.
 */
class StatisticActor extends Actor with ActorLogging {

  private var totalTweets = 0

  import actors.StatisticActor._

  def receive: Receive = {

    case Process(tweet) =>
      updateCounters(tweet)

    case TweetsCount =>
      sender() ! totalTweets

    case Slice(startTime, endTime) =>
      sender() ! TwitterStatistics(totalTweets, tweetsPerSecond(startTime, endTime))

  }

  private def updateCounters(tweet: Tweet): Unit = {
    totalTweets += 1
  }

  private def tweetsPerSecond(startTime: Double, endTime: Double): Double = {
    totalTweets / (endTime - startTime)
  }

}
