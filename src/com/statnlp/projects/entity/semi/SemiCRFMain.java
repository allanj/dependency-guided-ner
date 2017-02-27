package com.statnlp.projects.entity.semi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.ml.opt.OptimizerFactory;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;
import com.statnlp.hybridnetworks.DiscriminativeNetworkModel;
import com.statnlp.hybridnetworks.GenerativeNetworkModel;
import com.statnlp.hybridnetworks.GlobalNetworkParam;
import com.statnlp.hybridnetworks.NetworkConfig;
import com.statnlp.hybridnetworks.NetworkConfig.ModelType;
import com.statnlp.hybridnetworks.NetworkModel;
import com.statnlp.neural.NeuralConfigReader;
import com.statnlp.projects.entity.EntityChecker;

public class SemiCRFMain {
	
	
	public static int trainNum = 1000;
	public static int testNumber = -1;
	public static int numThread = 8;
	public static int numIterations = 5000;
	public static double AdaGrad_Learning_Rate = 0.1;
	public static double l2 = 0.01;
	public static boolean nonMarkov = false;
	public static String neuralConfig = "config/debug.config";
	public static boolean useAdaGrad = false;
	public static boolean depFeature = false;
	public static boolean useIncompleteSpan = false;
	public static boolean useDepNet = false;
	public static String modelFile = null;
	public static boolean isTrain = true;
	public static String dataType = "semeval10t1";
	public static String testSuff = "test";
	public static String train_filename = "data/alldata/nbc/ecrf.train.MISC.txt";
	public static String test_filename = "data/alldata/nbc/ecrf.test.MISC.txt";
	public static String extention = "semi";
	/** true means using the predicted dependency features.. if not used dep features, this option does not matter**/
	public static boolean isPipe = false; 
	public static boolean ignore = false;
	public static String dataset = "allanprocess"; //default
	public static boolean cross_validation = false; //
	
//	public static int[] debug = new int[150];
//	public static int[] debugNum = new int[150];
	
	
	private static void processArgs(String[] args) throws FileNotFoundException{
		for(int i=0;i<args.length;i=i+2){
			switch(args[i]){
				case "-trainNum": trainNum = Integer.valueOf(args[i+1]); break;   //default: all 
				case "-testNum": testNumber = Integer.valueOf(args[i+1]); break;    //default:all
				case "-iter": numIterations = Integer.valueOf(args[i+1]); break;   //default:100;
				case "-thread": numThread = Integer.valueOf(args[i+1]); break;   //default:5
				case "-windows": SemiEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
				case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
								NetworkConfig.BATCH_SIZE = Integer.valueOf(args[i+1]); break;
				case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
				case "-neural": if(args[i+1].equals("true")){ 
										NetworkConfig.USE_NEURAL_FEATURES = true; 
										NetworkConfig.REGULARIZE_NEURAL_FEATURES = false;
										NetworkConfig.OPTIMIZE_NEURAL = false;  //not optimize in CRF..
										NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
									}
								break;
				case "-neuralconfig":neuralConfig = args[i+1]; break;
				case "-reg": l2 = Double.valueOf(args[i+1]);  break;
				case "-lr": AdaGrad_Learning_Rate = Double.valueOf(args[i+1]); break;
				case "-adagrad": useAdaGrad = args[i+1].equals("true")? true:false;break;
				case "-nonmarkov": if(args[i+1].equals("true")) nonMarkov = true; else nonMarkov = false; break;
				case "-depf": if(args[i+1].equals("true")) depFeature = true; else depFeature= false; break;
//				case "-useincom": useIncompleteSpan = args[i+1].equals("true")? true:false;break;
//				case "-usedepnet": useDepNet = args[i+1].equals("true")? true:false;break;
				case "-modelPath": modelFile = args[i+1]; break;
				case "-dev": testSuff = args[i+1].equals("true")? "dev":"test"; 
						break; //this option currently deprecated, since we are using cross_validation. no dev.conllx file 
				case "-data": dataType = args[i+1]; break;
				case "-ext": extention = args[i+1]; break;
				case "-mode": isTrain = args[i+1].equals("train")?true:false; break;
				case "-pipe": isPipe = args[i+1].equals("true")?true:false;break;
				case "-ignore": ignore = args[i+1].equals("true")?true:false;break;
				case "-dataset": dataset = args[i+1]; break;
				case "-cv": cross_validation = args[i+1].equals("true")?true:false; 
							break;//cross validation
				case "-trainFile": train_filename = args[i+1]; break;
				case "-testFile": test_filename = args[i+1]; break;
				default: System.err.println("Invalid arguments, please check usage."); System.exit(0);
			}
		}
	}
	
	
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		
		
