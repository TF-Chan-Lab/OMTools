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


package aldenjava.opticalmapping.data.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.file.ListExtractor;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class MultipleReferenceReader {
	private List<String> filelist;
	private List<Double> ratiolist;
	private String filename = "";
	private int format;

	public MultipleReferenceReader(OptionSet options) throws IOException {
		if (options.has("refmaplistin")) {
			this.set((String) options.valueOf("refmaplistin"), (int) options.valueOf("refmapinformat"));
		} else if (options.has("refmapin")) {
			
			this.filename = (String) options.valueOf("refmapin");
			System.out.println(filename);
			this.format = (int) options.valueOf("refmapinformat");
		}
		else
			throw new RuntimeException("Either refmapin or refmaplistin option is required.");
	}

	public MultipleReferenceReader(String filename, int format) throws IOException {
		this.set(filename, format);
	}

	private void set(String filename, int format) throws IOException {
		List<String> stringlist = ListExtractor.extractList(filename);
		if (!stringlist.isEmpty()) {
			if (stringlist.get(0).trim().split("\t").length > 1) {
				filelist = new ArrayList<String>();
				ratiolist = new ArrayList<Double>();
				for (String s : stringlist) {
					String[] sarray = s.trim().split("\t");
					filelist.add(sarray[0]);
					ratiolist.add(Double.parseDouble(sarray[1]));
				}
			} else
				filelist = stringlist;
		} else
			filelist = stringlist;
		this.format = format;

	}

	public MultipleReferenceReader(List<String> filelist, List<Double> ratiolist, int format) {
		this.filelist = filelist;
		this.ratiolist = ratiolist;
		this.format = format;
	}

	public LinkedHashMap<String, ReferenceClusterNode> readAllData() throws IOException {
		LinkedHashMap<String, ReferenceClusterNode> optclusmap = new LinkedHashMap<String, ReferenceClusterNode>();
		if (this.filename.isEmpty()) {
			for (int i = 0; i < filelist.size(); i++) {
				String filename = filelist.get(i);
				double ratio;
				if (ratiolist != null)
					ratio = ratiolist.get(i);
				else
					ratio = 1 / (double) filelist.size();

				ReferenceReader refreader = new ReferenceReader(filename, format);
				LinkedHashMap<String, DataNode> optrefmap = refreader.readAllData();
				refreader.close();
				optclusmap.put(filename, new ReferenceClusterNode(filename, optrefmap, ratio));
			}
		} else {
			optclusmap.put(filename, new ReferenceClusterNode(filename, ReferenceReader.readAllData(this.filename, format), 1));
		}
		return optclusmap;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		ReferenceReader.assignOptions(parser);
		parser.addHeader("Multiple Reference Reader Options", level);
		parser.accepts("refmaplistin", "Input reference map file list with ratio").withRequiredArg().ofType(String.class);
		parser.accepts("refmapinformat", DataFormat.getFormatHelp()).withOptionalArg().ofType(Integer.class).defaultsTo(-1);
		parser.addHeader(null, 0);
		parser.accepts("refmapin", "Input reference map file").withRequiredArg().ofType(String.class);
	}
}
