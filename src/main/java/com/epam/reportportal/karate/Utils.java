/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epam.reportportal.karate;

import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.intuit.karate.core.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.reactivex.Maybe;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author kvijayapandian
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final String TABLE_INDENT = "          ";
    private static final String TABLE_SEPARATOR = "|";
    private static final String DOCSTRING_DECORATOR = "\n\"\"\"\n";
    private static final String PASSED = "passed";
    private static final String SKIPPED = "skipped";
    private static final String INFO = "INFO";
    private static final String WARN = "WARN";
    private static final String ERROR = "ERROR";
    private static final String EMPTY = "";
    private static final String ONE_SPACE = " ";
    private static final String HOOK_ = "Hook: ";
    private static final String NEW_LINE = "\r\n";

    private static final String DEFINITION_MATCH_FIELD_NAME = "definitionMatch";
    private static final String STEP_DEFINITION_FIELD_NAME = "stepDefinition";
    private static final String GET_LOCATION_METHOD_NAME = "getLocation";
    private static final String METHOD_OPENING_BRACKET = "(";

    static void printFeatureResult(FeatureResult featureResult) {
        
        try{
             System.out.println(" **** FEATURE RESULT *************************** ");

       
        System.out.println("\t\tcallName=" + featureResult.getCallName());
         System.out.println("\t\tFailedCount=" + featureResult.getFailedCount());
        System.out.println("\t\tScenarioCount=" + featureResult.getScenarioCount());
        System.out.println("\t\tScenarioResults=" +featureResult.getScenarioResults());
        System.out.println("\t\tStepResults=" + featureResult.getStepResults());
        System.out.println(" **** END FEATURE RESULT  *************************** ");
             System.out.println("\t\tResults=" + featureResult.getResults());
   
        }catch (Exception ex){
        ex.printStackTrace();

        }
                  
        
       }

    private Utils() {
        throw new AssertionError("No instances should exist for the class!");
    }
    
        static void finishTestItem(Launch rp, Maybe<String> itemId) {
        finishTestItem(rp, itemId, null);
    }

    static Date finishTestItem(Launch rp, Maybe<String> itemId, String status) {
        if (itemId == null) {
            LOGGER.error("BUG: Trying to finish unspecified test item.");
            return null;
        }
        FinishTestItemRQ rq = new FinishTestItemRQ();
        Date endTime = Calendar.getInstance().getTime();
        rq.setEndTime(endTime);
        rq.setStatus(status);
        rp.finishTestItem(itemId, rq);
        return endTime;
    }
    

    /**
     * Generate name representation
     *
     * @param prefix - substring to be prepended at the beginning (optional)
     * @param infix - substring to be inserted between keyword and name
     * @param argument - main text to process
     * @param suffix - substring to be appended at the end (optional)
     * @return transformed string
     */
    //TODO: pass Node as argument, not test event
    static String buildNodeName(String prefix, String infix, String argument, String suffix) {
        return buildName(prefix, infix, argument, suffix);
    }

    private static String buildName(String prefix, String infix, String argument, String suffix) {
        return (prefix == null ? EMPTY : prefix) + infix + argument + (suffix == null ? EMPTY : suffix);
    }

    /**
     * Transform tags from Cucumber to RP format
     *
     * @param tags - Cucumber tags
     * @return set of tags
     */
    public static Set<ItemAttributesRQ> extractAttributes(List<Tag> tags) {
        Set<ItemAttributesRQ> attributes = new HashSet<ItemAttributesRQ>();
        for (Tag tag : tags) {
            attributes.add(new ItemAttributesRQ(null, tag.getName()));
        }
        return attributes;
    }

    public static void printExecutionContext(ExecutionContext executionContext) {
        System.out.println(" **** FEATURE EXECUTION CONTEXT *************************** ");
        System.out.println("\t\tparentPath=" + executionContext.featureContext.parentPath);
    
        System.out.println(" **** END FEATURE EXECUTION CONTEXT *************************** ");
    }

    public static void printFeature(Feature feature) {
        System.out.println(" **** FEATURE *************************** ");
        System.out.println("\t\tname=" + feature.getName());
        System.out.println("\t\tdescription=" + feature.getDescription());
        System.out.println("\t\tcallTag=" + feature.getCallTag());
         System.out.println("\t\tcallLine=" + feature.getCallLine());
        System.out.println("\t\tnameForReport=" + feature.getNameForReport());
        System.out.println("\t\tresource=" + feature.getResource());
        System.out.println("\t\ttags=" + feature.getTags());
        System.out.println("\t\tisBackgroundPresent=" + feature.isBackgroundPresent());
        System.out.println(" **** END FEATURE *************************** ");

    }
    
       public static void printScenario(Scenario scenario) {
        System.out.println(" **** SCENARIO *************************** ");
        System.out.println("\t\tName=" + scenario.getName());
        System.out.println("\t\tKeyword=" + scenario.getKeyword());
        System.out.println("\t\tUniqueId=" + scenario.getUniqueId());
         System.out.println("\t\tDescription=" + scenario.getDescription());
        System.out.println("\t\tIndex=" + scenario.getIndex());
        System.out.println(" **** END SCENARIO *************************** ");

    }
}
