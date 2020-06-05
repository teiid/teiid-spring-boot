/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.data.hdfs;

import java.util.List;
import java.util.Map;

public class HdfsConfiguration {

    private String fsUri;
    private String resourceManagerAddress;
    private String resourceManagerSchedulerAddress;
    private String resourceManagerHost;
    private Integer resourceManagerPort;
    private Integer resourceManagerSchedulerPort;
    private String jobHistoryAddress;
    private List<String> resources;
    private Map<String, Map<String, Map<String, String>>> config;

    public String getFsUri() {
        return fsUri;
    }

    public void setFsUri(String fsUri) {
        this.fsUri = fsUri;
    }

    public String getResourceManagerAddress() {
        return resourceManagerAddress;
    }

    public void setResourceManagerAddress(String resourceManagerAddress) {
        this.resourceManagerAddress = resourceManagerAddress;
    }

    public String getResourceManagerSchedulerAddress() {
        return resourceManagerSchedulerAddress;
    }

    public void setResourceManagerSchedulerAddress(String resourceManagerSchedulerAddress) {
        this.resourceManagerSchedulerAddress = resourceManagerSchedulerAddress;
    }

    public String getResourceManagerHost() {
        return resourceManagerHost;
    }

    public void setResourceManagerHost(String resourceManagerHost) {
        this.resourceManagerHost = resourceManagerHost;
    }

    public Integer getResourceManagerPort() {
        return resourceManagerPort;
    }

    public void setResourceManagerPort(Integer resourceManagerPort) {
        this.resourceManagerPort = resourceManagerPort;
    }

    public Integer getResourceManagerSchedulerPort() {
        return resourceManagerSchedulerPort;
    }

    public void setResourceManagerSchedulerPort(Integer resourceManagerSchedulerPort) {
        this.resourceManagerSchedulerPort = resourceManagerSchedulerPort;
    }

    public String getJobHistoryAddress() {
        return jobHistoryAddress;
    }

    public void setJobHistoryAddress(String jobHistoryAddress) {
        this.jobHistoryAddress = jobHistoryAddress;
    }

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public Map<String, Map<String, Map<String, String>>> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Map<String, Map<String, String>>> config) {
        this.config = config;
    }
}