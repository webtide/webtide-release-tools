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

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.webtide.tools.github.Commit;
import net.webtide.tools.github.CrossReference;
import net.webtide.tools.github.GitHubApi;
import net.webtide.tools.github.GitHubResourceNotFoundException;
import net.webtide.tools.github.Issue;
import net.webtide.tools.github.IssueEvents;
import net.webtide.tools.github.Label;
import net.webtide.tools.github.PullRequestCommits;
import net.webtide.tools.github.PullRequests;
import net.webtide.tools.github.cache.PersistentCache;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChangelogTool implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(ChangelogTool.class);

    private final Git git;
    private final Repository repository;
    private final ChangelogCache changelogCache;
    private final Authors authors = Authors.load();
    private final Changelog changelog = new Changelog();
    private final Map<Integer, ChangeIssue> issueMap = new HashMap<>();
    private final Map<String, ChangeCommit> commitMap = new HashMap<>();
    private final List<Predicate<String>> branchExclusion = new ArrayList<>();
    private final List<Predicate<String>> commitPathExclusionFilters = new ArrayList<>();
    private final Set<String> excludedLabels = new HashSet<>();
    private String githubOwner;
    private String githubRepoName;
    private GitHubApi github;
    private Path gitCacheDir;
    private String branch;
    private String tagOldVersion;
    private String refCurrentVersion;
    private Predicate<String> branchesExclusionPredicate;

    public ChangelogTool(Path localGitRepo) throws IOException
    {
        git = Git.open(localGitRepo.toFile());
        repository = git.getRepository();
        changelogCache = new ChangelogCache(git);
        System.out.println("Repository: " + repository);
    }

    public ChangelogTool(Config config) throws IOException
    {
        this(config.getRepoPath());
        setGithubRepo(config.getGithubRepoOwner(), config.getGithubRepoName());
        setBranch(config.getBranch());
        setVersionRange(config.getTagVersionPrior(), config.getRefVersionCurrent());
        setGitCacheDir(config.getGitCacheDir());
        config.getLabelExclusions().forEach(this::addLabelExclusion);
        config.getCommitPathRegexExclusions().forEach(this::addCommitPathRegexExclusion);
        config.getBranchRegexExclusions().forEach(this::addBranchRegexExclusion);
    }

    /**
     * If commit has specific branch, do not include it in the results.
     */
    public void addBranchExclusion(Predicate<String> predicate)
    {
        this.branchExclusion.add(predicate);
    }

    /**
     * Exclude paths on commit with this predicate.
     * If the resulting commit is devoid of paths as a result, it is flagged as skipped.
     */
    public void addCommitPathExclusionFilter(Predicate<String> predicate)
    {
        Objects.requireNonNull(predicate, "predicate");
        this.commitPathExclusionFilters.add(predicate);
    }

    /**
     * Exclude paths on commit with this predicate.
     * If the resulting commit is devoid of paths as a result, it is flagged as skipped.
     */
    public void addCommitPathRegexExclusion(String regex)
    {
        Objects.requireNonNull(regex, "regex");
        this.commitPathExclusionFilters.add((filename) -> filename.matches(regex));
    }

    public void addLabelExclusion(String label)
    {
        this.excludedLabels.add(label);
    }

    @Override
    public void close()
    {
        this.changelogCache.close();
        this.repository.close();
        this.git.close();
    }

    public void discoverChanges() throws IOException, InterruptedException, GitAPIException
    {
        branchesExclusionPredicate = newStringPredicate(branchExclusion);

        // equivalent of git log <old>..<new>
        discoverCommitsInRange();

        // recursively find issue and pull request references in commits
        discoverChangesRecursively();

        // resolve issue/PR relevancy
        resolveRelevancy();

        // resolve all the discovered commits and issues into a set of changes
        resolveChanges();
    }

    private void discoverChangesRecursively() throws IOException, InterruptedException
    {
        boolean needsResolve = true;

        while (needsResolve)
        {
            needsResolve = false;

            if (hasUnResolvedCommits())
            {
                resolveCommits();
                needsResolve = true;
            }

            if (hasUnResolvedIssues())
            {
                resolveIssues();
                needsResolve = true;
            }
        }
    }

    private void resolveCommits() throws IOException, InterruptedException
    {
        Set<String> unresolvedShas = commitMap.values()
            .stream()
            .filter(c -> !c.isResolved())
            .map(ChangeCommit::getSha)
            .collect(Collectors.toSet());

        LOG.info("Need to resolve {} more commits", unresolvedShas.size());

        for (String sha : unresolvedShas)
        {
            ChangeCommit changeCommit = getCommit(sha);

            if (changeCommit.isResolved())
                continue; // skip

            try (RevWalk walk = new RevWalk(repository))
            {
                ObjectId commitId = ObjectId.fromString(sha);
                RevCommit commit = walk.parseCommit(commitId);
                LOG.debug("Found commit: {} - {}", commit.getId().getName(), commit.getShortMessage());
                resolveCommit(changeCommit, commit);
            }
            catch (MissingObjectException moe)
            {
                changeCommit.addSkipReason(Skip.GIT_OBJ_MISSING);
                changeCommit.setResolved();
            }
        }
    }

    private boolean hasUnResolvedCommits()
    {
        return commitMap.values().stream()
            .anyMatch(c -> !c.isResolved());
    }

    private boolean hasUnResolvedIssues()
    {
        return issueMap.values().stream()
            .anyMatch(c -> !c.isResolved());
    }

    public Changelog getChangelog()
    {
        return changelog;
    }

    public Collection<ChangeCommit> getCommits()
    {
        return this.commitMap.values();
    }

    public ZonedDateTime getCurrentVersionCommitterWhen() throws IOException
    {
        RevCommit commit = findCommitForCurrent();
        if (commit == null)
            return ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("GMT"));
        PersonIdent committerIdent = commit.getCommitterIdent();
        Instant instant = committerIdent.getWhenAsInstant();
        return ZonedDateTime.ofInstant(instant, committerIdent.getZoneId());
    }

    public Path getGitCacheDir()
    {
        return gitCacheDir;
    }

    public void setGitCacheDir(Path gitCacheDir)
    {
        if (gitCacheDir == null)
        {
            this.gitCacheDir = null;
            return;
        }

        this.gitCacheDir = gitCacheDir.toAbsolutePath();
        try
        {
            FS.ensureDirectoryExists(this.gitCacheDir);
        }
        catch (IOException e)
        {
            LOG.warn("Unable to create git cache dir: {}", this.gitCacheDir, e);
        }
    }

    private ChangeIssue getIssue(int num)
    {
        ChangeIssue issue = this.issueMap.get(num);
        if (issue == null)
        {
            issue = new ChangeIssue(num);
            issueMap.put(num, issue);
        }
        return issue;
    }

    public Collection<ChangeIssue> getIssues()
    {
        return this.issueMap.values();
    }

    private void saveLog(Gson gson, Path logFile, Object obj, Class<?> clazz) throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(obj, clazz, jsonWriter);
        }
    }

    public void save(ChangeMetadata changeMetadata) throws IOException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        Path outputDir = changeMetadata.config().getOutputPath();

        saveLog(gson, outputDir.resolve("authors-scan.json"), authors, Authors.class);
        saveLog(gson, outputDir.resolve("change-issues.json"), issueMap.values(), Set.class);
        saveLog(gson, outputDir.resolve("change-issues-relevant.json"), getRelevantKnownIssues(), List.class);
        saveLog(gson, outputDir.resolve("change-commits.json"), commitMap.values(), Set.class);
        saveLog(gson, outputDir.resolve("change-groups.json"), changelog, List.class);

        Path changePaths = changeMetadata.config().getOutputPath().resolve("change-paths.log");
        try (BufferedWriter writer = Files.newBufferedWriter(changePaths))
        {
            Set<String> changedFiles = new HashSet<>();
            for (ChangeCommit commit : commitMap.values())
            {
                if (commit.isSkipped())
                    continue;
                if (commit.getFiles() != null)
                    changedFiles.addAll(commit.getFiles());
            }
            for (String filename : changedFiles.stream().sorted().toList())
            {
                writer.write(filename);
                writer.write("\n");
            }
            System.out.printf("Found %,d Files changed in the various commits%n", changedFiles.size());
        }

        for (WriteOutput.Type outputType : changeMetadata.config().getOutputTypes())
        {
            try
            {
                WriteOutput writeOutput = outputType.getType().getDeclaredConstructor().newInstance();
                writeOutput.write(changeMetadata);
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public void setGithubRepo(String owner, String repoName)
    {
        this.githubOwner = owner;
        this.githubRepoName = repoName;
    }

    public void setVersionRange(String tagOldVersion, String refCurrentVersion)
    {
        this.tagOldVersion = tagOldVersion;
        this.refCurrentVersion = refCurrentVersion;
    }

    /**
     * If commit has specific branch, do not include it in the results.
     */
    private void addBranchRegexExclusion(String regex)
    {
        Objects.requireNonNull(regex, "regex");
        this.branchExclusion.add((branch) -> branch.matches(regex));
    }

    private RevCommit findCommitForCurrent() throws IOException
    {
        // current version null as we are interested only by current head
        if (refCurrentVersion != null)
        {
            try (RevWalk walk = new RevWalk(repository))
            {
                List<String> refNames = new ArrayList<>();
                if (refCurrentVersion.contains("/"))
                    refNames.add(refCurrentVersion);
                refNames.add("refs/tags/" + refCurrentVersion);
                refNames.add("refs/heads/" + refCurrentVersion);
                refNames.add("refs/remotes/" + refCurrentVersion);
                refNames.add("refs/" + branch + "/HEAD");
                refNames.add(refCurrentVersion); // for commit-ids

                for (String refName : refNames)
                {
                    LOG.debug("Finding commit ref for refs/tags/{}", refName);
                    Ref ref = repository.findRef(refName);
                    if (ref != null)
                    {
                        return walk.parseCommit(ref.getObjectId());
                    }
                }
            }
        }

        // nothing found so we return head of branch
        //Ref headRef = branch == null ? repository.exactRef("HEAD") : repository.findRef(branch);
        Ref headRef = repository.findRef(branch);
        ObjectId headHash = headRef.getObjectId();
        try (RevWalk walk = new RevWalk(repository))
        {
            return walk.parseCommit(headHash);
        }
    }

    private RevCommit findCommitForTag(String tagName) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository))
        {
            String refName = "refs/tags/" + tagName;
            LOG.debug("Finding commit ref {}", refName);
            Ref tagRef = repository.findRef(refName);
            if (tagRef == null)
            {
                throw new ChangelogException("Ref not found: " + tagName);
            }
            return walk.parseCommit(tagRef.getObjectId());
        }
    }

    private Author getAuthor(Authors authors, RevCommit commit)
    {
        Author author = authors.find(commit.getAuthorIdent().getEmailAddress());

        if (author == null)
        {
            author = new Author(commit.getAuthorIdent().getName())
                .email(commit.getAuthorIdent().getEmailAddress())
                .committer(false);

            try
            {
                String commitId = commit.getId().getName();
                Commit ghCommit = getGitHubApi().commit(this.githubOwner, this.githubRepoName, commitId);
                if (ghCommit != null)
                {
                    ChangeCommit changeCommit = getCommit(commitId);
                    changeCommit.setBody(ghCommit.getCommit().getMessage());

                    if (ghCommit.getAuthor() != null)
                    {
                        String githubAuthorLogin = ghCommit.getAuthor().getLogin();
                        author.github(githubAuthorLogin);
                        changeCommit.setAuthor(author);
                    }
                    else
                    {
                        System.out.printf("Has no author: %s%n", commitId);
                    }
                }
                else
                {
                    System.out.printf("Not a valid commit id: %s%n", commitId);
                }
            }
            catch (InterruptedException | IOException e)
            {
                LOG.debug("Ignoring Exception", e);
            }
            authors.add(author);
        }

        return author;
    }

    private ChangeCommit getCommit(String sha)
    {
        String lowerSha = sha.toLowerCase(Locale.US);
        ChangeCommit commit = commitMap.get(lowerSha);
        if (commit == null)
        {
            commit = new ChangeCommit();
            commit.setSha(lowerSha);
            commitMap.put(lowerSha, commit);
        }
        return commit;
    }

    private GitHubApi getGitHubApi() throws IOException, InterruptedException
    {
        if (github == null)
        {
            github = GitHubApi.connect();
            if (gitCacheDir != null && Files.isDirectory(gitCacheDir))
            {
                github.setCache(new PersistentCache(gitCacheDir));
                LOG.info("Git Cache Enabled: {}", gitCacheDir);
            }
            LOG.info("GitHub API Rate Limits: {}", github.getRateLimits());
        }
        return github;
    }

    private List<ChangeIssue> getRelevantKnownIssues()
    {
        return issueMap.values().stream()
            .filter((issue) -> !issue.isSkipped())
            .sorted(Comparator.comparing(ChangeIssue::getNum).reversed())
            .collect(Collectors.toList());
    }

    private Predicate<String> newStringPredicate(Collection<Predicate<String>> filters)
    {
        Predicate<String> predicate = str -> true;
        for (Predicate<String> logPredicate : filters)
        {
            predicate = predicate.and(logPredicate);
        }
        return predicate;
    }

    private boolean isExcludedPath(String path)
    {
        for (Predicate<String> exclusion : commitPathExclusionFilters)
        {
            if (exclusion.test(path))
                return true;
        }
        return false;
    }

    private boolean isMergeCommit(RevCommit commit)
    {
        return ((commit.getParents() != null) && (commit.getParents().length >= 2));
    }

    private void resolveRelevancy()
    {
        for (ChangeIssue issue : issueMap.values())
        {
            int relevantCommitCount = 0;
            for (String sha : issue.getCommits())
            {
                ChangeCommit commit = getCommit(sha);
                if (!commit.isSkipped())
                    relevantCommitCount++;
            }
            if (relevantCommitCount == 0)
            {
                issue.addSkipReason(Skip.NO_RELEVANT_COMMITS);
            }
        }
    }

    private void resolveChanges()
    {
        // Create Change list
        int changeId = 0;
        List<ChangeIssue> relevantIssues = getRelevantKnownIssues();
        System.out.printf("Found %,d relevant issue/pr references%n", relevantIssues.size());
        for (ChangeIssue issue : relevantIssues)
        {
            if (issue.isSkipped() || issue.hasChangeRef())
                continue;

            Change change = new Change(changeId++);
            // add commits
            issue.getCommits().forEach((commitSha) -> updateChangeCommit(change, commitSha));
            // add issues & pull requests
            issue.getReferencedIssues().forEach((issueNum) -> updateChangeIssues(change, issueNum));
            changelog.add(change);
        }

        changelog.forEach((change) -> change.normalize(IssueType.ISSUE));
    }

    private void discoverCommitsInRange() throws IOException, GitAPIException, InterruptedException
    {
        RevCommit commitOld = findCommitForTag(tagOldVersion);
        RevCommit commitNew = findCommitForCurrent();
        LOG.debug("commit log: {} .. {}", commitOld.getId().getName(), commitNew.getId().getName());

        int count = 0;

        LogCommand logCommand = git.log().addRange(commitOld, commitNew);

        for (RevCommit commit : logCommand.call())
        {
            LOG.debug("Found commit: {} - {}", commit.getId().getName(), commit.getShortMessage());
            ChangeCommit changeCommit = getCommit(commit.getId().getName());
            resolveCommit(changeCommit, commit);
            count++;
        }

        LOG.debug("Found {} commits", count);
    }

    private void resolveCommit(ChangeCommit changeCommit, RevCommit commit) throws IOException, InterruptedException
    {
        if (changeCommit.isResolved())
            return;

        String sha = changeCommit.getSha();

        Author author = getAuthor(authors, commit);
        PersonIdent authorIdent = commit.getAuthorIdent();
        changeCommit.setCommitTime(ZonedDateTime.ofInstant(authorIdent.getWhenAsInstant(), authorIdent.getZoneId()));
        changeCommit.setAuthor(author);
        changeCommit.setTitle(commit.getShortMessage());
        changeCommit.setBody(commit.getFullMessage());

        // List of referenced issues and/or prs that are discoverable from this commit
        Set<Integer> allRefs = new HashSet<>();

        if (isMergeCommit(commit))
        {
            changeCommit.addSkipReason(Skip.IS_MERGE_COMMIT);
        }
        else
        {
            // discover any issue/pr references in title or body
            Set<Integer> issueRefs = new HashSet<>();
            issueRefs.addAll(IssueScanner.scan(changeCommit.getTitle()));
            issueRefs.addAll(IssueScanner.scanResolutions(changeCommit.getBody()));
            changeCommit.addIssueRefs(issueRefs);

            allRefs.addAll(issueRefs);

            Set<String> diffPaths = changelogCache.getPaths(sha)
                .stream()
                .filter(Predicate.not(this::isExcludedPath))
                .collect(Collectors.toSet());
            changeCommit.setFiles(diffPaths);
            if (diffPaths.isEmpty())
            {
                changeCommit.addSkipReason(Skip.NO_INTERESTING_PATHS_LEFT);
            }

            // Note: this lookup (all branches that commit exists in) is VERY time consuming.

            Set<String> branchesWithCommit = changelogCache.getBranchesContaining(sha);
            changeCommit.setBranches(branchesWithCommit);
            if (branchesWithCommit.stream().anyMatch(branchesExclusionPredicate))
            {
                changeCommit.addSkipReason(Skip.EXCLUDED_BRANCH);
            }
        }

        // is this commit linked to a PullRequest?
        PullRequests pullRequests = getGitHubApi().commitPullRequests(this.githubOwner, this.githubRepoName, changeCommit.getSha());
        Set<Integer> prRefs = pullRequests.stream().map(Issue::getNumber).collect(Collectors.toSet());
        changeCommit.addPullRequestRefs(prRefs);
        allRefs.addAll(prRefs);

        // Initialize issues/prs found (for later resolve)
        for (int num : allRefs)
        {
            ChangeIssue issue = getIssue(num);
            issue.addCommit(changeCommit.getSha());
        }

        // set this commit as resolved
        changeCommit.setResolved();
    }

    private void resolveIssues()
    {
        LOG.debug("Resolving issue details ...");
        List<ChangeIssue> unresolvedIssues = issueMap.values().stream()
            .filter((issue) -> !issue.isResolved())
            .toList();

        long issuesLeft = unresolvedIssues.size();

        for (ChangeIssue unresolvedIssue : unresolvedIssues)
        {
            LOG.info("Need to resolve {} more issues ...", issuesLeft--);
            resolveIssue(unresolvedIssue);
        }

        LOG.debug("Tracking {} issues", issueMap.size());
    }

    private void resolveIssue(ChangeIssue issue)
    {
        LOG.debug("Resolve Issue: {}", issue);
        if (issue.isResolved())
            return;

        try
        {
            net.webtide.tools.github.Issue ghIssue = getGitHubApi().issue(githubOwner, githubRepoName, issue.getNum());
            issue.addLabels(ghIssue.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));

            if (ghIssue.getPullRequest() != null)
            {
                net.webtide.tools.github.PullRequest ghPullRequest = getGitHubApi().pullRequest(githubOwner, githubRepoName, issue.getNum());
                issue.addLabels(ghPullRequest.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));
                issue.setBaseRef(ghPullRequest.getBase().getRef());
                issue.setTitle(ghPullRequest.getTitle());
                issue.setBody(ghPullRequest.getBody());
                issue.setState(ghPullRequest.getState());
                issue.setType(IssueType.PULL_REQUEST);

                // if the PR have a label and even in part of some exclusion we may want to include it into changelog
                for (String excludeLabel : excludedLabels)
                {
                    if (ghPullRequest.getLabels().stream().anyMatch(label -> label.getName().equals(excludeLabel)))
                    {
                        issue.addSkipReason(Skip.EXCLUDED_LABEL);
                    }
                }

                if (!ghPullRequest.isMerged())
                {
                    issue.addSkipReason(Skip.NOT_CLOSED);
                }

                if (!branch.equals(issue.getBaseRef()))
                {
                    issue.addSkipReason(Skip.NOT_CORRECT_BASE_REF);
                }
            }
            else
            {
                issue.setTitle(ghIssue.getTitle());
                issue.setBody(ghIssue.getBody());
                issue.setState(ghIssue.getState());
                issue.setType(IssueType.ISSUE);
            }

            Set<Integer> issueRefs = new HashSet<>();
            issueRefs.addAll(IssueScanner.scan(issue.getTitle()));
            issueRefs.addAll(IssueScanner.scanResolutions(issue.getBody()));
            issueRefs.remove(issue.getNum()); // remove self
            issue.addReferencedIssues(issueRefs);

            // Discover any newly referenced issue for later resolve
            for (int issueNum : issueRefs)
            {
                issueMap.putIfAbsent(issueNum, new ChangeIssue(issueNum));
            }

            // Test labels
            for (String excludedLabel : excludedLabels)
            {
                if (issue.hasLabel(excludedLabel))
                {
                    issue.addSkipReason(Skip.EXCLUDED_LABEL);
                }
            }

            if (!issue.isSkipped())
            {
                if (issue.getType() == IssueType.ISSUE)
                {
                    IssueEvents ghIssueEvents = getGitHubApi().issueEvents(githubOwner, githubRepoName, issue.getNum());
                    for (IssueEvents.IssueEvent event : ghIssueEvents)
                    {
                        if (!Strings.isNullOrEmpty(event.getCommitId()))
                        {
                            String sha = event.getCommitId();
                            issue.addCommit(sha);
                            ChangeCommit changeCommit = getCommit(sha);
                            changeCommit.addIssueRef(issue.getNum());
                        }
                    }
                    List<CrossReference> crossReferences = getGitHubApi().issueCrossReferences(githubOwner, githubRepoName, issue.getNum());
                    for (CrossReference crossReference : crossReferences)
                    {
                        net.webtide.tools.github.Ref ref = crossReference.getBaseRef();
                        if (ref != null)
                        {
                            issue.setBaseRef(ref.getName());
                        }
                    }
                }
                else if (issue.getType() == IssueType.PULL_REQUEST)
                {
                    PullRequestCommits ghPullRequestCommits = getGitHubApi().pullRequestCommits(githubOwner, githubRepoName, issue.getNum());
                    for (PullRequestCommits.Commit commit : ghPullRequestCommits)
                    {
                        issue.addCommit(commit.getSha());
                        ChangeCommit changeCommit = getCommit(commit.getSha());
                        changeCommit.addIssueRef(issue.getNum());
                    }
                }
            }
        }
        catch (GitHubResourceNotFoundException e)
        {
            issue.setType(IssueType.INVALID);
            issue.addSkipReason(Skip.INVALID_ISSUE_REF);
        }
        catch (IOException | InterruptedException e)
        {
            LOG.warn("Ignore", e);
        }
        issue.setResolved();
    }

    private void updateChangeCommit(Change change, String commitSha)
    {
        String sha = Sha.toLowercase(commitSha);
        ChangeCommit commit = this.commitMap.get(sha);
        if (commit != null)
        {
            if (commit.hasChangeRef() || // does it already have a change reference?
                commit.isSkipped()) // is it skipped for some reason?
                return; // ignore it

            if (LOG.isDebugEnabled())
                LOG.debug("Update Change [{}]: commit: {}", change.getNumber(), commit);
            commit.setChangeRef(change);

            change.addCommit(commit);
            change.addAuthor(commit.getAuthor());

            if (commit.getIssueRefs() != null)
                commit.getIssueRefs().forEach((ref) -> updateChangeIssues(change, ref));
            if (commit.getPullRequestRefs() != null)
                commit.getPullRequestRefs().forEach((ref) -> updateChangeIssues(change, ref));
        }
    }

    private void updateChangeIssues(Change change, int issueNum)
    {
        ChangeIssue issue = this.issueMap.get(issueNum);
        if (issue != null)
        {
            if (issue.isSkipped() || // skipped
                issue.hasChangeRef()) // already assigned to a change ref
                return; // ignore it

            issue.setChangeRef(change);

            if (LOG.isDebugEnabled())
                LOG.debug("Update Change [{}]: issue: {}", change.getNumber(), issue);

            switch (issue.getType())
            {
                case ISSUE:
                    change.addIssue(issue);
                    break;
                case PULL_REQUEST:
                    change.addPullRequest(issue);
                    break;
                default:
                    break;
            }

            issue.getReferencedIssues().forEach((ref) -> updateChangeIssues(change, ref));
            issue.getCommits().forEach((sha) -> updateChangeCommit(change, sha));
        }
    }
}
