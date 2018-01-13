package net.blerf.ftl.model;

import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;


public class ShipLayout {

	private int offsetX = 0, offsetY = 0;
	private int horizontal = 0, vertical = 0;
	private Rectangle shieldEllipse = new Rectangle();
	private TreeMap<Integer, ShipLayoutRoom> roomMap = new TreeMap<Integer, ShipLayoutRoom>();
	private Map<DoorCoordinate, ShipLayoutDoor> doorMap = new LinkedHashMap<DoorCoordinate, ShipLayoutDoor>();


	/**
	 * Constructs a layout with uninteresting defaults.
	 */
	public ShipLayout() {
	}

	/**
	 * Sets a positive offset to the entire ship in square-sized (35x35) units.
	 *
	 * This positive offset shifts right/down.
	 *
	 * Sprite locations in saved games will have this offset baked in.
	 *
	 * ShipChassis will further offset the ship images specifically.
	 *
	 * @see net.blerf.ftl.xml.ShipChassis
	 */
	public void setOffsetX( int n ) { offsetX = n; }
	public void setOffsetY( int n ) { offsetY = n; }
	public int getOffsetX() { return offsetX; }
	public int getOffsetY() { return offsetY; }

	/**
	 * Sets an additional whole-ship offest in pixel units.
	 *
	 * TODO: Reportedly horizontal doesn't apply for nearby ships!?
	 */
	public void setHorizontal( int n ) { horizontal = n; }
	public void setVertical( int n ) { vertical = n; }
	public int getHorizontal() { return horizontal; }
	public int getVertical() { return vertical; }

	/**
	 * Sets the collision/orbit ellipse.
	 *
	 * Note: This is abstract and does not affect how shields are painted.
	 */
	public void setShieldEllipse( int w, int h, int x, int y ) {
		shieldEllipse = new Rectangle( x, y, w, h );
	}

	public Rectangle getShieldEllipse() { return shieldEllipse; }

	/**
	 * Sets a room's info.
	 *
	 * @param roomId a roomId
	 * @param layoutRoom room info
	 */
	public void setRoom( int roomId, ShipLayoutRoom layoutRoom ) {
		roomMap.put( roomId, layoutRoom );
	}

	public ShipLayoutRoom getRoom( int roomId ) {
		return roomMap.get( roomId );
	}

	/**
	 * Returns the highest roomId + 1.
	 */
	public int getRoomCount() {
		try {
			int lastKey = roomMap.lastKey();
			return (lastKey + 1);
		}
		catch ( NoSuchElementException e ) {
			return 0;
		}
	}

	/**
	 * Sets a door's info.
	 *
	 * @param wallX the 0-based Nth wall from the left
	 * @param wallY the 0-based Nth wall from the top
	 * @param vertical 1 for vertical wall coords, 0 for horizontal
	 * @param layoutDoor dooor info
	 * @see ShipLayoutDoor
	 */
	public void setDoor( int wallX, int wallY, int vertical, ShipLayoutDoor layoutDoor ) {
		DoorCoordinate doorCoord = new DoorCoordinate( wallX, wallY, vertical );

		doorMap.put( doorCoord, layoutDoor );
	}

	public ShipLayoutDoor getDoor( int wallX, int wallY, int vertical ) {
		return doorMap.get( new DoorCoordinate( wallX, wallY, vertical ) );
	}

	public int getDoorCount() {
		return doorMap.size();
	}

	/**
	 * Returns the map containing this layout's door info.
	 *
	 * Keys are in the order of the original layout config file.
	 * That is NOT the same order as doors in saved games.
	 */
	public Map<DoorCoordinate, ShipLayoutDoor> getDoorMap() {
		return doorMap;
	}
}
