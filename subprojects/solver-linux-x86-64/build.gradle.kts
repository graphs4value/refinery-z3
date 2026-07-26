/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

val classifier = "z3-${refinery.z3Version}-x64-glibc-2.39"
val library = "z3java-linux-x86-64"

refinery.nameSuffix = "Linux x86_64"

dependencies {
	z3("Z3Prover:z3:${refinery.z3Version}:${classifier}@zip")
}

val extractZ3Libs = tasks.register<Sync>("extractZ3Libs") {
	dependsOn(configurations.z3)
	from({
		val zipFile = configurations.z3.map { it.singleFile }
		zipTree(zipFile).matching {
			include("${classifier}/bin/*.so")
			includeEmptyDirs = false
		}
	})
	eachFile {
		val pathInBin = relativePath.segments.drop(2).toTypedArray()
		relativePath = RelativePath(true, library, *pathInBin)
	}
	into(layout.buildDirectory.dir("z3-extracted"))
	description = "Extract Z3 native libraries"
}

sourceSets.main {
	resources.srcDir(extractZ3Libs)
}
