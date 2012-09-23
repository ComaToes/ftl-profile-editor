package net.blerf.ftl.model;

public class Score {

	private String shipName;
	private String shipType;
	private int score;
	private int sector;
	private int difficulty;
	private int unknownFlag;

	public Score(String shipName, String shipType, int score, int sector, int difficulty, int unknownFlag) {
		this.shipName = shipName;
		this.shipType = shipType;
		this.score = score;
		this.sector = sector;
		this.difficulty = difficulty;
		this.unknownFlag = unknownFlag;
	}

	public String getShipName() {
		return shipName;
	}

	public void setShipName(String shipName) {
		this.shipName = shipName;
	}

	public String getShipType() {
		return shipType;
	}

	public void setShipType(String shipType) {
		this.shipType = shipType;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getSector() {
		return sector;
	}

	public void setSector(int sector) {
		this.sector = sector;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public int getUnknownFlag() {
		return unknownFlag;
	}

	public void setUnknownFlag(int unknownFlag) {
		this.unknownFlag = unknownFlag;
	}
	
}
