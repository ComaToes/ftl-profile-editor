package net.blerf.ftl.model;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.NoSuchElementException;


public class ShipLayout {
	// TODO: Some ROOM values haven't been deciphered (see: setRoom()).

	public enum RoomInfo { ALPHA, BETA, SQUARES_H, SQUARES_V }
	public enum DoorInfo { ROOM_ID_A, ROOM_ID_B }

	private int offsetX = 0, offsetY = 0, horizontal = 0, vertical = 0;
	private Rectangle shieldEllipse = new Rectangle();
	private TreeMap<Integer, EnumMap<RoomInfo,Integer>> roomMap = new TreeMap<Integer, EnumMap<RoomInfo,Integer>>();
	private LinkedHashMap<int[], EnumMap<DoorInfo,Integer>> doorMap = new LinkedHashMap<int[], EnumMap<DoorInfo,Integer>>();

	/**
	 * Constructs a layout with uninteresting defaults.
	 */
	public ShipLayout() {}

	public void setOffsetX( int n ) { offsetX = n; }
	public void setOffsetY( int n ) { offsetY = n; }
	public void setHorizontal( int n ) { vertical = n; }
	public void setVertical( int n ) { vertical = n; }

	public void setShieldEllipse( int w, int h, int x, int y ) {
		shieldEllipse = new Rectangle(x, y, w, h);
	}

	public int getOffsetX() { return offsetX; }
	public int getOffsetY() { return offsetY; }
	public int getHorizontal() { return horizontal; }
	public int getVertical() { return vertical; }
	public Rectangle getShieldEllipse() { return shieldEllipse; }

	/**
	 * Sets a room's info.
	 *
	 * @param roomId a roomId
	 * @param alpha ???
	 * @param beta ???
	 * @param squaresH horizontal count of tiles
	 * @param squaresV certical count of tiles
	 */
	public void setRoom( int roomId, int alpha, int beta, int squaresH, int squaresV ) {
		Integer roomIdObj = new Integer(roomId);
		EnumMap<RoomInfo,Integer> infoMap = new EnumMap<RoomInfo,Integer>(RoomInfo.class);
		infoMap.put( RoomInfo.ALPHA, new Integer(alpha) );
		infoMap.put( RoomInfo.BETA, new Integer(beta) );
		infoMap.put( RoomInfo.SQUARES_H, new Integer(squaresH) );
		infoMap.put( RoomInfo.SQUARES_V, new Integer(squaresV) );
		roomMap.put( roomIdObj, infoMap );
	}

	public EnumMap<RoomInfo, Integer> getRoomInfo( int roomId ) {
		return roomMap.get( new Integer(roomId) );
	}

	/**
	 * Returns the highest roomId + 1.
	 */
	public int getRoomCount() {
		try {
			Integer lastKey = roomMap.lastKey();
			return lastKey.intValue()+1;
		} catch (NoSuchElementException e) {
			return 0;
		}
	}

	/**
	 * Sets a door's info.
	 *
	 * @param wallX the 0-based Nth wall from the left
	 * @param wallY the 0-based Nth wall from the top
	 * @param vertical 1 for vertical wall coords, 0 for horizontal
	 * @param roomIdA an adjacent roomId, or -1 for vacuum
	 * @param roomIdB an adjacent roomId, or -1 for vacuum
	 */
	public void setDoor( int wallX, int wallY, int vertical, int roomIdA, int roomIdB ) {
		int[] doorCoord = new int[] {wallX, wallY, vertical};
		EnumMap<DoorInfo, Integer> infoMap = new EnumMap<DoorInfo, Integer>(DoorInfo.class);
		infoMap.put( DoorInfo.ROOM_ID_A, new Integer(roomIdA) );
		infoMap.put( DoorInfo.ROOM_ID_B, new Integer(roomIdB) );
		doorMap.put( doorCoord, infoMap );
	}

	public EnumMap<DoorInfo, Integer> getDoorInfo( int wallX, int wallY, int vertical ) {
		return doorMap.get( new int[] {wallX, wallY, vertical} );
	}

	public int getDoorCount() {
		return doorMap.size();
	}

	public LinkedHashMap<int[], EnumMap<DoorInfo,Integer>> getDoorMap() {
		return doorMap;
	}
}
