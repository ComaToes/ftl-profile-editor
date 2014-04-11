package net.blerf.ftl.model;


public class CrewRecord {

	private String name;
	private String race;
	private boolean male;
	private int value;


	public CrewRecord( String name, String race, boolean male, int value ) {
		this.name = name;
		this.race = race;
		this.male = male;
		this.value = value;
	}

	public void setName( String s ) { name = s; }
	public void setRace( String s ) { race = s; }
	public void setMale( boolean b ) { male = b; }
	public void setValue( int n ) { value = n; }

	public String getName() { return name; }
	public String getRace() { return race; }
	public boolean isMale() { return male; }
	public int getValue() { return value; }

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(String.format("Name: %-20s  Race: %-9s  Sex: %s  Score: %4d\n", name, race, (male ? "M" : "F"), value));

		return result.toString();
	}
}
