package yore.log1

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala.OutputTag
import org.apache.flink.util.Collector

/**
  *
  * Created by yore on 2019/6/9 12:53
  */
class BizIDSplitStream(outPutTag: OutputTag[Record]) extends ProcessFunction[Record, Record]{
  override def processElement(
                               record: Record,
                               ctx: ProcessFunction[Record, Record]#Context,
                               out: Collector[Record]): Unit = {
    if("1".equals(record.bizID)){
      // 将数据发送到常规流中
      //out.collect(record)

      // 将数据发送到测输出中
      ctx.output(outPutTag, record)
    }else{
      // other operation
    }
  }
}
