package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import javax.swing.JComponent;

import net.blerf.ftl.parser.SavedGameParser.DroneState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class DroneBoxSprite extends JComponent implements ReferenceSprite<DroneState> {

	private int preferredW = 45, preferredH = 45;

	private SpriteReference<DroneState> droneRef;
	private int slot;
	private String slotString;


	public DroneBoxSprite( SpriteReference<DroneState> droneRef, int slot ) {
		this.droneRef = droneRef;
		this.slot = slot;
		this.slotString = Integer.toString( slot+1 );

		this.setPreferredSize( new Dimension( preferredW, preferredH ) );
		this.setOpaque( false );

		droneRef.addSprite( this );
		referenceChanged();
	}

	public void setSlot( int n ) { slot = n; }
	public int getSlot() { return slot; }

	@Override
	public SpriteReference<DroneState> getReference() {
		return droneRef;
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
		int margin = 4;
		int slotStringX = (w-1)/2 - slotStringWidth/2;
		int slotStringY = (h-1)/2 + slotStringHeight/2;  // drawString draws text above Y.
		g2d.drawString( slotString, slotStringX, slotStringY );
	}
}