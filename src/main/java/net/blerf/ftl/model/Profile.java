package net.blerf.ftl.model;

import java.util.ArrayList;
import java.util.List;


public class Profile {

	private int version;
	private List<AchievementRecord> achievements;
	private boolean[] shipUnlocks;
	private Stats stats;
	
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public List<AchievementRecord> getAchievements() {
		return achievements;
	}
	public void setAchievements(List<AchievementRecord> achievements) {
		this.achievements = achievements;
	}
	public Stats getStats() {
		return stats;
	}
	public void setStats(Stats stats) {
		this.stats = stats;
	}
	public boolean[] getShipUnlocks() {
		return shipUnlocks;
	}
	public void setShipUnlocks(boolean[] shipUnlocks) {
		this.shipUnlocks = shipUnlocks;
	}
	

	public static Profile createEmptyProfile() {
		Profile profile = new Profile();
		profile.setVersion(4);
		boolean[] emptyUnlocks = new boolean[12];  // TODO: magic number.
		emptyUnlocks[0] = true;  // Kestrel starts unlocked.
		profile.setShipUnlocks( emptyUnlocks );
		profile.setAchievements( new ArrayList<AchievementRecord>() );
		Stats stats = new Stats();
		stats.setTopScores( new ArrayList<Score>() );
		stats.setShipBest( new ArrayList<Score>() );
		stats.setMostEvasions( new CrewRecord("", "", 0, 0) );
		stats.setMostJumps( new CrewRecord("", "", 0, 0) );
		stats.setMostKills( new CrewRecord("", "", 0, 0) );
		stats.setMostRepairs( new CrewRecord("", "", 0, 0) );
		stats.setMostSkills( new CrewRecord("", "", 0, 0) );
		profile.setStats( stats );

		return profile;
	}
}
