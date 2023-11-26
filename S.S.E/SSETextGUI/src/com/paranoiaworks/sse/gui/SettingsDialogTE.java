package com.paranoiaworks.sse.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.StaticConfig;
import com.paranoiaworks.sse.components.MessageDialog;
import com.paranoiaworks.sse.components.OkCancelDialog;
import com.paranoiaworks.sse.dao.SettingsDataHolder;

/**
 * Settings Dialog (single purposed - Text Encryptor)
 * 
 * @author Paranoia Works
 * @version 1.0.3
 */ 


@SuppressWarnings("serial")
public class SettingsDialogTE extends JDialog {
    
	private Frame parentFrame = null;
	private JButton okButton = null;
	private ResourceBundle textBundle;
	private ResourceBundle coreBundle;
	private SettingsDataHolder sdh;
	
	private static final int MIN_WIDTH = 380;
	private static final int HEIGHT = 100;
	private static final int LINE_TITLE_FONTSIZE = 14;
	private static final int LINE_CURRENT_FONTSIZE = 12;
	private static final int FONTSIZE_SMALL = 12;
	
	public SettingsDialogTE(Frame frame, ResourceBundle textBundle, ResourceBundle coreBundle, SettingsDataHolder sdh)
    {
        super(frame, textBundle.getString("ssetextgui.settings.settingsTitle"), true);
        this.parentFrame = frame;
        this.textBundle = textBundle;
        this.coreBundle = coreBundle;
        this.sdh = sdh;
        init();
    }
	
