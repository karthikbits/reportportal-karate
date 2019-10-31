package com.epam.reportportal.karate;

import com.epam.reportportal.service.Launch;
import com.intuit.karate.Results;
import com.intuit.karate.core.*;
import com.intuit.karate.http.HttpRequestBuilder;

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
import org.apache.commons.io.FilenameUtils;
import javafx.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Supplier;
import rp.com.google.common.base.Suppliers;
import java.util.*;

import static rp.com.google.common.base.Strings.isNullOrEmpty;

public class StepReporter implements ExecutionHook {

    Supplier<Launch> launch;
    static final String COLON_INFIX = ": ";
    private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
    private static final Logger LOGGER = LoggerFactory.getLogger(StepReporter.class);

    private Map<String, RunningContext.FeatureContext> currentFeatureContextMap = Collections.synchronizedMap(new HashMap<String, RunningContext.FeatureContext>());
    // private Map<Pair<String, String>, RunningContext.ScenarioContext> currentScenarioContextMap = Collections.synchronizedMap(new HashMap<Pair<String, String>, RunningContext.ScenarioContext>());
    // private Map<Long, RunningContext.ScenarioContext> threadCurrentScenarioContextMap = Collections.synchronizedMap(new HashMap<Long, RunningContext.ScenarioContext>());

    // There is no event for recognizing end of feature in Cucumber.
    // This map is used to record the last scenario time and its feature uri.
    // End of feature occurs once launch is finished.
    private Map<String, Date> featureEndTime = Collections.synchronizedMap(new HashMap<String, Date>());

    private Map<String, RunningContext.ScenarioContext> currentScenarioContextMap = Collections.synchronizedMap(new HashMap<String, RunningContext.ScenarioContext>());

    @Override
    public boolean beforeScenario(Scenario scenario, ScenarioContext scenarioContext) {
        
         try {
             
       
        Utils.printScenario(scenario);

        String scenarioURI = buildScenarioNode(scenario);
        RunningContext.ScenarioContext currentScenarioContext = currentScenarioContextMap.get(scenarioURI);
        currentScenarioContext = currentScenarioContext == null ? createScenarioContext(scenario, scenarioContext) : currentScenarioContext;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("*** SCENARIO EXCEPTION ** ");
        }
        return true;
    }

    @Override
    public void afterScenario(ScenarioResult scenarioResult, ScenarioContext scenarioContext) {

    }

    @Override
    public boolean beforeFeature(Feature feature, ExecutionContext executionContext) {
        try {

            Utils.printFeature(feature);
            Utils.printExecutionContext(executionContext);
            this.handleStartOfFeature(feature, executionContext);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("*** EXCEPTION ** ");
        }
        return true;
    }

    @Override
    public void afterFeature(FeatureResult featureResult, ExecutionContext executionContext) {
        if (featureResult.getFeature() == null) {
            return;
        }
        String featureURI = buildFeatureNode(featureResult.getFeature());
        RunningContext.FeatureContext currentFeatureContext = currentFeatureContextMap.get(featureURI);
        StatusEnum status = StatusEnum.SKIPPED;
        if (featureResult.getScenarioCount() > 0) {
            if (featureResult.isFailed()) {
                status = StatusEnum.FAILED;
            } else {
                status = StatusEnum.PASSED;
            }
        }
        if (currentFeatureContext != null) {
            System.out.println("*** SAN UPDATING STATUS ** " + currentFeatureContext.getFeatureId() + " to " + status);
            Date endTime = Utils.finishTestItem(launch.get(), currentFeatureContext.getFeatureId(), status.toString());
        }
    }

    @Override
    public void beforeAll(Results results) {
        System.out.println("*** SUITE  beforeAll ** ");
        startLaunch();
    }

    @Override
    public void afterAll(Results results) {
        System.out.println("*** SUITE   afterAll** ");
        stopLaunch();
    }

    @Override
    public boolean beforeStep(Step step, ScenarioContext scenarioContext) {
        return true;
    }

    @Override
    public void afterStep(StepResult stepResult, ScenarioContext scenarioContext) {

    }

    @Override
    public String getPerfEventName(HttpRequestBuilder httpRequestBuilder, ScenarioContext scenarioContext) {
        return null;
    }

    @Override
    public void reportPerfEvent(PerfEvent perfEvent) {

    }

