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


package aldenjava.opticalmapping.multiplealignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aldenjava.opticalmapping.data.Identifiable;
import aldenjava.opticalmapping.data.data.DataNode;

public class CollinearBlock implements Identifiable<String> {
	public final String name;
	public final LinkedHashMap<String, BlockInfo> groups;
	public CollinearBlock(String name) {
		this.name = name;
		this.groups = new LinkedHashMap<>();
	}

	public CollinearBlock(String name, LinkedHashMap<String, BlockInfo> groups) {
		this.name = name;
		this.groups = groups;
	}
	
	public void reverse() {
		for (BlockInfo vpm : groups.values())
			vpm.reverse();
	}
	
	public int getNumberOfSignals() {
		if (groups.size() == 0)
			return 0;
		return groups.values().iterator().next().getNumberOfSignals();
	}
	
	public List<Long> toSegments(Map<String, DataNode> dataMap) {
		List<Long> segments = new ArrayList<>();
		for (int round = 0; round < getNumberOfSignals() - 1; round++) {
			long totalSize = 0;
			for (String key : groups.keySet()) {
				BlockInfo pmi = groups.get(key);
				long s1;
				long s2;
				if (!pmi.isReverse()) {
					s1 = dataMap.get(key).refp[pmi.startSig + round];
					s2 = dataMap.get(key).refp[pmi.startSig + 1 + round];
				}
				else {
					s1 = dataMap.get(key).refp[pmi.startSig - 1 - round];
					s2 = dataMap.get(key).refp[pmi.startSig - round];	
				}
				totalSize += Math.abs(s2 - s1 - 1);
			}
			segments.add(totalSize / groups.size());
		}
		return segments;		
	}
	
	@Override
	public String getIdentifier() {
		return name;
	}


	/**
	 * Converts CollinearBlock to GroupingEntry
	 * @param collinearBlocks Multiple alignment in List of CollinearBlock
	 * @return Multiple alignment in List of GroupingEntry 
	 */
	public static List<GroupingEntry> toGroupingEntries(LinkedHashMap<String, CollinearBlock> collinearBlocks) {
		List<GroupingEntry> entries = new ArrayList<>();
		for (CollinearBlock block : collinearBlocks.values()) {
			List<LinkedHashMap<String, SingleGroup>> groupsList = new ArrayList<>();
			for (String key : block.groups.keySet()) {
				BlockInfo vpm = block.groups.get(key);
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

	/**
	 * Converts GroupingEntry to CollinearBlock. Successive GroupingEntrys that share same queries are compressed into one CollinearBlock. 
	 * @param collinearBlocks Multiple alignment in List of GroupingEntry
	 * @return Multiple alignment in List of CollinearBlock 
	 */
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
					LinkedHashMap<String, BlockInfo> pmiMap = new LinkedHashMap<String, BlockInfo>();
					for (String key : outputFirstEntry.groups.keySet()) {
						SingleGroup g1 = outputFirstEntry.groups.get(key);
						SingleGroup g2 = outputLastEntry.groups.get(key);
						if (g1 != null) {
							assert g2 != null;
							assert g1.orientation == g2.orientation;
						}
						int startSig = outputFirstEntry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
						int stopSig = outputLastEntry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
						pmiMap.put(key, new BlockInfo(startSig, stopSig));
					}
					collinearBlocks.add(new CollinearBlock(name, pmiMap));
					outputFirstEntry = entry;
					outputLastEntry = entry;
					
				}
			
		}
		if (outputLastEntry != null) {
			String name = "Block" + nextID++;
			LinkedHashMap<String, BlockInfo> pmiMap = new LinkedHashMap<String, BlockInfo>();
			for (String key : outputFirstEntry.groups.keySet()) {
				SingleGroup g1 = outputFirstEntry.groups.get(key);
				SingleGroup g2 = outputLastEntry.groups.get(key);
				if (g1 != null) {
					assert g2 != null;
					assert g1.orientation == g2.orientation;
				}
				int startSig = outputFirstEntry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
				int stopSig = outputLastEntry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
				pmiMap.put(key, new BlockInfo(startSig, stopSig));
			}
			collinearBlocks.add(new CollinearBlock(name, pmiMap));
		}
		return collinearBlocks;
	}

	/**
	 * Converts GroupingEntry to CollinearBlock. Successive GroupingEntrys that share same queries are not compressed. 
	 * @param collinearBlocks Multiple alignment in List of GroupingEntry
	 * @return Multiple alignment in List of CollinearBlock  
	 */
	public static List<CollinearBlock> toSingleSegmentCollinearBlocks(List<GroupingEntry> entries) {
		List<CollinearBlock> collinearBlocks = new ArrayList<>();
		// Reset the IDs for output group entries		
		int nextID = 1;
		for (GroupingEntry entry : entries) {
//			String name = entry.name;
			String name = "Block"+nextID++;
			LinkedHashMap<String, BlockInfo> pmiMap = new LinkedHashMap<String, BlockInfo>();
			for (String key : entry.groups.keySet()) {
				SingleGroup g1 = entry.groups.get(key);
				SingleGroup g2 = entry.groups.get(key);
				if (g1 != null) {
					assert g2 != null;
					assert g1.orientation == g2.orientation;
				}
				int startSig = entry.groups.get(key).segment + (g1.orientation == 1? -1 : 0);
				int stopSig = entry.groups.get(key).segment + (g2.orientation == 1? 0 : -1);
				pmiMap.put(key, new BlockInfo(startSig, stopSig));
			}
			collinearBlocks.add(new CollinearBlock(name, pmiMap));
		}
		return collinearBlocks;
	}

	/**
	 * Filters the multiple alignment results according to the queries. 
	 * @param collinearBlocks
	 * @param queries
	 * @return filtered multiple alignment results
	 */
	public static LinkedHashMap<String, CollinearBlock> filter(LinkedHashMap<String, CollinearBlock> collinearBlocks, Set<String> queries) {
		LinkedHashMap<String, CollinearBlock> newCollinearBlocks = new LinkedHashMap<>();
		for (Entry<String, CollinearBlock> entry : collinearBlocks.entrySet())
			if (!Collections.disjoint(queries, entry.getValue().groups.keySet())) {
				String key = entry.getKey();
				LinkedHashMap<String, BlockInfo> map = new LinkedHashMap<>();
				for (String o : queries) 
					if (entry.getValue().groups.containsKey(o))
						map.put(o, entry.getValue().groups.get(o));
				CollinearBlock block = new CollinearBlock(key, map);
				newCollinearBlocks.put(key, block);
			}

		// Convert back to GroupingEntry to recompress
		// newCollinearBlocks = Identifiable.convertToMap(toCollinearBlocks(toGroupingEntries(new ArrayList<>(newCollinearBlocks.values()))))

		return newCollinearBlocks;
	}
	
	
	@Deprecated
	public long getSize(Map<String, DataNode> dataMap) {
		
		// WRONG!
		long totalSize = 0;
		for (String key : groups.keySet()) {
			long s1 = dataMap.get(key).refp[groups.get(key).startSig];
			long s2 = dataMap.get(key).refp[groups.get(key).stopSig];
			totalSize += Math.abs(s2 - s1);
		}
		return totalSize / groups.size();
	}

}