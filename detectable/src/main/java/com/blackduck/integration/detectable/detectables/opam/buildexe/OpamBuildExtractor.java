package com.blackduck.integration.detectable.detectables.opam.buildexe;

import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.opam.buildexe.parse.OpamTreeParser;
import com.blackduck.integration.detectable.detectables.opam.parse.OpamFileParser;
import com.blackduck.integration.detectable.detectables.opam.parse.OpamParsedResult;
import com.blackduck.integration.common.util.Bds;
import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectables.opam.transform.OpamGraphTransformer;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.Extraction.Builder;
import com.blackduck.integration.executable.ExecutableOutput;
import com.blackduck.integration.executable.ExecutableRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OpamBuildExtractor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OpamGraphTransformer opamGraphTransformer;
    private final OpamTreeParser opamTreeParser;
    private final DetectableExecutableRunner executableRunner;
    private final File sourceDirectory;
    private static final String OPAM_TREE_FILE = "/opamTreeOutput.json";

    public OpamBuildExtractor(OpamGraphTransformer opamGraphTransformer, OpamTreeParser opamTreeParser, DetectableExecutableRunner executableRunner, File sourceDirectory) {
        this.opamGraphTransformer = opamGraphTransformer;
        this.opamTreeParser = opamTreeParser;
        this.executableRunner = executableRunner;
        this.sourceDirectory = sourceDirectory;
    }

    public Extraction extract(List<File> opamFiles, ExecutableTarget opamExe, File outputDirectory) throws ExecutableRunnerException {
      try {
          String opamVersion = getOpamVersion(opamExe); // run opam --version command

          if(decideOpamVersion(opamVersion)) {
              File fullOutputFileName = new File(outputDirectory.getAbsolutePath()+OPAM_TREE_FILE);
              List<String> tree = runOpamTree(opamExe, fullOutputFileName); // run opam tree command

              if(!tree.isEmpty()) {
                  List<OpamParsedResult> result = opamTreeParser.parseJsonTreeFile(fullOutputFileName); //parse the tree

                  List<CodeLocation> codeLocations = Bds.of(result).map(OpamParsedResult::getCodeLocation).toList();
                  Extraction.Builder treeBuilder = new Extraction.Builder().success(codeLocations);

                  addProjectInformation(treeBuilder, result);
                  return treeBuilder.build();
              }
          }

          OpamFileParser opamFileParser = new OpamFileParser(); // initialize opam file parser

          List<OpamParsedResult> opamParsedResults = new ArrayList<>();

          for (File opamFile : opamFiles) {
              opamParsedResults.add(opamFileParser.parse(opamFile)); // for all the opam files, parse direct dependencies found.
          }

          List<CodeLocation> codeLocations = new ArrayList<>();

          for (OpamParsedResult opamParsedResult : opamParsedResults) {
              codeLocations.add(opamGraphTransformer.transform(opamExe, opamParsedResult));
          }

          Extraction.Builder showBuilder = new Extraction.Builder().success(codeLocations);
          addProjectInformation(showBuilder, opamParsedResults);

          return showBuilder.build();
      } catch (Exception e) {
          return new Builder().exception(e).build();
      }

    }

    private String getOpamVersion(ExecutableTarget opamExe) throws ExecutableRunnerException {
        List<String> arguments = new ArrayList<>();
        arguments.add("--version");

        ExecutableOutput executableOutput = executableRunner.execute(ExecutableUtils.createFromTarget(sourceDirectory, opamExe, arguments));

        if(executableOutput.getReturnCode() == 0) {
            return executableOutput.getStandardOutputAsList().get(0);
        } else {
            logger.warn("Detect was not able to find opam version correctly, finding dependencies with opam show"); // if version command was not successful continue with opam show approach
            return "";
        }
    }

    public List<String> runOpamTree(ExecutableTarget opamExe, File fullOutputFileName) throws ExecutableRunnerException, ExecutableFailedException {
        List<String> arguments = new ArrayList<>();
        arguments.add("tree");
        arguments.add(".");
        arguments.add("--with-dev");
        arguments.add("--with-test");
        arguments.add("--with-doc");
        arguments.add("--recursive");
        arguments.add("--json=" + fullOutputFileName.getAbsolutePath());

        ExecutableOutput executableOutput = executableRunner.executeSuccessfully(ExecutableUtils.createFromTarget(sourceDirectory, opamExe, arguments));

        if(executableOutput.getReturnCode() == 0) {
            return executableOutput.getStandardOutputAsList();
        } else {
            logger.warn("Detect was not able to find dependency tree correctly, finding dependencies with opam show"); // if tree command fails due to some reason, then we return empty list to continue with show approach
            return Collections.emptyList();
        }
    }

    private boolean decideOpamVersion(String opamVersion) {
        if (!opamVersion.isEmpty()) {
            String[] versionParts = opamVersion.split("\\.");

            // check if version is greater than 2.2.0
            int firstIndex = Integer.parseInt(versionParts[0]);
            if (firstIndex == 2) {
                int secondIndex = Integer.parseInt(versionParts[1]);
                return secondIndex >= 2;
            } else return firstIndex > 2;
        }
        return false;
    }

    private void addProjectInformation(Builder builder, List<OpamParsedResult> opamParsedResults) {
        Optional<OpamParsedResult> resultWithVersion = Bds.of(opamParsedResults).firstFiltered(result -> !result.getProjectName().isEmpty());
        if(resultWithVersion.isPresent()) {
            builder.projectName(resultWithVersion.get().getProjectName());
            builder.projectVersion(resultWithVersion.get().getProjectVersion());
        }
    }
}
