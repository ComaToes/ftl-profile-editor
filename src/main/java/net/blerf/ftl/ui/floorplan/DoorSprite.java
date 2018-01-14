package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.swing.JComponent;

import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class DoorSprite extends JComponent implements ReferenceSprite<DoorState> {

	private BufferedImage currentImage = null;

	private SpriteReference<DoorState> doorRef;
	private Map<Integer, BufferedImage> closedImages;
	private Map<Integer, BufferedImage> openImages;
	private Integer level;
	private DoorCoordinate doorCoord;


	public DoorSprite( SpriteReference<DoorState> doorRef, Map<Integer, BufferedImage> closedImages, Map<Integer, BufferedImage> openImages, Integer level, DoorCoordinate doorCoord ) {
		this.doorRef = doorRef;
		this.closedImages = closedImages;
		this.openImages = openImages;
		this.level = level;
		this.doorCoord = doorCoord;

		int chop = 10;
		int longSide = 35, shortSide = 35-(chop*2);
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
		// Test if doorState's health fields are unreliable and check the
		// Doors system capacity.

		if ( doorRef.get().isOpen() ) {
			currentImage = openImages.get( level );
		} else {
			currentImage = closedImages.get( level );
		}

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		Color prevColor = g2d.getColor();
		int w = this.getWidth(), h = this.getHeight();

		if ( doorCoord.v == 0 ) {  // Use rotated coordinates to draw AS IF vertical.
			g2d.rotate( Math.toRadians( 90 ) );   // Clockwise.
			w = this.getHeight(); h = this.getWidth();
			g2d.translate( 0, -h );
		}

		// The big image may not have had enough rows to populate all potential Door levels.
		if ( currentImage != null ) {
			g2d.drawImage( currentImage, 0, 0, this );
		}

		g2d.setColor( prevColor );
	}
}
