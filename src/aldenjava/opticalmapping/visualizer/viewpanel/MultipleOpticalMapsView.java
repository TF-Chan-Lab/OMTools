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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import aldenjava.common.ElementsGroup;
import aldenjava.common.IntegerKeyFactory;
import aldenjava.common.SimpleLongLocation;
import aldenjava.common.UnweightedRange;
import aldenjava.common.WeightedRange;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.MultipleAlignmentFormat;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.miscellaneous.ProgressPrinter;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockOrder;
import aldenjava.opticalmapping.multiplealignment.GroupingEntry;
//import aldenjava.opticalmapping.multiplealignment.MultipleAlignment;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.utils.VPartialMoleculeInfo;
import aldenjava.opticalmapping.visualizer.vobject.VCoverage;
import aldenjava.opticalmapping.visualizer.vobject.VIndel;
import aldenjava.opticalmapping.visualizer.vobject.VMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VMultiAlignMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VObject;
import aldenjava.opticalmapping.visualizer.vobject.VRearrangement;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;
import aldenjava.opticalmapping.visualizer.vobject.VSpace;
import aldenjava.opticalmapping.visualizer.vobject.VVariability;

public class MultipleOpticalMapsView extends ViewPanel {

	private VRuler ruler;
	private VCoverage vcov;
	private VVariability vvar;
	
	private LinkedHashMap<String, CollinearBlock> collinearBlocks;
	private CollinearBlockOrder orders; // Order of optical map
	private LinkedHashMap<String, Color> colors; // Color options for group
	private List<AnnotationNode> annotations;
	private LinkedHashMap<String, DataNode> dataInfo;
	private LinkedHashMap<String, List<VObject>> objectListMap = new LinkedHashMap<>();
	private LinkedHashMap<String, VMultiAlignMapMolecule> vammMap = new LinkedHashMap<>();
	private LinkedHashMap<String, Point> nameLocations = new LinkedHashMap<>();
	private LinkedHashMap<String, SimpleLongLocation> annotationMap = new LinkedHashMap<>();
	private LinkedHashMap<String, Color> colorMap;
	private List<Integer> groupRowPos;
	private List<Integer> groupRowHeight;
	private Long seed = null;
	private int lineNo = 0;

