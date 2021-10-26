package com.wolt.osm.spark.OsmSource

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader, PartitionReaderFactory}
import org.apache.spark.sql.types.StructType

class OsmPartitionReaderFactory(input: String, schema: StructType, hadoop: SerializableHadoopConfigration,  threads: Int, partitionsNo: Int, useLocal: Boolean) extends PartitionReaderFactory {
  override def createReader(inputPartition: InputPartition): PartitionReader[InternalRow] = {
    new OsmPartitionReader(input, hadoop, schema, threads, partitionsNo, inputPartition.asInstanceOf[OsmPartition].partition, useLocal)
  }
}
