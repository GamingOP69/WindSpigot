package com.windpvp.windspigot.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class JavaCipherCodecTest {

    private static final byte[] TEST_KEY = "1234567890123456".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testRoundTripEncryptionDecryption() throws Exception {
        SecretKeySpec key = new SecretKeySpec(TEST_KEY, "AES");
        JavaCipherCodec encryptor = new JavaCipherCodec(key, true);
        JavaCipherCodec decryptor = new JavaCipherCodec(key, false);

        byte[] original = "Hello Minecraft Packet Data Stream!".getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(original);

        // Encrypt in-place
        encryptor.process(buf);
        byte[] encrypted = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), encrypted);

        // Assert data was encrypted (not identical to original)
        Assert.assertFalse("Encrypted data must differ from original", Arrays.equals(original, encrypted));
        Assert.assertEquals("Encrypted length must match original length", original.length, encrypted.length);

        // Decrypt in-place
        decryptor.process(buf);
        byte[] decrypted = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), decrypted);

        // Assert data decrypted back to original
        Assert.assertArrayEquals("Decrypted data must match original", original, decrypted);
        buf.release();
    }

    @Test
    public void testEmptyBufferHandling() throws Exception {
        SecretKeySpec key = new SecretKeySpec(TEST_KEY, "AES");
        JavaCipherCodec encryptor = new JavaCipherCodec(key, true);
        ByteBuf emptyBuf = Unpooled.buffer();

        encryptor.process(emptyBuf);
        Assert.assertEquals(0, emptyBuf.readableBytes());
        emptyBuf.release();
    }

    @Test
    public void testEnsureCompatibleRetainsBuffer() throws Exception {
        SecretKeySpec key = new SecretKeySpec(TEST_KEY, "AES");
        JavaCipherCodec codec = new JavaCipherCodec(key, true);
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(42);

        Assert.assertEquals(1, buf.refCnt());
        ByteBuf compatible = codec.ensureCompatible(null, buf);
        Assert.assertSame(buf, compatible);
        Assert.assertEquals(2, buf.refCnt());

        compatible.release();
        buf.release();
    }
}
