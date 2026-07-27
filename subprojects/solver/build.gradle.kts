/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import tools.refinery.z3.gradle.ClassFilePatcher

plugins {
	id("tools.refinery.z3.gradle.java-library")
}

refinery.pedigreeNotes = "The Java bindings were extracted from the official Z3 distribution and repackaged into " +
		"this artifact. The class initializer of com.microsoft.z3.Native has been removed, so that loading the " +
		"native libraries is controlled by tools.refinery.z3.Z3SolverLoader rather than the system library path."

val extractedJarDir = layout.buildDirectory.dir("z3-jar")
val extractedClassesDir = layout.buildDirectory.dir("z3-extracted")
val extractedSourcesDir = layout.buildDirectory.dir("z3-sources")

// Subprojects packaging the native libraries, keyed by the JNA resource prefix of the platform they target.
val nativeLibraryProjects = rootProject.subprojects
	.filter { it.name.startsWith("${project.name}-") }
	.associate {
		val name = it.name
		name.substring(project.name.length + 1) to ":${name}"
	}

// JNA resource prefix of the platform running the build, i.e., the platform the tests will run on.
val hostResourcePrefix: String = com.sun.jna.Platform.RESOURCE_PREFIX
val hostNativeLibraryProject = nativeLibraryProjects[hostResourcePrefix]

val z3Source = configurations.create("z3Source") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

// The Java bindings jar is the same in every Z3 distribution, so we take it from a fixed platform instead of
// downloading (and having to name) another archive of our own.
val javaBindingsProject = nativeLibraryProjects.getValue("linux-x86-64")

// Directory of already extracted native libraries for the platform running the build.
val hostNativeLibraries = configurations.create("hostNativeLibraries") {
	isCanBeConsumed = false
	isCanBeResolved = true
}
val hostNativeLibrariesDir = files(hostNativeLibraries)

// Unpack the Java bindings jar from the Z3 distribution on its own. Reading it out of the distribution and
// extracting its contents in a single task would require expanding the distribution while the build is configured,
// which breaks {@code clean build}: the expanded files live in the build directory and are deleted before the
// extraction gets to run.
val extractZ3DistributionJar = tasks.register<Sync>("extractZ3DistributionJar") {
	dependsOn(configurations.z3)
	from({
		val zipFile = configurations.z3.map { it.singleFile }
		zipTree(zipFile).matching {
			include("*/bin/com.microsoft.z3.jar")
		}
	})
	eachFile {
		relativePath = RelativePath(true, relativePath.lastName)
	}
	includeEmptyDirs = false
	into(extractedJarDir)
	description = "Extract the Z3 Java bindings jar"
}

val extractZ3Jar = tasks.register<Sync>("extractZ3Jar") {
	dependsOn(extractZ3DistributionJar)
	from(zipTree(extractedJarDir.map { it.file("com.microsoft.z3.jar") }).matching {
		exclude("META-INF/MANIFEST.MF")
	})
	includeEmptyDirs = false
	into(extractedClassesDir)
	// Capture the directory in a local, so that the action below doesn't have to reference the build script.
	val classesDir = extractedClassesDir
	doLast {
		// The class initializer off {@see com.microsoft.z3.Native} will try to load the Z3 native libraries
		// from the system default library path unless the {@code z3.skipLibraryLoad} system property is set.
		// Since we don't control the library path or system properties, we remove the class initializer entirely.
		val nativeClassFile = classesDir.get().file("com/microsoft/z3/Native.class").asFile
		ClassFilePatcher.removeClassInitializer(nativeClassFile)
	}
	description = "Extract Z3 Java classes"
}

val extractZ3Source = tasks.register<Sync>("extractZ3Source") {
	dependsOn(z3Source)
	from({
		val zipFile = z3Source.singleFile
		zipTree(zipFile).matching {
			include("z3-z3-${refinery.z3Version}/src/api/java/**/*")
			includeEmptyDirs = false
		}
	})
	eachFile {
		val pathInBin = relativePath.segments.drop(4).toTypedArray()
		relativePath = RelativePath(true, "com", "microsoft", "z3", *pathInBin)
	}
	into(extractedSourcesDir)
	description = "Extract Z3 Java sources"
}

