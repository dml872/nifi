/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.nifi.cluster.coordination.http.replication.okhttp


import org.apache.nifi.util.NiFiProperties
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@RunWith(JUnit4.class)
class OkHttpReplicationClientTest extends GroovyTestCase {
    private static final Logger logger = LoggerFactory.getLogger(OkHttpReplicationClientTest.class)

    @BeforeClass
    static void setUpOnce() throws Exception {
        logger.metaClass.methodMissing = { String name, args ->
            logger.info("[${name?.toUpperCase()}] ${(args as List).join(" ")}")
        }
    }

    private static NiFiProperties mockNiFiProperties() {
        [getClusterNodeConnectionTimeout: { -> "10 ms" },
         getClusterNodeReadTimeout      : { -> "10 ms" },
         getProperty                    : { String prop ->
             logger.mock("Requested getProperty(${prop}) -> \"\"")
             ""
         }] as NiFiProperties
    }

    @Test
    void testShouldReplaceNonZeroContentLengthHeader() {
        // Arrange
        def headers = ["Content-Length": "123", "Other-Header": "arbitrary value"]
        String method = "DELETE"
        logger.info("Original headers: ${headers}")

        NiFiProperties mockProperties = mockNiFiProperties()

        OkHttpReplicationClient client = new OkHttpReplicationClient(mockProperties)

        // Act
        client.checkContentLengthHeader(method, headers)
        logger.info("Checked headers: ${headers}")

        // Assert
        assert headers.size() == 2
        assert headers."Content-Length" == "0"
    }

    @Test
    void testShouldReplaceNonZeroContentLengthHeaderOnDeleteCaseInsensitive() {
        // Arrange
        def headers = ["Content-Length": "123", "Other-Header": "arbitrary value"]
        String method = "delete"
        logger.info("Original headers: ${headers}")

        NiFiProperties mockProperties = mockNiFiProperties()

        OkHttpReplicationClient client = new OkHttpReplicationClient(mockProperties)

        // Act
        client.checkContentLengthHeader(method, headers)
        logger.info("Checked headers: ${headers}")

        // Assert
        assert headers.size() == 2
        assert headers."Content-Length" == "0"
    }

    @Test
    void testShouldNotReplaceContentLengthHeaderWhenZeroOrNull() {
        // Arrange
        String method = "DELETE"
        def zeroOrNullContentLengths = [null, "0"]

        NiFiProperties mockProperties = mockNiFiProperties()

        OkHttpReplicationClient client = new OkHttpReplicationClient(mockProperties)

        // Act
        zeroOrNullContentLengths.each { String contentLength ->
            def headers = ["Content-Length": contentLength, "Other-Header": "arbitrary value"]
            logger.info("Original headers: ${headers}")

            logger.info("Trying method ${method}")
            client.checkContentLengthHeader(method, headers)
            logger.info("Checked headers: ${headers}")

            // Assert
            assert headers.size() == 2
            assert headers."Content-Length" == contentLength
        }
    }

    @Test
    void testShouldNotReplaceNonZeroContentLengthHeaderOnOtherMethod() {
        // Arrange
        def headers = ["Content-Length": "123", "Other-Header": "arbitrary value"]
        logger.info("Original headers: ${headers}")

        NiFiProperties mockProperties = mockNiFiProperties()

        OkHttpReplicationClient client = new OkHttpReplicationClient(mockProperties)

        def nonDeleteMethods = ["POST", "PUT", "GET", "HEAD"]

        // Act
        nonDeleteMethods.each { String method ->
            logger.info("Trying method ${method}")
            client.checkContentLengthHeader(method, headers)
            logger.info("Checked headers: ${headers}")

            // Assert
            assert headers.size() == 2
            assert headers."Content-Length" == "123"
        }
    }

