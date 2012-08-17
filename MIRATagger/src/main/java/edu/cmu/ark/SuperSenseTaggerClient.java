package edu.cmu.ark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class SuperSenseTaggerClient {

    public static void main(String[] args) {

        int port = 5555;

        String host = "127.0.0.1";
        String propertiesFile = "tagger.properties";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port")) {
                port = new Integer(args[i + 1]);
                i++;
            } else if (args[i].equals("--properties")) {
                propertiesFile = args[i + 1];
                i++;
            }
        }

        Properties properties = DiscriminativeTagger.loadProperties(propertiesFile);

        try {
            MaxentTagger tagger = new MaxentTagger(properties.getProperty("posTaggerModel"));

            String buf;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            String doc;
            while (true) {
                doc = "";
                buf = "";

                buf = br.readLine();
                if (buf == null) {
                    break;
                }
                doc += buf;

                while (br.ready()) {
                    buf = br.readLine();
                    if (buf == null) {
                        break;
                    }
                    doc += buf + " ";
                }

                List<String> sentences = DiscriminativeTagger.getSentences(doc);
                for (String s : sentences) {
                    String input = "";
                    String tagged = tagger.tagString(s);
                    String[] taggedTokens = tagged.split("\\s");
                    int idx;
                    for (int i = 0; i < taggedTokens.length; i++) {
                        idx = taggedTokens[i].lastIndexOf('_');
                        String token = taggedTokens[i].substring(0, idx);
                        String pos = taggedTokens[i].substring(idx + 1);
                        input += token + "\t" + pos + "\n";
                    }

                    String res = "";

                    Socket client;
                    PrintWriter pw;
                    BufferedReader br2;
                    String line;

                    try {
                        client = new Socket(host, port);

                        pw = new PrintWriter(client.getOutputStream());
                        br2 = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        pw.println(input);
                        pw.flush(); // flush to complete the transmission

                        System.err.println("sent:\n" + input);
                        while ((line = br2.readLine()) != null) {
                            res += line + "\n";
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    System.out.println(res);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
