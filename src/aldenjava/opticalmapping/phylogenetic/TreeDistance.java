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


package aldenjava.opticalmapping.phylogenetic;


public class TreeDistance implements Comparable<TreeDistance> {
	private final PhylogeneticTree subTree1;
	private final PhylogeneticTree subTree2;
	private final double distance;
	public TreeDistance(PhylogeneticTree subTree1, PhylogeneticTree subTree2, double distance) {
		this.subTree1 = subTree1;
		this.subTree2 = subTree2;
		this.distance = distance;
	}
	public PhylogeneticTree getTree1() {
		return subTree1;
	}
	public PhylogeneticTree getTree2() {
		return subTree2;
	}
	public double getDistance() {
		return distance;
	}
	@Override
	public int compareTo(TreeDistance td) {
		return Double.compare(this.getDistance(), td.getDistance());
	}
}

