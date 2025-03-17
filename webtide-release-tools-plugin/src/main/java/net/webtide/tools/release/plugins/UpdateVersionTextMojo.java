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
import java.nio.file.StandardOpenOption;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static net.webtide.tools.release.ChangeMetadata.OUTPUT_FORMAT.TAG_TXT;

@Mojo(name = "update-version-text", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class UpdateVersionTextMojo extends TagMojo
{

    /**
     * The original VERSION.txt file.
     */
    @Parameter(property = "version.text.input.file", defaultValue = "./VERSION.txt")
    protected File originalVersionTextOutputFile;

    /**
     * The generated VERSION.txt file.
     */
    @Parameter(property = "version.text.output.file", defaultValue = "${project.build.directory}/VERSION.txt")
    protected File versionTextOutputFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // TODO add a flag to reuse already generated files
        super.execute();
        try
        {
            Files.deleteIfExists(versionTextOutputFile.toPath());
            Path versionTagOutputFile = projectBuildDirectory.toPath().resolve(TAG_TXT.getFilename());
            Files.write(versionTextOutputFile.toPath(), Files.readAllBytes(versionTagOutputFile), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.write(versionTextOutputFile.toPath(), Files.readAllBytes(originalVersionTextOutputFile.toPath()), StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException(e);
        }
    }
}
