package net.blerf.ftl.parser;

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

			gameState.addMysteryBytes( new MysteryBytes(in, 8) );

			int stateVarCount = readInt(in);
			for (int i=0; i < stateVarCount; i++) {
				String stateVarId = readString(in);
				Integer stateVarValue = new Integer(readInt(in));
				gameState.setStateVar(stateVarId, stateVarValue);
			}

			ShipState playerShipState = readShip( in, true );
			gameState.setPlayerShipState( playerShipState );

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
		String shipLayoutId = readString(in);
		ShipState shipState = new ShipState(shipName, shipBlueprintId, shipLayoutId, playerControlled);

		// Use this for room and door info later.
		ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);

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

		LinkedHashMap<int[], EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for (int[] doorCoord : layoutDoorMap.keySet()) {
			shipState.addDoor( doorCoord, readDoor(in) );
		}

		shipState.addMysteryBytes( new MysteryBytes(in, 4) );

		int weaponCount = readInt(in);
		for (int i=0; i < weaponCount; i++) {
			String weaponId = readString(in);
			int weaponArmed = readInt(in);
			int weaponAlpha = readInt(in);  // ? (0 when not armed)
			shipState.addWeapon( new WeaponState(weaponId, weaponArmed, weaponAlpha) );
		}

		int droneCount = readInt(in);
		for (int i=0; i < droneCount; i++) {
			shipState.addDrone( readDrone(in) );
		}

		int augmentCount = readInt(in);
		for (int i=0; i < augmentCount; i++) {
			shipState.addAugmentId( readString(in) );
		}

		// The next 8 bytes might belong in the ship. Dunno.

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
		crew.setBeta( readInt(in) );         // 52,507,192,262,297,192?
		crew.setGamma( readInt(in) );        // 192,157,157,262,122,192?
		crew.setRoomId( readInt(in) );
		crew.setRoomSquare( readInt(in) );   // 0-based, as a wrapped H row.
		crew.setEpsilon( readInt(in) );      // Always 1?
		crew.setPilotSkill( readInt(in) );
		crew.setEngineSkill( readInt(in) );
		crew.setShieldSkill( readInt(in) );
		crew.setWeaponSkill( readInt(in) );  // Maybe
		crew.setRepairSkill( readInt(in) );
		crew.setDigamma( readInt(in) );      // ?
		crew.setZeta( readInt(in) );         // Always 1?
		crew.setEta( readInt(in) );          // Matches repair?
		crew.setTheta( readInt(in) );        // Matches digamma?
		crew.setCombatSkill( readInt(in) );  // Maybe
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
			system.addMysteryBytes( new MysteryBytes(in, 12) );
			system.setRepairProgress( readInt(in) );
			system.setAlpha( readInt(in) );
		}
		return system;
	}

	private RoomState readRoom( InputStream in, int squaresH, int squaresV ) throws IOException {
		RoomState room = new RoomState();
		room.setOxygen( readInt(in) );

		// TODO: Find out if this should loop v first instead.
		for (int h=0; h < squaresH; h++) {
			for (int v=0; v < squaresV; v++) {
				// Dunno what these ints are. One of em's likely for fire.
				// Values in the wild: 0 / 0 / -1.
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
		// This func won't work because it doesn't account for all bytes.
		// And the BeaconState class isn't fully defined yet.

		BeaconState beacon = new BeaconState();

		String bgStarscapeImageInnerPath = readString(in);
		String bgSpriteImageInnerPath = readString(in);
		int bgSpritePosX = readInt(in);
		int bgSpritePosY = readInt(in);
		beacon.setBackground(bgStarscapeImageInnerPath, bgSpriteImageInnerPath, bgSpritePosX, bgSpritePosY);

		readInt(in);  // ?
		readInt(in);  // ?
		readInt(in);  // A 1 might mean event info follows?

		// These are sometimes present (e.g., beacons covered by the fleet).
		String eventShipName = readString(in);
		String eventAutoBlueprintId = readString(in);
		readInt(in);  // ?

		// Mystery bytes...

		return beacon;
	}



	// Stash state classes here until they're finalized.

	public class SavedGameState {
		public String playerShipName = "";
		public String playerShipBlueprintId = "";
		public HashMap<String,Integer> stateVars = new HashMap<String,Integer>();
		public ShipState playerShipState = null;
		public ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

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

		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Name: %s\n", playerShipName));
			result.append(String.format("Ship Type: %s\n", playerShipBlueprintId));

			result.append("\nState Vars...\n");
			for (Map.Entry<String, Integer> entry : stateVars.entrySet()) {
				result.append(String.format("%s: %d\n", entry.getKey(), entry.getValue().intValue()));
			}

			result.append("\nPlayer Ship...\n");
			result.append(playerShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\n");

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
		public boolean playerControlled = false;
		public String shipName, shipBlueprintId, shipLayoutId;
		public ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		public int hullAmt, fuelAmt, dronePartsAmt, missilesAmt, scrapAmt;
		public ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		public int reservePowerCapacity;
		public ArrayList<SystemState> systemList = new ArrayList<SystemState>();
		public ArrayList<RoomState> roomList = new ArrayList<RoomState>();
		public LinkedHashMap<int[], DoorState> doorMap = new LinkedHashMap<int[], DoorState>();
		public ArrayList<WeaponState> weaponList = new ArrayList<WeaponState>();
		public ArrayList<DroneState> droneList = new ArrayList<DroneState>();
		public ArrayList<String> augmentIdList = new ArrayList<String>();
		public ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public ShipState(String shipName, String shipBlueprintId, String shipLayoutId, boolean playerControlled) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprintId;
			this.shipLayoutId = shipLayoutId;
			this.playerControlled = playerControlled;
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
		 * Adds a door.
		 *
		 * @param WallXY+Vertical coordinates, as in ShipLayout's setDoor().
		 */
		public void addDoor( int[] doorCoord, DoorState d ) {
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

		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

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
			result.append(String.format("Ship Name: %s\n", shipName));
			result.append(String.format("Ship Type: %s\n", shipBlueprintId));
			result.append(String.format("Ship Layout: %s\n", shipLayoutId));

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

			result.append("\nDoors...\n");
			int doorId = -1;
			first = true;
			for (Map.Entry<int[], DoorState> entry : doorMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }

				int[] doorCoord = entry.getKey();
				DoorState d = entry.getValue();
				String orientation = (doorCoord[2]==1 ? "V" : "H");

				result.append(String.format("DoorId: %2d (%d,%d,%s)\n", ++doorId, doorCoord[0], doorCoord[1], orientation));
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
			result.append("\n");

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

		private int unknownAlpha, unknownBeta, unknownGamma;
		private int unknownEpsilon, unknownDigamma, unknownZeta, unknownEta;
		private int unknownTheta, unknownIota;

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

		public void setAlpha( int n ) { unknownAlpha = n; }
		public void setBeta( int n ) { unknownBeta = n; }
		public void setGamma( int n ) { unknownGamma = n; }
		public void setEpsilon( int n ) { unknownEpsilon = n; }
		public void setDigamma( int n ) { unknownDigamma = n; }
		public void setZeta( int n ) { unknownZeta = n; }
		public void setEta( int n ) { unknownEta = n; }
		public void setTheta( int n ) { unknownTheta = n; }
		public void setIota( int n ) { unknownIota = n; }

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
			result.append(String.format("Weapon Skill?:  %3d\n", weaponSkill));
			result.append(String.format("Repair Skill:   %3d\n", repairSkill));
			result.append(String.format("Combat Skill?:  %3d\n", combatSkill));
			result.append(String.format("Jumps Survived: %3d\n", jumpsSurvived));
			result.append("/ / / Unknowns / / /\n");
			result.append(String.format("Alpha:          %3d\n", unknownAlpha));
			result.append(String.format("Beta:           %3d\n", unknownBeta));
			result.append(String.format("Gamma:          %3d\n", unknownGamma));
			result.append(String.format("Epsilon:        %3d\n", unknownEpsilon));
			result.append(String.format("Digamma:        %3d\n", unknownDigamma));
			result.append(String.format("Zeta:           %3d\n", unknownZeta));
			result.append(String.format("Eta:            %3d\n", unknownEta));
			result.append(String.format("Theta:          %3d\n", unknownTheta));
			result.append(String.format("Iota:           %3d\n", unknownIota));
			return result.toString();
		}
	}



	public class SystemState {
		private String name;
		private int capacity = 0;
		private int power = 0;
		private int repairProgress = 0;

		private int unknownAlpha = 0;

		public ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		public SystemState( String name ) {
			this.name = name;
		}

		public void setCapacity( int n ) { capacity = n; }
		public void setPower( int n ) { power = n; }
		public void setRepairProgress( int n ) { repairProgress = n; }
		public void setAlpha( int n ) { unknownAlpha = n; }

		public void addMysteryBytes( MysteryBytes m ) { mysteryList.add(m); }

		public String toString() {
			StringBuilder result = new StringBuilder();
			if (capacity > 0) {
				result.append(String.format("%s: %d/%d Power\n", name, power, capacity));
				result.append("/ / / Unknowns / / /\n");
				result.append(String.format("Repair Progress: %d%%\n", repairProgress));
				result.append(String.format("Alpha:           %d\n", unknownAlpha));
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
		public ArrayList<int[]> squareList = new ArrayList<int[]>();

		public void setOxygen( int n ) { oxygen = n; }

		public void addSquare( int alpha, int beta, int gamma ) {
			squareList.add( new int[] {alpha, beta, gamma} );
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Oxygen: %3d%%\n", oxygen));
			result.append("/ / / Unknowns / / /\n");
			for (int[] square : squareList) {
				result.append(String.format("Square: %2d? %2d? %2d?\n", square[0], square[1], square[2]));
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

		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Open: %d, Alpha?: %d\n", open, unknownAlpha));
			return result.toString();
		}
	}



	public class WeaponState {
		private String weaponId;
		private int armed;
		private int unknownAlpha;

		public WeaponState( String weaponId, int armed, int alpha ) {
			this.weaponId = weaponId;
			this.armed = armed;
			this.unknownAlpha = alpha;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("WeaponId: %s\n", weaponId));
			result.append(String.format("Armed: %d\n", armed));
			result.append("/ / / Unknowns / / /\n");
			result.append(String.format("Alpha: %d\n", unknownAlpha));
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
		private String bgStarscapeImageInnerPath;
		private String bgSpriteImageInnerPath;
		private int bgSpritePosX, bgSpritePosY;

		public void setBackground( String starscapeInnerPath, String spriteInnerPath, int spriteX, int spriteY ) {
			bgStarscapeImageInnerPath = starscapeInnerPath;
			bgSpriteImageInnerPath = spriteInnerPath;
			bgSpritePosX = spriteX;
			bgSpritePosY = spriteY;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Bkg Starscape: %s\n", bgStarscapeImageInnerPath));
			result.append(String.format("Bkg Sprite: %s\n", bgSpriteImageInnerPath));
			result.append(String.format("Bkg Sprite Coords: %d,%d\n", bgSpritePosX, bgSpritePosY));
			return result.toString();
		}
	}

}
