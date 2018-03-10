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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.miscellaneous.InvalidVObjectException;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.utils.VUtils;
import aldenjava.opticalmapping.visualizer.viewpanel.RegionalView;

public class VMapMolecule extends VObject implements Comparable<VMapMolecule>{
	private List<OptMapResultNode> resultlist; // assume 1. no more overlaps; 2. sorted by map start; 3. Shown in same region; 4. cannot be modified (e.g. prior reverse modification is not allowed)
//	private List<OptMapResultNode> modifiedresultlist;
	private List<VObject> moleculelist;
	private List<VObject> unalignedMoleculeList;
	private String id;
	private boolean isInvertedMolecule;
	
	public VMapMolecule(DataNode ref, List<OptMapResultNode> resultlist) throws InvalidVObjectException {
		super();
		this.id = resultlist.get(0).parentFrag.name;
		this.setToolTipText(resultlist.get(0).parentFrag.name);
//		super.setName(resultlist.get(0).id);
		this.initializeResultList(ref, resultlist);
	}

	private void initializeResultList(DataNode ref, List<OptMapResultNode> resultlist) throws InvalidVObjectException {
		this.removeAll();
		List<OptMapResultNode> sortedresultlist = new ArrayList<OptMapResultNode>(resultlist);		
		List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
		List<OptMapResultNode> modifiedresultlist = new ArrayList<OptMapResultNode>();
		List<VObject> moleculelist = new ArrayList<VObject>();
		List<VObject> unalignedMoleculeList = new ArrayList<VObject>();
		Collections.sort(sortedresultlist, OptMapResultNode.mappedstartcomparator);
		if (resultlist.get(0).subfragstart > resultlist.get(resultlist.size() - 1).subfragstop)
			isInvertedMolecule = true;
		else
			isInvertedMolecule = false;
		
		long lastmappedstop = -1;
		int lastsubfragstop = -1;
		long accumulateUnalignedSpace = -1;
		int laststrand = 0;
		OptMapResultNode lastResult = null;
		for (OptMapResultNode result : sortedresultlist) {
			boolean inverted = false;
			if (laststrand != 0)
				if (laststrand != result.mappedstrand)
					inverted = true;
			laststrand = result.mappedstrand;

			VMolecule mapMole;
			mapMole = new VMolecule(result.parentFrag);
			mapMole.setStartEndPoint(result.parentFrag.getGenomicPos(result.subfragstart, result.subfragstop, true).getLoc());
			mapMole.setScale(1 / result.getMapScale());
			mapMole.setBaseColor(Color.YELLOW);
			mapMole.setReverse(result.mappedstrand == -1);
			
			if (ViewSetting.useVariableColor) {
				// Set up regional color according to the difference in segment length 
				int[] mapSignals = result.getMapSignal();
				int[] mapRefSignals = result.getRefMapSignal();
				for (int i = 1; i < mapSignals.length; i++) {
					// range of scale: 0.5 to 1.5
					
					long querySize = result.mappedstrand==1?result.parentFrag.getSignalLength(mapSignals[i - 1], mapSignals[i]):result.parentFrag.getSignalLength(mapSignals[i], mapSignals[i - 1]);
					long refSize = ref.getSignalLength(mapRefSignals[i - 1], mapRefSignals[i]);

					// Determine the scaling factor of the individual segment
					double scale;
					if (querySize < refSize)
						scale = querySize / (double) refSize;
					else
						if (refSize < querySize)
							scale = 2 - refSize / (double) querySize;
						else
							scale = 1;
					
					// Add the boundary
					if (scale < 0.5) scale = 0.5;
					if (scale > 1.5) scale = 1.5;
					
					// compute the rgb
					float r = (float) (scale - 0.5) * 2;
					float g = (float) (1 - (scale - 0.5)) * 2;
					float b = 0;
					if (r >= 1) r = 1;
					if (g >= 1) g = 1;

					Color color = new Color(r, g, b);
					mapMole.addRegionColor(result.mappedstrand==1?new SimpleLongLocation(result.parentFrag.refp[mapSignals[i - 1]], result.parentFrag.refp[mapSignals[i]]):new SimpleLongLocation(result.parentFrag.refp[mapSignals[i]], result.parentFrag.refp[mapSignals[i - 1]]), color);
					
				}
				// Temporarily added
				if (OMView.dataColorMap.containsKey(result.parentFrag.name)) {
					for (int i = 0; i < result.parentFrag.getTotalSegment(); i++)	
						if (OMView.dataColorMap.get(result.parentFrag.name).contains(i))
							mapMole.addRegionColor(new SimpleLongLocation(result.parentFrag.refp[i - 1], result.parentFrag.refp[i]), Color.BLUE);
				}

			}
			

			// Set up color for mapped signals
			int[] mapSignals = result.getMapSignal();
			mapMole.addSignalColor(mapSignals, ViewSetting.regionalViewAlignedSignalColor);
			
			// Determine the unmapped start and stop position
			if (inverted)
				lastsubfragstop = result.parentFrag.getTotalSegment() - 1 - lastsubfragstop;
			int unmapstart = -1;
			int unmapstop = -1;
			if (lastResult == null) {
				if (result.mappedstrand == 1) {
					unmapstart = 0;
					unmapstop = result.subfragstart - 1;
				}
				else {
					unmapstart = result.subfragstart + 1;
					unmapstop = result.parentFrag.getTotalSegment() - 1;
				}
			}
			else
				if (lastResult.mappedstrand == 1)
					if (result.mappedstrand == 1) {
						unmapstart = lastResult.subfragstop + 1;
						unmapstop = result.subfragstart - 1;
					}
					else {
						unmapstart = lastResult.subfragstop + 1;
						unmapstop = result.subfragstop - 1;
					}
				else
					if (result.mappedstrand == 1) {
						unmapstart = lastResult.subfragstart + 1;
						unmapstop = result.subfragstart - 1;
					}
					else {
						unmapstart = result.subfragstart + 1;
						unmapstop = lastResult.subfragstop - 1;
					}
			
			
			VMolecule unmapMole = null;
			long unmappedLen = 0;
			if (!inverted && unmapstart <= unmapstop) {
				unmapMole = new VMolecule(result.parentFrag);
				long unmapstartloc = unmapstart==0?1:result.parentFrag.refp[unmapstart - 1];
				long unmapstoploc = unmapstop==result.parentFrag.getTotalSignal()?result.parentFrag.length():result.parentFrag.refp[unmapstop];;
				
				unmapMole.setStartEndPoint(new SimpleLongLocation(unmapstartloc, unmapstoploc));
				unmapMole.setReverse(result.mappedstrand == -1);
				unmappedLen = unmapstoploc - unmapstartloc + 1; // + 1 here?
				// Temporarily added
				if (OMView.dataColorMap.containsKey(result.parentFrag.name)) {
					for (int i = 0; i < result.parentFrag.getTotalSegment(); i++)	
						if (OMView.dataColorMap.get(result.parentFrag.name).contains(i))
							unmapMole.addRegionColor(new SimpleLongLocation(i==0?1:result.parentFrag.refp[i - 1], i==result.parentFrag.getTotalSegment()-1?result.parentFrag.size:result.parentFrag.refp[i]), Color.BLUE);
				}

				
			}
			
			if (lastResult != null)
			{
				VSpace space;
				if (inverted)
					space = new VInversion(result.mappedRegion.start - lastmappedstop + 1, unmappedLen);
				else
//						space = new VIndel(result.mappedRegion.start - lastmappedstop + 1, (long) (unmapPart.length() / ((lastResult.getMapScale() + oriResult.getMapScale()) / 2)));
					space = new VIndel(result.mappedRegion.start - lastmappedstop + 1, unmappedLen);
				moleculelist.add(space);
				VSpace unalignedSpace = new VSpace(accumulateUnalignedSpace, accumulateUnalignedSpace);
				unalignedMoleculeList.add(unalignedSpace);
				accumulateUnalignedSpace = space.getDNALength();
				if (unmapMole != null)
					accumulateUnalignedSpace -= unmapMole.getDNALength();
			}
			accumulateUnalignedSpace += mapMole.getDNALength();
			if (unmapMole != null)
				unalignedMoleculeList.add(unmapMole);
			

			moleculelist.add(mapMole);
						
			
			lastsubfragstop = result.subfragstop;
			lastmappedstop = result.mappedRegion.stop;
			lastResult = result;
			newresultlist.add(result);
			modifiedresultlist.add(new OptMapResultNode(result));
		}
		OptMapResultNode finalResult = newresultlist.get(newresultlist.size() - 1);
		int unmapstart = -1;
		int unmapstop = -1;
		if (finalResult.mappedstrand == 1)
		{
			unmapstart = finalResult.subfragstop + 1;
			unmapstop = finalResult.parentFrag.getTotalSegment() - 1;
		}
		else
		{
			unmapstart = 0;
			unmapstop = finalResult.subfragstop - 1;
		}

//		if (oriLastResult.subfragstop + 1 <= oriLastResult.getTotalSegment() - 1) // some mappers may align the last segment of a molecule
		if (finalResult.subfragstop + 1 <= finalResult.getTotalSegment() - 1)
		{
			VMolecule finalUnmapMole = new VMolecule(finalResult.parentFrag);
			finalUnmapMole.setStartEndPoint(finalResult.parentFrag.getGenomicPos(unmapstart, unmapstop, true).getLoc());
			finalUnmapMole.setReverse(finalResult.mappedstrand == -1);
			VSpace unalignedSpace = new VSpace(accumulateUnalignedSpace, accumulateUnalignedSpace);
			unalignedMoleculeList.add(unalignedSpace);
			unalignedMoleculeList.add(finalUnmapMole);
			
			// Temporarily added
			if (OMView.dataColorMap.containsKey(finalResult.parentFrag.name)) {
				for (int i = 0; i < finalResult.parentFrag.getTotalSegment(); i++)	
					if (OMView.dataColorMap.get(finalResult.parentFrag.name).contains(i))
						finalUnmapMole.addRegionColor(new SimpleLongLocation(i==0?1:finalResult.parentFrag.refp[i - 1], i==finalResult.parentFrag.getTotalSegment()-1?finalResult.parentFrag.size:finalResult.parentFrag.refp[i]), Color.BLUE);
			}
		}
		
//		moleculelist.add(finalUnmapMole);	
//		finalUnmapMole.setAlignedPortion(false);
		this.resultlist = newresultlist;		
//		this.modifiedresultlist = modifiedresultlist;

			
		this.moleculelist = moleculelist;
		this.unalignedMoleculeList = unalignedMoleculeList;
		for (VObject obj : moleculelist)
		{
			if (obj instanceof VSpace)
			{
				this.add(obj);
				obj.addMouseListener(this);
			}
		}
		for (VObject obj : moleculelist)
		{
			
			if (obj instanceof VMolecule)
			{
				obj.setToolTipText(this.getToolTipText());
				this.add(obj);
				obj.addMouseListener(this);
			}
		}
		
		for (VObject obj : unalignedMoleculeList)
		{
			obj.setToolTipText(this.getToolTipText());
			this.add(obj);
			obj.addMouseListener(this);
		}

		this.validate();
		this.autoSetSize();
	}
	
