package net.blerf.ftl.parser.sectormap;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import net.blerf.ftl.parser.sectormap.GeneratedBeacon;


/**
 * An object to communicate values FTL would generate at runtime.
 */
public class GeneratedSectorMap {

	protected Dimension preferredSize = null;
	protected Integer rebelFleetFudge = null;
	protected List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();


	public GeneratedSectorMap() {
	}

	/**
	 * Sets the size recommended to display the entire map, or null.
	 *
	 * This can vary across FTL versions.
	 *
	 * @see net.blerf.ftl.parser.sectormap.RandomSectorMapGenerator
	 */
	public void setPreferredSize( Dimension newSize ) {
		preferredSize = newSize;
	}

	public Dimension getPreferredSize() {
		return preferredSize;
	}

	/**
	 * Sets the generated rebelFleetFudge, or null.
	 *
	 * This generated value is unimportant, since the saved game overrides it.
	 *
	 * @see net.blerf.ftl.parser.SavedGameParser.SavedGameState#setRebelFleetFudge(int)
	 */
	public void setRebelFleetFudge( Integer n ) {
		rebelFleetFudge = n;
	}

	public Integer getRebelFleetFudge() {
		return rebelFleetFudge;
	}

	/**
	 * Sets the list of GeneratedBeacon objects, or null to clear it.
	 */
	public void setGeneratedBeaconList( List<GeneratedBeacon> newGenBeaconList ) {
		genBeaconList.clear();
		if ( newGenBeaconList != null ) {
			genBeaconList.addAll( newGenBeaconList );
		}
	}

	public List<GeneratedBeacon> getGeneratedBeaconList() {
		return genBeaconList;
	}
}
