package com.statnlp.projects.entity.semi;

import java.util.ArrayList;

import com.statnlp.commons.types.Sentence;

public class Utils {

	
	public static int[][] sent2LeftDepRel(Sentence sent){
		int[][] leftDepRel = new int[sent.length()][];
		ArrayList<ArrayList<Integer>> leftDepList = new ArrayList<ArrayList<Integer>>();
		for(int i=0;i<leftDepRel.length;i++) leftDepList.add(new ArrayList<Integer>());
		for(int pos = 0; pos<sent.length(); pos++){
			int headIdx = sent.get(pos).getHeadIndex();
			if(headIdx<0) continue;
			int smallOne = Math.min(pos, headIdx);
			int largeOne = Math.max(pos, headIdx);
			ArrayList<Integer> curr = leftDepList.get(largeOne);
			curr.add(smallOne);
		}
		for(int pos=0; pos<sent.length(); pos++){
			ArrayList<Integer> curr = leftDepList.get(pos);
			leftDepRel[pos] = new int[curr.size()];
			for(int j=0; j<curr.size();j++)
				leftDepRel[pos][j] = curr.get(j);
		}
		return leftDepRel;
	}

}
