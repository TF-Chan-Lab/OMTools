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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.mutable.MutableInt;

import aldenjava.file.ListExtractor;
import aldenjava.opticalmapping.clustering.KmerCluster;
import aldenjava.opticalmapping.data.ClusteringFormat;
import aldenjava.opticalmapping.data.MultipleAlignmentFormat;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.SegmentIdentifier;
import aldenjava.opticalmapping.data.data.SegmentIdentifierFormat;
import aldenjava.opticalmapping.data.data.SegmentIdentifierReader;
import aldenjava.opticalmapping.data.mappingresult.MatchingSignalPair;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.ResultFormat;
import aldenjava.opticalmapping.mapper.MatchHelper;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.ProgressPrinter;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.phylogenetic.UPGMATreeConstruction;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class MultipleAlignment {
	
	
	
	// Process segment links
	
	/**
	 * Get segment links from alignment results
	 * 
	 * @param resultList
	 *            the alignment results
	 * @return segment links
	 */
	public Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> getSegmentLinksFromAlignment(List<OptMapResultNode> resultList, Map<String, DataNode> dataMap, Set<SegmentIdentifier> maskedSegments) {
		Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> segmentLinks = new LinkedHashMap<>();

		VerbosePrinter.println("Parsing alignment results for matching segments");
		ProgressPrinter progressParsingResults = new ProgressPrinter(resultList.size(), 10000L);
		for (OptMapResultNode result : resultList) {
			if (!result.isUsed())
				continue;
			String refName = result.mappedRegion.ref;
			String queryName = result.parentFrag.name;
			int orientation = result.mappedstrand;
			result.updateMSP();

			MatchingSignalPair lastMsp = null;
			for (MatchingSignalPair msp : result.mspsR) {
				if (lastMsp != null)
					if (lastMsp.rpos + 1 == msp.rpos && lastMsp.qpos + orientation == msp.qpos) {
						int querySegment = orientation == 1 ? msp.qpos : lastMsp.qpos;
						int refSegment = msp.rpos;
						SegmentIdentifier sir = new SegmentIdentifier(refName, refSegment);
						SegmentIdentifier siq = new SegmentIdentifier(queryName, querySegment);
						if (!maskedSegments.contains(sir) && !maskedSegments.contains(siq)) {							
							if (!segmentLinks.containsKey(sir))
								segmentLinks.put(sir, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
							if (!segmentLinks.get(sir).containsKey(siq))
								segmentLinks.get(sir).put(siq, new StrandSupport());
							segmentLinks.get(sir).get(siq).assignIfLargerSupport(orientation, result.mappedscore);
							if (!segmentLinks.containsKey(siq))
								segmentLinks.put(siq, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
							if (!segmentLinks.get(siq).containsKey(sir))
								segmentLinks.get(siq).put(sir, new StrandSupport());
							segmentLinks.get(siq).get(sir).assignIfLargerSupport(orientation, result.mappedscore);
						}
					}
				lastMsp = msp;
			}
			progressParsingResults.update();
		}
		return segmentLinks;
	}

	/**
	 * Get segment links from multiple alignment results
	 * 
	 * @param blockList
	 *            the multiple alignment results
	 * @return segment links
	 */
	public Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> getSegmentLinksFromMultipleAlignment(LinkedHashMap<String, CollinearBlock> collinearBlocks) {
		Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> segmentLinks = new LinkedHashMap<>();

		VerbosePrinter.println("Parsing multiple alignment for matching segments");
		List<GroupingEntry> entries = CollinearBlock.toGroupingEntries(collinearBlocks);
		ProgressPrinter progressParsingResults = new ProgressPrinter(entries.size(), 10000L);
		for (GroupingEntry entry : entries) {
			for (String key1 : entry.groups.keySet()) {
				SingleGroup g1 = entry.groups.get(key1);
				SegmentIdentifier si1 = new SegmentIdentifier(key1, g1.segment);
				for (String key2 : entry.groups.keySet()) {
					if (key1.equals(key2))
						continue;
					SingleGroup g2 = entry.groups.get(key2);
					SegmentIdentifier si2 = new SegmentIdentifier(key2, g2.segment);
					if (!segmentLinks.containsKey(si1))
						segmentLinks.put(si1, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
					if (!segmentLinks.get(si1).containsKey(si2))
						segmentLinks.get(si1).put(si2, new StrandSupport());
					segmentLinks.get(si1).get(si2).addSupport(g1.orientation * g2.orientation, 1);
					if (!segmentLinks.containsKey(si2))
						segmentLinks.put(si2, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
					if (!segmentLinks.get(si2).containsKey(si1))
						segmentLinks.get(si2).put(si1, new StrandSupport());
					segmentLinks.get(si2).get(si1).addSupport(g1.orientation * g2.orientation, 1);
				}
			}
			progressParsingResults.update();
		}
		return segmentLinks;
	}

	/**
	 * Get segment links from clustering results
	 * 
	 * @param clusters
	 *            the clustering results
	 * @return segment links
	 */
	public Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> getSegmentLinksFromClustering(LinkedHashMap<Integer, KmerCluster> clusters) {
		Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> segmentLinks = new LinkedHashMap<>();

		VerbosePrinter.println("Parsing clustering results for matching segments");
		ProgressPrinter progressParsingResults = new ProgressPrinter(clusters.size(), 10000L);
		CLUSTERLOOP: for (KmerCluster cluster : clusters.values()) {
			LinkedHashSet<String> set = new LinkedHashSet<>();
			for (Kmer kmer : cluster.kmerList)
				if (!set.add(kmer.source))
					continue CLUSTERLOOP;
			for (int i = 0; i < cluster.getCount(); i++) {
				Kmer k1 = cluster.kmerList.get(i);
				// checking
				for (int j = i + 1; j < cluster.getCount(); j++) {
					Kmer k2 = cluster.kmerList.get(j);
					int orientation = 1;
					for (int index = 0; index < k1.k(); index++) {

						// Need to modify code to handle strand
						SegmentIdentifier si1 = new SegmentIdentifier(k1.source, index + k1.pos);
						SegmentIdentifier si2 = new SegmentIdentifier(k2.source, index + k2.pos);
						if (!segmentLinks.containsKey(si1))
							segmentLinks.put(si1, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
						if (!segmentLinks.get(si1).containsKey(si2))
							segmentLinks.get(si1).put(si2, new StrandSupport());
						segmentLinks.get(si1).get(si2).addSupport(orientation, 1);
						if (!segmentLinks.containsKey(si2))
							segmentLinks.put(si2, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
						if (!segmentLinks.get(si2).containsKey(si1))
							segmentLinks.get(si2).put(si1, new StrandSupport());
						segmentLinks.get(si2).get(si1).addSupport(orientation, 1);
					}
				}
			}
			progressParsingResults.update();
		}
		return segmentLinks;
	}
	
	/**
	 * Merges multiple segmentLinks into one segmentLinks
	 * @param segmentLinksList
	 * @return the combined segmentLinks 
	 */
	public Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> combineSegmentLinks(List<Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>>> segmentLinksList){
		Map<SegmentIdentifier, Map<SegmentIdentifier, StrandSupport>> combinedSegmentLinks = new LinkedHashMap<>();
		for (Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> segmentLinks : segmentLinksList) {
			for (SegmentIdentifier si1 : segmentLinks.keySet()) {
				Map<SegmentIdentifier, StrandSupport> map = segmentLinks.get(si1);
				if (!combinedSegmentLinks.containsKey(si1))
					combinedSegmentLinks.put(si1, new LinkedHashMap<SegmentIdentifier, StrandSupport>());
				for (SegmentIdentifier si2 : map.keySet()) {
					if (!combinedSegmentLinks.get(si1).containsKey(si2))
						combinedSegmentLinks.get(si1).put(si2, new StrandSupport());
					combinedSegmentLinks.get(si1).get(si2).addSupport(map.get(si2));
				}
			}
		}
		return combinedSegmentLinks;
	}

	
	// Build
	
	public void updatePreCollinearBlocks(PreCollinearBlocks preCollinearBlocks, Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> segmentLinks,
			Set<SegmentIdentifier> identifiersToProcess) {

		VerbosePrinter.println("Combining segment links...");
		ProgressPrinter pp = new ProgressPrinter(segmentLinks.size(), 10000L);
		LinkedHashSet<SegmentIdentifier> usedSI = new LinkedHashSet<>();
		// Identify all connected segment identifiers
		for (SegmentIdentifier si1 : segmentLinks.keySet()) {
			pp.update();
			if (usedSI.contains(si1))
				continue;
			if (identifiersToProcess != null && !identifiersToProcess.contains(si1))
				continue;

			boolean ambiguous = false;
			LinkedHashMap<SegmentIdentifier, Integer> map = new LinkedHashMap<>();

			// Initialize the list with the target element
			map.put(si1, 1);
			LinkedList<SegmentIdentifier> list = new LinkedList<>();
			list.add(si1);

			// Recur until all linked SegmentIdentifiers are identified
			while (!list.isEmpty()) {
				SegmentIdentifier si = list.removeFirst();
				int strand = map.get(si);
				Map<SegmentIdentifier, StrandSupport> m = segmentLinks.get(si);
				for (SegmentIdentifier nextSI : m.keySet()) {
					if (identifiersToProcess != null && !identifiersToProcess.contains(nextSI))
						continue;
					StrandSupport ss = m.get(nextSI);
					// Check if there exists more than one orientation support
					if (ss.forwardSupport >= 1 && ss.reverseSupport >= 1)
						ambiguous = true;
					if (map.containsKey(nextSI)) {
						// Check strand mismatching
						if (map.get(nextSI) != strand * ss.getBestStrand())
							ambiguous = true;
					} else {
						map.put(nextSI, strand * ss.getBestStrand());
						list.addLast(nextSI);
					}
				}
			}

			LinkedHashSet<String> nameSet = new LinkedHashSet<>();
			for (SegmentIdentifier si : map.keySet())
				if (!nameSet.add(si.name)) {
					ambiguous = true;
					break;
				}

			// If no ambiguous case occurs, add the SegmentIdentifiers to a singleton
			if (!ambiguous) {
				LinkedHashMap<String, SingleGroup> singleGroups = new LinkedHashMap<>();
				// filtering
//				HashSet<SegmentIdentifier> toBeRemoved = new HashSet<>();
//				for (SegmentIdentifier querySI : map.keySet()) {
//					int match = 0;
//					for (SegmentIdentifier targetSI : segmentLinks.get(querySI).keySet())
//						if (map.keySet().contains(targetSI))
//							match++;
//					if (match < 3)
//						toBeRemoved.add(querySI);
//				}
//				for (SegmentIdentifier si : map.keySet())
//					if (!toBeRemoved.contains(si))
//						singleGroups.put(si.name, new SingleGroup(si.segment, map.get(si)));				
				for (SegmentIdentifier si : map.keySet())
					singleGroups.put(si.name, new SingleGroup(si.segment, map.get(si)));
				preCollinearBlocks.addNewRelations(singleGroups);
			}

			// No matter ambiguous or not, we remove all of them
			for (SegmentIdentifier si : map.keySet())
				usedSI.add(si);
		}
	}
	private List<String> collinearBlockNJLog(Set<GroupingEntryChain> chains, PriorityQueue<EntryConnection> connections) {
		List<String> linesToWrite = new ArrayList<>();
		linesToWrite.add("digraph g {");
		Set<Integer> usedID = new LinkedHashSet<>();
		for (EntryConnection connection : connections) {
			if (connection.entryMatch < 5)
				 continue;
			if (!chains.contains(connection.baseEntryChain))
				continue;
			if (!chains.contains(connection.targetEntryChain))
				continue;
			usedID.add(connection.baseEntryChain.id);
			usedID.add(connection.targetEntryChain.id);
			
		}
		for (int id : usedID) {
			
			linesToWrite.add("\"" + id + "B\" -> \"" + id + "F\" [label=" + id + ", penwidth=" + 5 + ", weight=" + 5 + ", color=black" + "]");
		}
		for (EntryConnection connection : connections) {
			if (connection.entryMatch < 5)
				 continue;
			if (!chains.contains(connection.baseEntryChain))
				continue;
			if (!chains.contains(connection.targetEntryChain))
				continue;
			if (connection.entryMatch > 0)
				linesToWrite.add("\"" + connection.baseEntryChain.id + (connection.baseReverse==1?"F":"B") + "\" -> \"" + connection.targetEntryChain.id + (connection.targetReverse==1?"B":"F") + "\" [label=" + connection.entryMatch + ", penwidth=" + (connection.entryMatch>10?10:connection.entryMatch)/2. + ", weight=" + (connection.entryMatch>10?10:connection.entryMatch)/2. + ", color=blue" + "]");
			if (connection.entryRearrangement > 0)
				linesToWrite.add("\"" + connection.baseEntryChain.id + (connection.baseReverse==1?"F":"B") + "\" -> \"" + connection.targetEntryChain.id + (connection.targetReverse==1?"B":"F") + "\" [label=" + connection.entryRearrangement + ", penwidth=" + (connection.entryRearrangement>10?10:connection.entryRearrangement)/2. + ", weight=" + (connection.entryRearrangement>10?10:connection.entryRearrangement)/2. + ", color=red" + "]");
		}
		linesToWrite.add("}");
		return linesToWrite;
	}
	private List<String> collinearBlockNJLog(Set<GroupingEntryChain> chains) {
		int nextID = chains.stream().mapToInt(chain -> chain.id).max().getAsInt();
		boolean allowRearragement = true;
		// copy		

		List<GroupingEntryChain> chainList = new ArrayList<>(chains);
		LinkedHashMap<String, LinkedHashMap<Integer, GroupingEntryChain>> segmentGroup = new LinkedHashMap<>();
		for (GroupingEntryChain chain : chainList) {
			LinkedHashMap<String, Set<Integer>> segmentsFacingOutward = chain.getSegmentsFacingOutward();
			for (String key : segmentsFacingOutward.keySet()) {
				LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
				if (map == null) {
					map = new LinkedHashMap<>();
					segmentGroup.put(key, map);
				}
				for (Integer segment : segmentsFacingOutward.get(key))
					map.put(segment, chain);
			}
		}

		VerbosePrinter.println("Building initial connections...");
		ProgressPrinter connectionBuildingProgress = new ProgressPrinter(null, 10000L);
		PriorityQueue<EntryConnection> connections = new PriorityQueue<>(Collections.reverseOrder());
		for (int i = 0; i < chainList.size(); i++) {
			connectionBuildingProgress.update();
			GroupingEntryChain currentChain = chainList.get(i);
			LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = currentChain.getPotentialMatchingSegments();
			Set<GroupingEntryChain> potentialChains = new LinkedHashSet<GroupingEntryChain>();
			for (String key : potentialMatchingSegments.keySet()) {
				Set<Integer> segmentSet = potentialMatchingSegments.get(key);
				LinkedHashMap<Integer, GroupingEntryChain> linkedChains = segmentGroup.get(key);
				for (Integer segment : segmentSet) {
					GroupingEntryChain linkedChain = linkedChains.get(segment);
					if (linkedChain != null)
						potentialChains.add(linkedChain);
				}
			}
			for (GroupingEntryChain chain : potentialChains) {
				// if (chainList.indexOf(chain) < i)
				// continue;
				if (chain == currentChain)
					continue;
				EntryConnection connection1 = chain.getEntryConnection(currentChain, true, new ArrayList<>());
				if (!connection1.hasEntryMatch())
					continue;
				if (!allowRearragement && (connection1.hasRearrangement()))
					continue;
				connections.add(connection1);
				// connections.add(chain.getEntryConnection(currentChain, true, marefs));
				// connections.add(currentChain.getEntryConnection(chain, true, marefs));
			}
		}
		connectionBuildingProgress.printProgress();
		VerbosePrinter.println("Connecting chains...");
		ProgressPrinter connectionProgress = new ProgressPrinter(chains.size(), 10000L);
		ProgressPrinter connectionProgress2 = new ProgressPrinter(null, 10000L);
		while (chains.size() > 1 && connections.size() > 0) {
			// EntryConnection connection = connections.pollLast();
			// if (count % 1000 == 0) {
			// tc.outputtime();
			// GroupingEntryChain.tc.outputtime();
			// }
			EntryConnection connection = connections.poll();
			if (connection.hasRearrangement())
				break;
			if (connection.entryDirectMatch != connection.entryMatch)
				break;
			// Check if the connection still exists
			connectionProgress2.update("Remaining connections: " + connections.size());
			if (chains.contains(connection.baseEntryChain) && chains.contains(connection.targetEntryChain)) {
				connectionProgress.update();
//				if (connection.hasRearrangement())
//					System.out.println("REARRANGEMENT!");
				// Remove old chains
				// System.out.println(connection.baseEntryChain.getFirstEntry().name + "\t" + connection.targetEntryChain.getFirstEntry().name);
				chains.remove(connection.baseEntryChain);
				chains.remove(connection.targetEntryChain);
				// Remove segmentGroup of the old chains
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardBase = connection.baseEntryChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardBase.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					for (Integer segment : segmentsFacingOutwardBase.get(key))
						map.remove(segment);
				}
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardTarget = connection.targetEntryChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardTarget.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					for (Integer segment : segmentsFacingOutwardTarget.get(key))
						map.remove(segment);
				}

				// Create new chain
				// newChain = new GroupingEntryChain(nextID++, connection.baseReverse == 1 ? connection.baseEntryChain:connection.baseEntryChain.getReverse());
				// newChain.addChain(connection.targetReverse == 1 ? connection.targetEntryChain : connection.targetEntryChain.getReverse(), connection.insertionIndex);
				GroupingEntryChain newChain = GroupingEntryChain.joinChain(nextID++, connection);

				// if (connection.incomingReverse == 1)
				// newChain = new GroupingEntryChain(nextID++, connection.incomingEntryChain);
				// else
				// newChain = new GroupingEntryChain(nextID++, connection.incomingEntryChain.getReverse());
				// if (connection.outgoingReverse == 1)
				// newChain.addNextEntry(connection.outgoingEntryChain);
				// else
				// newChain.addNextEntry(connection.outgoingEntryChain.getReverse());
				chains.add(newChain);
				// Assign segmentGroup to the new chain
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardNew = newChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardNew.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					if (map == null) {
						map = new LinkedHashMap<>();
						segmentGroup.put(key, map);
					}
					for (Integer segment : segmentsFacingOutwardNew.get(key))
						map.put(segment, newChain);
				}

				// assert segmentGroup.values().stream().allMatch(segMap -> segMap.values().stream().allMatch(chain -> chains.contains(chain))) : "Incorrect assignment of segmentGroup, segmentGroup points to a chain not present in current chains.";

				// Update chain connection
				// if (connections.size() > initialConnectionNumber * 2) {
				// TreeSet<EntryConnection> newConnections = new TreeSet<>();
				// Iterator<EntryConnection> iter = connections.iterator();
				// while (iter.hasNext()) {
				// EntryConnection c = iter.next();
				// if ((chains.contains(c.incomingEntryChain) && chains.contains(c.outgoingEntryChain)))
				// newConnections.add(c);
				// }
				// connections = newConnections;
				// }
				// Iterator<EntryConnection> iter = connections.iterator();
				// while (iter.hasNext()) {
				// EntryConnection c = iter.next();
				// if (!(chains.contains(c.incomingEntryChain) && chains.contains(c.outgoingEntryChain)))
				// iter.remove();
				// }
				LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = newChain.getPotentialMatchingSegments();
				Set<GroupingEntryChain> potentialChains = new LinkedHashSet<GroupingEntryChain>();
				for (String key : potentialMatchingSegments.keySet()) {
					Set<Integer> segmentSet = potentialMatchingSegments.get(key);
					LinkedHashMap<Integer, GroupingEntryChain> linkedChains = segmentGroup.get(key);
					for (Integer segment : segmentSet) {
						GroupingEntryChain linkedChain = linkedChains.get(segment);
						if (linkedChain != null)
							potentialChains.add(linkedChain);
					}
				}
				// assert chains.containsAll(potentialChains) : "Some potentialChains are not present in chains.";

				for (GroupingEntryChain chain : potentialChains)
					if (chain != newChain) {
						EntryConnection connection1 = chain.getEntryConnection(newChain, true, new ArrayList<>());
						// Filter those without entry match
						if (!connection1.hasEntryMatch())
							continue;

						if (!allowRearragement && (connection1.hasRearrangement()))
							continue;

						connections.add(connection1);

					}
			}
		}
		connectionProgress.printProgress();

		return collinearBlockNJLog(chains, connections);
	}
	
	public void layoutCollinearBlocksNJ(Set<GroupingEntryChain> chains, List<String> marefs, boolean allowRearragement) {
		int nextID = chains.stream().max(Comparator.<GroupingEntryChain> comparingInt(e -> e.id)).get().id + 1;

		List<GroupingEntryChain> chainList = new ArrayList<>(chains);
		LinkedHashMap<String, LinkedHashMap<Integer, GroupingEntryChain>> segmentGroup = new LinkedHashMap<>();
		for (GroupingEntryChain chain : chainList) {
			LinkedHashMap<String, Set<Integer>> segmentsFacingOutward = chain.getSegmentsFacingOutward();
			for (String key : segmentsFacingOutward.keySet()) {
				LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
				if (map == null) {
					map = new LinkedHashMap<>();
					segmentGroup.put(key, map);
				}
				for (Integer segment : segmentsFacingOutward.get(key))
					map.put(segment, chain);
			}
		}

		VerbosePrinter.println("Building initial connections...");
		ProgressPrinter connectionBuildingProgress = new ProgressPrinter(null, 10000L);
		PriorityQueue<EntryConnection> connections = new PriorityQueue<>(Collections.reverseOrder());
		for (int i = 0; i < chainList.size(); i++) {
			connectionBuildingProgress.update();
			GroupingEntryChain currentChain = chainList.get(i);
			LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = currentChain.getPotentialMatchingSegments();
			Set<GroupingEntryChain> potentialChains = new LinkedHashSet<GroupingEntryChain>();
			for (String key : potentialMatchingSegments.keySet()) {
				Set<Integer> segmentSet = potentialMatchingSegments.get(key);
				LinkedHashMap<Integer, GroupingEntryChain> linkedChains = segmentGroup.get(key);
				for (Integer segment : segmentSet) {
					GroupingEntryChain linkedChain = linkedChains.get(segment);
					if (linkedChain != null)
						potentialChains.add(linkedChain);
				}
			}
			for (GroupingEntryChain chain : potentialChains) {
				// if (chainList.indexOf(chain) < i)
				// continue;
				if (chain == currentChain)
					continue;
				// EntryConnection connection1 = chain.getEntryConnection(currentChain, true, marefs);
				// EntryConnection connection2 = currentChain.getEntryConnection(chain, true, marefs);
				// if (!allowRearragement && (connection1.hasRearrangement() || connection2.hasRearrangement()))
				// connections.add(connection1);
				// connections.add(connection2);
				EntryConnection connection1 = chain.getEntryConnection(currentChain, true, marefs);
				if (!connection1.hasEntryMatch())
					continue;
				if (!allowRearragement && (connection1.hasRearrangement()))
					continue;
				connections.add(connection1);
				// connections.add(chain.getEntryConnection(currentChain, true, marefs));
				// connections.add(currentChain.getEntryConnection(chain, true, marefs));
			}
		}
		connectionBuildingProgress.printProgress();

		VerbosePrinter.println("Connecting chains...");
		ProgressPrinter connectionProgress = new ProgressPrinter(chains.size(), 10000L);
		ProgressPrinter connectionProgress2 = new ProgressPrinter(null, 10000L);
		while (chains.size() > 1 && connections.size() > 0) {
			// EntryConnection connection = connections.pollLast();
			// if (count % 1000 == 0) {
			// tc.outputtime();
			// GroupingEntryChain.tc.outputtime();
			// }
			EntryConnection connection = connections.poll();
			// Check if the connection still exists
			connectionProgress2.update("Remaining connections: " + connections.size());
			if (chains.contains(connection.baseEntryChain) && chains.contains(connection.targetEntryChain)) {
				connectionProgress.update();
//				if (connection.hasRearrangement())
//					System.out.println("REARRANGEMENT!");
				// Remove old chains
				// System.out.println(connection.baseEntryChain.getFirstEntry().name + "\t" + connection.targetEntryChain.getFirstEntry().name);
				chains.remove(connection.baseEntryChain);
				chains.remove(connection.targetEntryChain);
				// Remove segmentGroup of the old chains
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardBase = connection.baseEntryChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardBase.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					for (Integer segment : segmentsFacingOutwardBase.get(key))
						map.remove(segment);
				}
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardTarget = connection.targetEntryChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardTarget.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					for (Integer segment : segmentsFacingOutwardTarget.get(key))
						map.remove(segment);
				}

				// Create new chain
				// newChain = new GroupingEntryChain(nextID++, connection.baseReverse == 1 ? connection.baseEntryChain:connection.baseEntryChain.getReverse());
				// newChain.addChain(connection.targetReverse == 1 ? connection.targetEntryChain : connection.targetEntryChain.getReverse(), connection.insertionIndex);
				GroupingEntryChain newChain = GroupingEntryChain.joinChain(nextID++, connection);

				// if (connection.incomingReverse == 1)
				// newChain = new GroupingEntryChain(nextID++, connection.incomingEntryChain);
				// else
				// newChain = new GroupingEntryChain(nextID++, connection.incomingEntryChain.getReverse());
				// if (connection.outgoingReverse == 1)
				// newChain.addNextEntry(connection.outgoingEntryChain);
				// else
				// newChain.addNextEntry(connection.outgoingEntryChain.getReverse());
				chains.add(newChain);
				// Assign segmentGroup to the new chain
				LinkedHashMap<String, Set<Integer>> segmentsFacingOutwardNew = newChain.getSegmentsFacingOutward();
				for (String key : segmentsFacingOutwardNew.keySet()) {
					LinkedHashMap<Integer, GroupingEntryChain> map = segmentGroup.get(key);
					if (map == null) {
						map = new LinkedHashMap<>();
						segmentGroup.put(key, map);
					}
					for (Integer segment : segmentsFacingOutwardNew.get(key))
						map.put(segment, newChain);
				}

				// assert segmentGroup.values().stream().allMatch(segMap -> segMap.values().stream().allMatch(chain -> chains.contains(chain))) : "Incorrect assignment of segmentGroup, segmentGroup points to a chain not present in current chains.";

				// Update chain connection
				// if (connections.size() > initialConnectionNumber * 2) {
				// TreeSet<EntryConnection> newConnections = new TreeSet<>();
				// Iterator<EntryConnection> iter = connections.iterator();
				// while (iter.hasNext()) {
				// EntryConnection c = iter.next();
				// if ((chains.contains(c.incomingEntryChain) && chains.contains(c.outgoingEntryChain)))
				// newConnections.add(c);
				// }
				// connections = newConnections;
				// }
				// Iterator<EntryConnection> iter = connections.iterator();
				// while (iter.hasNext()) {
				// EntryConnection c = iter.next();
				// if (!(chains.contains(c.incomingEntryChain) && chains.contains(c.outgoingEntryChain)))
				// iter.remove();
				// }
				LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = newChain.getPotentialMatchingSegments();
				Set<GroupingEntryChain> potentialChains = new LinkedHashSet<GroupingEntryChain>();
				for (String key : potentialMatchingSegments.keySet()) {
					Set<Integer> segmentSet = potentialMatchingSegments.get(key);
					LinkedHashMap<Integer, GroupingEntryChain> linkedChains = segmentGroup.get(key);
					for (Integer segment : segmentSet) {
						GroupingEntryChain linkedChain = linkedChains.get(segment);
						if (linkedChain != null)
							potentialChains.add(linkedChain);
					}
				}
				// assert chains.containsAll(potentialChains) : "Some potentialChains are not present in chains.";

				for (GroupingEntryChain chain : potentialChains)
					if (chain != newChain) {
						// {
						// tc.start(5);
						// EntryConnection connection1 = chain.getEntryConnection(newChain, true, marefs);
						// EntryConnection connection2 = newChain.getEntryConnection(chain, true, marefs);
						// tc.stop(5);
						// if (!connection1.hasEntryMatch())
						// continue;
						// if (!allowRearragement && (connection1.hasRearrangement() || connection2.hasRearrangement()))
						// continue;
						// tc.start(6);
						// connections.add(connection1);
						// connections.add(connection2);
						// tc.stop(6);

						// if (connection1.compareTo(connection2) != 0) {
						// System.err.println(connection1.entryMatch);
						// System.err.println(connection1.entryDirectMatch);
						// System.err.println(connection1.entryRearrangement);
						// System.err.println(connection2.entryMatch);
						// System.err.println(connection2.entryDirectMatch);
						// System.err.println(connection2.entryRearrangement);
						// System.exit(0);
						// }
						// }
						EntryConnection connection1 = chain.getEntryConnection(newChain, true, marefs);
						// Filter those without entry match
						if (!connection1.hasEntryMatch())
							continue;

						if (!allowRearragement && (connection1.hasRearrangement()))
							continue;

						connections.add(connection1);

						// connections.add(chain.getEntryConnection(newChain, true, marefs));
						// connections.add(newChain.getEntryConnection(chain, true, marefs));
					}
			}
		}
		connectionProgress.printProgress();
		VerbosePrinter.println(chains.size() + " chains remain after joining.");
	}
	public Set<GroupingEntryChain> layoutCollinearBlocksNJ(List<GroupingEntry> entries, List<String> marefs, boolean allowRearragement) {
		VerbosePrinter.println("Assigning each entry into individual chain...");
		Set<GroupingEntryChain> chains = new LinkedHashSet<>();
		int nextID = 1;
		for (GroupingEntry entry : entries)
			chains.add(new GroupingEntryChain(nextID++, entry));
		this.layoutCollinearBlocksNJ(chains, marefs, allowRearragement);
		return chains;
	}
	/**
	 * Order the grouping entries to maximize number of matching and minimize number of rearrangements, using a nearest neighbour joining approach
	 * 
	 * @param entries
	 *            A list of grouping entries to be ordered
	 * @param marefs
	 *            A list of queries taken as references (Their rearrangements are minimized prior to other queries)
	 * @return A list of ordered grouping entries
	 */
	public Set<GroupingEntryChain> layoutCollinearBlocksNJ(List<GroupingEntry> entries, List<String> marefs) {
		return layoutCollinearBlocksNJ(entries, marefs, true);
	}

	
	
	// Merge
	
	
	
	/**
	 * Merge proximate blocks if the difference in size of blocks passes the error tolerance threshold 
	 * @param entries
	 * @param dataMap
	 * @param meas
	 * @param ear
	 * @return
	 */
	public List<GroupingEntry> mergeByProximitySimple(List<GroupingEntry> entries, Map<String, DataNode> dataMap, int meas, double ear) {
		VerbosePrinter.println("Establishing the entry map...");
		LinkedHashMap<String, GroupingEntry> entryMap = new LinkedHashMap<>();
		for (GroupingEntry entry : entries)
			entryMap.put(entry.name, entry);
		VerbosePrinter.println("Building a directed graph...");
		LinkedHashMap<String, String> lastAppeared = new LinkedHashMap<>();
		LinkedHashMap<String, Set<String>> forwardDirectedGraph = new LinkedHashMap<>();
		LinkedHashMap<String, Set<String>> reverseDirectedGraph = new LinkedHashMap<>();
		for (int i = 0; i < entries.size(); i++) {
			forwardDirectedGraph.put(entries.get(i).name, new LinkedHashSet<String>());
			reverseDirectedGraph.put(entries.get(i).name, new LinkedHashSet<String>());
		}
		for (int i = 0; i < entries.size(); i++) {
			GroupingEntry entry = entries.get(i);
			for (String name : entry.groups.keySet()) {
				if (lastAppeared.containsKey(name)) {
					// Check rearrangement later
					String previousEntryName = lastAppeared.get(name);
					forwardDirectedGraph.get(previousEntryName).add(entry.name);
					reverseDirectedGraph.get(entry.name).add(previousEntryName);
				}
				lastAppeared.put(name, entry.name);
			}
		}
		LinkedHashMap<String, Integer> entryIndex = new LinkedHashMap<>();
		for (int i = 0; i < entries.size(); i++)
			entryIndex.put(entries.get(i).name, i);

		List<GroupingEntry> newEntries = new ArrayList<>();
		LinkedHashSet<String> entriesRemoved = new LinkedHashSet<>();
		LinkedHashMap<String, GroupingEntry> replacedEntries = new LinkedHashMap<>();
		for (GroupingEntry entry1 : entries) {
			while (replacedEntries.containsKey(entry1.name))
				entry1 = replacedEntries.get(entry1.name);
			if (entriesRemoved.contains(entry1.name))
				continue;
			newEntries.add(entry1);
			entriesRemoved.add(entry1.name);

			String group1name = entry1.name;

			List<CurrentEntry> group2Entries = new ArrayList<>();
			// Obtain
			for (String group2name : forwardDirectedGraph.get(group1name)) {
				if (reverseDirectedGraph.get(group2name).size() > 1)
					continue;
				GroupingEntry entry2 = entryMap.get(group2name);
				group2Entries.add(new CurrentEntry(group2name, entry2.getAverageSize(dataMap), entry2.groups.size()));
			}
			Collections.sort(group2Entries, Collections.reverseOrder(CurrentEntry.queryNoComparator));

			LinkedHashSet<Integer> usedIndex = new LinkedHashSet<>();
			for (int i = 0; i < group2Entries.size(); i++) {
				if (usedIndex.contains(i))
					continue;
				GroupingEntry e1 = entryMap.get(group2Entries.get(i).name);
				for (int j = i + 1; j < group2Entries.size(); j++) {
					if (usedIndex.contains(j))
						continue;
					GroupingEntry e2 = entryMap.get(group2Entries.get(j).name);
					if (MatchHelper.fuzzyMatch(group2Entries.get(i).size, group2Entries.get(j).size, meas, ear)) {
						if (Collections.disjoint(e1.groups.keySet(), e2.groups.keySet())) {
							e1.groups.putAll(e2.groups);
							replacedEntries.put(e2.name, e1);
							usedIndex.add(j);
							forwardDirectedGraph.get(e1.name).addAll(forwardDirectedGraph.get(e2.name));
							for (String nextEntryName : forwardDirectedGraph.get(e2.name)) {
								reverseDirectedGraph.get(nextEntryName).remove(e2.name);
								reverseDirectedGraph.get(nextEntryName).add(e1.name);
							}
							forwardDirectedGraph.get(e2.name).remove(e2.name);
						}
					}
				}
			}

		}
		return newEntries;
	}
	/**
	 * Merge proximate blocks if the difference in size of blocks passes the error tolerance threshold. This one employs the directed graph maMergeGraph.
	 */
	public void mergeByProximity(MAMergeGraph maMergeGraph, Map<String, DataNode> dataMap, int meas, double ear) {
		Map<String, GroupingEntry> groupingEntryMap = maMergeGraph.groupingEntryMap;
		
		Map<String, Set<String>> dependentPrevBlocks = maMergeGraph.dependentPrevBlocks;
		Map<String, Set<String>> dependentNextBlocks = maMergeGraph.dependentNextBlocks;

		Map<String, Set<String>> prevBlocks = maMergeGraph.prevBlocks;
		Map<String, Set<String>> nextBlocks = maMergeGraph.nextBlocks;

		Set<String> blocksToCheck = new LinkedHashSet<>(groupingEntryMap.keySet());
		
		while (!blocksToCheck.isEmpty()) {
			String group1 = blocksToCheck.iterator().next();
			blocksToCheck.remove(group1);
			{
				List<CurrentEntry> group2Entries = new ArrayList<>();
				for (String group2 : nextBlocks.get(group1)) { // Here is prev
					GroupingEntry entry2 = groupingEntryMap.get(group2);
					group2Entries.add(new CurrentEntry(group2, entry2.getAverageSize(dataMap), entry2.groups.size()));
				}
				Collections.sort(group2Entries, Collections.reverseOrder(CurrentEntry.queryNoComparator));
				LinkedHashSet<Integer> usedIndex = new LinkedHashSet<>();
				for (int i = 0; i < group2Entries.size(); i++) {
					if (usedIndex.contains(i))
						continue;
					GroupingEntry e1 = groupingEntryMap.get(group2Entries.get(i).name);
					for (int j = i + 1; j < group2Entries.size(); j++) {
						if (usedIndex.contains(j))
							continue;
						GroupingEntry e2 = groupingEntryMap.get(group2Entries.get(j).name);
						if (MatchHelper.fuzzyMatch(group2Entries.get(i).size, group2Entries.get(j).size, meas, ear)) {
							if (Collections.disjoint(dependentPrevBlocks.get(e1.name), dependentNextBlocks.get(e2.name))
							&& Collections.disjoint(dependentPrevBlocks.get(e2.name), dependentNextBlocks.get(e1.name)))
							if (e1.canMerge(e2)) {
								// we can later facilitate the merging process by checking which dependent sizes are larger. If necessary, reverse e1, e2
								maMergeGraph.merge(e1.name, e2.name);
								usedIndex.add(j);
								blocksToCheck.remove(e2.name);
								group2Entries.set(i, new CurrentEntry(e1.name, e1.getAverageSize(dataMap), e1.groups.size()));
							}
						}
					}
				}
			}
			
			
			{
				List<CurrentEntry> group2Entries = new ArrayList<>();
				for (String group2 : prevBlocks.get(group1)) { // Here is prev
					GroupingEntry entry2 = groupingEntryMap.get(group2);
					group2Entries.add(new CurrentEntry(group2, entry2.getAverageSize(dataMap), entry2.groups.size()));
				}
				Collections.sort(group2Entries, Collections.reverseOrder(CurrentEntry.queryNoComparator));
				LinkedHashSet<Integer> usedIndex = new LinkedHashSet<>();
				for (int i = 0; i < group2Entries.size(); i++) {
					if (usedIndex.contains(i))
						continue;
					GroupingEntry e1 = groupingEntryMap.get(group2Entries.get(i).name);
					for (int j = i + 1; j < group2Entries.size(); j++) {
						if (usedIndex.contains(j))
							continue;
						GroupingEntry e2 = groupingEntryMap.get(group2Entries.get(j).name);
						if (MatchHelper.fuzzyMatch(group2Entries.get(i).size, group2Entries.get(j).size, meas, ear)) {
							if (Collections.disjoint(dependentPrevBlocks.get(e1.name), dependentNextBlocks.get(e2.name))
							&& Collections.disjoint(dependentPrevBlocks.get(e2.name), dependentNextBlocks.get(e1.name)))
							if (e1.canMerge(e2)) {
								// we can later facilitate the merging process by checking which dependent sizes are larger. If necessary, reverse e1, e2
								maMergeGraph.merge(e1.name, e2.name);
								usedIndex.add(j);
								blocksToCheck.remove(e2.name);
								group2Entries.set(i, new CurrentEntry(e1.name, e1.getAverageSize(dataMap), e1.groups.size()));
							}
						}
					}
				}
			}
		}		
		
	}

	/**
	 * Merges blocks according to segment links
	 * @param maMergeGraph 
	 * @param segmentLinks
	 * @param allowMergeChain When this option is enabled, linked segments from two different chains are combined 
	 */
	public void mergeBySegmentLinks(MAMergeGraph maMergeGraph, Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> segmentLinks, boolean allowMergeChain) {
			
		Map<SegmentIdentifier, String> entrySegmentIndices = maMergeGraph.entrySegmentIndices;
		Map<String, GroupingEntry> groupingEntryMap = maMergeGraph.groupingEntryMap;
		Map<String, Integer> chainEntryIndice = maMergeGraph.chainEntryIndice;
		Map<Integer, Set<String>> chainEntries = maMergeGraph.chainEntries;
		
		Map<String, Set<String>> dependentPrevBlocks = maMergeGraph.dependentPrevBlocks;
		Map<String, Set<String>> dependentNextBlocks = maMergeGraph.dependentNextBlocks;
		
		VerbosePrinter.println("Parsing segment links...");
		// Initialize linkage
		PotentialGroupEntryMergeManager manager = new PotentialGroupEntryMergeManager();
		entrySegmentIndices.keySet().forEach(si1 -> {
			if (!segmentLinks.containsKey(si1))
				return;
			Map<SegmentIdentifier, StrandSupport> map = segmentLinks.get(si1);
			map.forEach((si2, strandSupport) -> {
				// if (strandSupport.forwardSupport >= 1 && strandSupport.reverseSupport >= 1)
				// return;
				if (!entrySegmentIndices.containsKey(si2))
					return;

				String group1 = entrySegmentIndices.get(si1);
				String group2 = entrySegmentIndices.get(si2);
				if (group1.equals(group2))
					return;
				int siStrand = strandSupport.getBestStrand();
				int entryStrand = groupingEntryMap.get(group1).groups.get(si1.name).orientation * groupingEntryMap.get(group2).groups.get(si2.name).orientation;
				if (siStrand == entryStrand)
					manager.addLink(group1, group2, 1);
				else
					manager.addLink(group1, group2, -1);
			});
		});
		manager.buildQueue();

		VerbosePrinter.println("Merging blocks...");
		PotentialGroupEntryMerge potentialGroupEntryMerge;
		while ((potentialGroupEntryMerge = manager.getNext()) != null) {
			String tgroup1 = potentialGroupEntryMerge.group1;
			String tgroup2 = potentialGroupEntryMerge.group2;
			
			if (groupingEntryMap.containsKey(tgroup1) && groupingEntryMap.containsKey(tgroup2)) // Check if the group still exists
				if (groupingEntryMap.get(tgroup1).canMerge(groupingEntryMap.get(tgroup2))) // Check if the groups themselves are mergable
					if (chainEntryIndice.get(tgroup1).equals(chainEntryIndice.get(tgroup2)) || allowMergeChain) // Check if the two groups are from different chains  
						if ((!chainEntryIndice.get(tgroup1).equals(chainEntryIndice.get(tgroup2)))  // if it is a merge from two chains, we allow everything (Because there will be no overlapping dependent blocks
								|| (potentialGroupEntryMerge.orientation == 1 && Collections.disjoint(dependentPrevBlocks.get(tgroup1), dependentNextBlocks.get(tgroup2))
								&& Collections.disjoint(dependentPrevBlocks.get(tgroup2), dependentNextBlocks.get(tgroup1)))    // The orientation has to be the same, and there should be no overlap in dependent blocks 								 
							) {
							// Use the one with more dependent blocks as group1
							String group1;
							String group2;
							if (dependentPrevBlocks.get(tgroup1).size() + dependentNextBlocks.get(tgroup1).size() >= dependentPrevBlocks.get(tgroup2).size() + dependentNextBlocks.get(tgroup2).size()) {
								group1 = tgroup1;
								group2 = tgroup2;
							} else {
								group1 = tgroup2;
								group2 = tgroup1;
							}
							
							// Reverse orientation for the whole chain before merging if necessary
							if (potentialGroupEntryMerge.orientation == -1) {
								
								Integer chainIndex = chainEntryIndice.get(group2);
								maMergeGraph.reverseChain(chainIndex);
								
								Set<String> blocksToReverse = chainEntries.get(chainIndex);
								manager.changeOrientation(blocksToReverse);
								
							}
							
							maMergeGraph.merge(group1, group2);

							// Update manager
							// In case of orientation == -1, the map and queue is already reversed using the changeOrientation method. So we only need to use 1
							manager.combineGroup(group1, group2, 1);
							
						}
		}

	}

	

	// Order of query
	
	/**
	 * Order the queries according to order of blocks first appeared in the multiple alignment
	 * @param queryNames
	 * @param entries
	 * @return the order of queries
	 */
	public List<String> getQueryOrdersByFirstAppearance(List<String> queryNames, List<GroupingEntry> entries) {
		List<String> newOrders = new ArrayList<>();
		boolean[] usedOrders = new boolean[queryNames.size()];
		OUTLOOP: for (GroupingEntry entry : entries)
			for (int i = 0; i < queryNames.size(); i++)
				if (!usedOrders[i])
					if (entry.groups.get(queryNames.get(i)) != null) {
						newOrders.add(queryNames.get(i));
						usedOrders[i] = true;
						if (newOrders.size() == queryNames.size())
							break OUTLOOP;
					}
		assert newOrders.size() == queryNames.size();
		return newOrders;
	}

	/**
	 * Orders the queries based on similarity of queries deduced using UPGMA method
	 * @param queryNames
	 * @param entries
	 * @return the order of queries
	 */
	public List<String> getQueryOrderBySimilarity(List<String> queryNames, List<GroupingEntry> entries) {
		if (queryNames.isEmpty())
			return new ArrayList<>();
		return UPGMATreeConstruction.constructTree(queryNames, MultipleAlignment.getDissimilarityMatrix(queryNames, entries)).toFlatNameList();

	}
	
	/**
	 * A method to parse multiple alignment results into dissimilarity matrix
	 * @param queryNames Names of queries in the dissimilarity matrix
	 * @param entries multiple alignment results
	 * @return dissimilarity matrix
	 */
	public static Map<String, Map<String, Double>> getDissimilarityMatrix(Collection<String> queryNames, List<GroupingEntry> entries) {
		// For any two queries q1 and q2, we have dissimilarity equal to (no. of intersection blocks / total q1 blocks + no. of intersection blocks / total q2 blocks) / 2
		
		Map<String, Set<String>> queryBlocksAssignment = new LinkedHashMap<>();
		queryNames.forEach(queryName -> queryBlocksAssignment.put(queryName, new LinkedHashSet<>()));
		entries.forEach(e -> e.groups.keySet().forEach(queryName -> {
			queryBlocksAssignment.get(queryName).add(e.name);
		}));

		Map<String, Map<String, Double>> dissimilarityMatrix = new LinkedHashMap<>();
		queryNames.forEach(name -> dissimilarityMatrix.put(name, new LinkedHashMap<>()));
		queryNames.forEach(name1 -> queryNames.forEach(name2 -> {
			Set<String> blocks1 = queryBlocksAssignment.get(name1);
			Set<String> blocks2 = queryBlocksAssignment.get(name2);
			long intersectionCount = blocks1.stream().filter(b -> blocks2.contains(b)).count();
			double dissimilarity = 1 - ((intersectionCount / (double) blocks1.size()) + (intersectionCount / (double) blocks2.size())) / 2;
			dissimilarityMatrix.get(name1).put(name2, dissimilarity);
		}));
		return dissimilarityMatrix;

	}


	// Main class
	
	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(MultipleAlignment.class.getSimpleName(), "Performs multiple alignment taking multiple optical maps as queries");
		OptMapDataReader.assignOptions(parser);
		parser.addHeader("Multiple Alignment Options", 1);
		OptionSpec<String> omaproc = parser.accepts("maconfig", "Multiple alignment configuration file").withRequiredArg().ofType(String.class);
		OptionSpec<Integer> ominlink = parser.accepts("minlinksize", "Minimum segment size in linking process").withRequiredArg().ofType(Integer.class).defaultsTo(1000);
		OptionSpec<String> omaref = parser.accepts("maref", "References for multiple alignment").withRequiredArg().ofType(String.class);
		OptionSpec<Integer> oorderby = parser.accepts("maorderby", "1: First appearance of block; 2: similarity clustering").withRequiredArg().ofType(Integer.class).defaultsTo(1);
		CollinearBlockWriter.assignOptions(parser, 1);
		CollinearBlockOrder.assignWriteOptions(parser, 1);
		parser.addHeader(null, 0);
		OptionSpec<Boolean> osingle = parser.accepts("single", "Output as single").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<String> oconnectionlog = parser.accepts("connectionlog", "Connection log file").withRequiredArg().ofType(String.class);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);
		List<String> marefs = options.valuesOf(omaref);
		VerbosePrinter.println("Reading data...");
		LinkedHashMap<String, DataNode> rawDataMap = OptMapDataReader.readAllData(options);
		// Process data and extract basic information
		Map<String, DataNode> dataMap = rawDataMap.values().stream().filter(d -> d.getTotalSegment() > 2).collect(Collectors.toMap(d -> d.name, d -> d));
		List<String> queryNames = new ArrayList<>(dataMap.keySet());
		int minLinkSize = options.valueOf(ominlink);
		int noOfQueries = dataMap.size();
		if (noOfQueries < rawDataMap.size())
			VerbosePrinter.println((rawDataMap.size() - noOfQueries) + " queries are removed with too few segments available.");
		int noOfSegments = dataMap.values().stream().mapToInt(DataNode::getTotalSegment).reduce(0, (totalSegments, dataSegments) -> totalSegments + dataSegments - 2);
		VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries will be aligned.");

		MultipleAlignment multipleAlignment = new MultipleAlignment();
		
		PreCollinearBlocks preCollinearBlocks = new PreCollinearBlocks(queryNames);
		Set<GroupingEntryChain> chains = null;
		MAMergeGraph maMergeGraph = null;
		
		
		if (noOfSegments > 0) { // If there is no segment, don't do multiple alignment
			// Mask the small segments
			Set<SegmentIdentifier> maskedSegments = new LinkedHashSet<>();
			for (DataNode data : dataMap.values())
				for (int i = 1; i < data.getTotalSegment(); i++)
					if (data.getRefl(i) < minLinkSize)
						maskedSegments.add(new SegmentIdentifier(data.name, i));
			
			// A map to store linking information, in case the same file is used multiple times
			Map<String, Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>>> segmentLinksMap = new LinkedHashMap<>();
			// A map to determine whether segmentLink can be disposed
			Map<String, MutableInt> evidenceRemainingUse = new LinkedHashMap<>();
			
			VerbosePrinter.println("Processing configuration...");
			List<String> inputs = ListExtractor.extractList(options.valueOf(omaproc));
			// Parse the procedures for file usage to prevent IO from same file
			for (String input : inputs) {
				if (input.isEmpty())
					continue;
				if (input.startsWith("#"))
					continue;
				String[] l = input.split("\\s+");
				// Store linking information
				switch (l[0]) {
					case "link":
					case "merge":
						for (int i = 1; i < l.length; i++)
							if (!l[i].contains("=")) {
								String filename = l[i];
								if (!evidenceRemainingUse.containsKey(filename))
									evidenceRemainingUse.put(filename, new MutableInt(0));
								evidenceRemainingUse.get(filename).increment();
							}
						break;
				}
			}
			
			// Start the multiple alignment process
			for (String input : inputs) {
				if (input.isEmpty())
					continue;
				if (input.startsWith("#"))
					continue;
				String[] l = input.split("\\s+");
				String proc = l[0].toLowerCase();
				switch (proc) {
					case "mask": {
						String filename = l[1];
						VerbosePrinter.println("Reading evidence for segment masks from " + l[1]);
						String extension = FilenameUtils.getExtension(filename);
						try {
							if (SegmentIdentifierFormat.isValidFormat(extension)) {
								SegmentIdentifierReader sir = new SegmentIdentifierReader(filename);
								List<SegmentIdentifier> sis = sir.readAll();
								sir.close();
								maskedSegments.addAll(sis);
							} else {
								System.err.println("Unknown file extension for: " + filename);
							}
						} catch (IOException e) {
							System.err.println("Unexpected IO exception when reading the file: " + filename);
						}
						break;
					}
					case "removemask": { 
						// A sub-module to remove the existing mask based on .si files. 
						maskedSegments.clear();
						// Re-process small segment to be masked
						for (DataNode data : dataMap.values())
							for (int i = 1; i < data.getTotalSegment(); i++)
								if (data.getRefl(i) < minLinkSize)
									maskedSegments.add(new SegmentIdentifier(data.name, i));
						segmentLinksMap.clear(); // Remove existing segment links. This will force re-generating the segment links without the masks
						break;
					}
					case "link":
					case "merge": {
						List<String> filenames = new ArrayList<>();
						Map<String, String> attributes = new LinkedHashMap<>();
						for (int i = 1; i < l.length; i++)
							if (!l[i].contains("="))
								filenames.add(l[i]);
							else
								Arrays.stream(l[i].split(";")).map(s -> s.split("=")).forEach(ll -> attributes.put(ll[0].toLowerCase(), ll[1]));
						if (filenames.size() == 0)
							System.err.println("Warning: no input file for the current procedure \"" + input + "\".");
						
						List<Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>>> segmentLinksList = new ArrayList<>();
						for (String filename : filenames) {
							Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> segmentLinks = null;
							if (segmentLinksMap.containsKey(filename))
								segmentLinks = segmentLinksMap.get(filename);
							else {
								VerbosePrinter.println("Reading evidence for segment links from " + filename);
								String extension = FilenameUtils.getExtension(filename);
								try {
									if (ResultFormat.isValidFormat(extension)) {
										OptMapResultReader omrr = new OptMapResultReader(filename);
										omrr.importFragInfo(dataMap);
										List<OptMapResultNode> alignments = omrr.readAll();
										omrr.close();
										Collections.sort(alignments, Collections.reverseOrder(OptMapResultNode.mappedscorecomparator));
										segmentLinks = multipleAlignment.getSegmentLinksFromAlignment(alignments, dataMap, maskedSegments);
									} else if (MultipleAlignmentFormat.isValidCollinearBlockFormat(extension)) {
										LinkedHashMap<String, CollinearBlock> blocks = CollinearBlockReader.readAllData(filename);
										segmentLinks = multipleAlignment.getSegmentLinksFromMultipleAlignment(blocks);
									} else if (ClusteringFormat.isValidFormat(extension)) {
										int k = Integer.valueOf(Integer.parseInt(attributes.get("k")));
										List<String> clusterString = ListExtractor.extractList(filename);
										List<Integer> clusterList = new ArrayList<>();
										for (String s : clusterString)
											clusterList.add(Integer.parseInt(s));
	
										List<Kmer> kmerList = new ArrayList<>();
										for (DataNode data : dataMap.values())
											kmerList.addAll(data.getKmerWord(k, Integer.parseInt(attributes.get("maxnosignal"))));
	
										LinkedHashMap<Integer, KmerCluster> clusters = KmerCluster.createKmerClusters(clusterList, kmerList, k);
										segmentLinks = multipleAlignment.getSegmentLinksFromClustering(clusters);
	
									} else {
										System.err.println("Unknown file extension for: " + filename);
									}
	
								} catch (IOException e) {
									System.err.println("Unexpected IO exception when reading the file: " + filename);
								}
								segmentLinksMap.put(filename, segmentLinks);
							}
							evidenceRemainingUse.get(filename).decrement();
							if ((evidenceRemainingUse.get(filename)).intValue() == 0)
								segmentLinksMap.remove(filename);
							if (segmentLinks != null) {
								segmentLinksList.add(segmentLinks);
							} 
							else {
								System.err.println("The following file is skipped due to errors in parsing segment links: " + filename);
							}
						}
						
						// Empty if no segment link is processed and this command is skipped
						if (!segmentLinksList.isEmpty()) {
							Map<SegmentIdentifier, ? extends Map<SegmentIdentifier, StrandSupport>> segmentLinks;
							if (segmentLinksList.size() == 1)
								segmentLinks = segmentLinksList.get(0); // Do not need to reparse the segment links
							else
								segmentLinks = multipleAlignment.combineSegmentLinks(segmentLinksList);

							if (proc.equals("link")) {
								if (chains == null)
									multipleAlignment.updatePreCollinearBlocks(preCollinearBlocks, segmentLinks, null);
								else
									System.err.println("Grouping entries already exist! This link step is skipped.");
							} else if (proc.equals("merge")) {
								if (chains != null) {
									if (maMergeGraph == null) {
										VerbosePrinter.println("Building digraphs from collinear blocks for merging steps...");
										maMergeGraph = new MAMergeGraph(chains);
									}

									boolean allowMergeChain = true;
									if (attributes.containsKey("mergechain"))
										allowMergeChain = Boolean.parseBoolean(attributes.get("mergechain"));

									multipleAlignment.mergeBySegmentLinks(maMergeGraph, segmentLinks, allowMergeChain);
									VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into "
											+ maMergeGraph.getNumberOfBlocks() + " blocks in " + maMergeGraph.getNumberOfChains() + " chains.");
								} else
									System.err.println("Grouping entries does not exist. You need to build prior to merging.");
							} else
								assert false;
						}
						break;
					}
					case "build":
						if (chains == null) {
							boolean allowRearrangement = true; // Allow rearrangement by default
							Map<String, String> attributes = new LinkedHashMap<>();
							if (l.length > 1)
								Arrays.stream(l[1].split(";")).map(s -> s.split("=")).forEach(ll -> attributes.put(ll[0].toLowerCase(), ll[1]));
							if (attributes.containsKey("allowrearrangement"))
								allowRearrangement = Boolean.parseBoolean(attributes.get("allowrearrangement"));
//							VerbosePrinter.println("Filling unassigned segments to individual group...");
//							preCollinearBlocks.fillSegments(dataMap);
//							VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into " + preCollinearBlocks.getTotalBlocks() + " blocks.");
//							assert noOfSegments == preCollinearBlocks.getTotalAssignedSegmentIdentifiers();
//							assert noOfQueries == preCollinearBlocks.getTotalQueries();
							VerbosePrinter.println("Building collinear blocks...");
//							List<GroupingEntry> entries = preCollinearBlocks.toCollinearBlocks(queryNames);
							chains = preCollinearBlocks.toChains(dataMap, queryNames);
							// Connection log for debug
							if (options.has(oconnectionlog)) {
								VerbosePrinter.println("Processing log...");
								String filename = options.valueOf(oconnectionlog);
								ListExtractor.writeList(filename, multipleAlignment.collinearBlockNJLog(chains));
								VerbosePrinter.println("Complete writing connection log in " + filename + ".");
							}
							VerbosePrinter.println("Ordering collinear blocks...");							
//							chains = multipleAlignment.layoutCollinearBlocksNJ(entries, marefs, allowRearrangement);
							multipleAlignment.layoutCollinearBlocksNJ(chains, marefs, allowRearrangement);

							VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into " + chains.stream().mapToInt(GroupingEntryChain::getTotalEntries).sum()
									+ " blocks in " + chains.size() + " chains.");
						} else {
							System.err.println("Grouping entries already exist! This build step is skipped.");
						}
						break;
					case "mergeproximity": {
						if (chains != null) {
							if (maMergeGraph == null) {
								VerbosePrinter.println("Building digraphs from collinear blocks for merging steps...");
								maMergeGraph = new MAMergeGraph(chains);
							}
							Map<String, String> attributes = new LinkedHashMap<>();
							
							int meas = -1;
							double ear = -1;
							try {
								Arrays.stream(l[1].split(";")).map(s -> s.split("=")).forEach(ll -> attributes.put(ll[0].toLowerCase(), ll[1]));
								if (attributes.containsKey("meas"))
									meas = Integer.parseInt(attributes.get("meas"));
								if (attributes.containsKey("ear"))
									ear = Double.parseDouble(attributes.get("ear"));
							} catch (RuntimeException e) {
								throw new IllegalArgumentException("Error occurs when parsing the procedure parameters: \"" + input + "\". Please use the command as \"mergeproximity meas=500;ear=0.05\"");
							}
							multipleAlignment.mergeByProximity(maMergeGraph, dataMap, meas, ear);
							VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into "
									+ maMergeGraph.getNumberOfBlocks() + " blocks in " + maMergeGraph.getNumberOfChains() + " chains.");
						} else
							System.err.println("Grouping entries does not exist. You need to build prior to merging.");
						break;
					}
					case "mergeproximitysimple":
						// A simplifed version for mergeproximity. It breaks the "chain".  
						if (chains != null) {
							if (maMergeGraph != null) {
								VerbosePrinter.println("Retrieving collinear blocks from digraphs...");
								chains = maMergeGraph.getChains();
								maMergeGraph = null;
							}
							Map<String, String> attributes = new LinkedHashMap<>();
							int meas = -1;
							double ear = -1;
							try {
								Arrays.stream(l[1].split(";")).map(s -> s.split("=")).forEach(ll -> attributes.put(ll[0].toLowerCase(), ll[1]));
								meas = Integer.parseInt(attributes.get("meas"));
								ear = Double.parseDouble(attributes.get("ear"));
							} catch (RuntimeException e) {
								throw new IllegalArgumentException("Error occurs when parsing the procedure parameters: \"" + input + "\". Please use the command as \"mergeproximitysimple meas=500;ear=0.05\"");
							}
							VerbosePrinter.println("Merging collinear blocks by location...");
							Set<GroupingEntryChain> newChains = new LinkedHashSet<>();
							for (GroupingEntryChain chain : chains) {
								List<GroupingEntry> entries = chain.groupingEntries;
								boolean oneside = false;
								if (attributes.containsKey("oneside"))
									oneside = Boolean.parseBoolean(attributes.get("oneside"));
								entries = multipleAlignment.mergeByProximitySimple(entries, dataMap, meas, ear);
								if (!oneside) {
									List<GroupingEntry> reversedEntries = new ArrayList<>();
									for (int i = 0; i < entries.size(); i++)
										reversedEntries.add(entries.get(entries.size() - i - 1).getReverse(entries.get(entries.size() - i - 1).name));
									reversedEntries = multipleAlignment.mergeByProximitySimple(reversedEntries, dataMap, meas, ear);
									entries = new ArrayList<>();
									for (int i = 0; i < reversedEntries.size(); i++)
										entries.add(reversedEntries.get(reversedEntries.size() - i - 1).getReverse(reversedEntries.get(reversedEntries.size() - i - 1).name));
								}
								newChains.add(new GroupingEntryChain(chain.id, entries));
							}
							chains = newChains;
							VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into "
									+ chains.stream().mapToInt(GroupingEntryChain::getTotalEntries).sum() + " blocks in " + chains.size() + " chains.");
						} else
							System.err.println("Grouping entries does not exist. You need to build prior to merging.");
						break;
					default:
						System.err.println("Unknown procedure: " + proc);
						break;
				}
			}
			if (maMergeGraph != null) {
				VerbosePrinter.println("Retrieving collinear blocks from digraphs...");
				chains = maMergeGraph.getChains();
				maMergeGraph = null;
			}

		} else
			chains = new LinkedHashSet<>(); // Nothing to do when the chain is not processed at all

		if (chains.size() > 1) {
			multipleAlignment.layoutCollinearBlocksNJ(chains, marefs, true); // Perform rearrangement ordering
			VerbosePrinter.println(noOfSegments + " segments from " + noOfQueries + " queries are combined into " + chains.stream().mapToInt(GroupingEntryChain::getTotalEntries).sum() + " blocks in "
					+ chains.size() + " chains with rearrangement.");
		}
		VerbosePrinter.println("Writing collinear blocks to files...");
		List<GroupingEntry> entries = chains.stream().flatMap(chain -> chain.groupingEntries.stream()).collect(Collectors.toList());
		if (options.valueOf(osingle))
			CollinearBlockWriter.writeAll(options, CollinearBlock.toSingleSegmentCollinearBlocks(entries));
		else
			CollinearBlockWriter.writeAll(options, CollinearBlock.toCollinearBlocks(entries));

		List<String> order = new ArrayList<>();
		switch (options.valueOf(oorderby)) {
			case 1:
				order = multipleAlignment.getQueryOrdersByFirstAppearance(queryNames, entries);
				break;
			case 2:
				order = multipleAlignment.getQueryOrderBySimilarity(queryNames, entries);			
				break;
			default:
				order = multipleAlignment.getQueryOrdersByFirstAppearance(queryNames, entries);
				break;
		}		
		CollinearBlockOrder.writeAll(options, new CollinearBlockOrder(order));
		VerbosePrinter.println("Program ends.");
	}

}

