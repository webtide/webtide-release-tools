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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public abstract class ChangeRef
{
    private boolean resolved = false;
    private Set<Skip> skipSet;
    private int changeRef = -1;

    public boolean isResolved()
    {
        return resolved;
    }

    public void setResolved()
    {
        this.resolved = true;
    }

    public void addSkipReason(Skip skip)
    {
        if (skipSet == null)
        {
            skipSet = EnumSet.of(skip);
        }
        else
        {
            skipSet.add(skip);
        }
    }

    public int getChangeRef()
    {
        return this.changeRef;
    }

    public void setChangeRef(Change change)
    {
        assert (this.changeRef != -1);
        this.changeRef = change.getNumber();
    }

    public Set<Skip> getSkipSet()
    {
        return skipSet == null ? Collections.emptySet() : skipSet;
    }

    public boolean hasChangeRef()
    {
        return this.changeRef >= 0;
    }

    public boolean isSkipped()
    {
        return ((this.skipSet != null) && (!this.skipSet.isEmpty()));
    }
}
