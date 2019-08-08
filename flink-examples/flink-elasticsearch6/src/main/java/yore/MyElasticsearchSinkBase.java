/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yore;

import org.apache.flink.streaming.connectors.elasticsearch.ActionRequestFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkBase;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.util.NoOpFailureHandler;
import org.apache.flink.streaming.connectors.elasticsearch6.RestClientFactory;
import org.apache.flink.util.Preconditions;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yore on 2019/8/6 17:20
 */
public class MyElasticsearchSinkBase<T> extends ElasticsearchSinkBase<T, RestHighLevelClient> {


    public MyElasticsearchSinkBase(
            Map<String, String> bulkRequestsConfig,
            List<HttpHost> httpHosts,
            ElasticsearchSinkFunction<T> elasticsearchSinkFunction,
            ActionRequestFailureHandler failureHandler,
            RestClientFactory restClientFactory) {
        super(new MyElasticsearch6ApiCallBridge(httpHosts, restClientFactory),  bulkRequestsConfig, elasticsearchSinkFunction, failureHandler);
    }

    public static class Builder<T> {
        final List<HttpHost> httpHosts;
        final ElasticsearchSinkFunction<T> elasticsearchSinkFunction;

        Map<String, String> bulkRequestsConfig = new HashMap<>();
        RestClientFactory restClientFactory = restClientBuilder -> {};
        ActionRequestFailureHandler failureHandler = new NoOpFailureHandler();

//        public Builder() {
//            this.httpHosts = new ArrayList<>();
//            httpHosts.add(new HttpHost("cdh6", 9200, "http"));
//
//            this.bulkRequestsConfig.put("cluster.name", "docker-cluster");
//
//            this.elasticsearchSinkFunction = new ;
//
//        }

        public Builder(ElasticsearchSinkFunction<T> elasticsearchSinkFunction) {
            this.httpHosts = new ArrayList<>();
//            httpHosts.add(new HttpHost("cdh6", 9200, "http"));
            httpHosts.add(new HttpHost("cdh2", 9200, "http"));
            httpHosts.add(new HttpHost("cdh3", 9200, "http"));
            this.bulkRequestsConfig.put("cluster.name", "docker-cluster");
            this.bulkRequestsConfig.put("index.number_of_shards", "1");
            this.bulkRequestsConfig.put("index.number_of_replicas", "1");
            this.elasticsearchSinkFunction = Preconditions.checkNotNull(elasticsearchSinkFunction);
        }


        /**
         * Sets the maximum number of actions to buffer for each bulk request.
         *
         * @param numMaxActions the maxinum number of actions to buffer per bulk request.
         */
        public void setBulkFlushMaxActions(int numMaxActions) {
            Preconditions.checkArgument(
                    numMaxActions > 0,
                    "Max number of buffered actions must be larger than 0.");

            this.bulkRequestsConfig.put(CONFIG_KEY_BULK_FLUSH_MAX_ACTIONS, String.valueOf(numMaxActions));
        }

        /**
         * Sets a REST client factory for custom client configuration.
         *
         * @param restClientFactory the factory that configures the rest client.
         */
        public void setRestClientFactory(RestClientFactory restClientFactory) {
            this.restClientFactory = Preconditions.checkNotNull(restClientFactory);
        }

        /**
         * Sets a failure handler for action requests.
         *
         * @param failureHandler This is used to handle failed {@link ActionRequest}.
         */
        public void setFailureHandler(ActionRequestFailureHandler failureHandler) {
            this.failureHandler = Preconditions.checkNotNull(failureHandler);
        }

        /**
         * Creates the Elasticsearch sink.
         *
         * @return the created Elasticsearch sink.
         */
        public MyElasticsearchSinkBase<T> build() {
            return new MyElasticsearchSinkBase<T>(bulkRequestsConfig, httpHosts, elasticsearchSinkFunction, failureHandler, restClientFactory);
        }
    }
}
