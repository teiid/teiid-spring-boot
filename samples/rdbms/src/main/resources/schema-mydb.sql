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
   ID bigint,
   SSN char(25),
   NAME varchar(64),
   CONSTRAINT CUSTOMER_PK PRIMARY KEY(ID)
);

DROP TABLE IF EXISTS ADDRESS;
CREATE TABLE ADDRESS
(
   ID bigint,
   STREET char(25),
   ZIP char(10),
   CUSTOMER_ID bigint,
   CONSTRAINT ADDRESS_PK PRIMARY KEY(ID),
   CONSTRAINT CUSTOMER_FK FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMER (ID)
);

DROP TABLE IF EXISTS IDGENERATOR;
CREATE TABLE IDGENERATOR 
(
    IDKEY char(10) NOT NULL,
    IDVALUE bigint NOT NULL
);
-- H2 even though you ask to start at 200, in this example it always starting at 1
CREATE SEQUENCE IF NOT EXISTS customer_seq START WITH 200 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS addr_seq START WITH 200 INCREMENT BY 1;
