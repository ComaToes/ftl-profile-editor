package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.xml.DefaultDeferredText;


@XmlRootElement( name = "shipBlueprint" )
@XmlAccessorType( XmlAccessType.FIELD )
public class ShipBlueprint {
	
	@XmlAttribute( name = "name" )
	private String id;

	@XmlAttribute( name = "layout" )
	private String layoutId;

	@XmlAttribute( name = "img" )
	private String gfxBaseName;
	
	@XmlElement( name = "class" )
	private DefaultDeferredText shipClass;

	@XmlElement( required = false )
	private DefaultDeferredText name;

	@XmlElement( required = false )
	private DefaultDeferredText unlockTip;

	@XmlElement( required = false )
	private DefaultDeferredText desc;
	
	private SystemList systemList;

	@XmlElement( required = false )  // Not present in autoBlueprints.xml.
	private Integer weaponSlots, droneSlots;

	@XmlElement( required = false )
	private WeaponList weaponList;

	@XmlElement( name = "aug", required = false )
	private List<AugmentId> augmentIds;

	@XmlElement( required = false )
	private DroneList droneList;

	private Health health;
	private MaxPower maxPower;   // Initial reserve power capacity.
	private CrewCount crewCount;

	@XmlElement( required = false )
	private String boardingAI;  // Only present in autoBlueprints.xml.

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class SystemList {

		@XmlAccessorType( XmlAccessType.FIELD )
		public static class RoomSlot {

			/**
			 * The direction crew will face when standing at the terminal.
			 */
			@XmlElement( required = false )
			private String direction;

			private int number;  // The room square.
			
			public void setDirection( String direction ) {
				this.direction = direction;
			}
			public String getDirection() {
				return direction;
			}
			public void setNumber( int number ) {
				this.number = number;
			}
			public int getNumber() {
				return number;
			}
		}
		
		@XmlAccessorType( XmlAccessType.FIELD )
		public static class SystemRoom {

			/**
			 * Minimum random system capacity.
			 *
			 * Systems will try to be fully powered unless the ship's reserve
			 * is insufficient.
			 */
			@XmlAttribute
			private int power;

			/**
			 * Maximum random system capacity.
			 *
			 * Not capped by SystemBlueprint's maxPower.
			 */
			@XmlAttribute( name = "max", required = false )
			private Integer maxPower;

			@XmlAttribute( name = "room" )
			private int roomId;

			/**
			 * Whether this system comes pre-installed.
			 *
			 * Treat null omissions as as true.
			 * On randomly generated ships, false means it's sometimes present.
			 */
			@XmlAttribute( required = false )
			private Boolean start;

			@XmlAttribute( required = false )
			private String img;

			/**
			 * The room square that a system has its terminal at.
			 *
			 * For the medbay and clonebay, this is the blocked square.
			 * When omitted, each system has a different hard-coded default.
			 */
			@XmlElement( required = false )
			private RoomSlot slot;
			
			public void setPower( int power ) {
				this.power = power;
			}
			public int getPower() {
				return power;
			}
			public void setMaxPower( Integer maxPower ) {
				this.maxPower = maxPower;
			}
			public Integer getMaxPower() {
				return maxPower;
			}
			public void setRoomId( int roomId ) {
				this.roomId = roomId;
			}
			public int getRoomId() {
				return roomId;
			}
			public void setStart( Boolean start ) {
				this.start = start;
			}
			public Boolean getStart() {
				return start;
			}
			public void setImg( String img ) {
				this.img = img;
			}
			public String getImg() {
				return img;
			}
			public void setSlot( RoomSlot slot ) {
				this.slot = slot;
			}
			public RoomSlot getSlot() {
				return slot;
			}
		}
		
		@XmlElement( name = "pilot" )
		private SystemRoom pilotRoom;
		@XmlElement( name = "doors" )
		private SystemRoom doorsRoom;
		@XmlElement( name = "sensors" )
		private SystemRoom sensorsRoom;
		@XmlElement( name = "medbay" )
		private SystemRoom medicalRoom;
		@XmlElement( name = "oxygen" )
		private SystemRoom lifeSupportRoom;
		@XmlElement( name = "shields" )
		private SystemRoom shieldRoom;
		@XmlElement( name = "engines" )
		private SystemRoom engineRoom;
		@XmlElement( name = "weapons" )
		private SystemRoom weaponRoom;
		@XmlElement( name = "drones" )
		private SystemRoom droneRoom;
		@XmlElement( name = "teleporter" )
		private SystemRoom teleporterRoom;
		@XmlElement( name = "cloaking" )
		private SystemRoom cloakRoom;  // lol :)
		@XmlElement( name = "artillery", required = false )
		private List<SystemRoom> artilleryRooms;
		@XmlElement( name = "clonebay" )
		private SystemRoom cloneRoom;
		@XmlElement( name = "hacking" )
		private SystemRoom hackRoom;
		@XmlElement( name = "mind" )
		private SystemRoom mindRoom;
		@XmlElement( name = "battery" )
		private SystemRoom batteryRoom;


		public SystemRoom[] getSystemRooms() {
			SystemRoom[] rooms = new SystemRoom[] {
				pilotRoom, doorsRoom, sensorsRoom, medicalRoom, lifeSupportRoom, shieldRoom,
				engineRoom, weaponRoom, droneRoom, teleporterRoom, cloakRoom
			};

			List<SystemRoom> list = new ArrayList<SystemRoom>();
			for ( SystemRoom room : rooms ) {
				if( room != null ) list.add( room );
			}
			if( artilleryRooms != null ) {
				list.addAll( artilleryRooms );
			}
			return list.toArray( new SystemRoom[list.size()] );
		}
		
		public void setPilotRoom( SystemRoom pilotRoom ) {
			this.pilotRoom = pilotRoom;
		}
		public SystemRoom getPilotRoom() {
			return pilotRoom;
		}
		public void setDoorsRoom( SystemRoom doorsRoom ) {
			this.doorsRoom = doorsRoom;
		}
		public SystemRoom getDoorsRoom() {
			return doorsRoom;
		}
		public void setSensorsRoom( SystemRoom sensorsRoom ) {
			this.sensorsRoom = sensorsRoom;
		}
		public SystemRoom getSensorsRoom() {
			return sensorsRoom;
		}
		public void setMedicalRoom( SystemRoom medicalRoom ) {
			this.medicalRoom = medicalRoom;
		}
		public SystemRoom getMedicalRoom() {
			return medicalRoom;
		}
		public void setLifeSupportRoom( SystemRoom lifeSupportRoom ) {
			this.lifeSupportRoom = lifeSupportRoom;
		}
		public SystemRoom getLifeSupportRoom() {
			return lifeSupportRoom;
		}
		public void setShieldRoom( SystemRoom shieldRoom ) {
			this.shieldRoom = shieldRoom;
		}
		public SystemRoom getShieldRoom() {
			return shieldRoom;
		}
		public void setEngineRoom( SystemRoom engineRoom ) {
			this.engineRoom = engineRoom;
		}
		public SystemRoom getEngineRoom() {
			return engineRoom;
		}
		public void setWeaponRoom( SystemRoom weaponRoom ) {
			this.weaponRoom = weaponRoom;
		}
		public SystemRoom getWeaponRoom() {
			return weaponRoom;
		}
		public void setDroneRoom( SystemRoom droneRoom ) {
			this.droneRoom = droneRoom;
		}
		public SystemRoom getDroneRoom() {
			return droneRoom;
		}
		public void setTeleporterRoom( SystemRoom teleporterRoom ) {
			this.teleporterRoom = teleporterRoom;
		}
		public SystemRoom getTeleporterRoom() {
			return teleporterRoom;
		}
		public void setCloakRoom( SystemRoom cloakRoom ) {
			this.cloakRoom = cloakRoom;
		}
		public SystemRoom getCloakRoom() {
			return cloakRoom;
		}
		public void setArtilleryRooms( List<SystemRoom> artilleryRooms ) {
			this.artilleryRooms = artilleryRooms;
		}
		public List<SystemRoom> getArtilleryRooms() {
			return artilleryRooms;
		}
		public void setCloneRoom( SystemRoom cloneRoom ) {
			this.cloneRoom = cloneRoom;
		}
		public SystemRoom getCloneRoom() {
			return cloneRoom;
		}
		public void setHackRoom( SystemRoom hackRoom ) {
			this.hackRoom = hackRoom;
		}
		public SystemRoom getHackRoom() {
			return hackRoom;
		}
		public void setMindRoom( SystemRoom mindRoom ) {
			this.mindRoom = mindRoom;
		}
		public SystemRoom getMindRoom() {
			return mindRoom;
		}
		public void setBatteryRoom( SystemRoom batteryRoom ) {
			this.batteryRoom = batteryRoom;
		}
		public SystemRoom getBatteryRoom() {
			return batteryRoom;
		}

		/**
		 * Returns SystemRooms, or null if not present.
		 *
		 * @return an array of SystemRooms, usually only containing one
		 */
		public SystemList.SystemRoom[] getSystemRoom( SystemType systemType ) {
			SystemList.SystemRoom systemRoom = null;
			if ( SystemType.PILOT.equals( systemType ) ) systemRoom = getPilotRoom();
			else if ( SystemType.DOORS.equals( systemType ) ) systemRoom = getDoorsRoom();
			else if ( SystemType.SENSORS.equals( systemType ) ) systemRoom = getSensorsRoom();
			else if ( SystemType.MEDBAY.equals( systemType ) ) systemRoom = getMedicalRoom();
			else if ( SystemType.OXYGEN.equals( systemType ) ) systemRoom = getLifeSupportRoom();
			else if ( SystemType.SHIELDS.equals( systemType ) ) systemRoom = getShieldRoom();
			else if ( SystemType.ENGINES.equals( systemType ) ) systemRoom = getEngineRoom();
			else if ( SystemType.WEAPONS.equals( systemType ) ) systemRoom = getWeaponRoom();
			else if ( SystemType.DRONE_CTRL.equals( systemType ) ) systemRoom = getDroneRoom();
			else if ( SystemType.TELEPORTER.equals( systemType ) ) systemRoom = getTeleporterRoom();
			else if ( SystemType.CLOAKING.equals( systemType ) ) systemRoom = getCloakRoom();
			else if ( SystemType.BATTERY.equals( systemType ) ) systemRoom = getBatteryRoom();
			else if ( SystemType.CLONEBAY.equals( systemType ) ) systemRoom = getCloneRoom();
			else if ( SystemType.MIND.equals( systemType ) ) systemRoom = getMindRoom();
			else if ( SystemType.HACKING.equals( systemType ) ) systemRoom = getHackRoom();

			if ( systemRoom != null ) return new SystemList.SystemRoom[] { systemRoom };

			if ( SystemType.ARTILLERY.equals( systemType ) ) {
				if ( getArtilleryRooms() != null && getArtilleryRooms().size() > 0 ) {
					int n = 0;
					SystemList.SystemRoom[] result = new SystemList.SystemRoom[getArtilleryRooms().size()];
					for ( SystemRoom artilleryRoom : artilleryRooms ) {
						result[n++] = artilleryRoom;
					}
					return result;
				}
			}

			return null;
		}

		/**
		 * Returns the SystemType in a given room, or null.
		 *
		 * TODO: Make this return multiple SystemTypes (ex: medbay/clonebay).
		 */
		public SystemType getSystemTypeByRoomId( int roomId ) {
			for ( SystemType systemType : SystemType.values() ) {
				SystemList.SystemRoom[] systemRooms = getSystemRoom( systemType );
				if ( systemRooms != null ) {
					for ( SystemList.SystemRoom systemRoom : systemRooms ) {
						if ( systemRoom.getRoomId() == roomId )
							return systemType;
					}
				}
			}
			return null;
		}

		/**
		 * Returns roomId(s) that contain a given system, or null.
		 *
		 * @return an array of roomIds, usually only containing one
		 */
		public int[] getRoomIdBySystemType( SystemType systemType ) {
			int[] result = null;
			SystemList.SystemRoom[] systemRooms = getSystemRoom( systemType );
			if ( systemRooms != null ) {
				result = new int[ systemRooms.length ];
				for (int i=0; i < systemRooms.length; i++) {
					result[i] = systemRooms[i].getRoomId();
				}
			}
			return result;
		}
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class WeaponList {

		// 'count' isn't an independent field; a getter/setter calc's it (See below).

		@XmlAttribute
		public int missiles;

		@XmlAttribute( name = "load", required = false )
		public String blueprintListId;

		@XmlElement( name = "weapon" )
		private List<WeaponId> weaponIds;

		@XmlAccessorType( XmlAccessType.FIELD )
		public static class WeaponId {
			@XmlAttribute
			public String name;
		}


		public void setCount( int n ) { /* No-op */ }

		@XmlAttribute( name="count" )
		public int getCount() {
			return (weaponIds != null ? weaponIds.size() : 0);
		}

		public void setWeaponIds( List<WeaponId> weaponIds ) {
			this.weaponIds = weaponIds;
		}

		public List<WeaponId> getWeaponIds() {
			return weaponIds;
		}
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class AugmentId {
		@XmlAttribute
		public String name;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class DroneList {

		// 'count' isn't an independent field; a getter/setter calc's it (See below).

		@XmlAttribute
		public int drones;

		@XmlAttribute( name = "load", required = false )
		public String blueprintListId;

		@XmlElement( name = "drone" )
		private List<DroneId> droneIds;

		@XmlAccessorType( XmlAccessType.FIELD )
		public static class DroneId {
			@XmlAttribute
			public String name;
		}


		public void setCount( int n ) { /* No-op */ }

		@XmlAttribute( name = "count" )
		public int getCount() {
			return (droneIds != null ? droneIds.size() : 0);
		}

		public void setDroneIds( List<DroneId> droneIds ) {
			this.droneIds = droneIds;
		}

		public List<DroneId> getDroneIds() {
			return droneIds;
		}
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class Health {
		@XmlAttribute
		public int amount;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class MaxPower {
		@XmlAttribute
		public int amount;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class CrewCount {
		@XmlAttribute
		public int amount;

		@XmlAttribute( required = false )
		public Integer max;  // Only present in autoBlueprints.xml.

		@XmlAttribute( name = "class" )
		public String race;
	}


	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setLayout( String layoutId ) {
		this.layoutId = layoutId;
	}

	public String getLayoutId() {
		return layoutId;
	}

	public void setGraphicsBaseName( String gfxBaseName ) {
		this.gfxBaseName = gfxBaseName;
	}

	public String getGraphicsBaseName() {
		return gfxBaseName;
	}

	public void setShipClass( DefaultDeferredText shipClass ) {
		this.shipClass = shipClass;
	}

	public DefaultDeferredText getShipClass() {
		return shipClass;
	}

	public void setName( DefaultDeferredText name ) {
		this.name = name;
	}

	public DefaultDeferredText getName() {
		return name;
	}

	public void setUnlockTip( DefaultDeferredText unlockTip ) {
		this.unlockTip = unlockTip;
	}

	public DefaultDeferredText getUnlockTip() {
		return unlockTip;
	}

	public void setDescription( DefaultDeferredText desc ) {
		this.desc = desc;
	}

	public DefaultDeferredText getDescription() {
		return desc;
	}

	public void setSystemList( SystemList systemList ) {
		this.systemList = systemList;
	}

	public SystemList getSystemList() {
		return systemList;
	}

	public void setWeaponSlots( Integer weaponSlots ) {
		this.weaponSlots = weaponSlots;
	}

	public Integer getWeaponSlots() {
		return weaponSlots;
	}

	public void setDroneSlots( Integer droneSlots ) {
		this.droneSlots = droneSlots;
	}

	public Integer getDroneSlots() {
		return droneSlots;
	}

	public void setWeaponList( WeaponList weaponList ) {
		this.weaponList = weaponList;
	}

	public WeaponList getWeaponList() {
		return weaponList;
	}

	public void setAugments( List<AugmentId> augmentIds ) {
		this.augmentIds = augmentIds;
	}

	public List<AugmentId> getAugments() {
		return augmentIds;
	}

	public void setDroneList( DroneList droneList ) {
		this.droneList = droneList;
	}

	public DroneList getDroneList() {
		return droneList;
	}

	public void setHealth( Health health ) {
		this.health = health;
	}

	public Health getHealth() {
		return health;
	}

	public void setMaxPower( MaxPower maxPower ) {
		this.maxPower = maxPower;
	}

	public MaxPower getMaxPower() {
		return maxPower;
	}

	public void setCrewCount( CrewCount crewCount ) {
		this.crewCount = crewCount;
	}

	public CrewCount getCrewCount() {
		return crewCount;
	}

	public void setBoardingAI( String boardingAI ) {
		this.boardingAI = boardingAI;
	}

	public String getBoardingAI() {
		return boardingAI;
	}

	@Override
	public String toString() {
		return String.format( "%s (%s)", id, shipClass );
	}
}
