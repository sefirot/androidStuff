package com.applang;

import java.util.*;

/**
 * A <code>TreeMap</code> containing names as keys and sharing amounts as values
 *
 */
@SuppressWarnings("serial")
public class ShareMap extends TreeMap<String, Double> 
{
	protected ShareMap() {}
	/**
	 * creates a sorted map of deals for a number of participants.
	 * A portion is assigned to the name in the corresponding spot.
	 * If the portion of an participant appears as null value the corresponding name is ignored in the map.
	 * @param names	the array of names of the dealers (not necessarily sorted)
	 * @param portions	given deals, if any, for individual participants according to the order of the names
	 */
	public ShareMap(String[] names, Double[] portions) {
    	int n = Math.min(names.length, portions.length);
    	for (int i = 0; i < n; i++) {
    		if (Util.isAvailable(i, portions))
	        	put(names[i], portions[i]);
    	}
	}
    /**
	 * creates a sorted map of shares for a number of sharers according to fixed proportions for any of the sharers
	 * If no proportions are specified uniform sharing is assumed (amount divided by the number of sharers).
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param proportions	given proportions for the sharers according to the order of the names
     */
    public ShareMap(String[] names, double amount, Integer[] proportions) {
		this(names, amount);
		
    	int n = Util.isNullOrEmpty(proportions) ? 0 : proportions.length;
    	if (n > 0) {
    		Double[] normalized = normalize(proportions);
			if (normalized.length > 0) 
				for (int i = 0; i < names.length; i++) 
					if (Util.isAvailable(i, normalized))
						put(names[i], -amount * normalized[i]);
					else
						put(names[i], 0.);
    	}
    }
	/**
	 * creates a sorted map of shares for a number of sharers optionally allowing for fixed portions of any of the sharers
	 * except the first one whom the remainder of the amount is assigned to in any case.
	 * If no portion is specified for an individual sharer an uniformly shared portion is assumed (amount divided by the number of sharers).
	 * If the portion of an individual sharer appears as null value the corresponding name is ignored in the map.
	 * The values in the resulting map are the negated portions of the amount so that their sum equals the negative value of the amount.
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param portions	given portions, if any, for individual sharers according to the order of the names
	 */
    public ShareMap(String[] names, double amount, Double... portions) {
    	int n = names.length;
    	if (n > 0) {
    		double portion = amount / n;
    		
			for (int i = 1; i < n; i++) {
				String name = names[i];
				
				if (Util.isAvailable(i, portions))
					put(name, -portions[i]);
				else if (i >= portions.length)
					put(name, -portion);
				
				if (containsKey(name))
					amount += get(name);
			}
			
			put(names[0], -amount);
		}
    }
    /**
     * calculates the sum of all the values in the map
     * @return	the value of the sum
     */
    public double sum() {
    	double sum = 0;
    	for (double value : values()) 
			sum += value;
    	return sum;
    }
    /**
     * turns each value in the map to its negative
     * @return	the negated map
     */
    public ShareMap negated() {
		for (Map.Entry<String, Double> share : entrySet())
			put(share.getKey(), -share.getValue());
    	return this;
    }
    /**
     * subtracts a vector from the values (vector) of this map
     * @param subtrahend	the vector to be subtracted from this
     */
    public void minus(Collection<Double> vector) {
    	Double[] subtrahend = vector.toArray(new Double[0]);
    	int n = Math.min(size(), subtrahend.length);
    	if (n > 0) {
    		Set<String> keys = keySet();
    		Iterator<String> it = keys.iterator();
			for (int i = 0; i < n; i++) {
				String name = it.next();
				put(name, get(name) - subtrahend[i]);
			}
    	}
    }
    
    Double[] normalize(Integer[] proportions) {
    	int n = proportions.length;
    	Double[] normalized = new Double[n];
    	
    	double denominator = 0;
		for (int i = 0; i < n; i++)
			denominator += Math.abs(proportions[i]);
		
		if (denominator > 0) {
			for (int i = 0; i < n; i++) 
				normalized[i] = Math.abs(proportions[i]) / denominator;
		}

		return normalized;
    }
}