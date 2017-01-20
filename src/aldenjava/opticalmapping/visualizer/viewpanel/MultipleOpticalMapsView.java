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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import aldenjava.common.SimpleLongLocation;
import aldenjava.file.ListExtractor;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.MultipleAlignmentFormat;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockWriter;
import aldenjava.opticalmapping.multiplealignment.GroupingEntry;
//import aldenjava.opticalmapping.multiplealignment.MultipleAlignment;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.utils.VPartialMoleculeInfo;
import aldenjava.opticalmapping.visualizer.vobject.VIndel;
import aldenjava.opticalmapping.visualizer.vobject.VInversion;
import aldenjava.opticalmapping.visualizer.vobject.VMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VMultiAlignMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VObject;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;
import aldenjava.opticalmapping.visualizer.vobject.VSpace;

public class MultipleOpticalMapsView extends ViewPanel {

	private VRuler ruler;

	private LinkedHashMap<String, CollinearBlock> collinearBlocks;
	private List<String> orders; // Order of optical map
	private LinkedHashMap<String, Color> colors; // Color options for group
	private List<AnnotationNode> annotations;
	private LinkedHashMap<String, DataNode> dataInfo;
	private LinkedHashMap<String, List<VObject>> objectListMap = new LinkedHashMap<>();
	private LinkedHashMap<String, List<VObject>> rearrangementListMap = new LinkedHashMap<>();
	private LinkedHashMap<String, List<Long>> rearrangementPosListMap = new LinkedHashMap<>();
	private LinkedHashMap<String, SimpleLongLocation> annotationMap = new LinkedHashMap<>();
	private LinkedHashMap<String, Color> colorMap;
	private Long seed = null;
	// to solve (1) rearrangement reorganize
	// to solve (2) rearrangement display
	
	public MultipleOpticalMapsView(OMView mainView) {
		super(mainView);
		this.ruler = new VRuler();
		ruler.setVisible(true);
		ruler.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y);
		this.add(ruler);
		updateData();
		autoSetSize();
	}
	@Override
	public void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.MULTIPLEALIGNMENTBLOCK, new ArrayList<String>());
		dataSelection.put(VDataType.MULTIPLEALIGNMENTORDER, new ArrayList<String>());
		dataSelection.put(VDataType.MULTIPLEALIGNMENTCOLOR, new ArrayList<String>());
