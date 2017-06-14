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


package aldenjava.opticalmapping;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import aldenjava.opticalmapping.application.svdetection.SVDetection;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.data.data.OptMapDataGenerator;
import aldenjava.opticalmapping.data.data.RandomReferenceGenerator;
import aldenjava.opticalmapping.mapper.MapperConstructionException;
import aldenjava.opticalmapping.mapper.PairwiseAlignment;
import aldenjava.opticalmapping.mapper.omblastmapper.OMBlastMapper;
import aldenjava.opticalmapping.mapper.omfmmapper.OMFMMapper;
import aldenjava.opticalmapping.mapper.omhamapper.OMHAMapper;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.statistics.DataStatistics;
import aldenjava.opticalmapping.statistics.ResultStatistics;
import aldenjava.opticalmapping.tools.DataTools;
import aldenjava.opticalmapping.tools.DuplicatedMoleculesDetection;
import aldenjava.opticalmapping.tools.DuplicatedMoleculesRemover;
import aldenjava.opticalmapping.tools.FastaToOM;
import aldenjava.opticalmapping.tools.ResultMerger;
import aldenjava.opticalmapping.tools.ResultTools;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.script.PrecisionRecallGraphDataGenerator;

/**
 * This class is the user interface of all different tools.
 * @author Alden Leung
 * @version %I%, %G%
 */
public class OMTools {

	public final static String version = "OMTools Version 1.3a";
	public final static String author = "Alden Leung";
	
	private static void displayOptions() throws IOException, MapperConstructionException {		
		String[] arg = new String[]{};
		System.out.println("\\part{Mapper}");
		OMBlastMapper.main(arg);
		OMHAMapper.main(arg);
		OMFMMapper.main(arg);
		PairwiseAlignment.main(arg);
		System.out.println("\\part{Simulation}");
		OptMapDataGenerator.main(arg);
		RandomReferenceGenerator.main(arg);
		System.out.println("\\part{SV Detection}");
		SVDetection.main(arg);
		System.out.println("\\part{Fasta Tools}");
		FastaToOM.main(arg);
		System.out.println("\\part{Data Tools}");
		DataTools.main(arg);
		DataStatistics.main(arg);
		DuplicatedMoleculesDetection.main(arg);
		DuplicatedMoleculesRemover.main(arg);
		System.out.println("\\part{Alignment Tools}");
		ResultTools.main(arg);
		ResultMerger.main(arg);
		ResultStatistics.main(arg);
		PrecisionRecallGraphDataGenerator.main(arg);
		System.out.println("\\part{Visualization}");
		OMView.main(new String[] {"--help"});
		System.out.println("\\input{omview}");
		System.exit(0);
	}
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, IOException, MapperConstructionException
	{
//		displayOptions();
		OMWriter.setCommands(OMTools.version + "\n#" + StringUtils.join(args, " "));
		if (args.length == 0) {
			System.out.println(OMTools.version);
			System.out.println();
			System.out.println("Please run as java -jar OMTools.jar <command> [options]");
			System.out.println();
			System.out.println("Command List");
			System.out.println("========================================");
			System.out.println("================ Mapper ================");
			System.out.println("OMBlastMapper");
			System.out.println("OMHAMapper");
			System.out.println("OMFMMapper");
			System.out.println("PairwiseAlignment");
			System.out.println("============== Simulation ==============");
			System.out.println("OptMapDataGenerator");
			System.out.println("RandomReferenceGenerator");
			System.out.println("============== SV Detection ==============");
			System.out.println("SVDetection");
			System.out.println("============= Fasta Tools ==============");
			System.out.println("FastaToOM");
			System.out.println("============== Data Tools ===============");
			System.out.println("DataTools");
			System.out.println("DataStatistics");
			System.out.println("DuplicatedMoleculesDetection");
			System.out.println("DuplicatedMoleculesRemover");
			System.out.println("============= Alignment Tools ==============");
			System.out.println("ResultTools");
			System.out.println("ResultMerger");
			System.out.println("ResultStatistics");
			System.out.println("PrecisionRecallGraphDataGenerator");
			System.out.println("============= Multiple Alignment ==============");
			System.out.println("MultipleAlignment");
			System.out.println("BlockConnectionGraphGeneration");
			System.out.println("=============== Visualization ================");
			System.out.println("OMView");
		}
		else {
			String option;
			int pos = 0;
			while ((option = args[pos]).startsWith("-")) {
				switch (option) {
					case "-verbose": 
						VerbosePrinter.verbose = true;
						break;
					case "-verberr":
						VerbosePrinter.verbose = true;
						VerbosePrinter.stream = System.err;
						break;
					case "-disableheader":
						OMWriter.setHeader(false);
						break;
					case "-version":
						System.out.println(OMTools.version);
						return;
					default:
						System.err.println("Option " + option + " is not found.");
						return;
				}
				pos++;
			}
			String programname = args[pos++];
			String[] arg = Arrays.copyOfRange(args, pos, args.length);
			switch (programname.toLowerCase()) {
				// Mapper
				case "omblastmapper":
					OMBlastMapper.main(arg);
					break;
				case "omhamapper":
					OMHAMapper.main(arg);
					break;
				case "omfmmapper":
					OMFMMapper.main(arg);
					break;
				case "pairwisealignment":
					PairwiseAlignment.main(arg);
					break;
				// SVDetection
				case "svdetection":
					SVDetection.main(arg);
					break;
				// Visualizer
				case "omview":
					OMView.main(arg);
					break;
					
				// Simulation
				case "optmapdatagenerator":
					OptMapDataGenerator.main(arg);
					break;
				case "randomreferencegenerator":
					RandomReferenceGenerator.main(arg);
					break;
				// Fasta
				case "fastatoom":
					FastaToOM.main(arg);
					break;
				// Data
				case "datatools":
					DataTools.main(arg);
					break;
				case "datastatistics":
					DataStatistics.main(arg);
					break;
				case "duplicatedmoleculesdetection":
					DuplicatedMoleculesDetection.main(arg);
					break;
				case "duplicatedmoleculesremover":
					DuplicatedMoleculesRemover.main(arg);
					break;
				// Result
				case "resulttools":
					ResultTools.main(arg);
					break;
				case "resultmerger":
					ResultMerger.main(arg);
					break;
				case "resultstatistics":
					ResultStatistics.main(arg);
					break;
				case "precisionrecallgraphdatagenerator":	
					PrecisionRecallGraphDataGenerator.main(arg);
					break;
				default:
					System.err.println("No such program found: \"" + programname + "\".");
			}
			
			
		}
	}
}
