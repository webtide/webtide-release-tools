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

public enum Skip
{
    // Commit has 2 or more parents
    IS_MERGE_COMMIT,
    // Commit has no interesting diff paths left (after exclusions)
    NO_INTERESTING_PATHS_LEFT,
    // Issue/PR has label that is excluded
    EXCLUDED_LABEL,
    // Commit and Issue/PR has "#<num>" reference to an invalid issue
    INVALID_ISSUE_REF,
    // PR has base-ref that is not correct
    NOT_CORRECT_BASE_REF,
    // Commit has branch that is specifically excluded
    EXCLUDED_BRANCH,
    // Issue/PR has state that isn't "closed"
    NOT_CLOSED,
    // Issue/PR has no relevant commits (either no commits, or only commits that are skipped)
    NO_RELEVANT_COMMITS,
    // Referenced Git Object is missing
    GIT_OBJ_MISSING;
}
