package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BuildReference {
    private Integer number;
}

