package net.blerf.ftl.model;


public class Score {

	public enum Difficulty { EASY, NORMAL };

	private String shipName;
	private String shipType;
	private int score;
	private int sector;
	private Difficulty difficulty;
	private boolean victory;

	public Score( String shipName, String shipType, int score, int sector, Difficulty difficulty, boolean victory ) {
		this.shipName = shipName;
		this.shipType = shipType;
		this.score = score;
		this.sector = sector;
		this.difficulty = difficulty;
		this.victory = victory;
	}

	public void setShipName( String s ) { shipName = s; }
	public void setShipType( String s ) { shipType = s; }
	public void setScore( int n ) { score = n; }
	public void setSector( int n ) { sector = n; }
	public void setDifficulty( Difficulty d ) { difficulty = d; }
	public void setVictory( boolean b ) { victory = b; }

	public String getShipName() { return shipName; }
	public String getShipType() { return shipType; }
	public int getScore() { return score; }
	public int getSector() { return sector; }
	public Difficulty getDifficulty() { return difficulty; }
	public boolean isVictory() { return victory; }
}
