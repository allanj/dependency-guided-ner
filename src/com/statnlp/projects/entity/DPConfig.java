package com.statnlp.projects.entity;

public class DPConfig {

	public static String dataType = "semeval10t1";
	
	//this one for dependency parsing
//	public static String trainingPath = "data/"+dataType+"/en.train.txt";
//	public static String devPath = "data/"+dataType+"/en.devel.txt";
//	public static String testingPath = "data/"+dataType+"/en.test.txt";
	
	public static String trainingPath = "data/"+dataType+"/train.conllx";
	public static String devPath = "data/"+dataType+"/dev.conllx";
	public static String testingPath = "data/"+dataType+"/test.conllx";
	
//	public static String ecrftrain = "data/"+dataType+"/train.conllx";
//	public static String ecrfdev = "data/"+dataType+"/dev.conllx";
//	public static String ecrftest = "data/"+dataType+"/test.conllx";
	
	public static String data_prefix = "data/"+dataType+"/output/";
	
	public static String ner2dp_ner_dev_input = "data/"+dataType+"/pptest/ecrf.dev.ner.res.txt";
	public static String ner2dp_ner_test_input = "data/"+dataType+"/pptest/ecrf.test.ner.res.txt";
	public static String dp2ner_dp_dev_input = "data/"+dataType+"/pptest/only.dev.dp.res.txt";
	public static String dp2ner_dp_test_input = "data/"+dataType+"/pptest/only.test.dp.res.txt";
	public static String dp2ner_dp_topK_test_input = "data/"+dataType+"/pptest/only.test.dp.topk.res.txt";
	public static String ner2dp_ner_topK_test_input = "data/"+dataType+"/pptest/ecrf.test.ner.topk.res.txt";
	
	public static void changeDataType(String dataset){
		if(dataset.equals("semeval10t1")) dataset = "";
		trainingPath = "data/"+dataset+"/"+dataType+"/train.conllx";
		devPath = "data/"+dataset+"/"+dataType+"/dev.conllx";
		testingPath = "data/"+dataset+"/"+dataType+"/test.conllx";
		data_prefix = "data/"+dataset+"/"+dataType+"/output/";
		
		ner2dp_ner_dev_input = "data/"+dataset+"/"+dataType+"/pptest/ecrf.dev.ner.res.txt";
		ner2dp_ner_test_input = "data/"+dataset+"/"+dataType+"/pptest/ecrf.test.ner.res.txt";
		dp2ner_dp_dev_input = "data/"+dataset+"/"+dataType+"/pred_dev.conllx";
		dp2ner_dp_test_input = "data/"+dataset+"/"+dataType+"/pred_test.conllx";
		dp2ner_dp_topK_test_input = "data/"+dataset+"/"+dataType+"/pptest/pred_test.topk.conllx";
	}
	
	public static void changeDataType(){
		changeDataType("allanprocess");
	}
	
	public static void changeTrainingPath(){
		
		trainingPath = "data/"+dataType+"/en.train.combined.txt";
//		ecrftrain = "data/"+dataType+"/ecrf.train.MISC.combined.txt";
	}
	
	public static String ner_res_suffix = ".ner.res.txt";
	public static String ner_topk_res_suffix = ".ner.topk.res.txt";
	public static String dp_res_suffix = ".dp.res.txt";
	public static String dp_topk_res_suffix = ".dp.topk.res.txt";
	public static String dp_lab_res_suffix = ".depLab.res.txt";
	
	public static String ner_eval_suffix =".ner.eval.txt"; 
	public static String jointlinear_eval_suffix =".jointlinearner.eval.txt"; 
	
	public static String joint_res_suffix = ".joint.res.txt";
	
	/**
	 * This rand seed only for reading the input
	 */
	public static long randSeed = 1000;
	
	public static double L2 = 0.1;
	
	public static String[] others = {"plant","fac","loc","product","location",
			"event","animal","law","game","language","norp","org","disease","substance"};
	
	public static enum DIR {left, right};
	public static enum COMP{incomp, comp};
	
	public static String O_TYPE = "O";
	public static String MISC = "MISC";
	public static String E_B_PREFIX = "B-";
	public static String E_I_PREFIX = "I-";
	
	public static String PARENT_IS = "pae:";
	public static String OE = "OE";
	public static String ONE = "ONE";
	public static String NONE = "NONE";
	
	public static enum MODEL { ecrf,HYPEREDGE, DIVIDED, DIVCOPY, SegDep, LAB, HYBRID};
	
	public static boolean DEBUG = false;
	
	public static boolean windows = false;
	
	public static boolean comb = false;
	
	public static boolean writeWeight = false;
	
	public static boolean readWeight = false;
	public static String weightPath = "data/semeval10t1/dpWeight.txt";
	
	public static String modelRule = "data/semeval10t1/rule.txt";
	public static String currentModel;
	
	
	/******************For another simple model****************/
	public static String EMPTY = "EMPTY";
	
	public static enum WEIGHT_TYPE {local};
}
