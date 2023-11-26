package com.paranoiaworks.sse;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sse.org.bouncycastle.crypto.InvalidCipherTextException;
import sse.org.bouncycastle.crypto.modes.EAXBlockCipher;
import sse.org.bouncycastle.crypto.modes.SICBlockCipher;

/**
 * Platform Independent Encryptor helper
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */
public class EncryptorPI {

	private ExecutorService executor = null;
	private int parallelization = 1;

	public EncryptorPI()
	{
		this(1);
	}

	public EncryptorPI(int parallelization)
	{
		if(!isPowerOfTwo(parallelization)) throw new IllegalArgumentException("Parallelization has to be power of two.");
		this.parallelization = parallelization;
		if(parallelization > 1) executor = Executors.newFixedThreadPool(parallelization);
	}

	public byte[] encryptByteArrayCTR(CounterCTR counters[], byte[] key, int[] keySplit, byte[] data, int[] algorithmCodes) //CTR
	{
		if(keySplit == null) {
			return encryptByteArrayCTR(counters[0].getCounter(), key, data, algorithmCodes[0]);
		}
		else {
			int keyOffset = 0;
			for(int i = 0; i < counters.length; ++i)
			{
				int keyLength = keySplit[i];
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				data = encryptByteArrayCTR(counters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				keyOffset += keyLength;
				Arrays.fill(keyTemp, (byte) 0);
			}
			return data;
		}
	}

	public byte[]  encryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode) //CTR
	{
		return encryptDecryptByteArrayCTR(nonce, key, data, algorithmCode, true);
	}

	public byte[] decryptByteArrayCTR(CounterCTR counters[], byte[] key, int[] keySplit, byte[] data, int[] algorithmCodes) //CTR
	{
		if(keySplit == null) {
			return decryptByteArrayCTR(counters[0].getCounter(), key, data, algorithmCodes[0]);
		}
		else {
			int keyOffset = key.length;
			for(int i = counters.length - 1; i > -1; --i)
			{
				int keyLength = keySplit[i];
				keyOffset -= keyLength;
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				decryptByteArrayCTR(counters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				Arrays.fill(keyTemp, (byte) 0);
			}
			return data;
		}
	}

	public byte[] decryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode) //CTR
	{
		return encryptDecryptByteArrayCTR(nonce, key, data, algorithmCode, false);
	}

	public byte[] encryptByteArrayEAX(byte[] nonce, byte[] key, byte[] data, byte[] additionalData, Encryptor.AlgorithmBean algorithmBean) throws IllegalStateException, InvalidCipherTextException //EAX
	{
		if(algorithmBean.getNestedAlgs() == null) {
			return encryptEAXPI(nonce, key, data, additionalData, algorithmBean.getInnerCode());
		}
		else {
			CounterCTR ctrCounters[] = new CounterCTR[algorithmBean.getNestedAlgs().length];
			int[] algorithmCodes = algorithmBean.getNestedAlgs();

			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i) {
				int[] nonceSplit = algorithmBean.getNonceSplit();
				int nonceLength = nonceSplit[i];
				byte[] nonceTemp = Helpers.getSubarray(nonce, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
				nonceOffset += nonceLength;
			}
			int keyOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i)
			{
				int[] keySplit = algorithmBean.getKeySplit();
				int keyLength = keySplit[i];
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				if(i == ctrCounters.length - 1)
					data = encryptEAXPI(ctrCounters[i].getCounter(), keyTemp, data, additionalData, algorithmCodes[i]);
				else
					data = encryptByteArrayCTR(ctrCounters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				keyOffset += keyLength;
				Arrays.fill(keyTemp, (byte) 0);
			}
			return data;
		}
	}

	public byte[] decryptByteArrayEAX(byte[] nonce, byte[] key, byte[] data, byte[] additionalData, Encryptor.AlgorithmBean algorithmBean) throws IllegalStateException, InvalidCipherTextException //EAX
	{
		if(algorithmBean.getNestedAlgs() == null) {
			return decryptEAXPI(nonce, key, data, additionalData, algorithmBean.getInnerCode());
		}
		else {
			CounterCTR ctrCounters[] = new CounterCTR[algorithmBean.getNestedAlgs().length];
			int[] algorithmCodes = algorithmBean.getNestedAlgs();

			int nonceOffset = 0;
			for(int i = 0; i < ctrCounters.length; ++i) {
				int[] nonceSplit = algorithmBean.getNonceSplit();
				int nonceLength = nonceSplit[i];
				byte[] nonceTemp = Helpers.getSubarray(nonce, nonceOffset, nonceLength);
				ctrCounters[i] = new CounterCTR(nonceTemp);
				nonceOffset += nonceLength;
			}
			int keyOffset = key.length;
			for(int i = ctrCounters.length - 1; i > -1; --i)
			{
				int[] keySplit = algorithmBean.getKeySplit();
				int keyLength = keySplit[i];
				keyOffset -= keyLength;
				byte[] keyTemp = Helpers.getSubarray(key, keyOffset, keyLength);
				if(i == ctrCounters.length - 1)
					data = decryptEAXPI(ctrCounters[i].getCounter(), keyTemp, data, additionalData, algorithmCodes[i]);
				else
					data = decryptByteArrayCTR(ctrCounters[i].getCounter(), keyTemp, data, algorithmCodes[i]);
				Arrays.fill(keyTemp, (byte) 0);
			}
			return data;
		}
	}
	
