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


package aldenjava.opticalmapping.visualizer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.Cigar;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.MatchingSignalPair;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

/**
 * A utility class to manipulate results so that they are shown in a beautiful way. Functions include reversing, scaling and trimming (Trimming function was removed from OMView v1.05-beta).
 * @author Alden
 * 
 */
public class VUtils {
	public static OptMapResultNode modifyAllReverse(OptMapResultNode result)
	{
		if (result.mappedstrand == -1)
			return result.newReverseFragment();
		else
			return new OptMapResultNode(result);
	}
	private static OptMapResultNode scaleFragment(OptMapResultNode result)
	{
		OptMapResultNode newresult = new OptMapResultNode(result);
		newresult.scale(1 / result.getMapScale());
		return newresult;
	}
	public static OptMapResultNode modifyResult(OptMapResultNode result)
	{
		return scaleFragment(modifyAllReverse(result));
	}
	
	public static DataNode scaleIndividualFragment(OptMapResultNode result, DataNode ref)
	{ 
		OptMapResultNode newresult = new OptMapResultNode(result);
		DataNode d = new DataNode(newresult.parentFrag);
//		long[] refl = d.getRefl();
//		long[] refp = Arrays.copyOf(d.refp, d.refp.length);
		long[] refp = d.refp;
		result.updateMSP();
		for (int i = 0; i < result.mspsQ.size() - 1; i++) {
			MatchingSignalPair msp1 = result.mspsQ.get(i);
			MatchingSignalPair msp2 = result.mspsQ.get(i + 1);
			long refLen = Math.abs(ref.refp[msp2.rpos] - ref.refp[msp1.rpos]);
			long queryLen = refp[msp2.qpos] - refp[msp1.qpos];
			long diff = refLen - queryLen;
			double scale = refLen / (double) queryLen;
			for (int sigPos = msp1.qpos + 1; sigPos < d.refp.length; sigPos++)
				if (sigPos < msp2.qpos)
					refp[sigPos] = (long) ((refp[sigPos] - refp[msp1.qpos]) * scale) + refp[msp1.qpos];
				else
					refp[sigPos] += diff; 
			d.size += diff;
		}
		return d;
	}

	
	
	
	private static List<OptMapResultNode> modifyAllReverse(List<OptMapResultNode> resultlist)
	{
//		Collections.sort(resultlist, Collections.reverseOrder(OptMapResultNode.mappedscorecomparator));
		boolean needReverse = false;
		Collections.sort(resultlist, OptMapResultNode.subfragstartstopcomparator);
		if (resultlist.size() == 1)
			if (resultlist.get(0).mappedstrand == -1)
				needReverse = true;
		if (resultlist.size() > 1)
		{
			OptMapResultNode prevResult = null;
			for (OptMapResultNode result : resultlist)
			{
				if (prevResult != null)
					if (result.mappedRegion.start < prevResult.mappedRegion.start)
						needReverse = true;
				prevResult = result;
			}
		}
			
		if (needReverse)
		{
			List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
			for (OptMapResultNode result : resultlist)
				newresultlist.add(result.newReverseFragment());
			resultlist = newresultlist;
		}
		return resultlist;

	}
	private static List<OptMapResultNode> scaleFragment(List<OptMapResultNode> resultlist) 
	{
		Collections.sort(resultlist, OptMapResultNode.mappedstartcomparator);
		List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
		double ratio = 1;
		for (OptMapResultNode result : resultlist)
			if (result.mappedstrand == 1)
			{
				ratio = result.getMapScale();
				break;
			}

		
		for (OptMapResultNode result : resultlist)
		{
			OptMapResultNode newresult = new OptMapResultNode(result);
			newresult.scale(1 / ratio);
			newresultlist.add(newresult);
		}
		return newresultlist;
	}
	public static List<OptMapResultNode> modifyResult(List<OptMapResultNode> resultlist)
	{
//		System.out.println("Util");
		List<OptMapResultNode> newresultlist = modifyAllReverse(resultlist);
		newresultlist = scaleFragment(newresultlist);
//		for (int i = 0; i < resultlist.size(); i++)
//		{
//			System.out.printf("%d\t%d\t-\t%d\t%d\n", resultlist.get(i).subrefstart, resultlist.get(i). subrefstop, newresultlist.get(i).subrefstart, newresultlist.get(i).subrefstop);
//			System.out.println(newresultlist.get(0).subrefstart);
//		}
		return newresultlist;
	}
	
	
	
	public static List<OptMapResultNode> modifyAllReverseOnRef(List<OptMapResultNode> resultlist, DataNode ref)
	{
//		Collections.sort(resultlist, Collections.reverseOrder(OptMapResultNode.mappedscorecomparator));
		boolean needReverse = false;
		Collections.sort(resultlist, OptMapResultNode.subfragstartstopcomparator);
		if (resultlist.size() == 1)
			if (resultlist.get(0).mappedstrand == -1)
				needReverse = true;
		if (resultlist.size() > 1)
		{
			OptMapResultNode prevResult = null;
			for (OptMapResultNode result : resultlist)
			{
				if (prevResult != null)
					if (result.mappedRegion.start < prevResult.mappedRegion.start)
						needReverse = true;
				prevResult = result;
			}
		}
		if (needReverse)
		{
			int indicator = ref.refp.length + 1;
			ref.reverse();
			List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
			for (OptMapResultNode result : resultlist)
			{				
				int newsubrefstart = indicator - result.subrefstop - 1;
				int newsubrefstop = indicator - result.subrefstart - 1;
				int newsubfragstart = result.subfragstop;
				int newsubfragstop = result.subfragstart;
				int newmappedstrand = result.mappedstrand * -1;
				Cigar newCigar = new Cigar(result.cigar);
				newCigar.reverse();
				OptMapResultNode newresult = new OptMapResultNode(result);
				newresult.subfragstart = newsubfragstart;
				newresult.subfragstop = newsubfragstop;
				newresult.subrefstart = newsubrefstart;
				newresult.subrefstop = newsubrefstop;
				newresult.mappedstrand = newmappedstrand;
				newresult.setCigar(newCigar);
				newresult.updateMappedRegion(ref);
				newresultlist.add(newresult);
			}
			resultlist = newresultlist;
		}
		return resultlist;

	}
	private static List<OptMapResultNode> scaleFragmentOnRef(List<OptMapResultNode> resultlist, DataNode ref) 
	{
		Collections.sort(resultlist, OptMapResultNode.mappedstartcomparator);
		List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
		double ratio = 1;
		for (OptMapResultNode result : resultlist)
			if (result.mappedstrand == 1)
			{
				ratio = result.getMapScale();
				break;
			}
		ref.scale(ratio);
		for (OptMapResultNode result : resultlist)
		{
			OptMapResultNode newresult = new OptMapResultNode(result);
			newresult.updateMappedRegion(ref);
			newresultlist.add(newresult);
		}
		return newresultlist;
	}

	public static List<OptMapResultNode> modifyResultOnRef(List<OptMapResultNode> resultlist, DataNode ref)
	{
		// ref will be changed afterwards
		List<OptMapResultNode> newresultlist = modifyAllReverseOnRef(resultlist, ref);
		newresultlist = scaleFragmentOnRef(newresultlist, ref);
		return newresultlist;
	}

	
}
