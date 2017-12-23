package net.blerf.ftl.parser.sectormap;

import java.awt.Point;


/**
 * Beacon values FTL would generate at runtime.
 *
 * FTL populates these values based on the sector layout seed. They are not
 * serialized into a BeaconState.
 *
 * When FTL generates a map, it iterates over a rectangular grid, randomly
 * skipping spaces. When it loads a savedgame's list of BeaconStates, they are
 * associated with the nth.
 */
public class GeneratedBeacon {

	protected int throbTicks = 0;
	protected int x = 0;
	protected int y = 0;

	/**
	 * Sets time elapsed while this beacon's 'under attack' throbber
	 * oscillates.
	 *
	 * This counts from 0 (normal) to 1000 (glowing) to 2000 (gone again).
	 *
	 * FTL randomly assigns values to beacons, so they won't glow in sync.
	 *
	 * @see SavedGameParser.BeaconState#setUnderAttack(boolean)
	 */
	public void setThrobTicks( int n ) { throbTicks = n; }
	public int getThrobTicks() { return throbTicks; }

	/**
	 * Sets the pixel location of this beacon.
	 *
	 * This offset is from a point within the sector map window.
	 */
	public void setLocation( int newX, int newY ) { x = newX; y = newY; }
	public Point getLocation() { return new Point( x, y ); }
}
