package com.statnlp.projects.entity.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.statnlp.commons.types.Sentence;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.trees.UnnamedDependency;

public class DataChecker {
	


	/**
	 * check wheter this list of dependency link is projective for one whole dependency structure
	 * @param list
	 * @return
	 */
	public static boolean checkProjective(ArrayList<UnnamedDependency> list){
		for(int i=0;i<list.size()-1;i++){
			UnnamedDependency dependency_i = list.get(i);
			CoreLabel iHeadLabel = (CoreLabel)(dependency_i.governor());
			CoreLabel iModifierLabel = (CoreLabel)(dependency_i.dependent());
			int iSmallIndex = Math.min(iHeadLabel.sentIndex(), iModifierLabel.sentIndex());
			int iLargeIndex = Math.max(iHeadLabel.sentIndex(), iModifierLabel.sentIndex());
			for(int j=i+1;j<list.size();j++){
				UnnamedDependency dependency_j = list.get(j);
				CoreLabel jHeadLabel = (CoreLabel)(dependency_j.governor());
				CoreLabel jModifierLabel = (CoreLabel)(dependency_j.dependent());
				int jSmallIndex = Math.min(jHeadLabel.sentIndex(), jModifierLabel.sentIndex());
				int jLargeIndex = Math.max(jHeadLabel.sentIndex(), jModifierLabel.sentIndex());
				if(iSmallIndex<jSmallIndex && iLargeIndex<jLargeIndex && jSmallIndex<iLargeIndex) return false;
				if(iSmallIndex>jSmallIndex && jLargeIndex>iSmallIndex && iLargeIndex>jLargeIndex) return false;
			}
		}
		return true;
	}
	
	/**
	 * Using the heads only to check the projectiveness
	 * @param heads
	 * @return
	 */
	public static boolean checkProjective(List<Integer> heads){
		for (int i = 0; i < heads.size(); i++) {
			int ihead = heads.get(i);
			if (ihead == -1) continue;
			int iSmallIndex = Math.min(i, ihead);
			int iLargeIndex = Math.max(i, ihead);
			for (int j = 0; j < heads.size(); j++) {
				int jhead = heads.get(j);
				if (i==j || jhead == -1) continue;
				int jSmallIndex = Math.min(j, jhead);
				int jLargeIndex = Math.max(j, jhead);
				if(iSmallIndex < jSmallIndex && iLargeIndex < jLargeIndex && jSmallIndex < iLargeIndex) return false;
				if(iSmallIndex > jSmallIndex && jLargeIndex > iSmallIndex && iLargeIndex > jLargeIndex) return false;
			}
		}
		return true;
	}

	public static boolean checkIsTree(List<Integer> heads) {
		HashMap<Integer, List<Integer>> tree = new HashMap<Integer, List<Integer>>();
		for (int i = 0; i < heads.size(); i++) {
			int ihead = heads.get(i);
			if (ihead == -1) continue;
			if (tree.containsKey(ihead)) {
				tree.get(ihead).add(i);
			} else {
				List<Integer> children = new ArrayList<>();
				children.add(i);
				tree.put(ihead, children);
			}
		}
		boolean[] visited = new boolean[heads.size()];
		Arrays.fill(visited, false);
		visited[0] = true;
		traverse(visited, 0, tree);
		for(int i = 0; i < visited.length; i++)
			if (!visited[i])
				return false;
		return true;
	}
	
	
	private static void traverse(boolean[] visited, int parent, HashMap<Integer, List<Integer>> tree) {
		if (tree.containsKey(parent)) {
			for(int child: tree.get(parent)) {
				visited[child] = true;
				traverse(visited, child, tree);
			}
		}
		
	}
	
	
	
	/**
	 * Similar to check Joint method, but only check whether its incomplete return a boolean value
	 * @param sent: just check for one sentence.
	 * @param tags
	 */
	public static ArrayList<EntitySpan> checkAllIncomplete(Sentence sent){
		int start = 0; int end = -1;
		ArrayList<EntitySpan> elist = new ArrayList<EntitySpan>();
		String prevEntity = "";
		for(int i=1;i<sent.length();i++){
			String e = sent.get(i).getEntity();
			e = e.equals("O")? e: e.substring(2);
			prevEntity = sent.get(i-1).getEntity();
			prevEntity = prevEntity.equals("O")? prevEntity:prevEntity.substring(2);
			//Need to fix the case of continuous two entity, start:inclusive, end:exclusive..Fixed by the reader already
			if(!e.equals(prevEntity)){
				if(!e.equals("O")){
					if(!prevEntity.equals("O")){
						end = i;
						if(notIncomplete(sent,start,end))
							elist.add(new EntitySpan(prevEntity,start,end-1));
					}
					start = i;
				}else{
					end = i;
					if(notIncomplete(sent,start,end)){
						elist.add(new EntitySpan(prevEntity,start,end-1));
					}
				}
			}
		}
		String lastE = sent.get(sent.length()-1).getEntity();
		lastE = lastE.equals("O")? lastE: lastE.substring(2);
		if(!lastE.equals("O")){
			end = sent.length();
			if(notIncomplete(sent,start,end))
				elist.add(new EntitySpan(lastE,start,end-1));
		}
		return elist;
	}
	
	/**
	 * check this entity is incomplete or not
	 * @param sent: sentence
	 * @param start: inclusive
	 * @param end: exlucsive
	 * @return
	 */
	private static boolean notIncomplete(Sentence sent, int start, int end){
//		if(end>start+1){
//			for(int x=start;x<end;x++){
//				if(sent.get(x).getHeadIndex()>=start || sent.get(x).getHeadIndex()<end)
//					System.err.println(sent.get(x).getDepLabel());
//			}
//		}
		return (end>start+1)  && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start);
	}

	
	


}
