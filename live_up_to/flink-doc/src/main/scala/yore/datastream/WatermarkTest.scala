package yore.datastream


import java.text.SimpleDateFormat

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * <pre>
  *   开启socket: nc -lk 9999
  *
  *   输入:
  *       001,1555637580000
  *       001,1555637586000
  *       001,1555637590000
  *       001,1555637591000
  *       001,1555637592000
  *       001,1555637593000
  *       001,1555637594000
  *
  *   结果：
  *
  * </pre>
  * Created by yore on 2018/6/10 18:10
  */
object WatermarkTest {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)


    val input = env.socketTextStream(
      "localhost",
      9999
    )

    val inputMap = input
      .filter(org.apache.commons.lang3.StringUtils.isNotEmpty(_))
      .map(line =>{
        val arr = line.split("\\W+")
        // code, time
//        MyEvent(arr(0), arr(1).toLong)
        MyEvent(arr(0))
      })

    val window = inputMap.assignTimestampsAndWatermarks(new MyTimeWatermark)
      .keyBy(_.code)
      .window(TumblingEventTimeWindows.of(Time.seconds(3)))
      .apply(new MyWindowFunction())

    window.print()

    env.execute()

  }

}
