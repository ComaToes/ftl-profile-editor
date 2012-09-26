package net.blerf.ftl.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.blerf.ftl.model.Achievement;
import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Stats;

public class ProfileParser extends Parser {
	
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
		
		writeInt(out, p.getVersion());
		
		writeAchievements(out, p.getAchievements());
		
		writeShipUnlocks(out, p.getShipUnlocks());
		
		writeStats(out, p.getStats());
		
	}
	
	private List<Achievement> readAchievements(InputStream in) throws IOException {
		
		int achievementCount = readInt(in);
		
		List<Achievement> achievements = new ArrayList<Achievement>(achievementCount);
		
		for (int i = 0; i < achievementCount; i++) {
			String ach = readString(in);
			readInt(in); // 4 byte tailer always 0
			achievements.add( Achievement.get(ach) );
		}
		
		return achievements;
		
	}
	
	private void writeAchievements(OutputStream out, List<Achievement> achievements) throws IOException {
		
		writeInt(out, achievements.size());
		
		for (Achievement achievement : achievements) {
			writeString(out, achievement.getCode());
			writeInt(out, 0);
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
			writeInt(out, unlocks[i] ? 1 : 0);
		}
		
	}
	
	private Stats readStats(InputStream in) throws IOException {
		
		Stats stats = new Stats();
		
		// Top Scores
		stats.setTopScores(readScoreList(in));
		stats.setShipBest(readScoreList(in));
		
		// Stats
		stats.setMostShipsDefeated(readInt(in));
		stats.setTotalShipsDefeated(readInt(in));
		stats.setMostBeaconsExplored(readInt(in));
		stats.setTotalBeaconsExplored(readInt(in));
		stats.setMostScrapCollected(readInt(in));
		stats.setTotalScrapCollected(readInt(in));
		stats.setMostCrewHired(readInt(in));
		stats.setTotalCrewHired(readInt(in));
		stats.setTotalGamesPlayed(readInt(in));
		stats.setTotalVictories(readInt(in));
		
		// Crew
		stats.setMostRepairs(readCrewRecord(in));
		stats.setMostKills(readCrewRecord(in));
		stats.setMostEvasions(readCrewRecord(in));
		stats.setMostJumps(readCrewRecord(in));
		stats.setMostSkills(readCrewRecord(in));
		
		return stats;
		
	}
	
	private void writeStats(OutputStream out, Stats stats) throws IOException {
		
		writeScoreList(out, stats.getTopScores());
		writeScoreList(out, stats.getShipBest());
		
		writeInt(out, stats.getMostShipsDefeated());
		writeInt(out, stats.getTotalShipsDefeated());
		writeInt(out, stats.getMostBeaconsExplored());
		writeInt(out, stats.getTotalBeaconsExplored());
		writeInt(out, stats.getMostScrapCollected());
		writeInt(out, stats.getTotalScrapCollected());
		writeInt(out, stats.getMostCrewHired());
		writeInt(out, stats.getTotalCrewHired());
		writeInt(out, stats.getTotalGamesPlayed());
		writeInt(out, stats.getTotalVictories());
		
		writeCrewRecord(out, stats.getMostRepairs());
		writeCrewRecord(out, stats.getMostKills());
		writeCrewRecord(out, stats.getMostEvasions());
		writeCrewRecord(out, stats.getMostJumps());
		writeCrewRecord(out, stats.getMostSkills());
		
	}
	
	private CrewRecord readCrewRecord(InputStream in) throws IOException {
		
		int score = readInt(in);
		String name = readString(in);
		String race = readString(in);
		int unknownFlag = readInt(in); // Unknown always 1 or 0. Died?
		if( unknownFlag != 1 && unknownFlag != 0 )
			throw new RuntimeException();
		
		return new CrewRecord(name, race, score, unknownFlag);
		
	}
	
	private void writeCrewRecord(OutputStream out, CrewRecord rec) throws IOException {
		
		writeInt(out, rec.getScore());
		writeString(out, rec.getName());
		writeString(out, rec.getRace());
		writeInt(out, rec.getUnknownFlag());
		
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

			scores.add( new Score(shipName, shipType, score, sector, difficulty, victory) );
			
		}
		
		return scores;
		
	}
	
	private void writeScoreList(OutputStream out, List<Score> scores) throws IOException {
		
		writeInt(out, scores.size());
		
		for (Score score : scores) {
			writeString(out, score.getShipName());
			writeString(out, score.getShipType());
			writeInt(out, score.getScore());
			writeInt(out, score.getSector());
			writeInt(out, score.isVictory() ? 1 : 0 );
			writeInt(out, score.getDifficulty());
		}
		
	}
	

	
}
