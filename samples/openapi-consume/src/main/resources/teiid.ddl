CREATE DATABASE petstore OPTIONS (ANNOTATION 'Petstore VDB');
USE DATABASE petstore;

-- For OpenAPI replace below to openapi. Any 2.0 need to use swagger, > 3.0 openapi as translator 
CREATE SERVER myapi FOREIGN DATA WRAPPER swagger;

CREATE SCHEMA petapi SERVER myapi;
CREATE VIRTUAL SCHEMA virt;

SET SCHEMA petapi;

-- note: for swagger, it must be swagger.json, for openapi it can be endpoint you configured
IMPORT FOREIGN SCHEMA "foo" FROM SERVER myapi INTO petapi OPTIONS ("importer.metadataUrl" 'swagger.json');

SET SCHEMA virt;

CREATE VIEW Pet (name string, status string, category_name string) 
 	AS select x.name, x.status, x.category_name from (exec petapi.getPetById(10)) as x 
