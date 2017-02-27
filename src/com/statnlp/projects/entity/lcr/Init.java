package com.statnlp.projects.entity.lcr;

import java.util.HashSet;

public class Init {

	
	public static HashSet<String> iniOntoNotesData(){
		HashSet<String> set = new HashSet<String>();
		set.add("abc");
		set.add("cnn");
		set.add("mnb");
		set.add("nbc");
		set.add("p25");
		set.add("pri");
		set.add("voa");
		return set;
	}
	
	
	public static HashSet<String> iniOntoNotesData(String dataset){
		HashSet<String> set = new HashSet<String>();
		if(dataset.equals("ontonotes")){
			set.add("bc");
			set.add("bn");
			set.add("mz");
			set.add("nw");
			set.add("tc");
			set.add("wb");
		}else if(dataset.equals("allanprocess")){
			set.add("abc");
			set.add("cnn");
			set.add("mnb");
			set.add("nbc");
			set.add("p25");
			set.add("pri");
			set.add("voa");
		}else if(dataset.equals("semeval10t1") || dataset.equals("conll2003")){
			
		}else
			throw new RuntimeException("unknow dataset name:"+dataset);
//		set.add("ptb");
		return set;
	}

	
	
}
