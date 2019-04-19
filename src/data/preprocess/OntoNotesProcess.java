package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

import edu.stanford.nlp.trees.GrammaticalStructureConversionUtils;

public class OntoNotesProcess {

	
	public static String[] datasets = {"bc","bn","mz","nw","tc","wb"};
//	public static String[] datasets = {"bc"};
	public static String[] valid = {"PERSON", "ORG", "GPE","NORP","FAC","LOC","PRODUCT","DATE","TIME","PERCENT","MONEY","QUANTITY","ORDINAL","CARDINAL","EVENT","WORK_OF_ART","LAW","LANGUAGE"};
	public static HashSet<String> validSet;
	public static HashSet<String> dataNames;
	
	public static String[] fileType = {"train","development","conll-2012-test"}; //for english
//	public static String[] fileType = {"train","development","test"}; //for chinese
	
	public static String tmpParse = "F:/data/conll2012/tmp/tmp.parse";
	public static String tmpConllOut = "F:/data/conll2012/tmp/tmpOut.conll";
	public static String tmpConllErr = "F:/data/conll2012/tmp/tmp.err";
	
	public static String output_folder = "ontonotes_english";  //only need to change these two parameters during running
	public static String lang = "en"; //en or zh
	public static boolean useStanfordDep = true; //only need to change these two parameters during running
	
	public static void process() throws IOException, InterruptedException{
		dataNames = new HashSet<String>();
		for(String file: datasets) dataNames.add(file);
		validSet = new HashSet<String>();
		for(int i=0;i<valid.length;i++) { validSet.add(valid[i]);}
		
		for(int f=0;f<fileType.length;f++){
			String data = null;
			if (fileType[f].equals("conll-2012-test")) {
				data = "pradhan-processed";  //test set use this one
			} else if (fileType[f].equals("train") || fileType[f].equals("development")) {
				data = "conll2012-official-processed"; //training, dev use this one
			} else {
				if (fileType[f].equals("test") && lang.equals("zh")) {
					data = "conll2012-official-processed"; // for chinese all use train, dev, test on official
				} else 
					throw new RuntimeException("unknow type: " + fileType[f]);
			}
			String filePrefix = "F:/data/conll2012/"+data+"/data";
			String outputPrefix = useStanfordDep? "F:/data/conll2012/"+output_folder+"_sd" : "F:/data/conll2012/"+output_folder+"_ud";
			File directory = new File(outputPrefix);
		    if (! directory.exists()){
		        directory.mkdir();
		    }
			String language =  lang.equals("zh")? "chinese" : "english"; 
			String currPrefix = filePrefix+"/"+fileType[f]+"/data/"+language+"/annotations";
			File file = new File(currPrefix);
			String[] names = file.list();
			int numToken = 0;
			for(String newstype: names){
				if(dataNames.contains(newstype)){
					
					File subFile = new File(currPrefix+"/"+newstype); //the folder that bc/bn/nw
					String[] subNames = subFile.list();
					ArrayList<Sentence> sents = new ArrayList<Sentence>();
					for(String program: subNames){
						if(newstype.equals("nw") &&  (program.equals("p2.5_c2e") || program.equals("dev_09_c2e")) ) continue;  //these files do not have .name file.
						if(newstype.equals("wb") &&  ( program.equals("dev_09_c2e")|| program.equals("sel")) ) continue;
						File programFolder = new File(currPrefix+"/"+newstype+"/"+program); //abc/cctc/cnn/so on
						String[] numFolderList = programFolder.list();  //cnn/00 ,01,02,
						for(String numFolderName: numFolderList){
//							if(newstype.equals("nw") &&  program.equals("wsj") && (numFolderName.equals("00") || numFolderName.equals("01")|| numFolderName.equals("24")) ) continue;
							File numFolder = new File(currPrefix+"/"+newstype+"/"+program+"/"+numFolderName); //abc/00 folder
							String[] textFileList = numFolder.list();
							for(String textFile: textFileList){  
								if(textFile.endsWith("gold_conll")){ 
									numToken+=processNameFile(currPrefix+"/"+newstype+"/"+program+"/"+numFolderName+"/"+textFile, sents, useStanfordDep);
								}
							}
						}
						
					}
					System.err.println("[Info] Finishing "+fileType[f]+" dataset:"+newstype);
					//print these sentences. write to Files
					printConll(outputPrefix, newstype,sents, fileType[f]);
				}
			}
			System.err.println("[Info] number of token:"+numToken);
		}
		
		
	}
	
