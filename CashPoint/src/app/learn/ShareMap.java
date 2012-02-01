package app.learn;

import java.util.*;

/**
 * A map containing names as keys and sharing amounts as values
 *
 */
@SuppressWarnings("serial")
public class ShareMap extends TreeMap<String, Number> 
{
	protected ShareMap() {}
	/**
	 * creates a sorted map of deals for a number of individuals.
	 * A portion is assigned to the name in the corresponding spot.
	 * If the portion of an individual appears as null value the corresponding name is ignored in the map.
	 * @param names	the array of names of the dealers (not necessarily sorted)
	 * @param portions	given deals, if any, for individuals according to the order of the names
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
	 * If the proportion for an individual sharer is zero the corresponding name is ignored in the map.
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param proportions	given proportions for the sharers according to the order of the names
     */
    public ShareMap(String[] names, double amount, int[] proportions) {
    	int n = Math.min(names.length, proportions.length);
    	if (n > 0) {
			int denominator = 0;
			for (int i = 0; i < n; i++)
				denominator += Math.abs(proportions[i]);
			
			if (denominator > 0) 
				for (int i = 0; i < n; i++) 
					if (proportions[i] != 0)
						put(names[i], -amount * Math.abs(proportions[i]) / denominator);
    	}
   		else if (names.length > 0)
    		put(names[0], -amount);
    }
	/**
	 * creates a sorted map of shares for a number of sharers optionally allowing for fixed portions of any of the sharers
	 * except the first one whom the remainder of the amount is assigned to in any case.
	 * If no portion is given for an individual sharer an equally shared portion is assumed (amount divided by the number of sharers).
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
					amount += get(name).doubleValue();
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
    	for (Number value : values()) 
			sum += value.doubleValue();
    	return sum;
    }
    /**
     * turns each value in the map to its negative
     * @return	the negated map
     */
    public ShareMap negated() {
		for (Map.Entry<String, Number> share : entrySet())
			put(share.getKey(), -share.getValue().doubleValue());
    	return this;
    }
}