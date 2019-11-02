/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.epam.reportportal.karate;

import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.intuit.karate.core.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Strings;

/**
 *
 * @author kvijayapandian
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    private Utils() {
        throw new AssertionError("No instances should exist for the class!");
    }

    static void sendLog(final String itemUuid1, final String message, final String level, final SaveLogRQ.File file) {

        if (Strings.isNullOrEmpty(message)) {
            return;
        }
        ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
            @Override
            public SaveLogRQ apply(String itemUuid) {
                SaveLogRQ rq = new SaveLogRQ();
                rq.setMessage(message);
                rq.setUuid(itemUuid);
                rq.setItemUuid(itemUuid1);
                rq.setLevel(level);
                rq.setLogTime(Calendar.getInstance().getTime());
                if (file != null) {
                    rq.setFile(file);
                }
                return rq;
            }
        });
    }

    public static Set<ItemAttributesRQ> extractAttributes(List<Tag> tags) {
        Set<ItemAttributesRQ> attributes = new HashSet<ItemAttributesRQ>();
        tags.forEach((tag) -> {
            attributes.add(new ItemAttributesRQ(null, tag.getName()));
        });
        return attributes;
    }

    static String printStepResult(StepResult stepResult) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("\n\t\t\t[");
            sb.append("\n\t\t\terrorMessage=").append(stepResult.getErrorMessage());
            sb.append("\n\t\t\tstepLog=" + stepResult.getStepLog());
            sb.append("\n\t\t\tisFailed=" + stepResult.getResult().isFailed());
            sb.append("\n\t\t\tstatus=").append(stepResult.getResult().getStatus());
            //  sb.append(" stepText" + stepResult.getStep().getText());
            sb.append("\n\t\t\tstepdocString=").append(stepResult.getStep().getDocString());
            sb.append("\n\t\t\t]");
        } catch (Exception ex) {
            LOGGER.error("Exception wile printing step result", ex);
        }
        return sb.toString();
    }

    static String getURI(Feature feature) {
        return feature.getResource().getPath().toString();
    }

    static String printScenarioResult(ScenarioResult scenarioResult) {
        StringBuilder sb = new StringBuilder();
        try {

            sb.append("\n\t\t[");
            sb.append("\n\t\tstartTime=" + scenarioResult.getStartTime());
            sb.append("\n\t\tendTime=" + scenarioResult.getEndTime());
            sb.append("\n\t\tfailureMessageForDisplay=" + scenarioResult.getFailureMessageForDisplay());
            sb.append("\n\t\tstepResultsSize=" + scenarioResult.getStepResults().size());
            sb.append("\n\t\tscenarioName=" + scenarioResult.getScenario().getName());
            sb.append("\n\t\tscenarioNameForReport=" + scenarioResult.getScenario().getNameForReport());
            sb.append("\n\t\tscenarioKeyword=" + scenarioResult.getScenario().getKeyword());
            sb.append("\n\t\tscenarioDescription=" + scenarioResult.getScenario().getDescription());
            sb.append("\n\t\tstepResults=[");
            for (StepResult stepResult : scenarioResult.getStepResults()) {
                sb.append(Utils.printStepResult(stepResult));
            }
            sb.append("\n\t\t]");

        } catch (Exception ex) {
            LOGGER.error("Exception wile printing scenario result", ex);

        }
        return sb.toString();
    }

    static String printFeatureResult(FeatureResult featureResult) {
        StringBuffer sb = new StringBuffer();
        try {
            sb.append("[");
            sb.append("\n\tcallName=" + featureResult.getCallName());
            sb.append("\n\tscenarioCount=" + featureResult.getScenarioCount());
            sb.append("\n\tfailedCount=" + featureResult.getFailedCount());
            sb.append("\n\tstepResultsSize=" + featureResult.getStepResults().size());
            sb.append("\n\tresults=" + featureResult.getResults());
            sb.append("\n\tscenarioResults=[");
            for (ScenarioResult scenarioResult : featureResult.getScenarioResults()) {
                sb.append(Utils.printScenarioResult(scenarioResult));
            }
            sb.append("\n\t]");
            sb.append("\n]");
        } catch (Exception ex) {
            LOGGER.error("Exception wile printing feature result", ex);

        }
        return sb.toString();

    }

    public static String printFeature(Feature feature) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("[");
            sb.append("\n\tname=" + feature.getName());
            sb.append("\n\tdescription=" + feature.getDescription());
            sb.append("\n\tcallTag=" + feature.getCallTag());
            sb.append("\n\tcallLine=" + feature.getCallLine());
            sb.append("\n\tnameForReport=" + feature.getNameForReport());
            sb.append("\n\tresource=" + feature.getResource());
            sb.append("\n\tags=" + feature.getTags());
            sb.append("\n\tisBackgroundPresent=" + feature.isBackgroundPresent());
            sb.append("\n]");
        } catch (Exception ex) {
            LOGGER.error("Exception wile printing feature", ex);;

        }

        return sb.toString();

    }

}
