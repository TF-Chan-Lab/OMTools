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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import aldenjava.common.NumberOperation;
import aldenjava.common.UnweightedRange;

/**
 * GroupingEntryChain represents a chain of collinear blocks (GroupingEntry). 
 * @author Alden
 *
 */
public class GroupingEntryChain {
	public final int id; 
	public final List<GroupingEntry> groupingEntries;
	private GroupingEntry incomingEntry;
	private GroupingEntry outgoingEntry;
	private final Map<String, Integer> incomingEntryIndex;
	private final Map<String, Integer> outgoingEntryIndex;
	private final Map<String, ChainEntryProperty> entryProperties;
	public GroupingEntryChain(int id, GroupingEntry entry) {
		this.id = id;
		// Create the dummy incoming and going entries
		incomingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>(entry.groups));
		outgoingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>(entry.groups));
		incomingEntryIndex = new HashMap<>();
		outgoingEntryIndex = new HashMap<>();
		entry.groups.forEach((name, g) -> {
			incomingEntryIndex.put(name, 0);
			outgoingEntryIndex.put(name, 0);
		});
		groupingEntries = new ArrayList<>();
		groupingEntries.add(entry);
		entryProperties = new HashMap<>();
		entry.groups.forEach((name, g) -> {
			entryProperties.put(name, new ChainEntryProperty(0, 0, new SingleGroup(g), new SingleGroup(g)));
		});
	}
	public GroupingEntryChain(int id, List<GroupingEntry> groupingEntries) {
		this.id = id;
		this.groupingEntries = groupingEntries;
		incomingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>());
		outgoingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>());
		incomingEntryIndex = new HashMap<>();
		outgoingEntryIndex = new HashMap<>();

		for (int i = 0; i < groupingEntries.size(); i++) {
			GroupingEntry entry = groupingEntries.get(i);
			int index = i;
			entry.groups.forEach((name, g) -> {
				if (incomingEntry.groups.get(name) == null) {
					incomingEntry.groups.put(name, new SingleGroup(g));
					incomingEntryIndex.put(name, index);
				}
				outgoingEntry.groups.put(name, new SingleGroup(g));
				outgoingEntryIndex.put(name, index);
			});
		}
		
		entryProperties = new HashMap<>();
		for (int i = 0; i < groupingEntries.size(); i++) {
			GroupingEntry entry = groupingEntries.get(i);
			int index = i;
			entry.groups.forEach((name, g) -> {
				if (!entryProperties.containsKey(name))
					entryProperties.put(name, new ChainEntryProperty(index, index, new SingleGroup(g), new SingleGroup(g)));
				else {
					entryProperties.get(name).outIndex = index;
					entryProperties.get(name).outSegmentGroup = new SingleGroup(g);
				}
			});
		}

		
	}
	public GroupingEntryChain(int id, List<GroupingEntry> groupingEntries, GroupingEntry incomingEntry, GroupingEntry outgoingEntry, Map<String, Integer> incomingEntryIndex, Map<String, Integer> outgoingEntryIndex, Map<String, ChainEntryProperty> entryProperties) {
		this.id = id;
		this.groupingEntries = groupingEntries;
		this.incomingEntry = incomingEntry;
		this.outgoingEntry = outgoingEntry;
		this.incomingEntryIndex = incomingEntryIndex;
		this.outgoingEntryIndex = outgoingEntryIndex;
		this.entryProperties = entryProperties; 
	}

	public GroupingEntryChain getReverse() {
		List<GroupingEntry> newGroupingEntries = new ArrayList<>();
		for (GroupingEntry entry : groupingEntries)
			newGroupingEntries.add(entry.getReverse(entry.name));
		Collections.reverse(newGroupingEntries);
		
		Map<String, Integer> newIncomingEntryIndex = outgoingEntryIndex.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> this.getTotalEntries() - e.getValue() - 1));
		Map<String, Integer> newOutgoingEntryIndex = incomingEntryIndex.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> this.getTotalEntries() - e.getValue() - 1));
		Map<String, ChainEntryProperty> newEntryProperties = new HashMap<>();
		entryProperties.forEach((name, p) -> newEntryProperties.put(name, p.getReverse(getTotalEntries())));
		
		return new GroupingEntryChain(id, newGroupingEntries, outgoingEntry.getReverse("Dummy"), incomingEntry.getReverse("Dummy"), newIncomingEntryIndex, newOutgoingEntryIndex, newEntryProperties);
	}
	public GroupingEntryChain(int id, GroupingEntryChain entryChain) {
		// Not a copy of the old chain
		this.id = id;
		groupingEntries = entryChain.groupingEntries;
		incomingEntry = entryChain.incomingEntry;
		outgoingEntry = entryChain.outgoingEntry;
		incomingEntryIndex = entryChain.incomingEntryIndex;
		outgoingEntryIndex = entryChain.outgoingEntryIndex;
		entryProperties = entryChain.entryProperties;
		
	}
	/**
	 * By default, put targetChain first whenever possible
	 * @param id
	 * @param baseChain
	 * @param targetChain
	 * @param entryMatchRanges
	 * @return
	 */
	public static GroupingEntryChain joinChain(int id, EntryConnection connection) {
		GroupingEntryChain baseChain = connection.baseReverse == 1 ? connection.baseEntryChain : connection.baseEntryChain.getReverse();
		GroupingEntryChain targetChain = connection.targetReverse == 1 ? connection.targetEntryChain : connection.targetEntryChain.getReverse();
		List<EntryMatchRange<Integer, Integer>> entryMatchRanges = connection.entryMatchRanges;
		
		List<GroupingEntry> newGroupingEntries = new ArrayList<>();
		int nextBaseIndexToAdd = 0;
		int nextTargetIndexToAdd = 0;
		for (EntryMatchRange<Integer, Integer> entryMatchRange : entryMatchRanges) {
			for (int targetIndex = nextTargetIndexToAdd; targetIndex < entryMatchRange.limitingRange.min; targetIndex++) {
				newGroupingEntries.add(targetChain.groupingEntries.get(targetIndex));
				nextTargetIndexToAdd = targetIndex + 1;
			}
			for (int baseIndex = nextBaseIndexToAdd; baseIndex <= entryMatchRange.max; baseIndex++) {
				newGroupingEntries.add(baseChain.groupingEntries.get(baseIndex));
				nextBaseIndexToAdd = baseIndex + 1;
			}
		}
		// Add remaining grouping entries
		for (int targetIndex = nextTargetIndexToAdd; targetIndex < targetChain.getTotalEntries(); targetIndex++) {
			newGroupingEntries.add(targetChain.groupingEntries.get(targetIndex));
		}
		for (int baseIndex = nextBaseIndexToAdd; baseIndex < baseChain.getTotalEntries(); baseIndex++) {
			newGroupingEntries.add(baseChain.groupingEntries.get(baseIndex));
		}
//		System.out.println("E0");
//		System.out.println(connection.baseReverse + ", " + connection.targetReverse);
//		System.out.println("E1");
//		baseChain.groupingEntries.forEach(entry -> System.out.println(entry.name));
//		System.out.println("E2");
//		targetChain.groupingEntries.forEach(entry -> System.out.println(entry.name));
//		System.out.println("E3");
//		newGroupingEntries.forEach(entry -> System.out.println(entry.name));
//		System.out.println();
//		if (newGroupingEntries.size() != baseChain.groupingEntries.size() + targetChain.groupingEntries.size())
			
//		System.exit(0);
//		EntryMatchRange<Integer, Integer> entryMatchRange = new EntryMatchRange<>(0, baseEntryChain.getTotalEntries() - 1, Integer.MIN_VALUE, 0);

		return new GroupingEntryChain(id, newGroupingEntries);
	}
	public int getID() {
		return id;
	}
	@Deprecated
	public void addNextEntry(GroupingEntryChain entryChain) {
		// Update incomingEntry and outgoingEntry first
		for (String key : entryChain.incomingEntry.groups.keySet())
			if (incomingEntry.groups.get(key) == null) {
				incomingEntry.groups.put(key, entryChain.incomingEntry.groups.get(key));
				incomingEntryIndex.put(key, 0);
			}
		for (String key : entryChain.outgoingEntry.groups.keySet())
			outgoingEntry.groups.put(key, entryChain.outgoingEntry.groups.get(key));
		
		// Connect into the entry chain
		this.groupingEntries.addAll(entryChain.groupingEntries);
		
//		//
//		outgoingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>());
//		for (GroupingEntry entry : groupingEntries) {
//			entry.groups.forEach((name,g) ->
//			outgoingEntry.groups.put(name, new SingleGroup(g.segment, g.orientation)));
//		}
//		incomingEntry = new GroupingEntry("Dummy", new LinkedHashMap<String, SingleGroup>());
//		for (int i = groupingEntries.size() - 1; i >= 0; i--) {
//			GroupingEntry entry = groupingEntries.get(i);
//			entry.groups.forEach((name,g) ->
//				incomingEntry.groups.put(name, new SingleGroup(g.segment, g.orientation)));
//		}
		
		
//		for (int i = 0; i < incomingEntry.groups.size(); i++)
//			if (incomingEntry.groups.get(i) == null)
//				if (entryChain.incomingEntry.groups.get(i) != null)
//					incomingEntry.groups.set(i, entryChain.incomingEntry.groups.get(i));
//		for (int i = 0; i < outgoingEntry.groups.size(); i++)
//			if (entryChain.outgoingEntry.groups.get(i) != null)
//				outgoingEntry.groups.set(i, entryChain.outgoingEntry.groups.get(i));
//		this.groupingEntries.addAll(entryChain.groupingEntries);
	}
	public GroupingEntry getFirstEntry() {
		return groupingEntries.get(0);
	}
	public GroupingEntry getLastEntry() {
		return groupingEntries.get(groupingEntries.size() - 1);
	}
	public int getTotalEntries() {
		return groupingEntries.size();
	}
	public List<GroupingEntry> getGroupingEntries() {
		return groupingEntries;
	}
	public Map<String, EntryMatchState> getConnectEntryMatchStates(GroupingEntryChain entryChain, int baseDirection, int targetDirection, List<String> marefs) {
		Map<String, EntryMatchState> entryMatchStates = new HashMap<>();
		GroupingEntry baseEntry = baseDirection==1?this.outgoingEntry:this.incomingEntry;
		GroupingEntry targetEntry = targetDirection==1?entryChain.incomingEntry:entryChain.outgoingEntry;
		baseEntry.groups.keySet().stream().filter(key -> targetEntry.groups.keySet().contains(key)).forEach(key -> {
//		for (String key : baseEntry.groups.keySet()) {
			SingleGroup baseGroup = baseEntry.groups.get(key);
			SingleGroup targetGroup = targetEntry.groups.get(key);
			if (targetGroup == null)
				return;
			int outIndex = baseDirection==1?this.outgoingEntryIndex.get(key):(this.getTotalEntries() - this.incomingEntryIndex.get(key) - 1);
			int inIndex = targetDirection==1?entryChain.incomingEntryIndex.get(key):(entryChain.getTotalEntries() - entryChain.outgoingEntryIndex.get(key) - 1);

			int baseOrientation = baseGroup.orientation * baseDirection;
			int targetOrientation = targetGroup.orientation * targetDirection;
			if ((baseOrientation == targetOrientation) 
					&& ((baseOrientation == 1 && baseGroup.segment == targetGroup.segment - 1) 
							|| (baseOrientation == -1 && baseGroup.segment == targetGroup.segment + 1))) {
				if (outIndex == this.getTotalEntries() - 1 && inIndex == 0)
					entryMatchStates.put(key, new EntryMatchState(outIndex, inIndex, EntryMatch.DirectMatch));
				else
					entryMatchStates.put(key, new EntryMatchState(outIndex, inIndex, EntryMatch.IndirectMatch));
			}
			else {
				if (marefs.contains(key))
					entryMatchStates.put(key, new EntryMatchState(outIndex, inIndex, EntryMatch.RefRearrangement));
				else
					entryMatchStates.put(key, new EntryMatchState(outIndex, inIndex, EntryMatch.NonRefRearrangement));
			}
		});
		return entryMatchStates;

	}
	// Update incomingEntry and outgoingEntry first
