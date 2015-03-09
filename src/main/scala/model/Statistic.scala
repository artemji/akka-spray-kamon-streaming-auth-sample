package model

case class TwitterStatistics(totalTweets: Int, tweetsPerSecond: Double)

case class TrackerStatistics(query: String, twitterStatistic: TwitterStatistics)

case class TwitterAggregatorStatistics(actorName: String, trackerStatistics: TrackerStatistics)