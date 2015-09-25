package org.openhab.binding.deconz.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class RestResult {

	private final static boolean DO_PRINT = true;
	
	public final static int REST_OK = 0;
	public final static int REST_CONFIGURATION_ERROR = 1;
	public final static int REST_CONNECT_ERROR = 2;
	public final static int REST_AUTHENTICATION_ERROR = 3;
	public final static int REST_RESPONSE_ERROR = 4;
	public final static int REST_NOT_MODIFIED = 5;
	public final static int REST_NO_RESULT = 6;
	
	private int result = REST_NO_RESULT;
	private boolean hasResult = false;
	private JSONObject data = null;
	
	public int getResult() {
		if (hasResult()) {
			return result;
		}
		return REST_NO_RESULT;
	}

	public void setResult(int result) {
		this.result = result;
		hasResult = true;
	}

	public boolean hasResult() {
		return hasResult;
	}
	
	public void setData(InputStream in) {
		data = null;
		if (in != null) {
			try {
				String s = stream2String(in);
				data = new JSONObject(s);
			} catch (Exception e) {
			}
		}
	}

	public JSONObject getData() {
		if (result == REST_OK) {
			return data;
		}
		return null;
	}
		
	private String stream2String(InputStream in) {
        String ret = new String(); 
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();		
	        byte[] buffer = new byte[512];
	        boolean first = true;
	        for (int len; (len = in.read(buffer)) != -1;) {
        		if (first) {
    	        	int pos = 0;
        			while (buffer[pos] != '{') buffer[pos++] = ' ';
        			first = false;
        		}
        		os.write(buffer, 0, len);
	        }
	        os.flush();
	        ret = os.toString();
			if (DO_PRINT) {
				System.out.print("Response is " + ret.length() + " bytes. ");
				if (ret.contains("\"lights\"")) {
					System.out.print("Response contains lights. ");
//					BufferedWriter writer = null;
//					try {
//					    writer = new BufferedWriter(new FileWriter("c:\\deconz.log"));
//					    writer.write(ret);
//					    writer.flush();
//					} catch (IOException e) {
//					}
//					if (writer != null) {
//					    try {
//					        writer.close( );
//					    } catch (IOException e) {
//					    }
//					}
				}
				System.out.println(" ");
			}
		} catch (Exception e) {
		}
        return ret;
	}
}
