从源码构建Flink
------
Flink Development &nbsp; &nbsp;/ &nbsp; &nbsp; [Building Flink from Source](https://ci.apache.org/projects/flink/flink-docs-release-1.8/flinkDev/building.html)

# 目录
* Build Flink (构建 Flink)
* Dependency Shading (依赖 shading)
* Hadoop Versions (Hadoop 版本)
    - Packaging Hadoop into the Flink distribution (将Hadoop 打包到Flink发行版中)
    - Vendor-specific Versions (供应商特定版本)
* Scala Versions (Scala版本)
* Encrypted File Systems (加密文件系统)

------

# Build Flink (构建 Flink)
为了构建 Flink，您需要源代码。[下载发行版的源代码](http://flink.apache.org/downloads.html) 
或 [克隆git存储库](https://github.com/apache/flink)。

此外，您还需要 Maven 3 和 JDK（Java Development Kit）。 Flink至少需要 Java 8 才能构建。

**注意**：Maven 3.3.x 可以构建 Flink，但不会正确地遮蔽某些依赖项。 Maven 3.2.5正确创建了库。 
要构建单元测试，请使用 Java 8u51 或更高版本来防止使用 PowerMock 运行程序的单元测试失败。

要从git克隆，请输入：
```bash
git clone https://github.com/apache/flink.git
```

构建Flink的最简单方法是运行：
```bash
mvn clean install -DskipTests
```

这指示 Maven（mvn）首先清除所有已构建的（clean），然后创建一个新的 Flink 二进制文件（install）。

要加快构建速度，您可以跳过测试，QA插件和JavaDocs：
```bash
mvn clean install -DskipTests -Dfast
```
默认构建为 Hadoop 2 添加了特定于 Flink的JAR，以允许将 Flink 与 HDFS 和 YARN 一起使用。


# Dependency Shading (依赖 shading)
Flink 隐藏了它使用的一些库，以避免与使用这些库的不同版本的用户程序的版本冲突。 Shaded 库包括Google Guava，Asm，Apache Curator，Apache HTTP Components，Netty等。

最近在 Maven中 更改了依赖关系 shading 机制，并要求用户根据 Maven 版本略微不同地构建Flink：

Maven 3.0.x，3.1.x和3.2.x在Flink代码库的根目录中调用mvn clean install -DskipTests就足够了。

Maven 3.3.x构建必须分两步完成：首先在 base 目录中，然后在分发项目中：
```bash
mvn clean install -DskipTests
cd flink-dist
mvn clean install
```

注意：要检查Maven版本，请运行mvn --version。

# Hadoop Versions (Hadoop 版本)
**信息** 大多数用户不需要手动执行此操作。 [下载页面](http://flink.apache.org/downloads.html)包含常见Hadoop版本的二进制包。

Flink 依赖于 HDFS 和 YARN，它们都是来自 [Apache Hadoop](http://hadoop.apache.org/) 的依赖项。 
存在许多不同版本的 Hadoop（来自上游项目和不同的Hadoop发行版）。 如果使用错误的版本组合，则可能发生异常。
       
Hadoop仅从2.4.0版本开始支持。 您还可以指定要构建的特定Hadoop版本：
```bash
mvn clean install -DskipTests -Dhadoop.version=2.6.1
```

## Packaging Hadoop into the Flink distribution (将Hadoop 打包到Flink发行版中)
如果要构建一个在 lib 文件夹中预先打包 shaded Hadoop 的 Flink 发行版，则可以使用 include-hadoop 配置文件来执行此操作。您将如上所述构建 Flink，但包括配置文件：
```bash
mvn clean install -DskipTests -Pinclude-hadoop
```

## Vendor-specific Versions (供应商特定版本)
要查看支持的供应商版本列表，请查看 [https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-hdfs?repo=cloudera](https://mvnrepository.com/artifact/org.apache.hadoop/hadoop-hdfs?repo=cloudera) 
要针对特定​​于供应商的 Hadoop 版本构建 Flink，请发出以下命令：
```bash
mvn clean install -DskipTests -Pvendor-repos -Dhadoop.version=2.6.0-cdh5.16.1
```

`-Pvendor-repos`激活 Maven 构建[配置文件](http://maven.apache.org/guides/introduction/introduction-to-profiles.html)，
其中包括 Cloudera，Hortonworks 或 MapR 等流行的 Hadoop 供应商的存储库。


# Scala Versions (Scala版本)
**信息** 纯粹使用Java API 和 库的用户可以忽略此部分。

Flink 具有用 [Scala](http://scala-lang.org/) 编写的 API，库和运行时模块。 Scala API 和库的用户可能必须将 Flink 的 Scala 版本与其项目的 Scala 版本匹配（因为Scala不是严格向后兼容的）。
       
从版本 1.7 开始，Flink使用Scala版本2.11和2.12构建。


# Encrypted File Systems (加密文件系统)
如果您的主目录已加密，您可能会遇到`java.io.IOException：File name too long exception`。
某些加密文件系统（如 Ubuntu 使用的 encfs）不允许长文件名，这是导致此错误的原因。

解决方法是添加：
```xml
<args>
    <arg>-Xmax-classfile-name</arg>
    <arg>128</arg>
</args>
```

在导致错误的模块的 pom.xml 文件的编译器配置中。 例如，如果错误出现在 flink-yarn 模块中，则应在 scala-maven-plugin 的 <configuration> 标记下添加上述代码。 
有关更多信息，请参阅[此问题](https://issues.apache.org/jira/browse/FLINK-2003)。


----


从源码编译 - 基于 cdh 6.2.0 的Flink
====

* clone 源码
```bash
git clone https://github.com/apache/flink.git
```

* 查看版本
进入到源码根目录， `cd flink
```bash
git tag
```

* 切换到 flink 1.8 发行版
```bash
git checkout tags/release-1.8.0
```

* 查看当前所处的分支
```bash
git branch
```

* 开始编译
```bash
mvn -T2C clean install -DskipTests -Pvendor-repos -Dhadoop.version=3.0.0-cdh6.2.0

cd flink-dist
mvn clean install

```


# 待解决的问题
在上一步编译时可能会出现如下错误
```
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:17 min (Wall Clock)
[INFO] Finished at: 2019-05-24T15:03:08+08:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal on project flink-shaded-yarn-tests: Could not resolve dependencies for project org.apache.flink:flink-shaded-yarn-tests:jar:1.8.0: The following artifacts could not be resolved: org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.1.0, org.apache.hadoop:hadoop-yarn-server-tests:jar:3.0.0-cdh6.1.0, org.apache.hadoop:hadoop-yarn-server-tests:jar:tests:3.0.0-cdh6.1.0, org.apache.hadoop:hadoop-minicluster:jar:3.0.0-cdh6.1.0, org.apache.hadoop:hadoop-yarn-server-resourcemanager:jar:3.0.0-cdh6.1.0: Could not find artifact org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.1.0 in nexus-aliyun (http://maven.aliyun.com/nexus/content/groups/public) -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/DependencyResolutionException
[ERROR]
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <goals> -rf :flink-shaded-yarn-tests
```

原因是，我们指定的HDFS版本为 `3.0.0-cdh6.1.0`，hadoop-common的`3.0.0-cdh6.1.0`版本，可以使用 `3.0.0-cdh6.1.1`替换

hadoop-common                        3.0.0-cdh6.1.0  => 3.0.0-cdh6.2.0
hadoop-yarn-server-tests             3.0.0-cdh6.1.0  => 3.0.0-cdh6.2.0
hadoop-minicluster                   3.0.0-cdh6.1.0  => 3.0.0-cdh6.2.0
hadoop-yarn-server-resourcemanager   3.0.0-cdh6.1.0  => 3.0.0-cdh6.2.0

我们直接使用`3.0.0-cdh6.2.0`版本编译。


```
[ERROR] Failed to execute goal on project flink-shaded-hadoop2: Could not resolve dependencies for project org.apache.flink:flink-shaded-hadoop2:jar:3.0.0-cdh6.2.0-1.8.0: The following artifacts could not be resolved: org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.2.0, org.apache.hadoop:hadoop-hdfs:jar:3.0.0-cdh6.2.0, org.apache.hadoop:hadoop-mapreduce-client-core:jar:3.0.0-cdh6.2.0, org.apache.hadoop:hadoop-yarn-client:jar:3.0.0-cdh6.2.0, org.apache.hadoop:hadoop-yarn-common:jar:3.0.0-cdh6.2.0: Could not find artifact org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.2.0 in nexus-aliyun (http://maven.aliyun.com/nexus/content/groups/public) -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/DependencyResolutionException
[ERROR]
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <goals> -rf :flink-shaded-hadoop2

```

Could not resolve dependencies for project org.apache.flink:flink-shaded-hadoop2:jar:3.0.0-cdh6.2.0-1.8.0: 
The following artifacts could not be resolved: org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.2.0, 
org.apache.hadoop:hadoop-hdfs:jar:3.0.0-cdh6.2.0, 
org.apache.hadoop:hadoop-mapreduce-client-core:jar:3.0.0-cdh6.2.0, 
org.apache.hadoop:hadoop-yarn-client:jar:3.0.0-cdh6.2.0, 
org.apache.hadoop:hadoop-yarn-common:jar:3.0.0-cdh6.2.0: 
Could not find artifact org.apache.hadoop:hadoop-common:jar:3.0.0-cdh6.2.0 in nexus-aliyun (http://maven.aliyun.com/nexus/content/groups/public) -> [Help 1]


