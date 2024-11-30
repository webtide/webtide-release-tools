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

import net.webtide.tools.release.ChangelogTool;
import net.webtide.tools.release.Config;
import net.webtide.tools.release.SaveRequest;
import net.webtide.tools.release.SaveRequestBuilder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Produce a target/version-tag.txt which represents the changes
 * for this particular release.  A file suitable to use as the git tag
 * message body, in tagging the release.
 */
@SuppressWarnings("unused")
@Mojo(name = "tag", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class TagMojo extends AbstractReleaseToolsPlugin {

    /**
     * The generated version-tag.txt file.
     */
    @Parameter(property = "webtide.release.tools.tag.output.file", defaultValue = "${project.build.directory}/version-tag.txt")
    protected File versionTagOutputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

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

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");


            var saveRequest = SaveRequestBuilder.builder()
                            .outputDir(config.getOutputPath())
                            .includeDependencyChanges(config.isIncludeDependencyChanges())
                            .outputFormat(SaveRequest.OUTPUT_FORMAT.TAG_TXT)
                            .projectVersion(version)
                            .date(sdf.format(new Date()))
                            .build();

            changelog.save(saveRequest);
            getLog().info("Wrote version tag txt to" + versionTagOutputFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to tag changelog", e);
        }

    }
}
