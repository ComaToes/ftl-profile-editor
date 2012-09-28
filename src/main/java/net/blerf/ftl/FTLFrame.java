package net.blerf.ftl;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
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
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.xml.bind.JAXBException;

import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.model.Score;
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
	private String downloadUrl = "https://github.com/ComaToes/ftl-profile-editor/downloads";
	private String forumUrl = "http://www.ftlgame.com/forum/viewtopic.php?f=7&t=2877";
	
	// For checkbox icons
	private static final int maxIconWidth = 64;
	private static final int maxIconHeight = 64;
	private BufferedImage iconShadeImage;
	
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
			
			DataManager.init( ftlPath, new File("ftldata") );
			
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
		profile.setAchievements( new ArrayList<String>() );
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
		
		tabPane.add( "Unlocks" , new JScrollPane( createUnlocksPanel() ) );
		tabPane.add( "Stats" , createStatsPanel() );
		
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

	private JPanel createStatsPanel() {
		
		JPanel statsPanel = new JPanel();
		
		statsPanel.add( new JLabel("Stats display not implemented yet") );
		
		return statsPanel;
		
	}

	private void initCheckboxIcons() {

		log.trace("Initialising checkbox locked icon");
		iconShadeImage = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics g = iconShadeImage.getGraphics();
		g.setColor( new Color(0, 0, 0, 150) );
		g.fillRect(0, 0, maxIconWidth, maxIconHeight);
		try {
			BufferedImage lock = ImageIO.read( new File(DataManager.get().getDataFolder(), "img/customizeUI/box_lock_on.png") );
			int x = (maxIconWidth-lock.getWidth()) / 2;
			int y = (maxIconHeight-lock.getHeight()) / 2;
			g.drawImage(lock, x, y, null);
		} catch (IOException e) {
			log.error( "Error reading lock image" , e );
		}
		
	}
	
	private void setCheckboxIcons( JCheckBox box, File baseImage ) {
		
		log.trace("Setting icons for checkbox. Base image: " + baseImage.getPath());
		try {
			BufferedImage img = ImageIO.read( baseImage );
			
			int scaledHeight = img.getHeight()/(img.getWidth()/maxIconWidth);
			int scaledYoffset = (maxIconHeight-scaledHeight)/2;
			
			Image scaled = img.getScaledInstance(maxIconWidth, scaledHeight, Image.SCALE_SMOOTH);
			BufferedImage unlocked = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
			unlocked.getGraphics().drawImage(scaled, 0, scaledYoffset, null);
			BufferedImage locked = new BufferedImage(maxIconWidth, maxIconHeight, BufferedImage.TYPE_INT_ARGB);
			locked.getGraphics().drawImage(scaled, 0, scaledYoffset, null);
			locked.getGraphics().drawImage(iconShadeImage, 0, 0, null);
			box.setSelectedIcon( new ImageIcon( unlocked ) );
			box.setIcon( new ImageIcon( locked ) );
			
		} catch (IOException e) {
			log.error( "Error reading checkbox image" , e );
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
			setCheckboxIcons(shipUnlock, new File(DataManager.get().getDataFolder(), "img/ship/" + ship.getImg() + "_base.png") );
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
				return f.getName().equalsIgnoreCase("prof.sav");
			}
		});
		
		File[] profileLocations = new File[] {
				// Windows
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
								JOptionPane.showInputDialog( FTLFrame.this, "FTL Profile Editor has detected that it cannot interpret your profile correctly.\nUsing this app may result in loss of stats/achievements.\nPlease attach a copy of your original prof.sav to this forum thread so I can fix this:", "Parser Error Detected", JOptionPane.ERROR_MESSAGE, null, null, forumUrl );
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
		toolbar.add( unlockShipAchsButton );
		
		final JDialog aboutDialog = new JDialog(this,"About",true);
		JPanel aboutPanel = new JPanel();
		aboutPanel.setLayout( new BoxLayout(aboutPanel, BoxLayout.Y_AXIS) );
		aboutDialog.setContentPane(aboutPanel);
		aboutDialog.setSize(300, 200);
		aboutDialog.setLocationRelativeTo( this );
				
		final HyperlinkListener linkListener = new HyperlinkListener() {
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
		
		try {
			JEditorPane editor = new JEditorPane( aboutPage );
			editor.setEditable(false);
			editor.addHyperlinkListener(linkListener);
			aboutPanel.add(editor);
		} catch (IOException e) {
			log.error(e);
		}
		
		JButton aboutButton = new JButton("About", aboutIcon);
		aboutButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				log.trace("About button clicked");
				aboutDialog.setVisible(true);
			}
		});
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
			setCheckboxIcons(box, new File( DataManager.get().getDataFolder() , "img/" + ach.getImagePath() ) );
			box.setToolTipText( ach.getName() );
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

		for( Entry<Achievement, JCheckBox> e: shipAchievements.entrySet() ) {
			e.getValue().setSelected( p.getAchievements().contains( e.getKey().getId() ) );
		}
		
	}
	
	public void updateProfile( Profile p ) {
		
		log.trace("Updating profile from UI selections");
		
		List<Achievement> allAchs = DataManager.get().getAchievements();
		List<String> achs = new ArrayList<String>();
		List<String> existingAchs = p.getAchievements();
		
		for( Achievement ach: allAchs ) {
			String id = ach.getId();
			JCheckBox box = shipAchievements.get(ach);
			if( box != null ) {
				if( box.isSelected() )
					achs.add(id);
			} else if( existingAchs.contains(id) )
					achs.add(id);
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

}
