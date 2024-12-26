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
                          String date, Path outputFile)
{

    public enum OUTPUT_FORMAT
    {
        MARKDOWN, TAG_TXT
    }
}
