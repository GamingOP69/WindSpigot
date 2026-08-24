package net.minecraft.server;

import java.util.zip.Deflater;

import com.windpvp.windspigot.natives.WindCompressionCodec; // WindSpigot

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketCompressor extends MessageToByteEncoder<ByteBuf> {
	private final byte[] encodeBuf; // Paper
	private final Deflater deflater;
	// WindSpigot start - WindCompressionCodec abstracts velocity-native (Java 11+) or null (Java 8 Deflater fallback)
	private final WindCompressionCodec codec;

	public PacketCompressor(int compressionThreshold) {
		this(null, compressionThreshold);
	}

	public PacketCompressor(WindCompressionCodec codec, int compressionThreshold) {
		this.threshold = compressionThreshold;
		if (codec == null) {
			this.encodeBuf = new byte[8192];
			this.deflater = new Deflater();
		} else {
			this.encodeBuf = null;
			this.deflater = null;
		}
		this.codec = codec;
	}
	// WindSpigot end

	private int threshold;

	@Override
	protected void encode(ChannelHandlerContext var1, ByteBuf var2, ByteBuf var3) throws Exception {
		int var4 = var2.readableBytes();
		PacketDataSerializer var5 = new PacketDataSerializer(var3);
		if (var4 < this.threshold) {
			var5.b(0);
			var5.writeBytes(var2);
		} else {
			// Paper start
			if (this.deflater != null) {
				byte[] var6 = new byte[var4];
				var2.readBytes(var6);
				var5.b(var6.length);
				this.deflater.setInput(var6, 0, var4);
				this.deflater.finish();

				while (!this.deflater.finished()) {
					int var7 = this.deflater.deflate(this.encodeBuf);
					var5.writeBytes(this.encodeBuf, 0, var7);
				}

				this.deflater.reset();
				return;
			}

			// WindSpigot start - velocity-native / codec path
			var5.writeVarInt(var4);
			ByteBuf compatibleIn = this.codec.ensureCompatible(var1.alloc(), var2);
			try {
				this.codec.deflate(compatibleIn, var3);
			} finally {
				compatibleIn.release();
			}
			// WindSpigot end
			// Paper end
		}
	}

	// Paper start
	@Override
	protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
		if (this.codec != null) {
			// We allocate bytes to be compressed plus 1 byte. This covers two cases:
			//
			// - Compression
			// According to https://github.com/ebiggers/libdeflate/blob/master/libdeflate.h#L103,
			// if the data compresses well (and we do not have some pathological case) then
			// the maximum size the compressed size will ever be is the input size minus one.
			// - Uncompressed
			// This is fairly obvious - we will then have one more than the uncompressed size.
			int initialBufferSize = msg.readableBytes() + 1;
			return this.codec.preferredBuffer(ctx.alloc(), initialBufferSize);
		}

		return super.allocateBuffer(ctx, msg, preferDirect);
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		if (this.codec != null) {
			this.codec.close();
		}
	}
	// Paper end

	public void a(int var1) {
		// Nacho start - OBFHELPER
		this.setThreshold(var1);
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}
	// Nacho end
}