	public void setTranslocation(GenomicPosNode region1, GenomicPosNode region2)
	{
		// check if whole molecule inverted
		if (isInvertedMolecule)
		{
//			System.out.println(resultlist.get(0).id);
			GenomicPosNode tmp = region1;
			region1 = region2;
			region2 = tmp;
		}
		if (region1 != null)
		{
			VSpace trans1 = new VTranslocation(region1, -1);
			this.moleculelist.add(0, trans1);
			this.add(trans1);
		}
		if (region2 != null)
		{
			VSpace trans2 = new VTranslocation(region2, 1);
			this.moleculelist.add(trans2);
			this.add(trans2);
		}
		
		this.autoSetSize();
	}
	
	public boolean containSignal(String ref, int signalPos)
	{
		for (OptMapResultNode result : resultlist)
		{
			if (result.mappedRegion.ref.equalsIgnoreCase(ref))
				if (result.subrefstart - 1 <= signalPos && result.subrefstop >= signalPos)
					return true;
		}
		return false;
	}

	public boolean containPos(String ref, long pos)	
	{
		// even SV included is counted; *not the signalPos, but pure pos
		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (OptMapResultNode result : resultlist)
		{
			if (result.mappedRegion.start < min)
				min = result.mappedRegion.start;
			if (result.mappedRegion.stop > max)
				max = result.mappedRegion.stop;
		}
		return (pos >= min && pos <= max);
	}
	
