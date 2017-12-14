package net.blerf.ftl.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class EditorConfig {

	public static final String FTL_DATS_PATH = "ftl_dats_path";
	public static final String USE_DEFAULT_UI = "use_default_ui";

	private Properties config;
	private File configFile;


	public EditorConfig( Properties config, File configFile ) {
		this.config = config;
		this.configFile = configFile;
	}

	/**
	 * Copy constructor.
	 */
	public EditorConfig( EditorConfig srcConfig ) {
		this.configFile = srcConfig.getConfigFile();
		this.config = new Properties();
		this.config.putAll( srcConfig.getConfig() );
	}


	public Properties getConfig() { return config; }

	public File getConfigFile() { return configFile; }


	public Object setProperty( String key, String value ) {
		return config.setProperty( key, value );
	}

	public int getPropertyAsInt( String key, int defaultValue ) {
		String s = config.getProperty( key );
		if ( s != null && s.matches("^\\d+$") )
			return Integer.parseInt( s );
		else
			return defaultValue;
	}

	public String getProperty( String key, String defaultValue ) {
		return config.getProperty( key, defaultValue );
	}

	public String getProperty( String key ) {
		return config.getProperty( key );
	}


	public void writeConfig() throws IOException {

		OutputStream out = null;
		try {
			out = new FileOutputStream( configFile );

			Map<String, String> userFieldsMap = new LinkedHashMap<String, String>();
			Map<String, String> appFieldsMap = new LinkedHashMap<String, String>();

			userFieldsMap.put( FTL_DATS_PATH,     "The path to FTL's resources folder. If invalid, you'll be prompted." );
			userFieldsMap.put( USE_DEFAULT_UI,    "If true, no attempt will be made to resemble a native GUI. Default: false." );

			List<String> allFieldsList = new ArrayList<String>( userFieldsMap.size() + appFieldsMap.size() );
			allFieldsList.addAll( userFieldsMap.keySet() );
			allFieldsList.addAll( userFieldsMap.keySet() );
			int fieldWidth = 0;
			for ( String fieldName : allFieldsList ) {
				fieldWidth = Math.max( fieldName.length(), fieldWidth );
			}

			StringBuilder commentsBuf = new StringBuilder( "\n" );
			for ( Map.Entry<String, String> entry : userFieldsMap.entrySet() ) {
				commentsBuf.append( String.format( " %-"+ fieldWidth +"s - %s\n", entry.getKey(), entry.getValue() ) );
			}
			commentsBuf.append( "\n" );
			for ( Map.Entry<String, String> entry : appFieldsMap.entrySet() ) {
				commentsBuf.append( String.format( " %-"+ fieldWidth +"s - %s\n", entry.getKey(), entry.getValue() ) );
			}

			OutputStreamWriter writer = new OutputStreamWriter( out, "UTF-8" );
			config.store( writer, commentsBuf.toString() );
			writer.flush();
		}
		finally {
			try {if ( out != null ) out.close();}
			catch ( IOException e ) {}
		}
	}
}
