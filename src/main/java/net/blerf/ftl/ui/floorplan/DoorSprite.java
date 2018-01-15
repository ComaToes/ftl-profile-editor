package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.ui.floorplan.DoorAtlas;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class DoorSprite extends JComponent implements ReferenceSprite<DoorState> {

	private static final int CLOSED_CELL = 0;
	private static final int OPEN_CELL = 4;

	private static final Logger log = LoggerFactory.getLogger( FireSprite.class );

	private final Color dummyColor = new Color( 150, 150, 200 );
	int chop = 10;  // Chop 10 pixels off the sides for skinny doors.

	private Point currentFrame = null;

	private SpriteReference<DoorState> doorRef;
	private DoorAtlas doorAtlas;
	private int level;
	private DoorCoordinate doorCoord;


	public DoorSprite( SpriteReference<DoorState> doorRef, DoorAtlas doorAtlas, int level, DoorCoordinate doorCoord ) {
		this.doorRef = doorRef;
		this.doorAtlas = doorAtlas;
		this.level = level;
		this.doorCoord = doorCoord;

		int shortSide = doorAtlas.getFrameWidth() - 2*chop;
		int longSide = doorAtlas.getFrameHeight();
		if ( doorCoord.v == 1 ) {
			this.setPreferredSize( new Dimension( shortSide, longSide ) );
		} else {
			this.setPreferredSize( new Dimension( longSide, shortSide ) );
		}

		this.setOpaque( false );

		doorRef.addSprite( this );
		referenceChanged();
	}

	public void setLevel( int n ) { level = n; }
	public int getLevel() { return level; }

	public void setCoordinate( DoorCoordinate c ) { doorCoord = c; }
	public DoorCoordinate getCoordinate() { return doorCoord; }

	@Override
	public SpriteReference<DoorState> getReference() {
		return doorRef;
	}

	@Override
	public void referenceChanged() {
		// TODO: Do away with the sprite's "level" field.
		// Test against doorState's health fields?

		Point[] openingFrameset = doorAtlas.getOpeningFrameset( level );
		if ( openingFrameset != null ) {
			if ( doorRef.get().isOpen() ) {
				currentFrame = openingFrameset[OPEN_CELL];
			} else {
				currentFrame = openingFrameset[CLOSED_CELL];
			}
		}
		else {
			currentFrame = null;  // Atlas didn't have that level.

			log.error( "Expected level not present in DoorAtlas: "+ level );
		}

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g.create();
		try {
			int w = this.getWidth(), h = this.getHeight();

			// Shift origin to the component center.
			// Optionally involve rotation to draw *as if* vertical.
			AffineTransform at = new AffineTransform();

			if ( doorCoord.v == 0 ) {

				// Transforms apply in reverse order.
				at.translate( 1, 0 );               // TODO: Somewhere it's off by 1 on the long axis when horizontal!?
				at.rotate( Math.toRadians( 90 ) );  // Clockwise.
				at.translate( this.getHeight()/2, -this.getWidth()/2 );

				w = this.getHeight(); h = this.getWidth();  // Ninty degrees, swap axes.
			}
			else {
				// Transforms apply in reverse order.
				at.translate( this.getWidth()/2, this.getHeight()/2 );
			}
			g2d.transform( at );

			if ( currentFrame != null ) {
				int rX = currentFrame.x;
				int rY = currentFrame.y;
				int rW = doorAtlas.getFrameWidth();
				int rH = doorAtlas.getFrameHeight();
				g2d.drawImage( doorAtlas.getSheetImage(), -rW/2, -rH/2, -rW/2+rW-1, -rH/2+rH-1, rX, rY, rX+rW-1, rY+rH-1, this );
			}
			else {
				g2d.setColor( dummyColor );
				g2d.fillRect( -w/2, -h/2, w-1, h-1 );
			}
		}
		finally {
			g2d.dispose();
		}
	}
}
