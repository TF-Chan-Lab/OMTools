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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.miscellaneous.InvalidVObjectException;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.vobject.VCoverage;
import aldenjava.opticalmapping.visualizer.vobject.VMapMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VReference;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;

public class RegionalView extends ViewPanel {

	public static boolean showUnmap = false;

	private VReference vref = null;
	private VRuler ruler;
	private VCoverage vcov;
	private List<VMapMolecule> moleculelist = new ArrayList<VMapMolecule>();
	private int lineNo = 0;
	private MoleculeCreater moleCreater = null;
	private int sortingMethod = 0;
	private long vrefDisplace = 0;
	private LinkedHashMap<String, Point> nameLocations = new LinkedHashMap<>();

	public RegionalView(OMView mainView) {
		super(mainView);
		// Initialize title
		title = "Regional View";
		// Initialize ruler
		ruler = new VRuler();
		ruler.setVisible(true);
		add(ruler);
		
//		vcov = new VCoverage();
//		vcov.setVisible(true);
//		add(vcov);
		
		this.autoSetSize();
	}

	
	// Initialization
	@Override
	protected void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.ALIGNMENT, new ArrayList<String>());
	}
	@Override
	public void updateData() {
		super.updateData();
		this.buildMolecules();
	}

	// VComponent functions
	@Override
	public void setDNARatio(double dnaRatio) {
		if (!(vref == null || ruler == null || moleculelist == null)) {
			vref.setDNARatio(dnaRatio);
			ruler.setDNARatio(dnaRatio);
			for (VMapMolecule molecule : moleculelist)
				molecule.setDNARatio(dnaRatio);
		}
		super.setDNARatio(dnaRatio);
	}
	@Override
	public void setRatio(double ratio) {
		if (!(vref == null || ruler == null || moleculelist == null)) {
			vref.setRatio(ratio);
			ruler.setRatio(ratio);
			for (VMapMolecule molecule : moleculelist)
				molecule.setRatio(ratio);
		}
		super.setRatio(ratio);
	}
	@Override
	public void autoSetSize() {
		if (region == null)
			this.setSize(OMView.blankPanelSize);
		else
		{
			int heightOfRulerAndRef = (int) (objBorder.y + ruler.getHeight() + (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio);
			int height = (int) ((ViewSetting.moleculeSpace + ViewSetting.bodyHeight * (RegionalView.showUnmap?2:1)) * ratio);

			if (vref == null)
				height = mainView.getHeight();
			else
				height = (int) (objBorder.y * 2 + heightOfRulerAndRef + (lineNo * height));
			setSize((int) (region.length() / dnaRatio  * ratio + objBorder.x * 2), height);
		}
		setPreferredSize(getSize());
	}
	@Override
	public void reorganize()
	{	
		if (region == null || vref == null)
			return;
		switch (sortingMethod)
		{
			case 0: 
				sortNormal(objBorder.x, objBorder.y + ruler.getHeight());
				break;
			default:;
		}
		ruler.setLocation(objBorder.x, objBorder.y);
		
		vref.setLocation(objBorder.x + (int) (vrefDisplace * ratio / dnaRatio), (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio));
		int totalMolecules = 0;
		int totalVisibleMolecules = 0;
		for (VMapMolecule vmm : moleculelist)
		{
			totalMolecules++;
			if (vmm.isVisible())
				totalVisibleMolecules++;
		}
//		updateInfo(String.format("Region: %s\nMolecules Loaded: %d\nMolecule in Region: %d\nVisible Molecules: %d\n", region.toString(), mainView.dataModule.getResult(dataSelection.get(VDataType.ALIGNMENT)).size(), totalMolecules, totalVisibleMolecules)); 
	}	
	private void sortNormal(int leftMargin, int topMargin) {
		nameLocations = new LinkedHashMap<>();
		List<Long> lineEndPos = new ArrayList<Long>();
		int heightOfRulerAndRef = topMargin + (int) ((ViewSetting.moleculeSpace * 2 + ViewSetting.bodyHeight) * ratio);
		int height = (int) ((ViewSetting.moleculeSpace + ViewSetting.bodyHeight * (RegionalView.showUnmap?2:1)) * ratio);
		for (VMapMolecule vmm : moleculelist)
		{
			if (vmm.getMappedPos().overlapSize(region) > 0)
			{
				long displacement = vmm.getMappedPos().start - region.start;
				
				int x = (int) (displacement / dnaRatio * ratio) + leftMargin;
				int y = -1;
				for (int i = 0; i < lineEndPos.size(); i++)
					if (displacement - lineEndPos.get(i) >= 1500)
					{
						y = (i) * height;
						lineEndPos.set(i, displacement + vmm.getSpaceDNALength());
						break;
					}
				if (y == -1)
				{
					lineEndPos.add(displacement + vmm.getSpaceDNALength());
					y = (lineEndPos.size() - 1) * height;
				}
				
				y += heightOfRulerAndRef;
				vmm.setLocation(x, y);
				vmm.autoSetSize();
				vmm.reorganize();
				vmm.setVisible(true);
				nameLocations.put(vmm.getID(), new Point(x, y));
			}
			else
				vmm.setVisible(false);
		}
		lineNo = lineEndPos.size();
	}
	@Override
	protected JMenuItem getGotoMenu() {
		JMenuItem gotoPage = new JMenuItem("Goto...");
		gotoPage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input molecule:");
				if (ans != null) {
					if (nameLocations.containsKey(ans))
						navigateViewPortFromGoto(nameLocations.get(ans));
					else
						JOptionPane.showMessageDialog(mainView, "Alignment of molecule " + ans + " is not found in this region.");
				}
			}
		});
		return gotoPage;	
	}
	// Build molecules
	private void buildMolecules() {
		// Stop previous building process
		if ((moleCreater != null && !moleCreater.isDone())) {
			moleCreater.cancel(true);
		}
		if (region == null)
			return;

		// Update the reference, ruler
		// reference
		if (vref != null)
			this.remove(vref);
		DataNode ref = mainView.dataModule.getAllReference().get(region.ref);
		vref = new VReference(ref);
		long start = region.start;
		long stop = region.stop;
		if (start < 1) {
			vrefDisplace = 1 - start;
			start = 1;
		}
		else
			vrefDisplace = 0;
		if (start > ref.size)
			start = ref.size;
		if (stop < 1)
			stop = 1;
		if (stop > ref.size)
			stop = ref.size;
		vref.setStartEndPoint(new SimpleLongLocation(start, stop));
		vref.addMouseListener(this);
		this.add(vref);
		// change ruler
		ruler.setStartEndPoint(region.getLoc());
				
		// final ratio changes
		setRatio(ratio);
		setDNARatio(dnaRatio);

		// Start to load molecules
		moleCreater = new MoleculeCreater(mainView.dataModule.getAllReference(), mainView.dataModule.getResult(dataSelection.get(VDataType.ALIGNMENT)), region, this);
		moleCreater.addPropertyChangeListener(mainView.statusPanel);
		moleCreater.execute();
	}
	
	public void addMolecules(List<VMapMolecule> moleculelist) {
		this.clearMolecules();
		this.moleculelist = moleculelist;
		for (VMapMolecule vmm : moleculelist) {
			vmm.setVisible(false);
			this.add(vmm);
			vmm.addMouseListener(this);
		}
		setRatio(ratio);
		setDNARatio(dnaRatio);
		moleCreater = null;
	}
	
	private void clearMolecules() {
		for (VMapMolecule vmm : moleculelist) {
			this.remove(vmm);
			vmm.removeMouseListener(this);
		}
		this.moleculelist = new ArrayList<VMapMolecule>();
	}

	// Listeners
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof VMapMolecule)
		{			
			if (e.isPopupTrigger()) {
				
			}
			VMapMolecule vmm = (VMapMolecule) e.getSource();
			mainView.createAlignmentViewTab(dataSelection).setViewMolecule(vmm.getID());			
		}	
