package com.paranoiaworks.sse.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.zip.DataFormatException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import com.paranoiaworks.sse.Encryptor;
import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.StaticConfig;
import com.paranoiaworks.sse.checklic.Parcl;
import com.paranoiaworks.sse.components.ComboBoxDisableable;
import com.paranoiaworks.sse.components.MessageDialog;
import com.paranoiaworks.sse.components.PlaceholderPasswordField;
import com.paranoiaworks.sse.components.SimpleDragDropListener;
import com.paranoiaworks.sse.components.SteganogramSettingsDialog;
import com.paranoiaworks.sse.components.WaitDialog;
import com.paranoiaworks.sse.dao.SettingsDataHolder;

import ext.RelativeLayout;

/**
 * P.T.E. for PC
 * 
 * @author Paranoia Works
 * @version 1.2.6
 */ 

public class SSETextGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String appVersionCode = "15.0.4";
    private static final String publishYear = "2023";
    private static final String UNENCRYPTED_TEXTAREA_IDENTIFIER = "UTA";
    private static final String ENCRYPTED_TEXTAREA_IDENTIFIER = "ETA";
    private static final String ENC_FILES_DIR = "My PTE Files";
    private static final String STEGANOGRAMS_DIR = "Steganograms";
    
    private static final String PTE_SETTINGS_ALGORITHM = "PTE_SETTINGS_ALGORITHM";
    private static final String PTE_STEGANOGRAM_LAST_CARRIERDIR= "PTE_STEGANOGRAM_LAST_CARRIERDIR";
    protected static final String PTE_SETTINGS_FINAL_ENCODING = "PTE_SETTINGS_FINAL_ENCODING";
    protected static final String PTE_SETTINGS_COMPRESSION = "PTE_SETTINGS_COMPRESSION";
    protected static final String PTE_SETTINGS_ADD_WHITESPACE = "PTE_SETTINGS_ADD_WHITESPACE";
    protected static final String PTE_SETTINGS_FONT_SIZE = "PTE_SETTINGS_FONT_SIZE";
    
    private static final boolean IS_MAC = Helpers.isMac();
    private static final int SQUARE_BUTTON_SIZE = IS_MAC ? 40 : 34;
    
    private File saveLoadDir = null;
    private File steganogramsDir = null;
    private JComboBox algorithmCB;
    private PlaceholderPasswordField passwordTF;
    private PlaceholderPasswordField passwordConfirmTF;
    private JDialog helpDialog;
    
    private JTextArea unencryptedTextArea;
    private JTextArea encryptedTextArea;
    private JLabel statusBar;
    private JLabel charCounter;
    private final JCheckBox showHidePasswordCB;
		
    private int algorithmCode = -1;
    private TEChangeResolver changeResolver;
    private boolean proVersion = false;
    private boolean blinkBlock = false;
    private boolean longPasswordWarning = true;
    private File lastDirectorySteganogram = null;
    private int defFontSize;
    private double fontSizeMultiplier;
    int textSizeIncDec;
    
    private static final SettingsDataHolder sdh;
	private int finalEncoding;
	private int addWhiteSpace;
	
	private static ResourceBundle textBundle;
	private static ResourceBundle coreBundle;
	private static Locale locale;
	
	static {
		sdh = SettingsDataHolder.getSettingsDataHolder(StaticConfig.PTE_CONFIG_FILE); 
		String interfaceLanguage = sdh.getPersistentDataString(StaticConfig.PTE_SETTINGS_GUI_LANGUAGE, null);
		locale = Helpers.getLocale(interfaceLanguage);
		textBundle = ResourceBundle.getBundle("res.SSETextGUI", locale);
		coreBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", locale);
		
		if(Helpers.isCurrentLanguageInUnicodeList() && Helpers.isWindows()) {
			Helpers.setUnicodeFont();
		}
						
		ResourceBundle fileChooserRB = null;		
		try {
			List<String> fileChooserKeys = Collections.list(ResourceBundle.getBundle("res.JFileChooserKeys").getKeys());
			fileChooserRB = ResourceBundle.getBundle("res.JFileChooser", locale);		
			for(int i = 0; i < fileChooserKeys.size(); ++i)
			{
				try{UIManager.put(fileChooserKeys.get(i), fileChooserRB.getString(fileChooserKeys.get(i)));}catch(Exception e){}
			}
	
		} catch (Exception e) {
			// swallow exception
		}		
	}

    public static void main(String[] args) {
        new SSETextGUI();
    }

    /** Prepare and Render Main Window */
    public SSETextGUI() {
        super(StaticConfig.PFTE_BUNDLE ? textBundle.getString("ssetextgui.label.MainLabelPTE") : textBundle.getString("ssetextgui.label.MainLabel"));
        textSizeIncDec = Integer.parseInt(textBundle.getString("ssetextgui.spec.textPaneFontIncDec"));
        this.setMinimumSize(new Dimension(620, 440));
        this.setSize(new Dimension(640, 480));      
               
        String[] algorithmList = {"AES (256 bit)", "RC6 (256 bit)", "Serpent (256 bit)", "Twofish (256 bit)", "GOST28147 (256 bit)", "Blowfish (448 bit)", "Threefish (1024 bit)", "SHACAL-2 (512 bit)", "Paranoia C4 (2048 bit)"};
        changeResolver = new TEChangeResolver();
        
        fontSizeMultiplier = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_FONT_SIZE, 100) / 100.0;
        
        proVersion = Parcl.loadcheck(sdh);
        if(!proVersion) {
        	SettingsDataHolder alternativeSDH = SettingsDataHolder.getSettingsDataHolder(StaticConfig.SSEFE_CONFIG_FILE, true);
        	proVersion = Parcl.loadcheckAlternative(sdh, alternativeSDH);
        }   
        
        int[] selectableAlgInterval = {0, 5};
        if(proVersion) {
        	this.setTitle(this.getTitle() + " PRO");
        	selectableAlgInterval = null;
        } 
        
        saveLoadDir = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ENC_FILES_DIR);
        if(!saveLoadDir.exists()) saveLoadDir.mkdirs();
        steganogramsDir = new File(saveLoadDir.getAbsoluteFile() + System.getProperty("file.separator") + STEGANOGRAMS_DIR);
        if(!steganogramsDir.exists()) steganogramsDir.mkdirs();
        lastDirectorySteganogram = steganogramsDir;
        
        //+ Top Pane
        final JPanel topPane = new JPanel(new BorderLayout());
        JPanel topPaneLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPaneLeft.setBorder(new EmptyBorder(2, 0, 8, 0));
        JPanel bottomPane = new JPanel(new GridBagLayout());
        bottomPane.setBorder(new EmptyBorder(5, 0, 5, 2));
        
        JPanel passwordPane = new JPanel(new BorderLayout());
        JPanel passwordLabelPane = new JPanel(new BorderLayout());
        JLabel passworLabel = new JLabel(textBundle.getString("ssetextgui.label.PasswordLabel"), SwingConstants.LEFT);
        if(IS_MAC) passworLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        showHidePasswordCB = new JCheckBox(textBundle.getString("ssetextgui.label.ShowHidePasswordCB"));
        passwordTF = new PlaceholderPasswordField();
        Helpers.setUnicodeFontInWindows(passwordTF);
        passwordTF.enableInputMethods(true);
        passwordTF.setPlaceholder(" " + textBundle.getString("ssetextgui.label.PasswordPH"));
        final char echoChar = passwordTF.getEchoChar();
        passwordTF.setPreferredSize(new Dimension(250, 24));
        passwordTF.getDocument().addDocumentListener(getPasswordOnChangeListener());
        passwordConfirmTF = new PlaceholderPasswordField();
        Helpers.setUnicodeFontInWindows(passwordConfirmTF);
        passwordConfirmTF.enableInputMethods(true);
        passwordConfirmTF.setPlaceholder(" " + textBundle.getString("ssetextgui.label.PasswordConfirmPH"));
        passwordConfirmTF.setPreferredSize(new Dimension(250, 24));
        passwordLabelPane.add(passworLabel, BorderLayout.LINE_START);
        passwordLabelPane.add(showHidePasswordCB, BorderLayout.LINE_END);
        passwordPane.add(passwordLabelPane, BorderLayout.PAGE_START);
        passwordPane.add(passwordTF, BorderLayout.CENTER);
        passwordPane.add(passwordConfirmTF, BorderLayout.PAGE_END);
        
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
            	try {
            		sdh.addOrReplacePersistentDataObject(PTE_SETTINGS_ALGORITHM, algorithmCB.getSelectedIndex());
            		sdh.save();
				} catch (Exception e) {
					e.printStackTrace();
				}
                System.exit(0);
            }
        });
        
        showHidePasswordCB.addActionListener(new ActionListener() 
        {	
        	public void actionPerformed(ActionEvent evt)  
			{
        		char[] tempPw = passwordTF.getPassword();
        		if(showHidePasswordCB.isSelected()) {			
        			passwordTF.setEchoChar((char)0);
        			passwordConfirmTF.setText(Helpers.createStringWithLength(tempPw.length, '.'));
        			passwordConfirmTF.setEnabled(false);
        			passwordConfirmTF.setBackground(topPane.getBackground());
        		}
        		else { 
        			passwordTF.setEchoChar(echoChar);
        			passwordConfirmTF.setEnabled(true);
        			Helpers.setTextToPasswordField(passwordConfirmTF, tempPw);
        			passwordConfirmTF.setBackground(passwordTF.getBackground());
        		}
    			Arrays.fill(tempPw, '\u0000');
			}
        });
        
        JPanel algorithmPane = new JPanel(new BorderLayout());
        algorithmPane.setBorder(new EmptyBorder(0, 10, 21, 0));
        JLabel algorithmLabel = new JLabel(textBundle.getString("ssetextgui.label.AlgorithmLabel"), SwingConstants.LEFT);
        if(IS_MAC) 
        	algorithmLabel.setBorder(new EmptyBorder(3, 6, 4, 0));
        else 
        	algorithmLabel.setBorder(new EmptyBorder(3, 0, 4, 0));
        algorithmCB = new ComboBoxDisableable(SSETextGUI.this, algorithmList, selectableAlgInterval, coreBundle.getString("ssecore.pro.proVersionOnlyAlgorithm"));
        
        Integer algorithmCBSetting = sdh.getPersistentDataInteger(PTE_SETTINGS_ALGORITHM);
        if(algorithmCBSetting != null && algorithmCBSetting + 1 <= algorithmList.length)        
        	algorithmCB.setSelectedIndex(algorithmCBSetting);
        else
        	algorithmCB.setSelectedIndex(5);
        algorithmCB.setEditable(false);
        algorithmPane.add(algorithmLabel, BorderLayout.PAGE_START);
        algorithmPane.add(algorithmCB, BorderLayout.CENTER);
        
        JPanel topPaneRight = new JPanel(new BorderLayout());
        JPanel topPaneRightButtons = new JPanel(new BorderLayout());
        topPaneRight.setBorder(new EmptyBorder(5, 5, 5, 5));
        JButton helpButton = new JButton();
        helpButton.setToolTipText(textBundle.getString("ssetextgui.label.HelpButton"));
        helpButton.setPreferredSize(new Dimension(SQUARE_BUTTON_SIZE, SQUARE_BUTTON_SIZE));
        JButton settingsButton = new JButton();
        settingsButton.setToolTipText(textBundle.getString("ssetextgui.settings.settingsTitle"));
        settingsButton.setPreferredSize(new Dimension(SQUARE_BUTTON_SIZE, SQUARE_BUTTON_SIZE));     
        topPaneRightButtons.add(settingsButton, BorderLayout.BEFORE_LINE_BEGINS);
        topPaneRightButtons.add(new JLabel(" "), BorderLayout.CENTER);
        topPaneRightButtons.add(helpButton, BorderLayout.EAST);
        topPaneRight.add(topPaneRightButtons, BorderLayout.EAST);

        helpButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		ResourceBundle[] bundles = {textBundle, coreBundle};
        		final HelpDialogTE helpDialog = new HelpDialogTE(SSETextGUI.this, bundles, sdh, 
        				StaticConfig.PFTE_BUNDLE ? StaticConfig.PFTE_VERSION : appVersionCode, publishYear);
        		helpDialog.setShowProUpdate(!proVersion);
        		helpDialog.showDialog();
		    }
        });    
        
        settingsButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		final SettingsDialogTE settingsDialog = new SettingsDialogTE(SSETextGUI.this, textBundle, coreBundle, sdh);
        		settingsDialog.showDialog();

        		fontSizeMultiplier = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_FONT_SIZE, 100) / 100.0;
        		{
	        		Font f = encryptedTextArea.getFont();
	        		Font f2 = new Font(f.getFontName(), f.getStyle(), (int)Math.round(defFontSize * fontSizeMultiplier));
	        		encryptedTextArea.setFont(f2);
        		}
        		{
	        		Font f = unencryptedTextArea.getFont();
	        		Font f2 = new Font(f.getFontName(), f.getStyle(), (int)Math.round((defFontSize + textSizeIncDec) * fontSizeMultiplier));
	        		unencryptedTextArea.setFont(f2);
        		}
		    }
        }); 
        
        charCounter = new JLabel("0 | 0", SwingConstants.RIGHT);
        charCounter.setBorder(new EmptyBorder(35, 0, 0, 0));
        topPaneRight.add(charCounter, BorderLayout.SOUTH);

        topPaneLeft.add(passwordPane);
        topPaneLeft.add(algorithmPane);
        topPane.add(topPaneLeft, BorderLayout.LINE_START);
        topPane.add(topPaneRight, BorderLayout.LINE_END);
        //- Top Pane   

        
        //+ Main Pane
        RelativeLayout relativeCentral = new RelativeLayout(RelativeLayout.Y_AXIS);
        relativeCentral.setFill(true);
        JPanel centralPane = new JPanel(relativeCentral);
         
        // Unencrypted Text Area
        JPanel unencryptedTextAreaWrapper = new JPanel(new BorderLayout());
        
        unencryptedTextAreaWrapper.setBorder(BorderFactory.createLineBorder(Color.decode("#00FF00")));
        unencryptedTextArea = new JTextArea();
        Helpers.setUnicodeFontInWindows(unencryptedTextArea);
        unencryptedTextArea.setLineWrap(true);
        unencryptedTextArea.setWrapStyleWord(true);
        unencryptedTextArea.setEditable(true);        
        unencryptedTextArea.setMargin(new Insets(5,5,5,5));
        unencryptedTextArea.setName(UNENCRYPTED_TEXTAREA_IDENTIFIER); 
        JLabel placeHolderDec = new JLabel(textBundle.getString("ssetextgui.text.UnencryptedText"), SwingConstants.CENTER);
        placeHolderDec.setForeground(new Color(0.0f, 0.0f, 0.0f, 0.1f));
        unencryptedTextArea.setLayout(new BorderLayout());
        unencryptedTextArea.add(placeHolderDec, BorderLayout.CENTER);
        installContextMenu(unencryptedTextArea); 
        defFontSize = unencryptedTextArea.getFont().getSize();

        {
	        Font f = unencryptedTextArea.getFont();
	        Font f2 = new Font(f.getFontName(), f.getStyle(), (int)Math.round((f.getSize() + textSizeIncDec) * fontSizeMultiplier));
	        unencryptedTextArea.setFont(f2);
        }

        unencryptedTextArea.getDocument().addDocumentListener(getTextAreasOnChangeListener());
        
        JScrollPane scrollUnenc = new JScrollPane(unencryptedTextArea);
        scrollUnenc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollUnenc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        unencryptedTextAreaWrapper.add(scrollUnenc, BorderLayout.CENTER);
        centralPane.add(unencryptedTextAreaWrapper, new Float(50));
        
        // Encrypt/Decrypt Buttons
        JPanel centralBottom = new JPanel(new BorderLayout());
        JPanel encDecPane = new JPanel(new BorderLayout());
        JPanel encDecPaneLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel encDecPaneRight = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JLabel delimiterVertical = new JLabel("  |  ", SwingConstants.CENTER);
        JButton encryptButton = new JButton(textBundle.getString("ssetextgui.label.EncryptButton"));
        JButton decryptButton = new JButton(textBundle.getString("ssetextgui.label.DecryptButton"));
        final JButton cleanButton = new JButton("");
        final JButton moreButton = new JButton("");
        encDecPaneLeft.add(encryptButton);
        encDecPaneLeft.add(decryptButton);
        encDecPaneRight.add(cleanButton);
        encDecPaneRight.add(delimiterVertical);
        encDecPaneRight.add(moreButton);
        encDecPane.add(encDecPaneLeft, BorderLayout.WEST);
        encDecPane.add(encDecPaneRight, BorderLayout.EAST);
        centralBottom.add(encDecPane, BorderLayout.NORTH);
        
        try {
	        Image imgHelp = Helpers.loadImageFromResource(SSETextGUI.class, "icon_help", "png");
        	Image imgSettings = Helpers.loadImageFromResource(SSETextGUI.class, "icon_settings", "png");
        	Image imgEnc = Helpers.loadImageFromResource(SSETextGUI.class, "arrow_down_red", "png");
			Image imgDec = Helpers.loadImageFromResource(SSETextGUI.class, "arrow_up_green", "png");
			Image imgClean = Helpers.loadImageFromResource(SSETextGUI.class, "clean_icon", "png");
			Image imgMore = Helpers.loadImageFromResource(SSETextGUI.class, "more_icon", "png");
			
			helpButton.setIcon(new ImageIcon(imgHelp));
			settingsButton.setIcon(new ImageIcon(imgSettings));
			encryptButton.setIcon(new ImageIcon(imgEnc));
			decryptButton.setIcon(new ImageIcon(imgDec));
			cleanButton.setIcon(new ImageIcon(imgClean));
			moreButton.setIcon(new ImageIcon(imgMore));
			
			List<Image> appIcons = new ArrayList<Image>();
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_pte_16.png")));
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_pte_32.png")));	
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_pte_64.png")));	
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_pte_128.png")));	
			this.setIconImages(appIcons);
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        
        encryptButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		Encryptor encryptor = null;
        		String orgString = "";
        		String encString = "";
        		
        		orgString = unencryptedTextArea.getText().trim();
        		if(orgString.equals(""))
        		{
        			blinkComponent(unencryptedTextArea);
        			showAlert(textBundle.getString("ssetextgui.text.EmptyDecTextArea"));
        			
        			return;
        		}
        		
        		try {
        			encryptor = getEncryptor(true);
        			if(encryptor == null) return;  	
        			
        			finalEncoding = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_FINAL_ENCODING, 0);
        			addWhiteSpace = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_ADD_WHITESPACE, 0);
        			boolean compression = sdh.getPersistentDataInteger(SSETextGUI.PTE_SETTINGS_COMPRESSION, 1) == 1 ? true : false;
        			
        			if(addWhiteSpace > 0)
        				encryptedTextArea.setWrapStyleWord(true);
        			else
        				encryptedTextArea.setWrapStyleWord(false);
            		
        			encString = encryptor.encryptString(orgString, finalEncoding, addWhiteSpace, compression);
            		
            		encryptedTextArea.setText(encString);
            		changeResolver.setLastProcessed(unencryptedTextArea.getText(), encryptedTextArea.getText());
            		encryptedTextArea.setForeground(Color.black);
            		setStatusBarText(textBundle.getString("ssetextgui.label.EncryptButton") + " : " + encryptor.getEncryptAlgorithmComment() + " : OK");
        		} catch (Exception e) {
        			setStatusBarText(textBundle.getString("ssetextgui.text.EncryptionFailed"));
        			showAlert(textBundle.getString("ssetextgui.text.EncryptionFailed"));
        			e.printStackTrace();
        		}
        		finally {
					if(encryptor != null) {
	        			encryptor.wipeMasterKeys();
						encryptor = null;
					}
				}
		    }
        });
        
        decryptButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		Encryptor encryptor = null;
        		String orgString = "";
        		String decString = "";
        		
         		orgString = encryptedTextArea.getText().trim();
        		if(orgString.equals(""))
        		{
        			blinkComponent(encryptedTextArea);
        			showAlert(textBundle.getString("ssetextgui.text.EmptyEncTextArea"));
        			return;
        		}
         		
        		try {
        			encryptor = getEncryptor(false);
        			if(encryptor == null) return;
            		decString = encryptor.decryptString(orgString);
            		unencryptedTextArea.setText(decString);
            		changeResolver.setLastProcessed(unencryptedTextArea.getText(), encryptedTextArea.getText());
            		encryptedTextArea.setForeground(Color.black);
            		setStatusBarText(textBundle.getString("ssetextgui.label.DecryptButton") + " : " + encryptor.getDecryptAlgorithmComment() + " : OK");
        		} catch (NoSuchAlgorithmException e) {
        			setStatusBarText(textBundle.getString("ssetextgui.text.UnknownAlgorithm"));
        			showAlert(textBundle.getString("ssetextgui.text.UnknownAlgorithm"));
				} catch (DataFormatException e) {
					String legitPrefix = "3000::";
					String errorMessage = textBundle.getString("ssetextgui.text.DecryptionFailed");
					if(e.getMessage().startsWith(legitPrefix)) errorMessage = e.getMessage().substring(6, e.getMessage().length());
					setStatusBarText(errorMessage);
					showAlert(errorMessage);
        		} catch (Exception e) {
					setStatusBarText(textBundle.getString("ssetextgui.text.DecryptionFailed"));
					showAlert(textBundle.getString("ssetextgui.text.DecryptionFailed"));
        		}  	
        		finally {
					if(encryptor != null) {
	        			encryptor.wipeMasterKeys();
						encryptor = null;
					}
				}
		    }
        });
        
        cleanButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		unencryptedTextArea.setText("");
        		encryptedTextArea.setText("");
        		changeResolver.resetLastProcessed();
		    }
        });
        

        //+ More Button popup menu
        final JPopupMenu popupMore = new JPopupMenu();
        
        JMenuItem importSteganogram = new JMenuItem(new AbstractAction(textBundle.getString("ssetextgui.text.SteganogramImport") ) 
        {
            public void actionPerformed(ActionEvent e) 
            {
            	final Encryptor encryptor = getEncryptor(false);
            	if(encryptor == null) return;
            	importSteganogram(encryptor);
            }
        });

		
		JMenuItem exportSteganogram = new JMenuItem(new AbstractAction(textBundle.getString("ssetextgui.text.SteganogramExport") ) 
		{
            public void actionPerformed(ActionEvent e) 
            {
        		if(unencryptedTextArea.getText().trim().equals(""))
        		{
        			showAlert(textBundle.getString("ssetextgui.text.EmptyDecTextArea"));
        			return;
        		}
            	
            	final Encryptor encryptor = getEncryptor(true);
				if(encryptor == null) return;
				exportSteganogram(encryptor);
            }
        });
		
		
        try {
        	Image imgImportSteganogram = Helpers.loadImageFromResource(SSETextGUI.class, "import_steganogram", "png");
        	Image imgExportSteganogram = Helpers.loadImageFromResource(SSETextGUI.class, "export_steganogram", "png");       	
        	importSteganogram.setIcon(new ImageIcon(imgImportSteganogram));
        	exportSteganogram.setIcon(new ImageIcon(imgExportSteganogram));
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		popupMore.add(importSteganogram);
		popupMore.add(exportSteganogram);
		//- More Button popup menu
             
        moreButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		popupMore.show(moreButton, 0, moreButton.getBounds().height);
		    }
        });

        
        // Encrypted Text Area        
        JPanel encryptedTextAreaWrapper = new JPanel(new BorderLayout());
        encryptedTextAreaWrapper.setBorder(BorderFactory.createLineBorder(Color.decode("#FF0000")));
        encryptedTextArea = new JTextArea();  
        encryptedTextArea.setEditable(true);        
        encryptedTextArea.setMargin(new Insets(5,5,5,5));
        encryptedTextArea.setLineWrap(true);
        encryptedTextArea.setName(ENCRYPTED_TEXTAREA_IDENTIFIER);    
        encryptedTextArea.getDocument().addDocumentListener(getTextAreasOnChangeListener());
        
        {
	        Font f = encryptedTextArea.getFont();
	        Font f2 = new Font("monospaced", Font.PLAIN, (int)Math.round(f.getSize() * fontSizeMultiplier));
	        encryptedTextArea.setFont(f2);
        }
        
        JLabel placeHolderEnc = new JLabel(textBundle.getString("ssetextgui.text.EncryptedText"), SwingConstants.CENTER);
        placeHolderEnc.setForeground(new Color(0.0f, 0.0f, 0.0f, 0.1f));
        encryptedTextArea.setLayout(new BorderLayout());
        encryptedTextArea.add(placeHolderEnc, BorderLayout.CENTER);
        installContextMenu(encryptedTextArea);
        
        JScrollPane scrollEnc = new JScrollPane(encryptedTextArea);
        scrollEnc.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollEnc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        encryptedTextAreaWrapper.add(scrollEnc, BorderLayout.CENTER);
        centralBottom.add(encryptedTextAreaWrapper, BorderLayout.CENTER);
        centralPane.add(centralBottom, new Float(50));
        //- Main Pane

        //+ Bottom Pane
        GridBagConstraints gbc = new GridBagConstraints();
        JLabel copyrightLabel = new JLabel(textBundle.getString("ssetextgui.label.BottomLabel").replaceAll("!year!", publishYear), SwingConstants.RIGHT);
        copyrightLabel.setBorder(new EmptyBorder(0, 10, 0, 5));
        copyrightLabel.setForeground(Color.gray);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.gridx = 1;
        gbc.gridy = 0;
        bottomPane.add(copyrightLabel, gbc);
        
        statusBar = new JLabel(" ", SwingConstants.LEFT);
        statusBar.setBorder(new EmptyBorder(0, 5, 0, 0));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        bottomPane.add(statusBar, gbc);
        //- Bottom Pane

        
        // Add components to the content
        this.getContentPane().add(BorderLayout.NORTH, topPane);
        this.getContentPane().add(BorderLayout.CENTER, centralPane);
        this.getContentPane().add(BorderLayout.PAGE_END, bottomPane);
        
        // Center
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
        this.setLocation(x, y);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        new DropTarget(unencryptedTextArea, getStringLoadListener(unencryptedTextArea));
        new DropTarget(encryptedTextArea, getStringLoadListener(encryptedTextArea));
        
        EventQueue.invokeLater(new Runnable(){
        	public void run() 
        	{
        		passwordTF.grabFocus();
        		passwordTF.requestFocus();
        		if(sdh.getErrorMessage() != null)
        			showErrorDialog(sdh.getErrorMessage());
        		
        		/*
        		String newsDisplayed = "WNDV3";
        		boolean wndv3 = sdh.getPersistentDataBoolean(newsDisplayed, false);
        		if(!wndv3) {
        			showAlertNews(textBundle.getString("ssetextgui.spec.whatsnew3"));
        			
    				try {
    					sdh.addOrReplacePersistentDataObject(newsDisplayed, true);	
    					sdh.save();
    				} catch (Exception e1) {
    					showErrorDialog(Helpers.getShortenedStackTrace(e1, 1));
    				}
        		}
        		*/
        	}
        });
    }
    
    /** Make Encryptor */
    private Encryptor getEncryptor(boolean checkPasswordConfirmation)
    {
		char[] password = passwordTF.getPassword();
		password = Helpers.trim(password);
		
		char[] passwordConfirm = passwordConfirmTF.getPassword();
		passwordConfirm = Helpers.trim(passwordConfirm);
		
		algorithmCode = Encryptor.positionToAlgCode(algorithmCB.getSelectedIndex());
		
		try {
			if(password.length < 1)
			{
				showAlert(textBundle.getString("ssetextgui.text.EnterPassword"));
				throw new IllegalArgumentException();
			}
			
			if(!Arrays.equals(password, passwordConfirm) && passwordConfirmTF.isEnabled() && checkPasswordConfirmation)
			{
				showAlert(textBundle.getString("ssetextgui.text.PasswordsDontMatch"));
				throw new IllegalArgumentException();
			}
			
		} catch (IllegalArgumentException e) {
			Arrays.fill(password, '\u0000');
			Arrays.fill(passwordConfirm, '\u0000');
			return null;
		}
		
		Encryptor encryptor;
		try {
			encryptor = new Encryptor(password, algorithmCode, Encryptor.PURPOSE_TEXT_ENCRYPTION, true);
		} catch (Exception e) {
			encryptor = null;
			e.printStackTrace();
		}
		
		Arrays.fill(password, '\u0000');
		Arrays.fill(passwordConfirm, '\u0000');
		
		return encryptor;
    }
    
    /** Import Steganogram */
    private void importSteganogram(final Encryptor encryptor)
    {   	
		File startDir = lastDirectorySteganogram != null && lastDirectorySteganogram.exists() ? lastDirectorySteganogram : saveLoadDir;
		File selectedFile = Helpers.showOpenFileDialog(this, startDir, "P.T.E. Steganogram (*.jpg)", "jpg");
		
		if(selectedFile != null) 
		{
			final File file = selectedFile;
			lastDirectorySteganogram = file.getParentFile();
        	final WaitDialog waitDialog = new WaitDialog(SSETextGUI.this, textBundle.getString("ssetextgui.text.Processing") + " ...");
			final StringBuffer errorMessage = new StringBuffer();
        	
        	SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() 
        	{
        		@Override
        		protected Void doInBackground() throws Exception 
        		{	
                	try {
    					String out = encryptor.importTextFromSteganogram(file);     					
    					unencryptedTextArea.setText(out);

    				} catch (IllegalStateException e) {
    					errorMessage.append(textBundle.getString("ssetextgui.text.InvalidSteganogram"));
    				} catch (ArrayIndexOutOfBoundsException e) {
    					errorMessage.append(textBundle.getString("ssetextgui.text.InvalidSteganogram"));
    				} catch (Exception e) {
    					errorMessage.append(textBundle.getString("ssetextgui.text.DecryptionFailed") + "<br/><br/>" + Helpers.getShortenedStackTrace(e, 1));
    				} catch (Throwable t) {
    					errorMessage.append(Helpers.getShortenedStackTrace(t, 1));	
    				}
            		 
        			return null;
        		}

        		@Override
        		protected void done() 
        		{
        			waitDialog.dispose();
        			if(errorMessage.length() > 0) {
        				setStatusBarText(errorMessage.toString().replaceAll("<br/>", " "));
        				showErrorDialog(errorMessage.toString());  
        			}
        			else {
        				setStatusBarText(textBundle.getString("ssetextgui.label.DecryptButton") + " : " + encryptor.getDecryptAlgorithmComment() + " + F5 (" + file.getName() + ") : OK");
        			}
        			
					if(encryptor != null) {
	        			encryptor.wipeMasterKeys();
					}
        		}
        	};

        	sw.execute();
        	waitDialog.showDialog();
		}
    }
    
    /** Export Steganogram */
    private void exportSteganogram(final Encryptor encryptor)
    {   	
    	String lastCarrierPath = sdh.getPersistentDataString(PTE_STEGANOGRAM_LAST_CARRIERDIR);
    	File lastDirectorySteganogramSource = null;
    	try {lastDirectorySteganogramSource = new File(lastCarrierPath);} catch (Exception e) {} 

		
		File startDir = lastDirectorySteganogramSource != null && lastDirectorySteganogramSource.exists() ? lastDirectorySteganogramSource : saveLoadDir;
		File selectedFile = Helpers.showOpenFileDialog(this, startDir, textBundle.getString("ssetextgui.text.CarrierFile") + " (*.jpg, *.png, *.bmp)", "jpg", "png", "bmp");
		
		if(selectedFile != null) 
		{
			final File file = selectedFile;
			
			sdh.addOrReplacePersistentDataObject(PTE_STEGANOGRAM_LAST_CARRIERDIR, file.getParentFile().getAbsolutePath());
			
			Object[] steganogramParameters = null;
			try {
				final SteganogramSettingsDialog settingsDialog = new SteganogramSettingsDialog(SSETextGUI.this, file);
				steganogramParameters = settingsDialog.showDialog();
			} catch (IllegalStateException e1) {
				showErrorDialog(e1.getLocalizedMessage());  
			}catch (Exception e1) {
				showErrorDialog(Helpers.getShortenedStackTrace(e1, 1));  
			}
    		
    		final Double imageScale = (Double)steganogramParameters[0];
    		final Integer jpegQuality = (Integer)steganogramParameters[1];
    		
    		if(imageScale == null || jpegQuality == null) return;			
			
			final WaitDialog waitDialog = new WaitDialog(SSETextGUI.this, textBundle.getString("ssetextgui.text.Processing") + " ...");
			final StringBuffer outputMessage = new StringBuffer();
			final StringBuffer errorMessage = new StringBuffer();
			final StringBuffer outFileName = new StringBuffer();
        	
        	SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() 
        	{
        		@Override
        		protected Void doInBackground() throws Exception 
        		{                	
        			File outFile = null;
        			try {
                		String inputFilePath = file.getAbsolutePath();
                		String outputFilePath = steganogramsDir.getAbsolutePath() + System.getProperty("file.separator") + file.getName();
                		outFile = new File(inputFilePath);
                		int i = 0;
                		while (outFile.exists()) 
                		{
                			outFile = new File(outputFilePath.substring(0,
                					outputFilePath.lastIndexOf("."))
                					+ ".f5" + (i != 0 ? "(" + i + ")" : "") + ".jpg");
                			++i;
                		}                   		
                		
                		encryptor.exportTextToSteganogram(unencryptedTextArea.getText().trim(), file, outFile, jpegQuality, imageScale);
                		
            			outputMessage.append(textBundle.getString("ssetextgui.text.Algorithm") + ": " + encryptor.getEncryptAlgorithmComment() + " + F5<br/><br/>");
            			outputMessage.append(textBundle.getString("ssetextgui.text.OutputFile") + ": " + outFile.getName() + "<br/>"); 
            			outputMessage.append("<font size='2'>(" + outFile.getParent() + ")</font>"); 
            			outFileName.append(outFile.getName());

    				} catch (Exception e) {
    					String[] message = e.getMessage().split("::");
    					if(e instanceof ArrayIndexOutOfBoundsException || message[0].equals("1000")) {
    						errorMessage.append(textBundle.getString("ssetextgui.text.InsufficientCapacity"));
    					} 
    					else errorMessage.append(Helpers.getShortenedStackTrace(e, 1));	
    				} catch (Throwable t) {
    					errorMessage.append(Helpers.getShortenedStackTrace(t, 1));	
    				}
            		 
        			return null;
        		}

        		@Override
        		protected void done() 
        		{
        			waitDialog.dispose();
        			if(errorMessage.length() > 0) {
        				setStatusBarText(errorMessage.toString().replaceAll("<br/>", " "));
        				showErrorDialog(errorMessage.toString());  
        			}
        			else {
        				setStatusBarText(textBundle.getString("ssetextgui.label.EncryptButton") + " : " + encryptor.getEncryptAlgorithmComment() + " + F5 : " + outFileName.toString() + " : OK");
        				showOkDialog(outputMessage.toString());
        			}
					
        			if(encryptor != null) {
	        			encryptor.wipeMasterKeys();
					}
        		}
        	};
			
        	sw.execute();
        	waitDialog.showDialog();
		}
    }
    
    /** Set Status Bar Text */
    private synchronized void setStatusBarText(String text)
    {
    	statusBar.setText(Helpers.getFormatedTime(System.currentTimeMillis(), Locale.getDefault()) + " | " + text.replaceAll("\\<[^>]*>",""));
    }
    
    /** Text Areas Change Listener */
    private DocumentListener getTextAreasOnChangeListener()
    {
    	return new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
            	handleEvent();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
            	handleEvent();
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
            	handleEvent();
            }
            
            private void handleEvent() {
            	if(changeResolver.checkChange(unencryptedTextArea.getText(), encryptedTextArea.getText()))
            		encryptedTextArea.setForeground(Color.red);
            	else encryptedTextArea.setForeground(Color.black);
            	
            	charCounter.setText(unencryptedTextArea.getText().length() + " | " + encryptedTextArea.getText().length());
            }
    	};
    }
    
    /** Password Field Change Listener */
    private DocumentListener getPasswordOnChangeListener()
    {
    	return new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
            	handleEvent();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
            	handleEvent();
            }

            @Override
            public void changedUpdate(DocumentEvent arg0) {
            	handleEvent();
            }
            
            private void handleEvent() {
            	char[] tempPw = passwordTF.getPassword();
            	if(longPasswordWarning && tempPw.length > 1024) {
            		longPasswordWarning = false;
            		showAlert(coreBundle.getString("ssecore.text.longPasswordWarning"));
            	}
            	if(showHidePasswordCB.isSelected()) {
        			passwordConfirmTF.setText(Helpers.createStringWithLength(tempPw.length, '.'));
            	}
            	Arrays.fill(tempPw, '\u0000');
            }
    	};
    }
    
    /** Show Alert Dialog */
    private void showAlert(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSETextGUI.this, "", MessageDialog.ICON_INFO_RED);
    	messageDialog.setMinimumWidth(250);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Show Alert Dialog */
    private void showAlertNews(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSETextGUI.this, "", MessageDialog.ICON_INFO_BLUE);
    	messageDialog.setMinimumWidth(250);
    	messageDialog.setPreferredSize(new Dimension(700, 180));
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Show OK Dialog */
    private void showOkDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSETextGUI.this, "", MessageDialog.ICON_OK);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Show Error Dialog */
    private void showErrorDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSETextGUI.this, "", MessageDialog.ICON_NEGATIVE);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Show Confirm Dialog */
    private int showConfirmSaveDialog(String message, String title)
    {
	    int result = JOptionPane.showConfirmDialog(
	    	    this,
	    	    message,
	    	    title,
	    	    JOptionPane.YES_NO_OPTION);
	    return result;
    }
    
    /** Blink Red */
    private void blinkComponent(final JComponent component)
    {
    	if(blinkBlock) return;
    	blinkBlock = true;
    	final Color originalColor = component.getBackground();
    	
    	Timer blinkTimer = new Timer(200, new ActionListener() {
    	    private int count = 0;
    	    private int maxCount = 4;
    	    private boolean on = false;

    	    public void actionPerformed(ActionEvent e) {
    	        if (count >= maxCount) {
    	        	component.setBackground(originalColor);
    	            ((Timer) e.getSource()).stop();
    	            blinkBlock = false;
    	        } else {
    	        	component.setBackground(on ? Color.RED : originalColor);
    	            on = !on;
    	            count++;
    	        }
    	    }
    	});
    	blinkTimer.start();
    }
    
    /** Copy/Cut/Paste/... Popup Menu */
    private void installContextMenu(final JTextComponent component) {  
    	component.addMouseListener(new MouseAdapter() {  
            public void mouseReleased(final MouseEvent e) {  
                if (e.isPopupTrigger()) {  
                	installContextMenuImplementation(component, e);
                }  
            }  
        }); 
    	component.addMouseListener(new MouseAdapter() {  
            public void mousePressed(final MouseEvent e) {  
                if (e.isPopupTrigger()) {  
                	installContextMenuImplementation(component, e);
                }  
            }  
        }); 
    }
    
    private void installContextMenuImplementation(final JTextComponent component, final MouseEvent e) 
    { 
        final JPopupMenu menu = new JPopupMenu();
        component.requestFocusInWindow();
        JMenuItem item;  
        
        item = new JMenuItem(new DefaultEditorKit.CopyAction());  
        item.setText(textBundle.getString("ssetextgui.text.Copy"));  
        item.setEnabled(component.getSelectionStart() != component.getSelectionEnd());  
        menu.add(item);  
        
        item = new JMenuItem(new DefaultEditorKit.CutAction());  
        item.setText(textBundle.getString("ssetextgui.text.Cut"));  
        item.setEnabled(component.isEditable() && component.getSelectionStart() != component.getSelectionEnd());  
        menu.add(item);  
        
        item = new JMenuItem(new DefaultEditorKit.PasteAction());  
        item.setText(textBundle.getString("ssetextgui.text.Paste"));
		String result = ".";
		try {result = (String)Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);}catch (Exception e1){} 
        item.setEnabled(component.isEditable() && !result.trim().equals(""));  
        menu.add(item);  
        
        item = new JMenuItem();  
        item.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		component.selectAll();
		    }
        });
        item.setText(textBundle.getString("ssetextgui.text.SelectAll"));  
        item.setEnabled(component.getText().trim().length() > 0);  
        menu.add(item);
        
        if(component.getName().equals(ENCRYPTED_TEXTAREA_IDENTIFIER))
        {
	        item = new JMenuItem();  
	        item.addActionListener(new ActionListener() 
	        {
	        	public void actionPerformed(ActionEvent evt) 
	        	{
	        		showLoadDialog(component);
			    }
	        });
	        item.setText(textBundle.getString("ssetextgui.text.Load"));    
	        menu.add(item);
	        
	        item = new JMenuItem();  
	        item.addActionListener(new ActionListener() 
	        {
	        	public void actionPerformed(ActionEvent evt) 
	        	{
	        		showSaveDialog(component);
			    }
	        });
	        item.setText(textBundle.getString("ssetextgui.text.Save"));
	        item.setEnabled(component.getText().trim().length() > 0);  
	        menu.add(item);
        }
        
        menu.show(e.getComponent(), e.getX(), e.getY()); 
    }
    
    /** Show Load Text from File Dialog */
    private void showLoadDialog(JTextComponent component)
    {   	
    	File selectedFile = Helpers.showOpenFileDialog(this, saveLoadDir, "*.txt", "txt");
    	if(selectedFile != null) 
    	{
    		String text = null;
    		try {
    			text = Helpers.loadStringFromFile(selectedFile).trim();
    			setStatusBarText(textBundle.getString("ssetextgui.text.Load") + ": " + selectedFile.getName() + " : " + "OK");
    		} catch (IOException e) {
    			showAlert(e.getLocalizedMessage());
    			e.printStackTrace();
    		}
    		if(text != null) component.setText(text);
    	}
    }
    

    /** Show Save Text to File Dialog */
    private void showSaveDialog(JTextComponent component)
    {
    	String fileExtension = "txt";
       	File selectedFile = Helpers.showSaveFileDialog(this, saveLoadDir, "*.txt", fileExtension);
    	if (selectedFile != null)  
    	{
    		if(!selectedFile.getName().endsWith("." + fileExtension)) selectedFile = new File(selectedFile.getAbsolutePath() + "." + fileExtension);
    		int result = selectedFile.exists() ? 
    			  showConfirmSaveDialog(textBundle.getString("ssetextgui.text.FileExistsReplace")
    					  .replaceAll("<0>", selectedFile.getName()), textBundle.getString("ssetextgui.text.ReplaceFile")) : -1;
    		if(result == JOptionPane.NO_OPTION) return;
    		try {
    			Helpers.saveStringToFile(selectedFile, component.getText().trim());
    		} catch (IOException e) {
    			showAlert(e.getLocalizedMessage());
    			e.printStackTrace();
    		}
    	}
    }
    
    /** Drop Listener for text areas */
    public SimpleDragDropListener getStringLoadListener(final JTextArea textArea)
    {
        SimpleDragDropListener dropListener = new SimpleDragDropListener()
        {
		    @Override
		    public void drop(DropTargetDropEvent event) 
		    {
		        event.acceptDrop(DnDConstants.ACTION_COPY);
		        Transferable transferable = event.getTransferable();

		        List<File> files = null;
		        String droppedText = null;
		        try {
		        	files = Helpers.getFileListFromDrop(transferable);
		        } catch (Exception e) {
		        	e.printStackTrace();
		        }
		        
		        if(files == null || files.isEmpty())
		        	droppedText = Helpers.getStringFromDrop(transferable);

		        event.dropComplete(true); 	        
		        
		        if(files != null && files.size() > 0)
		        {
		        	File firstFile = files.get(0);
		        	if(firstFile.exists() && firstFile.isFile())
		        	{
		        		if(firstFile.length() > 1048576) {
		        			showErrorDialog(textBundle.getString("ssetextgui.text.DaDFileTooLarge"));
		        			return;
		        		}
		        		try {
							String loadedText = Helpers.loadStringFromFile(firstFile);
							setStatusBarText(textBundle.getString("ssetextgui.text.Load") + ": " + firstFile.getName() + " : " + "OK");
							Helpers.showReplaceTextWarningDialog(SSETextGUI.this, textArea, textBundle.getString("ssetextgui.text.ReplaceTextWarning"), loadedText);
						} catch (IOException e) {
							// N/A
						}
		        	}
		        } 
		        else if(droppedText != null && !droppedText.trim().equals("")) 
		        	textArea.append(droppedText.trim());
		    }
        };
        
        return dropListener;
    }
}