class PreCollinearBlocks {
	/**
	 * A class to store relations prior to collinear block construction 
	 */
	private int nextID = 1;
	private final LinkedHashMap<String, LinkedHashMap<Integer, Integer>> segmentGroup = new LinkedHashMap<>();
	private final LinkedHashMap<Integer, LinkedHashMap<String, SingleGroup>> assignedBlockInfo = new LinkedHashMap<>();

	public PreCollinearBlocks(List<String> orders) {
		for (String name : orders)
			segmentGroup.put(name, new LinkedHashMap<Integer, Integer>());
	}

	public boolean addNewRelations(GroupingEntry entry) {
		return addNewRelations(entry.groups);
	}

	public boolean addNewRelations(LinkedHashMap<String, SingleGroup> singleGroups) {
		return this.addNewRelations(singleGroups, false);
	}

	/**
	 * If testOnly is true, the method does not actually add the new relation but only test if the addition will be successful
	 * 
	 * @param singleGroups
	 * @param testOnly
	 * @return
	 */
	public boolean addNewRelations(LinkedHashMap<String, SingleGroup> singleGroups, boolean testOnly) {
		LinkedHashMap<Integer, Integer> assignedBlockStrand = new LinkedHashMap<>();
		for (String name : singleGroups.keySet()) {
			SingleGroup group = singleGroups.get(name);
			Integer blockID = segmentGroup.get(name).get(group.segment);
			if (blockID != null) {
				SingleGroup existGroup = assignedBlockInfo.get(blockID).get(name);
				int blockOrientation = group.orientation * existGroup.orientation;
				if (assignedBlockStrand.containsKey(blockID)) {
					if (assignedBlockStrand.get(blockID) != blockOrientation) // Some similar segments are assigned in different orientations in the same block
						return false;
				} else
					assignedBlockStrand.put(blockID, blockOrientation);
			}
		}

		if (assignedBlockStrand.size() == 1) {
			// Only one block is involved, we directly modify the block
			int blockID = assignedBlockStrand.keySet().iterator().next();
			int blockOrientation = assignedBlockStrand.values().iterator().next();
			LinkedHashMap<String, SingleGroup> block = assignedBlockInfo.get(blockID);

			for (String name : singleGroups.keySet()) {
				if (block.containsKey(name))
					if (block.get(name).segment != singleGroups.get(name).segment)
						return false;
			}

			// Put remaining in the block
			if (!testOnly)
				for (String name : singleGroups.keySet()) {
					if (block.containsKey(name))
						block.put(name, new SingleGroup(singleGroups.get(name).segment, singleGroups.get(name).orientation * blockOrientation));

				}
		} else {
			// Build a new block instead of retaining the old blocks
			int newID = nextID++;
			LinkedHashMap<String, SingleGroup> newBlock = new LinkedHashMap<>();

			for (Entry<Integer, Integer> blockStrandEntry : assignedBlockStrand.entrySet()) {
				int blockID = blockStrandEntry.getKey();
				int blockOrientation = blockStrandEntry.getValue();
				LinkedHashMap<String, SingleGroup> block = assignedBlockInfo.get(blockID);
				for (Entry<String, SingleGroup> entry : block.entrySet()) {
					String name = entry.getKey();
					if (!newBlock.containsKey(name)) {
						SingleGroup group = entry.getValue();
						newBlock.put(name, new SingleGroup(group.segment, group.orientation * blockOrientation));
					} else
						return false; // fail, some segments from same source are overlapping among blocks
				}
			}

			// Check all SingleGroup in the map for contradictions
			for (String name : singleGroups.keySet()) {
				if (newBlock.containsKey(name))
					if (newBlock.get(name).segment != singleGroups.get(name).segment)
						return false; // fail, some segments from same source exist in blocks
			}

			// Put remaining singleGroup, add the new block, and remove the old blocks
			if (!testOnly) {
				for (String name : singleGroups.keySet()) {
					if (!newBlock.containsKey(name))
						newBlock.put(name, new SingleGroup(singleGroups.get(name).segment, singleGroups.get(name).orientation));
				}

				assignedBlockInfo.put(newID, newBlock);
				for (int blockID : assignedBlockStrand.keySet())
					assignedBlockInfo.remove(blockID);
				for (Entry<String, SingleGroup> entry : newBlock.entrySet())
					segmentGroup.get(entry.getKey()).put(entry.getValue().segment, newID);
			}
		}

		return true;
	}

