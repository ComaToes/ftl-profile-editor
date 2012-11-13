package net.blerf.ftl.parser;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
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
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameParser extends DatParser {

	private static final Logger log = LogManager.getLogger(SavedGameParser.class);


	public SavedGameState readSavedGame( File datFile ) throws IOException {
		FileInputStream in = null;
		InputStream layoutStream = null;
		try {
			SavedGameState gameState = new SavedGameState();
			in = new FileInputStream(datFile);

			// This should always be 2.
			int headerAlpha = readInt(in);
			if ( headerAlpha != 2 )
				log.warn( "Unexpected first byte ("+ headerAlpha +"): it's either a bad file, or possibly too new for this tool" );

			gameState.setDifficultyEasy( readBool(in) );
			gameState.setTotalShipsDefeated( readInt(in) );
			gameState.setTotalBeaconsExplored( readInt(in) );
			gameState.setTotalScrapCollected( readInt(in) );
			gameState.setTotalCrewHired( readInt(in) );

			String playerShipName = readString(in);         // Redundant.
			gameState.setPlayerShipName( playerShipName );

			String playerShipBlueprintId = readString(in);  // Redundant.
			gameState.setPlayerShipBlueprintId( playerShipBlueprintId );

			int oneBasedSectorNumber = readInt(in);  // Redundant.

			// Always 0?
			gameState.setHeaderAlpha( readInt(in) );

			int stateVarCount = readInt(in);
			for (int i=0; i < stateVarCount; i++) {
				String stateVarId = readString(in);
				Integer stateVarValue = new Integer(readInt(in));
				gameState.setStateVar(stateVarId, stateVarValue);
			}

			ShipState playerShipState = readShip( in, true );
			gameState.setPlayerShipState( playerShipState );

			// Nearby ships have no cargo, so this isn't in readShip().
			int cargoCount = readInt(in);
			for (int i=0; i < cargoCount; i++) {
				gameState.addCargoItemId( readString(in) );
			}

			gameState.setSectorTreeSeed( readInt(in) );
			
			gameState.setSectorLayoutSeed( readInt(in) );
			
			gameState.setRebelFleetOffset( readInt(in) );
			
			gameState.setRebelFleetFudge( readInt(in) );

			gameState.setRebelPursuitMod( readInt(in) );

			gameState.setSectorHazardsVisible( readBool(in) );

			gameState.setRebelFlagshipVisible( readBool(in) );

			gameState.setRebelFlagshipHop( readInt(in) );

			gameState.setRebelFlagshipApproaching( readBool(in) );

			int sectorCount = readInt(in);
			for (int i=0; i < sectorCount; i++) {
				gameState.addSector( readBool(in) );
			}

			// The number on the sector map is this+1,
			// but the sector's type on the map is
			// unaffected when these bytes are modified.
			// All hazards and point-of-interest labels
			// will change, but not the beacons.
			// The sector tree is unaffected when modified.
			// Jumping from an exit beacon increments this
			// number and sets the header's sector number
			// to this+1.
			int sectorNumber = readInt(in);
			gameState.setSectorNumber( sectorNumber );

			gameState.setSectorIsHiddenCrystalWorlds( readBool(in) );
			
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

			RebelFlagshipState flagshipState = readRebelFlagship(in);
			gameState.setRebelFlagshipState( flagshipState );

			// The stream should end here.

			int bytesRemaining = (int)(in.getChannel().size() - in.getChannel().position());
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

	public void writeSavedGame( OutputStream out, SavedGameState gameState ) throws IOException {

		if ( gameState.getMysteryList().size() > 0 )
			log.warn( "The original saved game file contained mystery bytes, which will be omitted in the new file" );

		// This should always be 2.
		writeInt( out, 2 );

		writeBool( out, gameState.isDifficultyEasy() );
		writeInt( out, gameState.getTotalShipsDefeated() );
		writeInt( out, gameState.getTotalBeaconsExplored() );
		writeInt( out, gameState.getTotalScrapCollected() );
		writeInt( out, gameState.getTotalCrewHired() );

		writeString( out, gameState.getPlayerShipName() );
		writeString( out, gameState.getPlayerShipBlueprintId() );

		// Redundant 1-based sector number.
		writeInt( out, gameState.getSectorNumber()+1 );

		writeInt( out, gameState.getHeaderAlpha() );

		writeInt( out, gameState.getStateVars().size() );
		for (Map.Entry<String, Integer> entry : gameState.getStateVars().entrySet()) {
			writeString( out, entry.getKey() );
			writeInt( out, entry.getValue().intValue() );
		}

		writeShip( out, gameState.getPlayerShipState() );

		writeInt( out, gameState.getCargoIdList().size() );
		for (String cargoItemId : gameState.getCargoIdList()) {
			writeString( out, cargoItemId );
		}

		writeInt( out, gameState.getSectorTreeSeed() );
		writeInt( out, gameState.getSectorLayoutSeed() );
		writeInt( out, gameState.getRebelFleetOffset() );
		writeInt( out, gameState.getRebelFleetFudge() );
		writeInt( out, gameState.getRebelPursuitMod() );
		writeBool( out, gameState.areSectorHazardsVisible() );
		writeBool( out, gameState.isRebelFlagshipVisible() );
		writeInt( out, gameState.getRebelFlagshipHop() );
		writeBool( out, gameState.isRebelFlagshipApproaching() );

		writeInt( out, gameState.getSectorList().size() );
		for (Boolean visited : gameState.getSectorList()) {
			writeBool( out, visited.booleanValue() );
		}

		writeInt( out, gameState.getSectorNumber() );
		writeBool( out, gameState.isSectorHiddenCrystalWorlds() );

		writeInt( out, gameState.getBeaconList().size() );
		for (BeaconState beacon : gameState.getBeaconList()) {
			writeBeacon( out, beacon );
		}

		writeInt( out, gameState.getQuestEventMap().size() );
		for (Map.Entry<String, Integer> entry : gameState.getQuestEventMap().entrySet()) {
			writeString( out, entry.getKey() );
			writeInt( out, entry.getValue().intValue() );
		}

		writeInt( out, gameState.getDistantQuestEventList().size() );
		for (String questEventId : gameState.getDistantQuestEventList()) {
			writeString( out, questEventId );
		}

		writeInt( out, gameState.getCurrentBeaconId() );

		ShipState nearbyShip = gameState.getNearbyShipState();
		writeBool( out, (nearbyShip != null) );
		if ( nearbyShip != null ) {
			writeShip( out, nearbyShip );
		}

		writeRebelFlagship( out, gameState.getRebelFlagshipState() );

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

		ShipState shipState = new ShipState( shipName, shipBlueprintId, shipLayoutId, shipGfxBaseName, auto );

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
		ArrayList<String> systemIds = new ArrayList<String>();
		systemIds.add( SystemBlueprint.ID_SHIELDS );
		systemIds.add( SystemBlueprint.ID_ENGINES );
		systemIds.add( SystemBlueprint.ID_OXYGEN );
		systemIds.add( SystemBlueprint.ID_WEAPONS );
		systemIds.add( SystemBlueprint.ID_DRONE_CTRL );
		systemIds.add( SystemBlueprint.ID_MEDBAY );
		systemIds.add( SystemBlueprint.ID_PILOT );
		systemIds.add( SystemBlueprint.ID_SENSORS );
		systemIds.add( SystemBlueprint.ID_DOORS );
		systemIds.add( SystemBlueprint.ID_TELEPORTER );
		systemIds.add( SystemBlueprint.ID_CLOAKING );
		systemIds.add( SystemBlueprint.ID_ARTILLERY );

		shipState.setReservePowerCapacity( readInt(in) );
		for (String systemId : systemIds) {
			shipState.addSystem( readSystem(in, systemId) );
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

		// Doors are defined in the layout text file, but their
		// order is different at runtime. Vacuum-adjacent doors
		// are plucked out and moved to the end... for some
		// reason.
		LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> vacuumDoorMap = new LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>>();
		LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : layoutDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

			if ( doorInfo.get(ShipLayout.DoorInfo.ROOM_ID_A).intValue() == -1 ||
			     doorInfo.get(ShipLayout.DoorInfo.ROOM_ID_B).intValue() == -1 ) {
				vacuumDoorMap.put( doorCoord, doorInfo );
				continue;
			}
			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in) );
		}
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : vacuumDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in) );
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

		return shipState;
	}

	public void writeShip( OutputStream out, ShipState shipState ) throws IOException {
		String shipBlueprintId = shipState.getShipBlueprintId();

		ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
		if ( shipBlueprint == null )
			throw new RuntimeException( String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );

		String shipLayoutId = shipBlueprint.getLayout();

		ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
		if ( shipLayout == null )
			throw new RuntimeException( String.format("Could not find layout for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()) );


		writeString( out, shipBlueprintId );
		writeString( out, shipState.getShipName() );
		writeString( out, shipState.getShipGraphicsBaseName() );

		writeInt( out, shipState.getStartingCrewList().size() );
		for (StartingCrewState startingCrew : shipState.getStartingCrewList()) {
			writeStartingCrewMember( out, startingCrew );
		}

		writeInt( out, shipState.getHullAmt() );
		writeInt( out, shipState.getFuelAmt() );
		writeInt( out, shipState.getDronePartsAmt() );
		writeInt( out, shipState.getMissilesAmt() );
		writeInt( out, shipState.getScrapAmt() );

		writeInt( out, shipState.getCrewList().size() );
		for (CrewState crew : shipState.getCrewList()) {
			writeCrewMember( out, crew );
		}

		// System info is stored in this order.
		ArrayList<String> systemIds = new ArrayList<String>();
		systemIds.add( SystemBlueprint.ID_SHIELDS );
		systemIds.add( SystemBlueprint.ID_ENGINES );
		systemIds.add( SystemBlueprint.ID_OXYGEN );
		systemIds.add( SystemBlueprint.ID_WEAPONS );
		systemIds.add( SystemBlueprint.ID_DRONE_CTRL );
		systemIds.add( SystemBlueprint.ID_MEDBAY );
		systemIds.add( SystemBlueprint.ID_PILOT );
		systemIds.add( SystemBlueprint.ID_SENSORS );
		systemIds.add( SystemBlueprint.ID_DOORS );
		systemIds.add( SystemBlueprint.ID_TELEPORTER );
		systemIds.add( SystemBlueprint.ID_CLOAKING );
		systemIds.add( SystemBlueprint.ID_ARTILLERY );

		writeInt( out, shipState.getReservePowerCapacity() );

		Map<String, SystemState> systemMap = shipState.getSystemMap();
		for (String systemId : systemIds) {
			SystemState systemState = systemMap.get(systemId);
			if ( systemState != null )
				writeSystem( out, systemState );
			else
				writeInt( out, 0 );
		}

		for (RoomState room : shipState.getRoomList()) {
			writeRoom( out, room );
		}

		writeInt( out, shipState.getBreachMap().size() );
		for (Map.Entry<Point, Integer> entry : shipState.getBreachMap().entrySet()) {
			writeInt( out, entry.getKey().x );
			writeInt( out, entry.getKey().y );
			writeInt( out, entry.getValue().intValue() );
		}

		// Doors are defined in the layout text file, but their
		// order is different at runtime. Vacuum-adjacent doors
		// are plucked out and moved to the end... for some
		// reason.
		Map<ShipLayout.DoorCoordinate, DoorState> shipDoorMap = shipState.getDoorMap();
		LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> vacuumDoorMap = new LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>>();
		LinkedHashMap<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : layoutDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

			if ( doorInfo.get(ShipLayout.DoorInfo.ROOM_ID_A).intValue() == -1 ||
			     doorInfo.get(ShipLayout.DoorInfo.ROOM_ID_B).intValue() == -1 ) {
				vacuumDoorMap.put( doorCoord, doorInfo );
				continue;
			}
			writeDoor( out, shipDoorMap.get( doorCoord ) );
		}
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : vacuumDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();

			writeDoor( out, shipDoorMap.get( doorCoord ) );
		}

		writeInt( out, shipState.getWeaponList().size() );
		for (WeaponState weapon : shipState.getWeaponList()) {
			writeString( out, weapon.getWeaponId() );
			writeBool( out, weapon.isArmed() );
			writeInt( out, weapon.getCooldownTicks() );
		}

		writeInt( out, shipState.getDroneList().size() );
		for (DroneState drone : shipState.getDroneList()) {
			writeDrone( out, drone );
		}

		writeInt( out, shipState.getAugmentIdList().size() );
		for (String augmentId : shipState.getAugmentIdList()) {
			writeString( out, augmentId );
		}
	}

	private StartingCrewState readStartingCrewMember( InputStream in ) throws IOException {
		String crewRace = readString(in);
		String crewName = readString(in);
		StartingCrewState startingCrew = new StartingCrewState(crewName, crewRace);
		return startingCrew;
	}

	public void writeStartingCrewMember( OutputStream out, StartingCrewState startingCrew ) throws IOException {
		writeString( out, startingCrew.getRace() );
		writeString( out, startingCrew.getName() );
	}

	private CrewState readCrewMember( InputStream in ) throws IOException {
		CrewState crew = new CrewState();
		crew.setName( readString(in) );
		crew.setRace( readString(in) );
		crew.setEnemyBoardingDrone( readBool(in) );
		crew.setHealth( readInt(in) );
		crew.setSpriteX( readInt(in) );
		crew.setSpriteY( readInt(in) );
		crew.setRoomId( readInt(in) );
		crew.setRoomSquare( readInt(in) );
		crew.setPlayerControlled( readBool(in) );
		crew.setPilotSkill( readInt(in) );
		crew.setEngineSkill( readInt(in) );
		crew.setShieldSkill( readInt(in) );
		crew.setWeaponSkill( readInt(in) );
		crew.setRepairSkill( readInt(in) );
		crew.setCombatSkill( readInt(in) );
		crew.setMale( readBool(in) );
		crew.setRepairs( readInt(in) );
		crew.setCombatKills( readInt(in) );
		crew.setPilotedEvasions( readInt(in) );
		crew.setJumpsSurvived( readInt(in) );
		crew.setSkillMasteries( readInt(in) );
		return crew;
	}

	public void writeCrewMember( OutputStream out, CrewState crew ) throws IOException {
		writeString( out, crew.getName() );
		writeString( out, crew.getRace() );
		writeBool( out, crew.isEnemyBoardingDrone() );
		writeInt( out, crew.getHealth() );
		writeInt( out, crew.getSpriteX() );
		writeInt( out, crew.getSpriteY() );
		writeInt( out, crew.getRoomId() );
		writeInt( out, crew.getRoomSquare() );
		writeBool( out, crew.isPlayerControlled() );
		writeInt( out, crew.getPilotSkill() );
		writeInt( out, crew.getEngineSkill() );
		writeInt( out, crew.getShieldSkill() );
		writeInt( out, crew.getWeaponSkill() );
		writeInt( out, crew.getRepairSkill() );
		writeInt( out, crew.getCombatSkill() );
		writeBool( out, crew.isMale() );
		writeInt( out, crew.getRepairs() );
		writeInt( out, crew.getCombatKills() );
		writeInt( out, crew.getPilotedEvasions() );
		writeInt( out, crew.getJumpsSurvived() );
		writeInt( out, crew.getSkillMasteries() );
	}

	private SystemState readSystem( InputStream in, String systemId ) throws IOException {
		SystemState system = new SystemState( systemId );
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

			int deionizationTicks = readInt(in);
			if ( deionizationTicks == -2147483648 )
				deionizationTicks = Integer.MIN_VALUE;
			system.setDeionizationTicks( deionizationTicks );

			system.setRepairProgress( readInt(in) );
			system.setDamageProgress( readInt(in) );
		}
		return system;
	}

	public void writeSystem( OutputStream out, SystemState system ) throws IOException {
		writeInt( out, system.getCapacity() );
		if ( system.getCapacity() > 0 ) {
			writeInt( out, system.getPower() );
			writeInt( out, system.getDamagedBars() );
			writeInt( out, system.getIonizedBars() );

			if ( system.getDeionizationTicks() == Integer.MIN_VALUE )
				writeInt( out, -2147483648 );
			else
				writeInt( out, system.getDeionizationTicks() );

			writeInt( out, system.getRepairProgress() );
			writeInt( out, system.getDamageProgress() );
		}
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

	public void writeRoom( OutputStream out, RoomState room ) throws IOException {
		writeInt( out, room.getOxygen() );

		for (SquareState square : room.getSquareList()) {
			writeInt( out, square.fireHealth );
			writeInt( out, square.ignitionProgress );
			writeInt( out, square.gamma );
		}
	}

	private DoorState readDoor( InputStream in ) throws IOException {
		boolean open = readBool(in);
		boolean walkingThrough = readBool(in);
		DoorState door = new DoorState( open, walkingThrough );
		return door;
	}

	public void writeDoor( OutputStream out, DoorState door ) throws IOException {
		writeBool( out, door.isOpen() );
		writeBool( out, door.isWalkingThrough() );
	}

	private DroneState readDrone( InputStream in ) throws IOException {
		DroneState drone = new DroneState( readString(in) );
		drone.setArmed( readBool(in) );
		drone.setPlayerControlled( readBool(in) );
		drone.setSpriteX( readInt(in) );
		drone.setSpriteY( readInt(in) );
		drone.setRoomId( readInt(in) );
		drone.setRoomSquare( readInt(in) );
		drone.setHealth( readInt(in) );
		return drone;
	}

	public void writeDrone( OutputStream out, DroneState drone ) throws IOException {
		writeString( out, drone.getDroneId() );
		writeBool( out, drone.isArmed() );
		writeBool( out, drone.isPlayerControlled() );
		writeInt( out, drone.getSpriteX() );
		writeInt( out, drone.getSpriteY() );
		writeInt( out, drone.getRoomId() );
		writeInt( out, drone.getRoomSquare() );
		writeInt( out, drone.getHealth() );
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

	public void writeBeacon( OutputStream out, BeaconState beacon ) throws IOException {
		writeBool( out, beacon.isVisited() );
		if ( beacon.isVisited() ) {
			writeString( out, beacon.getBgStarscapeImageInnerPath() );
			writeString( out, beacon.getBgSpriteImageInnerPath() );
			writeInt( out, beacon.getBgSpritePosX() );
			writeInt( out, beacon.getBgSpritePosY() );
			writeInt( out, beacon.getUnknownVisitedAlpha() );
		}

		writeBool( out, beacon.isSeen() );

		writeBool( out, beacon.isEnemyPresent() );
		if ( beacon.isEnemyPresent() ) {
			writeString( out, beacon.getShipEventId() );
			writeString( out, beacon.getShipBlueprintListId() );
			writeInt( out, beacon.getUnknownEnemyPresentAlpha() );
		}

		FleetPresence fleetPresence = beacon.getFleetPresence();
		if ( fleetPresence == FleetPresence.NONE ) writeInt( out, 0 );
		else if ( fleetPresence == FleetPresence.REBEL ) writeInt( out, 1 );
		else if ( fleetPresence == FleetPresence.FEDERATION ) writeInt( out, 2 );
		else if ( fleetPresence == FleetPresence.BOTH ) writeInt( out, 3 );
		else throw new RuntimeException( "Unknown fleet presence: "+ fleetPresence );

		writeBool( out, beacon.isUnderAttack() );

		writeBool( out, beacon.isStorePresent() );
		if ( beacon.isStorePresent() ) {
			StoreState store = beacon.getStore();
			writeStoreShelf( out, store.getTopShelf() );
			writeStoreShelf( out, store.getBottomShelf() );
			writeInt( out, store.getFuel() );
			writeInt( out, store.getMissiles() );
			writeInt( out, store.getDroneParts() );
		}
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
			int available = readInt(in); // -1=no item, 0=bought already, 1=buyable
			if ( available < 0 )
				continue;
			String itemId = readString(in);
			shelf.addItem( new StoreItem( (available > 0), itemId) );
		}
		
		return shelf;
		
	}

	public void writeStoreShelf( OutputStream out, StoreShelf shelf ) throws IOException {

		StoreItemType itemType = shelf.getItemType();
		if ( itemType == StoreItemType.WEAPON ) writeInt( out, 0 );
		else if ( itemType == StoreItemType.DRONE ) writeInt( out, 1 );
		else if ( itemType == StoreItemType.AUGMENT ) writeInt( out, 2 );
		else if ( itemType == StoreItemType.CREW ) writeInt( out, 3 );
		else if ( itemType == StoreItemType.SYSTEM ) writeInt( out, 4 );
		else throw new RuntimeException( "Unknown store item type: "+ itemType );

		List<StoreItem> items = shelf.getItems();
		for (int i=0; i < 3; i++) {
			int available = -1;
			String itemId = null;
			if ( items.size() >= i ) {
				available = (items.get(i).isAvailable() ? 1 : 0);
				itemId = items.get(i).getItemId();
			}
			writeInt( out, available );
			if ( available >= 0 ) writeString( out, itemId );
		}
	}

	public RebelFlagshipState readRebelFlagship( InputStream in ) throws IOException {

		// TODO: Magic strings.
		String[] blueprintIds = new String[] {"BOSS_1", "BOSS_2", "BOSS_3"};

		RebelFlagshipState flagship = new RebelFlagshipState( blueprintIds );

		flagship.setPendingStage( readInt(in) );

		int previousRoomCount = readInt(in);
		for (int i=0; i < previousRoomCount; i++) {
			flagship.setPreviousOccupancy( i, readInt(in) );
		}

		return flagship;
	}

	public void writeRebelFlagship( OutputStream out, RebelFlagshipState flagship ) throws IOException {
		writeInt( out, flagship.getPendingStage() );

		writeInt( out, flagship.getOccupancyMap().size() );
		for (Map.Entry<Integer, Integer> entry : flagship.getOccupancyMap().entrySet()) {
			int occupantCount = entry.getValue().intValue();
			writeInt( out, occupantCount );
		}
	}



	// Stash state classes here until they're finalized.

	public static class SavedGameState {
		private boolean difficultyEasy = false;
		private int totalShipsDefeated = 0;
		private int totalBeaconsExplored = 0;
		private int totalScrapCollected = 0;
		private int totalCrewHired = 0;
		private String playerShipName = "";
		private String playerShipBlueprintId = "";
		private int sectorNumber = 1;
		private LinkedHashMap<String, Integer> stateVars = new LinkedHashMap<String, Integer>();
		private ShipState playerShipState = null;
		private ArrayList<String> cargoIdList = new ArrayList<String>();
		private int sectorTreeSeed = 42;      // Arbitrary default.
		private int sectorLayoutSeed = 42;    // Arbitrary default.
		private int rebelFleetOffset = -750;  // Arbitrary default.
		private int rebelFleetFudge = 100;    // Arbitrary default.
		private int rebelPursuitMod = 0;
		private boolean sectorHazardsVisible = false;
		private boolean rebelFlagshipVisible = false;
		private int rebelFlagshipHop = 0;
		private boolean rebelFlagshipApproaching = false;
		private ArrayList<Boolean> sectorList = new ArrayList<Boolean>();
		private boolean sectorIsHiddenCrystalWorlds = false;
		private ArrayList<BeaconState> beaconList = new ArrayList<BeaconState>();
		private LinkedHashMap<String, Integer> questEventMap = new LinkedHashMap<String, Integer>();
		private ArrayList<String> distantQuestEventList = new ArrayList<String>();
		private ShipState nearbyShipState = null;
		private int currentBeaconId = 0;
		private RebelFlagshipState rebelFlagshipState = null;
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		private int unknownHeaderAlpha = 0;

		public void setDifficultyEasy( boolean b ) { difficultyEasy = b; }
		public void setTotalShipsDefeated( int n ) { totalShipsDefeated = n; }
		public void setTotalBeaconsExplored( int n ) { totalBeaconsExplored = n; }
		public void setTotalScrapCollected( int n ) { totalScrapCollected = n; }
		public void setTotalCrewHired( int n ) { totalCrewHired = n; }

		public boolean isDifficultyEasy() { return difficultyEasy; }
		public int getTotalShipsDefeated() { return totalShipsDefeated; }
		public int getTotalBeaconsExplored() { return totalBeaconsExplored; }
		public int getTotalScrapCollected() { return totalScrapCollected; }
		public int getTotalCrewHired() { return totalCrewHired; }

		/** Sets redundant player ship name. */
		public void setPlayerShipName( String shipName) {
			playerShipName = shipName;
		}
		public String getPlayerShipName() { return playerShipName; }

		/** Sets redundant player ship blueprint. */
		public void setPlayerShipBlueprintId( String shipBlueprintId ) {
			playerShipBlueprintId = shipBlueprintId;
		}
		public String getPlayerShipBlueprintId() { return playerShipBlueprintId; }

		public void addCargoItemId( String cargoItemId ) {
			cargoIdList.add( cargoItemId );
		}

		public ArrayList<String> getCargoIdList() { return cargoIdList; }

		/**
		 * Sets the current sector's number (0-based).
		 *
		 * It's uncertain how soon sector-dependent events
		 * will take notice of changes. On the map, the
		 * number, all visible hazards, and
		 * point-of-interest labels will immediately
		 * change, but not the beacons' pixel positions.
		 *
		 * Ship encounters will not be immediately
		 * affected (TODO: turn #0 into #5, jump to the
		 * next sector, and see if the ships there are
		 * tough).
		 *
		 * Modifying this will not change the sector tree.
		 *
		 * TODO: Determine long-term effects of this.
		 * The Last Stand is baked into the sector tree,
		 * but weird things might happen at or above #7.
		 */
		public void setSectorNumber( int n ) { sectorNumber = n; }
		public int getSectorNumber() { return sectorNumber; }

		public void setHeaderAlpha( int n ) { unknownHeaderAlpha = n; }
		public int getHeaderAlpha() { return unknownHeaderAlpha; }

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

		public LinkedHashMap<String, Integer> getStateVars() { return stateVars; }

		public void setPlayerShipState( ShipState shipState ) {
			this.playerShipState = shipState;
		}
		public ShipState getPlayerShipState() { return playerShipState; }

		// TODO: See what havoc can occur when seeds change.
		// (Arrays might change size and overflow, etc)
		public void setSectorTreeSeed( int n ) { sectorTreeSeed = n; }
		public int getSectorTreeSeed() { return sectorTreeSeed; }

		public void setSectorLayoutSeed( int n ) { sectorLayoutSeed = n; }
		public int getSectorLayoutSeed() { return sectorLayoutSeed; }

		/**
		 * Sets the fleet position on the map.
		 * This is always a negative value that, when added to
		 * rebelFleetFudge, equals how far in from the left the
		 * warning circle has encroached (image has ~50px margin).
		 *
		 * Most sectors start with large negative value to keep
		 * this off-screen and increment toward 0 from there.
		 * The Last Stand sector uses a constant -25 and moderate
		 * rebelFleetFudge value to cover the map.
		 *
		 * This has always been observed in multiples of 25.
		 *
		 * @param n pixels from the map's right edge
		 *          (see 'img/map/map_warningcircle_point.png', 650px wide)
		 */
		public void setRebelFleetOffset( int n ) { rebelFleetOffset = n; }
		public int getRebelFleetOffset() { return rebelFleetOffset; }

		/**
		 * This is always a positive number around 75-310 that,
		 * when added to rebelFleetOffset, equals how far in
		 * from the left the warning circle has encroached.
		 *
		 * This varies seemingly randomly from game to game and
		 * sector to sector, but it's consistent while within
		 * each sector. Except in The Last Stand, in which it is
		 * always 200 (the warning circle will extend beyond
		 * both edges of the map).
		 */
		public void setRebelFleetFudge( int n ) { rebelFleetFudge = n; }
		public int getRebelFleetFudge() { return rebelFleetFudge; }

		/**
		 * Delays/alerts the rebel fleet (-/+).
		 * This adjusts the thickness of the warning zone.
		 * Example: Hiring a merc ship to distract sets -2.
		 */
		public void setRebelPursuitMod( int n ) { rebelPursuitMod = n; }
		public int getRebelPursuitMod() { return rebelPursuitMod; }

		/** Toggles visibility of beacon hazards for this sector. */
		public void setSectorHazardsVisible( boolean b ) { sectorHazardsVisible = b; }
		public boolean areSectorHazardsVisible() { return sectorHazardsVisible; }

		/** Toggles the flagship. Instant lose if not in sector 8. */
		public void setRebelFlagshipVisible( boolean b ) { rebelFlagshipVisible = b; }
		public boolean isRebelFlagshipVisible() { return rebelFlagshipVisible; }

		/** Set's the flagship's next/current beacon, as an index of a fixed list? */
		public void setRebelFlagshipHop( int n ) { rebelFlagshipHop = n; }
		public int getRebelFlagshipHop() { return rebelFlagshipHop; }

		/** Sets whether the flagship's approaching or circling its hop beacon. */
		public void setRebelFlagshipApproaching( boolean b ) { rebelFlagshipApproaching = b; }
		public boolean isRebelFlagshipApproaching() { return rebelFlagshipApproaching; }

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

		public ArrayList<Boolean> getSectorList() { return sectorList; }

		/**
		 * Sets whether this sector is hidden.
		 * The sector map will say "#? Hidden Crystal Worlds".
		 *
		 * When jumping from the exit beacon, you won't get to
		 * choose which branch of the sector tree will be
		 * next.
		 */
		public void setSectorIsHiddenCrystalWorlds( boolean b ) { sectorIsHiddenCrystalWorlds = b; }
		public boolean isSectorHiddenCrystalWorlds() { return sectorIsHiddenCrystalWorlds; }

		/**
		 * Adds a beacon to the sector map.
		 * Beacons are indexed top-to-bottom for each column,
		 * left-to-right. They're randomly offset a little
		 * when shown on screen to disguise the columns.
		 *
		 * The grid is approsimately 6 x 4, but each column
		 * can vary.
		 *
		 * Indexes can range from 0 to... presumably 23, but
		 * the sector layout seed may generate fewer.
		 */
		public void addBeacon( BeaconState beacon ) {
			beaconList.add( beacon );
		}

		public ArrayList<BeaconState> getBeaconList() { return beaconList; }

		public void addQuestEvent( String questEventId, int questBeaconId ) {
			questEventMap.put( questEventId, new Integer(questBeaconId) );
		}

		public LinkedHashMap<String, Integer> getQuestEventMap() {
			return questEventMap;
		}

		public void addDistantQuestEvent( String questEventId ) {
			distantQuestEventList.add( questEventId );
		}

		public ArrayList<String> getDistantQuestEventList() {
			return distantQuestEventList;
		}

		/** Sets where the player is. */
		public void setCurrentBeaconId( int n ) { currentBeaconId = n; }
		public int getCurrentBeaconId() { return currentBeaconId; }

		public void setNearbyShipState( ShipState shipState ) {
			this.nearbyShipState = shipState;
		}
		public ShipState getNearbyShipState() { return nearbyShipState; }

		public void setRebelFlagshipState( RebelFlagshipState flagshipState ) {
			this.rebelFlagshipState = flagshipState;
		}
		public RebelFlagshipState getRebelFlagshipState() {
			return rebelFlagshipState;
		}

		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

		public ArrayList<MysteryBytes> getMysteryList() { return mysteryList; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Name: %s\n", playerShipName));
			result.append(String.format("Ship Type: %s\n", playerShipBlueprintId));
			result.append(String.format("Difficulty:             %s\n", (difficultyEasy ? "Easy" : "Normal") ));
			result.append(String.format("Sector:                 %4d (%d)\n", sectorNumber, sectorNumber+1));
			result.append(String.format("Unknown?:               %4d\n", unknownHeaderAlpha));
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

			result.append("\nCargo...\n");
			for (String cargoItemId : cargoIdList) {
				result.append(String.format("CargoItemId: %s\n", cargoItemId));
			}

			result.append("\nSector Data...\n");
			result.append( String.format("Sector Tree Seed:   %5d\n", sectorTreeSeed) );
			result.append( String.format("Sector Layout Seed: %5d\n", sectorLayoutSeed) );
			result.append( String.format("Rebel Fleet Offset: %5d\n", rebelFleetOffset) );
			result.append( String.format("Rebel Fleet Fudge:  %5d\n", rebelFleetFudge) );
			result.append( String.format("Rebel Pursuit Mod:  %5d\n", rebelPursuitMod) );
			result.append( String.format("Sector Hazards Map: %b\n", sectorHazardsVisible) );
			result.append( String.format("In Hidden Sector:   %b\n", sectorIsHiddenCrystalWorlds) );
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


	public static class ShipState {
		public static final int MAX_RESERVE_POWER = 25;  // TODO: Magic number.

		private boolean auto = false;  // Is autoShip.
		private String shipName, shipBlueprintId, shipLayoutId;
		private String shipGfxBaseName;
		private ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private int hullAmt=0, fuelAmt=0, dronePartsAmt=0, missilesAmt=0, scrapAmt=0;
		private ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity = 0;
		private LinkedHashMap<String, SystemState> systemMap = new LinkedHashMap<String, SystemState>();
		private ArrayList<RoomState> roomList = new ArrayList<RoomState>();
		private LinkedHashMap<Point, Integer> breachMap = new LinkedHashMap<Point, Integer>();
		private LinkedHashMap<ShipLayout.DoorCoordinate, DoorState> doorMap = new LinkedHashMap<ShipLayout.DoorCoordinate, DoorState>();
		private ArrayList<WeaponState> weaponList = new ArrayList<WeaponState>();
		private ArrayList<DroneState> droneList = new ArrayList<DroneState>();
		private ArrayList<String> augmentIdList = new ArrayList<String>();

		public ShipState( String shipName, ShipBlueprint shipBlueprint, boolean auto ) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprint.getId();
			this.shipLayoutId = shipBlueprint.getLayout();
			this.shipGfxBaseName = shipBlueprint.getGraphicsBaseName();
			this.auto = auto;
		}

		public ShipState( String shipName, String shipBlueprintId, String shipLayoutId, String shipGfxBaseName, boolean auto ) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprintId;
			this.shipLayoutId = shipLayoutId;
			this.shipGfxBaseName = shipGfxBaseName;
			this.auto = auto;
		}

		public void setShipName( String s ) { shipName = s; }

		public String getShipName() { return shipName; }
		public String getShipBlueprintId() { return shipBlueprintId; }
		public String getShipLayoutId() { return shipLayoutId; }
		public boolean isAuto() { return auto; }

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
		public String getShipGraphicsBaseName() { return shipGfxBaseName; }

		public void addStartingCrewMember( StartingCrewState sc ) {
			startingCrewList.add(sc);
		}

		public ArrayList<StartingCrewState> getStartingCrewList() {
			return startingCrewList;
		}

		public void setHullAmt( int n ) { hullAmt = n; }
		public void setFuelAmt( int n ) { fuelAmt = n; }
		public void setDronePartsAmt( int n ) { dronePartsAmt = n; }
		public void setMissilesAmt( int n ) { missilesAmt = n; }
		public void setScrapAmt( int n ) { scrapAmt = n; }

		public int getHullAmt() { return hullAmt; }
		public int getFuelAmt() { return fuelAmt; }
		public int getDronePartsAmt() { return dronePartsAmt; }
		public int getMissilesAmt() { return missilesAmt; }
		public int getScrapAmt() { return scrapAmt; }

		public void addCrewMember( CrewState c ) {
			crewList.add(c);
		}
		public CrewState getCrewMember( int n ) {
			return crewList.get(n);
		}

		public ArrayList<CrewState> getCrewList() { return crewList; }

		public void setReservePowerCapacity( int n ) { reservePowerCapacity = n; }
		public int getReservePowerCapacity() { return reservePowerCapacity; }

		public void addSystem( SystemState s ) {
			systemMap.put( s.getSystemId(), s );
		}
		public SystemState getSystem( String systemId ) {
			return systemMap.get( systemId );
		}

		public LinkedHashMap<String, SystemState> getSystemMap() { return systemMap; }

		public void addRoom( RoomState r ) {
			roomList.add(r);
		}
		public RoomState getRoom( int roomId ) {
			return roomList.get( roomId );
		}

		public ArrayList<RoomState> getRoomList() { return roomList; }

		/**
		 * Adds a hull breach.
		 *
		 * @param x the 0-based Nth floor-square from the left (plus ShipLayout X_OFFSET)
		 * @param y the 0-based Nth floor-square from the top (plus ShipLayout Y_OFFSET)
		 * @param breachHealth 0 to 100.
		 */
		public void setBreach( int x, int y, int breachHealth ) {
			breachMap.put( new Point(x, y), new Integer(breachHealth) );
		}

		public LinkedHashMap<Point, Integer> getBreachMap() { return breachMap; }

		/**
		 * Adds a door.
		 *
		 * @param wallX the 0-based Nth wall from the left
		 * @param wallY the 0-based Nth wall from the top
		 * @param vertical 1 for vertical wall coords, 0 for horizontal
		 * @see net.blerf.ftl.model.ShipLayout
		 */
		public void setDoor( int wallX, int wallY, int vertical, DoorState d ) {
			ShipLayout.DoorCoordinate doorCoord = new ShipLayout.DoorCoordinate( wallX, wallY, vertical );
			doorMap.put(doorCoord, d);
		}
		public DoorState getDoor( int wallX, int wallY, int vertical ) {
			ShipLayout.DoorCoordinate doorCoord = new ShipLayout.DoorCoordinate( wallX, wallY, vertical );
			return doorMap.get(doorCoord);
		}

		/**
		 * Returns the map containing this ship's door states.
		 *
		 * Do not rely on the keys' order. ShipLayout config
		 * files have a different order than saved game files.
		 * Entries will be in whatever order setDoor was
		 * called, which generally will be in the saved game
		 * file's order.
		 */
		public LinkedHashMap<ShipLayout.DoorCoordinate, DoorState> getDoorMap() { return doorMap; }

		public void addWeapon( WeaponState w ) {
			weaponList.add(w);
		}

		public ArrayList<WeaponState> getWeaponList() { return weaponList; }

		public void addDrone( DroneState d ) {
			droneList.add(d);
		}

		public ArrayList<DroneState> getDroneList() { return droneList; }

		public void addAugmentId( String augmentId ) {
			augmentIdList.add(augmentId);
		}

		public ArrayList<String> getAugmentIdList() { return augmentIdList; }
		
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
			for (Map.Entry<String, SystemState> entry : systemMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(entry.getValue().toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nRooms...\n");
			first = true;
			for (ListIterator<RoomState> it=roomList.listIterator(); it.hasNext(); ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				int roomId = it.nextIndex();

				String systemId = blueprintSystems.getSystemIdByRoomId( roomId );
				if (systemId == null)
					systemId = "empty";

				result.append(String.format("RoomId: %2d (%s)\n", roomId, systemId));
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
			for (Map.Entry<ShipLayout.DoorCoordinate, DoorState> entry : doorMap.entrySet()) {
				if (first) { first = false; }
				else { result.append(",\n"); }

				ShipLayout.DoorCoordinate doorCoord = entry.getKey();
				DoorState d = entry.getValue();
				String orientation = (doorCoord.v==1 ? "V" : "H");

				result.append(String.format("DoorId: %2d (%2d,%2d,%2s)\n", ++doorId, doorCoord.x, doorCoord.y, orientation));
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
			
			return result.toString();
		}
	}



	public static class StartingCrewState {
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



	public static class CrewState {
		// TODO: magic numbers.
		// Might be worth putting in the config file.
		public static final int MASTERY_INTERVAL_PILOT = 15;
		public static final int MASTERY_INTERVAL_ENGINE = 15;
		public static final int MASTERY_INTERVAL_SHIELD = 55;
		public static final int MASTERY_INTERVAL_WEAPON = 65;
		public static final int MASTERY_INTERVAL_REPAIR = 18;
		public static final int MASTERY_INTERVAL_COMBAT = 8;

		public static final int MAX_HEALTH_CRYSTAL = 120;
		public static final int MAX_HEALTH_ENGI = 100;
		public static final int MAX_HEALTH_ENERGY = 70;
		public static final int MAX_HEALTH_HUMAN = 100;
		public static final int MAX_HEALTH_MANTIS = 100;
		public static final int MAX_HEALTH_ROCK = 150;
		public static final int MAX_HEALTH_SLUG = 100;
		public static final int MAX_HEALTH_GHOST = 50;
		public static final int MAX_HEALTH_BATTLE = 150;

		public static int getMaxHealth( String race ) {
			if ( race.equals("crystal") ) return MAX_HEALTH_CRYSTAL;
			else if ( race.equals("engi") ) return MAX_HEALTH_ENGI;
			else if ( race.equals("energy") ) return MAX_HEALTH_ENERGY;
			else if ( race.equals("human") ) return MAX_HEALTH_HUMAN;
			else if ( race.equals("mantis") ) return MAX_HEALTH_MANTIS;
			else if ( race.equals("rock") ) return MAX_HEALTH_ROCK;
			else if ( race.equals("slug") ) return MAX_HEALTH_SLUG;
			else if ( race.equals("ghost") ) return MAX_HEALTH_GHOST;
			else if ( race.equals("battle") ) return MAX_HEALTH_BATTLE;
			else throw new RuntimeException( "No max health known for race: "+ race );
		}

		// Neither Crystal crews' lockdown, nor its cooldown is stored.
		// Zoltan-produced power is not stored in SystemState.

		private String name="Frank", race="human";
		private boolean enemyBoardingDrone = false;
		private int health=0;
		private int blueprintRoomId;
		private int roomSquare;  // 0-based, L-to-R wrapped row.
		private boolean playerControlled=false;
		private int pilotSkill=0, engineSkill=0, shieldSkill=0;
		private int weaponSkill=0, repairSkill=0, combatSkill=0;
		private int repairs=0, combatKills=0, pilotedEvasions=0;
		private int jumpsSurvived=0, skillMasteries=0;
		private int spriteX, spriteY;
		private boolean male=true;

		public CrewState() {
		}

		public void setName( String s ) {name = s; }
		public void setRace( String s ) {race = s; }
		public void setHealth( int n ) {health = n; }
		public void setRoomId( int n ) {blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setPilotSkill( int n ) {pilotSkill = n; }
		public void setEngineSkill( int n ) {engineSkill = n; }
		public void setShieldSkill( int n ) {shieldSkill = n; }
		public void setWeaponSkill( int n ) {weaponSkill = n; }
		public void setRepairSkill( int n ) {repairSkill = n; }
		public void setCombatSkill( int n ) {combatSkill = n; }
		public void setRepairs( int n ) { repairs = n; }
		public void setCombatKills( int n ) { combatKills = n; }
		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }
		public void setSkillMasteries( int n ) { skillMasteries = n; }

		public String getName() { return name; }
		public String getRace() { return race; }
		public int getHealth() { return health; }
		public int getRoomId() { return blueprintRoomId; }
		public int getRoomSquare() { return roomSquare; }
		public boolean isPlayerControlled() { return playerControlled; }
		public int getPilotSkill() { return pilotSkill; }
		public int getEngineSkill() { return engineSkill; }
		public int getShieldSkill() { return shieldSkill; }
		public int getWeaponSkill() { return weaponSkill; }
		public int getRepairSkill() { return repairSkill; }
		public int getCombatSkill() { return combatSkill; }
		public int getRepairs() { return repairs; }
		public int getCombatKills() { return combatKills; }
		public int getPilotedEvasions() { return pilotedEvasions; }
		public int getJumpsSurvived() { return jumpsSurvived; }
		public int getSkillMasteries() { return skillMasteries; }

		/**
		 * Sets the position of the crew's image.
		 *
		 * Technically the roomId/square fields set the
		 * crew's desired location. This field is where
		 * the crew realy is, possibly en route.
		 *
		 * It's the position of the crew image's center,
		 * relative to the top-left square's corner, in
		 * pixels, plus (the ShipLayout's offset times
		 * the square-size, which is 35).
		 */
		public void setSpriteX( int n ) { spriteX = n; };
		public void setSpriteY( int n ) { spriteY = n; };
		public int getSpriteX() { return spriteX; }
		public int getSpriteY() { return spriteY; }


		/**
		 * Toggles gender.
		 * Humans with this set to false have a
		 * female image. Other races accept the
		 * flag but ignore it.
		 *
		 * No Andorians in the game, so this is
		 * only a two-state boolean.
		 */
		public void setMale( boolean b ) {
			male = b;
		}
		public boolean isMale() { return male; }

		/**
		 * Sets whether this crew member is a hostile drone.
		 * Upon loading after setting this on your crew,
		 * name will change to "Anti-Personnel Drone", race
		 * will be "battle", and playerControlled will be
		 * false.
		 *
		 * If after loading in-game, you re-edit this to false
		 * and leave the "battle" race, the game will change
		 * it to "human".
		 *
		 * Presumably this is so intruders can persist without
		 * a ship, which would normally have a drones section
		 * to contain them.
		 *
		 * TODO: Jump away from Boss #2 to see what its
		 * drone is (blueprints.xml mentions BOARDER_BOSS).
		 */
		public void setEnemyBoardingDrone( boolean b ) {
			enemyBoardingDrone = b;
		}
		public boolean isEnemyBoardingDrone() { return enemyBoardingDrone; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Name:              %s\n", name));
			result.append(String.format("Race:              %s\n", race));
			result.append(String.format("Enemy Drone:       %b\n", enemyBoardingDrone));
			result.append(String.format("Gender:            %s\n", (male ? "Male" : "Female") ));
			result.append(String.format("Health:            %3d\n", health));
			result.append(String.format("RoomId:            %3d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %3d\n", roomSquare));
			result.append(String.format("Player Controlled: %b\n", playerControlled));
			result.append(String.format("Sprite Position:   %3d,%3d\n", spriteX, spriteY));
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



	public static class SystemState {
		// Above this number, FTL can't find a number image to use.
		// A warning pic will appear in its place.
		public static final int MAX_IONIZED_BARS = 9;  // TODO: Magic number.

		private String systemId;
		private int capacity = 0;
		private int power = 0;
		private int damagedBars = 0;      // Number of unusable power bars.
		private int ionizedBars = 0;      // Number of disabled power bars; -1 while cloaked.
		private int repairProgress = 0;   // Turns bar yellow.
		private int damageProgress = 0;   // Turns bar red.
		private int deionizationTicks = Integer.MIN_VALUE;  // Millisecond counter.

		// ionizedBars may briefly be -1 initially when a system
		// disables itself. Then ionizedBars will be set to capacity+1.

		// deionizationTicks is reset upon loading.
		// The game's interface responds as it increments, including
		// resetting after intervals. If not needed, it may be 0, or
		// more often, MIN_INT (signed 32bit \x0000_0080) of the
		// compiler that built FTL. This parser will translate that
		// to Java's equivalent minimum during reading, and back during
		// writing.
		//   Deionization of each bar counts to 5000.
		//
		// TODO:
		// Nearly every system has been observed with non-zero values,
		// but aside from Teleporter/Cloaking, normal use doesn't reliably
		// set such values. Might be unspecified garbage when not actively
		// counting. Sometimes has huge positive and negative values.

		public SystemState( String systemId ) {
			this.systemId = systemId;
		}

		public String getSystemId() { return systemId; }

		public void setCapacity( int n ) { capacity = n; }
		public void setPower( int n ) { power = n; }
		public void setDamagedBars( int n ) { damagedBars = n; }
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public void setRepairProgress( int n ) { repairProgress = n; }
		public void setDamageProgress( int n ) { damageProgress = n; }
		public void setDeionizationTicks( int n ) { deionizationTicks = n; }

		public int getCapacity() { return capacity; }
		public int getPower() { return power; }
		public int getDamagedBars() { return damagedBars; }
		public int getIonizedBars() { return ionizedBars; }
		public int getRepairProgress() { return repairProgress; }
		public int getDamageProgress() { return damageProgress; }
		public int getDeionizationTicks() { return deionizationTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (capacity > 0) {
				result.append(String.format("SystemId:           %s\n", systemId));
				result.append(String.format("Power:              %d/%d\n", power, capacity));
				result.append(String.format("Damaged Bars:       %3d\n", damagedBars));
				result.append(String.format("Ionized Bars:       %3d\n", ionizedBars));
				result.append(String.format("Repair Progress:    %3d%%\n", repairProgress));
				result.append(String.format("Damage Progress:    %3d%%\n", damageProgress));
				result.append(String.format("Deionization Ticks: %s\n", (deionizationTicks==Integer.MIN_VALUE ? "N/A" : deionizationTicks) ));
			} else {
				result.append(String.format("%s: N/A\n", systemId));
			}
			return result.toString();
		}
	}



	public static class RoomState {
		private int oxygen = 100;
		private ArrayList<SquareState> squareList = new ArrayList<SquareState>();

		public void setOxygen( int n ) { oxygen = n; }
		public int getOxygen() { return oxygen; }

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
			squareList.add( new SquareState( fireHealth, ignitionProgress, gamma ) );
		}
		public SquareState getSquare( int n ) {
			return squareList.get(n);
		}

		public ArrayList<SquareState> getSquareList() { return squareList; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Oxygen: %3d%%\n", oxygen));
			for (SquareState square : squareList) {
				result.append(String.format("Square: Fire HP: %3d, Ignition: %3d%%, Gamma?: %2d\n", square.fireHealth, square.ignitionProgress, square.gamma));
			}
			return result.toString();
		}
	}

	public static class SquareState {
		public int fireHealth = 0;
		public int ignitionProgress = 0;
		public int gamma = -1;

		public SquareState( int fireHealth, int ignitionProgress, int gamma ) {
			this.fireHealth = fireHealth;
			this.ignitionProgress = ignitionProgress;
			this.gamma = gamma;
		}
		public SquareState() {
		}
	}



	public static class DoorState {
		private boolean open = false;
		private boolean walkingThrough = false;

		public DoorState() {
		}

		public DoorState( boolean open, boolean walkingThrough ) {
			this.open = open;
			this.walkingThrough = walkingThrough;
		}

		public void setOpen( boolean b ) { open = b; }
		public void setWalkingThrough( boolean b ) { walkingThrough = b; }

		public boolean isOpen() { return open; }
		public boolean isWalkingThrough() { return walkingThrough; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("Open: %b, Walking Through: %b\n", open, walkingThrough));
			return result.toString();
		}
	}



	public static class WeaponState {
		private String weaponId = null;
		private boolean armed = false;
		private int cooldownTicks = 0;

		public WeaponState() {
		}

		public WeaponState( String weaponId, boolean armed, int cooldownTicks ) {
			this.weaponId = weaponId;
			this.armed = armed;
			this.cooldownTicks = cooldownTicks;
		}

		public void setWeaponId( String s ) { weaponId = s; }
		public String getWeaponId() { return weaponId; }

		public void setArmed( boolean b ) {
			armed = b;
			if ( b == false ) cooldownTicks = 0;
		}
		public boolean isArmed() { return armed; }

		/**
		 * Sets the weapon's cooldown ticks.
		 * This increments from 0 each second until the
		 * weapon blueprint's cooldown. 0 when not armed.
		 */
		public void setCooldownTicks( int n ) { cooldownTicks = n; }
		public int getCooldownTicks() { return cooldownTicks; }

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



	public static class DroneState {
		// TODO: magic numbers.
		// Might be worth putting in the config file.
		public static final int MAX_HEALTH_BATTLE = 150;
		public static final int MAX_HEALTH_REPAIR = 25;
		public static final int MAX_HEALTH_BOARDER = 1;
		public static final int MAX_HEALTH_COMBAT = 1;
		public static final int MAX_HEALTH_DEFENSE = 1;
		public static final int MAX_HEALTH_SHIP_REPAIR = 1;

		public static int getMaxHealth( String type ) {
			if ( type.equals(DroneBlueprint.TYPE_BATTLE) ) return MAX_HEALTH_BATTLE;
			else if ( type.equals(DroneBlueprint.TYPE_REPAIR) ) return MAX_HEALTH_REPAIR;
			else if ( type.equals(DroneBlueprint.TYPE_BOARDER) ) return MAX_HEALTH_BOARDER;
			else if ( type.equals(DroneBlueprint.TYPE_COMBAT) ) return MAX_HEALTH_COMBAT;
			else if ( type.equals(DroneBlueprint.TYPE_DEFENSE) ) return MAX_HEALTH_DEFENSE;
			else if ( type.equals(DroneBlueprint.TYPE_SHIP_REPAIR) ) return MAX_HEALTH_SHIP_REPAIR;
			else throw new RuntimeException( "No max health known for drone: "+ type );
		}

		private String droneId = null;
		private boolean armed = false;
		private boolean playerControlled = true;  // False when not armed.
		private int spriteX = -1, spriteY = -1;   // -1 when body not present.
		private int blueprintRoomId = -1;  // -1 when body not present.
		private int roomSquare = -1;       // -1 when body not present.
		private int health = 1;

		public DroneState() {
		}

		public DroneState( String droneId ) {
			this.droneId = droneId;
		}

		public void setDroneId( String s ) { droneId = s; }
		public void setArmed( boolean b ) { armed = b; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setSpriteX( int n ) { spriteX = n; }
		public void setSpriteY( int n ) { spriteY = n; }
		public void setRoomId( int n ) { blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setHealth( int n ) { health = n; }

		public String getDroneId() { return droneId; }
		public boolean isArmed() { return armed; }
		public boolean isPlayerControlled() { return playerControlled; }
		public int getSpriteX() { return spriteX; }
		public int getSpriteY() { return spriteY; }
		public int getRoomId() { return blueprintRoomId; }
		public int getRoomSquare() { return roomSquare; }
		public int getHealth() { return health; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(String.format("DroneId:           %s\n", droneId));
			result.append(String.format("Armed:             %b\n", armed));
			result.append(String.format("Health:            %3d\n", health));
			result.append(String.format("RoomId:            %3d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %3d\n", roomSquare));
			result.append(String.format("Player Controlled: %b\n", playerControlled));
			result.append(String.format("Sprite Position:   %3d,%3d\n", spriteX, spriteY));
			return result.toString();
		}
	}



	public static enum FleetPresence { NONE, REBEL, FEDERATION, BOTH }

	public static class BeaconState {
		
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
				result.append(String.format("Unknown?:          %3d\n", unknownVisitedAlpha));
			}
			
			result.append(String.format("Seen:              %b\n", seen));
			
			result.append(String.format("Enemy Present:     %b\n", enemyPresent));
			if ( enemyPresent ) {
				result.append(String.format("  Ship Event ID:          %s\n", shipEventId));
				result.append(String.format("  Ship Blueprint List ID: %s\n", shipBlueprintListId));
				result.append(String.format("  Unknown?:               %5d\n", unknownEnemyPresentAlpha));
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



	public static class StoreState {
		
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
	
	public static class StoreShelf {
		
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

		public List<StoreItem> getItems() {
			return items;
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
	
	public static class StoreItem {
		private boolean available;
		private String itemId;

		public StoreItem(boolean available, String itemId) {
			this.available = available;
			this.itemId = itemId;
		}

		public boolean isAvailable() {
			return available;
		}

		public String getItemId() {
			return itemId;
		}

		@Override
		public String toString() {
			return String.format("%s (%s)\n", itemId, (available ? "Available" : "Sold Out"));
		}
	}



	public static class RebelFlagshipState {
		private String[] shipBlueprintIds;
		private int pendingStage = 1;
		private LinkedHashMap<Integer, Integer> occupancyMap = new LinkedHashMap<Integer, Integer>();

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

		public String[] getShipBlueprintIds() {
			return shipBlueprintIds;
		}

		/**
		 * Sets the next version of the flagship that will be encountered (1-based).
		 */
		public void setPendingStage( int pendingStage ) {
			if ( pendingStage <= 0 || pendingStage > shipBlueprintIds.length )
				throw new IndexOutOfBoundsException( "Attempted to set 1-based flagship stage "+ pendingStage +" of "+ shipBlueprintIds.length +" total" );
			this.pendingStage = pendingStage;
		}
		public int getPendingStage() {
			return pendingStage;
		}

		/**
		 * Sets whether a room had crew members in the last seen layout.
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
		 *   Having 0 rooms is allowed, meaning AI took over.
		 *
		 * Stage 2 will read altered bytes on additional skirmishes.
		 *
		 * Stage 3 probably will, too. (TODO: Confirm this.)
		 *
		 * @param roomId a room in the last seen stage's shipLayout
		 * @param n the number of crew in that room
		 */
		public void setPreviousOccupancy( int roomId, int n ) {
			occupancyMap.put( new Integer(roomId), new Integer(n) );
		}

		public LinkedHashMap<Integer, Integer> getOccupancyMap() {
			return occupancyMap;
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
			for (Map.Entry<Integer, Integer> entry : occupancyMap.entrySet()) {
				int roomId = entry.getKey().intValue();
				int occupantCount = entry.getValue().intValue();

				String systemId = blueprintSystems.getSystemIdByRoomId( roomId );
				if (systemId == null)
					systemId = "empty";

				result.append( String.format("RoomId: %2d (%-10s), Crew: %d\n", roomId, systemId, occupantCount) );
			}

			return result.toString();
		}
	}

}
