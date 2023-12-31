package com.paranoiaworks.sse.dao;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.paranoiaworks.sse.Encryptor;
import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.checklic.Parcl;

/**
 * Keeps Application Settings
 * 
 * @author Paranoia Works
 * @version 1.0.7
 */
public class SettingsDataHolder implements Serializable{

	private static final long serialVersionUID = 10L;	
	private Map<String, Object> persistentDataObjectMap;
	private byte[] licenseBytes = new byte[1];
	private transient Map<String, Object> sessionDataObjectMap;
	private transient String errorMessage;
	private transient File sdhFile;
	
	private SettingsDataHolder(File file) 
	{
		this.sdhFile = file;
	}	
	
	/** Get Persistent Data Object as Integer */
	public Integer getPersistentDataInteger(String key)
	{
		Integer item = null;
		try {
			item = (Integer)getPersistentDataObject(key);
		} catch (Exception e) {}
		
		return item;
	}
	
	public Integer getPersistentDataInteger(String key, Integer defValue)
	{
		Integer value = getPersistentDataInteger(key);
		if(value == null) return defValue;
		else return value;
	}
	
	/** Get Persistent Data Object as Boolean */
	public Boolean getPersistentDataBoolean(String key)
	{
		Boolean item = null;
		try {
			item = (Boolean)getPersistentDataObject(key);
		} catch (Exception e) {}
		
		return item;
	}
	
	public Boolean getPersistentDataBoolean(String key, Boolean defValue)
	{
		Boolean value = getPersistentDataBoolean(key);
		if(value == null) return defValue;
		else return value;
	}
	
	/** Get Persistent Data Object as String */
	public String getPersistentDataString(String key)
	{
		String item = null;
		try {
			item = (String)getPersistentDataObject(key);
		} catch (Exception e) {}
		
		return item;
	}
	
	public String getPersistentDataString(String key, String defValue)
	{
		String value = getPersistentDataString(key);
		if(value == null) return defValue;
		else return value;
	}

	/** Get Persistent Data Object */
	public Object getPersistentDataObject(String key)
	{
		if(persistentDataObjectMap == null ) createPersistentDataObjectMap();
		return persistentDataObjectMap.get(key);
	}
	
	/** Add or Replace Persistent Data Object */
	public void addOrReplacePersistentDataObject(String key, Object dataObject)
	{
		if(persistentDataObjectMap == null ) createPersistentDataObjectMap();
		Object test = persistentDataObjectMap.get(key);
		if(test != null) persistentDataObjectMap.remove(key);
		persistentDataObjectMap.put(key, dataObject);
	}
	
	
	/** Get Session Data Object */
	public Object getSessionDataObject(String key)
	{
		if(sessionDataObjectMap == null ) createSessionDataObjectMap();
		return sessionDataObjectMap.get(key);
	}
	
	/** Add or Replace Session Data Object */
	public void addOrReplaceSessionDataObject(String key, Object dataObject)
	{
		if(sessionDataObjectMap == null ) createSessionDataObjectMap();
		Object test = sessionDataObjectMap.get(key);
		if(test != null) sessionDataObjectMap.remove(key);
		sessionDataObjectMap.put(key, dataObject);
	}
	
	public void setErrorMessage(String message) 
	{
		this.errorMessage = message;
	}
	
	public String getErrorMessage()
	{
		return this.errorMessage;
	}
	
	public byte[] getLicenseBytes() {
		return this.licenseBytes;
	}
	
	public void setLicenseBytes(byte[] licenseBytes) throws Exception {
		this.licenseBytes = licenseBytes;
		save();
	}
	
	public String getConfigDirPath() 
	{
		String path = null;
		try {
			path = sdhFile.getParentFile().getAbsolutePath();
		} catch (Exception e) {
			// swallow
		}
		
		return path;
	}
	
	private void createSessionDataObjectMap()
	{
		sessionDataObjectMap = Collections.synchronizedMap(new HashMap<String, Object>());
	}
	
	private void createPersistentDataObjectMap()
	{
		persistentDataObjectMap = Collections.synchronizedMap(new HashMap<String, Object>());
	}
	
	/** Load SettingDataHolder object from File Name*/
	public static SettingsDataHolder getSettingsDataHolder(String fileName) 
	{
		return getSettingsDataHolder(fileName, false, null);
	}
	
	/** Load SettingDataHolder object from File Name*/
	public static SettingsDataHolder getSettingsDataHolder(String fileName, boolean readOnly) 
	{
		return getSettingsDataHolder(fileName, readOnly, null);
	}
	
	/** Load SettingDataHolder object from File Name*/
	public static SettingsDataHolder getSettingsDataHolder(String fileName, boolean readOnly, String customPath) 
	{
		Map<String, String> errors = new HashMap<String, String>();
		File configFile = getConfigFile(fileName, errors, readOnly, customPath);
		SettingsDataHolder sdh = SettingsDataHolder.getSettingsDataHolder(configFile); 
		
		String error = errors.get(configFile.getAbsolutePath());
		
		if(error != null )
			sdh.setErrorMessage(error);
		
		return sdh;
	}

