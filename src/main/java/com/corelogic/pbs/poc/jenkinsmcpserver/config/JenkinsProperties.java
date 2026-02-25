package com.corelogic.pbs.poc.jenkinsmcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsProperties {
    private String baseUrl;
    private String username;
    private String password;
    private List<String> jobs;
    private Integration integration;

    @Data
    public static class Integration {
        private Github github;
    }

    @Data
    public static class Github {
        private List<String> repos;
    }
}


