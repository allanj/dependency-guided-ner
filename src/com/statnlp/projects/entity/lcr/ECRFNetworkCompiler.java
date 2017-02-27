package com.statnlp.projects.entity.lcr;

import java.util.ArrayList;
import java.util.Arrays;

import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.Network;
import com.statnlp.hybridnetworks.NetworkCompiler;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkIDMapper;
import com.statnlp.projects.entity.Entity;

public class ECRFNetworkCompiler extends NetworkCompiler{

	private static final long serialVersionUID = -2388666010977956073L;

	public enum NODE_TYPES {LEAF,NODE,ROOT};
	public int _size;
	public ECRFNetwork genericUnlabeledNetwork;
	private boolean useSSVMCost;
	private static boolean DEBUG = false;
	
	public ECRFNetworkCompiler(boolean useSSVMCost){
		this._size = 150;
		this.useSSVMCost = useSSVMCost;
		this.compileUnlabeledInstancesGeneric();
	}
	
	@Override
	public ECRFNetwork compile(int networkId, Instance inst, LocalNetworkParam param) {
		// TODO Auto-generated method stub
		ECRFInstance lcrfInstance = (ECRFInstance)inst;
		if(lcrfInstance.isLabeled())
			return compileLabeledInstances(networkId, lcrfInstance, param);
		else
			return compileUnlabeledInstances(networkId, lcrfInstance, param);
	}
	
