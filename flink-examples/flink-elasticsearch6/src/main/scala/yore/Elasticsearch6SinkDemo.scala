package yore

import org.apache.flink.api.common.functions.{FlatMapFunction, RuntimeContext}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch.util.RetryRejectedExecutionFailureHandler
import org.apache.flink.streaming.connectors.elasticsearch.{ActionRequestFailureHandler, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.{ElasticsearchSink, RestClientFactory}
import org.apache.flink.util.Collector
import org.apache.http.{Header, HttpHost}
import org.elasticsearch.action.ActionRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.{Requests, RestClientBuilder}

/**
  *
  * 删除索引名 curl -XDELETE -u elastic:changeme http://cdh6:9200/索引名
  *
  * Created by yore on 2019/8/3 14:51
  */
object Elasticsearch6SinkDemo {

  /**
    * --numRecords 需要测试的记录数
    * --index es 的 index （类似于database）
    * --type es 的 type （类似于 table）
    *
    * @param args Array[String]
    */
  def main(args: Array[String]): Unit = {
    val parameterTool = ParameterTool.fromArgs(args)
    if (parameterTool.getNumberOfParameters < 3) {
      System.err.println("Missing parameters!\n" + "Usage: --numRecords <numRecords> --index <index> --type <type>")
      return
    }

    val env = StreamExecutionEnvironment.getExecutionEnvironment
//    env.getConfig.disableSysoutLogging()
    env.enableCheckpointing(5000)
    env.setParallelism(1)

    val source: DataStream[(String, String)] = env.generateSequence(0, 1)
      .flatMap(new FlatMapFunction[Long, (String, String)] {
        override def flatMap(value: Long, out: Collector[(String, String)]): Unit = {
          val key = value.toString
          val message = "message #" + value
          val out1 = (key, message + "update #1")
          val out2 = (key, message + "update #2")
          out.collect(out1)
          out.collect(out2)
        }
      })

    source.print()




    val httpHosts = new java.util.ArrayList[HttpHost]
    httpHosts.add(new HttpHost("cdh6", 9200, "http"))
    //      import scala.collection.JavaConversions._
//    val esSinkBuilder = new ElasticsearchSink.Builder[(String, String)](
//      httpHosts,
//      new ElasticsearchSinkFunction[(String, String)] {
//        override def process(element: (String, String), ctx: RuntimeContext, indexer: RequestIndexer): Unit = {
//          indexer.add(createIndexRequest(element._2, parameterTool))
////          indexer.add(createUpdateRequest(element, parameterTool))
//
//
//        }
//      }
//    )

    val esSinkBuilder = new MyElasticsearchSinkBase.Builder[(String, String)](
      new ElasticsearchSinkFunction[(String, String)] {
        override def process(element: (String, String), ctx: RuntimeContext, indexer: RequestIndexer) = {
          println("\t\t" + element)
          Requests.indexRequest()
            .index(parameterTool.getRequired("index"))
            .`type`(parameterTool.getRequired("type"))
            .id(element._1)
            .source(Map("data" -> element._2))

//          indexer.add(createIndexRequest(element._2, parameterTool))
//          indexer.add(createUpdateRequest(element, parameterTool))
        }
      }
    )

    //指示 sink 在每个元素之后发出，否则它们将被缓冲
    esSinkBuilder.setBulkFlushMaxActions(1)

    // provide a RestClientFactory for custom configuration on the internally created REST client
    esSinkBuilder.setRestClientFactory(new RestClientFactoryImpl)
    esSinkBuilder.setFailureHandler(new RetryRejectedExecutionFailureHandler)

    source.addSink(esSinkBuilder.build())

    env.execute("Elasticsearch 6.x end to end sink test example")

  }


  def createIndexRequest(str: String, parameterTool: ParameterTool): IndexRequest = {
    val json = Map("data" -> str)

    var index: String = ""
    var dataType: String = ""

    println("IndexRequest \t" + json)

    if(str.startsWith("message #15")){
      index = ":intentional invalid index:"
      dataType = ":intentional invalid type:"
    }else {
      index = parameterTool.getRequired("index")
      dataType = parameterTool.getRequired("type")
    }

    Requests.indexRequest()
      .index(parameterTool.getRequired("index"))
      .`type`(parameterTool.getRequired("type"))
      .id(str)
      .source(json)

  }



  def createUpdateRequest(element: (String, String), parameterTool: ParameterTool): UpdateRequest = {
    val json = Map("data" -> element._2)
    println("UpdateRequest \t" + json)
    new UpdateRequest(
      parameterTool.getRequired("index"),
      parameterTool.getRequired("type"),
      element._1
    ).doc(json)
      .upsert(json)
  }

  private class CustomFailureHandler(val index: String, val dataType: String) extends ActionRequestFailureHandler{
    override def onFailure(action: ActionRequest, failure: Throwable, restStatusCode: Int, indexer: RequestIndexer): Unit = {
      if(action.isInstanceOf[IndexRequest]){
        val json = Map("data" -> action.asInstanceOf[IndexRequest].source())
        println("##FailureHandler \t" + json)
        indexer.add(
          Requests.indexRequest()
            .index(index)
            .`type`(dataType)
            .id(action.asInstanceOf[IndexRequest].id())
            .source(json)
        )
      }else{
        throw new IllegalStateException("unexpected")
      }
    }
  }

  class RestClientFactoryImpl extends RestClientFactory{
    override def configureRestClientBuilder(restClientBuilder: RestClientBuilder): Unit = {
      import org.apache.http.message.BasicHeader
      val headers: Array[Header] = Array(new BasicHeader("Content-Type", "application/json"))
      restClientBuilder.setDefaultHeaders(headers)
      restClientBuilder.setMaxRetryTimeoutMillis(90000)
    }
  }

}

