# AI Repo Reviewer

This is a toy project to play around with chainlang4j.  What it does is
* Download a repo from git, or use a local repo (any source control or lack thereof)
* Load a pre-canned set of prompts, which can be customized
* Load the files within the repo into the working memory of the LLM model (which model can be customized)
* And finally execute each of the prompts against the working memory of the LLM and output a report

It currently runs via an IDE without issue but as a self contained jar it's got problems.

Requires
* If downloading from a git repo that the git command line tool be installed/available
* A docker runtime is available to spin up the locally run LLM containers (2 containers)

Consider this to follow the GNU public license.
