/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.4 -- March 10, 2018
**  
**  Copyright (C) 2018 by Alden Leung, Ting-Fung Chan, All rights reserved.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JMenuItem;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;

public abstract class ViewPanel extends ScrollablePanel {

	protected LinkedHashMap<VDataType, List<String>> dataSelection;
	
	public ViewPanel(OMView mainView) {
		super(mainView);
		initializeDataSelection();
		assert dataSelection != null;
		updateMenu();
	}

	protected abstract void initializeDataSelection();
	
	// Redirection Functions from OMView
	public abstract void setRegion(GenomicPosNode region) throws UnsupportedOperationException;
	public abstract void setViewMolecule(String id) throws UnsupportedOperationException;
	public abstract void setAnchorPoint(GenomicPosNode anchorPoint) throws UnsupportedOperationException;

	public void updateData() {
	}


	private boolean isSameSelection(LinkedHashMap<VDataType, List<String>> oldDataSelection, LinkedHashMap<VDataType, List<String>> newDataSelection) {
		if (oldDataSelection == null || newDataSelection == null)
			return false;
		if (!(oldDataSelection.keySet().containsAll(newDataSelection.keySet()) && oldDataSelection.keySet().size() == newDataSelection.size()))
			return false;
		for (VDataType type : oldDataSelection.keySet()) {
			List<String> oldData = oldDataSelection.get(type);
			List<String> newData = newDataSelection.get(type);
			if (!(newData.containsAll(oldData) && newData.size() == oldData.size()))
				return false;
		}
		return true;		
	}
	
	public void updateDataSelection(LinkedHashMap<VDataType, List<String>> newDataSelection) {
		if (newDataSelection == null)
			return;
		if (!isSameSelection(this.dataSelection, newDataSelection)) {
			dataSelection = newDataSelection;
			updateData();
		}
	}
	public void updateDataSelection() {
		if (dataSelection.isEmpty())
			return;
		updateDataSelection(mainView.dataModule.retainValidSelection(dataSelection));
	}
	

	// Update Title, Info, Menu
	@Override
	protected void updateMenu() {
		super.updateMenu();
		menu.addSeparator();
		JMenuItem selectDataItem = new JMenuItem("Select Data");
		selectDataItem.setMnemonic('D');
		selectDataItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LinkedHashMap<VDataType, List<String>> newDataSelection = mainView.dataModule.getDataSelection(new ArrayList<VDataType>(ViewPanel.this.dataSelection.keySet()), ViewPanel.this.dataSelection);
				ViewPanel.this.updateDataSelection(newDataSelection);
			}
		});
		menu.add(selectDataItem);
	}

	protected void updateInfo(String newInfo) {
		if (newInfo != null) {
			this.firePropertyChange("PanelInformation", "", newInfo);
			information = newInfo;
		}
	}

	public boolean isLoadingCompleted() {
		return true;
	}
	public void waitLoadingComplete() {
	}
}

