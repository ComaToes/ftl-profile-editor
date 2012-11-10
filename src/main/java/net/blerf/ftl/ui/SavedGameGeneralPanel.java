package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

	private static final String TOTAL_SHIPS_DEFEATED = "Total Ships Defeated";
	private static final String TOTAL_BEACONS = "Total Beacons";
	private static final String TOTAL_SCRAP = "Total Scrap";
	private static final String TOTAL_CREW_HIRED = "Total Crew Hired";
	private static final String ALPHA = "Alpha?";
	private static final String DIFFICULTY_EASY = "Easy Difficulty";

	private static final String HAZARDS_VISIBLE = "Hazards Visible";

	private FTLFrame frame;
	private FieldEditorPanel sessionPanel = null;
	private FieldEditorPanel sectorPanel = null;

	public SavedGameGeneralPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		sessionPanel = new FieldEditorPanel( true );
		sessionPanel.setBorder( BorderFactory.createTitledBorder("Session") );
		sessionPanel.addRow( TOTAL_SHIPS_DEFEATED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_BEACONS, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_SCRAP, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_CREW_HIRED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( ALPHA, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( DIFFICULTY_EASY, FieldEditorPanel.ContentType.BOOLEAN );
		sessionPanel.addBlankRow();

		sessionPanel.getInt(ALPHA).addMouseListener( new StatusbarMouseListener(frame, "Unknown session field. Always 0?") );
		sessionPanel.getBoolean(DIFFICULTY_EASY).addMouseListener( new StatusbarMouseListener(frame, "Uncheck for normal difficulty.") );

		sectorPanel = new FieldEditorPanel( true );
		sectorPanel.setBorder( BorderFactory.createTitledBorder("Sector") );
		sectorPanel.addRow( HAZARDS_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );
		sectorPanel.addBlankRow();

		sectorPanel.getBoolean(HAZARDS_VISIBLE).addMouseListener( new StatusbarMouseListener(frame, "Show hazards on the current sector map.") );

		JPanel otherBorderPanel = new JPanel( new BorderLayout() );
		otherBorderPanel.setBorder( BorderFactory.createTitledBorder("Other") );
		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout(otherPanel, BoxLayout.Y_AXIS) );
		otherPanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 4) );
		JButton stealNearbyShipBtn = new JButton( "Steal Nearby Ship" );
		otherPanel.add( stealNearbyShipBtn );
		otherBorderPanel.add( otherPanel, BorderLayout.CENTER );

		stealNearbyShipBtn.addMouseListener( new StatusbarMouseListener(frame, "Take the ship parked nearby.") );

		stealNearbyShipBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SavedGameGeneralPanel.this.frame.stealNearbyShip();
			}
		});

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.fill = GridBagConstraints.NONE;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( sessionPanel, thisC );

		thisC.gridy++;
		this.add( sectorPanel, thisC );

		thisC.gridy++;
		this.add( otherBorderPanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		setGameState( null );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		sessionPanel.reset();
		sectorPanel.reset();

		if ( gameState != null ) {
			SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

			sessionPanel.setIntAndReminder( TOTAL_SHIPS_DEFEATED, gameState.getTotalShipsDefeated() );
			sessionPanel.setIntAndReminder( TOTAL_BEACONS, gameState.getTotalBeaconsExplored() );
			sessionPanel.setIntAndReminder( TOTAL_SCRAP, gameState.getTotalScrapCollected() );
			sessionPanel.setIntAndReminder( TOTAL_CREW_HIRED, gameState.getTotalCrewHired() );
			sessionPanel.setIntAndReminder( ALPHA, gameState.getHeaderAlpha() );
			sessionPanel.setBoolAndReminder( DIFFICULTY_EASY, gameState.isDifficultyEasy() );

			sectorPanel.setBoolAndReminder( HAZARDS_VISIBLE, gameState.areSectorHazardsVisible() );
		}

		this.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
		String newString = null;

		newString = sessionPanel.getInt(TOTAL_SHIPS_DEFEATED).getText();
		try { gameState.setTotalShipsDefeated(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sessionPanel.getInt(TOTAL_BEACONS).getText();
		try { gameState.setTotalBeaconsExplored(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sessionPanel.getInt(TOTAL_SCRAP).getText();
		try { gameState.setTotalScrapCollected(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sessionPanel.getInt(TOTAL_CREW_HIRED).getText();
		try { gameState.setTotalCrewHired(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sessionPanel.getInt(ALPHA).getText();
		try { gameState.setHeaderAlpha(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		gameState.setDifficultyEasy( sessionPanel.getBoolean(DIFFICULTY_EASY).isSelected() );

		gameState.setSectorHazardsVisible( sectorPanel.getBoolean(HAZARDS_VISIBLE).isSelected() );
	}
}
