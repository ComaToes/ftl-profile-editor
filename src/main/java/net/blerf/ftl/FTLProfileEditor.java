package net.blerf.ftl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.ui.FTLFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FTLProfileEditor {

	private static final Logger log = LogManager.getLogger(FTLProfileEditor.class);
	
	private static final int VERSION = 11;

	public static void main(String[] args) {
		
		// Read config file and locate FTL install
		File propFile = new File("ftl-editor.cfg");
		File ftlPath = null;

		boolean writeConfig = false;
		Properties config = new Properties();
		config.setProperty( "useDefaultUI", "false" );
	
		InputStream in = null;
		try {
			if ( propFile.exists() ) {
				log.trace( "Loading properties from config file" );
				in = new FileInputStream(propFile);
				config.load( in );
			} else {
				writeConfig = true;  // Create a new cfg, but only if necessary.
			}
		} catch (IOException e) {
			log.error( "Error loading config", e );
			showErrorDialog( "Error loading config from " + propFile.getPath() );
		} finally {
			if ( in != null ) { try { in.close(); } catch (IOException e) {} }
		}

		// LnF
		String useDefaultUI = config.getProperty("useDefaultUI");

		if ( useDefaultUI == null || !useDefaultUI.equals("true") ) {
			try {
				log.trace( "Using system Look and Feel" );
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				log.error( "Error setting system Look and Feel", e );
				log.info( "Setting 'useDefaultUI=true' in the config file will prevent this error." );
			}
		} else {
			log.debug( "Using default Look and Feel" );
		}

		// FTL path
		String ftlPathString = config.getProperty("ftlPath");
			
		if ( ftlPathString != null ) {
			log.info( "Using FTL path from config: " + ftlPathString );
			ftlPath = new File(ftlPathString);
			if ( !ftlPath.exists() ) {
				log.error( "The config's FTL path does not exist" );
				ftlPath = null;
			}
		} else {
			log.trace( "No FTL path available" );
		}
		if ( ftlPath == null ) {
			ftlPath = promptForFtlPath();
			if ( ftlPath == null )
				System.exit(1); // User cancelled, exit
			config.setProperty( "ftlPath", ftlPath.getAbsolutePath() );
			writeConfig = true;
		}

		OutputStream out = null;
		if ( writeConfig ) {
			try {
				out = new FileOutputStream(propFile);
				config.store( out, "FTL Profile Editor - Config File" );

			} catch (IOException e) {
				log.error( "Error saving config to " + propFile.getPath(), e );
				showErrorDialog( "Error saving config to " + propFile.getPath() );

			} finally {
				if ( out != null ) { try { out.close(); } catch (IOException e) {} }
			}
		}
		
		// Initialise data store
		try {
			
			DataManager.init( ftlPath );
			
		} catch (Exception e) {
			log.error( "Error parsing FTL data files", e );
			showErrorDialog( "Error parsing FTL data files" );
			System.exit(1);
		}

		try {
			FTLFrame frame = new FTLFrame(VERSION);
			frame.setVisible(true);

		} catch (Exception e) {
			log.error( "Exception while creating FTLFrame", e );
			// Required to kill Swing or process will remain active
			System.exit(1);
		}
		
	}
	
	private static File promptForFtlPath() {
		
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
		
		for ( File path: paths ) {
			if ( path.exists() ) {
				ftlPath = path;
				break;
			}
		}
		
		if ( ftlPath == null ) {
			
			log.trace("FTL path not located automatically. Prompting user for location.");
			
			JOptionPane.showMessageDialog(null, "FTL Profile Editor uses images and data from FTL but was unable to locate your FTL installation.\n" +
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
			
			// TODO Store full data file path - Mac install is packaged and does not have the resources dir
			// (Will need to add something to convert existing config files)
			if ( fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ) {
				ftlPath = fc.getSelectedFile().getParentFile().getParentFile();
				log.trace( "User selected: " + ftlPath.getAbsolutePath() );
			} else
				log.trace( "User cancelled FTL path selection" );

		}
		
		if ( ftlPath != null && ftlPath.exists() && ftlPath.isDirectory() && new File(ftlPath,"resources/data.dat").exists() ) {
			log.info( "FTL located at: " + ftlPath.getAbsolutePath() );
			return ftlPath;
		} else {
			log.error( "FTL was not found" );
			showErrorDialog( "FTL data was not found.\nFTL Profile Editor will now exit." );
			System.exit(1);
		}
		
		return null;
		
	}
	
	private static void showErrorDialog( String message ) {
		
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
		
	}
	
}
