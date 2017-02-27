package com.statnlp.projects.entity.semi;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.FeatureArray;
import com.statnlp.hybridnetworks.FeatureBox;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.neural.NeuralConfig;
import com.statnlp.projects.entity.semi.SemiCRFNetworkCompiler.NodeType;
import com.statnlp.projects.entity.utils.Extractor;

public class SemiCRFFeatureManager extends FeatureManager {
	
	private static final long serialVersionUID = 6510131496948610905L;
	private boolean depFeature;
	private int prefixSuffixLen = 3;
	//private int twoDirInsideLen = 3;

	public enum FeaType {
		seg_prev_word,
		seg_prev_word_shape,
		seg_prev_tag,
		seg_next_word,
		seg_next_word_shape,
		seg_next_tag,
		segment,
		seg_len,
		start_word,
		start_tag,
		end_word,
		end_tag,
		word,
		tag,
		shape,
		seg_pref,
		seg_suff,
		transition,
		neural,
		head_word,
		head_tag,
		dep_word_label,
		dep_tag_label,
		cheat,
		modifier_word,
		modifier_tag
//		left_3,
//		right_3,
//		left_3_shape,
//		right_3_shape,
//		segment_shape
	}
	
	private String OUT_SEP = NeuralConfig.OUT_SEP; 
	private String IN_SEP = NeuralConfig.IN_SEP;
	private final boolean CHEAT = false;
	private boolean nonMarkovFeature;
	
	public SemiCRFFeatureManager(GlobalNetworkParam param_g, boolean nonMarkov, boolean depFeature) {
		super(param_g);
		this.nonMarkovFeature = nonMarkov;
		this.depFeature = depFeature;
		if(CHEAT)
			System.out.println("[Info] Using the cheat features now..");
	}
	
	@Override
	protected FeatureArray extract_helper(Network net, int parent_k, int[] children_k) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance instance = (SemiCRFInstance)network.getInstance();
		
		Sentence sent = instance.getInput();
		
		
		int[] parent_arr = network.getNodeArray(parent_k);
		int parentPos = parent_arr[0] - 1;
		
		NodeType parentType = NodeType.values()[parent_arr[2]];
		int parentLabelId = parent_arr[1];
		
		//since unigram, root is not needed
		if(parentType == NodeType.LEAF || parentType == NodeType.ROOT){
			return FeatureArray.EMPTY;
		}
//		System.out.println("isLabeled: "+network.getInstance().isLabeled()+" parent:"+parentPos+" "+parentLabelId);
//		System.out.println("instance size:"+sent.length());
		int[] child_arr = network.getNodeArray(children_k[0]);
		int childPos = child_arr[0] + 1 - 1;
		NodeType childType = NodeType.values()[child_arr[2]];
		int childLabelId = child_arr[1];

		if(CHEAT){
			//int instanceId = Math.abs(instance.getInstanceId());
			//int cheatFeature = _param_g.toFeature(network, FeatureType.cheat.name(), parentLabelId+"", instanceId+" "+parentPos);
			int cheatFeature = _param_g.toFeature(network, FeaType.cheat.name(), "cheat", "cheat");
			return new FeatureArray(new int[]{cheatFeature});
		}
		
		FeatureArray fa = null;
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		int start = childPos;
		int end = parentPos;
		if(parentPos==0 || childType==NodeType.LEAF ) start = childPos;
		String currEn = Label.get(parentLabelId).getForm();
		
