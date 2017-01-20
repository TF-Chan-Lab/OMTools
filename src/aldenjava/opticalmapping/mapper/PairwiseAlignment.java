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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.mapper.multithread.MultiThreadMapper;
import aldenjava.opticalmapping.mapper.omblastmapper.OMBlastMapper;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import joptsimple.OptionSet;

public class PairwiseAlignment {

	public static void main(String[] args) throws MapperConstructionException, IOException {
		ExtendOptionParser parser = new ExtendOptionParser(PairwiseAlignment.class.getSimpleName(), "Performs pairwise alignment of data files based on OMBlastMapper. Input multiple data files for pair-wise alignment between each pair of them. ");
		OMBlastMapper.assignOptions(parser, 1);
		MultiThreadMapper.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser, 1);
		
		parser.addHeader("Pairwise alignment options", 1);
		parser.accepts("output", "output prefix").withRequiredArg().ofType(String.class).required();
		parser.accepts("rerun", "Rerun even if the result file exists").withOptionalArg().ofType(Boolean.class).defaultsTo(false);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		
		OptionSet options = parser.parse(args);
		List<String> optmapins = (List<String>) options.valuesOf("optmapin");
		String outputDir = (String) options.valueOf("output");
		boolean rerun = (boolean) options.valueOf("rerun");
		
		LinkedHashMap<String, LinkedHashMap<String, DataNode>> dataMaps = new LinkedHashMap<>();
		for (String optmapin : optmapins) {
			try {
				String basename = FilenameUtils.getBaseName(optmapin);
				LinkedHashMap<String, DataNode> dataMap = OptMapDataReader.readAllData(optmapin);
				dataMaps.put(basename, dataMap);
			} catch (IOException e) {
				System.err.println("IOException occurs on the file: " + optmapin);
			}
		}
		
		for (String ref : dataMaps.keySet()) { 
			LinkedHashMap<String, DataNode> optrefmap = dataMaps.get(ref);
			VerbosePrinter.println("Reference: " + ref);
			MultiThreadMapper mapper = new MultiThreadMapper(OMBlastMapper.class, optrefmap);
			mapper.setParameters(options);
			for (String data : dataMaps.keySet()) {
				VerbosePrinter.println("\tData: " + data);
				String resultFile = outputDir + data + "_blast_" + ref + ".oma";
				if (rerun || !Files.exists(Paths.get(resultFile))) {
					try (OptMapResultWriter omrw = new OptMapResultWriter(resultFile)) {
						LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = (mapper.mapAll(dataMaps.get(data)));
						for (String key : resultlistmap.keySet()) {
							List<OptMapResultNode> resultlist = resultlistmap.get(key);
							if (resultlist == null || resultlist.isEmpty())
								omrw.write(OptMapResultNode.newBlankMapNode(dataMaps.get(data).get(key)));
							else
								omrw.writeAll(resultlist);
						}
							
					}
					catch (IOException e) {
						System.err.println("IOException occurs on writing result file: " + resultFile);
					}
				}
			}
			mapper.close();
		}
		
		VerbosePrinter.println("Program ends.");
	}
}
