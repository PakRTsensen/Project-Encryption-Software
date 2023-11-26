package com.paranoiaworks.sse;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Cipher OutputStream - Base Class
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */
public abstract class PWCipherOutputStream extends FilterOutputStream {
	
	protected byte[] iv;
	protected byte[] nonce;
	protected byte[] key;
	protected byte[] macKey;
	protected int algorithmCode;
	
	public PWCipherOutputStream(final OutputStream out, byte[] iv, byte[] key, int algorithmCode)
	{
		super(out);
        this.iv = iv;
        this.key = key;
        this.algorithmCode = algorithmCode;
	}
	
	public PWCipherOutputStream(final OutputStream out, byte[] nonce, byte[] key, byte[] macKey, int algorithmCode)
	{
		super(out);
        this.nonce = nonce;
        this.key = key;
        this.macKey = macKey;
        this.algorithmCode = algorithmCode;
	}
	
	abstract public void doFinal() throws IOException;
}
