package net.blerf.ftl.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicArrowButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.constants.OriginalFTLConstants;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.BeaconState;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.FleetPresence;
import net.blerf.ftl.parser.SavedGameParser.RebelFlagshipState;
import net.blerf.ftl.parser.SavedGameParser.StoreShelf;
import net.blerf.ftl.parser.SavedGameParser.StoreState;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.parser.random.FTL_1_6_Random;
import net.blerf.ftl.parser.random.GNULibCRandom;
import net.blerf.ftl.parser.random.MsRandom;
import net.blerf.ftl.parser.random.NativeRandom;
import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.sectormap.GeneratedBeacon;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
import net.blerf.ftl.parser.sectormap.GridSectorMapGenerator;
import net.blerf.ftl.parser.sectormap.RandomSectorMapGenerator;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.RegexDocument;
import net.blerf.ftl.ui.SectorMapLayout;
import net.blerf.ftl.ui.SectorMapLayout.SectorMapConstraints;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.ui.StoreShelfPanel;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.StatusViewport;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.BackgroundImage;
import net.blerf.ftl.xml.BackgroundImageList;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.ShipEvent;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;


public class SavedGameSectorMapPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger( SavedGameSectorMapPanel.class );

	// Dimensions for placing beacons' background sprite images.
	private static final int SCREEN_WIDTH = 1280;
	private static final int SCREEN_HEIGHT = 720;

	private static final int GRID_GEN_COLS = 6;
	private static final int GRID_GEN_ROWS = 4;
	private static final int GRID_GEN_COL_W = 116;
	private static final int GRID_GEN_ROW_H = 103;

	// mapHolderPanel Layers
	private static final Integer MAP_LAYER = 10;
	private static final Integer MISC_SELECTION_LAYER = 50;

	private FTLFrame frame;

	private List<SpriteReference<BeaconState>> beaconRefs = new ArrayList<SpriteReference<BeaconState>>();

	private List<BeaconSprite> beaconSprites = new ArrayList<BeaconSprite>();
	private List<StoreSprite> storeSprites = new ArrayList<StoreSprite>();
	private List<QuestSprite> questSprites = new ArrayList<QuestSprite>();
	private List<PlayerShipSprite> playerShipSprites = new ArrayList<PlayerShipSprite>();

	private Map<String, Map<Rectangle, BufferedImage>> cachedImages = new HashMap<String, Map<Rectangle, BufferedImage>>();

	private Random javaRandom = new Random();
	private int fileFormat = 2;
	private FTLConstants ftlConstants = new AdvancedFTLConstants();

	private RandRNG forcedRNG = null;
	private int sectorLayoutSeed = 1;

	private int rebelFleetOffset = -750;  // Arbitrary default.
	private int rebelFleetFudge = 100;    // Arbitrary default.
	private int rebelPursuitMod = 0;
	private boolean hiddenCrystalWorlds = false;
	private boolean hazardsVisible = false;

	private boolean flagshipVisible = false;
	private int flagshipHop = 0;
	private boolean flagshipMoving = false;
	private boolean flagshipRetreating = false;
	private int flagshipBaseTurns = 0;
	private boolean flagshipNearby = false;
	private RebelFlagshipState flagship = null;

	private SectorMapLayout mapLayout = null;

	private JPanel mapPanel = null;
	private JLayeredPane mapHolderPanel = null;
	private StatusViewport mapViewport = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private SpriteSelector miscSelector = null;
	private ActionListener columnCtrlListener = null;


	public SavedGameSectorMapPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		mapHolderPanel = new JLayeredPane();

		mapLayout = new SectorMapLayout();

		mapPanel = new JPanel( mapLayout );
		mapPanel.setBackground( Color.BLACK );
		mapPanel.setOpaque( true );
		mapPanel.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( mapPanel, MAP_LAYER );

		sidePanel = new JPanel();
		sidePanel.setLayout( new BoxLayout( sidePanel, BoxLayout.Y_AXIS ) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 6 ) );

		miscSelector = new SpriteSelector();
		miscSelector.setOpaque( false );
		miscSelector.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( miscSelector, MISC_SELECTION_LAYER );

		MouseInputAdapter miscListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved( MouseEvent e ) {
				miscSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked( MouseEvent e ) {
				// Left-click triggers callback. Other buttons cancel.

				if ( e.getButton() == MouseEvent.BUTTON1 ) {
					if ( !miscSelector.isCurrentSpriteValid() ) return;
					boolean keepSelecting = false;
					SpriteSelectionCallback callback = miscSelector.getCallback();
					if ( callback != null )
						keepSelecting = callback.spriteSelected( miscSelector, miscSelector.getSprite() );
					if ( keepSelecting == false )
						miscSelector.reset();
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					miscSelector.reset();
				}
			}
			@Override
			public void mouseEntered( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( true );
				mapViewport.setStatusString( miscSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
			}
			@Override
			public void mouseExited( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( false );
				mapViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		};
		miscSelector.addMouseListener( miscListener );
		miscSelector.addMouseMotionListener( miscListener );

		// Clear the status string when the selector is reset (hidden).
		// Had there been another delector underneath, this wouldn't be
		// necessary. Without this, the viewport isn't affected immediately by
		// a right click. The mouse has to move first to trigger an exit.
		//
		// TODO: If another selector is ever added, remove this code.
		miscSelector.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentHidden( ComponentEvent e ) {
				mapViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		});

		Insets ctrlInsets = new Insets( 3, 4, 3, 4 );

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout( selectPanel, BoxLayout.X_AXIS ) );
		selectPanel.setBorder( BorderFactory.createTitledBorder( "Select" ) );
		final JButton selectBeaconBtn = new JButton( "Beacon" );
		selectBeaconBtn.setMargin( ctrlInsets );
		selectPanel.add( selectBeaconBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectPlayerShipBtn = new JButton( "Player Ship" );
		selectPlayerShipBtn.setMargin( ctrlInsets );
		selectPanel.add( selectPlayerShipBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectStoreBtn = new JButton( "Store" );
		selectStoreBtn.setMargin( ctrlInsets );
		selectPanel.add( selectStoreBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectQuestBtn = new JButton( "Quest" );
		selectQuestBtn.setMargin( ctrlInsets );
		selectPanel.add( selectQuestBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout( addPanel, BoxLayout.X_AXIS ) );
		addPanel.setBorder( BorderFactory.createTitledBorder( "Add" ) );
		final JButton addStoreBtn = new JButton( "Store" );
		addStoreBtn.setMargin( ctrlInsets );
		addPanel.add( addStoreBtn );
		addPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton addQuestBtn = new JButton( "Quest" );
		addQuestBtn.setMargin( ctrlInsets );
		addPanel.add( addQuestBtn );

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout( otherPanel, BoxLayout.X_AXIS ) );
		otherPanel.setBorder( BorderFactory.createTitledBorder( "Other" ) );
		final JButton otherLayoutBtn = new JButton( "Layout" );
		otherLayoutBtn.setMargin( ctrlInsets );
		otherPanel.add( otherLayoutBtn );
		otherPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton otherGeneralBtn = new JButton( "General" );
		otherGeneralBtn.setMargin( ctrlInsets );
		otherPanel.add( otherGeneralBtn );
		otherPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton otherFlagshipBtn = new JButton( "Flagship" );
		otherFlagshipBtn.setMargin( ctrlInsets );
		otherPanel.add( otherFlagshipBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout( ctrlRowOnePanel, BoxLayout.X_AXIS ) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut( 15 ) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlRowTwoPanel = new JPanel();
		ctrlRowTwoPanel.setLayout( new BoxLayout( ctrlRowTwoPanel, BoxLayout.X_AXIS ) );
		ctrlRowTwoPanel.add( otherPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout( ctrlPanel, BoxLayout.Y_AXIS ) );
		ctrlPanel.add( ctrlRowOnePanel );
		ctrlPanel.add( Box.createVerticalStrut( 8 ) );
		ctrlPanel.add( ctrlRowTwoPanel );

		ActionListener ctrlListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				//TODO: When there's a central object to edit, test for null here and return.

				Object source = e.getSource();
				if ( source == selectBeaconBtn ) {
					selectBeacon();
				}
				else if ( source == selectPlayerShipBtn ) {
					selectPlayerShip();
				}
				else if ( source == selectStoreBtn ) {
					selectStore();
				}
				else if ( source == selectQuestBtn ) {
					selectQuest();
				}
				else if ( source == addStoreBtn ) {
					addStore();
				}
				else if ( source == addQuestBtn ) {
					addQuest();
				}
				else if ( source == otherLayoutBtn ) {
					showLayoutEditor();
				}
				else if ( source == otherGeneralBtn ) {
					showGeneralEditor();
				}
				else if ( source == otherFlagshipBtn ) {
					showFlagshipEditor();
				}
			}
		};

		selectBeaconBtn.addActionListener( ctrlListener );
		selectPlayerShipBtn.addActionListener( ctrlListener );
		selectStoreBtn.addActionListener( ctrlListener );
		selectQuestBtn.addActionListener( ctrlListener );

		addStoreBtn.addActionListener( ctrlListener );
		addQuestBtn.addActionListener( ctrlListener );

		otherLayoutBtn.addActionListener( ctrlListener );
		otherGeneralBtn.addActionListener( ctrlListener );
		otherFlagshipBtn.addActionListener( ctrlListener );

		otherLayoutBtn.addMouseListener( new StatusbarMouseListener( frame, "Edit the sector layout." ) );
		otherGeneralBtn.addMouseListener( new StatusbarMouseListener( frame, "Edit the rebel fleet, etc." ) );
		otherFlagshipBtn.addMouseListener( new StatusbarMouseListener( frame, "Edit the rebel flagship." ) );

		JPanel centerPanel = new JPanel( new GridBagLayout() );

		GridBagConstraints gridC = new GridBagConstraints();

		mapViewport = new StatusViewport();

		gridC.fill = GridBagConstraints.BOTH;
		gridC.weightx = 1.0;
		gridC.weighty = 1.0;
		gridC.gridx = 0;
		gridC.gridy = 0;
		JScrollPane mapScroll = new JScrollPane();
		mapScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		mapScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		mapScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		mapScroll.setViewport( mapViewport );
		mapScroll.setViewportView( mapHolderPanel );
		centerPanel.add( mapScroll, gridC );

		gridC.insets = new Insets( 4, 4, 4, 4 );

		gridC.anchor = GridBagConstraints.CENTER;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 1.0;
		gridC.weighty = 0.0;
		gridC.gridx = 0;
		gridC.gridy++;
		centerPanel.add( ctrlPanel, gridC );

		this.add( centerPanel, BorderLayout.CENTER );

		sideScroll = new JScrollPane( sidePanel );
		sideScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		sideScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sideScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		sideScroll.setVisible( false );
		this.add( sideScroll, BorderLayout.EAST );

		// As scrollpane resizes, adjust the view's size to fill the viewport.
		// No need for AncestorListener to track tab switching. Event fires even if hidden.
		mapScroll.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent e ) {
				fitViewToViewport();
			}
		});

		fitViewToViewport();
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		mapPanel.removeAll();
		mapLayout.setBeaconLocations( null );
		mapLayout.setBeaconRegionSize( null );
		miscSelector.setVisible( false );
		miscSelector.setMousePoint( -1, -1 );
		mapViewport.setStatusString( null );
		clearSidePanel();

		// These were already removed from mapPanel.
		beaconSprites.clear();
		storeSprites.clear();
		questSprites.clear();
		playerShipSprites.clear();

		beaconRefs.clear();

		forcedRNG = null;

		if ( gameState == null ) {
			mapPanel.revalidate();
			fitViewToViewport();
			mapViewport.repaint();
			return;
		}

		fileFormat = gameState.getFileFormat();

		if ( fileFormat == 2 ) {
			ftlConstants = new OriginalFTLConstants();
		} else {
			ftlConstants = new AdvancedFTLConstants();
		}

		if ( fileFormat == 11 && !gameState.isRandomNative() ) {
			forcedRNG = new FTL_1_6_Random( "FTL 1.6+" );
		}

		sectorLayoutSeed = gameState.getSectorLayoutSeed();

		GeneratedSectorMap newGenMap = null;
		if ( forcedRNG != null ) {
			// If the RNG is known, try to use it immediately, falling back to
			// a grid, if necessary.

			synchronized ( forcedRNG ) {
				forcedRNG.srand( sectorLayoutSeed );

				RandomSectorMapGenerator randomMapGen = new RandomSectorMapGenerator();
				try {
					newGenMap = randomMapGen.generateSectorMap( forcedRNG, fileFormat );
				}
				catch ( IllegalStateException e ) {
					log.error( "Map generation failed", e );
				}
			}
		}
		if ( newGenMap == null ) {
			GridSectorMapGenerator gridMapGen = new GridSectorMapGenerator();
			newGenMap = gridMapGen.generateSectorMap( GRID_GEN_COLS, GRID_GEN_ROWS, GRID_GEN_COL_W, GRID_GEN_ROW_H );
		}

		List<GeneratedBeacon> genBeacons = newGenMap.getGeneratedBeaconList();
		List<Point> newLocations = new ArrayList<Point>( genBeacons.size() );

		for ( GeneratedBeacon genBeacon : genBeacons ) {
			newLocations.add( genBeacon.getLocation() );
		}
		mapLayout.setBeaconLocations( newLocations );
		mapLayout.setBeaconRegionSize( newGenMap.getPreferredSize() );

		// Beacons.
		for ( BeaconState beaconState : gameState.getBeaconList() ) {
			SpriteReference<BeaconState> beaconRef = new SpriteReference<BeaconState>( new BeaconState( beaconState ) );
			beaconRefs.add( beaconRef );

			BeaconSprite beaconSprite = new BeaconSprite( beaconRef );
			SectorMapConstraints beaconC = new SectorMapConstraints( SectorMapConstraints.BEACON );
			beaconSprites.add( beaconSprite );
			mapPanel.add( beaconSprite, beaconC );
		}

		// Stores.
		for ( int i=0; i < beaconRefs.size(); i++ ) {
			SpriteReference<BeaconState> beaconRef = beaconRefs.get( i );

			if ( beaconRef.get().getStore() != null ) {
				StoreSprite storeSprite = new StoreSprite( beaconRef );
				SectorMapConstraints storeC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
				storeC.setBeaconId( i );
				storeSprites.add( storeSprite );
				mapPanel.add( storeSprite, storeC );
			}
		}

		// Quests.
		for ( Map.Entry<String, Integer> entry : gameState.getQuestEventMap().entrySet() ) {
			String questEventId = entry.getKey();
			int questBeaconId = entry.getValue().intValue();

			QuestSprite questSprite = new QuestSprite( questEventId );
			SectorMapConstraints questC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
			questC.setBeaconId( questBeaconId );
			questSprites.add( questSprite );
			mapPanel.add( questSprite, questC );
		}

		// Player ship.
		PlayerShipSprite playerShipSprite = new PlayerShipSprite();
		SectorMapConstraints playerShipC = new SectorMapConstraints( SectorMapConstraints.PLAYER_SHIP );
		playerShipC.setBeaconId( gameState.getCurrentBeaconId() );
		playerShipSprites.add( playerShipSprite );
		mapPanel.add( playerShipSprite, playerShipC );

		// General.
 		rebelFleetOffset = gameState.getRebelFleetOffset();
		rebelFleetFudge = gameState.getRebelFleetFudge();
		rebelPursuitMod = gameState.getRebelPursuitMod();
		hiddenCrystalWorlds = gameState.isSectorHiddenCrystalWorlds();
		hazardsVisible = gameState.areSectorHazardsVisible();

		// Flagship.
		flagshipVisible = gameState.isRebelFlagshipVisible();
		flagshipHop = gameState.getRebelFlagshipHop();
		flagshipMoving = gameState.isRebelFlagshipMoving();
		flagshipRetreating = gameState.isRebelFlagshipRetreating();
		flagshipBaseTurns = gameState.getRebelFlagshipBaseTurns();
		flagshipNearby = gameState.isRebelFlagshipNearby();
		flagship = gameState.getRebelFlagshipState();

		mapPanel.revalidate();
		fitViewToViewport();
		mapViewport.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		if ( gameState == null ) return;

		List<BeaconState> beaconStateList = gameState.getBeaconList();
		beaconStateList.clear();

		gameState.setSectorLayoutSeed( sectorLayoutSeed );

		// Player ship.
		for ( PlayerShipSprite playerShipSprite : playerShipSprites ) {
			int beaconId = mapLayout.getBeaconId( playerShipSprite );
			if ( beaconId != -1 ) {
				gameState.setCurrentBeaconId( beaconId );
			}
		}

		// Beacons.
		for ( SpriteReference<BeaconState> beaconRef : beaconRefs ) {
			beaconStateList.add( new BeaconState( beaconRef.get() ) );
		}

		// Stores are included in the beacons.

		// Quests.
		gameState.getQuestEventMap().clear();
		for ( QuestSprite questSprite : questSprites ) {
			String questId = questSprite.getQuestId();
			int beaconId = mapLayout.getBeaconId( questSprite );
			if ( beaconId != -1 && questId != null && questId.length() > 0 ) {
				gameState.getQuestEventMap().put( questSprite.getQuestId(), beaconId );
			}
		}

		// General.
 		gameState.setRebelFleetOffset( rebelFleetOffset );
		gameState.setRebelFleetFudge( rebelFleetFudge );
		gameState.setRebelPursuitMod( rebelPursuitMod );
		gameState.setSectorIsHiddenCrystalWorlds( hiddenCrystalWorlds );
		gameState.setSectorHazardsVisible( hazardsVisible );

		// Flagship.
		gameState.setRebelFlagshipVisible( flagshipVisible );
		gameState.setRebelFlagshipHop( flagshipHop );
		gameState.setRebelFlagshipMoving( flagshipMoving );
		gameState.setRebelFlagshipRetreating( flagshipRetreating );
		gameState.setRebelFlagshipBaseTurns( flagshipBaseTurns );
		gameState.setRebelFlagshipNearby( flagshipNearby );
		gameState.setRebelFlagshipState( flagship );
	}

	/**
	 * Clears the map, creates new BeaconStates, and moves the player ship to
	 * beacon 0.
	 */
	public void replaceMap( GeneratedSectorMap newGenMap, boolean clearSidePanel ) {
		mapPanel.removeAll();
		mapLayout.setBeaconLocations( null );
		mapLayout.setBeaconRegionSize( null );
		miscSelector.setVisible( false );
		miscSelector.setMousePoint( -1, -1 );
		mapViewport.setStatusString( null );
		if ( clearSidePanel ) clearSidePanel();

		beaconSprites.clear();
		storeSprites.clear();
		questSprites.clear();
		playerShipSprites.clear();

		beaconRefs.clear();

		// Beacons.
		List<GeneratedBeacon> genBeacons = newGenMap.getGeneratedBeaconList();
		List<Point> newLocations = new ArrayList<Point>( genBeacons.size() );

		for ( GeneratedBeacon genBeacon : genBeacons ) {
			newLocations.add( genBeacon.getLocation() );
		}
		mapLayout.setBeaconLocations( newLocations );
		mapLayout.setBeaconRegionSize( newGenMap.getPreferredSize() );

		for ( GeneratedBeacon genBeacon : genBeacons ) {
			BeaconState beaconState = new BeaconState();

			SpriteReference<BeaconState> beaconRef = new SpriteReference<BeaconState>( beaconState );
			beaconRefs.add( beaconRef );

			BeaconSprite beaconSprite = new BeaconSprite( beaconRef );
			SectorMapConstraints beaconC = new SectorMapConstraints( SectorMapConstraints.BEACON );
			beaconSprites.add( beaconSprite );
			mapPanel.add( beaconSprite, beaconC );
		}

		// Player ship.
		PlayerShipSprite playerShipSprite = new PlayerShipSprite();
		SectorMapConstraints playerShipC = new SectorMapConstraints( SectorMapConstraints.PLAYER_SHIP );
		playerShipC.setBeaconId( 0 );  // TODO: Magic number.
		playerShipSprites.add( playerShipSprite );
		mapPanel.add( playerShipSprite, playerShipC );

		mapPanel.revalidate();
		fitViewToViewport();
		mapViewport.repaint();
	}

	/**
	 * Ensures the view (and selectors) are at least as large as the viewport.
	 *
	 * Without this method, the view may be undersized for the viewport,
	 * leaving dead space to the right and below the view.
	 *
	 * The view will be made large enough to enclose all non-selector child
	 * components' bounds. Selectors will be given the same size.
	 *
	 * Note: Layout components, stretch the holder panel to fit, and paint:
	 *   mapPanel.revalidate();
	 *   fitViewToViewport();
	 *   Viewport.repaint();
	 */
	private void fitViewToViewport() {
		// Calculate needed dimensions for all non-selector components.

		mapPanel.setSize( mapPanel.getPreferredSize() );

		int neededWidth = 0, neededHaight = 0;
		for ( Component c : mapHolderPanel.getComponents() ) {
			if ( c == miscSelector ) continue;

			neededWidth = Math.max( c.getX()+c.getWidth(), neededWidth );
			neededHaight = Math.max( c.getY()+c.getHeight(), neededHaight );
		}

		Dimension viewExtents = mapViewport.getExtentSize();
		// Possibly account for scrollbar thickness?

		int desiredWidth = Math.max( viewExtents.width, neededWidth );
		int desiredHeight = Math.max( viewExtents.height, neededHaight );
		mapHolderPanel.setPreferredSize( new Dimension( desiredWidth, desiredHeight ) );

		miscSelector.setSize( desiredWidth, desiredHeight );
	}

	public void selectBeacon() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Beacon";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					SpriteReference<BeaconState> beaconRef = ((BeaconSprite)sprite).getReference();
					showBeaconEditor( beaconRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectPlayerShip() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( playerShipSprites );
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Player Ship";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof PlayerShipSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof PlayerShipSprite ) {
					showPlayerShipEditor( (PlayerShipSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectStore() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( storeSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Store";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof StoreSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof StoreSprite ) {
					SpriteReference<BeaconState> beaconRef = ((StoreSprite)sprite).getReference();
					showStoreEditor( beaconRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	public void selectQuest() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( questSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Quest";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof QuestSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof QuestSprite ) {
					showQuestEditor( (QuestSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void addStore() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Store";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId( beaconId ) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId( beaconId ) == null ) {
						SpriteReference<BeaconState> beaconRef = ((BeaconSprite)sprite).getReference();

						beaconRef.get().setStore( new StoreState() );
						beaconRef.get().getStore().addShelf( new StoreShelf() );

						StoreSprite storeSprite = new StoreSprite( beaconRef );
						SectorMapConstraints storeC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
						storeC.setBeaconId( beaconId );
						storeSprites.add( storeSprite );
						mapPanel.add( storeSprite, storeC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void addQuest() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Quest";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId( beaconId ) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						QuestSprite questSprite = new QuestSprite( "NOTHING" );
						SectorMapConstraints questC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
						questC.setBeaconId( beaconId );
						questSprites.add( questSprite );
						mapPanel.add( questSprite, questC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void movePlayerShip( final PlayerShipSprite mobileSprite ) {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( beaconSprites );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Move: Player Ship";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 ) {
						mapPanel.remove( mobileSprite );
						SectorMapConstraints mobileC = new SectorMapConstraints( SectorMapConstraints.PLAYER_SHIP );
						mobileC.setBeaconId( beaconId );
						mapPanel.add( mobileSprite, mobileC );

						mapPanel.revalidate();
						mapViewport.repaint();
					}
				}

				return false;
			}
		});
		miscSelector.setVisible( true );
	}

	private void showSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension( sideWidth + vbarWidth, 1 ) );
		sideScroll.setVisible( true );

		this.revalidate();
		this.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				sideScroll.getVerticalScrollBar().setValue( 0 );
			}
		});
	}

	private void fitSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension( sideWidth + vbarWidth, 1 ) );

		this.revalidate();
		this.repaint();
	}

	private void clearSidePanel() {
		sideScroll.setVisible( false );
		sidePanel.removeAll();

		sidePanel.revalidate();
		this.revalidate();
		this.repaint();
	}

	/**
	 * Clears and ropulates the side panel.
	 * Includes a title, arbitrary content, and
	 * cancel/apply buttons.
	 *
	 * More can be added to the side panel manually.
	 *
	 * Afterward, showSidePanel() should be called.
	 *
	 * @param title
	 * @param editorPanel
	 * @param extraContent optional component(s) to add before cancel/apply buttons
	 * @param applyCallback method to call when the apply button is clicked
	 */
	private void createSidePanel( String title, final FieldEditorPanel editorPanel, JComponent extraContent, final Runnable applyCallback ) {
		clearSidePanel();
		JLabel titleLbl = new JLabel( title );
		titleLbl.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( titleLbl );
		addSidePanelSeparator( 4 );

		// Keep the editor from growing and creating gaps around it.
		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		sidePanel.add( editorPanel );

		if ( extraContent != null ) {
			sidePanel.add( Box.createVerticalStrut( 10 ) );
			sidePanel.add( extraContent );
		}

		sidePanel.add( Box.createVerticalStrut( 10 ) );

		JPanel applyPanel = new JPanel();
		applyPanel.setLayout( new BoxLayout( applyPanel, BoxLayout.X_AXIS ) );
		JButton closeBtn = new JButton( "Close" );
		applyPanel.add( closeBtn );
		applyPanel.add( Box.createRigidArea( new Dimension( 15, 1 ) ) );
		JButton applyBtn = new JButton( "Apply" );
		applyPanel.add( applyBtn );
		sidePanel.add( applyPanel );

		closeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
			}
		});

		applyBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				applyCallback.run();
			}
		});
	}

	/**
	 * Adds a separator to the side panel.
	 */
	private void addSidePanelSeparator( int spacerSize ) {
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
		JSeparator newSep = new JSeparator( JSeparator.HORIZONTAL );
		newSep.setMaximumSize( new Dimension( Short.MAX_VALUE, newSep.getPreferredSize().height ) );
		sidePanel.add( newSep );
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
	}

	/**
	 * Adds a wrapped label the side panel.
	 */
	private void addSidePanelNote( String s ) {
		JTextArea labelArea = new JTextArea( s );
		labelArea.setBackground( null );
		labelArea.setEditable( false );
		labelArea.setBorder( null );
		labelArea.setLineWrap( true );
		labelArea.setWrapStyleWord( true );
		labelArea.setFocusable( false );
		sidePanel.add( labelArea );
	}

	/**
	 * Sets a JCheckBox selection and triggers all ItemListeners.
	 */
	private void forceCheckBox( JCheckBox box, boolean selected ) {
		if ( box.isSelected() != selected ) {
			box.setSelected( selected );

			// No need to manually trigger listeners, since it really changed.
		}
		else {
			box.setSelected( selected );

			for ( ItemListener l : box.getListeners( ItemListener.class ) ) {
				ItemEvent evt = new ItemEvent( box, ItemEvent.ITEM_STATE_CHANGED, box, (selected ? ItemEvent.SELECTED : ItemEvent.DESELECTED) );
				l.itemStateChanged( evt );
			}
		}
	}

	private void showLayoutEditor() {
		final String ALGORITHM = "RNG";
		final String LAYOUT = "Preview";
		final String LAYOUT_SEED = "Seed";

		final String LAYOUT_GRID = "Grid";
		final String LAYOUT_SEEDED = "Seeded";

		String title = String.format( "Sector Layout" );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( true );
		editorPanel.addRow( ALGORITHM, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addBlankRow();
		editorPanel.addRow( LAYOUT, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addBlankRow();
		editorPanel.addRow( LAYOUT_SEED, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( LAYOUT_SEED ).setDocument( new RegexDocument( "-?[0-9]*" ) );

		editorPanel.getCombo( ALGORITHM ).addMouseListener( new StatusbarMouseListener( frame, "Algorithm of the OS the saved game was created under, or native for the current OS." ) );
		editorPanel.getCombo( LAYOUT ).addMouseListener( new StatusbarMouseListener( frame, "The type of map to generate." ) );
		editorPanel.getInt( LAYOUT_SEED ).addMouseListener( new StatusbarMouseListener( frame, "A per-sector constant that seeds random generation of the map, events, etc. (potentially dangerous)." ) );

		if ( forcedRNG != null ) {
			// Since FTL 1.6.1, non-migrated game states have a known RNG.
			editorPanel.getCombo( ALGORITHM ).addItem( forcedRNG );
		}
		else {
			if ( fileFormat == 11 ) {  // FTL 1.6.1.
				editorPanel.getCombo( ALGORITHM ).addItem( new FTL_1_6_Random( "FTL 1.6+" ) );
			}
			editorPanel.getCombo( ALGORITHM ).addItem( new NativeRandom( "Native" ) );
			editorPanel.getCombo( ALGORITHM ).addItem( new GNULibCRandom( "GLibC (Linux/OSX)" ) );
			editorPanel.getCombo( ALGORITHM ).addItem( new MsRandom( "Microsoft" ) );
		}

		editorPanel.getCombo( LAYOUT ).addItem( LAYOUT_GRID );
		editorPanel.getCombo( LAYOUT ).addItem( LAYOUT_SEEDED );
		editorPanel.getCombo( LAYOUT ).setSelectedItem( LAYOUT_SEEDED );

		editorPanel.setIntAndReminder( LAYOUT_SEED, sectorLayoutSeed );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				int newSeed = editorPanel.parseInt( LAYOUT_SEED );

				GeneratedSectorMap newGenMap = null;

				if ( LAYOUT_GRID.equals( editorPanel.getCombo( LAYOUT ).getSelectedItem() ) ) {

					GridSectorMapGenerator gridMapGen = new GridSectorMapGenerator();
					newGenMap = gridMapGen.generateSectorMap( GRID_GEN_COLS, GRID_GEN_ROWS, GRID_GEN_COL_W, GRID_GEN_ROW_H );

				}
				else if ( LAYOUT_SEEDED.equals( editorPanel.getCombo( LAYOUT ).getSelectedItem() ) ) {

					Object selectedRNGObj = editorPanel.getCombo( ALGORITHM ).getSelectedItem();
					if ( selectedRNGObj == null ) {
						log.warn( "No RNG selected to generate a sector map!?" );
						return;
					}

					@SuppressWarnings( "unchecked" )
					RandRNG selectedRNG = (RandRNG)selectedRNGObj;

					synchronized ( selectedRNG ) {
						selectedRNG.srand( newSeed );

						RandomSectorMapGenerator randomMapGen = new RandomSectorMapGenerator();
						try {
							newGenMap = randomMapGen.generateSectorMap( selectedRNG, fileFormat );
						}
						catch ( IllegalStateException e ) {
							log.error( "Map generation failed", e );
							JOptionPane.showMessageDialog( frame, "Map generation failed:\n"+ e.toString(), "Map generation failed", JOptionPane.ERROR_MESSAGE );
						}
					}
				}

				if ( newGenMap != null ) {
					sectorLayoutSeed = newSeed;

					List<GeneratedBeacon> genBeacons = newGenMap.getGeneratedBeaconList();
					List<Point> newLocations = new ArrayList<Point>( genBeacons.size() );

					for ( GeneratedBeacon genBeacon : genBeacons ) {
						newLocations.add( genBeacon.getLocation() );
					}
					mapLayout.setBeaconLocations( newLocations );
					mapLayout.setBeaconRegionSize( newGenMap.getPreferredSize() );

					mapPanel.revalidate();
					fitViewToViewport();
					mapViewport.repaint();
				}

				editorPanel.getInt( LAYOUT_SEED ).setText( ""+ sectorLayoutSeed );
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		addSidePanelSeparator( 6 );

		JButton randomSeedBtn = new JButton( "Random Seed" );
		randomSeedBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		randomSeedBtn.addMouseListener( new StatusbarMouseListener( frame, "Generate an entirely new sector map." ) );
		sidePanel.add( randomSeedBtn );

		randomSeedBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				int newSeed = javaRandom.nextInt( Integer.MAX_VALUE );

				editorPanel.getInt( LAYOUT_SEED ).setText( ""+ newSeed );

				GeneratedSectorMap newGenMap = null;

				if ( LAYOUT_GRID.equals( editorPanel.getCombo( LAYOUT ).getSelectedItem() ) ) {

					GridSectorMapGenerator gridMapGen = new GridSectorMapGenerator();
					newGenMap = gridMapGen.generateSectorMap( GRID_GEN_COLS, GRID_GEN_ROWS, GRID_GEN_COL_W, GRID_GEN_ROW_H );

				}
				else if ( LAYOUT_SEEDED.equals( editorPanel.getCombo( LAYOUT ).getSelectedItem() ) ) {

					Object selectedRNGObj = editorPanel.getCombo( ALGORITHM ).getSelectedItem();
					if ( selectedRNGObj == null ) {
						log.warn( "No RNG selected to generate a sector map!?" );
						return;
					}

					@SuppressWarnings( "unchecked" )
					RandRNG selectedRNG = (RandRNG)selectedRNGObj;

					synchronized ( selectedRNG ) {
						selectedRNG.srand( newSeed );

						RandomSectorMapGenerator randomMapGen = new RandomSectorMapGenerator();
						try {
							newGenMap = randomMapGen.generateSectorMap( selectedRNG, fileFormat );
						}
						catch ( IllegalStateException f ) {
							log.error( "Map generation failed", f );
							JOptionPane.showMessageDialog( frame, "Map generation failed:\n"+ f.toString(), "Map generation failed", JOptionPane.ERROR_MESSAGE );
						}
					}
				}

				if ( newGenMap != null ) {
					sectorLayoutSeed = newSeed;

					replaceMap( newGenMap, false );
				}

				editorPanel.getInt( LAYOUT_SEED ).setText( ""+ sectorLayoutSeed );
			}
		});

		addSidePanelSeparator( 8 );
		String notice = ""
			+ "A sector map is part random, part fixed & editable.\n"
			+ "\n"
			+ "FTL first builds a map with random layout and events. "
			+ "Then FTL overlays it with a flat list of saved beacon "
			+ "details, each overriding the nth random beacon.\n"
			+ "\n"
			+ "FTL sends the sector seed to the OS's random number generator. "
			+ "As such, the map is fragile: platform-dependent.\n"
			+ "\n"
			+ "This editor can reconstruct an RNG-informed preview using "
			+ "various algorithms, or passively display the fixed beacon list "
			+ "wrapped vertically in a grid.\n"
			+ "\n"
			+ "If FTL interprets the seed differently in-game, the beacon count "
			+ "will vary, along with all other random elements.\n"
			+ "\n"
			+ "FTL 1.6.1+ uses a built-in RNG regardless of OS (unless the campaign was "
			+ "migrated from an earlier edition).\n"
			+ "\n"
			+ "A grid layout with the original seed should always be safe.";

		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showGeneralEditor() {
		final String REBEL_FLEET_OFFSET = "Rebel Fleet Offset";
		final String REBEL_FLEET_FUDGE = "Rebel Fleet Fudge";
		final String REBEL_PURSUIT_MOD = "Rebel Pursuit Mod";
		final String HIDDEN_SECTOR = "In Hidden Sector";
		final String HAZARDS_VISIBLE = "Hazards Visible";

		String title = String.format( "General" );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( true );
		editorPanel.addRow( REBEL_FLEET_OFFSET, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( REBEL_FLEET_OFFSET ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( REBEL_FLEET_FUDGE, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( REBEL_FLEET_FUDGE ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( REBEL_PURSUIT_MOD, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( REBEL_PURSUIT_MOD ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( HIDDEN_SECTOR, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( HAZARDS_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );

		editorPanel.getInt( REBEL_FLEET_OFFSET ).addMouseListener( new StatusbarMouseListener( frame, "A large negative var (-750,-250,...,-n*25, approaching 0) + fudge = the fleet circle's leading edge." ) );
		editorPanel.getInt( REBEL_FLEET_FUDGE ).addMouseListener( new StatusbarMouseListener( frame, "A random per-sector constant (usually around 75-310) + offset = the fleet circle's edge." ) );
		editorPanel.getInt( REBEL_PURSUIT_MOD ).addMouseListener( new StatusbarMouseListener( frame, "Delay/alert the fleet, changing the warning zone thickness (e.g., merc distraction = -2)." ) );
		editorPanel.getBoolean( HIDDEN_SECTOR ).addMouseListener( new StatusbarMouseListener( frame, "Sector #?: Hidden Crystal Worlds. At the exit, you won't get to choose the next sector." ) );
		editorPanel.getBoolean( HAZARDS_VISIBLE ).addMouseListener( new StatusbarMouseListener( frame, "Show hazards on the current sector map." ) );

		editorPanel.setIntAndReminder( REBEL_FLEET_OFFSET, rebelFleetOffset );
		editorPanel.setIntAndReminder( REBEL_FLEET_FUDGE, rebelFleetFudge );
		editorPanel.setIntAndReminder( REBEL_PURSUIT_MOD, rebelPursuitMod );
		editorPanel.setBoolAndReminder( HIDDEN_SECTOR, hiddenCrystalWorlds );
		editorPanel.setBoolAndReminder( HAZARDS_VISIBLE, hazardsVisible );


		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				try { rebelFleetOffset = editorPanel.parseInt( REBEL_FLEET_OFFSET ); }
				catch ( NumberFormatException e ) {}

				try { rebelFleetFudge = editorPanel.parseInt( REBEL_FLEET_FUDGE ); }
				catch ( NumberFormatException e ) {}

				try { rebelPursuitMod = editorPanel.parseInt( REBEL_PURSUIT_MOD ); }
				catch ( NumberFormatException e ) {}

				hiddenCrystalWorlds = editorPanel.getBoolean( HIDDEN_SECTOR ).isSelected();
				hazardsVisible = editorPanel.getBoolean( HAZARDS_VISIBLE ).isSelected();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showFlagshipEditor() {
		final String FLAGSHIP_VISIBLE = "Flagship Visible";
		final String FLAGSHIP_HOP = "Flagship Hop";
		final String FLAGSHIP_MOVING = "Flagship Moving";
		final String FLAGSHIP_RETREATING = "Flagship Retreating";
		final String FLAGSHIP_BASE_TURNS = "Flagship Base Turns";
		final String FLAGSHIP_NEARBY = "Flagship Nearby";
		final String FLAGSHIP_ALPHA = "Alpha?";
		final String FLAGSHIP_GAMMA = "Gamma?";
		final String FLAGSHIP_DELTA = "Delta?";

		String title = String.format( "Flagship" );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( true );
		editorPanel.addRow( FLAGSHIP_VISIBLE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( FLAGSHIP_HOP, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( FLAGSHIP_HOP ).setMaximum( 10 );
		editorPanel.addRow( FLAGSHIP_MOVING, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( FLAGSHIP_RETREATING, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( FLAGSHIP_BASE_TURNS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( FLAGSHIP_NEARBY, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( FLAGSHIP_ALPHA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( FLAGSHIP_ALPHA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( FLAGSHIP_GAMMA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( FLAGSHIP_GAMMA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( FLAGSHIP_DELTA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( FLAGSHIP_DELTA ).setDocument( new RegexDocument( "-?[0-9]*" ) );

		editorPanel.getBoolean( FLAGSHIP_VISIBLE ).addMouseListener( new StatusbarMouseListener( frame, "Toggle the rebel flagship on the map. (FTL 1.01-1.03.3: Instant loss if not in sector 8)" ) );
		editorPanel.getSlider( FLAGSHIP_HOP ).addMouseListener( new StatusbarMouseListener( frame, "The flagship is at it's Nth random beacon. (0-based) Sector layout seed affects where that will be. (FTL 1.01-1.03.3: Instant loss may occur beyond 4)" ) );
		editorPanel.getBoolean( FLAGSHIP_MOVING ).addMouseListener( new StatusbarMouseListener( frame, "The flagship is moving from its current beacon toward the next." ) );
		editorPanel.getBoolean( FLAGSHIP_RETREATING ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Increments after defeating the flagship. Decrements on re-saving. It glitches the hop index!?" ) );
		editorPanel.getInt( FLAGSHIP_BASE_TURNS ).addMouseListener( new StatusbarMouseListener( frame, "Number of turns the flagship has started at the fed base. Instant loss will occur beyond 3." ) );
		editorPanel.getBoolean( FLAGSHIP_NEARBY ).addMouseListener( new StatusbarMouseListener( frame, "True if nearby ship is the flagship. Only set when a nearby ship is present." ) );
		editorPanel.getInt( FLAGSHIP_ALPHA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Last seen rebel flagship stage, or 0. Redundant?" ) );
		editorPanel.getInt( FLAGSHIP_GAMMA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Varies during first and second rebel flagship stages, or 0." ) );
		editorPanel.getInt( FLAGSHIP_DELTA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Surge ticks during all rebel flagship stages, or 0. Goal varies. No visible effect during first stage." ) );

		editorPanel.setBoolAndReminder( FLAGSHIP_VISIBLE, flagshipVisible );
		editorPanel.setSliderAndReminder( FLAGSHIP_HOP, flagshipHop );
		editorPanel.setBoolAndReminder( FLAGSHIP_MOVING, flagshipMoving );
		editorPanel.setBoolAndReminder( FLAGSHIP_RETREATING, flagshipRetreating );
		editorPanel.setIntAndReminder( FLAGSHIP_BASE_TURNS, flagshipBaseTurns );
		editorPanel.setBoolAndReminder( FLAGSHIP_NEARBY, flagshipNearby );

		boolean flagshipEnabled = ( flagship != null );
		editorPanel.getInt( FLAGSHIP_ALPHA ).setEnabled( flagshipEnabled );
		editorPanel.getInt( FLAGSHIP_GAMMA ).setEnabled( flagshipEnabled );
		editorPanel.getInt( FLAGSHIP_DELTA ).setEnabled( flagshipEnabled );

		if ( flagshipEnabled ) {
			editorPanel.setIntAndReminder( FLAGSHIP_ALPHA, flagship.getUnknownAlpha() );
			editorPanel.setIntAndReminder( FLAGSHIP_GAMMA, flagship.getUnknownGamma() );
			editorPanel.setIntAndReminder( FLAGSHIP_DELTA, flagship.getUnknownDelta() );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				flagshipVisible = editorPanel.getBoolean( FLAGSHIP_VISIBLE ).isSelected();

				if ( flagshipVisible ) {
					flagshipHop = editorPanel.getSlider( FLAGSHIP_HOP ).getValue();
					flagshipMoving = editorPanel.getBoolean( FLAGSHIP_MOVING ).isSelected();
					flagshipRetreating = editorPanel.getBoolean( FLAGSHIP_RETREATING ).isSelected();

					try { flagshipBaseTurns = editorPanel.parseInt( FLAGSHIP_BASE_TURNS ); }
					catch ( NumberFormatException e ) {}
				}
				else {
					flagshipHop = 0;
					flagshipMoving = false;
					flagshipRetreating = false;
					flagshipBaseTurns = 0;
				}
				flagshipNearby = editorPanel.getBoolean( FLAGSHIP_NEARBY ).isSelected();

				if ( flagship != null ) {
					try { flagship.setUnknownAlpha( editorPanel.parseInt( FLAGSHIP_ALPHA ) ); }
					catch ( NumberFormatException e ) {}

					try { flagship.setUnknownGamma( editorPanel.parseInt( FLAGSHIP_GAMMA ) ); }
					catch ( NumberFormatException e ) {}

					try { flagship.setUnknownDelta( editorPanel.parseInt( FLAGSHIP_DELTA ) ); }
					catch ( NumberFormatException e ) {}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		ItemListener visibleListener = new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean flagshipVisible = ( e.getStateChange() == ItemEvent.SELECTED );

				if ( !flagshipVisible ) {
					editorPanel.getSlider( FLAGSHIP_HOP ).setValue( 0 );
					editorPanel.getBoolean( FLAGSHIP_MOVING ).setSelected( false );
					editorPanel.getBoolean( FLAGSHIP_RETREATING ).setSelected( false );
					editorPanel.getInt( FLAGSHIP_BASE_TURNS ).setText( "0" );
				}
				editorPanel.getSlider( FLAGSHIP_HOP ).setEnabled( flagshipVisible );
				editorPanel.getBoolean( FLAGSHIP_MOVING ).setEnabled( flagshipVisible );
				editorPanel.getBoolean( FLAGSHIP_RETREATING ).setEnabled( flagshipVisible );
				editorPanel.getInt( FLAGSHIP_BASE_TURNS ).setEnabled( flagshipVisible );
			}
		};
		editorPanel.getBoolean( FLAGSHIP_VISIBLE ).addItemListener( visibleListener );

		forceCheckBox( editorPanel.getBoolean( FLAGSHIP_VISIBLE ), editorPanel.getBoolean( FLAGSHIP_VISIBLE ).isSelected() );

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showBeaconEditor( final SpriteReference<BeaconState> beaconRef ) {
		final String VISIT_COUNT = "Visit Count";
		final String STARS_LIST = "Bkg List";
		final String STARS_IMAGE = "Bkg Image";
		final String SPRITE_LIST = "Sprite List";
		final String SPRITE_IMAGE = "Sprite Image";
		final String SPRITE_X = "Sprite PosX";
		final String SPRITE_Y = "Sprite PosY";
		final String SPRITE_ROT = "Sprite Rot";
		final String SEEN = "Seen";
		final String ENEMY_PRESENT = "Enemy Present";
		final String SHIP_EVENT = "Ship Event";
		final String AUTO_SHIP = "Auto Ship";
		final String SHIP_EVENT_SEED = "Ship Event Seed";
		final String FLEET = "Fleet";
		final String UNDER_ATTACK = "Under Attack";

		final Map<String, BackgroundImageList> allImageListsMap = DataManager.get().getBackgroundImageLists();
		final Map<String, ShipEvent> allShipEventsMap = DataManager.get().getShipEvents();

		int beaconId = mapLayout.getBeaconId( beaconRef.getSprite( BeaconSprite.class ) );
		String title = String.format( "Beacon %02d", beaconId );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( VISIT_COUNT, FieldEditorPanel.ContentType.SPINNER );
		editorPanel.getSpinnerField( VISIT_COUNT ).addMouseListener( new StatusbarMouseListener( frame, "Number of times the player has been here. If visited, random events won't occur. (All nearby fields need values) Hit enter after typing." ) );
		editorPanel.addRow( STARS_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( STARS_LIST ).setEnabled( false );
		editorPanel.getCombo( STARS_LIST ).addMouseListener( new StatusbarMouseListener( frame, "An image list from which to choose a background starscape. (BG_*)" ) );
		editorPanel.addRow( STARS_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( STARS_IMAGE ).setEnabled( false );
		editorPanel.getCombo( STARS_IMAGE ).addMouseListener( new StatusbarMouseListener( frame, "Background starscape, a fullscreen image." ) );
		editorPanel.addRow( SPRITE_LIST, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( SPRITE_LIST ).setEnabled( false );
		editorPanel.getCombo( SPRITE_LIST ).addMouseListener( new StatusbarMouseListener( frame, "An image list from which to choose a background sprite. (PLANET_*)" ) );
		editorPanel.addRow( SPRITE_IMAGE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( SPRITE_IMAGE ).setEnabled( false );
		editorPanel.getCombo( SPRITE_IMAGE ).addMouseListener( new StatusbarMouseListener( frame, "Background sprite, which appears in front of the starscape." ) );
		editorPanel.addRow( SPRITE_X, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( SPRITE_X ).setEnabled( false );
		editorPanel.getInt( SPRITE_X ).addMouseListener( new StatusbarMouseListener( frame, "Background sprite X position." ) );
		editorPanel.addRow( SPRITE_Y, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( SPRITE_Y ).setEnabled( false );
		editorPanel.getInt( SPRITE_Y ).addMouseListener( new StatusbarMouseListener( frame, "Background sprite Y position." ) );
		editorPanel.addRow( SPRITE_ROT, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( SPRITE_ROT ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt( SPRITE_ROT ).setText( "0" );
		editorPanel.getInt( SPRITE_ROT ).setEnabled( false );
		editorPanel.getInt( SPRITE_ROT ).addMouseListener( new StatusbarMouseListener( frame, "Background sprite rotation. (degrees, positive = clockwise)" ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( SEEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( SEEN ).addMouseListener( new StatusbarMouseListener( frame, "The player has been within one hop of this beacon." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( ENEMY_PRESENT, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( ENEMY_PRESENT ).addMouseListener( new StatusbarMouseListener( frame, "A ship is waiting at this beacon. (All nearby fields need values)" ) );
		editorPanel.addRow( SHIP_EVENT, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( SHIP_EVENT ).setEnabled( false );
		editorPanel.getCombo( SHIP_EVENT ).addMouseListener( new StatusbarMouseListener( frame, "A ship event to trigger and forget upon arrival (spawning a new nearby ship)." ) );
		editorPanel.addRow( AUTO_SHIP, FieldEditorPanel.ContentType.STRING );
		editorPanel.getString( AUTO_SHIP ).setEditable( false );
		editorPanel.getString( AUTO_SHIP ).setEnabled( false );
		editorPanel.getString( AUTO_SHIP ).addMouseListener( new StatusbarMouseListener( frame, "The blueprint (or blueprintList) of an auto ship to appear." ) );
		editorPanel.addRow( SHIP_EVENT_SEED, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( SHIP_EVENT_SEED ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt( SHIP_EVENT_SEED ).setText( "0" );
		editorPanel.getInt( SHIP_EVENT_SEED ).setEnabled( false );
		editorPanel.getInt( SHIP_EVENT_SEED ).addMouseListener( new StatusbarMouseListener( frame, "A constant that seeds the random generation of the enemy ship." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( FLEET, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( FLEET ).addMouseListener( new StatusbarMouseListener( frame, "Fleet background sprites." ) );
		editorPanel.addRow( UNDER_ATTACK, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( UNDER_ATTACK ).addMouseListener( new StatusbarMouseListener( frame, "The beacon is under attack by rebels (flashing red)." ) );

		editorPanel.getCombo( STARS_LIST ).addItem( "" );
		editorPanel.getCombo( SPRITE_LIST ).addItem( "" );
		editorPanel.getCombo( STARS_IMAGE ).addItem( "" );
		editorPanel.getCombo( SPRITE_IMAGE ).addItem( "" );
		for ( BackgroundImageList imageList : allImageListsMap.values() ) {
			editorPanel.getCombo( STARS_LIST ).addItem( imageList );
			editorPanel.getCombo( SPRITE_LIST ).addItem( imageList );
		}

		editorPanel.getCombo( SHIP_EVENT ).addItem( "" );
		for ( ShipEvent shipEvent : allShipEventsMap.values() ) {
			editorPanel.getCombo( SHIP_EVENT ).addItem( shipEvent );
		}

		if ( beaconRef.get().getVisitCount() > 0 ) {
			editorPanel.getCombo( STARS_LIST ).setEnabled( true );
			editorPanel.getCombo( STARS_IMAGE ).setEnabled( true );
			editorPanel.getCombo( SPRITE_LIST ).setEnabled( true );
			editorPanel.getCombo( SPRITE_IMAGE ).setEnabled( true );
			editorPanel.getInt( SPRITE_X ).setEnabled( true );
			editorPanel.getInt( SPRITE_Y ).setEnabled( true );
			editorPanel.getInt( SPRITE_ROT ).setEnabled( true );

			editorPanel.getSpinner( VISIT_COUNT ).setValue( beaconRef.get().getVisitCount() );

			editorPanel.getCombo( STARS_IMAGE ).addItem( beaconRef.get().getBgStarscapeImageInnerPath() );
			editorPanel.getCombo( STARS_IMAGE ).setSelectedItem( beaconRef.get().getBgStarscapeImageInnerPath() );

			editorPanel.getCombo( SPRITE_IMAGE ).addItem( beaconRef.get().getBgSpriteImageInnerPath() );
			editorPanel.getCombo( SPRITE_IMAGE ).setSelectedItem( beaconRef.get().getBgSpriteImageInnerPath() );

			editorPanel.getInt( SPRITE_X ).setText( ""+ beaconRef.get().getBgSpritePosX() );
			editorPanel.getInt( SPRITE_Y ).setText( ""+ beaconRef.get().getBgSpritePosY() );
			editorPanel.getInt( SPRITE_ROT ).setText( ""+ beaconRef.get().getBgSpriteRotation() );
		}

		editorPanel.getBoolean( SEEN ).setSelected( beaconRef.get().isSeen() );

		if ( beaconRef.get().isEnemyPresent() ) {
			editorPanel.getCombo( SHIP_EVENT ).setEnabled( true );
			editorPanel.getString( AUTO_SHIP ).setEnabled( true );
			editorPanel.getInt( SHIP_EVENT_SEED ).setEnabled( true );

			editorPanel.getBoolean( ENEMY_PRESENT ).setSelected( true );

			ShipEvent currentShipEvent = allShipEventsMap.get( beaconRef.get().getShipEventId() );
			if ( currentShipEvent != null )
				editorPanel.getCombo( SHIP_EVENT ).setSelectedItem( currentShipEvent );

			editorPanel.getString( AUTO_SHIP ).setText( beaconRef.get().getAutoBlueprintId() );

			editorPanel.getInt( SHIP_EVENT_SEED ).setText( ""+ beaconRef.get().getShipEventSeed() );
		}

		for ( FleetPresence fleetPresence : FleetPresence.values() ) {
			editorPanel.getCombo( FLEET ).addItem( fleetPresence );
		}
		editorPanel.getCombo( FLEET ).setSelectedItem( beaconRef.get().getFleetPresence() );

		editorPanel.getBoolean( UNDER_ATTACK ).setSelected( beaconRef.get().isUnderAttack() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String newString;

				int visitCount = 0;
				String bgStarscapeImageInnerPath = null;
				String bgSpriteImageInnerPath = null;
				int bgSpritePosX = 0;
				int bgSpritePosY = 0;
				int bgSpriteRotation = 0;

				Object bgStarscapeImageInnerPathObj = editorPanel.getCombo( STARS_IMAGE ).getSelectedItem();
				if ( bgStarscapeImageInnerPathObj instanceof BackgroundImage ) {
					bgStarscapeImageInnerPath = ((BackgroundImage)bgStarscapeImageInnerPathObj).getInnerPath();
				}
				else if ( bgStarscapeImageInnerPathObj instanceof String ) {
					if ( !"".equals( bgStarscapeImageInnerPath ) && !"null".equals( bgStarscapeImageInnerPath ) )
						bgStarscapeImageInnerPath = (String)bgStarscapeImageInnerPathObj;
				}

				Object bgSpriteImageInnerPathObj = editorPanel.getCombo( SPRITE_IMAGE ).getSelectedItem();
				if ( bgSpriteImageInnerPathObj instanceof BackgroundImage ) {
					bgSpriteImageInnerPath = ((BackgroundImage)bgSpriteImageInnerPathObj).getInnerPath();
				}
				else if ( bgSpriteImageInnerPathObj instanceof String ) {
					if ( !"".equals( bgSpriteImageInnerPathObj ) && !"null".equals( bgSpriteImageInnerPathObj ) )
						bgSpriteImageInnerPath = (String)bgSpriteImageInnerPathObj;
				}

				visitCount = editorPanel.parseSpinnerInt( VISIT_COUNT );

				try { bgSpritePosX = editorPanel.parseInt( SPRITE_X ); }
				catch ( NumberFormatException e ) {}

				try { bgSpritePosY = editorPanel.parseInt( SPRITE_Y ); }
				catch ( NumberFormatException e ) {}

				try { bgSpriteRotation = editorPanel.parseInt( SPRITE_ROT ); }
				catch ( NumberFormatException e ) {}

				if ( "NONE".equals( bgSpriteImageInnerPath ) ) {
					bgSpritePosX = 0;
					bgSpritePosY = 0;
					bgSpriteRotation = 0;
				}

				if ( visitCount > 0 && bgStarscapeImageInnerPath != null && bgSpriteImageInnerPath != null ) {
					beaconRef.get().setVisitCount( visitCount );
					beaconRef.get().setBgStarscapeImageInnerPath( bgStarscapeImageInnerPath );
					beaconRef.get().setBgSpriteImageInnerPath( bgSpriteImageInnerPath );
					beaconRef.get().setBgSpritePosX( bgSpritePosX );
					beaconRef.get().setBgSpritePosY( bgSpritePosY );
					beaconRef.get().setBgSpriteRotation( bgSpriteRotation );
				} else {
					beaconRef.get().setVisitCount( visitCount );
					beaconRef.get().setBgStarscapeImageInnerPath( null );
					beaconRef.get().setBgSpriteImageInnerPath( null );
					beaconRef.get().setBgSpritePosX( -1 );
					beaconRef.get().setBgSpritePosY( -1 );
					beaconRef.get().setBgSpriteRotation( 0 );
				}

				beaconRef.get().setSeen( editorPanel.getBoolean( SEEN ).isSelected() );

				String shipEventId = null;
				String autoBlueprintId = null;
				int shipEventSeed = 0;
				boolean enemyPresent = editorPanel.getBoolean( ENEMY_PRESENT ).isSelected();

				Object shipEventObj = editorPanel.getCombo( SHIP_EVENT ).getSelectedItem();
				if ( shipEventObj instanceof ShipEvent )
					shipEventId = ((ShipEvent)shipEventObj).getId();

				autoBlueprintId = editorPanel.getString(AUTO_SHIP).getText();

				newString = editorPanel.getInt( SHIP_EVENT_SEED ).getText();
				try { shipEventSeed = Integer.parseInt( newString ); }
				catch ( NumberFormatException e ) {}

				if ( enemyPresent && shipEventId != null && autoBlueprintId.length() > 0 && !"null".equals( autoBlueprintId ) ) {
					beaconRef.get().setEnemyPresent( true );
					beaconRef.get().setShipEventId( shipEventId );
					beaconRef.get().setAutoBlueprintId( autoBlueprintId );
					beaconRef.get().setShipEventSeed( shipEventSeed );
				} else {
					beaconRef.get().setEnemyPresent( false );
					beaconRef.get().setShipEventId( null );
					beaconRef.get().setAutoBlueprintId( null );
					beaconRef.get().setShipEventSeed( 0 );
				}

				beaconRef.get().setFleetPresence( (FleetPresence)editorPanel.getCombo( FLEET ).getSelectedItem() );
				beaconRef.get().setUnderAttack( editorPanel.getBoolean( UNDER_ATTACK ).isSelected() );

				beaconRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		ActionListener beaconListener = new ActionListener() {
			private JComboBox starsListCombo = editorPanel.getCombo( STARS_LIST );
			private JComboBox starsImageCombo = editorPanel.getCombo( STARS_IMAGE );
			private JComboBox spriteListCombo = editorPanel.getCombo( SPRITE_LIST );
			private JComboBox spriteImageCombo = editorPanel.getCombo( SPRITE_IMAGE );
			private JTextField spriteXField = editorPanel.getInt( SPRITE_X );
			private JTextField spriteYField = editorPanel.getInt( SPRITE_Y );
			private JTextField spriteRotField = editorPanel.getInt( SPRITE_ROT );

			private JCheckBox enemyPresentCheck = editorPanel.getBoolean( ENEMY_PRESENT );
			private JComboBox shipEventCombo = editorPanel.getCombo( SHIP_EVENT );
			private JTextField autoShipField = editorPanel.getString( AUTO_SHIP );
			private JTextField shipEventSeedField = editorPanel.getInt( SHIP_EVENT_SEED );

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				boolean resize = false;

				if ( source == starsListCombo ) {
					Object imageListObj = starsListCombo.getSelectedItem();
					starsImageCombo.removeAllItems();
					starsImageCombo.addItem( "" );
					if ( imageListObj instanceof BackgroundImageList ) {
						for ( BackgroundImage img : ((BackgroundImageList)imageListObj).getImages() ) {
							starsImageCombo.addItem( img );
						}
					}
					resize = true;
				}
				else if ( source == spriteListCombo ) {
					Object imageListObj = spriteListCombo.getSelectedItem();
					spriteImageCombo.removeAllItems();
					spriteImageCombo.addItem( "" );
					spriteImageCombo.addItem( "NONE" );
					if ( imageListObj instanceof BackgroundImageList ) {
						for ( BackgroundImage img : ((BackgroundImageList)imageListObj).getImages() ) {
							spriteImageCombo.addItem( img );
						}
					}
					resize = true;
				}
				else if ( source == enemyPresentCheck ) {
					boolean enemyPresent = enemyPresentCheck.isSelected();
					if ( !enemyPresent ) {
						shipEventCombo.setSelectedItem( "" );
						shipEventSeedField.setText( "0" );
					}
					shipEventCombo.setEnabled( enemyPresent );
					autoShipField.setEnabled( enemyPresent );
					shipEventSeedField.setEnabled( enemyPresent );
				}
				else if ( source == shipEventCombo ) {
					Object shipEventObj = shipEventCombo.getSelectedItem();
					if ( shipEventObj instanceof ShipEvent ) {
						String autoBlueprintId = ((ShipEvent)shipEventObj).getAutoBlueprintId();
						editorPanel.getString( AUTO_SHIP ).setText( autoBlueprintId );
					} else {
						editorPanel.getString( AUTO_SHIP ).setText( "" );
					}
				}
				if ( resize ) {
					editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
					fitSidePanel();
				}
			}
		};
		editorPanel.getCombo( STARS_LIST ).addActionListener( beaconListener );
		editorPanel.getCombo( SPRITE_LIST ).addActionListener( beaconListener );
		editorPanel.getBoolean( ENEMY_PRESENT ).addActionListener( beaconListener );
		editorPanel.getCombo( SHIP_EVENT ).addActionListener( beaconListener );

		ChangeListener visitListener = new ChangeListener() {
			private JSpinner visitCountSpinner = editorPanel.getSpinner( VISIT_COUNT );
			private JComboBox starsListCombo = editorPanel.getCombo( STARS_LIST );
			private JComboBox starsImageCombo = editorPanel.getCombo( STARS_IMAGE );
			private JComboBox spriteListCombo = editorPanel.getCombo( SPRITE_LIST );
			private JComboBox spriteImageCombo = editorPanel.getCombo( SPRITE_IMAGE );
			private JTextField spriteXField = editorPanel.getInt( SPRITE_X );
			private JTextField spriteYField = editorPanel.getInt( SPRITE_Y );
			private JTextField spriteRotField = editorPanel.getInt( SPRITE_ROT );

			@Override
			public void stateChanged( ChangeEvent e ) {
				Object source = e.getSource();
				boolean resize = false;

				if ( source == visitCountSpinner ) {
					int visitCount = editorPanel.parseSpinnerInt( VISIT_COUNT );
					if ( visitCount == 0 ) {
						starsListCombo.setSelectedItem( "" );
						spriteListCombo.setSelectedItem( "" );
						starsImageCombo.setSelectedItem( "" );
						spriteImageCombo.setSelectedItem( "" );
						spriteXField.setText( "" );
						spriteYField.setText( "" );
						spriteRotField.setText( "0" );
					}
					starsListCombo.setEnabled( (visitCount > 0) );
					starsImageCombo.setEnabled( (visitCount > 0) );
					spriteListCombo.setEnabled( (visitCount > 0) );
					spriteImageCombo.setEnabled( (visitCount > 0) );
					spriteXField.setEnabled( (visitCount > 0) );
					spriteYField.setEnabled( (visitCount > 0) );
					spriteRotField.setEnabled( (visitCount > 0) );
				}
			}
		};
		editorPanel.getSpinner( VISIT_COUNT ).addChangeListener( visitListener );

		addSidePanelSeparator( 6 );

		JButton visitBtn = new JButton( "Visit" );
		visitBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		visitBtn.addMouseListener( new StatusbarMouseListener( frame, "Mark this beacon as visited, using random images." ) );
		sidePanel.add( visitBtn );

		visitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				String[] listCombos = new String[] {STARS_LIST, SPRITE_LIST};
				String[] listNames = new String[] {"BACKGROUND", "PLANET"};
				BackgroundImageList[] defaultLists = new BackgroundImageList[listCombos.length];
				final String[] imageCombos = new String[] {STARS_IMAGE, SPRITE_IMAGE};
				final BackgroundImage[] randomImages = new BackgroundImage[listCombos.length];

				for ( int i=0; i < listCombos.length; i++ ) {
					defaultLists[i] = allImageListsMap.get( listNames[i] );
					if ( defaultLists[i] == null || defaultLists[i].getImages().size() == 0 ) {
						frame.setStatusText( String.format( "Random visit failed. The default \"%s\" image list was missing or empty.", listNames[i] ) );
						return;
					}
					randomImages[i] = defaultLists[i].getImages().get( (int)(Math.random()*defaultLists[i].getImages().size()) );
				}

				if ( editorPanel.parseSpinnerInt( VISIT_COUNT ) == 0 ) {
					editorPanel.getSpinner( VISIT_COUNT ).setValue( 1 );
				}

				for ( int i=0; i < listCombos.length; i++ ) {
					editorPanel.getCombo( listCombos[i] ).setSelectedItem( defaultLists[i] );
				}

				// Wait for the image combos to populate.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						for ( int i=0; i < imageCombos.length; i++ ) {
							editorPanel.getCombo( imageCombos[i] ).setSelectedItem( randomImages[i] );
							if ( imageCombos[i].equals( SPRITE_IMAGE ) ) {
								int bgSpritePosX = (int)(Math.random() * (SCREEN_WIDTH - randomImages[i].getWidth()));
								int bgSpritePosY = (int)(Math.random() * (SCREEN_HEIGHT - randomImages[i].getHeight()));
								int bgSpriteRotation = (Math.random() >= 0.5 ? 0 : 180);
								editorPanel.getInt( SPRITE_X ).setText( ""+ bgSpritePosX );
								editorPanel.getInt( SPRITE_Y ).setText( ""+ bgSpritePosY );
								editorPanel.getInt( SPRITE_ROT ).setText( ""+ bgSpriteRotation );
							}
						}
					}
				});
			}
		});

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showPlayerShipEditor( final PlayerShipSprite playerShipSprite ) {

		String title = String.format( "Player Ship (Beacon %02d)", mapLayout.getBeaconId( playerShipSprite ) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		addSidePanelSeparator( 6 );

		JButton moveBtn = new JButton( "Move To..." );
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( moveBtn );

		moveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				movePlayerShip( playerShipSprite );
			}
		});

		showSidePanel();
	}

	private void showStoreEditor( final SpriteReference<BeaconState> beaconRef ) {
		final String FUEL = "Fuel";
		final String MISSILES = "Missiles";
		final String DRONE_PARTS = "Drone Parts";

		final StoreSprite storeSprite = beaconRef.getSprite( StoreSprite.class );

		String title = String.format("Store (Beacon %02d)", mapLayout.getBeaconId( storeSprite ) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );

		editorPanel.getInt( FUEL ).setText( ""+ beaconRef.get().getStore().getFuel() );
		editorPanel.getInt( MISSILES ).setText( ""+ beaconRef.get().getStore().getMissiles() );
		editorPanel.getInt( DRONE_PARTS ).setText( ""+ beaconRef.get().getStore().getDroneParts() );

		final JTabbedPane shelfTabsPane = new JTabbedPane();
		shelfTabsPane.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		final List<StoreShelfPanel> shelfPanels = new ArrayList<StoreShelfPanel>();

		for ( StoreShelf shelf : beaconRef.get().getStore().getShelfList() ) {
			StoreShelfPanel shelfPanel = new StoreShelfPanel( frame, ftlConstants );
			shelfPanel.setShelf( shelf );
			shelfPanels.add( shelfPanel );
			shelfTabsPane.addTab( "Shelf #"+ (shelfPanels.size()-1), shelfPanel );
		}

		JPanel extraPanel = new JPanel();
		extraPanel.setLayout( new BoxLayout( extraPanel, BoxLayout.Y_AXIS ) );

		JPanel shelfCtrlPanel = new JPanel();
		shelfCtrlPanel.setLayout( new BoxLayout( shelfCtrlPanel, BoxLayout.X_AXIS ) );
		JButton shelfRemBtn = new JButton( "-1 Shelf" );
		shelfRemBtn.addMouseListener( new StatusbarMouseListener( frame, "Remove a shelf. (FTL 1.01-1.03.3 is limited to two shelves)" ) );
		shelfCtrlPanel.add( shelfRemBtn );
		shelfCtrlPanel.add( Box.createRigidArea( new Dimension( 15, 1 ) ) );
		JButton shelfAddBtn = new JButton( "+1 Shelf" );
		shelfAddBtn.addMouseListener( new StatusbarMouseListener( frame, "Add a shelf. (FTL 1.01-1.03.3 is limited to two shelves)" ) );
		shelfCtrlPanel.add( shelfAddBtn );
		extraPanel.add( shelfCtrlPanel );

		extraPanel.add( Box.createVerticalStrut( 10 ) );

		shelfTabsPane.setMaximumSize( new Dimension( Integer.MAX_VALUE, shelfTabsPane.getPreferredSize().height ) );
		extraPanel.add( shelfTabsPane );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				try { beaconRef.get().getStore().setFuel( editorPanel.parseInt( FUEL ) ); }
				catch ( NumberFormatException e ) {}

				try { beaconRef.get().getStore().setMissiles( editorPanel.parseInt( MISSILES ) ); }
				catch ( NumberFormatException e ) {}

				try { beaconRef.get().getStore().setDroneParts( editorPanel.parseInt( DRONE_PARTS ) ); }
				catch ( NumberFormatException e ) {}

				beaconRef.get().getStore().getShelfList().clear();
				for ( StoreShelfPanel shelfPanel : shelfPanels ) {
					StoreShelf newShelf = new StoreShelf();

					SavedGameParser.StoreItemType itemType = shelfPanel.getItemType();
					List<SavedGameParser.StoreItem> items = shelfPanel.getItems();

					newShelf.setItemType( itemType );
					for ( SavedGameParser.StoreItem item : items ) {
						newShelf.addItem( item );
					}

					beaconRef.get().getStore().addShelf( newShelf );
				}

				beaconRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, extraPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( removeBtn );

		shelfRemBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( shelfPanels.size() > 1 ) {
					int lastIndex = shelfPanels.size()-1;
					shelfPanels.remove( lastIndex );
					shelfTabsPane.removeTabAt( lastIndex );
				}
			}
		});

		shelfAddBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				StoreShelfPanel shelfPanel = new StoreShelfPanel( frame, ftlConstants );
				shelfPanels.add( shelfPanel );
				shelfTabsPane.addTab( "Shelf #"+ (shelfPanels.size()-1), shelfPanel );
				shelfTabsPane.setSelectedIndex( shelfPanels.size()-1 );
			}
		});

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				storeSprites.remove( storeSprite );
				mapPanel.remove( storeSprite );
				beaconRef.get().setStore( null );

				beaconRef.fireReferenceChange();
			}
		});

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}

	private void showQuestEditor( final QuestSprite questSprite ) {
		final String ENCOUNTERS_FILE = "File";
		final String EVENT = "Event";

		final Map<String, Encounters> allEncountersMap = DataManager.get().getEncounters();

		String title = String.format("Quest (Beacon %02d)", mapLayout.getBeaconId( questSprite ) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( ENCOUNTERS_FILE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( EVENT, FieldEditorPanel.ContentType.COMBO );

		editorPanel.getCombo( ENCOUNTERS_FILE ).addItem( "" );
		for ( String fileName : allEncountersMap.keySet() ) {
			editorPanel.getCombo( ENCOUNTERS_FILE ).addItem( fileName );
		}

		// Preselect the file of the current event.
		editorPanel.getCombo( EVENT ).addItem( "" );
		for ( Map.Entry<String,Encounters> entry : allEncountersMap.entrySet() ) {
			FTLEvent currentEvent = entry.getValue().getEventById( questSprite.getQuestId() );
			FTLEventList currentEventList = entry.getValue().getEventListById( questSprite.getQuestId() );
			if ( currentEvent != null || currentEventList != null ) {
				editorPanel.getCombo( ENCOUNTERS_FILE ).setSelectedItem( entry.getKey() );

				for ( FTLEvent tmpEvent : entry.getValue().getEvents() ) {
					if ( tmpEvent.getId() != null )
						editorPanel.getCombo( EVENT ).addItem( tmpEvent );
				}
				editorPanel.getCombo( EVENT ).addItem( "- = Lists = -" );
				for ( FTLEventList tmpEventList : entry.getValue().getEventLists() ) {
					if ( tmpEventList.getId() != null )
						editorPanel.getCombo( EVENT ).addItem( tmpEventList );
				}
				if ( currentEvent != null ) {
					editorPanel.getCombo( EVENT ).setSelectedItem( currentEvent );
				} else if ( currentEventList != null ) {
					editorPanel.getCombo( EVENT ).setSelectedItem( currentEventList );
				} else {
					editorPanel.getCombo( EVENT ).setSelectedItem( "" );
				}
				break;
			}
		}
		// If no file contains the current event, complain.
		if ( "".equals( editorPanel.getCombo( ENCOUNTERS_FILE ).getSelectedItem() ) ) {
			String message = String.format( "The current event/eventlist id is unrecognized: %s", questSprite.getQuestId() );
			log.error( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object evtObj = editorPanel.getCombo( EVENT ).getSelectedItem();
				if ( evtObj instanceof FTLEvent ) {
					questSprite.setQuestId( ((FTLEvent)evtObj).getId() );
				}
				else if ( evtObj instanceof FTLEventList ) {
					questSprite.setQuestId( ((FTLEventList)evtObj).getId() );
				}
				else {
					frame.setStatusText( "No event/eventlist id has been selected." );
					return;
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, null, applyCallback );

		ActionListener questListener = new ActionListener() {
			private JComboBox fileCombo = editorPanel.getCombo( ENCOUNTERS_FILE );
			private JComboBox eventCombo = editorPanel.getCombo( EVENT );

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == fileCombo ) {
					eventCombo.removeAllItems();
					Object selectedFile = fileCombo.getSelectedItem();

					editorPanel.getCombo( EVENT ).addItem( "" );
					if ( "".equals( selectedFile ) == false ) {
						Encounters tmpEncounters = allEncountersMap.get( selectedFile );
						if ( tmpEncounters != null ) {
							for ( FTLEvent tmpEvent : tmpEncounters.getEvents() ) {
								if ( tmpEvent.getId() != null )
									eventCombo.addItem( tmpEvent );
							}
							eventCombo.addItem( "- = Lists = -" );
							for ( FTLEventList tmpEventList : tmpEncounters.getEventLists() ) {
								if ( tmpEventList.getId() != null )
									eventCombo.addItem( tmpEventList );
							}
							editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
							fitSidePanel();
						}
					}
				}
			}
		};
		editorPanel.getCombo( ENCOUNTERS_FILE ).addActionListener( questListener );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( removeBtn );

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				questSprites.remove( questSprite );
				mapPanel.remove( questSprite );
			}
		});

		editorPanel.setMaximumSize( editorPanel.getPreferredSize() );
		showSidePanel();
	}



	/**
	 * Reticle-bordered text, with an arrow in the south-west corner.
	 *
	 * FTL's image for map boxes has changed significantly across editions.
	 * This component just draws everything from scratch for consistency. The
	 * border will stretch to fit the text. Colors default to white on green.
	 * To change that, call setForeground() / setBackground().
	 *
	 * FTL 1.01-1.5.13 had dedicated images for: distress, quest, repair,
	 * store, and exit. FTL 1.6.1 switched to a stretchy NinePatch approach: a
	 * left border, a right border, and a single-pixel middle.
	 *
	 * FTL 1.03.3: "img/map/map_box_[...].png" (128x64, extra black
	 * bottom/right margins).
	 * 
	 * FTL 1.5.13: "img/map/map_box_[...].png" (80x40, as before but without
	 * those margins).
	 *
	 * FTL 1.6.1: "img/map/map_box_white_[123].png" (19x32, 1x32, 15x32, for
	 * roughly 8pt text 9px tall).
	 */
	public static class MapBoxComponent extends JComponent {

		private Font titleFont = new Font( Font.SANS_SERIF, Font.BOLD, 8 );
		private Insets boxMargin = new Insets( 6, 12, 12, 8 );
		private Insets boxPadding = new Insets( 2, 3, 2, 3 );
		private int boxThickness = 2;
		private int reticleInner = 0;  // Stroke extends inward already.
		private int reticleOuter = 2;
		private BasicStroke boxStroke = new BasicStroke( boxThickness );

		private String title;


		public MapBoxComponent( String title ) {
			this.title = title;
			this.setForeground( new Color( 234, 245, 229 ) );
			this.setBackground( new Color( 40, 81, 84 ) );

			BufferedImage dummyImage = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_RGB );
			Graphics2D g2d = dummyImage.createGraphics();
			try {
				g2d.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
				g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP );

				g2d.setFont( titleFont );
				FontMetrics fm = g2d.getFontMetrics();
				Rectangle2D titleBounds = fm.getStringBounds( title, g2d );
				int titleWidth = (int)titleBounds.getWidth();
				int titleHeight = fm.getAscent();
				int boxWidth = boxPadding.left + titleWidth + boxPadding.right + boxThickness*2;
				int boxHeight = boxPadding.top + titleHeight + boxPadding.bottom + boxThickness*2;

				int preferredWidth = boxMargin.left + boxWidth + boxMargin.right + reticleOuter;
				int preferredHeight = boxMargin.top + boxHeight + boxMargin.bottom + reticleOuter;
				this.setPreferredSize( new Dimension( preferredWidth, preferredHeight ) );
			}
			finally {
				g2d.dispose();
			}

			// Converting text to a shape is more accurate than FontMetrics estimates.
			//   https://stackoverflow.com/a/26955266
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			// Painting on a copy means no need to undo changes afterward.
			Graphics2D g2d = (Graphics2D)g.create();
			try {
				g2d.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
				g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
				g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP );
				g2d.setColor( this.getForeground() );

				g2d.setFont( titleFont );
				FontMetrics fm = g2d.getFontMetrics();
				Rectangle2D titleBounds = fm.getStringBounds( title, g2d );
				int titleWidth = (int)titleBounds.getWidth();
				int titleHeight = fm.getAscent();
				int titleX = boxMargin.left + boxThickness + boxPadding.left;
				int titleY = this.getHeight() - boxMargin.bottom - boxThickness - boxPadding.bottom;
				// drawString()'s y it at the baseline.

				int boxWidth = boxPadding.left + titleWidth + boxPadding.right + boxThickness*2;
				int boxHeight = boxPadding.top + titleHeight + boxPadding.bottom + boxThickness*2;
				int boxX = boxMargin.left;
				int boxY = this.getHeight() - boxHeight - boxMargin.bottom;

				g2d.setColor( this.getBackground() );
				g2d.fillRect( boxX, boxY, boxWidth, boxHeight );

				g2d.setColor( this.getForeground() );
				g2d.drawString( title, titleX, titleY );

				g2d.setStroke( boxStroke );
				g2d.drawRect( boxX, boxY, boxWidth, boxHeight );

				int reticleLength = reticleInner + boxThickness + reticleOuter;
				int reticleWestFromX = boxX - reticleOuter;
				int reticleWestToX = reticleWestFromX + reticleLength;
				int reticleEastFromX = boxX + boxWidth + reticleOuter;
				int reticleEastToX = reticleEastFromX - reticleLength;
				int reticleNorthFromY = boxY - reticleOuter;
				int reticleNorthToY = reticleNorthFromY + reticleLength;
				int reticleSouthFromY = boxY + boxHeight + reticleOuter;
				int reticleSouthToY = reticleSouthFromY - reticleLength;
				g2d.drawLine( reticleWestFromX, boxY + boxHeight/2, reticleWestToX, boxY + boxHeight/2 );
				g2d.drawLine( reticleEastFromX, boxY + boxHeight/2, reticleEastToX, boxY + boxHeight/2 );
				g2d.drawLine( boxX + boxWidth/2, reticleNorthFromY, boxX + boxWidth/2, reticleNorthToY );
				g2d.drawLine( boxX + boxWidth/2, reticleSouthFromY, boxX + boxWidth/2, reticleSouthToY );

				int triangleSide = 7;
				int triangleNorthX = boxX - triangleSide;
				int triangleNorthY = boxY + boxHeight;
				int triangleSouthX = triangleNorthX;
				int triangleSouthY = triangleNorthY + triangleSide;
				int triangleEastX = triangleSouthX + triangleSide;
				int triangleEastY = triangleSouthY;
				g2d.fillPolygon( new int[] {triangleNorthX, triangleSouthX, triangleEastX}, new int[] {triangleNorthY, triangleSouthY, triangleEastY}, 3 );
			}
			finally {
				g2d.dispose();
			}
		}
	}

	public class StoreSprite extends MapBoxComponent implements ReferenceSprite<BeaconState> {

		private SpriteReference<BeaconState> beaconRef;


		public StoreSprite( SpriteReference<BeaconState> beaconRef ) {
			super( "STORE" );
			this.beaconRef = beaconRef;

			beaconRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<BeaconState> getReference() {
			return beaconRef;
		}

		@Override
		public void referenceChanged() {
		}
	}



	public class QuestSprite extends MapBoxComponent {

		private String questId = null;
		private BufferedImage currentImage = null;


		public QuestSprite( String questId ) {
			super( "QUEST" );
			this.questId = questId;
		}

		public void setQuestId( String s ) { questId = s; }
		public String getQuestId() { return questId; }
	}



	public class BeaconSprite extends JComponent implements ReferenceSprite<BeaconState> {
		private BufferedImage currentImage = null;

		private SpriteReference<BeaconState> beaconRef;


		public BeaconSprite( SpriteReference<BeaconState> beaconRef ) {
			this.beaconRef = beaconRef;

			beaconRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<BeaconState> getReference() {
			return beaconRef;
		}

		@Override
		public void referenceChanged() {
			if ( FleetPresence.REBEL.equals( beaconRef.get().getFleetPresence() ) ) {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_warning.png", -1*32, -1*32, cachedImages );
			}
			else if ( beaconRef.get().getVisitCount() > 0 ) {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_diamond_blue.png", -1*32, -1*32, cachedImages );
			}
			else {
				currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_diamond_yellow.png", -1*32, -1*32, cachedImages );
			}
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;

			// If under attack, paint a 24x24 red (#AA2D1F) circle.
			if ( beaconRef.get().isUnderAttack() ) {
				g2d.setColor( new Color(170, 45, 31) );
				int diameter = 24;
				g2d.fill( new Ellipse2D.Double(this.getWidth()/2-diameter/2, this.getHeight()/2-diameter/2, diameter, diameter) );
			}
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class PlayerShipSprite extends JComponent {
		private BufferedImage currentImage = null;

		public PlayerShipSprite() {
			currentImage = ImageUtilities.getScaledImage( "img/map/map_icon_ship.png", -1*64, -1*64, cachedImages );
			this.setPreferredSize( new Dimension( currentImage.getWidth(), currentImage.getHeight() ) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this );
		}
	}
}
