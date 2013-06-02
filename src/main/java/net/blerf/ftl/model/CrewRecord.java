package net.blerf.ftl.model;


public class CrewRecord {

	private String name;
	private String race;
	private boolean male;
	private int score;

	public CrewRecord( String name, String race, boolean male, int score ) {
		this.name = name;
		this.race = race;
		this.male = male;
		this.score = score;
	}

	public void setName( String s ) { name = s; }
	public void setRace( String s ) { race = s; }
	public void setMale( boolean b ) { male = b; }
	public void setScore( int n ) { score = n; }

	public String getName() { return name; }
	public String getRace() { return race; }
	public boolean isMale() { return male; }
	public int getScore() { return score; }
}
