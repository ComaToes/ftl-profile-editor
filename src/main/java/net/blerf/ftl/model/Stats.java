package net.blerf.ftl.model;
import java.util.List;


public class Stats {

	private List<Score> topScores;
	private List<Score> shipBest;
	
	private int mostShipsDefeated;
	private int totalShipsDefeated;
	private int mostBeaconsExplored;
	private int totalBeaconsExplored;
	private int mostScrapCollected;
	private int totalScrapCollected;
	private int mostCrewHired;
	private int totalCrewHired;
	private int totalGamesPlayed;
	private int totalVictories;
	
	private CrewRecord mostRepairs;
	private CrewRecord mostKills;
	private CrewRecord mostEvasions;
	private CrewRecord mostJumps;
	private CrewRecord mostSkills;
	
	public List<Score> getTopScores() {
		return topScores;
	}
	public void setTopScores(List<Score> topScores) {
		this.topScores = topScores;
	}
	public List<Score> getShipBest() {
		return shipBest;
	}
	public void setShipBest(List<Score> shipBest) {
		this.shipBest = shipBest;
	}
	public int getMostShipsDefeated() {
		return mostShipsDefeated;
	}
	public void setMostShipsDefeated(int mostShipsDefeated) {
		this.mostShipsDefeated = mostShipsDefeated;
	}
	public int getTotalShipsDefeated() {
		return totalShipsDefeated;
	}
	public void setTotalShipsDefeated(int totalShipsDefeated) {
		this.totalShipsDefeated = totalShipsDefeated;
	}
	public int getMostBeaconsExplored() {
		return mostBeaconsExplored;
	}
	public void setMostBeaconsExplored(int mostBeaconsExplored) {
		this.mostBeaconsExplored = mostBeaconsExplored;
	}
	public int getTotalBeaconsExplored() {
		return totalBeaconsExplored;
	}
	public void setTotalBeaconsExplored(int totalBeaconsExplored) {
		this.totalBeaconsExplored = totalBeaconsExplored;
	}
	public int getMostScrapCollected() {
		return mostScrapCollected;
	}
	public void setMostScrapCollected(int mostScrapCollected) {
		this.mostScrapCollected = mostScrapCollected;
	}
	public int getTotalScrapCollected() {
		return totalScrapCollected;
	}
	public void setTotalScrapCollected(int totalScrapCollected) {
		this.totalScrapCollected = totalScrapCollected;
	}
	public int getMostCrewHired() {
		return mostCrewHired;
	}
	public void setMostCrewHired(int mostCrewHired) {
		this.mostCrewHired = mostCrewHired;
	}
	public int getTotalCrewHired() {
		return totalCrewHired;
	}
	public void setTotalCrewHired(int totalCrewHired) {
		this.totalCrewHired = totalCrewHired;
	}
	public int getTotalGamesPlayed() {
		return totalGamesPlayed;
	}
	public void setTotalGamesPlayed(int totalGamesPlayed) {
		this.totalGamesPlayed = totalGamesPlayed;
	}
	public int getTotalVictories() {
		return totalVictories;
	}
	public void setTotalVictories(int totalVictories) {
		this.totalVictories = totalVictories;
	}
	public CrewRecord getMostRepairs() {
		return mostRepairs;
	}
	public void setMostRepairs(CrewRecord mostRepairs) {
		this.mostRepairs = mostRepairs;
	}
	public CrewRecord getMostKills() {
		return mostKills;
	}
	public void setMostKills(CrewRecord mostKills) {
		this.mostKills = mostKills;
	}
	public CrewRecord getMostEvasions() {
		return mostEvasions;
	}
	public void setMostEvasions(CrewRecord mostEvasions) {
		this.mostEvasions = mostEvasions;
	}
	public CrewRecord getMostJumps() {
		return mostJumps;
	}
	public void setMostJumps(CrewRecord mostJumps) {
		this.mostJumps = mostJumps;
	}
	public CrewRecord getMostSkills() {
		return mostSkills;
	}
	public void setMostSkills(CrewRecord mostSkills) {
		this.mostSkills = mostSkills;
	}
	
}
