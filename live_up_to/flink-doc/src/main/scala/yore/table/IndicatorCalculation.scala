package yore.table

import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.scala._

/**
  * nc -l 19999
  *
  *
  * Created by yore on 2019/4/10 10:07
  */
object IndicatorCalculation {

  private var host: String = "localhost"
  private var port: Int = 19999

  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    /**
      * 旧 TableEnvironment.getTableEnvironment(env)
      *
      * 创建一个 TableEnvironment。
      *   在内部目录注册一个表
      *   注册一个外部目录
      *   执行SQL查询
      *   注册用户自定义的函数（标量、表、或者聚合）
      *   转换 DataStream 或 DataSet 为一个表
      *   持有一个 ExecutionEnvironment 或 StreamExecutionEnvironment 的引用
      */
    val tableEnv = StreamTableEnvironment.create(env)
    tableEnv.registerFunction("last_day", new LastDay())

    val words = env.socketTextStream(host, port)
      .flatMap(_.split(" "))
      .map(Word(_, 1))



    tableEnv.registerDataStream("words", words)
    tableEnv.sqlQuery("select w,sum(c) from words group by w")
      .toRetractStream[Word]
      .filter(_._1)
      .map(_._2)
      .print()

    env.execute()



  }

  case class Word(w: String, c: Long)


}
