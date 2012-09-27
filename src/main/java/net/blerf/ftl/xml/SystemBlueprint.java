package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="systemBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemBlueprint {
	
	private String type, title, desc;
	private int startPower, maxPower, rarity;
	private UpgradeCost upgradeCost;
	private int cost;
	@XmlElement(required=false)
	private int locked;
	
	@XmlRootElement(name="upgradeCost")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class UpgradeCost {
		private List<Integer> level;
	}

}
