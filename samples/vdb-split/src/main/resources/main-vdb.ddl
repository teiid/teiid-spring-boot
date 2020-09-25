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
CREATE SERVER mydb FOREIGN DATA WRAPPER h2;
CREATE VIRTUAL SCHEMA virt;
CREATE SCHEMA accounts SERVER mydb;

-- H2 converts the schema name to capital case
IMPORT FOREIGN SCHEMA MYSCHEMA FROM SERVER mydb INTO accounts OPTIONS("importer.useFullSchemaName" 'false');

IMPORT FROM REPOSITORY "DDL-FILE" INTO accounts OPTIONS ("ddl-file" 'accounts/accounts.ddl');
IMPORT FROM REPOSITORY "DDL-FILE" INTO virt OPTIONS ("ddl-file" 'virt/virt.ddl');
