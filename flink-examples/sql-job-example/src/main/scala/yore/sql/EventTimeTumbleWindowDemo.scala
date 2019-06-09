package yore.sql

import java.io.File
import java.lang.{Long => JLong}
import java.util
import java.util.Collections

import org.apache.flink.api.common.typeinfo.{TypeInformation, Types}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.sinks.{CsvTableSink, TableSink}
import org.apache.flink.table.sources.tsextractors.ExistingField
import org.apache.flink.table.sources.wmstrategies.PreserveWatermarks
import org.apache.flink.table.sources.{DefinedRowtimeAttributes, RowtimeAttributeDescriptor, StreamTableSource}
import org.apache.flink.types.Row

/**
  *
  * Created by yore on 2018/12/18 16:35
  */
object EventTimeTumbleWindowDemo {

  def main(args: Array[String]): Unit = {
    // Streaming 环境
    import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val tEnv = StreamTableEnvironment.create(env)

    // 设置EventTime
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //方便我们查出输出数据
    env.setParallelism(1)

    val sourceTableName = "mySource"
    // 创建自定义source数据结构
    val tableSource = new MyTableSource

    val sinkTableName = "csvSink"
    // 创建CSV sink 数据结构
    val tableSink = getCsvTableSink

    // 注册source
    tEnv.registerTableSource(sourceTableName, tableSource)
    // 注册sink
    tEnv.registerTableSink(sinkTableName, tableSink)

    val sql =
      "SELECT  " +
        "  region, " +
        "  TUMBLE_START(accessTime, INTERVAL '2' MINUTE) AS winStart," +
        "  TUMBLE_END(accessTime, INTERVAL '2' MINUTE) AS winEnd, COUNT(region) AS pv " +
        " FROM mySource " +
        " GROUP BY TUMBLE(accessTime, INTERVAL '2' MINUTE), region"

    tEnv.sqlQuery(sql).insertInto(sinkTableName)
    env.execute()

  }


  def getCsvTableSink: TableSink[Row] = {
    val tempFile = File.createTempFile("csv_sink_", "tem")
    println("Sink path : " + tempFile)
    if (tempFile.exists()) {
      tempFile.delete()
    }
    new CsvTableSink(tempFile.getAbsolutePath).configure(
      Array[String]("region", "winStart", "winEnd", "pv"),
      Array[TypeInformation[_]](Types.STRING, Types.SQL_TIMESTAMP, Types.SQL_TIMESTAMP, Types.LONG))
  }

}



import org.apache.flink.api.java.typeutils.RowTypeInfo
import org.apache.flink.table.api.TableSchema

/**
  * 特质：StreamTableSource
  *     定义外部流表并提供对其数据的读访问权。
  *
  * 特质：DefinedRowtimeAttributes
  *     Extends a TableSource to specify rowtime attributes via a RowtimeAttributeDescriptor
  */
class MyTableSource extends StreamTableSource[Row] with DefinedRowtimeAttributes {

  val fieldNames = Array("accessTime", "region", "userId")
  val schema = new TableSchema(fieldNames, Array(Types.SQL_TIMESTAMP, Types.STRING, Types.STRING))
  val rowType = new RowTypeInfo(
    Array(Types.LONG, Types.STRING, Types.STRING).asInstanceOf[Array[TypeInformation[_]]],
    fieldNames)

  // 页面访问表数据 rows with timestamps and watermarks
  val data = Seq(
    Left(1510365660000L, Row.of(new JLong(1510365660000L), "ShangHai", "U0010")),
    Right(1510365660000L),
    Left(1510365660000L, Row.of(new JLong(1510365660000L), "BeiJing", "U1001")),
    Right(1510365660000L),
    Left(1510366200000L, Row.of(new JLong(1510366200000L), "BeiJing", "U2032")),
    Right(1510366200000L),
    Left(1510366260000L, Row.of(new JLong(1510366260000L), "BeiJing", "U1100")),
    Right(1510366260000L),
    Left(1510373400000L, Row.of(new JLong(1510373400000L), "ShangHai", "U0011")),
    Right(1510373400000L)
  )

  /**
    * 对于所有的rowtime属性的表返回一个RowtimeAttributeDescriptor列表
    */
  override def getRowtimeAttributeDescriptors: util.List[RowtimeAttributeDescriptor] = {
    Collections.singletonList(new RowtimeAttributeDescriptor(
      "accessTime",
      new ExistingField("accessTime"),
      PreserveWatermarks.INSTANCE))
  }

  override def getDataStream(execEnv: StreamExecutionEnvironment): DataStream[Row] = {
//    import org.apache.flink.streaming.api.scala._
    execEnv.addSource(new MySourceFunction[Row](data)).setParallelism(1).returns(rowType)
  }

  override def getReturnType: TypeInformation[Row] = rowType

  override def getTableSchema: TableSchema = schema

}


/**
  *
  * Water mark 生成器
  *
  * 支持接收携带 EventTime 的数据集合，
  * Either 的数据结构 Right 是 WaterMark，Left 是元数据:
  *
  */
class MySourceFunction[T](dataWithTimestampList: Seq[Either[(Long, T), Long]]) extends SourceFunction[T] {

  override def run(ctx: SourceFunction.SourceContext[T]): Unit = {
    dataWithTimestampList.foreach{
      case Left(t) => ctx.collectWithTimestamp(t._2, t._1)
      case Right(w) => ctx.emitWatermark(new Watermark(w))
    }
  }

  override def cancel(): Unit = ???
}
