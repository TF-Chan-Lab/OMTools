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


package aldenjava.opticalmapping.visualizer.viewpanel;

import javax.swing.JOptionPane;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.ViewSetting;
import aldenjava.opticalmapping.visualizer.viewpanel.annotation.AnnotationPanel;

public class AnchorControlPanel extends ControlPanel {

	public AnchorControlPanel(OMView mainView) {
		super(mainView);
	}

	public AnchorControlPanel(OMView mainView, AnchorView... aViews) {
		super(mainView);
		createViewPanel(aViews);
		reorganizePanel();
	}

	@Override
	public void setAnchorPoint(GenomicPosNode anchorPoint) {
		DataNode ref = mainView.dataModule.getAllReference().get(anchorPoint.ref);
		if (ref == null) {
			JOptionPane.showMessageDialog(mainView, "Reference " + anchorPoint.ref + " cannot be found.");
			return;
		}
		int targetSig1 = ref.findExactRefpIndex(anchorPoint.start);
		int targetSig2 = ref.findExactRefpIndex(anchorPoint.stop);
		if (targetSig1 == -1 || targetSig2 == -1)
			throw new IllegalArgumentException("Invalid anchor point position. Anchor point " + anchorPoint + " cannot be found.");
		updateTitle("Anchor:" + anchorPoint.toString());
		super.setAnchorPoint(anchorPoint);
		GenomicPosNode region = new GenomicPosNode(anchorPoint.ref, anchorPoint.start - ViewSetting.anchorFlankSize, anchorPoint.stop + ViewSetting.anchorFlankSize);
		setRegion(region);
	}
	
	@Override
	public void setRegion(GenomicPosNode region) {
		if (!mainView.dataModule.getAllReference().containsKey(region.ref)) {
			JOptionPane.showMessageDialog(mainView, "Reference not found.");
			return;
		}
		if (anchorPoint != null && !region.contains(anchorPoint))
			VerbosePrinter.println("Selected region " + region.toString() + " does not include the anchor point " + anchorPoint.toString() + ".");
		super.setRegion(region);
	}

	
	
	@Override
	public void initializeTitle() {
		this.title = "Anchor view";
	};
	
	@Override
	public void createViewPanel() {
		this.createViewPanel(new AnchorView(mainView));
	}

	@Override
	public void createAnnotationPanel() {
		createAnnotationPanel(new AnnotationPanel(mainView));
	}
}
