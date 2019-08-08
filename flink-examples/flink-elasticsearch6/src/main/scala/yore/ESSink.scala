package yore

import org.apache.commons.lang3.StringUtils
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.action.bulk.{BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.{DocWriteRequest, DocWriteResponse}
import org.elasticsearch.client.{Requests, RestClient, RestClientBuilder, RestHighLevelClient}
import org.elasticsearch.common.xcontent.XContentType

import scala.collection.mutable.ListBuffer

/**
  *
  * Created by yore on 2019/8/7 19:56
  */
class ESSink(val esIndex: String, val esType: String) extends RichSinkFunction[(String, String)] /*with CheckpointedFunction*/{

  private[this] var esClient: RestHighLevelClient = _
  private[this] var credentialsProvider: CredentialsProvider = _
  private[this] val batchSize: Int = 10
  private[this] var list: ListBuffer[String] = _


  override def open(parameters: Configuration): Unit = {
    list = ListBuffer[String]()
    credentialsProvider = new BasicCredentialsProvider

    credentialsProvider.setCredentials(
      AuthScope.ANY,
      new UsernamePasswordCredentials("elastic", "123456"))

    esClient = new RestHighLevelClient(
      RestClient.builder(
        new HttpHost("cdh2", 9200, "http"),
        new HttpHost("cdh3", 9200, "http")
    ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
        override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
          httpClientBuilder.disableAuthCaching
          httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
        }
      })
    )
  }

  var batchIndex: Int = 0
  override def invoke(value: (String, String), context: SinkFunction.Context[_]): Unit = {
    list += "{\"data\": \"" + value._2 + "\"}"

    if(batchIndex % batchSize == 0){
      bulkDate(value._2, list)
      list.clear()
      batchIndex = 0
    }else{
      batchIndex += 1
    }

  }

  override def close(): Unit = {
    esClient.close()
  }


  def bulkDate(id: String, list: ListBuffer[/*Map[String, Object]*/String]): Unit = {
    try {
      if(null!=list && list.size>0 && StringUtils.isNoneBlank(id)){
        val request: BulkRequest = new BulkRequest()
        for(json <- list){
            request.add(
//              new IndexRequest(indexName, esType, id).source(map, XContentType.JSON)
              Requests.indexRequest()
                .index(esIndex)
                .`type`(esType)
                .id(id)
                .source(json, XContentType.JSON)
            )
        }

        // 推荐使用  bulk(BulkRequest, RequestOptions)
        val bulkResponse: BulkResponse = esClient.bulk(request)
        if(bulkResponse != null){
          val it = bulkResponse.iterator()
          while (it.hasNext){
            val bulkItemResponse = it.next()
            val itemResponse: DocWriteResponse = bulkItemResponse.getResponse()
            if(bulkItemResponse.getOpType() == DocWriteRequest.OpType.INDEX || bulkItemResponse.getOpType() == DocWriteRequest.OpType.CREATE){
              val indexResponse: IndexResponse = itemResponse.asInstanceOf[IndexResponse]
              //TODO 新增成功的处理
              //println("新增成功,{}"+ indexResponse.toString())
            }else if(bulkItemResponse.getOpType() == DocWriteRequest.OpType.UPDATE) {
              val updateResponse: UpdateResponse = itemResponse.asInstanceOf[UpdateResponse]
              //TODO 修改成功的处理
              //println("修改成功,{}" + updateResponse.toString())
            }else if(bulkItemResponse.getOpType() == DocWriteRequest.OpType.DELETE){
              val deleteResponse: DeleteResponse = itemResponse.asInstanceOf[DeleteResponse]
              println("删除成功,{}"+ deleteResponse.toString())
            }
          }
        }
      }

    }catch {
      case e: Exception => e.printStackTrace()
    }
  }


  /**
    * //用于从之前的检查点中恢复function或operator的状态
    *
    * @param context
    */
//  override def initializeState(context: FunctionInitializationContext): Unit = {
//    //TODO
//  }

  /**
    * 获得function或者operator的当前状态（快照）
    *
    * @param context
    */
//  override def snapshotState(context: FunctionSnapshotContext): Unit = {
//    //TODO
//  }

}
