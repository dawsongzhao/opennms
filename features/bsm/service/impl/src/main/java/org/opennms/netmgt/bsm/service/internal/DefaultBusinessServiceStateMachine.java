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
 * OpenNMS(R) Licensing <license@opennms.org>
 *      http://www.opennms.org/
 *      http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.bsm.service.internal;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.imageio.ImageIO;

import org.apache.commons.collections15.Transformer;
import org.opennms.netmgt.bsm.service.AlarmProvider;
import org.opennms.netmgt.bsm.service.BusinessServiceStateChangeHandler;
import org.opennms.netmgt.bsm.service.BusinessServiceStateMachine;
import org.opennms.netmgt.bsm.service.model.AlarmWrapper;
import org.opennms.netmgt.bsm.service.model.IpService;
import org.opennms.netmgt.bsm.service.model.ReadOnlyBusinessService;
import org.opennms.netmgt.bsm.service.model.Status;
import org.opennms.netmgt.bsm.service.model.edge.IpServiceEdge;
import org.opennms.netmgt.bsm.service.model.edge.ro.ReadOnlyEdge;
import org.opennms.netmgt.bsm.service.model.graph.BusinessServiceGraph;
import org.opennms.netmgt.bsm.service.model.graph.GraphEdge;
import org.opennms.netmgt.bsm.service.model.graph.GraphVertex;
import org.opennms.netmgt.bsm.service.model.graph.internal.BusinessServiceGraphImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationImageServer;

