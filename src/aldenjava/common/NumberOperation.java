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

public class NumberOperation {
	public static <R extends Number & Comparable<R>> R addition(R summand1, R summand2) {
		if (!summand1.getClass().equals(summand2.getClass()))
			throw new IllegalArgumentException("Two different classes are used in the parameters: " + summand1.getClass().getName() + ", " + summand2.getClass().getName());
	    if(summand1 instanceof Double && summand2 instanceof Double)
	        return (R) new Double(summand1.doubleValue() + summand2.doubleValue());
	    else if(summand1 instanceof Float && summand2 instanceof Float)
	        return (R) new Float(summand1.floatValue() + summand2.floatValue());
	    else if(summand1 instanceof Long && summand2 instanceof Long)
	        return (R) new Long(summand1.longValue() + summand2.longValue());
	    else if(summand1 instanceof Integer && summand2 instanceof Integer)
	        return (R) new Integer(summand1.intValue() + summand2.intValue());
	    else if(summand1 instanceof Short && summand2 instanceof Short)
	        return (R) new Short((short) (summand1.shortValue() + summand2.shortValue()));
	    else if(summand1 instanceof Byte && summand2 instanceof Byte)
	        return (R) new Byte((byte) (summand1.byteValue() + summand2.byteValue()));
	    else
	    	throw new IllegalArgumentException(summand1.getClass().getName() + " is not supported.");
	}
	
	public static <R extends Number & Comparable<R>> R subtraction(R minuend, R substrahend) {
		if (!minuend.getClass().equals(substrahend.getClass()))
			throw new IllegalArgumentException("Two different classes are used in the parameters: " + minuend.getClass().getName() + ", " + substrahend.getClass().getName());
	    if(minuend instanceof Double && substrahend instanceof Double)
	        return (R) new Double(minuend.doubleValue() - substrahend.doubleValue());
	    else if(minuend instanceof Float && substrahend instanceof Float)
	        return (R) new Float(minuend.floatValue() - substrahend.floatValue());
	    else if(minuend instanceof Long && substrahend instanceof Long)
	        return (R) new Long(minuend.longValue() - substrahend.longValue());
	    else if(minuend instanceof Integer && substrahend instanceof Integer)
	        return (R) new Integer(minuend.intValue() - substrahend.intValue());
	    else if(minuend instanceof Short && substrahend instanceof Short)
	        return (R) new Short((short) (minuend.shortValue() - substrahend.shortValue()));
	    else if(minuend instanceof Byte && substrahend instanceof Byte)
	        return (R) new Byte((byte) (minuend.byteValue() - substrahend.byteValue()));
	    else
	    	throw new IllegalArgumentException(minuend.getClass().getName() + " is not supported.");
	}
	public static <R extends Number & Comparable<R>> R multiplication(R factor1, R factor2) {
	    if(factor1 instanceof Double && factor2 instanceof Double)
	        return (R) new Double(factor1.doubleValue() * factor2.doubleValue());
	    else if(factor1 instanceof Float && factor2 instanceof Float)
	        return (R) new Float(factor1.floatValue() * factor2.floatValue());
	    else if(factor1 instanceof Long && factor2 instanceof Long)
	        return (R) new Long(factor1.longValue() * factor2.longValue());
	    else if(factor1 instanceof Integer && factor2 instanceof Integer)
	        return (R) new Integer(factor1.intValue() * factor2.intValue());
	    else if(factor1 instanceof Short && factor2 instanceof Short)
	        return (R) new Short((short) (factor1.shortValue() * factor2.shortValue()));
	    else if(factor1 instanceof Byte && factor2 instanceof Byte)
	        return (R) new Byte((byte) (factor1.byteValue() * factor2.byteValue()));
	    else
	    	throw new IllegalArgumentException(factor1.getClass().getName() + " is not supported.");
	}

	public static <R extends Number & Comparable<R>> R getNumber(Class<R> rClass, Number n) {
		if (rClass.equals(Double.class))
			return (R) (Double) n.doubleValue();
		if (rClass.equals(Float.class))
			return (R) (Float) n.floatValue();
		if (rClass.equals(Long.class))
			return (R) (Long) n.longValue();
		if (rClass.equals(Integer.class))
			return (R) (Integer) n.intValue();
		if (rClass.equals(Short.class))
			return (R) (Short) n.shortValue();
		if (rClass.equals(Byte.class))
			return (R) (Byte) n.byteValue();
		throw new IllegalArgumentException(rClass.getName() + " is not supported.");
	}

}