	public boolean mapSignal(String refname, int targetSig)
	{
		for (OptMapResultNode result : resultlist)
		{
			if (result.mapSignal(refname, targetSig))
				return true;
		}
		return false;
	}
	public int getSignalDisplace(String refname, int targetSig)
	{

		if (!mapSignal(refname, targetSig))
			return Integer.MIN_VALUE;
		for (int i = 0; i < resultlist.size(); i++)
		{
			OptMapResultNode result = resultlist.get(i);
			if (result.mapSignal(refname, targetSig))
			{
				int displace = 0;
				int count = 0;
				for (VObject obj : moleculelist)
				{
					if (obj instanceof VMolecule)
					{
						if (count >= i)
						{
							break;							
						}
						count++;
						
					}
					displace += (obj).getDNALength();
				}
				displace += result.getSignalDisplace(refname, targetSig);
				
//				System.out.println(result.getSignalDisplace(refname, targetSig));
//				System.out.println(result.getMapScale());
				return displace;
			}
		}
		System.err.println("Warning: Unfound signal in result for signal displace. Continue.");
		return Integer.MIN_VALUE;
	}
	public boolean startWithTranslocation()
	{
		return (moleculelist.get(0) instanceof VSpace); 
	}
	
	public long getTranslocationLength()
	{
		if (startWithTranslocation())
			return moleculelist.get(0).getDNALength();
		else
			return 0;
	}
	public GenomicPosNode getMappedPos()
	{
		// Consider translocation flanking
		String ref = resultlist.get(0).mappedRegion.ref;
		long start = resultlist.get(0).mappedRegion.start;
		long stop = resultlist.get(resultlist.size() - 1).mappedRegion.stop;
		if (RegionalView.showUnmap)
		{
			start -= unalignedMoleculeList.get(0).getDNALength();
			stop += unalignedMoleculeList.get(unalignedMoleculeList.size() - 1).getDNALength();
		}
		else
		{
			if (moleculelist.get(0) instanceof VSpace)
				start -= moleculelist.get(0).getDNALength();
			if (moleculelist.get(moleculelist.size() - 1) instanceof VSpace)
				stop += moleculelist.get(moleculelist.size() - 1).getDNALength();
		}
		return new GenomicPosNode(ref, start, stop);
	}
	public GenomicPosNode getRelativePos()
	{
		OptMapResultNode firstResult = resultlist.get(0);
		OptMapResultNode lastResult = resultlist.get(0);
		String ref = firstResult.mappedRegion.ref;
		long startPos = firstResult.mappedRegion.start - firstResult.length(0, firstResult.subfragstart - 1);
//		System.out.println(firstResult.subfragstart);
//		System.out.println(firstResult.length(0, firstResult.subfragstart));
		long endPos = lastResult.mappedRegion.stop + lastResult.length(lastResult.subfragstop + 1, lastResult.getTotalSegment() - 1);
		return new GenomicPosNode(ref, startPos, endPos);
	}
	
