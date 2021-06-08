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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GitCacheTest
{
    @Test
    @Disabled("Not functioning yet")
    public void testGitCache() throws IOException
    {
        Path cloneDir = GitUtil.findGitRoot();
        Git git = Git.open(cloneDir.toFile());
        GitCache cache = new GitCache(git);
        Set<String> diffPaths = cache.getPaths("101d803ee281bf1de8acb71f76169ba39ca2806b");
        assertEquals(3, diffPaths.stream().filter((filename) -> filename.startsWith("src/")).count());
        Set<String> branchesContaining =
            cache.getBranchesContaining("101d803ee281bf1de8acb71f76169ba39ca2806b");
        assertNotNull(branchesContaining);
    }
}