public class DefaultBusinessServiceStateMachine implements BusinessServiceStateMachine {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultBusinessServiceStateMachine.class);
    public static final Status DEFAULT_SEVERITY = Status.NORMAL;
    public static final Status MIN_SEVERITY = Status.NORMAL;

    @Autowired
    private AlarmProvider m_alarmProvider;

    private final List<BusinessServiceStateChangeHandler> m_handlers = Lists.newArrayList();
    private final ReadWriteLock m_rwLock = new ReentrantReadWriteLock();
    private BusinessServiceGraph m_g = new BusinessServiceGraphImpl(Collections.emptyList());

    @Override
    public void setBusinessServices(List<? extends ReadOnlyBusinessService> businessServices) {
        m_rwLock.writeLock().lock();
        try {
            // Create a new graph
            BusinessServiceGraph g = new BusinessServiceGraphImpl(businessServices);

            // Prime the graph with the state from the previous graph and
            // keep track of the new reductions keys
            Set<String> reductionsKeysToLookup = Sets.newHashSet();
            for (String reductionKey : g.getReductionKeys()) {
                GraphVertex reductionKeyVertex = m_g.getVertexByReductionKey(reductionKey);
                if (reductionKeyVertex != null) {
                    updateAndPropagateVertex(g, g.getVertexByReductionKey(reductionKey), reductionKeyVertex.getStatus());
                } else {
                    reductionsKeysToLookup.add(reductionKey);
                }
            }

            if (m_alarmProvider == null && reductionsKeysToLookup.size() > 0) {
                LOG.warn("There are one or more reduction keys to lookup, but no alarm provider is set.");
            } else {
                // Query the status of the reductions keys that were added
                // We do this so that we can immediately reflect the state of the new
                // graph without having to wait for calls to handleNewOrUpdatedAlarm()
                for (String reductionKey : reductionsKeysToLookup) {
                    AlarmWrapper alarm = m_alarmProvider.lookup(reductionKey);
                    if (alarm != null) {
                        updateAndPropagateVertex(g, g.getVertexByReductionKey(reductionKey), alarm.getStatus());
                    }
                }
            }
            m_g = g;
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    @Override
    public void handleNewOrUpdatedAlarm(AlarmWrapper alarm) {
        m_rwLock.writeLock().lock();
        try {
            // Recursively propagate the status
            updateAndPropagateVertex(m_g, m_g.getVertexByReductionKey(alarm.getReductionKey()), alarm.getStatus());
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    private void updateAndPropagateVertex(BusinessServiceGraph graph, GraphVertex vertex, Status newStatus) {
        if (vertex == null) {
            // Nothing to do here
            return;
        }

        // Update the status if necessary
        Status previousStatus = vertex.getStatus();
        if (previousStatus.equals(newStatus)) {
            // The status hasn't changed, there's nothing to propagate
            return;
        }
        vertex.setStatus(newStatus);
        // Notify the listeners
        onStatusUpdated(graph, vertex, previousStatus);

        // Update the edges with the mapped status
        List<GraphEdge> updatedEges = Lists.newArrayList();
        for (GraphEdge edge : graph.getInEdges(vertex)) {
            Status mappedStatus = edge.getMapFunction().map(newStatus).orElse(DEFAULT_SEVERITY);
            if (mappedStatus.equals(edge.getStatus())) {
                // The status hasn't changed
                continue;
            }

            // Update the status and add it to the list of edges to propagate
            edge.setStatus(mappedStatus);
            updatedEges.add(edge);
        }

        // Propagate once all of the edges have been updated
        for (GraphEdge edge : updatedEges) {
            reduceUpdateAndPropagateVertex(graph, graph.getOpposite(vertex, edge));
        }
    }

    private void reduceUpdateAndPropagateVertex(BusinessServiceGraph graph, GraphVertex vertex) {
        if (vertex == null) {
            // Nothing to do here
            return;
        }

        // Calculate the weighed statuses from the child edges
        List<Status> statuses = weighStatuses(graph.getOutEdges(vertex));

        // Reduce
        Status newStatus = vertex.getReductionFunction().reduce(statuses).orElse(DEFAULT_SEVERITY);

        // Apply lower bound
        newStatus = newStatus.isLessThan(MIN_SEVERITY) ? MIN_SEVERITY : newStatus;

        // Update and propagate
        updateAndPropagateVertex(graph, vertex, newStatus);
    }

    protected static List<Status> weighStatuses(Collection<GraphEdge> edges) {
        // Find the greatest common divisor of all the weights
        int gcd = edges.stream()
                .map(e -> e.getWeight())
                .reduce((a,b) -> BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValue())
                .orElse(1);

        // Multiply the statuses based on their relative weight
        List<Status> statuses = Lists.newArrayList();
        for (GraphEdge edge : edges) {
            int relativeWeight = Math.floorDiv(edge.getWeight(), gcd);
            for (int i = 0; i < relativeWeight; i++) {
                statuses.add(edge.getStatus());
            }
        }

        return statuses;
    }

    private void onStatusUpdated(BusinessServiceGraph graph, GraphVertex vertex, Status previousStatus) {
        ReadOnlyBusinessService businessService = vertex.getBusinessService();
        if (businessService == null) {
            // Only send updates for business services (and not for reduction keys)
            return;
        }

        if (graph != m_g) {
            // We're working with a new graph, only send a status update if the new status is different
            // than the one in the previous graph
            GraphVertex previousVertex = m_g.getVertexByBusinessServiceId(businessService.getId());
            if (previousVertex != null && vertex.getStatus().equals(previousVertex.getStatus())) {
                // The vertex for this business service in the previous graph
                // had the same status, don't issue any notifications
                return;
            }
        }

        for (BusinessServiceStateChangeHandler handler : m_handlers) {
            handler.handleBusinessServiceStateChanged(businessService, vertex.getStatus(), previousStatus);
        }
    }

    @Override
    public Status getOperationalStatus(ReadOnlyBusinessService businessService) {
        Objects.requireNonNull(businessService);
        m_rwLock.readLock().lock();
        try {
            GraphVertex vertex = m_g.getVertexByBusinessServiceId(businessService.getId());
            if (vertex != null) {
                return vertex.getStatus();
            }
            return null;
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    @Override
    public Status getOperationalStatus(IpService ipService) {
        m_rwLock.readLock().lock();
        try {
            GraphVertex vertex = m_g.getVertexByIpServiceId(Long.valueOf(ipService.getId()));
            if (vertex != null) {
                return vertex.getStatus();
            }
            return null;
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    @Override
    public Status getOperationalStatus(String reductionKey) {
        m_rwLock.readLock().lock();
        try {
            GraphVertex vertex = m_g.getVertexByReductionKey(reductionKey);
            if (vertex != null) {
                return vertex.getStatus();
            }
            return null;
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    @Override
    public Status getOperationalStatus(ReadOnlyEdge edge) {
        m_rwLock.readLock().lock();
        try {
            GraphVertex vertex = m_g.getVertexByEdgeId(edge.getId());
            if (vertex != null) {
                return vertex.getStatus();
            }
            return null;
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    public void setAlarmProvider(AlarmProvider alarmProvider) {
        m_rwLock.writeLock().lock();
        try {
            m_alarmProvider = alarmProvider;
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    @Override
    public void addHandler(BusinessServiceStateChangeHandler handler, Map<String, String> attributes) {
        m_rwLock.writeLock().lock();
        try {
            m_handlers.add(handler);
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeHandler(BusinessServiceStateChangeHandler handler, Map<String, String> attributes) {
        m_rwLock.writeLock().lock();
        try {
            return m_handlers.remove(handler);
        } finally {
            m_rwLock.writeLock().unlock();
        }
    }

    @Override
    public void renderGraphToPng(File tempFile) {
        m_rwLock.readLock().lock();
        try {
            Layout<GraphVertex,GraphEdge> layout = new KKLayout<GraphVertex,GraphEdge>(m_g);
            layout.setSize(new Dimension(1024,1024)); // Size of the layout

            VisualizationImageServer<GraphVertex, GraphEdge> vv = new VisualizationImageServer<GraphVertex, GraphEdge>(layout, layout.getSize());
            vv.setPreferredSize(new Dimension(1200,1200)); // Viewing area size
            vv.getRenderContext().setVertexLabelTransformer(new Transformer<GraphVertex,String>() {
                @Override
                public String transform(GraphVertex vertex) {
                    if (vertex.getBusinessService() != null) {
                        return String.format("BS[%s]", vertex.getBusinessService().getName());
                    }
                    if (vertex.getReductionKey() != null) {
                        return String.format("RK[%s]", vertex.getReductionKey());
                    }
                    // Check for type last, as the reduction key edges of ip services are of type IP_SERVICE
                    if (vertex.getEdge().getType() == ReadOnlyEdge.Type.IP_SERVICE) {
                        IpService ipService = ((IpServiceEdge) vertex.getEdge()).getIpService();
                        return String.format("IP_SERVICE[%s,%s]", ipService.getId(), ipService.getServiceName());
                    }
                    return String.format("%s[%d]", vertex.getEdge().getType(), vertex.getEdge().getId());
                }
            });
            vv.getRenderContext().setEdgeLabelTransformer(new Transformer<GraphEdge,String>() {
                @Override
                public String transform(GraphEdge edge) {
                    return String.format("%s", edge.getMapFunction().getClass().getSimpleName());
                }
            });

            // Create the buffered image
            BufferedImage image = (BufferedImage) vv.getImage(
                    new Point2D.Double(vv.getGraphLayout().getSize().getWidth() / 2,
                    vv.getGraphLayout().getSize().getHeight() / 2),
                    new Dimension(vv.getGraphLayout().getSize()));

            // Render
            try {
                ImageIO.write(image, "png", tempFile);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        } finally {
            m_rwLock.readLock().unlock();
        }
    }

    @Override
    public BusinessServiceGraph getGraph() {
        return m_g;
    }
}