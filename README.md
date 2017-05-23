# Teiid Spring Boot Starter

Primary purpose of `teiid-spring-boot` is enables Teiid in Spring Boot applications via adding the Maven dependency below to your Spring Boot application pom file.<br>

``` xml
<dependency>
   <groupId>org.teiid</groupId>
   <artifactId>teiid-spring-boot-starter</artifactId>
</dependency>
```
## Features

* Enables Teiid for Spring Boot applications
* Supports translators/connectors auto-detection 
* Enable Externalized .yml/.properties based configuration
* Supports vdb deployment either .sql, or .xml 

## Build
- install JDK 1.8 or higher
- install maven 3.2+ - http://maven.apache.org/download.html
- Create a github account fork

Enter the following:

        $ git clone https://github.com/<yourname>/teiid-spring-boot.git
        $ cd teiid-spring-boot
        $ mvn clean install -s settings.xml

## Useful Links
- Website - http://teiid.org
- Documentation - https://teiid.gitbooks.io/documents/content/
- Documentation Project - https://teiid.gitbooks.io
- JIRA Issues -  https://issues.jboss.org/browse/TEIID
- User Forum - https://community.jboss.org/en/teiid?view=discussions
- Wiki - https://community.jboss.org/wiki/TheTeiidProject


