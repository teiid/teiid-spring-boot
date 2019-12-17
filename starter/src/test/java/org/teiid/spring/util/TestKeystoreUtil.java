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

import org.junit.Test;

public class TestKeystoreUtil {

    private static String tlsKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEAw7K81Aomo+YmPdaKI2oSNghYeRycmbTTfZxs8zmSKjGK5xiz\n" +
            "c8KdYMm6nC2OwG/SenCxpoxznrJqhLxA5jnQxLOqJpdksPzKUzPHKJwtQzsVDL73\n" +
            "Npr1e7yxtX7sZAaDylhlhugdPaxU+DZDODY4Ei4eUn4oBm0z0hHx5jc+atkE/6Bt\n" +
            "MNjkjQ4pEBQ0dlGMz1CAqHD1CHS+WKoAYd+Nl+JpxtgiCakHxj9XduaAT4DmtK6e\n" +
            "gRTH56iwHgGXFNiYoRuTXVmfZRK+VjeFOhkO721tLXd58O+QSBufSHPBdI2FESuU\n" +
            "tluQ79NAg82vNMpLiWvTEPYAWtdEm1KKWSy+tQIDAQABAoIBAEzBBZ6EfLM7yTPn\n" +
            "uTY6m/nlPA4EUsdWTLyvZWvf4Jv74mbRIVD9u4wCktY+aAbB1OOA36xa2d4cS/rT\n" +
            "ewDRHDiJ3upT5oIkI5aOcEZVOstrmM3u+DbKgA2P7IMqzLxOcdIi6W8ICq/tr0XK\n" +
            "woKBgWxf+jSQllIu8mwp7Z5FfI2a75K0gZ19kNEfuzs+DZl3Ve0niZTrUkdxtHoq\n" +
            "RbcuPPca09nJM1G0W/CfScDT+9gVcvLbExxejaYzo4wQwQk/pa99fYJTb+FwqVJg\n" +
            "uY+OT2Wo0KLbnJOINYR1q0NymATZD5wudLuv+cbGHGK9HTCAY9UDluSjPVVctE7Z\n" +
            "HvVGTeECgYEAxZ9jYiaNAhvSnuno2yqCDAn7jCPIyYWISh88+9BKGSk54vCuLKub\n" +
            "QCIgeJhE67yCyUQSL6bm3XGN5RBa74GE/bCxIeWbSUnnNhHqu/nkE+X+yEFnuWDW\n" +
            "pJC/lZ1hifVLzlyLkAeJMP0ezK1EDAi5zIlX+PLYm0md4T3noZv6hI8CgYEA/YHS\n" +
            "O9ymaee0rxbb0s6ramqveuNDd5MjT9p20biBkvmJQ5+dcDMIJXM/MgzyFirDXQS/\n" +
            "v/fhzgyZszupO6x/W69J4rCLFR8RnCfVykinZ/6YF8dePS9m/jv9JWJtfcb0Yh3l\n" +
            "kQ0clCYEfueSzQakA3K8znuNzGbGt6CH2n4zEnsCgYEAhLNFgffPAdZ69Liu4tRF\n" +
            "ZR4i6nRw9FkVLmiRg6nWx9R2CVyCKH7HDiacVT4yMXVxAQMTjynsplAE7waveVoo\n" +
            "Wk2Wc+OBBZJ5jkYzsCvZqj7reb7pjoJnPzPvYeC/SWsMjzJ3iKx2xA2D6/6Aze/i\n" +
            "C2VCTGmNZ6DkQgZWjp91diMCgYEA0qDuwHYQfqXZ5jAj7P9yRTTnID97avqBuHNX\n" +
            "jeCzKB7VAa647OQ5vIQI2dkPu4NEVyD/AM1AfCbT/atwbPhhyRfXV9Y/eQkbZJdk\n" +
            "dnHvReSvEfLARi4AcPP+3PTu7DZGDs+wUdiHCkCcM6TMwDToSUUnwpe9tTsfmKp0\n" +
            "tgFtBckCgYBgp1VBCzsm9fHab3x0BnSWwxb+oDi7OHTSzhWwTPKFMcX8qgO8P0yq\n" +
            "MuL60HiJP2HQsP6qntbOpG1fhH0bNcz6VNTj+W4MS2Kz/My1493kpMT9dInXBl+r\n" +
            "FNk3ibU2wffFL7xE0xxP5yD2ZfRrHKSa/WaWu+V+hsY/Ikl7Mr0hGw==\n" +
            "-----END RSA PRIVATE KEY-----";

