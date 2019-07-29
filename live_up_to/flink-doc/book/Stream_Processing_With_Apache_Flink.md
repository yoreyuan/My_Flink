Stream Processing with Apache Flink - Fundamentals,Implementation,and Operation of Streaming Applications &nbsp; 应用Apache Flink进行流式处理 - 流式应用的架构、实现和操作
======
![lrg.jpg](https://covers.oreillystatic.com/images/0636920057321/lrg.jpg)

作者： **Fabian Hueske** (费比安 韦斯克)， **Vasiliki Kalavri** (瓦西利基 卡拉夫里)


如果是相对Flink有跟深入的了解，除了查看官方文档和源码外，这里推荐可以看下这本书，如果支持原版的可以网上查询购买，书籍详细信息可访问 OREILLY 官网 [Books & Videos](http://shop.oreilly.com/product/0636920057321.do)。
后续有时间我可能会更新书中比较重要和精彩的部分的翻译吧。先释放出这本书的目录，大家可以参考这本书的目录对相应的知识点进行了解和学习。

书中**源码**可以访问 GitHub [streaming-with-flink](https://github.com/streaming-with-flink)


catalogue  | 目录
:---- | ----
1. Preface                                        | 前言 
&nbsp;&nbsp; a. What You Will Learn in This Book  | 你将在本书中学到什么
&nbsp;&nbsp; b. Conventions Used in This Book     | 本书中使用的约定
&nbsp;&nbsp; c. Using Code Examples               | 使用代码示例
&nbsp;&nbsp; d. O’Reilly Online Learning          | O'Reilly在线学习
&nbsp;&nbsp; e. How to Contact Us                 | 如何联系我们
&nbsp;&nbsp; f. Acknowledgments                   | 致谢
 &nbsp; | 
2. 1. Introduction to Stateful Stream Processing                   | 有状态流处理简介
&nbsp;&nbsp; a. Traditional Data Infrastructures                   | 传统数据基础架构
&nbsp;&nbsp; &nbsp;&nbsp; i. Transactional Processing              | 事务性处理
&nbsp;&nbsp; &nbsp;&nbsp; ii. Analytical Processing                | 分析处理
&nbsp;&nbsp; b. Stateful Stream Processing                         | 有状态的流处理
&nbsp;&nbsp; &nbsp;&nbsp; i. Event-Driven Applications             | 事件驱动的应用程序
&nbsp;&nbsp; &nbsp;&nbsp; ii. Data Pipelines                       | 数据管道
&nbsp;&nbsp; &nbsp;&nbsp; iii. Streaming Analytics                 | 流式分析
&nbsp;&nbsp; c. The Evolution of Open Source Stream Processing     | 开源流处理的演进
&nbsp;&nbsp; &nbsp;&nbsp; i. A Bit of History                      | 一点历史
&nbsp;&nbsp; d. A Quick Look at Flink                              | 快速浏览Flink
&nbsp;&nbsp; &nbsp;&nbsp; i. Running Your First Flink Application  | 运行您的第一个Flink应用程序
&nbsp;&nbsp; e. Summary                                            | 小结
 &nbsp; | 
3. 2. Stream Processing Fundamentals                                         | 流处理基础
&nbsp;&nbsp; a. Introduction to Dataflow Programming                         | Dataflow编程简介
&nbsp;&nbsp; &nbsp;&nbsp; i. Dataflow Graphs                                 | 数据流图
&nbsp;&nbsp; &nbsp;&nbsp; ii. Data Parallelism and Task Parallelism          | 数据并行与Task并行
&nbsp;&nbsp; &nbsp;&nbsp; iii. Data Exchange Strategies                      | 数据交换策略
&nbsp;&nbsp; b. Processing Streams in Parallel                               | 并行处理流
&nbsp;&nbsp; &nbsp;&nbsp; i. Latency and Throughput                          | 延迟和吞吐量
&nbsp;&nbsp; &nbsp;&nbsp; ii. Operations on Data Streams                     | 数据流上的Operation
&nbsp;&nbsp; c. Time Semantics                                               | 时间语义
&nbsp;&nbsp; &nbsp;&nbsp; i. What Does One Minute Mean in Stream Processing? | 流处理中的一分钟意味着什么？
&nbsp;&nbsp; &nbsp;&nbsp; ii. Processing Time                                | 处理时间
&nbsp;&nbsp; &nbsp;&nbsp; iii. Event Time                                    | 事件时间
&nbsp;&nbsp; &nbsp;&nbsp; iv. Watermarks                                     | Watermark
&nbsp;&nbsp; &nbsp;&nbsp; v. Processing Time Versus Event Time               | 处理时间与事件时间
&nbsp;&nbsp; d. State and Consistency Models                                 | 状态和一致性模型
&nbsp;&nbsp; &nbsp;&nbsp; i. Task Failures                                   | 任务失败
&nbsp;&nbsp; &nbsp;&nbsp; ii. Result Guarantees                              | 结果保证
&nbsp;&nbsp; e. Summary                                                      | 小结
 &nbsp; | 
4. 3. The Architecture of Apache Flink                                      | Apache Flink的体系结构
&nbsp;&nbsp; a. System Architecture                                         | 系统架构
&nbsp;&nbsp; &nbsp;&nbsp; i. Components of a Flink Setup                    | Flink设置的组件
&nbsp;&nbsp; &nbsp;&nbsp; ii. Application Deployment                        | 应用程序部署
&nbsp;&nbsp; &nbsp;&nbsp; iii. Task Execution                               | Task执行
&nbsp;&nbsp; &nbsp;&nbsp; iv. Highly Available Setup                        | 高度可用的设置
&nbsp;&nbsp; b. Data Transfer in Flink                                      | Flink中的数据传输
&nbsp;&nbsp; &nbsp;&nbsp; i. Credit-Based Flow Control                      | 基于信用的流量控制
&nbsp;&nbsp; &nbsp;&nbsp; ii. Task Chaining                                 | Task链
&nbsp;&nbsp; c. Event-Time Processing                                       | 事件时间处理
&nbsp;&nbsp; &nbsp;&nbsp; i. Timestamps                                     | 时间戳
&nbsp;&nbsp; &nbsp;&nbsp; ii. Watermarks                                    | 水位线
&nbsp;&nbsp; &nbsp;&nbsp; iii. Watermark Propagation and Event Time         | 水位线传播和事件时间
&nbsp;&nbsp; &nbsp;&nbsp; iv. Timestamp Assignment and Watermark Generation | 时间戳分配和水位线生成
&nbsp;&nbsp; d. State Management                                            | 状态管理
&nbsp;&nbsp; &nbsp;&nbsp; i. Operator State                                 | Operator状态
&nbsp;&nbsp; &nbsp;&nbsp; ii. Keyed State                                   | Keyed状态
&nbsp;&nbsp; &nbsp;&nbsp; iii. State Backends                               | 状态后端
&nbsp;&nbsp; &nbsp;&nbsp; iv. Scaling Stateful Operators                    | 扩展有状态的Operator
&nbsp;&nbsp; e. Checkpoints, Savepoints, and State Recovery                 | Checkpoints、Savepoints和状态恢复 
&nbsp;&nbsp; &nbsp;&nbsp; i. Consistent Checkpoints                         | 一致的Checkpoint
&nbsp;&nbsp; &nbsp;&nbsp; ii. Recovery from a Consistent Checkpoint         | 从一致的Checkpoint恢复
&nbsp;&nbsp; &nbsp;&nbsp; iii. Flink’s Checkpointing Algorithm              | Flink的Checkpoint算法
&nbsp;&nbsp; &nbsp;&nbsp; iv. Performace Implications of Checkpointing      | Checkpoint的性能影响
&nbsp;&nbsp; &nbsp;&nbsp; v. Savepoints                                     | Savepoint
&nbsp;&nbsp; f. Summary                                                     | 小节
 &nbsp; | 
5. 4. Setting Up a Development Environment for Apache Flink        | 为Apache Flink设置开发环境
&nbsp;&nbsp; a. Required Software                                  | 软件要求
&nbsp;&nbsp; b. Run and Debug Flink Applications in an IDE         | 在IDE中运行和调试Flink应用程序
&nbsp;&nbsp; &nbsp;&nbsp; i. Import the Book’s Examples in an IDE  | 在IDE中导入Book的示例
&nbsp;&nbsp; &nbsp;&nbsp; ii. Run Flink Applications in an IDE     | 在IDE中运行Flink应用程序
&nbsp;&nbsp; &nbsp;&nbsp; iii. Debug Flink Applications in an IDE  | 在IDE中调试Flink应用程序
&nbsp;&nbsp; c. Bootstrap a Flink Maven Project                    | 引导Flink Maven项目
&nbsp;&nbsp; d. Summary                                            | 小节
 &nbsp; | 
6. 5. The DataStream API (v1.7)                                          | DataStream API（v1.7）
&nbsp;&nbsp; a. Hello, Flink!                                            | Hello, Flink! 
&nbsp;&nbsp; &nbsp;&nbsp; i. Set Up the Execution Environment            | 设置执行环境
&nbsp;&nbsp; &nbsp;&nbsp; ii. Read an Input Stream                       | 读取输入流
&nbsp;&nbsp; &nbsp;&nbsp; iii. Apply Transformations                     | 应用转换
&nbsp;&nbsp; &nbsp;&nbsp; iv. Output the Result                          | 输出结果
&nbsp;&nbsp; &nbsp;&nbsp; v. Execute                                     | 执行
&nbsp;&nbsp; b. Transformations                                          | 转换
&nbsp;&nbsp; &nbsp;&nbsp; i. Basic Transformations                       | 基本转换
&nbsp;&nbsp; &nbsp;&nbsp; ii. KeyedStream Transformations                | KeyedStream转换
&nbsp;&nbsp; &nbsp;&nbsp; iii. Multistream Transformations               | 多流转换
&nbsp;&nbsp; &nbsp;&nbsp; iv. Distribution Transformations               | 分布变换
&nbsp;&nbsp; c. Setting the Parallelism                                  | 设置并行度
&nbsp;&nbsp; d. Types                                                    | 类型
&nbsp;&nbsp; &nbsp;&nbsp; i. Supported Data Types                        | 支持的数据类型
&nbsp;&nbsp; &nbsp;&nbsp; ii. Creating Type Information for Data Types   | 为数据类型创建类型信息
&nbsp;&nbsp; &nbsp;&nbsp; iii. Explicitly Providing Type Information     | 明确提供类型信息
&nbsp;&nbsp; e. Defining Keys and Referencing Fields                     | 定义key和引用字段
&nbsp;&nbsp; &nbsp;&nbsp; i. Field Positions                             | 字段位置
&nbsp;&nbsp; &nbsp;&nbsp; ii. Field Expressions                          | 字段表达
&nbsp;&nbsp; &nbsp;&nbsp; iii. Key Selectors                             | key选择器
&nbsp;&nbsp; f. Implementing Functions                                   | 实现Functions
&nbsp;&nbsp; &nbsp;&nbsp; i. Function Classes                            | Functions类
&nbsp;&nbsp; &nbsp;&nbsp; ii. Lambda Functions                           | Lambda函数
&nbsp;&nbsp; &nbsp;&nbsp; iii. Rich Functions                            | 符函数
&nbsp;&nbsp; g. Including External and Flink Dependencies                | 包括外部和Flink依赖项
&nbsp;&nbsp; h. Summary                                                  | 小节
 &nbsp; | 
7. 6. Time-Based and Window Operators                                       | 基于时间和窗口的Operator
&nbsp;&nbsp; a. Configuring Time Characteristics                            | 配置时间特征
&nbsp;&nbsp; &nbsp;&nbsp; i. Assigning Timestamps and Generating Watermarks | 分配时间戳和生成Watermark
&nbsp;&nbsp; &nbsp;&nbsp; ii. Watermarks, Latency, and Completeness         | 水位线、延迟和完整性
&nbsp;&nbsp; b. Process Functions                                           | 处理Functions
&nbsp;&nbsp; &nbsp;&nbsp; i. TimerService and Timers                        | TimerService和定时器
&nbsp;&nbsp; &nbsp;&nbsp; ii. Emitting to Side Outputs                      | 发射到侧面输出
&nbsp;&nbsp; &nbsp;&nbsp; iii. CoProcessFunction                            | CoProcessFunction
&nbsp;&nbsp; c. Window Operators                                            | 窗口Operator
&nbsp;&nbsp; &nbsp;&nbsp; i. Defining Window Operators                      | 定义窗口Operator
&nbsp;&nbsp; &nbsp;&nbsp; ii. Built-in Window Assigners                     | 内置窗口分配器
&nbsp;&nbsp; &nbsp;&nbsp; iii. Applying Functions on Windows                | 在Windows上应用功能
&nbsp;&nbsp; &nbsp;&nbsp; iv. Customizing Window Operators                  | 自定义窗口运算符
&nbsp;&nbsp; d. Joining Streams on Time                                     | 按时加入Streams
&nbsp;&nbsp; &nbsp;&nbsp; i. Interval Join                                  | 间隔加入
&nbsp;&nbsp; &nbsp;&nbsp; ii. Window Join                                   | 窗口加入
&nbsp;&nbsp; e. Handling Late Data                                          | 处理迟到的数据
&nbsp;&nbsp; &nbsp;&nbsp; i. Dropping Late Events                           | 放弃迟到的事件
&nbsp;&nbsp; &nbsp;&nbsp; ii. Redirecting Late Events                       | 重定向延迟事件
&nbsp;&nbsp; &nbsp;&nbsp; iii. Updating Results by Including Late Events    | 通过包含延迟事件来更新结果
&nbsp;&nbsp; f. Summary                                                     | 小节
 &nbsp; | 
8. 7. Stateful Operators and Applications                                                          | 有状态的Operator和应用
&nbsp;&nbsp; a. Implementing Stateful Functions                                                    | 实现有状态的函数
&nbsp;&nbsp; &nbsp;&nbsp; i. Declaring Keyed State at RuntimeContext                               | 在RuntimeContext中声明keyed状态
&nbsp;&nbsp; &nbsp;&nbsp; ii. Implementing Operator List State with the ListCheckpointed Interface | 使用ListCheckpointed接口实现Operator列表状态
&nbsp;&nbsp; &nbsp;&nbsp; iii. Using Connected Broadcast State                                     | 使用连接广播状态
&nbsp;&nbsp; &nbsp;&nbsp; iv. Using the CheckpointedFunction Interface                             | 使用CheckpointedFunction接口
&nbsp;&nbsp; &nbsp;&nbsp; v. Receiving Notifications About Completed Checkpoints                   | 接收有关已完成Checkpoint的通知
&nbsp;&nbsp; b. Enabling Failure Recovery for Stateful Applications                                | 为有状态应用程序启用故障恢复
&nbsp;&nbsp; c. Ensuring the Maintainability of Stateful Applications                              | 确保有状态应用的可维护性
&nbsp;&nbsp; &nbsp;&nbsp; i. Specifying Unique Operator Identifiers                                | 指定唯一的Operator标识符
&nbsp;&nbsp; &nbsp;&nbsp; ii. Defining the Maximum Parallelism of Keyed State Operators            | 定义keyed状态算子的最大并行性
&nbsp;&nbsp; d. Performance and Robustness of Stateful Applications                                | 状态应用的性能和稳健性
&nbsp;&nbsp; &nbsp;&nbsp; i. Choosing a State Backend                                              | 选择状态后端
&nbsp;&nbsp; &nbsp;&nbsp; ii. Choosing a State Primitive                                           | 选择状态原始
&nbsp;&nbsp; &nbsp;&nbsp; iii. Preventing Leaking State                                            | 防止泄漏状态
&nbsp;&nbsp; e. Evolving Stateful Applications                                                     | 不断发展的有状态应用
&nbsp;&nbsp; &nbsp;&nbsp; i. Updating an Application without Modifying Existing State              | 更新应用程序而不修改现有状态
&nbsp;&nbsp; &nbsp;&nbsp; ii. Removing State from an Application                                   | 从应用程序中删除状态
&nbsp;&nbsp; &nbsp;&nbsp; iii. Modifying the State of an Operator                                  | 修改Operator的状态
&nbsp;&nbsp; f. Queryable State                                                                    | 可查询状态
&nbsp;&nbsp; &nbsp;&nbsp; i. Architecture and Enabling Queryable State                             | 架构和启用可查询状态
&nbsp;&nbsp; &nbsp;&nbsp; ii. Exposing Queryable State                                             | 公开可查询状态
&nbsp;&nbsp; &nbsp;&nbsp; iii. Querying State from External Applications                           | 从外部应用程序查询状态
&nbsp;&nbsp; g. Summary                                                                            | 小节
 &nbsp; | 
9. 8. Reading from and Writing to External Systems                          | 读写外部系统
&nbsp;&nbsp; a. Application Consistency Guarantees                          | 应用程序一致性保证
&nbsp;&nbsp; &nbsp;&nbsp; i. Idempotent Writes                              | 幂等性写
&nbsp;&nbsp; &nbsp;&nbsp; ii. Transactional Writes                          | 事务性写
&nbsp;&nbsp; b. Provided Connectors                                         | 提供连接器
&nbsp;&nbsp; &nbsp;&nbsp; i. Apache Kafka Source Connector                  | Apache Kafka源连接器
&nbsp;&nbsp; &nbsp;&nbsp; ii. Apache Kafka Sink Connector                   | Apache Kafka Sink连接器
&nbsp;&nbsp; &nbsp;&nbsp; iii. Filesystem Source Connector                  | 文件系统源连接器
&nbsp;&nbsp; &nbsp;&nbsp; iv. Filesystem Sink Connector                     | 文件系统Sink连接器
&nbsp;&nbsp; &nbsp;&nbsp; v. Apache Cassandra Sink Connector                | Apache Cassandra Sink连接器
&nbsp;&nbsp; c. Implementing a Custom Source Function                       | 实现自定义源方法
&nbsp;&nbsp; &nbsp;&nbsp; i. Resettable Source Functions                    | 可复位的源功能
&nbsp;&nbsp; &nbsp;&nbsp; ii. Source Functions, Timestamps, and Watermarks  | 源函数，时间戳和水位线
&nbsp;&nbsp; d. Implementing a Custom Sink Function                         | 实现自定义Sink功能
&nbsp;&nbsp; &nbsp;&nbsp; i. Idempotent Sink Connectors                     | 幂等性Sink连接器
&nbsp;&nbsp; &nbsp;&nbsp; ii. Transactional Sink Connectors                 | 事务性Sink连接器
&nbsp;&nbsp; e. Asynchronously Accessing External Systems                   | 异步访问外部系统
&nbsp;&nbsp; f. Summary                                                     | 小节
 &nbsp; | 
10. 9. Setting Up Flink for Streaming Applications              | 为流式应用程序设置Flink
&nbsp;&nbsp; a. Deployment Modes                                | 部署模式
&nbsp;&nbsp; &nbsp;&nbsp; i. Standalone Cluster                 | 独立群集
&nbsp;&nbsp; &nbsp;&nbsp; ii. Docker                            | Docker
&nbsp;&nbsp; &nbsp;&nbsp; iii. Apache Hadoop YARN               | Apache Hadoop YARN
&nbsp;&nbsp; &nbsp;&nbsp; iv. Kubernetes                        | Kubernetes
&nbsp;&nbsp; b. Highly Available Setups                         | 高可用的设置
&nbsp;&nbsp; &nbsp;&nbsp; i. HA Standalone Setup                | HA独立安装程序
&nbsp;&nbsp; &nbsp;&nbsp; ii. HA YARN Setup                     | HA YARN设置
&nbsp;&nbsp; &nbsp;&nbsp; iii. HA Kubernetes Setup              | HA Kubernetes设置
&nbsp;&nbsp; c. Integration with Hadoop Components              | 与Hadoop组件集成
&nbsp;&nbsp; d. Filesystem Configuration                        | 文件系统配置
&nbsp;&nbsp; e. System Configuration                            | 系统配置
&nbsp;&nbsp; &nbsp;&nbsp; i. Java and Classloading              | Java和类加载
&nbsp;&nbsp; &nbsp;&nbsp; ii. CPU                               | CPU
&nbsp;&nbsp; &nbsp;&nbsp; iii. Main Memory and Network Buffers  | 主存储器和网络缓冲器
&nbsp;&nbsp; &nbsp;&nbsp; iv. Disk Storage                      | 磁盘存储
&nbsp;&nbsp; &nbsp;&nbsp; v. Checkpointing and State Backends   | Checkpoint和状态后端
&nbsp;&nbsp; &nbsp;&nbsp; vi. Security                          | 安全
&nbsp;&nbsp; f. Summary                                         | 小节
 &nbsp; | 
11. 10. Operating Flink and Streaming Applications                                | 运行Flink和流式应用程序
&nbsp;&nbsp; a. Running and Managing Streaming Applications                       | 运行和管理流应用程序
&nbsp;&nbsp; &nbsp;&nbsp; i. Savepoints                                           | Savepoint
&nbsp;&nbsp; &nbsp;&nbsp; ii. Managing Applications with the Command-Line Client  | 使用命令行客户端管理应用程序
&nbsp;&nbsp; &nbsp;&nbsp; iii. Managing Applications with the REST API            | 使用REST API管理应用程序
&nbsp;&nbsp; &nbsp;&nbsp; iv. Bundling and Deploying Applications in Containers   | 在容器中捆绑和部署应用程序
&nbsp;&nbsp; b. Controlling Task Scheduling                                       | 控制Task调度
&nbsp;&nbsp; &nbsp;&nbsp; i. Controlling Task Chaining                            | 控制Task链
&nbsp;&nbsp; &nbsp;&nbsp; ii. Defining Slot-Sharing Groups                        | 定义Slot共享组
&nbsp;&nbsp; c. Tuning Checkpointing and Recovery                                 | 调整Checkpoint和恢复
&nbsp;&nbsp; &nbsp;&nbsp; i. Configuring Checkpointing                            | 配置Checkpoint
&nbsp;&nbsp; &nbsp;&nbsp; ii. Configuring State Backends                          | 配置状态后端
&nbsp;&nbsp; &nbsp;&nbsp; iii. Configuring Recovery                               | 配置恢复
&nbsp;&nbsp; d. Monitoring Flink Clusters and Applications                        | 监控Flink群集和应用程序
&nbsp;&nbsp; &nbsp;&nbsp; i. Flink Web UI                                         | Flink Web UI
&nbsp;&nbsp; &nbsp;&nbsp; ii. Metric System                                       | Metric系统
&nbsp;&nbsp; &nbsp;&nbsp; iii. Monitoring Latency                                 | 监控延迟
&nbsp;&nbsp; e. Configuring the Logging Behavior                                  | 配置日志记录行为
&nbsp;&nbsp; f. Summary                                                           | 小节
 &nbsp; | 
12. 11. Where to Go from Here?                                                             | 下一步？
&nbsp;&nbsp; a. The Rest of the Flink Ecosystem                                            | Flink生态系统的其余部分
&nbsp;&nbsp; &nbsp;&nbsp; i. The DataSet API for Batch Processing                          | 用于批处理的DataSet API
&nbsp;&nbsp; &nbsp;&nbsp; ii. Table API and SQL for Relational Analysis                    | 用于关系分析的Table API和SQL
&nbsp;&nbsp; &nbsp;&nbsp; iii. FlinkCEP for Complex Event Processing and Pattern Matching  | FlinkCEP用于复杂事件处理和模式匹配
&nbsp;&nbsp; &nbsp;&nbsp; iv. Gelly for Graph Processing                                   | 用于图处理的Gelly
&nbsp;&nbsp; b. A Welcoming Community                                                      | 欢迎的社区
 &nbsp; | 
13. Index   | 索引

----------

# 1. Preface （前言 ）
## a. What You Will Learn in This Book（你将在本书中学到什么）
本书将向您介绍使用Apache Flink进行流式处理时需要了解的所有内容。 它由11章组成，希望能够讲述一个连贯的故事。 虽然有些章节是描述性的，旨在介绍高级设计概念，但其他章节则更具实际操作性并包含许多代码示例。

虽然我们打算在编写本书时以章节顺序阅读本书，但熟悉章节内容的读者可能希望跳过它。 其他人对立即编写Flink代码感兴趣可能需要先阅读实用章节。 在下文中，我们将简要介绍每章的内容，以便您可以直接跳转到您最感兴趣的章节。

* 第1章概述了有状态流处理，数据处理应用程序体系结构，应用程序设计以及流处理相对于传统方法的好处。 它还简要介绍了在本地Flink实例上运行第一个流应用程序的情况。

* 第2章讨论了流处理的基本概念和挑战，独立于Flink。

* 第3章介绍了Flink的系统架构和内部构件。 它讨论了流式应用程序中的分布式体系结构，时间和状态处理，以及Flink的容错机制。

* 第4章介绍如何设置开发和调试Flink应用程序的环境。

* 第5章介绍了Flink的DataStream API的基础知识。您将学习如何实现DataStream应用程序以及支持哪些流转换，函数和数据类型。

* 第6章讨论DataStream API的基于时间的operator。这包括窗口operator和基于时间的连接以及在流应用程序中处理时间时提供最大灵活性的过程函数。

* 第7章解释了如何实现有状态函数并讨论了有关该主题的所有内容，例如状态函数的性能，健壮性和演变。它还显示了如何使用Flink的可查询状态。

* 第8章介绍了Flink最常用的源和Sink连接器。它讨论了Flink的端到端应用程序一致性方法，以及如何实现自定义连接器从外部系统中提取数据和向外部系统发送数据。

* 第9章讨论如何在各种环境中设置和配置Flink集群。

* 第10章介绍了全天候运行的流应用程序的操作，监视和维护。

* 最后第11章包含可用于提问，参加Flink相关事件以及了解Flink当前如何使用的资源。

## b. Conventions Used in This Book （本书中使用的约定）

## c. Using Code Examples （使用代码示例）

## d. O’Reilly Online Learning（O'Reilly在线学习）

## e. How to Contact Us（如何联系我们）

## f. Acknowledgments （致谢）




