	private static int processNameFile(String filePath, ArrayList<Sentence> sents, boolean sd) throws IOException{
		BufferedReader reader = RAWF.reader(filePath);
		PrintWriter pw = RAWF.writer(tmpParse);
		String line = null;
		String prevEntity = "O";
		String prevLine = "*";
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		wts.add(new WordToken("root", "root", -1, "O", "root"));
		int numToken = 0;
		while((line = reader.readLine())!=null){
			if(line.startsWith("#end")) continue;
			if(line.startsWith("#")) continue;
			String[] vals = line.split("\\s+");
			//first-step convert to dep structure
			if(line.equals("")){
				//finish a sentence
				//convert to dependency here.
				
				WordToken[] wtarr = new WordToken[wts.size()];
				wts.toArray(wtarr);
				Sentence sent = new Sentence(wtarr);
				pw.close();
				PrintStream out = new PrintStream(new FileOutputStream(tmpConllOut));
//				PrintStream err = new PrintStream(new FileOutputStream(tmpConllErr));
				PrintStream console = System.out;
				System.setOut(out);
//				System.setErr(err);
				String dep_format = sd ? lang+"-sd" : lang; //stanford dep or universal dep
				//-basic -keepPunct  -language en-sd -conllx -makeCopulaHead -treeFile D:/Downloads/cnn_0103.parse
//				EnglishGrammaticalStructure.main(new String[]{"-basic","-keepPunct","-conllx","-treeFile",tmpParse, "-language", "en"}); // en-sd if use Stanford dependency
				GrammaticalStructureConversionUtils.convertTrees(new String[]{"-basic","-keepPunct","-conllx","-treeFile",tmpParse}, dep_format);// en-sd if use Stanford dependency 
				System.setOut(console);
//				System.setErr(System.err);
				BufferedReader depReader = RAWF.reader(tmpConllOut);
				String depLine = null;
				int index = 1;
				while((depLine=depReader.readLine())!=null){
					if(depLine.equals("")) continue;
					String[] components = depLine.split("\\t+");
					int headIdx = Integer.valueOf(components[6]);
					String depLabel = components[7];
					sent.get(index).setHead(headIdx);
					sent.get(index).setDepLabel(depLabel);
					index++;
				}
				depReader.close();
				if(index!=sent.length()) {
					System.err.println("The length is not equal to each other. index:"+index+" sent length:"+sent.length()+"\n"+filePath+"\n"+sent.toString());
				}else{
					sents.add(sent);
				}
				prevEntity = "O";
				prevLine = "*";
				pw = RAWF.writer(tmpParse);
				wts = new ArrayList<WordToken>();
				wts.add(new WordToken("root", "root", -1, "O", "root"));
				continue;
			}
			if(vals.length<11) throw new RuntimeException("split length smaller than 11:. length is:"+vals.length+"\n"+filePath+"\n"+line);
			String word = vals[3];
			String pos = vals[4]; 
			//if(pos.equals("XX")) throw new RuntimeException("POS: "+pos+" No POS tag:\n"+filePath);
			String parseBit = vals[5];
			if(word.equals("［") || word.equals("〈")||word.equals("＜")||word.equals("｛")) {
				word = "-LRB-";
			} 
			if(word.equals("］")|| word.equals("〉")|| word.equals("＞")||word.equals("｝")) {
				word = "-RRB-";
			}
			parseBit = parseBit.replace("*", "("+pos+" "+word+")");  
			pw.write(parseBit+"\n");

			String[] infos = getCurrEntity(prevEntity, prevLine, vals[10]);
			String entity = infos[0];
			prevEntity = infos[1];
			prevLine = infos[2];
			WordToken wt = new WordToken(word, pos, -100, entity); //does not have the dep at this stage.
			wts.add(wt);
			if(vals[10].startsWith("("))
				numToken++;
			/***later check if all documents here have the entity**/
			//later remember to check the entity information
		}
		reader.close();
		return numToken;
	}
	
