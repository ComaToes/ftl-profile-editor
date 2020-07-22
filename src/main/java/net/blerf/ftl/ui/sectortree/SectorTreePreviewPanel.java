package net.blerf.ftl.ui.sectortree;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;

import net.blerf.ftl.model.sectortree.SectorDot;
import net.blerf.ftl.model.sectortree.SectorTree;
import net.blerf.ftl.model.sectortree.SectorTreeEvent;
import net.blerf.ftl.model.sectortree.SectorTreeListener;


public class SectorTreePreviewPanel extends JPanel implements SectorTreeListener {

	protected SectorTree tree = new SectorTree();
	protected boolean treeExpanded = true;
	protected int dotRegionW = 494, dotRegionH = 162;
	protected int dotRadius = 7;

	protected Map<String,Color> dotColorMap = new HashMap<String,Color>();
	protected Color possibleRouteColor = new Color( 255, 255, 255 );
	protected Color traveledRouteColor = new Color( 255, 255, 50 );
	protected Color inaccessibleRouteColor = new Color( 125, 125, 125 );

	private Point aXY = new Point();
	private Point bXY = new Point();
	private List<SectorDot> tmpList = new ArrayList<SectorDot>( 4 );


	public SectorTreePreviewPanel() {
		super();
		this.setLayout( null );

		dotColorMap.put( "CIVILIAN", new Color( 135, 199, 74 ) );
		dotColorMap.put( "HOSTILE", new Color( 214, 50, 50 ) );
		dotColorMap.put( "NEBULA", new Color( 128, 51, 210 ) );
		dotColorMap.put( null, new Color( 50, 50, 50 ) );

		this.setPreferredSize( new Dimension( dotRegionW, dotRegionH ) );
		this.setOpaque( false );
	}


	public void setSectorTree( SectorTree newTree ) {
		tree = newTree;
		this.repaint();
	}

	public SectorTree getSectorTree() {
		return tree;
	}

	/**
	 * Toggles whether the tree will be flattened.
	 *
	 * One dot per column will be drawn, a visited one, if present.
	 * Forward travel will not be possible.
	 */
	public void setTreeExpanded( boolean b ) {
		treeExpanded = b;
	}

	public boolean isTreeExpanded() {
		return treeExpanded;
	}


	@Override
	public void sectorTreeChanged( SectorTreeEvent e ) {
		this.repaint();
	}


	/**
	 * Returns the radius of circles used to represent SectorDots.
	 */
	public int getDotRadius() {
		return dotRadius;
	}


	/**
	 * Returns the center of a dot's visual bounds.
	 */
	public int getDotX( int column, int row ) {
		int colHGap = dotRegionW / tree.getColumnsCount();

		return colHGap/2 + column * colHGap;
	}

	/**
	 * Returns the center of a dot's visual bounds.
	 */
	public int getDotY( int column, int row ) {
		if ( !treeExpanded ) {
			return dotRegionH / 2;
		}
		else {
			int colSize = tree.getColumn( column ).size();

			int colVGap = dotRegionH / (colSize-1 + 2);
			// Two extra gaps (before and after the dots) to vertically center.

			return colVGap + row * colVGap;
		}
	}

	/**
	 * Stores the center of a dot's visual bounds in a Point.
	 */
	public void getDotXY( int column, int row, Point resultPoint ) {
		resultPoint.setLocation( getDotX( column, row ), getDotY( column, row ) );
	}

	/**
	 * Returns the center of a dot's visual bounds in a Point.
	 */
	public boolean getDotXY( SectorDot dot, Point resultPoint ) {
		int column = -1;
		int row = -1;
		for ( int c=tree.getColumnsCount()-1; c >= 0; c-- ) {
			if ( tree.getColumn( c ).contains( dot ) ) {
				column = c;
				row = tree.getColumn( c ).indexOf( dot );
				resultPoint.setLocation( getDotX( column, row ), getDotY( column, row ) );
				return true;
			}
		}
		resultPoint.setLocation( 0, 0 );
		return false;
	}

	/**
	 * Returns the visual bounds of all dots, plus col/row info as Points.
	 *
	 * Each call creates new Rectangles and Points.
	 */
	public Map<Rectangle,Point> getDotsBounds() {
		int dotCount = 0;

		for ( int c=0; c < tree.getColumnsCount(); c++ ) {
			List<SectorDot> columnDots = tree.getColumn( c );
			dotCount += columnDots.size();
		}

		Map<Rectangle,Point> resultMap = new HashMap<Rectangle,Point>( dotCount );

		for ( int c=0; c < tree.getColumnsCount(); c++ ) {
			List<SectorDot> columnDots = tree.getColumn( c );

			for ( int r=0; r < columnDots.size(); r++ ) {
				int boundsX = getDotX( c, r ) - dotRadius;
				int boundsY = getDotY( c, r ) - dotRadius;
				int boundsW = dotRadius * 2;
				int boundsH = dotRadius * 2;
				resultMap.put( new Rectangle( boundsX, boundsY, boundsW, boundsH ), new Point( c, r ) );
			}
		}

		return resultMap;
	}


