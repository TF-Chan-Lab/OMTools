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


package aldenjava.opticalmapping.visualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.GenomicPosNode;
//import aldenjava.opticalmapping.OMTools;
import aldenjava.opticalmapping.application.svdetection.StandardSVNode;
import aldenjava.opticalmapping.application.svdetection.StandardSVReader;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.data.MultipleAlignmentFormat;
import aldenjava.opticalmapping.data.annotation.AnnotationFormat;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.annotation.BEDNode;
import aldenjava.opticalmapping.data.annotation.BEDReader;
import aldenjava.opticalmapping.data.annotation.GFFNode;
import aldenjava.opticalmapping.data.annotation.GFFReader;
import aldenjava.opticalmapping.data.annotation.GVFNode;
import aldenjava.opticalmapping.data.annotation.GVFReader;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.data.mappingresult.ResultFormat;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockReader;
import aldenjava.opticalmapping.visualizer.viewpanel.AlignmentControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.AlignmentView;
import aldenjava.opticalmapping.visualizer.viewpanel.AnchorControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.AnchorView;
import aldenjava.opticalmapping.visualizer.viewpanel.ControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.ImageSaveFormat;
import aldenjava.opticalmapping.visualizer.viewpanel.MoleculeControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.MoleculeView;
import aldenjava.opticalmapping.visualizer.viewpanel.MultipleOpticalMapsControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.MultipleOpticalMapsView;
import aldenjava.opticalmapping.visualizer.viewpanel.RegionalControlPanel;
import aldenjava.opticalmapping.visualizer.viewpanel.RegionalView;
import aldenjava.opticalmapping.visualizer.viewpanel.ViewSaver;
import aldenjava.opticalmapping.visualizer.vobject.VRuler;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * The main class for OMView.
 * @author Alden
 *
 */
public class OMView extends JFrame implements PropertyChangeListener {

	
//	private final static String aboutMessage = "Program Version: " + OMTools.version + "\nAuthor: " + OMTools.author;
	private final static String aboutMessage = "Program Version: ";
	public final static Dimension blankPanelSize = new Dimension(2000, 2000);
	
	public File workingDirectory = new File(System.getProperty("user.dir"));
	public DataModule dataModule;
	
	public ControlPanel controlPanel; // The current control panel
	public JTabbedPane tabPanel;
	public StatusPanel statusPanel;
	
	public List<SwingWorker<? extends Object, ? extends Object>> taskList = new ArrayList<SwingWorker<? extends Object, ? extends Object>>();
	
	public OMView()
	{
		super("OMView - Optical Mapping Visualizer");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setSize(1000, 1000);
		this.setPreferredSize(new Dimension(1000, 1000));
		this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		
		initMenuBar();
		this.dataModule = new DataModule(this);
		this.statusPanel = new StatusPanel();
		initTabPanel();
		new IOLoader().execute();
		this.add(statusPanel, BorderLayout.SOUTH);
		this.setTransferHandler(handler);
		this.revalidate();
	}

	
	public void changeMenuEnabled(String viewPanel) {
		switch (viewPanel) {
			case "": 
				setRegionItem.setEnabled(false);
				enlargeRegionItem.setEnabled(false);
				setMoleculeItem.setEnabled(false);	
				setAnchorItem.setEnabled(false);
				break;
			case "Regional view":
				setRegionItem.setEnabled(true);
				enlargeRegionItem.setEnabled(true);
				setMoleculeItem.setEnabled(false);	
				setAnchorItem.setEnabled(false);
				break;
			case "Alignment view":
				setRegionItem.setEnabled(false);
				enlargeRegionItem.setEnabled(false);
				setMoleculeItem.setEnabled(true);	
				setAnchorItem.setEnabled(false);
				break;
			case "Anchor view":
				setRegionItem.setEnabled(true);
				enlargeRegionItem.setEnabled(true);
				setMoleculeItem.setEnabled(false);	
				setAnchorItem.setEnabled(true);
				break;
			case "Molecule view": 
				setRegionItem.setEnabled(false);
				enlargeRegionItem.setEnabled(false);
				setMoleculeItem.setEnabled(false);	
				setAnchorItem.setEnabled(false);
				break;
			case "Multiple alignment view":
				setRegionItem.setEnabled(false);
				enlargeRegionItem.setEnabled(false);
				setMoleculeItem.setEnabled(false);	
				setAnchorItem.setEnabled(false);
				break;
			default:
				;
		}
	}
	
	
	private JMenuItem setRegionItem;
	private JMenuItem enlargeRegionItem;
	private JMenuItem setMoleculeItem;
	private JMenuItem setAnchorItem;
	
	private void initMenuBar() 
	{
		final JFrame workingFrame = this;
		JMenuBar menuBar = new JMenuBar();
		
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		JMenu loadSubMenu = new JMenu("Load");
		loadSubMenu.setMnemonic('L');
		JMenuItem loadRefItem = new JMenuItem("Reference");
		loadRefItem.setMnemonic('R');
		loadRefItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				loadRefChooser.setCurrentDirectory(workingDirectory);
				int selection = loadRefChooser.showOpenDialog(workingFrame);
				
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					workingDirectory = loadRefChooser.getCurrentDirectory();
					loadReference(loadRefChooser.getSelectedFiles());
				}
				
			}
		});
		loadSubMenu.add(loadRefItem);

		JMenuItem loadMoleculeItem = new JMenuItem("Molecule");
		loadMoleculeItem.setMnemonic('M');
		loadMoleculeItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				loadMoleculeChooser.setCurrentDirectory(workingDirectory);
				int selection = loadMoleculeChooser.showOpenDialog(workingFrame);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					workingDirectory = loadMoleculeChooser.getCurrentDirectory();
					loadMolecule(loadMoleculeChooser.getSelectedFiles());					
				}
			}
		});		
		loadSubMenu.add(loadMoleculeItem);

		JMenuItem loadResultItem = new JMenuItem("Alignment Result");
		loadResultItem.setMnemonic('A');
		loadResultItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				loadResultChooser.setCurrentDirectory(workingDirectory);
				int selection = loadResultChooser.showOpenDialog(workingFrame);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					workingDirectory = loadResultChooser.getCurrentDirectory();
					loadResult(loadResultChooser.getSelectedFiles());
				}
			}
		});		
		loadSubMenu.add(loadResultItem);
		JMenuItem loadMultipleAlignmentItem = new JMenuItem("Multiple alignment");
		loadMultipleAlignmentItem.setMnemonic('U');
		loadMultipleAlignmentItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				loadMultipleAlignmentChooser.setCurrentDirectory(workingDirectory);
				int selection = loadMultipleAlignmentChooser.showOpenDialog(workingFrame);
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					workingDirectory = loadMultipleAlignmentChooser.getCurrentDirectory();
					loadMultipleAlignment(loadMultipleAlignmentChooser.getSelectedFiles());
				}
			}
		});		
		loadSubMenu.add(loadMultipleAlignmentItem);
		JMenuItem loadAnnotateItem = new JMenuItem("Annotation");
		loadAnnotateItem.setMnemonic('O');
		loadAnnotateItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				loadAnnotationChooser.setCurrentDirectory(workingDirectory);
				int selection = loadAnnotationChooser.showOpenDialog(workingFrame);
				
				if (selection == JFileChooser.APPROVE_OPTION)
				{
					workingDirectory = loadAnnotationChooser.getCurrentDirectory();
					loadAnnotation(loadAnnotationChooser.getSelectedFiles());
				}
			}
		});
		loadSubMenu.add(loadAnnotateItem);
		
		fileMenu.add(loadSubMenu);	
		
