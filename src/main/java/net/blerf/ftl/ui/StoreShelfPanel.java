package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.AdvancedFTLConstants;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.CrewType;
import net.blerf.ftl.parser.SavedGameParser.StoreItem;
import net.blerf.ftl.parser.SavedGameParser.StoreItemType;
import net.blerf.ftl.parser.SavedGameParser.StoreShelf;
import net.blerf.ftl.parser.SavedGameParser.SystemType;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.StatusbarMouseListener;


public class StoreShelfPanel extends JPanel implements ActionListener {

	private static final Logger log = LoggerFactory.getLogger( StoreShelfPanel.class );

	private static final String SHELF_TYPE = "Type";
	private static final String ITEM_ZERO = "#0 Item";
	private static final String AVAIL_ZERO = "#0 In Stock";
	private static final String EXTRA_ZERO = "#0 Extra";
	private static final String ITEM_ONE = "#1 Item";
	private static final String AVAIL_ONE = "#1 In Stock";
	private static final String EXTRA_ONE = "#1 Extra";
	private static final String ITEM_TWO = "#2 Item";
	private static final String AVAIL_TWO = "#2 In Stock";
	private static final String EXTRA_TWO = "#2 Extra";

	private static final String[] SLOTS = new String[] { ITEM_ZERO, ITEM_ONE, ITEM_TWO };
	private static final String[] AVAIL = new String[] { AVAIL_ZERO, AVAIL_ONE, AVAIL_TWO };
	private static final String[] EXTRA = new String[] { EXTRA_ZERO, EXTRA_ONE, EXTRA_TWO };

	private FTLFrame frame;

	private StoreItemType[] itemTypes = StoreItemType.values();
	private Map<StoreItemType, Map<String, ?>> itemLookups = new HashMap<StoreItemType, Map<String, ?>>();

	private FieldEditorPanel editorPanel = null;
	private boolean ignoreChanges = false;

	private FTLConstants ftlConstants;


	public StoreShelfPanel( FTLFrame frame, FTLConstants ftlConstants ) {
		super( new BorderLayout() );
		this.frame = frame;

		editorPanel = new FieldEditorPanel( false );

		editorPanel.addRow( SHELF_TYPE, FieldEditorPanel.ContentType.COMBO );

		for ( int i=0; i < SLOTS.length; i++ ) {
			editorPanel.addSeparatorRow();
			editorPanel.addRow( SLOTS[i], FieldEditorPanel.ContentType.COMBO );
			editorPanel.addRow( AVAIL[i], FieldEditorPanel.ContentType.BOOLEAN );
			editorPanel.addRow( EXTRA[i], FieldEditorPanel.ContentType.INTEGER );

			editorPanel.getBoolean( AVAIL[i] ).addMouseListener( new StatusbarMouseListener( frame, "Toggle whether this item has already been bought." ) );
			editorPanel.getInt( EXTRA[i] ).addMouseListener( new StatusbarMouseListener( frame, "Unknown. Seen on DroneCtrl, Cloaking, Clonebay. Reloading sets it to 0!?" ) );
		}

		this.add( editorPanel, BorderLayout.CENTER );

		editorPanel.getCombo( SHELF_TYPE ).addActionListener( this );

		for ( String SLOT : SLOTS ) {
			editorPanel.getCombo( SLOT ).addActionListener( this );
		}

		setFTLConstants( ftlConstants );
	}

