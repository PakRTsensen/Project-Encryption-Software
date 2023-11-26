package com.paranoiaworks.sse.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.paranoiaworks.sse.Helpers;

/**
 * Simple OK/Cancel Dialog
 * 
 * @author Paranoia Works
 * @version 1.0.2
 */ 

@SuppressWarnings("serial")
public class OkCancelDialog extends JDialog {
    
	private Frame parentFrame = null;
	private JLabel iconL = null;
	private JLabel textL = null;
	private ResourceBundle textBundle;
	JButton okButton = null;
	JButton cancelButton = null;
	
	public static final int ICON_QUESTION = 1;
	public static final int ICON_INFO_BLUE = 2;
	public static final int ICON_INFO_RED = 3;
	
	
	private static final int MINIMAL_HEIGHT = 100;
	
	public OkCancelDialog(Frame frame)
    {
		this(frame, ICON_QUESTION);
    }
	
	public OkCancelDialog(Frame frame, int iconCode)
    {
        super(frame, "", true);
        this.parentFrame = frame;
        this.textBundle = ResourceBundle.getBundle("rescore.SSEBaseEngineText", Locale.getDefault());
        
        Image image = null;       
        switch (iconCode) 
        {        
        	case ICON_QUESTION:
        		image = Helpers.loadImageFromResource(MessageDialog.class, "ask_icon", "png", "rescore/");
            	break;
        	case ICON_INFO_BLUE:
        		image = Helpers.loadImageFromResource(MessageDialog.class, "info_icon_blue", "png", "rescore/");
            	break;
        	case ICON_INFO_RED:
        		image = Helpers.loadImageFromResource(MessageDialog.class, "info_icon_red", "png", "rescore/");
            	break;
        	default: 
        		image = Helpers.loadImageFromResource(MessageDialog.class, "ask_icon", "png", "rescore/");
            	break;
        }
        
        if(image != null) {
	        Icon icon = new ImageIcon(image);
	        iconL = new JLabel(icon);
        }
        else {
        	iconL = new JLabel();
        }
        
        iconL.setBorder(new EmptyBorder(14, 15, 10, 15));
        
        textL = new JLabel();
        textL.setBorder(new EmptyBorder(5, 5, 5, 30));
        
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridBagLayout());
        buttonPane.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.BASELINE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        String okButtonLabel = (iconCode == ICON_QUESTION) ? textBundle.getString("ssecore.text.Yes") : "OK";
        okButton = new JButton(okButtonLabel);
        okButton.setPreferredSize(new Dimension(100, 30));      
        
        String cancelButtonLabel = (iconCode == ICON_QUESTION) ? textBundle.getString("ssecore.text.No") : textBundle.getString("ssecore.text.Cancel");
        cancelButton = new JButton(cancelButtonLabel);
        cancelButton.setPreferredSize(new Dimension(100, 30));   
        
        
        if(Helpers.isMac()) {
        	buttonPane.add(cancelButton, gbc);
	        buttonPane.add(new JLabel("       "), gbc);
	        buttonPane.add(okButton, gbc);
        }
        else {
	        buttonPane.add(okButton, gbc);
	        buttonPane.add(new JLabel("       "), gbc);
	        buttonPane.add(cancelButton, gbc);
        }       
               
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setMinimumSize(new Dimension(500, MINIMAL_HEIGHT));

        this.setResizable(false);
        this.setLocationByPlatform(true);
        this.setLocationRelativeTo(frame);     
        
        this.add(iconL, BorderLayout.LINE_START);
        this.add(textL, BorderLayout.CENTER);
        this.add(buttonPane, BorderLayout.SOUTH);
    }
	
    public synchronized void setText(String text) 
    {
    	textL.setText("<html>" + text + "</html>");
    }
    
    public synchronized void setListeners(ActionListener okButtonListener, ActionListener cancelButtonListener) 
    {
    	okButton.addActionListener(okButtonListener);
    	cancelButton.addActionListener(cancelButtonListener);
    }
    
    public synchronized void setMinimumWidth(int width) 
    {
    	this.setMinimumSize(new Dimension(width, MINIMAL_HEIGHT));
    }
	
    public synchronized void showDialog() 
    {
    	this.pack();
    	this.setLocationRelativeTo(parentFrame);
    	this.setVisible(true);
    }
}
