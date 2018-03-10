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

import java.util.List;

public class EntryConnection implements Comparable<EntryConnection> {
	public final GroupingEntryChain baseEntryChain;
	public final GroupingEntryChain targetEntryChain;
	public final int baseReverse;
	public final int targetReverse;
	public final List<EntryMatchRange<Integer, Integer>> entryMatchRanges;
	public final int entryMatch;
	public final int entryDirectMatch;
	public final int entryRearrangement;
	public final int referenceRearrangement;
	
	
	public EntryConnection(GroupingEntryChain baseEntryChain, GroupingEntryChain targetEntryChain, int baseReverse, int targetReverse, List<EntryMatchRange<Integer, Integer>> entryMatchRanges,
			int entryMatch, int entryDirectMatch, int entryRearrangement, int referenceRearrangement) {
		this.baseEntryChain = baseEntryChain;
		this.targetEntryChain = targetEntryChain;
		this.baseReverse = baseReverse;
		this.targetReverse = targetReverse;
		this.entryMatchRanges = entryMatchRanges;
		this.entryMatch = entryMatch;
		this.entryDirectMatch = entryDirectMatch;
		this.entryRearrangement = entryRearrangement;
		this.referenceRearrangement = referenceRearrangement;
	}
	
	public boolean hasEntryMatch() {
		return entryMatch > 0;
	}
	public boolean hasDirectEntryMatch() {
		return entryDirectMatch > 0;
	}
	public boolean hasRearrangement() {
		return entryRearrangement > 0;
	}
	public boolean hasRefRearrangement() {
		return referenceRearrangement > 0;
	}
	@Override
	public int compareTo(EntryConnection entryConnection) {
		// Criteria 1: Better to have at least one entry match
		if (this.hasEntryMatch() && !entryConnection.hasEntryMatch())
			return 1;
		if (!this.hasEntryMatch() && entryConnection.hasEntryMatch())
			return -1;
		
		// Criteria 2: Better to have no rearrangements
		if (this.hasRearrangement() && !entryConnection.hasRearrangement())
			return -1;
		if (!this.hasRearrangement() && entryConnection.hasRearrangement())
			return 1;
		

		// Criteria 3: If no rearrangements occur, better to have fewer non-direct entry matches, followed by more direct entry matches, followed by more entry matches
		if (!this.hasRearrangement() && !entryConnection.hasRearrangement()) {
			int x = Integer.compare(this.entryMatch - this.entryDirectMatch, entryConnection.entryMatch - entryConnection.entryDirectMatch) * -1;
			if (x != 0) return x;
			x = Integer.compare(this.entryDirectMatch, entryConnection.entryDirectMatch);
			if (x != 0) return x;
			x = Integer.compare(this.entryMatch, entryConnection.entryMatch);
			if (x != 0) return x;
		}
		else { 
			//If rearrangements occurs...
			
			// Criteria 4: when using a reference for multiple alignment, better to have no reference rearrangements
			if (this.hasRefRearrangement() && !entryConnection.hasRefRearrangement())
				return -1;
			if (!this.hasRefRearrangement() && entryConnection.hasRefRearrangement())
				return 1;

			// Criteria 5: minimize the number of rearrangements
			int x = Integer.compare(this.entryRearrangement, entryConnection.entryRearrangement) * -1;
			if (x != 0) return x;
			
			// Criteria 6: maixmize the number of matches, followed by more direct entry matches
			x = Integer.compare(this.entryMatch, entryConnection.entryMatch);
			if (x != 0) return x;
			x = Integer.compare(this.entryDirectMatch, entryConnection.entryDirectMatch);
			if (x != 0) return x;

		}
		// If the score is the same, just return 0
		return 0;
		
		// If the score is the same, just return the comparison of entry chain IDs
//		int x = Integer.compare(this.baseEntryChain.id, entryConnection.baseEntryChain.id);
//		if (x != 0)	return x;
//		return Integer.compare(this.targetEntryChain.id, entryConnection.targetEntryChain.id);
		
	}
	