		//always use conll data
//		train_filename = "data/semi/semi.train.txt";
//		test_filename = "data/semi/semi.test.txt";
		processArgs(args);
		System.out.println("[Info] using the predicted dependency?:"+isPipe);
		/**data is 0-indexed, network compiler is 1-indexed since we have leaf nodes.**/
//		train_filename = "data/semeval10t1/ecrf.train.MISC.txt";
//		test_filename = "data/semeval10t1/ecrf."+testSuff+".MISC.txt";
		/**Read the all data**/
		String prefix = "data/"+dataset+"/"+dataType+"/";
		train_filename = prefix+"train.conllx";
		test_filename = isPipe? prefix+"pred_"+testSuff+".conllx":prefix+testSuff+".conllx";
		String depStruct = isPipe? "pred":"gold";
		boolean model1 = false;
		boolean model2 = false;
		if(extention.equals("dgm-s")) { model1 = true; useDepNet = true; }
		else if(extention.equals("dgm")) {model2 = true; useDepNet = true;}
		System.out.println("[Info] Current Model Extention:"+extention);
		System.out.println("[Info] Cross Validation:"+cross_validation);
		System.out.println("[Info] Ignore those not fit in "+extention+":"+ignore);
		System.out.println("[Info] Current Dataset:"+dataType);
		String ign = ignore? "ignore":"noignore";
		String resEval = "data/"+dataset+"/"+dataType+"/output/semi."+extention+"."+depStruct+".depf-"+depFeature+"."+ign+".eval.txt";
		String resRes  = "data/"+dataset+"/"+dataType+"/output/semi."+extention+"."+depStruct+".depf-"+depFeature+"."+ign+".res.txt";
		
		System.out.println("[Info] Reading data:"+train_filename);
		System.out.println("[Info] Reading data:"+test_filename);
		SemiCRFInstance[] trainInstances = readCoNLLData(train_filename, true,	trainNum);
		SemiCRFInstance[] testInstances	 = readCoNLLData(test_filename, false,	testNumber);
		
		/****Printing the total Number of entities***/
		int totalNumber = 0;
		int tokenNum = 0;
		for(SemiCRFInstance inst: trainInstances){
			totalNumber+=totalEntities(inst);
			tokenNum += inst.size();
		}
		System.out.println("[Info] Total number of entities in training:"+totalNumber+" token:"+tokenNum);
		totalNumber = 0;
		tokenNum = 0;
		for(SemiCRFInstance inst: testInstances){
			totalNumber+=totalEntities(inst);
			tokenNum += inst.size();
		}
		System.out.println("[Info] Total number of entities in testing:"+totalNumber+" token:"+tokenNum);
		/****(END) Printing the total Number of entities***/
		
