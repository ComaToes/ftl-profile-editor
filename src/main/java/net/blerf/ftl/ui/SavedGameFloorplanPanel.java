package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;
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
import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.RoomAndSquare;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;
import net.blerf.ftl.model.XYPair;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.BatteryInfo;
import net.blerf.ftl.parser.SavedGameParser.BoarderDronePodInfo;
import net.blerf.ftl.parser.SavedGameParser.CloakingInfo;
import net.blerf.ftl.parser.SavedGameParser.ClonebayInfo;
import net.blerf.ftl.parser.SavedGameParser.CrewState;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DoorState;
import net.blerf.ftl.parser.SavedGameParser.DronePodState;
import net.blerf.ftl.parser.SavedGameParser.DroneState;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.parser.SavedGameParser.ExtendedDroneInfo;
import net.blerf.ftl.parser.SavedGameParser.ExtendedDronePodInfo;
import net.blerf.ftl.parser.SavedGameParser.ExtendedSystemInfo;
import net.blerf.ftl.parser.SavedGameParser.RoomState;
import net.blerf.ftl.parser.SavedGameParser.ShieldsInfo;
import net.blerf.ftl.parser.SavedGameParser.ShipState;
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
import net.blerf.ftl.ui.floorplan.AnimAtlas;
import net.blerf.ftl.ui.floorplan.BreachSprite;
import net.blerf.ftl.ui.floorplan.CrewSprite;
import net.blerf.ftl.ui.floorplan.DefaultSpriteImageProvider;
import net.blerf.ftl.ui.floorplan.DoorAtlas;
import net.blerf.ftl.ui.floorplan.DoorSprite;
import net.blerf.ftl.ui.floorplan.DroneBodySprite;
import net.blerf.ftl.ui.floorplan.DroneBoxSprite;
import net.blerf.ftl.ui.floorplan.FireSprite;
import net.blerf.ftl.ui.floorplan.FloorplanCoord;
import net.blerf.ftl.ui.floorplan.RoomSprite;
import net.blerf.ftl.ui.floorplan.ShipBundle;
import net.blerf.ftl.ui.floorplan.ShipInteriorComponent;
import net.blerf.ftl.ui.floorplan.SpriteImageProvider;
import net.blerf.ftl.ui.floorplan.SystemRoomSprite;
import net.blerf.ftl.ui.floorplan.WeaponSprite;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.SquareSelector;
import net.blerf.ftl.ui.hud.SquareSelector.SquareCriteria;
import net.blerf.ftl.ui.hud.SquareSelector.SquareSelectionCallback;
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
	private static final Integer INTERIOR_LAYER = 15;
	private static final Integer SYSTEM_LAYER = 16;
	private static final Integer BREACH_LAYER = 17;
	private static final Integer FIRE_LAYER = 18;
	private static final Integer CREW_LAYER = 19;
	private static final Integer DOOR_LAYER = 20;
	private static final Integer DRONE_LAYER = 21;
	private static final Integer DEFAULT_SELECTION_LAYER = 50;
	private static final Integer MISC_SELECTION_LAYER = 55;
	private static final Integer SQUARE_SELECTION_LAYER = 60;

	private static final int PLAYER_ORIGIN_X = 0;
	private static final int PLAYER_ORIGIN_Y = 0;
	private static final int NEARBY_CENTER_X = 1075;
	private static final int NEARBY_CENTER_Y = 322;

	private static final int squareSize = 35;
	private static final int tileEdge = 1;

	private GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	private GraphicsDevice gs = ge.getDefaultScreenDevice();
	private GraphicsConfiguration gc = gs.getDefaultConfiguration();

	private FTLFrame frame;

	private FTLConstants ftlConstants = new OriginalFTLConstants();
	private List<ShipBundle> shipBundles = new ArrayList( 2 );
	private ShipBundle playerBundle = null;
	private ShipBundle nearbyBundle = null;

	private DefaultSpriteImageProvider spriteImageProvider = new DefaultSpriteImageProvider();
	private Map<String, Map<Rectangle, BufferedImage>> cachedImages = new HashMap<String, Map<Rectangle, BufferedImage>>();

	private JLayeredPane shipPanel = null;
	private StatusViewport shipViewport = null;
	private JPanel sidePanel = null;
	private JScrollPane sideScroll = null;

	private SpriteSelector defaultSelector = null;
	private SpriteSelector miscSelector = null;
	private SquareSelector<FloorplanCoord> squareSelector = null;



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

		defaultSelector = new SpriteSelector();
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

					ShipBundle shipBundle = null;
					for ( ShipBundle bundle : shipBundles ) {
						if ( bundle.getDoorRefs().contains( doorRef ) ) {
							shipBundle = bundle;
							break;
						}
					}

					showDoorEditor( shipBundle, doorRef );
				}
				else if ( sprite instanceof DroneBoxSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Drone editing is not possible yet for Advanced Edition saved games.\n\nHowever, cargo (General tab) and stores (Sector Map tab) can be edited.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						SpriteReference<DroneState> droneRef = ((DroneBoxSprite)sprite).getReference();

						ShipBundle shipBundle = null;
						for ( ShipBundle bundle : shipBundles ) {
							if ( bundle.getDroneRefs().contains( droneRef ) ) {
								shipBundle = bundle;
								break;
							}
						}

						showDroneEditor( shipBundle, droneRef );
					}
				}
				else if ( sprite instanceof WeaponSprite ) {
					if ( ftlConstants instanceof AdvancedFTLConstants ) {  // TODO: Remove this.
						JOptionPane.showMessageDialog( SavedGameFloorplanPanel.this.frame, "Weapon editing is not possible yet for Advanced Edition saved games.\n\nHowever, cargo (General tab) and stores (Sector Map tab) can be edited.", "Work in Progress", JOptionPane.WARNING_MESSAGE );
					}
					else {
						SpriteReference<WeaponState> weaponRef = ((WeaponSprite)sprite).getReference();

						ShipBundle shipBundle = null;
						for ( ShipBundle bundle : shipBundles ) {
							if ( bundle.getWeaponRefs().contains( weaponRef ) ) {
								shipBundle = bundle;
								break;
							}
						}

						showWeaponEditor( shipBundle, weaponRef );
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

		squareSelector = new SquareSelector<FloorplanCoord>();
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
					if ( callback != null ) {
						keepSelecting = callback.squareSelected( squareSelector, squareSelector.getSquareCoord() );
					}
					if ( keepSelecting == false ) {
						squareSelector.reset();
					}
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
		otherPanel.setBorder( BorderFactory.createTitledBorder( "Other (Select a ship)" ) );
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
				if ( shipBundles.isEmpty() ) return;  // No ship to edit!

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
					for ( SpriteReference<RoomState> roomRef : playerBundle.getRoomRefs() ) {
						if ( roomRef.get().getOxygen() != 100 ) {
							roomRef.get().setOxygen( 100 );
							roomRef.fireReferenceChange();
						}
					}
					shipViewport.repaint();
				}
				else if ( source == resetSystemsBtn ) {
					clearSidePanel();
					for ( SpriteReference<SystemState> systemRef : playerBundle.getSystemRefs() ) {
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
					for ( ListIterator<SpriteReference<CrewState>> it = playerBundle.getCrewRefs().listIterator(); it.hasNext(); ) {
						SpriteReference<CrewState> crewRef = it.next();

						if ( !crewRef.get().isPlayerControlled() ) {
							CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
							shipPanel.remove( crewSprite );
							playerBundle.getCrewSprites().remove( crewSprite );
							it.remove();
						}
					}
					shipViewport.repaint();
				}
				else if ( source == resetBreachesBtn ) {
					clearSidePanel();
					for ( BreachSprite breachSprite : playerBundle.getBreachSprites() ) {
						shipPanel.remove( breachSprite );
					}
					playerBundle.getBreachSprites().clear();
					shipViewport.repaint();
				}
				else if ( source == resetFiresBtn ) {
					clearSidePanel();
					for ( FireSprite fireSprite : playerBundle.getFireSprites() ) {
						shipPanel.remove( fireSprite );
					}
					playerBundle.getFireSprites().clear();
					shipViewport.repaint();
				}
				else if ( source == otherGeneralBtn ) {
					selectGeneral();
				} else if (source == otherAugmentsBtn ) {
					selectAugments();
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

	private void addBundle( ShipBundle shipBundle, ShipState shipState, boolean playerControlled ) {

		ShipBlueprint shipBlueprint = DataManager.get().getShip( shipState.getShipBlueprintId() );
		ShipLayout shipLayout = DataManager.get().getShipLayout( shipState.getShipLayoutId() );
		ShipChassis shipChassis = DataManager.get().getShipChassis( shipState.getShipLayoutId() );
		String shipGfxBaseName = shipState.getShipGraphicsBaseName();

		shipBundle.setShipBlueprint( shipBlueprint );
		shipBundle.setShipLayout( shipLayout );
		shipBundle.setShipChassis( shipChassis );
		shipBundle.setShipGraphicsBaseName( shipGfxBaseName );

		shipBundle.setReservePowerCapacity( shipState.getReservePowerCapacity() );
		shipBundle.setShipName( shipState.getShipName() );
		shipBundle.setHullAmt( shipState.getHullAmt() );
		shipBundle.setFuelAmt( shipState.getFuelAmt() );
		shipBundle.setDronePartsAmt( shipState.getDronePartsAmt() );
		shipBundle.setMissilesAmt( shipState.getMissilesAmt() );
		shipBundle.setScrapAmt( shipState.getScrapAmt() );
		shipBundle.setHostile( shipState.isHostile() );
		shipBundle.setJumpChargeTicks( shipState.getJumpChargeTicks() );
		shipBundle.setJumping( shipState.isJumping() );
		shipBundle.setJumpAnimTicks( shipState.getJumpAnimTicks() );
		shipBundle.setCloakAnimTicks( shipState.getCloakAnimTicks() );

		shipBundle.setPlayerControlled( playerControlled );
		shipBundle.getAugmentIdList().addAll( shipState.getAugmentIdList() );

		if ( shipBundle.isPlayerControlled() ) {  // Allow ship images their negative offsets.
			shipBundle.setOriginX( PLAYER_ORIGIN_X + shipChassis.getImageBounds().x * -1 );
			shipBundle.setOriginY( PLAYER_ORIGIN_Y + shipChassis.getImageBounds().y * -1 );
		}
		else {
			// Hardcoded arbitrary offset.
			shipBundle.setOriginX( NEARBY_CENTER_X + shipChassis.getImageBounds().x * -1 - shipChassis.getImageBounds().w/2 );
			shipBundle.setOriginY( NEARBY_CENTER_Y + shipChassis.getImageBounds().y * -1 - shipChassis.getImageBounds().h/2 );
		}
		shipBundle.setLayoutX( shipBundle.getOriginX() + shipLayout.getOffsetX() * squareSize );
		shipBundle.setLayoutY( shipBundle.getOriginY() + shipLayout.getOffsetY() * squareSize );


		for ( int roomId=0; roomId < shipLayout.getRoomCount(); roomId++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
			int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

			Rectangle roomRect = new Rectangle( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
			shipBundle.getRoomRegionRoomIdMap().put( roomRect, roomId );

			for ( int s=0; s < squaresH * squaresV; s++ ) {
				int squareX = roomX + (s%squaresH)*squareSize;
				int squareY = roomY + (s/squaresH)*squareSize;
				Rectangle squareRect = new Rectangle( squareX, squareY, squareSize, squareSize );

				FloorplanCoord squareCoord = new FloorplanCoord( (playerControlled ? 0 : 1), roomId, s );
				shipBundle.getSquareRegionCoordMap().put( squareRect, squareCoord );
			}
		}

		// Find squares that don't allow crew in them (medbay's slot, same as clonebay).
		// TODO: Enemy ships don't have a blocked slot.
		//   Dunno if that's from being in "autoBlueprints.xml" or non-player controlled.
		//   Commandeer one and find out.
		ShipBlueprint.SystemList.SystemRoom medicalSystem = shipBlueprint.getSystemList().getMedicalRoom();
		if ( medicalSystem != null ) {
			ShipBlueprint.SystemList.RoomSlot medicalSlot = medicalSystem.getSlot();
			int badRoomId = medicalSystem.getRoomId();

			int badSquareId;
			if ( medicalSlot != null ) {
				badSquareId = medicalSlot.getNumber();
			} else {
				badSquareId = ftlConstants.getDefaultSystemRoomSlotSquare( SystemType.MEDBAY );
			}

			if ( badSquareId >= 0 ) {  // When -2, there's no blocked square.
				shipBundle.getBlockedRasList().add( new RoomAndSquare( badRoomId, badSquareId ) );
			}
		}

		// Hull.
		JLabel baseLbl = new JLabel();
		baseLbl.setOpaque( false );
		baseLbl.setSize( shipChassis.getImageBounds().w, shipChassis.getImageBounds().h );
		baseLbl.setLocation( shipBundle.getLayoutX() + shipChassis.getImageBounds().x, shipBundle.getLayoutY() + shipChassis.getImageBounds().y );
		shipPanel.add( baseLbl, BASE_LAYER );

		BufferedImage baseImage = spriteImageProvider.getShipBaseImage( shipGfxBaseName, shipChassis.getImageBounds().w, shipChassis.getImageBounds().h );
		baseLbl.setIcon( new ImageIcon( baseImage ) );
		shipBundle.setBaseLbl( baseLbl );

		// Floor.
		JLabel floorLbl = new JLabel();
		floorLbl.setOpaque( false );
		floorLbl.setLocation( shipBundle.getLayoutX() + shipChassis.getImageBounds().x, shipBundle.getLayoutY() + shipChassis.getImageBounds().y );
		shipPanel.add( floorLbl, FLOOR_LAYER );

		BufferedImage floorImage = spriteImageProvider.getShipFloorImage( shipGfxBaseName );
		if ( floorImage != null ) {
			floorLbl.setIcon( new ImageIcon( floorImage ) );
			floorLbl.setSize( floorImage.getWidth(), floorImage.getHeight() );

			if ( shipChassis.getOffsets() != null ) {
				Offset floorOffset = shipChassis.getOffsets().floorOffset;
				if ( floorOffset != null ) {
					floorLbl.setLocation( shipBundle.getLayoutX() + shipChassis.getImageBounds().x + floorOffset.x, shipBundle.getLayoutY() + shipChassis.getImageBounds().y + floorOffset.y );
				}
			}
		}
		shipBundle.setFloorLbl( floorLbl );

		// Floor cracks, decor, and walls.
		ShipInteriorComponent interiorComp = new ShipInteriorComponent( shipLayout );
		interiorComp.setSize( interiorComp.getPreferredSize() );
		interiorComp.setLocation( shipBundle.getLayoutX() - interiorComp.getLocationFudge(), shipBundle.getLayoutY() - interiorComp.getLocationFudge() );
		shipPanel.add( interiorComp, INTERIOR_LAYER );

		for ( ShipBlueprint.SystemList.SystemRoom systemRoom : shipBlueprint.getSystemList().getSystemRooms() ) {
			int roomId = systemRoom.getRoomId();

			ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;

			String decorName = systemRoom.getImg();

			// TODO: Looks like when medbay omits img, it's "room_medbay.png".

			if ( decorName == null ) {
				if ( systemRoom == shipBlueprint.getSystemList().getTeleporterRoom() ) {
					decorName = "teleporter_off";  // Draw a teleporter pad on each square.
				}
			}

			if ( decorName != null ) {
				BufferedImage decorImage = spriteImageProvider.getRoomDecorImage( decorName, squaresH, squaresV );

				if ( decorImage != null ) {
					interiorComp.getDecorMap().put( roomId, decorImage );
				}
			}
		}
		shipBundle.setInteriorComp( interiorComp );

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
			if ( droneList.size() > minDroneSlots && shipBundle.isPlayerControlled() ) {
				log.warn( String.format( "Ship state has %d drones, exceeding the default %d droneSlots when not set by its blueprint", droneList.size(), minDroneSlots ) );
			}
			actualDroneSlots = Math.max( minDroneSlots, droneList.size() );
		}

		for ( int i=0; i < actualDroneSlots; i++ ) {
			// It's fine if droneState is null. Empty slot.
			SpriteReference<DroneState> droneRef = new SpriteReference<DroneState>( null );
			if ( droneList.size() > i ) droneRef.set( new DroneState( droneList.get( i ) ) );

			shipBundle.getDroneRefs().add( droneRef );

			int droneBoxX = shipBundle.getOriginX() + 50 + i*75;
			int droneBoxY = shipBundle.getLayoutY() + shipChassis.getImageBounds().y + shipChassis.getImageBounds().h;

			addDroneSprite( shipBundle, droneRef, droneBoxX, droneBoxY, i );
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

			shipBundle.getWeaponRefs().add( weaponRef );

			addWeaponSprite( shipBundle, weaponRef, weaponMount, i );
		}

		// Flagship has no Weapons system but it omits weaponSlots, so first 4 mounts are junk.
		if ( shipBlueprint.getSystemList().getWeaponRoom() == null ) {
			for ( WeaponSprite weaponSprite : shipBundle.getWeaponSprites() ) {
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
			int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
			int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

			int oxygen = shipState.getRoom( i ).getOxygen();

			SpriteReference<RoomState> roomRef = new SpriteReference<RoomState>( new RoomState( shipState.getRoom( i ) ) );
			shipBundle.getRoomRefs().add( roomRef );

			RoomSprite roomSprite = new RoomSprite( roomRef, i );
			roomSprite.setBounds( roomX, roomY, squaresH*squareSize, squaresV*squareSize );
			shipBundle.getRoomSprites().add( roomSprite );
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
					int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
					int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

					int systemX = roomX + squaresH*squareSize/2;
					int systemY = roomY + squaresV*squareSize/2;

					SystemState systemState = shipState.getSystem( systemType );
					if ( systemState == null ) break;  // TODO: Support systems that aren't on the shipState.

					SpriteReference<SystemState> systemRef = new SpriteReference<SystemState>( new SystemState( systemState ) );
					shipBundle.getSystemRefs().add( systemRef );

					addSystemRoomSprite( shipBundle, systemRef, systemX, systemY );
				}
			}
		}

		// Add Extended System Info.
		for ( ExtendedSystemInfo info : shipState.getExtendedSystemInfoList() ) {
			shipBundle.getExtendedSystemInfoList().add( info.copy() );
		}

		// Add breaches.
		for ( Map.Entry<XYPair, Integer> breachEntry : shipState.getBreachMap().entrySet() ) {
			int breachCoordX = breachEntry.getKey().x - shipLayout.getOffsetX();  // Convert from goofy coords.
			int breachCoordY = breachEntry.getKey().y - shipLayout.getOffsetY();
			int breachX = shipBundle.getLayoutX() + breachCoordX*squareSize + squareSize/2;
			int breachY = shipBundle.getLayoutY() + breachCoordY*squareSize + squareSize/2;

			Rectangle squareRect = null;
			int roomId = -1;
			int squareId = -1;
			for ( Map.Entry<Rectangle, FloorplanCoord> regionEntry : shipBundle.getSquareRegionCoordMap().entrySet() ) {
				if ( regionEntry.getKey().contains( breachX, breachY ) ) {
					squareRect = regionEntry.getKey();
					roomId = regionEntry.getValue().roomId;
					squareId = regionEntry.getValue().squareId;
					break;
				}
			}
			addBreachSprite( shipBundle, breachX, breachY, roomId, squareId, breachEntry.getValue().intValue() );
		}

		// Add fires.
		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( i );
			int squaresH = layoutRoom.squaresH;
			int squaresV = layoutRoom.squaresV;
			int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
			int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

			RoomState roomState = shipState.getRoom( i );
			for ( int s=0; s < squaresH * squaresV; s++ ) {
				int fireHealth = roomState.getSquare( s ).getFireHealth();
				if ( fireHealth > 0 ) {
					int fireX = roomX + (s%squaresH)*squareSize + squareSize/2;
					int fireY = roomY + (s/squaresH)*squareSize + squareSize/2;
					addFireSprite( shipBundle, fireX, fireY, i, s, fireHealth );
				}
			}
		}

		// Add doors.
		int doorLevel = shipState.getSystem( SystemType.DOORS ).getCapacity()-1;  // Convert to 0-based.
		if ( doorLevel < 0 ) doorLevel = 0;  // Door subsystem was absent, 0-Capacity.
		for ( Map.Entry<DoorCoordinate, DoorState> entry : shipState.getDoorMap().entrySet() ) {
			DoorCoordinate doorCoord = entry.getKey();
			DoorState doorState = entry.getValue();
			int doorX = shipBundle.getLayoutX() + doorCoord.x*squareSize + (doorCoord.v==1 ? 0 : squareSize/2);
			int doorY = shipBundle.getLayoutY() + doorCoord.y*squareSize + (doorCoord.v==1 ? squareSize/2 : 0);

			SpriteReference<DoorState> doorRef = new SpriteReference<DoorState>( new DoorState( doorState ) );
			shipBundle.getDoorRefs().add( doorRef );

			addDoorSprite( shipBundle, doorRef, doorX, doorY, doorLevel, doorCoord );
		}

		// Add crew.
		// TODO: Add dead crew at their spriteX/spriteY but toggle visibility?
		int hadesX = shipBundle.getOriginX() + 50 - (int)(squareSize * 1.5);
		int hadesY = shipBundle.getLayoutY() + shipChassis.getImageBounds().y + shipChassis.getImageBounds().h;

		for ( CrewState crewState : shipState.getCrewList() ) {
			SpriteReference<CrewState> crewRef = new SpriteReference<CrewState>( new CrewState( crewState ) );
			shipBundle.getCrewRefs().add( crewRef );

			int crewX = 0, crewY = 0;
			int goalX = 0, goalY = 0;

			if ( crewState.getRoomId() != -1 ) {
				crewX = shipBundle.getOriginX() + crewState.getSpriteX();
				crewY = shipBundle.getOriginY() + crewState.getSpriteY();

				// TODO: Draw lines to goal dots for walking crew?
				ShipLayoutRoom layoutRoom = shipLayout.getRoom( crewState.getRoomId() );
				int squaresH = layoutRoom.squaresH;
				int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
				int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

				goalX = roomX + (crewState.getRoomSquare()%squaresH)*squareSize + squareSize/2;
				goalY = roomY + (crewState.getRoomSquare()/squaresH)*squareSize + squareSize/2;
			}
			else {
				crewX = hadesX;
				crewY = hadesY;
			}
			addCrewSprite( shipBundle, crewRef, crewX, crewY );
		}
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		defaultSelector.setVisible( false );
		defaultSelector.setMousePoint( -1, -1 );
		defaultSelector.clearSpriteLists();

		miscSelector.reset();
		miscSelector.clearSpriteLists();

		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		shipViewport.setStatusString( null );
		clearSidePanel();

		for ( ShipBundle bundle : shipBundles ) {
			for ( DroneBoxSprite droneBoxSprite : bundle.getDroneBoxSprites() ) {
				shipPanel.remove( droneBoxSprite );
			}
			for ( DroneBodySprite droneBodySprite : bundle.getDroneBodySprites() ) {
				shipPanel.remove( droneBodySprite );
			}
			for ( WeaponSprite weaponSprite : bundle.getWeaponSprites() ) {
				shipPanel.remove( weaponSprite );
			}
			for ( RoomSprite roomSprite : bundle.getRoomSprites() ) {
				shipPanel.remove( roomSprite );
			}
			for ( SystemRoomSprite systemRoomSprite : bundle.getSystemRoomSprites() ) {
				shipPanel.remove( systemRoomSprite );
			}
			for ( BreachSprite breachSprite : bundle.getBreachSprites() ) {
				shipPanel.remove( breachSprite );
			}
			for ( FireSprite fireSprite : bundle.getFireSprites() ) {
				shipPanel.remove( fireSprite );
			}
			for ( DoorSprite doorSprite : bundle.getDoorSprites() ) {
				shipPanel.remove( doorSprite );
			}
			for ( CrewSprite crewSprite : bundle.getCrewSprites() ) {
				shipPanel.remove( crewSprite );
			}

			shipPanel.remove( bundle.getBaseLbl() );
			shipPanel.remove( bundle.getFloorLbl() );
			shipPanel.remove( bundle.getInteriorComp() );
		}
		shipBundles.clear();

		if ( gameState == null ) {
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

		playerBundle = new ShipBundle();
		playerBundle.setFTLConstants( ftlConstants );
		shipBundles.add( playerBundle );
		addBundle( playerBundle, gameState.getPlayerShip(), true );

		if ( gameState.getNearbyShip() != null ) {
			nearbyBundle = new ShipBundle();
			nearbyBundle.setFTLConstants( ftlConstants );
			shipBundles.add( nearbyBundle );
			addBundle( nearbyBundle, gameState.getNearbyShip(), false );

			// Boarder drone body sprites from opposing ships (FTL 1.5.4+).
			// In FTL 1.01-1.03.3, they would've been actual crew.
			for ( ShipBundle bundle : shipBundles ) {
				for ( ShipBundle otherBundle : shipBundles ) {
					if ( bundle == otherBundle ) continue;

					for ( SpriteReference<DroneState> droneRef : bundle.getDroneRefs() ) {
						if ( droneRef.get() == null ) continue;

						DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneRef.get().getDroneId() );
						DroneType droneType = DroneType.findById( droneBlueprint.getType() );
						if ( !DroneType.BOARDER.equals( droneType ) ) continue;

						ExtendedDroneInfo droneInfo = droneRef.get().getExtendedDroneInfo();
						if ( droneInfo == null ) continue;

						DronePodState dronePod = droneInfo.getDronePod();
						if ( dronePod == null ) {
							log.warn( "Boarder drone has extended info but lacks a drone pod!?" );
							continue;
						}

						BoarderDronePodInfo boarderPodInfo = dronePod.getExtendedInfo( BoarderDronePodInfo.class );
						if ( dronePod == null ) {
							log.warn( "Boarder drone has extended info and a pod but lacks extended pod info!?" );
							continue;
						}

						int bodySpriteX = otherBundle.getOriginX() + boarderPodInfo.getBodyX();
						int bodySpriteY = otherBundle.getOriginY() + boarderPodInfo.getBodyY();

						BufferedImage bodyImage = spriteImageProvider.getDroneBodyImage( droneType, droneRef.get().isPlayerControlled() );

						DroneBodySprite droneBodySprite = new DroneBodySprite( droneRef, bodyImage );
						droneBodySprite.setSize( droneBodySprite.getPreferredSize() );
						droneBodySprite.setLocation( bodySpriteX - droneBodySprite.getPreferredSize().width/2, bodySpriteY - droneBodySprite.getPreferredSize().height/2 );
						otherBundle.getDroneBodySprites().add( droneBodySprite );
						shipPanel.add( droneBodySprite, DRONE_LAYER );
					}
				}
			}
		}

		fitViewToViewport();

		for ( ShipBundle bundle : shipBundles ) {
			defaultSelector.addSpriteList( bundle.getDroneBoxSprites() );
			defaultSelector.addSpriteList( bundle.getWeaponSprites() );
			defaultSelector.addSpriteList( bundle.getDoorSprites() );
		}
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

	public void updateShipState( ShipState shipState, ShipBundle shipBundle ) {
		if ( shipState == null ) return;

		ShipBlueprint shipBlueprint = DataManager.get().getShip( shipBundle.getShipBlueprint().getId() );
		ShipLayout shipLayout = DataManager.get().getShipLayout( shipBundle.getShipBlueprint().getLayoutId() );
		ShipChassis shipChassis = DataManager.get().getShipChassis( shipBundle.getShipBlueprint().getLayoutId() );

		shipState.setShipBlueprintId( shipBundle.getShipBlueprint().getId() );
		shipState.setShipLayoutId( shipBundle.getShipBlueprint().getLayoutId() );
		shipState.setShipGraphicsBaseName( shipBundle.getShipGraphicsBaseName() );

		shipState.setReservePowerCapacity( shipBundle.getReservePowerCapacity() );

		// General.
		shipState.setShipName( shipBundle.getShipName() );
		shipState.setHullAmt( shipBundle.getHullAmt() );
		shipState.setFuelAmt( shipBundle.getFuelAmt() );
		shipState.setDronePartsAmt( shipBundle.getDronePartsAmt() );
		shipState.setMissilesAmt( shipBundle.getMissilesAmt() );
		shipState.setScrapAmt( shipBundle.getScrapAmt() );
		shipState.setHostile( shipBundle.isHostile() );
		shipState.setJumpChargeTicks( shipBundle.getJumpChargeTicks() );
		shipState.setJumping( shipBundle.isJumping() );
		shipState.setJumpAnimTicks( shipBundle.getJumpAnimTicks() );
		shipState.setCloakAnimTicks( shipBundle.getCloakAnimTicks() );

		// Augments.
		shipState.getAugmentIdList().clear();
		shipState.getAugmentIdList().addAll( shipBundle.getAugmentIdList() );

		// Drones.
		List<DroneState> droneList = shipState.getDroneList();
		droneList.clear();
		for ( SpriteReference<DroneState> droneRef : shipBundle.getDroneRefs() ) {
			if ( droneRef.get() != null ) {
				DroneState droneState = new DroneState( droneRef.get() );
				droneList.add( droneState );
			}
		}

		// Weapons.
		List<WeaponState> weaponList = shipState.getWeaponList();
		weaponList.clear();
		for ( SpriteReference<WeaponState> weaponRef : shipBundle.getWeaponRefs() ) {
			if ( weaponRef.get() != null ) {
				WeaponState weaponState = new WeaponState( weaponRef.get() );
				weaponList.add( weaponState );
			}
		}

		// Rooms (Fires modify these rooms further).
		List<RoomState> roomList = shipState.getRoomList();
		roomList.clear();
		for ( SpriteReference<RoomState> roomRef : shipBundle.getRoomRefs() ) {
			RoomState roomState = new RoomState( roomRef.get() );
			roomList.add( roomState );
		}

		// Systems.
		shipState.getSystemsMap().clear();

		for ( SpriteReference<SystemState> systemRef : shipBundle.getSystemRefs() ) {
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
		for ( ExtendedSystemInfo info : shipBundle.getExtendedSystemInfoList() ) {
			infoList.add( info.copy() );
		}

		// Breaches.
		Map<XYPair, Integer> breachMap = shipState.getBreachMap();
		breachMap.clear();
		for ( BreachSprite breachSprite : shipBundle.getBreachSprites() ) {
			int roomId = breachSprite.getRoomId();
			int squareId = breachSprite.getSquareId();

			ShipLayoutRoom layoutRoom = shipLayout.getRoom( roomId );
			int squaresH = layoutRoom.squaresH;

			int breachCoordX = layoutRoom.locationX + squareId%squaresH;
			int breachCoordY = layoutRoom.locationY + squareId/squaresH;
			XYPair goofyCoord = new XYPair( shipLayout.getOffsetX() + breachCoordX, shipLayout.getOffsetY() + breachCoordY );
			breachMap.put( goofyCoord, breachSprite.getHealth() );
		}

		// Fires (modifying rooms added above).
		for ( int i=0; i < shipLayout.getRoomCount(); i++ ) {  // Clear existing fires.
			RoomState roomState = shipState.getRoom( i );
			for ( SquareState squareState : roomState.getSquareList() ) {
				squareState.setFireHealth( 0 );
			}
		}
		for ( FireSprite fireSprite : shipBundle.getFireSprites() ) {
			RoomState roomState = shipState.getRoom( fireSprite.getRoomId() );
			SquareState squareState = roomState.getSquareList().get( fireSprite.getSquareId() );
			squareState.setFireHealth( fireSprite.getHealth() );
		}

		// Doors.
		Map<DoorCoordinate, DoorState> shipDoorMap = shipState.getDoorMap();
		shipDoorMap.clear();
		for ( SpriteReference<DoorState> doorRef : shipBundle.getDoorRefs() ) {
			DoorSprite doorSprite = doorRef.getSprite( DoorSprite.class );
			DoorState doorState = new DoorState( doorRef.get() );
			shipDoorMap.put( doorSprite.getCoordinate(), doorState );
		}

		// Crew.
		List<CrewState> crewList = shipState.getCrewList();
		crewList.clear();
		for ( SpriteReference<CrewState> crewRef : shipBundle.getCrewRefs() ) {
			CrewState crewState = new CrewState( crewRef.get() );
			crewList.add( crewState );
		}
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		updateShipState( gameState.getPlayerShip(), playerBundle );

		if ( gameState.getNearbyShip() != null && nearbyBundle != null ) {
			updateShipState( gameState.getNearbyShip(), nearbyBundle );
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
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Select: Room";

			@Override
			public String getDescription() { return desc; }
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				SpriteReference<RoomState> roomRef = shipBundle.getRoomRefs().get( squareCoord.roomId );  // Nth room.
				showRoomEditor( shipBundle, roomRef, squareCoord.squareId );
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

		miscSelector.reset();
		miscSelector.clearSpriteLists();

		for ( ShipBundle bundle : shipBundles ) {
			miscSelector.addSpriteList( bundle.getSystemRoomSprites() );
		}

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

					ShipBundle shipBundle = null;
					for ( ShipBundle bundle : shipBundles ) {
						if ( bundle.getSystemRefs().contains( systemRef ) ) {
							shipBundle = bundle;
							break;
						}
					}

					showSystemEditor( shipBundle, systemRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );
	}

	private void selectCrew() {
		miscSelector.reset();
		miscSelector.clearSpriteLists();

		for ( ShipBundle bundle : shipBundles ) {
			miscSelector.addSpriteList( bundle.getCrewSprites() );
		}

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

					ShipBundle shipBundle = null;
					for ( ShipBundle bundle : shipBundles ) {
						if ( bundle.getCrewRefs().contains( crewRef ) ) {
							shipBundle = bundle;
							break;
						}
					}

					showCrewEditor( shipBundle, crewRef );
				}
				return true;
			}
		});
		miscSelector.setVisible( true );

		showCrewRoster();  // A list of all sprites, including dead crew.
	}

	private void selectBreach() {
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Select: Breach";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( BreachSprite breachSprite : shipBundle.getBreachSprites() ) {
					if ( breachSprite.getRoomId() == squareCoord.roomId && breachSprite.getSquareId() == squareCoord.squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( BreachSprite breachSprite : shipBundle.getBreachSprites() ) {
					if ( breachSprite.getRoomId() == squareCoord.roomId && breachSprite.getSquareId() == squareCoord.squareId ) {
						showBreachEditor( shipBundle, breachSprite );
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
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Select: Fire";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( FireSprite fireSprite : shipBundle.getFireSprites() ) {
					if ( fireSprite.getRoomId() == squareCoord.roomId && fireSprite.getSquareId() == squareCoord.squareId ) {
						return true;
					}
				}
				return false;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( FireSprite fireSprite : shipBundle.getFireSprites() ) {
					if ( fireSprite.getRoomId() == squareCoord.roomId && fireSprite.getSquareId() == squareCoord.squareId ) {
						showFireEditor( shipBundle, fireSprite );
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
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Add: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( RoomAndSquare ras : shipBundle.getBlockedRasList() ) {
					if ( ras.roomId == squareCoord.roomId  && ras.squareId == squareCoord.squareId ) return false;
				}

				// TODO: Check friendly drone body sprites.

				for ( SpriteReference<CrewState> crewRef : shipBundle.getCrewRefs() ) {
					if ( crewRef.get().getRoomId() == squareCoord.roomId && crewRef.get().getRoomSquare() == squareCoord.squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				Point center = squareSelector.getSquareCenter();
				CrewState crewState = new CrewState();
				crewState.setHealth( crewState.getRace().getMaxHealth() );
				crewState.setPlayerControlled( true );
				crewState.setRoomId( squareCoord.roomId );
				crewState.setRoomSquare( squareCoord.squareId );
				crewState.setSpriteX( center.x - shipBundle.getOriginX() );
				crewState.setSpriteY( center.y - shipBundle.getOriginY() );
				crewState.setSavedRoomId( squareCoord.roomId );
				crewState.setSavedRoomSquare( squareCoord.squareId );
				crewState.setMale( DataManager.get().getCrewSex() );
				crewState.setName( DataManager.get().getCrewName( crewState.isMale() ) );

				SpriteReference<CrewState> crewRef = new SpriteReference<CrewState>( crewState );
				shipBundle.getCrewRefs().add( crewRef );

				addCrewSprite( shipBundle, crewRef, center.x, center.y );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible( true );
	}

	private void addBreach() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Add: Breach";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( RoomAndSquare ras : shipBundle.getBlockedRasList() ) {
					if ( ras.roomId == squareCoord.roomId  && ras.squareId == squareCoord.squareId ) return false;
				}

				for ( BreachSprite breachSprite : shipBundle.getBreachSprites() ) {
					if ( breachSprite.getRoomId() == squareCoord.roomId && breachSprite.getSquareId() == squareCoord.squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				Point center = squareSelector.getSquareCenter();
				addBreachSprite( shipBundle, center.x, center.y, squareCoord.roomId, squareCoord.squareId, 100 );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible( true );
	}

	private void addFire() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Add: Fire";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( RoomAndSquare ras : shipBundle.getBlockedRasList() ) {
					if ( ras.roomId == squareCoord.roomId && ras.squareId == squareCoord.squareId ) return false;
				}

				for ( FireSprite fireSprite : shipBundle.getFireSprites() ) {
					if ( fireSprite.getRoomId() == squareCoord.roomId && fireSprite.getSquareId() == squareCoord.squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				Point center = squareSelector.getSquareCenter();
				addFireSprite( shipBundle, center.x, center.y, squareCoord.roomId, squareCoord.squareId, 100 );
				shipViewport.repaint();
				return true;
			}
		});
		squareSelector.setVisible( true );
	}

	private void selectGeneral() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Select: Ship (General)";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				showGeneralEditor( shipBundle );
				return false;
			}
		});
		squareSelector.setVisible( true );
	}

	private void selectAugments() {
		clearSidePanel();
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Select: Ship (Augments)";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				showAugmentsEditor( shipBundle );
				return false;
			}
		});
		squareSelector.setVisible( true );
	}

	private void moveCrew( final SpriteReference<CrewState> mobileRef ) {
		squareSelector.reset();
		squareSelector.clearSquarsCoordMap();

		for ( ShipBundle bundle : shipBundles ) {
			// Only move within their current ship.
			if ( bundle.getCrewRefs().contains( mobileRef ) ) {
				squareSelector.putSquareRegionCoordMap( bundle.getSquareRegionCoordMap() );
			}
		}

		squareSelector.setCriteria(new SquareCriteria<FloorplanCoord>() {
			private final String desc = "Move: Crew";

			@Override
			public String getDescription() { return desc; }

			@Override
			public boolean isSquareValid( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				if ( squareCoord == null ) return false;
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				for ( RoomAndSquare ras : shipBundle.getBlockedRasList() ) {
					if ( ras.roomId == squareCoord.roomId  && ras.squareId == squareCoord.squareId ) return false;
				}

				// TODO: Check friendly drone body sprites.

				for ( SpriteReference<CrewState> crewRef : shipBundle.getCrewRefs() ) {
					if ( crewRef.get().getRoomId() == squareCoord.roomId && crewRef.get().getRoomSquare() == squareCoord.squareId ) {
						return false;
					}
				}
				return true;
			}
		});
		squareSelector.setCallback(new SquareSelectionCallback<FloorplanCoord>() {
			@Override
			public boolean squareSelected( SquareSelector squareSelector, FloorplanCoord squareCoord ) {
				ShipBundle shipBundle = shipBundles.get( squareCoord.bundleId );

				Point center = squareSelector.getSquareCenter();
				CrewSprite mobileSprite = mobileRef.getSprite( CrewSprite.class );

				int oldSpriteRoomId = shipBundle.getSpriteRoomId( mobileSprite );  // Actual sprite location, if walking.

				mobileSprite.setLocation( center.x - mobileSprite.getSize().width/2, center.y - mobileSprite.getSize().height/2 );
				mobileRef.get().setRoomId( squareCoord.roomId );
				mobileRef.get().setRoomSquare( squareCoord.squareId );
				mobileRef.get().setSpriteX( center.x - shipBundle.getOriginX() );
				mobileRef.get().setSpriteY( center.y - shipBundle.getOriginY() );
				mobileRef.fireReferenceChange();

				return false;
			}
		});
		shipViewport.setStatusString( squareSelector.getCriteria().getDescription() +"   (Right-click to cancel)" );
		squareSelector.setVisible( true );
	}

	private void addDroneSprite( ShipBundle shipBundle, SpriteReference<DroneState> droneRef, int centerX, int centerY, int slot ) {
		// Represent the slot whether or not there's a drone equipped in it.
		DroneBoxSprite droneBoxSprite = new DroneBoxSprite( droneRef, slot );
		droneBoxSprite.setSize( droneBoxSprite.getPreferredSize() );
		droneBoxSprite.setLocation( centerX - droneBoxSprite.getPreferredSize().width/2, centerY - droneBoxSprite.getPreferredSize().height/2 );
		shipBundle.getDroneBoxSprites().add( droneBoxSprite );
		shipPanel.add( droneBoxSprite, DRONE_LAYER );

		if ( droneRef.get() != null ) {
			DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneRef.get().getDroneId() );
			DroneType droneType = DroneType.findById( droneBlueprint.getType() );

			if ( DroneType.BATTLE.equals( droneType ) || DroneType.REPAIR.equals( droneType ) ) {
				int bodySpriteX = shipBundle.getOriginX() + droneRef.get().getBodyX();
				int bodySpriteY = shipBundle.getOriginY() + droneRef.get().getBodyY();

				BufferedImage bodyImage = spriteImageProvider.getDroneBodyImage( droneType, droneRef.get().isPlayerControlled() );

				DroneBodySprite droneBodySprite = new DroneBodySprite( droneRef, bodyImage );
				droneBodySprite.setSize( droneBodySprite.getPreferredSize() );
				droneBodySprite.setLocation( bodySpriteX - droneBodySprite.getPreferredSize().width/2, bodySpriteY - droneBodySprite.getPreferredSize().height/2 );
				shipBundle.getDroneBodySprites().add( droneBodySprite );
				shipPanel.add( droneBodySprite, DRONE_LAYER );
			}
		}
	}

	private void addWeaponSprite( ShipBundle shipBundle, SpriteReference<WeaponState> weaponRef, ShipChassis.WeaponMount weaponMount, int slot ) {
		ShipChassis shipChassis = shipBundle.getShipChassis();

		WeaponSprite weaponSprite = new WeaponSprite( weaponRef, slot, weaponMount.rotate );
		weaponSprite.setSize( weaponSprite.getPreferredSize() );

		if ( weaponMount.rotate ) {
			// Right of x,y and centered vertically.
			weaponSprite.setLocation( shipBundle.getLayoutX() + shipChassis.getImageBounds().x + weaponMount.x, shipBundle.getLayoutY() + shipChassis.getImageBounds().y + weaponMount.y - weaponSprite.getPreferredSize().height/2 );
		}
		else {
			// Above x,y and centered horizontally.
			weaponSprite.setLocation( shipBundle.getLayoutX() + shipChassis.getImageBounds().x + weaponMount.x - weaponSprite.getPreferredSize().width/2, shipBundle.getLayoutY() + shipChassis.getImageBounds().y + weaponMount.y - weaponSprite.getPreferredSize().height );
		}
		shipBundle.getWeaponSprites().add( weaponSprite );
		shipPanel.add( weaponSprite, WEAPON_LAYER );
	}

	private void addDoorSprite( ShipBundle shipBundle, SpriteReference<DoorState> doorRef, int centerX, int centerY, int level, DoorCoordinate doorCoord ) {
		DoorAtlas doorAtlas = spriteImageProvider.getDoorAtlas();

		DoorSprite doorSprite = new DoorSprite( doorRef, doorAtlas, level, doorCoord );
		doorSprite.setSize( doorSprite.getPreferredSize() );
		doorSprite.setLocation( centerX - doorSprite.getPreferredSize().width/2, centerY - doorSprite.getPreferredSize().height/2 );
		shipBundle.getDoorSprites().add( doorSprite );
		shipPanel.add( doorSprite, DOOR_LAYER );
	}

	private void addSystemRoomSprite( ShipBundle shipBundle, SpriteReference<SystemState> systemRef, int centerX, int centerY ) {
		BufferedImage overlayImage = spriteImageProvider.getSystemRoomImage( systemRef.get().getSystemType() );

		SystemRoomSprite systemRoomSprite = new SystemRoomSprite( systemRef, overlayImage );
		systemRoomSprite.setSize( systemRoomSprite.getPreferredSize() );
		systemRoomSprite.setLocation( centerX - systemRoomSprite.getPreferredSize().width/2, centerY - systemRoomSprite.getPreferredSize().height/2 );
		shipBundle.getSystemRoomSprites().add( systemRoomSprite );
		shipPanel.add( systemRoomSprite, SYSTEM_LAYER );
	}

	private void addBreachSprite( ShipBundle shipBundle, int centerX, int centerY, int roomId, int squareId, int health ) {
		AnimAtlas breachAtlas = spriteImageProvider.getBreachAtlas();

		BreachSprite breachSprite = new BreachSprite( breachAtlas, roomId, squareId, health );
		breachSprite.setSize( breachSprite.getPreferredSize() );
		breachSprite.setLocation( centerX - breachSprite.getPreferredSize().width/2, centerY - breachSprite.getPreferredSize().height/2 );
		shipBundle.getBreachSprites().add( breachSprite );
		shipPanel.add( breachSprite, BREACH_LAYER );
	}

	private void addFireSprite( ShipBundle shipBundle, int centerX, int centerY, int roomId, int squareId, int health ) {
		AnimAtlas fireAtlas = spriteImageProvider.getFireAtlas();

		FireSprite fireSprite = new FireSprite( fireAtlas, roomId, squareId, health );
		fireSprite.setSize( fireSprite.getPreferredSize() );
		fireSprite.setLocation( centerX - fireSprite.getPreferredSize().width/2, centerY - fireSprite.getPreferredSize().height/2 );
		shipBundle.getFireSprites().add( fireSprite );
		shipPanel.add( fireSprite, FIRE_LAYER );
	}

	private void addCrewSprite( ShipBundle shipBundle, SpriteReference<CrewState> crewRef, int centerX, int centerY ) {
		CrewSprite crewSprite = new CrewSprite( crewRef, spriteImageProvider );
		crewSprite.setSize( crewSprite.getPreferredSize() );
		crewSprite.setLocation( centerX - crewSprite.getPreferredSize().width/2, centerY - crewSprite.getPreferredSize().height/2 );
		shipBundle.getCrewSprites().add( crewSprite );
		shipPanel.add( crewSprite, CREW_LAYER );
	}

	/**
	 * Disarms a drone.
	 *
	 * This method will not notify that the reference has changed.
	 *
	 * This method will not update system power usage.
	 *
	 * TODO: Make this viable beyond FTL 1.03.3.
	 */
	private void disarmDrone( ShipBundle shipBundle, SpriteReference<DroneState> droneRef ) {
		if ( droneRef.get() == null ) return;

		droneRef.get().setArmed( false );

		// Only meaningful for drone types with local bodies.
		// Others should always have these values.
		droneRef.get().setBodyRoomId( -1 );
		droneRef.get().setBodyRoomSquare( -1 );
		droneRef.get().setBodyX( -1 );
		droneRef.get().setBodyY( -1 );

		// TODO: Extended info, pods, etc.
	}

	private void unequipDrone( ShipBundle shipBundle, SpriteReference<DroneState> droneRef ) {
		if ( droneRef.get() == null ) return;

		DroneBodySprite droneBodySprite = droneRef.getSprite( DroneBodySprite.class );
		if ( droneBodySprite != null ) {
			droneRef.removeSprite( droneBodySprite );
			shipBundle.getDroneBodySprites().remove( droneBodySprite );
			shipPanel.remove( droneBodySprite );
		}

		droneRef.set( null );
	}

	/**
	 * Attempts to place an armed drone's body in a room.
	 *
	 * The DroneBodySprite will be added, if absent, and moved to match the
	 * DroneState. If the DroneState doesn't have a roomId set, an unoccupied
	 * square will be assigned first.
	 *
	 * This begins scanning the Drone_Ctrl room, then progressively expanding
	 * through adjacent rooms. Squares are occupied by drone bodies and crew
	 * (of the same playerControlled status, or mind controlled). Blocked
	 * squares are considered occupied.
	 *
	 * Note: It is possible that the body will not be placed. Check the roomId
	 * afterward, and disarm if necessary.
	 *
	 * Boarder bodies are crew on foreign ships. This method will not affect
	 * them.
	 *
	 * TODO: Blocked squares.
	 * TODO: Mimic FTL's algorithm.
	 */
	private void placeDroneBody( ShipBundle shipBundle, SpriteReference<DroneState> droneRef ) {
		if ( droneRef.get() == null ) return;

		DroneBlueprint droneBlueprint = DataManager.get().getDrone( droneRef.get().getDroneId() );
		DroneType droneType = DroneType.findById( droneBlueprint.getType() );

		// Don't bother if not a type that has a body.
		if ( !DroneType.BATTLE.equals( droneType ) && !DroneType.REPAIR.equals( droneType ) ) {
			return;
		}

		// Don't bother if not armed.
		if ( !droneRef.get().isArmed() ) return;

		if ( droneRef.get().getBodyRoomId() < 0 ) {

			// FTL avoids squares with player-controlled crew/drones.
			// FTL places bodies first in DroneCtrl, then in spatially
			// nearby rooms. Possibly prioritizing station squares.
			// RoomId does not seem relevant. Algorithm unknown.
			// ShipLayout Door order (original or sorted)?

			boolean playerControlled = droneRef.get().isPlayerControlled();

			List<Integer> candidateRoomIds = new ArrayList<Integer>();

			int[] droneSystemRoomId = shipBundle.getShipBlueprint().getSystemList().getRoomIdBySystemType( SystemType.DRONE_CTRL );
			if ( droneSystemRoomId != null ) {
				candidateRoomIds.add( droneSystemRoomId[0] );
			}

			for ( int i=0; i < candidateRoomIds.size(); i++ ) {
				int roomId = candidateRoomIds.get( i );

				ShipLayoutRoom layoutRoom = shipBundle.getShipLayout().getRoom( roomId );
				int squaresH = layoutRoom.squaresH;
				int squaresV = layoutRoom.squaresV;
				int roomCoordX = layoutRoom.locationX;
				int roomCoordY = layoutRoom.locationY;
				int roomX = shipBundle.getLayoutX() + layoutRoom.locationX*squareSize;
				int roomY = shipBundle.getLayoutY() + layoutRoom.locationY*squareSize;

				for ( int s=0; s < squaresH * squaresV; s++ ) {
					boolean occupied = false;

					// Check blocked squares.
					for ( RoomAndSquare ras : shipBundle.getBlockedRasList() ) {
						if ( ras.roomId == roomId && ras.squareId == s ) {
							occupied = true;
							break;
						}
					}
					// Check crew.
					if ( !occupied ) {
						for ( SpriteReference<CrewState> crewRef : shipBundle.getCrewRefs() ) {
							if ( crewRef.get().getRoomId() == roomId
								&& crewRef.get().getRoomSquare() == s
								&& crewRef.get().getHealth() > 0
								&& (crewRef.get().isPlayerControlled() == playerControlled
									|| crewRef.get().isMindControlled()) ) {

								occupied = true;
								break;
							}
						}
					}
					// Check drone bodies.
					if ( !occupied ) {
						for ( SpriteReference<DroneState> otherDroneRef : shipBundle.getDroneRefs() ) {
							if ( otherDroneRef.get() != null
								&& otherDroneRef.get().getBodyRoomId() == roomId
								&& otherDroneRef.get().getBodyRoomSquare() == s
								&& otherDroneRef.get().isArmed()
								&& otherDroneRef.get().isPlayerControlled() == playerControlled ) {

								occupied = true;
								break;
							}
						}
					}

					// Place the body.
					if ( !occupied ) {
						int bodyX = shipBundle.getShipLayout().getOffsetX()*squareSize + roomCoordX*squareSize + (s%squaresH)*squareSize + squareSize/2;
						int bodyY = shipBundle.getShipLayout().getOffsetY()*squareSize + roomCoordY*squareSize + (s/squaresH)*squareSize + squareSize/2;
						droneRef.get().setBodyRoomId( roomId );
						droneRef.get().setBodyRoomSquare( s );
						droneRef.get().setBodyX( bodyX );
						droneRef.get().setBodyY( bodyY );
						break;
					}
				}

				if ( droneRef.get().getBodyRoomId() >= 0 ) {
					break;  // Done scanning rooms.
				}
				else {
					// Add adjacent rooms to the candidates, while looping.
					for ( Integer otherRoomId : shipBundle.getShipLayout().getAdjacentRoomIds( roomId ) ) {
						if ( !candidateRoomIds.contains( otherRoomId ) ) {
							candidateRoomIds.add( otherRoomId );
						}
					}
				}
			}
		}

		if ( droneRef.get().getBodyRoomId() >= 0 ) {  // Place the body sprite.

			DroneBodySprite droneBodySprite = droneRef.getSprite( DroneBodySprite.class );

			if ( droneBodySprite == null ) {
				BufferedImage bodyImage = spriteImageProvider.getDroneBodyImage( droneType, droneRef.get().isPlayerControlled() );

				droneBodySprite = new DroneBodySprite( droneRef, bodyImage );
				droneBodySprite.setSize( droneBodySprite.getPreferredSize() );
				shipBundle.getDroneBodySprites().add( droneBodySprite );
				shipPanel.add( droneBodySprite, DRONE_LAYER );
			}

			int bodySpriteX = shipBundle.getOriginX() + droneRef.get().getBodyX();
			int bodySpriteY = shipBundle.getOriginY() + droneRef.get().getBodyY();
			droneBodySprite.setLocation( bodySpriteX - droneBodySprite.getPreferredSize().width/2, bodySpriteY - droneBodySprite.getPreferredSize().height/2 );
		}
		else {
			log.warn( "Failed to place an armed drone's body in a room: "+ droneRef.get().getDroneId() );
		}
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

	private void showGeneralEditor( final ShipBundle shipBundle ) {
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
		editorPanel.getSlider( HULL ).setMaximum( shipBundle.getShipBlueprint().getHealth().amount );
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

		editorPanel.getString( SHIP_NAME ).setText( shipBundle.getShipName() );
		editorPanel.getSlider( HULL ).setValue( shipBundle.getHullAmt() );
		editorPanel.getInt( FUEL ).setText( ""+ shipBundle.getFuelAmt() );
		editorPanel.getInt( DRONE_PARTS ).setText( ""+ shipBundle.getDronePartsAmt() );
		editorPanel.getInt( MISSILES ).setText( ""+ shipBundle.getMissilesAmt() );
		editorPanel.getInt( SCRAP ).setText( ""+ shipBundle.getScrapAmt() );
		editorPanel.getBoolean( HOSTILE ).setSelected( shipBundle.isHostile() );
		editorPanel.getInt( JUMP_CHARGE_TICKS ).setText( ""+ shipBundle.getJumpChargeTicks() );
		editorPanel.getBoolean( JUMPING ).setSelected( shipBundle.isJumping() );
		editorPanel.getSlider( JUMP_ANIM_TICKS ).setValue( shipBundle.getJumpAnimTicks() );
		editorPanel.getSlider( CLOAK_ANIM_TICKS ).setValue( shipBundle.getCloakAnimTicks() );

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
				if ( newString.length() > 0 ) shipBundle.setShipName( newString );

				shipBundle.setHullAmt( editorPanel.getSlider( HULL ).getValue() );

				try { shipBundle.setFuelAmt( editorPanel.parseInt( FUEL ) ); }
				catch ( NumberFormatException e ) {}

				try { shipBundle.setDronePartsAmt( editorPanel.parseInt( DRONE_PARTS ) ); }
				catch ( NumberFormatException e ) {}

				try { shipBundle.setMissilesAmt( editorPanel.parseInt( MISSILES ) ); }
				catch ( NumberFormatException e ) {}

				try { shipBundle.setScrapAmt( editorPanel.parseInt( SCRAP ) ); }
				catch ( NumberFormatException e ) {}

				shipBundle.setHostile( editorPanel.getBoolean( HOSTILE ).isSelected() );

				try { shipBundle.setJumpChargeTicks( editorPanel.parseInt( JUMP_CHARGE_TICKS ) ); }
				catch ( NumberFormatException e ) {}

				shipBundle.setJumping( editorPanel.getBoolean( JUMPING ).isSelected() );

				shipBundle.setJumpAnimTicks( editorPanel.getSlider( JUMP_ANIM_TICKS ).getValue() );

				shipBundle.setCloakAnimTicks( editorPanel.getSlider( CLOAK_ANIM_TICKS ).getValue() );

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showAugmentsEditor( final ShipBundle shipBundle ) {
		final String DESC = "Desc";
		final String ID_ONE = "#1";
		final String ID_TWO = "#2";
		final String ID_THREE = "#3";
		final String[] augSlots = new String[] { ID_ONE, ID_TWO, ID_THREE };

		final List<String> shipAugmentIdList = shipBundle.getAugmentIdList();

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

	private void showDroneEditor( final ShipBundle shipBundle, final SpriteReference<DroneState> droneRef ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "DroneId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String HEALTH = "Health";
		final String PLAYER_CONTROLLED = "Player Ctrl";

		final Map<String, DroneBlueprint> allDronesMap = DataManager.get().getDrones();

		SpriteReference<SystemState> droneSystemRef = shipBundle.getSystemRef( SystemType.DRONE_CTRL );
		if ( droneSystemRef == null || droneSystemRef.get().getCapacity() == 0 ) {
			JOptionPane.showMessageDialog( frame, "A Drone Control system must be present with capacity > 0 before adding drones.", "System Not Installed", JOptionPane.WARNING_MESSAGE );
			return;
		}

		int otherDronesDemand = 0;
		for ( SpriteReference<DroneState> otherDroneRef : shipBundle.getDroneRefs() ) {
			if ( otherDroneRef != droneRef && otherDroneRef.get() != null && otherDroneRef.get().isArmed() ) {
				otherDronesDemand += allDronesMap.get( otherDroneRef.get().getDroneId() ).getPower();
			}
		}

		DroneBoxSprite droneBoxSprite = droneRef.getSprite( DroneBoxSprite.class );
		int droneSystemCapacity = droneSystemRef.get().getCapacity();
		int droneSystemDamage = droneSystemRef.get().getDamagedBars();
		final int excludedReservePool = shipBundle.getReservePool( droneSystemRef );
		final int excludedBatteryPool = shipBundle.getBatteryPool( droneSystemRef );
		final int zoltanBars = shipBundle.getRoomZoltanEnergy( shipBundle.getSpriteRoomId( droneBoxSprite ) );
		final int availablePower = Math.min( excludedReservePool + excludedBatteryPool + zoltanBars, droneSystemRef.get().getUsableCapacity() ) - otherDronesDemand;

		String title = String.format("Drone %d", droneBoxSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( AVAILABLE_POWER ).setMaximum( shipBundle.getReservePowerCapacity() );
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

			DroneType selectedType = DroneType.findById( selectedBlueprint.getType() );
			if ( selectedType == null ) {
				throw new IllegalArgumentException( "Selected blueprint has an unsupported DroneType: "+ selectedBlueprint.getType() );
			}

			editorPanel.getSlider( AVAILABLE_POWER ).setValue( availablePower - (armable && droneRef.get().isArmed() ? selectedBlueprint.getPower() : 0) );
			editorPanel.getCombo( ID ).setSelectedItem( selectedBlueprint );
			editorPanel.getWrappedLabel( DESC ).setText( selectedBlueprint.getDescription().getTextValue() );
			editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
			editorPanel.getBoolean( ARMED ).setEnabled( armable );
			editorPanel.getBoolean( PLAYER_CONTROLLED ).setSelected( droneRef.get().isPlayerControlled() );
			editorPanel.getSlider( HEALTH ).setMaximum( selectedType.getMaxHealth() );
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
					DroneBlueprint droneBlueprint = ((DroneBlueprint)blueprintObj);

					String droneId = droneBlueprint.getId();
					DroneType droneType = DroneType.findById( droneBlueprint.getType() );
					boolean blueprintChanged = false;

					if ( droneRef.get() == null ) {
						droneRef.set( new DroneState() );
					}
					else if ( !droneId.equals( droneRef.get().getDroneId() ) ) {
						blueprintChanged = true;  // TODO: unequipDrone()?

						// Remove the old body. A new one might get placed below.
						DroneBodySprite droneBodySprite = droneRef.getSprite( DroneBodySprite.class );
						if ( droneBodySprite != null ) {
							droneRef.removeSprite( droneBodySprite );
							shipBundle.getDroneBodySprites().remove( droneBodySprite );
							shipPanel.remove( droneBodySprite );
						}
					}
					droneRef.get().setDroneId( droneId );

					droneRef.get().setArmed( editorPanel.getBoolean( ARMED ).isSelected() );
					droneRef.get().setPlayerControlled( editorPanel.getBoolean( PLAYER_CONTROLLED ).isSelected() );
					droneRef.get().setHealth( editorPanel.getSlider( HEALTH ).getValue() );

					if ( DroneType.BATTLE.equals( droneType ) || DroneType.REPAIR.equals( droneType ) ) {
						if ( droneRef.get().isArmed() ) {
							placeDroneBody( shipBundle, droneRef );

							if ( droneRef.get().getBodyRoomId() < 0 ) {  // Body placement failed.
								disarmDrone( shipBundle, droneRef );
							}
						}
					}
					// TODO: Extended info, Boarder drone crew on the other ship, etc.
				}
				else {
					unequipDrone( shipBundle, droneRef );
				}
				droneRef.fireReferenceChange();

				// Set the Drones system power based on all armed drones.
				SpriteReference<SystemState> droneSystemRef = shipBundle.getSystemRef( SystemType.DRONE_CTRL );
				int usableCapacity = droneSystemRef.get().getUsableCapacity();
				int prevSystemPower = droneSystemRef.get().getPower();
				int prevSystemBattery = droneSystemRef.get().getBatteryPower();
				int totalBars = prevSystemPower + prevSystemBattery + zoltanBars;
				int totalDemand = 0;
				boolean disarming = false;

				// Tally up demand from drones until bars are insufficient, allocate
				// more bars if possible, then disarm the rest.
				for ( SpriteReference<DroneState> otherDroneRef : shipBundle.getDroneRefs() ) {
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
							disarmDrone( shipBundle, otherDroneRef );
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
				shipBundle.updateBatteryPool();

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

						DroneType selectedType = DroneType.findById( selectedBlueprint.getType() );
						if ( selectedType == null ) {
							throw new IllegalArgumentException( "Selected blueprint has an unsupported DroneType: "+ selectedBlueprint.getType() );
						}

						editorPanel.getWrappedLabel( DESC ).setText( ""+selectedBlueprint.getDescription() );
						editorPanel.getLabel( POWER_REQ ).setText( ""+selectedBlueprint.getPower() );
						healthSlider.setMaximum( selectedType.getMaxHealth() );
						healthSlider.setValue( selectedType.getMaxHealth() );

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

	private void showWeaponEditor( final ShipBundle shipBundle, final SpriteReference<WeaponState> weaponRef ) {
		final String AVAILABLE_POWER = "Available Power";
		final String ID = "WeaponId";
		final String DESC = "Desc";
		final String POWER_REQ = "Power Req";
		final String ARMED = "Armed";
		final String COOLDOWN_TICKS = "Cooldown Ticks";

		final Map<String, WeaponBlueprint> allWeaponsMap = DataManager.get().getWeapons();

		SpriteReference<SystemState> weaponSystemRef = shipBundle.getSystemRef( SystemType.WEAPONS );
		if ( weaponSystemRef == null || weaponSystemRef.get().getCapacity() == 0 ) {
			JOptionPane.showMessageDialog( frame, "A weapons system must be present with capacity > 0 before adding weapons.", "System Not Installed", JOptionPane.WARNING_MESSAGE );
			return;
		}

		int otherWeaponsDemand = 0;
		for ( SpriteReference<WeaponState> otherWeaponRef : shipBundle.getWeaponRefs() ) {
			if ( otherWeaponRef != weaponRef && otherWeaponRef.get() != null && otherWeaponRef.get().isArmed() ) {
				otherWeaponsDemand += allWeaponsMap.get( otherWeaponRef.get().getWeaponId() ).getPower();
			}
		}

		final int excludedReservePool = shipBundle.getReservePool( weaponSystemRef );
		final int excludedBatteryPool = shipBundle.getBatteryPool( weaponSystemRef );
		final int zoltanBars = shipBundle.getRoomZoltanEnergy( shipBundle.getSpriteRoomId( weaponSystemRef.getSprite( SystemRoomSprite.class ) ) );
		final int availablePower = Math.min( excludedReservePool + excludedBatteryPool + zoltanBars, weaponSystemRef.get().getUsableCapacity() ) - otherWeaponsDemand;

		WeaponSprite weaponSprite = weaponRef.getSprite( WeaponSprite.class );

		String title = String.format("Weapon %d", weaponSprite.getSlot()+1);

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( AVAILABLE_POWER, FieldEditorPanel.ContentType.SLIDER );
		editorPanel.getSlider( AVAILABLE_POWER ).setMaximum( shipBundle.getReservePowerCapacity() );
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
			editorPanel.getCombo( ID ).addItem( weaponBlueprint );

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
				SpriteReference<SystemState> weaponSystemRef = shipBundle.getSystemRef( SystemType.WEAPONS );
				int usableCapacity = weaponSystemRef.get().getUsableCapacity();
				int prevSystemPower = weaponSystemRef.get().getPower();
				int prevSystemBattery = weaponSystemRef.get().getBatteryPower();
				int totalBars = prevSystemPower + prevSystemBattery + zoltanBars;
				int totalDemand = 0;
				boolean disarming = false;

				// Tally up demand from weapons until bars are insufficient, allocate
				// more bars if possible, then disarm the rest.
				for ( SpriteReference<WeaponState> otherWeaponRef : shipBundle.getWeaponRefs() ) {
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
				shipBundle.updateBatteryPool();

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

	private void showRoomEditor( final ShipBundle shipBundle, final SpriteReference<RoomState> roomRef, final int squareId ) {
		final String OXYGEN = "Oxygen";
		final String STATION_HERE = "Station Here";
		final String STATION_DIR = "Station Direction";
		final String IGNITION = "Ignition Progress";
		final String EXTINGUISHMENT = "Extinguishment Progress";

		RoomSprite roomSprite = roomRef.getSprite( RoomSprite.class );
		String title = String.format( "Room %2d (Square %d)", roomSprite.getRoomId(), squareId );

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

	private void showSystemEditor( final ShipBundle shipBundle, final SpriteReference<SystemState> systemRef ) {
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
		Integer maxPowerOverride = shipBundle.getShipBlueprint().getSystemList().getSystemRoom( systemRef.get().getSystemType() )[0].getMaxPower();
		if ( maxPowerOverride != null ) {
			maxSystemCapacity = maxPowerOverride.intValue();
		}

		final int batteryCapacity = shipBundle.getBatteryPoolCapacity();
		final int excludedReservePool = shipBundle.getReservePool( systemRef );
		final int excludedBatteryPool = shipBundle.getBatteryPool( systemRef );
		final int zoltanBars = shipBundle.getRoomZoltanEnergy( shipBundle.getSpriteRoomId( systemRef.getSprite( SystemRoomSprite.class ) ) );

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
		editorPanel.getSlider( RESERVE_CAPACITY ).setValue( shipBundle.getReservePowerCapacity() );
		editorPanel.getSlider( RESERVE_POWER ).setMaximum( shipBundle.getReservePowerCapacity() );
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
				shipBundle.setReservePowerCapacity( editorPanel.getSlider( RESERVE_CAPACITY ).getValue() );

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
						for ( SpriteReference<WeaponState> weaponRef : shipBundle.getWeaponRefs() ) {
							weaponRef.set( null );
							weaponRef.fireReferenceChange();
						}
					}
					else {
						// Disarm everything rightward of first underpowered weapon.
						int weaponPower = 0;
						for ( SpriteReference<WeaponState> weaponRef : shipBundle.getWeaponRefs() ) {
							if ( weaponRef.get() != null && weaponRef.get().isArmed() ) {
								weaponPower += DataManager.get().getWeapon( weaponRef.get().getWeaponId() ).getPower();
								if ( weaponPower > systemPower ) weaponRef.get().setArmed( false );
							}
						}
						// Re-calc system power based on all armed weapons.
						systemPower = 0;
						for ( SpriteReference<WeaponState> weaponRef : shipBundle.getWeaponRefs() ) {
							if ( weaponRef.get() != null && weaponRef.get().isArmed() ) {
								systemPower += DataManager.get().getWeapon( weaponRef.get().getWeaponId() ).getPower();
							}
						}
						systemRef.get().setPower( systemPower );
						systemRef.get().setBatteryPower( systemBattery );
						shipBundle.updateBatteryPool();
					}
				}
				else if ( SystemType.DRONE_CTRL.equals( systemRef.get().getSystemType() ) ) {
					if ( systemRef.get().getCapacity() == 0 ) {
						// When capacity is 0, nullify all drones.
						for ( SpriteReference<DroneState> droneRef : shipBundle.getDroneRefs() ) {
							unequipDrone( shipBundle, droneRef );
							droneRef.fireReferenceChange();
						}
					}
					else {
						// Disarm everything rightward of first underpowered drone.
						int dronePower = 0;
						for ( SpriteReference<DroneState> droneRef : shipBundle.getDroneRefs() ) {
							if ( droneRef.get() != null && droneRef.get().isArmed() ) {
								dronePower += DataManager.get().getDrone( droneRef.get().getDroneId() ).getPower();
								if ( dronePower > systemPower ) {
									disarmDrone( shipBundle, droneRef );
									droneRef.fireReferenceChange();
								}
							}
						}
						// Re-calc system power based on all armed drones.
						systemPower = 0;
						for ( SpriteReference<DroneState> droneRef : shipBundle.getDroneRefs() ) {
							if ( droneRef.get() != null && droneRef.get().isArmed() ) {
								systemPower += DataManager.get().getDrone( droneRef.get().getDroneId() ).getPower();
							}
						}
						systemRef.get().setPower( systemPower );
						systemRef.get().setBatteryPower( systemBattery );
						shipBundle.updateBatteryPool();
					}
				}
				else {
					systemRef.get().setPower( systemPower );
					systemRef.get().setBatteryPower( systemBattery );
					shipBundle.updateBatteryPool();
				}
				systemRef.fireReferenceChange();

				if ( SystemType.DOORS.equals( systemRef.get().getSystemType() ) ) {
					for ( SpriteReference<DoorState> doorRef : shipBundle.getDoorRefs() ) {
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

	private void showBreachEditor( final ShipBundle shipBundle, final BreachSprite breachSprite ) {
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
		sidePanel.add( removeBtn );

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				shipBundle.getBreachSprites().remove( breachSprite );
				shipPanel.remove( breachSprite );
			}
		});

		showSidePanel();
	}

	private void showFireEditor( final ShipBundle shipBundle, final FireSprite fireSprite ) {
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
		sidePanel.add( removeBtn );

		removeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearSidePanel();
				shipBundle.getFireSprites().remove( fireSprite );
				shipPanel.remove( fireSprite );
			}
		});

		showSidePanel();
	}

	private void showDoorEditor( final ShipBundle shipBundle, final SpriteReference<DoorState> doorRef ) {
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
		editorPanel.getInt( MAX_HEALTH ).setText( ""+ doorRef.get().getCurrentMaxHealth() );
		editorPanel.getInt( HEALTH ).setText( ""+ doorRef.get().getHealth() );
		editorPanel.getInt( NOMINAL_HEALTH ).setText( ""+ doorRef.get().getNominalHealth() );
		editorPanel.getInt( DELTA ).setText( ""+ doorRef.get().getUnknownDelta() );
		editorPanel.getInt( EPSILON ).setText( ""+ doorRef.get().getUnknownEpsilon() );

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

		final ShipBundle shipBundle = playerBundle;  // TODO: Select ship!

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( CREW, FieldEditorPanel.ContentType.COMBO );

		for ( SpriteReference<CrewState> crewRef : shipBundle.getCrewRefs() ) {
			CrewSprite crewSprite = crewRef.getSprite( CrewSprite.class );
			editorPanel.getCombo( CREW ).addItem( crewSprite );
		}

		final Runnable applyCallback = new Runnable() {
			@Override
			public void run() {
				Object crewObj = editorPanel.getCombo( CREW ).getSelectedItem();
				if ( crewObj instanceof CrewSprite ) {
					SpriteReference<CrewState> crewRef = ((CrewSprite)crewObj).getReference();
					showCrewEditor( shipBundle, crewRef );
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

	private void showCrewEditor( final ShipBundle shipBundle, final SpriteReference<CrewState> crewRef ) {
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

					int pilotInterval = ftlConstants.getMasteryIntervalPilot( crewType );
					int engineInterval = ftlConstants.getMasteryIntervalEngine( crewType );
					int shieldInterval = ftlConstants.getMasteryIntervalShield( crewType );
					int weaponInterval = ftlConstants.getMasteryIntervalWeapon( crewType );
					int repairInterval = ftlConstants.getMasteryIntervalRepair( crewType );
					int combatInterval = ftlConstants.getMasteryIntervalCombat( crewType );

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
		editorPanel.getCombo( RACE ).setSelectedItem( crewRef.get().getRace() );

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
				CrewType prevRace = crewRef.get().getRace();

				crewRef.get().setName( editorPanel.getString( NAME ).getText() );
				crewRef.get().setRace( (CrewType)editorPanel.getCombo( RACE ).getSelectedItem() );

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
				if ( crewRef.get().isEnemyBoardingDrone() && !CrewType.BATTLE.equals( crewRef.get().getRace() ) ) {
					crewRef.get().setRace( CrewType.BATTLE );
				}
				if ( crewRef.get().isEnemyBoardingDrone() && !crewRef.get().getName().equals( "Anti-Personnel Drone" ) ) {
					crewRef.get().setName( "Anti-Personnel Drone" );
				}
				if ( !crewRef.get().isEnemyBoardingDrone() && CrewType.BATTLE.equals( crewRef.get().getRace() ) ) {
					crewRef.get().setRace( CrewType.HUMAN );
				}

				crewRef.fireReferenceChange();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator( 6 );

		JButton moveBtn = new JButton( "Move To..." );
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( moveBtn );

		sidePanel.add( Box.createVerticalStrut( 6 ) );

		JButton removeBtn = new JButton( "Remove" );
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add( removeBtn );

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

				shipPanel.remove( crewSprite );
				shipBundle.getCrewSprites().remove( crewSprite );
				shipBundle.getCrewRefs().remove( crewRef );
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
}
