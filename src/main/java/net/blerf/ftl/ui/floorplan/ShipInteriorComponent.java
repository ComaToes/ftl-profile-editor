package net.blerf.ftl.ui.floorplan;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.Map;
import javax.swing.JComponent;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;


public class ShipInteriorComponent extends JComponent {

	private static final int squareSize = 35;
	private static final int jambLength = 5;

	private Color floorCrackColor = new Color( 125, 125, 125 );
	private Stroke floorCrackStroke = new BasicStroke( 1 );
	private Color roomBorderColor = new Color( 15, 15, 15 );
	private Stroke roomBorderStroke = new BasicStroke( 4 );

	private ShipLayout shipLayout;


	public ShipInteriorComponent( ShipLayout shipLayout, int chassisWidth, int chassisHeight ) {
		this.shipLayout = shipLayout;

		this.setPreferredSize( new Dimension( chassisWidth, chassisHeight ) );
		this.setOpaque( false );
	}

	@Override
	public void paintComponent( Graphics g ) {

		Graphics2D g2d = (Graphics2D)g;
		Color prevColor = g2d.getColor();
		Stroke prevStroke = g2d.getStroke();

		int fromX, fromY, toX, toY;

		Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
		DoorCoordinate doorCoord = null;
		ShipLayoutDoor layoutDoor = null;

		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomCoordX = layoutRoom.locationX;
			int roomCoordY = layoutRoom.locationY;
			int roomX = shipLayout.getOffsetX()*squareSize + layoutRoom.locationX*squareSize;
			int roomY = shipLayout.getOffsetY()*squareSize + layoutRoom.locationY*squareSize;

			// Draw floor lines within rooms.
			g2d.setColor( floorCrackColor );
			g2d.setStroke( floorCrackStroke );
			for ( int n=0; n < squaresV-1; n++ ) {  // H lines.
				g2d.drawLine( roomX+1, roomY + (n+1)*squareSize, roomX + squaresH*squareSize - 1, roomY + (n+1)*squareSize );
			}
			for ( int n=0; n < squaresH-1; n++ ) {  // V lines.
				g2d.drawLine( roomX + (n+1)*squareSize, roomY+1, roomX + (n+1)*squareSize, roomY + squaresV*squareSize - 1 );
			}

			// Draw borders around rooms.
			for ( int n=0; n < squaresV; n++ ) {  // V lines.
				// West side.
				fromX = roomX;
				fromY = roomY + n*squareSize;
				toX = roomX;
				toY = roomY + (n+1)*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX, roomCoordY + n, 1 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, fromY+jambLength );
					g2d.drawLine( fromX, toY-jambLength, toX, toY );
				}
				else {
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, toY );
				}

				// East Side.
				fromX = roomX + squaresH*squareSize;
				fromY = roomY + n*squareSize;
				toX = roomX + squaresH*squareSize;
				toY = roomY + (n+1)*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX + squaresH, roomCoordY + n, 1 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, fromY+jambLength );
					g2d.drawLine( fromX, toY-jambLength, toX, toY );
				}
				else {
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, toY );
				}
			}

			g2d.setStroke( roomBorderStroke );
			g2d.setColor( roomBorderColor );
			for ( int n=0; n < squaresH; n++ ) {  // H lines.
				// North side.
				fromX = roomX + n*squareSize;
				fromY = roomY;
				toX = roomX + (n+1)*squareSize;
				toY = roomY;
				doorCoord = new DoorCoordinate( roomCoordX + n, roomCoordY, 0 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, fromX+jambLength, fromY );
					g2d.drawLine( toX-jambLength, fromY, toX, toY );
				}
				else {
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, toY );
				}

				// South side.
				fromX = roomX + n*squareSize;
				fromY = roomY + squaresV*squareSize;
				toX = roomX + (n+1)*squareSize;
				toY = roomY + squaresV*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX + n, roomCoordY + squaresV, 0 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, fromX+jambLength, fromY );
					g2d.drawLine( toX-jambLength, fromY, toX, toY );
				}
				else {
					g2d.setStroke( roomBorderStroke );
					g2d.setColor( roomBorderColor );
					g2d.drawLine( fromX, fromY, toX, toY );
				}
			}
		}

		g2d.setColor( prevColor );
		g2d.setStroke( prevStroke );
	}
}
