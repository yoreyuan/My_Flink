Flink安装
==

# 1. 准备
服务环境中先安装好 `java`和`scala`,
* 比如 java 安装的为 jdk8 （在 Flink 1.8时要求java必须为1.8.x或更高版本），设置JAVA_HOME环境变量。
* Scala安装的为 2.11.8
* SSH配置完成


# 2.下载
例如这里下载最新版本的，Flink 1.8.0
```bash
wget http://archive.apache.org/dist/flink/flink-1.8.0/flink-1.8.0-bin-scala_2.11.tgz
```


# 3. 安装 
主要展示集群方式部署，一般集群中有一个主节点和一个或多个工作节点组成。

## 3.1 Standalone 集群

1. 解压
```bash
tar -xvf flink-1.8.0-bin-scala_2.11.tgz 
cd flink-1.8.0
```

2. 配置
编辑flink配置文件 `conf/flink-conf.yaml`

```bash
vim conf/flink-conf.yaml
```
修改如下配置项，其他默认即可，
更详细的配置参数请访问 [Configuration](https://ci.apache.org/projects/flink/flink-docs-release-1.8/ops/config.html)
```
# 配置主节点名
jobmanager.rpc.address: node1

# 访问 JobManager 的RPC端口
jobmanager.rpc.port: 6123

# JobManager JVM 的堆大小
jobmanager.heap.size: 1024m


# TaskManager JVM 的堆大小
taskmanager.heap.size: 1024m

# 每一个TaskManager可以使用CPU数
taskmanager.numberOfTaskSlots: 1

# job的默认并行度
parallelism.default: 1

# 是否允许在web提交jar，默认为true
web.submit.enable: true

# 是否开启增量检查点，默认为false
state.backend.incremental: true

```

配置工作节点，编辑 `conf/slaves`，
在此文件中添加工作节点，每个工作节点稍后将运行一个TaskManager进程。
```
node1
node2
node3
```

3. 启动 Flink
`bin/start-cluster.sh` 
也可以单独将 JobManager 或者 TaskManager 实例启动添加到集群
````
bin/jobmanager.sh (start | start-foreground | stop | stop-all)
bin/taskmanager.sh (start | start-foreground | stop | stop-all)
````


4. 停止
`bin/stop-cluster.sh`
































