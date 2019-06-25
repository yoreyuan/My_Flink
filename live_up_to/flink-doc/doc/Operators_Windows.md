 Flink Streaming (DataStream API) Operators - Windows
 ===
 Application Development / [Streaming (DataStream API)](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/datastream_api.html) / [Operators](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/) / Windows
 
 # [Windows](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html)
 
 Windows是处理无限流的核心。Windows将流拆分为有限大小的“桶”，我们可以在其上应用计算。本文档重点介绍如何在Flink中执行窗口，以及程序员如何从其提供的函数中获益最大化。
 
 窗口Flink程序的一般结构如下所示。第一个片段指的是被Keys化的流，而第二个片段指的是非Keys化的流。
 正如你所见，唯一的区别是`keyBy(...)` 调用Keys化的流那么window(...)成为非Key化的数据流的windowAll(...)。这也将作为页面其余部分的路线图。
 
##### Keyed Windows
- stream
```
.keyBy(...)               <-  keyed versus non-keyed windows
 .window(...)              <-  required: "assigner"
[.trigger(...)]            <-  optional: "trigger" (else default trigger)
[.evictor(...)]            <-  optional: "evictor" (else no evictor)
[.allowedLateness(...)]    <-  optional: "lateness" (else zero)
[.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
 .reduce/aggregate/fold/apply()      <-  required: "function"
[.getSideOutput(...)]      <-  optional: "output tag"
```

##### Non-Keyed Windows
- stream
```
 .windowAll(...)           <-  required: "assigner"
[.trigger(...)]            <-  optional: "trigger" (else default trigger)
[.evictor(...)]            <-  optional: "evictor" (else no evictor)
[.allowedLateness(...)]    <-  optional: "lateness" (else zero)
[.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
 .reduce/aggregate/fold/apply()      <-  required: "function"
[.getSideOutput(...)]      <-  optional: "output tag"
``` 
       
在上面，方括号（[...]）中的命令是可选的。这表明Flink允许您以多种不同方式自定义窗口逻辑，以便最适合您的需求。 

# 目录 

- Window Lifecycle
- Keyed vs Non-Keyed Windows
- Window Assigners
    + Tumbling Windows
    + Sliding Windows
    + Session Windows
    + Global Windows
- Window Functions
    + ReduceFunction
    + AggregateFunction
    + FoldFunction
    + ProcessWindowFunction
    + ProcessWindowFunction with Incremental Aggregation
    + Using per-window state in ProcessWindowFunction
    + WindowFunction (Legacy)
- Triggers
    + Fire and Purge
    + Default Triggers of WindowAssigners
    + Built-in and Custom Triggers
- Evictors
- Allowed Lateness
    + Getting late data as a side output
    + Late elements considerations
- Working with window results
    + Interaction of watermarks and windows
    + Consecutive windowed operations
- Useful state size considerations
   
****

