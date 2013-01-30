package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.plaf.basic.BasicArrowButton;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.ui.FieldEditorPanel;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.SectorMapLayout;
import net.blerf.ftl.ui.SectorMapLayout.SectorMapConstraints;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.ui.hud.SpriteSelector;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteCriteria;
import net.blerf.ftl.ui.hud.SpriteSelector.SpriteSelectionCallback;
import net.blerf.ftl.ui.hud.StatusViewport;
import net.blerf.ftl.xml.AugBlueprint;
import net.blerf.ftl.xml.CrewBlueprint;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.Encounters;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.SystemBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class SavedGameSectorMapPanel extends JPanel {

	// mapHolderPanel Layers
	private static final Integer MAP_LAYER = new Integer(10);
	private static final Integer MISC_SELECTION_LAYER = new Integer(50);
	// mapPanel Layers
	private static final Integer BEACON_LAYER = new Integer(10);
	private static final Integer MISC_BOX_LAYER = new Integer(15);
	private static final Integer SHIP_LAYER = new Integer(30);
	private static final Integer CTRL_LAYER = new Integer(50);
	private static final Logger log = LogManager.getLogger(SavedGameSectorMapPanel.class);

	private FTLFrame frame;

	private ArrayList<BeaconSprite> beaconSprites = new ArrayList<BeaconSprite>();
	private ArrayList<StoreSprite> storeSprites = new ArrayList<StoreSprite>();
	private ArrayList<QuestSprite> questSprites = new ArrayList<QuestSprite>();
	private ArrayList<PlayerShipSprite> playerShipSprites = new ArrayList<PlayerShipSprite>();

	private HashMap<String, BufferedImage> cachedImages = new HashMap<String, BufferedImage>();

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
		sidePanel.setLayout( new BoxLayout(sidePanel, BoxLayout.Y_AXIS) );
		sidePanel.setBorder( BorderFactory.createEmptyBorder(4, 4, 4, 6) );

		miscSelector = new SpriteSelector( new ArrayList[] {} );
		miscSelector.setOpaque(false);
		miscSelector.setSize( mapPanel.getPreferredSize() );
		mapHolderPanel.add( miscSelector, MISC_SELECTION_LAYER );

		MouseInputAdapter miscListener = new MouseInputAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				miscSelector.setMousePoint( e.getX(), e.getY() );
			}
			@Override
			public void mouseClicked(MouseEvent e) {
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
			public void mouseEntered(MouseEvent e) {
				//miscSelector.setDescriptionVisible( true );
				mapViewport.setStatusString( miscSelector.getCriteria().getDescription() );
			}
			@Override
			public void mouseExited(MouseEvent e) {
				//miscSelector.setDescriptionVisible( false );
				mapViewport.setStatusString( null );
				miscSelector.setMousePoint( -1, -1 );
			}
		};
		miscSelector.addMouseListener( miscListener );
		miscSelector.addMouseMotionListener( miscListener );

		Insets ctrlInsets = new Insets(3, 4, 3, 4);

		JPanel selectPanel = new JPanel();
		selectPanel.setLayout( new BoxLayout(selectPanel, BoxLayout.X_AXIS) );
		selectPanel.setBorder( BorderFactory.createTitledBorder("Select") );
		final JButton selectBeaconBtn = new JButton("Beacon");
		selectBeaconBtn.setMargin(ctrlInsets);
		selectPanel.add( selectBeaconBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectPlayerShipBtn = new JButton("Player Ship");
		selectPlayerShipBtn.setMargin(ctrlInsets);
		selectPanel.add( selectPlayerShipBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectStoreBtn = new JButton("Store");
		selectStoreBtn.setMargin(ctrlInsets);
		selectPanel.add( selectStoreBtn );
		selectPanel.add( Box.createHorizontalStrut(5) );
		final JButton selectQuestBtn = new JButton("Quest");
		selectQuestBtn.setMargin(ctrlInsets);
		selectPanel.add( selectQuestBtn );

		JPanel addPanel = new JPanel();
		addPanel.setLayout( new BoxLayout(addPanel, BoxLayout.X_AXIS) );
		addPanel.setBorder( BorderFactory.createTitledBorder("Add") );
		final JButton addStoreBtn = new JButton("Store");
		addStoreBtn.setMargin(ctrlInsets);
		addPanel.add( addStoreBtn );
		addPanel.add( Box.createHorizontalStrut(5) );
		final JButton addQuestBtn = new JButton("Quest");
		addQuestBtn.setMargin(ctrlInsets);
		addPanel.add( addQuestBtn );

		JPanel ctrlRowOnePanel = new JPanel();
		ctrlRowOnePanel.setLayout( new BoxLayout(ctrlRowOnePanel, BoxLayout.X_AXIS) );
		ctrlRowOnePanel.add( selectPanel );
		ctrlRowOnePanel.add( Box.createHorizontalStrut(15) );
		ctrlRowOnePanel.add( addPanel );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS) );
		ctrlPanel.add( ctrlRowOnePanel );
		//ctrlPanel.add( Box.createVerticalStrut(8) );

		ActionListener ctrlListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//if ( shipBlueprint == null ) return;  // No map to edit!

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
			}
		};

		selectBeaconBtn.addActionListener( ctrlListener );
		selectPlayerShipBtn.addActionListener( ctrlListener );
		selectStoreBtn.addActionListener( ctrlListener );
		selectQuestBtn.addActionListener( ctrlListener );

		addStoreBtn.addActionListener( ctrlListener );
		addQuestBtn.addActionListener( ctrlListener );

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
		mapScroll.setViewport( mapViewport );
		mapScroll.setViewportView( mapHolderPanel );
		centerPanel.add( mapScroll, gridC );

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

		JPanel borderPanel = new JPanel( new BorderLayout() );
		borderPanel.setBorder( BorderFactory.createTitledBorder("Sector Map") );


		//JLabel noticeLbl = new JLabel("The number of beacons in each column can't be determined automatically.");
		//noticeLbl.setHorizontalAlignment( SwingConstants.CENTER );
		//borderPanel.add( noticeLbl, BorderLayout.NORTH );

		columnCtrlListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();

				BasicArrowButton srcBtn = (BasicArrowButton)source;
				IncrementBox iBox = (IncrementBox)(srcBtn).getParent();
				int column = mapLayout.getCtrlColumn( iBox );
				if ( column != -1 ) {
					int colSize = mapLayout.getColumnSize( column );
					if ( srcBtn == iBox.decBtn )
						mapLayout.setColumnSize( column, colSize-1 );
					else if ( srcBtn == iBox.incBtn )
						mapLayout.setColumnSize( column, colSize+1 );

					mapPanel.revalidate();
					mapViewport.repaint();
				}
			}
		};
	}

	public void setGameState( SavedGameParser.SavedGameState gameState ) {
		mapPanel.removeAll();
		miscSelector.setVisible( false );
		miscSelector.setMousePoint( -1, -1 );
		mapViewport.setStatusString( null );
		clearSidePanel();

		for ( BeaconSprite beaconSprite : beaconSprites )
			mapPanel.remove( beaconSprite );
		beaconSprites.clear();

		for ( StoreSprite storeSprite : storeSprites )
			mapPanel.remove( storeSprite );
		storeSprites.clear();

		for ( QuestSprite questSprite : questSprites )
			mapPanel.remove( questSprite );
		questSprites.clear();

		for ( PlayerShipSprite playerShipSprite : playerShipSprites )
			mapPanel.remove( playerShipSprite );
		playerShipSprites.clear();

		if ( gameState == null ) {
			mapPanel.revalidate();
			mapViewport.repaint();
			return;
		}

		int beaconId;
		List<SavedGameParser.BeaconState> beaconStateList = gameState.getBeaconList();

		// Beacons.
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			BeaconSprite beaconSprite = new BeaconSprite( beaconState );
			SectorMapConstraints beaconC = new SectorMapConstraints( SectorMapConstraints.BEACON );
			beaconSprites.add( beaconSprite );
			mapPanel.add( beaconSprite, beaconC );
		}

		// Stores.
		beaconId = 0;
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			if ( beaconState.isStorePresent() ) {
				StoreSprite storeSprite = new StoreSprite( beaconState.getStore() );
				SectorMapConstraints storeC = new SectorMapConstraints( SectorMapConstraints.MISC_BOX );
				storeC.setBeaconId( beaconId );
				storeSprites.add( storeSprite );
				mapPanel.add( storeSprite, storeC );
			}
			beaconId++;
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

		// Add column controls.
		for (int i=0; i < 6; i++) {
			IncrementBox iBox = new IncrementBox();
			iBox.decBtn.addActionListener( columnCtrlListener );
			iBox.incBtn.addActionListener( columnCtrlListener );
			mapPanel.add( iBox, new SectorMapConstraints( SectorMapConstraints.COLUMN_CTRL ) );

			String message = "Adjust the number of beacons in this column.";
			iBox.addMouseListener( new StatusbarMouseListener(frame, message) );
			iBox.decBtn.addMouseListener( new StatusbarMouseListener(frame, message) );
			iBox.incBtn.addMouseListener( new StatusbarMouseListener(frame, message) );
		}

		mapPanel.revalidate();
		mapViewport.repaint();
	}

	public void updateGameState( SavedGameParser.SavedGameState gameState ) {
		if ( gameState == null ) return;

		List<SavedGameParser.BeaconState> beaconStateList = gameState.getBeaconList();

		// Player ship.
		for ( PlayerShipSprite playerShipSprite : playerShipSprites ) {
			int beaconId = mapLayout.getBeaconId( playerShipSprite );
			if ( beaconId != -1 ) {
				gameState.setCurrentBeaconId( beaconId );
			}
		}

		// Reset Beacon states.
		for ( SavedGameParser.BeaconState beaconState : beaconStateList ) {
			beaconState.setSeen( false );
			beaconState.setStorePresent( false );
			beaconState.setStore( null );
		}

		// Beacons.
		for ( BeaconSprite beaconSprite : beaconSprites) {
			int beaconId = mapLayout.getBeaconId( beaconSprite );
			if ( beaconId > 0 && beaconId < beaconStateList.size() ) {
				SavedGameParser.BeaconState beaconState = beaconStateList.get( beaconId );
				beaconState.setSeen( beaconSprite.isSeen() );
			}
		}

		// Stores.
		for ( StoreSprite storeSprite : storeSprites ) {
			int beaconId = mapLayout.getBeaconId( storeSprite );
			if ( beaconId > 0 && beaconId < beaconStateList.size() ) {
				SavedGameParser.StoreState storeState = new SavedGameParser.StoreState();
				storeState.setFuel( storeSprite.getFuel() );
				storeState.setMissiles( storeSprite.getMissiles() );
				storeState.setDroneParts( storeSprite.getDroneParts() );

				SavedGameParser.StoreShelf tmpShelf;
				SavedGameParser.StoreShelf topShelf = new SavedGameParser.StoreShelf();
				tmpShelf = storeSprite.getTopShelf();
				topShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					topShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
				SavedGameParser.StoreShelf bottomShelf = new SavedGameParser.StoreShelf();
				tmpShelf = storeSprite.getBottomShelf();
				bottomShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					bottomShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}

				storeState.setTopShelf( topShelf );
				storeState.setBottomShelf( bottomShelf );

				SavedGameParser.BeaconState beaconState = beaconStateList.get( beaconId );
				beaconState.setStore( storeState );
				beaconState.setStorePresent( true );
			}
		}

		// Quests.
		gameState.getQuestEventMap().clear();
		for ( QuestSprite questSprite : questSprites ) {
			String questId = questSprite.getQuestId();
			int beaconId = mapLayout.getBeaconId( questSprite );
			if ( beaconId != -1 && questId != null && questId.length() > 0 ) {
				gameState.getQuestEventMap().put( questSprite.getQuestId(), new Integer(beaconId) );
			}
		}
	}

	public void selectBeacon() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Beacon";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					showBeaconEditor( (BeaconSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectPlayerShip() {
		miscSelector.setSpriteLists( new ArrayList[] {playerShipSprites} );
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Player Ship";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof PlayerShipSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof PlayerShipSprite ) {
					showPlayerShipEditor( (PlayerShipSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectStore() {
		miscSelector.setSpriteLists( new ArrayList[] {storeSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Store";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof StoreSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof StoreSprite ) {
					showStoreEditor( (StoreSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	public void selectQuest() {
		miscSelector.setSpriteLists( new ArrayList[] {questSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Select: Quest";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof QuestSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof QuestSprite ) {
					showQuestEditor( (QuestSprite)sprite );
				}
				return true;
			}
		});
		miscSelector.setVisible(true);
	}

	private void addStore() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Store";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						StoreSprite storeSprite = new StoreSprite( null );
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
		miscSelector.setVisible(true);
	}

	private void addQuest() {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Add: Quest";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						return true;
					}
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
			public boolean spriteSelected( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite instanceof BeaconSprite ) {
					int beaconId = mapLayout.getBeaconId( sprite );

					if ( beaconId != -1 && mapLayout.getMiscBoxAtBeaconId(beaconId) == null ) {
						QuestSprite questSprite = new QuestSprite("NOTHING");
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
		miscSelector.setVisible(true);
	}

	private void movePlayerShip( final PlayerShipSprite mobileSprite ) {
		miscSelector.setSpriteLists( new ArrayList[] {beaconSprites} );
		miscSelector.reset();
		miscSelector.setCriteria(new SpriteCriteria() {
			private final String desc = "Move: Player Ship";

			public String getDescription() { return desc; }

			public boolean isSpriteValid( SpriteSelector spriteSelector, JComponent sprite ) {
				if ( sprite == null ) return false;
				if ( sprite instanceof BeaconSprite ) {
					return true;
				}
				return false;
			}
		});
		miscSelector.setCallback(new SpriteSelectionCallback() {
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
		miscSelector.setVisible(true);
	}

	/**
	 * Gets an image, and caches the result.
	 */
	private BufferedImage getImage( String innerPath ) {
		BufferedImage result = cachedImages.get(innerPath);
		if (result != null) return result;
		log.trace( "Image not in cache, loading and scaling...: "+ innerPath );

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			result = ImageIO.read( in );

		} catch (IOException e) {
			log.error( "Error reading image: "+ innerPath, e );
		}	finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
		}
		cachedImages.put( innerPath, result );

		return result;
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
			public void run() {
				sideScroll.getVerticalScrollBar().setValue(0);
			}
		});
	}

	private void fitSidePanel() {
		sidePanel.revalidate();
		int sideWidth = sidePanel.getPreferredSize().width;
		int vbarWidth = sideScroll.getVerticalScrollBar().getPreferredSize().width;
		sideScroll.setPreferredSize( new Dimension(sideWidth + vbarWidth, 1) );

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
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
			}
		});

		applyBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
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

	private void showBeaconEditor( final BeaconSprite beaconSprite ) {
		final String SEEN = "Seen";

		String title = String.format( "Beacon %02d", mapLayout.getBeaconId(beaconSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( SEEN, FieldEditorPanel.ContentType.BOOLEAN );
		editorPanel.getBoolean(SEEN).setSelected( beaconSprite.isSeen() );
		editorPanel.getBoolean(SEEN).addMouseListener( new StatusbarMouseListener(frame, "Spoiled by sensor data, or player has been within one hop.") );

		final Runnable applyCallback = new Runnable() {
			public void run() {
				beaconSprite.setSeen( editorPanel.getBoolean(SEEN).isSelected() );
				beaconSprite.makeSane();

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		showSidePanel();
	}

	private void showPlayerShipEditor( final PlayerShipSprite playerShipSprite ) {

		String title = "Player Ship";

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );

		final Runnable applyCallback = new Runnable() {
			public void run() {
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		addSidePanelSeparator(6);

		JButton moveBtn = new JButton("Move To...");
		moveBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(moveBtn);

		moveBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				movePlayerShip( playerShipSprite );
			}
		});

		showSidePanel();
	}

	private void showStoreEditor( final StoreSprite storeSprite ) {
		final String FUEL = "Fuel";
		final String MISSILES = "Missiles";
		final String DRONE_PARTS = "Drone Parts";
		final String TOP_TYPE = "Top Type";
		final String TOP_ONE = "T1 Item";
		final String TOP_AVAIL_ONE = "T1 In Stock";
		final String TOP_TWO = "T2 Item";
		final String TOP_AVAIL_TWO = "T2 In Stock";
		final String TOP_THREE = "T3 Item";
		final String TOP_AVAIL_THREE = "T3 In Stock";
		final String BOTTOM_TYPE = "Bottom Type";
		final String BOTTOM_ONE = "B1 Item";
		final String BOTTOM_AVAIL_ONE = "B1 In Stock";
		final String BOTTOM_TWO = "B2 Item";
		final String BOTTOM_AVAIL_TWO = "B2 In Stock";
		final String BOTTOM_THREE = "B3 Item";
		final String BOTTOM_AVAIL_THREE = "B3 In Stock";

		final String[] topSlots = new String[] { TOP_ONE, TOP_TWO, TOP_THREE };
		final String[] topAvail = new String[] { TOP_AVAIL_ONE, TOP_AVAIL_TWO, TOP_AVAIL_THREE };
		final String[] bottomSlots = new String[] { BOTTOM_ONE, BOTTOM_TWO, BOTTOM_THREE };
		final String[] bottomAvail = new String[] { BOTTOM_AVAIL_ONE, BOTTOM_AVAIL_TWO, BOTTOM_AVAIL_THREE };
		final SavedGameParser.StoreItemType[] itemTypes = SavedGameParser.StoreItemType.class.getEnumConstants();

		// Use array-loops to manage both shelves.
		final SavedGameParser.StoreShelf[] shelves = new SavedGameParser.StoreShelf[] { storeSprite.getTopShelf(), storeSprite.getBottomShelf() };
		final String[] types = new String[] { TOP_TYPE, BOTTOM_TYPE };
		final String[][] slots = new String[][] { topSlots, bottomSlots };
		final String[][] avails = new String[][] { topAvail, bottomAvail };

		// Build interchangeable id-vs-toStringable maps, mapped by item type.
		Map weaponLookup = DataManager.get().getWeapons();
		Map droneLookup = DataManager.get().getDrones();
		Map augmentLookup = DataManager.get().getAugments();
		Map crewLookup = new LinkedHashMap();
		for ( Object o : new Object[] {
			CrewBlueprint.RACE_CRYSTAL, CrewBlueprint.RACE_ENERGY,
			CrewBlueprint.RACE_ENGI, CrewBlueprint.RACE_HUMAN,
			CrewBlueprint.RACE_MANTIS, CrewBlueprint.RACE_ROCK,
			CrewBlueprint.RACE_SLUG } ) {
			crewLookup.put( o, o );
		}
		Map systemLookup = new LinkedHashMap();
		for ( Object o : new Object[] {
			SystemBlueprint.ID_PILOT, SystemBlueprint.ID_DOORS,
			SystemBlueprint.ID_SENSORS, SystemBlueprint.ID_MEDBAY,
			SystemBlueprint.ID_OXYGEN, SystemBlueprint.ID_SHIELDS,
			SystemBlueprint.ID_ENGINES, SystemBlueprint.ID_WEAPONS,
			SystemBlueprint.ID_DRONE_CTRL, SystemBlueprint.ID_TELEPORTER,
			SystemBlueprint.ID_CLOAKING, SystemBlueprint.ID_ARTILLERY } ) {
			systemLookup.put( o, o );
		}
		final Map<SavedGameParser.StoreItemType, Map> itemLookups = new HashMap<SavedGameParser.StoreItemType, Map>();
		itemLookups.put( SavedGameParser.StoreItemType.WEAPON, weaponLookup );
		itemLookups.put( SavedGameParser.StoreItemType.DRONE, droneLookup );
		itemLookups.put( SavedGameParser.StoreItemType.AUGMENT, augmentLookup );
		itemLookups.put( SavedGameParser.StoreItemType.CREW, crewLookup );
		itemLookups.put( SavedGameParser.StoreItemType.SYSTEM, systemLookup );

		String title = String.format("Store @%02d", mapLayout.getBeaconId(storeSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( FUEL, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(FUEL).setText( ""+storeSprite.getFuel() );
		editorPanel.addRow( MISSILES, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(MISSILES).setText( ""+storeSprite.getMissiles() );
		editorPanel.addRow( DRONE_PARTS, FieldEditorPanel.ContentType.INTEGER );
		editorPanel.getInt(DRONE_PARTS).setText( ""+storeSprite.getDroneParts() );

		List<String> badIds = new ArrayList<String>();
		for (int i=0; i < shelves.length; i++) {
			editorPanel.addBlankRow();
			editorPanel.addRow( types[i], FieldEditorPanel.ContentType.COMBO );
			for (int j=0; j < itemTypes.length; j++) {
				editorPanel.getCombo(types[i]).addItem( itemTypes[j] );
			}
			editorPanel.getCombo(types[i]).setSelectedItem( shelves[i].getItemType() );

			for (int j=0; j < slots[i].length; j++) {
				editorPanel.addRow( slots[i][j], FieldEditorPanel.ContentType.COMBO );
				editorPanel.addRow( avails[i][j], FieldEditorPanel.ContentType.BOOLEAN );

				editorPanel.getCombo(slots[i][j]).addItem( "" );
				Map lookupMap = itemLookups.get( shelves[i].getItemType() );
				if ( lookupMap != null ) {
					for ( Object o : lookupMap.values() ) {
						editorPanel.getCombo(slots[i][j]).addItem( o );
					}
					if ( shelves[i].getItems().size() > j ) {
						String itemId = shelves[i].getItems().get(j).getItemId();
						Object itemChoice = lookupMap.get( itemId );
						if ( itemChoice != null ) {
							editorPanel.getCombo(slots[i][j]).setSelectedItem( itemChoice );
						} else {
							editorPanel.getCombo(slots[i][j]).setSelectedItem( "" );
							badIds.add( itemId );
						}

						editorPanel.getBoolean(avails[i][j]).setSelected( shelves[i].getItems().get(j).isAvailable() );
					}
				}
			}
		}
		if ( badIds.size() > 0 ) {
			String message = "The following item ids are unrecognized:";
			for ( String badId : badIds )
				message += " "+ badId;
			message += ".";
			log.debug( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			public void run() {
				String newString;

				newString = editorPanel.getInt(FUEL).getText();
				try { storeSprite.setFuel( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(MISSILES).getText();
				try { storeSprite.setMissiles( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				newString = editorPanel.getInt(DRONE_PARTS).getText();
				try { storeSprite.setDroneParts( Integer.parseInt(newString) ); }
				catch (NumberFormatException e) {}

				for (int i=0; i < shelves.length; i++) {
					SavedGameParser.StoreItemType itemType = (SavedGameParser.StoreItemType)editorPanel.getCombo(types[i]).getSelectedItem();
					shelves[i].setItemType( itemType );
					shelves[i].getItems().clear();
					for (int j=0; j < slots[i].length; j++) {
						Object selectedItem = editorPanel.getCombo(slots[i][j]).getSelectedItem();
						if ( "".equals( selectedItem ) == false && itemLookups.get(itemType) != null ) {
							// Do a reverse lookup on the map to get an item id.
							for ( Object entry : itemLookups.get(itemType).entrySet() ) {
								if ( ((Map.Entry)entry).getValue().equals( selectedItem ) ) {
									boolean available = editorPanel.getBoolean(avails[i][j]).isSelected();
									String id = (String)((Map.Entry)entry).getKey();

									SavedGameParser.StoreItem newItem = new SavedGameParser.StoreItem( available, id );
									shelves[i].getItems().add( newItem );
									break;
								}
							}
						}
					}
				}

				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener shelfListener = new ActionListener() {
			private boolean ignoreChanges = false;

			public void actionPerformed(ActionEvent e) {
				if ( ignoreChanges ) return;
				ignoreChanges = true;

				Object source = e.getSource();
				boolean resize = false;
				for (int i=0; i < shelves.length; i++) {
					JComboBox typeCombo = editorPanel.getCombo(types[i]);
					if ( source == typeCombo ) {
						for (int j=0; j < slots[i].length; j++) {
							JComboBox itemCombo = editorPanel.getCombo(slots[i][j]);
							itemCombo.removeAllItems();
							Object selectedType = typeCombo.getSelectedItem();

							itemCombo.addItem( "" );
							Map lookupMap = itemLookups.get( selectedType );
							if ( lookupMap != null ) {
								for ( Object o : lookupMap.values() ) {
									editorPanel.getCombo(slots[i][j]).addItem( o );
								}
							}

							editorPanel.getBoolean(avails[i][j]).setSelected( false );
						}
						resize = true;
					}
					else {
						// Check if the source was a slot combo.
						for (int j=0; j < slots[i].length; j++) {
							JComboBox itemCombo = editorPanel.getCombo(slots[i][j]);
							if ( source == itemCombo ) {
								// Toggle the avail checkbox to sell items and disable "".
								editorPanel.getBoolean(avails[i][j]).setSelected( !"".equals( itemCombo.getSelectedItem() ) );
								break;
							}
						}
					}
				}
				if ( resize ) {
					editorPanel.setMaximumSize(editorPanel.getPreferredSize());
					fitSidePanel();
				}

				// After all the secondary events, resume monitoring.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ignoreChanges = false;
					}
				});
			}
		};
		for (int i=0; i < shelves.length; i++) {
			editorPanel.getCombo(types[i]).addActionListener( shelfListener );
			for (int j=0; j < slots[i].length; j++) {
				editorPanel.getCombo(slots[i][j]).addActionListener( shelfListener );
			}
		}

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				storeSprites.remove( storeSprite );
				mapPanel.remove( storeSprite );
			}
		});

		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		showSidePanel();
	}

	private void showQuestEditor( final QuestSprite questSprite ) {
		final String ENCOUNTERS_FILE = "File";
		final String EVENT = "Event";

		final Map<String, Encounters> allEncountersMap = DataManager.get().getEncounters();

		String title = String.format("Quest @%02d", mapLayout.getBeaconId(questSprite) );

		final FieldEditorPanel editorPanel = new FieldEditorPanel( false );
		editorPanel.addRow( ENCOUNTERS_FILE, FieldEditorPanel.ContentType.COMBO );
		editorPanel.addRow( EVENT, FieldEditorPanel.ContentType.COMBO );

		editorPanel.getCombo(ENCOUNTERS_FILE).addItem( "" );
		for ( String fileName : allEncountersMap.keySet() ) {
			editorPanel.getCombo(ENCOUNTERS_FILE).addItem( fileName );
		}

		// Preselect the file of the current event.
		editorPanel.getCombo(EVENT).addItem( "" );
		for ( Map.Entry<String,Encounters> entry : allEncountersMap.entrySet() ) {
			FTLEvent currentEvent = entry.getValue().getEventById(questSprite.getQuestId());
			FTLEventList currentEventList = entry.getValue().getEventListById(questSprite.getQuestId());
			if ( currentEvent != null || currentEventList != null ) {
				editorPanel.getCombo(ENCOUNTERS_FILE).setSelectedItem(entry.getKey());

				for ( FTLEvent tmpEvent : entry.getValue().getEvents() ) {
					if ( tmpEvent.getId() != null )
						editorPanel.getCombo(EVENT).addItem( tmpEvent );
				}
				editorPanel.getCombo(EVENT).addItem( "- = Lists = -" );
				for ( FTLEventList tmpEventList : entry.getValue().getEventLists() ) {
					if ( tmpEventList.getId() != null )
						editorPanel.getCombo(EVENT).addItem( tmpEventList );
				}
				if ( currentEvent != null ) {
					editorPanel.getCombo(EVENT).setSelectedItem(currentEvent);
				} else if ( currentEventList != null ) {
					editorPanel.getCombo(EVENT).setSelectedItem(currentEventList);
				} else {
					editorPanel.getCombo(EVENT).setSelectedItem( "" );
				}
				break;
			}
		}
		// If no file contains the current event, complain.
		if ( "".equals( editorPanel.getCombo(ENCOUNTERS_FILE).getSelectedItem() ) ) {
			String message = "The current event/eventlist id is unrecognized: "+ questSprite.getQuestId() +".";
			log.debug( message );
			frame.setStatusText( message );
		}

		final Runnable applyCallback = new Runnable() {
			public void run() {
				Object evtObj = editorPanel.getCombo(EVENT).getSelectedItem();
				if ( evtObj instanceof FTLEvent ) {
					questSprite.setQuestId( ((FTLEvent)evtObj).getId() );
				} else if ( evtObj instanceof FTLEventList ) {
					questSprite.setQuestId( ((FTLEventList)evtObj).getId() );
				} else {
					frame.setStatusText( "No event/eventlist id has been selected." );
					return;
				}
				clearSidePanel();
			}
		};
		createSidePanel( title, editorPanel, applyCallback );

		ActionListener questListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox fileCombo = editorPanel.getCombo(ENCOUNTERS_FILE);
				JComboBox eventCombo = editorPanel.getCombo(EVENT);
				Object source = e.getSource();
				if ( source == fileCombo ) {
					eventCombo.removeAllItems();
					Object selectedFile = fileCombo.getSelectedItem();

					editorPanel.getCombo(EVENT).addItem( "" );
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
							editorPanel.setMaximumSize(editorPanel.getPreferredSize());
							fitSidePanel();
						}
					}
				}
			}
		};
		editorPanel.getCombo(ENCOUNTERS_FILE).addActionListener( questListener );

		addSidePanelSeparator(6);

		JButton removeBtn = new JButton("Remove");
		removeBtn.setAlignmentX( Component.CENTER_ALIGNMENT );
		sidePanel.add(removeBtn);

		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearSidePanel();
				questSprites.remove( questSprite );
				mapPanel.remove( questSprite );
			}
		});

		editorPanel.setMaximumSize(editorPanel.getPreferredSize());
		showSidePanel();
	}



	public class BeaconSprite extends JComponent {
		private boolean visited = false;
		private boolean seen = false;
		private SavedGameParser.FleetPresence fleetPresence = SavedGameParser.FleetPresence.NONE;
		private BufferedImage currentImage = null;

		public BeaconSprite( SavedGameParser.BeaconState beaconState ) {
			if ( beaconState != null ) {
				visited = beaconState.isVisited();
				seen = beaconState.isSeen();
				fleetPresence = beaconState.getFleetPresence();
			}
			makeSane();
		}

		public boolean isSeen() { return seen; }

		public void setSeen( boolean b ) { seen = b; }

		public void makeSane() {
			if ( fleetPresence == SavedGameParser.FleetPresence.REBEL ) {
				currentImage = getImage( "img/map/map_icon_warning.png" );
			}
			else if ( visited ) {
				currentImage = getImage( "img/map/map_icon_diamond_blue.png" );
			}
			else {
				currentImage = getImage( "img/map/map_icon_diamond_yellow.png" );
			}
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class StoreSprite extends JComponent {
		private int fuel;
		private int missiles;
		private int droneParts;
		private SavedGameParser.StoreShelf topShelf = new SavedGameParser.StoreShelf();
		private SavedGameParser.StoreShelf bottomShelf = new SavedGameParser.StoreShelf();
		private BufferedImage currentImage = null;

		public StoreSprite( SavedGameParser.StoreState storeState ) {
			if ( storeState != null ) {
				fuel = storeState.getFuel();
				missiles = storeState.getMissiles();
				droneParts = storeState.getDroneParts();

				SavedGameParser.StoreShelf tmpShelf;
				tmpShelf = storeState.getTopShelf();
				topShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					topShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
				tmpShelf = storeState.getBottomShelf();
				bottomShelf.setItemType( tmpShelf.getItemType() );
				for ( SavedGameParser.StoreItem tmpItem : tmpShelf.getItems() ) {
					bottomShelf.addItem( new SavedGameParser.StoreItem( tmpItem.isAvailable(), tmpItem.getItemId() ) );
				}
			}

			currentImage = getImage( "img/map/map_box_store.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		public void setFuel( int n ) { fuel = n; }
		public void setMissiles( int n ) { missiles = n; }
		public void setDroneParts( int n ) { droneParts = n; }

		public int getFuel() { return fuel; }
		public int getMissiles() { return missiles; }
		public int getDroneParts() { return droneParts; }
		public SavedGameParser.StoreShelf getTopShelf() { return topShelf; }
		public SavedGameParser.StoreShelf getBottomShelf() { return bottomShelf; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class QuestSprite extends JComponent {
		private String questId = null;
		private BufferedImage currentImage = null;

		public QuestSprite( String questId ) {
			this.questId = questId;
			currentImage = getImage( "img/map/map_box_quest.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		public void setQuestId( String s ) { questId = s; }
		public String getQuestId() { return questId; }

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class PlayerShipSprite extends JComponent {
		private BufferedImage currentImage = null;

		public PlayerShipSprite() {
			currentImage = getImage( "img/map/map_icon_ship.png" );
			this.setPreferredSize( new Dimension(currentImage.getWidth(), currentImage.getHeight()) );
		}

		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			Graphics2D g2d = (Graphics2D)g;
			g2d.drawImage( currentImage, 0, 0, this.getWidth(), this.getHeight(), this);
		}
	}



	public class IncrementBox extends JPanel {
		public BasicArrowButton decBtn = new BasicArrowButton( BasicArrowButton.SOUTH );
		public BasicArrowButton incBtn = new BasicArrowButton( BasicArrowButton.NORTH );

		public IncrementBox() {
			super( new FlowLayout( FlowLayout.CENTER, 4, 2 ) );
			this.setBorder( BorderFactory.createEtchedBorder() );
			this.add( decBtn );
			this.add( incBtn );
		}
	}
}
