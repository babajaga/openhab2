package org.openhab.binding.deconz.upnp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A <tt>GatewayDevice</tt> is a class that abstracts UPnP-compliant gateways
 * <p/>
 * It holds all the information that comes back as UPnP responses, and
 * provides methods to issue UPnP commands to a gateway.
 *
 * @author casta
 */
public class GatewayDevice {

	/**
	 * Receive timeout when requesting data from device
	 */
    private static final int HTTP_RECEIVE_TIMEOUT = 7000;
    
	private String st;
    private String location;
    private String serviceType;
    private String serviceTypeCIF;
    private String urlBase;
    private String controlURL;
    private String controlURLCIF;
    private String eventSubURL;
    private String eventSubURLCIF;
    private String sCPDURL;
    private String sCPDURLCIF;
    private String deviceType;
    private String deviceTypeCIF;

    // description data

    /**
     * The friendly (human readable) name associated with this device
     */
    private String friendlyName;

    /**
     * The device manufacturer name
     */
    private String manufacturer;

    /**
     * The model description as a string
     */
    private String modelDescription;

    /**
     * The URL that can be used to access the IGD interface
     */
    private String presentationURL;

    /**
     * The address used to reach this machine from the GatewayDevice
     */
    private InetAddress localAddress;

    /**
     * The model number (used by the manufacturer to identify the product)
     */
    private String modelNumber;

    /**
     * The model name
     */
    private String modelName;

    /**
     * Creates a new instance of GatewayDevice
     */
    public GatewayDevice() {
    }

    /**
     * Retrieves the properties and description of the GatewayDevice.
     * <p/>
     * Connects to the device's {@link #location} and parses the response
     * using a {@link GatewayDeviceHandler} to populate the fields of this
     * class
     *
     * @throws SAXException if an error occurs while parsing the request
     * @throws IOException  on communication errors
     * @see org.bitlet.weupnp.GatewayDeviceHandler
     */
    public void loadDescription() throws SAXException, IOException {

        URLConnection urlConn = new URL(getLocation()).openConnection();
        urlConn.setReadTimeout(HTTP_RECEIVE_TIMEOUT);

        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new GatewayDeviceHandler(this));
        parser.parse(new InputSource(urlConn.getInputStream()));


        /* fix urls */
        String ipConDescURL;
        if (urlBase != null && urlBase.trim().length() > 0) {
            ipConDescURL = urlBase;
        } else {
            ipConDescURL = location;
        }

        int lastSlashIndex = ipConDescURL.indexOf('/', 7);
        if (lastSlashIndex > 0) {
            ipConDescURL = ipConDescURL.substring(0, lastSlashIndex);
        }


