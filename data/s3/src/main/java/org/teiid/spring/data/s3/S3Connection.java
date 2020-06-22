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
import org.teiid.file.VirtualFile;
import org.teiid.file.VirtualFileConnection;
import org.teiid.translator.TranslatorException;

import java.io.InputStream;
import java.util.Vector;

public class S3Connection implements VirtualFileConnection {


    private final S3Configuration s3Config;
    private final AmazonS3 s3Client;

    public S3Connection(S3Configuration s3Config, AmazonS3 s3Client) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
    }

    @Override
    public VirtualFile[] getFiles(String s) throws TranslatorException {
        if(s3Client.doesObjectExist(s3Config.getBucket(), s)){
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(s3Config.getBucket())
                    .withPrefix(s);
            ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            return new VirtualFile[] {new S3VirtualFile(s3Client, objectListing.getObjectSummaries().get(0))};
        }
        if(isDirectory(s)){
            if(!s.endsWith("/")) {
                s += "/";
            }
            return convert(s);
        }
        String parentPath;
        if(s.contains("/")) {
            parentPath = s.substring(0, s.lastIndexOf('/'));
        }
        else {
            parentPath = "";
        }

        if(!isDirectory(parentPath))
            return null;
        if(s.contains("*"))
        {
            return globSearch(parentPath, s);
        }
        return null;
    }

    private VirtualFile[] globSearch(String parentPath, String s) {
        Vector<S3VirtualFile> s3VirtualFiles = new Vector<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3Config.getBucket())
                .withPrefix(parentPath)
                .withDelimiter("/");
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        while(objectListing != null) {
            for(S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                if(!s3ObjectSummary.getKey().endsWith("/")) {
                    if(matchString(s3ObjectSummary.getKey(), s)) {
                        s3VirtualFiles.add(new S3VirtualFile(s3Client, s3ObjectSummary));
                    }
                }
            }
            if(!objectListing.isTruncated())
                break;
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }
        VirtualFile[] virtualFiles = new VirtualFile[s3VirtualFiles.size()];
        for(int i = 0; i < s3VirtualFiles.size(); i++) {
            virtualFiles[i] = s3VirtualFiles.get(i);
        }
        return virtualFiles;
    }

    private boolean matchString(String key, String pattern) {
        String slicedKey, slicedPattern;
        if(key.contains("/")) {
            slicedKey = key.substring(key.lastIndexOf("/") + 1);
        }
        else {
            slicedKey = key;
        }
        if(pattern.contains("/")) {
            slicedPattern = pattern.substring(pattern.lastIndexOf("/"));
        }
        else {
            slicedPattern = pattern;
        }
        if(slicedPattern.endsWith("*")) {
            if(slicedKey.contains(slicedPattern.substring(0, slicedPattern.length() - 1))) {
                return true;
            }
            return false;
        }
        else if (slicedPattern.startsWith("*")) {
            if(slicedKey.contains(slicedPattern.substring(1))) {
                return true;
            }
            return false;
        }
        else {
            String begin, end;
            int index0fAsterisk = slicedPattern.indexOf("*");
            begin = slicedPattern.substring(0, index0fAsterisk);
            end = slicedPattern.substring(index0fAsterisk+1);
            if(key.contains(begin) && key.contains(end) && (slicedPattern.indexOf(end) > slicedPattern.indexOf(begin)+begin.length() - 1)) {
                return true;
            }
            return false;
        }
    }

    private VirtualFile[] convert(String s) {
        Vector<S3VirtualFile> s3VirtualFiles = new Vector<>();
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(s3Config.getBucket())
                .withPrefix(s)
                .withDelimiter("/");
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
        while(objectListing != null) {
            for(S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                if(!s3ObjectSummary.getKey().endsWith("/")) {
                    s3VirtualFiles.add(new S3VirtualFile(s3Client,s3ObjectSummary));
                }
            }
            if(!objectListing.isTruncated())
                break;
            objectListing = s3Client.listNextBatchOfObjects(objectListing);
        }
        VirtualFile[] virtualFiles = new VirtualFile[s3VirtualFiles.size()];
        for(int i = 0; i < s3VirtualFiles.size(); i++) {
            virtualFiles[i] = s3VirtualFiles.get(i);
        }
        return virtualFiles;
    }

    private boolean isDirectory(String s) {
        if(!s.endsWith("/"))
            s += "/";
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
            s3Client.putObject(s3Config.getBucket(), s, inputStream, metadata);
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
