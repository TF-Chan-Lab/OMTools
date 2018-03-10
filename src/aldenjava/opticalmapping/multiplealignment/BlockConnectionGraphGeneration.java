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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class BlockConnectionGraphGeneration {


	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(BlockConnectionGraphGeneration.class.getSimpleName(), "Generates a dot graph file representing the progression of collinear blocks.");
		
		CollinearBlockReader.assignOptions(parser, 1);
		
		parser.addHeader("Block connection graph generation options", 1);
		OptionSpec<String> omaref = parser.accepts("maref", "References for multiple alignment").withRequiredArg().ofType(String.class);
		OptionSpec<Boolean> orev = parser.accepts("reverse", "Reverse the direction of progression of collinear blocks").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<Integer> ominedgeweight = parser.accepts("minedgeweight", "Filter with minimum edge weight").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Boolean> odisplayall = parser.accepts("displayall", "Output all blocks (Set false to stop the program from outputing blocks without any linkage to other block)").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		OptionSpec<String> odotout = parser.accepts("dotout", "Dot file output").withRequiredArg().ofType(String.class).required();
				
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		
		// options.valueOf();
		
		LinkedHashMap<String, CollinearBlock> collinearBlocks = CollinearBlockReader.readAllData(options);
//
		
		Map<String, Map<String, BlockConnectionEdge>> prevBlocks = new HashMap<>();
		Map<String, Map<String, BlockConnectionEdge>> nextBlocks = new HashMap<>();
	
		LinkedHashMap<String, String> lastBlock = new LinkedHashMap<>();
		collinearBlocks.values().forEach(block -> {
			String group = block.name;
			Map<String, BlockConnectionEdge> prevBlockMap = new HashMap<>();
			block.groups.forEach((queryName, sg) -> {
				if (lastBlock.containsKey(queryName)) {
					if (!prevBlockMap.containsKey(lastBlock.get(queryName)))
						prevBlockMap.put(lastBlock.get(queryName), new BlockConnectionEdge());
					prevBlockMap.get(lastBlock.get(queryName)).addSupport();;
					if (!(collinearBlocks.get(lastBlock.get(queryName)).groups.get(queryName).stopSig == collinearBlocks.get(group).groups.get(queryName).startSig
							&& (collinearBlocks.get(lastBlock.get(queryName)).groups.get(queryName).isReverse() == collinearBlocks.get(group).groups.get(queryName).isReverse())))
						prevBlockMap.get(lastBlock.get(queryName)).addRearrangement();
				}
				lastBlock.put(queryName, group);
			});
			
			prevBlocks.put(group, prevBlockMap);
			
			
			nextBlocks.put(group, new HashMap<String, BlockConnectionEdge>());
			prevBlockMap.forEach((prevBlock, edge) -> {
				nextBlocks.get(prevBlock).put(group, new BlockConnectionEdge(edge));
			});
		});
		
		int minEdgeWeight = options.valueOf(ominedgeweight);
		Set<String> usedBlocks = new HashSet<>();
		// Draw after block 
		List<String> linesToWrite = new ArrayList<>();
		boolean reverse = options.valueOf(orev);
		linesToWrite.add("digraph g {");
		nextBlocks.forEach((k,v) -> {
			v.forEach((s, edge) -> {
				if (edge.support >= minEdgeWeight) {
					if (edge.support - edge.rearrangement > 0) {
						int support = edge.support - edge.rearrangement;
						linesToWrite.add("\"" + (reverse?(s + "\" -> \"" + k):(k + "\" -> \"" + s)) + "\" [label=" + support + ", penwidth=" + (support>10?10:support)/2.0 + ", weight=" + (support>10?10:support)/2.0 + ", color=black" + "]");
					}
					if (edge.rearrangement > 0) {
						int support = edge.rearrangement;
						linesToWrite.add("\"" + (reverse?(s + "\" -> \"" + k):(k + "\" -> \"" + s)) + "\" [label=" + support + ", penwidth=" + (support>10?10:support)/2.0 + ", weight=" + (support>10?10:support)/2.0 + ", color=red" + "]");
					}
					usedBlocks.add(s);
					usedBlocks.add(k);
				}
			});
		});
		boolean displayAll = options.valueOf(odisplayall);
		List<String> marefs = options.valuesOf(omaref);
		collinearBlocks.values().forEach(v -> { 
			if (displayAll || usedBlocks.contains(v.name))
				linesToWrite.add("\"" + v.name + "\"" + (Collections.disjoint(marefs, v.groups.keySet())?"":" [style=filled,fillcolor=yellow]"));
		});
		linesToWrite.add("}");
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(options.valueOf(odotout)))) {
			for (String line : linesToWrite)
				bw.write(line + "\n");
		}
		
	}


}

class BlockConnectionEdge {
	int support;
	int rearrangement;
	public BlockConnectionEdge() {
		this.support = 0;
		this.rearrangement = 0;
	}

	public BlockConnectionEdge(int support, int rearrangement) {
		this.support = support;
		this.rearrangement = rearrangement;
	}
	public BlockConnectionEdge(BlockConnectionEdge edge) {
		this.support = edge.support;
		this.rearrangement = edge.rearrangement;
	}

	void addSupport() {
		support++;
	};
	void addRearrangement() {
		rearrangement++;
	};
}
