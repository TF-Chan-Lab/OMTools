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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import aldenjava.opticalmapping.data.data.SegmentIdentifier;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class MAMergeGraph extends MADependentDirectedGraph {
	Map<SegmentIdentifier, String> entrySegmentIndices = new LinkedHashMap<>();
	Map<String, Set<SegmentIdentifier>> entrySegments = new LinkedHashMap<>();
	Map<String, GroupingEntry> groupingEntryMap = new LinkedHashMap<>();
	Map<String, Integer> chainEntryIndice = new LinkedHashMap<>(); // State the chain a groupingEntry belongs to
	Map<Integer, Set<String>> chainEntries = new LinkedHashMap<>();

	public MAMergeGraph(Set<GroupingEntryChain> chains) {
		super(chains);
		VerbosePrinter.println("Initializing merge graph...");
		for (GroupingEntryChain chain : chains) {
			List<GroupingEntry> groupingEntries = chain.groupingEntries;
			Set<String> chainEntriesSet = chain.groupingEntries.stream().map(groupingEntry -> groupingEntry.name).collect(Collectors.toSet());
			chainEntries.put(chain.id, chainEntriesSet);
			groupingEntries.forEach(groupingEntry -> {
				chainEntryIndice.put(groupingEntry.name, chain.id);
				groupingEntryMap.put(groupingEntry.name, groupingEntry);
				entrySegments.put(groupingEntry.name, groupingEntry.getSegmentIdentifiers());
				entrySegments.get(groupingEntry.name).forEach(si -> entrySegmentIndices.put(si, groupingEntry.name));
			});
		}
	}

	@Override
	public void merge(String b1, String b2) {
		super.merge(b1, b2);
		// Update groupingEntryMap
		groupingEntryMap.get(b1).groups.putAll(groupingEntryMap.get(b2).groups);
		groupingEntryMap.remove(b2);
				
		// Update segments
		Set<SegmentIdentifier> sis = entrySegments.remove(b2);
		for (SegmentIdentifier si : sis)
			entrySegmentIndices.put(si, b1);
		entrySegments.get(b1).addAll(sis);
		
		// Update chain
		int b1ID = chainEntryIndice.get(b1);
		int b2ID = chainEntryIndice.get(b2);
		if (b1ID != b2ID) {
			// Add all entries from b2-chain to b1-chain
			chainEntries.get(b2ID).forEach(name -> chainEntryIndice.put(name, b1ID));
			chainEntries.get(b1ID).addAll(chainEntries.remove(b2ID));
		}
		// Remove b2 from b1-chain
		chainEntryIndice.remove(b2);
		chainEntries.get(b1ID).remove(b2);

	}

	/**
	 * Reverse the specified chain
	 * @param chainIndex
	 */
	public void reverseChain(Integer chainIndex) {
		Set<String> blocksToReverse = chainEntries.get(chainIndex);
		blocksToReverse.forEach(key -> {
			Set<String> tmp = nextBlocks.get(key);
			nextBlocks.put(key, prevBlocks.get(key));
			prevBlocks.put(key, tmp);
			Set<String> tmp2 = dependentNextBlocks.get(key);
			dependentNextBlocks.put(key, dependentPrevBlocks.get(key));
			dependentPrevBlocks.put(key, tmp2);
			groupingEntryMap.put(key, groupingEntryMap.get(key).getReverse(key));
		});
	}
	
	
	public Set<GroupingEntryChain> getChains() {
		// This procedure will disrupt the prevBlocks
		assert (groupingEntryMap.size() == chainEntries.values().stream().mapToInt(Set::size).sum());
		
		Set<GroupingEntryChain> newChains = new LinkedHashSet<>();
		for (Entry<Integer, Set<String>> chainEntry : chainEntries.entrySet()) {
			Integer chainID = chainEntry.getKey();
			Set<String> chainEntriesSet = chainEntry.getValue();
			List<GroupingEntry> newGroupingEntries = new ArrayList<>();
			while (newGroupingEntries.size() < chainEntriesSet.size()) {
				Entry<String, Set<String>> sourceEntry = prevBlocks.entrySet().stream().filter(e -> chainEntriesSet.contains(e.getKey()) && e.getValue().isEmpty()).findFirst().get();
				String thisBlock = sourceEntry.getKey();
				
				while (thisBlock != null) {
					String blockToRemove = thisBlock;
					nextBlocks.get(blockToRemove).forEach(nextBlock -> prevBlocks.get(nextBlock).remove(blockToRemove));
					prevBlocks.remove(blockToRemove);
					newGroupingEntries.add(groupingEntryMap.get(blockToRemove));
					
					if (nextBlocks.get(thisBlock).size() == 1 && prevBlocks.get(nextBlocks.get(thisBlock).iterator().next()).size() == 0)
						thisBlock = nextBlocks.get(thisBlock).iterator().next();
					else
						thisBlock = null;
				}
			}
			newChains.add(new GroupingEntryChain(chainID, newGroupingEntries));
		}
		return newChains;

	}
	public int getNumberOfChains() {
		return chainEntries.size();
	}
	
	public int getNumberOfBlocks() {
		return groupingEntryMap.size();
	}

}
