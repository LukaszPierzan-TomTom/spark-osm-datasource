package com.wolt.osm.spark.OsmSource

import org.apache.spark.sql.connector.read.{Scan, ScanBuilder}

class OsmScanBuilder(input: String, hadoop: SerializableHadoopConfigration, partitions: String, threads: String, useLocal: Boolean) extends ScanBuilder{
  override def build(): Scan = new OsmScan(input,hadoop,partitions,threads,useLocal)
}