//		if (e.getSource() instanceof VReference)
//			JOptionPane.showMessageDialog(this, ((VReference) e.getSource()).getInfo());
		if (e.getSource() instanceof VReference)
		{
			VReference vref = (VReference) e.getSource();
			int signal = vref.findClosestSignal(e.getX());
			DataNode ref = mainView.dataModule.getAllReference().get(vref.getName());
			if (signal != -1) {
				AnchorControlPanel controlPanel = mainView.createAnchorViewTab(dataSelection);
				controlPanel.setAnchorPoint(new GenomicPosNode(ref.name, ref.refp[signal]));
			}
//				DataNode ref = vref.data;
//				String refname = ref.name;
//				long pos = ref.refp[vsig.getSignalNo()];
//				RegionalView rView = new RegionalView(mainView);
//				mainView.createRegionalViewTab(rView);				
//				rView.loadReference(optrefmap);
//				rView.loadProcessedResult(resultlistmap);
//				rView.loadResult(mainView.dataModule.getAllResult());
//				GenomicPosNode region = new GenomicPosNode(refname, pos, pos);
//				rView.setRegion(region);
//				rView.setSortingSignal(ref.name, vsig.getSignalNo());
				
				// Normal
//					RegionalView rView = mainView.createSortRegionalViewTab(region, selectedResult);
//					rView.setSortingSignal(ref.name, vsig.getSignalNo());
				// ENd normal
//				GenomicPosNode targetRegion = new GenomicPosNode(region.ref, ref.refp[vsig.getSignalNo()]);
//				AnchorView aView = mainView.createAnchorViewTab(region, selectedResult);
//				aView.setAnchorPoint(targetRegion);
//				aView.createMolecules();
//			}

		/*
		if (e.getSource() instanceof VSignal)
		{
			VSignal vsig = (VSignal) e.getSource();
			if (vsig.getParent() instanceof VReference)
			{
				VReference vref = (VReference) vsig.getParent();
				DataNode ref = vref.data;
//				String refname = ref.name;
//				long pos = ref.refp[vsig.getSignalNo()];
//				RegionalView rView = new RegionalView(mainView);
//				mainView.createRegionalViewTab(rView);				
//				rView.loadReference(optrefmap);
//				rView.loadProcessedResult(resultlistmap);
//				rView.loadResult(mainView.dataModule.getAllResult());
//				GenomicPosNode region = new GenomicPosNode(refname, pos, pos);
//				rView.setRegion(region);
//				rView.setSortingSignal(ref.name, vsig.getSignalNo());
				
				// Normal
//					RegionalView rView = mainView.createSortRegionalViewTab(region, selectedResult);
//					rView.setSortingSignal(ref.name, vsig.getSignalNo());
				// ENd normal
				GenomicPosNode targetRegion = new GenomicPosNode(region.ref, ref.refp[vsig.getSignalNo()]);
				AnchorView aView = mainView.createAnchorViewTab(region, selectedResult);
				aView.setAnchorPoint(targetRegion);
//				aView.createMolecules();
			}*/
		}
	}
	
	@Override
	public void setRegion(GenomicPosNode region) {
		//
		if (region == null) {
			autoSetSize();
			return;
		}
		// Do not proceed if the region isn't updated
		if (this.region != null && this.region.equals(region))
			return;
		
		this.region = region;
		
		this.buildMolecules();
	}
	
	
	@Override
	public void setViewMolecule(String id) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLoadingCompleted() {
		return moleCreater == null;
	}


	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);		
		Graphics2D g = (Graphics2D) graphics;
		if (vref == null || ruler == null || moleculelist == null)
			return;
		
		// Draw the query names
		// Determine the font size
		Font font; 
		int fsize = 1;
		int h = (int) (ViewSetting.maMoleculeSpace * ratio);
		while (true) {
			font = new Font("Arial", Font.PLAIN, fsize + 1);
			int testHeight = getFontMetrics(font).getHeight();
			if (testHeight > h)
				break;
			fsize++;
		}
		font = new Font("Arial", Font.PLAIN, fsize);
		g.setFont(font);
		g.setPaint(ViewSetting.queryNameColor);
		
		if (ViewSetting.displayQueryName)
			nameLocations.forEach((name,point) -> g.drawString(name, point.x, point.y));

	}

}


