package net.blerf.ftl.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.Stats;
import net.blerf.ftl.model.Stats.StatType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ProfileParser extends Parser {

	private static final Logger log = LogManager.getLogger(ProfileParser.class);

	public Profile readProfile(InputStream in) throws IOException {
		Profile p = new Profile();

		// Presumed version header
		int version = readInt(in);
		if( version != 4 )
			throw new RuntimeException("Initial int (assumed to be file format version) not expected value: " + version);
		p.setVersion(4);

		p.setAchievements( readAchievements(in) );

		p.setShipUnlocks( readShipUnlocks(in) );

		p.setStats( readStats(in) );

		return p;
	}

	public void writeProfile(OutputStream out, Profile p) throws IOException {
		writeInt( out, p.getVersion() );

		writeAchievements( out, p.getAchievements() );

		writeShipUnlocks( out, p.getShipUnlocks() );

		writeStats( out, p.getStats() );
	}

	private List<AchievementRecord> readAchievements(InputStream in) throws IOException {
		int achievementCount = readInt(in);

		List<AchievementRecord> achievements = new ArrayList<AchievementRecord>(achievementCount);

		for (int i = 0; i < achievementCount; i++) {
			String achName = readString(in);
			int diffFlag = readInt(in);
			Difficulty diff;
			switch( diffFlag ) {
				case 1: diff = Difficulty.NORMAL; break;
				default: diff = Difficulty.EASY;
			}
			achievements.add( new AchievementRecord( achName , diff ) );
		}

		return achievements;
	}

	private void writeAchievements(OutputStream out, List<AchievementRecord> achievements) throws IOException {
		writeInt(out, achievements.size());

		for (AchievementRecord rec : achievements) {
			writeString(out, rec.getAchievementId());
			int diff = 0;
			switch(rec.getDifficulty()) {
				case NORMAL: diff = 1; break;
				case EASY: diff = 0; break;
			}
			writeInt(out, diff);
		}
	}

	private boolean[] readShipUnlocks(InputStream in) throws IOException {
		boolean[] unlocks = new boolean[12];

		for (int i = 0; i < unlocks.length; i++) {
			unlocks[i] = readInt(in) == 1;
		}

		return unlocks;
	}

	private void writeShipUnlocks(OutputStream out, boolean[] unlocks) throws IOException {
		for (int i = 0; i < unlocks.length; i++) {
			writeInt( out, (unlocks[i] ? 1 : 0) );
		}
	}

	private Stats readStats(InputStream in) throws IOException {
		Stats stats = new Stats();

		// Top Scores
		stats.setTopScores( readScoreList(in) );
		stats.setShipBest( readScoreList(in) );

		// Stats
		stats.setIntRecord( StatType.MOST_SHIPS_DEFEATED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_SHIPS_DEFEATED, readInt(in) );
		stats.setIntRecord( StatType.MOST_BEACONS_EXPLORED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_BEACONS_EXPLORED, readInt(in) );
		stats.setIntRecord( StatType.MOST_SCRAP_COLLECTED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_SCRAP_COLLECTED, readInt(in) );
		stats.setIntRecord( StatType.MOST_CREW_HIRED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_CREW_HIRED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_GAMES_PLAYED, readInt(in) );
		stats.setIntRecord( StatType.TOTAL_VICTORIES, readInt(in) );

		stats.setCrewRecord( StatType.MOST_REPAIRS, readCrewRecord(in) );
		stats.setCrewRecord( StatType.MOST_COMBAT_KILLS, readCrewRecord(in) );
		stats.setCrewRecord( StatType.MOST_PILOTED_EVASIONS, readCrewRecord(in) );
		stats.setCrewRecord( StatType.MOST_JUMPS_SURVIVED, readCrewRecord(in) );
		stats.setCrewRecord( StatType.MOST_SKILL_MASTERIES, readCrewRecord(in) );

		return stats;
	}

	private void writeStats(OutputStream out, Stats stats) throws IOException {
		writeScoreList( out, stats.getTopScores() );
		writeScoreList( out, stats.getShipBest() );

		writeInt( out, stats.getIntRecord( StatType.MOST_SHIPS_DEFEATED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_SHIPS_DEFEATED ) );
		writeInt( out, stats.getIntRecord( StatType.MOST_BEACONS_EXPLORED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_BEACONS_EXPLORED ) );
		writeInt( out, stats.getIntRecord( StatType.MOST_SCRAP_COLLECTED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_SCRAP_COLLECTED ) );
		writeInt( out, stats.getIntRecord( StatType.MOST_CREW_HIRED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_CREW_HIRED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_GAMES_PLAYED ) );
		writeInt( out, stats.getIntRecord( StatType.TOTAL_VICTORIES ) );

		writeCrewRecord( out, stats.getCrewRecord( StatType.MOST_REPAIRS ) );
		writeCrewRecord( out, stats.getCrewRecord( StatType.MOST_COMBAT_KILLS ) );
		writeCrewRecord( out, stats.getCrewRecord( StatType.MOST_PILOTED_EVASIONS ) );
		writeCrewRecord( out, stats.getCrewRecord( StatType.MOST_JUMPS_SURVIVED ) );
		writeCrewRecord( out, stats.getCrewRecord( StatType.MOST_SKILL_MASTERIES ) );
	}

	private CrewRecord readCrewRecord(InputStream in) throws IOException {
		int score = readInt(in);
		String name = readString(in);
		String race = readString(in);
		boolean male = readBool(in);

		return new CrewRecord(name, race, male, score);
	}

	private void writeCrewRecord(OutputStream out, CrewRecord rec) throws IOException {
		writeInt( out, rec.getScore() );
		writeString( out, rec.getName() );
		writeString( out, rec.getRace() );
		writeBool( out, rec.isMale() );
	}

	private List<Score> readScoreList(InputStream in) throws IOException {
		int scoreCount = readInt(in);

		List<Score> scores = new ArrayList<Score>(scoreCount);

		for (int i = 0; i < scoreCount; i++) {
			String shipName = readString(in);
			String shipType = readString(in);
			int score = readInt(in);
			int sector = readInt(in);
			boolean victory = readInt(in) == 1;
			int difficulty = readInt(in); // Difficulty 0=normal, 1=easy

			Difficulty diff;
			switch( difficulty ) {
				case 0: diff = Difficulty.NORMAL; break;
				case 1: diff = Difficulty.EASY; break;
				default: throw new IOException("Invalid difficulty value: "+ difficulty);
			}

			scores.add( new Score(shipName, shipType, score, sector, diff, victory) );
		}

		return scores;
	}

	private void writeScoreList(OutputStream out, List<Score> scores) throws IOException {
		writeInt(out, scores.size());

		for (Score score : scores) {
			writeString( out, score.getShipName() );
			writeString( out, score.getShipType() );
			writeInt( out, score.getScore() );
			writeInt( out, score.getSector() );
			writeInt( out, (score.isVictory() ? 1 : 0) );
			switch( score.getDifficulty() ) {
				case NORMAL: writeInt( out, 0 ); break;
				case EASY: writeInt( out, 1 ); break;
				default: throw new IOException("Invalid difficulty value: "+ score.getDifficulty());
			}
		}
	}
}
