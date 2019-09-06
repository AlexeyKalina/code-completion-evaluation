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
  2. Change title of evaluation (results will group by this name in HTML-report) in `<outputDir>/data/config.json`.
  3. Select multiple `config.json` from different evaluations.
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
  - Precision@1
  - Precision@5
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

- For running from command line you need:
  1. Add `-Djava.awt.headless=true` to jvm-options. [Instruction](https://www.jetbrains.com/help/idea/tuning-the-ide.html).
  2. Create command line launcher for Intellij IDEA. [Instruction](https://www.jetbrains.com/help/idea/working-with-the-ide-features-from-command-line.html).
  3. Run command `<Intellij IDEA> evaluate-completion [path_to_config]`.
  4. If `path_to_config` missing, default config path will be used (`config.json`). 
  5. If config missing, default config will be created. Fill settings in default config before restarting evaluation. All settings:
    ```javascript
    {
      "projectPath": "", // string with path to idea project
      "listOfFiles": [ "" ], // list of string with paths to files/directories for evaluation
      "language": "Java", // Java, Python, Shell Script or Another
      "strategy": {
        "prefix": {
          "name": "SimplePrefix", // SimplePrefix, CapitalizePrefix or NoPrefix
          "n": 2, // length of prefix (for SimplePrefix)
          "emulateTyping": true // emulate typing or not for Simple and Capitalize prefix
        },
        "statement": "ALL", // METHOD_CALLS, ARGUMENTS, VARIABLES, ALL_STATIC, ALL, ALL_TOKENS
        "context": "ALL" // ALL, PREVIOUS
      },
      "completionType": "BASIC", // BASIC, SMART, ML
      "outputDir": "", // string with path to output directory
      "saveLogs": true, // save completion logs or not (only if Completion-Stats-Collector plugin installed)
      "logsTrainingPercentage": 70, // percentage for logs separation on training/validate
      "interpretActions": true // interpret or not actions after its generation
    }
- For running in debug mode you need:
  1. Install plugin to builded IDEA.
  2. Create debug-configuration:
    ![run-configuration](https://user-images.githubusercontent.com/7608535/61994170-ef155a80-b07f-11e9-9a5b-fbfba5008875.png)
  3. Start created configuration.