class MoleculeCreater extends SwingWorker<List<VMapMolecule>, Void> {

	private LinkedHashMap<String, DataNode> optrefmap;
	private LinkedHashMap<String, List<List<OptMapResultNode>>> resultlistlistmap;
	private GenomicPosNode region;
	private RegionalView viewPanel; // For registration of listener
	public MoleculeCreater(LinkedHashMap<String, DataNode> optrefmap, LinkedHashMap<String, List<List<OptMapResultNode>>> resultlistlistmap, GenomicPosNode region, RegionalView viewPanel)
	{
		this.optrefmap = optrefmap;
		this.resultlistlistmap = resultlistlistmap;
		this.region = region;
		this.viewPanel = viewPanel;
	}
	@Override
	protected List<VMapMolecule> doInBackground() {
		List<VMapMolecule> moleculelist = new ArrayList<VMapMolecule>();
		if (resultlistlistmap == null)
			return moleculelist;
		for (List<List<OptMapResultNode>> resultlistlist : resultlistlistmap.values())
		{
			List<GenomicPosNode> regionList = new ArrayList<GenomicPosNode>();
			for (List<OptMapResultNode> resultlist : resultlistlist)
			{
				String ref = resultlist.get(0).mappedRegion.ref;
				long mappedstart = Long.MAX_VALUE;
				long mappedstop = Long.MIN_VALUE;
				for (OptMapResultNode result : resultlist)
				{
					if (result.mappedRegion.start < mappedstart)
						mappedstart = result.mappedRegion.start;
					if (result.mappedRegion.stop > mappedstop)
						mappedstop = result.mappedRegion.stop;
				}
				regionList.add(new GenomicPosNode(ref, mappedstart, mappedstop));
			}
			
			for (int i = 0; i < resultlistlist.size(); i++)
			{
				if (regionList.get(i).isClose(this.region, 0))
				{
					GenomicPosNode region1 = null;
					GenomicPosNode region2 = null;
					if (i != 0) 
						region1 = regionList.get(i - 1);
					if (i != resultlistlist.size() - 1)
						region2 = regionList.get(i + 1);
					VMapMolecule vmm;
					try {
						vmm = new VMapMolecule(optrefmap.get(region.ref), resultlistlist.get(i));
						vmm.setVisible(false);
						vmm.setTranslocation(region1, region2);
						moleculelist.add(vmm);
					} catch (InvalidVObjectException e) {
						System.err.println(e.getMessage());
						System.err.printf("%s is not created.\n", resultlistlist.get(i).get(0).parentFrag.name);
					}

				}
			}
		}
		Collections.sort(moleculelist);
		return moleculelist;
	}
	@Override
	protected void done()
	{
		List<VMapMolecule> moleculelist = null;
		if (!isCancelled())
			try {
				moleculelist = get();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(viewPanel, "Error occurs in creating molecules.");
				e.printStackTrace();
			}
//		else
//			JOptionPane.showMessageDialog(RegionalView.this, "Cancelled.");
		if (moleculelist != null)
			viewPanel.addMolecules(moleculelist);
	}
}


