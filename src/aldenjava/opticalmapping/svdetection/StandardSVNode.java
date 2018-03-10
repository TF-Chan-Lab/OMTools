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

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;

/**
 * @author Alden
 *
 */
public class StandardSVNode extends AnnotationNode implements Comparable<StandardSVNode>{
//	public GenomicPosNode region;
	public String type;
	public String variant_id;
	public List<String> svDetectionMethod;
	public String sample_id;
	public LinkedHashMap<String, Object> sv_attribute;
	public String zygosity;
	public String origin;
	public double score;
	public String notes;
	public String link_exp_result;
	
	// Added information?
	public int haplotype;
	public StandardSVNode(GenomicPosNode region, String type, String variant_id,
			List<String> svDetectionMethod, String sample_id,
			LinkedHashMap<String, Object> sv_attribute, String zygosity,
			String origin, double score, String notes, String link_exp_result) {
		super(region);
//		this.region = region;
		this.type = type;
		this.variant_id = variant_id;
		this.svDetectionMethod = svDetectionMethod;
		this.sample_id = sample_id;
		this.sv_attribute = sv_attribute;
		this.zygosity = zygosity;
		this.origin = origin;
		this.score = score;
		this.notes = notes;
		this.link_exp_result = link_exp_result;
		if (sv_attribute.containsKey("haplotype"))
			haplotype = Integer.parseInt((String) sv_attribute.get("haplotype"));
	}
	public StandardSVNode(StandardSVNode sv) {
		super(new GenomicPosNode(sv.region));
//		this.region = region;
		this.type = sv.type;
		this.variant_id = sv.variant_id;
		this.svDetectionMethod = new ArrayList<String>(sv.svDetectionMethod);
		this.sample_id = sv.sample_id;
		this.sv_attribute = new LinkedHashMap<String, Object>(sv.sv_attribute);
		this.zygosity = sv.zygosity;
		this.origin = sv.origin;
		this.score = sv.score;
		this.notes = sv.notes;
		this.link_exp_result = sv.link_exp_result;
		if (sv_attribute.containsKey("haplotype"))
			haplotype = Integer.parseInt((String) sv_attribute.get("haplotype"));
	}


	public StandardSVNode(SVNode sv)
	{
		super(new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp2.start));
		this.type = sv.getType();
		this.variant_id = "";
		this.svDetectionMethod = new ArrayList<String>();
		this.sample_id = "";

		if (sv instanceof IndelNode)
		{
			IndelNode indel = (IndelNode) sv;
			this.region = new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp2.start);
			this.sv_attribute = new LinkedHashMap<String, Object>();
			this.sv_attribute.put("sizeChange", indel.getIndelSize());
		}
		if (sv instanceof InversionNode)
		{
			if (((InversionNode) sv).secondInversionNode != null)
				this.region = new GenomicPosNode(sv.bp1.ref, sv.bp2.start, ((InversionNode) sv).secondInversionNode.bp1.start);
			else
				this.region = new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp2.start);
			this.sv_attribute = new LinkedHashMap<String, Object>();
		}
		if (sv instanceof TranslocationNode)
		{
			this.region = new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp1.start);
			this.sv_attribute = new LinkedHashMap<String, Object>();
			this.sv_attribute.put("newLoc", sv.bp2.ref + ":" + Long.toString(sv.bp2.start));
		}
		this.zygosity = "";
		this.origin = "";
		this.score = sv.getSupport() / (double) (sv.getSupport() + sv.getAntiSupport());
		this.notes = "";
		this.notes += "TotalSupport=" + Integer.toString(sv.getSupport());
		this.notes += ";";
		this.notes += "TotalOppose=" + Integer.toString(sv.getAntiSupport());
		
		if (!sv.evidence.isEmpty())
		{
			this.notes += ";";
			this.notes += "Support=";
			String tnotes = "";
			for (SVEvidence sve : sv.evidence)
			{
				if (!tnotes.isEmpty())
					tnotes += ",";
				tnotes += sve.result1.parentFrag.name;
			}
			this.notes += tnotes;
		}
		if (!sv.getAntiEvidenceIDList().isEmpty())
		{
			this.notes += ";";
			this.notes += "Oppose=";
			String tnotes = "";
			for (String id : sv.getAntiEvidenceIDList())
			{
				if (!tnotes.isEmpty())
					tnotes += ",";
				tnotes += id;
			}
			this.notes += tnotes;
		}
		
		
		this.link_exp_result = "";
	}
	
	public StandardSVNode(GenomicPosNode region, String type) {
		this(region, type, "", new ArrayList<String>(), "", new LinkedHashMap<String, Object>(), "", "", 0, "", "");
	}
	public boolean isSimilarTo(StandardSVNode sv, long closeSV)
	{
		if (!type.equalsIgnoreCase(sv.type))
			return false;
		
		if (!zygosity.isEmpty() && !sv.zygosity.isEmpty()) // Have zygosity to compare
			if (!zygosity.equals("Unknown") && !sv.zygosity.equals("Unknown")) // Unknown is not compared
				if (!zygosity.equals(sv.zygosity))
					return false;
		
		switch (type)
		{
			case "Insertion":
			case "Deletion":
//				if (!region.isClose(sv.region, closeRef))
//					return false;
//				if (Math.abs(Integer.parseInt((String) sv_attribute.get("sizeChange")) - Integer.parseInt((String) sv.sv_attribute.get("sizeChange"))) > closeSize)
//					return false;
				
				if (!sv_attribute.containsKey("sizeChange") || !sv.sv_attribute.containsKey("sizeChange")) {// Don't check if there's nothing that can be checked...
					int maxsize = 0;
					if ((!sv_attribute.containsKey("sizeChange") && !sv.sv_attribute.containsKey("sizeChange")))
						if (region.isClose(sv.region, closeSV))
							return true;
						else
							return false;
					else
						if (sv_attribute.containsKey("sizeChange"))
							maxsize = Integer.parseInt((String) sv_attribute.get("sizeChange"));
						else
							maxsize = Integer.parseInt((String) sv.sv_attribute.get("sizeChange"));
					return (region.isClose(sv.region, closeSV + maxsize));
				}
				int size1 = Integer.parseInt((String) sv_attribute.get("sizeChange"));
				int size2 = Integer.parseInt((String) sv.sv_attribute.get("sizeChange"));
				if (!region.isClose(sv.region, closeSV + Math.max(Math.abs(size1),  Math.abs(size2))))
					return false;								
				double closeSize = 1000 + Math.max(Math.abs(size1), Math.abs(size2)) * 0.1;
				if (Math.abs(size1 - size2) > closeSize)
					return false;
				return true;
			case "Inversion":
				return region.isClose(sv.region, closeSV);
				
			case "Insertion.site":
			case "Deletion.site":
				int closeSignalSize = 1500;
				return region.isClose(sv.region, closeSignalSize);
			default:
				System.out.println(type + " comparison is not supported.");
				return false;
		}
		
	}	
	public int getTotalSupport()
	{
		if (!notes.contains("TotalSupport="))
			return -1;
		return Integer.parseInt(notes.split("TotalSupport=")[1].split(";")[0]);
	}
	public int getTotalOppose()
	{
		if (!notes.contains("TotalOppose="))
			return -1;
		return Integer.parseInt(notes.split("TotalOppose=")[1].split(";")[0]);
	}
	public boolean isIndel()
	{
		return (type.equals("Insertion") || type.equals("Deletion"));
	}
	public boolean isSignalIndel()
	{
		return (type.equals("Insertion.site") || type.equals("Deletion.site"));
	}
