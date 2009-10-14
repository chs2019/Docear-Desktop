/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file author is Christian Foltin
 *  It is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.common.addins.misc;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.freeplane.core.addins.NodeHookDescriptor;
import org.freeplane.core.addins.PersistentNodeHook;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.icon.IconController;
import org.freeplane.core.icon.MindIcon;
import org.freeplane.core.icon.UIIcon;
import org.freeplane.core.icon.UIIconSet;
import org.freeplane.core.icon.ZoomedIcon;
import org.freeplane.core.io.IReadCompletionListener;
import org.freeplane.core.modecontroller.IMapChangeListener;
import org.freeplane.core.modecontroller.INodeChangeListener;
import org.freeplane.core.modecontroller.MapChangeEvent;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.modecontroller.NodeChangeEvent;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.ui.ActionLocationDescriptor;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * @author Foltin
 */
@NodeHookDescriptor(hookName = "accessories/plugins/HierarchicalIcons.properties")
@ActionLocationDescriptor(locations = { "/menu_bar/format/nodes/automaticLayout2" })
public class HierarchicalIcons extends PersistentNodeHook implements INodeChangeListener, IMapChangeListener,
        IReadCompletionListener, IExtension {
	final private Map<NodeModel, TreeSet<MindIcon>> nodeIconSets = new HashMap<NodeModel, TreeSet<MindIcon>>();
	boolean removing = false;

	public HierarchicalIcons(final ModeController modeController) {
		super(modeController);
		modeController.getMapController().getReadManager().addReadCompletionListener(this);
		modeController.getMapController().addNodeChangeListener(this);
		modeController.getMapController().addMapChangeListener(this);
	}

	@Override
	protected void add(final NodeModel node, final IExtension extension) {
		gatherLeavesAndSetStyle(node);
		gatherLeavesAndSetParentsStyle(node);
		super.add(node, extension);
	}

	/**
	 */
	private void addAccumulatedIconsToTreeSet(final NodeModel child, final TreeSet<MindIcon> iconSet, final TreeSet<MindIcon> childsTreeSet) {
		for (final MindIcon icon : child.getIcons()) {
			iconSet.add(icon);
		}
		if (childsTreeSet == null) {
			return;
		}
		for (final MindIcon icon : childsTreeSet) {
			iconSet.add(icon);
		}
	}

	@Override
	protected IExtension createExtension(final NodeModel node, final XMLElement element) {
		return this;
	}

	/**
	 */
	private void gatherLeavesAndSetParentsStyle(final NodeModel node) {
		if (node.getChildCount() == 0) {
			if (node.getParentNode() != null) {
				setStyleRecursive(node.getParentNode());
			}
			return;
		}
		final ListIterator<NodeModel> childrenUnfolded = getModeController().getMapController().childrenUnfolded(node);
		while(childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			gatherLeavesAndSetParentsStyle(child);
		}
	}

	/**
	 */
	private void gatherLeavesAndSetStyle(final NodeModel node) {
		if (node.getChildCount() == 0) {
			setStyle(node);
			return;
		}
		final ListIterator<NodeModel> childrenUnfolded = getModeController().getMapController().childrenUnfolded(node);
		while(childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			gatherLeavesAndSetStyle(child);
		}
	}

	public void mapChanged(final MapChangeEvent event) {
		// TODO Auto-generated method stub
	}

	public void nodeChanged(final NodeChangeEvent event) {
		final NodeModel node = event.getNode();
		if (!isActive(node)) {
			return;
		}
		setStyle(node);
		if (!event.getProperty().equals("icon")) {
			return;
		}
		onUpdateChildren(node);
	}

	public void onNodeDeleted(final NodeModel parent, final NodeModel child, final int index) {
		if (!isActive(parent)) {
			return;
		}
		setStyleRecursive(parent);
	}

	public void onNodeInserted(final NodeModel parent, final NodeModel child, final int newIndex) {
		if (!isActive(parent)) {
			return;
		}
		setStyleRecursive(child);
	}

	public void onNodeMoved(final NodeModel oldParent, final int oldIndex, final NodeModel newParent,
	                        final NodeModel child, final int newIndex) {
		if (!isActive(newParent)) {
			return;
		}
		setStyleRecursive(oldParent);
		setStyleRecursive(child);
	}

	public void onPreNodeDelete(final NodeModel parent, final NodeModel child, final int index) {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.extensions.PermanentNodeHook#onUpdateChildrenHook(freeplane.modes
	 * .MindMapNode)
	 */
	private void onUpdateChildren(final NodeModel updatedNode) {
		setStyleRecursive(updatedNode);
	}

	public void readingCompleted(final NodeModel topNode, final HashMap<String, String> newIds) {
		if (!topNode.containsExtension(getClass()) && !topNode.getMap().getRootNode().containsExtension(getClass())) {
			return;
		}
		gatherLeavesAndSetStyle(topNode);
		gatherLeavesAndSetParentsStyle(topNode);
	}

	@Override
	protected void remove(final NodeModel node, final IExtension extension) {
		nodeIconSets.clear();
		removeIcons(node);
		super.remove(node, extension);
	}

	/**
	 */
	private void removeIcons(final NodeModel node) {
		removing = true;
		node.removeStateIcons(getHookName());
		getModeController().getMapController().nodeRefresh(node);
		final ListIterator<NodeModel> childrenUnfolded = getModeController().getMapController().childrenUnfolded(node);
		while(childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			removeIcons(child);
		}
		removing = false;
	}

	private void setStyle(final NodeModel node) {
		final TreeSet<MindIcon> iconSet = new TreeSet<MindIcon>();
		final ListIterator<NodeModel> childrenUnfolded = getModeController().getMapController().childrenUnfolded(node);
		while(childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			addAccumulatedIconsToTreeSet(child, iconSet, nodeIconSets.get(child));
		}
		for(MindIcon icon : IconController.getIcons(getModeController(), node)) {
			iconSet.remove(icon);
		}
		boolean dirty = true;
		if (nodeIconSets.containsKey(node)) {
			final TreeSet<MindIcon> storedIconSet = nodeIconSets.get(node);
			if (storedIconSet.equals(iconSet)) {
				dirty = false;
			}
		}
		nodeIconSets.put(node, iconSet);
		if (dirty) {
			if (!removing && iconSet.size() > 0) {
				Set<UIIcon> zoomedIcons = new TreeSet<UIIcon>();
				for (MindIcon icon : iconSet) {
					zoomedIcons.add(new ZoomedIcon(icon, 0.75f));
			    }
				node.setStateIcon(getHookName(), new UIIconSet(zoomedIcons));
			}
			else {
				node.removeStateIcons(getHookName());
			}
			getModeController().getMapController().nodeRefresh(node);
		}
	}

	/**
	 */
	private void setStyleRecursive(final NodeModel node) {
		setStyle(node);
		if (node.getParentNode() != null) {
			setStyleRecursive(node.getParentNode());
		}
	}

	public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child, int newIndex) {
	    // TODO Auto-generated method stub
	    
    }
}
