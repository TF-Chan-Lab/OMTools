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


package aldenjava.opticalmapping.assembler;

import java.util.ArrayList;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.DataSignalCountNode;

public class LocalConsensusPattern {
	public static DataSignalCountNode consensusMap(List<DataNode> reflist, long size) {
		int winSize = 1500;
		int leapSize = 50;
		List<Integer> winSignalList = new ArrayList<Integer>();
		for (DataNode r : reflist) {
			for (long pos : r.refp) {
				int start = (int) ((pos - (winSize - 1) - 1 + (leapSize - 1)) / leapSize);
				if (start < 0)
					start = 0;
				int stop = (int) ((pos + (leapSize - 1)) / leapSize); // inclusive

				for (int i = start; i <= stop; i++) {
					while (i > winSignalList.size() - 1)
						winSignalList.add(0);
					winSignalList.set(i, winSignalList.get(i) + 1);
				}
			}
		}

		int total = 0;
		for (int i = 0; i < winSignalList.size(); i++) {
			total += winSignalList.get(i);
		}
		int background = (int) Math.ceil(total / (double) winSignalList.size());

		int last = 0;
		int peakpos = -1;
		int peakvalue = -1;
		int peakposno = 0;
		int highestpeakvalue = 0;
		List<Integer> peaklist = new ArrayList<Integer>();
		List<Integer> peakvaluelist = new ArrayList<Integer>();
		// will there be last peak or first peak not called?
		for (int i = 0; i < winSignalList.size(); i++) {
			int winSignal = winSignalList.get(i);
			if (winSignal >= peakvalue)
				if (winSignal > last) {
					peakvalue = winSignal;
					peakpos = i;
					peakposno = 1;

				} else if (winSignal == last) {
					peakpos = peakpos + i;
					peakposno++;
				}
			if (winSignal < last) {
				// drops to 1/2 of the highest value
				if (winSignal < peakvalue / 2) {
					if (peakvalue - background > 0) {
						peaklist.add(peakpos / peakposno);
						peakvaluelist.add(peakvalue);
						if (peakvalue > highestpeakvalue)
							highestpeakvalue = peakvalue;
					}
					peakpos = -1;
					peakvalue = -1;
					peakposno = 0;
				}
			}
			last = winSignal;
		}
		// final fix for last sliding window
		while ((winSignalList.size() - 1) * leapSize + winSize < size)
			winSignalList.add(0);
		while ((winSignalList.size() - 1) * leapSize + winSize > size && winSignalList.size() > 1) // prevent only single too small region (< winSize)
			winSignalList.remove(winSignalList.size() - 1);

		for (int i = peaklist.size() - 1; i >= 0; i--)
			if (peakvaluelist.get(i) < highestpeakvalue / 2.0) {
				peaklist.remove(i);
				peakvaluelist.remove(i);
			}
		long[] newrefp = new long[peaklist.size()];
		for (int i = 0; i < peaklist.size(); i++)
			if (peakvaluelist.get(i) >= highestpeakvalue / 2.0) {
				newrefp[i] = (peaklist.get(i) * leapSize + winSize / 2);
			}
		DataSignalCountNode rSig = new DataSignalCountNode(new DataNode("ConsensusRef", size, newrefp));
		for (int i = 0; i < peaklist.size(); i++)
			rSig.refpCount[i] = peakvaluelist.get(i);
		return rSig;
	}

}
