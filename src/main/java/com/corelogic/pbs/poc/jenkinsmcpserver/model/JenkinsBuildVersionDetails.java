package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JenkinsBuildVersionDetails {
    private Boolean building;
    private String description;
    private String displayName;
    private Integer number;
    private String result;
    private Long timestamp;
    private Boolean inProgress;
    private BuildReference nextBuild;
    private BuildReference previousBuild;

    @JsonGetter
    public LocalDateTime getTimestampPST() {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.of("America/Los_Angeles")
        );
    }

    @JsonGetter
    public String getBuildVersion() {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }

        // Pattern: #37-v1.0.214-6b442130 OR #130-v1.0.0-SNAPSHOT-03aa178c
        // Extract everything between first dash and last dash (skip build# and commit hash)
        String cleanName = displayName.trim();
        int firstDash = cleanName.indexOf('-');
        int lastDash = cleanName.lastIndexOf('-');

        if (firstDash != -1 && lastDash != -1 && firstDash < lastDash) {
            return cleanName.substring(firstDash + 1, lastDash);
        }

        return null;
    }
}

