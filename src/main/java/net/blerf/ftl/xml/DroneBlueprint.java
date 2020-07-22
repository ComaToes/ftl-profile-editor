package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.DefaultDeferredText;


@XmlRootElement( name = "droneBlueprint" )
@XmlAccessorType( XmlAccessType.FIELD )
public class DroneBlueprint {

	@XmlAttribute( name = "name" )
	private String id;

	private String type;

	@XmlElement( required = false )
	private Integer locked;

	private DefaultDeferredText title;

	@XmlElement( name = "short" )
	private DefaultDeferredText shortTitle;

	private DefaultDeferredText desc;

	@XmlElement( name = "bp" )
	private int bp;  // TODO: Rename this.

	@XmlElement( required = false )
	private Integer cooldown, dodge, speed;

	private int power;
	private int cost;

	@XmlElement( required = false )
	private String droneImage;

	@XmlElement( name="image", required = false )
	private String imagePath;  // InnerPath of a projectile anim sheet. Unused?

	@XmlElement( required = false )
	private String iconImage;  // TODO: FTL 1.5.4 introduced this. For iPad?

	@XmlElement( name="weaponBlueprint", required = false )
	private String weaponId;

	private int rarity;


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

	public void setLocked( Integer locked ) {
		this.locked = locked;
	}

	public Integer getLocked() {
		return locked;
	}

	public void setTitle( DefaultDeferredText title ) {
		this.title = title;
	}

	public DefaultDeferredText getTitle() {
		return title;
	}

	public void setShortTitle( DefaultDeferredText shortTitle ) {
		this.shortTitle = shortTitle;
	}

	public DefaultDeferredText getShortTitle() {
		return shortTitle;
	}

	public void setDescription( DefaultDeferredText desc ) {
		this.desc = desc;
	}

	public DefaultDeferredText getDescription() {
		return desc;
	}

	public void setBP( int bp ) {
		this.bp = bp;
	}

	public int getBP() {
		return bp;
	}

	public void setPower( int power ) {
		this.power = power;
	}

	public int getPower() {
		return power;
	}

	public void setCooldown( Integer cooldown ) {
		this.cooldown = cooldown;
	}

	public Integer getCooldown() {
		return cooldown;
	}

	public void setDodge( Integer dodge ) {
		this.dodge = dodge;
	}

	public Integer getDodge() {
		return dodge;
	}

	public void setSpeed( Integer speed ) {
		this.speed = speed;
	}

	public Integer getSpeed() {
		return speed;
	}

	public void setCost( int cost ) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

	public void setDroneImage( String droneImage ) {
		this.droneImage = droneImage;
	}

	public String getDroneImage() {
		return droneImage;
	}

	public void setImage( String innerPath ) {
		this.imagePath = innerPath;
	}

	public String getImage() {
		return imagePath;
	}

	public void setIconImage( String iconImage ) {
		this.iconImage = iconImage;
	}

	public String getIconImage() {
		return iconImage;
	}

	public void setWeaponId( String weaponId ) {
		this.weaponId = weaponId;
	}

	public String getWeaponId() {
		return weaponId;
	}

	public void setRarity( int rarity ) {
		this.rarity = rarity;
	}

	public int getRarity() {
		return cost;
	}

	@Override
	public String toString() {
		return ""+title;
	}
}