//	for (String key : entryChain.incomingEntry.groups.keySet())
//		if (incomingEntry.groups.get(key) == null) {
//			incomingEntry.groups.put(key, entryChain.incomingEntry.groups.get(key));
//			incomingEntryIndex.put(key, entryChain.incomingEntryIndex.get(key) + this.getTotalEntries());
//		}
//	for (String key : entryChain.outgoingEntry.groups.keySet()) {
//		outgoingEntry.groups.put(key, entryChain.outgoingEntry.groups.get(key));
//		outgoingEntryIndex.put(key, entryChain.outgoingEntryIndex.get(key) + this.getTotalEntries());
//	}

	private EntryConnection createEntryConnectionFromStates(GroupingEntryChain baseEntryChain, GroupingEntryChain targetEntryChain, int baseReverse, int targetReverse, Map<String, EntryMatchState> entryMatchStates) {
		int entryMatch = 0;
		int entryDirectMatch = 0;
		int entryRearrangement = 0;
		int entryRefRearrangement = 0;
		for (EntryMatchState state : entryMatchStates.values())
			switch (state.entryMatch) {
				case DirectMatch:
					entryDirectMatch++;
					entryMatch++;
					break;
				case IndirectMatch:
					entryMatch++;
					break;
				case RefRearrangement:
					entryRefRearrangement++;
					entryRearrangement++;
					break;
				case NonRefRearrangement:
					entryRearrangement++;
					break;
				default:
					assert false;
			}
		// Create range
		EntryMatchRange<Integer, Integer> entryMatchRange = new EntryMatchRange<>(0, baseEntryChain.getTotalEntries() - 1, Integer.MIN_VALUE, 0);
		List<EntryMatchRange<Integer, Integer>> entryMatchRanges = new ArrayList<>();
		entryMatchRanges.add(entryMatchRange);
		return new EntryConnection(baseEntryChain, targetEntryChain, baseReverse, targetReverse, entryMatchRanges, entryMatch, entryDirectMatch, entryRearrangement, entryRefRearrangement);
	}
	
