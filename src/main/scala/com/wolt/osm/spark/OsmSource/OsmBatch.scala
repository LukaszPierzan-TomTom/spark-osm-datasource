package com.wolt.osm.spark.OsmSource


import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory}
import org.apache.spark.sql.types.StructType

class OsmBatch(input: String, schema: StructType, hadoop: SerializableHadoopConfigration, partitions: Int, threads: Int, useLocal: Boolean) extends Batch{

  override def planInputPartitions(): Array[InputPartition] = {
    val shiftedPartitions = partitions - 1
    (0 to shiftedPartitions).map(p => new OsmPartition(p)).toArray.asInstanceOf[Array[InputPartition]]
  }

  override def createReaderFactory(): PartitionReaderFactory = {
    new OsmPartitionReaderFactory(input,schema,hadoop,threads,partitions,useLocal)
  }
}
