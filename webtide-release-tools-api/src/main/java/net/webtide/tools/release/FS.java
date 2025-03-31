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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FileSystem utility methods
 */
public class FS
{
    public static void ensureDirectoryExists(Path dir) throws IOException
    {
        if (Files.isDirectory(dir))
        {
            // nothing else to do, it exists
            return;
        }

        if (Files.exists(dir))
        {
            throw new IOException("Unable to create directory (conflicts with file of same name): " + dir);
        }

        Files.createDirectory(dir);
    }
}
