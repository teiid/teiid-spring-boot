/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.autoconfigure;

import static org.junit.Assert.assertEquals;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBNAME;
import static org.teiid.spring.autoconfigure.TeiidConstants.VDBVERSION;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.teiid.adminapi.Model;
import org.teiid.adminapi.impl.ModelMetaData;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.query.metadata.DDLStringVisitor;
import org.teiid.query.metadata.SystemMetadata;

public class TestModelGenerator {
    private TeiidServer server;
    private ApplicationContext context;

    @Before
    public void setup() {
        this.server = Mockito.mock(TeiidServer.class);
        this.context = Mockito.mock(ApplicationContext.class);
        Environment env = Mockito.mock(Environment.class);
        Mockito.stub(env.getProperty(Mockito.anyString())).toReturn(null);
        Mockito.stub(context.getEnvironment()).toReturn(env);
    }

    private ModelMetaData buildSourceTable() {
        ModelMetaData model = new ModelMetaData();
        model.setName("source");
        model.setModelType(Model.Type.PHYSICAL);
        MetadataFactory target = new MetadataFactory(VDBNAME, VDBVERSION,
                SystemMetadata.getInstance().getRuntimeTypeMap(), model);
        Table t = target.addTable("Person");
        target.addColumn("id", "integer", t);
        target.addColumn("name", "string", t);
        target.addColumn("dob", "date", t);
        model.addAttchment(MetadataFactory.class, target);
        String ddl = DDLStringVisitor.getDDLString(target.getSchema(), null, null);
        model.addSourceMetadata("ddl", ddl);
        return model;
    }

    private ModelMetaData buildSourceTableWithPK() {
        ModelMetaData model = new ModelMetaData();
        model.setName("source");
        model.setModelType(Model.Type.PHYSICAL);
        MetadataFactory target = new MetadataFactory(VDBNAME, VDBVERSION,
                SystemMetadata.getInstance().getRuntimeTypeMap(), model);
        Table t = target.addTable("Person");
        target.addColumn("id", "integer", t);
        target.addColumn("name", "string", t);
        target.addColumn("dob", "date", t);
        target.addPrimaryKey("PK", Arrays.asList("id"), t);

        Table addr = target.addTable("address");
        target.addColumn("id", "integer", addr);
        target.addColumn("street", "string", addr);
        target.addColumn("pid", "integer", addr);
        target.addPrimaryKey("PK", Arrays.asList("id"), addr);
        target.addForeignKey("FK", Arrays.asList("pid"), Arrays.asList("id"), "Person", addr);

        String ddl = DDLStringVisitor.getDDLString(target.getSchema(), null, null);
        model.addSourceMetadata("ddl", ddl);
        model.addAttchment(MetadataFactory.class, target);
        return model;
    }

    private ModelMetaData buildSourceTableWithCompositePK() {
        ModelMetaData model = new ModelMetaData();
        model.setName("source");
        model.setModelType(Model.Type.PHYSICAL);
        MetadataFactory target = new MetadataFactory(VDBNAME, VDBVERSION,
                SystemMetadata.getInstance().getRuntimeTypeMap(), model);
        Table t = target.addTable("Person");
        target.addColumn("id", "integer", t);
        target.addColumn("name", "string", t);
        target.addColumn("dob", "date", t);
        target.addPrimaryKey("PK", Arrays.asList("id", "name"), t);
        String ddl = DDLStringVisitor.getDDLString(target.getSchema(), null, null);
        model.addSourceMetadata("ddl", ddl);
        model.addAttchment(MetadataFactory.class, target);
        return model;
    }

    @Test(expected = IllegalStateException.class)
    public void testRedirectionLayer_noPK() {
        RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(this.context, "redirected");
        VDBMetaData vdb = new VDBMetaData();
        vdb.addModel(buildSourceTable());
        mg.buildRedirectionLayer(buildSourceTable().getAttachment(MetadataFactory.class), "base");
    }

    @Test
    public void testRedirectionLayerSelectPlan() {
        RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(this.context, "redirected");
        VDBMetaData vdb = new VDBMetaData();
        vdb.addModel(buildSourceTable());
        ModelMetaData model = mg.buildRedirectionLayer(buildSourceTableWithPK().getAttachment(MetadataFactory.class),
                "base");
        String expected = "SELECT o.id, o.name, o.dob FROM internal.Person AS o LEFT OUTER JOIN "
                + "redirected.Person_REDIRECTED AS m ON (o.id = m.id) WHERE m.ROW__STATUS IS NULL \n" + " UNION ALL \n"
                + "SELECT id, name, dob FROM redirected.Person_REDIRECTED WHERE ROW__STATUS <> 3";

        MetadataFactory mf = model.getAttachment(MetadataFactory.class);
        Table table = mf.getSchema().getTable("Person");
        assertEquals(expected, table.getSelectTransformation());
    }

