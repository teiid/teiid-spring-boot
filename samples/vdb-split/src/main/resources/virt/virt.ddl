SET SCHEMA virt;
CREATE VIRTUAL FUNCTION addSalutation(param1 string) RETURNS string
    OPTIONS (NAMEINSOURCE 'addSalutation', JAVA_CLASS 'org.teiid.spring.example.UserFunctions', 
    JAVA_METHOD 'addSalutation');

CREATE VIRTUAL VIEW V_CUSTMER(
  ID long,
  NAME string,
  SSN string
) AS
SELECT id, ADDSALUTATION(name) as name, repeat(ssn, 2) as ssn FROM customer