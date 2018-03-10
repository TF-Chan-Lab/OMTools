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


package aldenjava.opticalmapping.visualizer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
 
public class ReferenceDataSelectionPanel extends JPanel {
    
	// The selected values are extracted from these JLists	
	private JList<File> refJList;
	private JList<File> dataJList;
	private List<File> refList;
	private List<File> dataList;
	private void rebuildJList() {
		Collections.sort(refList);
		Collections.sort(dataList);
		refJList.setListData(refList.toArray(new File[refList.size()]));
		dataJList.setListData(dataList.toArray(new File[dataList.size()]));
	}
    public ReferenceDataSelectionPanel(List<File> refFiles, List<File> dataFiles) {
    	super(new FlowLayout());
        this.refList = refFiles;
        this.dataList = dataFiles;
    	{
    		JPanel listContainer = new JPanel(new GridLayout(1,1));
    		listContainer.setBorder(BorderFactory.createTitledBorder("Reference")); 
    		refJList = new JList<>();
	    	JScrollPane listPane = new JScrollPane(refJList);
	    	listContainer.add(listPane);
	    	listContainer.setPreferredSize(new Dimension(400, 500));
	    	this.add(listContainer);
    	}
    	
    	JPanel buttonPanel = new JPanel();
    	buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
    	JButton allRefToData = new JButton(">>");
    	allRefToData.setAlignmentX(Component.CENTER_ALIGNMENT);
    	allRefToData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dataList.addAll(refList);
				refList.clear();
				rebuildJList();
			}
    	});
    	JButton refToData = new JButton(">");
    	refToData.setAlignmentX(Component.CENTER_ALIGNMENT);
    	refToData.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<File> selectedRefList = refJList.getSelectedValuesList();
				refList.removeAll(selectedRefList);
				dataList.addAll(selectedRefList);
				rebuildJList();
			}
    	});
    	JButton dataToRef = new JButton("<");
    	dataToRef.setAlignmentX(Component.CENTER_ALIGNMENT);
    	dataToRef.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<File> selectedDataList = dataJList.getSelectedValuesList();
				dataList.removeAll(selectedDataList);
				refList.addAll(selectedDataList);
				rebuildJList();
			}
    	});
    	JButton allDataToRef = new JButton("<<");
    	allDataToRef.setAlignmentX(Component.CENTER_ALIGNMENT);
    	allDataToRef.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refList.addAll(dataList);
				dataList.clear();
				rebuildJList();
			}
    	});

    	buttonPanel.add(allRefToData);
    	buttonPanel.add(refToData);
    	buttonPanel.add(dataToRef);
    	buttonPanel.add(allDataToRef);
    	add(buttonPanel);
    	
    	
    	{
    		JPanel listContainer = new JPanel(new GridLayout(1,1));
    		listContainer.setBorder(BorderFactory.createTitledBorder("Molecule"));
	    	dataJList = new JList<>();
	    	JScrollPane listPane = new JScrollPane(dataJList);
	    	listContainer.add(listPane);
	    	listContainer.setPreferredSize(new Dimension(400, 500));
	    	this.add(listContainer);
    	}	
    	
    	rebuildJList();
        	
        setMinimumSize(new Dimension(250, 250));
        setPreferredSize(new Dimension(1000, 500));
    }
    
    public List<File> getReferenceFiles() {
    	return refList;
    }
    public List<File> getDataFiles() {
    	return dataList;
    }
    
}