package com.corelogic.pbs.poc.jenkinsmcpserver.client;

import com.corelogic.pbs.poc.jenkinsmcpserver.config.JenkinsProperties;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsCrumb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * RestClient-based implementation of JenkinsApiClient.
 * Handles all HTTP communication with Jenkins API using Spring's RestClient.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsApiRestClient implements JenkinsApiClient {

    private final RestClient jenkinsRestClient;
    private final JenkinsProperties jenkinsProperties;

    @Override
    public JenkinsBuildInfo getBuildInfo(String jobName, String branchName) {
        log.debug("Calling Jenkins API to get build info for job: {} and branch: {}", jobName, branchName);

        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/api/json";

        JenkinsBuildInfo response = jenkinsRestClient.get()
                .uri(url, jobName, branchName)
                .retrieve()
                .body(JenkinsBuildInfo.class);

        log.debug("Successfully retrieved build info from Jenkins API");
        return response;
    }

    @Override
    public JenkinsBuildVersionDetails getLatestBuildDetails(String jobName, String branchName, Integer buildNumber) {
        log.debug("Calling Jenkins API to get build details for job: {}, branch: {}, build: {}",
                jobName, branchName, buildNumber);

        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/{number}/api/json";

        JenkinsBuildVersionDetails response = jenkinsRestClient.get()
                .uri(url, jobName, branchName, buildNumber)
                .retrieve()
                .body(JenkinsBuildVersionDetails.class);

        log.debug("Successfully retrieved build version details from Jenkins API");
        return response;
    }

    @Override
    public JenkinsCrumb getCrumb() {
        log.debug("Calling Jenkins API to get CSRF crumb");

        String crumbUrl = "/credit-us/crumbIssuer/api/json";

        JenkinsCrumb crumbResponse = jenkinsRestClient.get()
                .uri(crumbUrl)
                .retrieve()
                .body(JenkinsCrumb.class);

        log.debug("Successfully retrieved crumb from Jenkins API");
        return crumbResponse;
    }

    @Override
    public DeploymentResponse deployApplication(DeploymentRequest request) {
        log.debug("Calling Jenkins API to trigger deployment for repo: {}", request.getGithubRepoName());

        JenkinsCrumb crumb = getCrumb();
        log.debug("Retrieved crumb for deployment request");

        String buildUrl = "/credit-us/job/pbs/job/build-release/job/build-release/buildWithParameters";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("GITHUB_REPO_NAME", request.getGithubRepoName());
        formData.add("BRANCH_NAME", request.getBranchName());
        formData.add("ARTIFACT_VERSION", request.getArtifactVersion());
        formData.add("ENVS_TO_DEPLOY", request.getEnvsToDeployTo());
        formData.add("BUILD", "");
        formData.add("Jenkins-Crumb", crumb.getCrumb());

        jenkinsRestClient.post()
                .uri(buildUrl)
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.debug("Successfully triggered deployment via Jenkins API");

        String deploymentUrl = jenkinsProperties.getBaseUrl() + "/credit-us/job/pbs/job/build-release/job/build-release/";
        String message = String.format("Successfully triggered deployment for %s (branch: %s, version: %s) to environments: %s",
                request.getGithubRepoName(), request.getBranchName(),
                request.getArtifactVersion(), request.getEnvsToDeployTo());

        return new DeploymentResponse(message, deploymentUrl);
    }
}