        sCPDURL = copyOrCatUrl(ipConDescURL, sCPDURL);
        controlURL = copyOrCatUrl(ipConDescURL, controlURL);
        controlURLCIF = copyOrCatUrl(ipConDescURL, controlURLCIF);
        presentationURL = copyOrCatUrl(ipConDescURL, presentationURL);
    }

    /**
     * Issues UPnP commands to a GatewayDevice that can be reached at the
     * specified <tt>url</tt>
     * <p/>
     * The command is identified by a <tt>service</tt> and an <tt>action</tt>
     * and can receive arguments
     *
     * @param url     the url to use to contact the device
     * @param service the service to invoke
     * @param action  the specific action to perform
     * @param args    the command arguments
     * @return the response to the performed command, as a name-value map.
     *         In case errors occur, the returned map will be <i>empty.</i>
     * @throws IOException  on communication errors
     * @throws SAXException if errors occur while parsing the response
     */
    public static Map<String, String> simpleUPnPcommand(String url,
                                                        String service, String action, Map<String, String> args)
            throws IOException, SAXException {
        String soapAction = "\"" + service + "#" + action + "\"";
        StringBuilder soapBody = new StringBuilder();

        soapBody.append("<?xml version=\"1.0\"?>\r\n" +
                "<SOAP-ENV:Envelope " +
                "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<SOAP-ENV:Body>" +
                "<m:" + action + " xmlns:m=\"" + service + "\">");

        if (args != null && args.size() > 0) {

            Set<Map.Entry<String, String>> entrySet = args.entrySet();

            for (Map.Entry<String, String> entry : entrySet) {
                soapBody.append("<" + entry.getKey() + ">" + entry.getValue() +
                        "</" + entry.getKey() + ">");
            }

        }

        soapBody.append("</m:" + action + ">");
        soapBody.append("</SOAP-ENV:Body></SOAP-ENV:Envelope>");

        URL postUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) postUrl.openConnection();

        conn.setRequestMethod("POST");
        conn.setReadTimeout(HTTP_RECEIVE_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("SOAPAction", soapAction);
        conn.setRequestProperty("Connection", "Close");

        byte[] soapBodyBytes = soapBody.toString().getBytes();

        conn.setRequestProperty("Content-Length",
                String.valueOf(soapBodyBytes.length));

        conn.getOutputStream().write(soapBodyBytes);

        Map<String, String> nameValue = new HashMap<String, String>();
        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new NameValueHandler(nameValue));
        if (conn.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            try {
                // attempt to parse the error message
                parser.parse(new InputSource(conn.getErrorStream()));
            } catch (SAXException e) {
                // ignore the exception
                // FIXME We probably need to find a better way to return
                // significant information when we reach this point
            }
            conn.disconnect();
            return nameValue;
        } else {
            parser.parse(new InputSource(conn.getInputStream()));
            conn.disconnect();
            return nameValue;
        }
    }

    /**
     * Retrieves the connection status of this device
     *
     * @return true if connected, false otherwise
     * @throws IOException
     * @throws SAXException
     * @see #simpleUPnPcommand(java.lang.String, java.lang.String,
     *      java.lang.String, java.util.Map)
     */
    public boolean isConnected() throws IOException, SAXException {
        Map<String, String> nameValue = simpleUPnPcommand(controlURL,
                serviceType, "GetStatusInfo", null);

        String connectionStatus = nameValue.get("NewConnectionStatus");
        if (connectionStatus != null
                && connectionStatus.equalsIgnoreCase("Connected")) {
            return true;
        }

        return false;
    }

    /**
     * Retrieves the external IP address associated with this device
     * <p/>
     * The external address is the address that can be used to connect to the
     * GatewayDevice from the external network
     *
     * @return the external IP
     * @throws IOException
     * @throws SAXException
     * @see #simpleUPnPcommand(java.lang.String, java.lang.String,
     *      java.lang.String, java.util.Map)
     */
    public String getExternalIPAddress() throws IOException, SAXException {
        Map<String, String> nameValue = simpleUPnPcommand(controlURL,
                serviceType, "GetExternalIPAddress", null);

        return nameValue.get("NewExternalIPAddress");
    }

    // getters and setters

    /**
     * Gets the local address to connect the gateway through
     *
     * @return the {@link #localAddress}
     */
    public InetAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Sets the {@link #localAddress}
     *
     * @param localAddress the address to set
     */
    public void setLocalAddress(InetAddress localAddress) {
        this.localAddress = localAddress;
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceTypeCIF() {
        return serviceTypeCIF;
    }

    public void setServiceTypeCIF(String serviceTypeCIF) {
        this.serviceTypeCIF = serviceTypeCIF;
    }

    public String getControlURL() {
        return controlURL;
    }

    public void setControlURL(String controlURL) {
        this.controlURL = controlURL;
    }

    public String getControlURLCIF() {
        return controlURLCIF;
    }

    public void setControlURLCIF(String controlURLCIF) {
        this.controlURLCIF = controlURLCIF;
    }

    public String getEventSubURL() {
        return eventSubURL;
    }

    public void setEventSubURL(String eventSubURL) {
        this.eventSubURL = eventSubURL;
    }

    public String getEventSubURLCIF() {
        return eventSubURLCIF;
    }

    public void setEventSubURLCIF(String eventSubURLCIF) {
        this.eventSubURLCIF = eventSubURLCIF;
    }

    public String getSCPDURL() {
        return sCPDURL;
    }

    public void setSCPDURL(String sCPDURL) {
        this.sCPDURL = sCPDURL;
    }

    public String getSCPDURLCIF() {
        return sCPDURLCIF;
    }

    public void setSCPDURLCIF(String sCPDURLCIF) {
        this.sCPDURLCIF = sCPDURLCIF;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceTypeCIF() {
        return deviceTypeCIF;
    }

    public void setDeviceTypeCIF(String deviceTypeCIF) {
        this.deviceTypeCIF = deviceTypeCIF;
    }

    public String getURLBase() {
        return urlBase;
    }

    public void setURLBase(String uRLBase) {
        this.urlBase = uRLBase;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public String getPresentationURL() {
        return presentationURL;
    }

    public void setPresentationURL(String presentationURL) {
        this.presentationURL = presentationURL;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    // private methods
    private String copyOrCatUrl(String dst, String src) {
        if (src != null) {
            if (src.startsWith("http://")) {
                dst = src;
            } else {
                if (!src.startsWith("/")) {
                    dst += "/";
                }
                dst += src;
            }
        }
        return dst;
    }
}
