package net.blerf.ftl.ui;

import javax.swing.filechooser.FileFilter;
import java.io.File;


/**
 * A JFileChooser filter for arbitrary extensions.
 * If any of the suffixes appear at the end of a file, it is shown.
 * Existing directories are always shown.
 */
public class ExtensionFileFilter extends FileFilter {

	private String desc = null;
	private String[] exts = null;


	/**
	 * Constructor.
	 *
	 * @param description the description of this filter
	 * @param suffixes an array of extensions to check, or null for all files
	 */
	public ExtensionFileFilter( String description, String[] suffixes ) {
		desc = description;
		exts = suffixes;
	}

	@Override
	public boolean accept( File file ) {
		if ( file.exists() ) {
			if ( file.isDirectory() ) return true;
			if ( !file.isFile() ) return false;
		}
		if ( exts == null ) return true;

		String filename = file.getName();
		for ( String ext : exts ) {
			if ( filename.endsWith( ext ) ) return true;
		}
		return false;
	}

	/**
	 * Returns the description of this filter.
	 */
	@Override
	public String getDescription() {
		return desc;
	}

	/**
	 * Returns the first suffix this filter checks.
	 */
	public String getPrimarySuffix() {
		if ( exts != null && exts.length > 0 ) return exts[0];
		else return null;
	}
}
