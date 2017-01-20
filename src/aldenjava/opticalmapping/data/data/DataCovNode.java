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


package aldenjava.opticalmapping.data.data;

import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

public class DataCovNode extends DataNode {
	public int[] reflCount;
	public DataCovNode(DataNode data) {
		super(data);
		reset();
	}
	public void reset() {
		reflCount = new int[getTotalSegment()];
	}
	public void update(OptMapResultNode result) {
		for (int i = result.subrefstart; i <= result.subrefstop; i++)
			reflCount[i]++;
	}
	public int getAlignedRefl(int cov) {
		int total = 0;
		for (int count : reflCount)
			if (count >= cov)
				total++;
		return total;
	}
	public long getAlignedLength(int cov) {
		long length = 0;
		for (int i = 0; i < reflCount.length; i++)
			if (reflCount[i] >= cov)
				length += super.getRefl(i);
		return length;	
	}
}
