package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
	
	private int weaponSlots, droneSlots;
	
	private Object weaponList, maxPower, crewCount; // TODO model
	
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
		 * Returns the system's name in a given room, or null.
		 */
		public String getSystemNameByRoomId( int roomId ) {
			if ( getPilotRoom() != null && getPilotRoom().getRoomId() == roomId ) return "Pilot";
			if ( getDoorsRoom() != null && getDoorsRoom().getRoomId() == roomId ) return "Doors";
			if ( getSensorsRoom() != null && getSensorsRoom().getRoomId() == roomId ) return "Sensors";
			if ( getMedicalRoom() != null && getMedicalRoom().getRoomId() == roomId ) return "Medbay";
			if ( getLifeSupportRoom() != null && getLifeSupportRoom().getRoomId() == roomId ) return "Oxygen";
			if ( getShieldRoom() != null && getShieldRoom().getRoomId() == roomId ) return "Shields";
			if ( getEngineRoom() != null && getEngineRoom().getRoomId() == roomId ) return "Engines";
			if ( getWeaponRoom() != null && getWeaponRoom().getRoomId() == roomId ) return "Weapons";
			if ( getDroneRoom() != null && getDroneRoom().getRoomId() == roomId ) return "Drone Ctrl";
			if ( getTeleporterRoom() != null && getTeleporterRoom().getRoomId() == roomId ) return "Teleporter";
			if ( getCloakRoom() != null && getCloakRoom().getRoomId() == roomId ) return "Cloaking";
			if ( getArtilleryRooms() != null ) {
				for ( SystemRoom artilleryRoom : artilleryRooms ) {
					if ( artilleryRoom.getRoomId() == roomId ) return "Artillery";
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

	public Object getMaxPower() {
		return maxPower;
	}

	public void setMaxPower(Object maxPower) {
		this.maxPower = maxPower;
	}

	public Object getCrewCount() {
		return crewCount;
	}

	public void setCrewCount(Object crewCount) {
		this.crewCount = crewCount;
	}

}
