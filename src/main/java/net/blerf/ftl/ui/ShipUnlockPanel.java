package net.blerf.ftl.ui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.ShipAvailability;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.IconCycleButton;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ShipUnlockPanel extends JPanel {

	private static final int ACH_LOCKED = 0;
	private static final int ACH_EASY = 1;
	private static final int ACH_NORMAL = 2;
	private static final int ACH_HARD = 3;
	private static final int SHIP_LOCKED = 0;
	private static final int SHIP_UNLOCKED = 1;

	private static final Logger log = LogManager.getLogger(ShipUnlockPanel.class);

	private FTLFrame frame;

	private HashMap<String, IconCycleButton> shipABoxes = new HashMap<String, IconCycleButton>();
	private HashMap<String, IconCycleButton> shipCBoxes = new HashMap<String, IconCycleButton>();
	private HashMap<Achievement, IconCycleButton> shipAchBoxes = new HashMap<Achievement, IconCycleButton>();


	public ShipUnlockPanel( FTLFrame frame ) {
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		this.frame = frame;

		log.trace( "Creating Ship Unlock panel" );

		log.trace("Adding ship unlocks");

		JPanel shipPanel = new JPanel();
		shipPanel.setLayout( new GridLayout(0, 3) );
		shipPanel.setBorder( BorderFactory.createTitledBorder("Ship Unlocks") );
		this.add( shipPanel );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			JPanel panel = createShipUnlockPanel( baseId );
			if ( panel != null ) shipPanel.add( panel );
		}

		log.trace("Adding ship achievements");

		JPanel shipAchPanel = new JPanel();
		shipAchPanel.setLayout( new GridLayout(0, 3) );
		shipAchPanel.setBorder( BorderFactory.createTitledBorder("Ship Achievements") );
		this.add( shipAchPanel );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			JPanel panel = createShipAchPanel( baseId );
			if ( panel != null ) shipAchPanel.add( panel );
		}
	}

	private JPanel createShipUnlockPanel( String baseId ) {

		log.trace( "Creating ship unlock panel for: "+ baseId );

		ShipBlueprint variantAShip = DataManager.get().getPlayerShipVariant( baseId, 0 );
		ShipBlueprint variantBShip = DataManager.get().getPlayerShipVariant( baseId, 1 );
		ShipBlueprint variantCShip = DataManager.get().getPlayerShipVariant( baseId, 2 );
		if ( variantAShip == null ) return null;

		String shipClass = variantAShip.getShipClass();

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		panel.setBorder( BorderFactory.createTitledBorder( shipClass ) );

		IconCycleButton shipABox = frame.createCycleButton( "img/ship/"+ variantAShip.getGraphicsBaseName() +"_base.png", false );
		shipABox.addMouseListener( new StatusbarMouseListener(frame, "Type-A: "+ variantAShip.getName()) );
		shipABoxes.put( baseId, shipABox );
		panel.add( shipABox );

		if ( variantBShip != null ) {
			IconCycleButton shipBBox = frame.createDummyCycleButton();
			shipBBox.addMouseListener( new StatusbarMouseListener(frame, "Type-B: "+ variantBShip.getName() +" (To unlock, choose two ship achievements below.)") );
			panel.add( shipBBox );
		} else {
			IconCycleButton shipBBox = frame.createDummyCycleButton();
			shipBBox.addMouseListener( new StatusbarMouseListener(frame, "Type-B: N/A") );
			panel.add( shipBBox );
		}

		if ( variantCShip != null ) {
			IconCycleButton shipCBox = frame.createCycleButton( "img/ship/"+ variantCShip.getGraphicsBaseName() +"_base.png", false );
			shipCBox.addMouseListener( new StatusbarMouseListener(frame, "Type-C: "+ variantCShip.getName()) );
			shipCBoxes.put( baseId, shipCBox );
			panel.add( shipCBox );
		} else {
			IconCycleButton shipCBox = frame.createDummyCycleButton();
			shipCBox.addMouseListener( new StatusbarMouseListener(frame, "Type-C: N/A") );
			shipCBoxes.put( baseId, shipCBox );
			panel.add( shipCBox );
		}

		return panel;
	}

	private JPanel createShipAchPanel( String baseId ) {

		log.trace( "Creating ship achievements panel for: "+ baseId );

		ShipBlueprint variantAShip = DataManager.get().getPlayerShipVariant( baseId, 0 );
		if ( variantAShip == null ) return null;

		String shipClass = variantAShip.getShipClass();

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		panel.setBorder( BorderFactory.createTitledBorder( shipClass ) );

		List<Achievement> shipAchs = DataManager.get().getShipAchievements( variantAShip );		
		if ( shipAchs != null ) {
			for ( Achievement shipAch : shipAchs ) {
				IconCycleButton box = frame.createCycleButton( "img/" + shipAch.getImagePath(), true );
				box.setToolTipText( shipAch.getName() );

				String achDesc = shipAch.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
				box.addMouseListener( new StatusbarMouseListener(frame, achDesc) );

				shipAchBoxes.put( shipAch, box );
				panel.add( box );
			}
		}
		else {
			return null;
		}
		
		return panel;
	}

	public void unlockAllShips() {
		for ( IconCycleButton box : shipABoxes.values() ) {
			if ( box.getSelectedState() == SHIP_LOCKED )
				box.setSelectedState( SHIP_UNLOCKED );
		}
		for ( IconCycleButton box : shipCBoxes.values() ) {
			if ( box.getSelectedState() == SHIP_LOCKED )
				box.setSelectedState( SHIP_UNLOCKED );
		}
	}

	public void unlockAllShipAchievements() {
		for ( IconCycleButton box : shipAchBoxes.values() ) {
			if ( box.getSelectedState() == ACH_LOCKED )
				box.setSelectedState( ACH_EASY );
		}
	}

	public void setProfile( Profile p ) {

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			IconCycleButton shipABox = shipABoxes.get( baseId );
			IconCycleButton shipCBox = shipCBoxes.get( baseId );

			ShipAvailability shipAvail = p.getShipUnlockMap().get( baseId );
			if ( shipAvail != null ) {
				if ( shipABox != null ) shipABox.setSelectedState( (shipAvail.isUnlockedA() ? SHIP_UNLOCKED : SHIP_LOCKED) );
				if ( shipCBox != null ) shipCBox.setSelectedState( (shipAvail.isUnlockedC() ? SHIP_UNLOCKED : SHIP_LOCKED) );
			} else {
				if ( shipABox != null ) shipABox.setSelectedState( SHIP_LOCKED );
				if ( shipCBox != null ) shipCBox.setSelectedState( SHIP_LOCKED );
			}
		}

		for ( Map.Entry<Achievement, IconCycleButton> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			box.setSelectedState( ACH_LOCKED );

			for ( AchievementRecord rec : p.getAchievements() ) {
				if ( rec.getAchievementId().equals( achId ) ) {
					if ( rec.getDifficulty() == Difficulty.EASY ) {
						box.setSelectedState( ACH_EASY );
					}
					else if ( rec.getDifficulty() == Difficulty.NORMAL ) {
						box.setSelectedState( ACH_NORMAL );
					}
					else if ( rec.getDifficulty() == Difficulty.HARD ) {
						box.setSelectedState( ACH_HARD );
					}
					else {
						log.warn( String.format("Unexpected difficulty for achievement (\"%s\"): %s. Changed to EASY.", achId, rec.getDifficulty().toString()) );
						box.setSelectedState( ACH_EASY );
					}
				}
			}
		}
		this.repaint();
	}

	public void updateProfile( Profile p ) {

		ArrayList<AchievementRecord> newAchRecs = new ArrayList<AchievementRecord>();
		newAchRecs.addAll( p.getAchievements() );

		for ( Map.Entry<Achievement, IconCycleButton> entry : shipAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			if ( box.getSelectedState() != ACH_LOCKED ) {
				// Add selected achievement recs if not already present.

				Difficulty difficulty = null;
				if ( box.getSelectedState() == ACH_EASY ) {
					difficulty = Difficulty.EASY;
				}
				else if ( box.getSelectedState() == ACH_NORMAL ) {
					difficulty = Difficulty.NORMAL;
				}
				else if ( box.getSelectedState() == ACH_HARD ) {
					difficulty = Difficulty.HARD;
				}
				else {
					log.warn( String.format("Unexpected difficulty for achievement (\"%s\"): %d. Changed to EASY.", achId, box.getSelectedState()) );
					difficulty = Difficulty.EASY;
				}

				AchievementRecord newAch = AchievementRecord.getFromListById( newAchRecs, achId );
				if ( newAch != null ) {
					newAch.setDifficulty( difficulty );
				} else {
					newAch = new AchievementRecord( achId, difficulty );
					newAchRecs.add( newAch );
				}

			} else {
				// Remove achievement recs that are not selected.
				AchievementRecord.removeFromListById( newAchRecs, achId );
			}
		}

		Map<String, ShipAvailability> shipUnlockMap = new LinkedHashMap<String, ShipAvailability>();
		shipUnlockMap.putAll( p.getShipUnlockMap() );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			IconCycleButton shipABox = shipABoxes.get( baseId );
			IconCycleButton shipCBox = shipCBoxes.get( baseId );

			boolean unlockedA = ( shipABox != null && shipABox.getSelectedState() == SHIP_UNLOCKED );
			boolean unlockedC = ( shipCBox != null && shipCBox.getSelectedState() == SHIP_UNLOCKED );
			ShipAvailability shipAvail = shipUnlockMap.get( baseId );
			if ( shipAvail == null ) {
				shipAvail = new ShipAvailability( baseId );
				shipUnlockMap.put( baseId, shipAvail );
			}
			shipAvail.setUnlockedA( unlockedA );
			shipAvail.setUnlockedC( unlockedC );

			// Remove ship achievements for locked ships
			if ( !unlockedA ) {
				List<Achievement> shipAchs = DataManager.get().getShipAchievements( DataManager.get().getPlayerShipVariant( baseId, 0 ) );
				if ( shipAchs != null ) {
					for ( Achievement shipAch : shipAchs ) {
						// Search for records with the doomed id.
						AchievementRecord.removeFromListById( newAchRecs, shipAch.getId() );
					}
				}
			}
		}
		p.setShipUnlockMap( shipUnlockMap );

		p.setAchievements( newAchRecs );
	}
}
