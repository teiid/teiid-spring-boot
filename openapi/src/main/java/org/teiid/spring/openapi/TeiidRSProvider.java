/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.openapi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.multipart.MultipartFile;
import org.teiid.adminapi.VDB;
import org.teiid.core.types.BlobImpl;
import org.teiid.core.types.BlobType;
import org.teiid.core.types.ClobImpl;
import org.teiid.core.types.DataTypeManager;
import org.teiid.core.types.InputStreamFactory;
import org.teiid.core.types.JDBCSQLTypeInfo;
import org.teiid.core.types.SQLXMLImpl;
import org.teiid.core.types.Transform;
import org.teiid.core.types.TransformationException;
import org.teiid.core.types.XMLType;
import org.teiid.core.util.Base64;
import org.teiid.core.util.ReaderInputStream;
import org.teiid.core.util.StringUtil;
import org.teiid.jdbc.ConnectionImpl;
import org.teiid.jdbc.LocalProfile;
import org.teiid.jdbc.TeiidDriver;
import org.teiid.net.TeiidURL;
import org.teiid.query.function.source.XMLSystemFunctions;
import org.teiid.query.sql.symbol.XMLSerialize;
import org.teiid.query.sql.visitor.SQLStringVisitor;
import org.teiid.spring.autoconfigure.TeiidServer;

