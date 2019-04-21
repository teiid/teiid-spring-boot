/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.spring.openapi;

import java.io.IOException;
import java.io.InputStream;

public class OpenApiInputStream extends InputStream {
    private InputStream delegate;

    public OpenApiInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return delegate.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return delegate.read(b);
    }


    @Override
    public void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public long skip(long arg0) throws IOException {
        return delegate.skip(arg0);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
