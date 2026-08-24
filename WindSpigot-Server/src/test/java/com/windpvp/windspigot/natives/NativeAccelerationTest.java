package com.windpvp.windspigot.natives;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class NativeAccelerationTest {

    private static final Logger LOGGER = LogManager.getLogger(NativeAccelerationTest.class);
    private static final byte[] TEST_KEY = "1234567890123456".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testJavaMajorVersionPositive() {
        int major = NativeAcceleration.getJavaMajorVersion();
        Assert.assertTrue("Major Java version must be >= 8", major >= 8);
    }

    @Test
    public void testCreateCipherNeverReturnsNull() throws Exception {
        SecretKeySpec key = new SecretKeySpec(TEST_KEY, "AES");
        WindCipherCodec encryptor = NativeAcceleration.createCipher(key, true);
        WindCipherCodec decryptor = NativeAcceleration.createCipher(key, false);

        Assert.assertNotNull("Encryptor must never be null", encryptor);
        Assert.assertNotNull("Decryptor must never be null", decryptor);

        encryptor.close();
        decryptor.close();
    }

    @Test
    public void testLogStatusDoesNotThrow() {
        // Must never throw any exception regardless of environment
        NativeAcceleration.logStatus(LOGGER);
    }
}
