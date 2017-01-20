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

// For future development
public class ReferenceSignal implements Comparable<ReferenceSignal>{
	public final String ref;
	public final int refpPos;
	
	public ReferenceSignal(String ref, int refpPos) {
		this.ref = ref;
		this.refpPos = refpPos;
	}

	public ReferenceSignal(String sourcestring) {
		String[] l = sourcestring.split(":");
		if (l.length != 2)
			throw new IllegalArgumentException();
		this.ref = l[0];
		this.refpPos = Integer.parseInt(l[1]);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ReferenceSignal) {
			ReferenceSignal refSig = (ReferenceSignal) obj;
			return ref.equals(refSig.ref) && refpPos == refSig.refpPos;
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return ref.hashCode() + refpPos;
	}
	
	@Override
	public String toString() {
		return ref + ":" + Integer.toString(refpPos);
	}

	@Override
	public int compareTo(ReferenceSignal refSig) {
		int x = this.ref.compareTo(refSig.ref);
		if (x == 0)
			return Integer.compare(this.refpPos, refSig.refpPos);
		else
			return x;
	}
}
