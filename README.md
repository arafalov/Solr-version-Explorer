# Solr-version-Explorer

Compare Javadoc parsing information with git release tag information to extract interesting historical facts.
For example, in which version a particular component (implementing a root class) is introduced.

A total hack, using multiple undocumented APIs, but it works well enough to speed up any in-depth research.

Outputs lots of various debug, at least one of which looks like:


     ClassicTokenizerFactory.java first shows up in 3.1
     EdgeNGramTokenizerFactory.java first shows up in 1.2.0
     HMMChineseTokenizerFactory.java first shows up in 4.8.0
     ICUTokenizerFactory.java first shows up in 3.1
     JapaneseTokenizerFactory.java first shows up in 3.6.0
     KeywordTokenizerFactory.java first shows up in 1.1.0
     LetterTokenizerFactory.java first shows up in 1.1.0
     LowerCaseTokenizerFactory.java first shows up in 1.1.0
     NGramTokenizerFactory.java first shows up in 1.2.0
     PathHierarchyTokenizerFactory.java first shows up in 3.1
     PatternTokenizerFactory.java first shows up in solr1.2
     SimplePatternSplitTokenizerFactory.java first shows up in 6.5.0
     SimplePatternTokenizerFactory.java first shows up in 6.5.0
     ...
