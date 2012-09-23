package net.blerf.ftl;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;

import net.blerf.ftl.model.Achievement;
import net.blerf.ftl.model.CrewRecord;
import net.blerf.ftl.model.Profile;
import net.blerf.ftl.model.Score;
import net.blerf.ftl.model.Ship;
import net.blerf.ftl.model.Stats;

public class FTLFrame extends JFrame {
	
	private List<JCheckBox> shipUnlocks = new ArrayList<JCheckBox>();
	
	private HashMap<Achievement,JCheckBox> shipAchievements = new HashMap<Achievement, JCheckBox>();
	
	private Profile profile;
	
	private ImageIcon openIcon = new ImageIcon( ClassLoader.getSystemResource("open.gif") );
	private ImageIcon saveIcon = new ImageIcon( ClassLoader.getSystemResource("save.gif") );
	private ImageIcon unlockIcon = new ImageIcon( ClassLoader.getSystemResource("unlock.png") );
	private ImageIcon aboutIcon = new ImageIcon( ClassLoader.getSystemResource("about.gif") );
	private URL aboutPage = ClassLoader.getSystemResource("about.html");
	
	public FTLFrame(int version) {
		
		// Create empty profile
		profile = new Profile();
		profile.setVersion(4);
		boolean[] emptyUnlocks = new boolean[12]; // TODO magic number
		emptyUnlocks[0] = true; // Kestrel starts unlocked
		profile.setShipUnlocks( emptyUnlocks );
		profile.setAchievements( new ArrayList<Achievement>() );
		Stats stats = new Stats();
		stats.setTopScores( new ArrayList<Score>() );
		stats.setShipBest( new ArrayList<Score>() );
		stats.setMostEvasions( new CrewRecord("", "", 0, 0) );
		stats.setMostJumps( new CrewRecord("", "", 0, 0) );
		stats.setMostKills( new CrewRecord("", "", 0, 0) );
		stats.setMostRepairs( new CrewRecord("", "", 0, 0) );
		stats.setMostSkills( new CrewRecord("", "", 0, 0) );
		profile.setStats( stats );

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);
		setTitle("FTL Profile Editor v" + version);
		try {
			setIconImage( ImageIO.read( ClassLoader.getSystemResource("unlock.png") ) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout( new BorderLayout() );
		setContentPane(contentPane);
		
		JTabbedPane tabPane = new JTabbedPane();
		contentPane.add( tabPane );
		
		JToolBar toolbar = new JToolBar();
		contentPane.add(toolbar, BorderLayout.PAGE_START);
		setupToolbar(toolbar);
		
		tabPane.add( "Unlocks" , createUnlocksPanel() );
		tabPane.add( "Stats" , createStatsPanel() );
		
		// Load blank profile (sets Kestrel unlock)
		loadProfile(profile);
		
	}
	
	private JPanel createStatsPanel() {
		
		JPanel statsPanel = new JPanel();
		
		statsPanel.add( new JLabel("Stats display not implemented yet") );
		
		return statsPanel;
		
	}
	
	private JPanel createUnlocksPanel() {
		
		JPanel unlockAchPanel = new JPanel();
		unlockAchPanel.setLayout( new BoxLayout(unlockAchPanel, BoxLayout.Y_AXIS) );
		
		JPanel shipPanel = new JPanel();
		unlockAchPanel.add( shipPanel );
		shipPanel.setBorder( BorderFactory.createTitledBorder("Ship Unlocks") );
		shipPanel.setLayout( new GridLayout(0, 3) );

		for( Ship ship: Ship.ALL ) {
			JCheckBox shipUnlock = new JCheckBox( ship.getName() );
			shipPanel.add(shipUnlock);
			shipUnlocks.add(shipUnlock);
		}

		JPanel shipAchPanel = new JPanel();
		unlockAchPanel.add( shipAchPanel );
		shipAchPanel.setBorder( BorderFactory.createTitledBorder("Ship Achievements") );
		shipAchPanel.setLayout( new GridLayout(0, 3) );

		for( Ship ship: Ship.ALL )
			shipAchPanel.add( createShipPanel( ship ) );

		JPanel generalAchPanel = new JPanel();
		unlockAchPanel.add( generalAchPanel );
		generalAchPanel.setBorder( BorderFactory.createTitledBorder("General Achievements") );
		
		generalAchPanel.add( new JLabel("General Achievements display not implemented yet") );
		
		return unlockAchPanel;
		
	}
	
	private void setupToolbar(JToolBar toolbar) {
		
		
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
		fc.setSelectedFile( new File( System.getProperty("user.home") + "/Documents/My Games/FasterThanLight/prof.sav") ); // TODO make multi-platform
		fc.setMultiSelectionEnabled(false);
		
		JButton openButton = new JButton("Open", openIcon);
		openButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( fc.showOpenDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						
						File f = fc.getSelectedFile();
						FTLParser ftl = new FTLParser();
						InputStream in = new FileInputStream( f );
						profile = ftl.readProfile(in);
						in.close();
						
						FTLFrame.this.loadProfile(profile);
						
					} catch( IOException ex ) {
						ex.printStackTrace();
						// TODO report error
					}
				}
			}
		});
		toolbar.add( openButton );
		
		JButton saveButton = new JButton("Save", saveIcon);
		saveButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if( fc.showSaveDialog(FTLFrame.this) == JFileChooser.APPROVE_OPTION ) {
					try {
						
						File f = fc.getSelectedFile();
						FTLParser ftl = new FTLParser();
						OutputStream out = new FileOutputStream( f );
						FTLFrame.this.updateProfile(profile);
						ftl.writeProfile(out, profile);
						out.close();
						
					} catch( IOException ex ) {
						ex.printStackTrace();
						// TODO report error
					}
				}
			}
		});
		toolbar.add( saveButton );
		

		JButton unlockShipsButton = new JButton("Unlock All Ships", unlockIcon);
		unlockShipsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for( JCheckBox box: shipUnlocks )
					box.setSelected(true);
			}
		});
		toolbar.add( unlockShipsButton );

		
		JButton unlockShipAchsButton = new JButton("Unlock All Ship Achievements", unlockIcon);
		unlockShipAchsButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
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
				
		JEditorPane editor = null;
		try {
			editor = new JEditorPane( aboutPage );
			editor.setEditable(false);
			editor.addHyperlinkListener(new HyperlinkListener() {
			    public void hyperlinkUpdate(HyperlinkEvent e) {
			        if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			        	if(Desktop.isDesktopSupported()) {
			        	    try {
								Desktop.getDesktop().browse(e.getURL().toURI());
							} catch (Exception e1) {
								e1.printStackTrace();
							}
			        	}
			        }
			    }
			});
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		aboutPanel.add(editor);
		
		JButton aboutButton = new JButton("About", aboutIcon);
		aboutButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				aboutDialog.setVisible(true);
			}
		});
		toolbar.add( aboutButton );
		
		
	}
	
	private JPanel createShipPanel( Ship ship ) {
		
		JPanel panel = new JPanel();
		
		panel.setBorder( BorderFactory.createTitledBorder( ship.getName() ) );
		panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS) );
		
		for (Achievement ach : ship.getAchievements()) {
			JCheckBox box = new JCheckBox( ach.getName() );
			shipAchievements.put(ach, box);
			panel.add( box );
		}
		
		return panel;
		
	}
	
	public void loadProfile( Profile p ) {
		
		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipUnlocks.size(); i++) {
			shipUnlocks.get(i).setSelected( unlocks[i] );
		}

		for( Entry<Achievement, JCheckBox> e: shipAchievements.entrySet() ) {
			e.getValue().setSelected( p.getAchievements().contains( e.getKey() ) );
		}
		
	}
	
	public void updateProfile( Profile p ) {
		
		boolean[] unlocks = p.getShipUnlocks();
		for (int i = 0; i < shipUnlocks.size(); i++) {
			unlocks[i] = shipUnlocks.get(i).isSelected();
		}
		p.setShipUnlocks(unlocks);
		
		List<Achievement> achievements = p.getAchievements();
		for( Entry<Achievement, JCheckBox> e: this.shipAchievements.entrySet() ) {
			Achievement a = e.getKey();
			if( e.getValue().isSelected() && !achievements.contains( a ) )
				achievements.add( a );
			else
				achievements.remove( a );
		}
		p.setAchievements(achievements);
		
	}

}
