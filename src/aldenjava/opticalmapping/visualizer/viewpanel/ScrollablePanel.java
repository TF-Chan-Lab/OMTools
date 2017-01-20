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


package aldenjava.opticalmapping.visualizer.viewpanel;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.visualizer.OMView;
import aldenjava.opticalmapping.visualizer.VComponent;
import aldenjava.opticalmapping.visualizer.ViewSetting;

public abstract class ScrollablePanel extends VComponent implements Scrollable{
	protected OMView mainView;
	protected GenomicPosNode region;
	protected String title = "View";
	protected String information;
	protected Point objBorder = new Point(20, 20);
	protected JPopupMenu menu = new JPopupMenu();
	
	public ScrollablePanel(OMView mainView)	{
		super();
		this.mainView = mainView;
		initMouseInput();
		updateMenu();
	}
	
	private void navigateViewPort(Point p) {
        JViewport jv = (JViewport) (this.getParent());
        int newX = p.x;
        int newY = p.y;

        int maxX = ScrollablePanel.this.getWidth() - jv.getWidth();
        int maxY = ScrollablePanel.this.getHeight() - jv.getHeight();
        if (maxX < 0)
      	  maxX = ScrollablePanel.this.getWidth();
        if (maxY < 0)
      	  maxY = ScrollablePanel.this.getHeight();
        if (newX < 0)
          newX = 0;
        if (newX > maxX)
          newX = maxX;
        if (newY < 0)
          newY = 0;
        if (newY > maxY)
          newY = maxY;
        Point oldPoint = jv.getViewPosition();
        jv.setViewPosition(new Point(newX, newY));		          
        this.firePropertyChange("ViewPosition", oldPoint, jv.getViewPosition());

	}
	// Listener for dragging and zooming
	private void initMouseInput() {
	   MouseInputAdapter mia = new MouseInputAdapter() {
		      int xDiff, yDiff;

		      Container c;
		      @Override
		      public void mouseClicked(MouseEvent e){
//		    	  updateInfo(information);
		      }
		      @Override
			public void mouseDragged(MouseEvent e) {
		        c = ScrollablePanel.this.getParent();
		        if (c instanceof JViewport) {
				  JViewport jv = (JViewport) c;
				  Point p = jv.getViewPosition();
				  int newX = p.x - (e.getX() - xDiff);
				  int newY = p.y - (e.getY() - yDiff);
				
				  int maxX = ScrollablePanel.this.getWidth()
				      - jv.getWidth();
				  int maxY = ScrollablePanel.this.getHeight() - jv.getHeight();
				  if (maxX < 0)
					  maxX = ScrollablePanel.this.getWidth();
				  if (maxY < 0)
					  maxY = ScrollablePanel.this.getHeight();
				  if (newX < 0)
				    newX = 0;
				  if (newX > maxX)
				    newX = maxX;
				  if (newY < 0)
				    newY = 0;
				  if (newY > maxY)
				    newY = maxY;
				  Point oldPoint = jv.getViewPosition();
				  
				  jv.setViewPosition(new Point(newX, newY));		          
				  ScrollablePanel.this.firePropertyChange("ViewPosition", oldPoint, jv.getViewPosition());
		        }
		      }
		      @Override
		      public void mouseWheelMoved(MouseWheelEvent event)
		      {
	    		  int rotations = event.getWheelRotation(); 
	    		  JViewport jv = (JViewport) ScrollablePanel.this.getParent();
	    		  Point p = jv.getViewPosition();
	    		  int mx = event.getX();
	    		  int my = event.getY();
	    		  int xdisplace = mx - p.x;
	    		  int ydisplace = my - p.y;
	    		  double oldRatio = ScrollablePanel.this.ratio;
	    		  double newRatio = ScrollablePanel.this.ratio - rotations * ViewSetting.zoomPerRotation;
	    		  newRatio = limitRatio(newRatio);
	    		  int newmx = (int) (mx * newRatio / oldRatio);
	    		  int newmy = (int) (my * newRatio / oldRatio);
	    		  ScrollablePanel.this.firePropertyChange("ViewRatio", oldRatio, newRatio);
	    		  ScrollablePanel.this.navigateViewPort(new Point(newmx - xdisplace, newmy - ydisplace));	    		  
		      }
		      @Override
			public void mousePressed(MouseEvent e) {
		    	  
		        xDiff = e.getX();
		        yDiff = e.getY();
		    	  if (e.isPopupTrigger())
		    		  menu.show(ScrollablePanel.this, e.getXOnScreen() - ScrollablePanel.this.getLocationOnScreen().x, e.getYOnScreen() - ScrollablePanel.this.getLocationOnScreen().y);
		      }
		      @Override
			public void mouseReleased(MouseEvent e) {
		    	  
		    	  if (e.isPopupTrigger())
		    		  menu.show(ScrollablePanel.this, e.getXOnScreen() - ScrollablePanel.this.getLocationOnScreen().x, e.getYOnScreen() - ScrollablePanel.this.getLocationOnScreen().y);
		      }
		      
		      
		    };
		addMouseMotionListener(mia);
	    addMouseListener(mia);
	    addMouseWheelListener(mia);
	}
	
	
	// Update Title, Info, Menu
	
