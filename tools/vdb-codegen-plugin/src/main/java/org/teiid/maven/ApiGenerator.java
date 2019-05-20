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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.maven.plugin.logging.Log;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import org.teiid.core.types.DataTypeManager;
import org.teiid.metadata.Column;
import org.teiid.metadata.ColumnSet;
import org.teiid.metadata.Database;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;

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

        Swagger swagger = null;
        if (this.openApiFile.exists()) {
            log.info("OpenApi definition file found: " + this.openApiFile.getCanonicalPath());
            SwaggerParser parser = new SwaggerParser();
            swagger =  parser.read(this.openApiFile.getAbsolutePath(), null, true);

        } else {
            log.warn("No OpenApi definition file configured. Return types may be be defined in user friendly way");
        }

        // create swagger config file.
        if (swagger == null) {
            createSwaggerConfig(mf, javaSrcDir, database, parentMap);
        }

        // create api
        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            HashMap<String, String> replacementMap = new HashMap<String, String>(parentMap);
            String schemaName = schema.getName();
            String modelName = schemaName.substring(0, 1).toUpperCase() + schemaName.substring(1);

            replacementMap.put("schemaName", schemaName);
            replacementMap.put("modelName", modelName);

            // try building the custom api
            CustomApiGenerator custom = new CustomApiGenerator(this.log);
            custom.generate(mf, javaSrcDir, replacementMap, schema);

            // try swagger based building now
            if (swagger != null) {
                //OpenApiGenerator openApi = new OpenApiGenerator(this.log);
                //openApi.generate(swagger, cfg, javaSrcDir, replacementMap, schema);
            }
        }

        // generate the classes
        if (swagger != null) {
            generateModels(this.openApiFile, parentMap.get("packageName"), outputDirectory.getAbsolutePath());
        }
    }

    private void createSwaggerConfig(MustacheFactory mf, File javaSrcDir, Database database,
            HashMap<String, String> props) throws Exception {
        Mustache mustache = mf.compile(
                new InputStreamReader(getClass().getResourceAsStream("/templates/SwaggerConfig.mustache")), "config");
        Writer out = new FileWriter(new File(javaSrcDir, "SwaggerConfig.java"));
        mustache.execute(out, props);
        out.close();
    }

    static HashSet<String> getPathParameters(String uri ) {
        HashSet<String> pathParams = new HashSet<String>();
        String param;
        if (uri.contains("{")) {
            while (uri.indexOf("}") > -1) {
                int start = uri.indexOf("{");
                int end = uri.indexOf("}");
                param = uri.substring(start + 1, end);
                uri = uri.substring(end + 1);
                pathParams.add(param);
            }
        }
        return pathParams;
    }

    static String findContentType(Procedure procedure) {
        String contentType = "plain";
        ColumnSet<Procedure> rs = procedure.getResultSet();
        if (rs != null) {
            Column returnColumn = rs.getColumns().get(0);
            String type = returnColumn.getDatatype().getRuntimeTypeName();
            if (type.equals(DataTypeManager.DefaultDataTypes.XML)) {
                contentType = "xml"; //$NON-NLS-1$
            }
            else if (type.equals(DataTypeManager.DefaultDataTypes.CLOB)
                    || type.equals(DataTypeManager.DefaultDataTypes.JSON)) {
                contentType = "json";
            }
        }
        else {
            for (ProcedureParameter pp:procedure.getParameters()) {
                if (pp.getType().equals(ProcedureParameter.Type.ReturnValue)) {
                    String type = pp.getDatatype().getRuntimeTypeName();
                    if (type.equals(DataTypeManager.DefaultDataTypes.XML)) {
                        contentType = "xml"; //$NON-NLS-1$
                    }
                    else if (type.equals(DataTypeManager.DefaultDataTypes.CLOB)
                            || type.equals(DataTypeManager.DefaultDataTypes.JSON)) {
                        contentType = "json"; //$NON-NLS-1$
                    }
                }
            }
        }
        contentType = contentType.toLowerCase().trim();
        if (contentType.equals("xml")) {
            contentType = "application/xml";
        } else if (contentType.equals("json")) {
            contentType = "application/json;charset=UTF-8";
        } else if (contentType.equals("plain")) {
            contentType = "text/plain";
        }
        return contentType;
    }

    private void generateModels(File file, String packageName, String outputDir) {
        CodegenConfigurator configurator = new CodegenConfigurator();
        configurator.setPackageName(packageName);
        configurator.setApiPackage(packageName);
        configurator.addDynamicProperty("configPackage", packageName);
        configurator.addDynamicProperty("basePackage", packageName);
        configurator.setModelPackage(packageName);

        //configurator.addSystemProperty("models", "");
        //configurator.addSystemProperty("modelDocs", "false");
        //configurator.addSystemProperty("modelTests", "false");

        configurator.setInputSpec(file.getAbsolutePath());

        configurator.setGeneratorName("org.teiid.maven.TeiidCodegen");
        configurator.setOutputDir(outputDir);
        configurator.setLibrary("spring-boot");
        configurator.addDynamicProperty("delegatePattern", "true");
        configurator.setIgnoreFileOverride(null);

        final ClientOptInput input = configurator.toClientOptInput();
        new DefaultGenerator().opts(input).generate();
    }
}
