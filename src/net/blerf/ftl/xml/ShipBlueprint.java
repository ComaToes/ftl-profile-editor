package net.blerf.ftl.xml;

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
	
	private Object systemList; // TODO model
	
	private int weaponSlots, droneSlots;
	
	private Object weaponList, health, maxPower, crewCount; // TODO model
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public class SystemList {
		
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

	public Object getSystemList() {
		return systemList;
	}

	public void setSystemList(Object systemList) {
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

	public Object getHealth() {
		return health;
	}

	public void setHealth(Object health) {
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
