/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

val refineryExtension = the<RefineryZ3Extension>()

refineryExtension.pedigreeNotes.convention(
	"The native libraries were extracted unmodified from the official Z3 distribution for this platform and " +
			"repackaged into this artifact."
)
val refineryNativesExtension = extensions.create("refineryNatives", RefineryNativesExtension::class)

// The subproject can only set the classifier after this plugin has been applied, so the dependency notation has to
// be computed lazily.
dependencies.addProvider("z3", refineryNativesExtension.classifier.map {
	"Z3Prover:z3:${refineryExtension.z3Version}:$it@zip"
})

val extractedLibrariesDir = layout.buildDirectory.dir("z3-libraries")

val libraryName = project.name.replace("refinery-z3-solver", "z3java")

fun transformPaths(prop: ListProperty<String>): List<String> =
	prop.flatMap { values ->
		refineryNativesExtension.classifier.map { classifier ->
			values.map { "$classifier/bin/$it" }
		}
	}.getOrElse(listOf())

val extractZ3Libs = tasks.register<Sync>("extractZ3Libs") {
	dependsOn(configurations.named("z3"))
	from(provider {
		val zipFile = configurations.named("z3").map { it.singleFile }
		zipTree(zipFile).matching {
			include(transformPaths(refineryNativesExtension.includePatterns))
			exclude(transformPaths(refineryNativesExtension.excludePatterns))
			includeEmptyDirs = false
		}
	})
	eachFile {
		relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
	}
	into(extractedLibrariesDir)
	description = "Extract Z3 native libraries"
}

// Package the libraries into a directory named after the JNA resource prefix of the target platform, so that the
// jars for several platforms can coexist on the classpath.
tasks.processResources {
	into(libraryName) {
		from(extractZ3Libs)
	}
}

// Expose the Z3 distribution archive itself, so that other projects can pick platform-independent parts out of it
// (such as the Java bindings jar) without having to repeat the classifier.
configurations.create("z3Archive") {
	isCanBeConsumed = true
	isCanBeResolved = false
	outgoing.artifact(configurations.named("z3").map { it.singleFile })
}

// Expose the extracted native libraries to :refinery-z3-solver, which puts their directory on the dynamic linker
// search path to test loading them without extracting them from the jars on the classpath.
configurations.create("nativeLibraries") {
	isCanBeConsumed = true
	isCanBeResolved = false
	outgoing.artifact(extractedLibrariesDir) {
		builtBy(extractZ3Libs)
	}
}
