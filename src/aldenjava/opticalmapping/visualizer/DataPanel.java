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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang.ArrayUtils;
 
public class DataPanel extends JPanel {
    
	// The selected values are extracted from these JLists	
	private LinkedHashMap<VDataType, JList<String>> jLists = new LinkedHashMap<>();
	
    public DataPanel(LinkedHashMap<VDataType, List<String>> emptyDataSelection, LinkedHashMap<VDataType, List<String>> oldDataSelection) {
    	super(new GridLayout(1,3));
        for (Entry<VDataType, List<String>> entry : emptyDataSelection.entrySet()) {
        	List<String> dataSelectionList = new ArrayList<>(entry.getValue());
        	Collections.sort(dataSelectionList);
        	JPanel listContainer = new JPanel(new GridLayout(1,1));
        	listContainer.setBorder(BorderFactory.createTitledBorder(entry.getKey().getDescription()));
        	JList<String> list = new JList<>(dataSelectionList.toArray(new String[entry.getValue().size()]));
        	if (oldDataSelection != null) {
        		List<Integer> selectedIndices = new ArrayList<>();
        		for (String selected : oldDataSelection.get(entry.getKey())) {
        			int index = dataSelectionList.indexOf(selected);
        			if (index != -1)
        				selectedIndices.add(index);
        		}
        		list.setSelectedIndices(ArrayUtils.toPrimitive(selectedIndices.toArray(new Integer[selectedIndices.size()])));
        	}
        	JScrollPane listPane = new JScrollPane(list);
        	listContainer.add(listPane);
        	add(listContainer);
        	jLists.put(entry.getKey(), list);
        }
        setMinimumSize(new Dimension(250, 250));
        setPreferredSize(new Dimension(750, 500));
    }
    
    // Obtain the selected values
    public List<String> getValues(VDataType type) {
    	return jLists.get(type).getSelectedValuesList();
    }
    public LinkedHashMap<VDataType, List<String>> getValues() {
    	LinkedHashMap<VDataType, List<String>> map = new LinkedHashMap<VDataType, List<String>>();
    	for (VDataType type : jLists.keySet())
    		map.put(type, jLists.get(type).getSelectedValuesList());
    	return map;
    }
}