package net.blerf.ftl.ui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.TransferHandler;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;


/**
 * A basic handler for HTML content.
 *
 * The clipboard will be filled with both HTML and plain text that was
 * extracted from among the tags (BR tags are treated as line breaks).
 */
class HTMLEditorTransferHandler extends TransferHandler {

	public HTMLEditorTransferHandler() {
	}


	protected Transferable createTransferable( JComponent c ) {
		final JEditorPane pane = (JEditorPane)c;
		final String htmlText = pane.getText();
		final String plainText = extractText( new StringReader( htmlText ) );
		return new HTMLTransferable( plainText, htmlText );
	}


	public String extractText( Reader reader ) {
		final List<String> fragmentList = new ArrayList<String>();

		HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
			public void handleText( final char[] data, final int pos ) {
				fragmentList.add( new String( data ) );
			}

			public void handleStartTag( HTML.Tag tag, MutableAttributeSet attribute, int pos ) {
			}

			public void handleEndTag( HTML.Tag t, final int pos ) {
			}

			public void handleSimpleTag( HTML.Tag t, MutableAttributeSet a, final int pos ) {
				if ( t.equals( HTML.Tag.BR ) ) {
					fragmentList.add( "\n" );
				}
			}

			public void handleComment( final char[] data, final int pos ) {
			}

			public void handleError( final String errMsg, final int pos ) {
			}
		};
		try {
			new ParserDelegator().parse( reader, parserCallback, true );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}

		StringBuilder result = new StringBuilder();
		for ( String s : fragmentList ) {
			result.append( s );
		}

		return result.toString();
	}


	@Override
	public void exportToClipboard( JComponent comp, Clipboard clip, int action ) throws IllegalStateException {
		if ( action == TransferHandler.COPY ) {
			clip.setContents( this.createTransferable( comp ), null );
		}
	}

	@Override
	public int getSourceActions( JComponent c ) {
		return TransferHandler.COPY;
	}



	public static class HTMLTransferable implements Transferable {

		private static final DataFlavor[] supportedFlavors;

		static {
			try {
				supportedFlavors = new DataFlavor[]{
						new DataFlavor( "text/html;class=java.lang.String" ),
						new DataFlavor( "text/plain;class=java.lang.String" )
				};
			}
			catch ( ClassNotFoundException e ) {
				throw new ExceptionInInitializerError( e );
			}
		}

		private final String plainData;
		private final String htmlData;

		public HTMLTransferable( String plainData, String htmlData ) {
			this.plainData = plainData;
			this.htmlData = htmlData;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return supportedFlavors;
		}

		public boolean isDataFlavorSupported( DataFlavor flavor ) {
			for ( DataFlavor supportedFlavor : supportedFlavors ) {
				if ( supportedFlavor == flavor ) {
					return true;
				}
			}
			return false;
		}

		public Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException, IOException {
			if ( flavor.equals( supportedFlavors[0] ) ) {
				return htmlData;
			}
			if ( flavor.equals( supportedFlavors[1] ) ) {
				return plainData;
			}
			throw new UnsupportedFlavorException( flavor );
		}
	}
}