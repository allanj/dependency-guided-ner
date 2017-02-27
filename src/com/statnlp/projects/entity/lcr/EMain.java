package com.statnlp.projects.entity.lcr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.ModelType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfigReader;
import com.statnlp.projects.entity.DPConfig;


public class EMain {

	public static int trainNumber = -100;
	public static int testNumber = -100;
	public static int numIteration = -100;
	public static int numThreads = -100;
	public static String testFile = "";
	public static boolean isPipe = false;
	public static String nerOut;
	public static String topKNEROut;
	public static String nerRes;
	public static boolean isDev = false;
	public static HashSet<String> dataTypeSet;
	public static boolean topkinput = false;
	public static String MODEL = "crf";
	public static double adagrad_learningRate = 0.1;
	public static boolean useSSVMCost = false;
	public static boolean useAdaGrad = false;
	public static boolean useDepf = false;
	private static boolean testOnTrain = false;
	public static String dataset = "allanprocess";
	public static boolean cross_validation = false;
	
	
	public static void main(String[] args) throws IOException, InterruptedException{
		
		trainNumber = 80;
		testNumber = 2;
		numThreads = 5;
		numIteration = 200;
		isPipe = false;
		processArgs(args);
		dataTypeSet = Init.iniOntoNotesData(dataset);
		String modelType = DPConfig.MODEL.ecrf.name();

		
		String middle = isDev? ".dev":".test";
		nerOut = DPConfig.data_prefix+modelType+middle+".depf-"+useDepf+DPConfig.ner_eval_suffix;
		topKNEROut = DPConfig.data_prefix + modelType + middle +".depf-"+useDepf+ DPConfig.ner_topk_res_suffix;
		nerRes = DPConfig.data_prefix+modelType+middle+".depf-"+useDepf+ DPConfig.ner_res_suffix;
		testFile = isDev? DPConfig.devPath:DPConfig.testingPath;
		if(isPipe){
			testFile = isDev?DPConfig.dp2ner_dp_dev_input:DPConfig.dp2ner_dp_test_input;
			if(topkinput)
				testFile = isDev?DPConfig.dp2ner_dp_dev_input:DPConfig.dp2ner_dp_topK_test_input;
			nerOut = DPConfig.data_prefix+modelType+middle+".pred.depf-"+useDepf+DPConfig.ner_eval_suffix;
			nerRes = DPConfig.data_prefix+modelType+middle+".pred.depf-"+useDepf+ DPConfig.ner_res_suffix;
		}
		System.err.println("[Info] trainingFile: "+DPConfig.trainingPath);
		System.err.println("[Info] Cross_Validation: "+cross_validation);
		System.err.println("[Info] testFile: "+testFile);
		System.err.println("[Info] nerOut: "+nerOut);
		System.err.println("[Info] nerRes: "+nerRes);
		
		List<ECRFInstance> trainInstances = null;
		List<ECRFInstance> testInstances = null;
		/***********DEBUG*****************/
//		DPConfig.ecrftrain = "data/semeval10t1/output/ecrf.train.part.txt";
//		testFile="data/semeval10t1/ecrf.smalltest.txt";
//		DPConfig.writeWeight = true;
//		DPConfig.weightPath = "data/semeval10t1/ecrfWeight.txt";
//		DPConfig.readWeight = false;
//		testFile = DPConfig.ecrftrain;
//		testFile = "data/semeval10t1/ecrf.test.part.txt";
		/***************************/
		if(dataTypeSet.contains(DPConfig.dataType)){
			System.out.println("Reading Ontonotes conllx format...");
			trainInstances = EReader.readCoNLLX(DPConfig.trainingPath, true, trainNumber);
			testInstances = EReader.readCoNLLX(testFile, false, testNumber);
		}else{
			trainInstances = EReader.readData(DPConfig.trainingPath,true,trainNumber);
			testInstances = EReader.readData(testFile,false,testNumber);
			
//			testInstances = EReader.readData(testFile,false,testNumber,entityMap);
		}
//		System.out.println(com.statnlp.entity.Entity.Entities.toString());
//		Formatter.ner2Text(trainInstances, "data/testRandom2.txt");
//		System.exit(0);
		
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = DPConfig.L2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;
		
		OptimizerFactory of = OptimizerFactory.getLBFGSFactory();
		NetworkConfig.MODEL_TYPE = MODEL.equals("crf")? ModelType.CRF:ModelType.SSVM;
		if(NetworkConfig.USE_NEURAL_FEATURES){
			NeuralConfigReader.readConfig("config/neural.config");
			of = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(adagrad_learningRate);
		}
		if(NetworkConfig.MODEL_TYPE==ModelType.SSVM) of = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(adagrad_learningRate);
		if(useAdaGrad) of = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(adagrad_learningRate);
		
		if(cross_validation){
			crossValidate(trainInstances.toArray(new ECRFInstance[trainInstances.size()]), of);
			System.exit(0);
		}
		ECRFFeatureManager fa = new ECRFFeatureManager(new GlobalNetworkParam(of),useDepf);
		ECRFNetworkCompiler compiler = new ECRFNetworkCompiler(useSSVMCost);
		NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		ECRFInstance[] ecrfs = trainInstances.toArray(new ECRFInstance[trainInstances.size()]);
		/***Debug information****/
//		for(int n=1; n<=100; n++){
//			System.out.println("[Info] Now n is:"+n);
//			ArrayList<ECRFInstance> trains = new ArrayList<ECRFInstance>();
//			int idxId = 1;
//			for(ECRFInstance inst: trainInstances){
//				if(inst.size()==n) { inst.setLabeled(); inst.setInstanceId(idxId++); trains.add(inst); }
//			}
//			for(ECRFInstance inst: testInstances){
//				if(inst.size()==n) { inst.setLabeled(); inst.setInstanceId(idxId++); trains.add(inst); }
//			}
//			if(trains.size()==0) continue;
//			fa = new ECRFFeatureManager(new GlobalNetworkParam(of),useDepf);
//			compiler = new ECRFNetworkCompiler(useSSVMCost);
//			model = DiscriminativeNetworkModel.create(fa, compiler);
//			model.train(trains.toArray(new ECRFInstance[trains.size()]), numIteration);
// 		}
//		System.exit(0);
		/*********************/
		model.train(ecrfs, numIteration);
		if(testOnTrain){
			for(ECRFInstance inst:trainInstances) inst.setUnlabeled();
			testInstances = trainInstances;
		}
		Instance[] predictions = model.decode(testInstances.toArray(new ECRFInstance[testInstances.size()]));
		ECRFEval.evalNER(predictions, nerOut);
		ECRFEval.writeNERResult(predictions, nerRes, true);
		if(NetworkConfig._topKValue>1)
			ECRFEval.outputTopKNER(predictions, topKNEROut);
	}

