package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class BreachSprite extends JComponent {

	private BufferedImage breachImage;
	private int roomId;
	private int squareId;
	private int health;


	public BreachSprite( BufferedImage breachImage, int roomId, int squareId, int health ) {
		this.breachImage = breachImage;
		this.roomId = roomId;
		this.squareId = squareId;
		this.health = health;
		this.setOpaque( false );

		this.setPreferredSize( new Dimension( breachImage.getWidth(), breachImage.getHeight() ) );
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
		g2d.drawImage( breachImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this );
	}
}