/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

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
package com.statnlp.commons.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.statnlp.commons.types.SequencePair;

public class IBMModel1<SRC, TGT> {
	
	//Note: we are always generating TGT from SRC!
	private ArrayList<SequencePair<SRC, TGT>> _pairs;
	
	private HashMap<SRC, HashMap<TGT, Double>> _prob;
	private HashMap<SRC, HashMap<TGT, Double>> _counts;
	
	private int _iteration = 0;
	
	public IBMModel1(ArrayList<SequencePair<SRC, TGT>> pairs){
		this._pairs = pairs;
		this._counts = new HashMap<SRC, HashMap<TGT, Double>>();
	}
	
	public double getProb_ext(SRC src, TGT tgt){
		//if this is the first time to run this algorithm.
		if(this._prob == null) 
			throw new RuntimeException("The probs are null");
		if(!this._prob.containsKey(src))
			throw new RuntimeException("The probs does not contain "+src);
		if(!this._prob.get(src).containsKey(tgt))
			throw new RuntimeException("The probs does not contain the pair "+src+"/"+tgt);
		return this._prob.get(src).get(tgt);
	}
	
	//get the probability for generating tgt from src
	//note the direction!
	private double getProb(SRC src, TGT tgt){
		
		//if this is the first time to run this algorithm.
		if(this._prob == null && this._iteration == 0) return 0.01;//return Math.random();
		if(!this._prob.containsKey(src)){
			return 0.0;
		}
		if(!this._prob.get(src).containsKey(tgt)) return 0.0;
		return this._prob.get(src).get(tgt);
		
	}
	
	//we need to normalize the counts table
	//and then swap this table with the prob.
	//and then re-initialize the count table.
	private void update(){
		
		Iterator<SRC> srcs = this._counts.keySet().iterator();
		while(srcs.hasNext()){
			SRC src = srcs.next();
			HashMap<TGT, Double> map = this._counts.get(src);
			Iterator<TGT> tgts;
			
			double sum = 0;
			
			tgts = map.keySet().iterator();
			while(tgts.hasNext())
				sum += map.get(tgts.next());
			
			tgts = map.keySet().iterator();
			while(tgts.hasNext()){
				TGT tgt = tgts.next();
				map.put(tgt, map.get(tgt)/sum);
			}
		}
		this._prob = this._counts;
		this._counts = new HashMap<SRC, HashMap<TGT, Double>>();
		
	}
	
	//add the count to the given src-tgt pair.
	private void addCount(SRC src, TGT tgt, double count){
		
		if(!this._counts.containsKey(src))
			this._counts.put(src, new HashMap<TGT, Double>());
		HashMap<TGT, Double> map = this._counts.get(src);
		if(!map.containsKey(tgt)) map.put(tgt, 0.0);
		double oldCount = map.get(tgt);
		map.put(tgt, oldCount + count);
		
	}
	
	//for the instance at position "index", get its membership.
	//note that [i][j] means the the membership for i-th TGT and j-th SRC
	public double[][] getMembership(int index){
		
		SequencePair<SRC, TGT> pair = this._pairs.get(index);
		ArrayList<SRC> srcs = pair.getSrc();
		ArrayList<TGT> tgts = pair.getTgt();
		double[][] membership = new double[tgts.size()][srcs.size()];
		for(int i = 0; i<tgts.size(); i++){
			TGT tgt = tgts.get(i);
			double sum = 0;
			for(SRC src : srcs)
				sum += this.getProb(src, tgt);
			for(int j = 0; j <srcs.size(); j++){
				SRC src = srcs.get(j);
				double count = this.getProb(src, tgt)/sum;
				membership[i][j] = count;
			}
		}
		return membership;
		
	}
	
	//perform the EM training
	public void EM(int num_iterations){
		
		double oldProb = Double.NEGATIVE_INFINITY;
		for(int i = 0; i<num_iterations; i++){
			double prob = this.getLogLikelihood();
			if(this._iteration>1){
				if(prob >= oldProb){
					System.err.println(i+"\tLL = " + prob+"\t Improvement ="+(prob-oldProb));
				} else {
					System.err.println(i+"\tLL = " + prob+"\t Improvement ="+(prob-oldProb));
					System.err.println("ERROR.");
					System.exit(1);
				}
			}
			this.oneIteration();
			oldProb = prob;
		}
		
	}
	
	//compute the log-likelihood of the model
	public double getLogLikelihood(){
		
		double score = 0;
		for(SequencePair<SRC, TGT> pair : this._pairs){
			ArrayList<SRC> srcs = pair.getSrc();
			ArrayList<TGT> tgts = pair.getTgt();
			double weight = pair.getWeight();
			
			double sub_score = 0;
			double N = srcs.size();
			double M = tgts.size();
			for(TGT tgt : tgts){
				double sum = 0;
				for(SRC src : srcs)
					sum += this.getProb(src, tgt);
				sub_score += Math.log(sum);
			}
			sub_score -= Math.log(N) * M;
			score += sub_score * weight;
		}
		return score;
		
	}
	
	//perform one EM iteration
	private void oneIteration(){
		
		for(SequencePair<SRC, TGT> pair : this._pairs){
			ArrayList<SRC> srcs = pair.getSrc();
			ArrayList<TGT> tgts = pair.getTgt();
			double weight = pair.getWeight();
			
			for(int k = 0; k<tgts.size(); k++){
				TGT tgt = tgts.get(k);
				double sum = 0;
				for(int i = 0; i<srcs.size(); i++){
					SRC src = srcs.get(i);
					sum += this.getProb(src, tgt);
				}
				
				for(int i = 0; i<srcs.size(); i++){
					SRC src = srcs.get(i);
					double count = this.getProb(src, tgt)/sum;
					this.addCount(src, tgt, count * weight);
				}
			}
		}
		this.update();
		this._iteration++;
	}
	
}
