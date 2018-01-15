package net.blerf.ftl.ui.floorplan;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.parser.SavedGameParser.CrewState;
import net.blerf.ftl.ui.floorplan.SpriteImageProvider;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;


public class CrewSprite extends JComponent implements ReferenceSprite<CrewState> {

	private int w=35, h=35;
	private BufferedImage crewImage = null;

	private SpriteReference<CrewState> crewRef;
	private SpriteImageProvider spriteImageProvider;


	public CrewSprite( SpriteReference<CrewState> crewRef, SpriteImageProvider spriteImageProvider ) {
		this.crewRef = crewRef;
		this.spriteImageProvider = spriteImageProvider;

		this.setPreferredSize( new Dimension( w, h ) );
		this.setOpaque( false );

		crewRef.addSprite( this );
		referenceChanged();
	}

	@Override
	public SpriteReference<CrewState> getReference() {
		return crewRef;
	}

	@Override
	public void referenceChanged() {
		crewImage = spriteImageProvider.getCrewBodyImage( crewRef.get().getRace(), crewRef.get().isMale(), crewRef.get().isPlayerControlled() );

		this.repaint();
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		g2d.drawImage( crewImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this );
	}

	@Override
	public String toString() {
		return String.format( "%s (%s, %d HP)", crewRef.get().getName(), crewRef.get().getRace().getId(), crewRef.get().getHealth() );
	}
}