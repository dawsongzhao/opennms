package org.opennms.features.topology.plugins.topo.adapter.internal;

import com.vaadin.data.Item;

interface ItemFinder {
	Item getItem(Object itemId);
}