package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResponse {
    private String message;
    private String deploymentUrl;
}


