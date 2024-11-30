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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.webtide.tools.github.GitHubApi;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import net.webtide.tools.github.gson.PathTypeAdapter;
import org.eclipse.jetty.toolchain.test.MavenPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ChangeIssueTest
{
    private Gson gson;

    @BeforeEach
    public void initGson()
    {
        gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }

    @Test
    public void testIsSkipped() throws IOException
    {
        Map<Integer, ChangeIssue> issues = loadIssues("changes-12_0_1/change-issues.json");
        ChangeIssue issue10330 = issues.get(10330);
        assertFalse(issue10330.isSkipped());
    }

    @Test
    public void testIssueHasBaseRef() throws IOException
    {
        Map<Integer, ChangeIssue> issues = loadIssues("changes-12_0_1/change-issues.json");
        ChangeIssue issue10330 = issues.get(10330);
        assertThat("issue should have base-ref", issue10330.getBaseRef(), is("jetty-12.0.x"));
        Map<String, ChangeCommit> commits = loadCommits("changes-12_0_1/change-commits.json");
        int foundCommits = 0;
        for (String sha : issue10330.getCommits())
        {
            ChangeCommit commit = commits.get(sha);
            if (commit != null)
                foundCommits++;
        }
        assertThat("Found Commits", foundCommits, greaterThan(0));
    }

    @Test
    public void testIssueWithPullRequest() throws IOException, InterruptedException
    {
        GitHubApi github = GitHubApi.connect();
        net.webtide.tools.github.Issue ghIssue = github.issue("jetty", "jetty.project", 10330);
        System.out.printf("ghIssue.pullRequest = %s%n", ghIssue.getPullRequest());
    }

    private Map<Integer, ChangeIssue> loadIssues(String path) throws IOException
    {
        Path issuesFile = MavenPaths.findTestResourceFile(path);

        try (BufferedReader reader = Files.newBufferedReader(issuesFile, StandardCharsets.UTF_8))
        {
            return Stream.of(gson.fromJson(reader, ChangeIssue[].class))
                .collect(Collectors.toMap(ChangeIssue::getNum, value -> value));
        }
    }

    private Map<String, ChangeCommit> loadCommits(String path) throws IOException
    {
        Path commitsFile = MavenPaths.findTestResourceFile(path);

        try (BufferedReader reader = Files.newBufferedReader(commitsFile, StandardCharsets.UTF_8))
        {
            return Stream.of(gson.fromJson(reader, ChangeCommit[].class))
                .collect(Collectors.toMap(ChangeCommit::getSha, value -> value));
        }
    }
}
