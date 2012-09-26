package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="crewBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class CrewBlueprint {
	
	private String desc;
	private int cost, bp;
	private String title;
	@XmlElement(name="short")
	private String shortTitle;
	private int rarity;
	private PowerList powerList; 
	
	@XmlRootElement(name="powerList")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class PowerList {
		private List<String> power;
	}

}
