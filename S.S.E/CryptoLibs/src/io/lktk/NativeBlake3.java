package io.lktk;

import static io.lktk.NativeBlake3Util.checkArgument;
import static io.lktk.NativeBlake3Util.checkOutput;
import static io.lktk.NativeBlake3Util.checkState;

import java.awt.Window;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.lktk.NativeBlake3Util.InvalidNativeOutput;
import sse.org.bouncycastle.crypto.digests.Blake3Digest;
import sse.org.bouncycastle.crypto.params.Blake3Parameters;

public class NativeBlake3 {
    public static final int KEY_LEN = 32;
    public static final int OUT_LEN = 32;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock w = rwl.writeLock();
    private static ThreadLocal<ByteBuffer> nativeByteBuffer = new ThreadLocal<ByteBuffer>();

    private static boolean loaded;
    private static boolean rust = false;

    private long hasher = -1;

    static {
        try {
            try {
				System.loadLibrary("blake3mt");
				rust = true;
			} catch (Throwable e) {
				System.loadLibrary("blake3");
			}

            byte[] testKey = getPseudoRandomBytes(32);
            byte[] dataA = getPseudoRandomBytes(50000);
            byte[] dataB = getPseudoRandomBytes(262144);

            NativeBlake3 blake3DigestNC = createHasher(testKey, true);
            blake3DigestNC.update(dataA);
            blake3DigestNC.update(dataB);
            byte[] outputNC = blake3DigestNC.getOutput();

            byte[] outputPI = new byte[32];
            Blake3Digest piDigest = new Blake3Digest();
            piDigest.init(Blake3Parameters.key(testKey));
            piDigest.update(dataA, 0, dataA.length);
            piDigest.update(dataB, 0, dataB.length);
            piDigest.doFinal(outputPI, 0);

            loaded = Arrays.equals(outputNC, outputPI);

        } catch (Throwable e) {
            loaded = false;
            //e.printStackTrace();
        }
        
        try {
			if(loaded && Window.getWindows().length > 0) System.out.println("Native BLAKE3: " + (rust ? "Rust MT" : "C") + "\n");
		} catch (Exception e) {}
    }
    
    public static NativeBlake3 createHasher(byte[] key)
    {
    	return createHasher(key, false);
    }
    
    private static NativeBlake3 createHasher(byte[] key, boolean ignoreLoadCheck)
    {
    	NativeBlake3 blake3DigestNC = null;
    	if(rust){
    		blake3DigestNC = new NativeBlake3(key, ignoreLoadCheck);
    	} 
    	else {
    		blake3DigestNC = new NativeBlake3(ignoreLoadCheck);
            blake3DigestNC.initKeyed(key);
    	}
    	
    	return blake3DigestNC;
    }

    public static boolean isNativeCodeAvailable() {
        return loaded;
    }

    private NativeBlake3(boolean ignoreLoadCheck) throws IllegalStateException {
        if(!ignoreLoadCheck)checkState(loaded);
        long initHasher;
        initHasher = rust ? JNIRust.create_hasher() : JNI.create_hasher();
        checkState(initHasher != 0);
        hasher = initHasher;
    }
    
    private NativeBlake3(byte[] key, boolean ignoreLoadCheck) throws IllegalStateException {
    	if(!ignoreLoadCheck) checkState(loaded);
    	checkState(rust);
        checkArgument(key.length == KEY_LEN);
        w.lock();
        try {
            long initHasher;
            initHasher = JNIRust.create_hasher_keyed(key);
            checkState(initHasher != 0);
            hasher = initHasher;
        } finally {
            w.unlock();
        }      
    }

    public boolean isValid() {
        return hasher != -1;
    }

    public void close() {
        if (isValid()) {
            cleanUp();
        }
    }

    private void initKeyed(byte[] key) {
    	checkState(!rust);
        checkArgument(key.length == KEY_LEN);
        ByteBuffer byteBuff = nativeByteBuffer.get();
        if (byteBuff == null || byteBuff.capacity() < key.length) {
            byteBuff = ByteBuffer.allocateDirect(key.length);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeByteBuffer.set(byteBuff);
        }
        byteBuff.rewind();
        byteBuff.put(key);

        w.lock();
        try {
            JNI.blake3_hasher_init_keyed(getHasher(), byteBuff);
        } finally {
            w.unlock();
        }
    }

    public void update(byte[] data) {
        if(rust) 
        {
            w.lock();
            try {
            	JNIRust.blake3_hasher_update(getHasher(), data, data.length);
            } finally {
                w.unlock();
            }
        } 
        else 
        {
        	ByteBuffer byteBuff = nativeByteBuffer.get();
            if (byteBuff == null || byteBuff.capacity() < data.length) {
            	byteBuff = ByteBuffer.allocateDirect(data.length);
                byteBuff.order(ByteOrder.nativeOrder());
                nativeByteBuffer.set(byteBuff);
            }
            byteBuff.rewind();
            byteBuff.put(data);
            
            w.lock();
            try {
            	JNI.blake3_hasher_update(getHasher(), byteBuff, data.length);
            } finally {
                w.unlock();
            }
        }
    }
    
    public void update(ByteBuffer data, long length) {
    	w.lock();
    	try {
    		if(rust) JNIRust.blake3_hasher_updatefb(getHasher(), data, length);
    		else JNI.blake3_hasher_update(getHasher(), data, (int)length);
    	} finally {
    		w.unlock();
    	}
    }

    public byte[] getOutput() throws InvalidNativeOutput {
        return getOutput(OUT_LEN);
    }

    public byte[] getOutput(int outputLength) throws InvalidNativeOutput {
        ByteBuffer byteBuff = nativeByteBuffer.get();

        if (byteBuff == null || byteBuff.capacity() < outputLength) {
            byteBuff = ByteBuffer.allocateDirect(outputLength);
            byteBuff.order(ByteOrder.nativeOrder());
            nativeByteBuffer.set(byteBuff);
        }
        byteBuff.rewind();

        w.lock();
        byte[] retByteArray = new byte[outputLength];
        try {
        	if(rust) retByteArray = JNIRust.blake3_hasher_finalize(getHasher(), outputLength);
        	else { 
        		JNI.blake3_hasher_finalize(getHasher(), byteBuff, outputLength);
        		byteBuff.get(retByteArray);
        	}
        } finally {
            w.unlock();
        }       

        checkOutput(retByteArray.length == outputLength, "Output size produced by lib doesnt match:" + retByteArray.length + " expected:" + outputLength);

        return retByteArray;
    }

    private long getHasher() throws IllegalStateException {
        checkState(isValid());
        return hasher;
    }

    private void cleanUp() {
        w.lock();
        try {
            JNI.destroy_hasher(getHasher());
        } finally {
            hasher = -1;
            w.unlock();
        }
    }

    private static byte[] getPseudoRandomBytes(int size)
    {
        byte[] output = new byte[size];
        Random rand = new Random(System.currentTimeMillis());
        for(int i = 0; i < size; ++i)
            output[i] = (byte)(rand.nextInt());
        return output;
    }
}
