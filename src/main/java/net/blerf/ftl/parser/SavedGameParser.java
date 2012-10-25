package net.blerf.ftl.parser;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.MysteryBytes;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameParser extends DatParser {

	private static final Logger log = LogManager.getLogger(SavedGameParser.class);


	public SavedGameState readSavedGame(File datFile) throws IOException {
		FileInputStream in = null;
		InputStream layoutStream = null;
		try {
			SavedGameState gameState = new SavedGameState();
			in = new FileInputStream(datFile);

			gameState.addMysteryBytes( new MysteryBytes(in, 8) );

			gameState.setTotalShipsDefeated( readInt(in) );
			gameState.setTotalBeaconsExplored( readInt(in) );
			gameState.setTotalScrapCollected( readInt(in) );
			gameState.setTotalCrewHired( readInt(in) );

			String playerShipName = readString(in);         // Redundant.
			String playerShipBlueprintId = readString(in);  // Redundant.
			gameState.setPlayerShipInfo( playerShipName, playerShipBlueprintId );

			int sectorNumber = readInt(in);
			gameState.setSectorNumber( sectorNumber );

			gameState.addMysteryBytes( new MysteryBytes(in, 4) );

			int stateVarCount = readInt(in);
			for (int i=0; i < stateVarCount; i++) {
				String stateVarId = readString(in);
				Integer stateVarValue = new Integer(readInt(in));
				gameState.setStateVar(stateVarId, stateVarValue);
			}

			ShipState playerShipState = readShip( in, true );
			gameState.setPlayerShipState( playerShipState );

			gameState.addMysteryBytes( new MysteryBytes(in, 4) );
			
			gameState.setSectorLayoutSeed( readInt(in) );
			
			gameState.setRebelFleetOffset( readInt(in) );
			
			gameState.addMysteryBytes( new MysteryBytes(in, 4) );

			gameState.setRebelPursuitMod( readInt(in) );

			gameState.setSectorHazardsVisible( readBool(in) );

			gameState.setRebelFlagshipVisible( readBool(in) );

			gameState.setRebelFlagshipHop( readInt(in) );

			gameState.setRebelFlagshipApproaching( readBool(in) );

			int sectorCount = readInt(in);
			for (int i=0; i < sectorCount; i++) {
				gameState.addSector( readBool(in) );
			}

			int zeroBasedSectorNumber = readInt( in );  // Redundant.

			gameState.addMysteryBytes( new MysteryBytes(in, 4) );
			
			int beaconCount = readInt(in);
			for (int i=0; i < beaconCount; i++) {
				gameState.addBeacon( readBeacon(in) );
			}

			int questEventCount = readInt(in);
			for (int i=0; i < questEventCount; i++) {
				String questEventId = readString(in);
				int questBeaconId = readInt(in);
				gameState.addQuestEvent( questEventId, questBeaconId );
			}

			int distantQuestEventCount = readInt(in);
			for (int i=0; i < distantQuestEventCount; i++) {
				String distantQuestEventId = readString(in);
				gameState.addDistantQuestEvent( distantQuestEventId );
			}

			gameState.setCurrentBeaconId( readInt(in) );

			boolean shipNearby = readBool(in);
			if ( shipNearby ) {
				ShipState nearbyShipState = readShip( in, false );
				gameState.setNearbyShipState(nearbyShipState);
			}

			// Here, the stream might end.

			int bytesRemaining = (int)(in.getChannel().size() - in.getChannel().position());

			// Or, if this is sector 8 and the boss has been engaged at
			// least once, this will definitely be present.
			if ( sectorNumber == 8 && bytesRemaining > 2*4 ) {
				RebelFlagshipState flagshipState = readRebelFlagship(in);
				gameState.setRebelFlagshipState( flagshipState );
			}

			// Otherwise this is sometimes present...
			// This hasn't been observed to coincide with the above, but
			// this is intermittent in all sectors, which would be
			// odd if it were boss related.
			//
			//   0x0100_0000 0x0000_0000 == 1 0 as ints. No idea what for.

			bytesRemaining = (int)(in.getChannel().size() - in.getChannel().position());
			if ( bytesRemaining > 0 ) {
				gameState.addMysteryBytes( new MysteryBytes(in, bytesRemaining) );
			}

			return gameState;  // The finally block will still be executed.

		} finally {
			try {if (in != null) in.close();}
			catch (IOException e) {}

			try {if (layoutStream != null) in.close();}
			catch (IOException e) {}
		}
	}

	private ShipState readShip( InputStream in, boolean auto ) throws IOException {

		String shipBlueprintId = readString(in);  // blueprints.xml / autoBlueprints.xml.
		String shipName = readString(in);
		String shipGfxBaseName = readString(in);

		ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
		if ( shipBlueprint == null )
			throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (auto ? " auto" : ""), shipName) );

		String shipLayoutId = shipBlueprint.getLayout();

		// Use this for room and door info later.
		ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
		if ( shipLayout == null )
			throw new RuntimeException( String.format("Could not find layout for%s ship: %s", (auto ? " auto" : ""), shipName) );

		ShipState shipState = new ShipState(shipName, shipBlueprintId, shipLayoutId, auto);
		shipState.setShipGraphicsBaseName( shipGfxBaseName );

		int startingCrewCount = readInt(in);
		for (int i=0; i < startingCrewCount; i++) {
			shipState.addStartingCrewMember( readStartingCrewMember(in) );
		}

		shipState.setHullAmt( readInt(in) );
		shipState.setFuelAmt( readInt(in) );
		shipState.setDronePartsAmt( readInt(in) );
		shipState.setMissilesAmt( readInt(in) );
		shipState.setScrapAmt( readInt(in) );

		int crewCount = readInt(in);
		for (int i=0; i < crewCount; i++) {
			shipState.addCrewMember( readCrewMember(in) );
		}

		// System info is stored in this order.
		String[] systemNames = new String[] {"Shields", "Engines", "Oxygen",
		                                     "Weapons", "Drone Ctrl", "Medbay",
		                                     "Pilot", "Sensors", "Doors",
		                                     "Teleporter", "Cloaking", "Artillery"};
		shipState.setReservePowerCapacity( readInt(in) );
		for (String name : systemNames) {
			shipState.addSystem( readSystem(in, name) );
		}

		int roomCount = shipLayout.getRoomCount();
		for (int r=0; r < roomCount; r++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfo = shipLayout.getRoomInfo(r);
			int squaresH = roomInfo.get(ShipLayout.RoomInfo.SQUARES_H).intValue();
			int squaresV = roomInfo.get(ShipLayout.RoomInfo.SQUARES_V).intValue();

			// Room states are stored in roomId order.
			shipState.addRoom( readRoom(in, squaresH, squaresV) );
		}

		int breachCount = readInt(in);
		for (int i=0; i < breachCount; i++) {
			shipState.setBreach( readInt(in), readInt(in), readInt(in) );
		}

		LinkedHashMap<int[], EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for (int[] doorCoord : layoutDoorMap.keySet()) {
			shipState.setDoor( doorCoord[0], doorCoord[1], doorCoord[2], readDoor(in) );
		}

		int weaponCount = readInt(in);
		for (int i=0; i < weaponCount; i++) {
			String weaponId = readString(in);
			boolean weaponArmed = readBool(in);
			int weaponCooldownTicks = readInt(in);
			shipState.addWeapon( new WeaponState(weaponId, weaponArmed, weaponCooldownTicks) );
		}

		int droneCount = readInt(in);
		for (int i=0; i < droneCount; i++) {
			shipState.addDrone( readDrone(in) );
		}

		int augmentCount = readInt(in);
		for (int i=0; i < augmentCount; i++) {
			shipState.addAugmentId( readString(in) );
		}

		int cargoCount = readInt(in);  // TODO: Nearby ships say 1, but have none?
		for (int i=0; i < cargoCount; i++) {
			shipState.addCargoItemId( readString(in) );
		}

		return shipState;
	}

	private StartingCrewState readStartingCrewMember( InputStream in ) throws IOException {
		String crewRace = readString(in);
		String crewName = readString(in);
		StartingCrewState startingCrew = new StartingCrewState(crewName, crewRace);
		return startingCrew;
	}

	private CrewState readCrewMember( InputStream in ) throws IOException {
		CrewState crew = new CrewState();
		crew.setName( readString(in) );
		crew.setRace( readString(in) );
		crew.setEnemyBoardingDrone( readBool(in) );
		crew.setHealth( readInt(in) );
		crew.setX( readInt(in) );
		crew.setY( readInt(in) );
		crew.setRoomId( readInt(in) );
		crew.setRoomSquare( readInt(in) );
		crew.setPlayerControlled( readBool(in) );
		crew.setPilotSkill( readInt(in) );
		crew.setEngineSkill( readInt(in) );
		crew.setShieldSkill( readInt(in) );
		crew.setWeaponSkill( readInt(in) );
		crew.setRepairSkill( readInt(in) );
		crew.setCombatSkill( readInt(in) );
		crew.setGender( readInt(in) );
		crew.setRepairs( readInt(in) );
		crew.setCombatKills( readInt(in) );
		crew.setPilotedEvasions( readInt(in) );
		crew.setJumpsSurvived( readInt(in) );
		crew.setSkillMasteries( readInt(in) );
		return crew;
	}

	private SystemState readSystem( InputStream in, String name ) throws IOException {
		SystemState system = new SystemState( name );
		int capacity = readInt(in);

		// Normally systems are 28 bytes, but if not present on the
		// ship, capacity will be zero, and the system will only
		// occupy the 4 bytes that declared the capacity. And the
		// next system will begin 24 bytes sooner.
		if (capacity > 0) {
			system.setCapacity( capacity );
			system.setPower( readInt(in) );
			system.setDamagedBars( readInt(in) );
			system.setIonizedBars( readInt(in) );

			int miscTicks = readInt(in);
			if ( miscTicks == -2147483648 )
				miscTicks = Integer.MIN_VALUE;
			system.setMiscTicks( miscTicks );

			system.setRepairProgress( readInt(in) );
			system.setBurnProgress( readInt(in) );
		}
		return system;
	}

	private RoomState readRoom( InputStream in, int squaresH, int squaresV ) throws IOException {
		RoomState room = new RoomState();
		room.setOxygen( readInt(in) );

		for (int h=0; h < squaresH; h++) {
			for (int v=0; v < squaresV; v++) {
				// Dunno what the third int is. The others are for fire.
				// Values in the wild: 0-100 / 0-100 / -1.
				// It is not related to hull breaches.
				room.addSquare( readInt(in), readInt(in), readInt(in) );
			}
		}

		return room;
	}

	private DoorState readDoor( InputStream in ) throws IOException {
		boolean open = readBool(in);
		int alpha = readInt(in);  // 0. What else would a door have; damage?
		DoorState door = new DoorState( open, alpha );
		return door;
	}

	private DroneState readDrone( InputStream in ) throws IOException {
		DroneState drone = new DroneState( readString(in) );
		drone.setArmed( readBool(in) );
		drone.setPlayerControlled( readBool(in) );
		drone.setX( readInt(in) );
		drone.setY( readInt(in) );
		drone.setRoomId( readInt(in) );
		drone.setRoomSquare( readInt(in) );
		drone.setHealth( readInt(in) );
		return drone;
	}

	private BeaconState readBeacon( InputStream in ) throws IOException {

		BeaconState beacon = new BeaconState();

		boolean visited = readBool(in);
		beacon.setVisited(visited);
		if ( visited ) {
			beacon.setBgStarscapeImageInnerPath( readString(in) );
			beacon.setBgSpriteImageInnerPath( readString(in) );
			beacon.setBgSpritePosX( readInt(in) );
			beacon.setBgSpritePosY( readInt(in) );
			beacon.setUnknownVisitedAlpha( readInt(in) );
		}
		
		beacon.setSeen( readBool(in) );
		
		boolean enemyPresent = readBool(in);
		beacon.setEnemyPresent(enemyPresent);
		if ( enemyPresent ) {
			beacon.setShipEventId( readString(in) );
			beacon.setShipBlueprintListId( readString(in) );
			beacon.setUnknownEnemyPresentAlpha( readInt(in) );
		}
		
		int fleetPresence = readInt(in);
		switch ( fleetPresence ) {
			case 0: beacon.setFleetPresence( FleetPresence.NONE ); break;
			case 1: beacon.setFleetPresence( FleetPresence.REBEL ); break;
			case 2: beacon.setFleetPresence( FleetPresence.FEDERATION ); break;
			case 3: beacon.setFleetPresence( FleetPresence.BOTH ); break;
			default: throw new RuntimeException( "Unknown fleet presence: " + fleetPresence );
		}
	
		beacon.setUnderAttack( readBool(in) );
		
		boolean storePresent = readBool(in);
		beacon.setStorePresent(storePresent);
		if ( storePresent ) {
			StoreState store = new StoreState();
			store.setTopShelf( readStoreShelf(in) );
			store.setBottomShelf( readStoreShelf(in) );
			store.setFuel( readInt(in) );
			store.setMissiles( readInt(in) );
			store.setDroneParts( readInt(in) );
			beacon.setStore(store);
		}

		return beacon;
		
	}
	
	private StoreShelf readStoreShelf( InputStream in ) throws IOException {
		
		StoreShelf shelf = new StoreShelf();
		
		int itemType = readInt(in);
		switch ( itemType ) {
			case 0: shelf.setItemType( StoreItemType.WEAPON ); break;
			case 1: shelf.setItemType( StoreItemType.DRONE ); break;
			case 2: shelf.setItemType( StoreItemType.AUGMENT ); break;
			case 3: shelf.setItemType( StoreItemType.CREW ); break;
			case 4: shelf.setItemType( StoreItemType.SYSTEM ); break;
			default: throw new RuntimeException( "Unknown store item type: " + itemType );
		}
		
		for (int i = 0; i < 3; i++) {
			int available = readInt(in);
			if ( available < 0 )
				continue; // -1 means no item
			String itemId = readString(in);
			shelf.addItem( new StoreItem(available > 0, itemId) );
		}
		
		return shelf;
		
	}



	public RebelFlagshipState readRebelFlagship( InputStream in ) throws IOException {

		// TODO: Magic strings.
		String[] blueprintIds = new String[] {"BOSS_1", "BOSS_2", "BOSS_3"};

		RebelFlagshipState flagship = new RebelFlagshipState( blueprintIds );

		flagship.setPendingStage( readInt(in) );

		int previousRoomCount = readInt(in);
		for (int i=0; i < previousRoomCount; i++) {
			flagship.setPreviousOccupancy( i, readBool(in) );
		}

		return flagship;
	}



	// Stash state classes here until they're finalized.

	public class SavedGameState {
		private int totalShipsDefeated = 0;
		private int totalBeaconsExplored = 0;
		private int totalScrapCollected = 0;
		private int totalCrewHired = 0;
		private String playerShipName = "";
		private String playerShipBlueprintId = "";
		private int sectorNumber = 1;
		private HashMap<String, Integer> stateVars = new HashMap<String, Integer>();
		private ShipState playerShipState = null;
		private int sectorLayoutSeed;
		private int rebelFleetOffset;
		private int rebelPursuitMod = 0;
		private boolean sectorHazardsVisible = false;
		private boolean rebelFlagshipVisible = false;
		private int rebelFlagshipHop = 0;
		private boolean rebelFlagshipApproaching = false;
		private ArrayList<Boolean> sectorList = new ArrayList<Boolean>();
		private ArrayList<BeaconState> beaconList = new ArrayList<BeaconState>();
		private LinkedHashMap<String, Integer> questEventMap = new LinkedHashMap<String, Integer>();
		private ArrayList<String> distantQuestEventList = new ArrayList<String>();
		private ShipState nearbyShipState = null;
		private int currentBeaconId = 0;
		private RebelFlagshipState rebelFlagshipState = null;
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public void setTotalShipsDefeated( int n ) { totalShipsDefeated = n; }
		public void setTotalBeaconsExplored( int n ) { totalBeaconsExplored = n; }
		public void setTotalScrapCollected( int n ) { totalScrapCollected = n; }
		public void setTotalCrewHired( int n ) { totalCrewHired = n; }

		/**
		 * Set redundant player ship info.
		 */
		public void setPlayerShipInfo( String shipName, String shipBlueprintId ) {
			playerShipName = shipName;
			playerShipBlueprintId = shipBlueprintId;
		}

		public void setSectorNumber( int n ) { sectorNumber = n; }

		/**
		 * Sets a state var.
		 * The following ids have been seen in the wild:
		 * blue_alien, dead_crew, destroyed_rock, env_danger, fired_shot,
		 * killed_crew, nebula, offensive_drone, reactor_upgrade,
		 * store_purchase, store_repair, system_upgrade, teleported,
		 * used_drone, used_missile, weapon_upgrade.
		 *
		 * Each is optional, and counts something.
		 * *_upgrade counts upgrades beyond the ship's default levels.
		 */
		public void setStateVar( String stateVarId, int stateVarValue ) {
			stateVars.put(stateVarId, new Integer(stateVarValue));
		}

		public boolean hasStateVar( String stateVarId ) {
			return stateVars.containsKey(stateVarId);
		}

		public int getStateVar( String stateVarId ) {
			// Don't ask for vars that aren't present!

			Integer result = stateVars.get(stateVarId);
			return result.intValue();
		}

		public void setPlayerShipState( ShipState shipState ) {
			this.playerShipState = shipState;
		}

		public void setSectorLayoutSeed( int n ) { sectorLayoutSeed = n; }

		/** Sets the fleet position, in pixels from far right of sector map. */
		public void setRebelFleetOffset( int n ) { rebelFleetOffset = n; }

		/** Delays/alerts the rebel fleet (-/+). */
		public void setRebelPursuitMod( int n ) { rebelPursuitMod = n; }

		/** Toggles visibility of beacon hazards for this sector. */
		public void setSectorHazardsVisible( boolean b ) { sectorHazardsVisible = b; }

		/** Toggles the flagship. Instant lose if not in sector 8. */
		public void setRebelFlagshipVisible( boolean b ) { rebelFlagshipVisible = b; }

		/** Set's the flagship's next/current beacon, as an index of a fixed list? */
		public void setRebelFlagshipHop( int n ) { rebelFlagshipHop = n; }

		/** Sets whether the flagship's approaching or circling its hop beacon. */
		public void setRebelFlagshipApproaching( boolean b ) { rebelFlagshipApproaching = b; }

		/**
		 * Adds a dot of the sector tree.
		 * Dots are indexed top-to-bottom for each column, left-to-right.
		 */
		public void addSector( boolean visited ) {
			sectorList.add( new Boolean(visited) );
		}

		public void setSectorVisited( int sector, boolean visited ) {
			sectorList.set( sector, new Boolean(visited) );
		}

		/**
		 * Adds a beacon to the sector map.
		 * Beacons are indexed top-to-bottom for each column,
		 * left-to-right. They're randomly offset a little
		 * when shown on screen to disguise the columns.
		 */
		public void addBeacon( BeaconState beacon ) {
			beaconList.add( beacon );
		}

		public void addQuestEvent( String questEventId, int questBeaconId ) {
			questEventMap.put( questEventId, new Integer(questBeaconId) );
		}

		public void addDistantQuestEvent( String questEventId ) {
			distantQuestEventList.add( questEventId );
		}

		/** Sets where the player is. */
		public void setCurrentBeaconId( int n ) { currentBeaconId = n; }

		public void setNearbyShipState( ShipState shipState ) {
			this.nearbyShipState = shipState;
		}

		public void setRebelFlagshipState( RebelFlagshipState flagshipState ) {
			this.rebelFlagshipState = flagshipState;
		}

		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Name: %s\n", playerShipName));
			result.append(String.format("Ship Type: %s\n", playerShipBlueprintId));
			result.append(String.format("Sector:                 %4d\n", sectorNumber));
			result.append(String.format("Total Ships Defeated:   %4d\n", totalShipsDefeated));
			result.append(String.format("Total Beacons Explored: %4d\n", totalBeaconsExplored));
			result.append(String.format("Total Scrap Collected:  %4d\n", totalScrapCollected));
			result.append(String.format("Total Crew Hired:       %4d\n", totalCrewHired));

			result.append("\nState Vars...\n");
			for (Map.Entry<String, Integer> entry : stateVars.entrySet()) {
				result.append(String.format("%-16s %4d\n", entry.getKey() +":", entry.getValue().intValue()));
			}

			result.append("\nPlayer Ship...\n");
			if ( playerShipState != null )
				result.append(playerShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nSector Data...\n");
			result.append( String.format("Sector Layout Seed: %5d\n", sectorLayoutSeed) );
			result.append( String.format("Rebel Fleet Offset: %5d\n", rebelFleetOffset) );
			result.append( String.format("Rebel Pursuit Mod:  %5d\n", rebelPursuitMod) );
			result.append( String.format("Sector Hazards Map: %b\n", sectorHazardsVisible) );
			result.append( String.format("Rebel Flagship On:  %b\n", rebelFlagshipVisible) );
			result.append( String.format("Flagship Nth Hop:   %5d\n", rebelFlagshipHop) );
			result.append( String.format("Flagship Moving:    %b\n", rebelFlagshipApproaching) );
			result.append( String.format("Player BeaconId:    %5d\n", currentBeaconId) );

			result.append("\nSector Tree Breadcrumbs...\n");
			first = true;
			for (Boolean b : sectorList) {
				if (first) { first = false; }
				else { result.append(","); }
				result.append( (b ? "T" : "F") );
			}
			result.append("\n");

			result.append("\nSector Beacons...\n");
			int beaconId = 0;
			first = true;
			for( BeaconState beacon : beaconList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append( String.format("BeaconId: %2d\n", beaconId++) );
				result.append( beacon.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			result.append("\nQuests...\n");
			for (Map.Entry<String, Integer> entry : questEventMap.entrySet()) {
				String questEventId = entry.getKey();
				int questBeaconId = entry.getValue().intValue();
				result.append(String.format("QuestEventId: %s, BeaconId: %d\n", questEventId, questBeaconId));
			}

			result.append("\nNext Sector Quests...\n");
			for (String questEventId : distantQuestEventList) {
				result.append(String.format("QuestEventId: %s\n", questEventId));
			}

			result.append("\nNearby Ship...\n");
			if ( nearbyShipState != null )
				result.append(nearbyShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nRebel Flagship...\n");
			if ( rebelFlagshipState != null )
				result.append(rebelFlagshipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nMystery Bytes...\n");
			first = true;
			for (MysteryBytes m : mysteryList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(m.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			// ...
			return result.toString();
		}
	}



	public class ShipState {
		private boolean auto = false;  // Is autoShip.
		private String shipName, shipBlueprintId, shipLayoutId;
		private String shipGfxBaseName;
		private ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private int hullAmt, fuelAmt, dronePartsAmt, missilesAmt, scrapAmt;
		private ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity;
		private ArrayList<SystemState> systemList = new ArrayList<SystemState>();
		private ArrayList<RoomState> roomList = new ArrayList<RoomState>();
		private LinkedHashMap<Point, Integer> breachMap = new LinkedHashMap<Point, Integer>();
		private LinkedHashMap<int[], DoorState> doorMap = new LinkedHashMap<int[], DoorState>();
		private ArrayList<WeaponState> weaponList = new ArrayList<WeaponState>();
		private ArrayList<DroneState> droneList = new ArrayList<DroneState>();
		private ArrayList<String> augmentIdList = new ArrayList<String>();
		private ArrayList<String> cargoIdList = new ArrayList<String>();

		public ShipState(String shipName, String shipBlueprintId, String shipLayoutId, boolean auto) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprintId;
			this.shipLayoutId = shipLayoutId;
			this.auto = auto;
		}

		/**
		 * Sets the basename to use when loading ship images.
		 * See 'img/ship/basename_*.png'.
		 *
		 * Values in the wild:
		 *   jelly_croissant_pirate, rebel_long_pirate...
		 *
		 * It often resembles the layout id, but they're not interchangeable.
		 * The proper shipLayoutId comes from the ShipBlueprint.
		 */
		public void setShipGraphicsBaseName( String shipGfxBaseName ) {
			this.shipGfxBaseName = shipGfxBaseName;
		}

		public void addStartingCrewMember( StartingCrewState sc ) {
			startingCrewList.add(sc);
		}

		public void setHullAmt( int n ) { hullAmt = n; }
		public void setFuelAmt( int n ) { fuelAmt = n; }
		public void setDronePartsAmt( int n ) { dronePartsAmt = n; }
		public void setMissilesAmt( int n ) { missilesAmt = n; }
		public void setScrapAmt( int n ) { scrapAmt = n; }

		public void addCrewMember( CrewState c ) {
			crewList.add(c);
		}

		public void setReservePowerCapacity( int n ) {
			reservePowerCapacity = n;
		}

		public void addSystem( SystemState s ) {
			systemList.add(s);
		}

		public void addRoom( RoomState r ) {
			roomList.add(r);
		}

		/**
		 * Adds a hull breach.
		 *
		 * @param x the 0-based Nth floor-square from the left (minus ShipLayout X_OFFSET)
		 * @param y the 0-based Nth floor-square from the top (minus ShipLayout Y_OFFSET)
		 * @param breachHealth 0 to 100.
		 */
		public void setBreach( int x, int y, int breachHealth ) {
			breachMap.put( new Point(x, y), new Integer(breachHealth) );
		}

		/**
		 * Adds a door.
		 *
		 * @param wallX the 0-based Nth wall from the left
		 * @param wallY the 0-based Nth wall from the top
		 * @param vertical 1 for vertical wall coords, 0 for horizontal
		 * @see net.blerf.ftl.model.ShipLayout
		 */
		public void setDoor( int wallX, int wallY, int vertical, DoorState d ) {
			int[] doorCoord = new int[] { wallX, wallY, vertical };
			doorMap.put(doorCoord, d);
		}

		public void addWeapon( WeaponState w ) {
			weaponList.add(w);
		}

		public void addDrone( DroneState d ) {
			droneList.add(d);
		}

		public void addAugmentId( String augmentId ) {
			augmentIdList.add(augmentId);
		}
		
		public void addCargoItemId( String cargoItemId ) {
			cargoIdList.add( cargoItemId );
		}

		@Override
		public String toString() {
			// The blueprint fetching might vary if auto == true.
			// See autoBlueprints.xml vs blueprints.xml.
			ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
			ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

			ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
			if ( shipLayout == null )
				throw new RuntimeException( String.format("Could not find layout for%s ship: %s", (auto ? " auto" : ""), shipName) );

			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Name:    %s\n", shipName));
			result.append(String.format("Ship Type:    %s\n", shipBlueprintId));
			result.append(String.format("Ship Layout:  %s\n", shipLayoutId));
			result.append(String.format("Gfx BaseName: %s\n", shipGfxBaseName));

			result.append("\nSupplies...\n");
			result.append(String.format("Hull:        %3d\n", hullAmt));
			result.append(String.format("Fuel:        %3d\n", fuelAmt));
			result.append(String.format("Drone Parts: %3d\n", dronePartsAmt));
			result.append(String.format("Missiles:    %3d\n", missilesAmt));
			result.append(String.format("Scrap:       %3d\n", scrapAmt));

			result.append("\nStarting Crew...\n");
			first = true;
			for (StartingCrewState sc : startingCrewList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(sc.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nCurrent Crew...\n");
			first = true;
			for (CrewState c : crewList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(c.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nSystems...\n");
			result.append(String.format("  Reserve Power Capacity: %2d\n", reservePowerCapacity));
			first = false;
			for (SystemState s : systemList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(s.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nRooms...\n");
			first = true;
			for (ListIterator<RoomState> it=roomList.listIterator(); it.hasNext(); ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				int roomId = it.nextIndex();
				String roomName = blueprintSystems.getSystemNameByRoomId( roomId );
				if (roomName == null) roomName = "Empty";
				result.append(String.format("RoomId: %2d (%s)\n", roomId, roomName));
				result.append(it.next().toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nHull Breaches...\n");
			int breachId = -1;
			first = true;
			for (Map.Entry<Point, Integer> entry : breachMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }

				Point breachCoord = entry.getKey();
				int breachHealth = entry.getValue().intValue();

				result.append(String.format("BreachId: %2d, Raw Coords: %2d,%2d (-Layout Offset: %2d,%2d)\n", ++breachId, breachCoord.x, breachCoord.y, breachCoord.x-shipLayout.getOffsetX(), breachCoord.y-shipLayout.getOffsetY()));
				result.append(String.format("  Breach HP: %3d\n", breachHealth));
			}

			result.append("\nDoors...\n");
			int doorId = -1;
			first = true;
			for (Map.Entry<int[], DoorState> entry : doorMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }

				int[] doorCoord = entry.getKey();
				DoorState d = entry.getValue();
				String orientation = (doorCoord[2]==1 ? "V" : "H");

				result.append(String.format("DoorId: %2d (%2d,%2d,%2s)\n", ++doorId, doorCoord[0], doorCoord[1], orientation));
				result.append(d.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nWeapons...\n");
			first = true;
			for (WeaponState w : weaponList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(w.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nDrones...\n");
			first = true;
			for (DroneState d : droneList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(d.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nAugments...\n");
			for (String augmentId : augmentIdList) {
				result.append(String.format("AugmentId: %s\n", augmentId));
			}
			
			result.append("\nCargo...\n");
			for (String cargoItemId : cargoIdList) {
				result.append(String.format("CargoItemId: %s\n", cargoItemId));
			}

			return result.toString();
		}
	}



	public class StartingCrewState {
		private String name, race;

		public StartingCrewState(String name, String race) {
			this.name = name;
			this.race = race;
		}

		public String getName() {
			return name;
		}

		public String getRace() {
			return race;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Name: %s\n", name));
			result.append(String.format("Race: %s\n", race));
			return result.toString();
		}
	}



	public class CrewState {
		// TODO: magic numbers.
		// Make these static when the class gets its own file.
		public final int MASTERY_INTERVAL_PILOT = 15;
		public final int MASTERY_INTERVAL_ENGINE = 15;
		public final int MASTERY_INTERVAL_SHIELD = 55;
		public final int MASTERY_INTERVAL_WEAPON = 65;
		public final int MASTERY_INTERVAL_REPAIR = 18;
		public final int MASTERY_INTERVAL_COMBAT = 8;

		// Neither Crystal crews' lockdown, nor its cooldown is stored.
		// Zoltan-produced power is not stored in SystemState.

		private String name, race;
		private boolean enemyBoardingDrone = false;
		private int health;
		private int blueprintRoomId;
		private int roomSquare;  // 0-based, L-to-R wrapped row.
		private boolean playerControlled;
		private int pilotSkill, engineSkill, shieldSkill;
		private int weaponSkill, repairSkill, combatSkill;
		private int repairs, combatKills, pilotedEvasions;
		private int jumpsSurvived, skillMasteries;
		private int x, y;
		private int gender;  // 1=Male, 0=Female.

		private int unknownAlpha;

		public CrewState() {
		}

		public void setName( String s ) {name = s; }
		public void setRace( String s ) {race = s; }
		public void setHealth( int n ) {health = n; }
		public void setX( int x ) { this.x = x; };
		public void setY( int y ) { this.y = y; };
		public void setRoomId( int n ) {blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setPilotSkill( int n ) {pilotSkill = n; }
		public void setEngineSkill( int n ) {engineSkill = n; }
		public void setShieldSkill( int n ) {shieldSkill = n; }
		public void setWeaponSkill( int n ) {weaponSkill = n; }
		public void setRepairSkill( int n ) {repairSkill = n; }
		public void setCombatSkill( int n ) {combatSkill = n; }
		public void setGender( int gender ) { this.gender = gender; }
		public void setRepairs( int n ) { repairs = n; }
		public void setCombatKills( int n ) { combatKills = n; }
		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }
		public void setSkillMasteries( int n ) { skillMasteries = n; }

		/**
		 * Sets whether this crew member is a hostile drone.
		 * Bizarrely, this trumps race and playerControlled.
		 *
		 * Presumably this is so intruders can persist without
		 * a ship, which would normally have a drones section
		 * to contain them.
		 */
		public void setEnemyBoardingDrone( boolean b ) {
			enemyBoardingDrone = b;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Name:              %s\n", name));
			result.append(String.format("Race:              %s\n", race));
			result.append(String.format("Enemy Drone:       %b\n", enemyBoardingDrone));
			result.append(String.format("Gender:            %s\n", (gender == 1 ? "Male" : "Female") ));
			result.append(String.format("Health:            %3d\n", health));
			result.append(String.format("RoomId:            %3d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %3d\n", roomSquare));
			result.append(String.format("Player Controlled: %b\n", playerControlled));
			result.append(String.format("Position:          %3d,%3d\n", x, y));
			result.append(String.format("Pilot Skill:       %3d (Mastery Interval: %2d)\n", pilotSkill, MASTERY_INTERVAL_PILOT));
			result.append(String.format("Engine Skill:      %3d (Mastery Interval: %2d)\n", engineSkill, MASTERY_INTERVAL_ENGINE));
			result.append(String.format("Shield Skill:      %3d (Mastery Interval: %2d)\n", shieldSkill, MASTERY_INTERVAL_SHIELD));
			result.append(String.format("Weapon Skill:      %3d (Mastery Interval: %2d)\n", weaponSkill, MASTERY_INTERVAL_WEAPON));
			result.append(String.format("Repair Skill:      %3d (Mastery Interval: %2d)\n", repairSkill, MASTERY_INTERVAL_REPAIR));
			result.append(String.format("Combat Skill:      %3d (Mastery Interval: %2d)\n", combatSkill, MASTERY_INTERVAL_COMBAT));
			result.append(String.format("Repairs:           %3d\n", repairs));
			result.append(String.format("Combat Kills:      %3d\n", combatKills));
			result.append(String.format("Piloted Evasions:  %3d\n", pilotedEvasions));
			result.append(String.format("Jumps Survived:    %3d\n", jumpsSurvived));
			result.append(String.format("Skill Masteries:   %3d\n", skillMasteries));
			return result.toString();
		}
	}



	public class SystemState {
		private String name;
		private int capacity = 0;
		private int power = 0;
		private int damagedBars = 0;      // Number of unusable power bars.
		private int ionizedBars = 0;      // Number of disabled power bars; -1 while cloaked.
		private int repairProgress = 0;   // Turns bar yellow.
		private int burnProgress = 0;     // Turns bar red.
		private int miscTicks = Integer.MIN_VALUE;  // Millisecond counter.

		// ionizedBars may briefly be -1 initially when a system
		// disables itself. Then ionizedBars will be set to capacity+1.

		// miscTicks is reset upon loading.
		// Whatever needs timing will respond to it as it increments,
		// including resetting after intervals. If nothing needs it,
		// it may be 0, or more often, MIN_INT (signed 32bit \x0000_0080)
		// of the compiler that built FTL. This parser will translate that
		// to Java's equivalent minimum during reading, and back during
		// writing.
		//   Deionization: each bar counts to 5000.
		//
		// TODO:
		// Nearly every system has been observed with non-zero values,
		// but aside from Teleporter/Cloaking, normal use doesn't reliably
		// set such values. Might be unspecified garbage when not actively
		// counting. Sometimes has huge positive and negative values.

		public SystemState( String name ) {
			this.name = name;
		}

		public void setCapacity( int n ) { capacity = n; }
		public void setPower( int n ) { power = n; }
		public void setDamagedBars( int n ) { damagedBars = n; }
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public void setRepairProgress( int n ) { repairProgress = n; }
		public void setBurnProgress( int n ) { burnProgress = n; }
		public void setMiscTicks( int n ) { miscTicks = n; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (capacity > 0) {
				result.append(String.format("%s: %d/%d Power\n", name, power, capacity));
				result.append(String.format("Damaged Bars:    %3d\n", damagedBars));
				result.append(String.format("Ionized Bars:    %3d\n", ionizedBars));
				result.append(String.format("Repair Progress: %3d%%\n", repairProgress));
				result.append(String.format("Burn Progress:   %3d%%\n", burnProgress));
				result.append(String.format("Misc Ticks:      %s\n", (miscTicks==Integer.MIN_VALUE ? "N/A" : miscTicks) ));
			} else {
				result.append(String.format("%s: N/A\n", name));
			}
			return result.toString();
		}
	}



	public class RoomState {
		private int oxygen = 100;
		private ArrayList<int[]> squareList = new ArrayList<int[]>();

		public void setOxygen( int n ) { oxygen = n; }

		/**
		 * Adds a floor square to the room.
		 * Squares are indexed horizontally, left-to-right, wrapping
		 * into the next row down.
		 *
		 * Squares adjacent to a fire grow closer to igniting as
		 * time passes. Then a new fire spawns in them at full health.
		 *
		 * @param fireHealth 0 to 100.
		 * @param ignitionProgress 0 to 100.
		 * @param gamma -1?
		 */
		public void addSquare( int fireHealth, int ignitionProgress, int gamma ) {
			squareList.add( new int[] {fireHealth, ignitionProgress, gamma} );
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Oxygen: %3d%%\n", oxygen));
			for (int[] square : squareList) {
				result.append(String.format("Square: Fire HP: %3d, Ignition: %3d%% %2d?\n", square[0], square[1], square[2]));
			}
			return result.toString();
		}
	}



	public class DoorState {
		private boolean open;

		private int unknownAlpha;

		public DoorState( boolean open, int alpha ) {
			this.open = open;
			this.unknownAlpha = alpha;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Open: %b, Alpha?: %d\n", open, unknownAlpha));
			return result.toString();
		}
	}



	public class WeaponState {
		private String weaponId;
		private boolean armed;
		private int cooldownTicks;  // Increments from 0 until the weapon's cooldown. 0 when not armed.

		public WeaponState( String weaponId, boolean armed, int cooldownTicks ) {
			this.weaponId = weaponId;
			this.armed = armed;
			this.cooldownTicks = cooldownTicks;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			WeaponBlueprint weaponBlueprint = DataManager.get().getWeapon(weaponId);
			String cooldownString = ( weaponBlueprint!=null ? weaponBlueprint.getCooldown()+"" : "?" );

			result.append(String.format("WeaponId:       %s\n", weaponId));
			result.append(String.format("Armed:          %b\n", armed));
			result.append(String.format("Cooldown Ticks: %2d (max: %-2s)\n", cooldownTicks, cooldownString));
			return result.toString();
		}
	}



	public class DroneState {
		private String droneId;
		private boolean armed = false;
		private boolean playerControlled = true;  // False when not armed.
		private int x = -1, y = -1;        // -1 when not armed.
		private int blueprintRoomId = -1;  // -1 when not armed.
		private int roomSquare = -1;       // -1 when not armed.
		private int health = 1;


		public DroneState( String droneId ) {
			this.droneId = droneId;
		}

		public void setArmed( boolean b ) { armed = b; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setX( int n ) { x = n; }
		public void setY( int n ) { y = n; }
		public void setRoomId( int n ) { blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setHealth( int n ) { health = n; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("DroneId:           %s\n", droneId));
			result.append(String.format("Armed:             %b\n", armed));
			result.append(String.format("Health:            %3d\n", health));
			result.append(String.format("RoomId:            %3d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %3d\n", roomSquare));
			result.append(String.format("Player Controlled: %b\n", playerControlled));
			result.append(String.format("Position:          %3d,%3d\n", x, y));
			return result.toString();
		}
	}



	public enum FleetPresence { NONE, REBEL, FEDERATION, BOTH }

	public class BeaconState {
		
		private boolean visited;
		private String bgStarscapeImageInnerPath;
		private String bgSpriteImageInnerPath;
		private int bgSpritePosX, bgSpritePosY;
		private int unknownVisitedAlpha; // Sprite rotation in degrees? (observed values: 0, 180)
		
		private boolean seen; // True if player has been within one hop of beacon
		
		private boolean enemyPresent;
		private String shipEventId; // <ship> event from events_ships.xml
		private String shipBlueprintListId; // <blueprintList> to choose enemy ship from
		private int unknownEnemyPresentAlpha;
		
		private FleetPresence fleetPresence; // Determines which fleet image to use
		
		private boolean underAttack; // True if under attack by rebels (flashing red) in boss sector
		
		private boolean storePresent; // True if beacon contains a store (may require beacon to have been seen first)
		private StoreState store;

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append(String.format("Visited:           %b\n", visited));
			if ( visited ) {
				result.append(String.format("Bkg Starscape:     %s\n", bgStarscapeImageInnerPath));
				result.append(String.format("Bkg Sprite:        %s\n", bgSpriteImageInnerPath));
				result.append(String.format("Bkg Sprite Coords: %3d,%3d\n", bgSpritePosX, bgSpritePosY));
				result.append(String.format("Unknown:           %3d\n", unknownVisitedAlpha));
			}
			
			result.append(String.format("Seen:              %b\n", seen));
			
			result.append(String.format("Enemy Present:     %b\n", enemyPresent));
			if ( enemyPresent ) {
				result.append(String.format("  Ship Event ID:          %s\n", shipEventId));
				result.append(String.format("  Ship Blueprint List ID: %s\n", shipBlueprintListId));
				result.append(String.format("  Unknown:                %5d\n", unknownEnemyPresentAlpha));
			}
			
			result.append(String.format("Fleets Present:    %s\n", fleetPresence));
			
			result.append(String.format("Under Attack:      %b\n", underAttack));
			
			result.append(String.format("Store Present:     %b\n", storePresent));
			if ( storePresent ) {
				result.append( store.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			return result.toString();
		}
		
		public String getBgSpriteImageInnerPath() {
			return bgSpriteImageInnerPath;
		}
		public void setBgSpriteImageInnerPath(String bgSpriteImageInnerPath) {
			this.bgSpriteImageInnerPath = bgSpriteImageInnerPath;
		}
		public boolean isVisited() {
			return visited;
		}
		public void setVisited(boolean visited) {
			this.visited = visited;
		}
		public String getBgStarscapeImageInnerPath() {
			return bgStarscapeImageInnerPath;
		}
		public void setBgStarscapeImageInnerPath(String bgStarscapeImageInnerPath) {
			this.bgStarscapeImageInnerPath = bgStarscapeImageInnerPath;
		}
		public int getBgSpritePosX() {
			return bgSpritePosX;
		}
		public void setBgSpritePosX(int bgSpritePosX) {
			this.bgSpritePosX = bgSpritePosX;
		}
		public int getBgSpritePosY() {
			return bgSpritePosY;
		}
		public void setBgSpritePosY(int bgSpritePosY) {
			this.bgSpritePosY = bgSpritePosY;
		}
		public int getUnknownVisitedAlpha() {
			return unknownVisitedAlpha;
		}
		public void setUnknownVisitedAlpha(int unknownVisitedAlpha) {
			this.unknownVisitedAlpha = unknownVisitedAlpha;
		}
		public boolean isSeen() {
			return seen;
		}
		public void setSeen(boolean seen) {
			this.seen = seen;
		}
		public boolean isEnemyPresent() {
			return enemyPresent;
		}
		public void setEnemyPresent(boolean enemyPresent) {
			this.enemyPresent = enemyPresent;
		}
		public String getShipEventId() {
			return shipEventId;
		}
		public void setShipEventId(String shipEventId) {
			this.shipEventId = shipEventId;
		}
		public String getShipBlueprintListId() {
			return shipBlueprintListId;
		}
		public void setShipBlueprintListId(String shipBlueprintListId) {
			this.shipBlueprintListId = shipBlueprintListId;
		}
		public int getUnknownEnemyPresentAlpha() {
			return unknownEnemyPresentAlpha;
		}
		public void setUnknownEnemyPresentAlpha(int unknownEnemyPresentAlpha) {
			this.unknownEnemyPresentAlpha = unknownEnemyPresentAlpha;
		}
		public FleetPresence getFleetPresence() {
			return fleetPresence;
		}
		public void setFleetPresence(FleetPresence fleetPresence) {
			this.fleetPresence = fleetPresence;
		}
		public boolean isUnderAttack() {
			return underAttack;
		}
		public void setUnderAttack(boolean underAttack) {
			this.underAttack = underAttack;
		}
		public boolean isStorePresent() {
			return storePresent;
		}
		public void setStorePresent(boolean storePresent) {
			this.storePresent = storePresent;
		}
		public StoreState getStore() {
			return store;
		}
		public void setStore(StoreState store) {
			this.store = store;
		}
	}



	public class StoreState {
		
		private int fuel, missiles, droneParts;
		private StoreShelf topShelf, bottomShelf;
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append( String.format("Fuel:        %2d\n", fuel) );
			result.append( String.format("Missiles:    %2d\n", missiles) );
			result.append( String.format("Drone Parts: %2d\n", droneParts) );
			
			result.append( "\nTop Shelf...\n" );
			result.append( topShelf.toString().replaceAll("(^|\n)(.+)", "$1  $2") );

			result.append( "\nBottom Shelf...\n" );
			result.append( bottomShelf.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			
			return result.toString();
		}
		
		public int getFuel() {
			return fuel;
		}
		public void setFuel(int fuel) {
			this.fuel = fuel;
		}
		public int getMissiles() {
			return missiles;
		}
		public void setMissiles(int missiles) {
			this.missiles = missiles;
		}
		public int getDroneParts() {
			return droneParts;
		}
		public void setDroneParts(int droneParts) {
			this.droneParts = droneParts;
		}
		public StoreShelf getTopShelf() {
			return topShelf;
		}
		public void setTopShelf(StoreShelf topShelf) {
			this.topShelf = topShelf;
		}
		public StoreShelf getBottomShelf() {
			return bottomShelf;
		}
		public void setBottomShelf(StoreShelf bottomShelf) {
			this.bottomShelf = bottomShelf;
		}
		
	}

	public enum StoreItemType { WEAPON, DRONE, AUGMENT, CREW, SYSTEM };
	
	public class StoreShelf {
		
		private StoreItemType itemType;
		
		private List<StoreItem> items;
		
		public StoreShelf() {
			items = new ArrayList<StoreItem>(3);
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append( String.format("Item Type: %s\n", itemType) );
			for (StoreItem item : items) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append( item.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}
			
			return result.toString();
		}

		public StoreItemType getItemType() {
			return itemType;
		}
		public void setItemType(StoreItemType itemType) {
			this.itemType = itemType;
		}
		public void addItem( StoreItem item ) {
			items.add( item );
		}
		
	}
	
	public class StoreItem {
		private boolean available;
		private String itemId;

		public StoreItem(boolean available, String itemId) {
			this.available = available;
			this.itemId = itemId;
		}

		@Override
		public String toString() {
			return String.format("%s (%s)\n", itemId, (available ? "Available" : "Sold Out"));
		}
	}



	public class RebelFlagshipState {
		private String[] shipBlueprintIds;
		private int pendingStage = 1;
		private LinkedHashMap<Integer, Boolean> occupancyMap = new LinkedHashMap<Integer, Boolean>();

		/**
		 * Constructor.
		 * This info is not present in saved games until after engaging
		 * the rebel flagship in sector 8 for the first time.
		 *
		 * @param shipBlueprintIds The versions about to be fought
		 *                         (BOSS_1/BOSS_2/BOSS_3)
		 */
		public RebelFlagshipState( String[] shipBlueprintIds ) {
			this.shipBlueprintIds = shipBlueprintIds;
		}

		/**
		 * Sets the next version of the flagship that will be encountered (1-based).
		 */
		public void setPendingStage( int pendingStage ) {
			if ( pendingStage <= 0 || pendingStage > shipBlueprintIds.length )
				throw new IndexOutOfBoundsException( "Attempted to set 1-based flagship stage "+ pendingStage +" of "+ shipBlueprintIds.length +" total" );
			this.pendingStage = pendingStage;
		}

		/**
		 * Sets whether a room had a crew member in the last seen layout.
		 *
		 * Stage 1 sets this, but doesn't read it.
		 * Fleeing stage 1, altering these bytes, then returning
		 * only results in a fresh fight.
		 *
		 * Upon first engaging stage 2, the layout is migrated.
		 * Previous roomIds are truncated to the new layout's count.
		 * (The blueprints happen to have matching low ids.)
		 *
		 *   Stage 1: 0x13=19 rooms
		 *   Stage 2: 0x0F=15 rooms
		 *   Stage 3: 0x0B=11 rooms
		 *
		 * Stage 2 will read altered bytes on additional skirmishes.
		 *
		 * Stage 3 probably will, too. (TODO: Confirm this.)
		 *
		 * @param roomId a room in the last seen stage's shipLayout
		 * @param b true if there was crew, false otherwise
		 */
		public void setPreviousOccupancy( int roomId, boolean b ) {
			occupancyMap.put( new Integer(roomId), new Boolean(b) );
		}

		@Override
		public String toString() {
			// Use the first, most complete, blueprint for room names.
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBlueprintIds[0] );
			if ( shipBlueprint == null )
				throw new RuntimeException( String.format("Could not find blueprint for flagship: %s", shipBlueprintIds[0]) );

			ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();


			StringBuilder result = new StringBuilder();

			result.append( String.format("Pending Ship Type: %s\n", shipBlueprintIds[pendingStage-1] ) );

			result.append( "\nOccupancy of Last Seen Type...\n" );
			for (Map.Entry<Integer, Boolean> entry : occupancyMap.entrySet()) {
				int roomId = entry.getKey().intValue();
				boolean b = entry.getValue().booleanValue();

				String roomName = blueprintSystems.getSystemNameByRoomId( roomId );
				if (roomName == null) roomName = "Empty";

				result.append( String.format("RoomId: %2d (%-10s), Occupied: %b\n", roomId, roomName, b) );
			}

			return result.toString();
		}
	}

}
