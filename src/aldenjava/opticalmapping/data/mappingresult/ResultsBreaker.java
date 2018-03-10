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


package aldenjava.opticalmapping.data.mappingresult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.Cigar;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.mapper.MatchHelper;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.MissingInformationException;
import aldenjava.opticalmapping.miscellaneous.SelectableMode;

public class ResultsBreaker implements SelectableMode {

	private LinkedHashMap<String, DataNode> optrefmap;
	private int breakermode;
	private int meas;
	private double ear;
	private int match;
	private int fpp;
	private int fnp;

	public ResultsBreaker(LinkedHashMap<String, DataNode> optrefmap)
	{
		this.optrefmap = optrefmap;
	}
	@Override
	public void setMode(OptionSet options) {
		this.setMode((int) options.valueOf("breakermode"));		
	}
	@Override
	public void setMode(int mode) {
		// TODO Auto-generated method stub
		this.breakermode = mode;
	}
	@Override
	public int getMode() {
		return this.breakermode;
	}

	public void setParameters(OptionSet options)
	{
		setParameters((int) options.valueOf("meas"),
				(double) options.valueOf("ear"), 
				(int) options.valueOf("match"), 
				(int) options.valueOf("fpp"), 				
				(int) options.valueOf("fnp"));
	}
	public void setParameters(int meas, double ear, int match, int fpp, int fnp)
	{
		this.meas = meas;
		this.ear = ear;
		this.match = match;
		this.fpp = fpp;
		this.fnp = fnp;
	}
	public List<OptMapResultNode> breakResult(OptMapResultNode result) throws MissingInformationException, ResultsBreakingException
	{
		List<OptMapResultNode> resultList = new ArrayList<OptMapResultNode>();
		switch (breakermode)
		{
		case 0:
			resultList.add(result);
			return resultList;
		case 1:
			if (result.parentFrag == null || optrefmap == null)
				throw new MissingInformationException();
			if (!result.isUsed())
			{
				resultList.add(result);
				return resultList;
			}
		
//			ReferenceNode ref = optrefmap.get(result.mappedRegion.ref);
			try {
				resultList.addAll(result.getBreakResult(optrefmap, meas, ear));
			}
			catch (IllegalArgumentException e) {
				throw new ResultsBreakingException("Exception in result breaking");
			}
			for (OptMapResultNode r : resultList) {
				r.updateScore(optrefmap, match, fpp, fnp);
				r.confidence = result.confidence;
			}
			return resultList;
		default:
			resultList.add(result);
			return resultList;
		}
	}
	
