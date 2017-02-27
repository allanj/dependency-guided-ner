package com.statnlp.hybridnetworks;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

public class FeatureBox implements Serializable {

	private static final long serialVersionUID = 1779316632297457057L;

	/**
	 * Feature index array
	 */
	protected int[] _fs;
//	protected double _totalScore;
	protected double _currScore;
	
	protected int _version;
	protected boolean _alwaysChange = false;
	
	public FeatureBox(int[] fs) {
		this._fs = fs;
		this._version = -1; //the score is not calculated yet.
	}
	
	public int length() {
		return this._fs.length;
	}
	
	public int[] get() {
		return this._fs;
	}
	
	public int get(int pos) {
		return this._fs[pos];
	}

	/**
	 * Use the map to cache the feature index array to save the memory.
	 * @param fs
	 * @param param
	 * @return
	 */
	public static FeatureBox getFeatureBox(int[] fs, LocalNetworkParam param){
		FeatureBox fb = new FeatureBox(fs);
		if (!NetworkConfig.AVOID_DUPLICATE_FEATURES) {
			return fb;
		}
		if (param.fbMap == null) {
			param.fbMap = new HashMap<>();
		}
		if (param.fbMap.containsKey(fb)) {
			return param.fbMap.get(param.fbMap.get(fb));
		} else{
			param.fbMap.put(fb, fb);
			return fb;
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(_fs);
	}

	@Override
	public boolean equals(Object obj) {
		FeatureBox other = (FeatureBox)obj;
		return Arrays.equals(_fs, other._fs);
	}
	
	
}
