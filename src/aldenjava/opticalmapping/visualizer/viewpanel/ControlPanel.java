/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.2 -- January 1, 2017
**  
**  Copyright (C) 2017 by Alden Leung, Ting-Fung Chan, All rights reserved.
**  Contact:  alden.leung@gmail.com, tf.chan@cuhk.edu.hk
**  Organization:  School of Life Sciences, The Chinese University of Hong Kong,
**                 Shatin, NT, Hong Kong SAR
**  
**  This file is part of OMTools.
**  
**  OMTools is free software; you can redistribute it and/or 
**  modify it under the terms of the GNU General Public License 
**  as published by the Free Software Foundation; either version 
**  3 of the License, or (at your option) any later version.
**  
**  OMTools is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**  
**  You should have received a copy of the GNU General Public 
**  License along with OMTools; if not, see 
**  <http://www.gnu.org/licenses/>.
**************************************************************************/


package aldenjava.opticalmapping.visualizer.viewpanel;

import java.awt.Adjustable;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;

import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.MultiSplitLayout.Divider;
import org.jdesktop.swingx.MultiSplitLayout.Leaf;
import org.jdesktop.swingx.MultiSplitLayout.Node;
import org.jdesktop.swingx.MultiSplitLayout.Split;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VComponent;
import aldenjava.opticalmapping.visualizer.VTab;
import aldenjava.opticalmapping.visualizer.viewpanel.annotation.AnnotationPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.annotation.InformationPanel;

public abstract class ControlPanel extends VComponent implements AdjustmentListener, PropertyChangeListener {

	// control panel has to synchronize all viewPanels
	protected OMView mainView;
	protected List<ViewPanel> scrollablePanelList = new ArrayList<ViewPanel>();
	protected List<JScrollPane> scrollPaneList = new ArrayList<JScrollPane>();
	protected InformationPanel infoPanel = new InformationPanel();
	protected GenomicPosNode region;
	protected GenomicPosNode anchorPoint;
	protected String title;
	protected int orgHorizontal = 0;
	private JXMultiSplitPane multiSplitPane;
	public ControlPanel(OMView mainView) {
		this.mainView = mainView;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		multiSplitPane = new JXMultiSplitPane();
		this.add(multiSplitPane);
		initializeTitle();
	}

	// VComponent functions
	@Override
	public void setDNARatio(double dnaRatio) {
		for (ScrollablePanel scrollablePanel : scrollablePanelList)
			scrollablePanel.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		for (ScrollablePanel scrollablePanel : scrollablePanelList)
			scrollablePanel.setRatio(ratio);
		super.setRatio(ratio);
	}

	@Override
	public void autoSetSize() {
		//		for (ViewPanel viewPanel : viewPanelList)
		//			viewPanel.autoSetSize();
	}

	@Override
	public void reorganize() {
		for (ScrollablePanel scrollablePanel : scrollablePanelList)
			scrollablePanel.reorganize();
	}

