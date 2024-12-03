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
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RemoteConfig;
import org.junit.jupiter.api.Test;

public class ChangelogToolTest
{
    @Test
    public void testFindGithubRepo() throws IOException, GitAPIException
    {
        Path gitRoot = GitUtil.findGitRoot();
        try (Git git = Git.open(gitRoot.toFile()))
        {
            List<RemoteConfig> remotes = git.remoteList().call();
            for (RemoteConfig remote : remotes)
            {
                System.out.printf("remote: [%s]%n", remote.getName());
                remote.getURIs().forEach((urIish) ->
                {
                    System.out.printf("   %s%n", urIish.toASCIIString());
                    System.out.printf("   .user = %s%n", urIish.getUser());
                    System.out.printf("   .host = %s%n", urIish.getHost());
                    System.out.printf("   .port = %d%n", urIish.getPort());
                    System.out.printf("   .path = %s%n", urIish.getPath());
                });
            }
        }
    }
}
