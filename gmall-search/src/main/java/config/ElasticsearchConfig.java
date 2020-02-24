package config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public RestHighLevelClient restHighLevelClient(){

        return new RestHighLevelClient(RestClient.builder(HttpHost.create("http://39.105.153.243:9200")));
    }
}
