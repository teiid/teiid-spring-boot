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

package org.teiid.spring.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.teiid.common.buffer.AutoCleanupUtil;
import org.teiid.common.buffer.AutoCleanupUtil.Removable;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.query.metadata.NioVirtualFile;

public class NioZipFileSystem {

    /**
     * Get the root {@link VirtualFile} for the given url.
     * Any previous filesystem for this url will be closed.
     * The returned file will be used to auto close the filesystem.
     * It should be strongly held until the filesystem is no longer needed.
     *
     * @param url
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static NioVirtualFile mount(InputStream is) throws IOException, URISyntaxException {
        synchronized (NioZipFileSystem.class) {
            File f = File.createTempFile("temp", null);
            ObjectConverterUtil.write(is, f);

            Map<String, String> env = new HashMap<>();
            env.put("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            FileSystem zipfs = FileSystems.newFileSystem(f.toPath(), NioVirtualFile.class.getClassLoader());
            Path root = zipfs.getPath("/"); //$NON-NLS-1$
            AutoCleanupUtil.setCleanupReference(root, new Removable() {

                @Override
                public void remove() {
                    try {
                        zipfs.close();
                    } catch (IOException e) {
                    }
                }
            });
            return new NioVirtualFile(root);
        }
    }
}
