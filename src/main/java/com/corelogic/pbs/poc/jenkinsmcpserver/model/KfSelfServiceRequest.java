package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KfSelfServiceRequest {
    @JsonProperty("env")
    private String environment;
    @JsonProperty("cmd")
    private String kfCommand;
    @JsonProperty("params")
    private String kfCommandParameters;
}

