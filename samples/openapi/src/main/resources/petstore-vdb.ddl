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

CREATE SERVER mydb FOREIGN DATA WRAPPER h2;

CREATE VIRTUAL SCHEMA pets;
CREATE SCHEMA petdb SERVER mydb;

-- H2 converts the schema name to capital case
IMPORT FOREIGN SCHEMA PETSTORE FROM SERVER mydb INTO petdb OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA pets;

CREATE VIRTUAL PROCEDURE addPet(IN body json) OPTIONS (UPDATECOUNT 1)AS
BEGIN
   declare integer _petId =  convert(jsonPathValue(body, '$.id', true), integer);
   declare string _name =  jsonPathValue(body, '$.name', true);
   declare string _status =  jsonPathValue(body, '$.status', true);
   
   INSERT INTO petdb.Pet(id, name, status) VALUES (_petId, _name, _status); 
   
   /*
   LOOP ON (SELECT j.id, j.name, j.status FROM JSONTABLE(body, '$', false COLUMNS id integer, name string, status string) as j) AS x
   BEGIN
       ERROR '**********************values ' || x.id || ',' || x.name;
       INSERT INTO petdb.Pet(id, name, status) VALUES (x.id, x.name, x.status);
   END
   */
END

CREATE VIRTUAL PROCEDURE updatePet(IN body json) OPTIONS (UPDATECOUNT 1)AS
BEGIN
   declare integer _petId =  convert(jsonPathValue(body, '$.id', true), integer);
   declare string _name =  jsonPathValue(body, '$.name', true);
   declare string _status =  jsonPathValue(body, '$.status', true);

   update petdb.Pet SET name =  _name, status = _status where id = _petId;
END

CREATE VIRTUAL PROCEDURE findPetsByStatus(IN status string[]) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    declare json x = (SELECT JSONARRAY_AGG(JSONOBJECT(p.id, p.name, p.status)) 
        FROM petdb.Pet p WHERE p.status in (status));
    return x;
END

CREATE VIRTUAL PROCEDURE getPetById(IN petId integer) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    declare json x = (SELECT JSONOBJECT(id, name, status) FROM petdb.Pet where id = petId);
    return x;
END

CREATE VIRTUAL PROCEDURE updatePetWithForm(IN petId integer, IN name string, IN status string) OPTIONS (UPDATECOUNT 1)AS
BEGIN
    update petdb.Pet SET name =  updatePetWithForm.name, status=updatePetWithForm.status where id = updatePetWithForm.petId;
END

CREATE VIRTUAL PROCEDURE deletePet(IN petId integer, IN apiKey string) OPTIONS (UPDATECOUNT 0)AS
BEGIN
    delete from petdb.Pet where id = deletePet.petId;
END

CREATE VIRTUAL PROCEDURE uploadFile(IN petId integer, IN additionalMetadata string, IN file blob) RETURNS json OPTIONS (UPDATECOUNT 1)AS
BEGIN
    UPSERT INTO Photo (pet_id, content) values (petId, file);
    return jsonParse('{"code": 9,"message": "image updated", "type": "string"}', true);
END
