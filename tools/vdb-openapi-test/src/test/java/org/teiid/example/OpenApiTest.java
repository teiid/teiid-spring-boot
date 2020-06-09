/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

public class OpenApiTest {

    @Test
    public void test() throws Exception {
        File outputDirectory = new File("target/generated-sources/teiid-sb/src/main/java/org/teiid/spring/example");
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        testDataSourceGeneration(outputDirectory);

        testDelegate(outputDirectory);
    }

    public void testDataSourceGeneration(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "DataSourcessampledb.java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/datasource.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }

    public void testDelegate(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "PetApiDelegate.java");
        assertTrue(dsFile.exists());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApiDelegate.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());


        dsFile = new File(outputDirectory, "PetApiController.java");
        assertTrue(dsFile.exists());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApiController.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());

        dsFile = new File(outputDirectory, "PetApi.java");
        assertTrue(dsFile.exists());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApi.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }
}