		if(model2){
			//print some information if using model 2
			int notConnected = 0;
			PrintWriter pw = RAWF.writer("data/allanprocess/voa/train.lab");
			for(SemiCRFInstance inst: trainInstances){
				notConnected+=checkConnected(inst);
				/**** Only for outside usage.
				if(checkConnected(inst) == 0) {
					Sentence input = inst.getInput();
					String words = input.get(0).getName();
					String tags = input.get(0).getTag();
					String heads = "" + (input.get(0).getHeadIndex()+1);
					String depLabels = input.get(0).getEntity().equals("O") ? input.get(0).getDepLabel().toUpperCase() :
										//means this one is entity
										input.get(0).getHeadIndex() == -1 ? input.get(0).getDepLabel().toUpperCase() + "-" + input.get(0).getEntity().substring(2) : 
											EntityChecker.isEntity(input, 0, input.get(0).getHeadIndex()) ? input.get(0).getEntity().substring(2) : input.get(0).getDepLabel().toUpperCase() + "-" + input.get(0).getEntity().substring(2) ;
					
					for (int pos = 1; pos < inst.size(); pos++) {
						words += "\t" + input.get(pos).getName();
						tags += "\t" + input.get(pos).getTag();
						heads += "\t" + (input.get(pos).getHeadIndex()+1);
						String depLabel = input.get(pos).getEntity().equals("O") ? input.get(pos).getDepLabel().toUpperCase() :
							//means this one is entity
							input.get(pos).getHeadIndex() == -1 ? input.get(pos).getDepLabel().toUpperCase() + "-" + input.get(pos).getEntity().substring(2) : 
								EntityChecker.isEntity(input, pos, input.get(pos).getHeadIndex()) ? input.get(pos).getEntity().substring(2) : input.get(pos).getDepLabel().toUpperCase() + "-" + input.get(pos).getEntity().substring(2) ;
						depLabels += "\t" + depLabel;
					}
					pw.println(words);
					pw.println(tags);
					pw.println(depLabels);
					pw.println(heads);
					pw.println();
				}
				****/
			}
			pw.close();
			System.out.println("isgnore:"+ignore+" not connected entities in train:"+notConnected);
			notConnected = 0;
			pw = RAWF.writer("data/allanprocess/voa/test.lab");
			for(SemiCRFInstance inst: testInstances){
				notConnected+=checkConnected(inst);
				if(checkConnected(inst) == 0) {
					Sentence input = inst.getInput();
					String words = input.get(0).getName();
					String tags = input.get(0).getTag();
					String heads = "" + (input.get(0).getHeadIndex()+1);
					String depLabels = input.get(0).getEntity().equals("O") ? input.get(0).getDepLabel().toUpperCase() :
										//means this one is entity
										input.get(0).getHeadIndex() == -1 ? input.get(0).getDepLabel().toUpperCase() + "-" + input.get(0).getEntity().substring(2) : 
											EntityChecker.isEntity(input, 0, input.get(0).getHeadIndex()) ? input.get(0).getEntity().substring(2) : input.get(0).getDepLabel().toUpperCase() + "-" + input.get(0).getEntity().substring(2) ;
					
					for (int pos = 1; pos < inst.size(); pos++) {
						words += "\t" + input.get(pos).getName();
						tags += "\t" + input.get(pos).getTag();
						heads += "\t" + (input.get(pos).getHeadIndex()+1);
						String depLabel = input.get(pos).getEntity().equals("O") ? input.get(pos).getDepLabel().toUpperCase() :
							//means this one is entity
							input.get(pos).getHeadIndex() == -1 ? input.get(pos).getDepLabel().toUpperCase() + "-" + input.get(pos).getEntity().substring(2) : 
								EntityChecker.isEntity(input, pos, input.get(pos).getHeadIndex()) ? input.get(pos).getEntity().substring(2) : input.get(pos).getDepLabel().toUpperCase() + "-" + input.get(pos).getEntity().substring(2) ;
						depLabels += "\t" + depLabel;
					}
					pw.println(words);
					pw.println(tags);
					pw.println(depLabels);
					pw.println(heads);
					pw.println();
				}
			}
			pw.close();
			System.out.println("not connected entities in test:"+notConnected);
		}
		if(model1){
			//print some information if using model 2
			int incom = 0;
			for(SemiCRFInstance inst: trainInstances){
				incom+=EntityChecker.checkAllIncomplete(inst.input).size();
//				if(EntityChecker.checkAllIncomplete(inst.input).size()>0 && inst.size()<=15)
//					System.out.println(inst.input.toString()+"\n");
			}
			System.out.println("incomplete entities in train:"+incom);
			incom = 0;
			for(SemiCRFInstance inst: testInstances){
				incom+=EntityChecker.checkAllIncomplete(inst.input).size();
//				if(EntityChecker.checkAllIncomplete(inst.input).size()>0 && inst.size()<=15)
//					System.out.println(inst.input.toString()+"\n");
			}
			System.out.println("incomplete entities in test:"+incom);
		}
	
		int maxSize = 0;
		int maxSpan = 0;
		for(SemiCRFInstance instance: trainInstances){
			maxSize = Math.max(maxSize, instance.size());
			for(Span span: instance.output){
				maxSpan = Math.max(maxSpan, span.end-span.start+1);
			}
		}
		for(SemiCRFInstance instance: testInstances){
			maxSize = Math.max(maxSize, instance.size());
		}
		
		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = false;
		NetworkConfig.CACHE_FEATURES_DURING_TRAINING = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThread;
		NetworkConfig.PARALLEL_FEATURE_EXTRACTION = true;

		//modify this. and read neural config
		OptimizerFactory of = NetworkConfig.USE_NEURAL_FEATURES || NetworkConfig.MODEL_TYPE==ModelType.SSVM? 
				OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(AdaGrad_Learning_Rate):OptimizerFactory.getLBFGSFactory();
		
		if(NetworkConfig.USE_NEURAL_FEATURES) NeuralConfigReader.readConfig(neuralConfig);
		if(useAdaGrad) of = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(AdaGrad_Learning_Rate);
		
		
		int size = trainInstances.length;
		
		System.err.println("Read.."+size+" instances.");
		
