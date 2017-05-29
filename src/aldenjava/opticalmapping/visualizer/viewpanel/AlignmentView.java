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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.vobject.VAlignment;

public class AlignmentView extends ViewPanel {

	private List<VAlignment> valignList = new ArrayList<VAlignment>();
	private String id;
public AlignmentView(OMView mainView) {
		super(mainView);
		autoSetSize();
	}
	@Override
	protected void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.ALIGNMENT, new ArrayList<String>());
	}
	@Override
	public void updateData() {
		super.updateData();
		setViewMolecule(id);
	}
	// VComponent Functions
	@Override
	public void setDNARatio(double dnaRatio)
	{
		for (VAlignment valign : valignList)
			valign.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}
	@Override
	public void setRatio(double ratio)
	{
		for (VAlignment valign : valignList)
			valign.setRatio(ratio);
		super.setRatio(ratio);
	}
	@Override
	public void autoSetSize() {
		Dimension d = new Dimension(0, 0);
		if (valignList != null)
			for (VAlignment valign : valignList)
			{
				if (valign.getSize().width > d.width)
					d.width = valign.getSize().width;
				d.height += valign.getSize().height + 60;
				
			}
		if (d.width == 0 || d.height == 0)
			this.setSize(OMView.blankPanelSize);
		else
		{
			d.width += objBorder.x * 2;
			d.height += objBorder.y * 2;
			this.setSize(d);
		}
		this.setPreferredSize(this.getSize());
	}
	@Override
	public void reorganize() {
		int x = objBorder.x;
		int y = objBorder.y;
		for (VAlignment valign : valignList)
		{
			valign.setLocation(x, y);
			y += valign.getHeight() + 60;
		}
	}

	// Functions redirected from OMView
	@Override
	public void setRegion(GenomicPosNode region) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setViewMolecule(String id) {
		if (id == null)
			return;
		
		// Check if the id exists
		if (!mainView.dataModule.getResult(dataSelection.get(VDataType.ALIGNMENT)).containsKey(id)) {
			JOptionPane.showMessageDialog(mainView, "Molecule " + id + " cannot be found.");
			return;
		}
		
		this.id = id;
		for (VAlignment valign : valignList)
			this.remove(valign);
		valignList.clear();
		List<List<OptMapResultNode>> resultlistlist = mainView.dataModule.getResult(dataSelection.get(VDataType.ALIGNMENT)).get(id);
		List<VAlignment> valignList = new ArrayList<VAlignment>();
		for (List<OptMapResultNode> resultlist : resultlistlist)
		{
			boolean debugSeparateAlignment = false;
			if (debugSeparateAlignment)
				for (OptMapResultNode r : resultlist)
				{
					List<OptMapResultNode> tmpList = new ArrayList<OptMapResultNode>();
					tmpList.add(r);
					valignList.add(new VAlignment(mainView.dataModule.getAllReference().get(resultlist.get(0).mappedRegion.ref), tmpList));
				}
			else
				valignList.add(new VAlignment(mainView.dataModule.getAllReference().get(resultlist.get(0).mappedRegion.ref), resultlist));
		}
		int totalPartialMap = 0;
		for (VAlignment valign : valignList)
			totalPartialMap += valign.getPartialMapCount();
		this.updateInfo(String.format("Molecule: %s\nAligned Regions: %d\nPartial Map Count: %d", id, valignList.size(), totalPartialMap));
		for (VAlignment valign : valignList)
			this.add(valign);
		this.valignList = valignList; 
		this.autoSetSize();
		this.reorganize();
	}
	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected JMenuItem getGotoMenu() {
		JMenuItem gotoPage = new JMenuItem("Goto...");
		gotoPage.setEnabled(false);
		return gotoPage;
	}

}
