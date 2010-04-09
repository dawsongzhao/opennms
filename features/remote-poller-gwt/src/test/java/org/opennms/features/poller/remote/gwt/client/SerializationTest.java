package org.opennms.features.poller.remote.gwt.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opennms.features.poller.remote.gwt.client.location.LocationDetails;
import org.opennms.features.poller.remote.gwt.client.location.LocationInfo;

import com.google.gwt.user.server.rpc.impl.LegacySerializationPolicy;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;

public class SerializationTest {
	private static final ServerSerializationStreamWriter writer = new ServerSerializationStreamWriter(LegacySerializationPolicy.getInstance());

	@Test
	public void testGWTLatLng() throws Exception {
		GWTLatLng point = new GWTLatLng(1D,1D);
		writer.writeObject(point);
	}

	@Test
	public void testGWTLocationMonitor() throws Exception {
		GWTLocationMonitor monitor = getMonitor();
		writer.writeObject(monitor);
	}

	@Test
	public void testGWTMonitoredService() throws Exception {
		GWTMonitoredService service = getMonitoredService();
		writer.writeObject(service);
	}

	@Test
	public void testGWTLocationSpecificStatus() throws Exception {
		GWTLocationSpecificStatus status = getLocationSpecificStatus();
		writer.writeObject(status);
	}

	@Test
	public void testLocationInfo() throws Exception {
		LocationInfo location = new LocationInfo();
		location.setName("Bob");
		location.setPollingPackageName("Harry");
		location.setArea("East Coast");
		location.setGeolocation("RDU");
		location.setCoordinates("0.0,0.0");
		location.setMonitorStatus(Status.UP);
		writer.writeObject(location);
	}

	@Test
	public void testLocationDetails() throws Exception {
		LocationDetails l = new LocationDetails();
		l.setLocationMonitorState(getLocationMonitorState());
		writer.writeObject(l);
	}

	@Test
	public void testBaseLocation() throws Exception {
		BaseLocation location = new BaseLocation();

		LocationInfo info = new LocationInfo();
		info.setArea("East Coast");
		info.setGeolocation("RDU");
		info.setCoordinates("0.0,0.0");
		info.setName("Bob");
		info.setPollingPackageName("Harry");

		LocationDetails details = new LocationDetails();
		details.setLocationMonitorState(getLocationMonitorState());
		details.setApplicationState(getApplicationState());

		location.setLocationInfo(info);
		location.setLocationDetails(details);

		writer.writeObject(location);
	}

	private ApplicationState getApplicationState() {
		final Collection<String> applications = new ArrayList<String>();
		final Collection<GWTLocationSpecificStatus> statuses = new ArrayList<GWTLocationSpecificStatus>();
		final Map<String,Collection<GWTLocationSpecificStatus>> applicationStatuses = new HashMap<String,Collection<GWTLocationSpecificStatus>>();
		applications.add("TestApp1");
		applicationStatuses.put("TestApp1", statuses);
		statuses.add(getLocationSpecificStatus());
		return new ApplicationState(applications, applicationStatuses);
	}

	private LocationMonitorState getLocationMonitorState() {
		Collection<GWTLocationMonitor> monitors = new ArrayList<GWTLocationMonitor>();
		Collection<GWTLocationSpecificStatus> statuses = new ArrayList<GWTLocationSpecificStatus>();
		monitors.add(getMonitor());
		statuses.add(getLocationSpecificStatus());
		return new LocationMonitorState(monitors, statuses);
	}

	private GWTLocationSpecificStatus getLocationSpecificStatus() {
		GWTLocationSpecificStatus status = new GWTLocationSpecificStatus();
		status.setId(1);
		status.setLocationMonitor(getMonitor());
		status.setMonitoredService(getMonitoredService());
		status.setPollResult(getPollResult());
		status.setPollTime(new Date());
		return status;
	}

	private GWTPollResult getPollResult() {
		GWTPollResult result = new GWTPollResult();
		result.setReason("because!");
		result.setResponseTime(300D);
		result.setStatus("Up");
		result.setTimestamp(new Date());
		return result;
	}

	private GWTMonitoredService getMonitoredService() {
		GWTMonitoredService service = new GWTMonitoredService();
		service.setHostname("localhost");
		service.setId(1);
		service.setIfIndex(0);
		service.setIpAddress("127.0.0.1");
		service.setIpInterfaceId(2);
		service.setNodeId(3);
		service.setServiceName("HTTP");
		service.setApplications(Arrays.asList("TestApp1","TestApp3"));
		return service;
	}

	private GWTLocationMonitor getMonitor() {
		GWTLocationMonitor monitor = new GWTLocationMonitor();
		monitor.setDefinitionName("blah");
		monitor.setId(1);
		monitor.setLastCheckInTime(new Date());
		monitor.setName("foo");
		monitor.setStatus("STARTED");
		return monitor;
	}
}
