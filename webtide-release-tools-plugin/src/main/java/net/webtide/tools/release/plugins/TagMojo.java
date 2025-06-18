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

package net.webtide.tools.release.plugins;

import net.webtide.tools.release.Config;
import net.webtide.tools.release.WriteOutput;
import net.webtide.tools.release.WriteVersionTagText;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.jgit.api.Git;

import java.io.IOException;

/**
 * Produce a target/version-tag.txt which represents the changes
 * for this particular release.  A file suitable to use as the git tag
 * message body, in tagging the release.
 */
@Mojo(name = "tag", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class TagMojo extends AbstractReleaseToolsPlugin
{

    @Parameter(property = "webtide.release.tools.branch")
    private String branch;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            Config config = buildConfig();
            config.getOutputTypes().add(WriteOutput.Type.VERSION_TXT);
            // should be done from the current branch but just in case this can be used with a parameter
            config.setBranch(branch == null ? getCurrentBranch() : branch);
            doExecute();
            getLog().info("Wrote version tag txt to" + config.getOutputPath().resolve(WriteVersionTagText.FILENAME));
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failed to tag changelog", e);
        }
    }

    protected String getCurrentBranch() throws MojoExecutionException, IOException {
        try (Git git = Git.open(buildConfig().getRepoPath().toAbsolutePath().toFile())) {
            return git.getRepository().getBranch();
        }
    }
}
