package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="weaponBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class WeaponBlueprint {
	
	private String type, title;
	@XmlElement(name="short")
	private String shortTitle;
	private String desc, tooltip;
	private int damage, shots, sp, fireChance, breachChance, cooldown, power, cost, bp, rarity;
	private String image;
	private SoundList launchSounds, hitShipSounds, hitShieldSounds, missSounds;
	private String weaponArt;
	
	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SoundList {
		private List<String> sound;
	}

}
