// Variables with unknown meanings are named with greek letters.
// Classes for unknown objects are named after deities.
// http://en.wikipedia.org/wiki/List_of_Greek_mythological_figures#Personified_concepts

// For reference on weapons and projectiles, see the "Complete Weapon Attribute Table":
// https://subsetgames.com/forum/viewtopic.php?f=12&t=24600


package net.blerf.ftl.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.constants.OriginalFTLConstants;
import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;
import net.blerf.ftl.model.XYPair;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.MysteryBytes;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;


public class SavedGameParser extends Parser {

	private static final Logger log = LoggerFactory.getLogger( SavedGameParser.class );


	public SavedGameParser() {
	}

	public SavedGameState readSavedGame( File savFile ) throws IOException {
		SavedGameState gameState = null;

		FileInputStream in = null;
		try {
			in = new FileInputStream( savFile );
			gameState = readSavedGame( in );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		return gameState;
	}

	public SavedGameState readSavedGame( FileInputStream in ) throws IOException {
		InputStream layoutStream = null;
		try {
			SavedGameState gameState = new SavedGameState();

			int fileFormat = readInt( in );
			gameState.setFileFormat( fileFormat );

			// FTL 1.6.1 introduced UTF-8 strings.
			super.setUnicode( fileFormat >= 11 );

			if ( fileFormat == 11 ) {
				gameState.setRandomNative( readBool( in ) );
			} else {
				gameState.setRandomNative( true );  // Always native before FTL 1.6.1.
			}

			if ( fileFormat == 2 ) {
				// FTL 1.03.3 and earlier.
				gameState.setDLCEnabled( false );  // Not present before FTL 1.5.4.
			}
			else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				// FTL 1.5.4-1.5.10, 1.5.12, 1.5.13, or 1.6.1.
				gameState.setDLCEnabled( readBool( in ) );
			}
			else {
				throw new IOException( String.format( "Unexpected first byte (%d) for a SAVED GAME.", fileFormat ) );
			}

			int diffFlag = readInt( in );
			Difficulty diff;
			if ( diffFlag == 0 ) {
				diff = Difficulty.EASY;
			}
			else if ( diffFlag == 1 ) {
				diff = Difficulty.NORMAL;
			}
			else if ( diffFlag == 2 && ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) ) {
				diff = Difficulty.HARD;
			}
			else {
				throw new IOException( String.format( "Unsupported difficulty flag for saved game: %d", diffFlag ) );
			}

			gameState.setDifficulty( diff );
			gameState.setTotalShipsDefeated( readInt( in ) );
			gameState.setTotalBeaconsExplored( readInt( in ) );
			gameState.setTotalScrapCollected( readInt( in ) );
			gameState.setTotalCrewHired( readInt( in ) );

			String playerShipName = readString( in );         // Redundant.
			gameState.setPlayerShipName( playerShipName );

			String playerShipBlueprintId = readString( in );  // Redundant.
			gameState.setPlayerShipBlueprintId( playerShipBlueprintId );

			int oneBasedSectorNumber = readInt( in );  // Redundant.

			// Always 0?
			gameState.setUnknownBeta( readInt( in ) );

			int stateVarCount = readInt( in );
			for ( int i=0; i < stateVarCount; i++ ) {
				String stateVarId = readString( in );
				Integer stateVarValue = readInt( in );
				gameState.setStateVar( stateVarId, stateVarValue );
			}

			ShipState playerShipState = readShip( in, false, fileFormat, gameState.isDLCEnabled() );
			gameState.setPlayerShip( playerShipState );

			// Nearby ships have no cargo, so this isn't in readShip().
			int cargoCount = readInt( in );
			for ( int i=0; i < cargoCount; i++ ) {
				gameState.addCargoItemId( readString( in ) );
			}

			gameState.setSectorTreeSeed( readInt( in ) );

			gameState.setSectorLayoutSeed( readInt( in ) );

			gameState.setRebelFleetOffset( readInt( in ) );

			gameState.setRebelFleetFudge( readInt( in ) );

			gameState.setRebelPursuitMod( readInt( in ) );

			if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				gameState.setCurrentBeaconId( readInt( in ) );

				gameState.setWaiting( readBool( in ) );
				gameState.setWaitEventSeed( readInt( in ) );
				gameState.setUnknownEpsilon( readString( in ) );
				gameState.setSectorHazardsVisible( readBool( in ) );
				gameState.setRebelFlagshipVisible( readBool( in ) );
				gameState.setRebelFlagshipHop( readInt( in ) );
				gameState.setRebelFlagshipMoving( readBool( in ) );
				gameState.setRebelFlagshipRetreating( readBool( in ) );
				gameState.setRebelFlagshipBaseTurns( readInt( in ) );
			}
			else if ( fileFormat == 2 ) {
				gameState.setSectorHazardsVisible( readBool( in ) );

				gameState.setRebelFlagshipVisible( readBool( in ) );

				gameState.setRebelFlagshipHop( readInt( in ) );

				gameState.setRebelFlagshipMoving( readBool( in ) );
			}

			int sectorVisitationCount = readInt( in );
			List<Boolean> route = new ArrayList<Boolean>();
			for ( int i=0; i < sectorVisitationCount; i++ ) {
				route.add( readBool( in ) );
			}
			gameState.setSectorVisitation( route );

			int sectorNumber = readInt( in );
			gameState.setSectorNumber( sectorNumber );

			gameState.setSectorIsHiddenCrystalWorlds( readBool( in ) );

			int beaconCount = readInt( in );
			for ( int i=0; i < beaconCount; i++ ) {
				gameState.addBeacon( readBeacon( in, fileFormat ) );
			}

			int questEventCount = readInt( in );
			for ( int i=0; i < questEventCount; i++ ) {
				String questEventId = readString( in );
				int questBeaconId = readInt( in );
				gameState.addQuestEvent( questEventId, questBeaconId );
			}

			int distantQuestEventCount = readInt( in );
			for ( int i=0; i < distantQuestEventCount; i++ ) {
				String distantQuestEventId = readString( in );
				gameState.addDistantQuestEvent( distantQuestEventId );
			}

			if ( fileFormat == 2 ) {
				gameState.setCurrentBeaconId( readInt( in ) );

				boolean shipNearby = readBool( in );
				if ( shipNearby ) {
					ShipState nearbyShipState = readShip( in, true, fileFormat, gameState.isDLCEnabled() );
					gameState.setNearbyShip( nearbyShipState );
				}

				RebelFlagshipState flagshipState = readRebelFlagship( in );
				gameState.setRebelFlagshipState( flagshipState );
			}
			else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				// Current beaconId was set earlier.

				gameState.setUnknownMu( readInt( in ) );

				EncounterState encounter = readEncounter( in, fileFormat );
				gameState.setEncounter( encounter );

				boolean shipNearby = readBool( in );
				if ( shipNearby ) {
					gameState.setRebelFlagshipNearby( readBool( in ) );

					ShipState nearbyShipState = readShip( in, true, fileFormat, gameState.isDLCEnabled() );
					gameState.setNearbyShip( nearbyShipState );

					gameState.setNearbyShipAI( readNearbyShipAI( in ) );
				}

				gameState.setEnvironment( readEnvironment( in ) );

				// Flagship state is set much later.

				int projectileCount = readInt( in );
				for ( int i=0; i < projectileCount; i++ ) {
					gameState.addProjectile( readProjectile( in, fileFormat ) );
				}

				readExtendedShipInfo( in, gameState.getPlayerShip(), fileFormat );

				if ( gameState.getNearbyShip() != null ) {
					readExtendedShipInfo( in, gameState.getNearbyShip(), fileFormat );
				}

				gameState.setUnknownNu( readInt( in ) );

				if ( gameState.getNearbyShip() != null ) {
					gameState.setUnknownXi( readInt( in ) );
				}

				gameState.setAutofire( readBool( in ) );

				RebelFlagshipState flagship = new RebelFlagshipState();

				flagship.setUnknownAlpha( readInt( in ) );
				flagship.setPendingStage( readInt( in ) );
				flagship.setUnknownGamma( readInt( in ) );
				flagship.setUnknownDelta( readInt( in ) );

				int flagshipOccupancyCount = readInt( in );
				for ( int i=0; i < flagshipOccupancyCount; i++ ) {
					flagship.setPreviousOccupancy( i, readInt( in ) );
				}

				gameState.setRebelFlagshipState( flagship );
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

		int fileFormat = gameState.getFileFormat();
		writeInt( out, fileFormat );

		// FTL 1.6.1 introduced UTF-8 strings.
		super.setUnicode( fileFormat >= 11 );

		if ( fileFormat == 11 ) {
			writeBool( out, gameState.isRandomNative() );
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeBool( out, gameState.isDLCEnabled() );
		}
		else {
			throw new IOException( "Unsupported fileFormat: "+ fileFormat );
		}

