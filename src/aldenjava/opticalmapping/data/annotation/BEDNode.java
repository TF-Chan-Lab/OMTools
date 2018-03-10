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

import java.awt.Color;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;

/**
 * 
 * Node for storing entry from a BED file
 * 
 * @author Alden
 *
 */
public class BEDNode extends AnnotationNode {
	public String chrom;
	public long chromStart;
	public long chromEnd;
	public String name;
	public Integer score; // [0, 1000]
	public String strand;
	public Long thickStart;
	public Long thickEnd;
	public Color itemRgb;
	public Integer blockCount;
	public List<Long> blockSizes;
	public List<Long> blockStarts;
	
	public BEDNode(String chrom, long chromStart, long chromEnd) {
		this(chrom, chromStart, chromEnd, null, null, null, null, null, null, null, null, null);
	}
	public BEDNode(GenomicPosNode region) {
		this(region.ref, region.start, region.stop);
	}

	
	public BEDNode(String chrom, long chromStart, long chromEnd, String name,
			Integer score, String strand, Long thickStart, Long thickEnd,
			Color itemRgb, Integer blockCount, List<Long> blockSizes,
			List<Long> blockStarts) {
		super(new GenomicPosNode(chrom, chromStart, chromEnd));
		this.chrom = chrom;
		this.chromStart = chromStart;
		this.chromEnd = chromEnd;
		this.name = name;
		this.score = score;
		this.strand = strand;
		this.thickStart = thickStart;
		this.thickEnd = thickEnd;
		this.itemRgb = itemRgb;
		this.blockCount = blockCount;
		this.blockSizes = blockSizes;
		this.blockStarts = blockStarts;
	}




	@Override
	public String getAnnoType() {
		return "BED";
	}
	
	@Override
	public Color getColor() {
		Color color;
		if (itemRgb != null)
			color = itemRgb;
		else
			color = Color.BLACK;
		return color;
	}
	
	@Override
	public String getName() {
		if (name != null)
			return name;
		else
			return super.getName();
	}

}
