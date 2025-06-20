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
import net.webtide.tools.release.WriteMarkdown;
import net.webtide.tools.release.WriteOutput;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Produce a changelog.md which represents the changes
 * for this particular release.  A file suitable to use as body, in the Github release.
 */
@Mojo(name = "gh-release", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class GitHubReleaseMojo extends AbstractReleaseToolsPlugin
{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            Config config = buildConfig();
            config.getOutputTypes().add(WriteOutput.Type.MARKDOWN);
            doExecute();
            getLog().info("Wrote changelog.md to" + config.getOutputPath().resolve(WriteMarkdown.FILENAME));
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failed to tag changelog", e);
        }
    }
}