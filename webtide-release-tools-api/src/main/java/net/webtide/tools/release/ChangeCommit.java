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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChangeCommit extends ChangeRef
{
    private String sha;
    private Author author;
    private String title;
    private String body;
    private ZonedDateTime commitTime;
    private List<String> files;
    private List<String> branches;
    private Set<Integer> issueRefs;
    private Set<Integer> pullRequestRefs;

    public void addIssueRef(int ref)
    {
        if (issueRefs == null)
            issueRefs = new HashSet<>();
        issueRefs.add(ref);
    }

    public void addIssueRefs(Collection<Integer> refs)
    {
        if (issueRefs == null)
            issueRefs = new HashSet<>();
        issueRefs.addAll(refs);
    }

    public void addPullRequestRef(int ref)
    {
        if (pullRequestRefs == null)
            pullRequestRefs = new HashSet<>();
        pullRequestRefs.add(ref);
    }

    public void addPullRequestRefs(Collection<Integer> refs)
    {
        if (refs.isEmpty())
            return;
        if (pullRequestRefs == null)
            pullRequestRefs = new HashSet<>();
        pullRequestRefs.addAll(refs);
    }

    public Author getAuthor()
    {
        return author;
    }

    public void setAuthor(Author author)
    {
        this.author = author;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public List<String> getBranches()
    {
        return branches;
    }

    public void setBranches(Collection<String> branches)
    {
        if (branches == null)
            this.branches = null;
        else
        {
            this.branches = new ArrayList<>();
            this.branches.addAll(branches);
        }
    }

    public ZonedDateTime getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(ZonedDateTime commitTime)
    {
        this.commitTime = commitTime;
    }

    public List<String> getFiles()
    {
        return files;
    }

    public void setFiles(Collection<String> files)
    {
        if (files == null)
            this.files = null;
        else
        {
            this.files = new ArrayList<>();
            this.files.addAll(files);
        }
    }

    public Set<Integer> getIssueRefs()
    {
        return issueRefs;
    }

    public Set<Integer> getPullRequestRefs()
    {
        return pullRequestRefs;
    }

    public String getSha()
    {
        return sha;
    }

    public void setSha(String sha)
    {
        this.sha = sha;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public String toString()
    {
        return getSha() + ":" + getTitle();
    }
}
