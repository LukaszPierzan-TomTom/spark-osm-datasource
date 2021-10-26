package com.wolt.osm.spark.OsmSource

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkFiles
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.io.File
import java.util
import scala.collection.JavaConverters.setAsJavaSetConverter

class OsmTable() extends Table with SupportsRead{

  override def capabilities: java.util.Set[TableCapability] = Set(TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap) : ScanBuilder = {
    val path = options.get("path")
    val useLocal = "true".equalsIgnoreCase(options.get("useLocalFile"))

    val spark = SparkSession.active
    val hadoop = spark.sessionState.newHadoopConf()
    val hadoopConfiguration = new SerializableHadoopConfigration(hadoop)

    if (useLocal) {
      if (!new File(SparkFiles.get(path)).canRead) {
        throw new RuntimeException(s"Input unavailable: $path")
      }
    } else {
      val source = new Path(path)
      val fs = source.getFileSystem(hadoop)
      if (!fs.exists(source)) {
        throw new RuntimeException(s"Input unavailable: $path")
      }
    }
    new OsmScanBuilder(path, hadoopConfiguration, options.computeIfAbsent("partitions",key => "1"), options.computeIfAbsent("threads",key=>"1"), useLocal)
  }

  override def name(): String = OsmSource.OSM_SOURCE_NAME

  override def schema(): StructType = OsmSource.schema
}
