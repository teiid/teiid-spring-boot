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

import org.junit.*;
import org.teiid.file.VirtualFile;
import org.teiid.translator.TranslatorException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@Ignore
public class TestS3Connection {

    private static S3Connection s3Connection;
    private static S3ConnectionFactory s3ConnectionFactory;
    private static S3Configuration s3Configuration;

    @BeforeClass
    public static void setUp() throws Exception {
        s3Configuration = new S3Configuration();
        s3Configuration.setAwsAccessKey("minioadmin");
        s3Configuration.setAwsSecretKey("minioadmin");
        s3Configuration.setBucket("test");
        s3Configuration.setEndpoint("http://192.168.1.9:9000");
        s3ConnectionFactory = new S3ConnectionFactory(s3Configuration);
        s3Connection = s3ConnectionFactory.getConnection();
    }

    @Test
    public void testAdd() throws FileNotFoundException, TranslatorException {
        File file = new File("src/main/resources/sample2.txt");
        InputStream inputStream = new FileInputStream(file);
        s3Connection.add(inputStream, "folder1/folder2/sample2.txt");
        VirtualFile[] virtualFiles = s3Connection.getFiles("folder1/folder2/sample2.txt");
        Assert.assertEquals("The test fails", "folder1/folder2/sample2.txt", virtualFiles[0].getName());
    }

    @Test
    public void testDeleteFile() throws TranslatorException {
        Assert.assertTrue(s3Connection.remove("hello.txt"));
    }

    @Test
    public void testgetAllFiles() throws TranslatorException {
        VirtualFile[] virtualFiles = s3Connection.getFiles("folder1/folder2");
        Assert.assertEquals(1, virtualFiles.length);
    }

    @Test
    public void testBlobSearch() throws TranslatorException {
        VirtualFile[] virtualFiles = s3Connection.getFiles("folder1/folder2/*.txt");
        System.out.println(virtualFiles[0].getName());
        Assert.assertEquals(2, virtualFiles.length);
    }

    @AfterClass
    public static void teardown() throws Exception{
        s3Connection.close();
    }
}
