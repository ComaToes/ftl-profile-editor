package net.blerf.ftl.ui.sectortree;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;

import net.blerf.ftl.model.sectortree.SectorDot;
import net.blerf.ftl.model.sectortree.SectorTree;
import net.blerf.ftl.model.sectortree.SectorTreeEvent;
import net.blerf.ftl.model.sectortree.SectorTreeListener;
import net.blerf.ftl.ui.sectortree.SectorTreePreviewPanel;


public class SectorTreeEditPanel extends JLayeredPane implements SectorTreeListener {

	protected static final Integer PREVIEW_LAYER = 0;
	protected static final Integer RETICLE_LAYER = 10;
	protected static final Integer PEEK_LAYER = 20;
	protected static final Integer ROLLBACK_LAYER = 30;
	protected static final Integer TITLE_LAYER = 50;

	protected int marginX = 45+50;
	protected int marginY = 48;
	protected Color peekColor = new Color( 200, 200, 200 );
	protected Color disabledLightStripeColor = new Color( 61, 26, 25 );
	protected Color disabledDarkStripeColor = new Color( 35, 19, 19 );

	protected SectorTreePreviewPanel previewPanel;

	protected Map<JLabel,Integer> titleLblMap = new HashMap<JLabel,Integer>();
	protected Map<JLabel,Integer> peekLblMap = new HashMap<JLabel,Integer>();
	protected Map<JButton,Integer> rollbackBtnMap = new HashMap<JButton,Integer>();

	protected List<NextDotReticle> nextReticleList = new ArrayList<NextDotReticle>();
	protected CurrentDotReticle currentReticle = null;
	protected boolean peekEnabled = true;

	protected MouseListener peekListener;
	protected ActionListener rollbackListener;
	protected MouseListener nextListener;


	public SectorTreeEditPanel( SectorTreePreviewPanel previewPanel ) {
		super();

		this.previewPanel = previewPanel;
		previewPanel.setSize( previewPanel.getPreferredSize() );
		previewPanel.setLocation( marginX, marginY );
		this.add( previewPanel, PREVIEW_LAYER );

		this.setPreferredSize( new Dimension( marginX*2 + previewPanel.getPreferredSize().width, marginY*2 + previewPanel.getPreferredSize().height ) );
		this.setMinimumSize( this.getPreferredSize() );

		peekListener = new MouseAdapter() {
			@Override
			public void mouseEntered( MouseEvent e ) {
				Object source = e.getSource();
				if ( source instanceof JLabel ) {
					JLabel sourceLbl = (JLabel)source;

					Integer columnInt = peekLblMap.get( sourceLbl );
					if ( columnInt == null || columnInt.intValue() >= getPreviewPanel().getSectorTree().getColumnsCount() ) return;

					setPeekColumn( columnInt.intValue() );
				}
				else if ( source instanceof NextDotReticle ) {
					NextDotReticle sourceReticle = (NextDotReticle)source;

					int dotCol = sourceReticle.getDotColumn();
					if ( dotCol == -1 ) return;
					setPeekColumn( dotCol );
				}
			}

			@Override
			public void mouseExited( MouseEvent e ) {
				Object source = e.getSource();
				if ( source instanceof JLabel || source instanceof NextDotReticle ) {
					setPeekColumn( -1 );
				}
			}
		};

		rollbackListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source instanceof JButton == false ) return;

				JButton sourceBtn = (JButton)source;
				Integer columnInt = rollbackBtnMap.get( sourceBtn );
				if ( columnInt == null || columnInt.intValue() >= getPreviewPanel().getSectorTree().getColumnsCount() ) return;

