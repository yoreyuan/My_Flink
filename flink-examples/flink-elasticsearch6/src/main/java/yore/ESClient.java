package yore;

import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Created by yore on 2019/8/5 16:40
 */
public class ESClient {

    private static  RestHighLevelClient esClient = null;
    static {
        //初始化ES操作客户端
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "123456"));  //es账号密码
        esClient =new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("cdh2",9200, "http"),
                        new HttpHost("cdh3",9200, "http")
                ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        httpClientBuilder.disableAuthCaching();
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                })/*.setMaxRetryTimeoutMillis(2000)*/
        );
    }

    public static void main(String[] args) throws Exception{
//        index();

        RestClientFactory restClientFactory = restClientBuilder -> {};
        System.out.println(restClientFactory);
//        restClientFactory
    }

    static void  index() throws Exception{
        GetRequest getRequest = new GetRequest("kibana_sample_data_logs","_doc", "JX7VWGwBRFIj6SU3vkG3");
        GetResponse response = esClient.get(getRequest);
        System.out.println(response.getSource());

    }


}
