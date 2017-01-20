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


package aldenjava.opticalmapping.mapper.postmappingmodule;

import java.util.List;

import aldenjava.opticalmapping.Cigar;

public class PostJoinPathEdge
{
	public static int falseppenalty = 2;
	public static int falsenpenalty = 2;
	public final int deletion;
	public final int insertion;
	public final PostJoinPathNode nextNode;
	public final String localprecigar;
	public PostJoinPathEdge(String localprecigar, PostJoinPathNode nextNode)
	{
		this.insertion = Cigar.getCertainNumberFromPrecigar(localprecigar, 'I');
		this.deletion = Cigar.getCertainNumberFromPrecigar(localprecigar, 'D');
		this.localprecigar = localprecigar;
		this.nextNode = nextNode;
	}
	public boolean equals(PostJoinPathEdge edge)
	{
		return (this.deletion == edge.deletion && this.insertion == edge.insertion && this.nextNode == edge.nextNode); //this.nextNode == edge.nextNode is correct, because we are checking for index and no overlap Node can be found
	}
	public List<Integer> process(int pass)
	{
//		System.out.println("PathEdge process...");
		return nextNode.process(pass);
	}
	
	public double getScore()
	{
		return nextNode.bestscore - insertion * falseppenalty - deletion * falsenpenalty;
	}
	
	
	public static PostJoinPathEdge endEdge = new PostJoinPathEdge("", null);
}
