package org.opennms.web.rest;

import javax.ws.rs.core.MediaType;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/*
 * TODO
 * 1. Need to figure it out how to create a Mock for EventProxy to validate events sent by RESTful service
 */
public class NodeRestServiceTest extends AbstractSpringJerseyRestTestCase {
    
    public void testNode() throws Exception {
        // Testing POST
        createNode();
        String url = "/nodes";
        // Testing GET Collection
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("Darwin TestMachine 9.4.0 Darwin Kernel Version 9.4.0"));
        url += "/1";
        // Testing PUT
        sendPut(url, "sysContact=OpenNMS&assetRecord.manufacturer=Apple&assetRecord.operatingSystem=MacOSX Leopard");
        // Testing GET Single Object
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<sysContact>OpenNMS</sysContact>"));        
        assertTrue(xml.contains("<operatingSystem>MacOSX Leopard</operatingSystem>"));        
        // Testing DELETE
        sendRequest(DELETE, url, 200);
        sendRequest(GET, url, 204);
    }

    public void testIpInterface() throws Exception {
        createIpInterface();
        String url = "/nodes/1/ipinterfaces";
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<ipAddress>10.10.10.10</ipAddress>"));
        url += "/10.10.10.10";
        sendPut(url, "ipStatus=0");
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<ipStatus>0</ipStatus>"));
        sendRequest(DELETE, url, 200);
        sendRequest(GET, url, 204);
    }

    public void testSnmpInterface() throws Exception {
        createSnmpInterface();
        String url = "/nodes/1/snmpinterfaces";
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<ifIndex>6</ifIndex>"));
        url += "/6";
        sendPut(url, "ifName=eth0");
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<ifName>eth0</ifName>"));
        sendRequest(DELETE, url, 200);
        sendRequest(GET, url, 204);
    }

    public void testMonitoredService() throws Exception {
        createService();
        String url = "/nodes/1/ipinterfaces/10.10.10.10/services";
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<name>ICMP</name>"));
        url += "/ICMP";
        sendPut(url, "status=A");
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<status>A</status>"));
        sendRequest(DELETE, url, 200);
        sendRequest(GET, url, 204);
    }
    
    public void testCategory() throws Exception {
        createCategory();
        String url = "/nodes/1/categories";
        String xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<name>Routers</name>"));
        url += "/Routers";
        sendPut(url, "description=My Equipment");
        xml = sendRequest(GET, url, 200);
        assertTrue(xml.contains("<description>My Equipment</description>"));
        sendRequest(DELETE, url, 200);
        sendRequest(GET, url, 204);
    }

    private void sendPost(String url, String xml) throws Exception {
        sendData(POST, MediaType.APPLICATION_XML, url, xml);
    }

    private void sendPut(String url, String formData) throws Exception {
        sendData(PUT, MediaType.APPLICATION_FORM_URLENCODED, url, formData);
    }
    
    private void sendData(String requestType, String contentType, String url, String data) throws Exception {
        MockHttpServletRequest request = createRequest(requestType, url);
        request.setContentType(contentType);
        request.setContent(data.getBytes());
        MockHttpServletResponse response = createResponse();        
        dispatch(request, response);
        assertEquals(200, response.getStatus());
    }

    private String sendRequest(String requestType, String url, int spectedStatus) throws Exception {
        MockHttpServletRequest request = createRequest(requestType, url);
        MockHttpServletResponse response = createResponse();
        dispatch(request, response);
        assertEquals(spectedStatus, response.getStatus());
        String xml = response.getContentAsString();
        if (xml != null)
            System.err.println(xml);
        return xml;
    }
    
    private void createNode() throws Exception {
        String node = "<node>" +            
        "<label>TestMachine</label>" +
        "<labelSource>H</labelSource>" +
        "<sysContact>The Owner</sysContact>" +
        "<sysDescription>" +
        "Darwin TestMachine 9.4.0 Darwin Kernel Version 9.4.0: Mon Jun  9 19:30:53 PDT 2008; root:xnu-1228.5.20~1/RELEASE_I386 i386" +
        "</sysDescription>" +
        "<sysLocation>DevJam</sysLocation>" +
        "<sysName>TestMachine</sysName>" +
        "<sysObjectId>.1.3.6.1.4.1.8072.3.2.255</sysObjectId>" +
        "<type>A</type>" +
        "</node>";
        sendPost("/nodes", node);
    }
    
    private void createIpInterface() throws Exception {
        createNode();
        String ipInterface = "<ipInterface>" +
        "<ipAddress>10.10.10.10</ipAddress>" +
        "<ipHostName>TestMachine</ipHostName>" +
        "<ipStatus>1</ipStatus>" +
        "<isManaged>M</isManaged>" +
        "<isSnmpPrimary>" +
        "<charCode>80</charCode>" +
        "</isSnmpPrimary>" +
        "</ipInterface>";
        sendPost("/nodes/1/ipinterfaces", ipInterface);
    }

    private void createSnmpInterface() throws Exception {
        createIpInterface();
        String snmpInterface = "<snmpInterface>" +
        "<ifAdminStatus>1</ifAdminStatus>" +
        "<ifDescr>en1</ifDescr>" +
        "<ifIndex>6</ifIndex>" +
        "<ifName>en1</ifName>" +
        "<ifOperStatus>1</ifOperStatus>" +
        "<ifSpeed>10000000</ifSpeed>" +
        "<ifType>6</ifType>" +
        "<ipAddress>10.10.10.10</ipAddress>" +
        "<netMask>255.255.255.0</netMask>" +
        "<physAddr>001e5271136d</physAddr>" +
        "</snmpInterface>";
        sendPost("/nodes/1/snmpinterfaces", snmpInterface);
    }
    
    private void createService() throws Exception {
        createIpInterface();
        String service = "<service>" +
        "<notify>Y</notify>" +
        "<serviceType>" +
        "<name>ICMP</name>" +
        "</serviceType>" +
        "<source>P</source>" +
        "<status>N</status>" +
        "</service>";
        sendPost("/nodes/1/ipinterfaces/10.10.10.10/services", service);
    }

    private void createCategory() throws Exception {
        createNode();
        String service = "<category>" +
        "<name>Routers</name>" +
        "<description>Core Routers</description>" +
        "</category>";
        sendPost("/nodes/1/categories", service);
    }

}