	/** Load SettingDataHolder object from File*/
	private static SettingsDataHolder getSettingsDataHolder(File file) 
	{
		SettingsDataHolder settingsDataHolder = null;
		byte[] sdhBytes = null;
		String errorMessage = null;
		try {
			if(file.exists()) {
				sdhBytes = Helpers.loadBytesFromFile(file);
				if(sdhBytes.length > 1) {
					sdhBytes[0] = (byte)0x1F; 
					sdhBytes[1] = (byte)0x8B;
				}
				settingsDataHolder = (SettingsDataHolder)Encryptor.unzipObject(sdhBytes, null);
				settingsDataHolder.sdhFile = file;
			}			
		} catch (Exception e) {
			errorMessage = ("Error reading config file!\n");
			errorMessage += ("Path: " + file.getAbsolutePath() + "\n");
			errorMessage += ("Bytes: " + (sdhBytes != null ? sdhBytes.length : "null") + "\n");
			System.out.println(errorMessage);
			System.out.println("Content: " + (sdhBytes != null ? Helpers.byteArrayToHexString(sdhBytes) : "null") + "\n");
			e.printStackTrace();
			errorMessage = errorMessage.replaceAll("\n", "<br/>");
		} 
		if(settingsDataHolder == null) settingsDataHolder = new SettingsDataHolder(file);
		if(errorMessage != null) {
			settingsDataHolder.setErrorMessage(errorMessage);
		}
		
		return settingsDataHolder;
	}
	
	/** Save SettingDataHolder object to File*/
	public synchronized void save() throws Exception
	{
    	byte[] sdhBytes;
    	sdhBytes = Encryptor.zipObject(this, null);
    	if(sdhBytes.length > 1) {
			sdhBytes[0] = 'P'; 
			sdhBytes[1] = 'W';
		}
		Helpers.saveBytesToFile(this.sdhFile, sdhBytes);
		
		if(!this.sdhFile.isHidden()) {
			try {
				Path path = Paths.get(this.sdhFile.getAbsolutePath());
				Files.setAttribute(path, "dos:hidden", true, LinkOption.NOFOLLOW_LINKS);
			} catch (Exception e) {
				// swallow
			}
		}	
	}
	
	/** Get Configuration File */
	private static File getConfigFile(String fileName, Map<String, String> errors, boolean readonly, String customPath)
	{
		File configFile = null;
		
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource(".");
			File directory = null;
			try {
				directory = new File(url.toURI());
			} catch(URISyntaxException e) {
				directory = new File(url.getPath());
			}
			
			String primaryPath = customPath == null ? directory.getAbsolutePath() : customPath;
			configFile = new File(primaryPath + System.getProperty("file.separator") + "." + fileName);
			if(!configFile.exists()) 
				configFile = new File(primaryPath + System.getProperty("file.separator") + fileName);
			configFile = normalizeFile(configFile);
			
		} catch (Exception e) {
			// swallow
		}
				
		File configFileAlt = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "." + fileName);
		if(!configFileAlt.exists()) 
			configFileAlt = new File(System.getProperty("user.home") + System.getProperty("file.separator") + fileName);
		configFileAlt = normalizeFile(configFileAlt);
		
		
		SettingsDataHolder sdh = null;
		SettingsDataHolder sdhAlt = null;
		
		boolean sdhWritable = false;
		boolean sdhAltWritable = false;
		boolean sdhPro = false;
		boolean sdhAltPro = false;
		
		int useFileIndex = 0;		

		try {
			sdh = getSettingsDataHolder(configFile);
			if(sdh.getErrorMessage() != null)
				errors.put(configFile.getAbsolutePath(), sdh.getErrorMessage());
			sdhPro = Parcl.loadcheck(sdh);
			if(!readonly) { 
				sdh.save();
				sdhWritable = true;
			}
		} catch (Exception e) {
			// swallow
		}
		
		try {
			sdhAlt = getSettingsDataHolder(configFileAlt);
			if(sdh.getErrorMessage() != null)
				errors.put(configFileAlt.getAbsolutePath(), sdh.getErrorMessage());
			sdhAltPro = Parcl.loadcheck(sdhAlt);
			if(!sdhWritable && !readonly) {
				sdhAlt.save();
				sdhAltWritable = true;
			}			
		} catch (Exception e) {
			// swallow
		}

		if(sdhWritable) useFileIndex = 1;
		else if(sdhAltWritable) useFileIndex = 2;
		else if(sdh != null && Parcl.loadcheck(sdh)) useFileIndex = 1;
		else if(sdhAlt != null && Parcl.loadcheck(sdhAlt)) useFileIndex = 2;
		
		if(useFileIndex == 1 && !sdhPro && sdhAltPro) 
		{
			try {
				sdh.setLicenseBytes(sdhAlt.getLicenseBytes());
			} catch (Exception e) {
				// swallow
			}
		}
		else if(useFileIndex == 2 && sdhPro && !sdhAltPro) 
		{
			try {
				sdhAlt.setLicenseBytes(sdh.getLicenseBytes());
			} catch (Exception e) {
				// swallow
			}
		}
		
		if(useFileIndex == 1) return configFile;
		else if (useFileIndex == 2) return configFileAlt;
		else return normalizeFile(new File(fileName));
	}
	
	private static File normalizeFile(File file)
	{
		File outputFile = file;
		
		if(!file.getName().startsWith(".")) {
			File newFile = new File(file.getParent(), "." + file.getName());
			try {
				if(file.exists()) Files.move(file.toPath(), newFile.toPath());
				outputFile = newFile;
			} catch (IOException e) {
				// swallow
			}	
		}
		
		return outputFile;
	}
}
