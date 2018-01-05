package net.blerf.ftl.ui;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import net.blerf.ftl.core.EditorConfig;
import net.blerf.ftl.net.TaggedString;
import net.blerf.ftl.net.TaggedStringResponseHandler;
import net.blerf.ftl.ui.FTLFrame;


public class EditorInitThread extends Thread {

	private static final Logger log = LoggerFactory.getLogger( EditorInitThread.class );

	private final String latestVersionUrl = "https://raw.github.com/Vhati/ftl-profile-editor/master/latest-version.txt";
	private final String versionHistoryUrl = "https://raw.github.com/Vhati/ftl-profile-editor/master/release-notes.txt";

	private final FTLFrame frame;
	private final EditorConfig appConfig;
	private final int appVersion;


	public EditorInitThread( FTLFrame frame, EditorConfig appConfig, int appVersion ) {
		super( "init" );
		this.frame = frame;
		this.appConfig = appConfig;
		this.appVersion = appVersion;
	}

	@Override
	public void run() {
		try {
			init();
		}
		catch ( Exception e ) {
			log.error( "Error during EditorFrame init.", e );
		}
	}

	private void init() {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectionRequestTimeout( 5000 )
			.setConnectTimeout( 5000 )
			.setSocketTimeout( 10000 )
			.setRedirectsEnabled( true )
			.build();

		CloseableHttpClient httpClient = HttpClientBuilder.create()
			.setDefaultRequestConfig( requestConfig )
			.disableAuthCaching()
			.disableAutomaticRetries()
			.disableConnectionState()
			.disableCookieManagement()
			//.setUserAgent( "" )
			.build();

		String eTagCached = null;  // TODO.

		HttpGet request = null;
		try {
			TaggedStringResponseHandler responseHandler = new TaggedStringResponseHandler();

			log.debug( "Checking for latest version" );
			request = new HttpGet( latestVersionUrl );

			if ( eTagCached != null ) request.addHeader( "If-None-Match", eTagCached );

			TaggedString latestResult = httpClient.execute( request, responseHandler );
			// TODO: Remember latestResult.etag.

			// When an ETag is known and the file hasn't changed, latestResult will be null.

			if ( latestResult != null ) {
				int latestVersion = Integer.parseInt( latestResult.text.trim() );

				if ( latestVersion > appVersion ) {
					request = new HttpGet( versionHistoryUrl );
					TaggedString historyResult = httpClient.execute( request, responseHandler );

					final Map<Integer, List<String>> historyMap = parseVersionHistory( historyResult.text );

					// Make changes from the GUI thread.
					Runnable r = new Runnable() {
						@Override
						public void run() {
							frame.setVersionHistory( historyMap );
						}
					};
					SwingUtilities.invokeLater( r );
				}
			}
		}
		catch( ClientProtocolException e ) {
			log.error( "GET request failed for url: "+ request.getURI().toString(), e );
		}
		catch ( Exception e ) {
			log.error( "Checking for latest version failed", e );
		}
		finally {
			try{httpClient.close();}
			catch ( IOException e ) {}
		}
	}



	/**
	 * Parses history text to a Map of release versions with itemized changes.
	 */
	private Map<Integer, List<String>> parseVersionHistory( String historyText ) {
		Map<Integer, List<String>> historyMap = new LinkedHashMap<Integer, List<String>>();

		Scanner historyScanner = new Scanner( historyText );
		while ( historyScanner.hasNextLine() ) {
			int releaseVersion = Integer.parseInt( historyScanner.nextLine() );
			List<String> releaseChangeList = new ArrayList<String>();
			historyMap.put( releaseVersion, releaseChangeList );

			while ( historyScanner.hasNextLine() ) {
				String line = historyScanner.nextLine();
				if ( line.isEmpty() ) break;

				releaseChangeList.add( line );
			}

			// Must've either hit a blank or done.
		}
		historyScanner.close();

		return historyMap;
	}
}
