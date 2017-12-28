package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "crewBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class CrewBlueprint {

	@XmlAttribute(name = "name")
	private String id;

	private String desc;
	private int cost;

	@XmlElement(name = "bp")
	private int bp;  // TODO: Rename this.

	private String title;

	@XmlElement(name = "short")
	private String shortTitle;

	private int rarity;

	@XmlElementWrapper(name = "powerList")
	@XmlElement(name = "power")
	private List<String> powerList;

	@XmlElementWrapper(name = "colorList", required = false)
	@XmlElement(name = "layer", required = false)
	private List<SpriteTintLayer> spriteTintLayerList;  // FTL 1.5.4 introduced sprite tinting.

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SpriteTintLayer {

		@XmlElement(name="color")
		public List<SpriteTintColor> tintList;

		@XmlAccessorType(XmlAccessType.FIELD)
		public static class SpriteTintColor {
			@XmlAttribute
			public int r, g, b;
			@XmlAttribute
			public float a;
		}
	}

	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setDescription( String desc ) {
		this.desc = desc;
	}

	public String getDescription() {
		return desc;
	}

	public void setCost( int cost ) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

	public void setBP( int bp ) {
		this.bp = bp;
	}

	public int getBP() {
		return bp;
	}

	public void setTitle( String title ) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setShortTitle( String shortTitle ) {
		this.shortTitle = shortTitle;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setRarity( int rarity ) {
		this.rarity = rarity;
	}

	public int getRarity() {
		return rarity;
	}

	public void setPowerList( List<String> powerList ) {
		this.powerList = powerList;
	}

	public List<String> getPowerList() {
		return powerList;
	}

	public void setSpriteTintLayerList( List<SpriteTintLayer> layerList ) {
		spriteTintLayerList = layerList;
	}

	public List<SpriteTintLayer> getSpriteTintLayerList() {
		return spriteTintLayerList;
	}
}
