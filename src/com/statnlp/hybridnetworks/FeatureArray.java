/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.statnlp.hybridnetworks;

import java.io.Serializable;
import java.util.HashMap;

public class FeatureArray implements Serializable{
	
	private static final long serialVersionUID = 9170537017171193020L;
	
	private double _totalScore;
	private FeatureBox _fb;
	protected boolean _isLocal = false;
	
	public static final FeatureArray EMPTY = new FeatureArray(new int[0]);
	public static final FeatureArray NEGATIVE_INFINITY = new FeatureArray(Double.NEGATIVE_INFINITY);
	
	private FeatureArray _next;
	
	/**
	 * Merges the features in <code>fs</code> and in <code>next</code>
//	 *@deprecated This one is inefficient. Use {@link #FeatureArray(int[])} instead.
	 * @param fs
	 * @param next
	 */
	public FeatureArray(int[] fs, FeatureArray next) {
		this._fb = new FeatureBox(fs);
		this._next = next;
	}
	
	/**
	 * Construct a feature array containing the features identified by their indices
	 * @param fs
	 */
	public FeatureArray(int[] fs) {
		this._fb = new FeatureBox(fs);
		this._next = null;
		this._isLocal = false;
	}
	
	public FeatureArray(FeatureBox fia) {
		this(fia, null);
		this._isLocal = false;
	}
	
	public FeatureArray(FeatureBox fia, FeatureArray next) {
		this._fb = fia;
		this._next = next;
		this._isLocal = false;
	}
	
	public void next(FeatureArray next){
		this._next = next;
	}
	
	private FeatureArray(double score) {
		this._totalScore = score;
	}
	
	public void setAlwaysChange(){
		this._fb._alwaysChange = true;
	}
	
	public FeatureArray toLocal(LocalNetworkParam param){
		if(this==NEGATIVE_INFINITY){
			return this;
		}
		if(this._isLocal){
			return this;
		}
		
		int length = this._fb.length();
		if(NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY){
			for(int fs: this._fb.get()){
				if(fs == -1){
					length--;
				}
			}
		}
		
		int[] fs_local = new int[length];
		int localIdx = 0;
		for(int k = 0; k<this._fb.length(); k++, localIdx++){
			if(this._fb.get(k) == -1 && NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY){
				localIdx--;
				continue;
			}
			if(!NetworkConfig.PARALLEL_FEATURE_EXTRACTION || NetworkConfig.NUM_THREADS == 1 || param._isFinalized){
				fs_local[localIdx] = param.toLocalFeature(this._fb.get(k));
			} else {
				fs_local[localIdx] = this._fb.get(k);
			}
			if(fs_local[localIdx]==-1){
				throw new RuntimeException("The local feature got an id of -1 for " + this._fb.get(k));
			}
		}
		
		FeatureArray fa;
		if (this._next != null){
//			fa = new FeatureArray(fs_local, this._next.toLocal(param)); //previous usage
			fa = new FeatureArray(FeatureBox.getFeatureBox(fs_local, param), this._next.toLocal(param)); //saving memory
		}else{
//			fa = new FeatureArray(fs_local);  //previous usage
			fa = new FeatureArray(FeatureBox.getFeatureBox(fs_local, param)); //saving memory
		}
		fa._isLocal = true;
		fa._fb._alwaysChange = this._fb._alwaysChange;
		return fa;
	}
	
	public int[] getCurrent(){
		return this._fb.get();
	}
	
	public FeatureArray getNext(){
		return this._next;
	}
	
	public void update(LocalNetworkParam param, double count){
		if(this == NEGATIVE_INFINITY){
			return;
		}
		
//		if(!this._isLocal)
//			throw new RuntimeException("This feature array is not local");
		
		int[] fs_local = this.getCurrent();
		for(int f_local : fs_local){
			param.addCount(f_local, count);
		}
		if(this._next != null){
			this._next.update(param, count);
		}
	}
	
	
	public void update_MF_Version(LocalNetworkParam param, double count, HashMap<Integer, Integer> fIdx2DstNode, HashMap<Integer, Double> marginalMap){
		if(this == NEGATIVE_INFINITY){
			return;
		}
		
		int[] fs_local = this.getCurrent();
		for (int f_local : fs_local) {
			double featureValue = 1.0;
			if (fIdx2DstNode.containsKey(f_local)) {
				int dstNode = fIdx2DstNode.get(f_local);
				if (marginalMap.containsKey(dstNode))
					featureValue = Math.exp(marginalMap.get(dstNode));
				else
					featureValue = 0.0;
			}
			param.addCount(f_local, featureValue * count);
		}
		if(this._next != null){
			this._next.update_MF_Version(param, count, fIdx2DstNode, marginalMap);
		}
	}
	
