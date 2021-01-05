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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.teiid.metadata.Database;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.Server;
import org.teiid.query.function.SystemFunctionManager;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.spring.common.ExternalSource;
import org.teiid.spring.common.ExternalSources;
import org.yaml.snakeyaml.Yaml;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


@Mojo(name = "vdb-codegen", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyCollection=ResolutionScope.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, requiresProject = true, threadSafe=true)
public class VdbCodeGeneratorMojo extends AbstractMojo {
    public static final SystemFunctionManager SFM = SystemMetadata.getInstance().getSystemFunctionManager();
    public static final String ISPN = "infinispan-hotrod";

    @Parameter(defaultValue = "${basedir}/src/main/resources/teiid.ddl")
    private File vdbFile;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/teiid-sb")
    private File outputDirectory;

    @Parameter
    private String packageName;

    @Parameter
    private Boolean generateApplicationClass = true;

    @Parameter
    private Boolean generateDataSourceClasses = true;

    @Parameter
    private Boolean generatePomXml = false;

    @Parameter
    private Boolean generateApplicationProperties = false;

    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor pluginDescriptor;

    @Parameter
    private String componentScanPackageNames = "org.teiid.spring.data";

    @Parameter
    private String materializationType = ISPN;

    @Parameter
    private Boolean materializationEnable = false;

    @Parameter
    private String vdbVersion = "";

    @Parameter
    private File yamlFile;

    public File getOutputDirectory() {
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        return outputDirectory;
    }

    public File getJavaSrcDirectory() {
        String codeDirectory = this.packageName.replace('.', '/');
        File javaSrcDir = new File(getOutputDirectory()+"/src/main/java", codeDirectory);
        if (!javaSrcDir.exists()) {
            javaSrcDir.mkdirs();
        }
        return javaSrcDir;
    }

