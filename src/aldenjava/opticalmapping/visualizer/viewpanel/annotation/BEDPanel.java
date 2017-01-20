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


package aldenjava.opticalmapping.visualizer.viewpanel.annotation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.annotation.BEDNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.vobject.VObject;

public class BEDPanel extends AnnotationPanel {


	private List<BEDNode> bedList = new ArrayList<BEDNode>();
	private List<VBED> vbedList = new ArrayList<VBED>();
	private int lineNo = 0;
	public BEDPanel(OMView mainView) {
		super(mainView);
//		objBorder = new Point(50, 10);
//		
//		setMaximumSize(new Dimension(10000, 50));
//		setSize(new Dimension(10000, 50));
//		setPreferredSize(new Dimension(10000, 50));
//		setMaximumSize(new Dimension(10000, 50));
		

	}
	public void loadAnnotation(List<? extends AnnotationNode> annoList)
	{
		this.bedList.addAll((List<BEDNode>) annoList);
		this.setRegion(region);
	}
	
	public void setRegion(GenomicPosNode region)
	{		
		super.setRegion(region);
		if (region == null)
			return;
		
		if (bedList == null)
			return;
		
		this.removeAll();
		vbedList.clear();
		for (BEDNode bed : bedList)
			if (bed.region.overlapSize(region) >= 1)
				vbedList.add(new VBED(bed));
		Collections.sort(vbedList);
		for (VBED vbed : vbedList)
			this.add(vbed);
		
		this.setDNARatio(dnaRatio);
		this.setRatio(ratio);
	}
	@Override 
	public void setDNARatio(double dnaRatio)
	{
		super.setDNARatio(dnaRatio);
		for (VBED vbed : vbedList)
			vbed.setDNARatio(dnaRatio);
	}
	@Override 
	public void setRatio(double ratio)
	{
		super.setRatio(ratio);
		for (VBED vbed : vbedList)
			vbed.setRatio(ratio);
	}
	@Override
	public void autoSetSize()
	{
		if (region == null)
			setSize(0, 0);
		else
			setSize((int) (region.length() / dnaRatio * ratio) + objBorder.x * 2, (int) (lineNo * 20 * ratio) + objBorder.y * 2);
//		if (this.getParent() != null)
//			this.getParent().setMaximumSize(getSize());
		setPreferredSize(getSize());
//		setMaximumSize(getSize());

	}
	@Override
	public void reorganize() {
//		int x = objBorder.x;
//		int y = objBorder.y;
//		for (VBED vbed : vbedList)
//		{
//			vbed.setLocation((int) ((vbed.bed.region.start - region.start) / dnaRatio * ratio) + x, y);
//			y += 20 * ratio;
//		}
//			
		List<Long> lineEndPos = new ArrayList<Long>();
		int height = (int) (20 * ratio);
		for (VBED vbed : vbedList)
		{
			long displacement = vbed.bed.region.start - region.start;
			
			int x = (int) (displacement / dnaRatio * ratio) + objBorder.x;
			int y = -1;
			for (int i = 0; i < lineEndPos.size(); i++)
				if (displacement - lineEndPos.get(i) >= 1500)
				{
					y = (i + 1) * height;
					lineEndPos.set(i, displacement + vbed.bed.region.length());
					break;
				}
			if (y == -1)
			{
				lineEndPos.add(displacement + vbed.bed.region.length());
//					lineEndPos.add(x + vmm.getSize().width);
				y = (lineEndPos.size() - 1 + 1) * height;
			}
			
			y += objBorder.y;
			vbed.setLocation(x, y);
			vbed.setVisible(true);
		}
		lineNo = lineEndPos.size();

	}
	
}

class VBED extends VObject implements Comparable<VBED>
{
//	private Font font = new Font("Arial", Font.PLAIN, 12);
	public BEDNode bed;
//	public int textLength = 0;
	public VBED(BEDNode bed)
	{
		if (bed == null)
			throw new NullPointerException("VBED must be initiated with proper BEDNode.");
		this.bed = bed;
		if (bed.name != null)
			this.setToolTipText(bed.name);
	}

	@Override
	public long getDNALength() 
	{
		return bed.region.length();
	}

	@Override
	public void autoSetSize() 
	{
		
		setSize((int) (bed.region.length() / dnaRatio * ratio), (int) (10 * ratio));
		if (this.getWidth() == 0)
			this.setSize(1, this.getHeight());
	}

	@Override
	public void reorganize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics;
		g.setFont(new Font("Arial", Font.PLAIN, 12));
//		System.out.println(bed.itemRgb);
		if (bed.itemRgb != null)
			g.setPaint(bed.itemRgb);
		else
			g.setPaint(Color.black);
		if (bed.blockCount != null)
		{
			g.fill(new Rectangle2D.Double(0, this.getHeight() / 4, this.getWidth(), this.getHeight() / 2));
			for (int i = 0; i < bed.blockCount; i++)
				g.fill(new Rectangle2D.Double((bed.blockStarts.get(i) - bed.region.start) / dnaRatio, 0, bed.blockSizes.get(i), this.getHeight()));
		}
		else
			g.fill(new Rectangle2D.Double(0, 0, this.getWidth(), this.getHeight()));
	}

	public int compareTo(VBED vbed) {
		return this.bed.region.compareTo(vbed.bed.region);
	}

}