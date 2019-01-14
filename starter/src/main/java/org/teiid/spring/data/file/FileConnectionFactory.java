/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.teiid.spring.data.file;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.teiid.core.util.StringUtil;
import org.teiid.spring.data.BaseConnectionFactory;

@ConfigurationProperties(prefix="spring.teiid.file")
public class FileConnectionFactory extends BaseConnectionFactory<FileConnection> {
    private String parentDirectory;
    private String fileMapping;
    private boolean allowParentPaths = true;

    public FileConnectionFactory() {
        super.setTranslatorName("file");
    }

    @Override
    public FileConnection getConnection() throws Exception {
        if (this.parentDirectory == null) {
            this.parentDirectory = System.getProperty("user.dir");
        }
        final Map<String, String> map = StringUtil.valueOf(this.fileMapping, Map.class);

        return new FileConnection(parentDirectory, map, allowParentPaths);
    }

    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(String parentDirectory) {
        this.parentDirectory = parentDirectory;
    }

    public String getFileMapping() {
        return fileMapping;
    }

    public void setFileMapping(String fileMapping) {
        this.fileMapping = fileMapping;
    }

    public Boolean isAllowParentPaths() {
        return allowParentPaths;
    }

    public void setAllowParentPaths(Boolean allowParentPaths) {
        this.allowParentPaths = allowParentPaths != null && allowParentPaths;
    }
}
