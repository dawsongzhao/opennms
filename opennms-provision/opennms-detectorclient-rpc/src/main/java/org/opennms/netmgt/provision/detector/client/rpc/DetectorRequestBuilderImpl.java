/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.provision.detector.client.rpc;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.opennms.netmgt.provision.DetectorRequestBuilder;
import org.opennms.netmgt.provision.ServiceDetector;
import org.opennms.netmgt.provision.ServiceDetectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorRequestBuilderImpl implements DetectorRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(DetectorRequestBuilderImpl.class);

    private static final String PORT = "port";

    private String location;

    private String className;

    private InetAddress address;

    private Map<String, String> attributes = new HashMap<>();

    private final LocationAwareDetectorClientRpcImpl client;

    public DetectorRequestBuilderImpl(LocationAwareDetectorClientRpcImpl client) {
        this.client = client;
    }

    @Override
    public DetectorRequestBuilder withLocation(String location) {
        this.location = location;
        return this;
    }


    @Override
    public DetectorRequestBuilder withClassName(String className) {
        this.className = className;
        return this;
    }

    @Override
    public DetectorRequestBuilder withServiceName(String serviceName) {
        ServiceDetector detector = client.getRegistry().getDetectorByServiceName(serviceName);
        if (detector == null) {
            throw new IllegalArgumentException("No detector found with service name '" + serviceName + "'.");
        }
        this.className = detector.getClass().getCanonicalName();
        return this;
    }

    @Override
    public DetectorRequestBuilder withAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public DetectorRequestBuilder withAttributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public DetectorRequestBuilder withAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public CompletableFuture<Boolean> execute() {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        } else if (className == null) {
            throw new IllegalArgumentException("Detector class name is required.");
        }

        ServiceDetectorFactory<?> factory = client.getRegistry().getDetectorFactoryByClassName(className);
        if (factory == null) {
            throw new IllegalArgumentException("No factory found for detector with class name '" + className + "'.");
        }

        Integer port = null;
        String portString = attributes.get(PORT);
        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to parse port as integer from: ", portString);
            }
        }

        final DetectorRequestDTO detectorRequestDTO = new DetectorRequestDTO();
        detectorRequestDTO.setLocation(location);
        detectorRequestDTO.setClassName(className);
        detectorRequestDTO.setAddress(address);
        detectorRequestDTO.addDetectorAttributes(attributes);
        detectorRequestDTO.addRuntimeAttributes(factory.getRuntimeAttributes(location, address, port));

        return client.getDelegate().execute(detectorRequestDTO)
            .thenApply(res -> {
                if (res.getFailureMesage() != null) {
                    throw new RuntimeException(res.getFailureMesage());
                } else {
                    return res.isDetected();
                }
            });
    }
}