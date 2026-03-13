package com.corelogic.pbs.poc.jenkinsmcpserver.model;
import lombok.Data;
@Data
public class VeracodeScanRequest {
    private String version;
    private String jobName;
    private String scanType;
    private String excludePattern;
    private String includePattern;
}