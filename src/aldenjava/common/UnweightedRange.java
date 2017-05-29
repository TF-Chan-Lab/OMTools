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


package aldenjava.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class UnweightedRange<R extends Number & Comparable<R>> {
	public R min;
	public R max;
	public UnweightedRange(R min, R max) {
		
		if (!rangeClassSupported(min) || !rangeClassSupported(max))
			throw new IllegalArgumentException(min.getClass().getName() + " is not supported.");
		if (min.compareTo(max) > 0)
			throw new IllegalArgumentException("Min is larger than Max");
		this.min = min;
		this.max = max;
	}
	
	public UnweightedRange(UnweightedRange<R> range) {
		this.min = range.min;
		this.max = range.max;
	}

	public boolean overlap(UnweightedRange<R> range) {
		Class<R> rClass = (Class<R>) min.getClass();
		return overlap(range, NumberOperation.<R>getNumber(rClass, 0));
	}

	public boolean overlap(UnweightedRange<R> range, R allowedgap) {
		Class<R> rClass = (Class<R>) min.getClass();
		if (overlapSize(range).compareTo(NumberOperation.subtraction(NumberOperation.<R>getNumber(rClass, 1), allowedgap)) >= 0)
			return true;
		else
			return false;
	}

	public R overlapSize(UnweightedRange<R> range) {
		Class<R> rClass = (Class<R>) min.getClass();
		R max = range.max.compareTo(this.max) > 0 ? this.max : range.max;
		R min = range.min.compareTo(this.min) < 0 ? this.min : range.min;
		return NumberOperation.addition(NumberOperation.subtraction(max, min), NumberOperation.<R>getNumber(rClass, 1));
	}

	public R length() {
		return NumberOperation.subtraction(this.max, this.min);
	}
	
	private boolean rangeClassSupported(R r) {
		if (r instanceof Byte)
			return true;
		if (r instanceof Short)
			return true;
		if (r instanceof Integer)
			return true;
		if (r instanceof Long)
			return true;
		return false;
	}
	
	public boolean canStitch(UnweightedRange<R> range) {
		Class<R> rClass = (Class<R>) min.getClass();
		return NumberOperation.subtraction(this.max, range.min).compareTo(NumberOperation.<R>getNumber(rClass, 1)) == 0;
	}
	
	public static <R extends Number & Comparable<R>> UnweightedRange<R> getRangeIntersection(UnweightedRange<R> range1, UnweightedRange<R> range2) {
		R min = range1.min.compareTo(range2.min) > 0? range1.min : range2.min;
		R max = range1.max.compareTo(range2.max) < 0? range1.max : range2.max;
		if (min.compareTo(max) > 0)
			return null;
		return new UnweightedRange<R>(min, max);
	}
	public static <R extends Number & Comparable<R>> UnweightedRange<R> getRangeUnion(UnweightedRange<R> range1, UnweightedRange<R> range2) {
		R min = range1.min.compareTo(range2.min) < 0? range1.min : range2.min;
		R max = range1.max.compareTo(range2.max) > 0? range1.max : range2.max;
		assert min.compareTo(max) <= 0;
		return new UnweightedRange<R>(min, max);
	}

	public static <R extends Number & Comparable<R>> List<UnweightedRange<R>> stitchUnweightedRange(List<UnweightedRange<R>> ranges) {
		List<UnweightedRange<R>> newRanges = new ArrayList<>();
		UnweightedRange<R> previousRange = null;
		for (UnweightedRange<R> range : ranges) {
			if (previousRange != null) {
				if (previousRange.canStitch(range))
					previousRange = new UnweightedRange<R>(previousRange.min, range.max);
				else {
					newRanges.add(previousRange);
					previousRange = range;
				}
			}
			else
				previousRange = range;
		}
		newRanges.add(previousRange);
		return newRanges;
	}

	public static <R extends Number & Comparable<R>> List<UnweightedRange<R>> mergeUnweightedRange(List<UnweightedRange<R>> ranges) {
		if (ranges.isEmpty())
			return new ArrayList<UnweightedRange<R>>(ranges);
		
		List<UnweightedRange<R>> newRanges = new ArrayList<UnweightedRange<R>>();
		// Initial list is sorted according to min
		Collections.sort(ranges, UnweightedRange.minComparator());
		PriorityQueue<UnweightedRange<R>> workingRanges = new PriorityQueue<>(UnweightedRange.maxComparator());
		
		Class<R> rClass = (Class<R>) ranges.get(0).min.getClass();
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

					
					if (stop.compareTo(start) >= 0) {
						// Valid location: If the new working location shares the same min as first.min, it will become a zero-length loc and we don't add this 
						newRanges.add(new UnweightedRange<R>(start, stop));
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
					assert stop.compareTo(start) >= 0;
					newRanges.add(new UnweightedRange<R>(start, stop));
					lastStopPos = stop;
				}
				// Remove all completed workingLoc
				while (!workingRanges.isEmpty() && workingRanges.peek().max.compareTo(lastStopPos) <= 0)
					workingRanges.poll();
			}
		}
		
		// Stitching
		newRanges = stitchUnweightedRange(newRanges);
		
		return newRanges;
	}

	
	public static <R extends Number & Comparable<R>> Comparator<UnweightedRange<R>> minComparator() {
		return new Comparator<UnweightedRange<R>>() {
			@Override
			public int compare(UnweightedRange<R> r1, UnweightedRange<R> r2) {
				return r1.min.compareTo(r2.min);
			}
		};
	};
	public static <R extends Number & Comparable<R>> Comparator<UnweightedRange<R>> maxComparator() {
		return new Comparator<UnweightedRange<R>>() {
			@Override
			public int compare(UnweightedRange<R> r1, UnweightedRange<R> r2) {
				return r1.max.compareTo(r2.max);
			}
		};
	}

	
}