		GlobalNetworkParam gnp = null;
		if(isTrain || modelFile==null || modelFile.equals("") || !new File(modelFile).exists()){
			gnp = new GlobalNetworkParam(of);
		}else{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(modelFile));
			gnp=(GlobalNetworkParam)in.readObject();
			in.close();
		}
		
		SemiCRFNetworkCompiler compiler = new SemiCRFNetworkCompiler(maxSize, maxSpan, useDepNet, model1, model2, ignore);
		SemiCRFFeatureManager fm = new SemiCRFFeatureManager(gnp, nonMarkov, depFeature);
		NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
		
//		/***Debug information**/
//		for(int n=1; n<=100; n++){
//			System.out.println("[Info] Now n is:"+n);
//			ArrayList<SemiCRFInstance> trains = new ArrayList<SemiCRFInstance>();
//			int idxId = 1;
//			for(SemiCRFInstance inst: trainInstances){
//				if(inst.size()==n) { inst.setLabeled();inst.setInstanceId(idxId++); trains.add(inst); }
//			}
//			for(SemiCRFInstance inst: testInstances){
//				if(inst.size()==n) { inst.setLabeled(); inst.setInstanceId(idxId++);trains.add(inst); }
//			}
//			if(trains.size()==0) continue;
//			gnp = new GlobalNetworkParam(of);
//			fm = new SemiCRFFeatureManager(gnp, nonMarkov, depFeature);
//			model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
//			model.train(trains.toArray(new SemiCRFInstance[trains.size()]), numIterations);
//		}
//		System.exit(0);
//		/**debug***/
		
		
		if(isTrain){
			
			if(cross_validation){
				int foldNum = 10;
				//by default do 10-fold cross-validation
				int devSize = trainInstances.length/foldNum;
				ArrayList<SemiCRFInstance> cvTestList = new ArrayList<SemiCRFInstance>();
				ArrayList<SemiCRFInstance> cvTrainList = new ArrayList<SemiCRFInstance>();
				for(int fold = 0; fold < foldNum; fold++){
					int start = fold*devSize;
					int end = (fold+1)*devSize;
					System.out.println("\n[Info] The "+(fold+1)+"th fold validation.");
					cvTrainList = new ArrayList<SemiCRFInstance>();
					cvTestList = new ArrayList<SemiCRFInstance>();
					int trainId = 0;
					for(int idx=0; idx<trainInstances.length; idx++){
						if(idx>=start && idx<end){
							trainInstances[idx].setInstanceId(idx - start + 1);
							trainInstances[idx].setUnlabeled();
							cvTestList.add(trainInstances[idx]);
						}else{
							trainInstances[idx].setInstanceId(trainId+1);
							trainInstances[idx].setLabeled();
							trainId++;
							cvTrainList.add(trainInstances[idx]);
						}
					}
					gnp = new GlobalNetworkParam(of);
					fm = new SemiCRFFeatureManager(gnp, nonMarkov, depFeature);
					model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fm, compiler) : DiscriminativeNetworkModel.create(fm, compiler);
					model.train(cvTrainList.toArray(new SemiCRFInstance[cvTrainList.size()]), numIterations);
					Instance[] predictions = model.decode(cvTestList.toArray(new SemiCRFInstance[cvTestList.size()]));
					SemiEval.evalNER(predictions, resEval);
				}
			}else
				model.train(trainInstances, numIterations);
			if(!cross_validation && modelFile!=null && !modelFile.equals("")){
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(modelFile));
				out.writeObject(fm.getParam_G());
				out.close();
			}
		}
		if(!cross_validation){
			Instance[] predictions = model.decode(testInstances);
			SemiEval.evalNER(predictions, resEval);
			SemiEval.writeNERResult(predictions, resRes);
		}
		
