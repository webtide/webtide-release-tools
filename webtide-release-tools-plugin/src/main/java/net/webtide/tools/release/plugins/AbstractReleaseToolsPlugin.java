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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractReleaseToolsPlugin extends AbstractMojo  {

    @Parameter(required = true)
    private Path configFile;

    @Parameter(defaultValue = "target/release-output")
    private Path outputDir;

    @Parameter(property = "webtide.release.tools.refVersionCurrent")
    private String refVersionCurrent;

    @Parameter(property = "webtide.release.tools.tagVersionPrior")
    private String tagVersionPrior;

    /**
     * The maven project version.
     */
    @Parameter(property = "webtide.release.tools.version.section", defaultValue = "${project.version}", required = true)
    protected String version;

    protected Config buildConfig() throws MojoExecutionException {
        try {
            Config config = Config.loadConfig(configFile);
            config.setOutputPath(outputDir);
            config.setRefVersionCurrent(refVersionCurrent);
            config.setTagVersionPrior(tagVersionPrior);
            config.setRepoPath(Paths.get("./"));
            return config;
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    protected void generateChanges() {

    }
}
