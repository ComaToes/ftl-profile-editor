package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "systemBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class SystemBlueprint {

	@XmlAttribute(name = "name")
	private String id;

	private String type;
	private String title;
	private String desc;
	private int startPower;  // Initial system capacity.
	private int maxPower;    // Highest possible capacity attainable by upgrading.
	private int rarity;

	@XmlElementWrapper(name = "upgradeCost")
	@XmlElement(name = "level")
	private List<Integer> upgradeCosts;

	private int cost;

	@XmlElement(required = false)
	private Integer locked;

	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setType( String type ) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setTitle( String title ) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setDescription( String desc ) {
		this.desc = desc;
	}

	public String getDescription() {
		return desc;
	}

	public void setStartPower( int startPower ) {
		this.startPower = startPower;
	}

	public int getStartPower() {
		return startPower;
	}

	public void setMaxPower( int maxPower ) {
		this.maxPower = maxPower;
	}

	public int getMaxPower() {
		return maxPower;
	}

	public void setRarity( int rarity ) {
		this.rarity = rarity;
	}

	public int getRarity() {
		return rarity;
	}

	public void setUpgradeCosts( List<Integer> upgradeCosts ) {
		this.upgradeCosts = upgradeCosts;
	}

	public List<Integer> getUpgradeCosts() {
		return upgradeCosts;
	}

	public void setCost( int cost ) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

	public void setLocked( Integer locked ) {
		this.locked = locked;
	}

	public Integer getLocked() {
		return locked;
	}
}