//	private int includeChainIndex(Map<String, EntryMatchState> baseChainState, Map<String, EntryMatchState> targetChainState) {
//		int firstIndex = Integer.MAX_VALUE;
//		int lastIndex = -1;
//		for (String key : baseChainState.keySet()) {
//			if (baseChainState.get(key).entryMatch.isMatch()) {
//				if (lastIndex < baseChainState.get(key).outIndex)
//					lastIndex = baseChainState.get(key).outIndex;
//			}
//			else if (targetChainState.get(key).entryMatch.isMatch()) {
//				if (firstIndex > targetChainState.get(key).inIndex)
//					firstIndex = targetChainState.get(key).inIndex;
//			}
//			else { // Both are rearrangements. Cannot be included
//				return -1;
//			}
//		}
//		if (lastIndex < firstIndex)
//			return lastIndex + 1; // insertion point
//		return -1;
//
//	}
	
	
	private List<EntryMatchRange<Integer, Integer>> getEntryMergeRange(GroupingEntryChain baseChain, GroupingEntryChain targetChain, Map<String, EntryMatchState> baseChainState, Map<String, EntryMatchState> targetChainState) {
//		if (!(baseChain.id == 100 && targetChain.id == 95))
//			return null;
//		System.out.println();;
//		System.out.println(baseChain.id + "/" + targetChain.id);
		List<EntryMatchRange<Integer, Integer>> entryMatchRanges = new ArrayList<>();
		for (String key : baseChainState.keySet()) {
			ChainEntryProperty baseProperty = baseChain.entryProperties.get(key);
			ChainEntryProperty targetProperty = targetChain.entryProperties.get(key);
			
			if (baseChainState.get(key).entryMatch.isMatch()) {
//				entryMatchRanges.add(new EntryMatchRange<Integer, Integer>(baseProperty.inIndex, baseProperty.outIndex, targetProperty.outIndex + 1, Integer.MAX_VALUE));
				entryMatchRanges.add(new EntryMatchRange<Integer, Integer>(baseProperty.inIndex, baseProperty.outIndex, Integer.MIN_VALUE, targetProperty.inIndex));
//				if ((baseChain.id == 100 && targetChain.id == 95))
//					System.out.println(key + "\tTFF\t" + baseProperty.inIndex +"\t" + baseProperty.outIndex + "\t" +  Integer.MIN_VALUE + "\t" + targetProperty.inIndex);
				
			}
			else if (targetChainState.get(key).entryMatch.isMatch()) {
				entryMatchRanges.add(new EntryMatchRange<Integer, Integer>(baseProperty.inIndex, baseProperty.outIndex, targetProperty.outIndex + 1, Integer.MAX_VALUE));
//				entryMatchRanges.add(new EntryMatchRange<Integer, Integer>(baseProperty.inIndex, baseProperty.outIndex, Integer.MIN_VALUE, targetProperty.inIndex));
//				if ((baseChain.id == 100 && targetChain.id == 95))
//					System.out.println(key + "\tEFF\t" + baseProperty.inIndex +"\t" + baseProperty.outIndex + "\t" +  (targetProperty.outIndex + 1) + "\t" + Integer.MAX_VALUE);
				
			}
			else { // Both are rearrangements. Cannot be included 

				return null;
				
			}
		}
//		System.out.println("pass");
//		System.exit(0);
//		for (EntryMatchRange<Integer, Integer> range : entryMatchRanges)
//			System.out.println(baseChain.id + "/" + targetChain.id +":"+ range.min + "~" + range.max + "\t" + range.limitingRange.min + "~" + range.limitingRange.max);
		return EntryMatchRange.mergeEntryMatchRange(entryMatchRanges);
	}
	public EntryConnection getEntryConnection(GroupingEntryChain entryChain, boolean canReverseThis, List<String> marefs) {
		List<EntryConnection> entryConnections = new ArrayList<>();
		
//		Map<String, EntryMatchState> entryMatchesTFF = this.getConnectEntryMatchStates(entryChain, 1, 1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(this, entryChain, 1, 1, entryMatchesTFF));
//		
//		Map<String, EntryMatchState> entryMatchesTFR = this.getConnectEntryMatchStates(entryChain, 1, -1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(this, entryChain, 1, -1, entryMatchesTFR));
//		Map<String, EntryMatchState> entryMatchesERF = entryChain.getConnectEntryMatchStates(this, -1, 1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(entryChain, this, -1, 1, entryMatchesERF));
//		
////		Map<String, EntryMatchState> entryMatchesEFF = entryChain.getConnectEntryMatchStates(this, 1, 1, marefs);
////		entryConnections.add(createEntryConnectionFromStates(entryChain, this, 1, 1, entryMatchesEFF));
//
//		Map<String, EntryMatchState> entryMatchesEFF = this.getConnectEntryMatchStates(entryChain, -1, -1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(this, entryChain, -1, -1, entryMatchesEFF));
		Map<String, EntryMatchState> entryMatchesTFF = this.getConnectEntryMatchStates(entryChain, 1, 1, marefs);
		entryConnections.add(createEntryConnectionFromStates(this, entryChain, 1, 1, entryMatchesTFF));
		Map<String, EntryMatchState> entryMatchesEFF = entryChain.getConnectEntryMatchStates(this, 1, 1, marefs);		
		entryConnections.add(createEntryConnectionFromStates(entryChain, this, 1, 1, entryMatchesEFF));		
		Map<String, EntryMatchState> entryMatchesTFR = this.getConnectEntryMatchStates(entryChain, 1, -1, marefs);	
		entryConnections.add(createEntryConnectionFromStates(this, entryChain, 1, -1, entryMatchesTFR));
		Map<String, EntryMatchState> entryMatchesERF = entryChain.getConnectEntryMatchStates(this, -1, 1, marefs);	
		entryConnections.add(createEntryConnectionFromStates(entryChain, this, -1, 1, entryMatchesERF));
//		Map<String, EntryMatchState> entryMatchesERF = entryChain.getReverse().getConnectEntryMatchStates(this, 1, 1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(entryChain, this, -1, 1, entryMatchesERF));
		

//		Map<String, EntryMatchState> entryMatchesEFF = this.getReverse().getConnectEntryMatchStates(entryChain.getReverse(), 1, 1, marefs);
//		entryConnections.add(createEntryConnectionFromStates(this, entryChain, -1, -1, entryMatchesEFF));

		assert entryMatchesTFF.size() == entryMatchesEFF.size();
		assert entryMatchesTFF.size() == entryMatchesTFR.size();
		assert entryMatchesTFF.size() == entryMatchesERF.size();
		
		boolean allConnectionsHaveRearrangement = true;
		for (EntryConnection entryConnection : entryConnections)
			if (!entryConnection.hasRearrangement())
				allConnectionsHaveRearrangement = false;
		if (allConnectionsHaveRearrangement) {
			
//			int insertionIndex;
//			insertionIndex = includeChainIndex(entryMatchesTFF, entryMatchesEFF);
//			if (insertionIndex >= 0)
//				entryConnections.add(new EntryConnection(this, entryChain, 1, 1, insertionIndex, entryMatchesTFF.size(), 0, 0, 0)); // There should be no rearrangements. All match doesn't count as direct match
//			insertionIndex = includeChainIndex(entryMatchesEFF, entryMatchesTFF);
//			if (insertionIndex >= 0)
//				entryConnections.add(new EntryConnection(entryChain, this, 1, 1, insertionIndex, entryMatchesEFF.size(), 0, 0, 0)); // There should be no rearrangements. All match doesn't count as direct match
//			insertionIndex = includeChainIndex(entryMatchesTFR, entryMatchesERF);
//			if (insertionIndex >= 0)
//				entryConnections.add(new EntryConnection(this, entryChain, 1, -1, insertionIndex, entryMatchesTFR.size(), 0, 0, 0)); // There should be no rearrangements. All match doesn't count as direct match
//			insertionIndex = includeChainIndex(entryMatchesERF, entryMatchesTFR);
//			if (insertionIndex >= 0)
//				entryConnections.add(new EntryConnection(entryChain, this, -1, 1, insertionIndex, entryMatchesERF.size(), 0, 0, 0)); // There should be no rearrangements. All match doesn't count as direct match
			List<EntryMatchRange<Integer, Integer>> entryMatchRange;
			entryMatchRange = getEntryMergeRange(this, entryChain, entryMatchesTFF, entryMatchesEFF);
			if (entryMatchRange != null)
				entryConnections.add(new EntryConnection(this, entryChain, 1, 1, entryMatchRange, entryMatchesTFF.size(), 0, 0, 0));
			entryMatchRange = getEntryMergeRange(this, entryChain, entryMatchesTFR, entryMatchesERF);
			if (entryMatchRange != null)
				entryConnections.add(new EntryConnection(this, entryChain, 1, -1, entryMatchRange, entryMatchesTFR.size(), 0, 0, 0));

		}
		
		// Format 1 F-F 
		/*
		{
			int entryMatch = this.outgoingEntry.getConnectEntryMatches(entryChain.incomingEntry, 1, 1);
			int entryDirectMatch = this.getLastEntry().getConnectEntryMatches(entryChain.getFirstEntry(), 1, 1);		
			int entryRearrangement = this.outgoingEntry.getConnectEntryRearrangements(entryChain.incomingEntry, 1, 1);
			int refEntryRearrangement = this.outgoingEntry.getConnectReferenceEntryRearrangements(entryChain.incomingEntry, 1, 1, marefs);
			entryConnections.add(new EntryConnection(this, entryChain, 1, 1, this.getTotalEntries(), entryMatch, entryDirectMatch, entryRearrangement, refEntryRearrangement));
		}
		// Format 2 F-R
		{
			int entryMatch = this.outgoingEntry.getConnectEntryMatches(entryChain.outgoingEntry, 1, -1);
			int entryDirectMatch = this.getLastEntry().getConnectEntryMatches(entryChain.getLastEntry(), 1, -1);		
			int entryRearrangement = this.outgoingEntry.getConnectEntryRearrangements(entryChain.outgoingEntry, 1, -1);
			int refEntryRearrangement = this.outgoingEntry.getConnectReferenceEntryRearrangements(entryChain.outgoingEntry, 1, -1, marefs);
			entryConnections.add(new EntryConnection(this, entryChain, 1, -1, this.getTotalEntries(), entryMatch, entryDirectMatch, entryRearrangement, refEntryRearrangement));
		}
		// Format 3 R-F
		if (canReverseThis)
		{
			int entryMatch = this.incomingEntry.getConnectEntryMatches(entryChain.incomingEntry, -1, 1);
			int entryDirectMatch = this.getFirstEntry().getConnectEntryMatches(entryChain.getFirstEntry(), -1, 1);		
			int entryRearrangement = this.incomingEntry.getConnectEntryRearrangements(entryChain.incomingEntry, -1, 1);
			int refEntryRearrangement = this.incomingEntry.getConnectReferenceEntryRearrangements(entryChain.incomingEntry, -1, 1, marefs);
			entryConnections.add(new EntryConnection(this, entryChain, -1, 1, this.getTotalEntries(), entryMatch, entryDirectMatch, entryRearrangement, refEntryRearrangement));
		}
		
		// Format 4 R-R		
		if (canReverseThis)
		{
			int entryMatch = this.incomingEntry.getConnectEntryMatches(entryChain.outgoingEntry, -1, -1);
			int entryDirectMatch = this.getFirstEntry().getConnectEntryMatches(entryChain.getLastEntry(), -1, -1);		
			int entryRearrangement = this.incomingEntry.getConnectEntryRearrangements(entryChain.outgoingEntry, -1, -1);
			int refEntryRearrangement = this.incomingEntry.getConnectReferenceEntryRearrangements(entryChain.outgoingEntry, -1, -1, marefs);
			entryConnections.add(new EntryConnection(this, entryChain, -1, -1, this.getTotalEntries(), entryMatch, entryDirectMatch, entryRearrangement, refEntryRearrangement));
		}
		
		// This should be the same in Format 1 F-F in entryChain.getEntryConnection(this)
		entryConnections.forEach(e -> System.out.println(e.entryDirectMatch + ", " + e.entryMatch + ", " + e.entryRearrangement));
		*/
		return Collections.max(entryConnections);
	}
	
	public LinkedHashMap<String, Set<Integer>> getSegmentsFacingOutward() {
		LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = new LinkedHashMap<>();
		for (String key : incomingEntry.groups.keySet()) {
			SingleGroup g = incomingEntry.groups.get(key);
			Set<Integer> set = potentialMatchingSegments.get(key);
			if (set == null) {
				set = new HashSet<Integer>();
				potentialMatchingSegments.put(key, set);
			}
			set.add(g.segment);
		}
		for (String key : outgoingEntry.groups.keySet()) {
			SingleGroup g = outgoingEntry.groups.get(key);
			Set<Integer> set = potentialMatchingSegments.get(key);
			if (set == null) {
				set = new HashSet<Integer>();
				potentialMatchingSegments.put(key, set);
			}
			set.add(g.segment);
		}
		return potentialMatchingSegments;
	}

	public LinkedHashMap<String, Set<Integer>> getPotentialMatchingSegments() {
		LinkedHashMap<String, Set<Integer>> potentialMatchingSegments = new LinkedHashMap<>();
		for (String key : incomingEntry.groups.keySet()) {
			SingleGroup g = incomingEntry.groups.get(key);
			Set<Integer> set = potentialMatchingSegments.get(key);
			if (set == null) {
				set = new HashSet<Integer>();
				potentialMatchingSegments.put(key, set);
			}
			set.add(g.segment - g.orientation);
		}
		for (String key : outgoingEntry.groups.keySet()) {
			SingleGroup g = outgoingEntry.groups.get(key);
			Set<Integer> set = potentialMatchingSegments.get(key);
			if (set == null) {
				set = new HashSet<Integer>();
				potentialMatchingSegments.put(key, set);
			}
			set.add(g.segment + g.orientation);
		}
		return potentialMatchingSegments;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof GroupingEntryChain)
			return ((GroupingEntryChain) o).id == this.id;
		return false;
	}
	@Override
	public int hashCode() {
		return this.id;
	}
}

