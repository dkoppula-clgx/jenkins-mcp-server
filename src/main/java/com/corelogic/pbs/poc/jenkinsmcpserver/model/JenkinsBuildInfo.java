package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JenkinsBuildInfo {
    private List<BuildReference> builds;
    private BuildReference firstBuild;
    private BuildReference lastBuild;
    private BuildReference lastCompletedBuild;
    private BuildReference lastFailedBuild;
    private BuildReference lastStableBuild;
    private BuildReference lastSuccessfulBuild;
    private BuildReference lastUnstableBuild;
    private BuildReference lastUnsuccessfulBuild;
}


