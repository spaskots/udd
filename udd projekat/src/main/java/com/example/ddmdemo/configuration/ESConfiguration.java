package com.example.ddmdemo.configuration;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.ddmdemo.indexrepository")
public class ESConfiguration
    extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username}")
    private String userName;

    @Value("${elasticsearch.password}")
    private String password;

    @NotNull
    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder().connectedTo(host + ":" + port)
            .withBasicAuth(userName, password).build();
    }
}
