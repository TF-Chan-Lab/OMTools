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


package aldenjava.sequence.fasta;

import java.io.IOException;
import java.util.LinkedHashMap;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class StreamFastaWriter extends OMWriter<StreamFastaNode>{
//	private BufferedWriter bw;

	public StreamFastaWriter(OptionSet options) throws IOException 
	{
		this((String) options.valueOf("fastaout"));
	}

	public StreamFastaWriter(String filename) throws IOException
	{
		super(filename);
//		bw = new BufferedWriter(new FileWriter(filename));
//		initializeHeader();
	}
	
//	public void initializeHeader() throws IOException
//	{
		// Nothing to initialize
//	}
	public void write(StreamFastaNode sfn) throws IOException
	{
		bw.write(">");
		bw.write(sfn.name);
		bw.write("\n");
		bw.write(sfn.seq);
		bw.write("\n");
	}

	public static void writeAllData(String filename, LinkedHashMap<String, StreamFastaNode> fastaInfo) throws IOException
	{
		StreamFastaWriter sfw = new StreamFastaWriter(filename);
		for (StreamFastaNode sfn : fastaInfo.values())
			sfw.write(sfn);
		sfw.close();
	}
	public static void writeAllData(OptionSet options, LinkedHashMap<String, StreamFastaNode> fastaInfo) throws IOException
	{
		writeAllData((String) options.valueOf("fastaout"), fastaInfo);
	}

	public static void assignOptions(ExtendOptionParser parser)
	{
		parser.addHeader("Stream Fasta Writer Options", 1);
		parser.accepts("fastaout", "fasta output file").withRequiredArg().ofType(String.class);		
	}

}

