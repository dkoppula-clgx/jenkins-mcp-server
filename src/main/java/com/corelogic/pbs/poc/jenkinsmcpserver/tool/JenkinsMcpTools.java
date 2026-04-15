package com.corelogic.pbs.poc.jenkinsmcpserver.tool;

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
import com.corelogic.pbs.poc.jenkinsmcpserver.service.JenkinsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP tools for Jenkins operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JenkinsMcpTools {

    private final JenkinsService jenkinsService;

    /**
     * Fetches all build details for specified parent and child jobs.
     * This tool retrieves comprehensive build information including references to all builds,
     * first build, last build, last successful build, last failed build, and more.
     */
    @McpTool(name = "getAllBuildDetails",
             description = """
                     Fetches all/recent build details for a job given the parentJob and childJob. \
                     Returns comprehensive build information including all builds, \
                     first build, last build, last successful build, last failed build, and more.\
                     """)
    public JenkinsBuildInfo getBuildDetails(
            @McpToolParam(description = "The name of the parent job") String parentJob,
            @McpToolParam(description = "The name of the child job") String childJob) {

        log.info("MCP Tool invoked: getBuildDetails - parentJob: {}, childJob: {}", parentJob, childJob);

        JenkinsBuildInfo result = jenkinsService.getRecentJobBuildDetails(parentJob, childJob);

        log.info("MCP Tool completed: getBuildDetails - total builds: {}", 
                result.getBuilds() != null ? result.getBuilds().size() : 0);

        return result;
    }

    /**
     * Fetches build version details for a specific build number of a job.
     * This tool retrieves detailed information for a particular build including
     * build number, result, timestamp, build version, and other relevant details.
     */
    @McpTool(name = "getBuildDetailsByBuildNumber",
             description = """
                     Fetches build version details for a specific build number of a job. \
                     Returns detailed information for a particular build including \
                     build number, result, timestamp, build version, and other relevant details.\
                     """)
    public JenkinsBuildVersionDetails getBuildDetailsByBuildNumber(
            @McpToolParam(description = "The name of the parent job") String parentJob,
            @McpToolParam(description = "The name of the child job") String childJob,
            @McpToolParam(description = "The build number to retrieve details for") Integer buildNumber) {

        log.info("MCP Tool invoked: getBuildDetailsByBuildNumber - parentJob: {}, childJob: {}, buildNumber: {}", 
                parentJob, childJob, buildNumber);

        JenkinsBuildVersionDetails result = jenkinsService.getJobBuildDetailsByBuildNumber(parentJob, childJob, buildNumber);

        log.info("MCP Tool completed: getBuildDetailsByBuildNumber - build number: {}", result.getNumber());

        return result;
    }

    /**
     * Fetches a list of all project-specific Jenkins jobs available in the system.
     * Returns individual application jobs configured for the current project.
     * MUST be called before performing any job-related actions to discover available jobs and their exact names.
     */
    @McpTool(name = "getAllProjectJobs",
             description = """
                     Fetches a list of all project-specific Jenkins jobs available in the system. \
                     Returns individual application jobs. \
                     **IMPORTANT: This tool MUST be called BEFORE performing any project-specific job-related actions** \
                      to discover available project-specific jobs and their exact names.\
                     """)
    public List<String> getAllProjectJobs() {
        log.info("MCP Tool invoked: getAllProjectJobs");

        AllJobsResponse response = jenkinsService.getJobs();
        List<String> projectJobs = response.getProjectJobs();

        log.info("MCP Tool completed: getAllProjectJobs - total jobs: {}", 
                projectJobs != null ? projectJobs.size() : 0);

        return projectJobs;
    }

    /**
     * Fetches a map of all common Jenkins jobs with their child jobs and descriptions.
     * Returns parent-child job relationships with descriptions for common operations.
     * MUST be called before performing any common job-related actions.
     */
    @McpTool(name = "getAllCommonJobs",
             description = """
                     Fetches a map of all common Jenkins jobs with their child jobs and descriptions. \
                     Returns parent-child job relationships with descriptions (e.g., build-release, self-service). \
                     Each child job includes its name and description of what it manages. \
                     **IMPORTANT: This tool MUST be called BEFORE performing any common job-related actions** \
                      to discover available common jobs, their descriptions, and their exact names.\
                     """)
    public Map<String, List<CommonJobInfo>> getAllCommonJobs() {
        log.info("MCP Tool invoked: getAllCommonJobs");

        AllJobsResponse response = jenkinsService.getJobs();
        Map<String, List<CommonJobInfo>> commonJobs = response.getCommonJobs();

        log.info("MCP Tool completed: getAllCommonJobs - total common job groups: {}", 
                commonJobs != null ? commonJobs.size() : 0);

        return commonJobs;
    }

    /**
     * Fetches a list of all GitHub repositories associated with Jenkins jobs.
     * MUST be called before performing any deployment-related actions to discover available repositories and their exact names.
     */
    @McpTool(name = "getAllRepos",
             description = """
                     Fetches a list of all GitHub repositories associated with Jenkins jobs. \
                     **IMPORTANT: This tool MUST be called BEFORE performing any deployment-related actions** \
                     (such as deployApplication) to discover available repositories and their exact names.\
                     """)
    public List<String> getAllRepos() {
        log.info("MCP Tool invoked: getAllRepos");

        List<String> repos = jenkinsService.getRepos();

        log.info("MCP Tool completed: getAllRepos - total repos: {}", repos != null ? repos.size() : 0);

        return repos;
    }

    /**
     * Deploys an application using Jenkins based on a deployment request.
     * This tool triggers a Jenkins deployment job with the specified parameters including
     * GitHub repository name, branch, artifact version, and target environments.
     */
    @McpTool(name = "deployApplication",
    description = """
                     Deploys an application using Jenkins. Triggers a deployment job with specified \
                     parameters including GitHub repository, branch, artifact version, and target environments.
                     Ensure to call getAllRepos() before using this tool to discover available repositories and their exact names.\
                     """)
    public String deployApplication(
            @McpToolParam(description = "The GitHub repository name (e.g., 'credit_us-pbs-am_input_handler')") String githubRepoName,
            @McpToolParam(description = "The Git branch to deploy from (e.g., 'master', 'develop', 'feature/CSIA-12345')") String branchName,
            @McpToolParam(description = "The artifact version to deploy. Expected format is numbers separated by dots (eg: 1.0.212)") String artifactVersion,
            @McpToolParam(description = "Comma-separated list of environments to deploy to (e.g., 'dev', 'qa', 'uat' or 'dev,qa')") String envsToDeployTo,
            @McpToolParam(description = "The Platform to deploy in. Supported platforms are 'kf' and 'cntv'.") String platform,
            @McpToolParam(description = "Region in format us{direction}1. Examples: 'usw1', 'use1', 'uss1', 'usn1', 'usc1'.") String region) {

        log.info("MCP Tool invoked: deployApplication - repo: {}, branch: {}, version: {}, envs: {}, platform: {}, region: {}",
                githubRepoName, branchName, artifactVersion, envsToDeployTo, platform, region);

        if (githubRepoName == null || githubRepoName.trim().isEmpty()) {
            throw new IllegalArgumentException("githubRepoName parameter is required.");
        }
        if (branchName == null || branchName.trim().isEmpty()) {
            throw new IllegalArgumentException("branchName parameter is required.");
        }
        if (artifactVersion == null || artifactVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("artifactVersion parameter is required.");
        }
        if (envsToDeployTo == null || envsToDeployTo.trim().isEmpty()) {
            throw new IllegalArgumentException("envsToDeployTo parameter is required.");
        }
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("region parameter is required.");
        }

        String transformedEnvs = transformEnvironmentNames(envsToDeployTo.trim(), platform, region.trim());

        DeploymentRequest request = new DeploymentRequest();
        request.setGithubRepoName(githubRepoName.trim());
        request.setBranchName(branchName.trim());
        request.setArtifactVersion(artifactVersion.trim());
        request.setEnvsToDeployTo(transformedEnvs);

        DeploymentResponse response = jenkinsService.deployApplication(request);

        String result = String.format("%s\n\nCheck deployment status at: %s",
                response.getMessage(), response.getDeploymentUrl());

        log.info("MCP Tool completed: deployApplication - {}", result);

        return result;
    }

    /**
     * Builds an application for a specified Jenkins job and branch.
     * This tool triggers a Jenkins build job and returns the job URL for monitoring the build progress.
     */
    @McpTool(name = "buildApplication",
             description = """
                     Builds an application for a specified Jenkins job and branch. \
                     Triggers a Jenkins build job and returns the job URL for monitoring the build progress.\
                     """)
    public String buildApplication(
            @McpToolParam(description = "The name of the Jenkins job") String jobName,
            @McpToolParam(description = "The name of the branch to build") String branchName) {

        log.info("MCP Tool invoked: buildApplication - job: {}, branch: {}", jobName, branchName);

        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("jobName parameter is required.");
        }
        if (branchName == null || branchName.trim().isEmpty()) {
            throw new IllegalArgumentException("branchName parameter is required.");
        }

        BuildResponse response = jenkinsService.buildApplication(jobName.trim(), branchName.trim());

        String result = String.format("%s\n\nMonitor build at: %s",
                response.getMessage(), response.getJobUrl());

        log.info("MCP Tool completed: buildApplication - {}", result);

        return result;
    }

    /**
     * Builds KF self-service for a specified environment with KF CLI commands.
     * This tool triggers a Jenkins KF self-service job and requires manual approval.
     */
    @McpTool(name = "manageKubernetesServicesInKfPlatform",
             description = """
                     Performs start, stop, restart and other operations on k8s services in kf platform. Use this to manage your applications on k8s with the ability to start, restart, stop (and any other similar operation) them\
                     Triggers a Jenkins job that requires manual approval to execute KF commands.\
                     """)
    public String buildKfSelfService(
            @McpToolParam(description = "The environment name (e.g., 'dev', 'qa', 'uat')") String environment,
            @McpToolParam(description = "The KF command to execute (e.g., 'restart', 'stop', 'start')") String kfCommand,
            @McpToolParam(description = "The job name (e.g., 'bps-coordinator', 'pbs-input-handler')") String jobName,
            @McpToolParam(description = "The version without 'v' prefix (e.g., '1.0.759' or '1-0-282')") String version,
            @McpToolParam(description = "Region in format us{direction}1. Examples: 'usw1', 'use1', 'uss1', 'usn1', 'usc1'.") String region) {

        log.info("MCP Tool invoked: buildKfSelfService - environment: {}, command: {}, job: {}, version: {}, region: {}",
                environment, kfCommand, jobName, version, region);

        if (environment == null || environment.trim().isEmpty()) {
            throw new IllegalArgumentException("environment parameter is required.");
        }
        if (kfCommand == null || kfCommand.trim().isEmpty()) {
            throw new IllegalArgumentException("kfCommand parameter is required.");
        }
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("jobName parameter is required.");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("version parameter is required.");
        }
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("region parameter is required.");
        }

        // Transform environment name: append -{region}-kf suffix
        String transformedEnv = transformEnvironmentName(environment.trim(), region.trim());

        // Build command parameters: combine jobName and version into required format
        String commandParameters = buildCommandParameters(jobName.trim(), version.trim());

        KfSelfServiceRequest request = new KfSelfServiceRequest();
        request.setEnvironment(transformedEnv);
        request.setKfCommand(kfCommand.trim());
        request.setKfCommandParameters(commandParameters);

        KfSelfServiceResponse response = jenkinsService.buildKfSelfService(request);

        String result = String.format("%s\n\n%s",
                response.getMessage(), response.getJobUrl());

        log.info("MCP Tool completed: buildKfSelfService - {}", result);

        return result;
    }

    /**
     * Runs a Veracode security scan on a specified application version.
     * This tool triggers a Jenkins Veracode scan job with the specified scan type and patterns.
     */
    @McpTool(name = "runVeracodeScan",
             description = """
                     Runs a Veracode security scan on a project-specific job with the specified version. \
                     Ensure to use the exact job name retrieved from getAllProjectJobs tool. \
                     Triggers a Jenkins Veracode scan job with the specified scan type and patterns.\
                     """)
    public String runVeracodeScan(
            @McpToolParam(description = "The artifact version to scan (e.g., '1.0.71')") String version,
            @McpToolParam(description = "The job name (e.g., 'bps-coordinator', 'pbs-input-handler')") String jobName) {

        log.info("MCP Tool invoked: runVeracodeScan - job: {}, version: {}",
                jobName, version);

        if (version == null || version.trim().isEmpty()) {
            throw new IllegalArgumentException("version parameter is required.");
        }
        if (jobName == null || jobName.trim().isEmpty()) {
            throw new IllegalArgumentException("jobName parameter is required.");
        }

        VeracodeScanRequest request = new VeracodeScanRequest();
        request.setVersion(version.trim());
        request.setJobName(jobName.trim());
        request.setScanType("policy");
        request.setExcludePattern("");
        request.setIncludePattern("*");

        VeracodeScanResponse response = jenkinsService.runVeracodeScan(request);

        String result = String.format("%s\n\nCheck scan status at: %s",
                response.getMessage(), response.getScanUrl());

        log.info("MCP Tool completed: runVeracodeScan - {}", result);

        return result;
    }

    /**
     * Transforms a single environment name by appending -{region}-kf suffix.
     * Example: "dev", "usw1" -> "dev-usw1-kf"
     *
     * @param env environment name
     * @param region normalized region (e.g., "usw1")
     * @return transformed environment name with -{region}-kf suffix
     */
    private String transformEnvironmentName(String env, String region) {
        String suffix = "-" + region + "-kf";
        if (env.endsWith(suffix)) {
            return env;
        }
        return env + suffix;
    }

    /**
     * Builds command parameters by combining jobName and version into the required format.
     * Converts version separators (dots or hyphens) to hyphens and adds 'v' prefix.
     * Examples:
     * - jobName: "bps-coordinator", version: "1.0.759" -> "bps-coordinator-v-1-0-759"
     * - jobName: "pbs-input-handler", version: "1-0-282" -> "pbs-input-handler-v-1-0-282"
     *
     * @param jobName the job name (e.g., "bps-coordinator")
     * @param version the version without 'v' prefix (e.g., "1.0.759" or "1-0-282")
     * @return formatted command parameters "jobName-v-version_separated_by_hyphens"
     */
    private String buildCommandParameters(String jobName, String version) {
        // Replace dots with hyphens in version
        String normalizedVersion = version.replace(".", "-");

        // Combine: jobName + "-v-" + normalizedVersion
        return jobName + "-v" + normalizedVersion;
    }

    /**
     * Transforms environment names by appending -{region}-{platform} suffix to each environment.
     * Examples:
     * - "dev", "kf", "usw1" -> "dev-usw1-kf"
     * - "dev,qa", "kf", "usw1" -> "dev-usw1-kf,qa-usw1-kf"
     * - "dev,uat", "cntv", "use1" -> "dev-use1-cntv,uat-use1-cntv"
     *
     * @param envs comma-separated list of environment names
     * @param platform the platform (kf or cntv)
     * @param region normalized region (e.g., "usw1")
     * @return transformed comma-separated list with -{region}-{platform} suffix
     */
    private String transformEnvironmentNames(String envs, String platform, String region) {
        String suffix = "-" + region + "-" + platform;
        return envs.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(env -> !env.isEmpty())
                .map(env -> env.endsWith(suffix) ? env : env + suffix)
                .collect(Collectors.joining(","));
    }
}


