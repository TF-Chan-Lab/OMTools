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


package aldenjava.opticalmapping.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import aldenjava.opticalmapping.data.data.DataCovNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;



public class ResultStatistics {
	LinkedHashMap<String, DataNode> optrefmap = null;
	LinkedHashMap<String, DataCovNode> optrefcovmap = null;
	
	LinkedHashMap<String, Integer> alignedMaps;
	LinkedHashMap<String, Set<String>> alignedMolecules;
	LinkedHashMap<String, DataNode> dataInfo;
	LinkedHashMap<String, Long> alignedLength;
	
	HashSet<String> appearedMolecules;
	
	int correctMappedMolecules;
	int unalignedRefl;
	long unalignedLength; 
	
	private boolean checkStrand;
	
	public ResultStatistics(LinkedHashMap<String, DataNode> optrefmap, LinkedHashMap<String, DataNode> dataInfo) {
		this.optrefmap = optrefmap;
		this.dataInfo = dataInfo;
		this.reset();
	}
	
	public void reset()	{
		if (optrefmap != null) {
			optrefcovmap = new LinkedHashMap<>();
			for (DataNode data : optrefmap.values())
				optrefcovmap.put(data.name, new DataCovNode(data)); 
		}

		alignedMaps = new LinkedHashMap<String, Integer>();
		alignedMolecules = new LinkedHashMap<String, Set<String>>();
		alignedLength = new LinkedHashMap<String, Long>();
		if (optrefcovmap != null) {
			for (String key : optrefcovmap.keySet())
				alignedMaps.put(key, 0);
			for (String key : optrefcovmap.keySet())
				alignedMolecules.put(key, new HashSet<>());
			for (String key : optrefcovmap.keySet())
				alignedLength.put(key, 0L);
		}
		correctMappedMolecules = 0;
		unalignedRefl = 0;
		unalignedLength = 0;
		appearedMolecules = new HashSet<>();
	}
	int x = 1;
	
	public int getAlignedSegments(int cov) {
		int totalAlignedSegments = 0;
		for (DataCovNode refcov : optrefcovmap.values()) {
			totalAlignedSegments += refcov.getAlignedRefl(cov);
		}
		return totalAlignedSegments;
	}
	public double getAlignedSegmentRatio(int cov) {
		int totalAlignedSegments = 0;
		int totalSegments = 0;
		for (DataCovNode refcov : optrefcovmap.values()) {
			totalAlignedSegments += refcov.getAlignedRefl(cov);
			totalSegments += refcov.getTotalSegment() - 2; // because the last two segments are not aligned
		}
		return totalAlignedSegments / (double) totalSegments;
	}
	
	public void update(List<OptMapResultNode> resultlist) {
		DataNode data = resultlist.get(0).parentFrag;		
		appearedMolecules.add(data.name);
		if (resultlist.get(0).isUsed()) {
			// Simulation statistics
			boolean correct = false;
			for (OptMapResultNode result : resultlist) {
				if (result.correctlyMapped())
					correct = true;
				if (!checkStrand) {
					result.mappedstrand *= -1;
					if (result.correctlyMapped())
						correct = true;
					result.mappedstrand *= -1;
				}
			}
			if (correct)
				correctMappedMolecules++;

		
			HashSet<String> alignedRef = new HashSet<String>();
//			int alignedRefl = 0;
			boolean[] alignedRefl = new boolean[data.getTotalSegment()];
			for (OptMapResultNode result : resultlist) {
				alignedRef.add(result.mappedRegion.ref);
				if (!alignedMaps.containsKey(result.mappedRegion.ref))
					alignedMaps.put(result.mappedRegion.ref, 0);
				alignedMaps.put(result.mappedRegion.ref, alignedMaps.get(result.mappedRegion.ref) + 1);
				if (!alignedLength.containsKey(result.mappedRegion.ref))
					alignedLength.put(result.mappedRegion.ref, 0L);
				alignedLength.put(result.mappedRegion.ref, alignedLength.get(result.mappedRegion.ref) + (result.mappedRegion.stop - result.mappedRegion.start + 1));
				if (optrefcovmap != null) {
					optrefcovmap.get(result.mappedRegion.ref).update(result);
				}
				if (result.mappedstrand == 1)
					for (int i = result.subfragstart; i <= result.subfragstop; i++)
						alignedRefl[i] = true;
				else
					for (int i = result.subfragstop; i <= result.subfragstart; i++)
						alignedRefl[i] = true;
			}
			for (int i = 1; i < data.getTotalSegment() - 1; i++)
				if (!alignedRefl[i]) {
					unalignedLength += data.getRefl(i);
					unalignedRefl++;
				}
			
			for (String s : alignedRef) {
				if (!alignedMolecules.containsKey(s))
					alignedMolecules.put(s, new HashSet<>());
				alignedMolecules.get(s).add(data.name);
			}
		}
		else {
			if (resultlist.get(0).parentFrag.getTotalSegment() > 2) {
				unalignedRefl += data.getTotalSegment() - 2;
				for (int i = 1; i < data.getTotalSegment() - 1; i++)
					unalignedLength += data.getRefl(i);
			}
		}
	}
	