		int diffFlag = 0;
		if ( gameState.getDifficulty() == Difficulty.EASY ) {
			diffFlag = 0;
		}
		else if ( gameState.getDifficulty() == Difficulty.NORMAL ) {
			diffFlag = 1;
		}
		else if ( gameState.getDifficulty() == Difficulty.HARD && ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) ) {
			diffFlag = 2;
		}
		else {
			log.warn( String.format( "Substituting EASY for unsupported difficulty for saved game: %s", gameState.getDifficulty().toString() ) );
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
		for ( Map.Entry<String, Integer> entry : gameState.getStateVars().entrySet() ) {
			writeString( out, entry.getKey() );
			writeInt( out, entry.getValue().intValue() );
		}

		writeShip( out, gameState.getPlayerShip(), fileFormat );

		writeInt( out, gameState.getCargoIdList().size() );
		for ( String cargoItemId : gameState.getCargoIdList() ) {
			writeString( out, cargoItemId );
		}

		writeInt( out, gameState.getSectorTreeSeed() );
		writeInt( out, gameState.getSectorLayoutSeed() );
		writeInt( out, gameState.getRebelFleetOffset() );
		writeInt( out, gameState.getRebelFleetFudge() );
		writeInt( out, gameState.getRebelPursuitMod() );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, gameState.getCurrentBeaconId() );

			writeBool( out, gameState.isWaiting() );
			writeInt( out, gameState.getWaitEventSeed() );
			writeString( out, gameState.getUnknownEpsilon() );
			writeBool( out, gameState.areSectorHazardsVisible() );
			writeBool( out, gameState.isRebelFlagshipVisible() );
			writeInt( out, gameState.getRebelFlagshipHop() );
			writeBool( out, gameState.isRebelFlagshipMoving() );
			writeBool( out, gameState.isRebelFlagshipRetreating() );
			writeInt( out, gameState.getRebelFlagshipBaseTurns() );
		}
		else if ( fileFormat == 2 ) {
			writeBool( out, gameState.areSectorHazardsVisible() );
			writeBool( out, gameState.isRebelFlagshipVisible() );
			writeInt( out, gameState.getRebelFlagshipHop() );
			writeBool( out, gameState.isRebelFlagshipMoving() );
		}

		writeInt( out, gameState.getSectorVisitation().size() );
		for ( Boolean visited : gameState.getSectorVisitation() ) {
			writeBool( out, visited.booleanValue() );
		}

		writeInt( out, gameState.getSectorNumber() );
		writeBool( out, gameState.isSectorHiddenCrystalWorlds() );

		writeInt( out, gameState.getBeaconList().size() );
		for ( BeaconState beacon : gameState.getBeaconList() ) {
			writeBeacon( out, beacon, fileFormat );
		}

		writeInt( out, gameState.getQuestEventMap().size() );
		for ( Map.Entry<String, Integer> entry : gameState.getQuestEventMap().entrySet() ) {
			writeString( out, entry.getKey() );
			writeInt( out, entry.getValue().intValue() );
		}

		writeInt( out, gameState.getDistantQuestEventList().size() );
		for ( String questEventId : gameState.getDistantQuestEventList() ) {
			writeString( out, questEventId );
		}

		if ( fileFormat == 2 ) {
			writeInt( out, gameState.getCurrentBeaconId() );

			ShipState nearbyShip = gameState.getNearbyShip();
			writeBool( out, (nearbyShip != null) );
			if ( nearbyShip != null ) {
				writeShip( out, nearbyShip, fileFormat );
			}

			writeRebelFlagship( out, gameState.getRebelFlagshipState() );
		}
		else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			// Current beaconId was set earlier.

			writeInt( out, gameState.getUnknownMu() );

			writeEncounter( out, gameState.getEncounter(), fileFormat );

			ShipState nearbyShip = gameState.getNearbyShip();
			writeBool( out, (nearbyShip != null) );
			if ( nearbyShip != null ) {
				writeBool( out, gameState.isRebelFlagshipNearby() );

				writeShip( out, nearbyShip, fileFormat );

				writeNearbyShipAI( out, gameState.getNearbyShipAI() );
			}

			writeEnvironment( out, gameState.getEnvironment() );

			// Flagship state is set much later.

			writeInt( out, gameState.getProjectileList().size() );
			for ( ProjectileState projectile : gameState.getProjectileList() ) {
				writeProjectile( out, projectile, fileFormat );
			}

			writeExtendedShipInfo( out, gameState.getPlayerShip(), fileFormat );

			if ( gameState.getNearbyShip() != null ) {
				writeExtendedShipInfo( out, gameState.getNearbyShip(), fileFormat );
			}

			writeInt( out, gameState.getUnknownNu() );

			if ( gameState.getNearbyShip() != null ) {
				writeInt( out, gameState.getUnknownXi().intValue() );
			}

			writeBool( out, gameState.getAutofire() );

			RebelFlagshipState flagship = gameState.getRebelFlagshipState();

			writeInt( out, flagship.getUnknownAlpha() );
			writeInt( out, flagship.getPendingStage() );
			writeInt( out, flagship.getUnknownGamma() );
			writeInt( out, flagship.getUnknownDelta() );

			writeInt( out, flagship.getOccupancyMap().size() );
			for (Map.Entry<Integer, Integer> entry : flagship.getOccupancyMap().entrySet()) {
				int occupantCount = entry.getValue().intValue();
				writeInt( out, occupantCount );
			}
		}
	}

	private ShipState readShip( InputStream in, boolean auto, int fileFormat, boolean dlcEnabled ) throws IOException {

		String shipBlueprintId = readString( in );
		String shipName = readString( in );
		String shipGfxBaseName = readString( in );

		ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBlueprintId );
		if ( shipBlueprint == null ) {
			throw new RuntimeException( String.format( "Could not find blueprint for%s ship: %s", (auto ? " auto" : ""), shipName ) );
		}

		String shipLayoutId = shipBlueprint.getLayoutId();

		// Use this for room and door info later.
		ShipLayout shipLayout = DataManager.get().getShipLayout( shipLayoutId );
		if ( shipLayout == null ) {
			throw new RuntimeException( String.format( "Could not find layout for%s ship: %s", (auto ? " auto" : ""), shipName ) );
		}

		ShipState shipState = new ShipState( shipName, shipBlueprintId, shipLayoutId, shipGfxBaseName, auto );

		int startingCrewCount = readInt( in );
		for ( int i=0; i < startingCrewCount; i++ ) {
			shipState.addStartingCrewMember( readStartingCrewMember( in ) );
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			shipState.setHostile( readBool( in ) );
			shipState.setJumpChargeTicks( readInt( in ) );
			shipState.setJumping( readBool( in ) );
			shipState.setJumpAnimTicks( readInt( in ) );
		}

		shipState.setHullAmt( readInt( in ) );
		shipState.setFuelAmt( readInt( in ) );
		shipState.setDronePartsAmt( readInt( in ) );
		shipState.setMissilesAmt( readInt( in ) );
		shipState.setScrapAmt( readInt( in ) );

		int crewCount = readInt( in );
		for ( int i=0; i < crewCount; i++ ) {
			shipState.addCrewMember( readCrewMember( in, fileFormat ) );
		}

		// System info is stored in this order.
		List<SystemType> systemTypes = new ArrayList<SystemType>();
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
		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			systemTypes.add( SystemType.BATTERY );
			systemTypes.add( SystemType.CLONEBAY );
			systemTypes.add( SystemType.MIND );
			systemTypes.add( SystemType.HACKING );
		}

		shipState.setReservePowerCapacity( readInt( in ) );
		for ( SystemType systemType : systemTypes ) {
			shipState.addSystem( readSystem( in, systemType, fileFormat ) );

			// Systems that exist in multiple rooms have additional SystemStates.
			// Example: Flagship's artillery.
			//
			// In FTL 1.01-1.03.3 the flagship wasn't a nearby ship outside of combat,
			// So this never occurred. TODO: Confirm reports that 1.5.4 allows
			// multi-room systems on regular ships and check the editor's
			// compatibility.

			ShipBlueprint.SystemList.SystemRoom[] rooms = shipBlueprint.getSystemList().getSystemRoom( systemType );
			if ( rooms != null && rooms.length > 1 ) {
				for ( int q=1; q < rooms.length; q++ ) {
					shipState.addSystem( readSystem( in, systemType, fileFormat ) );
				}
			}
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {

			SystemState tmpSystem = null;

			tmpSystem = shipState.getSystem( SystemType.CLONEBAY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				ClonebayInfo clonebayInfo = new ClonebayInfo();

				clonebayInfo.setBuildTicks( readInt( in ) );
				clonebayInfo.setBuildTicksGoal( readInt( in ) );
				clonebayInfo.setDoomTicks( readInt( in ) );

				shipState.addExtendedSystemInfo( clonebayInfo );
			}
			tmpSystem = shipState.getSystem( SystemType.BATTERY );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				BatteryInfo batteryInfo = new BatteryInfo();

				batteryInfo.setActive( readBool( in ) );
				batteryInfo.setUsedBattery( readInt( in ) );
				batteryInfo.setDischargeTicks( readInt( in ) );

				shipState.addExtendedSystemInfo( batteryInfo );
			}

			// The shields info always exists, even if the shields system doesn't.
			if ( true ) {
				ShieldsInfo shieldsInfo = new ShieldsInfo();

				shieldsInfo.setShieldLayers( readInt( in ) );
				shieldsInfo.setEnergyShieldLayers( readInt( in ) );
				shieldsInfo.setEnergyShieldMax( readInt( in ) );
				shieldsInfo.setShieldRechargeTicks( readInt( in ) );

				shieldsInfo.setShieldDropAnimOn( readBool( in ) );
				shieldsInfo.setShieldDropAnimTicks( readInt( in ) );    // TODO: Confirm.

				shieldsInfo.setShieldRaiseAnimOn( readBool( in ) );
				shieldsInfo.setShieldRaiseAnimTicks( readInt( in ) );   // TODO: Confirm.

				shieldsInfo.setEnergyShieldAnimOn( readBool( in ) );
				shieldsInfo.setEnergyShieldAnimTicks( readInt( in ) );  // TODO: Confirm.

				// A pair. Usually noise. Sometimes 0.
				shieldsInfo.setUnknownLambda( readInt( in ) );   // TODO: Confirm: Shield down point X.
				shieldsInfo.setUnknownMu( readInt( in ) );       // TODO: Confirm: Shield down point Y.

				shipState.addExtendedSystemInfo( shieldsInfo );
			}

			tmpSystem = shipState.getSystem( SystemType.CLOAKING );
			if ( tmpSystem != null && tmpSystem.getCapacity() > 0 ) {
				CloakingInfo cloakingInfo = new CloakingInfo();

				cloakingInfo.setUnknownAlpha( readInt( in ) );
				cloakingInfo.setUnknownBeta( readInt( in ) );
				cloakingInfo.setCloakTicksGoal( readInt( in ) );
				cloakingInfo.setCloakTicks( readMinMaxedInt( in ) );

				shipState.addExtendedSystemInfo( cloakingInfo );
			}

			// Other ExtendedSystemInfo may be added to the ship later (FTL 1.5.4+).
		}

		// Room states are stored in roomId order.
		int roomCount = shipLayout.getRoomCount();
		for ( int r=0; r < roomCount; r++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( r );

			shipState.addRoom( readRoom( in, layoutRoom.squaresH, layoutRoom.squaresV, fileFormat ) );
		}

		int breachCount = readInt( in );
		for ( int i=0; i < breachCount; i++ ) {
			shipState.setBreach( readInt( in ), readInt( in ), readInt( in ) );
		}

		// Doors are defined in the layout text file, but their order is
		// different at runtime. Vacuum-adjacent doors are plucked out and
		// moved to the end... for some reason.
		Map<DoorCoordinate, ShipLayoutDoor> vacuumDoorMap = new LinkedHashMap<DoorCoordinate, ShipLayoutDoor>();
		Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
		for ( Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : layoutDoorMap.entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();
			ShipLayoutDoor layoutDoor = entry.getValue();

			if ( layoutDoor.roomIdA == -1 || layoutDoor.roomIdB == -1 ) {
				vacuumDoorMap.put( doorCoord, layoutDoor );
				continue;
			}
			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor( in, fileFormat ) );
		}
		for ( Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : vacuumDoorMap.entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();

			shipState.setDoor( doorCoord.x, doorCoord.y, doorCoord.v, readDoor( in, fileFormat ) );
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			shipState.setCloakAnimTicks( readInt( in ) );
		}

		if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			int crystalCount = readInt( in );
			List<LockdownCrystal> crystalList = new ArrayList<LockdownCrystal>();
			for ( int i=0; i < crystalCount; i++ ) {
				crystalList.add( readLockdownCrystal( in ) );
			}
			shipState.setLockdownCrystalList( crystalList );
		}

		int weaponCount = readInt( in );
		for ( int i=0; i < weaponCount; i++ ) {
			WeaponState weapon = new WeaponState();
			weapon.setWeaponId( readString( in ) );
			weapon.setArmed( readBool( in ) );

			if ( fileFormat == 2 ) {  // No longer used as of FTL 1.5.4.
				weapon.setCooldownTicks( readInt( in ) );
			}

			shipState.addWeapon( weapon );
		}
		// WeaponStates may have WeaponModules set on them later (FTL 1.5.4+).

		int droneCount = readInt( in );
		for ( int i=0; i < droneCount; i++ ) {
			shipState.addDrone( readDrone( in ) );
		}
		// DroneStates may have ExtendedDroneInfo set on them later (FTL 1.5.4+).

		int augmentCount = readInt( in );
		for ( int i=0; i < augmentCount; i++ ) {
			shipState.addAugmentId( readString( in ) );
		}

		// Standalone drones may be added to the ship later (FTL 1.5.4+).

		return shipState;
	}

	public void writeShip( OutputStream out, ShipState shipState, int fileFormat ) throws IOException {
		String shipBlueprintId = shipState.getShipBlueprintId();

		ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBlueprintId );
		if ( shipBlueprint == null )
			throw new RuntimeException( String.format( "Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName() ) );

		String shipLayoutId = shipBlueprint.getLayoutId();

		ShipLayout shipLayout = DataManager.get().getShipLayout( shipLayoutId );
		if ( shipLayout == null )
			throw new RuntimeException( String.format( "Could not find layout for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName() ) );


		writeString( out, shipBlueprintId );
		writeString( out, shipState.getShipName() );
		writeString( out, shipState.getShipGraphicsBaseName() );

		writeInt( out, shipState.getStartingCrewList().size() );
		for ( StartingCrewState startingCrew : shipState.getStartingCrewList() ) {
			writeStartingCrewMember( out, startingCrew );
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeBool( out, shipState.isHostile() );
			writeInt( out, shipState.getJumpChargeTicks() );
			writeBool( out, shipState.isJumping() );
			writeInt( out, shipState.getJumpAnimTicks() );
		}

		writeInt( out, shipState.getHullAmt() );
		writeInt( out, shipState.getFuelAmt() );
		writeInt( out, shipState.getDronePartsAmt() );
		writeInt( out, shipState.getMissilesAmt() );
		writeInt( out, shipState.getScrapAmt() );

		writeInt( out, shipState.getCrewList().size() );
		for ( CrewState crew : shipState.getCrewList() ) {
			writeCrewMember( out, crew, fileFormat );
		}

		// System info is stored in this order.
		List<SystemType> systemTypes = new ArrayList<SystemType>();
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
		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
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
					writeSystem( out, systemState, fileFormat );
				}
			}
			else {
				writeInt( out, 0 );  // Equivalent to constructing and writing a 0-capacity system.
			}
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {

			SystemState clonebayState = shipState.getSystem( SystemType.CLONEBAY );
			if ( clonebayState != null && clonebayState.getCapacity() > 0 ) {
				ClonebayInfo clonebayInfo = shipState.getExtendedSystemInfo( ClonebayInfo.class );
				// This should not be null.
				writeInt( out, clonebayInfo.getBuildTicks() );
				writeInt( out, clonebayInfo.getBuildTicksGoal() );
				writeInt( out, clonebayInfo.getDoomTicks() );
			}

			SystemState batteryState = shipState.getSystem( SystemType.BATTERY );
			if ( batteryState != null && batteryState.getCapacity() > 0 ) {
				BatteryInfo batteryInfo = shipState.getExtendedSystemInfo( BatteryInfo.class );
				// This should not be null.
				writeBool( out, batteryInfo.isActive() );
				writeInt( out, batteryInfo.getUsedBattery() );
				writeInt( out, batteryInfo.getDischargeTicks() );
			}

			if ( true ) {
				ShieldsInfo shieldsInfo = shipState.getExtendedSystemInfo( ShieldsInfo.class );
				// This should not be null.
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

			SystemState cloakingState = shipState.getSystem( SystemType.CLOAKING );
			if ( cloakingState != null && cloakingState.getCapacity() > 0 ) {
				CloakingInfo cloakingInfo = shipState.getExtendedSystemInfo( CloakingInfo.class );
				// This should not be null.
				writeInt( out, cloakingInfo.getUnknownAlpha() );
				writeInt( out, cloakingInfo.getUnknownBeta() );
				writeInt( out, cloakingInfo.getCloakTicksGoal() );

				writeMinMaxedInt( out, cloakingInfo.getCloakTicks() );
			}
    }

		int roomCount = shipLayout.getRoomCount();
		for ( int r=0; r < roomCount; r++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( r );

			RoomState room = shipState.getRoom( r );
			writeRoom( out, room, layoutRoom.squaresH, layoutRoom.squaresV, fileFormat );
		}

		writeInt( out, shipState.getBreachMap().size() );
		for ( Map.Entry<XYPair, Integer> entry : shipState.getBreachMap().entrySet() ) {
			writeInt( out, entry.getKey().x );
			writeInt( out, entry.getKey().y );
			writeInt( out, entry.getValue().intValue() );
		}

		// Doors are defined in the layout text file, but their
		// order is different at runtime. Vacuum-adjacent doors
		// are plucked out and moved to the end... for some
		// reason.
		Map<DoorCoordinate, DoorState> shipDoorMap = shipState.getDoorMap();
		Map<DoorCoordinate, ShipLayoutDoor> vacuumDoorMap = new LinkedHashMap<DoorCoordinate, ShipLayoutDoor>();
		Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
		for ( Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : layoutDoorMap.entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();
			ShipLayoutDoor layoutDoor = entry.getValue();

			if ( layoutDoor.roomIdA == -1 || layoutDoor.roomIdB == -1 ) {
				vacuumDoorMap.put( doorCoord, layoutDoor );
				continue;
			}
			writeDoor( out, shipDoorMap.get( doorCoord ), fileFormat );
		}
		for ( Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : vacuumDoorMap.entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();

			writeDoor( out, shipDoorMap.get( doorCoord ), fileFormat );
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, shipState.getCloakAnimTicks() );
		}

		if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, shipState.getLockdownCrystalList().size() );
			for ( LockdownCrystal crystal : shipState.getLockdownCrystalList() ) {
				writeLockdownCrystal( out, crystal );
			}
		}

		writeInt( out, shipState.getWeaponList().size() );
		for ( WeaponState weapon : shipState.getWeaponList() ) {
			writeString( out, weapon.getWeaponId() );
			writeBool( out, weapon.isArmed() );

			if ( fileFormat == 2 ) {  // No longer used as of FTL 1.5.4.
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
		StartingCrewState startingCrew = new StartingCrewState();

		String raceString = readString( in );
		CrewType race = CrewType.findById( raceString );
		if ( race != null ) {
			startingCrew.setRace( race );
		} else {
			throw new IOException( "Unsupported starting crew race: "+ raceString );
		}

		startingCrew.setName( readString( in ) );

		return startingCrew;
	}

	public void writeStartingCrewMember( OutputStream out, StartingCrewState startingCrew ) throws IOException {
		writeString( out, startingCrew.getRace().getId() );
		writeString( out, startingCrew.getName() );
	}

	private CrewState readCrewMember( InputStream in, int fileFormat ) throws IOException {
		CrewState crew = new CrewState();
		crew.setName( readString( in ) );

		String raceString = readString( in );
		CrewType race = CrewType.findById( raceString );
		if ( race != null ) {
			crew.setRace( race );
		} else {
			throw new IOException( "Unsupported crew race: "+ raceString );
		}

		crew.setEnemyBoardingDrone( readBool( in ) );
		crew.setHealth( readInt( in ) );
		crew.setSpriteX( readInt( in ) );
		crew.setSpriteY( readInt( in ) );
		crew.setRoomId( readInt( in ) );
		crew.setRoomSquare( readInt( in ) );
		crew.setPlayerControlled( readBool( in ) );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			crew.setCloneReady( readInt( in ) );

			int deathOrder = readInt( in );  // Redundant. Exactly the same as Clonebay Priority.

			int tintCount = readInt( in );
			List<Integer> spriteTintIndeces = new ArrayList<Integer>();
			for ( int i=0; i < tintCount; i++ ) {
				spriteTintIndeces.add( readInt( in ) );
			}
			crew.setSpriteTintIndeces( spriteTintIndeces );

			crew.setMindControlled( readBool( in ) );
			crew.setSavedRoomSquare( readInt( in ) );
			crew.setSavedRoomId( readInt( in ) );
		}

		crew.setPilotSkill( readInt( in ) );
		crew.setEngineSkill( readInt( in ) );
		crew.setShieldSkill( readInt( in ) );
		crew.setWeaponSkill( readInt( in ) );
		crew.setRepairSkill( readInt( in ) );
		crew.setCombatSkill( readInt( in ) );
		crew.setMale( readBool( in ) );
		crew.setRepairs( readInt( in ) );
		crew.setCombatKills( readInt( in ) );
		crew.setPilotedEvasions( readInt( in ) );
		crew.setJumpsSurvived( readInt( in ) );
		crew.setSkillMasteriesEarned( readInt( in ) );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			crew.setStunTicks( readInt( in ) );
			crew.setHealthBoost( readInt( in ) );
			crew.setClonebayPriority( readInt( in ) );
			crew.setDamageBoost( readInt( in ) );
			crew.setUnknownLambda( readInt( in ) );
			crew.setUniversalDeathCount( readInt( in ) );

			if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				crew.setPilotMasteryOne( readBool( in ) );
				crew.setPilotMasteryTwo( readBool( in ) );
				crew.setEngineMasteryOne( readBool( in ) );
				crew.setEngineMasteryTwo( readBool( in ) );
				crew.setShieldMasteryOne( readBool( in ) );
				crew.setShieldMasteryTwo( readBool( in ) );
				crew.setWeaponMasteryOne( readBool( in ) );
				crew.setWeaponMasteryTwo( readBool( in ) );
				crew.setRepairMasteryOne( readBool( in ) );
				crew.setRepairMasteryTwo( readBool( in ) );
				crew.setCombatMasteryOne( readBool( in ) );
				crew.setCombatMasteryTwo( readBool( in ) );
			}

			crew.setUnknownNu( readBool( in ) );

			crew.setTeleportAnim( readAnim( in ) );

			crew.setUnknownPhi( readBool( in ) );

			if ( CrewType.CRYSTAL.equals( crew.getRace() ) ) {
				crew.setLockdownRechargeTicks( readInt( in ) );
				crew.setLockdownRechargeTicksGoal( readInt( in ) );
				crew.setUnknownOmega( readInt( in ) );
			}
		}

		return crew;
	}

	public void writeCrewMember( OutputStream out, CrewState crew, int fileFormat ) throws IOException {
		writeString( out, crew.getName() );
		writeString( out, crew.getRace().getId() );
		writeBool( out, crew.isEnemyBoardingDrone() );
		writeInt( out, crew.getHealth() );
		writeInt( out, crew.getSpriteX() );
		writeInt( out, crew.getSpriteY() );
		writeInt( out, crew.getRoomId() );
		writeInt( out, crew.getRoomSquare() );
		writeBool( out, crew.isPlayerControlled() );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, crew.getCloneReady() );

			int deathOrder = crew.getClonebayPriority();  // Redundant.
			writeInt( out, deathOrder );

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
		writeInt( out, crew.getSkillMasteriesEarned() );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, crew.getStunTicks() );
			writeInt( out, crew.getHealthBoost() );
			writeInt( out, crew.getClonebayPriority() );
			writeInt( out, crew.getDamageBoost() );
			writeInt( out, crew.getUnknownLambda() );
			writeInt( out, crew.getUniversalDeathCount() );

			if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				writeBool( out, crew.getPilotMasteryOne() );
				writeBool( out, crew.getPilotMasteryTwo() );
				writeBool( out, crew.getEngineMasteryOne() );
				writeBool( out, crew.getEngineMasteryTwo() );
				writeBool( out, crew.getShieldMasteryOne() );
				writeBool( out, crew.getShieldMasteryTwo() );
				writeBool( out, crew.getWeaponMasteryOne() );
				writeBool( out, crew.getWeaponMasteryTwo() );
				writeBool( out, crew.getRepairMasteryOne() );
				writeBool( out, crew.getRepairMasteryTwo() );
				writeBool( out, crew.getCombatMasteryOne() );
				writeBool( out, crew.getCombatMasteryTwo() );
			}

			writeBool( out, crew.getUnknownNu() );

			writeAnim( out, crew.getTeleportAnim() );

			writeBool( out, crew.getUnknownPhi() );

			if ( CrewType.CRYSTAL.equals( crew.getRace() ) ) {
				writeInt( out, crew.getLockdownRechargeTicks() );
				writeInt( out, crew.getLockdownRechargeTicksGoal() );
				writeInt( out, crew.getUnknownOmega() );
			}
		}
	}

	private SystemState readSystem( InputStream in, SystemType systemType, int fileFormat ) throws IOException {
		SystemState system = new SystemState( systemType );
		int capacity = readInt( in );

		// Normally systems are 28 bytes, but if not present on the
		// ship, capacity will be zero, and the system will only
		// occupy the 4 bytes that declared the capacity. And the
		// next system will begin 24 bytes sooner.
		if ( capacity > 0 ) {
			system.setCapacity( capacity );
			system.setPower( readInt( in ) );
			system.setDamagedBars( readInt( in ) );
			system.setIonizedBars( readInt( in ) );       // TODO: Active mind control has -1?

			system.setDeionizationTicks( readMinMaxedInt( in ) );  // May be MIN_VALUE.

			system.setRepairProgress( readInt( in ) );
			system.setDamageProgress( readInt( in ) );

			if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				system.setBatteryPower( readInt( in ) );
				system.setHackLevel( readInt( in ) );
				system.setHacked( readBool( in ) );
				system.setTemporaryCapacityCap( readInt( in ) );
				system.setTemporaryCapacityLoss( readInt( in ) );
				system.setTemporaryCapacityDivisor( readInt( in ) );
			}
		}
		return system;
	}

	public void writeSystem( OutputStream out, SystemState system, int fileFormat ) throws IOException {
		writeInt( out, system.getCapacity() );
		if ( system.getCapacity() > 0 ) {
			writeInt( out, system.getPower() );
			writeInt( out, system.getDamagedBars() );
			writeInt( out, system.getIonizedBars() );

			writeMinMaxedInt( out, system.getDeionizationTicks() );  // May be MIN_VALUE.

			writeInt( out, system.getRepairProgress() );
			writeInt( out, system.getDamageProgress() );

			if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				writeInt( out, system.getBatteryPower() );
				writeInt( out, system.getHackLevel() );
				writeBool( out, system.isHacked() );
				writeInt( out, system.getTemporaryCapacityCap() );
				writeInt( out, system.getTemporaryCapacityLoss() );
				writeInt( out, system.getTemporaryCapacityDivisor() );
			}
		}
	}

	private RoomState readRoom( InputStream in, int squaresH, int squaresV, int fileFormat ) throws IOException {
		RoomState room = new RoomState();
		int oxygen = readInt( in );
		if ( oxygen < 0 || oxygen > 100 ) {
			throw new IOException( "Unsupported room oxygen: "+ oxygen );
		}
		room.setOxygen( oxygen );

		// Squares are written to disk top-to-bottom, left-to-right. (Index != ID!)
		SquareState[][] tmpSquares = new SquareState[squaresH][squaresV];
		for ( int h=0; h < squaresH; h++ ) {
			for ( int v=0; v < squaresV; v++ ) {
				tmpSquares[h][v] = new SquareState( readInt( in ), readInt( in ), readInt( in ) );
			}
		}
		// Add them to the room left-to-right, top-to-bottom. (Index == ID)
		for ( int v=0; v < squaresV; v++ ) {
			for ( int h=0; h < squaresH; h++ ) {
				room.addSquare( tmpSquares[h][v] );
			}
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			room.setStationSquare( readInt( in ) );

			StationDirection stationDirection = null;
			int stationDirectionFlag = readInt( in );

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

	public void writeRoom( OutputStream out, RoomState room, int squaresH, int squaresV, int fileFormat ) throws IOException {
		writeInt( out, room.getOxygen() );

		// Squares referenced by IDs left-to-right, top-to-bottom. (Index == ID)
		List<SquareState> squareList = room.getSquareList();
		int squareIndex = 0;
		SquareState[][] tmpSquares = new SquareState[squaresH][squaresV];
		for ( int v=0; v < squaresV; v++ ) {
			for ( int h=0; h < squaresH; h++ ) {
				tmpSquares[h][v] = squareList.get( squareIndex++ );
			}
		}
		// Squares are written to disk top-to-bottom, left-to-right. (Index != ID!)
		for ( int h=0; h < squaresH; h++ ) {
			for ( int v=0; v < squaresV; v++ ) {
				SquareState square = tmpSquares[h][v];
				writeInt( out, square.getFireHealth() );
				writeInt( out, square.getIgnitionProgress() );
				writeInt( out, square.getExtinguishmentProgress() );
			}
		}

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
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

	private DoorState readDoor( InputStream in, int fileFormat ) throws IOException {
		DoorState door = new DoorState();

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			door.setCurrentMaxHealth( readInt( in ) );
			door.setHealth( readInt( in ) );
			door.setNominalHealth( readInt( in ) );
		}

		door.setOpen( readBool( in ) );
		door.setWalkingThrough( readBool( in ) );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			door.setUnknownDelta( readInt( in ) );
			door.setUnknownEpsilon( readInt( in ) );  // TODO: Confirm: Drone lockdown.
		}

		return door;
	}

	public void writeDoor( OutputStream out, DoorState door, int fileFormat ) throws IOException {
		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, door.getCurrentMaxHealth() );
			writeInt( out, door.getHealth() );
			writeInt( out, door.getNominalHealth() );
		}

		writeBool( out, door.isOpen() );
		writeBool( out, door.isWalkingThrough() );

		if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, door.getUnknownDelta() );
			writeInt( out, door.getUnknownEpsilon() );
		}
	}

	private LockdownCrystal readLockdownCrystal( InputStream in ) throws IOException {
		LockdownCrystal crystal = new LockdownCrystal();

		crystal.setCurrentPositionX( readInt( in ) );
		crystal.setCurrentPositionY( readInt( in ) );
		crystal.setSpeed( readInt( in ) );
		crystal.setGoalPositionX( readInt( in ) );
		crystal.setGoalPositionY( readInt( in ) );
		crystal.setArrived( readBool( in ) );
		crystal.setDone( readBool( in ) );
		crystal.setLifetime( readInt( in ) );
		crystal.setSuperFreeze( readBool( in ) );
		crystal.setLockingRoom( readInt( in ) );
		crystal.setAnimDirection( readInt( in ) );
		crystal.setShardProgress( readInt( in ) );

		return crystal;
	}

	public void writeLockdownCrystal( OutputStream out, LockdownCrystal crystal ) throws IOException {
		writeInt( out, crystal.getCurrentPositionX() );
		writeInt( out, crystal.getCurrentPositionY() );
		writeInt( out, crystal.getSpeed() );
		writeInt( out, crystal.getGoalPositionX() );
		writeInt( out, crystal.getGoalPositionY() );
		writeBool( out, crystal.hasArrived() );
		writeBool( out, crystal.isDone() );
		writeInt( out, crystal.getLifetime() );
		writeBool( out, crystal.isSuperFreeze() );
		writeInt( out, crystal.getLockingRoom() );
		writeInt( out, crystal.getAnimDirection() );
		writeInt( out, crystal.getShardProgress() );
	}

	private DroneState readDrone( InputStream in ) throws IOException {
		DroneState drone = new DroneState( readString( in ) );
		drone.setArmed( readBool( in ) );
		drone.setPlayerControlled( readBool( in ) );
		drone.setBodyX( readInt( in ) );
		drone.setBodyY( readInt( in ) );
		drone.setBodyRoomId( readInt( in ) );
		drone.setBodyRoomSquare( readInt( in ) );
		drone.setHealth( readInt( in ) );
		return drone;
	}

	public void writeDrone( OutputStream out, DroneState drone ) throws IOException {
		writeString( out, drone.getDroneId() );
		writeBool( out, drone.isArmed() );
		writeBool( out, drone.isPlayerControlled() );
		writeInt( out, drone.getBodyX() );
		writeInt( out, drone.getBodyY() );
		writeInt( out, drone.getBodyRoomId() );
		writeInt( out, drone.getBodyRoomSquare() );
		writeInt( out, drone.getHealth() );
	}

	private BeaconState readBeacon( InputStream in, int fileFormat ) throws IOException {
		BeaconState beacon = new BeaconState();

		beacon.setVisitCount( readInt( in ) );
		if ( beacon.getVisitCount() > 0 ) {
			beacon.setBgStarscapeImageInnerPath( readString( in ) );
			beacon.setBgSpriteImageInnerPath( readString( in ) );
			beacon.setBgSpritePosX( readInt( in ) );
			beacon.setBgSpritePosY( readInt( in ) );
			beacon.setBgSpriteRotation( readInt( in ) );
		}

		beacon.setSeen( readBool( in ) );

		boolean enemyPresent = readBool( in );
		beacon.setEnemyPresent( enemyPresent );
		if ( enemyPresent ) {
			beacon.setShipEventId( readString( in ) );
			beacon.setAutoBlueprintId( readString( in ) );
			beacon.setShipEventSeed( readInt( in ) );

			// When player's at this beacon, the seed here matches
			// current encounter's seed.
		}

		int fleetPresence = readInt( in );
		switch ( fleetPresence ) {
			case 0: beacon.setFleetPresence( FleetPresence.NONE ); break;
			case 1: beacon.setFleetPresence( FleetPresence.REBEL ); break;
			case 2: beacon.setFleetPresence( FleetPresence.FEDERATION ); break;
			case 3: beacon.setFleetPresence( FleetPresence.BOTH ); break;
			default: throw new RuntimeException( "Unknown fleet presence: " + fleetPresence );
		}

		beacon.setUnderAttack( readBool( in ) );

		boolean storePresent = readBool( in );
		if ( storePresent ) {
			StoreState store = new StoreState();

			int shelfCount = 2;          // FTL 1.01-1.03.3 only had two shelves.
			if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				shelfCount = readInt( in );  // FTL 1.5.4 made shelves into an N-sized list.
			}
			for ( int i=0; i < shelfCount; i++ ) {
				store.addShelf( readStoreShelf( in, fileFormat ) );
			}

			store.setFuel( readInt( in ) );
			store.setMissiles( readInt( in ) );
			store.setDroneParts( readInt( in ) );
			beacon.setStore(store);
		}

		return beacon;

	}

	public void writeBeacon( OutputStream out, BeaconState beacon, int fileFormat ) throws IOException {
		writeInt( out, beacon.getVisitCount() );
		if ( beacon.getVisitCount() > 0 ) {
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

			if ( fileFormat == 2 ) {
				// FTL 1.01-1.03.3 always had two shelves.

				int shelfLimit = 2;
				int shelfCount = Math.min( store.getShelfList().size(), shelfLimit );
				for ( int i=0; i < shelfCount; i++ ) {
					writeStoreShelf( out, store.getShelfList().get( i ), fileFormat );
				}
				for ( int i=0; i < shelfLimit - shelfCount; i++ ) {
					StoreShelf dummyShelf = new StoreShelf();
					writeStoreShelf( out, dummyShelf, fileFormat );
				}
			}
			else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				// FTL 1.5.4+ requires at least one shelf.
				int shelfReq = 1;

				List<StoreShelf> pendingShelves = new ArrayList<StoreShelf>();
				pendingShelves.addAll( store.getShelfList() );

				while ( pendingShelves.size() < shelfReq ) {
					StoreShelf dummyShelf = new StoreShelf();
					pendingShelves.add( dummyShelf );
				}

				writeInt( out, pendingShelves.size() );

				for ( StoreShelf shelf : pendingShelves ) {
					writeStoreShelf( out, shelf, fileFormat );
				}
			}

			writeInt( out, store.getFuel() );
			writeInt( out, store.getMissiles() );
			writeInt( out, store.getDroneParts() );
		}
	}

	private StoreShelf readStoreShelf( InputStream in, int fileFormat ) throws IOException {
		StoreShelf shelf = new StoreShelf();

		int itemType = readInt( in );
		switch ( itemType ) {
			case 0: shelf.setItemType( StoreItemType.WEAPON ); break;
			case 1: shelf.setItemType( StoreItemType.DRONE ); break;
			case 2: shelf.setItemType( StoreItemType.AUGMENT ); break;
			case 3: shelf.setItemType( StoreItemType.CREW ); break;
			case 4: shelf.setItemType( StoreItemType.SYSTEM ); break;
			default: throw new RuntimeException( "Unknown store item type: " + itemType );
		}

		for ( int i = 0; i < 3; i++ ) {
			int available = readInt( in ); // -1=no item, 0=bought already, 1=buyable
			if ( available < 0 )
				continue;

			StoreItem item = new StoreItem( readString( in ) );
			item.setAvailable( (available > 0) );

			if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
				item.setExtraData( readInt( in ) );
			}

			shelf.addItem( item );
		}

		return shelf;
	}

	public void writeStoreShelf( OutputStream out, StoreShelf shelf, int fileFormat ) throws IOException {
		StoreItemType itemType = shelf.getItemType();
		if ( itemType == StoreItemType.WEAPON ) writeInt( out, 0 );
		else if ( itemType == StoreItemType.DRONE ) writeInt( out, 1 );
		else if ( itemType == StoreItemType.AUGMENT ) writeInt( out, 2 );
		else if ( itemType == StoreItemType.CREW ) writeInt( out, 3 );
		else if ( itemType == StoreItemType.SYSTEM ) writeInt( out, 4 );
		else throw new RuntimeException( "Unknown store item type: "+ itemType );

		List<StoreItem> items = shelf.getItems();
		for ( int i=0; i < 3; i++ ) {  // TODO: Magic number.
			if ( items.size() > i ) {
				StoreItem item = items.get( i );

				int available = (item.isAvailable() ? 1 : 0);
				writeInt( out, available );
				writeString( out, item.getItemId() );

				if ( fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
					writeInt( out, item.getExtraData() );
				}
			}
			else {
				writeInt( out, -1 );  // No item.
			}
		}
	}

	public EncounterState readEncounter( InputStream in, int fileFormat ) throws IOException {
		EncounterState encounter = new EncounterState();

		encounter.setShipEventSeed( readInt( in ) );
		encounter.setSurrenderEventId( readString( in ) );
		encounter.setEscapeEventId( readString( in ) );
		encounter.setDestroyedEventId( readString( in ) );
		encounter.setDeadCrewEventId( readString( in ) );
		encounter.setGotAwayEventId( readString( in ) );

		encounter.setLastEventId( readString( in ) );

		if ( fileFormat == 11 ) {
			encounter.setUnknownAlpha( readInt( in ) );
		}

		encounter.setText( readString( in ) );
		encounter.setAffectedCrewSeed( readInt( in ) );

		int choiceCount = readInt( in );
		List<Integer> choiceList = new ArrayList<Integer>();
		for ( int i=0; i < choiceCount; i++ ) {
			choiceList.add( readInt( in ) );
		}
		encounter.setChoiceList( choiceList );

		return encounter;
	}

	public void writeEncounter( OutputStream out, EncounterState encounter, int fileFormat ) throws IOException {
		writeInt( out, encounter.getShipEventSeed() );
		writeString( out, encounter.getSurrenderEventId() );
		writeString( out, encounter.getEscapeEventId() );
		writeString( out, encounter.getDestroyedEventId() );
		writeString( out, encounter.getDeadCrewEventId() );
		writeString( out, encounter.getGotAwayEventId() );

		writeString( out, encounter.getLastEventId() );

		if ( fileFormat == 11 ) {
			writeInt( out, encounter.getUnknownAlpha() );
		}

		writeString( out, encounter.getText() );
		writeInt( out, encounter.getAffectedCrewSeed() );

		writeInt( out, encounter.getChoiceList().size() );
		for ( Integer choiceInt : encounter.getChoiceList() ) {
			writeInt( out, choiceInt.intValue() );
		}
	}

	private NearbyShipAIState readNearbyShipAI( FileInputStream in ) throws IOException {
		NearbyShipAIState ai = new NearbyShipAIState();

		ai.setSurrendered( readBool( in ) );
		ai.setEscaping( readBool( in ) );
		ai.setDestroyed( readBool( in ) );
		ai.setSurrenderThreshold( readInt( in ) );
		ai.setEscapeThreshold( readInt( in ) );
		ai.setEscapeTicks( readInt( in ) );
		ai.setStalemateTriggered( readBool( in ) );
		ai.setStalemateTicks( readInt( in ) );
		ai.setBoardingAttempts( readInt( in ) );
		ai.setBoardersNeeded( readInt( in ) );

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
		EnvironmentState env = new EnvironmentState();

		env.setRedGiantPresent( readBool( in ) );
		env.setPulsarPresent( readBool( in ) );
		env.setPDSPresent( readBool( in ) );

		int vulnFlag = readInt( in );
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
			throw new IOException( String.format( "Unsupported environment vulnerability flag: %d", vulnFlag ) );
		}
		env.setVulnerableShips( vuln );

		boolean asteroidsPresent = readBool( in );
		if ( asteroidsPresent ) {
			AsteroidFieldState asteroidField = new AsteroidFieldState();
			asteroidField.setUnknownAlpha( readInt( in ) );
			asteroidField.setStrayRockTicks( readInt( in ) );
			asteroidField.setUnknownGamma( readInt( in ) );
			asteroidField.setBgDriftTicks( readInt( in ) );
			asteroidField.setCurrentTarget( readInt( in ) );
			env.setAsteroidField( asteroidField );
		}
		env.setSolarFlareFadeTicks( readInt( in ) );
		env.setHavocTicks( readInt( in ) );
		env.setPDSTicks( readInt( in ) );

		return env;
	}

	public void writeEnvironment( OutputStream out, EnvironmentState env ) throws IOException {
		writeBool( out, env.isRedGiantPresent() );
		writeBool( out, env.isPulsarPresent() );
		writeBool( out, env.isPDSPresent() );

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
			throw new IOException( String.format( "Unsupported environment vulnerability: %s", env.getVulnerableShips().toString() ) );
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

		flagship.setPendingStage( readInt( in ) );

		int previousRoomCount = readInt( in );
		for ( int i=0; i < previousRoomCount; i++ ) {
			flagship.setPreviousOccupancy( i, readInt( in ) );
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

	public AnimState readAnim( InputStream in ) throws IOException {
		AnimState anim = new AnimState();

		anim.setPlaying( readBool( in ) );
		anim.setLooping( readBool( in ) );
		anim.setCurrentFrame( readInt( in ) );
		anim.setProgressTicks( readInt( in ) );
		anim.setScale( readInt( in ) );
		anim.setX( readInt( in ) );
		anim.setY( readInt( in ) );

		return anim;
	}

	public void writeAnim( OutputStream out, AnimState anim ) throws IOException {
		writeBool( out, anim.isPlaying() );
		writeBool( out, anim.isLooping() );
		writeInt( out, anim.getCurrentFrame() );
		writeInt( out, anim.getProgressTicks() );
		writeInt( out, anim.getScale() );
		writeInt( out, anim.getX() );
		writeInt( out, anim.getY() );
	}

	private ProjectileState readProjectile( FileInputStream in, int fileFormat ) throws IOException {
		//log.debug( String.format( "Projectile: @%d", in.getChannel().position() ) );

		ProjectileState projectile = new ProjectileState();

		int projectileTypeFlag = readInt( in );
		if ( projectileTypeFlag == 0 ) {
			projectile.setProjectileType( ProjectileType.INVALID );
			return projectile;  // No other fields are set for invalid projectiles.
		}
		else if ( projectileTypeFlag == 1 ) {
			projectile.setProjectileType( ProjectileType.LASER_OR_BURST );
		}
		else if ( projectileTypeFlag == 2 ) {
			projectile.setProjectileType( ProjectileType.ROCK_OR_EXPLOSION );
		}
		else if ( projectileTypeFlag == 3 ) {
			projectile.setProjectileType( ProjectileType.MISSILE );
		}
		else if ( projectileTypeFlag == 4 ) {
			projectile.setProjectileType( ProjectileType.BOMB );
		}
		else if ( projectileTypeFlag == 5 ) {
			projectile.setProjectileType( ProjectileType.BEAM );
		}
		else if ( projectileTypeFlag == 6 && fileFormat == 11 ) {
			projectile.setProjectileType( ProjectileType.PDS );
		}
		else {
			throw new IOException( String.format( "Unsupported projectileType flag: %d", projectileTypeFlag ) );
		}

		projectile.setCurrentPositionX( readInt( in ) );
		projectile.setCurrentPositionY( readInt( in ) );
		projectile.setPreviousPositionX( readInt( in ) );
		projectile.setPreviousPositionY( readInt( in ) );
		projectile.setSpeed( readInt( in ) );
		projectile.setGoalPositionX( readInt( in ) );
		projectile.setGoalPositionY( readInt( in ) );
		projectile.setHeading( readInt( in ) );
		projectile.setOwnerId( readInt( in ) );
		projectile.setSelfId( readInt( in ) );

		projectile.setDamage( readDamage( in ) );

		projectile.setLifespan( readInt( in ) );
		projectile.setDestinationSpace( readInt( in ) );
		projectile.setCurrentSpace( readInt( in ) );
		projectile.setTargetId( readInt( in ) );
		projectile.setDead( readBool( in ) );
		projectile.setDeathAnimId( readString( in ) );
		projectile.setFlightAnimId( readString( in ) );

		projectile.setDeathAnim( readAnim( in ) );
		projectile.setFlightAnim( readAnim( in ) );

		projectile.setVelocityX( readInt( in ) );
		projectile.setVelocityY( readInt( in ) );
		projectile.setMissed( readBool( in ) );
		projectile.setHitTarget( readBool( in ) );
		projectile.setHitSolidSound( readString( in ) );
		projectile.setHitShieldSound( readString( in ) );
		projectile.setMissSound( readString( in ) );
		projectile.setEntryAngle( readMinMaxedInt( in ) );
		projectile.setStartedDying( readBool( in ) );
		projectile.setPassedTarget( readBool( in ) );

		projectile.setType( readInt( in ) );
		projectile.setBroadcastTarget( readBool( in ) );

		ExtendedProjectileInfo extendedInfo = null;
		if ( ProjectileType.LASER_OR_BURST.equals( projectile.getProjectileType() ) ) {
			// Laser/Burst (2).
			// Usually getType() is 4 for Laser, 2 for Burst.
			extendedInfo = readLaserProjectileInfo( in );
		}
		else if ( ProjectileType.ROCK_OR_EXPLOSION.equals( projectile.getProjectileType() ) ) {
			// Explosion/Asteroid (0).
			// getType() is always 2?
			extendedInfo = new EmptyProjectileInfo();
		}
		else if ( ProjectileType.MISSILE.equals( projectile.getProjectileType() ) ) {
			// Missile (0).
			// getType() is always 1?
			extendedInfo = new EmptyProjectileInfo();
		}
		else if ( ProjectileType.BOMB.equals( projectile.getProjectileType() ) ) {
			// Bomb (5).
			// getType() is always 5?
			extendedInfo = readBombProjectileInfo( in );
		}
		else if ( ProjectileType.BEAM.equals( projectile.getProjectileType() ) ) {
			// Beam (25).
			// getType() is always 5?
			extendedInfo = readBeamProjectileInfo( in );
		}
		else if ( ProjectileType.PDS.equals( projectile.getProjectileType() ) ) {
			// PDS (12)
			// getType() is always 5?
			extendedInfo = readPDSProjectileInfo( in );
		}
		projectile.setExtendedInfo( extendedInfo );

		return projectile;
	}

	public void writeProjectile( OutputStream out, ProjectileState projectile, int fileFormat ) throws IOException {

		int projectileTypeFlag = 0;
		if ( ProjectileType.INVALID.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 0;
		}
		else if ( ProjectileType.LASER_OR_BURST.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 1;
		}
		else if ( ProjectileType.ROCK_OR_EXPLOSION.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 2;
		}
		else if ( ProjectileType.MISSILE.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 3;
		}
		else if ( ProjectileType.BOMB.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 4;
		}
		else if ( ProjectileType.BEAM.equals( projectile.getProjectileType() ) ) {
			projectileTypeFlag = 5;
		}
		else if ( ProjectileType.PDS.equals( projectile.getProjectileType() ) && fileFormat == 11 ) {
			projectileTypeFlag = 6;
		}
		else {
			throw new IOException( String.format( "Unsupported projectileType: %s", projectile.getProjectileType().toString() ) );
		}
		writeInt( out, projectileTypeFlag );

		if ( ProjectileType.INVALID.equals( projectile.getProjectileType() ) ) {
			return;  // No other fields are set for invalid projectiles.
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

		writeAnim( out, projectile.getDeathAnim() );
		writeAnim( out, projectile.getFlightAnim() );

		writeInt( out, projectile.getVelocityX() );
		writeInt( out, projectile.getVelocityY() );
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

		ExtendedProjectileInfo extendedInfo = projectile.getExtendedInfo( ExtendedProjectileInfo.class );
		if ( extendedInfo instanceof IntegerProjectileInfo ) {
			IntegerProjectileInfo intInfo = projectile.getExtendedInfo( IntegerProjectileInfo.class );
			for ( int i=0; i < intInfo.getSize(); i++ ) {
				writeMinMaxedInt( out, intInfo.get( i ) );
			}
		}
		else if ( extendedInfo instanceof BeamProjectileInfo ) {
			writeBeamProjectileInfo( out, projectile.getExtendedInfo( BeamProjectileInfo.class ) );
		}
		else if ( extendedInfo instanceof BombProjectileInfo ) {
			writeBombProjectileInfo( out, projectile.getExtendedInfo( BombProjectileInfo.class ) );
		}
		else if ( extendedInfo instanceof LaserProjectileInfo ) {
			writeLaserProjectileInfo( out, projectile.getExtendedInfo( LaserProjectileInfo.class ) );
		}
		else if ( extendedInfo instanceof PDSProjectileInfo ) {
			writePDSProjectileInfo( out, projectile.getExtendedInfo( PDSProjectileInfo.class ) );
		}
		else if ( extendedInfo instanceof EmptyProjectileInfo ) {
			// No-op.
		}
		else {
			throw new IOException( "Unsupported extended projectile info: "+ extendedInfo.getClass().getSimpleName() );
		}
	}

	public DamageState readDamage( InputStream in ) throws IOException {
		DamageState damage = new DamageState();

		damage.setHullDamage( readInt( in ) );
		damage.setShieldPiercing( readInt( in ) );
		damage.setFireChance( readInt( in ) );
		damage.setBreachChance( readInt( in ) );
		damage.setIonDamage( readInt( in ) );
		damage.setSystemDamage( readInt( in ) );
		damage.setPersonnelDamage( readInt( in ) );
		damage.setHullBuster( readBool( in ) );
		damage.setOwnerId( readInt( in ) );
		damage.setSelfId( readInt( in ) );
		damage.setLockdown( readBool( in ) );
		damage.setCrystalShard( readBool( in ) );
		damage.setStunChance( readInt( in ) );
		damage.setStunAmount( readInt( in ) );

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

	private BeamProjectileInfo readBeamProjectileInfo( FileInputStream in ) throws IOException {
		BeamProjectileInfo beamInfo = new BeamProjectileInfo();

		beamInfo.setEmissionEndX( readInt( in ) );
		beamInfo.setEmissionEndY( readInt( in ) );
		beamInfo.setStrafeSourceX( readInt( in ) );
		beamInfo.setStrafeSourceY( readInt( in ) );

		beamInfo.setStrafeEndX( readInt( in ) );
		beamInfo.setStrafeEndY( readInt( in ) );
		beamInfo.setUnknownBetaX( readInt( in ) );
		beamInfo.setUnknownBetaY( readInt( in ) );

		beamInfo.setSwathEndX( readInt( in ) );
		beamInfo.setSwathEndY( readInt( in ) );
		beamInfo.setSwathStartX( readInt( in ) );
		beamInfo.setSwathStartY( readInt( in ) );

		beamInfo.setUnknownGamma( readInt( in ) );
		beamInfo.setSwathLength( readInt( in ) );
		beamInfo.setUnknownDelta( readInt( in ) );

		beamInfo.setUnknownEpsilonX( readInt( in ) );
		beamInfo.setUnknownEpsilonY( readInt( in ) );

		beamInfo.setUnknownZeta( readInt( in ) );
		beamInfo.setUnknownEta( readInt( in ) );
		beamInfo.setEmissionAngle( readInt( in ) );

		beamInfo.setUnknownIota( readBool( in ) );
		beamInfo.setUnknownKappa( readBool( in ) );
		beamInfo.setFromDronePod( readBool( in ) );
		beamInfo.setUnknownMu( readBool( in ) );
		beamInfo.setUnknownNu( readBool( in ) );

		return beamInfo;
	}

	public void writeBeamProjectileInfo( OutputStream out, BeamProjectileInfo beamInfo ) throws IOException {
		writeInt( out, beamInfo.getEmissionEndX() );
		writeInt( out, beamInfo.getEmissionEndY() );
		writeInt( out, beamInfo.getStrafeSourceX() );
		writeInt( out, beamInfo.getStrafeSourceY() );

		writeInt( out, beamInfo.getStrafeEndX() );
		writeInt( out, beamInfo.getStrafeEndY() );
		writeInt( out, beamInfo.getUnknownBetaX() );
		writeInt( out, beamInfo.getUnknownBetaY() );

		writeInt( out, beamInfo.getSwathEndX() );
		writeInt( out, beamInfo.getSwathEndY() );
		writeInt( out, beamInfo.getSwathStartX() );
		writeInt( out, beamInfo.getSwathStartY() );

		writeInt( out, beamInfo.getUnknownGamma() );
		writeInt( out, beamInfo.getSwathLength() );
		writeInt( out, beamInfo.getUnknownDelta() );

		writeInt( out, beamInfo.getUnknownEpsilonX() );
		writeInt( out, beamInfo.getUnknownEpsilonY() );

		writeInt( out, beamInfo.getUnknownZeta() );
		writeInt( out, beamInfo.getUnknownEta() );
		writeInt( out, beamInfo.getEmissionAngle() );

		writeBool( out, beamInfo.getUnknownIota() );
		writeBool( out, beamInfo.getUnknownKappa() );
		writeBool( out, beamInfo.isFromDronePod() );
		writeBool( out, beamInfo.getUnknownMu() );
		writeBool( out, beamInfo.getUnknownNu() );
	}

	private BombProjectileInfo readBombProjectileInfo( FileInputStream in ) throws IOException {
		BombProjectileInfo bombInfo = new BombProjectileInfo();

		bombInfo.setUnknownAlpha( readInt( in ) );
		bombInfo.setFuseTicks( readInt( in ) );
		bombInfo.setUnknownGamma( readInt( in ) );
		bombInfo.setUnknownDelta( readInt( in ) );
		bombInfo.setArrived( readBool( in ) );

		return bombInfo;
	}

	public void writeBombProjectileInfo( OutputStream out, BombProjectileInfo bombInfo ) throws IOException {
		writeInt( out, bombInfo.getUnknownAlpha() );
		writeInt( out, bombInfo.getFuseTicks() );
		writeInt( out, bombInfo.getUnknownGamma() );
		writeInt( out, bombInfo.getUnknownDelta() );
		writeBool( out, bombInfo.hasArrived() );
	}

	private LaserProjectileInfo readLaserProjectileInfo( FileInputStream in ) throws IOException {
		LaserProjectileInfo laserInfo = new LaserProjectileInfo();

		laserInfo.setUnknownAlpha( readInt( in ) );
		laserInfo.setSpin( readInt( in ) );

		return laserInfo;
	}

	public void writeLaserProjectileInfo( OutputStream out, LaserProjectileInfo laserInfo ) throws IOException {
		writeInt( out, laserInfo.getUnknownAlpha() );
		writeInt( out, laserInfo.getSpin() );
	}

	private PDSProjectileInfo readPDSProjectileInfo( FileInputStream in ) throws IOException {
		PDSProjectileInfo pdsInfo = new PDSProjectileInfo();

		pdsInfo.setUnknownAlpha( readInt( in ) );
		pdsInfo.setUnknownBeta( readInt( in ) );
		pdsInfo.setUnknownGamma( readInt( in ) );
		pdsInfo.setUnknownDelta( readInt( in ) );
		pdsInfo.setUnknownEpsilon( readInt( in ) );
		pdsInfo.setUnknownZeta( readAnim( in ) );

		return pdsInfo;
	}

	public void writePDSProjectileInfo( OutputStream out, PDSProjectileInfo pdsInfo ) throws IOException {
		writeInt( out, pdsInfo.getUnknownAlpha() );
		writeInt( out, pdsInfo.getUnknownBeta() );
		writeInt( out, pdsInfo.getUnknownGamma() );
		writeInt( out, pdsInfo.getUnknownDelta() );
		writeInt( out, pdsInfo.getUnknownEpsilon() );
		writeAnim( out, pdsInfo.getUnknownZeta() );
	}



	/**
	 * Counters used for event criteria and achievements.
	 *
	 * FTL 1.5.4 introduced HIGH_O2 and SUFFOCATED_CREW.
	 */
	public static enum StateVar {
		// TODO: Magic strings.
		BLUE_ALIEN     ( "blue_alien",      "Blue event choices clicked. (Only ones that require a race.)" ),
		DEAD_CREW      ( "dead_crew",       "Ships defeated by killing all enemy crew." ),
		DESTROYED_ROCK ( "destroyed_rock",  "Rock ships destroyed, including pirates." ),
		ENV_DANGER     ( "env_danger",      "Jumps into beacons with environmental dangers." ),
		FIRED_SHOT     ( "fired_shot",      "Individual beams/blasts/projectiles fired. (See also: used_missile)" ),
		HIGH_O2        ( "higho2",          "Times oxygen exceeded 20%, incremented when arriving at a beacon. (Bug: Or loading in FTL 1.5.4-1.5.10)" ),
		KILLED_CREW    ( "killed_crew",     "Enemy crew killed. (And possibly friendly fire?)" ),
		LOST_CREW      ( "lost_crew",       "Crew you've lost: killed, abandoned on nearby ships, taken by events?, but not dismissed. Even if cloned later. (See also: dead_crew)" ),
		NEBULA         ( "nebula",          "Jumps into nebula beacons." ),
		OFFENSIVE_DRONE( "offensive_drone", "The number of times drones capable of damaging an enemy ship powered up." ),
		REACTOR_UPGRADE( "reactor_upgrade", "Reactor (power bar) upgrades beyond the ship's default levels." ),
		STORE_PURCHASE ( "store_purchase",  "Non-repair purchases, such as crew/items. (Selling isn't counted.)" ),
		STORE_REPAIR   ( "store_repair",    "Store repair button clicks." ),
		SUFFOCATED_CREW( "suffocated_crew", "???" ),
		SYSTEM_UPGRADE ( "system_upgrade",  "System (and subsystem; not reactor) upgrades beyond the ship's default levels." ),
		TELEPORTED     ( "teleported",      "Teleporter activations, in either direction." ),
		USED_DRONE     ( "used_drone",      "The number of times drone parts were consumed." ),
		USED_MISSILE   ( "used_missile",    "Missile/bomb weapon discharges. (See also: fired_shot)" ),
		WEAPON_UPGRADE ( "weapon_upgrade",  "Weapons system upgrades beyond the ship's default levels. (See also: system_upgrade)" );

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
				if ( v.getId().equals( id ) ) return v;
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
		private int fileFormat = 0;
		private boolean randomNative = true;
		private boolean dlcEnabled = false;
		private Difficulty difficulty = Difficulty.EASY;
		private int totalShipsDefeated = 0;
		private int totalBeaconsExplored = 0;
		private int totalScrapCollected = 0;
		private int totalCrewHired = 0;
		private String playerShipName = "";
		private String playerShipBlueprintId = "";
		private int sectorNumber = 1;
		private int unknownBeta = 0;
		private Map<String, Integer> stateVars = new LinkedHashMap<String, Integer>();
		private ShipState playerShipState = null;
		private List<String> cargoIdList = new ArrayList<String>();
		private int sectorTreeSeed = 42;      // Arbitrary default.
		private int sectorLayoutSeed = 42;    // Arbitrary default.
		private int rebelFleetOffset = -750;  // Arbitrary default.
		private int rebelFleetFudge = 100;    // Arbitrary default.
		private int rebelPursuitMod = 0;
		private int currentBeaconId = 0;
		private boolean waiting = false;
		private int waitEventSeed = -1;
		private String unknownEpsilon = "";
		private boolean sectorHazardsVisible = false;
		private boolean rebelFlagshipVisible = false;
		private int rebelFlagshipHop = 0;
		private boolean rebelFlagshipMoving = false;
		private boolean rebelFlagshipRetreating = false;
		private int rebelFlagshipBaseTurns = 0;
		private List<Boolean> sectorVisitationList = new ArrayList<Boolean>();
		private boolean sectorIsHiddenCrystalWorlds = false;
		private List<BeaconState> beaconList = new ArrayList<BeaconState>();
		private Map<String, Integer> questEventMap = new LinkedHashMap<String, Integer>();
		private List<String> distantQuestEventList = new ArrayList<String>();
		private int unknownMu = 0;
		private EncounterState encounter = null;
		private boolean rebelFlagshipNearby = false;
		private ShipState nearbyShipState = null;
		private NearbyShipAIState nearbyShipAI = null;
		private EnvironmentState environment = null;
		private List<ProjectileState> projectileList = new ArrayList<ProjectileState>();
		private int unknownNu = 0;
		private Integer unknownXi = null;
		private boolean autofire = false;
		private RebelFlagshipState rebelFlagshipState = null;
		private List<MysteryBytes> mysteryList = new ArrayList<MysteryBytes>();


		public SavedGameState() {
		}

		/**
		 * Sets the magic number indicating file format.
		 *
		 * Observed values:
		 *   2 = Saved Game, FTL 1.01-1.03.3
		 *   7 = Saved Game, FTL 1.5.4-1.5.10
		 *   8 = Saved Game, FTL 1.5.12
		 *   9 = Saved Game, FTL 1.5.13
		 *  11 = Saved Game, FTL 1.6.1
		 *
		 * Unicode strings were introduced in FTL 1.6.1. Unlike
		 * profiles, saved games DO have a magic number to detect that
		 * version, so windows-1252 characters can be enforced for
		 * earlier FTL versions.
		 *
		 * @see net.blerf.ftl.parser.Parser#setUnicode(boolean)
		 */
		public void setFileFormat( int n ) { fileFormat = n; }
		public int getFileFormat() { return fileFormat; }

		/**
		 * Sets whether the native RNG was used.
		 *
		 * FTL 1.6.1 introduced a hard-coded RNG to use on all platforms.
		 * Earlier editions delegated to the OS srand()/rand() functions,
		 * making saved games platform-dependent.
		 *
		 * This value is set to true if a saved game from an earlier edition
		 * is migrated, requiring a native RNG to interpret its seeds.
		 *
		 * This was introduced in FTL 1.6.1.
		 */
		public void setRandomNative( boolean b ) { randomNative = b; }
		public boolean isRandomNative() { return randomNative; }

		/**
		 * Sets the difficulty.
		 *
		 * EASY
		 * NORMAL
		 * HARD (FTL 1.5.4+)
		 */
		public void setDifficulty( Difficulty d ) { difficulty = d; }
		public Difficulty getDifficulty() { return difficulty; }

		/**
		 * Sets the total number of ships defeated.
		 *
		 * Either reducing hull to 0 or eliminating crew will increase this total.
		 */
		public void setTotalShipsDefeated( int n ) { totalShipsDefeated = n; }
		public int getTotalShipsDefeated() { return totalShipsDefeated; }

		public void setTotalBeaconsExplored( int n ) { totalBeaconsExplored = n; }
		public int getTotalBeaconsExplored() { return totalBeaconsExplored; }

		/**
		 * Sets the total scrap collected.
		 *
		 * Sales at stores and the Scrap Recovery Arm do not increase this total.
		 */
		public void setTotalScrapCollected( int n ) { totalScrapCollected = n; }
		public int getTotalScrapCollected() { return totalScrapCollected; }

		public void setTotalCrewHired( int n ) { totalCrewHired = n; }
		public int getTotalCrewHired() { return totalCrewHired; }

		/**
		 * Returns the computed score, as would be displayed in FTL if the game had ended.
		 *
		 * @see #setDifficulty(Difficulty)
		 * @see #setTotalShipsDefeated(int)
		 * @see #setTotalBeaconsExplored(int)
		 * @see #setTotalScrapCollected(int)
		 */
		public int calculateScore() {
			float diffMod;
			if ( difficulty == Difficulty.EASY ) {
				diffMod = 1.0f;
			}
			else if ( difficulty == Difficulty.NORMAL ) {
				diffMod = 1.25f;
			}
			else if ( difficulty == Difficulty.HARD ) {
				diffMod = 1.5f;
			}
			else {
				log.warn( String.format( "Substituting EASY for unsupported difficulty while calculating score: %s", difficulty.toString() ) );
				diffMod = 1.0f;
			}
			return (int)((totalScrapCollected + 10*totalBeaconsExplored + 20*totalShipsDefeated) * diffMod);
		}

		/**
		 * Sets a redundant player ship name.
		 */
		public void setPlayerShipName( String shipName) {
			playerShipName = shipName;
		}
		public String getPlayerShipName() { return playerShipName; }

		/**
		 * Sets a redundant player ship blueprint.
		 */
		public void setPlayerShipBlueprintId( String shipBlueprintId ) {
			playerShipBlueprintId = shipBlueprintId;
		}
		public String getPlayerShipBlueprintId() { return playerShipBlueprintId; }

		/**
		 * Adds cargo to the player ship (N/A for enemy ships).
		 */
		public void addCargoItemId( String cargoItemId ) {
			cargoIdList.add( cargoItemId );
		}

		public void setCargoList( ArrayList<String> cargoIdList ) {
			this.cargoIdList = cargoIdList;
		}
		public List<String> getCargoIdList() { return cargoIdList; }

		/**
		 * Sets the current sector's number (0-based).
		 *
		 * After editing, the map's displayed sector number, all visible
		 * hazards, and point-of-interest labels will immediately change, but
		 * not the beacons' pixel positions.
		 *
		 * Previously-visited beacons with lingering ship encounters will
		 * retain their events, as those details come from the fixed
		 * BeaconState list.
		 *
		 * Modifying this will not affect the sector tree.
		 *
		 * After jumping from the exit into a new sector, FTL will increment
		 * this number, and also set a redundant 1-based sector number in the
		 * saved game's header.
		 *
		 * TODO: Determine long-term effects of this. The Last Stand is baked
		 * into the sector tree, but weird things might happen at or above #7.
		 *
		 * @see #addBeacon(BeaconState)
		 * @see #setSectorLayoutSeed(int)
		 * @see #setSectorTreeSeed(int)
		 */
		public void setSectorNumber( int n ) { sectorNumber = n; }
		public int getSectorNumber() { return sectorNumber; }

		/**
		 * Toggles FTL:AE content.
		 *
		 * Note: Bad things may happen if you change the value from true to
		 * false, if this saved game depends on AE resources.
		 *
		 * Sector tree reconstruction will be affected by changes to available
		 * sectors.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setSectorLayoutSeed(int)
		 */
		public void setDLCEnabled( boolean b ) { dlcEnabled = b; }
		public boolean isDLCEnabled() { return dlcEnabled; }

		/**
		 * Unknown.
		 *
		 * Always 0?
		 */
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

		/**
		 * Sets the value of a state var.
		 *
		 * State vars are mostly used to test candidacy for achievements.
		 *
		 * See StateVar enums for known vars and descriptions.
		 */
		public void setStateVar( String stateVarId, int stateVarValue ) {
			stateVars.put( stateVarId, stateVarValue );
		}

		/**
		 * Returns true if a state var has been set, false otherwise.
		 */
		public boolean hasStateVar( String stateVarId ) {
			return stateVars.containsKey( stateVarId );
		}

		/**
		 * Returns the value of a state var.
		 *
		 * If the state var has not been set, a NullPointerException will be
		 * thrown.
		 */
		public int getStateVar( String stateVarId ) {
			Integer result = stateVars.get( stateVarId );
			return result.intValue();
		}

		public Map<String, Integer> getStateVars() { return stateVars; }

		public void setPlayerShip( ShipState shipState ) {
			this.playerShipState = shipState;
		}
		public ShipState getPlayerShip() { return playerShipState; }

		/**
		 * Sets the seed for generating the sector tree.
		 *
		 * Note: When this is changed, you MUST reset sector visitation.
		 *
		 * @see #setSectorVisitation(List)
		 * @see net.blerf.ftl.parser.sectortree.RandomSectorTreeGenerator
		 */
		public void setSectorTreeSeed( int n ) { sectorTreeSeed = n; }
		public int getSectorTreeSeed() { return sectorTreeSeed; }

		/**
		 * Sets the seed for randomness in the current sector.
		 *
		 * This determines the graphical positioning of beacons, as well as
		 * their environment hazards (like nebula/storm) and events.
		 *
		 * Reloading a saved game from the end of the previous sector, and
		 * exiting again will yield a different seed. So sectors' layout seeds
		 * aren't predetermined at the start of the game.
		 *
		 * Changing this may affect the beacon count. The game will generate
		 * additional beacons if it expects them (and probably truncate the
		 * excess if there are too many).
		 *
		 * Note: The RNG algorithm that FTL uses to interpret seeds will vary
		 * with each platform. Results will be inconsistent if a saved game is
		 * resumed on another operating system.
		 *
		 * @see #addBeacon(BeaconState)
		 */
		public void setSectorLayoutSeed( int n ) { sectorLayoutSeed = n; }
		public int getSectorLayoutSeed() { return sectorLayoutSeed; }

		/**
		 * Sets the raw fleet position on the map.
		 *
		 * This is always a negative value that, when added to rebelFleetFudge,
		 * equals how far in from the right the warning circle has encroached.
		 *
		 * Most sectors start with large negative value to keep this off-screen
		 * and increment toward 0 from there.
		 *
		 * In FTL 1.01-1.03.3, The Last Stand sector used a constant -25 and
		 * moderate rebelFleetFudge value to cover the map. In other sectors,
		 * This was always observed in multiples of 25.
		 *
		 * The image is 'img/map/map_warningcircle_point.png' (650px wide, with
		 * a ~50px margin).
		 *
		 * TODO: After loading a saved game from FTL 1.03.3 into FTL 1.5.4,
		 * this value was observed going from -250 to -459. The fudge was
		 * unchanged. The significance of this is unknown.
		 *
		 * @param n pixels from the map's right edge
		 * @see #setRebelFleetFudge(int)
		 */
		public void setRebelFleetOffset( int n ) { rebelFleetOffset = n; }
		public int getRebelFleetOffset() { return rebelFleetOffset; }

		/**
		 * Sets an intra-sector constant adjusting initial fleet encroachment.
		 *
		 * This is always a positive number around 75-310 that,
		 * when added to rebelFleetOffset, equals how far in
		 * from the right the warning circle has encroached.
		 *
		 * This varies seemingly randomly from game to game and
		 * sector to sector, but it's consistent while within
		 * each sector. Except in The Last Stand, in which it is
		 * always 200 (the warning circle will extend beyond
		 * the righthand edge of the map, covering everything).
		 *
		 * @see #setRebelFleetOffset(int)
		 */
		public void setRebelFleetFudge( int n ) { rebelFleetFudge = n; }
		public int getRebelFleetFudge() { return rebelFleetFudge; }

		/**
		 * Delays/alerts the rebel fleet (-/+).
		 *
		 * This adjusts the thickness of the warning zone.
		 * Example: Hiring a merc ship to distract sets -2.
		 */
		public void setRebelPursuitMod( int n ) { rebelPursuitMod = n; }
		public int getRebelPursuitMod() { return rebelPursuitMod; }

		/**
		 * Toggles whether a wait event is active (as in out of fuel).
		 *
		 * If true, the waitEventSeed must be set, or FTL will crash.
		 *
		 * If true, a random fuel-related event will be chosen (distress signal
		 * status is not saved, only remembered while FTL runs - even bouncing to
		 * the main menu). The EncounterState's choice list will need to be empty
		 * or incomplete for the event popup to appear.
		 *
		 * Note: The EncounterState's text will NOT be synchronized with the
		 * event. Any lingering value will display with this event's choices
		 * below it... unless the text is set manually.
		 *
		 * After the wait event has completed, this will be set to false.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setWaitEventSeed(int)
		 * @see EncounterState#setText(String)
		 * @see EncounterState#setChoiceList(List<Integer>)
		 */
		public void setWaiting( boolean b ) { waiting = b; }
		public boolean isWaiting() { return waiting; }

		/**
		 * Sets a seed for wait events.
		 *
		 * This has no effect when not waiting.
		 *
		 * When not set, this is -1.
		 *
		 * This value lingers.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setWaiting(boolean)
		 */
		public void setWaitEventSeed( int n ) { waitEventSeed = n; }
		public int getWaitEventSeed() { return waitEventSeed; }

		/**
		 * Unknown.
		 *
		 * This has been observed to be an eventId of some sort
		 * ("FUEL_ESCAPE_ASTEROIDS") related to waiting.
		 *
		 * When not set, this is "".
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownEpsilon( String s ) { unknownEpsilon = s; }
		public String getUnknownEpsilon() { return unknownEpsilon; }

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
		 * a few turns before the game ends.
		 *
		 * If moving, this will be the beacon it's departing from.
		 */
		public void setRebelFlagshipHop( int n ) { rebelFlagshipHop = n; }
		public int getRebelFlagshipHop() { return rebelFlagshipHop; }

		/**
		 * Sets whether the flagship is circling its beacon or moving toward the next.
		 */
		public void setRebelFlagshipMoving( boolean b ) { rebelFlagshipMoving = b; }
		public boolean isRebelFlagshipMoving() { return rebelFlagshipMoving; }

		/**
		 * Sets whether the flagship is reversing course to a previous beacon.
		 *
		 * FTL sets this immediately after defeating the flagship.
		 *
		 * Observed values: 0 (Almost always), 1 (Immediately after defeating
		 * the flagship, but reverts if loaded and saved again!?).
		 *
		 * Bug in FTL 1.5.4-1.6.2: Bouncing to the main menu twice after
		 * defeating the flagship at the base will reset hops and baseTurns to
		 * 0, teleporting it to where it started.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setRebelFlagshipHop(int)
		 */
		public void setRebelFlagshipRetreating( boolean b ) { rebelFlagshipRetreating = b; }
		public boolean isRebelFlagshipRetreating() { return rebelFlagshipRetreating; }

		/**
		 * Sets the number of turns the rebel flagship has started at the
		 * federation base.
		 *
		 * At the 4th turn, the game will end. (TODO: Confirm.)
		 * This resets to 0 when the flagship flees to another beacon after
		 * defeat.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @param n 0-4
		 */
		public void setRebelFlagshipBaseTurns( int n ) { rebelFlagshipBaseTurns = n; }
		public int getRebelFlagshipBaseTurns() { return rebelFlagshipBaseTurns; }

		/**
		 * Toggles whether a dot on the sector tree has been visited.
		 *
		 * @param sector an index of the sector list (0-based)
		 * @param visited true if visited, false otherwise
		 */
		public void setSectorVisited( int sector, boolean visited ) {
			sectorVisitationList.set( sector, visited );
		}

		/**
		 * Sets a list of sector tree breadcrumbs.
		 *
		 * Saved games only contain a linear set of boolean flags to
		 * track visited status. FTL reconstructs the sector tree at
		 * runtime using the sector tree seed, and it maps these
		 * booleans to the dots: top-to-bottom for each column,
		 * left-to-right.
		 *
		 * @see #setSectorTreeSeed(int)
		 * @see net.blerf.ftl.model.SectorDot#setVisited(boolean)
		 * @see net.blerf.ftl.model.SectorTree#setSectorVisitation(List)
		 */
		public void setSectorVisitation( List<Boolean> route ) { sectorVisitationList = route; }
		public List<Boolean> getSectorVisitation() { return sectorVisitationList; }

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
		 *
		 * Beacons are indexed top-to-bottom for each column, left-to-right.
		 * They're randomly offset a little when shown on screen to disguise
		 * the columns.
		 *
		 * The grid is approximately 6 x 4, but each column may be smaller.
		 *
		 * Indexes can range from 0 to 23, but the sector layout seed may
		 * generate fewer.
		 *
		 * @see #setSectorLayoutSeed(int)
		 */
		public void addBeacon( BeaconState beacon ) {
			beaconList.add( beacon );
		}

		public List<BeaconState> getBeaconList() { return beaconList; }

		public void addQuestEvent( String questEventId, int questBeaconId ) {
			questEventMap.put( questEventId, questBeaconId );
		}

		public Map<String, Integer> getQuestEventMap() {
			return questEventMap;
		}

		public void addDistantQuestEvent( String questEventId ) {
			distantQuestEventList.add( questEventId );
		}

		public List<String> getDistantQuestEventList() {
			return distantQuestEventList;
		}

		/**
		 * Sets which beacon the the player ship is at.
		 *
		 * @see #getBeaconList()
		 */
		public void setCurrentBeaconId( int n ) { currentBeaconId = n; }
		public int getCurrentBeaconId() { return currentBeaconId; }

		/**
		 * Unknown
		 *
		 * Observed values: 1.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownMu( int n ) { unknownMu = n; }
		public int getUnknownMu() { return unknownMu; }

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

		/**
		 * Sets a nearby ship, or null.
		 *
		 * Since FTL 1.5.4, when this is non-null, a NearbyShipAI must be set.
		 *
		 * @see #setNearbyShipAI(NearbyShipAIState)
		 */
		public void setNearbyShip( ShipState shipState ) {
			this.nearbyShipState = shipState;
		}
		public ShipState getNearbyShip() { return nearbyShipState; }

		/**
		 * Sets fields related to AI that controls the nearby ship, or null.
		 *
		 * To set this, the nearby ship must be non-null.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setNearbyShipState(ShipState)
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
		 * Adds a projectile, currently in transit between ships.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void addProjectile( ProjectileState projectile ) {
			projectileList.add( projectile );
		}

		public List<ProjectileState> getProjectileList() { return projectileList; }


		/**
		 * Unknown.
		 *
		 * Erratic values, large and small. Even changes mid-combat!?
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownNu( int n ) { unknownNu = n; }
		public int getUnknownNu() { return unknownNu; }

		/**
		 * Unknown.
		 *
		 * Erratic values, large and small.
		 *
		 * This is only set when a nearby ship is present.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownXi( Integer n ) { unknownXi = n; }
		public Integer getUnknownXi() { return unknownXi; }

		/**
		 * Toggles autofire.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setAutofire( boolean b ) { autofire = b; }
		public boolean getAutofire() { return autofire; }

		/**
		 * Sets info about the next encounter with the rebel flagship.
		 */
		public void setRebelFlagshipState( RebelFlagshipState flagshipState ) {
			this.rebelFlagshipState = flagshipState;
		}
		public RebelFlagshipState getRebelFlagshipState() {
			return rebelFlagshipState;
		}


		public void addMysteryBytes( MysteryBytes m ) {
			mysteryList.add(m);
		}
		public List<MysteryBytes> getMysteryList() { return mysteryList; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			String formatDesc = null;
			switch ( fileFormat ) {
				case( 2 ): formatDesc = "Saved Game, FTL 1.01-1.03.3"; break;
				case( 7 ): formatDesc = "Saved Game, FTL 1.5.4-1.5.10"; break;
				case( 8 ): formatDesc = "Saved Game, FTL 1.5.12"; break;
				case( 9 ): formatDesc = "Saved Game, FTL 1.5.13"; break;
				case( 11 ): formatDesc = "Saved Game, FTL 1.6.1"; break;
				default: formatDesc = "???"; break;
			}

			boolean first = true;
			result.append( String.format( "File Format:            %5d (%s)\n", fileFormat, formatDesc ) );
			result.append( String.format( "Native RNG:             %5b (True for games migrated into FTL 1.6.1+)\n", randomNative ) );
			result.append( String.format( "AE Content:             %5s\n", (dlcEnabled ? "Enabled" : "Disabled" ) ) );
			result.append( String.format( "Ship Name:              %s\n", playerShipName ) );
			result.append( String.format( "Ship Type:              %s\n", playerShipBlueprintId ) );
			result.append( String.format( "Difficulty:             %s\n", difficulty.toString() ) );
			result.append( String.format( "Sector:                 %5d (%d)\n", sectorNumber, sectorNumber+1 ) );
			result.append( String.format( "Beta?:                  %5d (Always 0?)\n", unknownBeta ) );
			result.append( String.format( "Total Ships Defeated:   %5d\n", totalShipsDefeated ) );
			result.append( String.format( "Total Beacons Explored: %5d\n", totalBeaconsExplored ) );
			result.append( String.format( "Total Scrap Collected:  %5d\n", totalScrapCollected ) );
			result.append( String.format( "Total Crew Hired:       %5d\n", totalCrewHired ) );

			result.append( "\nState Vars...\n" );
			for ( Map.Entry<String, Integer> entry : stateVars.entrySet() ) {
				result.append( String.format( "%-16s %4d\n", entry.getKey() +":", entry.getValue().intValue() ) );
			}

			result.append( "\nPlayer Ship...\n" );
			if ( playerShipState != null )
				result.append( playerShipState.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );

			result.append( "\nCargo...\n" );
			for ( String cargoItemId : cargoIdList ) {
				result.append( String.format( "CargoItemId: %s\n", cargoItemId ) );
			}

			result.append( "\nSector Data...\n" );
			result.append( String.format( "Sector Tree Seed:    %5d\n", sectorTreeSeed ) );
			result.append( String.format( "Sector Layout Seed:  %5d\n", sectorLayoutSeed ) );
			result.append( String.format( "Rebel Fleet Offset:  %5d\n", rebelFleetOffset ) );
			result.append( String.format( "Rebel Fleet Fudge:   %5d\n", rebelFleetFudge ) );
			result.append( String.format( "Rebel Pursuit Mod:   %5d\n", rebelPursuitMod ) );
			result.append( String.format( "Player BeaconId:     %5d\n", currentBeaconId ) );
			result.append( String.format( "Waiting:             %5b\n", waiting ) );
			result.append( String.format( "Wait Event Seed:     %5d\n", waitEventSeed ) );
			result.append( String.format( "Epsilon?:            %s\n", unknownEpsilon ) );
			result.append( String.format( "Sector Hazards Map:  %5b\n", sectorHazardsVisible ) );
			result.append( String.format( "In Hidden Sector:    %5b\n", sectorIsHiddenCrystalWorlds ) );
			result.append( "\n" );
			result.append( String.format( "Flagship Visible:    %5b\n", rebelFlagshipVisible ) );
			result.append( String.format( "Flagship Nth Hop:    %5d\n", rebelFlagshipHop ) );
			result.append( String.format( "Flagship Moving:     %5b\n", rebelFlagshipMoving ) );
			result.append( String.format( "Flagship Retreating: %5b\n", rebelFlagshipRetreating ) );
			result.append( String.format( "Flagship Base Turns: %5d\n", rebelFlagshipBaseTurns ) );

			result.append( "\nSector Tree Breadcrumbs...\n" );
			first = true;
			for ( Boolean b : sectorVisitationList ) {
				if ( first ) { first = false; }
				else { result.append( "," ); }
				result.append( (b ? "T" : "F") );
			}
			result.append( "\n" );

			result.append( "\nSector Beacons...\n" );
			int beaconId = 0;
			first = true;
			for( BeaconState beacon : beaconList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "BeaconId: %2d\n", beaconId++ ) );
				result.append( beacon.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nQuests...\n" );
			for ( Map.Entry<String, Integer> entry : questEventMap.entrySet() ) {
				String questEventId = entry.getKey();
				int questBeaconId = entry.getValue().intValue();
				result.append( String.format( "QuestEventId: %s, BeaconId: %d\n", questEventId, questBeaconId ) );
			}

			result.append( "\nNext Sector Quests...\n" );
			for ( String questEventId : distantQuestEventList ) {
				result.append( String.format( "QuestEventId: %s\n", questEventId ) );
			}

			result.append( "\n" );
			result.append( String.format( "Mu?:                %5d\n", unknownMu ) );

			result.append( "\nCurrent Encounter...\n" );
			if ( encounter != null ) {
				result.append( encounter.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );
			result.append( String.format( "Flagship Nearby:    %5b (Only set when a nearby ship is present)\n", rebelFlagshipNearby ) );

			result.append( "\nNearby Ship...\n" );
			if ( nearbyShipState != null ) {
				result.append( nearbyShipState.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nNearby Ship AI...\n" );
			if ( nearbyShipAI != null ) {
				result.append( nearbyShipAI.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nEnvironment Hazards...\n" );
			if ( environment != null ) {
				result.append( environment.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nProjectiles...\n" );
			int projectileIndex = 0;
			first = true;
			for ( ProjectileState projectile : projectileList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "Projectile # %2d:\n", projectileIndex++ ) );
				result.append( projectile.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );
			result.append( String.format( "Nu?:          %11d (Player Ship)\n", unknownNu ) );
			result.append( String.format( "Xi?:          %11s (Nearby Ship)\n", (unknownXi != null ? unknownXi.intValue() : "N/A") ) );
			result.append( String.format( "Autofire:           %5b\n", autofire ) );

			result.append( "\nRebel Flagship...\n" );
			if ( rebelFlagshipState != null ) {
				result.append( rebelFlagshipState.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nMystery Bytes...\n" );
			first = true;
			for ( MysteryBytes m : mysteryList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( m.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			// ...
			return result.toString();
		}
	}


	public static class ShipState {
		private String shipName = null;
		private String shipBlueprintId = null;
		private String shipLayoutId = null;
		private String shipGfxBaseName = null;
		private boolean auto = false;

		private List<StartingCrewState> startingCrewList = new ArrayList<StartingCrewState>();
		private boolean hostile = false;
		private int jumpChargeTicks = 0;
		private boolean jumping = false;
		private int jumpAnimTicks = 0;
		private int hullAmt = 0, fuelAmt = 0, dronePartsAmt = 0, missilesAmt = 0, scrapAmt = 0;
		private List<CrewState> crewList = new ArrayList<CrewState>();
		private int reservePowerCapacity = 0;
		private Map<SystemType, List<SystemState>> systemsMap = new LinkedHashMap<SystemType, List<SystemState>>();
		private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<ExtendedSystemInfo>();
		private List<RoomState> roomList = new ArrayList<RoomState>();
		private Map<XYPair, Integer> breachMap = new LinkedHashMap<XYPair, Integer>();
		private Map<DoorCoordinate, DoorState> doorMap = new LinkedHashMap<DoorCoordinate, DoorState>();
		private int cloakAnimTicks = 0;
		private List<LockdownCrystal> lockdownCrystalList = new ArrayList<LockdownCrystal>();
		private List<WeaponState> weaponList = new ArrayList<WeaponState>();
		private List<DroneState> droneList = new ArrayList<DroneState>();
		private List<String> augmentIdList = new ArrayList<String>();
		private List<StandaloneDroneState> standaloneDroneList = new ArrayList<StandaloneDroneState>();


		/**
		 * Constructs an incomplete ShipState.
		 *
		 * It will need systems, reserve power, rooms, doors, and supplies.
		 */
		public ShipState( String shipName, ShipBlueprint shipBlueprint, boolean auto ) {
			this( shipName, shipBlueprint.getId(), shipBlueprint.getLayoutId(), shipBlueprint.getGraphicsBaseName(), auto );
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
			ShipLayout shipLayout = DataManager.get().getShipLayout( shipBlueprint.getLayoutId() );

			// Systems.
			systemsMap.clear();
			int powerRequired = 0;
			for ( SystemType systemType : SystemType.values() ) {
				SystemState systemState = new SystemState( systemType );

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
			for ( int r=0; r < shipLayout.getRoomCount(); r++ ) {
				ShipLayoutRoom layoutRoom = shipLayout.getRoom( r );
				int squaresH = layoutRoom.squaresH;
				int squaresV = layoutRoom.squaresV;

				RoomState roomState = new RoomState();
				for ( int s=0; s < squaresH * squaresV; s++ ) {
					roomState.addSquare( new SquareState( 0, 0, -1 ) );
				}
				addRoom( roomState );
			}

			// Doors.
			getDoorMap().clear();
			Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
			for ( Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : layoutDoorMap.entrySet() ) {
				DoorCoordinate doorCoord = entry.getKey();

				setDoor( doorCoord.x, doorCoord.y, doorCoord.v, new DoorState() );
			}

			// Augments.
			getAugmentIdList().clear();
			if ( shipBlueprint.getAugments() != null ) {
				for ( ShipBlueprint.AugmentId augId : shipBlueprint.getAugments() ) {
					addAugmentId( augId.name );
				}
			}

			// Supplies.
			setHullAmt( shipBlueprint.getHealth().amount );
			setFuelAmt( 20 );
			setDronePartsAmt( 0 );
			setMissilesAmt( 0 );
			if ( shipBlueprint.getDroneList() != null ) {
				setDronePartsAmt( shipBlueprint.getDroneList().drones );
			}
			if ( shipBlueprint.getWeaponList() != null ) {
				setMissilesAmt( shipBlueprint.getWeaponList().missiles );
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This should be called when turning a nearby ship into a player ship.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public void commandeer() {
			setHostile( false );
			setJumpChargeTicks( 0 );
			setJumping( false );
			setJumpAnimTicks( 0 );

			for ( CrewState crew : getCrewList() ) {
				crew.commandeer();
			}

			for ( Map.Entry<SystemType, List<SystemState>> entry : getSystemsMap().entrySet() ) {
				for ( SystemState s : entry.getValue() ) {
					s.commandeer();
				}
			}

			for ( ExtendedSystemInfo info : getExtendedSystemInfoList() ) {
				info.commandeer();
			}

			for ( DoorState door : getDoorMap().values() ) {
				door.commandeer();
			}

			setCloakAnimTicks( 0 );
			getLockdownCrystalList().clear();

			for ( WeaponState weapon : getWeaponList() ) {
				weapon.commandeer();
			}

			for ( DroneState drone : getDroneList() ) {
				drone.commandeer();
			}

			getStandaloneDroneList().clear();
		}

		public void setShipName( String s ) { shipName = s; }
		public String getShipName() { return shipName; }

		public void setShipBlueprintId( String shipBlueprintId ) {
			this.shipBlueprintId = shipBlueprintId;
		}
		public String getShipBlueprintId() { return shipBlueprintId; }

		public void setShipLayoutId( String shipLayoutId ) { this.shipLayoutId = shipLayoutId; }
		public String getShipLayoutId() { return shipLayoutId; }

		/**
		 * Sets the basename to use when loading ship images.
		 *
		 * It often resembles the layout id, but they're not interchangeable.
		 * The proper shipLayoutId comes from the ShipBlueprint.
		 *
		 * FTL 1.01-1.03.3 used the following path for all ships.
		 * FTL 1.5.4 used this path for player ships only.
		 * "img/ship/{shipGfxBaseName}_base.png"
		 *
		 * FTL 1.5.4 used the following path for enemy ships only.
		 * "img/ships_glow/{shipGfxBaseName}_base.png"
		 *
		 * The basename is combined with suffixes:
		 *   _base = Background hull layer.
		 *   _cloak = Foreground hull layer when cloaked.
		 *   _floor = Decorative floorplan outline layer.
		 *   _gib[0-9] = Post-destruction hull fragments.
		 *
		 * The "_floor" image is displayed below the programmatically painted
		 * grid of squares. It is typically only present for player ships.
		 *
		 * Observed values: jelly_croissant_pirate, rebel_long_pirate.
		 */
		public void setShipGraphicsBaseName( String shipGfxBaseName ) {
			this.shipGfxBaseName = shipGfxBaseName;
		}
		public String getShipGraphicsBaseName() { return shipGfxBaseName; }

		/**
		 * Sets whether this is a randomized NPC ship or player ship.
		 */
		public void setAuto( boolean b ) { auto = b; }
		public boolean isAuto() { return auto; }

		/**
		 * Toggles whether this ship is hostile or neutral.
		 *
		 *
		 * Neutral ships hide their floorplans unless a player-controlled crew
		 * is on board. They also don't attack, of course.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setHostile( boolean b ) { hostile = b; }
		public boolean isHostile() { return hostile; }

		/**
		 * Sets time elapsed while waiting for the FTL drive to charge.
		 *
		 * This counts to 85000.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setJumpChargeTicks( int n ) { jumpChargeTicks = n; }
		public int getJumpChargeTicks() { return jumpChargeTicks; }

		/**
		 * Toggles whether this ship is currently jumping away.
		 *
		 * If true, this ship will fade out immediately upon loading. If
		 * paused, the animation will play anyway, but the ship will still be
		 * present, albeit invisible. Once unpaused, an event popup will
		 * appear in response to the ship's departure.
		 *
		 * This value is ignored on player ships.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setJumpAnimTicks(int)
		 */
		public void setJumping( boolean b ) { jumping = b; }
		public boolean isJumping() { return jumping; }

		/**
		 * Sets time elapsed while jumping away.
		 *
		 * This counts from 0 (normal) to 2000 (gone).
		 *
		 * This value is ignored on player ships - and on nearby ships which
		 * aren't currently jumping.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setJumping(boolean)
		 */
		public void setJumpAnimTicks( int n ) { jumpAnimTicks = n; }
		public int getJumpAnimTicks() { return jumpAnimTicks; }

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

		/**
		 * Adds an entry to the list of starting crew.
		 *
		 * When the player flees a nearby ship and later revisits, the
		 * starting crew are respawned. This list has no apparent effect on the
		 * player ship, except to memorialize the crew decided in the hangar at
		 * the start of the campaign.
		 */
		public void addStartingCrewMember( StartingCrewState sc ) {
			startingCrewList.add( sc );
		}

		public List<StartingCrewState> getStartingCrewList() {
			return startingCrewList;
		}

		public void addCrewMember( CrewState c ) {
			crewList.add( c );
		}
		public CrewState getCrewMember( int n ) {
			return crewList.get( n );
		}

		public List<CrewState> getCrewList() { return crewList; }


		/**
		 * Sets the reserve power capacity, which systems draw upon.
		 *
		 * Unlike SystemStates, there is no explicit field for temporary limits.
		 * A hack-disrupted battery will cause a temporary loss of 2. And an
		 * event, whose xml includes an "environment" tag with the "type=storm"
		 * attribute, causes a temporary divisor of 2.
		 *
		 * @see SavedGameState.setSectorLayoutSeed(int)
		 */
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


		public void addExtendedSystemInfo( ExtendedSystemInfo info ) {
			extendedSystemInfoList.add( info );
		}

		public void setExtendedSystemInfoList( List<ExtendedSystemInfo> extendedSystemInfoList ) { this.extendedSystemInfoList = extendedSystemInfoList; }
		public List<ExtendedSystemInfo> getExtendedSystemInfoList() { return extendedSystemInfoList; }

		public <T extends ExtendedSystemInfo> List<T> getExtendedSystemInfoList( Class<T> infoClass ) {
			List<T> result = new ArrayList<T>( 1 );
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if ( infoClass.isInstance( info ) ) {
					result.add( infoClass.cast( info ) );
				}
			}
			return result;
		}

		/**
		 * Returns the first extended system info of a given class, or null.
		 */
		public <T extends ExtendedSystemInfo> T getExtendedSystemInfo( Class<T> infoClass ) {
			T result = null;
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if ( infoClass.isInstance( info ) ) {
					result = infoClass.cast( info );
					break;
				}
			}
			return result;
		}


		public void addRoom( RoomState r ) {
			roomList.add(r);
		}
		public RoomState getRoom( int roomId ) {
			return roomList.get( roomId );
		}

		public List<RoomState> getRoomList() { return roomList; }


		/**
		 * Adds a hull breach.
		 *
		 * @param x the 0-based Nth floor-square from the left (plus ShipLayout X_OFFSET)
		 * @param y the 0-based Nth floor-square from the top (plus ShipLayout Y_OFFSET)
		 * @param breachHealth 0-100.
		 */
		public void setBreach( int x, int y, int breachHealth ) {
			breachMap.put( new XYPair( x, y ), breachHealth );
		}

		public Map<XYPair, Integer> getBreachMap() { return breachMap; }


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
			DoorCoordinate doorCoord = new DoorCoordinate( wallX, wallY, vertical );
			doorMap.put( doorCoord, d );
		}
		public DoorState getDoor( int wallX, int wallY, int vertical ) {
			DoorCoordinate doorCoord = new DoorCoordinate( wallX, wallY, vertical );
			return doorMap.get( doorCoord );
		}

		/**
		 * Returns the map containing this ship's door states.
		 *
		 * Do not rely on the keys' order. ShipLayout config files have a
		 * different order than saved game files.Entries will be in whatever
		 * order setDoor was called, which generally will be in the saved game
		 * file's order.
		 */
		public Map<DoorCoordinate, DoorState> getDoorMap() { return doorMap; }


		/**
		 * Sets visibility of the ship's cloak image.
		 *
		 * This counts from 0 (uncloaked) to 500 (cloaked) and back.
		 *
		 * Presumably, this is stored separately from CloakingInfo in case the
		 * Cloaking system is uninstalled while still cloaked!?
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setCloakAnimTicks( int n ) { cloakAnimTicks = n; }
		public int getCloakAnimTicks() { return cloakAnimTicks; }


		/**
		 * Sets deployed lockdown crystals.
		 *
		 * This was introduced in FTL 1.5.12.
		 */
		public void setLockdownCrystalList( List<LockdownCrystal> crystalList ) { lockdownCrystalList = crystalList; }
		public List<LockdownCrystal> getLockdownCrystalList() { return lockdownCrystalList; }


		public void addWeapon( WeaponState w ) {
			weaponList.add( w );
		}

		public List<WeaponState> getWeaponList() { return weaponList; }


		public void addDrone( DroneState d ) {
			droneList.add( d );
		}

		public List<DroneState> getDroneList() { return droneList; }


		public void addAugmentId( String augmentId ) {
			augmentIdList.add( augmentId );
		}

		public List<String> getAugmentIdList() { return augmentIdList; }


		/**
		 * Adds a standalone surge drone.
		 *
		 * TODO: See what happens when standalone drones are added to ships that
		 * aren't rebel flagships.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void addStandaloneDrone( StandaloneDroneState standaloneDrone ) {
			standaloneDroneList.add( standaloneDrone );
		}

		public List<StandaloneDroneState> getStandaloneDroneList() { return standaloneDroneList; }


		@Override
		public String toString() {
			// The blueprint fetching might vary if auto == true.
			// See "autoBlueprints.xml" vs "blueprints.xml".
			ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBlueprintId );
			ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

			ShipLayout shipLayout = DataManager.get().getShipLayout( shipLayoutId );
			if ( shipLayout == null )
				throw new RuntimeException( String.format( "Could not find layout for%s ship: %s", (auto ? " auto" : ""), shipName ) );

			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append( String.format( "Ship Name:    %s\n", shipName ) );
			result.append( String.format( "Ship Type:    %s\n", shipBlueprintId ) );
			result.append( String.format( "Ship Layout:  %s\n", shipLayoutId ) );
			result.append( String.format( "Gfx BaseName: %s\n", shipGfxBaseName ) );

			result.append( "\nSupplies...\n" );
			result.append( String.format( "Hull:        %3d\n", hullAmt ) );
			result.append( String.format( "Fuel:        %3d\n", fuelAmt ) );
			result.append( String.format( "Drone Parts: %3d\n", dronePartsAmt ) );
			result.append( String.format( "Missiles:    %3d\n", missilesAmt ) );
			result.append( String.format( "Scrap:       %3d\n", scrapAmt ) );
			result.append( "\n" );
			result.append( String.format( "Hostile:           %7b\n", hostile ) );
			result.append( String.format( "Jump Charge Ticks: %7d (85000 is fully charged)\n", jumpChargeTicks ) );
			result.append( String.format( "Jumping:           %7b\n", jumping ) );
			result.append( String.format( "Jump Anim Ticks:   %7d (0=Normal to 2000=Gone)\n", jumpAnimTicks ) );

			result.append( "\nStarting Crew...\n" );
			first = true;
			for ( StartingCrewState sc : startingCrewList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( sc.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nCurrent Crew...\n" );
			first = true;
			for ( CrewState c : crewList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( c.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nSystems...\n" );
			result.append( String.format( "  Reserve Power Capacity: %2d\n", reservePowerCapacity ) );
			result.append( "\n" );
			first = true;
			for ( Map.Entry<SystemType, List<SystemState>> entry : systemsMap.entrySet() ) {
				for ( SystemState s : entry.getValue() ) {
					if ( first ) { first = false; }
					else { result.append( ",\n" ); }
					result.append( s.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
				}
			}

			result.append( "\nExtended System Info...\n" );
			first = true;
			for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( info.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nRooms...\n" );
			first = true;
			for (ListIterator<RoomState> it=roomList.listIterator(); it.hasNext(); ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				int roomId = it.nextIndex();

				SystemType systemType = blueprintSystems.getSystemTypeByRoomId( roomId );
				String systemId = (systemType != null) ? systemType.getId() : "empty";

				result.append( String.format( "Room Id: %2d (%s)\n", roomId, systemId ) );
				result.append( it.next().toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nHull Breaches...\n" );
			int breachId = -1;
			first = true;
			for ( Map.Entry<XYPair, Integer> entry : breachMap.entrySet() ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }

				XYPair breachCoord = entry.getKey();
				int breachHealth = entry.getValue().intValue();

				result.append( String.format( "BreachId: %2d, Raw Coords: %2d,%2d (-Layout Offset: %2d,%2d)\n", ++breachId, breachCoord.x, breachCoord.y, breachCoord.x-shipLayout.getOffsetX(), breachCoord.y-shipLayout.getOffsetY() ) );
				result.append( String.format( "  Breach HP: %3d\n", breachHealth ) );
			}

			result.append( "\nDoors...\n" );
			int doorId = -1;
			first = true;
			for ( Map.Entry<DoorCoordinate, DoorState> entry : doorMap.entrySet() ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }

				DoorCoordinate doorCoord = entry.getKey();
				DoorState d = entry.getValue();
				String orientation = ( doorCoord.v == 1 ) ? "V" : "H";

				result.append( String.format( "DoorId: %2d (%2d,%2d,%2s)\n", ++doorId, doorCoord.x, doorCoord.y, orientation ) );
				result.append( d.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( String.format( "\nCloak Anim Ticks:  %3d (0=Uncloaked to 500=Cloaked)\n", cloakAnimTicks ) );

			result.append( "\nLockdown Crystals...\n" );
			first = true;
			for ( LockdownCrystal c : lockdownCrystalList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( c.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nWeapons...\n" );
			first = true;
			for ( WeaponState w : weaponList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( w.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nDrones...\n" );
			first = true;
			for ( DroneState d : droneList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( d.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}


			result.append( "\nStandalone Drones... (Surge)\n" );
			int standaloneDroneIndex = 0;
			first = true;
			for ( StandaloneDroneState standaloneDrone : standaloneDroneList ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "Surge Drone # %2d:\n", standaloneDroneIndex++ ) );
				result.append( standaloneDrone.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nAugments...\n" );
			for ( String augmentId : augmentIdList ) {
				result.append( String.format( "AugmentId: %s\n", augmentId ) );
			}

			return result.toString();
		}
	}



	public static class StartingCrewState {

		private String name = "Frank";
		private CrewType race = CrewType.HUMAN;


		public StartingCrewState() {
		}

		public void setName( String s ) { name = s; }
		public String getName() { return name; }

		public void setRace( CrewType race ) { this.race = race; }
		public CrewType getRace() { return race; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append( String.format( "Name: %s\n", name ) );
			result.append( String.format( "Race: %s\n", race.getId() ) );
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
				if ( race.getId().equals( id ) ) return race;
			}
			return null;
		}
	}

	/**
	 * Crew.
	 *
	 * Zoltan-produced power is not stored in SystemState.
	 *
	 * TODO: Disrupting Mind system stuns and turns enemy crew against each
	 * other, yet doesn't seem to modify CrewStates!?
	 */
	public static class CrewState {

		private String name = "Frank";
		private CrewType race = CrewType.HUMAN;
		private boolean enemyBoardingDrone = false;
		private int health = 0;
		private int spriteX = 0, spriteY = 0;
		private int roomId = -1;
		private int roomSquare = -1;
		private boolean playerControlled = false;
		private int cloneReady = 0;
		private List<Integer> spriteTintIndeces = new ArrayList<Integer>();
		private boolean mindControlled = false;
		private int savedRoomId = 0;
		private int savedRoomSquare = 0;
		private int pilotSkill = 0, engineSkill = 0, shieldSkill = 0;
		private int weaponSkill = 0, repairSkill = 0, combatSkill = 0;
		private boolean male = true;
		private int repairs = 0;
		private int combatKills = 0;
		private int pilotedEvasions = 0;
		private int jumpsSurvived = 0;
		private int skillMasteriesEarned = 0;
		private int stunTicks = 0;
		private int healthBoost = 0;
		private int clonebayPriority = -1;
		private int damageBoost = 1000;
		private int unknownLambda = 0;
		private int universalDeathCount = 0;
		private boolean pilotMasteryOne = false, pilotMasteryTwo = false;
		private boolean engineMasteryOne = false, engineMasteryTwo = false;
		private boolean shieldMasteryOne = false, shieldMasteryTwo = false;
		private boolean weaponMasteryOne = false, weaponMasteryTwo = false;
		private boolean repairMasteryOne = false, repairMasteryTwo = false;
		private boolean combatMasteryOne = false, combatMasteryTwo = false;
		private boolean unknownNu = false;
		private AnimState teleportAnim = new AnimState();
		private boolean unknownPhi = false;
		private int lockdownRechargeTicks = 0;
		private int lockdownRechargeTicksGoal = 0;
		private int unknownOmega = 0;


		/**
		 * Constructor.
		 */
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
			cloneReady = srcCrew.getCloneReady();

			for ( Integer colorIndex : srcCrew.getSpriteTintIndeces() ) {
				// Integer wrapper is immutable, no need for defensive copying.
				spriteTintIndeces.add( colorIndex );
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
			skillMasteriesEarned = srcCrew.getSkillMasteriesEarned();
			stunTicks = srcCrew.getStunTicks();
			healthBoost = srcCrew.getHealthBoost();
			clonebayPriority = srcCrew.getClonebayPriority();
			damageBoost = srcCrew.getDamageBoost();
			unknownLambda = srcCrew.getUnknownLambda();
			universalDeathCount = srcCrew.getUniversalDeathCount();
			pilotMasteryOne = srcCrew.getPilotMasteryOne();
			pilotMasteryTwo = srcCrew.getPilotMasteryTwo();
			engineMasteryOne = srcCrew.getEngineMasteryOne();
			engineMasteryTwo = srcCrew.getEngineMasteryTwo();
			shieldMasteryOne = srcCrew.getShieldMasteryOne();
			shieldMasteryTwo = srcCrew.getShieldMasteryTwo();
			weaponMasteryOne = srcCrew.getWeaponMasteryOne();
			weaponMasteryTwo = srcCrew.getWeaponMasteryTwo();
			repairMasteryOne = srcCrew.getRepairMasteryOne();
			repairMasteryTwo = srcCrew.getRepairMasteryTwo();
			combatMasteryOne = srcCrew.getCombatMasteryOne();
			combatMasteryTwo = srcCrew.getCombatMasteryTwo();
			unknownNu = srcCrew.getUnknownNu();
			teleportAnim = srcCrew.getTeleportAnim();
			unknownPhi = srcCrew.getUnknownPhi();
			lockdownRechargeTicks = srcCrew.getLockdownRechargeTicks();
			lockdownRechargeTicksGoal = srcCrew.getLockdownRechargeTicksGoal();
			unknownOmega = srcCrew.getUnknownOmega();
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Normal Crew will NOT have their playerControlled status toggled.
		 *
		 * Crew flagged as enemy boarding drones will remain so; when a nearby
		 * ship becomes the player ship, such crew, which formerly belonged to
		 * the player, will then be hostile to the player. Their playerControlled
		 * status will be set to false, as FTL woulld set it on the player ship.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public void commandeer() {

			if ( isEnemyBoardingDrone() ) {
				setPlayerControlled( false );
			}

			setCloneReady( 0 );  // TODO: Vet this default.
			setMindControlled( false );

			setSavedRoomId( -1 );
			setSavedRoomSquare( -1 );

			setStunTicks( 0 );

			if ( getHealthBoost() > 0 ) {
				setHealth( getHealth() - getHealthBoost() );
				setHealthBoost( 0 );
			}

			setDamageBoost( 0 );
			setUnknownLambda( 0 );               // TODO: Vet this default;
			setUnknownNu( false );               // TODO: Vet this default;

			getTeleportAnim().setPlaying( false );
			getTeleportAnim().setCurrentFrame( 0 );
			getTeleportAnim().setProgressTicks( 0 );

			setUnknownPhi( false );              // TODO: Vet this default;
			setLockdownRechargeTicks( 0 );
			setLockdownRechargeTicksGoal( 0 );
			setUnknownOmega( 0 );                // TODO: Vet this default;
		}


		public void setName( String s ) { name = s; }
		public String getName() { return name; }

		public void setRace( CrewType race ) { this.race = race; }
		public CrewType getRace() { return race; }

		/**
		 * Sets whether this crew member is a hostile boarding drone.
		 *
		 * Upon loading after setting this on a crew member, name will change
		 * to "Anti-Personnel Drone", race will be "battle", and
		 * playerControlled will be false on the player ship or true on a
		 * nearby ship.
		 *
		 * If after loading in-game, you re-edit this to false and leave the
		 * "battle" race, the game will change it to "human".
		 *
		 * Drones on nearby ships (which are playerControlled) will not be
		 * preserved the next time the game saves, even if you modify the
		 * player ship's drone list to have an armed boarder.
		 *
		 * Presumably this is so intruders can persist without a ship, which
		 * would normally have a drones section to contain them.
		 *
		 * TODO: Jump away from Boss #2 to see what its drone is
		 * ("blueprints.xml" mentions BOARDER_BOSS).
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
		 * @see #setHealthBoost(int)
		 */
		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		/**
		 * Sets the position of the crew's image.
		 *
		 * Technically the roomId/square fields set the crew's goal location.
		 * This field is where the body really is, possibly en route.
		 *
		 * It's the position of the body image's center, relative to the
		 * top-left corner of the floor layout of the ship it's on.
		 *
		 * For preserved dead crew, which have no body, this lingers, or may be
		 * (0,0).
		 */
		public void setSpriteX( int n ) { spriteX = n; }
		public void setSpriteY( int n ) { spriteY = n; }
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

		/**
		 * Unknown.
		 *
		 * Matthew's hint: He says it's an int.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setCloneReady( int n ) { cloneReady = n; }
		public int getCloneReady() { return cloneReady; }

		/**
		 * Sets a list of tints to apply to the sprite.
		 *
		 * The tints themselves are defined in
		 * "blueprints.xml":crewBlueprint/colorList
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
		 * engi=0(0), human=2(2,4), mantis=1(9), rock=1(7), slug=1(8).
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
		 * This was introduced in FTL 1.5.4.
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
		 *
		 * Humans with this set to false have a female image. Other races
		 * accept the flag but use the same image as male.
		 *
		 * No Andorians in the game, so this is only a two-state boolean.
		 */
		public void setMale( boolean b ) { male = b; }
		public boolean isMale() { return male; }

		public void setRepairs( int n ) { repairs = n; }
		public int getRepairs() { return repairs; }

		public void setCombatKills( int n ) { combatKills = n; }
		public int getCombatKills() { return combatKills; }

		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public int getPilotedEvasions() { return pilotedEvasions; }

		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }
		public int getJumpsSurvived() { return jumpsSurvived; }

		/**
		 * Sets the total number of skill masteries ever earned by this crew.
		 *
		 * This is incremented when any skill reaches the first or second
		 * mastery interval. So this is intended to max out at 12.
		 *
		 * Bug: In FTL 1.5.4-1.5.10, Clonebay skill penalties allowed crew to
		 * re-earn masteries and increment further.
		 *
		 * FTL 1.5.12 added methods to track each level of each skill
		 * individually.
		 *
		 * @see #setPilotMasteryOne(int)
		 */
		public void setSkillMasteriesEarned( int n ) { skillMasteriesEarned = n; }
		public int getSkillMasteriesEarned() { return skillMasteriesEarned; }

		/**
		 * Sets time elapsed while waiting for a stun effect to wear off.
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
		 * Observed values:
		 *   15 = Mind Ctrl Level 2.
		 *   30 = Mind Ctrl Level 3.
		 *
		 * When the mind control effect expires, the boost amount will be
		 * subtracted from health, and this value will reset to 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setHealthBoost( int n ) { healthBoost = n; }
		public int getHealthBoost() { return healthBoost; }

		/**
		 * Sets the Clonebay's queue priority for this crew (lowest is first).
		 *
		 * When this crew dies, this is set to the newly incremented universal
		 * death count. Then this value lingers. When this crew has not yet
		 * died, this is -1.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setUniversalDeathCount(int)
		 */
		public void setClonebayPriority( int n ) { clonebayPriority = n; }
		public int getClonebayPriority() { return clonebayPriority; }

		/**
		 * Sets a multiplier to apply to damage dealt by this crew.
		 *
		 * Observed values:
		 *   1250 (1.25) = Mind Ctrl Level 2.
		 *   2000 (2.00) = Mind Ctrl Level 3.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @param a pseudo-float (1000 is 1.0)
		 */
		public void setDamageBoost( int n ) { damageBoost = n; }
		public int getDamageBoost() { return damageBoost; }

		/**
		 * Unknown.
		 *
		 * Matthew's hint: dyingTimer.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public int getUnknownLambda() { return unknownLambda; }

		/**
		 * Sets the total deaths of all crew everywhere.
		 *
		 * All crew, friend and foe, have an identical field, which increments
		 * whenever someone dies. When nobody has died yet, this is 0.
		 *
		 * According to Matthew, FTL made this a static variable on the crew
		 * class. It's purpose was to serve as an ever-increasing number to mark
		 * the deceased with a unique Clonebay queue priority to sort by.
		 * There's a comment in FTL's source that says "This is stupid."
		 *
		 * Note: Any time this is is set, the same value should be set on all
		 * CrewStates.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setClonebayPriority(int)
		 */
		public void setUniversalDeathCount( int n ) { universalDeathCount = n; }
		public int getUniversalDeathCount() { return universalDeathCount; }

		/**
		 * Sets whether this crew ever earned the first level of pilot mastery.
		 *
		 * This value does not affect the in-game progress bars. It's probably
		 * solely for the "Most Skill Masteries" stat.
		 *
		 * This was introduced in FTL 1.5.12.
		 *
		 * @see #setSkillMasteries(int)
		 * @see net.blerf.ftl.model.Stats.StatType#MOST_SKILL_MASTERIES
		 */
		public void setPilotMasteryOne( boolean b ) { pilotMasteryOne = b; }
		public void setPilotMasteryTwo( boolean b ) { pilotMasteryTwo = b; }
		public void setEngineMasteryOne( boolean b ) { engineMasteryOne = b; }
		public void setEngineMasteryTwo( boolean b ) { engineMasteryTwo = b; }
		public void setShieldMasteryOne( boolean b ) { shieldMasteryOne = b; }
		public void setShieldMasteryTwo( boolean b ) { shieldMasteryTwo = b; }
		public void setWeaponMasteryOne( boolean b ) { weaponMasteryOne = b; }
		public void setWeaponMasteryTwo( boolean b ) { weaponMasteryTwo = b; }
		public void setRepairMasteryOne( boolean b ) { repairMasteryOne = b; }
		public void setRepairMasteryTwo( boolean b ) { repairMasteryTwo = b; }
		public void setCombatMasteryOne( boolean b ) { combatMasteryOne = b; }
		public void setCombatMasteryTwo( boolean b ) { combatMasteryTwo = b; }

		public boolean getPilotMasteryOne() { return pilotMasteryOne; }
		public boolean getPilotMasteryTwo() { return pilotMasteryTwo; }
		public boolean getEngineMasteryOne() { return engineMasteryOne; }
		public boolean getEngineMasteryTwo() { return engineMasteryTwo; }
		public boolean getShieldMasteryOne() { return shieldMasteryOne; }
		public boolean getShieldMasteryTwo() { return shieldMasteryTwo; }
		public boolean getWeaponMasteryOne() { return weaponMasteryOne; }
		public boolean getWeaponMasteryTwo() { return weaponMasteryTwo; }
		public boolean getRepairMasteryOne() { return repairMasteryOne; }
		public boolean getRepairMasteryTwo() { return repairMasteryTwo; }
		public boolean getCombatMasteryOne() { return combatMasteryOne; }
		public boolean getCombatMasteryTwo() { return combatMasteryTwo; }

		/**
		 * Unknown.
		 *
		 * Went from 0 to 1 while fresh clone materialized via the teleport
		 * anim.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownNu( boolean b ) { unknownNu = b; }
		public boolean getUnknownNu() { return unknownNu; }

		/**
		 * Sets the crew's teleport anim state.
		 *
		 * After cloning, this plays as the new body spawns.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setTeleportAnim( AnimState anim ) { teleportAnim = anim; }
		public AnimState getTeleportAnim() { return teleportAnim; }

		/**
		 * Unknown.
		 *
		 * Related to walking/fighting?
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownPhi( boolean b ) { unknownPhi = b; }
		public boolean getUnknownPhi() { return unknownPhi; }

		/**
		 * Sets time elapsed waiting for the lockdown ability to recharge.
		 *
		 * In 1.5.13, this was observed at 50000 when the game started.
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
		 * In 1.5.13, this was observed at 50000 when the game started.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setLockdownRechargeTicks(int)
		 */
		public void setLockdownRechargeTicksGoal( int n ) { lockdownRechargeTicksGoal = n; }
		public int getLockdownRechargeTicksGoal() { return lockdownRechargeTicksGoal; }

		/**
		 * Unknown.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownOmega( int n ) { unknownOmega = n; }
		public int getUnknownOmega() { return unknownOmega; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			CrewBlueprint crewBlueprint = DataManager.get().getCrew( race.getId() );

			List<CrewBlueprint.SpriteTintLayer> tintLayerList = null;
			if ( crewBlueprint != null ) {
				tintLayerList = crewBlueprint.getSpriteTintLayerList();
			}

			result.append( String.format( "Name:                   %s\n", name ) );
			result.append( String.format( "Race:                   %s\n", race.getId() ) );
			result.append( String.format( "Enemy Drone:            %5b\n", enemyBoardingDrone ) );
			result.append( String.format( "Sex:                    %s\n", (male ? "Male" : "Female") ) );
			result.append( String.format( "Health:                 %5d\n", health));
			result.append( String.format( "Sprite Position:          %3d,%3d\n", spriteX, spriteY ) );
			result.append( String.format( "Room Id:                %5d\n", roomId ) );
			result.append( String.format( "Room Square:            %5d\n", roomSquare ) );
			result.append( String.format( "Player Controlled:      %5b\n", playerControlled ) );
			result.append( String.format( "Clone Ready?:           %5d\n", cloneReady ) );
			result.append( String.format( "Mind Controlled:        %5b\n", mindControlled ) );

			result.append( "\nSprite Tints...\n" );
			for ( int i=0; i < spriteTintIndeces.size(); i++ ) {
				Integer colorIndex = spriteTintIndeces.get( i );

				String colorHint = null;
				if ( tintLayerList != null && i < tintLayerList.size() ) {
					CrewBlueprint.SpriteTintLayer tintLayer = tintLayerList.get( i );
					if ( tintLayer.tintList != null && colorIndex < tintLayer.tintList.size() && colorIndex >= 0 ) {
						CrewBlueprint.SpriteTintLayer.SpriteTintColor tintColor = tintLayer.tintList.get( colorIndex );
						colorHint = String.format( "r=%3d,g=%3d,b=%3d,a=%.1f", tintColor.r, tintColor.g, tintColor.b, tintColor.a );
					} else {
						colorHint = "Color not in blueprint's layer.";
					}
				} else {
					colorHint = "Layer not in blueprint's colorList.";
				}

				result.append( String.format( "  Layer %2d: Color: %3d (%s)\n", i, colorIndex, colorHint ) );
			}
			result.append( "\n" );

			FTLConstants origConstants = new OriginalFTLConstants();
			FTLConstants advConstants = new AdvancedFTLConstants();

			result.append( String.format( "Saved Room Id:          %5d\n", savedRoomId));
			result.append( String.format( "Saved Room Square:      %5d\n", savedRoomSquare));
			result.append( String.format( "Pilot Skill:            %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", pilotSkill, origConstants.getMasteryIntervalPilot( race ), advConstants.getMasteryIntervalPilot( race ) ) );
			result.append( String.format( "Engine Skill:           %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", engineSkill, origConstants.getMasteryIntervalEngine( race ), advConstants.getMasteryIntervalEngine( race ) ) );
			result.append( String.format( "Shield Skill:           %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", shieldSkill, origConstants.getMasteryIntervalShield( race ), advConstants.getMasteryIntervalShield( race ) ) );
			result.append( String.format( "Weapon Skill:           %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", weaponSkill, origConstants.getMasteryIntervalWeapon( race ), advConstants.getMasteryIntervalWeapon( race ) ) );
			result.append( String.format( "Repair Skill:           %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", repairSkill, origConstants.getMasteryIntervalRepair( race ), advConstants.getMasteryIntervalRepair( race ) ) );
			result.append( String.format( "Combat Skill:           %5d (Mastery Interval: %2d in FTL:AE, Originally %2d)\n", combatSkill, origConstants.getMasteryIntervalCombat( race ), advConstants.getMasteryIntervalCombat( race ) ) );
			result.append( String.format( "Repairs:                %5d\n", repairs ) );
			result.append( String.format( "Combat Kills:           %5d\n", combatKills ) );
			result.append( String.format( "Piloted Evasions:       %5d\n", pilotedEvasions ) );
			result.append( String.format( "Jumps Survived:         %5d\n", jumpsSurvived ) );
			result.append( String.format( "Skill Masteries Earned: %5d\n", skillMasteriesEarned ) );
			result.append( String.format( "Stun Ticks:            %6d (Decrements to 0)\n", stunTicks ) );
			result.append( String.format( "Health Boost:          %6d (Subtracted from health when Mind Ctrl expires)\n", healthBoost ) );
			result.append( String.format( "Clonebay Priority:     %6d (On death, copies Universal Death Count for a big number)\n", clonebayPriority ) );
			result.append( String.format( "Damage Boost:          %6d (%5.03f)\n", damageBoost, damageBoost/1000f ) );
			result.append( String.format( "Dying Ticks?:          %6d\n", unknownLambda ) );
			result.append( String.format( "Universal Death Count:  %5d (Shared across crew everywhere, for assigning next Clonebay priority)\n", universalDeathCount ) );
			result.append( String.format( "Pilot Mastery (1,2):   %6b, %5b\n", pilotMasteryOne, pilotMasteryTwo ) );
			result.append( String.format( "Engine Mastery (1,2):  %6b, %5b\n", engineMasteryOne, engineMasteryTwo ) );
			result.append( String.format( "Shield Mastery (1,2):  %6b, %5b\n", shieldMasteryOne, shieldMasteryTwo ) );
			result.append( String.format( "Weapon Mastery (1,2):  %6b, %5b\n", weaponMasteryOne, weaponMasteryTwo ) );
			result.append( String.format( "Repair Mastery (1,2):  %6b, %5b\n", repairMasteryOne, repairMasteryTwo ) );
			result.append( String.format( "Combat Mastery (1,2):  %6b, %5b\n", combatMasteryOne, combatMasteryTwo ) );
			result.append( String.format( "Nu?:                   %6b\n", unknownNu ) );

			result.append( "\nTeleport Anim...\n" );
			if ( teleportAnim != null) {
				result.append( teleportAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );

			result.append( String.format( "Phi?:                  %6b\n", unknownPhi ) );
			result.append( String.format( "Lockdown Ticks:        %6d (Crystal only, time elapsed recharging ability)\n", lockdownRechargeTicks ) );
			result.append( String.format( "Lockdown Ticks Goal:   %6d (Crystal only, time needed to recharge)\n", lockdownRechargeTicksGoal ) );
			result.append( String.format( "Omega?:                %6d (Crystal only)\n", unknownOmega ) );
			return result.toString();
		}
	}


	/**
	 * Types of systems.
	 *
	 * FTL 1.5.4 introduced BATTERY, CLONEBAY, MIND, and HACKING.
	 */
	public static enum SystemType {
		// SystemType ids are tied to "img/icons/s_*_overlay.png" and store item ids.
		// TODO: Magic booleans.
		PILOT     ( "pilot",      true ),
		DOORS     ( "doors",      true ),
		SENSORS   ( "sensors",    true ),
		MEDBAY    ( "medbay",     false ),
		OXYGEN    ( "oxygen",     false ),
		SHIELDS   ( "shields",    false ),
		ENGINES   ( "engines",    false ),
		WEAPONS   ( "weapons",    false ),
		DRONE_CTRL( "drones",     false ),
		TELEPORTER( "teleporter", false ),
		CLOAKING  ( "cloaking",   false ),
		ARTILLERY ( "artillery",  false ),
		BATTERY   ( "battery",    true ),
		CLONEBAY  ( "clonebay",   false ),
		MIND      ( "mind",       false ),
		HACKING   ( "hacking",    false );

		private String id;
		private boolean subsystem;
		private SystemType( String id, boolean subsystem ) {
			this.id = id;
			this.subsystem = subsystem;
		}
		public String getId() { return id; }
		public boolean isSubsystem() { return subsystem; }
		public String toString() { return id; }

		public static SystemType findById( String id ) {
			for ( SystemType s : values() ) {
				if ( s.getId().equals( id ) ) return s;
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


		/**
		 * Constructor.
		 */
		public SystemState( SystemType systemType ) {
			this.systemType = systemType;
		}

		/**
		 * Copy constructor.
		 */
		public SystemState( SystemState srcSystem ) {
			systemType = srcSystem.getSystemType();
			capacity = srcSystem.getCapacity();
			power = srcSystem.getPower();
			damagedBars = srcSystem.getDamagedBars();
			ionizedBars = srcSystem.getIonizedBars();
			repairProgress = srcSystem.getRepairProgress();
			damageProgress = srcSystem.getDamageProgress();
			deionizationTicks = srcSystem.getDeionizationTicks();
			batteryPower = srcSystem.getBatteryPower();
			hackLevel = srcSystem.getHackLevel();
			hacked = srcSystem.isHacked();
			temporaryCapacityCap = srcSystem.getTemporaryCapacityCap();
			temporaryCapacityLoss = srcSystem.getTemporaryCapacityLoss();
			temporaryCapacityDivisor = srcSystem.getTemporaryCapacityDivisor();
		}

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		public void commandeer() {
			if ( !systemType.isSubsystem() ) {
				setPower( 0 );
			}
			// TODO: Find out if NOT resetting subsystem power is okay.
			// Otherwise, damage, etc. will need to be taken into account.

			setBatteryPower( 0 );
			setHackLevel( 0 );
			setHacked( false );
			setTemporaryCapacityCap( 1000 );
			setTemporaryCapacityLoss( 0 );
			setTemporaryCapacityDivisor( 1 );
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
		 * Power bars appear at the bottom of the stack.
		 *
		 * Note: For Weapons and DroneCtrl systems, this value ideally would
		 * be set whenever a weapon/drone is armed/disarmed and vice versa.
		 *
		 * FTL seems to recalculate Weapons system power upon loading, so this
		 * value can be sloppily set to 0 or ignored while editing.
		 *
		 * @see #setBatteryPower(int)
		 */
		public void setPower( int n ) { power = n; }
		public int getPower() { return power; }

		/**
		 * Sets the number of unusable power bars, in need of repair.
		 *
		 * Damaged bars appear at the top of the stack, painted over capacity
		 * limit bars.
		 */
		public void setDamagedBars( int n ) { damagedBars = n; }
		public int getDamagedBars() { return damagedBars; }

		/**
		 * Sets the number of ionized power bars.
		 *
		 * In-game when N ion damage is applied, up to N power/battery bars are
		 * deallocated. Any others remain to power the system, but for a short
		 * time, transfers to/from that system will not be possible.
		 *
		 * A countdown will repeat N times, decrementing this value to 0. N may
		 * exceed the total number of power bars on a system, to increase the
		 * total time required to remove all the 'ionized bars'.
		 *
		 * This should be less than or equal to MAX_IONIZED_BARS, or FTL's
		 * interface will be unable to find an image to display the number, and
		 * a warning graphic will appear.
		 *
		 * When a system disables itself (white lock), this will be -1. For
		 * the Cloaking system in FTL 1.01-1.03.3, setting this to -1 would
		 * engage the cloak. Systems which do not normally disable themselves
		 * will remain locked until they get hit with a weapon that produces
		 * ion damage. See ExtendedSystemInfo classes for timer fields that
		 * might used to unlock systems on their own.
		 *
		 * TODO: Teleporter has not been tested. AE systems have not been
		 * tested.
		 *
		 * @see net.blerf.ftl.constants.FTLConstants#getMaxIonizedBars()
		 */
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public int getIonizedBars() { return ionizedBars; }

		/**
		 * Sets progress toward repairing one damaged power bar.
		 *
		 * A growing portion of the bottommost damaged bar will turn yellow.
		 *
		 * Note: Repair progress and damage progress can both be non-zero at the
		 * same time. They affect different bars.
		 *
		 * @param n 0-100 (0 when not repairing)
		 */
		public void setRepairProgress( int n ) { repairProgress = n; }
		public int getRepairProgress() { return repairProgress; }

		/**
		 * Sets progress toward damaging one power bar.
		 *
		 * A growing portion of the topmost empty/energy/battery/power bar will
		 * turn red.
		 *
		 * This is typically caused by fire or boarders attempting sabotage.
		 *
		 * Note: Repair progress and damage progress can both be non-zero at the
		 * same time. They affect different bars.
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
		 * power, and the battery deactivates, a lock countdown will complete
		 * immediately (but not a plain ion countdown).
		 *
		 * The game's interface responds as this increments, including
		 * resetting after intervals. If not needed, it may be 0, or
		 * more often, MIN_INT.
		 *
		 * It was thought that in FTL 1.01-1.03.3, deionization of each bar
		 * counted to 5000. In FTL 1.5.13, it was observed at 14407 (with half
		 * the circle remaining).
		 *
		 * TODO:
		 * Nearly every system has been observed with non-zero values,
		 * but aside from Teleporter/Cloaking, normal use doesn't reliably
		 * set such values. Might be unspecified garbage when not actively
		 * counting. Sometimes has huge positive and negative values.
		 *
		 * This value is reset upon loading.
		 * (TODO: Check if still true in FTL 1.5.4.)
		 *
		 * @see #setIonizedBars(int)
		 */
		public void setDeionizationTicks( int n ) { deionizationTicks = n; }
		public int getDeionizationTicks() { return deionizationTicks; }


		/**
		 * Sets the number of battery power bars assigned to this system.
		 *
		 * Battery bars have an orange border and will appear above reserve
		 * power bars in the stack. When the battery system is fully discharged,
		 * they will be lost, but spare reserve power at that moment will be
		 * allocated to replace them.
		 *
		 * Note: For Weapons and DroneCtrl systems, this value must be set
		 * whenever a weapon/drone is armed/disarmed and vice versa.
		 *
		 * Note: Whenever this value changes, the ship's Battery extended system
		 * info must be updated.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setPower(int)
		 * @see BatteryInfo#setUsedBattery(int)
		 */
		public void setBatteryPower( int n ) { batteryPower = n; }
		public int getBatteryPower() { return batteryPower; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (no hacking drone pod), 1 (pod passively
		 * attached, set on contact), 2 (disrupting).
		 *
		 * If the hacking system of the other ship is inoperative, this will be
		 * set to 0, even though there is still a pod attached.
		 *
		 * TODO: Revise this description.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setHacked(int)
		 */
		public void setHackLevel( int n ) { hackLevel = n; }
		public int getHackLevel() { return hackLevel; }

		/**
		 * Toggles whether this system has a hacking drone pod attached.
		 *
		 * This only describes attachment (set the moment the pod makes
		 * contact), not disruption.
		 *
		 * If the hacking system of the other ship is inoperative, this will be
		 * set to false, even though there is still a pod attached.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setHackLevel(int)
		 */
		public void setHacked( boolean b ) { hacked = b; }
		public boolean isHacked() { return hacked; }

		/**
		 * Sets an upper limit on this system's usable capacity.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "limit=" attribute.
		 *
		 * Mods are reportedly only capable of using one flavor of capacity
		 * limit, but in saved games, they're all set, and the most restrictive
		 * one applies.
		 *
		 * Under normal circumstances, the cap is 1000.
		 * At a beacon with a nebula, the Sensors system has cap of 0.
		 *
		 * This was introduced in FTL 1.5.4.
		 *
		 * @see #setTemporaryCapacityLoss(int)
		 * @see #setTemporaryCapacityDivisor(int)
		 */
		public void setTemporaryCapacityCap( int n ) { temporaryCapacityCap = n; }
		public int getTemporaryCapacityCap() { return temporaryCapacityCap; }

		/**
		 * Sets a number to subtract from this system's usable capacity.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "loss=" attribute.
		 *
		 * Mods are reportedly only capable of using one flavor of capacity
		 * limit, but in saved games, they're all set, and the most restrictive
		 * one applies.
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
		 * Sets a number to divide this system's usable capacity by.
		 *
		 * The real capacity will be rounded up to the nearest multiple of N
		 * before dividing.
		 *
		 * The effect lasts for the current beacon only, or until reset.
		 *
		 * In the game's xml resources, the cap value comes from a "status" tag
		 * with the "divide=" attribute.
		 *
		 * Mods are reportedly only capable of using one flavor of capacity
		 * limit, but in saved games, they're all set, and the most restrictive
		 * one applies.
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


		/**
		 * Returns the effective capacity after applying limits (min 0).
		 *
		 * Damage is not considered.
		 *
		 * @see #getUsableCapacity()
		 */
		public int getLimitedCapacity() {
			int capLimit = temporaryCapacityCap;
			int lossLimit = capacity - temporaryCapacityLoss;
			int divLimit = (capacity + temporaryCapacityDivisor-1) / temporaryCapacityDivisor;

			int limit = Math.max( 0, Math.min( capLimit, Math.min( lossLimit, divLimit ) ) );
			return limit;
		}

		/**
		 * Returns the effective capacity after applying limits and damage
		 * (min 0).
		 *
		 * The result is the maximum total power, battery, or zoltan bars.
		 *
		 * @see #getLimitedCapacity()
		 */
		public int getUsableCapacity() {
			int limitedCapacity = getLimitedCapacity();
			int damagedCapacity = capacity - damagedBars;
			return Math.max( 0, Math.min( limitedCapacity, damagedCapacity ) );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "SystemId:              %s\n", systemType.getId() ) );
			if (capacity > 0) {
				result.append( String.format( "Capacity:                %3d\n", capacity ) );
				result.append( String.format( "Power:                   %3d\n", power ) );
				result.append( String.format( "Damaged Bars:            %3d\n", damagedBars ) );
				result.append( String.format( "Ionized Bars:            %3d\n", ionizedBars ) );
				result.append( String.format( "Repair Progress:         %3d%%\n", repairProgress ) );
				result.append( String.format( "Damage Progress:         %3d%%\n", damageProgress ) );
				result.append( String.format( "Deionization Ticks:    %5s\n", (deionizationTicks == Integer.MIN_VALUE ? "N/A" : deionizationTicks) ) );
				result.append( String.format( "Battery Power:           %3d\n", batteryPower ) );
				result.append( String.format( "Hack Level:              %3d\n", hackLevel ) );
				result.append( String.format( "Hacked:                %5b\n", hacked ) );
				result.append( String.format( "Temp Capacity Cap:     %5d\n", temporaryCapacityCap ) );
				result.append( String.format( "Temp Capacity Loss:      %3d\n", temporaryCapacityLoss ) );
				result.append( String.format( "Temp Capacity Divisor:   %3d\n", temporaryCapacityDivisor ) );
			}
			else {
				result.append( String.format( "(Not installed)\n" ) );
			}

			return result.toString();
		}
	}



	/**
	 * The direction crew will face when standing at a system room's terminal.
	 */
	public static enum StationDirection { DOWN, RIGHT, UP, LEFT, NONE }

	public static class RoomState {
		private int oxygen = 100;
		private List<SquareState> squareList = new ArrayList<SquareState>();
		private int stationSquare = -1;
		private StationDirection stationDirection = StationDirection.NONE;


		/**
		 * Constructs an incomplete RoomState.
		 *
		 * It will need squares.
		 */
		public RoomState() {
		}

		/**
		 * Copy constructor.
		 *
		 * Each SquareState will be copy-constructed as well.
		 */
		public RoomState( RoomState srcRoom ) {
			oxygen = srcRoom.getOxygen();

			for ( SquareState square : srcRoom.getSquareList() ) {
				squareList.add( new SquareState( square ) );
			}

			stationSquare = srcRoom.getStationSquare();
			stationDirection = srcRoom.getStationDirection();
		}

		/**
		 * Set's the oxygen percentage in the room.
		 *
		 * When this is below 5, a warning appears.
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
		 */
		public void addSquare( SquareState square ) {
			squareList.add( square );
		}
		public SquareState getSquare( int n ) {
			return squareList.get( n );
		}

		public List<SquareState> getSquareList() { return squareList; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Oxygen: %3d%%\n", oxygen ) );
			result.append( String.format( "Station Square: %2d, Station Direction: %s\n", stationSquare, stationDirection.toString() ) );

			result.append( "Squares...\n" );
			for ( SquareState square : squareList ) {
				result.append( square.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}

	public static class SquareState {
		private int fireHealth = 0;
		private int ignitionProgress = 0;
		private int extinguishmentProgress = -1;


		public SquareState() {
		}

		public SquareState( int fireHealth, int ignitionProgress, int extinguishmentProgress ) {
			this.fireHealth = fireHealth;
			this.ignitionProgress = ignitionProgress;
			this.extinguishmentProgress = extinguishmentProgress;
		}

		/**
		 * Copy constructor.
		 */
		public SquareState( SquareState srcSquare ) {
			this.fireHealth = srcSquare.getFireHealth();
			this.ignitionProgress = srcSquare.getIgnitionProgress();
			this.extinguishmentProgress = srcSquare.getExtinguishmentProgress();
		}

		/**
		 * Sets the health of a fire in this square, or 0.
		 *
		 * @param n 0-100
		 */
		public void setFireHealth( int n ) { fireHealth = n; }
		public int getFireHealth() { return fireHealth; }

		/**
		 * Sets the square's ignition progress.
		 *
		 * Squares adjacent to a fire grow closer to igniting as
		 * time passes. Then a new fire spawns in them at full health.
		 *
		 * @param n 0-100
		 */
		public void setIgnitionProgress( int n ) { ignitionProgress = n; }
		public int getIgnitionProgress() { return ignitionProgress; }

		/**
		 * Unknown.
		 *
		 * This is a rapidly decrementing number, as a fire disappears in a puff
		 * of smoke. When not set, this is -1.
		 *
		 * Starving a fire of oxygen does not affect its health.
		 *
		 * In FTL 1.01-1.5.10 this always seemed to be -1. In FTL 1.5.13, other
		 * values were finally observed.
		 *
		 * Observed values: -1 (almost always), 9,8,7,6,5,2,1,0.
		 */
		public void setExtinguishmentProgress( int n ) { extinguishmentProgress = n; }
		public int getExtinguishmentProgress() { return extinguishmentProgress; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Fire HP: %3d, Ignition: %3d%%, Extinguishment: %2d\n", fireHealth, ignitionProgress, extinguishmentProgress ) );

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


		/**
		 * Constructor.
		 */
		public DoorState() {
		}

		/**
		 * Copy constructor.
		 */
		public DoorState( DoorState srcDoor ) {
			open = srcDoor.isOpen();
			walkingThrough = srcDoor.isWalkingThrough();
			currentMaxHealth = srcDoor.getCurrentMaxHealth();
			health = srcDoor.getHealth();
			nominalHealth = srcDoor.getNominalHealth();
			unknownDelta = srcDoor.getUnknownDelta();
			unknownEpsilon = srcDoor.getUnknownEpsilon();
		}

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		public void commandeer() {
			setCurrentMaxHealth( getNominalHealth() );
			setHealth( getCurrentMaxHealth() );

			setUnknownDelta( 0 );    // TODO: Vet this default.
			setUnknownEpsilon( 0 );  // TODO: Vet this default.
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
		 * Observed values:
		 *   04 = Level 0 (un-upgraded or damaged Doors system).
		 *   08 = Level 1 (???)
		 *   12 = Level 2 (confirmed)
		 *   16 = Level 3 (confirmed)
		 *   20 = Level 4 (Level 3, plus manned; confirmed)
		 *   18 = Level 3 (max, plus manned) (or is it 15, 10 while unmanned?)
		 *   50 = Lockdown.
		 *
		 * TODO: The Mantis Basilisk ship's doors went from 4 to 12 when the
		 * 1-capacity Doors system was manned. Doors that were already hacked at
		 * the time stayed at 16.
		 *
		 * TODO: Check what the Rock B Ship's doors have (it lacks a Doors
		 * system). Damaged system is 4 (hacked doors were still 16).
		 *
		 * TODO: Investigate why an attached hacking drone adds to ALL THREE
		 * healths (set on contact). Observed diffs: 4 to 16.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setNominalHealth( int n ) { nominalHealth = n; }
		public int getNominalHealth() { return nominalHealth; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (normal), 1 (while level 2 Doors system is
		 * unmanned), 1 (while level 1 Doors system is manned), 2 (while level 3
		 * Doors system is unmanned), 3 (while level 3 Doors system is manned),
		 * 2 (hacking pod passively attached, set on
		 * contact). Still 2 while hack-disrupting.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets hacking drone lockdown status.
		 *
		 * Observed values:
		 *   0 = N/A
		 *   1 = Hacking drone pod passively attached.
		 *   2 = Hacking drone pod attached and disrupting.
		 *
		 * A hacking system launches a drone pod that will latch onto a target
		 * system room, granting visibility. While the pod is attached and there
		 * is power to the hacking system, the doors of the room turn purple,
		 * locked to the crew of the targeted ship, but passable to the hacker's
		 * crew.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Open: %-5b, Walking Through: %-5b\n", open, walkingThrough ) );
			result.append( String.format( "Full HP: %3d, Current HP: %3d, Nominal HP: %3d, Delta?: %3d, Epsilon?: %3d\n", currentMaxHealth, health, nominalHealth, unknownDelta, unknownEpsilon ) );

			return result.toString();
		}
	}



	public static class LockdownCrystal {
		private int currentPosX = 0, currentPosY = 0;
		private int speed = 0;
		private int goalPosX = 0, goalPosY = 0;
		private boolean arrived = false;
		private boolean done = false;
		private int lifetime = 0;
		private boolean superFreeze = false;
		private int lockingRoom = 0;
		private int animDirection = 0;
		private int shardProgress = 0;


		public LockdownCrystal() {
		}

		public void setCurrentPositionX( int n ) { currentPosX = n; }
		public void setCurrentPositionY( int n ) { currentPosY = n; }
		public void setSpeed( int n ) { speed = n; }
		public void setGoalPositionX( int n ) { goalPosX = n; }
		public void setGoalPositionY( int n ) { goalPosY = n; }
		public void setArrived( boolean b ) { arrived = b; }
		public void setDone( boolean b ) { done = b; }
		public void setLifetime( int n ) { lifetime = n; }
		public void setSuperFreeze( boolean b ) { superFreeze = b; }
		public void setLockingRoom( int n ) { lockingRoom = n; }
		public void setAnimDirection( int n ) { animDirection = n; }
		public void setShardProgress( int n ) { shardProgress = n; }

		public int getCurrentPositionX() { return currentPosX; }
		public int getCurrentPositionY() { return currentPosY; }
		public int getSpeed() { return speed; }
		public int getGoalPositionX() { return goalPosX; }
		public int getGoalPositionY() { return goalPosY; }
		public boolean hasArrived() { return arrived; }
		public boolean isDone() { return done; }
		public int getLifetime() { return lifetime; }
		public boolean isSuperFreeze() { return superFreeze; }
		public int getLockingRoom() { return lockingRoom; }
		public int getAnimDirection() { return animDirection; }
		public int getShardProgress() { return shardProgress; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Current Position:  %8d,%8d (%9.03f,%9.03f)\n", currentPosX, currentPosY, currentPosX/1000f, currentPosY/1000f ) );
			result.append( String.format( "Speed?:            %8d\n", speed ) );
			result.append( String.format( "Goal Position:     %8d,%8d (%9.03f,%9.03f)\n", goalPosX, goalPosY, goalPosX/1000f, goalPosY/1000f ) );
			result.append( String.format( "Arrived?:          %8b\n", arrived ) );
			result.append( String.format( "Done?:             %8b\n", done ) );
			result.append( String.format( "Lifetime?:         %8d\n", lifetime ) );
			result.append( String.format( "SuperFreeze?:      %8b\n", superFreeze ) );
			result.append( String.format( "Locking Room?:     %8d\n", lockingRoom ) );
			result.append( String.format( "Anim Direction?:   %8d\n", animDirection ) );
			result.append( String.format( "Shard Progress?:   %8d\n", shardProgress ) );

			return result.toString();
		}
	}



	public static class WeaponState {
		private String weaponId = null;
		private boolean armed = false;
		private int cooldownTicks = 0;
		private WeaponModuleState weaponMod = null;


		/**
		 * Constructs an incomplete WeaponState.
		 *
		 * It will need a weaponId.
		 *
		 * For FTL 1.5.4+ saved games, a weapon module will be needed.
		 */
		public WeaponState() {
		}

		/**
		 * Copy constructor.
		 *
		 * The weapon module will be copy-constructed as well.
		 */
		public WeaponState( WeaponState srcWeapon ) {
			weaponId = srcWeapon.getWeaponId();
			armed = srcWeapon.isArmed();
			cooldownTicks = srcWeapon.getCooldownTicks();

			if ( srcWeapon.getWeaponModule() != null ) {
				weaponMod = new WeaponModuleState( srcWeapon.getWeaponModule() );
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		public void commandeer() {
			setArmed( false );
			setCooldownTicks( 0 );

			if ( getWeaponModule() != null ) {
				getWeaponModule().commandeer();
			}
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
		 * This increments from 0, by 1 each second. Its goal is the value of
		 * the 'coolown' tag in its WeaponBlueprint xml (0 when not armed).
		 *
		 * Since FTL 1.5.4, this is no longer saved.
		 *
		 * @see WeaponModuleState.setCooldownTicks(int)
		 */
		public void setCooldownTicks( int n ) { cooldownTicks = n; }
		public int getCooldownTicks() { return cooldownTicks; }

		/**
		 * Sets additional weapon fields.
		 *
		 * Advanced Edition added extra weapon fields at the end of saved game
		 * files. They're nested inside this class for convenience.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setWeaponModule( WeaponModuleState weaponMod ) { this.weaponMod = weaponMod; }
		public WeaponModuleState getWeaponModule() { return weaponMod; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			WeaponBlueprint weaponBlueprint = DataManager.get().getWeapon( weaponId );
			String cooldownString = ( weaponBlueprint != null ? weaponBlueprint.getCooldown()+"" : "?" );

			result.append( String.format( "WeaponId:       %s\n", weaponId ) );
			result.append( String.format( "Armed:          %b\n", armed ) );
			result.append( String.format( "Cooldown Ticks: %2d (max: %2s) (Not used as of FTL 1.5.4)\n", cooldownTicks, cooldownString ) );

			result.append( "\nWeapon Module...\n" );
			if ( weaponMod != null ) {
				result.append( weaponMod.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	/**
	 * Types of drones.
	 *
	 * FTL 1.5.4 introduced HACKING, BEAM, and SHIELD.
	 */
	public static enum DroneType {
		// TODO: Magic numbers.
		BATTLE     ( "BATTLE",      150 ),
		REPAIR     ( "REPAIR",       25 ),
		BOARDER    ( "BOARDER",       1 ),
		HACKING    ( "HACKING",       1 ),
		COMBAT     ( "COMBAT",        1 ),
		BEAM       ( "BEAM",          1 ),
		DEFENSE    ( "DEFENSE",       1 ),
		SHIELD     ( "SHIELD",        1 ),
		SHIP_REPAIR( "SHIP_REPAIR",   1 );

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
				if ( d.getId().equals( id ) ) return d;
			}
			return null;
		}
	}

	public static class DroneState {
		private String droneId = null;
		private boolean armed = false;
		private boolean playerControlled = false;
		private int bodyX = -1, bodyY = -1;
		private int bodyRoomId = -1;
		private int bodyRoomSquare = -1;
		private int health = 1;
		private ExtendedDroneInfo droneInfo = null;


		/**
		 * Constructs an incomplete DroneState.
		 *
		 * It will need a droneId.
		 *
		 * For FTL 1.5.4+ saved games, extended info may be needed.
		 */
		public DroneState() {
		}

		/**
		 * Constructs an incomplete DroneState.
		 *
		 * For FTL 1.5.4+ saved games, extended info may be needed.
		 */
		public DroneState( String droneId ) {
			this.droneId = droneId;
		}

		/**
		 * Copy constructor.
		 *
		 * The extended info will be copy-constructed as well.
		 */
		public DroneState( DroneState srcDrone ) {
			droneId = srcDrone.getDroneId();
			armed = srcDrone.isArmed();
			playerControlled = srcDrone.isPlayerControlled();
			bodyX = srcDrone.getBodyX();
			bodyY = srcDrone.getBodyY();
			bodyRoomId = srcDrone.getBodyRoomId();
			bodyRoomSquare = srcDrone.getBodyRoomSquare();
			health = srcDrone.getHealth();

			if ( srcDrone.getExtendedDroneInfo() != null ) {
				droneInfo = new ExtendedDroneInfo( srcDrone.getExtendedDroneInfo() );
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public void commandeer() {
			setArmed( false );
			setPlayerControlled( false );

			if ( getExtendedDroneInfo() != null ) {
				getExtendedDroneInfo().commandeer();
			}
		}


		public void setDroneId( String s ) { droneId = s; }
		public String getDroneId() { return droneId; }

		/**
		 * Sets whether this drone is powered.
		 *
		 * @see ExtendedDroneInfo#setArmed(boolean)
		 */
		public void setArmed( boolean b ) { armed = b; }
		public boolean isArmed() { return armed; }

		/**
		 * Sets whether this drone is controlled by the player.
		 *
		 * When the drone is not armed, this should be set to false.
		 */
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public boolean isPlayerControlled() { return playerControlled; }

		/**
		 * Sets the position of the drone's body image.
		 *
		 * Technically the roomId/square fields set the goal location.
		 * This field is where the body really is, possibly en route.
		 *
		 * It's the position of the body image's center, relative to the
		 * top-left corner of the floor layout of the ship it's on.
		 *
		 * This value lingers, even after the body is gone.
		 *
		 * Note: This is only set by drones which have a body on their own ship.
		 */
		public void setBodyX( int n ) { bodyX = n; }
		public void setBodyY( int n ) { bodyY = n; }
		public int getBodyX() { return bodyX; }
		public int getBodyY() { return bodyY; }

		/**
		 * Sets the room this drone's body is in (or at least trying to move
		 * toward).
		 *
		 * When no body is present, this is -1.
		 *
		 * roomId and roomSquare need to be specified together.
		 *
		 * Note: This is only set by drones which have a body on their own ship.
		 */
		public void setBodyRoomId( int n ) { bodyRoomId = n; }
		public void setBodyRoomSquare( int n ) { bodyRoomSquare = n; }
		public int getBodyRoomId() { return bodyRoomId; }
		public int getBodyRoomSquare() { return bodyRoomSquare; }

		public void setHealth( int n ) { health = n; }
		public int getHealth() { return health; }

		/**
		 * Sets additional drone fields.
		 *
		 * Advanced Edition added extra drone fields at the end of saved game
		 * files. They're nested inside this class for convenience.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setExtendedDroneInfo( ExtendedDroneInfo droneInfo ) { this.droneInfo = droneInfo; }
		public ExtendedDroneInfo getExtendedDroneInfo() { return droneInfo; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "DroneId:           %s\n", droneId ) );
			result.append( String.format( "Armed:             %5b\n", armed ) );
			result.append( String.format( "Health:            %5d\n", health ) );
			result.append( String.format( "Body Position:     %3d,%3d\n", bodyX, bodyY ) );
			result.append( String.format( "Body Room Id:      %5d\n", bodyRoomId ) );
			result.append( String.format( "Body Room Square:  %5d\n", bodyRoomSquare ) );
			result.append( String.format( "Player Controlled: %5b\n", playerControlled ) );

			result.append( "\nExtended Drone Info...\n" );
			if ( droneInfo != null ) {
				result.append( droneInfo.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	public static enum FleetPresence {
		NONE( "None" ), REBEL( "Rebel" ), FEDERATION( "Federation" ), BOTH( "Both" );

		private String title;
		private FleetPresence( String title ) { this.title = title; }
		public String toString() { return title; }
	}

	/**
	 * A beacon on the sector map.
	 *
	 * Beacon states do not contain their randomly determined values until they
	 * are actually visited.
	 *
	 * FTL uses the sector layout seed to decide pending events and such upon
	 * entering the sector. Any distress, stores, etc. events get signs when
	 * seen (or hazard icons when the map is revealed).
	 *
	 * @see SavedGameState#setSectorLayoutSeed(int)
	 */
	public static class BeaconState {
		private int visitCount = 0;
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


		/**
		 * Constructor.
		 */
		public BeaconState() {
		}

		/**
		 * Copy constructor.
		 *
		 * Any store will be copy-constructed as well.
		 */
		public BeaconState( BeaconState srcBeacon ) {
			visitCount = srcBeacon.getVisitCount();
			bgStarscapeImageInnerPath = srcBeacon.getBgStarscapeImageInnerPath();
			bgSpriteImageInnerPath = srcBeacon.getBgSpriteImageInnerPath();
			bgSpritePosX = srcBeacon.getBgSpritePosX();
			bgSpritePosY = srcBeacon.getBgSpritePosY();
			bgSpriteRotation = srcBeacon.getBgSpriteRotation();
			seen = srcBeacon.isSeen();
			enemyPresent = srcBeacon.isEnemyPresent();
			shipEventId = srcBeacon.getShipEventId();
			autoBlueprintId = srcBeacon.getAutoBlueprintId();
			shipEventSeed = srcBeacon.getShipEventSeed();
			fleetPresence = srcBeacon.getFleetPresence();
			underAttack = srcBeacon.isUnderAttack();

			if ( srcBeacon.getStore() != null ) {
				store = new StoreState( srcBeacon.getStore() );
			}
		}

		/**
		 * Sets the number of times the player has arrived at this beacon.
		 *
		 * If non-zero, starscape and sprite paths must be set,
		 * as well as the sprite's X, Y, and rotation.
		 *
		 * When non-zero, this prevents randomly generated events
		 * from triggering. The sector exit will still exist.
		 */
		public void setVisitCount( int n ) { visitCount = n; }
		public int getVisitCount() { return visitCount; }

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
		 * When the sprite's inner path is "NONE", this should be 0.
		 *
		 * @param n degrees clockwise (may be negative)
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

			result.append( String.format( "Visit Count:           %5d\n", visitCount ) );
			if ( visitCount > 0 ) {
				result.append( String.format( "  Bkg Starscape:       %s\n", bgStarscapeImageInnerPath ) );
				result.append( String.format( "  Bkg Sprite:          %s\n", bgSpriteImageInnerPath ) );
				result.append( String.format( "  Bkg Sprite Position:   %3d,%3d\n", bgSpritePosX, bgSpritePosY ) );
				result.append( String.format( "  Bkg Sprite Rotation:   %3d\n", bgSpriteRotation ) );
			}

			result.append( String.format( "Seen:                  %5b\n", seen ) );

			result.append( String.format( "Enemy Present:         %5b\n", enemyPresent ) );
			if ( enemyPresent ) {
				result.append( String.format( "  Ship Event ID:       %s\n", shipEventId ) );
				result.append( String.format( "  Auto Blueprint ID:   %s\n", autoBlueprintId ) );
				result.append( String.format( "  Ship Event Seed:     %5d\n", shipEventSeed ) );
			}

			result.append( String.format( "Fleets Present:        %s\n", fleetPresence ) );

			result.append( String.format( "Under Attack:          %5b\n", underAttack ) );

			if ( store != null ) {
				result.append( "\nStore...\n" );
				result.append( store.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	/**
	 * A store, which contains supplies and item shelves of various types.
	 *
	 * FTL 1.01-1.03.3 always had two StoreShelf objects.
	 * If more are added, only the first two will be saved.
	 * If fewer, writeBeacon() will add dummy shelves with no items.
	 *
	 * FTL 1.5.4 can have a variable number of shelves, but zero
	 * crashes the game. So writeBeacon() will add a dummy shelf.
	 *
	 * TODO: Find out what happens if more than four shelves are added.
	 */
	public static class StoreState {
		private int fuel = 0, missiles = 0, droneParts = 0;
		private List<StoreShelf> shelfList = new ArrayList<StoreShelf>( 4 );  // TODO: Magic number.


		/**
		 * Constructs a StoreState.
		 */
		public StoreState() {
		}

		/**
		 * Copy constructor.
		 *
		 * Each StoreShelf will be copy-constructed as well.
		 */
		public StoreState( StoreState srcStore ) {
			fuel = srcStore.getFuel();
			missiles = srcStore.getMissiles();
			droneParts = srcStore.getDroneParts();

			for ( StoreShelf srcShelf : srcStore.getShelfList() ) {
				addShelf( new StoreShelf( srcShelf ) );
			}
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

			result.append( String.format( "Fuel:        %2d\n", fuel ) );
			result.append( String.format( "Missiles:    %2d\n", missiles ) );
			result.append( String.format( "Drone Parts: %2d\n", droneParts ) );

			for ( int i=0; i < shelfList.size(); i++ ) {
				result.append( String.format( "\nShelf %d...\n", i ) );
				result.append( shelfList.get( i ).toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}

	public static enum StoreItemType {
		WEAPON( "Weapon" ), DRONE( "Drone" ), AUGMENT( "Augment" ),
		CREW( "Crew" ), SYSTEM( "System" );

		private String title;
		private StoreItemType( String title ) { this.title = title; }
		public String toString() { return title; }
	}

	public static class StoreShelf {
		private StoreItemType itemType = StoreItemType.WEAPON;
		private List<StoreItem> items = new ArrayList<StoreItem>( 3 );  // TODO: Magic number.


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
			itemType = srcShelf.getItemType();

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

			result.append( String.format( "Item Type: %s\n", itemType ) );
			for ( StoreItem item : items ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( item.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}

	/**
	 * An item in a store which either can be bought or has been bought already.
	 */
	public static class StoreItem {
		private String itemId = null;
		private boolean available = false;
		private int extraData = 0;


		/**
		 * Constructor.
		 *
		 * @param itemId a weapon/drone/augment blueprint or crew-race/system id
		 */
		public StoreItem( String itemId ) {
			this.itemId = itemId;
		}

		/**
		 * Copy constructor.
		 */
		public StoreItem( StoreItem srcItem ) {
			this( srcItem.getItemId() );
			available = srcItem.isAvailable();
			extraData = srcItem.getExtraData();
		}

		public String getItemId() { return itemId; }

		/**
		 * Sets whether this item has been sold already.
		 */
		public void setAvailable( boolean b ) { available = b; }
		public boolean isAvailable() { return available; }

		/**
		 * Unknown.
		 *
		 * Bonus drones (Repair/Defense 1) are not remembered, so it's not
		 * that. Reloading at a store offering a bonus Repair always results in
		 * a Defense 1.
		 *
		 * Observed values: 1 (w/Drone_Ctrl+Repair), 2 (w/Cloaking),
		 * 1 (w/Clonebay), 0 (on them all after reloading!?). Also seen:
		 * 2 (w/Drone_Ctrl), 1 (w/Teleporter), 2 (w/Battery).
		 *
		 * This was introduced in FTL 1.5.12.
		 */
		public void setExtraData( int n ) { extraData = n; }
		public int getExtraData() { return extraData; }

		@Override
		public String toString() {
			return String.format( "Item: %s, Available: %5b, Extra?: %3d\n", itemId, available, extraData );
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
		private int unknownAlpha = 0;
		private String text = "";
		private int affectedCrewSeed = -1;
		private List<Integer> choiceList = new ArrayList<Integer>();


		public EncounterState() {
		}


		/**
		 * Sets a seed to randomly generate the enemy ship (layout, etc).
		 *
		 * When the player ship visits a beacon, the resulting encounter
		 * will use the beacon's enemy ship event seed.
		 *
		 * When not set, this is 0. After encountering ships, this value lingers.
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

		/**
		 * Sets the id of the most recent (possibly current) event id.
		 *
		 * As secondary and tertiary events are triggered at a beacon, this
		 * value will be replaced by their ids.
		 *
		 * Sometimes this is blank.
		 *
		 * Matthew's hint: There are two kinds of event: static events, assigned
		 * randomly based on the sector seed and "sector_data.xml" (like for
		 * nebula beacons); and dynamic events. This value only tracks dynamic
		 * events.
		 */
		public void setLastEventId( String s ) { lastEventId = s; }
		public String getLastEventId() { return lastEventId; }

		/**
		 * Unknown.
		 *
		 * This was introduced in FTL 1.6.1.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		/**
		 * Sets the last situation-describing text shown in an event window.
		 *
		 * Any event - 'static', secondary, or wait - may set this value. It
		 * may have no relation to the last event id.
		 *
		 * Note: Wait events triggered in-game set this value. Toggling waiting
		 * programmatically does NOT set this value. That must be done
		 * manually.
		 *
		 * FTL 1.6.1 introduced XML "id" attributes on elements, which
		 * referenced text elsewhere. This value may be one of those references
		 * instead of the actual text.
		 *
		 * After the event popup is dismissed, this value lingers.
		 *
		 * This may include line breaks ("\n").
		 *
		 * @see SavedGameState#setWaiting(boolean)
		 */
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
		 * TODO: 52 was observed in the list once!?
		 *
		 * The event will still be in-progress if there aren't enough
		 * breadcrumbs to renavigate to the end of the event.
		 *
		 * The list typically ends with a 0, since events usually conclude with
		 * a lone "continue" choice.
		 *
		 * Note: If waiting, this list will cause a wait event to be selected
		 * from fuel-related event lists, instead of a normal event.
		 *
		 * @see SavedGameState#setWaiting(boolean)
		 */
		public void setChoiceList( List<Integer> choiceList ) { this.choiceList = choiceList; }
		public List<Integer> getChoiceList() { return choiceList; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;
			result.append( String.format( "Ship Event Seed:    %3d\n", shipEventSeed ) );
			result.append( String.format( "Surrender Event:    %s\n", surrenderEventId ) );
			result.append( String.format( "Escape Event:       %s\n", escapeEventId ) );
			result.append( String.format( "Destroyed Event:    %s\n", destroyedEventId ) );
			result.append( String.format( "Dead Crew Event:    %s\n", deadCrewEventId ) );
			result.append( String.format( "Got Away Event:     %s\n", gotAwayEventId ) );

			result.append( "\n" );

			result.append( String.format( "Last Event:         %s\n", lastEventId ) );
			result.append( String.format( "Alpha?:             %3d\n", unknownAlpha ) );

			result.append( "\nText...\n" );
			result.append( String.format( "%s\n", text ) );
			result.append( "\n" );

			result.append( String.format( "Affected Crew Seed: %3d\n", affectedCrewSeed ) );

			result.append( "\nLast Event Choices...\n" );
			first = true;
			for ( Integer choiceInt : choiceList ) {
				if ( first ) { first = false; }
				else { result.append( "," ); }
				result.append( choiceInt );
			}
			result.append( "\n" );

			return result.toString();
		}
	}



	public static class NearbyShipAIState {
		private boolean surrendered = false;
		private boolean escaping = false;
		private boolean destroyed = false;
		private int surrenderThreshold = 0;
		private int escapeThreshold = -1;
		private int escapeTicks = 15000;
		private boolean stalemateTriggered = false;  // TODO: Does this start sudden death, or mark its completion?
		private int stalemateTicks = 0;
		private int boardingAttempts = 0;
		private int boardersNeeded = 0;


		/**
		 * Constructor.
		 */
		public NearbyShipAIState() {
		}

		/**
		 * Toggles whether the ship has offered surrender.
		 *
		 * FTL sets this the moment it triggers the surrender event (before the
		 * player accepts/declines).
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
		 * For the rebel flagship, this is -100.
		 */
		public void setSurrenderThreshold( int n ) { surrenderThreshold = n; }
		public int getSurrenderThreshold() { return surrenderThreshold; }

		/**
		 * Sets the hull amount that will cause the ship to flee.
		 *
		 * For the rebel flagship, this is -101.
		 *
		 * When not set, this is -1.
		 */
		public void setEscapeThreshold( int n ) { escapeThreshold = n; }
		public int getEscapeThreshold() { return escapeThreshold; }

		/**
		 * Sets time elapsed while waiting for the FTL drive to charge.
		 *
		 * This decrements to 0; then the nearby ship jumps away.
		 *
		 * Observed values: 15000 (initially), 27533 (30000?).
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

			result.append( String.format( "Surrender Offered:   %7b\n", surrendered ) );
			result.append( String.format( "Escaping:            %7b\n", escaping ) );
			result.append( String.format( "Destroyed?:          %7b\n", destroyed ) );
			result.append( String.format( "Surrender Threshold: %7d (Hull amount when surrender is offered)\n", surrenderThreshold ) );
			result.append( String.format( "Escape Threshold:    %7d (Hull amount when escape begins)\n", escapeThreshold ) );
			result.append( String.format( "Escape Ticks:        %7d (Decrements to 0)\n", escapeTicks ) );
			result.append( String.format( "Stalemate Triggered?:%7b\n", stalemateTriggered ) );
			result.append( String.format( "Stalemate Ticks?:    %7d\n", stalemateTicks ) );
			result.append( String.format( "Boarding Attempts?:  %7d\n", boardingAttempts ) );
			result.append( String.format( "Boarders Needed?:    %7d\n", boardersNeeded ) );

			return result.toString();
		}
	}



	public static enum HazardVulnerability {
		PLAYER_SHIP, NEARBY_SHIP, BOTH_SHIPS
	}

	public static class EnvironmentState {
		private boolean redGiantPresent = false;
		private boolean pulsarPresent = false;
		private boolean pdsPresent = false;
		private HazardVulnerability vulnerableShips = HazardVulnerability.BOTH_SHIPS;
		private AsteroidFieldState asteroidField = null;
		private int solarFlareFadeTicks = 0;
		private int havocTicks = 0;
		private int pdsTicks = 0;  // Used by: PDS. Value lingers after leaving a beacon (sometimes varying by 1).


		public EnvironmentState() {
		}


		/**
		 * Toggles the presence of a red giant hazard.
		 *
		 * Red giant, pulsar, and PDS hazards can coexist.
		 */
		public void setRedGiantPresent( boolean b ) { redGiantPresent = b; }
		public boolean isRedGiantPresent() { return redGiantPresent; }

		/**
		 * Toggles the presence of a pulsar hazard.
		 *
		 * Red giant, pulsar, and PDS hazards can coexist.
		 */
		public void setPulsarPresent( boolean b ) { pulsarPresent = b; }
		public boolean isPulsarPresent() { return pulsarPresent; }

		/**
		 * Toggles the presence of a PDS hazard.
		 *
		 * Red giant, pulsar, and PDS hazards can coexist.
		 */
		public void setPDSPresent( boolean b ) { pdsPresent = b; }
		public boolean isPDSPresent() { return pdsPresent; }

		/**
		 * Sets which ships will be targeted by a PDS.
		 *
		 * Matthew's hint: Values are 0,1,2 for player ship, nearby ship, or
		 * both. (0 and 1 are unconfirmed.)
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

			result.append( String.format( "Red Giant Present: %5b\n", redGiantPresent ) );
			result.append( String.format( "Pulsar Present:    %5b\n", pulsarPresent ) );
			result.append( String.format( "PDS Present:       %5b\n", pdsPresent ) );
			result.append( String.format( "Vulnerable Ships:  %s (PDS only)\n", vulnerableShips.toString() ) );

			result.append( "\nAsteroid Field...\n" );
			if ( asteroidField != null )
				result.append( asteroidField.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );

			result.append( "\n" );

			result.append( String.format( "Flare Fade Ticks?: %7d\n", solarFlareFadeTicks ) );
			result.append( String.format( "Havoc Ticks?:      %7d (Red Giant/Pulsar/PDS only, Goal varies)\n", havocTicks ) );
			result.append( String.format( "PDS Ticks?:        %7d (PDS only)\n", pdsTicks ) );

			return result.toString();
		}
	}

	public static class AsteroidFieldState {
		private int unknownAlpha = -1000;
		private int strayRockTicks = 0;
		private int unknownGamma = 0;
		private int bgDriftTicks = 0;
		private int currentTarget = 0;


		public AsteroidFieldState() {
		}

		/**
		 * Unknown.
		 *
		 * Observed values: 3, 0; 4.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		/**
		 * Unknown.
		 *
		 * Observed values: 15853, 15195, 14786, 12873, 12741, 12931. It's been
		 * seen at 6545 immediately after reaching 0 (random starting value?).
		 */
		public void setStrayRockTicks( int n ) { strayRockTicks = n; }
		public int getStrayRockTicks() { return strayRockTicks; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0, 1, 2, 0, 1.
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Sets time elapsed while the background shifts left.
		 *
		 * Observed values: 1952, 1294, 885, 817, 685, 335. It's been seen
		 * stuck at 143 until strayRockTicks hit 0, then became 1102!? Then
		 * seen decrementing to 0, then became 1399.
		 */
		public void setBgDriftTicks( int n ) { bgDriftTicks = n; }
		public int getBgDriftTicks() { return bgDriftTicks; }

		/**
		 * Unknown.
		 *
		 * This seems to be an incrementing counter.
		 *
		 * Observed values: 1, 8, 13.
		 */
		public void setCurrentTarget( int n ) { currentTarget = n; }
		public int getCurrentTarget() { return currentTarget; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Alpha?:            %7d\n", unknownAlpha ) );
			result.append( String.format( "Stray Rock Ticks?: %7d\n", strayRockTicks ) );
			result.append( String.format( "Gamma?:            %7d\n", unknownGamma ) );
			result.append( String.format( "Bkg Drift Ticks?:  %7d\n", bgDriftTicks ) );
			result.append( String.format( "Current Target?:   %7d\n", currentTarget ) );

			return result.toString();
		}
	}



	/**
	 * Info used for spawning the rebel flagship.
	 *
	 * Whereas regular ship encounters are preserved in BeaconStates for repeat
	 * visits, the flagship is not tied to a location.
	 *
	 * In FTL 1.01-1.03.3, this info is not present in saved games until
	 * after engaging the rebel flagship in sector 8 for the first time.
	 *
	 * In FTL 1.5.4, this is always present, though the occupancy map may be
	 * empty.
	 */
	public static class RebelFlagshipState {
		private int unknownAlpha = 0;
		private int pendingStage = 1;
		private int unknownGamma = 30000;
		private int unknownDelta = 0;
		private Map<Integer, Integer> occupancyMap = new LinkedHashMap<Integer, Integer>();


		/**
		 * Constructor.
		 */
		public RebelFlagshipState() {
		}

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (normal), 1 (after encountering
		 * first-stage boss), 2 (after encountering second-stage boss), 3
		 * (after encountering third-stage boss).
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

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
		 * Unknown.
		 *
		 * During the third-stage boss fight, this does not change.
		 *
		 * Observed values: 30000 (normal), 21326 (after encountering
		 * first-stage boss).
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (normal), 240 (after encountering
		 * first-stage boss), 26563 (after defeating second-stage boss). Seems
		 * to have no effect on first-stage boss, but this changes nonetheless.
		 * During the second-stage boss, counts to ~25000, then it resets to 0,
		 * and surge drones appear. During the third-stage boss, counts to
		 * ~16000, then it either recharges its Zoltan shield or fires lots of
		 * laser projectiles.
		 *
		 * This was introduced in FTL 1.5.4.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets whether a room had crew members in the last seen layout.
		 *
		 * Stage 1 sets this, but doesn't read it.
		 * Fleeing stage 1, editing, then returning only results in a fresh
		 * fight.
		 *
		 * Upon first engaging stage 2, the layout is migrated.
		 * The occupancy list is truncated to the new layout's rooms.
		 * (The blueprints happen to have matching low roomIds.)
		 *
		 *   Stage 1 (BOSS_1): 19 rooms.
		 *   Stage 2 (BOSS_2): 15 rooms.
		 *   Stage 3 (BOSS_3): 11 rooms.
		 *   Having 0 rooms occupied is allowed, meaning AI took over.
		 *
		 * Stage 2 will respond to pre-skirmish editing.
		 *
		 * Stage 3 probably will, too. (TODO: Confirm this.)
		 *
		 * @param roomId a room in the last seen stage's shipLayout
		 * @param n the number of crew in that room
		 */
		public void setPreviousOccupancy( int roomId, int n ) {
			occupancyMap.put( new Integer( roomId ), n );
		}

		public Map<Integer, Integer> getOccupancyMap() {
			return occupancyMap;
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Alpha?:                 %11d\n", unknownAlpha ) );
			result.append( String.format( "Pending Flagship Stage: %11d\n", pendingStage ) );
			result.append( String.format( "Gamma?:                 %11d\n", unknownGamma ) );
			result.append( String.format( "Delta?:                 %11d\n", unknownDelta ) );

			result.append( "\nOccupancy of Last Seen Flagship...\n" );
			for ( Map.Entry<Integer, Integer> entry : occupancyMap.entrySet() ) {
				int roomId = entry.getKey().intValue();
				int occupantCount = entry.getValue().intValue();

				result.append( String.format( "Room Id: %2d, Crew: %d\n", roomId, occupantCount ) );
			}

			return result.toString();
		}
	}



	public static class AnimState {
		private boolean playing = false;
		private boolean looping = false;
		private int currentFrame = 0;
		private int progressTicks = 0;
		private int scale = 1000;
		private int x = -1000;
		private int y = -1000;


		/**
		 * Constructor.
		 */
		public AnimState() {
		}

		/**
		 * Copy constructor.
		 */
		public AnimState( AnimState srcAnim ) {
			playing = srcAnim.isPlaying();
			looping = srcAnim.isLooping();
			currentFrame = srcAnim.getCurrentFrame();
			progressTicks = srcAnim.getProgressTicks();
			scale = srcAnim.getScale();
			x = srcAnim.getX();
			y = srcAnim.getY();
		}

		public void setPlaying( boolean b ) { playing = b; }
		public boolean isPlaying() { return playing; }

		public void setLooping( boolean b ) { looping = b; }
		public boolean isLooping() { return looping; }

		/**
		 * Sets the current frame of this anim (0-based).
		 *
		 * Start/end frames during playback vary. Anims, and their important
		 * frames, are defined in "animations.xml".
		 *
		 * FTL seems to clobber this value upon loading, based on the
		 * circumstances driving the anim, so editing it is probably useless.
		 */
		public void setCurrentFrame( int n ) { currentFrame = n; }
		public int getCurrentFrame() { return currentFrame; }

		/**
		 * Sets time elapsed while playing this anim.
		 *
		 * Technically this doesn't count, so much as remember how far into the
		 * anim playback was when the current frame appeared.
		 *
		 * This value is 1000 / (animSheet's frame count) * (currentFrame).
		 * Sometimes that's off by 1 due to rounding somewhere.
		 *
		 * TODO: That formula matched WeaponModuleState's weaponAnim, at least.
		 *
		 * FTL seems to clobber this value upon loading, based on the
		 * circumstances driving the anim, so editing it is probably useless.
		 */
		public void setProgressTicks( int n ) { progressTicks = n; }
		public int getProgressTicks() { return progressTicks; }

		/**
		 * Sets a scale factor.
		 *
		 * Projectiles with flightAnimId "debris_small" set their deathAnim
		 * scale to 250.
		 *
		 * @param n a pseudo-float (1000 is 1.0)
		 */
		public void setScale( int n ) { scale = n; }
		public int getScale() { return scale; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (when playing), -1000 (when not playing).
		 * One time, a missile exploded whose deathAnim had -32000.
		 */
		public void setX( int n ) { x = n; }
		public void setY( int n ) { y = n; }
		public int getX() { return x; }
		public int getY() { return y; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Playing:           %7b\n", playing ) );
			result.append( String.format( "Looping?:          %7b\n", looping ) );
			result.append( String.format( "Current Frame:     %7d\n", currentFrame ) );
			result.append( String.format( "Progress Ticks:    %7d\n", progressTicks ) );
			result.append( String.format( "Scale:             %7d (%5.03f)\n", scale, scale/1000f ) );
			result.append( String.format( "X,Y?:                %5d,%5d\n", x, y ) );

			return result.toString();
		}
	}



	public static abstract class ExtendedSystemInfo {

		protected ExtendedSystemInfo() {
		}

		protected ExtendedSystemInfo( ExtendedSystemInfo srcInfo ) {
		}

		/**
		 * Blindly copy-constructs objects.
		 *
		 * Subclasses override this with return values of their own type.
		 */
		public abstract ExtendedSystemInfo copy();

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		public abstract void commandeer();
	}

	public static class ClonebayInfo extends ExtendedSystemInfo {
		private int buildTicks = 0;
		private int buildTicksGoal = 0;
		private int doomTicks = 0;


		public ClonebayInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected ClonebayInfo( ClonebayInfo srcInfo ) {
			super( srcInfo );
			buildTicks = srcInfo.getBuildTicks();
			buildTicksGoal = srcInfo.getBuildTicksGoal();
			doomTicks = srcInfo.getDoomTicks();
		}

		@Override
		public ClonebayInfo copy() { return new ClonebayInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 */
		@Override
		public void commandeer() {
			setBuildTicks( 0 );
			setBuildTicksGoal( 0 );
			setDoomTicks( 0 );
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

			result.append( String.format( "SystemId:                 %s\n", SystemType.CLONEBAY.getId() ) );
			result.append( String.format( "Build Ticks:            %7d (For the current dead crew being cloned)\n", buildTicks ) );
			result.append( String.format( "Build Ticks Goal:       %7d\n", buildTicksGoal ) );
			result.append( String.format( "DoomTicks:              %7d (If unpowered, dead crew are lost at 3000)\n", doomTicks ) );

			return result.toString();
		}
	}

	public static class BatteryInfo extends ExtendedSystemInfo {
		private boolean active = false;
		private int usedBattery = 0;
		private int dischargeTicks = 1000;

		// Plasma storms only halve *reserve* power.
		// The Battery system is unaffected by plasma storms (<environment type="storm"/>).


		public BatteryInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected BatteryInfo( BatteryInfo srcInfo ) {
			super( srcInfo );
			active = srcInfo.isActive();
			usedBattery = srcInfo.getUsedBattery();
			dischargeTicks = srcInfo.getDischargeTicks();
		}

		@Override
		public BatteryInfo copy() { return new BatteryInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 */
		@Override
		public void commandeer() {
			setActive( false );
			setUsedBattery( 0 );
			setDischargeTicks( 1000 );
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
		public void setUsedBattery( int n ) { usedBattery = n; }
		public int getUsedBattery() { return usedBattery; }

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

			result.append( String.format( "SystemId:                 %s\n", SystemType.BATTERY.getId() ) );
			result.append( String.format( "Active:                   %5b\n", active ) );
			result.append( String.format( "Battery Power in Use:     %5d\n", usedBattery ) );
			result.append( String.format( "Discharge Ticks:          %5d\n", dischargeTicks ) );

			return result.toString();
		}
	}

	public static class ShieldsInfo extends ExtendedSystemInfo {
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
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected ShieldsInfo( ShieldsInfo srcInfo ) {
			super( srcInfo );
			shieldLayers = srcInfo.getShieldLayers();
			energyShieldLayers = srcInfo.getEnergyShieldLayers();
			energyShieldMax = srcInfo.getEnergyShieldMax();
			shieldRechargeTicks = srcInfo.getShieldRechargeTicks();

			shieldDropAnimOn = srcInfo.isShieldDropAnimOn();
			shieldDropAnimTicks = srcInfo.getShieldDropAnimTicks();

			shieldRaiseAnimOn = srcInfo.isShieldRaiseAnimOn();
			shieldRaiseAnimTicks = srcInfo.getShieldRaiseAnimTicks();

			energyShieldAnimOn = srcInfo.isEnergyShieldAnimOn();
			energyShieldAnimTicks = srcInfo.getEnergyShieldAnimTicks();

			unknownLambda = srcInfo.getUnknownLambda();
			unknownMu = srcInfo.getUnknownMu();
		}

		@Override
		public ShieldsInfo copy() { return new ShieldsInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		@Override
		public void commandeer() {
			setShieldLayers( 0 );
			setShieldRechargeTicks( 0 );
			setShieldDropAnimOn( false );
			setShieldDropAnimTicks( 0 );   // TODO: Vet this default.
			setShieldRaiseAnimOn( false );
			setShieldRaiseAnimTicks( 0 );  // TODO: Vet this default.
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
		 * This counts to 2000. When not recharging, it is 0.
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

			result.append( String.format( "SystemId:                 %s\n", SystemType.SHIELDS.getId() ) );
			result.append( String.format( "Shield Layers:            %5d (Currently filled bubbles)\n", shieldLayers ) );
			result.append( String.format( "Energy Shield Layers:     %5d\n", energyShieldLayers ) );
			result.append( String.format( "Energy Shield Max:        %5d (Layers when fully charged)\n", energyShieldLayers ) );
			result.append( String.format( "Shield Recharge Ticks:    %5d\n", shieldRechargeTicks ) );
			result.append( "\n" );
			result.append( String.format( "Shield Drop Anim:   Play: %-5b, Ticks: %4d\n", shieldDropAnimOn, shieldDropAnimTicks ) );
			result.append( String.format( "Shield Raise Anim:  Play: %-5b, Ticks: %4d\n", shieldRaiseAnimOn, shieldRaiseAnimTicks ) );
			result.append( String.format( "Energy Shield Anim: Play: %-5b, Ticks: %4d\n", energyShieldAnimOn, energyShieldAnimTicks ) );
			result.append( String.format( "Lambda?, Mu?:           %7d,%7d (Some kind of coord?)\n", unknownLambda, unknownMu ) );

			return result.toString();
		}
	}

	public static class CloakingInfo extends ExtendedSystemInfo {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int cloakTicksGoal = 0;
		private int cloakTicks = Integer.MIN_VALUE;


		public CloakingInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected CloakingInfo( CloakingInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
			unknownBeta = srcInfo.getUnknownBeta();
			cloakTicksGoal = srcInfo.getCloakTicksGoal();
			cloakTicks = srcInfo.getCloakTicks();
		}

		@Override
		public CloakingInfo copy() { return new CloakingInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		@Override
		public void commandeer() {
			setUnknownAlpha( 0 );    // TODO: Vet this default.
			setUnknownBeta( 0 );     // TODO: Vet this default.
			setCloakTicksGoal( 0 );
			setCloakTicks( Integer.MIN_VALUE );
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
		 * When this is not set, it is MIN_INT. After reaching or passing the
		 * goal, this value lingers.
		 *
		 * @param n a positive int less than, or equal to, the goal (or MIN_INT)
		 *
		 * @see #setCloakTicksGoal(int)
		 */
		public void setCloakTicks( int n ) { cloakTicks = n; }
		public int getCloakTicks() { return cloakTicks; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "SystemId:                 %s\n", SystemType.CLOAKING.getId() ) );
			result.append( String.format( "Alpha?:                 %7d\n", unknownAlpha ) );
			result.append( String.format( "Beta?:                  %7d\n", unknownBeta ) );
			result.append( String.format( "Cloak Ticks Goal:       %7d\n", cloakTicksGoal ) );
			result.append( String.format( "Cloak Ticks:            %7s\n", (cloakTicks == Integer.MIN_VALUE ? "MIN" : cloakTicks) ) );

			return result.toString();
		}
	}

	/**
	 * Extended info about the Hacking system.
	 *
	 * @see DoorState
	 * @see SystemState#setHacked(boolean)
	 * @see SystemState#setHackLevel(int)
	 */
	public static class HackingInfo extends ExtendedSystemInfo {
		private SystemType targetSystemType = null;
		private int unknownBeta = 0;
		private boolean dronePodVisible = false;
		private int unknownDelta = 0;

		private int unknownEpsilon = 0;
		private int unknownZeta = 0;
		private int unknownEta = 0;

		private int disruptionTicks = 0;
		private int disruptionTicksGoal = 10000;
		private boolean disrupting = false;

		private DronePodState dronePod = null;


		/**
		 * Constructs an incomplete HackingInfo.
		 *
		 * It will need a hacking DronePodState.
		 */
		public HackingInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 *
		 * The DronePodState will be copy-constructed as well.
		 */
		protected HackingInfo( HackingInfo srcInfo ) {
			super( srcInfo );
			targetSystemType = srcInfo.getTargetSystemType();
			unknownBeta = srcInfo.getUnknownBeta();
			dronePodVisible = srcInfo.isDronePodVisible();
			unknownDelta = srcInfo.getUnknownDelta();
			unknownEpsilon = srcInfo.getUnknownEpsilon();
			unknownZeta = srcInfo.getUnknownZeta();
			unknownEta = srcInfo.getUnknownEta();
			disruptionTicks = srcInfo.getDisruptionTicks();
			disruptionTicksGoal = srcInfo.getDisruptionTicksGoal();
			disrupting = srcInfo.isDisrupting();
			dronePod = new DronePodState( srcInfo.getDronePod() );
		}

		@Override
		public HackingInfo copy() { return new HackingInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		@Override
		public void commandeer() {
			setTargetSystemType( null );
			setUnknownBeta( 0 );
			setDronePodVisible( false );
			setUnknownDelta( 0 );

			setUnknownEpsilon( 0 );
			setUnknownZeta( 0 );
			setUnknownEta( 0 );

			setDisruptionTicks( 0 );
			setDisruptionTicksGoal( 10000 );
			setDisrupting( false );

			if ( getDronePod() != null ) {
				getDronePod().commandeer();
			}
		}

		/**
		 * Sets the target system to hack.
		 *
		 * This is set when the drone pod is launched. Pressing the hack
		 * button to select a system while paused will have no immediate
		 * effect on a saved game; unpausing is necessary for tha pod to
		 * launch and commit the changes.
		 *
		 * Editing this value when a system had already been hacked will not
		 * unhack the original system. Upon loading, FTL will modify the new
		 * system's hacked and hackLevel values, reveal the room, lock the
		 * doors, etc. If edited while disrupting, the previous system will
		 * stay disrupted indefinitely. Only the current system will return to
		 * normal when disruptionTicks reaches its goal.
		 *
		 * FTL 1.5.13 bug: The hacking system only remembers the type of system
		 * targeted, not a specific room. The rebel flagship has multiple
		 * artillery rooms. An in-game choice to hack any of the right three
		 * rooms will set the 'hacked' flag on that SystemState, but upon
		 * reloading, the hacking system will seek the *first* artillery room
		 * (the leftmost one) instead, which will get marked as 'hacked' and be
		 * subject to disruption. The original room will still have its flag
		 * lingering from before, but the hacking system only affects one room
		 * and it already picked the left one. Both flagged rooms will be
		 * revealed, but disruption will only affect only the left one.
		 *
		 * When not set, this is null.
		 */
		public void setTargetSystemType( SystemType systemType ) { targetSystemType = systemType; }
		public SystemType getTargetSystemType() { return targetSystemType; }

		/**
		 * Unknown.
		 *
		 * Observed values: Went from 0 to 1 when drone pod was launched.
		 * Went from 1 to 0 when hacking system was inoperative (either from
		 * damage or depowering) while the pod was still attached.
		 */
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

		/**
		 * Sets the drone pod's visibility.
		 *
		 * Editing this to false after the drone pod has been launched will
		 * only make the pod invisible. The Hacking system will continue to
		 * function normally as if the pod were there.
		 *
		 * Observed values: true (when launched), false (when the nearby ship
		 * is defeated and has disappeared).
		 */
		public void setDronePodVisible( boolean b ) { dronePodVisible = b; }
		public boolean isDronePodVisible() { return dronePodVisible; }

		/**
		 * Unknown.
		 *
		 * Went from 0 to 1 when hacking drone pod was launched.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		public void setUnknownZeta( int n ) { unknownZeta = n; }
		public int getUnknownZeta() { return unknownZeta; }

		public void setUnknownEta( int n ) { unknownEta = n; }
		public int getUnknownEta() { return unknownEta; }

		/**
		 * Sets elapsed time while systems are disrupted.
		 *
		 * When this is not set, it is 0. After reaching or passing the goal,
		 * this value lingers.
		 *
		 * When the goal is reached, the Hacking system will get 4 ionized bars
		 * (ionized bars had been -1 while disrupting).
		 *
		 * @param n a positive int less than, or equal to, the goal
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

		/**
		 * Sets whether an enemy system is currently being disrupted.
		 *
		 * @see SystemState.setHackLevel(int)
		 */
		public void setDisrupting( boolean b ) { disrupting = b; }
		public boolean isDisrupting() { return disrupting; }

		public void setDronePod( DronePodState pod ) { dronePod = pod; }
		public DronePodState getDronePod() { return dronePod; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format( "%d", n );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append( String.format( "SystemId:                 %s\n", SystemType.HACKING.getId() ) );
			result.append( String.format( "Target SystemId:          %s\n", (targetSystemType != null ? targetSystemType.getId() : "N/A") ) );
			result.append( String.format( "Beta?:                  %7d\n", unknownBeta ) );
			result.append( String.format( "Drone Pod Visible:      %7b\n", dronePodVisible ) );
			result.append( String.format( "Delta?:                 %7d\n", unknownDelta ) );
			result.append( String.format( "Epsilon?:               %7d\n", unknownEpsilon ) );
			result.append( String.format( "Zeta?:                  %7d\n", unknownZeta ) );
			result.append( String.format( "Eta?:                   %7d\n", unknownEta ) );
			result.append( String.format( "Disruption Ticks:       %7d\n", disruptionTicks ) );
			result.append( String.format( "Disruption Ticks Goal:  %7d\n", disruptionTicksGoal ) );
			result.append( String.format( "Disrupting:             %7b\n", disrupting ) );

			result.append( "\nDrone Pod...\n" );
			if ( dronePod != null ) {
				result.append( dronePod.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}

	public static class MindInfo extends ExtendedSystemInfo {
		private int mindControlTicksGoal = 0;
		private int mindControlTicks = 0;


		/**
		 * Constructor.
		 */
		public MindInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected MindInfo( MindInfo srcInfo ) {
			super( srcInfo );
			mindControlTicksGoal = srcInfo.getMindControlTicksGoal();
			mindControlTicks = srcInfo.getMindControlTicks();
		}

		@Override
		public MindInfo copy() { return new MindInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 */
		@Override
		public void commandeer() {
			setMindControlTicks( 0 );
			setMindControlTicksGoal( 0 );
		}

		/**
		 * Sets elapsed time while crew are mind controlled.
		 *
		 * After reaching or passing the goal, this value lingers.
		 *
		 * When the goal is reached, the Mind system will get 4 ionized bars
		 * (ionized bars had been -1 while disrupting).
		 *
		 * @param n a positive int less than, or equal to, the goal
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

			result.append( String.format( "SystemId:                 %s\n", SystemType.MIND.getId() ) );
			result.append( String.format( "Mind Ctrl Ticks:        %7d\n", mindControlTicks ) );
			result.append( String.format( "Mind Ctrl Ticks Goal:   %7d\n", mindControlTicksGoal ) );

			return result.toString();
		}
	}

	public static class ArtilleryInfo extends ExtendedSystemInfo {
		private WeaponModuleState weaponMod = null;


		/**
		 * Constructor.
		 *
		 * It will need a WeaponModuleState.
		 */
		public ArtilleryInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected ArtilleryInfo( ArtilleryInfo srcInfo ) {
			super( srcInfo );
			weaponMod = new WeaponModuleState( srcInfo.getWeaponModule() );
		}

		@Override
		public ArtilleryInfo copy() { return new ArtilleryInfo( this ); }

		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		@Override
		public void commandeer() {
			if ( getWeaponModule() != null ) {
				getWeaponModule().commandeer();
			}
		}

		public void setWeaponModule( WeaponModuleState weaponMod ) { this.weaponMod = weaponMod; }
		public WeaponModuleState getWeaponModule() { return weaponMod; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "SystemId:                 %s\n", SystemType.ARTILLERY.getId() ) );

			result.append( "\nWeapon Module...\n" );
			if ( weaponMod != null ) {
				result.append( weaponMod.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	public static enum ProjectileType {
		BEAM, BOMB, LASER_OR_BURST, MISSILE, ROCK_OR_EXPLOSION, PDS, INVALID
	}



	/**
	 * Constants for projectile/damage ownership.
	 *
	 * OwnerId (-1, 0, 1)
	 */
	public static enum Affiliation {
		OTHER, PLAYER_SHIP, NEARBY_SHIP
	}



	public static class ProjectileState {
		private ProjectileType projectileType = ProjectileType.INVALID;
		private int currentPosX = 0, currentPosY = 0;
		private int prevPosX = 0, prevPosY = 0;
		private int speed = 0;
		private int goalPosX = 0, goalPosY = 0;
		private int heading = 0;
		private int ownerId = 0;
		private int selfId = 0;

		private DamageState damage = null;

		private int lifespan = 0;
		private int destinationSpace = 0;
		private int currentSpace = 0;
		private int targetId = 0;
		private boolean dead = false;

		private String deathAnimId = "";
		private String flightAnimId = "";

		private AnimState deathAnim = new AnimState();
		private AnimState flightAnim = new AnimState();

		private int velocityX = 0, velocityY = 0;
		private boolean missed = false;
		private boolean hitTarget = false;

		private String hitSolidSound = "";
		private String hitShieldSound = "";
		private String missSound = "";

		private int entryAngle = -1;  // Guess: X degrees CCW, where 0 is due East.
		private boolean startedDying = false;
		private boolean passedTarget = false;

		private int type = 0;
		private boolean broadcastTarget = false;

		private ExtendedProjectileInfo extendedInfo = null;


		/**
		 * Constructs an incomplete ProjectileState.
		 *
		 * It will need Damage and type-specific extended info.
		 */
		public ProjectileState() {
		}

		/**
		 * Copy constructor.
		 *
		 * Each anim, Damage, and ExtendedProjectileInfo will be
		 * copy-constructed as well.
		 */
		public ProjectileState( ProjectileState srcProjectile ) {
			projectileType = srcProjectile.getProjectileType();
			currentPosX = srcProjectile.getCurrentPositionX();
			currentPosY = srcProjectile.getCurrentPositionY();
			prevPosX = srcProjectile.getPreviousPositionX();
			prevPosY = srcProjectile.getPreviousPositionY();
			speed = srcProjectile.getSpeed();
			goalPosX = srcProjectile.getGoalPositionX();
			goalPosY = srcProjectile.getGoalPositionY();
			heading = srcProjectile.getHeading();
			ownerId = srcProjectile.getOwnerId();
			selfId = srcProjectile.getSelfId();

			damage = new DamageState( srcProjectile.getDamage() );

			lifespan = srcProjectile.getLifespan();
			destinationSpace = srcProjectile.getDestinationSpace();
			currentSpace = srcProjectile.getCurrentSpace();
			targetId = srcProjectile.getTargetId();
			dead = srcProjectile.isDead();

			deathAnimId = srcProjectile.getDeathAnimId();
			flightAnimId = srcProjectile.getFlightAnimId();

			deathAnim = new AnimState( srcProjectile.getDeathAnim() );
			flightAnim = new AnimState( srcProjectile.getFlightAnim() );

			velocityX = srcProjectile.getVelocityX();
			velocityY = srcProjectile.getVelocityY();
			missed = srcProjectile.hasMissed();
			hitTarget = srcProjectile.hasHitTarget();

			hitSolidSound = srcProjectile.getHitSolidSound();
			hitShieldSound = srcProjectile.getHitShieldSound();
			missSound = srcProjectile.getMissSound();

			entryAngle = srcProjectile.getEntryAngle();
			startedDying = srcProjectile.hasStartedDying();
			passedTarget = srcProjectile.hasPassedTarget();

			type = srcProjectile.getType();
			broadcastTarget = srcProjectile.getBroadcastTarget();

			if ( srcProjectile.getExtendedInfo( ExtendedProjectileInfo.class ) != null ) {
				extendedInfo = srcProjectile.getExtendedInfo(  ExtendedProjectileInfo.class  ).copy();
			}
		}

		public void setProjectileType( ProjectileType t ) { projectileType = t; }
		public ProjectileType getProjectileType() { return projectileType; }

		public void setCurrentPositionX( int n ) { currentPosX = n; }
		public void setCurrentPositionY( int n ) { currentPosY = n; }
		public int getCurrentPositionX() { return currentPosX; }
		public int getCurrentPositionY() { return currentPosY; }

		public void setPreviousPositionX( int n ) { prevPosX = n; }
		public void setPreviousPositionY( int n ) { prevPosY = n; }
		public int getPreviousPositionX() { return prevPosX; }
		public int getPreviousPositionY() { return prevPosY; }

		/**
		 * Sets the projectile's speed.
		 *
		 * This is a pseudo-float based on the 'speed' tag of the
		 * WeaponBlueprint's xml.
		 */
		public void setSpeed( int n ) { speed = n; }
		public int getSpeed() { return speed; }

		public void setGoalPositionX( int n ) { goalPosX = n; }
		public void setGoalPositionY( int n ) { goalPosY = n; }
		public int getGoalPositionX() { return goalPosX; }
		public int getGoalPositionY() { return goalPosY; }

		/**
		 * Set's the projectile's orientation.
		 *
		 * MISSILE_2's image file points north.
		 * A heading of 0 renders it pointing east.
		 * A heading of 45 points southeast, pivoting the body around the tip.
		 * A heading of 90 points south, with the body above the pivot point.
		 *
		 * @param n degrees clockwise (may be negative)
		 */
		public void setHeading( int n ) { heading = n; }
		public int getHeading() { return heading; }

		/**
		 * Unknown.
		 *
		 * @param n player ship (0) or nearby ship (1), even for drones' projectiles
		 */
		public void setOwnerId( int n ) { ownerId = n; }
		public int getOwnerId() { return ownerId; }

		/**
		 * Unknown.
		 *
		 * A unique number for this projectile, presumably copied from some
		 * global counter which increments with each new projectile.
		 *
		 * The DamageState will usually have the same value set for its own
		 * selfId. But not always!? Projectile type and pending/fired status are
		 * not predictive.
		 */
		public void setSelfId( int n ) { selfId = n; }
		public int getSelfId() { return selfId; }

		public void setDamage( DamageState damage ) { this.damage = damage; }
		public DamageState getDamage() { return damage; }

		/**
		 * Unknown.
		 *
		 * There doesn't appear to be a ticks field to track when to start
		 * dying?
		 */
		public void setLifespan( int n ) { lifespan = n; }
		public int getLifespan() { return lifespan; }

		/**
		 * Sets which ship to eventually use as the origin for position
		 * coordinates.
		 *
		 * @param n player ship (0) or nearby ship (1)
		 * @see #setCurrentSpace(int)
		 */
		public void setDestinationSpace( int n ) { destinationSpace = n; }
		public int getDestinationSpace() { return destinationSpace; }

		/**
		 * Sets which ship to use as the origin for position coordinates.
		 *
		 * @param n player ship (0) or nearby ship (1)
		 * @see #setDestinationSpace(int)
		 */
		public void setCurrentSpace( int n ) { currentSpace = n; }
		public int getCurrentSpace() { return currentSpace; }

		/**
		 * Unknown.
		 *
		 * @param n player ship (0) or nearby ship (1)
		 *
		 * @see #setDestinationSpace(int)
		 * @see #setOwnerId(int)
		 */
		public void setTargetId( int n ) { targetId = n; }
		public int getTargetId() { return targetId; }

		public void setDead( boolean b ) { dead = b; }
		public boolean isDead() { return dead; }

		public void setDeathAnimId( String s ) { deathAnimId = s; }
		public String getDeathAnimId() { return deathAnimId; }

		/**
		 * Sets an animSheet to play depcting the projectile in flight.
		 *
		 * TODO: This has been observed as "" when it's an asteroid!?
		 */
		public void setFlightAnimId( String s ) { flightAnimId = s; }
		public String getFlightAnimId() { return flightAnimId; }

		/**
		 * Sets the death anim state, played on impact.
		 *
		 * TODO: Determine what happens when the projectile is shot.
		 *
		 * @see #setDeathAnimId(String)
		 */
		public void setDeathAnim( AnimState anim ) { deathAnim = anim; }
		public AnimState getDeathAnim() { return deathAnim; }

		/**
		 * Sets the flight anim state, played while in transit.
		 *
		 * Newly spawned projectiles, and pending ones that haven't been fired
		 * yet, have their flightAnim's playing set to true.
		 *
		 * @see #setFlightAnimId(String)
		 */
		public void setFlightAnim( AnimState anim ) { flightAnim = anim; }
		public AnimState getFlightAnim() { return flightAnim; }

		public void setVelocityX( int n ) { velocityX = n; }
		public void setVelocityY( int n ) { velocityY = n; }
		public int getVelocityX() { return velocityX; }
		public int getVelocityY() { return velocityY; }

		/**
		 * Sets whether this projectile will never hit its target.
		 *
		 * FTL will mark it as missed before it passes its target. This is
		 * probably set when it's created.
		 *
		 * @see #setPassedTarget(boolean)
		 */
		public void setMissed( boolean b ) { missed = b; }
		public boolean hasMissed() { return missed; }

		/**
		 * Sets whether this projectile hit a target (even shields).
		 */
		public void setHitTarget( boolean b ) { hitTarget = b; }
		public boolean hasHitTarget() { return hitTarget; }

		/**
		 * Sets the sound to play when this projectile hits something solid.
		 *
		 * This will be a tag name from "sounds.xml", such as "hitHull2".
		 */
		public void setHitSolidSound( String s ) { hitSolidSound = s; }
		public String getHitSolidSound() { return hitSolidSound; }

		/**
		 * Sets the sound to play when this projectile hits shields.
		 *
		 * This will be a tag name from "sounds.xml", such as "hitShield3".
		 */
		public void setHitShieldSound( String s ) { hitShieldSound = s; }
		public String getHitShieldSound() { return hitShieldSound; }

		/**
		 * Sets the sound to play when this projectile misses.
		 *
		 * This will be a tag name from "sounds.xml", such as "miss".
		 */
		public void setMissSound( String s ) { missSound = s; }
		public String getMissSound() { return missSound; }

		/**
		 * Unknown.
		 *
		 * When not set, this is -1.
		 */
		public void setEntryAngle( int n ) { entryAngle = n; }
		public int getEntryAngle() { return entryAngle; }

		/**
		 * Unknown.
		 */
		public void setStartedDying( boolean b ) { startedDying = b; }
		public boolean hasStartedDying() { return startedDying; }

		/**
		 * Sets whether this projectile has passed its target.
		 *
		 * FTL will have already marked it as having missed first.
		 *
		 * @see setMissed(boolean)
		 */
		public void setPassedTarget( boolean b ) { passedTarget = b; }
		public boolean hasPassedTarget() { return passedTarget; }

		public void setType( int n ) { type = n; }
		public int getType() { return type; }

		/**
		 * Sets whether a red dot should be painted at the targeted location.
		 *
		 * This is used by burst volleys (e.g., flak).
		 */
		public void setBroadcastTarget( boolean b ) { broadcastTarget = b; }
		public boolean getBroadcastTarget() { return broadcastTarget; }

		public void setExtendedInfo( ExtendedProjectileInfo info ) {
			extendedInfo = info;
		}
		public <T extends ExtendedProjectileInfo> T getExtendedInfo( Class<T> infoClass ) {
			if ( extendedInfo == null ) return null;
			return infoClass.cast( extendedInfo );
		}


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format( "%d", n );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Projectile Type:   %s\n", projectileType.toString() ) );

			if ( ProjectileType.INVALID.equals( projectileType ) ) {
				result.append( "\n" );
				result.append( "(When Projectile Type is INVALID, no other fields are set.)\n" );
				return result.toString();
			}

			result.append( String.format( "Current Position:  %8d,%8d (%9.03f,%9.03f)\n", currentPosX, currentPosY, currentPosX/1000f, currentPosY/1000f ) );
			result.append( String.format( "Previous Position: %8d,%8d (%9.03f,%9.03f)\n", prevPosX, prevPosY, prevPosX/1000f, prevPosY/1000f ) );
			result.append( String.format( "Speed:             %8d (%7.03f)\n", speed, speed/1000f ) );
			result.append( String.format( "Goal Position:     %8d,%8d (%9.03f,%9.03f)\n", goalPosX, goalPosY, goalPosX/1000f, goalPosY/1000f ) );
			result.append( String.format( "Heading:           %8d\n", heading ) );
			result.append( String.format( "Owner Id?:         %8d\n", ownerId ) );
			result.append( String.format( "Self Id?:          %8d\n", selfId ) );

			result.append( String.format( "\nDamage...\n" ) );
			if ( damage != null ) {
				result.append( damage.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );

			result.append( String.format( "Lifespan:          %8d\n", lifespan ) );
			result.append( String.format( "Destination Space: %8d\n", destinationSpace ) );
			result.append( String.format( "Current Space:     %8d\n", currentSpace ) );
			result.append( String.format( "Target Id?:        %8d\n", targetId ) );
			result.append( String.format( "Dead:              %8b\n", dead ) );
			result.append( String.format( "Death AnimId:      %s\n", deathAnimId ) );
			result.append( String.format( "Flight AnimId:     %s\n", flightAnimId ) );

			result.append( String.format( "\nDeath Anim?...\n" ) );
			if ( deathAnim != null ) {
				result.append( deathAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( String.format( "\nFlight Anim?...\n" ) );
			if ( flightAnim != null ) {
				result.append( flightAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );

			result.append( String.format( "Velocity (x,y):    %8d,%6d (%7.03f,%7.03f)\n", velocityX, velocityY, velocityX/1000f, velocityY/1000f));
			result.append( String.format( "Missed:            %8b\n", missed ) );
			result.append( String.format( "Hit Target:        %8b\n", hitTarget ) );
			result.append( String.format( "Hit Solid Sound:   %s\n", hitSolidSound ) );
			result.append( String.format( "Hit Shield Sound:  %s\n", hitShieldSound ) );
			result.append( String.format( "Miss Sound:        %s\n", missSound ) );
			result.append( String.format( "Entry Angle?:      %8s\n", prettyInt( entryAngle ) ) );
			result.append( String.format( "Started Dying:     %8b\n", startedDying ) );
			result.append( String.format( "Passed Target?:    %8b\n", passedTarget ) );

			result.append( "\n" );

			result.append( String.format( "Type?:             %8d\n", type ) );
			result.append( String.format( "Broadcast Target:  %8b (Red dot at targeted location)\n", broadcastTarget ) );

			result.append( String.format( "\nExtended Projectile Info...\n" ) );
			if ( extendedInfo != null ) {
				result.append( extendedInfo.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

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
		private int ownerId = -1;
		private int selfId = -1;
		private boolean lockdown = false;
		private boolean crystalShard = false;
		private int stunChance = 0;
		private int stunAmount = 0;


		/**
		 * Constructor.
		 */
		public DamageState() {
		}

		/**
		 * Copy constructor.
		 */
		public DamageState( DamageState srcDamage ) {
			hullDamage = srcDamage.getHullDamage();
			shieldPiercing = srcDamage.getShieldPiercing();
			fireChance = srcDamage.getFireChance();
			breachChance = srcDamage.getBreachChance();
			ionDamage = srcDamage.getIonDamage();
			systemDamage = srcDamage.getSystemDamage();
			personnelDamage = srcDamage.getPersonnelDamage();
			boolean hullBuster = srcDamage.isHullBuster();
			ownerId = srcDamage.getOwnerId();
			selfId = srcDamage.getSelfId();
			lockdown = srcDamage.isLockdown();
			crystalShard = srcDamage.isCrystalShard();
			stunChance = srcDamage.getStunChance();
			stunAmount = srcDamage.getStunAmount();
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

		/**
		 * Unknown.
		 *
		 * When not set, this is -1.
		 *
		 * This only seems to be set by projectiles from bomb weapons: 1 when
		 * from the nearby ship, once it materializes (-1 a moment before).
		 */
		public void setOwnerId( int n ) { ownerId = n; }
		public int getOwnerId() { return ownerId; }

		/**
		 * Unknown.
		 *
		 * When not set, this is -1.
		 */
		public void setSelfId( int n ) { selfId = n; }
		public int getSelfId() { return selfId; }

		public void setLockdown( boolean b ) { lockdown = b; }
		public void setCrystalShard( boolean b ) { crystalShard = b; }
		public void setStunChance( int n ) { stunChance = n; }
		public void setStunAmount( int n ) { stunAmount = n; }

		public boolean isLockdown() { return lockdown; }
		public boolean isCrystalShard() { return crystalShard; }
		public int getStunChance() { return stunChance; }
		public int getStunAmount() { return stunAmount; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Hull Damage:       %7d\n", hullDamage ) );
			result.append( String.format( "ShieldPiercing:    %7d\n", shieldPiercing ) );
			result.append( String.format( "Fire Chance:       %7d\n", fireChance ) );
			result.append( String.format( "Breach Chance:     %7d\n", breachChance ) );
			result.append( String.format( "Ion Damage:        %7d\n", ionDamage ) );
			result.append( String.format( "System Damage:     %7d\n", systemDamage ) );
			result.append( String.format( "Personnel Damage:  %7d\n", personnelDamage ) );
			result.append( String.format( "Hull Buster:       %7b (2x Hull damage vs systemless rooms)\n", hullBuster ) );
			result.append( String.format( "Owner Id?:         %7d\n", ownerId ) );
			result.append( String.format( "Self Id?:          %7d\n", selfId ) );
			result.append( String.format( "Lockdown:          %7b\n", lockdown ) );
			result.append( String.format( "Crystal Shard:     %7b\n", crystalShard ) );
			result.append( String.format( "Stun Chance:       %7d\n", stunChance ) );
			result.append( String.format( "Stun Amount:       %7d\n", stunAmount ) );

			return result.toString();
		}
	}

	public static abstract class ExtendedProjectileInfo {

		protected ExtendedProjectileInfo() {
		}

		protected ExtendedProjectileInfo( ExtendedProjectileInfo srcInfo ) {
		}

		/**
		 * Blindly copy-constructs objects.
		 *
		 * Subclasses override this with return values of their own type.
		 */
		public abstract ExtendedProjectileInfo copy();
	}

	public static class EmptyProjectileInfo extends ExtendedProjectileInfo {

		/**
		 * Constructor.
		 */
		public EmptyProjectileInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected EmptyProjectileInfo( EmptyProjectileInfo srcInfo ) {
			super( srcInfo );
		}

		@Override
		public EmptyProjectileInfo copy() { return new EmptyProjectileInfo( this ); }

		@Override
		public String toString() {
			return "N/A\n";
		}
	}

	public static class IntegerProjectileInfo extends ExtendedProjectileInfo {
		private int[] unknownAlpha;

		/**
		 * Constructs an incomplete IntegerProjectileInfo.
		 *
		 * A number of integers equal to size will need to be set.
		 */
		public IntegerProjectileInfo( int size ) {
			super();
			unknownAlpha = new int[size];
		}

		/**
		 * Copy constructor.
		 */
		protected IntegerProjectileInfo( IntegerProjectileInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = new int[srcInfo.getSize()];
			for ( int i=0; i < unknownAlpha.length; i++ ) {
				unknownAlpha[i] = srcInfo.get( i );
			}
		}

		@Override
		public IntegerProjectileInfo copy() { return new IntegerProjectileInfo( this ); }

		public int getSize() { return unknownAlpha.length; }

		public void set( int index, int n ) { unknownAlpha[index] = n; }
		public int get( int index ) { return unknownAlpha[index]; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format( "%d", n );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Type:               Unknown Info\n" ) );

			result.append( String.format( "\nAlpha?...\n" ) );
			for ( int i=0; i < unknownAlpha.length; i++ ) {
				result.append( String.format( "%7s", prettyInt( unknownAlpha[i] ) ) );

				if ( i != unknownAlpha.length-1 ) {
					if ( i % 2 == 1 ) {
						result.append( ",\n" );
					} else {
						result.append( ", " );
					}
				}
			}
			result.append( "\n" );

			return result.toString();
		}
	}

	/**
	 * Extended info for Beam projectiles.
	 *
	 * Beam projectiles have several parts.
	 *   An emission line, drawn from the weapon.
	 *   A strafe line, drawn toward the target ship.
	 *   A spot, where the strafe line hits the target ship.
	 *   A swath, the path the spot tries to travel along.
	 *
	 * For ship weapons, the emission line ends off-screen, and the strafe line
	 * begins somewhere off-screen. For Beam drones, the emission line is
	 * ignored, and the strafe line is drawn from the drone pod directly to the
	 * swath.
	 *
	 * The ProjectileState's current/previous position is the emission line's
	 * source - at the weapon or drone pod that fired. The ProjectileState's
	 * goal position is where the spot is, along the swath (shield blocking is
	 * not considered).
	 */
	public static class BeamProjectileInfo extends ExtendedProjectileInfo {
		private int emissionEndX = 0, emissionEndY = 0;
		private int strafeSourceX = 0, strafeSourceY = 0;
		private int strafeEndX = 0, strafeEndY = 0;
		private int unknownBetaX = 0, unknownBetaY = 0;
		private int swathEndX = 0, swathEndY = 0;
		private int swathStartX = 0, swathStartY = 0;
		private int unknownGamma = 0;
		private int swathLength = 0;
		private int unknownDelta = 0;
		private int unknownEpsilonX = 0, unknownEpsilonY = 0;
		private int unknownZeta = 0;
		private int unknownEta = 0;
		private int emissionAngle = 0;
		private boolean unknownIota = false;
		private boolean unknownKappa = false;
		private boolean fromDronePod = false;
		private boolean unknownMu = false;
		private boolean unknownNu = false;


		/**
		 * Constructor.
		 */
		public BeamProjectileInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected BeamProjectileInfo( BeamProjectileInfo srcInfo ) {
			super( srcInfo );
			emissionEndX = srcInfo.getEmissionEndX();
			emissionEndY = srcInfo.getEmissionEndY();
			strafeSourceX = srcInfo.getStrafeSourceX();
			strafeSourceY = srcInfo.getStrafeSourceY();
			strafeEndX = srcInfo.getStrafeEndX();
			strafeEndY = srcInfo.getStrafeEndY();
			unknownBetaX = srcInfo.getUnknownBetaX();
			unknownBetaY = srcInfo.getUnknownBetaY();
			swathEndX = srcInfo.getSwathEndX();
			swathEndY = srcInfo.getSwathEndY();
			swathStartX = srcInfo.getSwathStartX();
			swathStartY = srcInfo.getSwathStartY();
			unknownGamma = srcInfo.getUnknownGamma();
			swathLength = srcInfo.getSwathLength();
			unknownDelta = srcInfo.getUnknownDelta();
			unknownEpsilonX = srcInfo.getUnknownEpsilonX();
			unknownEpsilonY = srcInfo.getUnknownEpsilonY();
			unknownZeta = srcInfo.getUnknownZeta();
			unknownEta = srcInfo.getUnknownEta();
			emissionAngle = srcInfo.getEmissionAngle();
			unknownIota = srcInfo.getUnknownIota();
			unknownKappa = srcInfo.getUnknownKappa();
			fromDronePod = srcInfo.isFromDronePod();
			unknownMu = srcInfo.getUnknownMu();
			unknownNu = srcInfo.getUnknownNu();
		}

		@Override
		public BeamProjectileInfo copy() { return new BeamProjectileInfo( this ); }

		/**
		 * Sets the off-screen endpoint of the line drawn from the weapon.
		 *
		 * For Beam drones, this point will be the same as strafeSource, except
		 * this y will be shifted upward by -2000. The emission line won't be
		 * drawn in that case, obvisously, since the drone is right there.
		 *
		 * This is relative to the ship space the beam was emitted from
		 * (e.g., weapon of a player ship, or a drone hovering over a nearby
		 * ship).
		 */
		public void setEmissionEndX( int n ) { emissionEndX = n; }
		public void setEmissionEndY( int n ) { emissionEndY = n; }
		public int getEmissionEndX() { return emissionEndX; }
		public int getEmissionEndY() { return emissionEndY; }

		/**
		 * Sets the off-screen endpoint of the line drawn toward the swath.
		 *
		 * This is relative to the target ship.
		 */
		public void setStrafeSourceX( int n ) { strafeSourceX = n; }
		public void setStrafeSourceY( int n ) { strafeSourceY = n; }
		public int getStrafeSourceX() { return strafeSourceX; }
		public int getStrafeSourceY() { return strafeSourceY; }

		/**
		 * Sets the on-screen endpoint of the line drawn toward the swath.
		 *
		 * When shields are up, this point is not on the swath but at the
		 * intersection of the line and shield oval.
		 *
		 * This is relative to the target ship.
		 */
		public void setStrafeEndX( int n ) { strafeEndX = n; }
		public void setStrafeEndY( int n ) { strafeEndY = n; }
		public int getStrafeEndX() { return strafeEndX; }
		public int getStrafeEndY() { return strafeEndY; }

		/**
		 * Unknown.
		 *
		 * Observed values: The current location of the travelling spot.
		 *
		 * This is relative to the target ship.
		 */
		public void setUnknownBetaX( int n ) { unknownBetaX = n; }
		public void setUnknownBetaY( int n ) { unknownBetaY = n; }
		public int getUnknownBetaX() { return unknownBetaX; }
		public int getUnknownBetaY() { return unknownBetaY; }

		/**
		 * Sets the point the travelling spot will end at.
		 *
		 * This is relative to the target ship.
		 */
		public void setSwathEndX( int n ) { swathEndX = n; }
		public void setSwathEndY( int n ) { swathEndY = n; }
		public int getSwathEndX() { return swathEndX; }
		public int getSwathEndY() { return swathEndY; }

		/**
		 * Sets the point the travelling spot will start from.
		 *
		 * This is relative to the target ship.
		 */
		public void setSwathStartX( int n ) { swathStartX = n; }
		public void setSwathStartY( int n ) { swathStartY = n; }
		public int getSwathStartX() { return swathStartX; }
		public int getSwathStartY() { return swathStartY; }

		/**
		 * Unknown.
		 *
		 * This is always 1000.
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Unknown.
		 *
		 * This is a pseudo-float based on the 'length' tag of the
		 * WeaponBlueprint's xml.
		 */
		public void setSwathLength( int n ) { swathLength = n; }
		public int getSwathLength() { return swathLength; }

		/**
		 * Unknown.
		 *
		 * This is a constant, at least for a given WeaponBlueprint.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Unknown.
		 *
		 * Observed values: The current location of the travelling spot.
		 *
		 * This is relative to the target ship.
		 */
		public void setUnknownEpsilonX( int n ) { unknownEpsilonX = n; }
		public void setUnknownEpsilonY( int n ) { unknownEpsilonY = n; }
		public int getUnknownEpsilonX() { return unknownEpsilonX; }
		public int getUnknownEpsilonY() { return unknownEpsilonY; }

		/**
		 * Unknown.
		 *
		 * This is an erratic int (seen 0-350) with no clear progression from
		 * moment to moment.
		 */
		public void setUnknownZeta( int n ) { unknownZeta = n; }
		public int getUnknownZeta() { return unknownZeta; }

		/**
		 * Unknown.
		 *
		 * Possibly damage per room, based on the 'damage' tag of the
		 * WeaponBlueprint's xml? (That's DamageState's hullDamage)
		 *
		 * Observed values: 1, 2.
		 */
		public void setUnknownEta( int n ) { unknownEta = n; }
		public int getUnknownEta() { return unknownEta; }

		/**
		 * Sets the angle of the line drawn from the weapon.
		 *
		 * For ships, this will be 0 (player ship) or 270000 (nearby ship).
		 *
		 * For Beam drones, this is related to the turret angle, though this
		 * may be a large negative angle while the turret may be a small
		 * positive one.
		 *
		 * Observed values: 0, 270000, -323106.
		 *
		 * @param n a pseudo-float (n degrees clockwise from east)
		 */
		public void setEmissionAngle( int n ) { emissionAngle = n; }
		public int getEmissionAngle() { return emissionAngle; }

		/**
		 * Unknown.
		 */
		public void setUnknownIota( boolean b ) { unknownIota = b; }
		public boolean getUnknownIota() { return unknownIota; }

		/**
		 * Unknown.
		 *
		 * Seems to be true only when the target ship's shields are down, and
		 * the line will reach the swath without being blocked (even set while
		 * pending).
		 */
		public void setUnknownKappa( boolean b ) { unknownKappa = b; }
		public boolean getUnknownKappa() { return unknownKappa; }

		/**
		 * Sets whether this this beam was fired from a drone pod or a ship
		 * weapon.
		 *
		 * For ship weapons, this is false, and both the emission and strafe
		 * lines will be drawn.
		 *
		 * If true, only the strafe line be drawn - from the ProjectileState's
		 * current position (the drone's aperture).
		 *
		 * If edited to false on a drone, the emission line will be drawn,
		 * northward, with no strafe line - completely missing the target ship.
		 * This weirdness may have to to with current/destination space not
		 * being separate, as they would be for a ship weapon?
		 */
		public void setFromDronePod( boolean b ) { fromDronePod = b; }
		public boolean isFromDronePod() { return fromDronePod; }

		/**
		 * Unknown.
		 */
		public void setUnknownMu( boolean b ) { unknownMu = b; }
		public boolean getUnknownMu() { return unknownMu; }

		/**
		 * Unknown.
		 *
		 * Might have to do with the line having hit crew?
		 */
		public void setUnknownNu( boolean b ) { unknownNu = b; }
		public boolean getUnknownNu() { return unknownNu; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Type:               Beam Info\n" ) );
			result.append( String.format( "Emission End:       %8d,%8d (%9.03f,%9.03f) (Off-screen endpoint of line from weapon)\n", emissionEndX, emissionEndY, emissionEndX/1000f, emissionEndY/1000f ) );
			result.append( String.format( "Strafe Source:      %8d,%8d (%9.03f,%9.03f) (Off-screen endpoint of line drawn toward swath)\n", strafeSourceX, strafeSourceY, strafeSourceX/1000f, strafeSourceY/1000f ) );
			result.append( String.format( "Strafe End:         %8d,%8d (%9.03f,%9.03f) (On-screen endpoint of line drawn toward swath)\n", strafeEndX, strafeEndY, strafeEndX/1000f, strafeEndY/1000f ) );
			result.append( String.format( "Beta?:              %8d,%8d (%9.03f,%9.03f)\n", unknownBetaX, unknownBetaY, unknownBetaX/1000f, unknownBetaY/1000f ) );
			result.append( String.format( "Swath End:          %8d,%8d (%9.03f,%9.03f)\n", swathEndX, swathEndY, swathEndX/1000f, swathEndY/1000f ) );
			result.append( String.format( "Swath Start:        %8d,%8d (%9.03f,%9.03f)\n", swathStartX, swathStartY, swathStartX/1000f, swathStartY/1000f ) );
			result.append( String.format( "Gamma?:             %8d\n", unknownGamma ) );
			result.append( String.format( "Swath Length:       %8d (%9.03f)\n", swathLength, swathLength/1000f ) );
			result.append( String.format( "Delta?:             %8d\n", unknownDelta ) );
			result.append( String.format( "Epsilon?:           %8d,%8d (%9.03f,%9.03f)\n", unknownEpsilonX, unknownEpsilonY, unknownEpsilonX/1000f, unknownEpsilonY/1000f ) );
			result.append( String.format( "Zeta?:              %8d\n", unknownZeta ) );
			result.append( String.format( "Eta?:               %8d\n", unknownEta ) );
			result.append( String.format( "Emission Angle:     %8d\n", emissionAngle ) );
			result.append( String.format( "Iota?:              %8b\n", unknownIota ) );
			result.append( String.format( "Kappa?:             %8b\n", unknownKappa ) );
			result.append( String.format( "From Drone Pod:     %8b\n", fromDronePod ) );
			result.append( String.format( "Mu?:                %8b\n", unknownMu ) );
			result.append( String.format( "Nu?:                %8b\n", unknownNu ) );

			return result.toString();
		}
	}

	public static class BombProjectileInfo extends ExtendedProjectileInfo {
		private int unknownAlpha = 0;
		private int fuseTicks = 400;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private boolean arrived = false;


		/**
		 * Constructor.
		 */
		public BombProjectileInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected BombProjectileInfo( BombProjectileInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
			fuseTicks = srcInfo.getFuseTicks();
			unknownGamma = srcInfo.getUnknownGamma();
			unknownDelta = srcInfo.getUnknownDelta();
			arrived = srcInfo.hasArrived();
		}

		@Override
		public BombProjectileInfo copy() { return new BombProjectileInfo( this ); }

		/**
		 * Unknown.
		 *
		 * Observed values: 0.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		/**
		 * Sets time elapsed while this bomb is about to detonate.
		 *
		 * After fading into a room, this decrements ticks from 400. At 0, the
		 * bomb's casing (flightAnimId) disappears, and the explosion anim
		 * plays. This value continues decrementing to some negative number
		 * (varies by weapon), until the explosion completes, and the
		 * projectile is gone.
		 *
		 * Changing this from a positive value to a higher one will delay the
		 * detonation. Once negative, the explosion will have already started,
		 * and setting a new positive value will only make the casing visible
		 * amidst the blast for whatever time is left of that animation.
		 *
		 * Observed values: 400 (During fade-in), 356, -205, -313, -535.
		 *
		 * @see #setArrived(boolean)
		 */
		public void setFuseTicks( int n ) { fuseTicks = n; }
		public int getFuseTicks() { return fuseTicks; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0.
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets whether this bomb has begun fading in.
		 *
		 * When FTL sees this is false, the value will become true, and the
		 * bomb will begin fading into a room. If set to false once more, the
		 * fade will start over. Fuse ticks will cease decrementing while this
		 * is false or while the fade is still in progress.
		 *
		 * When FTL sees this is true, the bomb casing is fully opaque and fuse
		 * ticks immediately resume decrementing.
		 *
		 * Newly spawned projectiles have this set to false.
		 *
		 * @see #setFuseTicks(int)
		 */
		public void setArrived( boolean b ) { arrived = b; }
		public boolean hasArrived() { return arrived; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Type:               Bomb Info\n" ) );
			result.append( String.format( "Alpha?:             %7d\n", unknownAlpha ) );
			result.append( String.format( "Fuse Ticks:         %7d (Explodes at 0)\n", fuseTicks ) );
			result.append( String.format( "Gamma?:             %7d\n", unknownGamma ) );
			result.append( String.format( "Delta?:             %7d\n", unknownDelta ) );
			result.append( String.format( "Arrived:            %7b\n", arrived ) );

			return result.toString();
		}
	}

	public static class LaserProjectileInfo extends ExtendedProjectileInfo {
		private int unknownAlpha = 0;
		private int spin = 0;

		// This class represents projectiles from both Laser and Burst weapons.

		/**
		 * Constructor.
		 */
		public LaserProjectileInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected LaserProjectileInfo( LaserProjectileInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
			spin = srcInfo.getSpin();
		}

		@Override
		public LaserProjectileInfo copy() { return new LaserProjectileInfo( this ); }

		/**
		 * Unknown.
		 *
		 * Observed values: For burst, it varies in the range 100000-3000000.
		 * Some kind of seed? For regular lasers, it is 0.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		/**
		 * Unknown.
		 *
		 * This is a pseudo-float based on the 'spin' tag of the
		 * WeaponBlueprint's xml (burst-type weapons), if present, or 0.
		 */
		public void setSpin( int n ) { spin = n; }
		public int getSpin() { return spin; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Type:               Laser/Burst Info\n" ) );
			result.append( String.format( "Alpha?:             %7d\n", unknownAlpha ) );
			result.append( String.format( "Spin:               %7d\n", spin ) );

			return result.toString();
		}
	}



	/**
	 * Extended info for PDS projectiles (called ASB in-game).
	 *
	 * This was introduced in FTL 1.6.1.
	 */
	public static class PDSProjectileInfo extends ExtendedProjectileInfo {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;
		private AnimState unknownZeta = new AnimState();

		// This class represents projectiles from PDS hazards.

		/**
		 * Constructor.
		 */
		public PDSProjectileInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected PDSProjectileInfo( PDSProjectileInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
			unknownBeta = srcInfo.getUnknownBeta();
			unknownGamma = srcInfo.getUnknownGamma();
			unknownDelta = srcInfo.getUnknownDelta();
			unknownEpsilon = srcInfo.getUnknownEpsilon();
			unknownZeta = srcInfo.getUnknownZeta();
		}

		@Override
		public PDSProjectileInfo copy() { return new PDSProjectileInfo( this ); }

		/**
		 * Unknown.
		 *
		 * Seems to be the spawn X position relative to ship space?
		 *
		 * This is a pseudo-float.
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		/**
		 * Unknown.
		 *
		 * Seems to be the spawn Y position relative to ship space?
		 *
		 * This is a pseudo-float.
		 */
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

		/**
		 * Unknown.
		 *
		 * Observed values: 1, 0.
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Unknown.
		 *
		 * Always matches the projectile's flightAnim scale.
		 *
		 * Observed values: 10277; 11438, 15690, 19896, 26832, 34719; 1139.
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0.
		 */
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		/**
		 * Unknown.
		 *
		 * Observed values:
		 *   Looping: 0.
		 *   Frame: 0, 2, 4, 5, 8.
		 *   Progress Ticks: 0, 245, 467, 648, 892.
		 *   Scale: 1.0
		 *   Position: (0, 0), (213000, 213000).
		 */
		public void setUnknownZeta( AnimState anim ) { unknownZeta = anim; }
		public AnimState getUnknownZeta() { return unknownZeta; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Type:               PDS Info\n" ) );
			result.append( String.format( "Alpha?:             %7d\n", unknownAlpha ) );
			result.append( String.format( "Beta?:              %7d\n", unknownBeta ) );
			result.append( String.format( "Gamma?:             %7d\n", unknownGamma ) );
			result.append( String.format( "Delta?:             %7d\n", unknownDelta ) );
			result.append( String.format( "Epsilon?:           %7d\n", unknownEpsilon ) );

			result.append( "\nZeta? Anim...\n" );
			if ( unknownZeta != null) {
				result.append( unknownZeta.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	public static class DronePodState {
		private DroneType droneType = null;
		private int mourningTicks = 0;
		private int currentSpace = 0;
		private int destinationSpace = -1;
		private int currentPosX = 0, currentPosY = 0;
		private int prevPosX = 0, prevPosY = 0;
		private int goalPosX = 0, goalPosY = 0;

		// This block was formerly a length 6 array named beta.
		private int unknownEpsilon = Integer.MIN_VALUE;
		private int unknownZeta = Integer.MIN_VALUE;
		private int nextTargetX = Integer.MIN_VALUE;
		private int nextTargetY = Integer.MIN_VALUE;
		private int unknownIota = Integer.MIN_VALUE;
		private int unknownKappa = Integer.MIN_VALUE;

		// This block was formerly a length 14 array named gamma.
		private int buildupTicks = -1000;
		private int stationaryTicks = 0;
		private int cooldownTicks = 0;
		private int orbitAngle = 0;
		private int turretAngle = 0;
		private int unknownXi = 0;
		private int hopsToLive = Integer.MAX_VALUE;
		private int unknownPi = 0;
		private int unknownRho = 0;
		private int overloadTicks = 0;
		private int unknownTau = -1000;
		private int unknownUpsilon = 0;
		private int deltaPosX = 0, deltaPosY = 0;

		private AnimState deathAnim = new AnimState();
		private ExtendedDronePodInfo extendedInfo = null;


		/**
		 * Constructs an incomplete DronePodState.
		 */
		public DronePodState() {
		}

		/**
		 * Copy constructor.
		 */
		public DronePodState( DronePodState srcPod ) {
			droneType = srcPod.getDroneType();
			mourningTicks = srcPod.getMourningTicks();
			currentSpace = srcPod.getCurrentSpace();
			destinationSpace = srcPod.getDestinationSpace();
			currentPosX = srcPod.getCurrentPositionX();
			currentPosY = srcPod.getCurrentPositionY();
			prevPosX = srcPod.getPreviousPositionX();
			prevPosY = srcPod.getPreviousPositionY();
			goalPosX = srcPod.getGoalPositionX();
			goalPosY = srcPod.getGoalPositionY();

			unknownEpsilon = srcPod.getUnknownEpsilon();
			unknownZeta = srcPod.getUnknownZeta();
			nextTargetX = srcPod.getNextTargetX();
			nextTargetY = srcPod.getNextTargetY();
			unknownIota = srcPod.getUnknownIota();
			unknownKappa = srcPod.getUnknownKappa();

			buildupTicks = srcPod.getBuildupTicks();
			stationaryTicks = srcPod.getStationaryTicks();
			cooldownTicks = srcPod.getCooldownTicks();
			orbitAngle = srcPod.getOrbitAngle();
			turretAngle = srcPod.getTurretAngle();
			unknownXi = srcPod.getUnknownXi();
			hopsToLive = srcPod.getHopsToLive();
			unknownPi = srcPod.getUnknownPi();
			unknownRho = srcPod.getUnknownRho();
			overloadTicks = srcPod.getOverloadTicks();
			unknownTau = srcPod.getUnknownTau();
			unknownUpsilon = srcPod.getUnknownUpsilon();
			deltaPosX = srcPod.getDeltaPositionX();
			deltaPosY = srcPod.getDeltaPositionY();

			deathAnim = srcPod.getDeathAnim();

			if ( srcPod.getExtendedInfo( ExtendedDronePodInfo.class ) != null ) {
				extendedInfo = srcPod.getExtendedInfo( ExtendedDronePodInfo.class ).copy();
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		public void commandeer() {
			setMourningTicks( 0 );
			setCurrentSpace( 0 );
			setDestinationSpace( -1 );

			//setNextTargetX( Integer.MIN_VALUE )?
			//setNextTargetY( Integer.MIN_VALUE )?

			setBuildupTicks( -1000 );
			setStationaryTicks( 0 );

			setOverloadTicks( 0 );

			// TODO: Unknowns.

			getDeathAnim().setPlaying( false );
			getDeathAnim().setCurrentFrame( 0 );
			getDeathAnim().setProgressTicks( 0 );

			if ( getExtendedInfo( ExtendedDronePodInfo.class ) != null ) {
				getExtendedInfo( ExtendedDronePodInfo.class ).commandeer();
			}
		}


		public void setDroneType( DroneType droneType ) { this.droneType = droneType; }
		public DroneType getDroneType() { return droneType; }

		/**
		 * Sets time elapsed while this drone is un-redeployable after
		 * destruction.
		 *
		 * This value begins decrementing from 10000 when the drone pod is
		 * destroyed and the deathAnim completes. After reaching or passing 0,
		 * this value lingers.
		 */
		public void setMourningTicks( int n ) { mourningTicks = n; }
		public int getMourningTicks() { return mourningTicks; }

		/**
		 * Sets which ship to use as the origin for position coordinates.
		 *
		 * @param n player ship (0) or nearby ship (1)
		 * @see #setDestinationSpace(int)
		 */
		public void setCurrentSpace( int n ) { currentSpace = n; }
		public int getCurrentSpace() { return currentSpace; }

		/**
		 * Sets which ship to eventually use as the origin for position
		 * coordinates.
		 *
		 * This value is initially -1. It is set to one of the ships when the
		 * drone pod is deployed. Then this value lingers.
		 *
		 * @param n player ship (0) or nearby ship (1) or none (-1)
		 * @see #setCurrentSpace(int)
		 */
		public void setDestinationSpace( int n ) { destinationSpace = n; }
		public int getDestinationSpace() { return destinationSpace; }

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


		/**
		 * Unknown.
		 *
		 * Observed values: Always MIN_INT?
		 */
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		/**
		 * Unknown.
		 *
		 * Observed values: Always MIN_INT?
		 */
		public void setUnknownZeta( int n ) { unknownZeta = n; }
		public int getUnknownZeta() { return unknownZeta; }

		/**
		 * Sets the position where this drone's next projectile will end up.
		 *
		 * This is analogous to weapon targeting reticles deciding the next
		 * target. The drone may spin or maneuver in transit, but it will
		 * ultimately turn to face this point before firing.
		 *
		 * Combat/Ship_Repair drones set a new value after each shot. Defense
		 * drones intermittently set it (e.g., directly at an opposing drone)
		 * and unset it.
		 *
		 * When not set, this is MIN_INT.
		 *
		 * @param n a pseudo-float
		 * @see WeaponModuleState#setCurrentTargets(List)
		 */
		public void setNextTargetX( int n ) { nextTargetX = n; }
		public void setNextTargetY( int n ) { nextTargetY = n; }
		public int getNextTargetX() { return nextTargetX; }
		public int getNextTargetY() { return nextTargetY; }

		/**
		 * Unknown.
		 *
		 * When not set, this is MIN_INT.
		 *
		 * Observed values: Defense (erratic +/- 0-20000); 962, 144, -988.
		 */
		public void setUnknownIota( int n ) { unknownIota = n; }
		public int getUnknownIota() { return unknownIota; }

		/**
		 * Unknown.
		 *
		 * When not set, this is MIN_INT.
		 *
		 * Observed values: Defense (erratic +/- 0-20000); -2384, 26, 2373.
		 */
		public void setUnknownKappa( int n ) { unknownKappa = n; }
		public int getUnknownKappa() { return unknownKappa; }


		/**
		 * Sets time elapsed while this drone is about to fire.
		 *
		 * Drones telegraph when they're about to fire, a light will change
		 * color (COMBAT_1) or glow intensely (COMBAT_BEAM). While positive,
		 * this value decrements to 0. At that point, this is set to -1000,
		 * firing occurs and a projectile is launched.
		 *
		 * Observed values: 365, 43 (COMBAT_BEAM); 500 (when a launched Hacking
		 * drone entered the target ship space).
		 *
		 * When not set, this is -1000.
		 */
		public void setBuildupTicks( int n ) { buildupTicks = n; }
		public int getBuildupTicks() { return buildupTicks; }

		/**
		 * Sets time elapsed while this drone is stationary.
		 *
		 * While positive, this value decrements to 0. The drone will be
		 * completely still for the duration. Beam drones set this when
		 * buildupTicks reaches 0, so the drone will hold still to be the
		 * beam's origin point.
		 *
		 * This works on Combat drones, too, if edited.
		 *
		 * Observed values: 470, 322, 167 (COMBAT_BEAM).
		 *
		 * When not set, this is 0.
		 */
		public void setStationaryTicks( int n ) { stationaryTicks = n; }
		public int getStationaryTicks() { return stationaryTicks; }

		/**
		 * Sets time elapsed while this drone is unable to shoot again after
		 * firing.
		 *
		 * This is based on the 'cooldown' tag of the DroneBlueprint's xml.
		 *
		 * While positive, for Defense and Shield drones, this value decrements
		 * to 0. The drone will be passive (e.g., not firing) for the duration.
		 * After reaching or passing 0, this is set to -1000.
		 *
		 * A Defense drone's light will turn red while passive (as opposed to
		 * green).
		 *
		 * Combat and Beam drones leave this at the xml's value without ever
		 * decrementing.
		 *
		 * When not set, this is 0.
		 *
		 * TODO: Check Hacking and Ship_Repair drones.
		 */
		public void setCooldownTicks( int n ) { cooldownTicks = n; }
		public int getCooldownTicks() { return cooldownTicks; }

		/**
		 * Sets the drone's orbital progress around the shield ellipse.
		 *
		 * Drones which do not orbit will have some lingering value instead.
		 *
		 * TODO: Modify this value in the editor. In CheatEngine, changing
		 * this has no effect, appearing to be read-only field for reference.
		 *
		 * @param n a pseudo-float (n degrees clockwise from east)
		 */
		public void setOrbitAngle( int n ) { orbitAngle = n; }
		public int getOrbitAngle() { return orbitAngle; }

		/**
		 * Sets the drone's turret angle.
		 *
		 * When not set, this is 0.
		 *
		 * @param n a pseudo-float (n degrees clockwise from east)
		 */
		public void setTurretAngle( int n ) { turretAngle = n; }
		public int getTurretAngle() { return turretAngle; }


		/**
		 * Unknown.
		 *
		 * Might be facing, to rotate the entire drone?
		 *
		 * Observed values: Hacking (U:-89 L:179 R:8.745 D:89); Combat drones
		 * have strange values. Boarder (Ion Drone) body in flight is rotated
		 * as expected (Eastward:0 SW:121), and turret its value is synched.
		 *
		 * When not set, this is 0.
		 *
		 * @param n a pseudo-float (n degrees clockwise from east)
		 */
		public void setUnknownXi( int n ) { unknownXi = n; }
		public int getUnknownXi() { return unknownXi; }

		/**
		 * Sets the number of waypoints this drone should arrive at before
		 * disappearing.
		 *
		 * This value decrements the moment this drone finishes idling at one
		 * waypoint and begins moving toward the next. After reaching 0, the
		 * drone vanishes. Then this value lingers.
		 *
		 * When not set, this is MAX_INT.
		 *
		 * Observed values: 4, 3, 2, 1, 0 (Ship_Repair).
		 */
		public void setHopsToLive( int n ) { hopsToLive = n; }
		public int getHopsToLive() { return hopsToLive; }

		public void setUnknownPi( int n ) { unknownPi = n; }
		public int getUnknownPi() { return unknownPi; }

		/**
		 * Unknown.
		 *
		 * Observed values: 1, 0.
		 */
		public void setUnknownRho( int n ) { unknownRho = n; }
		public int getUnknownRho() { return unknownRho; }

		/**
		 * Sets time elapsed while this drone is stunned, with a chance of
		 * exploding.
		 *
		 * This value begins decrementing from a positive integer after taking
		 * ion damage (e.g., from an Anti-Combat Drone). After reaching 0, the
		 * drone returns to normal.
		 *
		 * While stunned, the drone will halt movement, it'll be covered in
		 * arcs of electricity, and the turret will spin rapidly. It may
		 * explode at a random moment prior to reaching 0 - at which point,
		 * this value will be set to 0.
		 *
		 * When not set, this is 0. This value lingers and may even end up a
		 * little negative.
		 *
		 * Observed values: 4378 (Combat drone shot by Anti-Combat Drone)
		 *
		 * TODO: It's unclear what determines if/when an explosion occurs.
		 */
		public void setOverloadTicks( int n ) { overloadTicks = n; }
		public int getOverloadTicks() { return overloadTicks; }

		/**
		 * Unknown.
		 *
		 * Observed values: -5704; -173834; 110067, 230637.
		 *
		 * When not set, this is -1000.
		 */
		public void setUnknownTau( int n ) { unknownTau = n; }
		public int getUnknownTau() { return unknownTau; }

		/**
		 * Unknown.
		 *
		 * Observed values: 1.
		 */
		public void setUnknownUpsilon( int n ) { unknownUpsilon = n; }
		public int getUnknownUpsilon() { return unknownUpsilon; }

		/**
		 * Sets the recent change in position (Current - Previous + 1).
		 *
		 * TODO: Modify this value in the editor. In CheatEngine, changing
		 * this has no effect, appearing to be read-only field for reference.
		 *
		 * @param n a pseudo-float
		 *
		 * @see #setCurrentPositionX(int)
		 * @see #setPreviousPositionX(int)
		 */
		public void setDeltaPositionX( int n ) { deltaPosX = n; }
		public void setDeltaPositionY( int n ) { deltaPosY = n; }
		public int getDeltaPositionX() { return deltaPosX; }
		public int getDeltaPositionY() { return deltaPosY; }


		public void setDeathAnim( AnimState anim ) { deathAnim = anim; }
		public AnimState getDeathAnim() { return deathAnim; }

		public void setExtendedInfo( ExtendedDronePodInfo info ) {
			extendedInfo = info;
		}
		public <T extends ExtendedDronePodInfo> T getExtendedInfo( Class<T> infoClass ) {
			if ( extendedInfo == null ) return null;
			return infoClass.cast( extendedInfo );
		}


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format( "%d", n );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			boolean first = true;

			result.append( String.format( "Drone Type:        %s\n", droneType.getId() ) );
			result.append( String.format( "Mourning Ticks:    %7d (Decrements to 0 from 10000)\n", mourningTicks ) );
			result.append( String.format( "Current Space:     %7d\n", currentSpace ) );
			result.append( String.format( "Destination Space: %7d\n", destinationSpace ) );
			result.append( String.format( "Current Position:  %7s,%7s\n", prettyInt( currentPosX), prettyInt( currentPosY ) ) );
			result.append( String.format( "Previous Position: %7s,%7s\n", prettyInt( prevPosX ), prettyInt( prevPosY ) ) );
			result.append( String.format( "Goal Position:     %7s,%7s\n", prettyInt( goalPosX ), prettyInt( goalPosY ) ) );

			result.append( String.format( "\n" ));
			result.append( String.format( "Epsilon?, Zeta?:   %7s,%7s\n", prettyInt( unknownEpsilon ), prettyInt( unknownZeta ) ) );
			result.append( String.format( "Next Target:       %7s,%7s\n", prettyInt( nextTargetX ), prettyInt( nextTargetY ) ) );
			result.append( String.format( "Iota?, Kappa?:     %7s,%7s\n", prettyInt( unknownIota ), prettyInt( unknownKappa ) ) );

			result.append( String.format( "\n" ));
			result.append( String.format( "Buildup Ticks:     %7d (Decrements to 0 while about to fire)\n", buildupTicks ) );
			result.append( String.format( "Stationary Ticks:  %7d (Decrements to 0 while stationary)\n", stationaryTicks ) );
			result.append( String.format( "Cooldown Ticks:    %7d (Decrements to 0 while passive, Defense/Shield only)\n", cooldownTicks ) );
			result.append( String.format( "Orbit Angle:       %7d\n", orbitAngle ) );
			result.append( String.format( "Turret Angle:      %7d\n", turretAngle ) );
			result.append( String.format( "Xi?:               %7d\n", unknownXi ) );
			result.append( String.format( "Hops to Live:      %7s (Waypoints to idle at before undeploying)\n", prettyInt( hopsToLive ) ) );
			result.append( String.format( "Pi?:               %7d\n", unknownPi ) );
			result.append( String.format( "Rho?:              %7d\n", unknownRho ) );
			result.append( String.format( "Overload Ticks:    %7d (Decrements to 0 while shocked by ion weapons)\n", overloadTicks ) );
			result.append( String.format( "Tau?:              %7d\n", unknownTau ) );
			result.append( String.format( "Upsilon?:          %7d\n", unknownUpsilon ) );
			result.append( String.format( "Delta Position:    %7d,%7d (Current - Previous + 1)\n", deltaPosX, deltaPosY ) );

			result.append( "\nDeath Anim...\n" );
			if ( deathAnim != null) {
				result.append( deathAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\nExtended Drone Pod Info... (Varies by Drone Type)\n" );
			if ( extendedInfo != null) {
				result.append( extendedInfo.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	public static abstract class ExtendedDronePodInfo {

		protected ExtendedDronePodInfo() {
		}

		protected ExtendedDronePodInfo( ExtendedDronePodInfo srcInfo ) {
		}

		/**
		 * Blindly copy-constructs objects.
		 *
		 * Subclasses override this with return values of their own type.
		 */
		public abstract ExtendedDronePodInfo copy();


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public abstract void commandeer();
	}

	public static class EmptyDronePodInfo extends ExtendedDronePodInfo {

		/**
		 * Constructor.
		 */
		public EmptyDronePodInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected EmptyDronePodInfo( EmptyDronePodInfo srcInfo ) {
			super( srcInfo );
		}

		@Override
		public EmptyDronePodInfo copy() { return new EmptyDronePodInfo( this ); }


		@Override
		public void commandeer() {
		}


		@Override
		public String toString() {
			return "N/A\n";
		}
	}

	/**
	 * Generic drone pod info consisting of an int array.
	 *
	 * No longer used, this had allowed read/write of various drones' unknown
	 * structures each with different lengths.
	 */
	public static class IntegerDronePodInfo extends ExtendedDronePodInfo {
		private int[] unknownAlpha;

		/**
		 * Constructs an incomplete IntegerDronePodInfo.
		 *
		 * A number of integers equal to size will need to be set.
		 */
		public IntegerDronePodInfo( int size ) {
			super();
			unknownAlpha = new int[size];
		}

		/**
		 * Copy constructor.
		 */
		protected IntegerDronePodInfo( IntegerDronePodInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = new int[srcInfo.getSize()];
			for ( int i=0; i < unknownAlpha.length; i++ ) {
				unknownAlpha[i] = srcInfo.get( i );
			}
		}

		@Override
		public IntegerDronePodInfo copy() { return new IntegerDronePodInfo( this ); }


		@Override
		public void commandeer() {
		}


		public int getSize() { return unknownAlpha.length; }

		public void set( int index, int n ) { unknownAlpha[index] = n; }
		public int get( int index ) { return unknownAlpha[index]; }


		private String prettyInt( int n ) {
			if ( n == Integer.MIN_VALUE ) return "MIN";
			if ( n == Integer.MAX_VALUE ) return "MAX";

			return String.format( "%d", n );
		}


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Alpha?...\n" ) );
			for ( int i=0; i < unknownAlpha.length; i++ ) {
				result.append( String.format( "%7s", prettyInt( unknownAlpha[i] ) ) );

				if ( i != unknownAlpha.length-1 ) {
					if ( i % 2 == 1 ) {
						result.append( ",\n" );
					} else {
						result.append( ", " );
					}
				}
			}
			result.append( "\n" );

			return result.toString();
		}
	}

	/**
	 * Extended Combat/Beam/Ship_Repair drone info.
	 *
	 * These drones flit to a random (?) point, stop, then move to another,
	 * and so on.
	 */
	public static class ZigZagDronePodInfo extends ExtendedDronePodInfo {
		private int lastWaypointX = 0;
		private int lastWaypointY = 0;
		private int transitTicks = 0;
		private int exhaustAngle = 0;
		private int unknownEpsilon = 0;

		/**
		 * Constructor.
		 */
		public ZigZagDronePodInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected ZigZagDronePodInfo( ZigZagDronePodInfo srcInfo ) {
			super( srcInfo );
			lastWaypointX = srcInfo.getLastWaypointX();
			lastWaypointY = srcInfo.getLastWaypointY();
			transitTicks = srcInfo.getTransitTicks();
			exhaustAngle = srcInfo.getExhaustAngle();
			unknownEpsilon = srcInfo.getUnknownEpsilon();
		}

		@Override
		public ZigZagDronePodInfo copy() { return new ZigZagDronePodInfo( this ); }


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 */
		@Override
		public void commandeer() {
		}


		/**
		 * Sets the cached position from when the drone last stopped.
		 *
		 * TODO: Modify this value in the editor. In CheatEngine, changing
		 * this has no effect, appearing to be read-only field for reference.
		 *
		 * @param n a pseudo-float
		 */
		public void setLastWaypointX( int n ) { lastWaypointX = n; }
		public void setLastWaypointY( int n ) { lastWaypointY = n; }
		public int getLastWaypointX() { return lastWaypointX; }
		public int getLastWaypointY() { return lastWaypointY; }

		/**
		 * Sets time elapsed while this drone moves.
		 *
		 * This increments from 0 to 1000 as the drone drifts toward a new
		 * waypoint. While this value is below 200, exhaust flames are
		 * visible. Then they vanish. The moment the drone pauses at the
		 * destination, this is set to 1000.
		 *
		 * When not set, this is MIN_INT. This happens when stationary while
		 * stunned.
		 *
		 * Observed values: 153 (stunned drift begins), 153000 (mid drift),
		 * 153000000 (near end of drift).
		 *
		 * TODO: Modify this value in the editor. In CheatEngine, changing
		 * this has no effect, appearing to be read-only field for reference.
		 *
		 * @see #setExhaustAngle(int)
		 */
		public void setTransitTicks( int n ) { transitTicks = n; }
		public int getTransitTicks() { return transitTicks; }

		/**
		 * Sets the angle to display exhaust flames thrusting toward.
		 *
		 * When not set, this is MIN_INT.
		 *
		 * TODO: Modify this value in the editor. In CheatEngine, changing
		 * this DOES work.
		 *
		 * @param n a pseudo-float (n degrees clockwise from east)
		 *
		 * @see #setTransitTicks(int)
		 */
		public void setExhaustAngle( int n ) { exhaustAngle = n; }
		public int getExhaustAngle() { return exhaustAngle; }

		/**
		 * Unknown.
		 *
		 * When not set, this is MIN_INT.
		 */
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Last Waypoint:      %7d,%7d\n", lastWaypointX, lastWaypointY ) );
			result.append( String.format( "TransitTicks:       %7s\n", (transitTicks == Integer.MIN_VALUE ? "N/A" : transitTicks) ) );
			result.append( String.format( "Exhaust Angle:      %7s\n", (exhaustAngle == Integer.MIN_VALUE ? "N/A" : exhaustAngle) ) );
			result.append( String.format( "Epsilon?:           %7s\n", (unknownEpsilon == Integer.MIN_VALUE ? "N/A" : unknownEpsilon) ) );

			return result.toString();
		}
	}

	/**
	 * Extended boarder drone info.
	 *
	 * Boarder drones exclusively store body info in ExtendedDronePodInfo.
	 * The traditional DroneState's body fields remain at inoperative defaults.
	 *
	 * In FTL 1.01-1.03.3, Boarder drone bodies were actual crew on foreign
	 * ships.
	 *
	 * @see DroneState
	 */
	public static class BoarderDronePodInfo extends ExtendedDronePodInfo {
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private int bodyHealth = 1;
		private int bodyX = -1, bodyY = -1;
		private int bodyRoomId = -1;
		private int bodyRoomSquare = -1;

		/**
		 * Constructor.
		 */
		public BoarderDronePodInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected BoarderDronePodInfo( BoarderDronePodInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
			unknownBeta = srcInfo.getUnknownBeta();
			unknownGamma = srcInfo.getUnknownGamma();
			unknownDelta = srcInfo.getUnknownDelta();
			bodyHealth = srcInfo.getBodyHealth();
			bodyX = srcInfo.getBodyX();
			bodyY = srcInfo.getBodyY();
			bodyRoomId = srcInfo.getBodyRoomId();
			bodyRoomSquare = srcInfo.getBodyRoomSquare();
		}

		@Override
		public BoarderDronePodInfo copy() { return new BoarderDronePodInfo( this ); }


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		@Override
		public void commandeer() {
			// TODO
		}


		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public int getUnknownBeta() { return unknownBeta; }

		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		public void setBodyHealth( int n ) { bodyHealth = n; }
		public int getBodyHealth() { return bodyHealth; }

		/**
		 * Sets the position of the drone's body image.
		 *
		 * Technically the roomId/square fields set the goal location.
		 * This field is where the body really is, possibly en route.
		 *
		 * It's the position of the body image's center, relative to the
		 * top-left corner of the floor layout of the ship it's on.
		 *
		 * This value lingers, even after the body is gone.
		 */
		public void setBodyX( int n ) { bodyX = n; }
		public void setBodyY( int n ) { bodyY = n; }
		public int getBodyX() { return bodyX; }
		public int getBodyY() { return bodyY; }

		/**
		 * Sets the room this drone's body is in (or at least trying to move
		 * toward).
		 *
		 * When no body is present, this is -1.
		 *
		 * roomId and roomSquare need to be specified together.
		 */
		public void setBodyRoomId( int n ) { bodyRoomId = n; }
		public void setBodyRoomSquare( int n ) { bodyRoomSquare = n; }
		public int getBodyRoomId() { return bodyRoomId; }
		public int getBodyRoomSquare() { return bodyRoomSquare; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Alpha?:             %7d\n", unknownAlpha ) );
			result.append( String.format( "Beta?:              %7d\n", unknownBeta ) );
			result.append( String.format( "Gamma?:             %7d\n", unknownGamma ) );
			result.append( String.format( "Delta?:             %7d\n", unknownDelta ) );
			result.append( String.format( "Body Health:        %7d\n", bodyHealth ) );
			result.append( String.format( "Body Position:      %7d,%7d\n", bodyX, bodyY ) );
			result.append( String.format( "Body Room Id:       %7d\n", bodyRoomId ) );
			result.append( String.format( "Body Room Square:   %7d\n", bodyRoomSquare ) );

			return result.toString();
		}
	}

	public static class ShieldDronePodInfo extends ExtendedDronePodInfo {
		private int unknownAlpha = -1000;

		/**
		 * Constructor.
		 */
		public ShieldDronePodInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected ShieldDronePodInfo( ShieldDronePodInfo srcInfo ) {
			super( srcInfo );
			unknownAlpha = srcInfo.getUnknownAlpha();
		}

		@Override
		public ShieldDronePodInfo copy() { return new ShieldDronePodInfo( this ); }


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		@Override
		public void commandeer() {
			setUnknownAlpha( -1000 );
		}


		/**
		 * Unknown.
		 *
		 * Zoltan shield recharge ticks?
		 *
		 * Observed values: -1000 (inactive)
		 */
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public int getUnknownAlpha() { return unknownAlpha; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Alpha?:             %7d\n", unknownAlpha ) );

			return result.toString();
		}
	}

	public static class HackingDronePodInfo extends ExtendedDronePodInfo {
		private int attachPositionX = 0;
		private int attachPositionY = 0;
		private int unknownGamma = 0;
		private int unknownDelta = 0;
		private AnimState landingAnim = new AnimState();
		private AnimState extensionAnim = new AnimState();

		/**
		 * Constructor.
		 */
		public HackingDronePodInfo() {
			super();
		}

		/**
		 * Copy constructor.
		 */
		protected HackingDronePodInfo( HackingDronePodInfo srcInfo ) {
			super( srcInfo );
			attachPositionX = srcInfo.getAttachPositionX();
			attachPositionY = srcInfo.getAttachPositionY();
			unknownGamma = srcInfo.getUnknownGamma();
			unknownDelta = srcInfo.getUnknownDelta();
			landingAnim = new AnimState( srcInfo.getLandingAnim() );
			extensionAnim = new AnimState( srcInfo.getExtensionAnim() );
		}

		@Override
		public HackingDronePodInfo copy() { return new HackingDronePodInfo( this ); }


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		@Override
		public void commandeer() {
			setAttachPositionX( 0 );
			setAttachPositionY( 0 );
			setUnknownGamma( 0 );
			setUnknownDelta( 0 );

			getLandingAnim().setPlaying( false );
			getLandingAnim().setCurrentFrame( 0 );
			getLandingAnim().setProgressTicks( 0 );

			getExtensionAnim().setPlaying( false );
			getExtensionAnim().setCurrentFrame( 0 );
			getExtensionAnim().setProgressTicks( 0 );
		}

		//Alpha and beta might be xy of the center of the claw on the wall.

		/**
		 * Sets the position of this drone pod's attachment to a ship wall.
		 *
		 * This is the center of the claw-side edge of the drone sprite, at the
		 * senter of the wall of the room it attached to. This point might not
		 * be ON the wall, possibly be a few pixels outside or inside.
		 *
		 * This is set when the done pod makes contact with the wall.
		 *
		 * When not set, this is (0, 0).
		 */
		public void setAttachPositionX( int n ) { attachPositionX = n; }
		public void setAttachPositionY( int n ) { attachPositionY = n; }
		public int getAttachPositionX() { return attachPositionX; }
		public int getAttachPositionY() { return attachPositionY; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (in flight), 1 (on contact).
		 */
		public void setUnknownGamma( int n ) { unknownGamma = n; }
		public int getUnknownGamma() { return unknownGamma; }

		/**
		 * Unknown.
		 *
		 * Observed values: 0 (in flight), 1 (after extension anim completes and
		 * purple lights turn on). Stayed at 1 after the hacking system was
		 * damaged and inoperative while the pod was still attached (purple
		 * lights turned off).
		 */
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public int getUnknownDelta() { return unknownDelta; }

		/**
		 * Sets the attachment/grappling anim.
		 *
		 * This begins playing on contact.
		 */
		public void setLandingAnim( AnimState anim ) { landingAnim = anim; }
		public AnimState getLandingAnim() { return landingAnim; }

		/**
		 * Sets the antenna extension anim.
		 *
		 * This begins playing after the landing anim completes.
		 */
		public void setExtensionAnim( AnimState anim ) { extensionAnim = anim; }
		public AnimState getExtensionAnim() { return extensionAnim; }

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Attach Position: %7s,%7s\n", attachPositionX, attachPositionY ) );
			result.append( String.format( "Gamma?:              %7d\n", unknownGamma ) );
			result.append( String.format( "Delta?:              %7d\n", unknownDelta ) );

			result.append( String.format( "\nLanding Anim?...\n" ) );
			if ( landingAnim != null ) {
				result.append( landingAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( String.format( "\nExtension Anim?...\n" ) );
			if ( extensionAnim != null ) {
				result.append( extensionAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	/**
	 * Extra drone info stored separately from the original DroneState.
	 *
	 * This was introduced in FTL 1.5.4.
	 */
	public static class ExtendedDroneInfo {
		private boolean deployed = false;
		private boolean armed = false;
		private DronePodState dronePod = null;


		/**
		 * Constructs an incomplete ExtendedDroneInfo.
		 *
		 * It will need a DronePodState.
		 */
		public ExtendedDroneInfo() {
		}

		/**
		 * Copy-constructor.
		 *
		 * The drone pod will be copy-constructed as well.
		 */
		public ExtendedDroneInfo( ExtendedDroneInfo srcInfo ) {
			deployed = srcInfo.isDeployed();
			armed = srcInfo.isArmed();

			if ( srcInfo.getDronePod() != null ) {
				dronePod = new DronePodState( srcInfo.getDronePod() );
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public void commandeer() {
			setDeployed( false );
			setArmed( false );

			if ( getDronePod() != null ) {
				getDronePod().commandeer();
			}
		}

		/**
		 * Sets whether the drone's body/pod exists.
		 *
		 * Re-arming an already deployed drone doesn't cost a drone part.
		 *
		 * After defeating a nearby ship, and the window disappears, player
		 * drone pods there are lost and this is set to false.
		 */
		public void setDeployed( boolean b ) { deployed = b; }
		public boolean isDeployed() { return deployed; }

		/**
		 * Sets whether this drone is powered.
		 *
		 * TODO: See what happens when this conflists with the DroneState.
		 *
		 * @see DroneState#setArmed(boolean)
		 */
		public void setArmed( boolean b ) { armed = b; }
		public boolean isArmed() { return armed; }

		/**
		 * Sets a drone pod, which varies by DroneType.
		 *
		 * For BATTLE and REPAIR, this should be null.
		 */
		public void setDronePod( DronePodState pod ) { dronePod = pod; }
		public DronePodState getDronePod() { return dronePod; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "Deployed:        %5b\n", deployed ) );
			result.append( String.format( "Armed:           %5b\n", armed ) );

			result.append( "\nDrone Pod...\n" );
			if ( dronePod != null ) {
				result.append( dronePod.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			} else {
				result.append( "N/A\n" );
			}

			return result.toString();
		}
	}



	public static class StandaloneDroneState {
		private String droneId = null;
		private DronePodState dronePod = null;
		private int unknownAlpha = 0;
		private int unknownBeta = 0;
		private int unknownGamma = 0;


		/**
		 * Constructs an incomplete StandaloneDroneState.
		 */
		public StandaloneDroneState() {
		}

		public void setDroneId( String s ) { droneId = s; }
		public String getDroneId() { return droneId; }

		public void setDronePod( DronePodState pod ) { dronePod = pod; }
		public DronePodState getDronePod() { return dronePod; }

		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }
		public void setUnknownGamma( int n ) { unknownGamma = n; }

		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }
		public int getUnknownGamma() { return unknownGamma; }


		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();

			result.append( String.format( "DroneId:           %s\n", droneId ) );

			result.append( "\nDrone Pod...\n" );
			result.append( dronePod.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );

			result.append( "\n" );

			result.append( String.format( "Alpha?:            %3d\n", unknownAlpha ) );
			result.append( String.format( "Beta?:             %3d\n", unknownBeta ) );
			result.append( String.format( "Gamma?:            %3d\n", unknownGamma ) );

			return result.toString();
		}
	}



	public static class WeaponModuleState {
		private int cooldownTicks = 0;
		private int cooldownTicksGoal = 0;
		private int subcooldownTicks = 0;
		private int subcooldownTicksGoal = 0;
		private int boost = 0;
		private int charge = 0;

		private List<XYPair> currentTargets = new ArrayList<XYPair>();
		private List<XYPair> prevTargets = new ArrayList<XYPair>();

		private boolean autofire = false;
		private boolean fireWhenReady = false;
		private int targetId = -1;
		private AnimState weaponAnim = new AnimState();
		private int protractAnimTicks = 0;
		private boolean firing = false;
		private boolean unknownPhi = false;
		private int animCharge = -1;
		private AnimState chargeAnim = new AnimState();
		private int lastProjectileId = -1;

		private List<ProjectileState> pendingProjectiles = new ArrayList<ProjectileState>();


		/**
		 * Constructor.
		 */
		public WeaponModuleState() {
		}

		/**
		 * Copy constructor.
		 *
		 * Each target and projectile will be copy-constructed as well.
		 */
		public WeaponModuleState( WeaponModuleState srcMod ) {
			cooldownTicks = srcMod.getCooldownTicks();
			cooldownTicksGoal = srcMod.getCooldownTicksGoal();
			subcooldownTicks = srcMod.getSubcooldownTicks();
			subcooldownTicksGoal = srcMod.getSubcooldownTicksGoal();
			boost = srcMod.getBoost();
			charge = srcMod.getCharge();

			for ( XYPair target : srcMod.getCurrentTargets() ) {
				currentTargets.add( new XYPair( target ) );
			}
			for ( XYPair target : srcMod.getPreviousTargets() ) {
				prevTargets.add( new XYPair( target ) );
			}

			autofire = srcMod.getAutofire();
			fireWhenReady = srcMod.getFireWhenReady();
			targetId = srcMod.getTargetId();
			weaponAnim = srcMod.getWeaponAnim();
			protractAnimTicks = srcMod.getProtractAnimTicks();
			firing = srcMod.isFiring();
			unknownPhi = srcMod.getUnknownPhi();
			animCharge = srcMod.getAnimCharge();
			chargeAnim = srcMod.getChargeAnim();
			lastProjectileId = srcMod.getLastProjectileId();

			for ( ProjectileState projectile : srcMod.getPendingProjectiles() ) {
				pendingProjectiles.add( new ProjectileState( projectile ) );
			}
		}


		/**
		 * Resets aspects of an existing object to be viable for player use.
		 *
		 * This will be called by the ship object when it is commandeered.
		 *
		 * Warning: Dangerous while values remain undeciphered.
		 * TODO: Recurse into all nested objects.
		 */
		public void commandeer() {
			setCooldownTicks( 0 );
			setCooldownTicksGoal( 0 );     // TODO: Vet this default.
			setSubcooldownTicks( 0 );      // TODO: Vet this default.
			setSubcooldownTicksGoal( 0 );  // TODO: Vet this default.
			setBoost( 0 );
			setCharge( 0 );

			getCurrentTargets().clear();
			getPreviousTargets().clear();

			setAutofire( false );
			setFireWhenReady( false );
			setTargetId( -1 );

			getWeaponAnim().setPlaying( false );
			getWeaponAnim().setCurrentFrame( 0 );
			getWeaponAnim().setProgressTicks( 0 );

			setProtractAnimTicks( 0 );
			setFiring( false );

			setAnimCharge( -1 );
			setUnknownPhi( false );

			getChargeAnim().setPlaying( false );
			getChargeAnim().setCurrentFrame( 0 );
			getChargeAnim().setProgressTicks( 0 );

			setLastProjectileId( -1 );

			getPendingProjectiles().clear();
		}


		/**
		 * Sets time elapsed waiting for the weapon to cool down.
		 *
		 * @param n a positive int less than, or equal to, the goal (0 when not armed)
		 *
		 * @see #setCooldownTicksGoal(int)
		 * @see WeaponState#setCooldownTicks(int)
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

		public void setSubcooldownTicks( int n ) { subcooldownTicks = n; }
		public int getSubcooldownTicks() { return subcooldownTicks; }

		public void setSubcooldownTicksGoal( int n ) { subcooldownTicksGoal = n; }
		public int getSubcooldownTicksGoal() { return subcooldownTicksGoal; }

		/**
		 * Sets the boost level on a weapon whose cooldown decreases with
		 * consecutive shots.
		 *
		 * This is represented in-game on the HUD as "Name +X".
		 * Example: LASER_CHAINGUN.
		 *
		 * @param number of consecutive shots, up to the blueprint's boost count limit, or 0
		 */
		public void setBoost( int n ) { boost = n; }
		public int getBoost() { return boost; }

		/**
		 * Sets the number of charges on a charge weapon, or 0.
		 *
		 * Charges increment when a weapon's cooldown is allowed to elapse
		 * additional times without firing... up to a maximum count.
		 *
		 * Immediately before firing, this value is cached in another field for
		 * animation purposes, and this resets to 0.
		 *
		 * This is represented in-game on the HUD as circles with dots.
		 * Example: ION_CHARGEGUN.
		 *
		 * Note: Modded WeaponBlueprints with "chargeLevels" greater than 7
		 * crash FTL 1.5.4+.
		 *
		 * Note: Modded WeaponBlueprints with both the beam "type" and
		 * "chargeLevels" crash FTL.
		 *
		 * @see #setChargeAnim(AnimState)
		 */
		public void setCharge( int n ) { charge = n; }
		public int getCharge() { return charge; }

		/**
		 * Sets currently targeted locations.
		 *
		 * This is a list of coordinates relative to the top-left corner of the
		 * enemy ship's floor layout. There will be a coordinate for every
		 * projectile the weapon will fire, or pairs of start/end coords for a
		 * beam weapon's line.
		 *
		 * The moment the player places a target reticle, this is populated.
		 *
		 * Immediately before firing, these coordinates are copied to become the
		 * previous targets, and if autofire is off this list is cleared.
		 *
		 * Note: These are NOT pseudo-floats.
		 *
		 * TODO: Confirm autofire behavior.
		 *
		 * @see #setPreviousTargets(List)
		 */
		public void setCurrentTargets( List<XYPair> targetList ) { currentTargets = targetList; }
		public List<XYPair> getCurrentTargets() { return currentTargets; }

		/**
		 * Sets previously targeted locations.
		 *
		 * The moment the player places a target reticle, this is populated.
		 *
		 * Note: These are NOT pseudo-floats.
		 *
		 * @see #setCurrentTargets(List)
		 */
		public void setPreviousTargets( List<XYPair> targetList ) { prevTargets = targetList; }
		public List<XYPair> getPreviousTargets() { return prevTargets; }

		/**
		 * Toggles whether fireWhenReady will be disabled after any volley.
		 *
		 * TODO: Determine what other fields must be set along with this.
		 *
		 * @see #setFireWhenReady(boolean)
		 */
		public void setAutofire( boolean b ) { autofire = b; }
		public boolean getAutofire() { return autofire; }

		/**
		 * Toggles whether this weapon will fire its next volley once the
		 * cooldown expires.
		 *
		 * The moment the player places a target reticle, this is set to true.
		 *
		 * Immediately before firing, if autofire is off, this is set to false.
		 *
		 * TODO: Determine what other fields must be set along with this.
		 *
		 * @see #setAutofire(boolean)
		 */
		public void setFireWhenReady( boolean b ) { fireWhenReady = b; }
		public boolean getFireWhenReady() { return fireWhenReady; }

		/**
		 * Unknown.
		 *
		 * When not set, this is -1.
		 *
		 * The moment the player places a target reticle, this is set.
		 *
		 * @param n player ship (0) or nearby ship (1)
		 */
		public void setTargetId( int n ) { targetId = n; }
		public int getTargetId() { return targetId; }

		/**
		 * Sets the weapon anim state, depicting idle/cooldown/fire.
		 */
		public void setWeaponAnim( AnimState anim ) { weaponAnim = anim; }
		public AnimState getWeaponAnim() { return weaponAnim; }

		/**
		 * Sets time elapsed while this weapon slides out from the hull.
		 *
		 * This counts from 0 (retracted) to 1000 (protracted) and back.
		 * Upon pausing or saving, this will snap to whichever number it was
		 * approaching at the time.
		 *
		 * TODO: Determine what happens when edited to a value somewhere in
		 * between.
		 */
		public void setProtractAnimTicks( int n ) { protractAnimTicks = n; }
		public int getProtractAnimTicks() { return protractAnimTicks; }

		/**
		 * Toggles whether this weapon is currently firing.
		 */
		public void setFiring( boolean b ) { firing = b; }
		public boolean isFiring() { return firing; }

		/**
		 * Unknown.
		 *
		 * Matthew's hint: fireshot.
		 */
		public void setUnknownPhi( boolean b ) { unknownPhi = b; }
		public boolean getUnknownPhi() { return unknownPhi; }

		/**
		 * Sets the cached charge.
		 *
		 * Immediately before firing, FTL copies the main charge value minus 1
		 * and caches it here for reference while playing the chargeAnim. After
		 * firing, this is resynchronized to the main charge value minus 1.
		 *
		 * When not set, this is -1.
		 *
		 * TODO: Boost weapons set this too!?
		 *
		 * This was introduced in FTL 1.5.13.
		 *
		 * @see #setChargeAnim(AnimState)
		 */
		public void setAnimCharge( int n ) { animCharge = n; }
		public int getAnimCharge() { return animCharge; }

		/**
		 * Sets the charge anim state.
		 *
		 * This is overlaid onto the weaponAnim to add minor accents.
		 * It's current frame depends on the cached charge.
		 *
		 * In "dlcAnimations.xml", if the weaponAnim's name is "X", the
		 * boostAnim's name will be "X_charge". (Yes, really.)
		 *
		 * TODO: Sort out charge vs boost.
		 *
		 * This was introduced in FTL 1.5.13.
		 *
		 * @see #setAnimCharge(int)
		 */
		public void setChargeAnim( AnimState anim ) { chargeAnim = anim; }
		public AnimState getChargeAnim() { return chargeAnim; }

		/**
		 * Unknown.
		 *
		 * When not set, this is -1.
		 */
		public void setLastProjectileId( int n ) { lastProjectileId = n; }
		public int getLastProjectileId() { return lastProjectileId; }


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

			result.append( String.format( "Cooldown Ticks:          %7d\n", cooldownTicks ) );
			result.append( String.format( "Cooldown Goal:           %7d\n", cooldownTicksGoal ) );
			result.append( String.format( "Subcooldown Ticks?:      %7d\n", subcooldownTicks ) );
			result.append( String.format( "Subcooldown Ticks Goal?: %7d\n", subcooldownTicksGoal ) );
			result.append( String.format( "Boost:                   %7d\n", boost ) );
			result.append( String.format( "Charge:                  %7d\n", charge ) );

			result.append( "\nCurrent Targets?... (Reticle Coords)\n" );
			first = true;
			for ( XYPair target : currentTargets ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "  X,Y: %3d,%3d\n", target.x, target.y ) );
			}

			result.append( "\nPrevious Targets?... (Reticle Coords)\n" );
			first = true;
			for ( XYPair target : prevTargets ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "  X,Y: %3d,%3d\n", target.x, target.y ) );
			}

			result.append( "\n" );

			result.append( String.format( "Autofire:                %7b\n", autofire ) );
			result.append( String.format( "Fire When Ready?:        %7b\n", fireWhenReady ) );
			result.append( String.format( "Target Id?:              %7d\n", targetId ) );

			result.append( "\nWeapon Anim...\n" );
			if ( weaponAnim != null ) {
				result.append( weaponAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );

			result.append( String.format( "Protract Anim Ticks:     %7d (0=Retracted or 1000=Protracted)\n", protractAnimTicks ) );
			result.append( String.format( "Firing:                  %7b\n", firing ) );
			result.append( String.format( "Phi?:                    %7b\n", unknownPhi ) );
			result.append( String.format( "Anim Charge:             %7d (Caches charge while firing to use in chargeAnim)\n", animCharge ) );

			result.append( "\nCharge Anim?...\n" );
			if ( chargeAnim != null) {
				result.append( chargeAnim.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			result.append( "\n" );

			result.append( String.format( "Last Projectile Id?:     %7d\n", lastProjectileId ) );

			result.append( "\nPending Projectiles... (Queued before firing)\n" );
			int projectileIndex = 0;
			first = true;
			for ( ProjectileState projectile : pendingProjectiles ) {
				if ( first ) { first = false; }
				else { result.append( ",\n" ); }
				result.append( String.format( "Projectile # %2d:\n", projectileIndex++ ) );
				result.append( projectile.toString().replaceAll( "(^|\n)(.+)", "$1  $2" ) );
			}

			return result.toString();
		}
	}



	private int readMinMaxedInt( InputStream in ) throws IOException {
		int n = readInt( in );

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

	/**
	 * Reads additional fields of various ship-related classes.
	 *
	 * This method does not involve a dedicated class.
	 */
	private void readExtendedShipInfo( FileInputStream in, ShipState shipState, int fileFormat ) throws IOException {
		// There is no explicit list count for drones.
		for ( DroneState drone : shipState.getDroneList() ) {
			ExtendedDroneInfo droneInfo = new ExtendedDroneInfo();

			droneInfo.setDeployed( readBool( in ) );
			droneInfo.setArmed( readBool( in ) );

			String droneId = drone.getDroneId();
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneId );
			if ( droneBlueprint == null ) throw new IOException( "Unrecognized DroneBlueprint: "+ droneId );

			DroneType droneType = DroneType.findById( droneBlueprint.getType() );
			if ( droneType == null ) throw new IOException( String.format( "DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType() ) );

			if ( DroneType.REPAIR.equals( droneType ) ||
			     DroneType.BATTLE.equals( droneType ) ) {
				// No drone pod for these types.
			}
			else {
				DronePodState dronePod = readDronePod( in, droneType );
				droneInfo.setDronePod( dronePod );
			}

			drone.setExtendedDroneInfo( droneInfo );
		}

		SystemState hackingState = shipState.getSystem( SystemType.HACKING );
		if ( hackingState != null && hackingState.getCapacity() > 0 ) {
			HackingInfo hackingInfo = new HackingInfo();

			int targetSystemTypeFlag = readInt( in );
			SystemType targetSystemType;
			if ( targetSystemTypeFlag == -1 ) targetSystemType = null;
			else if ( targetSystemTypeFlag == 0 ) targetSystemType = SystemType.SHIELDS;
			else if ( targetSystemTypeFlag == 1 ) targetSystemType = SystemType.ENGINES;
			else if ( targetSystemTypeFlag == 2 ) targetSystemType = SystemType.OXYGEN;
			else if ( targetSystemTypeFlag == 3 ) targetSystemType = SystemType.WEAPONS;
			else if ( targetSystemTypeFlag == 4 ) targetSystemType = SystemType.DRONE_CTRL;
			else if ( targetSystemTypeFlag == 5 ) targetSystemType = SystemType.MEDBAY;
			else if ( targetSystemTypeFlag == 6 ) targetSystemType = SystemType.PILOT;
			else if ( targetSystemTypeFlag == 7 ) targetSystemType = SystemType.SENSORS;
			else if ( targetSystemTypeFlag == 8 ) targetSystemType = SystemType.DOORS;
			else if ( targetSystemTypeFlag == 9 ) targetSystemType = SystemType.TELEPORTER;
			else if ( targetSystemTypeFlag == 10 ) targetSystemType = SystemType.CLOAKING;
			else if ( targetSystemTypeFlag == 11 ) targetSystemType = SystemType.ARTILLERY;
			else if ( targetSystemTypeFlag == 12 ) targetSystemType = SystemType.BATTERY;
			else if ( targetSystemTypeFlag == 13 ) targetSystemType = SystemType.CLONEBAY;
			else if ( targetSystemTypeFlag == 14 ) targetSystemType = SystemType.MIND;
			else if ( targetSystemTypeFlag == 15 ) targetSystemType = SystemType.HACKING;
			else {
				throw new IOException( String.format( "Unsupported hacking targetSystemTypeFlag: %d", targetSystemTypeFlag ) );
			}
			hackingInfo.setTargetSystemType( targetSystemType );

			hackingInfo.setUnknownBeta( readInt( in ) );
			hackingInfo.setDronePodVisible( readBool( in ) );
			hackingInfo.setUnknownDelta( readInt( in ) );

			hackingInfo.setDisruptionTicks( readInt( in ) );
			hackingInfo.setDisruptionTicksGoal( readInt( in ) );

			hackingInfo.setDisrupting( readBool( in ) );

			DronePodState dronePod = readDronePod( in, DroneType.HACKING );  // The hacking drone.
			hackingInfo.setDronePod( dronePod );

			shipState.addExtendedSystemInfo( hackingInfo );
		}

		SystemState mindState = shipState.getSystem( SystemType.MIND );
		if ( mindState != null && mindState.getCapacity() > 0 ) {
			MindInfo mindInfo = new MindInfo();

			mindInfo.setMindControlTicks( readInt( in ) );
			mindInfo.setMindControlTicksGoal( readInt( in ) );

			shipState.addExtendedSystemInfo( mindInfo );
		}

		SystemState weaponsState = shipState.getSystem( SystemType.WEAPONS );
		if ( weaponsState != null && weaponsState.getCapacity() > 0 ) {

			int weaponCount = shipState.getWeaponList().size();
			int weaponModCount = readInt( in );
			if ( weaponModCount != weaponCount ) {
				throw new IOException( String.format( "Found %d WeaponModules, but there are %d Weapons.", weaponModCount, weaponCount ) );
			}

			for ( WeaponState weapon : shipState.getWeaponList() ) {
				WeaponModuleState weaponMod = readWeaponModule( in, fileFormat );
				weapon.setWeaponModule( weaponMod );
			}
		}

		// Get ALL artillery rooms' SystemStates from the ShipState.
		List<SystemState> artilleryStateList = shipState.getSystems( SystemType.ARTILLERY );
		for ( SystemState artilleryState : artilleryStateList ) {

			if ( artilleryState.getCapacity() > 0 ) {
				ArtilleryInfo artilleryInfo = new ArtilleryInfo();

				artilleryInfo.setWeaponModule( readWeaponModule( in, fileFormat ) );

				shipState.addExtendedSystemInfo( artilleryInfo );
			}
		}

		// A list of standalone drones, for flagship swarms. Always 0 for player.

		int standaloneDroneCount = readInt( in );
		for ( int i=0; i < standaloneDroneCount; i++ ) {
			String droneId = readString( in );
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneId );
			if ( droneBlueprint == null ) throw new IOException( "Unrecognized DroneBlueprint: "+ droneId );

			StandaloneDroneState standaloneDrone = new StandaloneDroneState();
			standaloneDrone.setDroneId( droneId );

			DroneType droneType = DroneType.findById( droneBlueprint.getType() );
			if ( droneType == null ) throw new IOException( String.format( "DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType() ) );

			DronePodState dronePod = readDronePod( in, droneType );
			standaloneDrone.setDronePod( dronePod );

			standaloneDrone.setUnknownAlpha( readInt( in ) );
			standaloneDrone.setUnknownBeta( readInt( in ) );
			standaloneDrone.setUnknownGamma( readInt( in ) );

			shipState.addStandaloneDrone( standaloneDrone );
		}
	}

	/**
	 * Writes additional fields of various ship-related classes.
	 *
	 * This method does not involve a dedicated class.
	 */
	public void writeExtendedShipInfo( OutputStream out, ShipState shipState, int fileFormat ) throws IOException {
		// There is no explicit list count for drones.
		for ( DroneState drone : shipState.getDroneList() ) {
			ExtendedDroneInfo droneInfo = drone.getExtendedDroneInfo();
			writeBool( out, droneInfo.isDeployed() );
			writeBool( out, droneInfo.isArmed() );

			if ( droneInfo.getDronePod() != null ) {
				writeDronePod( out, droneInfo.getDronePod() );
			}
		}

		SystemState hackingState = shipState.getSystem( SystemType.HACKING );
		if ( hackingState != null && hackingState.getCapacity() > 0 ) {
			// TODO: Compare system room count with extended info count.

			HackingInfo hackingInfo = shipState.getExtendedSystemInfo( HackingInfo.class );
			// This should not be null.

			SystemType targetSystemType = hackingInfo.getTargetSystemType();
			int targetSystemTypeFlag;
			if ( targetSystemType == null ) targetSystemTypeFlag = -1;
			else if ( SystemType.SHIELDS.equals( targetSystemType ) ) targetSystemTypeFlag = 0;
			else if ( SystemType.ENGINES.equals( targetSystemType ) ) targetSystemTypeFlag = 1;
			else if ( SystemType.OXYGEN.equals( targetSystemType ) ) targetSystemTypeFlag = 2;
			else if ( SystemType.WEAPONS.equals( targetSystemType ) ) targetSystemTypeFlag = 3;
			else if ( SystemType.DRONE_CTRL.equals( targetSystemType ) ) targetSystemTypeFlag = 4;
			else if ( SystemType.MEDBAY.equals( targetSystemType ) ) targetSystemTypeFlag = 5;
			else if ( SystemType.PILOT.equals( targetSystemType ) ) targetSystemTypeFlag = 6;
			else if ( SystemType.SENSORS.equals( targetSystemType ) ) targetSystemTypeFlag = 7;
			else if ( SystemType.DOORS.equals( targetSystemType ) ) targetSystemTypeFlag = 8;
			else if ( SystemType.TELEPORTER.equals( targetSystemType ) ) targetSystemTypeFlag = 9;
			else if ( SystemType.CLOAKING.equals( targetSystemType ) ) targetSystemTypeFlag = 10;
			else if ( SystemType.ARTILLERY.equals( targetSystemType ) ) targetSystemTypeFlag = 11;
			else if ( SystemType.BATTERY.equals( targetSystemType ) ) targetSystemTypeFlag = 12;
			else if ( SystemType.CLONEBAY.equals( targetSystemType ) ) targetSystemTypeFlag = 13;
			else if ( SystemType.MIND.equals( targetSystemType ) ) targetSystemTypeFlag = 14;
			else if ( SystemType.HACKING.equals( targetSystemType ) ) targetSystemTypeFlag = 15;
			else {
				throw new IOException( String.format( "Unsupported hacking targetSystemType: %s", targetSystemType.getId() ) );
			}
			writeInt( out, targetSystemTypeFlag );

			writeInt( out, hackingInfo.getUnknownBeta() );
			writeBool( out, hackingInfo.isDronePodVisible() );
			writeInt( out, hackingInfo.getUnknownDelta() );

			writeInt( out, hackingInfo.getDisruptionTicks() );
			writeInt( out, hackingInfo.getDisruptionTicksGoal() );

			writeBool( out, hackingInfo.isDisrupting() );

			writeDronePod( out, hackingInfo.getDronePod() );
		}

		SystemState mindState = shipState.getSystem( SystemType.MIND );
		if ( mindState != null && mindState.getCapacity() > 0 ) {
			MindInfo mindInfo = shipState.getExtendedSystemInfo( MindInfo.class );
			// This should not be null.
			writeInt( out, mindInfo.getMindControlTicks() );
			writeInt( out, mindInfo.getMindControlTicksGoal() );
		}

		// If there's a Weapons system, write the weapon modules (even if there are 0 of them).
		SystemState weaponsState = shipState.getSystem( SystemType.WEAPONS );
		if ( weaponsState != null && weaponsState.getCapacity() > 0 ) {

			int weaponCount = shipState.getWeaponList().size();
			writeInt( out, weaponCount );
			for ( WeaponState weapon : shipState.getWeaponList() ) {
				writeWeaponModule( out, weapon.getWeaponModule(), fileFormat );
			}
		}

		List<ArtilleryInfo> artilleryInfoList = shipState.getExtendedSystemInfoList( ArtilleryInfo.class );
		for ( ArtilleryInfo artilleryInfo : artilleryInfoList ) {
			writeWeaponModule( out, artilleryInfo.getWeaponModule(), fileFormat );
		}

		writeInt( out, shipState.getStandaloneDroneList().size() );
		for ( StandaloneDroneState standaloneDrone : shipState.getStandaloneDroneList() ) {
			writeString( out, standaloneDrone.getDroneId() );

			writeDronePod( out, standaloneDrone.getDronePod() );

			writeInt( out, standaloneDrone.getUnknownAlpha() );
			writeInt( out, standaloneDrone.getUnknownBeta() );
			writeInt( out, standaloneDrone.getUnknownGamma() );
		}
	}

	private DronePodState readDronePod( FileInputStream in, DroneType droneType ) throws IOException {
		if ( droneType == null ) throw new IllegalArgumentException( "DroneType cannot be null." );

		//log.debug( String.format( "Drone Pod: @%d", in.getChannel().position() ) );

		DronePodState dronePod = new DronePodState();
		dronePod.setDroneType( droneType );
		dronePod.setMourningTicks( readInt( in ) );
		dronePod.setCurrentSpace( readInt( in ) );
		dronePod.setDestinationSpace( readInt( in ) );

		dronePod.setCurrentPositionX( readMinMaxedInt( in ) );
		dronePod.setCurrentPositionY( readMinMaxedInt( in ) );
		dronePod.setPreviousPositionX( readMinMaxedInt( in ) );
		dronePod.setPreviousPositionY( readMinMaxedInt( in ) );
		dronePod.setGoalPositionX( readMinMaxedInt( in ) );
		dronePod.setGoalPositionY( readMinMaxedInt( in ) );

		dronePod.setUnknownEpsilon( readMinMaxedInt( in ) );
		dronePod.setUnknownZeta( readMinMaxedInt( in ) );
		dronePod.setNextTargetX( readMinMaxedInt( in ) );
		dronePod.setNextTargetY( readMinMaxedInt( in ) );
		dronePod.setUnknownIota( readMinMaxedInt( in ) );
		dronePod.setUnknownKappa( readMinMaxedInt( in ) );

		dronePod.setBuildupTicks( readInt( in ) );
		dronePod.setStationaryTicks( readInt( in ) );
		dronePod.setCooldownTicks( readInt( in ) );
		dronePod.setOrbitAngle( readInt( in ) );
		dronePod.setTurretAngle( readInt( in ) );
		dronePod.setUnknownXi( readInt( in ) );
		dronePod.setHopsToLive( readMinMaxedInt( in ) );
		dronePod.setUnknownPi( readInt( in ) );
		dronePod.setUnknownRho( readInt( in ) );
		dronePod.setOverloadTicks( readInt( in ) );
		dronePod.setUnknownTau( readInt( in ) );
		dronePod.setUnknownUpsilon( readInt( in ) );
		dronePod.setDeltaPositionX( readInt( in ) );
		dronePod.setDeltaPositionY( readInt( in ) );

		dronePod.setDeathAnim( readAnim( in ) );

		ExtendedDronePodInfo extendedInfo = null;
		if ( DroneType.BOARDER.equals( droneType ) ) {
			BoarderDronePodInfo boarderPodInfo = new BoarderDronePodInfo();
			boarderPodInfo.setUnknownAlpha( readInt( in ) );
			boarderPodInfo.setUnknownBeta( readInt( in ) );
			boarderPodInfo.setUnknownGamma( readInt( in ) );
			boarderPodInfo.setUnknownDelta( readInt( in ) );
			boarderPodInfo.setBodyHealth( readInt( in ) );
			boarderPodInfo.setBodyX( readInt( in ) );
			boarderPodInfo.setBodyY( readInt( in ) );
			boarderPodInfo.setBodyRoomId( readInt( in ) );
			boarderPodInfo.setBodyRoomSquare( readInt( in ) );
			extendedInfo = boarderPodInfo;
		}
		else if ( DroneType.HACKING.equals( droneType ) ) {
			HackingDronePodInfo hackingPodInfo = new HackingDronePodInfo();
			hackingPodInfo.setAttachPositionX( readInt( in ) );
			hackingPodInfo.setAttachPositionY( readInt( in ) );
			hackingPodInfo.setUnknownGamma( readInt( in ) );
			hackingPodInfo.setUnknownDelta( readInt( in ) );
			hackingPodInfo.setLandingAnim( readAnim( in ) );
			hackingPodInfo.setExtensionAnim( readAnim( in ) );
			extendedInfo = hackingPodInfo;
		}
		else if ( DroneType.COMBAT.equals( droneType ) ||
		          DroneType.BEAM.equals( droneType ) ) {

			ZigZagDronePodInfo zigPodInfo = new ZigZagDronePodInfo();
			zigPodInfo.setLastWaypointX( readInt( in ) );
			zigPodInfo.setLastWaypointY( readInt( in ) );
			zigPodInfo.setTransitTicks( readMinMaxedInt( in ) );
			zigPodInfo.setExhaustAngle( readMinMaxedInt( in ) );
			zigPodInfo.setUnknownEpsilon( readMinMaxedInt( in ) );
			extendedInfo = zigPodInfo;
		}
		else if ( DroneType.DEFENSE.equals( droneType ) ) {
			extendedInfo = new EmptyDronePodInfo();
		}
		else if ( DroneType.SHIELD.equals( droneType ) ) {
			ShieldDronePodInfo shieldPodInfo = new ShieldDronePodInfo();
			shieldPodInfo.setUnknownAlpha( readInt( in ) );
			extendedInfo = shieldPodInfo;
		}
		else if ( DroneType.SHIP_REPAIR.equals( droneType ) ) {
			ZigZagDronePodInfo zigPodInfo = new ZigZagDronePodInfo();
			zigPodInfo.setLastWaypointX( readInt( in ) );
			zigPodInfo.setLastWaypointY( readInt( in ) );
			zigPodInfo.setTransitTicks( readMinMaxedInt( in ) );
			zigPodInfo.setExhaustAngle( readMinMaxedInt( in ) );
			zigPodInfo.setUnknownEpsilon( readMinMaxedInt( in ) );
			extendedInfo = zigPodInfo;
		}
		else {
			throw new IOException( "Unsupported droneType for drone pod: "+ droneType.getId() );
		}

		dronePod.setExtendedInfo( extendedInfo );

		return dronePod;
	}

	public void writeDronePod( OutputStream out, DronePodState dronePod ) throws IOException {
		writeInt( out, dronePod.getMourningTicks() );
		writeInt( out, dronePod.getCurrentSpace() );
		writeInt( out, dronePod.getDestinationSpace() );

		writeMinMaxedInt( out, dronePod.getCurrentPositionX() );
		writeMinMaxedInt( out, dronePod.getCurrentPositionY() );
		writeMinMaxedInt( out, dronePod.getPreviousPositionX() );
		writeMinMaxedInt( out, dronePod.getPreviousPositionY() );
		writeMinMaxedInt( out, dronePod.getGoalPositionX() );
		writeMinMaxedInt( out, dronePod.getGoalPositionY() );

		writeMinMaxedInt( out, dronePod.getUnknownEpsilon() );
		writeMinMaxedInt( out, dronePod.getUnknownZeta() );
		writeMinMaxedInt( out, dronePod.getNextTargetX() );
		writeMinMaxedInt( out, dronePod.getNextTargetY() );
		writeMinMaxedInt( out, dronePod.getUnknownIota() );
		writeMinMaxedInt( out, dronePod.getUnknownKappa() );

		writeInt( out, dronePod.getBuildupTicks() );
		writeInt( out, dronePod.getStationaryTicks() );
		writeInt( out, dronePod.getCooldownTicks() );
		writeInt( out, dronePod.getOrbitAngle() );
		writeInt( out, dronePod.getTurretAngle() );
		writeInt( out, dronePod.getUnknownXi() );
		writeMinMaxedInt( out, dronePod.getHopsToLive() );
		writeInt( out, dronePod.getUnknownPi() );
		writeInt( out, dronePod.getUnknownRho() );
		writeInt( out, dronePod.getOverloadTicks() );
		writeInt( out, dronePod.getUnknownTau() );
		writeInt( out, dronePod.getUnknownUpsilon() );
		writeInt( out, dronePod.getDeltaPositionX() );
		writeInt( out, dronePod.getDeltaPositionY() );

		writeAnim( out, dronePod.getDeathAnim() );

		ExtendedDronePodInfo extendedInfo = dronePod.getExtendedInfo( ExtendedDronePodInfo.class );
		if ( extendedInfo instanceof IntegerDronePodInfo ) {
			IntegerDronePodInfo intPodInfo = dronePod.getExtendedInfo( IntegerDronePodInfo.class );
			for ( int i=0; i < intPodInfo.getSize(); i++ ) {
				writeMinMaxedInt( out, intPodInfo.get( i ) );
			}
		}
		else if ( extendedInfo instanceof BoarderDronePodInfo ) {
			BoarderDronePodInfo boarderPodInfo = dronePod.getExtendedInfo( BoarderDronePodInfo.class );
			writeInt( out, boarderPodInfo.getUnknownAlpha() );
			writeInt( out, boarderPodInfo.getUnknownBeta() );
			writeInt( out, boarderPodInfo.getUnknownGamma() );
			writeInt( out, boarderPodInfo.getUnknownDelta() );
			writeInt( out, boarderPodInfo.getBodyHealth() );
			writeInt( out, boarderPodInfo.getBodyX() );
			writeInt( out, boarderPodInfo.getBodyY() );
			writeInt( out, boarderPodInfo.getBodyRoomId() );
			writeInt( out, boarderPodInfo.getBodyRoomSquare() );
		}
		else if ( extendedInfo instanceof ShieldDronePodInfo ) {
			ShieldDronePodInfo shieldPodInfo = dronePod.getExtendedInfo( ShieldDronePodInfo.class );
			writeInt( out, shieldPodInfo.getUnknownAlpha() );
		}
		else if ( extendedInfo instanceof HackingDronePodInfo ) {
			HackingDronePodInfo hackingPodInfo = dronePod.getExtendedInfo( HackingDronePodInfo.class );
			writeInt( out, hackingPodInfo.getAttachPositionX() );
			writeInt( out, hackingPodInfo.getAttachPositionY() );
			writeInt( out, hackingPodInfo.getUnknownGamma() );
			writeInt( out, hackingPodInfo.getUnknownDelta() );
			writeAnim( out, hackingPodInfo.getLandingAnim() );
			writeAnim( out, hackingPodInfo.getExtensionAnim() );
		}
		else if ( extendedInfo instanceof ZigZagDronePodInfo ) {
			ZigZagDronePodInfo zigPodInfo = dronePod.getExtendedInfo( ZigZagDronePodInfo.class );
			writeInt( out, zigPodInfo.getLastWaypointX() );
			writeInt( out, zigPodInfo.getLastWaypointY() );
			writeMinMaxedInt( out, zigPodInfo.getTransitTicks() );
			writeMinMaxedInt( out, zigPodInfo.getExhaustAngle() );
			writeMinMaxedInt( out, zigPodInfo.getUnknownEpsilon() );
		}
		else if ( extendedInfo instanceof EmptyDronePodInfo ) {
			// No-op.
		}
		else {
			throw new IOException( "Unsupported extended drone pod info: "+ extendedInfo.getClass().getSimpleName() );
		}
	}

	private WeaponModuleState readWeaponModule( FileInputStream in, int fileFormat ) throws IOException {
		WeaponModuleState weaponMod = new WeaponModuleState();

		weaponMod.setCooldownTicks( readInt( in ) );
		weaponMod.setCooldownTicksGoal( readInt( in ) );
		weaponMod.setSubcooldownTicks( readInt( in ) );
		weaponMod.setSubcooldownTicksGoal( readInt( in ) );
		weaponMod.setBoost( readInt( in ) );
		weaponMod.setCharge( readInt( in ) );

		int currentTargetsCount = readInt( in );
		List<XYPair> currentTargetsList = new ArrayList<XYPair>();
		for ( int i=0; i < currentTargetsCount; i++ ) {
			currentTargetsList.add( readReticleCoordinate( in ) );
		}
		weaponMod.setCurrentTargets( currentTargetsList );

		int prevTargetsCount = readInt( in );
		List<XYPair> prevTargetsList = new ArrayList<XYPair>();
		for ( int i=0; i < prevTargetsCount; i++ ) {
			prevTargetsList.add( readReticleCoordinate( in ) );
		}
		weaponMod.setPreviousTargets( prevTargetsList );

		weaponMod.setAutofire( readBool( in ) );
		weaponMod.setFireWhenReady( readBool( in ) );
		weaponMod.setTargetId( readInt( in ) );

		weaponMod.setWeaponAnim( readAnim( in ) );

		weaponMod.setProtractAnimTicks( readInt( in ) );
		weaponMod.setFiring( readBool( in ) );
		weaponMod.setUnknownPhi( readBool( in ) );

		if ( fileFormat == 9 || fileFormat == 11 ) {
			weaponMod.setAnimCharge( readInt( in ) );

			weaponMod.setChargeAnim( readAnim( in ) );
		}
		weaponMod.setLastProjectileId( readInt( in ) );

		int pendingProjectilesCount = readInt( in );
		List<ProjectileState> pendingProjectiles = new ArrayList<ProjectileState>();
		for ( int i=0; i < pendingProjectilesCount; i++ ) {
			pendingProjectiles.add( readProjectile( in, fileFormat ) );
		}
		weaponMod.setPendingProjectiles( pendingProjectiles );

		return weaponMod;
	}

	public void writeWeaponModule( OutputStream out, WeaponModuleState weaponMod, int fileFormat ) throws IOException {
		writeInt( out, weaponMod.getCooldownTicks() );
		writeInt( out, weaponMod.getCooldownTicksGoal() );
		writeInt( out, weaponMod.getSubcooldownTicks() );
		writeInt( out, weaponMod.getSubcooldownTicksGoal() );
		writeInt( out, weaponMod.getBoost() );
		writeInt( out, weaponMod.getCharge() );

		writeInt( out, weaponMod.getCurrentTargets().size() );
		for ( XYPair target : weaponMod.getCurrentTargets() ) {
			writeReticleCoordinate( out, target );
		}

		writeInt( out, weaponMod.getPreviousTargets().size() );
		for ( XYPair target : weaponMod.getPreviousTargets() ) {
			writeReticleCoordinate( out, target );
		}

		writeBool( out, weaponMod.getAutofire() );
		writeBool( out, weaponMod.getFireWhenReady() );
		writeInt( out, weaponMod.getTargetId() );

		writeAnim( out, weaponMod.getWeaponAnim() );

		writeInt( out, weaponMod.getProtractAnimTicks() );
		writeBool( out, weaponMod.isFiring() );
		writeBool( out, weaponMod.getUnknownPhi() );

		if ( fileFormat == 9 || fileFormat == 11 ) {
			writeInt( out, weaponMod.getAnimCharge() );

			writeAnim( out, weaponMod.getChargeAnim() );
		}

		writeInt( out, weaponMod.getLastProjectileId() );

		writeInt( out, weaponMod.getPendingProjectiles().size() );
		for ( ProjectileState projectile : weaponMod.getPendingProjectiles() ) {
			writeProjectile( out, projectile, fileFormat );
		}
	}

	private XYPair readReticleCoordinate( FileInputStream in ) throws IOException {
		int reticleX = readInt( in );
		int reticleY = readInt( in );

		XYPair reticle = new XYPair( reticleX, reticleY );

		return reticle;
	}

	public void writeReticleCoordinate( OutputStream out, XYPair reticle ) throws IOException {
		writeInt( out, reticle.x );
		writeInt( out, reticle.y );
	}
}
