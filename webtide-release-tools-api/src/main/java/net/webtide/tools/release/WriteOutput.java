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

import java.io.IOException;

public interface WriteOutput
{
    enum Type
    {
        MARKDOWN(WriteMarkdown.class),
        VERSION_TXT(WriteVersionTagText.class);

        private final Class<? extends WriteOutput> type;

        Type(Class<? extends WriteOutput> outputType)
        {
            this.type = outputType;
        }

        public Class<? extends WriteOutput> getType()
        {
            return type;
        }
    }

    void write(ChangeMetadata change) throws IOException;
}
