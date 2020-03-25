CREATE DATABASE "mat-example";
        USE DATABASE "mat-example";
        
        CREATE SERVER web FOREIGN DATA WRAPPER rest;
        CREATE SERVER ispn FOREIGN DATA WRAPPER "infinispan-hotrod";

        CREATE SCHEMA vix_source SERVER web;
        CREATE SCHEMA vix_mat SERVER ispn;
        CREATE VIRTUAL SCHEMA vix_virt;

        IMPORT FROM server web into vix_source;

        SET SCHEMA vix_mat;
        
        CREATE FOREIGN TABLE vixcache (
            "date" date primary key,
            "open" double, 
            "high" double,
            "low" double,
            "close" double,
            MA10 double,
            loadNumber long
        ) OPTIONS(UPDATABLE true, "teiid_ispn:cache" 'vixcache');
        
        CREATE FOREIGN TABLE status (
            VDBName varchar(50) not null,
            VDBVersion varchar(50) not null,
            SchemaName varchar(50) not null,
            Name varchar(256) not null,
            TargetSchemaName varchar(50),
            TargetName varchar(256) not null,
            Valid boolean not null,
            LoadState varchar(25) not null,
            Cardinality long,
            Updated timestamp not null,
            LoadNumber long not null,
            NodeName varchar(25) not null,
            StaleCount long,
            PRIMARY KEY (VDBName, VDBVersion, SchemaName, Name)
        ) OPTIONS(UPDATABLE true, "teiid_ispn:cache" 'status');
        
        IMPORT FROM server ispn into vix_mat;

        SET SCHEMA vix_virt;

        CREATE VIEW vix_raw (
            "date" date primary key,
            "open" double, 
            "high" double,
            "low" double,
            "close" double,
            MA10 double
        ) AS 
            select t.*, AVG("close") OVER (ORDER BY "date" ASC ROWS 9 PRECEDING) AS MA10 from 
              (call vix_source.invokeHttp(action=>'GET', endpoint=>'https://datahub.io/core/finance-vix/r/vix-daily.csv')) w, 
              texttable(to_chars(w.result, 'ascii') COLUMNS "date" date, "open" HEADER 'Vix Open' double, "high" HEADER 'Vix High' double, "low" HEADER 'Vix Low' double, "close" HEADER 'Vix Close' double HEADER) t;
              
        /* materialized vix data.  this could be combined with vix_raw, but
         * it makes demonstrating the difference easier with odata to have two views.
         */
        CREATE VIEW vix (
            "date" date primary key,
            "open" double, 
            "high" double,
            "low" double,
            "close" double,
            MA10 double
        ) OPTIONS (
            MATERIALIZED 'TRUE',
            MATERIALIZED_TABLE 'vix_mat.vixcache',
            "teiid_rel:ALLOW_MATVIEW_MANAGEMENT" 'true',
            "teiid_rel:MATVIEW_LOADNUMBER_COLUMN" 'LoadNumber',
            "teiid_rel:MATVIEW_STATUS_TABLE" 'vix_mat.status'
        ) AS 
            select * from vix_raw;