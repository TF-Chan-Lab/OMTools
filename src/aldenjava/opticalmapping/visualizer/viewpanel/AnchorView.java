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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.miscellaneous.InvalidVObjectException;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.vobject.VMapMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VReference;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;

public class AnchorView extends ViewPanel {

	VReference headvref;
	VRuler ruler;
	private List<VMolecule> vrefList = new ArrayList<VMolecule>();
	private List<Long> displaceList = new ArrayList<Long>();
	HashSet<String> idarray = null;
	private GenomicPosNode targetSignal;
	private long vrefDisplace = 0;
	private LinkedHashMap<String, Point> nameLocations = new LinkedHashMap<>();
	
	
	public AnchorView(OMView mainView) {
		super(mainView);
		title = "Anchor View";
		// Initialize header
		this.ruler = new VRuler();
		ruler.setVisible(true);
		ruler.setLocation(objBorder.x, 20);
		this.add(ruler);
		autoSetSize();
	}

	@Override
	protected void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.ALIGNMENT, new ArrayList<String>());
	}

	// VComponent functions
	@Override
	public void setDNARatio(double dnaRatio) {
		if (!(headvref == null || ruler == null || vrefList == null)) {
			headvref.setDNARatio(dnaRatio);
			ruler.setDNARatio(dnaRatio);
			for (VMolecule vref : vrefList)
				vref.setDNARatio(dnaRatio);
		}
		super.setDNARatio(dnaRatio);
	}
	@Override
	public void setRatio(double ratio) {
		if (!(headvref == null || ruler == null || vrefList == null)) {
			headvref.setRatio(ratio);
			ruler.setRatio(ratio);
			for (VMolecule vref : vrefList)
				vref.setRatio(ratio);
		}
		super.setRatio(ratio);
	}

	@Override
	public void autoSetSize() {
		if (region == null || targetSignal == null)
			this.setSize(OMView.blankPanelSize);
		else
		{
			int height;
			int width;
			if (headvref == null)
			{
				height = mainView.getHeight();
				width = mainView.getWidth();
			}
			else
			{
				height = (int) (ruler.getHeight() + headvref.getHeight() + objBorder.y * 2
						+ (vrefList.size() + 1) * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight * (RegionalView.showUnmap?2:1)) * ratio);
				width = (int)(region.length() * ratio / dnaRatio) + objBorder.x * 2;
			}
			setSize(width, height);
		}
		setPreferredSize(getSize());
	}

	@Override
	public void reorganize() 
	{
		if (region == null || headvref == null)			
			return;
		ruler.setLocation(objBorder.x, objBorder.y);
		headvref.setLocation(objBorder.x + (int) (vrefDisplace * ratio / dnaRatio), (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio));
		int height = (int) ((ViewSetting.moleculeSpace + ViewSetting.bodyHeight * (RegionalView.showUnmap?2:1)) * ratio);
		int heightOfRulerAndRef = (int) (objBorder.y + ruler.getHeight() + (ViewSetting.moleculeSpace * 2 + ViewSetting.bodyHeight) * ratio);
		nameLocations.clear();
		for (int i = 0; i < vrefList.size(); i++)
		{
			
			vrefList.get(i).setLocation(objBorder.x + (int) ((displaceList.get(i) - region.start + 1) * ratio / dnaRatio), heightOfRulerAndRef + (int) ((height * (i))));
			
			nameLocations.put(vrefList.get(i).getName(), new Point(objBorder.x + (int) ((displaceList.get(i) - region.start + 1) * ratio / dnaRatio), heightOfRulerAndRef + (int) ((height * (i)))));
//			labelList.get(i).setLocation(objBorder.x + (int) (region.start * ratio / dnaRatio) - 80, objBorder.y + (int) ((((vrefList.get(i).getHeight() + 5) * (i + 1)) * ratio) - 10));
//			if (labelList5.get(i) != null)
//				labelList5.get(i).setLocation(objBorder.x + (int) (displaceList5.get(i) * ratio / dnaRatio), objBorder.y + (int) ((((vrefList.get(i).getHeight() + 5) * (i + 1)) * ratio)) + 5);
//			if (labelList3.get(i) != null)
//				labelList3.get(i).setLocation(objBorder.x + (int) (displaceList3.get(i) * ratio / dnaRatio), objBorder.y + (int) ((((vrefList.get(i).getHeight() + 5) * (i + 1)) * ratio)) + 5);
			
		}
	}

	@Override
	public void setRegion(GenomicPosNode region) 
	{
		if (region == null)
			return;
		this.region = region;
		construct();
//		this.createMolecules();
//		System.out.println(vrefList.size());
//		for (JLabel label : labelList)
//			this.remove(label);
//		labelList.clear();
//		
//		for (JLabel label : labelList5)
//			if (label != null)
//				this.remove(label);
//		for (JLabel label : labelList3)
//			if (label != null)
//				this.remove(label);
//		labelList5.clear();
//		labelList3.clear();
//		displaceList5.clear();
//		displaceList3.clear();
		
		
		
//		String[] tmptarget = {"ZW85-1", "AB307-0294", "178831", "D1279779", "ACICU", "ATCC_17978", "BJAB0715", "1656-2", "MDR-ZJ06", "BJAB07104", "BJAB0868", "MDR-TJ", "151155", "TCDC-AB0715", "AB0057", "AYE", "71853", "TYTH-1", "SDF"};
//		int count = 0;
//		for (String d : tmptarget)
//		{
//			String e = d + ".oma";
////			for (List<List<OptMapResultNode>> resultlistlist : mainView.dataModule.getResult(selectedResult).values())
//			for (List<List<OptMapResultNode>> resultlistlist : mainView.dataModule.getResult(e).values())
//			{
//				for (List<OptMapResultNode> resultList : resultlistlist)
//				{
//					boolean found = false;
//					for (OptMapResultNode result : resultList)
//					{
//						if (result.mappedRegion.overlapSize(region) > 0)
//						{
//							String folder = "C:\\Users\\Alden\\Desktop\\Projects\\OpticalMapping\\Scaffolding\\ABCrossAlignment2\\ATPaseBlast\\";
//							String filename = folder + d + ".blast";
//							GenomicPosNode region5 = null;
//							GenomicPosNode region3 = null;
//							
//							try {
//								BufferedReader br = new BufferedReader(new FileReader(filename));
//								String s; 
//								while ((s = br.readLine())!= null)
//								{
//									String[] l = s.split("\t");
//									long pos1 = Long.parseLong(l[8]);
//									long pos2 = Long.parseLong(l[9]);
//									if (s.startsWith("5_ATPase"))
//										region5 = new GenomicPosNode("ATCC", Math.min(pos1, pos2), Math.max(pos1, pos2));  
//									if (s.startsWith("3_ATPase"))
//										region3 = new GenomicPosNode("ATCC", Math.min(pos1, pos2), Math.max(pos1, pos2));
//									
//								}
//								if (d.equalsIgnoreCase("71853"))
////									region3 = new GenomicPosNode(region3.ref, region3.start - result.parentFrag.length() / 2, region3.stop - result.parentFrag.length() / 2);
//									region3 = null;
//								br.close();
//							} catch (IOException e1) {
//								// TODO Auto-generated catch block
//								e1.printStackTrace();
//							}
//
//							
//							
//							
//							found = true;
//							long displace;
//							DataNode ref = new DataNode(result.parentFrag);
////							FragmentNode f = new FragmentNode(result.parentFrag);
////							f.scale(1 / result.getMapScale());
//							result.scale(1 / result.getMapScale());
//							
//							ref.scale(1 / result.getMapScale());
//							boolean reversed = false;
//							if (result.mappedstrand == -1)
//							{
//								displace = headDisplace - result.getSignalDisplace(region.ref, targetSig) - result.parentFrag.length(result.subfragstart + 1, result.getTotalSegment() - 1);
//	//							System.out.println(result.parentFrag.id);
//	//							System.out.println(headDisplace);
//	//							System.out.println(result.getSignalDisplace(region.ref, targetSig));
//	//							System.out.println(result.parentFrag.length(result.subfragstart + 1, result.getTotalSegment() - 1));
//								ref.reverse();					
//								reversed = true;
//							}
//							else
//							{
//								displace = headDisplace - result.getSignalDisplace(region.ref, targetSig) - result.parentFrag.length(0, result.subfragstart - 1);
//							}
////							labelList.get(i).setLocation(objBorder.x + (int) (region.start * ratio / dnaRatio) - 80, objBorder.y + (int) ((((vrefList.get(i).getHeight() + 5) * (i + 1)) * ratio) - 10));
//
//							if (region3 != null)
//							{
//								JLabel cl = new JLabel();
//								labelList3.add(cl);								
//								if (!reversed)
//									displaceList3.add(displace + region3.start);
//								else
//									displaceList3.add((displace + ref.length() - region3.stop));
//								cl.setSize((int) (region3.length() * ratio / dnaRatio), 10);
//								cl.setBackground(Color.blue);
//								cl.setOpaque(true);
//								this.add(cl);
//								
//							}
//							else
//							{
//								labelList3.add(null);
//								displaceList3.add(null);
//							}
//							if (region5 != null)
//							{
//								JLabel cl = new JLabel();
//								labelList5.add(cl);
//								if (!reversed)
//									displaceList5.add(displace + region5.start);
//								else
//									displaceList5.add((displace + ref.length() - region5.stop));
//								cl.setSize((int) (region5.length() * ratio / dnaRatio), 10);
//								cl.setBackground(Color.red);
//								cl.setOpaque(true);
//								this.add(cl);
//								
//							}
//							else
//							{
//								labelList5.add(null);
//								displaceList5.add(null);
//							}
//							VReference vref = new VReference(ref);
//														
//							vref.setColor1(Color.YELLOW);
//							vref.setStartEndPoint(1, ref.size);							
//							vrefList.add(vref);							
//							this.add(vref);
//							displaceList.add(displace);
//							JLabel label = new JLabel(d);
//							label.setSize(80, 20);
//							labelList.add(label);
//							this.add(label);
//							
//							
//							
//
////							count++;
//							break;
//						}
//					}
//					if (found) 
//						break;
//				}
//			}
//		}
		// final ratio changes
//		setRatio(ratio);
//		setDNARatio(dnaRatio);
	}
	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) throws IllegalArgumentException {
		this.targetSignal = anchorPoint;
		construct();
	}

	private void construct() {
		if (region == null || targetSignal == null)
			return;
		DataNode ref = mainView.dataModule.getAllReference().get(targetSignal.ref);
		mainView.dataModule.getAllReference().get(targetSignal.ref);	
		int targetSig1 = ref.findExactRefpIndex(targetSignal.start);
		int targetSig2 = ref.findExactRefpIndex(targetSignal.stop);
		reset();
		createReference(ref, region, targetSig1, targetSig2);
		createMolecules(targetSig1, targetSig2);
		autoSetSize();
		reorganize();

	}
	public void reset() {
		if (headvref != null) {
			this.remove(headvref);
			headvref.removeMouseListener(this);
		}
		for (VMolecule vref : vrefList) {
			vref.removeMouseListener(this);
			this.remove(vref);
		}
		vrefList = new ArrayList<>();
		displaceList = new ArrayList<>();
	}
	public void createReference(DataNode ref, GenomicPosNode region, int targetSig1, int targetSig2) {
		// change ruler
		ruler.setStartEndPoint(new SimpleLongLocation(region.start, region.stop));
		headvref = new VReference(ref);
		long pt1 = region.start;
		long pt2 = region.stop;
		if (pt1 < 1) {
			vrefDisplace = 1 - region.start;
			pt1 = 1;
		}
		else
			vrefDisplace = 0;
		if (pt2 > ref.size)
			pt2 = ref.size;
		headvref.setStartEndPoint(new SimpleLongLocation(pt1, pt2));
//		headvref.setLocation(objBorder.x, 50);
		headvref.addSignalColor(targetSig1, ViewSetting.anchorViewAnchoredSignalColor);
		headvref.addSignalColor(targetSig2, ViewSetting.anchorViewAnchoredSignalColor);
		headvref.setVisible(true);
		headvref.addMouseListener(this);
		this.add(headvref);
	}
	public void createMolecules(int targetSig1, int targetSig2)
	{
//		reset();
		if (region == null || targetSignal == null)	{
			autoSetSize();
			return;
		}

		if (region.overlapSize(targetSignal) <= 0) {
			autoSetSize();
			return;
		}
		if (!mainView.dataModule.getAllReference().containsKey(targetSignal.ref)) {
			JOptionPane.showMessageDialog(mainView, "Reference not found.");
			autoSetSize();			
			return;
		}
//		int targetSig1 = ref.findExactRefpIndex(targetSignal.start);
//		int targetSig2 = ref.findExactRefpIndex(targetSignal.stop);
//		assert(targetSig1 != -1 && targetSig2 != -1); // Should be validated when user input targetSignal
			
//		headvref.setStartEndPoint(new SimpleLongLocation(region.start, region.stop));
////		headvref.setStartEndPoint(new SimpleLongLocation(-4000000, headvref.getDNALength()));
//		headvref.setLocation(objBorder.x, 50);
//		headvref.addMouseListener(this);
////		headvref.setColor1(Color.RED);
//		headvref.addSignalColor(targetSig1, Color.BLUE);
//		headvref.addSignalColor(targetSig2, Color.BLUE);
//		headvref.setVisible(true);
//		this.add(headvref);
		
//		this.region = region;

		long headDisplace = targetSignal.start;
		// Create other reference
		for (VMolecule vref : vrefList)
			this.remove(vref);
		vrefList.clear();
		displaceList.clear();
//		System.out.println(mainView.dataModule.getResult(selectedResult.get(0)).values().size());
		class SavedResult implements Comparable<SavedResult> {

			private final DataNode ref;
			private final long size;	
			private final long displace;
			private final int sig1;
			private final int sig2;
			private final List<OptMapResultNode> resultList;
			
			public SavedResult(DataNode ref, long size, long displace, int sig1, int sig2, List<OptMapResultNode> resultList) {
				super();
				this.ref = ref;
				this.size = size;
				this.displace = displace;
				this.sig1 = sig1;
				this.sig2 = sig2;
				this.resultList = resultList;
			}

			@Override
			public int compareTo(SavedResult r) {
				int x = Long.compare(this.size, r.size);
				if (x != 0)
					return x;
				else
					return Long.compare(this.displace, r.displace);
			}
			
		}
		
		List<SavedResult> savedList = new ArrayList<>();
		for (String r : dataSelection.get(VDataType.ALIGNMENT))
			for (List<List<OptMapResultNode>> resultlistlist : mainView.dataModule.getResult(r).values()) {
				if (idarray == null || idarray.contains(resultlistlist.get(0).get(0).parentFrag.name)) // Restrict molecules displayed
				{
					for (List<OptMapResultNode> resultList : resultlistlist)
					{
						OptMapResultNode refResult = null;
						int signal1 = -1;
						int signal2 = -1;
						for (OptMapResultNode result : resultList) {
							if (signal1 == -1) {
								signal1 = result.getSignal(targetSignal.ref, targetSig1);
								refResult = result;
							}
							if (signal2 == -1)
								signal2 = result.getSignal(targetSignal.ref, targetSig2);
							if (signal1 != -1 && signal2 != -1)
								break;
						}
						
						if (signal1 != -1 && signal2 != -1) {
							
							boolean reverse = refResult.mappedstrand == -1;
							
							if (reverse) {
								List<OptMapResultNode> tmpList = new ArrayList<>();
								for (OptMapResultNode result : resultList)
									if (reverse) {
										result = new OptMapResultNode(result);
										result.reverse();
										tmpList.add(result);
									}
								resultList = tmpList;
							}
							signal1 = -1;
							signal2 = -1;
							for (OptMapResultNode result : resultList) {
								if (signal1 == -1) {
									signal1 = result.getSignal(targetSignal.ref, targetSig1);
								}
								if (signal2 == -1)
									signal2 = result.getSignal(targetSignal.ref, targetSig2);
								if (signal1 != -1 && signal2 != -1)
									break;
							}
							assert(signal1 != -1 && signal2 != -1);
							
							DataNode ref = new DataNode(refResult.parentFrag);
							ref.scale(1 / refResult.getMapScale());
							if (reverse)
								ref.reverse();
							long size = ref.getSignalLength(signal1, signal2);
							long displace = headDisplace - ref.length(0, signal1);
							savedList.add(new SavedResult(ref, size, displace, signal1, signal2, resultList));
						}
							
//						for (OptMapResultNode result : resultList)
//						{							
//							if (result.mappedRegion.overlapSize(targetSignal) > 0)
//							{								
//								if (!(result.mapSignal(targetSignal.ref, targetSig1)))
//								{
//									found = true;
//									break;
//								}
//								long displace;
//								DataNode ref = new DataNode(result.parentFrag);
//								ref.scale(1 / result.getMapScale());
//								boolean reversed = false;
//								result = new OptMapResultNode(result);
//								result.scale(1 / result.getMapScale());
//								
//								
//								assert(result.mapSignal(targetSignal.ref, targetSig1)):targetSignal.ref; // Should be dealt with by the if clause
//								
//	
//								if (result.mappedstrand == -1)
//								{
//		//							displace = headDisplace - result.getSignalDisplace(region.ref, targetSig) - ref.length(result.subfragstart + 1, result.getTotalSegment() - 1);
//									result.reverse();
//									ref.reverse();	
//									displace = headDisplace - result.getSignalDisplace(targetSignal.ref, targetSig) - ref.length(0, result.subfragstart - 1);
//		//							System.out.println(ref.name);
//		//							System.out.println(displace);
//									reversed = true;
//								}
//								else
//								{
//									displace = headDisplace - result.getSignalDisplace(targetSignal.ref, targetSig) - ref.length(0, result.subfragstart - 1);
//								}
//								displaceList.add(displace);
//								VMolecule vref;
//								try {
//									vref = new VMolecule(ref);
//									vref.setSignalMatchStatus(result.getSignal(targetSignal.ref, targetSig), true);
//									vrefList.add(vref);							
//									this.add(vref);
//
//								} catch (InvalidVObjectException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//								
////								vref.setColor1(Color.YELLOW);
////								vref.setStartEndPoint(1, ref.size);							
//								found = true;
//								break;
//								
//							}
//						}
//						if (found)
//							break;
					}
					
				}
			}
		Collections.sort(savedList);
		
		for (SavedResult sr : savedList) {
				VMolecule vmole = new VMolecule(sr.ref);
				vmole.setBaseColor(Color.YELLOW);
				vmole.addSignalColor(OptMapResultNode.getMapSignal(sr.resultList), ViewSetting.anchorViewAlignedSignalColor);
				vrefList.add(vmole);
				vmole.addMouseListener(this);
				vmole.addSignalColor(sr.sig1, ViewSetting.anchorViewAnchoredSignalColor);
				vmole.addSignalColor(sr.sig2, ViewSetting.anchorViewAnchoredSignalColor);
				this.add(vmole);
				displaceList.add(sr.displace);
			
		}
		
		
		setRatio(ratio);
		setDNARatio(dnaRatio);
	}

	@Override
	public void setViewMolecule(String id) {
	}
	public void setViewMolecules(String[] idarray) {
		this.idarray = new HashSet<String>(Arrays.asList(idarray));
		}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (targetSignal == null)
			return;
		Graphics2D g = (Graphics2D) graphics;
		g.setPaint(Color.GRAY);
		g.setStroke(
		  new BasicStroke((float) (ViewSetting.signalStrokeWidth * ratio / 2),
                  BasicStroke.CAP_BUTT,
                  BasicStroke.JOIN_MITER,
                  10.0f, new float[] {(float) (ViewSetting.signalStrokeWidth * ratio)}, 0.0f));
		int x1 = (int) (objBorder.x + (targetSignal.start - region.start) / dnaRatio * ratio);
		int x2 = (int) (objBorder.x + (targetSignal.stop - region.start) / dnaRatio * ratio);
		g.drawLine(x1, (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio), x1, this.getHeight());
		g.drawLine(x2, (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio), x2, this.getHeight());
		
		// Draw the query names
		// Determine the font size
		Font font; 
		int fsize = 1;
		int h = (int) (ViewSetting.moleculeSpace * ratio);
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
	@Override
	public void updateData() {
		super.updateData();
		this.setRegion(region);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof VMolecule)
		{			
			VMolecule vm = (VMolecule) e.getSource();
			mainView.createAlignmentViewTab(dataSelection).setViewMolecule(vm.getName());;
		}	
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
}
