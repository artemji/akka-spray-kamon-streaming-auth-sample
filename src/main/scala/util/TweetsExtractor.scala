package util

import model.Tweet

import scala.util.Try
import spray.json._

object TweetsExtractor {

  private val Separator = "\r\n"

  def extractCompleteMessages(partialContent: String): (Seq[String], String) = {
    val completeMessagesEnd = partialContent.lastIndexOf(Separator)

    if (completeMessagesEnd >= 0) {
      val (completeMessagesString, remainingPartialContent) = partialContent.splitAt(completeMessagesEnd)
      val completeMessages = completeMessagesString.split(Separator).toSeq.filter(_.nonEmpty)

      (completeMessages, remainingPartialContent.substring(Separator.length))
    } else {
      (Nil, partialContent)
    }
  }
  
  def extractTweets(completeMessages: Seq[String]): Seq[Try[Tweet]] = {
    completeMessages map { message =>
      Try(message.parseJson.convertTo[Tweet])
    }
  }
}
