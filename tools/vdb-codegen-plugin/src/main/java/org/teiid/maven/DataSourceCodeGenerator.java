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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.teiid.metadata.Server;
import org.teiid.spring.common.ExternalSource;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class DataSourceCodeGenerator implements CodeGenerator {
    private MustacheFactory mf;
    private File javaSrcDir;
    private ClassLoader classLoader;

    public DataSourceCodeGenerator(MustacheFactory mf, File javaSrcDir, ClassLoader classLoader) {
        this.mf = mf;
        this.javaSrcDir = javaSrcDir;
        this.classLoader = classLoader;
    }

    @Override
    public void generate(ExternalSource source, Server server, Map<String, String> props) throws MojoExecutionException {
        try {
            Mustache mustache = loadMustache(this.mf, source, this.classLoader);
            if (mustache != null) {
                Writer out = new FileWriter(new File(javaSrcDir, "DataSources" + server.getName() + ".java"));
                mustache.execute(out, props);
                out.close();
            } else {
                throw new MojoExecutionException("Failed to generate source for name :" + source.getTranslatorName() +
                        " make sure it is supported source, if this a custom source, it is developed with "
                        + "@ConnectionFactoryConfiguration annotation and Mustache file is provided");
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
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
}
