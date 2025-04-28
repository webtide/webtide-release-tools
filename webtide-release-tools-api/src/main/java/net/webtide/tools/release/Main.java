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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        Config config = Config.parseArgs(new Args(args));

        try (ChangelogTool tool = new ChangelogTool(config))
        {
            tool.discoverChanges();

            System.out.printf("Found %,d commit entries%n", tool.getCommits().size());
            System.out.printf("Found %,d issue/pr references%n", tool.getIssues().size());
            System.out.printf("Found %,d changes%n", tool.getChangelog().size());

            FS.ensureDirectoryExists(config.outputPath);

            String projectVersion = config.getRefVersionCurrent();
            ZonedDateTime versionDate = tool.getCurrentVersionCommitterWhen();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            String date = formatter.format(versionDate);

            if (config.getOutputTypes().isEmpty())
                config.getOutputTypes().add(WriteOutput.Type.MARKDOWN);

            ChangeMetadata saveRequest = new ChangeMetadata(config,
                projectVersion,
                date,
                tool.getChangelog());
            tool.save(saveRequest);
            System.out.printf("Wrote changelog to %s%n", config.outputPath.toAbsolutePath());
        }
    }
}
