package com.wolt.osm.spark.OsmSource

import org.apache.spark.sql.connector.read.{Batch, Scan}
import org.apache.spark.sql.types.StructType

import scala.util.Try

class OsmScan(input: String, hadoop: SerializableHadoopConfigration, partitions: String, threads: String, useLocal: Boolean) extends Scan {
  override def readSchema(): StructType = OsmSource.schema

  override def toBatch: Batch = {
    val partitionsNo = Try(partitions.toInt).getOrElse(1)
    val threadsNo = Try(threads.toInt).getOrElse(1)
    new OsmBatch(input,readSchema(),hadoop,partitionsNo,threadsNo,useLocal)
  }
}
