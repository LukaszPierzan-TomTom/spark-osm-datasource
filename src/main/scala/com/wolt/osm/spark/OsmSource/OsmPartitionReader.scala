package com.wolt.osm.spark.OsmSource

import java.io.{File, FileInputStream, InputStream}
import java.util.concurrent._
import java.util.function.Consumer
import com.wolt.osm.parallelpbf.ParallelBinaryParser
import com.wolt.osm.parallelpbf.entity._
import org.apache.hadoop.fs.{FSDataInputStream, Path}
import org.apache.log4j.Logger
import org.apache.spark.SparkFiles
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util._
import org.apache.spark.sql.connector.read.PartitionReader
import org.apache.spark.sql.types.StructType
import org.apache.spark.unsafe.types.UTF8String

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}
import scala.collection.JavaConversions._
import scala.collection.mutable

class OsmPartitionReader(input: String, hadoop:SerializableHadoopConfigration, schema: StructType, threads: Int, partitionsNo: Int, partition: Int, useLocal: Boolean) extends PartitionReader[InternalRow] {
  private val schemaColumnNames = schema.fields.map(_.name)
  private val parserConfigured = new AtomicBoolean(false)
  private val log = Logger.getLogger("com.wolt.osm.spark.OsmSource.OsmPartitionReader")
  private val counter = new AtomicLong(0);

  private val parserTask = new FutureTask[Unit](new Callable[Unit]() {
    override def call: Unit = {
      log.info("Parse started.")
      val parser = new ParallelBinaryParser(inputStream, threads, partitionsNo, partition)
      parser.onNode(onNode)
      parser.onWay(onWay)
      parser.onRelation(onRelation)

      try {
        parser.parse()
      } catch {
        case e: Throwable =>
          log.error("Exception thrown while parsing input!", e)
      } finally {
        log.info(s"Parse ended reading ${counter.get()} entries.")
        inputStream.close()
      }

    }
  })
  private var parseThread: Thread = _
  private val queue = new SynchronousQueue[InternalRow]
  private var currentRow: InternalRow = _
  private var inputStream: InputStream = _

  private def getInputStream: InputStream = {
    if (useLocal) {
      val fname = SparkFiles.get(input)
      val file = new File(fname)
      if(file.exists()){
        log.error(s"File $fname has size ${file.length()}.")
      }else{
        log.error("File doesn't exists!!!")
      }
      new FileInputStream(fname)
    } else {
      val source = new Path(input)
      val hadoopConfigration = hadoop.get()
      val fs = source.getFileSystem(hadoopConfigration)
      fs.open(source,64*1024*1024)
    }
  }

  override def next(): Boolean = {
    if (parserConfigured.compareAndSet(false,true)) {
      inputStream = getInputStream
      parseThread = new Thread(parserTask)
      parseThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler {
        override def uncaughtException(t: Thread, e: Throwable): Unit = {
          log.error("Exception thrown during parse thread!",e)
          throw new RuntimeException(e);
        }
      });
      parseThread.start()
    }
    while (!(queue.isEmpty && parserTask.isDone)) {
      currentRow = queue.poll(1, TimeUnit.SECONDS)
      if (currentRow != null) {
        return true
      }
    }
    false
  }

  override def get(): InternalRow = currentRow

  override def close(): Unit = {
    inputStream.close()
    parserTask.cancel(true)
  }

  def makeTags(tags: java.util.Map[String, String]): MapData = {
    val stringifiedTags = tags.toMap.flatMap(kv => Map(UTF8String.fromString(kv._1.toLowerCase) -> UTF8String.fromString(kv._2)))
    ArrayBasedMapData(stringifiedTags)
  }

  def makeInfo(entity: OsmEntity): InternalRow = {
    if (entity.getInfo != null) {
      val info = entity.getInfo
      val username = if (info.getUsername != null) {
        UTF8String.fromString(info.getUsername)
      } else {
        null
      }
      InternalRow(info.getUid, username, info.getVersion, info.getTimestamp, info.getChangeset, info.isVisible)
    } else {
      null
    }
  }

  def makeRowPreamble(entity: OsmEntity): mutable.MutableList[Any] = {
    val content = mutable.MutableList[Any]()
    if (schemaColumnNames.exists(_.equalsIgnoreCase("ID"))) {
      content += entity.getId
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("TAG"))) {
      content += makeTags(entity.getTags)
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("INFO"))) {
      content += makeInfo(entity)
    }
    content
  }

  def callback[T <: OsmEntity](handler: T => mutable.MutableList[Any]): Consumer[T] = {
    new Consumer[T] {
      override def accept(t: T): Unit = {
        counter.incrementAndGet();
        val content = makeRowPreamble(t) ++= handler(t)
        val row = InternalRow.fromSeq(content)
        queue.put(row)
      }
    }
  }

  private val onNode = callback[Node](t => {
    val content = mutable.MutableList[Any]()
    if (schemaColumnNames.exists(_.equalsIgnoreCase("TYPE"))) {
      content += RelationMember.Type.NODE.ordinal()
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LAT"))) {
      content += t.getLat
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LON"))) {
      content += t.getLon
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("WAY"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("RELATION"))) {
      content += null
    }
    content
  })

  private val onWay = callback[Way](t => {
    val content = mutable.MutableList[Any]()
    if (schemaColumnNames.exists(_.equalsIgnoreCase("TYPE"))) {
      content += RelationMember.Type.WAY.ordinal()
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LAT"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LON"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("WAY"))) {
      content += ArrayData.toArrayData(t.getNodes.toArray)
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("RELATION"))) {
      content += null
    }
    content
  })

  private val onRelation = callback[Relation](t => {
    val content = mutable.MutableList[Any]()
    val members = t.getMembers.toSeq.map(member => {
      val role = if (member.getRole != null) {
        UTF8String.fromString(member.getRole)
      } else {
        null
      }
      InternalRow(member.getId, role, member.getType.ordinal())
    })

    if (schemaColumnNames.exists(_.equalsIgnoreCase("TYPE"))) {
      content += RelationMember.Type.RELATION.ordinal()
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LAT"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("LON"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("WAY"))) {
      content += null
    }
    if (schemaColumnNames.exists(_.equalsIgnoreCase("RELATION"))) {
      content += ArrayData.toArrayData(members)
    }
    content
  })
}
