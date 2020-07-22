package net.blerf.ftl.ui.floorplan;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;

import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.model.shiplayout.RoomAndSquare;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.BatteryInfo;
import net.blerf.ftl.parser.SavedGameParser.CloakingInfo;
import net.blerf.ftl.parser.SavedGameParser.ClonebayInfo;
import net.blerf.ftl.parser.SavedGameParser.CrewState;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.parser.SavedGameParser.DronePodState;
import net.blerf.ftl.parser.SavedGameParser.DroneState;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.ExtendedSystemInfo;
import net.blerf.ftl.parser.SavedGameParser.RoomState;
import net.blerf.ftl.parser.SavedGameParser.ShieldsInfo;
import net.blerf.ftl.parser.SavedGameParser.SquareState;
import net.blerf.ftl.parser.SavedGameParser.StationDirection;
import net.blerf.ftl.parser.SavedGameParser.SystemState;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.parser.SavedGameParser.WeaponState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.ui.floorplan.BreachSprite;
import net.blerf.ftl.ui.floorplan.CrewSprite;
import net.blerf.ftl.ui.floorplan.DoorSprite;
import net.blerf.ftl.ui.floorplan.DroneBodySprite;
import net.blerf.ftl.ui.floorplan.DroneBoxSprite;
import net.blerf.ftl.ui.floorplan.FireSprite;
import net.blerf.ftl.ui.floorplan.FloorplanCoord;
import net.blerf.ftl.ui.floorplan.RoomSprite;
import net.blerf.ftl.ui.floorplan.ShipInteriorComponent;
import net.blerf.ftl.ui.floorplan.WeaponSprite;
import net.blerf.ftl.xml.Offset;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;


/**
 * A container to organize ship variables.
 *
 * Had this been a regular parent component, its bounds would limit the visible
 * area of descendants.
 */
public class ShipBundle {

	private FTLConstants ftlConstants = null;

	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;

	private int reservePowerCapacity = 0;
	private String shipName = null;

	private int hullAmt = 0;
	private int fuelAmt = 0;
	private int dronePartsAmt = 0;
	private int missilesAmt = 0;
	private int scrapAmt = 0;
	private boolean hostile = false;
	private int jumpChargeTicks = 0;
	private boolean jumping = false;
	private int jumpAnimTicks = 0;
	private int cloakAnimTicks = 0;
	private boolean playerControlled = false;
	private List<String> augmentIdList = new ArrayList<String>();

	private int originX = 0, originY = 0;
	private int layoutX = 0, layoutY = 0;
	private Map<Rectangle, Integer> roomRegionRoomIdMap = new HashMap<Rectangle, Integer>();
	private Map<Rectangle, FloorplanCoord> squareRegionCoordMap = new HashMap<Rectangle, FloorplanCoord>();
	private List<RoomAndSquare> blockedRasList = new ArrayList<RoomAndSquare>( 1 );

	private List<SpriteReference<DroneState>> droneRefs = new ArrayList<SpriteReference<DroneState>>();
	private List<SpriteReference<WeaponState>> weaponRefs = new ArrayList<SpriteReference<WeaponState>>();
	private List<SpriteReference<RoomState>> roomRefs = new ArrayList<SpriteReference<RoomState>>();
	private List<SpriteReference<SystemState>> systemRefs = new ArrayList<SpriteReference<SystemState>>();
	private List<SpriteReference<DoorState>> doorRefs = new ArrayList<SpriteReference<DoorState>>();
	private List<SpriteReference<CrewState>> crewRefs = new ArrayList<SpriteReference<CrewState>>();

	private List<DroneBoxSprite> droneBoxSprites = new ArrayList<DroneBoxSprite>();
	private List<DroneBodySprite> droneBodySprites = new ArrayList<DroneBodySprite>();
	private List<WeaponSprite> weaponSprites = new ArrayList<WeaponSprite>();
	private List<RoomSprite> roomSprites = new ArrayList<RoomSprite>();
	private List<SystemRoomSprite> systemRoomSprites = new ArrayList<SystemRoomSprite>();
	private List<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private List<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private List<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private List<CrewSprite> crewSprites = new ArrayList<CrewSprite>();

	private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<ExtendedSystemInfo>();

	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private ShipInteriorComponent interiorComp = null;


	public ShipBundle() {
	}

	public void setFTLConstants( FTLConstants ftlConstants ) { this.ftlConstants = ftlConstants; }
	public FTLConstants getFTLConstants() { return ftlConstants; }

	public void setShipBlueprint( ShipBlueprint shipBlueprint ) { this.shipBlueprint = shipBlueprint; }
	public ShipBlueprint getShipBlueprint() { return shipBlueprint; }

