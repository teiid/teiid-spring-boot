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
CREATE SERVER mysite FOREIGN DATA WRAPPER file;
CREATE SCHEMA accounts SERVER mysite;

IMPORT FOREIGN SCHEMA MYSCHEMA FROM SERVER mysite INTO accounts OPTIONS("importer.useFullSchemaName" 'false');

SET SCHEMA accounts;

CREATE VIEW stock_price (
	symbol string(255) NOT NULL AUTO_INCREMENT,
	price double NOT NULL,
	PRIMARY KEY(symbol)
)
AS
SELECT
tt.symbol, tt.price
FROM (EXEC accounts.getTextFiles('/user/aditya/stock.txt')) AS f,
TEXTTABLE(f.file COLUMNS symbol string, price double  HEADER) AS tt;

