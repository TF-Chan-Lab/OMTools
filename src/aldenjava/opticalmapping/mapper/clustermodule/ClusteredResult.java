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


package aldenjava.opticalmapping.mapper.clustermodule;

import java.util.List;

import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

/**
 * The joined/clustered alignment results
 * @author Alden
 *
 */
public class ClusteredResult implements Comparable<ClusteredResult> {
	public List<OptMapResultNode> updatedResult = null;
	public Double score = null;

	public ClusteredResult() {
	}

	public void process(VirtualMapProcessor vmProcessor) {
		score = vmProcessor.calcScore(updatedResult);
	}

	public void importUpdatedResult(List<OptMapResultNode> updatedResult) {
		this.updatedResult = updatedResult;
	}

	@Override
	public int compareTo(ClusteredResult cr) {
		return Double.compare(this.score, cr.score);
	}

	public double getFragRatio() {
		double total = 0;
		for (OptMapResultNode result : updatedResult)
			total += result.getSubFragRatio();
		return total;
	}

	public double getMapSigRatio() {
		double total = 0;
		for (OptMapResultNode result : updatedResult)
			total += result.getMapSigRatio();
		return total;
	}

}