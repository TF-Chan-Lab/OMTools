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

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VDataType;
import aldenjava.opticalmapping.visualizer.viewpanel.ViewPanel;
import aldenjava.opticalmapping.visualizer.vobject.VAnno;

public class AnnotationPanel extends ViewPanel {

	protected GenomicPosNode region;
	protected Point objBorder = new Point(20, 20);
	private int lineNo = 0;
	List<? extends AnnotationNode> annoList;
	List<VAnno> vAnnoList = new ArrayList<VAnno>();
	public AnnotationPanel(OMView mainView)
	{
		super(mainView);
		this.addMouseListener(this);
	}
	
	@Override
	protected void initializeDataSelection() {
		dataSelection = new LinkedHashMap<>();
		dataSelection.put(VDataType.ANNOTATION, new ArrayList<String>());
	}

	public void loadAnnotation(List<? extends AnnotationNode> annoList)
	{
		this.annoList = annoList;
		this.setRegion(region);
	}

	@Override
	public void setRegion(GenomicPosNode region)
	{
		this.region = region;
		if (region == null) {
			autoSetSize();
			return;
		}		
		if (annoList == null) {
			autoSetSize();
			return;
		}
		
		this.removeAll();
		vAnnoList.clear();
		for (AnnotationNode anno : annoList)
			if (anno.region.overlapSize(region) >= 1)
				vAnnoList.add(new VAnno(anno));
		Collections.sort(vAnnoList);
		for (VAnno vanno : vAnnoList)
			this.add(vanno);
		
		this.setDNARatio(dnaRatio);
		this.setRatio(ratio);

		autoSetSize();
		reorganize();
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		this.getParent().dispatchEvent(e);
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		this.getParent().dispatchEvent(e);
	}
	@Override
	public void mouseExited(MouseEvent e) {
		this.getParent().dispatchEvent(e);
	}
	@Override
	public void mousePressed(MouseEvent e) {
		this.getParent().dispatchEvent(e);
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		this.getParent().dispatchEvent(e);
	}
	@Override 
	public void setDNARatio(double dnaRatio)
	{
		for (VAnno vAnno : vAnnoList)
			vAnno.setDNARatio(dnaRatio);
		super.setDNARatio(dnaRatio);
	}
	@Override 
	public void setRatio(double ratio)
	{
		for (VAnno vAnno : vAnnoList)
			vAnno.setRatio(ratio);
		super.setRatio(ratio);

	}

	@Override
	public void autoSetSize() {				
		if (region == null)
			this.setSize(OMView.blankPanelSize);
		else	
			setSize((int) (region.length() / dnaRatio * ratio) + objBorder.x * 2, (int) (lineNo * 20 * ratio) + objBorder.y * 2);
		for (VAnno vanno : vAnnoList)
			vanno.autoSetSize();
		setPreferredSize(getSize());
	}
	@Override
	public void reorganize() {
		List<Long> lineEndPos = new ArrayList<>();
		int height = (int) (25 * ratio);
		for (VAnno vanno : vAnnoList)
		{
			vanno.reorganize();
			long displacement = vanno.anno.region.start - region.start;
			
			int x = (int) (displacement / dnaRatio * ratio) + objBorder.x;
			int y = -1;
			for (int i = 0; i < lineEndPos.size(); i++)
				if (displacement > lineEndPos.get(i))
				{
					y = (i) * height;
					lineEndPos.set(i, displacement + vanno.getDNALength());
					break;
				}
			if (y == -1)
			{
				lineEndPos.add(displacement + vanno.getDNALength());
				y = (lineEndPos.size() - 1) * height;
			}
			
			y += objBorder.y;
			vanno.setLocation(x, y);
			vanno.setVisible(true);
		}

		lineNo = lineEndPos.size();

	}
	@Override
	public void setViewMolecule(String id) throws UnsupportedOperationException {
	}
	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) throws UnsupportedOperationException {
	}
	
	@Override
	public void updateData() {
		super.updateData();
		List<? extends AnnotationNode> annoList = mainView.dataModule.getAnno(dataSelection.get(VDataType.ANNOTATION));
		this.loadAnnotation(annoList);
	}

}