    public File getResourceDirectory() {
        File resourcesDir = new File(getOutputDirectory()+"/src/main/resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        return resourcesDir;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try {
            if (this.packageName == null) {
                this.packageName = project.getGroupId();
            }

            MustacheFactory mf = new DefaultMustacheFactory();

            ClassLoader pluginClassloader = getClassLoader();
            Thread.currentThread().setContextClassLoader(pluginClassloader);

            // find connection factories
            ExternalSources sources = new ExternalSources();
            StringTokenizer st = new StringTokenizer(componentScanPackageNames, ",");
            while (st.hasMoreTokens()) {
                sources.loadConnctionFactories(pluginClassloader, st.nextToken());
            }

            // check if YAML file exists, then build VDB from it
            boolean validateDependencies = true;
            Map<?,?> yamlContents = null;
            if (this.yamlFile!= null && this.yamlFile.exists()) {
                yamlContents = processYamlVdb();
                this.vdbFile = grabVdbFromYaml(yamlContents);
                this.generatePomXml = true;
                this.generateApplicationProperties = true;
                this.generateApplicationClass = false;
                this.generateDataSourceClasses = false;
                validateDependencies = false;
            }

            // Load the DDL into data store
            File vdbfile = this.getVDBFile();
            PluginDatabaseStore databaseStore = getDatabaseStore(vdbfile);
            Database database = databaseStore.db();

            // Add materialization model, if any views with materialization detected
            getLog().info("Materialization enabled: " + this.materializationEnable);
            getLog().info("Materialization type: " + this.materializationType);
            MaterializationEnhancer materialization = new MaterializationEnhancer(this.materializationType, getLog(),
                    this.vdbVersion);
            if (this.materializationEnable && materialization.isMaterializationRequired(databaseStore)) {
                getLog().info("VDB requires Materialization Intrumentation: " + databaseStore.db().getName());
                materialization.instrumentForMaterialization(databaseStore, getResourceDirectory());
                Resource resource = new Resource();
                resource.setDirectory(getOutputDirectory().getPath()+"/src/main/resources");
                this.project.addResource(resource);
            }

            HashMap<String, String> parentMap = new HashMap<String, String>();
            parentMap.put("packageName", this.packageName);
            parentMap.put("vdbName", database.getName());
            parentMap.put("vdbDescription", database.getAnnotation());

            if (this.generateApplicationClass) {
                createApplication(mf, database, parentMap);
            }
            if (this.generateDataSourceClasses) {
                walkDataSources(database, parentMap, sources,
                        new DataSourceCodeGenerator(mf, getJavaSrcDirectory(), pluginClassloader));
            }

            // only validate dependencies if we are working off the final pom file
            // sometimes we are also generating the final pom file, where we are gathering these dependencies
            if (validateDependencies) {
                verifyTranslatorDependencies(database, sources, pluginClassloader);
            }

            if (this.generatePomXml) {
                createPomXml(database, yamlContents, mf, parentMap);
            }

            if (this.generateApplicationProperties) {
                createApplicationProperties(database, yamlContents, mf, parentMap, sources);
            }
            this.project.addCompileSourceRoot(getJavaSrcDirectory().getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Error running the vdb-codegen-plugin.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private Map<?, ?> processYamlVdb() throws FileNotFoundException, IOException {
        Map<?, ?> yamlContents;
        Yaml yaml = new Yaml();
        FileInputStream fis = new FileInputStream(this.yamlFile);
        yamlContents = yaml.loadAs(fis, Map.class);
        fis.close();

        return (Map<?,?>)yamlContents.get("spec");

    }

    private File grabVdbFromYaml(Map<?,?> spec) throws IOException {
        Map<?,?> build = (Map<?,?>)spec.get("build");
        Map<?,?> source = (Map<?,?>)build.get("source");
        String ddl = (String)source.get("ddl");
        if (ddl != null) {
            File f = new File(getResourceDirectory()+"/teiid.ddl");
            FileWriter fw = new FileWriter(f);
            fw.write(ddl);
            fw.close();
            return f;
        }
        return null;
    }

    private PluginDatabaseStore getDatabaseStore(File vdbfile) throws IOException {
        Map<String, Datatype> typeMap = SystemMetadata.getInstance().getRuntimeTypeMap();
        PluginDatabaseStore store = new PluginDatabaseStore(typeMap);
        store.parse(vdbfile);
        return store;
    }

    private void createApplication(MustacheFactory mf, Database database, HashMap<String, String> props)
            throws Exception {
        getLog().info("Creating the Application.java class");
        Mustache mustache = mf.compile(
                new InputStreamReader(this.getClass().getResourceAsStream("/templates/Application.mustache")),
                "application");
        Writer out = new FileWriter(new File(getJavaSrcDirectory(), "Application.java"));
        mustache.execute(out, props);
        out.close();
    }

    private void verifyTranslatorDependencies(Database database, ExternalSources sources, ClassLoader classloader)
            throws Exception {
        for(Server server : database.getServers()) {
            String translator = getBaseDataWrapper(database, server.getDataWrapper());
            ExternalSource es = sources.find(translator);
            if (es == null) {
                throw new MojoExecutionException("No supported source found with name:" + server.getDataWrapper());
            }

            boolean foundDependency = false;
            if (es.isJdbc() && es.getDriverNames() != null && es.getDriverNames().length > 0) {
                for (String driverName: es.getDriverNames()) {
                    try {
                        Class.forName(driverName, false, classloader);
                        foundDependency = true;
                        break;
                    } catch (ClassNotFoundException e) {
                        continue;
                    }
                }
                if (!foundDependency) {
                    String str = "Drivers for translator \"" + server.getDataWrapper()
                    + "\" can not be verified. Make sure you have the required dependencies in the pom.xml";
                    getLog().error(str);
                    throw new MojoExecutionException(str);
                }
            }
        }
    }

    private void createPomXml(Database database, Map<?, ?> spec, MustacheFactory mf, HashMap<String, String> props)
            throws Exception {
        StringBuilder sb = new StringBuilder();

        for(Server server : database.getServers()) {
            String translator = getBaseDataWrapper(database, server.getDataWrapper());
            sb.append("    <dependency>\n");
            sb.append("      <groupId>org.teiid</groupId>\n");
            sb.append("      <artifactId>spring-data-").append(translator).append("</artifactId>\n");
            sb.append("      <version>${teiid.springboot.version}</version>\n");
            sb.append("    </dependency>\n");
        }

        Map<?,?> build = (Map<?,?>)spec.get("build");
        Map<?,?> source = (Map<?,?>)build.get("source");
        List<String> dependencies = (List<String>)source.get("dependencies");
        if (dependencies != null) {
            for (String gav : dependencies) {
                int idx = gav.indexOf(':');
                int versionIdx = -1;
                if (idx != -1) {
                    versionIdx = gav.indexOf(idx, ':');
                }
                if (idx == -1 || versionIdx == -1) {
                    throw new MojoExecutionException("dependencies defined are not in correct GAV format. Must be in"
                            + "\"groupId:artifactId:version\" format.");
                }
                String groupId = gav.substring(0, idx);
                String artifactId = gav.substring(idx+1, versionIdx);
                String version = gav.substring(versionIdx+1);

                sb.append("    <dependency>\n");
                sb.append("      <groupId>").append(groupId).append("</groupId>\n");
                sb.append("      <artifactId>").append(artifactId).append("</artifactId>\n");
                sb.append("      <version>").append(version).append("</version>\n");
                sb.append("    </dependency>\n");
            }
        }

        props.put("vdbDependencies", sb.toString());

        getLog().info("Creating the pom.xml");
        Mustache mustache = mf.compile(
                new InputStreamReader(this.getClass().getResourceAsStream("/templates/pom.mustache")),
                "application");
        Writer out = new FileWriter(new File(getOutputDirectory(), "pom.xml"));
        mustache.execute(out, props);
        out.close();
    }

    private void createApplicationProperties(Database database, Map<?, ?> spec, MustacheFactory mf,
            HashMap<String, String> props, ExternalSources externalSources) throws Exception {
        StringBuilder sb = new StringBuilder();

        List<?> dataSources = (List<?>)spec.get("datasources");
        for (Object ds : dataSources) {
            Map<?,?> datasource = (Map<?,?>)ds;
            String name = (String) datasource.get("name");
            String type = (String) datasource.get("type");
            List<?> properties = (List<?>)datasource.get("properties");
            for (Object p : properties) {
                Map<?,?> prop = (Map<?,?>)p;
                sb.append("spring.teiid.data.").append(type).append(".")
                .append(name).append(".").append((String) prop.get("name")).append("=")
                .append((String) prop.get("value")).append("\n");
            }
        }

        props.put("dsProperties", sb.toString());
        getLog().info("Creating the application.properties");
        Mustache mustache = mf.compile(
                new InputStreamReader(this.getClass().getResourceAsStream("/templates/application_properties.mustache")),
                "application");
        Writer out = new FileWriter(new File(getResourceDirectory(), "application.properties"));
        mustache.execute(out, props);
        out.close();
    }

    private String getBaseDataWrapper(Database db, String name) {
        if (db.getDataWrapper(name) != null) {
            if (db.getDataWrapper(name).getType() == null) {
                return name;
            }
            return getBaseDataWrapper(db, db.getDataWrapper(name).getType());
        }
        return name;
    }

    private void walkDataSources(Database database,
            HashMap<String, String> parentMap, ExternalSources externalSources, CodeGenerator c) throws Exception {

        TreeMap<String, ExternalSource> sources = new TreeMap<>();
        for (ExternalSource source : externalSources.getItems().values()) {
            //we're using translator and alias name interchangeably
            sources.put(source.getName(), source);
            //there is a utility method that returns a list of sources,
            //but that's a linear scan and we only need the source type
            if (sources.get(source.getTranslatorName()) == null) {
                sources.put(source.getTranslatorName(), source);
            }
        }

        for (Server server : database.getServers()) {
            HashMap<String, String> tempMap = new HashMap<String, String>(parentMap);
            tempMap.put("dsName", server.getName());
            tempMap.put("sanitized-dsName", removeCamelCase(server.getName()));


            getLog().info("Building DataSource.java for source :" + server.getName());

            // Custom data sources are expected to provide their own DataSource classes
            // when application is built
            String translator = getBaseDataWrapper(database, server.getDataWrapper());

            ExternalSource source = sources.get(translator);

            if (source == null) {
                throw new MojoExecutionException("No Source found with name :" + translator +
                        " make sure it is supported source, if this a custom source, it is developed with "
                        + "@ConnectionFactoryConfiguration annotation");
            }
            tempMap.put("dsPropertyPrefix", "spring.teiid.data." + source.getName());
            c.generate(source, server, tempMap);
        }
    }

    static String removeCamelCase(String name) throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();

        boolean lastCharIsCaptital = false;
        for (int i = 0; i < name.length(); i++) {
            Character ch = name.charAt(i);
            if (Character.isWhitespace(ch) || ch == '.') {
                throw new MojoExecutionException("Space or dot (.) are not allowed in a property name : " + name);
            } else if (Character.isUpperCase(ch)) {
                if (!lastCharIsCaptital && i > 0) {
                    sb.append('-');
                    sb.append(Character.toLowerCase(ch));
                    lastCharIsCaptital = true;
                } else {
                    sb.append(Character.toLowerCase(ch));
                    lastCharIsCaptital = true;
                }
            } else {
                sb.append(ch);
                lastCharIsCaptital = false;
            }
        }
        return sb.toString();
    }



    private File getVDBFile() throws MojoExecutionException, IOException {
        // this is custom defined path, or default teiid.ddl
        if (this.vdbFile.exists()) {
            getLog().info("Found VDB = " + this.vdbFile);
            // if this is .vdb, then we need to extract the ddl file inside,
            // as vdb-generator only understands to handle ddl file
            if (this.vdbFile.getName().endsWith(".vdb")) {
                File vdbDir = unzipContents(this.vdbFile);
                this.vdbFile = new File(vdbDir, "/META-INF/vdb.ddl");
            }
            return this.vdbFile;
        }

        // teiid.vdb is also default version, check for that
        String f = this.vdbFile.getAbsolutePath().replace("teiid.ddl", "teiid.vdb");
        if (!f.contentEquals(this.vdbFile.getAbsolutePath())) {
            this.vdbFile = new File(f);
            if (this.vdbFile.exists()) {
                File vdbDir = unzipContents(this.vdbFile);
                this.vdbFile = new File(vdbDir, "/META-INF/vdb.ddl");
                return this.vdbFile;
            }
        }

        // find VDB from pom.xml dependencies, there can not be more than single
        // dependency of VDBs
        Set<Artifact> dependencies = project.getArtifacts();
        for (Artifact d : dependencies) {
            if (d.getFile() == null || !d.getFile().getName().endsWith(".vdb")) {
                continue;
            }
            File vdbDir = unzipContents(d);
            this.vdbFile = new File(vdbDir, "/META-INF/vdb.ddl");
            return this.vdbFile;
        }

        throw new MojoExecutionException(
                "No VDB File found at location " + this.vdbFile + " or no VDB dependencies found in pom.xml");
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

    private File unzipContents(Artifact d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getArtifactId());
        f.mkdirs();
        getLog().info("unzipping " + d.getArtifactId() + " to directory " + f.getCanonicalPath());

        return ZipUtils.unzipContents(d.getFile(), f);
    }

    public File unzipContents(File d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getName());
        f.mkdirs();
        getLog().info("unzipping " + d.getName() + " to directory " + f.getCanonicalPath());

        return ZipUtils.unzipContents(d, f);
    }
}
