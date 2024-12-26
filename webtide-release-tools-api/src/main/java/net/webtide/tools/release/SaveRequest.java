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

import java.nio.file.Path;

public record SaveRequest(Path outputDir, boolean includeDependencyChanges, OUTPUT_FORMAT outputFormat, String projectVersion,
                          String date)
{
    public enum OUTPUT_FORMAT
    {
        MARKDOWN("changelog.md"),
        TAG_TXT("version-tag.txt");

        String filename;

        OUTPUT_FORMAT(String filename)
        {
            this.filename = filename;
        }

        public String getFilename()
        {
            return filename;
        }
    }
}
