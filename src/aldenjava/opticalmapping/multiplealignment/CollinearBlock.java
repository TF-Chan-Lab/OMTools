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


package aldenjava.opticalmapping.multiplealignment;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.visualizer.utils.VPartialMoleculeInfo;

public class CollinearBlock {
	public String name;
	public LinkedHashMap<String, VPartialMoleculeInfo> groups;
	public CollinearBlock(String name) {
		this.name = name;
		this.groups = new LinkedHashMap<>();
	}

	public CollinearBlock(String name, LinkedHashMap<String, VPartialMoleculeInfo> groups) {
		this.name = name;
		this.groups = groups;
	}
	
	public void reverse() {
		for (VPartialMoleculeInfo vpm : groups.values())
			vpm.reverse();
	}
	
	public static List<GroupingEntry> toGroupingEntries(List<CollinearBlock> collinearBlocks) {
		List<GroupingEntry> entries = new ArrayList<>();
		for (CollinearBlock block : collinearBlocks) {
			List<LinkedHashMap<String, SingleGroup>> groupsList = new ArrayList<>();
			for (String key : block.groups.keySet()) {
				VPartialMoleculeInfo vpm = block.groups.get(key);
				boolean reverse = vpm.isReverse();
				int startSig = vpm.startSig;
				int stopSig = vpm.stopSig;
				int totalSegment = Math.abs(stopSig - startSig);
				if (groupsList.isEmpty()) 
					for (int i = 0; i < totalSegment; i++)
						groupsList.add(new LinkedHashMap<String, SingleGroup>());
				else
					if (groupsList.size() != totalSegment)
						throw new UnsupportedOperationException("Conversion does not allow mismatch number of segments in different queries: " + key);
				for (int i = 0; i < totalSegment; i++)
					groupsList.get(i).put(key, reverse? new SingleGroup(startSig - i, -1) : new SingleGroup(startSig + 1 + i, 1));
			}
			for (int i = 0; i < groupsList.size(); i++)
				entries.add(new GroupingEntry(block.name + "_" + i, groupsList.get(i)));
			
		}
		return entries;
	}
	public static List<CollinearBlock> toCollinearBlocks(List<GroupingEntry> entries) {
		List<CollinearBlock> collinearBlocks = new ArrayList<>();
		// Reset the IDs for output group entries		
		int nextID;
		nextID = 1;
		GroupingEntry outputFirstEntry = null;
		GroupingEntry outputLastEntry = null;
		for (GroupingEntry entry : entries) {
			if (outputLastEntry == null) {
				outputFirstEntry = entry;
				outputLastEntry = entry;
			}
			else
				if (outputLastEntry.canDirectlyConnect(entry)) {
					outputLastEntry = entry;
				}
				else {
					String name = "Block" + nextID++;
					LinkedHashMap<String, VPartialMoleculeInfo> pmiMap = new LinkedHashMap<String, VPartialMoleculeInfo>();
					for (String key : outputFirstEntry.groups.keySet()) {
						SingleGroup g1 = outputFirstEntry.groups.get(key);
						SingleGroup g2 = outputLastEntry.groups.get(key);
						if (g1 != null) {
							assert g2 != null;
							assert g1.orientation == g2.orientation;
						}
						int startSig = outputFirstEntry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
						int stopSig = outputLastEntry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
						pmiMap.put(key, new VPartialMoleculeInfo(startSig, stopSig));
					}
					collinearBlocks.add(new CollinearBlock(name, pmiMap));
					outputFirstEntry = entry;
					outputLastEntry = entry;
					
				}
			
		}
		if (outputLastEntry != null) {
			String name = "Block" + nextID++;
			LinkedHashMap<String, VPartialMoleculeInfo> pmiMap = new LinkedHashMap<String, VPartialMoleculeInfo>();
			for (String key : outputFirstEntry.groups.keySet()) {
				SingleGroup g1 = outputFirstEntry.groups.get(key);
				SingleGroup g2 = outputLastEntry.groups.get(key);
				if (g1 != null) {
					assert g2 != null;
					assert g1.orientation == g2.orientation;
				}
				int startSig = outputFirstEntry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
				int stopSig = outputLastEntry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
				pmiMap.put(key, new VPartialMoleculeInfo(startSig, stopSig));
			}
			collinearBlocks.add(new CollinearBlock(name, pmiMap));
		}
		return collinearBlocks;
	}
	public static List<CollinearBlock> toSingleSegmentCollinearBlocks(List<GroupingEntry> entries) {
		List<CollinearBlock> collinearBlocks = new ArrayList<>();
		// Reset the IDs for output group entries		
		int nextID = 1;
		for (GroupingEntry entry : entries) {
			String name = entry.name;
			LinkedHashMap<String, VPartialMoleculeInfo> pmiMap = new LinkedHashMap<String, VPartialMoleculeInfo>();
			for (String key : entry.groups.keySet()) {
				SingleGroup g1 = entry.groups.get(key);
				SingleGroup g2 = entry.groups.get(key);
				if (g1 != null) {
					assert g2 != null;
					assert g1.orientation == g2.orientation;
				}
				int startSig = entry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
				int stopSig = entry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
				pmiMap.put(key, new VPartialMoleculeInfo(startSig, stopSig));
			}
			collinearBlocks.add(new CollinearBlock(name, pmiMap));
		}
		return collinearBlocks;
	}

	public long getSize(LinkedHashMap<String, DataNode> dataMap) {
		long totalSize = 0;
		for (String key : groups.keySet()) {
			long s1 = dataMap.get(key).refp[groups.get(key).startSig];
			long s2 = dataMap.get(key).refp[groups.get(key).stopSig];
			totalSize += Math.abs(s2 - s1);
		}
		return totalSize / groups.size();
	}

}