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

package org.jboss.teiid.springboot.sample;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.h2.tools.RunScript;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;
import org.teiid.embedded.helper.EmbeddedHelper;
import org.teiid.embedded.helper.utils.JDBCUtils;
import org.teiid.security.SecurityHelper;

@SpringBootApplication
public class ExampleMain {
    
    @Bean
    public Map<String, DataSource> account() throws ResourceException {
        
        Map<String, DataSource> datasources = new HashMap<>(1); 
        DataSource ds = EmbeddedHelper.Factory.newDataSource(c -> c.localManagedConnectionFactory(mcf -> {
            mcf.setDriverClass("org.h2.Driver");
            mcf.setConnectionURL(URL);
            mcf.setUserName("sa");
            mcf.setPassword("sa");
        }).poolConfiguration(p -> {
            p.setMaxSize(30);
            p.setMinSize(5);
            p.setBlockingTimeout(30000);
            p.setIdleTimeoutMinutes(10);
        }));
        
        datasources.put("accounts-ds", ds);
        return datasources;
    }
    
    @Bean
    public Map<String, ConnectionFactory> marketData() throws ResourceException {
        Map<String, ConnectionFactory> factories = new HashMap<>();
        ConnectionFactory cf = EmbeddedHelper.Factory.fileConnectionFactory(mcf -> mcf.setParentDirectory(marketdataDir));
        factories.put("marketdata-file", cf);
        return factories;
    }
    
    @Bean
    public TransactionManager transactionManager() {
        return  EmbeddedHelper.Factory.transactionManager(c -> c.coreEnvironmentBean(core -> {
            core.setSocketProcessIdPort(0);
            core.setSocketProcessIdMaxPorts(10);
        }).coordinatorEnvironmentBean(coordinator -> {
            coordinator.setEnableStatistics(false);
            coordinator.setDefaultTimeout(300);
            coordinator.setTransactionStatusManagerEnable(false);
            coordinator.setTxReaperCancelFailWaitPeriod(120000);
        }).objectStoreEnvironmentBean(objectStore -> {
            objectStore.setObjectStoreDir(System.getProperty("java.io.tmpdir") + "/narayana");
        }));
    }
    
    @Bean
    public SecurityHelper securityHelper() {
        return new EmbeddedSecurityHelper();
    }
    
    private static String URL = "jdbc:h2:file:" + System.getProperty("java.io.tmpdir") + "/test";
    private static String marketdataDir;

    public static void main(String[] args) throws Exception {
        prepareSampleData();
        SpringApplication.run(ExampleMain.class, args);
    }
    
    private static void prepareSampleData() throws Exception {
        
        Connection conn = JDBCUtils.getDriverConnection("org.h2.Driver", URL, "sa", "sa");
        RunScript.execute(conn, new InputStreamReader(ExampleMain.class.getClassLoader().getResourceAsStream("teiidfiles/customer-schema.sql")));
        JDBCUtils.close(conn);

        Path target = Paths.get(System.getProperty("java.io.tmpdir"), "teiidfiles");
        marketdataDir = target.toString();
        if(!Files.exists(target)){
            Files.createDirectories(target);
        }
        target.toFile().deleteOnExit();
        Files.copy(ExampleMain.class.getClassLoader().getResourceAsStream("teiidfiles/data/marketdata-price.txt"), target.resolve("marketdata-price.txt"), REPLACE_EXISTING);
        Files.copy(ExampleMain.class.getClassLoader().getResourceAsStream("teiidfiles/data/marketdata-price1.txt"), target.resolve("marketdata-price1.txt"), REPLACE_EXISTING);

    }


}
