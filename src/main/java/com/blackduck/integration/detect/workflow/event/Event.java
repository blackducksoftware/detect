package com.blackduck.integration.detect.workflow.event;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import com.blackduck.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.blackduck.integration.detect.tool.detector.DetectorToolResult;
import com.blackduck.integration.detect.tool.detector.executable.ExecutedExecutable;
import com.blackduck.integration.detect.workflow.codelocation.DetectCodeLocationNamesResult;
import com.blackduck.integration.detect.workflow.profiling.DetectorTimings;
import com.blackduck.integration.detect.workflow.result.DetectResult;
import com.blackduck.integration.detect.workflow.status.DetectIssue;
import com.blackduck.integration.detect.workflow.status.FormattedCodeLocation;
import com.blackduck.integration.detect.workflow.status.Operation;
import com.blackduck.integration.detect.workflow.status.Status;
import com.blackduck.integration.detect.workflow.status.UnrecognizedPaths;
import com.blackduck.integration.util.NameVersion;

public class Event {
    public static final EventType<DetectorToolResult> DetectorsComplete = new EventType<>(DetectorToolResult.class);
    public static final EventType<DetectorTimings> DetectorsProfiled = new EventType<>(DetectorTimings.class);
    public static final EventType<Integer> ExtractionCount = new EventType<>(Integer.class);
    //    public static final EventType<DetectorEvaluation> ExtractionStarted = new EventType<>(DetectorEvaluation.class);
    //    public static final EventType<DetectorEvaluation> ExtractionEnded = new EventType<>(DetectorEvaluation.class);
    public static final EventType<DetectCodeLocationNamesResult> DetectCodeLocationNamesCalculated = new EventType<>(DetectCodeLocationNamesResult.class);
    public static final EventType<Collection<FormattedCodeLocation>> CodeLocationsCompleted = new EventType(Collection.class);
    public static final EventType<ExitCodeRequest> ExitCode = new EventType<>(ExitCodeRequest.class);
    public static final EventType<Status> StatusSummary = new EventType<>(Status.class);
    public static final EventType<DetectIssue> Issue = new EventType<>(DetectIssue.class);
    public static final EventType<DetectResult> ResultProduced = new EventType<>(DetectResult.class);
    public static final EventType<File> CustomerFileOfInterest = new EventType<>(File.class);
    public static final EventType<NameVersion> ProjectNameVersionChosen = new EventType<>(NameVersion.class);
    public static final EventType<ExecutedExecutable> Executable = new EventType<>(ExecutedExecutable.class);
    public static final EventType<UnrecognizedPaths> UnrecognizedPaths = new EventType<>(UnrecognizedPaths.class);
    public static final EventType<SortedMap<String, String>> RawMaskedPropertyValuesCollected = new EventType(SortedMap.class);
    public static final EventType<Collection<Operation>> DetectOperationsComplete = new EventType(Collection.class);
    public static final EventType<Map<String, String>> BlackDuckServerPropertiesCollected = new EventType(Map.class);
}
