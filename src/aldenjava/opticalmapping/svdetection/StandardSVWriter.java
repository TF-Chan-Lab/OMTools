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


package aldenjava.opticalmapping.svdetection;

import java.io.IOException;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class StandardSVWriter extends OMWriter<StandardSVNode> {

/*	GenomicPosNode region;
	String type;
	int variant_id;
	List<String> svDetectionMethod;
	String sample_id;
	LinkedHashMap<String, Object> SV_attribute;
	String zygosity;
	String origin;
	double score;
	String notes;
	String link_exp_result;
*/
	
//	private String filename;
//	private BufferedWriter bw;
	public StandardSVWriter(String filename) throws IOException
	{
//		this.filename = filename;
		super(filename);
//		this.bw = new BufferedWriter(new FileWriter(filename));
//		this.initializeHeader();
	}
	
	public StandardSVWriter(OptionSet options) throws IOException {
		this((String) options.valueOf("svout"));
	}

	@Override
	public void initializeHeader() throws IOException	
	{
		bw.write("#Standard SV format V4\n");
		bw.write("#chr\tstart\tstop\tvariant_call_type\tvariant_call_id\tSV_detection_method\tsample_id\tSV_attributes\tzygosity\torigin\tscore\tFreeNotes\tLink.to.expt.result\n");
	}
	@Override
	public void write(StandardSVNode sv) throws IOException
	{
		String detection_method = "";
		for (String s : sv.svDetectionMethod)
		{
			if (!detection_method.isEmpty())
				detection_method += ",";
			detection_method += s;
		}
		
		
		String attribute = "";
		
		for (String key : sv.sv_attribute.keySet())
		{
			if (!attribute.isEmpty())
				attribute += ";";
			attribute += key + "=" + sv.sv_attribute.get(key).toString();
		}
		bw.write(String.format("%s\t%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.4f\t%s\t%s\n",
				sv.region.ref, sv.region.start, sv.region.stop,
				sv.type, sv.variant_id, detection_method, sv.sample_id,
				attribute, sv.zygosity, sv.origin, sv.score, sv.notes, sv.link_exp_result
				));
				
	}
	public static void writeAll(OptionSet options, List<StandardSVNode> svList) throws IOException {
		writeAll((String) options.valueOf("svout"), svList);
	}

	public static void writeAll(String filename, List<StandardSVNode> svList) throws IOException {
		StandardSVWriter svw = new StandardSVWriter(filename);
		svw.writeAll(svList);
		svw.close();
	}

	public static void assignOptions(ExtendOptionParser parser, int level) 
	{
		parser.addHeader("Standard SV Writer Options", level);
		parser.accepts("svout", "Output SV file").withRequiredArg().ofType(String.class);		
	}
}
