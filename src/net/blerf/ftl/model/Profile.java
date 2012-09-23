package net.blerf.ftl.model;
import java.util.List;


public class Profile {

	private int version;
	private List<Achievement> achievements;
	private boolean[] shipUnlocks;
	private Stats stats;
	
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public List<Achievement> getAchievements() {
		return achievements;
	}
	public void setAchievements(List<Achievement> achievements) {
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
	
}
