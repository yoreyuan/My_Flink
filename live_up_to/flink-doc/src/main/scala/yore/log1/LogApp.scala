package yore.log1

import java.util.concurrent.TimeUnit

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.time.Time
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

/**
  *
  * Created by yore on 2019/6/9 12:53
  */
object LogApp {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(3000)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(1000)
    env.getCheckpointConfig.setCheckpointTimeout(10000)
    // 设置时间模型
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    // 固定间隔内失败了3次
//    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
//      3,// 尝试重启的次数
//      Time.of(5, TimeUnit.SECONDS) // 间隔
//    ));
    // 5分钟内若失败了3次则认为该job失败，重试间隔为10s
    env.setRestartStrategy(RestartStrategies.failureRateRestart(
      3,//一个时间段内的最大失败次数
      Time.of(5, TimeUnit.MINUTES), // 衡量失败次数的是时间段
      Time.of(10, TimeUnit.SECONDS) // 间隔
    ));

    val logDataStream: DataStream[Record] = env.addSource(new ConnectSource)
    val configDataStream: DataStream[String] = env.socketTextStream("localhost", 9999).broadcast

    val connectStreams: ConnectedStreams[Record, String] = logDataStream.connect(configDataStream)
    val flatMapDataStream = connectStreams.flatMap(new CoFlatMapFunction[Record, String, Record] {
      var config: String = _
      override def flatMap1(in1: Record, collector: Collector[Record]): Unit = {
        /*
         * 处理业务逻辑
         */
        config match {
          case "0" => collector.collect(in1)
          case "1" => collector.collect(in1)
        }
      }
      override def flatMap2(in2: String, collector: Collector[Record]): Unit = {
        /*
         * 处理配置
         */
        config = in2
      }
    })

    // side-output
    val outputTag = OutputTag[Record]("biz-id-1")
    val splitStream/*: SplitStream[String]*/ = logDataStream.process(new BizIDSplitStream(outputTag))

    splitStream.getSideOutput(outputTag)//.addSink()



  }

}

class BizIDSplitStream(outPutTag: OutputTag[Record]) extends ProcessFunction[Record, Record]{
  override def processElement(
                               record: Record,
                               ctx: ProcessFunction[Record, Record]#Context,
                               out: Collector[Record]): Unit = {
    if("1".equals(record.bizID)){
      // 将数据发送到常规流中
      //out.collect(record)

      // 将数据发送到测输出中
      ctx.output(outPutTag, Record)
    }else{
      // other operation
    }
  }
}
