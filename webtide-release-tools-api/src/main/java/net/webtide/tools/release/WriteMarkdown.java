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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WriteMarkdown implements WriteOutput
{
    @Override
    public void write(ChangeMetadata changeMetadata) throws IOException
    {
        Path markdown = changeMetadata.config().getOutputPath().resolve("changelog.md");
        try (BufferedWriter writer = Files.newBufferedWriter(markdown, UTF_8);
             PrintWriter out = new PrintWriter(writer))
        {
            List<Change> relevantChanges = changeMetadata.changelog().stream()
                .filter(Predicate.not(Change::isSkip))
                .sorted(Comparator.comparingInt(Change::getRefNumber).reversed())
                .toList();

            // Collect list of community member participation
            Set<String> community = new HashSet<>();
            for (Change change : relevantChanges)
            {
                for (Author author : change.getAuthors())
                {
                    if (!author.committer())
                    {
                        community.add(String.format("%s (%s)", author.toNiceName(), author.name()));
                    }
                }
            }

            if (!community.isEmpty())
            {
                out.println("# Special Thanks to the following Eclipse Jetty community members");
                out.println();
                community.forEach((author) -> out.printf("* %s%n", author));
                out.println();
            }

            // resolve titles, ids, etc ....
            writeSection(out, "# Changelog", relevantChanges.stream().filter((c) ->
                !c.hasLabel("dependencies")));

            if (changeMetadata.config().isIncludeDependencyChanges())
            {
                // Write out a filtered dependabot "Bump <dep> from <oldver> to <newver>" detail
                writeSection(out, "# Dependencies", relevantChanges.stream()
                    .filter((c) -> c.hasLabel("dependencies"))
                    .map(new Dependencies.BumpFromToSimplifier())
                    .sorted(new Dependencies.BumpToComparator())
                    .filter(new Dependencies.BumpDistinct())
                );
            }
        }
    }

    private void writeSection(PrintWriter out, String sectionName, Stream<Change> changesStream)
    {
        List<Change> changes = changesStream.toList();
        if (!changes.isEmpty())
        {
            out.println();
            out.println(sectionName);
            out.println();

            for (Change change : changes)
            {
                out.printf("+ #%d - ", change.getRefNumber());
                out.print(change.getRefTitle());
                Set<String> authors = change.getAuthors().stream().filter(Predicate.not(Author::committer)).map(Author::toNiceName).collect(Collectors.toSet());
                if (!authors.isEmpty())
                {
                    out.printf(" (%s)", String.join(", ", authors));
                }
                out.print("\n");
            }
        }
    }
}
