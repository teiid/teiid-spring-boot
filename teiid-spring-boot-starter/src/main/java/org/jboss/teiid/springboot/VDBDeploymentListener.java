package org.jboss.teiid.springboot;

import static org.jboss.teiid.springboot.TeiidEmbeddedConstants.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.teiid.adminapi.impl.VDBMetadataParser;
import org.teiid.core.TeiidRuntimeException;
import org.teiid.core.util.ApplicationInfo;
import org.teiid.deployers.VirtualDatabaseException;
import org.teiid.dqp.internal.datamgr.ConnectorManagerRepository.ConnectorManagerException;
import org.teiid.logging.LogManager;
import org.teiid.runtime.EmbeddedServer;
import org.teiid.translator.TranslatorException;
import org.xml.sax.SAXException;

public class VDBDeploymentListener implements SpringApplicationRunListener, Ordered {
    
    private final SpringApplication application;

    private final String[] args;
    
    private String[] vdbs;
    
    private EmbeddedServer embeddedServer;
    
    public VDBDeploymentListener(SpringApplication application, String[] args) {
        this.application = application;
        this.args = args;
    }

    @Override
    public void starting() {
        LogManager.logInfo(CTX_EMBEDDED, TeiidEmbeddedPlugin.Util.gs(TeiidEmbeddedPlugin.Event.TEIID42001, ApplicationInfo.getInstance().getReleaseNumber()));

        List<String> vdbList = new ArrayList<>();
        for(String arg : args) {
            Path path = Paths.get(arg);
            if(Files.exists(path)) {
                try (InputStream content = Files.newInputStream(path, StandardOpenOption.READ)) {
                    VDBMetadataParser.validate(content);
                    vdbList.add(arg);
                } catch (IOException | SAXException e) {
                }    
            }
        }
        
        vdbs = vdbList.toArray(new String[vdbList.size()]);
        
        application.setBannerMode(Banner.Mode.OFF);
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
        
        final CountDownLatch closeLatch = context.getBean(CountDownLatch.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> closeLatch.countDown()));
        try {
            closeLatch.await();
        } catch (InterruptedException e) {
            throw new TeiidRuntimeException(e);
        }
        
    }

    @Override
    public int getOrder() {
        return 5; 
    }

}
