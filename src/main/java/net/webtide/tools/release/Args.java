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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import com.google.common.base.Strings;

public class Args extends HashMap<String, String>
{
    public static class ArgException extends RuntimeException
    {
        public ArgException(String format, Object... args)
        {
            super(String.format(format, args));
        }
    }

    public Args(String... args)
    {
        super();

        for (String arg : args)
        {
            if (!arg.startsWith("--"))
            {
                throw new ArgException("Unrecognized option: %s", arg);
            }

            int idxEqual = arg.indexOf('=');
            if (idxEqual > 0)
            {
                put(arg.substring(2, idxEqual), arg.substring(idxEqual + 1));
            }
            else
            {
                put(arg.substring(2), null);
            }
        }
    }

    public String getOptional(String key)
    {
        return get(key);
    }

    public String getRequired(String key)
    {
        String value = get(key);
        if (Strings.isNullOrEmpty(value))
        {
            throw new ArgException("Missing required value for option --%s=<value>", key);
        }
        return value;
    }

    public int getInteger(String key)
    {
        String value = get(key);
        if (value == null)
        {
            throw new ArgException("Missing required value for option --%s=<number>", key);
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new ArgException("Invalid number value for option --%s=%s", key, value);
        }
    }

    public Path getPath(String key, Path defaultPath)
    {
        String pathStr = get(key);
        if (pathStr == null)
            return defaultPath;
        return Paths.get(pathStr);
    }
}
