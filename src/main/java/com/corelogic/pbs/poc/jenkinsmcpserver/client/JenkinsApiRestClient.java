package com.corelogic.pbs.poc.jenkinsmcpserver.client;

import com.corelogic.pbs.poc.jenkinsmcpserver.config.JenkinsProperties;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.BuildResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsCrumb;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        log.info("Calling Jenkins API to get build info for job: {} and branch: {}", jobName, branchName);

        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/api/json";

        JenkinsBuildInfo response = jenkinsRestClient.get()
                .uri(url, jobName, branchName)
                .retrieve()
                .body(JenkinsBuildInfo.class);

        log.info("Successfully retrieved build info from Jenkins API");
        return response;
    }

    @Override
    public JenkinsBuildVersionDetails getLatestBuildDetails(String jobName, String branchName, Integer buildNumber) {
        log.info("Calling Jenkins API to get build details for job: {}, branch: {}, build: {}",
                jobName, branchName, buildNumber);

        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/{number}/api/json";

        JenkinsBuildVersionDetails response = jenkinsRestClient.get()
                .uri(url, jobName, branchName, buildNumber)
                .retrieve()
                .body(JenkinsBuildVersionDetails.class);

        log.info("Successfully retrieved build version details from Jenkins API");
        return response;
    }

    @Override
    public JenkinsCrumb getCrumb() {
        log.info("Calling Jenkins API to get CSRF crumb");

        String crumbUrl = "/credit-us/crumbIssuer/api/json";

        JenkinsCrumb crumbResponse = jenkinsRestClient.get()
                .uri(crumbUrl)
                .retrieve()
                .body(JenkinsCrumb.class);

        log.info("Successfully retrieved crumb from Jenkins API");
        return crumbResponse;
    }

    @Override
    public DeploymentResponse deployApplication(DeploymentRequest request) {
        log.info("Calling Jenkins API to trigger deployment for repo: {}", request.getGithubRepoName());

        JenkinsCrumb crumb = getCrumb();
        log.info("Retrieved crumb for deployment request");

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

        log.info("Successfully triggered deployment via Jenkins API");

        String deploymentUrl = jenkinsProperties.getBaseUrl() + "/credit-us/job/pbs/job/build-release/job/build-release/";
        String message = String.format("Successfully triggered deployment for %s (branch: %s, version: %s) to environments: %s",
                request.getGithubRepoName(), request.getBranchName(),
                request.getArtifactVersion(), request.getEnvsToDeployTo());

        return new DeploymentResponse(message, deploymentUrl);
    }

    @Override
    public BuildResponse buildApplication(String jobName, String branchName) {
        log.info("Calling Jenkins API to build application for job: {} and branch: {}", jobName, branchName);

        // Step 1: Get crumb from Jenkins
        JenkinsCrumb crumb = getCrumb();
        log.info("Retrieved crumb for build request");

        // Step 2: Trigger build with crumb
        String buildUrl = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/build?delay=0sec";

        jenkinsRestClient.post()
                .uri(buildUrl, jobName, branchName)
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered build via Jenkins API");

        // Step 3: Construct the job URL and message
        String jobUrl = jenkinsProperties.getBaseUrl() + "/credit-us/job/pbs/job/" + jobName + "/job/" + URLEncoder.encode(branchName, StandardCharsets.UTF_8) + "/";
        String message = String.format("Successfully triggered build for job: %s, branch: %s", jobName, branchName);

        log.info("Build job URL: {}", jobUrl);

        return new BuildResponse(message, jobUrl);
    }

    @Override
    public KfSelfServiceResponse buildKfSelfService(KfSelfServiceRequest request) {
        log.info("Calling Jenkins API to build KF self-service for environment: {}", request.getEnvironment());

        // Step 1: Get crumb from Jenkins
        JenkinsCrumb crumb = getCrumb();
        log.info("Retrieved crumb for KF self-service request");

        // Step 2: Trigger build with parameters
        String buildUrl = "/credit-us/job/pbs/job/self-service/job/kf-cli-execution/buildWithParameters";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Environment", request.getEnvironment());
        formData.add("KF Command", request.getKfCommand());
        formData.add("KF Command Parameters", request.getKfCommandParameters());
        formData.add("Jenkins-Crumb", crumb.getCrumb());

        jenkinsRestClient.post()
                .uri(buildUrl)
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered KF self-service build via Jenkins API");

        // Step 3: Construct the job URL and message
        String jobUrl = jenkinsProperties.getBaseUrl() + "/credit-us/job/pbs/job/self-service/job/kf-cli-execution/";
        String message = String.format("Successfully triggered KF self-service build for environment: %s, command: %s. Please follow the URL and approve the build.",
                request.getEnvironment(), request.getKfCommand());

        log.info("KF self-service job URL: {}", jobUrl);

        return new KfSelfServiceResponse(message, jobUrl);
    }
}




