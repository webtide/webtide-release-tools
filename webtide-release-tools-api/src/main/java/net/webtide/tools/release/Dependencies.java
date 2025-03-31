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

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dependencies
{
    public static class BumpDistinct implements Predicate<Change>
    {
        private Map<String, Boolean> bumps = new ConcurrentHashMap<>();
        private Pattern patternBump = Pattern.compile("^Bump (.*) to (.*)$");

        @Override
        public boolean test(Change change)
        {
            String title = change.getRefTitle();
            Matcher matcher = patternBump.matcher(title);
            if (matcher.find())
            {
                String depName = matcher.group(1);
                return bumps.putIfAbsent(depName, Boolean.TRUE) == null;
            }

            // allow non-matching through.
            return true;
        }
    }

    public static class BumpToComparator implements Comparator<Change>
    {
        private Pattern patternBump = Pattern.compile("^Bump (.*) to (.*)$");

        @Override
        public int compare(Change c1, Change c2)
        {
            Matcher matcher1 = patternBump.matcher(c1.getRefTitle());
            Matcher matcher2 = patternBump.matcher(c2.getRefTitle());

            if (matcher1.find())
            {
                if (matcher2.find())
                {
                    int diff = matcher1.group(1).compareTo(matcher2.group(1));
                    if (diff != 0)
                        return diff;
                    return matcher2.group(2).compareTo(matcher1.group(2));
                }
            }

            return c1.getRefTitle().compareTo(c2.getRefTitle());
        }
    }

    public static class BumpFromToSimplifier implements Function<Change, Change>
    {
        private Pattern patternBump = Pattern.compile("^Bump (.*) from (.*) to (.*)$");

        @Override
        public Change apply(Change change)
        {
            Matcher matcher = patternBump.matcher(change.getRefTitle());
            if (matcher.find())
            {
                String depName = matcher.group(1);
                String latestVersion = matcher.group(3);
                String replacementTitle = String.format("Bump %s to %s", depName, latestVersion);
                change.setRefTitle(replacementTitle);
            }
            // return same change.
            return change;
        }
    }
}
