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


package aldenjava.opticalmapping.visualizer.vobject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JTextArea;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.utils.VUtils;

public class VAlignment extends VObject{
	
	private VRuler vRefRuler = null;
	private VReference vref = null;
	private VMolecule vmole = null;
	private VAlignmentLines vlines = null;
	private VRuler vMoleRuler = null;
	private JTextArea label = null;
	long fragDisplace;
	long refDisplace;
	private String id;
	private List<OptMapResultNode> resultlist;
	public VAlignment(DataNode ref, List<OptMapResultNode> resultlist) {
		if (ref == null)
			throw new NullPointerException("ref");
		if (resultlist == null)
			throw new NullPointerException("resultlist");
		if (resultlist.isEmpty())
			throw new IllegalArgumentException("resultlist");

		// Basic information
		DataNode originalRef = ref;
		ref = new DataNode(ref);
		resultlist = new ArrayList<>(resultlist);
		Collections.sort(resultlist, OptMapResultNode.subfragstartstopcomparator);
		DataNode mole = resultlist.get(0).parentFrag; 
		id = mole.name;
		
		// Build Caption for the results (Results not modified)
//		StringBuilder labelCaption = new StringBuilder(String.format("%-30s%-15s%-10s%-10s%-15s%-10s%-10s%-10s%-20s\n", "RefRegion", "MoleRegion", "Orient", "Score", "Confidence", "FPR", "FNR", "Scale", "SubFragRatio"));
		StringBuilder labelCaption = new StringBuilder(String.format("%-30s%-30s%-10s%-10s%-15s%-10s%-10s%-10s%-20s\n", "RefRegion", "MoleRegion", "Orient", "Score", "Confidence", "FPR", "FNR", "Scale", "SubFragRatio"));
		for (OptMapResultNode result : resultlist)
//			labelCaption.append(String.format("%-30s%-15s%-10s%-10.1f%-15.2f%-10.6f%-10.2f%-10.2f%-20.2f\n", result.mappedRegion.toString(), String.format("%d-%d", result.subfragstart, result.subfragstop), result.mappedstrand==1?"forward":result.mappedstrand==-1?"reverse":"", result.mappedscore, result.confidence, result.getFPRate(), result.getFNRate(), result.getMapScale(), result.getSubFragRatio()));
			labelCaption.append(String.format("%-30s%-30s%-10s%-10.1f%-15.2f%-10.6f%-10.2f%-10.2f%-20.2f\n", result.mappedRegion.toString(), result.getMoleMappedRegion(), result.mappedstrand==1?"forward":result.mappedstrand==-1?"reverse":"", result.mappedscore, result.confidence, result.getFPRate(), result.getFNRate(), result.getMapScale(), result.getSubFragRatio()));
			
		label = new JTextArea(labelCaption.toString());
		label.setFont(new Font("monospaced", Font.PLAIN, label.getFont().getSize()));
		label.setEditable(false);

		// modify result		
		boolean reversed = false;
		double refScale = 1.0;
		int orgStrand = resultlist.get(0).mappedstrand;
		if (ViewSetting.alignmentViewModify)
			if (ViewSetting.alignmentViewModifyScale)
				resultlist = VUtils.modifyResultOnRef(resultlist, ref);
			else
				resultlist = VUtils.modifyAllReverseOnRef(resultlist, ref);
		this.resultlist = resultlist;
		
		// Save all reverse and scaling information modification
		if (orgStrand != resultlist.get(0).mappedstrand)
			reversed = true;
		refScale = (ref.length() - (ref.refp.length)) / (double) (originalRef.length() - (originalRef.refp.length));
		
		// Set reference region (auto-selection by max and min aligned subrefpos, and have +- 1 on them)
		int subrefstart = Integer.MAX_VALUE;
		int subrefstop = Integer.MIN_VALUE;
//		long mappedstart = Long.MAX_VALUE;
//		long mappedstop = Long.MIN_VALUE;
		

		for (OptMapResultNode result : resultlist)
		{
			if (result.subrefstart < subrefstart)
				subrefstart = result.subrefstart;
			if (result.subrefstop > subrefstop)
				subrefstop = result.subrefstop;

//			if (result.mappedRegion.start < mappedstart)
//				mappedstart = result.mappedRegion.start;
//			if (result.mappedRegion.stop > mappedstop)
//				mappedstop = result.mappedRegion.stop;
		}		
//		subrefstart--;
//		subrefstop++;
		
		
		// Calculate displacement of reference and fragment
		// Rationale: (Note that results are modified
		// 1. If there is any forward aligned result, uses it to parse the displacement and leave
		// 2. Else use the last reverse aligned result to parse displacement
		long refDisplace = 0;
		long fragDisplace = 0;
		for (OptMapResultNode result : resultlist)
			if (result.mappedstrand == 1)
			{
				// Note fragDisplace depends on reference coordinate, while refDisplace depends on fragment coordinate
				long fraglen = result.length(0, result.subfragstart - 1);
				long reflen = ref.length(subrefstart, result.subrefstart - 1); // If subrefstart == result.subrefstart then the length is zero
				if (fraglen > reflen) // Only need displace
				{
					fragDisplace = 0;
					refDisplace = fraglen - reflen;
				}
				else 
					if (reflen > fraglen)
					{
						refDisplace = 0;
						fragDisplace = reflen - fraglen;
					}
					else
					{
						refDisplace = 0;
						fragDisplace = 0;
					}
				break;
			}
			else
				if (result.mappedstrand == -1)
				{
					long fraglen = result.length(0, result.subfragstop - 1);
					long reflen = ref.length(subrefstart, result.subrefstart - 1);
					if (fraglen > reflen)
					{
						fragDisplace = 0;
						refDisplace = fraglen - reflen;
					}
					else
					{
						refDisplace = 0;
						fragDisplace = reflen - fraglen;
					}
					
				}

		// Correct start point (if the reference exists, let the start point decrease
		long refstartCorrect = refDisplace;
		long refstopCorrect = fragDisplace + resultlist.get(0).length() - refDisplace - ref.length(subrefstart, subrefstop);
		long startpoint = ref.length(0, subrefstart - 1);
		long stoppoint = ref.length(0, subrefstop + 1);
		startpoint -= refstartCorrect;
		if (refstopCorrect > 0)
			stoppoint += refstopCorrect;
		if (startpoint < 1)
		{
			refstartCorrect -= (1 - startpoint);
			startpoint = 1;
		}
		if (stoppoint > ref.size) stoppoint = ref.size;

		// Construct vref, vmole and vlines
		List<SimpleLongLocation> refLocList = GenomicPosNode.getLoc(OptMapResultNode.getMappedRegion(resultlist));
		vref = new VReference(ref);
		vref.setBaseColor(ViewSetting.unalignedRefColor);
		vref.addRegionColor(refLocList, ViewSetting.alignedRefColor);
		vref.setStartEndPoint(new SimpleLongLocation(startpoint, stoppoint));

		List<SimpleLongLocation> moleLocList = GenomicPosNode.getLoc(OptMapResultNode.getMoleMappedRegion(resultlist));
		vmole = new VMolecule(mole);
		vmole.setBaseColor(ViewSetting.unalignedMoleculeColor);
		vmole.addRegionColor(moleLocList, ViewSetting.alignedMoleculeColor);
		vlines = new VAlignmentLines(ref, resultlist, subrefstart, subrefstop, refDisplace, fragDisplace);
		
		// Construct ruler. Ruler requires special handling as reference is modified previously
		vRefRuler = new VRuler();
		{
			int p = Arrays.binarySearch(ref.refp, startpoint);
			if (p < 0)
				p = (p + 1) * -1;
			p -= 1;
			long realstartpoint = (long) ((startpoint - p) / refScale + p);
			p = Arrays.binarySearch(ref.refp, stoppoint);
			if (p < 0)
				p = (p + 1) * -1;
			p -= 1;
			long realstoppoint = (long) ((stoppoint - p) / refScale + p);
			if (!reversed)
				vRefRuler.setStartEndPoint(new SimpleLongLocation(realstartpoint, realstoppoint));
			else
				vRefRuler.setStartEndPoint(new SimpleLongLocation(originalRef.length() - realstartpoint + 1, originalRef.length() - realstoppoint + 1));
			vRefRuler.setScale(refScale);
			vRefRuler.setReverse(reversed);
		}
		vMoleRuler = new VRuler(); 
		{
			vMoleRuler.setStartEndPoint(new SimpleLongLocation(1, mole.size));
			vMoleRuler.setScale(1);
			vMoleRuler.setReverse(false);
			vMoleRuler.setInvert(true);
		}
		
		
		this.refDisplace = refDisplace - refstartCorrect; // This is necessary to minus refstartCorrect as more reference is displayed and hence the 
		this.fragDisplace = fragDisplace;
		
		this.add(vRefRuler);
		this.add(vref);
		this.add(vlines);
		this.add(vmole);
		this.add(vMoleRuler);
		this.add(label);
		this.autoSetSize();
		this.reorganize();
	}
	