//		dataSelection.put(VDataType.ANNOTATION, new ArrayList<String>());
	}
	
	@Override
	public void setDNARatio(double dnaRatio) {
		if (!(ruler == null || orders == null || objectListMap == null || rearrangementListMap == null)) {
	
			ruler.setDNARatio(dnaRatio);
			for (String name : objectListMap.keySet()) {
				for (VObject vobj : objectListMap.get(name))
					vobj.setDNARatio(dnaRatio);
				for (VObject vobj : rearrangementListMap.get(name))
					vobj.setDNARatio(dnaRatio);
			}
		}
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		if (!(ruler == null || orders == null || objectListMap == null || rearrangementListMap == null)) {
			ruler.setRatio(ratio);
			for (String name : objectListMap.keySet()) {
				for (VObject vobj : objectListMap.get(name))
					vobj.setRatio(ratio);
				for (VObject vobj : rearrangementListMap.get(name))
					vobj.setRatio(ratio);
			}
		}
		super.setRatio(ratio);
	}

	@Override
	public void updateData() {
		super.updateData();
		updateMolecules();
	}

	private void createEmptyPanel() {
		for (String name : objectListMap.keySet())
			for (VObject vobj : objectListMap.get(name))
				this.remove(vobj);
		objectListMap = new LinkedHashMap<>();
		ruler.setVisible(false);
	}
	
	private void updateMolecules() {
		dataInfo = new LinkedHashMap<String, DataNode>();
		for (DataNode data : mainView.dataModule.getAllData().values()) {
			dataInfo.put(data.name, data);
		}
		List<String> selectedBlocks = dataSelection.get(VDataType.MULTIPLEALIGNMENTBLOCK);
		if (selectedBlocks.size() > 1) {
			System.err.println("More than one multiple alignment file is not supported. ");
			return;
		}
		if (selectedBlocks.size() == 1)
			collinearBlocks = mainView.dataModule.getMultipleAlignmentBlock(selectedBlocks.get(0));
		else
			collinearBlocks = null;
		
		List<String> selectedOrders = dataSelection.get(VDataType.MULTIPLEALIGNMENTORDER);
		if (selectedOrders.size() > 1) {
			System.err.println("More than one multiple alignment order file is not supported. ");
			return;
		}
		if (selectedOrders.size() == 1) 
			orders = mainView.dataModule.getMultipleAlignmentOrder(selectedOrders.get(0));	
		else
			orders = null;
		
		List<String> selectedColors = dataSelection.get(VDataType.MULTIPLEALIGNMENTCOLOR);
		if (selectedColors.size() > 1) {
			System.err.println("More than one multiple alignment file is not supported. ");
			return;
		}
		if (selectedColors.size() == 1)
			colors = mainView.dataModule.getMultipleAlignmentColor(selectedColors.get(0));
		else
			colors = null;
		annotations = mainView.dataModule.getAllAnno();
		if (dataInfo.isEmpty() || collinearBlocks == null || collinearBlocks.isEmpty() || orders == null)
			createEmptyPanel();
		else
			createMolecules();
	}

	
	private void createMolecules() {
		this.setVisible(false);
		VerbosePrinter.println("Removing old Vobjects...");
		if (objectListMap != null)
			for (String name : objectListMap.keySet())
				for (VObject vobj : objectListMap.get(name))
					this.remove(vobj);
		if (rearrangementListMap != null)
			for (String name : rearrangementListMap.keySet())
				for (VObject vobj : rearrangementListMap.get(name))
					this.remove(vobj);
		colorMap = null;
		
		VerbosePrinter.println("Initializing panels and lists...");
		LinkedHashMap<String, List<VMolecule>> vmoleGroups = new LinkedHashMap<>();	
		
		objectListMap = new LinkedHashMap<>();
		rearrangementListMap = new LinkedHashMap<>();
		rearrangementPosListMap = new LinkedHashMap<>();
		annotationMap = new LinkedHashMap<>();
		LinkedHashMap<String, Long> lastPos = new LinkedHashMap<>();
		LinkedHashMap<String, Integer> lastSig = new LinkedHashMap<>();
		for (String name : orders) {
			objectListMap.put(name, new ArrayList<VObject>());
			rearrangementListMap.put(name, new ArrayList<VObject>());
			rearrangementPosListMap.put(name, new ArrayList<Long>());
			lastPos.put(name, 0L);
			lastSig.put(name, -1);
		}
		
		VerbosePrinter.println("Laying out collinear blocks...");
		for (String group : collinearBlocks.keySet()) {
//			Color color; 
//			if (colors != null)
//				color = colors.get(group);
//			else {
//				float r = rand.nextFloat() / 2f + 0.5f;
//				float g = rand.nextFloat() / 2f + 0.5f;
//				float b = rand.nextFloat() / 2f + 0.5f;
//				color = new Color(r, g, b);
//			}
			vmoleGroups.put(group, new ArrayList<VMolecule>());
			LinkedHashMap<String, VPartialMoleculeInfo> map = collinearBlocks.get(group).groups;
			long maxLastPos = Long.MIN_VALUE;
			for (String name : map.keySet())
				if (lastPos.get(name) > maxLastPos)
					maxLastPos = lastPos.get(name);
			
			// Calculate groupDNALength now
			long groupDNALength = -1;
			for (String name : map.keySet()) {
				VPartialMoleculeInfo pmi = map.get(name);
				DataNode data = dataInfo.get(name);
				long length = Math.abs(data.refp[pmi.stopSig] - data.refp[pmi.startSig]) + 1;
				if (length > groupDNALength)
					groupDNALength = length;
			}
			
			for (String name : map.keySet()) {
//				long spacelen = maxLastPos - lastPos.get(name) - 1;
				long spacelen = maxLastPos - lastPos.get(name);
				assert spacelen >= 0;
				VPartialMoleculeInfo pmi = map.get(name);


//				if (spacelen > 0) {
//					if (pmi.startSig == 0)
//					if (lastPos.get(name) == 0)
//						objectListMap.get(name).add(new VSpace(spacelen, 0));
//					else
//						objectListMap.get(name).add(new VIndel(spacelen, 0));
//				}

					
				if (lastSig.get(name) != -1 && lastSig.get(name) != pmi.startSig) {
//					rearrangementListMap.get(name).add(new VRearrangement());
//					rearrangementPosListMap.get(name).add(lastPos.get(name) + spacelen / 2 - ViewSetting.minSVObjectSize / 2);
					objectListMap.get(name).add(new VInversion(spacelen, 0));
				}
				else
					if (lastPos.get(name) == 0)
						objectListMap.get(name).add(new VSpace(spacelen, 0));
					else
						objectListMap.get(name).add(new VIndel(spacelen, 0));

	
					
				DataNode data = new DataNode(dataInfo.get(name));
				GenomicPosNode pos;
				if (!pmi.isReverse()) {
					pos = new GenomicPosNode(data.name, data.refp[pmi.startSig], data.refp[pmi.stopSig]);
				}
				else
					pos = new GenomicPosNode(data.name, data.refp[pmi.stopSig], data.refp[pmi.startSig]);
				
				VMolecule vmole = new VMultiAlignMolecule(data, group, pmi);
				vmole.setStartEndPoint(pos.getLoc());
				vmole.toolTipText = group + "_" + data.name;
				long length = Math.abs(data.refp[pmi.stopSig] - data.refp[pmi.startSig]) + 1;
				vmole.setReverse(pmi.isReverse());
				for (AnnotationNode anno : annotations)
					if (anno.region.overlapSize(pos) > 0) {
						long p1;
						long p2;
						if (pmi.isReverse()) {// Untested for reverse 
							p1 = lastPos.get(name) + spacelen + groupDNALength - (anno.region.start - data.refp[pmi.startSig]) * groupDNALength / length;
							p2 = lastPos.get(name) + spacelen + groupDNALength - (anno.region.stop - data.refp[pmi.startSig]) * groupDNALength / length;
						}
						else {
							p1 = lastPos.get(name) + spacelen + (anno.region.start - data.refp[pmi.startSig]) * groupDNALength / length;
							p2 = lastPos.get(name) + spacelen + (anno.region.stop - data.refp[pmi.startSig]) * groupDNALength / length;
						}
						annotationMap.put(anno.getName(), new SimpleLongLocation(p1, p2));
					}
				vmoleGroups.get(group).add(vmole);
				objectListMap.get(name).add(vmole);
				lastSig.put(name, pmi.stopSig);
				lastPos.put(name, lastPos.get(name) + spacelen + length);
			}
			
				
		}
		

		List<Entry<String, List<VMolecule>>> entries = new ArrayList<>(vmoleGroups.entrySet());
		for (int i = entries.size() - 1; i >= 0; i--) {
			List<VMolecule> vmoles = entries.get(i).getValue();
			boolean canAlignToRight = true;
			long minLength = Long.MAX_VALUE;
			for (VMolecule vmole : vmoles) {
				List<VObject> objectList = objectListMap.get(vmole.getName());
				int index = objectList.indexOf(vmole);
				if (index == objectList.size() - 1 || !((objectList.get(index + 1) instanceof VIndel) || (objectList.get(index + 1) instanceof VInversion))) {
					canAlignToRight = false; 
					break;
				}
				long length = objectList.get(index + 1).getDNALength();
				if (length < minLength)
					minLength = length;
			}
			if (minLength > 0 && canAlignToRight) {
				for (VMolecule vmole : vmoles) {
					List<VObject> objectList = objectListMap.get(vmole.getName());
					int index = objectList.indexOf(vmole);
					if (index < objectList.size() - 1) {
						VSpace vSpace = (VSpace) objectList.get(index + 1);
						vSpace.setRefDNALength(vSpace.getRefDNALength() - minLength);
						}
					if (index > 0) {
						VSpace vSpace = (VSpace) objectList.get(index - 1);
						vSpace.setRefDNALength(vSpace.getRefDNALength() + minLength);
					}
				}
			}
				
		}
		
		
		// Update color
		resetVMoleColor();
		
		// Add the vcomponents, set mouse listener and visibility
		for (String name : orders) {
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					this.add(vobj);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					this.add(vobj);
				}
		}
		for (String name : orders) {
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					vobj.addMouseListener(this);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					vobj.addMouseListener(this);
				}
		}
		for (String name : orders) {
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					vobj.setVisible(true);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					vobj.setVisible(true);
				}
		}

		// Calculate ruler position
		long maxLastPos = 0;
		for (String name : lastPos.keySet())
			if (lastPos.get(name) > maxLastPos)
				maxLastPos = lastPos.get(name);

		ruler.setStartEndPoint(new SimpleLongLocation(1, maxLastPos));
		ruler.setVisible(true);
		setRatio(ratio);
		setDNARatio(dnaRatio);
		this.setVisible(true);
	}

	private void resetVMoleColor() {
		LinkedHashMap<String, Color> colorMap = new LinkedHashMap<>();
		if (seed == null)
			seed = new Random().nextLong();
		Random rand = new Random(seed);
		for (String group : collinearBlocks.keySet()) {
			Color color; 
			if (colors != null)
				color = colors.get(group);
			else {
				float r = rand.nextFloat() / 2f + 0.5f;
				float g = rand.nextFloat() / 2f + 0.5f;
				float b = rand.nextFloat() / 2f + 0.5f;
				color = new Color(r, g, b);
			}
			colorMap.put(group, color);
		}
		this.colorMap = colorMap;
		for (String name : orders)
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMultiAlignMolecule) {
					VMultiAlignMolecule vmole = (VMultiAlignMolecule) vobj;
					Color color = colorMap.get(vmole.group);
					vmole.setBaseColor(color);
				}
		this.repaint();
	}
	
	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);		
		Graphics2D g = (Graphics2D) graphics;
		int height = objBorder.y + ruler.getHeight();
		int i = 1;
		if (orders == null)
			return;
		for (String name : orders)
			g.drawString(name, objBorder.x, (int) (height + i++ * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));

		g.setPaint(Color.RED);
		g.setStroke(
		  new BasicStroke((float) (ViewSetting.signalStrokeWidth * ratio / 2),
                  BasicStroke.CAP_BUTT,
                  BasicStroke.JOIN_MITER,
                  10.0f, new float[] {(float) (ViewSetting.signalStrokeWidth * ratio)}, 0.0f));
		for (String key : annotationMap.keySet()) {
			int x1 = objBorder.x + ViewSetting.moleculeNameSize + (int) (annotationMap.get(key).min / dnaRatio * ratio);
			int x2 = objBorder.x + ViewSetting.moleculeNameSize + (int) (annotationMap.get(key).max / dnaRatio * ratio);
			g.drawString(key, x1, (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio));
			g.drawRect(x1, (int) (objBorder.y + ruler.getHeight() + ViewSetting.moleculeSpace * ratio), x2 - x1, this.getHeight());
		}

	}

	@Override
	public void setRegion(GenomicPosNode region) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void setViewMolecule(String id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) {
		throw new UnsupportedOperationException();

	}

	
	
	
	
	
	@Override
	protected void updateMenu() {
		super.updateMenu();
		menu.addSeparator();

		JMenu sortMenu = new JMenu("Sort");
		sortMenu.setMnemonic('o');
		
		JMenuItem sortByGroupItem = new JMenuItem("Group");
		sortByGroupItem.setMnemonic('g');
		sortByGroupItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> sortCris = new ArrayList<>();
				
				String ans = JOptionPane.showInputDialog(mainView, "Please input the groups (separated by \"-\")", "");
				if (ans == null)
					return;
				sortCris = Arrays.asList(ans.split("\\-"));
				for (String sortCri : sortCris)
					if (!collinearBlocks.containsKey(sortCri)) {
						System.err.println("The selected groups are not found. ");
					 	return;
					}

				List<String> myOrder = new ArrayList<>(orders);
				for (int i = sortCris.size() - 1; i >= 0; i--) {
					String sortCri = sortCris.get(i);
					HashSet<String> existingGroups = new HashSet<>(collinearBlocks.get(sortCri).groups.keySet());
					for (int j = 0; j < myOrder.size(); j++) {
						int k = j;
						if (existingGroups.contains(myOrder.get(k)))
							while (k > 0 && !existingGroups.contains(myOrder.get(k - 1))){
								String t1 = myOrder.get(k - 1);
								String t2 = myOrder.get(k);
								myOrder.set(k - 1, t2);
								myOrder.set(k, t1);
								k--;
							}
					}
				}
				orders = myOrder;
				MultipleOpticalMapsView.this.reorganize();
			}
		});
		sortMenu.add(sortByGroupItem);
		
		JMenuItem sortByNameItem = new JMenuItem("Name");
		sortByNameItem.setMnemonic('n');
		sortByNameItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> myOrder = new ArrayList<>(orders);
				Collections.sort(myOrder);
				orders = myOrder;
				MultipleOpticalMapsView.this.reorganize();

			}
		});
		sortMenu.add(sortByNameItem);

		JMenuItem sortBySignalItem = new JMenuItem("Signals between two groups");
		sortBySignalItem.setMnemonic('s');
		sortBySignalItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans1 = JOptionPane.showInputDialog(mainView, "Please input group 1", "");
				String ans2 = JOptionPane.showInputDialog(mainView, "Please input group 2", "");
				if (ans1 == null || ans2 == null)
					return;
				if (ans1.equals(ans2)) {
					System.err.println("The selected groups must be different.");
				 	return;
				}
				if (!collinearBlocks.containsKey(ans1) || !collinearBlocks.containsKey(ans2)) {
					System.err.println("The selected groups are not found. ");
				 	return;
				}
				// Obtain the signal count between two groups
				HashSet<String> set1 = new HashSet<>();
				HashSet<String> set2 = new HashSet<>();
				boolean toCalculate = false;
				LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
				for (CollinearBlock block : collinearBlocks.values()) {
					if (block.name.equals(ans1)) {
						toCalculate = true;
						for (String name : block.groups.keySet()) 
							set1.add(name);
						continue;
					}
					if (block.name.equals(ans2)) {
						if (!toCalculate) {
							System.err.println(ans2 + " appears earlier than " + ans1 + ".");
						 	return;
						}
						toCalculate = false;
						for (String name : block.groups.keySet()) 
							set2.add(name);
						break;
					}
						
					if (toCalculate) {
						for (String name : block.groups.keySet()) {
							if (!counts.containsKey(name))
								counts.put(name, -1); // Starting with -1 signals, as flanking signals are not counted
							VPartialMoleculeInfo vp = block.groups.get(name);
							int sig = Math.abs(vp.startSig - vp.stopSig);
							counts.put(name, counts.get(name) + sig);
						}
					}
				}

				// Sort the contigs by (1) existing group (2) number of signals
				List<String> nameList = new ArrayList<>();
				List<Integer> countList = new ArrayList<>();
				for (String name : set1)
					if (set2.contains(name))
						if (counts.containsKey(name)) {
							nameList.add(name);
							countList.add(counts.get(name));
						}
				for (int i = 0; i < countList.size(); i++)
					for (int j = countList.size() - 2; j >= i; j--)
						if (countList.get(j) < countList.get(j + 1)) {
							String tmpName = nameList.get(j);
							Integer tmpCount = countList.get(j);
							nameList.set(j, nameList.get(j + 1));
							countList.set(j, countList.get(j + 1));
							nameList.set(j + 1, tmpName);
							countList.set(j + 1, tmpCount);
						}
				
				// Update the order
				List<String> myOrder = new ArrayList<>();
				myOrder.addAll(nameList);
				for (String order : orders)
					if (!nameList.contains(order))
						myOrder.add(order);
				orders = myOrder;
				MultipleOpticalMapsView.this.reorganize();

			}
		});
		sortMenu.add(sortBySignalItem);
		
		JMenuItem sortByLengthItem = new JMenuItem("Length between two groups");
		sortByLengthItem.setMnemonic('l');
		sortByLengthItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans1 = JOptionPane.showInputDialog(mainView, "Please input group 1", "");
				String ans2 = JOptionPane.showInputDialog(mainView, "Please input group 2", "");
				if (ans1 == null || ans2 == null)
					return;
				if (ans1.equals(ans2)) {
					System.err.println("The selected groups must be different.");
				 	return;
				}
				if (!collinearBlocks.containsKey(ans1) || !collinearBlocks.containsKey(ans2)) {
					System.err.println("The selected groups are not found. ");
				 	return;
				}
				// Obtain the signal count between two groups
				HashSet<String> set1 = new HashSet<>();
				HashSet<String> set2 = new HashSet<>();
				boolean toCalculate = false;
				LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
				for (CollinearBlock block : collinearBlocks.values()) {
					if (block.name.equals(ans1)) {
						toCalculate = true;
						for (String name : block.groups.keySet()) 
							set1.add(name);
						continue;
					}
					if (block.name.equals(ans2)) {
						if (!toCalculate) {
							System.err.println(ans2 + " appears earlier than " + ans1 + ".");
						 	return;
						}
						toCalculate = false;
						for (String name : block.groups.keySet()) 
							set2.add(name);
						break;
					}
						
					if (toCalculate) {
						for (String name : block.groups.keySet()) {
							if (!counts.containsKey(name))
								counts.put(name, -1L); // Starting with -1 length, as flanking signals are not counted in length
							VPartialMoleculeInfo vp = block.groups.get(name);
							long len = Math.abs(dataInfo.get(name).refp[vp.stopSig] - dataInfo.get(name).refp[vp.startSig]);
							counts.put(name, counts.get(name) + len);
						}
					}
				}

				// Sort the contigs by (1) existing group (2) number of signals
				List<String> nameList = new ArrayList<>();
				List<Long> countList = new ArrayList<>();
				for (String name : set1)
					if (set2.contains(name))
						if (counts.containsKey(name)) {
							nameList.add(name);
							countList.add(counts.get(name));
						}
				for (int i = 0; i < countList.size(); i++)
					for (int j = countList.size() - 2; j >= i; j--)
						if (countList.get(j) < countList.get(j + 1)) {
							String tmpName = nameList.get(j);
							Long tmpCount = countList.get(j);
							nameList.set(j, nameList.get(j + 1));
							countList.set(j, countList.get(j + 1));
							nameList.set(j + 1, tmpName);
							countList.set(j + 1, tmpCount);
						}
				
				// Update the order
				List<String> myOrder = new ArrayList<>();
				myOrder.addAll(nameList);
				for (String order : orders)
					if (!nameList.contains(order))
						myOrder.add(order);
				orders = myOrder;
				MultipleOpticalMapsView.this.reorganize();

			}
		});
		sortMenu.add(sortByLengthItem);

		menu.add(sortMenu);
		

		JMenu saveMenu = new JMenu("Save");
		saveMenu.setMnemonic('v');
		
		JMenuItem saveEntriesItem = new JMenuItem("Entries");
		saveEntriesItem.setMnemonic('e');
		saveEntriesItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser dataSaveFileChooser = new JFileChooser();
				
				dataSaveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				dataSaveFileChooser.setAcceptAllFileFilterUsed(false);
				dataSaveFileChooser.setMultiSelectionEnabled(false);
				dataSaveFileChooser.setDialogTitle("Save data");
				dataSaveFileChooser.setCurrentDirectory(mainView.workingDirectory);
				dataSaveFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(MultipleAlignmentFormat.CBL.getDescription(), MultipleAlignmentFormat.CBL.getExtension()));
				int selection = dataSaveFileChooser.showSaveDialog(mainView);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					mainView.workingDirectory = dataSaveFileChooser.getCurrentDirectory();
					String path = dataSaveFileChooser.getSelectedFile().getAbsolutePath();
					try {
						CollinearBlockWriter.writeAll(path, orders.toArray(new String[orders.size()]), collinearBlocks);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
				}


			}
		});
		saveMenu.add(saveEntriesItem);
		
		JMenuItem saveOrderItem = new JMenuItem("Order");
		saveOrderItem.setMnemonic('o');
		saveOrderItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser dataSaveFileChooser = new JFileChooser();
				
				dataSaveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				dataSaveFileChooser.setAcceptAllFileFilterUsed(false);
				dataSaveFileChooser.setMultiSelectionEnabled(false);
				dataSaveFileChooser.setDialogTitle("Save data");
				dataSaveFileChooser.setCurrentDirectory(mainView.workingDirectory);
				dataSaveFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(MultipleAlignmentFormat.CBO.getDescription(), MultipleAlignmentFormat.CBO.getExtension()));
				int selection = dataSaveFileChooser.showSaveDialog(mainView);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					mainView.workingDirectory = dataSaveFileChooser.getCurrentDirectory();
					String path = dataSaveFileChooser.getSelectedFile().getAbsolutePath();
					try {
						ListExtractor.writeList(path, orders);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
				}


			}
		});
		saveMenu.add(saveOrderItem);
		
		JMenuItem saveColorItem = new JMenuItem("Color");
		saveColorItem.setMnemonic('c');
		saveColorItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser dataSaveFileChooser = new JFileChooser();
				
				dataSaveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				dataSaveFileChooser.setAcceptAllFileFilterUsed(false);
				dataSaveFileChooser.setMultiSelectionEnabled(false);
				dataSaveFileChooser.setDialogTitle("Save color");
				dataSaveFileChooser.setCurrentDirectory(mainView.workingDirectory);
				dataSaveFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(MultipleAlignmentFormat.CBC.getDescription(), MultipleAlignmentFormat.CBC.getExtension()));
				int selection = dataSaveFileChooser.showSaveDialog(mainView);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					mainView.workingDirectory = dataSaveFileChooser.getCurrentDirectory();
					String path = dataSaveFileChooser.getSelectedFile().getAbsolutePath();
					try {
						BufferedWriter bw = new BufferedWriter(new FileWriter(path));
						for (String group : collinearBlocks.keySet())
							bw.write(group + "\t" + colorMap.get(group).getRGB() + "\n");
						bw.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
				}


			}
		});
		saveMenu.add(saveColorItem);

		menu.add(saveMenu);
		
		
		JMenu countMenu = new JMenu("Count");
		countMenu.setMnemonic('C');

		JMenuItem countSignalItem = new JMenuItem("Signals");
		countSignalItem.setMnemonic('S');
		countSignalItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans1 = JOptionPane.showInputDialog(mainView, "Please input group 1", "");
				String ans2 = JOptionPane.showInputDialog(mainView, "Please input group 2", "");
				if (ans1 == null || ans2 == null)
					return;
				if (ans1.equals(ans2)) {
					System.err.println("The selected groups must be different.");
				 	return;
				}
				if (!collinearBlocks.containsKey(ans1) || !collinearBlocks.containsKey(ans2)) {
					System.err.println("The selected groups are not found. ");
				 	return;
				}
				HashSet<String> set1 = new HashSet<>();
				HashSet<String> set2 = new HashSet<>();
				boolean toCalculate = false;
				LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
				for (CollinearBlock block : collinearBlocks.values()) {
					if (block.name.equals(ans1)) {
						toCalculate = true;
						for (String name : block.groups.keySet()) 
							set1.add(name);
						continue;
					}
					if (block.name.equals(ans2)) {
						if (!toCalculate) {
							System.err.println(ans2 + " appears earlier than " + ans1 + ".");
						 	return;
						}
						toCalculate = false;
						for (String name : block.groups.keySet()) 
							set2.add(name);
						break;
					}
						
					if (toCalculate) {
						for (String name : block.groups.keySet()) {
							if (!counts.containsKey(name))
								counts.put(name, -1); // Starting with -1 signals, as flanking signals are not counted
							VPartialMoleculeInfo vp = block.groups.get(name);
							int sig = Math.abs(vp.startSig - vp.stopSig);
							counts.put(name, counts.get(name) + sig);
						}
					}
				}

				System.out.println("No. of signals between " + ans1 + " and " + ans2 + ": ");
				for (String name : set1)
					if (set2.contains(name))
						if (counts.containsKey(name))
							System.out.println(name + ":" + counts.get(name));
