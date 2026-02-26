package com.corelogic.pbs.poc.jenkinsmcpserver.client;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsCrumb;

/**
 * API abstraction for Jenkins operations.
 * This interface decouples the service layer from the underlying HTTP client implementation.
 */
public interface JenkinsApiClient {

    JenkinsBuildInfo getBuildInfo(String jobName, String branchName);

    JenkinsBuildVersionDetails getLatestBuildDetails(String jobName, String branchName, Integer buildNumber);

    JenkinsCrumb getCrumb();

    DeploymentResponse deployApplication(DeploymentRequest request);
}





