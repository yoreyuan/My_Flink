[State & Fault Tolerance](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/)
====

# 目录
* [Overview](#一) （概述）
* [Working with State](#二) （带状态的工作）
* [The Broadcast State Pattern](#三)  （广播状态模式）
* [Checkpointing](#四) 
* [Queryable State ](#五)  （可查询状态 ）
* [State Backends ](#六)  （状态后端）
* [State Schema Evolution](#七)  （状态模式演变）
* [Custom State Serialization](#八)  （自定义状态序列化）

<br/>

# 一、[Overview](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/)  （概述）
有状态的函数和 Operators 在各个`元素/事件`的处理中存储数据，使状态成为任何类型的更精细操作的关键构建部分。

例如：
* 当应用程序搜索某些事件模式时，状态将存储到目前为止遇到的事件序列。
* 在每`minute/hour/day`聚合事件时，状态保存待处理的聚合。
* 当在数据点流上训练机器学习模型时，状态保持模型参数的当前版本。
* 当需要管理历史数据时，状态允许有效访问过去发生的事件。

意识到Flink需要状态，为了使状态容错使用了 [Checkpoint](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/checkpointing.html)
，并允许流式应用程序使用[savepoints](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/state/savepoints.html)。

有关状态的知识还允许重新调整Flink应用程序，这意味着Flink负责跨并行实例重新分配状态。

Flink的可[查询状态](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/queryable_state.html)功能允许您在运行时从Flink外部访问状态。

在使用 state 时，阅读 Flink 的状态后端机制可能也很有用。 Flink提供了不同的状态后端机制，用于指定状态的存储方式和位置。 State可以位于Java的堆上或堆外。 
根据您的状态后端，Flink还可以管理应用程序的状态，这意味着Flink处理内存管理（如果需要可能会溢出到磁盘）以允许应用程序保持非常大的状态。 可以在不更改应用程序逻辑的情况下配置状态后端。

## 1.1 下一步去哪儿？
* [Working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)：
显示如何在 Flink 应用程序中使用状态并解释不同类型的状态。
* [The Broadcast State Pattern](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/broadcast_state.html)：
说明如何将广播流与非广播流连接，并使用状态在它们之间交换信息。
* [Checkpointing](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/checkpointing.html)：
描述如何启用和配置容错检查点。
* [Queryable State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/queryable_state.html)：
说明如何在运行时从Flink外部访问状态。
* [State Schema Evolution](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/schema_evolution.html)：
显示状态类型的模式是如何演变的。
* [Custom Serialization for Managed State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/custom_serialization.html)：
讨论如何实现自定义序列化程序，尤其是模式演变。


<br/>

******
Application Development &nbsp; / &nbsp; 
[Streaming (DataStream API)](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/datastream_api.html) &nbsp; / &nbsp; 
[State & Fault Tolerance](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/) &nbsp; / &nbsp; 

# 二、[Working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html) （带状态的工作）

本节目录 |
:---- | 
 [Keyed State and Operator State](#2.1)（Keyed状态和Operator状态） |
&nbsp; &nbsp;  [Keyed State](#2.1.1) （Keyed状态） |
&nbsp; &nbsp;  [Operator State](#2.1.2) （Operator状态） |
 [Raw and Managed State](#2.2)（Raw和状态管理）|
[Using Managed Keyed State](#2.3)（使用Managed Keyed状态）| 
&nbsp; &nbsp;  [State Time-To-Live (TTL)](#2.3.1)（状态存活时间）|
&nbsp; &nbsp;  [State in the Scala DataStream API ](#2.3.2) |
[Using Managed Operator State](#2.4) (使用Managed Operator状态) |
 &nbsp; &nbsp;  [Stateful Source Functions](#2.4.1)（有状态的Source函数）|
 
<br/>

## 2.1 Keyed State and Operator State（Keyed状态和Operator状态）
Flink中有两种基本的状态：`Keyed State` 和 `Operator State`。

### 2.1.1 Keyed State （Keyed状态）
Keyed State 始终与键相关，只能在 KeyedStream 上的函数和 operators 中使用。

您可以将 Keyed State 视为已分区或分片的 Operator State，每个 key 只有一个状态分区。 每个 keyed-state 在逻辑上绑定到 `<parallel-operator-instance，key>` 的唯一组合模式，
并且由于每个键“属于” keyed operator 的一个并行实例，我们可以将其简单地视为 `<operator, key>`。

Keyed State 进一步组织成所谓的 Key Groups 。Key Groups 是 Flink 可以重新分配 Keyed State 的原子单元; Key Groups 与定义的最大并行度完全一样多。 
在执行期间，keyed operator 的每个并行实例都使用一个或多个 Key Groups 的 key。

### 2.1.2 Operator State （Operator状态）
使用 Operator State（或non-keyed state），每个 operator state 都绑定到一个并行运算符实例。 [Kafka Connector](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/connectors/kafka.html) 
是在 Flink 中使用 Operator State 的一个很好的激励示例。 Kafka consumer 的每个并行实例都将 topic 分区和偏移的映射维护为其 Operator State。

Operator State 接口支持在并行性更改时在并行 operator 实例之间重新分配状态。 进行此重新分配可以有不同的方案。


## 2.2 Raw and Managed State（Raw和状态管理）
Keyed State 和 Operator State 有两种形式：managed 和 raw。

Managed State 由 Flink 运行时控制的数据结构表示，例如内部哈希表或RocksDB。 例如“ValueState”, “ListState”等。Flink的运行时对状态进行编码并将它们写入checkpoint。

Raw State 是 operators 保留在自己的数据结构中的状态。 当checkpointe时，它们只会将一个字节序列写入checkpoint。 Flink对状态的数据结构一无所知，只看到原始字节。

所有数据流功能都可以使用 managed state，但 raw state 接口只能在实现operator使用。 建议使用managed state（而不是raw state），因为在raw state下，
Flink能够在并行性更改时自动重新分配状态，并且还可以进行更好的内存管理。

**注意** 如果您的managed state需要自定义序列化逻辑，请参阅[相应的指南](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/custom_serialization.html)
以确保将来的兼容性。 Flink的默认序列化器不需要特殊处理。


## 2.3 Using Managed Keyed State （使用Managed Keyed状态）
managed keyed state 接口提供对不同类型状态的访问，这些状态都限定为当前输入元素的key。 这意味着这种类型的状态只能在KeyedStream上使用，
KeyedStream可以通过`stream.keyBy(…)`创建。

现在，我们将首先查看可用的不同类型的状态，然后我们将看到它们如何在程序中使用。 可用的状态原语是：

* `ValueState<T>`：这保留了一个可以更新和检索的值（如上所述，作用于输入元素的key的范围，因此运算看到的每个key可能有一个值）。 可以使用`update(T)`设置该值，并使用`T value()`检索该值。

* `ListState<T>`：保留的元素列表。 您可以追加元素并在所有当前存储的元素上检索`Iterable`。 使用`add(T) `或`addAll(List<T>)`添加元素，
可以使用`Iterable<T> get()`检索Iterable。 您还可以使用`update(List<T>)`重写现有的list。

* `ReducingState<T>`：保留一个值，表示添加到状态的所有值的聚合。 该接口类似于ListState，不过是使用`add(T) `添加的元素，reduced一个聚合是使用特定的`ReduceFunction`。

* `AggregatingState<IN, OUT>`：保留一个值表示添加到状态的所有值的聚合。 与ReducingState相反，聚合类型可能与添加到状态的元素类型不同。 
接口与ListState相同，不过是使用`add(T) `添加的元素，reduced一个聚合是使用特定的`AggregateFunction`。

* `FoldingState<T, ACC>`：保留一个值表示添加到状态的所有值的聚合。 与ReducingState相反，聚合类型可能与添加到状态的元素类型不同。 
该接口类似于ListState，不过是使用`add(T) `添加的元素，reduced一个聚合是使用特定的`FoldFunction`。
  
* `MapState<UK, UV>`：保留一个映射列表。 您可以将键值对放入状态，并在所有当前存储的映射上检索Iterable。 使用`put(UK, UV)`或`putAll(Map<UK, UV>)`添加映射。 
可以使用`get(UK)`检索与用户key关联的值。 可以分别使用`entries()`、`keys() `和`values() `来检索映射、键和值的可迭代视图。

所有类型的状态还有一个方法`clear()`，它清除当前活动key的状态，即输入元素的key。

**注意** `FoldingState`和`FoldingStateDescriptor`已在Flink 1.4中弃用，将来会被完全删除。 请改用`AggregatingState`和`AggregatingStateDescriptor`。

重要的是要牢记这些状态对象仅用于与状态接口。 状态不一定存储在内部，但可能驻留在磁盘或其他位置。 要记住的第二件事是，从状态获得的值取决于input元素的key。 
因此如果涉及的key不同，则在一次调用用户函数时获得的值可能与另一次调用中的值不同。

要获取状态句柄，您必须创建`StateDescriptor`。 这保存了状态的名称（正如我们稍后将看到的，您可以创建多个状态，并且它们必须具有唯一的名称以便您可以引用它们），状态所持有的值的类型，并且可能是用户指定的函数，
例如`ReduceFunction`。 根据要检索的状态类型，可以创建`ValueStateDescriptor`、`ListStateDescriptor`、`ReducingStateDescriptor`、`FoldingStateDescriptor`或`MapStateDescriptor`。

使用`RuntimeContext`访问状态，因此只能在富函数中使用。 请参阅[此处](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/api_concepts.html#rich-functions)
了解相关信息，但我们很快也会看到一个示例。 `RichFunction`中可用的`RuntimeContext`具有以下访问状态的方法：

* `ValueState<T> getState(ValueStateDescriptor<T>)`
* `ReducingState<T> getReducingState(ReducingStateDescriptor<T>)`
* `ListState<T> getListState(ListStateDescriptor<T>)`
* `AggregatingState<IN, OUT> getAggregatingState(AggregatingStateDescriptor<IN, ACC, OUT>)`
* `FoldingState<T, ACC> getFoldingState(FoldingStateDescriptor<T, ACC>)`
* `MapState<UK, UV> getMapState(MapStateDescriptor<UK, UV>)`

这是一个示例`FlatMapFunction`，显示所有的部分如何组合在一起：

```
class CountWindowAverage extends RichFlatMapFunction[(Long, Long), (Long, Long)] {

  private var sum: ValueState[(Long, Long)] = _

  override def flatMap(input: (Long, Long), out: Collector[(Long, Long)]): Unit = {

    // access the state value
    val tmpCurrentSum = sum.value

    // If it hasn't been used before, it will be null
    val currentSum = if (tmpCurrentSum != null) {
      tmpCurrentSum
    } else {
      (0L, 0L)
    }

    // update the count
    val newSum = (currentSum._1 + 1, currentSum._2 + input._2)

    // update the state
    sum.update(newSum)

    // if the count reaches 2, emit the average and clear the state
    if (newSum._1 >= 2) {
      out.collect((input._1, newSum._2 / newSum._1))
      sum.clear()
    }
  }

  override def open(parameters: Configuration): Unit = {
    sum = getRuntimeContext.getState(
      new ValueStateDescriptor[(Long, Long)]("average", createTypeInformation[(Long, Long)])
    )
  }
}


object ExampleCountWindowAverage extends App {
  val env = StreamExecutionEnvironment.getExecutionEnvironment

  env.fromCollection(List(
    (1L, 3L),
    (1L, 5L),
    (1L, 7L),
    (1L, 4L),
    (1L, 2L)
  )).keyBy(_._1)
    .flatMap(new CountWindowAverage())
    .print()
  // the printed output will be (1,4) and (1,5)

  env.execute("ExampleManagedState")
}
```

这个例子实现了一个`poor man’s`的计数窗口。 我们通过第一个字段键入元组（在示例中都具有相同的key 1）。 该函数将计数和运行总和存储在ValueState中。 一旦计数达到2，它将发出平均值并清除状态，以便我们从0开始。
注意，如果我们在第一个字段中有不同值的元组，这将为每个不同的输入key保持不同的状态值。

### 2.3.1 State Time-To-Live (TTL)（状态存活时间）
可以将存活时间（TTL）分配给任何类型的keyed state。 如果配置了TTL并且状态值已过期，则将尽力清除存储的值，这将在下面更详细地讨论。

所有状态集合类型都支持 per-entry TTL。 这意味着列表元素和map条目将独立到期。

为了使用状态TTL，必须首先构建`StateTtlConfig`配置对象。 然后，可以通过传递配置在任何状态描述符中启用TTL功能：
```
import org.apache.flink.api.common.state.StateTtlConfig
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.api.common.time.Time

val ttlConfig = StateTtlConfig
    .newBuilder(Time.seconds(1))
    .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
    .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
    .build
    
val stateDescriptor = new ValueStateDescriptor[String]("text state", classOf[String])
stateDescriptor.enableTimeToLive(ttlConfig)
```

配置有几个选项需要考虑：

`newBuilder`方法的第一个参数是必需的，它是值为存活时间。

当状态存活时间刷新时更新类型配置（默认为`OnCreateAndWrite`）：
* StateTtlConfig.UpdateType.OnCreateAndWrite - 仅限创建和写入访问
* StateTtlConfig.UpdateType.OnReadAndWrite - 也是读访问权限

状态可见性配置是否在读取访问时返回过期值（如果尚未清除）（默认为`NeverReturnExpired`）：
* StateTtlConfig.StateVisibility.NeverReturnExpired - 永远不会返回过期值
* StateTtlConfig.StateVisibility.ReturnExpiredIfNotCleanedUp - 如果仍然可用则返回

在`NeverReturnExpired`的情况下，过期状态表现得好像它不再存在，即使它仍然必须被删除。 该选项对于在TTL之后必须严格用于读取访问的数据的用例是有用的，例如，应用程序使用隐私敏感数据。

另一个选项`ReturnExpiredIfNotCleanedUp`允许在清理之前返回过期状态。

**笔记**：
* 状态后端存储上次修改的时间戳以及用户值，这意味着启用此功能会增加状态存储的消耗。堆状态后端存储一个额外的Java对象，其中包含对用户状态对象的引用和内存中的原始long值。
RocksDB状态后端为每个存储值，list条目或map条目添加8个字节。
* 目前仅支持参考处理时间的TTL。
* 尝试恢复先前未使用TTL配置的状态，使用启用TTL的描述符（反之亦然）将导致兼容性失败和`StateMigrationException`。
* TTL配置不是checkpoint或savepoint的一部分，而是Flink如何在当前运行的作业中处理它的方式。
* 仅当用户值序列化程序可以处理空值时，具有TTL的映射状态当前才支持空用户值。如果序列化程序不支持空值，则可以使用NullableSerializer包装它，代价是序列化形式的额外字节。

#### Cleanup of Expired State（清除过期状态）
默认情况下，只有在明确读出过期值时才会删除过期值，例如通过调用 `ValueState.value()`。

**注意** 这意味着默认情况下，如果未读取过期状态，则不会删除它，可能会导致状态不断增长。 这可能在将来的版本中发生变化

##### 清除完整快照
此外，您可以在获取完整状态快照时激活清理，这将减小其大小。 在当前实现下不清除本地状态，但在从上一个快照恢复的情况下，它不会包括已删除的过期状态。 它可以在`StateTtlConfig`中配置：
```
import org.apache.flink.api.common.state.StateTtlConfig
import org.apache.flink.api.common.time.Time

val ttlConfig = StateTtlConfig
    .newBuilder(Time.seconds(1))
    .cleanupFullSnapshot
    .build
```

此选项不适用于RocksDB状态后端中的增量Checkpoint。

**笔记：**
* 对于现有作业，可以在`StateTtlConfig`中随时激活或停用此清理策略，例如， 从savepoint重启后。

#### Cleanup in background (在后台清除)
除了在完整快照中清理外，您还可以在后台激活清理。如果使用的后端支持以下选项，则会激活`StateTtlConfig`中的默认后台清理：
```
import org.apache.flink.api.common.state.StateTtlConfig
val ttlConfig = StateTtlConfig
    .newBuilder(Time.seconds(1))
    .cleanupInBackground
    .build
```

要在后台对某些特殊清理进行更精细的控制，可以按照下面的说明单独配置它。 目前，堆状态后端依赖于增量清理，RocksDB后端使用压缩过滤器进行后台清理。

##### 增量清理
另一种选择是逐步触发一些状态条目的清理。 触发器可以是来自每个状态访问or/and 每个记录处理的回调。 如果此清理策略对某些状态处于活动状态，
则存储后端会在其所有条目上为此状态保留一个惰性全局迭代器。 每次触发增量清理时，迭代器都会被提前。 检查遍历的状态条目，清除过期的状态条目。

可以在`StateTtlConfig`中激活此功能：
```
import org.apache.flink.api.common.state.StateTtlConfig
val ttlConfig = StateTtlConfig
    .newBuilder(Time.seconds(1))
    .cleanupIncrementally
    .build
```

该策略有两个参数。第一个是每次清理触发的已检查状态条目数。 如果启用，则始终按每个状态访问触发。 第二个参数定义是否每个记录处理另外触发清理。 
如果启用默认后台清理，则将为具有5个已检查条目的堆后端激活此策略，并且不对每个记录处理进行清理。

**笔记**：
* 如果状态没有访问或没有处理记录，则过期状态将持续存在。
* 增量清理所花费的时间会增加记录处理延迟。
* 目前，仅针对堆状态后端实施增量清理。 为RocksDB设置它将不起作用。
* 如果堆状态后端与同步快照一起使用，则全局迭代器会在迭代时保留所有key的副本，因为它的特定实现不支持并发修改。 启用此功能会增加内存消耗。 异步快照没有此问题。
* 对于现有作业，可以在`StateTtlConfig`中随时激活或停用此清理策略，例如，从savepoint重启后。

##### 在RocksDB压缩期间进行清理
如果使用RocksDB状态后端，则另一种清理策略是激活Flink特定的压缩过滤器。 RocksDB定期运行异步压缩以合并状态更新并减少存储。 
Flink压缩过滤器使用TTL检查状态条目的到期时间戳，并排除过期值。

默认情况下禁用此功能。必须首先通过设置Flink配置选项`state.backend.rocksdb.ttl.compaction.filter.enabled`或通过调用`RocksDBStateBackend::enableTtlCompactionFilter`
为RocksDB后端激活它，如果为作业创建了自定义RocksDB状态后端。 然后可以将任何具有TTL的状态配置为使用过滤器：
```
import org.apache.flink.api.common.state.StateTtlConfig

val ttlConfig = StateTtlConfig
    .newBuilder(Time.seconds(1))
    .cleanupInRocksdbCompactFilter(1000)
    .build
```

每次处理一定数量的状态条目后，RocksDB压缩过滤器将从Flink查询用于检查过期的当前时间戳。 您可以更改它并将自定义值传递给`StateTtlConfig.newBuilder(...).cleanupInRocksdbCompactFilter(long queryTimeAfterNumEntries)`方法。 
更频繁地更新时间戳可以提高清理速度，但是它会降低压缩性能，因为它使用来自本机代码的JNI调用。 如果启用默认后台清理，则将为RocksDB后端激活此策略，并且每次处理1000个条目时将查询当前时间戳。

您可以通过激活`FlinkCompactionFilter`的调试级别来激活RocksDB过滤器的本机代码中的调试日志：
`log4j.logger.org.rocksdb.FlinkCompactionFilter= DEBUG`

**笔记**：
* 在压缩期间调用TTL过滤器会减慢它的速度。 TTL过滤器必须解析上次访问的时间戳，并检查每个正在压缩的key的每个存储状态条目的到期时间。 在收集状态类型（list或map）的情况下，还对每个存储的元素调用检查。
* 如果此功能与包含非固定字节长度的元素的列表状态一起使用，则本机TTL过滤器必须每个状态条目另外调用JNI上元素的Flink java类型序列化程序，其中至少第一个元素已到期确定下一个未到期元素的偏移量。
* 对于现有作业，可以在`StateTtlConfig`中随时激活或停用此清理策略，例如从savepoint重启后。

### 2.3.2 State in the Scala DataStream API
除了上面描述的接口之外，Scala API还具有有状态`map()`或`flatMap()`函数的快捷方式，在KeyedStream上具有单个ValueState。 用户函数在Option中获取ValueState的当前值，并且必须返回将用于更新状态的更新值。
```
val stream: DataStream[(String, Int)] = ...

val counts: DataStream[(String, Int)] = stream
  .keyBy(_._1)
  .mapWithState((in: (String, Int), count: Option[Int]) =>
    count match {
      case Some(c) => ( (in._1, c), Some(c + in._2) )
      case None => ( (in._1, 0), Some(in._2) )
    })
```


## 2.4 Using Managed Operator State (使用Managed Operator状态)
要使用managed operator状态，有状态函数可以实现更通用的`CheckpointedFunction`接口，或者`ListCheckpointed<T extends Serializable>`接口。

#### CheckpointedFunction
`CheckpointedFunction`接口提供对具有不同重新分发方案的非non-keyed状态的访问。它需要实现两种方法：

```
void snapshotState(FunctionSnapshotContext context) throws Exception;

void initializeState(FunctionInitializationContext context) throws Exception;
```

每当必须执行checkpoint时，都会调用`snapshotState()`。 每次初始化用户定义的函数时，都会调用对应的`initializeState()`，即首次初始化函数时，
或者当函数实际从早期checkpoint恢复时。 鉴于此`initializeState()`不仅是初始化不同类型状态的地方，而且还包括状态恢复逻辑。

目前，支持列表样式的managed operator状态。 该状态应该是一个可序列化对象的列表，彼此独立，因此有资格在重新缩放时重新分配。 换句话说，
这些对象是可以重新分配non-keyed状态的最精细的粒度。 根据状态访问方法，定义了以下重新分发方案：

* **Even-split再分配**：每个运算符返回一个状态元素列表。 整个状态在逻辑上是所有列表的串联。 在恢复/重新分发时，列表被平均分成与并行运算符一样多的子列表。 
每个运算符都获得一个子列表，该子列表可以为空，或包含一个或多个元素。 例如，如果使用并行度为1，运算符的checkpoint状态包含元素element1和element2，
当将并行度增加到2时，element1可能最终在operator实例0中，而element2将转到operator实例1。

* **Union重新分配**：每个运算符返回一个状态元素列表。 整个状态在逻辑上是所有列表的串联。 在恢复/重新分配时，每个运算符都会获得完整的状态元素列表。

下面是一个有状态`SinkFunction`的示例，它使用`CheckpointedFunction`缓冲元素，然后再将它们发送到外部世界。 它演示了基本的`Even-split再分配`列表状态：
```
class BufferingSink(threshold: Int = 0)
  extends SinkFunction[(String, Int)]
    with CheckpointedFunction {

  @transient
  private var checkpointedState: ListState[(String, Int)] = _

  private val bufferedElements = ListBuffer[(String, Int)]()

  override def invoke(value: (String, Int), context: Context): Unit = {
    bufferedElements += value
    if (bufferedElements.size == threshold) {
      for (element <- bufferedElements) {
        // send it to the sink
      }
      bufferedElements.clear()
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    checkpointedState.clear()
    for (element <- bufferedElements) {
      checkpointedState.add(element)
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    val descriptor = new ListStateDescriptor[(String, Int)](
      "buffered-elements",
      TypeInformation.of(new TypeHint[(String, Int)]() {})
    )

    checkpointedState = context.getOperatorStateStore.getListState(descriptor)

    if(context.isRestored) {
      for(element <- checkpointedState.get()) {
        bufferedElements += element
      }
    }
  }

}
```

`initializeState`方法将`FunctionInitializationContext`作为参数。 这用于初始化non-keyed状态“containers”。 这些是ListState类型的容器，其中non-keyed状态对象将在checkpoint存储。

注意状态是如何初始化的，类似于keyed状态，`StateDescriptor`包含状态名称和有关状态值的类型的信息：
```
val descriptor = new ListStateDescriptor[(String, Long)](
    "buffered-elements",
    TypeInformation.of(new TypeHint[(String, Long)]() {})
)

checkpointedState = context.getOperatorStateStore.getListState(descriptor)
```

状态访问方法的命名约定包含其重新分发模式，后跟其状态结构。 例如，要在还原时使用**联合重新分发**方案的列表状态，请使用`getUnionListState(descriptor)`访问该状态。 
如果方法名称不包含重新分发模式，例如 `getListState(descriptor)`，它只是意味着将使用基本的even-split再分配方案。

在初始化容器之后，我们使用上下文的` isRestored()`方法来检查我们是否在失败后恢复。 如果这是真的，即我们正在恢复，则应用恢复逻辑。

如修改后的`BufferingSink`的代码所示，在状态初始化期间恢复的ListState保存在类变量中以供将来在`snapshotState()`中使用。 在那里，
ListState被清除前一个checkpoint包含的所有对象，然后填充我们想要checkpoint的新对象。

作为旁注，keyed状态也可以在`initializeState()`方法中初始化。这可以使用提供的`FunctionInitializationContext`来完成。

#### ListCheckpointed
`ListCheckpointed`接口是`CheckpointedFunction`的一个更有限的变体，它仅支持在恢复时具有even-split再分配方案的列表样式状态。它还需要实现两种方法：
```
List<T> snapshotState(long checkpointId, long timestamp) throws Exception;

void restoreState(List<T> state) throws Exception;
```

在`snapshotState()`上operator应该返回checkpoint的对象列表，restoreState必须在恢复时处理这样的列表。 如果状态不可重新分区，
则始终可以在`snapshotState()`中返回`Collections.singletonList(MY_STATE)`。

### 2.4.1 Stateful Source Functions （有状态的Source函数）
与其他operators相比，有状态的来源需要更多的关注。 为了使状态和输出集合的更新成为原子性的（在故障/恢复时精确一次语义所需），用户需要从源的上下文获取锁定。

```
class CounterSource
       extends RichParallelSourceFunction[Long]
       with ListCheckpointed[Long] {

  @volatile
  private var isRunning = true

  private var offset = 0L

  override def run(ctx: SourceFunction.SourceContext[Long]): Unit = {
    val lock = ctx.getCheckpointLock

    while (isRunning) {
      // output and state update are atomic
      lock.synchronized({
        ctx.collect(offset)

        offset += 1
      })
    }
  }

  override def cancel(): Unit = isRunning = false

  override def restoreState(state: util.List[Long]): Unit =
    for (s <- state) {
      offset = s
    }

  override def snapshotState(checkpointId: Long, timestamp: Long): util.List[Long] =
    Collections.singletonList(offset)

}
```
当Flink完全确认checkpoint与外界通信时，某些operators可能需要这些信息。在这种情况下，请参阅`org.apache.flink.runtime.state.CheckpointListener`接口。


<br/>

******

# 三、[The Broadcast State Pattern](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/broadcast_state.html) （广播状态模式）

本节目录 |
:---- | 
 [Provided APIs](#3.1) （提供的API） |
&nbsp; &nbsp;  [BroadcastProcessFunction and KeyedBroadcastProcessFunction ](#3.1.1) |
[Important Considerations](#3.2) （重要考虑因素）|

<br/>

[working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)描述operator状态，该运算符状态在恢复时均匀分布在operator的并行任务中，或者联合使用，整个状态用于初始化已恢复的并行任务。

第三种支持的operator状态是广播状态。 引入广播状态是为了支持这样的用例，其中来自一个流的一些数据需要被广播到所有下游任务，其中它被本地存储并用于处理另一个流上的所有传入元素。 作为一个广播状态可以自然出现的例子，
可以想象包含一组规则的低吞吐量流，我们希望针对来自另一个流的所有元素进行评估。 考虑到上述类型的用例，广播状态与其他operator状态的不同之处在于：
* 它有一个Map格式，
* 它仅适用于具有广播流和非广播流的输入的特定operators
* 这样的operators可以具有不同名称的多个广播状态。

## 3.1 Provided APIs （提供的API）
为了显示提供的API，我们将在展示其完整功能之前先举一个示例。作为我们的运行示例，我们将使用我们拥有不同颜色和形状的对象流的情况，并且我们想要找到遵循特定模式的相同颜色的对象对儿，
例如，一个矩形后跟一个三角形。我们假设这组有趣的模式随着时间的推移而发展。

在此示例中，第一个流将包含Item类型的元素，其中包含Color和Shape属性。 另一个流将包含规则。

从Items流开始，我们只需要通过Color作为key，因为我们需要相同颜色的对。 这将确保相同颜色的元素最终在同一台物理机器上。

```
// key the shapes by color
KeyedStream<Item, Color> colorPartitionedStream = shapeStream
                        .keyBy(new KeySelector<Shape, Color>(){...});
```

继续执行规则部分，包含它们的流应该被广播到所有下游任务，并且这些任务应该在本地存储它们，以便它们可以针对所有传入的Items对它们进行评估。下面的片段将 (i)广播规则流和 (ii)使用提供的MapStateDescriptor，它将创建存储规则的广播状态。

```
// a map descriptor to store the name of the rule (string) and the rule itself.
MapStateDescriptor<String, Rule> ruleStateDescriptor = new MapStateDescriptor<>(
			"RulesBroadcastState",
			BasicTypeInfo.STRING_TYPE_INFO,
			TypeInformation.of(new TypeHint<Rule>() {}));
		
// broadcast the rules and create the broadcast state
BroadcastStream<Rule> ruleBroadcastStream = ruleStream
                        .broadcast(ruleStateDescriptor);
```
最后，为了根据Item流中的传入元素评估规则，我们需要：
* 连接两个流，然后
* 指定我们的匹配检测逻辑。

将流（keyed或non-keyed）与BroadcastStream连接可以通过在非广播流上调用connect()，并将BroadcastStream作为参数来完成。 这将返回一个BroadcastConnectedStream，
我们可以使用特殊类型的CoProcessFunction调用process()。 该函数将包含我们的匹配逻辑。 函数的确切类型取决于非广播流的类型：
* 如果是keyed，则该函数是KeyedBroadcastProcessFunction。
* 如果它是non-keyed，则该函数是BroadcastProcessFunction。

鉴于我们的非广播流是keyed的，以下代码段包含以上调用：

**注意**：应该在非广播流上调用connect，并将BroadcastStream作为参数。

```
DataStream<String> output = colorPartitionedStream
                 .connect(ruleBroadcastStream)
                 .process(
                     
                     // 我们的KeyedBroadcastProcessFunction中的类型参数表示：
                     //   1. keyed流的键
                     //   2. 非广播方的元素类型
                     //   3. 广播方的元素类型
                     //   4. 结果的类型, 这里是一个 string
                     new KeyedBroadcastProcessFunction<Color, Item, Rule, String>() {
                         // my matching logic
                     }
                 );
```

### 3.1.1 BroadcastProcessFunction and KeyedBroadcastProcessFunction
与CoProcessFunction的情况一样，这些函数有两种实现方法; `processBroadcastElement()`负责处理广播流中的传入元素，和`processElement()`用于非广播流。 这些方法的完整签名如下：
```
public abstract class BroadcastProcessFunction<IN1, IN2, OUT> extends BaseBroadcastProcessFunction {

    public abstract void processElement(IN1 value, ReadOnlyContext ctx, Collector<OUT> out) throws Exception;

    public abstract void processBroadcastElement(IN2 value, Context ctx, Collector<OUT> out) throws Exception;
}
```

```
public abstract class KeyedBroadcastProcessFunction<KS, IN1, IN2, OUT> {

    public abstract void processElement(IN1 value, ReadOnlyContext ctx, Collector<OUT> out) throws Exception;

    public abstract void processBroadcastElement(IN2 value, Context ctx, Collector<OUT> out) throws Exception;

    public void onTimer(long timestamp, OnTimerContext ctx, Collector<OUT> out) throws Exception;
}
```

首先要注意的是，这两个函数都需要实现`processBroadcastElement()`方法来处理广播端中的元素，而`processElement()`则需要非广播端的元素。

这两种方法在提供的上下文中有所不同。非广播端有一个`ReadOnlyContext`，而广播端有一个Context。

这两个上下文(在一下列举的ctx)：
1. 访问广播状态: `ctx.getBroadcastState(MapStateDescriptor<K, V> stateDescriptor)`，
2. 允许查询元素的时间戳: `ctx.timestamp()`,
3. 获取当前水印: `ctx.currentWatermark()`
4. 获取当前处理时间: `ctx.currentProcessingTime()`，然后
5. 侧输出发出元素: `ctx.output(OutputTag<X> outputTag, X value)`。

`getBroadcastState()`中的stateDescriptor应该与上面的`.broadcast(ruleStateDescriptor)`中的stateDescriptor相同。

不同之处在于每个人对广播状态的访问类型。广播方对其具有**读写访问权限**，而非广播方具有**只读访问权**（因这个名称）。 原因是在Flink中没有跨任务通信。 因此，
为了保证广播状态中的内容在我们的operator的所有并行实例中是相同的，我们只对广播端提供读写访问，广播端在所有任务中看到相同的元素，并且我们需要对每个任务进行计算。 
这一侧的传入元素在所有任务中都是相同的。 忽略此规则会破坏状态的一致性保证，从而通常导致不一致且难以调试的结果。

**注意**：`processBroadcast()`中实现的逻辑必须在所有并行实例中具有相同的确定性行为！

最后，由于`KeyedBroadcastProcessFunction`在keyed流上运行，它向外提供了一些BroadcastProcessFunction不可用的功能。 那是：
1. `processElement()`方法中的ReadOnlyContext可以访问Flink的底层计时器服务，该服务允许注册事件 and/or 处理时间计时器。 当一个计时器触发时，
`onTimer()`（如上所示）被一个OnTimerContext调用，它公开了与ReadOnlyContext plus 相同的功能
    * 触发的计时器是事件时间还是处理时间的请求能力
    * 查询与计时器关联的key。
2. `processBroadcastElement()`方法中的Context包含方法`applyToKeyedState(StateDescriptor<S, VS> stateDescriptor, KeyedStateFunction<KS, S> function)`。 
这允许注册KeyedStateFunction以应用于与提供的stateDescriptor相关联的所有key所有状态。

**注意**：只能在`KeyedBroadcastProcessFunction`的`processElement()`中注册定时器。 在`processBroadcastElement()`方法中是不可能的，因为没有与广播元素相关联的key。

回到我们的原先的示例，我们的KeyedBroadcastProcessFunction可能如下所示：
```
new KeyedBroadcastProcessFunction<Color, Item, Rule, String>() {

    // 保存部分的匹配项, 即等待其第二个元素pair的第一个元素
    // 我们保存一个list, 因为我们可能有许多对一个元素在等待
    private final MapStateDescriptor<String, List<Item>> mapStateDesc =
        new MapStateDescriptor<>(
            "items",
            BasicTypeInfo.STRING_TYPE_INFO,
            new ListTypeInfo<>(Item.class));

    // identical to our ruleStateDescriptor above
    private final MapStateDescriptor<String, Rule> ruleStateDescriptor = 
        new MapStateDescriptor<>(
            "RulesBroadcastState",
            BasicTypeInfo.STRING_TYPE_INFO,
            TypeInformation.of(new TypeHint<Rule>() {}));

    @Override
    public void processBroadcastElement(Rule value,
                                        Context ctx,
                                        Collector<String> out) throws Exception {
        ctx.getBroadcastState(ruleStateDescriptor).put(value.name, value);
    }

    @Override
    public void processElement(Item value,
                               ReadOnlyContext ctx,
                               Collector<String> out) throws Exception {

        final MapState<String, List<Item>> state = getRuntimeContext().getMapState(mapStateDesc);
        final Shape shape = value.getShape();
    
        for (Map.Entry<String, Rule> entry :
                ctx.getBroadcastState(ruleStateDescriptor).immutableEntries()) {
            final String ruleName = entry.getKey();
            final Rule rule = entry.getValue();
    
            List<Item> stored = state.get(ruleName);
            if (stored == null) {
                stored = new ArrayList<>();
            }
    
            if (shape == rule.second && !stored.isEmpty()) {
                for (Item i : stored) {
                    out.collect("MATCH: " + i + " - " + value);
                }
                stored.clear();
            }
    
            // there is no else{} to cover if rule.first == rule.second
            if (shape.equals(rule.first)) {
                stored.add(value);
            }
    
            if (stored.isEmpty()) {
                state.remove(ruleName);
            } else {
                state.put(ruleName, stored);
            }
        }
    }
}
```

## 3.2 Important Considerations （重要考虑因素）
在描述了提供的API之后，本节重点介绍使用广播状态时要记住的重要事项。这些是：
* **没有cross-task通信**: 如上所述，这就是为什么只有(Keyed)-BroadcastProcessFunction的广播端可以修改广播状态的内容。此外，用户必须确保所有任务以相同的方式为每个传入元素修改广播状态的内容。 否则，不同的任务可能具有不同的内容，从而导致不一致的结果。
* **广播状态中的事件顺序可能因任务而异**: 尽管广播流的元素保证所有元素将（最终）转到所有下游任务，但元素可能以不同的顺序到达每个任务。 因此，每个传入元素的状态更新绝不取决于传入事件的顺序。
* **所有任务都checkpoint其广播状态**: 虽然checkpoint发生时所有任务在广播状态中都具有相同的元素（checkpoint的barriers不会覆盖元素），但所有任务都会checkpoint其广播状态，而不仅仅是其中一个。 
这是一个设计决策，以避免在恢复期间从同一文件中读取所有任务（从而避免热点），尽管它的代价是将检查点状态的大小增加p(等于并行度)。Flink保证在restoring/rescaling时**不会出现重复**数据，也**不会丢失数据**。 
在具有相同或更小并行度的恢复的情况下，每个任务读取其restoring/rescaling状态。 调整比例时，每个任务都读取其自己的状态，其余任务（p_new-p_old）以循环方式读取先前任务的checkpoints。
* **没有RocksDB状态后端**: 广播状态在运行时保留在内存中，并且应该相应地进行内存配置。这适用于所有operator状态。


<br/>

******

# 四、[Checkpointing](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/checkpointing.html)

本节目录 |
:---- | 
 [Prerequisites](#4.1)  (要求)  |
 [Enabling and Configuring Checkpointing](#4.2) (启用和配置 Checkpoint) |
 &nbsp; &nbsp;  [Related Config Options](#4.2.1) (相关配置项) |
 [Selecting a State Backend](#4.3) (选择状态后端)|
 [State Checkpoints in Iterative Jobs](#4.4) (迭代job中的状态Checkpoint)| 
 [Restart Strategies](#4.5) (重启策略)|
 
 <br/>

Flink中的每个函数和operator都可以是有状态的（有关详细信息请参阅[working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)）。
有状态函数在各个元素/事件的处理中存储数据，使状态成为任何类型的更复杂operator的关键构建部分。

为了使状态容错，Flink需要Checkpoint状态。 checkpoint允许Flink恢复流中的状态和位置，从而为应用程序提供与无故障执行相同的语义。

在[流容错的文档](https://ci.apache.org/projects/flink/flink-docs-release-1.8/internals/stream_checkpointing.html)中详细描述了Flink的流容错机制背后的技术。

## 4.1 Prerequisites (要求) 
Flink的checkpoint机制与流和状态的持久存储交互。 一般来说，它需要：
* 持久（或持续）数据源，可以在一定时间内重放记录。 这种源的示例是持久消息队列（例如，Apache Kafka，RabbitMQ，Amazon Kinesis，Google PubSub）或文件系统（例如，HDFS，S3，GFS，NFS，Ceph，......）。
* 状态的持久存储，通常是分布式文件系统（例如，HDFS，S3，GFS，NFS，Ceph，...）

## 4.2 Enabling and Configuring Checkpointing (启用和配置 Checkpoint)
默认情况下checkpoint是被禁用的。要启用检查点，请在StreamExecutionEnvironment上调用`enableCheckpointing(n)`，其中n是检查点间隔（以毫秒为单位）。

检查点的其他参数包括：
* **exactly-once 与 at-least-once**: 您可以选择将模式传递给`enableCheckpointing(n)`方法，以便在两个保证级别之间进行选择。 对于大多数应用来说，恰好一次是优选的。至少一次可能与某些超低延迟（始终为几毫秒）的应用程序相关。
* **checkpoint超时时间**: 如果在那之前没有完成，则中止正在进行的checkpoint的时间。
* **checkpoint之间的最短时间**: 为了确保流应用程序在检查点之间取得一定进展，可以定义checkpoint之间需要经过多长时间。 如果将此值设置为例如5000，则无论checkpoint持续时间和checkpoint间隔如何，
下一个checkpoint将在上一个checkpoint完成后的5秒内启动。 请注意，这意味着checkpoint间隔永远不会小于此参数。

通过定义“checkpoint间的时间”而不是checkpoint间隔来配置应用程序通常更容易，因为“checkpoint间的时间”不易受checkpoint到有时需要的比平均时间更长的事实影响（例如，如果目标存储系统暂时很慢）。

请注意，此值还表示并发checkpoint的数量为1。

* **并发检查点数**：默认情况下，当一个checkpoint仍处于运行状态时，系统不会触发另一个检查点。 这可确保拓扑不会在checkpoint上花费太多时间，也不会在处理流方面进展。 
它可以允许多个重叠检查点，这对于具有特定处理延迟的管道（例如，因为函数调用需要一些时间来响应的外部服务）而有兴趣，但是仍然希望执行非常频繁的checkpoint（100毫秒）在失败时重新处理很少。

当定义checkpoint间的最短时间时，不能使用此选项。

* **外部化checkpoint**：您可以配置定期checkpoint以在外部持久化。外部化checkpoint将其元数据写入持久存储，并且在作业失败时不会自动清除。
这样，如果你的工作失败，你将有一个checkpoint来恢复。有关[外部化checkpoint的部署说明](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/state/checkpoints.html#externalized-checkpoints)中有更多详细信息。

* **checkpoint错误时的任务失败/继续**：这确定如果在执行任务的checkpoint过程中发生错误，任务是否将失败。这是默认行为。或者，当禁用此选项时，任务将简单地拒绝checkpoint协调器的checkpoint并继续运行。

```
val env = StreamExecutionEnvironment.getExecutionEnvironment()

// 每 1000 毫秒启动一个检查点
env.enableCheckpointing(1000)

// 高级选项:

// 设置模式为 exactly-once (这也是默认的)
env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)

// 确保checkpoint之间发生500毫秒的处理
env.getCheckpointConfig.setMinPauseBetweenCheckpoints(500)

// checkpoint必须在1分钟内完成，否则被丢弃
env.getCheckpointConfig.setCheckpointTimeout(60000)

// 如果在checkpoint发生错误时防止任务失败，checkpoint将被拒绝。
env.getCheckpointConfig.setFailTasksOnCheckpointingErrors(false)

// allow only one checkpoint to be in progress at the same time
env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
```

### 4.2.1 Related Config Options (相关配置项)
可以通过`conf/flink-conf.yaml`设置更多参数and/or默认值（参见完整指南的[配置](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/config.html)）：

key | 默认值 | 说明
:---- | :----: | :----
state.backend | (none) | 用于存储和checkpoint状态的状态后端。
state.backend.async | true | 选择状态后端是否应在可能和可配置的情况下使用异步快照方法。某些状态后端可能不支持异步快照，或仅支持异步快照，时并忽略此选项。
state.backend.fs.memory-threshold | 1024 | 状态数据文件的最小大小。小于该值的所有状态块都内联存储在根checkpoint元数据文件中。
state.backend.incremental | false | 如果可能，选择状态后端是否应创建增量checkpoint。 对于增量checkpoint，仅存储来自先前检查点的差异，而不是完整的检查点状态。 某些状态后端可能不支持增量checkpoint并忽略此选项。
state.backend.local-recovery | false | 此选项配置此状态后端的本地恢复。默认情况下，禁用本地恢复。本地恢复目前仅涵盖关键状态后端。目前，MemoryStateBackend不支持本地恢复并忽略此选项。
state.checkpoints.dir | (none) | 用于在Flink支持的文件系统中存储checkpoint的数据文件和元数据的默认目录。必须可以从所有参与的进程/节点（即所有TaskManagers和JobManagers）访问存储路径。
state.checkpoints.num-retained | 1 | 要保留的已完成checkpoint的最大数量。
state.savepoints.dir | (none) | savepoint的默认目录。 由将后端写入文件系统的状态后端（MemoryStateBackend，FsStateBackend，RocksDBStateBackend）使用。
taskmanager.state.local.root-dirs | (none) | config参数定义根目录，用于存储基于文件的状态以进行本地恢复。本地恢复目前仅涵盖关键状态后端。目前，MemoryStateBackend不支持本地恢复并忽略此选项

## 4.3 Selecting a State Backend (选择状态后端)
Flink的[checkpoint机制存](https://ci.apache.org/projects/flink/flink-docs-release-1.8/internals/stream_checkpointing.html)储所有定时器和有状态operators中的状态一致快照，
包括连接器、窗口和任何[用户定义的状态](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)。 
存储checkpoint的位置（例如，JobManager内存，文件系统，数据库）取决于配置的状态后端。

默认情况下，状态保存在TaskManagers的内存中，checkpoint存储在JobManager的内存中。 为了适当持久化大状态，Flink支持在其他状态后端中存储和checkpoint状态的各种方法。 可以通过`StreamExecutionEnvironment.setStateBackend(…)`配置状态后端的选择。

有关可用状态后端的详细信息以及作业范围和群集范围配置的选项，请参阅[状态后端](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/state/state_backends.html)。

## 4.4 State Checkpoints in Iterative Jobs (迭代job中的状态Checkpoint)
Flink目前仅为没有iteration的作业提供处理保证。 **在迭代作业上启用checkpoint会导致异常**。 为了强制对迭代程序进行checkpoint，用户在启用checkpoint时需要设置一个特殊标志：`env.enableCheckpointing(interval, CheckpointingMode.EXACTLY_ONCE, force = true)`。

请注意，在失败期间，循环边缘中的记录（以及与它们相关的状态变化）将丢失。

## 4.5 Restart Strategies (重启策略)
Flink支持不同的重启策略，可以控制在发生故障时如何重新启动作业。 有关更多信息，请参阅[重启策略](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/restart_strategies.html)。


<br/>

******

# 五、[Queryable State Beta](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/queryable_state.html) （可查询状态 测试版）

本节目录 |
:---- | 
 [Architecture](#5.1) (架构)  |
 [Activating Queryable State](#5.2) (激活可查询状态) |
 [Making State Queryable](#5.3) （使状态可插叙） |
 &nbsp; &nbsp;  [Queryable State Stream](#5.3.1) （可查询状态流）|
 &nbsp; &nbsp;  [Managed Keyed State](#5.3.2) （Managed Keyed 状态）|
 [Querying State](#5.4) （查询状态）|
 &nbsp; &nbsp;  [Example](#5.4.1) （例）|
 [Configuration](#5.5) （组态） |
 &nbsp; &nbsp;  [State Server](#5.5.1) （状态服务） |
 &nbsp; &nbsp;  [Proxy](#5.5.2) （代理） |
 [Limitations](#5.6) （限制） |
 
 <br/>

**注意**：可查询状态的客户端API当前处于不断发展的状态，并且不保证所提供接口的稳定性。在即将推出的Flink版本中，客户端可能会发生重大的API更改。

简而言之，此功能将Flink的managed keyed（分区）状态（请参阅[Working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)）公开给外部世界，
并允许用户从Flink外部查询作业的状态。 对于某些情况，可查询状态消除了对外部系统（例如键值存储）的分布式 operations/transactions 的需要，这通常是实践中的瓶颈。 此外，此功能对于调试目的可能特别有用。

**注意**：查询状态对象时，无需任何同步或复制即可从并发线程访问该对象。这是一种设计选择，因为上述任何一种都会导致增加的作业延迟，我们希望避免这种情况。 由于任何状态后端使用Java堆空间，
例如`MemoryStateBackend`或`FsStateBackend`在检索值时不能与副本一起使用，而是直接引用存储的值，`读取-修改-写入`模式是不安全的，并且可能导致可查询状态服务由于并发修改而失败。 `RocksDBStateBackend`可以避免这些问题。

## 5.1 Architecture (架构) 
在展示如何使用可查询的状态之前，简要描述组成它的实体是很有用的。可查询的状态功能包含三个主要实体：
1. `QueryableStateClient`，它（可能）在Flink集群之外运行并提交用户查询，
2. `QueryableStateClientProxy`，在每个TaskManager上运行（即在Flink集群内），负责接收客户端的查询，代表他从负责的TaskManager获取请求的状态，并将其返回给客户端，然后
3. `QueryableStateServer`，在每个TaskManager上运行，负责提供本地存储状态。

客户端连接到其中一个代理，并发送与特定键 k 相关联的状态的请求。如[Working with State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html)中所述，
keyed state 在key组中组织，每个TaskManager都分配了许多这些key组。要发现哪个TaskManager负责持有k的key组，代理将询问JobManager。根据答案，代理将查询在该TaskManager上运行的QueryableStateServer以获取与k相关联的状态，并将响应转发回客户端。

## 5.2 Activating Queryable State (激活可查询状态)
要在Flink集群上启用可查询状态，只需将[flink-queryable-state-runtime_2.11-1.8.0.jar](https://repo1.maven.org/maven2/org/apache/flink/flink-queryable-state-runtime_2.11/1.8.0/flink-queryable-state-runtime_2.11-1.8.0.jar)
从[Flink发行版](https://flink.apache.org/downloads.html)的`opt/`文件夹复制到`lib/`文件夹即可。 否则，未启用可查询状态功能。

要验证群集是否在启用了可查询状态的情况下运行，请检查该行的任何任务管理器的日志：“Started the Queryable State Proxy Server @ ...”。

## 5.3 Making State Queryable （使状态可插叙）
现在您已在群集上激活了可查询状态，现在是时候看看如何使用它了。 为了使状态对外界可见，需要使用以下方法明确查询状态：
* `QueryableStateStream`，一个充当接收器的便捷对象，并将其传入值作为可查询状态提供，或者
* `stateDescriptor.setQueryable(String queryableStateName)`方法，它使得状态描述符所代表的keyed state可查询。

以下部分解释了这两种方法的用法。

### 5.3.1 Queryable State Stream （可查询状态流）
在KeyedStream上调用`.asQueryableState(stateName, stateDescriptor)`会返回一个QueryableStateStream，它将其值提供为可查询状态。 根据状态的类型`asQueryableState()`方法有以下变体：
```
// ValueState
QueryableStateStream asQueryableState(
    String queryableStateName,
    ValueStateDescriptor stateDescriptor)

// Shortcut for explicit ValueStateDescriptor variant
QueryableStateStream asQueryableState(String queryableStateName)

// FoldingState
QueryableStateStream asQueryableState(
    String queryableStateName,
    FoldingStateDescriptor stateDescriptor)

// ReducingState
QueryableStateStream asQueryableState(
    String queryableStateName,
    ReducingStateDescriptor stateDescriptor)
```

**注意**：没有可查询的`ListState`接收器，因为它会导致不断增长的列表，这些列表可能无法清除，因此最终会消耗太多内存。

返回的QueryableStateStream可以看作是接收器，无法进一步转换。 在内部`QueryableStateStream`被转换为operator，该operator使用所有传入记录来更新可查询状态实例。 
更新逻辑由`asQueryableState`调用中提供的`StateDescriptor`的类型暗示。 在如下所示的程序中，keyed流的所有记录将用于通过`ValueState.update(value)`更新状态实例：

```
stream.keyBy(0).asQueryableState("query-name")
```

这就像Scala API的flatMapWithState。

### 5.3.2 Managed Keyed State （Managed Keyed 状态）
可以通过`StateDescriptor.setQueryable(String queryableStateName)`查询适当的状态描述符来查询operator的managed keyed状态（请参阅[Using Managed Keyed State](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html#using-managed-keyed-state)），
如下例所示：
```
ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
        new ValueStateDescriptor<>(
                "average", // the state name
                TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {})); // type information
descriptor.setQueryable("query-name"); // queryable state name
```

**注意**：`queryableStateName`参数可以任意选择，仅用于查询。 它不必与状态自己的名字相同。

该变体对于哪种类型的状态可以被查询没有限制。 这意味着它可以用于任何ValueState，ReduceState，ListState，MapState，AggregatingState和当前不推荐使用的FoldingState。

## 5.4 Querying State （查询状态）
到目前为止，您已将集群设置为以可查询状态运行，并且已将（某些）状态声明为可查询状态。 现在是时候看看如何查询这个状态了。

为此，您可以使用`QueryableStateClient`帮助程序类。 这可以在`flink-queryable-state-client`jar中找到，它必须作为项目的pom.xml中的依赖项与`flink-core`一起显式包含，如下所示：
```
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-core</artifactId>
  <version>1.8.0</version>
</dependency>
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-queryable-state-client-java_2.11</artifactId>
  <version>1.8.0</version>
</dependency>
```

有关详细信息，您可以查看如何[设置Flink程序](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/linking_with_flink.html)。

`QueryableStateClient`将您的查询提交给内部代理，然后内部代理将处理您的查询并返回最终结果。初始化客户端的唯一要求是提供有效的TaskManager hostname（请记住，
每个任务管理器上都运行可查询的状态代理）以及代理侦听的端口。 更多有关如何配置代理和状态服务port(s)在[Configuration Section](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/queryable_state.html#Configuration)。

```
QueryableStateClient client = new QueryableStateClient(tmHostname, proxyPort);
```

在客户端准备好的情况下，要查询与类型K的键相关联的类型V的状态，您可以使用以下方法：
```
CompletableFuture<S> getKvState(
    JobID jobId,
    String queryableStateName,
    K key,
    TypeInformation<K> keyTypeInfo,
    StateDescriptor<S, V> stateDescriptor)
```

上面返回一个`CompletableFuture`，最终保存具有ID jobID作业的`queryableStateName`标识的可查询状态实例的状态值。 key是您感兴趣的状态的键，keyTypeInfo将告诉Flink如何序列化/反序列化它。
 最后，`stateDescriptor`包含有关请求状态的必要信息，即其类型（Value，Reduce等）以及有关如何序列化/反序列化它的必要信息。

细心的读者会注意到返回的future包含一个S类值，即一个包含实际值的State对象。这可以是Flink支持的任何状态类型：ValueState，ReduceState，ListState，MapState，AggregatingState和当前不推荐使用的FoldingState。

**注意**：这些状态对象不允许修改包含的状态。 您可以使用它们来获取状态的实际值，例如使用`valueState.get()`，或迭代所包含的`<K，V>`条目，例如使用`mapState.entries()`，
但您无法修改它们。 例如，在返回的列表状态上调用`add()`方法将抛出`UnsupportedOperationException`。

**注意**：客户端是异步的，可以由多个线程共享。它需要在未使用时通过`QueryableStateClient.shutdown()`关闭以释放资源。

### 5.4.1 Example （例）
以下示例通过使其可查询来扩展`CountWindowAverage`示例（请参阅[使用 Managed Keyed 状态](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state.html#using-managed-keyed-state)），并显示如何查询此值：

```
public class CountWindowAverage extends RichFlatMapFunction<Tuple2<Long, Long>, Tuple2<Long, Long>> {

    private transient ValueState<Tuple2<Long, Long>> sum; // 包含 count 和 sum 的 tuple 

    @Override
    public void flatMap(Tuple2<Long, Long> input, Collector<Tuple2<Long, Long>> out) throws Exception {
        Tuple2<Long, Long> currentSum = sum.value();
        currentSum.f0 += 1;
        currentSum.f1 += input.f1;
        sum.update(currentSum);

        if (currentSum.f0 >= 2) {
            out.collect(new Tuple2<>(input.f0, currentSum.f1 / currentSum.f0));
            sum.clear();
        }
    }

    @Override
    public void open(Configuration config) {
        ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
                new ValueStateDescriptor<>(
                        "average", // 状态的名字
                        TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {})); // 类型信息
        descriptor.setQueryable("query-name");
        sum = getRuntimeContext().getState(descriptor);
    }
}

```

在job中使用后，您可以检索job ID，然后从此operator查询任何键的当前状态：

```
QueryableStateClient client = new QueryableStateClient(tmHostname, proxyPort);

// the state descriptor of the state to be fetched.
ValueStateDescriptor<Tuple2<Long, Long>> descriptor =
        new ValueStateDescriptor<>(
          "average",
          TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {}));

CompletableFuture<ValueState<Tuple2<Long, Long>>> resultFuture =
        client.getKvState(jobId, "query-name", key, BasicTypeInfo.LONG_TYPE_INFO, descriptor);

// now handle the returned value
resultFuture.thenAccept(response -> {
        try {
            Tuple2<Long, Long> res = response.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
});
```

## 5.5 Configuration （组态）
以下配置参数会影响可查询状态服务和客户端的行为。它们在`QueryableStateOptions`中定义。

### 5.5.1 State Server （状态服务）
* `queryable-state.server.ports`：可查询状态服务器的服务端口范围。 如果有多个task manager在同一台机器上运行，这对于避免端口冲突很有用。 指定的范围可以是：
端口：“9123”，一系列端口：“50100-50200”，或范围and/or列表：“50100-50200,50300-50400,51234”。 默认端口为9067。
* `queryable-state.server.network-threads`：接收状态服务传入请求的网络（事件循环）线程数(0 => #slots)
* `queryable-state.server.query-threads`：处理/提供状态服务传入请求的线程数 (0 => #slots)。

### 5.5.2  Proxy （代理）
* `queryable-state.proxy.ports`：可查询状态代理的服务器端口范围。 如果有多个task manager在同一台机器上运行，这对于避免端口冲突很有用。 指定的范围可以是：
端口：“9123”，一系列端口：“50100-50200”，或范围and/or列表：“50100-50200,50300-50400,51234”。 默认端口为9069。
* `queryable-state.proxy.network-threads`：接收客户端代理传入请求的网络（事件循环）线程数（0 => #slots）
* `queryable-state.proxy.query-threads`：处理/提供客户端代理的传入请求的线程数（0 => #slots）。

## 5.6 Limitations （限制）
* 可查询状态生命周期与作业的生命周期绑定，例如，任务在启动时注册可查询状态，并在释放时注销它。 在将来的版本中，需要将其解耦以便在任务完成后允许查询，并通过状态复制加速恢复。
* 关于可用KvState的通知通过简单的告知发生。 在未来，应该通过询问和确认来改进这一点。
* 服务器和客户端会跟踪查询的统计信息。默认情况下，这些功能目前处于禁用状态，一旦有更好的支持通过Metrics系统发布这些数字，我们就应该启用统计数据。


<br/>

******

# 六、[State Backends](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/state_backends.html) （状态后端）
Flink提供了不同的状态后端，用于指定状态的存储方式和位置。

State可以位于Java的堆上或堆外。 根据您的状态后端，Flink还可以管理应用程序的状态，这意味着Flink处理内存管理（如果需要可能会溢出到磁盘）以允许应用程序保持非常大的状态。
默认情况下，配置文件`flink-conf.yaml`确定所有Flink作业的状态后端。

但是，可以基于每个作业重写默认状态后端，如下所示。

有关可用状态后端，其优点、限制和配置参数的详细信息，请参阅[Deployment & Operations](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/state/state_backends.html)中的相应部分。

```
val env = StreamExecutionEnvironment.getExecutionEnvironment()
env.setStateBackend(...)
```

<br/>

******

# 七、[State Schema Evolution](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/schema_evolution.html) （状态模式演进）

本节目录 |
:---- | 
[Evolving state schema](#7.1) （不断演进的状态模式） |
[Supported data types for schema evolution](#7.2) （支持状态模式的数据类型） |
&nbsp; &nbsp;  [POJO types](#7.2.1) |
&nbsp; &nbsp;  [Avro types](#7.2.2) |

<br/>

Apache Flink流式应用程序通常设计为无限期或长时间运行。 与所有长期运行的服务一样，需要更新应用程序以适应不断变化的需求。 与应用程序工作的数据模式相同，它们随应用程序一起演进。

此页面概述了如何改进状态类型的数据模式。 当前限制因不同类型和状态结构（ValueState，ListState等）而异。

请注意，仅当您使用由Flink自己的[类型序列化框架](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/types_serialization.html)生成的状态序列化程序时，
此页面上的信息才是相关的。也就是说，在声明您的状态时，提供的状态描述符未配置为使用特定的TypeSerializer或TypeInformation，在这种情况下，Flink会推断有关状态类型的信息：

```
ListStateDescriptor<MyPojoType> descriptor =
    new ListStateDescriptor<>(
        "state-name",
        MyPojoType.class);

checkpointedState = getRuntimeContext().getListState(descriptor);
```

在引擎下是否可以演进状态模型取决于用于读/写持久状态字节的串行器。 简而言之，注册状态的架构只有在其序列化器正确支持它时才能演进。 
这由Flink的类型序列化框架生成的序列化器透明地处理（当前的支持范围[如下](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/schema_evolution.html#supported-data-types-for-schema-evolution)所列）。

如果您打算为状态类型实现自定义TypeSerializer，并希望了解如何实现序列化程序以支持状态模式演变，请参阅[自定义状态序列化](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/custom_serialization.html)。 
那个文档还包含有关状态序列化器和Flink状态后端之间相互作用的必要内部细节，以支持状态模式演变。

## 7.1 Evolving state schema （不断演进的状态模式）
要演进给定状态类型的模式，您将执行以下步骤：
* 获取Flink流式作业的savepoint。
* 更新应用程序中的状态类型（例如，修改Avro类型架构）。
* 从savepoint还原作业。 首次访问状态时，Flink将评估模式是否已更改为状态，并在必要时迁移状态模式。

迁移状态以适应更改的模式的过程自动发生，并且对于每个状态独立发生。 此过程由Flink在内部执行，首先检查状态的新序列化程序是否具有与先前序列化程序不同的序列化模式; 
如果是这样，之前的序列化器用于将状态读取到对象，并使用新的序列化器再次写回字节。

有关迁移过程的更多详细信息超出了本文档的范围; 请参考[这里](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/custom_serialization.html)。

## 7.2 Supported data types for schema evolution （支持状态模式的数据类型）
目前，仅支持POJO和Avro类型的模式演变。 因此，如果您关心状态的模式演变，**目前建议始终使用Pojo或Avro作为状态数据类型**。

有计划扩大对更多复合类型的支持; 有关详细信息，请参阅[FLINK-10896](https://issues.apache.org/jira/browse/FLINK-10896)。

### 7.2.1 POJO types
Flink支持基于以下规则集的[POJO类型](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/types_serialization.html#rules-for-pojo-types)模式演变：
1. 字段可以删除。 删除后，将在以后的检查点和保存点中删除已删除字段的先前值。
2. 可以添加新字段。 根据[Java的定义](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html)，新字段将初始化为其类型的默认值。
3. 声明的字段类型不能更改。
4. POJO类型的类名称不能更改，包括类的namespace。

请注意，只有在使用Flink版本高于1.8.0的先前savepoint进行恢复时，才能演变POJO类型状态的模式。 使用早于1.8.0的Flink版本进行还原时，无法更改此模式。

### 7.2.2 Avro types
Flink完全支持Avro类型状态的演进模式，只要模式更改被[Avro的模式解析规则](http://avro.apache.org/docs/current/spec.html#Schema+Resolution)视为兼容。

一个限制是，当作业恢复时，Avro生成的用作状态类型的类无法重定位或拥有不同的命名空间。


<br/>

******

# 八、[Custom State Serialization](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/stream/state/custom_serialization.html)（自定义状态序列化）

## 8.1 Custom Serialization for Managed State （Managed State的自定义序列化）

本节目录 |
:---- | 
[Using custom state serializers](#8.1) （使用自定义状态序列化） |
[State serializers and schema evolution](#8.2) （状态序列化和模式演变）|
&nbsp; &nbsp;  [The TypeSerializerSnapshot abstraction](#8.2.1) （TypeSerializerSnapshot抽象） |
&nbsp; &nbsp;  [How Flink interacts with the TypeSerializer and TypeSerializerSnapshot abstractions](#8.2.2) （Flink如何与TypeSerializer和TypeSerializerSnapshot抽象交互） |
[Predefined convenient TypeSerializerSnapshot classes](#8.3) （预定义的方便的TypeSerializerSnapshot类）|
&nbsp; &nbsp;  [Implementing a SimpleTypeSerializerSnapshot ](#8.3.1)（实现一个SimpleTypeSerializerSnapshot） |
&nbsp; &nbsp;  [Implementing a CompositeTypeSerializerSnapshot](#8.3.2) （实现一个CompositeTypeSerializerSnapshot） |
[Implementation notes and best practices](#8.4) （实施说明和最佳实践）|
[Migrating from deprecated serializer snapshot APIs before Flink 1.7](#8.5) （从Flink 1.7之前已弃用的序列化程序快照API迁移）|

<br/>

此页面的目标是需要对其状态使用自定义序列化的用户，包括如何提供自定义状态序列化程序以及实现允许状态模式演变的序列化程序的指南和最佳实践。

如果您只是使用Flink自有的序列化程序，则此页面无关紧要，可以忽略。

## 8.2 Using custom state serializers （使用自定义状态序列化）
注册managed operator或keyed state时，需要StateDescriptor指定状态名称以及有关状态类型的信息。 Flink的[类型序列化框架](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/types_serialization.html)
使用类型信息为状态创建适当的序列化程序。

也可以完全绕过这个并让Flink使用您自己的自定义序列化程序来序列化managed states，使用您自己的TypeSerializer实现来直接实例化StateDescriptor：
```
class CustomTypeSerializer extends TypeSerializer[(String, Integer)] {...}

val descriptor = new ListStateDescriptor[(String, Integer)](
    "state-name",
    new CustomTypeSerializer)
)

checkpointedState = getRuntimeContext.getListState(descriptor)
```

## 8.3  State serializers and schema evolution （状态序列化和模式演变）
本节介绍与状态序列化和模式演变相关的面向用户的抽象，以及有关Flink如何与这些抽象交互的必要内部详细信息。

从savepoint恢复时，Flink允许更改用于读取和写入先前注册状态的序列化程序，以便用户不会锁定到任何特定的序列化模式。恢复状态时，
将为状态注册新的序列化程序（即，用于访问还原作业中的状态的StateDescriptor附带的序列化程序）。此新序列化程序可能具有与先前序列化程序不同的架构。因此，
在实现状态序列化器时，除了读/写数据的基本逻辑之外，还要记住的另一个重要事项是如何在将来更改序列化模式。

当谈到模式时，在该上下文中，该术语在引用状态类型的数据模型和状态类型的序列化二进制格式之间是可互换的。一般来说，架构可能会在以下几种情况下发生变化：
1. 状态类型的数据模式已经改变，即从用作状态的POJO添加或删除字段。
2. 一般来说，在更改数据模式后，需要升级序列化程序的序列化格式。
3. 序列化的配置已更改。

为了使新执行具有关于所写状态的状态的信息并检测模式是否已经改变，在获取operator状态的保存点时，需要将状态序列化的快照与状态字节一起写入。 这被抽象为TypeSerializerSnapshot，在下一小节中进行了解释。

### 8.3.1 The TypeSerializerSnapshot abstraction （TypeSerializerSnapshot抽象）
```
public interface TypeSerializerSnapshot<T> {
    int getCurrentVersion();
    void writeSnapshot(DataOuputView out) throws IOException;
    void readSnapshot(int readVersion, DataInputView in, ClassLoader userCodeClassLoader) throws IOException;
    TypeSerializerSchemaCompatibility<T> resolveSchemaCompatibility(TypeSerializer<T> newSerializer);
    TypeSerializer<T> restoreSerializer();
}
```

```
public abstract class TypeSerializer<T> {    
    
    // ...
    
    public abstract TypeSerializerSnapshot<T> snapshotConfiguration();
}
```

序列化程序的`TypeSerializerSnapshot`是一个时间点信息，作为状态序列化程序写入模式的单一事实来源，以及恢复与给定时间点相同的序列化程序所必需的任何其他信息。
关于在恢复时应该写入和读取的内容的逻辑，因为在`writeSnapshot`和`readSnapshot`方法中定义了序列化器快照。

请注意，快照自己的写入架构可能还需要随时间更改（例如，当您希望向快照添加有关序列化程序的更多信息时）。为此，快照的版本化，并在`getCurrentVersion`方法中定义当前版本号。
在还原时，从savepoint读取序列化程序快照时，将在其中写入快照的架构版本提供给`readSnapshot`方法，以便读取实现可以处理不同的版本。

在还原时，应在`resolveSchemaCompatibility`方法中实现检测新序列化程序的架构是否已更改的逻辑。在恢复的Operator执行中使用新的序列化程序再次注册先前的已注册状态时，
新的序列化程序将通过此方法提供给先前的序列化程序的快照。此方法返回`TypeSerializerSchemaCompatibility`，表示兼容性解析的结果，可以是以下之一：
1. **TypeSerializerSchemaCompatibility.compatibleAsIs()**: 此结果表明新串行器是兼容的，这意味着新的串行器与先前的串行器具有相同的架构。 可能已在`resolveSchemaCompatibility`方法中重新配置了新的序列化程序，以使其兼容。
2. **TypeSerializerSchemaCompatibility.compatibleAfterMigration()**: 此结果表明新的序列化程序具有不同的序列化架构，并且可以通过使用先前的序列化程序（识别旧架构）从旧架构迁移以将字节读取到状态对象，然后将对象重写为字节使用新的序列化程序（识别新架构）。
3. **TypeSerializerSchemaCompatibility.incompatible()**: 此结果表明新的序列化程序具有不同的序列化架构，但无法从旧架构迁移。

最后一点详细说明了在需要迁移的情况下如何获得先前的序列化器。序列化程序的`TypeSerializerSnapshot`的另一个重要作用是它用作恢复先前序列化程序的工厂。 
更具体地说，`TypeSerializerSnapshot`应该实现`restoreSerializer`方法来实例化一个识别前一个序列化程序的模式和配置的序列化程序实例，因此可以安全地读取前一个序列化程序写入的数据。

### 8.3.2 How Flink interacts with the TypeSerializer and TypeSerializerSnapshot abstractions （Flink如何与TypeSerializer和TypeSerializerSnapshot抽象交互）
总结一下，本节总结了Flink，或者更具体地说是状态后端如何与抽象交互。根据状态后端，交互略有不同，但这与状态序列化程序及其序列化程序快照的实现是正交的。

#### Off-heap state backends (e.g. RocksDBStateBackend) （堆外状态后端，例如RocksDBStateBackend）
1. Register new state with a state serializer that has schema A （使用具有模式A的状态序列化程序注册新状态）
    * 状态的已注册TypeSerializer用于在每次状态访问时读/写状态。
    * 状态在模式A中写。
2. Take a savepoint（取得一个savepoint）
    * 通过`TypeSerializer＃snapshotConfiguration`方法提取序列化程序快照。
    * 序列化程序快照将写入savepoint，以及已经序列化的状态字节（使用架构A）。
3. Restored execution re-accesses restored state bytes with new state serializer that has schema B(使用新状态序列化器的模式B来恢复执行重新访问恢复的状态字节)
    * 恢复先前的状态序列化程序的快照。
    * 状态字节在还原时不反序列化，仅加载回状态后端（因此，仍在模式A中）。
    * 收到新的序列化程序后，它将通过`TypeSerializer＃resolveSchemaCompatibility`提供给已恢复的先前序列化程序的快照，以检查架构兼容性。
4. Migrate state bytes in backend from schema A to schema B（将后端中的状态字节从架构A迁移到架构B）
    * 如果兼容性解决方案反映了架构已更改并且可以进行迁移，则会执行架构迁移。 识别模式A的先前状态序列化程序将通过`TypeSerializerSnapshot＃restoreSerializer()`从序列化程序快照中获取，
    并用于将状态字节反序列化为对象，然后使用新的序列化程序重新编写，后者识别模式B完成迁移。 在继续处理之前，将访问状态的所有条目全部迁移。
    * 如果解析信号不兼容，则状态访问失败并出现异常。

#### Heap state backends (e.g. MemoryStateBackend, FsStateBackend) （堆状态后端，例如MemoryStateBackend，FsStateBackend ）
1. Register new state with a state serializer that has schema A （使用具有模式A的状态序列化程序注册新状态）
    * 注册的TypeSerializer由状态后端维护。
2. Take a savepoint, serializing all state with schema A （获取savepoint，使用模式A序列化所有状态）
    * 通过`TypeSerializer＃snapshotConfiguration`方法提取序列化程序快照。
    * 序列化程序快照将写入保存点。
    * 现在，状态对象被序列化为savepoint，以模式A编写。
3. On restore, deserialize state into objects in heap （在还原时，将状态反序列化为堆中的对象）
    * 恢复先前的状态序列化程序的快照。
    * 识别模式A的先前序列化程序是通过`TypeSerializerSnapshot#restoreSerializer()`从序列化程序快照获取的，用于将状态字节反序列化为对象。
    * 从现在开始，所有的状态都已经反序列化了。
4. Restored execution re-accesses previous state with new state serializer that has schema B（使用新状态序列化器的模式B来恢复执行重新访问恢复的状态字节）
    * 收到新的序列化程序后，它将通过`TypeSerializer＃resolveSchemaCompatibility`提供给已恢复的先前序列化程序的快照，以检查架构兼容性。
    * 如果兼容性检查发出需要迁移的信号，则在这种情况下不会发生任何事情，因为对于堆后端，所有状态都已反序列化为对象。
    * 如果解析信号不兼容，则状态访问失败并出现异常。
5. Take another savepoint, serializing all state with schema B（拿另一个savepoint，使用模式B序列化所有状态）
    * 与步骤2相同，但现在状态字节全部在模式B中。

## 8.4 Predefined convenient TypeSerializerSnapshot classes （预定义的方便的TypeSerializerSnapshot类）
Flink提供了两个可用于典型场景的抽象基类`TypeSerializerSnapshot`类：`SimpleTypeSerializerSnapshot`和`CompositeTypeSerializerSnapshot`。

提供这些预定义快照作为其序列化程序快照的序列化程序必须始终具有自己的独立子类实现。这对应于不在不同序列化程序之间共享快照类的最佳实践，这将在下一节中进行更全面的解释。

### 8.4.1 Implementing a SimpleTypeSerializerSnapshot （实现一个SimpleTypeSerializerSnapshot）
`SimpleTypeSerializerSnapshot`适用于没有任何状态或配置的序列化程序，这实际上意味着序列化程序的序列化模式仅由序列化程序的类定义。

使用`SimpleTypeSerializerSnapshot`作为序列化程序的快照类时，只有2种可能的兼容性解析结果：
* `TypeSerializerSchemaCompatibility.compatibleAsIs()`, 如果新的序列化程序类保持相同，或
* `TypeSerializerSchemaCompatibility.incompatible()`, 如果新的序列化程序类与前一个类不同。

下面是如何使用`SimpleTypeSerializerSnapshot`的示例，使用Flink的IntSerializer作为示例：
```
public class IntSerializerSnapshot extends SimpleTypeSerializerSnapshot<Integer> {
    public IntSerializerSnapshot() {
        super(() -> IntSerializer.INSTANCE);
    }
}
```

`IntSerializer`没有状态或配置。 序列化格式仅由序列化程序类本身定义，并且只能由另一个IntSerializer读取。 因此，它适合SimpleTypeSerializerSnapshot的用例。

`SimpleTypeSerializerSnapshot`的基础超类构造函数需要相应序列化程序的提供者实例，无论快照当前是在恢复还是在快照期间写入。 该提供者用于创建还原序列化程序，以及类型检查以验证新的序列化程序是否与预期的序列化程序类相同。

### 8.4.2 Implementing a CompositeTypeSerializerSnapshot （实现一个CompositeTypeSerializerSnapshot）
`CompositeTypeSerializerSnapshot`适用于依赖多个嵌套序列化程序进行序列化的序列化程序。

在进一步解释之前，我们将依赖于多个嵌套序列化器的串行器称为此上下文中的“外部”序列化器。这方面的例子可能是MapSerializer，ListSerializer，GenericArraySerializer等。例如，
考虑MapSerializer - 键和值序列化器将是嵌套的序列化器，而MapSerializer本身就是“外部”序列化器。

在这种情况下，外部序​​列化程序的快照还应包含嵌套序列化程序的快照，以便可以独立检查嵌套序列化程序的兼容性。在解析外部序列化程序的兼容性时，需要考虑每个嵌套序列化程序的兼容性。

提供`CompositeTypeSerializerSnapshot`以帮助实现这些复合序列化器的快照。它涉及读取和编写嵌套的序列化程序快照，以及解决最终的兼容性结果，同时考虑到所有嵌套序列化程序的兼容性。

下面是如何使用`CompositeTypeSerializerSnapshot`的示例，使用Flink的MapSerializer作为示例：
```
public class MapSerializerSnapshot<K, V> extends CompositeTypeSerializerSnapshot<Map<K, V>, MapSerializer> {

    private static final int CURRENT_VERSION = 1;

    public MapSerializerSnapshot() {
        super(MapSerializer.class);
    }

    public MapSerializerSnapshot(MapSerializer<K, V> mapSerializer) {
        super(mapSerializer);
    }

    @Override
    public int getCurrentOuterSnapshotVersion() {
        return CURRENT_VERSION;
    }

    @Override
    protected MapSerializer createOuterSerializerWithNestedSerializers(TypeSerializer<?>[] nestedSerializers) {
        TypeSerializer<K> keySerializer = (TypeSerializer<K>) nestedSerializers[0];
        TypeSerializer<V> valueSerializer = (TypeSerializer<V>) nestedSerializers[1];
        return new MapSerializer<>(keySerializer, valueSerializer);
    }

    @Override
    protected TypeSerializer<?>[] getNestedSerializers(MapSerializer outerSerializer) {
        return new TypeSerializer<?>[] { outerSerializer.getKeySerializer(), outerSerializer.getValueSerializer() };
    }
}
```

将新的序列化程序快照实现为`CompositeTypeSerializerSnapshot`的子类时，必须实现以下三种方法：
* `#getCurrentOuterSnapshotVersion()`: 此方法定义当前外部序列化程序快照的序列化二进制格式的版本。
* `#getNestedSerializers(TypeSerializer)`: 给定外部序列化程序，返回其嵌套的序列化程序。
* `#createOuterSerializerWithNestedSerializers(TypeSerializer[])`: 给定嵌套的序列化程序，创建外部序列化程序的实例。

上面的示例是`CompositeTypeSerializerSnapshot`，除了嵌套的序列化程序的快照之外，没有额外的信息要进行快照。 因此，其外部快照版本可能永远不需要上升。 但是，
其他一些序列化程序包含一些需要与嵌套组件序列化程序一起保存的其他静态配置。这方面的一个例子是Flink的`GenericArraySerializer`，除了嵌套的元素序列化器之外，它还包含数组元素类的类。

在这些情况下，需要在`CompositeTypeSerializerSnapshot`上实现另外三种方法：
* `#writeOuterSnapshot(DataOutputView)`: 定义外部快照信息的写入方式。
* `#readOuterSnapshot(int, DataInputView, ClassLoader)`: 定义如何读取外部快照信息。
* `#isOuterSnapshotCompatible(TypeSerializer)`: 检查外部快照信息是否保持相同。

默认情况下，`CompositeTypeSerializerSnapshot`假定没有任何外部快照信息可供读/写，因此上述方法具有空的默认实现。 如果子类具有外部快照信息，则必须实现所有三种方法。

下面是使用Flink的`GenericArraySerializer`作为示例，将`CompositeTypeSerializerSnapshot`用于具有外部快照信息的复合串行器快照的示例：
```
public final class GenericArraySerializerSnapshot<C> extends CompositeTypeSerializerSnapshot<C[], GenericArraySerializer> {

    private static final int CURRENT_VERSION = 1;

    private Class<C> componentClass;

    public GenericArraySerializerSnapshot() {
        super(GenericArraySerializer.class);
    }

    public GenericArraySerializerSnapshot(GenericArraySerializer<C> genericArraySerializer) {
        super(genericArraySerializer);
        this.componentClass = genericArraySerializer.getComponentClass();
    }

    @Override
    protected int getCurrentOuterSnapshotVersion() {
        return CURRENT_VERSION;
    }

    @Override
    protected void writeOuterSnapshot(DataOutputView out) throws IOException {
        out.writeUTF(componentClass.getName());
    }

    @Override
    protected void readOuterSnapshot(int readOuterSnapshotVersion, DataInputView in, ClassLoader userCodeClassLoader) throws IOException {
        this.componentClass = InstantiationUtil.resolveClassByName(in, userCodeClassLoader);
    }

    @Override
    protected boolean isOuterSnapshotCompatible(GenericArraySerializer newSerializer) {
        return this.componentClass == newSerializer.getComponentClass();
    }

    @Override
    protected GenericArraySerializer createOuterSerializerWithNestedSerializers(TypeSerializer<?>[] nestedSerializers) {
        TypeSerializer<C> componentSerializer = (TypeSerializer<C>) nestedSerializers[0];
        return new GenericArraySerializer<>(componentClass, componentSerializer);
    }

    @Override
    protected TypeSerializer<?>[] getNestedSerializers(GenericArraySerializer outerSerializer) {
        return new TypeSerializer<?>[] { outerSerializer.getComponentSerializer() };
    }
}
```

上面的代码片段中有两点需要注意。 首先，由于此`CompositeTypeSerializerSnapshot`实现具有作为快照的一部分编写的外部快照信息，
因此只要外部快照信息的序列化格式发生更改，就必须升级由`getCurrentOuterSnapshotVersion()`定义的外部快照版本。

其次，请注意**在编写组件类时我们如何避免使用Java序列化，只需编写类名并在读回快照时动态加载它**。
避免用于编写串行器快照内容的Java序列化通常是一个很好的做法。 有关这方面的更多详细信息将在下一节中介绍。

## 8.5 Implementation notes and best practices （实施说明和最佳实践）
### 1. Flink通过使用classname实例化它们来恢复序列化程序快照
序列化程序的快照是注册状态序列化的唯一真实来源，它是savepoint中读取状态的入口点。 为了能够恢复和访问先前的状态，必须能够恢复先前的状态序列化程序的快照。

Flink通过首先使用其classname（与快照字节一起写入）实例化TypeSerializerSnapshot来恢复序列化程序快照。 因此，为了避免出现意外的类名更改或实例化失败，TypeSerializerSnapshot类应该：
* 避免被实现为匿名类或嵌套类，
* 有一个public，无参的构造函数用于实例化

### 2. 避免跨不同的序列化程序共享相同的TypeSerializerSnapshot类
由于架构兼容性检查是通过序列化程序快照，因此让多个序列化程序返回与其快照相同的`TypeSerializerSnapshot`类会使
`TypeSerializerSnapshot#resolveSchemaCompatibility`和`TypeSerializerSnapshot#restoreSerializer()`方法的实现复杂化。

这也是对问题的严重分离; 单个序列化程序的序列化架构，配置以及如何还原它应该合并到其自己的专用TypeSerializerSnapshot类中。

### 3. 避免对序列化程序快照内容使用Java序列化
在编写持久化串行器快照的内容时，不应使用Java序列化。 例如，一个序列化程序需要将其目标类型的类保留为其快照的一部分。 有关该类的信息应该通过编写类名来保留，
而不是使用Java直接序列化该类。 读取快照时，将读取类名，并用于通过名称动态加载类。

此做法可确保始终可以安全地读取序列化程序快照。在上面的示例中，如果使用Java序列化持久保存类型类，则一旦类实现发生更改，快照可能不再可读，并且根据Java序列化细节不再具有二进制兼容性。

## 8.6 Migrating from deprecated serializer snapshot APIs before Flink 1.7 （从Flink 1.7之前已弃用的序列化程序快照API迁移）
本节是从Flink 1.7之前存在的序列化程序和序列化程序快照进行API迁移的指南。

在Flink 1.7之前，序列化程序快照是作为`TypeSerializerConfigSnapshot`实现的（现在已弃用，并且最终将被删除以完全替换为新的`TypeSerializerSnapshot`接口）。 
此外，序列化器模式兼容性检查的责任在TypeSerializer中生效，在`TypeSerializer#ensureCompatibility(TypeSerializerConfigSnapshot)`方法中实现。

新旧抽象之间的另一个主要区别是，已弃用的~~TypeSerializerConfigSnapshot~~无法实例化先前的序列化程序。 因此，如果序列化程序仍将~~TypeSerializerConfigSnapshot~~的子类作为其快照返回，
则序列化程序实例本身将始终使用Java序列化写入保存点，以便在还原时可以使用先前的序列化程序。 这是非常不合需要的，因为恢复作业是否成功容易受到先前序列化程序类的可用性的影响，或者通常，
是否可以使用Java序列化在恢复时读回序列化程序实例。 这意味着您只能使用适用于您的状态的相同序列化程序，并且一旦您想要升级序列化程序类或执行架构迁移，就可能会出现问题。

为了面向未来并具有迁移状态序列化程序和模式的灵活性，强烈建议从旧的抽象中进行迁移。 执行此操作的步骤如下：
1. 实现`TypeSerializerSnapshot`的新子类。 这将是序列化程序的新快照。
2. 在`TypeSerializer#snapshotConfiguration()`方法中将新的`TypeSerializerSnapshot`作为序列化程序的序列化程序快照返回。
3. 从Flink 1.7之前存在的savepoint还原作业，然后再次使用savepoint。 请注意，在此步骤中，序列化程序的旧~~TypeSerializerConfigSnapshot~~必须仍存在于类路径中，并且**不得删除**
`TypeSerializer#ensureCompatibility(TypeSerializerConfigSnapshot)`方法的实现。 此过程的目的是使用新实现的序列化程序`TypeSerializerSnapshot`替换旧savepoint中编写的~~TypeSerializerConfigSnapshot~~。
4. 使用Flink 1.7获取保存点后，保存点将包含`TypeSerializerSnapshot`作为状态序列化程序快照，并且不再在保存点中写入序列化程序实例。此时，
现在可以安全地删除旧抽象类的所有实现（从序列化器中删除旧的~~TypeSerializerConfigSnapshot~~实现以及`TypeSerializer#ensureCompatibility(TypeSerializerConfigSnapshot)`）。


<br/><br/><br/>

--------

<br/><br/>


Flink状态管理与恢复
----

# 状态（State）
* 定义
    - 某task/operator在某时刻的一个中间结果
    - 快照(snapshot)
    
* 作用
    - State可以被记录，在失败的情况下可以恢复


## State类型
状态的基本类型有两类：Operator state、 Keyed state

* Operator state
    - 唯一绑定到特定operator
    - 与key无关
    
* Keyed State
    - 基于KeyStream之上的状态，dataStream.keyBy()
    - KeyBy之后的Operator State
    - Organized by keyGroups

* Keyed state
    - ListState<T>

## Managed Operator State 案例
[CountWithOperatorState.java](https://github.com/streaming-olap/training/blob/master/flink-state-example/src/main/java/com/xiaoxiang/function/CountWithOperatorState.java)

* 数据结构
    - ValueState<T>
    - ListState<T>
    - ReducingState<T>
    - MapState<UK, UV>


## Repartition Key State
* 将Key分为group
* 每个key分配到唯一的group
* 将group分配给task
* Key group 由最大的并行度的大小所决定
* \[K, G] 取值方式
    * hash = hash(key)
    * KG = hash % numOfKeyGroups
    * Subtask = KG * parallelism / numOfKeyGroups
    

## Rescale (重新分配)


# 状态容错
* 依靠checkpoint 机制
* exactly-once (只能保证flink 系统内，对于sink 和 source 需要依赖的外部的组件一同保证)


# checkpoint
* 概念
    * 全局快照，持久化保存所有的task/operator的State
    * 序列化数据集合
    
* 特点
    * 异步
    * 全量 或 增量
    * Barrier机制
    * 失败情况可回滚致最近一次成功的checkpoint
    * 周期性
    
## Checkpoint 参数
* 默认checkpointing 是disabled
* Checkpointing开启后，默认的CheckpointMode是 exactly-once
* Checkpointing的checkpointMode有两种，一种 AT_LEAST_ONCE，另一种是EXACTLY_ONCE ,
两种的实现原理不一样，区别在checkpointing的时候是否需要对齐


```
# checkpoint 周期
env.enableCheckpointing(60000L)
# checkpointMode
env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
# 最小间隔时间
env.getCheckpointConfig.setMinPauseBetweenCheckpoints(30000L)
# 超时时间
env.getCheckpointConfig.setCheckpointTimeout(8000L)
```

## Checkpoint 过程
* JobManager发送Snapshot消 息 到source0
* Source0收到消息以后确认 受到消息
* Source0向下游算子发送Barrier
* Source0执行snapshot，发送成功消息致JobManager
* 下游各算子收齐所有的Barrier（先放到input buffer中），并立刻分别再向各自的下游发送Barrier
* 然后下游的算子成功执行snapshot，并发送成功确认消息致JobManager


## barrier的作用
barrier来划分快照


# Restart Strategies （重启策略）
* fixed-delay
```
// restart-strategy.fixed-delay.attempts: 3 
// restart-strategy.fixed-delay.delay: 10 s


env.setRestartStrategy(RestartStrategies.fixedDelayRestart( 3, Time.of(10, TimeUnit.SECONDS) ))
```

* failure-rate
```
// restart-strategy.failure-rate.max-failures-per-interval = 3
// restart-strategy.failure-rate.failure-rate-interval = 5 min
// restart-strategy.failure-rate.delay = 10 s

env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.of(5, TimeUnit.MINUTES), Time.of(10, TimeUnit.SECONDS) ))
```

* none
```
env.setRestartStrategy(RestartStrategies.noRestart())
```


# Savepoint
* 概念
    * 让时光倒流的语法糖
    * 全局、一致性快照，如数据源offset、并行操作状态
    * 可以从应用在过去的任意做了savepoint的时刻开始继续消费


* 作用
    * 开发新版本，应用重新发布
    * 业务迁移，集群需要迁移，不容许数据丢失

* 触发
    * 用户在特定时刻手动触发
    * ① 获取Job ID : `./bin/flink list -r –yid application_id`，或者 Flink UI
    * ② 触发： `bin/flink savepoint :jobId [:targetDirectory]`
    * ③ 查看：Flink UI -> Checkpoints -> Details for Checkpoint xx
    * ④ 或者取消：`bin/flink cancel -s [:targetDirectory] :jobId`