	/**
	 * For segments that have not been assigned to any block, assigned a block to each of them. This is no longer used. 
	 * 
	 * @param dataMap
	 */
	@Deprecated
	public void fillSegments(Map<String, DataNode> dataMap) {
		for (DataNode data : dataMap.values())
			for (int segment = 1; segment < data.getTotalSegment() - 1; segment++)
				if (!segmentGroup.get(data.name).containsKey(segment)) {
					int newID = nextID++;
					segmentGroup.get(data.name).put(segment, newID);
					LinkedHashMap<String, SingleGroup> block = new LinkedHashMap<>();
					block.put(data.name, new SingleGroup(segment, 1));
					assignedBlockInfo.put(newID, block);
				}

	}
	
	@Deprecated
	public List<GroupingEntry> toCollinearBlocks(List<String> orders) {
		List<GroupingEntry> collinearBlocks = new ArrayList<>();
		for (Entry<Integer, LinkedHashMap<String, SingleGroup>> entry : assignedBlockInfo.entrySet()) {
			// List<SingleGroup> groups = new ArrayList<>();
			LinkedHashMap<String, SingleGroup> groups = new LinkedHashMap<>();
			LinkedHashMap<String, SingleGroup> groupMap = entry.getValue();
			for (String name : orders)
				// groups.add(groupMap.get(name));
				if (groupMap.containsKey(name))
					groups.put(name, groupMap.get(name));
			collinearBlocks.add(new GroupingEntry(entry.getKey() + "", groups));
		}
		return collinearBlocks;
	}
	
