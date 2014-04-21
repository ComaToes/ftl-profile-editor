// Variables with unknown meanings are named with greek letters.
// Classes for unknown objects are named after deities.
// http://en.wikipedia.org/wiki/List_of_Greek_mythological_figures#Personified_concepts

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
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;


public class SavedGameParser extends Parser {


	public SavedGameState readSavedGame( File savFile ) throws IOException {
		SavedGameState gameState = null;

		FileInputStream in = null;
		try {
			in = new FileInputStream( savFile );
			gameState = readSavedGame(in);
		}
		finally {
			try {if (in != null) in.close();}
			catch (IOException e) {}
		}

		return gameState;
	}

	public SavedGameState readSavedGame( FileInputStream in ) throws IOException {
		InputStream layoutStream = null;
		try {
			SavedGameState gameState = new SavedGameState();

			int headerAlpha = readInt(in);
			if ( headerAlpha == 2 ) {
				// FTL 1.03.3 and earlier.
				gameState.setHeaderAlpha( headerAlpha );
				gameState.setDLCEnabled( false );  // Not present before FTL 1.5.4.
			}
			else if ( headerAlpha == 7 ) {
				// FTL 1.5.4+.
				gameState.setHeaderAlpha( headerAlpha );
				gameState.setDLCEnabled( readBool(in) );
			}
			else {
				throw new IOException( "Unexpected first byte ("+ headerAlpha +") for a SAVED GAME." );
			}

			gameState.setDifficulty( readInt(in) );
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
			gameState.setUnknownBeta( readInt(in) );

			int stateVarCount = readInt(in);
			for (int i=0; i < stateVarCount; i++) {
				String stateVarId = readString(in);
				Integer stateVarValue = new Integer(readInt(in));
				gameState.setStateVar(stateVarId, stateVarValue);
			}

			ShipState playerShipState = readShip( in, false, headerAlpha, gameState.isDLCEnabled() );
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

			if ( headerAlpha == 7 ) {
				gameState.setCurrentBeaconId( readInt(in) );

				gameState.setUnknownGamma( readInt(in) );
				gameState.setUnknownDelta( readInt(in) );
				gameState.setUnknownEpsilon( readInt(in) );
				gameState.setSectorHazardsVisible( readBool(in) );
				gameState.setRebelFlagshipVisible( readBool(in) );
				gameState.setRebelFlagshipHop( readInt(in) );
				gameState.setRebelFlagshipMoving( readBool(in) );
				gameState.setUnknownKappa( readInt(in) );
				gameState.setRebelFlagshipBaseTurns( readInt(in) );
			}
			else if ( headerAlpha == 2 ) {
				gameState.setSectorHazardsVisible( readBool(in) );

				gameState.setRebelFlagshipVisible( readBool(in) );

				gameState.setRebelFlagshipHop( readInt(in) );

				gameState.setRebelFlagshipMoving( readBool(in) );
			}

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
				gameState.addBeacon( readBeacon(in, headerAlpha) );
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

			if ( headerAlpha == 2 ) {
				gameState.setCurrentBeaconId( readInt(in) );

				boolean shipNearby = readBool(in);
				if ( shipNearby ) {
					ShipState nearbyShipState = readShip( in, true, headerAlpha, gameState.isDLCEnabled() );
					gameState.setNearbyShipState(nearbyShipState);
				}

				RebelFlagshipState flagshipState = readRebelFlagship(in);
				gameState.setRebelFlagshipState( flagshipState );
			}
			else {
				// Current beaconId was set earlier.

				gameState.setUnknownMu( readInt(in) );

				EncounterState encounter = readEncounter(in);
				gameState.setEncounter( encounter );

				boolean shipNearby = readBool(in);
				if ( shipNearby ) {
					gameState.addMysteryBytes( new MysteryBytes(in, 4*1) );

					ShipState nearbyShipState = readShip( in, true, headerAlpha, gameState.isDLCEnabled() );
					gameState.setNearbyShipState(nearbyShipState);
				}

				gameState.setRebelFlagshipState( new RebelFlagshipState(new String[] {"BOSS_1", "BOSS_2", "BOSS_3"}) );

				//System.err.println( in.getChannel().position() );
			}

			// The stream should end here.

			int bytesRemaining = (int)(in.getChannel().size() - in.getChannel().position());
			if ( bytesRemaining > 0 ) {
				gameState.addMysteryBytes( new MysteryBytes(in, bytesRemaining) );
			}

			return gameState;  // The finally block will still be executed.
		}
		finally {
			try {if (layoutStream != null) layoutStream.close();}
			catch ( IOException e ) {}
		}
	}