	public void setShipLayout( ShipLayout shipLayout ) { this.shipLayout = shipLayout; }
	public ShipLayout getShipLayout() { return shipLayout; }

	public void setShipChassis( ShipChassis shipChassis ) { this.shipChassis = shipChassis; }
	public ShipChassis getShipChassis() { return shipChassis; }

	public void setShipGraphicsBaseName( String s ) { shipGfxBaseName = s; }
	public String getShipGraphicsBaseName() { return shipGfxBaseName; }


	public void setBaseLbl( JLabel baseLbl ) { this.baseLbl = baseLbl; }
	public JLabel getBaseLbl() { return baseLbl; }

	public void setFloorLbl( JLabel floorLbl ) { this.floorLbl = floorLbl; }
	public JLabel getFloorLbl() { return floorLbl; }

	public void setInteriorComp( ShipInteriorComponent interiorComp ) { this.interiorComp = interiorComp; }
	public ShipInteriorComponent getInteriorComp() { return interiorComp; }


	public void setReservePowerCapacity( int n ) { reservePowerCapacity = n; }
	public int getReservePowerCapacity() { return reservePowerCapacity; }

	public void setShipName( String s ) { shipName = s; }
	public String getShipName() { return shipName; }

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

	public void setHostile( boolean b ) { hostile = b; }
	public boolean isHostile() { return hostile; }

	public void setJumpChargeTicks( int n ) { jumpChargeTicks = n; }
	public int getJumpChargeTicks() { return jumpChargeTicks; }

	public void setJumping( boolean b ) { jumping = b; }
	public boolean isJumping() { return jumping; }

	public void setJumpAnimTicks( int n ) { jumpAnimTicks = n; }
	public int getJumpAnimTicks() { return jumpAnimTicks; }

	public void setCloakAnimTicks( int n ) { cloakAnimTicks = n; }
	public int getCloakAnimTicks() { return cloakAnimTicks; }

	public void setPlayerControlled( boolean b ) { playerControlled = b; }
	public boolean isPlayerControlled() { return playerControlled; }

	public List<String> getAugmentIdList() { return augmentIdList; }


	public void setOriginX( int n ) { originX = n; }
	public void setOriginY( int n ) { originY = n; }
	public int getOriginX() { return originX; }
	public int getOriginY() { return originY; }

	public void setLayoutX( int n ) { layoutX = n; }
	public void setLayoutY( int n ) { layoutY = n; }
	public int getLayoutX() { return layoutX; }
	public int getLayoutY() { return layoutY; }

	public Map<Rectangle, Integer> getRoomRegionRoomIdMap() { return roomRegionRoomIdMap; }
	public Map<Rectangle, FloorplanCoord> getSquareRegionCoordMap() { return squareRegionCoordMap; }
	public List<RoomAndSquare> getBlockedRasList() { return blockedRasList; }

	public List<SpriteReference<DroneState>> getDroneRefs() { return droneRefs; }
	public List<SpriteReference<WeaponState>> getWeaponRefs() { return weaponRefs; }
	public List<SpriteReference<RoomState>> getRoomRefs() { return roomRefs; }
	public List<SpriteReference<SystemState>> getSystemRefs() { return systemRefs; }
	public List<SpriteReference<DoorState>> getDoorRefs() { return doorRefs; }
	public List<SpriteReference<CrewState>> getCrewRefs() { return crewRefs; }

	public List<DroneBoxSprite> getDroneBoxSprites() { return droneBoxSprites; }
	public List<DroneBodySprite> getDroneBodySprites() { return droneBodySprites; }
	public List<WeaponSprite> getWeaponSprites() { return weaponSprites; }
	public List<RoomSprite> getRoomSprites() { return roomSprites; }
	public List<SystemRoomSprite> getSystemRoomSprites() { return systemRoomSprites; }
	public List<BreachSprite> getBreachSprites() { return breachSprites; }
	public List<FireSprite> getFireSprites() { return fireSprites; }
	public List<DoorSprite> getDoorSprites() { return doorSprites; }
	public List<CrewSprite> getCrewSprites() { return crewSprites; }

	public List<ExtendedSystemInfo> getExtendedSystemInfoList() { return extendedSystemInfoList; }

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

	/**
	 * Returns the first system reference, or null.
	 */
	public SpriteReference<SystemState> getSystemRef( SystemType systemType ) {
		SpriteReference<SystemState> result = null;

		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( systemType.equals( systemRef.get().getSystemType() ) ) {
				result = systemRef;
				break;
			}
		}

