# AI Repo Reviewer

This is a toy project to play around with chainlang4j.  What it does is
* download a repo from git, or use a local repo (any source control or lack thereof)
* load a pre-canned set of prompts, which can be customized
* load the files within the repo into the working memory of the LLM model (which model can be customized)
* and finally execute each of the prompts against the working memory of the LLM and output a report

It currently runs via an IDE without issue but as a self contained jar it's got problems.

Consider this to follow the GNU public license.