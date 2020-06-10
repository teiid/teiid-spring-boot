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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
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

import com.github.jknack.handlebars.internal.text.StringTokenizer;
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

    @Parameter(defaultValue = "${basedir}/src/main/resources/openapi.json")
    private File openApiFile;

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



    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        try {
            MustacheFactory mf = new DefaultMustacheFactory();

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
            File javaSrcDir = new File(getOutputDirectory()+"/src/main/java", codeDirectory);
            if (!javaSrcDir.exists()) {
                javaSrcDir.mkdirs();
            }
            File resourcesDir = new File(getOutputDirectory()+"/src/main/resources");
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs();
            }

            // find connection factories
            ExternalSources sources = new ExternalSources();
            StringTokenizer st = new StringTokenizer(componentScanPackageNames, ",");
            while (st.hasNext()) {
                sources.loadConnctionFactories(pluginClassloader, st.next());
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
                materialization.instrumentForMaterialization(databaseStore, resourcesDir);
                Resource resource = new Resource();
                resource.setDirectory(getOutputDirectory().getPath()+"/src/main/resources");
                this.project.addResource(resource);
            }

            HashMap<String, String> parentMap = new HashMap<String, String>();
            parentMap.put("packageName", this.packageName);
            parentMap.put("vdbName", database.getName());
            parentMap.put("vdbDescription", database.getAnnotation());
            if (generateOpenApiScoffolding()) {
                parentMap.put("openapi", generateOpenApiScoffolding()?"true":"false");
            }
            if (this.generateApplicationClass) {
                createApplication(mf, javaSrcDir, database, parentMap);
            }
            if (this.generateDataSourceClasses) {
                createDataSources(mf, javaSrcDir, database, parentMap, pluginClassloader, sources);
            }
            verifyTranslatorDependencies(database, sources, pluginClassloader);

            // also look for .yml equivalent
            if (!this.openApiFile.exists()) {
                String ymlFile = this.openApiFile.getAbsolutePath().replace("openapi.json", "openapi.yml");
                if (!ymlFile.contentEquals(this.openApiFile.getAbsolutePath())) {
                    this.openApiFile = new File(ymlFile);
                }
            }

            if (generateOpenApiScoffolding() && this.openApiFile.exists()) {
                getLog().info("Found the OpenAPI document at " + this.openApiFile.getAbsolutePath());
                ApiGenerator generator = new ApiGenerator(openApiFile, outputDirectory, getLog());
                generator.generate(mf, javaSrcDir, database, parentMap);
            } else {
                getLog().info("No OpenAPI document found, no classes for the OpenAPI will be generated ");
            }
            this.project.addCompileSourceRoot(javaSrcDir.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException("Error running the vdb-codegen-plugin.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private PluginDatabaseStore getDatabaseStore(File vdbfile) throws IOException {
        Map<String, Datatype> typeMap = SystemMetadata.getInstance().getRuntimeTypeMap();
        PluginDatabaseStore store = new PluginDatabaseStore(typeMap);
        store.parse(vdbfile);
        return store;
    }

    private void createApplication(MustacheFactory mf, File javaSrcDir, Database database, HashMap<String, String> props)
            throws Exception {
        getLog().info("Creating the Application.java class");
        Mustache mustache = mf.compile(
                new InputStreamReader(this.getClass().getResourceAsStream("/templates/Application.mustache")),
                "application");
        Writer out = new FileWriter(new File(javaSrcDir, "Application.java"));
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

    private String getBaseDataWrapper(Database db, String name) {
        if (db.getDataWrapper(name) != null) {
            if (db.getDataWrapper(name).getType() == null) {
                return name;
            }
            return getBaseDataWrapper(db, db.getDataWrapper(name).getType());
        }
        return name;
    }

    private void createDataSources(MustacheFactory mf, File javaSrcDir, Database database,
            HashMap<String, String> parentMap, ClassLoader classLoader, ExternalSources externalSources) throws Exception {

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

            Mustache mustache = loadMustache(mf, source, classLoader);
            if (mustache != null) {
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                mustache.execute(out, tempMap);
                out.close();
            } else {
                throw new MojoExecutionException("Failed to generate source for name :" + translator +
                        " make sure it is supported source, if this a custom source, it is developed with "
                        + "@ConnectionFactoryConfiguration annotation and Mustache file is provided");
            }
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

    static Mustache loadMustache(MustacheFactory mf, ExternalSource source, ClassLoader classLoader) {
        Mustache mustache = null;
        InputStream is = classLoader.getResourceAsStream(source.getName() + ".mustache");
        if (is != null) {
            mustache = mf.compile(new InputStreamReader(is),source.getName().toLowerCase());
        } else {
            mustache = mf.compile(
                    new InputStreamReader(VdbCodeGeneratorMojo.class.getResourceAsStream("/templates/Jdbc.mustache")),
                    source.getName().toLowerCase());
        }
        return mustache;
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
                File openapi = new File(vdbDir, "openapi.json");
                if (openapi.exists()) {
                    this.openApiFile = openapi;
                }
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
                File openapi = new File(vdbDir, "openapi.json");
                if (openapi.exists()) {
                    this.openApiFile = openapi;
                }
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
            File openapi = new File(vdbDir, "openapi.json");
            if (openapi.exists()) {
                this.openApiFile = openapi;
            }
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

    private boolean generateOpenApiScoffolding() {
        List<Dependency> dependencies = project.getDependencies();
        for (Dependency d : dependencies) {
            String gav = d.getGroupId() + ":" + d.getArtifactId();
            if (gav.equals("org.teiid:spring-openapi")) {
                getLog().info("OpenAPI dependency is found in the pom.xml");
                return true;
            }
        }
        getLog().info("No OpenAPI dependency is found in the pom.xml, skipping the generation of the OpenAPI classes");
        return false;
    }

    private File unzipContents(Artifact d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getArtifactId());
        f.mkdirs();
        getLog().info("unzipping " + d.getArtifactId() + " to directory " + f.getCanonicalPath());

        return unzipContents(d.getFile(), f);
    }

    private File unzipContents(File d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getName());
        f.mkdirs();
        getLog().info("unzipping " + d.getName() + " to directory " + f.getCanonicalPath());

        return unzipContents(d, f);
    }

    public static File unzipContents(File in, File out) throws FileNotFoundException, IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(in));
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(out, fileName);
            if (ze.isDirectory()) {
                newFile.mkdirs();
            } else {
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        zis.close();
        return out;
    }
}
