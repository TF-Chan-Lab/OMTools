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
import java.util.List;

/**
 * A class for storing temporary data during generation 
 */
public class SimuDataNode {
	List<Long> refp;
	List<VirtualSignal> vsList;
	public SimuDataNode(List<Long> refp, List<VirtualSignal> vsList) {
		this.refp = refp;
		this.vsList = vsList;
	}
	public static SimuDataNode createEmptyNode() {
		return new SimuDataNode(new ArrayList<Long>(), new ArrayList<VirtualSignal>());
	}
	public int getTotalSignal() {
		return refp.size();
	}
	public void sort() {
		for (int i = 0; i < getTotalSignal(); i++)
			for (int j = 0; j < getTotalSignal() - i - 1; j++) {
				if (refp.get(j) > refp.get(j + 1)) {
					Long tmp = refp.get(j);
					refp.set(j, refp.get(j + 1));
					refp.set(j + 1, tmp);
					VirtualSignal tmpv = vsList.get(j);
					vsList.set(j, vsList.get(j + 1));
					vsList.set(j + 1, tmpv);
				}
			}
	}
	public void merge() {
		if (getTotalSignal() == 0)
			return;
		List<Long> newrefp = new ArrayList<>(); 
		List<VirtualSignal> newvsList = new ArrayList<>();
		long lastSig = -1;
		int start = -1;
		for (int i = 0; i < getTotalSignal() + 1; i++) {
			if (i == getTotalSignal() || lastSig != refp.get(i)) {
				if (start != -1) {
					newrefp.add(lastSig);
					if (start == i - 1) // only one element
						newvsList.add(vsList.get(start));
					else {
						List<VirtualSignal> tmpvsList = new ArrayList<>();
						for (int j = start; j <= i - 1; j++) {
							if (vsList.get(j).sources == null)
								tmpvsList.add(vsList.get(j));
							else
								tmpvsList.addAll(vsList.get(j).sources);
						}
						newvsList.add(new VirtualSignal(tmpvsList));
					}
				}
				start = i;
				if (i < getTotalSignal())
					lastSig = refp.get(i);
			}
				
		}
		this.refp = newrefp;
		this.vsList = newvsList;
	}

}
