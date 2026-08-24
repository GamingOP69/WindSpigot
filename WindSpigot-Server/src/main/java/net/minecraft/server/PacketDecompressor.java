package net.minecraft.server;

import java.util.List;
import java.util.zip.Inflater;

import com.windpvp.windspigot.natives.WindCompressionCodec; // WindSpigot

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

public class PacketDecompressor extends ByteToMessageDecoder {
	private final Inflater inflater;
	// WindSpigot start - WindCompressionCodec abstracts velocity-native (Java 11+) or null (Java 8 Inflater fallback)
	private final WindCompressionCodec codec;
	private int threshold;

	public PacketDecompressor(int compressionThreshold) {
		this(null, compressionThreshold);
	}

	public PacketDecompressor(WindCompressionCodec codec, int compressionThreshold) {
		this.threshold = compressionThreshold;
		this.inflater = codec == null ? new Inflater() : null;
		this.codec = codec;
	}
	// WindSpigot end

	@Override
	protected void decode(ChannelHandlerContext var1, ByteBuf var2, List<Object> var3) throws Exception {
		if (var2.readableBytes() != 0) {
			PacketDataSerializer var4 = new PacketDataSerializer(var2);
			int var5 = var4.e();
			if (var5 == 0) {
				var3.add(var4.readBytes(var4.readableBytes()));
			} else {
				if (var5 < this.threshold) {
					throw new DecoderException("Badly compressed packet - size of " + var5
							+ " is below server threshold of " + this.threshold);
				}

				if (var5 > 2097152) {
					throw new DecoderException("Badly compressed packet - size of " + var5
							+ " is larger than protocol maximum of " + 2097152);
				}
				// Paper start
				if (this.inflater != null) {
					byte[] var6 = new byte[var4.readableBytes()];
					var4.readBytes(var6);
					this.inflater.setInput(var6);
					byte[] var7 = new byte[var5];
					this.inflater.inflate(var7);
					var3.add(Unpooled.wrappedBuffer(var7));
					this.inflater.reset();
					return;
				}
				// WindSpigot start - velocity-native / codec path
				int claimedUncompressedSize = var5; // OBFHELPER
				ByteBuf compatibleIn = this.codec.ensureCompatible(var1.alloc(), var2);
				ByteBuf uncompressed = this.codec.preferredBuffer(var1.alloc(), claimedUncompressedSize);
				try {
					this.codec.inflate(compatibleIn, uncompressed, claimedUncompressedSize);
					var3.add(uncompressed);
					var2.clear();
				} catch (Exception e) {
					uncompressed.release();
					throw e;
				} finally {
					compatibleIn.release();
				}
				// WindSpigot end
				// Paper end
			}
		}
	}

	// Paper start
	@Override
	public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
		if (this.codec != null) {
			this.codec.close();
		}
	}
	// Paper end

	public void a(int var1) {
		this.threshold = var1;
	}
}
