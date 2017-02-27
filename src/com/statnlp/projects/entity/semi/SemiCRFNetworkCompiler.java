package com.statnlp.projects.entity.semi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkException;
import com.statnlp.hybridnetworks.NetworkIDMapper;

public class SemiCRFNetworkCompiler extends NetworkCompiler {
	
	private final static boolean DEBUG = false;
	
	private static final long serialVersionUID = 6585870230920484539L;
	public int maxSize = 128;
	public int maxSegmentLength = 8;
	public long[] allNodes;
	public int[][][] allChildren;
	private boolean useDepNet = false; 
	private boolean incom2Linear = false; //means if not incomplete then change to linear. //model 1
	private boolean notConnect2Linear = false; //model 2
	private boolean ignoreDisconnect = false; // if true, means the data must fit in the case that all of them are connected
	
	public enum NodeType {
		LEAF,
		NORMAL,
		ROOT,
	}
	
	static {
		NetworkIDMapper.setCapacity(new int[]{10000, 20, 100});
	}

	public SemiCRFNetworkCompiler(int maxSize, int maxSegLength,boolean useDepNet, boolean incom2Linear, boolean notConnect2Linear, boolean ignoreDisconnect) {
		this.maxSize = Math.max(maxSize, this.maxSize);
//		this.maxSize = 3;
		maxSegmentLength = Math.max(maxSegLength, maxSegmentLength);
//		this.maxSegmentLength = 2;
		System.out.println(String.format("Max size: %s, Max segment length: %s", maxSize, maxSegLength));
		System.out.println(Label.LABELS.toString());
		this.useDepNet = useDepNet;
		this.incom2Linear = incom2Linear;
		this.notConnect2Linear = notConnect2Linear;
		this.ignoreDisconnect = ignoreDisconnect;
		if(!useDepNet)
			buildUnlabeled(); //maybe use dep net.. we don't need to build it.
	}

