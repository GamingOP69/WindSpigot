package com.windpvp.windspigot.natives;

import com.velocitypowered.natives.compression.VelocityCompressor;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

// WindSpigot start - velocity-native compression wrapper, only loaded on Java 11+ by NativeAcceleration
/**
 * WindCompressionCodec implementation backed by velocity-native libdeflate.
 *
 * IMPORTANT: This class directly references com.velocitypowered.natives (compiled for Java 11,
 * class file version 55.0). It MUST NOT be directly imported or referenced from any other class.
 * NativeAcceleration loads this via Class.forName() inside a try/catch(Throwable), which safely
 * catches UnsupportedClassVersionError on Java 8 and returns null (Deflater/Inflater fallback).
 */
public final class VelocityCompressionCodec implements WindCompressionCodec {

    private final VelocityCompressor compressor;

    private VelocityCompressionCodec(VelocityCompressor compressor) {
        this.compressor = compressor;
    }

    /** Static factory invoked reflectively by NativeAcceleration on Java 11+. */
    public static VelocityCompressionCodec create() {
        return new VelocityCompressionCodec(Natives.compress.get().create(-1));
    }

    /** Returns the human-readable name of the loaded compression impl (e.g. "libdeflate"). */
    public static String getLoadedVariant() {
        return Natives.compress.getLoadedVariant();
    }

    @Override
    public void deflate(ByteBuf in, ByteBuf out) throws Exception {
        compressor.deflate(in, out);
    }

    @Override
    public void inflate(ByteBuf in, ByteBuf out, int uncompressedSize) throws Exception {
        compressor.inflate(in, out, uncompressedSize);
    }

    @Override
    public ByteBuf preferredBuffer(ByteBufAllocator alloc, int size) {
        return MoreByteBufUtils.preferredBuffer(alloc, compressor, size);
    }

    @Override
    public ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf) {
        return MoreByteBufUtils.ensureCompatible(alloc, compressor, buf);
    }

    @Override
    public void close() {
        compressor.close();
    }
}
// WindSpigot end
