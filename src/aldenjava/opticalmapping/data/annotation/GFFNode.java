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

import java.util.LinkedHashMap;

import aldenjava.opticalmapping.GenomicPosNode;

public class GFFNode extends AnnotationNode {

	public String seqname;
	public String source;
	public String feature;
	public long start;
	public long end;
	public Double score;
	public String strand;
	public Integer frame;
	public LinkedHashMap<String, String> attribute;

	public GFFNode(String seqname, String source, String feature, long start, long end, Double score, String strand, Integer frame, String attributestring) {
		super(new GenomicPosNode(seqname, start, end));
		this.seqname = seqname;
		this.source = source;
		this.feature = feature;
		this.start = start;
		this.end = end;
		this.score = score;
		this.strand = strand;
		this.frame = frame;
		String[] a = attributestring.split(";");
		attribute = new LinkedHashMap<String, String>();
		for (String s : a) {
			s = s.trim();
			String[] l = s.split("(\\s|=)+");
			attribute.put(l[0], l[1]);
		}
	}

	@Override
	public String getAnnoType() {
		return "GFF";
	}
	@Override
	public String getName() {
		if (attribute.containsKey("gene_name"))
			return attribute.get("gene_name");
		if (attribute.containsKey("gene_id"))
			return attribute.get("gene_id");
		return super.getName();
	}
}
