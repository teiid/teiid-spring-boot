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

public class VdbMojoTest {

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
        File pom = new File( "target/test-classes/test-project/" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        VdbCodeGeneratorMojo myMojo = ( VdbCodeGeneratorMojo ) rule.lookupConfiguredMojo( pom, "vdb-codegen" );
        assertNotNull( myMojo );
        myMojo.execute();

        File outputDirectory = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        testDataSourceGeneration(outputDirectory);
        testMongoGeneration(outputDirectory);
        testSalesforceGeneration(outputDirectory);
        testRestGeneration(outputDirectory);
        testSwaggerConfig(outputDirectory);
    }


    public void testDataSourceGeneration(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "com/example/DataSourcessampledb.java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/datasource.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }

    public void testMongoGeneration(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "com/example/DataSourcessamplemango.java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/mongo.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }

    public void testSalesforceGeneration(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "com/example/DataSourcessamplesf.java");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/salesforce.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());
    }

    public void testSwaggerConfig(File outputDirectory)throws Exception {
        File file = new File(outputDirectory, "com/example/SwaggerConfig.java");
        assertTrue( file.exists() );
        //System.out.println(FileUtils.readFileToString(file, "utf-8"));
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/swaggerconfig.txt"), "utf-8").trim(),
                FileUtils.readFileToString(file, "utf-8").trim());
    }

    public void testRestGeneration(File outputDirectory)throws Exception {
        File file = new File(outputDirectory, "com/example/portfolio.java");
        assertTrue( file.exists() );
        //System.out.println(FileUtils.readFileToString(file, "utf-8"));
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/controller.txt"), "utf-8").trim(),
                FileUtils.readFileToString(file, "utf-8").trim());
    }
}