    private void startLaunch() {
        launch = Suppliers.memoize(new Supplier<Launch>() {

            /* should no be lazy */
            private final Date startTime = Calendar.getInstance().getTime();

            @Override
            public Launch get() {
                final ReportPortal reportPortal = ReportPortal.builder().build();
                ListenerParameters parameters = reportPortal.getParameters();

                StartLaunchRQ rq = new StartLaunchRQ();
                rq.setName(parameters.getLaunchName());
                rq.setStartTime(startTime);
                rq.setMode(parameters.getLaunchRunningMode());
                rq.setAttributes(parameters.getAttributes());
                rq.setDescription(parameters.getDescription());
                rq.setRerun(parameters.isRerun());
                if (!isNullOrEmpty(parameters.getRerunOf())) {
                    rq.setRerunOf(parameters.getRerunOf());
                }

                if (null != parameters.getSkippedAnIssue()) {
                    ItemAttributesRQ skippedIssueAttribute = new ItemAttributesRQ();
                    skippedIssueAttribute.setKey(SKIPPED_ISSUE_KEY);
                    skippedIssueAttribute.setValue(parameters.getSkippedAnIssue().toString());
                    skippedIssueAttribute.setSystem(true);
                    rq.getAttributes().add(skippedIssueAttribute);
                }

                return reportPortal.newLaunch(rq);
            }
        });
    }

    private void stopLaunch() {
        FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
        finishLaunchRq.setEndTime(Calendar.getInstance().getTime());
        launch.get().finish(finishLaunchRq);
    }

    /**
     * It's essential to operate with feature URI, to prevent problems with the
     * same feature names in different folders/packages
     */
    private String buildScenarioNode(Scenario scenario) {
        RunningContext.FeatureContext featureContext = new RunningContext.FeatureContext().createFeatureContext(scenario.getFeature());
        RunningContext.ScenarioContext scenarioContext = new RunningContext.ScenarioContext().createScenarioContext(scenario, featureContext);
        return scenarioContext.getUri();
    }

    private String buildFeatureNode(Feature feature) {
        RunningContext.FeatureContext featureContext = new RunningContext.FeatureContext().createFeatureContext(feature);
        //String featureKeyword = featureContext.getFeature().getDescription();
        // String featureName = featureContext.getFeature().getName();
        // Utils.buildNodeName(featureKeyword, StepReporter.COLON_INFIX, featureName, null);
        return featureContext.getUri();
    }

    private void handleStartOfFeature(Feature feature, ExecutionContext executionContext) {
        String featureURI = buildFeatureNode(feature);
        RunningContext.FeatureContext currentFeatureContext = currentFeatureContextMap.get(featureURI);
        currentFeatureContext = currentFeatureContext == null ? createFeatureContext(feature, featureURI) : currentFeatureContext;
        if (!currentFeatureContext.getUri().equals(featureURI)) {
            throw new IllegalStateException("Scenario URI does not match Feature URI.");
        }
    }

    protected Maybe<String> getRootItemId() {
        return null;
    }

    protected String getFeatureTestItemType() {
        return "SUITE";
    }

    private RunningContext.FeatureContext createFeatureContext(Feature feature, String featureURI) {
        RunningContext.FeatureContext currentFeatureContext;
        currentFeatureContext = new RunningContext.FeatureContext().createFeatureContext(feature);
        StartTestItemRQ rq = new StartTestItemRQ();
        Maybe<String> root = getRootItemId();
        rq.setDescription(currentFeatureContext.getUri());
        rq.setName(FilenameUtils.getBaseName(currentFeatureContext.getUri()));
        rq.setAttributes(currentFeatureContext.getAttributes());
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType(getFeatureTestItemType());
        currentFeatureContext.setFeatureId(root == null ? launch.get().startTestItem(rq) : launch.get().startTestItem(root, rq));
        currentFeatureContextMap.put(featureURI, currentFeatureContext);
        return currentFeatureContext;
    }

    private RunningContext.ScenarioContext createScenarioContext(Scenario scenario, ScenarioContext scenarioContext) {

        RunningContext.FeatureContext featureContext = new RunningContext.FeatureContext().createFeatureContext(scenario.getFeature());
        RunningContext.ScenarioContext currentScenarioContext;
        currentScenarioContext = new RunningContext.ScenarioContext().createScenarioContext(scenario, featureContext);
        StartTestItemRQ rq = new StartTestItemRQ();
        Maybe<String> root = getRootItemId();
        rq.setDescription(scenario.getDescription());
        rq.setName((scenario.getName()));
        //rq.setAttributes(currentScenarioContext.);
        rq.setStartTime(Calendar.getInstance().getTime());
        rq.setType("SCENARIO");
        currentScenarioContext.setScenarioId(launch.get().startTestItem(featureContext.getFeatureId(), rq));
        currentScenarioContextMap.put(currentScenarioContext.getUri(), currentScenarioContext);
        return currentScenarioContext;

    }
}
