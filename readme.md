# Technical Test Machine Learning / Teads.tv
**Matthieu MOISSON-FRANCKHAUSER**
**06/05/2018**
**https://github.com/matthieuMoisson/teads**
## Context
Every time a user browses an eligible web page, we have to decide whether to display an ad video or not.

## Log description

Csv file of 100000 examples of video ad displays. This log contains the following columns:
* creative_id: A unique identifier of the video that has been displayed to the user
* user_operating_system: The user Operating System (OS)
* user_device: The user device type
* average_seconds_played: The average number of seconds the user usually watches our videos (only if we already know the user, based on the user history)
* cost: The cost we had to pay to display the video
* revenue: The revenue generated by this video when it has been watched

## Read
 
I decided to use scala. So I created the structure and class as the schema of the csv. 
````
    import org.apache.spark.sql.types._
    val schemaAft = StructType(
     Seq(
       StructField("creative_id", LongType),
       StructField("user_operating_system", StringType),
       StructField("user_device", StringType),
       StructField("average_seconds_played", FloatType),
       StructField("cost", FloatType),
       StructField("revenue", FloatType)
     )
    )
    
    case class Aft (
                    creative_id: Long,
                    user_operating_system: String,
                    user_device: String,
                    average_seconds_played: Float,
                    cost: Float,
                    revenue: Float
                  )
    
    val dataframeAft = spark.read.option("header","true").schema(schemaAft).csv("C:\\Users\\Matthieu\\Documents\\Scala\\teadsTest\\src\\main\\scala\\ressources\\data.csv") 
    
    val DSAft = dataframeAft.as[Aft]

````

We now have a Dataframe and a Dataset 

## The margin being defined as (revenue - cost) / revenue, what is our global margin based on this log?

We need to calculate the total revenue and cost :

````
    val marginTotal: Float = DSAft.map(aft => aft.revenue - aft.cost).reduce((x, y) => x+y)
    val totalRevenue: Float = DSAft.map(_.revenue).reduce((x,y) => x+y)
    val totalCost: Float = DSAft.map(_.cost).reduce((x,y) => x+y)
    val globalMargin: Float = (totalRevenue-totalCost)/totalRevenue
````

The global Margin is **0.27191696** , and the total margin is **211.765**

## Can you think of other interesting metrics to optimize?

The best solution to define new metric is to consult an expert which is the most important.
I suggest two others metrix :
* (revenue - cost) / n Where n is the number of elements, this metrix calculate the means margin
* (revenue - cost) This metrix calculate the total margin

## What are the most profitable Operating Systems?

To find the most profitable Operating System, we need to group by system, then calculate the best margin for all groups

````
    // Group by system
    val DSAftGrouped: KeyValueGroupedDataset[String, Aft] = DSAft.groupByKey(_.user_operating_system)
     
    // Calculate the margin for all groups
    val DSGroupReduced: Dataset[(String, Aft)] = DSAftGrouped.reduceGroups((x,y) => Aft(0, "", "", 0, x.cost + y.cost, x.revenue + y.revenue))
    val DSSystemMargin: Dataset[(String, Float)] = DSGroupReduced.map(elem => {
      (elem._1,(elem._2.revenue-elem._2.cost)/elem._2.revenue)
    })
    // Show the result
    DSSystemMargin.show(20)
````

| System | Result |
| ------ | ------ |
| RIM OS | -Infinity |
| Fire OS | 0.10198293 |
| iOS |  0.3737093 |
| unknown | 0.46523762 |
| OS X | 0.049053278 |
| Linux | 0.13623613 |
| macOS | 0.014937006 |
| Chrome OS | 0.2023785 |
| BlackBerry OS | 0.21675675 |
| Android | 0.23200189 |
| BSD | 0.64773405 |
| Windows | 0.22414444 |


The high score is BSD, but it is not the best because of these ten lines (it is not representative).
The best significative element is **OS X**

## How would you use this historical data to predict the event 'we benefit from a revenue' (ie revenue > 0) in a dataset where the revenue is not known yet?

I split the data into training and test. Then I have to transform into the format needed by the algorithm used. In this case, I use the **MultilayerPerceptronClassifier** in MlLib.

````
    val DSAftModified: Dataset[AftModified] = DSAft.map(aft => {
      val result: Int = {
        if(aft.revenue-aft.cost) {
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
````

I decided to use a neural network. If I had more time, I will compared this algorithm with others (random forest, ...).

What I decided is to detect is the positive benefit.

````
    // specify layers for the neural network:
    // input layer of size 3 (features), one intermediate of size 5
    // and output of size 2 (classes)
    val layers = Array[Int](3, 5, 2)

    // create the trainer and set its parameters
    val trainer = new MultilayerPerceptronClassifier()
      .setLayers(layers)
      .setBlockSize(128)
      .setSeed(1234L)
      .setMaxIter(100)
````

Next I fit the model and calculate the accuracy 

````
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
````

I show the count of positive and negative elements in the data test. So I can compared my result in the case I choose automaticaly the value.
The result seems to be good **0.72**. However I obtain the same result if I put all individus to 1. So this model don't bring information.

To improve this model we have many choices :
* Standardized the input
* Tries with other models
* ...

I decided to change my previous model to detect the ad which had a benefit less important   ((revenue-cost) < 0.01).

I just simplified the previous model and changed one line. 

````
      val DSAftModified: Dataset[AftModified] = DSAft.map(aft => {
      val result: Int = {
        if(aft.revenue-aft.cost  < 0.01) {
          1 // true
        }else {
          0 // false
        }
      }
      AftModified(result, aft.user_device, aft.user_operating_system, aft.cost)
    })
````

Result :
* Accuracy = **0.9651**
* Value(1)/total = **0.89560521973**

 
However I think I could obtain best results if I had more time.

## Ideas
* Standardized input
* Random forest
* ...















