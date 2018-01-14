package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;


public class FireSprite extends JComponent {

	private BufferedImage fireImage;
	private int roomId;
	private int squareId;
	private int health;


	public FireSprite( BufferedImage fireImage, int roomId, int squareId, int health ) {
		this.fireImage = fireImage;
		this.roomId = roomId;
		this.squareId = squareId;
		this.health = health;
		this.setOpaque( false );

		this.setPreferredSize( new Dimension( fireImage.getWidth(), fireImage.getHeight() ) );
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
		g2d.drawImage( fireImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this );
	}
}