package com.windpvp.windspigot.natives;

import com.velocitypowered.natives.encryption.VelocityCipher;
import com.velocitypowered.natives.util.MoreByteBufUtils;
import com.velocitypowered.natives.util.Natives;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

// WindSpigot start - velocity-native cipher wrapper, only loaded on Java 11+ by NativeAcceleration
/**
 * WindCipherCodec implementation backed by velocity-native OpenSSL cipher.
 *
 * IMPORTANT: This class directly references com.velocitypowered.natives (compiled for Java 11,
 * class file version 55.0). It MUST NOT be directly imported or referenced from any other class.
 * NativeAcceleration loads this via Class.forName() inside a try/catch(Throwable), which safely
 * catches UnsupportedClassVersionError on Java 8 and falls back to JavaCipherCodec.
 */
public final class VelocityCipherCodec implements WindCipherCodec {

    private final VelocityCipher cipher;

    private VelocityCipherCodec(VelocityCipher cipher) {
        this.cipher = cipher;
    }

    /** Factory invoked reflectively by NativeAcceleration for decryption (server-bound stream). */
    public static VelocityCipherCodec forDecryption(SecretKey key) throws GeneralSecurityException {
        return new VelocityCipherCodec(Natives.cipher.get().forDecryption(key));
    }

    /** Factory invoked reflectively by NativeAcceleration for encryption (client-bound stream). */
    public static VelocityCipherCodec forEncryption(SecretKey key) throws GeneralSecurityException {
        return new VelocityCipherCodec(Natives.cipher.get().forEncryption(key));
    }

    /** Returns the human-readable name of the loaded cipher impl (e.g. "OpenSSL"). */
    public static String getLoadedVariant() {
        return Natives.cipher.getLoadedVariant();
    }

    @Override
    public void process(ByteBuf buf) throws Exception {
        cipher.process(buf);
    }

    @Override
    public ByteBuf ensureCompatible(ByteBufAllocator alloc, ByteBuf buf) {
        return MoreByteBufUtils.ensureCompatible(alloc, cipher, buf);
    }

    @Override
    public void close() {
        cipher.close();
    }
}
// WindSpigot end
