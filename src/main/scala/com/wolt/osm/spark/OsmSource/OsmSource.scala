package com.wolt.osm.spark.OsmSource

import java.io.File
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkFiles
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability, TableProvider}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types._
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import scala.collection.JavaConverters.setAsJavaSetConverter

object OsmSource {
  val OSM_SOURCE_NAME = "com.wolt.osm.spark.OsmSource"

  private val info = Seq(
    StructField("UID", IntegerType, nullable = true),
    StructField("USERNAME", StringType, nullable = true),
    StructField("VERSION", IntegerType, nullable = true),
    StructField("TIMESTAMP", LongType, nullable = true),
    StructField("CHANGESET", LongType, nullable = true),
    StructField("VISIBLE", BooleanType, nullable = false)
  )

  private val member = Seq(
    StructField("ID", LongType, nullable = false),
    StructField("ROLE", StringType, nullable = true),
    StructField("TYPE", IntegerType, nullable = false)
  )

  private val fields = Seq(
    StructField("ID", LongType, nullable = false),
    StructField("TAG", MapType(StringType, StringType, valueContainsNull = false), nullable = false),
    StructField("INFO", StructType(info), nullable = true),
    StructField("TYPE", IntegerType, nullable = false),
    StructField("LAT", DoubleType, nullable = true),
    StructField("LON", DoubleType, nullable = true),
    StructField("WAY", ArrayType(LongType, containsNull = false), nullable = true),
    StructField("RELATION", ArrayType(StructType(member), containsNull = false), nullable = true)
  )

  val schema: StructType = StructType(fields)
}

class DefaultSource extends TableProvider {

  override def inferSchema(options: CaseInsensitiveStringMap): StructType = null

  override def getTable(schema: StructType, partitioning: Array[Transform], properties: util.Map[String, String]): Table = new OsmTable()
}