package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.ui.floorplan.AnimAtlas;


public class BreachSprite extends JComponent {

	private static final String BREACH_ANIM = "breach";

	private static final Logger log = LoggerFactory.getLogger( BreachSprite.class );

	private final Color dummyColor = new Color( 150, 150, 200 );

	private Point currentFrame = null;

	private AnimAtlas breachAtlas;
	private int roomId;
	private int squareId;
	private int health;


	public BreachSprite( AnimAtlas breachAtlas, int roomId, int squareId, int health ) {
		this.breachAtlas = breachAtlas;
		this.roomId = roomId;
		this.squareId = squareId;
		this.health = health;
		this.setOpaque( false );

		int prefWidth = Math.max( breachAtlas.getFrameWidth(), 10 );
		int prefHeight = Math.max( breachAtlas.getFrameHeight(), 10 );
		this.setPreferredSize( new Dimension( prefWidth, prefHeight ) );

		// Get the last frame of the "breach" Anim.
		Point[] frameset = breachAtlas.getFrameset( BREACH_ANIM );
		if ( frameset != null && frameset.length > 0 ) {
			currentFrame = frameset[frameset.length - 1];
		}
		else {
			log.error( "Expected Anim not present in Atlas: "+ BREACH_ANIM );
		}
	}

	public void setRoomId( int n ) { roomId = n; }
	public void setSquareId( int n ) { squareId = n; }
	public void setHealth( int n ) { health = n; }

	public int getRoomId() { return roomId; }
	public int getSquareId() { return squareId; }
	public int getHealth() { return health; }

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;

		if ( currentFrame != null ) {
			int rX = currentFrame.x;
			int rY = currentFrame.y;
			int rW = breachAtlas.getFrameWidth();
			int rH = breachAtlas.getFrameHeight();
			g2d.drawImage( breachAtlas.getSheetImage(), 0, 0, this.getWidth()-1, this.getHeight()-1, rX, rY, rX+rW-1, rY+rH-1, this );
		}
		else {
			Color prevColor = g2d.getColor();

			g2d.setColor( dummyColor );
			g2d.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1 );

			g2d.setColor( prevColor );
		}
	}
}