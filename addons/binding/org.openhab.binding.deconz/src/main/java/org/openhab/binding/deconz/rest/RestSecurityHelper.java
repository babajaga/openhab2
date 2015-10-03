package org.openhab.binding.deconz.rest;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RestSecurityHelper {

	private static TrustManager[] trustAllCertificates = new TrustManager[] {
        new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Do nothing. Just allow them all.
			}
			@Override
			public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                // Do nothing. Just allow them all.
			}
			@Override
			public X509Certificate[] getAcceptedIssuers() {
                return null; // Not relevant.
			}
        }
    };

    private static HostnameVerifier trustAllHostnames = new HostnameVerifier() {
		@Override
		public boolean verify(String arg0, SSLSession arg1) {
            return true; // Just allow them all.
		}
    };

	// Sometimes you need to connect a HTTPS URL. In that case, you may likely face a 
	// javax.net.ssl.SSLException: Not trusted server certificate on some HTTPS sites who 
	// doesn't keep their SSL certificates up to date, or a 
	// java.security.cert.CertificateException: No subject alternative DNS name matching [hostname] found or 
	// javax.net.ssl.SSLProtocolException: handshake alert: unrecognized_name on some misconfigured HTTPS sites.
	// The following one-time-run initializer should make HttpsURLConnection more lenient as to those 
	// HTTPS sites and thus not throw those exceptions anymore.
	public static void httpsTrustAllCerticicates() {
	    try {
	        System.setProperty("jsse.enableSNIExtension", "false");
	        SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCertificates, new SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	        HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
}
