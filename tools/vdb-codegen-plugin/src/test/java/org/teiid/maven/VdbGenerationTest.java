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
package org.teiid.maven;

import static org.junit.Assert.assertEquals;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

public class VdbGenerationTest {

    @Test(expected=MojoExecutionException.class)
    public void testWithSpaces() throws Exception {
        VdbCodeGeneratorMojo.removeCamelCase("sample name");
    }

    @Test(expected=MojoExecutionException.class)
    public void testWithDot() throws Exception {
        VdbCodeGeneratorMojo.removeCamelCase("sample.name");
    }

    @Test
    public void testWithCamelCase() throws Exception {
        assertEquals("sample-name", VdbCodeGeneratorMojo.removeCamelCase("sampleName"));
        assertEquals("sample-name", VdbCodeGeneratorMojo.removeCamelCase("SampleName"));
        assertEquals("samplename", VdbCodeGeneratorMojo.removeCamelCase("SAMPLEName"));
        assertEquals("sample-name", VdbCodeGeneratorMojo.removeCamelCase("sample-name"));
        assertEquals("sample-name", VdbCodeGeneratorMojo.removeCamelCase("sampleNAme"));
        assertEquals("sample-name", VdbCodeGeneratorMojo.removeCamelCase("sampleNAme"));
        assertEquals("samplename", VdbCodeGeneratorMojo.removeCamelCase("samplename"));
    }
}
