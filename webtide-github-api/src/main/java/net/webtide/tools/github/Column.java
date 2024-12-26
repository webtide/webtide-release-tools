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

public class Column
{
    protected String name;
    protected ZonedDateTime createdAt;
    protected ZonedDateTime updatedAt;
    private long id;

    public ZonedDateTime getCreatedAt()
    {
        return createdAt;
    }

    public long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    @Override
    public String toString()
    {
        return "Column{" + "id=" + id + ", name='" + name + '\'' + ", createdAt=" + createdAt + ", updatedAt="
            + updatedAt + '}';
    }
}
