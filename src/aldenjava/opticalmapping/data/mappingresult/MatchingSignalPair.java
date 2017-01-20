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


package aldenjava.opticalmapping.data.mappingresult;

import java.util.Comparator;

public class MatchingSignalPair implements Comparable<MatchingSignalPair> {
	public final int rpos;
	public final int qpos;

	public MatchingSignalPair(int rpos, int qpos) {
		this.rpos = rpos;
		this.qpos = qpos;
	}

	public boolean equals(MatchingSignalPair msp) {
		return this.rpos == msp.rpos && this.qpos == msp.qpos;
	}

	public boolean overlap(MatchingSignalPair msp) {
		return this.rpos == msp.rpos || this.qpos == msp.qpos;
	}

	// In a list of MSPs in an alignment, qpos and rpos should be unique
	public static Comparator<MatchingSignalPair> rposComparator = new Comparator<MatchingSignalPair>() {
		@Override
		public int compare(MatchingSignalPair msp1, MatchingSignalPair msp2) {
			return Integer.compare(msp1.rpos, msp2.rpos);
		}
	};
	public static Comparator<MatchingSignalPair> qposComparator = new Comparator<MatchingSignalPair>() {
		@Override
		public int compare(MatchingSignalPair msp1, MatchingSignalPair msp2) {
			return Integer.compare(msp1.qpos, msp2.qpos);
		}
	};

	@Override
	public int compareTo(MatchingSignalPair msp) {
		if (this.rpos != msp.rpos)
			return Integer.compare(this.rpos, msp.rpos);
		else
			return Integer.compare(this.qpos, msp.qpos);
	}
	@Override
	public int hashCode() {
		return rpos * 31 + qpos;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MatchingSignalPair) {
			MatchingSignalPair msp = (MatchingSignalPair) obj;
			return (this.rpos == msp.rpos && this.qpos == msp.qpos);
		}
		else
			return false;
		
	}
}

