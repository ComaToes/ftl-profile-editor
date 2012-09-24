package net.blerf.ftl.model;
import java.util.HashMap;


public class Achievement implements Comparable<Achievement> {
	
	// Kestrel
	public static final Achievement ACH_UNITED_FEDERATION = new Achievement("ACH_UNITED_FEDERATION", "The United Federation");
	public static final Achievement ACH_FULL_ARSENAL = new Achievement("ACH_FULL_ARSENAL", "Full Arsenal");
	public static final Achievement ACH_TOUGH_SHIP = new Achievement("ACH_TOUGH_SHIP", "Tough Little Ship");
	
	// Stealth
	public static final Achievement ACH_STEALTH_DESTROY = new Achievement("ACH_STEALTH_DESTROY", "Bird of Prey");
	public static final Achievement ACH_STEALTH_AVOID = new Achievement("ACH_STEALTH_AVOID", "Phase Shift");
	public static final Achievement ACH_STEALTH_TACTICAL = new Achievement("ACH_STEALTH_TACTICAL", "Tactical Approach");

	// Mantis
	public static final Achievement ACH_MANTIS_CREW_DEAD = new Achievement("ACH_MANTIS_CREW_DEAD", "Take no prisoners!");
	public static final Achievement ACH_MANTIS_SLAUGHTER = new Achievement("ACH_MANTIS_SLAUGHTER", "Avast, ye scurvy dogs!");
	public static final Achievement ACH_MANTIS_SURVIVOR = new Achievement("ACH_MANTIS_SURVIVOR", "Battle Royale");
	
	// Engi
	public static final Achievement ACH_ROBOTIC = new Achievement("ACH_ROBOTIC", "Robotic Warfare");
	public static final Achievement ACH_ONLY_DRONES = new Achievement("ACH_ONLY_DRONES", "I hardly lifted a finger");
	public static final Achievement ACH_IONED = new Achievement("ACH_IONED", "The guns... They've stopped");
	
	// Fed
	public static final Achievement ACH_FED_PATIENCE = new Achievement("ACH_FED_PATIENCE", "Master of Patience");
	public static final Achievement ACH_FED_DIPLOMACY = new Achievement("ACH_FED_DIPLOMACY", "Diplomatic Immunity");
	public static final Achievement ACH_FED_UPGRADE = new Achievement("ACH_FED_UPGRADE", "Artillery Mastery");

	// Slug
	public static final Achievement ACH_SLUG_VISION = new Achievement("ACH_SLUG_VISION", "We're in Position!");
	public static final Achievement ACH_SLUG_NEBULA = new Achievement("ACH_SLUG_NEBULA", "Home Sweet Home");
	public static final Achievement ACH_SLUG_BIO = new Achievement("ACH_SLUG_BIO", "Disintegration Ray");

	// Rock
	public static final Achievement ACH_ROCK_FIRE = new Achievement("ACH_ROCK_FIRE", "Is it warm in here?");
	public static final Achievement ACH_ROCK_MISSILES = new Achievement("ACH_ROCK_MISSILES", "Defense Drones Don't Do D'anything!");
	public static final Achievement ACH_ROCK_CRYSTAL = new Achievement("ACH_ROCK_CRYSTAL", "Ancestry");

	// Zoltan
	public static final Achievement ACH_ENERGY_SHIELDS = new Achievement("ACH_ENERGY_SHIELDS", "Shields Holding");
	public static final Achievement ACH_ENERGY_POWER = new Achievement("ACH_ENERGY_POWER", "Givin' her all she's got, Captain!");
	public static final Achievement ACH_ENERGY_MANPOWER = new Achievement("ACH_ENERGY_MANPOWER", "Manpower");

	// Crystal
	public static final Achievement ACH_CRYSTAL_SHARD = new Achievement("ACH_CRYSTAL_SHARD", "Sweet Revenge");
	public static final Achievement ACH_CRYSTAL_LOCKDOWN = new Achievement("ACH_CRYSTAL_LOCKDOWN", "No Escape");
	public static final Achievement ACH_CRYSTAL_CLASH = new Achievement("ACH_CRYSTAL_CLASH", "Clash of the Titans");

	// General
	//public static final Achievement ACH_SECTOR_5 = new Achievement("ACH_SECTOR_5", "Reach Sector 5");
	//public static final Achievement ACH_SECTOR_8 = new Achievement("ACH_SECTOR_8", "Reach Sector 8");
	
	private static HashMap<String,Achievement> achievements = new HashMap<String, Achievement>();
	
	static {
		achs( ACH_UNITED_FEDERATION, ACH_FULL_ARSENAL, ACH_TOUGH_SHIP,
				ACH_STEALTH_DESTROY, ACH_STEALTH_AVOID, ACH_STEALTH_TACTICAL,
				ACH_MANTIS_CREW_DEAD, ACH_MANTIS_SLAUGHTER, ACH_MANTIS_SURVIVOR,
				ACH_ROBOTIC, ACH_ONLY_DRONES, ACH_IONED,
				ACH_FED_PATIENCE, ACH_FED_DIPLOMACY, ACH_FED_UPGRADE,
				ACH_SLUG_VISION, ACH_SLUG_NEBULA, ACH_SLUG_BIO,
				ACH_ROCK_FIRE, ACH_ROCK_MISSILES, ACH_ROCK_CRYSTAL,
				ACH_ENERGY_SHIELDS, ACH_ENERGY_POWER, ACH_ENERGY_MANPOWER,
				ACH_CRYSTAL_SHARD, ACH_CRYSTAL_LOCKDOWN, ACH_CRYSTAL_CLASH
		);
		
		//achs( ACH_SECTOR_5, ACH_SECTOR_8 );
	}
	
	private static void achs(Achievement... achs) {
		for (Achievement ach: achs) {
			achievements.put( ach.getCode(), ach );
		}
	}
	
	public static Achievement get( String code ) {
		Achievement ach = achievements.get(code);
		if( ach == null )
			ach = new Achievement(code, "Unknown Achievement");
		return ach;
	}
	
	private static int maxId; // TODO super hacky way to order achievements. fix later
	private String code, name, desc;
	private int id;
	
	protected Achievement(String code, String name) {
		this.code = code;
		this.name = name;
		id = maxId++;
	}
	
	public int getId() {
		return id;
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

	@Override
	public String toString() {
		return code + ": " + name;
	}
	
	@Override
	public int compareTo(Achievement o) {
		return id > o.getId() ? 1 : -1;
	}
	
}