	/**
	 * Return the sum of weights of the features in this array
	 * @param param
	 * @return
	 */
	public double getScore(LocalNetworkParam param, int version){
		if(this == NEGATIVE_INFINITY){
			return this._totalScore;
		}
		
		if(!this._isLocal != param.isGlobalMode()) {
			System.err.println(this._next);
			throw new RuntimeException("This FeatureArray is local? "+this._isLocal+"; The param is "+param.isGlobalMode());
		}
		
		//if the score is negative infinity, it means disabled.
		if(this._totalScore == Double.NEGATIVE_INFINITY){
			return this._totalScore;
		}
		this._totalScore = 0.0;
		if (this._fb._version != version){
			this._fb._currScore = this.computeScore(param, this.getCurrent());
			this._fb._version = version;
		}
		this._totalScore += this._fb._currScore;
		if (this._next != null){
			this._totalScore += this._next.getScore(param, version);
		}
		return this._totalScore;
	}
	
	/**
	 * Compute the score using the parameter and the feature array
	 * @param param
	 * @param fs
	 * @return
	 */
	private double computeScore(LocalNetworkParam param, int[] fs){
		if(!this._isLocal != param.isGlobalMode()) {
			throw new RuntimeException("This FeatureArray is local? "+this._isLocal+"; The param is "+param.isGlobalMode());
		}
		
		double score = 0.0;
		for(int f : fs){
			if(f!=-1){
				score += param.getWeight(f);
			}
		}
		return score;
	}
	
	/**
	 * Get the marginal score using the marginal score as feature value
	 * @param param
	 * @param <featureIdx, targetNode> map, the target node is the corresponding node.
	 * @param marginals score array, serve as being the feature value. 
	 * @return
	 */
	public double getScore_MF_Version(LocalNetworkParam param, HashMap<Integer, Integer> fIdx2DstNode, HashMap<Integer, Double> marginalMap, int version){
		if(this == NEGATIVE_INFINITY){
			return this._totalScore;
		}
		if(!this._isLocal != param.isGlobalMode()) {
			throw new RuntimeException("This FeatureArray is local? "+this._isLocal+"; The param is "+param.isGlobalMode());
		}
		//if the score is negative infinity, it means disabled.
		if(this._totalScore == Double.NEGATIVE_INFINITY){
			return this._totalScore;
		}
		this._totalScore = 0.0;
		if (this._fb._version != version || this._fb._alwaysChange){
			this._fb._currScore = 0.0;
			for(int f : this.getCurrent()){
				if(f!=-1){
					//note that in training, f is the local feature index.
					//in testing, f is the global feature index
					double featureValue = 1.0;
					if (fIdx2DstNode.containsKey(f)) {
						int dstNode = fIdx2DstNode.get(f);
						if (marginalMap.containsKey(dstNode))
							featureValue = Math.exp(marginalMap.get(dstNode));
						else
							featureValue = 0.0;
					}
					this._fb._currScore += param.getWeight(f) * featureValue;
				}
			}
			this._fb._version = version;
		}
		this._totalScore += this._fb._currScore;
		
		if (this._next != null){
			this._totalScore += this._next.getScore_MF_Version(param, fIdx2DstNode, marginalMap, version);
		}
		return this._totalScore;
	}
	
	//returns the number of elements in the feature array
	public int size(){
		int size = this._fb.length();
		if (this._next != null){
			size += this._next.size();
		}
		return size;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for(int k = 0; k<this._fb.length(); k++){
			if(k!=0)
				sb.append(' ');
			sb.append(this._fb.get(k));
		}
		sb.append(']');
		return sb.toString();
	}
	
	@Override
	public int hashCode(){
		int code = 0;
		for(int i = 0; i<this._fb.length(); i++){
			code ^= this._fb.get(i);
		}
		if (this._next != null){
			code = code ^ this._next.hashCode();
		}
		return code;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof FeatureArray){
			FeatureArray fa = (FeatureArray)o;
			for(int k = 0; k< this._fb.length(); k++){
				if(this._fb.get(k) != fa._fb.get(k)){
					return false;
				}
			}
			if(this._next == null){
				if(fa._next != null){
					return false;
				}
				return true;
			}else{
				return this._next.equals(fa._next);
			}
		}
		return false;
	}
	
}