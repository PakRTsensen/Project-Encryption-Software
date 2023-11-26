package io.lktk;

import java.nio.ByteBuffer;

class JNIRust {
	
	static native long create_hasher();
	
	static native long create_hasher_keyed(byte[] key);
	
	static native void destroy_hasher(long hasher);
	
	static native void blake3_hasher_update(long hasher, byte[] data, long dataLength);
	
	static native void blake3_hasher_updatefb(long hasher, ByteBuffer byteBuff, long dataLength);

    static native byte[] blake3_hasher_finalize(long hasher, int outputLength);
}