//				JFileChooser dataSaveFileChooser = new JFileChooser();
//				
//				dataSaveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
//				dataSaveFileChooser.setAcceptAllFileFilterUsed(false);
//				dataSaveFileChooser.setMultiSelectionEnabled(false);
//				dataSaveFileChooser.setDialogTitle("Save data");
//				dataSaveFileChooser.setCurrentDirectory(mainView.workingDirectory);
//				int selection = dataSaveFileChooser.showSaveDialog(mainView);
//				if (selection == JFileChooser.APPROVE_OPTION)
//				{
//					mainView.workingDirectory = dataSaveFileChooser.getCurrentDirectory();
//					String path = dataSaveFileChooser.getSelectedFile().getAbsolutePath();
//					try {
//						new MultipleAlignment().outputCollinearBlocks(path, CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values())), orders);
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
//					
//				}
			}
		});
		countMenu.add(countSignalItem);

		JMenuItem countLengthItem = new JMenuItem("Length");
		countLengthItem.setMnemonic('l');
		countLengthItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans1 = JOptionPane.showInputDialog(mainView, "Please input group 1", "");
				String ans2 = JOptionPane.showInputDialog(mainView, "Please input group 2", "");
				if (ans1 == null || ans2 == null)
					return;
				if (ans1.equals(ans2)) {
					System.err.println("The selected groups must be different.");
				 	return;
				}
				if (!collinearBlocks.containsKey(ans1) || !collinearBlocks.containsKey(ans2)) {
					System.err.println("The selected groups are not found. ");
				 	return;
				}
				HashSet<String> set1 = new HashSet<>();
				HashSet<String> set2 = new HashSet<>();
				boolean toCalculate = false;
				LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
				for (CollinearBlock block : collinearBlocks.values()) {
					if (block.name.equals(ans1)) {
						toCalculate = true;
						for (String name : block.groups.keySet()) 
							set1.add(name);
						continue;
					}
					if (block.name.equals(ans2)) {
						if (!toCalculate) {
							System.err.println(ans2 + " appears earlier than " + ans1 + ".");
						 	return;
						}
						toCalculate = false;
						for (String name : block.groups.keySet()) 
							set2.add(name);
						break;
					}
						
					if (toCalculate) {
						for (String name : block.groups.keySet()) {
							if (!counts.containsKey(name))
								counts.put(name, -1L); // Starting with -1 length, as flanking signals are not counted in length
							VPartialMoleculeInfo vp = block.groups.get(name);
							long len = Math.abs(dataInfo.get(name).refp[vp.stopSig] - dataInfo.get(name).refp[vp.startSig]);
							counts.put(name, counts.get(name) + len);
						}
					}
				}

				System.out.println("Length between " + ans1 + " and " + ans2 + ": ");
				for (String name : set1)
					if (set2.contains(name))
						if (counts.containsKey(name))
							System.out.println(name + ":" + counts.get(name));
			}
		});
		countMenu.add(countLengthItem);

		JMenuItem countGroupItem = new JMenuItem("Groups");
		countGroupItem.setMnemonic('g');
		countGroupItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input group", "");
				if (ans == null)
					return;
				if (!collinearBlocks.containsKey(ans)) {
					System.err.println("The selected group " + ans + " is not found. ");
				 	return;
				}
				System.out.println("Count of " + ans + ": " + collinearBlocks.get(ans).groups.size());
				for (String name : collinearBlocks.get(ans).groups.keySet())
					System.out.println(name);
			}
		});
		countMenu.add(countGroupItem);

		menu.add(countMenu);
		
		
		JMenu manipulateMenu = new JMenu("Manipulate");
		manipulateMenu.setMnemonic('m');
		JMenuItem setSeedItem = new JMenuItem("Seed for random color");
		setSeedItem.setMnemonic('S');
		setSeedItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input the seed", MultipleOpticalMapsView.this.seed==null ? "" : MultipleOpticalMapsView.this.seed + "");
				if (ans != null) {
					try {
						MultipleOpticalMapsView.this.seed = Long.parseLong(ans);
						MultipleOpticalMapsView.this.resetVMoleColor();
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
					}
				}
			}
		});
		manipulateMenu.add(setSeedItem);

		JMenuItem reverseItem = new JMenuItem("Reverse all");
		reverseItem.setMnemonic('r');
		reverseItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				VerbosePrinter.println("Reversing the whole panel");
				List<GroupingEntry> entries = CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values()));
				List<GroupingEntry> newEntries = new ArrayList<>(); 
				for (int i = entries.size() - 1; i >= 0; i--)
					newEntries.add(entries.get(i).getReverse(entries.get(i).name));
				
				collinearBlocks.clear();
				for (CollinearBlock block : CollinearBlock.toCollinearBlocks(newEntries))
					collinearBlocks.put(block.name, block);
				selectedMoleSet.clear();
				VerbosePrinter.println("Updating the view panels...");
				MultipleOpticalMapsView.this.updateData();
			}
		});
		manipulateMenu.add(reverseItem);
		
		JMenuItem moveItem = new JMenuItem("Move");
		moveItem.setMnemonic('M');
		moveItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input the group", "");
				if (ans != null) {
					if (!collinearBlocks.containsKey(ans))
						collinearBlocks.put(ans, new CollinearBlock(ans));
					for (VMultiAlignMolecule vmamole : selectedMoleSet) {
						if (collinearBlocks.get(ans).groups.containsKey(vmamole.getName()))
							System.err.println("Error. The group " + ans + " already contains " + vmamole.getName());
					}
					for (VMultiAlignMolecule vmamole : selectedMoleSet) {
						collinearBlocks.get(ans).groups.put(vmamole.getName(), collinearBlocks.get(vmamole.group).groups.remove(vmamole.getName()));	
					}
					List<GroupingEntry> entries = CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values()));