    private static String tlsCert = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDmTCCAoGgAwIBAgIINH5ly1fUe/0wDQYJKoZIhvcNAQELBQAwNjE0MDIGA1UE\n" +
            "Awwrb3BlbnNoaWZ0LXNlcnZpY2Utc2VydmluZy1zaWduZXJAMTU3NjA4NDU3MDAe\n" +
            "Fw0xOTEyMTUxODUyMzNaFw0yMTEyMTQxODUyMzRaMCQxIjAgBgNVBAMTGWR2LWN1\n" +
            "c3RvbWVyLm15cHJvamVjdC5zdmMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n" +
            "AoIBAQDDsrzUCiaj5iY91oojahI2CFh5HJyZtNN9nGzzOZIqMYrnGLNzwp1gybqc\n" +
            "LY7Ab9J6cLGmjHOesmqEvEDmOdDEs6oml2Sw/MpTM8conC1DOxUMvvc2mvV7vLG1\n" +
            "fuxkBoPKWGWG6B09rFT4NkM4NjgSLh5SfigGbTPSEfHmNz5q2QT/oG0w2OSNDikQ\n" +
            "FDR2UYzPUICocPUIdL5YqgBh342X4mnG2CIJqQfGP1d25oBPgOa0rp6BFMfnqLAe\n" +
            "AZcU2JihG5NdWZ9lEr5WN4U6GQ7vbW0td3nw75BIG59Ic8F0jYURK5S2W5Dv00CD\n" +
            "za80ykuJa9MQ9gBa10SbUopZLL61AgMBAAGjgbwwgbkwDgYDVR0PAQH/BAQDAgWg\n" +
            "MBMGA1UdJQQMMAoGCCsGAQUFBwMBMAwGA1UdEwEB/wQCMAAwTQYDVR0RBEYwRIIZ\n" +
            "ZHYtY3VzdG9tZXIubXlwcm9qZWN0LnN2Y4InZHYtY3VzdG9tZXIubXlwcm9qZWN0\n" +
            "LnN2Yy5jbHVzdGVyLmxvY2FsMDUGCysGAQQBkggRZAIBBCYTJGZlZjMxYTk1LTFl\n" +
            "ZDAtMTFlYS05ZmRmLTUyNTQwMGI5N2ZiYTANBgkqhkiG9w0BAQsFAAOCAQEAm5J1\n" +
            "buN/2z7fVAwvKLbImsUWDaH7Hg8gxot2+6rzx6QydxyY5jq7DZBW/jMlqmXBob1B\n" +
            "9Ou3fbPa3lDQOnPgs3FIiRPRCdi8L7VuhZGO+LpD7wXvazTsGeicdpYDDZwhDgyV\n" +
            "OF7PEY4LJfYLkGc6ks5uVdDc5IYjKIESeynQ1QGhrzqv9Ibk6KC4M8LsguhiJYHC\n" +
            "tOi5OxmC0Oths9p3ACvkTVxAgBU/v9xGhViQyzbe253cPRCILPFy+5pOuSP6HR0d\n" +
            "HxaevF5y3nUe2a4zL6z3ER1fYAlRfEJG8ldMCQ+7wjzAY3Ujq/vLddz9iA2v+fsh\n" +
            "MmMSipLHWOl4mo7lPA==\n" +
            "-----END CERTIFICATE-----\n" +
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDCjCCAfKgAwIBAgIBATANBgkqhkiG9w0BAQsFADA2MTQwMgYDVQQDDCtvcGVu\n" +
            "c2hpZnQtc2VydmljZS1zZXJ2aW5nLXNpZ25lckAxNTc2MDg0NTcwMB4XDTE5MTIx\n" +
            "MTE3MTYwOVoXDTI0MTIwOTE3MTYxMFowNjE0MDIGA1UEAwwrb3BlbnNoaWZ0LXNl\n" +
            "cnZpY2Utc2VydmluZy1zaWduZXJAMTU3NjA4NDU3MDCCASIwDQYJKoZIhvcNAQEB\n" +
            "BQADggEPADCCAQoCggEBAN6A4VC3zLfjKTEX+/YWFFdtMB0WZxqlilT3pJz1Vr+F\n" +
            "/GWNKjWvs0PshEtYsPvkzEh1JCI8ZlgDyzCuCSkd3x8HAotleZMU/l68iNQ0vUzJ\n" +
            "GIgvIbDJ1mk+PuTEyytu4/XtTQ9skv/k1bsxJEhLHwk3R/bGpY5UXU57wfZ1QUVf\n" +
            "nFj+0+pSei+86QXsCcvnbiVQbvhK9QWFMpdSonTn45WNummQ7MK/uTY4W3SUxk6I\n" +
            "c8fYVgLQTW0DmnCaDomEtKBxOnuU3PHiyEqLqYvPOuN7cjkpWxRm8HQXkYwL3y/u\n" +
            "I5SjqPJ+uPKQMFoyZGEmwIaTigvnhe5mBnUa6SPnrocCAwEAAaMjMCEwDgYDVR0P\n" +
            "AQH/BAQDAgKkMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAKSu\n" +
            "lqO//doIAQiZ5IhyakPGy/kEUnZfWo8eVheDIuMtzLvl4qYaYlNFyK+jSBJy5oKX\n" +
            "qM/hbOxyIrOmuC7z/QYvyoiI4vnvyigR3GlcaMd68GwX7uwjVfhyzutrlD2f4+gZ\n" +
            "uhcM+bVhs9kWbHWrEoEL/X18Hy4FHh2eZ3n9Ff19CQFi4aHYbr7+prNN9eywbJHx\n" +
            "qhB3W5U5oC5aWIt1mVlJzGuzpoylxLSFJJfypjy1q1DUraoQhUzeinqK79ZHYu7O\n" +
            "0B4rC9n+LmV2clXdPasidYK/w75FHvsk0szODjQjaYRDOmspfTTDqDNGb1wi42IJ\n" +
            "Xt44XApfi06KFtVJm/c=\n" +
            "-----END CERTIFICATE-----";

    @Test
    public void test() throws Exception {
        KeystoreUtil.createKeystore(tlsKey, tlsCert, "foo.bar", "target/keystore.jks", "changeit");
    }
}
