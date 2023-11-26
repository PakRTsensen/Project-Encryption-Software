package com.paranoiaworks.sse;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.PlainDocument;

import org.apache.commons.compress.archivers.zip.ParanoiaExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipExtraField;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import com.paranoiaworks.sse.components.OkCancelDialog;

import f5.engine.RawImage;
import steos.jnafilechooser.api.JnaFileChooser;


public class Helpers {
	
	public static final String DATE_FORMAT = "dd/MM/yyyy HH:mm";
	public static final String DATE_FORMAT_DATEONLY = "dd/MM/yyyy";
	public static final String REGEX_REPLACEALL_LASTDOT = "\\.(?!.*\\.)";
	
	public static final String UNIX_FILE_SEPARATOR = "/";
	public static final String WINDOWS_FILE_SEPARATOR = "\\";
	
	
	public static byte[] xorit(byte[] text, byte[] passPhrase)
	{		
		if (passPhrase.length == 0) passPhrase = "x".getBytes();
		byte[] outputBuffer = new byte[text.length];
		int counter = 0;
		for (int i = 0; i < text.length; ++i)
		{
			byte a = text[i];
			byte b = passPhrase[counter];
			outputBuffer[i] = (byte)(a ^ b);	
			++counter;
			if (counter == passPhrase.length) counter = 0;
		}		
		return outputBuffer;
	}
	
	public static byte[] concat(byte[]... args) 
	{
		int fulllength = 0;
		for (byte[] arrItem : args) 
		{
			fulllength += arrItem.length;
        }
		byte[] retArray = new byte[fulllength];
		int start = 0;
		for (byte[] arrItem : args) 
		{
			System.arraycopy(arrItem, 0, retArray, start, arrItem.length);
			start += arrItem.length;
		}
		return retArray;
	}
	
	public static byte[] getSubarray(byte[] array, int offset, int length) 
	{
		byte[] result = new byte[length];
		System.arraycopy(array, offset, result, 0, length);
		return result;
	}

	public static String removeExt (String fileName, String extension)
    {
    	String name = fileName;
    	if (fileName.endsWith("." + extension))
    		name = name.substring(0, name.lastIndexOf('.')); 		
    	return name;
    }
	
	public static String getFirstDirFromFilepath(String filepath)
    {
		filepath = convertToCurrentFileSeparator(filepath);
		String[] temp = filepath.split(Pattern.quote(File.separator));
    	if(temp[0].equals("") && temp.length > 1) return temp[1];
    	return temp[0];
    }
	
	public static String getFirstDirFromFilepathWithLFS(String filepath) //leading file separator (/...)
    {
		filepath = convertToCurrentFileSeparator(filepath);
		if(regexGetCountOf(filepath, File.separator) == 1) return filepath;
		String[] temp = filepath.split(Pattern.quote(File.separator));
    	if(temp[0].equals("") && temp.length > 1) return File.separator + temp[1];
    	return File.separator + temp[0];
    }
	
	public static String[] listToStringArray (List<String> strings)
    {
		String[] sList = new String[strings.size()];
		for(int i = 0; i < strings.size(); ++i)
		sList[i] = strings.get(i);
		return sList;
    }
	
	public static String[] fileListToNameStringArray (List<File> files)
    {
		String[] sList = new String[files.size()];
		for(int i = 0; i < files.size(); ++i)
		sList[i] = files.get(i).getName();
		return sList;
    }
	
	public static boolean isSubDirectory(File base, File child) 
	{
		  try {
			  if(!base.isDirectory())
				  return false;
			  
			  base = base.getCanonicalFile();
			  child = child.getCanonicalFile();

			  File parentFile = child;
			  while (parentFile != null) 
			  {
				  if(base.getAbsolutePath().length() > parentFile.getAbsolutePath().length())
					  break;
				  
				  if (base.equals(parentFile)) {
					  return true;
				  }
				  parentFile = parentFile.getParentFile();
			  }
		  } catch (Exception e) {
				// swallow
		  }

		  return false;
	}
	
