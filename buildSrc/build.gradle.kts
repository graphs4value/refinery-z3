/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	`kotlin-dsl`
	alias(libs.plugins.versions)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.asm)
	implementation(libs.jna)
}
