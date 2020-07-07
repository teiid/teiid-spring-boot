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

import org.teiid.s3.S3Connection;
import org.teiid.spring.data.BaseConnectionFactory;
import org.teiid.spring.data.ConnectionFactoryConfiguration;
import org.teiid.translator.TranslatorException;

@ConnectionFactoryConfiguration(
        alias = "s3",
        translatorName = "file"
)
public class S3ConnectionFactory extends org.teiid.s3.S3ConnectionFactory implements BaseConnectionFactory<S3Connection> {

    public S3ConnectionFactory(S3Configuration s3Config) throws TranslatorException {
        super(s3Config);
    }

    @Override
    public S3Connection getConnection() throws Exception {
        return new S3Connection(getS3Config(), getS3Client());
    }

}
