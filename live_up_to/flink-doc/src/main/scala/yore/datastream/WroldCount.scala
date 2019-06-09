package yore.datastream

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
  *
  * Created by yore on 2019/6/9 05:39
  */
object WroldCount {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val dataStream = env.socketTextStream("localhost", 9999)
      .flatMap(_.split(" "))
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)

    dataStream.print()

    env.execute("window word count")

  }

}
