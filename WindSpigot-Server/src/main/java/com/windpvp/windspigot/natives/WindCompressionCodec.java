package com.windpvp.windspigot.natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

// WindSpigot start - compression codec abstraction for Java 8 / Java 11+ dual-runtime support
/**
 * Abstracts over velocity-native libdeflate (Java 11+) and java.util.zip.Deflater/Inflater (Java 8).
 * When NativeAcceleration.tryCreateCompressor() returns null, callers use the Deflater/Inflater
 * fallback path that already exists in PacketCompressor / PacketDecompressor.
 */
public interface WindCompressionCodec {

    /** Compress bytes from {@code in} into {@code out}. */
    void deflate(ByteBuf in, ByteBuf out) throws Exception;

    /** Decompress bytes from {@code in} into {@code out}, expecting {@code uncompressedSize} output. */
    void inflate(ByteBuf in, ByteBuf out, int uncompressedSize) throws Exception;

    /**
     * Returns a buffer compatible with this codec's memory constraints.
     * Callers must release the returned buffer if they do not pass it along.
     */
    ByteBuf preferredBuffer(ByteBufAllocator alloc, int size);

    /**
     * Ensures {@code buf} is memory-compatible with this codec.
     * May return a copy. Callers must release the returned buffer when done.
     */
    ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf);

    /** Release any native resources held by this codec. */
    void close();
}
// WindSpigot end
