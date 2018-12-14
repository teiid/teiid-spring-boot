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
DROP TABLE IF EXISTS CUSTOMER;
CREATE TABLE CUSTOMER
(
   SSN char(10),
   FIRSTNAME varchar(64),
   LASTNAME varchar(64),
   ST_ADDRESS varchar(256),
   APT_NUMBER varchar(32),
   CITY varchar(64),
   STATE varchar(32),
   ZIPCODE varchar(10),
   PHONE varchar(15),
   CONSTRAINT CUSTOMER_PK PRIMARY KEY(SSN)
);

DROP TABLE IF EXISTS ACCOUNT;
CREATE TABLE ACCOUNT
(
   ACCOUNT_ID integer,
   SSN char(10),
   STATUS char(10),
   TYPE char(10),
   DATEOPENED timestamp,
   DATECLOSED timestamp,
   CONSTRAINT ACCOUNT_PK PRIMARY KEY(ACCOUNT_ID),
   CONSTRAINT CUSTOMER_FK FOREIGN KEY (SSN) REFERENCES CUSTOMER (SSN)
);

DROP TABLE IF EXISTS PRODUCT;
CREATE TABLE PRODUCT (
   ID integer,
   SYMBOL varchar(16),
   COMPANY_NAME varchar(256),
   CONSTRAINT PRODUCT_PK PRIMARY KEY(ID)
);

DROP TABLE IF EXISTS HOLDINGS;
CREATE TABLE HOLDINGS
(
   TRANSACTION_ID integer IDENTITY,
   ACCOUNT_ID integer,
   PRODUCT_ID integer,
   PURCHASE_DATE timestamp,
   SHARES_COUNT integer,
   CONSTRAINT HOLDINGS_PK PRIMARY KEY (TRANSACTION_ID),
   CONSTRAINT ACCOUNT_FK FOREIGN KEY (ACCOUNT_ID) REFERENCES ACCOUNT (ACCOUNT_ID),
   CONSTRAINT PRODUCT_FK FOREIGN KEY (PRODUCT_ID) REFERENCES PRODUCT (ID)
);