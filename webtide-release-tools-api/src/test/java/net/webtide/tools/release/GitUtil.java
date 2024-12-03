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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assumptions;

public class GitUtil
{
    public static Path findGitRoot()
    {
        return findGitRoot(Paths.get(System.getProperty("user.dir")));
    }

    public static Path findGitRoot(Path path)
    {
        Assumptions.assumeTrue(path != null);

        Path gitDir = path.resolve(".git");
        if (Files.exists(gitDir) && Files.isDirectory(gitDir))
            return path;
        return path.getParent();
    }
}
