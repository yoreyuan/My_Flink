Flink 部署
---

# 1 Flink 安装

## 1.1 Flink 配置
* Akka方面配置
* Checkpoint方面配置
* HA配置
* 内存配置
* MetricReporter
* Yarn方面配置（如果是On YARN时）



# 2 Flink on yarn
[YARN Setup](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/deployment/yarn_setup.html)

* Quickstart
    - Start a long-running Flink cluster on YARN (在YARN上启动 long-running 的Flink集群)
    - Run a Flink job on YARN (在YARN上运行Flink job)
* Flink YARN Session 
    - Start Flink Session (开始 Flink Session)
    - Submit Job to Flink (向Flink提交Job)
* Run a single Flink job on YARN (在YARN上运行单个Flink Job)
    - User jars & Classpath 
* Recovery behavior of Flink on YARN (Flink在YARN上的恢复行为)
* Debugging a failed YARN session (调试失败的YARN Session)
    - Log Files (日志文件)
    - YARN Client console & Web interfaces (YARN客户端控制台和Web界面)
* Build YARN client for a specific Hadoop version (为特定的Hadoop版本构建YARN客户端)
* Running Flink on YARN behind Firewalls (在防火墙后面的YARN上运行Flink)
* Background / Internals (后台 / 内部组件)

- - - - 
## 2.1 Quickstart
### 2.1.1 Start a long-running Flink cluster on YARN (在YARN上启动 long-running 的Flink集群)
启动 YARN Session，其中 Job Manager 获得 1 GB 的堆空间，Task Manager 分配 4 GB 的堆空间：
```
# get the hadoop2 package from the Flink download page at
# http://flink.apache.org/downloads.html
curl -O <flink_hadoop2_download_url>
tar xvzf flink-1.8.0-bin-hadoop2.tgz
cd flink-1.8.0/
./bin/yarn-session.sh -jm 1024m -tm 4096m
```

`-s`标志为每个 Job Manager 指定处理 slots 数。 我们建议将 slots 设置为每台计算机的处理器数。

session 启动后，您可以使用 `./bin/flink` 工具将作业提交到群集。

### 2.1.2 Run a Flink job on YARN (在YARN上运行Flink job)
```
# get the hadoop2 package from the Flink download page at
# http://flink.apache.org/downloads.html
curl -O <flink_hadoop2_download_url>
tar xvzf flink-1.8.0-bin-hadoop2.tgz
cd flink-1.8.0/
./bin/flink run -m yarn-cluster -p 4 -yjm 1024m -ytm 4096m ./examples/batch/WordCount.jar
```


