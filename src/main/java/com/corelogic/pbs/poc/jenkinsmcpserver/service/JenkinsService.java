package com.corelogic.pbs.poc.jenkinsmcpserver.service;

import com.corelogic.pbs.poc.jenkinsmcpserver.config.JenkinsProperties;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsCrumb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsService {

    private final RestClient jenkinsRestClient;
    private final JenkinsProperties jenkinsProperties;

    public JenkinsBuildInfo getBuildInformationByJobAndBranch(String jobName, String branchName) {
        log.info("Fetching build information for job: {} and branch: {}", jobName, branchName);

        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/api/json";

        JenkinsBuildInfo response = jenkinsRestClient.get()
                .uri(url, jobName, branchName)
                .retrieve()
                .body(JenkinsBuildInfo.class);

        log.info("Successfully retrieved build information");
        return response;
    }

    public JenkinsBuildVersionDetails getLatestBuildDetailsByJobAndBranch(String jobName, String branchName) {
        log.info("Fetching build version number for job: {} and branch: {}", jobName, branchName);

        // Step 1: Get build information
        JenkinsBuildInfo buildInfo = getBuildInformationByJobAndBranch(jobName, branchName);

        // Step 2: Get the lastBuild number
        Integer buildNumber = buildInfo.getLastBuild().getNumber();
        log.info("Last build number: {}", buildNumber);

        // Step 3: Call /credit-us/job/pbs/job/{jobName}/job/{branchName}/{number}/api/json
        String url = "/credit-us/job/pbs/job/{jobName}/job/{branchName}/{number}/api/json";

        JenkinsBuildVersionDetails response = jenkinsRestClient.get()
                .uri(url, jobName, branchName, buildNumber)
                .retrieve()
                .body(JenkinsBuildVersionDetails.class);

        log.info("Successfully retrieved build version details");
        return response;
    }

    public DeploymentResponse deployApplication(DeploymentRequest request) {
        log.info("Starting deployment process for repo: {}, branch: {}, version: {}, environments: {}",
                request.getGithubRepoName(), request.getBranchName(),
                request.getArtifactVersion(), request.getEnvsToDeployTo());

        // Step 1: Get crumb from crumbIssuer
        String crumbUrl = "/credit-us/crumbIssuer/api/json";

        JenkinsCrumb crumbResponse = jenkinsRestClient.get()
                .uri(crumbUrl)
                .retrieve()
                .body(JenkinsCrumb.class);

        String crumb = crumbResponse.getCrumb();
        String crumbRequestField = crumbResponse.getCrumbRequestField();

        // Step 2: Trigger the Jenkins job with the crumb
        String buildUrl = "/credit-us/job/pbs/job/build-release/job/build-release/buildWithParameters";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("GITHUB_REPO_NAME", request.getGithubRepoName());
        formData.add("BRANCH_NAME", request.getBranchName());
        formData.add("ARTIFACT_VERSION", request.getArtifactVersion());
        formData.add("ENVS_TO_DEPLOY", request.getEnvsToDeployTo());
        formData.add("BUILD", "");
        formData.add("Jenkins-Crumb", crumb);

        jenkinsRestClient.post()
                .uri(buildUrl)
                .header(crumbRequestField, crumb)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered Jenkins deployment job");

        // Step 3: Construct the deployment URL
        String deploymentUrl = jenkinsProperties.getBaseUrl() + "/credit-us/job/pbs/job/build-release/job/build-release/";
        String message = String.format("Successfully triggered deployment for %s (branch: %s, version: %s) to environments: %s",
                request.getGithubRepoName(), request.getBranchName(),
                request.getArtifactVersion(), request.getEnvsToDeployTo());

        log.info("Deployment URL: {}", deploymentUrl);

        return new DeploymentResponse(message, deploymentUrl);
    }

    public List<String> getJobs() {
        log.info("Retrieving jobs list");
        return jenkinsProperties.getJobs();
    }

    public List<String> getRepos() {
        log.info("Retrieving GitHub repos list");
        return jenkinsProperties.getIntegration().getGithub().getRepos();
    }
}






