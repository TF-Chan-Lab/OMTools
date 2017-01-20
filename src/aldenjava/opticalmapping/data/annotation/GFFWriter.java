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

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class GFFWriter extends OMWriter<GFFNode> {

//	BufferedWriter bw;
	public GFFWriter(String filename) throws IOException
	{
		super(filename);
//		bw = new BufferedWriter(new FileWriter(filename));
//		initializeHeader();
	}
	public GFFWriter(OptionSet options) throws IOException
	{
		this((String) options.valueOf("gffout"));
	}
	@Override
	public void initializeHeader()
	{
		// NO header temporarily
	}
	public void write(GFFNode gff) throws IOException
	{
		bw.write(gff.seqname);
		bw.write("\t");
		bw.write(gff.source);
		bw.write("\t");
		bw.write(gff.feature);
		bw.write("\t");
		bw.write(Long.toString(gff.start));
		bw.write("\t");
		bw.write(Long.toString(gff.end));
		bw.write("\t");
		if (gff.score != null)
			bw.write(Double.toString(gff.score));
		else
			bw.write(".");
		bw.write("\t");
		bw.write(gff.strand);
		bw.write("\t");
		if (gff.score != null)
			bw.write(Integer.toString(gff.frame));
		else
			bw.write(".");
		bw.write("\t");
		for (String key : gff.attribute.keySet())
		{
			bw.write(key);
			bw.write("=");
			bw.write(gff.attribute.get(key));
			bw.write(";");
		}
		bw.write("\n");
	}
			
	public static void assignOptions(ExtendOptionParser parser, int level)
	{
		parser.addHeader("GFF Writer", level);
		parser.accepts("gffout", "GFF file output").withRequiredArg().ofType(String.class);
	}
}
