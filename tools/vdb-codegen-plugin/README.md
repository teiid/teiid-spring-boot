# VDB Maven Plugin

A Maven PlugIn to build a VDB file. Thorntail only allows ZIP archive based artifact deployment when a VDB needs to be deployed with Thorntail Teiid. This Maven plugin has packaging type of "vdb" can be defined in a pom.xml as

````
<modelVersion>4.0.0</modelVersion>
<groupId>com.example</groupId>
<artifactId>teiid-demo</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>vdb</packaging>

<build>
  <plugins>
    <plugin>
      <groupId>org.teiid</groupId>
      <artifactId>vdb-maven-plugin</artifactId>
      <version>1.2</version>
      <extensions>true</extensions>
      <executions>
        <execution>
          <id>test</id>
          <goals>
            <goal>vdb</goal>
          </goals>
          <configuration>
            <!-- your configuration here -->
            <!-- 
            <vdbXmlFile>path/to/vdbfile</vdbXmlFile> <!-- optional -->
            <vdbFolder>path/to/vdbfile</vdbFolder> <!-- optional -->
            -->
          </configuration>          
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
````
The default `vdbXMLFile` is set to "${basedir}/src/main/vdb/META-INF/vdb.xml", if this file is available, then it this file is used as the VDB file. If this file is not there then code will look into `vdbFolder` directory and scan for `-vdb.xml` or `-vdb.ddl` files in this folder or any of sub folders. The very first file found with either of the name is choosen as the VDB file. So, only provide single vdb file for project. Inside the `vdbFolder` one can have any number of other supporting files in same folder or sub folders, they will be copied as is into target archive. Default `vdbFolder` value is `${basedir}/src/main/vdb`. The vdb structure can be like follows.

```
src
  /main
    /vdb
      sample-vdb.xml
      /ddl
         model1.ddl
         model2.ddl
      /misc
        foo.txt 
```

NOTE: the `vdbFolder` is always defined in exploded format, it will NOT work if you copied the legacy Designer based VDB into this folder and expect to convert it. If you have a Designer based vdb, first load that into the your Designer, export as -vdb.xml file, and then copy that file here.

When the build process finishes, it will create zip archive with .vdb extension with all the vdb files and marks it as artifact of the build process.

If your VDB does import other VDBs, then define those VDBs as dependencies in this project. For ex:

```
<dependency>
  <groupId>com.example</groupId>
  <artifactId>another-vdb</artifactId>
  <version>1.0.0</version>
  <type>vdb</type>
</dependency>
```
The build process will pull in the these dependencies and will it a part of the main VDB.

When it finds the -vdb.xml file, 
NOTE: Currently `-vdb.ddl` based vdbs do not support the vdb import feature, it is limited to `-vdb.xml` based vdbs.

# How to do a release
````
git pull upstream master
mvn -DautoVersionSubmodules=true -P release clean package release:prepare
mvn -P release release:perform
````
