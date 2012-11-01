package net.blerf.ftl.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.blerf.ftl.ui.RegexDocument;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FieldEditorPanel extends JPanel {
	public enum ContentType { STRING, INTEGER, BOOLEAN, SLIDER };

	private static final Logger log = LogManager.getLogger(SavedGameGeneralPanel.class);

	private HashMap<String, JTextField> stringMap = new HashMap<String, JTextField>();
	private HashMap<String, JTextField> intMap = new HashMap<String, JTextField>();
	private HashMap<String, JCheckBox> boolMap = new HashMap<String, JCheckBox>();
	private HashMap<String, JSlider> sliderMap = new HashMap<String, JSlider>();
	private HashMap<String, JLabel> reminderMap = new HashMap<String, JLabel>();

	private GridBagConstraints gridC = new GridBagConstraints();

	private Component valueStrut = Box.createHorizontalStrut(120);
	private Component reminderStrut = Box.createHorizontalStrut(90);

	private boolean remindersVisible;

	public FieldEditorPanel( boolean remindersVisible ) {
		super( new GridBagLayout() );
		this.remindersVisible = remindersVisible;

		gridC.anchor = GridBagConstraints.WEST;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 0.0;
		gridC.weighty = 0.0;
		gridC.gridwidth = 1;
		gridC.gridx = 0;
		gridC.gridy = 0;

		// No default width for col 0.
		gridC.gridx = 0;
		this.add( Box.createVerticalStrut(1), gridC );
		gridC.gridx++;
		this.add( valueStrut, gridC );
		gridC.gridx++;
		if ( remindersVisible ) {
			this.add( reminderStrut, gridC );
			gridC.gridy++;
		}

		gridC.insets = new Insets(2, 4, 2, 4);
	}

	public void setValueWidth( int width ) {
		valueStrut.setMinimumSize( new Dimension(width, 0) );
		valueStrut.setPreferredSize( new Dimension(width, 0) );
	}

	public void setReminderWidth( int width ) {
		reminderStrut.setMinimumSize( new Dimension(width, 0) );
		reminderStrut.setPreferredSize( new Dimension(width, 0) );
	}

	/**
	 * Constructs JComponents for a given type of value.
	 * A row consists of a static label, some JComponent,
	 * and a reminder label.
	 *
	 * The component and reminder will be accessable later
	 * via getter methods.
	 */
	public void addRow( String valueName, ContentType contentType ) {
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.gridwidth = 1;
		gridC.weighty = 0.0;
		gridC.gridx = 0;
		this.add( new JLabel( valueName +":" ), gridC );

		gridC.gridx++;
		if ( contentType == ContentType.STRING ) {
			gridC.anchor = GridBagConstraints.WEST;
			JTextField valueField = new JTextField();
			stringMap.put( valueName, valueField );
			this.add( valueField, gridC );
		}
		else if ( contentType == ContentType.INTEGER ) {
			gridC.anchor = GridBagConstraints.WEST;
			JTextField valueField = new JTextField();
			valueField.setHorizontalAlignment( JTextField.RIGHT );
			valueField.setDocument( new RegexDocument("[0-9]*") );
			intMap.put( valueName, valueField );
			this.add( valueField, gridC );
		}
		else if ( contentType == ContentType.BOOLEAN ) {
			gridC.anchor = GridBagConstraints.CENTER;
			JCheckBox valueCheck = new JCheckBox();
			valueCheck.setHorizontalAlignment( SwingConstants.CENTER );
			boolMap.put( valueName, valueCheck );
			this.add( valueCheck, gridC );
		}
		else if ( contentType == ContentType.SLIDER ) {
			gridC.anchor = GridBagConstraints.CENTER;
			JPanel panel = new JPanel();
			panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
			final JSlider valueSlider = new JSlider( JSlider.HORIZONTAL );
			valueSlider.setPreferredSize( new Dimension(50, valueSlider.getPreferredSize().height) );
			sliderMap.put( valueName, valueSlider );
			panel.add(valueSlider);
			final JTextField valueField = new JTextField(3);
			valueField.setMaximumSize( valueField.getPreferredSize() );
			valueField.setHorizontalAlignment( JTextField.RIGHT );
			valueField.setEditable( false );
			panel.add(valueField);
			this.add( panel, gridC );

			valueSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					valueField.setText( ""+valueSlider.getValue() );
				}
			});
		}
		gridC.gridx++;

		if ( remindersVisible ) {
			gridC.anchor = GridBagConstraints.WEST;
			JLabel valueReminder = new JLabel();
			reminderMap.put( valueName, valueReminder );
			this.add( valueReminder, gridC );
		}

		gridC.gridy++;
	}

	public void addBlankRow() {
		gridC.fill = GridBagConstraints.NONE;
		gridC.weighty = 0.0;
		gridC.gridwidth = GridBagConstraints.REMAINDER;
		gridC.gridx = 0;

		this.add(Box.createVerticalStrut(12), gridC);
		gridC.gridy++;
	}

	public void setStringAndReminder( String valueName, String s ) {
		JTextField valueField = stringMap.get( valueName );
		if ( valueField != null ) valueField.setText(s);
		if ( remindersVisible ) setReminder( valueName, s );
	}

	public void setIntAndReminder( String valueName, int n ) {
		setIntAndReminder( valueName, n, ""+n );
	}
	public void setIntAndReminder( String valueName, int n, String s ) {
		JTextField valueField = intMap.get( valueName );
		if ( valueField != null ) valueField.setText( ""+n );
		if ( remindersVisible ) setReminder( valueName, s );
	}

	public void setBoolAndReminder( String valueName, boolean b ) {
		setBoolAndReminder( valueName, b, ""+b );
	}
	public void setBoolAndReminder( String valueName, boolean b, String s ) {
		JCheckBox valueCheck = boolMap.get( valueName );
		if ( valueCheck != null ) valueCheck.setSelected(b);
		if ( remindersVisible ) setReminder( valueName, s );
	}

	public void setSliderAndReminder( String valueName, int n ) {
		setSliderAndReminder( valueName, n, ""+n );
	}
	public void setSliderAndReminder( String valueName, int n, String s ) {
		JSlider valueSlider = sliderMap.get( valueName );
		if ( valueSlider != null ) valueSlider.setValue(n);
		if ( remindersVisible ) setReminder( valueName, s );
	}

	public void setReminder( String valueName, String s ) {
		JLabel valueReminder = reminderMap.get( valueName );
		if ( valueReminder != null ) valueReminder.setText( "( "+ s +" )" );
	}

	public JTextField getString( String valueName ) {
		return stringMap.get( valueName );
	}

	public JTextField getInt( String valueName ) {
		return intMap.get( valueName );
	}

	public JCheckBox getBoolean( String valueName ) {
		return boolMap.get( valueName );
	}

	public JSlider getSlider( String valueName ) {
		return sliderMap.get( valueName );
	}

	public void reset() {
		for (JTextField valueField : stringMap.values())
			valueField.setText("");

		for (JTextField valueField : intMap.values())
			valueField.setText("");

		for (JCheckBox valueCheck : boolMap.values())
			valueCheck.setSelected(false);

		for (JSlider valueSlider : sliderMap.values())
			valueSlider.setValue(0);

		for (JLabel valueReminder : reminderMap.values())
			valueReminder.setText("");
	}
}
