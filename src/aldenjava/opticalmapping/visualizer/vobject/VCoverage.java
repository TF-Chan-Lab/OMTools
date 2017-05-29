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


/*
 * package aldenjava.opticalmapping.visualizer.vobject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import aldenjava.common.ExtendedLongLocation;
import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VCoverage extends VObject {
	
	private SimpleLongLocation startEndPoint;
	private List<ExtendedLongLocation> locList;
	
	public VCoverage(List<ExtendedLongLocation> locList) {
		this.locList = locList;
	}
	
	public void setStartEndPoint(SimpleLongLocation startEndPoint) {
		this.startEndPoint = startEndPoint;
		this.autoSetSize();
	}
	
	@Override
	public long getDNALength() {
		return startEndPoint.length();
	}

	@Override
	public void autoSetSize() {
		this.setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.coverageHeight * ratio));
	}


	@Override
	public void reorganize() {
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setPaint(ViewSetting.coverageBGColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setPaint(ViewSetting.coverageColor);
		int max = 0;
		for (ExtendedLongLocation loc : locList)
			if (max < loc.indicator)
				max = loc.indicator;
		System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("MAX: " + max);
		for (ExtendedLongLocation loc : locList)
			System.out.println(loc.min + "\t" + loc.max + "\t" + loc.indicator);
//		
		for (ExtendedLongLocation loc : locList) {
			g.fillRect((int) ((loc.min - startEndPoint.min) / dnaRatio * ratio), (int) ((max - loc.indicator) / (double) max * ViewSetting.coverageHeight * ratio), (int) (loc.length() / dnaRatio * ratio), (int) (loc.indicator / (double) max * ViewSetting.coverageHeight * ratio));
//			System.out.println((int) ((loc.min - startEndPoint.min) / dnaRatio * ratio) + "\t" + (int) ((max - loc.indicator) / (double) max * ViewSetting.coverageHeight * ratio) + "\t" + (int) (loc.length() / dnaRatio * ratio) + "\t" + (int) (loc.indicator / (double) max * ViewSetting.coverageHeight * ratio));
		}
		
		
	}


}

*/
package aldenjava.opticalmapping.visualizer.vobject;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import aldenjava.common.SimpleLongLocation;
import aldenjava.common.WeightedRange;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VCoverage extends VObject {
	
	private SimpleLongLocation startEndPoint;
	private List<WeightedRange<Long, Integer>> coverageRanges;
	
	public VCoverage() {
		this(new ArrayList<>());
	}
	public VCoverage(List<WeightedRange<Long, Integer>> coverageRanges) {
		this.coverageRanges = coverageRanges;
	}

	public void setCoverageRanges(List<WeightedRange<Long, Integer>> coverageRanges) {
		this.coverageRanges = coverageRanges;
	}
	public void setStartEndPoint(SimpleLongLocation startEndPoint) {
		this.startEndPoint = startEndPoint;
		this.autoSetSize();
	}
	
	@Override
	public long getDNALength() {
		return startEndPoint.length();
	}

	@Override
	public void autoSetSize() {
		this.setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.coverageHeight * ratio));
	}


	@Override
	public void reorganize() {
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setPaint(ViewSetting.coverageBGColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setPaint(ViewSetting.coverageColor);
		int max = 0;
		for (WeightedRange<Long, Integer> range : coverageRanges)
			if (max < range.weight)
				max = range.weight;
		max = 200;
		for (WeightedRange<Long, Integer> range : coverageRanges) {
			Rectangle2D rect = new Rectangle2D.Double((range.min - startEndPoint.min) / dnaRatio * ratio, (max - range.weight >= 0 ? (max - range.weight) : 0) / (double) max * ViewSetting.coverageHeight * ratio, range.length() / dnaRatio * ratio, range.weight / (double) max * ViewSetting.coverageHeight * ratio);
			g.fill(rect);
//			g.fillRect((int) ((range.min - startEndPoint.min) / dnaRatio * ratio), (int) ((max - range.weight) / (double) max * ViewSetting.coverageHeight * ratio), (int) (range.length() / dnaRatio * ratio), (int) (range.weight / (double) max * ViewSetting.coverageHeight * ratio));
//			System.out.println((int) ((range.min - startEndPoint.min) / dnaRatio * ratio) + "\t" + (int) ((max - range.indicator) / (double) max * ViewSetting.coverageHeight * ratio) + "\t" + (int) (range.length() / dnaRatio * ratio) + "\t" + (int) (range.indicator / (double) max * ViewSetting.coverageHeight * ratio));
		}
		
		
	}


}
