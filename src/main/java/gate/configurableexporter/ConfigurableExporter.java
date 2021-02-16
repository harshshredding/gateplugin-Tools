/*
 *  ConfigurableExporter.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *  
 *  Genevieve Gorrell, 23 Jul 2012
 *  
 *  $Id: ConfigurableExporter.java 19751 2016-11-18 09:04:17Z markagreenwood $
 */

package gate.configurableexporter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.ResourceReference;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;




/**
 *  Configurable Exporter takes a configuration file specifying the
 *  format of the output file. The configuration file consists of a
 *  single line specifying output format with annotation names 
 *  surrounded by curly braces. E.g.<br>
 *  
 *  <pre>
 *  {Index}, {Class}, "{Content}"
 *  </pre>
 *  
 *  might result in an output file something like
 *  <pre>
 *  10000004, A, "Some text .."
 *  10000005, A, "Some more text .."
 *  10000006, B, "Further text .."
 *  10000007, B, "Additional text .."
 *  10000008, B, "Yet more text .."
 *  </pre>
 *  Annotation features can also be specified using dot notation,
 *  for example;
 *  <pre>
 *  {Index}, {Instance.class}, "{Content}"
 *  </pre>
 *  The PR is useful for outputting data for use in machine learning,
 *  and so each line is considered an "instance". Instance is specified
 *  at run time, and by default is a document, but might be an
 *  annotation type. Instances are output one per line and the config
 *  file specifies how to output each instance. Annotations included
 *  in the output file are the first incidence of the specified type in
 *  the instance. If there is ever a need for it I might fix it so you
 *  can output more than one incidence of the same annotation type.
 *
 */
@CreoleResource(name = "Configurable Exporter", 
  comment = "Allows annotations to be exported according to a specified format.")
public class ConfigurableExporter extends AbstractLanguageAnalyser {

  /**
   * 
   */
  private static final long serialVersionUID = -7237223509897088067L;

  /**
   * The ConfigurableExporter configuration file.
   * 
   */
  private ResourceReference configFileURL;

  /**
   * The file to export the data to.
   * 
   */
  private URL outputURL;

  /**
   * The annotation set from which to draw the annotations.
   * 
   */
  private String inputASName;

  /**
   * The annotation type to be treated as instance.
   * 
   */
  private String instanceName;
  
  private ArrayList<String> annsToInsert = new ArrayList<String>();
  private ArrayList<String> bridges = new ArrayList<String>();


  private PrintStream outputStream = System.out;

  @CreoleParameter(comment = "The configuration file specifying output format.",
      defaultValue="resources/configurableexporter/example.conf", 
      suffixes=".conf")
  public void setConfigFileURL(ResourceReference configFileURL) {
    this.configFileURL = configFileURL;
  }

  @Deprecated
  public void setConfigFileURL(URL configFileURL) {
    try {
      this.setConfigFileURL(new ResourceReference(configFileURL));
    } catch (URISyntaxException e) {
      throw new RuntimeException("Error converting URL to ResourceReference", e);
    }
  }
  
  public ResourceReference getConfigFileURL() {
    return configFileURL;
  }


