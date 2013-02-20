package net.blerf.ftl.model;
import java.util.List;


public class Stats {

	private List<Score> topScores;
	private List<Score> shipBest;

	private int mostShipsDefeated;
	private int mostBeaconsExplored;
	private int mostScrapCollected;
	private int mostCrewHired;

	private int totalShipsDefeated;
	private int totalBeaconsExplored;
	private int totalScrapCollected;
	private int totalCrewHired;
	private int totalGamesPlayed;
	private int totalVictories;

	private CrewRecord mostRepairs;
	private CrewRecord mostKills;
	private CrewRecord mostEvasions;
	private CrewRecord mostJumps;
	private CrewRecord mostSkills;

	public void setTopScores( List<Score> topScores ) { this.topScores = topScores; }
	public void setShipBest( List<Score> shipBest ) { this.shipBest = shipBest; }

	public List<Score> getTopScores() { return topScores; }
	public List<Score> getShipBest() { return shipBest; }

	public void setMostShipsDefeated( int n ) { mostShipsDefeated = n; }
	public void setMostBeaconsExplored( int n ) { mostBeaconsExplored = n; }
	public void setMostScrapCollected( int n ) { mostScrapCollected = n; }
	public void setMostCrewHired( int n ) { mostCrewHired = n; }

	public int getMostShipsDefeated() { return mostShipsDefeated; }
	public int getMostBeaconsExplored() { return mostBeaconsExplored; }
	public int getMostScrapCollected() { return mostScrapCollected; }
	public int getMostCrewHired() { return mostCrewHired; }

	public void setTotalShipsDefeated( int n ) { totalShipsDefeated = n; }
	public void setTotalBeaconsExplored( int n ) { totalBeaconsExplored = n; }
	public void setTotalScrapCollected( int n ) { totalScrapCollected = n; }
	public void setTotalCrewHired( int n ) { totalCrewHired = n; }
	public void setTotalGamesPlayed( int n ) { totalGamesPlayed = n; }
	public void setTotalVictories( int n ) { totalVictories = n; }

	public int getTotalShipsDefeated() { return totalShipsDefeated; }
	public int getTotalBeaconsExplored() { return totalBeaconsExplored; }
	public int getTotalScrapCollected() { return totalScrapCollected; }
	public int getTotalCrewHired() { return totalCrewHired; }
	public int getTotalGamesPlayed() { return totalGamesPlayed; }
	public int getTotalVictories() { return totalVictories; }

	public void setMostRepairs( CrewRecord record ) { mostRepairs = record; }
	public void setMostKills( CrewRecord record ) { mostKills = record; }
	public void setMostEvasions( CrewRecord record ) { mostEvasions = record; }
	public void setMostJumps( CrewRecord record ) { mostJumps = record; }
	public void setMostSkills( CrewRecord record ) { mostSkills = record; }

	public CrewRecord getMostRepairs() { return mostRepairs; }
	public CrewRecord getMostKills() { return mostKills; }
	public CrewRecord getMostEvasions() { return mostEvasions; }
	public CrewRecord getMostJumps() { return mostJumps; }
	public CrewRecord getMostSkills() { return mostSkills; }
}
