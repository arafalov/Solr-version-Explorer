package com.solrstart.version.since;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;
import com.sun.tools.doclets.internal.toolkit.util.ClassTree;
import com.sun.tools.javadoc.Main;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by arafalov on 10/14/17.
 */
public class Indexer {

    private static TreeMap<String, String> firstOccurance = new TreeMap<>();
    private static TreeMap<String, String> classToFile = new TreeMap<>();
    private static TreeMap<String, TreeSet<String>> tagToFiles = new TreeMap<>();

    private static String[] hierarchyRoots;

    public static void main(String[] args) throws IOException {
        String propertiesFilePath = args[0];
        Properties props = new Properties();
        props.load(new FileReader(propertiesFilePath));

        // JavaDoc likes its paths' separated by colons.
        // Let's do it for our tags list too.

        String gitRepoRoot = props.getProperty("gitRepoRoot");

        //release tags, but could also include an active branch to pick up next-version files
        String[] tagsList = props.getProperty("tagsList").split(":");

        String sourcePathList = props.getProperty("sourcePathList");
        String subpackagesList = props.getProperty("subpackagesList");  // actually, prefixes list
        hierarchyRoots = props.getProperty("hierarchyRoots").split(":"); //full class name to find all children for


        // Run Javadoc, which will create an instance of this class and run its start() method
        // We get the results back through the shared static variables
        // Parameters: nameForMessages, docletName, parameters
        Main.execute("JavadocIndexer", Indexer.class.getCanonicalName(), new String[] {
                "-sourcepath", sourcePathList,
                "-subpackages", subpackagesList
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
        for (String tagName : tagsList) {
            Ref tagRef = repo.findRef(tagName);
            System.out.printf("CHECKING TAG %s (%s)\n", tagName, tagRef.getName());
            RevTree treeAtTag = repo.parseCommit(tagRef.getObjectId()).getTree();

            try(TreeWalk treeWalk = new TreeWalk(repo)) {
                treeWalk.addTree(treeAtTag);
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String fileName = treeWalk.getNameString();

                    if (firstOccurance.containsKey(fileName) && firstOccurance.get(fileName) == null) {
                        // we do not have explicit @version tag yet for this file
                        String tagNameVersion = tagName.substring(tagName.lastIndexOf('/') + 1); //remove leading tag path
                        String filePath = treeWalk.getPathString();
                        System.out.printf("Found %s at %s in version %s\n",
                                fileName, filePath, tagNameVersion);
                        firstOccurance.put(fileName, tagNameVersion);
                        if (tagToFiles.containsKey(tagNameVersion)){
                            tagToFiles.get(tagNameVersion).add(filePath);
                        } else {
                            TreeSet<String> filePaths = new TreeSet<>();
                            filePaths.add(filePath);
                            tagToFiles.put(tagNameVersion, filePaths);
                        }

                    }
                }
            }

        }

        ArrayList<String> futureTag = new ArrayList<>();
        System.out.println("-------------------------------------------------------------------");
        for (String fileName : firstOccurance.keySet()) {
            String tagName = firstOccurance.get(fileName);
            if (tagName != null) {
                System.out.printf("%s first shows up in %s\n", fileName, tagName);
            } else {
                System.out.printf("%s NOT FOUND AT KNOWN TAG/BRANCH\n", fileName);
                futureTag.add(fileName);
            }

        }

        System.out.println("-------------------------------------------------------------------");
        for (String tag : tagToFiles.keySet()) {
            TreeSet<String> paths = tagToFiles.get(tag);
            System.out.printf("FOUND %d un-@version-ed files at tag/branch: %s\n", paths.size(), tag);
            for (String path : paths) {
                System.out.print(path);
                System.out.print(' ');
            }
            System.out.println();
            System.out.println();
        }

        System.out.printf("FOUND %d files without known tag\n", futureTag.size());
        for (String fileName: futureTag) {
            System.out.print(fileName);
            System.out.print(' ');
        }
        System.out.println();
        System.out.println();

        System.out.println("-------------------------------------------------------------------");

    }

    /**
     * Called by the Javadoc engine
     * @param root - root of all classes
     * @return true - always, as all work is created on the side
     */
    public static boolean start(RootDoc root) {
        System.out.println("ClassDoc packages: " + root.classes().length);

        for (String hierarchyRoot: hierarchyRoots) {
            ClassDoc hierarchyRootDoc = root.classNamed(hierarchyRoot);
            if (hierarchyRootDoc == null) {
                System.err.printf("Root class not found: '%s'\n", hierarchyRoot);
                System.exit(-2);
            }
            ClassTree javadocTree = new ClassTree(root, new EmptyJavadocConfiguration());

            List<ClassDoc> classDocs;

            if (hierarchyRootDoc.isInterface()) {
                classDocs = javadocTree.implementingclasses(hierarchyRootDoc);
            } else {
                classDocs = javadocTree.allSubs(hierarchyRootDoc, false);
            }
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
                System.out.printf("Class: %s (%s): @since: %s\n", className, containingFile, (sinceVal == null) ? "not found" : sinceVal);
                classToFile.put(className, containingFile);
                if (firstOccurance.containsKey(containingFile)) {
                    System.err.printf("Already stored %s from %s, must be an inner class; skipping!\n", className, containingFile);
                } else {
                    firstOccurance.put(containingFile, sinceVal);
                }
            }
        }
        return true;
    }

}