	/**
	 * Clears the panel and sets new FTL constants to use.
	 *
	 * @see net.blerf.ftl.constants.AdvancedFTLConstants
	 * @see net.blerf.ftl.constants.OriginalFTLConstants
	 */
	public void setFTLConstants( FTLConstants c ) {
		ignoreChanges = true;

		ftlConstants = c;

		for ( Map<String, ?> lookupMap : itemLookups.values() ) {
			lookupMap.clear();
		}
		itemLookups.clear();

		// Build interchangeable id-vs-toStringable maps, mapped by item type.
		Map<String, ?> weaponLookup = DataManager.get().getWeapons();
		Map<String, ?> droneLookup = DataManager.get().getDrones();
		Map<String, ?> augmentLookup = DataManager.get().getAugments();
		Map<String, CrewType> crewLookup = new LinkedHashMap<String, CrewType>();
		for ( CrewType crewType : ftlConstants.getCrewTypes() ) {
			crewLookup.put( crewType.getId(), crewType );
		}

		Map<String, SystemType> systemLookup = new LinkedHashMap<String, SystemType>();
		for ( SystemType systemType : SystemType.values() ) {
			systemLookup.put( systemType.getId(), systemType );
		}
		itemLookups.put( StoreItemType.WEAPON, weaponLookup );
		itemLookups.put( StoreItemType.DRONE, droneLookup );
		itemLookups.put( StoreItemType.AUGMENT, augmentLookup );
		itemLookups.put( StoreItemType.CREW, crewLookup );
		itemLookups.put( StoreItemType.SYSTEM, systemLookup );

		editorPanel.getCombo( SHELF_TYPE ).removeAllItems();
		for ( StoreItemType itemType : itemTypes ) {
			editorPanel.getCombo( SHELF_TYPE ).addItem( itemType );
		}
		typeChanged();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ignoreChanges = false;
			}
		});
	}

	/**
	 * Adjusts this panel match the content of a shelf.
	 *
	 * Note: Items with unrecognized ids will be logged and discarded.
	 */
	public void setShelf( StoreShelf shelf ) {
		ignoreChanges = true;

		StoreItemType itemType = shelf.getItemType();

		editorPanel.getCombo( SHELF_TYPE ).setSelectedItem( itemType );
		typeChanged();

		Map<String, ?> lookupMap = itemLookups.get( itemType );

		List<String> badIds = new ArrayList<String>();

		for ( int i=0; i < SLOTS.length; i++ ) {
			if ( i < shelf.getItems().size() ) {
				String itemId = shelf.getItems().get( i ).getItemId();
				Object itemChoice;

				if ( lookupMap != null && (itemChoice=lookupMap.get( itemId )) != null ) {

					editorPanel.getCombo( SLOTS[i] ).setSelectedItem( itemChoice );
				}
				else {
					editorPanel.getCombo( SLOTS[i] ).setSelectedItem( "" );
					badIds.add( itemId );
				}

				editorPanel.getBoolean( AVAIL[i] ).setSelected( shelf.getItems().get( i ).isAvailable() );
				editorPanel.getInt( EXTRA[i] ).setText( ""+shelf.getItems().get( i ).getExtraData() );
			}
			else {
				editorPanel.getCombo( SLOTS[i] ).setSelectedItem( "" );

				itemSlotChanged( i );
			}
		}

		if ( badIds.size() > 0 ) {
			StringBuilder errorBuf = new StringBuilder();
			errorBuf.append( "Unrecognized store items:" );
			for ( String badId : badIds ) {
				errorBuf.append( " " ).append( badId );
			}
			log.error( errorBuf.toString() );
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ignoreChanges = false;
			}
		});
	}

	/**
	 * Returns a list of StoreItems constructed from non-blank selections.
	 */
	@SuppressWarnings("unchecked")
	public List<StoreItem> getItems() {
		List<StoreItem> result = new ArrayList<StoreItem>( 3 );

		Map<String, ?> lookupMap = null;
		Object selectedTypeObj = editorPanel.getCombo( SHELF_TYPE ).getSelectedItem();

		if ( selectedTypeObj != null ) {
			StoreItemType selectedType = (StoreItemType)selectedTypeObj;
			lookupMap = itemLookups.get( selectedType );

			if ( lookupMap != null ) {
				for ( int i=0; i < SLOTS.length; i++ ) {

					Object selectedItem = editorPanel.getCombo( SLOTS[i] ).getSelectedItem();
					if ( "".equals( selectedItem ) == false ) {

						// Do a reverse lookup on the map to get an item id.
						for ( Map.Entry<String, ?> entry : lookupMap.entrySet() ) {
							if ( entry.getValue().equals( selectedItem ) ) {
								boolean available = editorPanel.getBoolean( AVAIL[i] ).isSelected();
								String id = entry.getKey();

								StoreItem newItem = new StoreItem( id );
								newItem.setAvailable( available );

								try { newItem.setExtraData( editorPanel.parseInt( EXTRA[i] ) ); }
								catch ( NumberFormatException e ) {}

								result.add( newItem );
							}
						}
					}
				}
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public StoreItemType getItemType() {
		Object selectedType = editorPanel.getCombo( SHELF_TYPE ).getSelectedItem();
		return (StoreItemType)selectedType;
	}

	/**
	 * Repopulates slot combos and resets availability, after type changes.
	 */
	private void typeChanged() {
		Map<String, ?> lookupMap = null;
		Object selectedTypeObj = editorPanel.getCombo( SHELF_TYPE ).getSelectedItem();

		if ( selectedTypeObj != null ) {
			StoreItemType selectedType = (StoreItemType)selectedTypeObj;
			lookupMap = itemLookups.get( selectedType );
		}

		for (int i=0; i < SLOTS.length; i++) {
			editorPanel.getCombo( SLOTS[i] ).removeAllItems();
			editorPanel.getCombo( SLOTS[i] ).addItem( "" );

			if ( lookupMap != null ) {
				for ( Object o : lookupMap.values() ) {
					editorPanel.getCombo( SLOTS[i] ).addItem( o );
				}
			}

			itemSlotChanged( i );
		}
	}

	/**
	 * Resets availability and extra data, after an item slot combo changes.
	 *
	 * Availability defaults to true if item is non-blank, false otherwise.
	 */
	private void itemSlotChanged( int n ) {
		JComboBox itemCombo = editorPanel.getCombo( SLOTS[n] );
		editorPanel.getBoolean( AVAIL[n] ).setSelected( !"".equals( itemCombo.getSelectedItem() ) );
		editorPanel.getInt( EXTRA[n] ).setText( ""+ 0 );
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed( ActionEvent e ) {
		if ( ignoreChanges ) return;
		boolean startedIgnoring = false;

		Object source = e.getSource();

		if ( source == editorPanel.getCombo( SHELF_TYPE ) ) {
			ignoreChanges = true;
			startedIgnoring = true;

			typeChanged();
		}
		else {
			// Check if the source was an item slot combo.
			for ( int i=0; i < SLOTS.length; i++ ) {
				if ( source == editorPanel.getCombo( SLOTS[i] ) ) {
					ignoreChanges = true;
					startedIgnoring = true;

					itemSlotChanged( i );
					break;
				}
			}
		}

		if ( startedIgnoring ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ignoreChanges = false;
				}
			});
		}
	}
}
