package net.blerf.ftl.model;

public class CrewRecord {

	private String name, race;
	private int score, unknownFlag;
	
	public CrewRecord(String name, String race, int score, int unknownFlag) {
		this.name = name;
		this.race = race;
		this.score = score;
		this.unknownFlag = unknownFlag;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRace() {
		return race;
	}

	public void setRace(String race) {
		this.race = race;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getUnknownFlag() {
		return unknownFlag;
	}

	public void setUnknownFlag(int unknownFlag) {
		this.unknownFlag = unknownFlag;
	}
	
	
	
}
