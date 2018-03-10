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


package aldenjava.opticalmapping.data.annotation;

import java.io.IOException;
import java.io.InputStream;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class GFFReader extends OMReader<GFFNode> {

	public GFFReader(String filename) throws IOException {
		super(filename);
	}
	public GFFReader(InputStream stream) throws IOException {
		super(stream);
	}

	public GFFReader(OptionSet options) throws IOException {
		this((String) options.valueOf("gffin"));
	}

	@Override
	public GFFNode read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\t");
		GFFNode gtf = new GFFNode(l[0], l[1], l[2], Long.parseLong(l[3]), Long.parseLong(l[4]), l[5].equalsIgnoreCase(".") ? null : Double.parseDouble(l[5]), l[6], l[7].equalsIgnoreCase(".") ? null
				: Integer.parseInt(l[7]), l[8]);
		proceedNextLine();
		return gtf;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("GFF Reader", level);
		parser.accepts("gffin", "Input GFF File").withRequiredArg().ofType(String.class);
	}
}
