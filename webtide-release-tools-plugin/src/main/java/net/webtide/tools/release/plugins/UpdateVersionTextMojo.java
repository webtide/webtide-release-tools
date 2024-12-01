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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

@Mojo(name = "update-version-text", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class UpdateVersionTextMojo extends TagMojo {

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
    public void execute() throws MojoExecutionException, MojoFailureException {
        // TODO add a flag to reuse already generated files
        super.execute();
        try {
            Files.copy(originalVersionTextOutputFile.toPath(), versionTextOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            byte[] verstonTxttEntry = Files.readAllBytes(versionTagOutputFile.toPath());
            Files.write(versionTextOutputFile.toPath(), verstonTxttEntry, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }

    }
}
