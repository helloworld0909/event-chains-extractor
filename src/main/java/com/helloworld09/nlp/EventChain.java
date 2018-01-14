package com.helloworld09.nlp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.FileWriter;

import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.international.Language;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.Pair;

import edu.stanford.nlp.util.Quadruple;
import org.apache.log4j.BasicConfigurator;
import org.json.JSONObject;
import org.json.JSONArray;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;

import com.helloworld09.nlp.util.Pipeline;
import com.helloworld09.nlp.util.Interpreter;


public class EventChain {

    private static Logger logger;
    private Pipeline pipeline;

    public EventChain(String property) {

        logger = Logger.getLogger(EventChain.class);
        pipeline = new Pipeline(property);
    }

    public EventChain(Properties property) {

        logger = Logger.getLogger(EventChain.class);
        pipeline = new Pipeline(property);
    }

    public void buildEventChain(String filename, GrammaticalRelation[] filters, String inputDir, String outputEventDir, String outputDetailEventDir) {

        try {
            String content = readFile(inputDir + filename, StandardCharsets.UTF_8);
            JSONArray obj = new JSONArray(content);
            FileWriter outputFile = new FileWriter(outputEventDir + filename.split("\\.")[0] + ".txt");
            FileWriter outputFileDetail = new FileWriter(outputDetailEventDir + filename.split("\\.")[0] + ".txt");

            for (int i = 0; i < obj.length(); i++) {
                JSONObject docObj = obj.getJSONObject(i);
                String docID = docObj.getString("id");

                if (!docObj.isNull("paragraphs")) {
                    JSONArray paragraphs = docObj.getJSONArray("paragraphs");
                    StringBuilder docBuilder = new StringBuilder();
                    for (Object p : paragraphs) {
                        p = StringUtils.trim((String) p);
                        docBuilder.append(p);
                        docBuilder.append('\n');
                    }
                    String text = docBuilder.toString();
                    Pair<List<List<Event>>, List<List<Event>>> twoEventChains = extractEventChains(text, filters);
                    List<List<Event>> eventChains = twoEventChains.first();
                    List<List<Event>> detailEventChains = twoEventChains.second();

                    int chainIdx1 = 0;
                    int chainIdx2 = 0;
                    for (List<Event> chain : eventChains) {
                        outputFile.write(docID + "\t" + chainIdx1 + "\n");
                        for (Event e : chain) {
                            outputFile.write(e.toString(true) + "\n");
                        }
                        outputFile.write("\n");
                        chainIdx1++;
                    }
                    outputFile.flush();

                    for (List<Event> chain : detailEventChains) {
                        outputFileDetail.write(docID + "\t" + chainIdx2 + "\n");
                        for (Event e : chain) {
                            outputFileDetail.write(e.toString() + "\n");
                        }
                        outputFileDetail.write("\n");
                        chainIdx2++;
                    }
                    outputFileDetail.flush();
                }
            }
            outputFile.close();
            outputFileDetail.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            logger.info("Finish building event train on " + filename);
        }
    }

    private void getEdgeMaps(List<CoreMap> sentences,
                             GrammaticalRelation[] filters,
                             Map<IntPair, List<SemanticGraphEdge>> edgeMapByVerbPosition,
                             Map<IntPair, List<SemanticGraphEdge>> edgeMapByProtaPosition) {

        // Indexed from 1
        for (int sentNum = 1; sentNum <= sentences.size(); sentNum++) {
            CoreMap sentence = sentences.get(sentNum - 1);
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
            Map<String, List<SemanticGraphEdge>> relations = Interpreter.filterRelnsByDep(dependencies, filters);
            for (GrammaticalRelation filter : filters) {
                String shortName = filter.getShortName();
                for (SemanticGraphEdge edge : relations.get(shortName)) {
                    Pair<IndexedWord, IndexedWord> protaAndVerb = getNounAndVerb(edge);
                    IndexedWord protagonist = protaAndVerb.first();
                    IndexedWord verb = protaAndVerb.second();

                    IntPair verbPosition = new IntPair(sentNum, verb.index());
                    IntPair protagonistPosition = new IntPair(sentNum, protagonist.index());

                    edgeMapByVerbPosition.putIfAbsent(verbPosition, new LinkedList<>());
                    edgeMapByProtaPosition.putIfAbsent(protagonistPosition, new LinkedList<>());
                    edgeMapByVerbPosition.get(verbPosition).add(edge);
                    edgeMapByProtaPosition.get(protagonistPosition).add(edge);

                }
            }
        }
    }

