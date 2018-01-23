# event-chains-extractor
Implementation of event chains extraction algorithm using Dependency Parsing &amp; Coreference Resolution via CoreNLP

### Input

Gigaword English NY Times (SGML format version)

### Build with Maven

##### Compile

    mvn compile
or

##### Build jar

    mvn package

or

##### Build jar with dependencies

    mvn package assembly:single

### Run

1.  Adjust input path and put input corpus files like this:

    data/nyt_eng_199407

2.  Convert SGML format to JSON

        cd scripts
        python sgml2json.py

3.  Run extraction program

        java -jar finalpj-0.1-XXXXX.jar

    or

        mvn exec:java -Dexec.mainClass="com.helloworld09.nlp.EventChain"

### Reference
1.  Chambers N, Jurafsky D. Unsupervised learning of narrative event chains[C]//ACL. 2008, 94305: 789-797.
2.  Wang Z, Zhang Y, Chang C Y. Integrating Order Information and Event Relation for Script Event Prediction[C]//Proceedings of the 2017 Conference on Empirical Methods in Natural Language Processing. 2017: 57-67.
3.  Granroth-Wilding M, Clark S. What Happens Next? Event Prediction Using a Compositional Neural Network Model[C]//AAAI. 2016: 2727-2733.
