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

import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;

/**
 * Stores the simulation information of a simulated optical map molecule
 * @author Alden
 *
 */
public class SimulationInfo {
	public final GenomicPosNode simuRegion;
	public final int simuStrand;
	public List<VirtualSignal> vsList;
	
	public SimulationInfo(GenomicPosNode simuRegion, int simuStrand) {
		this.simuRegion = simuRegion;
		this.simuStrand = simuStrand;
		this.vsList = null;
	}
	public SimulationInfo(GenomicPosNode simuRegion, int simuStrand, List<VirtualSignal> vsList) {
		this.simuRegion = simuRegion;
		this.simuStrand = simuStrand;
		this.vsList = vsList;
	}
	public SimulationInfo(SimulationInfo simuInfo) {
		this.simuRegion = simuInfo.simuRegion;
		this.simuStrand = simuInfo.simuStrand;
		this.vsList = simuInfo.vsList;
	}

	public boolean hasVirtualSignalInfo() {
		return vsList != null;
	}
	public void removeVirtualSignalInfo() {
		vsList = null;
	}
	public static boolean checkInfoValid(String ref, long start, long stop, int strand) {
		return (ref != null && !ref.isEmpty()) && (start >= 1) && (stop >= 1) && ((strand == 1) || (strand == -1));
	}
}






//class VirtualInfo {
//	public List<VirtualSignal> vsList;
//	public void remove(List<Integer> index) {
//		Collections.sort(index, Collections.reverseOrder());
//		for (int i : index)
//			vsList.remove(i);
//	}
//}
