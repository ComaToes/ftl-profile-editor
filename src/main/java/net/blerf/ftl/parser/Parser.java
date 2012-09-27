package net.blerf.ftl.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Parser {
	
	private byte[] intbuf = new byte[4];

	protected int readInt(InputStream in) throws IOException {
		
		in.read(intbuf);
		
		int v = 0;
		
		for (int i = 0; i < intbuf.length; i++) {
			v |= (((int)intbuf[i]) & 0xff) << (i*8);
		}
		
		return v;
		
	}
	
	protected void writeInt(OutputStream out, int value) throws IOException {
		
		for (int i = 0; i < intbuf.length; i++) {
			intbuf[i] = (byte)(value >> (i*8));
		}
		
		out.write(intbuf);
		
	}
	
	protected String readString(InputStream in) throws IOException {
		
		int length = readInt(in);
		
		byte[] strarr = new byte[length];
		
		in.read(strarr);
		
		return new String(strarr);
		
	}
	
	protected void writeString(OutputStream out, String str) throws IOException {
		
		writeInt(out, str.length());
		
		out.write( str.getBytes() );
		
	}
	
}
