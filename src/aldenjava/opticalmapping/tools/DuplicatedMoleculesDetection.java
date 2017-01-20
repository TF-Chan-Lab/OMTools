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


package aldenjava.opticalmapping.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.mapper.seeding.MultiThreadSeedDatabase;
import aldenjava.opticalmapping.mapper.seeding.SeedDatabase;
import aldenjava.opticalmapping.mapper.seeding.SeedingResultNode;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

/**
 * Detects duplicated entries in an optical map data set. Duplicated molecules contain same number of total segment, and the difference between size of each segment is very small (< 100 bp). Based on this criteria, DuplicatedOpticalMapsDetection employs seeding to quickly locate all potentially duplicated optical maps.
 * 
 * @author Alden
 *
 */
public class DuplicatedMoleculesDetection {

	private int minseg;
	private OptionSet options;
	private int nextGroup = 1;

	public DuplicatedMoleculesDetection(OptionSet options) {
		this.minseg = (int) options.valueOf("minseg");
		this.options = options;
	}

	/**
	 * Checks and assigns groupID to duplicated kmers
	 * 
	 * @param assignedGroup
	 * @param groupElements
	 * @param targetKmer
	 * @param hitKmers
	 */
	private void process(LinkedHashMap<String, Integer> assignedGroup, LinkedHashMap<Integer, List<String>> groupElements, Kmer targetKmer, List<Kmer> hitKmers) {
		HashSet<Integer> involvedGroups = new HashSet<>();

		if (assignedGroup.containsKey(targetKmer.source))
			involvedGroups.add(assignedGroup.get(targetKmer.source));

		boolean found = false;
		for (Kmer hitKmer : hitKmers)
			if (hitKmer != targetKmer) {
				found = true;
				if (assignedGroup.containsKey(hitKmer.source))
					involvedGroups.add(assignedGroup.get(hitKmer.source));
			}
		if (found) {
			if (involvedGroups.size() == 0) {
				// create new group
				groupElements.put(nextGroup, new ArrayList<String>());
				involvedGroups.add(nextGroup);
				nextGroup++;
			}
			int targetGroup = Collections.min(involvedGroups);
			if (!assignedGroup.containsKey(targetKmer.source))
				groupElements.get(targetGroup).add(targetKmer.source);
			assignedGroup.put(targetKmer.source, targetGroup);
			for (Kmer hitKmer : hitKmers)
				if (hitKmer != targetKmer) {
					if (!assignedGroup.containsKey(hitKmer.source))
						groupElements.get(targetGroup).add(hitKmer.source);
					assignedGroup.put(hitKmer.source, targetGroup);
				}
			for (int group : involvedGroups)
				if (group != targetGroup)
					groupElements.get(targetGroup).addAll(groupElements.remove(group));
		}

	}

	/**
	 * Extracts duplications from optical maps with k segments
	 * 
	 * @param k Number of segments in each optical map
	 * @param dataMap Optical maps with exactly the same number of segments k 
	 * @return A map of duplicated optical maps with duplication groupID as key
	 */
	public LinkedHashMap<Integer, List<String>> extractDuplications(int k, LinkedHashMap<String, DataNode> dataMap) {
		assert k >= 0;
		if (k < minseg) {
			VerbosePrinter.println("Optical maps with " + k + " segments are skipped.");
			return new LinkedHashMap<>();
		}
		VerbosePrinter.println("Start checking duplications on optical maps with " + k + " segments...");
		List<Kmer> kmerList = new ArrayList<>();
		for (DataNode data : dataMap.values()) {
			assert k == data.getTotalSegment();
			kmerList.add(data.getKmer(k, 0));
		}

		SeedDatabase seedDatabase = new SeedDatabase(kmerList, k);
		seedDatabase.setMode(options);
		seedDatabase.setParameters(k);
		seedDatabase.buildDatabase();

		MultiThreadSeedDatabase mtsd = new MultiThreadSeedDatabase(seedDatabase);
		mtsd.setParameters(options);
		LinkedHashMap<String, Integer> assignedGroup = new LinkedHashMap<>();
		LinkedHashMap<Integer, List<String>> groupElements = new LinkedHashMap<>();
		try {
			for (Kmer kmer : kmerList) {
				while (!mtsd.startNext(kmer)) {
					SeedingResultNode seedingResultNode = mtsd.getNextResult();
					Kmer targetKmer = seedingResultNode.kmer;
					List<Kmer> hitKmers = seedingResultNode.kmerList;
					process(assignedGroup, groupElements, targetKmer, hitKmers);
				}
			}
			while (mtsd.getStatus() != -1) {
				SeedingResultNode seedingResultNode = mtsd.getNextResult();
				Kmer targetKmer = seedingResultNode.kmer;
				List<Kmer> hitKmers = seedingResultNode.kmerList;
				process(assignedGroup, groupElements, targetKmer, hitKmers);
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		mtsd.close();
		VerbosePrinter.println(groupElements.size() + " duplications are found.");
		return groupElements;
	}

	public int getTotalDuplications() {
		return nextGroup - 1;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		MultiThreadSeedDatabase.assignOptions(parser, level);
		SeedDatabase.assignOptions(parser, level);
		parser.addHeader("Duplicated Molecules Detecting Options", level);
		parser.accepts("dupout", "Files containing duplicated molecules").withRequiredArg().ofType(String.class).required();
		parser.accepts("minseg", "Minimum segments to be considered duplicated").withRequiredArg().ofType(Integer.class).defaultsTo(15);
		parser.addHeader(null, -1);
		parser.accepts("meas", "Measuring Errors. Usually it is much smaller than normal measuring errors in discovering duplicated molecules").withRequiredArg().ofType(Integer.class).defaultsTo(100);
		parser.accepts("ear", "Error acceptable range").withRequiredArg().ofType(Double.class).defaultsTo(0.0);

	}

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(DuplicatedMoleculesDetection.class.getSimpleName(), "Detects duplicated molecules in an optical map data set. Duplicated molecules contain same number of total segment, and the difference between size of each segment is very small (usually smaller than 100 bp)");
		DuplicatedMoleculesDetection.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);
		VerbosePrinter.println("Reading data...");
		LinkedHashMap<Integer, LinkedHashMap<String, DataNode>> labelMap = OptMapDataReader.getLabelMap(options);
		VerbosePrinter.println("Initializing...");
		DuplicatedMoleculesDetection dma = new DuplicatedMoleculesDetection(options);
		String output = (String) options.valueOf("dupout");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		bw.write("#Group\tMoleculeID\tSize\tTotalSegments\tSegmentInfo\n");
		for (int k : labelMap.keySet()) {
			LinkedHashMap<String, DataNode> dataMap = labelMap.get(k);
			LinkedHashMap<Integer, List<String>> groupElements = dma.extractDuplications(k, dataMap);
			for (int groupID : groupElements.keySet()) {
				for (String name : groupElements.get(groupID)) {
					DataNode data = dataMap.get(name);
					bw.write(String.format("%d\t%s\t%d\t%d\t%s\n", groupID, data.name, data.length(), data.getTotalSegment(), data.getReflString()));
				}
			}
			bw.flush();
		}
		bw.close();
		VerbosePrinter.println("");
		if (dma.getTotalDuplications() > 0)
			VerbosePrinter.println(dma.getTotalDuplications() + " duplications are found in total.");
		else
			VerbosePrinter.println("No duplication is found.");
		VerbosePrinter.println("Program ends.");
	}

}
