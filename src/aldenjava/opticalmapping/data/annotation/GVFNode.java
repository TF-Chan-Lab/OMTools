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

public class GVFNode extends AnnotationNode {
	public String seqid;
	public String source;
	public String type;
	public Long start;
	public Long end;
	public Double score;
	public int strand;
	public String phase;
	public LinkedHashMap<String, String> attributes;
	
	public GVFNode(String seqid, String source, String type, Long start,
			Long end, Double score, int strand, String phase,
			LinkedHashMap<String, String> attributes) {
		super(new GenomicPosNode(seqid, start, end));
		this.seqid = seqid;
		this.source = source;
		this.type = type;
		this.start = start;
		this.end = end;
		this.score = score;
		this.strand = strand;
		this.phase = phase;
		this.attributes = attributes;
	}
	@Override
	public String getAnnoType() {
		return "GVF";
	}
	
	

}