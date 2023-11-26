package com.paranoiaworks.sse;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import org.apache.commons.compress.archivers.zip.ParanoiaExtraField;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import ext.Base32EAM;
import ext.Base64;
import f5.engine.RawImage;
import f5.main.Embed;
import f5.main.Extract;
import io.lktk.NativeBlake3;
import sse.org.bouncycastle.crypto.BufferedBlockCipher;
import sse.org.bouncycastle.crypto.InvalidCipherTextException;
import sse.org.bouncycastle.crypto.PBEParametersGenerator;
import sse.org.bouncycastle.crypto.digests.SHA1Digest;
import sse.org.bouncycastle.crypto.digests.SHA256Digest;
import sse.org.bouncycastle.crypto.digests.SHA3Digest;
import sse.org.bouncycastle.crypto.digests.SHA512Digest;
import sse.org.bouncycastle.crypto.digests.SkeinDigest;
import sse.org.bouncycastle.crypto.engines.AESFastEngine;
import sse.org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import sse.org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import sse.org.bouncycastle.crypto.generators.SCrypt;
import sse.org.bouncycastle.crypto.modes.CBCBlockCipher;
import sse.org.bouncycastle.crypto.params.HKDFParameters;
import sse.org.bouncycastle.crypto.params.KeyParameter;
import sse.org.bouncycastle.crypto.params.ParametersWithIV;
import sse.org.bouncycastle.crypto.prng.ThreadedSeedGenerator;
import sse.org.bouncycastle.util.encoders.SSEBase64;


/**
 * Main Encryptor Class 
 * 
 * @author Paranoia Works
 * @version 2.0.0 Revision 3
 */ 
public class Encryptor {
	
	public static final String SSE_FILEHEADER_PREFIX = CryptFile.ENC_FILE_HEADER_PREFIX;
	public static final int SSE_FILEHEADER_SIZE = 8;
	public static final String ENC_FILE_EXTENSION = CryptFile.ENC_FILE_EXTENSION;
	public static final String ENC_FILE_UNFINISHED_EXTENSION = "unfinished";
	
	public static final int OUTPUT_ENCODING_SSE64 = 0;
	public static final int OUTPUT_ENCODING_BASE32EAM = 1;
	public static final int OUTPUT_ENCODING_BASE10NUM = 2;
		
	public static final int PURPOSE_BASIC = -1;
	public static final int PURPOSE_AUTHENTICATION = 0;
	public static final int PURPOSE_PASSWORD_VAULT = 1;
	public static final int PURPOSE_TEXT_ENCRYPTION = 2;
	public static final int PURPOSE_FILE_ENCRYPTION = 3;
	
	private static final double CRC_TIMECOEF = 0.3;
	
	private Map<Integer, AlgorithmBean> availableAlgorithms = new HashMap<Integer, AlgorithmBean>();
	
	private byte[] l0PWHash = null;
	private byte[] l0PWHashV3 = null; // password derivative
	private byte[] l0PWHashB = null; // password hash 512bit (SHA3)
	private int purpose = -1;
	private int encryptAlgorithmCode;
	private int decryptAlgorithmCode;
	private boolean lastEncZipped = false;
	private boolean lastDecZipped = false;
	
	private static ResourceBundle textBundle;
	
	static 
	{
		Locale locale = Locale.getDefault();
		textBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", locale);
	}
	
	public static void setLocale(Locale locale)
	{
		Locale.setDefault(locale);
		textBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", Locale.getDefault());
	}
	
    /** Encryptor with entered password, chosen algorithm and unicode character allowed (for backwards compatibility - currently always allowed) */
    public Encryptor(char[] password, int algorithmCode, int purpose, boolean unicodeAllowed) throws GeneralSecurityException, UnsupportedEncodingException
    {   	
    	this.purpose = purpose;
    	setAvailableAlgorithms();
    	encryptAlgorithmCode = algorithmCode;
    	generatePBKeys1024Max(password, unicodeAllowed);
    	generateL0PasswordHash(password);
    }
    
    /** Encryptor with entered password, chosen algorithm, unicode character allowed (for backwards compatibility - currently always allowed), and configured for text encryption */
    public Encryptor(char[] password, int algorithmCode, boolean unicodeAllowed) throws GeneralSecurityException, UnsupportedEncodingException
    {
    	this(password, algorithmCode, PURPOSE_BASIC, unicodeAllowed);
    }
    
    /** Encryptor with entered password, chosen algorithm, and configured for text encryption */
    public Encryptor(char[] password, int algorithmCode) throws GeneralSecurityException, UnsupportedEncodingException
    {
    	this(password, algorithmCode, PURPOSE_BASIC, false);
    }
    
    /** Get Map with Available Algorithms */
    public Map<Integer, AlgorithmBean> getAvailableAlgorithms()
    {
    	return availableAlgorithms;
    }
        
    /** Get Hash of current Encryption Key */
    public String getEncKeyHash()
	{
		return getMD5Hash(getEncKey());
	}
	
    /** Get current Encryption Algorithm Code */
    public int getEncryptAlgorithmCode()
	{
		return encryptAlgorithmCode;
	}
    
    /** Get Hash of current Decryption Key */
    public String getDecKeyHash()
	{
		return getMD5Hash(getDecKey());
	}
	
    /** Get current Decryption Algorithm Code */
    public int getDecryptAlgorithmCode()
	{
		return decryptAlgorithmCode;
	}
	
