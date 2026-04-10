package com.corelogic.pbs.poc.jenkinsmcpserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class representing a child job with its name and description.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonJobInfo {
    /**
     * Name of the child job.
     */
    private String name;
    
    /**
     * Description of what this job does or manages.
     */
    private String description;
}

