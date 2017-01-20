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

	
//	public static List<OptMapResultNode> resultlist modfiyVariedReference(List<OptMapResultNode> resultlist, ReferenceNode ref)
//	{
//		FragmentNode f = new FragmentNode(ref);
//		Collections.sort(resultlist, Collections.reverseOrder(OptMapResultNode.mappedscorecomparator));
//		if (resultlist.get(0).mappedstrand == -1)
//			f.reverse();
//		
//	}

	
//	public static boolean trimOverlap(LinkedHashMap<String, ReferenceNode> optrefmap, int maxTrim, OptMapResultNode result1, OptMapResultNode result2)
//	{
//		// must be same strand
//		if (result1.mappedstrand != result2.mappedstrand)
//			return false;
//		if (!result1.overlap(result2))
//			return true;
////		boolean reverseResult = false;
////		if (result1.mappedstrand == -1)
////		{
////			result1.reverse();
////			result2.reverse();
////			reverseResult = true;
////		}
//		boolean debug = false;
////		if (result1.id.equalsIgnoreCase("2126043"))
////			debug = true;
//		if (result1.mappedstart > result2.mappedstart)
//		{
//			OptMapResultNode tmpresult = result1;
//			result1 = result2;
//			result2 = tmpresult;
//		}
//		// naive trim
//		// pair 1: 
//		if (debug)
//		{
//			System.out.println("=======Start========");
//			System.out.printf("%d\t%d\t%d\t%d\t%s\n", result1.subfragstart, result1.subfragstop, result1.subrefstart, result1.subrefstop, result1.cigar.getPrecigar());
//			System.out.printf("%d\t%d\t%d\t%d\t%s\n", result2.subfragstart, result2.subfragstop, result2.subrefstart, result2.subrefstop, result2.cigar.getPrecigar());
//			System.out.println("======================");
//		}
//		double bestscore = Double.NEGATIVE_INFINITY;
//		int bestTrim1 = -1;
//		int bestTrim2 = -1;
//		for (int trimmed = 1; trimmed <= maxTrim; trimmed++)
//		{
//			for (int trim1 = 0; trim1 <= trimmed; trim1++)
//			{
//				int trim2 = trimmed - trim1;
//				OptMapResultNode tresult1 = new OptMapResultNode(result1);
//				OptMapResultNode tresult2 = new OptMapResultNode(result2);
//				
//				if ((tresult1.cigar.getMatch() - trim1 >= 5) && (tresult2.cigar.getMatch() - trim2 >= 5)) // at least 5 matches
//				{
//					tresult1.trimResult(trim1 * -1, optrefmap);
//					tresult2.trimResult(trim2, optrefmap);
//					tresult1.updateScore(optrefmap, 5, 2, 2);
//					tresult2.updateScore(optrefmap, 5, 2, 2);
//					if (debug)
//					{
//						System.out.printf("%d\t%d\t%b\t%.2f\t%.2f\n", trim1, trim2, tresult1.overlap(tresult2), tresult1.mappedscore, tresult2.mappedscore);
//						System.out.printf("%d\t%d\t%d\t%d\t%s\n", tresult1.subfragstart, tresult1.subfragstop, tresult1.subrefstart, tresult1.subrefstop, tresult1.cigar.getPrecigar());
//						System.out.printf("%d\t%d\t%d\t%d\t%s\n", tresult2.subfragstart, tresult2.subfragstop, tresult2.subrefstart, tresult2.subrefstop, tresult2.cigar.getPrecigar());
//						System.out.println("--------------------------");
//					}
//					if (!tresult1.overlap(tresult2))
//						if (tresult1.mappedscore + tresult2.mappedscore > bestscore)
//						{
//							bestscore = tresult1.mappedscore + tresult2.mappedscore;
//							bestTrim1 = trim1;
//							bestTrim2 = trim2;
//						}
//				}
//			}
//		}
//		if (bestTrim1 == -1 || bestscore < 50)
//			return false;
//		else
//		{
//			
////			if (result1.id.equalsIgnoreCase("2466029"))
////			{
////				System.out.println("Inside");
////				System.out.printf("bestTrim1 %d; BestTrim2 %d\n", bestTrim1, bestTrim2);
////				System.out.printf("1: %d-%d, %s\n", result1.subfragstart, result1.subfragstop, result1.cigar.getPrecigar());
////				System.out.printf("2: %d-%d, %s\n", result2.subfragstart, result2.subfragstop, result2.cigar.getPrecigar());
////			}
//			
//			result1.trimResult(bestTrim1 * -1, optrefmap);
//			result2.trimResult(bestTrim2, optrefmap);
//			result1.updateScore(optrefmap, 5, 2, 2);
//			result2.updateScore(optrefmap, 5, 2, 2);
//			if (debug)
//			{
//				System.out.println("======================");
//				System.out.printf("%d\t%d\t%d\t%d\t%s\n", result1.subfragstart, result1.subfragstop, result1.subrefstart, result1.subrefstop, result1.cigar.getPrecigar());
//				System.out.printf("%d\t%d\t%d\t%d\t%s\n", result2.subfragstart, result2.subfragstop, result2.subrefstart, result2.subrefstop, result2.cigar.getPrecigar());
//				System.out.println("======================");
//			}
////			if (result1.id.equalsIgnoreCase("2466029"))
////			{
////				System.out.println("Inside 2");
////				System.out.printf("bestTrim1 %d; BestTrim2 %d\n", bestTrim1, bestTrim2);
////				System.out.printf("1: %d-%d, %s\n", result1.subfragstart, result1.subfragstop, result1.cigar.getPrecigar());
////				System.out.printf("2: %d-%d, %s\n", result2.subfragstart, result2.subfragstop, result2.cigar.getPrecigar());
////			}
////			if (reverseResult)
////			{
////				result1.reverse();
////				result2.reverse();
////			}
//			return true;
//		}
//	}

}
