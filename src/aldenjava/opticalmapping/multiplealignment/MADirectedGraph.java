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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class MADirectedGraph {
	public Map<String, Set<String>> prevBlocks = new LinkedHashMap<>();
	public Map<String, Set<String>> nextBlocks = new LinkedHashMap<>();

	public MADirectedGraph(Set<GroupingEntryChain> chains) {
		VerbosePrinter.println("Initializing block connections...");
		prevBlocks = new LinkedHashMap<>();
		nextBlocks = new LinkedHashMap<>();
		for (GroupingEntryChain chain : chains) {
			LinkedHashMap<String, String> lastBlock = new LinkedHashMap<>();
			List<GroupingEntry> groupingEntries = chain.groupingEntries;
			
			groupingEntries.forEach((groupingEntry) -> {
				String group = groupingEntry.name;
				Set<String> prevBlockSet = new LinkedHashSet<>();
				groupingEntry.groups.forEach((queryName, sg) -> {
					if (lastBlock.containsKey(queryName))
						prevBlockSet.add(lastBlock.get(queryName));
					lastBlock.put(queryName, group);
				});
				prevBlocks.put(group, prevBlockSet);
	
				nextBlocks.put(group, new LinkedHashSet<String>());
				prevBlockSet.forEach(prevBlock -> {
					nextBlocks.get(prevBlock).add(group);
				});
	
			});
	
		}
	}
	public MADirectedGraph(LinkedHashMap<String, CollinearBlock> collinearBlocks) {
		VerbosePrinter.println("Initializing block connections...");
		prevBlocks = new LinkedHashMap<>();
		nextBlocks = new LinkedHashMap<>();
		Map<String, String> lastBlock = new LinkedHashMap<>();
		collinearBlocks.values().forEach(block -> {
			String group = block.name;
			Set<String> prevBlockSet = new LinkedHashSet<>();
			block.groups.forEach((queryName, sg) -> {
				if (lastBlock.containsKey(queryName))
					prevBlockSet.add(lastBlock.get(queryName));
				lastBlock.put(queryName, group);
			});
			
			prevBlocks.put(group, prevBlockSet);
			
			nextBlocks.put(group, new LinkedHashSet<String>());
			prevBlockSet.forEach((prevBlock) -> {
				nextBlocks.get(prevBlock).add(group);
			});
		});

	}

	public void merge(String b1, String b2) {
		prevBlocks.get(b1).addAll(prevBlocks.get(b2));
		nextBlocks.get(b1).addAll(nextBlocks.get(b2));
		prevBlocks.get(b2).forEach(g -> {
			nextBlocks.get(g).add(b1);
			nextBlocks.get(g).remove(b2);
		});
		nextBlocks.get(b2).forEach(g -> {
			prevBlocks.get(g).add(b1);
			prevBlocks.get(g).remove(b2);
		});

	}

//	public List<List<String>> concatenateBlocks() {
//		String startBlock = null;
//		
//		List<String> blocksToConcatenate = new ArrayList<>();
//		blocksToConcatenate.add(startBlock);
//		
//		String currentBlock = startBlock;
//		boolean currentBlockHasUpdate = true;
//		while (currentBlockHasUpdate) {
//			currentBlockHasUpdate = false;
//			if (nextBlocks.get(currentBlock).size() == 1) {
//				String potentialBlock = nextBlocks.get(startBlock).iterator().next();
//				assert prevBlocks.get(potentialBlock).size() >= 1;
//				if (prevBlocks.get(potentialBlock).size() == 1) {
//					currentBlock = potentialBlock;
//					blocksToConcatenate.add(currentBlock);
//					currentBlockHasUpdate = true;
//				}
//			}
//		}
//		String stopBlock = currentBlock;
//		
//		if (blocksToConcatenate.size() >= 2) {
//			String newBlockName = "";
//			
//			// Update the reference outside the block chain
//			for (String block : prevBlocks.get(startBlock)) {
//				nextBlocks.get(block).remove(startBlock);
//				nextBlocks.get(block).add(newBlockName);
//			}
//			for (String block : nextBlocks.get(stopBlock)) {
//				prevBlocks.get(block).remove(stopBlock);
//				prevBlocks.get(block).add(newBlockName);
//			}
//			
//			// Add new reference block representing the block chain
//			prevBlocks.put(newBlockName, prevBlocks.get(startBlock));
//			nextBlocks.put(newBlockName, nextBlocks.get(stopBlock));
//			
//			// Remove all reference within the block chain
//			for (String block : blocksToConcatenate) {
//				prevBlocks.remove(block);
//				nextBlocks.remove(block);
//			}
//			
//			
//			
//			
//		}
//			
//	}
}
