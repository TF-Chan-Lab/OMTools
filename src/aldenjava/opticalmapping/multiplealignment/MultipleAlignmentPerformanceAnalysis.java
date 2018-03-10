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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.MAFEntry;
import aldenjava.opticalmapping.data.MAFNode;
import aldenjava.opticalmapping.data.MAFReader;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class MultipleAlignmentPerformanceAnalysis {

	public static List<MAFEntry> getPath(EntryVertex vertex, long destination) {
		// Visited to be implemented
		
		if (vertex.entry.start + vertex.entry.length == destination) {
			List<MAFEntry> entries = new ArrayList<>();
			entries.add(vertex.entry);
			return entries;
		}
		else
			for (EntryVertex nextVertex : vertex.nextPaths) {
				List<MAFEntry> entries = getPath(nextVertex, destination);
				if (entries != null) {
					entries.add(vertex.entry);
					return entries;
				}
			}
		return null;
	}
	/**
	 * Processes the sequence blocks and every base pair in the multiple alignment must appear exactly once.  
	 * @param sequenceBlocks
	 * @param dataInfo
	 * @return a map with a genome as key
	 */
	public static LinkedHashMap<String, List<MAFEntry>> getSeqEntries(LinkedHashMap<String, MAFNode> sequenceBlocks, LinkedHashMap<String, DataNode> dataInfo) {
		LinkedHashMap<String, List<MAFEntry>> entriesMap = new LinkedHashMap<>();
		for (MAFNode maf : sequenceBlocks.values())
			for (MAFEntry entry : maf.entries.values()) {
				List<MAFEntry> list = entriesMap.get(entry.id);
				if (list == null) {
					list = new ArrayList<>();
					entriesMap.put(entry.id, list);
				}
				list.add(entry);
			}
		for (List<MAFEntry> entries : entriesMap.values()) 
			Collections.sort(entries, MAFEntry.startEndComparator);
		for (String key : entriesMap.keySet()) {

			List<MAFEntry> entries = entriesMap.get(key);
			List<EntryVertex> vertices = new ArrayList<>();
			for (MAFEntry entry : entries)
				vertices.add(new EntryVertex(entry));
			for (int i = 0; i < entries.size(); i++) {
				MAFEntry currentEntry = entries.get(i);
				for (int j = i - 1; j >= 0; j--) {
					MAFEntry previousEntry = entries.get(j);
					if (previousEntry.start + previousEntry.length == currentEntry.start) {
						vertices.get(j).nextPaths.add(vertices.get(i));
						vertices.get(i).prevPaths.add(vertices.get(j));
					}
				}
			}
			List<MAFEntry> filteredEntries = null; 			
			for (int i = 0; i < vertices.size(); i++) {
				if (entries.get(i).start == 1) {
					filteredEntries = getPath(vertices.get(i), dataInfo.get(entries.get(i).id).size + 1);
					if (filteredEntries != null) {
						Collections.reverse(filteredEntries); 
						break;
					}
						
				}
			}
			if (filteredEntries == null)
				System.err.println("Warning: Multiple alignment does not cover the whole genome " + key);

			for (MAFEntry entry : entries)
				if (!filteredEntries.contains(entry)) {
					sequenceBlocks.get(entry.label).entries.remove(entry.id);
					if (sequenceBlocks.get(entry.label).entries.size() == 0) {
						sequenceBlocks.remove(entry.label);
					}
				}
			entries.clear();
			entries.addAll(filteredEntries);
		}
		return entriesMap;
	}
	
	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(MultipleAlignmentPerformanceAnalysis.class.getSimpleName(), "Analyzes performance of multiple OM alignment based on the multiple sequence alignment.");
		OptMapDataReader.assignOptions(parser);
		MAFReader.assignOptions(parser, 1);
		CollinearBlockReader.assignOptions(parser, 1);
		parser.addHeader("multiple alignment options", 1);
		parser.accepts("statout", "Statistics output").withRequiredArg().ofType(String.class);
		
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		VerbosePrinter.println("Reading OM data...");
		LinkedHashMap<String, DataNode> dataInfo = OptMapDataReader.readAllData(options);
		VerbosePrinter.println("Reading multiple alignment based on sequence...");
		LinkedHashMap<String, MAFNode> sequenceBlocks = MAFReader.readAllData(options); // By ID
		VerbosePrinter.println("Reading multiple alignment based on optical mapping...");
		LinkedHashMap<String, CollinearBlock> collinearBlocks = CollinearBlockReader.readAllData(options);
		
		VerbosePrinter.println("Processing multiple alignment based on sequence...");
		LinkedHashMap<String, List<MAFEntry>> seqEntriesMap = getSeqEntries(sequenceBlocks, dataInfo);;
		// Read collinear blocks
		VerbosePrinter.println("Analyzing performance...");
		List<GroupingEntry> omEntries = CollinearBlock.toGroupingEntries(collinearBlocks);
		LinkedHashMap<String, MAPerformance> performances = new LinkedHashMap<>();
		for (String key : dataInfo.keySet())
			performances.put(key, new MAPerformance());
		for (GroupingEntry entry : omEntries)
			for (String id : entry.groups.keySet()) {
				SingleGroup g = entry.groups.get(id);
				DataNode data = dataInfo.get(id);
				int omOrientation = g.orientation;
				long omStart = data.refp[g.segment - 1] + 1;
				long omStop = data.refp[g.segment] - 1;
				if (omStop < omStart)
					continue; // 0 segment length, not counted.
				MAPerformance performance = performances.get(id);
				Set<String> omIDSet = new HashSet<>(entry.groups.keySet());				
				for (MAFEntry seqEntry : seqEntriesMap.get(id)) {
					long seqStart = seqEntry.start;
					long seqStop = seqEntry.start + seqEntry.length - 1;
					if (seqStart > omStop)
						continue;
					if (seqStop < omStart)
						continue;
					long max = seqStop > omStop ? omStop : seqStop;
					long min = seqStart < omStart ? omStart : seqStart;
					long overlapSize = max - min + 1;
					if (overlapSize >= 0) { 
						int matched = 0;
						Set<String> seqIDSet = sequenceBlocks.get(seqEntry.label).entries.keySet();
						int seqOrientation = seqEntry.orientation;
						for (String seqID : seqIDSet)
							if (omIDSet.contains(seqID)) {
								int omSameStrand = omOrientation * entry.groups.get(seqID).orientation;
								int seqSameStrand = seqOrientation * sequenceBlocks.get(seqEntry.label).entries.get(seqID).orientation;
								if (omSameStrand == seqSameStrand)
								matched++;
							}
						
						performance.addPerformance(matched, omIDSet.size(), seqIDSet.size(), overlapSize);
					}

				}
				
			}
		
		for (String id : seqEntriesMap.keySet())
			for (MAFEntry entry : seqEntriesMap.get(id))
				if (sequenceBlocks.get(entry.label).getMult() > 1)
					performances.get(id).addSharedSeqSize(entry.length);
				else
					performances.get(id).addNonSharedSeqSize(entry.length);
		for (GroupingEntry entry : omEntries)
			for (String id : entry.groups.keySet()) {
				SingleGroup g = entry.groups.get(id);
				DataNode data = dataInfo.get(id);	
				if (entry.groups.size() > 1)
					performances.get(id).addSharedOMSize(data.refp[g.segment] - 1 - data.refp[g.segment - 1] - 1);
				else
					performances.get(id).addNonSharedOMSize(data.refp[g.segment] - 1 - data.refp[g.segment - 1] - 1);
			}
		
		VerbosePrinter.println("Output results...");
		BufferedWriter bw = new BufferedWriter(new FileWriter((String) options.valueOf("statout")));
		for (String key : performances.keySet())
			bw.write(key + "\t" + performances.get(key).getSensitivity() + "\t" + performances.get(key).getSpecificity() + "\t" + performances.get(key).getSharedOMPortion() + "\t" + performances.get(key).getSharedSeqPortion() + "\n");
		bw.close();
	}

}