	public Set<GroupingEntryChain> toChains(Map<String, DataNode> dataMap, List<String> queries) {
		LinkedHashSet<GroupingEntryChain> chains = new LinkedHashSet<>();
		
		int chainID = 1;
		
		// Fill segments and connect consecutive segments directly
		int entryID = this.nextID;
		for (String query : queries) {
			DataNode data = dataMap.get(query);
			List<GroupingEntry> groupingEntries = new ArrayList<>();
			int lastSegment = -1;
			for (int segment = 1; segment < data.getTotalSegment() - 1; segment++)
				if (!segmentGroup.get(data.name).containsKey(segment)) {
					int newID = entryID++;
					segmentGroup.get(data.name).put(segment, newID);
					LinkedHashMap<String, SingleGroup> groups = new LinkedHashMap<>();
					groups.put(data.name, new SingleGroup(segment, 1));
					if (lastSegment != segment - 1)
						if (!groupingEntries.isEmpty()) {
							chains.add(new GroupingEntryChain(chainID++, groupingEntries));
							groupingEntries = new ArrayList<>();
						}
					groupingEntries.add(new GroupingEntry(newID + "", groups));
					lastSegment = segment;
				}
			if (!groupingEntries.isEmpty())
				chains.add(new GroupingEntryChain(chainID++, groupingEntries));
		}

		// Add the already processed segments
		for (Entry<Integer, LinkedHashMap<String, SingleGroup>> entry : assignedBlockInfo.entrySet()) {
			LinkedHashMap<String, SingleGroup> groups = new LinkedHashMap<>();
			LinkedHashMap<String, SingleGroup> groupMap = entry.getValue();
			for (String name : queries)
				if (groupMap.containsKey(name))
					groups.put(name, groupMap.get(name));
			chains.add(new GroupingEntryChain(chainID++, (new GroupingEntry(entry.getKey() + "", groups))));
		}
		return chains;
	}

