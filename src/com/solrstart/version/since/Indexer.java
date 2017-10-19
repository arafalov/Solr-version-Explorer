package com.solrstart.version.since;

import com.sun.javadoc.*;
import com.sun.tools.doclets.internal.toolkit.Configuration;
import com.sun.tools.doclets.internal.toolkit.Content;
import com.sun.tools.doclets.internal.toolkit.WriterFactory;
import com.sun.tools.doclets.internal.toolkit.util.ClassTree;
import com.sun.tools.doclets.internal.toolkit.util.MessageRetriever;
import com.sun.tools.javadoc.Main;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import javax.tools.JavaFileManager;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by arafalov on 10/14/17.
 */
public class Indexer {

    public static TreeMap<String, String> firstOccurance = new TreeMap<>();
    public static TreeMap<String, String> classToFile = new TreeMap<>();
    public static TreeMap<String, TreeSet<String>> versionToFiles = new TreeMap<>();

    public static String hierarchyRoot;

    public static void main(String[] args) throws IOException {
        String gitRepoRoot = args[0];
        String[] tagsSequence = args[1].split(";");
        String sourcePath = args[2];
        String subpackages = args[3];
        hierarchyRoot = args[4]; //full class name to find all children for

        Main.execute("JavadocIndexer", "com.solrstart.version.since.Indexer", new String[] {
                "-sourcepath", sourcePath,
                "-subpackages", subpackages
        });


        System.out.println("Check classes against git repo");
        File repoDir = new File(gitRepoRoot);
//        String repoName = repoDir.getName(); //just last component
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists()) {
            System.err.println("Git directory is not found at: " + gitDir.getAbsolutePath());
            return;
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repo = builder.setGitDir(gitDir).build();
        for (String tagName : tagsSequence) {
            Ref tagRef = repo.findRef(tagName);
            System.out.printf("CHECKING TAG %s (%s)\n", tagName, tagRef.getName());
            RevTree treeAtTag = repo.parseCommit(tagRef.getObjectId()).getTree();

            try(TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(treeAtTag);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String fileName = treeWalk.getNameString();
//                    System.out.printf("Checking: %s\n", name);
                    if (firstOccurance.containsKey(fileName) && firstOccurance.get(fileName) == null) {
                        String tagNameVersion = tagName.substring(tagName.lastIndexOf('/') + 1); //remove leading tag path
                        String filePath = treeWalk.getPathString();
                        System.out.printf("Found %s at %s in version %s\n",
                                fileName, filePath, tagNameVersion);
                        firstOccurance.put(fileName, tagNameVersion);
                        if (versionToFiles.containsKey(tagNameVersion)){
                            versionToFiles.get(tagNameVersion).add(filePath);
                        } else {
                            TreeSet<String> filePaths = new TreeSet<>();
                            filePaths.add(filePath);
                            versionToFiles.put(tagNameVersion, filePaths);
                        }

                    }
                }
            }

        }

        System.out.println("-------------------------------------------------------------------");
        for (String fileName : firstOccurance.keySet()) {
            String tagName = firstOccurance.get(fileName);
            if (tagName != null) {
                System.out.printf("%s first shows up in %s\n", fileName, tagName);
            } else {
                System.out.printf("%s NOT FOUND AT ALL\n", fileName);
            }

        }

        System.out.println("-------------------------------------------------------------------");
        for (String version : versionToFiles.keySet()) {
            TreeSet<String> paths = versionToFiles.get(version);
            System.out.printf("FOUND %d files at version: %s\n", paths.size(), version);
            for (String path : paths) {
                System.out.print(path);
                System.out.print(' ');
            }
            System.out.println();
            System.out.println();
        }
        System.out.println("-------------------------------------------------------------------");

    }

    public static boolean start(RootDoc root) {
        System.out.println("ClassDoc packages: " + root.classes().length);

        ClassDoc hierarchyRootDoc = root.classNamed(hierarchyRoot);
        if (hierarchyRootDoc == null) {
            System.err.printf("Root class not found: '%s'\n", hierarchyRoot);
            System.exit(-2);
        }
        ClassTree javadocTree = new ClassTree(root, new EmptyJavadocConfiguration());

        List<ClassDoc> classDocs = javadocTree.allSubs(hierarchyRootDoc, false);
        classDocs.add(hierarchyRootDoc); //not to forget the main one


//        for (ClassDoc classDoc : root.classes()) {
        for (ClassDoc classDoc : classDocs) {
            Tag[] sinceTags = classDoc.tags("since");
            String sinceVal = null;
            if (sinceTags != null && sinceTags.length > 0) {
                if (sinceTags.length > 1) {
                    sinceVal = "Multiple since tags!";
                } else {
                    sinceVal = sinceTags[0].text();
                }
            }


            String containingFile = classDoc.position().file().getName();
            String className = classDoc.name();
            System.out.printf("Class: %s (%s): @since: %s\n", className, containingFile, (sinceVal==null)?"not found": sinceVal);
            classToFile.put(className, containingFile);
            if (firstOccurance.containsKey(containingFile)) {
                System.err.printf("Already stored %s from %s, must be an inner class; skipping!\n", className, containingFile);
            } else {
                firstOccurance.put(containingFile, sinceVal);
            }
        }
        return true;
    }

    /**
     * Keep minimal, as we are not actually writing anything out. Just reading.
     */
    private static class EmptyJavadocConfiguration extends Configuration {

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

}
