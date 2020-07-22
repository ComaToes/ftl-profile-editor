package net.blerf.ftl.net;


/**
 * A container for an HTTP(S) response text, with an ETag.
 */
public class TaggedString {
	public final String text;
	public final String etag;

	public TaggedString( String text, String etag ) {
		this.text = text;
		this.etag = etag;
	}
}
