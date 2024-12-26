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

package net.webtide.tools.github;

import java.time.ZonedDateTime;

import com.google.gson.annotations.SerializedName;

public class Repository
{
    protected String name;
    protected String fullName;
    @SerializedName("private")
    protected boolean privateFlag;
    protected User owner;
    protected String description;
    protected boolean fork;
    protected String url;
    protected String gitUrl;
    protected String sshUrl;
    protected String cloneUrl;
    protected ZonedDateTime createdAt;
    protected ZonedDateTime updatedAt;
    protected ZonedDateTime pushedAt;
    protected int stargazersCount;
    protected int watchersCount;
    protected int forksCount;
    protected String language;
    protected long size;
    protected String mirrorUrl;
    protected boolean disabled;
    protected boolean archived;
    protected int openIssuesCount;
    protected String defaultBranch;

    public String getCloneUrl()
    {
        return cloneUrl;
    }

    public ZonedDateTime getCreatedAt()
    {
        return createdAt;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public String getDescription()
    {
        return description;
    }

    public int getForksCount()
    {
        return forksCount;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getGitUrl()
    {
        return gitUrl;
    }

    public String getLanguage()
    {
        return language;
    }

    public String getMirrorUrl()
    {
        return mirrorUrl;
    }

    public String getName()
    {
        return name;
    }

    public int getOpenIssuesCount()
    {
        return openIssuesCount;
    }

    public User getOwner()
    {
        return owner;
    }

    public ZonedDateTime getPushedAt()
    {
        return pushedAt;
    }

    public long getSize()
    {
        return size;
    }

    public String getSshUrl()
    {
        return sshUrl;
    }

    public int getStargazersCount()
    {
        return stargazersCount;
    }

    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    public String getUrl()
    {
        return url;
    }

    public int getWatchersCount()
    {
        return watchersCount;
    }

    public boolean isArchived()
    {
        return archived;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public boolean isFork()
    {
        return fork;
    }

    public boolean isPrivateFlag()
    {
        return privateFlag;
    }
}
