-- db: test
CREATE DATABASE test;
\c test
CREATE TABLE customers (
    id bigint NOT NULL,
    name character varying(25),
    ssn character varying(25)
);

INSERT INTO Customers values (1, 'christian', '999-99-9999');
INSERT INTO Customers values (2, 'john', '888-88-8888');

-- db customer
CREATE DATABASE customer;
\c customer
CREATE TABLE customer (
    id bigint NOT NULL,
    name character varying(25),
    ssn character varying(25)
);

CREATE TABLE address (
    id bigint NOT NULL,
    street character varying(25),
    customer_id bigint
);
INSERT INTO Customer values (1, 'claire', '777-77-7777');