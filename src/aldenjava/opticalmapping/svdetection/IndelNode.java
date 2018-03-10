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


package aldenjava.opticalmapping.svdetection;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.StatUtils;

import aldenjava.opticalmapping.GenomicPosNode;

public class IndelNode extends SVNode{
	public final int refSize;
	public final int fragSize;
	
//	private final List<IndelNode> svList;
	public IndelNode(GenomicPosNode bp1, GenomicPosNode bp2, int refSize, int fragSize) {
		super(bp1, bp2);
		this.refSize = refSize;
		this.fragSize = fragSize;
	}
	public IndelNode(List<IndelNode> indelList)
	{
		super(indelList);
		int totalRefSize = 0;
		int totalFragSize = 0;
		double[] refSizeArray = new double[indelList.size()];
		double[] fragSizeArray = new double[indelList.size()];
		int count = 0;
		for (IndelNode indel : indelList)
		{
			refSizeArray[count] = indel.refSize;
			fragSizeArray[count] = indel.fragSize;
			totalRefSize += indel.refSize;
			totalFragSize += indel.fragSize;
			count++;
		}
		Arrays.sort(refSizeArray);
		Arrays.sort(fragSizeArray);
//		this.refSize = (int) StatUtils.percentile(refSizeArray, 50);
//		this.fragSize = (int) StatUtils.percentile(fragSizeArray, 50);
		
		this.refSize = totalRefSize / indelList.size();
		this.fragSize = totalFragSize / indelList.size();
		
	}
	public IndelNode(GenomicPosNode bp1, GenomicPosNode bp2, int sizeChange) {
		super(bp1, bp2);
		this.refSize = 0;
		this.fragSize = sizeChange;
	}
	@Override
	public String getType() 
	{
		if (refSize > fragSize)
			return "Deletion";
		if (fragSize > refSize)
			return "Insertion";
		return "VirtualIndel";
	}
	public static boolean printout = false;
	@Override
	public boolean isSimilarSV(SVNode sv, long closeSV)
	{
		if (!isSameType(sv))
			return false;
		IndelNode indel = (IndelNode) sv;
		int size1 = Math.abs(getIndelSize());
		int size2 = Math.abs(indel.getIndelSize());
		if (!super.isSimilarSV(sv, closeSV + Math.max(size1, size2)))
			return false;
		
		// check if indel size differs too much		
		double similarIndelSizeDiff = 1000 + Math.max(size1, size2) * 0.1;
		if (Math.abs(getIndelSize() - indel.getIndelSize()) > similarIndelSizeDiff)
			return false;
		
		return true;
	}
	public int getIndelSize()
	{
		return fragSize - refSize;
	}
	
	
//	public IndelNode(GenomicPosNode region, int refSize, int fragSize) {
//		super(region);
//		this.refSize = refSize;
//		this.fragSize = fragSize;
//	}
//
//	public int getSizeDiff()
//	{
//		return fragSize - refSize;
//	}
//	public int getAbsSizeDiff()
//	{
//		return Math.abs(getSizeDiff());
//	}
//	public double getSizeRatio()
//	{
//		if (refSize == 0)
//			return Double.POSITIVE_INFINITY;
//		else
//			return fragSize / (double) refSize;
//	}
//	public double getRelativeSizeRatio()
//	{
//		double ratio = getSizeRatio();
//		if (ratio < 1)
//			if (ratio != 0)
//				return 1 / ratio;
//			else
//				return Double.POSITIVE_INFINITY;
//		else
//			return ratio;
//	}
//
//	@Override
//	public String type() {
//		return "indel";
//	}
//	
//	public static Comparator<IndelNode> regionComparator = new Comparator<IndelNode>(){
//		public int compare(IndelNode indel1, IndelNode indel2)
//		{
//			return indel1.region.compareTo(indel2.region);
////			if (!indel1.ref.equalsIgnoreCase(indel2.ref))
////				return indel1.ref.compareToIgnoreCase(indel2.ref);
////			else
////				if (indel1.region.min != indel2.region.min)
////					return Long.compare(indel1.region.min, indel2.region.min);
////				else
////					return Long.compare(indel1.region.max, indel2.region.max);
//		}
//	};
//	public static Comparator<IndelNode> indelSizeComparator = new Comparator<IndelNode>(){
//		public int compare(IndelNode indel1, IndelNode indel2)
//		{
//			return Integer.compare(indel1.getSizeDiff(), indel2.getSizeDiff());
//		}
//	};
}
