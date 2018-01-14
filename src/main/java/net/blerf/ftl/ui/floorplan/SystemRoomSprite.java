package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import net.blerf.ftl.parser.SavedGameParser.SystemState;
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.ImageUtilities.Tint;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class SystemRoomSprite extends JComponent implements ReferenceSprite<SystemState> {

	// Absent, brown.
	private Tint absentTint = new Tint( new float[] { 0.792f, 0.467f, 0.275f, 1f }, new float[] { 0, 0, 0, 0 } );

	// Ionized, blue.
	private Tint ionizedTint = new Tint( new float[] { 0.51f, 0.898f, 0.937f, 1f }, new float[] { 0, 0, 0, 0 } );

	// Destroyed, red (softer shade than in-game).
	private Tint destroyedTint = new Tint( new float[] { 0.85f, 0.24f, 0.24f, 1f }, new float[] { 0, 0, 0, 0 } );

	// Damaged, orange.
	private Tint damagedTint = new Tint( new float[] { 0.99f, 0.6f, 0.3f, 1f }, new float[] { 0, 0, 0, 0 } );

	// Default, gray.
	private Tint defaultTint = new Tint( new float[] { 0.49f, 0.49f, 0.49f, 1f }, new float[] { 0, 0, 0, 0 } );

	private int scaleW = 32, scaleH = 32;
	private BufferedImage overlayImage;
	private BufferedImage currentImage = null;

	private SpriteReference<SystemState> systemRef;


	public SystemRoomSprite( SpriteReference<SystemState> systemRef, BufferedImage overlayImage ) {
		this.systemRef = systemRef;
		this.overlayImage = overlayImage;

		this.setPreferredSize( new Dimension( scaleW, scaleH ) );
		this.setOpaque( false );

		systemRef.addSprite( this );
		referenceChanged();
	}

	@Override
	public SpriteReference<SystemState> getReference() {
		return systemRef;
	}

	@Override
	public void referenceChanged() {
		// The original overlayImage is white with a black border.
		Tint tint = null;

		if ( systemRef.get().getCapacity() == 0 ) {
			tint = absentTint;
		}
		else if ( systemRef.get().getIonizedBars() > 0 ) {
			tint = ionizedTint;
		}
		else if ( systemRef.get().getDamagedBars() == systemRef.get().getCapacity() ) {
			tint = destroyedTint;
		}
		else if ( systemRef.get().getDamagedBars() > 0 ) {
			tint = damagedTint;
		}
		else {
			tint = defaultTint;
		}

		currentImage = ImageUtilities.getTintedImage( overlayImage, tint, null );

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this );
	}
}