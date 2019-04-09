 Flink Streaming (DataStream API) Operators
 ===
 Application Development / [Streaming (DataStream API)](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/datastream_api.html) / Operators
 
 # [Operators](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/)
 
算子将一个或多个 DataStreams 转换为新的 DataStream。 程序能将多个转换结合成复杂的数据流拓扑。
 本部分介绍了基本的数据转换操作，应用这些内容后的有效物理分区以及对 Flink operator chaining 的见解。
 
 
 目录 |
  ---- |
 DataStream Transformations | 
 Physical partitioning | 
 Task chaining and resource groups |  
 
 
## DataStream Transformations （DataStream 转换）
  
### Scala
Transformation |	Description

1. Map
    * DataStream → DataStream 
    * 采用一个元素并生成一个元素。一个将输入流的值加倍的map函数：
    * `dataStream.map { x => x * 2 }`
    
2. FlatMap
    * DataStream → DataStream
    * 采用一个元素并生成零个、一个、或多个元素。将句子分割为单词的flatmap函数：
    * `dataStream.flatMap { str => str.split(" ") }`

3. Filter
    * DataStream → DataStream
    * 对每个元素计算一个布尔的函数，并保存函数返回true的元素。过滤掉零值的过滤器：
    * `dataStream.filter { _ != 0 }`

