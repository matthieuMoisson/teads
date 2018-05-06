package teads

import org.apache.spark.sql._
import org.apache.log4j.{Level, Logger}
import model._
import org.apache.spark.ml.classification.MultilayerPerceptronClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg.Vectors
import serializer.Serializer._

object Analysis {

  def main(cmdLineArgs: Array[String]): Unit = {
    println("Teads Up & Running")
    val start = System.currentTimeMillis

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val spark: SparkSession = SparkSession.builder.master("local").appName("Analysis").getOrCreate()
    import spark.implicits._

    println("To begining we will load all data, then display top five rows ")
    val dataframeAft = spark.read.option("header","true").schema(schemaAft).csv("C:\\Users\\Matthieu\\Documents\\Scala\\teadsTest\\src\\main\\scala\\ressources\\data.csv")

    //dataframeAft.show(5)

    val DSAft = dataframeAft.as[Aft]

    DSAft.show(5)

    println("The margin being defined as (revenue - cost) / revenue, what is our global margin based on this log?")

    val marginTotal: Float = DSAft.map(aft => aft.revenue - aft.cost).reduce((x, y) => x+y)

    val totalRevenue: Float = DSAft.map(_.revenue).reduce((x,y) => x+y)
    val totalCost: Float = DSAft.map(_.cost).reduce((x,y) => x+y)
    val globalMargin: Float = (totalRevenue-totalCost)/totalRevenue

    println("The global Margin is " + globalMargin + " , and the total margin is " + marginTotal)
    println("")
    println("Can you think of other interesting metrics to optimize?")
    println("(revenue - cost)/n where n is the number of elements, this metrix calculate the means margin")
    println("(revenue - cost), this metrix calculate the total margin")
    println("")
    println("What are the most profitable Operating Systems?")
    println("To find the most profitable Operating System, we need to group by system, then calculate the best margin for all groups")

    val DSAftGrouped: KeyValueGroupedDataset[String, Aft] = DSAft.groupByKey(_.user_operating_system)

    val DSGroupReduced: Dataset[(String, Aft)] = DSAftGrouped.reduceGroups((x,y) => Aft(0, "", "", 0, x.cost + y.cost, x.revenue + y.revenue))

    val DSSystemMargin: Dataset[(String, Float)] = DSGroupReduced.map(elem => {
      (elem._1,(elem._2.revenue-elem._2.cost)/elem._2.revenue)
    })

    println("This table show all systems with it's global margin")
    DSSystemMargin.show(20)
    println("")
    
    println("Now we will transform the data")
    val DSAftModified: Dataset[AftModified] = DSAft.map(aft => {
      val result: Int = {
        if(aft.revenue-aft.cost > -0.01) {
          1 // true
        }else {
          0 // false
        }
      }
      AftModified(result, aft.user_device, aft.user_operating_system, aft.cost)
    })

    val trainingAndTest: Array[Dataset[AftModified]] = DSAftModified.randomSplit(Array(0.6,0.4), seed = 1234L)

    val train: Dataset[AftModified] = trainingAndTest(0)
    val test: Dataset[AftModified] = trainingAndTest(1)

    val parsedTraining = train.map(elem => {
      val systemDouble: Double = systemStringToDouble(elem.user_operating_system)
      val deviceDouble: Double = deviceStringToDouble(elem.user_device)
      LabeledPoint(elem.etat, Vectors.dense(Array(systemDouble,deviceDouble, elem.cost)))
    })

    val parsedTest = test.map(elem => {
      val systemDouble: Double = systemStringToDouble(elem.user_operating_system)
      val deviceDouble: Double = deviceStringToDouble(elem.user_device)
      LabeledPoint(elem.etat, Vectors.dense(Array(systemDouble,deviceDouble, elem.cost)))
    })

    println("The training countain " + train.count + " elements")
    println("The test countain " + test.count + " elements")

    // specify layers for the neural network:
    // input layer of size 4 (features), two intermediate of size 5 and 4
    // and output of size 3 (classes)
    val layers = Array[Int](3, 5, 2)

    // create the trainer and set its parameters
    val trainer = new MultilayerPerceptronClassifier()
      .setLayers(layers)
      .setBlockSize(128)
      .setSeed(1234L)
      .setMaxIter(100)

    // train the model
    val model = trainer.fit(parsedTraining)

    // Compute accuracy on the test set
    val result = model.transform(parsedTest)
    val predictionAndLabels = result.select("prediction","label")

    val evaluator = new MulticlassClassificationEvaluator()
      .setMetricName("accuracy")

    val testGroupByEtat = test.groupByKey(_.etat)
    val nbElemByGroup = testGroupByEtat.count()
    nbElemByGroup.show(2)
    println("Test set accuracy = " + evaluator.evaluate(predictionAndLabels))

    println("")
    val end = System.currentTimeMillis
    println("The job has been done in : " + (end - start) + " ms")
  }


  def deviceStringToDouble(device: String): Double = {
    if(device == "PersonalComputer") {
      1.0
    }else if (device == "Phone"){
      2.0
    }else if (device == "Tablet"){
      3.0
    }else if (device == "null"){
      4.0
    }else if (device == "ConnectedTv"){
      5.0
    }else{
      0.0
    }
  }

  def systemStringToDouble(system: String): Double = {
    if(system == "Android") {
      1.0
    }else if (system == "BlackBerry OS"){
      2.0
    }else if (system == "Chrome OS"){
      3.0
    }else if (system == "macOS"){
      4.0
    }else if (system == "RIM OS"){
      5.0
    }else if (system == "Fire OS"){
      6.0
    }else if (system == "iOS"){
      7.0
    }else if (system == "unknown"){
      8.0
    }else if (system == "OS X"){
      9.0
    }else if (system == "Linux"){
      10.0
    }else if (system == "BSD"){
      11.0
    }else if (system == "Windows"){
      12
    }else{
      0.0
    }
  }



}
