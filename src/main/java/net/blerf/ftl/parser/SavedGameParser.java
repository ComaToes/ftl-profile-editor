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

			// Mystery bytes x24
			gameState.addMysteryBytes( new MysteryBytes(in, 24) );

			String playerShipName = readString(in);         // Redundant.
			String playerShipBlueprintId = readString(in);  // Redundant.
			gameState.setPlayerShipInfo( playerShipName, playerShipBlueprintId );

			gameState.setSectorNumber( readInt(in) );

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
			
			gameState.sectorLayoutSeed = readInt(in);
			
			// Pixel offset from far right of sector map
			gameState.rebelFleetOffset = readInt(in);
			
			gameState.addMysteryBytes( new MysteryBytes(in, 24) );
			
			// Variable length unknown list. Need to read to get to the right position for beacon list
			int unknownCount = readInt(in);
			List<Integer> mil = new ArrayList<Integer>();
			for (int i=0; i < unknownCount; i++) {
				mil.add( readInt(in) );
			}
			gameState.mysteryIntList = mil;
			
			gameState.addMysteryBytes( new MysteryBytes(in, 8) );
			
			int beaconCount = readInt(in);
			for (int i=0; i < beaconCount; i++) {
				gameState.addBeacon( readBeacon(in) );
			}

			// Mystery bytes (including recent beacon info)...
			int bytesRemaining = (int)(in.getChannel().size() - in.getChannel().position());
			gameState.addMysteryBytes( new MysteryBytes(in, bytesRemaining) );

			return gameState;  // The finally block will still be executed.

		} finally {
			try {if (in != null) in.close();}
			catch (IOException e) {}

			try {if (layoutStream != null) in.close();}
			catch (IOException e) {}
		}
	}

	private ShipState readShip( InputStream in, boolean playerControlled ) throws IOException {

		String shipBlueprintId = readString(in);  // blueprints.xml / autoBlueprints.xml.
		String shipName = readString(in);
		String pseudoLayoutId = readString(in);   // ?

		ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
		if ( shipBlueprint == null )
			throw new RuntimeException( String.format("Could not find blueprint for %s ship: %s", (playerControlled ? "player" : "non-player"), shipName) );

		String shipLayoutId = shipBlueprint.getLayout();

		// Use this for room and door info later.
		ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
		if ( shipLayout == null )
			throw new RuntimeException( String.format("Could not find layout for %s ship: %s", (playerControlled ? "player" : "non-player"), shipName) );

		ShipState shipState = new ShipState(shipName, shipBlueprintId, shipLayoutId, playerControlled);
		shipState.setPseudoLayoutId( pseudoLayoutId );

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

		int warningLightCount = readInt(in);
		for (int i=0; i < warningLightCount; i++) {
			shipState.setWarningLight( readInt(in), readInt(in), readInt(in) );
		}

		LinkedHashMap<int[], EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for (int[] doorCoord : layoutDoorMap.keySet()) {
			shipState.setDoor( doorCoord[0], doorCoord[1], doorCoord[2], readDoor(in) );
		}

		int weaponCount = readInt(in);
		for (int i=0; i < weaponCount; i++) {
			String weaponId = readString(in);
			int weaponArmed = readInt(in);
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

		int cargoCount = readInt(in);  // TODO: Nearby ships say 1, but have none?.
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
		crew.setAlpha( readInt(in) );        // Always 0?
		crew.setHealth( readInt(in) );
		crew.setX( readInt(in) );
		crew.setY( readInt(in) );
		crew.setRoomId( readInt(in) );
		crew.setRoomSquare( readInt(in) );   // 0-based, as a wrapped H row.
		crew.setEpsilon( readInt(in) );      // Always 1?
		crew.setPilotSkill( readInt(in) );
		crew.setEngineSkill( readInt(in) );
		crew.setShieldSkill( readInt(in) );
		crew.setWeaponSkill( readInt(in) );
		crew.setRepairSkill( readInt(in) );
		crew.setCombatSkill( readInt(in) );  // Maybe
		crew.setGender( readInt(in) );       // 1 == male, 0 == female (only human females are 0, prob because other races don't have female gfx)
		crew.setEta( readInt(in) );          // Matches repair?
		crew.setTheta( readInt(in) );        // Matches combat, sometimes less?
		crew.setKappa( readInt(in) );
		crew.setJumpsSurvived( readInt(in) );
		crew.setIota( readInt(in) );         // ?
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
			system.addMysteryBytes( new MysteryBytes(in, 4) );
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
		int alpha = readInt(in);  // 0. What else would a door have; damage?
		int open = readInt(in);   // 0=Closed, 1=Open
		DoorState door = new DoorState( alpha, open );
		return door;
	}

	private DroneState readDrone( InputStream in ) throws IOException {
		DroneState drone = new DroneState( readString(in) );
		drone.setArmed( readInt(in) );
		drone.setAlpha( readInt(in) );
		drone.setBeta( readInt(in) );
		drone.setGamma( readInt(in) );
		drone.setDelta( readInt(in) );
		drone.setEpsilon( readInt(in) );
		drone.setDigamma( readInt(in) );
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
			case 3: shelf.setItemType( StoreItemType.CREW ); break; // TODO: this is a guess. no sample save to verify
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



	// Stash state classes here until they're finalized.

	public class SavedGameState {
		private String playerShipName = "";
		private String playerShipBlueprintId = "";
		private int sectorNumber = 1;
		private HashMap<String, Integer> stateVars = new HashMap<String, Integer>();
		private ShipState playerShipState = null;
		private int sectorLayoutSeed, rebelFleetOffset;
		private List<Integer> mysteryIntList;
		private List<BeaconState> beacons = new ArrayList<BeaconState>();
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public void setSectorNumber( int n ) { sectorNumber = n; }

		/**
		 * Sets a state var.
		 * The following ids have been seen in the wild:
		 * blue_alien, dead_crew, destroyed_rock, env_danger, fired_shot,
		 * killed_crew, nebula, reactor_upgrade, store_purchase, store_repair,
		 * system_upgrade, teleported, used_missile, weapon_upgrade.
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

		/**
		 * Set redundant player ship info.
		 */
		public void setPlayerShipInfo( String shipName, String shipBlueprintId ) {
			playerShipName = shipName;
			playerShipBlueprintId = shipBlueprintId;
		}

		public void setPlayerShipState( ShipState shipState ) {
			this.playerShipState = shipState;
		}

		public void addBeacon( BeaconState beacon ) {
			beacons.add( beacon );
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
			result.append(String.format("Sector: %d\n", sectorNumber));

			result.append("\nState Vars...\n");
			for (Map.Entry<String, Integer> entry : stateVars.entrySet()) {
				result.append(String.format("%s: %d\n", entry.getKey(), entry.getValue().intValue()));
			}

			result.append("\nPlayer Ship...\n");
			if ( playerShipState != null )
				result.append(playerShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nSector Data...\n");
			result.append( String.format("Sector Layout Seed: %d\n", sectorLayoutSeed) );
			result.append( String.format("Rebel Fleet Offset: %d\n", rebelFleetOffset) );
			result.append( String.format("Mystery Int List: %s\n", mysteryIntList) );
			
			result.append("\nSector Beacons...\n");
			int beaconId = 0;
			first = true;
			for( BeaconState beacon: beacons ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append( String.format("BeaconId: %2d\n", beaconId++) );
				result.append( beacon.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

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
		private boolean playerControlled = false;
		private String shipName, shipBlueprintId, shipLayoutId;
		private String pseudoLayoutId;
		private ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private int hullAmt, fuelAmt, dronePartsAmt, missilesAmt, scrapAmt;
		private ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity;
		private ArrayList<SystemState> systemList = new ArrayList<SystemState>();
		private ArrayList<RoomState> roomList = new ArrayList<RoomState>();
		private LinkedHashMap<Point, Integer> warningLightMap = new LinkedHashMap<Point, Integer>();
		private LinkedHashMap<int[], DoorState> doorMap = new LinkedHashMap<int[], DoorState>();
		private ArrayList<WeaponState> weaponList = new ArrayList<WeaponState>();
		private ArrayList<DroneState> droneList = new ArrayList<DroneState>();
		private ArrayList<String> augmentIdList = new ArrayList<String>();
		private ArrayList<String> cargoIdList = new ArrayList<String>();
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public ShipState(String shipName, String shipBlueprintId, String shipLayoutId, boolean playerControlled) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprintId;
			this.shipLayoutId = shipLayoutId;
			this.playerControlled = playerControlled;
		}

		/**
		 * Sets what resembles a ShipLayout id string, but isn't.
		 * TODO: Find out what this is for.
		 *
		 * Values in the wild:
		 *   jelly_croissant_pirate, rebel_long_pirate...
		 *
		 * The proper shipLayoutId comes from the ShipBlueprint.
		 */
		public void setPseudoLayoutId( String pseudoLayoutId ) {
			this.pseudoLayoutId = pseudoLayoutId;
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
		 * Adds a flashing red light.
		 * These are associated with hull breaches.
		 *
		 * @param x the 0-based Nth floor-square corner from the left
		 * @param y the 0-based Nth floor-square corner from the top
		 * @param breachHealth 0 to 100.
		 */
		public void setWarningLight( int x, int y, int breachHealth ) {
			warningLightMap.put( new Point(x, y), new Integer(breachHealth) );
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
		
		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

		@Override
		public String toString() {
			// The blueprint fetching might vary if !playerControlled.
			// See autoBlueprints.xml vs blueprints.xml.
			ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
			ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

			// Build a roomId-to-name lookup table.
			// But first, aggregate the rooms to test for nulls.
			HashMap<String, ShipBlueprint.SystemList.SystemRoom> roomNameMap = new HashMap<String, ShipBlueprint.SystemList.SystemRoom>();
			roomNameMap.put( "Pilot", blueprintSystems.getPilotRoom() );
			roomNameMap.put( "Doors", blueprintSystems.getDoorsRoom() );
			roomNameMap.put( "Sensors", blueprintSystems.getSensorsRoom() );
			roomNameMap.put( "Medbay", blueprintSystems.getMedicalRoom() );
			roomNameMap.put( "Oxygen", blueprintSystems.getLifeSupportRoom() );
			roomNameMap.put( "Shields", blueprintSystems.getShieldRoom() );
			roomNameMap.put( "Engines", blueprintSystems.getEngineRoom() );
			roomNameMap.put( "Weapons", blueprintSystems.getWeaponRoom() );
			roomNameMap.put( "Drone Ctrl", blueprintSystems.getDroneRoom() );
			roomNameMap.put( "Teleporter", blueprintSystems.getTeleporterRoom() );
			roomNameMap.put( "Cloaking", blueprintSystems.getCloakRoom() );
			// Artillery's non-unique, but it can be tested for null elsewhere.

			HashMap<Integer, String> roomIdNameMap = new HashMap<Integer, String>();

			for (Map.Entry<String, ShipBlueprint.SystemList.SystemRoom> entry : roomNameMap.entrySet()) {
				String systemName = entry.getKey();
				ShipBlueprint.SystemList.SystemRoom room = entry.getValue();
				if ( room == null ) continue;  // Ineligible systems will be null.
				roomIdNameMap.put(new Integer( room.getRoomId() ), systemName);
			}

			List<ShipBlueprint.SystemList.SystemRoom> artilleryRooms = blueprintSystems.getArtilleryRooms();
			if (artilleryRooms != null) {
				for (ShipBlueprint.SystemList.SystemRoom artilleryRoom : artilleryRooms) {
					roomIdNameMap.put(new Integer( artilleryRoom.getRoomId() ), "Artillery");
				}
			}

			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Name:   %s\n", shipName));
			result.append(String.format("Ship Type:   %s\n", shipBlueprintId));
			result.append(String.format("Ship Layout: %s (Actually %s)\n", shipLayoutId, pseudoLayoutId));

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
			first = true;
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
				String roomName = roomIdNameMap.get( new Integer(roomId) );
				if (roomName == null) roomName = "Empty";
				result.append(String.format("RoomId: %2d (%s)\n", roomId, roomName));
				result.append(it.next().toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nWarning Lights...\n");
			int warningLightId = -1;
			first = true;
			for (Map.Entry<Point, Integer> entry : warningLightMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }

				Point lightCoord = entry.getKey();
				int breachHealth = entry.getValue().intValue();

				result.append(String.format("LightId: %2d (%2d,%2d)\n", ++warningLightId, lightCoord.x, lightCoord.y));
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

			result.append("\nMystery Bytes...\n");
			first = true;
			for (MysteryBytes m : mysteryList) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(m.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
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
		private String name, race;
		private int health;
		private int blueprintRoomId, roomSquare;
		private int pilotSkill, engineSkill, shieldSkill;
		private int weaponSkill, repairSkill, combatSkill;
		private int jumpsSurvived;
		private int x, y;
		private int gender;

		private int unknownAlpha;
		private int unknownEpsilon, unknownDigamma, unknownEta;
		private int unknownTheta, unknownKappa, unknownIota;

		public CrewState() {
		}

		public void setName( String s ) {name = s; }
		public void setRace( String s ) {race = s; }
		public void setHealth( int n ) {health = n; }
		public void setRoomId( int n ) {blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setPilotSkill( int n ) {pilotSkill = n; }
		public void setEngineSkill( int n ) {engineSkill = n; }
		public void setShieldSkill( int n ) {shieldSkill = n; }
		public void setWeaponSkill( int n ) {weaponSkill = n; }
		public void setRepairSkill( int n ) {repairSkill = n; }
		public void setCombatSkill( int n ) {combatSkill = n; }
		public void setJumpsSurvived( int n ) {jumpsSurvived = n; }
		public void setX( int x ) { this.x = x; };
		public void setY( int y ) { this.y = y; };
		public void setGender( int gender ) { this.gender = gender; }

		public void setAlpha( int n ) { unknownAlpha = n; }
		public void setEpsilon( int n ) { unknownEpsilon = n; }
		public void setEta( int n ) { unknownEta = n; }
		public void setTheta( int n ) { unknownTheta = n; }
		public void setKappa( int n ) { unknownKappa = n; }
		public void setIota( int n ) { unknownIota = n; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Name: %s\n", name));
			result.append(String.format("Race: %s\n", race));
			result.append(String.format("Health:         %3d\n", health));
			result.append(String.format("RoomId:         %3d\n", blueprintRoomId));
			result.append(String.format("Room Square:    %3d\n", roomSquare));
			result.append(String.format("Pilot Skill:    %3d\n", pilotSkill));
			result.append(String.format("Engine Skill:   %3d\n", engineSkill));
			result.append(String.format("Shield Skill:   %3d\n", shieldSkill));
			result.append(String.format("Weapon Skill:   %3d\n", weaponSkill));
			result.append(String.format("Repair Skill:   %3d\n", repairSkill));
			result.append(String.format("Combat Skill?:  %3d\n", combatSkill));
			result.append(String.format("Jumps Survived: %3d\n", jumpsSurvived));
			result.append(String.format("Position:       (%d,%d)\n", x, y));
			result.append(String.format("Gender:         %s\n", gender == 1 ? "Male" : "Female" ));
			result.append("/ / / Unknowns / / /\n");
			result.append(String.format("Alpha:          %3d\n", unknownAlpha));
			result.append(String.format("Epsilon:        %3d\n", unknownEpsilon));
			result.append(String.format("Eta:            %3d\n", unknownEta));
			result.append(String.format("Theta:          %3d\n", unknownTheta));
			result.append(String.format("Kappa:          %3d\n", unknownKappa));
			result.append(String.format("Iota:           %3d\n", unknownIota));
			return result.toString();
		}
	}



	public class SystemState {
		private String name;
		private int capacity = 0;
		private int power = 0;
		private int damagedBars = 0;     // Number of unusable power bars.
		private int ionizedBars = 0;     // Number of disabled power bars.
		private int repairProgress = 0;  // Turns bar yellow.
		private int burnProgress = 0;    // Turns bar red.

		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public SystemState( String name ) {
			this.name = name;
		}

		public void setCapacity( int n ) { capacity = n; }
		public void setPower( int n ) { power = n; }
		public void setDamagedBars( int n ) { damagedBars = n; }
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public void setRepairProgress( int n ) { repairProgress = n; }
		public void setBurnProgress( int n ) { burnProgress = n; }

		public void addMysteryBytes( MysteryBytes m ) { mysteryList.add(m); }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (capacity > 0) {
				result.append(String.format("%s: %d/%d Power\n", name, power, capacity));
				result.append(String.format("Damaged Bars:    %d\n", damagedBars));
				result.append(String.format("Ionized Bars:    %d\n", ionizedBars));
				result.append(String.format("Repair Progress: %d%%\n", repairProgress));
				result.append(String.format("Burn Progress:   %d%%\n", burnProgress));
				result.append("/ / / Unknowns / / /\n");
				if ( mysteryList.size() > 0 ) {
					result.append("Mystery Bytes...\n");
					boolean first = true;
					for (MysteryBytes m : mysteryList) {
						if (first) { first = false; }
						else { result.append(",\n"); }
						result.append(m.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
					}
				}
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
			result.append("/ / / Unknowns / / /\n");
			for (int[] square : squareList) {
				result.append(String.format("Square: Fire HP: %3d, Ignition: %3d%% %2d?\n", square[0], square[1], square[2]));
			}
			return result.toString();
		}
	}



	public class DoorState {
		private int open;

		private int unknownAlpha;

		public DoorState( int alpha, int open ) {
			this.unknownAlpha = alpha;
			this.open = open;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Open: %d, Alpha?: %d\n", open, unknownAlpha));
			return result.toString();
		}
	}



	public class WeaponState {
		private String weaponId;
		private int armed;
		private int cooldownTicks;  // Increments from 0 until the weapon's cooldown.

		public WeaponState( String weaponId, int armed, int cooldownTicks ) {
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
			result.append(String.format("Armed:          %d\n", armed));
			result.append(String.format("Cooldown Ticks: %d (max: %s)\n", cooldownTicks, cooldownString));
			return result.toString();
		}
	}



	public class DroneState {
		private String droneId;
		private int armed;
		private int unknownAlpha, unknownBeta, unknownGamma, unknownDelta;
		private int unknownEpsilon, unknownDigamma;

		public DroneState( String droneId ) {
			this.droneId = droneId;
		}

		public void setArmed( int n ) { armed = n; }
		public void setAlpha( int n ) { unknownAlpha = n; }
		public void setBeta( int n ) { unknownBeta = n; }
		public void setGamma( int n ) { unknownGamma = n; }
		public void setDelta( int n ) { unknownDelta = n; }
		public void setEpsilon( int n ) { unknownEpsilon = n; }
		public void setDigamma( int n ) { unknownDigamma = n; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("DroneId: %s\n", droneId));
			result.append(String.format("Armed:   %3d\n", armed));
			result.append("/ / / Unknowns / / /\n");
			result.append(String.format("Alpha:   %3d\n", unknownAlpha));
			result.append(String.format("Beta:    %3d\n", unknownBeta));
			result.append(String.format("Gamma:   %3d\n", unknownGamma));
			result.append(String.format("Delta:   %3d\n", unknownDelta));
			result.append(String.format("Epsilon: %3d\n", unknownEpsilon));
			result.append(String.format("Digamma: %3d\n", unknownDigamma));
			return result.toString();
		}
	}


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
				result.append(String.format("Bkg Sprite Coords: %d,%d\n", bgSpritePosX, bgSpritePosY));
				result.append(String.format("Unknown:           %d\n", unknownVisitedAlpha));
			}
			
			result.append(String.format("Seen:              %b\n", seen));
			
			result.append(String.format("Enemy Present:     %b\n", enemyPresent));
			if ( enemyPresent ) {
				result.append(String.format("  Ship Event ID:          %s\n", shipEventId));
				result.append(String.format("  Ship Blueprint List ID: %s\n", shipBlueprintListId));
				result.append(String.format("  Unknown:                %d\n", unknownEnemyPresentAlpha));
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
	
	public enum FleetPresence { NONE, REBEL, FEDERATION, BOTH }
	
	public class StoreState {
		
		private int fuel, missiles, droneParts;
		private StoreShelf topShelf, bottomShelf;
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			
			result.append( String.format("Fuel: %d\n" , fuel) );
			result.append( String.format("Missiles: %d\n" , missiles) );
			result.append( String.format("Drone Parts: %d\n" , droneParts) );
			
			result.append( "\nTop Shelf:..." );
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
			return String.format("%s (%s)\n" , itemId, (available ? "Available" : "Sold Out"));
		}
	}

}
