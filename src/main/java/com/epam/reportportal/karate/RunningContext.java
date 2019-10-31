
package com.epam.reportportal.karate;

import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;

import java.util.*;
import com.epam.reportportal.service.Launch;
import com.intuit.karate.Results;
import com.intuit.karate.core.*;
import com.intuit.karate.http.HttpRequestBuilder;
import com.intuit.karate.core.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Supplier;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;

import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ.File;
import io.reactivex.Maybe;

/**
 * Running context that contains mostly manipulations with Gherkin objects.
 * Keeps necessary information regarding current Feature, Scenario and Step
 *
 * @author Serhii Zharskyi
 * @author Vitaliy Tsvihun
 */
class RunningContext {

    private RunningContext() {
        throw new AssertionError("No instances should exist for the class!");
    }

    static class ScenarioContext {

        private Maybe<String> scenarioId;
        private String uri;
        private FeatureContext featureContext;
        private Scenario scenario;

        public Maybe<String> getScenarioId() {
            return scenarioId;
        }

        public String getUri() {
            return uri;
        }

        public FeatureContext getFeatureContext() {
            return featureContext;
        }

        ScenarioContext createScenarioContext(Scenario scenario,FeatureContext featureContext) {
            this.scenario = scenario;
            this.featureContext=featureContext;
            this.uri = featureContext.getUri() + "/scenario/" + scenario.getIndex();
            return this;
        }

        public void setScenarioId(Maybe<String> scenarioId) {
            this.scenarioId = scenarioId;
        }

    }

    static class FeatureContext {

        private String currentFeatureUri;
        private Maybe<String> currentFeatureId;
        private Feature currentFeature;
        private Set<ItemAttributesRQ> attributes;

        FeatureContext() {
            attributes = new HashSet<ItemAttributesRQ>();
        }

        FeatureContext createFeatureContext(Feature feature) {
            currentFeature = feature;
            currentFeatureUri = feature.getResource().getPath().toString();
            if (feature.getTags() != null && !feature.getTags().isEmpty()) {
                attributes = Utils.extractAttributes(feature.getTags());
            }
            return this;
        }

        Feature getFeature() {
            return currentFeature;
        }

        Set<ItemAttributesRQ> getAttributes() {
            return attributes;
        }

        String getUri() {
            return currentFeatureUri;
        }

        Maybe<String> getFeatureId() {
            return currentFeatureId;
        }

        void setFeatureId(Maybe<String> featureId) {
            this.currentFeatureId = featureId;
        }

    }

}