	//+ Other Algorithm Attributes
    public String getEncryptAlgorithmComment()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).comment;
		return cipher;
	}
	
	public String getDecryptAlgorithmComment()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).comment;
		return cipher;
	}
	
	public String getEncryptAlgorithmShortComment()
	{
		String cipher = availableAlgorithms.get(encryptAlgorithmCode).shortComment;
		//if(isEncNativeCodeAvailable()) cipher += " NC";
		return cipher;
	}
	
	public String getDecryptAlgorithmShortComment()
	{
		String cipher = availableAlgorithms.get(decryptAlgorithmCode).shortComment;
		//if(isDecNativeCodeAvailable()) cipher += " NC";
		return cipher;
	}
	//- Other Algorithm Attributes
	
	/** Encrypt Text (String output) -  Version 3 */
	public synchronized String encryptString(String text, int outputEncoding, int spaceEveryNthChar) throws Exception
	{	
		return encryptString(text, outputEncoding, spaceEveryNthChar, true);
	}
    	
	/** Encrypt Text (String output) -  Version 3 */
	public synchronized String encryptString(String text, int outputEncoding, int spaceEveryNthChar, boolean compress) throws Exception
	{			
    	byte[] outputBytes = encryptStringToBytesVer3(text, compress);
    	
	    String output = null;
        switch (outputEncoding) 
        {     
        	case OUTPUT_ENCODING_BASE32EAM:
        		output = Base32EAM.encode(outputBytes);
        		break;
        		
        	case OUTPUT_ENCODING_BASE10NUM:
        		output = SSEBase10.encode(outputBytes);
        		break;
        		
        	default: 
        		output = new String(SSEBase64.encode(outputBytes), "UTF-8") + "!";	
            	break;
        }
        
        if(spaceEveryNthChar > 0) 
        	output = Helpers.insertTextPeriodically(output, " ", spaceEveryNthChar);
        
    	final int paragraphLength = 500000;
    	if(output.length() > paragraphLength * 1.01)
    		output = Helpers.insertTextPeriodically(output, "\n", paragraphLength);	

     // Verification - little paranoid, but can be useful in case of some hardware failure.
        String testDecryption = decryptString(output);
	    if(!text.trim().equals(testDecryption))
	    	throw new Exception("Unexpected error during encryption process - CODE BTV31.");

		return output;
	}
	
	/** Encrypt Text (byte[] output) -  Version 3 */
	public synchronized byte[] encryptStringToBytesVer3(String text) throws Exception
	{	
		return encryptStringToBytesVer3(text, true);
	}
	
	/** Encrypt Text (byte[] output) -  Version 3 */
	public synchronized byte[] encryptStringToBytesVer3(String text, boolean compress) throws Exception
	{				
		byte[] charEnc = text.trim().getBytes("UTF-8");	
		
		AlgorithmBean ab = getAvailableAlgorithms().get((Integer)encryptAlgorithmCode);
		
		int saltSize = ab.blockSize;
		if(saltSize > 256) saltSize = 256;		
		byte[] salt = getRandomBA(saltSize / 8);

		List<byte[]> encParams = deriveParamsArgon2id(salt, null, ab.keySize / 8, ab.blockSize / 8, null, Argon2Params.APP_CODE_TEXT_ENCRYPTOR, 3);
		
		byte[] outputBytes = null;
		if(compress) {
			outputBytes = compressByteArrayLZMA(charEnc, true);
		}
		else {
			outputBytes = charEnc;
			lastEncZipped = false;
		}
		
		String compressionBString = lastEncZipped ? "1" : "0";
		String versionBString = "011"; // version 3
	 	byte ac = (byte) getEncryptAlgorithmCode();
    	String algorithBString = String.format("%4s", Integer.toBinaryString(ac & 0xFF)).replace(' ', '0');
    	String configBString = compressionBString + versionBString + algorithBString;   	
    	byte[] configByte = new byte[1];
    	configByte[0] = (byte)Integer.valueOf(configBString, 2).intValue();
		
		outputBytes = encryptEAX(outputBytes, encParams.get(0), encParams.get(1), Helpers.concat(salt, configByte));
    	
       	outputBytes = Helpers.concat(salt, outputBytes, configByte);
       	
		Arrays.fill(encParams.get(0), (byte) 0);
		Arrays.fill(encParams.get(1), (byte) 0);

		return outputBytes;
	}
	
	/** Decrypt Text */
	public synchronized String decryptString(String text) throws Exception
	{
		text = text.trim().replaceAll("\\s+", "");
		String output = "-1";
		String startSample = text.substring(0, text.length() < 1000 ? text.length() : 1000);
		if(text.endsWith("!") || startSample.lastIndexOf("-") > -1) // version > 1 + standard output encoding
			output = decryptStringVerX(text, startSample);
		else
			output = decryptStringVerAlt(text);

		return output;
	}
	
	/** Decrypt Text (from String) - Version Alternative */
	private synchronized String decryptStringVerAlt(String text) throws Exception
	{		
		DataFormatException versionException = null;
		
		byte[] input = null;
		if(text.matches("[0-9]+"))
		{
			try {
				input = SSEBase10.decode(text);
				return decryptStringFromBytesVerX(input);
			} catch (DataFormatException e) {
				if(e.getMessage().startsWith("3000::")) versionException = e;
			} catch (Exception e) {
				//swallow
			}
		}
		
		String textUpperCase = text.toUpperCase();
		if(textUpperCase.matches(Base32EAM.getAlphabetRegex())) 
		{
			try {
				input = Base32EAM.decode(textUpperCase);
				return decryptStringFromBytesVerX(input);
			} catch (Exception e) {
				//swallow
			}
		}
		textUpperCase = null;
		
		if(versionException != null) throw versionException;		
		
		return decryptStringVer1(text);
	}
	
	/** Decrypt Text (from String) - Version 2+ */
	private synchronized String decryptStringVerX(String text, String startSample) throws Exception
	{
		int encTextLength = text.length();
		if(startSample.lastIndexOf("=") > -1)
			text = text.substring(text.lastIndexOf("=") + 1, encTextLength);
		else if(startSample.lastIndexOf("/") > -1)
			text = text.substring(text.lastIndexOf("/") + 1, encTextLength);
		if(startSample.lastIndexOf("-") > -1)
			text = text.replaceAll("-", "").replace("~", "]").replace(".", ")");

		byte[] input = text.trim().replaceAll("\\s+", "").getBytes("UTF-8");
		if((input[input.length - 1]) == '!') input = Helpers.getSubarray(input, 0, input.length - 1);

		input = SSEBase64.decode(input);

		return decryptStringFromBytesVerX(input);
	} 
	
	/** Decrypt Text (from byte[]) - Versions 2+ */
	private synchronized String decryptStringFromBytesVerX(byte[] input) throws Exception
	{
		byte firstConfigByte = (input[input.length - 1]);
		int version = 4 * ((firstConfigByte >> 6) & 1) + 2 * ((firstConfigByte >> 5) & 1) + ((firstConfigByte >> 4) & 1);

		if(version == 2){
			return decryptStringFromBytesVer2(input, firstConfigByte);
		}
		else if(version == 3){
			return decryptStringFromBytesVer3(input, firstConfigByte);
		}
		else{
			throw new DataFormatException("3000::" + textBundle.getString("ssecore.exception.InvalidVersion").replaceAll("<1>", Integer.toString(version)));
		}
	}
	
	/** Decrypt Text (from byte[]) - Version 3 */
	private synchronized String decryptStringFromBytesVer3(byte[] input, byte configByte) throws Exception
	{
		String output = "-1";

		int compressed = (configByte >> 7) & 1;
		int algorithmCode = 8 * ((configByte >> 3) & 1) + 4 * ((configByte >> 2) & 1) + 2 * ((configByte >> 1) & 1) + ((configByte >> 0) & 1);
		
		if(compressed == 1) lastDecZipped = true;
		else lastDecZipped = false;
		
		decryptAlgorithmCode = algorithmCode;
		AlgorithmBean ab = getAvailableAlgorithms().get(decryptAlgorithmCode);
		if(ab == null) throw new NoSuchAlgorithmException();
		
		int saltSize = ab.blockSize;
		if(saltSize > 256) saltSize = 256;
		
		byte[] salt = Helpers.getSubarray(input, 0, saltSize / 8);
		input = Helpers.getSubarray(input, saltSize / 8, input.length - 1 - saltSize / 8);
		
		List<byte[]> encParams = deriveParamsArgon2id(salt, null, ab.keySize / 8, ab.blockSize / 8, null, Argon2Params.APP_CODE_TEXT_ENCRYPTOR, 3);
		byte[] outputBytes = decompressByteArrayLZMA(decryptEAX(input, encParams.get(0), encParams.get(1), Helpers.concat(salt, new byte[]{configByte})), true);
		
		output = new String(outputBytes, "UTF-8");
		
		Arrays.fill(encParams.get(0), (byte) 0);
		Arrays.fill(encParams.get(1), (byte) 0);

	    return output;
	}
	
	/** Encrypt Text and Make Steganogram */
	public void exportTextToSteganogram(String text, File sourceFile, File outputFile, int quality, double imageScale) throws Throwable
	{
		List<byte[]> encParams = null;
		
		try {
    		RawImage rawImage = Helpers.loadToRawImage(sourceFile, imageScale);	
			
			byte[] encryptedText = encryptStringToBytesVer3(text);
			
			byte[] salt = (Integer.toString(rawImage.getWidth()) + Integer.toString(rawImage.getHeight())).getBytes("UTF-8");
			encParams = deriveParamsScrypt(salt, 512 / 8, salt.length, ScryptParams.APP_CODE_TEXT_ENCRYPTOR, 2, l0PWHashB);	
			
			byte[] key = (byte[])encParams.get(0);
			Embed.go(key, encryptedText, rawImage, outputFile.getAbsolutePath(), quality);
			rawImage = null;
					
			// Verify output
			String testDecryption = importTextFromSteganogram(outputFile);
			if(!text.trim().equals(testDecryption)) {
				throw new Exception("2000:: Verification Failed");
			}
			
		} catch (Throwable t) {
			if(outputFile != null && outputFile.exists()) outputFile.delete();
			throw t;
		}
		finally {
			if(encParams != null) Arrays.fill(encParams.get(0), (byte) 0);
		}
	}
	
	/** Load Steganogram and Decrypt Text */
	public String importTextFromSteganogram(File inputFile) throws Exception
	{
		Dimension imageDimension = Helpers.getImageDimension(inputFile);
		
		byte[] salt = (Integer.toString(imageDimension.width) + Integer.toString(imageDimension.height)).getBytes("UTF-8");
		List<byte[]> encParams = deriveParamsScrypt(salt, 512 / 8, salt.length, ScryptParams.APP_CODE_TEXT_ENCRYPTOR, 2, l0PWHashB);
		
		byte[] key = (byte[])encParams.get(0);
		byte[] encryptedText = Extract.go(key, inputFile.getAbsolutePath());
		
		Arrays.fill(key, (byte) 0);
		
		String text = decryptStringFromBytesVerX(encryptedText);
		
		return text;
	}
	
	/** Decrypt byte array */
	public byte[] decryptUseEncAlg(byte input[], boolean decompress) throws Exception
    {
		this.decryptAlgorithmCode = this.encryptAlgorithmCode;
		if(decompress) return unzipByteArray(decrypt(input, getDecKey()), false);
    	else return decrypt(input, getDecKey());
    }
	
	/** Get SHA256 of Byte Array */
	public static byte[] getSHA256Hash(byte[] data)
    {
    	byte[] hash = new byte[32];
    	SHA256Digest digester = new SHA256Digest();
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
    
	/** Get SHA512 of Byte Array */
    public static byte[] getSHA512Hash(byte[] data)
    {
    	byte[] hash = new byte[64];
    	SHA512Digest digester = new SHA512Digest();
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
    
	/** Get SHA3 of Byte Array */
    public static byte[] getSHA3Hash(byte[] data, int outputSizeBits)
    {
    	byte[] hash = new byte[outputSizeBits / 8];
    	SHA3Digest digester = new SHA3Digest(outputSizeBits);
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }
    
	/** Get Skein of Byte Array */
    public static byte[] getSkeinHash(byte[] data, int outputSizeBits)
    {
    	byte[] hash = new byte[outputSizeBits / 8];
    	SkeinDigest digester = new SkeinDigest(SkeinDigest.SKEIN_1024, outputSizeBits);
    	digester.update(data, 0, data.length);
    	digester.doFinal(hash, 0);
    	return hash;
    }

	
	/** Encrypt Byte Array using EAX Mode - Execution  */
	private byte[] encryptEAX(byte[] inputText, byte[] key, byte[] nonce, byte[] additionalData) throws IllegalStateException, InvalidCipherTextException
	{
		AlgorithmBean ab = getAvailableAlgorithms().get((Integer)encryptAlgorithmCode);
		EncryptorPI encryptorPI = new EncryptorPI();
		byte[] output = encryptorPI.encryptByteArrayEAX(nonce, key, inputText, additionalData, ab);

		return output;
	}

	/** Decrypt Byte Array using EAX Mode  - Execution  */
	private byte[] decryptEAX(byte[] inputText, byte[] key, byte[] nonce, byte[] additionalData) throws IllegalStateException, InvalidCipherTextException
	{
		AlgorithmBean ab = getAvailableAlgorithms().get((Integer)decryptAlgorithmCode);
		EncryptorPI encryptorPI = new EncryptorPI();
		byte[] output = encryptorPI.decryptByteArrayEAX(nonce, key, inputText, additionalData, ab);

		return output;
	}
	
	/** Compress Byte Array */
	public synchronized byte[] compressByteArrayLZMA(byte[] input)
	{	
		return compressByteArrayLZMA(input, false);
	}
	
	/** Compress Byte Array LZMA
	 *  note: checkLenght (if true - try to compress, but if longer then original return original input)
	 */
	private byte[] compressByteArrayLZMA(byte[] input, boolean checkLenght)
	{				
		ByteArrayOutputStream baos = null;;
		try {		
			long fileSize;
			boolean eos = false;
			
			baos = new ByteArrayOutputStream();
			ByteArrayInputStream bais = new ByteArrayInputStream(input);

			SevenZip.Compression.LZMA.Encoder encoder = new SevenZip.Compression.LZMA.Encoder();			
			
			encoder.SetDictionarySize(1048576);
			encoder.SetEndMarkerMode(eos);
			encoder.WriteCoderProperties(baos);
			
			if (eos)
				fileSize = -1;
			else
				fileSize = input.length;
			for (int i = 0; i < 8; i++)
				baos.write((int)(fileSize >>> (8 * i)) & 0xFF);
			encoder.Code(bais, baos, -1, -1, null);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	    byte[] compressedData = baos.toByteArray(); 
	    
	    if ((compressedData.length > input.length) && checkLenght) 
	    {	
	    	lastEncZipped = false;
	    	return input;
	    }
	    
	    lastEncZipped = true;
	    return compressedData;		
	}
	
	/** Decompress Byte Array LZMA */
	public synchronized byte[] decompressByteArrayLZMA(byte[] input)
	{	
		return decompressByteArrayLZMA(input, false);
	}
	
	/** Decompress Byte Array LZMA, if checkLenght is true - only if necessary */
	private byte[] decompressByteArrayLZMA(byte[] compressedData, boolean checkLenght)
    {    	
       	if(!lastDecZipped && checkLenght) return compressedData;

       	ByteArrayOutputStream baos = null;
		try {
			baos = new ByteArrayOutputStream();
			ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
			
			int propertiesSize = 5;
			byte[] properties = new byte[propertiesSize];
			if (bais.read(properties, 0, propertiesSize) != propertiesSize)
				throw new Exception("input .lzma file is too short");
			SevenZip.Compression.LZMA.Decoder decoder = new SevenZip.Compression.LZMA.Decoder();
			if (!decoder.SetDecoderProperties(properties))
				throw new Exception("Incorrect stream properties");
			long outSize = 0;
			for (int i = 0; i < 8; i++)
			{
				int v = bais.read();
				if (v < 0)
					throw new Exception("Can't read stream size");
				outSize |= ((long)v) << (8 * i);
			}
			if (!decoder.Code(bais, baos, outSize))
				throw new Exception("Error in data stream");

		} catch (Exception e) {
			e.printStackTrace();
		}
		
        byte[] decompressedData = baos.toByteArray();       
        return decompressedData;
    }
    
	/** Compress Byte Array */
	public synchronized byte[] zipByteArray(byte[] input)
	{	
		return zipByteArray(input, false);
	}
	
	/** Compress Byte Array
	 *  note: checkLenght (if true - try to compress, but if longer then original return original input )
	 */
	private byte[] zipByteArray(byte[] input, boolean checkLenght)
	{				
		Deflater compressor = new Deflater();
	    compressor.setLevel(Deflater.BEST_COMPRESSION);    
	    compressor.setInput(input);
	    compressor.finish();
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
	    
	    byte[] buf = new byte[4096];
	    while (!compressor.finished()) {
	        int count = compressor.deflate(buf);
	        bos.write(buf, 0, count);
	    }
	    try {
	        bos.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    byte[] compressedData = bos.toByteArray();     
	    
	    if ((compressedData.length > input.length) && checkLenght) 
	    {	
	    	lastEncZipped = false;
	    	return input;
	    }
	    
	    lastEncZipped = true;
	    return compressedData;		
	}
	
	/** Decompress Byte Array */
	public synchronized byte[] unzipByteArray(byte[] input)
	{	
		return unzipByteArray(input, false);
	}
	
	/** Decompress Byte Array, if checkLenght is true - only if necessary */
	private byte[] unzipByteArray(byte[] compressedData, boolean checkLenght)
    {    	
       	if(!lastDecZipped && checkLenght) return compressedData;
    	Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);

        byte[] buf = new byte[4096];
        try {
			while (!decompressor.finished()) 
			{
				int count = decompressor.inflate(buf);
				if (count > 0) bos.write(buf, 0, count);
					else if (count == 0 && decompressor.finished()) break;
						else throw new DataFormatException(textBundle.getString("ssecore.exception.BadDataSize") + ": " + compressedData.length);
			}
			bos.close();		
		} catch (DataFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        byte[] decompressedData = bos.toByteArray();       
        return decompressedData;
    }
	
	public static byte[] zipObject(Object inputObject, List<String> outputChecksum) throws IOException
    {
    	long processTime = System.currentTimeMillis();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CheckedOutputStream csumo = new CheckedOutputStream (baos, new CRC32());
	    GZIPOutputStream gzos = new GZIPOutputStream(csumo);
	    ObjectOutputStream oos = new ObjectOutputStream(gzos);

		oos.writeObject(inputObject);			
		oos.flush();
		oos.close();
		
		if(outputChecksum != null) outputChecksum.add(getMD5Hash(Long.toString(csumo.getChecksum().getValue())));
		processTime = System.currentTimeMillis() - processTime;
		return baos.toByteArray();
    }    
   
    /** Decompress Object */
    public static Object unzipObject(byte[] zippedObject, List<String> outputChecksum) throws IOException, ClassNotFoundException
    {
    	long processTime = System.currentTimeMillis();
    	
	    ByteArrayInputStream bais = new ByteArrayInputStream(zippedObject);
	    CheckedInputStream cis = new CheckedInputStream(bais, new CRC32());
	    GZIPInputStream gzis = new GZIPInputStream(cis);
	    ObjectInputStream ois = new ObjectInputStream(gzis);
	    
	    Object object = ois.readObject();
		ois.close();
		
		if(outputChecksum != null) outputChecksum.add(getMD5Hash(Long.toString(cis.getChecksum().getValue())));
		processTime = System.currentTimeMillis() - processTime;
		return object;
    }
	
    /** Compress and Encrypt File/Directory */
	public synchronized long zipAndEncryptFile(File inputFile, Boolean compress, MessageHandler mh, File outputdir, List<Object> logs) throws Exception
    {
		checkThreadInterruption(mh);
		long processTime = Calendar.getInstance().getTimeInMillis();
    	boolean nativeCode = isEncNativeCodeAvailable();
		
        final int BUFFER = 65536;
		
		lastEncZipped = true;
		
		String outputFileName = "";
		
		// Generate Key/IV
		AlgorithmBean ab = getAvailableAlgorithms().get((Integer)encryptAlgorithmCode);
		byte[] salt = getRandomBA(32);
		byte customParamsByte = Argon2Params.getCustomParamsByteFE(3, 0);
		List<byte[]> encParams = deriveParamsArgon2id(salt, customParamsByte, ab.keySize / 8, ab.blockSize / 8, 32, Argon2Params.APP_CODE_FILE_ENCRYPTOR, 3);
		
		// FileOutputStream
		File outputFile = new File((outputdir == null ? inputFile.getParent() : outputdir.getAbsolutePath())
				+ File.separator + inputFile.getName() + "." + ENC_FILE_EXTENSION + "." + ENC_FILE_UNFINISHED_EXTENSION);
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		outputFileName = outputFile.getAbsolutePath();
		
    	// CipherOutputStream		
		OutputStream cipherOutputStream = null;
		if(nativeCode) {
			cipherOutputStream = new CipherOutputStreamCTRNCPV4(new BufferedOutputStream(fileOutputStream, BUFFER), encParams.get(1), encParams.get(0), encParams.get(2), ab);
		}
		else {
			cipherOutputStream = new CipherOutputStreamCTRPIV4(new BufferedOutputStream(fileOutputStream, BUFFER), encParams.get(1), encParams.get(0), encParams.get(2), ab);
		}
		
		long inputDirSize = -1;
		if(compress == null && inputFile.isFile()) {
			compress = Helpers.isCompressible(inputFile, textBundle);
		}
		else if(inputFile.isDirectory()) {
			long[] sizes = Helpers.getDirectorySize(inputFile, textBundle);
			
	    	if(sizes != null) {
	    		inputDirSize = sizes[0];
	    		if(compress == null) {
	    			compress = (sizes[2] / 20) < sizes[1];
	    		}
	    		//System.out.println(sizes[0] + " : " + sizes[1] + " : " + sizes[2] + " : " + compress);
	    	}
		}	
		
		// ZipOutputStream
		ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(cipherOutputStream);
		if(compress)zipOutputStream.setMethod(ZipArchiveOutputStream.DEFLATED);
		else zipOutputStream.setMethod(ZipArchiveOutputStream.STORED);
		zipOutputStream.setUseZip64(Zip64Mode.AsNeeded);
				
		// Write File Header		
		byte[] header = Helpers.concat(
				(SSE_FILEHEADER_PREFIX + "FE").getBytes("UTF-8"),
				new byte[]{(byte) 4}, // version
				new byte[]{(byte) encryptAlgorithmCode}, // algorithm
				new byte[]{customParamsByte} // KDF config
				);
		fileOutputStream.write(header);
		((WithMAC)cipherOutputStream).addToMAC(header);
		
		// Write Salt
		fileOutputStream.write(salt);
		((WithMAC)cipherOutputStream).addToMAC(salt);
		
		// Write Encrypted Header
		byte[] checkcode = getRandomString(32, "0123456789abcdefghijklmnopqrstuvwxyz").getBytes("UTF-8");
		cipherOutputStream.write(checkcode); // checkcode (32B)
		
		if(inputFile.isFile()) {
			zipSingleFile(inputFile, zipOutputStream, BUFFER, compress, mh);
		}
		else if(inputFile.isDirectory()) {
			mh.setInpuFileSize(inputDirSize);
			zipDir(inputFile, zipOutputStream, BUFFER, compress, mh);
			//Log.v("DIRSIZE: ", "" + sizeCounter + " : " + Helpers.getDirectorySize(inputFile));
		}
		
		//zipOutputStream.flush();
		zipOutputStream.finish();
		((PWCipherOutputStream)cipherOutputStream).doFinal(); // last buffer
		byte[] mac = ((WithMAC)cipherOutputStream).getMAC();
		fileOutputStream.write(mac); // write MAC
		
		cipherOutputStream.flush();
		fileOutputStream.flush();
		zipOutputStream.close();
		fileOutputStream.close();
		
		processTime = Calendar.getInstance().getTimeInMillis() - processTime;
		
		//+ Header verification - little paranoid, but can be useful in case of some hardware failure.
		try {
			unzipAndDecryptFileV3Plus(outputFile, null, null, 4, true, null);
		} catch (Exception e) {
			throw new Exception("Unexpected error during encryption process - CODE BFV31.");
		}
		//-
		
		// Rename Output File
		int outputFilePathLength = outputFile.getAbsolutePath().length();
		String outputFileFinalName = outputFile.getAbsolutePath().substring(0, outputFilePathLength - (ENC_FILE_UNFINISHED_EXTENSION.length() + 1));
		File outputFileFinal = new File(outputFileFinalName);
		if(outputFileFinal.exists() && !outputFileFinal.delete()) throw new IOException("ENC File Delete: Failed");
		if(!outputFile.renameTo(outputFileFinal)) throw new IOException("ENC File Rename: Failed");

		mh.println("");
		if(inputFile.isDirectory())
			mh.println(textBundle.getString("ssecore.text.InputFolder") + ": " + inputFile.getAbsolutePath(), null, false, true);
		else
			mh.println(textBundle.getString("ssecore.text.InputFile") + ": " + inputFile.getAbsolutePath(), null, false, true);
		mh.println(textBundle.getString("ssecore.text.OutputFile") + ": " + outputFileFinalName, null, false, true);
		
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setMinimumFractionDigits(1); formatter.setMaximumFractionDigits(1);
		
		if(compress) {
			double compressionRatio = (double)outputFileFinal.length() / mh.getInpuFileSize() * 100;
			mh.println(textBundle.getString("ssecore.text.CompressionRatio") + ": " + (compressionRatio > 100 ? ">100%" : formatter.format(compressionRatio) + "%"));
		}
		else {
			mh.println(textBundle.getString("ssecore.text.CompressionRatio") + ": " + textBundle.getString("ssecore.text.compressionOff"));
		}
		
		logs.add(outputFileFinal.getAbsolutePath());
		
		Arrays.fill(encParams.get(0), (byte) 0);
		Arrays.fill(encParams.get(1), (byte) 0);
		Arrays.fill(encParams.get(2), (byte) 0);
		
		return 0;
    }
   
	/** Extension of zipAndEncryptFile Method */
	private long zipSingleFile(File inputFile, ZipArchiveOutputStream zos, final int BUFFER, boolean compress, MessageHandler mh) throws IOException, InterruptedException
	{
		byte data[] = new byte[BUFFER];
		ProgressMessage hm = new ProgressMessage();
        long inputFileSize = inputFile.length();
        mh.setInpuFileSize(inputFileSize);
		if(compress)hm.setFullSize(inputFileSize);
        else hm.setFullSize((long)((CRC_TIMECOEF + 1) * inputFileSize));
        
		ZipArchiveEntry entry = new ZipArchiveEntry(Helpers.convertToUnixFileSeparator(inputFile.getName()));         
		if(!compress) // STORED ONLY
		{
			entry.setCompressedSize(inputFile.length());
            entry.setCrc(getCRC32(inputFile, hm, mh));
		}
		entry.setSize(inputFile.length());
		entry.setTime(inputFile.lastModified());
		
		Short unixMode = Helpers.getUnixMode(inputFile);
		if(unixMode != null) {
			ParanoiaExtraField pef = new ParanoiaExtraField((short)1, Helpers.getUnixMode(inputFile));
			entry.addExtraField(pef);
		}
		
		FileInputStream in = new FileInputStream(inputFile);
		BufferedInputStream origin = new BufferedInputStream(in, BUFFER);		
		zos.putArchiveEntry(entry);

		int count;
		while((count = origin.read(data, 0, BUFFER)) != -1) {
			zos.write(data, 0, count);
			
			hm.setProgressAbs(hm.getProgressAbs() + count);
			//progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
			if(!hm.isRelSameAsLast()) {
				mh.removeLast();
				mh.setProgress(hm.getProgressRel());
				mh.print("\r" + getProgressTextEnc(hm));
			}			
			checkThreadInterruption(mh);
		}
		origin.close();
		zos.closeArchiveEntry();
		return hm.getProgressAbs();
	}
	
	/** Extension of zipAndEncryptFile Method */
    private long zipDir(File dir, ZipArchiveOutputStream zos, final int BUFFER, Boolean compress, MessageHandler mh) throws IOException, InterruptedException, DataFormatException
    {
    	String originalPath = Helpers.replaceLast(dir.getAbsolutePath(), dir.getName(), "");
    	ProgressMessage hm = new ProgressMessage();
        if(compress)hm.setFullSize(mh.getInpuFileSize());
        else hm.setFullSize((long)((CRC_TIMECOEF + 1) * mh.getInpuFileSize()));
    	if(mh.getRealInpuFileSize() == 0) throw new DataFormatException(textBundle.getString("ssecore.exception.FolderSize0"));
    	hm.setProgressAbs(0);
    	return zipDir(dir, zos, BUFFER, originalPath, compress, hm, mh);
    }
    
    /** Extension of zipAndEncryptFile Method */
    private long zipDir(File dir, ZipArchiveOutputStream zos, final int BUFFER, String originalPath, boolean compress, ProgressMessage hm, MessageHandler mh) throws IOException, InterruptedException 
	{ 
		String[] dirList = dir.list();
		byte[] readBuffer = new byte[BUFFER]; 
		int bytesIn = 0; 
		
		for(int i = 0; i < dirList.length; i++)
		{ 
			File f = new File(dir, dirList[i]);
			
			String tempPath = f.getAbsolutePath().substring(originalPath.length(), f.getAbsolutePath().length());
			ZipArchiveEntry anEntry = null;
			
			if(f.isDirectory()) 
	        { 
				anEntry = new ZipArchiveEntry(Helpers.convertToUnixFileSeparator(tempPath + File.separator));
				if(!compress) // STORED ONLY
				{
					anEntry.setSize(0);				
					anEntry.setCrc(0);
				}
				anEntry.setTime(f.lastModified());
				zos.putArchiveEntry(anEntry);
				zos.closeArchiveEntry();			
				
				zipDir(f, zos, BUFFER, originalPath, compress, hm, mh);
	            continue; 
	        }
			anEntry = new ZipArchiveEntry(Helpers.convertToUnixFileSeparator(tempPath));
			
			anEntry.setSize(f.length()); // needed for unziped size
			anEntry.setTime(f.lastModified());
			
			Short unixMode = Helpers.getUnixMode(f);
			if(unixMode != null) {
				ParanoiaExtraField pef = new ParanoiaExtraField((short)1, Helpers.getUnixMode(f));
				anEntry.addExtraField(pef);
			}
			
			if(!compress) // STORED ONLY
			{
				anEntry.setCompressedSize(f.length());;
				anEntry.setCrc(getCRC32(f, hm, mh));
			}
	        zos.putArchiveEntry(anEntry); 
			
			FileInputStream fis = new FileInputStream(f);
			BufferedInputStream origin = new BufferedInputStream(fis, BUFFER);	


	        while((bytesIn = origin.read(readBuffer)) != -1) 
	        {        	
	        	zos.write(readBuffer, 0, bytesIn);
	        	hm.setProgressAbs(hm.getProgressAbs() + bytesIn);
	        	
	        	//progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
	        	if(!hm.isRelSameAsLast()) {
	        		mh.removeLast();
	        		mh.setProgress(hm.getProgressRel());
	        		mh.print("\r" + getProgressTextEnc(hm));
	        	}
	        	checkThreadInterruption(mh);
	        } 
	        zos.closeArchiveEntry();
	        origin.close();
	        fis.close();
	    }
		return hm.getProgressAbs();
	}
    
    /** Decompress and Decrypt File/Directory */
    public synchronized long unzipAndDecryptFile(File inputFile, MessageHandler mh, File outputdir, List<Object> logs) throws Exception
    {
    	int version = -1;
    	
    	byte preambleBuffer[] = new byte[SSE_FILEHEADER_SIZE];
    	    	
    	InputStream fileInputStream = null;
        String preamble = null;

		try {
	    	fileInputStream = new FileInputStream(inputFile);
	        fileInputStream.read(preambleBuffer);
	        preamble = new String(preambleBuffer, "UTF-8");
		} catch (Exception e) {
			// swallow
		} finally {
			try {
				fileInputStream.close();
			} catch (Exception e) {
				// swallow
			}
		}
		
		if(preamble == null || preamble.length() < 5)
			throw new DataFormatException(textBundle.getString("ssecore.exception.InvalidVersion").replaceAll("<1>", "?"));
        
        if(!preamble.substring(0, 5).equals(SSE_FILEHEADER_PREFIX + "FE"))
        	version = 1;
        else version = preambleBuffer[5];
        
        if(version == 1 || version == 2)
        	return unzipAndDecryptFileV2(inputFile, mh, outputdir, version, logs);
        else if (version == 3 || version == 4)
        	return unzipAndDecryptFileV3Plus(inputFile, mh, outputdir, version, false, logs);
        else
        	throw new DataFormatException(textBundle.getString("ssecore.exception.InvalidVersion").replaceAll("<1>", Integer.toString(version)));
    }
    
    /** Decompress and Decrypt File V3 */
    public synchronized long unzipAndDecryptFileV3Plus(File inputFile, MessageHandler mh, File outputdir, int formatVersionCode, boolean onlyPasswordCheck, List<Object> logs) throws Exception
    {
    	if(!onlyPasswordCheck) checkThreadInterruption(mh);
    	long processTime = Calendar.getInstance().getTimeInMillis();
        
        ProgressMessage hm = new ProgressMessage();
    	hm.setProgressAbs(0);
    	
		long tempSizeCounter = 0;
		int fileCounter = 0;
		int dirCounter = 0;
		int crcErrorCounter = 0;
		String outputFilePath = null;
		boolean onlyOneFileArchive = false;
    	
    	final int BUFFER = 65536;
		byte buffer[] = new byte[BUFFER];
    	final int CHECKCODE_SIZE = 32;
    	final int SALT_SIZE = 32;
		byte preambleBuffer[] = new byte[SSE_FILEHEADER_SIZE];
		byte checkCode[] = new byte[CHECKCODE_SIZE];
		byte checkCodeEnd[] = new byte[CHECKCODE_SIZE];
		byte salt[] = new byte[SALT_SIZE];

        hm.setFullSize(inputFile.length());
			
        // FileInputStream
        InputStream fileInputStream = new FileInputStream(inputFile);
        fileInputStream.read(preambleBuffer);
        
        BufferedInputStream cipherInputStream = null;
        WithMAC macStream = null;

        int algorithmCode = preambleBuffer[6];
		decryptAlgorithmCode = algorithmCode;
			
		// Generate Key/nonce
		AlgorithmBean ab = getAvailableAlgorithms().get((Integer)decryptAlgorithmCode); 
		fileInputStream.read(salt);
		byte customParamsByte = preambleBuffer[7];
		List<byte[]> encParams = null;
		if(formatVersionCode == 3)
			encParams = deriveParamsArgon2id(salt, customParamsByte, ab.keySize / 8, ab.blockSize / 8, 64, Argon2Params.APP_CODE_FILE_ENCRYPTOR, 3);
		else
			encParams = deriveParamsArgon2id(salt, customParamsByte, ab.keySize / 8, ab.blockSize / 8, 32, Argon2Params.APP_CODE_FILE_ENCRYPTOR, 3);
		
    	// CheckCodeParserInputStream
    	CheckCodeParserInputStream checkCodeParserInputStream = new CheckCodeParserInputStream(
    			fileInputStream, inputFile.length() - SSE_FILEHEADER_SIZE - SALT_SIZE, false);
			
		// CipherInputStream
		boolean nativeCode = isDecNativeCodeAvailable();
		int parallelization = DynamicConfig.getCTRParallelizationPI();
		if (nativeCode) {
			if(formatVersionCode == 3) {
				macStream = new CipherInputStreamCTRPIV3(checkCodeParserInputStream, encParams.get(1), encParams.get(0), encParams.get(2), ab); // not NC
				cipherInputStream = new BufferedInputStream((CipherInputStreamCTRPIV3)macStream, BUFFER * 32 * parallelization);
			}
			else {
				macStream = new CipherInputStreamCTRNCPV4(checkCodeParserInputStream, encParams.get(1), encParams.get(0), encParams.get(2), ab);
				cipherInputStream = new BufferedInputStream((CipherInputStreamCTRNCPV4) macStream, BUFFER * 64);
			}
		}
		else {
			if(formatVersionCode == 3) {
				macStream = new CipherInputStreamCTRPIV3(checkCodeParserInputStream, encParams.get(1), encParams.get(0), encParams.get(2), ab);
				cipherInputStream = new BufferedInputStream((CipherInputStreamCTRPIV3)macStream, BUFFER * 32 * parallelization);
			} else {
				macStream = new CipherInputStreamCTRPIV4(checkCodeParserInputStream, encParams.get(1), encParams.get(0), encParams.get(2), ab);
				cipherInputStream = new BufferedInputStream((CipherInputStreamCTRPIV4) macStream, BUFFER * 32 * parallelization);
			}
		}
		
		// MAC
		macStream.addToMAC(preambleBuffer);
		macStream.addToMAC(salt);
	    	
	    cipherInputStream.read(checkCode);
	    if(Helpers.regexGetCountOf(checkCode, "[^a-z0-9]") > 0) 
	    {
	    	fileInputStream.close();
	    	((PWCipherInputStream)macStream).shutDownThreadExecutor();
	    	throw new InvalidParameterException(textBundle.getString("ssecore.exception.InvalidPassword"));
	    }
	    else if(onlyPasswordCheck) {
			fileInputStream.close();
			((PWCipherInputStream)macStream).shutDownThreadExecutor();
			processTime = Calendar.getInstance().getTimeInMillis() - processTime;
			return processTime;
		}
	    
    	// ZipArchiveInputStream
    	ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(cipherInputStream);
	    
		String sOutputFolder = (outputdir == null ? inputFile.getParentFile().getAbsolutePath() : outputdir.getAbsolutePath());
		ZipArchiveEntry ze = zipArchiveInputStream.getNextZipEntry();
		String postFix = "";
		String firstDir = "";
		String outputFileName = "";
		if(Helpers.convertToCurrentFileSeparator(ze.getName()).indexOf(File.separator) > -1)  // directory/file archive
		{
			postFix = getFilePostfix(Helpers.convertToCurrentFileSeparator(ze.getName()), sOutputFolder);
			firstDir = Helpers.getFirstDirFromFilepath(Helpers.convertToCurrentFileSeparator(ze.getName()));
			outputFileName = sOutputFolder + File.separator + firstDir + (postFix == null ? "" : postFix);			
		}
		else
		{
			outputFilePath = sOutputFolder + File.separator + Helpers.convertToCurrentFileSeparator(ze.getName());
			postFix = getFilePostfix(outputFilePath, null);
			if(postFix != null) outputFilePath = Helpers.replaceLastDot(outputFilePath, postFix);
			outputFileName = outputFilePath;
			onlyOneFileArchive = true;
		}
		
		while (ze != null) 
		{
			if(!onlyOneFileArchive)
			{
				String fileName = Helpers.convertToCurrentFileSeparator(ze.getName());
				if(postFix != null) fileName = fileName.replaceFirst(Pattern.quote(firstDir), Matcher.quoteReplacement(firstDir + postFix));
				outputFilePath = sOutputFolder + File.separator + fileName;
			}
			
			if(outputFilePath.endsWith(File.separator)) // directory
			{
				File tempDir = new File(outputFilePath);
				tempDir.mkdirs();
				ze = zipArchiveInputStream.getNextZipEntry();
				++dirCounter;
				continue;
			}
			++fileCounter;
						
			File newFile = new File(outputFilePath);
			new File(newFile.getParent()).mkdirs();

			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(newFile), BUFFER);

			CRC32 crc = new CRC32();
			crc.reset();
			
			int bytesIn;
				
			try {
				while ((bytesIn = zipArchiveInputStream.read(buffer)) > 0)
				{
					os.write(buffer, 0, bytesIn);
					crc.update(buffer, 0, bytesIn);
					long size = zipArchiveInputStream.getBytesRead();
					
		            if (size - tempSizeCounter > BUFFER)
		            {
		            	hm.setProgressAbs(size);
			        	// progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
		            	if(!hm.isRelSameAsLast()) {
		            		mh.removeLast();
		            		mh.setProgress(hm.getProgressRel());
		            		mh.print("\r" + getProgressTextDec(hm));
		            	}
			        	checkThreadInterruption(outputFilePath, mh); // check and prepare path for wiping
			        	tempSizeCounter = size;
		            }
				}
			} catch (IOException e) {
				os.flush();
				os.close();
				if(e.getMessage().equals("canceled")) throw new InterruptedException(textBundle.getString("ssecore.text.CanceledByUser") + "||" + newFile.getAbsolutePath());
				else throw new InterruptedException(e.getLocalizedMessage() + "||" + newFile.getAbsolutePath());
			}

			os.flush();
			os.close();

			ZipArchiveEntry zeLast = ze;
			ze = zipArchiveInputStream.getNextZipEntry();
			if(zeLast.getTime() > -1) newFile.setLastModified(zeLast.getTime()); 
			
			Short unixMode = Helpers.getUnixMode(zeLast);
			if(unixMode != null) Helpers.setUnixMode(newFile, unixMode);
			//if(unixMode == null) unixMode = -1; mh.println(newFile.getName() + " : " + Integer.toString(unixMode) + " : " + Helpers.getUnixMode(newFile));mh.println("");
					
			if(crc.getValue() != zeLast.getCrc()) {
				++crcErrorCounter;
				mh.print("\n" + textBundle.getString("ssecore.text.IntegrityErrorInFile") + ": " + zeLast.getName(), Color.RED, true);
				mh.println("");
			}
			zeLast = null;
		}
		
		// MAC
		byte[] mac = macStream.getMAC();
		checkCodeEnd = checkCodeParserInputStream.getCheckCode();
		
		hm.set100();
		if(!hm.isRelSameAsLast()) {
			mh.removeLast();
			mh.setProgress(hm.getProgressRel());
			mh.print("\r" + getProgressTextDec(hm));
		}
		zipArchiveInputStream.close();
		cipherInputStream.close();
		((PWCipherInputStream)macStream).close();
		fileInputStream.close();
		
		
		//System.out.println(Helpers.byteArrayToHexString(mac) + " : " + Helpers.byteArrayToHexString(checkCodeEnd));
		if(!Arrays.equals(mac, checkCodeEnd))
    		throw new DataFormatException("\n" + textBundle.getString("ssecore.exception.IncorrectMacFile"));
		
		processTime = Calendar.getInstance().getTimeInMillis() - processTime;
		
		File outputFileBase = new File(outputFileName);
		mh.println("");
		mh.println(textBundle.getString("ssecore.text.InputFile") + ": " + inputFile.getAbsolutePath(), null, false, true);
		if(outputFileBase.isDirectory())
			mh.println(textBundle.getString("ssecore.text.OutputFolder") + ": " + outputFileBase.getAbsolutePath(), null, false, true);
		else
			mh.println(textBundle.getString("ssecore.text.OutputFile") + ": " + outputFileBase.getAbsolutePath(), null, false, true);
		
		logs.add(outputFileBase.getAbsolutePath());
		
		Arrays.fill(encParams.get(0), (byte) 0);
		Arrays.fill(encParams.get(1), (byte) 0);
		Arrays.fill(encParams.get(2), (byte) 0);
		
		return crcErrorCounter;
    }  
    
    /** Generate Level 0 Password Hash (will be used for Argon/scrypt key derivation) */
    private void generateL0PasswordHash(char[] pw) throws GeneralSecurityException, UnsupportedEncodingException
	{
    	pw = convertToCodePoints(pw);
    	byte[] pwb = Helpers.toBytes(pw, false);
    	
    	l0PWHash = getSkeinHash(pwb, 1024);
    	l0PWHashB = getSHA3Hash(pwb, 512);
    	
    	// V3+
    	l0PWHashV3 = new byte[256];
    	HKDFParameters params = null;
    	HKDFBytesGenerator hkdf = null;
		
    	switch (purpose)
        {        	
        	case PURPOSE_TEXT_ENCRYPTION: 
            	params = new HKDFParameters(pwb, "memorySaltTE".getBytes("UTF-8"), "memoryInfoTE".getBytes("UTF-8"));
            	hkdf = new HKDFBytesGenerator(new SHA3Digest(512));
            	break;
            	
        	case PURPOSE_FILE_ENCRYPTION: 
            	params = new HKDFParameters(pwb, "memorySalt".getBytes("UTF-8"), "memoryInfo".getBytes("UTF-8"));
            	hkdf = new HKDFBytesGenerator(new SkeinDigest(SkeinDigest.SKEIN_1024, 1024));
            	break;

        	case PURPOSE_PASSWORD_VAULT: 
            	params = new HKDFParameters(pwb, "memorySaltPWV".getBytes("UTF-8"), "memoryInfoPWV".getBytes("UTF-8"));
            	hkdf = new HKDFBytesGenerator(new SkeinDigest(SkeinDigest.SKEIN_1024, 1024));
            	break;

        	default: 
            	break;
        }

        if(purpose > -1) {
	    	hkdf.init(params);
	        hkdf.generateBytes(l0PWHashV3, 0, l0PWHashV3.length);
        }
        else l0PWHashV3 = null;
        
        Arrays.fill(pw, '\u0000');
        Arrays.fill(pwb, (byte) 0);
	}
    
    public void wipeMasterKeys()
    {
    	if(l0PWHashV3 != null) Arrays.fill(l0PWHashV3, (byte) 0);
    	l0PWHashV3 = null;
    	if(l0PWHash != null) Arrays.fill(l0PWHash, (byte) 0);
    	l0PWHash = null;
    	if(l0PWHashB != null) Arrays.fill(l0PWHashB, (byte) 0);
    	l0PWHashB = null;
    }
    
    /** Generate Key/nonce from l0PWHash using scrypt - return List object where: index 0 = key; index 1 = nonce */
    private List<byte[]> deriveParamsScrypt(byte[] salt, int keyLength, int nonceLength, Integer applicationCode, Integer formatVersion, byte[] customPassword) throws GeneralSecurityException
    {
    	if(l0PWHash == null || l0PWHashV3 == null || l0PWHashB == null) throw new Error("MKE Error: Master Key is NULL");
    	
    	ScryptParams sp = null;
    	if(applicationCode == null || formatVersion == null)
    		sp = ScryptParams.getDefaultParameters();
    	else
    		sp = ScryptParams.getParameters(applicationCode, formatVersion);
    	
    	int dkLen = keyLength + nonceLength;   	
    	
    	byte[] output = null;
		
    	if(customPassword == null)
    		output = SCrypt.generate(l0PWHash, salt, sp.getN(), sp.getR(), sp.getP(), dkLen);
    	else
    		output = SCrypt.generate(customPassword, salt, sp.getN(), sp.getR(), sp.getP(), dkLen);
    	
    	List<byte[]> outputValues = new ArrayList<byte[]> ();
    	outputValues.add(Helpers.getSubarray(output, 0, keyLength));
    	outputValues.add(Helpers.getSubarray(output, keyLength, nonceLength));
    	
    	return outputValues;
    }
    
    /** Generate key/nonce/MAC key from l0PWHash using Argon2id - return List object where: index 0 = key; index 1 = nonce; index 2 = MAC key */
    private List<byte[]> deriveParamsArgon2id(byte[] salt, Byte customParamsByte, int keyLength, int nonceLength, Integer macKeyLength, Integer applicationCode, Integer formatVersion) throws GeneralSecurityException, UnsupportedEncodingException
    {
    	if(l0PWHash == null || l0PWHashV3 == null || l0PWHashB == null) throw new Error("MKE Error: Master Key is NULL");
    	
    	Argon2Params ap = Argon2Params.getParameters(applicationCode, formatVersion, customParamsByte); 	 	
    	
    	byte[] argonOutput = Argon2Provider.Argon2idHash(l0PWHashV3, salt, ap, 256);
    	
    	// Key
    	byte[] encKey = new byte[keyLength];
    	HKDFParameters params = new HKDFParameters(argonOutput, "encKeySalt".getBytes("UTF-8"), "encKeyInfo".getBytes("UTF-8"));
    	HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA3Digest(512));
        hkdf.init(params);
        hkdf.generateBytes(encKey, 0, encKey.length);
        
    	// Mac Key
    	byte[] authKey = null;
    	if(macKeyLength != null) {
	    	authKey = new byte[macKeyLength];
	    	params = new HKDFParameters(argonOutput, "authKeySalt".getBytes("UTF-8"), "authKeyInfo".getBytes("UTF-8"));
	    	hkdf = new HKDFBytesGenerator(new SHA3Digest(512));
	        hkdf.init(params);
	        hkdf.generateBytes(authKey, 0, authKey.length);
    	}
        
    	// nonce
    	byte[] nonce = new byte[nonceLength];
    	params = new HKDFParameters(argonOutput, "nonceSalt".getBytes("UTF-8"), "nonceInfo".getBytes("UTF-8"));
    	hkdf = new HKDFBytesGenerator(new SHA3Digest(512));
        hkdf.init(params);
        hkdf.generateBytes(nonce, 0, nonce.length);
    	
    	
    	List<byte[]> outputValues = new ArrayList<byte[]> ();
    	outputValues.add(encKey);
    	outputValues.add(nonce);
    	outputValues.add(authKey);
    	
    	return outputValues;
    }   
        
	private boolean isEncNativeCodeAvailable()
	{
		return NativeBlake3.isNativeCodeAvailable();
		//return availableAlgorithms.get(encryptAlgorithmCode).nativeCodeAvailable;
	}
	
	private boolean isDecNativeCodeAvailable()
	{
		return NativeBlake3.isNativeCodeAvailable();
		//return availableAlgorithms.get(decryptAlgorithmCode).nativeCodeAvailable;
	}
	
	/** Get Random String from given CharSet */
	public static String getRandomString(int length, String charSet)
	{	
		StringBuffer sBuffer = new StringBuffer(length);
		byte[] randomBytesBuffer = getRandomBA(length);		
		
		for(int i = 0; i < length; ++i) {
			sBuffer.append(Helpers.getCharFromChosenCharset(charSet, randomBytesBuffer[i]));
		}
		
		return sBuffer.toString();
	}
	
	/** Get Random (64 bytes max) */
	public static byte[] getRandomBA(int sizeInBytes)
	{	
		ThreadedSeedGenerator tsg = new ThreadedSeedGenerator();
		byte[] tsgOutput = tsg.generateSeed(64, true);
		byte[] timeOutput = String.valueOf(System.currentTimeMillis()).getBytes();
		byte[] seed1 = getSHA3Hash(Helpers.concat(tsgOutput, timeOutput, SecureRandom.getSeed(sizeInBytes)), 512);
		byte[] output = null;
		
		try {
			SecureRandom rand = new SecureRandom();
			rand.setSeed(seed1);
			byte[]randomNum = new byte[64];
			rand.nextBytes(randomNum);
			
			byte[] seed2 = ExtendedEntropyProvider.getSystemStateDataDigested();
			
			output = getSkeinHash(Helpers.concat(randomNum, seed2), sizeInBytes * 8);
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}  

		return output;
	}
	
	/** Fill availableAlgorithms Map with Available Algorithms */
	private void setAvailableAlgorithms()
	{
		List<AlgorithmBean> algorithms = algorithmsCodeBook();
		for (int i = 0; i < algorithms.size(); ++i)
		{
			AlgorithmBean ab = algorithms.get(i);
			availableAlgorithms.put(ab.innerCode, ab);
		}
	}
	
	/** Get proper File Name for File Encryptor output files */
	private static String getFilePostfix(String origPath, String prefixDir)
	{
		String postFix = null;
		if(prefixDir != null)
		{
			if((new File(prefixDir + File.separator + origPath)).exists())
			{
				for(int i = 1; i < 1000; ++i)
				{
					String tempPF = "_(" + i + ")";
					String firstDir = Helpers.getFirstDirFromFilepath(origPath);
					String tempPath = prefixDir + File.separator + origPath.substring(0, origPath.indexOf(firstDir)) + firstDir + tempPF;
					if((new File(tempPath)).exists()) continue;
					postFix = tempPF;
					break;
				}			
			}
		} 
		else 
		{
			if((new File(origPath)).exists())
			{
				for(int i = 1; i < 1000; ++i)
				{
					String tempPF = "_(" + i + ").";
					String tempPath = Helpers.replaceLastDot(origPath, tempPF);
					if((new File(tempPath)).exists()) continue;
					postFix = tempPF;
					break;
				}			
			}
		}
		return postFix;
	}
	
    /** Convert text to "char + unicode int representation string" */
    private char[] convertToCodePoints(char[] text)
    {
    	int size = 0;
    	
    	for (int i = 0; i < text.length; ++i)
    	{
    		int unicode = Character.codePointAt(text, i);
    		if(unicode > 126 || unicode < 32)
    		{
    			size += Helpers.getNumberOfDigits(unicode);
    		}
    		else
    		{
    			++size;
    		}
    		unicode = 0;
    	}
    	
    	char[] codePointsText = new char[size];
    	int counter = 0;
    	
    	for (int i = 0; i < text.length; ++i)
    	{
    		int unicode = Character.codePointAt(text, i);
    		if(unicode > 126 || unicode < 32)
    		{
	    		char[] currentPoints = Helpers.intToChars(unicode);
	    		for(int j = 0; j < currentPoints.length; ++j)
	    		{
	    			codePointsText[counter + j] = currentPoints[j];
	    		}
	    		counter += currentPoints.length;
	    		Arrays.fill(currentPoints, '\u0000'); 
    		}
    		else
    		{
    			codePointsText[counter] = text[i];
    			++counter;
    		}
    		unicode = 0;
    	}
    	
    	return codePointsText;
    }
    
    /** Get CRC32 of a file */
    public long getCRC32(File file) throws IOException
    {
    	final int BUFFER = 131072;
    	CRC32 crc = new CRC32();     
		byte[] crcBuffer = new byte[BUFFER];
		int crcRead;
		BufferedInputStream crcBis = new BufferedInputStream(new FileInputStream(file), BUFFER);
		crc.reset();
		while ((crcRead = crcBis.read(crcBuffer)) != -1) {
			crc.update(crcBuffer, 0, crcRead);
		}
		crcBis.close();
		return crc.getValue();	
    }
    
    /** Get CRC32 of a file */
    private long getCRC32(File file, ProgressMessage hm, MessageHandler mh) throws IOException, InterruptedException
    {    	
    	final int BUFFER = 131072;
    	CRC32 crc = new CRC32();
		byte[] crcBuffer = new byte[BUFFER];
		int crcRead;
		BufferedInputStream crcBis = new BufferedInputStream(new FileInputStream(file), BUFFER);
		crc.reset();
		while ((crcRead = crcBis.read(crcBuffer)) != -1) {
			crc.update(crcBuffer, 0, crcRead);
			
			hm.setProgressAbs(hm.getProgressAbs() + (long)(CRC_TIMECOEF * crcRead));
			//progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
			if(!hm.isRelSameAsLast()){
				mh.removeLast();
				mh.setProgress(hm.getProgressRel());
				mh.print("\r" + getProgressTextEnc(hm));
			}
			checkThreadInterruption(mh);
		}
		crcBis.close();
		return crc.getValue();	
    }
	
    private void checkThreadInterruption(MessageHandler mh) throws InterruptedException
    {
    	if (mh.interrupted()){
    		mh.interruptReset();
    		throw new InterruptedException(textBundle.getString("ssecore.text.CanceledByUser"));
    	}
    }
    
    private void checkThreadInterruption(String wipeFilePath, MessageHandler mh) throws InterruptedException
    {
    	if (mh.interrupted()){
    		mh.interruptReset();
    		throw new InterruptedException(textBundle.getString("ssecore.text.CanceledByUser") + "||" + wipeFilePath);
    	}
    }
    
    public static AlgorithmBean getAlgorithmBean(int code)
	{
		return ((Map<Integer, AlgorithmBean>)getAlgorithmsCodeBooks()[1]).get(code);
	}
    
    private static List<AlgorithmBean> algorithmsCodeBook()
	{
		return (List<AlgorithmBean>)(getAlgorithmsCodeBooks()[0]);
	}
    
	private static Object[] getAlgorithmsCodeBooks()
	{
		List<AlgorithmBean> algorithms = new ArrayList<AlgorithmBean>();
		Map<Integer, AlgorithmBean> algorithmMap = new HashMap<Integer, AlgorithmBean>();
		Object[] books = new Object[2];
		books[0] = algorithms;
		books[1] = algorithmMap;
		
		AlgorithmBean aes256 = new AlgorithmBean();
		aes256.code = ("AES-256");
		aes256.innerCode = 0;
		aes256.keySize = 256;
		aes256.blockSize = 128;
		aes256.shortComment = "AES-256";
		aes256.comment = "AES (256 bit)";
		algorithms.add(aes256);
		algorithmMap.put(aes256.innerCode, aes256);
		
		AlgorithmBean rc6256 = new AlgorithmBean();
		rc6256.code = ("RC6-256");
		rc6256.innerCode = 1;
		rc6256.keySize = 256;
		rc6256.blockSize = 128;
		rc6256.shortComment = "RC6-256";
		rc6256.comment = "RC6 (256 bit)";
		algorithms.add(rc6256);
		algorithmMap.put(rc6256.innerCode, rc6256);
		
		AlgorithmBean serpent256 = new AlgorithmBean();
		serpent256.code = ("Serpent-256");
		serpent256.innerCode = 2;
		serpent256.keySize = 256;
		serpent256.blockSize = 128;
		serpent256.shortComment = "SERPENT-256";
		serpent256.comment = "Serpent (256 bit)";
		algorithms.add(serpent256);
		algorithmMap.put(serpent256.innerCode, serpent256);
		
		AlgorithmBean bf256 = new AlgorithmBean();
		bf256.code = ("BLOWFISH-256");
		bf256.innerCode = 3;
		bf256.keySize = 256;
		bf256.blockSize = 64;
		bf256.shortComment = "BLOWFISH-256";
		bf256.comment = "Blowfish (256 bit)";
		algorithms.add(bf256);
		algorithmMap.put(bf256.innerCode, bf256);
		
		AlgorithmBean tf256 = new AlgorithmBean();
		tf256.code = ("TWOFISH-256");
		tf256.innerCode = 4;
		tf256.keySize = 256;
		tf256.blockSize = 128;
		tf256.shortComment = "TWOFISH-256";
		tf256.comment = "Twofish (256 bit)";
		algorithms.add(tf256);
		algorithmMap.put(tf256.innerCode, tf256);
		
		AlgorithmBean gost256 = new AlgorithmBean();
		gost256.code = ("GOST28147-256");
		gost256.innerCode = 5;
		gost256.keySize = 256;
		gost256.blockSize = 64;
		gost256.shortComment = "GOST-256";
		gost256.comment = "GOST28147 (256 bit)";
		algorithms.add(gost256);
		algorithmMap.put(gost256.innerCode, gost256);
		
		AlgorithmBean bf448 = new AlgorithmBean();
		bf448.code = ("BLOWFISH-448");
		bf448.innerCode = 6;
		bf448.keySize = 448;
		bf448.blockSize = 64;
		bf448.shortComment = "BLOWFISH-448";
		bf448.comment = "Blowfish (448 bit)";
		algorithms.add(bf448);
		algorithmMap.put(bf448.innerCode, bf448);
		
		AlgorithmBean threefish1024 = new AlgorithmBean();
		threefish1024.code = ("THREEFISH-1024");
		threefish1024.innerCode = 7;
		threefish1024.keySize = 1024;
		threefish1024.blockSize = 1024;
		threefish1024.tweakSize = 128;
		threefish1024.shortComment = "THREEFISH-1024";
		threefish1024.comment = "Threefish (1024 bit)";
		algorithms.add(threefish1024);
		algorithmMap.put(threefish1024.innerCode, threefish1024);
		
		AlgorithmBean shacal2 = new AlgorithmBean();
		shacal2.code = ("SHACAL2-512");
		shacal2.innerCode = 8;
		shacal2.keySize = 512;
		shacal2.blockSize = 256;
		shacal2.shortComment = "SHACAL2-512";
		shacal2.comment = "SHACAL-2 (512 bit)";
		algorithms.add(shacal2);
		algorithmMap.put(shacal2.innerCode, shacal2);

		AlgorithmBean c4 = new AlgorithmBean();
		c4.code = ("C4-2048");
		c4.innerCode = 9;
		c4.keySize = 2048;
		c4.blockSize = 1536; // nonce size
		c4.shortComment = "C4-2048";
		c4.comment = "Paranoia C4 (2048 bit)";
		c4.nestedAlgs = new int[]{7, 2, 0, 8};
		c4.keySplit = new int[]{128, 32, 32, 64};
		c4.nonceSplit = new int[]{128, 16, 16, 32};
		algorithms.add(c4);
		algorithmMap.put(c4.innerCode, c4);
		
		return books;
	}
		
    public static class AlgorithmBean 
    {    
        private String code;
        private int innerCode;
        private int keySize;
        private int blockSize;
        private int tweakSize = 0;
        private String shortComment = "";
        private String comment = "";
        private boolean nativeCodeAvailable = false;
        private int[] nestedAlgs = null;
		private int[] keySplit = null;
		private int[] nonceSplit = null;
		       
        public String getStringCode() {
			return code;
		}
        
        public int getInnerCode() {
			return innerCode;
		}
        
        public int getBlockSize() {
			return blockSize;
		}

		public int getBlockSizeInBytes() {
			return blockSize / 8;
		}
        
        public int getKeySize() {
			return keySize;
		}
        
        public int getTweakSize() {
			return tweakSize;
		}
        
        public boolean isNativeCodeAvailable() {
			return nativeCodeAvailable;
		}
		
        public String getComment() {
			return comment;
		}
        
        public String getShortComment() {
			return shortComment;
		}

		public int[] getNestedAlgs() {
			return nestedAlgs;
		}

		public int[] getKeySplit() {
			return keySplit;
		}

		public int[] getNonceSplit() {
			return nonceSplit;
		}
	}
    
    public static int positionToAlgCode(int position)
    {
    	Map<Integer, Integer> positionMap = new HashMap<Integer, Integer>();
    	positionMap.put(0, 0);
    	positionMap.put(1, 1);
    	positionMap.put(2, 2);
    	positionMap.put(3, 4);
    	positionMap.put(4, 5);
    	positionMap.put(5, 6);
    	positionMap.put(6, 7);
    	positionMap.put(7, 8);
    	positionMap.put(8, 9);
    	    	
    	return positionMap.get(position);
    }
    
    //+ ------------------- PC only --------------------
    private String getProgressTextDec(ProgressMessage hm)
    {
    	String progressText = textBundle.getString("ssecore.text.Decrypting") + " (" + getDecryptAlgorithmShortComment() + "): " + hm.getProgressRel() + "%";
    	return progressText;
    }
    
    private String getProgressTextEnc(ProgressMessage hm)
    {
    	String progressText = textBundle.getString("ssecore.text.Encrypting") + " (" + getEncryptAlgorithmShortComment() + "): " + hm.getProgressRel() + "%";
    	return progressText;
    }
	//- ------------------- PC only --------------------
    
    
	/**
	* Used for backwards compatibility with format versions 1 and 2
	*
	*      !---------- OBSOLETE METHODS ----------!
	*/ 
    
	public static final String SSE_VERSION = "10";
	private static final int TEXT_HEADERSIZE = 8;
	private static final int BASE_ALGORITHM_CODE_OFFSET = 25;  //+25 zipped
	private static final String MODE_CODES = "tcabnqmjsldriovwupkhxyezfgTCABNQMJSLDRIOVWUPKHXYEZFG"; // last 2char reserve
	private Map<String, byte[]> keysVault = new HashMap<String, byte[]>();
    
	/** Decrypt Text (from byte[]) - OBSOLETE Version 2 */
	private synchronized String decryptStringFromBytesVer2(byte[] input, byte configByte) throws Exception
	{
		String output = "-1";

		int compressed = (configByte >> 7) & 1;
		int algorithmCode = 8 * ((configByte >> 3) & 1) + 4 * ((configByte >> 2) & 1) + 2 * ((configByte >> 1) & 1) + ((configByte >> 0) & 1);
		
		if(compressed == 1) lastDecZipped = true;
		else lastDecZipped = false;
		
		decryptAlgorithmCode = algorithmCode;
		AlgorithmBean ab = getAvailableAlgorithms().get(decryptAlgorithmCode);
		if(ab == null) throw new NoSuchAlgorithmException();
		
		int saltSize = ab.blockSize;
		if(saltSize > 256) saltSize = 256;
		
		byte[] salt = Helpers.getSubarray(input, 0, saltSize / 8);
		input = Helpers.getSubarray(input, saltSize / 8, input.length - 1 - saltSize / 8);
		
		List<byte[]> encParams = deriveParamsScrypt(salt, ab.keySize / 8, salt.length, ScryptParams.APP_CODE_TEXT_ENCRYPTOR, 2, null);		
		byte[] outputBytes = unzipByteArray(decryptEAX(input, encParams.get(0), encParams.get(1), null), true);
		
		output = new String(outputBytes, "UTF-8");

	    return output;
	}
	
	/** Decrypt Text - OBSOLETE Version 1 */
	private synchronized String decryptStringVer1(String text) throws Exception
	{
		String output = "-1";
		byte[] input = text.trim().replaceAll("!", "-").getBytes();
		setDecryptAlgorithmCodefromOneCharCode((input[input.length - 1]));
		input = Helpers.getSubarray(input, 0, input.length - 1);
		Base64 base = new Base64(true);
	    byte[] encrypted = base.decode(input);
	    byte[] bOutput = unzipByteArray(decrypt(encrypted, getDecKey()), true);
	    if (checkMessageIntegrity(bOutput))
	    {
	    	bOutput = Helpers.getSubarray(bOutput, TEXT_HEADERSIZE, bOutput.length - TEXT_HEADERSIZE);
	    	output = new String(bOutput, "UTF-8");
	    }
	    	else throw new DataFormatException("Incorrect checksum");
	    return output;
	}
    
	/** Verify checksum - OBSOLETE Version 1 */
	private static boolean checkMessageIntegrity (byte[] bOutput)
	{	
	    boolean t = false;
		byte[] checksumOrg = Helpers.getSubarray(bOutput, 0, TEXT_HEADERSIZE);
	    byte[] checksumCur = getShortHash(Helpers.getSubarray(bOutput, TEXT_HEADERSIZE, bOutput.length - TEXT_HEADERSIZE));
	    if ((checksumOrg[1] == checksumCur[0]) && (checksumOrg[3] == checksumCur[2]))
	    	t = true;
	    return t;
	}
	
	/** Encrypt byte array and attach provided crc - OBSOLETE Version 1 */
	@Deprecated
	public byte[] encryptWithCRC(byte input[], String crc)
    {
		byte[] innerHeader = new byte[64];
		byte[] toEncrypt = new byte[input.length + innerHeader.length];
		innerHeader = Helpers.concat(getRandomBA(32), crc.getBytes());
		toEncrypt = Helpers.concat(innerHeader, input);
		innerHeader = null; input = null;		
		byte[] encOutput = encrypt(toEncrypt, getEncKey());		
		toEncrypt = null;		
		byte[] output = Helpers.concat(getOneCharCode(encryptAlgorithmCode, true).getBytes(), encOutput);   	
    	   	
    	return output;
    }
	
	/** Decrypt byte array and return crc in List - OBSOLETE Version 1 */
	public byte[] decryptWithCRC(byte input[], List<String> crc) throws Exception
    {
    	setDecryptAlgorithmCodefromOneCharCode(input[0]);
    	
    	byte[] decOutput = decrypt(Helpers.getSubarray(input, 1, input.length - 1), getDecKey());
    	input = null;
    	crc.add(new String(Helpers.getSubarray(decOutput, 32, 32)));
    	
    	return Helpers.getSubarray(decOutput, 64, decOutput.length - 64);
    }
	
    /** Generate password-base Keys (128, 256, 448, 512, 1024 bits) - OBSOLETE Version 1 */
    private void generatePBKeys1024Max(char[] pw, boolean unicodeAllowed) throws NoSuchAlgorithmException, InvalidKeySpecException 
	{
		if(unicodeAllowed) pw = convertToCodePoints(pw);
		byte[] pwb = Helpers.toBytes(pw, true);

    	byte[] shaL1 = getSHA512Hash(pwb);
    	byte[] skein1024 = getSkeinHash(pwb, 1024);
    	byte[] shaSalt = getSHA256Hash(Helpers.getSubarray(shaL1, 0, 8));
    	byte[] shaIV = getSHA256Hash(Helpers.getSubarray(shaL1, 8, 8));
    	
		PKCS12ParametersGenerator pGen = new PKCS12ParametersGenerator(new SHA1Digest());
		char[] passwordChars = pw;
		final byte[] pkcs12PasswordBytes = PBEParametersGenerator.PKCS12PasswordToBytes(passwordChars);
		pGen.init(pkcs12PasswordBytes, shaSalt, 600);
		CBCBlockCipher aesCBC = new CBCBlockCipher(new AESFastEngine());
		ParametersWithIV aesCBCParams = (ParametersWithIV) pGen.generateDerivedParameters(256, 128);
		aesCBC.init(false, aesCBCParams);		
		byte[] key = ((KeyParameter)aesCBCParams.getParameters()).getKey();
		
		byte[] k01 = Helpers.getSubarray(key, 0, 16);
		byte[] k02 = Helpers.getSubarray(key, 16, 16);
		keysVault.put("KS256", key);
		keysVault.put("KS128", Helpers.xorit(k01, k02));
		
		byte[] k31 = Helpers.getSubarray(shaL1, 40, 24);
		byte[] k32 = Helpers.concat(key, k31);
		keysVault.put("KS448", k32);
		
		byte[] skein1024key = getSkeinHash(key, 1024);
		byte[] key1024 = Helpers.xorit(skein1024, skein1024key);
		byte[] key512 = getSkeinHash(key1024, 512);
		keysVault.put("KS512", key512);
		keysVault.put("KS1024", key1024);
		
		
		String forIV = new String(getMD5Hash(shaIV));	
		PKCS12ParametersGenerator pGenIV = new PKCS12ParametersGenerator(new SHA1Digest());
		char[] ivChars = forIV.toCharArray();
		final byte[] pkcs12IVBytes = PBEParametersGenerator.PKCS12PasswordToBytes(ivChars);
		pGenIV.init(pkcs12IVBytes, getMD5Hash(shaSalt).getBytes(), 100);
		CBCBlockCipher aesCBC2 = new CBCBlockCipher(new AESFastEngine());
		ParametersWithIV aesCBCParams2 = (ParametersWithIV) pGenIV.generateDerivedParameters(256, 128);
		aesCBC2.init(false, aesCBCParams2);		
		byte[] keyIV = ((KeyParameter)aesCBCParams2.getParameters()).getKey();		
		
		byte[] k11 = Helpers.getSubarray(keyIV, 0, 16);
		byte[] k12 = Helpers.getSubarray(keyIV, 16, 16);
		byte[] ivTemp = Helpers.xorit(k11, k12);
		byte[] k21 = Helpers.getSubarray(ivTemp, 0, 8);
		byte[] k22 = Helpers.getSubarray(ivTemp, 8, 8);
		keysVault.put("IS128", ivTemp);
		keysVault.put("IS64", Helpers.xorit(k21, k22));
		keysVault.put("IS256", getSkeinHash(ivTemp, 256));
		keysVault.put("IS512", getSkeinHash(ivTemp, 512));
		keysVault.put("IS1024", getSkeinHash(ivTemp, 1024));
		
		if(unicodeAllowed) Arrays.fill(pw, '\u0000');
		Arrays.fill(pwb, (byte) 0); 
	 }
    
    /** Decompress and Decrypt File V2 and V1  - OBSOLETE Version 1 & 2 */
    public synchronized long unzipAndDecryptFileV2(File inputFile, MessageHandler mh, File outputdir, int formatVersionCode, List<Object> logs) throws Exception
    {
    	checkThreadInterruption(mh);
    	long processTime = Calendar.getInstance().getTimeInMillis();
        
        ProgressMessage hm = new ProgressMessage();
    	hm.setProgressAbs(0);
    	
		long tempSizeCounter = 0;
		int fileCounter = 0;
		int dirCounter = 0;
		int crcErrorCounter = 0;
		String outputFilePath = null;
		boolean onlyOneFileArchive = false;
    	
    	final int BUFFER = 65536;
		byte buffer[] = new byte[BUFFER];
    	final int CHECKCODE_SIZE = 32;
    	final int SALT_SIZE = 32;
		byte preambleBuffer[] = new byte[SSE_FILEHEADER_SIZE];
		byte checkCode[] = new byte[CHECKCODE_SIZE];
		byte checkCodeEnd[] = new byte[CHECKCODE_SIZE];
		byte randomPrefix[] = new byte[CHECKCODE_SIZE];
		byte salt[] = new byte[SALT_SIZE];

        hm.setFullSize(inputFile.length());
			
        // FileInputStream
        InputStream fileInputStream = new FileInputStream(inputFile);
        fileInputStream.read(preambleBuffer);
        String preamble = new String(preambleBuffer);
        
        BufferedInputStream cipherInputStream = null;
        if(formatVersionCode < 2) // version < 2
        {    	
			String codingMethod = preamble.substring(5, 6);	  
			setDecryptAlgorithmCodefromOneCharCode((byte)codingMethod.charAt(0));    	
	    	
	    	// CipherInputStream
			cipherInputStream = new BufferedInputStream(new CipherInputStreamPI(fileInputStream, getDecIVBytes(), getDecKey(), getDecryptAlgorithmCode()), BUFFER * 2);
	    	
	    	cipherInputStream.read(randomPrefix); // remove random prefix
	    	cipherInputStream.read(checkCode);
	    	if(Helpers.regexGetCountOf(checkCode, "[^a-z0-9]") > 0)
	    	{
	    		fileInputStream.close();
	    		throw new InvalidParameterException(textBundle.getString("ssecore.exception.InvalidPassword"));
	    	}
        }
        else // version = 2
        {
        	int algorithmCode = preambleBuffer[6];
			decryptAlgorithmCode = algorithmCode;
			
			// Generate Key/IV
			AlgorithmBean ab = getAvailableAlgorithms().get((Integer)decryptAlgorithmCode); 
			fileInputStream.read(salt);
			List<byte[]> encParams = deriveParamsScrypt(salt, ab.keySize / 8, ab.blockSize / 8, ScryptParams.APP_CODE_FILE_ENCRYPTOR, 2, null);     	
			
			// CipherInputStream
			cipherInputStream = new BufferedInputStream(new CipherInputStreamPI(fileInputStream, encParams.get(1), encParams.get(0), getDecryptAlgorithmCode()), BUFFER * 2);
	    	
	    	cipherInputStream.read(checkCode);
	    	if(Helpers.regexGetCountOf(checkCode, "[^a-z0-9]") > 0) 
	    	{
	    		fileInputStream.close();
	    		throw new InvalidParameterException(textBundle.getString("ssecore.exception.InvalidPassword"));
	    	}
        }
	    
    	// CheckCodeParserInputStream
    	CheckCodeParserInputStream checkCodeParserInputStream = new CheckCodeParserInputStream(
    			cipherInputStream, inputFile.length() - SSE_FILEHEADER_SIZE - (2 * CHECKCODE_SIZE), true);
	    
    	// ZipArchiveInputStream
    	ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(checkCodeParserInputStream);
	    
		String sOutputFolder = (outputdir == null ? inputFile.getParentFile().getAbsolutePath() : outputdir.getAbsolutePath());
		ZipArchiveEntry ze = zipArchiveInputStream.getNextZipEntry();
		String postFix = "";
		String firstDir = "";
		String outputFileName = "";
		if(Helpers.convertToCurrentFileSeparator(ze.getName()).indexOf(File.separator) > -1)  // directory/file archive
		{
			postFix = getFilePostfix(Helpers.convertToCurrentFileSeparator(ze.getName()), sOutputFolder);
			firstDir = Helpers.getFirstDirFromFilepath(Helpers.convertToCurrentFileSeparator(ze.getName()));
			outputFileName = sOutputFolder + File.separator + firstDir + (postFix == null ? "" : postFix);			
		}
		else
		{
			outputFilePath = sOutputFolder + File.separator + Helpers.convertToCurrentFileSeparator(ze.getName());
			postFix = getFilePostfix(outputFilePath, null);
			if(postFix != null) outputFilePath = Helpers.replaceLastDot(outputFilePath, postFix);
			outputFileName = outputFilePath;
			onlyOneFileArchive = true;
		}
		
		while (ze != null) 
		{
			if(!onlyOneFileArchive)
			{
				String fileName = Helpers.convertToCurrentFileSeparator(ze.getName());
				if(postFix != null) fileName = fileName.replaceFirst(Pattern.quote(firstDir), Matcher.quoteReplacement(firstDir + postFix));
				outputFilePath = sOutputFolder + File.separator + fileName;
			}
			
			if(outputFilePath.endsWith(File.separator)) // directory
			{
				File tempDir = new File(outputFilePath);
				tempDir.mkdirs();
				ze = zipArchiveInputStream.getNextZipEntry();
				++dirCounter;
				continue;
			}
			++fileCounter;
			
			//SSElog.d("outputFilePath A", outputFilePath + " : " + postFix + " : " + fileCounter);
			
			File newFile = new File(outputFilePath);
			new File(newFile.getParent()).mkdirs();

			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(newFile), BUFFER);

			CRC32 crc = new CRC32();
			crc.reset();
			
			int bytesIn;
			while ((bytesIn = zipArchiveInputStream.read(buffer)) > 0)
			{
				os.write(buffer, 0, bytesIn);
				crc.update(buffer, 0, bytesIn);
				long size = zipArchiveInputStream.getBytesRead();
				
	            if (size - tempSizeCounter > BUFFER)
	            {
	            	hm.setProgressAbs(size);
		        	// progressHandler.sendMessage(Message.obtain(progressHandler, -1100, hm));
	            	if(!hm.isRelSameAsLast()) {
	            		mh.removeLast();
	            		mh.setProgress(hm.getProgressRel());
	            		mh.print("\r" + getProgressTextDec(hm));
	            	}
		        	checkThreadInterruption(outputFilePath, mh); // check and prepare path for wiping
		        	tempSizeCounter = size;
	            }
			}

			os.flush();
			os.close();

			ZipArchiveEntry zeLast = ze;
			ze = zipArchiveInputStream.getNextZipEntry();
			//mh.println("\nCRC3: " + crc.getValue() + " : " + zeLast.getCrc());
			if(zeLast.getTime() > -1) newFile.setLastModified(zeLast.getTime()); //TODO Executable Files
			if(crc.getValue() != zeLast.getCrc()) {
				++crcErrorCounter;
				mh.print("\n" + textBundle.getString("ssecore.text.IntegrityErrorInFile") + ": " + zeLast.getName(), Color.RED, true);
				mh.println("");
			}
			zeLast = null;
		}
		
		checkCodeEnd = checkCodeParserInputStream.getCheckCode();
		
		hm.set100();
		if(!hm.isRelSameAsLast()) {
			mh.removeLast();
			mh.setProgress(hm.getProgressRel());
			mh.print("\r" + getProgressTextDec(hm));
		}
		zipArchiveInputStream.close();
		cipherInputStream.close();
		fileInputStream.close();
		
		//SSElog.d("checkCode", "" + new String(checkCode) + " : " + new String(checkCodeEnd));
    	if(!(new String(checkCode).equals(new String(checkCodeEnd)))) throw new DataFormatException(textBundle.getString("ssecore.exception.InvalidChecksum")); // Text for old version with checksum
		
		processTime = Calendar.getInstance().getTimeInMillis() - processTime;
		
		File outputFileBase = new File(outputFileName);
		mh.println("");
		mh.println(textBundle.getString("ssecore.text.InputFile") + ": " + inputFile.getAbsolutePath(), null, false, true);
		if(outputFileBase.isDirectory())
			mh.println(textBundle.getString("ssecore.text.OutputFolder") + ": " + outputFileBase.getAbsolutePath(), null, false, true);
		else
			mh.println(textBundle.getString("ssecore.text.OutputFile") + ": " + outputFileBase.getAbsolutePath(), null, false, true);
		
		logs.add(outputFileBase.getAbsolutePath());
		
		return crcErrorCounter;
    }  
    
	/** Convert "Algorithm OneCharCode" (used in the "sse" encrypted texts and files) to Algorithm Code and set it as current - OBSOLETE Version 1 */
	private void setDecryptAlgorithmCodefromOneCharCode(byte ch) throws NoSuchAlgorithmException
	{
		int c = MODE_CODES.indexOf(ch);
		if (c >= BASE_ALGORITHM_CODE_OFFSET)
		{
			lastDecZipped = true;
			decryptAlgorithmCode = c - BASE_ALGORITHM_CODE_OFFSET;
		}
		else 
		{
			lastDecZipped = false;
			decryptAlgorithmCode = c;
		}
		
		AlgorithmBean testAb = getAvailableAlgorithms().get((Integer)decryptAlgorithmCode);
		if(testAb == null) throw new NoSuchAlgorithmException();	
	}
	
	/** Encrypt byte array - OBSOLETE Version 1 */
	public byte[] encrypt(byte input[], boolean compress)
    {
        if(compress) return encrypt(zipByteArray(input, false), getEncKey());
        else return encrypt(input, getEncKey());
    }

	/** Decrypt byte array - OBSOLETE Version 1 */
	public byte[] decrypt(byte input[], boolean decompress) throws Exception
    {
    	if(decompress) return unzipByteArray(decrypt(input, getDecKey()), false);
    	else return decrypt(input, getDecKey());
    }
	
    /** Encrypt Byte Array Execution - OBSOLETE Version 1 */
	private byte[] encrypt(byte[] inputText, byte[] key)
    {  	
    	byte[] output = null; 
	    
		try {
			int bytesProcessed;
			
			BufferedBlockCipher cipher = CipherProvider.getBufferedBlockCipher(true, getEncIVBytes(), key, getEncryptAlgorithmCode());
			output = new byte[cipher.getOutputSize(inputText.length)];
			bytesProcessed = cipher.processBytes(inputText, 0, inputText.length, output, 0);
			bytesProcessed = cipher.doFinal(output, bytesProcessed);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		Arrays.fill(key, (byte) 0);
				
		return output;
    }
	
	/** Decrypt Byte Array Execution - OBSOLETE Version 1 */
	private byte[] decrypt(byte[] inputText, byte[] key) throws Exception
    {  	
    	byte[] output = null;

		try {
			int length = 0;
			int bytesProcessed;
			
			BufferedBlockCipher cipher = CipherProvider.getBufferedBlockCipher(false, getDecIVBytes(), key, getDecryptAlgorithmCode());
			byte[] buffer = new byte[cipher.getOutputSize(inputText.length)];
			bytesProcessed = cipher.processBytes(inputText, 0, inputText.length, buffer, 0);
			length += bytesProcessed;
			bytesProcessed = cipher.doFinal(buffer, length);
			length += bytesProcessed;
			
	  	    output = new byte[length];
	  	    System.arraycopy(buffer, 0, output, 0, length);

		} catch (Exception e) {
			throw e;
		} 
		
		Arrays.fill(key, (byte) 0);

		return output;
    }
	
	/** Get MD5 of text in String - OBSOLETE Version 1 */
	public static String getMD5Hash (String text)
	{
		return getMD5Hash(text.getBytes());
	}
	
	/** Get MD5 of Byte Array - OBSOLETE Version 1 */
	public static String getMD5Hash (byte[] text)
	{
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    m.update(text, 0, text.length);
	    String hash = new BigInteger(1, m.digest()).toString(16);
		while (hash.length() < 32) hash = "0" + hash;
		return hash.toLowerCase();
	}
    
	/** Get Short (4B) hash of Byte Array - OBSOLETE Version 1 */
	public static byte[] getShortHash (byte[] data)
	{
		int pieceSize = 4;
		String md5s = getMD5Hash(data).toUpperCase();
		byte[] md5bin = new byte[16];
		List<byte[]> hashPiece = new ArrayList<byte[]>();
		
		for(int i = 0; i < md5s.length(); i += 2)
		{
			md5bin[i / 2] = Integer.valueOf(md5s.substring(i, i + 2), 16).byteValue();
		}
				
		for(int i = 0; i < md5bin.length; i += pieceSize)
		{
			byte[] temp = new byte[pieceSize];
				for(int j = 0; j < pieceSize; ++j)
				{
					temp[j] = md5bin[j + i];
				}
			hashPiece.add(temp);
		}
		
		byte[] outputBytes = hashPiece.get(0);
		for (int i = 1; i < hashPiece.size(); ++i)
		{
			outputBytes = Helpers.xorit(outputBytes, hashPiece.get(i));
		}
		
		return outputBytes;
	}
	
	/** OBSOLETE Version 1 */
	private String getOneCharCode(int ac, boolean zipped)
	{
		int tempEAC = ac;
		if(zipped) tempEAC += BASE_ALGORITHM_CODE_OFFSET;
		return Character.toString(MODE_CODES.charAt(tempEAC));
	}
	
	/** OBSOLETE Version 1 */
	private byte[] getEncKey()
	{
		int size = availableAlgorithms.get(encryptAlgorithmCode).keySize;
		String kCode = "KS" + Integer.toString(size);
		byte[] key = keysVault.get(kCode);
		return key;
	}
	
	/** OBSOLETE Version 1 */
	private byte[] getDecKey()
	{
		int size = availableAlgorithms.get(decryptAlgorithmCode).keySize;
		String kCode = "KS" + Integer.toString(size);
		byte[] key = keysVault.get(kCode);
		return key;
	}
	
	/** OBSOLETE Version 1 */
	private byte[] getEncIVBytes()
	{
		int size = availableAlgorithms.get(encryptAlgorithmCode).blockSize;
		String ivCode = "IS" + Integer.toString(size);
		byte[] iv = keysVault.get(ivCode);
		return iv;
	}
	
	/** OBSOLETE Version 1 */
	private byte[] getDecIVBytes()
	{
		int size = availableAlgorithms.get(decryptAlgorithmCode).blockSize;
		String ivCode = "IS" + Integer.toString(size);
		byte[] iv = keysVault.get(ivCode);
		return iv;
	}
}