	// Listener	
	@Override
	public void adjustmentValueChanged(AdjustmentEvent evt) {
		Adjustable source = evt.getAdjustable();

		int orient = source.getOrientation();
		if (orient == Adjustable.HORIZONTAL)
			this.setViewPosition(new Point(evt.getAdjustable().getValue(), 0));
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equalsIgnoreCase("ViewRatio")) {
			double ratio = (double) e.getNewValue();
			this.setRatio(ratio);
		}
		if (e.getPropertyName().equalsIgnoreCase("ViewPosition")) {
			Point p = (Point) e.getNewValue();
//			System.out.println("P:" + p.x);
			for (ScrollablePanel scrollablePanel : scrollablePanelList) {
				Container c = scrollablePanel.getParent();
				if (c instanceof JViewport) {
					JViewport jv = (JViewport) c;
					jv.setViewPosition(new Point(p.x, jv.getViewPosition().y));
//					System.out.println("AfterSet: " + jv.getViewPosition().x);
				}
			}
		}
		if (e.getPropertyName().equalsIgnoreCase("PanelClose")) {
			if ((boolean) e.getNewValue()) {
				this.removePanel((ScrollablePanel) e.getSource());
			}
		}
		if (e.getPropertyName().equalsIgnoreCase("PanelInformation")) {
			infoPanel.setInformation((String) e.getNewValue());
		}
		if (e.getPropertyName().equalsIgnoreCase("PanelTitle")) {
			JTabbedPane tabPanel = (JTabbedPane) this.getParent();
			((VTab) tabPanel.getTabComponentAt(tabPanel.indexOfComponent(this))).setTitle((String) e.getNewValue());
		}

	}

	// View Position
	protected Point getCurrentViewPosition() {
		for (JScrollPane scrollPane : scrollPaneList)
			if (scrollPane.getParent() != null)
				return scrollPane.getViewport().getViewPosition();
		return null;
	}

	protected void setViewPosition(Point p) {
		for (JScrollPane scrollPane : scrollPaneList)
			scrollPane.getViewport().setViewPosition(new Point(p.x, scrollPane.getViewport().getViewPosition().y));

	}

	// Create Panel
	protected JScrollPane createScrollPane(ScrollablePanel scrollPanel) {
		JScrollPane scrollPane = new JScrollPane(scrollPanel);
		scrollPane.setAutoscrolls(true);
		scrollPane.getHorizontalScrollBar().addAdjustmentListener(this);
		return scrollPane;
	}

	public abstract void createViewPanel();

	public void createViewPanel(ViewPanel... viewPanels) {
		for (ViewPanel viewPanel : viewPanels) {
			if (region != null)
				viewPanel.setRegion(region);
			if (anchorPoint != null)
				viewPanel.setAnchorPoint(anchorPoint);
			
			viewPanel.setRatio(ratio);
			viewPanel.setDNARatio(dnaRatio);
			// molecule id is not set
			viewPanel.addPropertyChangeListener(this);
			scrollablePanelList.add(viewPanel);
			scrollPaneList.add(createScrollPane(viewPanel));
		}
		this.reorganizePanel();
	}

	public abstract void createAnnotationPanel();

	public void createAnnotationPanel(AnnotationPanel annoPanel) {
		annoPanel.setRegion(region);
		annoPanel.addPropertyChangeListener(this);
		scrollablePanelList.add(annoPanel);
		scrollPaneList.add(createScrollPane(annoPanel));
		this.reorganizePanel();
	}

	public void removePanel(ScrollablePanel scrollPanel) {
		for (int i = 0; i < scrollablePanelList.size(); i++) {
			ScrollablePanel scrollablePanel = scrollablePanelList.get(i);
			if (scrollablePanel == scrollPanel) {
				scrollablePanelList.remove(scrollablePanel);
				break;
			}
		}
		for (int i = 0; i < scrollPaneList.size(); i++) {
			JScrollPane scrollPane = scrollPaneList.get(i);
			if (scrollPane.getViewport().getView() == scrollPanel) {
				this.remove(scrollPane);
				scrollPaneList.remove(i);
			}
		}
		if (scrollPaneList.size() == 0)
			createViewPanel();
		this.reorganizePanel();
	}

	private void sortScrollPane() {
		List<JScrollPane> tempViewScrollPane = new ArrayList<JScrollPane>();
		List<JScrollPane> tempAnnoScrollPane = new ArrayList<JScrollPane>();
		List<ViewPanel> tempViewScrollablePanel = new ArrayList<ViewPanel>();
		List<ViewPanel> tempAnnoScrollablePanel = new ArrayList<ViewPanel>();
		for (int i = 0; i < scrollablePanelList.size(); i++) {
			ViewPanel panel = scrollablePanelList.get(i);
			JScrollPane scrollPane = scrollPaneList.get(i);
			if (panel instanceof AnnotationPanel) {
				tempAnnoScrollPane.add(scrollPane);
				tempAnnoScrollablePanel.add(panel);
			}
			else {
				tempViewScrollPane.add(scrollPane);
				tempViewScrollablePanel.add(panel);
			}
		}
		scrollablePanelList.clear();
		scrollablePanelList.addAll(tempAnnoScrollablePanel);
		scrollablePanelList.addAll(tempViewScrollablePanel);
		scrollPaneList.clear();
		scrollPaneList.addAll(tempAnnoScrollPane);
		scrollPaneList.addAll(tempViewScrollPane);
		
		
//		for (JScrollPane scrollPane : tempAnnoScrollPane)
//			scrollPaneList.add(scrollPane);
//		for (JScrollPane scrollPane : tempViewScrollPane)
//			scrollPaneList.add(scrollPane);
		
	}

	public void saveImage() {
		if (scrollablePanelList.size() == 0)
			JOptionPane.showMessageDialog(mainView, "No panel is open. No image is saved. ");
		else {
			JComponent[] comp = new JComponent[scrollablePanelList.size()];
			int index = 0;
			for (ScrollablePanel panel : scrollablePanelList)
				comp[index++] = panel;
			mainView.saveImage(comp);
		}
	}
	public void saveImage(String path) {
		if (scrollablePanelList.size() == 0)
			return;
		else {
			JComponent[] comp = new JComponent[scrollablePanelList.size()];
			int index = 0;
			for (ViewPanel panel : scrollablePanelList) {
				while (!panel.isLoadingCompleted()) {
					try {
						Thread.sleep(1); // Wait till the loading complete
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				};
				comp[index++] = panel;
			}
			mainView.saveImage(path, comp);
		}
	}
	public void reorganizePanel() {
		// sort
		sortScrollPane();
		Point savedViewPosition = getCurrentViewPosition();
		List<Node> children = new ArrayList<Node>();
		double infoPanelWeight = 0;
		double weight = 0;
		for (int i = 0; i < scrollPaneList.size() + 1; i++) {
			Leaf leaf = new Leaf(Integer.toString(i));
			if (i > 0) {
				children.add(new Divider());
				leaf.setWeight((1 - infoPanelWeight) / (double) scrollPaneList.size());
			} else
				leaf.setWeight(infoPanelWeight);
			weight += leaf.getWeight();
			children.add(leaf);
		}
		// dealing with leaf weight Bug		

		// Have at least two panels
		if (scrollPaneList.size() == 0) {
			Leaf leaf = new Leaf(Integer.toString(1));
			children.add(new Divider());
			leaf.setWeight(1 - infoPanelWeight);
			children.add(leaf);
		} else
			children.get(0).setWeight(1 - weight + infoPanelWeight);

		Split modelRoot = new Split();
		modelRoot.setRowLayout(false);
		modelRoot.setChildren(children);

		multiSplitPane.removeAll();
		multiSplitPane.setModel(modelRoot);

		for (int i = 0; i < scrollPaneList.size() + 1; i++)
			if (i == 0) {
				multiSplitPane.add(infoPanel, 0);
				// Currently infoPanel size is zero
			} else
				multiSplitPane.add(scrollPaneList.get(i - 1), Integer.toString(i));
		// An additional empty panel
		if (scrollPaneList.size() == 0)
			multiSplitPane.add(new JPanel(), 1);

		if (savedViewPosition != null)
			this.setViewPosition(savedViewPosition);
	}

	// Redirect Functions from OMView
	public void setRegion(GenomicPosNode region) {
		this.region = region;
		for (ViewPanel scrollablePanel : scrollablePanelList)
			scrollablePanel.setRegion(region);
	}

	public void setViewMolecule(String id) {
		for (ViewPanel scrollablePanel : scrollablePanelList)
			scrollablePanel.setViewMolecule(id);
	}

	public void setAnchorPoint(GenomicPosNode anchorPoint) {
		this.anchorPoint = anchorPoint;
		for (ViewPanel scrollablePanel : scrollablePanelList)
			scrollablePanel.setAnchorPoint(anchorPoint);
	}

	public void updateData() {
		for (ViewPanel scrollablePanel : scrollablePanelList)
			scrollablePanel.updateData();
	}
	public void updateDataSelection() {
		for (ViewPanel scrollablePanel : scrollablePanelList)
			scrollablePanel.updateDataSelection();
	}

	protected void updateTitle(String title) {
		this.title = title;
		JTabbedPane tabPanel = (JTabbedPane) this.getParent();
		((VTab) tabPanel.getTabComponentAt(tabPanel.indexOfComponent(this))).setTitle(title);
	};
	public abstract void initializeTitle();
	public String getTitle() {
		return title;
	}
	public GenomicPosNode getRegion() {
		return region;
	}

	public GenomicPosNode getAnchorPoint() {
		return anchorPoint;
	}

}

//public ViewPanel getViewPanel(int index)
//{
//	return viewPanelList.get(index);
//}

