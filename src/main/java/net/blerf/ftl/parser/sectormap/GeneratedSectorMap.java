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
	protected int rebelFleetFudge = 100;  // Arbitrary default.
	protected List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();


	public GeneratedSectorMap() {
	}

	/**
	 * Sets the size expected to display the entire map, or null.
	 *
	 * The map size changed in FTL 1.5.4.
	 */
	public void setPreferredSize( Dimension newSize ) {
		preferredSize = newSize;
	}

	public Dimension getPreferredSize() {
		return preferredSize;
	}

	public void setRebelFleetFudge( int n ) {
		rebelFleetFudge = n;
	}

	public int getRebelFleetFudge() {
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
