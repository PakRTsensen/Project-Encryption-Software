package com.paranoiaworks.sse.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.paranoiaworks.sse.CryptFile;
import com.paranoiaworks.sse.Encryptor;
import com.paranoiaworks.sse.Helpers;
import com.paranoiaworks.sse.MessageHandler;
import com.paranoiaworks.sse.StaticConfig;
import com.paranoiaworks.sse.checklic.Parcl;
import com.paranoiaworks.sse.components.ComboBoxDisableable;
import com.paranoiaworks.sse.components.MessageDialog;
import com.paranoiaworks.sse.components.OkCancelDialog;
import com.paranoiaworks.sse.components.PlaceholderPasswordField;
import com.paranoiaworks.sse.components.SimpleDragDropListener;
import com.paranoiaworks.sse.components.SimplePasswordDialog;
import com.paranoiaworks.sse.dao.SettingsDataHolder;

/**
 * S.S.E. for PC - Drag and Drop GUI Main Class
 * 
 * @author Paranoia Works
 * @version 1.2.6
 */ 

public class SSEFileGUI extends JFrame {

    private static final long serialVersionUID = 1L;    
    private static final String appVersionCode = "15.0.6";
    private static final String publishYear = "2023";
    
    protected static final String FE_SETTINGS_ALGORITHM = "FE_SETTINGS_ALGORITHM";
    protected static final String FE_SETTINGS_OUTPUTDIR_ENC_ENABLED = "FE_SETTINGS_OUTPUTDIR_ENC_ENABLED";
    protected static final String FE_SETTINGS_OUTPUTDIR_DEC_ENABLED = "FE_SETTINGS_OUTPUTDIR_DEC_ENABLED";
    protected static final String FE_SETTINGS_CUSTOMCOMMAND_ENABLED = "FE_SETTINGS_CUSTOMCOMMAND_ENABLED";
    protected static final String FE_SETTINGS_COMPRESSION = "FE_SETTINGS_COMPRESSION";
    protected static final String FE_SETTINGS_OUTPUTDIR_ENC_PATH = "FE_SETTINGS_OUTPUTDIR_ENC_PATH";
    protected static final String FE_SETTINGS_OUTPUTDIR_DEC_PATH = "FE_SETTINGS_OUTPUTDIR_DEC_PATH";
    protected static final String FE_SETTINGS_REPLACE_WARNING = "FE_SETTINGS_REPLACE_WARNING";
    protected static final String FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC = "FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC";
    protected static final String FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC = "FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC";
    
    private static final boolean IS_MAC = Helpers.isMac();
    private static final int SQUARE_BUTTON_SIZE = IS_MAC ? 40 : 34;
    
    private JComboBox algorithmCB;
    private PlaceholderPasswordField passwordTF;
    private PlaceholderPasswordField passwordConfirmTF;
    private JButton stopButton;
    private JProgressBar filesPB;
    private JProgressBar currentFilePB;
    private JDialog helpDialog;
    private final JCheckBox showHidePasswordCB;
    
    private Thread workerThread;
    private MessageHandler mh;
    
    private String[] cmdLineArgs;
    private boolean proVersion = false;
    private boolean longPasswordWarning = true;
    private Boolean compress = null;
    private String inputFilePath = null;		
    private char[] password = null;
    private int algorithmCode = -1;
    private int errors = -1;
    private CryptFile inputFile = null;	
	
    private List<File> files;
    private boolean encDecLock = false;
	
    private static final SettingsDataHolder sdh;
    private static ResourceBundle textBundle;
	private static ResourceBundle coreBundle;
	private static Locale locale;
	
	static {
		sdh = SettingsDataHolder.getSettingsDataHolder(StaticConfig.SSEFE_CONFIG_FILE); 
		String interfaceLanguage = sdh.getPersistentDataString(StaticConfig.FE_SETTINGS_GUI_LANGUAGE, null);
		locale = Helpers.getLocale(interfaceLanguage);
		textBundle = ResourceBundle.getBundle("res.SSEFileGUI", locale);
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
    	new SSEFileGUI(args);
    }

