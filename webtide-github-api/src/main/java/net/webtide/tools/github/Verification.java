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

public class Verification
{
    protected boolean verified;
    protected String reason;
    protected String signature;

    public String getReason()
    {
        return reason;
    }

    public String getSignature()
    {
        return signature;
    }

    public boolean isVerified()
    {
        return verified;
    }
}