	public int getTotalAssignedSegmentIdentifiers() {
		int total = 0;
		for (LinkedHashMap<Integer, Integer> map : segmentGroup.values())
			total += map.size();
		return total;
	}

	public int getTotalQueries() {
		return segmentGroup.size();
	}

	public int getTotalBlocks() {
		return assignedBlockInfo.size();
	}
}

class CurrentEntry {
	String name;
	long size;
	int queryNo;

	public CurrentEntry(String name, long size, int queryNo) {
		super();
		this.name = name;
		this.size = size;
		this.queryNo = queryNo;
	}

	static Comparator<CurrentEntry> queryNoComparator = new Comparator<CurrentEntry>() {
		@Override
		public int compare(CurrentEntry e1, CurrentEntry e2) {
			return Integer.compare(e1.queryNo, e2.queryNo);
		}

	};

}

class PotentialGroupEntryMerge implements Comparable<PotentialGroupEntryMerge> {
	String group1;
	String group2;
	public int orientation;
	double support;

	public PotentialGroupEntryMerge(String group1, String group2, int orientation, double support) {
		this.group1 = group1;
		this.group2 = group2;
		this.orientation = orientation;
		this.support = support;
	}

	@Override
	public int compareTo(PotentialGroupEntryMerge gem) {
		return Double.compare(this.support, gem.support) * -1;
	}
}

