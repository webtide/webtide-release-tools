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

import java.text.SimpleDateFormat;
import java.util.Date;

import net.webtide.tools.release.ChangeMetadata;
import net.webtide.tools.release.Config;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import static net.webtide.tools.release.ChangeMetadata.OUTPUT_FORMAT.TAG_TXT;

/**
 * Produce a target/version-tag.txt which represents the changes
 * for this particular release.  A file suitable to use as the git tag
 * message body, in tagging the release.
 */
@Mojo(name = "tag", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class TagMojo extends AbstractReleaseToolsPlugin
{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
            Config config = buildConfig();
            ChangeMetadata saveRequest = new ChangeMetadata(
                config.getOutputPath(),
                config.isIncludeDependencyChanges(),
                TAG_TXT,
                version,
                sdf.format(new Date())
            );
            doExecute(saveRequest);

            getLog().info("Wrote version tag txt to" + config.getOutputPath().resolve(TAG_TXT.getFilename()));
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Failed to tag changelog", e);
        }
    }
}
