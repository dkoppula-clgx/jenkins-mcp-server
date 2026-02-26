package com.corelogic.pbs.poc.jenkinsmcpserver.tool;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.BuildResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.service.JenkinsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP tools for Jenkins operations
 */
@Slf4j
@Component
@ConditionalOnBean(JenkinsService.class)
@RequiredArgsConstructor
public class JenkinsMcpTools {

    private final JenkinsService jenkinsService;

    /**
     * Fetches the latest build details for a specified Jenkins job and branch.
     * This tool retrieves the most recent build information including build number,
     * result, timestamp, build version, and other relevant details.
     */
    @McpTool(name = "getLatestBuildDetailsByJobAndBranch",
             description = "Fetches the latest build details for a specified Jenkins job and branch. " +
                          "Returns the most recent build information including build number, result, " +
                          "timestamp, build version, and other relevant details.")
    public JenkinsBuildVersionDetails getLatestBuildDetailsByJobAndBranch(
            @McpToolParam(description = "The name of the Jenkins job") String jobName,
            @McpToolParam(description = "The name of the branch to filter builds by") String branchName) {

        log.info("MCP Tool invoked: getLatestBuildDetailsByJobAndBranch - job: {}, branch: {}", jobName, branchName);

        JenkinsBuildVersionDetails result = jenkinsService.getLatestBuildDetailsByJobAndBranch(jobName, branchName);

        log.info("MCP Tool completed: getLatestBuildDetailsByJobAndBranch - build number: {}", result.getNumber());

        return result;
    }

    /**
     * Fetches the details of all builds for a specified Jenkins job and branch.
     * This tool retrieves a comprehensive list of all builds including references to
     * first build, last build, last successful build, last failed build, and more.
     */
    @McpTool(name = "getBuildDetailsByJobAndBranch",
             description = "Fetches the details of all builds for a specified Jenkins job and branch. " +
                          "Returns a comprehensive list of all builds including references to " +
                          "first build, last build, last successful build, last failed build, and more.")
    public JenkinsBuildInfo getBuildDetailsByJobAndBranch(
            @McpToolParam(description = "The name of the Jenkins job") String jobName,
            @McpToolParam(description = "The name of the branch to filter builds by") String branchName) {

        log.info("MCP Tool invoked: getBuildDetailsByJobAndBranch - job: {}, branch: {}", jobName, branchName);

        JenkinsBuildInfo result = jenkinsService.getBuildInformationByJobAndBranch(jobName, branchName);

        log.info("MCP Tool completed: getBuildDetailsByJobAndBranch - total builds: {}",
                result.getBuilds() != null ? result.getBuilds().size() : 0);

        return result;
    }

    /**
     * Fetches a list of all Jenkins jobs available in the system.
     * Use this to discover available jobs and their names before querying build details.
     */
    @McpTool(name = "getAllJobs",
             description = "Fetches a list of all Jenkins jobs available in the system. " +
                          "Use this to discover available jobs and their names before querying build details.")
    public List<String> getAllJobs() {
        log.info("MCP Tool invoked: getAllJobs");

        List<String> jobs = jenkinsService.getJobs();

        log.info("MCP Tool completed: getAllJobs - total jobs: {}", jobs != null ? jobs.size() : 0);

        return jobs;
    }

    /**
     * Fetches a list of all GitHub repositories associated with Jenkins jobs.
     * These repositories are used to deploy code from and represent the available services that can be deployed.
     */
    @McpTool(name = "getAllRepos",
             description = "Fetches a list of all GitHub repositories associated with Jenkins jobs. " +
                          "These repositories are used to deploy code from. Use this to discover available repositories and their names before triggering deployments.")
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
             description = "Deploys an application using Jenkins. Triggers a deployment job with specified " +
                          "parameters including GitHub repository, branch, artifact version, and target environments.")
    public String deployApplication(
            @McpToolParam(description = "The GitHub repository name (e.g., 'credit_us-pbs-am_input_handler')") String githubRepoName,
            @McpToolParam(description = "The Git branch to deploy from (e.g., 'master', 'develop')") String branchName,
            @McpToolParam(description = "The artifact version to deploy (e.g., '1.0.71')") String artifactVersion,
            @McpToolParam(description = "Comma-separated list of environments to deploy to (e.g., 'dev', 'qa', 'uat' or 'dev,qa')") String envsToDeployTo) {

        log.info("MCP Tool invoked: deployApplication - repo: {}, branch: {}, version: {}, envs: {}",
                githubRepoName, branchName, artifactVersion, envsToDeployTo);

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

        String transformedEnvs = transformEnvironmentNames(envsToDeployTo.trim());

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
             description = "Builds an application for a specified Jenkins job and branch. " +
                          "Triggers a Jenkins build job and returns the job URL for monitoring the build progress.")
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
    @McpTool(name = "buildKfSelfService",
             description = "Builds KF self-service for a specified environment with KF CLI commands. " +
                          "Triggers a Jenkins job that requires manual approval to execute KF commands.")
    public String buildKfSelfService(
            @McpToolParam(description = "The environment name (e.g., 'dev', 'qa', 'uat')") String environment,
            @McpToolParam(description = "The KF command to execute (e.g., 'restart', 'stop', 'start')") String kfCommand,
            @McpToolParam(description = "The job name (e.g., 'bps-coordinator', 'pbs-input-handler')") String jobName,
            @McpToolParam(description = "The version without 'v' prefix (e.g., '1.0.759' or '1-0-282')") String version) {

        log.info("MCP Tool invoked: buildKfSelfService - environment: {}, command: {}, job: {}, version: {}",
                environment, kfCommand, jobName, version);

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

        // Transform environment name: append -usw1-kf suffix
        String transformedEnv = transformEnvironmentName(environment.trim());

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
     * Transforms a single environment name by appending -usw1-kf suffix.
     * Example: "dev" -> "dev-usw1-kf"
     *
     * @param env environment name
     * @return transformed environment name with -usw1-kf suffix
     */
    private String transformEnvironmentName(String env) {
        if (env.endsWith("-usw1-kf")) {
            return env;
        }
        return env + "-usw1-kf";
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
        return jobName + "-v-" + normalizedVersion;
    }

    /**
     * Transforms environment names by appending -usw1-kf suffix to each environment.
     * Examples:
     * - "dev" -> "dev-usw1-kf"
     * - "dev,qa" -> "dev-usw1-kf,qa-usw1-kf"
     * - "dev,uat" -> "dev-usw1-kf,uat-usw1-kf"
     *
     * @param envs comma-separated list of environment names
     * @return transformed comma-separated list with -usw1-kf suffix
     */
    private String transformEnvironmentNames(String envs) {
        return envs.lines()
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(env -> !env.isEmpty())
                .map(env -> env.endsWith("-usw1-kf") ? env : env + "-usw1-kf")
                .collect(Collectors.joining(","));
    }
}



