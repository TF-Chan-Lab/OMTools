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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

/**
 * The base class for all segmental structural variation nodes. For every SV, they should have a pair of exact break point bp1 and bp2.
 * 
 * @author Alden
 *
 */
public abstract class SVNode {

	protected GenomicPosNode bp1;
	protected GenomicPosNode bp2;

	public List<SVEvidence> evidence = new ArrayList<SVEvidence>();
	public List<OptMapResultNode> bp1_antiEvidence = new ArrayList<OptMapResultNode>();
	public List<OptMapResultNode> bp2_antiEvidence = new ArrayList<OptMapResultNode>();

	public int unsupported = 0;
	private long flankSize1 = 0;
	private long flankSize2 = 0;

	public SVNode(GenomicPosNode bp1, GenomicPosNode bp2) {
		this.bp1 = bp1;
		this.bp2 = bp2;
	}

	public SVNode(List<? extends SVNode> svList) {
		boolean zTEST_useDominateCoordinate = true;

		for (SVNode sv : svList)
			evidence.addAll(sv.evidence);
		/*
		 * String ref1 = null; String ref2 = null; long totalStart1 = 0; long totalStart2 = 0; for (SVNode sv : svList) { // System.out.println(sv.bp1.toString()); // System.out.println(ref1); // System.out.println(totalStart1); if (ref1 == null) { ref1 = sv.bp1.ref; totalStart1 = sv.bp1.start; } else if (!ref1.equalsIgnoreCase("undefined")) if (ref1.equalsIgnoreCase(sv.bp1.ref)) totalStart1 += sv.bp1.start; else { ref1 = "undefined"; totalStart1 = -1; }
		 * 
		 * if (ref2 == null) { ref2 = sv.bp2.ref; totalStart2 = sv.bp2.start; } else if (!ref2.equalsIgnoreCase("undefined")) if (ref2.equalsIgnoreCase(sv.bp2.ref)) totalStart2 += sv.bp2.start; else { ref2 = "undefined"; totalStart2 = -1; } } GenomicPosNode bp1 = new GenomicPosNode(ref1, totalStart1 / svList.size(), totalStart1 / svList.size()); GenomicPosNode bp2 = new GenomicPosNode(ref2, totalStart2 / svList.size(), totalStart2 / svList.size());
		 */
		GenomicPosNode bp1 = null;
		GenomicPosNode bp2 = null;
		long flankSize1 = 0;
		long flankSize2 = 0;
		if (zTEST_useDominateCoordinate) {
			List<GenomicPosNode> bp1list = new ArrayList<GenomicPosNode>();
			List<GenomicPosNode> bp2list = new ArrayList<GenomicPosNode>();
			List<Integer> supportlist = new ArrayList<Integer>();
			for (SVNode sv : svList) {
				boolean found = false;
				for (int i = 0; i < bp1list.size(); i++) {
					if (sv.bp1.equals(bp1list.get(i)) && sv.bp2.equals(bp2list.get(i))) {
						supportlist.set(i, supportlist.get(i) + 1);
						found = true;
						break;
					}
				}
				if (!found) {
					bp1list.add(sv.bp1);
					bp2list.add(sv.bp2);
					supportlist.add(1);
				}
			}
			int highest = -1;
			int highestindex = -1;
			for (int i = 0; i < supportlist.size(); i++)
				if (supportlist.get(i) > highest) {
					highest = supportlist.get(i);
					highestindex = i;
					bp1 = bp1list.get(highestindex);
					bp2 = bp2list.get(highestindex);
				}
			// later to add the smaller the bp difference the better
			// else
			// if (supportlist.get(i) == highest)
			// {
			// GenomicPosNode old = new GenomicPosNode();
			// }
			if (highestindex == -1) {
				bp1 = null;
				bp2 = null;
			} else {
				int totalSelectedSV = 0;
				long totalFlankSize1 = 0;
				long totalFlankSize2 = 0;
				long minFlankSize1 = Long.MAX_VALUE;
				long minFlankSize2 = Long.MAX_VALUE;
				
				for (SVNode sv : svList)
					if (sv.bp1.equals(bp1) && sv.bp2.equals(bp2)) {
						totalFlankSize1 += sv.getFlank1();
						totalFlankSize2 += sv.getFlank2();
						minFlankSize1 = Math.min(minFlankSize1, sv.getFlank1());
						minFlankSize2 = Math.min(minFlankSize2, sv.getFlank2());
						totalSelectedSV++;
					}
//				flankSize1 = totalFlankSize1 / totalSelectedSV;
//				flankSize2 = totalFlankSize2 / totalSelectedSV;
				flankSize1 = minFlankSize1;
				flankSize2 = minFlankSize2;
			}
		}

		this.bp1 = bp1;
		this.bp2 = bp2;
		this.flankSize1 = flankSize1;
		this.flankSize2 = flankSize2;
	}

	public void importEvidence(OptMapResultNode result1, OptMapResultNode result2) {
		// *incomplete. Only use the latest result as flanksize
		evidence.add(new SVEvidence(result1, result2));
		flankSize1 = result1.getMapLength();
		flankSize2 = result2.getMapLength();
	}

	public boolean isSameType(SVNode sv) {
		return getType().equalsIgnoreCase(sv.getType());
	}

