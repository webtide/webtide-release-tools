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

package net.webtide.tools.github.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import net.webtide.tools.github.Cache;
import net.webtide.tools.github.GitHubResourceNotFoundException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PersistentCache implements Cache
{
    private final Path root;

    public PersistentCache()
    {
        this(Paths.get(System.getProperty("user.home"), ".cache", "api.github.com"));
    }

    public PersistentCache(Path cacheDir)
    {
        this.root = cacheDir;
        if (!Files.exists(this.root))
        {
            try
            {
                Files.createDirectories(this.root);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Unable to create cache dir: " + this.root, e);
            }
        }
    }

    public String getCached(String path) throws IOException
    {
        Path cachedPath = toJsonPath(path);
        if (!Files.exists(cachedPath))
        {
            return null;
        }

        // TODO: expire file

        byte[] buf = Files.readAllBytes(cachedPath);
        String body = new String(buf, UTF_8);
        if (body.equals("-"))
            throw new GitHubResourceNotFoundException(path);
        return body;
    }

    public void save(String path, String body) throws IOException
    {
        Path destFile = toJsonPath(path);
        Path parentDir = destFile.getParent();
        if (!Files.exists(parentDir))
        {
            Files.createDirectories(parentDir);
        }
        Files.writeString(toJsonPath(path), body, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public void saveNotFound(String path) throws IOException
    {
        save(path, "-");
    }

    private Path toJsonPath(String path)
    {
        String relativePath = path;
        if (relativePath.startsWith("/"))
            relativePath = relativePath.substring(1);

        return this.root.resolve(relativePath + ".json");
    }
}
