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


package aldenjava.opticalmapping.phylogenetic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that stores pylogenetic tree
 * @author Alden
 *
 */
public class PhylogeneticTree {
	public static final int prime = 31;
	private final PhylogeneticTree subTree1;
	private final PhylogeneticTree subTree2;
	private final double branchLength; 
	private final String nodeName;
	private final int precompiledHashCode;
	public PhylogeneticTree(PhylogeneticTree subTree1, PhylogeneticTree subTree2, double branchLength) {
		this.subTree1 = subTree1;
		this.subTree2 = subTree2;
		this.nodeName = null;
		this.branchLength = branchLength;
		precompiledHashCode = compileHashCode();
		assert validateTree();
	}
	public PhylogeneticTree(String nodeName) {
		this.subTree1 = null;
		this.branchLength = 0;
		this.subTree2 = null;
		this.nodeName = nodeName;
		precompiledHashCode = compileHashCode();
	}
	private int compileHashCode() {
		if (isLeaf())
			return nodeName.hashCode();
		else
			return subTree1.hashCode() * PhylogeneticTree.prime + subTree2.hashCode();
	}
	public boolean isLeaf() {
		return subTree1 == null && subTree2 == null;
	}
	public int getNumberOfElements() {
		if (isLeaf())
			return 1;
		else
			return subTree1.getNumberOfElements() + subTree2.getNumberOfElements();
	}
	
	public PhylogeneticTree getTree1() {
		return subTree1;
	}
	public PhylogeneticTree getTree2() {
		return subTree2;
	}

	@Override
	public int hashCode() {
		return precompiledHashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof PhylogeneticTree))
			return false;
		PhylogeneticTree tree = (PhylogeneticTree) obj;
		if (isLeaf())
			return nodeName.equals(tree.nodeName);
		else
			return subTree1.equals(tree.subTree1) && subTree2.equals(tree.subTree2);
	}
	
	private void assignNodeNameList(List<String> list) {
		if (isLeaf()) 
			list.add(nodeName);
		else {
			subTree1.assignNodeNameList(list);
			subTree2.assignNodeNameList(list);
		}
	}
	
	public List<String> toFlatNameList() {
		List<String> list = new ArrayList<>();
		assignNodeNameList(list);
		return list;
	}
	
	private void assignNewickString(StringBuilder s, boolean outputDistance) {		
		if (isLeaf())
			s.append(nodeName);
		else {
			s.append("(");
			subTree1.assignNewickString(s, outputDistance);
			if (outputDistance) {
				s.append(":");
				s.append(Double.toString(branchLength - subTree1.branchLength));
			}
			s.append(",");
			subTree2.assignNewickString(s, outputDistance);
			if (outputDistance) {
				s.append(":");
				s.append(Double.toString(branchLength - subTree2.branchLength));
			}
			s.append(")");
		}		
	}
	public String toNewickString(boolean outputDistance) {
		StringBuilder s = new StringBuilder();
		assignNewickString(s, outputDistance);
		s.append(";");
		return s.toString();
	}
	/**
	 * An assertion method: Assert that its subtrees should not contain itself (making it a cyclic graph) 
	 * @return <code>true</code> if the tree passes the validation test 
	 */
	private boolean validateTree() {
		return validateTree(new HashSet<>()); 
	}
	
	private boolean validateTree(Set<PhylogeneticTree> set) {
		if (set.contains(this))
			return false;
		set.add(this);
		
		if (isLeaf())
			return true;
		else
			return subTree1.validateTree(set) && subTree2.validateTree(set); 
	}

}
