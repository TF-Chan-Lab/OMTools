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


package aldenjava.opticalmapping.phylogenetic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.multiplealignment.BlockInfo;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockReader;
import aldenjava.opticalmapping.multiplealignment.MADirectedGraph;
import aldenjava.opticalmapping.multiplealignment.MultipleAlignment;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;


public class UPGMATreeConstruction {
	public static PhylogeneticTree constructTree(Collection<String> queryNames, Map<String, Map<String, Double>> distanceMatrix) {
		Map<String, PhylogeneticTree> initialTrees = queryNames.stream().collect(Collectors.toMap(name -> name, name -> new PhylogeneticTree(name)));
		Map<PhylogeneticTree, Map<PhylogeneticTree, TreeDistance>> distances = distanceMatrix.entrySet().stream().collect(Collectors.toMap(entry1 -> initialTrees.get(entry1.getKey()), entry1 -> entry1.getValue().entrySet().stream().collect(Collectors.toMap(entry2 -> initialTrees.get(entry2.getKey()), entry2 -> new TreeDistance(initialTrees.get(entry1.getKey()), initialTrees.get(entry2.getKey()), entry2.getValue())))));
		Set<PhylogeneticTree> remainingTrees = initialTrees.values().stream().collect(Collectors.toSet());
		PriorityQueue<TreeDistance> queue = distances.values().stream().flatMap(m -> m.values().stream()).collect(Collectors.toCollection(PriorityQueue::new));
		
		while (!queue.isEmpty() && remainingTrees.size() > 1) {
			TreeDistance td = queue.poll();			
			if (remainingTrees.contains(td.getTree1()) && remainingTrees.contains(td.getTree2())
					&& td.getTree1() != td.getTree2()) {
				PhylogeneticTree newTree = new PhylogeneticTree(td.getTree1(), td.getTree2(), td.getDistance() / 2);
				remainingTrees.remove(td.getTree1());
				remainingTrees.remove(td.getTree2());
				remainingTrees.add(newTree);
				distances.put(newTree, new HashMap<>());
				remainingTrees.forEach(tree -> {
					if (tree != newTree) {
						TreeDistance dissimilarity1 = distances.get(td.getTree1()).get(tree);
						TreeDistance dissimilarity2 = distances.get(td.getTree2()).get(tree);
						double newDistance = (dissimilarity1.getDistance() * td.getTree1().getNumberOfElements() + dissimilarity2.getDistance() * td.getTree2().getNumberOfElements()) / (td.getTree1().getNumberOfElements() + td.getTree2().getNumberOfElements());
						TreeDistance newDissimilarity = new TreeDistance(newTree, tree, newDistance);
						queue.add(newDissimilarity);
						distances.get(tree).put(newTree, newDissimilarity);
						distances.get(newTree).put(tree, newDissimilarity);
					}
				});
			}
		}
		return remainingTrees.iterator().next();
	}

