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
CREATE SERVER sampledb FOREIGN DATA WRAPPER sqlserver;

CREATE SERVER samplemango FOREIGN DATA WRAPPER mongodb;

CREATE SERVER samplesf FOREIGN DATA WRAPPER salesforce;

CREATE SERVER ispn TYPE 'NONE' FOREIGN DATA WRAPPER "infinispan-hotrod";

CREATE SERVER sampleodata FOREIGN DATA WRAPPER odata4;

CREATE SERVER samplefile FOREIGN DATA WRAPPER file;

CREATE SERVER samplegoogle FOREIGN DATA WRAPPER "google-spreadsheet";

CREATE SERVER s3 FOREIGN DATA WRAPPER "amazon-s3";

-- create schema, then import the metadata from the PostgreSQL database
CREATE SCHEMA accounts SERVER sampledb;
CREATE VIRTUAL SCHEMA portfolio;

SET SCHEMA accounts;
CREATE FOREIGN TABLE G1 (e1 string, e2 integer);
CREATE FOREIGN TABLE G2 (e1 string, e2 integer);

SET SCHEMA portfolio;

CREATE VIEW CustomerZip(id bigint PRIMARY KEY, name string, ssn string, zip string) AS 
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
