package com.windpvp.windspigot.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;

// WindSpigot start - JCE cipher fallback used on Java 8 when velocity-native is unavailable
/**
 * WindCipherCodec implementation backed by {@link javax.crypto.Cipher} (AES/CFB8/NoPadding).
 * Used automatically on Java 8 where velocity-native (compiled for Java 11) cannot be loaded.
 * Also serves as a safe fallback on Java 11+ if native loading unexpectedly fails.
 */
public final class JavaCipherCodec implements WindCipherCodec {

    private final Cipher cipher;

    /**
     * @param key           AES secret key (128-bit as per Minecraft 1.8 protocol)
     * @param forEncryption true = encrypt (server→client), false = decrypt (client→server)
     */
    public JavaCipherCodec(SecretKey key, boolean forEncryption) throws GeneralSecurityException {
        this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        this.cipher.init(
                forEncryption ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                key,
                new IvParameterSpec(key.getEncoded()));
    }

    /**
     * Processes the readable bytes of {@code buf} in-place using AES/CFB8.
     * Uses getBytes/setBytes so reader/writer indices are not disturbed.
     * AES/CFB8 always produces output of equal length to input.
     */
    @Override
    public void process(ByteBuf buf) throws Exception {
        int readable = buf.readableBytes();
        if (readable == 0) return;

        int readerIndex = buf.readerIndex();

        // Peek bytes without advancing reader index
        byte[] input = new byte[readable];
        buf.getBytes(readerIndex, input);

        // AES/CFB8: output length == input length
        byte[] output = cipher.update(input);

        // Write back in-place without changing any indices
        buf.setBytes(readerIndex, output);
    }

    /**
     * JCE has no native memory constraints — any heap or direct buffer works fine.
     * Retains a reference so the caller's release semantics are preserved correctly.
     */
    @Override
    public ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf) {
        return buf.retain();
    }

    /** No-op: javax.crypto.Cipher holds no native resources to release. */
    @Override
    public void close() {
        // no-op
    }
}
// WindSpigot end