    @Test
    void testShouldUseKeystorePasswordIfKeyPasswordIsBlank() {
        // Arrange
        Map propsMap = [
                (NiFiProperties.SECURITY_TRUSTSTORE)       : "./src/test/resources/conf/truststore.jks",
                (NiFiProperties.SECURITY_TRUSTSTORE_TYPE)  : "JKS",
                (NiFiProperties.SECURITY_TRUSTSTORE_PASSWD): "passwordpassword",
                (NiFiProperties.SECURITY_KEYSTORE)         : "./src/test/resources/conf/keystore.jks",
                (NiFiProperties.SECURITY_KEYSTORE_TYPE)    : "JKS",
                (NiFiProperties.SECURITY_KEYSTORE_PASSWD)  : "passwordpassword",
                (NiFiProperties.SECURITY_KEY_PASSWD)       : "",
                (NiFiProperties.WEB_HTTPS_HOST)            : "localhost",
                (NiFiProperties.WEB_HTTPS_PORT)            : "51552",
        ]
        NiFiProperties mockNiFiProperties = new NiFiProperties(new Properties(propsMap))

        // Act
        OkHttpReplicationClient client = new OkHttpReplicationClient(mockNiFiProperties)
        logger.info("Created secure HTTPS client with TLS configured: ${client.isTLSConfigured()}")

        // Assert
        assert client.isTLSConfigured()
    }

    @Test
    void testShouldUseKeystorePasswordIfKeyPasswordIsNull() {
        // Arrange
        Map flowfileEncryptionProps = [
                (NiFiProperties.SECURITY_TRUSTSTORE)       : "./src/test/resources/conf/truststore.jks",
                (NiFiProperties.SECURITY_TRUSTSTORE_TYPE)  : "JKS",
                (NiFiProperties.SECURITY_TRUSTSTORE_PASSWD): "passwordpassword",
                (NiFiProperties.SECURITY_KEYSTORE)         : "./src/test/resources/conf/keystore.jks",
                (NiFiProperties.SECURITY_KEYSTORE_TYPE)    : "JKS",
                (NiFiProperties.SECURITY_KEYSTORE_PASSWD)  : "passwordpassword",
                (NiFiProperties.WEB_HTTPS_HOST)            : "localhost",
                (NiFiProperties.WEB_HTTPS_PORT)            : "51552",
        ]
        NiFiProperties mockNiFiProperties = new NiFiProperties(new Properties(flowfileEncryptionProps))

        // Act
        OkHttpReplicationClient client = new OkHttpReplicationClient(mockNiFiProperties)
        logger.info("Created secure HTTPS client with TLS configured: ${client.isTLSConfigured()}")

        // Assert
        assert client.isTLSConfigured()
    }

    @Test
    void testShouldFailIfKeyPasswordIsSetButKeystorePasswordIsBlank() {
        // Arrange
        Map propsMap = [
                (NiFiProperties.SECURITY_TRUSTSTORE)       : "./src/test/resources/conf/truststore.jks",
                (NiFiProperties.SECURITY_TRUSTSTORE_TYPE)  : "JKS",
                (NiFiProperties.SECURITY_TRUSTSTORE_PASSWD): "passwordpassword",
                (NiFiProperties.SECURITY_KEYSTORE)         : "./src/test/resources/conf/keystore.jks",
                (NiFiProperties.SECURITY_KEYSTORE_TYPE)    : "JKS",
                (NiFiProperties.SECURITY_KEYSTORE_PASSWD)  : "",
                (NiFiProperties.SECURITY_KEY_PASSWD)       : "passwordpassword",
                (NiFiProperties.WEB_HTTPS_HOST)            : "localhost",
                (NiFiProperties.WEB_HTTPS_PORT)            : "51552",
        ]
        NiFiProperties mockNiFiProperties = new NiFiProperties(new Properties(propsMap))

        // Act
        OkHttpReplicationClient client = new OkHttpReplicationClient(mockNiFiProperties)
        logger.info("Created (invalid) secure HTTPS client with TLS configured: ${client.isTLSConfigured()}")

        // Assert
        assert !client.isTLSConfigured()
    }

