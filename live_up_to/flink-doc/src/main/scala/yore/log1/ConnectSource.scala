package yore.log1

import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}


/**
  * 自定义 sourceFunction
  *
  * Created by yore on 2018/6/9 12:29
  */
class ConnectSource extends ParallelSourceFunction[Record]{
  private val random: java.util.Random = new java.util.Random(100)
  private var isRunning: Boolean = true

  /**
    * 生产数据
    *
    * @param sourceContext
    */
  override def run(sourceContext: SourceFunction.SourceContext[Record]): Unit = {
    while(isRunning){
      for(i <- 0 until 4 ){
        val record: Record = Record(i.toString, i, System.currentTimeMillis(), random.nextInt()/10, "json string or other")
        sourceContext.collect(record)
      }
      Thread.sleep(1000)
    }
  }

  override def cancel(): Unit = {
    isRunning = false
  }

}