	public static void crossValidate(ECRFInstance[] trainInsts, OptimizerFactory of) throws InterruptedException, IOException{
		System.err.println("[Info] Doing Cross Validation...");
		int foldNum = 10;
		//by default do 10-fold cross-validation
		int devSize = trainInsts.length/foldNum;
		ArrayList<ECRFInstance> cvInsts = new ArrayList<ECRFInstance>();
		ArrayList<ECRFInstance> cvTrainInsts = new ArrayList<ECRFInstance>();
		for(int fold = 0; fold < foldNum; fold++){
			int start = fold*devSize;
			int end = (fold+1)*devSize;
			System.out.println("\n[Info] The "+(fold+1)+"th fold validation.");
			int trainId = 0;
			cvInsts = new ArrayList<ECRFInstance>();
			cvTrainInsts = new ArrayList<ECRFInstance>();
			for(int idx=0; idx<trainInsts.length; idx++){
				if(idx>=start && idx<end){
					trainInsts[idx].setInstanceId(idx - start + 1);
					trainInsts[idx].setUnlabeled();
					cvInsts.add(trainInsts[idx]);
				}else{
					trainInsts[idx].setInstanceId(trainId+1);
					trainInsts[idx].setLabeled();
					trainId++;
					cvTrainInsts.add(trainInsts[idx]);
				}
			}
			ECRFFeatureManager fa = new ECRFFeatureManager(new GlobalNetworkParam(of),useDepf);
			ECRFNetworkCompiler compiler = new ECRFNetworkCompiler(useSSVMCost);
			NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
			model.train(cvTrainInsts.toArray(new ECRFInstance[cvTrainInsts.size()]), numIteration);
			Instance[] predictions = model.decode(cvInsts.toArray(new ECRFInstance[cvInsts.size()]));
			ECRFEval.evalNER(predictions, nerOut);
		}
	}
	
	
	public static void processArgs(String[] args){
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Linear-Chain CRF Version: Joint DEPENDENCY PARSING and Entity Recognition TASK: ");
			System.err.println("\t usage: java -jar dpe.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-trainNum": trainNumber = Integer.valueOf(args[i+1]); break;
					case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;
					case "-iter": numIteration = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;
					case "-pipe": isPipe = args[i+1].equals("true")?true:false; break;
					case "-testFile": testFile = args[i+1]; break;
					case "-reg": DPConfig.L2 = Double.valueOf(args[i+1]); break;
					case "-dev":isDev = args[i+1].equals("true")?true:false; break;
					case "-windows":DPConfig.windows = true; break;
					case "-comb": DPConfig.comb = true; break;
					case "-data":DPConfig.dataType=args[i+1];DPConfig.changeDataType(dataset); break;
					case "-topkinput": topkinput = true; break;
					case "-topk": NetworkConfig._topKValue = Integer.valueOf(args[i+1]); break;
					case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
									NetworkConfig.BATCH_SIZE = Integer.valueOf(args[i+1]); break;
					case "-model": MODEL = args[i+1]; break;
					case "-neural": if(args[i+1].equals("true")){ 
											NetworkConfig.USE_NEURAL_FEATURES = true; 
											NetworkConfig.OPTIMIZE_NEURAL = false;  //not optimize in CRF..
											NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
										} break;
					case "-lr": adagrad_learningRate = Double.valueOf(args[i+1]); break;
					case "-ssvmcost": if(args[i+1].equals("true")) useSSVMCost = true;
										else useSSVMCost = false; 
										break;
					case "-adagrad": useAdaGrad = args[i+1].equals("true")? true:false;break;
					case "-testtrain": testOnTrain = args[i+1].equals("true")? true:false;break;
					case "-depf": useDepf = args[i+1].equals("true")? true:false; break;
					case "-dataset": dataset = args[i+1];DPConfig.changeDataType(dataset); break;
					case "-cv": cross_validation = args[i+1].equals("true")? true:false; break;
					default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
				}
			}
			if(DPConfig.comb){
				DPConfig.changeTrainingPath();
			}
			System.err.println("[Info] trainNum: "+trainNumber);
			System.err.println("[Info] testNum: "+testNumber);
			System.err.println("[Info] numIter: "+numIteration);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] is Pipeline: "+isPipe);
			System.err.println("[Info] Using development set??: "+isDev);
			System.err.println("[Info] Data type: "+DPConfig.dataType);
			System.err.println("[Info] Regularization Parameter: "+DPConfig.L2);
			if(isPipe){
				System.err.println("[Info] *********PipeLine: from DP result to NER****");
			}
			String currentModel = isPipe? "Pipeline-DP2NER":"NER";
			System.err.println("[Info] CurrentModel:"+currentModel);
		}
	}
}
