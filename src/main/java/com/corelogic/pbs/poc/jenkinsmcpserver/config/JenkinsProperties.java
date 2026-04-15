package com.corelogic.pbs.poc.jenkinsmcpserver.config;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.CommonJobInfo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsProperties {
    private String baseUrl;
    private String username;
    private String password;
    private ApiPaths apiPaths;
    private List<String> branchSpecificJobs;
    private Map<String, List<CommonJobInfo>> commonJobs;
    private Integration integration;

    @Data
    public static class ApiPaths {
        private String businessUnitJob;
        private String projectSpaceJob;
    }

    @Data
    public static class Integration {
        private Github github;
    }

    @Data
    public static class Github {
        private List<String> repos;
    }
}


