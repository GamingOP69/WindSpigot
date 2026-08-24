package com.windpvp.windspigot.natives;

import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;

// WindSpigot start - central native acceleration controller with runtime Java version detection
/**
 * Controls velocity-native acceleration with runtime Java version detection so that one
 * single JAR works on both Java 8 (pure-Java fallback) and Java 11+ (velocity-native).
 *
 * How it works:
 *  - On Java 8: Class.forName("VelocityCompressionCodec") triggers loading of velocity-native
 *    classes (class file 55.0 / Java 11) which throws UnsupportedClassVersionError (an Error,
 *    not Exception). This is caught by catch(Throwable), NATIVE_AVAILABLE = false.
 *  - On Java 11+: loading succeeds, NATIVE_AVAILABLE = true, full native performance.
 *
 * IMPORTANT: No other class should directly import com.velocitypowered.natives.*
 *            All access goes through this class or the Velocity*Codec wrappers.
 */
public final class NativeAcceleration {

    private static final int JAVA_MAJOR;
    private static final boolean NATIVE_AVAILABLE;

    static {
        JAVA_MAJOR = detectMajorVersion();
        NATIVE_AVAILABLE = JAVA_MAJOR >= 11 && tryLoadVelocityNative();
    }

    private NativeAcceleration() {}

    // -------------------------------------------------------------------------
    // Initialisation helpers
    // -------------------------------------------------------------------------

    /**
     * Parses the JVM's "java.version" system property and returns the major version number.
     * Handles both old-style ("1.8.0_xxx" → 8) and new-style ("11.0.2" → 11) formats.
     */
    private static int detectMajorVersion() {
        String version = System.getProperty("java.version", "1.8");
        try {
            if (version.startsWith("1.")) {
                // Legacy format: "1.8.0_502" → split by "." → ["1","8","0_502"] → index 1 = 8
                return Integer.parseInt(version.split("\\.")[1]);
            } else {
                // Modern format: "11.0.2", "17.0.1", "21" → first segment before any delimiter
                return Integer.parseInt(version.split("[.\\-+]")[0]);
            }
        } catch (Exception e) {
            return 8; // safe conservative default
        }
    }

    /**
     * Attempts to load the two Velocity*Codec wrapper classes via Class.forName().
     * On Java 8 these classes link against velocity-native (class file 55.0) and throw
     * UnsupportedClassVersionError (an Error), which is caught here to return false.
     * On Java 11+ the load succeeds and returns true.
     */
    private static boolean tryLoadVelocityNative() {
        try {
            Class.forName("com.windpvp.windspigot.natives.VelocityCompressionCodec");
            Class.forName("com.windpvp.windspigot.natives.VelocityCipherCodec");
            return true;
        } catch (Throwable t) {
            // Catches UnsupportedClassVersionError (Java 8) or any other linkage failure
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link WindCompressionCodec} backed by velocity-native libdeflate on Java 11+,
     * or returns {@code null} on Java 8.
     *
     * <p>When null is returned, callers (PacketCompressor, PacketDecompressor) use their existing
     * java.util.zip.Deflater / Inflater fallback paths automatically.
     */
    public static WindCompressionCodec tryCreateCompressor() {
        if (!NATIVE_AVAILABLE) return null;
        try {
            Class<?> cls = Class.forName("com.windpvp.windspigot.natives.VelocityCompressionCodec");
            Method create = cls.getMethod("create");
            return (WindCompressionCodec) create.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Creates a {@link WindCipherCodec} — velocity-native OpenSSL on Java 11+, JCE AES/CFB8 on Java 8.
     * Never returns null; falls back to {@link JavaCipherCodec} if native loading fails for any reason.
     *
     * @param key           the AES secret key negotiated during login
     * @param forEncryption true for the server→client encrypt cipher, false for client→server decrypt
     * @throws GeneralSecurityException if the key is invalid (propagated from both paths)
     */
    public static WindCipherCodec createCipher(SecretKey key, boolean forEncryption)
            throws GeneralSecurityException {
        if (NATIVE_AVAILABLE) {
            try {
                Class<?> cls = Class.forName("com.windpvp.windspigot.natives.VelocityCipherCodec");
                String methodName = forEncryption ? "forEncryption" : "forDecryption";
                Method factory = cls.getMethod(methodName, SecretKey.class);
                return (WindCipherCodec) factory.invoke(null, key);
            } catch (InvocationTargetException ite) {
                // Unwrap GeneralSecurityException thrown by the factory method and propagate it
                if (ite.getCause() instanceof GeneralSecurityException) {
                    throw (GeneralSecurityException) ite.getCause();
                }
                // Any other failure: fall through to JCE fallback
            } catch (Throwable ignored) {
                // Fall through to JCE fallback
            }
        }
        return new JavaCipherCodec(key, forEncryption);
    }

    /** @return true if velocity-native is loaded and active. */
    public static boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    /** @return the detected Java major version (e.g. 8, 11, 17, 21). */
    public static int getJavaMajorVersion() {
        return JAVA_MAJOR;
    }

    /**
     * Logs the active compression and cipher implementations to the given logger.
     * Called once at server startup from ServerConnection.
     */
    public static void logStatus(Logger logger) {
        if (NATIVE_AVAILABLE) {
            try {
                Class<?> compCls = Class.forName("com.windpvp.windspigot.natives.VelocityCompressionCodec");
                Class<?> cipCls  = Class.forName("com.windpvp.windspigot.natives.VelocityCipherCodec");
                String compVariant = (String) compCls.getMethod("getLoadedVariant").invoke(null);
                String cipVariant  = (String) cipCls.getMethod("getLoadedVariant").invoke(null);
                logger.info("WindSpigot: Using {} compression from Velocity.", compVariant);
                logger.info("WindSpigot: Using {} cipher from Velocity.", cipVariant);
            } catch (Throwable t) {
                logger.info("WindSpigot: Native acceleration active (Java {}).", JAVA_MAJOR);
            }
        } else {
            logger.info("WindSpigot: Native acceleration unavailable on Java {}. Using built-in compression and AES/CFB8 cipher.", JAVA_MAJOR);
        }
    }
}
// WindSpigot end
