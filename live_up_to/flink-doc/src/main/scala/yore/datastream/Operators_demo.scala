package yore.datastream

import org.apache.flink.streaming.api.scala._

import scala.collection.mutable.ListBuffer

/**
  * sink 输出格式
  *   并行id> result
  *
  * Created by yore on 2019/3/28 10:36
  */
object Operators_demo extends App {

  val TEXT : List[String] = List(
    "Be all my sins remember d .",
    "Whether 'tis nobler in the mind to suffer",
  "to be or not to be to  "
  )

  val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
  env.enableCheckpointing(5000)
  env.setParallelism(2)

  val dataStream: DataStream[Int] = env.fromCollection(Array(0, 33, 30, 31))
  val lineStream = env.fromElements(TEXT : _*)
  val pairStream = dataStream.map(n => (n.toInt % 2, n))

  /** ========== Transformations ========== */

  /**
    * 1. Map
    *   DataStream → DataStream
    *   采用一个元素并生成一个元素。一个将输入流的值加倍的map函数：
    *   `dataStream.map { x => x * 2 }`
    */
  dataStream.map(n => n * 2)
//    .print()

  /**
    * 2. FlatMap
    *   DataStream → DataStream
    *   采用一个元素并生成零个、一个、或多个元素。将句子分割为单词的flatmap函数：
    *   {{{
    *     dataStream.flatMap { str => str.split(" ") }
    *   }}}
    */
  lineStream.flatMap(line => line.split(" "))
//      .print()

  /**
    * 3. Filter
    *   DataStream → DataStream
    *   对每个元素计算一个布尔的函数，并保存函数返回true的元素。过滤掉零值的过滤器：
    *   {{{
    *     dataStream.filter { _ != 0 }
    *   }}}
    */
  dataStream.filter(_ != 0)
//      .print()

  /**
    * 4. KeyBy
    *   DataStream → KeyedStream
    *   逻辑上将流分区为不相交的分区，每个分区含有相同key的元素。在内部 keyBy 是使用 hash 分区实现的。
    *   请参阅有关如何制定建的<a href="https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/api_concepts.html#specifying-keys">key</a>。
    *   此转换返回KeyedStream。
    * {{{
    *     dataStream.keyBy("someKey") // Key by field "someKey"
    *     dataStream.keyBy(0) // Key by the first element of a Tuple
    * }}}
    */
  val keyedStream = pairStream.keyBy(0)
//    keyedStream.print()

  /**
    * 5. Reduce
    *   KeyedStream → DataStream
    *   在一个 Keyed 化数据流上的“rolling” reduce。将当前元素与最后一个Reduce的值组合并发出新值。 用于创建部分和的流的reduce函数：
    *   {{{
    *     keyedStream.reduce { _ + _ }
    *   }}}
    */
  keyedStream.reduce((a, b) => (a._1, a._2+b._2))
//    .print()

  /**
    * 6. Fold
    *   KeyedStream → DataStream
    *   具有初始值的被 Keyed 化数据流上的“rolling” 折叠。将当前数据元与最后折叠的值组合并发出新值。
    *   折叠函数，当应用于序列（1,2,3,4,5）时，发出序列“start-1”，“start-1-2”，“start-1- 2-3”，. ..
    *   {{{
    *     val result: DataStream[String] =
    *        keyedStream.fold("start")((str, i) => { str + "-" + i })
    *   }}}
    */
  keyedStream.fold("start")((str, i) =>{str + "-" +i})
//      .print()

  /**
    * 7. Aggregations
    *   KeyedStream → DataStream
    *   在被 Keys 化数据流上滚动聚合。min 和 minBy 之间的差异是 min 返回最小值，而 minBy 返回该字段中具有最小值的数据元（max 和 maxBy 相同）。
    * {{{
    *   keyedStream.sum(0)    // 根据key，滚动返回给定处元素的和 (在初始值的状态上累加)
    *   keyedStream.sum("key")
    *   keyedStream.min(0)
    *   keyedStream.min("key")
    *   keyedStream.max(0)
    *   keyedStream.max("key")
    *   keyedStream.minBy(0)
    *   keyedStream.minBy("key")
    *   keyedStream.maxBy(0)
    *   keyedStream.maxBy("key")
    * }}}
    */
//  val keyedStream2 = lineStream.flatMap(line => line.split(" ")).map(a => (a, Array(1, a.charAt(0).toInt, a.length))).keyBy(0)
  val keyedStream2 = lineStream.map(line => line.split(" ").toSeq).map(arr => {
    val l = ListBuffer[(String, Int)]()
      for(a <- arr){
        val lineT = (a, a.length)
        l += lineT
      }
//    println("$ " + l)
    (arr.length, l)
  }).keyBy(0)

  keyedStream.sum(0)
//    .print()
  keyedStream2.min(0)
//    .print()
  keyedStream2.max(0)
//      .print()
  keyedStream2.minBy(0)
//    .print()
  keyedStream2.maxBy(0)
//    .print()


  /**
    * 8. Window
    *   KeyedStream → WindowedStream
    *   Windows可以定义在已经分区的 KeyedStream 上。Windows根据某些特征（例如，在最后5秒内到达的数据）对每个Keys中的数据进行分组。
    *   有关窗口的完整说明，请参见<a href="https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html">windows</a>。
    */
  import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
  import org.apache.flink.streaming.api.windowing.time.Time
  val windowedStream = pairStream.keyBy(0).window(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data

  /**
    * 9. WindowAll
    *   DataStream → AllWindowedStream
    *   Windows可以在常规DataStream上定义。Windows根据某些特征（例如，在最后5秒内到达的数据）对所有流事件进行分组。
    *   有关窗口的完整说明，请参见<a href="https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html">windows</a>。
    *
    *   警告：在许多情况下，这是非并行转换。所有记录将收集在windowAll 算子的一个任务中。
    *   {{{
    *     dataStream.windowAll(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data
    *   }}}
    */
  val allWindowedStream = pairStream.keyBy(0).windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))

  /**
    *
    */
//  windowedStream.apply { WindowFunction }

  // applying an AllWindowFunction on non-keyed window stream
//  allWindowedStream.apply { AllWindowFunction }



  /** ==========  ========== */

  env.execute("operators_dome")


}
