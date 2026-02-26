package com.corelogic.pbs.poc.jenkinsmcpserver.service;

import com.corelogic.pbs.poc.jenkinsmcpserver.client.JenkinsApiClient;
import com.corelogic.pbs.poc.jenkinsmcpserver.config.JenkinsProperties;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsService {

    private final JenkinsApiClient jenkinsApiRestClient;
    private final JenkinsProperties jenkinsProperties;

    public JenkinsBuildInfo getBuildInformationByJobAndBranch(String jobName, String branchName) {
        log.info("Fetching build information for job: {} and branch: {}", jobName, branchName);

        JenkinsBuildInfo response = jenkinsApiRestClient.getBuildInfo(jobName, branchName);

        log.info("Successfully retrieved build information");
        return response;
    }

    public JenkinsBuildVersionDetails getLatestBuildDetailsByJobAndBranch(String jobName, String branchName) {
        log.info("Fetching build version number for job: {} and branch: {}", jobName, branchName);

        JenkinsBuildInfo buildInfo = getBuildInformationByJobAndBranch(jobName, branchName);

        Integer buildNumber = buildInfo.getLastBuild().getNumber();
        log.info("Last build number: {}", buildNumber);

        JenkinsBuildVersionDetails response = jenkinsApiRestClient.getLatestBuildDetails(jobName, branchName, buildNumber);

        log.info("Successfully retrieved build version details");
        return response;
    }

    public DeploymentResponse deployApplication(DeploymentRequest request) {
        log.info("Starting deployment process for repo: {}, branch: {}, version: {}, environments: {}",
                request.getGithubRepoName(), request.getBranchName(),
                request.getArtifactVersion(), request.getEnvsToDeployTo());

        DeploymentResponse response = jenkinsApiRestClient.deployApplication(request);

        log.info("Successfully triggered Jenkins deployment job");
        log.info("Deployment URL: {}", response.getDeploymentUrl());

        return response;
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






