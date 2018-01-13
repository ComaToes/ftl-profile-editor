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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.constants.OriginalFTLConstants;
import net.blerf.ftl.model.ShipLayout;
import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;
import net.blerf.ftl.model.XYPair;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.BatteryInfo;
import net.blerf.ftl.parser.SavedGameParser.CloakingInfo;
import net.blerf.ftl.parser.SavedGameParser.ClonebayInfo;
import net.blerf.ftl.parser.SavedGameParser.CrewState;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.parser.SavedGameParser.DronePodState;
import net.blerf.ftl.parser.SavedGameParser.DroneState;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.ExtendedSystemInfo;
import net.blerf.ftl.parser.SavedGameParser.RoomState;
import net.blerf.ftl.parser.SavedGameParser.ShieldsInfo;
import net.blerf.ftl.parser.SavedGameParser.SquareState;
import net.blerf.ftl.parser.SavedGameParser.StationDirection;
import net.blerf.ftl.parser.SavedGameParser.SystemState;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.parser.SavedGameParser.WeaponState;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.ImageUtilities;
import net.blerf.ftl.ui.ImageUtilities.Tint;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.RegexDocument;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.StatusViewport;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.CrewNameList;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Offset;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.ShipChassis;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;


public class SavedGameFloorplanPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger( SavedGameFloorplanPanel.class );

	private static final Integer WEAPON_LAYER = 5;
	private static final Integer BASE_LAYER = 10;
	private static final Integer FLOOR_LAYER = 11;
	private static final Integer ROOM_LAYER = 12;
	private static final Integer DECOR_LAYER = 13;
	private static final Integer WALL_LAYER = 15;
	private static final Integer SYSTEM_LAYER = 16;
	private static final Integer BREACH_LAYER = 17;
	private static final Integer FIRE_LAYER = 18;
	private static final Integer CREW_LAYER = 19;
	private static final Integer DOOR_LAYER = 20;
	private static final Integer DRONE_LAYER = 21;
	private static final Integer DEFAULT_SELECTION_LAYER = 50;
	private static final Integer MISC_SELECTION_LAYER = 55;
	private static final Integer SQUARE_SELECTION_LAYER = 60;
	private static final int squareSize = 35;
	private static final int tileEdge = 1;
	private static final int jambLength = 5;

	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private GraphicsDevice gs = ge.getDefaultScreenDevice();
	private GraphicsConfiguration gc = gs.getDefaultConfiguration();

	private FTLFrame frame;

	private FTLConstants ftlConstants = new OriginalFTLConstants();
	private ShipBlueprint shipBlueprint = null;
	private ShipLayout shipLayout = null;
	private ShipChassis shipChassis = null;
	private String shipGfxBaseName = null;
	private int shipReserveCapacity = 0;
	private String shipName = null;
	private int shipHull = 0;
	private int shipFuel = 0;
	private int shipDroneParts = 0;
	private int shipMissiles = 0;
	private int shipScrap = 0;
	private boolean shipHostile = false;
	private int shipJumpChargeTicks = 0;
	private boolean shipJumping = false;
	private int shipJumpAnimTicks = 0;
	private int shipCloakAnimTicks = 0;
	private boolean shipPlayerControlled = false;
	private List<String> shipAugmentIdList = new ArrayList<String>();
	private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<ExtendedSystemInfo>();

	private int originX = 0, originY = 0;
	private int layoutX = 0, layoutY = 0;
	private Map<Rectangle, Integer> roomRegionRoomIdMap = new HashMap<Rectangle, Integer>();
	private Map<Rectangle, Integer> squareRegionRoomIdMap = new HashMap<Rectangle, Integer>();
	private Map<Rectangle, Integer> squareRegionSquareIdMap = new HashMap<Rectangle, Integer>();
	private List<Rectangle> blockedRegions = new ArrayList<Rectangle>();
	private List<JComponent> roomDecorations = new ArrayList<JComponent>();

	private List<SpriteReference<DroneState>> droneRefs = new ArrayList<SpriteReference<DroneState>>();
	private List<SpriteReference<WeaponState>> weaponRefs = new ArrayList<SpriteReference<WeaponState>>();
	private List<SpriteReference<RoomState>> roomRefs = new ArrayList<SpriteReference<RoomState>>();
	private List<SpriteReference<SystemState>> systemRefs = new ArrayList<SpriteReference<SystemState>>();
	private List<SpriteReference<DoorState>> doorRefs = new ArrayList<SpriteReference<DoorState>>();
	private List<SpriteReference<CrewState>> crewRefs = new ArrayList<SpriteReference<CrewState>>();

	private List<DroneBoxSprite> droneBoxSprites = new ArrayList<DroneBoxSprite>();
	private List<DroneBodySprite> droneBodySprites = new ArrayList<DroneBodySprite>();
	private List<WeaponSprite> weaponSprites = new ArrayList<WeaponSprite>();
	private List<RoomSprite> roomSprites = new ArrayList<RoomSprite>();
	private List<SystemRoomSprite> systemRoomSprites = new ArrayList<SystemRoomSprite>();
	private List<BreachSprite> breachSprites = new ArrayList<BreachSprite>();
	private List<FireSprite> fireSprites = new ArrayList<FireSprite>();
	private List<DoorSprite> doorSprites = new ArrayList<DoorSprite>();
	private List<CrewSprite> crewSprites = new ArrayList<CrewSprite>();

	private Map<String, Map<Rectangle, BufferedImage>> cachedImages = new HashMap<String, Map<Rectangle, BufferedImage>>();
	private Map<BufferedImage, Map<Tint, BufferedImage>> cachedTintedImages = new HashMap<BufferedImage, Map<Tint, BufferedImage>>();
	private Map<String, BufferedImage> cachedPlayerBodyImages = new HashMap<String, BufferedImage>();
	private Map<String, BufferedImage> cachedEnemyBodyImages = new HashMap<String, BufferedImage>();

	private JLayeredPane shipPanel = null;
	private StatusViewport shipViewport = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private JLabel baseLbl = null;
	private JLabel floorLbl = null;
	private JLabel wallLbl = null;
	private SpriteSelector defaultSelector = null;
	private SpriteSelector miscSelector = null;
	private SquareSelector squareSelector = null;



	public SavedGameFloorplanPanel( FTLFrame frame ) {
		super( new BorderLayout() );
		this.frame = frame;

		shipPanel = new JLayeredPane();
		shipPanel.setBackground( new Color( 212, 208, 200 ) );
		shipPanel.setOpaque( true );
		shipPanel.setPreferredSize( new Dimension( 50, 50 ) );

		sidePanel = new JPanel();
		sidePanel.setLayout( new BoxLayout( sidePanel, BoxLayout.Y_AXIS ) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 6 ) );

		baseLbl = new JLabel();
		baseLbl.setOpaque( false );
		baseLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( baseLbl, BASE_LAYER );

		floorLbl = new JLabel();
		floorLbl.setOpaque( false );
		floorLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( floorLbl, FLOOR_LAYER );

		wallLbl = new JLabel();
		wallLbl.setOpaque( false );
		wallLbl.setBounds( 0, 0, 50, 50 );
		shipPanel.add( wallLbl, WALL_LAYER );

		defaultSelector = new SpriteSelector();
		defaultSelector.addSpriteList( droneBoxSprites );
		defaultSelector.addSpriteList( weaponSprites );
		defaultSelector.addSpriteList( doorSprites );
		defaultSelector.setOpaque( false );
		defaultSelector.setBounds( 0, 0, 50, 50 );
		shipPanel.add( defaultSelector, DEFAULT_SELECTION_LAYER );

		MouseInputAdapter defaultListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved( MouseEvent e ) {
				defaultSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked( MouseEvent e ) {
				// Left-click triggers callback. Other buttons cancel.

				if ( e.getButton() == MouseEvent.BUTTON1 ) {
					if ( !defaultSelector.isCurrentSpriteValid() ) return;
					boolean keepSelecting = false;
					SpriteSelectionCallback callback = defaultSelector.getCallback();
					if ( callback != null )
						keepSelecting = callback.spriteSelected( defaultSelector, defaultSelector.getSprite() );
					//if ( keepSelecting == false )
						//defaultSelector.reset();  // Never stop selecting.
					defaultSelector.setMousePoint( -1, -1 );
				}
				else if ( e.getButton() != MouseEvent.NOBUTTON ) {
					//defaultSelector.reset();  // Never stop selecting.
					defaultSelector.setMousePoint( -1, -1 );
				}
			}
			@Override
			public void mouseEntered( MouseEvent e ) {
				//defaultSelector.setDescriptionVisible( true );
				shipViewport.setStatusString( defaultSelector.getCriteria().getDescription() );
			}
			@Override
			public void mouseExited( MouseEvent e ) {
				//defaultSelector.setDescriptionVisible( false );
				shipViewport.setStatusString( null );
				defaultSelector.setMousePoint( -1, -1 );
			}
		};
		defaultSelector.addMouseListener( defaultListener );
		defaultSelector.addMouseMotionListener( defaultListener );

		defaultSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Door, Drone, or Weapon";

			@Override
			public String getDescription() { return desc; }
		});

		defaultSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof DoorSprite ) {
					SpriteReference<DoorState> doorRef = ((DoorSprite)sprite).getReference();
					showDoorEditor( doorRef );
				}
				else if ( sprite instanceof DroneBoxSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Drone editing is not possible yet for Advanced Edition saved games.\n\nHowever, cargo (General tab) and stores (Sector Map tab) can be edited.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						SpriteReference<DroneState> droneRef = ((DroneBoxSprite)sprite).getReference();
						showDroneEditor( droneRef );
					}
				}
				else if ( sprite instanceof WeaponSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Weapon editing is not possible yet for Advanced Edition saved games.\n\nHowever, cargo (General tab) and stores (Sector Map tab) can be edited.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						SpriteReference<WeaponState> weaponRef = ((WeaponSprite)sprite).getReference();
						showWeaponEditor( weaponRef );
					}
				}

				return true;
			}
		});

		miscSelector = new SpriteSelector();
		miscSelector.setOpaque( false );
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
				shipViewport.setStatusString( miscSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
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

		squareSelector = new SquareSelector( squareRegionRoomIdMap, squareRegionSquareIdMap );
		squareSelector.setOpaque( false );
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

		Insets ctrlInsets = new Insets( 3, 4, 3, 4 );

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout( selectPanel, BoxLayout.X_AXIS ) );
		selectPanel.setBorder( BorderFactory.createTitledBorder( "Select" ) );
		final JButton selectRoomBtn = new JButton( "Room" );
		selectRoomBtn.setMargin( ctrlInsets );
		selectPanel.add( selectRoomBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectSystemBtn = new JButton( "System" );
		selectSystemBtn.setMargin( ctrlInsets );
		selectPanel.add( selectSystemBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectCrewBtn = new JButton( "Crew" );
		selectCrewBtn.setMargin( ctrlInsets );
		selectPanel.add( selectCrewBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectBreachBtn = new JButton( "Breach" );
		selectBreachBtn.setMargin( ctrlInsets );
		selectPanel.add( selectBreachBtn );
		selectPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton selectFireBtn = new JButton( "Fire" );
		selectFireBtn.setMargin( ctrlInsets );
		selectPanel.add( selectFireBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout( addPanel, BoxLayout.X_AXIS ) );
		addPanel.setBorder( BorderFactory.createTitledBorder( "Add" ) );
		final JButton addCrewBtn = new JButton( "Crew" );
		addCrewBtn.setMargin( ctrlInsets );
		addPanel.add( addCrewBtn );
		addPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton addBreachBtn = new JButton( "Breach" );
		addBreachBtn.setMargin( ctrlInsets );
		addPanel.add( addBreachBtn );
		addPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton addFireBtn = new JButton( "Fire" );
		addFireBtn.setMargin( ctrlInsets );
		addPanel.add( addFireBtn );

		JPanel resetPanel = new JPanel();
		resetPanel.setLayout( new BoxLayout( resetPanel, BoxLayout.X_AXIS ) );
		resetPanel.setBorder( BorderFactory.createTitledBorder( "Reset" ) );
		final JButton resetOxygenBtn = new JButton( "Oxygen" );
		resetOxygenBtn.setMargin( ctrlInsets );
		resetPanel.add( resetOxygenBtn );
		resetPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton resetSystemsBtn = new JButton( "Systems" );
		resetSystemsBtn.setMargin( ctrlInsets );
		resetPanel.add( resetSystemsBtn );
		resetPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton resetIntrudersBtn = new JButton( "Intruders" );
		resetIntrudersBtn.setMargin( ctrlInsets );
		resetPanel.add( resetIntrudersBtn );
		resetPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton resetBreachesBtn = new JButton( "Breaches" );
		resetBreachesBtn.setMargin( ctrlInsets );
		resetPanel.add( resetBreachesBtn );
		resetPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton resetFiresBtn = new JButton( "Fires" );
		resetFiresBtn.setMargin( ctrlInsets );
		resetPanel.add( resetFiresBtn );

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout( otherPanel, BoxLayout.X_AXIS ) );
		otherPanel.setBorder( BorderFactory.createTitledBorder( "Other" ) );
		final JButton otherGeneralBtn = new JButton( "General" );
		otherGeneralBtn.setMargin( ctrlInsets );
		otherPanel.add( otherGeneralBtn );
		otherPanel.add( Box.createHorizontalStrut( 5 ) );
		final JButton otherAugmentsBtn = new JButton( "Augments" );
		otherAugmentsBtn.setMargin( ctrlInsets );
		otherPanel.add( otherAugmentsBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout( ctrlRowOnePanel, BoxLayout.X_AXIS ) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut( 15 ) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlRowTwoPanel = new JPanel();
		ctrlRowTwoPanel.setLayout( new BoxLayout( ctrlRowTwoPanel, BoxLayout.X_AXIS ) );
		ctrlRowTwoPanel.add( resetPanel );
		ctrlRowTwoPanel.add( Box.createHorizontalStrut( 15 ) );
		ctrlRowTwoPanel.add( otherPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout( ctrlPanel, BoxLayout.Y_AXIS ) );
		ctrlPanel.add( ctrlRowOnePanel );
		ctrlPanel.add( Box.createVerticalStrut( 8 ) );
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
					for ( SpriteReference<RoomState> roomRef : roomRefs ) {
						if ( roomRef.get().getOxygen() != 100 ) {
							roomRef.get().setOxygen( 100 );
							roomRef.fireReferenceChange();
						}
					}
					shipViewport.repaint();
				}
				else if ( source == resetSystemsBtn ) {
					clearSidePanel();
					for ( SpriteReference<SystemState> systemRef : systemRefs ) {
						systemRef.get().setDamagedBars( 0 );
						systemRef.get().setIonizedBars( 0 );
						systemRef.get().setRepairProgress( 0 );
						systemRef.get().setDamageProgress( 0 );
						systemRef.get().setDeionizationTicks( Integer.MIN_VALUE );
						systemRef.get().setTemporaryCapacityCap( 1000 );
						systemRef.get().setTemporaryCapacityLoss( 0 );
						systemRef.get().setTemporaryCapacityDivisor( 1 );
						systemRef.fireReferenceChange();
					}
					shipViewport.repaint();
				}
				else if ( source == resetIntrudersBtn ) {
					clearSidePanel();
					for ( ListIterator<SpriteReference<CrewState>> it = crewRefs.listIterator(); it.hasNext(); ) {
						SpriteReference<CrewState> crewRef = it.next();

						if ( !crewRef.get().isPlayerControlled() ) {
							CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
							shipPanel.remove( crewSprite );
							crewSprites.remove( crewSprite );
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

		resetOxygenBtn.addMouseListener( new StatusbarMouseListener( frame, "Set all rooms' oxygen to 100%." ) );
		resetSystemsBtn.addMouseListener( new StatusbarMouseListener( frame, "Clear all system damage and temporary capacity limits." ) );
		resetIntrudersBtn.addMouseListener( new StatusbarMouseListener( frame, "Remove all NPC crew." ) );
		resetBreachesBtn.addMouseListener( new StatusbarMouseListener( frame, "Remove all breaches." ) );
		resetFiresBtn.addMouseListener( new StatusbarMouseListener( frame, "Remove all fires." ) );

		otherGeneralBtn.addMouseListener( new StatusbarMouseListener( frame, "Edit the ship's name, hull, and supplies." ) );
		otherAugmentsBtn.addMouseListener( new StatusbarMouseListener( frame, "Edit Augments." ) );

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
		shipScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		shipScroll.setViewport( shipViewport );
		shipScroll.setViewportView( shipPanel );
		centerPanel.add( shipScroll, gridC );

		gridC.insets = new Insets( 4, 4, 4, 4 );

		gridC.anchor = GridBagConstraints.CENTER;
		gridC.fill = GridBagConstraints.HORIZONTAL;
		gridC.weightx = 1.0;
		gridC.weighty = 0.0;
		gridC.gridx = 0;
		gridC.gridy++;
		centerPanel.add( ctrlPanel, gridC );

		this.add( centerPanel, BorderLayout.CENTER );

		// As scrollpane resizes, adjust the view's size to fill the viewport.
		// No need for AncestorListener to track tab switching. Event fires even if hidden.
		shipScroll.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized( ComponentEvent e ) {
				fitViewToViewport();
			}
		});

		sideScroll = new JScrollPane( sidePanel );
		sideScroll.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
		sideScroll.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		sideScroll.getVerticalScrollBar().setUnitIncrement( 14 );
		sideScroll.setVisible( false );
		this.add( sideScroll, BorderLayout.EAST );

		fitViewToViewport();
	}

	public void setShipState( SavedGameParser.SavedGameState gameState, SavedGameParser.ShipState shipState ) {
		String prevGfxBaseName = shipGfxBaseName;
		defaultSelector.setVisible( false );
		defaultSelector.setMousePoint( -1, -1 );
		miscSelector.reset();
		squareSelector.reset();
		shipViewport.setStatusString( null );
		clearSidePanel();

		shipAugmentIdList.clear();
		extendedSystemInfoList.clear();

		for ( DroneBoxSprite droneBoxSprite : droneBoxSprites ) {
			shipPanel.remove( droneBoxSprite );
		}
		droneBoxSprites.clear();

		for ( DroneBodySprite droneBodySprite : droneBodySprites ) {
			shipPanel.remove( droneBodySprite );
		}
		droneBodySprites.clear();

		for ( WeaponSprite weaponSprite : weaponSprites )
			shipPanel.remove( weaponSprite );
		weaponSprites.clear();

		for ( RoomSprite roomSprite : roomSprites )
			shipPanel.remove( roomSprite );
		roomSprites.clear();

		for ( SystemRoomSprite systemRoomSprite : systemRoomSprites )
			shipPanel.remove( systemRoomSprite );
		systemRoomSprites.clear();

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

		droneRefs.clear();
		weaponRefs.clear();
		roomRefs.clear();
		systemRefs.clear();
		doorRefs.clear();
		crewRefs.clear();

		if ( gameState == null || shipState == null ) {
			shipBlueprint = null;
			shipLayout = null;
			shipChassis = null;
			shipGfxBaseName = null;
			shipReserveCapacity = 0;
			shipName = null;
			shipHull = 0;
			shipFuel = 0;
			shipDroneParts = 0;
			shipMissiles = 0;
			shipScrap = 0;
			shipHostile = false;
			shipJumpChargeTicks = 0;
			shipJumping = false;
			shipJumpAnimTicks = 0;
			shipCloakAnimTicks = 0;
			shipPlayerControlled = false;
			roomRegionRoomIdMap.clear();
			squareRegionRoomIdMap.clear();
			squareRegionSquareIdMap.clear();
			blockedRegions.clear();
			baseLbl.setIcon( null );
			floorLbl.setIcon( null );

			for ( JComponent roomDecor : roomDecorations )
				shipPanel.remove( roomDecor );
			roomDecorations.clear();

			wallLbl.setIcon( null );

			fitViewToViewport();
			shipPanel.revalidate();
			shipViewport.repaint();
			return;
		}

		if ( gameState.getFileFormat() == 2 ) {
			ftlConstants = new OriginalFTLConstants();
		} else {
			ftlConstants = new AdvancedFTLConstants();
		}

		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );
		shipGfxBaseName = shipState.getShipGraphicsBaseName();
		shipReserveCapacity = shipState.getReservePowerCapacity();
		shipName = shipState.getShipName();
		shipHull = shipState.getHullAmt();
		shipFuel = shipState.getFuelAmt();
		shipDroneParts = shipState.getDronePartsAmt();
		shipMissiles = shipState.getMissilesAmt();
		shipScrap = shipState.getScrapAmt();
		shipHostile = shipState.isHostile();
		shipJumpChargeTicks = shipState.getJumpChargeTicks();
		shipJumping = shipState.isJumping();
		shipJumpAnimTicks = shipState.getJumpAnimTicks();
		shipCloakAnimTicks = shipState.getCloakAnimTicks();
		shipPlayerControlled = ( shipState == gameState.getPlayerShip() );
		shipAugmentIdList.addAll( shipState.getAugmentIdList() );
		ShipBlueprint.SystemList blueprintSystems = shipBlueprint.getSystemList();

		if ( shipPlayerControlled ) {
			originX = 0;
			originY = 0;
		}
		else {
			originX = 100;
			originY = 100;
		}
		originX += shipChassis.getImageBounds().x * -1;  // Allow ship images their negative offsets.
		originY += shipChassis.getImageBounds().y * -1;
		layoutX = originX + shipLayout.getOffsetX() * squareSize;
		layoutY = originY + shipLayout.getOffsetY() * squareSize;

		if ( shipGfxBaseName != prevGfxBaseName ) {
			// Associate graphical regions with roomIds and squareIds.
			roomRegionRoomIdMap.clear();
			squareRegionRoomIdMap.clear();
			squareRegionSquareIdMap.clear();
			for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
				ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
				int squaresH = layoutRoom.squaresH;
				int squaresV = layoutRoom.squaresV;
				int roomX = layoutX + layoutRoom.locationX*squareSize;
				int roomY = layoutY + layoutRoom.locationY*squareSize;

				Rectangle roomRect = new Rectangle( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
				roomRegionRoomIdMap.put( roomRect, i );

				for ( int s=0; s < squaresH * squaresV; s++ ) {
					int squareX = roomX + (s%squaresH)*squareSize;
					int squareY = roomY + (s/squaresH)*squareSize;
					Rectangle squareRect = new Rectangle( squareX, squareY, squareSize, squareSize );
					squareRegionRoomIdMap.put( squareRect, i );
					squareRegionSquareIdMap.put( squareRect, s );
				}
			}
			// Find squares that don't allow crew in them (medbay's slot, same as clonebay).
			// TODO: Enemy ships don't have a blocked slot.
			//   Dunno if that's from being in "autoBlueprints.xml" or non-player controlled.
			//   Commandeer one and find out.
			blockedRegions.clear();
			ShipBlueprint.SystemList.SystemRoom medicalSystem = blueprintSystems.getMedicalRoom();
			if ( medicalSystem != null ) {
				ShipBlueprint.SystemList.RoomSlot medicalSlot = medicalSystem.getSlot();
				int badRoomId = medicalSystem.getRoomId();
				int badSquareId = ftlConstants.getDefaultSystemRoomSlotSquare( SystemType.MEDBAY );

				if ( medicalSlot != null ) {
					badSquareId = medicalSlot.getNumber();
				}

				if ( badSquareId >= 0 ) {  // When -2, there's no blocked square.
					log.trace( String.format( "Found a blocked region: roomId: %2d, squareId: %d", badRoomId, badSquareId ) );

					ShipLayoutRoom layoutRoom = shipLayout.getRoom( badRoomId );
					int squaresH = layoutRoom.squaresH;
					int roomX = layoutX + layoutRoom.locationX*squareSize;
					int roomY = layoutY + layoutRoom.locationY*squareSize;

					int squareX = roomX + (badSquareId%squaresH)*squareSize;
					int squareY = roomY + (badSquareId/squaresH)*squareSize;
					Rectangle squareRect = new Rectangle( squareX, squareY, squareSize, squareSize );
					blockedRegions.add( squareRect );
				}
			}


			// Load the fuselage image.
			baseLbl.setIcon( null );
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
				baseLbl.setIcon( new ImageIcon( baseImage ) );
				baseLbl.setSize( new Dimension( baseImage.getWidth(), baseImage.getHeight() ) );
				baseLbl.setLocation( layoutX + shipChassis.getImageBounds().x, layoutY + shipChassis.getImageBounds().y );
			}
			catch ( FileNotFoundException e ) {
				log.warn( "No ship base image for ("+ shipGfxBaseName +")" );
			}
			catch ( IOException e ) {
				log.error( "Failed to load ship base image ("+ shipGfxBaseName +")", e );
			}
			finally {
				try {if ( in != null ) in.close();}
				catch ( IOException e ) {}
			}

			// Load the interior image.
			floorLbl.setIcon( null );
			floorLbl.setBounds( layoutX + shipChassis.getImageBounds().x, layoutY + shipChassis.getImageBounds().y, 50, 50 );
			try {
				in = DataManager.get().getResourceInputStream( "img/ship/"+ shipGfxBaseName +"_floor.png" );
				BufferedImage floorImage = ImageIO.read( in );
				in.close();
				floorLbl.setIcon( new ImageIcon( floorImage ) );
				floorLbl.setSize( new Dimension( floorImage.getWidth(), floorImage.getHeight() ) );

				if ( shipChassis.getOffsets() != null ) {
					Offset floorOffset = shipChassis.getOffsets().floorOffset;
					if ( floorOffset != null ) {
						floorLbl.setLocation( layoutX + shipChassis.getImageBounds().x + floorOffset.x, layoutY + shipChassis.getImageBounds().y + floorOffset.y );
					}
				}
			}
			catch ( FileNotFoundException e ) {
				log.debug( "No ship floor image for ("+ shipGfxBaseName +")" );
			}
			catch ( IOException e ) {
				log.error( "Failed to load ship floor image ("+ shipGfxBaseName +")", e );
			}
			finally {
				try {if ( in != null ) in.close();}
				catch ( IOException e ) {}
			}

			for ( JComponent roomDecor : roomDecorations )
				shipPanel.remove( roomDecor );
			roomDecorations.clear();
			for ( ShipBlueprint.SystemList.SystemRoom systemRoom : blueprintSystems.getSystemRooms() ) {
				String roomImgPath = systemRoom.getImg();

				int roomId = systemRoom.getRoomId();
				ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
				int squaresH = layoutRoom.squaresH;
				int squaresV = layoutRoom.squaresV;
				int roomX = layoutX + layoutRoom.locationX*squareSize;
				int roomY = layoutY + layoutRoom.locationY*squareSize;

				// TODO: Looks like when medbay omits img, it's "room_medbay.png".

				if ( roomImgPath != null ) {
					// Gotta scale because Zoltan #2's got a tall Doors image for a wide room. :/
					// FTL 1.5.4+ reportedly no longer scales, and even lets decor extend beyond room walls.

					BufferedImage decorImage = ImageUtilities.getScaledImage( "img/ship/interior/"+ roomImgPath +".png", squaresH*squareSize, squaresV*squareSize, cachedImages );
					JLabel decorLbl = new JLabel( new ImageIcon( decorImage ) );
					decorLbl.setOpaque( false );
					decorLbl.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
					roomDecorations.add( decorLbl );
					shipPanel.add( decorLbl, DECOR_LAYER );
				}

				if ( systemRoom == blueprintSystems.getTeleporterRoom() ) {
					for ( int s=0; s < squaresH * squaresV; s++ ) {
						int decorX = roomX + (s%squaresH)*squareSize + squareSize/2;
						int decorY = roomY + (s/squaresH)*squareSize + squareSize/2;

						BufferedImage decorImage = ImageUtilities.getScaledImage( "img/ship/interior/teleporter_off.png", 20, 20, cachedImages );
						JLabel decorLbl = new JLabel( new ImageIcon( decorImage ) );
						decorLbl.setOpaque( false );
						decorLbl.setSize( squaresH*squareSize, squaresV*squareSize );
						placeSprite( decorX, decorY, decorLbl );
						roomDecorations.add( decorLbl );
						shipPanel.add( decorLbl, DECOR_LAYER );
					}
				}
			}

			// Draw walls and floor crevices.
			BufferedImage wallImage = gc.createCompatibleImage( shipChassis.getImageBounds().w, shipChassis.getImageBounds().h, Transparency.BITMASK );
			Graphics2D wallG = wallImage.createGraphics();
			drawWalls( wallG, shipChassis.getImageBounds().x * -1, shipChassis.getImageBounds().y * -1, shipLayout );
			wallG.dispose();
			wallLbl.setIcon( new ImageIcon( wallImage ) );
			wallLbl.setSize( new Dimension( wallImage.getWidth(), wallImage.getHeight() ) );
			wallLbl.setLocation( layoutX + shipChassis.getImageBounds().x, layoutY + shipChassis.getImageBounds().y );
		}

		// Add Drones.
		List<DroneState> droneList = shipState.getDroneList();
		int actualDroneSlots;

		if ( shipBlueprint.getDroneSlots() != null ) {
			int minDroneSlots = shipBlueprint.getDroneSlots().intValue();
			if ( droneList.size() > minDroneSlots ) {
				log.warn( String.format( "Ship state has %d drones, exceeding %d droneSlots on its blueprint", droneList.size(), minDroneSlots ) );
			}
			actualDroneSlots = Math.max( minDroneSlots, droneList.size() );
		}
		else {
			// TODO: Magic number (when null: it's omitted in autoBlueprints.xml).
			// In-game GUI shows 2 or 3.
			int minDroneSlots = 3;
			if ( droneList.size() > minDroneSlots && shipPlayerControlled ) {
				log.warn( String.format( "Ship state has %d drones, exceeding the default %d droneSlots when not set by its blueprint", droneList.size(), minDroneSlots ) );
			}
			actualDroneSlots = Math.max( minDroneSlots, droneList.size() );
		}

		for ( int i=0; i < actualDroneSlots; i++ ) {
			// It's fine if droneState is null. Empty slot.
			SpriteReference<DroneState> droneRef = new SpriteReference<DroneState>( null );
			if ( droneList.size() > i ) droneRef.set( new DroneState( droneList.get( i ) ) );

			droneRefs.add( droneRef );

			int droneBoxX = 100 + i*75;
			int droneBoxY = layoutY + shipChassis.getImageBounds().y + shipChassis.getImageBounds().h;

			addDroneSprite( droneBoxX, droneBoxY, i, droneRef );
		}

		// Add Weapons.
		List<ShipChassis.WeaponMount> weaponMounts = shipChassis.getWeaponMountList();
		List<WeaponState> weaponList = shipState.getWeaponList();

		// TODO: Magic number (when null: it's omitted in "autoBlueprints.xml").
		// In-game GUI shows 3 or 4.
		int blueprintWeaponSlots = 4;
		if ( shipBlueprint.getWeaponSlots() != null ) blueprintWeaponSlots = shipBlueprint.getWeaponSlots();

		// Blueprint may restrict usable chassis mounts.
		int actualWeaponSlots = Math.min( weaponMounts.size(), blueprintWeaponSlots );

		if ( weaponList.size() > weaponMounts.size() || blueprintWeaponSlots > weaponMounts.size() ) {
			log.warn( String.format( "Ship state has %d weapons, the ship blueprint expects %d mounts, the chassis only has %d mounts", weaponList.size(), blueprintWeaponSlots, weaponMounts.size() ) );
		}

		for ( int i=0; i < actualWeaponSlots; i++ ) {
			ShipChassis.WeaponMount weaponMount = null;

			if ( weaponMounts.size() > i ) weaponMount = weaponMounts.get( i );
			if ( weaponMount == null ) continue;  // *shrug* Truncate extra weapons.

			// It's fine if weaponState is null. Empty slot.
			SpriteReference<WeaponState> weaponRef = new SpriteReference<WeaponState>( null );
			if ( weaponList.size() > i ) weaponRef.set( weaponList.get( i ) );

			weaponRefs.add( weaponRef );

			addWeaponSprite( weaponMount, i, weaponRef );
		}

		// Flagship has no Weapons system but it omits weaponSlots, so first 4 mounts are junk.
		if ( shipBlueprint.getSystemList().getWeaponRoom() == null ) {
			for ( WeaponSprite weaponSprite : weaponSprites ) {
				weaponSprite.setVisible( false );
			}
		}

		// TODO: Artillery weaponMounts come after the regular weaponMounts (see "fed_cruiser.xml").
		// Flagship has 4 Artillery.

		// Any more after that are junk (see "rebel_long.xml").

		// Add rooms.
		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomX = layoutX + layoutRoom.locationX*squareSize;
			int roomY = layoutY + layoutRoom.locationY*squareSize;

			int oxygen = shipState.getRoom( i ).getOxygen();

			SpriteReference<RoomState> roomRef = new SpriteReference<RoomState>( new RoomState( shipState.getRoom( i ) ) );
			roomRefs.add( roomRef );

			RoomSprite roomSprite = new RoomSprite( roomRef, i );
			roomSprite.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
			roomSprites.add( roomSprite );
			shipPanel.add( roomSprite, ROOM_LAYER );
		}

		// Add systems.
		for ( SystemType systemType : SystemType.values() ) {
			int[] roomIds = shipBlueprint.getSystemList().getRoomIdBySystemType( systemType );
			if ( roomIds != null ) {
				for ( int roomId : roomIds ) {
					ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
					int squaresH = layoutRoom.squaresH;
					int squaresV = layoutRoom.squaresV;
					int roomX = layoutX + layoutRoom.locationX*squareSize;
					int roomY = layoutY + layoutRoom.locationY*squareSize;

					int systemX = roomX + squaresH*squareSize/2;
					int systemY = roomY + squaresV*squareSize/2;

					SystemState systemState = shipState.getSystem( systemType );
					if ( systemState == null ) break;  // TODO: Support systems that aren't on the shipState.

					SpriteReference<SystemState> systemRef = new SpriteReference<SystemState>( new SystemState( systemState ) );
					systemRefs.add( systemRef );

					SystemRoomSprite systemRoomSprite = new SystemRoomSprite( systemRef );
					systemRoomSprite.setSize( systemRoomSprite.getPreferredSize() );
					systemRoomSprite.setLocation( systemX - systemRoomSprite.getPreferredSize().width/2, systemY - systemRoomSprite.getPreferredSize().height/2);
					systemRoomSprites.add( systemRoomSprite );
					shipPanel.add( systemRoomSprite, SYSTEM_LAYER );
				}
			}
		}

		// Add Extended System Info.
		for ( ExtendedSystemInfo info : shipState.getExtendedSystemInfoList() ) {
			extendedSystemInfoList.add( info.copy() );
		}

		// Add breaches.
		for ( Map.Entry<XYPair, Integer> breachEntry : shipState.getBreachMap().entrySet() ) {
			int breachCoordX = breachEntry.getKey().x - shipLayout.getOffsetX();
			int breachCoordY = breachEntry.getKey().y - shipLayout.getOffsetY();
			int breachX = layoutX + breachCoordX*squareSize + squareSize/2;
			int breachY = layoutY + breachCoordY*squareSize + squareSize/2;

			Rectangle squareRect = null;
			int roomId = -1;
			int squareId = -1;
			for ( Map.Entry<Rectangle, Integer> regionEntry : squareRegionRoomIdMap.entrySet() ) {
				if ( regionEntry.getKey().contains( breachX, breachY ) ) {
					squareRect = regionEntry.getKey();
					roomId = regionEntry.getValue().intValue();
					break;
				}
			}
			if ( squareRegionSquareIdMap.containsKey( squareRect ) ) {
				squareId = squareRegionSquareIdMap.get( squareRect ).intValue();
			}
			addBreachSprite( breachX, breachY, roomId, squareId, breachEntry.getValue().intValue() );
		}

		// Add fires.
		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomX = layoutX + layoutRoom.locationX*squareSize;
			int roomY = layoutY + layoutRoom.locationY*squareSize;

			RoomState roomState = shipState.getRoom( i );
			for ( int s=0; s < squaresH * squaresV; s++ ) {
				int fireHealth = roomState.getSquare( s ).getFireHealth();
				if ( fireHealth > 0 ) {
					int fireX = roomX + (s%squaresH)*squareSize + squareSize/2;
					int fireY = roomY + (s/squaresH)*squareSize + squareSize/2;
					addFireSprite( fireX, fireY, i, s, fireHealth );
				}
			}
		}

		// Add doors.
		int doorLevel = shipState.getSystem(SystemType.DOORS).getCapacity()-1;  // Convert to 0-based.
		if ( doorLevel < 0 ) doorLevel = 0;  // Door subsystem was absent, 0-Capacity.
		for ( Map.Entry<DoorCoordinate, DoorState> entry : shipState.getDoorMap().entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();
			DoorState doorState = entry.getValue();
			int doorX = layoutX + doorCoord.x*squareSize + (doorCoord.v==1 ? 0 : squareSize/2);
			int doorY = layoutY + doorCoord.y*squareSize + (doorCoord.v==1 ? squareSize/2 : 0);

			SpriteReference<DoorState> doorRef = new SpriteReference<DoorState>( new DoorState( doorState ) );
			doorRefs.add( doorRef );

			addDoorSprite( doorX, doorY, doorLevel, doorCoord, doorRef );
		}

		// Add crew.
		// TODO: Add dead crew at their spriteX/spriteY but toggle visibility?
		int hadesX = 100 - (int)(squareSize * 1.5);
		int hadesY = layoutY + shipChassis.getImageBounds().y + shipChassis.getImageBounds().h;

		for ( CrewState crewState : shipState.getCrewList() ) {
			SpriteReference<CrewState> crewRef = new SpriteReference<CrewState>( new CrewState( crewState ) );
			crewRefs.add( crewRef );

			int crewX = 0, crewY = 0;
			int goalX = 0, goalY = 0;

			if ( crewState.getRoomId() != -1 ) {
				crewX = originX + crewState.getSpriteX();
				crewY = originY + crewState.getSpriteY();

				// TODO: Draw lines to goal dots for walking crew?
				ShipLayoutRoom layoutRoom = shipLayout.getRoom( crewState.getRoomId() );
				int squaresH = layoutRoom.squaresH;
				int roomX = layoutX + layoutRoom.locationX*squareSize;
				int roomY = layoutY + layoutRoom.locationY*squareSize;

				goalX = roomX + (crewState.getRoomSquare()%squaresH)*squareSize + squareSize/2;
				goalY = roomY + (crewState.getRoomSquare()/squaresH)*squareSize + squareSize/2;
			}
			else {
				crewX = hadesX;
				crewY = hadesY;
			}
			addCrewSprite( crewX, crewY, crewRef );
		}

		fitViewToViewport();

		defaultSelector.setVisible( true );

		shipPanel.revalidate();
		shipViewport.repaint();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				shipViewport.setViewPosition( new Point( 0, 0 ) );
			}
		});
	}

	public void updateShipState( SavedGameParser.ShipState shipState ) {
		if ( shipState == null ) return;

		shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );

		shipState.setReservePowerCapacity( shipReserveCapacity );

		// General.
		shipState.setShipName( shipName );
		shipState.setHullAmt( shipHull );
		shipState.setFuelAmt( shipFuel );
		shipState.setDronePartsAmt( shipDroneParts );
		shipState.setMissilesAmt( shipMissiles );
		shipState.setScrapAmt( shipScrap );
		shipState.setHostile( shipHostile );
		shipState.setJumpChargeTicks( shipJumpChargeTicks );
		shipState.setJumping( shipJumping );
		shipState.setJumpAnimTicks( shipJumpAnimTicks );
		shipState.setCloakAnimTicks( shipCloakAnimTicks );

		// Augments.
		shipState.getAugmentIdList().clear();
		shipState.getAugmentIdList().addAll( shipAugmentIdList );

		// Drones.
		List<DroneState> droneList = shipState.getDroneList();
		droneList.clear();
		for ( SpriteReference<DroneState> droneRef : droneRefs ) {
			if ( droneRef.get() != null ) {
				DroneState droneState = new DroneState( droneRef.get() );
				droneList.add( droneState );
			}
		}

		// Weapons.
		List<WeaponState> weaponList = shipState.getWeaponList();
		weaponList.clear();
		for ( SpriteReference<WeaponState> weaponRef : weaponRefs ) {
			if ( weaponRef.get() != null ) {
				WeaponState weaponState = new WeaponState( weaponRef.get() );
				weaponList.add( weaponState );
			}
		}

		// Rooms (This must come before Fires to avoid clobbering).
		List<RoomState> roomList = shipState.getRoomList();
		roomList.clear();
		for ( SpriteReference<RoomState> roomRef : roomRefs ) {
			RoomState roomState = new RoomState( roomRef.get() );
			roomList.add( roomState );
		}

		// Systems.
		shipState.getSystemsMap().clear();

		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			SystemState systemState = new SystemState( systemRef.get() );

			shipState.addSystem( systemState );
		}
		// Add omitted systems.
		for ( SystemType systemType : SystemType.values() ) {
			if ( shipState.getSystem( systemType ) == null ) {
				shipState.addSystem( new SystemState( systemType ) );
			}
		}

		// Add Extended System Info.
		List<ExtendedSystemInfo> infoList = shipState.getExtendedSystemInfoList();
		infoList.clear();
		for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
			infoList.add( info.copy() );
		}

		// Breaches.
		Map<XYPair, Integer> breachMap = shipState.getBreachMap();
		breachMap.clear();
		for ( BreachSprite breachSprite : breachSprites ) {
			int roomId = breachSprite.getRoomId();
			int squareId = breachSprite.getSquareId();

			ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
			int squaresH = layoutRoom.squaresH;

			int breachCoordX = layoutRoom.locationX + squareId%squaresH;
			int breachCoordY = layoutRoom.locationY + squareId/squaresH;
			XYPair goofyCoord = new XYPair( shipLayout.getOffsetX() + breachCoordX, shipLayout.getOffsetY() + breachCoordY );
			breachMap.put( goofyCoord, breachSprite.getHealth() );
		}

		// Fires.
		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			RoomState roomState = shipState.getRoom( i );
			for ( SquareState squareState : roomState.getSquareList() ) {
				squareState.setFireHealth( 0 );
			}
		}
		for ( FireSprite fireSprite : fireSprites ) {
			RoomState roomState = shipState.getRoom( fireSprite.getRoomId() );
			SquareState squareState = roomState.getSquareList().get( fireSprite.getSquareId() );
			squareState.setFireHealth( fireSprite.getHealth() );
		}

		// Doors.
		Map<DoorCoordinate, DoorState> shipDoorMap = shipState.getDoorMap();
		shipDoorMap.clear();
		for ( SpriteReference<DoorState> doorRef : doorRefs ) {
			DoorSprite doorSprite = doorRef.getSprite( DoorSprite.class );
			DoorState doorState = new DoorState( doorRef.get() );
			shipDoorMap.put( doorSprite.getCoordinate(), doorState );
		}

		// Crew.
		List<CrewState> crewList = shipState.getCrewList();
		crewList.clear();
		for ( SpriteReference<CrewState> crewRef : crewRefs ) {
			CrewState crewState = new CrewState( crewRef.get() );
			crewList.add( crewState );
		}
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
	 * Note: After fitting, trigger layout and painting:
	 *   shipPanel.revalidate();
	 *   shipViewport.repaint();
	 */
	private void fitViewToViewport() {
		// Calculate needed dimensions for all non-selector components.

		int neededWidth = 0, neededHaight = 0;
		for ( Component c : shipPanel.getComponents() ) {
			if ( c == defaultSelector || c == miscSelector || c == squareSelector ) continue;

			neededWidth = Math.max( c.getX()+c.getWidth(), neededWidth );
			neededHaight = Math.max( c.getY()+c.getHeight(), neededHaight );
		}

		Dimension viewExtents = shipViewport.getExtentSize();
		// Possibly account for scrollbar thickness?

		int desiredWidth = Math.max( viewExtents.width, neededWidth );
		int desiredHeight = Math.max( viewExtents.height, neededHaight );
		shipPanel.setPreferredSize( new Dimension( desiredWidth, desiredHeight ) );

		defaultSelector.setSize( desiredWidth, desiredHeight );
		miscSelector.setSize( desiredWidth, desiredHeight );
		squareSelector.setSize( desiredWidth, desiredHeight );
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
				SpriteReference<RoomState> roomRef = roomRefs.get( roomId );  // Nth room.
				showRoomEditor( roomRef, squareId );
				return true;
			}
		});
		squareSelector.setVisible( true );
	}

	private void selectSystem() {
		if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
			JOptionPane.showMessageDialog( frame, "System editing is not possible yet for Advanced Edition saved games.\n\nHowever, stores (Sector Map tab) can be edited.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
			return;
		}

		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( systemRoomSprites );
		miscSelector.reset();

		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: System";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof SystemRoomSprite ) return true;
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof SystemRoomSprite ) {
					SpriteReference<SystemState> systemRef = ((SystemRoomSprite)sprite).getReference();
					showSystemEditor( systemRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void selectCrew() {
		miscSelector.clearSpriteLists();
		miscSelector.addSpriteList( crewSprites );
		miscSelector.reset();

		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null || !sprite.isVisible() ) return false;
				if ( sprite instanceof CrewSprite ) return true;
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			@Override
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof CrewSprite ) {
					SpriteReference<CrewState> crewRef = ((CrewSprite)sprite).getReference();
					showCrewEditor( crewRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );

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
		squareSelector.setVisible( true );
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
		squareSelector.setVisible( true );
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

				for ( SpriteReference<CrewState> crewRef : crewRefs ) {
					if ( crewRef.get().getRoomId() == roomId && crewRef.get().getRoomSquare() == squareId ) {
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
				CrewState crewState = new CrewState();
				crewState.setHealth( CrewType.getMaxHealth( crewState.getRace() ) );
				crewState.setPlayerControlled( true );
				crewState.setRoomId( roomId );
				crewState.setRoomSquare( squareId );
				crewState.setSpriteX( center.x - originX );
				crewState.setSpriteY( center.y - originY );
				crewState.setSavedRoomId( roomId );
				crewState.setSavedRoomSquare( squareId );
				crewState.setMale( DataManager.get().getCrewSex() );
				crewState.setName( DataManager.get().getCrewName( crewState.isMale() ) );

				SpriteReference<CrewState> crewRef = new SpriteReference<CrewState>( crewState );
				crewRefs.add( crewRef );

				addCrewSprite( center.x, center.y, crewRef );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible( true );
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
		squareSelector.setVisible( true );
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
		squareSelector.setVisible( true );
	}

	private void moveCrew( final SpriteReference<CrewState> mobileRef ) {
		squareSelector.reset();
		squareSelector.setCriteria(new SquareCriteria() {
			private final String desc = "Move: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, int roomId, int squareId ) {
				if ( roomId < 0 || squareId < 0 ) return false;
				if ( blockedRegions.contains( squareSelector.getSquareRectangle() ) ) return false;

				for ( SpriteReference<CrewState> crewRef : crewRefs ) {
					if ( crewRef.get().getRoomId() == roomId && crewRef.get().getRoomSquare() == squareId ) {
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
				CrewSprite mobileSprite = mobileRef.getSprite( CrewSprite.class );

				int oldSpriteRoomId = getSpriteRoomId( mobileSprite );  // Actual sprite location, if walking.

				placeSprite( center.x, center.y, mobileSprite );
				mobileRef.get().setRoomId( roomId );
				mobileRef.get().setRoomSquare( squareId );
				mobileRef.get().setSpriteX( center.x - originX );
				mobileRef.get().setSpriteY( center.y - originY );
				mobileRef.fireReferenceChange();

				return false;
			}
		});
		shipViewport.setStatusString( squareSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
		squareSelector.setVisible( true );
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

		if ( CrewType.BATTLE.getId().equals( imgRace ) ) {
			suffix = "_sheet";

			// All "battle" bodies on a ship are foreign, and unselectable in-game.
			// Hence, no color?
		}
		else if ( DroneType.REPAIR.getId().equals( imgRace ) ) {
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
			result = ImageUtilities.getCroppedImage( innerPath, offsetX, offsetY, w, h, cachedImages );
		}
		else {
			// FTL 1.5.4+
			BufferedImage colorImage = null;
			BufferedImage baseImage = null;

			String colorPath = "img/people/"+ imgRace +"_color.png";
			if ( DataManager.get().hasResourceInputStream( colorPath ) ) {
				colorImage = ImageUtilities.getCroppedImage( colorPath, offsetX, offsetY, w, h, cachedImages );
				float[] yellow = new float[] { 0.957f, 0.859f, 0.184f, 1f };
				float[] red = new float[] { 1.0f, 0.286f, 0.145f, 1f };
				Tint colorTint = new Tint( (playerControlled ? yellow: red), new float[] { 0, 0, 0, 0 } );
				colorImage = ImageUtilities.getTintedImage( colorImage, colorTint, cachedTintedImages );
			}

			String basePath = "img/people/"+ imgRace +"_base.png";
			if ( DataManager.get().hasResourceInputStream( basePath ) ) {
				baseImage = ImageUtilities.getCroppedImage( basePath, offsetX, offsetY, w, h, cachedImages );
			}

			if ( colorImage != null && baseImage != null ) {
				result = gc.createCompatibleImage( w, h, Transparency.TRANSLUCENT );
				Graphics2D g2d = result.createGraphics();
				g2d.drawImage( colorImage, 0, 0, null );
				g2d.drawImage( baseImage, 0, 0, null );
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

	private void addDroneSprite( int centerX, int centerY, int slot, SpriteReference<DroneState> droneRef ) {
		DroneBoxSprite droneBoxSprite = new DroneBoxSprite( droneRef, slot );
		droneBoxSprite.setSize( droneBoxSprite.getPreferredSize() );
		droneBoxSprite.setLocation( centerX - droneBoxSprite.getPreferredSize().width/2, centerY - droneBoxSprite.getPreferredSize().height/2 );
		droneBoxSprites.add( droneBoxSprite );
		shipPanel.add( droneBoxSprite, DRONE_LAYER );

		DroneBodySprite droneBodySprite = new DroneBodySprite( droneRef );
		droneBodySprite.setSize( droneBodySprite.getPreferredSize() );
		// Location?
		droneBodySprites.add( droneBodySprite );
		shipPanel.add( droneBodySprite, DRONE_LAYER );
	}

	private void addWeaponSprite( ShipChassis.WeaponMount weaponMount, int slot, SpriteReference<WeaponState> weaponRef ) {
		WeaponSprite weaponSprite = new WeaponSprite( weaponRef, slot, weaponMount.rotate );

		weaponSprite.setSize( weaponSprite.getPreferredSize() );

		if ( weaponMount.rotate ) {
			// Right of x,y and centered vertically.
			weaponSprite.setLocation( layoutX + shipChassis.getImageBounds().x + weaponMount.x, layoutY + shipChassis.getImageBounds().y + weaponMount.y - weaponSprite.getPreferredSize().height/2 );
		} else {
			// Above x,y and centered horizontally.
			weaponSprite.setLocation( layoutX + shipChassis.getImageBounds().x + weaponMount.x - weaponSprite.getPreferredSize().width/2, layoutY + shipChassis.getImageBounds().y + weaponMount.y - weaponSprite.getPreferredSize().height );
		}
		weaponSprites.add( weaponSprite );
		shipPanel.add( weaponSprite, WEAPON_LAYER );
	}

	private void addDoorSprite( int centerX, int centerY, int level, DoorCoordinate doorCoord, SpriteReference<DoorState> doorRef ) {
		int w = 35, h = 35;
		int chop = 10;       // Chop 10 pixels off the sides for skinny doors.
		int levelCount = 5;  // FTL 1.01-1.03.3 only had 3 Doors system levels. FTL 1.5.4+ had 5.

		// Don't scale the image, but pass negative size to define the fallback dummy image.
		BufferedImage bigImage = ImageUtilities.getScaledImage( "img/effects/door_sheet.png", -1*(5*w), -1*(levelCount*h), cachedImages );

		Map<Integer, BufferedImage> closedImages = new TreeMap<Integer, BufferedImage>();
		Map<Integer, BufferedImage>  openImages = new TreeMap<Integer, BufferedImage>();
		// If the image doesn't have enough rows for all levels, let the rest be null.

		for ( int i=0; i < levelCount && (i+1)*h <= bigImage.getHeight(); i++ ) {
			closedImages.put( i, bigImage.getSubimage( chop, i*h, w-chop*2, h ) );
			openImages.put( i, bigImage.getSubimage( 4*w+chop, i*h, w-chop*2, h ) );
		}

		DoorSprite doorSprite = new DoorSprite( doorRef, closedImages, openImages, level, doorCoord );
		doorSprite.setSize( doorSprite.getPreferredSize() );
		doorSprite.setLocation( centerX - doorSprite.getPreferredSize().width/2, centerY - doorSprite.getPreferredSize().height/2 );
		doorSprites.add( doorSprite );
		shipPanel.add( doorSprite, DOOR_LAYER );
	}

	private void addBreachSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 19, h = 19;

		BufferedImage breachImage = ImageUtilities.getCroppedImage( "img/effects/breach.png", offsetX+6*w, offsetY, w, h, cachedImages );

		BreachSprite breachSprite = new BreachSprite( breachImage, roomId, squareId, health );
		breachSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		breachSprites.add( breachSprite );
		shipPanel.add( breachSprite, BREACH_LAYER );
	}

	private void addFireSprite( int centerX, int centerY, int roomId, int squareId, int health ) {
		int offsetX = 0, offsetY = 0, w = 32, h = 32;

		BufferedImage fireImage = ImageUtilities.getCroppedImage( "img/effects/fire_L1_strip8.png", offsetX, offsetY, w, h, cachedImages );

		FireSprite fireSprite = new FireSprite( fireImage, roomId, squareId, health );
		fireSprite.setBounds( centerX-w/2, centerY-h/2, w, h );
		fireSprites.add( fireSprite );
		shipPanel.add( fireSprite, FIRE_LAYER );
	}

	private void addCrewSprite( int centerX, int centerY, SpriteReference<CrewState> crewRef ) {
		CrewSprite crewSprite = new CrewSprite( crewRef );
		crewSprite.setSize( crewSprite.getPreferredSize() );
		crewSprite.setLocation( centerX - crewSprite.getPreferredSize().width/2, centerY - crewSprite.getPreferredSize().height/2 );
		crewSprites.add( crewSprite );
		shipPanel.add( crewSprite, CREW_LAYER );
	}

	/** Relocates a JComponent within its parent's null layout. */
	private void placeSprite( int centerX, int centerY, JComponent sprite ) {
		Dimension spriteSize = sprite.getSize();
		sprite.setLocation( centerX - spriteSize.width/2, centerY - spriteSize.height/2 );
	}

	/** Draws each room's walls, door openings, and floor crevices. */
	private void drawWalls( Graphics2D wallG, int layoutX, int layoutY, ShipLayout shipLayout ) {

		Color prevColor = wallG.getColor();
		Stroke prevStroke = wallG.getStroke();
		Color floorCrackColor = new Color( 125, 125, 125 );
		Stroke floorCrackStroke = new BasicStroke( 1 );
		Color roomBorderColor = new Color( 15, 15, 15 );
		Stroke roomBorderStroke = new BasicStroke( 4 );
		int fromX, fromY, toX, toY;

		Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
		DoorCoordinate doorCoord = null;
		ShipLayoutDoor layoutDoor = null;

		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomCoordX = layoutRoom.locationX;
			int roomCoordY = layoutRoom.locationY;
			int roomX = layoutX + layoutRoom.locationX*squareSize;
			int roomY = layoutY + layoutRoom.locationY*squareSize;

			// Draw floor lines within rooms.
			wallG.setColor( floorCrackColor );
			wallG.setStroke( floorCrackStroke );
			for ( int n=1; n <= squaresV-1; n++ )  // H lines.
				wallG.drawLine( roomX+1, roomY+n*squareSize, roomX+squaresH*squareSize-1, roomY+n*squareSize );
			for ( int n=1; n <= squaresH-1; n++ )  // V lines.
				wallG.drawLine( roomX+n*squareSize, roomY+1, roomX+n*squareSize, roomY+squaresV*squareSize-1 );

			// Draw borders around rooms.
			for ( int n=1; n <= squaresV; n++ ) {  // V lines.
				// West side.
				fromX = roomX;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX;
				toY = roomY+n*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX, roomCoordY+n-1, 1 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, fromY+jambLength );
					wallG.drawLine( fromX, toY-jambLength, toX, toY );
				}
				else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, toY );
				}

				// East Side.
				fromX = roomX+squaresH*squareSize;
				fromY = roomY+(n-1)*squareSize;
				toX = roomX+squaresH*squareSize;
				toY = roomY+n*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX+squaresH, roomCoordY+n-1, 1 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, fromY+jambLength );
					wallG.drawLine( fromX, toY-jambLength, toX, toY );
				}
				else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, toY );
				}
			}

			wallG.setStroke( roomBorderStroke );
			wallG.setColor( roomBorderColor );
			for ( int n=1; n <= squaresH; n++ ) {  // H lines.
				// North side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY;
				toX = roomX+n*squareSize;
				toY = roomY;
				doorCoord = new DoorCoordinate( roomCoordX+n-1, roomCoordY, 0 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, fromX+jambLength, fromY );
					wallG.drawLine( toX-jambLength, fromY, toX, toY );
				}
				else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, toY );
				}

				// South side.
				fromX = roomX+(n-1)*squareSize;
				fromY = roomY+squaresV*squareSize;
				toX = roomX+n*squareSize;
				toY = roomY+squaresV*squareSize;
				doorCoord = new DoorCoordinate( roomCoordX+n-1, roomCoordY+squaresV, 0 );
				layoutDoor = layoutDoorMap.get( doorCoord );

				if ( layoutDoor != null ) {  // Must be a door there.
					// Draw stubs around door.
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, fromX+jambLength, fromY );
					wallG.drawLine( toX-jambLength, fromY, toX, toY );
				}
				else {
					wallG.setStroke( roomBorderStroke );
					wallG.setColor( roomBorderColor );
					wallG.drawLine( fromX, fromY, toX, toY );
				}
			}
		}
		wallG.setColor( prevColor );
		wallG.setStroke( prevStroke );
	}

	/**
	 * Returns the first extended system info of a given class, or null.
	 */
	private <T extends ExtendedSystemInfo> T getExtendedSystemInfo( Class<T> infoClass ) {
		T result = null;
		for ( ExtendedSystemInfo info : extendedSystemInfoList ) {
			if ( infoClass.isInstance(info) ) {
				result = infoClass.cast(info);
				break;
			}
		}
		return result;
	}

	/**
	 * Returns the first system reference, or null.
	 */
	private SpriteReference<SystemState> getSystemRef( SystemType systemType ) {
		SpriteReference<SystemState> result = null;

		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( systemType.equals( systemRef.get().getSystemType() ) ) {
				result = systemRef;
				break;
			}
		}

		return result;
	}

	/**
	 * Returns the roomId which contains the center of a given sprite, or -1.
	 */
	private int getSpriteRoomId( Component c ) {
		int result = -1;
		int centerX = c.getBounds().x + c.getBounds().width/2;
		int centerY = c.getBounds().y + c.getBounds().height/2;

		for ( Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet() ) {
			if ( regionEntry.getKey().contains( centerX, centerY ) ) {
				result = regionEntry.getValue().intValue();
			}
		}

		return result;
	}

	/**
	 * Returns available reserve power after limits are applied and systems'
	 * demand is subtracted (min 0).
	 *
	 * Note: Plasma storms are not considered, due to limitations in the saved
	 * game format. That info is buried in events and obscured by the random
	 * sector layout seed.
	 *
	 * Overallocation will cause FTL to depower systems in-game.
	 *
	 * @param excludeRef count demand from all systems except one (may be null)
	 */
	private int getReservePool( SpriteReference<SystemState> excludeRef ) {
		int result = shipReserveCapacity;

		int systemsPower = 0;
		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
				// TODO: Check if Battery system is currently being hack-disrupted,
				// then subtract 2.
			}

			if ( systemRef == excludeRef ) continue;

			if ( !systemRef.get().getSystemType().isSubsystem() ) {
				systemsPower += systemRef.get().getPower();
			}
		}
		result -= systemsPower;
		result = Math.max( 0, result );

		return result;
	}

	/**
	 * Returns the total battery power produced by the Battery system.
	 */
	private int getBatteryPoolCapacity() {
		int batteryPoolCapacity = 0;

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batteryInfo != null && batteryInfo.isActive() ) {
			SpriteReference<SystemState> batterySystemRef = getSystemRef( SystemType.BATTERY );
			// This should not be null.
			batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity( batterySystemRef.get().getCapacity() );
		}

		return batteryPoolCapacity;
	}

	/**
	 * Returns available battery power after systems' demand is subtracted
	 * (min 0).
	 *
	 * @param excludeRef count demand from all systems except one (may be null)
	 */
	private int getBatteryPool( SpriteReference<SystemState> excludeRef ) {
		int result = 0;

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batteryInfo != null && batteryInfo.isActive() ) {
			int batteryPoolCapacity = 0;
			int systemsBattery = 0;
			for ( SpriteReference<SystemState> systemRef : systemRefs ) {
				if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
					batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity( systemRef.get().getCapacity() );
				}

				if ( systemRef == excludeRef ) continue;

				if ( !systemRef.get().getSystemType().isSubsystem() ) {
					systemsBattery += systemRef.get().getBatteryPower();
				}
			}
			result = batteryPoolCapacity - systemsBattery;
			result = Math.max( 0, result );
		}

		return result;
	}

	private void updateBatteryPool() {
		SpriteReference<SystemState> batterySystemRef = null;
		int systemsBattery = 0;
		for ( SpriteReference<SystemState> systemRef : systemRefs ) {
			if ( SystemType.BATTERY.equals( systemRef.get().getSystemType() ) ) {
				batterySystemRef = systemRef;
			}

			if ( !systemRef.get().getSystemType().isSubsystem() ) {
				systemsBattery += systemRef.get().getBatteryPower();
			}
		}

		BatteryInfo batteryInfo = getExtendedSystemInfo( BatteryInfo.class );
		if ( batterySystemRef != null && batterySystemRef.get().getCapacity() > 0 ) {
			if ( batteryInfo == null ) {
				batteryInfo = new BatteryInfo();
				extendedSystemInfoList.add( batteryInfo );
			}
			if ( !batteryInfo.isActive() ) {
				batteryInfo.setActive( true );
				batteryInfo.setDischargeTicks( 0 );
			}
			batteryInfo.setUsedBattery( systemsBattery );
		}
		else {
			if ( batteryInfo != null ) {
				batteryInfo.setActive( false );
				batteryInfo.setDischargeTicks( 1000 );
				extendedSystemInfoList.remove( batteryInfo );
			}
		}
	}

	/**
	 * Returns the number of friendly Zoltan crew sprites in a room.
	 */
	private int getRoomZoltanEnergy( int roomId ) {
		if ( roomId < 0 ) return 0;

		int result = 0;
		Rectangle roomRect = null;

		for ( Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet() ) {
			if ( regionEntry.getValue().intValue() == roomId ) {
				roomRect = regionEntry.getKey();
				break;
			}
		}

		if ( roomRect != null ) {
			for ( SpriteReference<CrewState> crewRef : crewRefs ) {
				if ( CrewType.ENERGY.getId().equals( crewRef.get().getRace() ) ) {
					if ( crewRef.get().isPlayerControlled() == shipPlayerControlled ) {
						CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
						int centerX = crewSprite.getX() + crewSprite.getWidth()/2;
						int centerY = crewSprite.getY() + crewSprite.getHeight()/2;
						if ( roomRect.contains( centerX, centerY ) ) {
							result++;
						}
					}
				}
			}
		}

		return result;
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
		addSidePanelSeparator( 4 );

		// Keep the editor from growing and creating gaps around it.
		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		sidePanel.add( editorPanel );

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

	private void showGeneralEditor() {
		final String SHIP_NAME = "Ship Name";
		final String HULL = "Hull";
		final String FUEL = "Fuel";
		final String DRONE_PARTS = "Drone Parts";
		final String MISSILES = "Missiles";
		final String SCRAP = "Scrap";
		final String HOSTILE = "Hostile";
		final String JUMP_CHARGE_TICKS = "Jump Charge Ticks";
		final String JUMPING = "Jumping";
		final String JUMP_ANIM_TICKS = "Jump Anim Ticks";
		final String CLOAK_ANIM_TICKS = "Cloak Anim Ticks";

		String title = "General";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( SHIP_NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.addRow( HULL, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( HULL ).setMaximum( shipBlueprint.getHealth().amount );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( SCRAP, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addBlankRow();
		editorPanel.addRow( HOSTILE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( JUMP_CHARGE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addBlankRow();
		editorPanel.addRow( JUMPING, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( JUMP_ANIM_TICKS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( JUMP_ANIM_TICKS ).setMaximum( 2000 );  // TODO: Magic number.
		editorPanel.addBlankRow();
		editorPanel.addRow( CLOAK_ANIM_TICKS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( CLOAK_ANIM_TICKS ).setMaximum( 500 );  // TODO: Magic number.

		editorPanel.getString( SHIP_NAME ).setText( shipName );
		editorPanel.getSlider( HULL ).setValue( shipHull );
		editorPanel.getInt( FUEL ).setText( ""+shipFuel );
		editorPanel.getInt( DRONE_PARTS ).setText( ""+shipDroneParts );
		editorPanel.getInt( MISSILES ).setText( ""+shipMissiles );
		editorPanel.getInt( SCRAP ).setText( ""+shipScrap );
		editorPanel.getBoolean( HOSTILE ).setSelected( shipHostile );
		editorPanel.getInt( JUMP_CHARGE_TICKS ).setText( ""+shipJumpChargeTicks );
		editorPanel.getBoolean( JUMPING ).setSelected( shipJumping );
		editorPanel.getSlider( JUMP_ANIM_TICKS ).setValue( shipJumpAnimTicks );
		editorPanel.getSlider( CLOAK_ANIM_TICKS ).setValue( shipCloakAnimTicks );

		editorPanel.getBoolean( HOSTILE ).addMouseListener( new StatusbarMouseListener( frame, "Toggle hostile/neutral status (No effect on player ships)." ) );
		editorPanel.getInt( JUMP_CHARGE_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed waiting for the FTL to charge (Counts to 85000)." ) );
		editorPanel.getBoolean( JUMPING ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether the ship is jumping away (No effect on player ships)." ) );
		editorPanel.getSlider( JUMP_ANIM_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed while jumping away (0=Normal to 2000=Gone; No effect on player ships)." ) );
		editorPanel.getSlider( CLOAK_ANIM_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Cloak image visibility (0=Uncloaked to 500=Cloaked)." ) );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String newString;
				newString = editorPanel.getString( SHIP_NAME ).getText();
				if ( newString.length() > 0 ) shipName = newString;

				shipHull = editorPanel.getSlider( HULL ).getValue();

				try { shipFuel = editorPanel.parseInt( FUEL ); }
				catch ( NumberFormatException e ) {}

				try { shipDroneParts = editorPanel.parseInt( DRONE_PARTS ); }
				catch ( NumberFormatException e ) {}

				try { shipMissiles = editorPanel.parseInt( MISSILES ); }
				catch ( NumberFormatException e ) {}

				try { shipScrap = editorPanel.parseInt( SCRAP ); }
				catch ( NumberFormatException e ) {}

				shipHostile = editorPanel.getBoolean( HOSTILE ).isSelected();

				try { shipJumpChargeTicks = editorPanel.parseInt( JUMP_CHARGE_TICKS ); }
				catch ( NumberFormatException e ) {}

				shipJumping = editorPanel.getBoolean( JUMPING ).isSelected();

				shipJumpAnimTicks = editorPanel.getSlider( JUMP_ANIM_TICKS ).getValue();

				shipCloakAnimTicks = editorPanel.getSlider( CLOAK_ANIM_TICKS ).getValue();

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
		editorPanel.getWrappedLabel( DESC ).setRows( 12 );  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel( DESC ).setMinimumSize( new Dimension( 0, editorPanel.getWrappedLabel( DESC ).getPreferredSize().height ) );
		editorPanel.addBlankRow();

		for ( int i=0; i < augSlots.length; i++ ) {
			editorPanel.addRow( augSlots[i], FieldEditorPanel.ContentType.COMBO );

			editorPanel.getCombo( augSlots[i] ).addItem( "" );
			for ( AugBlueprint augBlueprint : allAugmentsMap.values() ) {
				editorPanel.getCombo( augSlots[i] ).addItem( augBlueprint );
			}
			if ( shipAugmentIdList.size() > i ) {
				editorPanel.getCombo( augSlots[i] ).setSelectedItem( allAugmentsMap.get( shipAugmentIdList.get( i ) ) );
			}
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				shipAugmentIdList.clear();

				for ( String augSlot : augSlots ) {
					Object augObj = editorPanel.getCombo( augSlot ).getSelectedItem();
					if ( augObj instanceof AugBlueprint ) {
						shipAugmentIdList.add( ((AugBlueprint)augObj).getId() );
					}
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener augListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				for ( int i=0; i < augSlots.length; i++ ) {
					JComboBox augCombo = editorPanel.getCombo(augSlots[i]);
					if ( source == augCombo ) {
						Object augObj = augCombo.getSelectedItem();
						if ( augObj instanceof AugBlueprint ) {
							editorPanel.getWrappedLabel( DESC ).setText( ((AugBlueprint)augObj).getDescription().getTextValue() );

							if ( ((AugBlueprint)augObj).isStackable() == false ) {
								// Clear other slots' copies of this unique augment.
								for ( int j=0; j < augSlots.length; j++ ) {
									if ( j == i ) continue;
									if ( editorPanel.getCombo(augSlots[j]).getSelectedItem() == augObj )
										editorPanel.getCombo(augSlots[j]).setSelectedItem( "" );
								}
							}
						}
						break;
					}
				}
			}
		};
		for ( String augSlot : augSlots ) {
			editorPanel.getCombo( augSlot ).addActionListener( augListener );
		}

		showSidePanel();
	}

	private void showDroneEditor( final SpriteReference<DroneState> droneRef ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "DroneId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String HEALTH = "Health";
		final String PLAYER_CONTROLLED = "Player Ctrl";

		final Map<String, DroneBlueprint> allDronesMap = DataManager.get().getDrones();

		SpriteReference<SystemState> droneSystemRef = getSystemRef( SystemType.DRONE_CTRL );
		if ( droneSystemRef == null || droneSystemRef.get().getCapacity() == 0 ) {
			JOptionPane.showMessageDialog( frame, "A Drone Control system must be present with capacity > 0 before adding drones.", "System Not Installed", JOptionPane.WARNING_MESSAGE );
			return;
		}

		int otherDronesDemand = 0;
		for ( SpriteReference<DroneState> otherDroneRef : droneRefs ) {
			if ( otherDroneRef != droneRef && otherDroneRef.get() != null && otherDroneRef.get().isArmed() ) {
				otherDronesDemand += allDronesMap.get( otherDroneRef.get().getDroneId() ).getPower();
			}
		}

		DroneBoxSprite droneBoxSprite = droneRef.getSprite( DroneBoxSprite.class );
		int droneSystemCapacity = droneSystemRef.get().getCapacity();
		int droneSystemDamage = droneSystemRef.get().getDamagedBars();
		final int excludedReservePool = getReservePool( droneSystemRef );
		final int excludedBatteryPool = getBatteryPool( droneSystemRef );
		final int zoltanBars = getRoomZoltanEnergy( getSpriteRoomId( droneBoxSprite ) );
		final int availablePower = Math.min( excludedReservePool + excludedBatteryPool + zoltanBars, droneSystemRef.get().getUsableCapacity() ) - otherDronesDemand;

		String title = String.format("Drone %d", droneBoxSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( AVAILABLE_POWER ).setMaximum( shipReserveCapacity );
		editorPanel.getSlider( AVAILABLE_POWER ).setValue( availablePower );
		editorPanel.getSlider( AVAILABLE_POWER ).setEnabled( false );
		editorPanel.addBlankRow();
		editorPanel.addRow( ID, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( DESC, FieldEditorPanel.ContentType.WRAPPED_LABEL );
		editorPanel.getWrappedLabel( DESC ).setRows( 8 );  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel( DESC ).setMinimumSize( new Dimension( 0, editorPanel.getWrappedLabel( DESC ).getPreferredSize().height ) );
		editorPanel.addRow( POWER_REQ, FieldEditorPanel.ContentType.LABEL );
		editorPanel.addRow( ARMED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( ARMED ).setEnabled( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( HEALTH ).setMaximum( 0 );
		editorPanel.addRow( PLAYER_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );

		editorPanel.getCombo( ID ).addItem( "" );
		for ( DroneBlueprint droneBlueprint : allDronesMap.values() ) {
			editorPanel.getCombo( ID ).addItem( droneBlueprint );
		}

		if ( droneRef.get() != null ) {
			DroneBlueprint selectedBlueprint = allDronesMap.get( droneRef.get().getDroneId() );
			boolean armable = (availablePower >= selectedBlueprint.getPower());

			editorPanel.getSlider( AVAILABLE_POWER ).setValue( availablePower - (armable && droneRef.get().isArmed() ? selectedBlueprint.getPower() : 0) );
			editorPanel.getCombo( ID ).setSelectedItem( selectedBlueprint );
			editorPanel.getWrappedLabel( DESC ).setText( selectedBlueprint.getDescription().getTextValue() );
			editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
			editorPanel.getBoolean( ARMED ).setEnabled( armable );
			editorPanel.getBoolean( PLAYER_CONTROLLED ).setSelected( droneRef.get().isPlayerControlled() );
			editorPanel.getSlider( HEALTH ).setMaximum( DroneType.getMaxHealth( selectedBlueprint.getType() ) );
			editorPanel.getSlider( HEALTH ).setValue( droneRef.get().getHealth() );

			if ( armable && droneRef.get().isArmed() ) {
				editorPanel.getBoolean( ARMED ).setSelected( true );
			}

			editorPanel.revalidate();
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object blueprintObj = editorPanel.getCombo( ID ).getSelectedItem();

				if ( blueprintObj instanceof DroneBlueprint ) {
					String droneId = ((DroneBlueprint)blueprintObj).getId();
					if ( droneRef.get() == null ) droneRef.set( new DroneState() );
					droneRef.get().setDroneId( droneId );

					droneRef.get().setArmed( editorPanel.getBoolean( ARMED ).isSelected() );
					droneRef.get().setPlayerControlled( editorPanel.getBoolean( PLAYER_CONTROLLED ).isSelected() );
					droneRef.get().setHealth( editorPanel.getSlider( HEALTH ).getValue() );
				}
				else {
					droneRef.set( null );
				}
				droneRef.fireReferenceChange();

				// Set the Drones system power based on all armed drones.
				SpriteReference<SystemState> droneSystemRef = getSystemRef( SystemType.DRONE_CTRL );
				int usableCapacity = droneSystemRef.get().getUsableCapacity();
				int prevSystemPower = droneSystemRef.get().getPower();
				int prevSystemBattery = droneSystemRef.get().getBatteryPower();
				int totalBars = prevSystemPower + prevSystemBattery + zoltanBars;
				int totalDemand = 0;
				boolean disarming = false;

				// Tally up demand from drones until bars are insufficient, allocate
				// more bars if possible, then disarm the rest.
				for ( SpriteReference<DroneState> otherDroneRef : droneRefs ) {
					if ( otherDroneRef.get() != null && otherDroneRef.get().isArmed() ) {
						int demand = allDronesMap.get( otherDroneRef.get().getDroneId() ).getPower();

						if ( !disarming && totalDemand + demand > usableCapacity ) {
							disarming = true;         // Not enough capacity for it.
						}

						if ( !disarming && totalDemand + demand > totalBars ) {
							int discrepancy = (totalDemand + demand) - totalBars;
							if ( discrepancy <= excludedReservePool + excludedBatteryPool ) {
								totalDemand += demand;  // There is enough spare power to cover it.
							}
							else {
								disarming = true;       // Not enough bars for it.
							}
						}
						else {
							totalDemand += demand;    // The system has enough bars for it already.
						}

						if ( disarming ) {
							otherDroneRef.get().setArmed( false );
							otherDroneRef.fireReferenceChange();
						}
					}
				}
				int newSystemPower = 0;
				int newSystemBattery = 0;
				if ( totalDemand > totalBars ) {
					// Allocate as much reserve power as possible, while keeping existing battery/Zoltan bars.
					newSystemPower = Math.max( 0, Math.min( excludedReservePool, totalDemand - prevSystemBattery - zoltanBars ) );
					// If reserve power alone is inadequate, get the rest from the battery.
					newSystemBattery = Math.max( 0, Math.min( excludedBatteryPool, totalDemand - newSystemPower - zoltanBars ) );
				}
				else if ( totalDemand < totalBars ) {
					// Dallocate excess bars, freeing battery first.
					// (Reallocate without regard to battery.)
					newSystemPower = Math.max( 0, Math.min( excludedReservePool, totalDemand - zoltanBars ) );
					// If reserve power alone is inadequate, get the rest from the battery.
					newSystemBattery = Math.max( 0, Math.min( excludedBatteryPool, totalDemand - newSystemPower - zoltanBars ) );
				}

				droneSystemRef.get().setPower( newSystemPower );
				droneSystemRef.get().setBatteryPower( newSystemBattery );
				droneSystemRef.fireReferenceChange();
				updateBatteryPool();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener droneListener = new ActionListener() {
			private JSlider availablePowerSlider = editorPanel.getSlider( AVAILABLE_POWER );
			private JComboBox idCombo = editorPanel.getCombo( ID );
			private JCheckBox armedCheck = editorPanel.getBoolean( ARMED );
			private JCheckBox playerControlledCheck = editorPanel.getBoolean( PLAYER_CONTROLLED );
			private JSlider healthSlider = editorPanel.getSlider( HEALTH );

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == idCombo ) {
					availablePowerSlider.setValue( availablePower );
					editorPanel.getWrappedLabel( DESC ).setText( "" );
					editorPanel.getLabel( POWER_REQ ).setText( "" );
					armedCheck.setSelected( false );
					armedCheck.setEnabled( false );
					healthSlider.setMaximum( 0 );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof DroneBlueprint ) {
						DroneBlueprint selectedBlueprint = (DroneBlueprint)blueprintObj;
						boolean armable = (availablePower >= selectedBlueprint.getPower());

						editorPanel.getWrappedLabel( DESC ).setText( ""+selectedBlueprint.getDescription() );
						editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
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
		editorPanel.getCombo( ID ).addActionListener( droneListener );
		editorPanel.getBoolean( ARMED ).addActionListener( droneListener );

		addSidePanelSeparator( 8 );
		String notice = ""
			+ "* Available power is remaining reserve power, up to the usable "
			+ "Drone Ctrl system capacity, that could go to this drone.\n"
			+ ""
			+ "* This tool doesn't alter nearby ships. It's unknown what will happen "
			+ "if a boarding drone's armed state is changed (what about the body?).\n"
			+ ""
			+ "* Player Ctrl works as expected while armed, but has the opposite "
			+ "value when disarmed?";
		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showWeaponEditor( final SpriteReference<WeaponState> weaponRef ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "WeaponId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String COOLDOWN_TICKS = "Cooldown Ticks";

		final Map<String, WeaponBlueprint> allWeaponsMap = DataManager.get().getWeapons();

		SpriteReference<SystemState> weaponSystemRef = getSystemRef( SystemType.WEAPONS );
		if ( weaponSystemRef == null || weaponSystemRef.get().getCapacity() == 0 ) {
			JOptionPane.showMessageDialog( frame, "A weapons system must be present with capacity > 0 before adding weapons.", "System Not Installed", JOptionPane.WARNING_MESSAGE );
			return;
		}

		int otherWeaponsDemand = 0;
		for ( SpriteReference<WeaponState> otherWeaponRef : weaponRefs ) {
			if ( otherWeaponRef != weaponRef && otherWeaponRef.get() != null && otherWeaponRef.get().isArmed() ) {
				otherWeaponsDemand += allWeaponsMap.get( otherWeaponRef.get().getWeaponId() ).getPower();
			}
		}

		final int excludedReservePool = getReservePool( weaponSystemRef );
		final int excludedBatteryPool = getBatteryPool( weaponSystemRef );
		final int zoltanBars = getRoomZoltanEnergy( getSpriteRoomId( weaponSystemRef.getSprite( SystemRoomSprite.class ) ) );
		final int availablePower = Math.min( excludedReservePool + excludedBatteryPool + zoltanBars, weaponSystemRef.get().getUsableCapacity() ) - otherWeaponsDemand;

		WeaponSprite weaponSprite = weaponRef.getSprite( WeaponSprite.class );

		String title = String.format("Weapon %d", weaponSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( AVAILABLE_POWER ).setMaximum( shipReserveCapacity );
		editorPanel.getSlider( AVAILABLE_POWER ).setValue( availablePower );
		editorPanel.getSlider( AVAILABLE_POWER ).setEnabled( false );
		editorPanel.addBlankRow();
		editorPanel.addRow( ID, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( DESC, FieldEditorPanel.ContentType.WRAPPED_LABEL );
		editorPanel.getWrappedLabel( DESC ).setRows( 8 );  // Help layoutmanagers calc height.
		editorPanel.getWrappedLabel( DESC ).setMinimumSize( new Dimension( 0, editorPanel.getWrappedLabel( DESC ).getPreferredSize().height ) );
		editorPanel.addRow( POWER_REQ, FieldEditorPanel.ContentType.LABEL );
		editorPanel.addRow( ARMED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( ARMED ).setEnabled( false );
		editorPanel.addRow( COOLDOWN_TICKS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( COOLDOWN_TICKS ).setMaximum( 0 );
		editorPanel.getSlider( COOLDOWN_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Seconds spent cooling down." ) );

		editorPanel.getCombo( ID ).addItem( "" );
		for ( WeaponBlueprint weaponBlueprint : allWeaponsMap.values() )
			editorPanel.getCombo( ID ).addItem(weaponBlueprint);

		if ( weaponRef.get() != null ) {
			WeaponBlueprint selectedBlueprint = allWeaponsMap.get( weaponRef.get().getWeaponId() );
			boolean armable = (availablePower >= selectedBlueprint.getPower());

			editorPanel.getSlider( AVAILABLE_POWER ).setValue( availablePower - (weaponRef.get().isArmed() ? selectedBlueprint.getPower() : 0) );
			editorPanel.getCombo( ID ).setSelectedItem( selectedBlueprint );
			editorPanel.getWrappedLabel( DESC ).setText( selectedBlueprint.getTooltip().getTextValue() );
			editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
			editorPanel.getBoolean( ARMED ).setSelected( (weaponRef.get().isArmed() && armable) );
			editorPanel.getBoolean( ARMED ).setEnabled( armable );
			editorPanel.getSlider( COOLDOWN_TICKS ).setMaximum( selectedBlueprint.getCooldown() );
			editorPanel.getSlider( COOLDOWN_TICKS ).setValue( weaponRef.get().getCooldownTicks() );

			editorPanel.revalidate();
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object blueprintObj = editorPanel.getCombo( ID ).getSelectedItem();

				if ( blueprintObj instanceof WeaponBlueprint ) {
					String weaponId = ((WeaponBlueprint)blueprintObj).getId();
					if ( weaponRef.get() == null ) weaponRef.set( new WeaponState() );
					weaponRef.get().setWeaponId( weaponId );

					weaponRef.get().setArmed( editorPanel.getBoolean( ARMED ).isSelected() );
					weaponRef.get().setCooldownTicks( editorPanel.getSlider( COOLDOWN_TICKS ).getValue() );
				}
				else {
					weaponRef.set( null );
				}
				weaponRef.fireReferenceChange();

				// Set the Weapons system power based on all armed weapons.
				SpriteReference<SystemState> weaponSystemRef = getSystemRef( SystemType.WEAPONS );
				int usableCapacity = weaponSystemRef.get().getUsableCapacity();
				int prevSystemPower = weaponSystemRef.get().getPower();
				int prevSystemBattery = weaponSystemRef.get().getBatteryPower();
				int totalBars = prevSystemPower + prevSystemBattery + zoltanBars;
				int totalDemand = 0;
				boolean disarming = false;

				// Tally up demand from weapons until bars are insufficient, allocate
				// more bars if possible, then disarm the rest.
				for ( SpriteReference<WeaponState> otherWeaponRef : weaponRefs ) {
					if ( otherWeaponRef.get() != null && otherWeaponRef.get().isArmed() ) {
						int demand = allWeaponsMap.get( otherWeaponRef.get().getWeaponId() ).getPower();

						if ( !disarming && totalDemand + demand > usableCapacity ) {
							disarming = true;         // Not enough capacity for it.
						}

						if ( !disarming && totalDemand + demand > totalBars ) {
							int discrepancy = (totalDemand + demand) - totalBars;
							if ( discrepancy <= excludedReservePool + excludedBatteryPool ) {
								totalDemand += demand;  // There is enough spare power to cover it.
							}
							else {
								disarming = true;       // Not enough bars for it.
							}
						}
						else {
							totalDemand += demand;    // The system has enough bars for it already.
						}

						if ( disarming ) {
							otherWeaponRef.get().setArmed( false );
							otherWeaponRef.get().setCooldownTicks( 0 );
							otherWeaponRef.fireReferenceChange();
						}
					}
				}
				int newSystemPower = 0;
				int newSystemBattery = 0;
				if ( totalDemand > totalBars ) {
					// Allocate as much reserve power as possible, while keeping existing battery/Zoltan bars.
					newSystemPower = Math.max( 0, Math.min( excludedReservePool, totalDemand - prevSystemBattery - zoltanBars ) );
					// If reserve power alone is inadequate, get the rest from the battery.
					newSystemBattery = Math.max( 0, Math.min( excludedBatteryPool, totalDemand - newSystemPower - zoltanBars ) );
				}
				else if ( totalDemand < totalBars ) {
					// Dallocate excess bars, freeing battery first.
					// (Reallocate without regard to battery.)
					newSystemPower = Math.max( 0, Math.min( excludedReservePool, totalDemand - zoltanBars ) );
					// If reserve power alone is inadequate, get the rest from the battery.
					newSystemBattery = Math.max( 0, Math.min( excludedBatteryPool, totalDemand - newSystemPower - zoltanBars ) );
				}

				weaponSystemRef.get().setPower( newSystemPower );
				weaponSystemRef.get().setBatteryPower( newSystemBattery );
				weaponSystemRef.fireReferenceChange();
				updateBatteryPool();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener weaponListener = new ActionListener() {
			private JSlider availablePowerSlider = editorPanel.getSlider( AVAILABLE_POWER );
			private JComboBox idCombo = editorPanel.getCombo( ID );
			private JCheckBox armedCheck = editorPanel.getBoolean( ARMED );
			private JSlider cooldownSlider = editorPanel.getSlider( COOLDOWN_TICKS );

			@Override
			public void actionPerformed( ActionEvent e ) {
				Object source = e.getSource();
				if ( source == idCombo ) {
					availablePowerSlider.setValue( availablePower );
					editorPanel.getWrappedLabel( DESC ).setText( "" );
					editorPanel.getLabel( POWER_REQ ).setText( "" );
					armedCheck.setSelected( false );
					armedCheck.setEnabled( false );
					cooldownSlider.setMaximum( 0 );

					Object blueprintObj = idCombo.getSelectedItem();
					if ( blueprintObj instanceof WeaponBlueprint ) {
						WeaponBlueprint selectedBlueprint = (WeaponBlueprint)blueprintObj;
						boolean armable = (availablePower >= selectedBlueprint.getPower());

						editorPanel.getWrappedLabel( DESC ).setText( selectedBlueprint.getTooltip().getTextValue() );
						editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
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
		editorPanel.getCombo( ID ).addActionListener( weaponListener );
		editorPanel.getBoolean( ARMED ).addActionListener( weaponListener );

		addSidePanelSeparator( 8 );
		String notice = ""
			+ "* Available power is reserve/battery/Zoltan bars not already "
			+ "claimed by other systems, minus other armed weapons' power, "
			+ "after applying Weapons system limits and damage.";
		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showRoomEditor( final SpriteReference<RoomState> roomRef, final int squareId ) {
		final String OXYGEN = "Oxygen";
		final String STATION_HERE = "Station Here";
		final String STATION_DIR = "Station Direction";
		final String IGNITION = "Ignition Progress";
		final String EXTINGUISHMENT = "Extinguishment Progress";

		RoomSprite roomSprite = roomRef.getSprite( RoomSprite.class );
		String title = String.format("Room %2d (Square %d)", roomSprite.getRoomId(), squareId);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OXYGEN, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( OXYGEN ).setMaximum( 100 );
		editorPanel.getSlider( OXYGEN ).addMouseListener( new StatusbarMouseListener( frame, "Oxygen level for the room as a whole." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( STATION_HERE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( STATION_HERE ).addMouseListener( new StatusbarMouseListener( frame, "Toggles whether this square has a station for manning a system." ) );
		editorPanel.addRow( STATION_DIR, FieldEditorPanel.ContentType.COMBO );
		editorPanel.getCombo( STATION_DIR ).addMouseListener( new StatusbarMouseListener( frame, "Placement of the station on the square (DOWN means on the bottom edge)." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( IGNITION, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( IGNITION ).setMaximum( 100 );
		editorPanel.getSlider( IGNITION ).addMouseListener( new StatusbarMouseListener( frame, "A new fire spawns in this square at 100." ) );
		editorPanel.addRow( EXTINGUISHMENT, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( EXTINGUISHMENT ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt( EXTINGUISHMENT ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Usually -1. When fire disappears in a puff of smoke, it's 9,8...,1,0." ) );

		editorPanel.getCombo( STATION_DIR ).addItem( StationDirection.DOWN );
		editorPanel.getCombo( STATION_DIR ).addItem( StationDirection.RIGHT );
		editorPanel.getCombo( STATION_DIR ).addItem( StationDirection.UP );
		editorPanel.getCombo( STATION_DIR ).addItem( StationDirection.LEFT );
		// NONE is omitted here, since the combo's disabled when there's no station.

		editorPanel.getSlider( OXYGEN ).setValue( roomRef.get().getOxygen() );

		editorPanel.getBoolean( STATION_HERE ).setSelected( (squareId == roomRef.get().getStationSquare()) );
		if ( squareId == roomRef.get().getStationSquare() ) {
			editorPanel.getCombo( STATION_DIR ).setSelectedItem( roomRef.get().getStationDirection() );
		}

		editorPanel.getSlider( IGNITION ).setValue( roomRef.get().getSquare( squareId ).getIgnitionProgress() );
		editorPanel.getInt( EXTINGUISHMENT ).setText( ""+roomRef.get().getSquare( squareId ).getExtinguishmentProgress() );

		editorPanel.getBoolean( STATION_HERE ).addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged( ItemEvent e ) {
				boolean stationHere = ( e.getStateChange() == ItemEvent.SELECTED );
				editorPanel.getCombo( STATION_DIR ).setEnabled( stationHere );
			}
		});
		editorPanel.getCombo( STATION_DIR ).setEnabled( editorPanel.getBoolean( STATION_HERE ).isSelected() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				roomRef.get().setOxygen( editorPanel.getSlider( OXYGEN ).getValue() );

				boolean stationHere = editorPanel.getBoolean( STATION_HERE ).isSelected();
				if ( !stationHere && roomRef.get().getStationSquare() == squareId ) {
					roomRef.get().setStationSquare( -1 );
					roomRef.get().setStationDirection( StationDirection.NONE );
				}
				else if ( stationHere ) {  // Square and/or dir may have changed.
					roomRef.get().setStationSquare( squareId );
					roomRef.get().setStationDirection( (StationDirection)editorPanel.getCombo( STATION_DIR ).getSelectedItem() );
				}

				roomRef.get().getSquare( squareId ).setIgnitionProgress( editorPanel.getSlider( IGNITION ).getValue() );

				try { roomRef.get().getSquare( squareId ).setExtinguishmentProgress( editorPanel.parseInt( EXTINGUISHMENT ) ); }
				catch ( NumberFormatException e ) {}

				roomRef.fireReferenceChange();
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showSystemEditor( final SpriteReference<SystemState> systemRef ) {
		final String RESERVE_CAPACITY = "Reserve Capacity";
		final String RESERVE_POWER = "Reserve Power";
		final String CAPACITY = "System Capacity";
		final String POWER = "System Power";
		final String BATTERY = "System Battery";
		final String DAMAGED_BARS = "Damaged Bars";
		final String IONIZED_BARS = "Ionized Bars";
		final String REPAIR_PROGRESS = "Repair Progress";
		final String DAMAGE_PROGRESS = "Damage Progress";
		final String DEIONIZATION_TICKS = "Deionization Ticks";

		final SystemBlueprint systemBlueprint = DataManager.get().getSystem( systemRef.get().getSystemType().getId() );

		int maxSystemCapacity = systemBlueprint.getMaxPower();
		Integer maxPowerOverride = shipBlueprint.getSystemList().getSystemRoom( systemRef.get().getSystemType() )[0].getMaxPower();
		if ( maxPowerOverride != null ) {
			maxSystemCapacity = maxPowerOverride.intValue();
		}

		final int batteryCapacity = getBatteryPoolCapacity();
		final int excludedReservePool = getReservePool( systemRef );
		final int excludedBatteryPool = getBatteryPool( systemRef );
		final int zoltanBars = getRoomZoltanEnergy( getSpriteRoomId( systemRef.getSprite( SystemRoomSprite.class ) ) );

		// Subsystems ignore the reserve, and power can't be directly changed.
		final boolean isSubsystem = systemRef.get().getSystemType().isSubsystem();

		String title = systemBlueprint.getTitle().getTextValue();

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( RESERVE_CAPACITY, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( RESERVE_CAPACITY ).addMouseListener( new StatusbarMouseListener( frame, "Total possible reactor bars (Increase to upgrade)." ) );
		editorPanel.addRow( RESERVE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( RESERVE_POWER ).addMouseListener( new StatusbarMouseListener( frame, "Unallocated power." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( CAPACITY, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( CAPACITY ).addMouseListener( new StatusbarMouseListener( frame, "Possible system bars (Increase to buy/upgrade, 0=absent)." ) );
		editorPanel.addRow( POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( POWER ).addMouseListener( new StatusbarMouseListener( frame, "System bars from reserve." ) );
		editorPanel.addRow( BATTERY, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( BATTERY ).addMouseListener( new StatusbarMouseListener( frame, "System bars from battery." ) );
		editorPanel.addRow( DAMAGED_BARS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( DAMAGED_BARS ).addMouseListener( new StatusbarMouseListener( frame, "Completely damaged bars." ) );
		editorPanel.addBlankRow();
		editorPanel.addRow( IONIZED_BARS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( IONIZED_BARS ).setDocument( new RegexDocument("-?1?|[0-9]*") );
		editorPanel.getInt( IONIZED_BARS ).addMouseListener( new StatusbarMouseListener(frame, String.format("Ionized bars (can exceed %d but the number won't appear in-game).", ftlConstants.getMaxIonizedBars())) );
		editorPanel.addBlankRow();
		editorPanel.addRow( REPAIR_PROGRESS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( REPAIR_PROGRESS ).addMouseListener( new StatusbarMouseListener( frame, "Turns a damaged bar yellow until restored." ) );
		editorPanel.addRow( DAMAGE_PROGRESS, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( DAMAGE_PROGRESS ).addMouseListener( new StatusbarMouseListener( frame, "Turns an undamaged bar red until damaged." ) );
		editorPanel.addRow( DEIONIZATION_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( DEIONIZATION_TICKS ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.getInt( DEIONIZATION_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed deionizing a bar: 0-5000 (Resets upon loading, weird values sometimes, -2147...=N/A, 0 is safe)." ) );

		editorPanel.getSlider( RESERVE_CAPACITY ).setMaximum( ftlConstants.getMaxReservePoolCapacity() );
		editorPanel.getSlider( RESERVE_CAPACITY ).setMinimum( ftlConstants.getMaxReservePoolCapacity() - excludedReservePool );  // otherSystemsPower
		editorPanel.getSlider( RESERVE_CAPACITY ).setValue( shipReserveCapacity );
		editorPanel.getSlider( RESERVE_POWER ).setMaximum( shipReserveCapacity );
		// Reserve power's value is set later.
		editorPanel.getSlider( RESERVE_POWER ).setEnabled( false );
		editorPanel.getSlider( CAPACITY ).setMaximum( maxSystemCapacity );
		editorPanel.getSlider( CAPACITY ).setValue( systemRef.get().getCapacity() );
		// Nothing for power here.
		editorPanel.getSlider( DAMAGED_BARS ).setMaximum( systemRef.get().getCapacity() );
		editorPanel.getSlider( DAMAGED_BARS ).setValue( systemRef.get().getDamagedBars() );
		editorPanel.getInt( IONIZED_BARS ).setText( ""+systemRef.get().getIonizedBars() );
		editorPanel.getSlider( REPAIR_PROGRESS ).setMaximum( (systemRef.get().getDamagedBars() == 0 ? 0 : 100) );
		editorPanel.getSlider( REPAIR_PROGRESS ).setValue( systemRef.get().getRepairProgress() );
		editorPanel.getSlider( DAMAGE_PROGRESS ).setMaximum( (systemRef.get().getDamagedBars() >= systemRef.get().getCapacity() ? 0 : 100) );
		editorPanel.getSlider( DAMAGE_PROGRESS ).setValue( systemRef.get().getDamageProgress() );
		editorPanel.getInt( DEIONIZATION_TICKS ).setText( ""+systemRef.get().getDeionizationTicks() );

		if ( isSubsystem ) {
			editorPanel.getSlider( RESERVE_CAPACITY ).setEnabled( false );
			editorPanel.getSlider( RESERVE_POWER ).setValue( excludedReservePool );
			editorPanel.getSlider( POWER ).setMaximum( systemRef.get().getCapacity() );
			editorPanel.getSlider( POWER ).setValue( systemRef.get().getPower() );
			editorPanel.getSlider( POWER ).setEnabled( false );
			editorPanel.getSlider( BATTERY ).setMaximum( excludedBatteryPool );
			editorPanel.getSlider( BATTERY ).setValue( systemRef.get().getBatteryPower() );
			editorPanel.getSlider( BATTERY ).setEnabled( false );
		}
		else {
			editorPanel.getSlider( RESERVE_POWER ).setValue( excludedReservePool - systemRef.get().getPower() );
			editorPanel.getSlider( POWER ).setMaximum(Math.min( systemRef.get().getCapacity(), excludedReservePool ));

			// Trust the power value on Weapons and DroneCtrl systems.
			editorPanel.getSlider( POWER ).setValue( systemRef.get().getPower() );

			editorPanel.getSlider( BATTERY ).setMaximum( excludedBatteryPool );
			editorPanel.getSlider( BATTERY ).setValue( systemRef.get().getBatteryPower() );
		}

		ChangeListener barListener = new ChangeListener() {
			private JSlider reserveCapacitySlider = editorPanel.getSlider( RESERVE_CAPACITY );
			private JSlider reservePowerSlider = editorPanel.getSlider( RESERVE_POWER );
			private JSlider capacitySlider = editorPanel.getSlider( CAPACITY );
			private JSlider powerSlider = editorPanel.getSlider( POWER );
			private JSlider batterySlider = editorPanel.getSlider( BATTERY );
			private JSlider damagedBarsSlider = editorPanel.getSlider( DAMAGED_BARS );
			private JSlider repairProgressSlider = editorPanel.getSlider( REPAIR_PROGRESS );
			private JSlider damageProgressSlider = editorPanel.getSlider( DAMAGE_PROGRESS );
			private boolean ignoreChanges = false;
			// Avoid getValueIsAdjusting() checks, which can fail on brief drags.

			@Override
			public void stateChanged( ChangeEvent e ) {
				if ( ignoreChanges ) return;
				ignoreChanges = true;

				Object source = e.getSource();

				syncBars( source );

				// After all the secondary slider events, resume monitoring.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ignoreChanges = false;
					}
				});
			}

			private void syncBars( Object source ) {
				if ( source == reserveCapacitySlider ) {  // Non-subystem only.
					// Cap power based on reserves.
					int capacity = capacitySlider.getValue();

					powerSlider.setMaximum(Math.min( capacity, excludedReservePool ));
					powerSlider.setValue(Math.min( powerSlider.getValue(), Math.max( 0, excludedReservePool ) ));
					reservePowerSlider.setValue( excludedReservePool - powerSlider.getValue() );
				}
				else if ( source == capacitySlider ) {
					// Set maxes.
					int capacity = capacitySlider.getValue();

					damagedBarsSlider.setMaximum( capacity );
					int damage = damagedBarsSlider.getValue();
					repairProgressSlider.setMaximum( (damage == 0 ? 0 : 100 ) );
					damageProgressSlider.setMaximum( (damage >= capacity ? 0 : 100 ) );

					if ( isSubsystem ) {  // Power ~= Capacity.
						powerSlider.setMaximum( capacity );
						powerSlider.setValue(Math.max( 0, getUsableCapacity() - zoltanBars ));
					}
					else {                // Power merely capped.
						powerSlider.setMaximum(Math.min( capacity, excludedReservePool ));
						reservePowerSlider.setValue( excludedReservePool - powerSlider.getValue() );
					}
				}
				else if ( source == powerSlider ) {  // Non-subystem only.
					batterySlider.setMaximum( Math.max( 0, getUsableCapacity() - powerSlider.getValue() - zoltanBars ) );
					reservePowerSlider.setValue( excludedReservePool - powerSlider.getValue() );
				}
				else if ( source == batterySlider ) {  // Non-subystem only.
				}
				else if ( source == damagedBarsSlider ) {
					// Interfere with Power.
					int capacity = capacitySlider.getValue();
					int damage = damagedBarsSlider.getValue();
					repairProgressSlider.setMaximum( (damage == 0 ? 0 : 100 ) );
					damageProgressSlider.setMaximum( (damage >= capacity ? 0 : 100 ) );

					if ( isSubsystem ) {  // Power ~= Capacity.
						powerSlider.setValue(Math.max( 0, getUsableCapacity() - zoltanBars ));
					}
					else {                // Power merely capped.
						powerSlider.setMaximum( Math.max( 0, getUsableCapacity() - zoltanBars ) );
						batterySlider.setMaximum( Math.max( 0, getUsableCapacity() - powerSlider.getValue() - zoltanBars ) );
						reservePowerSlider.setValue( excludedReservePool - powerSlider.getValue() );
					}
				}
			}

			/**
			 * Returns system capacity after applying limits (min 0).
			 */
			public int getLimitedCapacity() {
				int capacity = capacitySlider.getValue();
				return capacity;  // TODO: This is a stub.

				// TODO: Handle limit GUI better than editable text fields.
			}

			/**
			 * Returns system capacity after applying limits and damage (min 0).
			 */
			public int getUsableCapacity() {
				int capacity = capacitySlider.getValue();
				int damage = damagedBarsSlider.getValue();

				int limit = getLimitedCapacity();
				int usableCapacity = Math.max( 0, Math.min( limit, capacity - damage ) );
				return usableCapacity;
			}
		};
		editorPanel.getSlider( RESERVE_CAPACITY ).addChangeListener( barListener );
		editorPanel.getSlider( CAPACITY ).addChangeListener( barListener );
		editorPanel.getSlider( POWER ).addChangeListener( barListener );
		editorPanel.getSlider( BATTERY ).addChangeListener( barListener );
		editorPanel.getSlider( DAMAGED_BARS ).addChangeListener( barListener );
		editorPanel.getSlider( REPAIR_PROGRESS ).addChangeListener( barListener );
		editorPanel.getSlider( DAMAGE_PROGRESS ).addChangeListener( barListener );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				shipReserveCapacity = editorPanel.getSlider( RESERVE_CAPACITY ).getValue();

				systemRef.get().setCapacity( editorPanel.getSlider( CAPACITY ).getValue() );

				// Do stuff with this below...
				int systemPower = editorPanel.getSlider( POWER ).getValue();
				int systemBattery = editorPanel.getSlider( BATTERY ).getValue();

				systemRef.get().setDamagedBars( editorPanel.getSlider( DAMAGED_BARS ).getValue() );

				try { systemRef.get().setIonizedBars( editorPanel.parseInt( IONIZED_BARS ) ); }
				catch ( NumberFormatException e ) {}

				systemRef.get().setRepairProgress( editorPanel.getSlider( REPAIR_PROGRESS ).getValue() );
				systemRef.get().setDamageProgress( editorPanel.getSlider( DAMAGE_PROGRESS ).getValue() );

				try { systemRef.get().setDeionizationTicks( editorPanel.parseInt( DEIONIZATION_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				if ( SystemType.WEAPONS.equals( systemRef.get().getSystemType() ) ) {
					if ( systemRef.get().getCapacity() == 0 ) {
						// When capacity is 0, nullify all weapons.
						for ( SpriteReference<WeaponState> weaponRef : weaponRefs ) {
							weaponRef.set( null );
							weaponRef.fireReferenceChange();
						}
					}
					else {
						// Disarm everything rightward of first underpowered weapon.
						int weaponPower = 0;
						for ( SpriteReference<WeaponState> weaponRef : weaponRefs ) {
							if ( weaponRef.get() != null && weaponRef.get().isArmed() ) {
								weaponPower += DataManager.get().getWeapon( weaponRef.get().getWeaponId() ).getPower();
								if ( weaponPower > systemPower ) weaponRef.get().setArmed( false );
							}
						}
						// Re-calc system power based on all armed weapons.
						systemPower = 0;
						for ( SpriteReference<WeaponState> weaponRef : weaponRefs ) {
							if ( weaponRef.get() != null && weaponRef.get().isArmed() ) {
								systemPower += DataManager.get().getWeapon( weaponRef.get().getWeaponId() ).getPower();
							}
						}
						systemRef.get().setPower( systemPower );
						systemRef.get().setBatteryPower( systemBattery );
						updateBatteryPool();
					}
				}
				else if ( SystemType.DRONE_CTRL.equals( systemRef.get().getSystemType() ) ) {
					if ( systemRef.get().getCapacity() == 0 ) {
						// When capacity is 0, nullify all drones.
						for ( SpriteReference<DroneState> droneRef : droneRefs ) {
							droneRef.set( null );
							droneRef.fireReferenceChange();
						}
					}
					else {
						// Disarm everything rightward of first underpowered drone.
						int dronePower = 0;
						for ( SpriteReference<DroneState> droneRef : droneRefs ) {
							if ( droneRef.get() != null && droneRef.get().isArmed() ) {
								dronePower += DataManager.get().getDrone( droneRef.get().getDroneId() ).getPower();
								if ( dronePower > systemPower ) {
									droneRef.get().setArmed( false );
									droneRef.fireReferenceChange();
								}
							}
						}
						// Re-calc system power based on all armed drones.
						systemPower = 0;
						for ( SpriteReference<DroneState> droneRef : droneRefs ) {
							if ( droneRef.get() != null && droneRef.get().isArmed() ) {
								systemPower += DataManager.get().getDrone( droneRef.get().getDroneId() ).getPower();
							}
						}
						systemRef.get().setPower( systemPower );
						systemRef.get().setBatteryPower( systemBattery );
						updateBatteryPool();
					}
				}
				else {
					systemRef.get().setPower( systemPower );
					systemRef.get().setBatteryPower( systemBattery );
					updateBatteryPool();
				}
				systemRef.fireReferenceChange();

				if ( SystemType.DOORS.equals( systemRef.get().getSystemType() ) ) {
					for ( SpriteReference<DoorState> doorRef : doorRefs ) {
						// TODO: Test if doorState is ordinary, then change its health values.

						doorRef.fireReferenceChange();
					}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 8 );
		StringBuilder noticeBuf = new StringBuilder();
		if ( isSubsystem ) {
			noticeBuf.append( "* This is a subsystem, which means reserves are ignored, " );
			noticeBuf.append( "and power is always as full as possible.\n\n" );
		}
		if ( SystemType.SHIELDS.equals( systemRef.get().getSystemType() ) ) {
			noticeBuf.append( "* Partialy powered shields will steal an extra bar upon loading, " );
			noticeBuf.append( "from another system if need be.\n\n" );
		}
		else if ( SystemType.WEAPONS.equals( systemRef.get().getSystemType() ) ) {
			noticeBuf.append( "* Power can't be directly changed for the Weapons system. " );
			noticeBuf.append( "Toggle paraphernalia separately. " );
			noticeBuf.append( "If capacity/damage reduce power, " );
			noticeBuf.append( "things will get disarmed.\n\n" );
		}
		else if ( SystemType.DRONE_CTRL.equals( systemRef.get().getSystemType() ) ) {
			noticeBuf.append( "* Power can't be directly changed for the Drone Ctrl system. " );
			noticeBuf.append( "Toggle paraphernalia separately. " );
			noticeBuf.append( "If capacity/damage reduce power, " );
			noticeBuf.append( "things will get disarmed.\n\n" );
		}
		noticeBuf.append( "* Ion -1: in Cloaking initates cloak; " );
		noticeBuf.append( "in Teleporter might not be useful; " );
		noticeBuf.append( "elsewhere sets a locked appearance indefinitely " );
		noticeBuf.append( "until hit with an ion weapon.\n" );

		addSidePanelNote( noticeBuf.toString() );

		showSidePanel();
	}

	private void showBreachEditor( final BreachSprite breachSprite ) {
		final String HEALTH = "Health";

		String title = "Breach";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( HEALTH ).setMaximum( 100 );
		editorPanel.getSlider( HEALTH ).setValue( breachSprite.getHealth() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				breachSprite.setHealth( editorPanel.getSlider( HEALTH ).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
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
		editorPanel.getSlider( HEALTH ).setMaximum( 100 );
		editorPanel.getSlider( HEALTH ).setValue( fireSprite.getHealth() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				fireSprite.setHealth( editorPanel.getSlider( HEALTH ).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton removeBtn = new JButton( "Remove" );
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

	private void showDoorEditor( final SpriteReference<DoorState> doorRef ) {
		final String OPEN = "Open";
		final String WALKING_THROUGH = "Walking Through";
		final String MAX_HEALTH = "Current Max Health?";
		final String HEALTH = "Health";
		final String NOMINAL_HEALTH = "Nominal Health?";
		final String DELTA = "Delta?";
		final String EPSILON = "Epsilon?";

		DoorSprite doorSprite = doorRef.getSprite( DoorSprite.class );

		DoorCoordinate doorCoord = doorSprite.getCoordinate();
		String title = String.format( "Door (%2d,%2d, %s)", doorCoord.x, doorCoord.y, (doorCoord.v==1 ? "V" : "H") );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( OPEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( WALKING_THROUGH, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean( WALKING_THROUGH ).addMouseListener( new StatusbarMouseListener( frame, "Momentarily open as someone walks through." ) );
		editorPanel.addRow( MAX_HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( MAX_HEALTH ).addMouseListener( new StatusbarMouseListener( frame, "Nominal Health, plus situatinal modifiers like hacking?" ) );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( NOMINAL_HEALTH, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( NOMINAL_HEALTH ).addMouseListener( new StatusbarMouseListener( frame, "Default to reset Health to... sometime after combat?" ) );
		editorPanel.addRow( DELTA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( DELTA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Hacking related." ) );
		editorPanel.addRow( EPSILON, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( EPSILON ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Hacking related." ) );

		editorPanel.getBoolean( OPEN ).setSelected( doorRef.get().isOpen() );
		editorPanel.getBoolean( WALKING_THROUGH ).setSelected( doorRef.get().isWalkingThrough() );
		editorPanel.getInt( MAX_HEALTH ).setText( ""+doorRef.get().getCurrentMaxHealth() );
		editorPanel.getInt( HEALTH ).setText( ""+doorRef.get().getHealth() );
		editorPanel.getInt( NOMINAL_HEALTH ).setText( ""+doorRef.get().getNominalHealth() );
		editorPanel.getInt( DELTA ).setText( ""+doorRef.get().getUnknownDelta() );
		editorPanel.getInt( EPSILON ).setText( ""+doorRef.get().getUnknownEpsilon() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				doorRef.get().setOpen( editorPanel.getBoolean( OPEN ).isSelected() );
				doorRef.get().setWalkingThrough( editorPanel.getBoolean( WALKING_THROUGH ).isSelected() );

				try { doorRef.get().setCurrentMaxHealth( editorPanel.parseInt( MAX_HEALTH ) ); }
				catch ( NumberFormatException e ) {}

				try { doorRef.get().setHealth( editorPanel.parseInt( HEALTH ) ); }
				catch ( NumberFormatException e ) {}

				try { doorRef.get().setNominalHealth( editorPanel.parseInt( NOMINAL_HEALTH ) ); }
				catch ( NumberFormatException e ) {}

				try { doorRef.get().setUnknownDelta( editorPanel.parseInt( DELTA ) ); }
				catch ( NumberFormatException e ) {}

				try { doorRef.get().setUnknownEpsilon( editorPanel.parseInt( EPSILON ) ); }
				catch ( NumberFormatException e ) {}

				doorRef.fireReferenceChange();

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

		for ( SpriteReference<CrewState> crewRef : crewRefs ) {
			CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
			editorPanel.getCombo( CREW ).addItem( crewSprite );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object crewObj = editorPanel.getCombo( CREW ).getSelectedItem();
				if ( crewObj instanceof CrewSprite ) {
					SpriteReference<CrewState> crewRef = ((CrewSprite)crewObj).getReference();
					showCrewEditor( crewRef );
				}
			}
		};

		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 8 );
		String notice = ""
			+ "* All crew appear in this list, including dead ones awaiting "
			+ "cloned bodies. Living crew can also be clicked directly.\n";

		addSidePanelNote( notice );

		showSidePanel();
	}

	private void showCrewEditor( final SpriteReference<CrewState> crewRef ) {
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
		final String SKILL_MASTERIES = "Skill Masteries Earned";
		final String SEX = "Male";
		final String ENEMY_DRONE = "Enemy Drone";
		final String PLAYER_CONTROLLED = "Player Ctrl";
		final String CLONE_READY = "Clone Ready?";
		final String MIND_CONTROLLED = "Mind Ctrl";
		final String STUN_TICKS = "Stun Ticks";
		final String HEALTH_BOOST = "Health Boost";
		final String CLONEBAY_PRIORITY = "Clonebay Priority";
		final String DAMAGE_BOOST = "Damage Boost";
		final String LAMBDA = "Lambda?";
		final String UNIV_DEATH_COUNT = "Universal Death Count";
		final String PILOT_MASTERY_ONE = "Pilot Mastery 1";
		final String PILOT_MASTERY_TWO = "Pilot Mastery 2";
		final String ENGINE_MASTERY_ONE = "Engine Mastery 1";
		final String ENGINE_MASTERY_TWO = "Engine Mastery 2";
		final String SHIELD_MASTERY_ONE = "Shield Mastery 1";
		final String SHIELD_MASTERY_TWO = "Shield Mastery 2";
		final String WEAPON_MASTERY_ONE = "Weapon Mastery 1";
		final String WEAPON_MASTERY_TWO = "Weapon Mastery 2";
		final String REPAIR_MASTERY_ONE = "Repair Mastery 1";
		final String REPAIR_MASTERY_TWO = "Repair Mastery 2";
		final String COMBAT_MASTERY_ONE = "Combat Mastery 1";
		final String COMBAT_MASTERY_TWO = "Combat Mastery 2";
		final String NU = "Nu?";
		// TODO: Teleport anim.
		final String PHI = "Phi?";
		final String LOCKDOWN_RECHARGE_TICKS = "Lockdown Recharge Ticks";
		final String LOCKDOWN_RECHARGE_GOAL = "Lockdown Recharge Goal";
		final String OMEGA = "Omega?";

		String title = "Crew";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( NAME, FieldEditorPanel.ContentType.STRING );
		editorPanel.addRow( RACE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( HEALTH, FieldEditorPanel.ContentType.INTEGER );
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
		editorPanel.addRow( SEX, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( ENEMY_DRONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( PLAYER_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( CLONE_READY, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( CLONE_READY ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( MIND_CONTROLLED, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( STUN_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( HEALTH_BOOST, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( HEALTH_BOOST ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( CLONEBAY_PRIORITY, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( CLONEBAY_PRIORITY ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( DAMAGE_BOOST, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( DAMAGE_BOOST ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( LAMBDA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( LAMBDA ).setDocument( new RegexDocument( "-?[0-9]*" ) );
		editorPanel.addRow( UNIV_DEATH_COUNT, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addBlankRow();
		editorPanel.addRow( PILOT_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( PILOT_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( ENGINE_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( ENGINE_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( SHIELD_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( SHIELD_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( WEAPON_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( WEAPON_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( REPAIR_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( REPAIR_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( COMBAT_MASTERY_ONE, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( COMBAT_MASTERY_TWO, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( NU, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addRow( PHI, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.addBlankRow();
		editorPanel.addRow( LOCKDOWN_RECHARGE_TICKS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( LOCKDOWN_RECHARGE_GOAL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.addRow( OMEGA, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt( OMEGA ).setDocument( new RegexDocument( "-?[0-9]*" ) );

		editorPanel.getInt( HEALTH ).addMouseListener( new StatusbarMouseListener( frame, "Current health, including temporary boost. FTL 1.01-1.03.3 capped this at the race's max." ) );

		editorPanel.getInt( REPAIRS ).addMouseListener( new StatusbarMouseListener( frame, "Counter for the Hall of Fame." ) );
		editorPanel.getInt( COMBAT_KILLS ).addMouseListener( new StatusbarMouseListener( frame, "Counter for the Hall of Fame." ) );
		editorPanel.getInt( PILOTED_EVASIONS ).addMouseListener( new StatusbarMouseListener( frame, "Counter for the Hall of Fame." ) );
		editorPanel.getInt( JUMPS_SURVIVED ).addMouseListener( new StatusbarMouseListener( frame, "Counter for the Hall of Fame." ) );
		editorPanel.getInt( SKILL_MASTERIES ).addMouseListener( new StatusbarMouseListener( frame, "Total skill mastery levels ever earned, for the Hall of Fame (FTL 1.5.12+ tallies individually)." ) );
		editorPanel.getBoolean( SEX ).addMouseListener( new StatusbarMouseListener( frame, "Only human females have a distinct sprite (Other races look the same either way)." ) );
		editorPanel.getBoolean( ENEMY_DRONE ).addMouseListener( new StatusbarMouseListener( frame, "Turn into a boarding drone (clobbering other fields), hostile to this ship." ) );
		editorPanel.getBoolean( PLAYER_CONTROLLED ).addMouseListener( new StatusbarMouseListener( frame, "Whether this crew is player controlled or an NPC." ) );
		editorPanel.getBoolean( MIND_CONTROLLED ).addMouseListener( new StatusbarMouseListener( frame, "Whether this crew is mind controlled." ) );

		editorPanel.getInt( STUN_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed while waiting for a stun effect to wear off (Decrements to 0)." ) );
		editorPanel.getInt( HEALTH_BOOST ).addMouseListener( new StatusbarMouseListener( frame, "Temporary HP added from a foreign Mind Control system." ) );
		editorPanel.getInt( CLONEBAY_PRIORITY ).addMouseListener( new StatusbarMouseListener( frame, "Priority in the Clonebay's resurrection queue (Lowest first)." ) );
		editorPanel.getInt( DAMAGE_BOOST ).addMouseListener( new StatusbarMouseListener( frame, "Multiplier to apply to damage dealt by this crew (pseudo-float)." ) );
		editorPanel.getInt( LAMBDA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown." ) );
		editorPanel.getInt( UNIV_DEATH_COUNT ).addMouseListener( new StatusbarMouseListener( frame, "A value shared across crew everywhere. A death increments it to assign the next Clonebay priority." ) );

		editorPanel.getBoolean( PILOT_MASTERY_ONE ).addMouseListener( new StatusbarMouseListener( frame, "Whether the first skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( PILOT_MASTERY_TWO ).addMouseListener( new StatusbarMouseListener( frame, "Whether the second skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( ENGINE_MASTERY_ONE ).addMouseListener( new StatusbarMouseListener( frame, "Whether the first skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( ENGINE_MASTERY_TWO ).addMouseListener( new StatusbarMouseListener( frame, "Whether the second skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( SHIELD_MASTERY_ONE ).addMouseListener( new StatusbarMouseListener( frame, "Whether the first skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( SHIELD_MASTERY_TWO ).addMouseListener( new StatusbarMouseListener( frame, "Whether the second skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( REPAIR_MASTERY_ONE ).addMouseListener( new StatusbarMouseListener( frame, "Whether the first skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( REPAIR_MASTERY_TWO ).addMouseListener( new StatusbarMouseListener( frame, "Whether the second skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( COMBAT_MASTERY_ONE ).addMouseListener( new StatusbarMouseListener( frame, "Whether the first skill mastery was earned (Tallied for the Hall of Fame)." ) );
		editorPanel.getBoolean( COMBAT_MASTERY_TWO ).addMouseListener( new StatusbarMouseListener( frame, "Whether the second skill mastery was earned (Tallied for the Hall of Fame)." ) );

		editorPanel.getBoolean( NU ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Related to cloning?" ) );
		editorPanel.getBoolean( PHI ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Related to walking?" ) );

		editorPanel.getInt( LOCKDOWN_RECHARGE_TICKS ).addMouseListener( new StatusbarMouseListener( frame, "Time elapsed while waiting for the lockdown ability to recharge (Crystal only)." ) );
		editorPanel.getInt( LOCKDOWN_RECHARGE_GOAL ).addMouseListener( new StatusbarMouseListener( frame, "Time required for the lockdown ability to recharge (Crystal only)." ) );
		editorPanel.getInt( OMEGA ).addMouseListener( new StatusbarMouseListener( frame, "Unknown (Crystal only)." ) );

		ActionListener crewListener = new ActionListener() {
			private JComboBox raceCombo = editorPanel.getCombo( RACE );
			private JTextField healthField = editorPanel.getInt( HEALTH );

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

					editorPanel.getSlider( PILOT_SKILL ).setMaximum( pilotInterval*2 );
					editorPanel.getSlider( ENGINE_SKILL ).setMaximum( engineInterval*2 );
					editorPanel.getSlider( SHIELD_SKILL ).setMaximum( shieldInterval*2 );
					editorPanel.getSlider( WEAPON_SKILL ).setMaximum( weaponInterval*2 );
					editorPanel.getSlider( REPAIR_SKILL ).setMaximum( repairInterval*2 );
					editorPanel.getSlider( COMBAT_SKILL ).setMaximum( combatInterval*2 );
				}
			}
		};
		editorPanel.getCombo( RACE ).addActionListener( crewListener );

		for ( CrewType crewType : ftlConstants.getCrewTypes() ) {
			editorPanel.getCombo( RACE ).addItem( crewType );
		}
		editorPanel.getCombo( RACE ).setSelectedItem( CrewType.findById( crewRef.get().getRace() ) );

		SwingUtilities.invokeLater(new Runnable() {  // Set health after the race combo listener triggers.
			@Override
			public void run() {
				editorPanel.getInt( HEALTH ).setText( ""+crewRef.get().getHealth() );
			}
		});

		editorPanel.getString( NAME ).setText( crewRef.get().getName() );

		editorPanel.getSlider( PILOT_SKILL ).setValue( crewRef.get().getPilotSkill() );
		editorPanel.getSlider( ENGINE_SKILL ).setValue( crewRef.get().getEngineSkill() );
		editorPanel.getSlider( SHIELD_SKILL ).setValue( crewRef.get().getShieldSkill() );
		editorPanel.getSlider( WEAPON_SKILL ).setValue( crewRef.get().getWeaponSkill() );
		editorPanel.getSlider( REPAIR_SKILL ).setValue( crewRef.get().getRepairSkill() );
		editorPanel.getSlider( COMBAT_SKILL ).setValue( crewRef.get().getCombatSkill() );

		editorPanel.getInt( REPAIRS ).setText( ""+crewRef.get().getRepairs() );
		editorPanel.getInt( COMBAT_KILLS ).setText( ""+crewRef.get().getCombatKills() );
		editorPanel.getInt( PILOTED_EVASIONS ).setText( ""+crewRef.get().getPilotedEvasions() );
		editorPanel.getInt( JUMPS_SURVIVED ).setText( ""+crewRef.get().getJumpsSurvived() );
		editorPanel.getInt( SKILL_MASTERIES ).setText( ""+crewRef.get().getSkillMasteriesEarned() );
		editorPanel.getBoolean( SEX ).setSelected( crewRef.get().isMale() );
		editorPanel.getBoolean( ENEMY_DRONE ).setSelected( crewRef.get().isEnemyBoardingDrone() );
		editorPanel.getBoolean( PLAYER_CONTROLLED ).setSelected( crewRef.get().isPlayerControlled() );
		editorPanel.getInt( CLONE_READY ).setText( ""+crewRef.get().getCloneReady() );
		editorPanel.getBoolean( MIND_CONTROLLED ).setSelected( crewRef.get().isMindControlled() );

		editorPanel.getInt( STUN_TICKS ).setText( ""+crewRef.get().getStunTicks() );
		editorPanel.getInt( HEALTH_BOOST ).setText( ""+crewRef.get().getHealthBoost() );
		editorPanel.getInt( CLONEBAY_PRIORITY ).setText( ""+crewRef.get().getClonebayPriority() );
		editorPanel.getInt( DAMAGE_BOOST ).setText( ""+crewRef.get().getDamageBoost() );
		editorPanel.getInt( LAMBDA ).setText( ""+crewRef.get().getUnknownLambda() );
		editorPanel.getInt( UNIV_DEATH_COUNT ).setText( ""+crewRef.get().getUniversalDeathCount() );

		editorPanel.getBoolean( PILOT_MASTERY_ONE ).setSelected( crewRef.get().getPilotMasteryOne() );
		editorPanel.getBoolean( PILOT_MASTERY_TWO ).setSelected( crewRef.get().getPilotMasteryTwo() );
		editorPanel.getBoolean( ENGINE_MASTERY_ONE ).setSelected( crewRef.get().getEngineMasteryOne() );
		editorPanel.getBoolean( ENGINE_MASTERY_TWO ).setSelected( crewRef.get().getEngineMasteryTwo() );
		editorPanel.getBoolean( SHIELD_MASTERY_ONE ).setSelected( crewRef.get().getShieldMasteryOne() );
		editorPanel.getBoolean( SHIELD_MASTERY_TWO ).setSelected( crewRef.get().getShieldMasteryTwo() );
		editorPanel.getBoolean( WEAPON_MASTERY_ONE ).setSelected( crewRef.get().getWeaponMasteryOne() );
		editorPanel.getBoolean( WEAPON_MASTERY_TWO ).setSelected( crewRef.get().getWeaponMasteryTwo() );
		editorPanel.getBoolean( REPAIR_MASTERY_ONE ).setSelected( crewRef.get().getRepairMasteryOne() );
		editorPanel.getBoolean( REPAIR_MASTERY_TWO ).setSelected( crewRef.get().getRepairMasteryTwo() );
		editorPanel.getBoolean( COMBAT_MASTERY_ONE ).setSelected( crewRef.get().getCombatMasteryOne() );
		editorPanel.getBoolean( COMBAT_MASTERY_TWO ).setSelected( crewRef.get().getCombatMasteryTwo() );

		editorPanel.getBoolean( NU ).setSelected( crewRef.get().getUnknownNu() );
		editorPanel.getBoolean( PHI ).setSelected( crewRef.get().getUnknownPhi() );
		editorPanel.getInt( LOCKDOWN_RECHARGE_TICKS ).setText( ""+crewRef.get().getLockdownRechargeTicks() );
		editorPanel.getInt( LOCKDOWN_RECHARGE_GOAL ).setText( ""+crewRef.get().getLockdownRechargeTicksGoal() );
		editorPanel.getInt( OMEGA ).setText( ""+crewRef.get().getUnknownOmega() );

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				String prevRace = crewRef.get().getRace();

				crewRef.get().setName( editorPanel.getString( NAME ).getText() );
				crewRef.get().setRace( ((CrewType)editorPanel.getCombo( RACE ).getSelectedItem()).getId() );

				try { crewRef.get().setHealth( editorPanel.parseInt( HEALTH ) ); }
				catch ( NumberFormatException e ) {}

				crewRef.get().setPilotSkill( editorPanel.getSlider( PILOT_SKILL ).getValue() );
				crewRef.get().setEngineSkill( editorPanel.getSlider( ENGINE_SKILL ).getValue() );
				crewRef.get().setShieldSkill( editorPanel.getSlider( SHIELD_SKILL ).getValue() );
				crewRef.get().setWeaponSkill( editorPanel.getSlider( WEAPON_SKILL ).getValue() );
				crewRef.get().setRepairSkill( editorPanel.getSlider( REPAIR_SKILL ).getValue() );
				crewRef.get().setCombatSkill( editorPanel.getSlider( COMBAT_SKILL ).getValue() );

				try { crewRef.get().setRepairs( editorPanel.parseInt( REPAIRS ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setCombatKills( editorPanel.parseInt( COMBAT_KILLS ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setPilotedEvasions( editorPanel.parseInt( PILOTED_EVASIONS ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setJumpsSurvived( editorPanel.parseInt( JUMPS_SURVIVED ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setSkillMasteriesEarned( editorPanel.parseInt( SKILL_MASTERIES ) ); }
				catch ( NumberFormatException e ) {}

				crewRef.get().setMale( editorPanel.getBoolean( SEX ).isSelected() );
				crewRef.get().setEnemyBoardingDrone( editorPanel.getBoolean( ENEMY_DRONE ).isSelected() );
				crewRef.get().setPlayerControlled( editorPanel.getBoolean( PLAYER_CONTROLLED ).isSelected() );

				try { crewRef.get().setCloneReady( editorPanel.parseInt( CLONE_READY ) ); }
				catch ( NumberFormatException e ) {}

				crewRef.get().setMindControlled( editorPanel.getBoolean( MIND_CONTROLLED ).isSelected() );

				try { crewRef.get().setStunTicks( editorPanel.parseInt( STUN_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setHealthBoost( editorPanel.parseInt( HEALTH_BOOST ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setClonebayPriority( editorPanel.parseInt( CLONEBAY_PRIORITY ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setDamageBoost( editorPanel.parseInt( DAMAGE_BOOST ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setUnknownLambda( editorPanel.parseInt( LAMBDA ) ); }
				catch ( NumberFormatException e ) {}

				// TODO: Synchronize universal death count across all CrewStates on both ships.
				try { crewRef.get().setUniversalDeathCount( editorPanel.parseInt( UNIV_DEATH_COUNT ) ); }
				catch ( NumberFormatException e ) {}

				crewRef.get().setPilotMasteryOne( editorPanel.getBoolean( PILOT_MASTERY_ONE ).isSelected() );
				crewRef.get().setPilotMasteryTwo( editorPanel.getBoolean( PILOT_MASTERY_TWO ).isSelected() );
				crewRef.get().setEngineMasteryOne( editorPanel.getBoolean( ENGINE_MASTERY_ONE ).isSelected() );
				crewRef.get().setEngineMasteryTwo( editorPanel.getBoolean( ENGINE_MASTERY_TWO ).isSelected() );
				crewRef.get().setShieldMasteryOne( editorPanel.getBoolean( SHIELD_MASTERY_ONE ).isSelected() );
				crewRef.get().setShieldMasteryTwo( editorPanel.getBoolean( SHIELD_MASTERY_TWO ).isSelected() );
				crewRef.get().setWeaponMasteryOne( editorPanel.getBoolean( WEAPON_MASTERY_ONE ).isSelected() );
				crewRef.get().setWeaponMasteryTwo( editorPanel.getBoolean( WEAPON_MASTERY_TWO ).isSelected() );
				crewRef.get().setRepairMasteryOne( editorPanel.getBoolean( REPAIR_MASTERY_ONE ).isSelected() );
				crewRef.get().setRepairMasteryTwo( editorPanel.getBoolean( REPAIR_MASTERY_TWO ).isSelected() );
				crewRef.get().setCombatMasteryOne( editorPanel.getBoolean( COMBAT_MASTERY_ONE ).isSelected() );
				crewRef.get().setCombatMasteryTwo( editorPanel.getBoolean( COMBAT_MASTERY_TWO ).isSelected() );

				crewRef.get().setUnknownNu( editorPanel.getBoolean( NU ).isSelected() );
				crewRef.get().setUnknownPhi( editorPanel.getBoolean( PHI ).isSelected() );

				try { crewRef.get().setLockdownRechargeTicks( editorPanel.parseInt( LOCKDOWN_RECHARGE_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setLockdownRechargeTicksGoal( editorPanel.parseInt( LOCKDOWN_RECHARGE_GOAL ) ); }
				catch ( NumberFormatException e ) {}

				try { crewRef.get().setUnknownOmega( editorPanel.parseInt( OMEGA ) ); }
				catch ( NumberFormatException e ) {}

				// FTL would do the following as it loaded.
				if ( crewRef.get().isEnemyBoardingDrone() && !CrewType.BATTLE.getId().equals( crewRef.get().getRace() ) ) {
					crewRef.get().setRace( CrewType.BATTLE.getId() );
				}
				if ( crewRef.get().isEnemyBoardingDrone() && !crewRef.get().getName().equals( "Anti-Personnel Drone" ) ) {
					crewRef.get().setName( "Anti-Personnel Drone" );
				}
				if ( CrewType.BATTLE.getId().equals( crewRef.get().getRace() ) && !crewRef.get().isEnemyBoardingDrone() ) {
					crewRef.get().setRace( CrewType.HUMAN.getId() );
				}

				crewRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton moveBtn = new JButton( "Move To..." );
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(moveBtn);

		sidePanel.add( Box.createVerticalStrut( 6 ) );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		moveBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				moveCrew( crewRef );
			}
		});

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
				int spriteRoomId = getSpriteRoomId( crewSprite );

				shipPanel.remove( crewSprite );
				crewSprites.remove( crewSprite );
				crewRefs.remove( crewRef );
			}
		});

		addSidePanelSeparator( 8 );
		String notice = ""
			+ "* FTL will crash if you control more than 8 non-drone crew.\n"
			+ ""
			+ "* Boarding drones are drones on their owner's ship, and crew "
			+ "(race='battle') on the attacked ship.";

		addSidePanelNote( notice );

		showSidePanel();
	}



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



	public class DroneBodySprite extends JComponent implements ReferenceSprite<DroneState> {
		private BufferedImage bodyImage = null;

		private SpriteReference<DroneState> droneRef;


		public DroneBodySprite( SpriteReference<DroneState> droneRef ) {
			this.droneRef = droneRef;

			this.setPreferredSize( new Dimension( 0, 0 ) );
			this.setOpaque( false );

			droneRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<DroneState> getReference() {
			return droneRef;
		}

		@Override
		public void referenceChanged() {
			BufferedImage newBodyImage = null;
			String imgRace = null;
			boolean needsBody = false;
			int bodyX = -1, bodyY = -1;

			if ( droneRef.get() != null ) {
				DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneRef.get().getDroneId() );

				if ( DroneType.BATTLE.getId().equals( droneBlueprint.getType() ) ) {
					imgRace = "battle";
					needsBody = true;
				}
				else if ( DroneType.REPAIR.getId().equals( droneBlueprint.getType() ) ) {
					imgRace = "repair";
					needsBody = true;
				}
				else {
					// TODO: Move this into the showDroneEditor() method.

					// No body. And boarder bodies are crew on nearby ships.
					droneRef.get().setBodyX( -1 );
					droneRef.get().setBodyY( -1 );
				}
				bodyX = droneRef.get().getBodyX();
				bodyY = droneRef.get().getBodyY();

				if ( droneRef.get().isArmed() && needsBody && droneRef.get().getBodyRoomId() < 0 ) {
					// Search for an empty square in DroneCtrl.
					// This code assumes the room HAS an empty square, or it gives up and disarms.
					// TODO: Rework this.

					// FTL avoids squares with player-controlled crew/drones.
					// FTL places bodies first in DroneCtrl, then in spatially
					// nearby rooms. Possibly prioritizing station squares.
					// RoomId does not seem relevant. Algorithm unknown.
					// Presumably blocked squares are skipped.

					int[] droneSystemRoomId = shipBlueprint.getSystemList().getRoomIdBySystemType( SystemType.DRONE_CTRL );
					if ( droneSystemRoomId != null ) {
						ShipLayoutRoom layoutRoom = shipLayout.getRoom( droneSystemRoomId[0] );
						int squaresH = layoutRoom.squaresH;
						int squaresV = layoutRoom.squaresV;
						int roomCoordX = layoutRoom.locationX;
						int roomCoordY = layoutRoom.locationY;
						int roomX = layoutX + layoutRoom.locationX*squareSize;
						int roomY = layoutY + layoutRoom.locationY*squareSize;

						for ( int s=0; s < squaresH * squaresV; s++ ) {
							int squareX = roomX + (s%squaresH)*squareSize;
							int squareY = roomY + (s/squaresH)*squareSize;
							Rectangle squareRect = new Rectangle( squareX, squareY, squareSize, squareSize );

							boolean occupied = false;
							for ( DroneBodySprite otherBodySprite : droneBodySprites ) {
								int otherBodyCenterX = otherBodySprite.getX() + otherBodySprite.getPreferredSize().width/2;
								int otherBodyCenterY = otherBodySprite.getY() + otherBodySprite.getPreferredSize().height/2;
								if ( squareRect.contains( otherBodyCenterX, otherBodyCenterY ) ) {
									occupied = true;
									break;
								}
							}
							if ( occupied == false ) {
								bodyX = shipLayout.getOffsetX()*squareSize + roomCoordX*squareSize + (s%squaresH)*squareSize + squareSize/2;
								bodyY = shipLayout.getOffsetY()*squareSize + roomCoordY*squareSize + (s/squaresH)*squareSize + squareSize/2;
								droneRef.get().setBodyX( bodyX );
								droneRef.get().setBodyY( bodyY );
								droneRef.get().setBodyRoomId( droneSystemRoomId[0] );
								droneRef.get().setBodyRoomSquare( s );
								break;
							}
						}
					}
				}
				if ( droneRef.get().isArmed() && needsBody && droneRef.get().getBodyRoomId() < 0 ) {
					log.warn( "Failed to place an armed drone's body in a room: "+ droneRef.get().getDroneId() );
				}

				if ( needsBody && imgRace != null ) {
					newBodyImage = getBodyImage( imgRace, droneRef.get().isPlayerControlled() );
				}
			}

			if ( newBodyImage != bodyImage ) {
				int newW = newBodyImage != null ? newBodyImage.getWidth() : 0;
				int newH = newBodyImage != null ? newBodyImage.getHeight() : 0;

				this.setPreferredSize( new Dimension( newW, newH ) );

				// TODO: Grr, bounds manipulation...
				this.setSize( this.getPreferredSize() );

				if ( droneRef.get().getBodyRoomId() >= 0 ) {
					int bodySpriteX =  originX + bodyX;
					int bodySpriteY =  originY + bodyY;
					this.setLocation( bodySpriteX - newW/2, bodySpriteY - newH/2 );
					this.setVisible( true );
				}
				else {
					this.setLocation( 0, 0 );  // TODO: Pick a better drone hades point.
					this.setVisible( false );
				}

				bodyImage = newBodyImage;
			}

			this.repaint();
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;

			if ( bodyImage != null ) {
				g2d.drawImage( bodyImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
			}
		}
	}



	public class WeaponSprite extends JComponent implements ReferenceSprite<WeaponState> {
		private int longSide = 64, shortSide = 25;

		private SpriteReference<WeaponState> weaponRef;
		private int slot;
		private boolean rotated;
		private String slotString;


		public WeaponSprite( SpriteReference<WeaponState> weaponRef, int slot, boolean rotated ) {
			this.weaponRef = weaponRef;
			this.slot = slot;
			this.slotString = Integer.toString( slot+1 );
			this.rotated = rotated;

			if ( rotated ) {
				this.setPreferredSize( new Dimension( longSide, shortSide ) );
			} else {
				this.setPreferredSize( new Dimension( shortSide, longSide ) );
			}

			this.setOpaque( false );

			weaponRef.addSprite( this );
			referenceChanged();
		}

		public void setSlot( int n ) { slot = n; }
		public int getSlot() { return slot; }

		@Override
		public SpriteReference<WeaponState> getReference() {
			return weaponRef;
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



	public class RoomSprite extends JComponent implements ReferenceSprite<RoomState> {
		private final Color maxColor = new Color( 230, 226, 219 );
		private final Color minColor = new Color( 255, 176, 169 );
		private final Color vacuumBorderColor = new Color( 255, 180, 0 );
		private Color bgColor = maxColor;

		private SpriteReference<RoomState> roomRef;
		private int roomId;


		public RoomSprite( SpriteReference<RoomState> roomRef, int roomId ) {
			this.roomRef = roomRef;
			this.roomId = roomId;

			// No preferred size.
			this.setOpaque( true );

			roomRef.addSprite( this );
			referenceChanged();
		}

		public void setRoomId( int n ) { roomId = n; }
		public int getRoomId() { return roomId; }

		@Override
		public SpriteReference<RoomState> getReference() {
			return roomRef;
		}

		@Override
		public void referenceChanged() {
			if ( roomRef.get().getOxygen() == 100 ) {
				bgColor = maxColor;
			}
			else if ( roomRef.get().getOxygen() == 0 ) {
				bgColor = minColor;
			}
			else {
				double p = roomRef.get().getOxygen() / 100.0;
				int maxRed = maxColor.getRed();
				int maxGreen = maxColor.getGreen();
				int maxBlue = maxColor.getBlue();
				int minRed = minColor.getRed();
				int minGreen = minColor.getGreen();
				int minBlue = minColor.getBlue();
				bgColor = new Color( (int)(minRed+p*(maxRed-minRed)), (int)(minGreen+p*(maxGreen-minGreen)), (int)(minBlue+p*(maxBlue-minBlue)) );
			}

			this.repaint();
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();

			g2d.setColor( bgColor );
			g2d.fillRect( 0, 0, this.getWidth()-1, this.getHeight()-1);

			if ( roomRef.get().getOxygen() == 0 ) {  // Draw the yellow border.
				g2d.setColor( vacuumBorderColor );
				g2d.drawRect( 2, 2, (this.getWidth()-1)-4, (this.getHeight()-1)-4 );
				g2d.drawRect( 3, 3, (this.getWidth()-1)-6, (this.getHeight()-1)-6 );
			}

			g2d.setColor(prevColor);
		}
	}



	public class SystemRoomSprite extends JComponent implements ReferenceSprite<SystemState> {
		private int scaleW = 32, scaleH = 32;
		private BufferedImage overlayImage = null;
		private BufferedImage currentImage = null;

		private SpriteReference<SystemState> systemRef;


		public SystemRoomSprite( SpriteReference<SystemState> systemRef ) {
			this.systemRef = systemRef;

			// Assuming these are interchangeable.
			String iconBaseName = systemRef.get().getSystemType().getId();
			overlayImage = ImageUtilities.getScaledImage( "img/icons/s_"+ iconBaseName +"_overlay.png", scaleW, scaleH, cachedImages );

			this.setPreferredSize( new Dimension( scaleW, scaleH ) );
			this.setOpaque( false );

			systemRef.addSprite( this );
			referenceChanged();
		}

		@Override
		public SpriteReference<SystemState> getReference() {
			return systemRef;
		}

		@Override
		public void referenceChanged() {
			// The original overlayImage is white with a black border.
			Tint tint = null;

			if ( systemRef.get().getCapacity() == 0 ) {
				// Absent, selectively darken to brown.
				tint = new Tint( new float[] { 0.792f, 0.467f, 0.275f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( systemRef.get().getIonizedBars() > 0 ) {
				// Ionized, selectively darken to blue.
				tint = new Tint( new float[] { 0.51f, 0.898f, 0.937f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( systemRef.get().getDamagedBars() == systemRef.get().getCapacity() ) {
				// Crippled, selectively darken to red (softer shade than in-game).
				tint = new Tint( new float[] { 0.85f, 0.24f, 0.24f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else if ( systemRef.get().getDamagedBars() > 0 ) {
				// Damaged, selectively darken to orange.
				tint = new Tint( new float[] { 0.99f, 0.6f, 0.3f, 1f }, new float[] { 0, 0, 0, 0 } );
			}
			else {
				// Darken to gray...
				tint = new Tint( new float[] { 0.49f, 0.49f, 0.49f, 1f }, new float[] { 0, 0, 0, 0 } );
			}

			currentImage = overlayImage;
			if ( tint != null ) {
				currentImage = ImageUtilities.getTintedImage( currentImage, tint, cachedTintedImages );
			}
			this.repaint();
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

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
			this.setOpaque( false );
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

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
			this.setOpaque( false );
		}

		public void setRoomId( int n ) { roomId = n; }
		public void setSquareId( int n ) { squareId = n; }
		public void setHealth( int n ) { health = n; }

		public int getRoomId() { return roomId; }
		public int getSquareId() { return squareId; }
		public int getHealth() { return health; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( fireImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}
	}



	public class DoorSprite extends JComponent implements ReferenceSprite<DoorState> {
		private BufferedImage currentImage;

		private SpriteReference<DoorState> doorRef;
		private Map<Integer, BufferedImage> closedImages;
		private Map<Integer, BufferedImage> openImages;
		private Integer level;
		private DoorCoordinate doorCoord;


		public DoorSprite( SpriteReference<DoorState> doorRef, Map<Integer, BufferedImage> closedImages, Map<Integer, BufferedImage> openImages, Integer level, DoorCoordinate doorCoord ) {
			this.doorRef = doorRef;
			this.closedImages = closedImages;
			this.openImages = openImages;
			this.level = level;
			this.doorCoord = doorCoord;

			int chop = 10;
			int longSide = 35, shortSide = 35-(chop*2);
			if ( doorCoord.v == 1 ) {
				this.setPreferredSize( new Dimension( shortSide, longSide ) );
			} else {
				this.setPreferredSize( new Dimension( longSide, shortSide ) );
			}

			this.setOpaque( false );

			doorRef.addSprite( this );
			referenceChanged();
		}

		public void setLevel( int n ) { level = n; }
		public int getLevel() { return level; }

		public void setCoordinate( DoorCoordinate c ) { doorCoord = c; }
		public DoorCoordinate getCoordinate() { return doorCoord; }

		@Override
		public SpriteReference<DoorState> getReference() {
			return doorRef;
		}

		@Override
		public void referenceChanged() {
			// TODO: Do away with the sprite's "level" field.
			// Test if doorState's health fields are unreliable and check the
			// Doors system capacity.

			if ( doorRef.get().isOpen() ) {
				currentImage = openImages.get( level );
			} else {
				currentImage = closedImages.get( level );
			}

			this.repaint();
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			Color prevColor = g2d.getColor();
			int w = this.getWidth(), h = this.getHeight();

			if ( doorCoord.v == 0 ) {  // Use rotated coordinates to draw AS IF vertical.
				g2d.rotate( Math.toRadians( 90 ) );   // Clockwise.
				w = this.getHeight(); h = this.getWidth();
				g2d.translate( 0, -h );
			}

			// The big image may not have had enough rows to populate all potential Door levels.
			if ( currentImage != null ) {
				g2d.drawImage( currentImage, 0, 0, this);
			}

			g2d.setColor( prevColor );
		}
	}



	public class CrewSprite extends JComponent implements ReferenceSprite<CrewState> {
		private int w=35, h=35;
		private Tint ghostTint;
		private BufferedImage crewImage;

		private SpriteReference<CrewState> crewRef;


		public CrewSprite( SpriteReference<CrewState> crewRef ) {
			this.crewRef = crewRef;

			// Not an exact color match, but close enough.
			ghostTint = new Tint( new float[] { 1f, 1f, 1f, 0.6f }, new float[] { 0, 0, 0, 0 } );

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
			String imgRace = crewRef.get().getRace();
			Tint tint = null;

			if ( CrewType.HUMAN.getId().equals( crewRef.get().getRace() ) ) {
				// Human females have a distinct sprite (Other races look the same either way).
				if ( !crewRef.get().isMale() ) {
					imgRace = "female";  // Not an actual race.
				}
			}
			else if ( CrewType.GHOST.getId().equals( crewRef.get().getRace() ) ) {
				// Ghosts look like translucent humans.
				if ( crewRef.get().isMale() ) {
					imgRace = "human";
				} else {
					imgRace = "female";
				}

				tint = ghostTint;
			}

			crewImage = getBodyImage( imgRace, crewRef.get().isPlayerControlled() );
			if ( tint != null ) {
				crewImage = ImageUtilities.getTintedImage( crewImage, tint, cachedTintedImages );
			}

			this.repaint();
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent( g );

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( crewImage, 0, 0, this.getWidth()-1, this.getHeight()-1, this);
		}

		@Override
		public String toString() {
			return String.format("%s (%s, %d HP)", crewRef.get().getName(), crewRef.get().getRace(), crewRef.get().getHealth());
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

		private Map<Rectangle, Integer> squareRegionRoomIdMap;
		private Map<Rectangle, Integer> squareRegionSquareIdMap;
		private SquareCriteria squareCriteria = defaultCriteria;
		private SquareSelectionCallback callback = null;
		private Point mousePoint = new Point( -1, -1 );
		private Rectangle currentRect = null;
		private boolean paintDescription = false;

		public SquareSelector( Map<Rectangle, Integer> squareRegionRoomIdMap, Map<Rectangle, Integer> squareRegionSquareIdMap ) {
			this.squareRegionRoomIdMap = squareRegionRoomIdMap;
			this.squareRegionSquareIdMap = squareRegionSquareIdMap;
		}

		public void setMousePoint( int x, int y ) {
			if ( mousePoint.x != x || mousePoint.y != y) {
				mousePoint.x = x;
				mousePoint.y = y;

				Rectangle newRect = null;
				if ( mousePoint.x > 0 && mousePoint.y > 0 ) {
					for ( Map.Entry<Rectangle, Integer> entry : squareRegionSquareIdMap.entrySet() ) {
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
			if ( squareRegionRoomIdMap.containsKey( currentRect ) ) {
				roomId = squareRegionRoomIdMap.get( currentRect ).intValue();
			}
			return roomId;
		}

		public int getSquareId() {
			int squareId = -1;
			if ( squareRegionSquareIdMap.containsKey( currentRect ) ) {
				squareId = squareRegionSquareIdMap.get( currentRect ).intValue();
			}
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
		boolean squareSelected( SquareSelector squareSelector, int roomId, int squareId );
	}
}
