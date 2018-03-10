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


package aldenjava.opticalmapping.mapper.omhamapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.mapper.Mapper;
import aldenjava.opticalmapping.mapper.MapperConstructionException;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class OMHAMapper extends Mapper {

	private OMHACore omhacore = null;

	public OMHAMapper(LinkedHashMap<String, DataNode> optrefmap) {
		super(optrefmap);
	}

	public OMHAMapper(LinkedHashMap<String, DataNode> optrefmap, OMHACore omhacore) {
		this(optrefmap);
		this.omhacore = omhacore;
	}

	@Override
	public void setParameters(OptionSet options) throws IOException {
		super.setParameters(options);
		this.setParameters((boolean) options.valueOf("local"), (int) options.valueOf("localstart"), (int) options.valueOf("falselimit"), (int) options.valueOf("scorefilter"),
				(int) options.valueOf("meas"), (int) options.valueOf("deg"), (double) options.valueOf("ear"), (int) options.valueOf("match"), (int) options.valueOf("fpp"),
				(int) options.valueOf("fnp"));
	}

	public void setParameters(boolean allowLocalAlignment, int localstart, int falselimit, int scorefilter, int measure, int degeneracy, double ear, int matchscore, int falseppenalty,
			int falsenpenalty) {
		if (omhacore != null)
			throw new IllegalStateException("Parameters are already initialized.");
		omhacore = new OMHACore(optrefmap);
		omhacore.setParameters(allowLocalAlignment, localstart, falselimit, scorefilter, measure, degeneracy, ear, matchscore, falseppenalty, falsenpenalty);
	}

	@Override
	public List<OptMapResultNode> getResult(DataNode data, List<GenomicPosNode> regionList) {
		if (omhacore == null)
			throw new IllegalStateException("Parameters are not initialized for OMHA Mapper.");
		if (data == null)
			throw new NullPointerException();
		return omhacore.getResult(data);
	}

	@Override
	public Mapper copy() {
		Mapper mapper = new OMHAMapper(optrefmap, omhacore.copy());
		super.setCopyMapperParameters(mapper);
		return mapper;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {

		Mapper.assignOptions(parser, level);
		parser.addHeader("OMHAMapper Options", level);
		parser.accepts("local", "Enable local alignment").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		parser.accepts("localstart", "Local start pos for alignment, 0: starts at every signal (exhausive), x: starts at first x signals, -x: starts without last x signals.")
				.withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("scorefilter", "Primary score filter during alginment").withRequiredArg().ofType(Integer.class).defaultsTo(30);

		AlignmentOptions.assignResolutionOptions(parser);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		AlignmentOptions.assignScoreOptions(parser);
		

		parser.accepts("falselimit", "Max consecutive false signals").withRequiredArg().ofType(Integer.class).defaultsTo(5);

	}

	public static void main(String[] args) throws MapperConstructionException, IOException {
		Mapper.standardMapperProcedure(args, OMHAMapper.class);
	}

}
