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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.Cigar;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.assembler.LocalConsensusPattern;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.DataSignalCountNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.data.ReferenceSignal;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.mapper.MatchHelper;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.SelectableMode;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import joptsimple.OptionSet;

public class SVDetection implements SelectableMode {

	// private final LinkedHashMap<String, ReferenceNode> optrefmap;
	// flanking breakpoint
	private LinkedHashMap<String, DataNode> optrefmap;
	private int svmode;

	private long closeFragment = 250000;
	private long closeReference = 250000;
	private long closeSV = 10000;

	private int minIndelSize = 1000;
	private int maxIndelSize = 1000000;

	private int minsupport = 5;
	private int maxsupport = 100;
	private double minsvscore = 0.0;
	private boolean mergeSV = true;

	private int flankSig = 5;
	private long flankSize = 0;

	private int minInvSignal = 4;
	private int maxInvSize = 100000;
	private int deg = 1000;
	private double ear = 0.1;
	private int meas = 500;
	private double fp = 1e-5;
	private double fn = 0.125;
	private double pvalueThreshold = 1e-9;
	private double likelihoodRatio = 1e-6;

	public SVDetection(LinkedHashMap<String, DataNode> optrefmap) {
		this.optrefmap = optrefmap;
	}

	public void setParameters(OptionSet options) {
		this.setParameters((long) options.valueOf("closefrag"), (long) options.valueOf("closeref"), (long) options.valueOf("closesv"), (long) options.valueOf("flanksize"),
				(int) options.valueOf("flanksig"), (int) options.valueOf("minindelsize"), (int) options.valueOf("maxindelsize"), (int) options.valueOf("mininvsig"), (int) options.valueOf("maxinvsize"),
				(int) options.valueOf("minsupport"), (int) options.valueOf("maxsupport"), (double) options.valueOf("minsvscore"), (boolean) options.valueOf("mergesv"), (int) options.valueOf("deg"),
				(int) options.valueOf("meas"), (double) options.valueOf("ear"), (double) options.valueOf("fp"), (double) options.valueOf("fn"), (double) options.valueOf("p"),
				(double) options.valueOf("likelihood"));
	}

	public void setParameters(long closeFragment, long closeReference, long closeSV, long flankSize, int flankSig, int minIndelSize, int maxIndelSize, int minInvSignal, int maxInvSize, int minsupport, int maxsupport,
			double minsvscore, boolean mergeSV, int deg, int meas, double ear, double fp, double fn, double pvalueThreshold, double likelihoodRatio) {
		this.closeFragment = closeFragment;
		this.closeReference = closeReference;
		this.closeSV = closeSV;
		this.flankSize = flankSize;
		this.flankSig = flankSig;
		this.minIndelSize = minIndelSize;
		this.maxIndelSize = maxIndelSize;
		this.minInvSignal = minInvSignal;
		this.maxInvSize = maxInvSize;
		this.minsupport = minsupport;
		this.maxsupport = maxsupport;
		this.minsvscore = minsvscore;
		this.mergeSV = mergeSV;
		this.deg = deg;
		this.meas = meas;
		this.ear = ear;
		this.fp = fp;
		this.fn = fn;
		this.pvalueThreshold = pvalueThreshold;
		this.likelihoodRatio = likelihoodRatio;
	}

	@Override
	public void setMode(OptionSet options) {
		this.svmode = (int) options.valueOf("svmode");
	}

	@Override
	public void setMode(int svmode) {
		this.svmode = svmode;
	}

	@Override
	public int getMode() {
		return svmode;
	}

	public List<SVNode> detectSV(List<OptMapResultNode> resultlist) {
		List<SVNode> svList = new ArrayList<SVNode>();
		if (resultlist.get(0).isUsed()) {
			Collections.sort(resultlist, OptMapResultNode.subfragstartstopcomparator);
			OptMapResultNode prevResult = null;
			for (OptMapResultNode result : resultlist) {
				if (result.getMatch() < flankSig)
					continue; // Filtering those alignment with few signals, i.e. not confident
				if (prevResult != null) {
					if (result.isFragClose(prevResult, closeFragment)) {

						GenomicPosNode bp1;
						GenomicPosNode bp2;

						if (prevResult.mappedstrand == 1)
							bp1 = new GenomicPosNode(prevResult.mappedRegion.ref, prevResult.mappedRegion.stop, prevResult.mappedRegion.stop);
						else
							bp1 = new GenomicPosNode(prevResult.mappedRegion.ref, prevResult.mappedRegion.start, prevResult.mappedRegion.start);

						if (result.mappedstrand == 1)
							bp2 = new GenomicPosNode(result.mappedRegion.ref, result.mappedRegion.start, result.mappedRegion.start);
						else
							bp2 = new GenomicPosNode(result.mappedRegion.ref, result.mappedRegion.stop, result.mappedRegion.stop);

						if (bp1.compareTo(bp2) > 0) {
							GenomicPosNode tmp = bp1;
							bp1 = bp2;
							bp2 = tmp;

						}
						if (result.isRefClose(prevResult, closeReference)) {
							// indel
							if (result.mappedstrand == prevResult.mappedstrand) {

								int refSize = (int) Math.abs(bp1.start - bp2.start);
								int fragSize;
								if (result.mappedstrand == 1) {
									fragSize = (int) (result.length(prevResult.subfragstop + 1, result.subfragstart - 1) / ((result.getMapScale() + prevResult.getMapScale()) / 2));
								} else {
									fragSize = (int) (result.length(prevResult.subfragstart + 1, result.subfragstop - 1) / ((result.getMapScale() + prevResult.getMapScale()) / 2));
								}

								IndelNode indel = new IndelNode(bp1, bp2, refSize, fragSize);
								indel.importEvidence(prevResult, result);
								svList.add(indel);
							} else {
								// inversion
								// Coordinate may have problem
								InversionNode inversion = new InversionNode(bp1, bp2);
								inversion.importEvidence(prevResult, result);
								svList.add(inversion);
							}

						} else {// translocation
							TranslocationNode translocation = new TranslocationNode(bp1, bp2);
							translocation.importEvidence(prevResult, result);
							svList.add(translocation);
						}
					}
				}
				prevResult = result;
			}
		}
		return svList;
	}

