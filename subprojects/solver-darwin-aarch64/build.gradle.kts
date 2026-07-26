/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.native-library")
}

refinery.nameSuffix = "Darwin aarch64"
refineryNatives.classifier = "z3-${refinery.z3Version}-arm64-osx-13.3"
refineryNatives.includePatterns = listOf("*.dylib")
