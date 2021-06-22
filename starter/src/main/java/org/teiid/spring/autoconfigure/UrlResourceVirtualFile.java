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
package org.teiid.spring.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.UrlResource;
import org.teiid.query.metadata.VirtualFile;


public class UrlResourceVirtualFile implements VirtualFile {
    private UrlResource resource;
    String name;

    public UrlResourceVirtualFile(String name, UrlResource resource) {
        this.name = name;
        this.resource = resource;
    }

    @Override
    public InputStream openStream() throws IOException {
        return resource.getInputStream();
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<VirtualFile> getFileChildrenRecursively() throws IOException {
        return null;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public String getPathName() {
        return this.name;
    }

    @Override
    public VirtualFile getChild(String string) {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }
}
