package net.blerf.ftl;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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

		// Ensure all popups are triggered from the event dispatch thread.

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				guiInit();
			}
		});
	}

	private static void guiInit() {
		// Don't use the hard drive to buffer streams during ImageIO.read().
		ImageIO.setUseCache(false);  // Small images don't need extra buffering.

		log.debug( "FTL Editor v"+ VERSION );
		log.debug( System.getProperty("os.name") +" "+ System.getProperty("os.version") +" "+ System.getProperty("os.arch") );
		log.debug( System.getProperty("java.vm.name") +", "+ System.getProperty("java.version") );

		File configFile = new File( "ftl-editor.cfg" );
		File datsDir = null;

		boolean writeConfig = false;
		Properties config = new Properties();
		config.setProperty( "useDefaultUI", "false" );

		// Read the config file.
		InputStream in = null;
		try {
			if ( configFile.exists() ) {
				log.trace( "Loading properties from config file." );
				in = new FileInputStream( configFile );
				config.load( in );
			} else {
				writeConfig = true; // Create a new cfg, but only if necessary.
			}
		}
		catch ( IOException e ) {
			log.error( "Error loading config.", e );
			showErrorDialog( "Error loading config from "+ configFile.getPath() );
		}
		finally {
			try {if ( in != null ) in.close();}
			catch ( IOException e ) {}
		}

		// Look-and-Feel.
		String useDefaultUI = config.getProperty( "useDefaultUI" );

		if ( useDefaultUI == null || !useDefaultUI.equals("true") ) {
			try {
				log.trace( "Using system Look and Feel" );
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e) {
				log.error( "Error setting system Look and Feel.", e );
				log.info( "Setting 'useDefaultUI=true' in the config file will prevent this error." );
			}
		} else {
			log.debug( "Using default Look and Feel." );
		}

		// FTL Resources Path.
		String datsPath = config.getProperty( "ftlDatsPath" );

		if ( datsPath != null && datsPath.length() > 0 ) {
			log.info( "Using FTL dats path from config: "+ datsPath );
			datsDir = new File( datsPath );
			if ( isDatsDirValid( datsDir ) == false ) {
				log.error( "The config's ftlDatsPath does not exist, or it lacks data.dat." );
				datsDir = null;
			}
		} else {
			log.trace( "No FTL dats path previously set." );
		}

		// Find/prompt for the path to set in the config.
		if ( datsDir == null ) {
			datsDir = findDatsDir();
			if ( datsDir != null ) {
				int response = JOptionPane.showConfirmDialog(null, "FTL resources were found in:\n"+ datsDir.getPath() +"\nIs this correct?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if ( response == JOptionPane.NO_OPTION ) datsDir = null;
			}

			if ( datsDir == null ) {
				log.debug( "FTL dats path was not located automatically. Prompting user for location." );
				datsDir = promptForDatsDir();
			}

			if ( datsDir != null ) {
				config.setProperty( "ftlDatsPath", datsDir.getAbsolutePath() );
				writeConfig = true;
				log.info( "FTL dats located at: "+ datsDir.getAbsolutePath() );
			}
		}

		if ( datsDir == null ) {
			showErrorDialog( "FTL data was not found.\nFTL Profile Editor will now exit." );
			log.debug( "No FTL dats path found, exiting." );
			System.exit( 1 );
		}

		if ( writeConfig ) {
			OutputStream out = null;
			try {
				out = new FileOutputStream( configFile );
				String configComments = "FTL Profile Editor - Config File";
				config.store( new OutputStreamWriter( out, "UTF-8" ), configComments );
			}
			catch ( IOException e ) {
				log.error( "Error saving config to "+ configFile.getPath(), e );
				showErrorDialog( "Error saving config to "+ configFile.getPath() );
			}
			finally {
				try {if ( out != null ) out.close();}
				catch ( IOException e ) {}
			}
		}

		try {
			DataManager.init( datsDir ); // Parse the dats.
		}
		catch ( Exception e ) {
			log.error( "Error parsing FTL data files.", e );
			showErrorDialog( "Error parsing FTL data files." );
			System.exit(1);
		}

		try {
			FTLFrame frame = new FTLFrame( VERSION );
			frame.setVisible(true);
		}
		catch ( Exception e ) {
			log.error( "Exception while creating FTLFrame.", e );
			System.exit( 1 );
		}

	}

	private static boolean isDatsDirValid( File d ) {
		if ( !d.exists() || !d.isDirectory() ) return false;
		if ( !new File(d, "data.dat").exists() ) return false;
		if ( !new File(d, "resource.dat").exists() ) return false;
		return true;
	}

	private static File findDatsDir() {
		String steamPath = "Steam/steamapps/common/FTL Faster Than Light/resources";
		String gogPath = "GOG.com/Faster Than Light/resources";

		String xdgDataHome = System.getenv("XDG_DATA_HOME");
		if (xdgDataHome == null)
			xdgDataHome = System.getProperty("user.home") +"/.local/share";

		File[] candidates = new File[] {
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

		File ftlDir = null;

		for ( File candidate : candidates ) {
			if ( isDatsDirValid( candidate ) ) {
				ftlDir = candidate;
				break;
			}
		}

		return ftlDir;
	}

	private static File promptForDatsDir() {
		File result = null;

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
			if ( f.getName().equals("data.dat") ) {
				result = f.getParentFile();
			}
			else if ( f.getName().endsWith(".app") && f.isDirectory() ) {
				File contentsPath = new File(f, "Contents");
				if( contentsPath.exists() && contentsPath.isDirectory() && new File(contentsPath, "Resources").exists() )
					result = new File(contentsPath, "Resources");
			}
		}

		if ( result != null && isDatsDirValid( result ) ) {
			return result;
		}

		return null;
	}

	private static void showErrorDialog( String message ) {
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

}
