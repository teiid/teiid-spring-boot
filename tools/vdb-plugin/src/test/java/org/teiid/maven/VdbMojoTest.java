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

        VdbMojo myMojo = ( VdbMojo ) rule.lookupConfiguredMojo( pom, "vdb" );
        assertNotNull( myMojo );
        myMojo.execute();

        File out = ( File ) rule.getVariableValueFromObject( myMojo, "outputDirectory" );
        assertNotNull( out );
        assertTrue( out.exists() );

        testFileContents(out);
    }


    public void testFileContents(File outputDirectory)throws Exception {
        File dsFile = new File(outputDirectory, "vdb-code-1.0.vdb");
        assertTrue( dsFile.exists() );
        File unzippped = new File(dsFile.getParentFile(), "vdb-code");
        unzippped.mkdirs();
        ZipArchive.unzip(dsFile, unzippped);
        /*
        FileWriter fw = new FileWriter("foo.txt");
        fw.write(FileUtils.readFileToString(new File(unzippped.getAbsolutePath()+"/META-INF/vdb.ddl"), "utf-8"));
        fw.flush();
        fw.close();
         */
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/vdb-file.txt"), "utf-8").trim(),
                FileUtils.readFileToString(new File(unzippped, "META-INF/vdb.ddl"), "utf-8").trim());
    }


}
