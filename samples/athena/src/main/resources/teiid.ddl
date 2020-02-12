CREATE DATABASE customer OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE customer;

CREATE SERVER mydb TYPE 'NONE' FOREIGN DATA WRAPPER "amazon-athena"; 

CREATE SCHEMA accounts SERVER mydb;

SET SCHEMA accounts;
IMPORT FOREIGN SCHEMA sampledb FROM SERVER mydb INTO accounts OPTIONS("importer.useFullSchemaName" 'TABLE,VIEW,EXTERNAL_TABLE', 
    "importer.useCatalogName" 'false', "importer.useQualifiedName" 'true');
