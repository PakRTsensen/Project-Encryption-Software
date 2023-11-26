package com.paranoiaworks.sse.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.StaticConfig;
import com.paranoiaworks.sse.components.FilteredTextField;
import com.paranoiaworks.sse.components.MessageDialog;
import com.paranoiaworks.sse.components.OkCancelDialog;
import com.paranoiaworks.sse.dao.SettingsDataHolder;

import ext.BareBonesBrowserLaunch;

/**
 * Settings Dialog (single purposed - File Encryptor)
 * 
 * @author Paranoia Works
 * @version 1.0.10
 */ 


@SuppressWarnings("serial")
public class SettingsDialogFE extends JDialog {
    
	private Frame parentFrame = null;
	private JButton okButton = null;
	private ResourceBundle textBundle;
	private ResourceBundle coreBundle;
	private SettingsDataHolder sdh;
	private boolean frameExpanded = false;
	
	private static final int MIN_WIDTH = 550;
	private static final int MIN_HEIGHT = 300;
	private static final int HEIGHT = 520;
	private static final int LINE_TITLE_FONTSIZE = 14;
	private static final int LINE_CURRENT_FONTSIZE = 12;
	private static final int FONTSIZE_SMALL = 12;
	
	public SettingsDialogFE(Frame frame, ResourceBundle textBundle, ResourceBundle coreBundle, SettingsDataHolder sdh)
    {
        super(frame, textBundle.getString("ssefilegui.settings.settingsTitle"), true);
        this.parentFrame = frame;
        this.textBundle = textBundle;
        this.coreBundle = coreBundle;
        this.sdh = sdh;
        init();
    }
	
	public synchronized void showDialog() 
    {
		this.pack();
		if(frameExpanded) this.setPreferredSize(new Dimension(this.getWidth() + Helpers.getScrollBarSize(), HEIGHT));	
		this.pack();
    	this.setLocationRelativeTo(parentFrame);
    	this.setVisible(true);	
    }
    
