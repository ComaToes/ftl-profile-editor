package net.blerf.ftl.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * A layout for an FTL sector map.
 *
 * A list of beacons and map boxes are wrapped vertically,
 * over a background, and above each column is a component
 * that should hold buttons to adjust the beacon count in
 * that column.
 */
public class SectorMapLayout implements LayoutManager2 {

	private static final Logger log = LogManager.getLogger(SectorMapLayout.class);

	private ArrayList<Component> beaconList = new ArrayList<Component>();
	private ArrayList<Component> miscBoxList = new ArrayList<Component>();
	private ArrayList<Component> columnCtrlList = new ArrayList<Component>();
	private Component playerShipComp = null;
	private Component bgComp = null;

	private int defaultColumnSize = 4;
	private LinkedHashMap<Integer, Integer> columnSizeMap = new LinkedHashMap<Integer, Integer>();
	private HashMap<Component, SectorMapConstraints> constraintsMap = new HashMap<Component, SectorMapConstraints>();

	int ctrlZoneH = 50;
	int beaconZoneX = 0;
	int beaconZoneY = 0;
	int beaconZoneW = 480;  // Divided by 6 is 80.
	int beaconZoneH = 320;  // Divided by 4 is 80. Divide by current columnSize for rowHeights.

	int columnCount = 0;  // Determined by invalidateLayout().

	public void SectorMapLayout() {
	}

	public void setDefaultColumnSize( int n ) { defaultColumnSize = n; }
	public int getDefaultColumnSize() { return defaultColumnSize; }

	public void setColumnSize( int col, int size ) {
		if ( size < 1 ) return;
		columnSizeMap.put( new Integer(col), new Integer(size) );
	}
	public int getColumnSize( int n ) {
		if ( n < 0 || n >= columnCount)
			throw new ArrayIndexOutOfBoundsException(String.format("Attempted to get size of column %d out of %d", n, columnCount));

		if ( n < columnCount-1 ) {
			Integer size = columnSizeMap.get( new Integer(n) );
			return (size != null ? size.intValue() : defaultColumnSize);
		}
		else {  // Subtract all prior columns from the total to determine the last column.
			int col = 0;
			int b = 0;
			while (b < beaconList.size() && col < columnCount-1) {
				b += getColumnSize(col);
				col++;
			}
			return (beaconList.size() - b);
		}
	}

	public int getCtrlColumn( Component comp ) {
		return (columnCtrlList.contains(comp) ? columnCtrlList.indexOf(comp) : -1);
	}

	private void updateColumnCount() {
		columnCount = 0;
		for (int i=0; i < beaconList.size(); ) {
			// Avoid getColumnSize() until columnCount has been set.

			Integer size = columnSizeMap.get( new Integer(columnCount) );
			i += (size != null ? size.intValue() : defaultColumnSize);
			columnCount++;
		}
	}

	public int getBeaconId( Component comp ) {
		if ( beaconList.contains( comp) )
			return beaconList.indexOf( comp );

		SectorMapConstraints compC = constraintsMap.get( comp );
		if ( compC != null && compC.getBeaconId() < beaconList.size() )
			return compC.getBeaconId();

		return -1;
	}

	public Component getMiscBoxAtBeaconId( int beaconId ) {
		for (Component comp : miscBoxList) {
			SectorMapConstraints compC = constraintsMap.get( comp );
			if ( compC.getBeaconId() == beaconId ) {
				return comp;
			}
		}
		return null;
	}

	public void removeLayoutComponent( Component comp ) {
		constraintsMap.remove( comp );
		beaconList.remove( comp );
		miscBoxList.remove( comp );
		columnCtrlList.remove( comp );
		if ( comp == playerShipComp ) playerShipComp = null;
		if ( comp == bgComp ) bgComp = null;

		if ( beaconList.size() == 0 )
			columnSizeMap.clear();
	}

	public void invalidateLayout( Container target ) {
		updateColumnCount();
	}

