package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "weaponBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class WeaponBlueprint {
	
	@XmlAttribute(name = "name")
	private String id;

	private String type;
	private String title;

	@XmlElement(name = "short")
	private String shortTitle;

	@XmlElement(required = false)
	private Integer locked;

	private String desc;
	private String tooltip;

	@XmlElement(name = "sp")
	private int shieldPiercing;

	@XmlElement(name = "bp")
	private int bp;  // TODO: Rename this.

	private int damage;
	private int shots;
	private int fireChance;
	private int breachChance;
	private int cooldown;
	private int power;
	private int cost;
	private int rarity;

	@XmlElement(name = "image")
	private String projectileAnimId;  // Projectile / Beam-spot anim.

	@XmlElementWrapper(name = "launchSounds")
	@XmlElement(name = "sound")
	private List<String> launchSounds;

	@XmlElementWrapper(name = "hitShipSounds")
	@XmlElement(name = "sound")
	private List<String> hitShipSounds;

	@XmlElementWrapper(name = "hitShieldSounds")
	@XmlElement(name = "sound")
	private List<String> hitShieldSounds;

	@XmlElementWrapper(name = "missSounds")
	@XmlElement(name = "sound")
	private List<String> missSounds;

	@XmlElement(name = "weaponArt")
	private String weaponAnimId;
	
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

	public void setShortTitle( String shortTitle ) {
		this.shortTitle = shortTitle;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setLocked( Integer locked ) {
		this.locked = locked;
	}

	public Integer getLocked() {
		return locked;
	}

	public void setDescription( String desc ) {
		this.desc = desc;
	}

	public String getDescription() {
		return desc;
	}

	public void setTooltip( String tooltip ) {
		this.tooltip = tooltip;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setShieldPiercing( int shieldPiercing ) {
		this.shieldPiercing = shieldPiercing;
	}

	public int getShieldPiercing() {
		return shieldPiercing;
	}

	public void setBP( int bp ) {
		this.bp = bp;
	}

	public int getBP() {
		return bp;
	}

	public void setDamage( int damage ) {
		this.damage = damage;
	}

	public int getDamage() {
		return damage;
	}

	public void setShots( int shots ) {
		this.shots = shots;
	}

	public int getShots() {
		return shots;
	}

	public void setFireChance( int fireChance ) {
		this.fireChance = fireChance;
	}

	public int getFireChance() {
		return fireChance;
	}

	public void setBreachChance( int breachChance ) {
		this.breachChance = breachChance;
	}

	public int getBreachChance() {
		return breachChance;
	}

	public void setCooldown( int cooldown ) {
		this.cooldown = cooldown;
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setPower( int power ) {
		this.power = power;
	}

	public int getPower() {
		return power;
	}

	public void setCost( int cost ) {
		this.cost = cost;
	}

	public int getCost() {
		return cost;
	}

	public void setRarity( int rarity ) {
		this.rarity = rarity;
	}

	public int getRarity() {
		return cost;
	}

	public void setProjectileAnimId( String projectileAnimId ) {
		this.projectileAnimId = projectileAnimId;
	}

	public String getProjectileAnimId() {
		return projectileAnimId;
	}

	public void setLaunchSounds( List<String> launchSounds ) {
		this.launchSounds = launchSounds;
	}

	public List<String> getLaunchSounds() {
		return launchSounds;
	}

	public void setHitShipSounds( List<String> hitShipSounds ) {
		this.hitShipSounds = hitShipSounds;
	}

	public List<String> getHitShipSounds() {
		return hitShipSounds;
	}

	public void setHitShieldSounds( List<String> hitShieldSounds ) {
		this.hitShieldSounds = hitShieldSounds;
	}

	public List<String> getHitShieldSounds() {
		return hitShieldSounds;
	}

	public void setMissSounds( List<String> missSounds ) {
		this.missSounds = missSounds;
	}

	public List<String> getMissSounds() {
		return missSounds;
	}

	public void setWeaponAnimId( String weaponAnimId ) {
		this.weaponAnimId = weaponAnimId;
	}

	public String getWeaponAnimId() {
		return weaponAnimId;
	}

	@Override
	public String toString() {
		return ""+title;
	}
}
