package com.paranoiaworks.sse;

import java.math.BigInteger;

/**
 * Helper Class for CTR Mode
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */
public class CounterCTR {
	
	private byte[] counter;
		
	public CounterCTR(byte[] initVector) 
	{
		this.counter = getByteArrayCopy(initVector);
		
	}
	
	public byte[] getCounter()
	{
		return counter;
	}
	
	public void add(long value)
	{
		counter = add(
				counter, 
				bigIntegerToByteArray(BigInteger.valueOf(value), counter.length));
	}
	
	private static byte[] add(byte[] data1, byte[] data2) 
	{
		int size = data1.length;  
		byte[] result = new byte[size];
		for(int i = size - 1, overflow = 0; i >= 0; i--) 
		{
			int v = (data1[i]&0xff) + (data2[i]&0xff) + overflow;
			result[i] = (byte)v;
			overflow = v>>>8;
		}
		return result;
	}
	
	private static byte[] bigIntegerToByteArray(BigInteger value, int blockSize) 
	{
	    byte[] array = value.toByteArray();
	    int remainder = array.length % blockSize;
	    byte[] result = array;
	    byte[] tmp;

	    if (remainder > 0) {
	        tmp = new byte[result.length + blockSize - remainder];
	        System.arraycopy(result, 0, tmp, blockSize - remainder, result.length);
	        result = tmp;
	    }

	    return result;
	}	
	
	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
}
