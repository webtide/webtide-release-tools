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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import net.webtide.tools.release.ChangeMetadata;
import net.webtide.tools.release.ChangelogTool;
import net.webtide.tools.release.Config;
import net.webtide.tools.release.FS;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

    @Parameter(property = "webtide.release.tools.gitCacheDir") // , defaultValue = "${project.build.directory}/gitCacheDir")
    private File gitCacheDir;

    @Parameter(property = "webtide.release.tools.releaseVersion", defaultValue = "${project.version}")
    protected String releaseVersion;


    public void doExecute() throws MojoExecutionException
    {
        Config config = buildConfig();

        try (ChangelogTool tool = new ChangelogTool(config))
        {
            tool.discoverChanges();

            System.out.printf("Found %,d commit entries%n", tool.getCommits().size());
            System.out.printf("Found %,d issue/pr references%n", tool.getIssues().size());
            System.out.printf("Found %,d changes%n", tool.getChangelog().size());

            FS.ensureDirectoryExists(config.getOutputPath());

            String projectVersion = releaseVersion == null ? config.getRefVersionCurrent() : releaseVersion;
            ZonedDateTime versionDate = tool.getCurrentVersionCommitterWhen();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            String date = formatter.format(versionDate);

            ChangeMetadata saveRequest = new ChangeMetadata(config,
                projectVersion,
                date,
                tool.getChangelog());

            tool.save(saveRequest);
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
                if(gitCacheDir != null) {
                    this.config.setGitCacheDir(gitCacheDir.toPath());
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return config;
    }
}
