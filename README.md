# Code Completion Quality Evaluation Plugin

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
