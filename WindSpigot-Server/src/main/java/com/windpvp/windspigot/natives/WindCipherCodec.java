package com.windpvp.windspigot.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

// WindSpigot start - cipher codec abstraction for Java 8 / Java 11+ dual-runtime support
/**
 * Abstracts over velocity-native OpenSSL cipher (Java 11+) and javax.crypto.Cipher (Java 8).
 * NativeAcceleration.createCipher() always returns a non-null implementation.
 */
public interface WindCipherCodec {

    /**
     * Process (encrypt or decrypt) the readable bytes of {@code buf} in-place.
     * The reader/writer indices are not modified; content at [readerIndex, writerIndex)
     * is transformed in-place.
     */
    void process(ByteBuf buf) throws Exception;

    /**
     * Ensures {@code buf} is memory-compatible with this cipher's constraints.
     * May return a copy with a retained reference count. Callers must release the result.
     */
    ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf);

    /** Release any native resources held by this cipher. */
    void close();
}
// WindSpigot end