	LinkedHashMap<String, Long> groupStartPos;
	LinkedHashMap<String, Long> groupDNALengths;
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
		if (!(ruler == null || orders == null || vammMap == null)) {
	
			ruler.setDNARatio(dnaRatio);
			vcov.setDNARatio(dnaRatio);
			vvar.setDNARatio(dnaRatio);
//			for (String name : objectListMap.keySet()) {
//				for (VObject vobj : objectListMap.get(name))
//					vobj.setDNARatio(dnaRatio);
//			}
			for (VMultiAlignMapMolecule vamm : vammMap.values()) {
				vamm.setDNARatio(dnaRatio);
			}	
		}
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		if (!(ruler == null || orders == null || vammMap == null)) {
			ruler.setRatio(ratio);
			vcov.setRatio(ratio);
			vvar.setRatio(ratio);
//			for (String name : objectListMap.keySet()) {
//				for (VObject vobj : objectListMap.get(name))
//					vobj.setRatio(ratio);
//			}
			for (VMultiAlignMapMolecule vamm : vammMap.values()) {
				vamm.setRatio(ratio);
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
		
		if (collinearBlocks != null && orders == null) {
			Set<String> set = new HashSet<>();
			for (CollinearBlock block : collinearBlocks.values())
				set.addAll(block.groups.keySet());
			List<String> deducedOrder = new ArrayList<String>(set);
			orders = new CollinearBlockOrder(deducedOrder);
		}
			
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
		if (vammMap != null)
			for (String name : vammMap.keySet())
				this.remove(vammMap.get(name));
		vammMap.clear();
		colorMap = null;
		
		VerbosePrinter.println("Initializing panels and lists...");
		LinkedHashMap<String, List<VMultiAlignMolecule>> vmoleGroups = new LinkedHashMap<>();
//		LinkedHashMap<String, CoverageInfo> covMap = new LinkedHashMap<>();
		
		objectListMap = new LinkedHashMap<>();
		// Layout of an entry in objectListMap
		// VMolecule, VSpace 1, VSpace 2, VMolecule, VSpace 1, VSpace2, ..... VMolecule, VSpace 1		
		
		annotationMap = new LinkedHashMap<>();
		// Last position on the panel
		LinkedHashMap<String, Long> lastPos = new LinkedHashMap<>();
		// Last block of the contig. Used in adjusting layout.
		LinkedHashMap<String, String> lastBlock = new LinkedHashMap<>();
		// Last signal of the contig. Used to determine whether rearrangement occurs.
		LinkedHashMap<String, Integer> lastSig = new LinkedHashMap<>();
		
		List<String> individualOrder = orders.getIndividualOrder();
		for (String name : individualOrder) {
			objectListMap.put(name, new ArrayList<VObject>());
			lastPos.put(name, 0L);
			lastSig.put(name, -1);
		}
		groupStartPos = new LinkedHashMap<>();
		groupDNALengths = new LinkedHashMap<>();
		
		// The list of previous blocks
		HashMap<String, HashSet<String>> prevBlocks = new HashMap<>();
		HashMap<String, HashSet<String>> nextBlocks = new HashMap<>();
		
		VerbosePrinter.println("Laying out collinear blocks...");
		ProgressPrinter layoutProgress = new ProgressPrinter(collinearBlocks.size(), 10000L);
		for (String group : collinearBlocks.keySet()) {
			LinkedHashMap<String, VPartialMoleculeInfo> map = collinearBlocks.get(group).groups;
			
			// Parse the last block to assign list of previous blocks
			HashSet<String> prevBlockSet = new HashSet<>();
			for (String name : map.keySet()) {
				if (lastBlock.containsKey(name))
					prevBlockSet.add(lastBlock.get(name));
				lastBlock.put(name, group);
			}
			prevBlocks.put(group, prevBlockSet);

			nextBlocks.put(group, new HashSet<String>());
			for (String prevBlock : prevBlockSet)
				nextBlocks.get(prevBlock).add(group);
			// Parse the last block position
			long maxLastPos = Long.MIN_VALUE;
			for (String name : map.keySet())
				if (lastPos.get(name) > maxLastPos)
					maxLastPos = lastPos.get(name);

			// Calculate groupDNALength as the maximum block length 
			long groupDNALength = -1;
			for (String name : map.keySet()) {
				VPartialMoleculeInfo pmi = map.get(name);
				DataNode data = dataInfo.get(name);
				long length = Math.abs(data.refp[pmi.stopSig] - data.refp[pmi.startSig]) + 1;
				if (length > groupDNALength)
					groupDNALength = length;
			}
			groupStartPos.put(group, maxLastPos);
			groupDNALengths.put(group, groupDNALength);
			vmoleGroups.put(group, new ArrayList<VMultiAlignMolecule>());
			for (String name : map.keySet()) {
//				long spacelen = maxLastPos - lastPos.get(name) - 1;
				long spacelen = maxLastPos - lastPos.get(name);
				assert spacelen >= 0;
				VPartialMoleculeInfo pmi = map.get(name);

				if (lastSig.get(name) != -1 && lastSig.get(name) != pmi.startSig) {
					objectListMap.get(name).add(new VRearrangement(spacelen, 0));
				}
				else
					objectListMap.get(name).add(new VIndel(spacelen, 0));

	
					
				DataNode data = new DataNode(dataInfo.get(name));
				GenomicPosNode pos;
				if (!pmi.isReverse()) {
					pos = new GenomicPosNode(data.name, data.refp[pmi.startSig], data.refp[pmi.stopSig]);
				}
				else
					pos = new GenomicPosNode(data.name, data.refp[pmi.stopSig], data.refp[pmi.startSig]);
				
				VMultiAlignMolecule vmole = new VMultiAlignMolecule(data, group, pmi);
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
				
				objectListMap.get(name).add(vmole);
				objectListMap.get(name).add(new VSpace(groupDNALength - length, 0));
				lastSig.put(name, pmi.stopSig);
				
				vmoleGroups.get(group).add(vmole);
		
				lastPos.put(name, lastPos.get(name) + spacelen + groupDNALength);
			}
			layoutProgress.update();
		}
		
		VerbosePrinter.println("Adjusting layout...");
		
		List<Entry<String, List<VMultiAlignMolecule>>> entries = new ArrayList<>(vmoleGroups.entrySet());
		ProgressPrinter adjustLayoutProgress = new ProgressPrinter(entries.size(), 10000L);
		ElementsGroup<Integer, String> connectedBlocks = new ElementsGroup<>(new IntegerKeyFactory());
		
		for (int i = entries.size() - 1; i >= 0; i--) {
			List<VMultiAlignMolecule> vmoles = entries.get(i).getValue();
			String group = entries.get(i).getKey();
			HashMap<Integer, Long> minMoveLengthsWithinGroup = new HashMap<>();

			for (VMultiAlignMolecule vmole : vmoles) {
				List<VObject> objectList = objectListMap.get(vmole.getName());
				int index = objectList.indexOf(vmole);
				// This is the last vmole in the contig, so it is always free to move
				if (index == objectList.size() - 2)
					continue;
				
				// For the vmole that belongs to the set, check their DNA length
				// Get the connectedBlock that the nextVmole belongs to
				VMultiAlignMolecule nextVmole = (VMultiAlignMolecule) objectList.get(index + 3);
				Integer key = connectedBlocks.getElementKey(nextVmole.group);
					
				// Update the length if necessary
				long length = objectList.get(index + 2).getDNALength();
				if (minMoveLengthsWithinGroup.containsKey(key)) {
					if (length < minMoveLengthsWithinGroup.get(key))
						minMoveLengthsWithinGroup.put(key, length);
				}
				else
					minMoveLengthsWithinGroup.put(key, length);
				
			}			
			
			// We want to obtain the maximum length that can be moved across all the groups
			long maxMoveLength = 0;
			for (long moveLength : minMoveLengthsWithinGroup.values())
				if (moveLength > maxMoveLength)
					maxMoveLength = moveLength;
			if (maxMoveLength > 0) {
				for (VMultiAlignMolecule vmole : vmoles) {
					List<VObject> objectList = objectListMap.get(vmole.getName());
					int index = objectList.indexOf(vmole);
					if (index < objectList.size() - 2) {
						// alter the space in front
						VSpace vSpace = (VSpace) objectList.get(index + 2);
						vSpace.setRefDNALength(vSpace.getRefDNALength() - maxMoveLength);
						// alter the space behind
						vSpace = (VSpace) objectList.get(index - 1);
						vSpace.setRefDNALength(vSpace.getRefDNALength() + maxMoveLength);
					}
					else {
						// Only alter the space behind because no more space in front
						VSpace vSpace = (VSpace) objectList.get(index - 1);
						vSpace.setRefDNALength(vSpace.getRefDNALength() + maxMoveLength);
					}
				}

				// Move dependent blocks connected to the group
				for (Integer key : minMoveLengthsWithinGroup.keySet()) {
					// Calculate moveLength						 
					long moveLength = maxMoveLength - minMoveLengthsWithinGroup.get(key);
					assert moveLength >= 0;

					// moveLength == 0 implies the connected blocks do not need to move						
					if (moveLength == 0)
						continue;

					// moveLength > 0 implies we need to shift all related blocks
					Set<String> blocksToMove = connectedBlocks.getGroupElements(key);
					for (String b : blocksToMove) {
						List<VMultiAlignMolecule> vmoleList = vmoleGroups.get(b);
						for (VMultiAlignMolecule vmole : vmoleList) {
							List<VObject> objectList = objectListMap.get(vmole.getName());
							int index = objectList.indexOf(vmole);
							if (index < objectList.size() - 2) {
								// alter the space in front
								VSpace vSpace = (VSpace) objectList.get(index + 2);
								vSpace.setRefDNALength(vSpace.getRefDNALength() - moveLength);
								// alter the space behind
								vSpace = (VSpace) objectList.get(index - 1);
								vSpace.setRefDNALength(vSpace.getRefDNALength() + moveLength);
							}
							else {
								// Only alter the space behind because no more space in front
								VSpace vSpace = (VSpace) objectList.get(index - 1);
								vSpace.setRefDNALength(vSpace.getRefDNALength() + moveLength);
							}
						}
					}
				}
			}

			// Update connected blocks
			HashSet<String> connectedBlock = new HashSet<>(nextBlocks.get(group));
			connectedBlock.add(group);
			connectedBlocks.add(connectedBlock);
			adjustLayoutProgress.update();
		}
		
		// Update blocks starting positions
		VerbosePrinter.println("Updating starting positions of blocks...");
		LinkedHashMap<String, Long> startPos = new LinkedHashMap<>();
		// Remove initial vspace and take it as the starting point
		for (String name : individualOrder) {
			VObject vobj = objectListMap.get(name).remove(0);
			assert vobj instanceof VSpace;
			startPos.put(name, vobj.getDNALength());
		}		
		long minStartPos = Long.MAX_VALUE;
		for (long currentStartPos : startPos.values())
			if (currentStartPos < minStartPos)
				minStartPos = currentStartPos;
		if (minStartPos > 0)
			for (String name : individualOrder)
				startPos.put(name, startPos.get(name) - minStartPos);
		
		// Update color
		VerbosePrinter.println("Updating block color...");
		resetVMoleColor();
		
		VerbosePrinter.println("Creating blocks...");
		
		for (String name : individualOrder) {
			vammMap.put(name, new VMultiAlignMapMolecule(name, objectListMap.get(name), startPos.get(name)));
		}
		
		for (String name : individualOrder) {
			this.add(vammMap.get(name));
			vammMap.get(name).addMouseListener(this);
			vammMap.get(name).setVisible(true);
		}
		// Add the vcomponents, set mouse listener and visibility
	/*
		for (String name : individualOrder) {
			
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					this.add(vobj);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					this.add(vobj);
				}
		}
		for (String name : individualOrder) {
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					vobj.addMouseListener(this);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					vobj.addMouseListener(this);
				}
		}
		for (String name : individualOrder) {
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VSpace) {
					vobj.setVisible(true);
				}
			for (VObject vobj : objectListMap.get(name)) 
				if (vobj instanceof VMolecule) {
					vobj.setVisible(true);
				}
		}
*/
		VerbosePrinter.println("Adding ruler...");
		// Calculate ruler position
		long maxLastPos = 0;
		for (String name : lastPos.keySet())
			if (lastPos.get(name) > maxLastPos)
				maxLastPos = lastPos.get(name);

		ruler.setStartEndPoint(new SimpleLongLocation(1, maxLastPos));
		ruler.setVisible(true);
		
		
		VerbosePrinter.println("Parsing coverage...");
		

//		List<SimpleLongLocation> preIndelCovs = new ArrayList<>();
//		for (String name : individualOrder) {
//			long pos = startPos.get(name);
//			for (VObject vobj : vammMap.get(name).objectList) {
//				if (vobj instanceof VIndel || vobj instanceof VRearrangement)
//					if (vobj.getDNALength() > 0)
//						preIndelCovs.add(new SimpleLongLocation(pos, pos + vobj.getDNALength() - 1));
//				pos += vobj.getDNALength();
//			}
//		}
//		List<ExtendedLongLocation> indelCovs = ExtendedLongLocation.merge(ExtendedLongLocation.extendSimpleLongLocation(preIndelCovs, 1));
//		
//		List<ExtendedLongLocation> preBlockCovs = new ArrayList<>();
//		for (Entry<String, CoverageInfo> cov : covMap.entrySet()) {
//			preBlockCovs.add(new ExtendedLongLocation(cov.getValue().startPos, cov.getValue().startPos + cov.getValue().length - 1, cov.getValue().count));
//		}
//		List<ExtendedLongLocation> blockCovs = ExtendedLongLocation.merge(preBlockCovs);
//		
//		List<ExtendedLongLocation> preCombinedCovs = new ArrayList<>(); 
//		preCombinedCovs.addAll(indelCovs);
//		preCombinedCovs.addAll(blockCovs);
//		List<ExtendedLongLocation> combinedCovs = ExtendedLongLocation.merge(preCombinedCovs);
//		
//		vcov = new VCoverage(combinedCovs);
//		vcov.setStartEndPoint(new SimpleLongLocation(1, maxLastPos));
//		this.add(vcov);
//		
//		VerbosePrinter.println("Parsing variations...");
//		List<SimpleLongLocation> preBlockTypes = new ArrayList<>();
//		preBlockTypes.addAll(ExtendedLongLocation.toSimpleLongLocation(indelCovs)); // All indels and rearrangements are counted as one type of "block", in parallel to other blocks
//		preBlockTypes.addAll(ExtendedLongLocation.toSimpleLongLocation(preBlockCovs));
//		List<ExtendedLongLocation> blockTypes = ExtendedLongLocation.merge(ExtendedLongLocation.extendSimpleLongLocation(preBlockTypes, 1));
		
		LinkedHashMap<String, Long> blocksStartPos = new LinkedHashMap<>();
		List<UnweightedRange<Long>> preIndelCovs = new ArrayList<>();
		for (String name : individualOrder) {
			long pos = startPos.get(name);
			for (VObject vobj : vammMap.get(name).objectList) {
				if (vobj instanceof VIndel || vobj instanceof VRearrangement) {
					if (vobj.getDNALength() > 0) {
						preIndelCovs.add(new UnweightedRange<Long>(pos, pos + vobj.getDNALength() - 1));
					}
				}
				else
					if (vobj instanceof VMultiAlignMolecule) {
						VMultiAlignMolecule vmole = (VMultiAlignMolecule) vobj;
						blocksStartPos.put(vmole.group, pos);
					}
				pos += vobj.getDNALength();
			}
		}
		groupStartPos = blocksStartPos;
		List<WeightedRange<Long, Integer>> indelCovs = WeightedRange.mergeWeightedRange(WeightedRange.assignWeightToUweightedRange(preIndelCovs, 1));
		List<WeightedRange<Long, Integer>> preBlockCovs = new ArrayList<>();
		for (String group : vmoleGroups.keySet()) {
			long maxlen = 0;
			for (VMultiAlignMolecule vmole : vmoleGroups.get(group))
				if (vmole.getDNALength() > maxlen)
					maxlen = vmole.getDNALength(); 
			preBlockCovs.add(new WeightedRange<Long, Integer>(blocksStartPos.get(group), blocksStartPos.get(group) + maxlen - 1, vmoleGroups.get(group).size()));
		}
//			
//		for (Entry<String, CoverageInfo> cov : covMap.entrySet()) {
//			preBlockCovs.add(new WeightedRange<Long, Integer>(cov.getValue().startPos, cov.getValue().startPos + cov.getValue().length - 1, cov.getValue().count));
//		}
		List<WeightedRange<Long, Integer>> blockCovs = WeightedRange.mergeWeightedRange(preBlockCovs);
		
		List<WeightedRange<Long, Integer>> preCombinedCovs = new ArrayList<>(); 
		preCombinedCovs.addAll(indelCovs);
		preCombinedCovs.addAll(blockCovs);
		List<WeightedRange<Long, Integer>> combinedCovs = WeightedRange.mergeWeightedRange(preCombinedCovs);
		
		vcov = new VCoverage(combinedCovs);
		vcov.setStartEndPoint(new SimpleLongLocation(1, maxLastPos));
		this.add(vcov);
		
		VerbosePrinter.println("Parsing variations...");
		
		List<UnweightedRange<Long>> preBlockTypes = new ArrayList<>();
		preBlockTypes.addAll(WeightedRange.toUnweightedRange(indelCovs)); // All indels and rearrangements are counted as one type of "block", in parallel to other blocks
//		for (WeightedRange r : indelCovs)
//			System.out.println(r.min + "\t" + r.max + "\t" + r.weight);
		preBlockTypes.addAll(WeightedRange.toUnweightedRange(preBlockCovs));
		List<WeightedRange<Long, Integer>> blockTypes = WeightedRange.mergeWeightedRange(WeightedRange.assignWeightToUweightedRange(preBlockTypes, 1));
		
		List<WeightedRange<Long, Double>> variabilities = new ArrayList<>();
		int typeIndex = 0;
		int covIndex = 0;
		long lastVariabilityPos = 0;
		while (covIndex < combinedCovs.size() && typeIndex < blockTypes.size()) {
			
			
			// Find the next combinedCov that meets the current blockType
			while (combinedCovs.get(covIndex).max < blockTypes.get(typeIndex).min) {
				covIndex++;
				assert covIndex < combinedCovs.size();
			}
			// Now combinedCov and blockType should overlap
			
			// Move the lastVariabilityPos to the overlapping region
			if (lastVariabilityPos < blockTypes.get(typeIndex).min)
				lastVariabilityPos = blockTypes.get(typeIndex).min - 1;
			
			// Finding the range, with the stop point as the smaller max of combinedCov and blockType
			long start = lastVariabilityPos + 1;
			long stop;
			if (combinedCovs.get(covIndex).max < blockTypes.get(typeIndex).max) {
				stop = combinedCovs.get(covIndex).max;
			}
			else {
				stop = blockTypes.get(typeIndex).max;
			}
			double weight = 1 - blockTypes.get(typeIndex).weight / (double) combinedCovs.get(covIndex).weight;
			weight = 1 - 1 / (double) blockTypes.get(typeIndex).weight;
			weight = blockTypes.get(typeIndex).weight;
//			System.out.println(start + "\t" + stop + "\t" + blockTypes.get(typeIndex).weight + "\t" + combinedCovs.get(covIndex).weight + "\t" + weight);
			variabilities.add(new WeightedRange<Long, Double>(start, stop, weight));
			
			lastVariabilityPos = stop;
			if (blockTypes.get(typeIndex).max <= lastVariabilityPos)
				typeIndex++;
			if (combinedCovs.get(covIndex).max <= lastVariabilityPos)
				covIndex++;			
			
		}
		variabilities = WeightedRange.stitchWeightedRange(variabilities);
		vvar = new VVariability(variabilities);
		for (WeightedRange<Long, Integer> blockType : blockTypes)
			if (blockType.weight > ViewSetting.maxVariableBlockTypes)
				blockType.weight = ViewSetting.maxVariableBlockTypes;
//		vvar = new VCoverage(blockTypes);
		vvar.setStartEndPoint(new SimpleLongLocation(1, maxLastPos));
		this.add(vvar);
		
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
				boolean useVariationColor = false;
				if (useVariationColor) {
					HashSet<String> involved = new HashSet<>();
					for (String key : collinearBlocks.get(group).groups.keySet())
						involved.add(orders.assignedOrderMap.get(key));
//					float r = ((orders.orderMap.size() - involved.size())) / (float) orders.orderMap.size();
//					float g = (involved.size()) / (float) orders.orderMap.size();
//					float r = (float) (Math.log((orders.orderMap.size() - involved.size())==0?1:(orders.orderMap.size() - involved.size())) / Math.log(orders.orderMap.size()));
//					float g = (float) (Math.log(involved.size()) / Math.log(orders.orderMap.size()));
//					float r = ((orders.orderMap.size() - involved.size())) / (float) orders.orderMap.size();
//					float g = 0;
//					float b = 0;
//					color = new Color(r, g, b);
					
					float r = rand.nextFloat() / 2f + 0.5f;
					float g = rand.nextFloat() / 2f + 0.5f;
					float b = rand.nextFloat() / 2f + 0.5f;
//					float a =  1 - (float) (Math.log((orders.orderMap.size() - involved.size())==0?1:(orders.orderMap.size() - involved.size())) / Math.log(orders.orderMap.size()));
					int bin = 4;
					
					float a = 1 - ((involved.size() - 1) * bin / orders.orderMap.size() + 1) / (float) bin;
					color = new Color(r, g, b, a);
//					
				}
				else {
					float r = rand.nextFloat() / 2f + 0.5f;
					float g = rand.nextFloat() / 2f + 0.5f;
					float b = rand.nextFloat() / 2f + 0.5f;
					color = new Color(r, g, b);
				}
			}
			colorMap.put(group, color);
		}
		this.colorMap = colorMap;
		for (String name : orders.getIndividualOrder())
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
		if (orders == null)
			return;
		
		
		// Draw group bg color
		for (int i = 0; i < groupRowPos.size(); i++) {
			if (i % 2 == 0)
				g.setPaint(ViewSetting.maBGColor1);
			else
				g.setPaint(ViewSetting.maBGColor2);
			g.fillRect(0, groupRowPos.get(i), this.getWidth(), groupRowHeight.get(i));
		}

		
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
			for (String name : orders.getIndividualOrder()) {
				g.drawString(name, nameLocations.get(name).x, nameLocations.get(name).y);
			}
		
