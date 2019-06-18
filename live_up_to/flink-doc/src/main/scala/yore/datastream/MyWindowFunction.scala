package yore.datastream

import java.text.SimpleDateFormat

import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/**
  * [In, out, key, Window]
  *
  * Created by yore on 2018/6/10 18:10
  */
class MyWindowFunction extends WindowFunction[MyEvent, (String, Int, String, String, String, String), String, TimeWindow]{

  override def apply(key: String, window: TimeWindow, input: Iterable[MyEvent], out: Collector[(String, Int, String,String,String,String)]): Unit = {
    val list = input.toList.sortBy(_.time)
    val sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    println(list)
    out.collect(
      key,
      input.size,
      sf.format(list.head.time),
      sf.format(list.last.time),
      sf.format(window.getStart),
      sf.format(window.getEnd)
    )
  }

}
