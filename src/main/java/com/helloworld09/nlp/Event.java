package com.helloworld09.nlp;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.util.Quadruple;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;

enum EventRelation {
    SUBJ, OBJ, IOBJ, OTHER
}

class Event implements Serializable {
    private IndexedWord verb;
    private IndexedWord protagonist;

    protected EventRelation relation;

    public Event() {
    }

    public Event(IndexedWord verb, IndexedWord protagonist, String reln) {
        this.verb = verb;
        this.protagonist = protagonist;
        this.relation = convertRelation(reln);
    }

    public Event(IndexedWord verb, IndexedWord protagonist, GrammaticalRelation reln) {
        this.verb = verb;
        this.protagonist = protagonist;
        this.relation = convertRelation(reln.getShortName());
    }

    public static EventRelation convertRelation(String relation) {
        EventRelation reln;

        if (relation.contains("subj")) {
            reln = EventRelation.SUBJ;
        } else if (relation.equals("dobj")) {
            reln = EventRelation.OBJ;
        } else if (relation.equals("iobj")) {
            reln = EventRelation.IOBJ;
        } else {
            Logger logger = Logger.getLogger(Event.class);
            logger.error("Error event construction! relation = " + relation);
            reln = EventRelation.OTHER;
        }
        return reln;
    }

    public static EventRelation convertRelation(SemanticGraphEdge relation){
        return convertRelation(relation.getRelation().getShortName());
    }

    public static String convertRelationToString(String relation){
        return convertRelation(relation).toString();
    }

    public static String convertRelationToString(SemanticGraphEdge relation){
        return convertRelation(relation).toString();
    }

    @Override
    public String toString() {
        return verb.value() + "\t" + protagonist.value() + "\t" + StringUtils.lowerCase(relation.toString());
    }

    public String toString(boolean lemma) {
        if (lemma)
            return verb.value() + "\t" + verb.get(CoreAnnotations.LemmaAnnotation.class) + "\t" + protagonist.value() + "\t" + StringUtils.lowerCase(relation.toString());
        else
            return toString();
    }

}

class ComplexEvent extends Event implements Serializable {
    private IndexedWord verb;
    private IndexedWord subject;
    private IndexedWord object;
    private IndexedWord preposition;

    public ComplexEvent(Quadruple<IndexedWord, IndexedWord, IndexedWord, IndexedWord> objects, String reln) {
        verb = objects.first();
        subject = objects.second();
        object = objects.third();
        preposition = objects.fourth();
        relation = convertRelation(reln);
    }

    @Override
    public String toString() {
        ArrayList<String> sb = new ArrayList<>();
        sb.add(verb.value());
        sb.add(String.valueOf(verb.get(CoreAnnotations.LemmaAnnotation.class)));
        sb.add(String.valueOf(subject));
        sb.add(String.valueOf(object));
        sb.add(String.valueOf(preposition));
        sb.add(StringUtils.lowerCase(relation.toString()));
        return String.join("\t", sb);
    }

}
