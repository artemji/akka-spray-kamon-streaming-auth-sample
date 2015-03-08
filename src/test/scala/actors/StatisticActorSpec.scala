package actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import model.{TwitterStatistics, Tweet, TwitterUser}
import org.specs2.mutable.SpecificationLike

class StatisticActorSpec
  extends TestKit(ActorSystem("StatisticActorSpec")) with SpecificationLike with ImplicitSender {
  sequential

  val tweet: Tweet = Tweet(TwitterUser("sName", "dName"), "text")

  "Process(tweet)" >> {

    "Should increment tweets' counter to one" in {
      val testStatisticActor = TestActorRef(new StatisticActor)
      testStatisticActor ! StatisticActor.Process(tweet)
      testStatisticActor ! StatisticActor.TweetsCount
      val count = expectMsgType[Int]
      count mustEqual 1
      success
    }
  }

  "Slice(start, end)" >> {

    "Should return TwitterStatistic" in {
      val testStatisticActor = TestActorRef(new StatisticActor)
      (1 to 5).foreach(_ => testStatisticActor ! StatisticActor.Process(tweet))
      testStatisticActor ! StatisticActor.Slice(1, 6)
      val twitterStatistic = expectMsgType[TwitterStatistics]
      twitterStatistic.totalTweets mustEqual 5
      twitterStatistic.tweetsPerSecond mustEqual 1
      success
    }
  }

}