	@Override
	public void paintComponent( Graphics g ) {
		super.paintComponent( g );

		Graphics2D g2d = (Graphics2D)g;
		Object prevHintAlias = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
		Stroke prevStroke = g2d.getStroke();
		Color prevColor = g2d.getColor();

		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		g2d.setStroke( new BasicStroke( 1.6f ) );

		// Draw routes.
		if ( !treeExpanded ) {
			for ( int aCol=0; aCol < tree.getColumnsCount(); aCol++ ) {
				SectorDot aDot = tree.getVisitedDot( aCol );
				if ( aDot == null ) {
					aDot = tree.getDot( aCol, 0 );  // Get an unvisited dot instead.
				}
				getDotXY( aDot, aXY );

				int bCol = aCol+1;
				if ( bCol < tree.getColumnsCount() ) {
					SectorDot bDot = tree.getVisitedDot( bCol );
					if ( bDot == null ) {
						bDot = tree.getDot( bCol, 0 );  // Get an unvisited dot instead.
					}
					getDotXY( bDot, bXY );

					if ( bDot.isVisited() && aDot.isVisited() ) {
						g2d.setColor( traveledRouteColor );
					}
					else {
						g2d.setColor( inaccessibleRouteColor );
					}

					g2d.drawLine( aXY.x, aXY.y, bXY.x, bXY.y );
				}
			}
		}
		else {
			for ( int aCol=0; aCol < tree.getColumnsCount(); aCol++ ) {
				List<SectorDot> aDots = tree.getColumn( aCol );

				for ( int aRow=0; aRow < aDots.size(); aRow++ ) {
					SectorDot aDot = aDots.get( aRow );
					int aX = getDotX( aCol, aRow );
					int aY = getDotY( aCol, aRow );

					tree.getConnectedDots( aCol, aRow, true, tmpList );
					int bCol = aCol+1;

					for ( SectorDot bDot : tmpList ) {
						int bRow = tree.getColumn( bCol ).indexOf( bDot );
						int bX = getDotX( bCol, bRow );
						int bY = getDotY( bCol, bRow );

						if ( bDot.isVisited() && aDot.isVisited() ) {
							g2d.setColor( traveledRouteColor );
						}
						else if ( tree.isDotAccessible( bCol, bRow ) && tree.isDotAccessible( aCol, aRow ) ) {
							g2d.setColor( possibleRouteColor );
						}
						else {
							g2d.setColor( inaccessibleRouteColor );
						}

						g2d.drawLine( aX, aY, bX, bY );
					}
				}
			}
		}

		// Draw dots.
		if ( !treeExpanded ) {
			for ( int c=0; c < tree.getColumnsCount(); c++ ) {
				SectorDot currentDot = tree.getVisitedDot( c );
				if ( currentDot == null ) {
					currentDot = tree.getDot( c, 0 );  // Get an unvisited dot instead.
				}
				getDotXY( currentDot, aXY );
				Shape dotCircle = new Ellipse2D.Double( aXY.x - dotRadius, aXY.y - dotRadius, 2.0 * dotRadius, 2.0 * dotRadius );

				Color dotColor = dotColorMap.get( currentDot.getType() );
				if ( dotColor == null ) dotColorMap.get( null );
				g2d.setColor( dotColor );

				g2d.fill( dotCircle );


				if ( currentDot.isVisited() ) {
					g2d.setColor( traveledRouteColor );
				}
				else {
					g2d.setColor( inaccessibleRouteColor );
				}

				g2d.draw( dotCircle );
			}
		}
		else {
			for ( int c=0; c < tree.getColumnsCount(); c++ ) {
				List<SectorDot> columnDots = tree.getColumn( c );

				for ( int r=0; r < columnDots.size(); r++ ) {
					SectorDot currentDot = columnDots.get( r );

					int dotX = getDotX( c, r );
					int dotY = getDotY( c, r );
					Shape dotCircle = new Ellipse2D.Double( dotX - dotRadius, dotY - dotRadius, 2.0 * dotRadius, 2.0 * dotRadius );

					Color dotColor = dotColorMap.get( currentDot.getType() );
					if ( dotColor == null ) dotColorMap.get( null );
					g2d.setColor( dotColor );

					g2d.fill( dotCircle );


					if ( currentDot.isVisited() ) {
						g2d.setColor( traveledRouteColor );
					}
					else if ( tree.isDotAccessible( c, r ) ) {
						g2d.setColor( possibleRouteColor );
					}
					else {
						g2d.setColor( inaccessibleRouteColor );
					}

					g2d.draw( dotCircle );
				}
			}
		}

		g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, prevHintAlias );
		g2d.setStroke( prevStroke );
		g2d.setColor( prevColor );
	}


/*
	private static class SectorDotSprite extends JComponent {
		private Color fillColor = new Color( 100, 150, 100 );
		private Color edgeColor = new Color( 150, 100, 100 );
		private Stroke edgeStroke = new BasicStroke( 1.6f );
		private int radius = 7;

		public SectorDotSprite() {
			this.setPreferredSize( new Dimension( radius*2, radius*2 ) );
		}

		public void setFillColor( Color c ) { fillColor = c; }
		public Color getFillColor() { return fillColor; }

		public void setEdgeColor( Color c ) { edgeColor = c; }
		public Color getEdgeColor() { return edgeColor; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			Stroke prevStroke = g2d.getStroke();
			Color prevColor = g2d.getColor();

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setStroke( edgeStroke );

			Shape dotCircle = new Ellipse2D.Double( this.getWidth()/2-radius, this.getHeight()/2-radius, radius*2, radius*2 );
			g2d.setColor( fillColor );
			g2d.fill( dotCircle );

			g2d.setColor( edgeColor );
			g2d.draw( dotCircle );

			g2d.setStroke( prevStroke );
			g2d.setColor( prevColor );
		}
	}
*/
}
