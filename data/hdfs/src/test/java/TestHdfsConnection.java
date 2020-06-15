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

import org.apache.hadoop.conf.Configuration;
import org.junit.*;
import org.teiid.file.VirtualFile;
import org.teiid.spring.data.hdfs.HdfsConfiguration;
import org.teiid.spring.data.hdfs.HdfsConnection;
import org.teiid.spring.data.hdfs.HdfsConnectionFactory;
import org.teiid.translator.TranslatorException;

import java.io.*;

@Ignore
public class TestHdfsConnection {

    private static HdfsConnection hdfsConnection;
    private static HdfsConnectionFactory hdfsConnectionFactory;
    private static HdfsConfiguration hdfsConfiguration;

    @BeforeClass
    public static void SetUp() throws Exception {
        Configuration configuration = new Configuration();
        try {
            System.setProperty("hadoop.home.dir", "/");
            String hdfsUri = "hdfs://localhost:9000";
            hdfsConfiguration = new HdfsConfiguration();
            hdfsConfiguration.setFsUri(hdfsUri);
            hdfsConnectionFactory = new HdfsConnectionFactory(hdfsConfiguration);
            try {
                hdfsConnection = hdfsConnectionFactory.getConnection();
            } catch (Exception e) {
                throw new Exception(e);
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    @Test
    public void testAdd() throws FileNotFoundException, TranslatorException {
        File file = new File("sampleeg.txt");
        try {
            InputStream inputStream = new FileInputStream(file);
            hdfsConnection.add(inputStream, "main/sampleeg.txt");
            VirtualFile[] virtualFiles = hdfsConnection.getFiles("main/sampleeg.txt");
            Assert.assertEquals("The test fails", "sampleeg.txt", virtualFiles[0].getName());
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("File is not found");
        } catch (TranslatorException e) {
            throw new TranslatorException(e);
        }
    }

    @Test
    public void testDeleteFile() throws TranslatorException {
        try {
            Assert.assertTrue(hdfsConnection.remove("main/sample.txt"));
        } catch (TranslatorException e) {
            throw new TranslatorException(e);
        }
    }

    @Test
    public void testgetAllFiles() throws TranslatorException {
        try {
            VirtualFile[] virtualFiles = hdfsConnection.getFiles("/user/aditya/main/");
            for(int i = 0; i < virtualFiles.length; i++)
                System.out.println(virtualFiles[i].getName());
            Assert.assertTrue(true);
        } catch (TranslatorException e) {
            throw new TranslatorException(e);
        }
    }

    @Test
    public void testBlobSearch() throws TranslatorException {
        try {
            VirtualFile[] virtualFiles = hdfsConnection.getFiles("/user/aditya/main/sam?le");
            for(int i = 0; i < virtualFiles.length; i++)
                System.out.println(virtualFiles[i].getName());
            Assert.assertTrue(true);
        } catch (TranslatorException e) {
            throw new TranslatorException(e);
        }
    }

    @AfterClass
    public static void teardown() throws Exception{
        try {
            hdfsConnection.close();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
}
