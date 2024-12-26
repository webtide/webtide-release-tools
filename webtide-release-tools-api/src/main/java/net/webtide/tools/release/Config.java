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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import net.webtide.tools.github.gson.PathTypeAdapter;

public class Config
{
    // Local path to git repo
    protected Path repoPath;
    // github org/user name to github repo
    protected String githubRepoOwner;
    // github repo name
    protected String githubRepoName;
    // git branch name to generate changelog for
    protected String branch;
    // tag of prior version
    protected String tagVersionPrior;
    // tag of current version (the one we are generating the changelog for)
    protected String refVersionCurrent;
    // list of labels (on issues and prs) to exclude from changelog
    protected List<String> labelExclusions = new ArrayList<>();
    // list of regex strings to apply to commit paths to exclude from changelog
    // if resulting commit has no paths left after exclusion, that commit is excluded.
    protected List<String> commitPathRegexExclusions = new ArrayList<>();
    // list of regex strings to apply for branch exclusions.
    // if commit belongs to matching exclusion, then that commit is excluded from changelog
    protected List<String> branchRegexExclusions = new ArrayList<>();
    // include the list of dependency changes in the output
    protected boolean includeDependencyChanges = false;
    // output path to generate changelog details
    protected Path outputPath;

    public static Config loadConfig(Path path) throws IOException
    {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .registerTypeAdapter(Path.class, new PathTypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            return gson.fromJson(reader, Config.class);
        }
    }

    public static Config parseArgs(Args args) throws IOException
    {
        Config config = null;

        // load default configuration from file
        String configFile = args.getOptional("config_file");
        if (!Strings.isNullOrEmpty(configFile))
        {
            config = loadConfig(Paths.get(configFile));
        }
        else
        {
            config = loadConfig(Paths.get("changelog-tool.json"));
        }

        if (config == null)
        {
            config = new Config();
        }

        if (config.getOutputPath() == null)
        {
            config.setOutputPath(Paths.get(System.getProperty("user.dir")));
        }

        // overlay with arguments from command line
        config.setRepoPath(args.getPath("repo_path", config.getRepoPath()));
        config.setGithubRepoName(args.getOrDefault("github_repo_name", config.getGithubRepoName()));
        config.setGithubRepoOwner(args.getOrDefault("github_repo_owner", config.getGithubRepoOwner()));
        config.setBranch(args.getOrDefault("branch", config.getBranch()));
        config.setTagVersionPrior(args.getOrDefault("tag_version_prior", config.getTagVersionPrior()));
        config.setRefVersionCurrent(args.getOrDefault("ref_version_current", config.getRefVersionCurrent()));
        config.setOutputPath(args.getPath("output_path", config.getOutputPath()));
        config.setIncludeDependencyChanges(args.getBoolean("includeDependencyChanges", false));

        return config;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public List<String> getBranchRegexExclusions()
    {
        return branchRegexExclusions;
    }

    public void setBranchRegexExclusions(List<String> branchRegexExclusions)
    {
        this.branchRegexExclusions = branchRegexExclusions;
    }

    public List<String> getCommitPathRegexExclusions()
    {
        return commitPathRegexExclusions;
    }

    public void setCommitPathRegexExclusions(List<String> commitPathRegexExclusions)
    {
        this.commitPathRegexExclusions = commitPathRegexExclusions;
    }

    public String getGithubRepoName()
    {
        return githubRepoName;
    }

    public void setGithubRepoName(String githubRepoName)
    {
        this.githubRepoName = githubRepoName;
    }

    public String getGithubRepoOwner()
    {
        return githubRepoOwner;
    }

    public void setGithubRepoOwner(String githubRepoOwner)
    {
        this.githubRepoOwner = githubRepoOwner;
    }

    public List<String> getLabelExclusions()
    {
        return labelExclusions;
    }

    public void setLabelExclusions(List<String> labelExclusions)
    {
        this.labelExclusions = labelExclusions;
    }

    public Path getOutputPath()
    {
        return outputPath;
    }

    public void setOutputPath(Path outputPath)
    {
        this.outputPath = outputPath;
    }

    public String getRefVersionCurrent()
    {
        return refVersionCurrent;
    }

    public void setRefVersionCurrent(String refVersionCurrent)
    {
        this.refVersionCurrent = refVersionCurrent;
    }

    public Path getRepoPath()
    {
        return repoPath;
    }

    public void setRepoPath(Path repoPath)
    {
        this.repoPath = repoPath;
    }

    public String getTagVersionPrior()
    {
        return tagVersionPrior;
    }

    public void setTagVersionPrior(String tagVersionPrior)
    {
        this.tagVersionPrior = tagVersionPrior;
    }

    public boolean isIncludeDependencyChanges()
    {
        return includeDependencyChanges;
    }

    public void setIncludeDependencyChanges(boolean includeDependencyChanges)
    {
        this.includeDependencyChanges = includeDependencyChanges;
    }
}
