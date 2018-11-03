package com.solrstart.version.since;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.javadoc.SourcePosition;
import com.sun.tools.doclets.internal.toolkit.Configuration;
import com.sun.tools.doclets.internal.toolkit.Content;
import com.sun.tools.doclets.internal.toolkit.WriterFactory;
import com.sun.tools.doclets.internal.toolkit.util.MessageRetriever;

import javax.tools.JavaFileManager;
import java.util.Comparator;
import java.util.Locale;

/**
 * Keep minimal, as we are not actually writing anything out. Just reading.
 */
public class EmptyJavadocConfiguration extends Configuration {

    @Override
    public String getDocletSpecificBuildDate() {
        return null;
    }

    @Override
    public void setSpecificDocletOptions(String[][] strings) throws Fault {

    }

    @Override
    public MessageRetriever getDocletSpecificMsg() {
        return null;
    }

    @Override
    public boolean validOptions(String[][] strings, DocErrorReporter docErrorReporter) {
        return false;
    }

    @Override
    public Content newContent() {
        return null;
    }

    @Override
    public WriterFactory getWriterFactory() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public JavaFileManager getFileManager() {
        return null;
    }

    @Override
    public Comparator<ProgramElementDoc> getMemberComparator() {
        return null;
    }

    @Override
    public boolean showMessage(SourcePosition sourcePosition, String s) {
        return false;
    }
}
