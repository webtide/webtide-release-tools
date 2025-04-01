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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Represents the ChangeLog as it is being built.
 */
public class Changelog extends ArrayList<Change>
{
    public static class DistinctRefTitle implements Predicate<Change>
    {
        private Map<String, Boolean> seen = new ConcurrentHashMap<>();

        @Override
        public boolean test(Change change)
        {
            String title = change.getRefTitle();
            return seen.putIfAbsent(title, Boolean.TRUE) == null;
        }
    }
}
