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

package org.jboss.teiid.springboot;

import static org.jboss.teiid.springboot.TeiidEmbeddedConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.core.util.ApplicationInfo;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.logging.LogManager;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.TranslatorException;
import org.xml.sax.SAXException;

/**
 * @author Kylin Soong
 */
public class VDBDeploymentListener implements SpringApplicationRunListener, Ordered {
    
    private final SpringApplication application;

    private final String[] args;
    
    private String[] vdbs;
    private String[] ddls;
    
    private EmbeddedServer embeddedServer;
    
    private volatile boolean stopAwait = false;
    
    public VDBDeploymentListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
    }

    @Override
    public void starting() {
        LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42001, ApplicationInfo.getInstance().getReleaseNumber()));

        List<String> vdbList = new ArrayList<>();
        List<String> ddlList = new ArrayList<>();
        for(String arg : args) {
            Path path = Paths.get(arg);
            if(Files.exists(path)) {
                try (InputStream content = Files.newInputStream(path, StandardOpenOption.READ)) {
                    VDBMetadataParser.validate(content);
                    vdbList.add(arg);
                } catch (IOException | SAXException e) {
                    ddlList.add(arg);
                }    
            }
        }
        
        vdbs = vdbList.toArray(new String[vdbList.size()]);
        ddls = ddlList.toArray(new String[ddlList.size()]);
        
        application.setBanner(new Banner() {

            @Override
            public void printBanner(Environment environment, Class<?> sourceClass, PrintStream printStream) {
                
                printStream.println(AnsiOutput.toString(
                        AnsiColor.GREEN, "Spring Boot", 
                        AnsiColor.DEFAULT, " ", 
                        AnsiStyle.FAINT, SpringBootVersion.getVersion() == null ? "" : " (v" + SpringBootVersion.getVersion() + ")",
                        AnsiColor.DEFAULT, ", ",
                        AnsiColor.GREEN, "Teiid Embedded", 
                        AnsiColor.DEFAULT, " ", 
                        AnsiStyle.FAINT, "(v" +ApplicationInfo.getInstance().getReleaseNumber() + ")"));
            }});
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {
    }

    @Override
    public void finished(ConfigurableApplicationContext context, Throwable exception) {
        
        if(exception != null) {
            LogManager.logError(CTX_EMBEDDED, exception);
        }

        embeddedServer = context.getBean(EmbeddedServer.class);
        
        for(String vdb : vdbs) {
            try (InputStream is = Files.newInputStream(Paths.get(vdb), StandardOpenOption.READ)) {
                embeddedServer.deployVDB(is);
            } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                new TeiidRuntimeException(TeiidEmbeddedPlugin.Event.TEIID42002, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, vdb));
            }
        }
        
        for(String ddl : ddls) {
            try (InputStream is = Files.newInputStream(Paths.get(ddl), StandardOpenOption.READ)) {
                embeddedServer.deployVDB(is, true);
            } catch (IOException | VirtualDatabaseException | ConnectorManagerException | TranslatorException e) {
                new TeiidRuntimeException(TeiidEmbeddedPlugin.Event.TEIID42002, e, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42002, ddl));
            }
        }
        
        // block spring application exit
        startDaemonAwaitThread();
    }

    private void startDaemonAwaitThread() {

        Thread awaitThread = new Thread(() -> {
            try {
                while(!stopAwait) {
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                throw new TeiidRuntimeException(TeiidEmbeddedPlugin.Event.TEIID42003, e);
            }
        });
        
        awaitThread.setName("awaitDaemon");
        awaitThread.setContextClassLoader(getClass().getClassLoader());
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
    
    public void stopAwait() {
        this.stopAwait = true;
    }

    @Override
    public int getOrder() {
        return 5; 
    }

}