public abstract class TeiidRSProvider {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=\\s*\"?([^\\s;\"]*)");

    private TeiidServer server;
    private VDB vdb;

    public TeiidRSProvider(TeiidServer server, VDB vdb) {
        this.server = server;
        this.vdb = vdb;
    }

    public OpenApiInputStream execute(final String procedureName, final LinkedHashMap<String, Object> parameters,
            final String charSet, final boolean usingReturn) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            LinkedHashMap<String, Object> updatedParameters = convertParameters(conn, procedureName,
                    parameters);
            return executeProc(conn, procedureName, updatedParameters, charSet, usingReturn);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    public OpenApiInputStream executeProc(Connection conn, String procedureName,
            LinkedHashMap<String, Object> parameters, String charSet, boolean usingReturn) throws SQLException {
        // the generated code sends a empty string rather than null.
        if (charSet != null && charSet.trim().isEmpty()) {
            charSet = null;
        }
        Object result = null;
        StringBuilder sb = new StringBuilder();
        sb.append("{ "); //$NON-NLS-1$
        if (usingReturn) {
            sb.append("? = "); //$NON-NLS-1$
        }
        sb.append("CALL ").append(procedureName); //$NON-NLS-1$
        sb.append("("); //$NON-NLS-1$
        boolean first = true;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                sb.append(", "); //$NON-NLS-1$
            }
            first = false;
            sb.append(SQLStringVisitor.escapeSinglePart(entry.getKey())).append("=>?"); //$NON-NLS-1$
        }
        sb.append(") }"); //$NON-NLS-1$

        CallableStatement statement = conn.prepareCall(sb.toString());
        if (!parameters.isEmpty()) {
            int i = usingReturn ? 2 : 1;
            for (Object value : parameters.values()) {
                if (value == null) {
                    continue;
                }
                statement.setObject(i++, value);
            }
        }

        final boolean hasResultSet = statement.execute();
        if (hasResultSet) {
            ResultSet rs = statement.getResultSet();
            if (rs.next()) {
                result = rs.getObject(1);
            } else {
                throw new SQLException("Only result producing procedures are allowed");
            }
        } else if (!usingReturn) {
            throw new SQLException("Only result producing procedures are allowed");
        } else {
            result = statement.getObject(1);
        }
        return handleResult(charSet, result);
    }

    private LinkedHashMap<String, Object> convertParameters(Connection conn, String procedureName,
            LinkedHashMap<String, Object> inputParameters) throws SQLException {

        Map<String, Class<?>> expectedTypes = getParameterTypes(conn, this.vdb.getName(), procedureName);
        LinkedHashMap<String, Object> expectedValues = new LinkedHashMap<String, Object>();
        try {
            for (String columnName : inputParameters.keySet()) {
                Class<?> runtimeType = expectedTypes.get(columnName);
                if (runtimeType == null) {
                    throw new SQLException("Invalid form parameter; No column with name " + columnName
                            + " defined on procedure " + procedureName);
                }
                Object value = inputParameters.get(columnName);
                if (value != null) {
                    if (value instanceof MultipartFile) {
                        value = convertToRuntimeType(runtimeType, (MultipartFile) value);
                    } else if (Array.class.isAssignableFrom(runtimeType)) {
                        List<String> array = StringUtil.split((String) value, ","); //$NON-NLS-1$
                        value = array.toArray(new String[array.size()]);
                    } else if (DataTypeManager.DefaultDataClasses.VARBINARY.isAssignableFrom(runtimeType)) {
                        value = Base64.decode((String) value);
                    } else {
                        if (DataTypeManager.isTransformable(String.class, runtimeType)) {
                            Transform t = DataTypeManager.getTransform(String.class, runtimeType);
                            value = t.transform(value, runtimeType);
                        }
                    }
                }
                expectedValues.put(columnName, value);
            }
            return expectedValues;
        } catch (TransformationException | IOException e) {
            throw new SQLException(e);
        }
    }

    private Object convertToRuntimeType(Class<?> runtimeType, final MultipartFile part)
            throws IOException, SQLException {
        if (SQLXML.class.isAssignableFrom(runtimeType)) {
            SQLXMLImpl xml = new SQLXMLImpl(new InputStreamFactory() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return part.getInputStream();
                }
            });
            if (charset(part) != null) {
                xml.setEncoding(charset(part));
            }
            return xml;
        } else if (Blob.class.isAssignableFrom(runtimeType)) {
            return new BlobImpl(new InputStreamFactory() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return part.getInputStream();
                }
            });
        } else if (Clob.class.isAssignableFrom(runtimeType)) {
            ClobImpl clob = new ClobImpl(new InputStreamFactory() {
                @Override
                public InputStream getInputStream() throws IOException {
                    return part.getInputStream();
                }
            }, -1);
            if (charset(part) != null) {
                clob.setEncoding(charset(part));
            }
            return clob;
        } else if (DataTypeManager.DefaultDataClasses.VARBINARY.isAssignableFrom(runtimeType)) {
            return Base64.decode(new String(part.getBytes()));
        } else if (DataTypeManager.isTransformable(String.class, runtimeType)) {
            try {
                return DataTypeManager.transformValue(new String(part.getBytes()), runtimeType);
            } catch (TransformationException e) {
                throw new SQLException(e);
            }
        }
        return new String(part.getBytes());
    }

    private String charset(final MultipartFile part) {
        if (part == null) {
            return null;
        }
        String contentType = part.getContentType();
        if (contentType == null) {
            return null;
        }

        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }
        return null;
    }

    private LinkedHashMap<String, Class<?>> getParameterTypes(Connection conn, String vdbName, String procedureName)
            throws SQLException {
        String schemaName = procedureName.substring(0, procedureName.lastIndexOf('.')).replace('\"', ' ').trim();
        String procName = procedureName.substring(procedureName.lastIndexOf('.') + 1).replace('\"', ' ').trim();
        LinkedHashMap<String, Class<?>> expectedTypes = new LinkedHashMap<String, Class<?>>();
        try {
            ResultSet rs = conn.getMetaData().getProcedureColumns(vdbName, schemaName, procName, "%"); //$NON-NLS-1$
            while (rs.next()) {
                String columnName = rs.getString(4);
                int columnDataType = rs.getInt(6);
                Class<?> runtimeType = DataTypeManager
                        .getRuntimeType(Class.forName(JDBCSQLTypeInfo.getJavaClassName(columnDataType)));
                expectedTypes.put(columnName, runtimeType);
            }
            rs.close();
            return expectedTypes;
        } catch (ClassNotFoundException e) {
            throw new SQLException(e);
        }
    }

    private OpenApiInputStream handleResult(String charSet, Object result) throws SQLException {
        if (result == null) {
            return null; // or should this be an empty result?
        }

        if (result instanceof SQLXML) {
            if (charSet != null) {
                XMLSerialize serialize = new XMLSerialize();
                serialize.setTypeString("blob"); //$NON-NLS-1$
                serialize.setDeclaration(true);
                serialize.setEncoding(charSet);
                serialize.setDocument(true);
                try {
                    return new OpenApiInputStream(
                            ((BlobType) XMLSystemFunctions.serialize(serialize, new XMLType((SQLXML) result)))
                            .getBinaryStream());
                } catch (TransformationException e) {
                    throw new SQLException(e);
                }
            }
            return new OpenApiInputStream(((SQLXML) result).getBinaryStream());
        } else if (result instanceof Blob) {
            return new OpenApiInputStream(((Blob) result).getBinaryStream());
        } else if (result instanceof Clob) {
            return new OpenApiInputStream(new ReaderInputStream(((Clob) result).getCharacterStream(),
                    charSet == null ? Charset.defaultCharset() : Charset.forName(charSet)));
        }
        return new OpenApiInputStream(new ByteArrayInputStream(
                result.toString().getBytes(charSet == null ? Charset.defaultCharset() : Charset.forName(charSet))));
    }

    public OpenApiInputStream executeQuery(final String sql, boolean json, final boolean passthroughAuth)
            throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            Statement statement = conn.createStatement();
            final boolean hasResultSet = statement.execute(sql);
            Object result = null;
            if (hasResultSet) {
                ResultSet rs = statement.getResultSet();
                if (rs.next()) {
                    result = rs.getObject(1);
                } else {
                    throw new SQLException("Only result producing procedures are allowed");
                }
            }
            return handleResult(Charset.defaultCharset().name(), result);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return buildConnection(server.getDriver(), vdb.getName(), vdb.getVersion(), new Properties());
    }

    static ConnectionImpl buildConnection(TeiidDriver driver, String vdbName, String version, Properties props)
            throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:teiid:").append(vdbName); //$NON-NLS-1$
        if (version != null) {
            sb.append(".").append(version); //$NON-NLS-1$
        }
        sb.append(";"); //$NON-NLS-1$

        if (props.getProperty(TeiidURL.CONNECTION.PASSTHROUGH_AUTHENTICATION) == null) {
            props.setProperty(TeiidURL.CONNECTION.PASSTHROUGH_AUTHENTICATION, "true"); //$NON-NLS-1$
        }
        if (props.getProperty(LocalProfile.TRANSPORT_NAME) == null) {
            props.setProperty(LocalProfile.TRANSPORT_NAME, "openapi");
        }
        if (props.getProperty(LocalProfile.WAIT_FOR_LOAD) == null) {
            props.setProperty(LocalProfile.WAIT_FOR_LOAD, "0"); //$NON-NLS-1$
        }
        ConnectionImpl connection = driver.connect(sb.toString(), props);
        return connection;
    }
}