	public static long[] getDirectorySize(File directory, ResourceBundle configBundle) 
	{
		int totalFolder = 0, totalFile = 0;
		long[] sizes = new long[3]; // all, compressible, uncompressible

		totalFolder++; 
		File[] filelist = directory.listFiles();
		if(filelist == null) return null;
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				long[] tempSize = getDirectorySize(filelist[i], configBundle);
				sizes[0] += tempSize[0];
				sizes[1] += tempSize[1];
				sizes[2] += tempSize[2];
			} else {
				totalFile++;
				long tempSize = filelist[i].length();;
				sizes[0] += tempSize;
				if(isCompressible(filelist[i], configBundle)) sizes[1] += tempSize;
				else sizes[2] += tempSize;
			}
		}
		
		return sizes;
	}
	
	public static long getDirectorySizeWithInterruptionCheck(File directory) throws InterruptedException 
	{
		int totalFolder = 0, totalFile = 0;
		long foldersize = 0;

		totalFolder++; 
		File[] filelist = directory.listFiles();
		if(filelist == null) throw new InterruptedException("DirectorySize: FileList is NULL");
		for (int i = 0; i < filelist.length; i++) 
		{
			if (filelist[i].isDirectory()) 
			{
				long tempSize = getDirectorySizeWithInterruptionCheck(filelist[i]);
				if(tempSize == -1) return -1;
				foldersize += tempSize;
			} else {
				totalFile++;
				foldersize += filelist[i].length();
			}
			if (Thread.interrupted())
			{
				throw new InterruptedException("DirectorySize: Thread Interrupted");
			}
		}
		return foldersize;
	}
	
	public static boolean isCompressible(File file, ResourceBundle configBundle)
	{
		String fileExt = getFileExt(file);
		fileExt = "|" + fileExt.toLowerCase()+ "|";

		return !(configBundle.getString("ssecore.config.uncompressible").indexOf(fileExt) > -1);
	}
	
	public static boolean deleteDirectory(File directory) 
	{
		if (directory == null)
			return false;
		if (!directory.exists())
			return true;
		if (!directory.isDirectory())
			return false;

		String[] list = directory.list();

		if (list != null) 
		{
			for (int i = 0; i < list.length; i++) 
			{
				File entry = new File(directory, list[i]);

				if (entry.isDirectory())
				{
					if (!deleteDirectory(entry))
						return false;
				}
				else
				{
					if (!entry.delete())
						return false;
				}
			}
		}
		return directory.delete();
	}
	
	public static String getFormatedFileSize(long fileSize) 
	{
		NumberFormat formatter = NumberFormat.getInstance();
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		double fileSizeD = fileSize;
		if(fileSizeD < 1024) return ((long)fileSizeD + " B");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " kB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " MB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " GB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " TB");
		fileSizeD = fileSizeD / 1024;
		if(fileSizeD < 1024) return (formatter.format(fileSizeD) + " PB");
		return (formatter.format(fileSizeD / 1024) + " EB");	
	}
	
	public static String getFormatedDate(long time) 
	{
		return getFormatedDate(time, null);
	}
	
	public static String getFormatedDate(long time, String pattern) 
	{
		if(pattern == null) pattern = DATE_FORMAT;
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(time);
	}
	
	public static String replaceLastDot(String text, String replacement) 
	{
		return text.replaceAll(REGEX_REPLACEALL_LASTDOT, replacement);
	}
	
	public static int regexGetCountOf(byte[] input, String regex) throws UnsupportedEncodingException 
	{   
		return regexGetCountOf(new String(input, "UTF8"), regex);
	}
	
	public static int regexGetCountOf(String input, String regex) 
	{            	
		int count = 0;
		Pattern p = Pattern.compile(regex);   
		Matcher m = p.matcher(input);
		while (m.find()) ++count;
		return count;
	}
	
    public static String byteArrayToHexString(byte[] bytes) {
    	char[] hexArray = "0123456789ABCDEF".toCharArray();
    	char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
	public static String capitalizeAllFirstLetters(String string) {
		char[] chars = string.toLowerCase().toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i]) || chars[i]=='.' || chars[i]=='\'') { // You can add other chars here
				found = false;
			}
		}
		return String.valueOf(chars);
	}
    
	public static void saveStringToFile(File file, String text) throws IOException
	{
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "UTF8");
			out.write(text);
			out.flush();
			out.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
	}
	
	public static String loadStringFromFile(File file) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

			String line = bufferedReader.readLine();
			if(line != null) line = line.replaceAll("\\p{C}", "");
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}
			bufferedReader.close();
               
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString().trim();
	}
	
	public static byte[] loadBytesFromFile(File file) throws Exception 
	{
		byte[] b = new byte[(int) file.length()];
		InputStream fileInputStream = null;
		
        try {
			fileInputStream = Files.newInputStream(file.toPath());
			fileInputStream.read(b);
		} catch (Exception e) {
			throw e;
		} finally {
			if(fileInputStream != null) fileInputStream.close();
		}
        
        return b;
	}
	
	public static void saveBytesToFile(File file, byte[] bytes) throws Exception 
	{
        OutputStream fos = null;
        try {
        	fos = Files.newOutputStream(file.toPath());
			fos.write(bytes);
		} catch (Exception e) {
			throw e;
		} finally {
			if(fos != null) {
				fos.flush();
				fos.close();
			}
		}
	}
	
	public static String loadStringFromInputStream(InputStream is) throws IOException
	{
		StringBuilder text = new StringBuilder();
		
		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF8"));

			String line = bufferedReader.readLine();
			while(line != null){
				text.append(line.trim());
				text.append("\n");
				line = bufferedReader.readLine();
			}      
			bufferedReader.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } 
        return text.toString();
	}
    
    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                 + replacement
                 + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }
    
	public static char getCharFromChosenCharset(String charSet, byte deriveFromValue)
	{
		int byteValue = deriveFromValue + 128;
		int charIndex = (int)Math.floor(((double)byteValue / 255.001) * charSet.length());
		if(charIndex == charSet.length()) --charIndex;
		return charSet.charAt(charIndex);
	}
	
	public static String getFormatedTime(long time, Locale locale) 
	{
		if(locale == null) locale = Locale.getDefault();
		DateFormat formatter = DateFormat.getTimeInstance(
	            DateFormat.MEDIUM, 
	            locale);
		return formatter.format(new Date(time));
	}
    
    public static <T> T[] concatAll(T[] first, T[]... rest) 
    {
    	  int totalLength = first.length;
    	  for (T[] array : rest) {
    	    totalLength += array.length;
    	  }
    	  T[] result = Arrays.copyOf(first, totalLength);
    	  int offset = first.length;
    	  for (T[] array : rest) {
    	    System.arraycopy(array, 0, result, offset, array.length);
    	    offset += array.length;
    	  }
    	  return result;
    }
    
    public static char[] trim(char[] input) 
    {
        int len = input.length;
        int st = 0;
        char[] val = input;

        while ((st < len) && (val[st] <= ' ')) {
            st++;
        }
        while ((st < len) && (val[len - 1] <= ' ')) {
            len--;
        }
        
        if((st > 0) || (len < input.length))
        {
        	char[] output = Arrays.copyOfRange(input, st, len);
        	Arrays.fill(input, '\u0000'); 
        	return output;

        }
        else return input;
    }
    
	public static char[] intToChars(int input)
	{
		if(input < 0) throw new IllegalArgumentException("Cannot be < 0");
		if(input == 0) return (new char[]{'0'});
		int numOfDigits = getNumberOfDigits(input); 
	    char[] output = new char[numOfDigits];
	    for(int i = numOfDigits - 1; i > -1; i--, input /= 10) 
	    { 
	    	output[i] = (char)((input % 10) +  '0'); 
	    } 
	    input = 0;
	    return output;
	}
	
	public static int getNumberOfDigits(int input)
	{
		if(input < 0) throw new IllegalArgumentException("Cannot be < 0");
		if(input == 0) return 1;
		int length = 0;
		long temp = 1;
		while (temp <= input) {
		    length++;
		    temp *= 10;
		}
		input = 0;
		return length;
	}
	
	public static byte[] toBytes(char[] chars, boolean defaultCharset) 
	{
		  CharBuffer charBuffer = CharBuffer.wrap(chars);
		  ByteBuffer byteBuffer = defaultCharset ? Charset.defaultCharset().encode(charBuffer) : Charset.forName("UTF-8").encode(charBuffer);
		  byte[] bytes = Arrays.copyOfRange(byteBuffer.array(),
		            byteBuffer.position(), byteBuffer.limit());
		  Arrays.fill(byteBuffer.array(), (byte) 0);
		  return bytes;
	}
    
	public static <T> T[] getArrayCopy(T[] data)
	{
		T[] result = Arrays.copyOf(data, data.length);
		return result;
	}
    
	public static byte[] getByteArrayCopy(byte[] data)
	{
		byte[] dataCopy = new byte[data.length];
		System.arraycopy(data, 0, dataCopy, 0, data.length);
		return dataCopy;
	}
	
	public static String escapeHTML(String s) 
	{
	    StringBuilder out = new StringBuilder(Math.max(16, s.length()));
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
	            out.append("&#");
	            out.append((int) c);
	            out.append(';');
	        } else {
	            out.append(c);
	        }
	    }
	    return out.toString();
	}
	
	public static String[] parseCommand(String command)
	{
		String cmdParseRegex = "\\s(?=(?:[^'\"`]*(['\"`]).*?\\1)*[^'\"`]*$)";
		String[] cmdParams = command.split(cmdParseRegex);
		Helpers.trimQuotes(cmdParams);
		return cmdParams;
	}
	
	public static void trimQuotes(String[] array)
	{
		for(int i = 0; i < array.length; ++i) 
		{
            int length = array[i].length();
			if(length > 1) {
            	if(array[i].substring(0, 1).equals("\"") && array[i].substring(length - 1, length).equals("\"")) {
            		array[i] = array[i].substring(1, length - 1);
            	}	
            } 
        }
	}
	
	public static void setTextToPasswordField(JPasswordField field, char[] password)
	{
		InsertCharsHelper content = new InsertCharsHelper();
        PlainDocument doc = new PlainDocument(content);
        try {
            content.insertChars(0, password);
        } catch (Exception e) {
        	throw new Error("ERROR: setTextToPasswordField");
        }
        field.setDocument(doc);
	}
	
	public static List<File> getFileListFromDrop(Transferable t) 
	{
        try {
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                //windows & mac
                try {
                	return (List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
                } catch (InvalidDnDOperationException e) {
                    // swallow
                }
            }
            
            DataFlavor uriListDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
            if (t.isDataFlavorSupported(uriListDataFlavor)) {
                //linux
                String uriList = (String)t.getTransferData(uriListDataFlavor);
                return textURIListToFileList(uriList);
            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
            // swallow
        } 
        return null;
    }
	
	public static String getStringFromDrop(Transferable t) 
	{
        try {
            if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                	return (String)t.getTransferData(DataFlavor.stringFlavor);
                } catch (InvalidDnDOperationException e) {
                    // swallow
                }
            }
            
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            // swallow
        } 
        return null;
    }
	
    public static List<File> textURIListToFileList(String data) 
    {
        List<File> list = new ArrayList<File>();
        for (StringTokenizer st = new StringTokenizer(data, "\r\n\u0000");
                st.hasMoreTokens();) {
            String s = st.nextToken();
            if (s.startsWith("#")) {
                continue;
            }
            try {
            	URI uri = new URI(s.replaceAll(Pattern.quote("["), "%5B").replaceAll(Pattern.quote("]"), "%5D"));
            	File file = new File(uri);
                if(file.exists()) list.add(file);
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
        return list;
    }
		
	public static String replaceAndWrapPath(String inputString, String regex, String replacement)
	{
		return inputString.replaceAll(regex, "\"" + Matcher.quoteReplacement(replacement) + "\"");
	}
	
	public static String convertToUnixFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(File.separator), UNIX_FILE_SEPARATOR);
		return path;
	}
	
	public static String convertToCurrentFileSeparator(String path)
	{
		path = path.replaceAll(Pattern.quote(UNIX_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		path = path.replaceAll(Pattern.quote(WINDOWS_FILE_SEPARATOR), Matcher.quoteReplacement(File.separator));
		return path;
	}
	
	public static String getFileExt(File file)
	{
		if(file == null) return null;
		return file.getName().substring(file.getName().lastIndexOf(".") + 1);
	}
		
	public static FilenameFilter getOnlyExtFilenameFilter(String extension)
	{
		Helpers h = new Helpers();
		return h.getOnlyExtFF(extension);
	}
	
	private FilenameFilter getOnlyExtFF(String extension)
	{
		OnlyExt oe = new OnlyExt(extension);
		return oe;
	}
	
	private class OnlyExt implements FilenameFilter 
	{ 
		String ext;	
		public OnlyExt(String ext) 
		{ 
			this.ext = "." + ext; 
		}
		
		public boolean accept(File dir, String name) 
		{ 
			return name.endsWith(ext); 
		} 
	}
		
	public static DirectoryStats getDirectoryStats(File directory)
	{
		Helpers h = new Helpers();
		return h.getDirectoryStatsInner(directory);
	}
	
	private DirectoryStats getDirectoryStatsInner(File directory)
	{
		DirectoryStats ds =  new DirectoryStats();
		return ds;
	}
	
	public class DirectoryStats 
	{ 
		public int allFolders = 0, allFiles = 0;
		public int okFolders = 0, okFiles = 0;
	}
	
	public static String createStringWithLength(int length, char charToFill) 
	{
		if (length > 0) {
			char[] array = new char[length];
			Arrays.fill(array, charToFill);
			return new String(array);
		}
		return "";
	}
	
	public static String getShortenedStackTrace(Throwable e, int maxLines) 
	{
	    StringWriter writer = new StringWriter();
	    e.printStackTrace(new PrintWriter(writer));
	    String[] lines = writer.toString().split("\n");
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
	        sb.append(lines[i]).append("\n");
	    }
	    return sb.toString();
	}
	
	public static Dimension getImageDimension(File imgFile) throws IOException 
	{
		int pos = imgFile.getName().lastIndexOf(".");
		if (pos == -1)
			throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
		String suffix = imgFile.getName().substring(pos + 1);
		Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
		if (iter.hasNext()) {
			ImageReader reader = iter.next();
			try {
				ImageInputStream stream = new FileImageInputStream(imgFile);
				reader.setInput(stream);
				int width = reader.getWidth(reader.getMinIndex());
				int height = reader.getHeight(reader.getMinIndex());
				return new Dimension(width, height);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				reader.dispose();
			}
		}
		throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
	}
	
	public static boolean writeTestFile(File dir)
	{
		try {
			String name = null;
			File testFile = null;
			while(true) {
				name = (System.currentTimeMillis() + ".testfile");
				testFile = new File(dir.getAbsolutePath() + File.separator + name);
				if(!testFile.exists()) break;
			}
			OutputStream os = new FileOutputStream(testFile);
			os.write(0);
			os.flush();
			os.close();
			return testFile.delete();	
		} catch (Exception e) {
			return false;
		}		
	}
	
	public static RawImage loadToRawImage(File sourceFile, Double imageScale) throws Exception 
	{
		BufferedImage image = null;

		image = ImageIO.read(new File(sourceFile.getAbsolutePath()));

		if(imageScale < 1.0) 
		{
			int newWidth = (int)Math.round(image.getWidth() * imageScale);
			int newHeight = (int)Math.round(image.getHeight() * imageScale);
			
			ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
			resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
			image = resizeOp.filter(image, null);
		}			
		
		int width = image.getWidth();
		int height = image.getHeight();		
		
		int pixels[] = new int[width * height];
		
		PixelGrabber grabber = new PixelGrabber(image.getSource(), 0, 0, width, height, pixels, 0, width);			
		grabber.grabPixels();			
		image = null;
		
		RawImage rawImage = new RawImage(pixels, width, height);
		
		return rawImage;
	}
	
	private static PosixFilePermission[] posixFilePermissionsBitSorted = 
	{ 
		PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_READ,
		PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_READ,
		PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ 	
	};
	
    public static Set<PosixFilePermission> unixModeToPosixSet(short unixMode) 
    {
        int mask = 1;
        Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
        for (PosixFilePermission flag : posixFilePermissionsBitSorted) {
            if (flag != null && (mask & unixMode) != 0) {
                perms.add(flag);
            }
            mask = mask << 1;
        }
        return perms;
    }
    
	public static short posixPermissionToUnixMode(Set<PosixFilePermission> permission) 
	{
		int mode = 0;
		for (PosixFilePermission action : PosixFilePermission.values()) {
			mode = mode << 1;
			mode += permission.contains(action) ? 1 : 0;
		}
		return (short) mode;
	}
	
	public static void setUnixMode(File file, short unixMode)
	{
		if(!isWindows() && file != null)
		{
			try {
				Path path = Paths.get(file.getCanonicalPath());
				Files.setPosixFilePermissions(path, Helpers.unixModeToPosixSet(unixMode));
			} catch (Exception e) {
				// N/A
			}
		}
	}
	
	public static Short getUnixMode(File file)
	{
		if(!isWindows() && file != null)
		{
			Set<PosixFilePermission> filePerm = null;
			try {
			    filePerm = Files.getPosixFilePermissions(Paths.get(file.getCanonicalPath()));
			    return posixPermissionToUnixMode(filePerm);
			} catch (Exception e) {
			    //N/A
			}
		}
		return null;
	}
	
	public static Short getUnixMode(ZipArchiveEntry entry)
	{
		if(!isWindows())
		{
			for(int i = 0; i < entry.getExtraFields().length; ++i)
			{
				try {
					ZipExtraField field = entry.getExtraFields(true)[i];
					if(field instanceof ParanoiaExtraField) {
						ParanoiaExtraField paranoiaField = (ParanoiaExtraField)field;
						return paranoiaField.getUnixMode();
					}
				} catch (Exception e) {
					// N/A
				}
			}
		}
		return null;
	}
	
	
	public static boolean isMac()
	{
	    boolean mac = false;		
		try {
			String OS = System.getProperty("os.name").toLowerCase();
			if(OS != null && OS.indexOf("mac") > -1) mac = true;
		} catch (Exception e) {
			// swallow
		}
	    return mac;
	}
	
	public static boolean isWindows()
	{
	    boolean win = false;		
		try {
			String OS = System.getProperty("os.name").toLowerCase();
			if(OS != null && OS.indexOf("win") > -1) win = true;
		} catch (Exception e) {
			// swallow
		}
	    return win;
	}
	
	public static int getScrollBarSize()
	{
		int size = 0; 
		try {
			size = ((Integer)UIManager.get("ScrollBar.width")).intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return size;
	}
	
	public static String getCurrentAppDirPath() 
	{
		String path = null;
		try {
			URL url = ClassLoader.getSystemClassLoader().getResource(".");
			File directory = null;
			try {
				directory = new File(url.toURI());
			} catch(URISyntaxException e) {
				directory = new File(url.getPath());
			}
			
			path = directory.getAbsolutePath();
		} catch (Exception e) {
			// swallow
		}
		
		return path;
	}
	
	public static String insertTextPeriodically(String text, String insert, int period)
	{
		if(period < 1) return text;
		StringBuilder builder = new StringBuilder(text.length() + insert.length() * (text.length()/period)+1);

		int index = 0;
		String prefix = "";
		while (index < text.length())
		{
			builder.append(prefix);
			prefix = insert;
			builder.append(text.substring(index, 
					Math.min(index + period, text.length())));
			index += period;
		}
		return builder.toString();
	}
	
    public static Object[] getNumberOfEncAndUnenc(String[] filelist)
    {
    	List<CryptFile> files = new ArrayList<CryptFile>();
    	
    	int[] encdec = new int[2];
    	
		for(int i = 0; i < filelist.length; ++i)
		{
			CryptFile tempFile = new CryptFile(filelist[i]);
			tempFile = new CryptFile(tempFile.getAbsolutePath());
			if(tempFile.exists()) {
				files.add(tempFile);
				if(tempFile.isFile() && tempFile.isEncrypted()) ++encdec[0];
				else ++encdec[1];
			}
		}
		
		if(files.size() < 1) return null;
		
		Object[] results = new Object[2];
		results[0] = files;
		results[1] = encdec;
		
		return results;    	
    }
    
    public static boolean isActiveRightToLeftLang()
    {
    	Locale defaultLocale = Locale.getDefault();
    	if(defaultLocale.equals(new Locale("he"))) return true;
    	else return false;
    }
    
    public static Locale getLocale(String interfaceLanguage)
    {
    	Locale locale = (interfaceLanguage == null || interfaceLanguage.trim().equals("")) ? Locale.getDefault() : new Locale(interfaceLanguage);
    	if(locale.getLanguage().length() > 2) locale = new Locale(locale.getLanguage().substring(0,  2));
    	Locale.setDefault(locale);
    	Encryptor.setLocale(locale);
		return locale;
    }
    
    public static File showOpenDirDialog(Frame parent, File startDir)
    {
    	return showOpenFileDialog(parent, startDir, null,  "");
    }
    
    public static File showOpenFileDialog(Frame parent, File startDir, String filterComment, String... extensions)
    {
    	boolean nativeDialogError = false;
    	File file = null;
    	if(Helpers.isWindows())
    	{ 	
        	try {
				JnaFileChooser fileChooser = new JnaFileChooser(startDir);
				if(filterComment != null) fileChooser.addFilter(filterComment, extensions);
				else fileChooser.setMode(JnaFileChooser.Mode.Directories);
				if (fileChooser.showOpenDialog(parent)) 
				{
					file = fileChooser.getSelectedFile();
				}
			} catch (Throwable e) {
				//e.printStackTrace();
				nativeDialogError = true;
			}
    	}
    	
    	if(!Helpers.isWindows() || nativeDialogError)
    	{
	    	JFileChooser fileChooser = new JFileChooser(startDir);
			if(filterComment != null) { 
				FileFilter filter = new FileNameExtensionFilter(filterComment, extensions);
				fileChooser.setFileFilter(filter);
			}
			else {
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fileChooser.setAcceptAllFileFilterUsed(false);
			}
			if(fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) 
			{
				file = fileChooser.getSelectedFile();
			}
    	}
    	return file;
    }
    
    public static File showSaveFileDialog(Frame parent, File startDir, String filterComment, String... extensions)
    {
    	boolean nativeDialogError = false;
    	File file = null;
    	if(Helpers.isWindows())
    	{ 	
        	try {
				JnaFileChooser fileChooser = new JnaFileChooser(startDir);
				fileChooser.addFilter(filterComment, extensions);
				if(fileChooser.showSaveDialog(parent)) 
				{
					file = fileChooser.getSelectedFile();
				}
			} catch (Throwable e) {
				//e.printStackTrace();
				nativeDialogError = true;
			}
    	}
    	
    	if(!Helpers.isWindows() || nativeDialogError)
    	{
	    	JFileChooser fileChooser = new JFileChooser(startDir);
			FileFilter filter = new FileNameExtensionFilter(filterComment, extensions);
			fileChooser.setFileFilter(filter);
			if(fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) 
			{
				file = fileChooser.getSelectedFile();
			}
    	}
    	return file;
    }
    
    public static void showReplaceTextWarningDialog (JFrame parentFrame, final JTextArea textArea, String warningMessage, final String replacement)
    {
		if(textArea.getText().trim().equals("")) {
			textArea.setText(replacement);
			return;
		}
    	
    	final OkCancelDialog warningDialog = new OkCancelDialog(parentFrame);
		ActionListener warningDialogListenerOk = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				textArea.setText(replacement);
				warningDialog.dispose();
			}
		};
		ActionListener warningDialogListenerCancel = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				warningDialog.dispose();
				
			}
		};
		warningDialog.setListeners(warningDialogListenerOk, warningDialogListenerCancel);
		warningDialog.setText(warningMessage);
		warningDialog.showDialog();
	}
    
    public static boolean isAppFocused()
    {
		boolean focused = false;
    	Window windows[] = Window.getWindows();
		for (Window w : windows) {
			focused |= w.isFocused();
		}
		
		return focused;
    }
    
    public static void playFinishSound(boolean checkFocus)
    {
    	try {
			if(!isWindows()) return;
    		if(checkFocus && isAppFocused()) return;
    		
    		final Runnable runnable =
    			     (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
    			if (runnable != null) runnable.run();
    			
		} catch (Exception e) {
			// N/A
		}
    }
	
    /** Load Image from App's Resource */
    public static Image loadImageFromResource(Class usedClass, String name, String extension) 
    {	
    	return loadImageFromResource(usedClass, name, extension, "res/");
    }
    	
    /** Load Image from App's Resource */
    public static Image loadImageFromResource(Class usedClass, String name, String extension, String resPath) 
    {	
    	Image image = null;  
    	
    	try {
    		BufferedImage normalImage = (ImageIO.read(usedClass.getResource("/" + resPath + name + "." + extension)));
    		BufferedImage hdpiImage = (ImageIO.read(usedClass.getResource("/" + resPath + name + ".hdpi." + extension)));
    		
    		int originalWidth = normalImage.getWidth();
    		int originalHeight = normalImage.getHeight();
    		
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
    		
    		double scale = defaultScreen.getDefaultConfiguration().getDefaultTransform().getScaleX();
    		
    		if(scale > 1.0)
    		{
	    		int newWidth = (int)Math.round(originalWidth * scale);
	    		int newHeight = (int)Math.round(originalHeight * scale);	    		
	    		
	    		ResampleOp resizeOp = new ResampleOp(newWidth, newHeight);
	    		resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
	    		Image resapledImage = resizeOp.filter(hdpiImage, null);		
	    		
	    		List<Image> images = new ArrayList<Image>();
				images.add(normalImage);
				images.add(resapledImage);
				
				//image = new BaseMultiResolutionImage(images.toArray(new Image[0]));
				
				Class<?> multiImageClass = Class.forName("java.awt.image.BaseMultiResolutionImage");
				Object multiImageObject = multiImageClass.getConstructor(Image[].class).newInstance((Object)images.toArray(new Image[0]));
				image = (Image)multiImageObject;
    		}
		} catch (Exception e) {}
    	
    	if(image != null) return image;
    	
    	try {
    		image = Toolkit.getDefaultToolkit().getImage(usedClass.getClassLoader().getResource(resPath + name + "." + extension));
    	} catch (Exception e){}
    	
    	try {
    		if(image == null) image = ImageIO.read(usedClass.getResource("/" + resPath + name + "." + extension));
    	} catch (Exception e){}    	
    	
    	return image;   	
    }
    
    /** Get text list of the .enc files that will be replaced */
    public static String getReplaceEncFilesWarning(File outputDir, List<File> files)
    {
    	StringBuilder fileListString = new StringBuilder();
    	int maxListSize = 10;
    	int counter = 0;

    	for (int i = 0; i < files.size(); ++i)
    	{
			File selectedFileTemp = files.get(i);
			File outputDirTemp = (outputDir == null ? selectedFileTemp.getParentFile() : outputDir);
			if(selectedFileTemp != null && new CryptFile(files.get(i)).isEncrypted()) continue;
    		String tempName = files.get(i).getName() + "." + Encryptor.ENC_FILE_EXTENSION;
    		if(new File(outputDirTemp.getAbsolutePath() + File.separator + tempName).exists()) 
    		{ 
    			++counter;
    			if(counter <= maxListSize) fileListString.append(tempName + "<br/>");
    		}
    	}
    	if(counter > maxListSize)
    		fileListString.append("...<br/>+ " + (counter - maxListSize) + "<br/>");
    	
    	String output = fileListString.toString().trim();
    	if(output.length() == 0) return null;
    	else return output;
    }
    
    private static final String[] UNICODE_FONT_LANGUAGE = {"zh", "ja", "ko", "hi", "th"};

	public static boolean isCurrentLanguageInUnicodeList() {
		String currentLanguage = Locale.getDefault().getLanguage().toLowerCase();
		for (String language : UNICODE_FONT_LANGUAGE) {
			if (currentLanguage.startsWith(language.toLowerCase())) {
				return true;
			}
		}
		return false;
	}
	
	private static Boolean unicodeFontAvailable = null;
	
	public static boolean isUnicodeFontAvailable() 
    {
		if(unicodeFontAvailable != null) {
			return unicodeFontAvailable;
		}
		String targetFontName = "Arial Unicode MS";
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        unicodeFontAvailable = Arrays.asList(fontNames).contains(targetFontName);
        return unicodeFontAvailable;
    }
	
	public static void setUnicodeFont() 
    {
        String targetFontName = "Arial Unicode MS";
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();

        if (Arrays.asList(fontNames).contains(targetFontName)) {
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    Font defaultFont = UIManager.getDefaults().getFont(key);
                    Font newFont = new Font(targetFontName, defaultFont.getStyle(), defaultFont.getSize());
                    UIManager.put(key, new FontUIResource(newFont));
                }
            }
        } else {
            System.out.println("Font not found on the system: " + targetFontName);
        }
    }
	
	public static String htmlWrapToCode(String text)
	{
		String output = null;
		if(isWindows() && isUnicodeFontAvailable()) {
			output = "<span style=\"font-family: Arial Unicode MS;\">" + text + "</span>";
		}
		else {
			output = "<code>" + text + "</code>";
		}
		return output;
	}
	
	public static void setUnicodeFontInWindows(Component component)
	{
		/*
		Font customFont = null;		
		try {
			customFont = Font.createFont(Font.TRUETYPE_FONT, new File("DejaVuSans.ttf"));
            customFont = customFont.deriveFont(component.getFont().getSize());      
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
				
		if(isWindows() && isUnicodeFontAvailable()) {
    		Font defaultFont = component.getFont();
    		component.setFont(new Font("Arial Unicode MS", defaultFont.getStyle(), defaultFont.getSize()));
    		//component.setFont(customFont);
        }
	}
}
