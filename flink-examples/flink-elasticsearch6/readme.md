flink elasticsearch6
======
官方测试代码可查看 [flink-elasticsearch6-test](https://github.com/apache/flink/tree/master/flink-end-to-end-tests/flink-elasticsearch6-test)

Flink官方文档 [Elasticsearch Connector](https://ci.apache.org/projects/flink/flink-docs-release-1.8/dev/connectors/elasticsearch.html)

### Elasticsearch Connector
```xml
<dependency>
    <groupId>org.apache.flink</groupId>
    <artifactId>flink-connector-elasticsearch6_2.11</artifactId>
    <version>${flink.version}</version>
</dependency>
```

官方提供的`flink-connector-elasticsearch6_2.11`，如果ES设置的有用户校验，暂时未找到对应配置，
可以自定义一个Sink使用`elasticsearch-rest-high-level-client`中的对应api来实现

### ES [Java REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest)
可以访问 [https://www.elastic.co/guide/en/elasticsearch/client/java-rest/index.html](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/index.html)
查看对应版本的 Java REST Client API。例如 查看6.8版本

* [Java Low Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.8/java-rest-low.html)
* [Java High Level REST Client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.8/java-rest-high.html)






