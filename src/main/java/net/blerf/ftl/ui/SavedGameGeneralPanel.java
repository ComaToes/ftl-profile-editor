package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
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

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.RegexDocument;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameGeneralPanel extends JPanel {
	private enum ContentType { STRING, INTEGER, BOOLEAN, SLIDER };

	private static final Logger log = LogManager.getLogger(SavedGameGeneralPanel.class);

	private static final String SHIP_NAME="Ship Name", HULL="Hull", FUEL="Fuel", DRONE_PARTS="Drone Parts",
	                            MISSILES="Missiles", SCRAP="Scrap", HAZARDS_VISIBLE="Sector Hazards Visible",
	                            FULL_OXYGEN="Full Oxygen", NO_BREACHES="No Breaches", NO_FIRES="No Fires";

	private FTLFrame frame;

	private HashMap<String, JTextField> stringMap = new HashMap<String, JTextField>();
	private HashMap<String, JCheckBox> boolMap = new HashMap<String, JCheckBox>();
	private HashMap<String, JSlider> sliderMap = new HashMap<String, JSlider>();
	private HashMap<String, JLabel> reminderMap = new HashMap<String, JLabel>();

	public SavedGameGeneralPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		GridBagConstraints gridC = new GridBagConstraints();
		gridC.anchor = GridBagConstraints.WEST;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 0.0;
		gridC.weighty = 0.0;
		gridC.gridwidth = 1;
		gridC.gridx = 0;
		gridC.gridy = 0;

		JPanel contentPane = new JPanel( new GridBagLayout() );
		contentPane.setBorder( BorderFactory.createTitledBorder("General") );

		// No default width for col 0.
		gridC.gridx = 0;
		contentPane.add( Box.createVerticalStrut(1), gridC );
		gridC.gridx++;
		contentPane.add( Box.createHorizontalStrut(120), gridC );
		gridC.gridx++;
		contentPane.add( Box.createHorizontalStrut(90), gridC );
		gridC.gridy++;

		gridC.insets = new Insets(2, 4, 2, 4);
		addBlankRow( contentPane, gridC );
		addRow( contentPane, SHIP_NAME, ContentType.STRING, gridC );
		addRow( contentPane, HULL, ContentType.SLIDER, gridC );
		addRow( contentPane, FUEL, ContentType.INTEGER, gridC );
		addRow( contentPane, DRONE_PARTS, ContentType.INTEGER, gridC );
		addRow( contentPane, MISSILES, ContentType.INTEGER, gridC );
		addRow( contentPane, SCRAP, ContentType.INTEGER, gridC );
		addRow( contentPane, HAZARDS_VISIBLE, ContentType.BOOLEAN, gridC );
		addBlankRow( contentPane, gridC );
		addRow( contentPane, FULL_OXYGEN, ContentType.BOOLEAN, gridC );
		addRow( contentPane, NO_BREACHES, ContentType.BOOLEAN, gridC );
		addRow( contentPane, NO_FIRES, ContentType.BOOLEAN, gridC );
		addBlankRow( contentPane, gridC );

		boolMap.get(HAZARDS_VISIBLE).addMouseListener( new StatusbarMouseListener(frame, "Show hazards on the current sector map.") );
		boolMap.get(FULL_OXYGEN).addMouseListener( new StatusbarMouseListener(frame, "Set all rooms' oxygen to 100%.") );
		boolMap.get(NO_BREACHES).addMouseListener( new StatusbarMouseListener(frame, "Remove all hull breaches.") );
		boolMap.get(NO_FIRES).addMouseListener( new StatusbarMouseListener(frame, "Remove all fires.") );

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.fill = GridBagConstraints.NONE;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( contentPane, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		setGameState( null );
	}

	/**
	 * Constructs JComponents for a given type of value.
	 * A row consists of a static label, some JComponent, and a reminder label.
	 * The last two will be accessable later via Maps.
	 */
	private void addRow( JPanel parent, String valueName, ContentType contentType, GridBagConstraints gridC ) {
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.gridwidth = 1;
		gridC.gridx = 0;
		parent.add( new JLabel( valueName +":" ), gridC );

		gridC.gridx++;
		if ( contentType == ContentType.STRING ) {
			gridC.anchor = GridBagConstraints.WEST;
			JTextField valueField = new JTextField();
			stringMap.put( valueName, valueField );
			parent.add( valueField, gridC );
		}
		else if ( contentType == ContentType.INTEGER ) {
			gridC.anchor = GridBagConstraints.WEST;
			JTextField valueField = new JTextField();
			valueField.setHorizontalAlignment( JTextField.RIGHT );
			valueField.setDocument( new RegexDocument("[0-9]*") );
			stringMap.put( valueName, valueField );
			parent.add( valueField, gridC );
		}
		else if ( contentType == ContentType.BOOLEAN ) {
			gridC.anchor = GridBagConstraints.CENTER;
			JCheckBox valueCheck = new JCheckBox();
			valueCheck.setHorizontalAlignment( SwingConstants.CENTER );
			boolMap.put( valueName, valueCheck );
			parent.add( valueCheck, gridC );
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
			parent.add( panel, gridC );

			valueSlider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					valueField.setText( ""+valueSlider.getValue() );
				}
			});
		}

		gridC.anchor = GridBagConstraints.WEST;
		gridC.gridx++;
		JLabel valueReminder = new JLabel();
		reminderMap.put( valueName, valueReminder );
		parent.add( valueReminder, gridC );

		gridC.gridy++;
	}

	public void addBlankRow( JPanel parent, GridBagConstraints gridC ) {
		gridC.fill = GridBagConstraints.NONE;
		gridC.weighty = 0.0;
		gridC.gridwidth = GridBagConstraints.REMAINDER;
		gridC.gridx = 0;

		parent.add(Box.createVerticalStrut(12), gridC);
		gridC.gridy++;
	}

	private void setStringAndReminder( String valueName, String s ) {
		JTextField valueField = stringMap.get( valueName );
		if ( valueField != null ) valueField.setText(s);
		setReminder( valueName, s );
	}

	private void setBoolAndReminder( String valueName, boolean b ) {
		setBoolAndReminder( valueName, b, ""+b );
	}
	private void setBoolAndReminder( String valueName, boolean b, String s ) {
		JCheckBox valueCheck = boolMap.get( valueName );
		if ( valueCheck != null ) valueCheck.setSelected(b);
		setReminder( valueName, s );
	}

	private void setSliderAndReminder( String valueName, int n ) {
		setSliderAndReminder( valueName, n, ""+n );
	}
	private void setSliderAndReminder( String valueName, int n, String s ) {
		JSlider valueSlider = sliderMap.get( valueName );
		if ( valueSlider != null ) valueSlider.setValue(n);
		setReminder( valueName, s );
	}

	private void setReminder( String valueName, String s ) {
		JLabel valueReminder = reminderMap.get( valueName );
		if ( valueReminder != null ) valueReminder.setText( "( "+ s +" )" );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		for (JTextField valueField : stringMap.values())
			valueField.setText("");

		for (JCheckBox valueCheck : boolMap.values())
			valueCheck.setSelected(false);

		for (JSlider valueSlider : sliderMap.values())
			valueSlider.setValue(0);

		for (JLabel valueReminder : reminderMap.values())
			valueReminder.setText("");

		if ( gameState != null ) {
			SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

			setStringAndReminder( SHIP_NAME, gameState.getPlayerShipName() );

			sliderMap.get(HULL).setMaximum( shipBlueprint.getHealth().amount );
			setSliderAndReminder( HULL, shipState.getHullAmt() );

			setStringAndReminder( FUEL, ""+shipState.getFuelAmt() );
			setStringAndReminder( DRONE_PARTS, ""+shipState.getDronePartsAmt() );
			setStringAndReminder( MISSILES, ""+shipState.getMissilesAmt() );
			setStringAndReminder( SCRAP, ""+shipState.getScrapAmt() );
			setBoolAndReminder( HAZARDS_VISIBLE, gameState.areSectorHazardsVisible() );

			int meanOxygen = 0;
			int breachCount = shipState.getBreachMap().size();
			int fireCount = 0;
			for (SavedGameParser.RoomState room : shipState.getRoomList()) {
				meanOxygen += room.getOxygen();

				for (int[] square : room.getSquareList()) {
					if ( square[0] > 0 ) fireCount++;
				}
			}
			meanOxygen /= shipState.getRoomList().size();

			setBoolAndReminder( FULL_OXYGEN, (meanOxygen == 100), meanOxygen +"%" );
			setBoolAndReminder( NO_BREACHES, (breachCount == 0), ""+breachCount );
			setBoolAndReminder( NO_FIRES, (fireCount == 0), ""+fireCount );
		}

		this.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		String newString = null;

		newString = stringMap.get(SHIP_NAME).getText();
		if ( newString.length() > 0 ) {
			gameState.setPlayerShipName( newString );
			shipState.setShipName( newString );
		}

		shipState.setHullAmt( sliderMap.get(HULL).getValue() );

		newString = stringMap.get(FUEL).getText();
		try { shipState.setFuelAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = stringMap.get(DRONE_PARTS).getText();
		try { shipState.setDronePartsAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = stringMap.get(MISSILES).getText();
		try { shipState.setMissilesAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = stringMap.get(SCRAP).getText();
		try { shipState.setScrapAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		gameState.setSectorHazardsVisible( boolMap.get(HAZARDS_VISIBLE).isSelected() );

		boolean fullOxygen = boolMap.get(FULL_OXYGEN).isSelected();
		boolean noBreaches = boolMap.get(NO_BREACHES).isSelected();
		boolean noFires = boolMap.get(NO_FIRES).isSelected();

		if ( noBreaches ) shipState.getBreachMap().clear();

		for (SavedGameParser.RoomState room : shipState.getRoomList()) {
			if ( fullOxygen ) room.setOxygen(100);

			if ( noFires ) {
				for (int[] square : room.getSquareList()) {
					square[0] = 0;
					square[1] = 0;
					// TODO: Determine if the square's third value is relevant.
				}
			}
		}
	}
}
