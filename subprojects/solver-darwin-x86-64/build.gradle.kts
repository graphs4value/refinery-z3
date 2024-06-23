/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

val classifier = "z3-${version}-x64-osx-11.7.10"
val library = "z3java-darwin-x86-64"

mavenArtifact.nameSuffix = "Darwin x86_64"

dependencies {
	z3("Z3Prover:z3:${version}:${classifier}@zip")
}

val extractZ3Libs by tasks.registering(Sync::class) {
	dependsOn(configurations.z3)
	from({
		val zipFile = configurations.z3.map { it.singleFile }
		zipTree(zipFile).matching {
			include("${classifier}/bin/*.dylib")
			includeEmptyDirs = false
		}
	})
	eachFile {
		val pathInBin = relativePath.segments.drop(2).toTypedArray()
		relativePath = RelativePath(true, library, *pathInBin)
	}
	into(layout.buildDirectory.dir("z3-extracted"))
}

sourceSets.main {
	resources.srcDir(extractZ3Libs)
}
