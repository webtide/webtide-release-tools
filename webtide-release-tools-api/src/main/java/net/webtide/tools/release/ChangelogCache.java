//
// ========================================================================
// Copyright (c) Webtide LLC and others.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: Apache-2.0
// ========================================================================
//

package net.webtide.tools.release;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChangelogCache implements AutoCloseable
{
    private final static Logger LOG = LoggerFactory.getLogger(ChangelogCache.class);
    private final Git git;
    private final Repository repository;
    private final RevWalk revWalker;
    private final Gson gson;
    private final Path commitsCache;
    private final Commits commits;

    public ChangelogCache(Git git)
    {
        this(git, resolveCacheFile(git.getRepository()));
    }

    public ChangelogCache(Git git, Path cacheFile)
    {
        this.git = git;
        this.repository = git.getRepository();
        this.revWalker = new RevWalk(this.repository);
        this.gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        LOG.info("Git Cache: {}", cacheFile);
        this.commitsCache = cacheFile;
        this.commits = loadCommitsCache();
    }

    private static Path resolveCacheFile(Repository repository)
    {
        Path gitPath = repository.getDirectory().toPath();
        if (gitPath.getFileName().toString().equals(".git"))
        {
            gitPath = gitPath.getParent();
        }
        Path configRoot = Paths.get(System.getProperty("user.home"), ".cache", "git-changelog", gitPath.getFileName().toString());
        LOG.debug("Git Cache: {}", configRoot);

        try
        {
            FS.ensureDirectoryExists(configRoot);
        }
        catch (IOException e)
        {
            LOG.warn("Unable to create config root directories: {}", configRoot, e);
        }

        return configRoot.resolve("commits.json");
    }

    @Override
    public void close()
    {
        this.revWalker.close();
    }

    public Set<String> getBranchesContaining(String sha)
    {
        String commitId = Sha.toLowercase(sha);
        Commit commit = getCommit(commitId);
        Set<String> branches = commit.getBranches();
        if (branches == null)
        {
            // look up from git
            branches = getGitBranchesContaining(commitId);
            commit.setBranches(branches);
            commits.putCommit(commit);
            save();
        }
        return branches;
    }

    public Set<String> getPaths(String sha)
    {
        String commitId = Sha.toLowercase(sha);
        Commit commit = getCommit(commitId);
        Set<String> paths = commit.getDiffPaths();
        if (paths == null)
        {
            // look up from git
            paths = getGitCommitPaths(ObjectId.fromString(commitId));
            commit.setDiffPaths(paths);
            commits.putCommit(commit);
            save();
        }
        return paths;
    }

    private Set<String> collectPathsInCommit(RevCommit commit) throws IOException, GitAPIException
    {
        final String sha = commit.getId().getName();
        final List<DiffEntry> diffs = git.diff()
            .setOldTree(prepareTreeParser(sha + "^"))
            .setNewTree(prepareTreeParser(sha))
            .setPathFilter(TreeFilter.ANY_DIFF)
            .setShowNameOnly(true)
            .call();

        final Set<String> paths = new HashSet<>();

        for (DiffEntry diff : diffs)
        {
            paths.add(diff.getOldPath());
            paths.add(diff.getNewPath());
        }

        return paths;
    }

    private Commit getCommit(String commitId)
    {
        String sha = Sha.toLowercase(commitId);
        Commit commit = commits.getCommit(sha);
        if (commit == null)
        {
            commit = new Commit();
            commit.setSha(sha);
            commits.putCommit(commit);
        }
        return commit;
    }

    private Set<String> getGitBranchesContaining(String sha)
    {
        try
        {
            return git.branchList()
                .setListMode(ListBranchCommand.ListMode.ALL)
                .setContains(sha)
                .call()
                .stream()
                .map(Ref::getName)
                .collect(Collectors.toSet());
        }
        catch (GitAPIException e)
        {
            throw new ChangelogException("Unable to query git for branches containing: " + sha, e);
        }
    }

    private Set<String> getGitCommitPaths(ObjectId commitId)
    {
        try
        {
            RevCommit revCommit = revWalker.parseCommit(commitId);
            return collectPathsInCommit(revCommit);
        }
        catch (IOException | GitAPIException e)
        {
            throw new ChangelogException("Unable to get diff paths for commit: " + commitId, e);
        }
    }

    private Commits loadCommitsCache()
    {
        if (Files.exists(commitsCache))
        {
            try (BufferedReader reader = Files.newBufferedReader(commitsCache, UTF_8);
                 JsonReader jsonReader = gson.newJsonReader(reader))
            {
                return gson.fromJson(jsonReader, Commits.class);
            }
            catch (IOException e)
            {
                LOG.warn("Unable to load: {}", commitsCache, e);
            }
        }
        return new Commits();
    }

    private AbstractTreeIterator prepareTreeParser(String objectId) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository))
        {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader())
            {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();
            return treeParser;
        }
    }

    private void save()
    {
        try (BufferedWriter writer = Files.newBufferedWriter(commitsCache, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(commits, Commits.class, jsonWriter);
        }
        catch (IOException e)
        {
            LOG.warn("Unable to save: {}", commitsCache, e);
        }
    }

    public static class Commits
    {
        @SerializedName("commits")
        private Map<String, Commit> commitsMap = new HashMap<>();

        public Commit getCommit(String sha)
        {
            return commitsMap.get(sha);
        }

        public void putCommit(Commit commit)
        {
            commitsMap.put(commit.getSha(), commit);
        }
    }

    public static class Commit
    {
        private String sha;
        private Set<String> branches;
        private Set<String> diffPaths;

        public Set<String> getBranches()
        {
            return branches;
        }

        public void setBranches(Set<String> branches)
        {
            this.branches = branches;
        }

        public Set<String> getDiffPaths()
        {
            return diffPaths;
        }

        public void setDiffPaths(Set<String> diffPaths)
        {
            this.diffPaths = diffPaths;
        }

        public String getSha()
        {
            return sha;
        }

        public void setSha(String sha)
        {
            this.sha = Sha.toLowercase(sha);
        }
    }
}
