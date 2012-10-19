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
import net.blerf.ftl.ui.StatusbarMouseListener;
import net.blerf.ftl.xml.Achievement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class GeneralAchievementsPanel extends JPanel {

	private static final Logger log = LogManager.getLogger(GeneralAchievementsPanel.class);

	private FTLFrame frame;

	private HashMap<Achievement, JCheckBox> generalAchBoxes = new HashMap<Achievement, JCheckBox>();

	// TODO: allow user selection of difficulty.
	private Difficulty difficulty = Difficulty.EASY;

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
		
		// TODO: magic number 7.
		for (int i = 0; i < 7; i++) {
			Achievement ach = achievements.get( i+offset );
			log.trace( "Setting icons for checkbox. Base image: " + "img/" + ach.getImagePath() );

			JCheckBox box = new JCheckBox();
			frame.setCheckboxIcons(box, "img/" + ach.getImagePath());
			box.setToolTipText( ach.getName() );

			String achDesc = ach.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(frame, achDesc) );

			generalAchBoxes.put(ach, box);
			panel.add( box );
		}
		
		return panel;
	}

	public void setProfile( Profile p ) {

		for ( Map.Entry<Achievement, JCheckBox> entry : generalAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			JCheckBox box = entry.getValue();

			box.setSelected(false);

			for ( AchievementRecord rec : p.getAchievements() )
				if ( rec.getAchievementId().equals( achId ) )
					box.setSelected(true);
		}
		this.repaint();
	}

	public void updateProfile( Profile p ) {
		ArrayList<AchievementRecord> newAchRecs = new ArrayList<AchievementRecord>();
		newAchRecs.addAll( p.getAchievements() );

		for ( Map.Entry<Achievement, JCheckBox> entry : generalAchBoxes.entrySet() ) {
			String achId = entry.getKey().getId();
			JCheckBox box = entry.getValue();
			if ( box.isSelected() ) {
				// Add selected achievement recs if not already present.
				if ( !AchievementRecord.listContainsId( newAchRecs, achId ) ) {
					newAchRecs.add( new AchievementRecord(achId, difficulty) );
				}
			} else {
				// Remove achievement recs that are not selected.
				AchievementRecord.removeFromListById( newAchRecs, achId );
			}
		}

		p.setAchievements(newAchRecs);
	}
}
