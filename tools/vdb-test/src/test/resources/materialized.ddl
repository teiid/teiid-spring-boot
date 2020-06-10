/*
###########################################
# START DATABASE customer
###########################################
*/
CREATE DATABASE customer VERSION '1' OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE customer VERSION '1';

--############ Translators ############
CREATE FOREIGN DATA WRAPPER mypostgresql TYPE postgresql OPTIONS (supportsOrderBy 'true');


--############ Servers ############
CREATE SERVER cacheStore FOREIGN DATA WRAPPER "infinispan-hotrod";

CREATE SERVER ispn TYPE 'NONE' FOREIGN DATA WRAPPER "infinispan-hotrod";

CREATE SERVER oldsoapy FOREIGN DATA WRAPPER ws;

CREATE SERVER s3 FOREIGN DATA WRAPPER "amazon-s3";

CREATE SERVER sampleansi FOREIGN DATA WRAPPER "jdbc-ansi";

CREATE SERVER samplecassandra FOREIGN DATA WRAPPER cassandra;

CREATE SERVER sampledb FOREIGN DATA WRAPPER sqlserver;

CREATE SERVER samplefile FOREIGN DATA WRAPPER file;

CREATE SERVER sampleftp FOREIGN DATA WRAPPER ftp;

CREATE SERVER samplegoogle FOREIGN DATA WRAPPER "google-spreadsheet";

CREATE SERVER samplemango FOREIGN DATA WRAPPER mongodb;

CREATE SERVER sampleodata FOREIGN DATA WRAPPER odata4;

CREATE SERVER sampleoracle FOREIGN DATA WRAPPER mypostgresql;

CREATE SERVER samplesf FOREIGN DATA WRAPPER salesforce;

CREATE SERVER soapyCountry FOREIGN DATA WRAPPER soap;


--############ Schemas ############
CREATE SCHEMA accounts SERVER sampledb;

CREATE VIRTUAL SCHEMA portfolio;

CREATE SCHEMA materialized_1 SERVER cacheStore OPTIONS (VISIBLE 'false');


--############ Schema:accounts ############
SET SCHEMA accounts;

CREATE FOREIGN TABLE G1 (
	e1 string,
	e2 integer
);

CREATE FOREIGN TABLE G2 (
	e1 string,
	e2 integer
);
--############ Schema:portfolio ############
SET SCHEMA portfolio;

CREATE VIEW CustomerZip (
	id long,
	name string,
	ssn string,
	zip string,
	PRIMARY KEY(id)
) OPTIONS (MATERIALIZED TRUE, MATERIALIZED_TABLE 'materialized_1.customer_portfolio_CustomerZip', "teiid_rel:ALLOW_MATVIEW_MANAGEMENT" 'true', "teiid_rel:MATVIEW_LOADNUMBER_COLUMN" 'LoadNumber', "teiid_rel:MATVIEW_STATUS_TABLE" 'materialized_1.customer_materialized_1_status', "teiid_rel:MATVIEW_TTL" '300000')
AS
SELECT c.ID AS id, c.NAME AS name, c.SSN AS ssn, a.ZIP AS zip FROM accounts.CUSTOMER AS c LEFT OUTER JOIN accounts.ADDRESS AS a ON c.ID = a.CUSTOMER_ID;

CREATE VIRTUAL PROCEDURE g1Table(IN p1 integer, IN p2 string) RETURNS TABLE (xml_out xml)
OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'GET', "REST:URI" 'g1/{p1}')
AS
BEGIN
SELECT XMLELEMENT(NAME g1Table.p1, XMLATTRIBUTES(g1Table.p1 AS p1), XMLAGG(XMLELEMENT(NAME "row", XMLFOREST(e1, e2)))) AS xml_out FROM accounts.G1;
END;

CREATE VIRTUAL PROCEDURE g2Table() RETURNS TABLE (xml_out string)
OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'GET', "REST:URI" 'g2')
AS
BEGIN
SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' AS xml_out;
END;

CREATE VIRTUAL PROCEDURE g3Table(IN p1 integer, IN p2 json) RETURNS TABLE (xml_out json)
OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'POST', "REST:URI" 'g3')
AS
BEGIN
SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' AS xml_out;
END;
--############ Schema:materialized_1 ############
SET SCHEMA materialized_1;

CREATE FOREIGN TABLE customer_materialized_1_status (
	VDBName string(50) NOT NULL,
	VDBVersion string(50) NOT NULL,
	SchemaName string(50) NOT NULL,
	Name string(256) NOT NULL,
	TargetSchemaName string(50) NOT NULL,
	TargetName string(256) NOT NULL,
	Valid boolean NOT NULL,
	LoadState string(25) NOT NULL,
	Cardinality long,
	Updated timestamp NOT NULL,
	LoadNumber long NOT NULL,
	NodeName string(25) NOT NULL,
	StaleCount long,
	PRIMARY KEY(VDBName, VDBVersion, SchemaName, Name)
) OPTIONS (UPDATABLE TRUE, "teiid_ispn:cache" 'customer_materialized_1_status');

CREATE FOREIGN TABLE customer_portfolio_CustomerZip (
	id long,
	name string,
	ssn string,
	zip string,
	LoadNumber long,
	PRIMARY KEY(id)
) OPTIONS (UPDATABLE TRUE, "teiid_ispn:cache" 'customer_portfolio_CustomerZip');IMPORT FROM SERVER cacheStore INTO materialized_1;

/*
###########################################
# END DATABASE customer
###########################################
*/