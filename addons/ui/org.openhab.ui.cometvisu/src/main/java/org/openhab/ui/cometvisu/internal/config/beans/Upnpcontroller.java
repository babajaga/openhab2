//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.04.17 at 05:50:37 PM CEST 
//

package org.openhab.ui.cometvisu.internal.config.beans;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for upnpcontroller complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="upnpcontroller">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="player_ip_addr" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="player_port" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="refresh" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="debug" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "upnpcontroller")
public class Upnpcontroller {

    @XmlAttribute(name = "label", required = true)
    protected String label;
    @XmlAttribute(name = "player_ip_addr", required = true)
    protected String playerIpAddr;
    @XmlAttribute(name = "player_port")
    protected String playerPort;
    @XmlAttribute(name = "refresh", required = true)
    protected String refresh;
    @XmlAttribute(name = "debug")
    protected Boolean debug;

    /**
     * Gets the value of the label property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     * Gets the value of the playerIpAddr property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getPlayerIpAddr() {
        return playerIpAddr;
    }

    /**
     * Sets the value of the playerIpAddr property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setPlayerIpAddr(String value) {
        this.playerIpAddr = value;
    }

    /**
     * Gets the value of the playerPort property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getPlayerPort() {
        return playerPort;
    }

    /**
     * Sets the value of the playerPort property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setPlayerPort(String value) {
        this.playerPort = value;
    }

    /**
     * Gets the value of the refresh property.
     * 
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getRefresh() {
        return refresh;
    }

    /**
     * Sets the value of the refresh property.
     * 
     * @param value
     *            allowed object is
     *            {@link String }
     * 
     */
    public void setRefresh(String value) {
        this.refresh = value;
    }

    /**
     * Gets the value of the debug property.
     * 
     * @return
     *         possible object is
     *         {@link Boolean }
     * 
     */
    public Boolean isDebug() {
        return debug;
    }

    /**
     * Sets the value of the debug property.
     * 
     * @param value
     *            allowed object is
     *            {@link Boolean }
     * 
     */
    public void setDebug(Boolean value) {
        this.debug = value;
    }

}
