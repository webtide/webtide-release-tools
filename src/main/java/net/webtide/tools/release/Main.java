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

import java.nio.file.Files;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        Config config = Config.parseArgs(new Args(args));

        try (ChangelogTool changelog = new ChangelogTool(config))
        {
            // equivalent of git log <old>..<new>
            changelog.resolveCommits();

            // resolve all title/body fields (in commits, issues, and prs) for textual issues references (recursively)
            changelog.resolveIssues();

            // resolve all of the issue and pull requests commits
            changelog.resolveIssueCommits();

            // link up commits / issues / pull requests
            changelog.linkActivity();

            System.out.printf("Found %,d commit entries%n", changelog.getCommits().size());
            System.out.printf("Found %,d issue/pr references%n", changelog.getIssues().size());

            if (!Files.exists(config.outputPath))
            {
                Files.createDirectories(config.outputPath);
            }

            changelog.save(config.outputPath);
        }
    }
}