//					MultipleAlignment multipleAlignment = new MultipleAlignment();
//					entries = multipleAlignment.layoutCollinearBlocksNJ(entries, orders);
					collinearBlocks.clear();
					for (CollinearBlock block : CollinearBlock.toCollinearBlocks(entries))
						collinearBlocks.put(block.name, block);
					
					selectedMoleSet.clear();
					MultipleOpticalMapsView.this.updateData();
				}
			}
		});
		manipulateMenu.add(moveItem);
		
		JMenuItem mergeItem = new JMenuItem("Merge Group");
		mergeItem.setMnemonic('e');
		mergeItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input the new group", "");
				if (ans != null) {
					if (!collinearBlocks.containsKey(ans))
						collinearBlocks.put(ans, new CollinearBlock(ans));
					
					// Check all the group involved
					HashSet<String> groups = new HashSet<>();
					for (VMultiAlignMolecule vmamole : selectedMoleSet) {
						groups.add(vmamole.group);
					}
					
					// Put all selected collinear blocks into the target collinear blocks
					VerbosePrinter.println("Existing collinear blocks...");
					for (String name : collinearBlocks.get(ans).groups.keySet()) {
						System.out.println("\t" + name + ":" + collinearBlocks.get(ans).groups.get(name).startSig + "-" + collinearBlocks.get(ans).groups.get(name).stopSig);
					}
					VerbosePrinter.println("Moving collinear blocks...");
					for (String group : groups) {
						if (group.equals(ans))
							continue;
						for (String name : collinearBlocks.get(group).groups.keySet()) {
							if (collinearBlocks.get(ans).groups.containsKey(name)) {
								System.err.println("Error. The group " + ans + " already contains " + name);
								continue;
							}
							collinearBlocks.get(ans).groups.put(name, collinearBlocks.get(group).groups.get(name));
						}
						collinearBlocks.remove(group);
					}
					VerbosePrinter.println("Reprocess the order of alignments...");
					List<GroupingEntry> entries = CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values()));					
