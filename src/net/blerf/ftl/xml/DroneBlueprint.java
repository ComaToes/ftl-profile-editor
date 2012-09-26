package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="droneBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class DroneBlueprint {
	
	private String type, title;
	@XmlElement(name="short")
	private String shortTitle;
	private String desc;
	private int power, cooldown, dodge, speed, cost, bp;
	private String droneImage, weaponBlueprint;
	private int rarity;

}
