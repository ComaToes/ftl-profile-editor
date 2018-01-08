package net.blerf.ftl.net;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import net.blerf.ftl.net.TaggedString;


/**
 * A ResponseHandler that returns content as a String, with the ETag.
 *
 * If the server responds with status 304 (Not Modified), null is returned.
 * This will only occur if the request had included an "If-None-Match" header.
 *
 * Example: request.addHeader( "If-None-Match", "\"blah\"" );  // Quotes!
 *
 * If the response does not include an ETag header, the TaggedString's etag
 * will be null.
 */
public class TaggedStringResponseHandler implements ResponseHandler<TaggedString> {

	@Override
	public TaggedString handleResponse( HttpResponse response ) throws ClientProtocolException, IOException {

		int status = response.getStatusLine().getStatusCode();
		if ( status >= 200 && status < 300 ) {

			String eTagCurrent = null;
			if ( response.containsHeader( "ETag" ) ) {
				eTagCurrent = response.getLastHeader( "ETag" ).getValue();
			}

			HttpEntity entity = response.getEntity();
			String responseText = (entity != null ? EntityUtils.toString( entity ) : null);

			return new TaggedString( responseText, eTagCurrent );
		}
		else if ( status == 304 ) {  // Not modified.
			// Nothing to see.
			return null;
		}
		else {
			throw new ClientProtocolException( "Unexpected response status: "+ status );
		}
	}
}
