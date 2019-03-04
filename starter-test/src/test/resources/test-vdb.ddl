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

CREATE FOREIGN DATA WRAPPER fake;
CREATE FOREIGN DATA WRAPPER fake2;
CREATE FOREIGN DATA WRAPPER fake3 TYPE fake OPTIONS ("myProperty" 'foo');

CREATE SERVER fakeSource TYPE 'NONE' FOREIGN DATA WRAPPER fake OPTIONS ("jndi-name" 'fakeSource');
CREATE SERVER fakeSource2 TYPE 'NONE' FOREIGN DATA WRAPPER fake2 OPTIONS ("jndi-name" 'fakeSource2');
CREATE SERVER fakeSource3 TYPE 'NONE' FOREIGN DATA WRAPPER fake3 OPTIONS ("jndi-name" 'fakeSource3');

CREATE SCHEMA accounts SERVER fakeSource;
CREATE SCHEMA accounts2 SERVER fakeSource2;
CREATE SCHEMA accounts3 SERVER fakeSource3;

SET SCHEMA accounts;
IMPORT FOREIGN SCHEMA public FROM SERVER fakeSource INTO accounts OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA accounts2;
IMPORT FOREIGN SCHEMA public FROM SERVER fakeSource2 INTO accounts2 OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA accounts3;
IMPORT FOREIGN SCHEMA public FROM SERVER fakeSource3 INTO accounts3 OPTIONS("importer.useFullSchemaName" 'false');
