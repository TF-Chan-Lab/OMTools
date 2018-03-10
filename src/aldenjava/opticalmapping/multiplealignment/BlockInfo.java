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

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;

public class BlockInfo {
	// Signal-based
	public int startSig;
	public int stopSig;
	public BlockInfo(int startSig, int stopSig) {
		super();
		if (startSig == stopSig)
			throw new IllegalArgumentException("Start signal could not be equal to stop signal.");
		if (startSig < 0 || stopSig < 0)
			throw new IllegalArgumentException("Start signal and stop signal must be positive.");
		this.startSig = startSig;
		this.stopSig = stopSig;
	}
	public boolean isReverse() {
		return stopSig < startSig;
	}
	public void reverse() {
		int tmp = startSig;
		startSig = stopSig;
		stopSig = tmp;
	}
	public int getNumberOfSignals() {		
		return Math.abs(stopSig - startSig) + 1;
	}
	public GenomicPosNode getRegion(DataNode data) {
		if (!isReverse())
			return new GenomicPosNode(data.name, data.refp[startSig], data.refp[stopSig]);
		else
			return new GenomicPosNode(data.name, data.refp[stopSig], data.refp[startSig]);
	}
}