	private static Map<String, Map<String, Double>> getDissimilarityMatrix(String startBlock, String stopBlock, LinkedHashMap<String, CollinearBlock> collinearBlocks, MADirectedGraph maGraph) {

		// Check rearrangement
		HashSet<String> visited = new HashSet<>();
		visited.add(stopBlock);
		boolean hasRearrangement = false;
		LinkedList<String> toProcess = new LinkedList<>();
		toProcess.add(startBlock);
		while (!toProcess.isEmpty()) {
			String currentBlock = toProcess.pollFirst();
			
			Set<String> nextBlocks = maGraph.nextBlocks.get(currentBlock);
			
			LinkedHashMap<String, BlockInfo> currentBlockGroups = collinearBlocks.get(currentBlock).groups;
			int norearrangement = 0;
			for (String nextBlock : nextBlocks) {
				
				LinkedHashMap<String, BlockInfo> nextBlockGroups = collinearBlocks.get(nextBlock).groups;
				
				
				for (String queryName : currentBlockGroups.keySet())
					if (nextBlockGroups.containsKey(queryName))
						if (currentBlockGroups.get(queryName).stopSig == nextBlockGroups.get(queryName).startSig)
							norearrangement++;
			}
			if (norearrangement < currentBlockGroups.size())
				hasRearrangement = true;
			for (String nextBlock : nextBlocks) {
				if (!visited.contains(nextBlock)) {
					toProcess.add(nextBlock);
					visited.add(currentBlock);
				}
			}
		}
		if (hasRearrangement)
			VerbosePrinter.println("Warning: rearrangement detected");
		
		// Retain blocks between the start block and the stop block
		LinkedHashMap<String, CollinearBlock> newCollinearBlocks = collinearBlocks;
		for (String blockName : collinearBlocks.keySet())
			if (visited.contains(blockName))
				newCollinearBlocks.put(blockName, collinearBlocks.get(blockName));
		
		// Retain queries that appeared between the start block and the stop block
		Set<String> queries = new HashSet<>();
		for (CollinearBlock block : newCollinearBlocks.values())
			queries.addAll(block.groups.keySet());
		
		return MultipleAlignment.getDissimilarityMatrix(queries, CollinearBlock.toGroupingEntries(newCollinearBlocks));
		
	}
	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(UPGMATreeConstruction.class.getSimpleName(), "Reconstructs phylogenetic tree based on multiple alignment results using the UPGMA approach");
		parser.addHeader("UPGMATreeConstruction options", 1);
		parser.accepts("matrixout", "Output the distance matrix").withRequiredArg().ofType(String.class).required();
		parser.accepts("treeout", "Output trees in newick format").withRequiredArg().ofType(String.class).required();
		OptionSpec<String> ostartblock = parser.accepts("startblock", "Start block (Experimental parameters)").withRequiredArg().ofType(String.class);
		OptionSpec<String> ostopblock = parser.accepts("stopblock", "Stop block (Experimental parameters)").withRequiredArg().ofType(String.class);
		OptMapDataReader.assignOptions(parser);
		CollinearBlockReader.assignOptions(parser, 1);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		LinkedHashMap<String, DataNode> dataMap = OptMapDataReader.readAllData(options);
		LinkedHashMap<String, CollinearBlock> collinearBlocks = CollinearBlockReader.readAllData(options);
		
//		List<String> startBlocks = options.valuesOf(ostartblock);
//		List<String> stopBlocks = options.valuesOf(ostartblock);
//		if (startBlocks.size() != stopBlocks.size()) {
//			System.err.println("Mismatch of start blocks and stop blocks. Exit.");
//			return;
//		}
		
		Map<String, Map<String, Double>> dissimilarityMatrix;
		if (options.has(ostartblock) && options.has(ostopblock)) {
			MADirectedGraph maGraph = new MADirectedGraph(collinearBlocks);
			String startBlock = options.valueOf(ostartblock);
			String stopBlock = options.valueOf(ostopblock);
			dissimilarityMatrix = getDissimilarityMatrix(startBlock, stopBlock, collinearBlocks, maGraph);
		}
		else
			dissimilarityMatrix = MultipleAlignment.getDissimilarityMatrix(dataMap.keySet(), CollinearBlock.toGroupingEntries(collinearBlocks));
		
		if (options.has("matrixout")) {
			Writer bw = new BufferedWriter(new FileWriter((String) options.valueOf("matrixout")));
			List<String> queries = new ArrayList<>(dataMap.keySet());
			for (String q : queries)
				bw.write("\t" + q);
			bw.write("\n");
			for (String q1 : queries) {
				bw.write(q1);
				for (String q2 : queries)
					bw.write("\t" + dissimilarityMatrix.get(q1).get(q2));
				bw.write("\n");
			}
				
			bw.close();
		}
		if (options.has("treeout")) {
			Writer bw = new BufferedWriter(new FileWriter((String) options.valueOf("treeout")));
			bw.write(UPGMATreeConstruction.constructTree(dataMap.keySet(), dissimilarityMatrix).toNewickString(true) + "\n");
			bw.close();
		}
	}
}
