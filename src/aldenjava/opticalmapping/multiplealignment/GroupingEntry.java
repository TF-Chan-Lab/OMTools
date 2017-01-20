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


package aldenjava.opticalmapping.multiplealignment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
/**
 * GroupingEntry represents a collinear block, which contains none or one segment per query
 * @author Alden
 *
 */
public class GroupingEntry {
	public final String name;
//	final List<SingleGroup> groups;
	LinkedHashMap<String, SingleGroup> groups;
//	GroupingEntry directLastEntry = null;
	public GroupingEntry(String name, LinkedHashMap<String, SingleGroup> groups) {
		super();
		this.name = name;
		this.groups = groups;
	}


	public boolean canMerge(GroupingEntry entry) {
		if (this.getTotalNonEmtpySegments() != entry.getTotalNonEmtpySegments())
			return false;
		
		for (String key : groups.keySet()) {
			SingleGroup thisGroup = this.groups.get(key);
			SingleGroup entryGroup = entry.groups.get(key);
			if (entryGroup == null)
				return false;
			if (thisGroup.orientation != entryGroup.orientation)
				return false;
			if (thisGroup.segment != entryGroup.segment - thisGroup.orientation)
				return false;
		}
		return true;
		
//		for (int i = 0; i < groups.size(); i++) {
//			if (this.groups.get(i) == null) {
//				if (entry.groups.get(i) != null)
//					return false;
//			}
//			else 
//				if (entry.groups.get(i) == null)
//					return false;
//				else {
//					if (this.groups.get(i).orientation != entry.groups.get(i).orientation)
//						return false;
//					else
//						if (this.groups.get(i).segment != entry.groups.get(i).segment - this.groups.get(i).orientation) 
//							return false;
//				}
//		}
//		return true;
	}

	public int getTotalNonEmtpySegments() {
		return groups.size();
	}


//	public void combine(GroupingEntry entry) {
//		for (int i = 0; i < groups.size(); i++) {
//			if (entry.groups.get(i) == null)
//				continue;
////			if (this.groups.get(i) == null)
//			this.groups.set(i, entry.groups.get(i));
//		}
////		directLastEntry = entry;
//	}

	public int getConnectEntryMatches(GroupingEntry entry, int reverseThis, int reverseEntry) {
		int match = 0;
		for (String key : this.groups.keySet()) {
			SingleGroup thisGroup = this.groups.get(key);
			SingleGroup entryGroup = entry.groups.get(key);
			if (entryGroup == null)
				continue;
			int thisOrientation = thisGroup.orientation * reverseThis;
			int entryOrientation = entryGroup.orientation * reverseEntry;
			if ((thisOrientation == entryOrientation) 
					&& ((thisOrientation == 1 && thisGroup.segment == entryGroup.segment - 1) 
							|| (thisOrientation == -1 && thisGroup.segment == entryGroup.segment + 1))) {
				match++;
			}
		}
//		for (int i = 0; i < groups.size(); i++) {
//			if (this.groups.get(i) == null || entry.groups.get(i) == null)
//				continue;
//			int thisOrientation = this.groups.get(i).orientation * reverseThis;
//			int entryOrientation = entry.groups.get(i).orientation * reverseEntry;
//			if ((thisOrientation == entryOrientation) 
//					&& ((thisOrientation == 1 && this.groups.get(i).segment == entry.groups.get(i).segment - 1) 
//							|| (thisOrientation == -1 && this.groups.get(i).segment == entry.groups.get(i).segment + 1))) {
//				match++;
//			}
//		}
		return match;
	}
	public int getConnectEntryRearrangements(GroupingEntry entry, int reverseThis, int reverseEntry) {
		int rearrangment = 0;
		for (String key : this.groups.keySet()) {
			SingleGroup thisGroup = this.groups.get(key);
			SingleGroup entryGroup = entry.groups.get(key);
			if (entryGroup == null)
				continue;
			int thisOrientation = thisGroup.orientation * reverseThis;
			int entryOrientation = entryGroup.orientation * reverseEntry;
			if ((thisOrientation != entryOrientation) 
					|| ((thisOrientation== 1 && thisGroup.segment != entryGroup.segment - 1) 
							|| (thisOrientation == -1 && thisGroup.segment != entryGroup.segment + 1))) {
				rearrangment++;
			}
		}

//		for (int i = 0; i < groups.size(); i++) {
//			if (this.groups.get(i) == null || entry.groups.get(i) == null)
//				continue;
//			int thisOrientation = this.groups.get(i).orientation * reverseThis;
//			int entryOrientation = entry.groups.get(i).orientation * reverseEntry;
//			if ((thisOrientation != entryOrientation) 
//					|| ((thisOrientation== 1 && this.groups.get(i).segment != entry.groups.get(i).segment - 1) 
//							|| (thisOrientation == -1 && this.groups.get(i).segment != entry.groups.get(i).segment + 1))) {
//				rearrangment++;
//			}
//		}
		return rearrangment;
	}
	
	
	
