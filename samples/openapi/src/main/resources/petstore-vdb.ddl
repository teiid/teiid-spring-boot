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

CREATE DATABASE openapi OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE openapi;

SET NAMESPACE 'http://teiid.org/rest' AS REST;
CREATE FOREIGN DATA WRAPPER h2;
CREATE SERVER mydb TYPE 'NONE' FOREIGN DATA WRAPPER h2 OPTIONS ("jndi-name" 'mydb');

CREATE VIRTUAL SCHEMA pets;
CREATE SCHEMA petdb SERVER mydb;

-- H2 converts the schema name to capital case
IMPORT FOREIGN SCHEMA petstore FROM SERVER mydb INTO petdb OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA pets;

CREATE VIRTUAL PROCEDURE addPet(IN body json) OPTIONS (UPDATECOUNT 1)AS
BEGIN
    declare xml xmlBody = jsontoxml(body);
    insert into petdb.Pet(id, name, category_id, tag_id, status, photo_url_id) 
        values (xpathvalue(xmlBody, '/id'), xpathvalue(xmlBody, '/name'), xpathvalue(xmlBody, '/category/id'), 
            null, xpathvalue(xmlBody, '/status'), null);
   
END

CREATE VIRTUAL PROCEDURE updatePet(IN body json) OPTIONS (UPDATECOUNT 1)AS
BEGIN
    declare xml xmlBody = jsontoxml(body);    
    update petdb.Pet SET name =  xpathvalue(xmlBody, '/name'), category_id = xpathvalue(xmlBody, '/category/id'), 
        status=xpathvalue(xmlBody, '/status') where id = xpathvalue(xmlBody, '/id');
END

CREATE VIRTUAL PROCEDURE findPetsByStatus(IN status string[]) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    SELECT '{ "age":100, "name":test,messages:["msg1","msg2","msg3"]}' as json_out;
END

CREATE VIRTUAL PROCEDURE getPetById(IN petId integer) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    SELECT JSONOBJECT(id, name, status) FROM petdb.Pet where id = getPetById.petId;
END

CREATE VIRTUAL PROCEDURE updatePetWithForm(IN petId integer, IN name string, IN status string) OPTIONS (UPDATECOUNT 1)AS
BEGIN
    update petdb.Pet SET name =  updatePetWithForm.name, status=updatePetWithForm.status where id = updatePetWithForm.petId;
END

CREATE VIRTUAL PROCEDURE deletePet(IN petId integer, IN api_key string) OPTIONS (UPDATECOUNT 0)AS
BEGIN
    delete from petdb.Pet where id = deletePet.petId;
END

CREATE VIRTUAL PROCEDURE uploadFile(IN petId integer, IN additionalMetadata string, IN file blob) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    -- need to some tasks there
    SELECT JSONOBJECT(200 as code, 'any' as type, 'success' as message);
END
