/*
 * chombo: Hadoop Map Reduce utility
 * Author: Pranab Ghosh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.chombo.types;

/**
 * @author pranab
 *
 */
public class FloatDataType extends DataType {
	protected float min;
	protected float max;
	private boolean withLimitCheck;

	/**
	 * @param name
	 * @param strength
	 * @param min
	 * @param max
	 */
	public FloatDataType(String name,  int strength) {
		super(name, strength);
		withLimitCheck = false;
	}

	/**
	 * @param name
	 * @param strength
	 * @param min
	 * @param max
	 */
	public FloatDataType(String name, float min, float max, int strength) {
		super(name, strength);
		this.min = min;
		this.max = max;
		withLimitCheck = true;
	}
	
	@Override
	public boolean isMatched(String value) {
		boolean matched = false;
		float fVal = 0;
		try {
			fVal = Float.parseFloat(value);
			matched = withLimitCheck ? (fVal >= min && fVal <= max) : true;
		} catch (Exception ex) {
		}
		return matched;
	}
}
