package com.paranoiaworks.sse;

import java.lang.management.ManagementFactory;
import sse.org.bouncycastle.crypto.digests.SHA3Digest;

/**
 * Additional Entropy Provider
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */
public class ExtendedEntropyProvider {
	
	public static byte[] getSystemStateDataDigested() 
	{			
		return getSHA3Hash(getSystemStateData().getBytes(), 256);
	}
	
	public static String getSystemStateData()
	{
		StringBuffer systemVariables = new StringBuffer();
		
		systemVariables.append(String.valueOf(System.currentTimeMillis()));
		systemVariables.append(" ");
		
		try {
			systemVariables.append(System.nanoTime());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(ManagementFactory.getRuntimeMXBean().getUptime());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(Thread.currentThread().getId());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(Runtime.getRuntime().freeMemory());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(ManagementFactory.getThreadMXBean().getThreadCpuTime(Thread.currentThread().getId()));
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().toString());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}
		
		try {
			systemVariables.append(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().toString());
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}

		try {
			systemVariables.append(System.identityHashCode(systemVariables));
			systemVariables.append("-");
			systemVariables.append(System.identityHashCode(new String()));
			systemVariables.append(" ");
		} catch (Throwable e) {
			// N/A
		}

		return systemVariables.toString();
	}
    
    public static byte[] getSHA3Hash(byte[] data, int bits)
    {
    	byte[] hash = new byte[bits / 8];
    	SHA3Digest digester = new SHA3Digest(bits);
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
}