    /** Prepare and Render Main Window */
    public SSEFileGUI(String[] args) {
        super(StaticConfig.PFTE_BUNDLE ? textBundle.getString("ssefilegui.label.MainLabelPFE") : textBundle.getString("ssefilegui.label.MainLabel"));
        cmdLineArgs = args;
        int windowWidth = Integer.parseInt(textBundle.getString("ssefilegui.spec.mainWindowWidth"));
		int windowHeight = Integer.parseInt(textBundle.getString("ssefilegui.spec.mainWindowHeight"));
		int textSizeIncDec = Integer.parseInt(textBundle.getString("ssefilegui.spec.textPaneFontIncDec"));
        
        this.setMinimumSize(new Dimension(windowWidth, windowHeight));

        String[] algorithmList = {"AES (256 bit)", "RC6 (256 bit)", "Serpent (256 bit)", "Twofish (256 bit)", "GOST28147 (256 bit)", "Blowfish (448 bit)", "Threefish (1024 bit)", "SHACAL-2 (512 bit)", "Paranoia C4 (2048 bit)"};      
        
        proVersion = Parcl.loadcheck(sdh);
        if(!proVersion) {
        	SettingsDataHolder alternativeSDH = SettingsDataHolder.getSettingsDataHolder(StaticConfig.PTE_CONFIG_FILE, true);
        	proVersion = Parcl.loadcheckAlternative(sdh, alternativeSDH);
        }       
        
        int[] selectableAlgInterval = {0, 5};
        if(proVersion) {
        	this.setTitle(this.getTitle() + " PRO");
        	selectableAlgInterval = null;
        } 
        
        //+ Top Pane
        final JPanel topPane = new JPanel(new BorderLayout());
        JPanel topPaneLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPaneLeft.setBorder(new EmptyBorder(2, 0, 8, 0));
        JPanel bottomPane = new JPanel(new BorderLayout());
        bottomPane.setBorder(new EmptyBorder(5, 0, 5, 2));
        
        JPanel passwordPane = new JPanel(new BorderLayout());
        JPanel passwordLabelPane = new JPanel(new BorderLayout());
        JLabel passworLabel = new JLabel(textBundle.getString("ssefilegui.label.PasswordLabel"), SwingConstants.LEFT);
        if(IS_MAC) passworLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        showHidePasswordCB = new JCheckBox(textBundle.getString("ssefilegui.label.ShowHidePasswordCB"));
        passwordTF = new PlaceholderPasswordField();
        Helpers.setUnicodeFontInWindows(passwordTF);
        passwordTF.enableInputMethods(true);
        passwordTF.setPlaceholder(" " + textBundle.getString("ssefilegui.label.PasswordPH"));
        final char echoChar = passwordTF.getEchoChar();
        passwordTF.setPreferredSize(new Dimension(250, 24));
        passwordTF.getDocument().addDocumentListener(getPasswordOnChangeListener());
        passwordConfirmTF = new PlaceholderPasswordField();
        Helpers.setUnicodeFontInWindows(passwordConfirmTF);
        passwordConfirmTF.enableInputMethods(true);
        passwordConfirmTF.setPlaceholder(" " + textBundle.getString("ssefilegui.label.PasswordConfirmPH"));
        passwordConfirmTF.setPreferredSize(new Dimension(250, 24));
        passwordLabelPane.add(passworLabel, BorderLayout.LINE_START);
        passwordLabelPane.add(showHidePasswordCB, BorderLayout.LINE_END);
        passwordPane.add(passwordLabelPane, BorderLayout.PAGE_START);
        passwordPane.add(passwordTF, BorderLayout.CENTER);
        passwordPane.add(passwordConfirmTF, BorderLayout.PAGE_END);
        
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) 
            {
            	if(encDecLock) {
                	mh.interrupt();
                }
            	else {
	            	try {
	            		sdh.addOrReplacePersistentDataObject(FE_SETTINGS_ALGORITHM, algorithmCB.getSelectedIndex());
						sdh.save();
					} catch (Exception e) {
						e.printStackTrace();
					}
	            	if(password != null) Arrays.fill(password, '\u0000'); 
	            	System.exit(0);
            	}
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
        JLabel algorithmLabel = new JLabel(textBundle.getString("ssefilegui.label.AlgorithmLabel"), SwingConstants.LEFT);
        if(IS_MAC) 
        	algorithmLabel.setBorder(new EmptyBorder(3, 6, 4, 0));
        else 
        	algorithmLabel.setBorder(new EmptyBorder(3, 0, 4, 0));
        algorithmCB = new ComboBoxDisableable(SSEFileGUI.this, algorithmList, selectableAlgInterval, coreBundle.getString("ssecore.pro.proVersionOnlyAlgorithm"));
        algorithmCB.setEditable(false);
        algorithmPane.add(algorithmLabel, BorderLayout.PAGE_START);
        algorithmPane.add(algorithmCB, BorderLayout.CENTER);    
        
        Integer algorithmCBSetting = sdh.getPersistentDataInteger(FE_SETTINGS_ALGORITHM);
        if(algorithmCBSetting != null && algorithmCBSetting + 1 <= algorithmList.length)        
        	algorithmCB.setSelectedIndex(algorithmCBSetting);
        
        JPanel topPaneRight = new JPanel(new BorderLayout());
        topPaneRight.setBorder(new EmptyBorder(5, 5, 55, 5));
        JButton helpButton = new JButton();
        helpButton.setToolTipText(textBundle.getString("ssefilegui.label.HelpDialog"));
        helpButton.setPreferredSize(new Dimension(SQUARE_BUTTON_SIZE, SQUARE_BUTTON_SIZE));
        JButton settingsButton = new JButton();
        settingsButton.setToolTipText(textBundle.getString("ssefilegui.settings.settingsTitle"));
        settingsButton.setPreferredSize(new Dimension(SQUARE_BUTTON_SIZE, SQUARE_BUTTON_SIZE));
        topPaneRight.add(settingsButton, BorderLayout.WEST);
        topPaneRight.add(new JLabel(" "), BorderLayout.CENTER);
        topPaneRight.add(helpButton, BorderLayout.EAST);
        
        try {
			Image imgHelp = Helpers.loadImageFromResource(SSEFileGUI.class, "icon_help", "png");
			Image imgSettings = Helpers.loadImageFromResource(SSEFileGUI.class, "icon_settings", "png");
			
			helpButton.setIcon(new ImageIcon(imgHelp));
			settingsButton.setIcon(new ImageIcon(imgSettings));
			
			List<Image> appIcons = new ArrayList<Image>();
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_16.png")));
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_32.png")));	
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_64.png")));	
			appIcons.add(ImageIO.read(getClass().getResource("/res/icon_128.png")));	
			this.setIconImages(appIcons);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
        
        helpButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		ResourceBundle[] bundles = {textBundle, coreBundle};
        		final HelpDialogFE helpDialog = new HelpDialogFE(SSEFileGUI.this, bundles, sdh, 
        				StaticConfig.PFTE_BUNDLE ? StaticConfig.PFTE_VERSION : appVersionCode, publishYear);
        		helpDialog.setShowProUpdate(!proVersion);
        		helpDialog.showDialog();
		    }
        });  
        
        settingsButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		final SettingsDialogFE settingsDialog = new SettingsDialogFE(SSEFileGUI.this, textBundle, coreBundle, sdh);
        		settingsDialog.showDialog();
		    }
        }); 
        
        topPaneLeft.add(passwordPane);
        topPaneLeft.add(algorithmPane);
        topPane.add(topPaneLeft, BorderLayout.LINE_START);
        topPane.add(topPaneRight, BorderLayout.LINE_END);
        //- Top Pane
        

        //+ Bottom Pane
        JPanel progressBarPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filesPB = new JProgressBar();
        currentFilePB = new JProgressBar();
        filesPB.setStringPainted(true);
        currentFilePB.setStringPainted(true);
    
        progressBarPane.add(new JLabel(textBundle.getString("ssefilegui.label.ProgressBarPaneCurrent") + " ", SwingConstants.LEFT));
        progressBarPane.add(currentFilePB);
        progressBarPane.add(new JLabel("     ", SwingConstants.LEFT));
        progressBarPane.add(new JLabel(textBundle.getString("ssefilegui.label.ProgressBarPaneAll") + " ", SwingConstants.LEFT));
        progressBarPane.add(filesPB);
        
        stopButton = new JButton(textBundle.getString("ssefilegui.label.StopButton"));
        stopButton.setPreferredSize(new Dimension(80, 30));
        stopButton.setEnabled(false);
        
        stopButton.addActionListener(new ActionListener() 
        {
        	public void actionPerformed(ActionEvent evt) 
        	{
        		mh.interrupt();
		    }
        });
        
        bottomPane.add(progressBarPane, BorderLayout.LINE_START);
        bottomPane.add(stopButton, BorderLayout.LINE_END);
        //- Bottom Pane
        

        //+ Drop (center) Pane
        JPanel dropPane = new JPanel(new BorderLayout());
        
        JLabel dropLabel = new JLabel(textBundle.getString("ssefilegui.label.DropLabel"), SwingConstants.CENTER);
        int dropLabelFontSize = Integer.parseInt(textBundle.getString("ssefilegui.spec.dropLabelFontSize"));
        dropLabel.setFont(new Font(dropLabel.getFont().getFamily(), Font.BOLD, dropLabelFontSize));
        Border paddingBorder = BorderFactory.createEmptyBorder(2,0,2,0);
        dropLabel.setBorder(paddingBorder);       
        dropLabel.setOpaque(true);
        dropLabel.setForeground(Color.decode("#E0E0E0"));
        dropLabel.setBackground(Color.decode("#707070"));
        
        JPanel mainTextAreaWrapper = new JPanel(new BorderLayout());
        mainTextAreaWrapper.setBorder(BorderFactory.createLineBorder(Color.decode("#FF0000")));
        JTextPane mainTextArea = new JTextPane();
        //Helpers.setUnicodeFontInWindows(mainTextArea);
        mainTextArea.setEditable(false);        
        mainTextArea.setMargin(new Insets(5,5,5,5));
        //if(Helpers.isActiveRightToLeftLang()) mainTextArea.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        if(textSizeIncDec != 0) {
        	  Font f = mainTextArea.getFont();
        	  Font f2 = new Font(f.getFontName(), f.getStyle(), f.getSize() + textSizeIncDec);
        	  mainTextArea.setFont(f2);
        }  
        
        JScrollPane sbrText = new JScrollPane(mainTextArea);
        sbrText.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sbrText.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        mainTextAreaWrapper.add(sbrText, BorderLayout.CENTER);
        dropPane.add(dropLabel, BorderLayout.PAGE_START);
        dropPane.add(mainTextAreaWrapper, BorderLayout.CENTER);
             
        mh = new MessageHandler(mainTextArea);
        mh.addProgressBar(currentFilePB);
        
        // Drag and Drop listener implementation 
        SimpleDragDropListener ddListener = new SimpleDragDropListener()
        {
		    @Override
		    public void drop(DropTargetDropEvent event) 
		    {
		        if(encDecLock) return;
		        lockApp();
		        
				password = passwordTF.getPassword();
				password = Helpers.trim(password);
				
				char[] passwordConfirm = passwordConfirmTF.getPassword();
				passwordConfirm = Helpers.trim(passwordConfirm);
				
				algorithmCode = Encryptor.positionToAlgCode(algorithmCB.getSelectedIndex());
				
				int filesToEncrypt;
				int filesToDecrypt;
				try {
					if(password.length < 1)
					{
						mh.print(textBundle.getString("ssefilegui.text.EnterPassword") + "\n\n", Color.RED, true);
						throw new IllegalArgumentException();
					}
					
					event.acceptDrop(DnDConstants.ACTION_COPY);
					Transferable transferable = event.getTransferable();

					files = null;
					try {
						files = Helpers.getFileListFromDrop(transferable);
					} catch (Exception e) {
						e.printStackTrace();
					}

					event.dropComplete(true);    
					if (files == null || files.size() == 0) {
						mh.print(textBundle.getString("ssefilegui.text.IncorrectFile") + "\n\n", Color.RED, true);
						throw new IllegalArgumentException();
					}
					
					boolean checkPasswordConfirmation = false;
					
					filesToEncrypt = 0;
					filesToDecrypt = 0;
					
					for(int i = 0; i < files.size(); ++i) 
					{
						CryptFile tempFile = new CryptFile(files.get(i));
						if(tempFile.isFile() && tempFile.isEncrypted())
							++filesToDecrypt;
						else 
							++filesToEncrypt;
					}
					
					if(filesToEncrypt > 0) checkPasswordConfirmation = true;
					
					if(!Arrays.equals(password, passwordConfirm) && !showHidePasswordCB.isSelected() && checkPasswordConfirmation)
					{
						mh.print(textBundle.getString("ssefilegui.text.PasswordsDontMatch") + "\n\n", Color.RED, true);
						throw new IllegalArgumentException();
					}
				} catch (IllegalArgumentException e) {
					Arrays.fill(passwordConfirm, '\u0000');
					unlockApp();
					return;
				}
				
				Arrays.fill(passwordConfirm, '\u0000');
				
				Collections.sort(files);
				checkReplaceFilesContEncDec(filesToEncrypt, filesToDecrypt, true);
		    }
        };
        
        new DropTarget(mainTextArea, ddListener); // Connect the mainTextArea with a drag and drop listener
        //- Drop (center) Pane

        // Add components to the content
        this.getContentPane().add(BorderLayout.NORTH, topPane);
        this.getContentPane().add(BorderLayout.CENTER, dropPane);
        this.getContentPane().add(BorderLayout.PAGE_END, bottomPane);  
        
        // Center
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - this.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
        this.setLocation(x, y);

        this.setVisible(true);
        
        boolean showPasswordDialog = false;
        Object[] argsParsed = null;
        if(cmdLineArgs != null && cmdLineArgs.length > 0)
		{
        	try {
				argsParsed = Helpers.getNumberOfEncAndUnenc(cmdLineArgs);
			} catch (Exception e) {
				// N/A
			}	
		}
        
        final Object[] argsParsedFinal = argsParsed;
        if(argsParsedFinal != null) 
        {
        	showPasswordDialog = true;
	        
        	EventQueue.invokeLater(new Runnable() {
	        	public void run() 
	        	{
	        		try {			
	    	        	List<CryptFile> inputFiles = (List<CryptFile>) argsParsedFinal[0];
	    				int[] encdec = (int[])argsParsedFinal[1];
	    	
	    				String dialogLabel = null;
	    				if(inputFiles.size() > 1)
	    					dialogLabel = encdec[1] + "/" + encdec[0] + " (" + textBundle.getString("ssefilegui.text.Encryption") + "/" + textBundle.getString("ssefilegui.text.Decryption") + ")";
	    				else
	    					dialogLabel	= inputFiles.get(0).getName();
	    				SimplePasswordDialog spd = new SimplePasswordDialog(SSEFileGUI.this, dialogLabel, encdec[1] > 0 ? SimplePasswordDialog.MODE_SET : SimplePasswordDialog.MODE_ENTER, locale);
	    				password = spd.showDialog();
	    					
	    				if(password != null) {
	    					lockApp();
	    					Helpers.setTextToPasswordField(passwordTF, password);
	    					Helpers.setTextToPasswordField(passwordConfirmTF, password);
	    					files = new ArrayList<File>();
	    					files.addAll(inputFiles);
	    					algorithmCode = Encryptor.positionToAlgCode(algorithmCB.getSelectedIndex());
	    					Collections.sort(files);
	    					checkReplaceFilesContEncDec(null, null, false);
	    				}
	    					
	    			} catch (Exception e) {
	    				// swallow
	    			}
	        	}
	        });
        }
        
		if(!showPasswordDialog)
		{
	        EventQueue.invokeLater(new Runnable(){
	        	public void run() 
	        	{
	        		passwordTF.grabFocus();
	        		passwordTF.requestFocus();
	        		if(sdh.getErrorMessage() != null)
	        			showErrorDialog(sdh.getErrorMessage());  
	        	}
	        });
		}
    }
    
    /** Check existing encrypted files and continue Enc/Dec execution */
    public void checkReplaceFilesContEncDec(final Integer filesToEncrypt, final Integer filesToDecrypt, final boolean fromDrop) 
    {
    	File outputDir = null;	
    	String existingEncFiles = null;
    	
    	if(sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_REPLACE_WARNING, true))
    	{
	    	outputDir = fromDrop ? getOutputFolder(true) : null;   	
	    	existingEncFiles = Helpers.getReplaceEncFilesWarning(outputDir, files);
    	}
    	
    	if(existingEncFiles != null)
    	{
    		existingEncFiles = "<br/>" + textBundle.getString("ssefilegui.warning.replaceFiles") + "<br/><font color='blue'>" + Helpers.htmlWrapToCode(existingEncFiles) 
    				+ "</font><br/>" + textBundle.getString("ssefilegui.text.QuestionContinue");
    		
    		final OkCancelDialog replaceWarningDialog = new OkCancelDialog(SSEFileGUI.this);
			
			ActionListener replaceWarningDialogListenerOk = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					replaceWarningDialog.dispose();
					if(fromDrop)
						checkCustomCommandContEncDec(filesToEncrypt, filesToDecrypt, fromDrop);
					else
						encDecExecute(fromDrop);
				}
			};
			ActionListener replaceWarningDialogListenerCancel = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					unlockApp();
					replaceWarningDialog.dispose();
					
				}
			};
			replaceWarningDialog.setListeners(replaceWarningDialogListenerOk, replaceWarningDialogListenerCancel);
			replaceWarningDialog.setTitle(textBundle.getString("ssefilegui.text.replaceFile"));
			replaceWarningDialog.setText(existingEncFiles);
			replaceWarningDialog.showDialog();
    	}
    	else {
			if(fromDrop)
				checkCustomCommandContEncDec(filesToEncrypt, filesToDecrypt, fromDrop);
			else
				encDecExecute(fromDrop);
    	}
    }
    
    /** Check custom command and continue Enc/Dec execution */
    public void checkCustomCommandContEncDec(final int filesToEncrypt, final int filesToDecrypt, final boolean fromDrop) 
    {
    	boolean cceEnabled = sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED, false);
		String ccae = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC, "").trim();
		String ccad = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC, "").trim();
		String ccWarning = "";
		
		if(cceEnabled && !ccae.isEmpty() && filesToEncrypt > 0) {
			ccWarning += textBundle.getString("ssefilegui.settings.customCommandAfterEnc") + "<br/>";
			ccWarning += "<font color='blue'><code>" + Helpers.escapeHTML(ccae) + "</code></font>";
		}
		if(cceEnabled && !ccad.isEmpty() && filesToDecrypt > 0) {
			if(!ccWarning.isEmpty()) ccWarning += "<br/><br/>";
			ccWarning += textBundle.getString("ssefilegui.settings.customCommandAfterDec") + "<br/>";
			ccWarning += "<font color='blue'><code>" + Helpers.escapeHTML(ccad) + "</code></font>";
		}
		
		if(!ccWarning.isEmpty()) {
			ccWarning = "<br/>" + coreBundle.getString("ssecore.text.Warning") + ":<br/>" + textBundle.getString("ssefilegui.warning.CustomCommand") + "<br/><br/>" 
							+ ccWarning + "<br/><br/>" + textBundle.getString("ssefilegui.text.QuestionContinue");
    		
    		final OkCancelDialog ccWarningDialog = new OkCancelDialog(SSEFileGUI.this);
    		ActionListener ccWarningDialogListenerOk = new ActionListener()
    		{
    			public void actionPerformed(ActionEvent e)
    			{
    				encDecExecute(fromDrop);
    				ccWarningDialog.dispose();
    			}
    		};
    		ActionListener ccWarningDialogListenerCancel = new ActionListener()
    		{
    			public void actionPerformed(ActionEvent e)
    			{
    				unlockApp();
    				ccWarningDialog.dispose();
    				
    			}
    		};
    		ccWarningDialog.setListeners(ccWarningDialogListenerOk, ccWarningDialogListenerCancel);
    		ccWarningDialog.setTitle(Helpers.capitalizeAllFirstLetters(textBundle.getString("ssefilegui.settings.customCommand")));
    		ccWarningDialog.setText(ccWarning);
    		ccWarningDialog.showDialog();
		}
		else encDecExecute(fromDrop);  
    }
    
    /** Enc/Dec method */
    public synchronized void encDecExecute(final boolean fromDrop)
    {
    	int compressionCode = sdh.getPersistentDataInteger(SSEFileGUI.FE_SETTINGS_COMPRESSION, 2);
    	if(compressionCode == 0) compress = false;
    	else if(compressionCode == 1) compress = true;
    	else if(compressionCode == 2) compress = null;
    	
    	// Start enc/dec process
        workerThread = new Thread (new Runnable() 
		{
			public void run() 
			{	
				filesPB.setMaximum(files.size());
				filesPB.setValue(0);
				filesPB.setString(0 + "/" + files.size());
				
				if(!fromDrop && isDragDropExclusiveFeatureEnabled()) 
					mh.println(textBundle.getString("ssefilegui.warning.dragAndDropExclusive") + "\n\n", Color.RED, true);
				
				for (int i = 0; i < files.size(); ++i) 
		        {	
					inputFilePath = files.get(i).getPath();						
					inputFile = new CryptFile(inputFilePath);
					
					if(!mh.isBlank()) {
						if(i > 0) mh.print(textBundle.getString("ssefilegui.spec.horDelimiter") + "\n\n", Color.LIGHT_GRAY, false);
						else mh.print(textBundle.getString("ssefilegui.spec.horDelimiterFirst") + "\n\n");
					}
					
					mh.print(textBundle.getString("ssefilegui.text.Processing") + " (" + (i + 1) + "/" + files.size() + "): ", null, true);
					mh.print(inputFile.getName(), Color.decode("#0000AA"), true, true);
					if(inputFile.isFile() && inputFile.length() < 1) mh.println(textBundle.getString("ssefilegui.warning.FileSize") + " " + inputFile.length(), Color.RED, true);
					mh.println("");
					
					errors = -1;
					List<Object> logs = new ArrayList<Object>();
					Encryptor encryptor = null;
					try {
						encryptor = new Encryptor(password, algorithmCode, Encryptor.PURPOSE_FILE_ENCRYPTION, true);
						
						if(!inputFile.isEncrypted()) {
							if(Helpers.isSubDirectory(inputFile, getOutputFolder(true)))
								throw new InvalidParameterException(textBundle.getString("ssefilegui.warning.outputSubdirOfInput"));
							
							errors = (int)encryptor.zipAndEncryptFile(inputFile, compress, mh, fromDrop ? getOutputFolder(true) : null, logs);
						}
						else
							errors = (int)encryptor.unzipAndDecryptFile(new CryptFile(inputFilePath), mh, fromDrop ? getOutputFolder(false) : null, logs);
						
    					filesPB.setValue(i + 1);
    					filesPB.setString((i + 1) + "/" + files.size());
						
					} catch (InvalidParameterException e) {
						mh.print(e.getMessage() + "\n\n", Color.RED, true);
						break;
					}  catch (Exception e) {
						String message = "";
						if(!inputFile.isEncrypted())
						{
							System.gc(); try{Thread.sleep(200);}catch(InterruptedException e1){}; // Because of a bug in Java						
							message = e.getMessage();
							CryptFile tf = new CryptFile(inputFilePath + "." + Encryptor.ENC_FILE_EXTENSION + "." + Encryptor.ENC_FILE_UNFINISHED_EXTENSION);
							if(tf.exists()) tf.delete(); 
						}
						else
						{
							System.gc(); try{Thread.sleep(200);}catch(InterruptedException e1){}; // Because of a bug in Java
							message = e.getMessage();
							
							if(message == null) {
								StringWriter sw = new StringWriter();
		        	    		e.printStackTrace(new PrintWriter(sw));
		        	    		message = sw.toString();
							}
							
							String delimiter = "||";
							String[] messages = message.split(Pattern.quote(delimiter)); // try to split "message||delete file path"
							
							if(messages.length > 1)
							{
								int delimiterIndex = message.indexOf(delimiter) + delimiter.length();
								if(message.length() > delimiterIndex)
								{
									String deleteFilePath = message.substring(delimiterIndex);
									CryptFile tf = new CryptFile(deleteFilePath);
									if(tf.exists()) tf.delete();
								}
							}
							if(!(messages == null || messages.length < 1 || messages[0] == null || messages[0].trim().equals(""))) message = messages[0];
						}
						
						if(message.length() < 8) {
	        	    		StringWriter sw = new StringWriter();
	        	    		e.printStackTrace(new PrintWriter(sw));
	        	    		message = sw.toString();
						}
						
        	    		mh.println(message + "\n\n", Color.RED, true);
        	    		break;
					}
					finally {
						if(encryptor != null) {
		        			encryptor.wipeMasterKeys();
		        			encryptor = null;
						}
					}
					
					if(errors > -1)
					{
						if(errors == 0) mh.println("\n" + textBundle.getString("ssefilegui.text.Completed") + ": OK\n\n", null, true);
						else mh.println("\n" + textBundle.getString("ssefilegui.text.Completed")+ ": " + errors + " " + textBundle.getString("ssefilegui.text.Errors").toLowerCase() + "\n\n", Color.RED, true);
					}
					
					// Custom command execution
					String customCommand = null;
					if(errors == 0 && fromDrop && sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED, false)) 
					{
						try {
							if(!inputFile.isEncrypted()) {
								customCommand = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_ENC, "").trim();
								if(customCommand != null) {
									customCommand = Helpers.replaceAndWrapPath(customCommand, "<original-path>", inputFile.getAbsolutePath());
									customCommand = Helpers.replaceAndWrapPath(customCommand, "<encrypted-path>", logs.get(0).toString());
								}
							}
							else {
								customCommand = sdh.getPersistentDataString(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_AFTER_DEC, "").trim();
								if(customCommand != null) {
									customCommand = Helpers.replaceAndWrapPath(customCommand, "<original-path>", inputFile.getAbsolutePath());
									customCommand = Helpers.replaceAndWrapPath(customCommand, "<decrypted-path>", logs.get(0).toString());
								}
							}
						} catch (Exception e) {
							// Swallow
						}
					}
					
					if(customCommand != null && !customCommand.trim().isEmpty()) 
					{
						mh.print(textBundle.getString("ssefilegui.settings.customCommand") + ":\n", Color.BLACK, true);
						mh.print(customCommand + "\n", new Color(80, 0, 150), true);
						
						String[] origCmdParams = Helpers.parseCommand(customCommand);

						int attemptCounter = 0;
						while(true) 
						{
							try {
								++attemptCounter;
								String[] cmdParams = null;
								if(attemptCounter == 1) {
									cmdParams = Helpers.getArrayCopy(origCmdParams);
									cmdParams[0] = Helpers.getCurrentAppDirPath() + File.separator + cmdParams[0];
								}
								else if (attemptCounter == 2) {									
									cmdParams = Helpers.getArrayCopy(origCmdParams);
								}
								else {
									mh.print("\n");
									String[] cmdPrefix = {"cmd", "/c"};
									cmdParams = Helpers.concatAll(cmdPrefix, origCmdParams);
								}
								
								ProcessBuilder pb = new ProcessBuilder(cmdParams);
								pb.redirectErrorStream(true);
								Process p = pb.start();
								InputStream pis = p.getInputStream();
								int inInt = -1;
								boolean cr = false;
								while ((inInt = pis.read()) != -1) 
								{
								    if(inInt == 8) mh.removeLastChars(1);
								    else if(inInt == 13) cr = true;
								    else {
								    	if(cr && inInt != 10) mh.removeLastLine();
								    	mh.print(Character.toString((char)inInt), new Color(80, 0, 150), false);
								    	cr = false;
								    }
							    	
								    if (mh.interrupted()){
							    		mh.interruptReset();
							    		p.destroy();
							    		throw new InterruptedException(coreBundle.getString("ssecore.text.CanceledByUser"));
							    	}
	
								}
								mh.print("\n");
								break;
								
							} catch (IOException e) {
								if (attemptCounter < 2 || (attemptCounter < 3 && Helpers.isWindows())) continue;
								else {
									mh.print("\n" + e.getLocalizedMessage() + "\n\n", Color.RED, true);
									break;
								}
							} catch (Exception e) {
								mh.print("\n" + e.getLocalizedMessage() + "\n\n", Color.RED, true);
								break;
							}
						}
						
					}
				}
				
				Helpers.playFinishSound(true);
				
				unlockApp();
			}
		});
		workerThread.setPriority(Thread.MIN_PRIORITY);
		workerThread.start();
    }
    
    /** Prepare layout for enc/dec process */
    private void lockApp()
    {	
    	encDecLock = true;     
	    showHidePasswordCB.setEnabled(false);
	    passwordTF.setEnabled(false);
	    passwordConfirmTF.setEnabled(false);
	    algorithmCB.setEnabled(false);
	    stopButton.setEnabled(true);
    }
    
    /** Return layout back to "Enter Parameters" */
    private void unlockApp()
    {
    	if(password != null) {
    		char[] pwTemp = password;
    		password = null;
    		Arrays.fill(pwTemp, '\u0000');
    	}
    	
    	encDecLock = false;
        showHidePasswordCB.setEnabled(true);
        passwordTF.setEnabled(true);
        if(!showHidePasswordCB.isSelected()) passwordConfirmTF.setEnabled(true);
        algorithmCB.setEnabled(true);
        stopButton.setEnabled(false);
    }
    
    /** Show Alert Dialog */
    private void showAlert(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSEFileGUI.this, "", MessageDialog.ICON_INFO_RED);
    	messageDialog.setMinimumWidth(250);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Show Error Dialog */
    private void showErrorDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(SSEFileGUI.this, "", MessageDialog.ICON_NEGATIVE);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
    
    /** Get Enc/Dec Output Directory */
    private File getOutputFolder(boolean encrypted) 
    { 	
    	String enabledCode = encrypted ? FE_SETTINGS_OUTPUTDIR_ENC_ENABLED : FE_SETTINGS_OUTPUTDIR_DEC_ENABLED;
    	String pathCode = encrypted ? FE_SETTINGS_OUTPUTDIR_ENC_PATH : FE_SETTINGS_OUTPUTDIR_DEC_PATH;
    	
		Boolean enabled = sdh.getPersistentDataBoolean(enabledCode);		
		if(enabled == null || !enabled) return null;
		
		String path = sdh.getPersistentDataString(pathCode);
    	
		File outputDir = null;
    	
    	try {
			outputDir = new File(path);		
			if(!outputDir.exists() || !outputDir.isDirectory() || !Helpers.writeTestFile(outputDir))
				outputDir = null;				
		} catch (Exception e) {
			// swallow
		}
    	
    	if(outputDir == null) 
    	{
    		sdh.addOrReplacePersistentDataObject(enabledCode, false);
    		sdh.addOrReplacePersistentDataObject(pathCode, null);
    		
    		try {
				sdh.save();
			} catch (Exception e) {
				// swallow
			}
    	}
    	
    	return outputDir;
    }
    
    /** Is "Custom Command Execution" or "Custom Output Directory" enabled? */
    private boolean isDragDropExclusiveFeatureEnabled()
    {
    	return (sdh.getPersistentDataBoolean(SSEFileGUI.FE_SETTINGS_CUSTOMCOMMAND_ENABLED, false) 
    				|| sdh.getPersistentDataBoolean(FE_SETTINGS_OUTPUTDIR_ENC_ENABLED, false) 
    					|| sdh.getPersistentDataBoolean(FE_SETTINGS_OUTPUTDIR_DEC_ENABLED, false));
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
}