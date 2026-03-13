package com.corelogic.pbs.poc.jenkinsmcpserver.controller;

import com.corelogic.pbs.poc.jenkinsmcpserver.model.BuildResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.DeploymentResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildVersionDetails;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.JenkinsBuildInfo;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.KfSelfServiceResponse;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanRequest;
import com.corelogic.pbs.poc.jenkinsmcpserver.model.VeracodeScanResponse;
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

    @GetMapping("/build/latest")
    public ResponseEntity<JenkinsBuildVersionDetails> getLatestBuildDetailsByJobAndBranch(
            @RequestParam String job,
            @RequestParam String branch) {

        log.info("Received request for build version - job: {} and branch: {}", job, branch);

        JenkinsBuildVersionDetails buildVersion = jenkinsService.getLatestBuildDetailsByJobAndBranch(job, branch);

        return ResponseEntity.ok(buildVersion);
    }

    @PostMapping("/deploy")
    public ResponseEntity<DeploymentResponse> deployApplication(@RequestBody DeploymentRequest request) {
        log.info("Received deployment request for repo: {}", request.getGithubRepoName());

        DeploymentResponse deploymentResponse = jenkinsService.deployApplication(request);

        return ResponseEntity.ok(deploymentResponse);
    }

    @PostMapping("/build")
    public ResponseEntity<BuildResponse> buildApplication(
            @RequestParam String job,
            @RequestParam String branch) {

        log.info("Received build request for job: {} and branch: {}", job, branch);

        BuildResponse response = jenkinsService.buildApplication(job, branch);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/kf-self-service")
    public ResponseEntity<KfSelfServiceResponse> buildKfSelfService(@RequestBody KfSelfServiceRequest request) {
        log.info("Received KF self-service request for environment: {}, command: {}",
                request.getEnvironment(), request.getKfCommand());

        KfSelfServiceResponse response = jenkinsService.buildKfSelfService(request);

        return ResponseEntity.ok(response);
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

    @PostMapping("/veracode-scan")
    public ResponseEntity<VeracodeScanResponse> runVeracodeScan(@RequestBody VeracodeScanRequest request) {
        log.info("Received Veracode scan request for application: {}, version: {}",
                request.getJobName(), request.getVersion());

        VeracodeScanResponse response = jenkinsService.runVeracodeScan(request);

        return ResponseEntity.ok(response);
    }
}

