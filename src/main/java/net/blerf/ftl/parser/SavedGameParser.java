// Variables with unknown meanings are named with greek letters.
// Classes for unknown objects are named after deities.
// http://en.wikipedia.org/wiki/List_of_Greek_mythological_figures#Personified_concepts

// For reference on weapons and projectiles, see the "Complete Weapon Attribute Table":
// http://www.ftlgame.com/forum/viewtopic.php?f=12&t=24600


package net.blerf.ftl.parser;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.constants.OriginalFTLConstants;
import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.MysteryBytes;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameParser extends Parser {

	private static final Logger log = LogManager.getLogger(SavedGameParser.class);


	public SavedGameParser() {
	}

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

			int diffFlag = readInt(in);
			Difficulty diff;
			if ( diffFlag == 0 ) {
				diff = Difficulty.EASY;
			}
			else if ( diffFlag == 1 ) {
				diff = Difficulty.NORMAL;
			}
			else if ( diffFlag == 2 && headerAlpha == 7 ) {
				diff = Difficulty.HARD;
			}
			else {
				throw new IOException( String.format("Unsupported difficulty flag for saved game: %d", diffFlag) );
			}

			gameState.setDifficulty( diff );
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
			else if ( headerAlpha == 7 ) {
				// Current beaconId was set earlier.

				gameState.setUnknownMu( readInt(in) );

				EncounterState encounter = readEncounter(in);
				gameState.setEncounter( encounter );

				boolean shipNearby = readBool(in);
				if ( shipNearby ) {
					gameState.setRebelFlagshipNearby( readBool(in) );

					ShipState nearbyShipState = readShip( in, true, headerAlpha, gameState.isDLCEnabled() );
					gameState.setNearbyShipState(nearbyShipState);

					gameState.setNearbyShipAI( readNearbyShipAI(in) );
				}

				gameState.setEnvironment( readEnvironment(in) );

				// Flagship state is set much later.

				UnknownZeus zeus = readZeus( in, gameState );
				gameState.setUnknownZeus( zeus );
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

		int headerAlpha = gameState.getHeaderAlpha();

		if ( headerAlpha == 2 ) {
			// FTL 1.03.3 and earlier.
			writeInt( out, headerAlpha );
		}
		else if ( headerAlpha == 7 ) {
			// FTL 1.5.4+.
			writeInt( out, headerAlpha );
			writeBool( out, gameState.isDLCEnabled() );
		}
		else {
			throw new IOException( "Unsupported headerAlpha: "+ headerAlpha );
		}

		int diffFlag = 0;
		if ( gameState.getDifficulty() == Difficulty.EASY ) {
			diffFlag = 0;
		}
		else if ( gameState.getDifficulty() == Difficulty.NORMAL ) {
			diffFlag = 1;
		}
		else if ( gameState.getDifficulty() == Difficulty.HARD && headerAlpha == 7 ) {
			diffFlag = 2;
		}
		else {
			//throw new IOException( String.format("Unsupported difficulty for achievement (\"%s\"): %s", rec.getAchievementId(), rec.getDifficulty().toString()) );
			log.warn( String.format("Substituting EASY for unsupported difficulty for saved game: %s", gameState.getDifficulty().toString()) );
			diffFlag = 0;
		}
		writeInt( out, diffFlag );
		writeInt( out, gameState.getTotalShipsDefeated() );
		writeInt( out, gameState.getTotalBeaconsExplored() );
		writeInt( out, gameState.getTotalScrapCollected() );
		writeInt( out, gameState.getTotalCrewHired() );

		writeString( out, gameState.getPlayerShipName() );
		writeString( out, gameState.getPlayerShipBlueprintId() );

		// Redundant 1-based sector number.
		writeInt( out, gameState.getSectorNumber()+1 );

		writeInt( out, gameState.getUnknownBeta() );

		writeInt( out, gameState.getStateVars().size() );
		for (Map.Entry<String, Integer> entry : gameState.getStateVars().entrySet()) {
			writeString( out, entry.getKey() );
			writeInt( out, entry.getValue().intValue() );
		}

		writeShip( out, gameState.getPlayerShipState(), headerAlpha );

		writeInt( out, gameState.getCargoIdList().size() );
		for (String cargoItemId : gameState.getCargoIdList()) {
			writeString( out, cargoItemId );
		}

		writeInt( out, gameState.getSectorTreeSeed() );
		writeInt( out, gameState.getSectorLayoutSeed() );
		writeInt( out, gameState.getRebelFleetOffset() );
		writeInt( out, gameState.getRebelFleetFudge() );
		writeInt( out, gameState.getRebelPursuitMod() );

		if ( headerAlpha == 7 ) {
			writeInt( out, gameState.getCurrentBeaconId() );

			writeInt( out, gameState.getUnknownGamma() );
			writeInt( out, gameState.getUnknownDelta() );
			writeInt( out, gameState.getUnknownEpsilon() );
			writeBool( out, gameState.areSectorHazardsVisible() );
			writeBool( out, gameState.isRebelFlagshipVisible() );
			writeInt( out, gameState.getRebelFlagshipHop() );
			writeBool( out, gameState.isRebelFlagshipMoving() );
			writeInt( out, gameState.getUnknownKappa() );
			writeInt( out, gameState.getRebelFlagshipBaseTurns() );
		}
		else if ( headerAlpha == 2 ) {
			writeBool( out, gameState.areSectorHazardsVisible() );
			writeBool( out, gameState.isRebelFlagshipVisible() );
			writeInt( out, gameState.getRebelFlagshipHop() );
			writeBool( out, gameState.isRebelFlagshipMoving() );
		}

		writeInt( out, gameState.getSectorList().size() );
		for (Boolean visited : gameState.getSectorList()) {
			writeBool( out, visited.booleanValue() );
		}

		writeInt( out, gameState.getSectorNumber() );
		writeBool( out, gameState.isSectorHiddenCrystalWorlds() );

		writeInt( out, gameState.getBeaconList().size() );
		for (BeaconState beacon : gameState.getBeaconList()) {
			writeBeacon( out, beacon, headerAlpha );
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

		if ( headerAlpha == 2 ) {
			writeInt( out, gameState.getCurrentBeaconId() );

			ShipState nearbyShip = gameState.getNearbyShipState();
			writeBool( out, (nearbyShip != null) );
			if ( nearbyShip != null ) {
				writeShip( out, nearbyShip, headerAlpha );
			}

			writeRebelFlagship( out, gameState.getRebelFlagshipState() );
		}
		else if ( headerAlpha == 7 ) {
			// Current beaconId was set earlier.

			writeInt( out, gameState.getUnknownMu() );

			writeEncounter( out, gameState.getEncounter() );

			ShipState nearbyShip = gameState.getNearbyShipState();
			writeBool( out, (nearbyShip != null) );
			if ( nearbyShip != null ) {
				writeBool( out, gameState.isRebelFlagshipNearby() );

				writeShip( out, nearbyShip, headerAlpha );

				writeNearbyShipAI( out, gameState.getNearbyShipAI() );
			}

			writeEnvironment( out, gameState.getEnvironment() );

			// Flagship state is set much later.

			writeZeus( out, gameState, gameState.getUnknownZeus() );
		}
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
			// So this never occurred. TODO: There may also have been changes in 
			// 1.5.4 to allow multi-room systems on non-boss ships.

			ShipBlueprint.SystemList.SystemRoom[] rooms = shipBlueprint.getSystemList().getSystemRoom( systemType );
			if ( rooms != null && rooms.length > 1 ) {
				for ( int q=1; q < rooms.length; q++ ) {
					shipState.addSystem( readSystem(in, systemType, headerAlpha ) );
				}
			}
		}

		if ( headerAlpha == 7 ) {

			SystemState tmpSystem = null;

			tmpSystem = shipState.getSystem( SystemType.CLONEBAY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				ClonebayInfo clonebayInfo = new ClonebayInfo();

				clonebayInfo.setBuildTicks( readInt(in) );
				clonebayInfo.setBuildTicksGoal( readInt(in) );
				clonebayInfo.setDoomTicks( readInt(in) );

				shipState.addExtendedSystemInfo( clonebayInfo );
			}
			tmpSystem = shipState.getSystem( SystemType.BATTERY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				BatteryInfo batteryInfo = new BatteryInfo();

				batteryInfo.setActive( readBool(in) );
				batteryInfo.setUsedPower( readInt(in) );
				batteryInfo.setDischargeTicks( readInt(in) );

				shipState.addExtendedSystemInfo( batteryInfo );
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

				shipState.addExtendedSystemInfo( shieldsInfo );
			}

			tmpSystem = shipState.getSystem( SystemType.CLOAKING );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				CloakingInfo cloakingInfo = new CloakingInfo();

				cloakingInfo.setUnknownAlpha( readInt(in) );
				cloakingInfo.setUnknownBeta( readInt(in) );
				cloakingInfo.setCloakTicksGoal( readInt(in) );

				cloakingInfo.setCloakTicks( readMinMaxedInt(in) );  // May be MIN_VALUE.

				shipState.addExtendedSystemInfo( cloakingInfo );
			}
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

	public void writeShip( OutputStream out, ShipState shipState, int headerAlpha ) throws IOException {
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

		if ( headerAlpha == 7 ) {
			writeInt( out, shipState.getUnknownAlpha() );
			writeInt( out, shipState.getJumpTicks() );
			writeInt( out, shipState.getUnknownGamma() );
			writeInt( out, shipState.getUnknownDelta() );
		}

		writeInt( out, shipState.getHullAmt() );
		writeInt( out, shipState.getFuelAmt() );
		writeInt( out, shipState.getDronePartsAmt() );
		writeInt( out, shipState.getMissilesAmt() );
		writeInt( out, shipState.getScrapAmt() );

		writeInt( out, shipState.getCrewList().size() );
		for (CrewState crew : shipState.getCrewList()) {
			writeCrewMember( out, crew, headerAlpha );
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

		writeInt( out, shipState.getReservePowerCapacity() );

		for ( SystemType systemType : systemTypes ) {
			List<SystemState> systemList = shipState.getSystems( systemType );
			if ( systemList.size() > 0 ) {
				for ( SystemState systemState : systemList ) {
					writeSystem( out, systemState, headerAlpha );
				}
			}
			else {
				writeInt( out, 0 );  // Equivalent to constructing and writing a 0-capacity system.
			}
		}

		if ( headerAlpha == 7 ) {

			List<ClonebayInfo> clonebayInfoList = shipState.getExtendedSystemInfoList( ClonebayInfo.class );
			for ( ClonebayInfo clonebayInfo : clonebayInfoList ) {
				writeInt( out, clonebayInfo.getBuildTicks() );
				writeInt( out, clonebayInfo.getBuildTicksGoal() );
				writeInt( out, clonebayInfo.getDoomTicks() );
			}
			List<BatteryInfo> batteryInfoList = shipState.getExtendedSystemInfoList( BatteryInfo.class );
			for ( BatteryInfo batteryInfo : batteryInfoList ) {
				writeBool( out, batteryInfo.isActive() );
				writeInt( out, batteryInfo.getUsedPower() );
				writeInt( out, batteryInfo.getDischargeTicks() );
			}

			List<ShieldsInfo> shieldsInfoList = shipState.getExtendedSystemInfoList( ShieldsInfo.class );
			for ( ShieldsInfo shieldsInfo : shieldsInfoList ) {
				writeInt( out, shieldsInfo.getShieldLayers() );
				writeInt( out, shieldsInfo.getEnergyShieldLayers() );
				writeInt( out, shieldsInfo.getEnergyShieldMax() );
				writeInt( out, shieldsInfo.getShieldRechargeTicks() );

				writeBool( out, shieldsInfo.isShieldDropAnimOn() );
				writeInt( out, shieldsInfo.getShieldDropAnimTicks() );

				writeBool( out, shieldsInfo.isShieldRaiseAnimOn() );
				writeInt( out, shieldsInfo.getShieldRaiseAnimTicks() );

				writeBool( out, shieldsInfo.isEnergyShieldAnimOn() );
				writeInt( out, shieldsInfo.getEnergyShieldAnimTicks() );

				writeInt( out, shieldsInfo.getUnknownLambda() );
				writeInt( out, shieldsInfo.getUnknownMu() );
			}

			List<CloakingInfo> cloakingInfoList = shipState.getExtendedSystemInfoList( CloakingInfo.class );
			for ( CloakingInfo cloakingInfo : cloakingInfoList ) {
				writeInt( out, cloakingInfo.getUnknownAlpha() );
				writeInt( out, cloakingInfo.getUnknownBeta() );
				writeInt( out, cloakingInfo.getCloakTicksGoal() );

				writeMinMaxedInt( out, cloakingInfo.getCloakTicks() );  // May be MIN_VALUE.
			}
    }

		for (RoomState room : shipState.getRoomList()) {
			writeRoom( out, room, headerAlpha );
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
			writeDoor( out, shipDoorMap.get( doorCoord ), headerAlpha );
		}
		for (Map.Entry<ShipLayout.DoorCoordinate, EnumMap<ShipLayout.DoorInfo,Integer>> entry : vacuumDoorMap.entrySet()) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();

			writeDoor( out, shipDoorMap.get( doorCoord ), headerAlpha );
		}

		if ( headerAlpha == 7 ) {
			writeInt( out, shipState.getUnknownPhi() );
		}

		writeInt( out, shipState.getWeaponList().size() );
		for (WeaponState weapon : shipState.getWeaponList()) {
			writeString( out, weapon.getWeaponId() );
			writeBool( out, weapon.isArmed() );

			if ( headerAlpha == 2 ) {  // No longer used as of FTL 1.5.4.
				writeInt( out, weapon.getCooldownTicks() );
			}
		}

		writeInt( out, shipState.getDroneList().size() );
		for ( DroneState drone : shipState.getDroneList() ) {
			writeDrone( out, drone );
		}

		writeInt( out, shipState.getAugmentIdList().size() );
		for ( String augmentId : shipState.getAugmentIdList() ) {
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
			crew.setHealthBoost( readInt(in) );
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
				crew.setLockdownRechargeTicksGoal( readInt(in) );
				crew.setUnknownOmega( readInt(in) );
			}
		}

		return crew;
	}

	public void writeCrewMember( OutputStream out, CrewState crew, int headerAlpha ) throws IOException {
		writeString( out, crew.getName() );
		writeString( out, crew.getRace() );
		writeBool( out, crew.isEnemyBoardingDrone() );
		writeInt( out, crew.getHealth() );
		writeInt( out, crew.getSpriteX() );
		writeInt( out, crew.getSpriteY() );
		writeInt( out, crew.getRoomId() );
		writeInt( out, crew.getRoomSquare() );
		writeBool( out, crew.isPlayerControlled() );

		if ( headerAlpha == 7 ) {
			writeInt( out, crew.getUnknownAlpha() );
			writeInt( out, crew.getUnknownBeta() );

			writeInt( out, crew.getSpriteTintIndeces().size() );
			for ( Integer tintInt : crew.getSpriteTintIndeces() ) {
				writeInt( out, tintInt.intValue() );
			}

			writeBool( out, crew.isMindControlled() );
			writeInt( out, crew.getSavedRoomSquare() );
			writeInt( out, crew.getSavedRoomId() );
		}

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

		if ( headerAlpha == 7 ) {
			writeInt( out, crew.getStunTicks() );
			writeInt( out, crew.getHealthBoost() );
			writeInt( out, crew.getUnknownIota() );
			writeInt( out, crew.getUnknownKappa() );
			writeInt( out, crew.getUnknownLambda() );
			writeInt( out, crew.getUnknownMu() );
			writeInt( out, crew.getUnknownNu() );
			writeInt( out, crew.getUnknownXi() );
			writeInt( out, crew.getUnknownOmicron() );
			writeInt( out, crew.getUnknownPi() );
			writeInt( out, crew.getUnknownRho() );
			writeInt( out, crew.getUnknownSigma() );
			writeInt( out, crew.getUnknownTau() );
			writeInt( out, crew.getUnknownUpsilon() );
			writeInt( out, crew.getUnknownPhi() );

			if ( "crystal".equals(crew.getRace()) ) {
				writeInt( out, crew.getLockdownRechargeTicks() );
				writeInt( out, crew.getLockdownRechargeTicksGoal() );
				writeInt( out, crew.getUnknownOmega() );
			}
		}
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

			system.setDeionizationTicks( readMinMaxedInt(in) );  // May be MIN_VALUE.

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

	public void writeSystem( OutputStream out, SystemState system, int headerAlpha ) throws IOException {
		writeInt( out, system.getCapacity() );
		if ( system.getCapacity() > 0 ) {
			writeInt( out, system.getPower() );
			writeInt( out, system.getDamagedBars() );
			writeInt( out, system.getIonizedBars() );

			writeMinMaxedInt( out, system.getDeionizationTicks() );  // May be MIN_VALUE.

			writeInt( out, system.getRepairProgress() );
			writeInt( out, system.getDamageProgress() );

			if ( headerAlpha == 7 ) {
				writeInt( out, system.getBatteryPower() );
				writeInt( out, system.getHackLevel() );
				writeBool( out, system.isHacked() );
				writeInt( out, system.getTemporaryCapacityCap() );
				writeInt( out, system.getTemporaryCapacityLoss() );
				writeInt( out, system.getTemporaryCapacityDivisor() );
			}
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

			StationDirection stationDirection = null;
			int stationDirectionFlag = readInt(in);

			if ( stationDirectionFlag == 0 ) {
				stationDirection = StationDirection.DOWN;
			}
			else if ( stationDirectionFlag == 1 ) {
				stationDirection = StationDirection.RIGHT;
			}
			else if ( stationDirectionFlag == 2 ) {
				stationDirection = StationDirection.UP;
			}
			else if ( stationDirectionFlag == 3 ) {
				stationDirection = StationDirection.LEFT;
			}
			else if ( stationDirectionFlag == 4 ) {
				stationDirection = StationDirection.NONE;
			}
			else {
				throw new IOException( "Unsupported room station direction flag: "+ stationDirection );
			}
			room.setStationDirection( stationDirection );
		}

		return room;
	}

	public void writeRoom( OutputStream out, RoomState room, int headerAlpha ) throws IOException {
		writeInt( out, room.getOxygen() );

		for (SquareState square : room.getSquareList()) {
			writeInt( out, square.getFireHealth() );
			writeInt( out, square.getIgnitionProgress() );
			writeInt( out, square.getUnknownGamma() );
		}

		if ( headerAlpha == 7 ) {
			writeInt( out, room.getStationSquare() );

			int stationDirectionFlag = 0;
			if ( room.getStationDirection() == StationDirection.DOWN ) {
				stationDirectionFlag = 0;
			}
			else if ( room.getStationDirection() == StationDirection.RIGHT ) {
				stationDirectionFlag = 1;
			}
			else if ( room.getStationDirection() == StationDirection.UP ) {
				stationDirectionFlag = 2;
			}
			else if ( room.getStationDirection() == StationDirection.LEFT ) {
				stationDirectionFlag = 3;
			}
			else if ( room.getStationDirection() == StationDirection.NONE ) {
				stationDirectionFlag = 4;
			}
			else {
				throw new IOException( "Unsupported room station direction: "+ room.getStationDirection().toString() );
			}
			writeInt( out, stationDirectionFlag );
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

	public void writeDoor( OutputStream out, DoorState door, int headerAlpha ) throws IOException {
		if ( headerAlpha == 7 ) {
			writeInt( out, door.getCurrentMaxHealth() );
			writeInt( out, door.getHealth() );
			writeInt( out, door.getNominalHealth() );
		}

		writeBool( out, door.isOpen() );
		writeBool( out, door.isWalkingThrough() );

		if ( headerAlpha == 7 ) {
			writeInt( out, door.getUnknownDelta() );
			writeInt( out, door.getUnknownEpsilon() );
		}
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

		// TODO: Visited looked like a bool until 1.5.4; the base beacon has "2" when visited.
		int visited = readInt(in);
		beacon.setVisited( visited );
		if ( visited > 0 ) {
			beacon.setBgStarscapeImageInnerPath( readString(in) );
			beacon.setBgSpriteImageInnerPath( readString(in) );
			beacon.setBgSpritePosX( readInt(in) );
			beacon.setBgSpritePosY( readInt(in) );
			beacon.setBgSpriteRotation( readInt(in) );
		}
		
		beacon.setSeen( readBool(in) );
		
		boolean enemyPresent = readBool(in);
		beacon.setEnemyPresent( enemyPresent );
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
		if ( storePresent ) {
			StoreState store = new StoreState();

			int shelfCount = 2;          // FTL 1.01-1.03.3 only had two shelves.
			if ( headerAlpha == 7 ) {    // FTL 1.5.4 made shelves into an N-sized list.
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

	public void writeBeacon( OutputStream out, BeaconState beacon, int headerAlpha ) throws IOException {
		// FTL 1.01-1.03.3 might only allow visited to be 0 or 1.
		writeInt( out, beacon.getVisited() );
		if ( beacon.getVisited() > 0 ) {
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

		boolean storePresent = ( beacon.getStore() != null );
		writeBool( out, storePresent );

		if ( storePresent ) {
			StoreState store = beacon.getStore();

			if ( headerAlpha == 2 ) {
				// FTL 1.01-1.03.3 always had two shelves.

				int shelfLimit = 2;
				int shelfCount = Math.min( store.getShelfList().size(), shelfLimit );
				for (int i=0; i < shelfCount; i++) {
					writeStoreShelf( out, store.getShelfList().get( i ) );
				}
				for (int i=0; i < shelfLimit - shelfCount; i++) {
					StoreShelf dummyShelf = new StoreShelf();
					writeStoreShelf( out, dummyShelf );
				}
			}
			else if ( headerAlpha == 7 ) {
				// FTL 1.5.4 requires at least one shelf.
				int shelfReq = 1;

				List<StoreShelf> pendingShelves = new ArrayList<StoreShelf>();
				pendingShelves.addAll( store.getShelfList() );

				while ( pendingShelves.size() < shelfReq ) {
					StoreShelf dummyShelf = new StoreShelf();
					pendingShelves.add( dummyShelf );
				}

				writeInt( out, pendingShelves.size() );

				for ( StoreShelf shelf : pendingShelves ) {
					writeStoreShelf( out, shelf );
				}
			}

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
		encounter.setSurrenderEventId( readString(in) );
		encounter.setEscapeEventId( readString(in) );
		encounter.setDestroyedEventId( readString(in) );
		encounter.setDeadCrewEventId( readString(in) );
		encounter.setGotAwayEventId( readString(in) );

		encounter.setLastEventId( readString(in) );
		encounter.setText( readString(in) );
		encounter.setAffectedCrewSeed( readInt(in) );

		int choiceCount = readInt(in);
		ArrayList<Integer> choiceList = new ArrayList<Integer>();
		for (int i=0; i < choiceCount; i++) {
			choiceList.add( new Integer(readInt(in)) );
		}
		encounter.setChoiceList( choiceList );

		return encounter;
	}

	public void writeEncounter( OutputStream out, EncounterState encounter ) throws IOException {
		writeInt( out, encounter.getShipEventSeed() );
		writeString( out, encounter.getSurrenderEventId() );
		writeString( out, encounter.getEscapeEventId() );
		writeString( out, encounter.getDestroyedEventId() );
		writeString( out, encounter.getDeadCrewEventId() );
		writeString( out, encounter.getGotAwayEventId() );

		writeString( out, encounter.getLastEventId() );
		writeString( out, encounter.getText() );
		writeInt( out, encounter.getAffectedCrewSeed() );

		writeInt( out, encounter.getChoiceList().size() );
		for ( Integer choiceInt : encounter.getChoiceList() ) {
			writeInt( out, choiceInt.intValue() );
		}
	}

	private NearbyShipAIState readNearbyShipAI( FileInputStream in ) throws IOException {
System.err.println(String.format("Ship AI: @%d", in.getChannel().position()));
		NearbyShipAIState ai = new NearbyShipAIState();

		ai.setSurrendered( readBool(in) );
		ai.setEscaping( readBool(in) );
		ai.setDestroyed( readBool(in) );
		ai.setSurrenderThreshold( readInt(in) );
		ai.setEscapeThreshold( readInt(in) );
		ai.setEscapeTicks( readInt(in) );
		ai.setStalemateTriggered( readBool(in) );
		ai.setStalemateTicks( readInt(in) );
		ai.setBoardingAttempts( readInt(in) );
		ai.setBoardersNeeded( readInt(in) );

		return ai;
	}

	public void writeNearbyShipAI( OutputStream out, NearbyShipAIState ai ) throws IOException {
		writeBool( out, ai.hasSurrendered() );
		writeBool( out, ai.isEscaping() );
		writeBool( out, ai.isDestroyed() );
		writeInt( out, ai.getSurrenderThreshold() );
		writeInt( out, ai.getEscapeThreshold() );
		writeInt( out, ai.getEscapeTicks() );
		writeBool( out, ai.isStalemateTriggered() );
		writeInt( out, ai.getStalemateTicks() );
		writeInt( out, ai.getBoardingAttempts() );
		writeInt( out, ai.getBoardersNeeded() );
	}

	private EnvironmentState readEnvironment( FileInputStream in ) throws IOException {
System.err.println(String.format("Environment: @%d", in.getChannel().position()));
		EnvironmentState env = new EnvironmentState();

		env.setRedGiantLevel( readInt(in) );
		env.setPulsarLevel( readInt(in) );
		env.setPDSLevel( readInt(in) );

		int vulnFlag = readInt(in);
		HazardVulnerability vuln = null;
		if ( vulnFlag == 0 ) {
			vuln = HazardVulnerability.PLAYER_SHIP;
		}
		else if ( vulnFlag == 1 ) {
			vuln = HazardVulnerability.NEARBY_SHIP;
		}
		else if ( vulnFlag == 2 ) {
			vuln = HazardVulnerability.BOTH_SHIPS;
		}
		else {
			throw new IOException( String.format("Unsupported environment vulnerability flag: %d", vulnFlag) );
		}
		env.setVulnerableShips( vuln );

		boolean asteroidsPresent = readBool(in);
		if ( asteroidsPresent ) {
			AsteroidFieldState asteroidField = new AsteroidFieldState();
			asteroidField.setUnknownAlpha( readInt(in) );
			asteroidField.setStrayRockTicks( readInt(in) );
			asteroidField.setUnknownGamma( readInt(in) );
			asteroidField.setBgDriftTicks( readInt(in) );
			asteroidField.setCurrentTarget( readInt(in) );
			env.setAsteroidField( asteroidField );
		}
		env.setSolarFlareFadeTicks( readInt(in) );
		env.setHavocTicks( readInt(in) );
		env.setPDSTicks( readInt(in) );

		return env;
	}

	public void writeEnvironment( OutputStream out, EnvironmentState env ) throws IOException {
		writeInt( out, env.getRedGiantLevel() );
		writeInt( out, env.getPulsarLevel() );
		writeInt( out, env.getPDSLevel() );

		int vulnFlag = 0;
		if ( env.getVulnerableShips() == HazardVulnerability.PLAYER_SHIP ) {
			vulnFlag = 0;
		}
		else if ( env.getVulnerableShips() == HazardVulnerability.NEARBY_SHIP ) {
			vulnFlag = 1;
		}
		else if ( env.getVulnerableShips() == HazardVulnerability.BOTH_SHIPS ) {
			vulnFlag = 2;
		}
		else {
			throw new IOException( String.format("Unsupported environment vulnerability: %s", env.getVulnerableShips().toString()) );
		}
		writeInt( out, vulnFlag );

		boolean asteroidsPresent = ( env.getAsteroidField() != null );
		writeBool( out, asteroidsPresent );
		if ( asteroidsPresent ) {
			AsteroidFieldState asteroidField = env.getAsteroidField();
			writeInt( out, asteroidField.getUnknownAlpha() );
			writeInt( out, asteroidField.getStrayRockTicks() );
			writeInt( out, asteroidField.getUnknownGamma() );
			writeInt( out, asteroidField.getBgDriftTicks() );
			writeInt( out, asteroidField.getCurrentTarget() );
		}

		writeInt( out, env.getSolarFlareFadeTicks() );
		writeInt( out, env.getHavocTicks() );
		writeInt( out, env.getPDSTicks() );
	}

	public RebelFlagshipState readRebelFlagship( InputStream in ) throws IOException {
		RebelFlagshipState flagship = new RebelFlagshipState();

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

	private ProjectileState readProjectile( FileInputStream in ) throws IOException {
System.err.println(String.format("Projectile: @%d", in.getChannel().position()));
		ProjectileState projectile = new ProjectileState();

		projectile.setProjectileType( readInt(in) );

		if ( projectile.getProjectileType() == 0 ) {  // ProjectileType=0 is a null shot!?
			return projectile;
		}

		projectile.setCurrentPositionX( readInt(in) );
		projectile.setCurrentPositionY( readInt(in) );
		projectile.setPreviousPositionX( readInt(in) );
		projectile.setPreviousPositionY( readInt(in) );
		projectile.setSpeed( readInt(in) );
		projectile.setGoalPositionX( readInt(in) );
		projectile.setGoalPositionY( readInt(in) );
		projectile.setHeading( readInt(in) );
		projectile.setOwnerId( readInt(in) );
		projectile.setSelfId( readInt(in) );

		projectile.setDamage( readDamage(in) );

		projectile.setLifespan( readInt(in) );
		projectile.setDestinationSpace( readInt(in) );
		projectile.setCurrentSpace( readInt(in) );
		projectile.setTargetId( readInt(in) );
		projectile.setDead( readBool(in) );
		projectile.setDeathAnimId( readString(in) );
		projectile.setFlightAnimId( readString(in) );

		for (int i=0; i < 7; i++) {
			projectile.setDeathAnimState( i, readInt(in) );
		}
		for (int i=0; i < 7; i++) {
			projectile.setFlightAnimState( i, readInt(in) );
		}

		projectile.setSpeedX( readInt(in) );
		projectile.setSpeedY( readInt(in) );
		projectile.setMissed( readBool(in) );
		projectile.setHitTarget( readBool(in) );
		projectile.setHitSolidSound( readString(in) );
		projectile.setHitShieldSound( readString(in) );
		projectile.setMissSound( readString(in) );
		projectile.setEntryAngle( readMinMaxedInt(in) );
		projectile.setStartedDying( readBool(in) );
		projectile.setPassedTarget( readBool(in) );

		projectile.setType( readInt(in) );
		projectile.setBroadcastTarget( readBool(in) );

		List<Integer> alphaList = new ArrayList<Integer>();  // TODO: Awful kludge!
		if ( projectile.getProjectileType() == 1 ) {
			if ( projectile.getType() == 2 ) {                   // Flak
				for (int i=0; i < 2; i++) {
					alphaList.add( new Integer(readMinMaxedInt(in)) );
				}
			}
			else if ( projectile.getType() == 4 ) {              // Ion
				for (int i=0; i < 2; i++) {
					alphaList.add( new Integer(readMinMaxedInt(in)) );
				}
			}
			else if ( projectile.getType() == 5 ) {              // Beam
				for (int i=0; i < 25; i++) {
					alphaList.add( new Integer(readMinMaxedInt(in)) );
				}
			}
		}
		else if ( projectile.getProjectileType() == 2 ) {      // Explosion?
			if ( projectile.getType() == 2 ) {
				// No-op.
			}
		}
		else if ( projectile.getProjectileType() == 3 ) {      // Missile?
			if ( projectile.getType() == 1 ) {
				// No-op.
			}
		}
		else if ( projectile.getProjectileType() == 4 ) {      // Bomb?
			if ( projectile.getType() == 5 ) {
				for (int i=0; i < 5; i++) {
					alphaList.add( new Integer(readMinMaxedInt(in)) );
				}
			}
		}
		else if ( projectile.getProjectileType() == 5 ) {
			if ( projectile.getType() == 5 ) {                   // Beam
				for (int i=0; i < 25; i++) {
					alphaList.add( new Integer(readMinMaxedInt(in)) );
				}
			}
		}
		projectile.setUnknownAlpha( alphaList );

		return projectile;
	}

	public void writeProjectile( OutputStream out, ProjectileState projectile ) throws IOException {
		writeInt( out, projectile.getProjectileType() );

		if ( projectile.getProjectileType() == 0 ) {  // ProjectileType=0 is a null shot!?
			return;
		}

		writeInt( out, projectile.getCurrentPositionX() );
		writeInt( out, projectile.getCurrentPositionY() );
		writeInt( out, projectile.getPreviousPositionX() );
		writeInt( out, projectile.getPreviousPositionY() );
		writeInt( out, projectile.getSpeed() );
		writeInt( out, projectile.getGoalPositionX() );
		writeInt( out, projectile.getGoalPositionY() );
		writeInt( out, projectile.getHeading() );
		writeInt( out, projectile.getOwnerId() );
		writeInt( out, projectile.getSelfId() );

		writeDamage( out, projectile.getDamage() );

		writeInt( out, projectile.getLifespan() );
		writeInt( out, projectile.getDestinationSpace() );
		writeInt( out, projectile.getCurrentSpace() );
		writeInt( out, projectile.getTargetId() );
		writeBool( out, projectile.isDead() );
		writeString( out, projectile.getDeathAnimId() );
		writeString( out, projectile.getFlightAnimId() );

		for ( int n : projectile.getDeathAnimState() ) {
			writeInt( out, n );
		}

		for ( int n : projectile.getFlightAnimState() ) {
			writeInt( out, n );
		}

		writeInt( out, projectile.getSpeedX() );
		writeInt( out, projectile.getSpeedY() );
		writeBool( out, projectile.hasMissed() );
		writeBool( out, projectile.hasHitTarget() );
		writeString( out, projectile.getHitSolidSound() );
		writeString( out, projectile.getHitShieldSound() );
		writeString( out, projectile.getMissSound() );
		writeMinMaxedInt( out, projectile.getEntryAngle() );
		writeBool( out, projectile.hasStartedDying() );
		writeBool( out, projectile.hasPassedTarget() );

		writeInt( out, projectile.getType() );
		writeBool( out, projectile.getBroadcastTarget() );

		for ( Integer alphaInt : projectile.getUnknownAlpha() ) {
			writeMinMaxedInt( out, alphaInt.intValue() );
		}
	}

	public DamageState readDamage( InputStream in ) throws IOException {
		DamageState damage = new DamageState();

		damage.setHullDamage( readInt(in) );
		damage.setShieldPiercing( readInt(in) );
		damage.setFireChance( readInt(in) );
		damage.setBreachChance( readInt(in) );
		damage.setIonDamage( readInt(in) );
		damage.setSystemDamage( readInt(in) );
		damage.setPersonnelDamage( readInt(in) );
		damage.setHullBuster( readBool(in) );
		damage.setOwnerId( readInt(in) );
		damage.setSelfId( readInt(in) );
		damage.setLockdown( readBool(in) );
		damage.setCrystalShard( readBool(in) );
		damage.setStunChance( readInt(in) );
		damage.setStunAmount( readInt(in) );

		return damage;
	}

	public void writeDamage( OutputStream out, DamageState damage ) throws IOException {
		writeInt( out, damage.getHullDamage() );
		writeInt( out, damage.getShieldPiercing() );
		writeInt( out, damage.getFireChance() );
		writeInt( out, damage.getBreachChance() );
		writeInt( out, damage.getIonDamage() );
		writeInt( out, damage.getSystemDamage() );
		writeInt( out, damage.getPersonnelDamage() );
		writeBool( out, damage.isHullBuster() );
		writeInt( out, damage.getOwnerId() );
		writeInt( out, damage.getSelfId() );
		writeBool( out, damage.isLockdown() );
		writeBool( out, damage.isCrystalShard() );
		writeInt( out, damage.getStunChance() );
		writeInt( out, damage.getStunAmount() );
	}



	public static enum StateVar {
		// TODO: Magic strings.
		BLUE_ALIEN     ("blue_alien",      "Blue event choices clicked. (only ones that require a race)"),
		DEAD_CREW      ("dead_crew",       "???, plus boarding drone bodies? (see also: lost_crew)"),
		DESTROYED_ROCK ("destroyed_rock",  "Rock ships destroyed. (including pirates)"),
		ENV_DANGER     ("env_danger",      "Jumps into beacons with environmental dangers."),
		FIRED_SHOT     ("fired_shot",      "Individual beams/blasts/projectiles fired. (see also: used_missile)"),
		HIGH_O2        ("higho2",          "Times oxygen exceeded 20%, incremented when arriving at a beacon (Bug: Or loading in FTL 1.5.4-1.5.10)."),
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
		private Difficulty difficulty = Difficulty.EASY;
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
		private boolean rebelFlagshipNearby = false;
		private ShipState nearbyShipState = null;
		private NearbyShipAIState nearbyShipAI = null;
		private EnvironmentState environment = null;
		private int currentBeaconId = 0;
		private RebelFlagshipState rebelFlagshipState = null;
		private ArrayList<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();

		private int unknownBeta = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;
		private int unknownKappa = 0;

		private int unknownMu = 0;


		private UnknownZeus unknownZeus = null;


		public SavedGameState() {
		}

		/**
		 * Sets the difficulty.
		 *
		 * 0 = Easy
		 * 1 = Normal
		 * 2 = Hard   (FTL 1.5.4+)
		 */
		public void setDifficulty( Difficulty d ) { difficulty = d; }
		public Difficulty getDifficulty() { return difficulty; }

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
		 * Note: Bad things may happen if you change the value
		 * from true to false, if this saved game depends on
		 * AE resources.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setDLCEnabled( boolean b ) { dlcEnabled = b; }
		public boolean isDLCEnabled() { return dlcEnabled; }

		/**
		 * Sets a state var.
		 *
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
		 *
		 * This is always a negative value that, when added to
		 * rebelFleetFudge, equals how far in from the right the
		 * warning circle has encroached (image has ~50px margin).
		 *
		 * Most sectors start with large negative value to keep
		 * this off-screen and increment toward 0 from there.
		 *
		 * In FTL 1.01-1.03.3, The Last Stand sector used a constant -25 and
		 * moderate rebelFleetFudge value to cover the map. In other sectors,
		 * This was always observed in multiples of 25.
		 *
		 * Note: After loading a saved game from FTL 1.03.3 into FTL 1.5.4,
		 * this value was observed going from -250 to -459. The fudge was
		 * unchanged. The significance of this is unknown.
		 *
		 * @param n pixels from the map's right edge
		 *          (see 'img/map/map_warningcircle_point.png', 650px wide)
		 */
		public void setRebelFleetOffset( int n ) { rebelFleetOffset = n; }
		public int getRebelFleetOffset() { return rebelFleetOffset; }

		/**
		 * This is always a positive number around 75-310 that,
		 * when added to rebelFleetOffset, equals how far in
		 * from the right the warning circle has encroached.
		 *
		 * This varies seemingly randomly from game to game and
		 * sector to sector, but it's consistent while within
		 * each sector. Except in The Last Stand, in which it is
		 * always 200 (the warning circle will extend beyond
		 * the righthand edge of the map, covering everything).
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
		 * Toggles the flagship on the map.
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
		 * Saved games only contain a linear set of boolean flags to
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
		 * Sets fields related to AI that controls the nearby ship.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setNearbyShipAI( NearbyShipAIState ai ) { nearbyShipAI = ai; }
		public NearbyShipAIState getNearbyShipAI() { return nearbyShipAI; }

		/**
		 * Sets fields related to a hostile environment at a beacon.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setEnvironment( EnvironmentState env ) { environment = env; }
		public EnvironmentState getEnvironment() { return environment; }

		/**
		 * Sets info about the next encounter with the rebel flagship.
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

		/**
		 * Toggles whether the nearby ship is the rebel flagship.
		 *
		 * Saved games omit this value when a nearby ship is not present.
		 *
		 * TODO: Document what happens when set to true.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setRebelFlagshipNearby( boolean b ) { rebelFlagshipNearby = b; }
		public boolean isRebelFlagshipNearby() { return rebelFlagshipNearby; }


		public void setUnknownZeus( UnknownZeus zeus ) { unknownZeus = zeus; }
		public UnknownZeus getUnknownZeus() { return unknownZeus; }


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

			boolean first = true;
			result.append(String.format("File Format:            %5d (%s)\n", unknownHeaderAlpha, formatDesc));
			result.append(String.format("AE Content:             %5b\n", (dlcEnabled ? "Enabled" : "Disabled") ));
			result.append(String.format("Ship Name:              %s\n", playerShipName));
			result.append(String.format("Ship Type:              %s\n", playerShipBlueprintId));
			result.append(String.format("Difficulty:             %s\n", difficulty.toString() ));
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

			result.append("\n");
			result.append(String.format("Flagship Nearby:    %5b (Only set when a nearby ship is present)\n", rebelFlagshipNearby));

			result.append("\nNearby Ship...\n");
			if ( nearbyShipState != null )
				result.append(nearbyShipState.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nNearby Ship AI...\n");
			if ( nearbyShipAI != null )
				result.append(nearbyShipAI.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nEnvironment Hazards...\n");
			if ( environment != null )
				result.append(environment.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nZeus?...\n");
			if ( unknownZeus != null )
				result.append(unknownZeus.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nRebel Flagship...\n");
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
		private boolean auto = false;  // Is autoShip.
		private String shipName, shipBlueprintId, shipLayoutId;
		private String shipGfxBaseName;
		private ArrayList<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private int jumpTicks = 0;
		private int hullAmt=0, fuelAmt=0, dronePartsAmt=0, missilesAmt=0, scrapAmt=0;
		private ArrayList<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity = 0;
		private LinkedHashMap<SystemType, List<SystemState>> systemsMap = new LinkedHashMap<SystemType, List<SystemState>>();
		private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<ExtendedSystemInfo>();
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
			systemsMap.clear();
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
		 * This counts to 85000.
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


		public void addExtendedSystemInfo( ExtendedSystemInfo info ) {
			extendedSystemInfoList.add( info );
		}

		public void setExtendedSystemInfoList( List<ExtendedSystemInfo> iotaList ) { this.extendedSystemInfoList = extendedSystemInfoList; }
		public List<ExtendedSystemInfo> getExtendedSystemInfoList() { return extendedSystemInfoList; }

		public <T extends ExtendedSystemInfo> List<T> getExtendedSystemInfoList( Class<T> infoClass ) {
			List<T> result = new ArrayList<T>( 1 );
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if ( infoClass.isInstance(info) ) {
					result.add( infoClass.cast(info) );
				}
			}
			return result;
		}


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
			List<SystemState> systemList = systemsMap.get( s.getSystemType() );
			if ( systemList == null ) {
				systemList = new ArrayList<SystemState>( 0 );
				systemsMap.put( s.getSystemType(), systemList );
			}
			systemList.add( s );
		}

		/**
		 * Returns the first SystemState of a given type, or null.
		 */
		public SystemState getSystem( SystemType systemType ) {
			List<SystemState> systemList = systemsMap.get( systemType );

			if ( systemList != null && !systemList.isEmpty() ) {
				return systemList.get( 0 );
			}

			return null;
		}

		/**
		 * Returns a list of all SystemStates of a given type.
		 *
		 * If no SystemStates are present, an empty list is returned.
		 * That same list object will later contain systems if any are added.
		 */
		public List<SystemState> getSystems( SystemType systemType ) {
			List<SystemState> systemList = systemsMap.get( systemType );
			if ( systemList == null ) {
				systemList = new ArrayList<SystemState>( 0 );
				systemsMap.put( systemType, systemList );
			}

			return systemList;
		}

		public Map<SystemType, List<SystemState>> getSystemsMap() { return systemsMap; }


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
			first = true;
			for ( Map.Entry<SystemType, List<SystemState>> entry : systemsMap.entrySet() ) {
				for ( SystemState s : entry.getValue() ) {
					if (first) { first = false; }
					else { result.append(",\n"); }
					result.append(s.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
				}
			}

			result.append("\nExtended System Info...\n");
			first = true;
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(info.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
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
		ANAEROBIC("anaerobic", 100),
		BATTLE   ("battle",    150),
		CRYSTAL  ("crystal",   120),
		ENERGY   ("energy",     70),
		ENGI     ("engi",      100),
		GHOST    ("ghost",      50),
		HUMAN    ("human",     100),
		MANTIS   ("mantis",    100),
		ROCK     ("rock",      150),
		SLUG     ("slug",      100);

		// The following were introduced in FTL 1.5.4.
		// ANAEROBIC (when DLC is enabled)

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

		// Crystal crews' lockdown wall-coating is not stored, but ability recharge is.
		// Zoltan-produced power is not stored in SystemState.

		private String name = "Frank";
		private String race = CrewType.HUMAN.getId();
		private boolean enemyBoardingDrone = false;
		private int health = 0;
		private int spriteX=0, spriteY=0;
		private int roomId = -1;
		private int roomSquare = -1;
		private boolean playerControlled = false;
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private List<Integer> spriteTintIndeces = new ArrayList<Integer>();
		private boolean mindControlled = false;
		private int savedRoomId = 0;
		private int savedRoomSquare = 0;
		private int pilotSkill=0, engineSkill=0, shieldSkill=0;
		private int weaponSkill=0, repairSkill=0, combatSkill=0;
		private boolean male = true;
		private int repairs=0, combatKills=0, pilotedEvasions=0;
		private int jumpsSurvived=0, skillMasteries=0;
		private int stunTicks = 0;
		private int healthBoost = 0;
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
		private int lockdownRechargeTicksGoal = 0;
		private int unknownOmega = 0;


		public CrewState() {
		}

		/**
		 * Copy constructor.
		 */
		public CrewState( CrewState srcCrew ) {
			name = srcCrew.getName();
			race = srcCrew.getRace();
			enemyBoardingDrone = srcCrew.isEnemyBoardingDrone();
			health = srcCrew.getHealth();
			spriteX = srcCrew.getSpriteX();
			spriteY = srcCrew.getSpriteY();
			roomId = srcCrew.getRoomId();
			roomSquare = srcCrew.getRoomSquare();
			playerControlled = srcCrew.isPlayerControlled();
			unknownAlpha = srcCrew.getUnknownAlpha();
			unknownBeta = srcCrew.getUnknownBeta();

			for ( Integer colorIndex : srcCrew.getSpriteTintIndeces() ) {
				spriteTintIndeces.add( new Integer( colorIndex ) );
			}

			mindControlled = srcCrew.isMindControlled();
			savedRoomId = srcCrew.getSavedRoomId();
			savedRoomSquare = srcCrew.getSavedRoomSquare();
			pilotSkill = srcCrew.getPilotSkill();
			engineSkill = srcCrew.getEngineSkill();
			shieldSkill = srcCrew.getShieldSkill();
			weaponSkill = srcCrew.getWeaponSkill();
			repairSkill = srcCrew.getRepairSkill();
			combatSkill = srcCrew.getCombatSkill();
			male = srcCrew.isMale();
			repairs = srcCrew.getRepairs();
			combatKills = srcCrew.getCombatKills();
			pilotedEvasions = srcCrew.getPilotedEvasions();
			jumpsSurvived = srcCrew.getJumpsSurvived();
			skillMasteries = srcCrew.getSkillMasteries();
			stunTicks = srcCrew.getStunTicks();
			healthBoost = srcCrew.getHealthBoost();
			unknownIota = srcCrew.getUnknownIota();
			unknownKappa = srcCrew.getUnknownKappa();
			unknownLambda = srcCrew.getUnknownLambda();
			unknownMu = srcCrew.getUnknownMu();
			unknownNu = srcCrew.getUnknownNu();
			unknownXi = srcCrew.getUnknownXi();
			unknownOmicron = srcCrew.getUnknownOmicron();
			unknownPi = srcCrew.getUnknownPi();
			unknownRho = srcCrew.getUnknownRho();
			unknownSigma = srcCrew.getUnknownSigma();
			unknownTau = srcCrew.getUnknownTau();
			unknownUpsilon = srcCrew.getUnknownUpsilon();
			unknownPhi = srcCrew.getUnknownPhi();
			lockdownRechargeTicks = srcCrew.getLockdownRechargeTicks();
			lockdownRechargeTicksGoal = srcCrew.getLockdownRechargeTicksGoal();
			unknownOmega = srcCrew.getUnknownOmega();
		}

		public void setName( String s ) { name = s; }
		public String getName() { return name; }

		public void setRace( String s ) { race = s; }
		public String getRace() { return race; }

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

		/**
		 * Sets this crew's current hit points.
		 *
		 * For preserved dead crew, which have no body, this is 0.
		 *
		 * This value includes any temporary modifiers, and may exceed the
		 * CrewType's normal max health. FTL 1.01-1.03.3 had no such modifiers
		 * and capped health at the max.
		 *
		 * @see setHealthBoost(int)
		 */
		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		/**
		 * Sets the position of the crew's image.
		 *
		 * Technically the roomId/square fields set the crew's desired location.
		 * This field is where the crew really is, possibly en route.
		 *
		 * It's the position of the crew image's center, relative to the
		 * top-left square's corner, in pixels, plus (the ShipLayout's offset
		 * times the square-size, which is 35).
		 *
		 * For preserved dead crew, which have no body, this lingers, or may be
		 * (0,0).
		 */
		public void setSpriteX( int n ) { spriteX = n; };
		public void setSpriteY( int n ) { spriteY = n; };
		public int getSpriteX() { return spriteX; }
		public int getSpriteY() { return spriteY; }

		/**
		 * Sets the room this crew is in (or at least trying to move toward).
		 *
		 * For preserved dead crew, which have no body, this is -1.
		 *
		 * roomId and roomSquare need to be specified together.
		 */
		public void setRoomId( int n ) { roomId = n; }
		public int getRoomId() { return roomId; }

		/**
		 * Sets the square this crew is in (or at least trying to move toward).
		 *
		 * Squares are indexed horizontally, left-to-right, wrapping into the
		 * next row down.
		 *
		 * For preserved dead crew, which have no body, this is -1.
		 *
		 * roomId and roomSquare need to be specified together.
		 */
		public void setRoomSquare( int n ) { roomSquare = n; }
		public int getRoomSquare() { return roomSquare; }

		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public boolean isPlayerControlled() { return playerControlled; }

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

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

		public void setPilotSkill( int n ) { pilotSkill = n; }
		public void setEngineSkill( int n ) { engineSkill = n; }
		public void setShieldSkill( int n ) { shieldSkill = n; }
		public void setWeaponSkill( int n ) { weaponSkill = n; }
		public void setRepairSkill( int n ) { repairSkill = n; }
		public void setCombatSkill( int n ) { combatSkill = n; }

		public int getPilotSkill() { return pilotSkill; }
		public int getEngineSkill() { return engineSkill; }
		public int getShieldSkill() { return shieldSkill; }
		public int getWeaponSkill() { return weaponSkill; }
		public int getRepairSkill() { return repairSkill; }
		public int getCombatSkill() { return combatSkill; }

		/**
		 * Toggles sex.
		 * Humans with this set to false have a
		 * female image. Other races accept the
		 * flag but use the same image as male.
		 *
		 * No Andorians in the game, so this is
		 * only a two-state boolean.
		 */
		public void setMale( boolean b ) { male = b; }
		public boolean isMale() { return male; }

		public void setRepairs( int n ) { repairs = n; }
		public void setCombatKills( int n ) { combatKills = n; }
		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }

		public int getRepairs() { return repairs; }
		public int getCombatKills() { return combatKills; }
		public int getPilotedEvasions() { return pilotedEvasions; }
		public int getJumpsSurvived() { return jumpsSurvived; }

		/**
		 * Sets the total number of skill masteries ever earned.
		 *
		 * This is incremented when any skill reaches the first or second
		 * mastery interval. So this is intended to max out at 12.
		 *
		 * Bug: In FTL 1.5.4-1.5.10, Clonebay skill penalties allowed crew to
		 * re-earn masteries and increment further.
		 */
		public void setSkillMasteries( int n ) { skillMasteries = n; }
		public int getSkillMasteries() { return skillMasteries; }

		/**
		 * Sets time required for stun to wear off.
		 *
		 * If greater than 0, the crew will become unresponsive while this
		 * number decrements to 0.
		 *
		 * A weapon sets X*1000 ticks, where X is the value of the 'stun' tag
		 * in its WeaponBlueprint xml. Additional hits from a stun weapon will
		 * reset this value, but only if it would be higher than the current
		 * value.
		 *
		 * When not stunned, this will be 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setStunTicks( int n ) { stunTicks = n; }
		public int getStunTicks() { return stunTicks; }

		/**
		 * Sets temporary bonus health from a foreign Mind Control system.
		 *
		 * When the mind control effect expires, the boost amount will be
		 * subtracted from health, and this value will reset to 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setHealthBoost( int n ) { healthBoost = n; }
		public int getHealthBoost() { return healthBoost; }

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
		 * @see #setLockdownRechargeTicksGoal(int)
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
		public void setLockdownRechargeTicksGoal( int n ) { lockdownRechargeTicksGoal = n; }
		public int getLockdownRechargeTicksGoal() { return lockdownRechargeTicksGoal; }

		public void setUnknownOmega( int n ) { unknownOmega = n; }
		public int getUnknownOmega() { return unknownOmega; }


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
			result.append(String.format("RoomId:            %5d\n", roomId));
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

			FTLConstants origConstants = new OriginalFTLConstants();
			FTLConstants advConstants = new AdvancedFTLConstants();

			result.append(String.format("Saved RoomId:      %5d\n", savedRoomId));
			result.append(String.format("Saved Room Square: %5d\n", savedRoomSquare));
			result.append(String.format("Pilot Skill:       %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", pilotSkill, origConstants.getMasteryIntervalPilot(race), advConstants.getMasteryIntervalPilot(race)));
			result.append(String.format("Engine Skill:      %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", engineSkill, origConstants.getMasteryIntervalEngine(race), advConstants.getMasteryIntervalEngine(race)));
			result.append(String.format("Shield Skill:      %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", shieldSkill, origConstants.getMasteryIntervalShield(race), advConstants.getMasteryIntervalShield(race)));
			result.append(String.format("Weapon Skill:      %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", weaponSkill, origConstants.getMasteryIntervalWeapon(race), advConstants.getMasteryIntervalWeapon(race)));
			result.append(String.format("Repair Skill:      %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", repairSkill, origConstants.getMasteryIntervalRepair(race), advConstants.getMasteryIntervalRepair(race)));
			result.append(String.format("Combat Skill:      %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", combatSkill, origConstants.getMasteryIntervalCombat(race), advConstants.getMasteryIntervalCombat(race)));
			result.append(String.format("Repairs:           %5d\n", repairs));
			result.append(String.format("Combat Kills:      %5d\n", combatKills));
			result.append(String.format("Piloted Evasions:  %5d\n", pilotedEvasions));
			result.append(String.format("Jumps Survived:    %5d\n", jumpsSurvived));
			result.append(String.format("Skill Masteries:   %5d\n", skillMasteries));
			result.append(String.format("Stun Ticks:       %6d (Decrements to 0)\n", stunTicks));
			result.append(String.format("Health Boost:     %6d (Subtracted from health when Mind Ctrl expires)\n", healthBoost));
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
			result.append(String.format("Lockdown Goal:    %6d (Crystal only, ticks needed to recharge)\n", lockdownRechargeTicksGoal));
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
		BATTERY   ("battery",    true),
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
		private SystemType systemType;
		private int capacity = 0;
		private int power = 0;
		private int damagedBars = 0;
		private int ionizedBars = 0;
		private int repairProgress = 0;
		private int damageProgress = 0;
		private int deionizationTicks = Integer.MIN_VALUE;

		private int batteryPower = 0;
		private int hackLevel = 0;
		private boolean hacked = false;
		private int temporaryCapacityCap = 1000;
		private int temporaryCapacityLoss = 0;
		private int temporaryCapacityDivisor = 1;


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
		 *
		 * @see net.blerf.ftl.constants.FTLConstants#getMaxIonizedBars()
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
		 * While disrupting, this was observed to be 2; while the havking drone
		 * was passively attached, this was 1.
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
		 * This only describes attachment, not disruption.
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



	public static enum StationDirection { DOWN, RIGHT, UP, LEFT, NONE }

	public static class RoomState {
		private int oxygen = 100;
		private ArrayList<SquareState> squareList = new ArrayList<SquareState>();

		private int stationSquare = -1;
		private StationDirection stationDirection = StationDirection.NONE;


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
		 * When the system's capacity is 0, this is not set.
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
		 * When the system's capacity is 0, this is not set.
		 *
		 * The station's room square must be set as well.
		 *
		 * This was introduced in FTL 1.5.4.
		 * @param n 0=D,1=R,2=U,3=L,4=None
		 */
		public void setStationDirection( StationDirection d ) { stationDirection = d; }
		public StationDirection getStationDirection() { return stationDirection; }

		/**
		 * Adds a floor square to the room.
		 *
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

			result.append(String.format("Oxygen: %3d%%\n", oxygen));
			result.append(String.format("Station Square: %2d, Station Direction: %s\n", stationSquare, stationDirection.toString()));

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
		 * Sets time elapsed waiting for the weapon to cool down.
		 *
		 * This increments from 0, by 1 each second, until the
		 * weaponBlueprint's cooldown (0 when not armed).
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
			result.append(String.format("Cooldown Ticks: %2d (max: %-2s) (Not used as of FTL 1.5.4.)\n", cooldownTicks, cooldownString));

			return result.toString();
		}
	}



	public static enum DroneType {
		// TODO: Magic numbers.
		BATTLE     ("BATTLE",      150),
		REPAIR     ("REPAIR",       25),
		BOARDER    ("BOARDER",       1),
		HACKING    ("HACKING",       1),
		COMBAT     ("COMBAT",        1),
		BEAM       ("BEAM",          1),
		DEFENSE    ("DEFENSE",       1),
		SHIELD     ("SHIELD",        1),
		SHIP_REPAIR("SHIP_REPAIR",   1);

		// The following were introduced in FTL 1.5.4.
		// HACKING, BEAM, SHIELD

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
		private int visited = 0;
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

		private StoreState store = null;

		// TODO: Make 'visited' an enum.

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
		 * If non-zero, starscape and sprite paths must be set,
		 * as well as the sprite's X, Y, and rotation.
		 *
		 * When non-zero, this prevents randomly generated events
		 * from triggering. The sector exit will still exist.
		 *
		 * Values:
		 *   0 = Unvisited
		 *   1 = Visited
		 *   2 = ??? (Seen on the base beacon in sector 8 in FTL 1.5.4)
		 */
		public void setVisited( int n ) { visited = n; }
		public int getVisited() { return visited; }

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
		 * as well as the ShipEvent seed.
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
		 *
		 * In distant beacons occupied by the rebel fleet, this has been
		 * observed varying between saves during a single fight!?
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
		 * Places a store at this beacon, or null for none.
		 */
		public void setStore( StoreState storeState ) { store = storeState; }
		public StoreState getStore() { return store; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Visited:               %5d\n", visited));
			if ( visited > 0 ) {
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

			if ( store != null ) {
				result.append( store.toString().replaceAll("(^|\n)(.+)", "$1  $2") );
			}

			return result.toString();
		}
	}



	public static class StoreState {
		private int fuel = 0, missiles = 0, droneParts = 0;
		private List<StoreShelf> shelfList = new ArrayList<StoreShelf>(3);


		/**
		 * Constructs a StoreState.
		 *
		 * FTL 1.01-1.03.3 always had two StoreShelf objects.
		 * If more are added, only the first two will be saved.
		 * If fewer, writeBeacon() will add dummy shelves with no items.
		 *
		 * FTL 1.5.4 it can have a variable number of shelves, but zero
		 * crashes the game. So writeBeacon() will add a dummy shelf.
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


		/**
		 * Constructor.
		 *
		 * Up to 3 StoreItems may to be added (Set the StoreItemType, too.)
		 * Fewer StoreItems mean empty space on the shelf.
		 */
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

	/**
	 * An item in a store that either can be bought or has been bought already.
	 */
	public static class StoreItem {
		private boolean available;
		private String itemId;


		/**
		 * Constructor.
		 *
		 * @param available true if the item has not been sold already, false otherwise
		 * @param itemId an id of a blueprint/race/system
		 */
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
		private int shipEventSeed = 0;
		private String surrenderEventId = "";
		private String escapeEventId = "";
		private String destroyedEventId = "";
		private String deadCrewEventId = "";
		private String gotAwayEventId = "";

		private String lastEventId = "";
		private String text = "";
		private int affectedCrewSeed = -1;
		private ArrayList<Integer> choiceList = new ArrayList<Integer>();


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

		public void setSurrenderEventId( String s ) { surrenderEventId = s; }
		public void setEscapeEventId( String s ) { escapeEventId = s; }
		public void setDestroyedEventId( String s ) { destroyedEventId = s; }
		public void setDeadCrewEventId( String s ) { deadCrewEventId = s; }
		public void setGotAwayEventId( String s ) { gotAwayEventId = s; }

		public String getSurrenderEventId() { return surrenderEventId; }
		public String getEscapeEventId() { return escapeEventId; }
		public String getDestroyedEventId() { return destroyedEventId; }
		public String getDeadCrewEventId() { return deadCrewEventId; }
		public String getGotAwayEventId() { return gotAwayEventId; }

		public void setLastEventId( String s ) { lastEventId = s; }
		public String getLastEventId() { return lastEventId; }

		public void setText( String s ) { text = s; }
		public String getText() { return text; }

		/**
		 * Sets a seed used to randomly select crew.
		 *
		 * When saved mid-event, this allows FTL to reselect the same crew.
		 *
		 * When no random selection has been made, this is -1.
		 */
		public void setAffectedCrewSeed( int n ) { affectedCrewSeed = n; }
		public int getAffectedCrewSeed() { return affectedCrewSeed; }

		/**
		 * Sets a list of breadcrumbs for choices made during the last event.
		 *
		 * Each integer in the list corresponds to a prompt, and the Integer's
		 * value is the Nth choice that was clicked. (0-based)
		 *
		 * The event will still be in-progress if there aren't enough
		 * breadcrumbs to renavigate to the end of the event.
		 *
		 * The list typically ends with a 0, since events usually conclude with
		 * a lone "continue" choice.
		 */
		public void setChoiceList( ArrayList<Integer> choiceList ) { this.choiceList = choiceList; }
		public ArrayList<Integer> getChoiceList() { return choiceList; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append(String.format("Ship Event Seed:    %3d\n", shipEventSeed));
			result.append(String.format("Surrender Event:    %s\n", surrenderEventId));
			result.append(String.format("Escape Event:       %s\n", escapeEventId));
			result.append(String.format("Destroyed Event:    %s\n", destroyedEventId));
			result.append(String.format("Dead Crew Event:    %s\n", deadCrewEventId));
			result.append(String.format("Got Away Event:     %s\n", gotAwayEventId));

			result.append("\n");

			result.append(String.format("Last Event:         %s\n", lastEventId));

			result.append("\nText...\n");
			result.append(String.format("%s\n", text));
			result.append("\n");

			result.append(String.format("Affected Crew Seed: %3d\n", affectedCrewSeed));

			result.append("\nLast Event Choices...\n");
			first = true;
			for ( Integer choiceInt : choiceList ) {
				if ( first ) { first = false; }
				else { result.append(","); }
				result.append(choiceInt);
			}
			result.append("\n");

			return result.toString();
		}
	}



	public static class NearbyShipAIState {
		private boolean surrendered = false;
		private boolean escaping = false;
		private boolean destroyed = false;
		private int surrenderThreshold = 0;
		private int escapeThreshold = 0;
		private int escapeTicks = 15000;
		private boolean stalemateTriggered = false;  // TODO: Does this start sudden death, or mark its completion?
		private int stalemateTicks = 0;
		private int boardingAttempts = 0;
		private int boardersNeeded = 0;


		public NearbyShipAIState() {
		}

		/**
		 * Toggles whether the ship has offered surrender.
		 */
		public void setSurrendered( boolean b ) { surrendered = b; }
		public boolean hasSurrendered() { return surrendered; }

		/**
		 * Toggles whether the ship is powering up its FTL to escape.
		 */
		public void setEscaping( boolean b ) { escaping = b; }
		public boolean isEscaping() { return escaping; }

		/**
		 * Toggles whether the ship has been destroyed.
		 *
		 * TODO: Confirm this.
		 */
		public void setDestroyed( boolean b ) { destroyed = b; }
		public boolean isDestroyed() { return destroyed; }

		/**
		 * Sets the hull amount that will cause the ship will surrender.
		 *
		 * For the rebel flagship, this is negative.
		 */
		public void setSurrenderThreshold( int n ) { surrenderThreshold = n; }
		public int getSurrenderThreshold() { return surrenderThreshold; }

		/**
		 * Sets the hull amount that will cause the ship to flee.
		 *
		 * For the rebel flagship, this is negative.
		 */
		public void setEscapeThreshold( int n ) { escapeThreshold = n; }
		public int getEscapeThreshold() { return escapeThreshold; }

		/**
		 * Sets time elapsed while waiting for the FTL drive to charge.
		 *
		 * This decrements from 15000. At 0, the ship jumps away.
		 *
		 * TODO: An FTL Jammer augment might only override the default once an
		 * escape attempt is initiated. It was still 15000 at the beginning of
		 * one battle.
		 */
		public void setEscapeTicks( int n ) { escapeTicks = n; }
		public int getEscapeTicks() { return escapeTicks; }

		public void setStalemateTriggered( boolean b ) { stalemateTriggered = b; }
		public boolean isStalemateTriggered() { return stalemateTriggered; }

		public void setStalemateTicks( int n ) { stalemateTicks = n; }
		public int getStalemateTicks() { return stalemateTicks; }

		/**
		 * Sets the count of times crew teleported so far.
		 *
		 * After a certain number, no further boarding attempts will be made.
		 *
		 * TODO: Determine that limit, and whether it counts crew or parties.
		 */
		public void setBoardingAttempts( int n ) { boardingAttempts = n; }
		public int getBoardingAttempts() { return boardingAttempts; }

		/**
		 * Sets the number of crew to teleport as boarders.
		 *
		 * Matthew's hint: It's based on the original crew strength.
		 *
		 * TODO: Test if this is the limit for setBoardingAttempts().
		 */
		public void setBoardersNeeded( int n ) { boardersNeeded = n; }
		public int getBoardersNeeded() { return boardersNeeded; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Surrender Offered:   %7b\n", surrendered));
			result.append(String.format("Escaping:            %7b\n", escaping));
			result.append(String.format("Destroyed?:          %7b\n", destroyed));
			result.append(String.format("Surrender Threshold: %7d (Hull amount when surrender is offered)\n", surrenderThreshold));
			result.append(String.format("Escape Threshold:    %7d (Hull amount when escape begins)\n", escapeThreshold));
			result.append(String.format("Escape Ticks:        %7d (Decrements from 15000)\n", escapeTicks));
			result.append(String.format("Stalemate Triggered?:%7b\n", stalemateTriggered));
			result.append(String.format("Stalemate Ticks?:    %7d\n", stalemateTicks));
			result.append(String.format("Boarding Attempts?:  %7d\n", boardingAttempts));
			result.append(String.format("Boarders Needed?:    %7d\n", boardersNeeded));

			return result.toString();
		}
	}



	public static enum HazardVulnerability {
		PLAYER_SHIP, NEARBY_SHIP, BOTH_SHIPS;
	}

	public static class EnvironmentState {
		private int redGiantLevel = 0;
		private int pulsarLevel = 0;
		private int pdsLevel = 0;
		private HazardVulnerability vulnerableShips = HazardVulnerability.BOTH_SHIPS;
		private AsteroidFieldState asteroidField = null;
		private int solarFlareFadeTicks = 0;
		private int havocTicks = 0;
		private int pdsTicks = 0;  // Used by: PDS. Value lingers after leaving a beacon (sometimes varying by 1).


		public EnvironmentState() {
		}

		public void setRedGiantLevel( int n ) { redGiantLevel = n; }
		public int getRedGiantLevel() { return redGiantLevel; }

		public void setPulsarLevel( int n ) { pulsarLevel = n; }
		public int getPulsarLevel() { return pulsarLevel; }

		public void setPDSLevel( int n ) { pdsLevel = n; }
		public int getPDSLevel() { return pdsLevel; }

		/**
		 * Sets which ships will be targeted by a PDS.
		 *
		 * Hint: Values are 0,1,2 for player ship, nearby ship, or both.
		 * (0 and 1 are unconfirmed.)
		 */
		public void setVulnerableShips( HazardVulnerability vuln ) { vulnerableShips = vuln; }
		public HazardVulnerability getVulnerableShips() { return vulnerableShips; }

		public void setAsteroidField( AsteroidFieldState asteroidField ) { this.asteroidField = asteroidField; }
		public AsteroidFieldState getAsteroidField() { return asteroidField; }


		/**
		 * Sets elapsed time while the screen fades to/from white during a
		 * solar flare from a red giant or pulsar.
		 *
		 * TODO: Determine the number this counts to.
		 */
		public void setSolarFlareFadeTicks( int n ) { solarFlareFadeTicks = n; }
		public int getSolarFlareFadeTicks() { return solarFlareFadeTicks; }

		/**
		 * Sets elapsed time while waiting for havoc from a red giant/pulsar/PDS.
		 *
		 * For red giants, This counts to 30000, triggers a solar flare, and
		 * returns to 0. A warning appears around 25000.
		 *
		 * For pulsars, this hasn't been observed over 11000.
		 *
		 * For PDS, this might count to 20000 before firing AT the ship (as
		 * opposed to decorative misses)?
		 *
		 * After leaving a beacon with such hazards, this value lingers (+/-1).
		 */
		public void setHavocTicks( int n ) { havocTicks = n; }
		public int getHavocTicks() { return havocTicks; }

		public void setPDSTicks( int n ) { pdsTicks = n; }
		public int getPDSTicks() { return pdsTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Red Giant Level?:  %7d\n", redGiantLevel));
			result.append(String.format("Pulsar Level?:     %7d\n", pulsarLevel));
			result.append(String.format("PDS Level?:        %7d\n", pdsLevel));
			result.append(String.format("Vulnerable Ships:  %s (PDS only)\n", vulnerableShips.toString()));

			result.append("\nAsteroid Field...\n");
			if ( asteroidField != null )
				result.append( asteroidField.toString().replaceAll("(^|\n)(.+)", "$1  $2") );

			result.append("\n");

			result.append(String.format("Flare Fade Ticks?: %7d\n", solarFlareFadeTicks));
			result.append(String.format("Havoc Ticks?:      %7d (Red Giant/Pulsar/PDS only, Goal varies)\n", havocTicks));
			result.append(String.format("PDS Ticks?:        %7d (PDS only)\n", pdsTicks));

			return result.toString();
		}
	}

	public static class AsteroidFieldState {
		private int unknownAlpha = -1000;  // Decrementing ticks from ~1545, or -1000.
		private int strayRockTicks = 0;
		private int unknownGamma = 0;      // Incrementing counter. 0-based Nth rock? Total rocks?
		private int bgDriftTicks = 0;
		private int currentTarget = 0;


		public AsteroidFieldState() {
		}

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		public void setStrayRockTicks( int n ) { strayRockTicks = n; }
		public int getStrayRockTicks() { return strayRockTicks; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		public void setBgDriftTicks( int n ) { bgDriftTicks = n; }
		public int getBgDriftTicks() { return bgDriftTicks; }

		public void setCurrentTarget( int n ) { currentTarget = n; }
		public int getCurrentTarget() { return currentTarget; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Alpha?:            %7d\n", unknownAlpha));
			result.append(String.format("Stray Rock Ticks?: %7d\n", strayRockTicks));
			result.append(String.format("Gamma?:            %7d\n", unknownGamma));
			result.append(String.format("Bkg Drift Ticks:   %7d\n", bgDriftTicks));
			result.append(String.format("Current Target?:   %7d\n", currentTarget));

			return result.toString();
		}
	}



	/**
	 * Information from previous flagship encounters to use when setting up
	 * the next one.
	 */
	public static class RebelFlagshipState {
		private int pendingStage = 1;
		private LinkedHashMap<Integer, Integer> occupancyMap = new LinkedHashMap<Integer, Integer>();


		/**
		 * Constructor.
		 *
		 * In FTL 1.01-1.03.3, this info is not present in saved games until
		 * after engaging the rebel flagship in sector 8 for the first time.
		 *
		 * In FTL 1.5.4, this is always set, though the occupancy map is empty.
		 */
		public RebelFlagshipState() {
		}

		/**
		 * Sets the next version of the flagship that will be encountered (1-based).
		 *
		 * This must be one of the available stages: 1-3.
		 */
		public void setPendingStage( int pendingStage ) {
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
		 *   Stage 1 (BOSS_1): 0x13=19 rooms
		 *   Stage 2 (BOSS_2): 0x0F=15 rooms
		 *   Stage 3 (BOSS_3): 0x0B=11 rooms
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
			StringBuilder result = new StringBuilder();

			result.append(String.format("Pending Flagship Stage: %s\n", pendingStage));

			result.append("\nOccupancy of Last Seen Flagship...\n");
			for (Map.Entry<Integer, Integer> entry : occupancyMap.entrySet()) {
				int roomId = entry.getKey().intValue();
				int occupantCount = entry.getValue().intValue();

				result.append(String.format("RoomId: %2d, Crew: %d\n", roomId, occupantCount));
			}

			return result.toString();
		}
	}



	public static interface ExtendedSystemInfo {
	}

	public static class ClonebayInfo implements ExtendedSystemInfo {
		private int buildTicks = 0;
		private int buildTicksGoal = 0;
		private int doomTicks = 0;


		public ClonebayInfo() {
		}

		/**
		 * Sets elapsed time while building a clone.
		 *
		 * @param n a positive int less than, or equal to, the goal (0 when not engaged)
		 *
		 * @see #setBuildTicksGoal(int)
		 */
		public void setBuildTicks( int n ) { buildTicks = n; }
		public int getBuildTicks() { return buildTicks; }


		/**
		 * Sets total time needed to finish building a clone.
		 *
		 * This can vary depending on the system level when the clonebay is
		 * initially engaged. When not engaged, this value lingers.
		 *
		 * @see #setBuildTicks(int)
		 */
		public void setBuildTicksGoal( int n ) { buildTicksGoal = n; }
		public int getBuildTicksGoal() { return buildTicksGoal; }

		/**
		 * Sets elapsed time while there are dead crew and the clonebay is unpowered.
		 *
		 * This counts to 3000, at which point dead crew are lost.
		 *
		 * @param n 0-3000, or -1000
		 */
		public void setDoomTicks( int n ) { doomTicks = n; }
		public int getDoomTicks() { return doomTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.CLONEBAY.toString()));
			result.append(String.format("Build Ticks:            %7d (For the roster's topmost dead crew)\n", buildTicks));
			result.append(String.format("Build Ticks Goal:       %7d\n", buildTicksGoal));
			result.append(String.format("DoomTicks:              %7d (If unpowered, dead crew are lost at 3000)\n", doomTicks));

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
		 * This counts to 1000. When not discharging, it's 1000.
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
		 * This counts to 2000. When not recharging, it's 0.
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

	public static class HackingInfo implements ExtendedSystemInfo {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;

		private int unknownEpsilon = 0;
		private int unknownZeta = 0;
		private int unknownEta = 0;

		private int disruptionTicks = 0;
		private int disruptionTicksGoal = 0;
		private int unknownTheta = 0;

		private UnknownAres unknownAres = null;

		private List<Integer> unknownIota = new ArrayList<Integer>();
		private List<Integer> unknownKappa = new ArrayList<Integer>();

		private int unknownLambda = 0;
		private int unknownMu = 0;


		public HackingInfo() {
		}

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }
		public int getUnknownGamma() { return unknownGamma; }
		public int getUnknownDelta() { return unknownDelta; }

		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public void setUnknownZeta( int n ) { unknownZeta = n; }
		public void setUnknownEta( int n ) { unknownEta = n; }

		public int getUnknownEpsilon() { return unknownEpsilon; }
		public int getUnknownZeta() { return unknownZeta; }
		public int getUnknownEta() { return unknownEta; }

		/**
		 * Sets elapsed time while systems are disrupted.
		 *
		 * @param n a positive int less than, or equal to, the goal (0 when not engaged)
		 *
		 * @see #setDisruptionTicksGoal(int)
		 */
		public void setDisruptionTicks( int n ) { disruptionTicks = n; }
		public int getDisruptionTicks() { return disruptionTicks; }

		/**
		 * Sets total time systems will stay disrupted.
		 *
		 * This can vary depending on the system level when disruption is
		 * initially engaged. When not engaged, this is 10000!?
		 *
		 * @see #setDisruptionTicks(int)
		 */
		public void setDisruptionTicksGoal( int n ) { disruptionTicksGoal = n; }
		public int getDisruptionTicksGoal() { return disruptionTicksGoal; }

		public void setUnknownTheta( int n ) { unknownTheta = n; }
		public int getUnknownTheta() { return unknownTheta; }

		public void setUnknownAres( UnknownAres ares ) { unknownAres = ares; }
		public UnknownAres getUnknownAres() { return unknownAres; }

		public void setUnknownIota( List<Integer> iota ) { unknownIota = iota; }
		public void setUnknownKappa( List<Integer> kappa ) { unknownKappa = kappa; }

		public List<Integer> getUnknownIota() { return unknownIota; }
		public List<Integer> getUnknownKappa() { return unknownKappa; }

		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public void setUnknownMu( int n ) { unknownMu = n; }

		public int getUnknownLambda() { return unknownLambda; }
		public int getUnknownMu() { return unknownMu; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format("%d", n);
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append(String.format("SystemId:                 %s\n", SystemType.HACKING.toString()));
			result.append(String.format("Alpha?:                 %7d\n", unknownAlpha));
			result.append(String.format("Beta?:                  %7d\n", unknownBeta));
			result.append(String.format("Gamma?:                 %7d\n", unknownGamma));
			result.append(String.format("Delta?:                 %7d\n", unknownDelta));
			result.append(String.format("Epsilon?:               %7d\n", unknownEpsilon));
			result.append(String.format("Zeta?:                  %7d\n", unknownZeta));
			result.append(String.format("Eta?:                   %7d\n", unknownEta));
			result.append(String.format("Disruption Ticks:       %7d\n", disruptionTicks));
			result.append(String.format("Disruption Ticks Goal:  %7d\n", disruptionTicksGoal));
			result.append(String.format("Theta?:                 %7d\n", unknownTheta));

			result.append("\nAres?...\n");
			if ( unknownAres != null )
				result.append(unknownAres.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nIota?...\n");
			first = true;
			for (int i=0; i < unknownIota.size(); i++) {
				Integer iotaInt = unknownIota.get( i );
				result.append(String.format("%7s", prettyInt(iotaInt.intValue())));

				if ( i != unknownIota.size()-1 ) {
					if ( i % 3 == 2 ) {
						result.append(",\n");
					} else {
						result.append(", ");
					}
				}
			}
			result.append("\n");

			result.append("\nKappa?...\n");
			first = true;
			for (int i=0; i < unknownKappa.size(); i++) {
				Integer kappaInt = unknownKappa.get( i );
				result.append(String.format("%7s", prettyInt(kappaInt.intValue())));

				if ( i != unknownKappa.size()-1 ) {
					if ( i % 3 == 2 ) {
						result.append(",\n");
					} else {
						result.append(", ");
					}
				}
			}
			result.append("\n");

			result.append("\n");

			result.append(String.format("Lambda?:                %7d\n", unknownLambda));
			result.append(String.format("Mu?:                    %7d\n", unknownMu));

			return result.toString();
		}
	}

	public static class MindInfo implements ExtendedSystemInfo {
		private int mindControlTicksGoal = 0;
		private int mindControlTicks = 0;


		public MindInfo() {
		}

		/**
		 * Sets elapsed time while crew are mind controlled.
		 *
		 * @param n a positive int less than, or equal to, the goal (lingers at goal when not engaged)
		 *
		 * @see #setMindControlTicksGoal(int)
		 */
		public void setMindControlTicks( int n ) { mindControlTicks = n; }
		public int getMindControlTicks() { return mindControlTicks; }

		/**
		 * Sets total time crew will stay mind controlled.
		 *
		 * This can vary depending on the system level when mind control is
		 * initially engaged. When not engaged, this value lingers.
		 *
		 * @see #setMindControlTicks(int)
		 */
		public void setMindControlTicksGoal( int n ) { mindControlTicksGoal = n; }
		public int getMindControlTicksGoal() { return mindControlTicksGoal; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.MIND.toString()));
			result.append(String.format("Mind Ctrl Ticks:        %7d\n", mindControlTicks));
			result.append(String.format("Mind Ctrl Ticks Goal:   %7d\n", mindControlTicksGoal));

			return result.toString();
		}
	}

	public static class ArtilleryInfo implements ExtendedSystemInfo {
		private UnknownAthena unknownAthena = null;


		public ArtilleryInfo() {
		}

		public void setUnknownAthena( UnknownAthena athena ) { unknownAthena = athena; }
		public UnknownAthena getUnknownAthena() { return unknownAthena; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("SystemId:                 %s\n", SystemType.ARTILLERY.toString()));

			result.append("\nAthena?...\n");
			if ( unknownAthena != null )
				result.append(unknownAthena.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			return result.toString();
		}
	}



	public static class UnknownZeus {
		private List<ProjectileState> projectileList = new ArrayList<ProjectileState>();
		private UnknownHephaestus playerHephaestus = null;
		private UnknownHephaestus nearbyHephaestus = null;

		private int unknownEpsilon = 0;
		private Integer unknownZeta = null;

		private boolean autofire = false;

		private int unknownEta = 0;  // TODO: 0 until boss fight, then matches 1-based flagship stage.

		private int unknownIota = 0;
		private int unknownKappa = 0;


		public UnknownZeus() {
		}

		public void setProjectileList( List<ProjectileState> projectileList ) { this.projectileList = projectileList; }
		public List<ProjectileState> getProjectileList() { return projectileList; }

		public void setPlayerHephaestus( UnknownHephaestus hephaestus ) { playerHephaestus = hephaestus; }
		public UnknownHephaestus getPlayerHephaestus() { return playerHephaestus; }

		public void setNearbyHephaestus( UnknownHephaestus hephaestus ) { nearbyHephaestus = hephaestus; }
		public UnknownHephaestus getNearbyHephaestus() { return nearbyHephaestus; }

		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public void setUnknownZeta( Integer zeta ) { unknownZeta = zeta; }

		public int getUnknownEpsilon() { return unknownEpsilon; }
		public Integer getUnknownZeta() { return unknownZeta; }

		public void setAutofire( boolean b ) { autofire = b; }
		public boolean getAutofire() { return autofire; }

		public void setUnknownEta( int n ) { unknownEta = n; }
		public int getUnknownEta() { return unknownEta; }

		public void setUnknownIota( int n ) { unknownIota = n; }
		public void setUnknownKappa( int n ) { unknownKappa = n; }

		public int getUnknownIota() { return unknownIota; }
		public int getUnknownKappa() { return unknownKappa; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format("%d", n);
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append("(Screen coords may be N*1000, where 185.5 becomes 185500.)\n");
			result.append("(Numbers in greek lists are not necessarily related. Just conserving letters.)\n");

			result.append("\nProjectiles...\n");
			int projectileIndex = 0;
			first = true;
			for ( ProjectileState projectile : projectileList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(String.format("Projectile # %2d:\n", projectileIndex++));
				result.append(projectile.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nHephaestus?... (Player Ship)\n");
			if ( playerHephaestus != null )
				result.append(playerHephaestus.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\nHephaestus?... (Nearby Ship)\n");
			if ( nearbyHephaestus != null )
				result.append(nearbyHephaestus.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\n");

			result.append(String.format("Epsilon?:  %11s (Player Ship)\n", prettyInt(unknownEpsilon)));
			result.append(String.format("Zeta?:     %11s (Nearby Ship)\n", (unknownZeta != null ? prettyInt(unknownZeta.intValue()) : "N/A")));
			result.append(String.format("Autofire:  %11b\n", autofire));
			result.append(String.format("Eta?:      %11d\n", unknownEta));
			result.append(String.format("Iota?:     %11s\n", prettyInt(unknownIota)));
			result.append(String.format("Kappa?:    %11s\n", prettyInt(unknownKappa)));

			return result.toString();
		}
	}



	// Extended infos related to a ship.
	public static class UnknownHephaestus {
		private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<ExtendedSystemInfo>();
		private List<UnknownPolemos> polemosList = new ArrayList<UnknownPolemos>();
		private List<UnknownAthena> athenaList = new ArrayList<UnknownAthena>();
		private List<UnknownEnyalius> enyaliusList = new ArrayList<UnknownEnyalius>();


		public UnknownHephaestus() {
		}


		public void addExtendedSystemInfo( ExtendedSystemInfo info ) {
			extendedSystemInfoList.add( info );
		}

		public void setExtendedSystemInfoList( List<ExtendedSystemInfo> iotaList ) { this.extendedSystemInfoList = extendedSystemInfoList; }
		public List<ExtendedSystemInfo> getExtendedSystemInfoList() { return extendedSystemInfoList; }

		public <T extends ExtendedSystemInfo> List<T> getExtendedSystemInfoList( Class<T> infoClass ) {
			List<T> result = new ArrayList<T>( 1 );
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if ( infoClass.isInstance(info) ) {
					result.add( infoClass.cast(info) );
				}
			}
			return result;
		}


		public void setPolemosList( List<UnknownPolemos> polemosList ) { this.polemosList = polemosList; }
		public List<UnknownPolemos> getPolemosList() { return polemosList; }

		public void setAthenaList( List<UnknownAthena> athenaList ) { this.athenaList = athenaList; }
		public List<UnknownAthena> getAthenaList() { return athenaList; }

		public void setEnyaliusList( List<UnknownEnyalius> enyaliusList ) { this.enyaliusList = enyaliusList; }
		public List<UnknownEnyalius> getEnyaliusList() { return enyaliusList; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append("\nMore Extended System Info...\n");
			first = true;
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(info.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nExtended Drone Info...\n");
			int polemosIndex = 0;
			first = true;
			for ( UnknownPolemos polemos : polemosList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(String.format("Drone # %2d:\n", polemosIndex++));
				result.append(polemos.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nExtended Weapon Info...\n");
			int athenaIndex = 0;
			first = true;
			for ( UnknownAthena athena : athenaList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(String.format("Weapon #%2d:\n", athenaIndex++));
				result.append(athena.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nStandalone Surge Drones...\n");
			int enyaliusIndex = 0;
			first = true;
			for ( UnknownEnyalius enyalius : enyaliusList ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(String.format("Surge Drone # %2d:\n", enyaliusIndex++));
				result.append(enyalius.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			return result.toString();
		}
	}



	/**
	 * Constants for projectile/damage ownership.
	 *
	 * OwnerId (-1, 0, 1)
	 */
	public static enum Affiliation {
		NEUTRAL, PLAYER_SHIP, NEARBY_SHIP;
	}



	public static class ProjectileState {
		private int projectileType = 0;
		private int currentPosX = 0, currentPosY = 0;
		private int prevPosX = 0, prevPosY = 0;
		private int speed = 0;
		private int goalPosX = 0, goalPosY = 0;
		private int heading = 0;
		private int ownerId = 0;  // Might be 0 for player ship, 1 for nearby ship?
		private int selfId = 0;

		private DamageState damage = null;

		private int lifespan = 0;
		private int destinationSpace = 0;
		private int currentSpace = 0;
		private int targetId = 0;
		private boolean dead = false;

		private String deathAnimId = "";
		private String flightAnimId = "";

		// TODO: animState[3] is playTicks, animState[4] is playTicksGoal.
		// animState[0,1] may be bools, animState[2] is a small int.
		private int[] deathAnimState = new int[7];
		private int[] flightAnimState = new int[7];

		private int speedX = 0, speedY = 0;
		private boolean missed = false;
		private boolean hitTarget = false;

		private String hitSolidSound = "";
		private String hitShieldSound = "";
		private String missSound = "";

		private int entryAngle = 0;  // Guess: X degrees CCW, where 0 is due East.
		private boolean startedDying = false;
		private boolean passedTarget = false;

		private int type = 0;
		private boolean broadcastTarget = false;

		private List<Integer> unknownAlpha = new ArrayList<Integer>();


		/**
		 * Constructs an incomplete ProjectileState.
		 */
		public ProjectileState() {
		}

		public void setProjectileType( int n ) { projectileType = n; }
		public int getProjectileType() { return projectileType; }

		public void setCurrentPositionX( int n ) { currentPosX = n; }
		public void setCurrentPositionY( int n ) { currentPosY = n; }
		public int getCurrentPositionX() { return currentPosX; }
		public int getCurrentPositionY() { return currentPosY; }

		public void setPreviousPositionX( int n ) { prevPosX = n; }
		public void setPreviousPositionY( int n ) { prevPosY = n; }
		public int getPreviousPositionX() { return prevPosX; }
		public int getPreviousPositionY() { return prevPosY; }

		public void setSpeed( int n ) { speed = n; }
		public void setGoalPositionX( int n ) { goalPosX = n; }
		public void setGoalPositionY( int n ) { goalPosY = n; }
		public void setHeading( int n ) { heading = n; }
		public void setOwnerId( int n ) { ownerId = n; }
		public void setSelfId( int n ) { selfId = n; }

		public int getSpeed() { return speed; }
		public int getGoalPositionX() { return goalPosX; }
		public int getGoalPositionY() { return goalPosY; }
		public int getHeading() { return heading; }
		public int getOwnerId() { return ownerId; }
		public int getSelfId() { return selfId; }

		public void setDamage( DamageState damage ) { this.damage = damage; }
		public DamageState getDamage() { return damage; }

		public void setLifespan( int n ) { lifespan = n; }
		public void setDestinationSpace( int n ) { destinationSpace = n; }
		public void setCurrentSpace( int n ) { currentSpace = n; }
		public void setTargetId( int n ) { targetId = n; }
		public void setDead( boolean b ) { dead = b; }

		public int getLifespan() { return lifespan; }
		public int getDestinationSpace() { return destinationSpace; }
		public int getCurrentSpace() { return currentSpace; }
		public int getTargetId() { return targetId; }
		public boolean isDead() { return dead; }

		public void setDeathAnimId( String s ) { deathAnimId = s; }
		public void setFlightAnimId( String s ) { flightAnimId = s; }

		public String getDeathAnimId() { return deathAnimId; }
		public String getFlightAnimId() { return flightAnimId; }

		public void setDeathAnimState( int index, int n ) { deathAnimState[index] = n; }
		public void setFlightAnimState( int index, int n ) { flightAnimState[index] = n; }

		public int[] getDeathAnimState() { return deathAnimState; }
		public int[] getFlightAnimState() { return flightAnimState; }

		public void setSpeedX( int n ) { speedX = n; }
		public void setSpeedY( int n ) { speedY = n; }

		public int getSpeedX() { return speedX; }
		public int getSpeedY() { return speedY; }

		public void setMissed( boolean b ) { missed = b; }
		public void setHitTarget( boolean b ) { hitTarget = b; }
		public void setHitSolidSound( String s ) { hitSolidSound = s; }
		public void setHitShieldSound( String s ) { hitShieldSound = s; }
		public void setMissSound( String s ) { missSound = s; }
		public void setEntryAngle( int n ) { entryAngle = n; }
		public void setStartedDying( boolean b ) { startedDying = b; }
		public void setPassedTarget( boolean b ) { passedTarget = b; }

		public boolean hasMissed() { return missed; }
		public boolean hasHitTarget() { return hitTarget; }
		public String getHitSolidSound() { return hitSolidSound; }
		public String getHitShieldSound() { return hitShieldSound; }
		public String getMissSound() { return missSound; }
		public int getEntryAngle() { return entryAngle; }
		public boolean hasStartedDying() { return startedDying; }
		public boolean hasPassedTarget() { return passedTarget; }

		public void setType( int n ) { type = n; }
		public int getType() { return type; }

		public void setBroadcastTarget( boolean b ) { broadcastTarget = b; }
		public boolean getBroadcastTarget() { return broadcastTarget; }

		public void setUnknownAlpha( List<Integer> alpha ) { unknownAlpha = alpha; }
		public List<Integer> getUnknownAlpha() { return unknownAlpha; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format("%d", n);
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Projectile Type?:  %7d\n", projectileType));

			if ( projectileType == 0 ) {
				result.append("\n");
				result.append("(When Projectile Type is 0, no other fields are set.)\n");
				return result.toString();
			}

			result.append(String.format("Current Position:  %7d,%7d (%8.03f,%8.03f)\n", currentPosX, currentPosY, currentPosX/1000f, currentPosY/1000f));
			result.append(String.format("Previous Position: %7d,%7d (%8.03f,%8.03f)\n", prevPosX, prevPosY, prevPosX/1000f, prevPosY/1000f));
			result.append(String.format("Speed?:            %7d\n", speed));
			result.append(String.format("Goal Position:     %7d,%7d (%8.03f,%8.03f)\n", goalPosX, goalPosY, goalPosX/1000f, goalPosY/1000f));
			result.append(String.format("Heading?:          %7d\n", heading));
			result.append(String.format("OwnerId?:          %7d\n", ownerId));
			result.append(String.format("SelfId?:           %7d\n", selfId));

			result.append(String.format("\nDamage...\n"));
			if ( damage != null )
				result.append(damage.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\n");
			result.append(String.format("Lifespan:          %7d\n", lifespan));
			result.append(String.format("Destination Space?:%7d\n", destinationSpace));
			result.append(String.format("Current Space?:    %7d\n", currentSpace));
			result.append(String.format("TargetId?:         %7d\n", targetId));
			result.append(String.format("Dead:              %7b\n", dead));
			result.append(String.format("Death AnimId:      %s\n", deathAnimId));
			result.append(String.format("Flight AnimId:     %s\n", flightAnimId));

			result.append(String.format("\nDeath Anim State?...\n"));
			result.append(String.format("%7d, %7d, %7d,\n", deathAnimState[0], deathAnimState[1], deathAnimState[2]));
			result.append(String.format("%7d, %7d,\n", deathAnimState[3], deathAnimState[4]));
			result.append(String.format("%7d, %7d\n", deathAnimState[5], deathAnimState[6]));

			result.append(String.format("\nFlight Anim State?...\n"));
			result.append(String.format("%7d, %7d, %7d,\n", flightAnimState[0], flightAnimState[1], flightAnimState[2]));
			result.append(String.format("%7d, %7d,\n", flightAnimState[3], flightAnimState[4]));
			result.append(String.format("%7d, %7d\n", flightAnimState[5], flightAnimState[6]));

			result.append("\n");

			result.append(String.format("Speed (x,y):       %7d,%7d (%8.03f,%8.03f)\n", speedX, speedY, speedX/1000f, speedY/1000f));
			result.append(String.format("Missed:            %7b\n", missed));
			result.append(String.format("Hit Target:        %7b\n", hitTarget));
			result.append(String.format("Hit Hull Sound:    %s\n", hitSolidSound));
			result.append(String.format("Hit Shield Sound:  %s\n", hitShieldSound));
			result.append(String.format("Miss Sound:        %s\n", missSound));
			result.append(String.format("Entry Angle?:      %7s\n", prettyInt(entryAngle)));
			result.append(String.format("Started Dying?:    %7b\n", startedDying));
			result.append(String.format("Passed Target?:    %7b\n", passedTarget));

			result.append("\n");

			result.append(String.format("Type?:             %7d\n", type));
			result.append(String.format("Broadcast Target?: %7b\n", broadcastTarget));

			result.append(String.format("\nAlpha?...\n"));
			for (int i=0; i < unknownAlpha.size(); i++) {
				Integer eInt = unknownAlpha.get( i );
				result.append(String.format("%7s", prettyInt(eInt.intValue())));

				if ( i != unknownAlpha.size()-1 ) {
					if ( i % 2 == 1 ) {
						result.append(",\n");
					} else {
						result.append(", ");
					}
				}
			}
			result.append("\n");

			return result.toString();
		}
	}

	public static class DamageState {
		private int hullDamage = 0;
		private int shieldPiercing = 0;
		private int fireChance = 0;
		private int breachChance = 0;
		private int ionDamage = 0;
		private int systemDamage = 0;
		private int personnelDamage = 0;
		private boolean hullBuster = false;
		private int ownerId = 0;
		private int selfId = 0;
		private boolean lockdown = false;
		private boolean crystalShard = false;
		private int stunChance = 0;
		private int stunAmount = 0;


		/**
		 * Constructor.
		 */
		public DamageState() {
		}

		public void setHullDamage( int n ) { hullDamage = n; }
		public void setShieldPiercing( int n ) { shieldPiercing = n; }
		public void setFireChance( int n ) { fireChance = n; }
		public void setBreachChance( int n ) { breachChance = n; }
		public void setIonDamage( int n ) { ionDamage = n; }
		public void setSystemDamage( int n ) { systemDamage = n; }

		public int getHullDamage() { return hullDamage; }
		public int getShieldPiercing() { return shieldPiercing; }
		public int getFireChance() { return fireChance; }
		public int getBreachChance() { return breachChance; }
		public int getIonDamage() { return ionDamage; }
		public int getSystemDamage() { return systemDamage; }

		/**
		 * Sets damage to apply to personnel.
		 *
		 * This is dealt per-square to each crew in the room hit. A Beam weapon
		 * can injure someone twice if it follows them into another room.
		 */
		public void setPersonnelDamage( int n ) { personnelDamage = n; }
		public int getPersonnelDamage() { return personnelDamage; }

		/**
		 * Toggles whether this projectile deals double hull damage against
		 * systemless rooms.
		 *
		 * This is based on the 'hullBust' tag (0/1) of a WeaponBlueprint's xml.
		 */
		public void setHullBuster( boolean b ) { hullBuster = b; }
		public boolean isHullBuster() { return hullBuster; }

		public void setOwnerId( int n ) { ownerId = n; }
		public void setSelfId( int n ) { selfId = n; }
		public void setLockdown( boolean b ) { lockdown = b; }
		public void setCrystalShard( boolean b ) { crystalShard = b; }
		public void setStunChance( int n ) { stunChance = n; }
		public void setStunAmount( int n ) { stunAmount = n; }

		public int getOwnerId() { return ownerId; }
		public int getSelfId() { return selfId; }
		public boolean isLockdown() { return lockdown; }
		public boolean isCrystalShard() { return crystalShard; }
		public int getStunChance() { return stunChance; }
		public int getStunAmount() { return stunAmount; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Hull Damage:       %7d\n", hullDamage));
			result.append(String.format("ShieldPiercing:    %7d\n", shieldPiercing));
			result.append(String.format("Fire Chance:       %7d\n", fireChance));
			result.append(String.format("Breach Chance:     %7d\n", breachChance));
			result.append(String.format("Ion Damage:        %7d\n", ionDamage));
			result.append(String.format("System Damage:     %7d\n", systemDamage));
			result.append(String.format("Personnel Damage:  %7d\n", personnelDamage));
			result.append(String.format("Hull Buster:       %7b (2x Hull damage vs systemless rooms)\n", hullBuster));
			result.append(String.format("OwnerId?:          %7d\n", ownerId));
			result.append(String.format("SelfId?:           %7d\n", selfId));
			result.append(String.format("Lockdown:          %7b\n", lockdown));
			result.append(String.format("Crystal Shard:     %7b\n", crystalShard));
			result.append(String.format("Stun Chance:       %7d\n", stunChance));
			result.append(String.format("Stun Amount:       %7d\n", stunAmount));

			return result.toString();
		}
	}



	// Extended info for drones.
	public static class UnknownAres {
		// ALPHA
		//
		// Alpha[0] decrements from just over 8406 (10000) during preiod of
		// post-destruction un-redeployability. After it passes 0, the value
		// lingers.
		//
		// Alpha[1] went from 1 (when a hacking drone was moving claw-upwards
		// toward the top, from the nearby ship that launched it) to 0 (after
		// it arrived at the player ship, claw down). Coordinate origin switch?
		//
		// 0000 0000 0100 0000 0100 0000 

		// In this sample 228x17 is the offset from the top-left corner of the
		// enemy ship's floorplan (the squares), to the center of a hacking
		// drone vessel.
		//
		// F67B 0300 6842 0000 (228342:17000)
		// F67B 0300 6842 0000 (228342:17000)
		// F67B 0300 6842 0000 (228342:17000)
		//
		// BETA
		// 0000 0080 (MIN_VALUE)
		// 0000 0080 (MIN_VALUE)
		// 0000 0080 0000 0080 (MIN_VALUE:MIN_VALUE)
		// 0000 0080 (MIN_VALUE)
		// 0000 0080 (MIN_VALUE)

		// GAMMA
		// F401 0000 (500)
		// 0000 0000 
		// 0000 0000 
		// F818 0300 (203000)
		// 0000 0000 
		// 38BB 0200 (179000)
		// FFFF FF7F (MAX_VALUE)
		// 0000 0000 0000 0000 0000 0000 
		// 18FC FFFF (-1000)
		// 0000 0000 

		// DELTA
		// 8BE4 FFFF 0000 0000 (-7029:0)
		// 0000 0000 0000 0000 0A00 0000 
		// 0000 0000 E803 0000 
		// 0000 0000 0000 0000 

		// Epsilon and Zeta vary with the droneBlueprint type.

		private DroneType droneType = null;
		private int[] unknownAlpha = new int[3];
		private int currentPosX=0, currentPosY=0;
		private int prevPosX=0, prevPosY=0;
		private int goalPosX=0, goalPosY=0;
		private int[] unknownBeta = new int[6];
		private int[] unknownGamma = new int[12];
		private int[] unknownDelta = new int[9];
		private List<Integer> unknownEpsilon = new ArrayList<Integer>();
		private List<Integer> unknownZeta = new ArrayList<Integer>();


		/**
		 * Constructs an incomplete Ares.
		 */
		public UnknownAres() {
		}

		public void setDroneType( DroneType droneType ) { this.droneType = droneType; }
		public DroneType getDroneType() { return droneType; }

		public void setUnknownAlpha( int index, int n ) { unknownAlpha[index] = n; }
		public int[] getUnknownAlpha() { return unknownAlpha; }

		public void setCurrentPositionX( int n ) { currentPosX = n; }
		public void setCurrentPositionY( int n ) { currentPosY = n; }
		public int getCurrentPositionX() { return currentPosX; }
		public int getCurrentPositionY() { return currentPosY; }

		public void setPreviousPositionX( int n ) { prevPosX = n; }
		public void setPreviousPositionY( int n ) { prevPosY = n; }
		public int getPreviousPositionX() { return prevPosX; }
		public int getPreviousPositionY() { return prevPosY; }

		public void setGoalPositionX( int n ) { goalPosX = n; }
		public void setGoalPositionY( int n ) { goalPosY = n; }
		public int getGoalPositionX() { return goalPosX; }
		public int getGoalPositionY() { return goalPosY; }

		public void setUnknownBeta( int index, int n ) { unknownBeta[index] = n; }
		public void setUnknownGamma( int index, int n ) { unknownGamma[index] = n; }
		public void setUnknownDelta( int index, int n ) { unknownDelta[index] = n; }

		public int[] getUnknownBeta() { return unknownBeta; }
		public int[] getUnknownGamma() { return unknownGamma; }
		public int[] getUnknownDelta() { return unknownDelta; }

		public void setUnknownEpsilon( List<Integer> epsilonList ) { unknownEpsilon = epsilonList; }
		public void setUnknownZeta( List<Integer> zetaList ) { unknownZeta = zetaList; }

		public List<Integer> getUnknownEpsilon() { return unknownEpsilon; }
		public List<Integer> getUnknownZeta() { return unknownZeta; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format("%d", n);
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append(String.format("Drone Type:        %s\n", droneType.getId()));

			result.append(String.format("\nAlpha?...\n"));
			result.append(String.format("%7s, %7s, %7s\n", prettyInt(unknownAlpha[0]), prettyInt(unknownAlpha[1]), prettyInt(unknownAlpha[2])));

			result.append("\n");
			result.append(String.format("Current Position:  %7s,%7s\n", prettyInt(currentPosX), prettyInt(currentPosY)));
			result.append(String.format("Previous Position: %7s,%7s\n", prettyInt(prevPosX), prettyInt(prevPosY)));
			result.append(String.format("Goal Position:     %7s,%7s\n", prettyInt(goalPosX), prettyInt(goalPosY)));

			result.append(String.format("\nBeta?...\n"));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownBeta[0]), prettyInt(unknownBeta[1])));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownBeta[2]), prettyInt(unknownBeta[3])));
			result.append(String.format("%7s, %7s\n", prettyInt(unknownBeta[4]), prettyInt(unknownBeta[5])));

			result.append(String.format("\nGamma?...\n"));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownGamma[0]), prettyInt(unknownGamma[1])));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownGamma[2]), prettyInt(unknownGamma[3])));
			result.append(String.format("%7s,\n", prettyInt(unknownGamma[4])));
			result.append(String.format("%7s, %7s, %7s,\n", prettyInt(unknownGamma[5]), prettyInt(unknownGamma[6]), prettyInt(unknownGamma[7])));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownGamma[8]), prettyInt(unknownGamma[9])));
			result.append(String.format("%7s, %7s\n", prettyInt(unknownGamma[10]), prettyInt(unknownGamma[11])));

			result.append(String.format("\nDelta?...\n"));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownDelta[0]), prettyInt(unknownDelta[1])));
			result.append(String.format("%7s, %7s, %7s,\n", prettyInt(unknownDelta[2]), prettyInt(unknownDelta[3]), prettyInt(unknownDelta[4])));
			result.append(String.format("%7s, %7s,\n", prettyInt(unknownDelta[5]), prettyInt(unknownDelta[6])));
			result.append(String.format("%7s, %7s\n", prettyInt(unknownDelta[7]), prettyInt(unknownDelta[8])));

			result.append(String.format("\nEpsilon?...\n"));
			for (int i=0; i < unknownEpsilon.size(); i++) {
				Integer eInt = unknownEpsilon.get( i );
				result.append(String.format("%7s", prettyInt(eInt.intValue())));

				if ( i != unknownEpsilon.size()-1 ) {
					if ( i % 2 == 1 ) {
						result.append(",\n");
					} else {
						result.append(", ");
					}
				}
			}
			result.append("\n");

			result.append(String.format("\nZeta?...\n"));
			for (int i=0; i < unknownZeta.size(); i++) {
				Integer zInt = unknownZeta.get( i );
				result.append(String.format("%7s", prettyInt(zInt.intValue())));

				if ( i != unknownZeta.size()-1 ) {
					if ( i % 3 == 2 ) {
						result.append(",\n");
					} else {
						result.append(", ");
					}
				}
			}
			result.append("\n");

			return result.toString();
		}
	}

	// A normal drone.
	public static class UnknownPolemos {
		private boolean deployed = false;
		private boolean armed = false;
		private UnknownAres unknownAres = null;


		public UnknownPolemos() {
		}

		public void setDeployed( boolean b ) { deployed = b; }
		public void setArmed( boolean b ) { armed = b; }

		public boolean isDeployed() { return deployed; }
		public boolean isArmed() { return armed; }

		/**
		 * Sets extended drone info, which varies by DroneType (even null sometimes).
		 */
		public void setUnknownAres( UnknownAres ares ) { unknownAres = ares; }
		public UnknownAres getUnknownAres() { return unknownAres; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("Deployed:        %5b\n", deployed));
			result.append(String.format("Armed:           %5b\n", armed));

			result.append("\nAres?...\n");
			if ( unknownAres != null ) {
				result.append(unknownAres.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			} else {
				result.append("N/A\n");
			}

			return result.toString();
		}
	}

	// A standalone surge drone.
	public static class UnknownEnyalius {
		private String droneId = null;
		private UnknownAres unknownAres = null;
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int unknownGamma = 0;


		/**
		 * Constructs an incomplete Enyalius.
		 */
		public UnknownEnyalius() {
		}

		public void setDroneId( String s ) { droneId = s; }
		public String getDroneId() { return droneId; }

		public void setUnknownAres( UnknownAres ares ) { unknownAres = ares; }
		public UnknownAres getUnknownAres() { return unknownAres; }

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public void setUnknownGamma( int n ) { unknownGamma = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }
		public int getUnknownGamma() { return unknownGamma; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append(String.format("DroneId:           %s\n", droneId));

			result.append("\nExtended Drone Info...\n");
			result.append(unknownAres.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

			result.append("\n");

			result.append(String.format("Alpha?:            %3d\n", unknownAlpha));
			result.append(String.format("Beta?:             %3d\n", unknownBeta));
			result.append(String.format("Gamma?:            %3d\n", unknownGamma));

			return result.toString();
		}
	}



	// Extended info for individual weapons.
	// Game states contain two lists of these objects: player and nearby ship.
	public static class UnknownAthena {
		private int cooldownTicks = 0;
		private int cooldownTicksGoal = 0;

		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int boost = 0;
		private int charge = 0;

		// These two lists hold identical values, often in duplicate.
		// Enemy ships only populate the latter?
		// Beam weapons use pairs to designate the line's endpoints.
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

		private List<ProjectileState> pendingProjectiles = new ArrayList<ProjectileState>();


		public UnknownAthena() {
		}

		/**
		 * Sets time elapsed waiting for the weapon to cool down.
		 *
		 * @param n a positive int less than, or equal to, the goal (0 when not armed)
		 *
		 * @see #setCooldownTicksGoal(int)
		 */
		public void setCooldownTicks( int n ) { cooldownTicks = n; }
		public int getCooldownTicks() { return cooldownTicks; }

		/**
		 * Sets total time needed for the weapon to cool down.
		 *
		 * This can vary depending on weapon features and situational factors.
		 *
		 * @see #setCooldownTicks(int)
		 */
		public void setCooldownTicksGoal( int n ) { cooldownTicksGoal = n; }
		public int getCooldownTicksGoal() { return cooldownTicksGoal; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }

		public int getUnknownGamma() { return unknownGamma; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets the boost level on a weapon whose cooldown decreases with
		 * consecutive shots.
		 *
		 * This is represented in-game as "Name +X".
		 * Example: LASER_CHAINGUN.
		 *
		 * @param number of consecutive shots, up to the blueprint's boost count limit, or 0
		 */
		public void setBoost( int n ) { boost = n; }
		public int getBoost() { return boost; }

		/**
		 * Sets the number of charges on a charge weapon, or 0.
		 *
		 * This is represented in-game as circles with dots.
		 * Example: ION_CHARGEGUN.
		 *
		 * Note: Modded WeaponBlueprints with "chargeLevels" greater than 7
		 * crash FTL.
		 *
		 * Note: Modded WeaponBlueprints with both the beam "type" and
		 * "chargeLevels" crash FTL.
		 */
		public void setCharge( int n ) { charge = n; }
		public int getCharge() { return charge; }

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

		/**
		 * Sets a list of queued projectiles about to be fired.
		 *
		 * This is often seen with laser burst weapons mid-volley, but any
		 * weapon will momentarily queue at least one projectile an instant
		 * before firing.
		 */
		public void setPendingProjectiles( List<ProjectileState> pendingProjectiles ) { this.pendingProjectiles = pendingProjectiles; }
		public List<ProjectileState> getPendingProjectiles() { return pendingProjectiles; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append(String.format("Cooldown Ticks:    %3d\n", cooldownTicks));
			result.append(String.format("Cooldown Goal:     %3d\n", cooldownTicksGoal));
			result.append(String.format("Gamma?:            %3d\n", unknownGamma));
			result.append(String.format("Delta?:            %3d\n", unknownDelta));
			result.append(String.format("Boost:             %3d\n", boost));
			result.append(String.format("Charge:            %3d\n", charge));

			result.append("\nEta?... (Reticle Coords?)\n");
			first = true;
			for ( UnknownEulabeia x : unknownEta ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(x.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\nTheta?... (Reticle Coords?)\n");
			first = true;
			for ( UnknownEulabeia x : unknownTheta ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(x.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

			result.append("\n");

			result.append(String.format("Iota?:             %3d (Autofire?)\n", unknownIota));
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
			result.append(String.format("Chi?:              %3d\n", unknownChi));

			result.append("\nPending Projectiles... (Queued before firing)\n");
			int projectileIndex = 0;
			first = true;
			for ( ProjectileState projectile : pendingProjectiles ) {
				if (first) { first = false; }
				else { result.append(",\n"); }
				result.append(String.format("Projectile # %2d:\n", projectileIndex++));
				result.append(projectile.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
			}

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



	private int readMinMaxedInt( InputStream in ) throws IOException {
		int n = readInt(in);

		if ( n == -2147483648 ) {
			n = Integer.MIN_VALUE;
		}
		else if ( n == 2147483647 ) {
			n = Integer.MAX_VALUE;
		}

		return n;
	}

	private void writeMinMaxedInt( OutputStream out, int n ) throws IOException {
		if ( n == Integer.MIN_VALUE ) {
			n = -2147483648;
		}
		else if ( n == Integer.MAX_VALUE ) {
			n = 2147483647;
		}

		writeInt( out, n );
	}


	private UnknownZeus readZeus( FileInputStream in, SavedGameState gameState ) throws IOException {
System.err.println(String.format("\nZeus: @%d", in.getChannel().position()));
		UnknownZeus zeus = new UnknownZeus();

		long fileSize = in.getChannel().size();

		int projectileCount = readInt(in);
		List<ProjectileState> projectileList = new ArrayList<ProjectileState>();
		for (int i=0; i < projectileCount; i++) {
			projectileList.add( readProjectile(in) );
		}
		zeus.setProjectileList( projectileList );

		UnknownHephaestus playerHephaestus = readHephaestus( in, gameState.getPlayerShipState() );
		zeus.setPlayerHephaestus( playerHephaestus );

		if ( gameState.getNearbyShipState() != null ) {
			UnknownHephaestus nearbyHephaestus = readHephaestus( in, gameState.getNearbyShipState() );
			zeus.setNearbyHephaestus( nearbyHephaestus );
		}

		zeus.setUnknownEpsilon( readInt(in) );

		if ( gameState.getNearbyShipState() != null ) {
			zeus.setUnknownZeta( new Integer(readInt(in)) );
		}

		zeus.setAutofire( readBool(in) );

		RebelFlagshipState flagship = new RebelFlagshipState();

		zeus.setUnknownEta( readInt(in) );
		flagship.setPendingStage( readInt(in) );

		zeus.setUnknownIota( readInt(in) );
		zeus.setUnknownKappa( readInt(in) );

		int flagshipOccupancyCount = readInt(in);
		for (int i=0; i < flagshipOccupancyCount; i++) {
			flagship.setPreviousOccupancy( i, readInt(in) );
		}

		gameState.setRebelFlagshipState( flagship );

		return zeus;
	}

	public void writeZeus( OutputStream out, SavedGameState gameState, UnknownZeus zeus ) throws IOException {
		writeInt( out, zeus.getProjectileList().size() );
		for ( ProjectileState projectile : zeus.getProjectileList() ) {
			writeProjectile( out, projectile );
		}

		writeHephaestus( out, zeus.getPlayerHephaestus(), gameState.getPlayerShipState() );

		if ( gameState.getNearbyShipState() != null ) {
			writeHephaestus( out, zeus.getNearbyHephaestus(), gameState.getNearbyShipState() );
		}

		writeInt( out, zeus.getUnknownEpsilon() );

		if ( gameState.getNearbyShipState() != null ) {
			writeInt( out, zeus.getUnknownZeta().intValue() );
		}

		writeBool( out, zeus.getAutofire() );

		RebelFlagshipState flagship = gameState.getRebelFlagshipState();

		writeInt( out, zeus.getUnknownEta() );
		writeInt( out, flagship.getPendingStage() );
		writeInt( out, zeus.getUnknownIota() );
		writeInt( out, zeus.getUnknownKappa() );

		writeInt( out, flagship.getOccupancyMap().size() );
		for (Map.Entry<Integer, Integer> entry : flagship.getOccupancyMap().entrySet()) {
			int occupantCount = entry.getValue().intValue();
			writeInt( out, occupantCount );
		}
	}

	private UnknownHephaestus readHephaestus( FileInputStream in, ShipState shipState ) throws IOException {
System.err.println(String.format("Hephaestus: @%d", in.getChannel().position()));
		UnknownHephaestus hephaestus = new UnknownHephaestus();

		// There is no explicit list count for drones.
		List<UnknownPolemos> polemosList = new ArrayList<UnknownPolemos>();

		for ( DroneState drone : shipState.getDroneList() ) {
			UnknownPolemos polemos = new UnknownPolemos();

			polemos.setDeployed( readBool(in) );
			polemos.setArmed( readBool(in) );

			String droneId = drone.getDroneId();
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneId );
			if ( droneBlueprint == null ) throw new IOException( "Unrecognized DroneBlueprint: "+ droneId );

			DroneType droneType = DroneType.findById( droneBlueprint.getType() );
			if ( droneType == null ) throw new IOException( String.format("DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType()) );

			polemosList.add( polemos );

			if ( DroneType.REPAIR.equals(droneType) ||
			     DroneType.BATTLE.equals(droneType) ) {
				continue;
			}

			UnknownAres ares = readAres( in, droneType );
			polemos.setUnknownAres( ares );
		}
		hephaestus.setPolemosList( polemosList );

		SystemState hackingState = shipState.getSystem( SystemType.HACKING );
		if ( hackingState != null && hackingState.getCapacity() > 0 ) {
			HackingInfo hackingInfo = new HackingInfo();

			hackingInfo.setUnknownAlpha( readInt(in) );
			hackingInfo.setUnknownBeta( readInt(in) );
			hackingInfo.setUnknownGamma( readInt(in) );
			hackingInfo.setUnknownDelta( readInt(in) );

			hackingInfo.setDisruptionTicks( readInt(in) );
			hackingInfo.setDisruptionTicksGoal( readInt(in) );

			hackingInfo.setUnknownTheta( readInt(in) );  // TODO: Bool, disruption active?

			UnknownAres ares = readAres( in, DroneType.HACKING );  // The hacking drone.
			hackingInfo.setUnknownAres( ares );

			List<Integer> iotaList = new ArrayList<Integer>();
			for (int i=0; i < 7; i++) {
				iotaList.add( readInt(in) );
			}
			hackingInfo.setUnknownIota( iotaList );

			List<Integer> kappaList = new ArrayList<Integer>();
			for (int i=0; i < 7; i++) {
				kappaList.add( readInt(in) );
			}
			hackingInfo.setUnknownKappa( kappaList );

			hackingInfo.setUnknownLambda( readInt(in) );
			hackingInfo.setUnknownMu( readInt(in) );

			hephaestus.addExtendedSystemInfo( hackingInfo );
		}

		SystemState mindState = shipState.getSystem( SystemType.MIND );
		if ( mindState != null && mindState.getCapacity() > 0 ) {
			MindInfo mindInfo = new MindInfo();

			mindInfo.setMindControlTicks( readInt(in) );
			mindInfo.setMindControlTicksGoal( readInt(in) );

			hephaestus.addExtendedSystemInfo( mindInfo );
		}

		SystemState weaponsState = shipState.getSystem( SystemType.WEAPONS );
		if ( weaponsState != null && weaponsState.getCapacity() > 0 ) {

			//int playerWeaponCount = shipState.getWeaponList().size();
			int weaponCount = readInt(in);
			List<UnknownAthena> athenaList = new ArrayList<UnknownAthena>();
			for (int i=0; i < weaponCount; i++) {
				athenaList.add( readAthena(in) );
			}
			hephaestus.setAthenaList( athenaList );
		}

		// Get ALL artillery rooms' SystemStates from the ShipState.
		List<SystemState> artilleryStateList = shipState.getSystems( SystemType.ARTILLERY );
		for ( SystemState artilleryState : artilleryStateList ) {

			if ( artilleryState.getCapacity() > 0 ) {
				ArtilleryInfo artilleryInfo = new ArtilleryInfo();

				artilleryInfo.setUnknownAthena( readAthena(in) );

				hephaestus.addExtendedSystemInfo( artilleryInfo );
			}
		}

		// A list of standalone drones, for flagship swarms. Always 0 for player.

		int enyaliusCount = readInt(in);
		List<UnknownEnyalius> enyaliusList = new ArrayList<UnknownEnyalius>();
		for (int i=0; i < enyaliusCount; i++) {
			String droneId = readString(in);
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneId );
			if ( droneBlueprint == null ) throw new IOException( "Unrecognized DroneBlueprint: "+ droneId );

			UnknownEnyalius enyalius = new UnknownEnyalius();
			enyalius.setDroneId( droneId );

			DroneType droneType = DroneType.findById( droneBlueprint.getType() );
			if ( droneType == null ) throw new IOException( String.format("DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType()) );

			UnknownAres ares = readAres( in, droneType );
			enyalius.setUnknownAres( ares );

			enyalius.setUnknownAlpha( readInt(in) );
			enyalius.setUnknownBeta( readInt(in) );
			enyalius.setUnknownGamma( readInt(in) );

			enyaliusList.add( enyalius );
		}
		hephaestus.setEnyaliusList( enyaliusList );

		return hephaestus;
	}

	public void writeHephaestus( OutputStream out, UnknownHephaestus hephaestus, ShipState shipState ) throws IOException {
		// There is no explicit list count for drones.
		for ( UnknownPolemos polemos : hephaestus.getPolemosList() ) {
			writeBool( out, polemos.isDeployed() );
			writeBool( out, polemos.isArmed() );

			if ( polemos.getUnknownAres() != null ) {
				writeAres( out, polemos.getUnknownAres() );
			}
		}

		SystemState hackingState = shipState.getSystem( SystemType.HACKING );
		if ( hackingState != null && hackingState.getCapacity() > 0 ) {
			// TODO: Compare system room count with extended info count.

			List<HackingInfo> hackingInfoList = hephaestus.getExtendedSystemInfoList( HackingInfo.class );
			for ( HackingInfo hackingInfo : hackingInfoList ) {
				writeInt( out, hackingInfo.getUnknownAlpha() );
				writeInt( out, hackingInfo.getUnknownBeta() );
				writeInt( out, hackingInfo.getUnknownGamma() );
				writeInt( out, hackingInfo.getUnknownDelta() );

				writeInt( out, hackingInfo.getDisruptionTicks() );
				writeInt( out, hackingInfo.getDisruptionTicksGoal() );

				writeInt( out, hackingInfo.getUnknownTheta() );

				writeAres( out, hackingInfo.getUnknownAres() );

				for ( Integer iotaInt : hackingInfo.getUnknownIota() ) {
					writeInt( out, iotaInt.intValue() );
				}

				for ( Integer kappaInt : hackingInfo.getUnknownKappa() ) {
					writeInt( out, kappaInt.intValue() );
				}

				writeInt( out, hackingInfo.getUnknownLambda() );
				writeInt( out, hackingInfo.getUnknownMu() );
			}
		}

		List<MindInfo> mindInfoList = hephaestus.getExtendedSystemInfoList( MindInfo.class );
		for ( MindInfo mindInfo : mindInfoList ) {
			writeInt( out, mindInfo.getMindControlTicks() );
			writeInt( out, mindInfo.getMindControlTicksGoal() );
		}

		// If there's a Weapons system, write the weapon Athenas (even if there are 0 of them).
		SystemState weaponsState = shipState.getSystem( SystemType.WEAPONS );
		if ( weaponsState != null && weaponsState.getCapacity() > 0 ) {
			writeInt( out, hephaestus.getAthenaList().size() );
			for ( UnknownAthena athena : hephaestus.getAthenaList() ) {
				writeAthena( out, athena );
			}
		}

		List<ArtilleryInfo> artilleryInfoList = hephaestus.getExtendedSystemInfoList( ArtilleryInfo.class );
		for ( ArtilleryInfo artilleryInfo : artilleryInfoList ) {
			writeAthena( out, artilleryInfo.getUnknownAthena() );
		}

		writeInt( out, hephaestus.getEnyaliusList().size() );
		for ( UnknownEnyalius enyalius : hephaestus.getEnyaliusList() ) {
			writeString( out, enyalius.getDroneId() );

			writeAres( out, enyalius.getUnknownAres() );

			writeInt( out, enyalius.getUnknownAlpha() );
			writeInt( out, enyalius.getUnknownBeta() );
			writeInt( out, enyalius.getUnknownGamma() );
		}
	}

	private UnknownAres readAres( FileInputStream in, DroneType droneType ) throws IOException {
		if ( droneType == null ) throw new IllegalArgumentException( "DroneType cannot be null." );
System.err.println(String.format("Ares: @%d", in.getChannel().position()));

		UnknownAres ares = new UnknownAres();
		ares.setDroneType( droneType );

		for (int i=0; i < 3; i++) {
			ares.setUnknownAlpha( i, readMinMaxedInt(in) );
		}

		ares.setCurrentPositionX( readMinMaxedInt(in) );
		ares.setCurrentPositionY( readMinMaxedInt(in) );
		ares.setPreviousPositionX( readMinMaxedInt(in) );
		ares.setPreviousPositionY( readMinMaxedInt(in) );
		ares.setGoalPositionX( readMinMaxedInt(in) );
		ares.setGoalPositionY( readMinMaxedInt(in) );

		for (int i=0; i < 6; i++) {
			ares.setUnknownBeta( i, readMinMaxedInt(in) );
		}
		for (int i=0; i < 12; i++) {
			ares.setUnknownGamma( i, readMinMaxedInt(in) );
		}

		for (int i=0; i < 9; i++) {
			ares.setUnknownDelta( i, readMinMaxedInt(in) );
		}

		List<Integer> epsilonList = new ArrayList<Integer>();
		List<Integer> zetaList = new ArrayList<Integer>();

		if ( DroneType.BOARDER.equals(droneType) ) {
			for (int i=0; i < 5; i++) {
				epsilonList.add( new Integer(readMinMaxedInt(in)) );
			}
			for (int i=0; i < 4; i++) {
				zetaList.add( new Integer(readMinMaxedInt(in)) );
			}
		}
		else if ( DroneType.HACKING.equals(droneType) ) {
			for (int i=0; i < 2; i++) {
				zetaList.add( new Integer(readMinMaxedInt(in)) );
			}
		}
		else if ( DroneType.COMBAT.equals(droneType) || 
		          DroneType.BEAM.equals(droneType) ) {

			for (int i=0; i < 5; i++) {
				epsilonList.add( new Integer(readMinMaxedInt(in)) );
			}
		}
		else if ( DroneType.DEFENSE.equals(droneType) ) {
			// Nothing extra.
		}
		else if ( DroneType.SHIELD.equals(droneType) ) {
			for (int i=0; i < 1; i++) {
				epsilonList.add( new Integer(readMinMaxedInt(in)) );  // TODO: Recharge ticks (-1000, or incrementing to maybe 3000/4000/8000)?
			}
		}
		else if ( DroneType.SHIP_REPAIR.equals(droneType) ) {
			for (int i=0; i < 5; i++) {
				epsilonList.add( new Integer(readMinMaxedInt(in)) );
			}
		}

		ares.setUnknownEpsilon( epsilonList );
		ares.setUnknownZeta( zetaList );

		return ares;
	}

	public void writeAres( OutputStream out, UnknownAres ares ) throws IOException {
		for ( int n : ares.getUnknownAlpha() ) {
			writeMinMaxedInt( out, n );
		}

		writeMinMaxedInt( out, ares.getCurrentPositionX() );
		writeMinMaxedInt( out, ares.getCurrentPositionY() );
		writeMinMaxedInt( out, ares.getPreviousPositionX() );
		writeMinMaxedInt( out, ares.getPreviousPositionY() );
		writeMinMaxedInt( out, ares.getGoalPositionX() );
		writeMinMaxedInt( out, ares.getGoalPositionY() );

		for ( int n : ares.getUnknownBeta() ) {
			writeMinMaxedInt( out, n );
		}

		for ( int n : ares.getUnknownGamma() ) {
			writeMinMaxedInt( out, n );
		}

		for ( int n : ares.getUnknownDelta() ) {
			writeMinMaxedInt( out, n );
		}

		for ( Integer epsilonInt : ares.getUnknownEpsilon() ) {
			writeMinMaxedInt( out, epsilonInt.intValue() );
		}

		for ( Integer zetaInt : ares.getUnknownZeta() ) {
			writeMinMaxedInt( out, zetaInt.intValue() );
		}
	}

	private UnknownAthena readAthena( FileInputStream in ) throws IOException {
System.err.println(String.format("Athena: @%d", in.getChannel().position()));
		UnknownAthena athena = new UnknownAthena();

		athena.setCooldownTicks( readInt(in) );
		athena.setCooldownTicksGoal( readInt(in) );
		athena.setUnknownGamma( readInt(in) );
		athena.setUnknownDelta( readInt(in) );
		athena.setBoost( readInt(in) );
		athena.setCharge( readInt(in) );

		int etaCount = readInt(in);
		List<UnknownEulabeia> etaList = new ArrayList<UnknownEulabeia>();
		for (int i=0; i < etaCount; i++) {
			etaList.add( readEulabeia( in ) );
		}
		athena.setUnknownEta( etaList );

		int thetaCount = readInt(in);
		List<UnknownEulabeia> thetaList = new ArrayList<UnknownEulabeia>();
		for (int i=0; i < thetaCount; i++) {
			thetaList.add( readEulabeia( in ) );
		}
		athena.setUnknownTheta( thetaList );

		athena.setUnknownIota( readInt(in) );
		athena.setUnknownKappa( readInt(in) );
		athena.setUnknownLambda( readInt(in) );
		athena.setUnknownMu( readInt(in) );
		athena.setUnknownNu( readInt(in) );
		athena.setUnknownXi( readInt(in) );
		athena.setUnknownOmicron( readInt(in) );
		athena.setUnknownPi( readInt(in) );
		athena.setUnknownRho( readInt(in) );
		athena.setUnknownSigma( readInt(in) );
		athena.setUnknownTau( readInt(in) );
		athena.setUnknownUpsilon( readInt(in) );
		athena.setUnknownPhi( readInt(in) );
		athena.setUnknownChi( readInt(in) );

		int pendingProjectilesCount = readInt(in);
		List<ProjectileState> pendingProjectiles = new ArrayList<ProjectileState>();
		for ( int i=0; i < pendingProjectilesCount; i++ ) {
			pendingProjectiles.add( readProjectile(in) );
		}
		athena.setPendingProjectiles( pendingProjectiles );

		return athena;
	}

	public void writeAthena( OutputStream out, UnknownAthena athena ) throws IOException {
		writeInt( out, athena.getCooldownTicks() );
		writeInt( out, athena.getCooldownTicksGoal() );
		writeInt( out, athena.getUnknownGamma() );
		writeInt( out, athena.getUnknownDelta() );
		writeInt( out, athena.getBoost() );
		writeInt( out, athena.getCharge() );

		writeInt( out, athena.getUnknownEta().size() );
		for ( UnknownEulabeia eulabeia : athena.getUnknownEta() ) {
			writeEulabeia( out, eulabeia );
		}

		writeInt( out, athena.getUnknownTheta().size() );
		for ( UnknownEulabeia eulabeia : athena.getUnknownTheta() ) {
			writeEulabeia( out, eulabeia );
		}

		writeInt( out, athena.getUnknownIota() );
		writeInt( out, athena.getUnknownKappa() );
		writeInt( out, athena.getUnknownLambda() );
		writeInt( out, athena.getUnknownMu() );
		writeInt( out, athena.getUnknownNu() );
		writeInt( out, athena.getUnknownXi() );
		writeInt( out, athena.getUnknownOmicron() );
		writeInt( out, athena.getUnknownPi() );
		writeInt( out, athena.getUnknownRho() );
		writeInt( out, athena.getUnknownSigma() );
		writeInt( out, athena.getUnknownTau() );
		writeInt( out, athena.getUnknownUpsilon() );
		writeInt( out, athena.getUnknownPhi() );
		writeInt( out, athena.getUnknownChi() );

		writeInt( out, athena.getPendingProjectiles().size() );
		for ( ProjectileState projectile : athena.getPendingProjectiles() ) {
			writeProjectile( out, projectile );
		}
	}

	private UnknownEulabeia readEulabeia( FileInputStream in ) throws IOException {
		UnknownEulabeia eulabeia = new UnknownEulabeia();

		eulabeia.setUnknownAlpha( readInt(in) );
		eulabeia.setUnknownBeta( readInt(in) );

		return eulabeia;
	}

	public void writeEulabeia( OutputStream out, UnknownEulabeia eulabeia ) throws IOException {
		writeInt( out, eulabeia.getUnknownAlpha() );
		writeInt( out, eulabeia.getUnknownBeta() );
	}
}
