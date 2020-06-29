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

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.*;
import org.mockito.Mockito;
import org.teiid.file.VirtualFile;
import org.teiid.translator.TranslatorException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TestS3Connection {

    private  S3Connection s3Connection;

    private  S3Configuration s3Configuration = Mockito.mock(S3Configuration.class);

    private AmazonS3Client amazonS3Client = Mockito.mock(AmazonS3Client.class);

    @Before
    public  void setUp() {
        s3Connection = new S3Connection(s3Configuration, amazonS3Client);

    }

    @Test
    public void testAdd() throws SdkClientException, TranslatorException {
          InputStream inputStream = Mockito.mock(InputStream.class);
          s3Connection.add(inputStream, "");
          Assert.assertTrue(true);
    }

    @Test
    public void testDeleteFile() throws TranslatorException {
        Assert.assertTrue(s3Connection.remove("sl"));
    }

    @Test
    public void testgetAllFiles() throws TranslatorException {
        ListObjectsRequest request = Mockito.mock(ListObjectsRequest.class);
        ObjectListing objectListing = Mockito.mock(ObjectListing.class);
        Mockito.when(amazonS3Client.listObjects(request)).thenReturn(objectListing);
        List<S3ObjectSummary> objectSummaryList = new ArrayList<>();
        S3ObjectSummary summary1 = Mockito.mock(S3ObjectSummary.class);
        objectSummaryList.add(summary1);
        Mockito.when(objectListing.getObjectSummaries()).thenReturn(objectSummaryList);
        Mockito.when(objectSummaryList.size()).thenReturn(3);
        VirtualFile[] virtualFiles = s3Connection.getFiles("folder1/folder2");
        Assert.assertEquals(3, virtualFiles.length);
    }

    @Test
    public void testConvert() throws TranslatorException {

    }

    @Test
    public void testMatchString() throws TranslatorException {

    }
}
