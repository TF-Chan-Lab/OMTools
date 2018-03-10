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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.vobject.VMolecule;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;

public class MoleculeView extends ViewPanel {

	private VRuler ruler;
	private List<VMolecule> vmoleList = new ArrayList<VMolecule>();
	private List<DataNode> dataList = new ArrayList<DataNode>();
	private int page = 0;

	public MoleculeView(OMView mainView) {
		super(mainView);
		this.ruler = new VRuler();
		ruler.setVisible(true);
		ruler.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y);
		this.add(ruler);
		updateData();
		autoSetSize();
	}

	@Override
	protected void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.MOLECULE, new ArrayList<String>());
	}

	@Override
	public void setDNARatio(double dnaRatio) {
		ruler.setDNARatio(dnaRatio);
		for (VMolecule vmole : vmoleList)
			vmole.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}

	@Override
	public void setRatio(double ratio) {
		ruler.setRatio(ratio);
		for (VMolecule vmole : vmoleList)
			vmole.setRatio(ratio);
		super.setRatio(ratio);
	}

	@Override
	public void updateData() {
		super.updateData();		
		updateMolecules();
	}

	private void updateMolecules() {
		LinkedHashMap<String, DataNode> dataMap = mainView.dataModule.getData(dataSelection.get(VDataType.MOLECULE));
		dataList = new ArrayList<DataNode>(dataMap.values());
		if (dataList.isEmpty())
			createEmptyPanel();
		updatePage(1);
	}

	public void updatePage(int page) {
		if (page <= 0 || page > (dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1) {
			throw new IllegalArgumentException("Invalid page");
		}
		this.page = page;
		updateMenu();
		createMolecules(page);
	}

	private void createEmptyPanel() {
		for (VMolecule vmole : vmoleList) {
			this.remove(vmole);
		}
		vmoleList = new ArrayList<VMolecule>();
		ruler.setVisible(false);
	}

	private void createMolecules(int page) {

		for (VMolecule vmole : vmoleList) {
			this.remove(vmole);
		}

		vmoleList = new ArrayList<VMolecule>();
		long maxLength = 0;
		int startIndex = (page - 1) * ViewSetting.maxMoleculeViewItems;
		int endIndex = (page) * ViewSetting.maxMoleculeViewItems;
		if (endIndex > dataList.size())
			endIndex = dataList.size();
		for (int index = startIndex; index < endIndex; index++) {
			DataNode data = dataList.get(index);
			VMolecule vmole = new VMolecule(data);
			vmole.setToolTipText(data.name + ": Signal=" + data.getTotalSignal() + "; Size=" + data.size);

			if (OMView.dataColorMap.containsKey(data.name)) {
				for (int i = 0; i < data.getTotalSegment(); i++)	
					if (OMView.dataColorMap.get(data.name).contains(i))
						vmole.addRegionColor(new SimpleLongLocation(i==0?1:data.refp[i - 1], i==data.getTotalSegment()-1?data.size:data.refp[i]), Color.BLUE);
			}
			vmoleList.add(vmole);
			this.add(vmole);
			if (data.size > maxLength)
				maxLength = data.size;
		}
		ruler.setStartEndPoint(new SimpleLongLocation(1, maxLength));
		ruler.setVisible(true);
		setRatio(ratio);
		setDNARatio(dnaRatio);

	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		int height = objBorder.y + ruler.getHeight();
		int i = 1;
		if (page >= 1) {
			int startIndex = (page - 1) * ViewSetting.maxMoleculeViewItems;
			int endIndex = (page) * ViewSetting.maxMoleculeViewItems;
			if (endIndex > dataList.size())
				endIndex = dataList.size();
			for (int index = startIndex; index < endIndex; index++) {
				DataNode data = dataList.get(index);
				g.drawString(data.name, objBorder.x, (int) (height + i++ * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
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
	protected JMenuItem getGotoMenu() {
		if (dataList == null) {
			JMenuItem gotoPage = new JMenu("Goto");
			gotoPage.setEnabled(false);
			return gotoPage;
		}
		
		JMenuItem gotoPage = new JMenu("Goto");
		JMenuItem previousPage = new JMenuItem("Previous");
		previousPage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePage(page - 1);
			}
		});
		JMenuItem nextPage = new JMenuItem("Next");
		nextPage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePage(page + 1);
			}
		});

		JMenuItem specificPage = new JMenuItem("Page...");
		specificPage.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ans = JOptionPane.showInputDialog(mainView, "Please input page (1-" + ((dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1) + ")", page);
				if (ans != null) 
					try {
						int page = Integer.parseInt(ans);	
						if (page < 1 || (page > (dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1))
							System.err.println("Page out of range");
						else
							updatePage(page);
					} catch (NumberFormatException e) {
						System.err.println("Page should be a number");
					}

				
			}
		});
		if (dataList.isEmpty()) {
			previousPage.setEnabled(false);
			nextPage.setEnabled(false);
			specificPage.setEnabled(false);
		} else {
			if (page <= 1)
				previousPage.setEnabled(false);
			if (page >= (dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1)
				nextPage.setEnabled(false);
		}

		
		gotoPage.add(previousPage);
		gotoPage.add(nextPage);
		gotoPage.add(specificPage);

//		gotoPage.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent arg0) {
//				String ans = JOptionPane.showInputDialog(mainView, "Please input page (1-" + ((dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1) + ")", page);
//				if (ans != null) 
//					try {
//						int page = Integer.parseInt(ans);	
//						if (page < 1 || (page > (dataList.size() - 1) / ViewSetting.maxMoleculeViewItems + 1))
//							System.err.println("Page out of range");
//						else
//							updatePage(page);
//					} catch (NumberFormatException e) {
//						System.err.println("Page should be a number");
//					}
//			}
//		});
		return gotoPage;	
	}

	@Override
	protected void updateMenu() {
		super.updateMenu();
		if (dataList == null)
			return;
		menu.addSeparator();
		JMenu sortMenu = new JMenu("Sort");
		JMenuItem sortBySizeA = new JMenuItem("Size (A)");
		sortBySizeA.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, DataNode.sizecomparator);
				updatePage(1);
			}
		});

		JMenuItem sortBySizeD = new JMenuItem("Size (D)");
		sortBySizeD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, Collections.reverseOrder(DataNode.sizecomparator));
				updatePage(1);
			}
		});
		JMenuItem sortBySignalA = new JMenuItem("Signal (A)");
		sortBySignalA.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, DataNode.signalcomparator);
				updatePage(1);
			}
		});
		JMenuItem sortBySignalD = new JMenuItem("Signal (D)");
		sortBySignalD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, Collections.reverseOrder(DataNode.signalcomparator));
				updatePage(1);
			}
		});
		JMenuItem sortByNameA = new JMenuItem("Name (A)");
		sortByNameA.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, DataNode.namecomparator);
				updatePage(1);
			}
		});
		JMenuItem sortByNameD = new JMenuItem("Name (D)");
		sortByNameD.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Collections.sort(dataList, Collections.reverseOrder(DataNode.namecomparator));
				updatePage(1);
			}
		});
		
		sortMenu.add(sortBySizeA);
		sortMenu.add(sortBySizeD);
		sortMenu.add(sortBySignalA);
		sortMenu.add(sortBySignalD);
		sortMenu.add(sortByNameA);
		sortMenu.add(sortByNameD);
		menu.add(sortMenu);
		
		
		
	}

	@Override
	public void autoSetSize() {
		if (vmoleList.size() == 0) {
			this.setSize(OMView.blankPanelSize);
			setPreferredSize(getSize());
		}
		else {
			int max = 0;
			for (VMolecule vmole : vmoleList)
				if (vmole.getWidth() > max)
					max = vmole.getWidth();
			int heightOfRulerAndRef = (int) (objBorder.y + ruler.getHeight() + (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio);
			int height = (int) (objBorder.y * 2 + heightOfRulerAndRef + (vmoleList.size() * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
			setSize((int) (max + objBorder.x * 2 + ViewSetting.moleculeNameSize), height);
			setPreferredSize(getSize());
		}
	}

	@Override
	public void reorganize() {
		ruler.setLocation(objBorder.x + ViewSetting.moleculeNameSize, objBorder.y);
		int height = objBorder.y + ruler.getHeight();
		int index = 1;
		for (VMolecule vmole : vmoleList) {
			vmole.setLocation(objBorder.x + ViewSetting.moleculeNameSize, (int) (height + index++ * (ViewSetting.moleculeSpace + ViewSetting.bodyHeight) * ratio));
		}
	}

}
