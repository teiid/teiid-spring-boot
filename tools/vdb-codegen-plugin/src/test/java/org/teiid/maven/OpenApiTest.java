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
package org.teiid.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class OpenApiTest {

    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before() throws Throwable
        {
        }

        @Override
        protected void after()
        {
        }
    };

    @Test
    public void test() throws Exception {
        File pom = new File( "target/test-classes/test-openapi/" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        VdbCodeGeneratorMojo myMojo = ( VdbCodeGeneratorMojo ) rule.lookupConfiguredMojo( pom, "vdb-codegen" );
        assertNotNull( myMojo );
        myMojo.execute();

        File out = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
        File outputDirectory = new File(out, "src/main/java");
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );


        testDataSourceGeneration(outputDirectory);

        testDelegate(outputDirectory);
    }

    public void testDataSourceGeneration(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "com/example/DataSourcessampledb.java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/datasource.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }

    public void testDelegate(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "com/example/PetApiDelegate.java");
        assertTrue(dsFile.exists());
        System.out.println(FileUtils.readFileToString(dsFile, "utf-8").trim());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApiDelegate.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());


        dsFile = new File(outputDirectory, "com/example/PetApiController.java");
        assertTrue(dsFile.exists());
        System.out.println(FileUtils.readFileToString(dsFile, "utf-8").trim());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApiController.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());

        dsFile = new File(outputDirectory, "com/example/PetApi.java");
        assertTrue(dsFile.exists());
        System.out.println(FileUtils.readFileToString(dsFile, "utf-8").trim());

        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/PetApi.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }
}
