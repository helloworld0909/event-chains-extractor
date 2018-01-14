package com.helloworld09.nlp.util;

import java.io.*;
import java.util.*;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DOMParser {
    private DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

    public Document parse(String filePath) {
        Document document = null;
        try {
            //DOM parser instance
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            //parse an XML file into a DOM tree
            document = builder.parse(new File(filePath));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return document;
    }

    public static void main(String[] args) {
        final String INPUT_PATH = "data/";

        DOMParser parser = new DOMParser();

    }
}
