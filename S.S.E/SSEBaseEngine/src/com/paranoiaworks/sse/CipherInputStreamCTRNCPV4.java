package com.paranoiaworks.sse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CyclicBarrier;

import io.lktk.NativeBlake3;
import io.lktk.NativeBlake3Util;

/**
 * Cipher InputStream for CTR mode - Platform Independent + Native MAC
 * 
 * @author Paranoia Works
 * @version 1.0.2
 */
public class CipherInputStreamCTRNCPV4 extends PWCipherInputStream implements WithMAC {

	private EncryptorPI encryptorPI;

	private CounterCTR ctrCounters[] = null;
	private Encryptor.AlgorithmBean algorithmBean = null;
	private int largestBlockSize = -1;
	
	private NativeBlake3 blake3Mac;
	
	private CyclicBarrier barrier = new CyclicBarrier(2);
	ByteBuffer macByteBuff = null;
	
	public CipherInputStreamCTRNCPV4(final InputStream in, byte[] nonce, byte[] key, byte[] macKey, Encryptor.AlgorithmBean algorithmBean) {
        super(in, nonce, key, macKey, algorithmBean.getInnerCode());
		this.encryptorPI = new EncryptorPI(DynamicConfig.getCTRParallelizationPI());
		this.algorithmBean = algorithmBean;
		blake3Mac = NativeBlake3.createHasher(macKey);
        addToMAC(nonce);

		if(algorithmBean.getNestedAlgs() == null) {
			ctrCounters = new CounterCTR[1];
			ctrCounters[0] = new CounterCTR(nonce);
			largestBlockSize = nonce.length;
		}
		else {
			ctrCounters = new CounterCTR[algorithmBean.getNestedAlgs().length];
			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i) {
				int[] nonceSplit = algorithmBean.getNonceSplit();
				int nonceLength = nonceSplit[i];
				if(nonceLength > largestBlockSize) largestBlockSize = nonceLength;
				byte[] nonceTemp = Helpers.getSubarray(nonce, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
				nonceOffset += nonceLength;
			}
		}
    }
	
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
    	if(b.length % largestBlockSize != 0) throw new IOException("Bad data size CISPI");

    	final int r = in.read(b, off, len);
    	final int length = b.length;
    	  	
        if(macByteBuff == null || macByteBuff.capacity() < length) {
        	macByteBuff = ByteBuffer.allocateDirect(length);
        	macByteBuff.order(ByteOrder.nativeOrder());
        }
        macByteBuff.clear();
        macByteBuff.put(b);
    	
		new Thread (new Runnable() 
		{
			public void run() 
			{
				if(length == r) {
		    		addToMAC(macByteBuff, length);
		    	}
		    	else if(r > 0) {
		    		addToMAC(macByteBuff, r);
		    	}
    			try {
					barrier.await();
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}).start();

		encryptorPI.decryptByteArrayCTR(ctrCounters, key, algorithmBean.getKeySplit(), b,
				algorithmBean.getNestedAlgs() == null ? new int[] {algorithmBean.getInnerCode()} : algorithmBean.getNestedAlgs());
    	
		try {
			barrier.await();
		} catch (Exception e) {
			throw new IOException("canceled");
		}	
		
		barrier.reset();

		if(algorithmBean.getNestedAlgs() != null)
		{
			int[] nonceSplit = algorithmBean.getNonceSplit();
			for (int i = 0; i < ctrCounters.length; ++i) ctrCounters[i].add(length / nonceSplit[i]);
		}
		else ctrCounters[0].add(length / algorithmBean.getBlockSizeInBytes());

        return r;
    }
    
	public void addToMAC(final byte[] data)
	{
		blake3Mac.update(data);
	}
	
	public void addToMAC(final ByteBuffer data, long length)
	{
		blake3Mac.update(data, length);
	}
	
	public byte[] getMAC()
	{
		readRestOfFile();
		byte[] mac = null;
		try {
			mac = blake3Mac.getOutput();
		} catch (NativeBlake3Util.InvalidNativeOutput invalidNativeOutput) {
			invalidNativeOutput.printStackTrace();
		}

		return mac;
	}
	
	public void shutDownThreadExecutor()
	{
		encryptorPI.shutDownThreadExecutor();
	}
	
	private void readRestOfFile()
	{
		byte[] readBuffer = new byte[131072]; 
		int bytesIn = 0; 
		
		try {
			while((bytesIn = in.read(readBuffer)) != -1) 
			{        	
				addToMAC(getSubarray(readBuffer, 0, bytesIn));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	private static byte[] getSubarray(byte[] array, int offset, int length) 
	{
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}	
}
