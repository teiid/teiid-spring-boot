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
package org.teiid.spring.data.ftp;

import java.io.InputStream;

import org.teiid.file.VirtualFileConnection;
import org.teiid.spring.data.BaseConnection;
import org.teiid.translator.TranslatorException;

public class FtpConnection extends BaseConnection implements VirtualFileConnection {
    private org.teiid.file.ftp.FtpFileConnection delegate;

    public FtpConnection(org.teiid.file.ftp.FtpFileConnection conn) {
        this.delegate = conn;
    }

    @Override
    public void close() throws Exception {
        this.delegate.close();
    }

    @Override
    public org.teiid.file.VirtualFile[] getFiles(String namePattern) throws TranslatorException {
        return delegate.getFiles(namePattern);
    }

    @Override
    public void add(InputStream in, String path) throws TranslatorException {
        this.delegate.add(in, path);
    }

    @Override
    public boolean remove(String path) throws TranslatorException {
        return this.delegate.remove(path);
    }
}
