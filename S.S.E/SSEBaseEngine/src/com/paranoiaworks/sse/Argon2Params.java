package com.paranoiaworks.sse;

import java.util.ArrayList;
import java.util.List;

/**
 * Argon2 KDF parameters (t, m, h) for Application/Format Version
 * 
 * @author Paranoia Works
 * @version 1.0.1
 */ 
public class Argon2Params {

	private Argon2Params(){}
	
	public static final int APP_CODE_PASSWORD_VAULT = 1;
	public static final int APP_CODE_TEXT_ENCRYPTOR = 2;
	public static final int APP_CODE_FILE_ENCRYPTOR = 3;
	public static final int APP_CODE_AUTHENTICATION = 0;
	
	private int t = -1;
	private int m = -1;
	private int h = -1;
	
	public int getT() {
		return t;
	}
	public int getM() {
		return m;
	}
	public int getH() {
		return h;
	}
	
	public static Argon2Params createCustomParameters(int t, int m, int h)
	{
		Argon2Params ap = new Argon2Params();
		ap.t = t;
		ap.m = m;
		ap.h = h;
		return ap;
	}
	
	public static Argon2Params getParameters(int applicationCode, int formatVersion, Byte customParamsByte) throws IllegalArgumentException
	{
		Argon2Params ap = null;
		
		switch(applicationCode) 
        {    
	        case APP_CODE_FILE_ENCRYPTOR:
	    		ap = getParametersForFE(formatVersion, customParamsByte);
	    		break; 
	    		
	        case APP_CODE_TEXT_ENCRYPTOR:
	    		ap = getParametersForTE(formatVersion);
	    		break; 
	    	
	        default: 
	        	throw new IllegalArgumentException("Incorrect Application Code: " + applicationCode);
        }
		
		return ap;
	}
	
	public static Argon2Params getParametersForFE(int formatVersion, byte customParamsByte) throws IllegalArgumentException
	{
		Argon2Params ap = new Argon2Params();
		
		if(formatVersion == 3)
		{
			List<Integer> multipliers = getMultipliersFromParamsByte(customParamsByte);
			
			ap.t = 10 * (int)Math.pow(2, multipliers.get(0));
			ap.m = 10240 * (int)Math.pow(2, multipliers.get(1));
			ap.h = 4;
		}
		else throw new IllegalArgumentException("Incorrect Format Version FE: " + formatVersion);
		
		return ap;
	}
	
	public static Argon2Params getParametersForTE(int formatVersion) throws IllegalArgumentException
	{
		Argon2Params ap = new Argon2Params();
		
		if(formatVersion == 3)
		{
			ap.t = 3;
			ap.m = 30720;
			ap.h = 4;
		}
		else throw new IllegalArgumentException("Incorrect Format Version TE: " + formatVersion);
		
		return ap;
	}
	
	public static byte getCustomParamsByteFE(int formatVersion, int revisionVersion) throws IllegalArgumentException
	{
		if(formatVersion == 3)
		{
			if(revisionVersion == 0)
			{
				String bitString = "00010001"; // m = 1; t = 1;
				return (byte)Integer.valueOf(bitString, 2).intValue();
				
			}
			else throw new IllegalArgumentException("Incorrect Revision Version Param FE: " + formatVersion);
		}
		else throw new IllegalArgumentException("Incorrect Format Version Param FE: " + formatVersion);
	}
	
	private static List<Integer> getMultipliersFromParamsByte(byte customParamsByte)
	{
		int mm =  8 * ((customParamsByte >> 7) & 1) + 4 * ((customParamsByte >> 6) & 1) + 2 * ((customParamsByte >> 5) & 1) + ((customParamsByte >> 4) & 1);
		int tm =  8 * ((customParamsByte >> 3) & 1) + 4 * ((customParamsByte >> 2) & 1) + 2 * ((customParamsByte >> 1) & 1) + ((customParamsByte >> 0) & 1);
		
		List<Integer> multipliers = new ArrayList<Integer>();
		multipliers.add(tm);
		multipliers.add(mm);
		
		return multipliers;	
	}
}
