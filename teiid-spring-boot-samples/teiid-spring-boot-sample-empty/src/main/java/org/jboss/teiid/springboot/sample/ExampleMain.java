package org.jboss.teiid.springboot.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExampleMain {

    public static void main(String[] args) {
        args = new String[]{"src/main/resources/empty-vdb.xml"};
        SpringApplication.run(ExampleMain.class, args);
    }

}
