package yore.datastream

import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.DataStreamUtils
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * nc -lk 9999
  *
  * Created by yore on 2019/5/5 02:23
  */
object WindowWordCount {

  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // 本地调试可以使用：
//        StreamExecutionEnvironment.createLocalEnvironment()

    env.enableCheckpointing(1000)
    // set mode to exactly-once (this is the default)
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    // make sure 500 ms of progress happen between checkpoints
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(500)
    // checkpoints have to complete within one minute, or are discarded
    env.getCheckpointConfig.setCheckpointTimeout(60000)
    // prevent the tasks from failing if an error happens in their checkpointing, the checkpoint will just be declined.
    env.getCheckpointConfig.setFailOnCheckpointingErrors(false)
    // allow only one checkpoint to be in progress at the same time
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)

    // 设置缓冲数据发送超时时间. 默认为 100 毫秒
    env.setBufferTimeout(1000)

    /**
      * readTextFile(filePath:String)
      *     逐行读取文本文件，即符合规范的文件，并将它们作为字符串返回。
      * readFile(fileInputFormat, path)
      *     按指定的文件输入格式指定读取（一次）文件
      * readFile(fileInputFormat, path, watchType, interval, pathFilter)
      *
      * socketTextStream - 从套接字读取。元素可以用分隔符分隔。
      * fromCollection(Seq) - 从Java Java.util.Collection创建数据流。集合中的所有元素必须属于同一类型。
      * fromCollection(Iterator) - 从迭代器创建数据流。该类指定迭代器返回的元素的数据类型。
      * fromElements(elements: _*) - 从给定的对象序列创建数据流。所有对象必须属于同一类型。
      * fromParallelCollection(SplittableIterator) - 并行地从迭代器创建数据流。该类指定迭代器返回的元素的数据类型。
      * generateSequence(from, to) - 并行生成给定间隔中的数字序列。
      *
      * 自定义：
      *   addSource
      *       附加新的源功能。例如，要从Apache Kafka读取，您可以使用 addSource(new FlinkKafkaConsumer08<>(...)).
      *
      *
      */
    val text = env.socketTextStream("localhost", 9999)

    val counts = text.flatMap { _.toLowerCase.split("\\W+") filter { _.nonEmpty } }
      .map { (_, 1) }
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)

    counts.print()

    // Flink还提供了一个接收器来收集DataStream结果，以便进行测试和调试。它可以使用如下：
    import scala.collection.JavaConverters.asScalaIteratorConverter
    val myOutput: Iterator[String] = DataStreamUtils.collect(text.javaStream).asScala

    env.execute("Window Stream WordCount")

  }

}
