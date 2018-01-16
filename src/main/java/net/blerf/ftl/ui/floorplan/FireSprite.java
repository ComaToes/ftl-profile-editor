package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FireSprite extends JComponent {

	private static final String FIRE_ANIM = "fire_large";

	private static final Logger log = LoggerFactory.getLogger( FireSprite.class );

	private final Color dummyColor = new Color( 150, 150, 200 );

	private Point currentFrame = null;

	private AnimAtlas fireAtlas;
	private int roomId;
	private int squareId;
	private int health;


	public FireSprite( AnimAtlas fireAtlas, int roomId, int squareId, int health ) {
		this.fireAtlas = fireAtlas;
		this.roomId = roomId;
		this.squareId = squareId;
		this.health = health;

		int prefWidth = Math.max( fireAtlas.getFrameWidth(), 10 );
		int prefHeight = Math.max( fireAtlas.getFrameHeight(), 10 );
		this.setPreferredSize( new Dimension( prefWidth, prefHeight ) );

		// Get the first frame of the "fire_large" Anim.
		Point[] frameset = fireAtlas.getFrameset( FIRE_ANIM );
		if ( frameset != null && frameset.length > 0 ) {
			currentFrame = frameset[0];
		}
		else {
			log.error( "Expected Anim not present in Atlas: "+ FIRE_ANIM );
		}

		this.setOpaque( false );
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
			int rW = fireAtlas.getFrameWidth();
			int rH = fireAtlas.getFrameHeight();
			g2d.drawImage( fireAtlas.getSheetImage(), 0, 0, this.getWidth()-1, this.getHeight()-1, rX, rY, rX+rW-1, rY+rH-1, this );
		}
		else {
			Color prevColor = g2d.getColor();

			g2d.setColor( dummyColor );
			g2d.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1 );

			g2d.setColor( prevColor );
		}
	}
}