//		for(int i=1; i<150; i++){
//			System.out.println(debug[i]+"\t"+debugNum[i]);
//		}
		
	}
	
	/**
	 * Read data from file in a CoNLL format 0-index.
	 * @param fileName
	 * @param isLabeled
	 * @param isPipe: true means read the predicted features. always set to false for reading training instances.
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	private static SemiCRFInstance[] readCoNLLData(String fileName, boolean isLabeled, int number) throws IOException{
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<SemiCRFInstance> result = new ArrayList<SemiCRFInstance>();
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		List<Span> output = new ArrayList<Span>();
		int instanceId = 1;
		int start = -1;
		int end = 0;
		Label prevLabel = null;
		int sentIndex = 0;
		while(br.ready()){
			String line = br.readLine().trim();
			if(line.length() == 0){
				sentIndex++;
				end = wts.size()-1;
				if(start != -1){
					createSpan(output, start, end, prevLabel);
				}
				SemiCRFInstance instance = new SemiCRFInstance(instanceId, 1.0);
				WordToken[] wtArr = new WordToken[wts.size()];
				instance.input = new Sentence(wts.toArray(wtArr));
				instance.input.setRecognized();
				instance.output = output;
				/** debug information **/
				int realE = 0;
				for(int i=0;i<instance.input.length(); i++){
					if(instance.input.get(i).getEntity().startsWith("B-")) realE++;
				}
				int outputNum = 0;
				for(int i=0; i<instance.output.size();i++){
					if(!instance.output.get(i).label.form.equals("O")) outputNum++;
				}
				if(realE!=outputNum) {
					throw new RuntimeException("real number of entities:"+realE+" "+"span num:"+outputNum+" \n sent:"+sentIndex);
				}
				/***/
				//instance.leftDepRel = sent2LeftDepRel(instance.input);
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				if(isLabeled && ignore && extention.equals("dgm") && checkConnected(instance)>0){
					//do nothing. just don't add, ignore those invalid.
					
				}else if(isLabeled && ignore && extention.equals("dgm-s") && EntityChecker.checkAllIncomplete(instance.input).size()>0){
					//do nothing
				}else{
					instanceId++;
					result.add(instance);
				}
				wts = new ArrayList<WordToken>();
				output = new ArrayList<Span>();
				prevLabel = null;
				start = -1;
				end = 0;
				if(result.size()==number)
					break;
			} else {
				String[] values = line.split("[\t ]");
				int index = Integer.valueOf(values[0]) - 1; //because it is starting from 1
				String word = values[1];
				String pos = values[4];
				String depLabel = null;
				String form = values[10];
				int headIdx = -1;
				depLabel = values[7];
				headIdx = Integer.valueOf(values[6])-1;
				if(depLabel.contains("|")) throw new RuntimeException("Mutiple label?");
				wts.add(new WordToken(word, pos, headIdx, form, depLabel));
				Label label = null;
				if(form.startsWith("B")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel);
					}
					start = index;
					label = Label.get(form.substring(2));
					
				} else if(form.startsWith("I")){
					label = Label.get(form.substring(2));
				} else if(form.startsWith("O")){
					if(start != -1){
						end = index - 1;
						createSpan(output, start, end, prevLabel);
					}
					start = -1;
					createSpan(output, index, index, Label.get("O"));
					label = Label.get("O");
				}
				prevLabel = label;
			}
		}
		br.close();
		String type = isLabeled? "train":"test";
		System.out.println("[Info] number of "+type+" instances:"+result.size());
		return result.toArray(new SemiCRFInstance[result.size()]);
	}
	
 	private static void createSpan(List<Span> output, int start, int end, Label label){
		if(label==null){
			throw new RuntimeException("The label is null");
		}
		if(start>end){
			throw new RuntimeException("start cannot be larger than end");
		}
		if(label.form.equals("O")){
			for(int i=start; i<=end; i++){
				output.add(new Span(i, i, label));
			}
		} else {
			output.add(new Span(start, end, label));
		}
	}

	
	private static int checkConnected(SemiCRFInstance inst){
		int number = 0;
		List<Span> output = inst.getOutput();
		Sentence sent = inst.getInput();
		int[][] leftNodes = Utils.sent2LeftDepRel(sent);
		for(Span span: output){
			int start = span.start;
			int end = span.end;
			Label label = span.label;
			if(label.equals(Label.get("O")) || start==end) continue;
			boolean connected = traverseLeft(start, end, leftNodes);
			if(!connected) number++;
		}
//		if(number>0)
//			System.out.println(sent.toString());
		return number;
	}
	
	private static boolean traverseLeft(int start, int end, int[][] leftNodes){
		for(int l=0; l<leftNodes[end].length; l++){
			if(leftNodes[end][l]<start) continue;
			if(leftNodes[end][l]==start)
				return true;
			else if(traverseLeft(start, leftNodes[end][l], leftNodes))
				return true;
			else continue;
		}
		return false;
	}
	
	private static int totalEntities(SemiCRFInstance inst){
		int total = 0;
		List<Span> output = inst.getOutput();
//		Sentence sent = inst.getInput();
//		for(int i=0;i<sent.length();i++){
//			if(sent.get(i).getEntity().startsWith("B")) total++;
//		}
		for(Span span: output){
			Label label = span.label;
			if(label.equals(Label.get("O"))) continue;
			total++;
		}
		return total;
	}

}
