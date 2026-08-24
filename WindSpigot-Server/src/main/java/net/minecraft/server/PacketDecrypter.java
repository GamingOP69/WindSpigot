package net.minecraft.server;

import java.util.List;

import com.windpvp.windspigot.natives.WindCipherCodec; // WindSpigot

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketDecrypter extends MessageToMessageDecoder<ByteBuf> {
	// WindSpigot start - WindCipherCodec abstracts velocity-native cipher (Java 11+) or JCE AES/CFB8 (Java 8)
	private final WindCipherCodec cipher;

	public PacketDecrypter(WindCipherCodec cipher) {
		this.cipher = cipher;
	}

	@Override
	protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list)
			throws Exception {
		// Paper start
		ByteBuf compatible = cipher.ensureCompatible(channelHandlerContext.alloc(), byteBuf);
		try {
			cipher.process(compatible);
			list.add(compatible);
		} catch (Exception e) {
			compatible.release(); // compatible will never be used if we throw an exception
			throw e;
		}
		// Paper end
	}

	// Paper start
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		cipher.close();
	}
	// Paper end
	// WindSpigot end
}