class PotentialGroupEntryMergeManager {

	private Map<String, Map<String, StrandSupport>> map = new LinkedHashMap<>();
	private PriorityQueue<PotentialGroupEntryMerge> queue = null;

	void addLink(String group1, String group2, int orientation) {
		addLink(group1, group2, orientation, 1);
	}

	void addLink(String group1, String group2, int orientation, double count) {
		if (group1.equals(group2))
			throw new IllegalArgumentException("group1 and group2 are the same.");
		if (group1.compareTo(group2) > 0) {
			String tmp = group1;
			group1 = group2;
			group2 = tmp;
		}
		if (count == 0)
			return;

		if (!map.containsKey(group1))
			map.put(group1, new LinkedHashMap<String, StrandSupport>());
		if (!map.get(group1).containsKey(group2))
			map.get(group1).put(group2, new StrandSupport());
		map.get(group1).get(group2).addSupport(orientation, count);
		if (!map.containsKey(group2))
			map.put(group2, new LinkedHashMap<String, StrandSupport>());
		if (!map.get(group2).containsKey(group1))
			map.get(group2).put(group1, new StrandSupport());
		map.get(group2).get(group1).addSupport(orientation, count);
	}

	void addLink(String group1, String group2, StrandSupport strandSupport) {
		addLink(group1, group2, 1, strandSupport.forwardSupport);
		addLink(group1, group2, -1, strandSupport.reverseSupport);
	}

