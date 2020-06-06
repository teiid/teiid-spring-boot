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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.teiid.file.VirtualFile;
import org.teiid.file.VirtualFileConnection;
import org.teiid.translator.TranslatorException;
import sun.nio.ch.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

public class HdfsConnection implements VirtualFileConnection {

    private final FileSystem fileSystem;

    public HdfsConnection(HdfsConfiguration config) throws TranslatorException {
        this.fileSystem = createFileSystem(config.getFsUri());
    }

    private FileSystem createFileSystem(String fsUri) throws TranslatorException {
        Configuration configuration = new Configuration();
        try {
            return FileSystem.get(new URI(fsUri), configuration);
        } catch (IOException e) {
            throw new TranslatorException(e);
        } catch (URISyntaxException e) {
            throw new TranslatorException(e);
        }
    }

    @Override
    public VirtualFile[] getFiles(String location) throws TranslatorException {
        Path path = new Path(location);
        try {
            FileStatus fileStatus = fileSystem.getFileStatus(path);
            if(fileStatus.isDirectory()){
               return convert(path);
            }
            if(fileStatus.isFile()) return new VirtualFile[] {new HdfsVirtualFIle(fileSystem, fileStatus)};
        } catch (IOException e) {
            throw new TranslatorException(e);
        }
        if(location.contains("*")){
            try {
                FileStatus[] fileStatuses = fileSystem.globStatus(new Path(location));
                VirtualFile[] virtualFiles = new VirtualFile[fileStatuses.length];
                for(int i = 0; i < fileStatuses.length; i++){
                    virtualFiles[i] = new HdfsVirtualFIle(fileSystem, fileStatuses[i]);
                }
                return virtualFiles;
            } catch (IOException e) {
                throw new TranslatorException(e);
            }
        }
        return null;
    }

    private VirtualFile[] convert(Path path) throws IOException {
        RemoteIterator<LocatedFileStatus> fileStatusRemoteIterator = fileSystem.listFiles(path,false);
        Vector<VirtualFile> virtualFiles = new Vector<>();
        while(fileStatusRemoteIterator.hasNext()){
            virtualFiles.add(new HdfsVirtualFIle(fileSystem, fileStatusRemoteIterator.next()));
        }
        return (VirtualFile[]) virtualFiles.toArray();
    }

    @Override
    public void add(InputStream inputStream, String s) throws TranslatorException {
        try {
            OutputStream out = fileSystem.create(new Path(s));
            IOUtils.copyBytes(inputStream, out, 131072);
        } catch (IOException e) {
            throw new TranslatorException(e);
        }
    }

    @Override
    public boolean remove(String s) throws TranslatorException {
        try {
            return fileSystem.delete(new Path(s), true);
        } catch (IOException e) {
            throw new TranslatorException(e);
        }
    }

    @Override
    public void close() throws Exception {
        fileSystem.close();
    }

}
