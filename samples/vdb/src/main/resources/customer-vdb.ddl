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

CREATE DATABASE customer OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE customer;
CREATE FOREIGN DATA WRAPPER h2;
CREATE SERVER mydb TYPE 'NONE' FOREIGN DATA WRAPPER h2 OPTIONS ("jndi-name" 'mydb');
CREATE VIRTUAL SCHEMA virt;
CREATE SCHEMA accounts SERVER mydb;

-- H2 converts the schema name to capital case
IMPORT FOREIGN SCHEMA MYSCHEMA FROM SERVER mydb INTO accounts OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA accounts;
CREATE FOREIGN FUNCTION REPEAT (x string, y integer) RETURNS string 
    OPTIONS (JAVA_CLASS 'org.teiid.spring.example.UserFunctions',
             JAVA_METHOD 'repeat'); 


SET SCHEMA virt;
CREATE VIRTUAL FUNCTION addSalutation(param1 string) RETURNS string
    OPTIONS (NAMEINSOURCE 'addSalutation', JAVA_CLASS 'org.teiid.spring.example.UserFunctions', 
    JAVA_METHOD 'addSalutation');