	/**
	 * 
	 * @param prevEntity: have the prefix that "B-" and "I-
	 * @param currBit
	 * @return String[]{entity, new_prevEntity, new_prevLine}
	 */
	private static String[] getCurrEntity(String prevEntity, String prevLine,  String currBit){
		String entity = null;
		String[] infos = new String[3];
		if(!prevEntity.equals("O")){
			if(currBit.equals("*")){
				if(prevLine.endsWith(")")) entity = "O";
				else if(prevLine.endsWith("*")) entity = "I-"+prevEntity.substring(2);
				else throw new RuntimeException("unknow situation. prevEntity not O, current line: *");
			}else if(currBit.startsWith("(")){
				entity = "B-"+currBit.substring(1, currBit.length()-1);
			}else if(currBit.equals("*)")){
				entity = "I-"+prevEntity.substring(2);
			}else
				throw new RuntimeException("Unknown Situtation: prevEntity not O, current not*, not starts with (, not eqaly *): "+currBit);
		}else{
			//prevEntity == 'O'
			if(currBit.equals("*")) entity = "O";
			else if(currBit.startsWith("(")) entity = "B-"+currBit.substring(1, currBit.length()-1);
			else throw new RuntimeException("Unknow Situation: prevEntity=O, curr not *, not starts with (:"+currBit);
		}
		if(!entity.equals("O") && !validSet.contains(entity.substring(2))) throw new RuntimeException("unknow entity type:"+entity);
		prevLine = currBit;
		prevEntity = entity;
		infos[0] = entity;
		infos[1] = prevEntity;
		infos[2] = prevLine;
		return infos;
	}
	
	
	/**
	 * Write the sentence to files and save as conllx format
	 * @param datasetName: news type
	 * @param sents: the sentences read from original file
	 * @param fileType: train/test/development
	 * @throws IOException
	 */
	private static void printConll(String outputPrefix, String datasetName, ArrayList<Sentence> sents, String fileType) throws IOException{
		if(fileType.equals("development")) fileType = "dev";
		if(fileType.equals("conll-2012-test")) fileType = "test";
		File directory = new File(outputPrefix+"/"+datasetName);
	    if (! directory.exists()){
	        directory.mkdir();
	    }
		PrintWriter pw = RAWF.writer(outputPrefix+"/"+datasetName+"/"+fileType+".conllx");
		System.err.println("dataset:"+datasetName+" type:"+fileType+" size:"+sents.size());
		int num_entities = 0;
		int num_tokens = 0;
		for(Sentence sent: sents){
			num_tokens += sent.length() - 1;
			for(int i=1; i<sent.length(); i++) {
				pw.write(i+"\t"+sent.get(i).getName()+"\t_\t"+sent.get(i).getTag()+"\t"+sent.get(i).getTag()+"\t_\t"+sent.get(i).getHeadIndex()+"\t"+sent.get(i).getDepLabel()+"\t_\t_\t"+sent.get(i).getEntity()+"\n");
				if (sent.get(i).getEntity().startsWith("B-")) {
					num_entities ++;
				}
			}
			pw.write("\n");
		}
		
		System.err.println("num tokens: "+ num_tokens + " num_entities: " + num_entities);
		System.err.println();
		pw.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException{
		process();
	}
	
}
