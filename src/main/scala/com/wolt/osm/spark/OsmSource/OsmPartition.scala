package com.wolt.osm.spark.OsmSource

import org.apache.spark.sql.connector.read.InputPartition
import org.apache.spark.sql.types.StructType

case class OsmPartition(partition: Int) extends InputPartition
