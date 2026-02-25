package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JenkinsCrumb {
    @JsonProperty("_class")
    private String clazz;

    private String crumb;

    private String crumbRequestField;
}

