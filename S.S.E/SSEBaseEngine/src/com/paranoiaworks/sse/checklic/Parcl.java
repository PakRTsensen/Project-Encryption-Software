package com.paranoiaworks.sse.checklic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.paranoiaworks.sse.Encryptor;
import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.StaticConfig;
import com.paranoiaworks.sse.components.FilteredTextField;
import com.paranoiaworks.sse.components.MessageDialog;
import com.paranoiaworks.sse.dao.SettingsDataHolder;

import ext.BareBonesBrowserLaunch;

public class Parcl {

	private static final char[] pd = "ProVersionCode-YesItIsOpenSource".toCharArray();
	private static final char[] pa = "AndroidProCode-YeahItsStillOpenSourceAndWeDontWasteTimeOnNonsense".toCharArray();
	private static final String f = "ssepro.lic";
	private static final String ALLOWED_CHARS = "0123456789 abcdefABCDEF";
	
    public static void showLicDialog(Component comp, final SettingsDataHolder sdh)
    {
    	byte[] licenseBytes = sdh.getLicenseBytes();
    	final boolean proActivated = licenseBytes != null && checkAll(licenseBytes) ? true : false;
    	
    	final Frame window = new Frame();
		final ResourceBundle textBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", Locale.getDefault());
	
		final JDialog licDialog = new JDialog(window, !proActivated ? textBundle.getString("ssecore.pro.title") : "", true);
		JPanel mainLayout = new JPanel(new BorderLayout());
		mainLayout.setBorder(new EmptyBorder(8, 8, 8, 8));
		licDialog.setLayout(new BorderLayout());
		licDialog.setMinimumSize(new Dimension(360, 120));
		licDialog.setResizable(false);
		licDialog.setLocationByPlatform(true);
		
		
		final FilteredTextField textField = new FilteredTextField();
		textField.setAllowedChars(ALLOWED_CHARS, null);
		textField.setPreferredSize(new Dimension(300, 30));
		
		final JLabel label = new JLabel(!proActivated ? textBundle.getString("ssecore.pro.enterCode") : textBundle.getString("ssecore.pro.YourProCode") + ":", SwingConstants.LEFT);
		label.setBorder(new EmptyBorder(2, 0, 8, 0));
		JButton helpButton = new JButton("?");
		JPanel codeLayout = new JPanel(new BorderLayout());
		codeLayout.setBorder(new EmptyBorder(0, 0, 8, 0));
		codeLayout.add(label, BorderLayout.PAGE_START);
		codeLayout.add(textField, BorderLayout.WEST);
		codeLayout.add(new JLabel("  "), BorderLayout.CENTER);
		codeLayout.add(helpButton, BorderLayout.EAST);
		
		final JButton okButton = new JButton ("OK");
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean ok = false;
				try {
					ok = save(textField.getText(), sdh);
				} catch (Exception ex) {
			    	final MessageDialog messageDialog = new MessageDialog(window, "", MessageDialog.ICON_NEGATIVE);
			    	String errorMessage = "<br/>Error writing license file!";
			    	if(sdh!= null && sdh.getConfigDirPath() != null) 
			    		errorMessage += "<br/><br/>Read-only directory:<br/>" + sdh.getConfigDirPath();  	
			    	messageDialog.setText("<html>" + errorMessage + "</html>");
			    	messageDialog.showDialog();
				}
				if(ok) licDialog.setVisible(false);
			}
		});	
		
		helpButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				BareBonesBrowserLaunch.openURL(textBundle.getString("ssecore.pro.helpURL").trim());
			}
		});	
	
		textField.getDocument().addDocumentListener(new DocumentListener() {
		  public void changedUpdate(DocumentEvent e) {
			  execute();
		  }
		  public void removeUpdate(DocumentEvent e) {
			  execute();
		  }
		  public void insertUpdate(DocumentEvent e) {
			  execute();
		  }

		  public void execute() 
		  {
			    if(!proActivated) 
			    {
			    	if(checkAll(textField.getText()))
			    	{
			    		okButton.setEnabled(true);
			    		label.setText(textBundle.getString("ssecore.pro.ok"));
			    		licDialog.pack();
			    	}
			    	else
			    	{
			    		okButton.setEnabled(false);
			    		label.setText(textBundle.getString("ssecore.pro.enterCode"));
			    	}
			    }
		  }
		});
		
		if(proActivated) {
			String proCode = Helpers.byteArrayToHexString(licenseBytes);
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < proCode.length(); ++i)
			{
				sb.append(proCode.charAt(i));
				if((i + 1) % 4 == 0 && i != 0) sb.append(" ");
			}		
			textField.setText(sb.toString().trim());
			textField.setEditable(false);
		}
			
		mainLayout.add(codeLayout, BorderLayout.NORTH);
		if(!proActivated) mainLayout.add(okButton, BorderLayout.SOUTH);
		
		licDialog.add(mainLayout, BorderLayout.CENTER);
		licDialog.pack();
		licDialog.setLocationRelativeTo(comp);
		licDialog.setVisible(true);	
		
        EventQueue.invokeLater(new Runnable(){
        	public void run() 
        	{
        		textField.grabFocus();
        		textField.requestFocus();
        	}
        });
    }
	
	public static boolean loadcheck(SettingsDataHolder shd) 
	{
        boolean ok = false;
		
		if(shd != null) {
			ok = checkAll(shd.getLicenseBytes());
			if(ok) return ok;
		}
        
        File file = new File(f);
        if(!file.exists()) return false;
        
        FileInputStream fis = null;
        byte[] input = new byte[8];
        try {			
        	fis = new FileInputStream(file);
			fis.read(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
        ok = checkAll(input);
        
        if(ok && shd != null) {
        	try {
				shd.setLicenseBytes(input);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        return ok;
	}
	
	public static boolean loadcheckAlternative(SettingsDataHolder main, SettingsDataHolder alternative)
	{
    	boolean alternativeLicence = Parcl.loadcheck(alternative);
    	if(alternativeLicence) {
    		try {
				main.setLicenseBytes(alternative.getLicenseBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	else if(StaticConfig.AUTO_SEED) {
        	try {
				main.setLicenseBytes(getAutoSeedPc());
				alternativeLicence = true;
			} catch (Exception e) {
				e.printStackTrace();
			}	
    	}
    	return alternativeLicence;
	}
	
	private static byte[] getAutoSeedPc()
	{
		String input = "0" + Encryptor.getMD5Hash(Encryptor.getRandomBA(16)).substring(0, 6);
		byte[] licenceBytes = null;
		try {
			licenceBytes = new Encryptor("ProVersionCode-YesItIsOpenSource".toCharArray(), 6).encrypt(input.getBytes(), false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return licenceBytes;
	}
	
	private static boolean checkAll(byte[] input) 
	{
		return checkPc(input) || checkAndroid(input);
	}
	
	private static boolean checkPc(byte[] input) 
	{
        String output = null;
		try {
			output = new String(new Encryptor(pd, 6).decryptUseEncAlg(input, false));
		} catch (Exception e) {
			// N/A
		}
		
		if(output != null && Helpers.regexGetCountOf(output, "[^a-f0-9]") == 0) return true; 
		else return false;
	}
	
	private static boolean checkAndroid(byte[] input) 
	{
        String output = null;
		try {
			output = new String(new Encryptor(pa, 6).decryptUseEncAlg(input, false));
		} catch (Exception e) {
			// N/A
		}
		
		if(output != null && output.length() == 7 && Helpers.regexGetCountOf(output, "[^0-9]") == 0) return true; 
		else return false;
	}
	
	private static boolean checkAll(String input) 
	{
		return checkPc(input) || checkAndroid(input);
	}
	
	private static boolean checkAndroid(String input) 
	{
        input = input.replaceAll("\\s+", "").trim();
		String output = null;
		try {
			output = new String(new Encryptor(pa, 6).decryptUseEncAlg(Helpers.hexStringToByteArray(input), false));
		} catch (Exception e) {
			// N/A
		}
		
		if(output != null && output.length() == 7 && Helpers.regexGetCountOf(output, "[^0-9]") == 0) return true; 
		else return false;
	}
	
	private static boolean checkPc(String input) 
	{
        input = input.replaceAll("\\s+", "").trim();
		String output = null;
		try {
			output = new String(new Encryptor(pd, 6).decryptUseEncAlg(Helpers.hexStringToByteArray(input), false));
		} catch (Exception e) {
			// N/A
		}
		
		if(output != null && Helpers.regexGetCountOf(output, "[^a-f0-9]") == 0) return true; 
		else return false;
	}
	
	public static boolean save(String input, SettingsDataHolder shd) throws Exception
	{
		input = input.replaceAll("\\s+", "").trim();
		byte[] ib = Helpers.hexStringToByteArray(input);
		shd.setLicenseBytes(ib);

		return true;
	}
}