	public synchronized void showDialog() 
    {
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
						SettingsDialogTE.this.dispose();
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
				closeSettingsDialog.setTitle(textBundle.getString("ssetextgui.settings.settingsTitle"));
				closeSettingsDialog.setText(textBundle.getString("ssetextgui.settings.closeSettings"));
				closeSettingsDialog.showDialog();
			}
        });
        this.setMinimumSize(new Dimension(MIN_WIDTH, HEIGHT));

        this.setResizable(false);
        this.setLocationByPlatform(true);
        this.setLocationRelativeTo(parentFrame);   

        // Final Encoding
        JLabel finalEncodingTitleL = new JLabel(textBundle.getString("ssetextgui.settings.finalEncoding"));
        String[] encoderList = {
        		textBundle.getString("ssetextgui.settings.finalEncoding.Base64"),
        		textBundle.getString("ssetextgui.settings.finalEncoding.Base32"),
        		textBundle.getString("ssetextgui.settings.finalEncoding.Numbers")};
        final JComboBox encoderListCB = new JComboBox(encoderList);
        JPanel rowFinalEncoding = makeRowComboBox(finalEncodingTitleL, encoderListCB);       
        Integer finalEncoding = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_FINAL_ENCODING, 0);
        encoderListCB.setSelectedIndex(finalEncoding);
        
        // Compression
        JLabel compressionTitleL = new JLabel(textBundle.getString("ssetextgui.settings.compression"));
        String[] compressionList = {
        		textBundle.getString("ssetextgui.settings.addSpace.disabled"),
        		"LZMA"};
        final JComboBox compressionListCB = new JComboBox(compressionList);
        JPanel rowCompression = makeRowComboBox(compressionTitleL, compressionListCB);       
        Integer compression = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_COMPRESSION, 1);
        compressionListCB.setSelectedIndex(compression);
        
        // Add Space
        JLabel addSpaceTitleL = new JLabel(textBundle.getString("ssetextgui.settings.addSpace"));
        String[] spaceList = {textBundle.getString("ssetextgui.settings.addSpace.disabled"), "  1", "  2", "  3", "  4", "  5", "  6", "  7", "  8", "  9", "  10"};
        final JComboBox spaceListCB = new JComboBox(spaceList);
        JPanel rowAddSpace = makeRowComboBox(addSpaceTitleL, spaceListCB);      
        Integer addSpace = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_ADD_WHITESPACE, 0);
        spaceListCB.setSelectedIndex(addSpace);
        
        // Font Size
        JLabel fontSizeTitleL = new JLabel(textBundle.getString("ssetextgui.settings.fontSize"));
        String[] fontSizeList = {"  80%", "  100%", "  125%", "  150%", "  175%", "  200%", "  250%", "  300%", "  400%"};
        final JComboBox fontSizeListCB = new JComboBox(fontSizeList);
        JPanel rowFontSize = makeRowComboBox(fontSizeTitleL, fontSizeListCB);      
        Integer fontSize = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_FONT_SIZE, 100);
        int fontSizeIndex = 1;
        for(int i = 0; i < fontSizeList.length; ++i) {
        	if(fontSizeList[i].indexOf(Integer.toString(fontSize)) > 0) {
        		fontSizeIndex = i;
        		break;
        	}
        }      
        fontSizeListCB.setSelectedIndex(fontSizeIndex);
        
		//+ GUI Language
		JLabel interfaceLanguageTitleL = new JLabel(textBundle.getString("ssetextgui.settings.guiLanguage"));

        final JComboBox interfaceLanguageCB = new JComboBox();
        Helpers.setUnicodeFontInWindows(interfaceLanguageCB);
        interfaceLanguageCB.addItem(new Item("", textBundle.getString("ssetextgui.settings.system")));
        
        String[] languages = coreBundle.getString("ssecore.config.pte.guiLanguagesList").split(Pattern.quote("||"));       
        for(int i = 0; i < languages.length; ++i) {
        	String[] currentLang = languages[i].split(Pattern.quote("|"));
        	interfaceLanguageCB.addItem(new Item(currentLang[0].trim(), currentLang[1].trim()));
        }

        JPanel rowInterfaceLanguage = makeRowComboBox(interfaceLanguageTitleL, interfaceLanguageCB, "(" + textBundle.getString("ssetextgui.text.restartNeeded") + ")");       
        String interfaceLanguage = sdh.getPersistentDataString(StaticConfig.PTE_SETTINGS_GUI_LANGUAGE, "");

        int languageIndex = 0;
        for(int i = 0; i < interfaceLanguageCB.getItemCount(); ++i) {
        	if(interfaceLanguage.equals(((Item)interfaceLanguageCB.getItemAt(i)).getId())) {
        		languageIndex = i;
        		break;
        	}
        }
        interfaceLanguageCB.setSelectedIndex(languageIndex);
        //-
        
        okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				sdh.addOrReplacePersistentDataObject(SSETextGUI.PTE_SETTINGS_FINAL_ENCODING, encoderListCB.getSelectedIndex());
				sdh.addOrReplacePersistentDataObject(SSETextGUI.PTE_SETTINGS_COMPRESSION, compressionListCB.getSelectedIndex());
				sdh.addOrReplacePersistentDataObject(SSETextGUI.PTE_SETTINGS_ADD_WHITESPACE, spaceListCB.getSelectedIndex());
				
				try {
					int fontSize = Integer.parseInt(fontSizeListCB.getSelectedItem().toString().replaceAll("%", "").trim());
					sdh.addOrReplacePersistentDataObject(SSETextGUI.PTE_SETTINGS_FONT_SIZE, fontSize);					
				} catch (Exception e2) {

					e2.printStackTrace();
				}
				
				String languageId = ((Item)interfaceLanguageCB.getItemAt(interfaceLanguageCB.getSelectedIndex())).getId();
				sdh.addOrReplacePersistentDataObject(StaticConfig.PTE_SETTINGS_GUI_LANGUAGE, languageId);
				if(StaticConfig.PFTE_BUNDLE) {
					String[] alternativeLanguages = coreBundle.getString("ssecore.config.sse.guiLanguagesList").split(Pattern.quote("||"));  
					boolean isFoundAlternative = false;
					for (String language : alternativeLanguages) {
					    if (language.startsWith(languageId)) {
					    	isFoundAlternative = true;
					        break;
					    }
					}
					if(isFoundAlternative) {
						SettingsDataHolder alternativeSDH = SettingsDataHolder.getSettingsDataHolder(StaticConfig.SSEFE_CONFIG_FILE);
						String feInterfaceLanguage = alternativeSDH.getPersistentDataString(StaticConfig.FE_SETTINGS_GUI_LANGUAGE, "en");
						boolean isFound = false;
						for (String language : languages) {
						    if (language.startsWith(feInterfaceLanguage)) {
						    	isFound = true;
						        break;
						    }
						}
						
						if(isFound) {
							alternativeSDH.addOrReplacePersistentDataObject(StaticConfig.FE_SETTINGS_GUI_LANGUAGE, languageId);
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
				
				SettingsDialogTE.this.dispose();
			}
		});
        buttonPane.add(okButton, gbc);
        
        JPanel settingsPane = new JPanel();
        settingsPane.setLayout(new BoxLayout(settingsPane, BoxLayout.Y_AXIS));
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        settingsPane.add(rowFinalEncoding);
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        settingsPane.add(rowCompression);
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        settingsPane.add(rowAddSpace);
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        settingsPane.add(rowFontSize);
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        settingsPane.add(rowInterfaceLanguage);
        settingsPane.add(new JSeparator(SwingConstants.HORIZONTAL));
        
        this.add(settingsPane, BorderLayout.CENTER);
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
    
    private void showErrorDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SettingsDialogTE.this.parentFrame, "", MessageDialog.ICON_NEGATIVE);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
}
