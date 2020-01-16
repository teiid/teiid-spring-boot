CREATE DATABASE customer OPTIONS (ANNOTATION 'Customer VDB');
USE DATABASE customer;

CREATE SCHEMA foo SERVER mydb;
SET SCHEMA foo;

CREATE FOREIGN TABLE FOO(name string, id integer);

CREATE SCHEMA bar SERVER mydb;
SET SCHEMA bar;

CREATE FOREIGN TABLE BAR(name string, id integer);