	public byte[] encryptDecryptByteArrayCTR(byte[] nonce, byte[] key, byte[] data, int algorithmCode, boolean encrypt)
	{
		int blockSize = Encryptor.getAlgorithmBean(algorithmCode).getBlockSize() / 8;
		byte[] keyCopy = getByteArrayCopy(key);
		byte[] nonceCopy = getByteArrayCopy(nonce);
		byte output[] = null;

		if(parallelization > 1 && data.length >= 524288 && (data.length / parallelization) % blockSize == 0)
		{
			byte[][] chunks = new byte[parallelization][];
			byte[][] keys = new byte[parallelization][];
			byte[][] nonces = new byte[parallelization][];
			EncryptionDecryptionThread encryptionThreads[] = new EncryptionDecryptionThread[parallelization];
			CyclicBarrier barrier = new CyclicBarrier(parallelization + 1);

			for(int i = 0; i < parallelization; ++i) keys[i] = getByteArrayCopy(key);

			int chunkSize = data.length / parallelization;
			int counterOffset = chunkSize / blockSize;
			for(int i = 0; i < parallelization; ++i)
			{
				chunks[i] = getSubarray(data, i * chunkSize, chunkSize);
				if(i == 0) {
					nonces[i] = nonceCopy;
				}
				else {
					CounterCTR tempCounter = new CounterCTR(nonceCopy);
					tempCounter.add(i * counterOffset);
					nonces[i] = tempCounter.getCounter();
				}
				encryptionThreads[i] = new EncryptionDecryptionThread(i, barrier, nonces[i], keys[i], chunks[i], algorithmCode, encrypt);
			}

			for(int i = 0; i < encryptionThreads.length; ++i)
			{
				executor.execute(encryptionThreads[i]);
			}

			try {
				barrier.await();
			} catch (BrokenBarrierException e) {
				throw new Error("ENC Error: Broken Barrier");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// ------ B ------ //

			for(int i = 0; i < encryptionThreads.length; ++i)
			{
				System.arraycopy(encryptionThreads[i].getOutput(), 0, data, i * chunkSize, chunkSize);
			}

			for(int i = 0; i < parallelization; ++i) Arrays.fill(keys[i], (byte) 0);
		}
		else {
			if(encrypt)
				output = encryptCTRPI(nonceCopy, keyCopy, data, algorithmCode);
			else {
				output = decryptCTRPI(nonceCopy, keyCopy, data, algorithmCode);

			}
		}

		Arrays.fill(keyCopy, (byte) 0);

		if(output != null)
			return output;
		else
			return data;
	}

	private byte[] decryptCTRPI(byte[] nonce, byte[] key, byte[] data, int algorithmCode)
	{
		SICBlockCipher cipher = CipherProvider.getCTRCipher(false, nonce, key, algorithmCode);
		cipher.processBytes(data, 0, data.length, data, 0);

		return data;
	}

    private byte[] encryptCTRPI(byte[] nonce, byte[] key, byte[] data, int algorithmCode)
    {
    	SICBlockCipher cipher = CipherProvider.getCTRCipher(true, nonce, key, algorithmCode);
		cipher.processBytes(data, 0, data.length, data, 0);
    	
    	return data;
    }

	private byte[] decryptEAXPI(byte[] nonce, byte[] key, byte[] data, byte[] additionalData, int algorithmCode) throws IllegalStateException, InvalidCipherTextException
	{
		byte[] output = null;

		int length = 0;
		int bytesProcessed;

		EAXBlockCipher cipher = CipherProvider.getEAXCipher(false, nonce, key, algorithmCode, additionalData);
		byte[] buffer = new byte[cipher.getOutputSize(data.length)];
		bytesProcessed = cipher.processBytes(data, 0, data.length, buffer, 0);
		length += bytesProcessed;
		bytesProcessed = cipher.doFinal(buffer, length);
		length += bytesProcessed;

		output = new byte[length];
		System.arraycopy(buffer, 0, output, 0, length);

		return output;
	}

	private byte[] encryptEAXPI(byte[] nonce, byte[] key, byte[] data, byte[] additionalData, int algorithmCode) throws IllegalStateException, InvalidCipherTextException
	{
		byte[] output = null;

		int bytesProcessed;

		EAXBlockCipher cipher = CipherProvider.getEAXCipher(true, nonce, key, algorithmCode, additionalData);
		output = new byte[cipher.getOutputSize(data.length)];
		bytesProcessed = cipher.processBytes(data, 0, data.length, output, 0);
		cipher.doFinal(output, bytesProcessed);

		return output;
	}

	private static boolean isPowerOfTwo(int number)
	{
		return (number != 0) && ((number & (number - 1)) == 0);
	}

	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}

	private static byte[] getSubarray(byte[] array, int offset, int length)
	{
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}

	public void shutDownThreadExecutor()
	{
		if(executor != null) executor.shutdown();
	}

	class EncryptionDecryptionThread implements Runnable
	{
		int id;
		CyclicBarrier barrier;
		byte[] nonce;
		byte[] key;
		byte[] chunk;
		int algorithmCode;
		boolean encrypt;
		byte[] output;

		public byte[] getOutput() {
			return output;
		}

		public EncryptionDecryptionThread(int i, CyclicBarrier barrier, byte[] nonce, byte[] key, byte[] chunk, int algorithmCode, boolean encrypt)
		{
			this.id = i;
			this.barrier = barrier;
			this.nonce = nonce;
			this.key = key;
			this.chunk = chunk;
			this.algorithmCode = algorithmCode;
			this.encrypt = encrypt;
		}

		public void run()
		{
			if(encrypt)
				output = encryptCTRPI(nonce, key, chunk, algorithmCode);
			else
				output = decryptCTRPI(nonce, key, chunk, algorithmCode);
			try {
				barrier.await();
			} catch (BrokenBarrierException e) {
				throw new Error("ENC Error: Broken Barrier in " + id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}