tasks.jar {
	// Add class files to our jar manually.
	from(extractZ3Jar)
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// Everything the tests need, except the jars containing the native libraries, so that
// {@see tools.refinery.z3.Z3SolverLoader} can't extract them.
val classpathWithoutNativeLibraries = run {
	// Rebind as a local, so that the filter below captures only this set instead of the whole build script.
	val nativeLibraryProjectPaths = nativeLibraryProjects.values.toSet()
	files(
		sourceSets.main.map { it.output },
		configurations.testRuntimeClasspath.map { testRuntimeClasspath ->
			testRuntimeClasspath.incoming.artifactView {
				componentFilter { component ->
					component !is ProjectComponentIdentifier ||
							component.projectPath !in nativeLibraryProjectPaths
				}
			}.files
		},
	)
}

// Tests that must run without any of the platform-specific jars on the classpath.
val testMissingLibrariesSourceSet = sourceSets.create("testMissingLibraries") {
	compileClasspath = sourceSets.test.get().compileClasspath
	runtimeClasspath = output + classpathWithoutNativeLibraries
}

// Counterpart of {@code test}: instead of letting the loader extract the native libraries from the
// platform-specific jars, we point the dynamic linker at an already extracted set of libraries and check that the
// plain {@code System.loadLibrary} code path finds them.
val testExtractedLibraries = tasks.register<Test>("testExtractedLibraries") {
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	description = "Run tests against native libraries on the dynamic linker search path"

	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = classpathWithoutNativeLibraries + sourceSets.test.get().output

	// The library path is passed to the forked JVM as a plain string, so we have to declare the dependency on the
	// extracted libraries explicitly.
	inputs.files(hostNativeLibrariesDir)
		.withPropertyName("hostNativeLibraries")
		.withPathSensitivity(PathSensitivity.RELATIVE)

	val hasHostNativeLibraries = hostNativeLibraryProject != null
	onlyIf("Z3 native libraries are available for the platform running the build") {
		hasHostNativeLibraries
	}

	// Capture the file collection in a local, so that the action below doesn't have to reference the build script.
	val librariesDir = hostNativeLibrariesDir
	doFirst {
		val librariesPath = librariesDir.asPath
		// Lets {@code System.loadLibrary} find the JNI library.
		systemProperty("java.library.path", librariesPath)
		// Lets the dynamic linker find the Z3 solver library the JNI library links against.
		val libraryPathVariable = when {
			com.sun.jna.Platform.isWindows() -> "PATH"
			com.sun.jna.Platform.isMac() -> "DYLD_LIBRARY_PATH"
			else -> "LD_LIBRARY_PATH"
		}
		environment(
			libraryPathVariable, listOfNotNull(librariesPath, System.getenv(libraryPathVariable))
				.joinToString(File.pathSeparator)
		)
	}
}

// Negative control for {@code testExtractedLibraries}: neither the platform-specific jars nor a library path, so
// there is nothing left to load and the loader has to report an error. Without this, {@code testExtractedLibraries}
// could pass for the wrong reason, e.g., if the native libraries leaked back onto its classpath.
val testMissingLibraries = tasks.register<Test>("testMissingLibraries") {
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	description = "Check that loading fails if no Z3 native libraries are available at all"

	testClassesDirs = testMissingLibrariesSourceSet.output.classesDirs
	classpath = testMissingLibrariesSourceSet.runtimeClasspath

	doFirst {
		// Make sure a library path set in the environment of the build can't feed the loader after all.
		environment.remove("LD_LIBRARY_PATH")
		environment.remove("DYLD_LIBRARY_PATH")
	}
}

tasks.check {
	dependsOn(testExtractedLibraries, testMissingLibraries)
}

tasks.named<Jar>("sourcesJar") {
	from(extractZ3Source)
}

tasks.named<Javadoc>("javadoc") {
	source(sourceSets.main.map { it.allJava })
	source(fileTree(extractedSourcesDir) {
		builtBy(extractZ3Source)
		include("**/*.java")
	})
}

dependencies {
	z3(project(path = javaBindingsProject, configuration = "z3Archive"))
	z3Source("Z3Prover:z3:${refinery.z3Version}@zip")
	// This dependency doesn't get added to Maven metadata, so we have to add the class files to our jar manually.
	api(files(extractZ3Jar))
	implementation(libs.jna)
	for (projectPath in nativeLibraryProjects.values) {
		implementation(project(projectPath))
	}
	if (hostNativeLibraryProject != null) {
		hostNativeLibraries(project(path = hostNativeLibraryProject, configuration = "nativeLibraries"))
	}
	testImplementation(libs.junit.api)
	testRuntimeOnly(libs.junit.engine)
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
