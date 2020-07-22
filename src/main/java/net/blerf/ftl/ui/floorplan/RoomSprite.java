package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

import net.blerf.ftl.parser.SavedGameParser.RoomState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class RoomSprite extends JComponent implements ReferenceSprite<RoomState> {

	private final Color maxColor = new Color( 230, 226, 219 );
	private final Color minColor = new Color( 255, 176, 169 );
	private final Color vacuumBorderColor = new Color( 255, 180, 0 );
	private Color bgColor = maxColor;

	private SpriteReference<RoomState> roomRef;
	private int roomId;


	public RoomSprite( SpriteReference<RoomState> roomRef, int roomId ) {
		this.roomRef = roomRef;
		this.roomId = roomId;

		// No preferred size.
		this.setOpaque( true );

		roomRef.addSprite( this );
		referenceChanged();
	}

	public void setRoomId( int n ) { roomId = n; }
	public int getRoomId() { return roomId; }

	@Override
	public SpriteReference<RoomState> getReference() {
		return roomRef;
	}

	@Override
	public void referenceChanged() {
		if ( roomRef.get().getOxygen() == 100 ) {
			bgColor = maxColor;
		}
		else if ( roomRef.get().getOxygen() == 0 ) {
			bgColor = minColor;
		}
		else {
			double p = roomRef.get().getOxygen() / 100.0;
			int maxRed = maxColor.getRed();
			int maxGreen = maxColor.getGreen();
			int maxBlue = maxColor.getBlue();
			int minRed = minColor.getRed();
			int minGreen = minColor.getGreen();
			int minBlue = minColor.getBlue();
			bgColor = new Color( (int)(minRed+p*(maxRed-minRed)), (int)(minGreen+p*(maxGreen-minGreen)), (int)(minBlue+p*(maxBlue-minBlue)) );
		}

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		Color prevColor = g2d.getColor();

		g2d.setColor( bgColor );
		g2d.fillRect( 0, 0, this.getWidth(), this.getHeight() );

		if ( roomRef.get().getOxygen() == 0 ) {  // Draw the yellow border.
			g2d.setColor( vacuumBorderColor );
			g2d.drawRect( 2, 2, this.getWidth()-4 - 1, this.getHeight()-4 - 1 );
			g2d.drawRect( 3, 3, this.getWidth()-6 - 1, this.getHeight()-6 - 1 );

			// TODO: Draw pink stripes across floor instead of original-style outline.
		}

		g2d.setColor( prevColor );
	}
}