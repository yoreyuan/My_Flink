Flink 部署
---

# Flink 安装

## Flink 配置
* Akka方面配置
* Checkpoint方面配置
* HA配置
* 内存配置
* MetricReporter
* Yarn方面配置（如果是On YARN时）



# Flink on yarn
[YARN Setup](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/deployment/yarn_setup.html)

* ResourceManager
* NodeManager
* AppMaster (Jobmanager运行在其上)
* Container (Taskmanager运行在其上)
* YarnSession

[Background / Internals](https://ci.apache.org/projects/flink/flink-docs-release-1.8/fig/FlinkOnYarn.svg)


## 参数
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
     -s,--slots <arg>                每个 TaskManager 的 slot 数，默认一个slot对应一个vcore
     -sae,--shutdownOnAttachedExit   If the job is submitted in attached mode, perform a best-effort cluster shutdown when the CLI is terminated abruptly, e.g., in response to a user interrupt, such
                                     as typing Ctrl + C.
     -st,--streaming                 Start Flink in streaming mode
     -t,--ship <arg>                 Ship files in the specified directory (t for transfer)
     -tm,--taskManagerMemory <arg>   Memory per TaskManager Container with optional unit (default: MB)
     -yd,--yarndetached              If present, runs the job in detached mode (deprecated; use non-YARN specific option instead)
     -z,--zookeeperNamespace <arg>   Namespace to create the Zookeeper sub-paths for high availability mode
```

例如：
```
yarn-session.sh –n 2 -s 10 –jm 2048 –tm 10240 –qu root.default –nm test -d
flink run –j test.jar –a “test” –p 20 –yid appId –nm flink-test -d
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

# Standalone



# Mesos
# Docker
# Kubernetes
# AWS






