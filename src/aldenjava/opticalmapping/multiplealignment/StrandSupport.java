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


package aldenjava.opticalmapping.multiplealignment;

public class StrandSupport {
	double forwardSupport = 0;
	double reverseSupport = 0;

	public StrandSupport() {
		this(0, 0);
	}

	public StrandSupport(double forwardSupport, double reverseSupport) {
		this.forwardSupport = forwardSupport;
		this.reverseSupport = reverseSupport;
	}

	public void assignIfLargerSupport(int orientation, double mappedscore) {
		if (orientation == 1)
			forwardSupport = Math.max(forwardSupport, mappedscore);
		else if (orientation == -1)
			reverseSupport = Math.max(reverseSupport, mappedscore);
	}

	public void addSupport(int orientation, double mappedscore) {
		if (orientation == 1)
			forwardSupport += mappedscore;
		else if (orientation == -1)
			reverseSupport += mappedscore;
	}
	public void addSupport(StrandSupport support) {
		this.forwardSupport += support.forwardSupport;
		this.reverseSupport += support.reverseSupport;
	}
	public void reverse() {
		double tmp = forwardSupport;
		this.forwardSupport = this.reverseSupport;
		this.reverseSupport = tmp;
	}
	
	public int getBestStrand() {
		return forwardSupport >= reverseSupport ? 1 : -1;
	}

	@Override
	public String toString() {
		return String.format("F:%.1f R:%.1f", forwardSupport, reverseSupport);
	}

	public StrandSupport getReverse() {
		return new StrandSupport(reverseSupport, forwardSupport);
	}
}
