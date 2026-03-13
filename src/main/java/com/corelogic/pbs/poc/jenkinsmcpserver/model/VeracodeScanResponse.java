package com.corelogic.pbs.poc.jenkinsmcpserver.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VeracodeScanResponse {
    private String message;
    private String scanUrl;
}