		String lw = start>0? sent.get(start-1).getName():"STR";
//		String llw = start == 0 ? "STR1" : start == 1 ? "STR" : sent.get(start-2).getName();
//		String llt = start == 0 ? "STR1" : start == 1 ? "STR" : sent.get(start-2).getTag();
//		String lllw = start == 0 ? "STR2" : start == 1 ? "STR1" : start == 2 ? "STR" : sent.get(start-3).getName();
		String ls = start>0? shape(lw):"STR_SHAPE";
		String lt = start>0? sent.get(start-1).getTag():"STR";
		String rw = end<sent.length()-1? sent.get(end+1).getName():"END";
//		String rrw = end == sent.length() - 1? "END1" : end == sent.length() - 2? "END" : sent.get(end+2).getName();
//		String rrt = end == sent.length() - 1? "END1" : end == sent.length() - 2? "END" : sent.get(end+2).getTag();
//		String rrrw = end == sent.length() - 1? "END2" : end == sent.length() - 2? "END1" : end == sent.length() - 3? "END" : sent.get(end+3).getName();
		String rt = end<sent.length()-1? sent.get(end+1).getTag():"END";
		String rs = end<sent.length()-1? shape(rw):"END_SHAPE";
		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_word.name(), 		currEn,	lw));
		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_word_shape.name(), currEn, ls));
		featureList.add(this._param_g.toFeature(network, FeaType.seg_prev_tag.name(), 		currEn, lt));
		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_word.name(), 		currEn, rw));
		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_word_shape.name(), currEn, rs));
		featureList.add(this._param_g.toFeature(network, FeaType.seg_next_tag.name(), 	currEn, rt));
		
		StringBuilder segPhrase = new StringBuilder(sent.get(start).getName());
		StringBuilder segPhraseShape = new StringBuilder(shape(sent.get(start).getName()));
		for(int pos=start+1;pos<=end; pos++){
			String w = sent.get(pos).getName();
			segPhrase.append(" "+w);
			segPhraseShape.append(" " + shape(w));
		}
		featureList.add(this._param_g.toFeature(network, FeaType.segment.name(), currEn,	segPhrase.toString()));
//		featureList.add(this._param_g.toFeature(network, FeaType.segment_shape.name(), currEn,	segPhraseShape.toString()));
		
		int lenOfSeg = end-start+1;
		featureList.add(this._param_g.toFeature(network, FeaType.seg_len.name(), currEn, lenOfSeg+""));
		
		/** Start and end features. **/
		String startWord = sent.get(start).getName();
		String startTag = sent.get(start).getTag();
		featureList.add(this._param_g.toFeature(network, FeaType.start_word.name(),	currEn,	startWord));
		featureList.add(this._param_g.toFeature(network, FeaType.start_tag.name(),	currEn,	startTag));
		String endW = sent.get(end).getName();
		String endT = sent.get(end).getTag();
		featureList.add(this._param_g.toFeature(network, FeaType.end_word.name(),		currEn,	endW));
		featureList.add(this._param_g.toFeature(network, FeaType.end_tag.name(),		currEn,	endT));
		
		int insideSegLen = lenOfSeg; //Math.min(twoDirInsideLen, lenOfSeg);
		for (int i = 0; i < insideSegLen; i++) {
			featureList.add(this._param_g.toFeature(network, FeaType.word.name()+":"+i,		currEn, sent.get(start+i).getName()));
			featureList.add(this._param_g.toFeature(network, FeaType.tag.name()+":"+i,		currEn, sent.get(start+i).getTag()));
			featureList.add(this._param_g.toFeature(network, FeaType.shape.name()+":"+i,	currEn, shape(sent.get(start+i).getName())));

			featureList.add(this._param_g.toFeature(network, FeaType.word.name()+":-"+i,	currEn,	sent.get(start+lenOfSeg-i-1).getName()));
			featureList.add(this._param_g.toFeature(network, FeaType.tag.name()+":-"+i,		currEn,	sent.get(start+lenOfSeg-i-1).getTag()));
			featureList.add(this._param_g.toFeature(network, FeaType.shape.name()+":-"+i,	currEn,	shape(sent.get(start+lenOfSeg-i-1).getName())));
		}
		/** needs to be modified maybe ***/
		for(int i=0; i<prefixSuffixLen; i++){
			String prefix = segPhrase.substring(0, Math.min(segPhrase.length(), i+1));
			String suffix = segPhrase.substring(Math.max(segPhrase.length()-i-1, 0));
			featureList.add(this._param_g.toFeature(network, FeaType.seg_pref.name()+"-"+i,	currEn,	prefix));
			featureList.add(this._param_g.toFeature(network, FeaType.seg_suff.name()+"-"+i,	currEn,	suffix));
		}
		String prevEntity = Label.get(childLabelId).getForm();
		featureList.add(this._param_g.toFeature(network,FeaType.transition.name(), prevEntity+"-"+currEn,	""));
		
