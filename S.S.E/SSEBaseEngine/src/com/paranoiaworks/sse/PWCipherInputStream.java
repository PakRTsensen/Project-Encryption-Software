package com.paranoiaworks.sse;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cipher InputStream - Base Class
 * 
 * @author Paranoia Works
 * @version 1.0.2
 */
public abstract class PWCipherInputStream extends FilterInputStream {
	
	protected byte[] iv;
	protected byte[] nonce;
	protected byte[] key;
	protected byte[] macKey;
	protected int algorithmCode;
	
	public PWCipherInputStream(final InputStream in, byte[] iv, byte[] key, int algorithmCode)
	{
		super(in);
        this.iv = iv;
        this.key = key;
        this.algorithmCode = algorithmCode;
	}
	
	public PWCipherInputStream(final InputStream in, byte[] nonce, byte[] key, byte[] macKey, int algorithmCode)
	{
		super(in);
        this.nonce = nonce;
        this.key = key;
        this.macKey = macKey;
        this.algorithmCode = algorithmCode;
	}
	
	@Override
    public int read() throws IOException {
    	boolean noUse = true;
    	if(noUse) throw new IOException("Bad data size PWCIS");
        return 0;
    }
	
	abstract public void shutDownThreadExecutor();
}
