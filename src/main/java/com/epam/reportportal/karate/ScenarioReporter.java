package com.epam.reportportal.karate;

/**
 *
 * @author kvijayapandian
 */
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.intuit.karate.Results;
import com.intuit.karate.core.*;
import com.intuit.karate.http.HttpRequestBuilder;
import io.reactivex.Maybe;
import java.util.*;
import org.apache.commons.io.FilenameUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.base.Strings;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
import rp.com.google.common.base.Supplier;
import rp.com.google.common.base.Suppliers;

public class ScenarioReporter implements ExecutionHook {

    private Supplier<Launch> launch;
    private static final String COLON_INFIX = ": ";
    private static final String SKIPPED_ISSUE_KEY = "skippedIssue";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioReporter.class);
    private final Map<String, Date> featureStartDateMap = Collections.synchronizedMap(new HashMap<String, Date>());
    private String featureNamePrefix;

    public void setFeatureNamePrefix(String featureNamePrefix) {
        this.featureNamePrefix = featureNamePrefix;
    }

    @Override
    public boolean beforeScenario(Scenario scenario, ScenarioContext scenarioContext) {
        return true;
    }

    @Override
    public void afterScenario(ScenarioResult scenarioResult, ScenarioContext scenarioContext) {

    }

    @Override
    public boolean beforeFeature(Feature feature, ExecutionContext executionContext) {

        try {

            this.handleBeforeFeature(feature, executionContext);

        } catch (Exception ex) {
            LOGGER.error("Error while handling before feature event: {}", ex.getMessage(), ex);
        }

        return true;
    }

    @Override
    public void afterFeature(FeatureResult featureResult, ExecutionContext executionContext) {

        try {

            this.handleAfterFeature(featureResult, executionContext);

        } catch (Exception ex) {
            LOGGER.error("Error while handling after feature event: {}", ex.getMessage(), ex);
        }

    }

    @Override
    public void beforeAll(Results results) {

        try {
            this.startLaunch();
        } catch (Exception ex) {
            LOGGER.error("Error while handling before all event: {}", ex.getMessage(), ex);
        }

    }

    @Override
    public void afterAll(Results results) {

        try {
            this.stopLaunch(results);
        } catch (Exception ex) {
            LOGGER.error("Error while handling after all event: {}", ex.getMessage(), ex);
        }

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
            @Override
            public Launch get() {
                final ReportPortal reportPortal = ReportPortal.builder().build();
                ListenerParameters parameters = reportPortal.getParameters();
                StartLaunchRQ rq = buildStartLaunchRq(reportPortal.getParameters());
                return reportPortal.newLaunch(rq);
            }
        });

        launch.get().start();
    }

    private FinishTestItemRQ buildStopFeatureRq(FeatureResult featureResult) {
        Date now = Calendar.getInstance().getTime();
        FinishTestItemRQ rq = new FinishTestItemRQ();
        rq.setEndTime(now);
        rq.setStatus(getFeatureStatus(featureResult));
        return rq;
    }

    private StartTestItemRQ buildStartFeatureRq(FeatureResult featureResult) {
        Feature feature = featureResult.getFeature();
        StartTestItemRQ rq = new StartTestItemRQ();
        Maybe<String> root = getRootItemId();
        if (!Strings.isNullOrEmpty(feature.getName())) {
            rq.setDescription(feature.getName());
        }
        if (feature.getTags() != null && !feature.getTags().isEmpty()) {
            Set<ItemAttributesRQ> attributes = Utils.extractAttributes(feature.getTags());
            rq.setAttributes(attributes);
        }
        String featureUri = Utils.getURI(feature);
        rq.setName(FilenameUtils.getBaseName(featureUri));
        if (featureStartDateMap.containsKey(featureUri)) {
            rq.setStartTime(featureStartDateMap.get(featureUri));
        } else {
            rq.setStartTime(Calendar.getInstance().getTime());
        }
        rq.setType("TEST");
        return rq;
    }

    protected StartLaunchRQ buildStartLaunchRq(ListenerParameters parameters) {

        StartLaunchRQ rq = new StartLaunchRQ();
        rq.setName(parameters.getLaunchName());
        rq.setStartTime(Calendar.getInstance().getTime());
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
        return rq;
    }

    private void stopLaunch(Results results) {
        FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
        finishLaunchRq.setEndTime(Calendar.getInstance().getTime());
        finishLaunchRq.setStatus(getLaunchStatus(results));
        launch.get().finish(finishLaunchRq);
    }

    protected Maybe<String> getRootItemId() {
        return null;
    }

    private String getLaunchStatus(Results results) {
        StatusEnum status = StatusEnum.SKIPPED;
        if (results.getScenarioCount() > 0) {
            if (results.getFailCount() > 0) {
                status = StatusEnum.FAILED;
            } else {
                status = StatusEnum.PASSED;
            }
        }
        return status.toString();
    }

    private String getFeatureStatus(FeatureResult featureResult) {
        StatusEnum status = StatusEnum.SKIPPED;
        if (featureResult.getScenarioCount() > 0) {
            if (featureResult.isFailed()) {
                status = StatusEnum.FAILED;
            } else {
                status = StatusEnum.PASSED;
            }
        }
        return status.toString();
    }

    private String getScenerioStatus(ScenarioResult scenarioResult) {
        StatusEnum status = StatusEnum.SKIPPED;
        if (scenarioResult.getStepResults() != null && scenarioResult.getStepResults().size() > 0) {
            if (scenarioResult.getFailedStep() != null) {
                status = StatusEnum.FAILED;
            } else {
                status = StatusEnum.PASSED;
            }
        }
        return status.toString();
    }

    private void handleBeforeFeature(Feature feature, ExecutionContext executionContext) {
        String featureUri = Utils.getURI(feature);
        String featureName = FilenameUtils.getBaseName(featureUri);
        if (!Strings.isNullOrEmpty(this.featureNamePrefix)) {
            if (!featureName.startsWith(this.featureNamePrefix)) {
                LOGGER.warn("Dropping feature event due to feature name prefix mismatch featureName={} ", featureName);
                return;
            }
        }
        featureStartDateMap.put(featureUri, Calendar.getInstance().getTime());

    }

    private void handleAfterFeature(FeatureResult featureResult, ExecutionContext executionContext) {
        String featureUri = Utils.getURI(featureResult.getFeature());
        String featureName = FilenameUtils.getBaseName(featureUri);

        if (featureResult.getScenarioCount() <= 0) {
            LOGGER.warn("Dropping feature event as scenario count is zero featureName={} ", featureName);
            return;
        }

        if (!Strings.isNullOrEmpty(this.featureNamePrefix)) {
            if (!featureName.startsWith(this.featureNamePrefix)) {
                LOGGER.warn("Dropping feature event due to feature name prefix mismatch featureName={} ", featureName);
                return;
            }
        }
        String featureResultString = Utils.printFeatureResult(featureResult);
        String featureString = Utils.printFeature(featureResult.getFeature());
        LOGGER.info("AfterFeature event: featureResult={} feature={} ", featureResultString, featureString);

        Feature feature = featureResult.getFeature();
        StartTestItemRQ startTestItemRQ = buildStartFeatureRq(featureResult);
        Maybe<String> featureId = launch.get().startTestItem(this.getRootItemId(), startTestItemRQ);

        for (ScenarioResult scenarioResult : featureResult.getScenarioResults()) {
            Maybe<String> scenarioId = launch.get().startTestItem(featureId, this.buildStartScenerioRq(scenarioResult, executionContext));
            FinishTestItemRQ finishTestItemRQ = buildStopScenerioRq(scenarioResult, executionContext);
            launch.get().finishTestItem(scenarioId, finishTestItemRQ);
            this.reportErrorMessage(scenarioResult, scenarioId.blockingGet());

        }

        FinishTestItemRQ finishTestItemRQ = buildStopFeatureRq(featureResult);
        launch.get().finishTestItem(featureId, finishTestItemRQ);

    }

    protected StartTestItemRQ buildStartScenerioRq(ScenarioResult scenarioResult, ExecutionContext executionContext) {
        StartTestItemRQ rq = new StartTestItemRQ();
        rq.setDescription(scenarioResult.getScenario().getDescription());
        rq.setName(scenarioResult.getScenario().getName());
        String featureUri = Utils.getURI(scenarioResult.getScenario().getFeature());
        if (featureStartDateMap.containsKey(featureUri)) {
            rq.setStartTime(new Date(scenarioResult.getStartTime() + featureStartDateMap.get(featureUri).getTime()));
        } else {
            rq.setStartTime(Calendar.getInstance().getTime());
        }
        rq.setType("STEP");
        return rq;
    }

    private FinishTestItemRQ buildStopScenerioRq(ScenarioResult scenarioResult, ExecutionContext executionContext) {
        Date now = Calendar.getInstance().getTime();
        FinishTestItemRQ rq = new FinishTestItemRQ();
        String featureUri = Utils.getURI(scenarioResult.getScenario().getFeature());
        if (featureStartDateMap.containsKey(featureUri)) {
            rq.setEndTime(new Date(scenarioResult.getEndTime() + featureStartDateMap.get(featureUri).getTime()));
        } else {
            rq.setEndTime(Calendar.getInstance().getTime());
        }
        rq.setStatus(this.getScenerioStatus(scenarioResult));
        return rq;
    }

    private void reportErrorMessage(ScenarioResult scenarioResult, String itemUuid) {

        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(scenarioResult.getFailureMessageForDisplay())) {
            sb.append("  ****** Failure Message ******");
            sb.append("\n\n");
            sb.append(scenarioResult.getFailureMessageForDisplay());
            sb.append("\n\n");
            sb.append("  ****** End Failure Message ******");
            sb.append("\n\n");
        }
        for (StepResult stepResult : scenarioResult.getStepResults()) {
            if (!Strings.isNullOrEmpty(stepResult.getErrorMessage())) {
                sb.append("  ****** Error Message ******");
                sb.append("\n\n");
                sb.append(stepResult.getErrorMessage());
                sb.append("\n\n");
                sb.append("  ****** End Error Message ******");
                sb.append("\n\n");
            }
        }
        Utils.sendLog(itemUuid, sb.toString(), "ERROR", null);

    }

}