//					MultipleAlignment multipleAlignment = new MultipleAlignment();
//					entries = multipleAlignment.layoutCollinearBlocksNJ(entries, orders);
					collinearBlocks.clear();
					for (CollinearBlock block : CollinearBlock.toCollinearBlocks(entries))
						collinearBlocks.put(block.name, block);
					selectedMoleSet.clear();
					
					VerbosePrinter.println("Updating the view panels...");
					MultipleOpticalMapsView.this.updateData();
				}					
			}
		});
		manipulateMenu.add(mergeItem);


		JMenuItem splitGroupItem = new JMenuItem("Split group");
		splitGroupItem.setMnemonic('p');
		splitGroupItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input the group", "");
				if (ans != null) {
					if (!collinearBlocks.containsKey(ans)) {
						System.err.println("Error: Can't find the group. ");
						return;
					}
					CollinearBlock blockToSplit = collinearBlocks.get(ans);
					int nextID = 1; 
					
					// Put all selected collinear blocks into the target collinear blocks
					VerbosePrinter.println("Existing collinear blocks...");
					for (String name : blockToSplit.groups.keySet()) {
						while (collinearBlocks.containsKey("Block" + nextID))
							nextID++;
						CollinearBlock b = new CollinearBlock("Block" + nextID);
						b.groups.put(name, blockToSplit.groups.get(name));
						collinearBlocks.put(b.name, b);
					}
					collinearBlocks.remove(ans);
					
					VerbosePrinter.println("Reprocess the order of alignments...");
					List<GroupingEntry> entries = CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values()));					
