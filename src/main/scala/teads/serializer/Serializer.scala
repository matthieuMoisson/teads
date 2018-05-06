package teads.serializer

import org.apache.spark.sql.types._

object Serializer {
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
}