				SectorTree tree = getPreviewPanel().getSectorTree();
				tree.truncateSectorVisitation( columnInt.intValue() );
				tree.fireVisitationChanged();
			}
		};

		nextListener = new MouseAdapter() {
			@Override
			public void mouseEntered( MouseEvent e ) {
				Object source = e.getSource();
				if ( source instanceof NextDotReticle == false ) return;

				NextDotReticle nextReticle = (NextDotReticle)source;
				if ( !nextReticleList.contains( nextReticle ) ) return;

				nextReticle.setHovering( true );
				nextReticle.repaint();
			}

			@Override
			public void mouseExited( MouseEvent e ) {
				Object source = e.getSource();
				if ( source instanceof NextDotReticle == false ) return;

				NextDotReticle nextReticle = (NextDotReticle)source;
				if ( !nextReticleList.contains( nextReticle ) ) return;

				nextReticle.setHovering( false );
				nextReticle.repaint();
			}

			@Override
			public void mouseClicked( MouseEvent e ) {
				Object source = e.getSource();
				if ( source instanceof NextDotReticle == false ) return;

				NextDotReticle nextReticle = (NextDotReticle)source;
				if ( !nextReticleList.contains( nextReticle ) ) return;

				int dotRow = nextReticle.getDotRow();
				if ( dotRow == -1 ) return;
				getPreviewPanel().getSectorTree().setNextVisitedRow( dotRow );
				getPreviewPanel().getSectorTree().fireVisitationChanged();
			}
		};

		sectorTreeChanged( null );
	}

	public SectorTreePreviewPanel getPreviewPanel() {
		return previewPanel;
	}


	/**
	 * Toggles whether hovering over "???" displays SectorDot titles.
	 */
	public void setPeekEnabled( boolean b ) {
		peekEnabled = b;

		for ( JLabel peekLbl : peekLblMap.keySet() ) {
			peekLbl.setVisible( peekEnabled );
		}

		this.repaint();
	}

	public boolean isPeekEnabled() {
		return peekEnabled;
	}


	@Override
	public void sectorTreeChanged( SectorTreeEvent e ) {
		syncColumns();
	}

	private void syncColumns() {
		for ( JLabel titleLbl : titleLblMap.keySet() ) {
			this.remove( titleLbl );
		}
		titleLblMap.clear();

		for ( JLabel peekLbl : peekLblMap.keySet() ) {
			this.remove( peekLbl );
		}
		peekLblMap.clear();

		for ( JButton rollbackBtn : rollbackBtnMap.keySet() ) {
			this.remove( rollbackBtn );
		}
		rollbackBtnMap.clear();

		SectorTree tree = previewPanel.getSectorTree();

		Font labelFont = UIManager.getFont( "Label.font" );
		Font titleFont = labelFont.deriveFont( labelFont.getStyle() & ~Font.BOLD );

		for ( int c=0; c < tree.getColumnsCount(); c++ ) {
			List<SectorDot> columnDots = tree.getColumn( c );
			Integer columnInt = c;
			int columnX = marginX + previewPanel.getDotX( c, 0 );

			for ( int r=0; r < columnDots.size(); r++ ) {
				SectorDot dot = columnDots.get( r );
				JLabel titleLbl = new JLabel( dot.getTitle(), SwingConstants.LEFT );
				titleLbl.setFont( titleFont );
				titleLbl.setForeground( Color.WHITE );
				titleLbl.setBackground( Color.BLACK );
				titleLbl.setOpaque( true );
				titleLbl.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createLineBorder( Color.WHITE ), BorderFactory.createEmptyBorder( 2, 4, 2, 4 ) ) );
				titleLbl.setVisible( false );
				int titleLblX = columnX + 20;
				int titleLblY = marginY + previewPanel.getDotY( c, r ) - titleLbl.getPreferredSize().height/2;
				titleLbl.setSize( titleLbl.getPreferredSize() );
				titleLbl.setLocation( titleLblX, titleLblY );
				titleLblMap.put( titleLbl, columnInt );
				this.add( titleLbl, TITLE_LAYER );
			}

			JLabel peekLbl = new JLabel( "(???)", SwingConstants.CENTER );
			int peekLblX = columnX - peekLbl.getPreferredSize().width/2;
			int peekLblY = marginY - peekLbl.getPreferredSize().height;
			peekLbl.setForeground( peekColor );
			peekLbl.setVisible( peekEnabled );
			peekLbl.setSize( peekLbl.getPreferredSize() );
			peekLbl.setLocation( peekLblX, peekLblY );
			peekLblMap.put( peekLbl, columnInt );
			peekLbl.addMouseListener( peekListener );
			this.add( peekLbl, PEEK_LAYER );

			JButton rollbackBtn = new RollbackButton( BasicArrowButton.WEST );
			rollbackBtn.setToolTipText( "Roll back to this sector" );
			int rollbackBtnX = columnX - rollbackBtn.getPreferredSize().width/2;
			int rollbackBtnY = marginY + previewPanel.getPreferredSize().height;
			rollbackBtn.setSize( rollbackBtn.getPreferredSize() );
			rollbackBtn.setLocation( rollbackBtnX, rollbackBtnY );
			rollbackBtnMap.put( rollbackBtn, columnInt );
			rollbackBtn.addActionListener( rollbackListener );
			this.add( rollbackBtn, ROLLBACK_LAYER );
		}

		syncVisitation();

		this.revalidate();
		this.repaint();
	}

	private void syncVisitation() {
		if ( currentReticle != null ) this.remove( currentReticle );
		currentReticle = null;

		for ( NextDotReticle nextReticle : nextReticleList ) {
			this.remove( nextReticle );
		}
		nextReticleList.clear();

		SectorTree tree = previewPanel.getSectorTree();
		int lastVisitedColumn = tree.getLastVisitedColumn();
		Point dotXY = new Point();

		for ( Map.Entry<JButton,Integer> entry : rollbackBtnMap.entrySet() ) {
			JButton rollbackBtn = entry.getKey();
			int c = entry.getValue().intValue();
			rollbackBtn.setEnabled( (c < lastVisitedColumn) );
		}

		// Current reticle.
		if ( lastVisitedColumn >= 0 && lastVisitedColumn < tree.getColumnsCount() ) {
			currentReticle = new CurrentDotReticle( previewPanel.getDotRadius() );

			SectorDot dot = tree.getVisitedDot( lastVisitedColumn );
			previewPanel.getDotXY( dot, dotXY );
			int reticleX = marginX + dotXY.x - currentReticle.getPreferredSize().width/2;
			int reticleY = marginY + dotXY.y - currentReticle.getPreferredSize().height/2;
			currentReticle.setSize( currentReticle.getPreferredSize() );
			currentReticle.setLocation( reticleX, reticleY );
			this.add( currentReticle );
		}

		// Next reticles.
		if ( previewPanel.isTreeExpanded() && lastVisitedColumn >= 0 && lastVisitedColumn+1 < tree.getColumnsCount() ) {
			List<SectorDot> nearDots = new ArrayList<SectorDot>();
			tree.getConnectedDots( lastVisitedColumn, tree.getLastVisitedRow(), true, nearDots );

			for ( SectorDot dot : nearDots ) {
				NextDotReticle nextReticle = new NextDotReticle( previewPanel.getDotRadius() );
				nextReticle.setDotColumn( tree.getDotColumn( dot ) );
				nextReticle.setDotRow( tree.getDotRow( dot ) );

				previewPanel.getDotXY( dot, dotXY );
				int reticleX = marginX + dotXY.x - nextReticle.getPreferredSize().width/2;
				int reticleY = marginY + dotXY.y - nextReticle.getPreferredSize().height/2;
				nextReticle.setSize( nextReticle.getPreferredSize() );
				nextReticle.setLocation( reticleX, reticleY );
				nextReticleList.add( nextReticle );
				nextReticle.addMouseListener( peekListener );
				nextReticle.addMouseListener( nextListener );
				this.add( nextReticle );
			}
		}

		this.revalidate();
		this.repaint();
	}


	/**
	 * Toggles visibility of sector names to reveal a specific column.
	 */
	public void setPeekColumn( int column ) {
		for ( Map.Entry<JLabel,Integer> entry : titleLblMap.entrySet() ) {
			JLabel titleLbl = entry.getKey();
			int titleCol = entry.getValue().intValue();
			titleLbl.setVisible( (column == titleCol) );
		}
	}



	private class CurrentDotReticle extends JComponent {
		protected Color color = new Color( 255, 255, 50 );
		protected int dotRadius;

		public CurrentDotReticle( int dotRadius ) {
			super();
			this.dotRadius = dotRadius;

			this.setOpaque( false );
			this.setPreferredSize( new Dimension( (dotRadius*2)*2, (dotRadius*2)*2 ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			Object prevHintAlias = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
			Stroke prevStroke = g2d.getStroke();
			Color prevColor = g2d.getColor();

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setStroke( new BasicStroke( 2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 5.0f, new float[] { 5.0f }, 0.0f ) );
			g2d.setColor( color );

			Shape bigCircle = new Ellipse2D.Double( 1, 1, (this.getWidth()-1)-1, (this.getHeight()-1)-1 );
			g2d.draw( bigCircle );

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, prevHintAlias );
			g2d.setStroke( prevStroke );
			g2d.setColor( prevColor );
		}
	}



	private class NextDotReticle extends JComponent {

		protected Color normalColor = new Color( 255, 255, 50 );
		protected Color hoverColor = new Color( 46, 252, 51 );
		protected Color hoverFillColor = hoverColor.darker();

		protected Dimension lastSize = new Dimension();
		protected Dimension tmpSize = new Dimension();
		protected Area normalArea = new Area();
		protected Area hoverArea = new Area();

		protected int dotCol = -1;
		protected int dotRow = -1;
		protected boolean hovering = false;

		protected int drift = 6;  // Amount of separation between quarter-circle arcs.
		protected int dotRadius;

		public NextDotReticle( int dotRadius ) {
			super();
			this.dotRadius = dotRadius;

			this.setOpaque( false );
			this.setPreferredSize( new Dimension( (dotRadius*2)*2, (dotRadius*2)*2 ) );
		}

		public void setDotColumn( int n ) { dotCol = n; }
		public int getDotColumn() { return dotCol; }

		public void setDotRow( int n ) { dotRow = n; }
		public int getDotRow() { return dotRow; }

		public void setHovering( boolean b ) { hovering = b; }

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds( x, y, width, height );

			this.getSize( tmpSize );
			if ( tmpSize.equals(lastSize) ) return;
			lastSize.setSize( tmpSize );

			// Normal area.
			Rectangle normalCircleBounds = new Rectangle( this.getWidth()/2 - dotRadius, this.getHeight()/2 - dotRadius, dotRadius*2, dotRadius*2 );
			Shape normalArcNW = new Arc2D.Double( normalCircleBounds.x - drift, normalCircleBounds.y - drift, normalCircleBounds.width, normalCircleBounds.height, 180, -90, Arc2D.PIE );
			Shape normalArcNE = new Arc2D.Double( normalCircleBounds.x + drift, normalCircleBounds.y - drift, normalCircleBounds.width, normalCircleBounds.height, 90, -90, Arc2D.PIE );
			Shape normalArcSE = new Arc2D.Double( normalCircleBounds.x + drift, normalCircleBounds.y + drift, normalCircleBounds.width, normalCircleBounds.height, 0, -90, Arc2D.PIE );
			Shape normalArcSW = new Arc2D.Double( normalCircleBounds.x - drift, normalCircleBounds.y + drift, normalCircleBounds.width, normalCircleBounds.height, -90, -90, Arc2D.PIE );
			Shape normalHollow = new Ellipse2D.Double( normalCircleBounds.x - (drift/2f+1), normalCircleBounds.y - (drift/2f+1), normalCircleBounds.width + drift+2, normalCircleBounds.height + drift+2 );
			normalArea = new Area();
			normalArea.add( new Area( normalArcNW ) );
			normalArea.add( new Area( normalArcNE ) );
			normalArea.add( new Area( normalArcSE ) );
			normalArea.add( new Area( normalArcSW ) );
			normalArea.subtract( new Area( normalHollow ) );

			// Hover area.
			Rectangle hoverCircleBounds = new Rectangle( this.getWidth()/2 - dotRadius, this.getHeight()/2 - dotRadius, dotRadius*2, dotRadius*2 );
			Shape hoverArcNW = new Arc2D.Double( hoverCircleBounds.x - drift, hoverCircleBounds.y - drift, hoverCircleBounds.width, hoverCircleBounds.height, 180, -90, Arc2D.PIE );
			Shape hoverArcNE = new Arc2D.Double( hoverCircleBounds.x + drift, hoverCircleBounds.y - drift, hoverCircleBounds.width, hoverCircleBounds.height, 90, -90, Arc2D.PIE );
			Shape hoverArcSE = new Arc2D.Double( hoverCircleBounds.x + drift, hoverCircleBounds.y + drift, hoverCircleBounds.width, hoverCircleBounds.height, 0, -90, Arc2D.PIE );
			Shape hoverArcSW = new Arc2D.Double( hoverCircleBounds.x - drift, hoverCircleBounds.y + drift, hoverCircleBounds.width, hoverCircleBounds.height, -90, -90, Arc2D.PIE );
			Shape hoverHollow = new Ellipse2D.Double( hoverCircleBounds.x - (drift/2f+1), hoverCircleBounds.y - (drift/2f+1), hoverCircleBounds.width + drift+2, hoverCircleBounds.height + drift+2 );
			hoverArea = new Area();
			hoverArea.add( new Area( hoverArcNW ) );
			hoverArea.add( new Area( hoverArcNE ) );
			hoverArea.add( new Area( hoverArcSE ) );
			hoverArea.add( new Area( hoverArcSW ) );
			hoverArea.subtract( new Area( hoverHollow ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			Object prevHintAlias = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
			Color prevColor = g2d.getColor();

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			if ( hovering ) {
				g2d.setColor( hoverFillColor );
				g2d.fill( hoverArea );

				g2d.setColor( hoverColor );
				g2d.draw( hoverArea );
			}
			else {
				g2d.setColor( normalColor );
				g2d.draw( normalArea );
			}

			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, prevHintAlias );
			g2d.setColor( prevColor );
		}
	}



	private static class RollbackButton extends BasicArrowButton {

		public RollbackButton( int direction ) {
			super( direction );
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension( 23, 23 );
		}
	}
}
