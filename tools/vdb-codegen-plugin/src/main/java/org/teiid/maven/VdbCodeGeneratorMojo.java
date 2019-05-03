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

import static org.teiid.deployers.RestWarGenerator.REST_NAMESPACE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.metadata.Column;
import org.teiid.metadata.ColumnSet;
import org.teiid.metadata.DataWrapper;
import org.teiid.metadata.Database;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.Procedure;
import org.teiid.metadata.ProcedureParameter;
import org.teiid.metadata.Schema;
import org.teiid.metadata.Server;
import org.teiid.query.function.SystemFunctionManager;
import org.teiid.query.metadata.DatabaseStore;
import org.teiid.query.metadata.DatabaseStore.Mode;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.query.parser.QueryParser;
import org.teiid.spring.common.ExternalSource;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.PlainTextOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "vdb-codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, requiresProject = true)
public class VdbCodeGeneratorMojo extends AbstractMojo {
    public static final SystemFunctionManager SFM = SystemMetadata.getInstance().getSystemFunctionManager();

    @Parameter(defaultValue = "${basedir}/src/main/resources/teiid-vdb.ddl")
    private File vdbFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/teiid-sb")
    private File outputDirectory;

    @Parameter
    private String packageName;

    @Parameter
    private Boolean generateApplicationClass = true;

    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try {
            Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
            cfg.setTemplateLoader(new ClassTemplateLoader(getClassLoader(), "templates"));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
            cfg.setOutputFormat(PlainTextOutputFormat.INSTANCE);

            ClassLoader pluginClassloader = getClassLoader();
            Thread.currentThread().setContextClassLoader(pluginClassloader);

            File outputDir = getOutputDirectory();
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            if (this.packageName == null) {
                this.packageName = project.getGroupId();
            }
            String codeDirectory = this.packageName.replace('.', '/');

            // where to keep source files
            File javaSrcDir = new File(getOutputDirectory(), codeDirectory);
            if (!javaSrcDir.exists()) {
                javaSrcDir.mkdirs();
            }

            File vdbfile = this.getVDBFile();
            Database database = getDatabase(vdbfile);

            HashMap<String, String> parentMap = new HashMap<String, String>();
            parentMap.put("packageName", this.packageName);
            parentMap.put("vdbName", database.getName());
            parentMap.put("vdbDescription", database.getAnnotation());
            parentMap.put("openapi", generateOpenApiScoffolding()?"true":"false");
            if (this.generateApplicationClass) {
                createApplication(cfg, javaSrcDir, database, parentMap);
            }
            createDataSources(cfg, javaSrcDir, database, parentMap);
            verifyTranslatorDependencies(database);
            if (generateOpenApiScoffolding()) {
                createOpenApi(cfg, javaSrcDir, database, parentMap);
            }
            this.project.addCompileSourceRoot(javaSrcDir.getAbsolutePath());

        } catch (Exception e) {
            throw new MojoExecutionException("Error running the vdb-codegen-plugin.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private Database getDatabase(File vdbfile) throws IOException {
        DatabaseStore store = new DatabaseStore() {
            @Override
            public Map<String, Datatype> getRuntimeTypes() {
                return SystemMetadata.getInstance().getRuntimeTypeMap();
            }
            @Override
            public void importSchema(String schemaName, String serverType, String serverName, String foreignSchemaName,
                    List<String> includeTables, List<String> excludeTables, Map<String, String> properties) {
                // ignore
            }
        };
        Database db = null;
        String vdbStr = ObjectConverterUtil.convertToString(new FileInputStream(vdbfile));
        StringReader reader = new StringReader(vdbStr);
        try {
            store.startEditing(false);
            store.setMode(Mode.ANY);
            QueryParser.getQueryParser().parseDDL(store, new BufferedReader(reader));
            db = store.getDatabases().get(0);
        } finally {
            reader.close();
            store.stopEditing();
        }
        return db;
    }

    private void createApplication(Configuration cfg, File javaSrcDir, Database database, HashMap<String, String> props)
            throws Exception {
        Template template = cfg.getTemplate("Application.java");
        Writer out = new FileWriter(new File(javaSrcDir, "Application.java"));
        template.process(props, out);
        out.close();
    }

    private void createSwaggerConfig(Configuration cfg, File javaSrcDir, Database database, HashMap<String, String> props)
            throws Exception {
        Template template = cfg.getTemplate("SwaggerConfig.java");
        Writer out = new FileWriter(new File(javaSrcDir, "SwaggerConfig.java"));
        template.process(props, out);
        out.close();
    }

    private void verifyTranslatorDependencies(Database database) throws Exception {
        for(DataWrapper dw : database.getDataWrappers()) {
            ExternalSource source = ExternalSource.find(dw.getName());
            if (source == null) {
                throw new MojoExecutionException("No Translator found with name:" + dw.getName());
            }

            boolean foundDependency = false;
            List<Dependency> dependencies = project.getDependencies();
            for (Dependency d : dependencies) {
                String gav = d.getGroupId() + ":" + d.getArtifactId();
                getLog().info("Found dependency:" + gav);
                if (source.getGav().equals(gav)) {
                    foundDependency = true;
                }
            }

            if (!foundDependency) {
                if (source.getGav() != null) {
                    throw new MojoExecutionException("Drivers for translator \"" + dw.getName()
                    + "\" are not found. Include following dependecy in pom.xml\n" + "<dependency>\n"
                    + "    <groupId>" + source.getGav().substring(0, source.getGav().indexOf(':'))
                    + "</groupId>\n" + "    <artifactId>"
                    + source.getGav().substring(source.getGav().indexOf(':') + 1) + "</artifactId>\n"
                    + "</dependency>\n");
                } else {
                    getLog().error("Drivers for translator \"" + dw.getName()
                    + "\" can not be verified. Make sure you have the required dependencies in the pom.xml");
                }
            }
        }
    }

    private void createDataSources(Configuration cfg, File javaSrcDir, Database database,
            HashMap<String, String> parentMap) throws Exception {

        for (Server server : database.getServers()) {
            HashMap<String, String> tempMap = new HashMap<String, String>(parentMap);
            tempMap.put("dsName", server.getName());

            getLog().info("Building DataSource.java for source :" + server.getName());

            // Custom data sources are expected to provide their own DataSource classes
            // when application is built
            String translator = server.getDataWrapper();
            if (translator.equals(ExternalSource.MONGODB.getTranslatorName())) {
                Template template = cfg.getTemplate("DataSources_MongoDB.java");
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                template.process(tempMap, out);
                out.close();
            } else if (translator.equals(ExternalSource.SALESFORCE.getTranslatorName())) {
                Template template = cfg.getTemplate("DataSources_Salesforce.java");
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                template.process(tempMap, out);
                out.close();
            } else if (translator.equals(ExternalSource.GOOGLESHEETS.getTranslatorName())) {
                Template template = cfg.getTemplate("DataSources_GoogleSheets.java");
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                template.process(tempMap, out);
                out.close();
            } else if (translator.equals(ExternalSource.FILE.getTranslatorName())) {
                // ignore as by default it is created
            } else if (translator.equals(ExternalSource.REST.getTranslatorName())) {
                // ignore as by default it is created
            } else {
                Template template = cfg.getTemplate("DataSources_JDBC.java");
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                template.process(tempMap, out);
                out.close();
            }
        }
    }

    private File getVDBFile() throws MojoExecutionException, IOException {
        if (this.vdbFile.exists()) {
            getLog().info("Found VDB = " + this.vdbFile);
            return this.vdbFile;
        }

        // read import vdbs from dependencies
        Set<Artifact> dependencies = project.getArtifacts();
        for (Artifact d : dependencies) {
            if (d.getFile() == null || !d.getFile().getName().endsWith(".vdb")) {
                continue;
            }
            File vdbDir = unzipContents(d);
            File childFile = new File(vdbDir, "vdb.ddl");
            return childFile;
        }

        throw new MojoExecutionException(
                "No VDB File found at location " + this.vdbFile + " or no VDB dependencies defined");
    }



    private File unzipContents(Artifact d) throws IOException {
        File f = new File(this.project.getBuild().getDirectory(), d.getArtifactId());
        f.mkdirs();
        getLog().info("unzipping " + d.getArtifactId() + " to directory " + f.getCanonicalPath());

        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(d.getFile()));
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            getLog().info("\t" + fileName);
            File newFile = new File(f, fileName);
            new File(newFile.getParent()).mkdirs();
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();

            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        zis.close();
        return f;
    }

    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<URL> pathUrls = new ArrayList<>();
            for (String mavenCompilePath : project.getCompileClasspathElements()) {
                pathUrls.add(new File(mavenCompilePath).toURI().toURL());
            }