    private Pair<List<List<Event>>, List<List<Event>>> extractEventChains(String text, GrammaticalRelation[] filters) {

        Map<Integer, List<Event>> eventChainsMap = new HashMap<>();
        Map<Integer, List<Event>> detailEventChainsMap = new HashMap<>();

        Annotation document = pipeline.annotate(text);

        Map<Integer, CorefChain> graph = document.get(CorefCoreAnnotations.CorefChainAnnotation.class);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        Map<IntPair, List<SemanticGraphEdge>> edgeMapByVerbPosition = new LinkedHashMap<>();
        Map<IntPair, List<SemanticGraphEdge>> edgeMapByProtaPosition = new LinkedHashMap<>();
        getEdgeMaps(sentences, filters, edgeMapByVerbPosition, edgeMapByProtaPosition);

        for (CorefChain chain : graph.values()) {
            int chainID = chain.getChainID();
            for (CorefChain.CorefMention mention : chain.getMentionsInTextualOrder()) {
                IntPair mentionPosition = new IntPair();
                int sentNum = mention.sentNum;
                mentionPosition.set(0, sentNum);
                mentionPosition.set(1, mention.headIndex);
                List<SemanticGraphEdge> protaRelatedEdges = edgeMapByProtaPosition.get(mentionPosition);
                if (protaRelatedEdges == null)
                    continue;
                for (SemanticGraphEdge protaRelatedEdge : protaRelatedEdges) {
                    Pair<IndexedWord, IndexedWord> protaAndVerb = getNounAndVerb(protaRelatedEdge);
                    IndexedWord protagonist = protaAndVerb.first();
                    IndexedWord verb = protaAndVerb.second();

                    String chainRelationName = protaRelatedEdge.getRelation().getShortName();
                    String chainRelationStr = Event.convertRelationToString(chainRelationName);

                    // edge from protagonist should be either nsubj or dobj, cannot be other
                    if (!chainRelationStr.equals("SUBJ") & !chainRelationStr.equals("OBJ")) {
                        break;
                    }

                    // Add simple event to result chains
                    Event event = new Event(verb, protagonist, chainRelationName);
                    eventChainsMap.putIfAbsent(chainID, new ArrayList<>());
                    eventChainsMap.get(chainID).add(event);

                    // Add additional information
                    IndexedWord subject = null, object = null, prepositionalEntity = null;
                    switch (Event.convertRelationToString(chainRelationName)) {
                        case "SUBJ": {
                            subject = protagonist;
                            break;
                        }
                        case "OBJ": {
                            object = protagonist;
                            break;
                        }
                    }

                    IntPair verbPosition = new IntPair(sentNum, verb.index());
                    List<SemanticGraphEdge> verbRelatedEdges = edgeMapByVerbPosition.get(verbPosition);


                    if (verbRelatedEdges.size() > 1) {
                        for (SemanticGraphEdge relatedEdge : verbRelatedEdges) {
                            String relationName = relatedEdge.getRelation().getShortName();

                            IndexedWord indexedWord = getNounAndVerb(relatedEdge).first();

                            if (!indexedWord.equals(protagonist)) {
                                if (!relationName.equals(chainRelationName)) {
                                    switch (Event.convertRelationToString(relationName)) {
                                        case "SUBJ": {
                                            subject = indexedWord;
                                            break;
                                        }
                                        case "OBJ": {
                                            object = indexedWord;
                                            break;
                                        }
                                        case "IOBJ": {
                                            prepositionalEntity = indexedWord;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Quadruple<IndexedWord, IndexedWord, IndexedWord, IndexedWord> params = new Quadruple<>(verb, subject, object, prepositionalEntity);
                    Event detailEvent = new ComplexEvent(params, chainRelationName);
                    detailEventChainsMap.putIfAbsent(chainID, new ArrayList<>());
                    detailEventChainsMap.get(chainID).add(detailEvent);
                }
            }
        }

        logger.debug("Finish extracting event chain");
        Pair<List<List<Event>>, List<List<Event>>> ret = new Pair<>();
        ret.setFirst(new ArrayList<>(eventChainsMap.values()));
        ret.setSecond(new ArrayList<>(detailEventChainsMap.values()));
        return ret;
    }

    private Pair<IndexedWord, IndexedWord> getNounAndVerb(SemanticGraphEdge edge) {
        if (!edge.getSource().tag().startsWith("V")) {
            return new Pair<>(edge.getSource(), edge.getTarget());
        } else {
            return new Pair<>(edge.getTarget(), edge.getSource());
        }
    }

    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, mention, coref");
        props.put("threads", "4");
        EventChain eventChainBuilder = new EventChain(props);
        GrammaticalRelation[] filters = {
                new GrammaticalRelation(Language.Any, "nsubj", "Subject", null),
                new GrammaticalRelation(Language.Any, "dobj", "Object", null),
                new GrammaticalRelation(Language.Any, "nsubjpass", "SubjectPass", null),
                new GrammaticalRelation(Language.Any, "iobj", "IndirectObject", null),
        };

        String inputDir = "data/";
        String outputEventDir = "output/event/";
        String outputDetailEventDir = "output/event_detail/";
        File inputDirFile = new File(inputDir);
        if (inputDirFile.exists()) {
            String fileNameList[] = inputDirFile.list();
            if (fileNameList != null) {
                Arrays.sort(fileNameList);
                for (String fileName : fileNameList) {
                    if (fileName.endsWith(".json")) {
                        eventChainBuilder.buildEventChain(fileName, filters, inputDir, outputEventDir, outputDetailEventDir);
                    }
                }
            }
            else
                logger.warn("Empty input dir");
        } else
            logger.error("Input dir path not found");
    }
}
