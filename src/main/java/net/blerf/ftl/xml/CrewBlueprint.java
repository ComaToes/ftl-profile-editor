package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="crewBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class CrewBlueprint {

	public static final String RACE_BATTLE = "battle";
	public static final String RACE_CRYSTAL = "crystal";
	public static final String RACE_ENERGY = "energy";
	public static final String RACE_ENGI = "engi";
	public static final String RACE_GHOST = "ghost";
	public static final String RACE_HUMAN = "human";
	public static final String RACE_MANTIS = "mantis";
	public static final String RACE_ROCK = "rock";
	public static final String RACE_SLUG = "slug";
	
	@XmlAttribute(name="name")
	private String id;
	private String desc;
	private int cost;
	@XmlElement(name="bp")
	private int bp;  // TODO: Rename this.
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