	public List<SVNode> groupSV(List<SVNode> svList) {
		Collections.sort(svList, SVNode.typeComparator);
		List<Integer> groupPointer = new ArrayList<Integer>();
		List<List<Integer>> groupedSVList = new ArrayList<List<Integer>>();
		for (int i = 0; i < svList.size(); i++) {
			SVNode currentSV = svList.get(i);
			// find all similar SVs first
			HashSet<Integer> similarSVGroup = new HashSet<Integer>();
			for (int j = i - 1; j >= 0; j--) {
				SVNode prevSV = svList.get(j);

				if (!prevSV.bp1.isClose(currentSV.bp1, closeReference)) {
					break;
				} else if (prevSV.isSimilarSV(currentSV, closeSV))
					similarSVGroup.add(groupPointer.get(j));
			}
			// Merge SV Group and add current SVs
			List<Integer> similarSVGroupList = new ArrayList<Integer>(similarSVGroup);
			Collections.sort(similarSVGroupList);
			if (!similarSVGroupList.isEmpty()) {
				// Joining all group to the big group
				int bigGroup = similarSVGroupList.get(0);
				for (int k = 1; k < similarSVGroupList.size(); k++) {
					for (Integer svIndex : groupedSVList.get(similarSVGroupList.get(k))) {
						groupPointer.set(svIndex, bigGroup);
						groupedSVList.get(bigGroup).add(svIndex);
					}
					groupedSVList.set(similarSVGroupList.get(k), null);
				}
				groupPointer.add(bigGroup);
				groupedSVList.get(bigGroup).add(i);
			} else {
				List<Integer> newGroup = new ArrayList<Integer>();
				newGroup.add(i);
				groupedSVList.add(newGroup);
				groupPointer.add(groupedSVList.size() - 1);
			}
		}

		// From merged SV group, construct single SV for a group
		List<SVNode> combinedList = new ArrayList<SVNode>();
		for (List<Integer> group : groupedSVList)
			if (group != null) {
				List<SVNode> newSVList = new ArrayList<SVNode>();
				for (Integer i : group)
					newSVList.add(svList.get(i));
				combinedList.add(SVNode.buildSVNode(newSVList));
			}
		return combinedList;
	}

	public List<SVNode> getFilteredSV(List<SVNode> svList) {
		List<SVNode> newSVList = new ArrayList<SVNode>();
		for (SVNode sv : svList)
			if (sv.evidence.size() >= minsupport && sv.evidence.size() <= maxsupport)
				if (sv.getScore() >= minsvscore) {
					if (sv.getType().equalsIgnoreCase("Insertion") || sv.getType().equalsIgnoreCase("Deletion")) {
						if (Math.abs(((IndelNode) sv).getIndelSize()) >= minIndelSize && Math.abs(((IndelNode) sv).getIndelSize()) <= maxIndelSize)
							newSVList.add(sv);
					}
					// remove, not to call translocation and split-map inversion
					else if (true)
						newSVList.add(sv);

				}

		return newSVList;
	}

	public List<SVNode> mergeSimilarSV(List<SVNode> svList) {
		// More rough but can filter unwanted duplicating SV
		List<SVNode> newSVList = new ArrayList<SVNode>();
		Collections.sort(svList, SVNode.typeComparator);
		SVNode lastSV = null;
		List<IndelNode> mergeSVList = new ArrayList<IndelNode>();
		for (SVNode sv : svList) {
			if (!(sv.getType().equalsIgnoreCase("Insertion") || (sv.getType().equalsIgnoreCase("Deletion")))) {
				newSVList.add(sv);
				continue;
			}
			if (lastSV != null) {

				if (!((IndelNode) sv).isSimilarSV((IndelNode) lastSV, closeSV)) {
					SVNode newsv = new IndelNode(mergeSVList);
					newSVList.add(newsv);
					mergeSVList = new ArrayList<IndelNode>();
				}
			}
			mergeSVList.add((IndelNode) sv);
			lastSV = sv;
		}
		if (mergeSVList.size() > 0) {
			SVNode newsv = new IndelNode(mergeSVList);
			newSVList.add(newsv);
		}
		return newSVList;
	}

