# Code Completion Quality Evaluation Plugin [![Build Status](https://travis-ci.com/bibaev/code-completion-evaluation.svg?branch=master)](https://travis-ci.com/bibaev/code-completion-evaluation)

## Approach

The plugin deals with the quality evaluation of code completion based on artificial queries. General approach:
1. For selected files, Unified AST trees are built using Intellij PSI.
2. Based on the AST trees and the query generation strategy, *actions* are built - actions that need to be performed by IDE (move caret, print text, call completion, etc).
3. Generated actions are interpreted in the IDE. As a result, sessions are formed (include expected text and lookups for one token).
4. Calculation of metrics and output of results in HTML-report.

## Installation

1. In Intellij IDEA add custom plugin repository `https://raw.githubusercontent.com/bibaev/code-completion-evaluation/master/updatePlugins.xml`. [Instruction](https://www.jetbrains.com/help/idea/managing-plugins.html#repos)
2. Install plugin `Code Completion Quality Evaluation` in Marketplace.

## Usage
- Full. Evaluate quality for multiple files with HTML-report as result.
  1. Select files and/or directories for code completion evaluation.
  2. Right click and select `Evaluate Completion For Selected Files`.
  3. Select strategy for actions generation in opened dialog.
  4. After generation and interpretation of actions you will be asked to open the report in the browser.
- Quick. Evaluate quality for some element (all its content) in code with highlighting of completed tokens in editor.
  1. Right click on element you want to complete and select `Evaluate Completion Here`.
  2. Select strategy for actions generation in opened dialog.
  3. After generation and interpretation of actions tokens will be highlighted and provide information about completions by click.
- Compare multiple evaluations.
  1. Evaluate quality on different algorithms/strategies multiple times.
  2. Change `evaluationTitle` in `config.json` in corresponding workspaces. Results will group by this field in HTML-report.
  3. Select these workspaces.
  4. Right click and select `Generate Report By Selected Evaluations`.
  5. After report building you will be asked to open it in the browser.

## Features

- Different languages:
  - Java
  - Python
  - Bash
  - All tokens completion without AST building for all languages supported by IDEA
- Different completion types:
  - Basic
  - Smart
  - ML (basic with reordering based on machine learning)
- Different strategies of actions generation:
  - Context of completion token (previous and all)
  - Prefix of completion token (empty, first characters and uppercase characters). Also it can emulate typing
  - Type of completion tokens (method calls, variables, method arguments, static members and all tokens)
- Different metrics:
  - Found@1
  - Found@5
  - Recall
  - [eSaved](http://terrierteam.dcs.gla.ac.uk/publications/kharitonov-sigir2013.pdf)
- HTML-reports:
  - Global report with metrics and links to file reports
  - Reports for files with all completions
- Headless mode

## Headless Mode

You can run completion quality evaluation without IDEA UI.

### UI mode comparision

For comparision of two modes, a project with about a thousand java files was used.

1. Quality. Metric values are the same for both modes.
2. Performance. In headless mode completion quality was evaluated on the project about twice as fast as in UI mode. (7 min against 15 min).

### Usage

To start the evaluation in the headless mode you should describe where the project to evaluate is placed and rules for evaluation (language, strategy, output directories, etc.). We use JSON file for such king of description. The easiest way to create config is using `Create Config` button in settings dialog in UI mode of plugin. Here is an example of such file with description for possible options.
```javascript
{
  "projectPath": "", // string with path to idea project
  "language": "Java", // Java, Python, Shell Script or Another
  "outputDir": "", // string with path to output directory
  "actions": { // part of config about actions generation step
    "evaluationRoots": [ ], // list of string with paths to files/directories for evaluation
    "strategy": { // describes evaluation rules
      "completeAllTokens": false, // if true - all tokens will be tried to complete one by one
      "context": "ALL", // ALL, PREVIOUS
      "prefix": { // policy how to complete particular token
        "name": "SimplePrefix", // SimplePrefix (type 1 or more letters), CapitalizePrefix or NoPrefix
        "emulateTyping": false, // type token char by char and save intermediate results
        "n": 1 // numbers of char to type before trigger completion
      },
      "filters": { // set of filters that allow to filter some completion locations out
        "statementTypes": [ // possible values: METHOD_CALL, FIELD, VARIABLE
          "METHOD_CALL" 
        ],
        "isArgument": null, // null / true / false
        "isStatic": true, // null / true / false
        "packageRegex": ".*" // regex to check  if java package of resulting token is suitable for evaluation
      }
    }
  },
  "interpret": { // part of config about actions interpretation step
    "completionType": "BASIC", // BASIC, SMART, ML
    "completeTokenProbability": 1.0, // probability that token will be completed
    "completeTokenSeed": null, // seed for random (for previous option)
    "saveLogs": false, // save completion logs or not (only if Completion-Stats-Collector plugin installed)
    "logsTrainingPercentage": 70 // percentage for logs separation on training/validate
  },
  "reports": { // part of config about report generation step
    "evaluationTitle": "Basic", // header name in HTML-report (use different names for report generation on multiple evaluations)
    "sessionsFilters": [ // create multiple reports corresponding to these sessions filters (filter "All" creates by default)
      {
        "name": "Static method calls only",
        "filters": {
          "statementTypes": [
            "METHOD_CALL"
          ],
          "isArgument": null,
          "isStatic": true,
          "packageRegex": ".*"
        }
      }
    ]
  }
}
```

Example of `config.json` to evaluate code completion on several modules from intellij-community project
```javascript
{
  "projectPath": "PATH_TO_COMMUNITY_PROJECT",
  "language": "Java",
  "outputDir": "PATH_TO_COMMUNITY_PROJECT/completion-evaluation",
  "actions": {
    "evaluationRoots": [
      "java/java-indexing-impl",
      "java/java-analysis-impl",
      "platform/analysis-impl",
      "platform/core-impl",
      "platform/indexing-impl",
      "platform/vcs-impl",
      "platform/xdebugger-impl",
      "plugins/git4idea",
      "plugins/java-decompiler",
      "plugins/gradle",
      "plugins/markdown",
      "plugins/sh",
      "plugins/terminal",
      "plugins/yaml"
    ],
    "strategy": {
      "completeAllTokens": false,
      "context": "ALL",
      "prefix": {
        "name": "SimplePrefix",
        "emulateTyping": false,
        "n": 1
      },
      "filters": {
        "statementTypes": [
          "METHOD_CALL"
        ],
        "isArgument": null,
        "isStatic": null,
        "packageRegex": ".*"
      }
    }
  },
  "interpret": {
    "completionType": "BASIC",
    "completeTokenProbability": 1.0,
    "completeTokenSeed": null,
    "saveLogs": false,
    "trainTestSplit": 70
  },
  "reports": {
    "evaluationTitle": "Basic",
    "sessionsFilters": []
  }
}
```

There are several options for the plugin to work in headless mode:
- Full. Use the config to execute the plugin on a set of files / directories. As a result of execution, HTML report will be created.
  - Usage: `evaluate-completion full [PATH_TO_CONFIG]`
  - If `PATH_TO_CONFIG` missing, default config will be created.
  - If config missing, default config will be created. Fill settings in default config before restarting evaluation.
- Custom. Allows you to interpret actions and/or generate reports on an existing workspace.
  - Usage: `evaluate-completion custom [--interpret-actions | -i] [--generate-report | -r] PATH_TO_WORKSPACE`
- Multiple Evaluations. Create a report based on multiple evaluations.
  - Usage: `evaluate-completion multiple-evaluations PATH_TO_WORKSPACE...`
- Multiple Evaluations in Directory. Works as the previous option to all workspaces in the directory.
  - Usage: `evaluate-completion compare-in PATH_TO_DIRECTORY`

There are many ways to start the evaluation in headless mode. Some of them are listed below.

#### Run from command line:
  1. Add `-Djava.awt.headless=true` to jvm-options. [Instruction](https://www.jetbrains.com/help/idea/tuning-the-ide.html).
  2. Create command line launcher for Intellij IDEA. [Instruction](https://www.jetbrains.com/help/idea/working-with-the-ide-features-from-command-line.html).
  3. Run command `<Intellij IDEA> evaluate-completion OPTION OPTION_ARGS` with corresponding option.

#### Run with intellij from sources:
1. Install plugin to debuggee IDEA.
2. Create debug-configuration:
![run-configuration](https://user-images.githubusercontent.com/7608535/61994170-ef155a80-b07f-11e9-9a5b-fbfba5008875.png)
3. Start the configuration.

#### Run with intellij gradle plugin
1. Create gradle task:
``` groovy
import org.jetbrains.intellij.tasks.RunIdeTask
task evaluateCompletion(type: RunIdeTask) {
    jvmArgs = ['-Xmx4G', '-Djava.awt.headless=true']
    args = ['evaluate-completion' 'OPTION', 'OPTION_ARGS']
    ideaDirectory = { runIde.ideaDirectory }
    pluginsDirectory = { runIde.pluginsDirectory }
    configDirectory = { runIde.configDirectory }
    systemDirectory = { runIde.systemDirectory }
    dependsOn = runIde.dependsOn
    group 'intellij'
}
```
2. Install the plugin inside sandbox IDE (the IDE started by executing `runIde` gradle task)
3. Specify necessary arguments in the created task.
4. Start `evaluateCompletion` task
