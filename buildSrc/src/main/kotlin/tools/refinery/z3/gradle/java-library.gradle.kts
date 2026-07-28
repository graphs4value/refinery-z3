/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.z3.gradle

plugins {
	`java-library`
	`maven-publish`
	signing
}

java {
	withJavadocJar()
	withSourcesJar()

	toolchain {
		languageVersion.set(JavaLanguageVersion.of(25))
	}
}

repositories {
	mavenCentral()

	// Configuration based on https://stackoverflow.com/a/34327202 to pretend that GitHub is an Ivy repository
	// in order to take advantage of Gradle dependency caching.
	val github = ivy {
		setUrl("https://github.com")
		patternLayout {
			artifact("/[organisation]/[module]/releases/download/[module]-[revision]/[classifier].[ext]")
			artifact("/[organisation]/[module]/archive/refs/tags/[module]-[revision].[ext]")
		}
		metadataSources {
			artifact()
		}
	}

	exclusiveContent {
		forRepositories(github)
		filter {
			includeGroup("Z3Prover")
		}
	}
}

val z3 = configurations.register("z3") {
	isCanBeConsumed = false
	isCanBeResolved = true
}

val refinery = extensions.create("refinery", RefineryZ3Extension::class)

// Each project publishes into its own build directory, which the root project aggregates.
val mavenRepositoryDir = layout.buildDirectory.dir("repo")

tasks {
	jar {
		manifest {
			// The architecture has to be spelled with an underscore, because {@code 64} on its own is not a Java
			// identifier.
			val moduleName = "${project.group}." + project.name
				.removePrefix("refinery-z3-")
				.replace("x86-64", "x86_64")
				.replace('-', '.')
			attributes(
				"Automatic-Module-Name" to moduleName,
				// Documentation only, we don't set Bundle-ManifestVersion: 2, so these don't get interpreted by OSGi.
				"Bundle-SymbolicName" to "${project.group}.${project.name}",
				"Bundle-Version" to project.version,
				"Bundle-License" to "Apache-2.0 AND MIT",
			)
		}
	}

	named<Jar>("sourcesJar") {
		// No need to include binary resources in the sources jars.
		exclude("**/*.dll", "**/*.dylib", "**/*.so")
	}

	javadoc {
		options {
			this as StandardJavadocDocletOptions
			addBooleanOption("Xdoclint:none", true)
			// {@code -Xmaxwarns 0} will print all warnings, so we must keep at least one.
			addStringOption("Xmaxwarns", "1")
			quiet()
		}
	}
}

