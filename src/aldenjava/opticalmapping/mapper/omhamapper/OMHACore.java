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


package aldenjava.opticalmapping.mapper.omhamapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.mapper.ExtensionResult;
import aldenjava.opticalmapping.mapper.MatchHelper;

public class OMHACore {

	/**
	 * Main Mapper Class.
	 * 
	 * @author Alden
	 *
	 */
	LinkedHashMap<String, DataNode> optrefmap = null;
	private int falselimit;

	// Fragment Indicator
	private int matchscore;
	private int falseppenalty;
	private int falsenpenalty;
	private boolean allowLocalAlignment;
	private int measure;
	private double ear;

	private int localstart;
	private double scorefilter;

	private int measuretolerance;
	private int degeneracy;

	/* Setup */
	public OMHACore(LinkedHashMap<String, DataNode> optrefmap) {
		this.optrefmap = optrefmap;
	}

	public void setParameters(boolean allowLocalAlignment, int localstart, int falselimit, int scorefilter, int measure, int degeneracy, double ear, int matchscore, int falseppenalty,
			int falsenpenalty) {
		this.falselimit = falselimit;
		this.matchscore = matchscore;
		this.falseppenalty = falseppenalty;
		this.falsenpenalty = falsenpenalty;
		this.allowLocalAlignment = allowLocalAlignment;
		this.measure = measure;
		this.ear = ear;
		this.localstart = localstart;
		this.scorefilter = scorefilter;
		this.measuretolerance = measure * 2;
		this.degeneracy = degeneracy;
	}

	public List<OptMapResultNode> getResult(DataNode fragment) {
		if (fragment.getTotalSegment() < 3)
			return null;
		if (localstart > 0)
			if (localstart > fragment.getTotalSegment() - 2)
				return null;
		if (localstart < 0)
			if (fragment.getTotalSegment() - 2 + localstart <= 0)
				return null;
		List<OptMapResultNode> fragmentmaplist = new ArrayList<OptMapResultNode>();
		// forward
		List<ExtensionResult> forwardlist = mapFragment(fragment);
		for (ExtensionResult extensionresult : forwardlist)
			fragmentmaplist.add(extensionresult.toAlignment(fragment, optrefmap, 1));

		// reverse
		DataNode reversedfragment = fragment.getReverse();
		List<ExtensionResult> reverselist = mapFragment(reversedfragment);
		for (ExtensionResult extensionresult : reverselist)
			fragmentmaplist.add(extensionresult.toAlignment(fragment, optrefmap, -1));

		return fragmentmaplist;
	}