	public int getDNAlength()
	{
		int total = 0;
		for (VObject obj : moleculelist)
			if (obj instanceof VMolecule)
				total += ((VMolecule) obj).getDNALength();
			else
				if (obj instanceof VSpace)
					total += ((VSpace) obj).getDNALength();
		return total;
	}
	public int getSpaceDNALength()
	{
		int total = 0;
		if (RegionalView.showUnmap)
		{
			for (VObject obj : unalignedMoleculeList)
				if (obj instanceof VMolecule)
					total += ((VMolecule) obj).getDNALength();
				else
					if (obj instanceof VSpace)
						total += ((VSpace) obj).getRefDNALength();
		}
		else
			for (VObject obj : moleculelist)
				if (obj instanceof VMolecule)
					total += ((VMolecule) obj).getDNALength();
				else
					if (obj instanceof VSpace)
						total += ((VSpace) obj).getRefDNALength();
		return total;
		
	}
	@Override
	public void autoSetSize()
	{
		int objx = 0;
		long cumulateDNALength = 0;
		int objy = ViewSetting.bodyHeight;
		for (VObject obj : moleculelist) {
			obj.autoSetSize();
//			objx += obj.getSize().width;
			cumulateDNALength += obj.getDNALength();
		}

		if (RegionalView.showUnmap) {
			objx = 0; // replacing the objx setting using moleculelist
			cumulateDNALength = 0;
			for (VObject obj : unalignedMoleculeList) {
				obj.autoSetSize();
				cumulateDNALength += obj.getDNALength();
			}
			objy += ViewSetting.bodyHeight;
		}
		objx = (int) (cumulateDNALength / dnaRatio * ratio);
		this.setSize(objx, (int) (objy * ratio));
		this.reorganize();
	}
	@Override
	public void reorganize()
	{
		int objx = 0;
		int objy = 0;
		long cumulateDNALength = 0;
		if (RegionalView.showUnmap)
		{
//			System.out.println("Unaligned");
			for (VObject obj : unalignedMoleculeList)
			{
				obj.setVisible(true);
				obj.setLocation(objx, objy);
				cumulateDNALength += obj.getDNALength();
				objx = (int) (cumulateDNALength / dnaRatio * ratio);
			}
			
			cumulateDNALength = unalignedMoleculeList.get(0).getDNALength();
			if (moleculelist.get(0) instanceof VSpace) 
				cumulateDNALength -= moleculelist.get(0).getDNALength();
			objx = (int) (cumulateDNALength / dnaRatio * ratio);			
			objy = (int) (ViewSetting.bodyHeight * ratio);
		}
		else {
			for (VObject obj : unalignedMoleculeList)
				obj.setVisible(false);
		}
		
		
		for (VObject obj : moleculelist) {							
			if (obj instanceof VSpace && obj.getWidth() > obj.getDNALength() / dnaRatio * ratio) {
				obj.setLocation((int) (objx + (obj.getDNALength() / dnaRatio * ratio - obj.getWidth()) / 2), objy);
			}
			else
				obj.setLocation(objx, objy);
			cumulateDNALength += obj.getDNALength();
			objx = (int) (cumulateDNALength / dnaRatio * ratio);
		}
		
		
	}
	@Override
	public void setDNARatio(double dnaRatio)
	{
		for (VObject obj : moleculelist)
			obj.setDNARatio(dnaRatio);
		for (VObject obj : unalignedMoleculeList)
			obj.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}
	@Override 
	public void setRatio(double ratio)
	{
		for (VObject obj : moleculelist)
			obj.setRatio(ratio);
		for (VObject obj : unalignedMoleculeList)
			obj.setRatio(ratio);
		super.setRatio(ratio);
	}
	
	
	public VAlignment createAlignment(LinkedHashMap<String, DataNode> optrefmap) {
		return new VAlignment(optrefmap.get(resultlist.get(0).mappedRegion.ref), resultlist);
	}
	


