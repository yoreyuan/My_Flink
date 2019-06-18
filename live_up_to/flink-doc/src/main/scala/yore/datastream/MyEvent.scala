package yore.datastream

import java.text.SimpleDateFormat

/**
  *
  * Created by yore on 2019/6/10 18:00
  */
case class MyEvent(
                  code: String,
                  var time: Long = System.currentTimeMillis()
                  ){
  val sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  time = sf.parse(sf.format(time)).getTime
}