    private void init() 
    {       
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridBagLayout());
        buttonPane.setBorder(new EmptyBorder(16, 8, 16, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
               
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() 
        {
            public void windowClosing(WindowEvent winEvt) 
            {
				final OkCancelDialog closeSettingsDialog = new OkCancelDialog(parentFrame);
				
				ActionListener closeSettingsDialogOk = new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						closeSettingsDialog.dispose();
						SettingsDialogFE.this.dispose();
					}
				};
				ActionListener closeSettingsDialogCancel = new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						closeSettingsDialog.dispose();
					}
				};
				closeSettingsDialog.setListeners(closeSettingsDialogOk, closeSettingsDialogCancel);
				closeSettingsDialog.setTitle(textBundle.getString("ssefilegui.settings.settingsTitle"));
				closeSettingsDialog.setText(textBundle.getString("ssefilegui.settings.closeSettings"));
				closeSettingsDialog.showDialog();
			}
        });
        this.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        this.setResizable(true);
        this.setLocationByPlatform(true);
        this.setLocationRelativeTo(parentFrame);   

        // Encrypted files directory
        JLabel outputEncDirTitleL = new JLabel(textBundle.getString("ssefilegui.settings.outputEncDir"));
        final JLabel outputEncDirCurrentL = new JLabel();
        fillCurrentPathLabel(outputEncDirCurrentL, null);
        final JCheckBox outputEncDirCB = new JCheckBox();        
        JPanel rowEncDir = makeRow(outputEncDirTitleL, outputEncDirCurrentL, outputEncDirCB);
        outputEncDirCB.addActionListener(new ActionListener() 
        {	
        	public void actionPerformed(ActionEvent evt)  
			{
        		if(outputEncDirCB.isSelected()) {			 
        			chooseDir(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_PATH, 
        					getPath(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_PATH),
        					outputEncDirCurrentL, outputEncDirCB);
        		}
        		else { 
        			fillCurrentPathLabel(outputEncDirCurrentL, null);
        		}
			}
        });
        Boolean encDirEnabled = sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_ENABLED);
        String encDirPath = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_PATH);
        File encDirFile = encDirPath != null ? new File(encDirPath) : null;        
        if(encDirEnabled != null && encDirEnabled && encDirFile != null && encDirFile.canWrite())
        {
        	outputEncDirCB.setSelected(true);
        	fillCurrentPathLabel(outputEncDirCurrentL, encDirFile.getAbsolutePath());
        }
        
        // Decrypted files directory
        JLabel outputDecDirTitleL = new JLabel(textBundle.getString("ssefilegui.settings.outputDecDir"));
        final JLabel outputDecDirCurrentL = new JLabel();
        fillCurrentPathLabel(outputDecDirCurrentL, null);
        final JCheckBox outputDecDirCB = new JCheckBox();
        JPanel rowDecDir = makeRow(outputDecDirTitleL, outputDecDirCurrentL, outputDecDirCB);
        outputDecDirCB.addActionListener(new ActionListener() 
        {	
        	public void actionPerformed(ActionEvent evt)  
			{
        		if(outputDecDirCB.isSelected()) {			 
        			chooseDir(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_PATH, 
        					getPath(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_PATH),
        					outputDecDirCurrentL, outputDecDirCB);
        		}
        		else { 
        			fillCurrentPathLabel(outputDecDirCurrentL, null);
        		}
			}
        });
        Boolean decDirEnabled = sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_ENABLED);
        String decDirPath = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_PATH);
        File decDirFile = decDirPath != null ? new File(decDirPath) : null;        
        if(decDirEnabled != null && decDirEnabled && decDirFile != null && decDirFile.canWrite())
        {
        	outputDecDirCB.setSelected(true);
        	fillCurrentPathLabel(outputDecDirCurrentL, decDirFile.getAbsolutePath());
        }
          
        // Compression
        JLabel compressionTitleL = new JLabel(textBundle.getString("ssefilegui.settings.compression"));
        String[] compressionList = {
        		textBundle.getString("ssefilegui.settings.disabled"),
        		textBundle.getString("ssefilegui.settings.always"),
        		textBundle.getString("ssefilegui.settings.auto")};
        final JComboBox compressionListCB = new JComboBox(compressionList);
        JPanel rowCompression = makeRowComboBox(compressionTitleL, compressionListCB);       
        Integer compression = sdh.getPersistentDataInteger(SSEFileGUI.FE_SETTINGS_COMPRESSION, 2);  
        compressionListCB.setSelectedIndex(compression);
        
        // Show .enc File(s) Replace Warning
        JLabel replaceWarningTitleL = new JLabel(textBundle.getString("ssefilegui.settings.replaceWarning"));
        final JLabel replaceWarningTextL = new JLabel(textBundle.getString("ssefilegui.settings.replaceWarningComment"));
        final JCheckBox replaceWarningCB = new JCheckBox();
        JPanel rowReplaceWarning = makeRow(replaceWarningTitleL, replaceWarningTextL, replaceWarningCB);
        Boolean replaceWarningEnabled = sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_REPLACE_WARNING, true);       
        replaceWarningCB.setSelected(replaceWarningEnabled);
        
        //+ Custom command execution
        JLabel customCommandTitleL = new JLabel(textBundle.getString("ssefilegui.settings.customCommand"));
        final JPanel customCommandBottomPane = new JPanel();
        final JLabel customCommandInfoL = new JLabel(textBundle.getString("ssefilegui.settings.customCommandInfo"));
        final JCheckBox customCommandCB = new JCheckBox();
        final FilteredTextField ccAfterEncTF = new FilteredTextField();
        ccAfterEncTF.setAllowedChars(null, 50000);
        final FilteredTextField ccAfterDecTF = new FilteredTextField();
        ccAfterDecTF.setAllowedChars(null, 50000);
        JPanel firstRowCustomCommand = makeRow(customCommandTitleL, customCommandInfoL, customCommandCB);
        customCommandCB.addActionListener(new ActionListener() 
        {	
        	public void actionPerformed(ActionEvent evt)  
			{
        		if(customCommandCB.isSelected()) {			 
        			customCommandBottomPane.setVisible(true);
        			SettingsDialogFE.this.setPreferredSize(new Dimension(SettingsDialogFE.this.getWidth() + Helpers.getScrollBarSize(), HEIGHT));
        			SettingsDialogFE.this.pack();
        		}
        		else { 
        			SettingsDialogFE.this.setPreferredSize(null);
        			customCommandBottomPane.setVisible(false);
        			SettingsDialogFE.this.pack();
        		}
			}
        });
        
        Boolean customCommandEnabled = sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED);  
        if(customCommandEnabled != null && customCommandEnabled)
        {
        	frameExpanded = true;
        	customCommandCB.setSelected(true);
        	customCommandBottomPane.setVisible(true);
        }
        else
        {
        	customCommandBottomPane.setVisible(false);
        }
        String ccae = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC);
        String ccad = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC);
        
        if(ccae != null)
        	ccAfterEncTF.setText(ccae.trim());
        if(ccad != null)
        	ccAfterDecTF.setText(ccad.trim());
        
        
        JLabel ccAfterEncL = new JLabel(textBundle.getString("ssefilegui.settings.customCommandAfterEnc"));
        ccAfterEncL.setBorder(new EmptyBorder(0,0,3,0));
        JLabel ccAfterEncNoteL = new JLabel(textBundle.getString("ssefilegui.settings.customCommandAfterEncNote"));
        ccAfterEncNoteL.setFont(new Font(ccAfterEncNoteL.getFont().getFamily(), Font.PLAIN, FONTSIZE_SMALL));
        ccAfterEncTF.setPreferredSize(new Dimension(300, 24));
        JPanel ccAfterEncWrapper = new JPanel(new BorderLayout());
        ccAfterEncWrapper.setBorder(new EmptyBorder(0, 0, 15, 0));
        ccAfterEncWrapper.add(ccAfterEncL, BorderLayout.PAGE_START);
        ccAfterEncWrapper.add(ccAfterEncTF, BorderLayout.CENTER);
        ccAfterEncWrapper.add(ccAfterEncNoteL, BorderLayout.PAGE_END);
        
               
        JLabel ccAfterDecL = new JLabel(textBundle.getString("ssefilegui.settings.customCommandAfterDec"));
        ccAfterDecL.setBorder(new EmptyBorder(0,0,3,0));
        JLabel ccAfterDecNoteL = new JLabel(textBundle.getString("ssefilegui.settings.customCommandAfterDecNote"));
        ccAfterDecNoteL.setFont(new Font(ccAfterDecNoteL.getFont().getFamily(), Font.PLAIN, FONTSIZE_SMALL));
        ccAfterDecTF.setPreferredSize(new Dimension(300, 24));
        JPanel ccAfterDecWrapper = new JPanel(new BorderLayout());
        ccAfterDecWrapper.setBorder(new EmptyBorder(0, 0, 15, 0));
        ccAfterDecWrapper.add(ccAfterDecL, BorderLayout.PAGE_START);
        ccAfterDecWrapper.add(ccAfterDecTF, BorderLayout.CENTER);
        ccAfterDecWrapper.add(ccAfterDecNoteL, BorderLayout.PAGE_END);
        
        JPanel ccNoteWrapper = new JPanel(new BorderLayout());
        JLabel ccExampleLink = new JLabel(textBundle.getString("ssefilegui.settings.customCommandExampleLink"));
        ccExampleLink.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(9, 133, 114)));
        ccExampleLink.setForeground(new Color(9, 133, 114));
        ccExampleLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ccExampleLink.addMouseListener(new MouseAdapter() { 
            @Override
            public void mouseClicked(MouseEvent e) {
            	BareBonesBrowserLaunch.openURL(textBundle.getString("ssefilegui.url.CustomCommandExample").trim());
            }

        });
        ccNoteWrapper.add(new JLabel(textBundle.getString("ssefilegui.settings.customCommandNote")), BorderLayout.PAGE_START);
        ccNoteWrapper.add(ccExampleLink, BorderLayout.WEST);
        
        customCommandBottomPane.setLayout(new BoxLayout(customCommandBottomPane, BoxLayout.Y_AXIS));
        customCommandBottomPane.setBorder(new EmptyBorder(8, 10, 15, 15));
        customCommandBottomPane.add(ccAfterEncWrapper);
        customCommandBottomPane.add(ccAfterDecWrapper); 
        customCommandBottomPane.add(ccNoteWrapper);
		
        JPanel customCommandContainer = new JPanel();
		customCommandContainer.setLayout(new BoxLayout(customCommandContainer, BoxLayout.Y_AXIS));
		customCommandContainer.add(firstRowCustomCommand);
		customCommandContainer.add(customCommandBottomPane);
		//- Custom command execution
		
        
		//+ GUI Language
		JLabel interfaceLanguageTitleL = new JLabel(textBundle.getString("ssefilegui.settings.guiLanguage"));

        final JComboBox interfaceLanguageCB = new JComboBox();
        Helpers.setUnicodeFontInWindows(interfaceLanguageCB);
        interfaceLanguageCB.addItem(new Item("", textBundle.getString("ssefilegui.settings.system")));
        
        String[] languages = coreBundle.getString("ssecore.config.sse.guiLanguagesList").split(Pattern.quote("||"));       
        for(int i = 0; i < languages.length; ++i) {
        	String[] currentLang = languages[i].split(Pattern.quote("|"));
        	interfaceLanguageCB.addItem(new Item(currentLang[0].trim(), currentLang[1].trim()));
        }

        JPanel rowInterfaceLanguage = makeRowComboBox(interfaceLanguageTitleL, interfaceLanguageCB, "(" + textBundle.getString("ssefilegui.text.restartNeeded") + ")");       
        String interfaceLanguage = sdh.getPersistentDataString(StaticConfig.FE_SETTINGS_GUI_LANGUAGE, "");

        int languageIndex = 0;
        for(int i = 0; i < interfaceLanguageCB.getItemCount(); ++i) {
        	if(interfaceLanguage.equals(((Item)interfaceLanguageCB.getItemAt(i)).getId())) {
        		languageIndex = i;
        		break;
        	}
        }
        interfaceLanguageCB.setSelectedIndex(languageIndex);
        //-
        
        
        // OK button
		okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if(outputEncDirCB.isSelected()) {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_ENABLED, true);
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_PATH, outputEncDirCurrentL.getName());
				}
				else {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_ENC_ENABLED, false);
				}
				
				if(outputDecDirCB.isSelected()) {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_ENABLED, true);
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_PATH, outputDecDirCurrentL.getName());
				}
				else {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_OUTPUTDIR_DEC_ENABLED, false);
				}
				
				sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_COMPRESSION, compressionListCB.getSelectedIndex());
				
				if(replaceWarningCB.isSelected()) {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_REPLACE_WARNING, true);
				}
				else {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_REPLACE_WARNING, false);
				}
				
				if(customCommandCB.isSelected()) {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED, true);
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC, ccAfterEncTF.getText().trim());
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC, ccAfterDecTF.getText().trim());
				}
				else {
					sdh.addOrReplacePersistentDataObject(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED, false);
				}
				
				String languageId = ((Item)interfaceLanguageCB.getItemAt(interfaceLanguageCB.getSelectedIndex())).getId();
				sdh.addOrReplacePersistentDataObject(StaticConfig.FE_SETTINGS_GUI_LANGUAGE, languageId);
				if(StaticConfig.PFTE_BUNDLE) {
					String[] alternativeLanguages = coreBundle.getString("ssecore.config.pte.guiLanguagesList").split(Pattern.quote("||"));    
					boolean isFoundAlternative = false;
					for (String language : alternativeLanguages) {
					    if (language.startsWith(languageId)) {
					    	isFoundAlternative = true;
					        break;
					    }
					}
					if(isFoundAlternative) { 
						SettingsDataHolder alternativeSDH = SettingsDataHolder.getSettingsDataHolder(StaticConfig.PTE_CONFIG_FILE);
						String pteInterfaceLanguage = alternativeSDH.getPersistentDataString(StaticConfig.PTE_SETTINGS_GUI_LANGUAGE, "en");
						boolean isFound = false;
						for (String language : languages) {
						    if (language.startsWith(pteInterfaceLanguage)) {
						    	isFound = true;
						        break;
						    }
						}
						
						if(isFound) {
							alternativeSDH.addOrReplacePersistentDataObject(StaticConfig.PTE_SETTINGS_GUI_LANGUAGE, languageId);
							try {
								alternativeSDH.save();
							} catch (Exception e1) {
								showErrorDialog(Helpers.getShortenedStackTrace(e1, 1));
							}	
						}
					}
				}
				
				try {
					sdh.save();
				} catch (Exception e1) {
					showErrorDialog(Helpers.getShortenedStackTrace(e1, 1));
				}				 
				
				SettingsDialogFE.this.dispose();
			}
		});
        buttonPane.add(okButton, gbc);
        
         JPanel settingsPane = new JPanel();
         settingsPane.setLayout(new BoxLayout(settingsPane, BoxLayout.Y_AXIS));
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(rowEncDir);
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(rowDecDir);
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(rowCompression);
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(rowReplaceWarning);
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(customCommandContainer);
         settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
         settingsPane.add(rowInterfaceLanguage);
         
         JScrollPane scrollFrame = new JScrollPane(settingsPane);
         scrollFrame.getVerticalScrollBar().setUnitIncrement(8);
         settingsPane.setAutoscrolls(true);
        
        this.add(scrollFrame, BorderLayout.CENTER);
        this.add(buttonPane, BorderLayout.SOUTH);
    }
    
    class Item 
    {
    	private String id;
    	private String description;

    	public Item(String id, String description) {
    		this.id = id;
    		this.description = description;
    	}

    	public String getId() {
    		return id;
    	}

    	public String getDescription() {
    		return description;
    	}

    	@Override
    	public String toString() {
    		return description;
    	}
	}
    
	private JPanel makeRowComboBox(JLabel title, JComboBox comboBox)
	{
		return makeRowComboBox(title, comboBox, null);
	}
	
	private JPanel makeRow(JLabel title, JLabel current, JCheckBox checkBox) 
	{
		title.setFont(new Font(title.getFont().getFamily(), Font.BOLD, LINE_TITLE_FONTSIZE));
		current.setFont(new Font(current.getFont().getFamily(), Font.PLAIN, LINE_CURRENT_FONTSIZE));
		checkBox.setBorder(new EmptyBorder(0, 20, 0, 0));
		
		JPanel rowL =  new JPanel(new BorderLayout());
        rowL.add(title, BorderLayout.NORTH);
        rowL.add(current, BorderLayout.CENTER);
        JPanel row =  new JPanel(new BorderLayout());
        row.add(rowL, BorderLayout.LINE_START);
        row.add(checkBox, BorderLayout.LINE_END);
        row.setBorder(new EmptyBorder(8, 10, 8, 15));
        
        return row;	
	}
	
	private JPanel makeRowComboBox(JLabel title, JComboBox comboBox, String description) 
	{
		title.setFont(new Font(title.getFont().getFamily(), Font.BOLD, LINE_TITLE_FONTSIZE));
		comboBox.setBorder(new EmptyBorder(8, 0, 8, 250));
		
		JPanel rowL =  new JPanel(new BorderLayout());
        rowL.add(title, BorderLayout.NORTH);
        if(description != null) {
	        JLabel descriptionL = new JLabel(description);
	        descriptionL.setFont(new Font(descriptionL.getFont().getFamily(), Font.PLAIN, FONTSIZE_SMALL));        
	        rowL.add(descriptionL, BorderLayout.CENTER);
        }
        rowL.add(comboBox, BorderLayout.SOUTH);
        JPanel row =  new JPanel(new BorderLayout());
        row.add(rowL, BorderLayout.CENTER);
        row.setBorder(new EmptyBorder(8, 10, 8, 15));
        
        return row;	
	}
	
	private void fillCurrentPathLabel(JLabel label, String path) 
	{
		if(path != null) {
			String ellipsized = ellipsizeMiddle(path, 45, 45);
			label.setText("<html><b>" + textBundle.getString("ssefilegui.settings.current") + ": </b>" + ellipsized + "</html>");
			label.setName(path);
			if(!ellipsized.equals(path)) label.setToolTipText(path);
	    	
			this.pack();
	    	this.setLocationRelativeTo(parentFrame);
		}
		else {
			label.setText("<html><b>" + textBundle.getString("ssefilegui.settings.current") + ": </b>(" + textBundle.getString("ssefilegui.settings.sameAsSource") + ")</html>");
			label.setName("");
			label.setToolTipText(null);
		}
	}
	    
    private void chooseDir(String mode, String startPath, JLabel currentLabel, JCheckBox checkBox)
    {   	
    	File dir = Helpers.showOpenDirDialog(this.parentFrame, new File(startPath));
    	if(dir != null) 
		{
			if(Helpers.writeTestFile(dir)) {				
				sdh.addOrReplacePersistentDataObject(mode, dir.getAbsolutePath());
				fillCurrentPathLabel(currentLabel, dir.getAbsolutePath());
			}
			else {			
				checkBox.setSelected(false);
				showErrorDialog(textBundle.getString("ssefilegui.text.DirIsReadonly")); 
			}
		} 
		else
		{
			checkBox.setSelected(false);
		}
    }
    
    private String getPath(String code) 
    {
    	String path = null; 
		try {
			path = sdh.getPersistentDataString(code);
			if(path == null || !(new File(path).exists())) path = System.getProperty("user.home");
			if(path == null || !(new File(path).exists())) path = null;
		} catch (Exception e) {
			// swallow
		}
		
		if(path == null) path = ".";
		
		return path;
    }
    
    private static String ellipsizeMiddle(String text, int startChars, int endChars)
    {
    	String regex = "(.{" + startChars + "}).+(.{" + endChars + "})";   	
    	return text.replaceFirst(regex, "$1...$2");
    }
    
    private void showErrorDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SettingsDialogFE.this.parentFrame, "", MessageDialog.ICON_NEGATIVE);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
}
