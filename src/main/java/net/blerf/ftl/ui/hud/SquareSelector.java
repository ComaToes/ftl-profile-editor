package net.blerf.ftl.ui.hud;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;


/**
 * A glasspane-like layer to select squares.
 *
 * Usage: setVisible(), setCriteria(), setCallback()
 * To cancel selection, call reset();
 */
public class SquareSelector<T> extends JComponent {

	private SquareCriteria defaultCriteria = new SquareCriteria();

	private Map<Rectangle, T> squareRegionCoordMap = new LinkedHashMap<Rectangle, T>();
	private SquareCriteria squareCriteria = defaultCriteria;
	private SquareSelectionCallback callback = null;
	private Point mousePoint = new Point( -1, -1 );
	private Rectangle currentRect = null;
	private boolean paintDescription = false;


	public SquareSelector() {
	}

	public void clearSquarsCoordMap() {
		currentRect = null;
		squareRegionCoordMap.clear();
	}

	public void putSquareRegionCoordMap( Map<Rectangle, T> squareRegionCoordMap ) {
		this.squareRegionCoordMap.putAll( squareRegionCoordMap );
	}

	public void setMousePoint( int x, int y ) {
		if ( mousePoint.x != x || mousePoint.y != y ) {
			mousePoint.x = x;
			mousePoint.y = y;

			Rectangle newRect = null;
			if ( mousePoint.x > 0 && mousePoint.y > 0 ) {
				for ( Rectangle k : squareRegionCoordMap.keySet() ) {
					if ( k.contains( mousePoint ) ) {
						newRect = k;
						break;
					}
				}
			}
			if ( newRect != currentRect ) {
				if ( currentRect != null ) this.repaint( currentRect );
				currentRect = newRect;
				if ( currentRect != null ) this.repaint( currentRect );
			}
		}
	}

	public T getSquareCoord() {
		T squareCoord = null;
		if ( squareRegionCoordMap.containsKey( currentRect ) ) {
			squareCoord = squareRegionCoordMap.get( currentRect );
		}
		return squareCoord;
	}

	public Rectangle getSquareRectangle() {
		return currentRect;
	}

	public Point getSquareCenter() {
		Point result = null;
		if ( currentRect != null ) {
			int centerX = currentRect.x + currentRect.width/2;
			int centerY = currentRect.y + currentRect.height/2;
			result = new Point( centerX, centerY );
		}
		return result;
	}

	/** Sets the logic which decides square color and selectability. */
	public void setCriteria( SquareCriteria sc ) {
		if ( sc != null ) {
			squareCriteria = sc;
		} else {
			squareCriteria = defaultCriteria;
		}
	}

	public SquareCriteria getCriteria() { return squareCriteria; }

	public boolean isCurrentSquareValid() {
		return squareCriteria.isSquareValid( this, getSquareCoord() );
	}

	public void setCallback( SquareSelectionCallback cb ) {
		callback = cb;
	}
	public SquareSelectionCallback getCallback() {
		return callback;
	}

	public void setDescriptionVisible( boolean b ) {
		if ( paintDescription != b ) {
			paintDescription = b;
			this.repaint();
		}
	}

	public void reset() {
		this.setVisible( false );
		setDescriptionVisible( false );
		setCriteria( null );
		setCallback( null );
		setMousePoint( -1, -1 );
	}

	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		Color prevColor = g2d.getColor();

		if ( paintDescription && squareCriteria != null ) {
			String desc = squareCriteria.getDescription();
			if ( desc != null ) {
				LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics( desc, g2d );
				int descHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
				int descX = 8;
				int descY = descHeight + 6;
				g2d.setColor( Color.BLACK );
				g2d.drawString( desc, descX, descY );
			}
		}

		if ( currentRect != null ) {
			Color squareColor = squareCriteria.getSquareColor( this, getSquareCoord() );
			if ( squareColor != null ) {
				g2d.setColor( squareColor );
				g2d.drawRect( currentRect.x, currentRect.y, (currentRect.width-1), (currentRect.height-1) );
				g2d.drawRect( currentRect.x+1, currentRect.y+1, (currentRect.width-1)-2, (currentRect.height-1)-2 );
				g2d.drawRect( currentRect.x+2, currentRect.y+2, (currentRect.width-1)-4, (currentRect.height-1)-4 );
			}
		}

		g2d.setColor( prevColor );
	}



	public static class SquareCriteria<T> {
		private Color validColor = Color.GREEN.darker();
		private Color invalidColor = Color.RED.darker();

		/** Returns a message describing what will be selected. */
		public String getDescription() {
			return null;
		}

		/** Returns a highlight color when hovering over a square, or null for none. */
		public Color getSquareColor( SquareSelector squareSelector, T squareCoord ) {
			if ( squareCoord == null ) return null;
			if ( isSquareValid( squareSelector, squareCoord ) ) {
				return validColor;
			} else {
				return invalidColor;
			}
		}

		/** Returns true if a square can be selected, false otherwise. */
		public boolean isSquareValid( SquareSelector squareSelector, T squareCoord ) {
			if ( squareCoord == null ) return false;
			return true;
		}
	}



	public interface SquareSelectionCallback<T> {
		/** Responds to a clicked square, returning true to continue selecting. */
		boolean squareSelected( SquareSelector squareSelector, T squareCoord );
	}
}
