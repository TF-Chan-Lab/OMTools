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


package aldenjava.opticalmapping.data.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

public class DataSignalCountNode extends DataNode {
	public int[] refpSigMatchCount;
	public int[] refpCount;
	public int[] reflExtraSigCount;
	public int[] reflCount;
	public List<List<OptMapResultNode>> refpevidencelist = new ArrayList<List<OptMapResultNode>>(); // not used
	public List<List<OptMapResultNode>> reflevidencelist = new ArrayList<List<OptMapResultNode>>();
	public DataSignalCountNode(DataNode ref) {
		super(ref);
		refpSigMatchCount = new int[ref.refp.length];
		refpCount = new int[ref.refp.length];
		reflExtraSigCount = new int[ref.refp.length + 1];
		reflCount = new int[ref.refp.length + 1];
		for (int i = 0; i < ref.refp.length; i++)
			refpevidencelist.add(new ArrayList<OptMapResultNode>());
		for (int j = 0; j < ref.refp.length + 1; j++)
			reflevidencelist.add(new ArrayList<OptMapResultNode>());
	}
	
	
	public void update(OptMapResultNode result, int flankSig) {
		// This is not applied yet. Should be used in a more general way
		if (!result.mappedRegion.ref.equals(super.name))
			throw new IllegalArgumentException("Mismatch in update of aligned reference coverage");
		int limit1 = flankSig;
		int matchSigPos = 0;
		int limit2 = result.getMatch() - flankSig + 1;
		String precigar = result.cigar.getPrecigar();
		int currentrefpos = result.subrefstart;
		for (char c : precigar.toCharArray()) {
			switch (c) {
				case 'M':
					matchSigPos++;
					if (matchSigPos > limit1 && matchSigPos < limit2) {
						this.refpSigMatchCount[currentrefpos - 1]++;
						this.refpCount[currentrefpos - 1]++;
						this.refpevidencelist.get(currentrefpos - 1).add(result);
					}
					currentrefpos += 1;
					break;
				case 'D':
					if (matchSigPos > limit1 && matchSigPos < limit2) {
						this.refpCount[currentrefpos - 1]++;
						this.refpevidencelist.get(currentrefpos - 1).add(result);
					}
					currentrefpos += 1;
					break;
				default:
					;
			}
		}
	}
	public static LinkedHashMap<String, DataSignalCountNode> initialize(LinkedHashMap<String, DataNode> optrefmap) {
		LinkedHashMap<String, DataSignalCountNode> optrefsigmap = new LinkedHashMap<String, DataSignalCountNode>();
		for (DataNode ref : optrefmap.values())
			optrefsigmap.put(ref.name, new DataSignalCountNode(ref));
		return optrefsigmap;
	}
	

}
