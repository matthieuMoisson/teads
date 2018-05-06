name := "teadsTest"

version := "0.1"

scalaVersion := "2.11.8"

sbtVersion := "0.13.11"

val sparkStandAloneVersion = s"2.2.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkStandAloneVersion,
  "org.apache.spark" %% "spark-sql" % sparkStandAloneVersion,
  "org.apache.spark" %% "spark-mllib" % sparkStandAloneVersion,
  "org.apache.spark" %% "spark-streaming" % sparkStandAloneVersion,
  "org.apache.spark" %% "spark-hive" % sparkStandAloneVersion
)