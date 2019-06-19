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

import java.io.File;
import java.util.HashMap;

import org.apache.maven.plugin.logging.Log;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.teiid.metadata.Database;

import com.github.mustachejava.MustacheFactory;

public class ApiGenerator {
    private File openApiFile;
    private File outputDirectory;
    private Log log;

    public ApiGenerator(File openApiFile, File outputDirectory, Log log) {
        this.openApiFile = openApiFile;
        this.outputDirectory = outputDirectory;
        this.log = log;
    }

    protected void generate(MustacheFactory mf, File javaSrcDir, Database database, HashMap<String, String> parentMap)
            throws Exception {
        log.info("Starting to Generate the Java classes for OpenAPI document: " + this.openApiFile.getCanonicalPath());
        String packageName = parentMap.get("packageName");
        String outputDir = this.outputDirectory.getAbsolutePath();

        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setPackageName(packageName);
        configurator.setApiPackage(packageName);
        configurator.addDynamicProperty("configPackage", packageName);
        configurator.addDynamicProperty("basePackage", packageName);
        configurator.setModelPackage(packageName);

        //configurator.addSystemProperty("models", "");
        //configurator.addSystemProperty("modelDocs", "false");
        //configurator.addSystemProperty("modelTests", "false");

        configurator.setInputSpec(this.openApiFile.getAbsolutePath());

        configurator.setGeneratorName("org.teiid.maven.TeiidCodegen");
        configurator.setOutputDir(outputDir);
        configurator.setLibrary("spring-boot");
        configurator.addDynamicProperty("delegatePattern", "true");
        configurator.setIgnoreFileOverride(null);

        final ClientOptInput input = configurator.toClientOptInput();
        new DefaultGenerator().opts(input).generate();

        log.info("Generated the Java classes for OpenAPI document: " + this.openApiFile.getCanonicalPath());
    }
}
