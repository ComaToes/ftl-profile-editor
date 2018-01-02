package net.blerf.ftl.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A layout for an FTL sector map.
 *
 * Beacons and associted components will be scattered over a background.
 *
 * Scatter locations should be decided by a GeneratedSectorMap.
 *
 * @see net.blerf.ftl.parser.sectormap.GeneratedSectorMap
 */
public class SectorMapLayout implements LayoutManager2 {

	private static final Logger log = LoggerFactory.getLogger( SectorMapLayout.class );

	// These numbers were copied from RandomSectorMapGenerator's size for FTL 1.5.4.
	private static final int DEFAULT_BEACON_ZONE_W = 700;
	private static final int DEFAULT_BEACON_ZONE_H = 488;

	protected List<Point> beaconLocationList = new ArrayList<Point>();

	protected List<Component> beaconList = new ArrayList<Component>();
	protected List<Component> miscBoxList = new ArrayList<Component>();
	protected Component playerShipComp = null;
	protected Component bgComp = null;

	protected Map<Component, SectorMapConstraints> constraintsMap = new HashMap<Component, SectorMapConstraints>();

	protected Insets margin = new Insets( 20, 0, 0, 60 );
	protected int beaconZoneW = DEFAULT_BEACON_ZONE_W;
	protected int beaconZoneH = DEFAULT_BEACON_ZONE_H;


	public SectorMapLayout() {
	}

	/**
	 * Sets space for margin between the map's border and the beacons.
	 */
	public void setMargin( Insets m ) {
		margin = new Insets( m.top, m.left, m.bottom, m.right );
	}

	public Insets getMargin() {
		return margin;
	}

	/**
	 * Sets the expected range of x/y values for beacon locations.
	 *
	 * If null, hardcoded defaults will be used.
	 */
	public void setBeaconRegionSize( Dimension d ) {
		if ( d != null ) {
			beaconZoneW = d.width;
			beaconZoneH = d.height;
		}
		else {
			beaconZoneW = DEFAULT_BEACON_ZONE_W;
			beaconZoneH = DEFAULT_BEACON_ZONE_H;
		}
	}

	public Dimension getBeaconRegionSize() {
		return new Dimension( beaconZoneW, beaconZoneH );
	}

	/**
	 * Sets x/y pixel offsets to use for components, where index == beaconId.
	 *
	 * @ param newLocations a list of points, or null
	 * @see net.blerf.ftl.parser.sectormap.GeneratedBeacon
	 */
	public void setBeaconLocations( List<Point> newLocations ) {
		beaconLocationList.clear();

		if ( newLocations != null ) {
			beaconLocationList.addAll( newLocations );
		}
	}

	/**
	 * Returns the beacon id at which a component was placed, or -1.
	 */
	public int getBeaconId( Component comp ) {
		if ( beaconList.contains( comp ) ) {
			return beaconList.indexOf( comp );
		}

		SectorMapConstraints compC = constraintsMap.get( comp );
		if ( compC != null && compC.getBeaconId() < beaconList.size() ) {
			return compC.getBeaconId();
		}
		return -1;
	}

	public Component getMiscBoxAtBeaconId( int beaconId ) {
		for ( Component comp : miscBoxList ) {
			SectorMapConstraints compC = constraintsMap.get( comp );
			if ( compC.getBeaconId() == beaconId ) {
				return comp;
			}
		}
		return null;
	}

	@Override
	public void removeLayoutComponent( Component comp ) {
		constraintsMap.remove( comp );
		beaconList.remove( comp );
		miscBoxList.remove( comp );
		if ( comp == playerShipComp ) playerShipComp = null;
		if ( comp == bgComp ) bgComp = null;
	}

	@Override
	public void invalidateLayout( Container target ) {
	}

