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


package aldenjava.opticalmapping.data.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Scanner;

import joptsimple.OptionSet;

import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class GVFReader extends OMReader<GVFNode> {
	public GVFReader(String filename) throws IOException {
		super(filename);
	}

	public GVFReader(InputStream stream) throws IOException {
		super(stream);
	}

	public GVFReader(OptionSet options) throws IOException {
		this((String) options.valueOf("gvfin"));
	}

	@Override
	public GVFNode read() throws IOException {
		if (nextline == null)
			return null;
		Scanner scanner = new Scanner(nextline);
		scanner.useDelimiter("\t");
		String seqid = scanner.next();
		String source = scanner.next();
		String type = scanner.next();
		Long start;
		if (scanner.hasNextLong())
			start = scanner.nextLong();
		else {
			start = null;
			scanner.next();
		}
		Long end;
		if (scanner.hasNextLong())
			end = scanner.nextLong();
		else {
			end = null;
			scanner.next();
		}
		Double score;
		if (scanner.hasNextDouble())
			score = scanner.nextDouble();
		else {
			score = null;
			scanner.next();
		}
		String strandInString = scanner.next();
		char strand = strandInString.charAt(0); // == '+' ? 1 : strandInString.charAt(0) == '-' ? -1 : 0;
		String phase = scanner.next();
		String s_attributes = scanner.next();
		String[] s_attribute = s_attributes.split(";");
		LinkedHashMap<String, String> attribute = new LinkedHashMap<String, String>();
		for (String attr : s_attribute) {
			String[] attrsplit = attr.split("=");
			attribute.put(attrsplit[0], attrsplit[1]);
		}
		scanner.close();
		proceedNextLine();
		return new GVFNode(seqid, source, type, start, end, score, strand, phase, attribute);
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("GVF Reader options", level);
		parser.accepts("gvfin", "GVF file input").withRequiredArg().ofType(String.class);
	}
}
