package data.preprocess.conll2003;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

/**
 * Preprocess the CoNLL 2003 dataset.
 * 
 * @author allanjie
 *
 */
public class Processor {
	
	/**
	 * Read the sentence from the raw data. we ignore the chunk information from data
	 * @param inputFile
	 * @return
	 */
	private static List<Sentence> readSentences(String inputFile) {
		List<Sentence> sents = new ArrayList<>();
		BufferedReader br = null;
		String line = null;
		List<WordToken> wts = new ArrayList<>();
		try {
			br = RAWF.reader(inputFile);
			while ((line = br.readLine()) != null) {
				String[] vals = line.split(" ");
				if (line.equals("") && wts.size() != 0) {
					WordToken[] wtArr = new WordToken[wts.size()];
					wts.toArray(wtArr);
					Sentence sent = new Sentence(wtArr);
					sents.add(sent);
					wts = new ArrayList<>();
					continue;
				}
				if (vals[0].equals("-DOCSTART-") || line.equals("")) {
					continue;
				}
				String word = vals[0];
				String POS = vals[1];
				String entity = vals[3];
				WordToken wt = new WordToken(word, POS, -1, entity);
				wts.add(wt);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sents;
	}
	
	/**
	 * Export all the CoNLL 2003 sentence into CoNLL-X format for DGM model to run.
	 * Or for dependency parser to parse the sentence.
	 * 
	 * @param sents
	 */
	private static void output2CoNLLXFormat(List<Sentence> sents, String outputFile) {
		PrintWriter pw = null;
		try {
			pw = RAWF.writer(outputFile);
			for (Sentence sent : sents) {
				for (int p = 0; p < sent.length(); p++) {
					WordToken wt = sent.get(p);
					int id = p + 1;
					String word = wt.getName();
					String lemma = "_";
					String CPOS = wt.getTag();
					String POS = wt.getTag();
					String feats = "_";
					int head = -1; //dependency information is not available
					String deprel = "_";
					String phead = "_";
					String pdeprel = "_";
					String entity = wt.getEntity();
					pw.println(id + "\t" + word + "\t" + lemma + "\t" + CPOS + "\t" + POS + "\t" + feats + "\t" + head + "\t" + deprel + "\t" + phead + "\t" + pdeprel + "\t" + entity);
				}
				pw.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.close();
	}

	/**
	 * Process the CoNLL 2003 file into IOB encoding
	 * @param inputFile
	 * @param outputFile: with IOB encoding
	 */
	public static void processCoNLL2003(String inputFile, String outputFile) {
		List<Sentence> sents = readSentences(inputFile);
		for (Sentence sent : sents) {
			String prevEntity = "O";
			for (int p = 0; p < sent.length(); p++) {
				String currEntity = null;
				String rawCurrEntity = sent.get(p).getEntity();
				if (rawCurrEntity.equals("O"))
					currEntity = "O";
				else {
					if (prevEntity.equals("O"))
						currEntity = "B-" + rawCurrEntity.substring(2);
					else if (prevEntity.substring(2).equals(rawCurrEntity.substring(2))) {
						if (prevEntity.equals(rawCurrEntity)) {
							currEntity = prevEntity;
						} else {
							assert !prevEntity.equals("O");
							if (prevEntity.startsWith("B-"))
								currEntity = "I-" + rawCurrEntity.substring(2);
							else
								currEntity = "B-" + rawCurrEntity.substring(2);
						}
					} else {
						currEntity = "B-" + rawCurrEntity.substring(2);
					}
				}
				sent.get(p).setEntity(currEntity);
				prevEntity = currEntity;
			}
		}
		output2CoNLLXFormat(sents, outputFile);
	}
	
	public static void outputTextFileOnly(List<Sentence> sents, String outputFile) {
		PrintWriter pw = null;
		try {
			pw = RAWF.writer(outputFile);
			for (Sentence sent : sents) {
				for (int p = 0; p < sent.length(); p++) {
					WordToken wt = sent.get(p);
					String word = wt.getName();
					String space = p == 0? "" : " ";
					pw.print(space + word);
				}
				pw.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		pw.close();
	}
	
	public static void main(String... args) {
		processCoNLL2003("data/conll2003/eng.train", "data/conll2003/eng.train.iob");
		processCoNLL2003("data/conll2003/eng.testa", "data/conll2003/eng.testa.iob");
		processCoNLL2003("data/conll2003/eng.testb", "data/conll2003/eng.testb.iob");
	}
}
