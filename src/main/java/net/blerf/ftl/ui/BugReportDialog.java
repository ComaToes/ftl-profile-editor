package net.blerf.ftl.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.io.InputStream;
import java.io.IOException;
import javax.swing.JEditorPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * A generic dialog for bug reporting.
 *
 * Construct, provide content with methods, call build(), then show the dialog.
 *
 * Recommended: Call getMessageEditor().addHyperlinkListener(...).
 */
public class BugReportDialog extends JDialog {

	protected Font reportFont = new Font( Font.MONOSPACED, Font.PLAIN, 13 );

	protected String htmlMessage = null;
	protected String htmlInstructions = null;
	protected String reportTitle = null;
	protected String appDescription = null;
	protected Throwable exception = null;
	protected CharSequence attachmentContent = null;
	protected String attachmentName = null;

	protected JEditorPane messageEditor;
	protected JTextArea reportArea;


	public BugReportDialog( Frame owner ) {
		super( owner, "Bug Report", true );
		this.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );

		JPanel panel = new JPanel( new BorderLayout() );

		messageEditor = new JEditorPane( "text/html", "" );
		messageEditor.setEditable( false );
		panel.add( new JScrollPane( messageEditor ), BorderLayout.NORTH );

		reportArea = new JTextArea();
		reportArea.setTabSize( 4 );
		reportArea.setFont( reportFont );
		reportArea.setEditable( false );
		panel.add( new JScrollPane( reportArea ), BorderLayout.CENTER );

		this.setContentPane( panel );
		this.setSize( 600, 450 );
		this.setLocationRelativeTo( owner );
	}

	/**
	 * Formats provided content and populates the dialog.
	 */
	public void build() {
		StringBuilder htmlBuf = new StringBuilder();

		htmlBuf.append( htmlMessage );
		htmlBuf.append( "<br/>" );

		if ( htmlInstructions != null ) {
			htmlBuf.append( htmlInstructions );
			htmlBuf.append( "<br/>" );
		}

		htmlBuf.append( "Copy (Ctrl-A, Ctrl-C) the following text, including \"[ code ] tags\".<br/>" );
		htmlBuf.append( "<br/>" );

		StringBuilder reportBuf = new StringBuilder();

		reportBuf.append( "[code]\n" );

		if ( reportTitle != null ) {
			reportBuf.append( reportTitle ).append( "\n" );
			reportBuf.append( "\n" );
		}

		if ( exception != null ) {
			appendStackTrace( reportBuf, exception );
		}

		if ( appDescription != null ) {
			reportBuf.append( appDescription );
		}
		reportBuf.append( String.format( "OS: %s %s\n", System.getProperty( "os.name" ), System.getProperty( "os.version" ) ) );
		reportBuf.append( String.format( "VM: %s, %s, %s\n", System.getProperty( "java.vm.name" ), System.getProperty( "java.version" ), System.getProperty( "os.arch" ) ) );

		reportBuf.append( "[/code]\n" );
		reportBuf.append( "\n" );

		if ( attachmentName != null && attachmentContent != null ) {
			reportBuf.append( String.format( "File (\"%s\")...\n", attachmentName ) );
			reportBuf.append( "[code]\n" );
			reportBuf.append( attachmentContent );
			reportBuf.append( "\n[/code]\n" );
			reportBuf.append( "\n" );
		}

		messageEditor.setDocument( messageEditor.getEditorKit().createDefaultDocument() );
		messageEditor.setText( htmlBuf.toString() );
		messageEditor.setCaretPosition( 0 );

		reportArea.setText( reportBuf.toString() );
		reportArea.setCaretPosition( 0 );
	}

	/**
	 * Sets an html message.
	 */
	public void setHtmlMessage( String htmlMessage ) {
		this.htmlMessage = htmlMessage;
	}

	/**
	 * Sets boilerplace instructions to include in the html message.
	 */
	public void setHtmlInstructions( String htmlInstructions ) {
		this.htmlInstructions = htmlInstructions;
	}

	/**
	 * Sets a title for the report.
	 */
	public void setReportTitle( String reportTitle ) {
		this.reportTitle = reportTitle;
	}

	/**
	 * Sets an exception to include as a stack trace.
	 */
	public void setException( Throwable exception ) {
		this.exception = exception;
	}

	/**
	 * Sets an (app name: version) line for the runtime environment.
	 */
	public void setAppDescription( String appName, String appVersion ) {
		this.appDescription = String.format( "%s Version: %s\n", appName, appVersion );
	}

	/**
	 * Reads a stream to attach, encoded as hex.
	 */
	public void setAttachment( InputStream in, String attachmentName ) throws IOException {
		StringBuilder hexBuf = new StringBuilder();

		byte[] buf = new byte[4096];
		int len = 0;
		while ( (len = in.read( buf )) >= 0 ) {
			for ( int i=0; i < len; i++ ) {
				hexBuf.append( String.format( "%02x", buf[i] ) );
				if ( (i+1) % 32 == 0 ) {
					hexBuf.append( "\n" );
				}
			}
		}

		attachmentContent = hexBuf;
		attachmentName = attachmentName;
	}

	/**
	 * Sets a pre-encoded attachment.
	 */
	public void setAttachment( CharSequence attachmentContent, String attachmentName ) {
		this.attachmentContent = attachmentContent;
		this.attachmentName = attachmentName;
	}

	public JEditorPane getMessageEditor() {
		return messageEditor;
	}

	public JTextArea getReportArea() {
		return reportArea;
	}

	/**
	 * Formats an exception, appending lines to a bug report buffer.
	 */
	protected void appendStackTrace( StringBuilder reportBuf, Throwable exception ) {
		reportBuf.append( String.format( "Exception: %s\n", exception.toString() ) );
		reportBuf.append( "\n" );

		reportBuf.append( "Stack Trace...\n" );
		StackTraceElement[] traceElements = exception.getStackTrace();
		int traceDepth = 5;
		for ( int i=0; i < traceDepth && i < traceElements.length; i++ ) {
			reportBuf.append( String.format( "  %s\n", traceElements[i].toString() ) );
		}
/*
		Throwable currentCause = exception;

		// Traditional loggers truncate each cause's trace when a line is
		// already mentioned in the next downstream exception, i.e.,
		// remaining lines are redundant.

		while ( currentCause.getCause() != currentCause && null != (currentCause=currentCause.getCause()) ) {
			reportBuf.append( String.format( "Caused by: %s\n", currentCause.toString() ) );
			StackTraceElement[] causeElements = currentCause.getStackTrace();
			int causeDepth = 3;
			for ( int i=0; i < causeDepth && i < causeElements.length; i++ ) {
				reportBuf.append( String.format( "  %s\n", causeElements[i].toString() ) );
			}
		}
*/
		reportBuf.append( "\n" );
	}
}
