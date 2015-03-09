import java.io.File
import java.util.concurrent.Executor

import actors.TwitterAggregatorActor
import akka.actor._
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import auth._
import com.typesafe.config.{Config, ConfigFactory}
import model.TwitterAggregatorStatistics
import spray.can.Http

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.io.StdIn

object Main {

  def readPinCode(requestToken: RequestToken): String = {
    StdIn.readLine(s"\n\nPlease authorize application going to ${requestToken.authenticateUrl} and enter PIN code: ").trim()
  }

  def readOrCreateAccessToken(config: Config, system: ActorSystem): (ConsumerCredentials, AccessToken) = {
    val consumer = ConsumerCredentials(
      config.getString("consumer.key"),
      config.getString("consumer.secret")
    )

    val accessTokenFuture =
      (config.getString("access.token.key"), config.getString("access.token.secret")) match {
        case (key, secret) if key.nonEmpty && secret.nonEmpty =>
          Future.successful(AccessToken(token = key, secret = secret))

        case (_, _) =>
          val authenticator = new TwitterAuthenticator(system)

          import system.dispatcher
          for {
            requestToken <- authenticator.requestToken(consumer)
            pinCode = readPinCode(requestToken)
            accessToken <- authenticator.accessToken(consumer, requestToken, pinCode)
          } yield accessToken
      }

    val accessToken = Await.result(accessTokenFuture, Duration.Inf) // TODO: error handling
    println(s"Using access token $accessToken")

    (consumer, accessToken)
  }

  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      println("Please, provide a path to config file.")
      sys.exit()
    }

    val configPath = args(0) // configPath

    println(s"Config file path: $configPath")

    val userConfig = ConfigFactory.parseFile(new File(configPath))

    val applicationConfig: Config = ConfigFactory.load("application")

    val mergedConfig = userConfig.withFallback(applicationConfig)

    val system = ActorSystem()

    val searchQueries = mergedConfig.getStringList("twitter.search.queries").toList

    val (consumerCredentials, accessToken) = readOrCreateAccessToken(mergedConfig, system)

    implicit val twitterAuthorization = new PinBasedTwitterAuthorization(consumerCredentials, accessToken)

    val ioActor = IO(Http)(system)

    val twitterAggregator = system.actorOf(TwitterAggregatorActor.props(TwitterAggregatorActor.twitterUri, ioActor), "TwitterAggregator")

    searchQueries foreach {
      query => twitterAggregator ! TwitterAggregatorActor.Track(query)
    }

    @tailrec
    def commandLoop(): Unit = {

      def printStatistic(): Unit = {
        implicit val executor = system.dispatcher.asInstanceOf[Executor with ExecutionContext]

        implicit val timeout = Timeout(20 seconds)

        val eventualTwitterAggregatorStatistics = (twitterAggregator ? TwitterAggregatorActor.CollectStatistics).mapTo[Iterable[TwitterAggregatorStatistics]]

        val stats = Await.result(eventualTwitterAggregatorStatistics, timeout.duration)

        stats foreach {
          stat => println(
            s"""
               |Streamer: ${stat.actorName}
                |Query: ${stat.trackerStatistics.query}
                |TotalTweets: ${stat.trackerStatistics.twitterStatistic.totalTweets}
                |Tweets Per Second: ${stat.trackerStatistics.twitterStatistic.tweetsPerSecond}
            """.stripMargin
          )
        }
      }

      Console.readLine() match {
        case "quit" =>
          printStatistic()
          system.shutdown()
          return
        case _ => println("Only 'quit' is supported.")
      }

      commandLoop()
    }

    // start processing the commands
    commandLoop()
  }

}