	protected void updateMenu() {
		menu.removeAll();
		JMenuItem setZoomItem = new JMenuItem("Zoom");
		setZoomItem.setMnemonic('Z');
		setZoomItem.addActionListener(new ActionListener()
  		{
  			@Override
  			public void actionPerformed(ActionEvent event) 
  			{
				String ans = JOptionPane.showInputDialog(mainView, "Please input the zoom level:", ScrollablePanel.this.ratio);
				if (ans != null)
					try {
						JViewport jv = (JViewport) ScrollablePanel.this.getParent();
						Point oldPoint = jv.getViewPosition();
						double oldRatio = ScrollablePanel.this.ratio;
						double newRatio = Double.parseDouble(ans);
						newRatio = limitRatio(newRatio);
						ScrollablePanel.this.setRatio(newRatio);
			    		ScrollablePanel.this.firePropertyChange("ViewRatio", oldRatio, newRatio);
		    			ScrollablePanel.this.firePropertyChange("ViewPosition", oldPoint, jv.getViewPosition());

					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
  			}
  		});
  		menu.add(setZoomItem);

  		JMenuItem saveItem = new JMenuItem("Save to image");
  		saveItem.setMnemonic('S');
  		saveItem.addActionListener(new ActionListener()
  		{
  			@Override
  			public void actionPerformed(ActionEvent e) 
  			{
  				mainView.saveImage(ScrollablePanel.this);
  			}
  		});
  		menu.add(saveItem);
  		JMenuItem closeItem = new JMenuItem("Close");
  		closeItem.setMnemonic('C');
  		closeItem.addActionListener(new ActionListener()
  		{
  			@Override
  			public void actionPerformed(ActionEvent e) 
  			{
  				ScrollablePanel.this.firePropertyChange("PanelClose", false, true);
  			}	
  		});
  		menu.add(closeItem);
	}
//	protected void updateTitle(String newTitle) {
//		if (newTitle != null) {
//			this.firePropertyChange("PanelTitle", title, newTitle);
//			title = newTitle;
//		}
//	}

	// Scrollable
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		
		return new Dimension(1, 1);
	}
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) 
	{
		int maxUnitIncrement = 1;
        //Get the current position.
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL) {
            currentPosition = visibleRect.x;
        } else {
            currentPosition = visibleRect.y;
        }
        //Return the number of pixels between currentPosition
        //and the nearest tick mark in the indicated direction.
        if (direction < 0) {
            int newPosition = currentPosition -
                             (currentPosition / maxUnitIncrement)
                              * maxUnitIncrement;
            return (newPosition == 0) ? maxUnitIncrement : newPosition;
        } else {
            return ((currentPosition / maxUnitIncrement) + 1)
                   * maxUnitIncrement
                   - currentPosition;
        }

	}
	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)  {
		int maxUnitIncrement = 1;
        if (orientation == SwingConstants.HORIZONTAL) {
            return visibleRect.width - maxUnitIncrement;
        } else {
            return visibleRect.height - maxUnitIncrement;
        }
	}
	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}


}
