Flink Connector
====

* 1 常用Connector
    - 1.1 基本Source
    - 1.2 队列系统（Source和Sink）
    - 1.3 存储系统（Sink）
    - 1.4 Source 容错性保证
* 2 Sink
    - 2.1 基本Sink
    - 2.2 Sink 容错性保证
* 3 自定义Source与Sink   
* 4 应用场景
* 5 生产中FlinkKafkaConnector问题


------


# 1 常用Connector

## 1.1 基本Source
* Elements 和 Collections
* Sockets
* FileSystem


## 1.2 队列系统（Source和Sink）
* 支持Source和Sink
* Kafka
* RabbitMQ


## 1.3 存储系统（Sink）
* 支持Sink
* HDFS
* ElasticSearch
* Redis

## 1.4 Source 容错性保证
Source | 语义保证 | 备注
---- | ---- | ----
Apache Kafka | exactly once | 建议0.10版本及以上
AWS Kinesis Streams | exactly once | 
RabbitMQ | at most once(v0.10) / exactly once(v1.0) | 
Twitter Streaming API | at most once | 
Collections | exactly once | 
Files | exactly once | 
Sockets | at most once | 


# 2 Sink
## 2.1 基本Sink
* .stream.println()     **注意** 生产上杜绝使用，采用日志或抽样打印方式
* .stream.writeAsTest("/path"")
* .stream.writeAsCsv("/path")
* .stream.writeToSocket(host, port, SerializationSchema)

## 2.2 Sink 容错性保证
Sink | 语义保证 | 备注
---- | ---- | ----
HDFS rolling sink | exactly once | Implementation depends on Hadoop version
Elasticsearch | at least once | 
Kafka producer | at least once / exactly once | 
Cassandra sink | at least once / exactly once | exactly once only for idempotent updates
AWS Kinesis Streams | at least once | 
File sinks | at least once | 
Socket sinks | at least once
Standard output | at least once
Redis sink | at least once


# 3 自定义Source与Sink
## 3.1 并行度为1的自定义Source
适用配置流，通过广播与事件流做交互。

* 实现
    - 继承`SourceFunction`，实现run方法
    - `cancel`方法需要处理好（cancel应用的时候，这个方法会被调用）
    - 经常不需要做容错保证


## 3.2 并行化的自定义Source
适用于测试，接入新的第三方组件作为Source。

* 实现
    - 继承`ParallelsourceFunction`类（在运行会产生并行化source）
    - 实现切分数据的逻辑
    - 继承Checkpointed类，来保证容错
    - Source拥有回溯读取，以减少状态的保存。

## 3.3 自定义的Source运行与容错保证
* 在应用cancel的时候
* 在应用restarting的时候
* 在应用运行状态

[自定义Source示例](https://github.com/streaming-olap/training/blob/master/flink-connector-example/src/main/java/com/xiaoxiang/flink/source/SimpleSource.java)

## 3.4 自定义Sink
* 继承`SinkFunction`，实现`invoke`方法
* 实现`CheckpointedFunction`，做容错性保证

[自定义Sink示例](https://github.com/streaming-olap/training/blob/master/flink-connector-example/src/main/java/com/xiaoxiang/flink/sink/BufferingSink.java)


## 3.5 自定义Source与Sink测试

## 3.6 Kafka Connector
* Kafka的partition机制，和Flink的并行化数据切分
* Flink可以消费Kafka的topic，和Sink数据到Kafka
* 出现失败，Flink协调Kafka来恢复应用（主要设置Kafka的offset）

## 3.7 FlinkKafkaConsumer
* FlinkKafkaConsumerBase
* FlinkKafkaConsumer09
* FlinkKafkaConsumer010
    - FlinkKafkaConsumer010(String topic, DeserializationSchema<T> valueDeserializer, Properties props)
    - FlinkKafkaConsumer010(String topic, KeyedDeserializationSchema<T> deserializer, Properties props)
    - FlinkKafkaConsumer010(List<String> topics, DeserializationSchema<T> deserializer, Properties props)
    - FlinkKafkaConsumer010(List<String> topics, KeyedDeserializationSchema<T> deserializer, Properties props)

## 3.8 反序列化Schema类型
* DeserializationSchema
    - T deserializer(byte[] message) throws IOException
    - boolean isEndOfStream(T nextElement);	对于Kafka的，返回值为false
* KeyedDeserializationSchema
    - T deserialize(byte[] messageKey, byte[] message, String topic, int partition, long offset) thwos IOException;
    -  boolean isEndOfStream(T nextElement);
* SimpleStringSchema(实现两个反序列化Schema接口和序列化Schema)
* JSONDeserializationSchema


## 3.9 一个FlinkKafkaConsumer的小实例
```
Properties props = new Properties();
props.setProperty("bootstrap.servers", "localhost:9092");
props.setProperty("group.id", "flink_test");

DataStream<String> stream = env.addSource(myConsumer).print()
```


## 3.10 Kafka Consumer 消费策略设置
* startFromEarliest
* startFromLatest
* startFromGroupOffsets		默认的
* startFromSpecificOffsets(Map<TopicPartition, Long>的参数)


## 3.11 Kafka不同情况下的offset消费
* 第一次启动，无savepoint
* 第一次启动，通过savepoing启动
* 有checkpoint，失败后，job恢复的情况
* 无checkpoint，失败后，job恢复的情况


## 3.12 Kafka并行化Source
* 切分数据的方式（通过assign topicPartition）
* 并行度实例分配partition跟并行度ID的有关
* Topic的partition扩大了，出现的问题解决
* 如何现动态加载`Topic`


## 3.13 理解FlinkKafkaSource的容错性
* 通过实现 CheckpointedFunction
* ListState<Tuple2<KafkaTopicPartition, Long>>
* 保证仅消费一次
* 失败恢复设置Kafka Offset，继续消费
* offset commit。checkpoint/no-checkpoint


## 3.14 FlinkKafkaProducer
* FlinkKafkaProducerBase
* FlinkKafkaProducer09
* FlinkKafkaProducer010


## 3.15 KafkaProducer010序列化接口
* SerializationSchema
    - byte[] serialize(T element);
    - Sink跟并行度ID有关
* KeyedSerializationSchema 
    - byte[] serializeKey(T element);
    - byte[] serializeValue(T element);
    - String getTargetTopic(T element);
* SimpleStringSchema

## 3.16 FlinkKafkaProducer 实例
```
val stream: DataStream[String] = ...

val myProducerConfig = FlinkKafkaProducer010.writeToKafkaWithTimestamps( 
    stream,
    "my-topic",
    new SimpleStringSchema, 
    properties)
myProducerConfig.setLogFailuresOnly(false) 
myProducerConfig.setFlushOnCheckpoint(true)
```

## 3.17 理解FlinkKafkaProducer 容错性
* At_LEAST_ONCE
* setLogFailuresOnly(boolean)       默认是false。失败了真么做，默认为false，抛异常
* setFlushOnCheckpoint(boolean)     默认是false (true 保证at_least_once)。设置true才能保证At_LEAST_ONCE
* checkpointedFunction


# 4 应用场景
基于日志进行的分析，实现统计接口某个时间段访问成功率、访问时延、访问次数等的统计。
统计的规则是动态的，

* 要求
    - 支持动态的计算规则
    - 支持select、filter、groupBy
    - 支持`1min`到`60min`的聚合


# 5 生产中FlinkKafkaConnector问题
* 生产中的语义保证（具体跟应用的要求有关）
* Source的反序列化的考虑
* KafkaProducer异常的处理方式（获取Topic partition元数据失败、NotLeader、连接失败等）
    - Retries参数
    - 设置发送失败的策略

