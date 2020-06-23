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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.teiid.file.VirtualFile;
import org.teiid.hdfs.HdfsConnection;
import org.teiid.translator.TranslatorException;

@Ignore
public class TestHdfsConnection {

    private static HdfsConnection hdfsConnection;
    private static HdfsConnectionFactory hdfsConnectionFactory;
    private static HdfsConfiguration hdfsConfiguration;

    @BeforeClass
    public static void setUp() throws Exception {
        Configuration configuration = new Configuration();
        System.setProperty("hadoop.home.dir", "/");
        String hdfsUri = "hdfs://localhost:9000";
        hdfsConfiguration = new HdfsConfiguration();
        hdfsConfiguration.setFsUri(hdfsUri);
        hdfsConnectionFactory = new HdfsConnectionFactory(hdfsConfiguration);
        hdfsConnection = hdfsConnectionFactory.getConnection();
    }

    @Test
    public void testAdd() throws FileNotFoundException, TranslatorException {
        File file = new File("src/main/resources/hello");
        InputStream inputStream = new FileInputStream(file);
        hdfsConnection.add(inputStream, "folder1/hello");
        VirtualFile[] virtualFiles = hdfsConnection.getFiles("folder1/hello");
        Assert.assertEquals("The test fails", "hello", virtualFiles[0].getName());
    }

    @Test
    public void testDeleteFile() throws TranslatorException {
        Assert.assertTrue(hdfsConnection.remove("main/hello"));
    }

    @Test
    public void testgetAllFiles() throws TranslatorException {
        VirtualFile[] virtualFiles = hdfsConnection.getFiles("/user/aditya/");
        for(int i = 0; i < virtualFiles.length; i++)
            System.out.println(virtualFiles[i].getName());
    }

    @Test
    public void testBlobSearch() throws TranslatorException {
        VirtualFile[] virtualFiles = hdfsConnection.getFiles("/user/aditya/folder1/*");
        for(int i = 0; i < virtualFiles.length; i++)
            System.out.println(virtualFiles[i].getName());
    }

    @AfterClass
    public static void teardown() throws Exception{
        hdfsConnection.close();
    }
}
