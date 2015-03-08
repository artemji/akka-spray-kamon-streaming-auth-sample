package actors

import actors.TrackerActor.ConnectionError
import akka.actor.{ActorRef, ActorSystem}
import akka.actor.Status.Failure
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import model.TrackerStatistics
import org.specs2.mutable.SpecificationLike

class TrackerActorSpec
  extends TestKit(ActorSystem("TwitterAggregatorActorSpec")) with SpecificationLike with ImplicitSender with IOMock {
  sequential

  val ioActor: ActorRef = service

  "CollectStatistics" >> {

    "Should return TrackerStatistics" in {
      val testTrackerActor = TestActorRef(TrackerActor.props(uri, "test", ioActor))
      Thread.sleep(1000)
      testTrackerActor ! TrackerActor.CollectStatistics
      val trackerStatistic = expectMsgType[TrackerStatistics]
      trackerStatistic.query mustEqual "test"
      trackerStatistic.twitterStatistic.totalTweets mustEqual 1
    }

  }

  "TrackerActor ! Failure" >> {

    "Should throw ConnectionError" in {
      val testTrackerActor = TestActorRef(TrackerActor.props(uri, "test", ioActor))
      testTrackerActor.receive(Failure(new Exception("Connection error"))) must throwA[ConnectionError]
    }

  }

}
