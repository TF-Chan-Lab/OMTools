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


package aldenjava.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class WeightedRange<R extends Number & Comparable<R>, W extends Number & Comparable<W>> extends UnweightedRange<R> {
	
	public W weight;
	public WeightedRange(R min, R max, W weight) {
		super(min, max);
		if (!weightClassSupported(weight))
			throw new IllegalArgumentException(min.getClass().getName() + " is not supported.");
		this.weight = weight;
	}
	public WeightedRange(UnweightedRange<R> range, W weight) {
		this(range.min, range.max, weight);
	}
	public W getWeight() {
		return weight;
	}
	public void setWeight(W weight) {
		this.weight = weight;
	}
	
	private boolean weightClassSupported(W w) {
		if (w instanceof Byte)
			return true;
		if (w instanceof Short)
			return true;
		if (w instanceof Integer)
			return true;
		if (w instanceof Long)
			return true;
		if (w instanceof Float)
			return true;
		if (w instanceof Double)
			return true;
		return false;
	}

	public boolean canStitch(WeightedRange<R, W> range) {
		if (super.canStitch(range))
			if (this.weight.compareTo(range.weight) == 0)
				return true;
		return false;
	}
	
	@Override
	public String toString() {
		return (min.toString() + "-" + max.toString() + ":" + weight.toString());
	}
	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>> List<WeightedRange<R, W>> stitchWeightedRange(List<WeightedRange<R, W>> ranges) {
		List<WeightedRange<R, W>> newRanges = new ArrayList<>();
		WeightedRange<R, W> previousRange = null;
		for (WeightedRange<R, W> range : ranges) {
			if (previousRange != null) {
				if (previousRange.canStitch(range))
					previousRange = new WeightedRange<R, W>(previousRange.min, range.max, range.weight);
				else {
					newRanges.add(previousRange);
					previousRange = range;
				}
			}
			else
				previousRange = range;
		}
		if (previousRange != null)
			newRanges.add(previousRange);
		return newRanges;
	}
	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>>  List<WeightedRange<R, W>> mergeWeightedRange(List<WeightedRange<R, W>> ranges) {
		if (ranges.isEmpty())
			return new ArrayList<WeightedRange<R, W>>(ranges);
		
		List<WeightedRange<R, W>> newRanges = new ArrayList<WeightedRange<R, W>>();
		// Initial list is sorted according to min
		Collections.sort(ranges, UnweightedRange.minComparator());
		PriorityQueue<WeightedRange<R, W>> workingRanges = new PriorityQueue<>(UnweightedRange.maxComparator());
		
		Class<R> rClass = (Class<R>) ranges.get(0).min.getClass();
		Class<W> wClass = (Class<W>) ranges.get(0).weight.getClass();
//		R lastStopPos = NumberOperation.subtraction(ranges.get(0).min, NumberOperation.<R>getNumber(rClass, 1));
		R lastStopPos = null;
		int nextRangeIndex = 0;
		while (!workingRanges.isEmpty() || nextRangeIndex < ranges.size()) {
			if (workingRanges.isEmpty()) {
				// Add new workingRange to the empty workingRanges
				
				// Update the last position
				lastStopPos = NumberOperation.subtraction(ranges.get(nextRangeIndex).min, NumberOperation.<R>getNumber(rClass, 1));
				// Add to workingRanges
				workingRanges.add(ranges.get(nextRangeIndex));
				nextRangeIndex++;
			}
			else {
				if (nextRangeIndex < ranges.size() && ranges.get(nextRangeIndex).min.compareTo(workingRanges.peek().max) <= 0) {
					// If a new working location overlap the first working locations, add this new working location and create a merged loc from last position to the minimum of the new working location
					R start = NumberOperation.addition(lastStopPos, NumberOperation.<R>getNumber(rClass, 1));
					R stop = NumberOperation.subtraction(ranges.get(nextRangeIndex).min, NumberOperation.<R>getNumber(rClass, 1));
					W weight = NumberOperation.<W>getNumber(wClass, 0);
					for (WeightedRange<R, W> workingRange : workingRanges)
						weight = NumberOperation.addition(weight, workingRange.weight);
					
					if (stop.compareTo(start) >= 0) {
						// Valid location: If the new working location shares the same min as first.min, it will become a zero-length loc and we don't add this 
						newRanges.add(new WeightedRange<R, W>(start, stop, weight));
						lastStopPos = stop;
					}
					else {
						assert stop.compareTo(NumberOperation.subtraction(start, NumberOperation.<R>getNumber(rClass, 1))) == 0;
					}
					workingRanges.add(ranges.get(nextRangeIndex));
					nextRangeIndex++;
				}
				else {
					// No new working location
					R start = NumberOperation.addition(lastStopPos, NumberOperation.<R>getNumber(rClass, 1));
					R stop = workingRanges.peek().max;
					W weight = NumberOperation.<W>getNumber(wClass, 0);
					for (WeightedRange<R, W> workingRange : workingRanges)
						weight = NumberOperation.addition(weight, workingRange.weight);
					assert stop.compareTo(start) >= 0;
					newRanges.add(new WeightedRange<R, W>(start, stop, weight));
					lastStopPos = stop;
				}
				// Remove all completed workingLoc
				while (!workingRanges.isEmpty() && workingRanges.peek().max.compareTo(lastStopPos) <= 0)
					workingRanges.poll();
			}
		}
		
		// Stitching
		newRanges = stitchWeightedRange(newRanges);
		
		return newRanges;
	}

	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>> List<WeightedRange<R, W>> assignWeightToUweightedRange(List<UnweightedRange<R>> ranges, W weight) {
		List<WeightedRange<R, W>> newRanges = new ArrayList<>();
		for (UnweightedRange<R> range : ranges)
			newRanges.add(new WeightedRange<R, W>(range, weight));
		return newRanges;
	}
	public static <R extends Number & Comparable<R>, W extends Number & Comparable<W>> List<UnweightedRange<R>> toUnweightedRange(List<WeightedRange<R, W>> ranges) {
		List<UnweightedRange<R>> newRanges = new ArrayList<>();
		for (WeightedRange<R, W> range : ranges)
			newRanges.add(new UnweightedRange<R>(range.min, range.max));
		return newRanges;
	}

}
/*	



	public int indicator;
	public ExtendedLongLocation(long x1, long x2, int indicator) {
		super(x1, x2);
		this.indicator = indicator;
		// TODO Auto-generated constructor stub
	}

	public ExtendedLongLocation(SimpleLongLocation s, int indicator) {
		super(s);
		this.indicator = indicator;
		// TODO Auto-generated constructor stub
	}
	public ExtendedLongLocation(ExtendedLongLocation e)
	{
		this(e.min, e.max, e.indicator);
	}
	public static List<ExtendedLongLocation> combineExtendLocationList(List<ExtendedLongLocation> loclist1, List<ExtendedLongLocation> loclist2) // merged and sorted list by minC
	{
		List<ExtendedLongLocation> newloclist = new ArrayList<ExtendedLongLocation>();
		int locsource = 0;
		int p1 = 0;
		int p2 = 0;
		ExtendedLongLocation loc = null;
		while ((p1 < loclist1.size() || locsource == 1) && (p2 < loclist2.size() || locsource == 2))
		{
			switch (locsource)
			{
				case 0: 
				{
					if (loclist1.get(p1).min < loclist2.get(p2).min)
					{
						locsource = 1;
						loc = new ExtendedLongLocation(loclist1.get(p1));
						p1++;
					}
					else if (loclist1.get(p1).min > loclist2.get(p2).min)
					{
						locsource = 2;
						loc = new ExtendedLongLocation(loclist2.get(p2));
						p2++;
					}
					else
					{
						if (loclist1.get(p1).max > loclist2.get(p2).max)
						{
							newloclist.add(new ExtendedLongLocation(loclist1.get(p1).min, loclist2.get(p2).max, loclist1.get(p1).indicator + loclist2.get(p2).indicator));
							locsource = 1;
							loc = new ExtendedLongLocation(loclist2.get(p2).max + 1, loclist1.get(p1).max, loclist1.get(p1).indicator);
						}
						else if (loclist1.get(p1).max < loclist2.get(p2).max)
						{
							newloclist.add(new ExtendedLongLocation(loclist2.get(p2).min, loclist1.get(p1).max, loclist1.get(p1).indicator + loclist2.get(p2).indicator));
							locsource = 2;
							loc = new ExtendedLongLocation(loclist1.get(p1).max + 1, loclist2.get(p2).max, loclist2.get(p2).indicator);
						}
						else
							newloclist.add(new ExtendedLongLocation(loclist1.get(p1), loclist1.get(p1).indicator + loclist2.get(p2).indicator));
						p1++;
						p2++;
					}
					break;
				}
				case 1:
				{
					if (loclist2.get(p2).overlap(loc))
					{
						if (loc.min != loclist2.get(p2).min)
							newloclist.add(new ExtendedLongLocation(loc.min, loclist2.get(p2).min - 1, loc.indicator));
						if (loclist2.get(p2).max > loc.max)
						{
							newloclist.add(new ExtendedLongLocation(loclist2.get(p2).min, loc.max, loc.indicator + loclist2.get(p2).indicator));
							locsource = 2;
							loc = new ExtendedLongLocation(loc.max + 1, loclist2.get(p2).max, loclist2.get(p2).indicator);							
						}
						else
						{
							newloclist.add(new ExtendedLongLocation(loclist2.get(p2).min, loclist2.get(p2).max, loc.indicator + loclist2.get(p2).indicator));
							if (loc.max > loclist2.get(p2).max)
								loc.min = loclist2.get(p2).max + 1;
							else
							{
								loc = null;
								locsource = 0;								
							}
						}
						p2++;
					}
					else
					{
						newloclist.add(loc);
						loc = null;
						locsource = 0;
					}
					break;
				}
				case 2:
				{
					if (loclist1.get(p1).overlap(loc))
					{
						if (loc.min != loclist1.get(p1).min)
							newloclist.add(new ExtendedLongLocation(loc.min, loclist1.get(p1).min - 1, loc.indicator));
						if (loclist1.get(p1).max > loc.max)
						{
							newloclist.add(new ExtendedLongLocation(loclist1.get(p1).min, loc.max, loc.indicator + loclist1.get(p1).indicator));
							locsource = 1;
							loc = new ExtendedLongLocation(loc.max + 1, loclist1.get(p1).max, loclist1.get(p1).indicator);							
						}
						else
						{
							newloclist.add(new ExtendedLongLocation(loclist1.get(p1).min, loclist1.get(p1).max, loc.indicator + loclist1.get(p1).indicator));
							if (loc.max > loclist1.get(p1).max)
								loc.min = loclist1.get(p1).max + 1;
							else
							{
								loc = null;
								locsource = 0;								
							}
						}
						p1++;
					}
					else
					{
						newloclist.add(loc);
						loc = null;
						locsource = 0;
					}
					break;
				}
				default: return null;
			}
		}
		if (loc != null)
			newloclist.add(loc);
		if (p1 < loclist1.size())
			for (int i = p1; i < loclist1.size(); i++)
				newloclist.add(new ExtendedLongLocation(loclist1.get(i)));
		if (p2 < loclist2.size())
			for (int i = p2; i < loclist2.size(); i++)
				newloclist.add(new ExtendedLongLocation(loclist2.get(i)));
		return newloclist;

	}
	
	
	public static List<ExtendedLongLocation> stitch(List<ExtendedLongLocation> locList) {
		List<ExtendedLongLocation> newLocList = new ArrayList<ExtendedLongLocation>();
		ExtendedLongLocation previousLoc = null;
		for (ExtendedLongLocation loc : locList) {
			if (previousLoc != null) {
				if (loc.min - 1 == previousLoc.max)
					if (loc.indicator == previousLoc.indicator)
						previousLoc = new ExtendedLongLocation(previousLoc.min, loc.max, loc.indicator);
				else {
					newLocList.add(previousLoc);
					previousLoc = loc;
				}
			}
			else
				previousLoc = loc;
		}
		newLocList.add(previousLoc);
		return newLocList;
	}
	
	public static List<ExtendedLongLocation> merge(List<ExtendedLongLocation> locList) {
		List<ExtendedLongLocation> newLocList = new ArrayList<ExtendedLongLocation>();
		// Initial list is sorted according to min
		Collections.sort(locList, SimpleLongLocation.minc);
		PriorityQueue<ExtendedLongLocation> workingLocs = new PriorityQueue<>(SimpleLongLocation.maxc);
		long lastStopPos = 0;
		int nextLocIndex = 0;
		while (!workingLocs.isEmpty() || nextLocIndex < locList.size()) {
			if (workingLocs.isEmpty()) {
				// Add new workingLoc to the empty workingLocs
				workingLocs.add(locList.get(nextLocIndex));
				nextLocIndex++;
			}
			else {
				if (nextLocIndex < locList.size() && locList.get(nextLocIndex).min <= workingLocs.peek().max) {
					// If a new working location overlap the first working locations, add this new working location and create a merged loc from last position to the minimum of the new working location
					long start = lastStopPos + 1;
					long stop = locList.get(nextLocIndex).min - 1;
					int indicator = 0;
					for (ExtendedLongLocation workingLoc : workingLocs)
						indicator += workingLoc.indicator;
					
					if (stop >= start) {
						// Valid location: If the new working location shares the same min as first.min, it will become a zero-length loc and we don't add this 
						newLocList.add(new ExtendedLongLocation(start, stop, indicator));
						lastStopPos = stop;
					}
					else
						assert stop == start - 1;
					workingLocs.add(locList.get(nextLocIndex));
					nextLocIndex++;
				}
				else {
					// No new working location
					long start = lastStopPos + 1;
					long stop = workingLocs.peek().max;
					int indicator = 0;
					for (ExtendedLongLocation workingLoc : workingLocs)
						indicator += workingLoc.indicator;
					assert stop >= start;
					
					newLocList.add(new ExtendedLongLocation(start, stop, indicator));
					lastStopPos = stop;
				}
				// Remove all completed workingLoc
				while (!workingLocs.isEmpty() && workingLocs.peek().max <= lastStopPos)
					workingLocs.poll();
			}
		}
		
		// Stitching
		newLocList = stitch(newLocList);
		
		return newLocList;
	}
	public static List<ExtendedLongLocation> filter(List<ExtendedLongLocation> loclist, int cutoff) {
		List<ExtendedLongLocation> newloclist = new ArrayList<ExtendedLongLocation>();
		for (ExtendedLongLocation loc : loclist)
			if (loc.indicator >= cutoff)
				newloclist.add(loc);
		return newloclist;
	}
	public static List<ExtendedLongLocation> mergeCloseRegion(List<ExtendedLongLocation> loclist)
	{
		// input should be processed by combineExtendLocationList
		// also, if cutoff is needed, processed by filter
		// input must be sorted
		if (loclist.isEmpty())
			return loclist;
		ExtendedLongLocation previousloc = null;
		List<ExtendedLongLocation> newloclist = new ArrayList<ExtendedLongLocation>();
		for (ExtendedLongLocation loc : loclist)
		{
			if (previousloc != null)
			{
				if (loc.min - 1 == previousloc.max)
					previousloc = new ExtendedLongLocation(previousloc.min, loc.max, Math.max(loc.indicator, previousloc.indicator));

//				if (loc.min - 1 == previousloc.max)
//					if (loc.indicator == previousloc.indicator)
//						previousloc = new ExtendedLongLocation(previousloc.min, loc.max, Math.max(loc.indicator, previousloc.indicator));
				else
				{
					newloclist.add(previousloc);
					previousloc = loc;
				}
			}
			else
				previousloc = loc;
		}
		newloclist.add(previousloc);
		return newloclist;
	}

	@Override
	public String toString() {
		return (Long.toString(this.min) + "\t" + Long.toString(this.max) + "\t" + Integer.toString(indicator));
		
	}
	public static List<ExtendedLongLocation> extendSimpleLongLocation(List<SimpleLongLocation> loclist, int indicator)
	{
		List<ExtendedLongLocation> newloclist = new ArrayList<ExtendedLongLocation>();
		for (SimpleLongLocation loc : loclist)
			newloclist.add(new ExtendedLongLocation(loc, indicator));
		return newloclist;
	}
	public static List<SimpleLongLocation> toSimpleLongLocation(List<ExtendedLongLocation> loclist)
	{
		List<SimpleLongLocation> newloclist = new ArrayList<SimpleLongLocation>();
		for (ExtendedLongLocation loc : loclist)
			newloclist.add(new SimpleLongLocation(loc));
		return newloclist;
	}
}
*/