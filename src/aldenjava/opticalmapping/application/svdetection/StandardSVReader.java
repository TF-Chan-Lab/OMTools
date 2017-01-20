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


package aldenjava.opticalmapping.application.svdetection;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.data.mappingresult.ResultFormat;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class StandardSVReader extends OMReader<StandardSVNode> {

	
//	private BufferedReader br;
//	private String nextline = "";
//	public StandardSVReader(String filename) throws IOException
//	{
//		br = new BufferedReader(new FileReader(filename));
//		commentReader();
//	}
	public StandardSVReader(String filename) throws IOException
	{
		super(filename);
	}
	public StandardSVReader(InputStream stream) throws IOException
	{
		super(stream);
	}
	public StandardSVReader(OptionSet options) throws IOException
	{
		super((String) options.valueOf("svin"));
	}
	
	public StandardSVNode read() throws IOException
	{
		if (nextline == null)
			return null;
		String s = nextline;
		String[] l = s.split("\t", -1);
		int pos = 0;
		GenomicPosNode region = new GenomicPosNode(l[pos], Long.parseLong(l[pos + 1]), Long.parseLong(l[pos + 2]));
		pos += 3;
		String type = l[pos++];
		String variant_id = l[pos++];
		String svDetectionMethodString = l[pos++];
		List<String> svDetectionMethod = new ArrayList<String>();
		if (!svDetectionMethodString.isEmpty())
			svDetectionMethod = Arrays.asList(svDetectionMethodString.split(","));
		String sample_id = l[pos++];
		String sv_attribute_string = l[pos++];
		LinkedHashMap<String, Object> sv_attribute = new LinkedHashMap<String, Object>();
		if (!sv_attribute_string.isEmpty())
		{
			String[] sv_attribute_array = sv_attribute_string.split(";");
			for (String sv_att : sv_attribute_array)
			{
				String[] tmp = sv_att.split("=");
				String key = tmp[0];
				String value = tmp[1];
				sv_attribute.put(key, value);
			}
		}
		String zygosity = l[pos++];
		String origin = l[pos++];
		double score = Double.parseDouble(l[pos++]);
		String notes = l[pos++];
		String link_exp_result = l[pos++];
		proceedNextLine();
		return new StandardSVNode(region, type, variant_id, svDetectionMethod, sample_id, sv_attribute, zygosity, origin, score, notes, link_exp_result);
	}
//	public List<StandardSVNode> readAll() throws IOException
//	{
//		List<StandardSVNode> svList = new ArrayList<StandardSVNode>();
//		while (this.nextline != null)
//			svList.add(read());
//		return svList;
//	}

	public static List<StandardSVNode> readAll(OptionSet options) throws IOException
	{
		return readAll((String) options.valueOf("svin"));
	}
	public static List<StandardSVNode> readAll(String filename) throws IOException
	{
		StandardSVReader ssvr = new StandardSVReader(filename);
		List<StandardSVNode> svList = ssvr.readAll();
		ssvr.close();
		return svList;
	}
	
	
	public static void assignOptions(ExtendOptionParser parser)
	{
		parser.addHeader("Standard SV Reader Options", 1);
		parser.accepts("svin", "Input SV file").withRequiredArg().ofType(String.class);
	}


}
