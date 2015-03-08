package util

import org.specs2.Specification

import scala.io.Source

class ExtractorTest extends Specification { def is = s2"""

  This is a specification for the Extractor util

  The Extractor.extractCompleteMessages should
    extract 1 complete message                        $e1
                                                      """
    def e1 = {
      val body = Source.fromInputStream(getClass.getResourceAsStream("/tweet.json")).mkString

      val (completeMessages, remainingPartialContent) = TweetsExtractor.extractCompleteMessages(body)
      val tweets = TweetsExtractor.extractTweets(completeMessages)
      val successTweets = tweets filter(_.isSuccess)
      successTweets.length mustEqual 1
      remainingPartialContent mustEqual ""
    }

}