	public void updateAntiEvidence(List<SVNode> svList, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {

		List<OptMapResultNode> mergedresultlist = new ArrayList<OptMapResultNode>();
		for (List<OptMapResultNode> resultlist : resultlistmap.values())
			for (OptMapResultNode result : resultlist)
				mergedresultlist.add(result);
		Collections.sort(mergedresultlist, OptMapResultNode.mappedstartcomparator);

		for (SVNode sv : svList) {
			sv.bp1_antiEvidence = new ArrayList<OptMapResultNode>();
			sv.bp2_antiEvidence = new ArrayList<OptMapResultNode>();
		}
		int sv_index = 0;
		Collections.sort(svList, SVNode.bp1Comparator);
		for (OptMapResultNode result : mergedresultlist) {
			for (int i = sv_index; i < svList.size(); i++) {
				long flankSize = this.flankSize;
				SVNode sv = svList.get(i);
				if (flankSize < 0)
					flankSize = sv.getFlank1();
				if (sv.bp1.isClose(result.mappedRegion, 0)) {
					if (sv.bp1.start - result.mappedRegion.start >= flankSize && result.mappedRegion.stop - sv.bp1.start >= flankSize) {
						List<OptMapResultNode> rList = result.getBreakResult(optrefmap, sv.bp1);
						boolean flank = true;
						if (rList.size() == 2) {
							for (OptMapResultNode r : rList)
								if (r.getMatch() < flankSig)
									flank = false;
						} else
							flank = false;
						if (flank)
							sv.bp1_antiEvidence.add(result);
					}
				} else if (!sv.bp1.ref.equalsIgnoreCase(result.mappedRegion.ref)) {
					if (sv.bp1.ref.compareToIgnoreCase(result.mappedRegion.ref) < 0)
						sv_index++;
					else
						break;
				} else if (sv.bp1.stop < result.mappedRegion.start)
					sv_index++;
				else if (!sv.bp1.ref.equalsIgnoreCase(result.mappedRegion.ref) || sv.bp1.start > result.mappedRegion.stop)
					break;

			}
		}

		Collections.sort(svList, SVNode.bp2Comparator);
		for (OptMapResultNode result : mergedresultlist) {
			for (int i = sv_index; i < svList.size(); i++) {
				long flankSize = this.flankSize;
				SVNode sv = svList.get(i);
				if (flankSize < 0)
					flankSize = sv.getFlank2();
				if (sv.bp2.isClose(result.mappedRegion, 0)) {
					if (sv.bp2.start - result.mappedRegion.start >= flankSize && result.mappedRegion.stop - sv.bp2.start >= flankSize) {
						List<OptMapResultNode> rList = result.getBreakResult(optrefmap, sv.bp2);
						boolean flank = true;
						if (rList.size() == 2) {
							for (OptMapResultNode r : rList)
								if (r.getMatch() < flankSig)
									flank = false;
						} else
							flank = false;
						if (flank)
							sv.bp2_antiEvidence.add(result);
					}
				} else if (!sv.bp2.ref.equalsIgnoreCase(result.mappedRegion.ref) || sv.bp2.stop < result.mappedRegion.start)
					sv_index++;
			}
		}
		for (SVNode sv : svList)
			sv.removeInvalidAntiEvidence();
	}

	public List<SVNode> inversionFilter(List<SVNode> svList) {
		List<SVNode> newSVList = new ArrayList<SVNode>();

		SVNode prev = null;
		for (SVNode sv : svList) {
			if (sv.getType().equalsIgnoreCase("Inversion"))
				if (prev != null)
					if (prev.bp2.overlapSize(sv.bp1) < 0 && prev.bp2.isClose(sv.bp1, closeFragment)) {
						((InversionNode) prev).importSecondInversionNode((InversionNode) sv);
						newSVList.add(prev);
						prev = null;
					} else
						prev = sv;
				else
					prev = sv;
			else {
				newSVList.add(sv);
				// prev = null;
			}

		}
		return newSVList;
	}

	private List<StandardSVNode> splitMapSVDetection(LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {

		List<SVNode> svList = new ArrayList<SVNode>();
		for (List<OptMapResultNode> resultlist : resultlistmap.values())
			svList.addAll(this.detectSV(resultlist));
		svList = groupSV(svList);
		svList = getFilteredSV(svList); // first round filtering to reduce time for antievidence calculation
		if (mergeSV)
			svList = mergeSimilarSV(svList);

		updateAntiEvidence(svList, resultlistmap);
		svList = getFilteredSV(svList);
		// Inversion Filtering
		// Remove the inversion filter temporarily
//		svList = inversionFilter(svList);

		List<StandardSVNode> standardSVList = new ArrayList<StandardSVNode>();
		for (SVNode sv : svList) {
			standardSVList.add(new StandardSVNode(sv));

		}

		return standardSVList;
	}

	/*
	private double ncr(int n, int k) {
		double result = 1;
		for (int a = 1; a <= n; a++)
			result *= a;
		for (int a = 1; a <= n - k; a++)
			result /= a;
		for (int a = 1; a <= k; a++)
			result /= a;
		return result;
	}

	private double getFNpH0(int matched, int base) {
		double pH0 = 0;
		for (int x = 0; x <= matched; x++) {
			double factor = ncr(base, x);
			for (int a = 0; a < x; a++)
				factor *= (1 - fn);
			for (int a = 0; a < base - x; a++)
				factor *= (fn);
			pH0 += factor;
		}
		return pH0;
	}

	private double getFNLikelihoodH0(int matched, int base) {
		double factorMm = ncr(base, matched);
		double likelihoodH0 = factorMm;
		for (int a = 0; a < matched; a++)
			likelihoodH0 *= (1 - fn);
		for (int a = 0; a < base - matched; a++)
			likelihoodH0 *= fn;
		return likelihoodH0;
	}

	private double getFNLikelihoodHomo(int matched, int base, long len) {
		double factorMm = ncr(base, matched);
		double likelihoodhomo = factorMm;
		for (int a = 0; a < matched; a++)
			likelihoodhomo *= (fp * len);
		for (int a = 0; a < base - matched; a++)
			likelihoodhomo *= (1 - fp * len);
		return likelihoodhomo;
	}

	private double getFNLikelihoodHete(int matched, int base, long len) {
		double likelihoodhete = 0;
		for (int k = 0; k <= base; k++) {
			double factorMk = ncr(base, k);
			for (int a = 0; a < base; a++)
				factorMk *= 0.5;
			double sum = 0;
			for (int l = Math.max(0, matched - base + k); l <= Math.min(k, matched); l++) {
				double factor = 1;
				factor *= ncr(base, k);
				for (int a = 0; a < l; a++)
					factor *= (1 - fn);
				for (int a = 0; a < k - l; a++)
					factor *= fn;
				factor *= ncr(base - k, matched - l);
				for (int a = 0; a < matched - l; a++)
					factor *= (fp * len);
				for (int a = 0; a < base - k - matched + l; a++)
					factor *= (1 - fp * len);
				sum += factor;
			}
			factorMk *= sum;
			likelihoodhete += factorMk;
		}
		return likelihoodhete;
	}

	private double getFPpH0(int extra, int base, long len) {
		double pH0 = 0;
		for (int x = extra; x <= base; x++) {
			double factor = ncr(base, x);
			for (int a = 0; a < x; a++)
				factor *= (fp * len);
			for (int a = 0; a < base - x; a++)
				factor *= (1 - fp * len);
			pH0 += factor;
		}
		return pH0;
	}

	private double getFPLikelihoodH0(int extra, int base, long len) {
		return getFNLikelihoodHomo(extra, base, len);
	}

	private double getFPLikelihoodHomo(int extra, int base) {
		return getFNLikelihoodH0(extra, base);
	}

	private double getFPLikelihoodHete(int extra, int base, long len) {
		return getFNLikelihoodHete(extra, base, len);
	}
	*/
	private List<StandardSVNode> signalIndelSVDetection(LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {
		// Signal Deletion Calling
		VerbosePrinter.println("Detecting signal deletions...");
		List<StandardSVNode> standardSVList = new ArrayList<StandardSVNode>();
		HashSet<ReferenceSignal> lostSig = new HashSet<ReferenceSignal>();
		LinkedHashMap<String, DataSignalCountNode> optrefsigmap = DataSignalCountNode.initialize(optrefmap);
		for (List<OptMapResultNode> resultlist : resultlistmap.values())
			for (OptMapResultNode result : resultlist) {
				int limit1 = flankSig;
				int matchSigPos = 0;
				int limit2 = result.getMatch() - flankSig + 1;
				String precigar = result.cigar.getPrecigar();
				int currentrefpos = result.subrefstart;
				DataSignalCountNode rsig = optrefsigmap.get(result.mappedRegion.ref);
				for (char c : precigar.toCharArray()) {
					switch (c) {
						case 'M':
							matchSigPos++;
							if (matchSigPos > limit1 && matchSigPos < limit2) {
								rsig.refpSigMatchCount[currentrefpos - 1]++;
								rsig.refpCount[currentrefpos - 1]++;
								rsig.refpevidencelist.get(currentrefpos - 1).add(result);
							}
							currentrefpos += 1;
							break;
						case 'D':
							if (matchSigPos > limit1 && matchSigPos < limit2) {
								rsig.refpCount[currentrefpos - 1]++;
								rsig.refpevidencelist.get(currentrefpos - 1).add(result);
							}
							currentrefpos += 1;
							break;
						default:
							;
					}
				}

			}
		for (DataSignalCountNode rsig : optrefsigmap.values()) {
			DataNode newref = new DataNode(optrefmap.get(rsig.name));

			for (int i = rsig.reflCount.length - 1; i >= 0; i--) {
				if (i > 0) {
					int matched = rsig.refpSigMatchCount[i - 1];
					int base = rsig.refpCount[i - 1];

//					double pH0 = this.getFNpH0(matched, base);
//					if (pH0 > pvalueThreshold)
//						continue;
//					double likelihoodH0 = getFNLikelihoodH0(matched, base);
//					double likelihoodhomo = getFNLikelihoodHomo(matched, base, 100);
//					double likelihoodhete = getFNLikelihoodHete(matched, base, 100);
					String zygosity = "";
//					zygosity = likelihoodhomo >= likelihoodhete ? "Homozygous" : "Heterozygous";
//					if (pH0 <= pvalueThreshold && likelihoodH0 / Math.max(likelihoodhomo, likelihoodhete) <= likelihoodRatio) {
					if ((base - matched >= minsupport) && matched / (double) base <= 1 - minsvscore) {
						boolean passResolutionLimit = true;
						if (i > 1)
							if (newref.refp[i - 1] - newref.refp[i - 2] <= deg)
								passResolutionLimit = false;
						if (i < newref.refp.length)
							if (newref.refp[i] - newref.refp[i - 1] <= deg)
								passResolutionLimit = false;
						if (passResolutionLimit) {
							standardSVList
									.add(new StandardSVNode(new GenomicPosNode(newref.name, newref.refp[i - 1]), "Deletion.site", "", new ArrayList<String>(), "", new LinkedHashMap<String, Object>(),
											zygosity, "", (base - matched) / (double) base, "TotalSupport=" + Integer.toString(base - matched) + ";TotalOppose=" + Integer.toString(matched), ""));
							boolean successfulAddition = lostSig.add(new ReferenceSignal(rsig.name, i - 1));
							assert successfulAddition;
						}
					}

				}
			}
		}

		// Signal Insertion Calling
		// Note that this module depends on some info in Signal Deletion Calling
		// Define if: ---|---x---x---| if x are missing signals, count last refl
		VerbosePrinter.println("Detecting signal insertions...");
		for (List<OptMapResultNode> resultlist : resultlistmap.values())
			for (OptMapResultNode result : resultlist) {
				String precigar = result.cigar.getPrecigar();
				int currentrefpos = result.subrefstart;
				boolean continuousRefMatch = false;
				int internalAdditionalSig = 0;
				int limit1 = flankSig;
				int matchSigPos = 0;
				int limit2 = result.getMatch() - flankSig + 1;
				DataSignalCountNode rsig = optrefsigmap.get(result.mappedRegion.ref);
				for (char c : precigar.toCharArray()) {
					switch (c) {
						case 'M':
							matchSigPos++;
							if (matchSigPos > limit1 && matchSigPos < limit2) {
								if (!lostSig.contains(new ReferenceSignal(rsig.name, currentrefpos - 1))) {
									if (continuousRefMatch) {
										if (internalAdditionalSig > 0)
											rsig.reflExtraSigCount[currentrefpos - 1]++;
										rsig.reflCount[currentrefpos - 1]++;
										rsig.reflevidencelist.get(currentrefpos - 1).add(result);
									}
									continuousRefMatch = true;
								} else
									continuousRefMatch = false;
							}
							currentrefpos += 1;

							internalAdditionalSig = 0;
							break;
						case 'I':
							internalAdditionalSig++;
							break;
						case 'D':
							if (!lostSig.contains(new ReferenceSignal(rsig.name, currentrefpos - 1)))
								continuousRefMatch = false;
							currentrefpos += 1;
							break;
						default:
							;
					}
				}

			}

		for (DataSignalCountNode rsig : optrefsigmap.values()) {
			DataNode newref = new DataNode(optrefmap.get(rsig.name));
			for (int i = rsig.reflCount.length - 2; i > 0; i--) {
				int extra = rsig.reflExtraSigCount[i];
				int base = rsig.reflCount[i];
				int lastSignal = i - 1;
				while (lostSig.contains(new ReferenceSignal(rsig.name, lastSignal))) {
					lastSignal--; // Must find a refp and never get down to -1 or it's bug above
					assert lastSignal >= 0;
				}
				// comparison
//				double pH0 = this.getFPpH0(extra, base, 100);
//				if (pH0 > pvalueThreshold)
//					continue;
//				double likelihoodH0 = getFPLikelihoodH0(extra, base, 100);
//				double likelihoodhomo = getFPLikelihoodHomo(extra, base);
//				double likelihoodhete = getFPLikelihoodHete(extra, base, 100);

//				if (pH0 <= pvalueThreshold && likelihoodH0 / Math.max(likelihoodhomo, likelihoodhete) <= likelihoodRatio) {
				if (extra >= minsupport && extra / (double) base >= minsvscore) {
					List<DataNode> reflist = new ArrayList<DataNode>();

					for (OptMapResultNode result : rsig.reflevidencelist.get(i)) {

						result.updateMSP();
						int a = -1;
						int b = -1;
						for (int x = 0; x < result.mspsR.size(); x++) {
							if (result.mspsR.get(x).rpos == lastSignal)
								a = result.mspsR.get(x).qpos;
							if (result.mspsR.get(x).rpos == i)
								b = result.mspsR.get(x).qpos;
						}
						DataNode f;
						if (result.mappedstrand == 1) {
							f = result.parentFrag.subRefNode(a + 1, b, false);
						} else {
							f = result.parentFrag.subRefNode(b + 1, a, false);
							f.reverse();
						}
						reflist.add(f);
					}
					DataSignalCountNode consensusRef = LocalConsensusPattern.consensusMap(reflist, newref.getRefl(i));

					for (int k = 0; k < consensusRef.refp.length; k++) {
						long p = consensusRef.refp[k];
						int support = consensusRef.refpCount[k];
//						double inpH0 = this.getFPpH0(support, base, 100);
//						if (pH0 > pvalueThreshold)
//							continue;
//						double inlikelihoodH0 = getFPLikelihoodH0(support, base, 100);
//						double inlikelihoodhomo = getFPLikelihoodHomo(support, base);
//						double inlikelihoodhete = getFPLikelihoodHete(support, base, 100);
						String zygosity = "";
//						zygosity = likelihoodhomo >= likelihoodhete ? "Homozygous" : "Heterozygous";

//						if (!(inpH0 <= pvalueThreshold && inlikelihoodH0 / Math.max(inlikelihoodhomo, inlikelihoodhete) <= likelihoodRatio))
						if (support < minsupport || support / (double) base < minsvscore) // second round validation after consensus map
							continue;
						long closestSignalDist = Long.MAX_VALUE;
						long pos = (i > 0 ? newref.refp[lastSignal] : 0) + p;
						boolean passResolutionLimit = true;
						int j = newref.findRefpIndex(pos);
						if (j < newref.refp.length)
//							if (!lostSig.contains(new ReferenceSignal(rsig.name, j)))
								if (closestSignalDist > Math.abs(newref.refp[j] - pos))
									closestSignalDist = Math.abs(newref.refp[j] - pos);
						j--;
						if (j >= 0)
//							if (!lostSig.contains(new ReferenceSignal(rsig.name, j)))
								if (closestSignalDist > Math.abs(newref.refp[j] - pos))
									closestSignalDist = Math.abs(newref.refp[j] - pos);
						if (closestSignalDist <= deg)
							passResolutionLimit = false;
						if (passResolutionLimit)
							standardSVList.add(new StandardSVNode(new GenomicPosNode(newref.name, pos), "Insertion.site", "", new ArrayList<String>(), "", new LinkedHashMap<String, Object>(),
									zygosity, "", support / (double) base,
									"TotalSupport=" + Integer.toString(support) + ";TotalOppose=" + Integer.toString(base - support) + ";Resolution=" + closestSignalDist, ""));
					}
					i = lastSignal + 1;
				}
			}
		}

		// Signal Inversion Calling (based on signal indels information)
		standardSVList = callInversion(standardSVList);
		Collections.sort(standardSVList);
		return standardSVList;
	}

	public List<StandardSVNode> standardSVDetection(LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {
		List<StandardSVNode> standardSVList = new ArrayList<>();
		switch (svmode) {
			case 1:
				standardSVList.addAll(this.splitMapSVDetection(resultlistmap));
				break;
			case 2:
				standardSVList.addAll(this.signalIndelSVDetection(resultlistmap));
				break;
			case 3:
				standardSVList.addAll(this.splitMapSVDetection(resultlistmap));
				standardSVList.addAll(this.signalIndelSVDetection(resultlistmap));
				break;
			default:
				;
		}
		Collections.sort(standardSVList);
		return standardSVList;
	}

	private List<StandardSVNode> removeInversionSignalIndels(List<StandardSVNode> standardSVList, List<DetectedInversionNode> invList) {
		List<StandardSVNode> newList = new ArrayList<>();
		Collections.sort(invList, DetectedInversionNode.startEndComparator);
		int invListIndex = 0;
		for (int i = 0; i < standardSVList.size(); i++) {
			StandardSVNode sv = standardSVList.get(i);
			if (invListIndex < invList.size()) {
				DetectedInversionNode inv = invList.get(invListIndex);
				if (i < inv.startSVIndex) // signal indels are only added if they do not overlap the current inversion
					newList.add(sv);
				if (i == inv.stopSVIndex)
					invListIndex++;
			} else // remaining signal indels won't overlap inversions
				newList.add(sv);
		}
		return newList;
	}

	private List<StandardSVNode> parseInversionNode(List<StandardSVNode> standardSVList, List<DetectedInversionNode> invList) {
		List<StandardSVNode> newList = new ArrayList<>();
		for (DetectedInversionNode inv : invList) {
			assert standardSVList.get(inv.startSVIndex).region.ref.equals(standardSVList.get(inv.stopSVIndex).region.ref);
			LinkedHashMap<String, Object> sv_attribute = new LinkedHashMap<>();
			sv_attribute.put("signalInvolved", Integer.toString(inv.cigar.length()));
			sv_attribute.put("indelSignalInvolved", Integer.toString(inv.cigar.getFP()));
			int support = 100;
			int oppose = 0;
			newList.add(new StandardSVNode(
					new GenomicPosNode(standardSVList.get(inv.startSVIndex).region.ref, standardSVList.get(inv.startSVIndex).region.start, standardSVList.get(inv.stopSVIndex).region.start),
					"Inversion", "", new ArrayList<String>(), "", sv_attribute, "", "", support / (double) (support + oppose),
					"", ""));
//					"TotalSupport=" + Integer.toString(support) + ";TotalOppose=" + Integer.toString(oppose), ""));
		}
		return newList;
	}

	/**
	 * Detect inversions based on signal indels
	 * 
	 * @param standardSVList
	 *            The list of signal indels
	 * @return A list of inversions and updated signal indels
	 */
	public List<StandardSVNode> callInversion(List<StandardSVNode> standardSVList) {
		Collections.sort(standardSVList);
		List<DetectedInversionNode> invList = new ArrayList<>();
		for (int i = 0; i < standardSVList.size(); i++) {
			Cigar cigar = new Cigar();
			StandardSVNode firstSV = standardSVList.get(i);
			DataNode ref = optrefmap.get(firstSV.region.ref);
			if (firstSV.type.equalsIgnoreCase("Insertion.site"))
				cigar.append('I');
			else if (firstSV.type.equalsIgnoreCase("Deletion.site"))
				cigar.append('D');
			else
				continue;
			//
			int refpPos = ref.findRefpIndex(firstSV.region.start);
			// can have better algorithm here
			long lastPos = firstSV.region.start;
			List<Long> sizes = new ArrayList<>();
			for (int j = i + 1; j < standardSVList.size(); j++) {
				StandardSVNode lastSV = standardSVList.get(j);
				if (!lastSV.region.isClose(firstSV.region, maxInvSize))
					break;

				long pos1 = standardSVList.get(j - 1).region.start;
				long pos2 = standardSVList.get(j).region.start;
				if (ref.refp[refpPos] == pos1) // This signal is not counted.
					refpPos++;
				while (ref.refp[refpPos] < pos2) {
					cigar.append('M');
					sizes.add(ref.refp[refpPos] - lastPos - 1);
					lastPos = ref.refp[refpPos];
					refpPos++;
				}
				sizes.add(lastSV.region.start - lastPos - 1);
				lastPos = lastSV.region.start;

				if (lastSV.type.equalsIgnoreCase("Insertion.site"))
					cigar.append('I');
				else if (lastSV.type.equalsIgnoreCase("Deletion.site"))
					cigar.append('D');
				else
					continue;
				// Check if the current CIGAR meets the criteria (min signals and palindromic)

				if (cigar.length() >= minInvSignal) {
					if (cigar.isPalindromic()) {
						boolean sizeMatch = true;
						for (int sizeIndex = 0; sizeIndex < (sizes.size() + 1) / 2; sizeIndex++)
							if (!MatchHelper.fuzzyMatch(sizes.get(sizeIndex), sizes.get(sizes.size() - sizeIndex - 1), meas, ear)) {
								sizeMatch = false;
								break;
							}
						if (sizeMatch) {
							invList.add(new DetectedInversionNode(ref.name, i, j, new Cigar(cigar)));
						}
					}
				}
			}
		}
		// Sort the inversions by involved false positive signals in descending order
		Collections.sort(invList);
		List<DetectedInversionNode> filteredInvList = new ArrayList<DetectedInversionNode>();
		for (int i = 0; i < invList.size(); i++) {
			DetectedInversionNode inv = invList.get(i);
			boolean overlapped = false;
			for (int j = 0; j < filteredInvList.size(); j++) {
				DetectedInversionNode prevInv = filteredInvList.get(j);
				if (inv.ref.equals(prevInv.ref))
					if (!(prevInv.stopSVIndex < inv.startSVIndex || prevInv.startSVIndex > inv.stopSVIndex)) {
						overlapped = true;
						break;
					}

			}
			if (!overlapped)
				filteredInvList.add(inv);
		}
		// Convert to standard inversion lsit
		List<StandardSVNode> finalInvList = this.parseInversionNode(standardSVList, filteredInvList);
		// Removed signal indels comprising the inversions
		standardSVList = this.removeInversionSignalIndels(standardSVList, filteredInvList);
		// Combine two lists
		standardSVList.addAll(finalInvList);
		return standardSVList;

	}

	public static void assignOptions(ExtendOptionParser parser) {
		parser.addHeader("SV Detection", 1);
		parser.accepts("svmode", "SV Mode. 1: split-map approach (experimental); 2: signal-based approach; 3: both split-map and signal based approach").withRequiredArg().ofType(Integer.class)
				.defaultsTo(3);
		parser.accepts("minsupport", "Minimum molecule support for an SV").withRequiredArg().ofType(Integer.class).defaultsTo(5);		
		parser.accepts("flanksig", "Minimum number of flanking signals for an alignment").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.addHeader("Split-map based detection options", 2);
		parser.accepts("closeref", "The max distance (reference) between two results to be considered at same cluster.").withRequiredArg().ofType(Long.class).defaultsTo(250000L);
		parser.accepts("closefrag", "The max distance (fragment) between two results to be considered at same cluster.").withRequiredArg().ofType(Long.class).defaultsTo(250000L);
		parser.accepts("closesv", "Close SV (SVs are joined based on bp1bp2 comparison.").withRequiredArg().ofType(Long.class).defaultsTo(10000L);
		parser.accepts("mergesv", "Merge SV (SVs are joined based on simple region comparison).").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		parser.accepts("flanksize", "Minimum flanking size in alignment (Used in split-map detection only)").withRequiredArg().ofType(Long.class).defaultsTo(0L);
		parser.accepts("minindelsize", "Minimum indel size for an SV").withRequiredArg().ofType(Integer.class).defaultsTo(1500);
		parser.accepts("maxindelsize", "Maximum indel size for an SV").withRequiredArg().ofType(Integer.class).defaultsTo(1000000);
		parser.accepts("maxsupport", "Maximum molecule support for an SV").withRequiredArg().ofType(Integer.class).defaultsTo(100);
		parser.accepts("minsvscore", "Minimum sv score (ratio of support/opposition").withRequiredArg().ofType(Double.class).defaultsTo(0.5);
		parser.addHeader("Signal-based detection options", 2);
		parser.accepts("mininvsig", "Minimum signal involved in an inversion").withRequiredArg().ofType(Integer.class).defaultsTo(4);
		parser.accepts("maxinvsize", "Maximum inversion size").withRequiredArg().ofType(Integer.class).defaultsTo(100000);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		AlignmentOptions.assignResolutionOptions(parser);
		
		parser.addHeader(null, 0); // To be used in the future
		parser.accepts("fp", "False positive (extra signal) rate").withRequiredArg().ofType(Double.class).defaultsTo(1e-5);
		parser.accepts("fn", "False negative (missing signal) rate").withRequiredArg().ofType(Double.class).defaultsTo(0.125);
		parser.accepts("p", "p-value threshold").withRequiredArg().ofType(Double.class).defaultsTo(1e-9);
		parser.accepts("likelihood", "Likelihood threshold").withRequiredArg().ofType(Double.class).defaultsTo(1e-6);

	}

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(SVDetection.class.getSimpleName(), "Provides a basic SV detection module for from optical mapping alignment");
		ReferenceReader.assignOptions(parser);
		OptMapDataReader.assignOptions(parser);
		OptMapResultReader.assignOptions(parser);
		StandardSVWriter.assignOptions(parser, 1);
		SVDetection.assignOptions(parser);

		parser.addHeader(null, 0);
		parser.accepts("optmapin").withOptionalArg().ofType(String.class);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);
		LinkedHashMap<String, DataNode> optrefmap = ReferenceReader.readAllData(options);
		OptMapResultReader omrr = new OptMapResultReader(options);
		if (options.has("refmapin"))
			omrr.importRefInfo(ReferenceReader.readAllData(options));
		if (options.has("optmapin"))
			omrr.importFragInfo(OptMapDataReader.readAllData(options));

		StandardSVWriter svw = new StandardSVWriter((String) options.valueOf("svout"));
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = omrr.readAllDataInList();
		List<String> unusedKeyList = new ArrayList<String>();
		for (String key : resultlistmap.keySet())
			if (resultlistmap.get(key).isEmpty())
				unusedKeyList.add(key);
			else if (!resultlistmap.get(key).get(0).isUsed())
				unusedKeyList.add(key);
		for (String key : unusedKeyList)
			resultlistmap.remove(key);
		SVDetection svdetection = new SVDetection(optrefmap);
		svdetection.setMode(options);
		svdetection.setParameters(options);
		List<StandardSVNode> standardSVList = svdetection.standardSVDetection(resultlistmap);
		for (StandardSVNode sv : standardSVList)
			svw.write(sv);
		omrr.close();
		svw.close();
	}

}

class DetectedInversionNode implements Comparable<DetectedInversionNode> {

	public final String ref;
	public final int startSVIndex;
	public final int stopSVIndex;
	public final Cigar cigar;

	public DetectedInversionNode(String ref, int startSVIndex, int stopSVIndex, Cigar cigar) {
		super();
		this.ref = ref;
		this.startSVIndex = startSVIndex;
		this.stopSVIndex = stopSVIndex;
		this.cigar = cigar;
	}

	@Override
	public int compareTo(DetectedInversionNode o) {
		return Integer.compare(cigar.getFP(), o.cigar.getFP()) * -1; // more FP signals are on top
	}

	public static Comparator<DetectedInversionNode> startEndComparator = new Comparator<DetectedInversionNode>() {
		@Override
		public int compare(DetectedInversionNode o1, DetectedInversionNode o2) {
			int x = o1.ref.compareTo(o2.ref);
			if (x != 0)
				return x;
			x = Integer.compare(o1.startSVIndex, o2.startSVIndex);
			if (x != 0)
				return x;
			return Integer.compare(o1.stopSVIndex, o2.stopSVIndex);
		}

	};

}