            URL[] urlsForClassLoader = pathUrls.toArray(new URL[pathUrls.size()]);
            getLog().debug("urls for URLClassLoader: " + Arrays.asList(urlsForClassLoader));

            // need to define parent classloader which knows all dependencies of the plugin
            return new URLClassLoader(urlsForClassLoader, VdbCodeGeneratorMojo.class.getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    private boolean generateOpenApiScoffolding() {
        List<Dependency> dependencies = project.getDependencies();
        for (Dependency d : dependencies) {
            String gav = d.getGroupId() + ":" + d.getArtifactId();
            if (gav.equals("org.teiid:spring-openapi")) {
                getLog().info("OpenAPI dependency is found in the pom.xml");
                return true;
            }
        }
        getLog().info("No OpenAPI dependency is found in the pom.xml");
        return false;
    }

    private void createOpenApi(Configuration cfg, File javaSrcDir, Database database, HashMap<String, String> parentMap)
            throws Exception {

        // create swagger config file.
        createSwaggerConfig(cfg, javaSrcDir, database, parentMap);

        // create api
        for (Schema schema : database.getSchemas()) {
            if (schema.isPhysical()) {
                continue;
            }

            HashMap<String, String> replacementMap = new HashMap<String, String>(parentMap);
            replacementMap.put("modelName", schema.getName());

            Template template = cfg.getTemplate("Controller.java");
            FileWriter out = new FileWriter(new File(javaSrcDir, schema.getName() + ".java"));
            template.process(replacementMap, out);

            String servicePattern =
                    "    @RequestMapping(value = \"${uri}\", method = RequestMethod.${method}, produces = {\"${contentType}\"} <#if consumes??>, consumes = \"${consumes}\" </#if>)\n" +
                            "    @ResponseBody\n" +
                            "    @ApiOperation(value=\"${description}\"<#if responseClass??>, response=${responseClass}</#if>)\n" +
                            "    public ResponseEntity<InputStreamResource> ${procedureName}(${paramSignature}) {\n" +
                            "        setServer(this.server);\n"+
                            "        setVdb(this.vdb);\n"+
                            "        LinkedHashMap<String, Object> parameters = new LinkedHashMap<String, Object>();\n" +
                            "        ${paramMapping}\n" +
                            "        return execute(\"${procedureFullName}\", parameters, \"${charset}\", ${usingReturn});\n" +
                            "    }\n";
            StringTemplateLoader stl = new StringTemplateLoader();
            stl.putTemplate("service", servicePattern);
            cfg.setTemplateLoader(stl);

            Collection<Procedure> procedures = schema.getProcedures().values();
            for (Procedure procedure : procedures) {
                String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
                String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);
                if (uri != null && method != null) {
                    buildRestService(procedure, replacementMap, cfg, out);
                }
            }
            out.write("}");
            out.flush();
            out.close();
        }
    }

