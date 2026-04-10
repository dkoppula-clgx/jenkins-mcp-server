package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO containing both project-specific jobs and common jobs.
 * This DTO provides a comprehensive view of all available Jenkins jobs in the system.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllJobsResponse {
    /**
     * List of project-specific Jenkins jobs configured for the current project.
     * These are the individual application jobs like pbs-input-handler, bps-coordinator, etc.
     */
    private List<String> projectJobs;

    /**
     * Map of common jobs with their child jobs and descriptions.
     * Key: Parent job name (e.g., "build-release", "self-service")
     * Value: List of CommonJobInfo objects containing child job names and descriptions
     */
    private Map<String, List<CommonJobInfo>> commonJobs;
}

