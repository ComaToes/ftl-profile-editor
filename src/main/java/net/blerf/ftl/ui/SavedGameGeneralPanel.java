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
import java.util.List;

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
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

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

	private static final String SECTOR_LAYOUT_SEED = "Sector Layout Seed";
	private static final String REBEL_FLEET_OFFSET = "Rebel Fleet Offset";
	private static final String REBEL_FLEET_FUDGE = "Rebel Fleet Fudge";
	private static final String REBEL_PURSUIT_MOD = "Rebel Pursuit Mod";
	private static final String HAZARDS_VISIBLE = "Hazards Visible";

	private static final String CARGO_ONE = "#1";
	private static final String CARGO_TWO = "#2";
	private static final String CARGO_THREE = "#3";
	private static final String CARGO_FOUR = "#4";
	private static final String[] cargoSlots = new String[] { CARGO_ONE, CARGO_TWO, CARGO_THREE, CARGO_FOUR };

	private FTLFrame frame;
	private FieldEditorPanel sessionPanel = null;
	private FieldEditorPanel sectorPanel = null;
	private FieldEditorPanel cargoPanel = null;

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
		sectorPanel.addRow( SECTOR_LAYOUT_SEED, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(SECTOR_LAYOUT_SEED).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_FLEET_OFFSET, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_FLEET_OFFSET).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_FLEET_FUDGE, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_FLEET_FUDGE).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_PURSUIT_MOD, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_PURSUIT_MOD).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( HAZARDS_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );
		sectorPanel.addBlankRow();

		sectorPanel.getInt(SECTOR_LAYOUT_SEED).addMouseListener( new StatusbarMouseListener(frame, "A per-sector constant that seeds the random generation of the map, events, etc. (potentially dangerous).") );
		sectorPanel.getInt(REBEL_FLEET_OFFSET).addMouseListener( new StatusbarMouseListener(frame, "A large negative var (-750,-250,...,-n*25, approaching 0) + fudge = the fleet circle's edge.") );
		sectorPanel.getInt(REBEL_FLEET_FUDGE).addMouseListener( new StatusbarMouseListener(frame, "A random per-sector constant (usually around 75-310) + offset = the fleet circle's edge.") );
		sectorPanel.getInt(REBEL_PURSUIT_MOD).addMouseListener( new StatusbarMouseListener(frame, "Delay/alert the fleet, changing the warning zone thickness (e.g., merc distraction = -2).") );
		sectorPanel.getBoolean(HAZARDS_VISIBLE).addMouseListener( new StatusbarMouseListener(frame, "Show hazards on the current sector map.") );

		cargoPanel = new FieldEditorPanel( false );
		cargoPanel.setBorder( BorderFactory.createTitledBorder("Cargo") );

		for (int i=0; i < cargoSlots.length; i++) {
			cargoPanel.addRow( cargoSlots[i], FieldEditorPanel.ContentType.COMBO );
		}
		cargoPanel.addBlankRow();

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.fill = GridBagConstraints.NORTH;
		thisC.fill = GridBagConstraints.BOTH;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( sessionPanel, thisC );

		thisC.gridx++;
		this.add( cargoPanel, thisC );

		thisC.gridx = 0;
		thisC.gridy++;
		this.add( sectorPanel, thisC );

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
		cargoPanel.reset();

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

			sectorPanel.setIntAndReminder( SECTOR_LAYOUT_SEED, gameState.getSectorLayoutSeed() );
			sectorPanel.setIntAndReminder( REBEL_FLEET_OFFSET, gameState.getRebelFleetOffset() );
			sectorPanel.setIntAndReminder( REBEL_FLEET_FUDGE, gameState.getRebelFleetFudge() );
			sectorPanel.setIntAndReminder( REBEL_PURSUIT_MOD, gameState.getRebelPursuitMod() );
			sectorPanel.setBoolAndReminder( HAZARDS_VISIBLE, gameState.areSectorHazardsVisible() );

			for (int i=0; i < cargoSlots.length; i++) {
				cargoPanel.getCombo(cargoSlots[i]).addItem( "" );
				cargoPanel.getCombo(cargoSlots[i]).addItem( "Weapons" );
				cargoPanel.getCombo(cargoSlots[i]).addItem( "-------" );
				for ( WeaponBlueprint weaponBlueprint : DataManager.get().getWeapons().values() ) {
					cargoPanel.getCombo(cargoSlots[i]).addItem( weaponBlueprint );
				}
				cargoPanel.getCombo(cargoSlots[i]).addItem( "" );
				cargoPanel.getCombo(cargoSlots[i]).addItem( "Drones" );
				cargoPanel.getCombo(cargoSlots[i]).addItem( "------" );
				for ( DroneBlueprint droneBlueprint : DataManager.get().getDrones().values() ) {
					cargoPanel.getCombo(cargoSlots[i]).addItem( droneBlueprint );
				}

				if ( gameState.getCargoIdList().size() > i ) {
					String cargoId = gameState.getCargoIdList().get(i);

					if ( DataManager.get().getWeapons().containsKey( cargoId ) ) {
						WeaponBlueprint weaponBlueprint = DataManager.get().getWeapon( cargoId );
						cargoPanel.getCombo(cargoSlots[i]).setSelectedItem( weaponBlueprint );
					}
					else if ( DataManager.get().getDrones().containsKey( cargoId ) ) {
						DroneBlueprint droneBlueprint = DataManager.get().getDrone( cargoId );
						cargoPanel.getCombo(cargoSlots[i]).setSelectedItem( droneBlueprint );
					}
				}
			}
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

		newString = sectorPanel.getInt(SECTOR_LAYOUT_SEED).getText();
		try { gameState.setSectorLayoutSeed(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sectorPanel.getInt(REBEL_FLEET_OFFSET).getText();
		try { gameState.setRebelFleetOffset(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sectorPanel.getInt(REBEL_FLEET_FUDGE).getText();
		try { gameState.setRebelFleetFudge(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		newString = sectorPanel.getInt(REBEL_PURSUIT_MOD).getText();
		try { gameState.setRebelPursuitMod(Integer.parseInt(newString)); }
		catch (NumberFormatException e) {}

		gameState.setSectorHazardsVisible( sectorPanel.getBoolean(HAZARDS_VISIBLE).isSelected() );

		gameState.getCargoIdList().clear();
		for (int i=0; i < cargoSlots.length; i++) {
			Object cargoObj = cargoPanel.getCombo(cargoSlots[i]).getSelectedItem();
			if ( cargoObj instanceof WeaponBlueprint ) {
				gameState.addCargoItemId( ((WeaponBlueprint)cargoObj).getId() );
			}
			else if ( cargoObj instanceof DroneBlueprint ) {
				gameState.addCargoItemId( ((DroneBlueprint)cargoObj).getId() );
			}
		}
	}
}