//		JMenuItem dataPanelItem = new JMenuItem("Data Panel");
//		dataPanelItem.setMnemonic('P');
//		dataPanelItem.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				JOptionPane.showMessageDialog(workingFrame, "Under construction.");
//			}
//		});
//		fileMenu.add(dataPanelItem);
		
		JMenuItem saveImageItem = new JMenuItem("Save Images");
		saveImageItem.setMnemonic('S');
		saveImageItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				controlPanel.saveImage();
			}
		});
		fileMenu.add(saveImageItem);
		
		JMenu clearMenu = new JMenu("Clear");
		clearMenu.setMnemonic('C');
		JMenuItem clearResultItem = new JMenuItem("Clear Results");
		clearResultItem.setMnemonic('R');
		clearResultItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				dataModule.clearAllResult();
			}
		});
		clearMenu.add(clearResultItem);
		
		JMenuItem clearMoleculeItem = new JMenuItem("Clear Molecules");
		clearMoleculeItem.setMnemonic('M');
		clearMoleculeItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				dataModule.clearAllMolecule();
			}
		});
		clearMenu.add(clearMoleculeItem);
		
		fileMenu.add(clearMenu);
		
		JMenuItem exitItem = new JMenuItem("Quit");
		exitItem.setMnemonic('Q');
		exitItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		
		menuBar.add(fileMenu);
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		
		JMenuItem newViewItem = new JMenuItem("New View Panel");
		newViewItem.setMnemonic('V');
		newViewItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null) {
					JOptionPane.showMessageDialog(workingFrame, "No panel is opened.");
					return;
				}	
				controlPanel.createViewPanel();
			}
		});
		editMenu.add(newViewItem);
		
		JMenuItem newAnnotationItem = new JMenuItem("New Annotation Panel");
		newAnnotationItem.setMnemonic('A');
		newAnnotationItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No panel is opened.");
					return;
				}				
				controlPanel.createAnnotationPanel();
			}
		});
		editMenu.add(newAnnotationItem);
		editMenu.addSeparator();
		
		JMenuItem setDNARatioItem = new JMenuItem("Set DNA ratio");
		setDNARatioItem.setMnemonic('D');
		setDNARatioItem.addActionListener(new ActionListener()
  		{
  			@Override
  			public void actionPerformed(ActionEvent event) 
  			{
				String ans = JOptionPane.showInputDialog(workingFrame, "Please input the DNA ratio:",  controlPanel.dnaRatio);
				if (ans != null)
					try {
						double dnaRatio = Double.parseDouble(ans);
						controlPanel.setDNARatio(dnaRatio);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
  			}
  		});
		editMenu.add(setDNARatioItem);

		
		JMenuItem setRegionItem = new JMenuItem("Set View Region");
		setRegionItem.setMnemonic('R');
		setRegionItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No panel is opened.");
					return;
				}				
				String ans = JOptionPane.showInputDialog(workingFrame, "Please input the region to be shown in chrX:NNNNN-NNNNN format", controlPanel.getRegion()==null?"":controlPanel.getRegion().toString());
				if (ans != null)
					try {
						GenomicPosNode region = new GenomicPosNode(ans.trim());	
						controlPanel.setRegion(region);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
			}
		});
		editMenu.add(setRegionItem);
		
		JMenuItem enlargeRegionItem = new JMenuItem("Enlarge Region");
		enlargeRegionItem.setMnemonic('E');
		enlargeRegionItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No panel is opened.");
					return;
				}
				if (controlPanel.getRegion() == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No region is set yet.");
					return;
				}
				GenomicPosNode region = controlPanel.getRegion();
				String leftans = JOptionPane.showInputDialog(workingFrame, "Please input the left size", "200000");
				String rightans = JOptionPane.showInputDialog(workingFrame, "Please input the right size", "200000");
				long left = region.start;
				long right = region.stop;
				if (leftans != null)
					try {
						int m = Integer.parseInt(leftans);
						left -= m;
								
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				if (leftans != null)
					try {
						int m = Integer.parseInt(rightans);
						right += m;
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				if (left != region.start || right != region.stop)
					controlPanel.setRegion(new GenomicPosNode(region.ref, left, right));

			}
		});
		editMenu.add(enlargeRegionItem);
		
		JMenuItem setMoleculeItem = new JMenuItem("Set View Molecule");
		setMoleculeItem.setMnemonic('M');
		setMoleculeItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No view panel is opened.");
					return;
				}
				String ans = JOptionPane.showInputDialog(workingFrame, "Please input the molecule ID");
				if (ans != null)
					try {
						String id = ans.trim();	
						controlPanel.setViewMolecule(id);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}				
				
			}
		});
		editMenu.add(setMoleculeItem);
		
		JMenuItem setAnchorItem = new JMenuItem("Set Anchor Site");
		setAnchorItem.setMnemonic('N');
		setAnchorItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event) {
				if (controlPanel == null)
				{
					JOptionPane.showMessageDialog(workingFrame, "No panel is opened.");
					return;
				}				
				String ans = JOptionPane.showInputDialog(workingFrame, "Please input the region to be shown in chrX:NNNNN-NNNNN fromat", controlPanel.getAnchorPoint()==null?"":controlPanel.getAnchorPoint().toString());
				if (ans != null)
					try {
						GenomicPosNode anchorPoint = new GenomicPosNode(ans.trim());	
						controlPanel.setAnchorPoint(anchorPoint);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
			}
		});
		editMenu.add(setAnchorItem);
		
		menuBar.add(editMenu);
		
		
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic('V');
		JMenuItem newRegionViewItem = new JMenuItem("New Regional View");
		newRegionViewItem.setMnemonic('R');
		newRegionViewItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				OMView.this.createRegionalViewTab();
			}
		});
		viewMenu.add(newRegionViewItem);
		
		JMenuItem newAnchorViewItem = new JMenuItem("New Anchor View");
		newAnchorViewItem.setMnemonic('N');
		newAnchorViewItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				OMView.this.createAnchorViewTab();
			}
		});
		viewMenu.add(newAnchorViewItem);
		
		JMenuItem newAlignmentViewItem = new JMenuItem("New Alignment View");
		newAlignmentViewItem.setMnemonic('A');
		newAlignmentViewItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				OMView.this.createAlignmentViewTab();
			}
		});
		viewMenu.add(newAlignmentViewItem);
		
		JMenuItem newMultipleAlignmentViewItem = new JMenuItem("New Multiple Alignment View");
		newMultipleAlignmentViewItem.setMnemonic('U');
		newMultipleAlignmentViewItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				OMView.this.createMultipleOpticalMapsViewTab();
			}
		});
		viewMenu.add(newMultipleAlignmentViewItem);

		JMenuItem newMoleculeViewItem = new JMenuItem("New Molecule View");
		newMoleculeViewItem.setMnemonic('M');
		newMoleculeViewItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				OMView.this.createMoleculeViewTab();
			}
		});
		viewMenu.add(newMoleculeViewItem);

		menuBar.add(viewMenu);

		

		JMenu optionMenu = new JMenu("Options");
		optionMenu.setMnemonic('O');
		
		JMenu resultProcessorSubMenu = new JMenu("Alignment Result Processor");
		resultProcessorSubMenu.setMnemonic('A');
		final JCheckBoxMenuItem enabledRBItem = new JCheckBoxMenuItem("Enabled");
		enabledRBItem.setMnemonic('E');
		enabledRBItem.setState(DataModule.useResultsBreaker);
		enabledRBItem.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				DataModule.useResultsBreaker = enabledRBItem.getState();
			}
			
		});
		resultProcessorSubMenu.add(enabledRBItem);
		
		JMenuItem rbParameterMenuItem = new JMenuItem("Parameters");
		rbParameterMenuItem.setMnemonic('P');		
		rbParameterMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				String meas_ans = JOptionPane.showInputDialog(workingFrame, "Please input measuring error", dataModule.meas);
				String ear_ans = JOptionPane.showInputDialog(workingFrame, "Error acceptable range", dataModule.ear);
				if(meas_ans == null || ear_ans == null)
					return;
				dataModule.setResultsBreakerParameters(Integer.parseInt(meas_ans.trim()), Double.parseDouble(ear_ans.trim()));
			}		
		});
		resultProcessorSubMenu.add(rbParameterMenuItem);
		optionMenu.add(resultProcessorSubMenu);
		
		JMenu rulerSubMenu = new JMenu("Ruler");
		rulerSubMenu.setMnemonic('R');
		final JCheckBoxMenuItem displayRulerItem = new JCheckBoxMenuItem("Display Ruler");
		displayRulerItem.setMnemonic('D');
		displayRulerItem.setState(VRuler.display);
		displayRulerItem.addActionListener(new ActionListener()		
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				VRuler.display = displayRulerItem.getState();
				controlPanel.repaint();
			}		
		});
		rulerSubMenu.add(displayRulerItem);

		JMenuItem rulerMarkItem = new JMenuItem("Mark");
		rulerMarkItem.setMnemonic('M');		
		rulerMarkItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				String ans = JOptionPane.showInputDialog(workingFrame, "Please input ruler small mark size", VRuler.smallMark);
				VRuler.smallMark = Long.parseLong(ans.trim());
				ans = JOptionPane.showInputDialog(workingFrame, "Please input ruler large mark size", VRuler.largeMark);
				VRuler.largeMark = Long.parseLong(ans.trim());
				controlPanel.repaint();
			}		
		});
		
		rulerSubMenu.add(rulerMarkItem);
		optionMenu.add(rulerSubMenu);
		
