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
package org.teiid.spring.util;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import javax.security.auth.x500.X500Principal;

/**
 * Parts of the file are copied from This file is copied as is from Jolokia project at
 * https://github.com/rhuss/jolokia/blob/master/agent/jvm/src/main/java/org/jolokia/jvmagent/security/KeyStoreUtil.java
 * all credit goes to original authors
 *
 * modified slightly to fit needs
 */
public class KeystoreUtil {

    public static void createKeystore(String tlsKey, String tlsCert, String caCertFile, String keystoreFile,
            String password) throws Exception {

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, password.toCharArray());

        List<X509Certificate> certificateChain = readCertificateChain(tlsCert);
        for (X509Certificate certificate : certificateChain) {
            X500Principal principal = certificate.getSubjectX500Principal();
            ks.setCertificateEntry(principal.getName("RFC2253"), certificate);
        }

        List<byte[]> keyBytes = decodePem(tlsKey);
        PrivateKey privateKey;

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        try {
            // First let's try PKCS8
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes.get(0)));
        } catch (InvalidKeySpecException e) {
            // Otherwise try PKCS1
            RSAPrivateCrtKeySpec keySpec = PKCS1Util.decodePKCS1(keyBytes.get(0));
            privateKey = keyFactory.generatePrivate(keySpec);
        }
        ks.setKeyEntry("key-alias", privateKey, password.toCharArray(), certificateChain.stream().toArray(Certificate[]::new));

        // add trust store too (public key)
        File caCert = new File(caCertFile);
        if (caCert.exists()) {
            updateWithCaPem(ks, caCert);
        }

        try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            ks.store(fos, password.toCharArray());
        }
    }

    /**
     * Update a keystore with a CA certificate
     *
     * @param pTrustStore the keystore to update
     * @param pCaCert     CA cert as PEM used for the trust store
     */
    public static void updateWithCaPem(KeyStore pTrustStore, File pCaCert)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        InputStream is = new FileInputStream(pCaCert);
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            Collection<? extends Certificate> certificates = certFactory.generateCertificates(is);

            for (Certificate c : certificates) {
                X509Certificate cert = (X509Certificate) c;
                String alias = cert.getSubjectX500Principal().getName();
                pTrustStore.setCertificateEntry(alias, cert);
            }
        } finally {
            is.close();
        }
    }

    private static List<X509Certificate> readCertificateChain(String tlsCert)
            throws IOException, GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificates = new ArrayList<>();

        List<byte[]> certs = decodePem(tlsCert);
        for (byte[] cert : certs) {
            certificates.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(cert)));
        }
        return certificates;
    }

    private static byte[] base64Decode(String base64) {
        return Base64.getMimeDecoder().decode(base64.getBytes(US_ASCII));
    }

    // This method is inspired and partly taken over from
    // http://oauth.googlecode.com/svn/code/java/
    // All credits to belong to them.
    private static List<byte[]> decodePem(String pemFile) throws IOException {
        ArrayList<byte[]> list = new ArrayList<byte[]>();
        BufferedReader reader = new BufferedReader(new StringReader(pemFile));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-----BEGIN ")) {
                    list.add(readBytes(reader, line.trim().replace("BEGIN", "END")));
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
            throw new IOException("PEM " + pemFile + " is invalid: no begin marker");
        } finally {
            reader.close();
        }
    }

    private static byte[] readBytes(BufferedReader reader, String endMarker) throws IOException {
        String line;
        StringBuffer buf = new StringBuffer();

        while ((line = reader.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                return base64Decode(buf.toString());
            }
            buf.append(line.trim());
        }
        throw new IOException("Certificate is invalid : No end marker");
    }
}
