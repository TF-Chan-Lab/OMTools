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


package aldenjava.opticalmapping.mapper;

public class MatchHelper {

	/**
	 * Test if the query segment match the reference segment under the provided error tolerance. The query segment matches the reference segment if <code>refSegmentSize * (1-scalingRange) - measure <= querySegmentSize <= refSegmentSize * (1+scalingRange)</code>
	 * 
	 * @param refSegmentSize
	 *            Size of reference segment
	 * @param querySegmentSize
	 *            Size of query segment
	 * @param measure
	 *            Measurement error tolerance
	 * @param scalingRange
	 *            Scaling range tolerance
	 * @return <code>true</code> if the two segments match under the provided error tolerance
	 * @see MatchHelper#fuzzyMatch
	 */
	public static boolean match(double refSegmentSize, double querySegmentSize, long measure, double scalingRange) {
		assert refSegmentSize >= 0;
		assert querySegmentSize >= 0;
		assert measure >= 0;
		assert scalingRange >= 0;
		return refSegmentSize * (1 + scalingRange) + measure >= querySegmentSize && refSegmentSize * (1 - scalingRange) - measure <= querySegmentSize;
	}

	/**
	 * Test the match of two segments by using the larger segment size as reference.
	 * 
	 * @param segmentSize1
	 *            Size of first segment
	 * @param segmentSize2
	 *            Size of second segment
	 * @param measure
	 *            Measurement error tolerance
	 * @param scalingRange
	 *            Scaling range tolerance
	 * @return <code>true</code> if the two segments match under the provided error tolerance
	 * @see MatchHelper#match
	 */
	public static boolean fuzzyMatch(long segmentSize1, long segmentSize2, long measure, double scalingRange) {
		assert segmentSize1 >= 0;
		assert segmentSize2 >= 0;
		assert measure >= 0;
		assert scalingRange >= 0;
		long largerSize = Math.max(segmentSize1, segmentSize2);
		long smallerSize = Math.min(segmentSize1, segmentSize2);
		return match(largerSize, smallerSize, measure, scalingRange);
	}

	public static double getMaxMatchingRefSegmentSize(double querySegmentSize, long measure, double scalingRange) {
		assert querySegmentSize >= 0;
		assert measure >= 0;
		assert scalingRange >= 0;
		return (querySegmentSize + measure) / (1 - scalingRange);
	}
	public static double getMinMatchingRefSegmentSize(double querySegmentSize, long measure, double scalingRange) {
		assert querySegmentSize >= 0;
		assert measure >= 0;
		assert scalingRange >= 0;
		double len = (querySegmentSize - measure) / (1 + scalingRange);
		return len >= 0? len : 0;
	}

	public static double getScale(long refSegmentSize, long querySegmentSize) {
		return (double) querySegmentSize / (double) refSegmentSize; 
	}
}
