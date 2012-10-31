package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;

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

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameGeneralPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(SavedGameGeneralPanel.class);

	private static final String SHIP_NAME="Ship Name", HULL="Hull", FUEL="Fuel", DRONE_PARTS="Drone Parts",
	                            MISSILES="Missiles", SCRAP="Scrap", HAZARDS_VISIBLE="Sector Hazards Visible";

	private FTLFrame frame;
	private FieldEditorPanel generalPanel = null;

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

		generalPanel = new FieldEditorPanel( true );
		generalPanel.setBorder( BorderFactory.createTitledBorder("General") );

		generalPanel.addBlankRow();
		generalPanel.addRow( SHIP_NAME, FieldEditorPanel.ContentType.STRING );
		generalPanel.addRow( HULL, FieldEditorPanel.ContentType.SLIDER );
		generalPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		generalPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );
		generalPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		generalPanel.addRow( SCRAP, FieldEditorPanel.ContentType.INTEGER );
		generalPanel.addRow( HAZARDS_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );
		generalPanel.addBlankRow();

		generalPanel.getBoolean(HAZARDS_VISIBLE).addMouseListener( new StatusbarMouseListener(frame, "Show hazards on the current sector map.") );

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.fill = GridBagConstraints.NONE;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( generalPanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		setGameState( null );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		generalPanel.reset();

		if ( gameState != null ) {
			SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

			generalPanel.setStringAndReminder( SHIP_NAME, gameState.getPlayerShipName() );

			generalPanel.getSlider(HULL).setMaximum( shipBlueprint.getHealth().amount );
			generalPanel.setSliderAndReminder( HULL, shipState.getHullAmt() );

			generalPanel.setIntAndReminder( FUEL, shipState.getFuelAmt() );
			generalPanel.setIntAndReminder( DRONE_PARTS, shipState.getDronePartsAmt() );
			generalPanel.setIntAndReminder( MISSILES, shipState.getMissilesAmt() );
			generalPanel.setIntAndReminder( SCRAP, shipState.getScrapAmt() );
			generalPanel.setBoolAndReminder( HAZARDS_VISIBLE, gameState.areSectorHazardsVisible() );
		}

		this.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		String newString = null;

		newString = generalPanel.getString(SHIP_NAME).getText();
		if ( newString.length() > 0 ) {
			gameState.setPlayerShipName( newString );
			shipState.setShipName( newString );
		}

		shipState.setHullAmt( generalPanel.getSlider(HULL).getValue() );

		newString = generalPanel.getInt(FUEL).getText();
		try { shipState.setFuelAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = generalPanel.getInt(DRONE_PARTS).getText();
		try { shipState.setDronePartsAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = generalPanel.getInt(MISSILES).getText();
		try { shipState.setMissilesAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = generalPanel.getInt(SCRAP).getText();
		try { shipState.setScrapAmt(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		gameState.setSectorHazardsVisible( generalPanel.getBoolean(HAZARDS_VISIBLE).isSelected() );
	}
}
