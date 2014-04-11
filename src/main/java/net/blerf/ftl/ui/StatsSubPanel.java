package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.SavedGameParser.CrewType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class StatsSubPanel extends JPanel implements ActionListener {

	private static final Logger log = LogManager.getLogger(ProfileStatsPanel.class);

	private int COLUMN_COUNT = 0;
	private final int DESC_COL = COLUMN_COUNT++;
	private final int NAME_COL = COLUMN_COUNT++;
	private final int SCORE_COL = COLUMN_COUNT++;
	private final int GAP_COL = COLUMN_COUNT++;
	private final int EDIT_COL = COLUMN_COUNT++;

	private final String RACE = "Race";
	private final String MALE = "Male";
	private final String NAME = "Name";
	private final String SCORE = "Score";

	private HashMap<JButton,String> editMap = new HashMap<JButton,String>();
	private HashMap<String,StatRow> rowMap = new HashMap<String,StatRow>();

	GridBagConstraints gridC = null;

	public StatsSubPanel() {
		super(new GridBagLayout());
		removeAll();
	}

	@Override
	public void removeAll() {
		super.removeAll();
		rowMap.clear();
		editMap.clear();

		gridC = new GridBagConstraints();
		gridC.anchor = GridBagConstraints.WEST;
		gridC.fill = GridBagConstraints.NONE;
		gridC.weightx = 1.0;
		gridC.weighty = 0.0;
		gridC.insets = new Insets(2, 4, 2, 4);
		gridC.gridwidth = 1;
		gridC.gridx = 0;
		gridC.gridy = 0;
	}

	public void addRow( String desc, String race, boolean male, String name, int score ) {
		StatRow row = new StatRow( desc, race, male, name, score );

		gridC.gridx = DESC_COL;
		gridC.anchor = GridBagConstraints.WEST;
		gridC.fill = GridBagConstraints.NONE;
		gridC.weightx = 1.0;
		gridC.gridwidth = 1;
		this.add( row.descLbl, gridC );

		gridC.gridx = NAME_COL;
		gridC.anchor = GridBagConstraints.CENTER;
		gridC.fill = GridBagConstraints.NONE;
		gridC.weightx = 1.0;
		this.add( row.nameLbl, gridC );

		gridC.gridx = SCORE_COL;
		gridC.anchor = GridBagConstraints.CENTER;
		gridC.weightx = 0.0;
		this.add( row.scoreLbl, gridC );

		gridC.gridx = GAP_COL;
		gridC.anchor = GridBagConstraints.CENTER;
		gridC.weightx = 0.0;
		this.add( Box.createHorizontalStrut(5), gridC );

		gridC.gridx = EDIT_COL;
		gridC.anchor = GridBagConstraints.CENTER;
		gridC.weightx = 0.0;
		row.editBtn.addActionListener(this);
		this.add( row.editBtn, gridC );

		editMap.put( row.editBtn, row.desc );
		rowMap.put( row.desc, row );
		row.makeSane();

		gridC.gridy++;
	}

	public void addBlankRow() {
		gridC.fill = GridBagConstraints.NONE;
		gridC.weighty = 0.0;
		gridC.gridwidth = GridBagConstraints.REMAINDER;
		gridC.gridx = 0;

		this.add( Box.createVerticalStrut(12), gridC );
		gridC.gridy++;
	}

	public void addFillRow() {
		gridC.fill = GridBagConstraints.VERTICAL;
		gridC.weighty = 1.0;
		gridC.gridwidth = GridBagConstraints.REMAINDER;
		gridC.gridx = 0;

		this.add( Box.createVerticalGlue(), gridC );
		gridC.gridy++;
	}

	private StatRow getRow( String desc ) {
		StatRow result = rowMap.get( desc );
		if ( result == null )
			throw new IndexOutOfBoundsException("Requested stat was not found ("+ desc +")");
		return result;
	}

	public String getRace( String desc ) {
		return getRow(desc).race;
	}

	public boolean isMale( String desc ) {
		return getRow(desc).male;
	}

	public String getName( String desc ) {
		return getRow(desc).name;
	}

	public int getScore( String desc ) {
		return getRow(desc).score;
	}

	/**
	 * Gets a crew icon, cropped as small as possible.
	 */
	private ImageIcon getCrewIcon( String imgRace ) {
		if (imgRace == null || imgRace.length() == 0) return null;

		ImageIcon result = null;
		int offsetX = 0, offsetY = 0, w = 35, h = 35;
		String innerPath = null;

		String[] candidatePaths = new String[2];
		candidatePaths[0] = "img/people/"+ imgRace +"_player_yellow.png";  // FTL 1.01-1.03.3
		candidatePaths[1] = "img/people/"+ imgRace +"_base.png";  // FTL 1.5.4
		for ( String candidatePath : candidatePaths ) {
			if ( DataManager.get().hasResourceInputStream( candidatePath ) ) {
				innerPath = candidatePath;
			}
		}
		if ( innerPath == null ) {
			log.error( "Failed to find an image file for crew race: "+ imgRace );
			return null;
		}

		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream( innerPath );
			BufferedImage bigImage = ImageIO.read( in );
			BufferedImage croppedImage = bigImage.getSubimage(offsetX, offsetY, w, h);

			// Shrink the crop area until non-transparent pixels are hit.
			int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE;
			int highX = -1, highY = -1;
			for (int testY=0; testY < h; testY++) {
				for (int testX=0; testX < w; testX++) {
					int pixel = croppedImage.getRGB(testX, testY);
					int alpha = (pixel >> 24) & 0xFF;  // 24:A, 16:R, 8:G, 0:B.
					if (alpha != 0) {
						if (testX > highX) highX = testX;
						if (testY > highY) highY = testY;
						if (testX < lowX) lowX = testX;
						if (testY < lowY) lowY = testY;
					}
				}
			}
			log.trace( "Crew Icon Trim Bounds: "+ lowX +","+ lowY +" "+ highX +"x"+ highY +" "+ imgRace );
			if (lowX >= 0 && lowY >= 0 && highX < w && highY < h && lowX < highX && lowY < highY) {
				croppedImage = croppedImage.getSubimage(lowX, lowY, highX-lowX+1, highY-lowY+1);
			}
			result = new ImageIcon(croppedImage);
		}
		catch ( RasterFormatException e ) {
			log.error( String.format("Failed to load and crop crew icon (%s).", imgRace), e );
		}
		catch ( IOException e ) {
			log.error( String.format("Failed to load and crop crew icon (%s).", imgRace), e );
		}
		finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
    }
		return result;
	}

	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();
		if ( editMap.containsKey(source) ) {
			final StatRow row = getRow( editMap.get(source) );

			JPanel popupPanel = new JPanel(new BorderLayout());

			final FieldEditorPanel editorPanel = new FieldEditorPanel( true );
			editorPanel.addRow( RACE, FieldEditorPanel.ContentType.COMBO );
			editorPanel.addRow( MALE, FieldEditorPanel.ContentType.BOOLEAN );
			editorPanel.addRow( NAME, FieldEditorPanel.ContentType.STRING );
			editorPanel.addRow( SCORE, FieldEditorPanel.ContentType.INTEGER );

			if ( row.race != null ) {
				editorPanel.getCombo(RACE).addItem( CrewType.CRYSTAL.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.ENERGY.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.ENGI.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.GHOST.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.HUMAN.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.MANTIS.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.ROCK.getId() );
				editorPanel.getCombo(RACE).addItem( CrewType.SLUG.getId() );
				editorPanel.getCombo(RACE).setSelectedItem( row.race );
				editorPanel.setBoolAndReminder( MALE, row.male );
			} else {
				editorPanel.getCombo(RACE).setEnabled(false);
				editorPanel.getBoolean(MALE).setEnabled(false);
			}

			if ( row.name != null ) {
				editorPanel.setStringAndReminder( NAME, row.name );
			} else {
				editorPanel.getString(NAME).setEnabled(false);
			}

			editorPanel.setIntAndReminder( SCORE, row.score );
			popupPanel.add(new JLabel(row.desc), BorderLayout.NORTH);
			popupPanel.add(editorPanel, BorderLayout.CENTER);

			JPanel ctrlPanel = new JPanel();
			ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.X_AXIS));
			ctrlPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			ctrlPanel.add(Box.createHorizontalGlue());
			JButton popupOkBtn = new JButton("OK");
			ctrlPanel.add(popupOkBtn);
			ctrlPanel.add(Box.createHorizontalGlue());
			JButton popupCancelBtn = new JButton("Cancel");
			ctrlPanel.add(popupCancelBtn);
			ctrlPanel.add(Box.createHorizontalGlue());
			popupPanel.add(ctrlPanel, BorderLayout.SOUTH);
			popupOkBtn.setPreferredSize(popupCancelBtn.getPreferredSize());

			final JDialog popup = new JDialog((java.awt.Frame)this.getTopLevelAncestor(), "Edit Score", true);
			popup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			popup.getContentPane().add(popupPanel);
			popup.pack();
			popup.setLocationRelativeTo(null);

			popupCancelBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					popup.setVisible(false);
					popup.dispose();
				}
			});

			popupOkBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String newString = null;

					if ( row.race != null ) {
						newString = (String)editorPanel.getCombo(RACE).getSelectedItem();
						if ( CrewType.findById( newString ) != null ) {
							row.race = newString;
							row.male = editorPanel.getBoolean(MALE).isSelected();
						}
					}
					if ( row.name != null )
						row.name = editorPanel.getString(NAME).getText();

					newString = editorPanel.getInt(SCORE).getText();
					try { row.score = Integer.parseInt(newString); }
					catch (NumberFormatException f) {}

					row.makeSane();

					popup.setVisible(false);
					popup.dispose();
				}
			});

			popup.setVisible(true);
		}
	}



	private class StatRow {
		public String desc = null;
		public String race = null;
		public boolean male;
		public String name = null;
		public int score = 0;

		public JLabel descLbl = null;
		public JLabel nameLbl = null;
		public JLabel scoreLbl = null;
		public JButton editBtn = null;

		public StatRow( String desc, String race, boolean male, String name, int score ) {
			this.desc = desc;
			this.race = race;
			this.male = male;
			this.name = name;
			this.score = score;

			descLbl = new JLabel(desc);
			nameLbl = new JLabel();
			nameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
			scoreLbl = new JLabel(Integer.toString(score));
			editBtn = new JButton("Edit");
			editBtn.setMargin(new Insets(0,0,0,0));
		}

		public void makeSane() {
			descLbl.setText(desc);

			ImageIcon icon = null;
			if ( race != null ) {
				String imgRace = race;

				if ( CrewType.HUMAN.getId().equals(race) ) {
					// Human females have a distinct sprite (Other races look the same either way).
					if ( !male )
						imgRace = "female";
				}
				else if ( CrewType.GHOST.getId().equals(race) ) {
					// Ghosts look like translucent humans.
					if ( male )
						imgRace = "human";
					else
						imgRace = "female";
				}

				icon = getCrewIcon(imgRace);
			}
			nameLbl.setIcon(icon);

			if ( name != null )
				nameLbl.setText(name);
			else
				nameLbl.setText("");

			scoreLbl.setText(Integer.toString(score));
		}
	}
}
