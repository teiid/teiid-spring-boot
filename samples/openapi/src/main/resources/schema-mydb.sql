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
SET SCHEMA petstore;

DROP TABLE IF EXISTS Category;
CREATE TABLE Category
(
    id bigint,
    name varchar(25),
    CONSTRAINT PK PRIMARY KEY(id)
);

DROP TABLE IF EXISTS Tag;
CREATE TABLE Tag
(
    id bigint,
    name varchar(25),
    CONSTRAINT PK PRIMARY KEY(id)
);

DROP TABLE IF EXISTS Pet;
CREATE TABLE Pet
(
    id bigint,
    name varchar(50),
    category_id bigint,
    tag_id bigint[],
    status char(10),
    photo_url_id bigint[],
    CONSTRAINT PK PRIMARY KEY(id),
    CONSTRAINT CAT_FK FOREIGN KEY (category_id) REFERENCES Category (ID)
);

DROP TABLE IF EXISTS Photo;
CREATE TABLE Photo
(
    id bigint,
    content blob, 
    CONSTRAINT PK PRIMARY KEY(id)
);
