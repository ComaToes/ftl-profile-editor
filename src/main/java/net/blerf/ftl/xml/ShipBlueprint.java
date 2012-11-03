package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.SystemBlueprint;

@XmlRootElement(name="shipBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class ShipBlueprint {
	
	@XmlAttribute(name="name")
	private String id;
	@XmlAttribute
	private String layout;
	@XmlAttribute
	private String img;
	
	@XmlElement(name="class")
	private String shipClass;
	private String name, desc;
	
	private SystemList systemList;
	private Health health;
	private MaxPower maxPower;
	private int weaponSlots, droneSlots;
	
	private Object weaponList, crewCount; // TODO model
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SystemList {

		@XmlRootElement
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class RoomSlot {
			@XmlElement(required=false)
			private String direction;
			@XmlElement
			private int number;
			
			public String getDirection() {
				return direction;
			}
			public void setDirection(String direction) {
				this.direction = direction;
			}
			public int getNumber() {
				return number;
			}
			public void setNumber(int number) {
				this.number = number;
			}
			
		}
		
		@XmlRootElement
		@XmlAccessorType(XmlAccessType.FIELD)
		public static class SystemRoom {
			
			@XmlAttribute
			private int power;
			@XmlAttribute(name="room")
			private int roomId;
			@XmlAttribute(required=false)
			private boolean start;
			@XmlAttribute(required=false)
			private String img;
			@XmlElement(required=false)
			private RoomSlot slot;
			
			public int getPower() {
				return power;
			}
			public void setPower(int power) {
				this.power = power;
			}
			public int getRoomId() {
				return roomId;
			}
			public void setRoomId(int roomId) {
				this.roomId = roomId;
			}
			public boolean isStart() {
				return start;
			}
			public void setStart(boolean start) {
				this.start = start;
			}
			public String getImg() {
				return img;
			}
			public void setImg(String img) {
				this.img = img;
			}
			public RoomSlot getSlot() {
				return slot;
			}
			public void setSlot(RoomSlot slot) {
				this.slot = slot;
			}
			
		}
		
		@XmlElement(name="pilot")
		private SystemRoom pilotRoom;
		@XmlElement(name="doors")
		private SystemRoom doorsRoom;
		@XmlElement(name="sensors")
		private SystemRoom sensorsRoom;
		@XmlElement(name="medbay")
		private SystemRoom medicalRoom;
		@XmlElement(name="oxygen")
		private SystemRoom lifeSupportRoom;
		@XmlElement(name="shields")
		private SystemRoom shieldRoom;
		@XmlElement(name="engines")
		private SystemRoom engineRoom;
		@XmlElement(name="weapons")
		private SystemRoom weaponRoom;
		@XmlElement(name="drones")
		private SystemRoom droneRoom;
		@XmlElement(name="teleporter")
		private SystemRoom teleporterRoom;
		@XmlElement(name="cloaking")
		private SystemRoom cloakRoom; // lol :)
		@XmlElement(name="artillery",required=false)
		private List<SystemRoom> artilleryRooms;
		
		public SystemRoom[] getSystemRooms() {
			SystemRoom[] rooms = new SystemRoom[] { pilotRoom, doorsRoom, sensorsRoom, medicalRoom, lifeSupportRoom, shieldRoom, 
					engineRoom, weaponRoom, droneRoom, teleporterRoom, cloakRoom };
			List<SystemRoom> list = new ArrayList<SystemRoom>();
			for (SystemRoom room : rooms) {
				list.add(room);
			}
			if( artilleryRooms != null )
				list.addAll(artilleryRooms);
			return list.toArray(new SystemRoom[list.size()]);
		}
		
		public SystemRoom getPilotRoom() {
			return pilotRoom;
		}
		public void setPilotRoom(SystemRoom pilotRoom) {
			this.pilotRoom = pilotRoom;
		}
		public SystemRoom getDoorsRoom() {
			return doorsRoom;
		}
		public void setDoorsRoom(SystemRoom doorsRoom) {
			this.doorsRoom = doorsRoom;
		}
		public SystemRoom getSensorsRoom() {
			return sensorsRoom;
		}
		public void setSensorsRoom(SystemRoom sensorsRoom) {
			this.sensorsRoom = sensorsRoom;
		}
		public SystemRoom getMedicalRoom() {
			return medicalRoom;
		}
		public void setMedicalRoom(SystemRoom medicalRoom) {
			this.medicalRoom = medicalRoom;
		}
		public SystemRoom getLifeSupportRoom() {
			return lifeSupportRoom;
		}
		public void setLifeSupportRoom(SystemRoom lifeSupportRoom) {
			this.lifeSupportRoom = lifeSupportRoom;
		}
		public SystemRoom getShieldRoom() {
			return shieldRoom;
		}
		public void setShieldRoom(SystemRoom shieldRoom) {
			this.shieldRoom = shieldRoom;
		}
		public SystemRoom getEngineRoom() {
			return engineRoom;
		}
		public void setEngineRoom(SystemRoom engineRoom) {
			this.engineRoom = engineRoom;
		}
		public SystemRoom getWeaponRoom() {
			return weaponRoom;
		}
		public void setWeaponRoom(SystemRoom weaponRoom) {
			this.weaponRoom = weaponRoom;
		}
		public SystemRoom getDroneRoom() {
			return droneRoom;
		}
		public void setDroneRoom(SystemRoom droneRoom) {
			this.droneRoom = droneRoom;
		}
		public SystemRoom getTeleporterRoom() {
			return teleporterRoom;
		}
		public void setTeleporterRoom(SystemRoom teleporterRoom) {
			this.teleporterRoom = teleporterRoom;
		}
		public SystemRoom getCloakRoom() {
			return cloakRoom;
		}
		public void setCloakRoom(SystemRoom cloakRoom) {
			this.cloakRoom = cloakRoom;
		}
		public List<SystemRoom> getArtilleryRooms() {
			return artilleryRooms;
		}
		public void setArtilleryRooms(List<SystemRoom> artilleryRooms) {
			this.artilleryRooms = artilleryRooms;
		}

		/**
		 * Returns the systemId in a given room, or null.
		 *
		 * @return one of SystemBlueprint's ID_* constants.
		 */
		public String getSystemIdByRoomId( int roomId ) {
			if ( getPilotRoom() != null && getPilotRoom().getRoomId() == roomId ) return SystemBlueprint.ID_PILOT;
			if ( getDoorsRoom() != null && getDoorsRoom().getRoomId() == roomId ) return SystemBlueprint.ID_DOORS;
			if ( getSensorsRoom() != null && getSensorsRoom().getRoomId() == roomId ) return SystemBlueprint.ID_SENSORS;
			if ( getMedicalRoom() != null && getMedicalRoom().getRoomId() == roomId ) return SystemBlueprint.ID_MEDBAY;
			if ( getLifeSupportRoom() != null && getLifeSupportRoom().getRoomId() == roomId ) return SystemBlueprint.ID_OXYGEN;
			if ( getShieldRoom() != null && getShieldRoom().getRoomId() == roomId ) return SystemBlueprint.ID_SHIELDS;
			if ( getEngineRoom() != null && getEngineRoom().getRoomId() == roomId ) return SystemBlueprint.ID_ENGINES;
			if ( getWeaponRoom() != null && getWeaponRoom().getRoomId() == roomId ) return SystemBlueprint.ID_WEAPONS;
			if ( getDroneRoom() != null && getDroneRoom().getRoomId() == roomId ) return SystemBlueprint.ID_DRONE_CTRL;
			if ( getTeleporterRoom() != null && getTeleporterRoom().getRoomId() == roomId ) return SystemBlueprint.ID_TELEPORTER;
			if ( getCloakRoom() != null && getCloakRoom().getRoomId() == roomId ) return SystemBlueprint.ID_CLOAKING;
			if ( getArtilleryRooms() != null ) {
				for ( SystemRoom artilleryRoom : artilleryRooms ) {
					if ( artilleryRoom.getRoomId() == roomId ) return SystemBlueprint.ID_ARTILLERY;
				}
			}
			return null;
		}

		/**
		 * Returns roomId(s) that contain a given system, or null.
		 *
		 * @param name one of SystemBlueprint's ID_* constants.
		 * @return a list of roomIds, usually only containing one
		 */
		public int[] getRoomIdBySystemId( String systemId ) {
			SystemList.SystemRoom systemRoom = null;
			if ( systemId.equals(SystemBlueprint.ID_PILOT) ) systemRoom = getPilotRoom();
			else if ( systemId.equals(SystemBlueprint.ID_DOORS) ) systemRoom = getDoorsRoom();
			else if ( systemId.equals(SystemBlueprint.ID_SENSORS) ) systemRoom = getSensorsRoom();
			else if ( systemId.equals(SystemBlueprint.ID_MEDBAY) ) systemRoom = getMedicalRoom();
			else if ( systemId.equals(SystemBlueprint.ID_OXYGEN) ) systemRoom = getLifeSupportRoom();
			else if ( systemId.equals(SystemBlueprint.ID_SHIELDS) ) systemRoom = getShieldRoom();
			else if ( systemId.equals(SystemBlueprint.ID_ENGINES) ) systemRoom = getEngineRoom();
			else if ( systemId.equals(SystemBlueprint.ID_WEAPONS) ) systemRoom = getWeaponRoom();
			else if ( systemId.equals(SystemBlueprint.ID_DRONE_CTRL) ) systemRoom = getDroneRoom();
			else if ( systemId.equals(SystemBlueprint.ID_TELEPORTER) ) systemRoom = getTeleporterRoom();
			else if ( systemId.equals(SystemBlueprint.ID_CLOAKING) ) systemRoom = getCloakRoom();
			if ( systemRoom != null ) return new int[] { systemRoom.getRoomId() };

			if ( systemId.equals(SystemBlueprint.ID_ARTILLERY) ) {
				if ( getArtilleryRooms() != null && getArtilleryRooms().size() > 0 ) {
					int n = 0;
					int[] result = new int[getArtilleryRooms().size()];
					for ( SystemRoom artilleryRoom : artilleryRooms ) {
						result[n++] = artilleryRoom.getRoomId();
					}
					return result;
				}
			}

			return null;
		}
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Health {
		@XmlAttribute
		public int amount;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class MaxPower {
		@XmlAttribute
		public int amount;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public String getImg() {
		return img;
	}

	public void setImg(String img) {
		this.img = img;
	}

	public String getShipClass() {
		return shipClass;
	}

	public void setShipClass(String shipClass) {
		this.shipClass = shipClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public SystemList getSystemList() {
		return systemList;
	}

	public void setSystemList(SystemList systemList) {
		this.systemList = systemList;
	}

	public int getWeaponSlots() {
		return weaponSlots;
	}

	public void setWeaponSlots(int weaponSlots) {
		this.weaponSlots = weaponSlots;
	}

	public int getDroneSlots() {
		return droneSlots;
	}

	public void setDroneSlots(int droneSlots) {
		this.droneSlots = droneSlots;
	}

	public Object getWeaponList() {
		return weaponList;
	}

	public void setWeaponList(Object weaponList) {
		this.weaponList = weaponList;
	}

	public Health getHealth() {
		return health;
	}

	public void setHealth(Health health) {
		this.health = health;
	}

	public MaxPower getMaxPower() {
		return maxPower;
	}

	public void setMaxPower(MaxPower maxPower) {
		this.maxPower = maxPower;
	}

	public Object getCrewCount() {
		return crewCount;
	}

	public void setCrewCount(Object crewCount) {
		this.crewCount = crewCount;
	}

}