    @Test
    public void testRedirectionLayerInsertPlan() {
        RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(this.context, "redirected");
        VDBMetaData vdb = new VDBMetaData();
        vdb.addModel(buildSourceTable());
        ModelMetaData model = mg.buildRedirectionLayer(buildSourceTableWithPK().getAttachment(MetadataFactory.class),
                "base");
        String expected = "FOR EACH ROW\n" + "BEGIN ATOMIC\n"
                + "    DECLARE boolean VARIABLES.Person_PK_EXISTS = (SELECT true FROM source.Person WHERE id = NEW.id);\n"
                + "    IF (VARIABLES.Person_PK_EXISTS)\n" + "    BEGIN\n"
                + "        RAISE SQLEXCEPTION 'duplicate key';\n" + "    END\n" + "    ELSE\n" + "    BEGIN\n"
                + "        INSERT INTO redirected.Person_REDIRECTED (id, name, dob, ROW__STATUS) VALUES (NEW.id, NEW.name, NEW.dob, 1);\n"
                + "    END\n" + "END";

        MetadataFactory mf = model.getAttachment(MetadataFactory.class);
        Table table = mf.getSchema().getTable("Person");
        assertEquals(expected, table.getInsertPlan().replace("\t", "    "));
    }

    @Test
    public void testRedirectionLayerUpdatePlan() {
        RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(this.context, "redirected");
        ModelMetaData model = mg.buildRedirectionLayer(buildSourceTableWithPK().getAttachment(MetadataFactory.class),
                "base");
        String expected = "FOR EACH ROW\n" + "BEGIN ATOMIC\n" + "IF (CHANGING.id)\n" + "BEGIN\n"
                + "    DECLARE boolean VARIABLES.Person_PK_EXISTS = (SELECT true FROM source.Person WHERE id = NEW.id);\n"
                + "    IF (VARIABLES.Person_PK_EXISTS)\n" + "    BEGIN\n"
                + "        RAISE SQLEXCEPTION 'duplicate key';\n" + "    END\n" + "    ELSE\n" + "    BEGIN\n"
                + "        DECLARE boolean VARIABLES.address_FK_EXISTS = (SELECT COUNT(*) > 0 FROM teiid.address WHERE pid = OLD.id);\n"
                + "        IF (VARIABLES.address_FK_EXISTS)\n" + "        BEGIN\n"
                + "            RAISE SQLEXCEPTION 'referential integrity check failed on address table, cascade deletes are not supported';\n"
                + "        END\n"
                + "        UPSERT INTO redirected.Person_REDIRECTED(id, ROW__STATUS) VALUES (OLD.id, 3);\n"
                + "        UPSERT INTO redirected.Person_REDIRECTED(id, name, dob, ROW__STATUS) VALUES (NEW.id, NEW.name, NEW.dob, 1);\n"
                + "    END\n" + "END\n" + "ELSE\n" + "BEGIN\n"
                + "    UPSERT INTO redirected.Person_REDIRECTED(id, name, dob, ROW__STATUS) VALUES (NEW.id, NEW.name, NEW.dob, 2);\n"
                + "END\n" + "END";

        MetadataFactory mf = model.getAttachment(MetadataFactory.class);
        Table table = mf.getSchema().getTable("Person");
        assertEquals(expected, table.getUpdatePlan().replace("\t", "    "));
    }

    @Test
    public void testRedirectionLayerDeletePlan() {
        RedirectionSchemaBuilder mg = new RedirectionSchemaBuilder(this.context, "redirected");
        ModelMetaData model = mg.buildRedirectionLayer(buildSourceTableWithPK().getAttachment(MetadataFactory.class),
                "base");
        String expected = "FOR EACH ROW\n" + "BEGIN ATOMIC\n"
                + "        DECLARE boolean VARIABLES.address_FK_EXISTS = (SELECT COUNT(*) > 0 FROM teiid.address WHERE pid = OLD.id);\n"
                + "        IF (VARIABLES.address_FK_EXISTS)\n" + "        BEGIN\n"
                + "            RAISE SQLEXCEPTION 'referential integrity check failed on address table, cascade deletes are not supported';\n"
                + "        END\n"
                + "        UPSERT INTO redirected.Person_REDIRECTED(id, ROW__STATUS) VALUES (OLD.id, 3);\n" + "END";
        MetadataFactory mf = model.getAttachment(MetadataFactory.class);
        Table table = mf.getSchema().getTable("Person");
        assertEquals(expected, table.getDeletePlan().replace("\t", "    "));
    }
}
