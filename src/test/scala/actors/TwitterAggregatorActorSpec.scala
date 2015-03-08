package actors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import model.TwitterAggregatorStatistics
import org.specs2.mutable.SpecificationLike

class TwitterAggregatorActorSpec
  extends TestKit(ActorSystem("TwitterAggregatorActorSpec")) with SpecificationLike with ImplicitSender with IOMock {
  sequential

  val ioActor: ActorRef = service

  "Track 5 queries" >> {

    "Should return 5 TwitterAggregatorStatistics" in {
      val twitterAggregatorActor = TestActorRef(TwitterAggregatorActor.props(uri, service))
      (1 to 5) foreach(query => twitterAggregatorActor ! TwitterAggregatorActor.Track(query.toString))
      Thread.sleep(1000)
      twitterAggregatorActor ! TwitterAggregatorActor.CollectStatistics
      val twitterAggregatorStatistics = expectMsgType[Iterable[TwitterAggregatorStatistics]]
      twitterAggregatorStatistics.size mustEqual 5
    }

  }

}
