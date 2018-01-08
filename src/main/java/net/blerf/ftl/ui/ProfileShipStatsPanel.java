package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Stats;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.ScorePanel;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;


public class ProfileShipStatsPanel extends JPanel implements ActionListener {

	private static final Logger log = LoggerFactory.getLogger( ProfileShipStatsPanel.class );

	private static final int ACH_LOCKED = 0;
	private static final int MAX_SCORE_PANELS = 4;

	private FTLFrame frame;

	private Map<String, Map<Rectangle, BufferedImage>> cachedImages = new HashMap<String, Map<Rectangle, BufferedImage>>();
	private BufferedImage questImage = null;
	private BufferedImage victoryAImage = null;
	private BufferedImage victoryBImage = null;
	private BufferedImage victoryCImage = null;

	private Map<String, List<Score>> allBestMap = new LinkedHashMap<String, List<Score>>();
	private String currentShipId = null;

	private JComboBox bestCombo = null;
	private JPanel bestScoresPanel = null;
	private List<ScorePanel> bestScorePanels = new ArrayList<ScorePanel>();

	private Map<String, Achievement> questAchs = new HashMap<String, Achievement>();
	private Map<String, Achievement> victoryAchs = new HashMap<String, Achievement>();
	private Map<String, IconCycleButton> questBoxes = new HashMap<String, IconCycleButton>();
	private Map<String, IconCycleButton> victoryABoxes = new HashMap<String, IconCycleButton>();
	private Map<String, IconCycleButton> victoryBBoxes = new HashMap<String, IconCycleButton>();
	private Map<String, IconCycleButton> victoryCBoxes = new HashMap<String, IconCycleButton>();


	public ProfileShipStatsPanel( FTLFrame frame ) {
		this.frame = frame;

		this.setLayout( new GridLayout(0, 2) );

		JPanel leftPanel = new JPanel( new GridBagLayout() );
		leftPanel.setBorder( BorderFactory.createTitledBorder( "Ship Best" ) );

		GridBagConstraints leftC = new GridBagConstraints();

		leftC.anchor = GridBagConstraints.CENTER;
		leftC.fill = GridBagConstraints.HORIZONTAL;
		leftC.weightx = 1.0;
		leftC.weighty = 0.0;
		leftC.gridx = 0;
		leftC.gridy = 0;
		JPanel bestChooserPanel = new JPanel( new GridBagLayout() );
		GridBagConstraints chooserC = new GridBagConstraints();

		chooserC.fill = GridBagConstraints.HORIZONTAL;
		bestCombo = new JComboBox();
		bestChooserPanel.add( bestCombo );
		leftPanel.add( bestChooserPanel, leftC );

		leftC.anchor = GridBagConstraints.NORTH;
		leftC.fill = GridBagConstraints.HORIZONTAL;
		leftC.weightx = 1.0;
		leftC.weighty = 1.0;
		leftC.gridy++;
		bestScoresPanel = new JPanel();
		bestScoresPanel.setLayout( new BoxLayout(bestScoresPanel, BoxLayout.Y_AXIS ) );
		leftPanel.add( bestScoresPanel, leftC );

		this.add( leftPanel );

		questImage = ImageUtilities.getBundledImage( "ach_quest.png", this.getClass().getClassLoader() );
		victoryAImage = ImageUtilities.getBundledImage( "ach_victory_type-a.png", this.getClass().getClassLoader() );
		victoryBImage = ImageUtilities.getBundledImage( "ach_victory_type-b.png", this.getClass().getClassLoader() );
		victoryCImage = ImageUtilities.getBundledImage( "ach_victory_type-c.png", this.getClass().getClassLoader() );

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout( new BoxLayout( rightPanel, BoxLayout.Y_AXIS ) );
		rightPanel.setBorder( BorderFactory.createTitledBorder( "Quest and Victory Achievements" ) );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			JPanel panel = createQVAchPanel( baseId );
			if ( panel != null ) rightPanel.add( panel );
		}

