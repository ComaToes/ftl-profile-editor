package net.blerf.ftl.model;


/**
 * A summary of a single past campaign.
 */
public class Score {

	public enum Difficulty { EASY, NORMAL, HARD };

	private String shipName;
	private String shipId;
	private int value;
	private int sector;
	private Difficulty difficulty;
	private boolean victory;
	private boolean dlcEnabled = false;


	/**
	 * Constructs a Score.
	 *
	 * @param shipName an arbitrary label from the player
	 * @param shipId the id of the ShipBlueprint used
	 * @param value the number of points earned in that campaign
	 * @param difficulty the difficulty of that campaign
	 */
	public Score( String shipName, String shipId, int value, int sector, Difficulty difficulty, boolean victory ) {
		this.shipName = shipName;
		this.shipId = shipId;
		this.value = value;
		this.sector = sector;
		this.difficulty = difficulty;
		this.victory = victory;
	}

	public void setShipName( String s ) { shipName = s; }
	public void setShipId( String s ) { shipId = s; }
	public void setValue( int n ) { value = n; }
	public void setSector( int n ) { sector = n; }
	public void setDifficulty( Difficulty d ) { difficulty = d; }
	public void setVictory( boolean b ) { victory = b; }

	public String getShipName() { return shipName; }
	public String getShipId() { return shipId; }
	public int getValue() { return value; }
	public int getSector() { return sector; }
	public Difficulty getDifficulty() { return difficulty; }
	public boolean isVictory() { return victory; }

	/**
	 * Sets whether AE Content was enabled in that campaign.
	 *
	 * This was introduced in FTL 1.5.4.
	 */
	public void setDLCEnabled( boolean b ) { dlcEnabled = b; }
	public boolean isDLCEnabled() { return dlcEnabled; }

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append(String.format("Name: %-25s  ShipId: %-25s  Score: %5d\n", shipName, shipId, value));
		result.append(String.format("Sector: %d  Difficulty: %-6s  Victory: %-5b  DLC Enabled: %-5b\n", sector, difficulty.toString(), victory, dlcEnabled));

		return result.toString();
	}
}
