package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
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

import net.blerf.ftl.constants.Difficulty;
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
	private static final String DLC = "DLC";
	private static final String DIFFICULTY = "Difficulty";
	private static final String TOP_BETA = "Beta?";

	private static final String CARGO_ONE = "#1";
	private static final String CARGO_TWO = "#2";
	private static final String CARGO_THREE = "#3";
	private static final String CARGO_FOUR = "#4";
	private static final String[] cargoSlots = new String[] { CARGO_ONE, CARGO_TWO, CARGO_THREE, CARGO_FOUR };

	private static final String SECTOR_TREE_SEED = "Sector Tree Seed";
	private static final String SECTOR_NUMBER = "Sector Number";
	private static final String SECTOR_LAYOUT_SEED = "Sector Layout Seed";
	private static final String REBEL_FLEET_OFFSET = "Rebel Fleet Offset";
	private static final String REBEL_FLEET_FUDGE = "Rebel Fleet Fudge";
	private static final String REBEL_PURSUIT_MOD = "Rebel Pursuit Mod";
	private static final String HIDDEN_SECTOR = "In Hidden Sector";
	private static final String HAZARDS_VISIBLE = "Hazards Visible";

	private static final String FLAGSHIP = "Flagship Visible";
	private static final String FLAGSHIP_HOP = "Flagship Hop";
	private static final String FLAGSHIP_MOVING = "Flagship Moving";
	private static final String FLAGSHIP_BASE_TURNS = "Flagship Base Turns";
	private static final String FLAGSHIP_NEARBY = "Flagship Nearby";
	private static final String FLAGSHIP_ALPHA = "Alpha?";
	private static final String FLAGSHIP_GAMMA = "Gamma?";
	private static final String FLAGSHIP_DELTA = "Delta?";

	private static final String ENV_RED_GIANT_PRESENT = "Red Giant Present";
	private static final String ENV_PULSAR_PRESENT = "Pulsar Present";
	private static final String ENV_PDS_PRESENT = "PDS Present";
	private static final String ENV_VULN = "Vulnerable Ships";
	private static final String ENV_ASTEROID_FIELD = "Asteroid Field";
	private static final String ENV_ASTEROID_ALPHA = "Alpha?";
	private static final String ENV_ASTEROID_STRAY_TICKS = "Stray Rock Ticks?";
	private static final String ENV_ASTEROID_GAMMA = "Gamma?";
	private static final String ENV_ASTEROID_BKG_TICKS = "Bkg Drift Ticks";
	private static final String ENV_ASTEROID_TARGET = "Current Target?";
	private static final String ENV_FLARE_FADE_TICKS = "Flare Fade Ticks?";
	private static final String ENV_HAVOC_TICKS = "Havoc Ticks?";
	private static final String ENV_PDS_TICKS = "PDS Ticks?";

	private static final String AI_SURRENDERED = "Surrendered";
	private static final String AI_ESCAPING = "Escaping";
	private static final String AI_DESTROYED = "Destroyed";
	private static final String AI_SURRENDER_THRESHOLD = "Surrender Threshold";
	private static final String AI_ESCAPE_THRESHOLD = "Escape Threshold";
	private static final String AI_ESCAPE_TICKS = "Escape Ticks";
	private static final String AI_STALEMATE = "Stalemate Triggered?";
	private static final String AI_STALEMATE_TICKS = "Stalemate Ticks?";
	private static final String AI_BOARDING_ATTEMPTS = "Boarding Attempts?";
	private static final String AI_BOARDERS_NEEDED = "Boarders Needed?";

	private static final String TOP_WAITING = "Waiting";
	private static final String TOP_WAIT_EVENT_SEED = "Wait Event Seed";
	private static final String TOP_EPSILON = "Epsilon?";
	private static final String TOP_KAPPA = "Kappa?";
	private static final String TOP_MU = "Mu?";
	private static final String TOP_NU = "Nu?";
	private static final String TOP_XI = "Xi?";
	private static final String TOP_AUTOFIRE = "Autofire";

	private FTLFrame frame;

	private FieldEditorPanel sessionPanel = null;
	private FieldEditorPanel cargoPanel = null;
	private FieldEditorPanel sectorPanel = null;
	private FieldEditorPanel bossPanel = null;
	private FieldEditorPanel envPanel = null;
	private FieldEditorPanel aiPanel = null;
	private FieldEditorPanel unknownsPanel = null;

	private boolean flagshipEnabled = true;
	private boolean envEnabled = true;
	private boolean aiEnabled = true;


	public SavedGameGeneralPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		sessionPanel = new FieldEditorPanel( true );
		sessionPanel.setBorder( BorderFactory.createTitledBorder("Session") );
		sessionPanel.addRow( TOTAL_SHIPS_DEFEATED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_BEACONS, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_SCRAP, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_CREW_HIRED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( DLC, FieldEditorPanel.ContentType.BOOLEAN );
		sessionPanel.addRow( DIFFICULTY, FieldEditorPanel.ContentType.COMBO );
		sessionPanel.addRow( TOP_BETA, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.getInt(TOP_BETA).setDocument( new RegexDocument("-?[0-9]*") );
		sessionPanel.addBlankRow();
		sessionPanel.addFillRow();

		sessionPanel.getBoolean(DLC).addMouseListener( new StatusbarMouseListener(frame, "Toggle FTL:AE content (changing to false may be dangerous).") );
		sessionPanel.getCombo(DIFFICULTY).addMouseListener( new StatusbarMouseListener(frame, "Difficulty (FTL 1.01-1.03.3 did not have HARD).") );
		sessionPanel.getInt(TOP_BETA).addMouseListener( new StatusbarMouseListener(frame, "Unknown session field. Always 0?") );

		cargoPanel = new FieldEditorPanel( false );
		cargoPanel.setBorder( BorderFactory.createTitledBorder("Cargo") );

		for (int i=0; i < cargoSlots.length; i++) {
			cargoPanel.addRow( cargoSlots[i], FieldEditorPanel.ContentType.COMBO );
		}
		cargoPanel.addBlankRow();
		cargoPanel.addFillRow();

		sectorPanel = new FieldEditorPanel( true );
		sectorPanel.setBorder( BorderFactory.createTitledBorder("Sector") );
		sectorPanel.addRow( SECTOR_TREE_SEED, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(SECTOR_TREE_SEED).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( SECTOR_NUMBER, FieldEditorPanel.ContentType.SLIDER );
		sectorPanel.getSlider(SECTOR_NUMBER).setMaximum(0);
		sectorPanel.addBlankRow();
		sectorPanel.addRow( SECTOR_LAYOUT_SEED, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(SECTOR_LAYOUT_SEED).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_FLEET_OFFSET, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_FLEET_OFFSET).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_FLEET_FUDGE, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_FLEET_FUDGE).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( REBEL_PURSUIT_MOD, FieldEditorPanel.ContentType.INTEGER );
		sectorPanel.getInt(REBEL_PURSUIT_MOD).setDocument( new RegexDocument("-?[0-9]*") );
		sectorPanel.addRow( HIDDEN_SECTOR, FieldEditorPanel.ContentType.BOOLEAN );
		sectorPanel.addRow( HAZARDS_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );
		sectorPanel.addBlankRow();
		sectorPanel.addFillRow();

		sectorPanel.getInt(SECTOR_TREE_SEED).addMouseListener( new StatusbarMouseListener(frame, "A per-game constant that seeds the random generation of the sector tree (dangerous). Roll back to sector 1 if you change this!") );
		sectorPanel.getSlider(SECTOR_NUMBER).addMouseListener( new StatusbarMouseListener(frame, "Roll back to a previous sector.") );
		sectorPanel.getInt(SECTOR_LAYOUT_SEED).addMouseListener( new StatusbarMouseListener(frame, "A per-sector constant that seeds the random generation of the map, events, etc. (potentially dangerous).") );
		sectorPanel.getInt(REBEL_FLEET_OFFSET).addMouseListener( new StatusbarMouseListener(frame, "A large negative var (-750,-250,...,-n*25, approaching 0) + fudge = the fleet circle's leading edge.") );
		sectorPanel.getInt(REBEL_FLEET_FUDGE).addMouseListener( new StatusbarMouseListener(frame, "A random per-sector constant (usually around 75-310) + offset = the fleet circle's edge.") );
		sectorPanel.getInt(REBEL_PURSUIT_MOD).addMouseListener( new StatusbarMouseListener(frame, "Delay/alert the fleet, changing the warning zone thickness (e.g., merc distraction = -2).") );
		sectorPanel.getBoolean(HIDDEN_SECTOR).addMouseListener( new StatusbarMouseListener(frame, "Sector #?: Hidden Crystal Worlds. At the exit, you won't get to choose the next sector.") );
		sectorPanel.getBoolean(HAZARDS_VISIBLE).addMouseListener( new StatusbarMouseListener(frame, "Show hazards on the current sector map.") );

		bossPanel = new FieldEditorPanel( true );
		bossPanel.setBorder( BorderFactory.createTitledBorder("Boss") );
		bossPanel.addRow( FLAGSHIP, FieldEditorPanel.ContentType.BOOLEAN );
		bossPanel.addRow( FLAGSHIP_HOP, FieldEditorPanel.ContentType.SLIDER );
		bossPanel.getSlider(FLAGSHIP_HOP).setMaximum( 10 );
		bossPanel.addRow( FLAGSHIP_MOVING, FieldEditorPanel.ContentType.BOOLEAN );
		bossPanel.addRow( FLAGSHIP_BASE_TURNS, FieldEditorPanel.ContentType.INTEGER );
		bossPanel.addRow( FLAGSHIP_NEARBY, FieldEditorPanel.ContentType.BOOLEAN );
		bossPanel.addBlankRow();
		bossPanel.addRow( FLAGSHIP_ALPHA, FieldEditorPanel.ContentType.INTEGER );
		bossPanel.getInt(FLAGSHIP_ALPHA).setDocument( new RegexDocument("-?[0-9]*") );
		bossPanel.addRow( FLAGSHIP_GAMMA, FieldEditorPanel.ContentType.INTEGER );
		bossPanel.getInt(FLAGSHIP_GAMMA).setDocument( new RegexDocument("-?[0-9]*") );
		bossPanel.addRow( FLAGSHIP_DELTA, FieldEditorPanel.ContentType.INTEGER );
		bossPanel.getInt(FLAGSHIP_DELTA).setDocument( new RegexDocument("-?[0-9]*") );
		bossPanel.addBlankRow();
		bossPanel.addFillRow();

		bossPanel.getBoolean(FLAGSHIP).addMouseListener( new StatusbarMouseListener(frame, "Toggle the rebel flagship on the map. (FTL 1.01-1.03.3: Instant loss if not in sector 8)") );
		bossPanel.getSlider(FLAGSHIP_HOP).addMouseListener( new StatusbarMouseListener(frame, "The flagship is at it's Nth random beacon. (0-based) Sector layout seed affects where that will be. (FTL 1.01-1.03.3: Instant loss may occur beyond 4)") );
		bossPanel.getBoolean(FLAGSHIP_MOVING).addMouseListener( new StatusbarMouseListener(frame, "The flagship is moving from its current beacon toward the next.") );
		bossPanel.getInt(FLAGSHIP_BASE_TURNS).addMouseListener( new StatusbarMouseListener(frame, "Number of turns the flagship has started at the fed base. Instant loss will occur beyond 3.") );
		bossPanel.getBoolean(FLAGSHIP_NEARBY).addMouseListener( new StatusbarMouseListener(frame, "True if nearby ship is the flagship. Only set when a nearby ship is present.") );
		bossPanel.getInt(FLAGSHIP_ALPHA).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Last seen rebel flagship stage, or 0. Redundant?") );
		bossPanel.getInt(FLAGSHIP_GAMMA).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Varies during first and second rebel flagship stages, or 0.") );
		bossPanel.getInt(FLAGSHIP_DELTA).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Surge ticks during all rebel flagship stages, or 0. Goal varies. No visible effect during first stage.") );

		bossPanel.getBoolean(FLAGSHIP).addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean flagshipVisible = ( e.getStateChange() == ItemEvent.SELECTED );
				if ( !flagshipVisible ) {
					bossPanel.getSlider(FLAGSHIP_HOP).setValue( 0 );
					bossPanel.getBoolean(FLAGSHIP_MOVING).setSelected( false );
					bossPanel.getInt(FLAGSHIP_BASE_TURNS).setText( "0" );
				}
				bossPanel.getSlider(FLAGSHIP_HOP).setEnabled( flagshipVisible );
				bossPanel.getBoolean(FLAGSHIP_MOVING).setEnabled( flagshipVisible );
				bossPanel.getInt(FLAGSHIP_BASE_TURNS).setEnabled( flagshipVisible );
			}
		});
		forceCheckBox( bossPanel.getBoolean(FLAGSHIP), false );

		envPanel = new FieldEditorPanel( true );
		envPanel.setBorder( BorderFactory.createTitledBorder("Environment") );
		envPanel.addRow( ENV_RED_GIANT_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_PULSAR_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_PDS_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_VULN, FieldEditorPanel.ContentType.COMBO );
		envPanel.addBlankRow();
		envPanel.addRow( ENV_ASTEROID_FIELD, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_ASTEROID_ALPHA, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_ASTEROID_ALPHA).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_ASTEROID_STRAY_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_ASTEROID_STRAY_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_ASTEROID_GAMMA, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_ASTEROID_GAMMA).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_ASTEROID_BKG_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_ASTEROID_BKG_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_ASTEROID_TARGET, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_ASTEROID_TARGET).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addBlankRow();
		envPanel.addRow( ENV_FLARE_FADE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_FLARE_FADE_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_HAVOC_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_HAVOC_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addRow( ENV_PDS_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt(ENV_PDS_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		envPanel.addBlankRow();
		envPanel.addFillRow();

		envPanel.getBoolean(ENV_RED_GIANT_PRESENT).addMouseListener( new StatusbarMouseListener(frame, "Toggle the presence of a red giant hazard.") );
		envPanel.getBoolean(ENV_PULSAR_PRESENT).addMouseListener( new StatusbarMouseListener(frame, "Toggle the presence of a pulsar hazard.") );
		envPanel.getBoolean(ENV_PDS_PRESENT).addMouseListener( new StatusbarMouseListener(frame, "Toggle the presence of a PDS hazard.") );
		envPanel.getCombo(ENV_VULN).addMouseListener( new StatusbarMouseListener(frame, "Which ship the environment will affect (PDS only).") );
		envPanel.getBoolean(ENV_ASTEROID_FIELD).addMouseListener( new StatusbarMouseListener(frame, "Toggle the presence of asteroids.") );
		envPanel.getInt(ENV_ASTEROID_ALPHA).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		envPanel.getInt(ENV_ASTEROID_STRAY_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		envPanel.getInt(ENV_ASTEROID_GAMMA).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		envPanel.getInt(ENV_ASTEROID_BKG_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		envPanel.getInt(ENV_ASTEROID_TARGET).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		envPanel.getInt(ENV_FLARE_FADE_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Active during red giant/pulsar flares, when the entire screen fades.") );
		envPanel.getInt(ENV_HAVOC_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Red giant/pulsar/PDS only. Goal varies.") );
		envPanel.getInt(ENV_PDS_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );

		envPanel.getBoolean(ENV_ASTEROID_FIELD).addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean asteroidsPresent = ( e.getStateChange() == ItemEvent.SELECTED );
				if ( !asteroidsPresent ) {
					envPanel.getInt(ENV_ASTEROID_ALPHA).setText( "0" );
					envPanel.getInt(ENV_ASTEROID_STRAY_TICKS).setText( "0" );
					envPanel.getInt(ENV_ASTEROID_GAMMA).setText( "0" );
					envPanel.getInt(ENV_ASTEROID_BKG_TICKS).setText( "0" );
					envPanel.getInt(ENV_ASTEROID_TARGET).setText( "0" );
				}
				envPanel.getInt(ENV_ASTEROID_ALPHA).setEnabled( asteroidsPresent );
				envPanel.getInt(ENV_ASTEROID_STRAY_TICKS).setEnabled( asteroidsPresent );
				envPanel.getInt(ENV_ASTEROID_GAMMA).setEnabled( asteroidsPresent );
				envPanel.getInt(ENV_ASTEROID_BKG_TICKS).setEnabled( asteroidsPresent );
				envPanel.getInt(ENV_ASTEROID_TARGET).setEnabled( asteroidsPresent );
			}
		});
		forceCheckBox( envPanel.getBoolean(ENV_ASTEROID_FIELD), false );

		aiPanel = new FieldEditorPanel( true );
		aiPanel.setBorder( BorderFactory.createTitledBorder("Nearby Ship AI") );
		aiPanel.addRow( AI_SURRENDERED, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_ESCAPING, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_DESTROYED, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_SURRENDER_THRESHOLD, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.getInt(AI_SURRENDER_THRESHOLD).setDocument( new RegexDocument("-?[0-9]*") );
		aiPanel.addRow( AI_ESCAPE_THRESHOLD, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.getInt(AI_ESCAPE_THRESHOLD).setDocument( new RegexDocument("-?[0-9]*") );
		aiPanel.addRow( AI_ESCAPE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_STALEMATE, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_STALEMATE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_BOARDING_ATTEMPTS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_BOARDERS_NEEDED, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addBlankRow();
		aiPanel.addFillRow();

		aiPanel.getBoolean(AI_SURRENDERED).addMouseListener( new StatusbarMouseListener(frame, "Toggle whether surrender was offered.") );
		aiPanel.getBoolean(AI_ESCAPING).addMouseListener( new StatusbarMouseListener(frame, "Toggle whether the nearby ship's FTL is charging to escape.") );
		aiPanel.getBoolean(AI_DESTROYED).addMouseListener( new StatusbarMouseListener(frame, "Toggle whether the nearby ship was destroyed.") );
		aiPanel.getInt(AI_SURRENDER_THRESHOLD).addMouseListener( new StatusbarMouseListener(frame, "Hull amount that will cause the ship to surrender (may be negative).") );
		aiPanel.getInt(AI_ESCAPE_THRESHOLD).addMouseListener( new StatusbarMouseListener(frame, "Hull amount that will cause the ship to flee (may be negative).") );
		aiPanel.getInt(AI_ESCAPE_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Time elapsed while waiting for the nearby ship's FTL to charge (Decrements to 0).") );
		aiPanel.getBoolean(AI_STALEMATE).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		aiPanel.getInt(AI_STALEMATE_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		aiPanel.getInt(AI_BOARDING_ATTEMPTS).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		aiPanel.getInt(AI_BOARDERS_NEEDED).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );

		unknownsPanel = new FieldEditorPanel( true );
		unknownsPanel.setBorder( BorderFactory.createTitledBorder("Top-Level Unknowns") );
		unknownsPanel.addRow( TOP_WAITING, FieldEditorPanel.ContentType.BOOLEAN );
		unknownsPanel.addRow( TOP_WAIT_EVENT_SEED, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt(TOP_WAIT_EVENT_SEED).setDocument( new RegexDocument("-?[0-9]*") );
		unknownsPanel.addRow( TOP_EPSILON, FieldEditorPanel.ContentType.STRING );
		unknownsPanel.addRow( TOP_KAPPA, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt(TOP_KAPPA).setDocument( new RegexDocument("-?[0-9]*") );
		unknownsPanel.addRow( TOP_MU, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt(TOP_MU).setDocument( new RegexDocument("-?[0-9]*") );
		unknownsPanel.addBlankRow();
		unknownsPanel.addRow( TOP_NU, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt(TOP_NU).setDocument( new RegexDocument("-?[0-9]*") );
		unknownsPanel.addRow( TOP_XI, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt(TOP_XI).setDocument( new RegexDocument("-?[0-9]*") );
		unknownsPanel.addRow( TOP_AUTOFIRE, FieldEditorPanel.ContentType.BOOLEAN );
		unknownsPanel.addBlankRow();
		unknownsPanel.addFillRow();

		unknownsPanel.getBoolean(TOP_WAITING).addMouseListener( new StatusbarMouseListener(frame, "Toggle whether any current random event is a wait event. (Set a wait seed!)") );
		unknownsPanel.getInt(TOP_WAIT_EVENT_SEED).addMouseListener( new StatusbarMouseListener(frame, "Seed for random wait events. (-1 when not set. Waiting without a seed crashes FTL.)") );
		unknownsPanel.getString(TOP_EPSILON).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Rare eventId. Related to waiting?") );
		unknownsPanel.getInt(TOP_KAPPA).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Maybe flagship-related?") );
		unknownsPanel.getInt(TOP_MU).addMouseListener( new StatusbarMouseListener(frame, "Unknown.") );
		unknownsPanel.getInt(TOP_NU).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Probably a seed related to the player ship.") );
		unknownsPanel.getInt(TOP_XI).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Probably a seed related to the nearby ship, when one is present.") );
		unknownsPanel.getBoolean(TOP_AUTOFIRE).addMouseListener( new StatusbarMouseListener(frame, "Toggle autofire.") );

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

		thisC.gridx++;
		this.add( bossPanel, thisC );

		thisC.gridx = 0;
		thisC.gridy++;
		this.add( envPanel, thisC );

		thisC.gridx++;
		this.add( aiPanel, thisC );

		thisC.gridx = 0;
		thisC.gridy++;
		this.add( unknownsPanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		setGameState( null );
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		sectorPanel.getSlider(SECTOR_NUMBER).setMinimum( 0 );
		sectorPanel.getSlider(SECTOR_NUMBER).setMaximum( 0 );

		sessionPanel.reset();
		cargoPanel.reset();
		sectorPanel.reset();
		bossPanel.reset();
		envPanel.reset();
		aiPanel.reset();
		unknownsPanel.reset();

		if ( gameState != null ) {
			SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

			for ( Difficulty d : Difficulty.values() ) {
				sessionPanel.getCombo(DIFFICULTY).addItem( d );
			}

			sessionPanel.setIntAndReminder( TOTAL_SHIPS_DEFEATED, gameState.getTotalShipsDefeated() );
			sessionPanel.setIntAndReminder( TOTAL_BEACONS, gameState.getTotalBeaconsExplored() );
			sessionPanel.setIntAndReminder( TOTAL_SCRAP, gameState.getTotalScrapCollected() );
			sessionPanel.setIntAndReminder( TOTAL_CREW_HIRED, gameState.getTotalCrewHired() );
			sessionPanel.setBoolAndReminder( DLC, gameState.isDLCEnabled() );
			sessionPanel.setComboAndReminder( DIFFICULTY, gameState.getDifficulty() );
			sessionPanel.setIntAndReminder( TOP_BETA, gameState.getUnknownBeta() );

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

			sectorPanel.setIntAndReminder( SECTOR_TREE_SEED, gameState.getSectorTreeSeed() );
			sectorPanel.getSlider(SECTOR_NUMBER).setMaximum( gameState.getSectorNumber()+1 );
			sectorPanel.getSlider(SECTOR_NUMBER).setMinimum( 1 );
			sectorPanel.setSliderAndReminder( SECTOR_NUMBER, gameState.getSectorNumber()+1 );
			sectorPanel.setIntAndReminder( SECTOR_LAYOUT_SEED, gameState.getSectorLayoutSeed() );
			sectorPanel.setIntAndReminder( REBEL_FLEET_OFFSET, gameState.getRebelFleetOffset() );
			sectorPanel.setIntAndReminder( REBEL_FLEET_FUDGE, gameState.getRebelFleetFudge() );
			sectorPanel.setIntAndReminder( REBEL_PURSUIT_MOD, gameState.getRebelPursuitMod() );
			sectorPanel.setBoolAndReminder( HIDDEN_SECTOR, gameState.isSectorHiddenCrystalWorlds() );
			sectorPanel.setBoolAndReminder( HAZARDS_VISIBLE, gameState.areSectorHazardsVisible() );

			forceCheckBox( bossPanel.getBoolean(FLAGSHIP), false );

			bossPanel.setBoolAndReminder( FLAGSHIP, gameState.isRebelFlagshipVisible() );
			bossPanel.setSliderAndReminder( FLAGSHIP_HOP, gameState.getRebelFlagshipHop() );
			bossPanel.setBoolAndReminder( FLAGSHIP_MOVING, gameState.isRebelFlagshipMoving() );
			bossPanel.setIntAndReminder( FLAGSHIP_BASE_TURNS, gameState.getRebelFlagshipBaseTurns() );
			bossPanel.setBoolAndReminder( FLAGSHIP_NEARBY, gameState.isRebelFlagshipNearby() );

			SavedGameParser.RebelFlagshipState flagship = gameState.getRebelFlagshipState();
			flagshipEnabled = ( flagship != null );
			bossPanel.getInt(FLAGSHIP_ALPHA).setEnabled( flagshipEnabled );
			bossPanel.getInt(FLAGSHIP_GAMMA).setEnabled( flagshipEnabled );
			bossPanel.getInt(FLAGSHIP_DELTA).setEnabled( flagshipEnabled );

			if ( flagshipEnabled ) {
				bossPanel.setIntAndReminder( FLAGSHIP_ALPHA, flagship.getUnknownAlpha() );
				bossPanel.setIntAndReminder( FLAGSHIP_GAMMA, flagship.getUnknownGamma() );
				bossPanel.setIntAndReminder( FLAGSHIP_DELTA, flagship.getUnknownDelta() );
			}

			for ( SavedGameParser.HazardVulnerability h : SavedGameParser.HazardVulnerability.values() ) {
				envPanel.getCombo(ENV_VULN).addItem( h );
			}

			SavedGameParser.EnvironmentState env = gameState.getEnvironment();
			envEnabled = ( env != null );
			envPanel.getBoolean(ENV_RED_GIANT_PRESENT).setEnabled( envEnabled );
			envPanel.getBoolean(ENV_PULSAR_PRESENT).setEnabled( envEnabled );
			envPanel.getBoolean(ENV_PDS_PRESENT).setEnabled( envEnabled );
			envPanel.getCombo(ENV_VULN).setEnabled( envEnabled );
			envPanel.getBoolean(ENV_ASTEROID_FIELD).setEnabled( envEnabled );
			envPanel.getInt(ENV_ASTEROID_ALPHA).setEnabled( envEnabled );
			envPanel.getInt(ENV_ASTEROID_STRAY_TICKS).setEnabled( envEnabled );
			envPanel.getInt(ENV_ASTEROID_GAMMA).setEnabled( envEnabled );
			envPanel.getInt(ENV_ASTEROID_BKG_TICKS).setEnabled( envEnabled );
			envPanel.getInt(ENV_ASTEROID_TARGET).setEnabled( envEnabled );
			envPanel.getInt(ENV_FLARE_FADE_TICKS).setEnabled( envEnabled );
			envPanel.getInt(ENV_HAVOC_TICKS).setEnabled( envEnabled );
			envPanel.getInt(ENV_PDS_TICKS).setEnabled( envEnabled );

			forceCheckBox( envPanel.getBoolean(ENV_ASTEROID_FIELD), false );

			if ( envEnabled ) {
				envPanel.setBoolAndReminder( ENV_RED_GIANT_PRESENT, env.isRedGiantPresent() );
				envPanel.setBoolAndReminder( ENV_PULSAR_PRESENT, env.isPulsarPresent() );
				envPanel.setBoolAndReminder( ENV_PDS_PRESENT, env.isPDSPresent() );
				envPanel.setComboAndReminder( ENV_VULN, env.getVulnerableShips() );

				SavedGameParser.AsteroidFieldState asteroidField = env.getAsteroidField();
				boolean asteroidsPresent = ( asteroidField != null );
				envPanel.getBoolean(ENV_ASTEROID_FIELD).setSelected( asteroidsPresent );

				if ( asteroidsPresent ) {
					envPanel.setIntAndReminder( ENV_ASTEROID_ALPHA, asteroidField.getUnknownAlpha() );
					envPanel.setIntAndReminder( ENV_ASTEROID_STRAY_TICKS, asteroidField.getStrayRockTicks() );
					envPanel.setIntAndReminder( ENV_ASTEROID_GAMMA, asteroidField.getUnknownGamma() );
					envPanel.setIntAndReminder( ENV_ASTEROID_BKG_TICKS, asteroidField.getBgDriftTicks() );
					envPanel.setIntAndReminder( ENV_ASTEROID_TARGET, asteroidField.getCurrentTarget() );
				}

				envPanel.setIntAndReminder( ENV_FLARE_FADE_TICKS, env.getSolarFlareFadeTicks() );
				envPanel.setIntAndReminder( ENV_HAVOC_TICKS, env.getHavocTicks() );
				envPanel.setIntAndReminder( ENV_PDS_TICKS, env.getPDSTicks() );
			}

			SavedGameParser.NearbyShipAIState ai = gameState.getNearbyShipAI();
			aiEnabled = ( ai != null );
			aiPanel.getBoolean(AI_SURRENDERED).setEnabled( aiEnabled );
			aiPanel.getBoolean(AI_ESCAPING).setEnabled( aiEnabled );
			aiPanel.getBoolean(AI_DESTROYED).setEnabled( aiEnabled );
			aiPanel.getInt(AI_SURRENDER_THRESHOLD).setEnabled( aiEnabled );
			aiPanel.getInt(AI_ESCAPE_THRESHOLD).setEnabled( aiEnabled );
			aiPanel.getInt(AI_ESCAPE_TICKS).setEnabled( aiEnabled );
			aiPanel.getBoolean(AI_STALEMATE).setEnabled( aiEnabled );
			aiPanel.getInt(AI_STALEMATE_TICKS).setEnabled( aiEnabled );
			aiPanel.getInt(AI_BOARDING_ATTEMPTS).setEnabled( aiEnabled );
			aiPanel.getInt(AI_BOARDERS_NEEDED).setEnabled( aiEnabled );

			if ( aiEnabled ) {
				aiPanel.setBoolAndReminder( AI_SURRENDERED, ai.hasSurrendered() );
				aiPanel.setBoolAndReminder( AI_ESCAPING, ai.isEscaping() );
				aiPanel.setBoolAndReminder( AI_DESTROYED, ai.isDestroyed() );
				aiPanel.setIntAndReminder( AI_SURRENDER_THRESHOLD, ai.getSurrenderThreshold() );
				aiPanel.setIntAndReminder( AI_ESCAPE_THRESHOLD, ai.getEscapeThreshold() );
				aiPanel.setIntAndReminder( AI_ESCAPE_TICKS, ai.getEscapeTicks() );
				aiPanel.setBoolAndReminder( AI_STALEMATE, ai.isStalemateTriggered() );
				aiPanel.setIntAndReminder( AI_STALEMATE_TICKS, ai.getStalemateTicks() );
				aiPanel.setIntAndReminder( AI_BOARDING_ATTEMPTS, ai.getBoardingAttempts() );
				aiPanel.setIntAndReminder( AI_BOARDERS_NEEDED, ai.getBoardersNeeded() );
			}

			unknownsPanel.setBoolAndReminder( TOP_WAITING, gameState.isWaiting() );
			unknownsPanel.setIntAndReminder( TOP_WAIT_EVENT_SEED, gameState.getWaitEventSeed() );
			unknownsPanel.setStringAndReminder( TOP_EPSILON, gameState.getUnknownEpsilon() );
			unknownsPanel.setIntAndReminder( TOP_KAPPA, gameState.getUnknownKappa() );
			unknownsPanel.setIntAndReminder( TOP_MU, gameState.getUnknownMu() );
			unknownsPanel.setIntAndReminder( TOP_NU, gameState.getUnknownNu() );
			unknownsPanel.setIntAndReminder( TOP_XI, (gameState.getUnknownXi() != null ? gameState.getUnknownXi().intValue() : 0) );
			unknownsPanel.setBoolAndReminder( TOP_AUTOFIRE, gameState.getAutofire() );
		}

		this.repaint();
	}

	@SuppressWarnings("unchecked")
	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();

		try { gameState.setTotalShipsDefeated( sessionPanel.parseInt(TOTAL_SHIPS_DEFEATED) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalBeaconsExplored( sessionPanel.parseInt(TOTAL_BEACONS) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalScrapCollected( sessionPanel.parseInt(TOTAL_SCRAP) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalCrewHired( sessionPanel.parseInt(TOTAL_CREW_HIRED) ); }
		catch ( NumberFormatException e ) {}

		gameState.setDLCEnabled( sessionPanel.getBoolean(DLC).isSelected() );

		Object diffObj = sessionPanel.getCombo(DIFFICULTY).getSelectedItem();
		gameState.setDifficulty( (Difficulty)diffObj );

		try { gameState.setUnknownBeta( sessionPanel.parseInt(TOP_BETA) ); }
		catch ( NumberFormatException e ) {}

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

		try { gameState.setSectorTreeSeed( sectorPanel.parseInt(SECTOR_TREE_SEED) ); }
		catch ( NumberFormatException e ) {}

		// Set the current sector, and unvisit any tree breadcrumbs beyond it.
		int oneBasedSectorNum = sectorPanel.getSlider(SECTOR_NUMBER).getValue();
		List<Boolean> sectorList = gameState.getSectorList();
		gameState.setSectorNumber( oneBasedSectorNum-1 );
		for (int i=0, n=0; i < sectorList.size(); i++) {
			if ( sectorList.get(i).booleanValue() == true ) {
				n++;
				if ( n > oneBasedSectorNum )
					gameState.setSectorVisited( i, false );
			}
		}

		try { gameState.setSectorLayoutSeed( sectorPanel.parseInt(SECTOR_LAYOUT_SEED) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setRebelFleetOffset( sectorPanel.parseInt(REBEL_FLEET_OFFSET) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setRebelFleetFudge( sectorPanel.parseInt(REBEL_FLEET_FUDGE) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setRebelPursuitMod( sectorPanel.parseInt(REBEL_PURSUIT_MOD) ); }
		catch ( NumberFormatException e ) {}

		gameState.setSectorIsHiddenCrystalWorlds( sectorPanel.getBoolean(HIDDEN_SECTOR).isSelected() );
		gameState.setSectorHazardsVisible( sectorPanel.getBoolean(HAZARDS_VISIBLE).isSelected() );

		boolean flagshipVisible = bossPanel.getBoolean(FLAGSHIP).isSelected();

		gameState.setRebelFlagshipVisible( flagshipVisible );
		if ( flagshipVisible ) {
			gameState.setRebelFlagshipHop( bossPanel.getSlider(FLAGSHIP_HOP).getValue() );
			gameState.setRebelFlagshipMoving( bossPanel.getBoolean(FLAGSHIP_MOVING).isSelected() );

			try { gameState.setRebelFlagshipBaseTurns( bossPanel.parseInt(FLAGSHIP_BASE_TURNS) ); }
			catch ( NumberFormatException e ) {}
		}
		else {
			gameState.setRebelFlagshipHop( 0 );
			gameState.setRebelFlagshipMoving( false );
			gameState.setRebelFlagshipBaseTurns( 0 );
		}
		gameState.setRebelFlagshipNearby( bossPanel.getBoolean(FLAGSHIP_NEARBY).isSelected() );

		SavedGameParser.RebelFlagshipState flagship = gameState.getRebelFlagshipState();
		if ( flagship != null && flagshipEnabled ) {
			try { flagship.setUnknownAlpha( bossPanel.parseInt(FLAGSHIP_ALPHA) ); }
			catch ( NumberFormatException e ) {}

			try { flagship.setUnknownGamma( bossPanel.parseInt(FLAGSHIP_GAMMA) ); }
			catch ( NumberFormatException e ) {}

			try { flagship.setUnknownDelta( bossPanel.parseInt(FLAGSHIP_DELTA) ); }
			catch ( NumberFormatException e ) {}
		}

		SavedGameParser.EnvironmentState env = gameState.getEnvironment();
		if ( env != null && envEnabled ) {
			env.setRedGiantPresent( envPanel.getBoolean(ENV_RED_GIANT_PRESENT).isSelected() );
			env.setPulsarPresent( envPanel.getBoolean(ENV_PULSAR_PRESENT).isSelected() );
			env.setPDSPresent( envPanel.getBoolean(ENV_PDS_PRESENT).isSelected() );

			Object vulnObj = envPanel.getCombo(ENV_VULN).getSelectedItem();
			env.setVulnerableShips( (SavedGameParser.HazardVulnerability)vulnObj );

			SavedGameParser.AsteroidFieldState asteroidField = null;
			if ( envPanel.getBoolean(ENV_ASTEROID_FIELD).isSelected() ) {
				asteroidField = new SavedGameParser.AsteroidFieldState();

				try { asteroidField.setUnknownAlpha( envPanel.parseInt(ENV_ASTEROID_ALPHA) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setStrayRockTicks( envPanel.parseInt(ENV_ASTEROID_STRAY_TICKS) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setUnknownGamma( envPanel.parseInt(ENV_ASTEROID_GAMMA) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setBgDriftTicks( envPanel.parseInt(ENV_ASTEROID_BKG_TICKS) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setCurrentTarget( envPanel.parseInt(ENV_ASTEROID_TARGET) ); }
				catch ( NumberFormatException e ) {}
			}
			env.setAsteroidField( asteroidField );

			try { env.setSolarFlareFadeTicks( envPanel.parseInt(ENV_FLARE_FADE_TICKS) ); }
			catch ( NumberFormatException e ) {}

			try { env.setHavocTicks( envPanel.parseInt(ENV_HAVOC_TICKS) ); }
			catch ( NumberFormatException e ) {}

			try { env.setPDSTicks( envPanel.parseInt(ENV_PDS_TICKS) ); }
			catch ( NumberFormatException e ) {}
		}

		SavedGameParser.NearbyShipAIState ai = gameState.getNearbyShipAI();
		if ( ai != null && aiEnabled ) {
			ai.setSurrendered( aiPanel.getBoolean(AI_SURRENDERED).isSelected() );
			ai.setEscaping( aiPanel.getBoolean(AI_ESCAPING).isSelected() );
			ai.setDestroyed( aiPanel.getBoolean(AI_DESTROYED).isSelected() );

			try { ai.setSurrenderThreshold( aiPanel.parseInt(AI_SURRENDER_THRESHOLD) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setEscapeThreshold( aiPanel.parseInt(AI_ESCAPE_THRESHOLD) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setEscapeTicks( aiPanel.parseInt(AI_ESCAPE_TICKS) ); }
			catch ( NumberFormatException e ) {}

			ai.setStalemateTriggered( aiPanel.getBoolean(AI_STALEMATE).isSelected() );

			try { ai.setStalemateTicks( aiPanel.parseInt(AI_STALEMATE_TICKS) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setBoardingAttempts( aiPanel.parseInt(AI_BOARDING_ATTEMPTS) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setBoardersNeeded( aiPanel.parseInt(AI_BOARDERS_NEEDED) ); }
			catch ( NumberFormatException e ) {}
		}

		gameState.setWaiting( unknownsPanel.getBoolean(TOP_WAITING).isSelected() );

		try { gameState.setWaitEventSeed( unknownsPanel.parseInt(TOP_WAIT_EVENT_SEED) ); }
		catch ( NumberFormatException e ) {}

		gameState.setUnknownEpsilon( unknownsPanel.getString(TOP_EPSILON).getText() );

		try { gameState.setUnknownKappa( unknownsPanel.parseInt(TOP_KAPPA) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setUnknownMu( unknownsPanel.parseInt(TOP_MU) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setUnknownNu( unknownsPanel.parseInt(TOP_NU) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setUnknownXi( new Integer( unknownsPanel.parseInt(TOP_XI) ) ); }
		catch ( NumberFormatException e ) {}

		gameState.setAutofire( unknownsPanel.getBoolean(TOP_AUTOFIRE).isSelected() );
	}

	/**
	 * Sets a JCheckBox selection and triggers all ItemListeners.
	 */
	private void forceCheckBox( JCheckBox box, boolean selected ) {
		if ( box.isSelected() != selected ) {
			box.setSelected( selected );

			// No need to trigger listeners, since it really changed.
		}
		else {
			box.setSelected( selected );

			for ( ItemListener l : box.getListeners( ItemListener.class ) ) {
				ItemEvent evt = new ItemEvent( box, ItemEvent.ITEM_STATE_CHANGED, box, (selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED) );
				l.itemStateChanged( evt );
			}
		}
	}
}
