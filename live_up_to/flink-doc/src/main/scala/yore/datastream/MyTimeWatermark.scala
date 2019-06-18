package yore.datastream

import java.text.SimpleDateFormat

import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.watermark.Watermark

/**
  * 用于生成 Watermark ,处理的数据假设在某种程度上有时间上的乱序。
  *
  * 某个时间戳 t 的最新元素将会在时间戳t的最早元素之后最多 n 毫秒到达。
  *
  * Created by yore on 2018/6/10 17:48
  */
class MyTimeWatermark extends AssignerWithPeriodicWatermarks[MyEvent]{
  // 允许的最大乱序时间时10s
  val maxOutOfOrderness = 10000L
  var currentMaxTimestamp: Long = 0L

  val sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  /**
    * 会一直调用,当没有数据进入是，返回的watermark为 -10000
    * @return Watermark
    */
  override def getCurrentWatermark: Watermark = {
    // return the watermark as current highest timestamp minus the out-of-orderness bound
    new Watermark(currentMaxTimestamp - maxOutOfOrderness)
  }

  override def extractTimestamp(element: MyEvent, previousElementTimestamp: Long): Long = {
    val timestamp = element.time
    currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp)
    val formatCurr = sf.format(currentMaxTimestamp)
    // 原始数据 | 格式化后的时间，currentMaxTimestamp | 格式化后的currentMaxTimestamp，watermark
    println(s"$element | ${sf.format(element.time)}, $currentMaxTimestamp | $formatCurr, $getCurrentWatermark")
    timestamp
  }
}