	public long toNode_leaf(){
		int[] arr = new int[]{0,0,0,0,NODE_TYPES.LEAF.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode(int pos, int tag_id){
		int[] arr = new int[]{pos+1,tag_id,0,0,NODE_TYPES.NODE.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}
	
	public long toNode_root(int size){
		int[] arr = new int[]{size+1,Entity.get("O").id,0,0,NODE_TYPES.ROOT.ordinal()};
		return NetworkIDMapper.toHybridNodeID(arr);
	}

	@Override
	public ECRFInstance decompile(Network network) {
		ECRFNetwork lcrfNetwork = (ECRFNetwork)network;
		ECRFInstance lcrfInstance = (ECRFInstance)lcrfNetwork.getInstance();
		ECRFInstance result = lcrfInstance.duplicate();
		ArrayList<String> prediction = new ArrayList<String>();
		
		
		long root = toNode_root(lcrfInstance.size());
		int rootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
		//System.err.println(rootIdx+" final score:"+network.getMax(rootIdx));
		if(NetworkConfig._topKValue==1){
			for(int i=0;i<lcrfInstance.size();i++){
				int child_k = lcrfNetwork.getMaxPath(rootIdx)[0];
				long child = lcrfNetwork.getNode(child_k);
				rootIdx = child_k;
				int tagID = NetworkIDMapper.toHybridNodeArray(child)[1];
				prediction.add(0, Entity.get(tagID).getForm());
			}
			
			result.setPrediction(prediction);
			result.setPredictionScore(lcrfNetwork.getMax());
		}else{
			int sentRootIdx = Arrays.binarySearch(lcrfNetwork.getAllNodes(),root);
			ArrayList<String> tmpPrediction = new ArrayList<String>();
			String[][] topKPrediction = new String[NetworkConfig._topKValue][];
			double[] topKScore = new double[NetworkConfig._topKValue];
			Arrays.fill(topKScore, Double.NEGATIVE_INFINITY);
			for(int kth=0;kth<NetworkConfig._topKValue;kth++){
				int subk = kth;
				if(network.getMaxTopK(network.countNodes()-1, kth)== Double.NEGATIVE_INFINITY){
					break;
				}
				rootIdx = sentRootIdx;
				tmpPrediction = new ArrayList<String>();
				topKScore[kth] = lcrfNetwork.getMaxTopK(rootIdx, kth);
				for(int i=0;i<lcrfInstance.size();i++){
					int child_k = lcrfNetwork.getMaxTopKPath(rootIdx, subk)[0];
					int child_k_best_order = lcrfNetwork.getMaxTopKBestListPath(rootIdx, subk)[0];
					long child = lcrfNetwork.getNode(child_k);
					rootIdx = child_k;
					subk = child_k_best_order;
					int tagID = NetworkIDMapper.toHybridNodeArray(child)[1];
					tmpPrediction.add(0, Entity.get(tagID).getForm());
				}
				String[] tmpArr = new String[lcrfInstance.size()];
				tmpPrediction.toArray(tmpArr);
				topKPrediction[kth] = tmpArr;
			}
			result.setTopKPrediction(topKPrediction);
			result.setTopKPredictionScore(topKScore);
		}
		
		
		
		
		return result;
	}
	

	public ECRFNetwork compileLabeledInstances(int networkId, ECRFInstance inst, LocalNetworkParam param){
		ECRFNetwork lcrfNetwork = new ECRFNetwork(networkId, inst,param, this);
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<inst.size();i++){
			long node = toNode(i,Entity.get(inst.getOutput().get(i)).id);
			lcrfNetwork.addNode(node);
			long[] currentNodes = new long[]{node};
			lcrfNetwork.addEdge(node, children);
			children = currentNodes;
		}
		long root = toNode_root(inst.size());
		lcrfNetwork.addNode(root);
		lcrfNetwork.addEdge(root, children);
		
		lcrfNetwork.finalizeNetwork();
		
		if(DEBUG && !genericUnlabeledNetwork.contains(lcrfNetwork)){
			System.err.println("wrong");
		}
		return lcrfNetwork;
	}
	
	public ECRFNetwork compileUnlabeledInstances(int networkId, ECRFInstance inst, LocalNetworkParam param){
		
		long[] allNodes = genericUnlabeledNetwork.getAllNodes();
		long root = toNode_root(inst.size());
		int rootIdx = Arrays.binarySearch(allNodes, root);
		ECRFNetwork lcrfNetwork = new ECRFNetwork(networkId, inst,allNodes,genericUnlabeledNetwork.getAllChildren() , param, rootIdx+1, this);
		return lcrfNetwork;
	}
	
	public void compileUnlabeledInstancesGeneric(){
		ECRFNetwork lcrfNetwork = new ECRFNetwork();
		
		long leaf = toNode_leaf();
		long[] children = new long[]{leaf};
		lcrfNetwork.addNode(leaf);
		for(int i=0;i<_size;i++){
			long[] currentNodes = new long[Entity.Entities.size()];
			for(int l=0;l<Entity.Entities.size();l++){
				if(i==0 && Entity.get(l).getForm().startsWith("I-")){ currentNodes[l]=-1; continue;}
				long node = toNode(i,l);
				currentNodes[l] = node;
				lcrfNetwork.addNode(node);
				for(long child: children){
					if(child==-1) continue;
					int[] childArr = NetworkIDMapper.toHybridNodeArray(child);
					String currE = Entity.get(l).getForm();
					String childE = Entity.get(childArr[1]).getForm();
					if(childE.startsWith("B-") && currE.startsWith("I-") && !childE.substring(2).equals(currE.substring(2))) continue;
					if(childE.startsWith("I-") && currE.startsWith("I-") && !childE.substring(2).equals(currE.substring(2))) continue;
					//if(entities[childArr[1]].startsWith("I-") && entities[l].startsWith("B-") && entities[childArr[1]].substring(2).equals(entities[l].substring(2))) continue;
					if(childE.equals("O") && currE.startsWith("I-")) continue;
					lcrfNetwork.addEdge(node, new long[]{child});
				}
			}
			long root = toNode_root(i+1);
			lcrfNetwork.addNode(root);
			for(long child:currentNodes){
				if(child==-1) continue;
				lcrfNetwork.addEdge(root, new long[]{child});
			}
				
			children = currentNodes;
			
		}
		lcrfNetwork.finalizeNetwork();
		genericUnlabeledNetwork =  lcrfNetwork;
	}
	
	public double costAt(Network network, int parent_k, int[] child_k){
		if(this.useSSVMCost)
			return super.costAt(network, parent_k, child_k);
		else return 0.0;
	}
	
}
