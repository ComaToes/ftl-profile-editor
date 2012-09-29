package net.blerf.ftl.model;

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

	
	
}
