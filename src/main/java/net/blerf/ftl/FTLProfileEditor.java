package net.blerf.ftl;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import net.vhati.modmanager.core.FTLUtilities;

import net.blerf.ftl.core.EditorConfig;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.DefaultDataManager;
import net.blerf.ftl.ui.FTLFrame;


public class FTLProfileEditor {

	private static final Logger log = LoggerFactory.getLogger( FTLProfileEditor.class );

	public static final String APP_NAME = "FTL Profile Editor";
	public static final int APP_VERSION = 28;


	public static void main( String[] args ) {
		// Redirect any libraries' java.util.Logging messages.
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		// Doing this here instead of in "logback.xml", allows for conditional log files.
		// For example, the app could decide not to or in a different place.

		// Fork log into a file.
		LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext( lc );
		encoder.setCharset( Charset.forName( "UTF-8" ) );
		encoder.setPattern( "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n" );
		encoder.start();

		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setContext( lc );
		fileAppender.setName( "LogFile" );
		fileAppender.setFile( new File( "./ftl-editor-log.txt" ).getAbsolutePath() );
		fileAppender.setAppend( false );
		fileAppender.setEncoder( encoder );
		fileAppender.start();

		lc.getLogger( Logger.ROOT_LOGGER_NAME ).addAppender( fileAppender );

		// Log a welcome message.
		log.debug( "Started: {}", new Date() );
		log.debug( "{} v{}", APP_NAME, APP_VERSION );
		log.debug( "OS: {} {}", System.getProperty( "os.name" ), System.getProperty( "os.version" ) );
		log.debug( "VM: {}, {}, {}", System.getProperty( "java.vm.name" ), System.getProperty( "java.version" ), System.getProperty( "os.arch" ) );

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException( Thread t, Throwable e ) {
				log.error( "Uncaught exception in thread: "+ t.toString(), e );
			}
		});

		// Ensure all popups are triggered from the event dispatch thread.

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				guiInit();
			}
		});
	}

	private static void guiInit() {
		try {
			// Don't use the hard drive to buffer streams during ImageIO.read().
			ImageIO.setUseCache( false );  // Small images don't need extra buffering.

			File configFile = new File( "ftl-editor.cfg" );

			boolean writeConfig = false;
			Properties props = new Properties();
			props.setProperty( EditorConfig.FTL_DATS_PATH, "" );  // Prompt.
			props.setProperty( EditorConfig.UPDATE_APP, "" );     // Prompt.
			props.setProperty( EditorConfig.USE_DEFAULT_UI, "false" );
			// "app_update_timestamp" doesn't have a default.
			// "app_update_etag" doesn't have a default.
			// "app_update_available" doesn't have a default.

			// Read the config file.
			InputStream in = null;
			try {
				if ( configFile.exists() ) {
					log.trace( "Loading properties from config file." );
					in = new FileInputStream( configFile );
					props.load( new InputStreamReader( in, "UTF-8" ) );
				} else {
					writeConfig = true; // Create a new cfg, but only if necessary.
				}
			}
			catch ( IOException e ) {
				log.error( "Error loading config", e );
				showErrorDialog( "Error loading config from "+ configFile.getPath() );
			}
			finally {
				try {if ( in != null ) in.close();}
				catch ( IOException e ) {}
			}

			EditorConfig appConfig = new EditorConfig( props, configFile );

			// Look-and-Feel.
			boolean useDefaultUI = "true".equals( appConfig.getProperty( EditorConfig.USE_DEFAULT_UI, "false" ) );

			if ( !useDefaultUI ) {
				LookAndFeel defaultLaf = UIManager.getLookAndFeel();
				log.debug( "Default look and feel is: "+ defaultLaf.getName() );

				try {
					log.debug( "Setting system look and feel: "+ UIManager.getSystemLookAndFeelClassName() );

					// SystemLaf is risky. It may throw an exception, or lead to graphical bugs.
					// Problems are geneally caused by custom Windows themes.
					UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
				}
				catch ( Exception e ) {
					log.error( "Failed to set system look and feel", e );
					log.info( "Setting "+ EditorConfig.USE_DEFAULT_UI +"=true in the config file to prevent this error..." );

					appConfig.setProperty( EditorConfig.USE_DEFAULT_UI, "true" );
					writeConfig = true;

					try {
						UIManager.setLookAndFeel( defaultLaf );
					}
					catch ( Exception f ) {
						log.error( "Error returning to the default look and feel after failing to set system look and feel", f );

						// Write an emergency config and exit.
						try {
							appConfig.writeConfig();
						}
						catch ( IOException g ) {
							log.error( String.format( "Error writing config to \"%s\"", configFile.getPath(), g ) );
						}

						throw new ExitException();
					}
				}
			}
			else {
					log.debug( "Using default Look and Feel" );
			}

			// FTL Resources Path.
			File datsDir = null;
			String datsPath = appConfig.getProperty( EditorConfig.FTL_DATS_PATH, "" );

			if ( datsPath.length() > 0 ) {
				log.info( "Using FTL dats path from config: "+ datsPath );
				datsDir = new File( datsPath );
				if ( FTLUtilities.isDatsDirValid( datsDir ) == false ) {
					log.error( "The config's "+ EditorConfig.FTL_DATS_PATH +" does not exist, or it is invalid" );
					datsDir = null;
				}
			}
			else {
				log.debug( "No "+ EditorConfig.FTL_DATS_PATH +" previously set" );
			}

			// Find/prompt for the path to set in the config.
			if ( datsDir == null ) {
				datsDir = FTLUtilities.findDatsDir();
				if ( datsDir != null ) {
					int response = JOptionPane.showConfirmDialog( null, "FTL resources were found in:\n"+ datsDir.getPath() +"\nIs this correct?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
					if ( response == JOptionPane.NO_OPTION ) datsDir = null;
				}

				if ( datsDir == null ) {
					log.debug( "FTL dats path was not located automatically. Prompting user for location" );
					datsDir = FTLUtilities.promptForDatsDir( null );
				}

				if ( datsDir != null ) {
					appConfig.setProperty( EditorConfig.FTL_DATS_PATH, datsDir.getAbsolutePath() );
					writeConfig = true;
					log.info( "FTL dats located at: "+ datsDir.getAbsolutePath() );
				}
			}

			if ( datsDir == null ) {
				showErrorDialog( "FTL resources were not found.\nThe editor will now exit." );
				log.debug( "No FTL dats path found, exiting." );

				throw new ExitException();
			}

			// Prompt if update_catalog is invalid or hasn't been set.
			boolean askAboutUpdates = false;
			if ( !appConfig.getProperty( EditorConfig.UPDATE_APP, "" ).matches( "^\\d+$" ) )
				askAboutUpdates = true;

			if ( askAboutUpdates ) {
				String updatePrompt = "Would you like to periodically check for updates?";

				int response = JOptionPane.showConfirmDialog( null, updatePrompt, "Updates", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
				if ( response == JOptionPane.YES_OPTION ) {
					appConfig.setProperty( EditorConfig.UPDATE_APP, "4" );
				}
				else {
					appConfig.setProperty( EditorConfig.UPDATE_APP, "0" );
				}
				writeConfig = true;
			}

			if ( writeConfig ) {
				OutputStream out = null;
				try {
					appConfig.writeConfig();
				}
				catch ( IOException e ) {
					String errorMsg = String.format( "Error writing config to \"%s\"", configFile.getPath() );
					log.error( errorMsg, e );
					showErrorDialog( errorMsg );
				}
			}

			if ( writeConfig ) {
				String wipMsg = ""
					+ "FTL has revised its file formats several times, and not everything is deciphered \n"
					+ "yet, which limits what can be safely edited.\n"
					+ "\n"
					+ "FTL 1.5.4-1.6.3 profiles are fully editable. (ae_prof.sav)\n"
					+ "FTL 1.01-1.03.3 profiles are fully editable. (prof.sav)\n"
					+ "\n"
					+ "FTL 1.5.4-1.6.3 saved games are partially editable.\n"
					+ "FTL 1.01-1.03.3 saved games are fully editable.\n"
					+ "\n"
					+ "Choose the appropriate tab (Profile or Saved Game) and click the \"Open\" button.\n"
					+ "\n"
					+ "If you encounter a read error opening a file, that means the editor saw something \n"
					+ "new that it doesn't recognize. Submitting a bug report would be helpful.";

				JOptionPane.showMessageDialog( null, wipMsg, "Work in Progress", JOptionPane.PLAIN_MESSAGE );
			}

			// Parse the dats.
			try {
				DefaultDataManager dataManager = new DefaultDataManager( datsDir );
				DataManager.setInstance( dataManager );
				dataManager.setDLCEnabledByDefault( true );
			}
			catch ( Exception e ) {
				log.error( "Error parsing FTL resources", e );
				showErrorDialog( "Error parsing FTL resources" );

				throw new ExitException();
			}

			FTLFrame frame = null;
			try {
				frame = new FTLFrame( appConfig, APP_NAME, APP_VERSION );
				frame.init();
				frame.setVisible( true );
			}
			catch ( Exception e ) {
				log.error( "Failed to create and init the main window", e );

				// If the frame is constructed, but an exception prevents it
				// becoming visible, that *must* be caught. The frame registers
				// itself as a global uncaught exception handler. It doesn't
				// dispose() itself in the handler, so EDT will wait forever
				// for an invisible window to close.

				if ( frame != null && frame.isDisplayable() ) {
					frame.setDisposeNormally( false );
					frame.dispose();
				}

				throw new ExitException();
			}
		}
		catch ( ExitException e ) {
			System.gc();
			// System.exit( 1 );  // Don't do this (InterruptedException). Let EDT end gracefully.
			return;
		}

	}

	private static void showErrorDialog( String message ) {
		JOptionPane.showMessageDialog( null, message, "Error", JOptionPane.ERROR_MESSAGE );
	}



	private static class ExitException extends RuntimeException {
		public ExitException() {
		}

		public ExitException( String message ) {
			super( message );
		}

		public ExitException( Throwable cause ) {
			super( cause );
		}

		public ExitException( String message, Throwable cause ) {
			super( message, cause );
		}
	}
}
