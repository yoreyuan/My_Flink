 Flink Streaming (DataStream API) Operators
 ===
 Application Development / [Streaming (DataStream API)](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/datastream_api.html) / [Operators](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/) / Windows
 
 # [Windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html)
 
 Windows是处理无限流的核心。Windows将流拆分为有限大小的“桶”，我们可以在其上应用计算。本文档重点介绍如何在Flink中执行窗口，以及程序员如何从其提供的函数中获益最大化。
 
 窗口Flink程序的一般结构如下所示。第一个片段指的是被Keys化的流，而第二个片段指的是非Keys化的流。
 正如你所见，唯一的区别是`keyBy(...)` 调用Keys化的流那么window(...)成为非Key化的数据流的windowAll(...)。这也将作为页面其余部分的路线图。
 
 * Keyed Windows
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

* Non-Keyed Windows
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

## 目录 

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

### Window 生命周期   
简而言之，只要应该属于此窗口的第一个数据元到达就会创建一个窗口，当时间（事件或处理时间）超过其结束时间戳加上用户指定的允许延迟时，
窗口将被完全删除（请参阅[允许的延迟](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#allowed-lateness)）。
Flink保证仅删除基于时间的窗口而不是其他类型，例如全局窗口（请参阅[窗口分配器](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#window-assigners)）。
例如，使用基于事件时间的窗口策略，每隔5分钟创建一个非重叠（或翻滚）的窗口并允许延迟1分钟，Flink将创建一个新窗口，
用于间隔12:00和12:05当具有落入此间隔的时间戳的第一个数据元到达时，并且当水印通过12:06时间戳时它将删除它。

此外，每个窗口都有 Trigger（参见 [Triggers](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#triggers)）
和一个函数（ProcessWindowFunction，ReduceFunction，AggregateFunction 或 FoldFunction）（见[Window Functions](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#window-functions)）
连接到它。该函数将包含要应用于窗口内容的计算，而 Trigger 指定的窗口被认为准备好应用该函数的条件。
触发策略可能类似于“当窗口中的数据元数量大于4”时，或“当水印通过窗口结束时”。触发器还可以决定在创建和删除之间的任何时间清除窗口的内容。
在这种情况下，清除仅指窗口中的数据元，而不是窗口元数据。这意味着仍然可以将新数据添加到该窗口。

除了上述内容之外，您还可以指定一个 Evictor（参见 [Evictors](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#evictors)），
它可以在触发器触发后以及应用函数之前 and/or 之后从窗口中删除数据元。

在下文中，我们将详细介绍上述每个组件。在转到可选部分之前，我们从上面代码段中的必需部分开始（请参阅[Keyed vs Non-Keyed Windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#keyed-vs-non-keyed-windows)，
[Window Assigner](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#window-assigner)和 
[Window Function)](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html#window-function)）。


### 被 Keys 化与非被 Keys 化 Windows 对比
首先要说明的是你的流应该被 keys化 还是不 keys 化。必须在定义窗口之前完成此算子操作。使用 keyBy(...) 将你的无限流分成逻辑被 Key 化的数据流。
如果 keyBy(...) 未调用，则表示您的流不是被Keys化的。

对于被Key化的数据流，可以将传入事件的任何属性用作键（更多详细信息查看[here](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/api_concepts.html#specifying-keys)）。
具有被 Key 化的数据流将允许您的窗口计算由多个任务并行执行，因为每个逻辑被 Key 化的数据流可以独立于其余任务进行处理。引用相同Keys的所有数据元将被发送到**同一个并行任务(the same parallel task)**。

在非 Key 化的数据流的情况下，您的原始流将不会被拆分为多个逻辑流，并且所有窗口逻辑将由单个任务执行，即并行度为1。
 

### 窗口分配器（Window Assigners）
指定您的流是否已 kyes 化后，下一步是定义一个窗口分配器。窗口分配器定义如何将数据元分配给窗口。这是通过在 window(...)（对于被 Keys 化的流）
或 windowAll() （对于非被 Keys 化流）调用中指定所选择的 WindowAssigner 来完成的。

WindowAssigner 负责将每个传入元素分配给一个或多个窗口。Flink 带有预定义的窗口分配器用于最常见的用例，即翻滚窗口， 滑动窗口，会话窗口和全局窗口。
您还可以通过扩展 WindowAssigner 类来实现自定义窗口分配器。所有内置窗口分配器（全局窗口除外）都根据时间为窗口分配数据元，这可以是处理时间或事件时间。
请查看我们关于 [event time](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/event_time.html)的部分，了解处理时间和事件时间之间的差异以及时间戳和水印的生成方式。

基于时间的窗口具有开始时间戳（包括）和结束时间戳（不包括），它们一起描述窗口的大小。在代码中，Flink在使用基于时间的窗口时使用 TimeWindow，
该窗口具有查询开始和结束时间戳的方法 ，以及返回给定窗口的最大允许时间戳的附加方法 maxTimestamp()。

在下文中，我们将展示 Flink 的预定义窗口分配器如何工作以及如何在 DataStream 程序中使用它们。下图显示了每个分配者的工作情况。
紫色圆圈表示流的数据元，这些数据元由某个键（在这种情况下是用户1，用户2和用户3）划分。x轴显示时间的进度。

#### 翻滚的Windows（Tumbling Windows）
翻滚窗口分配器将每个元素分配给指定窗口大小的窗口。翻滚窗口具有固定的尺寸，不重叠。例如，如果指定大小为5分钟的翻滚窗口，则将评估当前窗口，并且每五分钟将启动一个新窗口，如下图所示。
![tumbling-windows.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.7/fig/tumbling-windows.svg)

#### Sliding Windows


#### Session Windows


#### Global Windows
 
 



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
 
 
 
 

 