	public void layoutContainer( Container parent ) {
		if ( bgComp != null ) {
			bgComp.setSize( bgComp.getPreferredSize() );
			bgComp.setLocation( beaconZoneX, beaconZoneY );
		}

		if ( beaconList.size() == 0 ) return;
		if ( columnCount == 0 ) updateColumnCount();  // If there are beacons, this must be outdated.

		int colWidth = beaconZoneW / columnCount;  // Add half colWidth to get centers.

		int col = 0;
		int colSize = getColumnSize( 0 );
		int nextColAt = colSize;
		int colPosition = 0;

		for (int b=0; b < beaconList.size(); b++) {
			Component comp = beaconList.get(b);

			if ( nextColAt == b ) {
				col++;
				colSize = getColumnSize( col );
				nextColAt += colSize;
				colPosition = 0;
			}

			int beaconX = beaconZoneX + col*colWidth + colWidth/2 - comp.getPreferredSize().width/2;
			int beaconY = beaconZoneY + colPosition * beaconZoneH/colSize + beaconZoneH/colSize/2 - comp.getPreferredSize().height/2;
			comp.setSize( comp.getPreferredSize() );
			comp.setLocation( beaconX, beaconY );

			colPosition++;
		}

		for (int i=0; i < miscBoxList.size(); i++) {
			Component comp = miscBoxList.get(i);
			SectorMapConstraints compC = constraintsMap.get( comp );

			if ( compC.getBeaconId() < beaconList.size() ) {
				Component beaconComp = beaconList.get( compC.getBeaconId() );

				int miscBoxX = beaconComp.getX()+beaconComp.getWidth()/3;
				int miscBoxY = beaconComp.getY()+beaconComp.getHeight()/3 - comp.getPreferredSize().height/2;

				comp.setSize( comp.getPreferredSize() );
				comp.setLocation( miscBoxX, miscBoxY );
			}
		}

		if ( playerShipComp != null ) {
			Component comp = playerShipComp;
			SectorMapConstraints compC = constraintsMap.get( comp );

			if ( compC.getBeaconId() < beaconList.size() ) {
				Component beaconComp = beaconList.get( compC.getBeaconId() );

				int playerShipX = beaconComp.getX()+beaconComp.getWidth()/2 - comp.getPreferredSize().width/4;
				int playerShipY = beaconComp.getY()+beaconComp.getHeight()/2 - comp.getPreferredSize().height/2;

				comp.setSize( comp.getPreferredSize() );
				comp.setLocation( playerShipX, playerShipY );
			}
		}

		for (int i=0; i < columnCtrlList.size(); i++) {
			Component comp = columnCtrlList.get(i);

			if ( i < columnCount ) {  // No need for controls on the final column.
				int ctrlX = beaconZoneX + i*colWidth + colWidth/2 - comp.getPreferredSize().width/2;
				int ctrlY = beaconZoneH + 10;

				comp.setSize( comp.getPreferredSize() );
				comp.setLocation( ctrlX, ctrlY );
				comp.setVisible( true );
			}
			else {  // Hide extra controls.
				comp.setVisible( false );
			}
		}
	}

	public void addLayoutComponent( Component comp, Object constraints ) {
		if ( constraints instanceof SectorMapConstraints == false ) return;
		SectorMapConstraints compC = (SectorMapConstraints)constraints;

		constraintsMap.put( comp, compC );
		if ( SectorMapConstraints.BEACON.equals( compC.type ) ) {
			if ( !beaconList.contains( comp ) )
				beaconList.add( comp );
		} else if ( SectorMapConstraints.MISC_BOX.equals( compC.type ) ) {
			if ( !miscBoxList.contains( comp ) )
				miscBoxList.add( comp );
		} else if ( SectorMapConstraints.PLAYER_SHIP.equals( compC.type ) ) {
			playerShipComp = comp;
		} else if ( SectorMapConstraints.COLUMN_CTRL.equals( compC.type ) ) {
			if ( !columnCtrlList.contains( comp ) )
				columnCtrlList.add( comp );
		} else if ( SectorMapConstraints.BACKGROUND.equals( compC.type ) ) {
			bgComp = comp;
		}
	}

	public void addLayoutComponent( String name, Component comp ) {
	}
	public Dimension minimumLayoutSize( Container parent ) {
		return new Dimension( beaconZoneW, beaconZoneH + ctrlZoneH );
	}
	public Dimension preferredLayoutSize( Container parent ) {
		return new Dimension( beaconZoneW, beaconZoneH + ctrlZoneH );
	}
	public Dimension maximumLayoutSize( Container target ) {
		return new Dimension( Short.MAX_VALUE, Short.MAX_VALUE );
	}
	public float getLayoutAlignmentX( Container target ) {
		return 0.5f;
	}
	public float getLayoutAlignmentY( Container target ) {
		return 0.5f;
	}



	// Constraint info:
	//   BEACON:      -
	//   MISC_BOX:    beaconId
	//   PLAYER_SHIP: beaconId
	//   COLUMN_CTRL: -
	//   BACKGROUND:  -
	//
	public static class SectorMapConstraints {
		public static final String BEACON = "BEACON";
		public static final String MISC_BOX = "MISC_BOX";
		public static final String PLAYER_SHIP = "PLAYER_SHIP";
		public static final String COLUMN_CTRL = "COLUMN_CTRL";
		public static final String BACKGROUND = "BACKGROUND";

		public String type = BEACON;
		private int beaconId = -1;

		public SectorMapConstraints( String type ) {
			this.type = type;
		}
		public void setBeaconId( int n ) {
			if ( MISC_BOX.equals(type) || PLAYER_SHIP.equals(type) )
				beaconId = n;
		}
		public int getBeaconId() { return beaconId; }
	}
}