	public ExtensionResult align(DataNode fragment, DataNode ref, int startrefpos, int startfragpos) {
		List<Turn> turn = new ArrayList<Turn>();
		String finalprecigarString = "";
		double finalscale = -1;

		double sub_best_score = Double.NEGATIVE_INFINITY;
		int refl_size = ref.refp.length + 1;

		int k = startrefpos;
		int f = startfragpos;
		int stoprefpos = -1;
		int stopfragpos = -1;

		// 3 attempts are used: first signal is true, first signal is FP, first two signals are FP
		// the introduction of turn is for dealing with the deviation at late false negative
		turn.add(new Turn(k, startfragpos, 0, 0, 0, new StringBuilder("M")));

		// Start Real Mapping
		int turncounter = 0;
		int internalfalseplimit = fragment.getTotalSegment() / 2; // if half of the signals are FP, then the fragment should already be very bad
		while ((turn.size() > turncounter)) // if found then wrong will be false, no need to replicate again
		{

			boolean wrong = false;
			k = turn.get(turncounter).genpos; // start from here (ref pos)
			f = turn.get(turncounter).frapos; // start from here (fragment pos)
			int totalfalsep = turn.get(turncounter).totalfalsep;
			int totalfalsen = turn.get(turncounter).totalfalsen;
			double bestscore = turn.get(turncounter).bestscore;
			// int lastmatchtotalfalsep = totalfalsep;
			// int lastmatchtotalfalsen = totalfalsen;

			StringBuilder precigarString = turn.get(turncounter).precigarString;
			String finalprecigar = "";
			int finalrefpos = -1;
			int finalfragpos = -1;

			long falsen = 0;
			long extrafalsen = 0;
			long falsep = 0;
			long extrafalsep = 0;
			int falsecount = 0;
			boolean toggled = false;
			if (totalfalsep > internalfalseplimit)
				wrong = true;
			while ((k < refl_size - 1) && (f < fragment.getTotalSegment() - 1) && (!(wrong))) // do not check the last sub-fragment
			{
//				double result = comparefrag(fragment.getRefl(f) + falsep, ref.getRefl(k) + falsen, ear, (falsep == 0 ? 1 : 2) * measuretolerance / (double) 2); // to be corrected to change // try using this to repair measuretolerance problem at falsep case
				long querySize = fragment.getRefl(f) + falsep;
				long refSize = ref.getRefl(k) + falsen;
				long measurementTolerance = falsep == 0 ? measuretolerance / 2 : measuretolerance;
				boolean matchResult = MatchHelper.match(refSize, querySize, measurementTolerance, ear);
				//	double measurementTolerance = (falsep == 0 ? 1 : 2) * measuretolerance / (double) 2;
				
				long extraQuerySize = fragment.getRefl(f) + falsep + extrafalsep;
				long extraRefSize = ref.getRefl(k) + falsen + extrafalsen;

				boolean extraMatchResult = MatchHelper.match(extraRefSize, extraQuerySize, measurementTolerance, ear);
				
//				double extraresult = comparefrag(extraQuerySize, extraRefSize, ear, measurementTolerance); // to be corrected to change
				// Extra FP FN System works and successfully mapped
				if (!matchResult && querySize > refSize && extraMatchResult && extrafalsen != 0) // handling result with extra false negatives
				{
					matchResult = true;
					falsen += extrafalsen;
					extrafalsen = 0;
					if (toggled) {
						totalfalsen--; // we don't want to regard it as false negative
					} else {
						totalfalsen++;
						precigarString.append("D");
					}
				}
				if (!matchResult && querySize < refSize && extraMatchResult && extrafalsep != 0) // handling result with extra false positives
				{
					matchResult = true;
					falsep += extrafalsep;
					extrafalsep = 0;
					// totalfalsep--; // false positive in another site, nice, still a false positive...

				}

				// Basic System
				if (!matchResult && querySize < refSize) // fragment < genome size, may have false positive
				{
					falsep += fragment.getRefl(f); // extra signal not included, should be degenerated
					totalfalsep++;
					falsecount++;
					f++;
					precigarString.append("I");
				} else if (!matchResult && querySize > refSize) // fragment > genome size, may have false negative
				{
					// duplicate signal at single position handling
					// if ((falsen == 0) && (degenerate(ref.refl[k], degeneracy) == 0) )
					// {
					// extrafalsen = ref.refl[k]; // may or may not be used in the future // don't support multiple extrafalsen, for later development, may need list to deal with it
					//
					// // test degeneration function loss
					// precigarString.append("D");// add here to test if resolution error occurs
					// totalfalsen++;
					// // /test degeneration function loss
					// toggled = false; // No need for penalty because of too close signal
					// k++;
					// }
					// else
					// normal false negative situation
					{
						falsen += ref.getRefl(k); // extra signal not included, should be degenerated
						k++;
						totalfalsen++;
						falsecount++;
						precigarString.append("D");
					}
				} else // Match
				{
					assert matchResult;
					extrafalsen = 0;
					extrafalsep = 0;
					// Dealing with extra FP FN concern
					if (f < fragment.getTotalSegment() - 2) // Extra false positives problems
					{
						long tmpQuerySize = fragment.getRefl(f) + falsep + fragment.getRefl(f + 1);
						long tmpRefSize = ref.getRefl(k) + falsen;
						long tmpMeasurementTolerance = falsep == 0 ? measuretolerance / 2 : measuretolerance;
						boolean tmpMatchResult = MatchHelper.match(tmpRefSize, tmpQuerySize, tmpMeasurementTolerance, ear);
						// may have bugs for tmpresult when extraresult is used instead of result
//						double tmpresult = comparefrag(fragment.getRefl(f) + falsep + fragment.getRefl(f + 1), ref.getRefl(k) + falsen, ear, (falsep == 0 ? 1 : 2) * measuretolerance / 2);

						// double tmpresult = comparefrag(fragment.fragment[f] + falsep + fragment.fragment[f + 1], refl[k] + falsen, ear, measuretolerance);
						if (tmpMatchResult) {
							// R |---------|----
							// F |--------|-|---
							if (MatchHelper.match(ref.getRefl(k + 1), fragment.getRefl(f + 1), measuretolerance / 2, ear))
//							tmpresult = comparefrag(fragment.getRefl(f + 1), ref.getRefl(k + 1), ear, measuretolerance / 2);
//							if (tmpresult != -1 && tmpresult != -2)
								// R A----------B--C---
								// F a--------b--c-d
								// two consecutive perfect map, or one false positive exist? AB:ac, BC:cd OR AB:ab, BC:bc, ....?
								turn.add(new Turn(k + 1, f + 2, totalfalsep + 1, totalfalsen, bestscore, new StringBuilder(precigarString + "IM")));
							else {
								// extrafalsep should be enough to handle
								extrafalsep = fragment.getRefl(f + 1);
								f++;// * if used, will get wrong extra totalfalsen
								totalfalsep++;
								precigarString.append("I");

							}
						}
					}
					if ((k < refl_size - 2)) // Extra false negatives problems
					{
						// may have bugs for tmpresult when extraresult is used instead of result
						// double tmpresult = comparefrag(fragment.fragment[f] + falsep, refl[k] + falsen + refl[k + 1], ear, measuretolerance);
						long tmpQuerySize = fragment.getRefl(f) + falsep;
						long tmpRefSize = ref.getRefl(k) + falsen + ref.getRefl(k + 1);
						long tmpMeasurementTolerance = falsep == 0 ? measuretolerance / 2 : measuretolerance;
						boolean tmpMatchResult = MatchHelper.match(tmpRefSize, tmpQuerySize, tmpMeasurementTolerance, ear);

//						double tmpresult = comparefrag(fragment.getRefl(f) + falsep, ref.getRefl(k) + falsen + ref.getRefl(k + 1), ear, (falsep == 0 ? 1 : 2) * measuretolerance / 2);
						if (tmpMatchResult)
						// remove degeneration function
						// if (degenerate(ref.refl[k + 1], degeneracy) != 0) // if == 0, then can be dealt with from other parts
						{
							if (MatchHelper.match(ref.getRefl(k + 1), fragment.getRefl(f + 1), measuretolerance / 2, ear))
//							tmpresult = comparefrag(fragment.getRefl(f + 1), ref.getRefl(k + 1), ear, measuretolerance / 2); // further check: if next fragment and next genome are matched properly, we need to separate two cases and so, add turns
							// tmpresult = comparefrag(fragment.fragment[f + 1], refl[k + 1], ear, measuretolerance);
//							if (f < fragment.getTotalSegment() - 2 && tmpresult != -1 && tmpresult != -2)
								// turn.add(new Turn(k + 2, f + 1, totalfalsep, totalfalsen + 1, skippedsize, new ArrayList<Double>()));
								turn.add(new Turn(k + 2, f + 1, totalfalsep, totalfalsen + 1, bestscore, new StringBuilder(precigarString + "DM")));
							else {
								extrafalsen = ref.getRefl(k + 1);
								k++;// * if used, will get wrong extra totalfalsen
								totalfalsen++;
								precigarString.append("D");

								// toggled is a boolean for too close signals
								// extrafalsen most likely used when two signals are too close to be identify as two on fragments
								//
								toggled = true;
								// toggledlen = precigarString.length();
							}
						}
					}
					precigarString.append("M");
					// proceed

					// double score = (allowLocalAlignment?
					// caldiffScore(f - startfragpos + 1 - totalfalsep, totalfalsep, totalfalsen, ref.length(startrefpos, k), fragment.length(startfragpos, f), 1):
					// caldiffScore(f - startfragpos + 1 - totalfalsep, totalfalsep, totalfalsen, ref.length(startrefpos, k), fragment.length(startfragpos, f), (f - startfragpos + 1) / (double) (fragment.totalsubfragment - 2)));
					// score+= matchscore because every match of length receives marks,
					// but score now is contributed by number of signal,
					double score = (allowLocalAlignment ? caldiffScore(f - startfragpos + 1 - totalfalsep, totalfalsep, totalfalsen, ref.length(startrefpos, k), fragment.length(startfragpos, f), 1)
							: caldiffScore(f - startfragpos + 1 - totalfalsep, totalfalsep, totalfalsen, ref.length(startrefpos, k), fragment.length(startfragpos, f), (f - startfragpos + 1)
									/ (double) (fragment.getTotalSegment() - 2)));
					if (score >= bestscore || !allowLocalAlignment) {
						bestscore = score;
						finalrefpos = k;
						finalfragpos = f;
						finalprecigar = precigarString.toString();
						// lastmatchtotalfalsep = totalfalsep;
						// lastmatchtotalfalsen = totalfalsen;
					}
					// if (score < 0 && allowLocalAlignment)
					// wrong = true;
					f++;
					k++;
					falsep = 0;
					falsen = 0;
					falsecount = 0;
				}
				// heuristic criteria to prevent large quantity of calculations
				if (falsecount > falselimit) // consecutive error limit)
					wrong = true;
				if (totalfalsep > internalfalseplimit)
					wrong = true;

			}
			// if (f < fragment.totalsubfragment - 1) // not all sub-fragments visited
			// totalfalsep += fragment.totalsubfragment - 1 - f; // all remaining are false positives!
			// if (totalfalsep > internalfalseplimit)
			// wrong = true;
			// if (k == startrefpos) // all are false positives on the fragment... non-sense match
			// wrong = true;

			// int index = precigarString.length() - 1;
			// while (index >= 0)
			// {
			// if (precigarString.charAt(index) == 'I')
			// finalfragpos--;
			// else
			// if (precigarString.charAt(index))
			// finalrefpos--
			if (k != startrefpos && finalrefpos != -1 && finalfragpos != -1) {
				// Score Calculation
				// double curr_score = - falsenpenalty * lastmatchtotalfalsen - falseppenalty * lastmatchtotalfalsep;
				// int index = 0;
				// int totalrefpos = finalrefpos - startrefpos;
				// int totalfragpos = finalfragpos - startfragpos;

				long refsize = ref.length(startrefpos, finalrefpos);
				long fragsize = fragment.length(startfragpos, finalfragpos);
				// curr_score = caldiffScore(fragment.totalsubfragment - 2 - totalfalsep, totalfalsep, totalfalsen, refsize, fragsize);

				if (bestscore > sub_best_score || (bestscore == sub_best_score && stopfragpos < finalfragpos)) {
					sub_best_score = bestscore;
					finalscale = (double) fragsize / (double) refsize;
					// reorganize precigar string to prevent problems of 9M3I, wrong final fp and fn
					// while (index < precigarString.length())
					// {
					// switch (precigarString.charAt(index))
					// {
					// case 'M':
					// totalrefpos--;
					// totalfragpos--;
					// break;
					// case 'D':
					// totalrefpos--;
					// break;
					// case 'I':
					// totalfragpos--;
					// break;
					// default:
					// }
					// if (totalfragpos < 0 && totalrefpos < 0) // must be simutaneously less than 0
					// break;
					// else
					// if (totalfragpos < 0 || totalrefpos < 0)
					// {
					// System.out.println("Wrong!Wrong!");
					// }
					// }

					// finalprecigarString = precigarString.substring(0, index);
					finalprecigarString = finalprecigar;
					stoprefpos = finalrefpos;
					stopfragpos = finalfragpos;
				}
			}
			turncounter++;
		}
		// return sub_best_score_turn;
		if (sub_best_score > Double.NEGATIVE_INFINITY) {
			return new ExtensionResult(ref.name, startrefpos, startfragpos, stoprefpos, stopfragpos, finalprecigarString, sub_best_score, finalscale);
		} else
			return null;
	}

