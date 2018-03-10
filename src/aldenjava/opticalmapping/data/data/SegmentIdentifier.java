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

public class SegmentIdentifier implements Comparable<SegmentIdentifier> {
	public final String name;
	public final int segment;
	public SegmentIdentifier(String name, int segment) {
		super();
		this.name = name;
		this.segment = segment;
	}
	@Override
	public int hashCode() {
		return name.hashCode() + segment;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SegmentIdentifier) {
			SegmentIdentifier segID = (SegmentIdentifier) obj;
			return segID.name.equals(this.name) && segID.segment == this.segment; 
		}
		return false;
	}
	@Override
	public int compareTo(SegmentIdentifier si) {
		int x = this.name.compareTo(si.name);
		if (x != 0)
			return x;
		return Integer.compare(this.segment, si.segment);
	}
	@Override
	public String toString() {
		return name + ": " + segment;
	}
	
}

