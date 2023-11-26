package com.paranoiaworks.sse;


/**
 * Argon2 KDF Executor
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */
public class Argon2Provider {
	
	private Argon2Provider(){}
	
	public static byte[] Argon2idHash(byte[] password, byte[] salt, Argon2Params ap, int outputLength)
	{		
		at.gadermaier.argon2.Argon2 argon2pi = at.gadermaier.argon2.Argon2Factory.create();
		argon2pi.setType(at.gadermaier.argon2.model.Argon2Type.Argon2id);
		argon2pi.setIterations(ap.getT());
		argon2pi.setMemoryInKiB(ap.getM());
		argon2pi.setParallelism(ap.getH());
		argon2pi.setOutputLength(outputLength);
		byte[] output = argon2pi.hashBytes(getByteArrayCopy(password), getByteArrayCopy(salt));
		    
		return output;
	}
	
	private static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
}
