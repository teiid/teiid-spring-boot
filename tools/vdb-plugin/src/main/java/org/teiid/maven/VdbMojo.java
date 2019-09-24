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
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.core.util.ReaderInputStream;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.Grant;
import org.teiid.metadata.Server;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;

/**
 * https://stackoverflow.com/questions/1427722/how-do-i-create-a-new-packaging-type-for-maven
 * http://softwaredistilled.blogspot.com/2015/07/how-to-create-custom-maven-packaging.html
 */
@Mojo(name = "vdb", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class VdbMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}/src/main/vdb")
    private File vdbFolder;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "project.build.directory", readonly = true)
    protected File outputDirectory;

    @Parameter(property = "project.build.finalName", readonly = true)
    protected String finalName;

    @Parameter
    private Boolean includeDependencies = false;

    private FileFilter vdbFilter = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return !f.getName().equals("vdb.ddl") && !f.getName().endsWith("-vdb.ddl") && !f.getName().endsWith(".vdb")
                    && !f.getName().endsWith("-vdb.xml");
        }
    };

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginClassloader = getClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(pluginClassloader);
            if (!this.outputDirectory.exists()) {
                this.outputDirectory.mkdirs();
            }

            File artifact = new File(this.outputDirectory, this.finalName + ".vdb");

            this.project.getArtifact().setFile(artifact);

            try (ZipArchive archive = new ZipArchive(artifact)) {
                File vdbFile = this.getMainVDBFile();
                if (vdbFile == null) {
                    throw new MojoExecutionException("No \"vdb.ddl\" File found in directory" + this.vdbFolder);
                }

                PluginDatabaseStore top = getDatabaseStore(vdbFile);
                // check if the VDB has any vdb imports, if yes, then check the dependencies
                if (!top.getVdbImports().isEmpty()) {
                    try {
                        top.startEditing(false);
                        top.databaseSwitched(top.db().getName(), top.db().getVersion());

                        // read import vdbs from files in folder
                        List<String> files = Files.walk(Paths.get(this.vdbFolder.getPath())).map(f -> f.toString())
                                .filter(f -> f.endsWith("-vdb.ddl")).collect(Collectors.toList());

                        for (String f : files) {
                            File childFile = new File(f);
                            mergeVDB(top, childFile, childFile.getName());
                        }

                        // read import vdbs from dependencies
                        Set<Artifact> dependencies = project.getArtifacts();
                        for (Artifact d : dependencies) {
                            if (d.getFile() == null || !d.getFile().getName().endsWith(".vdb")) {
                                continue;
                            }
                            File vdbDir = unzipContents(d);
                            File childFile = new File(vdbDir, "META-INF/vdb.ddl");
                            // if the merge target is correct then gather up the contents
                            if (mergeVDB(top, childFile, d.getArtifactId())) {
                                archive.addToArchive(vdbDir, "", this.vdbFilter);
                            }
                        }
                    } catch (IOException e) {
                        throw new MojoExecutionException(e.getMessage());
                    } finally {
                        top.stopEditing();
                    }
                }

                if (!top.getVdbImports().isEmpty()) {
                    throw new MojoExecutionException("VDB import for " + top.getVdbImports().get(0).getDbName()
                            + " is not resolved. Either provide the -vdb.ddl file in " + this.vdbFolder.getName()
                            + "folder, or define the dependency for the vdb in the pom.xml");
                }

                // gather local directory contents
                archive.addToArchive(this.vdbFolder, "", this.vdbFilter);

                File finalVDB = new File(this.outputDirectory.getPath(), "vdb.ddl");
                finalVDB.getParentFile().mkdirs();

                String vdbDDL = DDLStringVisitor.getDDLString(top.db());
                getLog().debug(vdbDDL);
                ObjectConverterUtil.write(new ReaderInputStream(new StringReader(vdbDDL), Charset.forName("UTF-8")),
                        finalVDB);

                archive.addToArchive(finalVDB, "META-INF/" + finalVDB.getName());

                // add dependencies, like JDBC driver files
                if (this.includeDependencies) {
                    Set<Artifact> dependencies = project.getArtifacts();
                    for (Artifact d : dependencies) {
                        if (d.getFile() == null || !d.getFile().getName().endsWith(".jar")) {
                            continue;
                        }
                        archive.addToArchive(d.getFile(), "/lib/" + d.getFile().getName());
                    }
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Exception when creating artifact archive.", e);
            }
        } catch (MojoExecutionException e) {
            throw new MojoExecutionException("Error running the vdb-plugin.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private File getMainVDBFile() {
        if (this.vdbFolder.exists() && this.vdbFolder.isDirectory()) {
            File f = new File(this.vdbFolder, "vdb.ddl");
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private PluginDatabaseStore getDatabaseStore(File vdbfile) throws IOException {
        Map<String, Datatype> typeMap = SystemMetadata.getInstance().getRuntimeTypeMap();
        PluginDatabaseStore store = new PluginDatabaseStore(typeMap);
        store.parse(vdbfile);
        return store;
    }

    private boolean mergeVDB(PluginDatabaseStore top, File childFile, String childName)
            throws MojoExecutionException, IOException {
        VdbImport matched = null;

        getLog().info("Merging VDB " + childFile.getCanonicalPath());
        PluginDatabaseStore child = getDatabaseStore(childFile);

        if (!child.getVdbImports().isEmpty()) {
            throw new MojoExecutionException("Nested VDB imports are not supported" + childName);
        }

        for (VdbImport importee : top.getVdbImports()) {
            if (child.db().getName().equalsIgnoreCase(importee.getDbName())) {

                child.db().getSchemas().forEach((v) -> {
                    if (v.isPhysical()) {
                        for (Server s : v.getServers()) {
                            if (top.getServer(s.getName()) == null) {
                                String dw = s.getDataWrapper();
                                if (top.getCurrentDatabase().getDataWrapper(dw) == null) {
                                    top.dataWrapperCreated(child.db().getDataWrapper(dw));
                                }
                                top.serverCreated(s);
                            }
                        }
                    }

                    top.schemaCreated(v, new ArrayList<String>());
                });

                if (importee.isImportPolicies()) {
                    for (Grant grant : child.db().getGrants()) {
                        top.grantCreated(grant);
                    }
                }
                matched = importee;
                break;
            }
        }
        if (matched != null) {
            top.getVdbImports().remove(matched);
            return true;
        }
        return false;
    }

    private File unzipContents(Artifact d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getArtifactId());
        f.mkdirs();
        getLog().info("unzipping " + d.getArtifactId() + " to directory " + f.getCanonicalPath());
        return ZipArchive.unzip(d.getFile(), f);
    }

    protected ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<URL> pathUrls = new ArrayList<>();
            for (String mavenCompilePath : project.getCompileClasspathElements()) {
                pathUrls.add(new File(mavenCompilePath).toURI().toURL());
            }

            URL[] urlsForClassLoader = pathUrls.toArray(new URL[pathUrls.size()]);
            getLog().debug("urls for URLClassLoader: " + Arrays.asList(urlsForClassLoader));

            // need to define parent classloader which knows all dependencies of the plugin
            return new URLClassLoader(urlsForClassLoader, VdbMojo.class.getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }
}
