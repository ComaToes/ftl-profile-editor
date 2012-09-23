package net.blerf.ftl.model;

import static net.blerf.ftl.model.Achievement.*;

import java.util.ArrayList;
import java.util.List;

public class Ship {
	
	public static Ship KESTREL = new Ship("PLAYER_SHIP_HARD", "Kestrel Cruiser", ACH_UNITED_FEDERATION, ACH_FULL_ARSENAL, ACH_TOUGH_SHIP);
	public static Ship STEALTH = new Ship("PLAYER_SHIP_STEALTH", "Stealth Cruiser", ACH_STEALTH_DESTROY, ACH_STEALTH_AVOID, ACH_STEALTH_TACTICAL);
	public static Ship MANTIS = new Ship("PLAYER_SHIP_MANTIS", "Mantis Cruiser", ACH_MANTIS_CREW_DEAD, ACH_MANTIS_SLAUGHTER, ACH_MANTIS_SURVIVOR);
	public static Ship ENGI = new Ship("PLAYER_SHIP_CIRCLE", "Engi Cruiser", ACH_ROBOTIC, ACH_ONLY_DRONES, ACH_IONED);
	public static Ship FEDERATION = new Ship("PLAYER_SHIP_FED", "Federation Cruiser", ACH_FED_PATIENCE, ACH_FED_DIPLOMACY, ACH_FED_UPGRADE);
	public static Ship SLUG = new Ship("PLAYER_SHIP_JELLY", "Slug Cruiser", ACH_SLUG_VISION, ACH_SLUG_NEBULA, ACH_SLUG_BIO);
	public static Ship ROCK = new Ship("PLAYER_SHIP_ROCK", "Rock Cruiser", ACH_ROCK_FIRE, ACH_ROCK_MISSILES, ACH_ROCK_CRYSTAL);
	public static Ship ZOLTAN = new Ship("PLAYER_SHIP_ENERGY", "Zoltan Cruiser", ACH_ENERGY_SHIELDS, ACH_ENERGY_POWER, ACH_ENERGY_MANPOWER);
	public static Ship CRYSTAL = new Ship("PLAYER_SHIP_CRYSTAL", "Crystal Cruiser", ACH_CRYSTAL_SHARD, ACH_CRYSTAL_LOCKDOWN, ACH_CRYSTAL_CLASH);
	
	public static Ship[] ALL = new Ship[] { KESTREL, STEALTH, MANTIS, ENGI, FEDERATION, SLUG, ROCK, ZOLTAN, CRYSTAL };
	
	private String code, name;
	private List<Achievement> achievements;
	
	public Ship(String code, String name, Achievement... achievements) {
		this.code = code;
		this.name = name;
		this.achievements = new ArrayList<Achievement>(achievements.length);
		for (Achievement achievement : achievements) {
			this.achievements.add( achievement );
		}
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Achievement> getAchievements() {
		return achievements;
	}

	public void setAchievements(List<Achievement> achievements) {
		this.achievements = achievements;
	}
	

}
