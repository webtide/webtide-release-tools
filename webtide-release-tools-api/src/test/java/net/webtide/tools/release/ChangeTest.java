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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;

public class ChangeTest
{
    @Test
    public void testSortDependencyChanges() throws IOException
    {
        List<Change> changes = loadSimpleChangeList(MavenTestingUtils.getTestResourcePathFile("dep-changes-unsorted.txt"));

        List<String> actual = changes.stream()
            .map(new ChangelogTool.DependencyBumpSimplifier())
            .sorted(new ChangelogTool.DependencyBumpComparator())
            .filter(new ChangelogTool.DistinctDependencyBumpRef())
            .map((change) -> String.format("* #%d - %s", change.getRefNumber(), change.getRefTitle()))
            .collect(Collectors.toList());

        List<String> expected = Files.lines(MavenTestingUtils.getTestResourcePathFile("dep-changes-sorted.txt"), StandardCharsets.UTF_8)
            .collect(Collectors.toList());

        MatcherAssert.assertThat("Sorted Dependency Bumps", actual, contains(expected.toArray()));
    }

    private List<Change> loadSimpleChangeList(Path testResource) throws IOException
    {
        Pattern pat = Pattern.compile("^. #([0-9]{4,}) - (.*)$");

        return Files.lines(testResource, StandardCharsets.UTF_8)
            .map((line) ->
            {
                Matcher matcher = pat.matcher(line);
                if (matcher.matches())
                {
                    int num = Integer.parseInt(matcher.group(1));
                    String text = matcher.group(2);
                    Change change = new Change(num);
                    ChangeIssue pr = new ChangeIssue(num);
                    pr.setTitle(text);
                    change.addPullRequest(pr);
                    change.normalize(IssueType.ISSUE);
                    return change;
                }
                return new Change(0);
            })
            .collect(Collectors.toList());
    }
}
