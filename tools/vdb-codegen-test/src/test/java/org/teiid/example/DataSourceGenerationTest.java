/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class DataSourceGenerationTest {

    @Test
    public void testDataSouceGeneration() throws Exception {
        File outputDirectory = new File("target/generated-sources/teiid-sb/src/main/java/org/teiid/spring/example");
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        testGeneration("datasource", "sampledb", outputDirectory);
        testGeneration("mongo", "samplemango", outputDirectory);
        testGeneration("salesforce", "samplesf", outputDirectory);
        testGeneration("odata4", "sampleodata", outputDirectory);
        testGeneration("file", "samplefile", outputDirectory);
        testGeneration("google", "samplegoogle", outputDirectory);
        testGeneration("infinispan", "ispn", outputDirectory);
        testGeneration("s3", "s3", outputDirectory);
        testGeneration("soap", "soapyCountry", outputDirectory);
        testGeneration("ftp", "sampleftp", outputDirectory);
        testGeneration("cassandra", "samplecassandra", outputDirectory);
    }

    public void testGeneration(String expected, String name, File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "DataSources"+name+".java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/"+expected+".txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }
}
