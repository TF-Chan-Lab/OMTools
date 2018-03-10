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

import aldenjava.opticalmapping.GenomicPosNode;
/**
 * Node for storing entry from an AGP file
 * 
 * @author Alden
 *
 */
public class AGPNode extends AnnotationNode {
	
	public String obj_name; 
	public long object_beg;
	public long obj_end;
	public int part_number;
	public char component_type;
	
	public String component_id; // 6a
	public Long component_beg; // 7a
	public Long component_end; // 8a
	public Integer orientation; // 9a
	
	public Long gap_length; // 6b
	public String gap_type; // 7b
	public String linkage; // 8b
	public String linkage_evidence; // 9b
	
	
	
	public AGPNode(String obj_name, long object_beg, long obj_end, int part_number, char component_type, String component_id, Long component_beg, Long component_end,
			Integer orientation) {
		super(new GenomicPosNode(obj_name, object_beg, obj_end));
		this.obj_name = obj_name;
		this.object_beg = object_beg;
		this.obj_end = obj_end;
		this.part_number = part_number;		
		this.component_type = component_type;
		if (component_type == 'N' || component_type == 'U')
			throw new IllegalArgumentException("The AGP with N/U should give gap information");
		this.component_id = component_id;
		this.component_beg = component_beg;
		this.component_end = component_end;
		this.orientation = orientation;
	}

	
	public AGPNode(String obj_name, long object_beg, long obj_end, int part_number, char component_type, Long gap_length, String gap_type, String linkage, String linkage_evidence) {
		super(new GenomicPosNode(obj_name, object_beg, obj_end));
		this.obj_name = obj_name;
		this.object_beg = object_beg;
		this.obj_end = obj_end;
		this.part_number = part_number;		
		this.component_type = component_type;

		if (!(component_type == 'N' || component_type == 'U'))
			throw new IllegalArgumentException("The AGP with non-N/U should give component information");
		this.gap_length = gap_length;
		this.gap_type = gap_type;
		this.linkage = linkage;
		this.linkage_evidence = linkage_evidence;
	}

	
	@Override
	public String getAnnoType() {
		return "AGP";
	}
	@Override
	public String getName() {
		if (component_type == 'N' || component_type == 'U')
			return part_number + ":" + gap_length + " " + gap_type + " " + linkage + " " + linkage_evidence;	
		else
			return part_number + ":" + component_id + " " + component_beg + " " + component_end + " " + orientation;
	}

	
}