	public String getMoleculeID()
	{
		return id;
	}
	public int getPartialMapCount()
	{
		return resultlist.size();
	}
	@Override
	public void setDNARatio(double dnaRatio)
	{
		if (vRefRuler != null)
			vRefRuler.setDNARatio(dnaRatio);
		if (vMoleRuler != null)
			vMoleRuler.setDNARatio(dnaRatio);
		if (vref != null)
			vref.setDNARatio(dnaRatio);
		if (vmole != null)
			vmole.setDNARatio(dnaRatio);
		if (vlines != null)
			vlines.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}
	@Override
	public void setRatio(double ratio)
	{
		if (vRefRuler != null)
			vRefRuler.setRatio(ratio);
		if (vMoleRuler != null)
			vMoleRuler.setRatio(ratio);
		if (vref != null)
			vref.setRatio(ratio);
		if (vmole != null)
			vmole.setRatio(ratio);
		if (vlines != null)
			vlines.setRatio(ratio);
		super.setRatio(ratio);
 
	}
	@Override
	public void autoSetSize() 
	{
		vRefRuler.autoSetSize();
		vMoleRuler.autoSetSize();
		vref.autoSetSize();
		vmole.autoSetSize();
		vlines.autoSetSize();
		
		label.setSize(1200, (resultlist.size() + 1) * 20);
		int width = (int) (vref.getWidth() + refDisplace / dnaRatio * ratio);
		if (vmole.getWidth() + fragDisplace / dnaRatio * ratio > width)
			width = (int) (vmole.getWidth() + fragDisplace / dnaRatio * ratio);
		if (width < label.getWidth())
			width = label.getWidth();
		this.setSize(width, vRefRuler.getHeight() + vMoleRuler.getHeight() + vref.getHeight() + vlines.getHeight() + vmole.getHeight() + label.getHeight());		
	}
	@Override
	public void reorganize()
	{
		int vy = 0;
//		vruler.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
//		vruler.setLocation(0, vy);
		vRefRuler.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
		vy += vRefRuler.getHeight();
		vref.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
		vy += vref.getHeight();
		vlines.setLocation((int) ((fragDisplace<refDisplace?fragDisplace:refDisplace) / dnaRatio * ratio), vy);
		vy += vlines.getHeight();
		vmole.setLocation((int) (fragDisplace / dnaRatio * ratio), vy);
		vy += vmole.getHeight();
		vMoleRuler.setLocation((int) (fragDisplace / dnaRatio * ratio), vy);
		vy += vMoleRuler.getHeight();
		label.setLocation(0, vy);
	}
	
