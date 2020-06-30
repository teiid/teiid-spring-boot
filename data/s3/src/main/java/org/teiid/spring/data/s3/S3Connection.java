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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSECustomerKey;
import org.teiid.file.VirtualFile;
import org.teiid.file.VirtualFileConnection;
import org.teiid.translator.TranslatorException;

import java.io.InputStream;
import java.util.ArrayList;

public class S3Connection implements VirtualFileConnection {


    private final S3Configuration s3Config;
    private final AmazonS3 s3Client;

    public S3Connection(S3Configuration s3Config, AmazonS3 s3Client) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
    }

    @Override
    public VirtualFile[] getFiles(String s) throws TranslatorException {
        if(s != "" && s3Client.doesObjectExist(s3Config.getBucket(), s)){
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(s3Config.getBucket())
                    .withPrefix(s);
            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            return new VirtualFile[] {new S3VirtualFile(s3Client, objectListing.getObjectSummaries().get(0), s3Config)};
        }
        if(isDirectory(s)){
            if(s == "") {
                // no change
            }
            else if(!s.endsWith("/")) {
                s += "/";
            }
            return convert(s);
        }
        String parentPath;
        if(s.contains("/")) {
            parentPath = s.substring(0, s.lastIndexOf('/') + 1);
        }
        else {
            parentPath = "";
        }

        if(!isDirectory(parentPath)) {
            return null;
        }
        if(s.contains("*"))
        {
            return globSearch(parentPath, s);
        }
        return null;
    }

    private VirtualFile[] globSearch(String parentPath, String s) {
        ArrayList<S3VirtualFile> s3VirtualFiles = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3Config.getBucket())
                .withPrefix(parentPath)
                .withDelimiter("/");
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        while(objectListing != null) {
            for(S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                if(!s3ObjectSummary.getKey().endsWith("/")) {
                    if(matchString(s3ObjectSummary.getKey(), s)) {
                        s3VirtualFiles.add(new S3VirtualFile(s3Client, s3ObjectSummary, s3Config));
                    }
                }
            }
            if(!objectListing.isTruncated()) {
                break;
            }
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }
        return s3VirtualFiles.toArray(new VirtualFile[s3VirtualFiles.size()]);
    }

    protected boolean matchString(String key, String pattern) {
        String slicedKey, slicedPattern;
        if(key.contains("/")) {
            slicedKey = key.substring(key.lastIndexOf("/") + 1);
        }
        else {
            slicedKey = key;
        }
        if(pattern.contains("/")) {
            slicedPattern = pattern.substring(pattern.lastIndexOf("/") + 1);
        }
        else {
            slicedPattern = pattern;
        }
        if(slicedPattern.endsWith("*")) {
            if(slicedKey.startsWith(slicedPattern.substring(0, slicedPattern.length() - 1))) {
                return true;
            }
            return false;
        }
        else if (slicedPattern.startsWith("*")) {
            if(slicedKey.endsWith(slicedPattern.substring(1))) {
                return true;
            }
            return false;
        }
        else {
            String begin, end;
            int index0fAsterisk = slicedPattern.indexOf("*");
            begin = slicedPattern.substring(0, index0fAsterisk);
            end = slicedPattern.substring(index0fAsterisk+1);
            if(slicedKey.startsWith(begin) && slicedKey.endsWith(end) && (slicedPattern.lastIndexOf(end) > slicedPattern.indexOf(begin)+begin.length() - 1)) {
                return true;
            }
            return false;
        }
    }

    private VirtualFile[] convert(String s) {
        ArrayList<S3VirtualFile> s3VirtualFiles = new ArrayList<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3Config.getBucket())
                .withPrefix(s)
                .withDelimiter("/");
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        while(objectListing != null) {
            for(S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                if(!s3ObjectSummary.getKey().endsWith("/")) {
                    s3VirtualFiles.add(new S3VirtualFile(s3Client,s3ObjectSummary, s3Config));
                }
            }
            if(!objectListing.isTruncated()) {
                break;
            }
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }
        return s3VirtualFiles.toArray(new VirtualFile[s3VirtualFiles.size()]);
    }

    private boolean isDirectory(String s) {
        if(!s.endsWith("/")) {
            s += "/";
        }
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3Config.getBucket())
                .withPrefix(s);
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        return objectListing.getObjectSummaries().size() > 0;
    }

    @Override
    public void add(InputStream inputStream, String s) throws TranslatorException {
        ObjectMetadata metadata = new ObjectMetadata();
        try{
            PutObjectRequest request = new PutObjectRequest(s3Config.getBucket(), s, inputStream, metadata);
            if(s3Config.getSseKey() != null) {
                request.withSSECustomerKey(new SSECustomerKey(s3Config.getSseKey()).withAlgorithm(s3Config.getSseAlgorithm()));
            }
            s3Client.putObject(request);
        } catch (SdkClientException e){
            throw new TranslatorException(e);
        }
    }

    @Override
    public boolean remove(String s) throws TranslatorException {
        try{
            s3Client.deleteObject(s3Config.getBucket(), s);
        } catch (SdkClientException e){
            return false;
        }
        return true;
    }

    @Override
    public void close() throws Exception {

    }
}