  @RunTime
  @Optional
  @CreoleParameter(comment = "The file to which data will be output. Leave " +
  		"blank for output to messages tab or standard out.")
  public void setOutputURL(java.net.URL output) {
    this.outputURL = output;
    outputStream = System.out;
    if(outputURL!=null){
    	try {
    		outputStream = new PrintStream(this.outputURL.getFile());
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
  }

  public URL getOutputURL() {
    return this.outputURL;
  }


  @RunTime
  @Optional
  @CreoleParameter(comment = "The name for annotation set used as input to " +
  		"the exporter.")
  public void setInputASName(String iasn) {
    this.inputASName = iasn;
  }

  public String getInputASName() {
    return this.inputASName;
  }

  
  @RunTime
  @Optional
  @CreoleParameter(comment = "The annotation type to be treated as instance. " +
  		"Leave blank to use document as instance.")
  public void setInstanceName(String inst) {
    this.instanceName = inst;
  }

  public String getInstanceName() {
    return this.instanceName;
  }

  @Override
  public Resource init() throws ResourceInstantiationException {
    this.annsToInsert = new ArrayList<String>();
    this.bridges = new ArrayList<String>();

    if(configFileURL == null) throw new IllegalArgumentException(
        "No value provided for the configFileURL parameter");
    
    String strLine = null;
    try (BufferedReader in =
          new BufferedReader(new InputStreamReader(configFileURL.openStream()))){
      if((strLine = in.readLine()) != null) {
        int upto = 0;
        while(upto < strLine.length()) {
          int startAnnoName = strLine.indexOf("{", upto);
          int endAnnoName = strLine.indexOf("}", upto);
          if(startAnnoName == -1 && endAnnoName == -1) {
            bridges.add(strLine.substring(upto, strLine.length()));
            upto = strLine.length();
          } else if((startAnnoName == -1 && endAnnoName != -1)
              || (startAnnoName != -1 && endAnnoName == -1)) {
            throw new ResourceInstantiationException(
                "Failed to parse configuration file for Configurable " +
                "Exporter.");
          } else {
            bridges.add(strLine.substring(upto, startAnnoName));
            this.annsToInsert.add(strLine.substring(startAnnoName + 1, endAnnoName));
            upto = endAnnoName + 1;
          }
        }
      }
    } catch(Exception e) {
      System.out.println("Failed to access configuration file.");
      e.printStackTrace();
    }
    return this;
  }

  @Override
  public void execute() throws ExecutionException {
    Document doc = getDocument();

    // Get the output annotation set
    AnnotationSet inputAS = null;
    if(inputASName == null || inputASName.equals("")) {
      inputAS = doc.getAnnotations();
    } else {
      inputAS = doc.getAnnotations(inputASName);
    }

    List<Annotation> instances = null;
    if(instanceName == null || instanceName.equals("")) {
      // There is no instance name so we will create one instance (line) per
      // document.
      // Here, we find the first annotation of the right type for each slot in
      // the config file.
      for(int i = 0; i < this.annsToInsert.size(); i++) {
        this.outputStream.print(this.bridges.get(i));
        // Check the annotation type isn't null
        String[] bits = annsToInsert.get(i).split("\\.", 2);
        String type = bits[0];
        String feature = null;
        if(bits.length>1) feature = bits[1];
        if(type != null) {
          List<Annotation> typedAnnotations = Utils.inDocumentOrder(
              inputAS.get(type));
	 if(typedAnnotations.size() > 0) {
          Annotation annotationToPrint = typedAnnotations.get(0);
          if(feature != null) {
            this.outputStream.print(annotationToPrint.getFeatures().get(
                feature));
          } else {
            // We have no feature to print so we will just print the text
            long startNode = annotationToPrint.getStartNode().getOffset();
            long endNode = annotationToPrint.getEndNode().getOffset();
            String annotationText = "";
            try {
              annotationText =
                  doc.getContent().getContent(startNode, endNode).toString();
            } catch(Exception e) {
              e.printStackTrace();
            }
            this.outputStream.print(annotationText);
          }
	 }
        }
      }
      if(bridges.size() > annsToInsert.size()) {
        this.outputStream.print(this.bridges.get(this.bridges.size()-1));
      }
      this.outputStream.println();
    } else {
      // We have an instance type so we will create one output line per
      // instance.
      instances = Utils.inDocumentOrder(inputAS.get(this.instanceName));
      
      Iterator<Annotation> instanceAnnotationsIterator = instances.iterator();
      while(instanceAnnotationsIterator.hasNext()) {
        Annotation thisInstanceAnnotation = instanceAnnotationsIterator.next();
        long startSearch = thisInstanceAnnotation.getStartNode().getOffset();
        long endSearch = thisInstanceAnnotation.getEndNode().getOffset();

        // Here, we find the first annotation of the right type within the span
        // of the
        // instance annotation for each slot in the config file.
        for(int i = 0; i < this.annsToInsert.size(); i++) {
          this.outputStream.print(this.bridges.get(i));
          String[] bits = annsToInsert.get(i).split("\\.", 2);
          String type = bits[0];
          String feature = null;
          if(bits.length>1) feature = bits[1];
          List<Annotation> typedAnnotations = Utils.inDocumentOrder(
              inputAS.get(type, startSearch, endSearch));
          if(typedAnnotations.size() > 0) {
            Annotation annotationToPrint = typedAnnotations.get(0);
            if(feature != null) {
              this.outputStream.print(annotationToPrint.getFeatures().get(
                 feature));
            } else {
              // We have no feature to print so we will just print the text
              long startNode = annotationToPrint.getStartNode().getOffset();
              long endNode = annotationToPrint.getEndNode().getOffset();
              String annotationText = "";
              try {
                annotationText =
                    doc.getContent().getContent(startNode, endNode).toString();
              } catch(Exception e) {
                e.printStackTrace();
              }
              this.outputStream.print(annotationText);
            }            
          }
        }
        if(bridges.size() > annsToInsert.size()) {
          this.outputStream.print(this.bridges.get(this.bridges.size()-1));
        }
        this.outputStream.println();
      }
    }

  }
  

  @Override
  public synchronized void interrupt() {
    super.interrupt();
  }

}
