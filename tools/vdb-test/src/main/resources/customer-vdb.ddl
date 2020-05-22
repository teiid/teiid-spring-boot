--
-- Copyright (C) 2016 Red Hat, Inc.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

-- This is simple VDB that connects to a single PostgreSQL database and exposes it 
-- as a Virtual Database.


-- create database  
CREATE DATABASE customer OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE customer;

-- create translators and connections to source
CREATE FOREIGN DATA WRAPPER sqlserver;
CREATE SERVER sampledb FOREIGN DATA WRAPPER sqlserver;

CREATE FOREIGN DATA WRAPPER myorcale handler oracle OPTIONS (supportsOrderBy true);
CREATE SERVER sampleoracle FOREIGN DATA WRAPPER myorcale;

CREATE FOREIGN DATA WRAPPER mongodb;
CREATE SERVER samplemango FOREIGN DATA WRAPPER mongodb;

CREATE SERVER sampleathena FOREIGN DATA WRAPPER "amazon-athena";

CREATE SERVER sampleansi FOREIGN DATA WRAPPER "jdbc-ansi";

CREATE FOREIGN DATA WRAPPER salesforce;
CREATE SERVER samplesf FOREIGN DATA WRAPPER salesforce;

CREATE FOREIGN DATA WRAPPER "infinispan-hotrod";
CREATE SERVER ispn TYPE 'NONE' FOREIGN DATA WRAPPER "infinispan-hotrod";

CREATE SERVER soapyCountry FOREIGN DATA WRAPPER "soap";

CREATE SERVER oldsoapy FOREIGN DATA WRAPPER "ws";

CREATE FOREIGN DATA WRAPPER ftp;
CREATE SERVER sampleftp FOREIGN DATA WRAPPER "ftp";

CREATE FOREIGN DATA WRAPPER odata4;
CREATE SERVER sampleodata FOREIGN DATA WRAPPER odata4;

CREATE FOREIGN DATA WRAPPER file;
CREATE SERVER samplefile FOREIGN DATA WRAPPER file;

CREATE FOREIGN DATA WRAPPER "google-spreadsheet";
CREATE SERVER samplegoogle FOREIGN DATA WRAPPER "google-spreadsheet";

CREATE FOREIGN DATA WRAPPER "amazon-s3";
CREATE SERVER s3 FOREIGN DATA WRAPPER "amazon-s3";

CREATE FOREIGN DATA WRAPPER "cassandra";
CREATE SERVER samplecassandra FOREIGN DATA WRAPPER "cassandra";

-- create schema, then import the metadata from the PostgreSQL database
CREATE SCHEMA accounts SERVER sampledb;
CREATE VIRTUAL SCHEMA portfolio;

SET SCHEMA accounts;
CREATE FOREIGN TABLE G1 (e1 string, e2 integer);
CREATE FOREIGN TABLE G2 (e1 string, e2 integer);

SET SCHEMA portfolio;

CREATE VIEW CustomerZip(id bigint PRIMARY KEY, name string, ssn string, zip string) 
  OPTIONS(MATERIALIZED 'TRUE', "teiid_rel:MATVIEW_TTL" 300000) 
  AS 
    SELECT c.ID as id, c.NAME as name, c.SSN as ssn, a.ZIP as zip 
    FROM accounts.CUSTOMER c LEFT OUTER JOIN accounts.ADDRESS a 
    ON c.ID = a.CUSTOMER_ID;
   
-- path parameter and xml out test
CREATE VIRTUAL PROCEDURE g1Table(IN p1 integer, IN p2 string) RETURNS TABLE (xml_out xml) 
  OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'GET', "REST:URI" 'g1/{p1}') AS
BEGIN
    SELECT XMLELEMENT(NAME g1Table.p1, XMLATTRIBUTES (g1Table.p1 as p1), 
           XMLAGG(XMLELEMENT(NAME "row", XMLFOREST(e1, e2)))) AS xml_out 
    FROM accounts.G1;
END

-- request parameter and xml out test        
CREATE VIRTUAL PROCEDURE g2Table() RETURNS TABLE (xml_out string) 
  OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'GET', "REST:URI" 'g2')AS
BEGIN
    SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' as xml_out;
END

-- test input with lob
CREATE VIRTUAL PROCEDURE g3Table(IN p1 integer, IN p2 json) RETURNS TABLE (xml_out json) 
  OPTIONS (UPDATECOUNT 0, "REST:METHOD" 'POST', "REST:URI" 'g3')AS
BEGIN
    SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' as xml_out;
END