4. KeyBy
    * DataStream → KeyedStream
    * 逻辑上将流分区为不相交的分区，每个分区含有相同key的元素。在内部 keyBy 是使用 hash 分区实现的。
    请参阅有关如何制定建的[key](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/api_concepts.html#specifying-keys)。此转换返回KeyedStream。
    ```
    dataStream.keyBy("someKey") // Key by field "someKey"
    dataStream.keyBy(0) // Key by the first element of a Tuple
    ```

5. Reduce
    * KeyedStream → DataStream
    * 在一个 Keyed 化数据流上的“rolling” reduce。将当前元素与最后一个Reduce的值组合并发出新值。 用于创建部分和的流的reduce函数：
    * `keyedStream.reduce { _ + _ }`

6. Fold
    * KeyedStream → DataStream 
    * 具有初始值的被 Keyed 化数据流上的“rolling” 折叠。将当前数据元与最后折叠的值组合并发出新值。折叠函数，当应用于序列（1,2,3,4,5）时，发出序列“start-1”，“start-1-2”，“start-1- 2-3”，. ..
    ```
    val result: DataStream[String] =
        keyedStream.fold("start")((str, i) => { str + "-" + i })
    ```

7. Aggregations
    * KeyedStream → DataStream
    * 在被 Keys 化数据流上滚动聚合。min 和 minBy 之间的差异是 min 返回最小值，而 minBy 返回该字段中具有最小值的数据元（max 和 maxBy 相同）。
    ```
    keyedStream.sum(0)
    keyedStream.sum("key")
    keyedStream.min(0)
    keyedStream.min("key")
    keyedStream.max(0)
    keyedStream.max("key")
    keyedStream.minBy(0)
    keyedStream.minBy("key")
    keyedStream.maxBy(0)
    keyedStream.maxBy("key")
    ``` 

8. Window
    * KeyedStream → WindowedStream
    * Windows可以定义在已经分区的 KeyedStream 上。Windows根据某些特征（例如，在最后5秒内到达的数据）对每个Keys中的数据进行分组。
     有关窗口的完整说明，请参见[windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html)。
    ```
    dataStream.keyBy(0).window(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data
    ```

9. WindowAll
    * DataStream → AllWindowedStream
    * Windows可以在常规DataStream上定义。Windows根据某些特征（例如，在最后5秒内到达的数据）对所有流事件进行分组。
    有关窗口的完整说明，请参见[windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html)。
    **警告**：在许多情况下，这是非并行转换。所有记录将收集在windowAll 算子的一个任务中。
    ```
    dataStream.windowAll(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data
    ```

10. Window Apply
    * WindowedStream → DataStream 
      AllWindowedStream → DataStream
    * 将一般函数应用于整个窗口。下面是一个手动求和窗口数据元的函数。
    **注意**：如果您正在使用windowAll转换，则需要使用AllWindowFunction。
    ```
    windowedStream.apply { WindowFunction }
    
    // applying an AllWindowFunction on non-keyed window stream
    allWindowedStream.apply { AllWindowFunction }
    
    ```

11. Window Reduce
    * WindowedStream → DataStream
    * 将函数 reduce函数应用于窗口并返回reduce的值。
    ```
    windowedStream.reduce { _ + _ } 
    ```

12. Window Fold
    * WindowedStream → DataStream 
    * 将函数 fold函数应用于窗口并返回折叠值。示例函数应用于序列（1,2,3,4,5）时，将序列折叠为字符串“start-1-2-3-4-5”：
    ```
    val result: DataStream[String] =
        windowedStream.fold("start", (str, i) => { str + "-" + i })
    ```

13. Aggregations on windows
    * WindowedStream → DataStream
    * 聚合窗口的内容。min和minBy之间的差异是 min返回最小值，而minBy返回该字段中具有最小值的数据元（max和maxBy相同）。
    ```
    windowedStream.sum(0);
    windowedStream.sum("key");
    windowedStream.min(0);
    windowedStream.min("key");
    windowedStream.max(0);
    windowedStream.max("key");
    windowedStream.minBy(0);
    windowedStream.minBy("key");
    windowedStream.maxBy(0);
    windowedStream.maxBy("key");
        
    ```

14. Union
    * DataStream* → DataStream 
    * 联合两个或多个数据流创建一个包含来自所有流的所有数据元的新流。
    **注意**：如果将数据流与自身联合，则会在结果流中获取两次数据元。
    ```
    dataStream.union(otherStream1, otherStream2, ...);
    ```

15. Window Join
    * DataStream,DataStream → DataStream
    * 在给定Keys和公共window上连接两个数据流。
    ```
    dataStream.join(otherStream)
        .where(<key selector>).equalTo(<key selector>)
        .window(TumblingEventTimeWindows.of(Time.seconds(3)))
        .apply { ... }
    ```

16. Interval Join
    * KeyedStream,KeyedStream → DataStream
    * 在给定的时间间隔的有公共Keys的两个被Key化的数据流上联合两个数据元e1和e2，
    以便e1.timestamp + lowerBound <= e2.timestamp <= e1.timestamp + upperBound
    ```
    // this will join the two streams so that
    // key1 == key2 && leftTs - 2 < rightTs < leftTs + 2
    keyedStream.intervalJoin(otherKeyedStream)
        .between(Time.milliseconds(-2), Time.milliseconds(2)) // lower and upper bound
        .upperBoundExclusive(true) // optional
        .lowerBoundExclusive(true) // optional
        .process(new IntervalJoinFunction() {...});
    ```

17. Window CoGroup
    * DataStream,DataStream → DataStream
    * 在给定Keys和公共窗口上对两个数据流进行Cogroup。
    ```
    dataStream.coGroup(otherStream)
        .where(0).equalTo(1)
        .window(TumblingEventTimeWindows.of(Time.seconds(3)))
        .apply {}
    ```


18. Connect
    * DataStream,DataStream → ConnectedStreams
    * “Connects”两个保留其类型的数据流。连接允许两个流之间的共享状态。
    ```
    someStream : DataStream[Int] = ...
    otherStream : DataStream[String] = ...
    
    val connectedStreams = someStream.connect(otherStream)
        
    ```

19. CoMap, CoFlatMap
    * ConnectedStreams → DataStream
    * 类似于连接数据流上的map和flatMap
    ```
    connectedStreams.map(
        (_ : Int) => true,
        (_ : String) => false
    )
    connectedStreams.flatMap(
        (_ : Int) => true,
        (_ : String) => false
    )
    ```

20. Split
    * DataStream → SplitStream
    * 根据某些标准将流拆分为两个或更多个流。
    ```
    val split = someDataStream.split(
      (num: Int) =>
        (num % 2) match {
          case 0 => List("even")
          case 1 => List("odd")
        }
    )
    ```

21. Select
    * SplitStream → DataStream
    * 从拆分流中选择一个或多个流。
    ```
    val even = split select("even")
    val odd = split select("odd")
    val all = split.select("even", "odd")
    ```

22. Iterate
    * DataStream → IterativeStream → DataStream
    * 通过重定向一个算子的输出到某个先前的算子在流中创建“反馈”循环。这对于定义不断更新模型的算法特别有用。
    以下代码以流开始并连续应用迭代体。大于0的数据元将被发送回反馈通道，其余数据元将向下游转发。
    有关完整说明，请参阅[iterations](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/#iterations)。
    ```
    initialStream.iterate {
      iteration => {
        val iterationBody = iteration.map {/*do something*/}
        (iterationBody.filter(_ > 0), iterationBody.filter(_ <= 0))
      }
    }
    ```

23. Extract Timestamps
    * DataStream → DataStream
    * 从记录中提取时间戳为了使用事件时间语义的窗口。
    查看[Event Time.](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/event_time.html)。
    ```
    stream.assignTimestamps { timestampExtractor }
    ```
  
  
通过匿名名师匹配从tuple、case类和集合中提取，如下所示：
```
val data: DataStream[(Int, String, Double)] = // [...]
data.map {
  case (id, name, temperature) => // [...]
}
```
  
API不支持开箱即用(out-of-the-box)。使用这个功能，你应该使用Scala API扩展。

以下转换可用于元组的数据流：
* lang: Java
* Transformation: Project   DataStream → DataStream
* Description: 从元组中选择字段的子集
    ```
    DataStream<Tuple3<Integer, Double, String>> in = // [...]
    DataStream<Tuple2<String, Integer>> out = in.project(2,0);
    ```

- - - - - - 
### Java
Transformation |	Description

1. Map
    * DataStream → DataStream 
    * 采用一个元素并生成一个元素。一个将输入流的值加倍的map函数：
    ```
    DataStream<Integer> dataStream = //...
    dataStream.map(new MapFunction<Integer, Integer>() {
        @Override
        public Integer map(Integer value) throws Exception {
            return 2 * value;
        }
    });
    ```
    
2. FlatMap
    * DataStream → DataStream
    * 采用一个元素并生成零个、一个、或多个元素。将句子分割为单词的flatmap函数：
    ```
    dataStream.flatMap(new FlatMapFunction<String, String>() {
        @Override
        public void flatMap(String value, Collector<String> out)
            throws Exception {
            for(String word: value.split(" ")){
                out.collect(word);
            }
        }
    });
    ```

3. Filter
    * DataStream → DataStream
    * 对每个元素计算一个布尔的函数，并保存函数返回true的元素。过滤掉零值的过滤器：
    ```
    dataStream.filter(new FilterFunction<Integer>() {
        @Override
        public boolean filter(Integer value) throws Exception {
            return value != 0;
        }
    });
    ```

4. KeyBy
    * DataStream → KeyedStream
    * 逻辑上将流分区为不相交的分区，每个分区含有相同key的元素。在内部 keyBy() 是使用 hash 分区实现的。
    有多种方式指定[specify keys](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/api_concepts.html#specifying-keys)。
    此转换返回KeyedStream，除此，这是使用[keyed state](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/state/state.html#keyed-state)所需的。
    ```
    dataStream.keyBy("someKey") // Key by field "someKey"
    dataStream.keyBy(0) // Key by the first element of a Tuple
    ```
    * **注意** 以下情况，类型不能成为key
        1. 它是POJO类型，但不override hashCode()方法 并依赖于Object.hashCode()实现。
        2. 它是任何类型的数组。

5. Reduce
    * KeyedStream → DataStream
    * 在一个 Keyed 化数据流上的“rolling” reduce。将当前元素与最后一个Reduce的值组合并发出新值。 用于创建部分和的流的reduce函数：
    ```
    keyedStream.reduce(new ReduceFunction<Integer>() {
        @Override
        public Integer reduce(Integer value1, Integer value2)
        throws Exception {
            return value1 + value2;
        }
    });
    ```

6. Fold
    * KeyedStream → DataStream 
    * 具有初始值的被 Keyed 化数据流上的“rolling” 折叠。将当前数据元与最后折叠的值组合并发出新值。折叠函数，当应用于序列（1,2,3,4,5）时，发出序列“start-1”，“start-1-2”，“start-1- 2-3”，. ..
    ```
    DataStream<String> result =
      keyedStream.fold("start", new FoldFunction<Integer, String>() {
        @Override
        public String fold(String current, Integer value) {
            return current + "-" + value;
        }
      });
       
    ```

7. Aggregations
    * KeyedStream → DataStream
    * 在被 Keys 化数据流上滚动聚合。min 和 minBy 之间的差异是 min 返回最小值，而 minBy 返回该字段中具有最小值的数据元（max 和 maxBy 相同）。
    ```
    keyedStream.sum(0)
    keyedStream.sum("key")
    keyedStream.min(0)
    keyedStream.min("key")
    keyedStream.max(0)
    keyedStream.max("key")
    keyedStream.minBy(0)
    keyedStream.minBy("key")
    keyedStream.maxBy(0)
    keyedStream.maxBy("key")
    ``` 

8. Window
    * KeyedStream → WindowedStream
    * Windows可以定义在已经分区的 KeyedStream 上。Windows根据某些特征（例如，在最后5秒内到达的数据）对每个Keys中的数据进行分组。
     有关窗口的完整说明，请参见[windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html)。
    ```
    dataStream.keyBy(0).window(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data
    ```

9. WindowAll
    * DataStream → AllWindowedStream
    * Windows可以在常规DataStream上定义。Windows根据某些特征（例如，在最后5秒内到达的数据）对所有流事件进行分组。
    有关窗口的完整说明，请参见[windows](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/windows.html)。
    **警告**：在许多情况下，这是**非并行**转换。所有记录将收集在windowAll 算子的一个任务中。
    ```
    dataStream.windowAll(TumblingEventTimeWindows.of(Time.seconds(5))) // Last 5 seconds of data
    ```

10. Window Apply
    * WindowedStream → DataStream 
      AllWindowedStream → DataStream
    * 将一般函数应用于整个窗口。下面是一个手动求和窗口数据元的函数。
    **注意**：如果您正在使用windowAll转换，则需要使用AllWindowFunction。
    ```
    windowedStream.apply (new WindowFunction<Tuple2<String,Integer>, Integer, Tuple, Window>() {
        public void apply (Tuple tuple,
                Window window,
                Iterable<Tuple2<String, Integer>> values,
                Collector<Integer> out) throws Exception {
            int sum = 0;
            for (value t: values) {
                sum += t.f1;
            }
            out.collect (new Integer(sum));
        }
    });
    
    // applying an AllWindowFunction on non-keyed window stream
    allWindowedStream.apply (new AllWindowFunction<Tuple2<String,Integer>, Integer, Window>() {
        public void apply (Window window,
                Iterable<Tuple2<String, Integer>> values,
                Collector<Integer> out) throws Exception {
            int sum = 0;
            for (value t: values) {
                sum += t.f1;
            }
            out.collect (new Integer(sum));
        }
    });
    
    ```

11. Window Reduce
    * WindowedStream → DataStream
    * 将函数 reduce函数应用于窗口并返回reduce的值。
    ```
    windowedStream.reduce (new ReduceFunction<Tuple2<String,Integer>>() {
        public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
            return new Tuple2<String,Integer>(value1.f0, value1.f1 + value2.f1);
        }
    });
        
    ```

12. Window Fold
    * WindowedStream → DataStream 
    * 将函数 fold函数应用于窗口并返回折叠值。示例函数应用于序列（1,2,3,4,5）时，将序列折叠为字符串“start-1-2-3-4-5”：
    ```
    windowedStream.fold("start", new FoldFunction<Integer, String>() {
        public String fold(String current, Integer value) {
            return current + "-" + value;
        }
    });
    ```

13. Aggregations on windows
    * WindowedStream → DataStream
    * 聚合窗口的内容。min和minBy之间的差异是 min返回最小值，而minBy返回该字段中具有最小值的数据元（max和maxBy相同）。
    ```
    windowedStream.sum(0);
    windowedStream.sum("key");
    windowedStream.min(0);
    windowedStream.min("key");
    windowedStream.max(0);
    windowedStream.max("key");
    windowedStream.minBy(0);
    windowedStream.minBy("key");
    windowedStream.maxBy(0);
    windowedStream.maxBy("key");
        
    ```

14. Union
    * DataStream* → DataStream 
    * 联合两个或多个数据流创建一个包含来自所有流的所有数据元的新流。
    **注意**：如果将数据流与自身联合，则会在结果流中获取两次数据元。
    ```
    dataStream.union(otherStream1, otherStream2, ...);
    ```

15. Window Join
    * DataStream,DataStream → DataStream
    * 在给定Keys和公共window上连接两个数据流。
    ```
    dataStream.join(otherStream)
        .where(<key selector>).equalTo(<key selector>)
        .window(TumblingEventTimeWindows.of(Time.seconds(3)))
        .apply (new JoinFunction () {...});
    ```

16. Interval Join
    * KeyedStream,KeyedStream → DataStream
    * 在给定的时间间隔的有公共Keys的两个被Key化的数据流上联合两个数据元e1和e2，
    以便e1.timestamp + lowerBound <= e2.timestamp <= e1.timestamp + upperBound
    ```
    // this will join the two streams so that
    // key1 == key2 && leftTs - 2 < rightTs < leftTs + 2
    keyedStream.intervalJoin(otherKeyedStream)
        .between(Time.milliseconds(-2), Time.milliseconds(2)) // lower and upper bound
        .upperBoundExclusive(true) // optional
        .lowerBoundExclusive(true) // optional
        .process(new IntervalJoinFunction() {...});
    ```

17. Window CoGroup
    * DataStream,DataStream → DataStream
    * 在给定Keys和公共窗口上对两个数据流进行Cogroup。
    ```
    dataStream.coGroup(otherStream)
        .where(0).equalTo(1)
        .window(TumblingEventTimeWindows.of(Time.seconds(3)))
        .apply (new CoGroupFunction () {...});
    ```


18. Connect
    * DataStream,DataStream → ConnectedStreams
    * “Connects”两个保留其类型的数据流。连接允许两个流之间的共享状态。
    ```
    DataStream<Integer> someStream = //...
    DataStream<String> otherStream = //...
    
    ConnectedStreams<Integer, String> connectedStreams = someStream.connect(otherStream);
        
    ```

19. CoMap, CoFlatMap
    * ConnectedStreams → DataStream
    * 类似于连接数据流上的map和flatMap
    ```
    connectedStreams.map(new CoMapFunction<Integer, String, Boolean>() {
        @Override
        public Boolean map1(Integer value) {
            return true;
        }
    
        @Override
        public Boolean map2(String value) {
            return false;
        }
    });
    connectedStreams.flatMap(new CoFlatMapFunction<Integer, String, String>() {
    
       @Override
       public void flatMap1(Integer value, Collector<String> out) {
           out.collect(value.toString());
       }
    
       @Override
       public void flatMap2(String value, Collector<String> out) {
           for (String word: value.split(" ")) {
             out.collect(word);
           }
       }
    });
        
    ```

20. Split
    * DataStream → SplitStream
    * 根据某些标准将流拆分为两个或更多个流。
    ```
    SplitStream<Integer> split = someDataStream.split(new OutputSelector<Integer>() {
        @Override
        public Iterable<String> select(Integer value) {
            List<String> output = new ArrayList<String>();
            if (value % 2 == 0) {
                output.add("even");
            }
            else {
                output.add("odd");
            }
            return output;
        }
    });
    ```

21. Select
    * SplitStream → DataStream
    * 从拆分流中选择一个或多个流。
    ```
    SplitStream<Integer> split;
    DataStream<Integer> even = split.select("even");
    DataStream<Integer> odd = split.select("odd");
    DataStream<Integer> all = split.select("even","odd");
    ```

22. Iterate
    * DataStream → IterativeStream → DataStream
    * 通过重定向一个算子的输出到某个先前的算子在流中创建“反馈”循环。这对于定义不断更新模型的算法特别有用。
    以下代码以流开始并连续应用迭代体。大于0的数据元将被发送回反馈通道，其余数据元将向下游转发。
    有关完整说明，请参阅[iterations](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/stream/operators/#iterations)。
    ```
    IterativeStream<Long> iteration = initialStream.iterate();
    DataStream<Long> iterationBody = iteration.map (/*do something*/);
    DataStream<Long> feedback = iterationBody.filter(new FilterFunction<Long>(){
        @Override
        public boolean filter(Long value) throws Exception {
            return value > 0;
        }
    });
    iteration.closeWith(feedback);
    DataStream<Long> output = iterationBody.filter(new FilterFunction<Long>(){
        @Override
        public boolean filter(Long value) throws Exception {
            return value <= 0;
        }
    });
    ```

23. Extract Timestamps
    * DataStream → DataStream
    * 从记录中提取时间戳为了使用事件时间语义的窗口。
    查看[Event Time.](https://ci.apache.org/projects/flink/flink-docs-release-1.7/dev/event_time.html)。
    ```
    stream.assignTimestamps (new TimeStampExtractor() {...});
    ```
    
以下转换可用于元组的数据流：
* lang: Java
* Transformation: Project   DataStream → DataStream
* Description: 从元组中选择字段的子集
    ```
    DataStream<Tuple3<Integer, Double, String>> in = // [...]
    DataStream<Tuple2<String, Integer>> out = in.project(2,0);
    ``` 


## Physical partitioning （物理分区）
Flink也提供了对转换后的精确刘分区的低级别控制（如果需要），通过以下函数：

### Scala
Transformation |	Description
1. Custom partitioning
    * DataStream → DataStream
    * 使用用户定义的分区程序为每个数据元选择目标任务。
    ```
    dataStream.partitionCustom(partitioner, "someKey")
    dataStream.partitionCustom(partitioner, 0)
    ```

2. Random partitioning
    * DataStream → DataStream
    * 根据均匀分布随机分配数据元。
    ```
    dataStream.shuffle();
    ```
    
3. Custom partitioning
    * DataStream → DataStream
    * 分区数据元循环，每个分区创建相等的负载。在存在数据偏斜时用于性能优化。
    ```
    dataStream.rebalance();
    ```

4. Custom partitioning
    * DataStream → DataStream
    * 分区数据元，循环，到下游算子操作的子集。如果您希望的地方拥有管道，这非常有用，
    例如，从数据源的每个并行实例扇出到多个映射器的子集以分配负载但又不希望发生rebalance()会产生的完全Rebalance ，那么这非常有用。
    这将仅需本地数据传输而不是通过网络传输数据，具体取决于其他配置值，例如TaskManagers的插槽数。    
    上游operation算子发送数据元的下游operation算子的子集取决于上游和下游operation算子的并行度。
    例如，如果上游operation算子具有2个并行度并且下游operation算子具有6个并行度，则1个上游operation算子将分配元素到3个下游operation算子，
    而另一个上游operation算子将分配到其他3个下游operation算子。
    另一方面，如果下游operation算子具有2个并行度而上游operation算子具有6个并行度，则3个上游operation算子将分配到1个下游operation算子，
    而其他3个上游operation算子将分配到另1个下游operation算子。  
    在不同并行度不是彼此的倍数的情况下，一个或多个下游operation算子将会有来自上游operation算子的不同数量的输入。  
    请参阅此图以获取可视化的上例中连接模式的可视化：
    ![rescale.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.7/fig/rescale.svg)
    ```
    dataStream.rescale();
    ```
    
5. Custom partitioning
    * DataStream → DataStream
    * 向每个分区广播数据元。
    ```
    dataStream.broadcast();
    ```


### Java
Transformation |	Description

1. Custom partitioning
    * DataStream → DataStream
    * 使用用户定义的分区程序为每个数据元选择目标任务。
    ```
    dataStream.partitionCustom(partitioner, "someKey");
    dataStream.partitionCustom(partitioner, 0);
    ```

2. Random partitioning
    * DataStream → DataStream
    * 根据均匀分布随机分配数据元。
    ```
    dataStream.shuffle();
    ```
    
3. Custom partitioning
    * DataStream → DataStream
    * 分区数据元循环，每个分区创建相等的负载。在存在数据偏斜时用于性能优化。
    ```
    dataStream.rebalance();
    ```

4. Custom partitioning
    * DataStream → DataStream
    * 分区数据元，循环，到下游算子操作的子集。如果您希望的地方拥有管道，这非常有用，
    例如，从数据源的每个并行实例扇出到多个映射器的子集以分配负载但又不希望发生rebalance()会产生的完全Rebalance ，那么这非常有用。
    这将仅需本地数据传输而不是通过网络传输数据，具体取决于其他配置值，例如TaskManagers的插槽数。    
    上游operation算子发送数据元的下游operation算子的子集取决于上游和下游operation算子的并行度。
    例如，如果上游operation算子具有2个并行度并且下游operation算子具有6个并行度，则1个上游operation算子将分配元素到3个下游operation算子，
    而另一个上游operation算子将分配到其他3个下游operation算子。
    另一方面，如果下游operation算子具有2个并行度而上游operation算子具有6个并行度，则3个上游operation算子将分配到1个下游operation算子，
    而其他3个上游operation算子将分配到另1个下游operation算子。  
    在不同并行度不是彼此的倍数的情况下，一个或多个下游operation算子将会有来自上游operation算子的不同数量的输入。  
    请参阅此图以获取可视化的上例中连接模式的可视化：
    ![rescale.svg](https://ci.apache.org/projects/flink/flink-docs-release-1.7/fig/rescale.svg)
    ```
    dataStream.rescale();
    ```
    
5. Custom partitioning
    * DataStream → DataStream
    * 向每个分区广播数据元。
    ```
    dataStream.broadcast();
    ```


## Task chaining and resource groups （任务链和资源组）
链接两个后续transformation算子意味着将它们共同定位在同一个线程中以获得更好的性能。
如果可能的话，Flink通过默认链算子（例如，两个后续的map转换算子）。如果需要，API可以对链接进行细粒度控制：

如果想要在整个job中禁用链使用 `StreamExecutionEnvironment.disableOperatorChaining()`。对于更细粒度的控制，可以使用以下函数。
请注意，这些函数只能在 DataStream 转换后立即使用，因为它们引用了前一个转换。
例如，您可以使用`someStream.map(...).startNewChain()` ，但不能使用 `someStream.startNewChain()` 。

资源组是Flink中的一个插槽，请参阅[solts](https://ci.apache.org/projects/flink/flink-docs-release-1.7/ops/config.html#configuring-taskmanager-processing-slots)。
如果需要，您可以在单独的slots中手动隔离operation算子。

### Scala
Transformation |	Description
1. 开始新的 chain
    * 从这个算子开始，开始一个新的链。两个映射器将被链接，并且过滤器将不会链接到第一个映射器。
    ```
    someStream.filter(...).map(...).startNewChain().map(...)
    ```

2. 禁用链
    * 不要链接Map算子
    ```
    someStream.map(...).disableChaining()
    ```
    
3. 设置slot共享组
    * 设置一个算子的槽共享组。Flink将把具有相同槽共享组的算子操作放入同一个槽，同时保持其他槽中没有槽共享组的算子操作。这可用于隔离槽。
    如果所有输入operation算子都在同一个槽共享组中，则槽共享组将继承输入算子操作。
    默认槽共享组的名称为“default”，可以通过调用`slotSharingGroup（“default”）`将算子操作显式放入此组中。
    ```
    someStream.filter(...).slotSharingGroup("name")
    ```

### Java
Transformation |	Description

1. 开始新的 chain
    * 从这个算子开始，开始一个新的链。两个映射器将被链接，并且过滤器将不会链接到第一个映射器。
    ```
    someStream.filter(...).map(...).startNewChain().map(...);
    ```

2. 禁用链
    * 不要链接Map算子
    ```
    someStream.map(...).disableChaining();
    ```
    
3. 设置slot共享组
    * 设置一个算子的槽共享组。Flink将把具有相同槽共享组的算子操作放入同一个槽，同时保持其他槽中没有槽共享组的算子操作。这可用于隔离槽。
    如果所有输入operation算子都在同一个槽共享组中，则槽共享组将继承输入算子操作。
    默认槽共享组的名称为“default”，可以通过调用`slotSharingGroup（“default”）`将算子操作显式放入此组中。
    ```
    someStream.filter(...).slotSharingGroup("name");
    ```

---
end

可以查看我的博客 [Flink Streaming (DataStream API) Operators (flink 1.7 文档)](https://blog.csdn.net/github_39577257/article/details/88874580)
 