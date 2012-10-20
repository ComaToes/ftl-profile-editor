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
	
	private static final int VERSION = 10;

	public static void main(String[] args) {
		
		// Read config file and locate FTL install
		File propFile = new File("ftl-editor.cfg");
		File ftlPath;
	
		InputStream in = null;
		OutputStream out = null;
		try {
			
			Properties config = new Properties();
			
			if ( propFile.exists() ) {
				log.trace( "Loading properties from config file" );
				in = new FileInputStream(propFile);
				config.load( in );
			}

			// FTL path
			String ftlPathString = config.getProperty("ftlPath");
			
			if ( ftlPathString != null ) {
				log.trace( "Using FTL path from config: " + ftlPathString );
				ftlPath = new File(ftlPathString);
			} else {
				log.trace( "No FTL path available" );
				ftlPath = promptForFtlPath();
				if ( ftlPath == null )
					return; // User cancelled, exit
				config.setProperty( "ftlPath", ftlPath.getAbsolutePath() );
				out = new FileOutputStream(propFile);
				config.store( out, "FTL Profile Editor - Config File" );
			}
			
			// LnF
			String useDefaultUI = config.getProperty("useDefaultUI");
			
			if ( useDefaultUI == null || !useDefaultUI.equals("true") ) {
				try {
					log.trace( "Using system Look and Feel" );
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					log.error( "Error setting system Look and Feel", e );
				}
			} else {
				log.trace( "Using default Look and Feel" );
			}
			
		} catch (IOException e) {
			showErrorDialog( "Error loading config from " + propFile.getPath() );
			log.error( "Error loading props", e );
			return;
			
		} finally {
			if ( in != null ) { try { in.close(); } catch (IOException e) {} }
			if ( out != null ) { try { out.close(); } catch (IOException e) {} }
		}
		
		// Initialise data store
		try {
			
			DataManager.init( ftlPath );
			
		} catch (Exception e) {
			showErrorDialog( "Error parsing FTL data files" );
			log.error( "Error parsing FTL data files", e );
		}

		try {
			FTLFrame frame = new FTLFrame(VERSION);
			frame.setVisible(true);

		} catch (Exception e) {
			log.error( "Exception while creating FTLFrame", e );
			// Required to kill Swing or process will remain active
			System.exit(0);
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
			log.trace( "FTL located at: " + ftlPath.getAbsolutePath() );
			return ftlPath;
		} else {
			log.trace( "FTL not located" );
			showErrorDialog( "FTL data not found. FTL Profile Editor will now exit." );
			System.exit(0);
		}
		
		return null;
		
	}
	
	private static void showErrorDialog( String message ) {
		
		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
		
	}
	
}
