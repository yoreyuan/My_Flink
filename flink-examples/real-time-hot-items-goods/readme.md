实时热门商品
---

## 创建Maven项目
```
mvn archetype:generate \
-DarchetypeGroupId=org.apache.flink \
-DarchetypeArtifactId=flink-quickstart-java \
-DarchetypeVersion=1.6.1 \
-DgroupId=my-flink-project \
-Dversion=0.1 \
-Dpackage=myflink \
-DinteractiveMode=false

```


mvn install:install-file -DgroupId=org.apache.flink -DartifactId=flink-table_2.11 -Dversion=1.7.0 -Dpackaging=jar -Dfile=flink-table_2.11-1.7.0.jar

## 业务分析
* 抽取出业务时间戳，告诉Flink框架基于业务时间做窗口
* 过滤出点击行为数据
* 按一小时的窗口大小，每5分钟统计一次，做滑动窗口聚合（Sliding Window）
* 按每个窗口聚合，输出每个窗口中点击量前N名的商品

## 数据
[User Behavior Data from Taobao for Recommendation 阿里云-天池](
https://tianchi.aliyun.com/datalab/dataSet.html?spm=5176.100073.0.0.6b2d6fc1o5ohbW&dataId=649
)
`curl https://raw.githubusercontent.com/wuchong/my-flink-project/master/src/main/resources/UserBehavior.csv > UserBehavior.csv`


## 项目源码
[wuchong/my-flink-project](https://github.com/wuchong/my-flink-project)


