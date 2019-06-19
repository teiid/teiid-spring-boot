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
CREATE DATABASE PetStore OPTIONS (ANNOTATION 'PetStore VDB');
USE DATABASE PetStore;

SET NAMESPACE 'http://teiid.org/rest' AS REST;

-- create translators and connections to source
CREATE FOREIGN DATA WRAPPER postgresql;
CREATE SERVER sampledb TYPE 'NONE' FOREIGN DATA WRAPPER postgresql OPTIONS ("jndi-name" 'sampledb');

CREATE VIRTUAL SCHEMA pets;
SET SCHEMA pets;


CREATE VIRTUAL PROCEDURE addPet(IN body json) RETURNS TABLE (json_out json) OPTIONS (UPDATECOUNT 0)AS
BEGIN
    SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' as json_out;
END