	@Override
	public SemiCRFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		try{
			if(inst.isLabeled()){
				return compileLabeled(networkId, (SemiCRFInstance)inst, param);
			} else {
				if(!useDepNet)
					return compileUnlabeled(networkId, (SemiCRFInstance)inst, param);
				else {
					if(incom2Linear)
						return buildDepBasedUnlabeled(networkId, (SemiCRFInstance)inst, param); //this one..only for incomplete case
					if(notConnect2Linear)
						return buildDepBasedUnlabeled_bottomUp(networkId, (SemiCRFInstance)inst, param); 
					/** Original setting for using the dependency net. **/
//					SemiCRFInstance semiInst = (SemiCRFInstance)inst;
//					if(inst.getInstanceId()<0){ //means the unlabel of trianing data
//						if(EntityChecker.checkAllIncomplete(semiInst.getInput()).size()==0) return buildDepBasedUnlabeled(networkId, (SemiCRFInstance)inst, param);
//						else return compileUnlabeled(networkId, (SemiCRFInstance)inst, param);
//					}else
//						return compileUnlabeled(networkId, (SemiCRFInstance)inst, param); //means the test data.
					/** (END) Original setting for using the dependency net. **/
					throw new RuntimeException("What's the config to build?");
					
				}
			}
		} catch (NetworkException e){
			System.out.println(inst);
			throw e;
		}
	}
	
	private boolean checkIncomSpan(Sentence sent, Span span){
		if(span.label.equals(Label.get("O")) || span.start==span.end) return true;
		if(!(sent.get(span.start).getHeadIndex()==span.end || sent.get(span.end).getHeadIndex()==span.start)) return false;
		return true;
	}
	

	private int checkConnected(Sentence sent, Span span){
		int number = 0;
		int start = span.start;
		int end = span.end;
		Label label = span.label;
		if(label.equals(Label.get("O")) || start==end) return 0;
		HashSet<Integer> set = new HashSet<Integer>();
		int times = 0;
		while(times<(end-start+1)){
			for(int pos = start; pos<=end; pos++){
				int headIdx = sent.get(pos).getHeadIndex();
				if(headIdx<start || headIdx>end) continue;
				if(set.size()==0 || set.contains(pos) || set.contains(headIdx)){
					set.add(pos); set.add(headIdx);
				}
			}
			times++;
		}
		if(set.size()!=(end-start+1)) {
			number++;
		}
		return number;
	}

	
	private SemiCRFNetwork compileLabeled(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		SemiCRFNetwork network = new SemiCRFNetwork(networkId, instance, param,this);
		
		int size = instance.size();
		List<Span> output = instance.getOutput();
		Collections.sort(output);
		Sentence sent = instance.getInput();
		long leaf = toNode_leaf();
		network.addNode(leaf);
		long prevNode = leaf;
		
		for(Span span: output){
			int labelId = span.label.id;
			long end = toNode(span.end, labelId);
			network.addNode(end);
			int disconnectNum = notConnect2Linear? checkConnected(sent, span):0;
			if(notConnect2Linear && ignoreDisconnect && disconnectNum>0) throw new RuntimeException("Already ignore, shouldn't have disconnected in training data.");
			if( useDepNet && ( (incom2Linear && !checkIncomSpan(sent,span)) 
					|| (notConnect2Linear && disconnectNum>0) ) ){
				for(int pos=span.start; pos<span.end; pos++){
					long node = toNode(pos, labelId);
					network.addNode(node);
					network.addEdge(node, new long[]{prevNode});
					prevNode = node;
				}
			}
			network.addEdge(end, new long[]{prevNode});
			prevNode = end;
			
		}
		long root = toNode_root(size);
		network.addNode(root);
		network.addEdge(root, new long[]{prevNode});
		
		network.finalizeNetwork();
		//sViewer.visualizeNetwork(network, null, "Labeled Network");
		if(DEBUG){
//			System.out.println(network);
//			SemiCRFNetwork unlabeled = compileUnlabeled(networkId, instance, param);
			SemiCRFNetwork unlabeled = buildDepBasedUnlabeled_bottomUp(networkId, instance, param);
//			System.out.println("for instance: "+instance.getInput().toString());
			if(!unlabeled.contains(network)){
				System.out.println("not contains");
				
			}
		}
		return network;
	}
	
	private SemiCRFNetwork compileUnlabeled(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		int size = instance.size();
		long root = toNode_root(size);
		int root_k = Arrays.binarySearch(allNodes, root);
		int numNodes = root_k + 1;
		return new SemiCRFNetwork(networkId, instance, allNodes, allChildren, param, numNodes, this);
	}
	
	//for O label, should only with span length 1.
	private synchronized void buildUnlabeled(){
		SemiCRFNetwork network = new SemiCRFNetwork();
		long leaf = toNode_leaf();
		network.addNode(leaf);
		List<Long> currNodes = new ArrayList<Long>();
		for(int pos=0; pos<maxSize; pos++){
			for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
				long node = toNode(pos, labelId);
				if(labelId!=Label.LABELS.get("O").id){
					for(int prevPos=pos-1; prevPos >= pos-maxSegmentLength && prevPos >= 0; prevPos--){
						for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
							long prevBeginNode = toNode(prevPos, prevLabelId);
							if(network.contains(prevBeginNode)){
								network.addNode(node);
								network.addEdge(node, new long[]{prevBeginNode});
							}
						}
					}
					if(pos>=0){
						network.addNode(node);
						network.addEdge(node, new long[]{toNode_leaf()});
					}
				}else{
					//O label should be with only length 1. actually does not really affect.
					int prevPos = pos - 1;
					if(prevPos>=0){
						for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
							long prevBeginNode = toNode(prevPos, prevLabelId);
							if(network.contains(prevBeginNode)){
								network.addNode(node);
								network.addEdge(node, new long[]{prevBeginNode});
							}
						}
					}
					if(pos==0){
						network.addNode(node);
						network.addEdge(node, new long[]{toNode_leaf()});
					}
				}
				currNodes.add(node);
			}
			long root = toNode_root(pos+1);
			network.addNode(root);
			for(long currNode: currNodes){
				if(network.contains(currNode)){
					network.addEdge(root, new long[]{currNode});
				}	
			}
			currNodes = new ArrayList<Long>();
		}
		network.finalizeNetwork();
		//sViewer.visualizeNetwork(network, null, "UnLabeled Network");
		allNodes = network.getAllNodes();
		allChildren = network.getAllChildren();
	}
	
	/**
	 * No same entity nodes connected in this path. Model 2 Networkcomplier
	 * @param networkId
	 * @param instance
	 * @param param
	 * @return
	 */
	private SemiCRFNetwork buildDepBasedUnlabeled_bottomUp(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		SemiCRFNetwork network = new SemiCRFNetwork(networkId, instance, param,this);
		long leaf = toNode_leaf();
		network.addNode(leaf);
		long root = toNode_root(instance.size());
		network.addNode(root);
		Sentence sent = instance.input;
		int[][] leftDepRel = Utils.sent2LeftDepRel(sent);
		for(int pos=0; pos<sent.length(); pos++){
			if(pos==0){ //means that from the start up to here, so connect to leaf. And of course no leftDepRel
				for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
					long node = toNode(pos, labelId);
					network.addNode(node);
					network.addEdge(node, new long[]{leaf});
					if(pos==instance.size()-1) network.addEdge(root, new long[]{node});
				}
			}else{
				
				for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
					//add the prevsPosition
					long node = toNode(pos, labelId);
					for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
						long child = toNode(pos-1, prevLabelId);
						//if(labelId==prevLabelId && labelId!=Label.get("O").id) continue;
						if(network.contains(child)){
							network.addNode(node);
							network.addEdge(node, new long[]{child});
						}
					}
					if(pos==instance.size()-1){
						network.addEdge(root, new long[]{node});
					}
				}
				//based on the dependency add node
				int[] leftDepIdxs = leftDepRel[pos];
				for(int l=0; l<leftDepIdxs.length; l++){
					if(leftDepIdxs[l]<0) continue;
					int len = pos-leftDepIdxs[l] + 1;
					if(len>maxSegmentLength) continue;
					for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
						if(labelId==Label.get("O").id) continue;
						long node = toNode(pos, labelId);
						boolean[][] added = new boolean[sent.length()][Label.LABELS.size()]; //1-index. so 0 in this array is leaf. edges in network.
						int leftDepId = leftDepIdxs[l];
						long leftDepNode = toNode(leftDepId, labelId);
						if(network.getChildren_tmp(leftDepNode)==null){
							throw new RuntimeException("Should have some children by: "+leftDepId+","+labelId);
						}
						for(long[] grandChildren: network.getChildren_tmp(leftDepNode)){
							network.addNode(node);
							int[] grandChild = NetworkIDMapper.toHybridNodeArray(grandChildren[0]);
							len = pos - (grandChild[0]-1);
							if(len > maxSegmentLength) continue;
							if(!added[grandChild[0]][grandChild[1]]){
								network.addEdge(node, new long[]{grandChildren[0]}); //if the grandchildren still have the same type..add it or not? an option.
								added[grandChild[0]][grandChild[1]] = true;
							}
						}
					}
				}
			}
		}
		network.finalizeNetwork();
		return network;
	}
	
	/**
	 * Model 1 Network Compiler
	 * @param networkId
	 * @param instance
	 * @param param
	 * @return
	 */
	private SemiCRFNetwork buildDepBasedUnlabeled(int networkId, SemiCRFInstance instance, LocalNetworkParam param){
		SemiCRFNetwork network = new SemiCRFNetwork(networkId, instance, param,this);
		long leaf = toNode_leaf();
		network.addNode(leaf);
		long root = toNode_root(instance.size());
		network.addNode(root);
		Sentence sent = instance.getInput();
		for(int pos=0; pos<instance.size(); pos++){
			int headIdx = sent.get(pos).getHeadIndex();
			if(headIdx<0) continue; //means this one, the head is outside the sentence, which is leftOutside
			int smallOne = Math.min(pos, headIdx);
			int largeOne = Math.max(pos, headIdx);
			int length = largeOne - smallOne +1;
			if(length > maxSegmentLength){
				continue;
			}
			if(smallOne==0){ //means that from the start up to here
				for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
					if(labelId==Label.LABELS.get("O").id) continue;
					long node = toNode(largeOne, labelId);
					network.addNode(node);
					network.addEdge(node, new long[]{leaf});
				}
			}else{
				int prevEnd = smallOne - 1;
				for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
					if(labelId==Label.LABELS.get("O").id) continue;
					long node = toNode(largeOne, labelId);
					for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
						long prevEndNode = toNode(prevEnd, prevLabelId);
						network.addNode(node);
						network.addNode(prevEndNode);
						network.addEdge(node, new long[]{prevEndNode});
					}
				}
			}
		}
		
		//sepecifically for O
		for(int pos=0; pos<instance.size(); pos++){
			for(int labelId=0; labelId<Label.LABELS.size(); labelId++){
				long node = toNode(pos, labelId);
				int prevPos = pos - 1;
				if(prevPos>=0){
					for(int prevLabelId=0; prevLabelId<Label.LABELS.size(); prevLabelId++){
						long prevEndNode = toNode(prevPos, prevLabelId);
						network.addNode(node);
						network.addNode(prevEndNode);
						network.addEdge(node, new long[]{prevEndNode});
					}
				}else{
					//pos==0
					network.addNode(node);
					network.addEdge(node, new long[]{leaf});
				}
				if(pos==instance.size()-1){
					network.addEdge(root, new long[]{node});
				}
			}
			
		}
		network.finalizeNetwork();
		return network;
	}
	
	private long toNode_leaf(){
		return toNode(0, Label.get("O").id, NodeType.LEAF);
	}
	
	private long toNode(int pos, int labelId){
		return toNode(pos+1, labelId, NodeType.NORMAL);
	}
	
	private long toNode_root(int size){
		return toNode(size, Label.LABELS.size(), NodeType.ROOT);
	}
	
	private long toNode(int pos, int labelId, NodeType type){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos, labelId, type.ordinal()});
	}

	@Override
	public SemiCRFInstance decompile(Network net) {
		SemiCRFNetwork network = (SemiCRFNetwork)net;
		SemiCRFInstance result = (SemiCRFInstance)network.getInstance().duplicate();
		List<Span> prediction = new ArrayList<Span>();
		int node_k = network.countNodes()-1;
		while(node_k > 0){
			int[] children_k = network.getMaxPath(node_k);
			int[] child_arr = network.getNodeArray(children_k[0]);
			int pos = child_arr[0] - 1;;
			
			int nodeType = child_arr[2];
			if(nodeType == NodeType.LEAF.ordinal()){
				break;
			} 
			int labelId = child_arr[1];
			//System.err.println(pos+","+Label.LABELS_INDEX.get(labelId).getForm()+" ," + nodeType.toString());
			int end = pos;
			if(end!=0){
				int[] children_k1 = network.getMaxPath(children_k[0]);
				int[] child_arr1 = network.getNodeArray(children_k1[0]);
				int start = child_arr1[0] + 1 - 1;
				if(child_arr1[2]==NodeType.LEAF.ordinal())
					start = child_arr1[0];
				prediction.add(new Span(start, end, Label.LABELS_INDEX.get(labelId)));
			}else{
				prediction.add(new Span(end, end, Label.LABELS_INDEX.get(labelId)));
			}
			node_k = children_k[0];
			
		}
		Collections.sort(prediction);
		result.setPrediction(prediction);
		return result;
	}

	public double costAt(Network network, int parent_k, int[] child_k){
		return 0.0;
	}
	
	
}
