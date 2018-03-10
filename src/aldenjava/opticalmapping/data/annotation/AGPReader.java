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

import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;
/**
 * The AGP reader that reads AGP files
 * @author Alden
 *
 */
public class AGPReader extends OMReader<AGPNode> {

	public AGPReader(String filename) throws IOException {
		super(filename);
	}
	public AGPReader(InputStream stream) throws IOException {
		super(stream);
	}
	public AGPReader(OptionSet options) throws IOException {
		this((String) options.valueOf("agpin"));
	}

	
	@Override
	public AGPNode read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\\s+");
		proceedNextLine();

		String obj_name = l[0];
		long object_beg = Long.parseLong(l[1]);
		long obj_end = Long.parseLong(l[2]);
		int part_number = Integer.parseInt(l[3]);
		char component_type = l[4].charAt(0);
		if (component_type == 'N' || component_type == 'U') {
			Long gap_length = Long.parseLong(l[5]);
			String gap_type = l[6];
			String linkage = l[7];
			String linkage_evidence = l[8]; // All support are stored in one string
			return new AGPNode(obj_name, object_beg, obj_end, part_number, component_type, gap_length, gap_type, linkage, linkage_evidence);
		}
		else {
			String component_id = l[5];
			Long component_beg = Long.parseLong(l[6]);
			Long component_end = Long.parseLong(l[7]);
			Integer orientation = l[8].equals("-")?-1:1; // By default, ?, 0, na are treated as positive
			return new AGPNode(obj_name, object_beg, obj_end, part_number, component_type, component_id, component_beg, component_end, orientation);
		}
	}
	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("AGP Reader", level);
		parser.accepts("agpin", "Input AGP File").withRequiredArg().ofType(String.class);
	}

}
