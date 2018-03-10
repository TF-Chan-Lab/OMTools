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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
import aldenjava.opticalmapping.miscellaneous.RandomSeed;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockOrder;
import aldenjava.opticalmapping.multiplealignment.GroupingEntry;
import aldenjava.opticalmapping.multiplealignment.MultipleAlignment;
import aldenjava.opticalmapping.multiplealignment.BlockInfo;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.vobject.VBlockSpace;
import aldenjava.opticalmapping.visualizer.vobject.VCoverage;
import aldenjava.opticalmapping.visualizer.vobject.VMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VMultiAlignMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VObject;
import aldenjava.opticalmapping.visualizer.vobject.VRearrangement;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;
import aldenjava.opticalmapping.visualizer.vobject.VSpace;
import aldenjava.opticalmapping.visualizer.vobject.VVariability;

public class MultipleOpticalMapsBlockView extends ViewPanel {
	private LinkedHashMap<String, DataNode> dataInfo;
	private LinkedHashMap<String, CollinearBlock> collinearBlocks;
	private CollinearBlockOrder orders; // Order of optical map
	private LinkedHashMap<String, Color> colors; // Color options for group
	private LinkedHashMap<String, Color> colorMap;
	private HashMap<String, Integer> sortingMap; // A processed map for queries
	private VRuler ruler;
	private LinkedHashMap<String, VMolecule> vmoleMap = new LinkedHashMap<String, VMolecule>();


	public MultipleOpticalMapsBlockView(OMView mainView) {
		super(mainView);
		this.ruler = new VRuler();
		ruler.setVisible(false);
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
		
		// Deduce the order if it is not provided
		if (collinearBlocks != null && orders == null) {
			Set<String> set = new LinkedHashSet<>();
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
		clear();
		
		long maxLength = 0;
		for (String query : orders.getIndividualOrder()) {
			DataNode data = dataInfo.get(query);
			VMolecule vmole = new VMolecule(data);
			vmoleMap.put(query, vmole);			
			this.add(vmole);
			if (data.size > maxLength)
				maxLength = data.size;
		}
		
		ruler.setStartEndPoint(new SimpleLongLocation(1, maxLength));
		ruler.setVisible(true);

		sortingMap = new HashMap<>();
		int priority = 1;
		for (String query : orders.getIndividualOrder())
			sortingMap.put(query, priority++);

		setRatio(ratio);
		setDNARatio(dnaRatio);

		resetVMoleColor();
	}
	
	private void clear() {
		// Remove previous molecules and create new molecules
		for (VMolecule vmole : vmoleMap.values()) {
			this.remove(vmole);
		}
		vmoleMap = new LinkedHashMap<>();
		sortingMap = new HashMap<>();
		ruler.setVisible(false);
		this.repaint();
	}
	
	private void resetVMoleColor() {
		// Process color
		LinkedHashMap<String, Color> colorMap = new LinkedHashMap<>();
		long seed = RandomSeed.getSeed();
		Random rand = new Random(seed);
		for (String group : collinearBlocks.keySet()) {
			Color color; 
			if (colors != null && colors.containsKey(group))
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
			
		
		for (VMolecule vmole : vmoleMap.values()) {
			vmole.setBaseColor(Color.white);
		}
		for (String group : collinearBlocks.keySet()) {
			CollinearBlock block = collinearBlocks.get(group);
			Color color = colorMap.get(group);
			// Sort by order
			List<String> queries = new ArrayList<>(block.groups.keySet());
			queries.retainAll(sortingMap.keySet());
			Collections.sort(queries, ((String q1, String q2) -> Integer.compare(sortingMap.get(q1), sortingMap.get(q2))));
			
			for (String query : queries) {
				GenomicPosNode region = block.groups.get(query).getRegion(dataInfo.get(query));
				vmoleMap.get(query).addRegionColor(new SimpleLongLocation(region.start, region.stop), color);
			}
		}
		this.repaint();
	}
	
	@Override
	public void setDNARatio(double dnaRatio) {
		ruler.setDNARatio(dnaRatio);
		for (VMolecule vmole : vmoleMap.values())
			vmole.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		ruler.setRatio(ratio);
		for (VMolecule vmole : vmoleMap.values())
			vmole.setRatio(ratio);
		super.setRatio(ratio);
	}

	@Override
	public void updateData() {
		super.updateData();		
		updateMolecules();
	}

	private void createEmptyPanel() {
		clear();
	}


	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		
		g.setStroke(new BasicStroke((float) (ViewSetting.blockConnectionLineWidth * ratio)));
		if (vmoleMap.isEmpty())
			return;
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
		for (String query : orders.getIndividualOrder()) {
			g.drawString(query, objBorder.x, (int) (objBorder.y + ruler.getHeight() + sortingMap.get(query) * (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio));
		}

		
		for (String group : collinearBlocks.keySet()) {
			CollinearBlock block = collinearBlocks.get(group);
			g.setColor(colorMap.get(group));
			// Sort by order
			List<String> queries = new ArrayList<>(block.groups.keySet());
			queries.retainAll(sortingMap.keySet());
			Collections.sort(queries, ((String q1, String q2) -> Integer.compare(sortingMap.get(q1), sortingMap.get(q2))));
			
			Point2D.Double prevP = null;
			// Create lines
			for (String query : queries) {
				GenomicPosNode region = block.groups.get(query).getRegion(dataInfo.get(query));
				Point2D.Double p = new Point2D.Double(objBorder.x + (region.start + region.stop) / 2.0 * ratio / dnaRatio, objBorder.y + ruler.getHeight() + (sortingMap.get(query) * (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio));
				if (prevP != null)
					g.draw(new Line2D.Double(prevP, p));
				prevP = new Point2D.Double(p.x, p.y + ViewSetting.bodyHeight * ratio);
			}
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
	public void autoSetSize() {
		if (vmoleMap.size() == 0) {
			this.setSize(OMView.blankPanelSize);
			setPreferredSize(getSize());
		}
		else {
			int max = 0;
			for (VMolecule vmole : vmoleMap.values())
				if (vmole.getWidth() > max)
					max = vmole.getWidth();
			int heightOfRulerAndRef = (int) (objBorder.y + ruler.getHeight() + (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio);
			int height = (int) (objBorder.y * 2 + heightOfRulerAndRef + (vmoleMap.size() * (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio));
			setSize((int) (max + objBorder.x * 2), height);
			setPreferredSize(getSize());
		}
	}

	@Override
	public void reorganize() {
		ruler.setLocation(objBorder.x, objBorder.y);
		int height = objBorder.y + ruler.getHeight();
		for (VMolecule vmole : vmoleMap.values()) {
			vmole.setLocation(objBorder.x, (int) (height + sortingMap.get(vmole.getData().name) * (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio));
		}
	}

	@Override
	protected JMenuItem getGotoMenu() {
		JMenu gotoMenu = new JMenu("Goto...");
		
		JMenuItem gotoMolecule = new JMenuItem("Molecule");
		gotoMolecule.setMnemonic('M');
		gotoMolecule.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input molecule: ");
				if (ans != null) {
					if (orders.getIndividualOrder().contains(ans))
						navigateViewPortFromGoto(new Point(objBorder.x, (int) (objBorder.y + ruler.getHeight() + sortingMap.get(ans) * (ViewSetting.mabMoleculeSpace + ViewSetting.bodyHeight) * ratio)));
					else
						JOptionPane.showMessageDialog(mainView, "Alignment of molecule " + ans + " is not found in this region.");
				}
			}
		});
		gotoMenu.add(gotoMolecule);
		return gotoMenu;
	}

}