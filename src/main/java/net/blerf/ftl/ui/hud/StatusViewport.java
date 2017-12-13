package net.blerf.ftl.ui.hud;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JViewport;


public class StatusViewport extends JViewport {
	private Color statusBgColor = new Color( 212, 208, 200 );
	private String statusString = null;

	public void setStatusString( String s ) {
		if ( statusString != s ) {
			statusString = s;
			this.repaint();
		}
	}
	public String getStatusString() { return statusString; }

	@Override
	public void paintChildren( Graphics g ) {
		super.paintChildren( g );
		Graphics2D g2d = (Graphics2D)g;
		Color prevColor = g2d.getColor();

		if ( statusString != null ) {
			LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics( statusString, g2d );
			int statusWidth = g2d.getFontMetrics().stringWidth( statusString );
			int statusHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
			int statusX = 8;
			int statusY = statusHeight + 6;
			g2d.setColor( statusBgColor );
			g2d.fillRect( statusX-3, statusY-((int)lineMetrics.getAscent())-3, statusWidth+6, statusHeight+6 );
			g2d.setColor( Color.BLACK );
			g2d.drawString( statusString, statusX, statusY );
		}

		g2d.setColor( prevColor );
	}
}
