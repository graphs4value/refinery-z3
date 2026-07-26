/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.native-library")
}

refinery.nameSuffix = "Win32 x86_64"
refineryNatives.classifier = "z3-${refinery.z3Version}-x64-win"
refineryNatives.includePatterns = listOf("*.dll")
refineryNatives.excludePatterns = listOf("Microsoft.Z3.dll")