class EntryVertex {
	MAFEntry entry;
	List<EntryVertex> prevPaths = new ArrayList<>();
	List<EntryVertex> nextPaths = new ArrayList<>();
	public EntryVertex(MAFEntry entry) {
		this.entry = entry;
	}
	
}
class MAPerformance {
	private long totalSize = 0;
	private double totalSensitivity = 0;
	private double totalSpecificity = 0;
	private long sharedSeqSize = 0;
	private long totalSeqSize = 0;
	private long sharedOMSize = 0;
	private long totalOMSize = 0;
	public void addPerformance(int matched, int totalOMIDs, int totalSeqIDs, long size) {
		totalSensitivity += matched / (double) totalSeqIDs * size;
		totalSpecificity += matched / (double) totalOMIDs * size;
		totalSize += size;
	}
	public void addSharedSeqSize(long size) {
		sharedSeqSize += size;
		totalSeqSize += size;
	}
	public void addNonSharedSeqSize(long size) {
		totalSeqSize += size;
	}
	public double getSharedSeqPortion() {
		return sharedSeqSize / (double) totalSeqSize;
	}
	public void addSharedOMSize(long size) {
		sharedOMSize += size;
		totalOMSize += size;
	}
	public void addNonSharedOMSize(long size) {
		totalOMSize += size;
	}
	public double getSharedOMPortion() {
		return sharedOMSize / (double) totalOMSize;
	}
	public double getSensitivity() {
		return totalSensitivity / totalSize;
	}
	public double getSpecificity() {
		return totalSpecificity / totalSize;
	}
	
}