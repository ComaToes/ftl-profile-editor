package net.blerf.ftl.model;

import java.util.List;
import java.util.ListIterator;

import net.blerf.ftl.model.Score.Difficulty;


public class AchievementRecord {

	private String achievementId;
	private Difficulty difficulty;

	private Difficulty typeADiff = null;
	private Difficulty typeBDiff = null;
	private Difficulty typeCDiff = null;


	public AchievementRecord( String achievementId, Difficulty difficulty ) {
		this.achievementId = achievementId;
		this.difficulty = difficulty;
	}

	public void setDifficulty( Difficulty d ) { difficulty = d; }
	public void setAchievementId( String s ) { achievementId = s; }

	public Difficulty getDifficulty() { return difficulty; }
	public String getAchievementId() { return achievementId; }

	/**
	 * Sets whether a ship's Type-A layout completed this achievement.
	 *
	 * The game will synchronize the difficulty of the achievement and all
	 * layout types with the highest one among them.
	 *
	 * This manifests in-game in the ship list's "V" with three dots.
	 *
	 * Note: This is only used by PLAYER_SHIP_*_VICTORY achievements.
	 * This was introduced in FTL 1.5.4.
	 *
	 * @param d difficulty for that run, or null (0=EASY, 1=NORMAL, 2=HARD, -1=N/A)
	 */
	public void setCompletedWithTypeA( Difficulty d ) { typeADiff = d; }
	public void setCompletedWithTypeB( Difficulty d ) { typeBDiff = d; }
	public void setCompletedWithTypeC( Difficulty d ) { typeCDiff = d; }
	public Difficulty getCompletedWithTypeA() { return typeADiff; }
	public Difficulty getCompletedWithTypeB() { return typeBDiff; }
	public Difficulty getCompletedWithTypeC() { return typeCDiff; }

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		String typeADiffString = (typeADiff != null ? typeADiff.toString() : "N/A");
		String typeBDiffString = (typeBDiff != null ? typeBDiff.toString() : "N/A");
		String typeCDiffString = (typeCDiff != null ? typeCDiff.toString() : "N/A");

		result.append(String.format("AchId: %-30s  Difficulty: %-6s\n", achievementId, difficulty.toString()));

		result.append(String.format("With TypeA: %-6s  With TypeB: %-6s  With TypeC: %-6s\n", typeADiffString, typeBDiffString, typeBDiffString));

		return result.toString();
	}


	public static AchievementRecord getFromListById( List<AchievementRecord> achList, String achievementId ) {
		for ( AchievementRecord rec : achList ) {
			if ( rec.getAchievementId().equals(achievementId) ) {
				return rec;
			}
		}
		return null;
	}

	public static void removeFromListById( List<AchievementRecord> achList, String achievementId ) {
		for ( ListIterator<AchievementRecord> it = achList.listIterator(); it.hasNext(); ) {
			AchievementRecord rec = it.next();
			if ( rec.getAchievementId().equals(achievementId) ) {
				it.remove();
			}
		}
	}
}
