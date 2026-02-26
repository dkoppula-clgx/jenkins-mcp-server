package com.corelogic.pbs.poc.jenkinsmcpserver.tool;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.service.JenkinsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

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
            @McpToolParam(description = "Comma-separated list of environments to deploy to (e.g., 'dev-usw1-kf', 'qa-usw1-kf')") String envsToDeployTo) {

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

        DeploymentRequest request = new DeploymentRequest();
        request.setGithubRepoName(githubRepoName.trim());
        request.setBranchName(branchName.trim());
        request.setArtifactVersion(artifactVersion.trim());
        request.setEnvsToDeployTo(envsToDeployTo.trim());

        jenkinsService.deployApplication(request);

        String successMessage = String.format("Successfully triggered deployment for %s (branch: %s, version: %s) to environments: %s",
                githubRepoName, branchName, artifactVersion, envsToDeployTo);

        log.info("MCP Tool completed: deployApplication - {}", successMessage);

        return successMessage;
    }
}



