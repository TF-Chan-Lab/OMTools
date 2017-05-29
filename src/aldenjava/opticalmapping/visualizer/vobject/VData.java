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


package aldenjava.opticalmapping.visualizer.vobject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import aldenjava.common.SimpleLongLocation;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public class VData extends VObject {

	protected final DataNode data;
	protected SimpleLongLocation startEndPoint;
	private boolean reverse;
	private double scale;
	private List<LocColorNode> locColorList;
	private List<SignalColorNode> signalColorList;
	private Color baseColor;
	private Color baseSignalColor;
	private boolean selected = false;
	
	public VData(DataNode data, Color baseColor, Color baseSignalColor) {
		if (data == null)
			throw new NullPointerException("Null: data");
		this.data = data;
		this.baseColor = baseColor;
		this.baseSignalColor = baseSignalColor;
		this.setToolTipText(data.name);
		startEndPoint = new SimpleLongLocation(1, data.length());
		reverse = false;
		scale = 1;
		reset();
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
	public boolean getReverse() {
		return reverse;
	}
	public void setScale(double scale) {
		if (scale < 0) 
			throw new IllegalArgumentException("Negative scaling in VData.");
		this.scale = scale;
	}
	public double getScale() {
		return scale;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
		this.repaint();
	}
	public void setBaseColor(Color baseColor) {
		if (baseColor == null)
			throw new NullPointerException("Null: baseColor");
		this.baseColor = baseColor;
	}

	public void resetRegionColor() {
		this.locColorList = new ArrayList<>();
	}

	public void resetSignalColor() {
		this.signalColorList = new ArrayList<>();
	}

	public void reset() {
		resetRegionColor();
		resetSignalColor();
	}

	public void addRegionColor(SimpleLongLocation loc, Color color) {
		if (loc == null)
			throw new NullPointerException("Null: loc");
		if (color == null)
			throw new NullPointerException("Null: color");
		this.locColorList.add(new LocColorNode(loc, color));
	}

	public void addRegionColor(List<SimpleLongLocation> locList, Color color) {
		if (locList == null)
			throw new NullPointerException("Null: locList");
		if (color == null)
			throw new NullPointerException("Null: color");
		for (SimpleLongLocation loc : locList)
			addRegionColor(loc, color);
	}

	public void addRegionColor(List<SimpleLongLocation> locList, List<Color> colorList) {
		if (locList == null)
			throw new NullPointerException("Null: locList");
		if (colorList == null)
			throw new NullPointerException("Null: colorList");
		if (locList.size() != colorList.size())
			throw new IllegalArgumentException("Size of locList does not match size of colorList.");
		for (int i = 0; i < locList.size(); i++)
			addRegionColor(locList.get(i), colorList.get(i));
	}

	public void addSignalColor(int signal, Color color) {
		if (color == null)
			throw new NullPointerException("Null: color");
		if (signal < 0 || signal >= data.refp.length)
			throw new IllegalArgumentException("Signal is out of bound");
		this.signalColorList.add(new SignalColorNode(signal, color));
	}

	public void addSignalColor(int[] signals, Color color) {
		if (signals == null)
			throw new NullPointerException("Null: signals");
		if (color == null)
			throw new NullPointerException("Null: color");
		for (int signal : signals)
			addSignalColor(signal, color);
	}

	public int findClosestSignal(int x) {
		long correctedX;
		if (reverse)
			correctedX = (long) (getDNALength() - x / scale * dnaRatio / ratio) + startEndPoint.min;
		else
			correctedX = (long) (x / scale * dnaRatio / ratio) + startEndPoint.min;
		int signal = -1;
		if (data.findExactRefpIndex(correctedX) != -1)
			signal = data.findExactRefpIndex(correctedX);
		else {
			int index = data.findRefpIndex(correctedX);
			long min = ViewSetting.closeSignal;
			if (index - 1 > 0)
				if (correctedX - data.refp[index - 1] <= min) {
					min = correctedX - data.refp[index - 1];
					signal = index - 1;
				}
			if (index < data.refp.length)
				if (data.refp[index] - correctedX <= min) {
					min = correctedX - data.refp[index];
					signal = index;
				}
		}
		return signal;
	}

	@Override
	public String getName() {
		return data.name;
	}

//	public GenomicPosNode getGenomicPos(int x) {
////		if (x < 0)
////		if (this.getSize().width )
//		return null;
//	}
	
	public void setStartEndPoint(SimpleLongLocation startEndPoint) {
		if (startEndPoint == null)
			throw new NullPointerException("Null: startEndPoint");
		if (startEndPoint.min < 1 || startEndPoint.max > this.data.length())
			throw new IndexOutOfBoundsException("startEndPoint exceeds data boundary. Name: " + data.name + " Size: " + data.size + " Start point: " + startEndPoint.min + " End point: " + startEndPoint.max);
		this.startEndPoint = startEndPoint;
	}

//	public long getDisplace(int targetSig) {
//		return data.refp[targetSig] - this.startEndPoint.min + 1;
//	}

	@Override
	public void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;

		// Draw base body
		
		if (!selected)
			g.setPaint(baseColor);
		else
			g.setPaint(baseColor.brighter());
		double width = getDNALength() / dnaRatio * ratio;
		g.fill(new Rectangle2D.Double(0, 0, width, ViewSetting.bodyHeight * ratio));

		// Draw regional body
		for (LocColorNode lc : locColorList) {
			SimpleLongLocation loc = lc.loc;
			Color color = lc.color;
			g.setPaint(color);
			
			double start;
			if (reverse)
				start = (startEndPoint.max - loc.max + 1) * scale / dnaRatio * ratio;
			else
				start = (loc.min - startEndPoint.min + 1) * scale / dnaRatio * ratio;
			double len = loc.length() * scale / dnaRatio * ratio;
			g.fill(new Rectangle2D.Double(start, 0, len, this.getHeight()));
		}

		// Draw signals
		Collections.sort(signalColorList);
		int index = 0;
		int firstSignal = data.findRefpIndex(startEndPoint.min);
		int lastSignal = data.findExactRefpIndex(startEndPoint.max);
		if (lastSignal == -1)
			lastSignal = data.findRefpIndex(startEndPoint.max) - 1;

		for (int sig = firstSignal; sig <= lastSignal; sig++) {
			while (index < signalColorList.size() && signalColorList.get(index).signal < sig)
				index++;
			Color color = baseSignalColor;
			while (index < signalColorList.size() && signalColorList.get(index).signal == sig)
				color = signalColorList.get(index++).color; // the last color is used

			g.setPaint(color);

			double sigPos = (data.refp[sig] - startEndPoint.min + 1) * scale / dnaRatio * ratio;
			if (reverse) {
				sigPos = width - sigPos + scale / dnaRatio * ratio;
			}
			g.setStroke(new BasicStroke((float) (ViewSetting.signalStrokeWidth * ratio)));
			g.drawLine((int) sigPos, 0, (int) sigPos, this.getHeight());

		}
	}

	@Override
	public long getDNALength() {
		return (long) (startEndPoint.length() * scale);
	}

	@Override
	public void autoSetSize() {
		setSize((int) (getDNALength() / dnaRatio * ratio), (int) (ViewSetting.bodyHeight * ratio));
	}

	@Override
	public void reorganize() {
	}

}

class LocColorNode {
	final SimpleLongLocation loc;
	final Color color;

	public LocColorNode(SimpleLongLocation loc, Color color) {
		this.loc = loc;
		this.color = color;
	}

}

class SignalColorNode implements Comparable<SignalColorNode> {
	final int signal;
	final Color color;

	public SignalColorNode(int signal, Color color) {
		this.signal = signal;
		this.color = color;
	}

	@Override
	public int compareTo(SignalColorNode sc) {
		return Integer.compare(this.signal, sc.signal);
	}

}