		this.add( rightPanel );
	}


	private JPanel createQVAchPanel( String baseId ) {

		log.trace( "Creating quest/victory achievement panel for: "+ baseId );

		ShipBlueprint variantAShip = DataManager.get().getPlayerShipVariant( baseId, 0 );
		ShipBlueprint variantBShip = DataManager.get().getPlayerShipVariant( baseId, 1 );
		ShipBlueprint variantCShip = DataManager.get().getPlayerShipVariant( baseId, 2 );
		if ( variantAShip == null ) return null;

		Achievement questAch = null;
		Achievement victoryAch = null;
		List<Achievement> shipAchs = DataManager.get().getShipAchievements( variantAShip );
		for ( Achievement ach : shipAchs ) {
			if ( ach.isQuest() ) questAch = ach;
			if ( ach.isVictory() ) victoryAch = ach;
		}
		questAchs.put( baseId, questAch );
		victoryAchs.put( baseId, victoryAch );

		String shipClass = variantAShip.getShipClass().getTextValue();

		JPanel panel = new JPanel();
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		panel.setBorder( BorderFactory.createTitledBorder( shipClass ) );

		if ( questAch != null ) {
			IconCycleButton questBox = ImageUtilities.createCycleButton( questImage, true );
			questBox.addMouseListener( new StatusbarMouseListener( frame, "Quest" ) );
			questBoxes.put( baseId, questBox );
			panel.add( questBox );
		} else {
			IconCycleButton questBox = ImageUtilities.createDummyCycleButton();
			questBox.addMouseListener( new StatusbarMouseListener( frame, "N/A" ) );
			questBoxes.put( baseId, questBox );
			panel.add( questBox );
		}

		if ( victoryAch != null ) {
			IconCycleButton victoryABox = ImageUtilities.createCycleButton( victoryAImage, true );
			victoryABox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-A: "+ variantAShip.getName().getTextValue() ) );
			victoryABoxes.put( baseId, victoryABox );
			panel.add( victoryABox );
		} else {
			IconCycleButton victoryABox = ImageUtilities.createDummyCycleButton();
			victoryABox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-A: N/A" ) );
			victoryABoxes.put( baseId, victoryABox );
			panel.add( victoryABox );
		}

		if ( victoryAch != null && variantBShip != null ) {
			IconCycleButton victoryBBox = ImageUtilities.createCycleButton( victoryBImage, true );
			victoryBBox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-B: "+ variantBShip.getName().getTextValue() ) );
			victoryBBoxes.put( baseId, victoryBBox );
			panel.add( victoryBBox );
		} else {
			IconCycleButton victoryBBox = ImageUtilities.createDummyCycleButton();
			victoryBBox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-B: N/A" ) );
			victoryBBoxes.put( baseId, victoryBBox );
			panel.add( victoryBBox );
		}

		if ( victoryAch != null && variantCShip != null ) {
			IconCycleButton victoryCBox = ImageUtilities.createCycleButton( victoryCImage, true );
			victoryCBox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-C: "+ variantCShip.getName().getTextValue() ) );
			victoryCBoxes.put( baseId, victoryCBox );
			panel.add( victoryCBox );
		} else {
			IconCycleButton victoryCBox = ImageUtilities.createDummyCycleButton();
			victoryCBox.addMouseListener( new StatusbarMouseListener( frame, "Victory with Type-C: N/A" ) );
			victoryCBoxes.put( baseId, victoryCBox );
			panel.add( victoryCBox );
		}

		return panel;
	}

	private int getCycleStateForDifficulty( Difficulty d ) {
		if ( d == null ) return ACH_LOCKED;
		return ( 1 + d.ordinal() );                // LOCKED plus 0-based enum index.
	}

	private Difficulty getDifficultyForCycleState( int cycleState ) {
		if ( cycleState == ACH_LOCKED ) return null;
		return Difficulty.values()[cycleState-1];  // Adjust array index because of LOCKED.
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

		if ( source == bestCombo ) {
			setShipId( getComboShipId() );
		}
	}

	@SuppressWarnings("unchecked")
	private String getComboShipId() {
		String result = null;
		Object shipObj = bestCombo.getSelectedItem();

		if ( shipObj instanceof ShipBlueprint ) {
			ShipBlueprint ship = (ShipBlueprint)shipObj;
			result = ship.getId();
		}
		else if ( shipObj instanceof String && "".equals(shipObj) == false ) {
			result = (String)shipObj;
		}
		return result;
	}

	private void setShipId( String newShipId ) {
		if ( currentShipId != null ) applyScores();

		bestScoresPanel.removeAll();
		bestScorePanels.clear();

		currentShipId = newShipId;

		if ( currentShipId != null ) {
			List<Score> shipScoreList = allBestMap.get( currentShipId );
			if ( shipScoreList != null ) {
				int i = 0;
				for ( Score s : shipScoreList ) {
					ScorePanel bsp = new ScorePanel( ++i, s );
					bsp.setCacheMap( cachedImages );
					bsp.setShipId( currentShipId );
					bsp.setShipIdEditingEnabled( false );
					bsp.setBlankable( true );
					bsp.setEditable( true );
					bestScoresPanel.add( bsp );
					bestScorePanels.add( bsp );
				}
			}
		}
		// Add blank panels to fill all remaining slots.
		while ( bestScorePanels.size() < MAX_SCORE_PANELS ) {
			ScorePanel bsp = new ScorePanel( bestScorePanels.size()+1, null );
			bsp.setCacheMap( cachedImages );
			bsp.setShipId( currentShipId );
			bsp.setShipIdEditingEnabled( false );
			bsp.setBlankable( true );
			bsp.setEditable( currentShipId != null );
			bestScoresPanel.add( bsp );
			bestScorePanels.add( bsp );
		}

		this.revalidate();
		this.repaint();
	}

	private void applyScores() {
		if ( currentShipId == null ) return;

		List<Score> shipScoreList = allBestMap.get( currentShipId );
		if ( shipScoreList == null ) {
			shipScoreList = new ArrayList<Score>(4);
			allBestMap.put( currentShipId, shipScoreList );
		}
		shipScoreList.clear();

		for ( ScorePanel bsp : bestScorePanels ) {
			if ( bsp.isBlank() ) continue;
			if ( bsp.getShipName().length() == 0 || bsp.getShipId().length() == 0 ) continue;

			// Ignore panels that changed to another ship? *shrug*
			// TODO: Handle ScorePanels with setShipIdEditable(true).

			if ( !currentShipId.equals( bsp.getShipId() ) ) continue;

			Score newScore = bsp.createScore();
			if ( newScore == null ) continue;
			shipScoreList.add( newScore );
		}
	}

	private void resetCombo() {
		setShipId( null );

		bestCombo.removeActionListener( this );
		bestCombo.removeAllItems();
		currentShipId = null;

		bestCombo.addItem( "" );
		bestCombo.setSelectedItem( "" );

		bestCombo.addActionListener( this );
	}

	public void setProfile( Profile p ) throws IOException {
		resetCombo();
		allBestMap.clear();

		for ( Score s : p.getStats().getShipBest() ) {
			List<Score> shipScoreList = allBestMap.get( s.getShipId() );
			if ( shipScoreList == null ) {
				shipScoreList = new ArrayList<Score>( 4 );
				allBestMap.put( s.getShipId(), shipScoreList );
			}
			shipScoreList.add( new Score( s ) );
		}

		Map<String, ShipBlueprint> playerShipIdMap = DataManager.get().getPlayerShips();
		for ( ShipBlueprint ship : playerShipIdMap.values() ) {
			bestCombo.addItem( ship );
		}

		// Add any non-standard ships...
		boolean first = true;
		for ( String shipId : allBestMap.keySet() ) {
			if ( !playerShipIdMap.containsKey( shipId ) ) {
				if ( first ) {
					bestCombo.addItem( "" );
					first = false;
				}
				bestCombo.addItem( shipId );
			}
		}

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			Achievement questAch = questAchs.get( baseId );
			Achievement victoryAch = victoryAchs.get( baseId );
			IconCycleButton questBox = questBoxes.get( baseId );
			IconCycleButton victoryABox = victoryABoxes.get( baseId );
			IconCycleButton victoryBBox = victoryBBoxes.get( baseId );
			IconCycleButton victoryCBox = victoryCBoxes.get( baseId );

			if ( questAch != null ) {
				questBox.setSelectedState( ACH_LOCKED );

				AchievementRecord questRec = AchievementRecord.getFromListById( p.getAchievements(), questAch.getId() );
				if ( questRec != null ) {
					questBox.setSelectedState( getCycleStateForDifficulty( questRec.getDifficulty() ) );
				}
			}

			if ( victoryAch != null ) {
				victoryABox.setSelectedState( ACH_LOCKED );
				victoryBBox.setSelectedState( ACH_LOCKED );
				victoryCBox.setSelectedState( ACH_LOCKED );

				AchievementRecord victoryRec = AchievementRecord.getFromListById( p.getAchievements(), victoryAch.getId() );
				if ( victoryRec != null ) {
					victoryABox.setSelectedState( getCycleStateForDifficulty( victoryRec.getCompletedWithTypeA() ) );
					victoryBBox.setSelectedState( getCycleStateForDifficulty( victoryRec.getCompletedWithTypeB() ) );
					victoryCBox.setSelectedState( getCycleStateForDifficulty( victoryRec.getCompletedWithTypeC() ) );
				}
			}
		}

		this.repaint();
	}

	public void updateProfile( Profile p ) {
		if ( currentShipId != null ) applyScores();

		Stats stats = p.getStats();

		List<Score> newBest = new ArrayList<Score>();
		
		for ( Map.Entry<String, List<Score>> entry : allBestMap.entrySet() ) {
			if ( entry.getValue() == null ) continue;
			for ( Score s : entry.getValue() ) {
				newBest.add( new Score( s ) );
			}
		}

		stats.setShipBest( newBest );

		for ( String baseId : DataManager.get().getPlayerShipBaseIds() ) {
			Achievement questAch = questAchs.get( baseId );
			Achievement victoryAch = victoryAchs.get( baseId );
			IconCycleButton questBox = questBoxes.get( baseId );
			IconCycleButton victoryABox = victoryABoxes.get( baseId );
			IconCycleButton victoryBBox = victoryBBoxes.get( baseId );
			IconCycleButton victoryCBox = victoryCBoxes.get( baseId );

			if ( questAch != null ) {
				AchievementRecord questRec = AchievementRecord.getFromListById( p.getAchievements(), questAch.getId() );

				Difficulty questDiff = getDifficultyForCycleState( questBox.getSelectedState() );

				if ( questDiff != null ) {
					if ( questRec == null ) {
						questRec = new AchievementRecord( questAch.getId(), questDiff );
						p.getAchievements().add( questRec );
					}
					questRec.setDifficulty( questDiff );
				}
				else if ( questRec != null ) {  // Null diff and there's a rec to remove.
					p.getAchievements().remove( questRec );
				}
			}

			if ( victoryAch != null ) {
				AchievementRecord victoryRec = AchievementRecord.getFromListById( p.getAchievements(), victoryAch.getId() );

				Difficulty victoryADiff = getDifficultyForCycleState( victoryABox.getSelectedState() );
				Difficulty victoryBDiff = getDifficultyForCycleState( victoryBBox.getSelectedState() );
				Difficulty victoryCDiff = getDifficultyForCycleState( victoryCBox.getSelectedState() );
				Difficulty highestDiff = null;
				for ( Difficulty d : new Difficulty[] {victoryADiff, victoryBDiff, victoryCDiff} ) {
					if ( highestDiff == null || ( d != null && d.compareTo( highestDiff ) > 0 ) ) {
						highestDiff = d;
					}
				}

				if ( highestDiff != null ) {
					if ( victoryRec == null ) {
						victoryRec = new AchievementRecord( victoryAch.getId(), highestDiff );
						p.getAchievements().add( victoryRec );
					}
					victoryRec.setDifficulty( highestDiff );
					victoryRec.setCompletedWithTypeA( victoryADiff );
					victoryRec.setCompletedWithTypeB( victoryBDiff );
					victoryRec.setCompletedWithTypeC( victoryCDiff );
				}
				else if ( victoryRec != null ) {  // Null diff and there's a rec to remove.
					p.getAchievements().remove( victoryRec );
				}
			}
		}
	}
}