	private List<ExtensionResult> mapFragment(DataNode fragment) {
		List<ExtensionResult> resultlist = new ArrayList<ExtensionResult>();
		int initialstartfragpos = 1;
		int finalstartfragpos = localstart;
		if (finalstartfragpos < 0)
			finalstartfragpos += fragment.getTotalSegment() - 2;
		if (finalstartfragpos == 0)
			finalstartfragpos = fragment.getTotalSegment() - 2;
		if (finalstartfragpos > fragment.getTotalSegment() - 2)
			finalstartfragpos = fragment.getTotalSegment() - 2;
		for (DataNode ref : optrefmap.values()) {
			List<HashSet<Integer>> omitlist = new ArrayList<HashSet<Integer>>();
			for (int i = 0; i < ref.refp.length + 1; i++)
				omitlist.add(new HashSet<Integer>(finalstartfragpos));
			for (int i = 1; i < ref.refp.length + 1 - 1; i++)
				for (int j = initialstartfragpos; j <= finalstartfragpos; j++)
					if (!omitlist.get(i).contains(j)) {
						ExtensionResult result = align(fragment, ref, i, j);
						if (result != null) {
							if (result.score >= scorefilter)
								resultlist.add(result);
							// omit list starts!
							char[] cigararray = result.precigar.toCharArray();
							int omitrefpos = i;
							int omitfragpos = j;
							int score = matchscore;
							for (char c : cigararray) {
								if (c == 'M') {
									score += matchscore;
									if (score >= 0) {
										omitlist.get(omitrefpos).add(omitfragpos);
										omitrefpos++;
										omitfragpos++;
									}

								} else if (c == 'I') {
									omitfragpos++;
									score -= falseppenalty;
								} else if (c == 'D') {
									omitrefpos++;
									score -= falsenpenalty;
								}
							}
						}
					}
		}
		return resultlist;

	}