// The POM can only say that a consumer may pick one of our licenses, and Z3 is bundled rather than resolved as a
// dependency, so neither the POM nor a dependency-derived SBOM can state what this artifact actually contains.
// We deliberately do not set `isExternal` on the bundled component, but we still declare version 1.7,
// so that consumers can see the field was available to us and we did not claim the component is provided by the
// environment. See https://github.com/CycloneDX/guides/issues/29#issuecomment-2785784811 for the semantics.
// The BOM carries no timestamp or serial number, so that it stays byte-for-byte reproducible.
val cyclonedxBom = tasks.register("cyclonedxBom") {
	val bomFile = layout.buildDirectory.file("cyclonedx/bom.json")
	outputs.file(bomFile)
	val artifactName = project.name
	val artifactVersion = project.version.toString()
	val purl = "pkg:maven/${project.group}/$artifactName@$artifactVersion"
	val z3Purl = "pkg:github/Z3Prover/z3@z3-${refinery.z3Version}"
	val z3Version = refinery.z3Version
	// Read when the task runs, because the subproject can only describe its own pedigree after this plugin has
	// been applied.
	val pedigreeNotes = refinery.pedigreeNotes
	doLast {
		val outputFile = bomFile.get().asFile
		outputFile.parentFile.mkdirs()
		outputFile.writeText(
			"""
			{
			  "bomFormat": "CycloneDX",
			  "specVersion": "1.7",
			  "version": 1,
			  "metadata": {
			    "component": {
			      "type": "library",
			      "bom-ref": "$purl",
			      "name": "$artifactName",
			      "version": "$artifactVersion",
			      "purl": "$purl",
			      "licenses": [{ "expression": "Apache-2.0 AND MIT" }]
			    }
			  },
			  "components": [
			    {
			      "type": "library",
			      "bom-ref": "$z3Purl",
			      "name": "z3",
			      "version": "$z3Version",
			      "publisher": "Microsoft Corporation",
			      "purl": "$z3Purl",
			      "licenses": [{ "license": { "id": "MIT" } }],
			      "pedigree": { "notes": "${pedigreeNotes.get()}" }
			    }
			  ],
			  "dependencies": [
			    { "ref": "$purl", "dependsOn": ["$z3Purl"] }
			  ],
			  "compositions": [
			    { "aggregate": "incomplete", "dependencies": ["$purl"] }
			  ]
			}
			""".trimIndent()
		)
	}
	description = "Generate a CycloneDX SBOM recording the bundled Z3 and the combined license"
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
			artifact(cyclonedxBom) {
				classifier = "cyclonedx"
				extension = "json"
			}
			pom {
				val prefix = "Z3 Java Bindings"
				val nameString = refinery.nameSuffix.map { "$prefix ($it)" }.orElse(prefix)
				name = nameString.map { "Refinery $it" }
				description = nameString.map {
					"$it for Refinery, an efficient graph solver for generating well-formed models"
				}
				url = "https://refinery.tools/"
				// Maven assumes that a consumer may pick any one of the licenses listed here, so we have to spell
				// out in the comments that this artifact combines works under both of them.
				licenses {
					license {
						name = "The Apache License, Version 2.0"
						url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
						comments = "Applies to the Refinery-authored parts of this artifact, including its " +
								"packaging. Both licenses apply; this is not a choice of license."
					}
					license {
						name = "MIT License"
						url = "https://raw.githubusercontent.com/Z3Prover/z3/master/LICENSE.txt"
						comments = "Applies to the bundled Z3 code and native libraries. Both licenses apply; " +
								"this is not a choice of license."
					}
				}
				developers {
					developer {
						name = "The Refinery Authors"
						url = "https://refinery.tools/"
					}
					developer {
						name = "Microsoft Corporation"
						url = "https://github.com/Z3Prover/z3"
					}
				}
				scm {
					connection = "scm:git:https://github.com/graphs4value/refinery-z3.git"
					developerConnection = "scm:git:ssh://github.com:graphs4value/refinery-z3.git"
					url = "https://github.com/graphs4value/refinery-z3"
				}
				issueManagement {
					url = "https://github.com/graphs4value/refinery-z3/issues"
				}
			}
		}
	}

	repositories {
		maven {
			name = "file"
			setUrl(mavenRepositoryDir.map { uri(it) })
		}
	}
}

val cleanMavenRepository = tasks.register<Delete>("cleanMavenRepository") {
	delete(mavenRepositoryDir)
	description = "Clean Maven repository output files"
}

tasks.named("publishMavenJavaPublicationToFileRepository") {
	// Publishing only ever adds files, so drop stale ones (e.g., left over from an earlier version) first.
	dependsOn(cleanMavenRepository)
}

// Expose the published files, so that the root project can aggregate them into a single Maven repository without
// having to reach into this project.
configurations.create("mavenRepositoryElements") {
	isCanBeConsumed = true
	isCanBeResolved = false
	outgoing.artifact(mavenRepositoryDir) {
		builtBy(tasks.named("publishMavenJavaPublicationToFileRepository"))
	}
}

signing {
	setRequired {
		!version.toString().endsWith("SNAPSHOT") && project.hasProperty("forceSign")
	}
	val signingKeyId = System.getenv("PGP_KEY_ID")
	val signingKey = System.getenv("PGP_KEY")
	val signingPassword = System.getenv("PGP_PASSWORD")
	useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
	sign(publishing.publications["mavenJava"])
}