	public GroupingEntry getReverse(String name) {
//		List<SingleGroup> newGroups = new ArrayList<>();
//		for (SingleGroup group : groups) {
//			if (group != null)
//				newGroups.add(new SingleGroup(group.segment, group.orientation * -1));
//			else
//				newGroups.add(null);
//		}
		LinkedHashMap<String, SingleGroup> newGroups = new LinkedHashMap<>();
		for (Entry<String, SingleGroup> e : groups.entrySet()) {
			String key = e.getKey();
			SingleGroup group = e.getValue();
			newGroups.put(key, new SingleGroup(group.segment, group.orientation * -1));
		}
		return new GroupingEntry(name, newGroups);
	}
	
	/*
	public boolean canConnect(GroupingEntry entry) {
		assert this.groups.size() == entry.groups.size();
//		System.out.println(this.getOutputString());
//		System.out.println(entry.getOutputString());
		for (int i = 0; i < groups.size(); i++) {
			if (this.groups.get(i) == null || entry.groups.get(i) == null)
				continue;
			
			if (this.groups.get(i).segment != entry.groups.get(i).segment - 1) {
//				System.out.println(this.groups.get(i).segment != entry.groups.get(i).segment - 1);
				return false;
			}
		}
		return true;
	}
	*/
	
//	public boolean isStart() {
//		for (int i = 0; i < groups.size(); i++) {
//			if (this.groups.get(i) == null)
//				continue;
//			if (this.groups.get(i).segment != 1)
//				return false;
//		}
//		return true;
//	}
	// This will lead to Comparison method violates its general contract
//	public int getComparison(GroupingEntry entry) {
//		assert this.groups.size() == entry.groups.size();
//		for (int i = 0; i < groups.size(); i++) {
//			if (this.groups.get(i) == null || entry.groups.get(i) == null)
//				continue;
//			if (this.groups.get(i).segment > entry.groups.get(i).segment)
//				return 1;
//			if (this.groups.get(i).segment < entry.groups.get(i).segment)
//				return -1;
//		}
//		return 0;
//	}
	
	public String getOutputString(List<String> orders) {
		String s = name;
		for (String key : orders) {
			SingleGroup g = groups.get(key);
			if (g != null) {
				if (g.orientation == 1)
					s += "\t" + (g.segment - 1) + "-" + (g.segment) + 'F';
				else
					s += "\t" + (g.segment) + "-" + (g.segment - 1) + 'R';
			}
			else
				s += "\t";
		}
//		for (SingleGroup g : groups) 
//			if (g != null) {
//				if (g.orientation == 1)
//					s += "\t" + (g.segment - 1) + "-" + (g.segment) + 'F';
//				else
//					s += "\t" + (g.segment) + "-" + (g.segment - 1) + 'R';
//			}
//			else
//				s += "\t";
		return s;
	}
	
	
	public static String getOutputString(List<String> orders, GroupingEntry startingEntry, GroupingEntry stoppingEntry, String name) {
		String s = name;
		for (String key : orders) {
			SingleGroup g1 = startingEntry.groups.get(key);
			SingleGroup g2 = stoppingEntry.groups.get(key);
			if (g1 != null) {
				assert g2 != null;
				assert g1.orientation == g2.orientation;
				if (g1.orientation == 1)
					s += "\t" + (g1.segment - 1) + "-" + (g2.segment) + 'F';
				else
					s += "\t" + (g1.segment) + "-" + (g2.segment - 1) + 'R';
			}
			else
				s += "\t";
		}

			
//		for (int i = 0; i < startingEntry.groups.size(); i++) {
//			SingleGroup g1 = startingEntry.groups.get(i);
//			SingleGroup g2 = stoppingEntry.groups.get(i);
//			
//			if (g1 != null) {
//				assert g2 != null;
//				assert g1.orientation == g2.orientation;
//				if (g1.orientation == 1)
//					s += "\t" + (g1.segment - 1) + "-" + (g2.segment) + 'F';
//				else
//					s += "\t" + (g1.segment) + "-" + (g2.segment - 1) + 'R';
//			}
//			else
//				s += "\t";
//		}

		return s;
	}

}

//public int getConnectPenalty(GroupingEntry entry) {
//int penalty = 0;
//boolean error = false;
//boolean pass = false;
//assert this.groups.size() == entry.groups.size();
//for (int i = 0; i < groups.size(); i++) {
//	if (this.groups.get(i) == null || entry.groups.get(i) == null)
//		continue;
//	
//	if ((this.groups.get(i).orientation != entry.groups.get(i).orientation) || (this.groups.get(i).orientation == 1 && this.groups.get(i).segment != entry.groups.get(i).segment - 1) || (this.groups.get(i).orientation == -1 && this.groups.get(i).segment != entry.groups.get(i).segment + 1)) {
////		penalty += 1000;
//		penalty += 2;
//		error = true;
//	}
//	else {
//		if (directLastEntry != null && directLastEntry.groups.get(i) != null)
//			penalty -= 2;
//		else
//			penalty--;
//		pass = true;
//	}
//}
//if (error) penalty += 1000;
//if (!pass) penalty += 100000;
//return penalty;
//
//}