	public boolean isSimilarSV(SVNode sv, long closeSV) {
		// check same type of sv
		if (!isSameType(sv))
			return false;
		// check close break points
		if (!(bp1.isClose(sv.bp1, closeSV) && bp2.isClose(sv.bp2, closeSV)))
			return false;
		return true;
	}

	public double getScore() {
		return getSupport() / (double) (getSupport() + getAntiSupport());
		// return 0;
	}

	public abstract String getType();

	public String getZygosity() {
		return "0";
	}

	public GenomicPosNode getSVRegion() {
		if (bp1.ref.equalsIgnoreCase(bp2.ref))
			return new GenomicPosNode(bp1.ref, bp1.start, bp2.stop);
		else
			return null;
	}

	public int getSupport() {
		return evidence.size();
	}

	public int getAntiSupport() {
		return getAntiEvidenceIDList().size();
	}

	public void removeInvalidAntiEvidence() {
		HashSet<String> evidenceHash = new HashSet<String>();
		for (SVEvidence e : evidence)
			evidenceHash.add(e.result1.parentFrag.name);
		List<OptMapResultNode> newList = new ArrayList<OptMapResultNode>();
		// remove duplicate and evidence
		HashSet<String> bpHash = new HashSet<String>();
		for (OptMapResultNode result : bp1_antiEvidence) {
			if (!bpHash.contains(result.parentFrag.name) && !evidenceHash.contains(result.parentFrag.name)) {
				bpHash.add(result.parentFrag.name);
				newList.add(result);
			}
		}
		bp1_antiEvidence = newList;

		newList = new ArrayList<OptMapResultNode>();
		bpHash = new HashSet<String>();
		for (OptMapResultNode result : bp2_antiEvidence) {
			if (!bpHash.contains(result.parentFrag.name) && !evidenceHash.contains(result.parentFrag.name)) {
				bpHash.add(result.parentFrag.name);
				newList.add(result);
			}
		}
		bp2_antiEvidence = newList;
	}

	public List<String> getAntiEvidenceIDList() {
		HashSet<String> bpHash = new HashSet<String>();
		for (OptMapResultNode result : bp1_antiEvidence)
			bpHash.add(result.parentFrag.name);
		for (OptMapResultNode result : bp2_antiEvidence)
			bpHash.add(result.parentFrag.name);
		return new ArrayList<String>(bpHash);

	}

	public long getFlank1() {
		return flankSize1;
	}

	public long getFlank2() {
		return flankSize2;
	}

	/**
	 * A utility to combine the SV List to build a consensus SVNode
	 * 
	 * @param svList
	 * @return
	 */
	public static SVNode buildSVNode(List<SVNode> svList) {
		if (svList == null)
			throw new NullPointerException("SV List is null.");
		if (svList.isEmpty())
			throw new IllegalArgumentException("SV List could not be empty");
		SVNode source = svList.get(0);
		for (SVNode sv : svList)
			if (!sv.getType().equals(source.getType()))
				throw new IllegalArgumentException("All SV must be of the same types in the SV List.");
		
		switch (source.getType()) {
			case "Insertion":
			case "Deletion":
			case "VirtualIndel":
				List<IndelNode> newIndelList = new ArrayList<IndelNode>();
				for (SVNode sv : svList)
					newIndelList.add((IndelNode) sv);
				return (new IndelNode(newIndelList));
			case "Translocation":
				List<TranslocationNode> newTranslocList = new ArrayList<TranslocationNode>();
				for (SVNode sv : svList)
					newTranslocList.add((TranslocationNode) sv);
				return (new TranslocationNode(newTranslocList));
			case "Inversion":
				List<InversionNode> newInversionList = new ArrayList<InversionNode>();
				for (SVNode sv : svList)
					newInversionList.add((InversionNode) sv);
				return (new InversionNode(newInversionList));
			default:
				throw new IllegalArgumentException("Unknown SV Type " + source.getType());
		}
	}

	// Static Comparator
	public static Comparator<SVNode> typeComparator = new Comparator<SVNode>() {
		@Override
		public int compare(SVNode sv1, SVNode sv2) {
			int comparison;
			comparison = sv1.getType().compareTo(sv2.getType());
			if (comparison != 0)
				return comparison;
			else {
				comparison = bp1Comparator.compare(sv1, sv2);
				if (comparison != 0)
					return comparison;
				else {
					comparison = bp2Comparator.compare(sv1, sv2);
					if (comparison != 0)
						return comparison;
					else {
						if ((sv1 instanceof IndelNode) && (sv2 instanceof IndelNode))
							return Integer.compare(((IndelNode) sv1).getIndelSize(), ((IndelNode) sv2).getIndelSize());
						else
							return comparison;
					}
				}
			}
		}

	};

	public static Comparator<SVNode> bp1Comparator = new Comparator<SVNode>() {
		@Override
		public int compare(SVNode sv1, SVNode sv2) {
			return sv1.bp1.compareTo(sv2.bp1);
		}

	};
	public static Comparator<SVNode> bp2Comparator = new Comparator<SVNode>() {
		@Override
		public int compare(SVNode sv1, SVNode sv2) {
			return sv1.bp2.compareTo(sv2.bp2);
		}

	};

}