	@Override
	public int compareTo(VMapMolecule o) {
		if (this.getMappedPos().compareTo(o.getMappedPos()) != 0)
			return this.getMappedPos().compareTo(o.getMappedPos());
		else
			return this.getRelativePos().compareTo(o.getRelativePos());
	}

//	@Override
//	public void onClick() {
//		String message = String.format("Molecule ID: %s\nPartialMaps: %d", resultlist.get(0).id, resultlist.size());
////		JOptionPane.showMessageDialog(this.getParent().getParent().getParent().getParent(), message);
//		JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(this), message);
//		displayMoleculeDialog(resultlist.get(0));
//	}

//	public void displayMoleculeDialog(FragmentNode fragment)
//	{
//		((RegionalView) this.getParent()).showAlignment(resultlist.get(0).id);
//		MView mview = new MView();
////		LinkedHashMap<String, FragmentNode> fragmentmap = new LinkedHashMap<String, FragmentNode>();
////		System.out.println("Hi");
//		mview.showMolecule(fragment);
//		System.out.println(mview.getSize());
//		JDialog dialog = new JDialog(SwingUtilities.windowForComponent(this), "Molecule " + fragment.id, Dialog.ModalityType.APPLICATION_MODAL);
//		dialog.setContentPane(mview);
//		dialog.setSize(mview.getSize());
//		
////		dialog.setPreferredSize(mview.getSize());
//		dialog.pack();
//		
//		dialog.setVisible(true);
//		
//	}

//	public static Comparator<VMapMolecule> mappedstartcomparator = new Comparator<VMapMolecule>()
//	{
//		@Override
//		public int compare(VMapMolecule vmm1, VMapMolecule vmm2) {
//			return ;
//		}
//	};
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getSource() instanceof VMolecule) {
//			if (e.isPopupTrigger()) {
//				e.getX();
//				((VMolecule) e.getSource()).
//			}
			this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX() + ((VObject) e.getSource()).getLocation().x, e.getY() + ((VObject) e.getSource()).getLocation().y, e.getClickCount(), false));
		}
		if (e.getSource() instanceof VSpace)
			this.dispatchEvent(new MouseEvent(this, e.getID(), e.getWhen(), e.getModifiers(), e.getX() + ((VObject) e.getSource()).getLocation().x, e.getY() + ((VObject) e.getSource()).getLocation().y, e.getClickCount(), false));
//		if (e.getSource() instanceof VSignal)
//			this.getParent().dispatchEvent(e);
	}
	public String getID()
	{
		return id;
	}
	
	
	@Override
	public long getDNALength()
	{
		// unfinish
		return 0;
	}

}