//		JMenuItem extendedOptionsItem = new JMenuItem("Extended Options");
//		extendedOptionsItem.setMnemonic('E');
//		extendedOptionsItem.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e) 
//			{
//				JOptionPane.showMessageDialog(workingFrame, new ExtendedOptionsPanel(), "", JOptionPane.PLAIN_MESSAGE);
//			}		
//		});
//		optionMenu.add(extendedOptionsItem);
		
		final JCheckBoxMenuItem unmapSubMenu = new JCheckBoxMenuItem("Show Unmap Portion");
		unmapSubMenu.setMnemonic('U');
		unmapSubMenu.setState(RegionalView.showUnmap);
		unmapSubMenu.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				RegionalView.showUnmap = unmapSubMenu.getState();
				controlPanel.repaint();
				controlPanel.revalidate();
				controlPanel.reorganize();
//				JOptionPane.showMessageDialog(workingFrame, "Under construction.");
			}
		});
		optionMenu.add(unmapSubMenu);
		menuBar.add(optionMenu);

		
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		
		JMenuItem aboutItem = new JMenuItem("About");
		aboutItem.setMnemonic('A');
		aboutItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(workingFrame, aboutMessage);
			}
		});
		helpMenu.add(aboutItem);
		
//		JMenuItem legendItem = new JMenuItem("Legend");
//		legendItem.setMnemonic('L');
//		legendItem.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//				JOptionPane.showMessageDialog(workingFrame, "Under construction");
//			}
//		});
//		helpMenu.add(legendItem);
		
		JMenuItem developerItem = new JMenuItem("Developer Options");
		developerItem.setMnemonic('D');
		developerItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e) {

				String setting = JOptionPane.showInputDialog(workingFrame, "Setting name:", "");
				
				String valueString = JOptionPane.showInputDialog(workingFrame, "Setting value:", "");
				try {
					int value = Integer.parseInt(valueString);
					ViewSetting.changeSetting(setting, value);
				}
				catch (NumberFormatException dummy) {
					System.err.println("Value has to be integer");
				}
			}
		});
		helpMenu.add(developerItem);
		
		
		menuBar.add(helpMenu);
		
		setJMenuBar(menuBar);
		
		this.setRegionItem = setRegionItem;
		this.setMoleculeItem = setMoleculeItem;
		this.setAnchorItem = setAnchorItem;
		this.enlargeRegionItem = enlargeRegionItem;
	}
	private void initTabPanel()
	{
		final JTabbedPane tabPanel = new JTabbedPane();
		tabPanel.addChangeListener(new ChangeListener() {
		    @Override
			public void stateChanged(ChangeEvent changeEvent) {
		    	if (tabPanel.getComponentCount() > 0)
		    		OMView.this.controlPanel = (ControlPanel) tabPanel.getSelectedComponent();
		    	JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
				int index = sourceTabbedPane.getSelectedIndex();
				if (index == -1)
					changeMenuEnabled("");
				else
					changeMenuEnabled(sourceTabbedPane.getTitleAt(index));
		    }
		});
		ChangeListener changeListener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent changeEvent) {
				

			}
 		};
 		tabPanel.addChangeListener(changeListener);
		this.tabPanel = tabPanel;
		this.add(tabPanel);
	}

	// Regional View
	public RegionalControlPanel createRegionalViewTab() {
		RegionalView rView = new RegionalView(this);
		return createRegionalViewTab(rView);
	}
	public RegionalControlPanel createRegionalViewTab(RegionalView... rViews) {
		RegionalControlPanel controlPanel = new RegionalControlPanel(this, rViews);
		tabPanel.addTab(controlPanel.getTitle(), controlPanel);
		tabPanel.setTabComponentAt(tabPanel.getTabCount() - 1, new VTab(tabPanel, controlPanel.getTitle()));
		tabPanel.setSelectedIndex(tabPanel.getTabCount() - 1);
		controlPanel.updateData();
		return controlPanel;
	}
	public RegionalControlPanel createRegionalViewTab(LinkedHashMap<VDataType, List<String>> dataSelection) {
		RegionalView rView = new RegionalView(this);
		rView.updateDataSelection(dataSelection);
		return createRegionalViewTab(rView);
	}
	public RegionalControlPanel createRegionalViewTab(List<LinkedHashMap<VDataType, List<String>>> dataSelections) {
		RegionalView[] rViews = new RegionalView[dataSelections.size()];
		int index = 0;
		for (LinkedHashMap<VDataType, List<String>> dataSelection : dataSelections) {
			RegionalView rView = new RegionalView(this);
			rView.updateDataSelection(dataSelection);
			rViews[index++] = rView;
		}
		return createRegionalViewTab(rViews);
	}
	
	// Anchor view
	private AnchorControlPanel createAnchorViewTab(AnchorView... aViews) {
		AnchorControlPanel controlPanel = new AnchorControlPanel(this, aViews);		
		tabPanel.addTab(controlPanel.getTitle(), controlPanel);
		tabPanel.setTabComponentAt(tabPanel.getTabCount() - 1, new VTab(tabPanel, controlPanel.getTitle()));
		tabPanel.setSelectedIndex(tabPanel.getTabCount() - 1);
		controlPanel.updateData();
		return controlPanel;
	}
	public AnchorControlPanel createAnchorViewTab() {
		AnchorView anchorView = new AnchorView(this);
		return createAnchorViewTab(anchorView); 
	}
	public AnchorControlPanel createAnchorViewTab(LinkedHashMap<VDataType, List<String>> dataSelection)
	{
		AnchorView aView = new AnchorView(this);
		aView.updateDataSelection(dataSelection);
		return createAnchorViewTab(aView);
	}
	public AnchorControlPanel createAnchorViewTab(List<LinkedHashMap<VDataType, List<String>>> dataSelections) {
		AnchorView[] aViews = new AnchorView[dataSelections.size()];
		int index = 0;
		for (LinkedHashMap<VDataType, List<String>> dataSelection : dataSelections) {
			AnchorView aView = new AnchorView(this);
			aView.updateDataSelection(dataSelection);
			aViews[index++] = aView;
		}
		return createAnchorViewTab(aViews);
	}

	// Alignment View
	private AlignmentControlPanel createAlignmentViewTab(AlignmentView aView) {
		aView.updateData();
		AlignmentControlPanel controlPanel = new AlignmentControlPanel(this, aView);
		tabPanel.addTab(controlPanel.getTitle(), controlPanel);
		tabPanel.setTabComponentAt(tabPanel.getTabCount() - 1, new VTab(tabPanel, controlPanel.getTitle()));
		tabPanel.setSelectedIndex(tabPanel.getTabCount() - 1);		
		return controlPanel;
	}
	public AlignmentControlPanel createAlignmentViewTab() {
		AlignmentView alignView = new AlignmentView(this);
		alignView.updateData();
		return createAlignmentViewTab(alignView);
	}
	public AlignmentControlPanel createAlignmentViewTab(LinkedHashMap<VDataType, List<String>> dataSelection) {
		AlignmentView alignView = new AlignmentView(this);
		AlignmentControlPanel controlPanel = createAlignmentViewTab(alignView);
		alignView.updateDataSelection(dataSelection);
		return controlPanel;
	}

	
	// Multiple alignment view
	private MultipleOpticalMapsControlPanel createMultipleOpticalMapsViewTab(MultipleOpticalMapsView mView) {
		MultipleOpticalMapsControlPanel controlPanel = new MultipleOpticalMapsControlPanel(this, mView);
		tabPanel.addTab(controlPanel.getTitle(), controlPanel);
		tabPanel.setTabComponentAt(tabPanel.getTabCount() - 1, new VTab(tabPanel, controlPanel.getTitle()));
		tabPanel.setSelectedIndex(tabPanel.getTabCount() - 1);
		return controlPanel;
	}
	public MultipleOpticalMapsControlPanel createMultipleOpticalMapsViewTab() {
		MultipleOpticalMapsView mView = new MultipleOpticalMapsView(this);
		return createMultipleOpticalMapsViewTab(mView);
	}
	public MultipleOpticalMapsControlPanel createMultipleOpticalMapsViewTab(LinkedHashMap<VDataType, List<String>> dataSelection) {
		MultipleOpticalMapsView mView = new MultipleOpticalMapsView(this);
		mView.updateDataSelection(dataSelection);
		return createMultipleOpticalMapsViewTab(mView);
	}

	// Molecule view
	private MoleculeControlPanel createMoleculeViewTab(MoleculeView mView) {
		mView.updateData();
		MoleculeControlPanel controlPanel = new MoleculeControlPanel(this, mView);
		tabPanel.addTab(controlPanel.getTitle(), controlPanel);
		tabPanel.setTabComponentAt(tabPanel.getTabCount() - 1, new VTab(tabPanel, controlPanel.getTitle()));
		tabPanel.setSelectedIndex(tabPanel.getTabCount() - 1);
		return controlPanel;
	}
	public MoleculeControlPanel createMoleculeViewTab() {
		MoleculeView mView = new MoleculeView(this);
		return createMoleculeViewTab(mView);
	}
	public MoleculeControlPanel createMoleculeViewTab(LinkedHashMap<VDataType, List<String>> dataSelection) {
		MoleculeView mView = new MoleculeView(this);
		mView.updateDataSelection(dataSelection);
		return createMoleculeViewTab(mView);
	}

	
	public void closeTab(ControlPanel controlPanel) {
		tabPanel.remove(tabPanel.indexOfComponent(controlPanel));
	}
	
	public void saveImage(String path, JComponent... comp)
	{		
		ViewSaver viewSaver = new ViewSaver(comp, path, ImageSaveFormat.lookup(path, -1));
		viewSaver.addPropertyChangeListener(statusPanel);
		synchronized (viewSaver) {
			viewSaver.execute();
			try {
				viewSaver.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public void saveImage(JComponent... comp)
	{
		if (comp == null)
		{
			JOptionPane.showMessageDialog(this, "No view panel is selected.");
			return;
		}
		imageSaveFileChooser.setCurrentDirectory(workingDirectory);
		int selection = imageSaveFileChooser.showSaveDialog(this);
		if (selection == JFileChooser.APPROVE_OPTION)
		{
			workingDirectory = imageSaveFileChooser.getCurrentDirectory();
			String path = imageSaveFileChooser.getSelectedFile().getAbsolutePath();
			String extension = ((FileNameExtensionFilter) imageSaveFileChooser.getFileFilter()).getExtensions()[0];
			ViewSaver viewSaver = new ViewSaver(comp, path, ImageSaveFormat.lookupfileext(extension));
			viewSaver.addPropertyChangeListener(statusPanel);
			viewSaver.execute();
		}
	}

	public synchronized void loadAnnotation(File[] filearray) {
		if (filearray.length == 0)
			return;
		synchronized(toLoadList) {
			for (File file : filearray)
				toLoadList.add(new IOLoadDuty(file, LoadDutyFileType.ANNOTATION));
			toLoadList.notify();
		}
	}
	public synchronized void loadReference(File[] filearray)
	{
		if (filearray.length == 0)
			return;
		synchronized(toLoadList) {
			for (File file : filearray)
				toLoadList.add(new IOLoadDuty(file, LoadDutyFileType.REFERENCE));
			toLoadList.notify();
		}
	}
	public synchronized void loadMolecule(File[] filearray)
	{
		if (filearray.length == 0)
			return;
		synchronized(toLoadList) {
			for (File file : filearray)
				toLoadList.add(new IOLoadDuty(file, LoadDutyFileType.MOLECULE));
			toLoadList.notify();
		}
	}
	public synchronized void loadResult(File[] filearray)
	{
		if (filearray.length == 0)
			return;
		synchronized(toLoadList) {
			for (File file : filearray)
				toLoadList.add(new IOLoadDuty(file, LoadDutyFileType.RESULT));
			toLoadList.notify();
		}
	}
	public synchronized void loadMultipleAlignment(File[] filearray) {
		if (filearray.length == 0)
			return;
		synchronized(toLoadList) {
			for (File file : filearray)
				toLoadList.add(new IOLoadDuty(file, LoadDutyFileType.MULTIPLEALIGNMENT));
			toLoadList.notify();
		}
	}
	public synchronized boolean isAllTasksDone() // Only main class uses
	{
		synchronized (toLoadList) {
			return toLoadList.isEmpty() && taskList.size() == 0  && dataModule.isAllTasksDone();
		}
	}
	
	List<IOLoadDuty> toLoadList = Collections.synchronizedList(new ArrayList<IOLoadDuty>());
	enum LoadDutyFileType{
		ANNOTATION (5),
		MOLECULE (2),
		REFERENCE (1),
		RESULT (3),
		MULTIPLEALIGNMENT (4);
		private final int priority;
		private LoadDutyFileType(int priority) {
			this.priority = priority;
		}
		public int getPriority() {
			return priority;
		}
	}

	class IOLoadDuty implements Comparable<IOLoadDuty> {
		public final File file;
		public final LoadDutyFileType type;
		public IOLoadDuty(File file, LoadDutyFileType type) {
			super();
			this.file = file;
			this.type = type;
		}
		@Override
		public int compareTo(IOLoadDuty duty) {
			return Integer.compare(this.type.getPriority(), duty.type.getPriority());
		}
		
	}
	class IOLoader extends SwingWorker<Void, Void> {

		@Override
		protected Void doInBackground() throws Exception {
			while (true) {
				IOLoadDuty duty;
				synchronized (toLoadList) {
					if (toLoadList.isEmpty()) {
						toLoadList.wait();
					}
					if (toLoadList.isEmpty()) // Someone has loaded an empty file array...
						continue;
					Collections.sort(toLoadList);
					duty = toLoadList.get(0);
				}
				// release to let loading add more IODuty
				File file = duty.file;
				String path = file.getAbsolutePath();
				switch (duty.type) {
					case ANNOTATION:
						AnnotationLoader annoLoader = new AnnotationLoader(path, AnnotationFormat.lookupfileext(FilenameUtils.getExtension(path)));
						annoLoader.addPropertyChangeListener(statusPanel);
						taskList.add(annoLoader);
						synchronized (annoLoader) {
							annoLoader.execute();
							annoLoader.wait();
						}
						taskList.remove(annoLoader);
						break;
					case MOLECULE:
						MoleculeLoader moleculeLoader = new MoleculeLoader(path, DataFormat.lookupfileext(FilenameUtils.getExtension(path)));
						moleculeLoader.addPropertyChangeListener(statusPanel);
						taskList.add(moleculeLoader);
						synchronized (moleculeLoader) {
							moleculeLoader.execute();
							moleculeLoader.wait();
						}
						taskList.remove(moleculeLoader);
						break;
					case REFERENCE:
						ReferenceLoader refLoader = new ReferenceLoader(path, DataFormat.lookupfileext(FilenameUtils.getExtension(path)));
						refLoader.addPropertyChangeListener(statusPanel);
						taskList.add(refLoader);
						synchronized (refLoader) {
							refLoader.execute();
							refLoader.wait();
						}
						taskList.remove(refLoader);
						break;
					case RESULT:
						ResultLoader resultLoader = new ResultLoader(path, ResultFormat.lookupfileext(FilenameUtils.getExtension(path)));
						resultLoader.setAdditionalInfo(dataModule.getAllReference(), dataModule.getAllData());
						resultLoader.addPropertyChangeListener(statusPanel);
						taskList.add(resultLoader);
						synchronized (resultLoader) {
							resultLoader.execute();
							resultLoader.wait();
						}
						taskList.remove(resultLoader);
						break;
					case MULTIPLEALIGNMENT:
						MultipleAlignmentFormat format = MultipleAlignmentFormat.lookupfileext(FilenameUtils.getExtension(path));
						switch (format) {
							case CBL:
								MultipleAlignmentLoader multipleAlignmentLoader = new MultipleAlignmentLoader(path, format);
								multipleAlignmentLoader.addPropertyChangeListener(statusPanel);
								taskList.add(multipleAlignmentLoader);
								synchronized (multipleAlignmentLoader) {
									multipleAlignmentLoader.execute();
									multipleAlignmentLoader.wait();
								}
								taskList.remove(multipleAlignmentLoader);
								break;
							case CBO:
								MultipleAlignmentOrderLoader multipleAlignmentOrderLoader = new MultipleAlignmentOrderLoader(path, format);
								multipleAlignmentOrderLoader.addPropertyChangeListener(statusPanel);
								taskList.add(multipleAlignmentOrderLoader);
								synchronized (multipleAlignmentOrderLoader) {
									multipleAlignmentOrderLoader.execute();
									multipleAlignmentOrderLoader.wait();
								}
								taskList.remove(multipleAlignmentOrderLoader);
								break;
							case CBC:
								MultipleAlignmentColorLoader multipleAlignmentColorLoader = new MultipleAlignmentColorLoader(path, format);
								multipleAlignmentColorLoader.addPropertyChangeListener(statusPanel);
								taskList.add(multipleAlignmentColorLoader);
								synchronized (multipleAlignmentColorLoader) {
									multipleAlignmentColorLoader.execute();
									multipleAlignmentColorLoader.wait();
								}
								taskList.remove(multipleAlignmentColorLoader);
								break;
						}
						break;

					default:
						break;

				}
					
				// We have to synchronize before removal
				synchronized (toLoadList) {
					duty = toLoadList.remove(0);
				}
				
			}
		}


	}
	
	private JFileChooser loadAnnotationChooser = new JFileChooser();
	{
		loadAnnotationChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		loadAnnotationChooser.setAcceptAllFileFilterUsed(false);
		loadAnnotationChooser.setMultiSelectionEnabled(true);
		loadAnnotationChooser.setDialogTitle("Open Annotation");
		for (FileNameExtensionFilter filter : AnnotationFormat.getFileNameExtensionFilter())
			loadAnnotationChooser.addChoosableFileFilter(filter);
	}
	private JFileChooser loadRefChooser = new JFileChooser();
	{
		loadRefChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		loadRefChooser.setAcceptAllFileFilterUsed(false);
		loadRefChooser.setMultiSelectionEnabled(true);
		loadRefChooser.setDialogTitle("Open Reference");
		for (FileNameExtensionFilter filter : DataFormat.getFileNameExtensionFilter())
			loadRefChooser.addChoosableFileFilter(filter);
	}
	private JFileChooser loadMoleculeChooser = new JFileChooser();
	{
		loadMoleculeChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		loadMoleculeChooser.setAcceptAllFileFilterUsed(false);
		loadMoleculeChooser.setMultiSelectionEnabled(true);
		loadMoleculeChooser.setDialogTitle("Open Molecule");
		for (FileNameExtensionFilter filter : DataFormat.getFileNameExtensionFilter())
			loadMoleculeChooser.addChoosableFileFilter(filter);

	}
	private JFileChooser loadResultChooser = new JFileChooser();
	{
		loadResultChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		loadResultChooser.setAcceptAllFileFilterUsed(false);
		loadResultChooser.setMultiSelectionEnabled(true);
		loadResultChooser.setDialogTitle("Open Alignment Result");
		for (FileNameExtensionFilter filter : ResultFormat.getFileNameExtensionFilter(true))
			loadResultChooser.addChoosableFileFilter(filter);
		
	}
	private JFileChooser loadMultipleAlignmentChooser = new JFileChooser();
	{
		loadMultipleAlignmentChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		loadMultipleAlignmentChooser.setAcceptAllFileFilterUsed(false);
		loadMultipleAlignmentChooser.setMultiSelectionEnabled(true);
		loadMultipleAlignmentChooser.setDialogTitle("Open Multiple Alignment");
		for (FileNameExtensionFilter filter : MultipleAlignmentFormat.getFileNameExtensionFilter(true))
			loadMultipleAlignmentChooser.addChoosableFileFilter(filter);
		
	}

	private JFileChooser saveResultChooser = new JFileChooser();
	{
		saveResultChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		saveResultChooser.setAcceptAllFileFilterUsed(false);
		saveResultChooser.setMultiSelectionEnabled(false);
		saveResultChooser.setDialogTitle("Save Alignment Result");
		for (FileNameExtensionFilter filter : ResultFormat.getFileNameExtensionFilter(false))
			saveResultChooser.addChoosableFileFilter(filter);
	}
	private JFileChooser imageSaveFileChooser = new JFileChooser();
	{
		imageSaveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		imageSaveFileChooser.setAcceptAllFileFilterUsed(false);
		imageSaveFileChooser.setMultiSelectionEnabled(false);
		imageSaveFileChooser.setDialogTitle("Save Image");
		for (FileNameExtensionFilter filter : ImageSaveFormat.getFileNameExtensionFilter())
			imageSaveFileChooser.addChoosableFileFilter(filter);
	}
	
	class AnnotationLoader extends SwingWorker<List<? extends AnnotationNode>, AnnotationNode>{

		private String filename;
		private AnnotationFormat aformat;
		public AnnotationLoader(String filename, int format) 
		{
			this(filename, AnnotationFormat.lookup(format));
		}
		public AnnotationLoader(String filename, AnnotationFormat aformat) 
		{
			this.filename = filename; 
			this.aformat = aformat;
		}
		@Override
		protected List<? extends AnnotationNode> doInBackground() throws Exception{
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			switch (aformat)
			{
				case BED:
					List<BEDNode> bedlist = new ArrayList<BEDNode>();
					BEDReader bedreader = new BEDReader(stream);
					BEDNode bed;
					while ((bed = bedreader.read()) != null)
					{
						publish(bed);
						bedlist.add(bed);
					}
					bedreader.close();
					return bedlist;
				case GVF:
					List<GVFNode> gvflist = new ArrayList<GVFNode>();
					GVFReader gvfreader = new GVFReader(stream);
					GVFNode gvf;
					while ((gvf = gvfreader.read()) != null)
					{
						publish(gvf);
						gvflist.add(gvf);
					}
					gvfreader.close();
					return gvflist;
				case GFF:
				case GTF:
					List<GFFNode> gfflist = new ArrayList<GFFNode>();
					GFFReader gffreader = new GFFReader(stream);
					GFFNode gff;
					while ((gff = gffreader.read()) != null)
					{
						publish(gff);
						gfflist.add(gff);
					}
					gffreader.close();
					return gfflist;
				case OSV:
					StandardSVReader svr = new StandardSVReader(stream);
					StandardSVNode sv;
					List<StandardSVNode> svlist = new ArrayList<StandardSVNode>();
					while ((sv = svr.read()) != null)
					{
						publish(sv);
						svlist.add(sv);
					}
					svr.close();
					return svlist;
					
				default:
					// Unknown format
					stream.close();
					return null;
			}
			
		}

		@Override
		public void done()
		{
			if (!isCancelled())
				try {
//					viewPanel.loadReference(get());
//					controlPanel.loadAnnotation(get());
					dataModule.addAnnotation(filename, get());
//					OMView.this.taskList.remove(this);
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}
				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}

		}
	}
	class MoleculeLoader extends SwingWorker <LinkedHashMap<String, DataNode>, Void>{

		private String filename;
		private DataFormat dformat;
		
		public MoleculeLoader(String filename, int format)
		{
			this(filename, DataFormat.lookup(format));
		}
		public MoleculeLoader(String filename, DataFormat dformat) 
		{
			this.filename = filename;
			this.dformat = dformat;
		}
		@Override
		protected LinkedHashMap<String, DataNode> doInBackground() throws Exception{
			LinkedHashMap<String, DataNode> fragmentmap = new LinkedHashMap<String, DataNode>();
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			OptMapDataReader omdr = new OptMapDataReader(stream, dformat);
			DataNode fragment;
			while ((fragment = omdr.read()) != null)
			{
				fragmentmap.put(fragment.name, fragment);
			}
			
			omdr.close();
			return fragmentmap;
		}
		
		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addData(filename, get());
					
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}
	class ReferenceLoader extends SwingWorker<LinkedHashMap<String, DataNode>, DataNode>{

		private String filename;
		private DataFormat dformat;
		public ReferenceLoader(String filename, int format) 
		{
			this(filename, DataFormat.lookup(format));
		}
		public ReferenceLoader(String filename, DataFormat dformat) 
		{
			this.filename = filename; 
			this.dformat = dformat;
		}
		@Override
		protected LinkedHashMap<String, DataNode> doInBackground() throws Exception{			
			ReferenceReader rr;
			DataNode ref;
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			LinkedHashMap<String, DataNode> optrefmap = new LinkedHashMap<String, DataNode>();
			rr = new ReferenceReader(stream, dformat);
			while ((ref = rr.read()) != null)
			{
//					publish(ref);
				optrefmap.put(ref.name, ref);
			}
			rr.close();
			return optrefmap;
		}

		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addReference(filename, get());
					
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}
				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}
	class ResultLoader extends SwingWorker<LinkedHashMap<String, List<OptMapResultNode>>, Void>{

		private String filename;
		private ResultFormat rformat;
		private GenomicPosNode region;
		
		private LinkedHashMap<String, DataNode> optrefmap = null;
		private LinkedHashMap<String, DataNode> fragmentmap = null;
		public ResultLoader(String filename, int format)
		{
			this(filename, ResultFormat.lookup(format));
		}
		public ResultLoader(String filename, ResultFormat rformat) 
		{
			this.filename = filename;
			this.rformat = rformat;
		}
		public void setAdditionalInfo(LinkedHashMap<String, DataNode> optrefmap, LinkedHashMap<String, DataNode> fragmentmap)
		{
			this.optrefmap = optrefmap;
			this.fragmentmap = fragmentmap;
		}
		public void setRegion(GenomicPosNode region)
		{
			this.region = region;
		}
		@Override
		protected LinkedHashMap<String, List<OptMapResultNode>> doInBackground() throws Exception{
			LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = new LinkedHashMap<String, List<OptMapResultNode>>();
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			OptMapResultReader omrr = new OptMapResultReader(stream, rformat);
			if (optrefmap != null)
				omrr.importRefInfo(optrefmap);
			if (fragmentmap != null)
				omrr.importFragInfo(fragmentmap);
			List<OptMapResultNode> resultlist = new ArrayList<OptMapResultNode>();
			
			while ((resultlist = omrr.readNextList()) != null)
			{
				
				boolean pass = true;
				if (resultlist.get(0).parentFrag.refp == null)
				{
					System.err.println("Molecule information of is missing.");
					System.err.printf("Alignment of \"%s\" is not loaded.\n", resultlist.get(0).parentFrag.name);
					pass = false;
				}
				if (!pass)
					continue;
				for (OptMapResultNode result : resultlist)
				{
					if (!result.isUsed())
						break;
					if (!result.isSubFragInfoValid())
					{
						System.err.println("Invalid SubMoleStart/SubMoleStop or Invalid molecule information.");
						System.err.printf("Alignment of \"%s\" is not loaded.\n", resultlist.get(0).parentFrag.name);
						pass = false;
						break;
					}
					if (optrefmap != null)						
						if (!optrefmap.containsKey(result.mappedRegion.ref))
						{
							System.err.println("Reference information of \"" + result.mappedRegion.ref + "\" is missing.");
							System.err.printf("Alignment of \"%s\" is not loaded.\n", resultlist.get(0).parentFrag.name);
							pass = false;
							break;
						}
						else
							if (!result.isSubRefInfoValid(optrefmap))
							{
								System.err.println("Invalid SubRefStart/SubRefStop or Invalid reference information.");
								System.err.printf("Alignment of \"%s\" is not loaded.\n", resultlist.get(0).parentFrag.name);
								pass = false;
								break;
							}
				}
				if (!pass)
					continue;
				try {
					Collections.sort(resultlist, Collections.reverseOrder(OptMapResultNode.mappedscorecomparator));					
					if (region == null || resultlist.get(0).isClose(region, 0))
					{
						if (resultlistmap.containsKey(resultlist.get(0).parentFrag.name))
							resultlistmap.get(resultlist.get(0).parentFrag.name).addAll(resultlist);
						else
							resultlistmap.put(resultlist.get(0).parentFrag.name, resultlist);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			omrr.close();
			return resultlistmap;
		}
		
		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addResult(filename, get());
					
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files. \nNote: for XMAP file, you have to import molecules first.");
					e.printStackTrace();
				}
				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}
	class MultipleAlignmentLoader extends SwingWorker<LinkedHashMap<String, CollinearBlock>, Void>{

		private String filename;
		private MultipleAlignmentFormat format;
		public MultipleAlignmentLoader(String filename, int format) 
		{
			this(filename, MultipleAlignmentFormat.lookup(format));
		}
		public MultipleAlignmentLoader(String filename, MultipleAlignmentFormat format) {
			this.filename = filename; 
			this.format = format;
		}
		@Override
		protected LinkedHashMap<String, CollinearBlock> doInBackground() throws Exception{			
			CollinearBlockReader reader;
			CollinearBlock block;
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			LinkedHashMap<String, CollinearBlock> blocks = new LinkedHashMap<String, CollinearBlock>();
			reader = new CollinearBlockReader(stream);
			while ((block = reader.read()) != null) {
				blocks.put(block.name, block);
			}
			reader.close();
			return blocks;
		}

		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addMultipleAlignment(filename, get());
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}
	class MultipleAlignmentOrderLoader extends SwingWorker<List<String>, Void>{

		private String filename;
		private MultipleAlignmentFormat format;
		public MultipleAlignmentOrderLoader(String filename, int format) 
		{
			this(filename, MultipleAlignmentFormat.lookup(format));
		}
		public MultipleAlignmentOrderLoader(String filename, MultipleAlignmentFormat format) {
			this.filename = filename; 
			this.format = format;
		}
		@Override
		protected List<String> doInBackground() throws Exception{			
			BufferedReader reader;
			String query;
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			List<String> order = new ArrayList<>();
			reader = new BufferedReader(new InputStreamReader(stream));
			while ((query = reader.readLine()) != null) {
				order.add(query);
			}
			reader.close();
			return order;
		}

		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addMultipleAlignmentOrder(filename, get());
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}
	class MultipleAlignmentColorLoader extends SwingWorker<LinkedHashMap<String, Color>, Void>{

		private String filename;
		private MultipleAlignmentFormat format;
		public MultipleAlignmentColorLoader(String filename, int format) 
		{
			this(filename, MultipleAlignmentFormat.lookup(format));
		}
		public MultipleAlignmentColorLoader(String filename, MultipleAlignmentFormat format) {
			this.filename = filename; 
			this.format = format;
		}
		@Override
		protected LinkedHashMap<String, Color> doInBackground() throws Exception{			
			
			
			BufferedReader reader;
			String s;
			ProgressMonitorInputStream stream = new ProgressMonitorInputStream(OMView.this, "Reading " + filename + "...", new FileInputStream(filename));
			LinkedHashMap<String, Color> colors = new LinkedHashMap<>();
			reader = new BufferedReader(new InputStreamReader(stream));
			while ((s = reader.readLine()) != null) {
				String[] l = s.split("\\s+");
				String group = l[0];
				Color color = Color.decode(l[1]);
				colors.put(group, color);
			}
			reader.close();
			return colors;
		}

		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					dataModule.addMultipleAlignmentColor(filename, get());
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(OMView.this, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(OMView.this, "Error occurs in parsing files.");
					e.printStackTrace();
				}				
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");
			synchronized (this) {
				notify();
			}
		}
	}

	class ResultSaver extends SwingWorker<Void, Void>{

		private String filename;
		private ResultFormat rformat;
		private GenomicPosNode region;
		private LinkedHashMap<String, List<OptMapResultNode>> resultlistmap;
		private boolean saveAll = true;
		public ResultSaver(String filename, int format, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap)
		{
			this(filename, ResultFormat.lookup(format), resultlistmap);
		}
		public ResultSaver(String filename, ResultFormat rformat, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) 
		{
			if (!filename.toLowerCase().endsWith('.' + rformat.getExtension()))
				filename += '.' + rformat.getExtension();
			this.filename = filename;
			this.rformat = rformat;
			this.resultlistmap = resultlistmap;
		}
		public void setSaveAll(boolean saveAll)
		{
			this.saveAll = saveAll;
		}
		public void setRegion(GenomicPosNode region)
		{
			this.region = region;
		}
		@Override
		protected Void doInBackground() throws Exception{
			OptMapResultWriter omrw = new OptMapResultWriter(filename, rformat);
			for (List<OptMapResultNode> resultlist : resultlistmap.values())
			{
				if (resultlist.get(0).isUsed())
				{
					boolean close = false;
					if (region != null)
						for (OptMapResultNode result : resultlist)
							close = close || result.isClose(region, 0);
					if (saveAll || close)
						omrw.write(resultlist);
				}
			}
			omrw.close();
			return null;
		}
		
		@Override
		public void done() {
			if (!isCancelled())
				;
			else
				JOptionPane.showMessageDialog(OMView.this, "Cancelled.");

		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		for (Component c : tabPanel.getComponents())
			if (c instanceof ControlPanel)
				((ControlPanel) c).updateDataSelection();
	}
	
	private TransferHandler handler = new TransferHandler() {
        @Override
		public boolean canImport(TransferHandler.TransferSupport support) {
            if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                return false;
            }
            support.setDropAction(COPY);
 
            return true;
        }
 
        @Override
		public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
             
            Transferable t = support.getTransferable();
 
            try {
                java.util.List<File> files = (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
                List<File> dataFiles = new ArrayList<File>();
                List<File> resultFiles = new ArrayList<File>();
                List<File> annoFiles = new ArrayList<File>();
                List<File> multiAlignFiles = new ArrayList<File>();
                for (File f : files) {
                	String extension = FilenameUtils.getExtension(f.getName());
                	if (ResultFormat.isValidFormat(extension))
                		resultFiles.add(f);
                	else
                		if (DataFormat.isValidFormat(extension))
                			dataFiles.add(f);
                		else
                			if (AnnotationFormat.isValidFormat(extension))
                				annoFiles.add(f);
                			else
                				if (MultipleAlignmentFormat.isValidFormat(extension))
                					multiAlignFiles.add(f);
                				else
                					System.err.println("File not loaded for unknown file extension: " + f.getName());
                }

                ReferenceDataSelectionPanel rdsp = new ReferenceDataSelectionPanel(dataFiles, new ArrayList<File>());
                JOptionPane.showMessageDialog(null, rdsp, "Choose to import as reference or molecule", JOptionPane.PLAIN_MESSAGE);
                
                List<File> refFiles = rdsp.getReferenceFiles();
                List<File> moleculeFiles = rdsp.getDataFiles();
                
                loadReference(refFiles.toArray(new File[refFiles.size()]));
                loadMolecule(moleculeFiles.toArray(new File[moleculeFiles.size()]));
                loadResult(resultFiles.toArray(new File[resultFiles.size()]));
                loadAnnotation(annoFiles.toArray(new File[annoFiles.size()]));
                loadMultipleAlignment(multiAlignFiles.toArray(new File[multiAlignFiles.size()]));
            } catch (UnsupportedFlavorException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
            return true;
        }
    };
	
	public static void main(String[] args) throws IOException
	{
		ExtendOptionParser parser = new ExtendOptionParser(OMView.class.getSimpleName(), "Visualizes optical mapping data. OMView provides a GUI to visualize optical mapping data for different purposes. ");	
		parser.addHeader("Data Loading", 1);
		OptionSpec<String> viewrefin = parser.accepts("viewrefin", "Load references").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewmapin = parser.accepts("viewmapin", "Load molecules").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewresin = parser.accepts("viewresin", "Load alignment results").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewcblin = parser.accepts("viewcblin", "Load collinear blocks").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewcboin = parser.accepts("viewcboin", "Load collinear blocks (order)").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewcbcin = parser.accepts("viewcbcin", "Load collinear blocks (color)").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewannoin = parser.accepts("viewannoin", "Load annotations").withRequiredArg().ofType(String.class);
		parser.addHeader("View Opening", 1);
		OptionSpec<String> viewregion = parser.accepts("viewregion", "Show a specific region on a regional view").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewanchor = parser.accepts("viewanchor", "Show a specific anchor on an anchor view").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewalignment = parser.accepts("viewalignment", "Show a specific alignment").withRequiredArg().ofType(String.class);
		OptionSpec<Boolean> viewma = parser.accepts("viewma", "Automatically open multiple alignment view").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<Boolean> viewmolecule = parser.accepts("viewmolecule", "Automatically open molecule view").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<String> viewsave = parser.accepts("viewsave", "Save views to specific location instead of starting OMView").withRequiredArg().ofType(String.class);
		OptionSpec<String> viewsaveformat = parser.accepts("viewsaveformat", "Formats of image to be saved. " + ImageSaveFormat.getFormatHelp()).withRequiredArg().ofType(String.class).defaultsTo("png");
		parser.addHeader("View Settings", 1);
		OptionSpec<Boolean> viewbreakresult = parser.accepts("viewbreakresult", "Enable Result Breaker").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		
		parser.addHeader("Help", 1);
		parser.accepts("help", "Display help menu").forHelp();
		OptionSet options = parser.parse(args);
		if (options.has("help")) {
			parser.printHelpOn(System.out);
			return;
		}
		
		ToolTipManager.sharedInstance().setInitialDelay(0);
		OMView omview = new OMView();
		
		// View settings
		DataModule.useResultsBreaker = viewbreakresult.value(options);
		
		// Load files
		if (options.has(viewrefin))
		{
			List<String> reflist = viewrefin.values(options);
			File[] filearray = new File[reflist.size()];
			for (int i = 0; i < reflist.size(); i++)
				filearray[i] = new File(reflist.get(i));
			omview.loadReference(filearray);
		}
		if (options.has(viewmapin))
		{
			List<String> maplist = viewmapin.values(options);
			File[] filearray = new File[maplist.size()];
			for (int i = 0; i < maplist.size(); i++)
				filearray[i] = new File(maplist.get(i));
			omview.loadMolecule(filearray);
		}
		if (options.has(viewresin))
		{
			List<String> reslist = viewresin.values(options);
			File[] filearray = new File[reslist.size()];
			for (int i = 0; i < reslist.size(); i++)
				filearray[i] = new File(reslist.get(i));
			omview.loadResult(filearray);
		}
		if (options.has(viewannoin)) {
			List<String> reslist = viewannoin.values(options);
			File[] filearray = new File[reslist.size()];
			for (int i = 0; i < reslist.size(); i++)
				filearray[i] = new File(reslist.get(i));
			omview.loadAnnotation(filearray);
		}

		if (options.has(viewcblin)) {
			List<String> list = viewcblin.values(options);
			File[] filearray = new File[list.size()];
			for (int i = 0; i < list.size(); i++)
				filearray[i] = new File(list.get(i));
			omview.loadMultipleAlignment(filearray);
		}
		if (options.has(viewcboin)) {
			List<String> list = viewcboin.values(options);
			File[] filearray = new File[list.size()];
			for (int i = 0; i < list.size(); i++)
				filearray[i] = new File(list.get(i));
			omview.loadMultipleAlignment(filearray);
		}
		if (options.has(viewcbcin)) {
			List<String> list = viewcbcin.values(options);
			File[] filearray = new File[list.size()];
			for (int i = 0; i < list.size(); i++)
				filearray[i] = new File(list.get(i));
			omview.loadMultipleAlignment(filearray);
		}
		while (!omview.isAllTasksDone()); // Wait for all results to be loaded
		
		
		// Open panels 
		
		boolean defaultOpen = true;
		// Open regional view
		if (options.has(viewregion))
		{
			List<LinkedHashMap<VDataType, List<String>>> dataSelections = omview.dataModule.getIndividualDataSelection(VDataType.ALIGNMENT);
			// Create single tab for each region, while showing multiple panels for different alignment files			
			List<String> regionlist = viewregion.values(options);
			for (String region : regionlist) {
				RegionalControlPanel controlPanel = omview.createRegionalViewTab(dataSelections);
				controlPanel.setRegion(new GenomicPosNode(region));
			}
			defaultOpen = false;
		}		
		// Open anchor view
		if (options.has(viewanchor))
		{
			List<LinkedHashMap<VDataType, List<String>>> dataSelections = omview.dataModule.getIndividualDataSelection(VDataType.ALIGNMENT);
			// Create single tab for each region, while showing multiple panels for different alignment files			
			List<String> anchorlist = viewanchor.values(options);
			for (String anchor : anchorlist) {
				AnchorControlPanel controlPanel = omview.createAnchorViewTab(dataSelections);
				controlPanel.setAnchorPoint(new GenomicPosNode(anchor));
			}
			defaultOpen = false;
		}		
		// Open alignment view
		if (options.has(viewalignment)) {
			LinkedHashMap<VDataType, List<String>> dataSelection = omview.dataModule.getAllDataSelection(VDataType.ALIGNMENT);
			// Create single tab for each alignment, look for the id from all alignment files
			List<String> moleculelist = viewalignment.values(options);
			for (String molecule : moleculelist) {
				AlignmentControlPanel controlPanel = omview.createAlignmentViewTab(dataSelection);
				controlPanel.setViewMolecule(molecule);
			}
			defaultOpen = false;
		}			
		
		// Open molecule view
		if (options.valueOf(viewmolecule)) {
			List<LinkedHashMap<VDataType, List<String>>> dataSelections = omview.dataModule.getIndividualDataSelection(VDataType.MOLECULE);
			// Create multiple tabs, each tab corresponds to one molecule file
			for (LinkedHashMap<VDataType, List<String>> dataSelection : dataSelections)
				omview.createMoleculeViewTab(dataSelection);
			defaultOpen = false;
		}	
		
		// Open multiple alignment view
		if (options.valueOf(viewma)) {
			LinkedHashMap<VDataType, List<String>> dataSelection = omview.dataModule.getAllDataSelection(VDataType.MULTIPLEALIGNMENTBLOCK, VDataType.MULTIPLEALIGNMENTORDER, VDataType.MULTIPLEALIGNMENTCOLOR);
			List<String> cblFiles = dataSelection.get(VDataType.MULTIPLEALIGNMENTBLOCK);
			List<String> cboFiles = dataSelection.get(VDataType.MULTIPLEALIGNMENTORDER);
			List<String> cbcFiles = dataSelection.get(VDataType.MULTIPLEALIGNMENTCOLOR);
			
			for (int i = 0; i < Math.min(cblFiles.size(), cboFiles.size()); i++) {
				LinkedHashMap<VDataType, List<String>> newDataSelection = new LinkedHashMap<>();
				newDataSelection.put(VDataType.MULTIPLEALIGNMENTBLOCK, cblFiles.subList(i, i + 1));
				newDataSelection.put(VDataType.MULTIPLEALIGNMENTORDER, cboFiles.subList(i, i + 1));
				if (i < cbcFiles.size())
					newDataSelection.put(VDataType.MULTIPLEALIGNMENTCOLOR, cbcFiles.subList(i, i + 1));
				else
					newDataSelection.put(VDataType.MULTIPLEALIGNMENTCOLOR, new ArrayList<String>());				
				omview.createMultipleOpticalMapsViewTab(newDataSelection);
			}
			defaultOpen = false;
		}
		
		
		// View save
		if (options.has(viewsave)) {
			String location = viewsave.value(options);
			LinkedHashMap<String, Integer> usedFileNames = new LinkedHashMap<>();
			
			List<String> extensions = viewsaveformat.values(options);
			for (String extension : extensions)
				for (Component c : omview.tabPanel.getComponents())
					if (c instanceof ControlPanel) {
						if (c instanceof MoleculeControlPanel)
							System.err.println("Warning: Only the first page in molecule view is saved. ");
						String filename = location + "/" + ((ControlPanel) c).getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
						if (!usedFileNames.containsKey(filename))
							usedFileNames.put(filename, 0);
						usedFileNames.put(filename, usedFileNames.get(filename) + 1);
						if (usedFileNames.get(filename) > 1)
							filename = filename + "-" + (usedFileNames.get(filename) - 1);
						((ControlPanel) c).saveImage(filename + "." + extension);
						
					}
			defaultOpen = false;
			
			return; // After saving, terminate the OMView program
		}
		if (defaultOpen)
			omview.createRegionalViewTab();
//		omview.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		omview.setVisible(true);
	}

}


class StatusPanel extends JPanel implements PropertyChangeListener {
	
	private JLabel statusLabel = new JLabel("Done.");
	private List<PropertyChangeEvent> taskList = new ArrayList<PropertyChangeEvent>();
	public StatusPanel() {
		setBorder(new BevelBorder(BevelBorder.LOWERED));
		setPreferredSize(new Dimension(1000, 16));
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(statusLabel);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		int changedTask = -1;
		for (int i = 0; i < taskList.size(); i++)
		{
			PropertyChangeEvent event = taskList.get(i);
			if (e.getSource() == event.getSource())
				changedTask = i;
		}
		if (changedTask == -1)
			taskList.add(e);
		else
			taskList.set(changedTask, e);
		removeFinishedTask();
		modifyLabel();
	}
	
	private void removeFinishedTask()
	{
		for (int i = taskList.size() - 1; i >= 0; i--)
		{
			PropertyChangeEvent event = taskList.get(i);
			if (event.getSource() instanceof SwingWorker)
				if (event.getNewValue().toString().equalsIgnoreCase("DONE"))
					taskList.remove(i);
		}
	}
	
	private void modifyLabel()
	{
		if (taskList.size() == 0)
			statusLabel.setText("Done.");
		else
		{
			PropertyChangeEvent event = taskList.get(0);
			String s = "";
			if (event.getSource() instanceof SwingWorker)
				s = String.format("%s %s: %s", event.getSource().getClass().getSimpleName(), event.getPropertyName(), event.getNewValue().toString());
			if (taskList.size() > 1)
				 s += (String.format(" (%d more tasks performing)", taskList.size() - 1));
			statusLabel.setText(s);
		}
	}
	
}
