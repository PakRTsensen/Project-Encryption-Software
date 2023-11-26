package com.paranoiaworks.sse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CyclicBarrier;

import ove.crypto.digest.Blake2b;

/**
 * Cipher InputStream for CTR mode - Platform Independent
 *
 * @author Paranoia Works
 * @version 1.1.1
 */
public class CipherInputStreamCTRPIV3 extends PWCipherInputStream implements WithMAC {

	private EncryptorPI encryptorPI;

	private CounterCTR ctrCounters[] = null;
	private Encryptor.AlgorithmBean algorithmBean = null;
	private int largestBlockSize = -1;

	private Blake2b blake2bMac = null;

	private CyclicBarrier barrier = new CyclicBarrier(2);

	public CipherInputStreamCTRPIV3(final InputStream in, byte[] nonce, byte[] key, byte[] macKey, Encryptor.AlgorithmBean algorithmBean) {
		super(in, nonce, key, macKey, algorithmBean.getInnerCode());
		this.encryptorPI = new EncryptorPI(DynamicConfig.getCTRParallelizationPI());
		this.algorithmBean = algorithmBean;
		blake2bMac = Blake2b.Mac.newInstance(macKey, 32);
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

		final byte[] copyForMac = getByteArrayCopy(b);

		new Thread (new Runnable()
		{
			public void run()
			{
				if(length == r) {
					addToMAC(copyForMac);
				}
				else if(r > 0) {
					addToMAC(Helpers.getSubarray(copyForMac, 0, r));
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
		blake2bMac.update(data, 0, data.length);
	}

	public byte[] getMAC()
	{
		readRestOfFile();
		byte[] mac = blake2bMac.digest();

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

	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
}