	public List<OptMapResultNode> joinResult(List<OptMapResultNode> resultList, boolean rangeCheck) throws MissingInformationException {
		if (resultList == null)
			throw new NullPointerException();
		if (resultList.isEmpty())
			return resultList;
		if (!resultList.get(0).isUsed())
			return resultList;
		OptMapResultNode r0 = resultList.get(0);
		for (OptMapResultNode result : resultList) {
			if (!result.mappedRegion.ref.equals(result.mappedRegion.ref))
				throw new UnsupportedOperationException("Results from different reference could not be joined.");
			if (r0.mappedstrand != result.mappedstrand)
				throw new UnsupportedOperationException("Results with different mappedstrand could not be joined.");
		}
		
		Collections.sort(resultList, OptMapResultNode.mappedstartcomparator);
		List<List<OptMapResultNode>> lists = new ArrayList<>();
		List<OptMapResultNode> currentList = new ArrayList<>();
		
		currentList.add(resultList.get(0));
		for (int i = 0; i < resultList.size() - 1; i++) {
			OptMapResultNode r1 = resultList.get(i);
			OptMapResultNode r2 = resultList.get(i + 1);
			if ((!r1.mappedRegion.ref.equals(r2.mappedRegion.ref)) ||
				(r1.mappedstrand != r2.mappedstrand) || 
				(rangeCheck && !MatchHelper.match(r2.mappedRegion.start - r1.mappedRegion.stop - 1, 
						r2.mappedstrand == 1 ? (r2.getMoleMappedRegion().start - r1.getMoleMappedRegion().stop - 1) : (r1.getMoleMappedRegion().start - r2.getMoleMappedRegion().stop - 1), 
								meas, ear))) {
				lists.add(currentList);
				currentList = new ArrayList<>();
			}
			
			currentList.add(r2);
		}
		lists.add(currentList);
		
		List<OptMapResultNode> finalList = new ArrayList<>();
		for (List<OptMapResultNode> list : lists) {
			int subfragstart = list.get(0).subfragstart;
			int subfragstop = list.get(list.size() - 1).subfragstop;
			int subrefstart = list.get(0).subrefstart;
			int subrefstop = list.get(list.size() - 1).subrefstop;
			Cigar cigar = new Cigar();
			for (int i = 0; i < list.size() - 1; i++) {
				OptMapResultNode r1 = list.get(i);
				OptMapResultNode r2 = list.get(i + 1);
				int missingSignal = r2.subrefstart - r1.subrefstop - 2;
				int extraSignal = Math.abs(r2.subfragstart - r1.subfragstop) - 2;
				assert missingSignal >= 0;
				assert extraSignal >= 0;
				
				cigar.append(r1.cigar);
				// Here, we could consider matching the position to build better cigar
				for (int j = 0; j < missingSignal; j++)
					cigar.append('D');
				for (int j = 0; j < extraSignal; j++)
					cigar.append('I');
			}
			cigar.append(list.get(list.size() - 1).cigar);
			OptMapResultNode newResult = new OptMapResultNode(r0.parentFrag, null, r0.mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, 0, r0.confidence);
			newResult.updateMappedRegion(optrefmap.get(r0.mappedRegion.ref));
			newResult.updateScore(optrefmap, match, fpp, fnp);
			finalList.add(newResult);
		}
		return finalList; 
	}
	/*
	public OptMapResultNode joinResult(List<OptMapResultNode> resultList) throws MissingInformationException
	{
		// The method is not finished
		class ResultScore {
			private int support;
			private double score;
			private List<OptMapResultNode> resultList;
			public ResultScore() {
				support = 0;
				score = 0;
				resultList = new ArrayList<OptMapResultNode>();
			}
			public void addAlignment(OptMapResultNode result) {
				support++;
				score += result.mappedscore;
				resultList.add(result);
			}
			
			public OptMapResultNode getRepresentative() {
				if (resultList.isEmpty())
					throw new RuntimeException("result list is empty!");
				return resultList.get(0);
			}
			
		}
		Comparator<ResultScore> resultScoreScoreComparator = new Comparator<ResultScore>() {
			@Override
			public int compare(ResultScore rs1, ResultScore rs2) {
				return Double.compare(rs1.score, rs2.score);
			}
			
		};
		
		if (resultList == null)
			throw new NullPointerException("resultList");
		if (resultList.isEmpty())
			return OptMapResultNode.newBlankMapNode(null);
		if (!resultList.get(0).isUsed())
			return resultList.get(0);
		
		Map<String, Map<Integer, ResultScore>> scoreMap = new LinkedHashMap<>();
		for (OptMapResultNode result : resultList) {
			if (!scoreMap.containsKey(result.mappedRegion.ref))
				scoreMap.put(result.mappedRegion.ref, new LinkedHashMap<Integer, ResultScore>());
			Map<Integer, ResultScore> map = scoreMap.get(result.mappedRegion.ref);
			if (!map.containsKey(result.mappedstrand))
				map.put(result.mappedstrand, new ResultScore());
			ResultScore rs = map.get(result.mappedstrand);
			rs.addAlignment(result);
		}
		List<ResultScore> rsList = new ArrayList<>();
		for (Map<Integer, ResultScore> map : scoreMap.values())
			rsList.addAll(map.values());
		Collections.sort(rsList, Collections.reverseOrder(resultScoreScoreComparator));
		OptMapResultNode representative = rsList.get(0).getRepresentative();
		String ref =representative.mappedRegion.ref;
		int strand = representative.mappedstrand;
		List<OptMapResultNode> finalResultList = new ArrayList<OptMapResultNode>();
		for (OptMapResultNode result : resultList) {
			if (result.mappedRegion.ref.equals(ref) && result.mappedstrand == strand)
				finalResultList.add(result);
		}
		
		assert(finalResultList.size() > 0);
		
		Collections.sort(finalResultList, OptMapResultNode.subfragstartcomparator);
		
		int mappedstrand = 0;
		
		int subfragstart = -1;
		int subfragstop = -1;
		int subrefstart = -1;
		int subrefstop = -1;
		
		int lastsubfragstart = -1;
		int lastsubfragstop = -1;
		int lastsubrefstart = -1;
		int lastsubrefstop = -1;
		Cigar cigar = new Cigar();
		for (OptMapResultNode result : finalResultList) {
			if (mappedstrand == 0)
				mappedstrand = result.mappedstrand;
			
			// Already have results
			if (lastsubfragstop != -1) {
				int insertion;
				if (mappedstrand == 1)
					insertion = result.subfragstart - lastsubfragstop - 2;
				else
					insertion = result.subfragstop - lastsubfragstart - 2;
				int deletion;
				if (mappedstrand == 1)
					deletion = result.subrefstart - lastsubrefstop - 2; 
				else
					deletion = lastsubrefstart - result.subrefstop - 2;
				Cigar tmpCigar = Cigar.newUnmapCigar(insertion, deletion);
				if (mappedstrand == 1)
					cigar.append(tmpCigar);
				else {
					tmpCigar.append(cigar);
					cigar = tmpCigar;
				}
			}
			// Append current result cigar
			if (mappedstrand == 1)
				cigar.append(result.cigar);
			else {
				Cigar tmpCigar = new Cigar(result.cigar);
				tmpCigar.append(cigar);
				cigar = tmpCigar;
			}

			// Save result for next turn to calculate insertion and deletion
			lastsubfragstart = result.subfragstart;
			lastsubfragstop = result.subfragstop;
			lastsubrefstart = result.subrefstart;
			lastsubrefstop = result.subrefstop;
			


			// Update final result subfragstart, subfragstop, subrefstart, subrefstop
			if (mappedstrand == 1) {
				if (subfragstart == -1)
					subfragstart = result.subfragstart;
				subfragstop = result.subfragstop;

				if (subrefstart == -1)
					subrefstart = result.subrefstart;
				subrefstop = result.subrefstop;
			}
			else {
				if (subfragstop == -1)
					subfragstop = result.subfragstop;
				subfragstart = result.subfragstart;

				if (subrefstop == -1)
					subrefstop = result.subrefstop;
				subrefstart = result.subrefstart;
			}
			
			
		}
		
		
		
		OptMapResultNode newResult = new OptMapResultNode(representative.parentFrag, null, mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, 0, representative.confidence);
		newResult.updateMappedRegion(optrefmap.get(representative.mappedRegion.ref));
		newResult.updateScore(optrefmap, match, fpp, fnp);
		return newResult;
	}
	*/
	public static void assignOptions(ExtendOptionParser parser, int parserLevel)
	{
		parser.addHeader("Results Breaker Option", parserLevel);
		parser.accepts("breakermode", "Mode 0: Disable the breaking function; 1: Break the alignment at query/reference segment with size deviating too much, into multiple partial alignments").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		AlignmentOptions.assignScoreOptions(parser);

	}
	
	
	
	
//	public static void main(String[] args) throws IOException
//	{
//		ExtendOptionParser parser = new ExtendOptionParser(ResultsBreaker.class.getSimpleName());
//		ReferenceReader.assignOptions(parser);
//		OptMapDataReader.assignOptions(parser);
//		OptMapResultReader.assignOptions(parser);
//		OptMapResultWriter.assignOptions(parser);
//		ResultsBreaker.assignOptions(parser, 1);
//		// retype
//		parser.addHeader(null, 0);
//		parser.accepts("optmapin").withOptionalArg().ofType(String.class);
//		if (args.length == 0)
//		{
//			parser.printHelpOn(System.out);
//			return;
//		}
//		OptionSet options = parser.parse(args);
//		LinkedHashMap<String, DataNode> optrefmap = ReferenceReader.readAllData(options);
//		OptMapResultReader omrr = new OptMapResultReader(options);
//		OptMapResultWriter omrw = new OptMapResultWriter(options);
//		if (options.has("optmapin"))
//			omrr.importFragInfo(OptMapDataReader.readAllData(options));
//		omrr.importRefInfo(optrefmap);
//		ResultsBreaker resultsBreaker = new ResultsBreaker(optrefmap);
//		resultsBreaker.setParameters(options);
//		
//		OptMapResultNode result;
//		while ((result = omrr.read()) != null)
//			omrw.write(resultsBreaker.breakResult(result));
//		omrr.close();
//		omrw.close();
//	}
}
