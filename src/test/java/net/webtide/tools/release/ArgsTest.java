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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArgsTest
{
    @Test
    public void testNoArgs()
    {
        Args args = new Args();
        assertThat("size", args.size(), is(0));
    }

    @Test
    public void testOneArg()
    {
        Args args = new Args("--show-branches");
        assertThat("size", args.size(), is(1));
        assertTrue(args.containsKey("show-branches"), "show-branches exists");
    }

    @Test
    public void testBadArg()
    {
        assertThrows(Args.ArgException.class, () -> new Args("joakime/bogus-repo"));
    }
}
