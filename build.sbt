name := "twitter_aggregator"

version := "1.0"

scalaVersion := "2.11.5"

resolvers ++= Seq(
  "spray" at "http://repo.spray.io/",
  Resolver.typesafeRepo("releases"),
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Kamon Repository" at "http://repo.kamon.io"
)

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.1"
  val kamonVersion = "0.3.5"
  Seq(
	  "io.spray"                	  %% "spray-can"            % sprayV,
	  "io.spray" 					          %% "spray-client" 		    % sprayV,
    "io.spray"               	    %% "spray-routing"        % sprayV,
    "io.spray"                    %% "spray-json"           % sprayV,
    "com.typesafe.akka"           %% "akka-actor"           % akkaV,
    "io.kamon"                    %% "kamon-core"           % kamonVersion,
    "io.kamon"                    %% "kamon-statsd"         % kamonVersion,
    "io.kamon"                    %% "kamon-log-reporter"   % kamonVersion,
    "io.kamon"                    %% "kamon-system-metrics" % kamonVersion,
    "org.aspectj"                  % "aspectjweaver"        % "1.8.1",
    "ch.qos.logback"               % "logback-classic"      % "1.1.2",
    "com.typesafe.akka"           %% "akka-testkit"         % akkaV     % "test",
    "org.specs2"                  %% "specs2-core"          % "3.0"     % "test",
    "io.spray"                	  %% "spray-testkit"        % sprayV    % "test"
  )
}

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj

// when you call "sbt run" aspectj weaving kicks in
fork in run := true

lazy val root = project in file(".")