# Window 生命周期   
简而言之，只要应该属于此窗口的第一个数据元到达就会创建一个窗口，当时间（事件或处理时间）超过其结束时间戳加上用户指定的允许延迟时，
窗口将被完全删除（请参阅[允许的延迟](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#allowed-lateness)）。
Flink保证仅删除基于时间的窗口而不是其他类型，例如全局窗口（请参阅[窗口分配器](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-assigners)）。
例如，使用基于事件时间的窗口策略，每隔5分钟创建一个非重叠（或翻滚）的窗口并允许延迟1分钟，Flink将创建一个新窗口，
用于间隔12:00和12:05当具有落入此间隔的时间戳的第一个数据元到达时，并且当水印通过12:06时间戳时它将删除它。

此外，每个窗口都有 Trigger（参见 [Triggers](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#triggers)）
和一个函数（ProcessWindowFunction，ReduceFunction，AggregateFunction 或 FoldFunction）（见[Window Functions](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-functions)）
连接到它。该函数将包含要应用于窗口内容的计算，而 Trigger 指定的窗口被认为准备好应用该函数的条件。
触发策略可能类似于“当窗口中的数据元数量大于4”时，或“当水印通过窗口结束时”。触发器还可以决定在创建和删除之间的任何时间清除窗口的内容。
在这种情况下，清除仅指窗口中的数据元，而不是窗口元数据。这意味着仍然可以将新数据添加到该窗口。

除了上述内容之外，您还可以指定一个 Evictor（参见 [Evictors](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#evictors)），
它可以在触发器触发后以及应用函数之前 and/or 之后从窗口中删除数据元。

在下文中，我们将详细介绍上述每个组件。在转到可选部分之前，我们从上面代码段中的必需部分开始（请参阅[Keyed vs Non-Keyed Windows](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#keyed-vs-non-keyed-windows)，
[Window Assigner](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-assigner)和 
[Window Function)](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-function)）。


# 被 Keys 化与非被 Keys 化 Windows 对比
首先要说明的是你的流应该被 keys化 还是不 keys 化。必须在定义窗口之前完成此算子操作。使用 keyBy(...) 将你的无限流分成逻辑被 Key 化的数据流。
如果 keyBy(...) 未调用，则表示您的流不是被Keys化的。

对于被Key化的数据流，可以将传入事件的任何属性用作键（更多详细信息查看[here](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/api_concepts.html#specifying-keys)）。
具有被 Key 化的数据流将允许您的窗口计算由多个任务并行执行，因为每个逻辑被 Key 化的数据流可以独立于其余任务进行处理。引用相同Keys的所有数据元将被发送到**同一个并行任务(the same parallel task)**。

在非 Key 化的数据流的情况下，您的原始流将不会被拆分为多个逻辑流，并且所有窗口逻辑将由单个任务执行，即并行度为1。
 

# 窗口分配器（Window Assigners）
指定您的流是否已 kyes 化后，下一步是定义一个窗口分配器。窗口分配器定义如何将数据元分配给窗口。这是通过在 window(...)（对于被 Keys 化的流）
或 windowAll() （对于非被 Keys 化流）调用中指定所选择的 WindowAssigner 来完成的。

WindowAssigner 负责将每个传入元素分配给一个或多个窗口。Flink 带有预定义的窗口分配器用于最常见的用例，即翻滚窗口， 滑动窗口，会话窗口和全局窗口。
您还可以通过扩展 WindowAssigner 类来实现自定义窗口分配器。所有内置窗口分配器（全局窗口除外）都根据时间为窗口分配数据元，这可以是处理时间或事件时间。
请查看我们关于 [event time](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/event_time.html)的部分，了解处理时间和事件时间之间的差异以及时间戳和水印的生成方式。

基于时间的窗口具有开始时间戳（包括）和结束时间戳（不包括），它们一起描述窗口的大小。在代码中，Flink在使用基于时间的窗口时使用 TimeWindow，
该窗口具有查询开始和结束时间戳的方法 ，以及返回给定窗口的最大允许时间戳的附加方法 maxTimestamp()。

在下文中，我们将展示 Flink 的预定义窗口分配器如何工作以及如何在 DataStream 程序中使用它们。下图显示了每个分配者的工作情况。
紫色圆圈表示流的数据元，这些数据元由某个键（在这种情况下是用户1，用户2和用户3）划分。x轴显示时间的进度。

## 翻滚的Windows（Tumbling Windows）
翻滚窗口分配器将每个元素分配给指定窗口大小的窗口。翻滚窗口具有固定的尺寸，不重叠。例如，如果指定大小为5分钟的翻滚窗口，则将评估当前窗口，并且每五分钟将启动一个新窗口，如下图所示。
![tumbling-windows.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/tumbling-windows.svg)

以下代码段显示了如何使用翻滚窗口。
```
val input: DataStream[T] = ...

// tumbling event-time windows
input
    .keyBy(<key selector>)
    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
    .<windowed transformation>(<window function>)

// tumbling processing-time windows
input
    .keyBy(<key selector>)
    .window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
    .<windowed transformation>(<window function>)

// daily tumbling event-time windows offset by -8 hours.
input
    .keyBy(<key selector>)
    .window(TumblingEventTimeWindows.of(Time.days(1), Time.hours(-8)))
    .<windowed transformation>(<window function>)
```

可以使用Time.milliseconds(x)，Time.seconds(x)，Time.minutes(x)等指定时间间隔。

如上一个示例所示，翻滚窗口分配器还采用可选的偏移参数，可用于更改窗口的对齐方式。 例如，没有偏移，每小时翻滚窗口与时期对齐，
即你将获得 1:00:00.000 - 1:59:59.999, 2:00:00.000 - 2:59:59.999 等窗口。 如果你想改变它，你可以给出一个偏移量。 
如果偏移15分钟，你会得到 1:15:00.000 - 2:14:59.999, 2:15:00.000 - 3:14:59.999 等。
偏移的一个重要用例是调整窗口到时区 UTC-0以外的。 例如，在中国，您必须指定Time.hours（-8）的偏移量。

## Sliding Windows
滑动窗口分配器将元素分配给固定长度的窗口。 与翻滚窗口分配器类似，窗口大小由窗口大小参数配置。 附加的窗口滑动参数控制滑动窗口的启动频率。 
因此，如果滑动小于窗口大小，则滑动窗口会重叠。 在这种情况下，元素被分配给多个窗口。

例如，您可以将大小为10分钟的窗口滑动5分钟。 有了这个，你每隔5分钟就会得到一个窗口，其中包含过去10分钟内到达的事件，如下图所示。
![sliding-windows.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/sliding-windows.svg)

以下代码段显示了如何使用滑动窗口。
```
val input: DataStream[T] = ...

// sliding event-time windows
input
    .keyBy(<key selector>)
    .window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5)))
    .<windowed transformation>(<window function>)

// sliding processing-time windows
input
    .keyBy(<key selector>)
    .window(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(5)))
    .<windowed transformation>(<window function>)

// sliding processing-time windows offset by -8 hours
input
    .keyBy(<key selector>)
    .window(SlidingProcessingTimeWindows.of(Time.hours(12), Time.hours(1), Time.hours(-8)))
    .<windowed transformation>(<window function>)
```

可以使用Time.milliseconds(x)，Time.seconds(x)，Time.minutes(x)等指定时间间隔。
如上一个示例所示，滑动窗口分配器还采用可选的偏移参数，该参数可用于更改窗口的对齐方式。 例如，没有偏移每小时窗口滑动30分钟与时期对齐，
也就是说你会得到 1:00:00.000 - 1:59:59.999, 1:30:00.000 - 2:29:59.999 等窗口等等。 如果你想改变它，你可以给出一个偏移量。 
如果偏移15分钟，你会得到 1:15:00.000 - 2:14:59.999, 1:45:00.000 - 2:44:59.999 等。
偏移的一个重要用例是调整窗口到时区 UTC-0以外的。 例如，在中国，您必须指定Time.hours（-8）的偏移量。

## Session Windows
会话窗口分配器按活动会话对元素进行分组。 会话窗口不重叠，没有固定的开始和结束时间，与翻滚窗口和滑动窗口相反。 
相反，当会话窗口在一段时间内没有接收到元素时，即当发生不活动的间隙时，会话窗口关闭。 
会话窗口分配器可以配置静态会话间隙或会话间隙提取器功能，该功能定义不活动时间段的长度。 
当此期限到期时，当前会话将关闭，后续元素将分配给新的会话窗口。
![session-windows.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/session-windows.svg)

以下代码段显示了如何使用会话窗口。
```
val input: DataStream[T] = ...

// event-time session windows with static gap
input
    .keyBy(<key selector>)
    .window(EventTimeSessionWindows.withGap(Time.minutes(10)))
    .<windowed transformation>(<window function>)

// event-time session windows with dynamic gap
input
    .keyBy(<key selector>)
    .window(EventTimeSessionWindows.withDynamicGap(new SessionWindowTimeGapExtractor[String] {
      override def extract(element: String): Long = {
        // determine and return session gap
      }
    }))
    .<windowed transformation>(<window function>)

// processing-time session windows with static gap
input
    .keyBy(<key selector>)
    .window(ProcessingTimeSessionWindows.withGap(Time.minutes(10)))
    .<windowed transformation>(<window function>)


// processing-time session windows with dynamic gap
input
    .keyBy(<key selector>)
    .window(DynamicProcessingTimeSessionWindows.withDynamicGap(new SessionWindowTimeGapExtractor[String] {
      override def extract(element: String): Long = {
        // determine and return session gap
      }
    }))
    .<windowed transformation>(<window function>)
```

静态间隙可以使用Time.milliseconds(x)，Time.seconds(x)，Time.minutes(x)等来指定。  

通过实现SessionWindowTimeGapExtractor接口指定动态间隙。

**注意**
由于会话窗口没有固定的开始和结束，因此它们的评估方式与翻滚和滑动窗口不同。 在内部，会话窗口操作算子为每个到达的记录创建一个新窗口，
如果它们彼此之间的距离比定义的间隙更接近，则将窗口合并在一起。 为了可合并，会话窗口运算符需要合并
[触发器](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#triggers)和合并
[窗口函数](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-functions)，
例如ReduceFunction，AggregateFunction或ProcessWindowFunction（FoldFunction无法合并。）

## Global Windows
全局窗口分配器将具有相同键的所有元素分配给同一个全局窗口。 此窗口方案仅在您还指定自定义[触发器](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#triggers)
时才有用。 否则，将不执行任何计算，因为全局窗口没有我们可以处理聚合元素的自然结束。
![non-windowed.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/non-windowed.svg)

以下代码段显示了如何使用全局窗口。
```
val input: DataStream[T] = ...

input
    .keyBy(<key selector>)
    .window(GlobalWindows.create())
    .<windowed transformation>(<window function>)
```
 
#  Window Functions
定义窗口分配器后，我们需要指定要在每个窗口上执行的计算。 这是窗口函数的职责，窗口函数用于在系统确定窗口准备好进行处理后处理
每个（可能是keys化的）窗口的元素（请参阅Flink如何确定窗口准备就绪的[触发器](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#triggers)）。

窗口函数可以是`ReduceFunction`，`AggregateFunction`，`FoldFunction`或`ProcessWindowFunction`之一。 
前两个可以更有效地执行（参见[State Size](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#state%20size)部分），
因为Flink可以在每个窗口到达时递增地聚合它们的元素。 `ProcessWindowFunction`获取窗口中包含的所有元素的Iterable以及有关元素所属窗口的其他元信息。

使用`ProcessWindowFunction`的窗口转换不能像其他情况一样有效地执行，因为Flink必须在调用函数之前在内部缓冲窗口的所有元素。 
这可以通过将`ProcessWindowFunction`与`ReduceFunction`，`AggregateFunction`或`FoldFunction`结合使用来获得窗口元素的
增量聚合和`ProcessWindowFunction`接收的其他窗口元数据。 我们将查看每个变换的示例。

## ReduceFunction
ReduceFunction 指定如何组合输入中的两个元素以生成相同类型的输出元素。 Flink使用ReduceFunction逐步聚合窗口的元素。

可以像这样定义和使用ReduceFunction：
```
val input: DataStream[(String, Long)] = ...

input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .reduce { (v1, v2) => (v1._1, v1._2 + v2._2) }
```
上面的示例总结了窗口中所有元素的元组的第二个字段。

## AggregateFunction
AggregateFunction 是 ReduceFunction 的通用版本，有三种类型：输入类型（IN），累加器类型（ACC）和输出类型（OUT）。 
输入类型是输入流中元素的类型，AggregateFunction具有将一个输入元素添加到累加器的方法。 
该接口还具有用于创建初始累加器的方法，用于将两个累加器合并到一个累加器中以及用于从累加器提取输出（类型OUT）的方法。 我们将在下面的示例中看到它的工作原理。

与 ReduceFunction 相同，Flink将在窗口到达时递增地聚合窗口的输入元素。

可以像这样定义和使用AggregateFunction：
```
/**
 * The accumulator is used to keep a running sum and a count. The [getResult] method
 * computes the average.
 */
class AverageAggregate extends AggregateFunction[(String, Long), (Long, Long), Double] {
  override def createAccumulator() = (0L, 0L)

  override def add(value: (String, Long), accumulator: (Long, Long)) =
    (accumulator._1 + value._2, accumulator._2 + 1L)

  override def getResult(accumulator: (Long, Long)) = accumulator._1 / accumulator._2

  override def merge(a: (Long, Long), b: (Long, Long)) =
    (a._1 + b._1, a._2 + b._2)
}

val input: DataStream[(String, Long)] = ...

input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .aggregate(new AverageAggregate)
```
上面的示例计算窗口中元素的第二个字段的平均值。

## FoldFunction
FoldFunction 指定窗口的输入元素如何与输出类型的元素组合。 对于添加到窗口的每个元素和当前输出值，将逐步调用FoldFunction。 第一个元素与输出类型的预定义初始值组合。

可以像这样定义和使用FoldFunction：
```
val input: DataStream[(String, Long)] = ...

input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .fold("") { (acc, v) => acc + v._2 }
```
上面的示例将所有输入Long值附加到最初为空的String。

**注意** fold()不能与`session windows`或其他`mergeable windows`一起使用。

## ProcessWindowFunction
ProcessWindowFunction 获取包含窗口所有元素的Iterable，以及可访问时间和状态信息的Context对象，这使其能够提供比其他窗口函数更多的灵活性。 
这是以性能和资源消耗为代价的，因为**元素不能以递增方式聚合**，而是需要在内部进行缓冲，直到认为窗口已准备好进行处理。

ProcessWindowFunction的签名如下：
```
abstract class ProcessWindowFunction[IN, OUT, KEY, W <: Window] extends Function {

  /**
    * Evaluates the window and outputs none or several elements.
    *
    * @param key      The key for which this window is evaluated.
    * @param context  The context in which the window is being evaluated.
    * @param elements The elements in the window being evaluated.
    * @param out      A collector for emitting elements.
    * @throws Exception The function may throw exceptions to fail the program and trigger recovery.
    */
  def process(
      key: KEY,
      context: Context,
      elements: Iterable[IN],
      out: Collector[OUT])

  /**
    * The context holding window metadata
    */
  abstract class Context {
    /**
      * Returns the window that is being evaluated.
      */
    def window: W

    /**
      * Returns the current processing time.
      */
    def currentProcessingTime: Long

    /**
      * Returns the current event-time watermark.
      */
    def currentWatermark: Long

    /**
      * State accessor for per-key and per-window state.
      */
    def windowState: KeyedStateStore

    /**
      * State accessor for per-key global state.
      */
    def globalState: KeyedStateStore
  }

}
```
**注意** 关键参数是通过为keyBy()调用指定的 KeySelector 提取的key。 在 tuple-index 键或字符串字段引用的情况下，此键类型始终为Tuple，
您必须手动将其转换为正确大小的元组以提取键字段。
       
可以像这样定义和使用ProcessWindowFunction：
```
val input: DataStream[(String, Long)] = ...

input
  .keyBy(_._1)
  .timeWindow(Time.minutes(5))
  .process(new MyProcessWindowFunction())

/* ... */

class MyProcessWindowFunction extends ProcessWindowFunction[(String, Long), String, String, TimeWindow] {

  def process(key: String, context: Context, input: Iterable[(String, Long)], out: Collector[String]): () = {
    var count = 0L
    for (in <- input) {
      count = count + 1
    }
    out.collect(s"Window ${context.window} count: $count")
  }
}
```
该示例显示了一个ProcessWindowFunction，用于计算窗口中的元素。 此外，窗口功能将有关窗口的信息添加到输出。

**注意** 请注意使用 ProcessWindowFunction 进行简单的聚合（例如count）效率非常低。 下一节将介绍如何将 ReduceFunction 或
AggregateFunction 与 ProcessWindowFunction 结合使用，以获取增量聚合和 ProcessWindowFunction 的添加信息。

## ProcessWindowFunction with Incremental Aggregation
ProcessWindowFunction 可以与 ReduceFunction，AggregateFunction 或 FoldFunction结合使用，以便在元素到达窗口时递增聚合元素。 
关闭窗口时，将为 ProcessWindowFunction 提供聚合结果。 这允许它在访问 ProcessWindowFunction 的附加窗口元信息的同时递增地计算窗口。

**注意** 您还可以使用旧版 WindowFunction 而不是 ProcessWindowFunction 进行增量窗口聚合。

### Incremental Window Aggregation with ReduceFunction
以下示例显示了如何将增量 ReduceFunction 与 ProcessWindowFunction 结合以返回窗口中的最小事件以及窗口的开始时间。
```
val input: DataStream[SensorReading] = ...

input
  .keyBy(<key selector>)
  .timeWindow(<duration>)
  .reduce(
    (r1: SensorReading, r2: SensorReading) => { if (r1.value > r2.value) r2 else r1 },
    ( key: String,
      context: ProcessWindowFunction[_, _, _, TimeWindow]#Context,
      minReadings: Iterable[SensorReading],
      out: Collector[(Long, SensorReading)] ) =>
      {
        val min = minReadings.iterator.next()
        out.collect((context.window.getStart, min))
      }
  )

```

### Incremental Window Aggregation with AggregateFunction
以下示例显示如何将增量 AggregateFunction 与 ProcessWindowFunction 结合以计算平均值，并同时发出键和窗口以及平均值。
```
val input: DataStream[(String, Long)] = ...

input
  .keyBy(<key selector>)
  .timeWindow(<duration>)
  .aggregate(new AverageAggregate(), new MyProcessWindowFunction())

// Function definitions

/**
 * The accumulator is used to keep a running sum and a count. The [getResult] method
 * computes the average.
 */
class AverageAggregate extends AggregateFunction[(String, Long), (Long, Long), Double] {
  override def createAccumulator() = (0L, 0L)

  override def add(value: (String, Long), accumulator: (Long, Long)) =
    (accumulator._1 + value._2, accumulator._2 + 1L)

  override def getResult(accumulator: (Long, Long)) = accumulator._1 / accumulator._2

  override def merge(a: (Long, Long), b: (Long, Long)) =
    (a._1 + b._1, a._2 + b._2)
}

class MyProcessWindowFunction extends ProcessWindowFunction[Double, (String, Double), String, TimeWindow] {

  def process(key: String, context: Context, averages: Iterable[Double], out: Collector[(String, Double)]): () = {
    val average = averages.iterator.next()
    out.collect((key, average))
  }
}
```

### Incremental Window Aggregation with FoldFunction
以下示例显示如何将增量 FoldFunction 与 ProcessWindowFunction 结合以提取窗口中的事件数并返回窗口的键和结束时间。
```
val input: DataStream[SensorReading] = ...

input
 .keyBy(<key selector>)
 .timeWindow(<duration>)
 .fold (
    ("", 0L, 0),
    (acc: (String, Long, Int), r: SensorReading) => { ("", 0L, acc._3 + 1) },
    ( key: String,
      window: TimeWindow,
      counts: Iterable[(String, Long, Int)],
      out: Collector[(String, Long, Int)] ) =>
      {
        val count = counts.iterator.next()
        out.collect((key, window.getEnd, count._3))
      }
  )
```

## Using per-window state in ProcessWindowFunction
除了访问kes化状态（任何富函数可以）之外，ProcessWindowFunction 还可以使用kes化状态，该kes化状态的作用域是函数当前正在处理的窗口。 
在这种情况下，了解每个窗口状态所指的窗口是很重要的。 涉及不同的“窗口”：

* 指定窗口操作时定义的窗口：这可能是1小时的翻滚窗口或一小时滑动的2小时长度的滑动窗口。
* 给定键的已定义窗口的实际实例：对于user-id xyz，这可能是从12:00到13:00的时间窗口。 这基于窗口定义，
并且将基于作业当前正在处理的键的数量以及基于事件落入的时隙而存在许多窗口。

每窗口状态与最近两者相关联。 这意味着如果我们处理1000个不同键的事件，并且所有这些事件的事件当前都落入\[12：00,13：00]时间窗口，
那么将有1000个窗口实例，每个窗口实例都有自己的Keys化的每窗口状态。

在Context对象上有两个方法，process()调用接收它们允许访问两种类型的状态：
* globalState(), 允许访问未限定为窗口的键控状态
* windowState(), 它允许访问也限定在窗口范围内的键控状态

如果您预计同一窗口会发生多次触发，则此功能非常有用，如果您迟到的数据或者您有自定义触发器进行推测性早期触发时可能会发生这种情况。 
在这种情况下，您将存储有关先前 firing 的信息或每个窗口状态的 firing 次数。

使用窗口状态时，清除窗口时清除该状态也很重要。 这应该在clear（）方法中发生。

## WindowFunction (Legacy)
在某些可以使用 ProcessWindowFunction 的地方，您也可以使用 WindowFunction。 这是 ProcessWindowFunction 的旧版本，
它提供较少的上下文信息，并且没有一些高级功能，例如 per-window 键控状态。 **此接口将在某个时候弃用**。

WindowFunction的签名如下所示：
```
trait WindowFunction[IN, OUT, KEY, W <: Window] extends Function with Serializable {

  /**
    * Evaluates the window and outputs none or several elements.
    *
    * @param key    The key for which this window is evaluated.
    * @param window The window that is being evaluated.
    * @param input  The elements in the window being evaluated.
    * @param out    A collector for emitting elements.
    * @throws Exception The function may throw exceptions to fail the program and trigger recovery.
    */
  def apply(key: KEY, window: W, input: Iterable[IN], out: Collector[OUT])
}
```
可以像如下使用
```
val input: DataStream[(String, Long)] = ...

input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .apply(new MyWindowFunction())
```

# Triggers
触发器确定何时一个窗(由窗口分配器形成)是被`window函数`准备好处理的。 每个`WindowAssigner`都带有一个默认触发器。 
如果默认触发器不符合您的需要，您可以使用`trigger(...)`指定自定义触发器。

触发器接口有五种方法允许触发器对不同的事件做出反应：
* 为添加到窗口的每个元素调用`onElement()`方法。
* 当注册的 event-time 计时器触发时，将调用`onEventTime() `方法。
* 当注册的 processing-time 计时器触发时，将调用`onProcessingTime()`方法。
* `onMerge()`方法与有状态触发器相关，并在它们相应的窗口合并时合并两个触发器的状态，例如： 使用会话窗口时。
* 最后`clear() `方法执行删除相应窗口时所需的任何操作。

关于上述方法需要注意两点：
1）前三个解决如何通过返回TriggerResult来对其调用事件进行操作。 该操作可以是以下之一：
    - CONTINUE：什么都不做，
    - FIRE：触发计算，
    - PURGE：清除窗口中的元素，和
    - FIRE_AND_PURGE：触发计算并在之后清除窗口中的元素。
2）这些方法中的任何一种都可用于为将来的操作注册处理或event-time计时器。

## Fire and Purge
一旦触发器确定window已准备好进行处理，它就会触发，即返回 FIRE 或 FIRE_AND_PURGE。 这是窗口 operator 发出当前窗口结果的信号。 
给定一个带有`ProcessWindowFunction`的窗口，所有元素都传递给ProcessWindowFunction（可能之后将他们交给一个evictor）。 
具有ReduceFunction，AggregateFunction或FoldFunction的Windows只会急切地发出聚合的结果。

当触发器触发时，它可以是 FIRE 或 FIRE_AND_PURGE。 当 FIRE 保留窗口内容时，FIRE_AND_PURGE会删除其内容。 
默认情况下，预先实现的触发器只需FIRE而不会清除窗口状态。

**注意** 清除将简单地删除窗口的内容，并将保留有关窗口和任何触发状态的任何潜在元信息。

## Default Triggers of WindowAssigners
WindowAssigner 的默认触发器适用于许多用例。 例如，所有事件时窗口分配器都将EventTimeTrigger作为默认触发器。 
一旦 watermark 通过窗口的末端，该触发器就会触发。

**注意** GlobalWindow的默认触发器是NeverTrigger，它永远不会触发。 因此，在使用GlobalWindow时，您始终必须定义自定义触发器。

**注意** 通过使用`trigger()` 指定触发器，您将覆盖WindowAssigner的默认触发器。 例如，如果为TumblingEventTimeWindows指定CountTrigger，
则不会再根据时间进度获取窗口，而只能按count计数。 现在，如果你想根据时间和数量做出反应，你必须编写自己的自定义触发器。

## Built-in and Custom Triggers
Flink附带了一些内置触发器。
    * EventTimeTrigger（已经提到过）根据watermark 测量的事件时间进度触发。
    * ProcessingTimeTrigger根据处理时间触发。
    * 一旦窗口中的元素数量超过给定限制，CountTrigger就会触发。
    * PurgingTrigger将另一个触发器作为参数，并将其转换为清除触发器。
如果需要实现自定义触发器，则应该检查抽象[Trigger](https://github.com/apache/flink/blob/master//flink-streaming-java/src/main/java/org/apache/flink/streaming/api/windowing/triggers/Trigger.java)类。
请注意，API仍在不断发展，可能会在Flink的未来版本中发生变化。


# Evictors（逐出器）
除了WindowAssigner和Trigger之外，Flink的窗口模型还允许指定可选的Evictor。 这可以使用`evictor(...) `方法完成（在本文档的开头显示）。 
`Evictors`可以在触发器触发后以及在应用窗口函数之前 and/or 之后从窗口中移除元素。 为此，Evictor接口有两种方法：
```
/**
 * 可选地 evicts 元素. windowing function后调用.
 *
 * @param elements 窗格中当前的元素.
 * @param size 窗格中当前的元素数.
 * @param window The {@link Window}
 * @param evictorContext Evictor的上下文
 */
void evictBefore(Iterable<TimestampedValue<T>> elements, int size, W window, EvictorContext evictorContext);

/**
 * 可选地 evicts 元素. windowing function后调用.
 *
 * @param elements 窗格中当前的元素.
 * @param size 窗格中当前的元素数.
 * @param window The {@link Window}
 * @param evictorContext Evictor的上下文
 */
void evictAfter(Iterable<TimestampedValue<T>> elements, int size, W window, EvictorContext evictorContext);

```

`evictBefore()`包含要在窗口函数之前应用的eviction逻辑，而`evictAfter()`包含要在窗口函数之后应用的逻辑。 
在应用窗口函数之前被逐出的元素将不会被它处理。

Flink附带三个预先实施的evictors。 这些是：
    * CountEvictor：从窗口保持用户指定数量的元素，并从窗口缓冲区的开头丢弃剩余的元素。
    * DeltaEvictor：采用DeltaFunction和阈值，计算窗口缓冲区中最后一个元素与其余每个元素之间的差值，并删除delta大于或等于阈值的值。
    * TimeEvictor：将参数作为一个间隔（以毫秒为单位），对于给定的窗口，它查找其元素中的最大时间戳max_ts，并删除时间戳小于`max_ts - interval`的所有元素。

**默认** 情况下，所有预先实现的`evictors`在窗口函数之前应用它们的逻辑。

**注意** 指定`Evictors`会阻止任何预聚合，因为在应用计算之前，必须将窗口的所有元素传递给`Evictors`。

**注意** Flink不保证窗口内元素的顺序。 这意味着尽管`Evictors`可以从窗口的开头移除元素，但这些元素不一定是首先到达或最后到达的元素。

# Allowed Lateness
当正处理 event-time 窗口时，可能会发生元素迟到的情况，即 Flink 用于跟踪事件时间进度的 watermark 已经超过元素所属的窗口的结束时间戳。 
查看[event time](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/event_time.html)，
特别是[late elements](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/event_time.html#late-elements)，
以便更全面地讨论Flink如何处理活动时间。

默认情况下，当 watermark 超过窗口末尾时，会删除延迟元素。 但是，Flink允许为窗口运算符指定最大允许延迟。 
允许延迟指定元素在被删除之前可以延迟多少时间，并且其默认值为0。在水印已经过了窗口结束但在它通过窗口结束加上允许的延迟之后到达的元素， 
仍然被添加到窗口中。 根据所使用的触发器，延迟但未丢弃的元素可能会导致窗口再次触发。 EventTimeTrigger就是这种情况。

为了使这项工作，Flink保持窗口的状态，直到他们允许的延迟到期。 一旦发生这种情况，Flink将删除窗口并删除其状态，如[Window Lifecycle](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-lifecycle)部分中所述。

**默认** 情况下，允许的延迟设置为0。也就是说，到达水印后面的元素将被删除。

您可以指定允许的延迟，如下所示：
```
val input: DataStream[T] = ...

input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .allowedLateness(<time>)
    .<windowed transformation>(<window function>)
```

**注意**当使用GlobalWindows窗口分配器时，没有数据被认为是迟到的，因为全局窗口的结束时间戳是`Long.MAX_VALUE`。

## Getting late data as a side output
使用Flink的[side output](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/side_output.html)功能，您可以获得最近丢弃的数据流。

首先需要在窗口化流上使用`sideOutputLateData(OutputTag)`指定要获取延迟数据。 然后，您可以在窗口操作的结果上获取侧输出流：
```
val lateOutputTag = OutputTag[T]("late-data")

val input: DataStream[T] = ...

val result = input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .allowedLateness(<time>)
    .sideOutputLateData(lateOutputTag)
    .<windowed transformation>(<window function>)

val lateStream = result.getSideOutput(lateOutputTag)
```

## Late elements considerations
当指定允许的延迟大于0时，在水印通过窗口结束后保留窗口及其内容。 在这些情况下，当一个迟到但未落下的元素到达时，它可能触发另一个窗口的发出。 
这些发出被称为后期射击，因为它们是由迟到事件触发的，与主要射击相反，后者是窗口的第一次发射。 在会话窗口的情况下，后期发出可以进一步导致窗口的合并，
因为它们可以“桥接”两个预先存在的未合并窗口之间的间隙。

**注意** 您应该知道，后期触发发出的元素应被视为先前计算的更新结果，即您的数据流将包含同一计算的多个结果。 根据您的应用程序，
您需要考虑这些重复的结果或对其进行重复数据删除。


# Working with window results
operations 保留在结果元素中，因此如果要保留有关窗口的元信息，则必须在`ProcessWindowFunction`的结果元素中手动编码该信息。 
在结果元素上设置的唯一相关信息是元素时间戳。 这被设置为已处理窗口的最大允许时间戳，即结束时间戳-1，因为窗口结束时间戳是独占的。 请注意，
事件时间窗口和处理时间窗口都是如此。 即在窗口化操作元素之后总是具有时间戳，但是这可以是event-time时间戳或processing-time时间戳。 
对于processing-time窗口，这没有特别的含义，但对于事件时间窗口，这与watermark与窗口交互的方式一起使得能够以相同的窗口大小进行[连续的窗口操作](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#consecutive-windowed-operations)。 
在看了 watermark 如何与窗口交互后，我们将介绍这一点。

## Interaction of watermarks and windows
在继续本节之前，您可能需要查看有关[event time and watermarks](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/event_time.html)的部分。

当watermark到达窗口操作符时，会触发两件事：
    * watermark触发计算所有窗口，其中最大时间戳（即结束时间戳-1）小于新watermark
    * watermark被转发（按原样）到下游操作

直观地，watermark “刷出”任何窗口，一旦接收到该watermark，将在下游操作中被认为是迟到的。

## Consecutive windowed operations
如前所述，计算窗口结果的时间戳的方式以及watermark与窗口交互的方式允许将连续的窗口操作串联在一起。 当您想要执行两个连续的窗口操作时，
这可能很有用，您希望使用不同的键，但仍希望来自同一上游窗口的元素最终位于同一下游窗口中。 考虑这个例子：
```
val input: DataStream[Int] = ...

val resultsPerKey = input
    .keyBy(<key selector>)
    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
    .reduce(new Summer())

val globalResults = resultsPerKey
    .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
    .process(new TopKWindowFunction())
```

在该示例中，来自第一操作的时间窗口\[0,5]的结果也将在随后的窗口化operation中的时间窗口\[0,5]中结束。 这允许计算每个键的和，
然后在第二个operation中计算同一窗口内的前k个元素。

# Useful state size considerations
Windows可以在很长一段时间内（例如几天，几周或几个月）定义，因此可以累积非常大的状态。 在估算窗口计算的存储要求时，需要记住几条规则：

1. Flink为每个窗口创建一个每个元素的副本。 鉴于此，翻滚窗口保留每个元素的一个副本（一个元素恰好属于一个窗口，除非它被延迟）。 相反，
滑动窗口会创建每个元素的几个，如[Window Assigners ](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#window-assigners)
部分中所述。 因此，尺寸为1天且滑动1秒的滑动窗口可能不是一个好主意。
2. ReduceFunction，AggregateFunction和FoldFunction可以显着降低存储要求，因为它们急切地聚合元素并且每个窗口只存储一个值。 
相反，只需使用ProcessWindowFunction就需要累积所有元素。
3. 使用Evictor可以防止 pre-aggregation，因为在应用计算之前，窗口的所有元素都必须通过逐出器传递（参见[Evictors](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/operators/windows.html#evictors)）。


<br/><br/><br/>
- - - -
<br/><br/>

Window 与 Time
----
源码 [streaming-olap/training](https://github.com/streaming-olap/training)

# 1 Window 概念理解
Window是一种切割无线数据为有限块进行处理的手段。

## 1.1 CountWindow

## 1.2 TimeWindow
### 1.2.1 Tumbling Window
翻滚窗口：将数据依据固定的窗口长度对数据进行切片。

特点：  
* 时间对齐
* 窗口长度固定
* 没有重叠

使用场景：适合做BI统计等（做每个时间段的聚合计算）

### 1.2.2 Sliding Window
滑动窗口：是固定窗口的更广泛的一种形式。由固定的窗口长度和滑动间隔组成。

特点：
* 时间对齐
* 窗口长度固定
* 有重叠

使用场景：连续统计最近一段时间的数据。例如，对最近一个时间段内的统计（求某接口最近5min的失败率来决定是否要报警）

### 1.2.3 Session Window
会话窗口：有一系列事件组合一个指定时间长度的timeout间隔组成，类似于web应用的session。也就是一段时间内没有接受到新数据就会昌盛新的窗口。

特定：
* 时间无对齐
* 适用于线上用户行为分析

## 1.3 Window api 定义
```
.keyBy(...)               <-  keyed versus non-keyed windows
 .window(...)              <-  required: "assigner"
[.trigger(...)]            <-  optional: "trigger" (else default trigger)
[.evictor(...)]            <-  optional: "evictor" (else no evictor)
[.allowedLateness(...)]    <-  optional: "lateness" (else zero)
[.sideOutputLateData(...)] <-  optional: "output tag" (else no side output for late data)
 .reduce/aggregate/fold/apply()      <-  required: "function"
[.getSideOutput(...)]      <-  optional: "output tag"
```

预定义的Keys化Window
```
# Tumbling time window
    .timeWindow(Time.seconds(30))
# Sliding time window
    .timeWindow(Time.seconds(30), Time.seconds(10))
# Tumbling count window
    .countWindow(1000)
# Sliding count window
    .countWindow(1000, 10)
# Session window
    .window(SessionWindows.withGap(Time.minutes(10)))

```

## 1.4 Window 聚合分类
### 1.4.1 全量聚合
等属于窗格的数据到齐，才开始进行聚合计算  

```
apply(windowFunction)

#  (1.3 新加的)
process(processWindowFunction)
```


### 1.4.2 增量聚合
窗格每进入一条数据，就进行一次计算

```
reduce(reduceFunction)
fold
aggregate(aggregateFunction)
sum(key), min(key), max(key)
sumBy(key), minBy(key), maxBy(key) (区别不带by)
```


# 2 时间语义理解

## 2.1 Time
Time类型：{@link org.apache.flink.streaming.api.TimeCharacteristic}

设置Time类型： `env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)`

## 2.2 Watermark
用来处理 eventtime 中数据的乱序问题。是Eventtime处理进度的标志。
通常又是结合window来实现。

* Timestamp assigner
* Wartermark generator

生成 Watermark 主要有如下两大类
* With `Periodic` Watermarks。指定一个允许最大的乱序时间
* With `Punctuated` Watermarks

### 2.2.1 Periodic Wartermarks (定期的)
基于 Timer  
ExecutionConfig.setAutoWatermarkInterval(msec) (默认是 100ms, 设置watermarker 发送的周期)  
实现 `AssignerWithPeriodicWatermarks[]` 接口  

### 2.2.2 Punctuated Wartermarks (依据标记的)
基于某些事件触发watermark 的生成和发送(由用户代码实现)  
实现 `AssignerWithPunctuatedWatermarks[]` 接口  

### 2.2.3 Watermark 的理解
如下图，事件到达Flink是一个乱序的，图中的数字表示时间戳。

![stream_watermark_out_of_order](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/stream_watermark_out_of_order.svg)

从图中可以看到时间戳为9的事件后到达了，当处理时按照事件发生的时间处理，这里显然就有问题了。
为了保证基于事件事件在处理实时的数据还是重新处理历史的数据时都能保证结果一致，需要一些额外的处理，这时就需要用到  Watermark 了。

例如在处理上面数据时，第一个为7，地二个是11，这时就输出结果吗，这个不好判断了，当数据时乱序到达，可能有些事件会晚到达，
谁都不确定事件7和事件11中间的事件是否存在，是否已经全部到达，或者什么时候到达。那么我们只能等待，等待就会有缓存，然后必然就会产生延迟。
那么等待多久，如果没有限制，那么有可能存在一直等下去，这样程序一直不敢输出结果了，
Watermark 正是限定这个等待时间的，表示早于这个时间的所有事件已全部到达，可以将计算结果输出了。



## 2.3 延迟数据处理
1. allowedLateness()
2. sideOutputTag

第一种：允许接收的最大延迟时间，延缓窗口内置状态清理时间。
第二种：延迟数据获取的一种方式，这样就可以不丢弃数据

```
val lateOutputTag = new OutputTag[T]("late-data")
val input = ……
val result = input
    .keyBy(<key selector>)
    .window(<window assigner>)
    .allowedLateness(<time>)

val lateStream = result.getSideOutput(lateOutputTag)

```

# 3 Window 机制内部实现源码解析
组建：
* Trigger
* windowAssigner
* internalTimeService 内部时间服务器
* windowState

## 3.1 StreamRecod
* 设置该 operator 的key为当前元素
* 根据element所携带的时间戳分配元素所属的窗口，一个元素可能会隶属于多个窗口
* 如果窗口是一个可merge的，就会进行和原有窗口进行合并和状态的更新。



# 4 生产环境中 window 使用容易遇到的问题
* Watermark不更新：①数据源问题；②Partition问题(broacast, froward)
* Window出现数据倾斜--加盐打散
* 数据计算准确度监控——currentLowWaterMark
* 数据丢失率过高：①丢弃指标；②manOutOfOrder（数据准确度和延迟的权衡）；③慎用allowLatency
* 任务启动前几个触发窗口数据不完整




 