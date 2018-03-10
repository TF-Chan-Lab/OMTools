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


package aldenjava.opticalmapping.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

/**
 * Merges alignment results from different alignment methods
 * @author Alden
 *
 */
public class ResultMerger {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(ResultMerger.class.getSimpleName(), "Merges alignment results from different alignment methods");
		OptMapResultReader.assignOptions(parser, 1);
		parser.addHeader("Result Merger Options", 1);
		parser.accepts("resultkey", "Keys (names) to represent the result files").withRequiredArg().ofType(String.class).required();
		parser.accepts("gapallowed", "Gaps allowed between results").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("analyzeall", "Analyze only if the query is present in all results").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		parser.accepts("prefix", "Output file prefix").withRequiredArg().ofType(String.class).required();
		parser.accepts("outtype", "Output file type").withRequiredArg().ofType(String.class).defaultsTo(".omd");
		
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		int gapAllowed = (int) options.valueOf("gapallowed");
		boolean analyzeAll = (boolean) options.valueOf("analyzeall");
		List<String> resultFileList = (List<String>) options.valuesOf("optresin");
		List<String> keyList = new ArrayList<>();
		for (int i = 0; i < resultFileList.size(); i++)
			keyList.add(Integer.toString(i + 1));
		if (options.has("resultkey")) {
			List<String> list = (List<String>) options.valuesOf("resultkey");
			if (list.size() == resultFileList.size())
				keyList = list;
			else
				System.err.println("File keys do not match number of result files!");
		}
		
		String prefix = (String) options.valueOf("prefix");
		String outtype = (String) options.valueOf("outtype");
		
		
		LinkedHashMap<String, List<List<OptMapResultNode>>> resultListMap = new LinkedHashMap<>();
		for (int index = 0; index < resultFileList.size(); index++) {
			String resultFile = resultFileList.get(index);
			OptMapResultReader omrr = new OptMapResultReader(resultFile);
			List<OptMapResultNode> resultList;
			while ((resultList = omrr.readNextList()) != null) {
				String id = resultList.get(0).parentFrag.name;
				List<List<OptMapResultNode>> eList = resultListMap.get(id);
				if (eList == null) {
					eList = new ArrayList<>();
					for (int i = 0; i < resultFileList.size(); i++)
						eList.add(null);
					resultListMap.put(id, eList);
				}
				eList.set(index, resultList);
			}
			omrr.close();
		}
		
		// Statistics
//		int uniqueNum = 0;
//		int mergedNum = 0;
//		int conflictNum = 0;
//		List<String> conflictID = new ArrayList<String>();
		
		LinkedHashMap<String, List<OptMapResultNode>> mergedResultMap = new LinkedHashMap<>();
		List<LinkedHashMap<String, List<OptMapResultNode>>> uniqueResultMap = new ArrayList<>();
		List<LinkedHashMap<String, List<OptMapResultNode>>> conflictResultMap = new ArrayList<>();
		
		for (int i = 0; i < resultListMap.size(); i++) {
			uniqueResultMap.add(new LinkedHashMap<String, List<OptMapResultNode>>());
			conflictResultMap.add(new LinkedHashMap<String, List<OptMapResultNode>>());
		
		}
		OUTLOOP:
		for (String id : resultListMap.keySet()) {
			try {
				// when analyzing all, only if all result file contains certain alignment to proceed.
				if (analyzeAll)
					for (int resultFileID = 0; resultFileID < resultFileList.size(); resultFileID++)
						if (resultListMap.get(id).get(resultFileID) == null)
							continue OUTLOOP;
				List<OptMapResultNode> finalList = null;
				boolean noConflict = true;
				int uniqueID = -1;
				for (int resultFileID = 0; resultFileID < resultFileList.size(); resultFileID++) {
					List<OptMapResultNode> resultList = resultListMap.get(id).get(resultFileID);
					if (resultList == null || !resultList.get(0).isUsed()) 
						continue;
					if (finalList == null) {
						finalList = resultList;
						uniqueID = resultFileID;
					}
					else {
						uniqueID = -1;
						boolean similar = false;
						outerloop:
						for (OptMapResultNode r1 : finalList)
							for (OptMapResultNode r2 : resultList)
								if (r1.isSimilar(r2, gapAllowed)) {
									similar = true;
									break outerloop;
								}
						if (!similar) {
	//						conflictID.add(id);
							noConflict = false;
							break;
						}
					}
				}	
				
				if (finalList == null) continue;
				
				if (noConflict) {				
					if (uniqueID == -1) 
						mergedResultMap.put(id, finalList);
					else
						uniqueResultMap.get(uniqueID).put(id, finalList);
				}
				else {
					for (int resultFileID = 0; resultFileID < resultFileList.size(); resultFileID++) {
						List<OptMapResultNode> resultList = resultListMap.get(id).get(resultFileID);
						if (resultList != null)
							conflictResultMap.get(resultFileID).put(id, resultList);
					}
				}
			}
			catch (RuntimeException e) {
				System.err.println("Results of molecule " + id + " contain problems and are discarded");
				System.err.println(keyList.get(0) + "\t" + keyList.get(1));
				e.printStackTrace();
			}
		}
		
		
		{
			String mergedFile = prefix + "merged" + outtype;
			OptMapResultWriter omrw = new OptMapResultWriter(mergedFile);
			for (List<OptMapResultNode> resultList : mergedResultMap.values())
				omrw.write(resultList);
			omrw.close();
		}
		{
			for (int i = 0; i < keyList.size(); i++) {
				String conflictFile = prefix + keyList.get(i) + "_conflict" + outtype;
				OptMapResultWriter omrw = new OptMapResultWriter(conflictFile);
				for (List<OptMapResultNode> resultList : conflictResultMap.get(i).values())
					omrw.write(resultList);
				omrw.close();
			}
		}
		{
			for (int i = 0; i < keyList.size(); i++) {
				String uniqueFile = prefix + keyList.get(i) + "_unique" + outtype;
				OptMapResultWriter omrw = new OptMapResultWriter(uniqueFile);
				for (List<OptMapResultNode> resultList : uniqueResultMap.get(i).values())
					omrw.write(resultList);
				omrw.close();
			}
		}
		
//		System.out.println("Total results: " + (uniqueNum + mergedNum + conflictNum));
//		System.out.println("Total unique results: " + (uniqueNum));
//		System.out.println("Total merged results: " + (mergedNum));
//		System.out.println("Total conflict (and discarded) results: " + (conflictNum));
		
	}
}

