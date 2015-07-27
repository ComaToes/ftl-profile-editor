package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name="droneBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class DroneBlueprint {

	@XmlAttribute(name="name")
	private String id;

	private String type;

	@XmlElement(required=false)
	private int locked;

	private String title;

	@XmlElement(name="short")
	private String shortTitle;

	private String desc;

	@XmlElement(name="bp")
	private int bp;  // TODO: Rename this.

	@XmlElement(required=false)
	private int cooldown, dodge, speed;

	private int power;
	private int cost;

	@XmlElement(required=false)
	private String droneImage;

	@XmlElement(name="image",required=false)
	private String imagePath;  // InnerPath of a projectile anim sheet. Unused?

	@XmlElement(required=false)
	private String iconImage;  // TODO: FTL 1.5.4 introduced this. For iPad?

	@XmlElement(name="weaponBlueprint",required=false)
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

	public void setLocked( int locked ) {
		this.locked = locked;
	}

	public int getLocked() {
		return locked;
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

	public void setDescription( String desc ) {
		this.desc = desc;
	}

	public String getDescription() {
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

	public void setCooldown( int cooldown ) {
		this.cooldown = cooldown;
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setDodge( int dodge ) {
		this.dodge = dodge;
	}

	public int getDodge() {
		return dodge;
	}

	public void setSpeed( int speed ) {
		this.speed = speed;
	}

	public int getSpeed() {
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