	@Override
	public long getDNALength()
	{
		return 0;
	}
}
	

class VAlignmentLines extends VObject{
	private DataNode ref;
	private List<OptMapResultNode> resultlist;
	private int subrefstart;
	private int subrefstop;
	private long refDisplace;
	private long fragDisplace;
	
	public VAlignmentLines(DataNode ref,
			List<OptMapResultNode> resultlist, int subrefstart, int subrefstop,
			long refDisplace, long fragDisplace) {
		super();
		this.ref = ref;
		this.resultlist = resultlist;
		this.subrefstart = subrefstart;
		this.subrefstop = subrefstop;
		setLineDisplace(refDisplace, fragDisplace);
	}
	
	public void setLineDisplace(long refDisplace, long fragDisplace)
	{
		this.refDisplace = refDisplace;
		this.fragDisplace = fragDisplace;
	}

	@Override
	public void autoSetSize() 
	{
		int refwidth = (int) ((ref.length(subrefstart, subrefstop) + refDisplace) / dnaRatio * ratio);
		int fragwidth = (int) ((resultlist.get(0).length() + fragDisplace) / dnaRatio * ratio);
		int width;
		if (refwidth > fragwidth)
			width = refwidth;
		else
			width = fragwidth;
		this.setSize(width, (int) (ViewSetting.alignmentLineHeight * ratio));
	}
	@Override
	public void reorganize() {
	}
	
