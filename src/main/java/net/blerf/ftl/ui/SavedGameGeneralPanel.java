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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;


public class SavedGameGeneralPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger( SavedGameGeneralPanel.class );

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
	private static final String TOP_MU = "Mu?";
	private static final String TOP_NU = "Nu?";
	private static final String TOP_XI = "Xi?";
	private static final String TOP_AUTOFIRE = "Autofire";

	private static final String ENC_SHIP_EVENT_SEED = "Ship Event Seed";
	private static final String ENC_SURRENDER_EVENT = "Surrender Event";
	private static final String ENC_ESCAPE_EVENT = "Escape Event";
	private static final String ENC_DESTROYED_EVENT = "Destroyed Event";
	private static final String ENC_DEAD_CREW_EVENT = "Dead Crew Event";
	private static final String ENC_GOT_AWAY_EVENT = "Got Away Event";
	private static final String ENC_LAST_EVENT = "Last Event";
	private static final String ENC_TEXT = "Text";
	private static final String ENC_CREW_SEED = "Affected Crew Seed";
	private static final String ENC_CHOICES = "Last Event Choices";

	private FTLFrame frame;

	private FieldEditorPanel sessionPanel = null;
	private FieldEditorPanel cargoPanel = null;
	private FieldEditorPanel envPanel = null;
	private FieldEditorPanel aiPanel = null;
	private FieldEditorPanel unknownsPanel = null;
	private FieldEditorPanel encPanel = null;

	private boolean envEnabled = true;
	private boolean aiEnabled = true;
	private boolean encEnabled = true;


	public SavedGameGeneralPanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		sessionPanel = new FieldEditorPanel( true );
		sessionPanel.setBorder( BorderFactory.createTitledBorder( "Session" ) );
		sessionPanel.addRow( TOTAL_SHIPS_DEFEATED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_BEACONS, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_SCRAP, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( TOTAL_CREW_HIRED, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.addRow( DLC, FieldEditorPanel.ContentType.BOOLEAN );
		sessionPanel.addRow( DIFFICULTY, FieldEditorPanel.ContentType.COMBO );
		sessionPanel.addRow( TOP_BETA, FieldEditorPanel.ContentType.INTEGER );
		sessionPanel.getInt( TOP_BETA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		sessionPanel.addBlankRow();
		sessionPanel.addFillRow();

		sessionPanel.getBoolean( DLC ).setEnabled( false );

		sessionPanel.getBoolean( DLC ).addMouseListener( new StatusbarMouseListener( frame, "Toggle FTL:AE content (changing to false may be dangerous)." ) );
		sessionPanel.getCombo( DIFFICULTY ).addMouseListener( new StatusbarMouseListener( frame, "Difficulty (FTL 1.01-1.03.3 did not have HARD)." ) );
		sessionPanel.getInt( TOP_BETA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown session field. Always 0?" ) );

		cargoPanel = new FieldEditorPanel( false );
		cargoPanel.setBorder( BorderFactory.createTitledBorder( "Cargo" ) );

		for ( int i=0; i < cargoSlots.length; i++ ) {
			cargoPanel.addRow( cargoSlots[i], FieldEditorPanel.ContentType.COMBO );
		}
		cargoPanel.addBlankRow();
		cargoPanel.addFillRow();

		envPanel = new FieldEditorPanel( true );
		envPanel.setBorder( BorderFactory.createTitledBorder( "Environment" ) );
		envPanel.addRow( ENV_RED_GIANT_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_PULSAR_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_PDS_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_VULN, FieldEditorPanel.ContentType.COMBO );
		envPanel.addBlankRow();
		envPanel.addRow( ENV_ASTEROID_FIELD, FieldEditorPanel.ContentType.BOOLEAN );
		envPanel.addRow( ENV_ASTEROID_ALPHA, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_ASTEROID_ALPHA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_ASTEROID_STRAY_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_ASTEROID_STRAY_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_ASTEROID_GAMMA, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_ASTEROID_GAMMA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_ASTEROID_BKG_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_ASTEROID_BKG_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_ASTEROID_TARGET, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_ASTEROID_TARGET ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addBlankRow();
		envPanel.addRow( ENV_FLARE_FADE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_FLARE_FADE_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_HAVOC_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_HAVOC_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addRow( ENV_PDS_TICKS, FieldEditorPanel.ContentType.INTEGER );
		envPanel.getInt( ENV_PDS_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		envPanel.addBlankRow();
		envPanel.addFillRow();

		envPanel.getBoolean( ENV_RED_GIANT_PRESENT ).addMouseListener( new StatusbarMouseListener( frame, "Toggle the presence of a red giant hazard." ) );
		envPanel.getBoolean( ENV_PULSAR_PRESENT ).addMouseListener( new StatusbarMouseListener( frame, "Toggle the presence of a pulsar hazard." ) );
		envPanel.getBoolean( ENV_PDS_PRESENT ).addMouseListener( new StatusbarMouseListener( frame, "Toggle the presence of a PDS hazard." ) );
		envPanel.getCombo( ENV_VULN ).addMouseListener( new StatusbarMouseListener( frame, "Which ship the environment will affect (PDS only)." ) );
		envPanel.getBoolean( ENV_ASTEROID_FIELD ).addMouseListener( new StatusbarMouseListener( frame, "Toggle the presence of asteroids." ) );
		envPanel.getInt( ENV_ASTEROID_ALPHA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		envPanel.getInt( ENV_ASTEROID_STRAY_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		envPanel.getInt( ENV_ASTEROID_GAMMA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		envPanel.getInt( ENV_ASTEROID_BKG_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		envPanel.getInt( ENV_ASTEROID_TARGET ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		envPanel.getInt( ENV_FLARE_FADE_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Active during red giant/pulsar flares, when the entire screen fades." ) );
		envPanel.getInt( ENV_HAVOC_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Red giant/pulsar/PDS only. Goal varies." ) );
		envPanel.getInt( ENV_PDS_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );

		envPanel.getBoolean( ENV_ASTEROID_FIELD ).addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean asteroidsPresent = ( e.getStateChange() == ItemEvent.SELECTED );
				if ( !asteroidsPresent ) {
					envPanel.getInt( ENV_ASTEROID_ALPHA ).setText( "0" );
					envPanel.getInt( ENV_ASTEROID_STRAY_TICKS ).setText( "0" );
					envPanel.getInt( ENV_ASTEROID_GAMMA ).setText( "0" );
					envPanel.getInt( ENV_ASTEROID_BKG_TICKS ).setText( "0" );
					envPanel.getInt( ENV_ASTEROID_TARGET ).setText( "0" );
				}
				envPanel.getInt( ENV_ASTEROID_ALPHA ).setEnabled( asteroidsPresent );
				envPanel.getInt( ENV_ASTEROID_STRAY_TICKS ).setEnabled( asteroidsPresent );
				envPanel.getInt( ENV_ASTEROID_GAMMA ).setEnabled( asteroidsPresent );
				envPanel.getInt( ENV_ASTEROID_BKG_TICKS ).setEnabled( asteroidsPresent );
				envPanel.getInt( ENV_ASTEROID_TARGET ).setEnabled( asteroidsPresent );
			}
		});
		forceCheckBox( envPanel.getBoolean( ENV_ASTEROID_FIELD ), false );

		aiPanel = new FieldEditorPanel( true );
		aiPanel.setBorder( BorderFactory.createTitledBorder( "Nearby Ship AI" ) );
		aiPanel.addRow( AI_SURRENDERED, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_ESCAPING, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_DESTROYED, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_SURRENDER_THRESHOLD, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.getInt( AI_SURRENDER_THRESHOLD ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		aiPanel.addRow( AI_ESCAPE_THRESHOLD, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.getInt( AI_ESCAPE_THRESHOLD ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		aiPanel.addRow( AI_ESCAPE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_STALEMATE, FieldEditorPanel.ContentType.BOOLEAN );
		aiPanel.addRow( AI_STALEMATE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_BOARDING_ATTEMPTS, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addRow( AI_BOARDERS_NEEDED, FieldEditorPanel.ContentType.INTEGER );
		aiPanel.addBlankRow();
		aiPanel.addFillRow();

		aiPanel.getBoolean( AI_SURRENDERED ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether surrender was offered." ) );
		aiPanel.getBoolean( AI_ESCAPING ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether the nearby ship's FTL is charging to escape." ) );
		aiPanel.getBoolean( AI_DESTROYED ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether the nearby ship was destroyed." ) );
		aiPanel.getInt( AI_SURRENDER_THRESHOLD ).addMouseListener( new StatusbarMouseListener( frame, "Hull amount that will cause the ship to surrender (may be negative)." ) );
		aiPanel.getInt( AI_ESCAPE_THRESHOLD ).addMouseListener( new StatusbarMouseListener( frame, "Hull amount that will cause the ship to flee (may be negative)." ) );
		aiPanel.getInt( AI_ESCAPE_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed while waiting for the nearby ship's FTL to charge (Decrements to 0)." ) );
		aiPanel.getBoolean( AI_STALEMATE ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		aiPanel.getInt( AI_STALEMATE_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		aiPanel.getInt( AI_BOARDING_ATTEMPTS ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		aiPanel.getInt( AI_BOARDERS_NEEDED ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );

		unknownsPanel = new FieldEditorPanel( true );
		unknownsPanel.setBorder( BorderFactory.createTitledBorder( "Top-Level Unknowns" ) );
		unknownsPanel.addRow( TOP_WAITING, FieldEditorPanel.ContentType.BOOLEAN );
		unknownsPanel.addRow( TOP_WAIT_EVENT_SEED, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt( TOP_WAIT_EVENT_SEED ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		unknownsPanel.addRow( TOP_EPSILON, FieldEditorPanel.ContentType.STRING );
		unknownsPanel.addRow( TOP_MU, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt( TOP_MU ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		unknownsPanel.addBlankRow();
		unknownsPanel.addRow( TOP_NU, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt( TOP_NU ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		unknownsPanel.addRow( TOP_XI, FieldEditorPanel.ContentType.INTEGER );
		unknownsPanel.getInt( TOP_XI ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		unknownsPanel.addRow( TOP_AUTOFIRE, FieldEditorPanel.ContentType.BOOLEAN );
		unknownsPanel.addBlankRow();
		unknownsPanel.addFillRow();

		unknownsPanel.getBoolean( TOP_WAITING ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether any current random event is a wait event. (Set a wait seed!)" ) );
		unknownsPanel.getInt( TOP_WAIT_EVENT_SEED ).addMouseListener( new StatusbarMouseListener( frame, "Seed for random wait events. (-1 when not set. Waiting without a seed crashes FTL.)" ) );
		unknownsPanel.getString( TOP_EPSILON ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Rare eventId. Related to waiting?" ) );
		unknownsPanel.getInt( TOP_MU ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Maybe event-related?" ) );
		unknownsPanel.getInt( TOP_NU ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Probably a seed related to the player ship." ) );
		unknownsPanel.getInt( TOP_XI ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Probably a seed related to the nearby ship, when one is present." ) );
		unknownsPanel.getBoolean( TOP_AUTOFIRE ).addMouseListener( new StatusbarMouseListener( frame, "Toggle autofire." ) );

		encPanel = new FieldEditorPanel( true );
		encPanel.setBorder( BorderFactory.createTitledBorder( "Encounter" ) );
		encPanel.addRow( ENC_SHIP_EVENT_SEED, FieldEditorPanel.ContentType.INTEGER );
		encPanel.addRow( ENC_SURRENDER_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_ESCAPE_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_DESTROYED_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_DEAD_CREW_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_GOT_AWAY_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_LAST_EVENT, FieldEditorPanel.ContentType.STRING );
		encPanel.addRow( ENC_TEXT, FieldEditorPanel.ContentType.STRING );
		encPanel.getString( ENC_TEXT ).setColumns( 10 );
		encPanel.addRow( ENC_CREW_SEED, FieldEditorPanel.ContentType.INTEGER );
		encPanel.getInt( ENC_CREW_SEED ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		encPanel.addRow( ENC_CHOICES, FieldEditorPanel.ContentType.STRING );
		encPanel.getString( ENC_CHOICES ).setDocument( new RegexDocument( "(?:-?[0-9]*)(?:,-?[0-9]*)*" ) );
		encPanel.addBlankRow();
		encPanel.addFillRow();

		encPanel.getInt( ENC_SHIP_EVENT_SEED ).addMouseListener( new StatusbarMouseListener( frame, "A seed for randomly generating a nearby ship. Copied from beacons on arrival." ) );
		encPanel.getString( ENC_LAST_EVENT ).addMouseListener( new StatusbarMouseListener( frame, "The last dynamically triggered event. (Sector's beacon events are initially static.)" ) );
		encPanel.getString( ENC_TEXT ).addMouseListener( new StatusbarMouseListener( frame, "Last situation-describing text shown in an event window. (From any event.)" ) );
		encPanel.getInt( ENC_CREW_SEED ).addMouseListener( new StatusbarMouseListener( frame, "A seed for randomly selecting crew. (-1 when not set.)" ) );
		encPanel.getString( ENC_CHOICES ).addMouseListener( new StatusbarMouseListener( frame, "Breadcrumbs tracking already-selected choices at each prompt. (0-based) Blank for fresh events." ) );

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.anchor = GridBagConstraints.NORTH;
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
		this.add( envPanel, thisC );

		thisC.gridx++;
		this.add( aiPanel, thisC );

		thisC.gridx = 0;
		thisC.gridy++;
		this.add( unknownsPanel, thisC );

		thisC.gridx++;
		this.add( encPanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		setGameState( null );
	}


	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		sessionPanel.reset();
		cargoPanel.reset();
		envPanel.reset();
		aiPanel.reset();
		unknownsPanel.reset();

		if ( gameState != null ) {
			SavedGameParser.ShipState shipState = gameState.getPlayerShipState();
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

			for ( Difficulty d : Difficulty.values() ) {
				sessionPanel.getCombo( DIFFICULTY ).addItem( d );
			}

			sessionPanel.setIntAndReminder( TOTAL_SHIPS_DEFEATED, gameState.getTotalShipsDefeated() );
			sessionPanel.setIntAndReminder( TOTAL_BEACONS, gameState.getTotalBeaconsExplored() );
			sessionPanel.setIntAndReminder( TOTAL_SCRAP, gameState.getTotalScrapCollected() );
			sessionPanel.setIntAndReminder( TOTAL_CREW_HIRED, gameState.getTotalCrewHired() );
			sessionPanel.setBoolAndReminder( DLC, gameState.isDLCEnabled() );
			sessionPanel.setComboAndReminder( DIFFICULTY, gameState.getDifficulty() );
			sessionPanel.setIntAndReminder( TOP_BETA, gameState.getUnknownBeta() );

			for ( int i=0; i < cargoSlots.length; i++ ) {
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "" );
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "Weapons" );
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "-------" );
				for ( WeaponBlueprint weaponBlueprint : DataManager.get().getWeapons().values() ) {
					cargoPanel.getCombo( cargoSlots[i] ).addItem( weaponBlueprint );
				}
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "" );
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "Drones" );
				cargoPanel.getCombo( cargoSlots[i] ).addItem( "------" );
				for ( DroneBlueprint droneBlueprint : DataManager.get().getDrones().values() ) {
					cargoPanel.getCombo( cargoSlots[i] ).addItem( droneBlueprint );
				}

				if ( gameState.getCargoIdList().size() > i ) {
					String cargoId = gameState.getCargoIdList().get( i );

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

			for ( SavedGameParser.HazardVulnerability h : SavedGameParser.HazardVulnerability.values() ) {
				envPanel.getCombo( ENV_VULN ).addItem( h );
			}

			SavedGameParser.EnvironmentState env = gameState.getEnvironment();
			envEnabled = ( env != null );
			envPanel.getBoolean( ENV_RED_GIANT_PRESENT ).setEnabled( envEnabled );
			envPanel.getBoolean( ENV_PULSAR_PRESENT ).setEnabled( envEnabled );
			envPanel.getBoolean( ENV_PDS_PRESENT ).setEnabled( envEnabled );
			envPanel.getCombo( ENV_VULN ).setEnabled( envEnabled );
			envPanel.getBoolean( ENV_ASTEROID_FIELD ).setEnabled( envEnabled );
			envPanel.getInt( ENV_ASTEROID_ALPHA ).setEnabled( envEnabled );
			envPanel.getInt( ENV_ASTEROID_STRAY_TICKS ).setEnabled( envEnabled );
			envPanel.getInt( ENV_ASTEROID_GAMMA ).setEnabled( envEnabled );
			envPanel.getInt( ENV_ASTEROID_BKG_TICKS ).setEnabled( envEnabled );
			envPanel.getInt( ENV_ASTEROID_TARGET ).setEnabled( envEnabled );
			envPanel.getInt( ENV_FLARE_FADE_TICKS ).setEnabled( envEnabled );
			envPanel.getInt( ENV_HAVOC_TICKS ).setEnabled( envEnabled );
			envPanel.getInt( ENV_PDS_TICKS ).setEnabled( envEnabled );

			forceCheckBox( envPanel.getBoolean( ENV_ASTEROID_FIELD ), false );

			if ( envEnabled ) {
				envPanel.setBoolAndReminder( ENV_RED_GIANT_PRESENT, env.isRedGiantPresent() );
				envPanel.setBoolAndReminder( ENV_PULSAR_PRESENT, env.isPulsarPresent() );
				envPanel.setBoolAndReminder( ENV_PDS_PRESENT, env.isPDSPresent() );
				envPanel.setComboAndReminder( ENV_VULN, env.getVulnerableShips() );

				SavedGameParser.AsteroidFieldState asteroidField = env.getAsteroidField();
				boolean asteroidsPresent = ( asteroidField != null );
				envPanel.getBoolean( ENV_ASTEROID_FIELD ).setSelected( asteroidsPresent );

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
			aiPanel.getBoolean( AI_SURRENDERED ).setEnabled( aiEnabled );
			aiPanel.getBoolean( AI_ESCAPING ).setEnabled( aiEnabled );
			aiPanel.getBoolean( AI_DESTROYED ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_SURRENDER_THRESHOLD ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_ESCAPE_THRESHOLD ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_ESCAPE_TICKS ).setEnabled( aiEnabled );
			aiPanel.getBoolean( AI_STALEMATE ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_STALEMATE_TICKS ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_BOARDING_ATTEMPTS ).setEnabled( aiEnabled );
			aiPanel.getInt( AI_BOARDERS_NEEDED ).setEnabled( aiEnabled );

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
			unknownsPanel.setIntAndReminder( TOP_MU, gameState.getUnknownMu() );
			unknownsPanel.setIntAndReminder( TOP_NU, gameState.getUnknownNu() );
			unknownsPanel.setIntAndReminder( TOP_XI, (gameState.getUnknownXi() != null ? gameState.getUnknownXi().intValue() : 0) );
			unknownsPanel.setBoolAndReminder( TOP_AUTOFIRE, gameState.getAutofire() );

			SavedGameParser.EncounterState enc = gameState.getEncounter();
			encEnabled = ( enc != null );
			encPanel.getInt( ENC_SHIP_EVENT_SEED ).setEnabled( encEnabled );
			encPanel.getString( ENC_SURRENDER_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_ESCAPE_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_DESTROYED_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_DEAD_CREW_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_GOT_AWAY_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_LAST_EVENT ).setEnabled( encEnabled );
			encPanel.getString( ENC_TEXT ).setEnabled( encEnabled );
			encPanel.getInt( ENC_CREW_SEED ).setEnabled( encEnabled );
			encPanel.getString( ENC_CHOICES ).setEnabled( encEnabled );

			if ( encEnabled ) {
				encPanel.setIntAndReminder( ENC_SHIP_EVENT_SEED, enc.getShipEventSeed() );
				encPanel.setStringAndReminder( ENC_SURRENDER_EVENT, enc.getSurrenderEventId() );
				encPanel.setStringAndReminder( ENC_ESCAPE_EVENT, enc.getEscapeEventId() );
				encPanel.setStringAndReminder( ENC_DESTROYED_EVENT, enc.getDestroyedEventId() );
				encPanel.setStringAndReminder( ENC_DEAD_CREW_EVENT, enc.getDeadCrewEventId() );
				encPanel.setStringAndReminder( ENC_GOT_AWAY_EVENT, enc.getGotAwayEventId() );
				encPanel.setStringAndReminder( ENC_LAST_EVENT, enc.getLastEventId() );
				encPanel.getString( ENC_TEXT ).setText( enc.getText() );
				encPanel.setIntAndReminder( ENC_CREW_SEED, enc.getAffectedCrewSeed() );

				StringBuilder choiceBuf = new StringBuilder();
				for ( Integer n : enc.getChoiceList() ) {
					if ( choiceBuf.length() > 0 ) choiceBuf.append( "," );
					choiceBuf.append( n.toString() );
				}
				encPanel.setStringAndReminder( ENC_CHOICES, choiceBuf.toString() );
			}
		}

		this.repaint();
	}

	@SuppressWarnings("unchecked")
	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		SavedGameParser.ShipState shipState = gameState.getPlayerShipState();

		try { gameState.setTotalShipsDefeated( sessionPanel.parseInt( TOTAL_SHIPS_DEFEATED ) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalBeaconsExplored( sessionPanel.parseInt( TOTAL_BEACONS ) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalScrapCollected( sessionPanel.parseInt( TOTAL_SCRAP ) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setTotalCrewHired( sessionPanel.parseInt( TOTAL_CREW_HIRED ) ); }
		catch ( NumberFormatException e ) {}

		gameState.setDLCEnabled( sessionPanel.getBoolean( DLC ).isSelected() );

		Object diffObj = sessionPanel.getCombo( DIFFICULTY ).getSelectedItem();
		gameState.setDifficulty( (Difficulty)diffObj );

		try { gameState.setUnknownBeta( sessionPanel.parseInt( TOP_BETA ) ); }
		catch ( NumberFormatException e ) {}

		gameState.getCargoIdList().clear();
		for ( int i=0; i < cargoSlots.length; i++ ) {
			Object cargoObj = cargoPanel.getCombo( cargoSlots[i] ).getSelectedItem();
			if ( cargoObj instanceof WeaponBlueprint ) {
				gameState.addCargoItemId( ((WeaponBlueprint)cargoObj).getId() );
			}
			else if ( cargoObj instanceof DroneBlueprint ) {
				gameState.addCargoItemId( ((DroneBlueprint)cargoObj).getId() );
			}
		}

		SavedGameParser.EnvironmentState env = gameState.getEnvironment();
		if ( env != null && envEnabled ) {
			env.setRedGiantPresent( envPanel.getBoolean( ENV_RED_GIANT_PRESENT ).isSelected() );
			env.setPulsarPresent( envPanel.getBoolean( ENV_PULSAR_PRESENT ).isSelected() );
			env.setPDSPresent( envPanel.getBoolean( ENV_PDS_PRESENT ).isSelected() );

			Object vulnObj = envPanel.getCombo( ENV_VULN ).getSelectedItem();
			env.setVulnerableShips( (SavedGameParser.HazardVulnerability)vulnObj );

			SavedGameParser.AsteroidFieldState asteroidField = null;
			if ( envPanel.getBoolean( ENV_ASTEROID_FIELD ).isSelected() ) {
				asteroidField = new SavedGameParser.AsteroidFieldState();

				try { asteroidField.setUnknownAlpha( envPanel.parseInt( ENV_ASTEROID_ALPHA ) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setStrayRockTicks( envPanel.parseInt( ENV_ASTEROID_STRAY_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setUnknownGamma( envPanel.parseInt( ENV_ASTEROID_GAMMA ) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setBgDriftTicks( envPanel.parseInt( ENV_ASTEROID_BKG_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				try { asteroidField.setCurrentTarget( envPanel.parseInt( ENV_ASTEROID_TARGET ) ); }
				catch ( NumberFormatException e ) {}
			}
			env.setAsteroidField( asteroidField );

			try { env.setSolarFlareFadeTicks( envPanel.parseInt( ENV_FLARE_FADE_TICKS ) ); }
			catch ( NumberFormatException e ) {}

			try { env.setHavocTicks( envPanel.parseInt( ENV_HAVOC_TICKS ) ); }
			catch ( NumberFormatException e ) {}

			try { env.setPDSTicks( envPanel.parseInt( ENV_PDS_TICKS ) ); }
			catch ( NumberFormatException e ) {}
		}

		SavedGameParser.NearbyShipAIState ai = gameState.getNearbyShipAI();
		if ( ai != null && aiEnabled ) {
			ai.setSurrendered( aiPanel.getBoolean( AI_SURRENDERED ).isSelected() );
			ai.setEscaping( aiPanel.getBoolean( AI_ESCAPING ).isSelected() );
			ai.setDestroyed( aiPanel.getBoolean( AI_DESTROYED ).isSelected() );

			try { ai.setSurrenderThreshold( aiPanel.parseInt( AI_SURRENDER_THRESHOLD ) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setEscapeThreshold( aiPanel.parseInt( AI_ESCAPE_THRESHOLD ) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setEscapeTicks( aiPanel.parseInt( AI_ESCAPE_TICKS ) ); }
			catch ( NumberFormatException e ) {}

			ai.setStalemateTriggered( aiPanel.getBoolean( AI_STALEMATE ).isSelected() );

			try { ai.setStalemateTicks( aiPanel.parseInt( AI_STALEMATE_TICKS ) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setBoardingAttempts( aiPanel.parseInt( AI_BOARDING_ATTEMPTS ) ); }
			catch ( NumberFormatException e ) {}

			try { ai.setBoardersNeeded( aiPanel.parseInt( AI_BOARDERS_NEEDED ) ); }
			catch ( NumberFormatException e ) {}
		}

		gameState.setWaiting( unknownsPanel.getBoolean( TOP_WAITING ).isSelected() );

		try { gameState.setWaitEventSeed( unknownsPanel.parseInt( TOP_WAIT_EVENT_SEED ) ); }
		catch ( NumberFormatException e ) {}

		gameState.setUnknownEpsilon( unknownsPanel.getString( TOP_EPSILON ).getText() );

		try { gameState.setUnknownMu( unknownsPanel.parseInt( TOP_MU ) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setUnknownNu( unknownsPanel.parseInt( TOP_NU ) ); }
		catch ( NumberFormatException e ) {}

		try { gameState.setUnknownXi( new Integer( unknownsPanel.parseInt( TOP_XI ) ) ); }
		catch ( NumberFormatException e ) {}

		gameState.setAutofire( unknownsPanel.getBoolean( TOP_AUTOFIRE ).isSelected() );

		SavedGameParser.EncounterState enc = gameState.getEncounter();
		if ( enc != null && encEnabled ) {
			try { enc.setShipEventSeed( encPanel.parseInt( ENC_SHIP_EVENT_SEED ) ); }
			catch ( NumberFormatException e ) {}

			enc.setEscapeEventId( encPanel.getString( ENC_ESCAPE_EVENT ).getText() );
			enc.setDestroyedEventId( encPanel.getString( ENC_DESTROYED_EVENT ).getText() );
			enc.setDeadCrewEventId( encPanel.getString( ENC_DEAD_CREW_EVENT ).getText() );
			enc.setGotAwayEventId( encPanel.getString( ENC_GOT_AWAY_EVENT ).getText() );
			enc.setLastEventId( encPanel.getString( ENC_LAST_EVENT ).getText() );
			enc.setText( encPanel.getString( ENC_TEXT ).getText() );

			try { enc.setAffectedCrewSeed( encPanel.parseInt( ENC_CREW_SEED ) ); }
			catch ( NumberFormatException e ) {}

			try {
				List<Integer> newChoices = new ArrayList<Integer>();
				String choicesString = encPanel.getString( ENC_CHOICES ).getText();
				choicesString = choicesString.replaceAll( ",,+", "," );
				choicesString = choicesString.replaceAll( "^,|,$", "" );
				for ( String chunk : choicesString.split( "," ) ) {
					newChoices.add( new Integer( chunk ) );
				}
				enc.setChoiceList( newChoices );
			}
			catch ( NumberFormatException e ) {}
		}
	}

	/**
	 * Sets a JCheckBox selection and triggers all ItemListeners.
	 */
	private void forceCheckBox( JCheckBox box, boolean selected ) {
		if ( box.isSelected() != selected ) {
			box.setSelected( selected );

			// No need to manually trigger listeners, since it really changed.
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
