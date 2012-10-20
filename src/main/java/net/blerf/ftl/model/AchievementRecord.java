package net.blerf.ftl.model;

import java.util.List;
import java.util.ListIterator;

import net.blerf.ftl.model.Score.Difficulty;


public class AchievementRecord {
	
	private String achievementId;
	private Difficulty difficulty;
	
	public AchievementRecord(String achievementId, Difficulty difficulty) {
		this.achievementId = achievementId;
		this.difficulty = difficulty;
	}

	public Difficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	public String getAchievementId() {
		return achievementId;
	}

	public void setAchievementId(String achievementId) {
		this.achievementId = achievementId;
	}

	
	public static boolean listContainsId(List<AchievementRecord> achList, String achievementId) {
		boolean found = false;
		for ( AchievementRecord rec : achList ) {
			if ( rec.getAchievementId().equals(achievementId) ) {
				found = true;
				break;
			}
		}
		return found;
	}

	public static void removeFromListById(List<AchievementRecord> achList, String achievementId) {
		for ( ListIterator<AchievementRecord> it = achList.listIterator(); it.hasNext(); ) {
			AchievementRecord rec = it.next();
			if ( rec.getAchievementId().equals(achievementId) ) {
				it.remove();
			}
		}
	}
}
