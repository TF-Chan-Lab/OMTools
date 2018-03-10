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


package aldenjava.opticalmapping.tools;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.multiplealignment.BlockInfo;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockOrder;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockReader;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockWriter;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class CBLTools {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(CBLTools.class.getSimpleName(), "Provides basic functions for filtering and processing multiple alignment results");
		OptMapDataReader.assignOptions(parser, 1);
		CollinearBlockReader.assignOptions(parser, 1);
		CollinearBlockWriter.assignOptions(parser, 1);

		parser.addHeader("CBL Tools Options", 1);
		parser.accepts("cboout", "Collinear block order output").withRequiredArg().ofType(String.class);
		parser.addHeader("Filtering Options", 2);
		OptionSpec<String> oallblock = parser.accepts("allblock", "Extract queries that contains all blocks in this list").withRequiredArg().ofType(String.class);
		// To be added
//		OptionSpec<String> oquery = parser.accepts("query", "Extract selected queries").withRequiredArg().ofType(String.class);
		OptionSpec<String> oreservequery = parser.accepts("reservequery", "Reserve the queries even they do not match other criteria").withRequiredArg().ofType(String.class);
		parser.addHeader("Sorting Options", 2);
		OptionSpec<String> osortblock = parser.accepts("sortblock", "Sort the queries by block existence").withRequiredArg().ofType(String.class);
		// To be added
//		OptionSpec<String> osortdeslen = parser.accepts("sortdeslen", "Sort the queries by block length in descending order").withRequiredArg().ofType(String.class);
		OptionSpec<String> osortdesflanklen = parser.accepts("sortdesflanklen", "Sort the queries by length between two flanking blocks (FB1a FB1b) in descending order").withRequiredArg().ofType(String.class);
		
		// Retype required to optional
		parser.addHeader(null, 0);
		parser.accepts("optmapin").withRequiredArg().ofType(String.class);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}		
		OptionSet options = parser.parse(args);
		LinkedHashMap<String, DataNode> dataMap = null;
		if (options.has("optmapin"))
			dataMap = OptMapDataReader.readAllData(options);
				
		LinkedHashMap<String, CollinearBlock> collinearBlocks = CollinearBlockReader.readAllData(options);
		List<String> queryNames = collinearBlocks.values().stream().map(b -> b.groups.keySet()).flatMap(g -> g.stream()).collect(Collectors.toCollection(LinkedHashSet::new)).stream().collect(Collectors.toList());
		
		// Filtering
		Set<String> reserveQueryNames = new HashSet<>(oreservequery.values(options));
		List<String> allBlocks = oallblock.values(options);
		if (!allBlocks.isEmpty()) {
			Set<String> retainEntries = null;
			for (String blockName : allBlocks)
				if (retainEntries == null)
					retainEntries = new HashSet<>(collinearBlocks.get(blockName).groups.keySet());
				else
					retainEntries.retainAll(collinearBlocks.get(blockName).groups.keySet());

			// Add the reserved queries
			retainEntries.addAll(reserveQueryNames);
			
			LinkedHashMap<String, CollinearBlock> newCollinearBlocks = new LinkedHashMap<>();
			for (Entry<String, CollinearBlock> e : collinearBlocks.entrySet()) {
				if (!Collections.disjoint(retainEntries, e.getValue().groups.keySet())) {
					e.getValue().groups.keySet().retainAll(retainEntries);
					newCollinearBlocks.put(e.getKey(), e.getValue());
					
				}
			}
			
			collinearBlocks = newCollinearBlocks;
			queryNames = collinearBlocks.values().stream().map(b -> b.groups.keySet()).flatMap(g -> g.stream()).collect(Collectors.toCollection(LinkedHashSet::new)).stream().collect(Collectors.toList());
		}

		// sorting
		LinkedHashMap<String, CollinearBlock> myCollinearBlocks = collinearBlocks;
		if (options.has(osortblock)) {
			List<String> sortBlockNames = osortblock.values(options);
			List<CollinearBlock> sortBlocks = sortBlockNames.stream().map(name -> myCollinearBlocks.get(name)).collect(Collectors.toList());
			Collections.sort(queryNames, (s1, s2) -> {
				for (CollinearBlock sortBlock : sortBlocks) {
					if (sortBlock.groups.containsKey(s1))
						if (!sortBlock.groups.containsKey(s2))
							return 1;
					if (sortBlock.groups.containsKey(s2))
						if (!sortBlock.groups.containsKey(s1))
							return -1;
				}
				return 0;
			});
		}
		
		
		if (options.has(osortdesflanklen)) {
			if (dataMap == null)
				throw new IllegalArgumentException("Data information is required to sort queries by flanking length. Please use the parameter --optmapin to state the data file.");
			List<String> flankingBlocks = options.valuesOf(osortdesflanklen);
			if (flankingBlocks.size() != 2) {
				throw new IllegalArgumentException("Flanking blocks must be in pair. You need to provide exactly 2 flanking blocks.");
			}
			String flank1 = flankingBlocks.get(0);
			String flank2 = flankingBlocks.get(1);
			boolean toCalculate = false;
			HashSet<String> set1 = new HashSet<>();
			HashSet<String> set2 = new HashSet<>();
			LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
			for (CollinearBlock block : collinearBlocks.values()) {
				if (block.name.equals(flank1)) {
					toCalculate = true;
					for (String name : block.groups.keySet()) 
						set1.add(name);
					continue;
				}
				if (block.name.equals(flank2)) {
					if (!toCalculate) {
						System.err.println(flank2 + " appears earlier than " + flank1 + ".");
					 	return;
					}
					toCalculate = false;
					for (String name : block.groups.keySet()) 
						set2.add(name);
					break;
				}
					
				if (toCalculate) {
					for (String name : block.groups.keySet()) {
						if (!counts.containsKey(name))
							counts.put(name, -1L); // Starting with -1 length, as flanking signals are not counted in length
						BlockInfo vp = block.groups.get(name);
						long len = Math.abs(dataMap.get(name).refp[vp.stopSig] - dataMap.get(name).refp[vp.startSig]);
						counts.put(name, counts.get(name) + len);
					}
				}
			}
//			counts.entrySet().stream().sorted((c1, c2) -> Long.compare(c1.getValue(), c2.getValue())).map(c -> c.getKey());
			
			int descending = -1;		
			Collections.sort(queryNames, (q1, q2) -> {
				if (counts.containsKey(q1) && counts.containsKey(q2))
					return Long.compare(counts.get(q1), counts.get(q2)) * descending;
				else
					if (!counts.containsKey(q1) && counts.containsKey(q2))
						return 1;
					else
						if (counts.containsKey(q1) && !counts.containsKey(q2))
							return -1;
						else
							return 0;
			});
			
		}
		
		CollinearBlockWriter.writeAll(options, collinearBlocks);
		if (options.has("cboout"))
			CollinearBlockOrder.writeAll(options, new CollinearBlockOrder(queryNames));
			
	}
}


//if (!sortBlocks.isEmpty()) {
//collinearBlocks = collinearBlocks.entrySet().stream().sorted(Entry.comparingByValue((CollinearBlock b1, CollinearBlock b2) -> {
//	for (String sortBlock : sortBlocks) {
//		if (b1.groups.containsKey(sortBlock))
//			if (!b2.groups.containsKey(sortBlock))
//				return 1;
//		if (b2.groups.containsKey(sortBlock))
//			if (!b1.groups.containsKey(sortBlock))
//				return -1;
//	}
//	return 0;
//})).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1,v2) -> {assert false; return v1;}, LinkedHashMap::new));
//
//
//}
