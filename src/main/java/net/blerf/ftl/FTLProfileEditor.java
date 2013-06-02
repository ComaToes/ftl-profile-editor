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

	private static final int VERSION = 16;

	public static void main( String[] args ) {
		log.debug( "FTL Editor v"+ VERSION );
		log.debug( System.getProperty("os.name") +" "+ System.getProperty("os.version") +" "+ System.getProperty("os.arch") );
		log.debug( System.getProperty("java.vm.name") +", "+ System.getProperty("java.version") );

		File propFile = new File("ftl-editor.cfg");
		File datsPath = null;

		boolean writeConfig = false;
		Properties config = new Properties();
		config.setProperty( "useDefaultUI", "false" );

		// Read the config file.
		InputStream in = null;
		try {
			if ( propFile.exists() ) {
				log.trace( "Loading properties from config file." );
				in = new FileInputStream(propFile);
				config.load( in );
			} else {
				writeConfig = true; // Create a new cfg, but only if necessary.
			}
		} catch (IOException e) {
			log.error( "Error loading config.", e );
			showErrorDialog( "Error loading config from " + propFile.getPath() );
		} finally {
			if ( in != null ) { try { in.close(); } catch (IOException e) {} }
		}

		// Look-and-Feel.
		String useDefaultUI = config.getProperty("useDefaultUI");

		if ( useDefaultUI == null || !useDefaultUI.equals("true") ) {
			try {
				log.trace( "Using system Look and Feel" );
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				log.error( "Error setting system Look and Feel.", e );
				log.info( "Setting 'useDefaultUI=true' in the config file will prevent this error." );
			}
		} else {
			log.debug( "Using default Look and Feel." );
		}

		// FTL Resources Path.
		String datsPathString = config.getProperty("ftlDatsPath");

		if ( datsPathString != null ) {
			log.info( "Using FTL dats path from config: " + datsPathString );
			datsPath = new File(datsPathString);
			if ( isDatsPathValid(datsPath) == false ) {
				log.error( "The config's ftlDatsPath does not exist, or it lacks data.dat." );
				datsPath = null;
			}
		} else {
			log.trace( "No FTL dats path previously set." );
		}

		// Find/prompt for the path to set in the config.
		if ( datsPath == null ) {
			datsPath = findFtlPath();
			if ( datsPath != null ) {
				int response = JOptionPane.showConfirmDialog(null, "FTL resources were found in:\n"+ datsPath.getPath() +"\nIs this correct?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if ( response == JOptionPane.NO_OPTION ) datsPath = null;
			}

			if ( datsPath == null ) {
				log.debug("FTL dats path was not located automatically. Prompting user for location.");
				datsPath = promptForFtlPath();
			}

			if ( datsPath != null ) {
				config.setProperty( "ftlDatsPath", datsPath.getAbsolutePath() );
				writeConfig = true;
				log.info( "FTL dats located at: " + datsPath.getAbsolutePath() );
			}
		}

		if ( datsPath == null ) {
			showErrorDialog( "FTL data was not found.\nFTL Profile Editor will now exit." );
			log.debug( "No FTL dats path found, exiting." );
			System.exit(1);
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

		try {
			DataManager.init( datsPath ); // Parse the dats.
		}
		catch (Exception e) {
			log.error( "Error parsing FTL data files.", e );
			showErrorDialog( "Error parsing FTL data files." );
			System.exit(1);
		}

		try {
			FTLFrame frame = new FTLFrame(VERSION);
			frame.setVisible(true);
		}
		catch (Exception e) {
			log.error( "Exception while creating FTLFrame.", e );
			// Kill Swing or the process will remain active.
			System.exit(1);
		}

	}

	private static boolean isDatsPathValid(File path) {
		return (path.exists() && path.isDirectory() && new File(path,"data.dat").exists());
	}

	private static File findFtlPath() {
		String steamPath = "Steam/steamapps/common/FTL Faster Than Light/resources";
		String gogPath = "GOG.com/Faster Than Light/resources";

		String xdgDataHome = System.getenv("XDG_DATA_HOME");
		if (xdgDataHome == null)
			xdgDataHome = System.getProperty("user.home") +"/.local/share";

		File[] paths = new File[] {
			// Windows - Steam
			new File( new File(""+System.getenv("ProgramFiles(x86)")), steamPath ),
			new File( new File(""+System.getenv("ProgramFiles")), steamPath ),
			// Windows - GOG
			new File( new File(""+System.getenv("ProgramFiles(x86)")), gogPath ),
			new File( new File(""+System.getenv("ProgramFiles")), gogPath ),
			// Linux - Steam
			new File( xdgDataHome +"/Steam/SteamApps/common/FTL Faster Than Light/data/resources" ),
			// OSX - Steam
			new File( System.getProperty("user.home") +"/Library/Application Support/Steam/SteamApps/common/FTL Faster Than Light/FTL.app/Contents/Resources" ),
			// OSX
			new File( "/Applications/FTL.app/Contents/Resources" )
		};

		File ftlPath = null;

		for ( File path: paths ) {
			if ( isDatsPathValid(path) ) {
				ftlPath = path;
				break;
			}
		}

		return ftlPath;
	}

	private static File promptForFtlPath() {
		File ftlPath = null;

		String message = "FTL Profile Editor uses images and data from FTL,\n";
		message += "but the path to FTL's resources could not be guessed.\n\n";
		message += "You will now be prompted to locate FTL manually.\n";
		message += "Select '(FTL dir)/resources/data.dat'.\n";
		message += "Or 'FTL.app', if you're on OSX.";
		JOptionPane.showMessageDialog(null,  message, "FTL Not Found", JOptionPane.INFORMATION_MESSAGE);

		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle( "Find data.dat or FTL.app" );
		fc.addChoosableFileFilter( new FileFilter() {
			@Override
			public String getDescription() {
				return "FTL Data File - (FTL dir)/resources/data.dat";
			}
			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().equals("data.dat") || f.getName().equals("FTL.app");
			}
		});
		fc.setMultiSelectionEnabled(false);

		if ( fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ) {
			File f = fc.getSelectedFile();
			if ( f.getName().equals("data.dat") )
				ftlPath = f.getParentFile();
			else if ( f.getName().endsWith(".app") && f.isDirectory() ) {
				File contentsPath = new File(f, "Contents");
				if( contentsPath.exists() && contentsPath.isDirectory() && new File(contentsPath, "Resources").exists() )
					ftlPath = new File(contentsPath, "Resources");
			}
			log.trace( "User selected: " + ftlPath.getAbsolutePath() );
		} else {
			log.trace( "User cancelled FTL dats path selection." );
		}

		if ( ftlPath != null && isDatsPathValid(ftlPath) ) {
			return ftlPath;
		}

		return null;
	}

	private static void showErrorDialog( String message ) {
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

}