	@Override 
	public void paintComponent(Graphics graphics)
	{		
		Graphics2D g = (Graphics2D) graphics;
		int fragy = this.getHeight() / 2;
		DataNode f = resultlist.get(0).parentFrag;
		for (OptMapResultNode result : resultlist)
		{
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke((int) (ViewSetting.signalStrokeWidth * ratio)));
			String precigar = result.cigar.getPrecigar();
			int direction = 1;
			if (result.mappedstrand == -1)
				direction = -1;
			int rpointer = result.subrefstart - 1;
			int fpointer = result.subfragstart - 1;
			if (direction == -1)
				fpointer += 1;
	
			for (char c : precigar.toCharArray())
			{
				if (c == 'M')
				{
					g.drawLine((int) ((refDisplace + ref.length(subrefstart, rpointer)) / dnaRatio * ratio), (int) (fragy - ViewSetting.alignmentLineLength * ratio / 2), (int) ((fragDisplace + f.length(0, fpointer)) / dnaRatio * ratio), fragy + (int) (ViewSetting.alignmentLineLength * ratio / 2));

					rpointer++;
					fpointer += direction;
				}
				else
					if (c == 'I')
						fpointer += direction;
					else
						if (c == 'D')
							rpointer++;		
			}
		}				
	}
	@Override
	public long getDNALength()	{
		return 0;
	}
	
}
/*
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTextArea;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.utils.VUtils;

public class VAlignment extends VObject{
	
	private VRuler vruler = null;
	private VReference vref = null;
	private VMolecule vmole = null;
	private VAlignmentLines vlines = null;
	private JTextArea label = null;
	private long fragDisplace;
	private long refDisplace;
	private String id;
	private List<OptMapResultNode> resultlist;
	public VAlignment(DataNode ref, List<OptMapResultNode> resultlist)
	{
		if (ref == null)
			throw new NullPointerException("Null ref");
		if (resultlist == null)
			throw new NullPointerException("Null resultlist");
		if (resultlist.isEmpty())
			throw new IllegalArgumentException("Empty resultlist");

		// Basic information
//		DataNode originalRef = ref;
//		ref = new DataNode(ref);
		DataNode mole = resultlist.get(0).parentFrag; 
		id = mole.name;
		
		// Build Caption for the results (Results not modified)
		StringBuilder labelCaption = new StringBuilder(String.format("%-30s%-15s%-10s%-10s%-15s%-10s%-10s%-10s%-20s\n", "RefRegion", "MoleRegion", "Orient", "Score", "Confidence", "FPR", "FNR", "Scale", "SubFragRatio"));
		for (OptMapResultNode result : resultlist)
			labelCaption.append(String.format("%-30s%-15s%-10s%-10.1f%-15.2f%-10.6f%-10.2f%-10.2f%-20.2f\n", result.mappedRegion.toString(), String.format("%d-%d", result.subfragstart, result.subfragstop), result.mappedstrand==1?"forward":result.mappedstrand==-1?"reverse":"", result.mappedscore, result.confidence, result.getFPRate(), result.getFNRate(), result.getMapScale(), result.getSubFragRatio()));
		label = new JTextArea(labelCaption.toString());
		label.setFont(new Font("monospaced", Font.PLAIN, label.getFont().getSize()));
		label.setEditable(false);


		// Auto-detect reference start and stop
		int subrefstart = Integer.MAX_VALUE;
		int subrefstop = Integer.MIN_VALUE;
		for (OptMapResultNode result : resultlist)	{
			if (result.subrefstart < subrefstart)
				subrefstart = result.subrefstart;
			if (result.subrefstop > subrefstop)
				subrefstop = result.subrefstop;
		}		
		// Calculate displacement of reference and fragment
		// Rationale:
		// 1. If there is any forward aligned result, uses it to parse the displacement and leave
		// 2. Else use the last reverse aligned result to parse displacement
		OptMapResultNode result = resultlist.get(0);
		long refDisplace = 0;
		long startpoint;
		long stoppoint;
		if (result.mappedstrand == 1) { 
			startpoint = result.mappedRegion.start - (long) (result.parentFrag.refp[result.subfragstart - 1] / result.getMapScale());
			stoppoint = result.mappedRegion.stop + (long) ((result.parentFrag.length() - result.parentFrag.refp[result.subfragstop]) / result.getMapScale());
			if (startpoint < 1) {
				refDisplace = 1 - startpoint; 
				startpoint = 1;		
			}
			if (stoppoint > ref.length())
				stoppoint = ref.length();
		}
		else {
			startpoint = result.mappedRegion.start - (long) ((result.parentFrag.length() - result.parentFrag.refp[result.subfragstart]) * result.getMapScale());
			stoppoint = result.mappedRegion.stop + (long) (result.parentFrag.refp[result.subfragstop - 1] * result.getMapScale());
//			System.out.println(((result.parentFrag.length() - result.parentFrag.refp[result.subfragstart]))) ;
//			System.out.println(stoppoint);
			if (startpoint < 1)
				startpoint = 1;		
			if (stoppoint > ref.length()) {
				refDisplace = stoppoint - ref.length(); 
				stoppoint = ref.length();
			}
//			System.out.println(stoppoint);
//			System.out.println("R" + refDisplace);
		}
		
//		long fragDisplace = 0;
//		for (OptMapResultNode result : resultlist)
//			if (result.mappedstrand == 1)
//			{
//				// Note fragDisplace depends on reference coordinate, while refDisplace depends on fragment coordinate
//				long fraglen = result.length(0, result.subfragstart - 1);
//				long reflen = ref.length(subrefstart, result.subrefstart - 1); // If subrefstart == result.subrefstart then the length is zero
//				if (fraglen > reflen) // Only need displace
//				{
//					fragDisplace = 0;
//					refDisplace = fraglen - reflen;
//				}
//				else 
//					if (reflen > fraglen)
//					{
//						refDisplace = 0;
//						fragDisplace = reflen - fraglen;
//					}
//					else
//					{
//						refDisplace = 0;
//						fragDisplace = 0;
//					}
//				break;
//			}
//			else
//				if (result.mappedstrand == -1)
//				{
//					long fraglen = result.length(0, result.subfragstop - 1);
//					long reflen = ref.length(subrefstart, result.subrefstart - 1);
//					if (fraglen > reflen)
//					{
//						fragDisplace = 0;
//						refDisplace = fraglen - reflen;
//					}
//					else
//					{
//						refDisplace = 0;
//						fragDisplace = reflen - fraglen;
//					}
//					
//				}

		// Correct start point (if the reference exists, let the start point decrease
//		long refstartCorrect = refDisplace;
//		long refstopCorrect = fragDisplace + resultlist.get(0).length() - refDisplace - ref.length(subrefstart, subrefstop);
//		long startpoint = ref.length(0, subrefstart - 1);
//		long stoppoint = ref.length(0, subrefstop + 1);
//		startpoint -= refstartCorrect;
//		if (refstopCorrect > 0)
//			stoppoint += refstopCorrect;
//		if (startpoint < 1)
//		{
//			refstartCorrect -= (1 - startpoint);
//			startpoint = 1;
//		}
//		if (stoppoint > ref.size) stoppoint = ref.size;

		// Construct vref, vmole and vlines
		List<SimpleLongLocation> refLocList = GenomicPosNode.getLoc(OptMapResultNode.getMappedRegion(resultlist));

		
		
		System.out.println(startpoint);
		System.out.println(stoppoint);
		// Build reference
		vref = new VReference(ref);
		vref.setBaseColor(Color.ORANGE);
		vref.addRegionColor(refLocList, Color.RED);
		vref.setStartEndPoint(new SimpleLongLocation(startpoint, stoppoint));
		vref.setReverse(resultlist.get(0).mappedstrand == -1);
		vref.setScale(resultlist.get(0).getMapScale());
		
		
		
		vruler = new VRuler();
		vruler.setStartEndPoint(new SimpleLongLocation(startpoint, stoppoint));
		System.out.println(resultlist.get(0).mappedstrand == -1);
		vref.setReverse(resultlist.get(0).mappedstrand == -1);
		vref.setScale(resultlist.get(0).getMapScale());
		
		this.refDisplace = refDisplace;
//		{
//			int p = Arrays.binarySearch(ref.refp, startpoint);
//			if (p < 0)
//				p = (p + 1) * -1;
//			p -= 1;
//			long realstartpoint = (long) ((startpoint - p) / refScale + p);
//			p = Arrays.binarySearch(ref.refp, stoppoint);
//			if (p < 0)
//				p = (p + 1) * -1;
//			p -= 1;
//			long realstoppoint = (long) ((stoppoint - p) / refScale + p);
//			if (!reversed)
//				vruler.setStartEndPoint(new SimpleLongLocation(realstartpoint, realstoppoint));
//			else
//				vruler.setStartEndPoint(new SimpleLongLocation(originalRef.length() - realstartpoint + 1, originalRef.length() - realstoppoint + 1));
//			vruler.setScale(refScale);
//			vruler.setReverse(reversed);
//		}
		
		// modify result		
//		boolean reversed = false;
//		double refScale = 1.0;
//		int orgStrand = resultlist.get(0).mappedstrand;
//		if (ViewSetting.alignmentViewModify)
//			if (ViewSetting.alignmentViewModifyScale)
//				resultlist = VUtils.modifyResultOnRef(resultlist, ref);
//			else
//				resultlist = VUtils.modifyAllReverseOnRef(resultlist, ref);
//		else
//			; // Do nothing
		
		
		
//		resultlist = VUtils.modifyResult(resultlist);
//		this.resultlist = resultlist;
//		
//		// Save all reverse and scaling information modification
//		if (orgStrand != resultlist.get(0).mappedstrand)
//			reversed = true;
//		refScale = (ref.length() - (ref.refp.length)) / (double) (originalRef.length() - (originalRef.refp.length));
		
//		// Set reference region (auto-selection by max and min aligned subrefpos, and have +- 1 on them)
//		int subrefstart = Integer.MAX_VALUE;
//		int subrefstop = Integer.MIN_VALUE;
////		long mappedstart = Long.MAX_VALUE;
////		long mappedstop = Long.MIN_VALUE;
//		
//
//		for (OptMapResultNode result : resultlist)
//		{
//			if (result.subrefstart < subrefstart)
//				subrefstart = result.subrefstart;
//			if (result.subrefstop > subrefstop)
//				subrefstop = result.subrefstop;
//
////			if (result.mappedRegion.start < mappedstart)
////				mappedstart = result.mappedRegion.start;
////			if (result.mappedRegion.stop > mappedstop)
////				mappedstop = result.mappedRegion.stop;
//		}		
////		subrefstart--;
////		subrefstop++;
//		
//		
//		// Calculate displacement of reference and fragment
//		// Rationale: (Note that results are modified
//		// 1. If there is any forward aligned result, uses it to parse the displacement and leave
//		// 2. Else use the last reverse aligned result to parse displacement
//		long refDisplace = 0;
//		long fragDisplace = 0;
//		for (OptMapResultNode result : resultlist)
//			if (result.mappedstrand == 1)
//			{
//				// Note fragDisplace depends on reference coordinate, while refDisplace depends on fragment coordinate
//				long fraglen = result.length(0, result.subfragstart - 1);
//				long reflen = ref.length(subrefstart, result.subrefstart - 1); // If subrefstart == result.subrefstart then the length is zero
//				if (fraglen > reflen) // Only need displace
//				{
//					fragDisplace = 0;
//					refDisplace = fraglen - reflen;
//				}
//				else 
//					if (reflen > fraglen)
//					{
//						refDisplace = 0;
//						fragDisplace = reflen - fraglen;
//					}
//					else
//					{
//						refDisplace = 0;
//						fragDisplace = 0;
//					}
//				break;
//			}
//			else
//				if (result.mappedstrand == -1)
//				{
//					long fraglen = result.length(0, result.subfragstop - 1);
//					long reflen = ref.length(subrefstart, result.subrefstart - 1);
//					if (fraglen > reflen)
//					{
//						fragDisplace = 0;
//						refDisplace = fraglen - reflen;
//					}
//					else
//					{
//						refDisplace = 0;
//						fragDisplace = reflen - fraglen;
//					}
//					
//				}
//
//		// Correct start point (if the reference exists, let the start point decrease
//		long refstartCorrect = refDisplace;
//		long refstopCorrect = fragDisplace + resultlist.get(0).length() - refDisplace - ref.length(subrefstart, subrefstop);
//		long startpoint = ref.length(0, subrefstart - 1);
//		long stoppoint = ref.length(0, subrefstop + 1);
//		startpoint -= refstartCorrect;
//		if (refstopCorrect > 0)
//			stoppoint += refstopCorrect;
//		if (startpoint < 1)
//		{
//			refstartCorrect -= (1 - startpoint);
//			startpoint = 1;
//		}
//		if (stoppoint > ref.size) stoppoint = ref.size;
//
		//Construct vref, vmole and vlines
//		List<SimpleLongLocation> refLocList = GenomicPosNode.getLoc(OptMapResultNode.getMappedRegion(resultlist));
//		vref = new VReference(ref);
//		vref.setBaseColor(Color.ORANGE);
//		vref.addRegionColor(refLocList, Color.RED);
//		vref.setStartEndPoint(new SimpleLongLocation(startpoint, stoppoint));
//		vref = new VReference(ref, resultlist);
//		vref.setStartEndPoint(startpoint, stoppoint);
		
		List<SimpleLongLocation> moleLocList = GenomicPosNode.getLoc(OptMapResultNode.getMoleMappedRegion(resultlist));
//		vmole = new VMolecule(resultlist);
		vmole = new VMolecule(mole);
		vmole.addRegionColor(moleLocList, Color.YELLOW);
//		vlines = new VAlignmentLines(ref, resultlist, subrefstart, subrefstop, refDisplace, fragDisplace);
		vlines = new VAlignmentLines(vref, resultlist, refDisplace, fragDisplace);
		if (fragDisplace != 0)
			System.out.println("Wow");
		
		
		// Construct ruler. Ruler requires special handling as reference is modified previously
		
		
//		this.refDisplace = refDisplace - refstartCorrect; // This is necessary to minus refstartCorrect as more reference is displayed and hence the 
//		this.fragDisplace = fragDisplace;
		this.resultlist = resultlist;
		this.add(vruler);
		this.add(vref);
		this.add(vlines);
		this.add(vmole);
		this.add(label);
		this.autoSetSize();
		this.reorganize();
	}
	
	public String getMoleculeID()
	{
		return id;
	}
	public int getPartialMapCount()
	{
		return resultlist.size();
	}
	@Override
	public void setDNARatio(double dnaRatio)
	{
		if (vruler != null)
			vruler.setDNARatio(dnaRatio);
		if (vref != null)
			vref.setDNARatio(dnaRatio);
		if (vmole != null)
			vmole.setDNARatio(dnaRatio);
		if (vlines != null)
			vlines.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}
	@Override
	public void setRatio(double ratio)
	{
		if (vruler != null)
			vruler.setRatio(ratio);
		if (vref != null)
			vref.setRatio(ratio);
		if (vmole != null)
			vmole.setRatio(ratio);
		if (vlines != null)
			vlines.setRatio(ratio);
		super.setRatio(ratio);
 
	}
	@Override
	public void autoSetSize() 
	{
		vruler.autoSetSize();
		vmole.autoSetSize();
		vref.autoSetSize();
		vlines.autoSetSize();
		label.setSize(800, (resultlist.size() + 1) * 20);
		int width = (int) (vref.getWidth() + refDisplace / dnaRatio * ratio);
		if (vmole.getWidth() + fragDisplace / dnaRatio * ratio > width)
			width = (int) (vmole.getWidth() + fragDisplace / dnaRatio * ratio);
		if (width < label.getWidth())
			width = label.getWidth();
		this.setSize(width, vruler.getHeight() + vref.getHeight() + vlines.getHeight() + vmole.getHeight() + label.getHeight());		
	}
	@Override
	public void reorganize()
	{
		int vy = 0;
//		vruler.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
//		vruler.setLocation(0, vy);
		vruler.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
		vy += vruler.getHeight();
		vref.setLocation((int) (refDisplace / dnaRatio * ratio), vy);
		vy += vref.getHeight();
		vlines.setLocation((int) ((fragDisplace<refDisplace?fragDisplace:refDisplace) / dnaRatio * ratio), vy);
		vy += vlines.getHeight();
		vmole.setLocation((int) (fragDisplace / dnaRatio * ratio), vy);
		vy += vmole.getHeight();
		label.setLocation(0, vy);
	}
	
	@Override
	public long getDNALength() {
		return 0;
	}
}
	

class VAlignmentLines extends VObject{
//	private final DataNode ref;
	private final List<OptMapResultNode> resultlist;
	VReference vref;
//	private int subrefstart;
//	private int subrefstop;
	SimpleLongLocation startEndPoint;

	private long refDisplace;
	private long fragDisplace;
	
	public VAlignmentLines(VReference vref,
			List<OptMapResultNode> resultlist,
			long refDisplace, long fragDisplace) {
		super();
		this.vref = vref;
		this.resultlist = resultlist;
//		this.subrefstart = subrefstart;
//		this.subrefstop = subrefstop;
		setLineDisplace(refDisplace, fragDisplace);
	}
	
	public void setLineDisplace(long refDisplace, long fragDisplace)
	{
		this.refDisplace = refDisplace;
		this.fragDisplace = fragDisplace;
	}

	@Override
	public void autoSetSize() 
	{
		int refwidth = (int) ((vref.getDNALength() + refDisplace) / dnaRatio * ratio);
		int fragwidth = (int) ((resultlist.get(0).length() + fragDisplace) / dnaRatio * ratio);
		int width;
		if (refwidth > fragwidth)
			width = refwidth;
		else
			width = fragwidth;
		this.setSize(width, (int) (ViewSetting.alignmentLineHeight * ratio));
	}
	@Override
	public void reorganize() {
	}
	
	@Override 
	public void paintComponent(Graphics graphics)
	{		
		DataNode ref = vref.data;
		Graphics2D g = (Graphics2D) graphics;
		int fragy = this.getHeight() / 2;
		DataNode f = resultlist.get(0).parentFrag;
		for (OptMapResultNode result : resultlist) {
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke((int) (ViewSetting.signalStrokeWidth * ratio)));
			String precigar = result.cigar.getPrecigar();
			int direction = 1;
			if (result.mappedstrand == -1)
				direction = -1;
			int rpointer = result.subrefstart - 1;
			int fpointer = result.subfragstart - 1;
			if (direction == -1)
				fpointer += 1;
			for (char c : precigar.toCharArray())
			{
				if (c == 'M')
				{
					
					double sigPos = (ref.refp[rpointer] - vref.startEndPoint.min + 1) * vref.getScale() / dnaRatio * ratio;
					if (vref.getReverse()) {
						sigPos = vref.getDNALength() / dnaRatio * ratio - sigPos + vref.getScale() / dnaRatio * ratio;
					}
					g.drawLine((int) (refDisplace / dnaRatio * ratio + sigPos), (int) (fragy - ViewSetting.alignmentLineLength * ratio / 2), (int) ((fragDisplace + f.refp[fpointer]) / dnaRatio * ratio), fragy + (int) (ViewSetting.alignmentLineLength * ratio / 2));
//					System.out.println((int) (refDisplace / dnaRatio * ratio));
//					System.out.println((int) (sigPos));
//					System.out.println((int) ((fragDisplace + f.refp[fpointer])) / dnaRatio * ratio);

					rpointer++;
					fpointer += direction;
				}
				else
					if (c == 'I')
						fpointer += direction;
					else
						if (c == 'D')
							rpointer++;		
			}
		}				
	}
	public long getDNALength()
	{
		return 0;
	}
	
}*/