	// private long degenerate(long x, int d) // d: measuretolerance factor
	// {
	// if (x % d >= (double) d / 2)
	// x += d;
	// x = d * (int) (x / d);
	// return x;
	// }
/*
	private double comparefrag(long f1, long f2, double f2acceptablefactor, double extrarange) {
		// prepare to correct the following: f2 larger, then f2 as base; f1 larger then f1 as base
		// if (((f1 <= f2 * (1 + f2acceptablefactor) + extrarange) && (f1 >= f2 * (1 - f2acceptablefactor) - extrarange) && f1 <= f2)
		// ||
		// ((f2 <= f1 * (1 + f2acceptablefactor) + extrarange) && (f2 >= f1 * (1 - f2acceptablefactor) - extrarange) && f1 > f2))
		if ((f1 <= f2 * (1 + f2acceptablefactor) + extrarange) && (f1 >= f2 * (1 - f2acceptablefactor) - extrarange))
			// if ((f1 <= (f2 + extrarange) * (1 + f2acceptablefactor)) && (f1 >= (f2 - extrarange) * (1 - f2acceptablefactor)))
			return ((double) f1 / (double) f2);
		else if (f1 < f2)
			return -1; // f1 << f2 // false positive, fragment plus
		else
			return -2; // f1 >> f2 // false negative, genome plus
	}
*/
	private double caldiffScore(int totalmapped, int totalfalsep, int totalfalsen, long refsize, long optsize, double fragmentusedratio) {
		double factor = (Math.min(refsize, optsize) / (double) Math.max(refsize, optsize));
		return (matchscore * totalmapped - falsenpenalty * totalfalsen - falseppenalty * totalfalsep) * factor * fragmentusedratio;
	}

	public OMHACore copy() {
		OMHACore core = new OMHACore(optrefmap);
		core.setParameters(allowLocalAlignment, localstart, falselimit, (int) scorefilter, measure, degeneracy, ear, matchscore, falseppenalty, falsenpenalty);
		return core;
	}
}

class Turn {
	public int genpos;
	public int frapos;
	public int totalfalsen;
	public int totalfalsep;
	public double bestscore;
	public StringBuilder precigarString;

	public Turn(int genpos, int frapos, int totalfalsep, int totalfalsen, double bestscore, StringBuilder precigarString) {
		this.genpos = genpos;
		this.frapos = frapos;
		this.totalfalsep = totalfalsep;
		this.totalfalsen = totalfalsen;
		this.bestscore = bestscore;
		this.precigarString = precigarString;
	}
}