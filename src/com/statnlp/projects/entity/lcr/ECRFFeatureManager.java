package com.statnlp.projects.entity.lcr;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.entity.Entity;
import com.statnlp.projects.entity.utils.Extractor;

public class ECRFFeatureManager extends FeatureManager {

	private static final long serialVersionUID = 376931974939202432L;

	public enum FeaType {word,
		tag,
		prev_tag,
		prev_word,
		shape,
		prev_shape,
		prefix, suffix,transition,
		head_word,
		head_tag,
		head_token,
		dep_word_label,
		dep_tag_label,
		neural,
		modifier_word,
		modifier_tag
//		next_word,
//		next_tag
//		word_2_back,
//		word_2_ahead,
//		tag_2_back,
//		tag_2_ahead,
//		surround_tags,
//		surround_shapes,
//		word_n_gram,
//		left_4,
//		right_4
		};
	protected boolean useDepF; 
//	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private int prefixLength = 3;
	
	public ECRFFeatureManager(GlobalNetworkParam param_g, boolean depf) {
		super(param_g);
		this.useDepF = depf;
	}
	
	//
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k) {
		// TODO Auto-generated method stub
		ECRFInstance inst = ((ECRFInstance)network.getInstance());
		//int instanceId = inst.getInstanceId();
		Sentence sent = inst.getInput();
		long node = network.getNode(parent_k);
		int[] nodeArr = NetworkIDMapper.toHybridNodeArray(node);
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int pos = nodeArr[0]-1;
		if(pos<0 || pos >= inst.size())
			return FeatureArray.EMPTY;
			
		int eId = nodeArr[1];
		int[] child = NetworkIDMapper.toHybridNodeArray(network.getNode(children_k[0]));
		int childEId = child[1];
//		int childPos = child[0]-1;
		
		String lw = pos>0? sent.get(pos-1).getName():"STR";
		String ls = pos>0? shape(lw):"STR_SHAPE";
		String lt = pos>0? sent.get(pos-1).getTag():"STR";
		String llw = pos==0? "STR1": pos==1? "STR": sent.get(pos-2).getName();
//		String lllw = pos==0? "STR2": pos==1? "STR1": pos==2? "STR": sent.get(pos-3).getName();
//		String llllw = pos==0? "STR3": pos==1? "STR2": pos==2? "STR1": pos==3? "STR":sent.get(pos-4).getName();
//		String llt = pos==0? "STR1": pos==1? "STR":sent.get(pos-2).getTag();
		
		String rw = pos<sent.length()-1? sent.get(pos+1).getName():"END";
//		String rt = pos<sent.length()-1? sent.get(pos+1).getTag():"END";
//		String rs = pos<sent.length()-1? shape(rw):"END_SHAPE";
		String rrw = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getName();
//		String rrrw = pos==sent.length()-1? "END2": pos==sent.length()-2? "END1":pos==sent.length()-3? "END": sent.get(pos+3).getName();
//		String rrrrw = pos==sent.length()-1? "END3": pos==sent.length()-2? "END2":pos==sent.length()-3? "END1":pos==sent.length()-4? "END":sent.get(pos+4).getName();
//		String rrt = pos==sent.length()-1? "END1": pos==sent.length()-2? "END":sent.get(pos+2).getTag();
		
		String currWord = inst.getInput().get(pos).getName();
		String currTag = inst.getInput().get(pos).getTag();
		String currShape = shape(currWord);
//		String childWord = childPos>=0? inst.getInput().get(childPos).getName():"STR";
//		String childTag = childPos>=0? inst.getInput().get(childPos).getTag():"STR";
		
		
		
		
		String currEn = Entity.get(eId).getForm();
		String prevEntity = Entity.get(childEId).getForm();
		if(NetworkConfig.USE_NEURAL_FEATURES){
//			featureList.add(this._param_g.toFeature(network, FEATYPE.neural.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+
//																				llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt));
			featureList.add(this._param_g.toFeature(network, FeaType.neural.name(), currEn, llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw));
		}
		
		/** Features adapted from Jenny Rose Finkel et.al 2009. (Order follows the table)**/
		featureList.add(this._param_g.toFeature(network, FeaType.word.name(), 			currEn,	currWord));
		featureList.add(this._param_g.toFeature(network, FeaType.prev_word.name(), 		currEn,	lw));
		featureList.add(this._param_g.toFeature(network, FeaType.tag.name(), 			currEn,	currTag));
		featureList.add(this._param_g.toFeature(network, FeaType.prev_tag.name(), 		currEn,	lt));
		featureList.add(this._param_g.toFeature(network, FeaType.shape.name(), 			currEn,	currShape));
		featureList.add(this._param_g.toFeature(network, FeaType.prev_shape.name(), 	currEn,	ls));
//		featureList.add(this._param_g.toFeature(network, FeaType.next_word.name(), 		currEn,	rw));
//		featureList.add(this._param_g.toFeature(network, FeaType.next_tag.name(), 		currEn,	rt));
//		featureList.add(this._param_g.toFeature(network, FeaType.surround_tags.name(), 	currEn,	lt + "-" + rt));
//		featureList.add(this._param_g.toFeature(network, FeaType.surround_shapes.name(),currEn,	shape(lw) + "-" + shape(rw)));
		
		
		/***Current Word Character n-gram**/
//		for (int l = 1; l <= currWord.length(); l++) {
//			for (int s = 0; s <= currWord.length() - l; s++) {
//				featureList.add(this._param_g.toFeature(network, FeaType.word_n_gram.name() + l, 	currEn,	currWord.substring(s, s+l)));
//			}
//		}
//		featureList.add(this._param_g.toFeature(network, FeaType.left_4.name(), 	currEn,	llllw + " & " + lllw + " & " + llw + " & " + lw));
//		featureList.add(this._param_g.toFeature(network, FeaType.right_4.name(), currEn,	rw + " & " + rrw + " & " + rrrw + " & " + rrrrw));
		/****/
		
		
		/****Add some prefix features******/
		for(int plen = 1;plen<=prefixLength;plen++){
			if(currWord.length()>=plen){
				String suff = currWord.substring(currWord.length()-plen, currWord.length());
				featureList.add(this._param_g.toFeature(network, FeaType.suffix.name()+"-"+plen, currEn, suff));
				String pref = currWord.substring(0,plen);
				featureList.add(this._param_g.toFeature(network, FeaType.prefix.name()+"-"+plen, currEn, pref));
			}
		}
		/*********Pairwise features********/
		featureList.add(this._param_g.toFeature(network, FeaType.transition.name(), currEn,	prevEntity));
		
		
		
//		if(true){
		if(this.useDepF){
			/** This option is for the pipeline from the dependency result to named entity recogntion.**/
			int currHeadIndex = sent.get(pos).getHeadIndex();
			String currHead = currHeadIndex>=0? sent.get(currHeadIndex).getName():"STR";
			String currHeadTag = currHeadIndex>=0? sent.get(currHeadIndex).getTag():"STR";
			String currDepLabel = currHeadIndex>=0? sent.get(currHeadIndex).getDepLabel():"NOLABEL";
			//This is the features that really help the model: most important features
			if(currDepLabel==null || currDepLabel.equals("null")) throw new RuntimeException("The depenency label is null?");
			featureList.add(this._param_g.toFeature(network, FeaType.head_word.name(), 		currEn, currWord+"& head:"+currHead));
			featureList.add(this._param_g.toFeature(network, FeaType.head_tag.name(), 		currEn, currTag+"& head:"+currHeadTag)); //the most powerful one
			featureList.add(this._param_g.toFeature(network, FeaType.dep_word_label.name(), 	currEn, currWord+"& head:"+currHead+"& label:"+currDepLabel));
			featureList.add(this._param_g.toFeature(network, FeaType.dep_tag_label.name(), 	currEn, currTag+"& head:"+currHeadTag+"& label:"+currDepLabel));
			
			/**The following set of dependency features are better***/
			
//			featureList.add(this._param_g.toFeature(network, FeaType.head_word.name(), 		currEn, currHead));
//			featureList.add(this._param_g.toFeature(network, FeaType.head_tag.name(), 		currEn, currHeadTag)); //the most powerful one
//			featureList.add(this._param_g.toFeature(network, FeaType.dep_word_label.name(), 	currEn, currDepLabel));
//			for (int p = 0; p < sent.length(); p++){
//				if (p == pos) continue;
//				if (sent.get(p).getHeadIndex() == pos) {
//					featureList.add(this._param_g.toFeature(network, FeaType.modifier_word.name(), 	currEn, sent.get(p).getName()));
//					featureList.add(this._param_g.toFeature(network, FeaType.modifier_tag.name(), 	currEn, sent.get(p).getTag()));
//				}
//			}
			
			/*****/
		}
		
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
//		fa = new FeatureArray(features);
		fa = new FeatureArray(FeatureBox.getFeatureBox(features, this.getParams_L()[network.getThreadId()]));
		
		return fa;
	}
	
	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}

}
