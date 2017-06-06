/*
 *  TestFlexibleGazetteer.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Mike Dowman, 25/3/2004
 *
 *  $Id: TestFlexibleGazetteer.java 19219 2016-04-09 17:16:18Z markagreenwood $
 */

package gate.creole.gazetteer;

import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.test.GATEPluginTestCase;

import java.util.ArrayList;
import java.util.List;

public class TestFlexibleGazetteer extends GATEPluginTestCase {

  private static final boolean DEBUG=false;

  /** Fixture tear down - does nothing */
  @Override
  public void tearDown() throws Exception {
  } // tearDown

  /** Tests the flexible gazetteer */
  public void testFlexibleGazetteer() throws Exception {
    //get a document - take it from the gate server.
    // tests/doc0.html is a simple html document.
	  Document doc = Factory.newDocument(this.getClass().getResource("/tests/doc0.html"));

    // Get a tokeniser - just use all the default settings.
    gate.creole.tokeniser.DefaultTokeniser tokeniser=
        (gate.creole.tokeniser.DefaultTokeniser) Factory.createResource(
        "gate.creole.tokeniser.DefaultTokeniser");

    gate.creole.splitter.SentenceSplitter splitter =
        (gate.creole.splitter.SentenceSplitter) Factory.createResource(
        "gate.creole.splitter.SentenceSplitter");

    gate.creole.POSTagger tagger = (gate.creole.POSTagger) Factory.createResource(
        "gate.creole.POSTagger");

    // Get a morphological analyser, again just use all the default settings.
    gate.creole.morph.Morph morphologicalAnalyser=
        (gate.creole.morph.Morph) Factory.createResource(
        "gate.creole.morph.Morph");

    // Get a default gazetteer, again just use all the default settings
    gate.creole.gazetteer.Gazetteer gazetteerInst =
        (gate.creole.gazetteer.DefaultGazetteer) Factory.createResource(
        "gate.creole.gazetteer.DefaultGazetteer");

    //create a flexible gazetteer
    // First create a feature map containing all the relevant parameters.
    FeatureMap params = Factory.newFeatureMap();
    // Create a list of input features with just one feature (root) and add it
    // to the feature map.
    List<String> testInputFeatures=new ArrayList<String>();
    testInputFeatures.add("Token.root");
    params.put("inputFeatureNames", testInputFeatures);
    params.put("gazetteerInst",gazetteerInst);

    // Actually create the gazateer
    FlexibleGazetteer flexGaz = (FlexibleGazetteer) Factory.createResource(
                          "gate.creole.gazetteer.FlexibleGazetteer", params);

    // runtime stuff - set the document to be used with the gazetteer, the
    // tokeniser and the analyser to doc, and run each of them in turn.
    tokeniser.setDocument(doc);
    tokeniser.execute();
    splitter.setDocument(doc);
    splitter.execute();
    tagger.setDocument(doc);
    tagger.execute();
    morphologicalAnalyser.setDocument(doc);
    morphologicalAnalyser.execute();
    flexGaz.setDocument(doc);
    flexGaz.execute();

    // Now check that the document has been annotated as expected.
    // First get the default annotations.
    AnnotationSet defaultAnnotations=doc.getAnnotations();

    // Now just get the lookups out of that set.
    AnnotationSet lookups=defaultAnnotations.get("Lookup");

    // And check that all the correct lookups have been found.
    // N.B. If the default gazetteer lists are ever changed, the correct value
    // for the number of lookups found may also change.

    if (DEBUG) {
      System.out.println("There are this many lookup annotations: "+
                         lookups.size());
    }
    assertEquals("Wrong number of lookup annotations",54,lookups.size());

    // Now clean up so we don't get a memory leak.
    Factory.deleteResource(doc);
    Factory.deleteResource(tokeniser);
    Factory.deleteResource(morphologicalAnalyser);
    Factory.deleteResource(flexGaz);
  }


} // TestFlexibleGazetteer
