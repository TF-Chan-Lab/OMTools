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

import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class AlignmentOptions {
	public static void assignScoreOptions(ExtendOptionParser parser) {
		parser.accepts("match", "Score for matching signal").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.accepts("fpp", "Penalty for extra signal").withRequiredArg().ofType(Integer.class).defaultsTo(2);
		parser.accepts("fnp", "Penalty for missing signal").withRequiredArg().ofType(Integer.class).defaultsTo(2);
	}
	public static void assignErrorToleranceOptions(ExtendOptionParser parser) {
		parser.accepts("meas", "Measurement error").withRequiredArg().ofType(Integer.class).defaultsTo(500);
		parser.accepts("ear", "Error acceptable range (Scaling error tolerance)").withRequiredArg().ofType(Double.class).defaultsTo(0.1);
	}
	public static void assignResolutionOptions(ExtendOptionParser parser) {
		parser.accepts("deg", "Degeneracy of close signals to handle resolution error.").withRequiredArg().ofType(Integer.class).defaultsTo(1500);
	}
}
