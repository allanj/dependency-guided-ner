package com.statnlp.projects.entity.lcr;

import java.util.ArrayList;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;


public class ECRFInstance extends Instance {


	private static final long serialVersionUID = 1851514046050983662L;
	protected Sentence sentence;
	protected ArrayList<String> entities;
	protected ArrayList<String> predictons;
	
	protected String[][] topKPredictions;
	protected double[] topKScore;
	
	protected int globalId = -1;
	protected double predictionScore;
	
	public ECRFInstance(int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
	}
	
	public ECRFInstance(int globalId, int instanceId, double weight, Sentence sent) {
		super(instanceId, weight);
		this.sentence = sent;
		this.globalId = globalId;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return this.sentence.length();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ECRFInstance duplicate() {
		ECRFInstance inst = new ECRFInstance(this.globalId, this._instanceId, this._weight,this.sentence);
		if(entities!=null)
			inst.entities = (ArrayList<String>)entities.clone();
		else inst.entities = null;
		if(predictons!=null)
			inst.predictons =(ArrayList<String>)predictons.clone();
		else inst.predictons = null;
		
		
		if(topKPredictions!=null)
			inst.topKPredictions = topKPredictions.clone();
		else inst.topKPredictions = null;
		inst.topKPredictions = this.topKPredictions;
		
		if(topKScore!=null)
			inst.topKScore = topKScore.clone();
		else inst.topKScore = null;
		inst.topKScore = this.topKScore;
		
		return inst;
	}

	@Override
	public void removeOutput() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removePrediction() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Sentence getInput() {
		return this.sentence;
	}

	@Override
	public ArrayList<String> getOutput() {
		// TODO Auto-generated method stub
		return this.entities;
	}

	@Override
	public ArrayList<String> getPrediction() {
		// TODO Auto-generated method stub
		return this.predictons;
	}

	@Override
	public boolean hasOutput() {
		if(entities!=null) return true;
		else return false;
	}

	@Override
	public boolean hasPrediction() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPrediction(Object o) {
		this.predictons = (ArrayList<String>)o;
	}
	
	public int getGlobalId(){
		return this.globalId;
	}

	public void setPredictionScore(double score){this.predictionScore = score;}
	public double getPredictionScore(){return this.predictionScore;}
	
	public void setTopKPrediction(String[][] topKPrediction){this.topKPredictions = topKPrediction;}
	public void setTopKPredictionScore(double[] topkScore){this.topKScore = topkScore;}
	
	public String[][] getTopKPrediction(){return this.topKPredictions;}
	public double[] getTopKScore(){return this.topKScore;}
}
