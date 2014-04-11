package net.blerf.ftl.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;
import net.blerf.ftl.ui.IconCycleButton;
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GeneralAchievementsPanel extends JPanel {

	private static final int ICON_LOCKED = 0;
	private static final int ICON_EASY = 1;
	private static final int ICON_NORMAL = 2;
	private static final int ICON_HARD = 3;

	private static final Logger log = LogManager.getLogger(GeneralAchievementsPanel.class);

	private FTLFrame frame;

	private HashMap<Achievement, IconCycleButton> generalAchBoxes = new HashMap<Achievement, IconCycleButton>();


	public GeneralAchievementsPanel( FTLFrame frame ) {
		this.setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
		this.frame = frame;

		log.trace( "Creating General Achievements panel" );

		List<Achievement> achievements = DataManager.get().getGeneralAchievements();

		// TODO: magic offsets.
		this.add( createAchievementsSubPanel( "General Progression", achievements, 0 ) );
		this.add( createAchievementsSubPanel( "Going the Distance", achievements, 7 ) );
		this.add( createAchievementsSubPanel( "Skill and Equipment Feats", achievements, 14 ) );
	}

	private JPanel createAchievementsSubPanel( String title, List<Achievement> achievements, int offset ) {
		JPanel panel = new JPanel();
		panel.setBorder( BorderFactory.createTitledBorder(title) );
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		
		// TODO: Magic number 7.
		for (int i=0; i < 7; i++) {
			Achievement ach = achievements.get( i+offset );
			log.trace( "Setting icons for cycle button. Base image: " + "img/" + ach.getImagePath() );

			IconCycleButton box = frame.createCycleButton( "img/" + ach.getImagePath(), true );
			box.setToolTipText( ach.getName() );

			String achDesc = ach.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(frame, achDesc) );

			generalAchBoxes.put( ach, box );
			panel.add( box );
		}
		
		return panel;
	}

	public void setProfile( Profile p ) {
		for ( Map.Entry<Achievement, IconCycleButton> entry : generalAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			box.setSelectedState( ICON_LOCKED );

			for ( AchievementRecord rec : p.getAchievements() ) {
				if ( rec.getAchievementId().equals( achId ) ) {
					if ( rec.getDifficulty() == Difficulty.EASY ) {
						box.setSelectedState( ICON_EASY );
					}
					else if ( rec.getDifficulty() == Difficulty.NORMAL ) {
						box.setSelectedState( ICON_NORMAL );
					}
					else if ( rec.getDifficulty() == Difficulty.HARD ) {
						box.setSelectedState( ICON_HARD );
					}
					else {
						log.warn( String.format("Unexpected difficulty for achievement (\"%s\"): %s. Changed to EASY.", achId, rec.getDifficulty().toString()) );
						box.setSelectedState( ICON_EASY );
					}
				}
			}
		}
		this.repaint();
	}

	public void updateProfile( Profile p ) {
		ArrayList<AchievementRecord> newAchRecs = new ArrayList<AchievementRecord>();
		newAchRecs.addAll( p.getAchievements() );

		for ( Map.Entry<Achievement, IconCycleButton> entry : generalAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			IconCycleButton box = entry.getValue();

			if ( box.getSelectedState() != ICON_LOCKED ) {
				// Add selected achievement recs if not already present.

				Difficulty difficulty = null;
				if ( box.getSelectedState() == ICON_EASY ) {
					difficulty = Difficulty.EASY;
				}
				else if ( box.getSelectedState() == ICON_NORMAL ) {
					difficulty = Difficulty.NORMAL;
				}
				else if ( box.getSelectedState() == ICON_HARD ) {
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

		p.setAchievements(newAchRecs);
	}
}
