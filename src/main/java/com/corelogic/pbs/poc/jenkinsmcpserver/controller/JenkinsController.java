package com.corelogic.pbs.poc.jenkinsmcpserver.controller;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.service.JenkinsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/jenkins")
@RequiredArgsConstructor
public class JenkinsController {

    private final JenkinsService jenkinsService;

    @GetMapping("/build")
    public ResponseEntity<JenkinsBuildInfo> getBuildInformation(
            @RequestParam String job,
            @RequestParam String branch) {

        log.info("Received request for job: {} and branch: {}", job, branch);

        JenkinsBuildInfo buildInfo = jenkinsService.getBuildInformationByJobAndBranch(job, branch);

        return ResponseEntity.ok(buildInfo);
    }

    @GetMapping("/build-version")
    public ResponseEntity<JenkinsBuildVersionDetails> getLatestBuildDetailsByJobAndBranch(
            @RequestParam String job,
            @RequestParam String branch) {

        log.info("Received request for build version - job: {} and branch: {}", job, branch);

        JenkinsBuildVersionDetails buildVersion = jenkinsService.getLatestBuildDetailsByJobAndBranch(job, branch);

        return ResponseEntity.ok(buildVersion);
    }

    @PostMapping("/deploy")
    public ResponseEntity<Void> deployApplication(@RequestBody DeploymentRequest request) {
        log.info("Received deployment request for repo: {}", request.getGithubRepoName());

        jenkinsService.deployApplication(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<String>> getJobs() {
        log.info("Received request for jobs list");

        List<String> jobs = jenkinsService.getJobs();

        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/repos")
    public ResponseEntity<List<String>> getRepos() {
        log.info("Received request for GitHub repos list");

        List<String> repos = jenkinsService.getRepos();

        return ResponseEntity.ok(repos);
    }
}