	/**
	 * Writes a gameState to a stream.
	 *
	 * Any MysteryBytes will be omitted.
	 */
	public void writeSavedGame( OutputStream out, SavedGameState gameState ) throws IOException {

		// This should always be 2.
		writeInt( out, 2 );

		writeInt( out, gameState.getDifficulty() );
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
		writeBool( out, gameState.isRebelFlagshipMoving() );

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

	private ShipState readShip( InputStream in, boolean auto, int headerAlpha, boolean dlcEnabled ) throws IOException {

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

		if ( headerAlpha == 7 ) {
			shipState.setUnknownAlpha( readInt(in) );
			shipState.setJumpTicks( readInt(in) );
			shipState.setUnknownGamma( readInt(in) );
			shipState.setUnknownDelta( readInt(in) );
		}

		shipState.setHullAmt( readInt(in) );
		shipState.setFuelAmt( readInt(in) );
		shipState.setDronePartsAmt( readInt(in) );
		shipState.setMissilesAmt( readInt(in) );
		shipState.setScrapAmt( readInt(in) );

		int crewCount = readInt(in);
		for (int i=0; i < crewCount; i++) {
			shipState.addCrewMember( readCrewMember(in, headerAlpha) );
		}

		// System info is stored in this order.
		ArrayList<SystemType> systemTypes = new ArrayList<SystemType>();
		systemTypes.add( SystemType.SHIELDS );
		systemTypes.add( SystemType.ENGINES );
		systemTypes.add( SystemType.OXYGEN );
		systemTypes.add( SystemType.WEAPONS );
		systemTypes.add( SystemType.DRONE_CTRL );
		systemTypes.add( SystemType.MEDBAY );
		systemTypes.add( SystemType.PILOT );
		systemTypes.add( SystemType.SENSORS );
		systemTypes.add( SystemType.DOORS );
		systemTypes.add( SystemType.TELEPORTER );
		systemTypes.add( SystemType.CLOAKING );
		systemTypes.add( SystemType.ARTILLERY );
		if ( headerAlpha == 7 ) {
			systemTypes.add( SystemType.BATTERY );
			systemTypes.add( SystemType.CLONEBAY );
			systemTypes.add( SystemType.MIND );
			systemTypes.add( SystemType.HACKING );
		}

		shipState.setReservePowerCapacity( readInt(in) );
		for ( SystemType systemType : systemTypes ) {
			shipState.addSystem( readSystem(in, systemType, headerAlpha ) );

			// Systems that exist in multiple rooms have additional SystemStates.
			// Example: Boss' artillery.
			//
			// In FTL 1.01-1.03.3 the boss wasn't a nearby ship outside of combat,
			// So this never occurred. There may also have been changes in 1.5.4
			// to allow multi-room systems.
			//
			// TODO: Report this.

			ShipBlueprint.SystemList.SystemRoom[] rooms = shipBlueprint.getSystemList().getSystemRoom( systemType );
			if ( rooms != null && rooms.length > 1 ) {
				System.err.println(String.format("Warning: Skipping extra rooms for system: %s", systemType.toString()));

				for ( int q=0; q < rooms.length-1; q++ ) {
					readSystem(in, systemType, headerAlpha );
				}
			}
		}

		if ( headerAlpha == 7 ) {

			List<ExtendedSystemInfo> iotaList = new ArrayList<ExtendedSystemInfo>();
			SystemState tmpSystem = null;

			tmpSystem = shipState.getSystem( SystemType.CLONEBAY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				ClonebayInfo clonebayInfo = new ClonebayInfo();

				clonebayInfo.setBuildTicks( readInt(in) );
				clonebayInfo.setBuildTicksGoal( readInt(in) );
				clonebayInfo.setUnknownGamma( readInt(in) );

				iotaList.add( clonebayInfo );
			}
			tmpSystem = shipState.getSystem( SystemType.BATTERY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				BatteryInfo batteryInfo = new BatteryInfo();

				batteryInfo.setActive( readBool(in) );
				batteryInfo.setUsedPower( readInt(in) );
				batteryInfo.setDischargeTicks( readInt(in) );

				iotaList.add( batteryInfo );
			}

			// The shields info always exists, even if the shields system doesn't.
			if ( true ) {
				ShieldsInfo shieldsInfo = new ShieldsInfo();

				shieldsInfo.setShieldLayers( readInt(in) );
				shieldsInfo.setEnergyShieldLayers( readInt(in) );
				shieldsInfo.setEnergyShieldMax( readInt(in) );
				shieldsInfo.setShieldRechargeTicks( readInt(in) );

				shieldsInfo.setShieldDropAnimOn( readBool(in) );
				shieldsInfo.setShieldDropAnimTicks( readInt(in) );    // TODO: Confirm.

				shieldsInfo.setShieldRaiseAnimOn( readBool(in) );
				shieldsInfo.setShieldRaiseAnimTicks( readInt(in) );   // TODO: Confirm.

				shieldsInfo.setEnergyShieldAnimOn( readBool(in) );
				shieldsInfo.setEnergyShieldAnimTicks( readInt(in) );  // TODO: Confirm.

				// A pair. Usually noise. Sometimes 0.
				shieldsInfo.setUnknownLambda( readInt(in) );   // TODO: Confirm: Shield down point X.
				shieldsInfo.setUnknownMu( readInt(in) );       // TODO: Confirm: Shield down point Y.

				iotaList.add( shieldsInfo );
			}

			tmpSystem = shipState.getSystem( SystemType.CLOAKING );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				CloakingInfo cloakingInfo = new CloakingInfo();

				cloakingInfo.setUnknownAlpha( readInt(in) );
				cloakingInfo.setUnknownBeta( readInt(in) );
				cloakingInfo.setCloakTicksGoal( readInt(in) );

				int delta = readInt(in);
				if ( delta == -2147483648 )
					delta = Integer.MIN_VALUE;
				cloakingInfo.setCloakTicks( delta );

				iotaList.add( cloakingInfo );
			}

			shipState.setUnknownIota( iotaList );
		}

		int roomCount = shipLayout.getRoomCount();
		for (int r=0; r < roomCount; r++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfo = shipLayout.getRoomInfo(r);
			int squaresH = roomInfo.get(ShipLayout.RoomInfo.SQUARES_H).intValue();
			int squaresV = roomInfo.get(ShipLayout.RoomInfo.SQUARES_V).intValue();

			// Room states are stored in roomId order.
			shipState.addRoom( readRoom(in, squaresH, squaresV, headerAlpha) );
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
			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in, headerAlpha) );
		}
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : vacuumDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in, headerAlpha) );
		}

		if ( headerAlpha == 7 ) {
			shipState.setUnknownPhi( readInt(in) );
		}

		int weaponCount = readInt(in);
		for (int i=0; i < weaponCount; i++) {
			WeaponState weapon = new WeaponState();
			weapon.setWeaponId( readString(in) );
			weapon.setArmed( readBool(in) );

			if ( headerAlpha == 2 ) {  // No longer used as of FTL 1.5.4.
				weapon.setCooldownTicks( readInt(in) );
			}

			shipState.addWeapon( weapon );
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
		ArrayList<SystemType> systemTypes = new ArrayList<SystemType>();
		systemTypes.add( SystemType.SHIELDS );
		systemTypes.add( SystemType.ENGINES );
		systemTypes.add( SystemType.OXYGEN );
		systemTypes.add( SystemType.WEAPONS );
		systemTypes.add( SystemType.DRONE_CTRL );
		systemTypes.add( SystemType.MEDBAY );
		systemTypes.add( SystemType.PILOT );
		systemTypes.add( SystemType.SENSORS );
		systemTypes.add( SystemType.DOORS );
		systemTypes.add( SystemType.TELEPORTER );
		systemTypes.add( SystemType.CLOAKING );
		systemTypes.add( SystemType.ARTILLERY );

		writeInt( out, shipState.getReservePowerCapacity() );

		Map<SystemType, SystemState> systemMap = shipState.getSystemMap();
		for ( SystemType systemType : systemTypes ) {
			SystemState systemState = systemMap.get(systemType);
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
		StartingCrewState startingCrew = new StartingCrewState( crewName, crewRace );
		return startingCrew;
	}

	public void writeStartingCrewMember( OutputStream out, StartingCrewState startingCrew ) throws IOException {
		writeString( out, startingCrew.getRace() );
		writeString( out, startingCrew.getName() );
	}

	private CrewState readCrewMember( InputStream in, int headerAlpha ) throws IOException {
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

		if ( headerAlpha == 7 ) {
			crew.setUnknownAlpha( readInt(in) );
			crew.setUnknownBeta( readInt(in) );

			int tintCount = readInt(in);
			List<Integer> spriteTintIndeces = new ArrayList<Integer>();
			for (int i=0; i < tintCount; i++) {
				spriteTintIndeces.add( new Integer(readInt(in)) );
			}
			crew.setSpriteTintIndeces( spriteTintIndeces );

			crew.setMindControlled( readBool(in) );
			crew.setSavedRoomSquare( readInt(in) );
			crew.setSavedRoomId( readInt(in) );
		}

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

		if ( headerAlpha == 7 ) {
			crew.setStunTicks( readInt(in) );
			crew.setUnknownTheta( readInt(in) );
			crew.setUnknownIota( readInt(in) );
			crew.setUnknownKappa( readInt(in) );
			crew.setUnknownLambda( readInt(in) );
			crew.setUnknownMu( readInt(in) );
			crew.setUnknownNu( readInt(in) );
			crew.setUnknownXi( readInt(in) );
			crew.setUnknownOmicron( readInt(in) );
			crew.setUnknownPi( readInt(in) );
			crew.setUnknownRho( readInt(in) );
			crew.setUnknownSigma( readInt(in) );
			crew.setUnknownTau( readInt(in) );
			crew.setUnknownUpsilon( readInt(in) );
			crew.setUnknownPhi( readInt(in) );

			if ( "crystal".equals(crew.getRace()) ) {
				crew.setLockdownRechargeTicks( readInt(in) );
				crew.setLockdownRechargeGoal( readInt(in) );
				crew.setUnknownOmega( readInt(in) );
			}
		}

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

	private SystemState readSystem( InputStream in, SystemType systemType, int headerAlpha ) throws IOException {
		SystemState system = new SystemState( systemType );
		int capacity = readInt(in);

		// Normally systems are 28 bytes, but if not present on the
		// ship, capacity will be zero, and the system will only
		// occupy the 4 bytes that declared the capacity. And the
		// next system will begin 24 bytes sooner.
		if (capacity > 0) {
			system.setCapacity( capacity );
			system.setPower( readInt(in) );
			system.setDamagedBars( readInt(in) );
			system.setIonizedBars( readInt(in) );       // TODO: Active mind control has -1?

			int deionizationTicks = readInt(in);
			if ( deionizationTicks == -2147483648 )
				deionizationTicks = Integer.MIN_VALUE;
			system.setDeionizationTicks( deionizationTicks );

			system.setRepairProgress( readInt(in) );
			system.setDamageProgress( readInt(in) );

			if ( headerAlpha == 7 ) {
				system.setBatteryPower( readInt(in) );
				system.setHackLevel( readInt(in) );
				system.setHacked( readBool(in) );
				system.setTemporaryCapacityCap( readInt(in) );
				system.setTemporaryCapacityLoss( readInt(in) );
				system.setTemporaryCapacityDivisor( readInt(in) );
			}
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

	private RoomState readRoom( InputStream in, int squaresH, int squaresV, int headerAlpha ) throws IOException {
		RoomState room = new RoomState();
		room.setOxygen( readInt(in) );

		for (int h=0; h < squaresH; h++) {
			for (int v=0; v < squaresV; v++) {
				// Fire HP (0-100), Ignition Progress (0-100), Extinguishment Progress (-1).
				// Matthew says that -1 is a bug in FTL 1.01-1.5.10.
				// TODO: Watch for that to be fixed.
				room.addSquare( readInt(in), readInt(in), readInt(in) );
			}
		}

		if ( headerAlpha == 7 ) {
			room.setStationSquare( readInt(in) );

			int stationDirection = readInt(in);
			if ( stationDirection < 0 || stationDirection > 4 )
				throw new IOException( "Invalid room station direction: "+ stationDirection );
			room.setStationDirection( stationDirection );
		}

		return room;
	}

	public void writeRoom( OutputStream out, RoomState room ) throws IOException {
		writeInt( out, room.getOxygen() );

		for (SquareState square : room.getSquareList()) {
			writeInt( out, square.getFireHealth() );
			writeInt( out, square.getIgnitionProgress() );
			writeInt( out, square.getUnknownGamma() );
		}
	}

	private DoorState readDoor( InputStream in, int headerAlpha ) throws IOException {
		DoorState door = new DoorState();

		if ( headerAlpha == 7 ) {
			door.setCurrentMaxHealth( readInt(in) );
			door.setHealth( readInt(in) );
			door.setNominalHealth( readInt(in) );
		}

		door.setOpen( readBool(in) );
		door.setWalkingThrough( readBool(in) );

		if ( headerAlpha == 7 ) {
			door.setUnknownDelta( readInt(in) );
			door.setUnknownEpsilon( readInt(in) );  // TODO: Confirm: Drone lockdown.
		}

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

	private BeaconState readBeacon( InputStream in, int headerAlpha ) throws IOException {
		BeaconState beacon = new BeaconState();

		int rawVisited = readInt(in);  // TODO: Decide on bool vs int.
		boolean visited = false;
		if ( rawVisited == 0 ) visited = false;
		else if ( rawVisited == 1 ) visited = true;
		else {
			System.err.println( String.format("Warning: Substituting true for beacon's strange visited value: %d", rawVisited) );
			visited = true;
		}

		beacon.setVisited( visited );
		if ( visited ) {
			beacon.setBgStarscapeImageInnerPath( readString(in) );
			beacon.setBgSpriteImageInnerPath( readString(in) );
			beacon.setBgSpritePosX( readInt(in) );
			beacon.setBgSpritePosY( readInt(in) );
			beacon.setBgSpriteRotation( readInt(in) );
		}
		
		beacon.setSeen( readBool(in) );
		
		boolean enemyPresent = readBool(in);
		beacon.setEnemyPresent(enemyPresent);
		if ( enemyPresent ) {
			beacon.setShipEventId( readString(in) );
			beacon.setAutoBlueprintId( readString(in) );
			beacon.setShipEventSeed( readInt(in) );

			// When player's at this beacon, the seed here matches
			// current encounter's seed.
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

			int shelfCount = 2;          // FTL 1.01-1.03.3 only had two shelves.
			if ( headerAlpha == 7 ) {
				shelfCount = readInt(in);
			}
			for (int i=0; i < shelfCount; i++) {
				store.addShelf( readStoreShelf(in) );
			}

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
			writeInt( out, beacon.getBgSpriteRotation() );
		}

		writeBool( out, beacon.isSeen() );

		writeBool( out, beacon.isEnemyPresent() );
		if ( beacon.isEnemyPresent() ) {
			writeString( out, beacon.getShipEventId() );
			writeString( out, beacon.getAutoBlueprintId() );
			writeInt( out, beacon.getShipEventSeed() );
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
			if ( items.size() > i ) {
				available = (items.get(i).isAvailable() ? 1 : 0);
				itemId = items.get(i).getItemId();
			}
			writeInt( out, available );
			if ( available >= 0 ) writeString( out, itemId );
		}
	}

	public EncounterState readEncounter( InputStream in ) throws IOException {
		EncounterState encounter = new EncounterState();

		encounter.setShipEventSeed( readInt(in) );  // Matches the beacon's seed.
		encounter.setUnknownBeta( readString(in) );
		encounter.setUnknownGamma( readString(in) );
		encounter.setUnknownDelta( readString(in) );
		encounter.setUnknownEpsilon( readString(in) );
		encounter.setUnknownZeta( readString(in) );
		encounter.setUnknownEta( readString(in) );
		encounter.setText( readString(in) );
		encounter.setUnknownIota( readInt(in) );

		int kappaCount = readInt(in);  // TODO: 0-based event choice index breadcrumbs?
		ArrayList<Integer> kappaList = new ArrayList<Integer>();
		for (int i=0; i < kappaCount; i++) {
			kappaList.add( new Integer(readInt(in)) );
		}
		encounter.setUnknownKappa( kappaList );

		return encounter;
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



	public static enum StateVar {
		// TODO: Magic strings.
		BLUE_ALIEN     ("blue_alien",      "Blue event choices clicked. (only ones that require a race)"),
		DEAD_CREW      ("dead_crew",       "???, plus boarding drone bodies? (see also: lost_crew)"),
		DESTROYED_ROCK ("destroyed_rock",  "Rock ships destroyed. (including pirates)"),
		ENV_DANGER     ("env_danger",      "Jumps into beacons with environmental dangers."),
		FIRED_SHOT     ("fired_shot",      "Individual beams/blasts/projectiles fired. (see also: used_missile)"),
		HIGH_O2        ("higho2",          "Times oxygen exceeded 20%, incremented when jumping from a beacon or saving."),
		KILLED_CREW    ("killed_crew",     "Enemy crew killed. (and possibly beam friendly fire?)"),
		LOST_CREW      ("lost_crew",       "Crew you've lost: killed, abandoned on nearby ships, taken by events?, but not dismissed. (see also: dead_crew)"),
		NEBULA         ("nebula",          "Jumps into nebula beacons."),
		OFFENSIVE_DRONE("offensive_drone", "The number of times drones capable of damaging an enemy ship powered up."),
		REACTOR_UPGRADE("reactor_upgrade", "Reactor (power bar) upgrades beyond the ship's default levels."),
		STORE_PURCHASE ("store_purchase",  "Non-repair crew/items purchased. (selling isn't counted)"),
		STORE_REPAIR   ("store_repair",    "Store repair button clicks."),
		SUFFOCATED_CREW("suffocated_crew", "???"),
		SYSTEM_UPGRADE ("system_upgrade",  "System (and subsystem; not reactor) upgrades beyond the ship's default levels."),
		TELEPORTED     ("teleported",      "Teleporter activations, in either direction."),
		USED_DRONE     ("used_drone",      "The number of times drone parts were consumed."),
		USED_MISSILE   ("used_missile",    "Missile/bomb weapon discharges. (see also: fired_shot)"),
		WEAPON_UPGRADE ("weapon_upgrade",  "Weapons system upgrades beyond the ship's default levels. (see also: system_upgrade)");

		// The following were introduced in FTL 1.5.4.
		// HIGH_O2, SUFFOCATED_CREW

		private String id;
		private String description;
		private StateVar( String id, String description ) {
			this.id = id;
			this.description = description;
		}
		public String getId() { return id; }
		public String getDescription() { return description; }
		public String toString() { return id; }

		public static StateVar findById( String id ) {
			for ( StateVar v : values() ) {
				if ( v.getId().equals(id) ) return v;
			}
			return null;
		}

		public static String getDescription( String id ) {
			StateVar v = StateVar.findById(id);
			if ( v != null ) return v.getDescription();
			return id +" is an unknown var. Please report it on the forum thread.";
		}
	}

	public static class SavedGameState {
		private int unknownHeaderAlpha = 0;   // Magic number indicating file format.
		private boolean dlcEnabled = false;
		private int difficulty = 0;
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
		private boolean rebelFlagshipMoving = false;
		private int rebelFlagshipBaseTurns = 0;
		private ArrayList<Boolean> sectorList = new ArrayList<Boolean>();
		private boolean sectorIsHiddenCrystalWorlds = false;
		private ArrayList<BeaconState> beaconList = new ArrayList<BeaconState>();
		private LinkedHashMap<String, Integer> questEventMap = new LinkedHashMap<String, Integer>();
		private ArrayList<String> distantQuestEventList = new ArrayList<String>();
		private EncounterState encounter = null;
		private ShipState nearbyShipState = null;
		private int currentBeaconId = 0;
		private RebelFlagshipState rebelFlagshipState = null;
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		private int unknownBeta = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;
		private int unknownKappa = 0;

		private int unknownMu = 0;


		public SavedGameState() {
		}

		/**
		 * Sets the difficulty.
		 *
		 * 0 = Easy
		 * 1 = Normal
		 * 2 = Hard   (FTL 1.5.4+)
		 */
		public void setDifficulty( int n ) { difficulty = n; }
		public int getDifficulty() { return difficulty; }

		public void setTotalShipsDefeated( int n ) { totalShipsDefeated = n; }
		public void setTotalBeaconsExplored( int n ) { totalBeaconsExplored = n; }
		public void setTotalScrapCollected( int n ) { totalScrapCollected = n; }
		public void setTotalCrewHired( int n ) { totalCrewHired = n; }

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

		public void setCargoList( ArrayList<String> cargoIdList ) {
			this.cargoIdList = cargoIdList;
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

		/**
		 * Sets the magic number indicating file format, apparently.
		 *
		 * 2 = Saved Game, FTL 1.01-1.03.3
		 * 7 = Saved Game, FTL 1.5.4+
		 */
		public void setHeaderAlpha( int n ) { unknownHeaderAlpha = n; }
		public int getHeaderAlpha() { return unknownHeaderAlpha; }

		/**
		 * Toggles FTL:AE content.
		 *
		 * This content was introduced in FTL 1.5.4.
		 *
		 * Note Bad things may happen if you change the value
		 * from true to false, if this saved game depends on
		 * AE resources.
		 */
		public void setDLCEnabled( boolean b ) { dlcEnabled = b; }
		public boolean isDLCEnabled() { return dlcEnabled; }

		/**
		 * Sets a state var.
		 * State vars are mostly used to test candidacy for achievements.
		 *
		 * See StateVar enums for known vars and descriptions.
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

		/**
		 * Sets the seed for randomness in the current sector.
		 *
		 * Reloading a saved game from the end of the previous sector,
		 * and exiting again will yield a different seed. So sectors'
		 * layout seeds aren't pretetermined at the start of the game.
		 *
		 * Changing this may affect the beacon count. The game will
		 * generate additional beacons if it expects them (and probably
		 * truncate the excess if there are too many).
		 */
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

		/**
		 * Toggles visibility of beacon hazards for this sector.
		 */
		public void setSectorHazardsVisible( boolean b ) { sectorHazardsVisible = b; }
		public boolean areSectorHazardsVisible() { return sectorHazardsVisible; }

		/**
		 * Toggles the flagship.
		 *
		 * If true, this causes instant loss if not in sector 8.
		 */
		public void setRebelFlagshipVisible( boolean b ) { rebelFlagshipVisible = b; }
		public boolean isRebelFlagshipVisible() { return rebelFlagshipVisible; }

		/**
		 * Sets the flagship's current beacon.
		 *
		 * The flagship will be at its Nth random beacon. (0-based)
		 * The sector layout seed affects where that will be.
		 *
		 * At or above the last hop (which varies), it causes instant loss in
		 * FTL 1.01-1.03.3. (Observed game-ending values: 5, 7, potentially 9.)
		 *
		 * Since FTL 1.5.4, the flagship must idle at the federation base for
		 * a few turns before tge game ends.
		 *
		 * If moving, this will be the beacon it's departing from.
		 */
		public void setRebelFlagshipHop( int n ) { rebelFlagshipHop = n; }
		public int getRebelFlagshipHop() { return rebelFlagshipHop; }

		/**
		 * Sets whether the flagship's circling its beacon or moving toward the next.
		 */
		public void setRebelFlagshipMoving( boolean b ) { rebelFlagshipMoving = b; }
		public boolean isRebelFlagshipMoving() { return rebelFlagshipMoving; }

		/**
		 * Sets the number of turns the rebel flagship has started at the
		 * federation base.
		 *
		 * At the 4th turn, the game will end. (TODO: Confirm.)
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @param n 0-4
		 */
		public void setRebelFlagshipBaseTurns( int n ) { rebelFlagshipBaseTurns = n; }
		public int getRebelFlagshipBaseTurns() { return rebelFlagshipBaseTurns; }

		/**
		 * Adds a dot of the sector tree.
		 * Dots are indexed top-to-bottom for each column, left-to-right.
		 */
		public void addSector( boolean visited ) {
			sectorList.add( new Boolean(visited) );
		}

		/**
		 * Toggles whether a dot on the sector tree has been visited.
		 *
		 * @param sector an index of the sector list (0-based)
		 * @param visited true if visited, false otherwise
		 */
		public void setSectorVisited( int sector, boolean visited ) {
			sectorList.set( sector, new Boolean(visited) );
		}

		/**
		 * Returns a list of sector tree breadcrumbs.
		 *
		 * Saved games only contain a linear set of boolean flage to
		 * track visited status. FTL reconstructs the sector tree at
		 * runtime using the sector tree seed, and it maps these
		 * booleans to the dots: top-to-bottom for each column,
		 * left-to-right.
		 */
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

		/**
		 * Sets the currently active encounter.
		 *
		 * The encounter will trigger upon loading.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setEncounter( EncounterState encounter ) {
			this.encounter = encounter;
		}
		public EncounterState getEncounter() { return encounter; }

		public void setNearbyShipState( ShipState shipState ) {
			this.nearbyShipState = shipState;
		}
		public ShipState getNearbyShipState() { return nearbyShipState; }

		/**
		 * Sets info about the next encounter with the rebel flagship.
		 *
		 * Since FTL 1.5.4, this is no longer saved.
		 */
		public void setRebelFlagshipState( RebelFlagshipState flagshipState ) {
			this.rebelFlagshipState = flagshipState;
		}
		public RebelFlagshipState getRebelFlagshipState() {
			return rebelFlagshipState;
		}

		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public void setUnknownKappa( int n ) { unknownKappa = n; }

		public int getUnknownGamma() { return unknownGamma; }
		public int getUnknownDelta() { return unknownDelta; }
		public int getUnknownEpsilon() { return unknownEpsilon; }
		public int getUnknownKappa() { return unknownKappa; }

		public void setUnknownMu( int n ) { unknownMu = n; }
		public int getUnknownMu() { return unknownMu; }

		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}

		public ArrayList<MysteryBytes> getMysteryList() { return mysteryList; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			String formatDesc = null;
			switch ( unknownHeaderAlpha ) {
				case( 2 ): formatDesc = "Saved Game, FTL 1.01-1.03.3"; break;
				case( 7 ): formatDesc = "Saved Game, FTL 1.5.4+"; break;
				default: formatDesc = "???"; break;
			}

			String difficultyString = null;
			if ( difficulty == 0 ) { difficultyString = "Easy"; }
			else if ( difficulty == 1 ) { difficultyString = "Normal"; }
			else if ( difficulty == 2 ) { difficultyString = "Hard"; }
			else { difficultyString = String.format("%4d (???)", difficulty); }

			boolean first = true;
			result.append(String.format("File Format:            %5d (%s)\n", unknownHeaderAlpha, formatDesc));
			result.append(String.format("AE Content:             %5b\n", (dlcEnabled ? "Enabled" : "Disabled") ));
			result.append(String.format("Ship Name:  %s\n", playerShipName));
			result.append(String.format("Ship Type:  %s\n", playerShipBlueprintId));
			result.append(String.format("Difficulty: %s\n", difficultyString ));
			result.append(String.format("Sector:                 %5d (%d)\n", sectorNumber, sectorNumber+1));
			result.append(String.format("Beta?:                  %5d (Always 0?)\n", unknownBeta));
			result.append(String.format("Total Ships Defeated:   %5d\n", totalShipsDefeated));
			result.append(String.format("Total Beacons Explored: %5d\n", totalBeaconsExplored));
			result.append(String.format("Total Scrap Collected:  %5d\n", totalScrapCollected));
			result.append(String.format("Total Crew Hired:       %5d\n", totalCrewHired));

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
			result.append(String.format("Sector Tree Seed:   %5d\n", sectorTreeSeed));
			result.append(String.format("Sector Layout Seed: %5d\n", sectorLayoutSeed));
			result.append(String.format("Rebel Fleet Offset: %5d\n", rebelFleetOffset));
			result.append(String.format("Rebel Fleet Fudge:  %5d\n", rebelFleetFudge));
			result.append(String.format("Rebel Pursuit Mod:  %5d\n", rebelPursuitMod));
			result.append(String.format("Sector Hazards Map: %5b\n", sectorHazardsVisible));
			result.append("\n");
			result.append(String.format("In Hidden Sector:   %5b\n", sectorIsHiddenCrystalWorlds));
			result.append(String.format("Rebel Flagship On:  %5b\n", rebelFlagshipVisible));
			result.append(String.format("Flagship Nth Hop:   %5d\n", rebelFlagshipHop));
			result.append(String.format("Flagship Moving:    %5b\n", rebelFlagshipMoving));
			result.append(String.format("Flagship Base Turns:%5d\n", rebelFlagshipBaseTurns));
			result.append("\n");
			result.append(String.format("Player BeaconId:    %5d\n", currentBeaconId));
			result.append(String.format("Gamma?:             %5d\n", unknownGamma));
			result.append(String.format("Delta?:             %5d\n", unknownDelta));
			result.append(String.format("Epsilon?:           %5d\n", unknownEpsilon));
			result.append(String.format("Kappa?:             %5d\n", unknownKappa));

			result.append("\nSector Tree Breadcrumbs...\n");
			first = true;
			for ( Boolean b : sectorList ) {
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
				result.append(String.format("BeaconId: %2d\n", beaconId++));
				result.append( beacon.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			result.append("\nQuests...\n");
			for ( Map.Entry<String, Integer> entry : questEventMap.entrySet() ) {
				String questEventId = entry.getKey();
				int questBeaconId = entry.getValue().intValue();
				result.append(String.format("QuestEventId: %s, BeaconId: %d\n", questEventId, questBeaconId));
			}

			result.append("\nNext Sector Quests...\n");
			for ( String questEventId : distantQuestEventList ) {
				result.append(String.format("QuestEventId: %s\n", questEventId));
			}

			result.append("\n");
			result.append(String.format("Mu?:                %5d\n", unknownMu));

			result.append("\nCurrent Encounter...\n");
			if ( encounter != null )
				result.append(encounter.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nNearby Ship...\n");
			if ( nearbyShipState != null )
				result.append(nearbyShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nRebel Flagship... (Not set when parsing FTL 1.5.4+ saved games)\n");
			if ( rebelFlagshipState != null )
				result.append(rebelFlagshipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nMystery Bytes...\n");
			first = true;
			for ( MysteryBytes m : mysteryList ) {
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
		                                                 // Reserve + Battery can exceed this.

		private boolean auto = false;  // Is autoShip.
		private String shipName, shipBlueprintId, shipLayoutId;
		private String shipGfxBaseName;
		private ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private int jumpTicks = 0;
		private int hullAmt=0, fuelAmt=0, dronePartsAmt=0, missilesAmt=0, scrapAmt=0;
		private ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity = 0;
		private LinkedHashMap<SystemType, SystemState> systemMap = new LinkedHashMap<SystemType, SystemState>();
		private ArrayList<RoomState> roomList = new ArrayList<RoomState>();
		private LinkedHashMap<Point, Integer> breachMap = new LinkedHashMap<Point, Integer>();
		private LinkedHashMap<ShipLayout.DoorCoordinate, DoorState> doorMap = new LinkedHashMap<ShipLayout.DoorCoordinate, DoorState>();
		private ArrayList<WeaponState> weaponList = new ArrayList<WeaponState>();
		private ArrayList<DroneState> droneList = new ArrayList<DroneState>();
		private ArrayList<String> augmentIdList = new ArrayList<String>();

		private int unknownAlpha = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;

		private int unknownEpsilon = 0;
		private int unknownZeta = 0;
		private int unknownEta = 0;
		private int unknownTheta = 0;
		private List<ExtendedSystemInfo> unknownIota = new ArrayList<ExtendedSystemInfo>();

		private int unknownPhi = 0;


		/**
		 * Constructs an incomplete ShipState.
		 *
		 * It will need systems, reserve power, rooms, doors, and supplies.
		 */
		public ShipState( String shipName, ShipBlueprint shipBlueprint, boolean auto ) {
			this( shipName, shipBlueprint.getId(), shipBlueprint.getLayout(), shipBlueprint.getGraphicsBaseName(), auto );
		}

		/**
		 * Constructs an incomplete ShipState.
		 *
		 * It will need systems, reserve power, rooms, doors, and supplies.
		 */
		public ShipState( String shipName, String shipBlueprintId, String shipLayoutId, String shipGfxBaseName, boolean auto ) {
			this.shipName = shipName;
			this.shipBlueprintId = shipBlueprintId;
			this.shipLayoutId = shipLayoutId;
			this.shipGfxBaseName = shipGfxBaseName;
			this.auto = auto;
		}

		/**
		 * Assigns the missing defaults of an incomplete ship.
		 *
		 * Based on its ShipBlueprint, the following will be set:
		 *   Systems, reserve power, rooms, doors, augments, and supplies.
		 *
		 * Reserve power will be the total of all system rooms' initial 'power'
		 * (aka minimum random capacity), capped by the shipBlueprint's
		 * maxPower.
		 */
		public void refit() {
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBlueprintId );
			ShipLayout shipLayout = DataManager.get().getShipLayout( shipBlueprint.getLayout() );

			// Systems.
			getSystemMap().clear();
			int powerRequired = 0;
			for ( SystemType systemType : SystemType.values() ) {
				SavedGameParser.SystemState systemState = new SavedGameParser.SystemState( systemType );

				// Set capacity for systems that're initially present.
				ShipBlueprint.SystemList.SystemRoom[] systemRoom = shipBlueprint.getSystemList().getSystemRoom( systemType );
				if ( systemRoom != null ) {
					Boolean start = systemRoom[0].getStart();
					if ( start == null || start.booleanValue() == true ) {
						SystemBlueprint systemBlueprint = DataManager.get().getSystem( systemType.getId() );
						systemState.setCapacity( systemBlueprint.getStartPower() );

						// The optional room max attribute caps randomly generated ships' system capacity.
						if ( systemRoom[0].getMaxPower() != null ) {
							systemState.setCapacity( systemRoom[0].getMaxPower().intValue() );
						}

						if ( systemType.isSubsystem() ) {
							// Give subsystems all the power they want.
							systemState.setPower( systemState.getCapacity() );
						} else {
							// The room power attribute is for initial system power usage (or minimum if for randomly generated ships).
							powerRequired += systemRoom[0].getPower();
						}
					}
				}
				addSystem( systemState );
			}
			if ( powerRequired > shipBlueprint.getMaxPower().amount ) {
				powerRequired = shipBlueprint.getMaxPower().amount;
			}
			setReservePowerCapacity( powerRequired );

			// Rooms.
			getRoomList().clear();
			for (int r=0; r < shipLayout.getRoomCount(); r++) {
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(r);
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				SavedGameParser.RoomState roomState = new SavedGameParser.RoomState();
				for (int s=0; s < squaresH*squaresV; s++) {
					roomState.addSquare( 0, 0, -1);
				}
				addRoom( roomState );
			}

			// Doors.
			getDoorMap().clear();
			Map<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> layoutDoorMap = shipLayout.getDoorMap();
			for ( Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : layoutDoorMap.entrySet() ) {
				ShipLayout.DoorCoordinate doorCoord = entry.getKey();
				EnumMap<ShipLayout.DoorInfo,Integer> doorInfo = entry.getValue();

				setDoor( doorCoord.x, doorCoord.y, doorCoord.v, new SavedGameParser.DoorState() );
			}

			// Augments.
			getAugmentIdList().clear();
			if ( shipBlueprint.getAugments() != null ) {
				for ( ShipBlueprint.AugmentId augId : shipBlueprint.getAugments() )
					addAugmentId( augId.name );
			}

			// Supplies.
			setHullAmt( shipBlueprint.getHealth().amount );
			setFuelAmt( 20 );
			setDronePartsAmt( 0 );
			setMissilesAmt( 0 );
			if ( shipBlueprint.getDroneList() != null )
				setDronePartsAmt( shipBlueprint.getDroneList().drones );
			if ( shipBlueprint.getWeaponList() != null )
				setMissilesAmt( shipBlueprint.getWeaponList().missiles );
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

		/**
		 * Sets time elapsed while waiting for the FTL drive to charge.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setJumpTicks( int n ) { jumpTicks = n; }
		public int getJumpTicks() { return jumpTicks; }

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

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownGamma() { return unknownGamma; }
		public int getUnknownDelta() { return unknownDelta; }

		public void setUnknownIota( List<ExtendedSystemInfo> iotaList ) { unknownIota = iotaList; }
		public List<ExtendedSystemInfo> getUnknownIota() { return unknownIota; }

		public void setUnknownPhi( int n ) { unknownPhi = n; }
		public int getUnknownPhi() { return unknownPhi; }

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
			systemMap.put( s.getSystemType(), s );
		}
		public SystemState getSystem( SystemType systemType ) {
			return systemMap.get( systemType );
		}

		public LinkedHashMap<SystemType, SystemState> getSystemMap() { return systemMap; }

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
		 * @param d a DoorState
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
			result.append("\n");
			result.append(String.format("Alpha?:      %3d\n", unknownAlpha));
			result.append(String.format("Jump Ticks:  %3d (85000 is fully charged)\n", jumpTicks));
			result.append(String.format("Gamma?:      %3d\n", unknownGamma));
			result.append(String.format("Delta?:      %3d\n", unknownDelta));

			result.append("\nStarting Crew...\n");
			first = true;
			for ( StartingCrewState sc : startingCrewList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(sc.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nCurrent Crew...\n");
			first = true;
			for ( CrewState c : crewList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(c.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nSystems...\n");
			result.append(String.format("  Reserve Power Capacity: %2d\n", reservePowerCapacity));
			first = false;
			for ( Map.Entry<SystemType, SystemState> entry : systemMap.entrySet() ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(entry.getValue().toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nExtended System Info...\n");
			first = false;
			for ( ExtendedSystemInfo tmpInfo : unknownIota ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(tmpInfo.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nRooms...\n");
			first = true;
			for (ListIterator<RoomState> it=roomList.listIterator(); it.hasNext(); ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				int roomId = it.nextIndex();

				SystemType systemType = blueprintSystems.getSystemTypeByRoomId( roomId );
				String systemId = (systemType != null) ? systemType.getId() : "empty";

				result.append(String.format("RoomId: %2d (%s)\n", roomId, systemId));
				result.append(it.next().toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nHull Breaches...\n");
			int breachId = -1;
			first = true;
			for ( Map.Entry<Point, Integer> entry : breachMap.entrySet() ) {
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

			result.append(String.format("\nPhi?:              %3d\n", unknownPhi));

			result.append("\nWeapons...\n");
			first = true;
			for ( WeaponState w : weaponList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(w.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nDrones...\n");
			first = true;
			for ( DroneState d : droneList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(d.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nAugments...\n");
			for ( String augmentId : augmentIdList ) {
				result.append(String.format("AugmentId: %s\n", augmentId));
			}

			return result.toString();
		}
	}



	public static class StartingCrewState {
		private String name, race;

		public StartingCrewState( String name, String race ) {
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



	public static enum CrewType {
		// TODO: Magic numbers.
		BATTLE ("battle",    150),
		CRYSTAL("crystal",   120),
		ENERGY ("energy",     70),
		ENGI   ("engi",      100),
		GHOST  ("ghost",      50),
		HUMAN  ("human",     100),
		LANIUS ("anaerobic", 100),
		MANTIS ("mantis",    100),
		ROCK   ("rock",      150),
		SLUG   ("slug",      100);

		private String id;
		private int maxHealth;
		private CrewType( String id, int maxHealth ) {
			this.id = id;
			this.maxHealth = maxHealth;
		}
		public String getId() { return id; }
		public int getMaxHealth() { return maxHealth; }
		public String toString() { return id; }

		public static CrewType findById( String id ) {
			for ( CrewType race : values() ) {
				if ( race.getId().equals(id) ) return race;
			}
			return null;
		}

		public static int getMaxHealth( String id ) {
			CrewType race = CrewType.findById( id );
			if ( race != null ) return race.getMaxHealth();
			throw new RuntimeException( "No max health known for crew race: "+ id );
		}
	}

	public static class CrewState {
		// TODO: Magic numbers.
		public static final int MASTERY_INTERVAL_PILOT = 15;   // 13 in FTL 1.5.4.
		public static final int MASTERY_INTERVAL_ENGINE = 15;  // 13 in FTL 1.5.4.
		public static final int MASTERY_INTERVAL_SHIELD = 55;  // 50 in FTL 1.5.4.
		public static final int MASTERY_INTERVAL_WEAPON = 65;  // 58 in FTL 1.5.4.
		public static final int MASTERY_INTERVAL_REPAIR = 18;  // 16 in FTL 1.5.4.
		public static final int MASTERY_INTERVAL_COMBAT = 8;   //  7 in FTL 1.5.4.

		// Crystal crews' lockdown wall-coating is not stored, but ability recharge is.
		// Zoltan-produced power is not stored in SystemState.

		private String name = "Frank";
		private String race = CrewType.HUMAN.getId();
		private boolean enemyBoardingDrone = false;
		private int health = 0;
		private int blueprintRoomId;
		private int roomSquare;  // 0-based, L-to-R wrapped row.
		private boolean playerControlled = false;
		private boolean mindControlled = false;
		private int savedRoomId = 0;
		private int savedRoomSquare = 0;
		private int pilotSkill=0, engineSkill=0, shieldSkill=0;
		private int weaponSkill=0, repairSkill=0, combatSkill=0;
		private int repairs=0, combatKills=0, pilotedEvasions=0;
		private int jumpsSurvived=0, skillMasteries=0;
		private int stunTicks = 0;
		private int spriteX=0, spriteY=0;
		private List<Integer> spriteTintIndeces = new ArrayList<Integer>();
		private boolean male=true;

		private int unknownAlpha = 0;
		private int unknownBeta = 0;

		private int unknownTheta = 0;
		private int unknownIota = 0;
		private int unknownKappa = 0;
		private int unknownLambda = 0;
		private int unknownMu = 0;
		private int unknownNu = 0;
		private int unknownXi = 0;
		private int unknownOmicron = 0;
		private int unknownPi = 0;
		private int unknownRho = 0;
		private int unknownSigma = 0;
		private int unknownTau = 0;
		private int unknownUpsilon = 0;
		private int unknownPhi = 0;

		private int lockdownRechargeTicks = 0;
		private int lockdownRechargeGoal = 0;
		private int unknownOmega = 0;


		public CrewState() {
		}

		public void setName( String s ) {name = s; }
		public void setRace( String s ) {race = s; }
		public void setHealth( int n ) {health = n; }
		public void setRoomId( int n ) {blueprintRoomId = n; }
		public void setRoomSquare( int n ) { roomSquare = n; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }

		public String getName() { return name; }
		public String getRace() { return race; }
		public int getHealth() { return health; }
		public int getRoomId() { return blueprintRoomId; }
		public int getRoomSquare() { return roomSquare; }
		public boolean isPlayerControlled() { return playerControlled; }

		/**
		 * Sets whether this crew is mind controlled.
		 *
		 * While setPlayerControlled() is permanent this temporarily yields
		 * control to the opposing side.
		 *
		 * TODO: Determine what circumstances cause this to wear off.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setMindControlled( boolean b ) { mindControlled = b; }
		public boolean isMindControlled() { return mindControlled; }

		/**
		 * Sets a saved position to return to.
		 *
		 * roomId and roomSquare need to be specified together.
		 *
		 * The "return crew to saved positions" feature was
		 * introduced in FTL 1.5.4.
		 */
		public void setSavedRoomId( int n ) { savedRoomId = n; }
		public int getSavedRoomId() { return savedRoomId; }
		public void setSavedRoomSquare( int n ) { savedRoomSquare = n; }
		public int getSavedRoomSquare() { return savedRoomSquare; }

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
		 * Sets time required for stun to wear off.
		 *
		 * If greater than 0, the crew will become unresponsive while this
		 * number decrements to 0. Additional stuns will probably add to it.
		 *
		 * A weapon adds X*1000 ticks, where X is the value of the 'stun' tag
		 * in its WeaponBlueprint xml.
		 *
		 * When not stunned, this will be 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setStunTicks( int n ) { stunTicks = n; }
		public int getStunTicks() { return stunTicks; }

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }

		public void setUnknownTheta( int n ) { unknownTheta = n; }
		public void setUnknownIota( int n ) { unknownIota = n; }
		public void setUnknownKappa( int n ) { unknownKappa = n; }
		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public void setUnknownMu( int n ) { unknownMu = n; }
		public void setUnknownNu( int n ) { unknownNu = n; }
		public void setUnknownXi( int n ) { unknownXi = n; }
		public void setUnknownOmicron( int n ) { unknownOmicron = n; }
		public void setUnknownPi( int n ) { unknownPi = n; }
		public void setUnknownRho( int n ) { unknownRho = n; }
		public void setUnknownSigma( int n ) { unknownSigma = n; }
		public void setUnknownTau( int n ) { unknownTau = n; }
		public void setUnknownUpsilon( int n ) { unknownUpsilon = n; }
		public void setUnknownPhi( int n ) { unknownPhi = n; }

		public int getUnknownTheta() { return unknownTheta; }
		public int getUnknownIota() { return unknownIota; }
		public int getUnknownKappa() { return unknownKappa; }
		public int getUnknownLambda() { return unknownLambda; }
		public int getUnknownMu() { return unknownMu; }
		public int getUnknownNu() { return unknownNu; }
		public int getUnknownXi() { return unknownXi; }
		public int getUnknownOmicron() { return unknownOmicron; }
		public int getUnknownPi() { return unknownPi; }
		public int getUnknownRho() { return unknownRho; }
		public int getUnknownSigma() { return unknownSigma; }
		public int getUnknownTau() { return unknownTau; }
		public int getUnknownUpsilon() { return unknownUpsilon; }
		public int getUnknownPhi() { return unknownPhi; }

		/**
		 * Sets time elapsed waiting for the lockdown ability to recharge.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @param n a positive int less than, or equal to, the goal (0 when not charging)
		 *
		 * @see #setLockdownRechargeGoal(int)
		 */
		public void setLockdownRechargeTicks( int n ) { lockdownRechargeTicks = n; }
		public int getLockdownRechargeTicks() { return lockdownRechargeTicks; }

		/**
		 * Sets the ticks needed to recharge the lockdown ability.
		 *
		 * This is normally 50000 while charging, 0 otherwise.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setLockdownRechargeTicks(int)
		 */
		public void setLockdownRechargeGoal( int n ) { lockdownRechargeGoal = n; }
		public int getLockdownRechargeGoal() { return lockdownRechargeGoal; }

		public void setUnknownOmega( int n ) { unknownOmega = n; }
		public int getUnknownOmega() { return unknownOmega; }

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
		 * Sets a list of tints to apply to the sprite.
		 *
		 * The tints themselves are defined in
		 * blueprints.xml:crewBlueprint/colorList
		 *
		 * The first Integer in the list corresponds to the first 'layer' tag
		 * in the xml, and the Integer's value is the nth 'color' tag to use.
		 *
		 * Note: Normal NPC crew have a hardcoded range of layer/color indeces
		 * based on stock blueprints. When mods add layers to a race blueprint,
		 * NPCs that spawn won't use them. When mods remove layers, NPCs that
		 * reference the missing layer/colors display as a gray rectangle.
		 *
		 * Stock layers(colors): anaerobic=1(4), crystal=1(4), energy=1(5),
		 * engi=0(0), human=2(2/4), mantis=1(9), rock=1(7) slug=1(8).
		 *
		 * TODO: Test if FTL honors non-standard tints of edited NPCs.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setSpriteTintIndeces( List<Integer> indeces ) {
			spriteTintIndeces = indeces;
		}
		public List<Integer> getSpriteTintIndeces() {
			return spriteTintIndeces;
		}

		/**
		 * Toggles sex.
		 * Humans with this set to false have a
		 * female image. Other races accept the
		 * flag but use the same image as male.
		 *
		 * No Andorians in the game, so this is
		 * only a two-state boolean.
		 */
		public void setMale( boolean b ) {
			male = b;
		}
		public boolean isMale() { return male; }

		/**
		 * Sets whether this crew member is a hostile boarding drone.
		 *
		 * Upon loading after setting this on a crew member,
		 * name will change to "Anti-Personnel Drone", race
		 * will be "battle", and playerControlled will be
		 * false on the player ship or true on a nearby ship.
		 *
		 * If after loading in-game, you re-edit this to false
		 * and leave the "battle" race, the game will change
		 * it to "human".
		 *
		 * Drones on nearby ships (which are playerControlled)
		 * will not be preserved the next time the game saves,
		 * even if you modify the player ship's drone list to
		 * have an armed boarder.
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
			boolean first = true;

			CrewBlueprint crewBlueprint = DataManager.get().getCrew( race );

			List<CrewBlueprint.SpriteTintLayer> tintLayerList = null;
			if ( crewBlueprint != null ) {
				tintLayerList = crewBlueprint.getSpriteTintLayerList();
			}

			result.append(String.format("Name:              %s\n", name));
			result.append(String.format("Race:              %s\n", race));
			result.append(String.format("Enemy Drone:       %5b\n", enemyBoardingDrone));
			result.append(String.format("Sex:               %s\n", (male ? "Male" : "Female") ));
			result.append(String.format("Health:            %5d\n", health));
			result.append(String.format("Sprite Position:    %3d,%3d\n", spriteX, spriteY));
			result.append(String.format("RoomId:            %5d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %5d\n", roomSquare));
			result.append(String.format("Player Controlled: %5b\n", playerControlled));
			result.append(String.format("Alpha?:            %5d\n", unknownAlpha));
			result.append(String.format("Beta?:             %5d\n", unknownBeta));
			result.append(String.format("Mind Controlled:   %5b\n", mindControlled));

			result.append("\nSprite Tints...\n");
			for (int i=0; i < spriteTintIndeces.size(); i++) {
				Integer colorIndex = spriteTintIndeces.get(i);

				String colorHint = null;
				if ( tintLayerList != null && i < tintLayerList.size() ) {
					CrewBlueprint.SpriteTintLayer tintLayer = tintLayerList.get( i );
					if ( tintLayer.tintList != null && colorIndex < tintLayer.tintList.size() && colorIndex >= 0 ) {
						CrewBlueprint.SpriteTintLayer.SpriteTintColor tintColor = tintLayer.tintList.get( colorIndex );
						colorHint = String.format("r=%3d,g=%3d,b=%3d,a=%.1f", tintColor.r, tintColor.g, tintColor.b, tintColor.a);
					} else {
						colorHint = "Color not in blueprint's layer.";
					}
				} else {
					colorHint = "Layer not in blueprint's colorList.";
				}

				result.append(String.format("  Layer %2d: Color: %3d (%s)\n", i, colorIndex, colorHint));
			}
			result.append("\n");

			result.append(String.format("Saved RoomId:      %5d\n", savedRoomId));
			result.append(String.format("Saved Room Square: %5d\n", savedRoomSquare));
			result.append(String.format("Pilot Skill:       %5d (Mastery Interval: %2d)\n", pilotSkill, MASTERY_INTERVAL_PILOT));
			result.append(String.format("Engine Skill:      %5d (Mastery Interval: %2d)\n", engineSkill, MASTERY_INTERVAL_ENGINE));
			result.append(String.format("Shield Skill:      %5d (Mastery Interval: %2d)\n", shieldSkill, MASTERY_INTERVAL_SHIELD));
			result.append(String.format("Weapon Skill:      %5d (Mastery Interval: %2d)\n", weaponSkill, MASTERY_INTERVAL_WEAPON));
			result.append(String.format("Repair Skill:      %5d (Mastery Interval: %2d)\n", repairSkill, MASTERY_INTERVAL_REPAIR));
			result.append(String.format("Combat Skill:      %5d (Mastery Interval: %2d)\n", combatSkill, MASTERY_INTERVAL_COMBAT));
			result.append(String.format("Repairs:           %5d\n", repairs));
			result.append(String.format("Combat Kills:      %5d\n", combatKills));
			result.append(String.format("Piloted Evasions:  %5d\n", pilotedEvasions));
			result.append(String.format("Jumps Survived:    %5d\n", jumpsSurvived));
			result.append(String.format("Skill Masteries:   %5d\n", skillMasteries));
			result.append(String.format("Stun Ticks:       %6d (Decrements to 0)\n", stunTicks));
			result.append(String.format("Theta?:           %6d\n", unknownTheta));
			result.append(String.format("Iota?:            %6d\n", unknownIota));
			result.append(String.format("Kappa?:           %6d\n", unknownKappa));
			result.append(String.format("Lambda?:          %6d\n", unknownLambda));
			result.append(String.format("Mu?:              %6d\n", unknownMu));
			result.append(String.format("Nu?:              %6d\n", unknownNu));
			result.append(String.format("Xi?:              %6d\n", unknownXi));
			result.append(String.format("Omicron?:         %6d\n", unknownOmicron));
			result.append(String.format("Pi?:              %6d\n", unknownPi));
			result.append(String.format("Rho?:             %6d\n", unknownRho));
			result.append(String.format("Sigma?:           %6d\n", unknownSigma));
			result.append(String.format("Tau?:             %6d\n", unknownTau));
			result.append(String.format("Upsilon?:         %6d\n", unknownUpsilon));
			result.append(String.format("Phi?:             %6d\n", unknownPhi));
			result.append(String.format("Lockdown Ticks:   %6d (Crystal only, time spent recharging)\n", lockdownRechargeTicks));
			result.append(String.format("Lockdown Goal:    %6d (Crystal only, ticks needed to recharge)\n", lockdownRechargeGoal));
			result.append(String.format("Omega?:           %6d (Crystal only)\n", unknownOmega));
			return result.toString();
		}
	}



	public static enum SystemType {
		// SystemType ids are tied to "img/icons/s_*_overlay.png" and store item ids.
		// TODO: Magic booleans.
		PILOT     ("pilot",      true),
		DOORS     ("doors",      true),
		SENSORS   ("sensors",    true),
		MEDBAY    ("medbay",     false),
		OXYGEN    ("oxygen",     false),
		SHIELDS   ("shields",    false),
		ENGINES   ("engines",    false),
		WEAPONS   ("weapons",    false),
		DRONE_CTRL("drones",     false),
		TELEPORTER("teleporter", false),
		CLOAKING  ("cloaking",   false),
		ARTILLERY ("artillery",  false),
		BATTERY   ("battery",    false),
		CLONEBAY  ("clonebay",   false),
		MIND      ("mind",      false),
		HACKING   ("hacking",    false);

		private String id;
		private boolean subsystem;
		private SystemType( String id, boolean subsystem ) {
			this.id = id;
			this.subsystem = subsystem;
		}
		public String getId() { return id; }
		public boolean isSubsystem() { return subsystem; }
		public String toString() { return id; }

		public static SystemType findById(String id) {
			for ( SystemType s : values() ) {
				if ( s.getId().equals(id) ) return s;
			}
			return null;
		}
	}

	public static class SystemState {
		public static final int MAX_IONIZED_BARS = 9;  // TODO: Magic number.

		private SystemType systemType;
		private int capacity = 0;
		private int power = 0;
		private int damagedBars = 0;
		private int ionizedBars = 0;
		private int repairProgress = 0;
		private int damageProgress = 0;
		private int deionizationTicks = Integer.MIN_VALUE;

		private int batteryPower = 0;

		private int temporaryCapacityCap = 1000;
		private int temporaryCapacityLoss = 0;
		private int temporaryCapacityDivisor = 1;

		private int hackLevel = 0;
		private boolean hacked = false;


		public SystemState( SystemType systemType ) {
			this.systemType = systemType;
		}

		public SystemType getSystemType() { return systemType; }

		/**
		 * Sets the number of power bars this system can use.
		 *
		 * A capacity of zero means the system is not currently installed.
		 */
		public void setCapacity( int n ) { capacity = n; }
		public int getCapacity() { return capacity; }

		/**
		 * Sets the number of reserve power bars assigned to this system.
		 *
		 * @see #setBatteryPower(int)
		 */
		public void setPower( int n ) { power = n; }
		public int getPower() { return power; }

		/**
		 * Sets the number of unusable power bars, in need of repair.
		 */
		public void setDamagedBars( int n ) { damagedBars = n; }
		public int getDamagedBars() { return damagedBars; }

		/**
		 * Sets the number of temporarily disabled power bars.
		 *
		 * This number may exceed the total number of power bars on a system,
		 * so removing all the ionization bars takes longer. This must be less
		 * than or equal to MAX_IONIZED_BARS, or the game's interface will be
		 * unable to find an image to display the number, and a warning graphic
		 * will appear.
		 *
		 * When a system disables itself, this may briefly be -1 initially. Then
		 * the count of ionized bars will be set to capacity + 1.
		 */
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public int getIonizedBars() { return ionizedBars; }

		/**
		 * Sets progress toward repairing one damaged power bar.
		 *
		 * A growing portion of the bar will turn yellow.
		 *
		 * @param n 0-100 (0 when not repairing)
		 */
		public void setRepairProgress( int n ) { repairProgress = n; }
		public int getRepairProgress() { return repairProgress; }

		/**
		 * Sets progress toward damaging one power bar.
		 *
		 * A growing portion of the bar will turn red.
		 * This is typically caused by boarders attempting sabotage.
		 *
		 * @param n 0-100 (0 when not damaging)
		 */
		public void setDamageProgress( int n ) { damageProgress = n; }
		public int getDamageProgress() { return damageProgress; }

		/**
		 * Sets elapsed time while waiting to remove each ionized power bar.
		 *
		 * The system is inoperative while any ionized bars remain, and any
		 * power assigned will be unavailable. If this system is using battery
		 * power, and the battery deactivates, deionization will complete
		 * immediately.
		 *
		 * The game's interface responds as this increments, including
		 * resetting after intervals. If not needed, it may be 0, or
		 * more often, MIN_INT (signed 32bit \x0000_0080) of the
		 * compiler that built FTL. This parser will translate that
		 * to Java's equivalent minimum during reading, and back during
		 * writing.
		 *
		 * Deionization of each bar counts to 5000.
		 *
		 * TODO:
		 * Nearly every system has been observed with non-zero values,
		 * but aside from Teleporter/Cloaking, normal use doesn't reliably
		 * set such values. Might be unspecified garbage when not actively
		 * counting. Sometimes has huge positive and negative values.
		 *
		 * This value is reset upon loading.
		 * (TODO: Check if still true in FTL 1.5.4.)
		 */
		public void setDeionizationTicks( int n ) { deionizationTicks = n; }
		public int getDeionizationTicks() { return deionizationTicks; }


		/**
		 * Sets the number of battery power bars assigned to this system.
		 *
		 * The power bars will have an orange border and will be lost when the
		 * battery system is fully discharged.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setPower(int)
		 */
		public void setBatteryPower( int n ) { batteryPower = n; }
		public int getBatteryPower() { return batteryPower; }

		/**
		 * Sets the level-based effect that a hacking drone's disruption will
		 * have when it activates.
		 *
		 * TODO: Revise this description.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @param n the 1-based level of the hacker's system, or 0 for none
		 *
		 * @see #setHacked(int)
		 */
		public void setHackLevel( int n ) { hackLevel = n; }
		public int getHackLevel() { return hackLevel; }

		/**
		 * Toggles whether this system has a hacking drone attached.
		 *
		 * TODO: Revise this description.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setHackLevel(int)
		 */
		public void setHacked( boolean b ) { hacked = b; }
		public boolean isHacked() { return hacked; }

		/**
		 * Sets an upper limit on this system's capacity.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 * Upon setting this, you need to reset the other temporary handicaps.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "limit=" attribute.
		 *
		 * Under normal circumstances, the cap is 1000.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setTemporaryCapacityLoss(int)
		 * @see #setTemporaryCapacityDivisor(int)
		 */
		public void setTemporaryCapacityCap( int n ) { temporaryCapacityCap = n; }
		public int getTemporaryCapacityCap() { return temporaryCapacityCap; }

		/**
		 * Sets a number to subtract from this system's capacity.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "loss=" attribute.
		 *
		 * Under normal circumstances, the loss is 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setTemporaryCapacityCap(int)
		 * @see #setTemporaryCapacityDivisor(int)
		 */
		public void setTemporaryCapacityLoss( int n ) { temporaryCapacityLoss = n; }
		public int getTemporaryCapacityLoss() { return temporaryCapacityLoss; }

		/**
		 * Sets a number to divide this system's capacity by, rounded down.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "divide=" attribute.
		 *
		 * Under normal circumstances, the divisor is 1.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setTemporaryCapacityCap(int)
		 * @see #setTemporaryCapacityLoss(int)
		 */
		public void setTemporaryCapacityDivisor( int n ) { temporaryCapacityDivisor = n; }
		public int getTemporaryCapacityDivisor() { return temporaryCapacityDivisor; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			if (capacity > 0) {
				result.append(String.format("SystemId:              %s\n", systemType.getId()));
				result.append(String.format("Power:                  %d/%d\n", power, capacity));
				result.append(String.format("Damaged Bars:          %3d\n", damagedBars));
				result.append(String.format("Ionized Bars:          %3d\n", ionizedBars));
				result.append(String.format("Repair Progress:       %3d%%\n", repairProgress));
				result.append(String.format("Damage Progress:       %3d%%\n", damageProgress));
				result.append(String.format("Deionization Ticks:    %s\n", (deionizationTicks==Integer.MIN_VALUE ? "N/A" : deionizationTicks) ));
				result.append("\n");
				result.append(String.format("Battery Power:         %3d\n", batteryPower));
				result.append(String.format("Hack Level:            %3d\n", hackLevel));
				result.append(String.format("Hacked:                %5b\n", hacked));
				result.append(String.format("Temp Capacity Cap:     %3d\n", temporaryCapacityCap));
				result.append(String.format("Temp Capacity Loss:    %3d\n", temporaryCapacityLoss));
				result.append(String.format("Temp Capacity Divisor: %3d\n", temporaryCapacityDivisor));
			} else {
				result.append(String.format("%s: N/A\n", systemType.getId()));
			}
			return result.toString();
		}
	}



	public static class RoomState {
		private int oxygen = 100;
		private ArrayList<SquareState> squareList = new ArrayList<SquareState>();

		private int stationSquare = -1;
		private int stationDirection = 4;


		public RoomState() {
		}

		/**
		 * Set's the oxygen percentage in the room.
		 *
		 * At 0, the game changes the room's appearance.
		 *   Since 1.03.1, it paints red stripes on the floor.
		 *   Before that, it highlighted the walls orange.
		 *
		 * @param n 0-100
		 */
		public void setOxygen( int n ) { oxygen = n; }
		public int getOxygen() { return oxygen; }

		/**
		 * Sets a room square for a station, to man a system.
		 *
		 * The station's direction must be set as well.
		 *
		 * This was introduced in FTL 1.5.4.
		 * @param n the room square index, or -1 for none
		 */
		public void setStationSquare( int n ) { stationSquare = n; }
		public int getStationSquare() { return stationSquare; }

		/**
		 * Sets which edge of a room square a station should be placed at.
		 *
		 * The station's room square must be set as well.
		 *
		 * This was introduced in FTL 1.5.4.
		 * @param n 0=D,1=R,2=U,3=L,4=None
		 */
		public void setStationDirection( int n ) { stationDirection = n; }
		public int getStationDirection() { return stationDirection; }

		/**
		 * Adds a floor square to the room.
		 * Squares are indexed horizontally, left-to-right, wrapping
		 * into the next row down.
		 *
		 * Squares adjacent to a fire grow closer to igniting as
		 * time passes. Then a new fire spawns in them at full health.
		 *
		 * Matthew says the third arg (-1) is a bug, seen in FTL 1.01-1.5.10.
		 * It should be extinguishmentProgress.
		 *
		 * @param fireHealth 0 to 100.
		 * @param ignitionProgress 0 to 100.
		 * @param gamma always -1?
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

			String dirDesc;
			switch ( stationDirection ) {
				case( 0 ): dirDesc = "Down"; break;
				case( 1 ): dirDesc = "Right"; break;
				case( 2 ): dirDesc = "Up"; break;
				case( 3 ): dirDesc = "Left"; break;
				case( 4 ): dirDesc = "N/A"; break;
				default: dirDesc = "???"; break;
			}

			result.append(String.format("Oxygen: %3d%%\n", oxygen));
			result.append(String.format("Station Square: %2d, Station Direction: %d (%s)\n", stationSquare, stationDirection, dirDesc));

			result.append("Squares...\n");
			for (SquareState square : squareList) {
				result.append(square.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			return result.toString();
		}
	}

	public static class SquareState {
		private int fireHealth = 0;
		private int ignitionProgress = 0;
		private int unknownGamma = -1;

		// See RoomState.addSquare() for javadocs.


		public SquareState( int fireHealth, int ignitionProgress, int gamma ) {
			this.fireHealth = fireHealth;
			this.ignitionProgress = ignitionProgress;
			this.unknownGamma = gamma;
		}

		public SquareState() {
		}

		/**
		 * Copy constructor.
		 */
		public SquareState( SquareState srcSquare ) {
			this.fireHealth = srcSquare.getFireHealth();
			this.ignitionProgress = srcSquare.getIgnitionProgress();
			this.unknownGamma = srcSquare.getUnknownGamma();
		}

		public void setFireHealth( int n ) { fireHealth = n; }
		public int getFireHealth() { return fireHealth; }

		public void setIgnitionProgress( int n ) { ignitionProgress = n; }
		public int getIgnitionProgress() { return ignitionProgress; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Fire HP: %3d, Ignition: %3d%%, Extinguishment: %2d\n", fireHealth, ignitionProgress, unknownGamma));

			return result.toString();
		}
	}



	public static class DoorState {
		private boolean open = false;
		private boolean walkingThrough = false;

		private int currentMaxHealth = 0;
		private int health = 0;
		private int nominalHealth = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;


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


		/**
		 * Sets current max door health.
		 *
		 * This is affected by situational modifiers like Crystal lockdown,
		 * but it likely copies the nominal value at some point.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setCurrentMaxHealth( int n ) { currentMaxHealth = n; }
		public int getCurrentMaxHealth() { return currentMaxHealth; }

		/**
		 * Sets the current door health.
		 *
		 * Starting at current max, this decreases as someone tries to break it
		 * down.
		 *
		 * TODO: After combat in which a hacking drone boosts the door's health,
		 * the current max returns to normal, but the actual health stays high
		 * for some reason.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		/**
		 * Sets nominal max door health.
		 * This is the value to which the current max will eventually reset.
		 *
		 * Values:
		 *   04 = Level 0 (un-upgraded or damaged door system).
		 *   08 = Level 1
		 *   12 = Level 2
		 *   18 = Level 3 (max, plus manned)
		 *   50 = Lockdown.
		 *
		 * TODO: Investigate why an attached hacking drone adds 6 to ALL THREE
		 * healths.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setNominalHealth( int n ) { nominalHealth = n; }
		public int getNominalHealth() { return nominalHealth; }

		/**
		 * ???
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets hacking drone lockdown status.
		 *
		 * Values:
		 *   0 = N/A
		 *   1 = Hacking drone attached, but not active.
		 *   2 = Hacking drone attached and active.
		 *
		 * A hacking system launches a drone that will latch onto a target
		 * system room, granting visibility. While the hacking drone is active
		 * and there is power to the hacking system, the doors of the room turn
		 * purple, locked to the crew of the targeted ship, but passable to the
		 * hacker's crew.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Open: %-5b, Walking Through: %-5b\n", open, walkingThrough));
			result.append(String.format("Full HP: %3d, Current HP: %3d, Nominal HP: %3d, Delta?: %3d, Epsilon?: %3d\n", currentMaxHealth, health, nominalHealth, unknownDelta, unknownEpsilon));

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
		 *
		 * This increments from 0 each second until the
		 * weapon blueprint's cooldown. 0 when not armed.
		 *
		 * Since FTL 1.5.4, this is no longer saved.
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



	public static enum DroneType {
		// TODO: Magic numbers.
		BATTLE     ("BATTLE",      150),
		REPAIR     ("REPAIR",       25),
		BOARDER    ("BOARDER",       1),
		COMBAT     ("COMBAT",        1),
		DEFENSE    ("DEFENSE",       1),
		SHIP_REPAIR("SHIP_REPAIR",   1);

		private String id;
		private int maxHealth;
		private DroneType( String id, int maxHealth ) {
			this.id = id;
			this.maxHealth = maxHealth;
		}
		public String getId() { return id; }
		public int getMaxHealth() { return maxHealth; }
		public String toString() { return id; }

		public static DroneType findById( String id ) {
			for ( DroneType d : values() ) {
				if ( d.getId().equals(id) ) return d;
			}
			return null;
		}

		public static int getMaxHealth( String id ) {
			DroneType d = DroneType.findById( id );
			if ( d != null ) return d.getMaxHealth();
			throw new RuntimeException( "No max health known for drone type: "+ id );
		}
	}

	public static class DroneState {
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
			result.append(String.format("Armed:             %5b\n", armed));
			result.append(String.format("Health:            %5d\n", health));
			result.append(String.format("Sprite Position:   %5d,%3d\n", spriteX, spriteY));
			result.append(String.format("RoomId:            %5d\n", blueprintRoomId));
			result.append(String.format("Room Square:       %5d\n", roomSquare));
			result.append(String.format("Player Controlled: %5b\n", playerControlled));
			return result.toString();
		}
	}



	public static enum FleetPresence {
		NONE("None"), REBEL("Rebel"), FEDERATION("Federation"), BOTH("Both");

		private String title;
		private FleetPresence( String title ) { this.title = title; }
		public String toString() { return title; }
	}

	public static class BeaconState {
		private boolean visited = false;
		private String bgStarscapeImageInnerPath = null;
		private String bgSpriteImageInnerPath = null;
		private int bgSpritePosX = -1, bgSpritePosY = -1;
		private int bgSpriteRotation = 0;

		private boolean seen = false;

		private boolean enemyPresent = false;
		private String shipEventId = null;
		private String autoBlueprintId = null;
		private int shipEventSeed = 0;

		private FleetPresence fleetPresence = FleetPresence.NONE;

		private boolean underAttack = false;

		private boolean storePresent = false;
		private StoreState store = null;

		// Randomly generated events at unvisited beacons are not
		// stored in the beacons themselves. They're tentatively
		// placed on the sector map in-game, and any that would
		// be distress, shops, etc get signs when seen (or hazard
		// icons when the map is revealed). What and where these
		// events are is determined by the SavedGameGameState's
		// sector layout seed.


		public BeaconState() {
		}

		/**
		 * Sets whether the player has been to this beacon.
		 *
		 * If true, starscape and sprite paths must be set,
		 * as well as the sprite's X, Y, and rotation.
		 *
		 * When true, this prevents randomly generated events
		 * from triggering. The sector exit will still exist.
		 */
		public void setVisited( boolean b ) { visited = b; }
		public boolean isVisited() { return visited; }

		/**
		 * Sets a fullscreen starscape image for the background.
		 *
		 * By convention, this path is from the BG_* imageLists.
		 */
		public void setBgStarscapeImageInnerPath( String s ) {
			bgStarscapeImageInnerPath = s;
		}
		public String getBgStarscapeImageInnerPath() {
			return bgStarscapeImageInnerPath;
		}

		/**
		 * Sets a background sprite to draw over the starscape.
		 *
		 * By convention, this path is from the PLANET_* imageLists.
		 * To not display a sprite, set it to "NONE".
		 */
		public void setBgSpriteImageInnerPath( String s ) {
			bgSpriteImageInnerPath = s;
		}
		public String getBgSpriteImageInnerPath() {
			return bgSpriteImageInnerPath;
		}

		/**
		 * Sets the position of the background sprite image.
		 *
		 * When the sprite's inner path is "NONE",
		 * X and Y should be 0.
		 */
		public void setBgSpritePosX( int n ) { bgSpritePosX = n; }
		public void setBgSpritePosY( int n ) { bgSpritePosY = n; }

		public int getBgSpritePosX() { return bgSpritePosX; }
		public int getBgSpritePosY() { return bgSpritePosY; }

		/**
		 * Sets the rotation of the background sprite image.
		 *
		 * When the sprite's inner path is "NONE",
		 * this should be 0.
		 *
		 * @param n positive degrees clockwise
		 */
		public void setBgSpriteRotation( int n ) { bgSpriteRotation = n; }
		public int getBgSpriteRotation() { return bgSpriteRotation; }

		/**
		 * Sets whether the player has been within one hop of this beacon.
		 */
		public void setSeen( boolean b ) { seen = b; }
		public boolean isSeen() { return seen; }

		/**
		 * Sets whether an enemy ship is waiting at this beacon.
		 *
		 * If true, a ShipEvent and AutoBlueprint must be set,
		 * as well as the unknown beta.
		 */
		public void setEnemyPresent( boolean b ) { enemyPresent = b; }
		public boolean isEnemyPresent() { return enemyPresent; }

		/**
		 * Sets a ShipEvent to trigger upon arrival.
		 */
		public void setShipEventId( String s ) { shipEventId = s; }
		public String getShipEventId() { return shipEventId; }

		/**
		 * Sets an auto blueprint (or blueprintList) to spawn with the ShipEvent.
		 */
		public void setAutoBlueprintId( String s ) { autoBlueprintId = s; }
		public String getAutoBlueprintId() { return autoBlueprintId; }

		/**
		 * Sets a seed to randomly generate the enemy ship (layout, etc).
		 *
		 * When the player ship visits this beacon, the resulting encounter
		 * will use this seed. When no enemy ship is present, this is 0.
		 */
		public void setShipEventSeed( int n ) { shipEventSeed = n; }
		public int getShipEventSeed() { return shipEventSeed; }

		/**
		 * Sets fleet background sprites and possibly the beacon icon.
		 *
		 * When FTL moves the rebel fleet over a beacon, the beacon's
		 * fleet presence becomes REBEL, and if it was visited, a
		 * LONG_FLEET ShipEvent is set. Otherwise, one of the FLEET_*
		 * events will be triggered to spawn the LONG_FLEET upon arrival.
		 */
		public void setFleetPresence( FleetPresence fp ) { fleetPresence = fp; }
		public FleetPresence getFleetPresence() { return fleetPresence; }

		/**
		 * Sets whether this beacon is under attack by rebels (flashing red).
		 *
		 * If true, the next time the player jumps to a beacon, this one
		 * will have a REBEL fleet and possibly a LONG_FLEET ShipEvent,
		 * and will no longer be under attack.
		 */
		public void setUnderAttack( boolean b ) { underAttack = b; }
		public boolean isUnderAttack() { return underAttack; }

		/**
		 * Sets whether a store is present.
		 *
		 * If true, a store must be set.
		 */
		public void setStorePresent( boolean b ) { storePresent = b; }
		public boolean isStorePresent() { return storePresent; }

		public void setStore( StoreState storeState ) { store = storeState; }
		public StoreState getStore() { return store; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Visited:               %5b\n", visited));
			if ( visited ) {
				result.append(String.format("  Bkg Starscape:       %s\n", bgStarscapeImageInnerPath));
				result.append(String.format("  Bkg Sprite:          %s\n", bgSpriteImageInnerPath));
				result.append(String.format("  Bkg Sprite Position:   %3d,%3d\n", bgSpritePosX, bgSpritePosY));
				result.append(String.format("  Bkg Sprite Rotation:   %3d\n", bgSpriteRotation));
			}

			result.append(String.format("Seen:                  %5b\n", seen));

			result.append(String.format("Enemy Present:         %5b\n", enemyPresent));
			if ( enemyPresent ) {
				result.append(String.format("  Ship Event ID:       %s\n", shipEventId));
				result.append(String.format("  Auto Blueprint ID:   %s\n", autoBlueprintId));
				result.append(String.format("  Ship Event Seed:     %5d\n", shipEventSeed));
			}

			result.append(String.format("Fleets Present:        %s\n", fleetPresence));

			result.append(String.format("Under Attack:          %5b\n", underAttack));

			result.append(String.format("Store Present:         %5b\n", storePresent));
			if ( storePresent ) {
				result.append( store.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			return result.toString();
		}
	}



	public static class StoreState {
		private int fuel = 0, missiles = 0, droneParts = 0;
		private List<StoreShelf> shelfList = new ArrayList<StoreShelf>(3);

		// TODO: Remove all references to setTopShelf()/setBottomShelf().


		/**
		 * Constructs an incomplete StoreState.
		 *
		 * It will need two StoreShelf objects for FTL 1.01-1.03.3.
		 */
		public StoreState() {
		}

		public void setFuel( int n ) { fuel = n; }
		public void setMissiles( int n ) { missiles = n; }
		public void setDroneParts( int n ) { droneParts = n; }

		public int getFuel() { return fuel; }
		public int getMissiles() { return missiles; }
		public int getDroneParts() { return droneParts; }

		public void setShelfList( List<StoreShelf> shelfList ) { this.shelfList = shelfList; }
		public List<StoreShelf> getShelfList() { return shelfList; }

		public void addShelf( StoreShelf shelf ) { shelfList.add( shelf ); }
		public void setTopShelf( StoreShelf shelf ) { shelfList.set( 0, shelf ); }
		public void setBottomShelf( StoreShelf shelf ) { shelfList.set( 1, shelf ); }

		public StoreShelf getTopShelf() { return shelfList.get( 0 ); }
		public StoreShelf getBottomShelf() { return shelfList.get( 1 ); }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Fuel:        %2d\n", fuel));
			result.append(String.format("Missiles:    %2d\n", missiles));
			result.append(String.format("Drone Parts: %2d\n", droneParts));

			for ( int i=0; i < shelfList.size(); i++ ) {
				result.append(String.format("\nShelf %d...\n", i));
				result.append( shelfList.get(i).toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			return result.toString();
		}
	}

	public static enum StoreItemType {
		WEAPON("Weapon"), DRONE("Drone"), AUGMENT("Augment"),
		CREW("Crew"), SYSTEM("System");

		private String title;
		private StoreItemType( String title ) { this.title = title; }
		public String toString() { return title; }
	}

	public static class StoreShelf {
		private StoreItemType itemType = StoreItemType.WEAPON;
		private List<StoreItem> items = new ArrayList<StoreItem>(3);


		public StoreShelf() {
		}

		/**
		 * Copy constructor.
		 *
		 * Each StoreItem will be copy-constructed as well.
		 */
		public StoreShelf( StoreShelf srcShelf ) {
			setItemType( srcShelf.getItemType() );
			for ( StoreItem tmpItem : srcShelf.getItems() ) {
				addItem( new StoreItem( tmpItem ) );
			}
		}

		public void setItemType( StoreItemType type ) { itemType = type; }
		public StoreItemType getItemType() { return itemType; }

		public void addItem( StoreItem item ) {
			items.add( item );
		}
		public List<StoreItem> getItems() { return items; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append(String.format("Item Type: %s\n", itemType));
			for (StoreItem item : items) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append( item.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			return result.toString();
		}
	}

	public static class StoreItem {
		private boolean available;
		private String itemId;


		public StoreItem( boolean available, String itemId ) {
			this.available = available;
			this.itemId = itemId;
		}

		/**
		 * Copy constructor.
		 */
		public StoreItem( StoreItem srcItem ) {
			this( srcItem.isAvailable(), srcItem.getItemId() );
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



	public static class EncounterState {
		private String text = "";

		private int shipEventSeed = 0;
		private String unknownBeta = "";
		private String unknownGamma = "";
		private String unknownDelta = "";
		private String unknownEpsilon = "";
		private String unknownZeta = "";
		private String unknownEta = "";

		private int unknownTheta = 0;
		private int unknownIota = 0;
		private ArrayList<Integer> unknownKappa = new ArrayList<Integer>();


		public EncounterState() {
		}

		/**
		 * Sets a seed to randomly generate the enemy ship (layout, etc).
		 *
		 * When the player ship visits a beacon, the resulting encounter
		 * will use the beacon's enemy ship event seed.
		 */
		public void setShipEventSeed( int n ) { shipEventSeed = n; }
		public int getShipEventSeed() { return shipEventSeed; }

		public void setUnknownBeta( String s ) { unknownBeta = s; }
		public void setUnknownGamma( String s ) { unknownGamma = s; }
		public void setUnknownDelta( String s ) { unknownDelta = s; }
		public void setUnknownEpsilon( String s ) { unknownEpsilon = s; }
		public void setUnknownZeta( String s ) { unknownZeta = s; }
		public void setUnknownEta( String s ) { unknownEta = s; }

		public String getUnknownBeta() { return unknownBeta; }
		public String getUnknownGamma() { return unknownGamma; }
		public String getUnknownDelta() { return unknownDelta; }
		public String getUnknownEpsilon() { return unknownEpsilon; }
		public String getUnknownZeta() { return unknownZeta; }
		public String getUnknownEta() { return unknownEta; }

		public void setText( String s ) { text = s; }
		public String getText() { return text; }

		public void setUnknownTheta( int n ) { unknownTheta = n; }
		public void setUnknownIota( int n ) { unknownIota = n; }
		public void setUnknownKappa( ArrayList<Integer> kappaList ) { unknownKappa = kappaList; }

		public int getUnknownTheta() { return unknownTheta; }
		public int getUnknownIota() { return unknownIota; }
		public ArrayList<Integer> getUnknownKappa() { return unknownKappa; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Event Seed:  %3d\n", shipEventSeed));
			result.append(String.format("Beta?:       %s\n", unknownBeta));
			result.append(String.format("Gamma?:      %s\n", unknownGamma));
			result.append(String.format("Delta?:      %s\n", unknownDelta));
			result.append(String.format("Epsilon?:    %s\n", unknownEpsilon));
			result.append(String.format("Zeta?:       %s\n", unknownZeta));
			result.append(String.format("Eta?:        %s\n", unknownEta));

			result.append("\nText...\n");
			result.append(String.format("%s\n", text));
			result.append("\n");

			result.append(String.format("Theta?:      %3d\n", unknownTheta));
			result.append(String.format("Iota?:       %3d\n", unknownIota));

			result.append("\nKappa?...\n");
			first = true;
			for ( Integer kInt : unknownKappa ) {
				if ( first ) { first = false; }
				else { result.append(","); }
				result.append(kInt);
			}
			result.append("\n");

			return result.toString();
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
		 * The occupancy list is truncated to the new layout's rooms.
		 * (The blueprints happen to have matching low roomIds.)
		 *
		 *   Stage 1: 0x13=19 rooms
		 *   Stage 2: 0x0F=15 rooms
		 *   Stage 3: 0x0B=11 rooms
		 *   Having 0 rooms occupied is allowed, meaning AI took over.
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

			result.append(String.format("Pending Ship Type: %s\n", shipBlueprintIds[pendingStage-1]));

			result.append("\nOccupancy of Last Seen Type...\n");
			for (Map.Entry<Integer, Integer> entry : occupancyMap.entrySet()) {
				int roomId = entry.getKey().intValue();
				int occupantCount = entry.getValue().intValue();

				SystemType systemType = blueprintSystems.getSystemTypeByRoomId( roomId );
				String systemId = (systemType != null) ? systemType.getId() : "empty";

				result.append(String.format("RoomId: %2d (%-10s), Crew: %d\n", roomId, systemId, occupantCount));
			}

			return result.toString();
		}
	}



	public static interface ExtendedSystemInfo {
	}

	public static class ClonebayInfo implements ExtendedSystemInfo {
		private int buildTicks = 0;
		private int buildTicksGoal = 0;
		private int unknownGamma = 0;


		public ClonebayInfo() {
		}

		public void setBuildTicks( int n ) { buildTicks = n; }
		public int getBuildTicks() { return buildTicks; }

		public void setBuildTicksGoal( int n ) { buildTicksGoal = n; }
		public int getBuildTicksGoal() { return buildTicksGoal; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.CLONEBAY.toString()));
			result.append(String.format("Build Ticks:            %7d\n", buildTicks));
			result.append(String.format("Build Ticks Goal:       %7d\n", buildTicksGoal));
			result.append(String.format("Gamma?:                 %7d\n", unknownGamma));

			return result.toString();
		}
	}

	public static class BatteryInfo implements ExtendedSystemInfo {
		private boolean active = false;
		private int usedPower = 0;
		private int dischargeTicks = 0;

		// The battery is unaffected by plasma storms (<environment type="storm"/>).
		// Storms only halve *reserve* power.


		public BatteryInfo() {
		}

		/**
		 * Toggles whether the battery is turned on.
		 *
		 * @see #setDischargeTicks(int)
		 */
		public void setActive( boolean b ) { active = b; }
		public boolean isActive() { return active; }

		/**
		 * Sets the total battery power currently assigned to systems.
		 *
		 * This is subtracted from a pool based on the battery system's level
		 * to calculate remaining battery power.
		 */
		public void setUsedPower( int n ) { usedPower = n; }
		public int getUsedPower() { return usedPower; }

		/**
		 * Sets elapsed time while the battery is active.
		 *
		 * This counts to 1000. It's 1000 when not discharging.
		 * After it's fully discharged, the battery system will be locked for
		 * a bit.
		 *
		 * @param n 0-1000
		 *
		 * @see #setDischargeTicks(int)
		 */
		public void setDischargeTicks( int n ) { dischargeTicks = n; }
		public int getDischargeTicks() { return dischargeTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.BATTERY.toString()));
			result.append(String.format("Active:                   %5b\n", active));
			result.append(String.format("Battery Power in Use:     %5d\n", usedPower));
			result.append(String.format("Discharge Ticks:          %5d\n", dischargeTicks));

			return result.toString();
		}
	}

	public static class ShieldsInfo implements ExtendedSystemInfo {
		private int shieldLayers = 0;
		private int energyShieldLayers = 0;
		private int energyShieldMax = 0;
		private int shieldRechargeTicks = 0;

		private boolean shieldDropAnimOn = false;
		private int shieldDropAnimTicks = 0;

		private boolean shieldRaiseAnimOn = false;
		private int shieldRaiseAnimTicks = 0;

		private boolean energyShieldAnimOn = false;
		private int energyShieldAnimTicks = 0;

		private int unknownLambda = 0;
		private int unknownMu = 0;


		public ShieldsInfo() {
		}

		/**
		 * Sets the current number of normal shield layers.
		 *
		 * This is indicated in-game by filled bubbles.
		 */
		public void setShieldLayers( int n ) { shieldLayers = n; }
		public int getShieldLayers() { return shieldLayers; }

		/**
		 * Sets the current number of energy shield layers.
		 *
		 * This is indicated in-game by green rectangles.
		 */
		public void setEnergyShieldLayers( int n ) { energyShieldLayers = n; }
		public int getEnergyShieldLayers() { return energyShieldLayers; }

		/**
		 * Sets the number of energy shield layers when fully charged.
		 *
		 * This is 0 until set by a mechanism that adds energy layers. This
		 * value lingers after a temporary energy shield is exhausted.
		 */
		public void setEnergyShieldMax( int n ) { energyShieldMax = n; }
		public int getEnergyShieldMax() { return energyShieldMax; }

		/**
		 * Sets elapsed time while waiting for the next normal shield layer
		 * to recharge.
		 *
		 * This counts to 2000. It's 0 when not recharging.
		 */
		public void setShieldRechargeTicks( int n ) { shieldRechargeTicks = n; }
		public int getShieldRechargeTicks() { return shieldRechargeTicks; }


		/**
		 * Toggles whether the regular shield drop animation is being played.
		 *
		 * Note: The drop and raise anims can both play simultaneously.
		 */
		public void setShieldDropAnimOn( boolean b ) { shieldDropAnimOn = b; }
		public boolean isShieldDropAnimOn() { return shieldDropAnimOn; }

		/**
		 * Sets elapsed time while playing the regular shield drop anim.
		 *
		 * @param n 0-1000
		 */
		public void setShieldDropAnimTicks( int n ) { shieldDropAnimTicks = n; }
		public int getShieldDropAnimTicks() { return shieldDropAnimTicks; }


		/**
		 * Toggles whether the regular shield raise animation is being played.
		 *
		 * Note: The drop and raise anims can both play simultaneously.
		 */
		public void setShieldRaiseAnimOn( boolean b ) { shieldRaiseAnimOn = b; }
		public boolean isShieldRaiseAnimOn() { return shieldRaiseAnimOn; }

		/**
		 * Sets elapsed time while playing the regular shield raise anim.
		 *
		 * @param n 0-1000
		 */
		public void setShieldRaiseAnimTicks( int n ) { shieldRaiseAnimTicks = n; }
		public int getShieldRaiseAnimTicks() { return shieldRaiseAnimTicks; }


		/**
		 * Toggles whether the energy shield animation is being played.
		 */
		public void setEnergyShieldAnimOn( boolean b ) { energyShieldAnimOn = b; }
		public boolean isEnergyShieldAnimOn() { return energyShieldAnimOn; }

		/**
		 * Sets elapsed time while playing the energy shield anim.
		 *
		 * @param n 0-1000
		 */
		public void setEnergyShieldAnimTicks( int n ) { energyShieldAnimTicks = n; }
		public int getEnergyShieldAnimTicks() { return energyShieldAnimTicks; }


		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public void setUnknownMu( int n ) { unknownMu = n; }

		public int getUnknownLambda() { return unknownLambda; }
		public int getUnknownMu() { return unknownMu; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.SHIELDS.toString()));
			result.append(String.format("Shield Layers:            %5d (Currently filled bubbles)\n", shieldLayers));
			result.append(String.format("Energy Shield Layers:     %5d\n", energyShieldLayers));
			result.append(String.format("Energy Shield Max:        %5d (Layers when fully charged)\n", energyShieldLayers));
			result.append(String.format("Shield Recharge Ticks:    %5d\n", shieldRechargeTicks));
			result.append("\n");
			result.append(String.format("Shield Drop Anim:   Play: %-5b, Ticks: %4d\n", shieldDropAnimOn, shieldDropAnimTicks));
			result.append(String.format("Shield Raise Anim:  Play: %-5b, Ticks: %4d\n", shieldRaiseAnimOn, shieldRaiseAnimTicks));
			result.append(String.format("Energy Shield Anim: Play: %-5b, Ticks: %4d\n", energyShieldAnimOn, energyShieldAnimTicks));
			result.append(String.format("Lambda?, Mu?:           %7d,%7d (Some kind of coord, divide by 1000?)\n", unknownLambda, unknownMu));

			return result.toString();
		}
	}

	public static class CloakingInfo implements ExtendedSystemInfo {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int cloakTicksGoal = 0;
		private int cloakTicks = 0;


		public CloakingInfo() {
		}

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }

		/**
		 * Sets total time the cloak will stay engaged.
		 *
		 * This can vary depending on the system level when the cloak is
		 * initially engaged. When not engaged, this is 0.
		 *
		 * @see #setCloakTicks(int)
		 */
		public void setCloakTicksGoal( int n ) { cloakTicksGoal = n; }
		public int getCloakTicksGoal() { return cloakTicksGoal; }

		/**
		 * Sets elapsed time while the cloak is engaged.
		 *
		 * @param n a positive int less than, or equal to, the goal (MIN_INT when not engaged)
		 *
		 * @see #setCloakTicksGoal(int)
		 */
		public void setCloakTicks( int n ) { cloakTicks = n; }
		public int getCloakTicks() { return cloakTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.CLOAKING.toString()));
			result.append(String.format("Alpha?:                 %7d\n", unknownAlpha));
			result.append(String.format("Beta?:                  %7d\n", unknownBeta));
			result.append(String.format("Cloak Ticks Goal:       %7d\n", cloakTicksGoal));
			result.append(String.format("Cloak Ticks:            %7s\n", (cloakTicks==Integer.MIN_VALUE ? "MIN_INT" : cloakTicks) ));

			return result.toString();
		}
	}



	public static class UnknownEulabeia {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;

		// Apparently an X,Y pair, relative to the top-left of the nearby ship.
		// Used for placing targeting reticles over the middle of rooms.

		public UnknownEulabeia() {
		}

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Alpha?: %3d, Beta?: %3d\n", unknownAlpha, unknownBeta));

			return result.toString();
		}
	}

	// Extended info for individual weapons.
	// Game states contain two lists of these objects: player and nearby ship.
	public static class UnknownAthena {
		private int unknownAlpha = 0;    // Incrementing millisecs elapsed during cooldown.
		private int unknownBeta = 0;     // Goal cooldown time. Resembles WeaponBlueprint's value, and other factors?
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;
		private int unknownZeta = 0;

		private List<UnknownEulabeia> unknownEta = new ArrayList<UnknownEulabeia>();
		private List<UnknownEulabeia> unknownTheta = new ArrayList<UnknownEulabeia>();

		private int unknownIota = 0;     // Autofire?
		private int unknownKappa = 0;
		private int unknownLambda = 0;
		private int unknownMu = 0;
		private int unknownNu = 0;
		private int unknownXi = 0;
		private int unknownOmicron = 0;
		private int unknownPi = 0;       // E803 0000 (1000)
		private int unknownRho = 0;
		private int unknownSigma = 0;
		private int unknownTau = 0;      // E803 0000 (1000)
		private int unknownUpsilon = 0;
		private int unknownPhi = 0;
		private int unknownChi = 0;
		private int unknownPsi = 0;

		// There are two lists of Athena objects at the end of AE saves.

		public UnknownAthena() {
		}

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public void setUnknownZeta( int n ) { unknownZeta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }
		public int getUnknownGamma() { return unknownGamma; }
		public int getUnknownDelta() { return unknownDelta; }
		public int getUnknownEpsilon() { return unknownEpsilon; }
		public int getUnknownZeta() { return unknownZeta; }

		public void setUnknownEta( List<UnknownEulabeia> eulabeiaList ) { unknownEta = eulabeiaList; }
		public void setUnknownTheta( List<UnknownEulabeia> eulabeiaList ) { unknownTheta = eulabeiaList; }

		public List<UnknownEulabeia> getUnknownEta() { return unknownEta; }
		public List<UnknownEulabeia> getUnknownTheta() { return unknownTheta; }

		public void setUnknownIota( int n ) { unknownIota = n; }
		public void setUnknownKappa( int n ) { unknownKappa = n; }
		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public void setUnknownMu( int n ) { unknownMu = n; }
		public void setUnknownNu( int n ) { unknownNu = n; }
		public void setUnknownXi( int n ) { unknownXi = n; }
		public void setUnknownOmicron( int n ) { unknownOmicron = n; }
		public void setUnknownPi( int n ) { unknownPi = n; }
		public void setUnknownRho( int n ) { unknownRho = n; }
		public void setUnknownSigma( int n ) { unknownSigma = n; }
		public void setUnknownTau( int n ) { unknownTau = n; }
		public void setUnknownUpsilon( int n ) { unknownUpsilon = n; }
		public void setUnknownPhi( int n ) { unknownPhi = n; }
		public void setUnknownChi( int n ) { unknownChi = n; }
		public void setUnknownPsi( int n ) { unknownPsi = n; }

		public int getUnknownIota() { return unknownIota; }
		public int getUnknownKappa() { return unknownKappa; }
		public int getUnknownLambda() { return unknownLambda; }
		public int getUnknownMu() { return unknownMu; }
		public int getUnknownNu() { return unknownNu; }
		public int getUnknownXi() { return unknownXi; }
		public int getUnknownOmicron() { return unknownOmicron; }
		public int getUnknownPi() { return unknownPi; }
		public int getUnknownRho() { return unknownRho; }
		public int getUnknownSigma() { return unknownSigma; }
		public int getUnknownTau() { return unknownTau; }
		public int getUnknownUpsilon() { return unknownUpsilon; }
		public int getUnknownPhi() { return unknownPhi; }
		public int getUnknownChi() { return unknownChi; }
		public int getUnknownPsi() { return unknownPsi; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append(String.format("Alpha?:            %3d\n", unknownAlpha));
			result.append(String.format("Beta?:             %3d\n", unknownBeta));
			result.append(String.format("Gamma?:            %3d\n", unknownGamma));
			result.append(String.format("Delta?:            %3d\n", unknownDelta));
			result.append(String.format("Epsilon?:          %3d\n", unknownEpsilon));
			result.append(String.format("Zeta?:             %3d\n", unknownZeta));

			result.append("\nEta?...\n");
			first = true;
			for ( UnknownEulabeia x : unknownEta ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(x.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nTheta?...\n");
			first = true;
			for ( UnknownEulabeia x : unknownTheta ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(x.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append(String.format("Iota?:             %3d\n", unknownIota));
			result.append(String.format("Kappa?:            %3d\n", unknownKappa));
			result.append(String.format("Lambda?:           %3d\n", unknownLambda));
			result.append(String.format("Mu?:               %3d\n", unknownMu));
			result.append(String.format("Nu?:               %3d\n", unknownNu));
			result.append(String.format("Xi?:               %3d\n", unknownXi));
			result.append(String.format("Omicron?:          %3d\n", unknownOmicron));
			result.append(String.format("Pi?:               %3d\n", unknownPi));
			result.append(String.format("Rho?:              %3d\n", unknownRho));
			result.append(String.format("Sigma?:            %3d\n", unknownSigma));
			result.append(String.format("Tau?:              %3d\n", unknownTau));
			result.append(String.format("Upsilon?:          %3d\n", unknownUpsilon));
			result.append(String.format("Phi?:              %3d\n", unknownPhi));

			return result.toString();
		}
	}
}
