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
package org.teiid.spring.data.s3;

public class S3Configuration implements org.teiid.s3.S3Configuration {

    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "US-EAST-1";
    private String sseAlgorithm = "AES256";
    private String sseKey = null;   // base64-encoded key
    private String endpoint = "https://s3.amazonaws.com";

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getSseAlgorithm() {
        return sseAlgorithm;
    }

    public void setSseAlgorithm(String sseAlgorithm) {
        this.sseAlgorithm = sseAlgorithm;
    }

    public String getSseKey() {
        return sseKey;
    }

    public void setSseKey(String sseKey) {
        this.sseKey = sseKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
