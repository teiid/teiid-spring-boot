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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.teiid.file.VirtualFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class S3VirtualFile implements VirtualFile {

    AmazonS3 s3Client;
    S3ObjectSummary summary;

    public S3VirtualFile(AmazonS3 s3Client, S3ObjectSummary s3ObjectSummary) {
        this.s3Client = s3Client;
        this.summary = s3ObjectSummary;
    }

    @Override
    public String getName() {
        return summary.getKey();
    }

    @Override
    public InputStream openInputStream(boolean b) throws IOException {
        S3Object object = s3Client.getObject(summary.getBucketName(), summary.getKey());
        return object.getObjectContent();
    }

    @Override
    public OutputStream openOutputStream(boolean b) throws IOException {
        throw new IOException("Output stream is not supported for use in s3.");
    }

    @Override
    public long getLastModified() {
        return summary.getLastModified().getTime();
    }

    @Override
    public long getCreationTime() {
        // no mechanism for creation time provided
        return summary.getLastModified().getTime();
    }

    @Override
    public long getSize() {
        return summary.getSize();
    }
}