//	public SVNode toSVNode(LinkedHashMap<String, FragmentNode>)
//	{
//		if (type.equalsIgnoreCase("Insertion") || type.equalsIgnoreCase("Deletion"))
//		{
//			IndelNode indel = new IndelNode(new GenomicPosNode(region.ref, region.start, region.start), new GenomicPosNode(region.ref, region.stop, region.stop), (int) sv_attribute.get("sizeChange"));
//			OptMapResultNode.newBlankMapNode(f)
//		}
//		if (sv instanceof InversionNode)
//		{
//			this.region = new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp2.start);
//			this.sv_attribute = new LinkedHashMap<String, Object>();
//		}
//		if (sv instanceof TranslocationNode)
//		{
//			this.region = new GenomicPosNode(sv.bp1.ref, sv.bp1.start, sv.bp1.start);
//			this.sv_attribute = new LinkedHashMap<String, Object>();
//			this.sv_attribute.put("newLoc", sv.bp2.ref + ":" + Long.toString(sv.bp2.start));
//		}
//
//		this.notes = "";
//		this.notes += "TotalSupport=" + Integer.toString(sv.getSupport());
//		this.notes += ";";
//		this.notes += "TotalOppose=" + Integer.toString(sv.getAntiSupport());
//		
//		if (!sv.evidence.isEmpty())
//		{
//			this.notes += ";";
//			this.notes += "Support=";
//			String tnotes = "";
//			for (SVEvidence sve : sv.evidence)
//			{
//				if (!tnotes.isEmpty())
//					tnotes += ",";
//				tnotes += sve.result1.parentFrag.id;
//			}
//			this.notes += tnotes;
//		}
//		if (!sv.getAntiEvidenceIDList().isEmpty())
//		{
//			this.notes += ";";
//			this.notes += "Oppose=";
//			String tnotes = "";
//			for (String id : sv.getAntiEvidenceIDList())
//			{
//				if (!tnotes.isEmpty())
//					tnotes += ",";
//				tnotes += id;
//			}
//			this.notes += tnotes;
//		}
//		
//		
//		this.link_exp_result = "";
//
//	}
	
	public static void setAutoVariantID(List<StandardSVNode> standardSVList)
	{
		int dummy = 0;
		for (StandardSVNode standardSV : standardSVList)
		{
			dummy++;
			standardSV.variant_id = Integer.toString(dummy);
		}
	}
	public boolean isSingleHaplotype() {
		return haplotype != 0 && (haplotype & (haplotype - 1)) == 0;
	}

	@Override
	public int compareTo(StandardSVNode sv) {
		return this.region.compareTo(sv.region);
	}


	@Override
	public String getAnnoType() {
		return "SV";
	}
	@Override
	public String getName() {
		if (this.isIndel())
			return this.type + " " + ((String) sv_attribute.get("sizeChange")) + ": " + region.toString();
		else
			return this.type + ": " + region.toString();
	}
	@Override
	public Color getColor() {
		if (type.equalsIgnoreCase("Insertion"))
			return Color.red;
		else
			if (type.equalsIgnoreCase("Deletion"))
				return Color.blue;
			else
				if (type.equalsIgnoreCase("Inversion"))
					return Color.green;
				else
					if (type.equalsIgnoreCase("Translocation"))
						return Color.cyan;
		return Color.black;
	}

	
}
