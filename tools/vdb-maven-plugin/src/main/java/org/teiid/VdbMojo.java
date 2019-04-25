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
package org.teiid;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.metadata.Database;
import org.teiid.metadata.Datatype;
import org.teiid.metadata.Grant;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.DatabaseStore;
import org.teiid.query.metadata.DatabaseStore.Mode;
import org.teiid.query.metadata.SystemMetadata;
import org.teiid.query.parser.QueryParser;

/**
 * https://stackoverflow.com/questions/1427722/how-do-i-create-a-new-packaging-type-for-maven
 * http://softwaredistilled.blogspot.com/2015/07/how-to-create-custom-maven-packaging.html
 */
@Mojo(name = "vdb", requiresDependencyResolution = ResolutionScope.COMPILE)
public class VdbMojo extends AbstractMojo {
    private static final String SLASH = "/";

    @Parameter(defaultValue = "${basedir}/src/main/vdb")
    private File vdbFolder;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "project.build.directory", readonly = true)
    private File outputDirectory;

    @Parameter(property = "project.build.finalName", readonly = true)
    private String finalName;

    //A list of folders or files to be included in the final artifact archive.
    @Parameter
    private File[] includes;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    private static class VdbImport {
        String dbName;
        String version;
        boolean importPolicies;
    }

    private static class DbWithImports {
        Database database;
        List<VdbImport> vdbImports;
        DatabaseStore store;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginClassloader = getClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(pluginClassloader);
            File artifact = new File(this.outputDirectory, this.finalName + ".vdb");

            this.project.getArtifact().setFile(artifact);

            try (ArchiveOutputStream archive = getStream(artifact)) {
                File vdbFile = this.getVDBFile();
                if (vdbFile == null) {
                    throw new MojoExecutionException("No VDB File found in directory" + this.vdbFolder);
                }

                // add config, classes, lib and META-INF directories
                Set<File> directories = new LinkedHashSet<>();
                gatherContents(this.classesDirectory, directories);
                gatherContents(this.vdbFolder, directories);

                DbWithImports top = getDatabase(vdbFile);

                // check if the VDB has any vdb imports, if yes, then check the dependencies
                if (!top.vdbImports.isEmpty()) {
                    top.store.startEditing(false);

                    // read import vdbs
                    Set<Artifact> dependencies = project.getArtifacts();
                    for (Artifact d : dependencies) {

                        if (d.getFile() == null || !d.getFile().getName().endsWith(".vdb")) {
                            continue;
                        }

                        File vdbDir = unzipContents(d);
                        File childFile = new File(vdbDir, "vdb.ddl");
                        getLog().info("Merging VDB " + childFile.getCanonicalPath());
                        DbWithImports child = getDatabase(childFile);

                        if (!child.vdbImports.isEmpty()) {
                            throw new MojoExecutionException("Nested VDB imports are not supported" + d.getArtifactId());
                        }

                        VdbImport matched = null;
                        for (VdbImport importee : top.vdbImports) {
                            if (child.database.getName().equals(importee.dbName)
                                    && child.database.getVersion().equals(importee.version)) {

                                gatherContents(vdbDir, directories);

                                child.database.getSchemas().forEach((v) -> {
                                    top.store.schemaCreated(v, new ArrayList<String>());
                                    /*
                                    String visibilityOverride = top.database.getProperty(v.getName() + ".visible", false); //$NON-NLS-1$
                                    if (visibilityOverride != null) {
                                        boolean visible = Boolean.valueOf(visibilityOverride);
                                        top.store.addOrSetOption(top.database.getName(), Database.ResourceType.DATABASE,
                                                v.getName() + ".visible", Boolean.toString(visible), false);
                                    }
                                     */
                                });

                                if (importee.importPolicies) {
                                    for (Grant grant : child.database.getGrants()) {
                                        top.store.grantCreated(grant);
                                    }
                                }
                                matched = importee;
                                break;
                            }
                        }
                        if (matched != null) {
                            top.vdbImports.remove(matched);
                        }
                    }
                }
                add(archive, "", directories.toArray(new File[0]));

                File finalVDB = new File(this.outputDirectory.getPath(), "vdb.ddl");
                finalVDB.getParentFile().mkdirs();
                top.store.stopEditing();
                String vdbDDL = DDLStringVisitor.getDDLString(top.store.getDatabases().get(0));
                getLog().debug(vdbDDL);
                ObjectConverterUtil.write(new StringReader(vdbDDL),  finalVDB);
                addFile(archive, "vdb.ddl", finalVDB);
            } catch (Exception e) {
                throw new MojoExecutionException("Exception when creating artifact archive.", e);
            }
        } catch (MojoExecutionException e) {
            throw new MojoExecutionException("Error running the vdb-maven-plugin.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    private DbWithImports getDatabase(File vdbfile) throws IOException {
        List<VdbImport> vdbImports = new ArrayList<>();
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
            @Override
            public void importDatabase(String dbName, String version, boolean importPolicies) {
                VdbImport vdb = new VdbImport();
                vdb.dbName = dbName;
                vdb.version = version;
                vdb.importPolicies = importPolicies;
                vdbImports.add(vdb);
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
        DbWithImports dbwi = new DbWithImports();
        dbwi.database = db;
        dbwi.vdbImports = vdbImports;
        dbwi.store = store;
        return dbwi;
    }

    private void gatherContents(File f, Set<File> directories) {
        if (f.exists() && f.isDirectory()) {
            File[] list = f.listFiles();

            for (File l : Objects.requireNonNull(list)) {
                if (l.isDirectory()) {
                    directories.add(l);
                }
                if (!l.getName().endsWith("vdb.ddl")) {
                    directories.add(l);
                }
            }
        }
    }

    private File unzipContents(Artifact d) throws IOException {
        File f = new File(this.outputDirectory.getPath(), d.getArtifactId());
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

    private File getVDBFile() {
        if (this.vdbFolder.exists() && this.vdbFolder.isDirectory()) {
            File[] list = this.vdbFolder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith("-vdb.ddl");
                }
            });
            if ((list != null ? list.length : 0) != 0) {
                getLog().info("Found VDB = " + list[0].getName());
                return list[0];
            }
        }
        return null;
    }

    private void addFile(ArchiveOutputStream archive, String name, File file) throws IOException {
        getLog().info("Adding file = " + name + " from " + file.getCanonicalPath());
        ArchiveEntry entry = this.entry(file, name);
        archive.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file), archive);
        archive.closeArchiveEntry();
    }

    private void add(ArchiveOutputStream archive, String path, File... files) throws IOException {
        for (File file : files) {
            if (!file.exists()) {
                throw new FileNotFoundException("Folder or file not found: " + file.getPath());
            }
            String name = path + file.getName();
            if (file.isDirectory()) {
                this.add(archive, name + SLASH, Objects.requireNonNull(file.listFiles()));
            } else {
                if (!name.endsWith("vdb.xml")) {
                    addFile(archive, name, file);
                }
            }
        }
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
            return new URLClassLoader(urlsForClassLoader, VdbMojo.class.getClassLoader());
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    protected ArchiveOutputStream getStream(File artifact) throws IOException {
        if (!this.outputDirectory.exists()) {
            this.outputDirectory.mkdirs();
        }
        FileOutputStream output = new FileOutputStream(artifact);
        try {
            return new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, output);
        } catch (ArchiveException e) {
            throw new IOException(e);
        }
    }

    protected ArchiveEntry entry(File file, String name) {
        return new ZipArchiveEntry(file, name);
    }
}