    private void buildRestService(Procedure procedure, HashMap<String, String> replacementMap, Configuration cfg,
            FileWriter out) throws Exception {
        String uri = procedure.getProperty(REST_NAMESPACE + "URI", false);
        String method = procedure.getProperty(REST_NAMESPACE + "METHOD", false);

        String contentType = procedure.getProperty(REST_NAMESPACE + "PRODUCES", false);
        if (contentType == null) {
            contentType = findContentType(procedure);
        }

        String charSet = procedure.getProperty(REST_NAMESPACE + "CHARSET", false);
        if (charSet == null) {
            charSet = Charset.defaultCharset().name();
        }

        List<ProcedureParameter> params = new ArrayList<ProcedureParameter>(procedure.getParameters().size());
        boolean usingReturn = false;
        boolean hasLobInput = false;
        for (ProcedureParameter p : procedure.getParameters()) {
            if (p.getType() == ProcedureParameter.Type.In || p.getType() == ProcedureParameter.Type.InOut) {
                params.add(p);
            } else if (p.getType() == ProcedureParameter.Type.ReturnValue && procedure.getResultSet() == null) {
                usingReturn = true;
            }
            if (!hasLobInput) {
                String runtimeType = p.getRuntimeType();
                hasLobInput = DataTypeManager.isLOB(runtimeType);
            }
        }

        boolean useMultipart = false;
        if (method.toUpperCase().equals("POST") && hasLobInput) {
            useMultipart = true;
        }

        replacementMap.put("contentType", contentType);
        replacementMap.put("uri", uri);
        replacementMap.put("method", method);
        replacementMap.put("charset", charSet);
        replacementMap.put("description", procedure.getAnnotation() == null ? "" : procedure.getAnnotation());
        replacementMap.put("procedureName", procedure.getName());
        replacementMap.put("procedureFullName", procedure.getFullName());
        replacementMap.put("usingReturn", usingReturn?"true":"false");

        // handle parameters
        StringBuilder paramSignature = new StringBuilder();
        StringBuilder paramMapper = new StringBuilder();
        int paramsSize = params.size();

        if (useMultipart) {
            replacementMap.put("consumes", "multipart/form-data");
        } else {
            // post only accepts Form inputs, not path params
            boolean get = method.toUpperCase().equals("GET");

            if (paramsSize > 0 && !get) {
                replacementMap.put("consumes", "application/x-www-form-urlencoded");
            }
        }

        HashSet<String> pathParms = getPathParameters(uri);
        for (int i = 0; i < paramsSize; i++) {
            String runtimeType = params.get(i).getRuntimeType();
            String paramType = "@RequestParam(name=\"" + params.get(i).getName() + "\")";
            if (pathParms.contains(params.get(i).getName())) {
                paramType = "@PathVariable(name=\"" + params.get(i).getName() + "\")";
            }
            if (i > 0) {
                paramSignature.append(", ");
            }
            String type = params.get(i).getJavaType().getName();
            if (DataTypeManager.isLOB(runtimeType)) {
                type = "MultipartFile";
            }
            paramSignature.append(paramType).append(" ").append(type).append(" ")
            .append(params.get(i).getName());
            paramMapper.append("parameters.put(\"").append(params.get(i).getName()).append("\",")
            .append(params.get(i).getName()).append(");\n    ");
        }
        replacementMap.put("paramSignature", paramSignature.toString());
        replacementMap.put("paramMapping", paramMapper.toString());

        //TODO: this needs to be fixed, such that correct return is done
        //replacementMap.put("responseClass", "OpenApiInputStream.class");

        Template template = cfg.getTemplate("service");
        template.process(replacementMap, out);
        out.write("\n");
    }

    private static HashSet<String> getPathParameters(String uri ) {
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

    private String findContentType(Procedure procedure) {
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
}
