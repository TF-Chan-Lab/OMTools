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


package aldenjava.opticalmapping.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.application.svdetection.StandardSVNode;
import aldenjava.opticalmapping.application.svdetection.StandardSVReader;
import aldenjava.opticalmapping.application.svdetection.StandardSVWriter;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class LiftOver {

	List<LinkedHashMap<String, List<LiftOverNode>>> fixedliftoverlistmaplist = new ArrayList<LinkedHashMap<String, List<LiftOverNode>>>();
	public LiftOver()
	{
	}
	public LiftOver(OptionSet options) throws IOException 
	{
		if (options.has("liftoverin"))
			for (String liftoverfile : (List<String>) options.valuesOf("liftoverin"))
				addLiftOverList(readLiftOverList(liftoverfile));
	}
	public void addLiftOverList(LinkedHashMap<String, List<LiftOverNode>> liftoverlistmap)
	{
		LinkedHashMap<String, List<LiftOverNode>> fixedliftoverlistmap = new LinkedHashMap<String, List<LiftOverNode>>();
		for (String key : liftoverlistmap.keySet())
		{
			
			List<LiftOverNode> liftoverlist = liftoverlistmap.get(key);
			List<LiftOverNode> fixedliftoverlist = new ArrayList<LiftOverNode>();
			Collections.sort(liftoverlist);
			long previousSizeChange = 0;
			long totalSizeChange = 0;
			for (LiftOverNode liftover : liftoverlist)
			{
				totalSizeChange += liftover.sizeChange;
				fixedliftoverlist.add(new LiftOverNode(liftover.name, liftover.pos + previousSizeChange, totalSizeChange));
				previousSizeChange = totalSizeChange;
			}
			fixedliftoverlistmap.put(key, fixedliftoverlist);
		}
		this.fixedliftoverlistmaplist.add(fixedliftoverlistmap);
	}
	public LinkedHashMap<String, List<LiftOverNode>> readLiftOverList(String liftoverfile) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(liftoverfile));
		String s;
		LinkedHashMap<String, List<LiftOverNode>> liftoverlistmap = new LinkedHashMap<String, List<LiftOverNode>>(); 
				
		while ((s = br.readLine()) != null)
			if (!s.startsWith("#") && !s.isEmpty())
			{
				String[] l = s.split("\t");
				List<LiftOverNode> liftoverlist = liftoverlistmap.get(l[0]);
				if (liftoverlist == null)
				{
					liftoverlist = new ArrayList<LiftOverNode>();
					liftoverlistmap.put(l[0], liftoverlist);
				}
				liftoverlist.add(new LiftOverNode(l[0], Long.parseLong(l[1]), Long.parseLong(l[2])));
			}
			
		br.close();
		return liftoverlistmap;
	}
	
	
	
	public GenomicPosNode getLiftRegion(GenomicPosNode region)
	{
		if (fixedliftoverlistmaplist.isEmpty())
			return new GenomicPosNode(region);
		for (LinkedHashMap<String, List<LiftOverNode>> fixedliftoverlistmap : fixedliftoverlistmaplist) 
		{
			List<LiftOverNode> fixedliftoverlist = fixedliftoverlistmap.get(region.ref);
			if (fixedliftoverlist == null)
				continue;

			// sequential search...
			int index = 0;
			long newstart;
			long newstop;
			while (index < fixedliftoverlist.size() && fixedliftoverlist.get(index).pos <= region.start)
				index++;
			index--;
			if (index < 0)
				newstart = region.start;
			else
				newstart = region.start - fixedliftoverlist.get(index).sizeChange;
			
			index = 0;
			while (index < fixedliftoverlist.size() && fixedliftoverlist.get(index).pos <= region.stop)
				index++;
			index--;
			if (index < 0)
				newstop = region.stop;
			else
				newstop = region.stop - fixedliftoverlist.get(index).sizeChange;
			region = new GenomicPosNode(region.ref, newstart, newstop);
		}
		return region;
	}
	public void lift(OptMapResultNode result)
	{
		if (fixedliftoverlistmaplist.isEmpty())
			return;
		result.mappedRegion = getLiftRegion(result.mappedRegion);
	}
	public void lift(StandardSVNode sv)
	{
		if (fixedliftoverlistmaplist.isEmpty())
			return;
		sv.region = getLiftRegion(sv.region);
	}
	public static void assignOptions(ExtendOptionParser parser, int parserLevel)
	{
		parser.addHeader("Lift Over Options", parserLevel);
		parser.accepts("liftoverin", "Input liftOver file, Format: chromosome\\t coordinate\\t size\\n").withRequiredArg().ofType(String.class);
	}
}

class LiftOverNode implements Comparable<LiftOverNode>
{
	final String name;
	final long pos;
	final long sizeChange;
	public LiftOverNode(String name, long pos, long sizeChange) {
		this.name = name;
		this.pos = pos;
		this.sizeChange = sizeChange;
	}
	@Override
	public int compareTo(LiftOverNode n) {
		return Long.compare(this.pos, n.pos);
	}
}