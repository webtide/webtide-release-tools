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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.webtide.tools.release.ChangelogTool;
import net.webtide.tools.release.Config;
import net.webtide.tools.release.SaveRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractReleaseToolsPlugin extends AbstractMojo
{

    /**
     * The maven project version.
     */
    @Parameter(property = "webtide.release.tools.version.section", defaultValue = "${project.version}", required = true)
    protected String version;
    @Parameter(required = true)
    private Path configFile;
    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    protected File projectBuildDirectory;
    @Parameter(property = "webtide.release.tools.refVersionCurrent")
    private String refVersionCurrent;
    @Parameter(property = "webtide.release.tools.tagVersionPrior")
    private String tagVersionPrior;
    private Config config;

    public void doExecute(SaveRequest saveRequest) throws MojoExecutionException, MojoFailureException
    {

        Config config = buildConfig();

        try (ChangelogTool changelog = new ChangelogTool(config))
        {
            // equivalent of git log <old>..<new>
            changelog.resolveCommits();

            // resolve all title/body fields (in commits, issues, and prs) for textual issues references (recursively)
            changelog.resolveIssues();

            // resolve all of the issue and pull requests commits
            changelog.resolveIssueCommits();

            // link up commits / issues / pull requests
            changelog.linkActivity();

            System.out.printf("Found %,d commit entries%n", changelog.getCommits().size());
            System.out.printf("Found %,d issue/pr references%n", changelog.getIssues().size());

            if (!Files.exists(config.getOutputPath()))
            {
                Files.createDirectories(config.getOutputPath());
            }

            changelog.save(saveRequest);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failed to tag changelog", e);
        }
    }

    protected Config buildConfig() throws MojoExecutionException
    {
        if (this.config == null)
        {
            try
            {
                this.config = Config.loadConfig(configFile);
                this.config.setOutputPath(projectBuildDirectory.toPath());
                this.config.setRefVersionCurrent(refVersionCurrent);
                this.config.setTagVersionPrior(tagVersionPrior);
                this.config.setRepoPath(Paths.get("./"));
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return config;
    }
}
