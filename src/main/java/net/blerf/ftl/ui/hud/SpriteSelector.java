package net.blerf.ftl.ui.hud;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;


/**
 * A glasspane-like layer to select scattered non-overlapping sprites.
 *
 * Usage: setVisible(), setCriteria(), setCallback()
 * To cancel selection, call reset();
 */
public class SpriteSelector extends JComponent {

	private SpriteCriteria defaultCriteria = new SpriteCriteria();

	private ArrayList<List<? extends JComponent>> spriteLists = new ArrayList<List<? extends JComponent>>();
	private SpriteCriteria spriteCriteria = defaultCriteria;
	private SpriteSelectionCallback callback = null;
	private Point mousePoint = new Point( -1, -1 );
	private JComponent currentSprite = null;
	private boolean paintDescription = false;
	private Color descriptionBgColor = new Color( 212, 208, 200 );


	public SpriteSelector() {
	}

	public void clearSpriteLists() {
		currentSprite = null;
		spriteLists.clear();
	}

	public void addSpriteList( List<? extends JComponent> spriteList ) {
		spriteLists.add( spriteList );
	}

	public void setMousePoint( int x, int y ) {
		if ( mousePoint.x != x || mousePoint.y != y) {
			mousePoint.x = x;
			mousePoint.y = y;

			JComponent newSprite = null;
			if ( mousePoint.x > 0 && mousePoint.y > 0 ) {
				for ( List<? extends JComponent> spriteList : spriteLists ) {
					for ( JComponent sprite : spriteList ) {
						if ( getConvertedSpriteBounds( sprite ).contains( mousePoint ) ) {
							newSprite = sprite;
							break;
						}
					}
					if ( newSprite != null ) break;
				}
			}
			if ( newSprite != currentSprite ) {
				if ( currentSprite != null ) this.repaint( getConvertedSpriteBounds( currentSprite ) );
				currentSprite = newSprite;
				if ( currentSprite != null ) this.repaint( getConvertedSpriteBounds( currentSprite ) );
			}
		}
	}

	private Rectangle getConvertedSpriteBounds( JComponent sprite ) {
		Rectangle origRect = sprite.getBounds();
		Rectangle convertedRect = SwingUtilities.convertRectangle( sprite.getParent(), origRect, this );
		return convertedRect;
	}

	public JComponent getSprite() {
		return currentSprite;
	}

	/** Sets the logic which decides sprite color and selectability. */
	public void setCriteria( SpriteCriteria sc ) {
		if ( sc != null )
			spriteCriteria = sc;
		else
			spriteCriteria = defaultCriteria;
	}

	public SpriteCriteria getCriteria() { return spriteCriteria; }

	public boolean isCurrentSpriteValid() {
		return spriteCriteria.isSpriteValid( this, currentSprite );
	}

	public void setCallback( SpriteSelectionCallback cb ) {
		callback = cb;
	}
	public SpriteSelectionCallback getCallback() {
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

		if ( paintDescription && spriteCriteria != null ) {
			String desc = spriteCriteria.getDescription();
			if ( desc != null ) {
				LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics( desc, g2d );
				int descWidth = g2d.getFontMetrics().stringWidth( desc );
				int descHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
				int descX = 8;
				int descY = descHeight + 6;
				g2d.setColor( descriptionBgColor );
				g2d.fillRect( descX-3, descY-((int)lineMetrics.getAscent())-3, descWidth+6, descHeight+6 );
				g2d.setColor( Color.BLACK );
				g2d.drawString( desc, descX, descY );
			}
		}

		if ( currentSprite != null ) {
			Color spriteColor = spriteCriteria.getSpriteColor( this, currentSprite );
			if ( spriteColor != null ) {
				Rectangle currentRect = getConvertedSpriteBounds( currentSprite );
				g2d.setColor( spriteColor );
				g2d.drawRect( currentRect.x, currentRect.y, (currentRect.width-1), (currentRect.height-1) );
				g2d.drawRect( currentRect.x+1, currentRect.y+1, (currentRect.width-1)-2, (currentRect.height-1)-2 );
				g2d.drawRect( currentRect.x+2, currentRect.y+2, (currentRect.width-1)-4, (currentRect.height-1)-4 );
			}
		}

		g2d.setColor( prevColor );
	}



	public static class SpriteCriteria {
		protected Color validColor = Color.GREEN.darker();
		protected Color invalidColor = Color.RED.darker();

		/** Returns a message describing what will be selected. */
		public String getDescription() {
			return null;
		}

		/** Returns a highlight color when hovering over a sprite, or null for none. */
		public Color getSpriteColor( SpriteSelector spriteSelector, JComponent sprite ) {
			if ( sprite == null ) return null;
			if ( isSpriteValid( spriteSelector, sprite ) )
				return validColor;
			else
				return invalidColor;
		}

		/** Returns true if a square can be selected, false otherwise. */
		public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
			if ( sprite == null || !sprite.isVisible() ) return false;
			return true;
		}
	}



	public interface SpriteSelectionCallback {
		/** Responds to a clicked sprite, returning true to continue selecting. */
		boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite );
	}
}