	void buildQueue() {
		queue = new PriorityQueue<PotentialGroupEntryMerge>();
		map.forEach((group1, map) -> map.forEach((group2, v) -> {
			queue.add(new PotentialGroupEntryMerge(group1, group2, 1, v.forwardSupport));
			queue.add(new PotentialGroupEntryMerge(group1, group2, -1, v.reverseSupport));
		}));
	}

	void combineGroup(String retainedGroup, String removedGroup, int orientation) {
		if (queue == null)
			throw new IllegalStateException("Build queue prior to combineGroup");
		if (retainedGroup.equals(removedGroup))
			throw new IllegalArgumentException("Cannot retain and remove the same group.");
		Map<String, StrandSupport> removedMap = map.remove(removedGroup);
		removedMap.forEach((k, v) -> {
			// Update other linked groups
			map.get(k).remove(removedGroup);
			// Update the link to retained group
			if (!k.equals(retainedGroup)) {
				addLink(k, retainedGroup, orientation == 1 ? v : v.getReverse());
				// Add in queue if needed
				queue.add(new PotentialGroupEntryMerge(retainedGroup, k, 1, map.get(retainedGroup).get(k).forwardSupport));
				queue.add(new PotentialGroupEntryMerge(retainedGroup, k, -1, map.get(retainedGroup).get(k).reverseSupport));
			}
		});
	}

	public PotentialGroupEntryMerge getNext() {
		if (queue == null)
			throw new IllegalStateException("Build queue prior to getNext");
		while (!queue.isEmpty()) {
			PotentialGroupEntryMerge y = queue.poll();
			if (map.containsKey(y.group1))
				if (map.get(y.group1).containsKey(y.group2)) {
					if (y.support > 0)
						return y;
				}
		}
		return null;
	}

	public void changeOrientation(Set<String> set) {
		for (String group1 : set) {
			if (map.containsKey(group1))
				for (String group2 : map.get(group1).keySet()) {
					if (!set.contains(group2)) {
						map.get(group1).get(group2).reverse();
						map.get(group2).get(group1).reverse();
					}
			}
		}
		queue.forEach(pgem -> {
			if (set.contains(pgem.group1) ^ set.contains(pgem.group2))
				pgem.orientation *= -1;
		});
	}

}