    @Test
    void testShouldFailIfKeyPasswordAndKeystorePasswordAreBlank() {
        // Arrange
        Map propsMap = [
                (NiFiProperties.SECURITY_TRUSTSTORE)       : "./src/test/resources/conf/truststore.jks",
                (NiFiProperties.SECURITY_TRUSTSTORE_TYPE)  : "JKS",
                (NiFiProperties.SECURITY_TRUSTSTORE_PASSWD): "passwordpassword",
                (NiFiProperties.SECURITY_KEYSTORE)         : "./src/test/resources/conf/keystore.jks",
                (NiFiProperties.SECURITY_KEYSTORE_TYPE)    : "JKS",
                (NiFiProperties.SECURITY_KEYSTORE_PASSWD)  : "",
                (NiFiProperties.SECURITY_KEY_PASSWD)       : "",
                (NiFiProperties.WEB_HTTPS_HOST)            : "localhost",
                (NiFiProperties.WEB_HTTPS_PORT)            : "51552",
        ]
        NiFiProperties mockNiFiProperties = new NiFiProperties(new Properties(propsMap))

        // Act
        OkHttpReplicationClient client = new OkHttpReplicationClient(mockNiFiProperties)
        logger.info("Created (invalid) secure HTTPS client with TLS configured: ${client.isTLSConfigured()}")

        // Assert
        assert !client.isTLSConfigured()
    }

    @Test
    void testShouldDetermineIfTLSConfigured() {
        // Arrange
        Map propsMap = [(NiFiProperties.WEB_HTTPS_HOST): "localhost",
                        (NiFiProperties.WEB_HTTPS_PORT): "51552",]

        Map tlsPropsMap = [
                (NiFiProperties.SECURITY_KEYSTORE)         : "./src/test/resources/conf/keystore.jks",
                (NiFiProperties.SECURITY_KEYSTORE_PASSWD)  : "passwordpassword",
                (NiFiProperties.SECURITY_KEY_PASSWD)       : "",
                (NiFiProperties.SECURITY_KEYSTORE_TYPE)    : "JKS",
                (NiFiProperties.SECURITY_TRUSTSTORE)       : "./src/test/resources/conf/truststore.jks",
                (NiFiProperties.SECURITY_TRUSTSTORE_PASSWD): "passwordpassword",
                (NiFiProperties.SECURITY_TRUSTSTORE_TYPE)  : "JKS",
        ] + propsMap


        NiFiProperties mockNiFiProperties = new NiFiProperties(new Properties(propsMap))
        NiFiProperties mockTLSNiFiProperties = new NiFiProperties(new Properties(tlsPropsMap))

        // Remove the keystore password to create an invalid configuration
        Map invalidTlsPropsMap = tlsPropsMap
        invalidTlsPropsMap.remove(NiFiProperties.SECURITY_KEYSTORE_PASSWD)
        NiFiProperties mockInvalidTLSNiFiProperties = new NiFiProperties(new Properties(invalidTlsPropsMap))

        // Act
        OkHttpReplicationClient client = new OkHttpReplicationClient(mockNiFiProperties)
        logger.info("Created plaintext HTTP client with TLS configured: ${client.isTLSConfigured()}")

        OkHttpReplicationClient invalidTlsClient = new OkHttpReplicationClient(mockInvalidTLSNiFiProperties)
        logger.info("Created (invalid) secure HTTPS client with TLS configured: ${invalidTlsClient.isTLSConfigured()}")

        OkHttpReplicationClient tlsClient = new OkHttpReplicationClient(mockTLSNiFiProperties)
        logger.info("Created secure HTTPS client with TLS configured: ${tlsClient.isTLSConfigured()}")


        // Assert
        assert !client.isTLSConfigured()
        assert !invalidTlsClient.isTLSConfigured()
        assert tlsClient.isTLSConfigured()
    }
}