## 2.2 Flink YARN Session 
Apache [Hadoop YARN](http://hadoop.apache.org/) 是一个集群资源管理框架。它允许在群集之上运行各种分布式应用程序。 
Flink 在 YARN 上紧接着运行其他应用程序上运行。 如果已经有 YARN 设置，用户不必设置或安装任何东西。

**要求**
* 至少 Apache Hadoop 2.2
* HDFS（Hadoop Distributed File System）（或Hadoop支持的其他分布式文件系统）

如果您在使用 Flink YARN 客户端时遇到麻烦，请查看[FAQ部分](http://flink.apache.org/faq.html#yarn-deployment)。

### 2.2.1 Start Flink Session (开始 Flink Session)
请按照以下说明了解如何在 YARN 群集中启动 Flink Session。

Session 将启动所有必需的 Flink服务（JobManager和TaskManagers），以便您可以将程序提交到群集。 请注意，您可以在每个 Session 中运行多个程序。

#### 下载 Flink
从下载页面下载 Hadoop >= 2 的Flink包。 它包含所需要的文件。

使用一下方法解压:
```bash
tar xvzf flink-1.8.0-bin-hadoop2.tgz
cd flink-1.8.0/
```

#### 开始一个 Session
使用一下命令启动 Session
```bash
./bin/yarn-session.sh
```
此命令将显示以下概述（**注**： 在1.8如果显示如下提示，需要加 `--help`）：
```
Usage:
   Required
     -n,--container <arg>   Number of YARN container to allocate (=Number of Task Managers)
   Optional
     -D <property=value>             use value for given property
     -d,--detached                   If present, runs the job in detached mode
     -h,--help                       Help for the Yarn session CLI.
     -id,--applicationId <arg>       Attach to running YARN session
     -j,--jar <arg>                  Path to Flink jar file
     -jm,--jobManagerMemory <arg>    Memory for JobManager Container with optional unit (default: MB)
     -m,--jobmanager <arg>           Address of the JobManager (master) to which to connect. Use this flag to connect to a different JobManager than the one specified in the configuration.
     -n,--container <arg>            Number of YARN container to allocate (=Number of Task Managers)
     -nl,--nodeLabel <arg>           Specify YARN node label for the YARN application
     -nm,--name <arg>                Set a custom name for the application on YARN
     -q,--query                      Display available YARN resources (memory, cores)
     -qu,--queue <arg>               Specify YARN queue.
     -s,--slots <arg>                Number of slots per TaskManager
     -sae,--shutdownOnAttachedExit   If the job is submitted in attached mode, perform a best-effort cluster shutdown when the CLI is terminated abruptly, e.g., in response to a user interrupt, such
                                     as typing Ctrl + C.
     -st,--streaming                 Start Flink in streaming mode
     -t,--ship <arg>                 Ship files in the specified directory (t for transfer)
     -tm,--taskManagerMemory <arg>   Memory per TaskManager Container with optional unit (default: MB)
     -yd,--yarndetached              If present, runs the job in detached mode (deprecated; use non-YARN specific option instead)
     -z,--zookeeperNamespace <arg>   Namespace to create the Zookeeper sub-paths for high availability mode
```

请注意，客户端需要将 `YARN_CONF_DIR` 或 `HADOOP_CONF_DIR` 环境变量设置为读取 YARN 和 HDFS 配置。

**示例**：发出以下命令以启动 Yarn session 集群，其中每个任务管理器以 8 GB内存和32个处理 slots 启动：
```bash
./bin/yarn-session.sh -tm 8192 -s 32
```

系统将使用 `conf/flink-conf.yaml` 中的配置。如果您想更改某些内容，请按照我们的[配置指南](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/config.html)操作。

YARN 上的 Flink 将覆盖以下配置参数 `jobmanager.rpc.address`（因为 JobManager 总是在不同的机器上分配），
`io.tmp.dirs`（我们使用 YARN 给出的 tmp 目录）和 `parallelism.default`如果数量是已指定的槽数。

如果您不想更改配置文件以设置配置参数，则可以选择通过 `-D` 标志传递动态属性。 所以你可以这样传递参数：
`-Dfs.overwrite-files=true -Dtaskmanager.network.memory.min=536346624`。

示例调用为 ApplicationMaster 启动单个 container 来运行 Job Manager。

当 Job 提交到群集时，Session 群集将自动分配运行 Task Manager 的其他 container。

在 YARN 群集中部署 Flink后，它将显示 Job Manager 的连接详细信息。

通过停止unix进程（使用 CTRL + C）或在客户端输入 `stop` 来停止YARN会话。

只有在群集上有足够的资源可用于 ApplicationMaster 时，YARN 上的 Flink 才会启动。 大多数 YARN 调度程序考虑了所请求的 container 内存，
一些帐户也考虑了核数。 默认情况下，**核数等于处理 slots（-s）参数**。 `yarn.containers.vcores` 允许使用自定义值覆盖核数。 
为了使此参数起作用，您应该在群集中启用CPU调度。

#### YARN Session 分离
如果您不希望 Flink YARN 客户端始终保持运行，则还可以启动分离的YARN session。 该参数称为`-d`或`--detached`。

在这种情况下，Flink YARN 客户端将仅向群集提交 Flink，然后自行关闭。 请注意，在这种情况下，无法使用 Flink 停止 YARN session。

使用 YARN 实用工具（`yarn application -kill <appId>`）来停止YARN session。

#### 附属于现有的 Session
使用以下命令启动 session
```bash
./bin/yarn-session.sh
```

此命令将显示以下概述：
```
Usage:
   Required
     -id,--applicationId <yarnAppId> YARN application Id
```

如上所述，必须将 `YARN_CONF_DIR` 或 `HADOOP_CONF_DIR` 环境变量设置为读取 YARN 和 HDFS 配置。

**示例**：发出以下命令以附加到正在运行的 Flink YARN session application_1463870264508_0029：
```bash
./bin/yarn-session.sh -id application_1463870264508_0029
```

附加到正在运行的会话使用 `YARN ResourceManager` 来确定 Job Manager RPC端口。

通过停止unix进程（使用CTRL + C）或在客户端输入“stop”来停止YARN会话。

### 2.2.2 Submit Job to Flink (向Flink提交Job)
使用以下命令将 Flink 程序提交到 YARN 群集：
```bash
./bin/flink
```

请参阅命[command-line client](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/cli.html)的文档。

该命令将显示如下的列表单：
```
[...]
Action "run" compiles and runs a program.

  Syntax: run [OPTIONS] <jar-file> <arguments>
  "run" action arguments:
     -c,--class <classname>           Class with the program entry point ("main"
                                      method or "getPlan()" method. Only needed
                                      if the JAR file does not specify the class
                                      in its manifest.
     -m,--jobmanager <host:port>      Address of the JobManager (master) to
                                      which to connect. Use this flag to connect
                                      to a different JobManager than the one
                                      specified in the configuration.
     -p,--parallelism <parallelism>   The parallelism with which to run the
                                      program. Optional flag to override the
                                      default value specified in the
                                      configuration
```

使用运行操作将 Job 提交给 YARN。 客户端能够确定 JobManager 的地址。 在极少数问题的情况下，您还可以使用`-m`参数传递`JobManager`地址。 
`JobManager`地址在 YARN 控制台中可见。

**例**
```bash
wget -O LICENSE-2.0.txt http://www.apache.org/licenses/LICENSE-2.0.txt
hadoop fs -copyFromLocal LICENSE-2.0.txt hdfs:/// ...
./bin/flink run ./examples/batch/WordCount.jar \
       --input hdfs:///..../LICENSE-2.0.txt --output hdfs:///.../wordcount-result.txt
```

如果出现以下错误，请确保所有 TaskManagers 都已启动：
```
Exception in thread "main" org.apache.flink.compiler.CompilerException:
    Available instances could not be determined from job manager: Connection timed out.
```

您可以在 JobManager Web 界面中检查 TaskManagers 的数量。 该接口的地址打印在 YARN session 控制台中。

如果一分钟后 TaskManagers 没有显示，您应该使用日志文件调查问题。


## 2.3 Run a single Flink job on YARN (在YARN上运行单个Flink Job)
上面的文档描述了如何在 Hadoop YARN 环境中启动 Flink 集群。也可以仅在执行单个作业时在 YARN 中启动 Flink。

**例**
```bash
./bin/flink run -m yarn-cluster ./examples/batch/WordCount.jar
```

YARN session 的命令行选项也可用于 `./bin/flink` 工具。 它们以 `y `或 `yarn` 为前缀（用于 long 参数选项）。

注意：通过设置环境变量 FLINK_CONF_DIR，您可以为每个作业使用不同的配置目录。 要使用此副本，请从 Flink 分发中复制 conf 目录，并根据每个作业修改日志记录设置。

注意：可以将 `-m yarn-cluster` 与分离的 YARN 提交（-yd）组合，以“触发并forget”到 YARN 群集的 Flink作业。 在这种情况下，
您的应用程序将不会从`ExecutionEnvironment.execute()`调用获得任何累加器结果或异常！

### 2.3.1 User jars & Classpath 
默认情况下，Flink 将在运行单个作业时将用户jar包含到系统类路径中。 可以使用 `yarn.per-job-cluster.include-user-jar` 参数控制此行为。

将此设置为 `DISABLED` 时，Flink 将在用户类路径中包含jar。

可以通过将参数设置为以下之一来控制类路径中的user-jar位置：
* ORDER :(默认）根据字典顺序将jar添加到系统类路径。
* FIRST：将jar添加到系统类路径的开头。
* LAST：将jar添加到系统类路径的末尾。


## 2.4 Recovery behavior of Flink on YARN (Flink在YARN上的恢复行为)
Flink 的 YARN 客户端具有以下配置参数来控制 container 故障时的行为方式。 可以使用 `-D` 参数从 `conf/flink-conf.yaml` 或启动 YARN session 时设置这些参数。

`yarn.application-attempts：` ApplicationMaster（+其TaskManager容器）尝试的次数。 如果此值设置为1（默认值），则当 `Application master` 失败时，
整个 YARN session 将失败。 较高的值指定 YARN 重新启动 ApplicationMaster 的次数。

## 2.5 Debugging a failed YARN session (调试失败的YARN Session)
Flink YARN session 部署失败的原因有很多。 配置错误的 Hadoop 设置（HDFS权限，YARN配置），版本不兼容（在 Cloudera Hadoop 上运行 Flink 与 vanilla Hadoop 依赖关系）或其他错误。

### 2.5.1 Log Files (日志文件)
如果 Flink YARN session 在部署期间失败，则用户必须依赖 Hadoop YARN 的日志记录功能。 最有用的功能是 [YARN 日志聚合](http://hortonworks.com/blog/simplifying-user-logs-management-and-access-in-yarn/)。 
要启用它，用户必须在`yarn-site.xml`文件中将`yarn.log-aggregation-enable`属性设置为 true。 启用后，用户可以使用以下命令检索（失败的）YARN session 的所有日志文件。

```
yarn logs -applicationId <application ID>
```

请注意，session 结束后需要几秒钟才会显示日志。

### 2.5.2 YARN Client console & Web interfaces (YARN客户端控制台和Web界面)
如果在运行期间发生错误，Flink YARN 客户端还会在终端中输出错误消息（例如，如果 TaskManager 在一段时间后停止工作）。

除此之外，还有 `YARN Resource Manager Web` 界面（默认情况下在端口8088上）。 Resource Manager Web 界面的端口由`yarn.resourcemanager.webapp.address`配置值确定。

它允许访问日志文件以运行 YARN 应用程序，并显示失败应用程序的诊断。

## 2.6 Build YARN client for a specific Hadoop version (为特定的Hadoop版本构建YARN客户端)
使用来自 Hortonworks，Cloudera 或 MapR 等公司的 Hadoop 发行版的用户可能必须针对其特定版本的 Hadoop（HDFS）和 YARN 构建Flink。 
有关更多详细信息，请阅读[构建说明](https://ci.apache.org/projects/flink/flink-docs-release-1.8/flinkDev/building.html)。

## 2.7 Running Flink on YARN behind Firewalls (在防火墙后面的YARN上运行Flink)
某些 YARN 群集使用防火墙来控制群集与网络其余部分之间的网络流量。在这些设置中，Flink作业只能从群集网络内（防火墙后面）提交到YARN会话。
如果这对于生产使用不可行，则 Flink 允许为其 REST endpoint 配置端口范围，用于客户端群集通信。配置此范围后，用户还可以通过防火墙向 Flink 提交作业。

用于指定 REST endpoint 端口的配置参数如下：
```
rest.bind-port
```

此配置选项接受单个端口（例如：`50010`），范围（`50000-50025`）或两者的组合（`50010,50011,50020-50025,50050-50075`）。

请确保未指定配置选项 `rest.port`，因为它优先于`rest.bind-port` 并且不接受任何范围。

（Hadoop 使用类似的机制，配置参数名为 `yarn.app.mapreduce.am.job.client.port-range`。）


## 2.8 Background / Internals (后台 / 内部组件)
本节简要介绍Flink和YARN如何交互。

![Background / Internals](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/FlinkOnYarn.svg)

YARN 客户端需要访问 Hadoop 配置以连接到 YARN resource manager 和 HDFS。它使用以下策略确定 Hadoop 配置：

* 测试是否设置了 `YARN_CONF_DIR`，`HADOOP_CONF_DIR` 或 `HADOOP_CONF_PATH`（按此顺序）。 如果设置了其中一个变量，则用于读取配置。
* 如果上述策略失败（在正确的 YARN 设置中不应该这样），则客户端正在使用 HADOOP_HOME 环境变量。 如果已设置，
客户端将尝试访问`$HADOOP_HOME/etc/hadoop`（Hadoop 2）和`$HADOOP_HOME/conf`（Hadoop 1）。

启动新的 Flink YARN session 时，客户端首先检查所请求的资源（ApplicationMaster 的内存和核数）是否可用。 之后，它将上传包含 Flink和配置的jar到HDFS（步骤1）。

客户端的下一步是请求（步骤2）YARN container 以启动 ApplicationMaster（步骤3）。 由于客户端将配置和jar文件注册为 container 的资源，
因此在该特定机器上运行的 YARN 的 NodeManager 将负责准备 container（例如，下载文件）。 完成后，将启动 ApplicationMaster（AM）。

JobManager 和 AM 在同一容器中运行。 一旦它们成功启动，AM 就知道 JobManager（它自己的主机）的地址。 它正在为 TaskManagers 生成一个新的
Flink配置文件（以便它们可以连接到 JobManager）。 该文件也上传到 HDFS。 此外，AM 容器还提供 Flink 的 Web 界面。 YARN 代码分配的所有端口都是临时端口。 
这允许用户并行执行多个 Flink YARN session。

之后，AM 开始为 Flink 的 TaskManagers 分配容器，这将从 HDFS 下载 jar 文件和修改后的配置。 完成这些步骤后，即可建立Flink并准备接受作业。



<br/><br/>

----------

<br/><br/>


* ResourceManager
* NodeManager
* AppMaster (Jobmanager运行在其上)
* Container (Taskmanager运行在其上)
* YarnSession

## 参数

例如：
```
# 启动一个Flink ApplicationMaster	
yarn-session.sh -n 4 -jm 1024 -tm 1024
yarn-session.sh -n 3 -jm 1024 -tm 1024  -s 3 -nm FlinkOnYarnSession -d -st
yarn-session.sh –n 2 -s 10 –jm 2048 –tm 10240 –qu root.default –nm test -d

# 提交Flink到YARN上
flink run –j test.jar –a “test” –p 20 –yid appId –nm flink-test -d
flink run -m yarn-cluster -yn 4 -yjm 1024 -ytm 1024 ./examples/batch/WordCount.jar
```

## 注意
**如果Hadoop是CDH安装的**，需要如下配置
* ① 环境中配置 `HADOOP_CONF_DIR` 路径
```bash
vim /etc/profile

# 输入：
export HADOOP_CONF_DIR=/etc/hadoop/conf

# 生效
. /etc/profile
```

* jar包设置
Flink通过YARN时有些类找不到，需要引入缺失的jar，
例如 cdh-5.16环境，将如下jar软连接到 Flink的资源目录下
```bash
ln -s /opt/cm-5.16.1/share/cmf/common_jars/commons-configuration-1.9.jar $FLINK_HOME/lib/commons-configuration-1.9.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/commons-lang-2.6.jar $FLINK_HOME/lib/commons-lang-2.6.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/commons-logging-1.1.3.jar $FLINK_HOME/lib/commons-logging-1.1.3.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/guava-14.0.jar $FLINK_HOME/lib/guava-14.0.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/hadoop-auth-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-auth-2.6.0-cdh5.14.0.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/hadoop-common-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-common-2.6.0-cdh5.14.0.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/hadoop-hdfs-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-hdfs-2.6.0-cdh5.14.0.jar
ln -s /opt/cm-5.16.1/share/cmf/common_jars/hadoop-yarn-api-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-yarn-api-2.6.0-cdh5.14.0.jar
```

* 如果是`CDH 6.2.0`。
有两种方式，①引入cdh5的jar；②引入cdh6的jar

*CDH5方式
```bash
ln -s /opt/cloudera/cm/lib/commons-configuration-1.9.jar $FLINK_HOME/lib/commons-configuration-1.9.jar
ln -s /opt/cloudera/cm/lib/commons-lang-2.6.jar $FLINK_HOME/lib/commons-lang-2.6.jar
ln -s /opt/cloudera/parcels/CDH-6.2.0-1.cdh6.2.0.p0.967373/jars/commons-logging-1.1.3.jar $FLINK_HOME/lib/commons-logging-1.1.3.jar
ln -s /opt/cloudera/cm/lib/guava-14.0.jar $FLINK_HOME/lib/guava-14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-auth-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-auth-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-common-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-common-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-hdfs-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-hdfs-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-yarn-api-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-yarn-api-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-yarn-client-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-yarn-client-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/hadoop-yarn-common-2.6.0-cdh5.14.0.jar $FLINK_HOME/lib/hadoop-yarn-common-2.6.0-cdh5.14.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/protobuf-java-2.5.0.jar $FLINK_HOME/lib/protobuf-java-2.5.0.jar
ln -s /opt/cloudera/cm/lib/cdh5/htrace-core4-4.0.1-incubating.jar $FLINK_HOME/lib/htrace-core4-4.0.1-incubating.jar

```

CDH6方式
```bash
ln -s /opt/cloudera/cm/lib/commons-configuration-1.9.jar $FLINK_HOME/lib/commons-configuration-1.9.jar
ln -s /opt/cloudera/cm/lib/cdh6/commons-configuration2-2.1.1.jar $FLINK_HOME/lib/commons-configuration2-2.1.1.jar
ln -s /opt/cloudera/cm/lib/commons-lang-2.6.jar $FLINK_HOME/lib/commons-lang-2.6.jar
ln -s /opt/cloudera/parcels/CDH-6.2.0-1.cdh6.2.0.p0.967373/jars/commons-logging-1.1.3.jar $FLINK_HOME/lib/commons-logging-1.1.3.jar
ln -s /opt/cloudera/cm/lib/guava-14.0.jar $FLINK_HOME/lib/guava-14.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-auth-3.0.0-cdh6.2.0.jar  $FLINK_HOME/lib/hadoop-auth-3.0.0-cdh6.2.0.jar 
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-common-3.0.0-cdh6.2.0.jar  $FLINK_HOME/lib/hadoop-common-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-hdfs-3.0.0-cdh6.2.0.jar  $FLINK_HOME/lib/hadoop-hdfs-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-hdfs-client-3.0.0-cdh6.2.0.jar $FLINK_HOME/lib/hadoop-hdfs-client-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-yarn-api-3.0.0-cdh6.2.0.jar $FLINK_HOME/lib/hadoop-yarn-api-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-yarn-client-3.0.0-cdh6.2.0.jar $FLINK_HOME/lib/hadoop-yarn-client-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/hadoop-yarn-common-3.0.0-cdh6.2.0.jar $FLINK_HOME/lib/hadoop-yarn-common-3.0.0-cdh6.2.0.jar
ln -s /opt/cloudera/cm/lib/cdh6/protobuf-java-2.5.0.jar $FLINK_HOME/lib/protobuf-java-2.5.0.jar
ln -s /opt/cloudera/cm/lib/woodstox-core-asl-4.4.1.jar $FLINK_HOME/lib/woodstox-core-asl-4.4.1.jar
ln -s /opt/cloudera/cm/lib/stax2-api-3.1.4.jar $FLINK_HOME/lib/stax2-api-3.1.4.jar
ln -s /opt/cloudera/cm/lib/cdh5/htrace-core4-4.0.1-incubating.jar $FLINK_HOME/lib/htrace-core4-4.0.1-incubating.jar
```

# Standalone



# Mesos
# Docker
# Kubernetes
# AWS