	/*
	public final GroupingEntryChain incomingEntryChain;
	public final GroupingEntryChain outgoingEntryChain;
	int incomingReverse;
	int outgoingReverse;
	
	int entryMatch;
	int entryDirectMatch;
	int entryRearrangement;

	// require the reference to match
	int referenceRearrangement;
	
	public EntryConnection(GroupingEntryChain incomingEntryChain, GroupingEntryChain outgoingEntryChain, int incomingReverse, int outgoingReverse, int entryMatch, int entryDirectMatch,
			int entryRearrangement, int referenceRearrangement) {
		super();
		this.incomingEntryChain = incomingEntryChain;
		this.outgoingEntryChain = outgoingEntryChain;
		this.incomingReverse = incomingReverse;
		this.outgoingReverse = outgoingReverse;
		this.entryMatch = entryMatch;
		this.entryDirectMatch = entryDirectMatch;
		this.entryRearrangement = entryRearrangement;
		this.referenceRearrangement = referenceRearrangement;
	}
	
	public boolean hasEntryMatch() {
		return entryMatch > 0;
	}
	public boolean hasRearrangement() {
		return entryRearrangement > 0;
	}
	public boolean hasRefRearrangement() {
		return referenceRearrangement > 0;
	}
	@Override
	public int compareTo(EntryConnection entryConnection) {
		// Criteria 1: Better to have at least one entry match
		if (this.hasEntryMatch() && !entryConnection.hasEntryMatch())
			return 1;
		if (!this.hasEntryMatch() && entryConnection.hasEntryMatch())
			return -1;
		
		// Criteria 2: Better to have no rearrangements
		if (this.hasRearrangement() && !entryConnection.hasRearrangement())
			return -1;
		if (!this.hasRearrangement() && entryConnection.hasRearrangement())
			return 1;
		

		// Criteria 3: If no rearrangements occur, better to have more direct entry matches, followed by more entry matches
		if (!this.hasRearrangement() && !entryConnection.hasRearrangement()) {
			int x = Integer.compare(this.entryDirectMatch, entryConnection.entryDirectMatch);
			if (x != 0) return x;
			x = Integer.compare(this.entryMatch, entryConnection.entryMatch);
			if (x != 0) return x;
		}
		else { 
			//If rearrangements occurs...
			
			// Criteria 4: when using a reference for multiple alignment, better to have no reference rearrangements
			if (this.hasRefRearrangement() && !entryConnection.hasRefRearrangement())
				return -1;
			if (!this.hasRefRearrangement() && entryConnection.hasRefRearrangement())
				return 1;

			// Criteria 5: minimize the number of rearrangements
			int x = Integer.compare(this.entryRearrangement, entryConnection.entryRearrangement) * -1;
			if (x != 0) return x;
			
			// Criteria 6: maixmize the number of matches, followed by more direct entry matches
			x = Integer.compare(this.entryMatch - 2 * this.entryRearrangement, entryConnection.entryMatch - 2 * entryConnection.entryRearrangement);
			if (x != 0) return x;
			x = Integer.compare(this.entryDirectMatch, entryConnection.entryDirectMatch);
			if (x != 0) return x;
//			x = Integer.compare(this.entryMatch - 2 * this.entryRearrangement, entryConnection.entryMatch - 2 * entryConnection.entryRearrangement);
//			if (x != 0) return x;
//			x = Integer.compare(this.entryDirectMatch, entryConnection.entryDirectMatch);
//			if (x != 0) return x;
//			System.out.println("CHAIN " + this.entryRearrangement + "\t" + this.incomingEntryChain.id + "\t" + entryConnection.incomingEntryChain.id + "\t" + this.outgoingEntryChain.id + "\t" +  entryConnection.outgoingEntryChain.id);

		}
		// If the score is the same, just return 0
//		return 0;
		
		// If the score is the same, just return the comparison of entry chain IDs
//		System.out.println("CHAIN " + this.entryRearrangement + "\t" + this.incomingEntryChain.id + "\t" + entryConnection.incomingEntryChain.id + "\t" + this.outgoingEntryChain.id + "\t" +  entryConnection.outgoingEntryChain.id);
		int x = Integer.compare(this.incomingEntryChain.id, entryConnection.incomingEntryChain.id);
		if (x != 0)	return x;
		return Integer.compare(this.outgoingEntryChain.id, entryConnection.outgoingEntryChain.id);
//		
		
//		x = Integer.compare(this.entryRearrangement, entryConnection.entryMatch);
//		int x = Integer.compare(this.penalty.penalty, entryConnection.penalty.penalty);
//		if (x != 0)
//			return x;
//		else {
//			int y = Integer.compare(this.incomingEntryChain.id, entryConnection.incomingEntryChain.id);
//			if (y != 0)
//				return y;
//			else
//				return Integer.compare(this.outgoingEntryChain.id, entryConnection.outgoingEntryChain.id);
//		}
	}
	*/
}
