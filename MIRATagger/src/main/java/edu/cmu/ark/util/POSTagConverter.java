package edu.cmu.ark.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.ark.DiscriminativeTagger;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class POSTagConverter {

    /**
     * @param args
     */
    public static void main(String[] args) {

        String inputFile = args[0];

        String outfile = args[1];

        try {
            MaxentTagger tagger = new MaxentTagger(DiscriminativeTagger.getProperties().getProperty("posTaggerModel"));

            PrintWriter pw = new PrintWriter(new FileOutputStream(outfile));
            String buf;
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            String tagged = "";
            List<String> toks = new ArrayList<String>();
            List<String> labels = new ArrayList<String>();

            while (true) {

                buf = br.readLine();
                if (buf == null || !buf.contains("\t")) {
                    String s = "";
                    for (String tok : toks) {
                        s += tok + " ";
                    }
                    if (s.length() == 0) {
                        break;
                    }
                    // String tagged = MaxentTagger.tagString(s);

                    try {
                        tagged = tagger.tagTokenizedString(s);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    String[] taggedTokens = tagged.split("\\s");
                    for (int i = 0; i < taggedTokens.length; i++) {
                        int idx = taggedTokens[i].lastIndexOf('_');
                        String pos = taggedTokens[i].substring(idx + 1);

                        pw.println(toks.get(i) + "\t" + pos + "\t" + labels.get(i));
                    }
                    pw.println();

                    toks.clear();
                    labels.clear();
                    pw.flush();
                } else {

                    String[] parts = buf.split("\\t");

                    toks.add(parts[0]);
                    labels.add(parts[2]);
                }

                if (buf == null) {
                    break;
                }

            }

            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
