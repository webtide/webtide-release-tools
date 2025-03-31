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
import java.nio.file.Path;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ChangelogCacheTest
{
    @Test
    public void testCache() throws IOException
    {
        String sha = "03984b49615e1cec8ba7edf82f0117a35dc0869a";
        Path cloneDir = GitUtil.findGitRoot();
        Git git = Git.open(cloneDir.toFile());
        ChangelogCache cache = new ChangelogCache(git);
        Set<String> diffPaths = cache.getPaths(sha);
        assertEquals(35, diffPaths.stream().filter((filename) -> filename.startsWith("src/")).count());
        Set<String> branchesContaining = cache.getBranchesContaining(sha);
        assertNotNull(branchesContaining);
    }
}
