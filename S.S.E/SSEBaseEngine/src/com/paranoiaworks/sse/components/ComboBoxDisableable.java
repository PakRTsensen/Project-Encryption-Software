package com.paranoiaworks.sse.components;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ItemEvent;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * ComboBox with disableable items
 * 
 * @author Paranoia Works
 * @version 1.0.0
 */ 


@SuppressWarnings("serial")
public class ComboBoxDisableable extends JComboBox<String> {
	
	private Frame parentFrame = null;
	private String predefinedDisabledSelectionMessage = null;
	private int lastOkIndex = 0;
	private DefaultListSelectionModel model;
    private EnabledComboBoxRenderer enableRenderer;
	
    public ComboBoxDisableable(Frame frame, String[] items, int[] selectableInterval, String disabledSelectionMessage)
    {
        super(items);
        if(selectableInterval == null) return;
        this.parentFrame = frame;
        this.predefinedDisabledSelectionMessage = disabledSelectionMessage;
        
        model = new DefaultListSelectionModel();
        enableRenderer = new EnabledComboBoxRenderer();
        model.addSelectionInterval(selectableInterval[0], selectableInterval[1]);
        enableRenderer.setEnabledItems(model);
        this.setRenderer(enableRenderer);
        
        this.addItemListener(e -> {
            if (this.getSelectedIndex() < selectableInterval[0] || this.getSelectedIndex() > selectableInterval[1]) 
            {
               if (e.getStateChange() == ItemEvent.SELECTED) 
               {
            	   try {
					final MessageDialog messageDialog = new MessageDialog(parentFrame, "", MessageDialog.ICON_INFO_RED);
					   messageDialog.setText("<html>" + this.getSelectedItem() + " — " + predefinedDisabledSelectionMessage + "</html>");
					   messageDialog.showDialog();
				} catch (Exception e1) {
					// N/A
				}
               } 
               this.setSelectedIndex(lastOkIndex);
            }
            else lastOkIndex = this.getSelectedIndex();
         });
  
    }
    
    private class EnabledComboBoxRenderer extends BasicComboBoxRenderer 
    {
		private ListSelectionModel enabledItems;
        public EnabledComboBoxRenderer() {}

        public void setEnabledItems(ListSelectionModel enabled) {
            this.enabledItems = enabled;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            Component c = super.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            if (!enabledItems.isSelectedIndex(index)) {
                if (isSelected) {
                    c.setBackground(UIManager.getColor("ComboBox.background"));
                } else {
                    c.setBackground(super.getBackground());
                }

                setForeground(UIManager.getColor("Label.disabledForeground"));

            } else {
                c.setBackground(super.getBackground());
                c.setForeground(super.getForeground());
            }
            return c;
        }
    }
    
}
