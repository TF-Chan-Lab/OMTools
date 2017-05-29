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


package aldenjava.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementsGroup<K, V> {
	private final Map<K, Set<V>> groupElements;
	private final Map<V, K> elementKeys;
	private final KeyFactory<K> keyFactory;
	
	public ElementsGroup(KeyFactory<K> keyFactory) {
		this.keyFactory = keyFactory;
		groupElements = new HashMap<>();
		elementKeys = new HashMap<>();
	}
	
	public void add(Set<V> values) {
		Set<K> hitKeys = new HashSet<>();
		Set<V> unassignedElements = new HashSet<>();
		for (V value : values)
			if (elementKeys.containsKey(value))
				hitKeys.add(elementKeys.get(value));
			else
				unassignedElements.add(value);
		
		K key;
		if (hitKeys.isEmpty()) {
			key = keyFactory.newKey();
			groupElements.put(key, new HashSet<V>());
		}
		else {
			key = hitKeys.iterator().next();
			for (K hitKey : hitKeys)
				if (!hitKey.equals(key))
					groupElements.get(key).addAll(groupElements.remove(hitKey));
		}

		groupElements.get(key).addAll(unassignedElements);
		for (V value : groupElements.get(key))
			elementKeys.put(value, key);
	}
	
	public boolean containsValue(V value) {
		return elementKeys.containsKey(value);
	}
	
	public K getElementKey(V value) {
		return elementKeys.get(value);
	}
	
	public Set<K> getElementKeys(Set<V> values) {
		Set<K> hitKeys = new HashSet<>();
		for (V value : values)
			if (elementKeys.containsKey(value))
				hitKeys.add(elementKeys.get(value));
		return hitKeys;
	}
	
	public Set<V> getGroupElements(K key) {
		Set<V> values = new HashSet<>(groupElements.get(key));
		return values;
	}
	
	public Set<V> getGroupElements(Set<K> keys) {
		Set<V> values = new HashSet<>();
		for (K key : keys)
			values.addAll(groupElements.get(key));
		return values;
	}
	
	public void reset() {
		groupElements.clear();
		elementKeys.clear();
		keyFactory.reset();
	}
}
