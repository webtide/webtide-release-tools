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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import net.webtide.tools.github.gson.PathTypeAdapter;
import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Test;

public class ConfigTest
{
    @Test
    public void testWriteConfig() throws IOException
    {
        Config config = new Config();
        config.setRepoPath(GitUtil.findGitRoot());

        Path outputDir = MavenTestingUtils.getTargetTestingPath("testWriteConfig");
        FS.ensureEmpty(outputDir);
        config.setOutputPath(outputDir);

        Path outputFile = outputDir.resolve("config.json");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile))
        {
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
                .registerTypeHierarchyAdapter(Path.class, new PathTypeAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
            JsonWriter jsonWriter = gson.newJsonWriter(writer);
            jsonWriter.setIndent("  ");
            jsonWriter.setSerializeNulls(true);
            gson.toJson(config, Config.class, jsonWriter);
        }
        System.out.println("Wrote: " + outputFile);
    }
}
