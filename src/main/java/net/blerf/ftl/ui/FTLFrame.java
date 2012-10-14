package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBException;

import net.blerf.ftl.model.AchievementRecord;
import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Score.Difficulty;
import net.blerf.ftl.model.Stats;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.ProfileParser;
import net.blerf.ftl.xml.Achievement;
import net.blerf.ftl.xml.ShipBlueprint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FTLFrame extends JFrame {
	
	private static final Logger log = LogManager.getLogger(FTLFrame.class);
	
	private List<JCheckBox> shipUnlocks = new ArrayList<JCheckBox>();
	
	private HashMap<Achievement,JCheckBox> shipAchievements = new HashMap<Achievement, JCheckBox>();
	private HashMap<Achievement,JCheckBox> generalAchievements = new HashMap<Achievement, JCheckBox>();
	
	private Profile profile;
	
	private ImageIcon openIcon = new ImageIcon( ClassLoader.getSystemResource("open.gif") );
	private ImageIcon saveIcon = new ImageIcon( ClassLoader.getSystemResource("save.gif") );
	private ImageIcon unlockIcon = new ImageIcon( ClassLoader.getSystemResource("unlock.png") );
	private ImageIcon aboutIcon = new ImageIcon( ClassLoader.getSystemResource("about.gif") );
	private ImageIcon updateIcon = new ImageIcon( ClassLoader.getSystemResource("update.gif") );
	
	private URL aboutPage = ClassLoader.getSystemResource("about.html");
	private URL versionHistoryTemplate = ClassLoader.getSystemResource("update.html");
	
	private String latestVersionUrl = "https://raw.github.com/ComaToes/ftl-profile-editor/master/latest-version.txt";
	private String versionHistoryUrl = "https://raw.github.com/ComaToes/ftl-profile-editor/master/release-notes.txt";
	private String bugReportUrl = "https://github.com/ComaToes/ftl-profile-editor/issues/new";
	private String forumThreadUrl = "http://www.ftlgame.com/forum/viewtopic.php?f=7&t=2877";
	
	// For checkbox icons
	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;
	private BufferedImage iconShadeImage;
	
	private JPanel topScoresPanel;
	private StatsSubPanel sessionRecordsPanel;
	private StatsSubPanel crewRecordsPanel;
	private StatsSubPanel totalStatsPanel;
	private JLabel statusLbl;
	private final HyperlinkListener linkListener;
	
	private int version;
	
	public FTLFrame(int version) {
		
		this.version = version;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Read config file and locate FTL install
		File propFile = new File("ftl-editor.cfg");
		File ftlPath;
	
		try {
			
			Properties config = new Properties();
			
			if( propFile.exists() ) {
				log.trace("Loading properties from config file");
				config.load( new FileInputStream(propFile) );
			}
			
			String ftlPathString = config.getProperty("ftlPath");
			
			if( ftlPathString != null ) {
				log.trace("Using FTL path from config: " + ftlPathString);
				ftlPath = new File(ftlPathString);
			} else {
				log.trace("No FTL path available");
				ftlPath = promptForFtlPath();
				config.setProperty("ftlPath", ftlPath.getAbsolutePath());
				config.store( new FileOutputStream(propFile) , "FTL Profile Editor - Config File" );
			}
			
		} catch (IOException e) {
			showErrorDialog( "Error loading " + propFile.getPath() + " : " + e.getMessage() );
			throw new RuntimeException("Error loading props", e);
		}
		
		// Initialise data store
		try {
			
			DataManager.init( ftlPath );
			
		} catch (IOException e) {
			showErrorDialog( "Error unpacking FTL data files: " + e.getMessage() );
			throw new RuntimeException(e);
		} catch (JAXBException e) {
			showErrorDialog( "Error unpacking FTL data files: " + e.getMessage() );
			throw new RuntimeException(e);
		}
		
		// Create empty profile
		profile = new Profile();
		profile.setVersion(4);
		boolean[] emptyUnlocks = new boolean[12]; // TODO magic number
		emptyUnlocks[0] = true; // Kestrel starts unlocked
		profile.setShipUnlocks( emptyUnlocks );
		profile.setAchievements( new ArrayList<AchievementRecord>() );
		Stats stats = new Stats();
		stats.setTopScores( new ArrayList<Score>() );
		stats.setShipBest( new ArrayList<Score>() );
		stats.setMostEvasions( new CrewRecord("", "", 0, 0) );
		stats.setMostJumps( new CrewRecord("", "", 0, 0) );
		stats.setMostKills( new CrewRecord("", "", 0, 0) );
		stats.setMostRepairs( new CrewRecord("", "", 0, 0) );
		stats.setMostSkills( new CrewRecord("", "", 0, 0) );
		profile.setStats( stats );
		
		// GUI setup
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 700);
		setLocationRelativeTo(null);
		setTitle("FTL Profile Editor v" + version);
		try {
			setIconImage( ImageIO.read( ClassLoader.getSystemResource("unlock.png") ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		linkListener = new HyperlinkListener() {
		    public void hyperlinkUpdate(HyperlinkEvent e) {
		        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
		        	log.trace("Dialog link clicked: "+ e.getURL());
		        	if(Desktop.isDesktopSupported()) {
		        	    try {
							Desktop.getDesktop().browse(e.getURL().toURI());
							log.trace("Link opened in external browser");
						} catch (Exception ex) {
							log.error("Unable to open link",ex);
						}
		        	}
		        }
		    }
		};
		
		initCheckboxIcons();
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout( new BorderLayout() );
		setContentPane(contentPane);
		
		JTabbedPane tabPane = new JTabbedPane();
		contentPane.add( tabPane );
		
		JToolBar toolbar = new JToolBar();
		contentPane.add(toolbar, BorderLayout.PAGE_START);
		toolbar.setMargin( new Insets(5, 5, 5, 5) );
		setupToolbar(toolbar);
		
		tabPane.add( "Ship Unlocks & Achievements" , new JScrollPane( createUnlocksPanel() ) );
		tabPane.add( "General Achievements" , new JScrollPane( createAchievementsPanel() ) );
		tabPane.add( "Stats" , new JScrollPane( createStatsPanel() ) );
		
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.Y_AXIS) );
		statusPanel.setBorder( BorderFactory.createLoweredBevelBorder() );
		statusLbl = new JLabel(" ");
		//statusLbl.setFont( statusLbl.getFont().deriveFont(Font.PLAIN) );
		statusLbl.setBorder( BorderFactory.createEmptyBorder(2, 4, 2, 4) );
		statusLbl.setAlignmentX( Component.LEFT_ALIGNMENT );
		statusPanel.add( statusLbl );
		contentPane.add( statusPanel, BorderLayout.SOUTH );

		// Load blank profile (sets Kestrel unlock)
		loadProfile(profile);
		
	}
	
	private void showErrorDialog( String message ) {
		
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
		
	}
	
	private File promptForFtlPath() {
		
		String steamPath = "Steam/steamapps/common/FTL Faster Than Light";
		String gogPath = "GOG.com/Faster Than Light";
		File[] paths = new File[] {
					// Windows - Steam
					new File( new File(""+System.getenv("ProgramFiles(x86)")), steamPath ),
					new File( new File(""+System.getenv("ProgramFiles")), steamPath ),
					// Windows - GOG
					new File( new File(""+System.getenv("ProgramFiles(x86)")), gogPath ),
					new File( new File(""+System.getenv("ProgramFiles")), gogPath )
					// TODO add more
				};
		
		File ftlPath = null;
		
		for( File path: paths ) {
			if( path.exists() ) {
				ftlPath = path;
				break;
			}
		}
		
		if( ftlPath == null ) {
			
			log.trace("FTL path not located automatically. Prompting user for location.");
			
			JOptionPane.showMessageDialog(this, "FTL Profile Editor uses images and data from FTL but was unable to locate your FTL installation.\n" +
												"You will now be prompted to locate FTL manually. (You will only need to do this once)", "FTL Not Found", JOptionPane.INFORMATION_MESSAGE);
			
			final JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter( new FileFilter() {
				@Override
				public String getDescription() {
					return "FTL Data File - (FTLInstall)/resources/data.dat";
				}
				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().equals("data.dat");
				}
			});
			fc.setMultiSelectionEnabled(false);
			
			if( fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ) {
				ftlPath = fc.getSelectedFile().getParentFile().getParentFile();
				log.trace("User selected: " + ftlPath.getAbsolutePath());
			} else
				log.trace("User cancelled FTL path selection");

		}
		
		if( ftlPath != null && ftlPath.exists() && ftlPath.isDirectory() && new File(ftlPath,"resources/data.dat").exists() ) {
			log.trace("FTL located at: " + ftlPath.getAbsolutePath());
			return ftlPath;
		} else {
			log.trace("FTL not located");
			showErrorDialog( "FTL data not found. FTL Profile Editor will now exit." );
			System.exit(0);
		}
		
		return null;
		
	}
	
	private JPanel createAchievementsPanel() {
		
		log.trace("Creating Achievements panel");
		JPanel achPanel = new JPanel();
		achPanel.setLayout( new BoxLayout(achPanel, BoxLayout.Y_AXIS) );
		
		List<Achievement> achievements = DataManager.get().getGeneralAchievements();
		
		// TODO magic offsets
		achPanel.add( createAchievementsSubPanel( "General Progression", achievements, 0 ) );
		achPanel.add( createAchievementsSubPanel( "Going the Distance", achievements, 7 ) );
		achPanel.add( createAchievementsSubPanel( "Skill and Equipment Feats", achievements, 14 ) );
		
		return achPanel;
		
	}
	
	private JPanel createAchievementsSubPanel( String title, List<Achievement> achievements, int offset ) {
		
		JPanel panel = new JPanel();
		panel.setBorder( BorderFactory.createTitledBorder(title) );
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		
		// TODO magic number 7
		for (int i = 0; i < 7; i++) {
			Achievement ach = achievements.get(i+offset);
			log.trace("Setting icons for checkbox. Base image: " + "img/" + ach.getImagePath());
			JCheckBox box = new JCheckBox();
			setCheckboxIcons(box, "img/" + ach.getImagePath());
			box.setToolTipText( ach.getName() );

			String achDesc = ach.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(this, achDesc) );

			generalAchievements.put(ach, box);
			panel.add( box );
		}
		
		return panel;
		
	}

	private JPanel createStatsPanel() {
		
		JPanel statsPanel = new JPanel();
		statsPanel.setLayout( new GridLayout(0, 2) );

		topScoresPanel = new JPanel();
		topScoresPanel.setLayout( new BoxLayout(topScoresPanel, BoxLayout.Y_AXIS ) );
		topScoresPanel.setBorder( BorderFactory.createTitledBorder("Top Scores") );
		statsPanel.add( topScoresPanel );

		JPanel statsSubPanelsHolder = new JPanel();
		statsSubPanelsHolder.setLayout( new BoxLayout(statsSubPanelsHolder, BoxLayout.Y_AXIS) );
		statsPanel.add( statsSubPanelsHolder );
		
		sessionRecordsPanel = new StatsSubPanel();
		sessionRecordsPanel.addFillRow();
		sessionRecordsPanel.setBorder( BorderFactory.createTitledBorder("Session Records") );
		statsSubPanelsHolder.add( sessionRecordsPanel );
		
		crewRecordsPanel = new StatsSubPanel();
		crewRecordsPanel.addFillRow();
		crewRecordsPanel.setBorder( BorderFactory.createTitledBorder("Crew Records") );
		statsSubPanelsHolder.add( crewRecordsPanel );

		totalStatsPanel = new StatsSubPanel();
		totalStatsPanel.addFillRow();
		totalStatsPanel.setBorder( BorderFactory.createTitledBorder("Totals") );
		statsSubPanelsHolder.add( totalStatsPanel );

		return statsPanel;
		
	}

	private void initCheckboxIcons() {

		log.trace("Initialising checkbox locked icon");
		iconShadeImage = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = iconShadeImage.getGraphics();
		g.setColor( new Color(0, 0, 0, 150) );
		g.fillRect(0, 0, maxIconWidth, maxIconHeight);
		InputStream stream = null;
		try {
			stream = DataManager.get().getResourceInputStream("img/customizeUI/box_lock_on.png");
			BufferedImage lock = ImageIO.read( stream );
			int x = (maxIconWidth-lock.getWidth()) / 2;
			int y = (maxIconHeight-lock.getHeight()) / 2;
			g.drawImage(lock, x, y, null);
		} catch (IOException e) {
			log.error( "Error reading lock image" , e );
		}	finally {
			try {if (stream != null) stream.close();}
			catch (IOException f) {}
		}
		
	}
	
	private Image getScaledImage( InputStream in ) throws IOException {
		BufferedImage img = ImageIO.read( in );
		int width = img.getWidth();
		int height = img.getHeight();
		
		if( width <= maxIconWidth && height < maxIconHeight )
			return img;
		
		if( width > height ) {
			height /= width/maxIconWidth;
			width = maxIconWidth;
		} else {
			width /= height/maxIconHeight;
			height = maxIconHeight;
		}
		Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		return scaled;
	}

	public ImageIcon getCrewIcon(String race) {
		if (race == null || race.length() == 0) return null;

		ImageIcon result = null;
		int offsetX = 0, offsetY = 0, w = 36, h = 36;
		InputStream in = null;
		try {
			in = DataManager.get().getResourceInputStream("img/people/"+ race +"_player_yellow.png");
			BufferedImage big = ImageIO.read( in );
			if (offsetX+w <= big.getWidth() || offsetY+h <= big.getHeight()) {
				BufferedImage cropped = big.getSubimage(offsetX, offsetY, w, h);

				// Shrink the crop area until non-transparent pixels are hit.
				int lowX = Integer.MAX_VALUE, lowY = Integer.MAX_VALUE;
				int highX = -1, highY = -1;
				for (int testY=0; testY < h; testY++) {
					for (int testX=0; testX < w; testX++) {
						int pixel = cropped.getRGB(testX, testY);
						int alpha = (pixel >> 24) & 0xFF;  // 24:A, 16:R, 8:G, 0:B.
						if (alpha != 0) {
							if (testX > highX) highX = testX;
							if (testY > highY) highY = testY;
							if (testX < lowX) lowX = testX;
							if (testY < lowY) lowY = testY;
						}
					}
				}
				log.trace("Crew Icon Trim Bounds: "+ lowX +","+ lowY +" "+ highX +"x"+ highY +" "+ race);
				if (lowX >= 0 && lowY >= 0 && highX < w && highY < h && lowX < highX && lowY < highY) {
					cropped = cropped.getSubimage(lowX, lowY, highX-lowX+1, highY-lowY+1);
				}
				result = new ImageIcon(cropped);
			}

		} catch (IOException e) {
			log.error( "Failed to load and crop race ("+ race +")", e );

		} finally {
			try {if (in != null) in.close();}
			catch (IOException f) {}
    }
		return result;
	}

	
	private void setCheckboxIcons( JCheckBox box, String baseImagePath ) {
		InputStream stream = null;
		try {
			stream = DataManager.get().getResourceInputStream(baseImagePath);
			Image scaled = getScaledImage(stream);
			int scaledYoffset = (maxIconHeight-scaled.getHeight(null))/2;
			BufferedImage unlocked = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
			unlocked.getGraphics().drawImage(scaled, 0, scaledYoffset, null);
			BufferedImage locked = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
			locked.getGraphics().drawImage(scaled, 0, scaledYoffset, null);
			locked.getGraphics().drawImage(iconShadeImage, 0, 0, null);
			box.setSelectedIcon( new ImageIcon( unlocked ) );
			box.setIcon( new ImageIcon( locked ) );
			
		} catch (IOException e) {
			log.error( "Error reading checkbox image (" + baseImagePath + ")" , e );

		}	finally {
			try {if (stream != null) stream.close();}
			catch (IOException f) {}
		}
		
	}
	
	private JPanel createUnlocksPanel() {
		
		log.trace("Creating unlocks panel");
		JPanel unlockAchPanel = new JPanel();
		unlockAchPanel.setLayout( new BoxLayout(unlockAchPanel, BoxLayout.Y_AXIS) );

		log.trace("Adding ship unlocks");
		JPanel shipPanel = new JPanel();
		unlockAchPanel.add( shipPanel );
		shipPanel.setBorder( BorderFactory.createTitledBorder("Ship Unlocks") );
		shipPanel.setLayout( new GridLayout(0, 3) );

		for( ShipBlueprint ship: DataManager.get().getPlayerShips() ) {
			JCheckBox shipUnlock = new JCheckBox( ship.getShipClass() );
			setCheckboxIcons(shipUnlock, "img/ship/" + ship.getImg() + "_base.png");
			shipPanel.add(shipUnlock);
			shipUnlocks.add(shipUnlock);
		}

		log.trace("Adding ship achievements");
		JPanel shipAchPanel = new JPanel();
		unlockAchPanel.add( shipAchPanel );
		shipAchPanel.setBorder( BorderFactory.createTitledBorder("Ship Achievements") );
		shipAchPanel.setLayout( new GridLayout(0, 3) );

		for( ShipBlueprint ship: DataManager.get().getPlayerShips() )
			shipAchPanel.add( createShipPanel( ship ) );

		return unlockAchPanel;
		
	}
	
	private void setupToolbar(final JToolBar toolbar) {
		
		log.trace("Initialising toolbar");
		
		final JFileChooser fc = new JFileChooser();
		fc.addChoosableFileFilter( new FileFilter() {
			@Override
			public String getDescription() {
				return "FTL Profile (prof.sav)";
			}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().equalsIgnoreCase("prof.sav");
			}
		});
		
		File[] profileLocations = new File[] {
				// Windows XP
				new File( System.getProperty("user.home") + "/My Documents/My Games/FasterThanLight/prof.sav"),
				// Windows Vista/7
				new File( System.getProperty("user.home") + "/Documents/My Games/FasterThanLight/prof.sav"),
				// Mac
				new File( System.getProperty("user.home") + "/Library/Application Support/FasterThanLight/prof.sav"),
				// Linux
				new File( System.getProperty("user.home") + "/.local/share/FasterThanLight/prof.sav")
		};
		
		for( File file: profileLocations )
			if( file.exists() )
				fc.setSelectedFile( file );
		
		fc.setMultiSelectionEnabled(false);
		
		// Set up dialog in case of hash failure
		
		// By default the editor scrolls to the bottom of the text
		// Making the dialog modal removes ability to programatically scroll to top
		final JDialog hashFailDialog = new JDialog(FTLFrame.this, "Profile Parser Error", false);
		JPanel failPanel = new JPanel();
		failPanel.setLayout( new BoxLayout(failPanel, BoxLayout.Y_AXIS) );
		hashFailDialog.setContentPane(failPanel);
		hashFailDialog.setSize(600, 400);
		hashFailDialog.setLocationRelativeTo( FTLFrame.this );
		
		final JEditorPane failEditorPane = new JEditorPane( "text/html", "" );
		failEditorPane.setEditable(false);
		failEditorPane.addHyperlinkListener(linkListener);
		final JScrollPane scrollPane = new JScrollPane(failEditorPane);
		failPanel.add( scrollPane );
		
		// Create open button
		JButton openButton = new JButton("Open", openIcon);
		openButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("Open button clicked");
				if( fc.showOpenDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {

						File f = fc.getSelectedFile();
						
						log.trace("File selected: " + f.getAbsolutePath());
						
						InputStream in = new FileInputStream( f );
						
						// Read whole file so we can hash it
						byte[] data = new byte[(int)f.length()];
						in.read(data);
						in.close();
						
						MessageDigest md = MessageDigest.getInstance("MD5");
						byte[] readHash = md.digest(data);
						
						in = new ByteArrayInputStream(data);
						// Parse file data
						ProfileParser ftl = new ProfileParser();
						profile = ftl.readProfile(in);
						in.close();
						
						FTLFrame.this.loadProfile(profile);
						
						// Perform mock write
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						FTLFrame.this.updateProfile(profile);
						ftl.writeProfile(out, profile);
						out.close();
						
						// Hash result
						byte[] outData = out.toByteArray();
						md.reset();
						byte[] writeHash = md.digest(outData);
						
						// Compare
						for (int i = 0; i < readHash.length; i++) {
							if( readHash[i] != writeHash[i] ) {
								log.error("Hash fail on mock write - Unable to assure valid parsing");
								
								String hex = "";
								for (int j = 0; j < data.length; j++) {
									hex += String.format("%02x", data[j]);
									if( (j+1) % 32 == 0 )
										hex +="\n";
								}
								
								String errText = "<b>FTL Profile Editor has detected that it cannot interpret your profile correctly.<br/>" +
										"Using this app may result in loss of stats/achievements.</b>" +
										"<br/><br/>" +
										"Please copy (Ctrl-A, Ctrl-C) the following text and paste it into a new bug report <a href='"+bugReportUrl+"'>here</a> " +
										"(GitHub signup is free) or post to the FLT forums <a href='"+forumThreadUrl+"'>here</a> (Signup also free)." +
										"<br/>If using GitHub, set the issue title as \"Profile Parser Error\"<br/><br/>I will fix the problem and release a new version as soon as I can :)" +
										"<br/><br/><pre>" + hex + "</pre>";
								
								failEditorPane.setText(errText);
								hashFailDialog.setVisible(true);

								// Try ALL the things to make it scroll to top
								failEditorPane.setCaretPosition(0);
								scrollPane.getVerticalScrollBar().setValue(0);
								scrollPane.getViewport().setViewPosition(new Point());
								scrollPane.scrollRectToVisible(new Rectangle());
								failEditorPane.setSelectionStart(0);
								failEditorPane.setSelectionEnd(0);

								break;
							}
						}
						
						log.trace("Read completed successfully");
						
					} catch( Exception ex ) {
						log.error("Error reading profile",ex);
						showErrorDialog("Error reading profile: " + ex.getMessage());
					}
				} else
					log.trace("Open dialog cancelled");
			}
		});
		openButton.addMouseListener( new StatusbarMouseListener(this, "Open a new profile.") );
		toolbar.add( openButton );
		
		JButton saveButton = new JButton("Save", saveIcon);
		saveButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("Save button clicked");
				if( fc.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						
						File f = fc.getSelectedFile();
						log.trace("File selected: " + f.getAbsolutePath());
						ProfileParser ftl = new ProfileParser();
						OutputStream out = new FileOutputStream( f );
						FTLFrame.this.updateProfile(profile);
						ftl.writeProfile(out, profile);
						out.close();
						
					} catch( IOException ex ) {
						log.error("Error writing profile",ex);
						showErrorDialog("Error saving profile: " + ex.getMessage());
					}
				} else
					log.trace("Save dialog cancelled");
			}
		});
		saveButton.addMouseListener( new StatusbarMouseListener(this, "Save the current profile.") );
		toolbar.add( saveButton );
		

		JButton unlockShipsButton = new JButton("Unlock All Ships", unlockIcon);
		unlockShipsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("Unlock all ships button clicked");
				for( JCheckBox box: shipUnlocks )
					box.setSelected(true);
			}
		});
		unlockShipsButton.addMouseListener( new StatusbarMouseListener(this, "Unlock All Ships.") );
		toolbar.add( unlockShipsButton );

		
		JButton unlockShipAchsButton = new JButton("Unlock All Ship Achievements", unlockIcon);
		unlockShipAchsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("Unlock all ship achievements button clicked");
				for( JCheckBox box: shipAchievements.values() )
					box.setSelected(true);
			}
		});
		unlockShipAchsButton.addMouseListener( new StatusbarMouseListener(this, "Unlock All Ship Achievements.") );
		toolbar.add( unlockShipAchsButton );
		
		final JDialog aboutDialog = new JDialog(this,"About",true);
		JPanel aboutPanel = new JPanel();
		aboutPanel.setLayout( new BoxLayout(aboutPanel, BoxLayout.Y_AXIS) );
		aboutDialog.setContentPane(aboutPanel);
		aboutDialog.setSize(300, 200);
		aboutDialog.setLocationRelativeTo( this );
				
		try {
			JEditorPane editor = new JEditorPane( aboutPage );
			editor.setEditable(false);
			editor.addHyperlinkListener(linkListener);
			aboutPanel.add(editor);
		} catch (IOException e) {
			log.error(e);
		}

		toolbar.add( Box.createHorizontalGlue() );

		JButton extractButton = new JButton("Extract Dats", saveIcon);
		extractButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("Extract button clicked");

				JFileChooser extractChooser = new JFileChooser();
				extractChooser.setDialogTitle("Choose a dir to extract into");
				extractChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				extractChooser.setMultiSelectionEnabled(false);

				if( extractChooser.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						
						File f = extractChooser.getSelectedFile();
						log.trace("Dir selected: " + f.getAbsolutePath());

						JOptionPane.showMessageDialog(FTLFrame.this, "This may take a few seconds.\nClick OK to proceed.", "About to Extract", JOptionPane.PLAIN_MESSAGE);

						DataManager.get().unpackData(f);
						DataManager.get().unpackResources(f);

						JOptionPane.showMessageDialog(FTLFrame.this, "All dat content extracted successfully.", "Extraction Complete", JOptionPane.PLAIN_MESSAGE);
						
					} catch( IOException ex ) {
						log.error("Error extracting dat",ex);
						showErrorDialog("Error extracting dat: " + ex.getMessage());
					}
				} else
					log.trace("Extract dialog cancelled");
			}
		});
		extractButton.addMouseListener( new StatusbarMouseListener(this, "Extract dat content to a directory.") );
		toolbar.add( extractButton );

		toolbar.add( Box.createHorizontalGlue() );
		
		JButton aboutButton = new JButton("About", aboutIcon);
		aboutButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("About button clicked");
				aboutDialog.setVisible(true);
			}
		});
		aboutButton.addMouseListener( new StatusbarMouseListener(this, "Show the about dialog.") );
		toolbar.add( aboutButton );
		
		// Check for new version in seperate thread so we don't hang the UI
		new Thread("CheckVersion") {
			@Override
			public void run() {
				checkForUpdate(toolbar, linkListener);
			}
		}.start();
		
	}
	
	private void checkForUpdate(final JToolBar toolbar, HyperlinkListener linkListener) {
		
		try {
			
			log.trace("Checking for latest version");
			
			URL url = new URL(latestVersionUrl);
			BufferedReader in = new BufferedReader( new InputStreamReader( (InputStream)url.getContent() ) );
			int latestVersion = Integer.parseInt( in.readLine() );
			in.close();
			
			if( latestVersion > version ) {
				log.trace("New version available");
				
				final JDialog updateDialog = new JDialog(this,"Update Available",true);
				JPanel updatePanel = new JPanel();
				updatePanel.setLayout( new BoxLayout(updatePanel, BoxLayout.Y_AXIS) );
				updateDialog.setContentPane(updatePanel);
				updateDialog.setSize(600, 400);
				updateDialog.setLocationRelativeTo( this );
				
				// TODO Template idea nice but a bit messy
				// Read history template
				in = new BufferedReader( new InputStreamReader( (InputStream)versionHistoryTemplate.getContent() ) );
				String historyTemplate = "";
				String line;
				while( (line = in.readLine()) != null )
					historyTemplate += line;
				in.close();
				
				String history = "";
				
				// Read remote history file
				in = new BufferedReader( new InputStreamReader( (InputStream)new URL(versionHistoryUrl).getContent() ) );
				line = in.readLine();
				int version = Integer.parseInt( line );
				String items = "";
				// Render each history file section using the template
				while( version > this.version && line != null ) {
					while( !"".equals(line = in.readLine()) && line != null ) {
						items += "<li>"+line+"</li>";
					}
					history += historyTemplate.replaceAll("\\{version\\}", "v"+version).replaceAll("\\{items\\}", items);
					line = in.readLine();
					if( line != null )
						version = Integer.parseInt( line );
				}
				in.close();

				// Create editor pane and populate with generated history html
				JEditorPane editor = new JEditorPane("text/html", history);
				editor.setEditable(false);
				editor.addHyperlinkListener(linkListener);
				updatePanel.add( new JScrollPane(editor) );

				// Add button to toolbar
				JButton newVersionButton = new JButton("New Version Available!", updateIcon);
				newVersionButton.addActionListener( new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						log.trace("New version button clicked");
						updateDialog.setVisible(true);
					}
				});
				newVersionButton.setBackground( new Color( 0xff, 0xaa, 0xaa ) );
				toolbar.add( newVersionButton );
				
			} else
				log.trace("Already up-to-date");
			
		} catch (IOException e) {
			log.error("Error checking for latest version",e);
			showErrorDialog("Error checking for latest version\n(Use the About window to check the download page manually)\n" + e);
		}
		
	}
	
	private JPanel createShipPanel( ShipBlueprint ship ) {
		
		log.trace("Creating ship panel for: " + ship.getId());
		
		JPanel panel = new JPanel();
		
		panel.setBorder( BorderFactory.createTitledBorder( ship.getShipClass() ) );
		panel.setLayout( new BoxLayout(panel, BoxLayout.X_AXIS) );
		
		for (Achievement ach : DataManager.get().getShipAchievements(ship)) {
			JCheckBox box = new JCheckBox();
			setCheckboxIcons(box, "img/" + ach.getImagePath() );
			box.setToolTipText( ach.getName() );

			String achDesc = ach.getDescription().replaceAll("(\r\n|\r|\n)+", " ");
			box.addMouseListener( new StatusbarMouseListener(this, achDesc) );

			shipAchievements.put(ach, box);
			panel.add( box );
		}
		
		return panel;
		
	}
	
	public void loadProfile( Profile p ) {
		
		log.trace("Loading profile data into UI");
		
		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipUnlocks.size(); i++) {
			shipUnlocks.get(i).setSelected( unlocks[i] );
		}
		
		for( JCheckBox box : shipAchievements.values() )
			box.setSelected(false);
		for( JCheckBox box : generalAchievements.values() )
			box.setSelected(false);

		for( Entry<Achievement, JCheckBox> e: shipAchievements.entrySet() ) {
			String achId = e.getKey().getId();
			JCheckBox box = e.getValue();
			for( AchievementRecord rec: p.getAchievements() )
				if( rec.getAchievementId().equals( achId ) )
					box.setSelected(true);
		}
		
		for( Entry<Achievement, JCheckBox> e: generalAchievements.entrySet() ) {
			String achId = e.getKey().getId();
			JCheckBox box = e.getValue();
			for( AchievementRecord rec: p.getAchievements() )
				if( rec.getAchievementId().equals( achId ) )
					box.setSelected(true);
		}
		
		topScoresPanel.removeAll();
		int i = 0;
		for( Score s : p.getStats().getTopScores() ) {
			InputStream stream = null;
			try {
				ShipBlueprint ship = DataManager.get().getShip( s.getShipType() );
				stream = DataManager.get().getResourceInputStream("img/ship/"+ship.getImg()+"_base.png");
				Image img = getScaledImage( stream );
				TopScorePanel tsp = new TopScorePanel( ++i, img, s.getShipName(), s.getScore(), s.getSector(), s.getDifficulty() );
				topScoresPanel.add( tsp );
			} catch (IOException e) {
				log.error(e);
				showErrorDialog("Error loading profile");
			}	finally {
				try {if (stream != null) stream.close();}
				catch (IOException f) {}
			}
		}

		Stats stats = p.getStats();

		sessionRecordsPanel.removeAll();
		sessionRecordsPanel.addRow("Most Ships Defeated", null, null, stats.getMostShipsDefeated());
		sessionRecordsPanel.addRow("Most Beacons Explored", null, null, stats.getMostBeaconsExplored());
		sessionRecordsPanel.addRow("Most Scrap Collected", null, null, stats.getMostScrapCollected());
		sessionRecordsPanel.addRow("Most Crew Hired", null, null, stats.getMostCrewHired());
		sessionRecordsPanel.addFillRow();

		crewRecordsPanel.removeAll();
		CrewRecord repairCrewRecord = stats.getMostRepairs();
		CrewRecord killsCrewRecord = stats.getMostKills();
		CrewRecord evasionsCrewRecord = stats.getMostEvasions();
		CrewRecord jumpsCrewRecord = stats.getMostJumps();
		CrewRecord skillsCrewRecord = stats.getMostSkills();

		crewRecordsPanel.addRow("Most Repairs", getCrewIcon(repairCrewRecord.getRace()), repairCrewRecord.getName(), repairCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Combat Kills", getCrewIcon(killsCrewRecord.getRace()), killsCrewRecord.getName(), killsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Piloted Evasions", getCrewIcon(evasionsCrewRecord.getRace()), evasionsCrewRecord.getName(), evasionsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Jumps Survived", getCrewIcon(jumpsCrewRecord.getRace()), jumpsCrewRecord.getName(), jumpsCrewRecord.getScore());
		crewRecordsPanel.addRow("Most Skill Masteries", getCrewIcon(skillsCrewRecord.getRace()), skillsCrewRecord.getName(), skillsCrewRecord.getScore());
		crewRecordsPanel.addFillRow();

		totalStatsPanel.removeAll();
		totalStatsPanel.addRow("Total Ships Defeated", null, null, stats.getTotalShipsDefeated());
		totalStatsPanel.addRow("Total Beacons Explored", null, null, stats.getTotalBeaconsExplored());
		totalStatsPanel.addRow("Total Scrap Collected", null, null, stats.getTotalScrapCollected());
		totalStatsPanel.addRow("Total Crew Hired", null, null, stats.getTotalCrewHired());
		totalStatsPanel.addBlankRow();
		totalStatsPanel.addRow("Total Games Played", null, null, stats.getTotalGamesPlayed());
		totalStatsPanel.addRow("Total Victories", null, null, stats.getTotalVictories());
		totalStatsPanel.addFillRow();

		this.repaint();
	}
	
	public void updateProfile( Profile p ) {
		
		log.trace("Updating profile from UI selections");
		
		List<Achievement> allAchs = DataManager.get().getAchievements();
		List<AchievementRecord> achs = new ArrayList<AchievementRecord>();
		List<AchievementRecord> existingAchs = p.getAchievements();
		
		for( Achievement ach: allAchs ) {
			String id = ach.getId();
			JCheckBox box = shipAchievements.get(ach);
			if( box == null )
				box = generalAchievements.get(ach);
			
			if( box != null ) {
				if( box.isSelected() ) {
					
					boolean existing = false;
					for( AchievementRecord rec: existingAchs )
						if( rec.getAchievementId().equals(id) ) {
							achs.add(rec);
							existing = true;
						}
					
					if( !existing )
						achs.add( new AchievementRecord(id, Difficulty.EASY) ); // TODO allow user selection of difficulty
					
				}
			} else {
				// Is an achievement we haven't created a checkbox for
				for( AchievementRecord rec: existingAchs )
					if( rec.getAchievementId().equals(id) )
						achs.add(rec);
			}
		}

		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipUnlocks.size(); i++) {
			unlocks[i] = shipUnlocks.get(i).isSelected();
			// Remove ship achievements for locked ships
			if( !unlocks[i] )
				for( Achievement ach : DataManager.get().getShipAchievements( DataManager.get().getPlayerShips().get(i) ) )
						achs.remove( ach.getId() );
		}
		p.setShipUnlocks(unlocks);
		
		p.setAchievements(achs);
		
		loadProfile(p);
		
	}

	public void setStatusText( String text ) {
		if (text.length() > 0)
			statusLbl.setText(text);
		else
			statusLbl.setText(" ");
	}



	private class StatusbarMouseListener extends MouseAdapter {
		private FTLFrame frame = null;
		private String text = null;

		public StatusbarMouseListener( FTLFrame frame, String text ) {
			this.frame = frame;
			this.text = text;
		}

		public void mouseEntered( MouseEvent e ) {
			frame.setStatusText( text );
		}
		public void mouseExited( MouseEvent e ) {
			frame.setStatusText("");
		}
	}



	private class StatsSubPanel extends JPanel {
		private int COLUMN_COUNT = 0;
		private final int NAME_COL = COLUMN_COUNT++;
		private final int RECIPIENT_COL = COLUMN_COUNT++;
		private final int VALUE_COL = COLUMN_COUNT++;

		GridBagConstraints gridC = null;

		public StatsSubPanel() {
			super(new GridBagLayout());
			removeAll();
		}

		@Override
		public void removeAll() {
			super.removeAll();
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

		public void addRow(String name, ImageIcon icon, String recipient, int value) {
			gridC.gridx = NAME_COL;
			gridC.anchor = GridBagConstraints.WEST;
			gridC.fill = GridBagConstraints.NONE;
			gridC.weightx = 1.0;
			JLabel nameLbl = new JLabel(name);
			this.add(nameLbl, gridC);

			gridC.gridx = RECIPIENT_COL;
			gridC.anchor = GridBagConstraints.CENTER;
			gridC.fill = GridBagConstraints.NONE;
			gridC.weightx = 1.0;
			JLabel recipientLbl = new JLabel();
			recipientLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
			if (recipient != null)
				recipientLbl.setText(recipient);
			if (icon != null)
				recipientLbl.setIcon(icon);
			this.add(recipientLbl, gridC);

			gridC.gridx = VALUE_COL;
			gridC.anchor = GridBagConstraints.CENTER;
			gridC.weightx = 0.0;
			JLabel valueLbl = new JLabel(Integer.toString(value));
			this.add(valueLbl, gridC);

			gridC.gridy++;
		}

		public void addBlankRow() {
			gridC.fill = GridBagConstraints.NONE;
			gridC.weighty = 0.0;
			gridC.gridwidth = GridBagConstraints.REMAINDER;
			gridC.gridx = 0;

			this.add(Box.createVerticalStrut(12), gridC);
			gridC.gridy++;
		}

		public void addFillRow() {
			gridC.fill = GridBagConstraints.VERTICAL;
			gridC.weighty = 1.0;
			gridC.gridwidth = GridBagConstraints.REMAINDER;
			gridC.gridx = 0;

			this.add(Box.createVerticalGlue(), gridC);
			gridC.gridy++;
		}
	}
}
