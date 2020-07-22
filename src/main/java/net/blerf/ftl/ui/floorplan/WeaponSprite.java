package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import net.blerf.ftl.parser.SavedGameParser.WeaponState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class WeaponSprite extends JComponent implements ReferenceSprite<WeaponState> {

	private int longSide = 64, shortSide = 25;

	private SpriteReference<WeaponState> weaponRef;
	private int slot;
	private boolean rotated;
	private String slotString;


	public WeaponSprite( SpriteReference<WeaponState> weaponRef, int slot, boolean rotated ) {
		this.weaponRef = weaponRef;
		this.slot = slot;
		this.slotString = Integer.toString( slot+1 );
		this.rotated = rotated;

		if ( rotated ) {
			this.setPreferredSize( new Dimension( longSide, shortSide ) );
		} else {
			this.setPreferredSize( new Dimension( shortSide, longSide ) );
		}

		this.setOpaque( false );

		weaponRef.addSprite( this );
		referenceChanged();
	}

	public void setSlot( int n ) { slot = n; }
	public int getSlot() { return slot; }

	@Override
	public SpriteReference<WeaponState> getReference() {
		return weaponRef;
	}

	@Override
	public void referenceChanged() {
		//this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		int w = this.getWidth(), h = this.getHeight();
		g2d.drawRect( 0, 0, w-1, h-1 );

		LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics( slotString, g2d );
		int slotStringWidth = g2d.getFontMetrics().stringWidth( slotString );
		int slotStringHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
		int margin = 6;

		if ( rotated ) {
			int slotStringX = (w-1) - slotStringWidth;
			int slotStringY = (h-1)/2 + slotStringHeight/2;  // drawString draws text above Y.
			g2d.drawString( slotString, slotStringX - margin, slotStringY );
		}
		else {
			int slotStringX = (w-1)/2 - slotStringWidth/2;
			int slotStringY = 0 + slotStringHeight;  // drawString draws text above Y.
			g2d.drawString( slotString, slotStringX, slotStringY + margin );
		}
	}
}