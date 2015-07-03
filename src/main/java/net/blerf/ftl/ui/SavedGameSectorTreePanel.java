package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.blerf.ftl.model.sectortree.SectorDot;
import net.blerf.ftl.model.sectortree.SectorTree;
import net.blerf.ftl.model.sectortree.SectorTreeEvent;
import net.blerf.ftl.model.sectortree.SectorTreeException;
import net.blerf.ftl.model.sectortree.SectorTreeListener;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.random.GNULibCRandom;
import net.blerf.ftl.parser.random.MsRandom;
import net.blerf.ftl.parser.random.NativeRandom;
import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.sectortree.LinearSectorTreeGenerator;
import net.blerf.ftl.parser.sectortree.RandomSectorTreeGenerator;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.sectortree.SectorTreePreviewPanel;
import net.blerf.ftl.ui.sectortree.SectorTreeEditPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameSectorTreePanel extends JPanel implements ActionListener {

	private static final Logger log = LogManager.getLogger(SavedGameSectorTreePanel.class);

	private static final String ALGORITHM = "RNG Algorithm";
	private static final String TREE_TYPE = "Preview";
	private static final String SECTOR_TREE_SEED = "Sector Tree Seed";
	private static final String SECTOR_NUMBER = "Sector Number";

	private static final String TREE_TYPE_LINEAR = "Linear";
	private static final String TREE_TYPE_EXPANDED = "Expanded";

	private FTLFrame frame;

	private LinearSectorTreeGenerator linearTreeGen = null;
	private RandomSectorTreeGenerator expandedTreeGen = null;
	private SectorTree tree = null;
	private SectorTreePreviewPanel treePreviewPanel = null;
	private SectorTreeEditPanel treeEditPanel = null;

	private FieldEditorPanel genPanel = null;
	private FieldEditorPanel miscPanel = null;
	private JButton genApplyBtn = null;
	private JButton miscRandomSeedBtn = null;
	private JButton miscRestoreBtn = null;

	private Font noticeFont = new Font( "Monospaced", Font.PLAIN, 13 );

	private Random javaRandom = new Random();
	private boolean dlcEnabled = true;
	private int sectorTreeSeed = 0;

	private int originalSectorTreeSeed = 0;
	private List<Boolean> originalRoute = null;
	private boolean seedChanged = false;


	public SavedGameSectorTreePanel( FTLFrame frame ) {
		this.setLayout( new GridBagLayout() );

		this.frame = frame;

		tree = new SectorTree();

		treePreviewPanel = new SectorTreePreviewPanel();
		treePreviewPanel.setTreeExpanded( false );
		treePreviewPanel.setSectorTree( tree );
		tree.addSectorTreeListener( treePreviewPanel );

		treeEditPanel = new SectorTreeEditPanel( treePreviewPanel );
		treeEditPanel.setBackground( new Color( 31, 19, 19 ) );
		treeEditPanel.setOpaque( true );
		tree.addSectorTreeListener( treeEditPanel );

		JPanel treeEditHolderPanel = new JPanel( new BorderLayout() );
		treeEditHolderPanel.setBorder( BorderFactory.createEtchedBorder() );
		treeEditHolderPanel.add( treeEditPanel, BorderLayout.CENTER );

		genPanel =  new FieldEditorPanel( true );
		genPanel.setBorder( BorderFactory.createTitledBorder("") );
		genPanel.addRow( ALGORITHM, FieldEditorPanel.ContentType.COMBO );
		genPanel.addBlankRow();
		genPanel.addRow( TREE_TYPE, FieldEditorPanel.ContentType.COMBO );
		genPanel.addBlankRow();
		genPanel.addRow( SECTOR_TREE_SEED, FieldEditorPanel.ContentType.INTEGER );
		genPanel.getInt(SECTOR_TREE_SEED).setDocument( new RegexDocument("-?[0-9]*") );
		genPanel.addBlankRow();
		genApplyBtn = new JButton( "Apply" );
		genPanel.addComponent( genApplyBtn );
		genPanel.addBlankRow();
		genPanel.addFillRow();

		genPanel.getCombo(ALGORITHM).addMouseListener( new StatusbarMouseListener(frame, "An algorithm of the OS the saved game was created under, or native for the current OS.") );
		genPanel.getCombo(TREE_TYPE).addMouseListener( new StatusbarMouseListener(frame, "The type of tree to generate.") );
		genPanel.getInt(SECTOR_TREE_SEED).addMouseListener( new StatusbarMouseListener(frame, "A per-game constant that seeds the random generation of the sector tree.") );
		genApplyBtn.addMouseListener( new StatusbarMouseListener(frame, "Generate a sector tree with the given algorithm, type, and seed.") );

		miscPanel =  new FieldEditorPanel( true );
		miscPanel.setBorder( BorderFactory.createTitledBorder("") );
		miscRandomSeedBtn = new JButton( "Random Seed" );
		miscPanel.addComponent( miscRandomSeedBtn );
		miscPanel.addBlankRow();
		miscRestoreBtn = new JButton( "Restore Original Tree" );
		miscPanel.addComponent( miscRestoreBtn );
		miscPanel.addBlankRow();
		miscPanel.addFillRow();

		miscRandomSeedBtn.addMouseListener( new StatusbarMouseListener(frame, "Generate an entirely new sector tree.") );
		miscRestoreBtn.addMouseListener( new StatusbarMouseListener(frame, "Restore the sector tree that was last opened/saved.") );

		StringBuilder noticeBuf = new StringBuilder();
		noticeBuf.append( "Saved games contain a seed for building the sector tree, " );
		noticeBuf.append( "which FTL sends to the OS's random number generator. " );
		noticeBuf.append( "As such, the tree is fragile: platform-dependent and mod-sensitive.\n" );
		noticeBuf.append( "\n" );
		noticeBuf.append( "This editor can reconstruct the tree using various algorithms, " );
		noticeBuf.append( "or display a flat line for backtracking.\n");
		noticeBuf.append( "\n" );
		noticeBuf.append( "Invoking an RNG (switching to the expanded view or setting a new seed) " );
		noticeBuf.append( "may risk creating visitation breadcrumbs inconsistent with the tree, " );
		noticeBuf.append( "if FTL interprets the seed differently in-game. " );
		noticeBuf.append( "In other words, charting a glitchy course through unexpected sectors.\n" );
		noticeBuf.append( "\n" );
		noticeBuf.append( "A linear preview with the original seed should always be safe." );

		JPanel noticePanel = new JPanel( new BorderLayout() );
		noticePanel.setBorder( BorderFactory.createEtchedBorder() );
		JTextArea noticeArea = new JTextArea();
		noticeArea.setText( noticeBuf.toString() );
		noticeArea.setBackground( null );
		noticeArea.setFont( noticeFont );
		noticeArea.setEditable( false );
		noticeArea.setBorder( BorderFactory.createEmptyBorder( 10, 10, 10, 10 ) );
		noticeArea.setLineWrap( true );
		noticeArea.setWrapStyleWord( true );
		noticeArea.setFocusable( false );
		noticePanel.add( noticeArea, BorderLayout.CENTER );

		GridBagConstraints thisC = new GridBagConstraints();
		thisC.anchor = GridBagConstraints.NORTH;
		thisC.fill = GridBagConstraints.BOTH;
		thisC.weightx = 0.0;
		thisC.weighty = 0.0;
		thisC.gridwidth = GridBagConstraints.REMAINDER;
		thisC.gridx = 0;
		thisC.gridy = 0;
		this.add( treeEditHolderPanel, thisC );

		thisC.gridwidth = 1;
		thisC.gridy++;
		this.add( genPanel, thisC );

		thisC.gridwidth = 1;
		thisC.gridx++;
		this.add( miscPanel, thisC );

		thisC.fill = GridBagConstraints.HORIZONTAL;
		thisC.weightx = 1.0;
		thisC.weighty = 0.0;
		thisC.gridwidth = GridBagConstraints.REMAINDER;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( noticePanel, thisC );

		thisC.fill = GridBagConstraints.BOTH;
		thisC.weighty = 1.0;
		thisC.gridwidth = GridBagConstraints.REMAINDER;
		thisC.gridx = 0;
		thisC.gridy++;
		this.add( Box.createVerticalGlue(), thisC );

		genApplyBtn.addActionListener( this );

		miscRandomSeedBtn.addActionListener( this );
		miscRestoreBtn.addActionListener( this );

		tree.addSectorTreeListener(new SectorTreeListener() {
			@Override
			public void sectorTreeChanged( SectorTreeEvent e ) {
			}
		});

		linearTreeGen = new LinearSectorTreeGenerator();
		expandedTreeGen = new RandomSectorTreeGenerator( new NativeRandom() );
	}


	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

		if ( source == genApplyBtn ) {
			try {
				int newSeed = genPanel.parseInt(SECTOR_TREE_SEED);

				seedChanged = ( newSeed != originalSectorTreeSeed );
				setSeed( newSeed );
			}
			catch ( NumberFormatException f ) {
			}
		}
		else if ( source == miscRandomSeedBtn ) {
			int newSeed = javaRandom.nextInt( Integer.MAX_VALUE );

			genPanel.getInt(SECTOR_TREE_SEED).setText( ""+ newSeed );
			seedChanged = true;
			setSeed( newSeed );
		}
		else if ( source == miscRestoreBtn ) {
			genPanel.getCombo(TREE_TYPE).setSelectedItem( TREE_TYPE_LINEAR );

			genPanel.getInt(SECTOR_TREE_SEED).setText( ""+ originalSectorTreeSeed );
			sectorTreeSeed = 0;
			seedChanged = false;

			treePreviewPanel.setTreeExpanded( false );
			treeEditPanel.setPeekEnabled( false );
			tree.clear();
			tree.fireColumnsChanged();
		}
	}


	private void setSeed( int newSeed ) {
		Object selectedNamedRNGObj = genPanel.getCombo(ALGORITHM).getSelectedItem();
		if ( selectedNamedRNGObj == null ) {
			log.warn( "No RNG selected to generate a sector tree!?" );
			return;
		}

		@SuppressWarnings("unchecked")
		NamedRNG selectedNamedRNG = (NamedRNG)selectedNamedRNGObj;
		RandRNG selectedRNG = selectedNamedRNG.getRNG();

		if ( selectedRNG != expandedTreeGen.getRNG() ) {
			expandedTreeGen.setRNG( selectedRNG );
		}


		if ( TREE_TYPE_LINEAR.equals(genPanel.getCombo(TREE_TYPE).getSelectedItem()) ) {
			treePreviewPanel.setTreeExpanded( false );
			treeEditPanel.setPeekEnabled( false );

			if ( !seedChanged ) {
				List<Boolean> route = originalRoute;

				List<List<SectorDot>> linearColumns = linearTreeGen.generateSectorTree( route, 8 );
				tree.setSectorDots( linearColumns );
			}
			else {
				List<List<SectorDot>> expandedColumns = expandedTreeGen.generateSectorTree( newSeed, dlcEnabled );

				SectorTree tmpTree = new SectorTree();
				tmpTree.setSectorDots( expandedColumns );
				List<Boolean> tmpRoute = tmpTree.getSectorVisitation();

				List<List<SectorDot>> linearColumns = linearTreeGen.generateSectorTree( tmpRoute, 8 );
				tree.setSectorDots( linearColumns );
				tree.setNextVisitedRow( 0 );
			}
		}
		else if ( TREE_TYPE_EXPANDED.equals(genPanel.getCombo(TREE_TYPE).getSelectedItem()) ) {

			List<List<SectorDot>> dotColumns = expandedTreeGen.generateSectorTree( newSeed, dlcEnabled );

			if ( !seedChanged ) {
				try {
					List<Boolean> route = originalRoute;

					if ( SectorTree.countDots( dotColumns ) != route.size() ) {
						throw new SectorTreeException( "The RNG produced a sector tree with a dot count that does not match the visitation list size." );
					}
					SectorTree.setVisitation( dotColumns, route );
					SectorTree.validate( dotColumns );
				}
				catch ( Exception e ) {
					log.error( e );
					JOptionPane.showMessageDialog( this, "An error occurred while attempting to reconstruct the sector tree with existing visitation breadcrumbs.\n\nThis saved game probably used a different algorithm. See log for details.", "Sector Tree", JOptionPane.ERROR_MESSAGE );
					return;
				}
			}

			tree.setSectorDots( dotColumns );

			treePreviewPanel.setTreeExpanded( true );
			treeEditPanel.setPeekEnabled( true );
			tree.setNextVisitedRow( 0 );
		}

		sectorTreeSeed = newSeed;

		tree.fireColumnsChanged();
	}


	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		dlcEnabled = true;
		sectorTreeSeed = 0;
		originalSectorTreeSeed = 0;
		originalRoute = null;
		seedChanged = false;
		genPanel.reset();
		miscPanel.reset();

		treePreviewPanel.setTreeExpanded( false );
		treeEditPanel.setPeekEnabled( false );
		tree.clear();
		tree.fireColumnsChanged();

		if ( gameState != null ) {
			genPanel.getCombo(ALGORITHM).addItem( new NamedRNG( new NativeRandom(), "Native" ) );
			genPanel.getCombo(ALGORITHM).addItem( new NamedRNG( new GNULibCRandom(), "GLibC (Linux/OSX)" ) );
			genPanel.getCombo(ALGORITHM).addItem( new NamedRNG( new MsRandom(), "Microsoft" ) );

			genPanel.getCombo(TREE_TYPE).addItem( TREE_TYPE_LINEAR );
			genPanel.getCombo(TREE_TYPE).addItem( TREE_TYPE_EXPANDED );
			genPanel.getCombo(TREE_TYPE).setSelectedItem( TREE_TYPE_LINEAR );

			dlcEnabled = gameState.isDLCEnabled();

			originalSectorTreeSeed = gameState.getSectorTreeSeed();
			originalRoute = new ArrayList<Boolean>( gameState.getSectorVisitation() );

			genPanel.setIntAndReminder( SECTOR_TREE_SEED, gameState.getSectorTreeSeed() );
		}

		// Scroll back to the top. (The notice area's wrap pulls the viewport down?)

		SavedGameSectorTreePanel.this.scrollRectToVisible( new Rectangle(0,0,0,0) );
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		if ( tree.isEmpty() ) {
			gameState.setSectorTreeSeed( originalSectorTreeSeed );

			int sectorNum = Collections.frequency( originalRoute, Boolean.TRUE ) - 1;
			if ( sectorNum < 0 ) sectorNum = 0;

			gameState.setSectorNumber( sectorNum );
			gameState.setSectorVisitation( originalRoute );
		}
		else {
			gameState.setSectorTreeSeed( sectorTreeSeed );

			int sectorNum = tree.getLastVisitedColumn();
			if ( sectorNum < 0 ) sectorNum = 0;

			gameState.setSectorNumber( sectorNum );
			gameState.setSectorVisitation( tree.getSectorVisitation() );
		}
	}



	private static class NamedRNG {
		protected final RandRNG rng;
		protected final String name;

		public NamedRNG( RandRNG rng, String name ) {
			this.rng = rng;
			this.name = name;
		}

		public RandRNG getRNG() { return rng; }
		public String toString() { return name; }
	}
}
