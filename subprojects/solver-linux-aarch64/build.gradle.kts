/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.native-library")
}

refinery.nameSuffix = "Linux aarch64"
refineryNatives.classifier = "z3-${refinery.z3Version}-arm64-glibc-2.38"
refineryNatives.includePatterns = listOf("*.so")
