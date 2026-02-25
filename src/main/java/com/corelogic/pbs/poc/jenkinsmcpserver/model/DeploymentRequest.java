package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import lombok.Data;

@Data
public class DeploymentRequest {
    private String githubRepoName;
    private String branchName;
    private String artifactVersion;
    private String envsToDeployTo;
}

