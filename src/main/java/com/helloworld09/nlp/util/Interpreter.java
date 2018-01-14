package com.helloworld09.nlp.util;

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.international.Language;
import edu.stanford.nlp.util.CoreMap;
import org.apache.log4j.BasicConfigurator;


public class Interpreter {

    public static AbstractMap<String, List<SemanticGraphEdge>> filterRelnsByDep(SemanticGraph dependencies, GrammaticalRelation[] filters) {
        HashMap<String, List<SemanticGraphEdge>> filteredRelationMap = new HashMap<>();
        for (GrammaticalRelation filter: filters) {

            List<SemanticGraphEdge> relns = dependencies.findAllRelns(filter);
            filteredRelationMap.put(filter.getShortName(), relns);
        }
        return filteredRelationMap;
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String property = "tokenize, ssplit, pos, lemma, ner, parse, dcoref";
        Pipeline ppl = new Pipeline(property);

        String text = "Lucy is in the sky with diamonds.";
        Annotation document = ppl.annotate(text);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class);
            GrammaticalRelation[] filters = {
                    new GrammaticalRelation(Language.Any, "nsubj", "Subject", null),
                    new GrammaticalRelation(Language.Any, "dobj", "Object", null),
            };
            System.out.println(filterRelnsByDep(dependencies, filters));
        }
    }
}
