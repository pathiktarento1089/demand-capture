package com.igot.demand.elasticsearch.esconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EsConnection {

  @Value("${elasticsearch.host}")
  private String elasticsearchHost;

  @Value("${elasticsearch.port}")
  private int elasticsearchPort;

  @Bean
  public RestHighLevelClient elasticsearchClient() {
    return new RestHighLevelClient(
        RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, "http")));
  }
}
