package net.blerf.ftl.model;

import java.util.ArrayList;
import java.util.List;

import net.blerf.ftl.model.Stats.StatType;


public class Profile {

	private int version;
	private List<AchievementRecord> achievements;
	private boolean[] shipUnlocks;
	private Stats stats;

	public void setVersion( int version ) {
		this.version = version;
	}
	public int getVersion() {
		return version;
	}

	public void setAchievements( List<AchievementRecord> achievements ) {
		this.achievements = achievements;
	}
	public List<AchievementRecord> getAchievements() {
		return achievements;
	}

	public void setStats( Stats stats ) {
		this.stats = stats;
	}
	public Stats getStats() {
		return stats;
	}

	public void setShipUnlocks( boolean[] shipUnlocks ) {
		this.shipUnlocks = shipUnlocks;
	}
	public boolean[] getShipUnlocks() {
		return shipUnlocks;
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

		StatType[] intStatTypes = {StatType.MOST_SHIPS_DEFEATED, StatType.MOST_BEACONS_EXPLORED,
		                           StatType.MOST_SCRAP_COLLECTED, StatType.MOST_CREW_HIRED,
		                           StatType.TOTAL_SHIPS_DEFEATED, StatType.TOTAL_BEACONS_EXPLORED,
		                           StatType.TOTAL_SCRAP_COLLECTED, StatType.TOTAL_CREW_HIRED,
                               StatType.TOTAL_GAMES_PLAYED, StatType.TOTAL_VICTORIES};
		StatType[] crewStatTypes = {StatType.MOST_REPAIRS, StatType.MOST_COMBAT_KILLS,
		                            StatType.MOST_PILOTED_EVASIONS, StatType.MOST_JUMPS_SURVIVED,
		                            StatType.MOST_SKILL_MASTERIES};
		for ( StatType type : intStatTypes ) {
			stats.setIntRecord( type, 0 );
		}
		for ( StatType type : crewStatTypes ) {
			stats.setCrewRecord( type, new CrewRecord("", "", true, 0) );
		}
		profile.setStats( stats );

		return profile;
	}
}