//		featureList.add(this._param_g.toFeature(network, FeaType.left_3.name(),  currEn,	lllw + " & " + llw + " & " + lw));
//		featureList.add(this._param_g.toFeature(network, FeaType.right_3.name(), currEn,	rw + " & " + rrw + " & " + rrrw));
//		featureList.add(this._param_g.toFeature(network, FeaType.left_3_shape.name(),  currEn,	shape(lllw) + " & " + shape(llw) + " & " + shape(lw)));
//		featureList.add(this._param_g.toFeature(network, FeaType.right_3_shape.name(), currEn,	shape(rw) + " & " + shape(rrw) + " & " + shape(rrrw)));
		
		
		
		/** add non-markovian neural features **/
		if(NetworkConfig.USE_NEURAL_FEATURES && nonMarkovFeature){
			String position = null;
			if(start==0) position = "start";
			if(parentPos==sent.length()-1) position = "end";
			if(start!=0 && parentPos!=(sent.length()-1)) position = "inside";
			if(start==0 && parentPos==(sent.length()-1)) position = "cover";
			featureList.add(this._param_g.toFeature(network, FeaType.neural.name(), currEn, lenOfSeg+OUT_SEP+position+OUT_SEP+startWord+IN_SEP+endW));
//			featureList.add(this._param_g.toFeature(network, FeatureType.neural.name(), currEn, lenOfSeg+OUT_SEP+position+OUT_SEP+startWord+IN_SEP+endW+OUT_SEP+startTag+IN_SEP+endT));
		}
		/**  End (non-markovian neural features)**/
		
		
		/**Add dependency features**/
		if(this.depFeature){
			for (int pos = start; pos <= end; pos++) {
				String currWord = sent.get(pos).getName();
				String currTag = sent.get(pos).getTag();
				int currHeadIndex = sent.get(pos).getHeadIndex();
				String currHead = currHeadIndex>=0? sent.get(currHeadIndex).getName():"STR";
				String currHeadTag = currHeadIndex>=0? sent.get(currHeadIndex).getTag():"STR";
				String currDepLabel = currHeadIndex>=0? sent.get(currHeadIndex).getDepLabel():"NOLABEL";
				if(currDepLabel==null || currDepLabel.equals("null")) throw new RuntimeException("The depenency label is null?");
				featureList.add(this._param_g.toFeature(network, FeaType.head_word.name(),	currEn, currWord+"& head:"+currHead));
				featureList.add(this._param_g.toFeature(network, FeaType.head_tag.name(),	currEn,	currTag+"& head:"+currHeadTag));
				featureList.add(this._param_g.toFeature(network, FeaType.dep_word_label.name(), 	currEn, currWord+"& head:"+currHead+"& label:"+currDepLabel));
				featureList.add(this._param_g.toFeature(network, FeaType.dep_tag_label.name(), 	currEn, currTag+"& head:"+currHeadTag+"& label:"+currDepLabel));
				
				/**The following set of dependency features are better***/
//				featureList.add(this._param_g.toFeature(network, FeaType.head_word.name(), 		currEn, currHead));
//				featureList.add(this._param_g.toFeature(network, FeaType.head_tag.name(), 		currEn, currHeadTag)); //the most powerful one
//				featureList.add(this._param_g.toFeature(network, FeaType.dep_word_label.name(), currEn, currDepLabel));
//				for (int p = 0; p < sent.length(); p++){
//					if (p == pos) continue;
//					if (sent.get(p).getHeadIndex() == pos) {
//						featureList.add(this._param_g.toFeature(network, FeaType.modifier_word.name(), 	currEn, sent.get(p).getName()));
//						featureList.add(this._param_g.toFeature(network, FeaType.modifier_tag.name(), 	currEn, sent.get(p).getTag()));
//					}
//				}
				/*****/
			}
			
		}
		/**(END) add dependency features**/
		
		ArrayList<Integer> finalList = new ArrayList<Integer>();
		for(int i=0;i<featureList.size();i++){
			if(featureList.get(i)!=-1)
				finalList.add(featureList.get(i));
		}
		int[] features = new int[finalList.size()];
		for(int i=0;i<finalList.size();i++) features[i] = finalList.get(i);
		if(features.length==0) return FeatureArray.EMPTY;
		fa = new FeatureArray(FeatureBox.getFeatureBox(features, this.getParams_L()[network.getThreadId()]));
		
		return fa;
		
	}

	private String shape(String word){
		return Extractor.wordShapeOf(word);
	}
}