		return result;
	}

	/**
	 * Returns the roomId which contains the center of a given sprite, or -1.
	 */
	public int getSpriteRoomId( Component c ) {
		int result = -1;
		int centerX = c.getBounds().x + c.getBounds().width/2;
		int centerY = c.getBounds().y + c.getBounds().height/2;

		for ( Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet() ) {
			if ( regionEntry.getKey().contains( centerX, centerY ) ) {
				result = regionEntry.getValue().intValue();
			}
		}

		return result;
	}

	/**
	 * Returns the number of friendly Zoltan crew sprites in a room.
	 */
	public int getRoomZoltanEnergy( int roomId ) {
		if ( roomId < 0 ) return 0;

		int result = 0;
		Rectangle roomRect = null;

		for ( Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet() ) {
			if ( regionEntry.getValue().intValue() == roomId ) {
				roomRect = regionEntry.getKey();
				break;
			}
		}

		if ( roomRect != null ) {
			for ( SpriteReference<CrewState> crewRef : crewRefs ) {
				if ( CrewType.ENERGY.equals( crewRef.get().getRace() ) ) {
					if ( crewRef.get().isPlayerControlled() == this.playerControlled ) {
						CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
						int centerX = crewSprite.getX() + crewSprite.getWidth()/2;
						int centerY = crewSprite.getY() + crewSprite.getHeight()/2;
						if ( roomRect.contains( centerX, centerY ) ) {
							result++;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Returns available reserve power after limits are applied and systems'
	 * demand is subtracted (min 0).
	 *
	 * Note: Plasma storms are not considered, due to limitations in the saved
	 * game format. That info is buried in events and obscured by the random
	 * sector layout seed.
	 *
	 * Overallocation will cause FTL to depower systems in-game.
	 *
	 * @param excludeRef count demand from all systems except one (may be null)
	 */
	public int getReservePool( SpriteReference<SystemState> excludeRef ) {
		int result = reservePowerCapacity;

		int systemsPower = 0;
		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
				// TODO: Check if Battery system is currently being hack-disrupted,
				// then subtract 2.
			}

			if ( systemRef == excludeRef ) continue;

			if ( !systemRef.get().getSystemType().isSubsystem() ) {
				systemsPower += systemRef.get().getPower();
			}
		}
		result -= systemsPower;
		result = Math.max( 0, result );

		return result;
	}

	/**
	 * Returns the total battery power produced by the Battery system.
	 */
	public int getBatteryPoolCapacity() {
		int batteryPoolCapacity = 0;

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batteryInfo != null && batteryInfo.isActive() ) {
			SpriteReference<SystemState> batterySystemRef = getSystemRef( SystemType.BATTERY );
			// This should not be null.
			batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity( batterySystemRef.get().getCapacity() );
		}

		return batteryPoolCapacity;
	}

	/**
	 * Returns available battery power after systems' demand is subtracted
	 * (min 0).
	 *
	 * @param excludeRef count demand from all systems except one (may be null)
	 */
	public int getBatteryPool( SpriteReference<SystemState> excludeRef ) {
		int result = 0;

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batteryInfo != null && batteryInfo.isActive() ) {
			int batteryPoolCapacity = 0;
			int systemsBattery = 0;
			for ( SpriteReference<SystemState> systemRef : systemRefs ) {
				if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
					batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity( systemRef.get().getCapacity() );
				}

				if ( systemRef == excludeRef ) continue;

				if ( !systemRef.get().getSystemType().isSubsystem() ) {
					systemsBattery += systemRef.get().getBatteryPower();
				}
			}
			result = batteryPoolCapacity - systemsBattery;
			result = Math.max( 0, result );
		}

		return result;
	}

	public void updateBatteryPool() {
		SpriteReference<SystemState> batterySystemRef = null;
		int systemsBattery = 0;
		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
				batterySystemRef = systemRef;
			}

			if ( !systemRef.get().getSystemType().isSubsystem() ) {
				systemsBattery += systemRef.get().getBatteryPower();
			}
		}

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batterySystemRef != null && batterySystemRef.get().getCapacity() > 0 ) {
			if ( batteryInfo == null ) {
				batteryInfo = new BatteryInfo();
				extendedSystemInfoList.add( batteryInfo );
			}
			if ( !batteryInfo.isActive() ) {
				batteryInfo.setActive( true );
				batteryInfo.setDischargeTicks( 0 );
			}
			batteryInfo.setUsedBattery( systemsBattery );
		}
		else {
			if ( batteryInfo != null ) {
				batteryInfo.setActive( false );
				batteryInfo.setDischargeTicks( 1000 );
				extendedSystemInfoList.remove( batteryInfo );
			}
		}
	}
}
