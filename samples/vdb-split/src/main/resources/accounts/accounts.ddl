SET SCHEMA accounts;
CREATE FOREIGN FUNCTION REPEAT (x string, y integer) RETURNS string 
    OPTIONS (JAVA_CLASS 'org.teiid.spring.example.UserFunctions',
             JAVA_METHOD 'repeat'); 
