DataStream
----

* WorldCount
* Graph
* DataStreamContext环境
* DataSource
* Transformation
* Sink
* 应用策略配置


# 1. Graph
## StreamGraph
* 根据用户代码生成最初的图
* 表示程序的拓扑结构
* 在client端生成

## 1.1 JobGraph
* 优化streamGraph
* 将多个符合条件的Node chain在一起
* 在client端生成


## 1.2 ExecutionGraph
JobManger根据JobGraph生成，并行化

## 1.3 物理执行图
实际执行图，不可见

## 1.4 StreamGraph -> JobGraph
* StreamNoe 转成 JobVertex
* StreamEdge 转为 JobEdge
* 将多个StreamNode Chain为一个JobVertex

* 根据group指定JboVertex所属SlotSharingGroup
* 配置checkpoint
* 配置重启策略

## 1.5 JobGraph -> ExecutionGraph
* JobVertex 转成 ExecutionJobVertex
* ExecutionVertex 并发任务
* JobEdge 转为 ExecutionEdge
* 是一个JobGraph的2维结构，根据2维结构分发对应Vertex到指定slot


# 2 DataSource
fromElements()  
fromCollection()  
自定义source

# 3 应用策略配置
## 3.1 Checkpoint配置
配置 | 描述
--- | ---
env.enableCheckpointing(interval : Long)   |    设置Checkpoint时间间隔
env.getCheckpointConfig.setMinPauseBetweenCheckpoints(long minPauseBetweenCheckpoints) | 设置检查点之间最小的暂停时间
env.getCheckpointConfig.setCheckpointTimeout(long checkpointTimeout) | 设置检查点在被丢弃之前可能需要的最长时间。

## 3.2 时间模型
* ProcessingTime
* IngestionTime
* EventTime
```
import org.apache.flink.streaming.api.TimeCharacteristic
env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
```

## 3.3 重启策略配置
* 默认：          使用默认的重启策略，以环境的配置文件为准
* fixed-delay：  策略固定间隔
* failure-rate： 失败率
* none：         无重启

### 3.3.1 固定策略方式
例如：`间隔10秒，尝试重启3次`

* 方法一：配置：flink-conf.yaml
```yaml
restart-strategy: fixed-delay 
restart-strategy.fixed-delay.attempts: 3 
restart-strategy.fixed-delay.delay: 10 s
```

* 方法二：代码设置
```
env.setRestartStrategy(
    RestartStrategies.fixedDelayRestart( 3, Time.of(10, TimeUnit.SECONDS) ) 
)
```

### 3.3.2 失败率方式
例如：`5分钟内若失败了3次则认为该job失败，重试间隔为10s`

* 方法一：配置：flink-conf.yaml
```yaml
restart-strategy: failure-rate
restart-strategy.failure-rate.max-failures-per-interval : 3
restart-strategy.failure-rate.failure-rate-interval : 5 min
restart-strategy.failure-rate.delay : 10 s
```

* 方法二：代码设置
```
env.setRestartStrategy(
    RestartStrategies.failureRateRestart(3, Time.of(5, TimeUnit.MINUTES), Time.of(10, TimeUnit.SECONDS) )
)
```

### 3.3.3 无重启
* 方法一：配置：flink-conf.yaml
```yaml
restart-strategy: none
```

* 方法二：代码设置
```
env.setRestartStrategy(
    RestartStrategies.noRestart()
)
```

# 4 Operator
## 4.1 Connect 和 union
Connect只能连接双流，可以共享状态，输出的流的类型可以不同

## 4.2 rescale、shuffle、rebalance
* 可以解决数据倾斜的问题
* rescale 针对并行度
* Shuffle针对数据，随机
* Rebalance针对数据，round-robin

## 4.3 Broadcast
广播数据到每个partition；
多用于配置流。  

## 4.4 RuntimeContext
* getIndexOfThisSubtask()
* getMetricGroup(可用于自定义metric)
* getState(用于状态获取)


# 5 Sink
* 常用的Sink  
* 自定义Sink，实现 SinkFunction 接口


















