package net.blerf.ftl.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.awt.image.RescaleOp;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.constants.OriginalFTLConstants;
import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.StationDirection;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.RegexDocument;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.StatusViewport;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameFloorplanPanel extends JPanel {

	private static final Integer WEAPON_LAYER = new Integer(5);
	private static final Integer BASE_LAYER = new Integer(10);
	private static final Integer FLOOR_LAYER = new Integer(11);
	private static final Integer ROOM_LAYER = new Integer(12);
	private static final Integer DECOR_LAYER = new Integer(13);
	private static final Integer WALL_LAYER = new Integer(15);
	private static final Integer SYSTEM_LAYER = new Integer(16);
	private static final Integer BREACH_LAYER = new Integer(17);
	private static final Integer FIRE_LAYER = new Integer(18);
	private static final Integer CREW_LAYER = new Integer(19);
	private static final Integer DOOR_LAYER = new Integer(20);
	private static final Integer DRONE_LAYER = new Integer(21);
	private static final Integer MISC_SELECTION_LAYER = new Integer(50);
	private static final Integer SQUARE_SELECTION_LAYER = new Integer(60);
	private static final int squareSize = 35, tileEdge = 1;
	private static final Logger log = LogManager.getLogger(SavedGameFloorplanPanel.class);

	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private GraphicsDevice gs = ge.getDefaultScreenDevice();
	private GraphicsConfiguration gc = gs.getDefaultConfiguration();

	private FTLFrame frame;

	private FTLConstants ftlConstants = new OriginalFTLConstants();
	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;
	private int shipReservePowerCapacity = 0;
	private String shipName = null;
	private int shipHull = 0;
	private int shipFuel = 0;
	private int shipDroneParts = 0;
	private int shipMissiles = 0;
	private int shipScrap = 0;
	private List<String> shipAugmentIdList = new ArrayList<String>();

	private int originX=0, originY=0;
	private HashMap<Rectangle, Integer> roomRegions = new HashMap<Rectangle, Integer>();
	private HashMap<Rectangle, Integer> squareRegions = new HashMap<Rectangle, Integer>();
	private ArrayList<Rectangle> blockedRegions = new ArrayList<Rectangle>();
	private ArrayList<JComponent> roomDecorations = new ArrayList<JComponent>();
	private ArrayList<DroneSprite> droneSprites = new ArrayList<DroneSprite>();
	private ArrayList<WeaponSprite> weaponSprites = new ArrayList<WeaponSprite>();
	private ArrayList<RoomSprite> roomSprites = new ArrayList<RoomSprite>();
	private ArrayList<SystemSprite> systemSprites = new ArrayList<SystemSprite>();
	private ArrayList<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private ArrayList<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private ArrayList<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private ArrayList<CrewSprite> crewSprites = new ArrayList<CrewSprite>();

	private HashMap<String, HashMap<Rectangle, BufferedImage>> cachedImages = new HashMap<String, HashMap<Rectangle, BufferedImage>>();
	private HashMap<BufferedImage, HashMap<Tint, BufferedImage>> cachedTintedImages = new HashMap<BufferedImage, HashMap<Tint, BufferedImage>>();
	private HashMap<String, BufferedImage> cachedPlayerBodyImages = new HashMap<String, BufferedImage>();
	private HashMap<String, BufferedImage> cachedEnemyBodyImages = new HashMap<String, BufferedImage>();

	private JLayeredPane shipPanel = null;
	private StatusViewport shipViewport = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private JLabel wallLbl = null;
	private JLabel crewLbl = null;
	private SpriteSelector miscSelector = null;
	private SquareSelector squareSelector = null;



	public SavedGameFloorplanPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		shipPanel = new JLayeredPane();
		shipPanel.setBackground( new Color(212, 208, 200) );
		shipPanel.setOpaque(true);
		shipPanel.setPreferredSize( new Dimension(50, 50) );

		sidePanel = new JPanel();
		sidePanel.setLayout( new BoxLayout(sidePanel, BoxLayout.Y_AXIS) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 6) );

		baseLbl = new JLabel();
		baseLbl.setOpaque(false);
		baseLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( baseLbl, BASE_LAYER );

		floorLbl = new JLabel();
		floorLbl.setOpaque(false);
		floorLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( floorLbl, FLOOR_LAYER );

		wallLbl = new JLabel();
		wallLbl.setOpaque(false);
		wallLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( wallLbl, WALL_LAYER );

		miscSelector = new SpriteSelector();
		miscSelector.addSpriteList(droneSprites);
		miscSelector.addSpriteList(weaponSprites);
		miscSelector.addSpriteList(doorSprites);
		miscSelector.setOpaque(false);
		miscSelector.setBounds( 0, 0, 50, 50 );
		shipPanel.add( miscSelector, MISC_SELECTION_LAYER );

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
					//if ( keepSelecting == false )
						//miscSelector.reset();  // Never stop selecting.
					miscSelector.setMousePoint( -1, -1 );
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					//miscSelector.reset();  // Never stop selecting.
					miscSelector.setMousePoint( -1, -1 );
				}
			}
			@Override
			public void mouseEntered( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( true );
				shipViewport.setStatusString( miscSelector.getCriteria().getDescription() );
			}
			@Override
			public void mouseExited( MouseEvent e ) {
				//miscSelector.setDescriptionVisible( false );
				shipViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		};
		miscSelector.addMouseListener( miscListener );
		miscSelector.addMouseMotionListener( miscListener );

		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Door, Drone, or Weapon";

			@Override
			public String getDescription() { return desc; }
		});

		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof DoorSprite ) {
					showDoorEditor( (DoorSprite)sprite );
				}
				else if ( sprite instanceof DroneSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Drone editing is not possible yet for Advanced Edition saved games.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						showDroneEditor( (DroneSprite)sprite );
					}
				}
				else if ( sprite instanceof WeaponSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Weapon editing is not possible yet for Advanced Edition saved games.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						showWeaponEditor( (WeaponSprite)sprite );
					}
				}

				return true;
			}
		});

		squareSelector = new SquareSelector( roomRegions, squareRegions );
		squareSelector.setOpaque(false);
		squareSelector.setBounds( 0, 0, 50, 50 );
		shipPanel.add( squareSelector, SQUARE_SELECTION_LAYER );

		MouseInputAdapter squareListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved( MouseEvent e ) {
				squareSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked( MouseEvent e ) {
				// Left-click triggers callback. Other buttons cancel.

				if ( e.getButton() == MouseEvent.BUTTON1 ) {
					if ( !squareSelector.isCurrentSquareValid() ) return;
					boolean keepSelecting = false;
					SquareSelectionCallback callback = squareSelector.getCallback();
					if ( callback != null )
						keepSelecting = callback.squareSelected( squareSelector, squareSelector.getRoomId(), squareSelector.getSquareId() );
					if ( keepSelecting == false )
						squareSelector.reset();
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					squareSelector.reset();
				}
			}
			@Override
			public void mouseEntered( MouseEvent e ) {
				//squareSelector.setDescriptionVisible( true );
				shipViewport.setStatusString( squareSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
			}
			@Override
			public void mouseExited( MouseEvent e ) {
				//squareSelector.setDescriptionVisible( false );
				shipViewport.setStatusString( null );
				squareSelector.setMousePoint( -1, -1 );
			}
		};
		squareSelector.addMouseListener( squareListener );
		squareSelector.addMouseMotionListener( squareListener );

		Insets ctrlInsets = new Insets(3, 4, 3, 4);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout(selectPanel, BoxLayout.X_AXIS) );
		selectPanel.setBorder( BorderFactory.createTitledBorder("Select") );
		final JButton selectRoomBtn = new JButton("Room");
		selectRoomBtn.setMargin(ctrlInsets);
		selectPanel.add( selectRoomBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectSystemBtn = new JButton("System");
		selectSystemBtn.setMargin(ctrlInsets);
		selectPanel.add( selectSystemBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectCrewBtn = new JButton("Crew");
		selectCrewBtn.setMargin(ctrlInsets);
		selectPanel.add( selectCrewBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectBreachBtn = new JButton("Breach");
		selectBreachBtn.setMargin(ctrlInsets);
		selectPanel.add( selectBreachBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectFireBtn = new JButton("Fire");
		selectFireBtn.setMargin(ctrlInsets);
		selectPanel.add( selectFireBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout(addPanel, BoxLayout.X_AXIS) );
		addPanel.setBorder( BorderFactory.createTitledBorder("Add") );
		final JButton addCrewBtn = new JButton("Crew");
		addCrewBtn.setMargin(ctrlInsets);
		addPanel.add( addCrewBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addBreachBtn = new JButton("Breach");
		addBreachBtn.setMargin(ctrlInsets);
		addPanel.add( addBreachBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addFireBtn = new JButton("Fire");
		addFireBtn.setMargin(ctrlInsets);
		addPanel.add( addFireBtn );

		JPanel resetPanel = new JPanel();
		resetPanel.setLayout( new BoxLayout(resetPanel, BoxLayout.X_AXIS) );
		resetPanel.setBorder( BorderFactory.createTitledBorder("Reset") );
		final JButton resetOxygenBtn = new JButton("Oxygen");
		resetOxygenBtn.setMargin(ctrlInsets);
		resetPanel.add( resetOxygenBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetSystemsBtn = new JButton("Systems");
		resetSystemsBtn.setMargin(ctrlInsets);
		resetPanel.add( resetSystemsBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetIntrudersBtn = new JButton("Intruders");
		resetIntrudersBtn.setMargin(ctrlInsets);
		resetPanel.add( resetIntrudersBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetBreachesBtn = new JButton("Breaches");
		resetBreachesBtn.setMargin(ctrlInsets);
		resetPanel.add( resetBreachesBtn );
		resetPanel.add( Box.createHorizontalStrut(5) );
		final JButton resetFiresBtn = new JButton("Fires");
		resetFiresBtn.setMargin(ctrlInsets);
		resetPanel.add( resetFiresBtn );

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout(otherPanel, BoxLayout.X_AXIS) );
		otherPanel.setBorder( BorderFactory.createTitledBorder("Other") );
		final JButton otherGeneralBtn = new JButton("General");
		otherGeneralBtn.setMargin(ctrlInsets);
		otherPanel.add( otherGeneralBtn );
		otherPanel.add( Box.createHorizontalStrut(5) );
		final JButton otherAugmentsBtn = new JButton("Augments");
		otherAugmentsBtn.setMargin(ctrlInsets);
		otherPanel.add( otherAugmentsBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout(ctrlRowOnePanel, BoxLayout.X_AXIS) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut(15) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlRowTwoPanel = new JPanel();
		ctrlRowTwoPanel.setLayout( new BoxLayout(ctrlRowTwoPanel, BoxLayout.X_AXIS) );
		ctrlRowTwoPanel.add( resetPanel );
		ctrlRowTwoPanel.add( Box.createHorizontalStrut(15) );
		ctrlRowTwoPanel.add( otherPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS) );
		ctrlPanel.add( ctrlRowOnePanel );
		ctrlPanel.add( Box.createVerticalStrut(8) );
		ctrlPanel.add( ctrlRowTwoPanel );

		ActionListener ctrlListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if ( shipBlueprint == null ) return;  // No ship to edit!

				Object source = e.getSource();
				if ( source == selectRoomBtn ) {
					selectRoom();
				} else if ( source == selectSystemBtn ) {
					selectSystem();
				} else if ( source == selectCrewBtn ) {
					selectCrew();
				} else if ( source == selectBreachBtn ) {
					selectBreach();
				} else if ( source == selectFireBtn ) {
					selectFire();
				} else if ( source == addCrewBtn ) {
					addCrew();
				} else if ( source == addBreachBtn ) {
					addBreach();
				} else if ( source == addFireBtn ) {
					addFire();
				}
				else if ( source == resetOxygenBtn ) {
					clearSidePanel();
					for ( RoomSprite roomSprite : roomSprites ) {
						if ( roomSprite.getOxygen() != 100 ) {
							roomSprite.setOxygen(100);
						}
					}
					shipViewport.repaint();
				}
				else if ( source == resetSystemsBtn ) {
					clearSidePanel();
					for ( SystemSprite systemSprite : systemSprites ) {
						systemSprite.setDamagedBars(0);
						systemSprite.setIonizedBars(0);
						systemSprite.setRepairProgress(0);
						systemSprite.setDamageProgress(0);
						systemSprite.setDeionizationTicks(Integer.MIN_VALUE);
						systemSprite.setTemporaryCapacityCap( 1000 );
						systemSprite.setTemporaryCapacityLoss( 0 );
						systemSprite.setTemporaryCapacityDivisor( 1 );
						systemSprite.makeSane();
					}
					shipViewport.repaint();
				}
				else if ( source == resetIntrudersBtn ) {
					clearSidePanel();
					for ( ListIterator<CrewSprite> it = crewSprites.listIterator(); it.hasNext(); ) {
						CrewSprite crewSprite = it.next();
						if ( !crewSprite.isPlayerControlled() ) {
							shipPanel.remove( crewSprite );
							it.remove();
						}
					}
					shipViewport.repaint();
				}
				else if ( source == resetBreachesBtn ) {
					clearSidePanel();
					for ( BreachSprite breachSprite : breachSprites )
						shipPanel.remove( breachSprite );
					breachSprites.clear();
					shipViewport.repaint();
				}
				else if ( source == resetFiresBtn ) {
					clearSidePanel();
					for ( FireSprite fireSprite : fireSprites )
						shipPanel.remove( fireSprite );
					fireSprites.clear();
					shipViewport.repaint();
				}
				else if ( source == otherGeneralBtn ) {
					showGeneralEditor();
				} else if (source == otherAugmentsBtn ) {
					showAugmentsEditor( shipAugmentIdList );
				}
			}
		};

		selectRoomBtn.addActionListener( ctrlListener );
		selectSystemBtn.addActionListener( ctrlListener );
		selectCrewBtn.addActionListener( ctrlListener );
		selectBreachBtn.addActionListener( ctrlListener );
		selectFireBtn.addActionListener( ctrlListener );

		addCrewBtn.addActionListener( ctrlListener );
		addBreachBtn.addActionListener( ctrlListener );
		addFireBtn.addActionListener( ctrlListener );

		resetOxygenBtn.addActionListener( ctrlListener );
		resetSystemsBtn.addActionListener( ctrlListener );
		resetIntrudersBtn.addActionListener( ctrlListener );
		resetBreachesBtn.addActionListener( ctrlListener );
		resetFiresBtn.addActionListener( ctrlListener );

		otherGeneralBtn.addActionListener( ctrlListener );
		otherAugmentsBtn.addActionListener( ctrlListener );

		resetOxygenBtn.addMouseListener( new StatusbarMouseListener(frame, "Set all rooms' oxygen to 100%.") );
		resetSystemsBtn.addMouseListener( new StatusbarMouseListener(frame, "Clear all system damage and temporary capacity limits.") );
		resetIntrudersBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all NPC crew.") );
		resetBreachesBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all breaches.") );
		resetFiresBtn.addMouseListener( new StatusbarMouseListener(frame, "Remove all fires.") );

		otherGeneralBtn.addMouseListener( new StatusbarMouseListener(frame, "Edit the ship's name, hull, and supplies.") );
		otherAugmentsBtn.addMouseListener( new StatusbarMouseListener(frame, "Edit Augments.") );

		JPanel centerPanel = new JPanel( new GridBagLayout() );

		GridBagConstraints gridC = new GridBagConstraints();

		shipViewport = new StatusViewport();

		gridC.fill = GridBagConstraints.BOTH;
		gridC.weightx = 1.0;
		gridC.weighty = 1.0;
		gridC.gridx = 0;
		gridC.gridy = 0;
		JScrollPane shipScroll = new JScrollPane();
		shipScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		shipScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
		shipScroll.setViewport( shipViewport );
		shipScroll.setViewportView( shipPanel );
		centerPanel.add( shipScroll, gridC );

		gridC.insets = new Insets(4, 4, 4, 4);

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
		sideScroll.setVisible( false );
		this.add( sideScroll, BorderLayout.EAST );
	}

	public void setShipState( SavedGameParser.SavedGameState gameState, SavedGameParser.ShipState shipState ) {
		String prevGfxBaseName = shipGfxBaseName;
		miscSelector.setVisible( false );
		miscSelector.setMousePoint( -1, -1 );
		squareSelector.reset();
		shipViewport.setStatusString( null );
		clearSidePanel();

		shipAugmentIdList.clear();

		for ( DroneSprite droneSprite : droneSprites ) {
			shipPanel.remove( droneSprite );
			shipPanel.remove( droneSprite.getBody() );
		}
		droneSprites.clear();

		for ( WeaponSprite weaponSprite : weaponSprites )
			shipPanel.remove( weaponSprite );
		weaponSprites.clear();

		for ( RoomSprite roomSprite : roomSprites )
			shipPanel.remove( roomSprite );
		roomSprites.clear();

		for ( SystemSprite systemSprite : systemSprites )
			shipPanel.remove( systemSprite );
		systemSprites.clear();

		for ( BreachSprite breachSprite : breachSprites )
			shipPanel.remove( breachSprite );
		breachSprites.clear();

		for ( FireSprite fireSprite : fireSprites )
			shipPanel.remove( fireSprite );
		fireSprites.clear();

		for ( DoorSprite doorSprite : doorSprites )
			shipPanel.remove( doorSprite );
		doorSprites.clear();

		for ( CrewSprite crewSprite : crewSprites )
			shipPanel.remove( crewSprite );
		crewSprites.clear();

		if ( shipState == null ) {
			shipBlueprint = null;
			shipLayout = null;
			shipChassis = null;
			shipGfxBaseName = null;
			shipReservePowerCapacity = 0;
			shipName = null;
			shipHull = 0;
			shipFuel = 0;
			shipDroneParts = 0;
			shipMissiles = 0;
			shipScrap = 0;
			roomRegions.clear();
			squareRegions.clear();
			blockedRegions.clear();
			baseLbl.setIcon(null);
			floorLbl.setIcon(null);

			for ( JComponent roomDecor : roomDecorations )
				shipPanel.remove( roomDecor );
			roomDecorations.clear();

			wallLbl.setIcon(null);
			return;
		}

		if ( gameState != null && gameState.getHeaderAlpha() == 2 ) {
			ftlConstants = new OriginalFTLConstants();
		} else {
			ftlConstants = new AdvancedFTLConstants();
		}

		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );
		shipGfxBaseName = shipState.getShipGraphicsBaseName();
		shipReservePowerCapacity = shipState.getReservePowerCapacity();
		shipName = shipState.getShipName();
		shipHull = shipState.getHullAmt();
		shipFuel = shipState.getFuelAmt();
		shipDroneParts = shipState.getDronePartsAmt();
		shipMissiles = shipState.getMissilesAmt();
		shipScrap = shipState.getScrapAmt();
		shipAugmentIdList.addAll( shipState.getAugmentIdList() );
		originX = shipChassis.getImageBounds().x * -1;
		originY = shipChassis.getImageBounds().y * -1;
		ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

		if ( shipGfxBaseName != prevGfxBaseName ) {
			// Associate graphical regions with roomIds and squares.
			roomRegions.clear();
			squareRegions.clear();
			for (int i=0; i < shipLayout.getRoomCount(); i++) {
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + roomLocX*squareSize;
				int roomY = originY + roomLocY*squareSize;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				for (int s=0; s < squaresH*squaresV; s++) {
					int squareX = roomX + tileEdge + (s%squaresH)*squareSize;
					int squareY = roomY + tileEdge + (s/squaresH)*squareSize;
					Rectangle squareRect = new Rectangle(squareX, squareY, squareSize, squareSize);
					roomRegions.put( squareRect, i );
					squareRegions.put( squareRect, s );
				}
			}
			// Find squares that don't allow crew in them (medbay's slot).
			blockedRegions.clear();
			ShipBlueprint.SystemList.SystemRoom medicalSystem = blueprintSystems.getMedicalRoom();
			if ( medicalSystem != null ) {
				ShipBlueprint.SystemList.RoomSlot medicalSlot = medicalSystem.getSlot();
				int badRoomId = medicalSystem.getRoomId();
				int badSquareId = 1;       // TODO: Magic number (when omitted, slot is #1).
				if ( medicalSlot != null )
					badSquareId = medicalSlot.getNumber();

				if ( badSquareId >= 0 ) {  // When -2, there's no blocked square.
					log.trace(String.format("Found a blocked region: roomId: %2d, squareId: %d", badRoomId, badSquareId) );

					EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(badRoomId);
					int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
					int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
					int roomX = originX + roomLocX*squareSize;
					int roomY = originY + roomLocY*squareSize;
					int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
					int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

					int squareX = roomX + tileEdge + (badSquareId%squaresH)*squareSize;
					int squareY = roomY + tileEdge + (badSquareId/squaresH)*squareSize;
					Rectangle squareRect = new Rectangle(squareX, squareY, squareSize, squareSize);
					blockedRegions.add( squareRect );
				}
			}


			// Load the fuselage image.
			baseLbl.setIcon(null);
			InputStream in = null;
			try {
				String baseImagePath = null;
				String[] candidatePaths = new String[2];
				candidatePaths[0] = "img/ship/"+ shipGfxBaseName +"_base.png";  // FTL 1.01-1.03.3 (All ships), 1.5.4 (Player ships)
				candidatePaths[1] = "img/ships_glow/"+ shipGfxBaseName +"_base.png";  // FTL 1.5.4 (Enemy ships)
				for ( String candidatePath : candidatePaths ) {
					if ( DataManager.get().hasResourceInputStream( candidatePath ) ) {
						baseImagePath = candidatePath;
					}
				}
				if ( baseImagePath == null ) {
					throw new FileNotFoundException();
				}

				in = DataManager.get().getResourceInputStream( baseImagePath );
				BufferedImage baseImage = ImageIO.read( in );
				in.close();
				baseLbl.setIcon( new ImageIcon(baseImage) );
				baseLbl.setSize( new Dimension(baseImage.getWidth(), baseImage.getHeight()) );
			}
			catch ( FileNotFoundException e ) {
				log.warn( "No ship base image for ("+ shipGfxBaseName +")" );
			}
			catch ( IOException e ) {
				log.error( "Failed to load ship base image ("+ shipGfxBaseName +")", e );
			}
			finally {
				try {if (in != null) in.close();}
				catch ( IOException e ) {}
	    }

			// Load the interior image.
			floorLbl.setIcon(null);
			floorLbl.setBounds( 0, 0, 50, 50 );
			try {
				in = DataManager.get().getResourceInputStream("img/ship/"+ shipGfxBaseName +"_floor.png");
				BufferedImage floorImage = ImageIO.read( in );
				in.close();
				floorLbl.setIcon( new ImageIcon(floorImage) );
				floorLbl.setSize( new Dimension(floorImage.getWidth(), floorImage.getHeight()) );

				if ( shipChassis.getOffsets() != null ) {
					ShipChassis.Offsets.Offset floorOffset = shipChassis.getOffsets().floorOffset;
					if ( floorOffset != null ) {
						floorLbl.setLocation( floorOffset.x, floorOffset.y );
					}
				}
			}
			catch ( FileNotFoundException e ) {
				log.warn( "No ship floor image for ("+ shipGfxBaseName +")" );
			}
			catch ( IOException e ) {
				log.error( "Failed to load ship floor image ("+ shipGfxBaseName +")", e );
			}
			finally {
				try {if (in != null) in.close();}
				catch ( IOException e ) {}
	    }

			for ( JComponent roomDecor : roomDecorations )
				shipPanel.remove( roomDecor );
			roomDecorations.clear();
			for ( ShipBlueprint.SystemList.SystemRoom systemRoom : blueprintSystems.getSystemRooms() ) {
				String roomImgPath = systemRoom.getImg();

				int roomId = systemRoom.getRoomId();
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(roomId);
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + roomLocX*squareSize;
				int roomY = originY + roomLocY*squareSize;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				// TODO: Looks like when medbay omits img, it's "room_medbay.png".

				if ( roomImgPath != null ) {
					// Gotta scale because Zoltan #2's got a tall Doors image for a wide room. :/
					BufferedImage decorImage = getScaledImage( "img/ship/interior/"+ roomImgPath +".png", squaresH*squareSize, squaresV*squareSize );
					JLabel decorLbl = new JLabel( new ImageIcon(decorImage) );
					decorLbl.setOpaque(false);
					decorLbl.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
					roomDecorations.add( decorLbl );
					shipPanel.add( decorLbl, DECOR_LAYER );
				}

				if ( systemRoom == blueprintSystems.getTeleporterRoom() ) {
					for (int s=0; s < squaresH*squaresV; s++) {
						int decorX = roomX + (s%squaresH)*squareSize + squareSize/2;
						int decorY = roomY + (s/squaresH)*squareSize + squareSize/2;

						BufferedImage decorImage = getScaledImage( "img/ship/interior/teleporter_off.png", 20, 20 );
						JLabel decorLbl = new JLabel( new ImageIcon(decorImage) );
						decorLbl.setOpaque(false);
						decorLbl.setSize( squaresH*squareSize, squaresV*squareSize );
						placeSprite( decorX, decorY, decorLbl );
						roomDecorations.add( decorLbl );
						shipPanel.add( decorLbl, DECOR_LAYER );
					}
				}
			}

			// Draw walls and floor crevices.
			BufferedImage wallImage = gc.createCompatibleImage( shipChassis.getImageBounds().w, shipChassis.getImageBounds().h, Transparency.BITMASK );
			Graphics2D wallG = (Graphics2D)wallImage.createGraphics();
			drawWalls( wallG, originX, originY, shipState, shipLayout );
			wallG.dispose();
			wallLbl.setIcon( new ImageIcon(wallImage) );
			wallLbl.setSize( new Dimension(wallImage.getWidth(), wallImage.getHeight()) );
		}

		// Add Drones.
		ArrayList<SavedGameParser.DroneState> droneList = shipState.getDroneList();
		Integer blueprintDroneSlots = shipBlueprint.getDroneSlots();

		if ( blueprintDroneSlots == null )       // TODO: Magic number (not set in autoBlueprints.xml).
			blueprintDroneSlots = new Integer(3);  // In-game GUI shows 2 or 3.

		int actualDroneSlots = droneList.size();
		if ( blueprintDroneSlots != null ) {
			if ( blueprintDroneSlots.intValue() >= droneList.size() )
				actualDroneSlots = blueprintDroneSlots.intValue();
			else
				log.warn( String.format("Ship state has %d drones, but its blueprint only expects %d", droneList.size(), shipBlueprint.getDroneSlots()) );
		}

		for (int i=0; i < actualDroneSlots; i++) {
			SavedGameParser.DroneState droneState = null;

			if ( droneList.size() > i ) droneState = droneList.get(i);
			// It's fine if droneState is null. Empty slot.

			int droneX = 100 + i*75;
			int droneY = shipChassis.getImageBounds().h;

			addDroneSprite( droneX, droneY, i, droneState );
		}

		// Add Weapons.
		List<ShipChassis.WeaponMountList.WeaponMount> weaponMounts = shipChassis.getWeaponMountList().mount;
		ArrayList<SavedGameParser.WeaponState> weaponList = shipState.getWeaponList();
		Integer blueprintWeaponSlots = shipBlueprint.getWeaponSlots();

		if ( blueprintWeaponSlots == null )       // TODO: Magic number (not set in autoBlueprints.xml).
			blueprintWeaponSlots = new Integer(4);  // But rebel_long's chassis has extra mounts!
		                                          // In-game GUI shows 3 or 4.

		int actualWeaponSlots = weaponMounts.size();
		if ( blueprintWeaponSlots != null ) {     // Check in case magic number is removed someday.
			if ( weaponMounts.size() < blueprintWeaponSlots.intValue() )
				log.warn( String.format("Ship blueprint expects %d mounts, but its chassis only has %d", shipBlueprint.getWeaponSlots(), weaponMounts.size()) );

			// Blueprint may restrict usable chassis mounts.
			actualWeaponSlots = Math.min( weaponMounts.size(), blueprintWeaponSlots.intValue() );
		}

		if ( weaponList.size() > weaponMounts.size() )
			log.warn( String.format("Ship state has %d weapons, but its chassis only has %d", weaponList.size(), weaponMounts.size()) );

		for (int i=0; i < actualWeaponSlots; i++) {
			ShipChassis.WeaponMountList.WeaponMount weaponMount = null;
			SavedGameParser.WeaponState weaponState = null;

			if ( weaponMounts.size() > i ) weaponMount = weaponMounts.get(i);
			if ( weaponMount == null ) continue;  // *shrug* Truncate extra weapons.

			if ( weaponList.size() > i ) weaponState = weaponList.get(i);
			// It's fine if weaponState is null. Empty slot.

			addWeaponSprite( i, weaponMount, weaponState );
		}

		// Add rooms.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + roomLocX*squareSize;
			int roomY = originY + roomLocY*squareSize;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();
			int oxygen = shipState.getRoom(i).getOxygen();

			RoomSprite roomSprite = new RoomSprite( i, shipState.getRoom(i) );
			roomSprite.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
			roomSprites.add( roomSprite );
			shipPanel.add( roomSprite, ROOM_LAYER );
		}

		// Add systems.
		for ( SystemType systemType : SystemType.values() ) {
			int[] roomIds = shipBlueprint.getSystemList().getRoomIdBySystemType( systemType );
			if ( roomIds != null ) {
				for (int i=0; i < roomIds.length; i++) {
					EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( roomIds[i] );
					int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
					int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
					int roomX = originX + roomLocX*squareSize;
					int roomY = originY + roomLocY*squareSize;
					int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
					int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

					int systemX = roomX + tileEdge + squaresH*squareSize/2;
					int systemY = roomY + tileEdge + squaresV*squareSize/2;

					SavedGameParser.SystemState systemState = shipState.getSystem( systemType );
					if ( systemState == null ) break;  // TODO: Support systems that aren't on the shipState.
					addSystemSprite( systemX, systemY, systemState );
				}
			}
		}

		// Add breaches
		for ( Map.Entry<Point, Integer> breachEntry : shipState.getBreachMap().entrySet() ) {
			int breachCoordX = breachEntry.getKey().x-shipLayout.getOffsetX();
			int breachCoordY = breachEntry.getKey().y-shipLayout.getOffsetY();
			int breachX = originX+tileEdge + breachCoordX*squareSize + squareSize/2;
			int breachY = originY+tileEdge + breachCoordY*squareSize + squareSize/2;

			Rectangle squareRect = null;
			int roomId = -1;
			int squareId = -1;
			for ( Map.Entry<Rectangle, Integer> regionEntry : roomRegions.entrySet() ) {
				if ( regionEntry.getKey().contains( breachX, breachY ) ) {
					squareRect = regionEntry.getKey();
					roomId = regionEntry.getValue().intValue();
					break;
				}
			}
			if ( squareRegions.containsKey(squareRect) )
				squareId = squareRegions.get(squareRect).intValue();

			addBreachSprite( breachX, breachY, roomId, squareId, breachEntry.getValue().intValue() );
		}

		// Add fires.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + roomLocX*squareSize;
			int roomY = originY + roomLocY*squareSize;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			for (int s=0; s < squaresH*squaresV; s++) {
				int fireHealth = roomState.getSquare(s).getFireHealth();
				if ( fireHealth > 0 ) {
					int fireX = roomX+tileEdge + (s%squaresH)*squareSize + squareSize/2;
					int fireY = roomY+tileEdge + (s/squaresH)*squareSize + squareSize/2;
					addFireSprite( fireX, fireY, i, s, fireHealth );
				}
			}
		}

		// Add doors.
		int doorLevel = shipState.getSystem(SystemType.DOORS).getCapacity()-1;  // Convert to 0-based.
		if ( doorLevel < 0 ) doorLevel = 0;  // Door subsystem was absent, 0-Capacity.
		for ( Map.Entry<ShipLayout.DoorCoordinate, SavedGameParser.DoorState> entry : shipState.getDoorMap().entrySet() ) {
			ShipLayout.DoorCoordinate doorCoord = entry.getKey();
			SavedGameParser.DoorState doorState = entry.getValue();
			int doorX = originX + doorCoord.x*squareSize + (doorCoord.v==1 ? 0 : squareSize/2);
			int doorY = originY + doorCoord.y*squareSize + (doorCoord.v==1 ? squareSize/2 : 0);

			addDoorSprite( doorX, doorY, doorLevel, doorCoord, doorState );
		}

		// Add crew.
		// TODO: Use the crew's actual spriteX/spriteY instead of room/square.
		// TODO: Add dead crew at their spriteX/spriteY but toggle visibility.
		int hadesX = 100 - (int)(squareSize * 1.5);
		int hadesY = shipChassis.getImageBounds().h;

		for ( SavedGameParser.CrewState crewState : shipState.getCrewList() ) {
			if ( crewState.getRoomId() != -1 ) {
				EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( crewState.getRoomId() );
				int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
				int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
				int roomX = originX + roomLocX*squareSize;
				int roomY = originY + roomLocY*squareSize;
				int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
				int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

				int crewX = roomX + tileEdge + (crewState.getRoomSquare()%squaresH)*squareSize + squareSize/2;
				int crewY = roomY + tileEdge + (crewState.getRoomSquare()/squaresH)*squareSize + squareSize/2;
				addCrewSprite( crewX, crewY, crewState );
			}
			else {
				addCrewSprite( hadesX, hadesY, crewState );
			}
		}

		int shipPanelWidth = 0, shipPanelHeight = 0;
		for ( Component c : shipPanel.getComponents() ) {
			shipPanelWidth = Math.max( c.getX()+c.getWidth(), shipPanelWidth );
			shipPanelHeight = Math.max( c.getY()+c.getHeight(), shipPanelHeight );
		}
		shipPanel.setPreferredSize( new Dimension(shipPanelWidth, shipPanelHeight) );

		miscSelector.setSize( shipPanelWidth, shipPanelHeight );
		squareSelector.setSize( shipPanelWidth, shipPanelHeight );

		miscSelector.setVisible( true );

		shipPanel.revalidate();
		shipViewport.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				shipViewport.setViewPosition(new Point(0,0));
			}
		});
	}

	public void updateShipState( SavedGameParser.ShipState shipState ) {
		if ( shipState == null ) return;

		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );

		shipState.setReservePowerCapacity( shipReservePowerCapacity );

		// General.
		shipState.setShipName( shipName );
		shipState.setHullAmt( shipHull );
		shipState.setFuelAmt( shipFuel );
		shipState.setDronePartsAmt( shipDroneParts );
		shipState.setMissilesAmt( shipMissiles );
		shipState.setScrapAmt( shipScrap );

		// Augments.
		shipState.getAugmentIdList().clear();
		shipState.getAugmentIdList().addAll( shipAugmentIdList );

		// Drones.
		ArrayList<SavedGameParser.DroneState> droneList = shipState.getDroneList();
		droneList.clear();
		for ( DroneSprite droneSprite : droneSprites ) {
			if ( droneSprite.getDroneId() != null ) {
				SavedGameParser.DroneState droneState = new SavedGameParser.DroneState();
				droneState.setDroneId( droneSprite.getDroneId() );
				droneState.setArmed( droneSprite.isArmed() );
				droneState.setPlayerControlled( droneSprite.isPlayerControlled() );
				droneState.setHealth( droneSprite.getHealth() );
				droneState.setSpriteX( droneSprite.getSpriteX() );
				droneState.setSpriteY( droneSprite.getSpriteY() );
				if ( droneSprite.getSpriteX() >= 0 && droneSprite.getSpriteY() >= 0 ) {
					for ( Map.Entry<Rectangle, Integer> regionEntry : squareRegions.entrySet() ) {
						if ( regionEntry.getKey().contains( droneSprite.getBody().getLocation() ) ) {
							droneState.setRoomSquare( regionEntry.getValue().intValue() );
							droneState.setRoomId( roomRegions.get(regionEntry.getKey()).intValue() );
							break;
						}
					}
				}
				droneList.add( droneState );
			}
		}

		// Weapons.
		ArrayList<SavedGameParser.WeaponState> weaponList = shipState.getWeaponList();
		weaponList.clear();
		for ( WeaponSprite weaponSprite : weaponSprites ) {
			if ( weaponSprite.getWeaponId() != null ) {
				SavedGameParser.WeaponState weaponState = new SavedGameParser.WeaponState();
				weaponState.setWeaponId( weaponSprite.getWeaponId() );
				weaponState.setArmed( weaponSprite.isArmed() );
				weaponState.setCooldownTicks( weaponSprite.getCooldownTicks() );
				weaponList.add( weaponState );
			}
		}

		// Rooms (This must come before Fires to avoid clobbering).
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			RoomSprite roomSprite = roomSprites.get(i);
			roomState.setOxygen( roomSprite.getOxygen() );
			roomState.setStationSquare( roomSprite.getStationSquare() );
			roomState.setStationDirection( roomSprite.getStationDirection() );

			ArrayList<SavedGameParser.SquareState> squareList = roomState.getSquareList();
			squareList.clear();
			squareList.addAll( roomSprites.get(i).getSquareList() );
		}

		// Systems.
		shipState.getSystemsMap().clear();

		for ( SystemSprite systemSprite : systemSprites ) {
			SavedGameParser.SystemState systemState = new SavedGameParser.SystemState( systemSprite.getSystemType() );
			systemState.setCapacity( systemSprite.getCapacity() );
			systemState.setPower( systemSprite.getPower() );
			systemState.setDamagedBars( systemSprite.getDamagedBars() );
			systemState.setIonizedBars( systemSprite.getIonizedBars() );
			systemState.setRepairProgress( systemSprite.getRepairProgress() );
			systemState.setDamageProgress( systemSprite.getDamageProgress() );
			systemState.setDeionizationTicks( systemSprite.getDeionizationTicks() );
			systemState.setBatteryPower( systemSprite.getBatteryPower() );
			systemState.setHackLevel( systemSprite.getHackLevel() );
			systemState.setHacked( systemSprite.isHacked() );
			systemState.setTemporaryCapacityCap( systemSprite.getTemporaryCapacityCap() );
			systemState.setTemporaryCapacityLoss( systemSprite.getTemporaryCapacityLoss() );
			systemState.setTemporaryCapacityDivisor( systemSprite.getTemporaryCapacityDivisor() );
			shipState.addSystem( systemState );
		}
		// Add omitted systems.
		for ( SystemType systemType : SystemType.values() ) {
			if ( shipState.getSystem( systemType ) == null )
				shipState.addSystem( new SavedGameParser.SystemState( systemType ) );
		}

		// Breaches.
		Map<Point, Integer> breachMap = shipState.getBreachMap();
		breachMap.clear();
		for ( BreachSprite breachSprite : breachSprites ) {
			int roomId = breachSprite.getRoomId();
			int squareId = breachSprite.getSquareId();

			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( roomId );
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			int breachX = roomLocX + squareId%squaresH + shipLayout.getOffsetX();
			int breachY = roomLocY + squareId/squaresV + shipLayout.getOffsetY();
			breachMap.put( new Point(breachX, breachY), new Integer(breachSprite.getHealth()) );
		}

		// Fires.
		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			SavedGameParser.RoomState roomState = shipState.getRoom(i);
			for ( SavedGameParser.SquareState squareState : roomState.getSquareList() )
				squareState.setFireHealth( 0 );
		}
		for ( FireSprite fireSprite : fireSprites ) {
			SavedGameParser.RoomState roomState = shipState.getRoom( fireSprite.getRoomId() );
			SavedGameParser.SquareState squareState = roomState.getSquareList().get( fireSprite.getSquareId() );
			squareState.setFireHealth( fireSprite.getHealth() );
		}

		// Doors.
		Map<ShipLayout.DoorCoordinate, SavedGameParser.DoorState> shipDoorMap = shipState.getDoorMap();
		shipDoorMap.clear();
		for ( DoorSprite doorSprite : doorSprites ) {
			SavedGameParser.DoorState doorState = new SavedGameParser.DoorState();
			doorState.setOpen( doorSprite.isOpen() );
			doorState.setWalkingThrough( doorSprite.isWalkingThrough() );
			doorState.setCurrentMaxHealth( doorSprite.getCurrentMaxHealth() );
			doorState.setHealth( doorSprite.getHealth() );
			doorState.setNominalHealth( doorSprite.getNominalHealth() );
			doorState.setUnknownDelta( doorSprite.getUnknownDelta() );
			doorState.setUnknownEpsilon( doorSprite.getUnknownEpsilon() );
			shipDoorMap.put( doorSprite.getCoordinate(), doorState );
		}

		// Crew.
		ArrayList<SavedGameParser.CrewState> crewList = shipState.getCrewList();
		crewList.clear();
		for ( CrewSprite crewSprite : crewSprites ) {
			SavedGameParser.CrewState crewState = new SavedGameParser.CrewState();

			crewState.setName( crewSprite.getName() );
			crewState.setRace( crewSprite.getRace() );
			crewState.setEnemyBoardingDrone( crewSprite.isEnemyBoardingDrone() );
			crewState.setHealth( crewSprite.getHealth() );
			crewState.setPlayerControlled( crewSprite.isPlayerControlled() );

			crewState.setUnknownAlpha( crewSprite.getUnknownAlpha() );
			crewState.setUnknownBeta( crewSprite.getUnknownBeta() );

			List<Integer> spriteTintIndeces = new ArrayList<Integer>( crewSprite.getSpriteTintIndeces().size() );
			for ( Integer colorIndex : crewSprite.getSpriteTintIndeces() ) {
				spriteTintIndeces.add( new Integer( colorIndex ) );
			}
			crewState.setSpriteTintIndeces( spriteTintIndeces );

			crewState.setMindControlled( crewSprite.isMindControlled() );
			crewState.setSavedRoomId( crewSprite.getSavedRoomId() );
			crewState.setSavedRoomSquare( crewSprite.getSavedRoomSquare() );

			crewState.setPilotSkill( crewSprite.getPilotSkill() );
			crewState.setEngineSkill( crewSprite.getEngineSkill() );
			crewState.setShieldSkill( crewSprite.getShieldSkill() );
			crewState.setWeaponSkill( crewSprite.getWeaponSkill() );
			crewState.setRepairSkill( crewSprite.getRepairSkill() );
			crewState.setCombatSkill( crewSprite.getCombatSkill() );

			crewState.setMale( crewSprite.isMale() );
			crewState.setRepairs( crewSprite.getRepairs() );
			crewState.setCombatKills( crewSprite.getCombatKills() );
			crewState.setPilotedEvasions( crewSprite.getPilotedEvasions() );
			crewState.setJumpsSurvived( crewSprite.getJumpsSurvived() );
			crewState.setSkillMasteries( crewSprite.getSkillMasteries() );

			crewState.setStunTicks( crewSprite.getStunTicks() );
			crewState.setHealthBoost( crewSprite.getHealthBoost() );
			crewState.setUnknownIota( crewSprite.getUnknownIota() );
			crewState.setUnknownKappa( crewSprite.getUnknownKappa() );
			crewState.setUnknownLambda( crewSprite.getUnknownLambda() );
			crewState.setUnknownMu( crewSprite.getUnknownMu() );
			crewState.setUnknownNu( crewSprite.getUnknownNu() );
			crewState.setUnknownXi( crewSprite.getUnknownXi() );
			crewState.setUnknownOmicron( crewSprite.getUnknownOmicron() );
			crewState.setTeleportAnimFrame( crewSprite.getTeleportAnimFrame() );
			crewState.setUnknownRho( crewSprite.getUnknownRho() );
			crewState.setUnknownSigma( crewSprite.getUnknownSigma() );
			crewState.setUnknownTau( crewSprite.getUnknownTau() );
			crewState.setUnknownUpsilon( crewSprite.getUnknownUpsilon() );
			crewState.setUnknownPhi( crewSprite.getUnknownPhi() );
			crewState.setLockdownRechargeTicks( crewSprite.getLockdownRechargeTicks() );
			crewState.setLockdownRechargeTicksGoal( crewSprite.getLockdownRechargeTicksGoal() );
			crewState.setUnknownOmega( crewSprite.getUnknownOmega() );

			crewState.setRoomId( crewSprite.getRoomId() );
			crewState.setRoomSquare( crewSprite.getSquareId() );
			crewState.setSpriteX( crewSprite.getSpriteX() );
			crewState.setSpriteY( crewSprite.getSpriteY() );

			crewList.add( crewState );
		}
	}

	private void selectRoom() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Select: Room";

			@Override
			public String getDescription() { return desc; }
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				RoomSprite roomSprite = roomSprites.get( roomId );
				showRoomEditor( roomSprite, squareId );
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectSystem() {
		if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
			JOptionPane.showMessageDialog( frame, "System editing is not possible yet for Advanced Edition saved games.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
			return;
		}

		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Select: System";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				RoomSprite roomSprite = roomSprites.get( roomId );

				// See if this room has a system in it, literally.
				for ( SystemSprite systemSprite : systemSprites ) {
					if ( roomSprite.getBounds().contains( systemSprite.getBounds() ) ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				RoomSprite roomSprite = roomSprites.get( roomId );

				for ( SystemSprite systemSprite : systemSprites ) {
					if ( roomSprite.getBounds().contains( systemSprite.getBounds() ) ) {
						showSystemEditor( systemSprite );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectCrew() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Select: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for ( CrewSprite crewSprite : crewSprites ) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for ( CrewSprite crewSprite : crewSprites ) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						showCrewEditor( crewSprite );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);

		showCrewRoster();  // A list of all sprites, including dead crew.
	}

	private void selectBreach() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Select: Breach";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for ( BreachSprite breachSprite : breachSprites ) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for ( BreachSprite breachSprite : breachSprites ) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						showBreachEditor( breachSprite );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void selectFire() {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Select: Fire";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				for ( FireSprite fireSprite : fireSprites ) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				for ( FireSprite fireSprite : fireSprites ) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						showFireEditor( fireSprite );
						break;
					}
				}
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addCrew() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Add: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for ( CrewSprite crewSprite : crewSprites ) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				SavedGameParser.CrewState crewState = new SavedGameParser.CrewState();
				crewState.setHealth( CrewType.getMaxHealth(crewState.getRace()) );
				crewState.setPlayerControlled( true );
				crewState.setRoomId( roomId );
				crewState.setRoomSquare( squareId );
				crewState.setSpriteX( center.x - originX - tileEdge + shipLayout.getOffsetX()*squareSize );
				crewState.setSpriteY( center.y - originY - tileEdge + shipLayout.getOffsetY()*squareSize );
				crewState.setSavedRoomId( roomId );
				crewState.setSavedRoomSquare( squareId );
				crewState.setMale( DataManager.get().getCrewSex() );
				crewState.setName( DataManager.get().getCrewName(crewState.isMale()) );
				addCrewSprite( center.x, center.y, crewState );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addBreach() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Add: Breach";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for ( BreachSprite breachSprite : breachSprites ) {
					if ( breachSprite.getRoomId() == roomId && breachSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				addBreachSprite( center.x, center.y, roomId, squareId, 100 );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void addFire() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Add: Fire";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for ( FireSprite fireSprite : fireSprites ) {
					if ( fireSprite.getRoomId() == roomId && fireSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				addFireSprite( center.x, center.y, roomId, squareId, 100 );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible(true);
	}

	private void moveCrew( final CrewSprite mobileSprite ) {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Move: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for ( CrewSprite crewSprite : crewSprites ) {
					if ( crewSprite.getRoomId() == roomId && crewSprite.getSquareId() == squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId ) {
				Point center = squareSelector.getSquareCenter();
				placeSprite( center.x, center.y, mobileSprite );
				mobileSprite.setRoomId( roomId );
				mobileSprite.setSquareId( squareId );
				mobileSprite.setSpriteX( center.x - originX - tileEdge + shipLayout.getOffsetX()*squareSize );
				mobileSprite.setSpriteY( center.y - originY - tileEdge + shipLayout.getOffsetY()*squareSize );
				return false;
			}
		});
		shipViewport.setStatusString( squareSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
		squareSelector.setVisible(true);
	}

	/**
	 * Gets a cropped area of an image and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with
	 * the expected dimensions.
	 */
	private BufferedImage getCroppedImage( String innerPath, int x, int y, int w, int h ) {
		Rectangle keyRect = new Rectangle( x, y, w, h );
		BufferedImage result = null;
		HashMap<Rectangle, BufferedImage> cacheMap = cachedImages.get(innerPath);
		if ( cacheMap != null ) result = cacheMap.get(keyRect);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and cropping...: "+ innerPath );

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage bigImage = ImageIO.read(in);
			result = bigImage.getSubimage(x, y, w, h);

		}
		catch ( RasterFormatException e ) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		}
		catch ( FileNotFoundException e ) {
			log.error( String.format("Failed to load and crop image (\"%s\"). Its innerPath was not found.", innerPath) );
		}
		catch ( IOException e ) {
			log.error( "Failed to load and crop image: "+ innerPath, e );
		}
		finally {
			try {if (in != null) in.close();}
			catch ( IOException e ) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = (Graphics2D)result.createGraphics();
			g2d.setColor( new Color(150, 150, 200) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cacheMap == null ) {
			cacheMap = new HashMap<Rectangle, BufferedImage>();
			cachedImages.put( innerPath, cacheMap );
		}
		cacheMap.put( keyRect, result );

		return result;
	}

	/**
	 * Gets an image, scaling if necessary, and caches the result.
	 *
	 * If something goes wrong, a dummy image will be created with
	 * the expected dimensions.
	 *
	 * If the dimensions are negative, the original unscaled image
	 * will be returned if possible, or the absolute values will be
	 * used for the dummy image.
	 */
	private BufferedImage getScaledImage( String innerPath, int w, int h ) {
		Rectangle keyRect = new Rectangle( 0, 0, w, h );
		BufferedImage result = null;
		HashMap<Rectangle, BufferedImage> cacheMap = cachedImages.get(innerPath);
		if ( cacheMap != null ) result = cacheMap.get(keyRect);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and scaling...: "+ innerPath );


		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage origImage = ImageIO.read(in);

			if ( w <= 0 || h <= 0 || (origImage.getWidth() == w && origImage.getHeight() == h) ) {
				result = origImage;
			} else {
				BufferedImage scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = scaledImage.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.drawImage(origImage, 0, 0, w, h, null);
				g2d.dispose();
				result = scaledImage;
			}
		}
		catch ( RasterFormatException e ) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		}
		catch ( IOException e ) {
			log.error( "Failed to load and scale image: "+ innerPath, e );
		}
		finally {
			try {if (in != null) in.close();}
			catch ( IOException e ) {}
		}

		if ( result == null ) {  // Guarantee a returned image, with a stand-in.
			w = Math.abs(w);
			h = Math.abs(h);
			result = gc.createCompatibleImage( w, h, Transparency.OPAQUE );
			Graphics2D g2d = (Graphics2D)result.createGraphics();
			g2d.setColor( new Color(150, 150, 200) );
			g2d.fillRect( 0, 0, w-1, h-1 );
			g2d.dispose();
		}

		if ( cacheMap == null ) {
			cacheMap = new HashMap<Rectangle, BufferedImage>();
			cachedImages.put( innerPath, cacheMap );
		}
		cacheMap.put( keyRect, result );

		return result;
	}

	/**
	 * Applies a RescaleOp to the palette of an image, and caches the result.
	 */
	private BufferedImage getTintedImage( BufferedImage srcImage, Tint tint ) {
		BufferedImage result = null;
		HashMap<Tint, BufferedImage> cacheMap = cachedTintedImages.get( srcImage );
		if ( cacheMap != null ) result = cacheMap.get( tint );
		if ( result != null ) return result;

		BufferedImage canvas = gc.createCompatibleImage(srcImage.getWidth(), srcImage.getHeight(), Transparency.TRANSLUCENT);
		Graphics2D g2d = canvas.createGraphics();
		g2d.drawImage(srcImage, 0, 0, null);
		g2d.dispose();
		RescaleOp op = new RescaleOp( tint.scaleFactors, tint.offsets, null);
		result = op.filter(canvas, null);

		if ( cacheMap == null ) {
			cacheMap = new HashMap<Tint, BufferedImage>();
			cachedTintedImages.put( srcImage, cacheMap );
		}
		cacheMap.put( tint, result );

		return result;
	}

	private BufferedImage getBodyImage( String imgRace, boolean playerControlled ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String suffix = "";
		String innerPath;
		BufferedImage result = null;

		if ( playerControlled ) {
			result = cachedPlayerBodyImages.get( imgRace );
		} else {
			result = cachedEnemyBodyImages.get( imgRace );
		}
		if ( result != null ) return result;

		// As of 1.01, drone images were "X_sheet" and "X_enemy_sheet".
		// As of 1.01, crew images were "X_player_[green|yellow]" and "X_enemy_red".
		// As of 1.03.1, drone images could also be "X_player[no color]" vs "X_enemy_red".
		// As of 1.5.4, crew images were "X_base" overlaid on a tinted "X_color".
		// As of 1.5.4, drone images were "X_base" with no color possibility.

		if ( CrewType.BATTLE.getId().equals(imgRace) ) {
			suffix = "_sheet";

			// All "battle" bodies on a ship are foreign, and unselectable in-game.
			// Hence, no color?
		}
		else if ( DroneType.REPAIR.getId().equals(imgRace) ) {
			suffix = "_sheet";

			// All "repair" bodies on a ship are local, and unselectable in-game.
			// Hence, no color?
		}
		else {
			if ( playerControlled ) {
				suffix = "_player_yellow";
			} else {
				suffix = "_enemy_red";
			}
		}

		innerPath = "img/people/"+ imgRace + suffix +".png";
		if ( DataManager.get().hasResourceInputStream( innerPath ) ) {
			// FTL 1.01-1.03.3
			result = getCroppedImage( innerPath, offsetX, offsetY, w, h );
		}
		else {
			// FTL 1.5.4+
			BufferedImage colorImage = null;
			BufferedImage baseImage = null;

			String colorPath = "img/people/"+ imgRace +"_color.png";
			if ( DataManager.get().hasResourceInputStream( colorPath ) ) {
				colorImage = getCroppedImage( colorPath, offsetX, offsetY, w, h );
				float[] yellow = new float[] { 0.957f, 0.859f, 0.184f, 1f };
				float[] red = new float[] { 1.0f, 0.286f, 0.145f, 1f };
				Tint colorTint = new Tint( (playerControlled ? yellow: red), new float[] { 0, 0, 0, 0 } );
				colorImage = getTintedImage( colorImage, colorTint );
			}

			String basePath = "img/people/"+ imgRace +"_base.png";
			if ( DataManager.get().hasResourceInputStream( basePath ) ) {
				baseImage = getCroppedImage( basePath, offsetX, offsetY, w, h );
			}

			if ( colorImage != null && baseImage != null ) {
				result = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
				Graphics2D g2d = result.createGraphics();
				g2d.drawImage(colorImage, 0, 0, null);
				g2d.drawImage(baseImage, 0, 0, null);
				g2d.dispose();
			}
			else if ( baseImage != null ) {
				// No colorImage to tint and outline the sprite, probably a drone.
				result = baseImage;
			}
		}

		if ( result != null ) {
			if ( playerControlled ) {
				cachedPlayerBodyImages.put( imgRace, result );
			} else {
				cachedEnemyBodyImages.put( imgRace, result );
			}
		}

		return result;
	}

	private void addDroneSprite( int centerX, int centerY, int slot, SavedGameParser.DroneState droneState ) {
		DroneSprite droneSprite = new DroneSprite( slot, droneState );

		droneSprite.setLocation( centerX - droneSprite.getWidth()/2, centerY - droneSprite.getHeight()/2 );
		droneSprites.add( droneSprite );
		shipPanel.add( droneSprite, DRONE_LAYER );
		shipPanel.add( droneSprite.getBody(), DRONE_LAYER );
	}

	private void addWeaponSprite( int slot, ShipChassis.WeaponMountList.WeaponMount weaponMount, SavedGameParser.WeaponState weaponState ) {
		WeaponSprite weaponSprite = new WeaponSprite( weaponMount.rotate, slot, weaponState );

		if ( weaponMount.rotate ) {
			// Right of x,y and centered vertically.
			weaponSprite.setLocation( weaponMount.x, weaponMount.y - weaponSprite.getHeight()/2 );
		} else {
			// Above x,y and centered horizontally.
			weaponSprite.setLocation( weaponMount.x - weaponSprite.getWidth()/2, weaponMount.y - weaponSprite.getHeight() );
		}
		weaponSprites.add( weaponSprite );
		shipPanel.add( weaponSprite, WEAPON_LAYER );
	}

	private void addDoorSprite( int centerX, int centerY, int level, ShipLayout.DoorCoordinate doorCoord, SavedGameParser.DoorState doorState ) {
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		int levelCount = 3;

		// Dom't scale the image, but pass negative size to define the fallback dummy image.
		BufferedImage bigImage = getScaledImage( "img/effects/door_sheet.png", -1*((offsetX+4*w)+w), -1*((offsetY+(levelCount-1)*h)+h) );

		BufferedImage[] closedImages = new BufferedImage[levelCount];
		BufferedImage[] openImages = new BufferedImage[levelCount];

		for (int i=0; i < levelCount; i++) {
			int chop = 10;  // Chop 10 pixels off the sides for skinny doors.
			closedImages[i] = bigImage.getSubimage(offsetX+chop, offsetY+i*h, w-chop*2, h);
			openImages[i] = bigImage.getSubimage(offsetX+4*w+chop, offsetY+i*h, w-chop*2, h);
		}

		DoorSprite doorSprite = new DoorSprite( closedImages, openImages, level, doorCoord, doorState );
		if ( doorCoord.v == 1 )
			doorSprite.setSize( closedImages[level].getWidth(), closedImages[level].getHeight() );
		else
			doorSprite.setSize( closedImages[level].getHeight(), closedImages[level].getWidth() );

		placeSprite( centerX, centerY, doorSprite );
		doorSprites.add( doorSprite );
		shipPanel.add( doorSprite, DOOR_LAYER );
	}

	private void addSystemSprite( int centerX, int centerY, SavedGameParser.SystemState systemState ) {
		int w = 32, h = 32;

		// Assuming these are interchangeable.
		String overlayBaseName = systemState.getSystemType().getId();
		BufferedImage overlayImage = getScaledImage( "img/icons/s_"+ overlayBaseName +"_overlay.png", w, h );

		SystemSprite systemSprite = new SystemSprite( overlayImage, systemState );
		systemSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		systemSprites.add( systemSprite );
		shipPanel.add( systemSprite, SYSTEM_LAYER );
	}

	private void addBreachSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 19, h = 19;

		BufferedImage breachImage = getCroppedImage( "img/effects/breach.png", offsetX+6*w, offsetY, w, h );

		BreachSprite breachSprite = new BreachSprite( breachImage, roomId, squareId, health );
		breachSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		breachSprites.add( breachSprite );
		shipPanel.add( breachSprite, BREACH_LAYER );
	}

	private void addFireSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 32, h = 32;

		BufferedImage fireImage = getCroppedImage( "img/effects/fire_L1_strip8.png", offsetX, offsetY, w, h );

		FireSprite fireSprite = new FireSprite( fireImage, roomId, squareId, health );
		fireSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		fireSprites.add( fireSprite );
		shipPanel.add( fireSprite, FIRE_LAYER );
	}

	private void addCrewSprite( int centerX, int centerY, SavedGameParser.CrewState crewState ) {
		CrewSprite crewSprite = new CrewSprite( crewState );
		int w = crewSprite.getImageWidth();
		int h = crewSprite.getImageHeight();
		crewSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		crewSprites.add( crewSprite );
		shipPanel.add( crewSprite, CREW_LAYER );
	}

	/** Relocates a JComponent within its parent's null layout. */
	private void placeSprite( int centerX, int centerY, JComponent sprite ) {
		Dimension spriteSize = sprite.getSize();
		sprite.setLocation( centerX-spriteSize.width/2, centerY-spriteSize.height/2 );
	}

	/** Draws each room's walls, door openings, and floor crevices. */
	private void drawWalls( Graphics2D wallG, int originX, int originY, SavedGameParser.ShipState shipState, ShipLayout shipLayout ) {

		Color prevColor = wallG.getColor();
		Stroke prevStroke = wallG.getStroke();
		Color floorCrackColor = new Color(125, 125, 125);
		Stroke floorCrackStroke = new BasicStroke(1);
		Color roomBorderColor = new Color(15, 15, 15);
		Stroke roomBorderStroke = new BasicStroke(4);
		SavedGameParser.DoorState doorState = null;
		int fromX, fromY, toX, toY;

		for (int i=0; i < shipLayout.getRoomCount(); i++) {
			EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo(i);
			int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
			int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
			int roomX = originX + roomLocX*squareSize;
			int roomY = originY + roomLocY*squareSize;
			int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
			int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

			// Draw floor lines within rooms.
			wallG.setColor( floorCrackColor );
			wallG.setStroke( floorCrackStroke );
			for (int n=1; n <= squaresV-1; n++)  // H lines.
				wallG.drawLine( roomX+1, roomY+n*squareSize, roomX+squaresH*squareSize-1, roomY+n*squareSize );
			for (int n=1; n <= squaresH-1; n++)  // V lines.
				wallG.drawLine( roomX+n*squareSize, roomY+1, roomX+n*squareSize, roomY+squaresV*squareSize-1 );

			// Draw borders around rooms.
			for (int n=1; n <= squaresV; n++) {  // V lines.
				// West side.
				fromX = roomX;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX;
				toY = roomY+n*squareSize;
				doorState = shipState.getDoor(roomLocX, roomLocY+n-1, 1);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, fromY+(toY-fromY)/8 );
				  wallG.drawLine( fromX, fromY+(toY-fromY)/8*7, toX, toY );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}

				// East Side.
				fromX = roomX+squaresH*squareSize;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX+squaresH*squareSize;
				toY = roomY+n*squareSize;
				doorState = shipState.getDoor(roomLocX+squaresH, roomLocY+n-1, 1);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, fromY+(toY-fromY)/8 );
				  wallG.drawLine( fromX, fromY+(toY-fromY)/8*7, toX, toY );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}
			}

			wallG.setStroke( roomBorderStroke );
			wallG.setColor( roomBorderColor );
			for (int n=1; n <= squaresH; n++) {  // H lines.
				// North side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY;
				toX = roomX+n*squareSize;
				toY = roomY;
				doorState = shipState.getDoor(roomLocX+n-1, roomLocY, 0);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, fromX+(toX-fromX)/8, fromY );
				  wallG.drawLine( fromX+(toX-fromX)/8*7, fromY, toX, toY );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}

				// South side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY+squaresV*squareSize;
				toX = roomX+n*squareSize;
				toY = roomY+squaresV*squareSize;
				doorState = shipState.getDoor(roomLocX+n-1, roomLocY+squaresV, 0);
				if ( doorState != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, fromX+(toX-fromX)/8, fromY );
				  wallG.drawLine( fromX+(toX-fromX)/8*7, fromY, toX, toY );
				} else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
				  wallG.drawLine( fromX, fromY, toX, toY );
				}
			}
		}
		wallG.setColor( prevColor );
		wallG.setStroke( prevStroke );
	}

	private void showSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension(sideWidth + vbarWidth, 1) );
		sideScroll.setVisible( true );

		this.revalidate();
		this.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				sideScroll.getVerticalScrollBar().setValue(0);
			}
		});
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
	 */
	private void createSidePanel( String title, final FieldEditorPanel editorPanel, final Runnable applyCallback ) {
		clearSidePanel();
		JLabel titleLbl = new JLabel(title);
		titleLbl.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( titleLbl );
		addSidePanelSeparator(4);

		// Keep the editor from growing and creating gaps around it.
		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		sidePanel.add( editorPanel );

		sidePanel.add( Box.createVerticalStrut(10) );

		JPanel applyPanel = new JPanel();
		applyPanel.setLayout( new BoxLayout(applyPanel, BoxLayout.X_AXIS) );
		JButton cancelBtn = new JButton("Cancel");
		applyPanel.add( cancelBtn );
		applyPanel.add( Box.createRigidArea( new Dimension(15, 1)) );
		JButton applyBtn = new JButton("Apply");
		applyPanel.add( applyBtn );
		sidePanel.add( applyPanel );

		cancelBtn.addActionListener(new ActionListener() {
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
		JSeparator newSep = new JSeparator(JSeparator.HORIZONTAL);
		newSep.setMaximumSize( new Dimension(Short.MAX_VALUE, newSep.getPreferredSize().height) );
		sidePanel.add( newSep );
		sidePanel.add( Box.createVerticalStrut( spacerSize ) );
	}

	/**
	 * Adds a wrapped label the side panel.
	 */
	private void addSidePanelNote( String s ) {
		JTextArea labelArea = new JTextArea( s );
		labelArea.setBackground(null);
		labelArea.setEditable(false);
		labelArea.setBorder(null);
		labelArea.setLineWrap(true);
		labelArea.setWrapStyleWord(true);
		labelArea.setFocusable(false);
		sidePanel.add( labelArea );
	}

	private void showGeneralEditor() {
		final String SHIP_NAME="Ship Name";
		final String HULL="Hull";
		final String FUEL="Fuel";
		final String DRONE_PARTS="Drone Parts";
		final String MISSILES="Missiles";
		final String SCRAP="Scrap";

		String title = "General";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( SHIP_NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.getString(SHIP_NAME).setText( shipName );
		editorPanel.addRow( HULL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HULL).setMaximum( shipBlueprint.getHealth().amount );
		editorPanel.getSlider(HULL).setValue( shipHull );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(FUEL).setText( ""+shipFuel );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(DRONE_PARTS).setText( ""+shipDroneParts );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(MISSILES).setText( ""+shipMissiles );
		editorPanel.addRow( SCRAP, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SCRAP).setText( ""+shipScrap );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String newString;
				newString = editorPanel.getString(SHIP_NAME).getText();
				if ( newString.length() > 0 ) shipName = newString;

				shipHull = editorPanel.getSlider(HULL).getValue();

				newString = editorPanel.getInt(FUEL).getText();
				try { shipFuel = Integer.parseInt(newString); }
				catch ( NumberFormatException e ) {}

				newString = editorPanel.getInt(DRONE_PARTS).getText();
				try { shipDroneParts = Integer.parseInt(newString); }
				catch ( NumberFormatException e ) {}

				newString = editorPanel.getInt(MISSILES).getText();
				try { shipMissiles = Integer.parseInt(newString); }
				catch ( NumberFormatException e ) {}

				newString = editorPanel.getInt(SCRAP).getText();
				try { shipScrap = Integer.parseInt(newString); }
				catch ( NumberFormatException e ) {}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showAugmentsEditor( final List<String> shipAugmentIdList ) {
		final String DESC = "Desc";
		final String ID_ONE = "#1";
		final String ID_TWO = "#2";
		final String ID_THREE = "#3";
		final String[] augSlots = new String[] { ID_ONE, ID_TWO, ID_THREE };

		final Map<String, AugBlueprint> allAugmentsMap = DataManager.get().getAugments();

		String title = "Augments";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( DESC, FieldEditorPanel.ContentType.WRAPPED_LABEL );
		editorPanel.getWrappedLabel(DESC).setRows(12);  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel(DESC).setMinimumSize( new Dimension(0, editorPanel.getWrappedLabel(DESC).getPreferredSize().height) );
		editorPanel.addBlankRow();

		for (int i=0; i < augSlots.length; i++) {
			editorPanel.addRow( augSlots[i], FieldEditorPanel.ContentType.COMBO );

			editorPanel.getCombo(augSlots[i]).addItem("");
			for ( AugBlueprint augBlueprint : allAugmentsMap.values() ) {
				editorPanel.getCombo(augSlots[i]).addItem( augBlueprint );
			}
			if ( shipAugmentIdList.size() > i)
				editorPanel.getCombo(augSlots[i]).setSelectedItem( allAugmentsMap.get(shipAugmentIdList.get(i)) );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				shipAugmentIdList.clear();

				for (int i=0; i < augSlots.length; i++) {
					Object augObj = editorPanel.getCombo(augSlots[i]).getSelectedItem();
					if ( augObj instanceof AugBlueprint )
						shipAugmentIdList.add( ((AugBlueprint)augObj).getId() );
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener augListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				for (int i=0; i < augSlots.length; i++) {
					JComboBox augCombo = editorPanel.getCombo(augSlots[i]);
					if ( source == augCombo ) {
						Object augObj = augCombo.getSelectedItem();
						if ( augObj instanceof AugBlueprint ) {
							editorPanel.getWrappedLabel(DESC).setText( ((AugBlueprint)augObj).getDescription() );

							if ( ((AugBlueprint)augObj).isStackable() == false ) {
								// Clear other slots' copies of this unique augment.
								for (int j=0; j < augSlots.length; j++) {
									if ( j == i ) continue;
									if ( editorPanel.getCombo(augSlots[j]).getSelectedItem() == augObj )
										editorPanel.getCombo(augSlots[j]).setSelectedItem("");
								}
							}
						}
						break;
					}
				}
			}
		};
		for (int i=0; i < augSlots.length; i++) {
			editorPanel.getCombo(augSlots[i]).addActionListener( augListener );
		}

		showSidePanel();
	}

	private void showDroneEditor( final DroneSprite droneSprite ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "DroneId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String HEALTH = "Health";
		final String PLAYER_CONTROLLED = "Player Ctrl";

		final Map<String, DroneBlueprint> allDronesMap = DataManager.get().getDrones();

		int droneSystemCapacity = 0;
		int a = 0, b = 0;  // a=other systems' power usage, b=this system's power and damage.
		for ( SystemSprite otherSystemSprite : systemSprites ) {
			if ( SystemType.DRONE_CTRL.equals( otherSystemSprite.getSystemType() ) ) {
				droneSystemCapacity = otherSystemSprite.getCapacity();
				b += otherSystemSprite.getDamagedBars();
				for ( DroneSprite otherDroneSprite : droneSprites ) {
					if ( otherDroneSprite != droneSprite && otherDroneSprite.getDroneId() != null && otherDroneSprite.isArmed() )
						b += allDronesMap.get( otherDroneSprite.getDroneId() ).getPower();
				}
			}
			else if ( SystemType.WEAPONS.equals( otherSystemSprite.getSystemType() ) ) {
				for ( WeaponSprite otherWeaponSprite : weaponSprites ) {
					if ( otherWeaponSprite.getWeaponId() != null && otherWeaponSprite.isArmed() )
						a += DataManager.get().getWeapon( otherWeaponSprite.getWeaponId() ).getPower();
				}
			}
			else if ( !otherSystemSprite.getSystemType().isSubsystem() ) {
				a += otherSystemSprite.getPower();
			}
		}
		final int availablePower = Math.min( shipReservePowerCapacity - a, droneSystemCapacity ) - b;
		if ( droneSystemCapacity == 0 ) {
			frame.setStatusText( "A drone system must be present with capacity > 0 before adding drones." );
			return;
		}

		String title = String.format("Drone %d", droneSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(AVAILABLE_POWER).setMaximum( shipReservePowerCapacity );
		editorPanel.getSlider(AVAILABLE_POWER).setValue( availablePower );
		editorPanel.getSlider(AVAILABLE_POWER).setEnabled( false );
		editorPanel.addBlankRow();
		editorPanel.addRow( ID, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( DESC, FieldEditorPanel.ContentType.WRAPPED_LABEL );
		editorPanel.getWrappedLabel(DESC).setRows(8);  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel(DESC).setMinimumSize( new Dimension(0, editorPanel.getWrappedLabel(DESC).getPreferredSize().height) );
		editorPanel.addRow( POWER_REQ, FieldEditorPanel.ContentType.LABEL );
		editorPanel.addRow( ARMED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ARMED).setEnabled( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( 0 );
		editorPanel.addRow( PLAYER_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );

		editorPanel.getCombo(ID).addItem("");
		for ( DroneBlueprint droneBlueprint : allDronesMap.values() ) {
			editorPanel.getCombo(ID).addItem( droneBlueprint );
		}

		if ( droneSprite.getDroneId() != null ) {
			DroneBlueprint selectedBlueprint = allDronesMap.get( droneSprite.getDroneId() );
			boolean armable = (availablePower >= selectedBlueprint.getPower());

			editorPanel.getSlider(AVAILABLE_POWER).setValue( availablePower - (armable && droneSprite.isArmed() ? selectedBlueprint.getPower() : 0) );
			editorPanel.getCombo(ID).setSelectedItem( selectedBlueprint );
			editorPanel.getWrappedLabel(DESC).setText( selectedBlueprint.getDescription() );
			editorPanel.getLabel(POWER_REQ).setText( ""+selectedBlueprint.getPower() );
			editorPanel.getBoolean(ARMED).setEnabled( armable );
			editorPanel.getBoolean(PLAYER_CONTROLLED).setSelected( droneSprite.isPlayerControlled() );
			editorPanel.getSlider(HEALTH).setMaximum( DroneType.getMaxHealth( selectedBlueprint.getType() ) );
			editorPanel.getSlider(HEALTH).setValue( droneSprite.getHealth() );

			if ( armable && droneSprite.isArmed() ) {
				editorPanel.getBoolean(ARMED).setSelected( true );
			}

			editorPanel.revalidate();
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object blueprintObj = editorPanel.getCombo(ID).getSelectedItem();
				String droneId = null;
				if ( blueprintObj instanceof DroneBlueprint )
					droneId = ((DroneBlueprint)blueprintObj).getId();
				droneSprite.setDroneId( droneId );

				droneSprite.setArmed( editorPanel.getBoolean(ARMED).isSelected() );
				droneSprite.setPlayerControlled( editorPanel.getBoolean(PLAYER_CONTROLLED).isSelected() );
				droneSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );
				droneSprite.makeSane();

				// Set the Drone Ctrl system power based on all armed drones.
				int neededPower = 0;
				for ( DroneSprite otherSprite : droneSprites ) {
					if ( otherSprite.getDroneId() != null && otherSprite.isArmed() )
						neededPower += allDronesMap.get( otherSprite.getDroneId() ).getPower();
				}
				for ( SystemSprite otherSprite : systemSprites ) {
					if ( SystemType.DRONE_CTRL.equals( otherSprite.getSystemType() ) ) {
						otherSprite.setPower( neededPower );
						otherSprite.makeSane();
						break;
					}
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener droneListener = new ActionListener() {
			private JSlider availablePowerSlider = editorPanel.getSlider(AVAILABLE_POWER);
			private JComboBox idCombo = editorPanel.getCombo(ID);
			private JCheckBox armedCheck = editorPanel.getBoolean(ARMED);
			private JCheckBox playerControlledCheck = editorPanel.getBoolean(PLAYER_CONTROLLED);
			private JSlider healthSlider = editorPanel.getSlider(HEALTH);

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == idCombo ) {
					availablePowerSlider.setValue( availablePower );
					editorPanel.getWrappedLabel(DESC).setText("");
					editorPanel.getLabel(POWER_REQ).setText("");
					armedCheck.setSelected( false );
					armedCheck.setEnabled( false );
					healthSlider.setMaximum( 0 );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof DroneBlueprint ) {
						DroneBlueprint selectedBlueprint = (DroneBlueprint)blueprintObj;
						boolean armable = (availablePower >= selectedBlueprint.getPower());

						editorPanel.getWrappedLabel(DESC).setText( ""+selectedBlueprint.getDescription() );
						editorPanel.getLabel(POWER_REQ).setText( ""+selectedBlueprint.getPower() );
						healthSlider.setMaximum( DroneType.getMaxHealth( selectedBlueprint.getType() ) );
						healthSlider.setValue( DroneType.getMaxHealth( selectedBlueprint.getType() ) );

						if ( armable ) {
							armedCheck.setEnabled( true );
						}
					}
				}
				else if ( source == armedCheck ) {
					availablePowerSlider.setValue( availablePower );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof DroneBlueprint ) {
						DroneBlueprint selectedBlueprint = (DroneBlueprint)blueprintObj;

						if ( armedCheck.isSelected() ) {
							availablePowerSlider.setValue( availablePower - selectedBlueprint.getPower() );
						}
					}
				}
			}
		};
		editorPanel.getCombo(ID).addActionListener( droneListener );
		editorPanel.getBoolean(ARMED).addActionListener( droneListener );

		addSidePanelSeparator(8);
		String notice = "";
		notice += "* Available power is [Reserve power or Drone Ctrl system capacity], ";
		notice += "minus other armed drones' power, minus Drone Ctrl system damage.\n";

		notice += "* This tool doesn't alter nearby ships. It's unknown what will happen ";
		notice += "if a boarding drone's armed state is changed (what about the body?).\n";

		notice += "* Player Ctrl works as expected while armed, but has the opposite ";
		notice += "value when disarmed?";
		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showWeaponEditor( final WeaponSprite weaponSprite ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "WeaponId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String COOLDOWN_TICKS = "Cooldown Ticks";

		final Map<String, WeaponBlueprint> allWeaponsMap = DataManager.get().getWeapons();

		int weaponSystemCapacity = 0;
		int a = 0, b = 0;  // a=other systems' power usage, b=this system's power and damage.
		for ( SystemSprite otherSystemSprite : systemSprites ) {
			if ( SystemType.WEAPONS.equals( otherSystemSprite.getSystemType() ) ) {
				weaponSystemCapacity = otherSystemSprite.getCapacity();
				b += otherSystemSprite.getDamagedBars();
				for ( WeaponSprite otherWeaponSprite : weaponSprites ) {
					if ( otherWeaponSprite != weaponSprite && otherWeaponSprite.getWeaponId() != null && otherWeaponSprite.isArmed() )
						b += allWeaponsMap.get( otherWeaponSprite.getWeaponId() ).getPower();
				}
			}
			else if ( SystemType.DRONE_CTRL.equals( otherSystemSprite.getSystemType() ) ) {
				for ( DroneSprite otherDroneSprite : droneSprites ) {
					if ( otherDroneSprite.getDroneId() != null && otherDroneSprite.isArmed() )
						a += DataManager.get().getDrone( otherDroneSprite.getDroneId() ).getPower();
				}
			}
			else if ( !otherSystemSprite.getSystemType().isSubsystem() ) {
				a += otherSystemSprite.getPower();
			}
		}
		final int availablePower = Math.min( shipReservePowerCapacity - a, weaponSystemCapacity ) - b;
		if ( weaponSystemCapacity == 0 ) {
			frame.setStatusText( "A weapon system must be present with capacity > 0 before adding weapons." );
			return;
		}

		String title = String.format("Weapon %d", weaponSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(AVAILABLE_POWER).setMaximum( shipReservePowerCapacity );
		editorPanel.getSlider(AVAILABLE_POWER).setValue( availablePower );
		editorPanel.getSlider(AVAILABLE_POWER).setEnabled( false );
		editorPanel.addBlankRow();
		editorPanel.addRow( ID, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( DESC, FieldEditorPanel.ContentType.WRAPPED_LABEL );
		editorPanel.getWrappedLabel(DESC).setRows(8);  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel(DESC).setMinimumSize( new Dimension(0, editorPanel.getWrappedLabel(DESC).getPreferredSize().height) );
		editorPanel.addRow( POWER_REQ, FieldEditorPanel.ContentType.LABEL );
		editorPanel.addRow( ARMED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ARMED).setEnabled( false );
		editorPanel.addRow( COOLDOWN_TICKS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(COOLDOWN_TICKS).setMaximum( 0 );
		editorPanel.getSlider(COOLDOWN_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Seconds spent cooling down.") );

		editorPanel.getCombo(ID).addItem("");
		for ( WeaponBlueprint weaponBlueprint : allWeaponsMap.values() )
			editorPanel.getCombo(ID).addItem(weaponBlueprint);

		if ( weaponSprite.getWeaponId() != null ) {
			WeaponBlueprint selectedBlueprint = allWeaponsMap.get( weaponSprite.getWeaponId() );
			boolean armable = (availablePower >= selectedBlueprint.getPower());

			editorPanel.getSlider(AVAILABLE_POWER).setValue( availablePower - (weaponSprite.isArmed() ? selectedBlueprint.getPower() : 0) );
			editorPanel.getCombo(ID).setSelectedItem( selectedBlueprint );
			editorPanel.getWrappedLabel(DESC).setText( selectedBlueprint.getTooltip() );
			editorPanel.getLabel(POWER_REQ).setText( ""+selectedBlueprint.getPower() );
			editorPanel.getBoolean(ARMED).setSelected( (weaponSprite.isArmed() && availablePower >= selectedBlueprint.getPower()) );
			editorPanel.getBoolean(ARMED).setEnabled( (availablePower >= selectedBlueprint.getPower()) );
			editorPanel.getSlider(COOLDOWN_TICKS).setMaximum( selectedBlueprint.getCooldown() );
			editorPanel.getSlider(COOLDOWN_TICKS).setValue( weaponSprite.getCooldownTicks() );

			editorPanel.revalidate();
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object blueprintObj = editorPanel.getCombo(ID).getSelectedItem();
				String weaponId = null;
				if ( blueprintObj instanceof WeaponBlueprint )
					weaponId = ((WeaponBlueprint)blueprintObj).getId();
				weaponSprite.setWeaponId( weaponId );

				weaponSprite.setArmed( editorPanel.getBoolean(ARMED).isSelected() );
				weaponSprite.setCooldownTicks( editorPanel.getSlider(COOLDOWN_TICKS).getValue() );

				// Set the Weapons system power based on all armed weapons.
				int neededPower = 0;
				for ( WeaponSprite otherSprite : weaponSprites ) {
					if ( otherSprite.getWeaponId() != null && otherSprite.isArmed() )
						neededPower += allWeaponsMap.get( otherSprite.getWeaponId() ).getPower();
				}
				for ( SystemSprite otherSprite : systemSprites ) {
					if ( SystemType.WEAPONS.equals( otherSprite.getSystemType() ) ) {
						otherSprite.setPower( neededPower );
						otherSprite.makeSane();
						break;
					}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener weaponListener = new ActionListener() {
			private JSlider availablePowerSlider = editorPanel.getSlider(AVAILABLE_POWER);
			private JComboBox idCombo = editorPanel.getCombo(ID);
			private JCheckBox armedCheck = editorPanel.getBoolean(ARMED);
			private JSlider cooldownSlider = editorPanel.getSlider(COOLDOWN_TICKS);

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == idCombo ) {
					availablePowerSlider.setValue( availablePower );
					editorPanel.getWrappedLabel(DESC).setText("");
					editorPanel.getLabel(POWER_REQ).setText("");
					armedCheck.setSelected( false );
					armedCheck.setEnabled( false );
					cooldownSlider.setMaximum( 0 );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof WeaponBlueprint ) {
						WeaponBlueprint selectedBlueprint = (WeaponBlueprint)blueprintObj;
						boolean armable = (availablePower >= selectedBlueprint.getPower());

						editorPanel.getWrappedLabel(DESC).setText( selectedBlueprint.getTooltip() );
						editorPanel.getLabel(POWER_REQ).setText( ""+selectedBlueprint.getPower() );
						if ( armable ) {
							armedCheck.setEnabled( true );
						}
					}
					editorPanel.revalidate();
				}
				else if ( source == armedCheck ) {
					availablePowerSlider.setValue( availablePower );
					cooldownSlider.setMaximum( 0 );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof WeaponBlueprint ) {
						WeaponBlueprint selectedBlueprint = (WeaponBlueprint)blueprintObj;
						boolean armed = armedCheck.isSelected();

						if ( armed ) {
							availablePowerSlider.setValue( availablePower - selectedBlueprint.getPower() );
							cooldownSlider.setMaximum( selectedBlueprint.getCooldown() );
						}
					}
				}
			}
		};
		editorPanel.getCombo(ID).addActionListener( weaponListener );
		editorPanel.getBoolean(ARMED).addActionListener( weaponListener );

		addSidePanelSeparator(8);
		String notice = "";
		notice += "* Available power is [Reserve power or Weapons system capacity], ";
		notice += "minus other armed weapons' power, minus Weapons system damage.";
		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showRoomEditor( final RoomSprite roomSprite, final int squareId ) {
		final String OXYGEN = "Oxygen";
		final String STATION_HERE = "Station Here";
		final String STATION_DIR = "Station Direction";
		final String IGNITION = "Ignition Progress";
		final String GAMMA = "Gamma?";

		int roomId = roomSprite.getRoomId();
		String title = String.format("Room %2d (Square %d)", roomId, squareId);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OXYGEN, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(OXYGEN).setMaximum( 100 );
		editorPanel.getSlider(OXYGEN).addMouseListener( new StatusbarMouseListener(frame, "Oxygen level for the room as a whole.") );
		editorPanel.addBlankRow();
		editorPanel.addRow( STATION_HERE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(STATION_HERE).addMouseListener( new StatusbarMouseListener(frame, "Toggles whether this square has a station for manning a system.") );
		editorPanel.addRow( STATION_DIR, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo(STATION_DIR).addMouseListener( new StatusbarMouseListener(frame, "Placement of the station on the square (DOWN means on the bottom edge).") );
		editorPanel.addBlankRow();
		editorPanel.addRow( IGNITION, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(IGNITION).setMaximum( 100 );
		editorPanel.getSlider(IGNITION).addMouseListener( new StatusbarMouseListener(frame, "A new fire spawns in this square at 100.") );
		editorPanel.addRow( GAMMA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(GAMMA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.getInt(GAMMA).addMouseListener( new StatusbarMouseListener(frame, "Extinguishment Progress? (Bug: FTL 1.01-1.5.10 always had -1.)") );

		editorPanel.getCombo(STATION_DIR).addItem( StationDirection.DOWN );
		editorPanel.getCombo(STATION_DIR).addItem( StationDirection.RIGHT );
		editorPanel.getCombo(STATION_DIR).addItem( StationDirection.UP );
		editorPanel.getCombo(STATION_DIR).addItem( StationDirection.LEFT );
		// NONE is omitted here, since the combo's disabled when there's no station.

		editorPanel.getSlider(OXYGEN).setValue( roomSprite.getOxygen() );

		editorPanel.getBoolean(STATION_HERE).setSelected( (squareId == roomSprite.getStationSquare()) );
		if ( (squareId == roomSprite.getStationSquare()) ) {
			editorPanel.getCombo(STATION_DIR).setSelectedItem( roomSprite.getStationDirection() );
		}

		editorPanel.getSlider(IGNITION).setValue( roomSprite.getSquare(squareId).getIgnitionProgress() );
		editorPanel.getInt(GAMMA).setText( ""+roomSprite.getSquare(squareId).getUnknownGamma() );

		editorPanel.getBoolean(STATION_HERE).addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean stationHere = ( e.getStateChange() == ItemEvent.SELECTED );
				editorPanel.getCombo(STATION_DIR).setEnabled( stationHere );
			}
		});
		editorPanel.getCombo(STATION_DIR).setEnabled( editorPanel.getBoolean(STATION_HERE).isSelected() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				roomSprite.setOxygen( editorPanel.getSlider(OXYGEN).getValue() );

				boolean stationHere = editorPanel.getBoolean(STATION_HERE).isSelected();
				if ( !stationHere && roomSprite.getStationSquare() == squareId) {
					roomSprite.setStationSquare( -1 );
					roomSprite.setStationDirection( StationDirection.NONE );
				}
				else if ( stationHere ) {  // Square and/or dir may have changed.
					roomSprite.setStationSquare( squareId );
					roomSprite.setStationDirection( (StationDirection)editorPanel.getCombo(STATION_DIR).getSelectedItem() );
				}

				roomSprite.getSquare(squareId).setIgnitionProgress( editorPanel.getSlider(IGNITION).getValue() );

				try { roomSprite.getSquare(squareId).setUnknownGamma( editorPanel.parseInt(GAMMA) ); }
				catch ( NumberFormatException e ) {}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showSystemEditor( final SystemSprite systemSprite ) {
		final String RESERVE_CAPACITY = "Reserve Capacity";
		final String RESERVE_POWER = "Reserve Power";
		final String CAPACITY = "System Capacity";
		final String POWER = "System Power";
		final String DAMAGED_BARS = "Damaged Bars";
		final String IONIZED_BARS = "Ionized Bars";
		final String REPAIR_PROGRESS = "Repair Progress";
		final String DAMAGE_PROGRESS = "Damage Progress";
		final String DEIONIZATION_TICKS = "Deionization Ticks";

		final SystemBlueprint systemBlueprint = DataManager.get().getSystem( systemSprite.getSystemType().getId() );

		int maxSystemCapacity = systemBlueprint.getMaxPower();
		Integer maxPowerOverride = shipBlueprint.getSystemList().getSystemRoom( systemSprite.getSystemType() )[0].getMaxPower();
		if ( maxPowerOverride != null )
			maxSystemCapacity = maxPowerOverride.intValue();

		int z = 0;  // Sum up all the other systems' power usage.
		for ( SystemSprite otherSystemSprite : systemSprites ) {
			if ( otherSystemSprite == systemSprite ) continue;

			if ( SystemType.DRONE_CTRL.equals( otherSystemSprite.getSystemType() ) ) {
				for ( DroneSprite otherDroneSprite : droneSprites ) {
					if ( otherDroneSprite.getDroneId() != null && otherDroneSprite.isArmed() )
						z += DataManager.get().getDrone( otherDroneSprite.getDroneId() ).getPower();
				}
			}
			else if ( SystemType.WEAPONS.equals( otherSystemSprite.getSystemType() ) ) {
				for ( WeaponSprite otherWeaponSprite : weaponSprites ) {
					if ( otherWeaponSprite.getWeaponId() != null && otherWeaponSprite.isArmed() )
						z += DataManager.get().getWeapon( otherWeaponSprite.getWeaponId() ).getPower();
				}
			}
			else if ( !otherSystemSprite.getSystemType().isSubsystem() ) {
				z += otherSystemSprite.getPower();
			}
		}
		final int otherPower = z;
		// Subsystems ignore the reserve, and power can't be directly changed.
		final boolean isSubsystem = systemSprite.getSystemType().isSubsystem();

		String title = systemBlueprint.getTitle();

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( RESERVE_CAPACITY, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(RESERVE_CAPACITY).setMaximum( ftlConstants.getMaxReservePower() );
		editorPanel.getSlider(RESERVE_CAPACITY).setMinimum( otherPower );
		editorPanel.getSlider(RESERVE_CAPACITY).setValue( shipReservePowerCapacity );
		editorPanel.getSlider(RESERVE_CAPACITY).addMouseListener( new StatusbarMouseListener(frame, "Total possible reactor bars (Increase to upgrade).") );
		editorPanel.addRow( RESERVE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(RESERVE_POWER).setMaximum( shipReservePowerCapacity );
		// Reserve power's value is set later.
		editorPanel.getSlider(RESERVE_POWER).setEnabled(false);
		editorPanel.getSlider(RESERVE_POWER).addMouseListener( new StatusbarMouseListener(frame, "Unallocated power.") );
		editorPanel.addBlankRow();
		editorPanel.addRow( CAPACITY, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(CAPACITY).setMaximum( maxSystemCapacity );
		editorPanel.getSlider(CAPACITY).setValue( systemSprite.getCapacity() );
		editorPanel.getSlider(CAPACITY).addMouseListener( new StatusbarMouseListener(frame, "Possible system bars (Increase to buy/upgrade, 0=absent).") );
		editorPanel.addRow( POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(POWER).addMouseListener( new StatusbarMouseListener(frame, "System power.") );
		editorPanel.addRow( DAMAGED_BARS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(DAMAGED_BARS).setMaximum( systemSprite.getCapacity() );
		editorPanel.getSlider(DAMAGED_BARS).setValue( systemSprite.getDamagedBars() );
		editorPanel.getSlider(DAMAGED_BARS).addMouseListener( new StatusbarMouseListener(frame, "Completely damaged bars.") );
		editorPanel.addBlankRow();
		editorPanel.addRow( IONIZED_BARS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(IONIZED_BARS).setDocument( new RegexDocument("-?1?|[0-9]*") );
		editorPanel.getInt(IONIZED_BARS).setText( ""+systemSprite.getIonizedBars() );
		editorPanel.getInt(IONIZED_BARS).addMouseListener( new StatusbarMouseListener(frame, String.format("Ionized bars (can exceed %d but the number won't appear in-game).", ftlConstants.getMaxIonizedBars())) );
		editorPanel.addBlankRow();
		editorPanel.addRow( REPAIR_PROGRESS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(REPAIR_PROGRESS).setMaximum( (systemSprite.getDamagedBars() == 0 ? 0 : 100) );
		editorPanel.getSlider(REPAIR_PROGRESS).setValue( systemSprite.getRepairProgress() );
		editorPanel.getSlider(REPAIR_PROGRESS).addMouseListener( new StatusbarMouseListener(frame, "Turns a damaged bar yellow until restored.") );
		editorPanel.addRow( DAMAGE_PROGRESS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(DAMAGE_PROGRESS).setMaximum( (systemSprite.getDamagedBars() >= systemSprite.getCapacity() ? 0 : 100) );
		editorPanel.getSlider(DAMAGE_PROGRESS).setValue( systemSprite.getDamageProgress() );
		editorPanel.getSlider(DAMAGE_PROGRESS).addMouseListener( new StatusbarMouseListener(frame, "Turns an undamaged bar red until damaged.") );
		editorPanel.addRow( DEIONIZATION_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(DEIONIZATION_TICKS).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.getInt(DEIONIZATION_TICKS).setText( ""+systemSprite.getDeionizationTicks() );
		editorPanel.getInt(DEIONIZATION_TICKS).addMouseListener( new StatusbarMouseListener(frame, "Milliseconds spent deionizing a bar: 0-5000 (Resets upon loading, weird values sometimes, -2147...=N/A, 0's safe too).") );

		if ( isSubsystem ) {
			editorPanel.getSlider(RESERVE_CAPACITY).setEnabled(false);
			editorPanel.getSlider(RESERVE_POWER).setValue( shipReservePowerCapacity-otherPower );
			editorPanel.getSlider(POWER).setMaximum( systemSprite.getCapacity() );
			editorPanel.getSlider(POWER).setValue( systemSprite.getPower() );
			editorPanel.getSlider(POWER).setEnabled(false);
		} else {
			editorPanel.getSlider(RESERVE_POWER).setValue( shipReservePowerCapacity-otherPower-systemSprite.getPower() );
			editorPanel.getSlider(POWER).setMaximum(Math.min( systemSprite.getCapacity(), (shipReservePowerCapacity-otherPower) ));

			// Some non-subsystems determine their power from outside the system sprite...
			int neededPower = 0;
			if ( SystemType.WEAPONS.equals( systemSprite.getSystemType() ) ) {
				neededPower = 0;
				for ( WeaponSprite otherSprite : weaponSprites ) {
					if ( otherSprite.getWeaponId() != null && otherSprite.isArmed() )
						neededPower += DataManager.get().getWeapon( otherSprite.getWeaponId() ).getPower();
				}
				editorPanel.getSlider(POWER).setEnabled( false );
			}
			else if ( SystemType.DRONE_CTRL.equals( systemSprite.getSystemType() ) ) {
				neededPower = 0;
				for ( DroneSprite otherSprite : droneSprites ) {
					if ( otherSprite.getDroneId() != null && otherSprite.isArmed() )
						neededPower += DataManager.get().getDrone( otherSprite.getDroneId() ).getPower();
				}
				editorPanel.getSlider(POWER).setEnabled( false );
			}
			else {
				neededPower = systemSprite.getPower();
			}
			editorPanel.getSlider(POWER).setValue( neededPower );
		}

		ChangeListener barListener = new ChangeListener() {
			private JSlider reserveCapacitySlider = editorPanel.getSlider(RESERVE_CAPACITY);
			private JSlider reservePowerSlider = editorPanel.getSlider(RESERVE_POWER);
			private JSlider capacitySlider = editorPanel.getSlider(CAPACITY);
			private JSlider powerSlider = editorPanel.getSlider(POWER);
			private JSlider damagedBarsSlider = editorPanel.getSlider(DAMAGED_BARS);
			private JSlider repairProgressSlider = editorPanel.getSlider(REPAIR_PROGRESS);
			private JSlider damageProgressSlider = editorPanel.getSlider(DAMAGE_PROGRESS);
			private boolean ignoreChanges = false;
			// Avoid getValueIsAdjusting() checks, which can fail on brief drags.

			@Override
			public void stateChanged( ChangeEvent e ) {
				if ( ignoreChanges ) return;
				ignoreChanges = true;

				Object source = e.getSource();

				syncProgress( source );
				syncBars( source );

				// After all the secondary slider events, resume monitoring.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ignoreChanges = false;
					}
				});
			}

			private void syncProgress( Object source ) {
				if ( source == repairProgressSlider ) {
					// Fight DamageProgess.

					if ( repairProgressSlider.getValue() > 0 )
						damageProgressSlider.setValue( 0 );   // Mutually exclusive.
				}
				else if ( source == damageProgressSlider ) {
					// Fight RepairProgress.

					if ( damageProgressSlider.getValue() > 0 )
						repairProgressSlider.setValue( 0 );   // Mutually exclusive.
				}
			}

			private void syncBars( Object source ) {
				if ( source == reserveCapacitySlider ) {  // Non-subystem only.
					// Cap power based on reserves.
					int reserveCapacity = reserveCapacitySlider.getValue();
					int capacity = capacitySlider.getValue();

					powerSlider.setMaximum(Math.min( capacity, reserveCapacity - otherPower ));
					powerSlider.setValue(Math.min( powerSlider.getValue(), Math.max(0, (reserveCapacity-otherPower)) ));
					drainReserve();
				}
				else if ( source == capacitySlider ) {
					// Set caps.
					int reserveCapacity = reserveCapacitySlider.getValue();
					int capacity = capacitySlider.getValue();

					damagedBarsSlider.setMaximum( capacity );
					int damage = damagedBarsSlider.getValue();
					repairProgressSlider.setMaximum( (damage == 0 ? 0 : 100 ) );
					damageProgressSlider.setMaximum( (damage >= capacity ? 0 : 100 ) );

					if ( isSubsystem ) {  // Power ~= Capacity.
						powerSlider.setMaximum( capacity );
						powerSlider.setValue(Math.min( capacity, Math.max(0, (capacity-damage)) ));
					} else {              // Power merely capped.
						powerSlider.setMaximum(Math.min( capacity, reserveCapacity - otherPower ));
						drainReserve();
					}
				}
				else if ( source == powerSlider ) {  // Non-subystem only.
					int capacity = capacitySlider.getValue();
					int power = powerSlider.getValue();
					damagedBarsSlider.setValue(Math.min( damagedBarsSlider.getValue(), Math.max(0, (capacity-power)) ));
					int damage = damagedBarsSlider.getValue();
					repairProgressSlider.setMaximum( (damage == 0 ? 0 : 100 ) );
					damageProgressSlider.setMaximum( (damage >= capacity ? 0 : 100 ) );

					drainReserve();
				}
				else if ( source == damagedBarsSlider ) {
					// Interfere with Power.
					int capacity = capacitySlider.getValue();
					int damage = damagedBarsSlider.getValue();
					repairProgressSlider.setMaximum( (damage == 0 ? 0 : 100 ) );
					damageProgressSlider.setMaximum( (damage >= capacity ? 0 : 100 ) );

					if ( isSubsystem ) {  // Power ~= Capacity.
						powerSlider.setValue(Math.min( capacity, Math.max(0, (capacity-damage)) ));
					} else {              // Power merely capped.
						powerSlider.setValue(Math.min( powerSlider.getValue(), Math.max(0, (capacity-damage)) ));
						drainReserve();
					}
				}
			}

			public void drainReserve() {
				int power = powerSlider.getValue();
				int reserveCapacity = reserveCapacitySlider.getValue();
				reservePowerSlider.setValue( reserveCapacity - otherPower - power );
			}
		};
		editorPanel.getSlider(RESERVE_CAPACITY).addChangeListener(barListener);
		editorPanel.getSlider(CAPACITY).addChangeListener(barListener);
		editorPanel.getSlider(POWER).addChangeListener(barListener);
		editorPanel.getSlider(DAMAGED_BARS).addChangeListener(barListener);
		editorPanel.getSlider(REPAIR_PROGRESS).addChangeListener(barListener);
		editorPanel.getSlider(DAMAGE_PROGRESS).addChangeListener(barListener);

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String newString;
				shipReservePowerCapacity = editorPanel.getSlider(RESERVE_CAPACITY).getValue();

				systemSprite.setCapacity( editorPanel.getSlider(CAPACITY).getValue() );
				systemSprite.setPower( editorPanel.getSlider(POWER).getValue() );
				systemSprite.setDamagedBars( editorPanel.getSlider(DAMAGED_BARS).getValue() );

				newString = editorPanel.getInt(IONIZED_BARS).getText();
				try { systemSprite.setIonizedBars( Integer.parseInt(newString) ); }
				catch ( NumberFormatException e ) {}

				systemSprite.setRepairProgress( editorPanel.getSlider(REPAIR_PROGRESS).getValue() );
				systemSprite.setDamageProgress( editorPanel.getSlider(DAMAGE_PROGRESS).getValue() );

				newString = editorPanel.getInt(DEIONIZATION_TICKS).getText();
				try { systemSprite.setDeionizationTicks( Integer.parseInt(newString) ); }
				catch ( NumberFormatException e ) {}

				systemSprite.makeSane();

				int neededPower = systemSprite.getPower();
				if ( SystemType.WEAPONS.equals( systemSprite.getSystemType() ) ) {
					if ( systemSprite.getCapacity() == 0 ) {
						// When capacity is 0, nullify all weapons.
						for ( WeaponSprite weaponSprite : weaponSprites )
							weaponSprite.setWeaponId( null );
					}
					else {
						// Disarm everything rightward of first underpowered weapon.
						int foundPower = 0;
						for ( WeaponSprite otherSprite : weaponSprites ) {
							if ( otherSprite.getWeaponId() != null && otherSprite.isArmed() ) {
								foundPower += DataManager.get().getWeapon( otherSprite.getWeaponId() ).getPower();
								if ( foundPower > neededPower ) otherSprite.setArmed( false );
							}
						}
					}
				}
				else if ( SystemType.DRONE_CTRL.equals( systemSprite.getSystemType() ) ) {
					if ( systemSprite.getCapacity() == 0 ) {
						// When capacity is 0, nullify all drones.
						for ( DroneSprite droneSprite : droneSprites ) {
							droneSprite.setDroneId( null );
							droneSprite.makeSane();
						}
					}
					else {
						// Disarm everything rightward of first underpowered drone.
						int foundPower = 0;
						for ( DroneSprite otherSprite : droneSprites ) {
							if ( otherSprite.getDroneId() != null && otherSprite.isArmed() ) {
								foundPower += DataManager.get().getDrone( otherSprite.getDroneId() ).getPower();
								if ( foundPower > neededPower ) {
									otherSprite.setArmed( false );
									otherSprite.makeSane();
								}
							}
						}
					}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(8);
		String notice = "";
		if ( isSubsystem ) {
			notice += "* This is a subsystem, which means reserves are ignored, ";
			notice += "and power is always as full as possible.\n\n";
		}
		if ( SystemType.SHIELDS.equals( systemSprite.getSystemType() ) ) {
			notice += "* Partialy powered shields will steal an extra bar upon loading, ";
			notice += "from another system if need be.\n\n";
		}
		if ( SystemType.WEAPONS.equals( systemSprite.getSystemType() ) ) {
			notice += "* Power can't be directly changed for the Weapons system. ";
			notice += "Toggle paraphernalia separately. ";
			notice += "If capacity/damage reduce power, ";
			notice += "things will get disarmed.\n\n";
		}
		if ( SystemType.DRONE_CTRL.equals( systemSprite.getSystemType() ) ) {
			notice += "* Power can't be directly changed for the Drone Ctrl system. ";
			notice += "Toggle paraphernalia separately. ";
			notice += "If capacity/damage reduce power, ";
			notice += "things will get disarmed.\n\n";
		}

		notice += "* Ion -1: in Cloaking initates cloak; ";
		notice += "in Teleporter might not be useful; ";
		notice += "elsewhere sets a locked appearance indefinitely ";
		notice += "until hit with an ion weapon.\n";

		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showBreachEditor( final BreachSprite breachSprite ) {
		final String HEALTH = "Health";

		String title = "Breach";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( 100 );
		editorPanel.getSlider(HEALTH).setValue( breachSprite.getHealth() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				breachSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				breachSprites.remove( breachSprite );
				shipPanel.remove( breachSprite );
			}
		});

		showSidePanel();
	}

	private void showFireEditor( final FireSprite fireSprite ) {
		final String HEALTH = "Health";

		String title = "Fire";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider(HEALTH).setMaximum( 100 );
		editorPanel.getSlider(HEALTH).setValue( fireSprite.getHealth() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				fireSprite.setHealth( editorPanel.getSlider(HEALTH).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				fireSprites.remove( fireSprite );
				shipPanel.remove( fireSprite );
			}
		});

		showSidePanel();
	}

	private void showDoorEditor( final DoorSprite doorSprite ) {
		final String OPEN = "Open";
		final String WALKING_THROUGH = "Walking Through";
		final String MAX_HEALTH = "Current Max Health?";
		final String HEALTH = "Health";
		final String NOMINAL_HEALTH = "Nominal Health?";
		final String DELTA = "Delta?";
		final String EPSILON = "Epsilon?";

		ShipLayout.DoorCoordinate doorCoord = doorSprite.getCoordinate();
		String title = String.format("Door (%2d,%2d, %s)", doorCoord.x, doorCoord.y, (doorCoord.v==1 ? "V" : "H"));

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OPEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( WALKING_THROUGH, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(WALKING_THROUGH).addMouseListener( new StatusbarMouseListener(frame, "Momentarily open as someone walks through.") );
		editorPanel.addRow( MAX_HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(MAX_HEALTH).addMouseListener( new StatusbarMouseListener(frame, "Nominal Health, plus situatinal modifiers like hacking?") );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( NOMINAL_HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(NOMINAL_HEALTH).addMouseListener( new StatusbarMouseListener(frame, "Default to reset Health to... sometime after combat?") );
		editorPanel.addRow( DELTA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(DELTA).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Hacking related.") );
		editorPanel.addRow( EPSILON, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(EPSILON).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Hacking related.") );

		editorPanel.getBoolean(OPEN).setSelected( doorSprite.isOpen() );
		editorPanel.getBoolean(WALKING_THROUGH).setSelected( doorSprite.isWalkingThrough() );
		editorPanel.getInt(MAX_HEALTH).setText( ""+doorSprite.getCurrentMaxHealth() );
		editorPanel.getInt(HEALTH).setText( ""+doorSprite.getHealth() );
		editorPanel.getInt(NOMINAL_HEALTH).setText( ""+doorSprite.getNominalHealth() );
		editorPanel.getInt(DELTA).setText( ""+doorSprite.getUnknownDelta() );
		editorPanel.getInt(EPSILON).setText( ""+doorSprite.getUnknownEpsilon() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				doorSprite.setOpen( editorPanel.getBoolean(OPEN).isSelected() );
				doorSprite.setWalkingThrough( editorPanel.getBoolean(WALKING_THROUGH).isSelected() );

				try { doorSprite.setCurrentMaxHealth( editorPanel.parseInt(MAX_HEALTH) ); }
				catch ( NumberFormatException e ) {}

				try { doorSprite.setHealth( editorPanel.parseInt(HEALTH) ); }
				catch ( NumberFormatException e ) {}

				try { doorSprite.setNominalHealth( editorPanel.parseInt(NOMINAL_HEALTH) ); }
				catch ( NumberFormatException e ) {}

				try { doorSprite.setUnknownDelta( editorPanel.parseInt(DELTA) ); }
				catch ( NumberFormatException e ) {}

				try { doorSprite.setUnknownEpsilon( editorPanel.parseInt(EPSILON) ); }
				catch ( NumberFormatException e ) {}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showCrewRoster() {
		final String CREW = "Crew";

		String title = "Select Crew";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( CREW, FieldEditorPanel.ContentType.COMBO );

		for ( CrewSprite crewSprite : crewSprites ) {
			editorPanel.getCombo(CREW).addItem( crewSprite );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object crewObj = editorPanel.getCombo(CREW).getSelectedItem();
				if ( crewObj instanceof CrewSprite ) {
					showCrewEditor( (CrewSprite)crewObj );
				}
			}
		};

		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(8);
		String notice = "";
		notice += "* All crew appear in this list, including dead ones awaiting ";
		notice += "cloned bodies. Living crew can also be clicked directly.\n";

		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showCrewEditor( final CrewSprite crewSprite ) {
		final String NAME = "Name";
		final String RACE = "Race";
		final String HEALTH = "Health";
		final String PILOT_SKILL = "Pilot Skill";
		final String ENGINE_SKILL = "Engine Skill";
		final String SHIELD_SKILL = "Shield Skill";
		final String WEAPON_SKILL = "Weapon Skill";
		final String REPAIR_SKILL = "Repair Skill";
		final String COMBAT_SKILL = "Combat Skill";
		final String REPAIRS = "Repairs";
		final String COMBAT_KILLS = "Combat Kills";
		final String PILOTED_EVASIONS = "Piloted Evasions";
		final String JUMPS_SURVIVED = "Jumps Survived";
		final String SKILL_MASTERIES = "Skill Masteries";
		final String SEX = "Male";
		final String ENEMY_DRONE = "Enemy Drone";
		final String PLAYER_CONTROLLED = "Player Ctrl";
		final String ALPHA = "Alpha?";
		final String BETA = "Beta?";
		final String MIND_CONTROLLED = "Mind Ctrl";
		final String STUN_TICKS = "Stun Ticks";
		final String HEALTH_BOOST = "Health Boost";
		final String IOTA = "Iota?";
		final String KAPPA = "Kappa?";
		final String LAMBDA = "Lambda?";
		final String MU = "Mu?";
		final String NU = "Nu?";
		final String XI = "Xi?";
		final String OMICRON = "Omicron?";
		final String TELEPORT_ANIM_FRAME = "Teleport Anim Frame";
		final String RHO = "Rho?";
		final String SIGMA = "Sigma?";
		final String TAU = "Tau?";
		final String UPSILON = "Upsilon?";
		final String PHI = "Phi?";
		final String LOCKDOWN_RECHARGE_TICKS = "Lockdown Recharge Ticks";
		final String LOCKDOWN_RECHARGE_GOAL = "Lockdown Recharge Goal";
		final String OMEGA = "Omega?";

		String title = "Crew";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.addRow( RACE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(HEALTH).addMouseListener( new StatusbarMouseListener(frame, "Current health, including temporary boost. FTL 1.01-1.03.3 capped this at the race's max.") );
		editorPanel.addRow( PILOT_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addRow( ENGINE_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addRow( SHIELD_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addRow( WEAPON_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addRow( REPAIR_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addRow( COMBAT_SKILL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.addBlankRow();
		editorPanel.addRow( REPAIRS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( COMBAT_KILLS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( PILOTED_EVASIONS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( JUMPS_SURVIVED, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( SKILL_MASTERIES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SKILL_MASTERIES).addMouseListener( new StatusbarMouseListener(frame, "Total skill mastery levels ever earned. Up to two from each skill.") );
		editorPanel.addRow( SEX, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(SEX).addMouseListener( new StatusbarMouseListener(frame, "Only human females have a distinct sprite (Other races look the same either way).") );
		editorPanel.addRow( ENEMY_DRONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(ENEMY_DRONE).addMouseListener( new StatusbarMouseListener(frame, "Turn into a boarding drone (clobbering other fields), hostile to this ship.") );
		editorPanel.addRow( PLAYER_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(PLAYER_CONTROLLED).addMouseListener( new StatusbarMouseListener(frame, "Player controlled vs NPC.") );
		editorPanel.addRow( ALPHA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(ALPHA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( BETA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(BETA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( MIND_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( STUN_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( HEALTH_BOOST, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(HEALTH_BOOST).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( IOTA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(IOTA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( KAPPA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(KAPPA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( LAMBDA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(LAMBDA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( MU, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(MU).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( NU, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(NU).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.getInt(NU).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Related to teleport anim?") );
		editorPanel.addRow( XI, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(XI).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( OMICRON, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(OMICRON).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( TELEPORT_ANIM_FRAME, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(TELEPORT_ANIM_FRAME).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( RHO, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(RHO).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.getInt(RHO).addMouseListener( new StatusbarMouseListener(frame, "Unknown. Related to teleport anim?") );
		editorPanel.addRow( SIGMA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(SIGMA).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( TAU, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(TAU).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( UPSILON, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(UPSILON).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( PHI, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(PHI).setDocument( new RegexDocument("-?[0-9]*") );
		editorPanel.addRow( LOCKDOWN_RECHARGE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( LOCKDOWN_RECHARGE_GOAL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( OMEGA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(OMEGA).setDocument( new RegexDocument("-?[0-9]*") );

		ActionListener crewListener = new ActionListener() {
			private JComboBox raceCombo = editorPanel.getCombo(RACE);
			private JTextField healthField = editorPanel.getInt(HEALTH);

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == raceCombo ) {
					CrewType crewType = (CrewType)raceCombo.getSelectedItem();

					int pilotInterval = ftlConstants.getMasteryIntervalPilot( crewType.getId() );
					int engineInterval = ftlConstants.getMasteryIntervalEngine( crewType.getId() );
					int shieldInterval = ftlConstants.getMasteryIntervalShield( crewType.getId() );
					int weaponInterval = ftlConstants.getMasteryIntervalWeapon( crewType.getId() );
					int repairInterval = ftlConstants.getMasteryIntervalRepair( crewType.getId() );
					int combatInterval = ftlConstants.getMasteryIntervalCombat( crewType.getId() );

					healthField.setText( ""+crewType.getMaxHealth() );

					editorPanel.getSlider(PILOT_SKILL).setMaximum( pilotInterval*2 );
					editorPanel.getSlider(ENGINE_SKILL).setMaximum( engineInterval*2 );
					editorPanel.getSlider(SHIELD_SKILL).setMaximum( shieldInterval*2 );
					editorPanel.getSlider(WEAPON_SKILL).setMaximum( weaponInterval*2 );
					editorPanel.getSlider(REPAIR_SKILL).setMaximum( repairInterval*2 );
					editorPanel.getSlider(COMBAT_SKILL).setMaximum( combatInterval*2 );
				}
			}
		};
		editorPanel.getCombo(RACE).addActionListener( crewListener );

		for ( CrewType crewType : ftlConstants.getCrewTypes() ) {
			editorPanel.getCombo(RACE).addItem( crewType );
		}
		editorPanel.getCombo(RACE).setSelectedItem( CrewType.findById( crewSprite.getRace() ) );

		SwingUtilities.invokeLater(new Runnable() {  // Set health after the race combo listener triggers.
			@Override
			public void run() {
				editorPanel.getInt(HEALTH).setText( ""+crewSprite.getHealth() );
			}
		});

		editorPanel.getString(NAME).setText( crewSprite.getName() );

		editorPanel.getSlider(PILOT_SKILL).setValue( crewSprite.getPilotSkill() );
		editorPanel.getSlider(ENGINE_SKILL).setValue( crewSprite.getEngineSkill() );
		editorPanel.getSlider(SHIELD_SKILL).setValue( crewSprite.getShieldSkill() );
		editorPanel.getSlider(WEAPON_SKILL).setValue( crewSprite.getWeaponSkill() );
		editorPanel.getSlider(REPAIR_SKILL).setValue( crewSprite.getRepairSkill() );
		editorPanel.getSlider(COMBAT_SKILL).setValue( crewSprite.getCombatSkill() );

		editorPanel.getInt(REPAIRS).setText( ""+crewSprite.getRepairs() );
		editorPanel.getInt(COMBAT_KILLS).setText( ""+crewSprite.getCombatKills() );
		editorPanel.getInt(PILOTED_EVASIONS).setText( ""+crewSprite.getPilotedEvasions() );
		editorPanel.getInt(JUMPS_SURVIVED).setText( ""+crewSprite.getJumpsSurvived() );
		editorPanel.getInt(SKILL_MASTERIES).setText( ""+crewSprite.getSkillMasteries() );
		editorPanel.getBoolean(SEX).setSelected( crewSprite.isMale() );
		editorPanel.getBoolean(ENEMY_DRONE).setSelected( crewSprite.isEnemyBoardingDrone() );
		editorPanel.getBoolean(PLAYER_CONTROLLED).setSelected( crewSprite.isPlayerControlled() );
		editorPanel.getInt(ALPHA).setText( ""+crewSprite.getUnknownAlpha() );
		editorPanel.getInt(BETA).setText( ""+crewSprite.getUnknownBeta() );
		editorPanel.getBoolean(MIND_CONTROLLED).setSelected( crewSprite.isMindControlled() );

		editorPanel.getInt(STUN_TICKS).setText( ""+crewSprite.getStunTicks() );
		editorPanel.getInt(HEALTH_BOOST).setText( ""+crewSprite.getHealthBoost() );
		editorPanel.getInt(IOTA).setText( ""+crewSprite.getUnknownIota() );
		editorPanel.getInt(KAPPA).setText( ""+crewSprite.getUnknownKappa() );
		editorPanel.getInt(LAMBDA).setText( ""+crewSprite.getUnknownLambda() );
		editorPanel.getInt(MU).setText( ""+crewSprite.getUnknownMu() );
		editorPanel.getInt(NU).setText( ""+crewSprite.getUnknownNu() );
		editorPanel.getInt(XI).setText( ""+crewSprite.getUnknownXi() );
		editorPanel.getInt(OMICRON).setText( ""+crewSprite.getUnknownOmicron() );
		editorPanel.getInt(TELEPORT_ANIM_FRAME).setText( ""+crewSprite.getTeleportAnimFrame() );
		editorPanel.getInt(RHO).setText( ""+crewSprite.getUnknownRho() );
		editorPanel.getInt(SIGMA).setText( ""+crewSprite.getUnknownSigma() );
		editorPanel.getInt(TAU).setText( ""+crewSprite.getUnknownTau() );
		editorPanel.getInt(UPSILON).setText( ""+crewSprite.getUnknownUpsilon() );
		editorPanel.getInt(PHI).setText( ""+crewSprite.getUnknownPhi() );
		editorPanel.getInt(LOCKDOWN_RECHARGE_TICKS).setText( ""+crewSprite.getLockdownRechargeTicks() );
		editorPanel.getInt(LOCKDOWN_RECHARGE_GOAL).setText( ""+crewSprite.getLockdownRechargeTicksGoal() );
		editorPanel.getInt(OMEGA).setText( ""+crewSprite.getUnknownOmega() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				crewSprite.setName( editorPanel.getString(NAME).getText() );
				crewSprite.setRace( ((CrewType)editorPanel.getCombo(RACE).getSelectedItem()).getId() );

				try { crewSprite.setHealth( editorPanel.parseInt(HEALTH) ); }
				catch ( NumberFormatException e ) {}

				crewSprite.setPilotSkill( editorPanel.getSlider(PILOT_SKILL).getValue() );
				crewSprite.setEngineSkill( editorPanel.getSlider(ENGINE_SKILL).getValue() );
				crewSprite.setShieldSkill( editorPanel.getSlider(SHIELD_SKILL).getValue() );
				crewSprite.setWeaponSkill( editorPanel.getSlider(WEAPON_SKILL).getValue() );
				crewSprite.setRepairSkill( editorPanel.getSlider(REPAIR_SKILL).getValue() );
				crewSprite.setCombatSkill( editorPanel.getSlider(COMBAT_SKILL).getValue() );

				try { crewSprite.setRepairs( editorPanel.parseInt(REPAIRS) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setCombatKills( editorPanel.parseInt(COMBAT_KILLS) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setPilotedEvasions( editorPanel.parseInt(PILOTED_EVASIONS) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setJumpsSurvived( editorPanel.parseInt(JUMPS_SURVIVED) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setSkillMasteries( editorPanel.parseInt(SKILL_MASTERIES) ); }
				catch ( NumberFormatException e ) {}

				crewSprite.setMale( editorPanel.getBoolean(SEX).isSelected() );
				crewSprite.setEnemyBoardingDrone( editorPanel.getBoolean(ENEMY_DRONE).isSelected() );
				crewSprite.setPlayerControlled( editorPanel.getBoolean(PLAYER_CONTROLLED).isSelected() );

				try { crewSprite.setUnknownAlpha( editorPanel.parseInt(ALPHA) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownBeta( editorPanel.parseInt(BETA) ); }
				catch ( NumberFormatException e ) {}

				crewSprite.setMindControlled( editorPanel.getBoolean(MIND_CONTROLLED).isSelected() );

				try { crewSprite.setStunTicks( editorPanel.parseInt(STUN_TICKS) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setHealthBoost( editorPanel.parseInt(HEALTH_BOOST) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownIota( editorPanel.parseInt(IOTA) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownKappa( editorPanel.parseInt(KAPPA) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownLambda( editorPanel.parseInt(LAMBDA) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownMu( editorPanel.parseInt(MU) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownNu( editorPanel.parseInt(NU) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownXi( editorPanel.parseInt(XI) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownOmicron( editorPanel.parseInt(OMICRON) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setTeleportAnimFrame( editorPanel.parseInt(TELEPORT_ANIM_FRAME) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownRho( editorPanel.parseInt(RHO) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownSigma( editorPanel.parseInt(SIGMA) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownTau( editorPanel.parseInt(TAU) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownUpsilon( editorPanel.parseInt(UPSILON) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownPhi( editorPanel.parseInt(PHI) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setLockdownRechargeTicks( editorPanel.parseInt(LOCKDOWN_RECHARGE_TICKS) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setLockdownRechargeTicksGoal( editorPanel.parseInt(LOCKDOWN_RECHARGE_GOAL) ); }
				catch ( NumberFormatException e ) {}

				try { crewSprite.setUnknownOmega( editorPanel.parseInt(OMEGA) ); }
				catch ( NumberFormatException e ) {}

				crewSprite.makeSane();
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton moveBtn = new JButton("Move To...");
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(moveBtn);

		sidePanel.add( Box.createVerticalStrut(6) );

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		moveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				moveCrew( crewSprite );
			}
		});

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				crewSprites.remove( crewSprite );
				shipPanel.remove( crewSprite );
			}
		});

		addSidePanelSeparator(8);
		String notice = "";
		notice += "* FTL will crash if you control more than 8 non-drone crew.\n";

		notice += "* Boarding drones are drones on their owner's ship, and crew ";
		notice += "(race='battle') on the attacked ship.";

		addSidePanelNote( notice );

		showSidePanel();
	}



	public class DroneSprite extends JComponent {
		private int imageWidth = 45, imageHeight = 45;
		private int slot;
		private String slotString;
		private String droneId = null;
		private boolean armed = false;
		private boolean playerControlled = false;
		private int health = -1;
		private int bodyX = -1, bodyY = -1;
		private JLabel droneBody = new JLabel();
		private BufferedImage bodyImage = null;

		public DroneSprite( int slot, SavedGameParser.DroneState droneState ) {
			this.slot = slot;
			this.slotString = Integer.toString( slot+1 );
			if ( droneState != null ) {
				droneId = droneState.getDroneId();
				armed = droneState.isArmed();
				playerControlled = droneState.isPlayerControlled();
				health = droneState.getHealth();
				bodyX = droneState.getSpriteX();
				bodyY = droneState.getSpriteY();
				droneBody.setLocation( bodyX, bodyY );  // Square center coords, but body's 0x0 right now.
			}
			this.setSize( imageWidth, imageHeight );
			this.setOpaque( false );
			makeSane();
		}

		public void setSpriteX( int n ) { bodyX = n; }
		public void setSpriteY( int n ) { bodyY = n; }
		public void setSlot( int n ) { slot = n; }

		public void setDroneId( String s ) {
			droneId = s;

			if ( droneId == null ) {  // Reset on null id.
				armed = false;
				playerControlled = false;
				health = -1;
				bodyX = -1;
				bodyY = -1;
			}
		}

		public void setArmed( boolean b ) { armed = b; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setHealth( int n ) { health = n; }

		public int getImageWidth() { return imageWidth; }
		public int getImageHeight() { return imageHeight; }
		public JLabel getBody() { return droneBody; }
		public int getSpriteX() { return bodyX; }
		public int getSpriteY() { return bodyY; }
		public int getSlot() { return slot; }
		public String getDroneId() { return droneId; }
		public boolean isArmed() { return armed; }
		public boolean isPlayerControlled() { return playerControlled; }
		public int getHealth() { return health; }

		public void makeSane() {
			BufferedImage newBodyImage = null;
			String imgRace = null;
			boolean needsBody = false;

			if ( droneId != null ) {
				DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneId );

				if ( DroneType.BATTLE.getId().equals(droneBlueprint.getType()) ) {
					imgRace = "battle";
					needsBody = true;
				}
				else if ( DroneType.REPAIR.getId().equals(droneBlueprint.getType()) ) {
					imgRace = "repair";
					needsBody = true;
				}
				else {
					bodyX = -1; bodyY = -1;  // No body. And boarder bodies are crew on nearby ships.
				}

				if ( armed && needsBody && (bodyX < 0 || bodyY < 0) ) {
					// Search for an empty square in droneCtrl.
					int[] droneSystemRoomId = shipBlueprint.getSystemList().getRoomIdBySystemType( SystemType.DRONE_CTRL );
					if ( droneSystemRoomId != null ) {
						EnumMap<ShipLayout.RoomInfo, Integer> roomInfoMap = shipLayout.getRoomInfo( droneSystemRoomId[0] );
						int roomLocX = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_X ).intValue();
						int roomLocY = roomInfoMap.get( ShipLayout.RoomInfo.LOCATION_Y ).intValue();
						int roomX = originX + roomLocX*squareSize;
						int roomY = originY + roomLocY*squareSize;
						int squaresH = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_H ).intValue();
						int squaresV = roomInfoMap.get( ShipLayout.RoomInfo.SQUARES_V ).intValue();

						for (int s=0; s < squaresH*squaresV; s++) {
							int squareX = roomX + tileEdge + (s%squaresH)*squareSize;
							int squareY = roomY + tileEdge + (s/squaresH)*squareSize;
							Rectangle squareRect = new Rectangle(squareX, squareY, squareSize, squareSize);

							boolean occupied = false;
							for ( DroneSprite otherSprite : droneSprites ) {
								JLabel otherBody = otherSprite.getBody();
								if ( squareRect.contains( otherBody.getLocation() ) ) {
									occupied = true;
									break;
								}
							}
							if ( occupied == false ) {
								bodyX = shipLayout.getOffsetX()*squareSize + roomLocX*squareSize + (s%squaresH)*squareSize + squareSize/2;
								bodyY = shipLayout.getOffsetY()*squareSize + roomLocY*squareSize + (s/squaresH)*squareSize + squareSize/2;
								break;
							}
						}  // Assume there'll never be more drones than can fit in the Drone Ctrl room.
					}
				}
				if ( armed && needsBody && (bodyX < 0 || bodyY < 0) ) {
					armed = false;  // Couldn't add the body.
				}
				if ( !armed ) {  // Drones that aren't armed are hidden.
					bodyX = -1; bodyY = -1;
				}

				if ( needsBody && imgRace != null ) {
					newBodyImage = getBodyImage( imgRace, isPlayerControlled() );
				}
			}

			if ( newBodyImage != bodyImage ) {
				int oldW = bodyImage != null ? bodyImage.getWidth() : 0;
				int oldH = bodyImage != null ? bodyImage.getHeight() : 0;
				int newW = newBodyImage != null ? newBodyImage.getWidth() : 0;
				int newH = newBodyImage != null ? newBodyImage.getHeight() : 0;

				if ( newBodyImage != null )
					droneBody.setIcon( new ImageIcon( newBodyImage ) );
				else
					droneBody.setIcon( null );

				int bodyPixelX = bodyX + originX + tileEdge - shipLayout.getOffsetX()*squareSize;
				int bodyPixelY = bodyY + originY + tileEdge - shipLayout.getOffsetY()*squareSize;

				droneBody.setVisible( (bodyX >=0 && bodyY >= 0) );
				droneBody.setSize( newW, newH );
				droneBody.setLocation( bodyPixelX-newW/2, bodyPixelY-newH/2 );
				bodyImage = newBodyImage;
			}
		}

		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			int w = this.getWidth(), h = this.getHeight();
			g2d.drawRect( 0, 0, w-1, h-1 );

			LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics(slotString, g2d);
			int slotStringWidth = g2d.getFontMetrics().stringWidth(slotString);
			int slotStringHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
			int margin = 4;
			int slotStringX = (w-1)/2 - slotStringWidth/2;
			int slotStringY = (h-1)/2 + slotStringHeight/2;  // drawString draws text above Y.
			g2d.drawString( slotString, slotStringX, slotStringY );
		}
	}



	public class WeaponSprite extends JComponent {
		private int imageWidth = 64, imageHeight = 25;
		private boolean rotated;
		private int slot;
		private String slotString;
		private String weaponId = null;
		private boolean armed = false;
		private int cooldownTicks = 0;

		public WeaponSprite( boolean rotated, int slot, SavedGameParser.WeaponState weaponState ) {
			this.rotated = rotated;
			this.slot = slot;
			this.slotString = Integer.toString( slot+1 );
			if ( weaponState != null ) {
				weaponId = weaponState.getWeaponId();
				armed = weaponState.isArmed();
				cooldownTicks = weaponState.getCooldownTicks();
			}
			if ( rotated )
				this.setSize( imageWidth, imageHeight );
			else
				this.setSize( imageHeight, imageWidth );
			this.setOpaque(false);
		}

		public void setSlot( int n ) { slot = n; }

		public void setWeaponId( String s ) {
			weaponId = s;

			if ( weaponId == null ) {  // Reset on null id.
				armed = false;
				cooldownTicks = 0;
			}
		}

		public void setArmed( boolean b ) { armed = b; }
		public void setCooldownTicks( int n ) { cooldownTicks = n; }

		public int getImageWidth() { return imageWidth; }
		public int getImageHeight() { return imageHeight; }
		public int getSlot() { return slot; }
		public String getWeaponId() { return weaponId; }
		public boolean isArmed() { return armed; }
		public int getCooldownTicks() { return cooldownTicks; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			int w = this.getWidth(), h = this.getHeight();
			g2d.drawRect( 0, 0, w-1, h-1 );

			LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics(slotString, g2d);
			int slotStringWidth = g2d.getFontMetrics().stringWidth(slotString);
			int slotStringHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
			int margin = 6;
			if ( rotated ) {
				int slotStringX = (w-1) - slotStringWidth;
				int slotStringY = (h-1)/2 + slotStringHeight/2;  // drawString draws text above Y.
				g2d.drawString( slotString, slotStringX - margin, slotStringY );
			} else {
				int slotStringX = (w-1)/2 - slotStringWidth/2;
				int slotStringY = 0 + slotStringHeight;  // drawString draws text above Y.
				g2d.drawString( slotString, slotStringX, slotStringY + margin );
			}
		}
	}



	public class RoomSprite extends JComponent {
		private final Color maxColor = new Color( 230, 226, 219 );
		private final Color minColor = new Color( 255, 176, 169 );
		private final Color vacuumBorderColor = new Color(255, 180, 0);

		private int roomId;
		private int oxygen;
		private int stationSquare;
		private StationDirection stationDirection;
		private ArrayList<SavedGameParser.SquareState> squareList = new ArrayList<SavedGameParser.SquareState>();
		private Color bgColor;

		public RoomSprite( int roomId, SavedGameParser.RoomState roomState) {
			this.roomId = roomId;
			setOxygen( roomState.getOxygen() );
			stationSquare = roomState.getStationSquare();
			stationDirection = roomState.getStationDirection();

			for ( SavedGameParser.SquareState squareState : roomState.getSquareList() ) {
				SavedGameParser.SquareState tmpSquare = new SavedGameParser.SquareState( squareState );
				squareList.add( tmpSquare );
			}
			this.setOpaque(true);
		}

		public void setRoomId( int n ) { roomId = n; }
		public int getRoomId() { return roomId; }

		public void setOxygen( int n ) {
			oxygen = n;
			if ( oxygen == 100 ) {
				bgColor = maxColor;
			} else if ( oxygen == 0 ) {
				bgColor = minColor;
			} else {
				double p = oxygen / 100.0;
				int maxRed = maxColor.getRed();
				int maxGreen = maxColor.getGreen();
				int maxBlue = maxColor.getBlue();
				int minRed = minColor.getRed();
				int minGreen = minColor.getGreen();
				int minBlue = minColor.getBlue();
				bgColor = new Color( (int)(minRed+p*(maxRed-minRed)), (int)(minGreen+p*(maxGreen-minGreen)), (int)(minBlue+p*(maxBlue-minBlue)) );
			}
		}
		public int getOxygen() { return oxygen; }

		public void setStationSquare( int n ) { stationSquare = n; }
		public int getStationSquare() { return stationSquare; }

		public void setStationDirection( StationDirection d ) { stationDirection = d; }
		public StationDirection getStationDirection() { return stationDirection; }

		public SavedGameParser.SquareState getSquare( int n ) {
			return squareList.get(n);
		}

		public ArrayList<SavedGameParser.SquareState> getSquareList() { return squareList; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();

			g2d.setColor( bgColor );
			g2d.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);

			if ( oxygen == 0 ) {  // Draw the yellow border.
				g2d.setColor( vacuumBorderColor );
				g2d.drawRect( 2, 2, (this.getWidth()-1)-4, (this.getHeight()-1)-4 );
				g2d.drawRect( 3, 3, (this.getWidth()-1)-6, (this.getHeight()-1)-6 );
			}

			g2d.setColor(prevColor);
		}
	}



	public class SystemSprite extends JComponent {
		private BufferedImage overlayImage;

		private SystemType systemType;
		private int capacity;
		private int power;
		private int damagedBars;
		private int ionizedBars;
		private int repairProgress;
		private int damageProgress;
		private int deionizationTicks;
		private int batteryPower;
		private int hackLevel;
		private boolean hacked;
		private int temporaryCapacityCap;
		private int temporaryCapacityLoss;
		private int temporaryCapacityDivisor;

		private BufferedImage currentImage;

		public SystemSprite( BufferedImage overlayImage, SavedGameParser.SystemState systemState ) {
			this.overlayImage = overlayImage;

			systemType = systemState.getSystemType();
			capacity = systemState.getCapacity();
			power = systemState.getPower();
			damagedBars = systemState.getDamagedBars();
			ionizedBars = systemState.getIonizedBars();
			repairProgress = systemState.getRepairProgress();
			damageProgress = systemState.getDamageProgress();
			deionizationTicks = systemState.getDeionizationTicks();
			batteryPower = systemState.getBatteryPower();
			hackLevel = systemState.getHackLevel();
			hacked = systemState.isHacked();
			temporaryCapacityCap = systemState.getTemporaryCapacityCap();
			temporaryCapacityLoss = systemState.getTemporaryCapacityLoss();
			temporaryCapacityDivisor = systemState.getTemporaryCapacityDivisor();

			this.setOpaque(false);
			makeSane();
		}

		public SystemType getSystemType() { return systemType; }

		public void setCapacity( int n ) { capacity = n; }
		public void setPower( int n ) { power = n; }
		public void setDamagedBars( int n ) { damagedBars = n; }
		public void setIonizedBars( int n ) { ionizedBars = n; }
		public void setRepairProgress( int n ) { repairProgress = n; }
		public void setDamageProgress( int n ) { damageProgress = n; }
		public void setDeionizationTicks( int n ) { deionizationTicks = n; }

		public int getCapacity() { return capacity; }
		public int getPower() { return power; }
		public int getDamagedBars() { return damagedBars; }
		public int getIonizedBars() { return ionizedBars; }
		public int getRepairProgress() { return repairProgress; }
		public int getDamageProgress() { return damageProgress; }
		public int getDeionizationTicks() { return deionizationTicks; }

		public void setBatteryPower( int n ) { batteryPower = n; }
		public void setHackLevel( int n ) { hackLevel = n; }
		public void setHacked( boolean b ) { hacked = b; }
		public void setTemporaryCapacityCap( int n ) { temporaryCapacityCap = n; }
		public void setTemporaryCapacityLoss( int n ) { temporaryCapacityLoss = n; }
		public void setTemporaryCapacityDivisor( int n ) { temporaryCapacityDivisor = n; }

		public int getBatteryPower() { return batteryPower; }
		public int getHackLevel() { return hackLevel; }
		public boolean isHacked() { return hacked; }
		public int getTemporaryCapacityCap() { return temporaryCapacityCap; }
		public int getTemporaryCapacityLoss() { return temporaryCapacityLoss; }
		public int getTemporaryCapacityDivisor() { return temporaryCapacityDivisor; }

		public void makeSane() {
			// The original overlayImage is white with a black border.
			Tint tint = null;

			if ( getCapacity() == 0 ) {
				// Absent, selectively darken to brown.
				tint = new Tint( new float[] { 0.792f, 0.467f, 0.275f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( getIonizedBars() > 0 ) {
				// Ionized, selectively darken to blue.
				tint = new Tint( new float[] { 0.51f, 0.898f, 0.937f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( getDamagedBars() == getCapacity() ) {
				// Crippled, selectively darken to red (softer shade than in-game).
				tint = new Tint( new float[] { 0.85f, 0.24f, 0.24f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( getDamagedBars() > 0 ) {
				// Damaged, selectively darken to orange.
				tint = new Tint( new float[] { 0.99f, 0.6f, 0.3f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else {
				// Darken to gray...
				tint = new Tint( new float[] { 0.49f, 0.49f, 0.49f, 1f }, new float[] { 0, 0, 0, 0 } );
			}

			currentImage = overlayImage;
			if ( tint != null )
				currentImage = getTintedImage( currentImage, tint );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class BreachSprite extends JComponent {
		private BufferedImage breachImage;
		private int roomId;
		private int squareId;
		private int health;

		public BreachSprite( BufferedImage breachImage, int roomId, int squareId, int health ) {
			this.breachImage = breachImage;
			this.roomId = roomId;
			this.squareId = squareId;
			this.health = health;
			this.setOpaque(false);
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( breachImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class FireSprite extends JComponent {
		private BufferedImage fireImage;
		private int roomId;
		private int squareId;
		private int health;

		public FireSprite( BufferedImage fireImage, int roomId, int squareId, int health ) {
			this.fireImage = fireImage;
			this.roomId = roomId;
			this.squareId = squareId;
			this.health = health;
			this.setOpaque(false);
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( fireImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class DoorSprite extends JComponent {
		private BufferedImage[] closedImages;
		private BufferedImage[] openImages;
		private int level;
		private ShipLayout.DoorCoordinate doorCoord;

		private boolean open;
		private boolean walkingThrough;

		private int currentMaxHealth = 0;
		private int health = 0;
		private int nominalHealth = 0;
		private int unknownDelta = 0;
		private int unknownEpsilon = 0;

		private Color validColor = Color.GREEN.darker();
		private boolean selectionRectVisible = false;

		public DoorSprite( BufferedImage[] closedImages, BufferedImage[] openImages, int level, ShipLayout.DoorCoordinate doorCoord, SavedGameParser.DoorState doorState ) {
			this.closedImages = closedImages;
			this.openImages = openImages;
			this.level = level;
			this.doorCoord = doorCoord;

			open = doorState.isOpen();
			walkingThrough = doorState.isWalkingThrough();
			currentMaxHealth = doorState.getCurrentMaxHealth();
			health = doorState.getHealth();
			nominalHealth = doorState.getNominalHealth();
			unknownDelta = doorState.getUnknownDelta();
			unknownEpsilon = doorState.getUnknownEpsilon();

			this.setOpaque(false);
		}

		public void setLevel( int n ) { level = n; }
		public int getLevel() { return level; }

		public void setCoordinate( ShipLayout.DoorCoordinate c ) { doorCoord = c; }
		public ShipLayout.DoorCoordinate getCoordinate() { return doorCoord; }

		public void setOpen( boolean b ) { open = b; }
		public void setWalkingThrough( boolean b ) { walkingThrough = b; }
		public void setCurrentMaxHealth( int n ) { currentMaxHealth = n; }
		public void setHealth( int n ) { health = n; }
		public void setNominalHealth( int n ) { nominalHealth = n; }
		public void setUnknownDelta( int n ) { unknownDelta = n; }
		public void setUnknownEpsilon( int n ) { unknownEpsilon = n; }

		public boolean isOpen() { return open; }
		public boolean isWalkingThrough() { return walkingThrough; }
		public int getCurrentMaxHealth() { return currentMaxHealth; }
		public int getHealth() { return health; }
		public int getNominalHealth() { return nominalHealth; }
		public int getUnknownDelta() { return unknownDelta; }
		public int getUnknownEpsilon() { return unknownEpsilon; }

		public void setSelectionRectVisible( boolean b ) { selectionRectVisible = b; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();
			int w = this.getWidth(), h = this.getHeight();

			if ( doorCoord.v == 0 ) {  // Use rotated coordinates to draw AS IF vertical.
				g2d.rotate( Math.toRadians(90) );   // Clockwise.
				w = this.getHeight(); h = this.getWidth();
				g2d.translate( 0, -h );
			}

			BufferedImage doorImage;
			if ( open )
				doorImage = openImages[level];
			else
				doorImage = closedImages[level];

			g2d.drawImage( doorImage, 0, 0, this);

			if ( selectionRectVisible ) {
				g2d.setColor( validColor );
				g2d.drawRect( 0, 0, w-1, h-1 );
				g2d.drawRect( 1, 1, (w-1)-2, (h-1)-2 );
				g2d.drawRect( 2, 2, (w-1)-4, (h-1)-4 );
			}

			g2d.setColor( prevColor );
		}
	}



	public class CrewSprite extends JComponent {
		private BufferedImage crewImage;

		private String name;
		private String race;
		private boolean enemyBoardingDrone;
		private int health;
		private boolean playerControlled;
		private int unknownAlpha;
		private int unknownBeta;
		private List<Integer> spriteTintIndeces;
		private boolean mindControlled;
		private int savedRoomId;
		private int savedRoomSquare;
		private int pilotSkill, engineSkill, shieldSkill;
		private int weaponSkill, repairSkill, combatSkill;
		private boolean male;
		private int repairs, combatKills, pilotedEvasions;
		private int jumpsSurvived;
		private int skillMasteries;
		private int stunTicks;
		private int healthBoost;
		private int unknownIota;
		private int unknownKappa;
		private int unknownLambda;
		private int unknownMu;
		private int unknownNu;
		private int unknownXi;
		private int unknownOmicron;
		private int teleportAnimFrame;
		private int unknownRho;
		private int unknownSigma;
		private int unknownTau;
		private int unknownUpsilon;
		private int unknownPhi;
		private int lockdownRechargeTicks;
		private int lockdownRechargeTicksGoal;
		private int unknownOmega;
		private int spriteX;
		private int spriteY;
		private int roomId;
		private int squareId;

		public CrewSprite( SavedGameParser.CrewState crewState ) {
			name = crewState.getName();
			race = crewState.getRace();
			enemyBoardingDrone = crewState.isEnemyBoardingDrone();
			health = crewState.getHealth();
			playerControlled = crewState.isPlayerControlled();
			unknownAlpha = crewState.getUnknownAlpha();
			unknownBeta = crewState.getUnknownBeta();

			spriteTintIndeces = new ArrayList<Integer>( crewState.getSpriteTintIndeces().size() );
			for ( Integer colorIndex : crewState.getSpriteTintIndeces() ) {
				spriteTintIndeces.add( new Integer( colorIndex ) );
			}

			mindControlled = crewState.isMindControlled();
			savedRoomId = crewState.getSavedRoomId();
			savedRoomSquare = crewState.getSavedRoomSquare();
			pilotSkill = crewState.getPilotSkill();
			engineSkill = crewState.getEngineSkill();
			shieldSkill = crewState.getShieldSkill();
			weaponSkill = crewState.getWeaponSkill();
			repairSkill = crewState.getRepairSkill();
			combatSkill = crewState.getCombatSkill();
			male = crewState.isMale();
			repairs = crewState.getRepairs();
			combatKills = crewState.getCombatKills();
			pilotedEvasions = crewState.getPilotedEvasions();
			jumpsSurvived = crewState.getJumpsSurvived();
			skillMasteries = crewState.getSkillMasteries();
			stunTicks = crewState.getStunTicks();
			healthBoost = crewState.getHealthBoost();
			unknownIota = crewState.getUnknownIota();
			unknownKappa = crewState.getUnknownKappa();
			unknownLambda = crewState.getUnknownLambda();
			unknownMu = crewState.getUnknownMu();
			unknownNu = crewState.getUnknownNu();
			unknownXi = crewState.getUnknownXi();
			unknownOmicron = crewState.getUnknownOmicron();
			teleportAnimFrame = crewState.getTeleportAnimFrame();
			unknownRho = crewState.getUnknownRho();
			unknownSigma = crewState.getUnknownSigma();
			unknownTau = crewState.getUnknownTau();
			unknownUpsilon = crewState.getUnknownUpsilon();
			unknownPhi = crewState.getUnknownPhi();
			lockdownRechargeTicks = crewState.getLockdownRechargeTicks();
			lockdownRechargeTicksGoal = crewState.getLockdownRechargeTicksGoal();
			unknownOmega = crewState.getUnknownOmega();

			spriteX = crewState.getSpriteX();
			spriteY = crewState.getSpriteY();
			roomId = crewState.getRoomId();
			squareId = crewState.getRoomSquare();

			makeSane();
			this.setOpaque(false);
		}

		public void setName( String s ) { name = s; }
		public void setRace( String s ) { race = s; }
		public void setEnemyBoardingDrone( boolean b ) { enemyBoardingDrone = b; }
		public void setHealth( int n ) {health = n; }
		public void setPlayerControlled( boolean b ) { playerControlled = b; }
		public void setUnknownAlpha( int n ) { unknownAlpha = n; }
		public void setUnknownBeta( int n ) { unknownBeta = n; }

		public String getName() { return name; }
		public String getRace() { return race; }
		public boolean isEnemyBoardingDrone() { return enemyBoardingDrone; }
		public int getHealth() { return health; }
		public boolean isPlayerControlled() { return playerControlled; }
		public int getUnknownAlpha() { return unknownAlpha; }
		public int getUnknownBeta() { return unknownBeta; }

		public void setSpriteTintIndeces( List<Integer> indeces ) {
			spriteTintIndeces = indeces;
		}
		public List<Integer> getSpriteTintIndeces() {
			return spriteTintIndeces;
		}

		public void setMindControlled( boolean b ) { mindControlled = b; }
		public boolean isMindControlled() { return mindControlled; }

		public void setSavedRoomId( int n ) { savedRoomId = n; }
		public int getSavedRoomId() { return savedRoomId; }
		public void setSavedRoomSquare( int n ) { savedRoomSquare = n; }
		public int getSavedRoomSquare() { return savedRoomSquare; }

		public void setPilotSkill( int n ) {pilotSkill = n; }
		public void setEngineSkill( int n ) {engineSkill = n; }
		public void setShieldSkill( int n ) {shieldSkill = n; }
		public void setWeaponSkill( int n ) {weaponSkill = n; }
		public void setRepairSkill( int n ) {repairSkill = n; }
		public void setCombatSkill( int n ) {combatSkill = n; }
		public void setMale( boolean b ) { male = b; }
		public void setRepairs( int n ) { repairs = n; }
		public void setCombatKills( int n ) { combatKills = n; }
		public void setPilotedEvasions( int n ) { pilotedEvasions = n; }
		public void setJumpsSurvived( int n ) { jumpsSurvived = n; }
		public void setSkillMasteries( int n ) { skillMasteries = n; }

		public int getPilotSkill() { return pilotSkill; }
		public int getEngineSkill() { return engineSkill; }
		public int getShieldSkill() { return shieldSkill; }
		public int getWeaponSkill() { return weaponSkill; }
		public int getRepairSkill() { return repairSkill; }
		public int getCombatSkill() { return combatSkill; }
		public boolean isMale() { return male; }
		public int getRepairs() { return repairs; }
		public int getCombatKills() { return combatKills; }
		public int getPilotedEvasions() { return pilotedEvasions; }
		public int getJumpsSurvived() { return jumpsSurvived; }
		public int getSkillMasteries() { return skillMasteries; }

		public void setStunTicks( int n ) { stunTicks = n; }
		public void setHealthBoost( int n ) { healthBoost = n; }
		public void setUnknownIota( int n ) { unknownIota = n; }
		public void setUnknownKappa( int n ) { unknownKappa = n; }
		public void setUnknownLambda( int n ) { unknownLambda = n; }
		public void setUnknownMu( int n ) { unknownMu = n; }
		public void setUnknownNu( int n ) { unknownNu = n; }
		public void setUnknownXi( int n ) { unknownXi = n; }
		public void setUnknownOmicron( int n ) { unknownOmicron = n; }
		public void setTeleportAnimFrame( int n ) { teleportAnimFrame = n; }
		public void setUnknownRho( int n ) { unknownRho = n; }
		public void setUnknownSigma( int n ) { unknownSigma = n; }
		public void setUnknownTau( int n ) { unknownTau = n; }
		public void setUnknownUpsilon( int n ) { unknownUpsilon = n; }
		public void setUnknownPhi( int n ) { unknownPhi = n; }
		public void setLockdownRechargeTicks( int n ) { lockdownRechargeTicks = n; }
		public void setLockdownRechargeTicksGoal( int n ) { lockdownRechargeTicksGoal = n; }
		public void setUnknownOmega( int n ) { unknownOmega = n; }

		public int getStunTicks() { return stunTicks; }
		public int getHealthBoost() { return healthBoost; }
		public int getUnknownIota() { return unknownIota; }
		public int getUnknownKappa() { return unknownKappa; }
		public int getUnknownLambda() { return unknownLambda; }
		public int getUnknownMu() { return unknownMu; }
		public int getUnknownNu() { return unknownNu; }
		public int getUnknownXi() { return unknownXi; }
		public int getUnknownOmicron() { return unknownOmicron; }
		public int getTeleportAnimFrame() { return teleportAnimFrame; }
		public int getUnknownRho() { return unknownRho; }
		public int getUnknownSigma() { return unknownSigma; }
		public int getUnknownTau() { return unknownTau; }
		public int getUnknownUpsilon() { return unknownUpsilon; }
		public int getUnknownPhi() { return unknownPhi; }
		public int getLockdownRechargeTicks() { return lockdownRechargeTicks; }
		public int getLockdownRechargeTicksGoal() { return lockdownRechargeTicksGoal; }
		public int getUnknownOmega() { return unknownOmega; }

		public void setSpriteX( int n ) { spriteX = n; }
		public void setSpriteY( int n ) { spriteY = n; }
		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }

		public int getSpriteX() { return spriteX; }
		public int getSpriteY() { return spriteY; }
		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }

		public int getImageWidth() { return crewImage.getWidth(); }
		public int getImageHeight() { return crewImage.getHeight(); }

		public void makeSane() {
			if ( isEnemyBoardingDrone() && !CrewType.BATTLE.getId().equals(getRace()) )
				setRace( "battle" );              // The game would do this when loaded.

			if ( isEnemyBoardingDrone() && !getName().equals("Anti-Personnel Drone") )
				setName("Anti-Personnel Drone");  // The game would do this when loaded.

			if ( CrewType.BATTLE.getId().equals(getRace()) && !isEnemyBoardingDrone() )
				setRace( CrewType.HUMAN.getId() );  // The game would do this when loaded.

			// Always same size: no repositioning needed to align image's center with the square's.
			int offsetX = 0, offsetY = 0, w = 35, h = 35;
			String imgRace = race;
			String suffix = "";
			Tint tint = null;

			if ( CrewType.BATTLE.getId().equals(getRace()) ) {
				suffix = "_enemy_sheet";
				// As of 1.01, "sheet" vs "enemy_sheet" images were available.
				// As of 1.03.1, "player" vs "enemy_red" images were also available
			}
			else {
				if ( CrewType.HUMAN.getId().equals(getRace()) ) {
					// Human females have a distinct sprite (Other races look the same either way).
					if ( !isMale() )
						imgRace = "female";  // Not an actual race.
				}
				else if ( CrewType.GHOST.getId().equals(getRace()) ) {
					// Ghosts look like translucent humans.
					if ( isMale() )
						imgRace = "human";
					else
						imgRace = "female";

					// Not an exact color match, but close enough.
					tint = new Tint( new float[] { 1f, 1f, 1f, 0.6f }, new float[] { 0, 0, 0, 0 } );
				}

				if ( isPlayerControlled() ) {
					suffix = "_player_yellow";
				} else {
					suffix = "_enemy_red";
				}
			}
			crewImage = getBodyImage( imgRace, isPlayerControlled() );
			if ( tint != null )
				crewImage = getTintedImage( crewImage, tint );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( crewImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}

		@Override
		public String toString() {
			return String.format("%s (%s, %d HP)", name, race, health);
		}
	}



	/**
	 * A glasspane-like layer to select squares.
	 *
	 * Usage: setVisible(), setCriteria(), setCallback()
	 * To cancel selection, call reset();
	 */
	public class SquareSelector extends JComponent {
		private SquareCriteria defaultCriteria = new SquareCriteria();

		private HashMap<Rectangle, Integer> roomRegions;
		private HashMap<Rectangle, Integer> squareRegions;
		private SquareCriteria squareCriteria = defaultCriteria;
		private SquareSelectionCallback callback = null;
		private Point mousePoint = new Point( -1, -1 );
		private Rectangle currentRect = null;
		private boolean paintDescription = false;

		public SquareSelector( HashMap<Rectangle, Integer> roomRegions, HashMap<Rectangle, Integer> squareRegions ) {
			this.roomRegions = roomRegions;
			this.squareRegions = squareRegions;
		}

		public void setMousePoint( int x, int y ) {
			if ( mousePoint.x != x || mousePoint.y != y) {
				mousePoint.x = x;
				mousePoint.y = y;

				Rectangle newRect = null;
				if ( mousePoint.x > 0 && mousePoint.y > 0 ) {
					for ( Map.Entry<Rectangle, Integer> entry : squareRegions.entrySet() ) {
						if ( entry.getKey().contains( mousePoint ) ) {
							newRect = entry.getKey();
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

		public int getRoomId() {
			int roomId = -1;
			if ( roomRegions.containsKey( currentRect ) )
				roomId = roomRegions.get( currentRect ).intValue();

			return roomId;
		}

		public int getSquareId() {
			int squareId = -1;
			if ( squareRegions.containsKey( currentRect ) )
				squareId = squareRegions.get( currentRect ).intValue();

			return squareId;
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
			if (sc != null)
				squareCriteria = sc;
			else
				squareCriteria = defaultCriteria;
		}

		public SquareCriteria getCriteria() { return squareCriteria; }

		public boolean isCurrentSquareValid() {
			return squareCriteria.isSquareValid( this, getRoomId(), getSquareId() );
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
			this.setVisible(false);
			setDescriptionVisible(false);
			setCriteria(null);
			setCallback(null);
			setMousePoint( -1, -1 );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();

			if ( paintDescription && squareCriteria != null ) {
				String desc = squareCriteria.getDescription();
				if ( desc != null ) {
					LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics(desc, g2d);
					int descHeight = (int)lineMetrics.getAscent() + (int)lineMetrics.getDescent();
					int descX = 8;
					int descY = descHeight + 6;
					g2d.setColor( Color.BLACK );
					g2d.drawString( desc, descX, descY );
				}
			}

			if ( currentRect != null ) {
				Color squareColor = squareCriteria.getSquareColor( this, getRoomId(), getSquareId() );
				if ( squareColor != null ) {
					g2d.setColor( squareColor );
					g2d.drawRect( currentRect.x, currentRect.y, (currentRect.width-1), (currentRect.height-1) );
					g2d.drawRect( currentRect.x+1, currentRect.y+1, (currentRect.width-1)-2, (currentRect.height-1)-2 );
					g2d.drawRect( currentRect.x+2, currentRect.y+2, (currentRect.width-1)-4, (currentRect.height-1)-4 );
				}
			}

			g2d.setColor( prevColor );
		}
	}



	public class SquareCriteria {
		private Color validColor = Color.GREEN.darker();
		private Color invalidColor = Color.RED.darker();

		/** Returns a message describing what will be selected. */
		public String getDescription() {
			return null;
		}

		/** Returns a highlight color when hovering over a square, or null for none. */
		public Color getSquareColor( SquareSelector squareSelector, int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return null;
			if ( isSquareValid(squareSelector, roomId, squareId) )
				return validColor;
			else
				return invalidColor;
		}

		/** Returns true if a square can be selected, false otherwise. */
		public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
			if ( roomId < 0 || squareId < 0 ) return false;
			return true;
		}
	}



	public interface SquareSelectionCallback {
		/** Responds to a clicked square, returning true to continue selecting. */
		public boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId );
	}



	private class Tint {
		public float[] scaleFactors;
		public float[] offsets;

		public Tint( float[] scaleFactors, float[] offsets ) {
			this.scaleFactors = scaleFactors;
			this.offsets = offsets;
		}

		public boolean equals( Object o ) {
			if ( o == this ) return true;
			if ( o instanceof Tint ) return this.hashCode() == o.hashCode();
			return false;
		}

		public int hashCode() {
			return ( java.util.Arrays.hashCode(scaleFactors) ^ java.util.Arrays.hashCode(offsets) );
		}
	}
}
