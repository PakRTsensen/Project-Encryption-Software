package com.paranoiaworks.sse.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.paranoiaworks.sse.Helpers;

/**
 * Simple Password Dialog
 * 
 * @author Paranoia Works
 * @version 1.1.4
 */ 


@SuppressWarnings("serial")
public class SimplePasswordDialog extends JDialog {
    
	public static final int MODE_ENTER = 0;
	public static final int MODE_SET = 1;
	
	private PlaceholderPasswordField passwordTF;
    private PlaceholderPasswordField passwordConfirmTF;
    private JCheckBox showHidePasswordCB;
	
	private Frame parentFrame = null;
	private JButton cancelButton = null;
	private JButton continueButton = null;
	
	private char[] p;
	private ResourceBundle textBundle;
	private int mode;

	
	private static final int MIN_WIDTH = 100;
	private static final int MIN_HEIGHT = 100;
	
	public SimplePasswordDialog(Frame frame, String title, int mode, Locale locale) throws Exception
    {
        super(frame, title, true);
        this.parentFrame = frame;
        this.textBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", locale);
        this.mode = mode;
        init();
    }
	
	public char[] showDialog() 
    {
    	this.pack();
    	this.setLocationRelativeTo(parentFrame);
    	this.setVisible(true);	
    	
    	return p;
    }
	
	Action action = new AbstractAction()
	{
	    @Override
	    public void actionPerformed(ActionEvent e)
	    {
			p = passwordTF.getPassword();
			p = Helpers.trim(p);
			
			char[] p2 = passwordConfirmTF.getPassword();
			p2 = Helpers.trim(p2);
			
			try {
				if(p.length < 1) {
					showErrorDialog(textBundle.getString("ssecore.text.EnterPassword"));
					throw new IllegalArgumentException();
				}				

				if(!Arrays.equals(p, p2) && !showHidePasswordCB.isSelected() && mode == 1)
				{
					showErrorDialog(textBundle.getString("ssecore.text.PasswordsDontMatch"));
					throw new IllegalArgumentException();
				}
				
			} catch (IllegalArgumentException ex) {
				Arrays.fill(p, '\u0000');
				Arrays.fill(p2, '\u0000');
				p = null;
				return;
			}				
			
			Arrays.fill(p2, '\u0000');
			
			SimplePasswordDialog.this.setVisible(false);
			SimplePasswordDialog.this.dispose();
	    }
	};
    
    private void init() throws Exception
    {       
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridBagLayout());
        buttonPane.setBorder(new EmptyBorder(16, 8, 16, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
               
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        this.setResizable(false);
        this.setLocationByPlatform(true);
        this.setLocationRelativeTo(parentFrame); 
        
        JPanel passwordPane = new JPanel(new BorderLayout());
        passwordPane.setBorder(new EmptyBorder(10, 8, 2, 8));
        JPanel passwordLabelPane = new JPanel(new BorderLayout());
        JLabel passworLabel = new JLabel(textBundle.getString("ssecore.PasswodDialog.Password"), SwingConstants.LEFT);
        showHidePasswordCB = new JCheckBox(textBundle.getString("ssecore.PasswodDialog.ShowHide"));
        passwordTF = new PlaceholderPasswordField();
        passwordTF.enableInputMethods(true);
        passwordTF.setPlaceholder(" " + textBundle.getString("ssecore.text.Password"));
        final char echoChar = passwordTF.getEchoChar();
        passwordTF.setPreferredSize(new Dimension(219, 24));
        passwordTF.getDocument().addDocumentListener(getPasswordOnChangeListener());
        passwordConfirmTF = new PlaceholderPasswordField();
        passwordConfirmTF.enableInputMethods(true);
        passwordConfirmTF.setPlaceholder(" " + textBundle.getString("ssecore.text.PasswordConfirm"));
        passwordConfirmTF.setPreferredSize(new Dimension(250, 24));
        passwordLabelPane.add(passworLabel, BorderLayout.LINE_START);
        passwordLabelPane.add(showHidePasswordCB, BorderLayout.LINE_END);
        passwordPane.add(passwordLabelPane, BorderLayout.PAGE_START);
        passwordPane.add(passwordTF, BorderLayout.CENTER);
        if(mode == 1) passwordPane.add(passwordConfirmTF, BorderLayout.PAGE_END);
        
        showHidePasswordCB.addActionListener(new ActionListener() 
        {	
        	public void actionPerformed(ActionEvent evt)  
			{
        		if(showHidePasswordCB.isSelected()) {
        			passwordTF.setEchoChar((char)0);
        			passwordConfirmTF.setText(Helpers.createStringWithLength(passwordTF.getPassword().length, '.'));
        			passwordConfirmTF.setEnabled(false);
        			passwordConfirmTF.setBackground(buttonPane.getBackground());
        		}
        		else { 
        			passwordTF.setEchoChar(echoChar);
        			passwordConfirmTF.setEnabled(true);
        			Helpers.setTextToPasswordField(passwordConfirmTF, passwordTF.getPassword());
        			passwordConfirmTF.setBackground(passwordTF.getBackground());
        		}
			}
        });
        
        continueButton = new JButton(textBundle.getString("ssecore.text.Continue"));
        continueButton.setPreferredSize(new Dimension(100, 30));
        continueButton.addActionListener(action);
        passwordTF.addActionListener(action);
        passwordConfirmTF.addActionListener(action);
        
        cancelButton = new JButton(textBundle.getString("ssecore.text.Cancel"));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				p = null;
				SimplePasswordDialog.this.setVisible(false);
				SimplePasswordDialog.this.dispose();
			}
		});
        
        
        if(Helpers.isMac()) {
        	buttonPane.add(cancelButton, gbc);
        	buttonPane.add(new JLabel("      "), gbc);   
        	buttonPane.add(continueButton, gbc); 
        }
        else {
            buttonPane.add(continueButton, gbc); 
            buttonPane.add(new JLabel("      "), gbc);   
            buttonPane.add(cancelButton, gbc);
        }
        
        this.add(passwordPane, BorderLayout.CENTER);
        this.add(buttonPane, BorderLayout.SOUTH);  
        
        EventQueue.invokeLater(new Runnable(){
        	public void run() 
        	{
	        	passwordTF.grabFocus();
	        	passwordTF.requestFocus();
        	}
        });
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
            	if(showHidePasswordCB.isSelected()) {
        			passwordConfirmTF.setText(Helpers.createStringWithLength(passwordTF.getPassword().length, '.'));
            	}
            }
    	};
    }
    
    /** Show Error Dialog */
    private void showErrorDialog(String message)
    {
    	final MessageDialog messageDialog = new MessageDialog(parentFrame, "", MessageDialog.ICON_NEGATIVE);
    	messageDialog.setText("<html>" + message + "</html>");
    	messageDialog.showDialog();
    }
}
