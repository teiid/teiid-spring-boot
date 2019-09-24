/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
package org.teiid.maven;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipArchive implements Closeable{
    private ZipOutputStream archive;
    private FileOutputStream fos;

    public ZipArchive(File artifact) throws IOException {
        this.fos = new FileOutputStream(artifact);
        this.archive = new ZipOutputStream(fos);
    }

    @Override
    public void close() throws IOException {
        this.archive.close();
        this.fos.close();
    }

    public void addToArchive(File fileToZip, String fileName) throws IOException {
        addToArchive(fileToZip, fileName, new FileFilter() {
            @Override
            public boolean accept(File f) {
                return true;
            }
        });
    }

    public void addToArchive(File fileToZip, String fileName, FileFilter ff) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }

        fileName = fileName.trim();

        if (fileToZip.isDirectory()) {
            if (!fileName.isEmpty()) {
                if (fileName.endsWith("/")) {
                    this.archive.putNextEntry(new ZipEntry(fileName));
                    this.archive.closeEntry();
                } else {
                    this.archive.putNextEntry(new ZipEntry(fileName + "/"));
                    this.archive.closeEntry();
                }
            }
            File[] children = fileToZip.listFiles(ff);
            for (File childFile : children) {
                addToArchive(childFile, fileName.isEmpty() ?
                        childFile.getName():fileName + "/" + childFile.getName(), ff);
            }
            return;
        }

        if (!fileName.isEmpty()) {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileName);
            this.archive.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                this.archive.write(bytes, 0, length);
            }
            fis.close();
        }
    }

    public static File unzip(File in, File out) throws FileNotFoundException, IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(in));
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            File newFile = new File(out, fileName);
            new File(newFile.getParent()).mkdirs();
            if (!newFile.isDirectory()) {
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        zis.close();
        return out;
    }
}