		// Draw the grey background rectangles
		/*
		int headerHeight = objBorder.y + ruler.getHeight() + vcov.getHeight() + vvar.getHeight() + ViewSetting.bodyHeight;
		g.setPaint(Color.GRAY);
		for (int i = 0; i < lineNo; i++)
			g.fillRect(objBorder.x, (int) (headerHeight + i * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio), (int) (ruler.getDNALength() / dnaRatio * ratio), (int) (ViewSetting.bodyHeight * ratio));
		*/
		
		// Draw annotations
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
	protected JMenuItem getGotoMenu() {
		JMenuItem gotoPage = new JMenuItem("Goto...");
		gotoPage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input molecule: ");
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
	@Override
	protected void updateMenu() {
		super.updateMenu();
		menu.addSeparator();
		/*
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
		*/
		
		JMenu saveMenu = new JMenu("Save");
		saveMenu.setMnemonic('v');
		/*
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
		*/
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
		/*
		JMenuItem importVariabilityItem = new JMenuItem("Import variability");
		importVariabilityItem.addActionListener(e -> {
			JFileChooser importVariabilityChooser = new JFileChooser();
			
			importVariabilityChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			importVariabilityChooser.setAcceptAllFileFilterUsed(false);
			importVariabilityChooser.setMultiSelectionEnabled(false);
			importVariabilityChooser.setDialogTitle("Load variability");
			importVariabilityChooser.setCurrentDirectory(mainView.workingDirectory);
			int selection = importVariabilityChooser.showOpenDialog(mainView);
			if (selection == JFileChooser.APPROVE_OPTION)
			{
				mainView.workingDirectory = importVariabilityChooser.getCurrentDirectory();
				String path = importVariabilityChooser.getSelectedFile().getAbsolutePath();
				try {
					List<WeightedRange<Long, Double>> variabilities = new ArrayList<>();
					List<String> list = ListExtractor.extractList(path);
					for (String s : list) {
						String[] l = s.split("\\t");
						String blockName = l[0];
						double weight = Double.parseDouble(l[1]);
						long start = groupStartPos.get(blockName);
						long stop = groupStartPos.get(blockName) + groupDNALengths.get(blockName) - 1;
						variabilities.add(new WeightedRange<Long, Double>(start, stop, weight));
					}
					variabilities = WeightedRange.mergeWeightedRange(variabilities);
					vvar.setVariabilities(variabilities);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
					
				
				
			}

			
		});		
		countMenu.add(importVariabilityItem);
		
		JMenuItem importVariabilityItem2 = new JMenuItem("Import variability 2");
		importVariabilityItem2.addActionListener(e -> {
			JFileChooser importVariabilityChooser = new JFileChooser();
			
			importVariabilityChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			importVariabilityChooser.setAcceptAllFileFilterUsed(false);
			importVariabilityChooser.setMultiSelectionEnabled(false);
			importVariabilityChooser.setDialogTitle("Load variability");
			importVariabilityChooser.setCurrentDirectory(mainView.workingDirectory);
			int selection = importVariabilityChooser.showOpenDialog(mainView);
			if (selection == JFileChooser.APPROVE_OPTION)
			{
				mainView.workingDirectory = importVariabilityChooser.getCurrentDirectory();
				String path = importVariabilityChooser.getSelectedFile().getAbsolutePath();
				try {
					List<WeightedRange<Long, Double>> variabilities = new ArrayList<>();
					List<String> list = ListExtractor.extractList(path);
					for (String s : list) {
						String[] l = s.split("\\t");
						long start = Long.parseLong(l[0]);
						long stop = Long.parseLong(l[1]);
						double weight = Double.parseDouble(l[2]);
						variabilities.add(new WeightedRange<Long, Double>(start, stop, weight));
					}
//					variabilities = WeightedRange.mergeWeightedRange(variabilities);
					vvar.setVariabilities(variabilities);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
					
				
				
			}

			
		});		
		countMenu.add(importVariabilityItem2);

		JMenuItem coordinateItem = new JMenuItem("Block coordinate");
		coordinateItem.addActionListener(e -> {
			JFileChooser saveBlockCountChooser = new JFileChooser();
			
			saveBlockCountChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			saveBlockCountChooser.setAcceptAllFileFilterUsed(false);
			saveBlockCountChooser.setMultiSelectionEnabled(false);
			saveBlockCountChooser.setDialogTitle("Save block coordinate");
			saveBlockCountChooser.setCurrentDirectory(mainView.workingDirectory);
			int selection = saveBlockCountChooser.showSaveDialog(mainView);
			if (selection == JFileChooser.APPROVE_OPTION)
			{
				mainView.workingDirectory = saveBlockCountChooser.getCurrentDirectory();
				String path = saveBlockCountChooser.getSelectedFile().getAbsolutePath();
				try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(path));
					groupStartPos.forEach((k,v)-> {
						try {
							bw.write(k + ":" + v + "-" + (v + groupDNALengths.get(k) - 1) + "\n");
						} catch (IOException e2) {
							e2.printStackTrace();
						}
					});
					bw.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				
			}

			
		});		
		countMenu.add(coordinateItem);
		*/
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
				LinkedHashMap<String, CollinearBlock> newCollinearBlocks = new LinkedHashMap<>();
				List<String> keys = new ArrayList<>(collinearBlocks.keySet());
				for (int i = keys.size() - 1; i >= 0; i--) {
					String key = keys.get(i);
					CollinearBlock block = collinearBlocks.get(key);
					block.reverse();
					newCollinearBlocks.put(key, block);
				}
				collinearBlocks = newCollinearBlocks;
				selectedMoleSet.clear();
				VerbosePrinter.println("Updating the view panels...");
				MultipleOpticalMapsView.this.createMolecules();
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
					MultipleOpticalMapsView.this.createMolecules();
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
					MultipleOpticalMapsView.this.createMolecules();
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
//					entries = multipleAlignment.layoutCollinearBlocksNJ(entries, new ArrayList<String>()).stream().flatMap(chain -> chain.groupingEntries.stream()).collect(Collectors.toList());
					collinearBlocks.clear();
					for (CollinearBlock block : CollinearBlock.toCollinearBlocks(entries))
						collinearBlocks.put(block.name, block);
					selectedMoleSet.clear();
					
					VerbosePrinter.println("Updating the view panels...");
					MultipleOpticalMapsView.this.createMolecules();
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
				MultipleOpticalMapsView.this.createMolecules();
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
//			for (String name : objectListMap.keySet()) {
//				long accumulateDNALength = 0;
//				for (VObject vobj : objectListMap.get(name)) {
//					vobj.autoSetSize();
//					accumulateDNALength += vobj.getDNALength();
//				}
//				if (maxDNALength < accumulateDNALength)
//					maxDNALength = accumulateDNALength;
//			}

			this.vvar.autoSetSize();
			this.vcov.autoSetSize();
			this.ruler.autoSetSize();
			
			for (String name : vammMap.keySet()) {
				vammMap.get(name).autoSetSize();
				long len = vammMap.get(name).startingPos + vammMap.get(name).getDNALength();
				if (len > maxDNALength)
					maxDNALength = len;
			}
			int heightOfRulerAndRef = (int) (objBorder.y + vvar.getHeight() + vcov.getHeight() + ruler.getHeight() + (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio);
			int height = (int) (objBorder.y * 2 + heightOfRulerAndRef + (lineNo * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
			setSize((int) (maxDNALength / dnaRatio * ratio + objBorder.x * 2 + ViewSetting.moleculeNameSize), height);
			setPreferredSize(getSize());
		}
	}

	@Override
	public void reorganize() {	
		if (!(ruler == null || orders == null || vammMap.isEmpty())) {

			nameLocations = new LinkedHashMap<>();
			vvar.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y);
			vcov.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y + vvar.getHeight());
			ruler.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y + vvar.getHeight() + vcov.getHeight());
			int headerHeight = objBorder.y + ruler.getHeight() + vcov.getHeight() + vvar.getHeight() + ViewSetting.bodyHeight;
			int lineNo = 0;
			List<Integer> groupRowPos = new ArrayList<>();
			List<Integer> groupRowHeight = new ArrayList<>();
			for (String groupName : orders.orderMap.keySet()) {
				List<Long> currentPosList = new ArrayList<>();
				List<String> individualNames = orders.orderMap.get(groupName);
				List<VMultiAlignMapMolecule> list = new ArrayList<>();
				for (String individualName : individualNames) 
					list.add(vammMap.get(individualName));
				
				Collections.sort(list);
				
				for (VMultiAlignMapMolecule vamm : list) {
					vamm.reorganize();
					int index = -1;
					for (int i = 0; i < currentPosList.size(); i++)
						if (vamm.startingPos - currentPosList.get(i) >= 0) {
							index = i;
							currentPosList.set(i, vamm.startingPos + vamm.getDNALength());
							break;
						}
					if (index == -1) {
						index = currentPosList.size();
						currentPosList.add(vamm.startingPos + vamm.getDNALength());
					}
					
					int x = objBorder.x + ViewSetting.moleculeNameSize + (int) (vamm.startingPos / dnaRatio * ratio);
					int y = (int) (headerHeight + (lineNo + index) * (ViewSetting.maMoleculeSpace + ViewSetting.bodyHeight) * ratio);
					vamm.setLocation(x, y);
					nameLocations.put(vamm.name, new Point(x, y));
				}
				groupRowPos.add((int) (headerHeight + ((lineNo) * (ViewSetting.maMoleculeSpace + ViewSetting.bodyHeight) - ViewSetting.moleculeSpace) * ratio));
				groupRowHeight.add((int) (currentPosList.size() * (ViewSetting.maMoleculeSpace + ViewSetting.bodyHeight) * ratio));
				lineNo += currentPosList.size();
			}
			
			this.lineNo = lineNo;
			this.groupRowPos = groupRowPos;
			this.groupRowHeight = groupRowHeight;
			/*
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
			}
			*/
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

class VMultiAlignMapMolecule extends VObject implements Comparable<VMultiAlignMapMolecule> {
	String name;
	List<VObject> objectList;
	Long startingPos;
	public VMultiAlignMapMolecule(String name, List<VObject> objectList, Long startingPos) {
		this.name = name;
		this.objectList = objectList;
		this.startingPos = startingPos;
		// Add VSpace first so that they always appear in front of VMolecule 
		for (VObject vobj : objectList)
			if (vobj instanceof VSpace)
				this.add(vobj);
		for (VObject vobj : objectList)
			if (vobj instanceof VMolecule)
				this.add(vobj);
	}
	@Override
	public int compareTo(VMultiAlignMapMolecule p) {
		return Long.compare(this.startingPos, p.startingPos);
	}
	@Override
	public long getDNALength() {
		long len = 0;
		for (VObject vobj : objectList)
			len += vobj.getDNALength();
		return len;
	}
	@Override
	public void setDNARatio(double dnaRatio) {
		for (VObject vobj : objectList)
			vobj.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		for (VObject vobj : objectList)
			vobj.setRatio(ratio);
		super.setRatio(ratio);
	}
	
	@Override
	public void autoSetSize() {
		for (VObject vobj : objectList)
			vobj.autoSetSize();
		this.setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.bodyHeight * ratio));
	}
	
	@Override
	public void reorganize() {
		long accumulateDNALength = 0;
		for (VObject vobj : objectList) {
			vobj.reorganize();
			// If object size has larger size than DNA length, put it in the middle
			if (vobj.getWidth() > (int) (vobj.getDNALength() / dnaRatio * ratio)) { 
				vobj.setLocation((int) (accumulateDNALength / dnaRatio * ratio + (vobj.getDNALength() / dnaRatio * ratio - vobj.getWidth()) / 2), 0);
			}
			else
				vobj.setLocation((int) (accumulateDNALength / dnaRatio * ratio), 0);
			accumulateDNALength += vobj.getDNALength();
		}
	}
}

class CoverageInfo {
	int count;
	long startPos;
	long length;
	public CoverageInfo(int count, long startPos, long length) {
		super();
		this.count = count;
		this.startPos = startPos;
		this.length = length;
	}
	
}