class ChainEntryProperty {
	int inIndex;
	int outIndex;
	SingleGroup inSegmentGroup;
	SingleGroup outSegmentGroup;
	public ChainEntryProperty(int inIndex, int outIndex, SingleGroup inSegmentGroup, SingleGroup outSegmentGroup) {
		super();
		this.inIndex = inIndex;
		this.outIndex = outIndex;
		this.inSegmentGroup = inSegmentGroup;
		this.outSegmentGroup = outSegmentGroup;
	}
	public ChainEntryProperty getReverse(int chainSize) {
		return new ChainEntryProperty(chainSize - outIndex - 1, chainSize - inIndex - 1, new SingleGroup(outSegmentGroup.segment, outSegmentGroup.orientation * -1), new SingleGroup(inSegmentGroup.segment, inSegmentGroup.orientation * -1));
	}
	
}
class EntryMatchState {
	int outIndex;
	int inIndex;
	EntryMatch entryMatch;
	public EntryMatchState(int outIndex, int inIndex, EntryMatch entryMatch) {
		this.outIndex = outIndex;
		this.inIndex = inIndex;
		this.entryMatch = entryMatch;
	}
}
class EntryMatchRange<R extends Number & Comparable<R>, W extends Number & Comparable<W>> extends UnweightedRange<R> {
	UnweightedRange<W> limitingRange;
	public EntryMatchRange(UnweightedRange<R> baseRange, UnweightedRange<W> limitRange) {
		super(baseRange);
		this.limitingRange = new UnweightedRange<W>(limitRange);
	}
	public EntryMatchRange(R baseMin, R baseMax, W limitMin, W limitMax) {
		super(baseMin, baseMax);
		this.limitingRange = new UnweightedRange<W>(limitMin, limitMax);
	}
	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>> List<EntryMatchRange<R, W>> mergeEntryMatchRange(List<EntryMatchRange<R, W>> ranges) {
		if (ranges.isEmpty())
			return new ArrayList<EntryMatchRange<R, W>>(ranges);
		
		List<EntryMatchRange<R, W>> newRanges = new ArrayList<EntryMatchRange<R, W>>();

		// Initial list is sorted according to min
		Collections.sort(ranges, UnweightedRange.minComparator());
		PriorityQueue<EntryMatchRange<R, W>> removeItems = new PriorityQueue<EntryMatchRange<R, W>>(UnweightedRange.maxComparator());
		PriorityQueue<W> limitingMin = new PriorityQueue<W>(Comparator.reverseOrder()); // We want the maximum of limiting min
		PriorityQueue<W> limitingMax = new PriorityQueue<W>(); // We want the minimum of limiting max
		PriorityQueue<W> limitingMinToRemove = new PriorityQueue<W>(Comparator.reverseOrder()); // We want the maximum of limiting min
		PriorityQueue<W> limitingMaxToRemove = new PriorityQueue<W>(); // We want the minimum of limiting max
		Class<R> rClass = (Class<R>) ranges.get(0).min.getClass();
		R previousIndex = NumberOperation.<R>getNumber(rClass, 0);

		EntryMatchRange<R, W> range;
		Iterator<EntryMatchRange<R, W>> rangeIterator = ranges.iterator();
		while (rangeIterator.hasNext() || limitingMax.size() > 0 || limitingMin.size() > 0) {
			if (rangeIterator.hasNext())
				range = rangeIterator.next();
			else
				range = null;
			while (!removeItems.isEmpty() && (range == null || removeItems.peek().max.compareTo(range.min) < 0)) {
				EntryMatchRange<R, W> currentItem = removeItems.peek();
				if (limitingMin.peek().compareTo(limitingMax.peek()) > 0) // At some point we can no longer place a valid limitingRange
					return null;
				newRanges.add(new EntryMatchRange<R, W>(previousIndex, currentItem.max, limitingMin.peek(), limitingMax.peek()));
				while (!removeItems.isEmpty() && removeItems.peek().max.compareTo(currentItem.max) == 0) {
					UnweightedRange<W> limitingRange = removeItems.poll().limitingRange;
					limitingMinToRemove.add(limitingRange.min);
					limitingMaxToRemove.add(limitingRange.max);
				}
				while (!limitingMinToRemove.isEmpty() && limitingMinToRemove.peek().compareTo(limitingMin.peek()) == 0) {
					limitingMinToRemove.poll();
					limitingMin.poll();
				}
				while (!limitingMaxToRemove.isEmpty() && limitingMaxToRemove.peek().compareTo(limitingMax.peek()) == 0) {
					limitingMaxToRemove.poll();
					limitingMax.poll();
				}
				previousIndex = NumberOperation.addition(currentItem.max, NumberOperation.<R>getNumber(rClass, 1));;
			}
			if (range != null) {
				limitingMin.add(range.limitingRange.min);
				limitingMax.add(range.limitingRange.max);
				removeItems.add(range);
			}
		}
		return newRanges;	
//				if (range.overlap(workingBaseRange)) {
//					UnweightedRange<R> newBaseRange = UnweightedRange.getRangeUnion(workingBaseRange, range);
//					UnweightedRange<W> newLimitingRange = UnweightedRange.getRangeIntersection(workingLimitingRange, range.limitingRange);
//					if (newLimitingRange == null)
//						return null;
//					workingBaseRange = newBaseRange;
//					workingLimitingRange = newLimitingRange;
//				}
//				else {
//					newRanges.add(new EntryMatchRange<R, W>(workingBaseRange, workingLimitingRange));
//					workingBaseRange = range;
//					if (workingLimitingRange.min.compareTo(range.limitingRange.max) > 0)
//						return null;
//					workingLimitingRange = new UnweightedRange<W>(workingLimitingRange.min.compareTo(range.limitingRange.min) > 0? workingLimitingRange.min : range.limitingRange.min, range.limitingRange.max);
//				}
//			}
//			else {
//				workingBaseRange = range;
//				workingLimitingRange = range.limitingRange;
//			}
//		}
//		newRanges.add(new EntryMatchRange<R, W>(workingBaseRange, workingLimitingRange));
//		return newRanges;
	}
//	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>> List<EntryMatchRange<R, W>> mergeEntryMatchRange(List<EntryMatchRange<R, W>> ranges) {
//		if (ranges.isEmpty())
//			return new ArrayList<EntryMatchRange<R, W>>(ranges);
//		
//		List<EntryMatchRange<R, W>> newRanges = new ArrayList<EntryMatchRange<R, W>>();
//
//		// Initial list is sorted according to min
//		Collections.sort(ranges, UnweightedRange.minComparator());
//
//		UnweightedRange<R> workingBaseRange = null;
//		UnweightedRange<W> workingLimitingRange = null;
//		for (EntryMatchRange<R, W> range : ranges) {
//			if (workingBaseRange != null) {
//				if (range.overlap(workingBaseRange)) {
//					UnweightedRange<R> newBaseRange = UnweightedRange.getRangeUnion(workingBaseRange, range);
//					UnweightedRange<W> newLimitingRange = UnweightedRange.getRangeIntersection(workingLimitingRange, range.limitingRange);
//					if (newLimitingRange == null)
//						return null;
//					workingBaseRange = newBaseRange;
//					workingLimitingRange = newLimitingRange;
//				}
//				else {
//					newRanges.add(new EntryMatchRange<R, W>(workingBaseRange, workingLimitingRange));
//					workingBaseRange = range;
//					if (workingLimitingRange.min.compareTo(range.limitingRange.max) > 0)
//						return null;
//					workingLimitingRange = new UnweightedRange<W>(workingLimitingRange.min.compareTo(range.limitingRange.min) > 0? workingLimitingRange.min : range.limitingRange.min, range.limitingRange.max);
//				}
//			}
//			else {
//				workingBaseRange = range;
//				workingLimitingRange = range.limitingRange;
//			}
//		}
//		newRanges.add(new EntryMatchRange<R, W>(workingBaseRange, workingLimitingRange));
//		return newRanges;
//	}

}

//public void addChain(GroupingEntryChain entryChain, int insertionIndex) {
//
//	for (String key : entryChain.incomingEntry.groups.keySet())
//		if (!incomingEntryIndex.containsKey(key)
//				|| (incomingEntryIndex.get(key) >= insertionIndex)) {
//				incomingEntry.groups.put(key, entryChain.incomingEntry.groups.get(key));
//				incomingEntryIndex.put(key, entryChain.incomingEntryIndex.get(key) + insertionIndex);
//		}
//	for (String key : entryChain.outgoingEntry.groups.keySet()) {
//		if (!outgoingEntryIndex.containsKey(key)
//				|| (outgoingEntryIndex.get(key) < insertionIndex)) {
//					outgoingEntry.groups.put(key, entryChain.outgoingEntry.groups.get(key));
//					outgoingEntryIndex.put(key, entryChain.outgoingEntryIndex.get(key) + insertionIndex);				
//		}
//	}
//		
//	this.groupingEntries.addAll(insertionIndex, entryChain.groupingEntries);
//}
