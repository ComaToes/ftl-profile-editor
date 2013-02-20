package net.blerf.ftl.model;


public class CrewRecord {

	private String name, race;
	private int score, unknownFlag;

	public CrewRecord( String name, String race, int score, int unknownFlag ) {
		this.name = name;
		this.race = race;
		this.score = score;
		this.unknownFlag = unknownFlag;
	}

	public void setName( String s ) { name = s; }
	public void setRace( String s ) { race = s; }
	public void setScore( int n ) { score = n; }

	public String getName() { return name; }
	public String getRace() { return race; }
	public int getScore() { return score; }

	public void setUnknownFlag( int n ) { unknownFlag = n; }
	public int getUnknownFlag() { return unknownFlag; }
}
