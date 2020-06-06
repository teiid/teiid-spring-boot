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

public class VdbCRTest {

    @Test
    public void test() throws Exception {

        File outputDirectory = new File("target/generated-sources/teiid-sb/");
        assertNotNull( outputDirectory );
        assertTrue( outputDirectory.exists() );

        File dsFile = new File(outputDirectory, "pom.xml");
        assertTrue( dsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/pom.txt"), "utf-8").trim(),
                FileUtils.readFileToString(dsFile, "utf-8").trim());

        File propsFile = new File(outputDirectory, "src/main/resources/application.properties");
        assertTrue( propsFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/application_properties.txt"), "utf-8").trim(),
                FileUtils.readFileToString(propsFile, "utf-8").trim());

        File vdbFile = new File(outputDirectory, "src/main/resources/teiid.ddl");
        assertTrue( vdbFile.exists() );
        assertEquals("The files differ!",
                FileUtils.readFileToString(new File( "target/test-classes/teiid_ddl.txt"), "utf-8").trim(),
                FileUtils.readFileToString(vdbFile, "utf-8").trim());
    }
}
