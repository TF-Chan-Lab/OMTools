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

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class MADependentDirectedGraph extends MADirectedGraph {

	public Map<String, Set<String>> dependentPrevBlocks = new LinkedHashMap<>();
	public Map<String, Set<String>> dependentNextBlocks = new LinkedHashMap<>();
	public MADependentDirectedGraph(Set<GroupingEntryChain> chains) {
		super(chains);
		initialize(chains);
	}

	private void initialize(Set<GroupingEntryChain> chains) {
		VerbosePrinter.println("Initializing dependent block sets..."); 
		for (GroupingEntryChain chain : chains) {
			List<GroupingEntry> groupingEntries = chain.groupingEntries;
			groupingEntries.forEach(groupingEntry -> {
				Set<String> set1 = new LinkedHashSet<String>();
				Set<String> set2 = new LinkedHashSet<String>();
				// add the block itself
				set1.add(groupingEntry.name);
				set2.add(groupingEntry.name);
				dependentPrevBlocks.put(groupingEntry.name, set1);
				dependentNextBlocks.put(groupingEntry.name, set2);
			});
	
			// Parse the dependentPrevBlocks based on nextBlocks
			for (int i = 0; i < groupingEntries.size(); i++) {
				String group = groupingEntries.get(i).name;
				Set<String> currentSet = dependentPrevBlocks.get(group);
				for (String nextBlock : nextBlocks.get(group))
					dependentPrevBlocks.get(nextBlock).addAll(currentSet);
			}
			// Parse the dependentNextBlocks based on prevBlocks
			for (int i = groupingEntries.size() - 1; i >= 0; i--) {
				String group = groupingEntries.get(i).name;
				Set<String> currentSet = dependentNextBlocks.get(group);
				for (String prevBlock : prevBlocks.get(group))
					dependentNextBlocks.get(prevBlock).addAll(currentSet);
			}
		}
	}
	
	@Override
	public void merge(String b1, String b2) {
		super.merge(b1, b2);

		// Update merging blocks
		LinkedList<String> prevBlocksToUpdate = new LinkedList<>();
		prevBlocksToUpdate.add(b1);
		
		// Update merging blocks
		Set<String> alteredSet = dependentNextBlocks.get(b2).stream().filter(p -> !dependentNextBlocks.get(b1).contains(p)).collect(Collectors.toSet());
		while (!prevBlocksToUpdate.isEmpty()) {
			String group = prevBlocksToUpdate.pollFirst();
			// count++;
			Set<String> set = dependentNextBlocks.get(group);
			// boolean changed = set.addAll(dependentNextBlocks.get(group2));
			boolean changed = set.addAll(alteredSet);
			if (changed)
				prevBlocksToUpdate.addAll(prevBlocks.get(group));
		}
		Set<String> alteredSet2 = dependentPrevBlocks.get(b2).stream().filter(p -> !dependentPrevBlocks.get(b1).contains(p)).collect(Collectors.toSet());
		LinkedList<String> nextBlocksToUpdate = new LinkedList<>();
		nextBlocksToUpdate.add(b1);
		while (!nextBlocksToUpdate.isEmpty()) {
			String group = nextBlocksToUpdate.pollFirst();
			Set<String> set = dependentPrevBlocks.get(group);
			boolean changed = set.addAll(alteredSet2);
			if (changed)
				nextBlocksToUpdate.addAll(nextBlocks.get(group));
		}
	}
}
