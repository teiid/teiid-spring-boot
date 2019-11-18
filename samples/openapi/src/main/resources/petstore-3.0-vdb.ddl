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

CREATE DATABASE PetStore OPTIONS (ANNOTATION 'PetStore VDB');
USE DATABASE PetStore;

CREATE FOREIGN DATA WRAPPER h2;
CREATE SERVER mydb FOREIGN DATA WRAPPER h2;

CREATE VIRTUAL SCHEMA pets;
CREATE SCHEMA petdb SERVER mydb;

-- H2 converts the schema name to capital case
IMPORT FOREIGN SCHEMA PETSTORE FROM SERVER mydb INTO petdb OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA pets;

CREATE VIRTUAL PROCEDURE createPets(IN pet json) OPTIONS (UPDATECOUNT 1)AS
BEGIN  
   LOOP ON (SELECT j.id, j.name, j.status FROM JSONTABLE(pet, '$', false COLUMNS id integer, name string, status string) as j) AS x
   BEGIN
       INSERT INTO petdb.Pet(id, name, status) VALUES (x.id, x.name, x.status);
   END
END

CREATE VIRTUAL PROCEDURE showPetById(IN petId integer) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    declare json x = (SELECT JSONOBJECT(id, name, status) FROM petdb.Pet where id = petId);
    return x;
END

CREATE VIRTUAL PROCEDURE listPets(IN "limit" integer) RETURNS json OPTIONS (UPDATECOUNT 0)AS
BEGIN
    declare json x = (SELECT JSONARRAY_AGG(JSONOBJECT(p.id, p.name, p.status)) 
        FROM petdb.Pet p WHERE p.status in (status));
    return x;
END
