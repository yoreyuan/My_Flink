package yore

import org.apache.flink.api.common.functions.FlatMapFunction
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
  *
  * Created by yore on 2019/8/3 14:51
  */
object ESSinkTest {


  /**
    * --numRecords 需要测试的记录数
    * --index es 的 index （类似于database）
    * --type es 的 type （类似于 table）
    *
    * @param args Array[String]
    */
  def main(args: Array[String]): Unit = {
    val parameterTool = ParameterTool.fromArgs(args)

    if (parameterTool.getNumberOfParameters < 3) {
      System.err.println("Missing parameters!\n" + "Usage: --numRecords <numRecords> --index <index> --type <type>")
      return
    }


    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(5000)

    val source: DataStream[(String, String)] = env.generateSequence(0, parameterTool.getRequired("numRecords").toInt -1)
      .flatMap(new FlatMapFunction[Long, (String, String)] {
        override def flatMap(value: Long, out: Collector[(String, String)]): Unit = {
          val key = value.toString
          val message = "message #" + value
          val out1 = (key, message + "update #1")
          val out2 = (key, message + "update #2")
          out.collect(out1)
          out.collect(out2)
        }
      })

    source.print()

    source.addSink(new ESSink(parameterTool.getRequired("index"), parameterTool.getRequired("type")))

    env.execute("Elasticsearch 6.x end to end sink test example")

  }


}
