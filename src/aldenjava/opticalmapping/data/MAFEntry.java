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


package aldenjava.opticalmapping.data;

import java.util.Comparator;

public class MAFEntry {
	public final String label;
	public final String id; 
	public final long start; // store as 1-based
	public final long length; 
	public final int orientation;
	public final long genomeSize;
	public final String seq;
	public MAFEntry(String label, String id, long start, long length, int orientation, long genomeSize, String seq) {
		super();
		this.label = label;
		this.id = id;
		this.start = start;
		this.length = length;
		this.orientation = orientation;
		this.genomeSize = genomeSize;
		this.seq = seq;
	}
	
	public static Comparator<MAFEntry> startEndComparator = new Comparator<MAFEntry>() {
		@Override
		public int compare(MAFEntry entry1, MAFEntry entry2) {
			int x = Long.compare(entry1.start, entry2.start);
			if (x != 0)
				return x;
			return Long.compare(entry1.length, entry2.length);
		}
		
	};
}
