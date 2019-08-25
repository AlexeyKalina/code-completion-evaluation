# Code Completion Quality Evaluation Plugin [![Build Status](https://travis-ci.com/bibaev/code-completion-evaluation.svg?branch=master)](https://travis-ci.com/bibaev/code-completion-evaluation)

## Approach

The plugin deals with the quality evaluation of code completion based on artificial queries. General approach:
1. For selected files, AST trees are built using Babelfish service.
2. Based on the AST trees and the query generation strategy, *actions* are built - actions that need to be performed by IDE (move caret, print text, call completion, etc).
3. Generated actions are interpreted in the IDE. As a result, sessions are formed (include expected text and lookups for one token).
4. Calculation of metrics and output of results in HTML-report.

## Requirements

You need to install [Babelfish](https://doc.bblf.sh) service with language drivers. The easiest way to do this is using [Docker](https://docs.docker.com). Run the following command:

`docker run -d --name bblfshd --privileged -p 9432:9432 bblfsh/bblfshd:latest-drivers`

Note: for python files Babelfish service is not needed.

## Installation

1. In Intellij IDEA add custom plugin repository `https://raw.githubusercontent.com/bibaev/code-completion-evaluation/master/updatePlugins.xml`. [Instruction](https://www.jetbrains.com/help/idea/managing-plugins.html#repos)
2. Install plugin `Code Completion Quality Evaluation` in Marketplace.

## Usage
1. Select files and/or directories for code completion evaluation.
2. Right click and select `Evaluate Completion For Selected Files`.
3. Select strategy for actions generation in opened dialog.
4. After generation and interpretation of actions you will be asked to open the report in the browser.

## Features

- Different languages:
  - Java
  - Python
  - C#
- Different completion types:
  - Basic
  - Smart
  - ML (basic with reordering based on machine learning)
- Different strategies of actions generation:
  - Context of completion token (previous and all)
  - Prefix of completion token (empty, first characters and uppercase characters)
  - Type of completion tokens (method calls, variables and method arguments)
- Different metrics:
  - Precision
  - Recall
  - F-measure
  - Mean Reciprocal Rank
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
  5. If config missing, default config will be created. Fill settings in default config before restarting evaluation.
- For running in debug mode you need:
  1. Install plugin to builded IDEA.
  2. Create debug-configuration:
    ![run-configuration](https://user-images.githubusercontent.com/7608535/61994170-ef155a80-b07f-11e9-9a5b-fbfba5008875.png)
  3. Start created configuration.