//					MultipleAlignment multipleAlignment = new MultipleAlignment();
//					entries = multipleAlignment.layoutCollinearBlocksNJ(entries, orders);
					collinearBlocks.clear();
					for (CollinearBlock block : CollinearBlock.toCollinearBlocks(entries))
						collinearBlocks.put(block.name, block);
					selectedMoleSet.clear();
					
					VerbosePrinter.println("Updating the view panels...");
					MultipleOpticalMapsView.this.updateData();
				}					
			}
		});
		manipulateMenu.add(splitGroupItem);
		
		JMenuItem ssItem = new JMenuItem("Get single-segment collinear blocks");
		ssItem.setMnemonic('b');
		ssItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				List<CollinearBlock> blocks = CollinearBlock.toSingleSegmentCollinearBlocks(CollinearBlock.toGroupingEntries(new ArrayList<>(collinearBlocks.values())));
				collinearBlocks.clear();
				for (CollinearBlock block : blocks)					
					collinearBlocks.put(block.name, block);
				VerbosePrinter.println("Updating the view panels...");
				MultipleOpticalMapsView.this.updateData();
			}
		});
		manipulateMenu.add(ssItem);

		
		menu.add(manipulateMenu);
	}

	@Override
	public void autoSetSize() {
		if (orders == null) {
			this.setSize(OMView.blankPanelSize);
			this.setPreferredSize(OMView.blankPanelSize);
		}
		else {
			long maxDNALength = 0;
			for (String name : objectListMap.keySet()) {
				long accumulateDNALength = 0;
				for (VObject vobj : objectListMap.get(name)) {
					vobj.autoSetSize();
					accumulateDNALength += vobj.getDNALength();
				}
				if (maxDNALength < accumulateDNALength)
					maxDNALength = accumulateDNALength;
				
				for (int i = 0; i < rearrangementListMap.get(name).size(); i++) {
					rearrangementListMap.get(name).get(i).autoSetSize();
				}
			}
			int heightOfRulerAndRef = (int) (objBorder.y + ruler.getHeight() + (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio);
			int height = (int) (objBorder.y * 2 + heightOfRulerAndRef + (orders.size() * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
			setSize((int) (maxDNALength / dnaRatio * ratio + objBorder.x * 2 + ViewSetting.moleculeNameSize), height);
			setPreferredSize(getSize());
		}
	}

	@Override
	public void reorganize() {	
		if (!(ruler == null || orders == null || objectListMap == null || rearrangementListMap == null)) {

			ruler.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y);
			int height = objBorder.y + ruler.getHeight();
			int index = 1;
			for (String name : orders) {
				long accumulateDNALength = 0;
				for (VObject vobj : objectListMap.get(name)) {
					if (vobj.getWidth() > vobj.getDNALength() / dnaRatio * ratio) {
						if (!(vobj instanceof VSpace))
							System.out.println(vobj.getClass().getSimpleName());
						vobj.setLocation(objBorder.x + ViewSetting.moleculeNameSize + (int) (accumulateDNALength / dnaRatio * ratio + (vobj.getDNALength() / dnaRatio * ratio - vobj.getWidth()) / 2), (int) (height + index * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
					}
					else
						vobj.setLocation(objBorder.x + ViewSetting.moleculeNameSize + (int) (accumulateDNALength / dnaRatio * ratio), (int) (height + index * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
					accumulateDNALength += vobj.getDNALength();
				}
				for (int i = 0; i < rearrangementListMap.get(name).size(); i++)
					rearrangementListMap.get(name).get(i).setLocation(objBorder.x + ViewSetting.moleculeNameSize + (int) (rearrangementPosListMap.get(name).get(i) / dnaRatio * ratio), (int) (height + index * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
				index++;
			}
		}
	}
	
	HashSet<VMultiAlignMolecule> selectedMoleSet = new HashSet<>();
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof VMultiAlignMolecule)
		{			
			VMultiAlignMolecule vmole = (VMultiAlignMolecule) e.getSource();
			if (!selectedMoleSet.contains(vmole)) {
				selectedMoleSet.add(vmole);
				vmole.setSelected(true);
			}
			else {
				selectedMoleSet.remove(vmole);
				vmole.setSelected(false);
			}
				
			
		}	

	}

	
}