	@Override
	public void layoutContainer( Container parent ) {
		if ( bgComp != null ) {
			bgComp.setSize( bgComp.getPreferredSize() );
			bgComp.setLocation( margin.left, margin.top );
		}

		if ( beaconList.size() == 0 ) return;

		for ( int b=0; b < beaconList.size(); b++ ) {
			Component comp = beaconList.get( b );

			int beaconLocX;
			int beaconLocY;

			if ( b < beaconLocationList.size() ) {
				Point beaconLoc = beaconLocationList.get( b );
				beaconLocX = beaconLoc.x;
				beaconLocY = beaconLoc.y;
			}
			else {
				int overflowCellW = 50;
				int overflowCellH = beaconZoneH / (beaconList.size() - beaconLocationList.size());
				beaconLocX = beaconZoneW - overflowCellW/2;
				beaconLocY = (b - beaconLocationList.size()) * overflowCellH + overflowCellH/2;
			}

			int beaconX = margin.left + beaconLocX - comp.getPreferredSize().width/2;
			int beaconY = margin.top + beaconLocY - comp.getPreferredSize().height/2;
			comp.setSize( comp.getPreferredSize() );
			comp.setLocation( beaconX, beaconY );
		}

		for ( Component comp : miscBoxList ) {
			SectorMapConstraints compC = constraintsMap.get( comp );

			if ( compC.getBeaconId() < beaconList.size() ) {
				Component beaconComp = beaconList.get( compC.getBeaconId() );

				int miscBoxX = beaconComp.getX() + beaconComp.getWidth()/2;
				int miscBoxY = beaconComp.getY() - beaconComp.getHeight()/3 - comp.getPreferredSize().height/2;

				comp.setSize( comp.getPreferredSize() );
				comp.setLocation( miscBoxX, miscBoxY );
			}
		}

		if ( playerShipComp != null ) {
			Component comp = playerShipComp;
			SectorMapConstraints compC = constraintsMap.get( comp );

			if ( compC.getBeaconId() < beaconList.size() ) {
				Component beaconComp = beaconList.get( compC.getBeaconId() );

				int playerShipX = beaconComp.getX() + beaconComp.getWidth()/2 - comp.getPreferredSize().width/4;
				int playerShipY = beaconComp.getY() + beaconComp.getHeight()/2 - comp.getPreferredSize().height/2;

				comp.setSize( comp.getPreferredSize() );
				comp.setLocation( playerShipX, playerShipY );
			}
		}
	}

	@Override
	public void addLayoutComponent( Component comp, Object constraints ) {
		if ( constraints instanceof SectorMapConstraints == false ) return;
		SectorMapConstraints compC = (SectorMapConstraints)constraints;

		constraintsMap.put( comp, compC );
		if ( SectorMapConstraints.BEACON.equals( compC.type ) ) {
			if ( !beaconList.contains( comp ) )
				beaconList.add( comp );
		}
		else if ( SectorMapConstraints.MISC_BOX.equals( compC.type ) ) {
			if ( !miscBoxList.contains( comp ) )
				miscBoxList.add( comp );
		}
		else if ( SectorMapConstraints.PLAYER_SHIP.equals( compC.type ) ) {
			playerShipComp = comp;
		}
		else if ( SectorMapConstraints.BACKGROUND.equals( compC.type ) ) {
			bgComp = comp;
		}
	}

	@Override
	public void addLayoutComponent( String name, Component comp ) {
	}

	@Override
	public Dimension minimumLayoutSize( Container parent ) {
		return new Dimension( margin.left + beaconZoneW + margin.right, margin.top + beaconZoneH + margin.bottom );
	}

	@Override
	public Dimension preferredLayoutSize( Container parent ) {
		return new Dimension( margin.left + beaconZoneW + margin.right, margin.top + beaconZoneH + margin.bottom );
	}

	@Override
	public Dimension maximumLayoutSize( Container target ) {
		return new Dimension( Short.MAX_VALUE, Short.MAX_VALUE );
	}

	@Override
	public float getLayoutAlignmentX( Container target ) {
		return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY( Container target ) {
		return 0.5f;
	}



	// Constraint info:
	//   BEACON:      -
	//   MISC_BOX:    beaconId
	//   PLAYER_SHIP: beaconId
	//   BACKGROUND:  -
	//
	public static class SectorMapConstraints {
		public static final String BEACON = "BEACON";
		public static final String MISC_BOX = "MISC_BOX";
		public static final String PLAYER_SHIP = "PLAYER_SHIP";
		public static final String BACKGROUND = "BACKGROUND";

		public String type = BEACON;
		private int beaconId = -1;

		public SectorMapConstraints( String type ) {
			this.type = type;
		}
		public void setBeaconId( int n ) {
			if ( MISC_BOX.equals( type ) || PLAYER_SHIP.equals( type ) )
				beaconId = n;
		}
		public int getBeaconId() { return beaconId; }
	}
}
