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
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanResponse;
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

    private static final String JOB_BUILD_INFO_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/{jobName}/job/{branchName}/api/json";
    private static final String JOB_BUILD_DETAILS_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/{jobName}/job/{branchName}/{buildNumber}/api/json";
    private static final String CRUMB_URL = "/{businessUnitJob}/crumbIssuer/api/json";
    private static final String DEPLOYMENT_BASE_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/build-release/job/build-release";
    private static final String BUILD_JOB_BASE_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/{jobName}/job/{branchName}";
    private static final String KF_SELF_SERVICE_BASE_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/self-service/job/kf-cli-execution";
    private static final String VERACODE_SCAN_BASE_URL = "/{businessUnitJob}/job/{projectSpaceJob}/job/self-service/job/veracodescan-ondemand";

    @Override
    public JenkinsBuildInfo getBuildInfo(String jobName, String branchName) {
        log.info("Calling Jenkins API to get build info for job: {} and branch: {}", jobName, branchName);

        JenkinsBuildInfo response = jenkinsRestClient.get()
                .uri(JOB_BUILD_INFO_URL,
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob(),
                        jobName,
                        branchName)
                .retrieve()
                .body(JenkinsBuildInfo.class);

        log.info("Successfully retrieved build info from Jenkins API");
        return response;
    }

    @Override
    public JenkinsBuildVersionDetails getBuildDetailsByBuildNumber(String jobName, String branchName, Integer buildNumber) {
        log.info("Calling Jenkins API to get build details for job: {}, branch: {}, build: {}",
                jobName, branchName, buildNumber);

        JenkinsBuildVersionDetails response = jenkinsRestClient.get()
                .uri(JOB_BUILD_DETAILS_URL,
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob(),
                        jobName,
                        branchName,
                        buildNumber)
                .retrieve()
                .body(JenkinsBuildVersionDetails.class);

        log.info("Successfully retrieved build version details from Jenkins API");
        return response;
    }

    @Override
    public JenkinsCrumb getCrumb() {
        log.info("Calling Jenkins API to get CSRF crumb");

        JenkinsCrumb crumbResponse = jenkinsRestClient.get()
                .uri(CRUMB_URL,
                        jenkinsProperties.getApiPaths().getBusinessUnitJob())
                .retrieve()
                .body(JenkinsCrumb.class);

        log.info("Successfully retrieved crumb from Jenkins API");
        return crumbResponse;
    }

    @Override
    public DeploymentResponse deployApplication(DeploymentRequest request) {
        log.info("Calling Jenkins API to trigger deployment for repo: {}", request.getGithubRepoName());

        JenkinsCrumb crumb = getCrumb();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("GITHUB_REPO_NAME", request.getGithubRepoName());
        formData.add("BRANCH_NAME", request.getBranchName());
        formData.add("ARTIFACT_VERSION", request.getArtifactVersion());
        formData.add("ENVS_TO_DEPLOY", request.getEnvsToDeployTo());
        formData.add("BUILD", "");
        formData.add("Jenkins-Crumb", crumb.getCrumb());

        jenkinsRestClient.post()
                .uri(DEPLOYMENT_BASE_URL + "/buildWithParameters",
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob())
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered deployment via Jenkins API");

        String deploymentUrl = """
                %s%s""".formatted(
                jenkinsProperties.getBaseUrl(),
                DEPLOYMENT_BASE_URL
                        .replace("{businessUnitJob}", jenkinsProperties.getApiPaths().getBusinessUnitJob())
                        .replace("{projectSpaceJob}", jenkinsProperties.getApiPaths().getProjectSpaceJob()));

        return new DeploymentResponse(
                String.format("Successfully triggered deployment for %s (branch: %s, version: %s) to environments: %s",
                        request.getGithubRepoName(), request.getBranchName(),
                        request.getArtifactVersion(), request.getEnvsToDeployTo()),
                deploymentUrl);
    }

    @Override
    public BuildResponse buildApplication(String jobName, String branchName) {
        log.info("Calling Jenkins API to build application for job: {} and branch: {}", jobName, branchName);

        JenkinsCrumb crumb = getCrumb();

        jenkinsRestClient.post()
                .uri(BUILD_JOB_BASE_URL + "/build?delay=0sec",
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob(),
                        jobName,
                        branchName)
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered build via Jenkins API");

        String jobUrl = """
                %s%s""".formatted(
                jenkinsProperties.getBaseUrl(),
                BUILD_JOB_BASE_URL
                        .replace("{businessUnitJob}", jenkinsProperties.getApiPaths().getBusinessUnitJob())
                        .replace("{projectSpaceJob}", jenkinsProperties.getApiPaths().getProjectSpaceJob())
                        .replace("{jobName}", jobName)
                        .replace("{branchName}", URLEncoder.encode(branchName, StandardCharsets.UTF_8)));

        return new BuildResponse(
                String.format("Successfully triggered build for job: %s, branch: %s", jobName, branchName),
                jobUrl);
    }

    @Override
    public KfSelfServiceResponse buildKfSelfService(KfSelfServiceRequest request) {
        log.info("Calling Jenkins API to build KF self-service for environment: {}", request.getEnvironment());

        JenkinsCrumb crumb = getCrumb();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("Environment", request.getEnvironment());
        formData.add("KF Command", request.getKfCommand());
        formData.add("KF Command Parameters", request.getKfCommandParameters());
        formData.add("Jenkins-Crumb", crumb.getCrumb());

        jenkinsRestClient.post()
                .uri(KF_SELF_SERVICE_BASE_URL + "/buildWithParameters",
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob())
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered KF self-service build via Jenkins API");

        String jobUrl = """
                %s%s""".formatted(
                jenkinsProperties.getBaseUrl(),
                KF_SELF_SERVICE_BASE_URL
                        .replace("{businessUnitJob}", jenkinsProperties.getApiPaths().getBusinessUnitJob())
                        .replace("{projectSpaceJob}", jenkinsProperties.getApiPaths().getProjectSpaceJob()));

        return new KfSelfServiceResponse(
                String.format("Successfully triggered KF self-service build for environment: %s, command: %s. Please follow the URL and approve the build.",
                        request.getEnvironment(), request.getKfCommand()),
                jobUrl);
    }

    @Override
    public VeracodeScanResponse runVeracodeScan(VeracodeScanRequest request) {
        log.info("Calling Jenkins API to run Veracode scan for application: {}", request.getJobName());

        JenkinsCrumb crumb = getCrumb();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("ARTIFACT_VERSION", request.getVersion());
        formData.add("APPLICATION_NAME", request.getJobName());
        formData.add("SCAN_TYPE", request.getScanType());
        formData.add("EXCLUDE_PATTERN", request.getExcludePattern());
        formData.add("INCLUDE_PATTERN", request.getIncludePattern());
        formData.add("Jenkins-Crumb", crumb.getCrumb());

        jenkinsRestClient.post()
                .uri(VERACODE_SCAN_BASE_URL + "/buildWithParameters",
                        jenkinsProperties.getApiPaths().getBusinessUnitJob(),
                        jenkinsProperties.getApiPaths().getProjectSpaceJob())
                .header(crumb.getCrumbRequestField(), crumb.getCrumb())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Successfully triggered Veracode scan via Jenkins API");

        String scanUrl = """
                %s%s""".formatted(
                jenkinsProperties.getBaseUrl(),
                VERACODE_SCAN_BASE_URL
                        .replace("{businessUnitJob}", jenkinsProperties.getApiPaths().getBusinessUnitJob())
                        .replace("{projectSpaceJob}", jenkinsProperties.getApiPaths().getProjectSpaceJob()));

        return new VeracodeScanResponse(
                String.format("Successfully triggered Veracode scan for application: %s, version: %s, scan type: %s",
                        request.getJobName(), request.getVersion(), request.getScanType()),
                scanUrl);
    }
}