	public void update(LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {
		for (List<OptMapResultNode> resultlist : resultlistmap.values())
			this.update(resultlist);
	}
	
	public int getTotalMoleculeSegments() {
		
		int total = 0;
		for (DataNode data : dataInfo.values())
			total += data.getTotalSegment() - 2;
		return total;
	}
	public int getUnalignedMoleculeSegments() {
		int total = unalignedRefl;
		for (DataNode data : dataInfo.values())
			if (!appearedMolecules.contains(data.name))
				total += data.getTotalSegment() - 2;
		return total;
	}
	public double getUnalignedMoleculeSegmentsRatio() {
		return this.getUnalignedMoleculeSegments() / (double) this.getTotalMoleculeSegments();
	}
	
	public String getStatisticsString(String name)
	{
		int totalMaps = 0;
		int totalMolecules = 0;
		long totalLength = 0;
		int[] covarray = new int[]{
				1, 3, 5, 10	
			};
		
		Set<String> alignedMoleculeSets = new HashSet<>();
		for (String key : alignedMolecules.keySet())
			alignedMoleculeSets.addAll(alignedMolecules.get(key));
		totalMolecules = alignedMoleculeSets.size();
		for (String key : alignedMaps.keySet())
			totalMaps += alignedMaps.get(key);
		for (String key : alignedLength.keySet())
			totalLength += alignedLength.get(key);
					
	
		
		String finalString = "";
		
		long size = 0;
		if (optrefcovmap != null) {
			for (DataCovNode refcov : optrefcovmap.values())
				size += refcov.size;
		}		
		finalString += (String.format("%s\t%d\t%d\t%d\t%f\t%f\t%f\t%d\t%d\t%.4f", name, size, dataInfo.size(), totalMolecules, totalMolecules / (double) dataInfo.size(), correctMappedMolecules / (double) totalMolecules, correctMappedMolecules / (double) dataInfo.size(), totalMaps, totalLength, totalLength / (double) size));
		if (optrefcovmap != null)
			for (int cov : covarray) {
				long alignedLen = 0;
				long totalLen = 0;
				for (DataCovNode refcov : optrefcovmap.values()) {
					alignedLen += refcov.getAlignedLength(cov);
					totalLen += refcov.size;
				}
				finalString += (String.format("\t%.4f", alignedLen / (double) totalLen));
			}
			finalString += ("\n");
		return finalString;
	}
	
	
	public static void main (String[] args) throws IOException
	{
		ExtendOptionParser parser = new ExtendOptionParser(ResultStatistics.class.getSimpleName(), "Generates statistics for alignment results");
		
		ReferenceReader.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser, 1);
		OptMapResultReader.assignOptions(parser, 1);
		parser.addHeader("Result Stat Options", 1);
		OptionSpec<Boolean> ocheckstrand = parser.accepts("checkstrand", "Checking strand for correctness").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		OptionSpec<String> ocovout = parser.accepts("covout", "Coverage output (Under construction)").withRequiredArg().ofType(String.class);
		OptionSpec<String> ostatout = parser.accepts("statout", "Statistics output").withRequiredArg().ofType(String.class);
		if (args.length == 0)
		{
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		
		LinkedHashMap<String, DataNode> optrefmap = null;
		if (options.has("refmapin"))
			optrefmap = ReferenceReader.readAllData(options);
		
		List<String> optmapins = null;
		if (options.has("optmapin")) {
			optmapins = (List<String>) options.valuesOf("optmapin");
		}
		List<String> optresins = (List<String>) options.valuesOf("optresin");
		if (optmapins != null && optresins.size() != optmapins.size()) {
			System.err.println("Wrong size maching between molecule and result files");
			return;
		}
		BufferedWriter covw = null;
		if (options.has(ocovout))
			covw = new BufferedWriter(new FileWriter(options.valueOf(ocovout)));
		BufferedWriter statw = null;
		if (options.has(ostatout))
			statw = new BufferedWriter(new FileWriter(options.valueOf(ostatout)));
		statw.write(String.format("#Name\tReferenceSize\tTotalMolecules\tAlignedMolecules\tMappingRate\tAccuracy\tAccurateMappingRate\tPartialMaps\tTotalAlignedSize\tCoverage\tCoveredPercent-1\t-3\t-5\t-10\n"));
		
		boolean checkStrand = options.valueOf(ocheckstrand);
		for (int i = 0; i < optresins.size(); i++)
		{
			LinkedHashMap<String, DataNode> dataMap = OptMapDataReader.readAllData(optmapins.get(i));
			ResultStatistics resultstat = new ResultStatistics(optrefmap, dataMap);
			resultstat.checkStrand = checkStrand;
//			if (optmapins != null)
//				resultstat.importFragmentInfo(OptMapDataReader.readAllData(optmapins.get(i)));
			
			
			OptMapResultReader omrr = new OptMapResultReader(optresins.get(i));
			if (options.has("optmapin"))
				omrr.importFragInfo(dataMap);
			List<OptMapResultNode> resultlist;
			while ((resultlist = omrr.readNextList()) != null)
				resultstat.update(resultlist);
			omrr.close();
//			resultstat.process();
			if (statw != null)
				statw.write(resultstat.getStatisticsString(optresins.get(i)));
			if (covw != null)
				;
//				covw.write(resultstat.getCoverageString());
		}
		if (covw != null)
			covw.close();
		if (statw != null)
			statw.close();
	}
}
