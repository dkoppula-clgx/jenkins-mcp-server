package com.corelogic.pbs.poc.jenkinsmcpserver.service;

import com.corelogic.pbs.poc.jenkinsmcpserver.client.JenkinsApiClient;
import com.corelogic.pbs.poc.jenkinsmcpserver.config.JenkinsProperties;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.AllJobsResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.BuildResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.CommonJobInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JenkinsService {

    private final JenkinsApiClient jenkinsApiRestClient;
    private final JenkinsProperties jenkinsProperties;

    public JenkinsBuildInfo getRecentJobBuildDetails(String parentJob, String childJob) {
        log.info("Fetching build information for job: {}/{}", parentJob, childJob);

        JenkinsBuildInfo response = jenkinsApiRestClient.getBuildInfo(parentJob, childJob);

        log.info("Successfully retrieved build information");
        return response;
    }

    public JenkinsBuildVersionDetails getLatestJobBuildDetails(String parentJob, String childJob) {
        log.info("Fetching latest build information for job: {}/{}", parentJob, childJob);

        JenkinsBuildInfo buildInfo = getRecentJobBuildDetails(parentJob, childJob);

        Integer buildNumber = buildInfo.getLastBuild().getNumber();
        log.info("Last build number: {}", buildNumber);

        return getJobBuildDetailsByBuildNumber(parentJob, childJob, buildNumber);
    }

    public JenkinsBuildVersionDetails getJobBuildDetailsByBuildNumber(String parentJob, String childJob,  Integer buildNumber) {
        log.info("Fetching build information for job: {}/{} and build number: {}", parentJob, childJob, buildNumber);

        JenkinsBuildVersionDetails response = jenkinsApiRestClient.getBuildDetailsByBuildNumber(parentJob, childJob, buildNumber);

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

    public BuildResponse buildApplication(String jobName, String branchName) {
        log.info("Starting build process for job: {} and branch: {}", jobName, branchName);

        BuildResponse response = jenkinsApiRestClient.buildApplication(jobName, branchName);

        log.info("Successfully triggered Jenkins build job");
        log.info("Job URL: {}", response.getJobUrl());

        return response;
    }

    public KfSelfServiceResponse buildKfSelfService(KfSelfServiceRequest request) {
        log.info("Starting KF self-service build for environment: {}, command: {}",
                request.getEnvironment(), request.getKfCommand());

        KfSelfServiceResponse response = jenkinsApiRestClient.buildKfSelfService(request);

        log.info("Successfully triggered KF self-service build");
        log.info("Job URL: {}", response.getJobUrl());

        return response;
    }

    public AllJobsResponse getJobs() {
        log.info("Retrieving all jobs (project-specific and common)");
        
        List<String> projectJobs = jenkinsProperties.getProjectSpecificJobs();
        Map<String, List<CommonJobInfo>> commonJobs = jenkinsProperties.getCommonJobs();
        
        AllJobsResponse response = new AllJobsResponse(projectJobs, commonJobs);
        
        log.info("Retrieved {} project jobs and {} common job groups", 
                projectJobs != null ? projectJobs.size() : 0,
                commonJobs != null ? commonJobs.size() : 0);
        
        return response;
    }

    public List<String> getRepos() {
        log.info("Retrieving GitHub repos list");
        return jenkinsProperties.getIntegration().getGithub().getRepos();
    }

    public VeracodeScanResponse runVeracodeScan(VeracodeScanRequest request) {
        log.info("Starting Veracode scan for application: {}, version: {}, scan type: {}",
                request.getJobName(), request.getVersion(), request.getScanType());

        VeracodeScanResponse response = jenkinsApiRestClient.runVeracodeScan(request);

        log.info("Successfully triggered Veracode scan");
        log.info("Scan URL: {}", response.getScanUrl());

        return response;
    }
}






