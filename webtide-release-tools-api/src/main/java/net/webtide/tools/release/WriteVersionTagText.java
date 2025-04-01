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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class WriteVersionTagText implements WriteOutput
{
    public static final String FILENAME = "version-tag.txt";

    @Override
    public void write(ChangeMetadata changeMetadata) throws IOException
    {
        Path versionTxt = changeMetadata.config().getOutputPath().resolve(FILENAME);
        try (BufferedWriter writer = Files.newBufferedWriter(versionTxt, UTF_8);
             PrintWriter out = new PrintWriter(writer))
        {
            List<Change> relevantChanges = changeMetadata.changelog().stream()
                .filter(Predicate.not(Change::isSkip))
                .sorted(Comparator.comparingInt(Change::getRefNumber).reversed())
                .toList();

            writeSection(out, " " + changeMetadata.projectVersion() + " - " + changeMetadata.date(), relevantChanges.stream().filter((c) ->
                !c.hasLabel("dependencies")));
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
