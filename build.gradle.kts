plugins {
    `java-library`
    `maven-publish`
}

// = = =

val pluginName = "PlayerTimeTracker"
// Paper coordinates for Minecraft/Pewpew 26.2, discovered from official metadata.
val paperApiName = "26.2.build.123-stable"

// Version used for distribution. Different from maven repo
group = "cat.nyaa"
version = "1.0.0-alpha.7"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 21 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } //paper
    maven { url = uri("https://libraries.minecraft.net") } // mojang
    maven { url = uri("https://repo.essentialsx.net/releases/") } // essentials
//    maven { url = uri("https://repo.essentialsx.net/snapshots/") } // essentials
    maven { url = uri("https://ci.nyaacat.com/maven/") } // nyaacat
//    maven {
//        url = uri("https://maven.pkg.github.com/NyaaCat/NyaaCore")
//        credentials {
//            username = System.getenv("GITHUB_MAVEN_USER") ?: project.findProperty("gpr.user").toString()
//            password = System.getenv("GITHUB_MAVEN_TOKEN") ?: project.findProperty("gpr.key").toString()
//        }
//    } // NyaaCore
    maven { url = uri("https://repo.extendedclip.com/releases/") } // placeholderapi
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiName")
    compileOnly("org.jetbrains:annotations:26.1.0")
    // Paper 26.2 no longer exports JSR-305 transitively, but the db layer documents its
    // threading contract with javax.annotation.concurrent.@NotThreadSafe.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    // soft dep
    compileOnly("io.netty:netty-common:4.2.15.Final") // should keep the same version as Minecraft
    compileOnly("me.clip:placeholderapi:2.12.3")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.0")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("net.essentialsx:EssentialsX:2.21.2") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    // other nyaa plugins
    compileOnly("cat.nyaa:nyaacore:9.12")
    compileOnly("cat.nyaa:ecore:0.3.6")  // optional

    // test
    // MockBukkit 4.x declares paper-api as a compile-only dependency, so it is no longer
    // pulled onto the test compile classpath transitively the way MockBukkit 3.x did.
    testImplementation("io.papermc.paper:paper-api:$paperApiName")
    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.9")
    // MockBukkit moved to the org.mockbukkit.mockbukkit coordinates with 4.x; v26.2 matches Paper 26.2.
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("cat.nyaa:nyaacore:9.12")
    testImplementation("cat.nyaa:ecore:0.3.6")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(getComponents()["java"])
            artifactId = pluginName.lowercase()
            groupId = group.toString()
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "GithubPackage"
            url = uri(System.getenv("MAVEN_PUBLISH_URL") ?: layout.buildDirectory.dir("repo"))
            val mavenUsername = System.getenv("MAVEN_PUBLISH_USERNAME")
            val mavenPassword = System.getenv("MAVEN_PUBLISH_PASSWORD")
            if(mavenUsername != null && mavenPassword != null) {
                credentials {
                    username = mavenUsername
                    password = mavenPassword
                }
            }
        }
        maven {
            name = "NyaaCatCILocal"
            //local maven repository
            url = uri("file://${System.getenv("MAVEN_DIR")}")
        }
    }
}

// Custom tasks for publishing to specific repositories
tasks.register("publishToGithubPackage") {
    dependsOn("publishMavenJavaPublicationToGithubPackageRepository")
    // auto generated task: publish<PublicationName>PublicationTo<RepositoryName>Repository
}

tasks.register("publishToNyaaCatCILocal") {
    dependsOn("publishMavenJavaPublicationToNyaaCatCILocalRepository")
}


tasks {

    withType<ProcessResources> {
        val newProperties = project.properties.toMutableMap()
        newProperties["api_version"] = getMcVersion(paperApiName, 2)
        filesMatching("plugin.yml") {
            expand(newProperties)
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(25)
    }

    javadoc {
        with((options as StandardJavadocDocletOptions)) {
            options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
            // ci.md-5.net/job/BungeeCord/... and guava 21.0 no longer serve an element-list,
            // which makes the javadoc task fail outright. Point at the live locations instead.
            links("https://docs.oracle.com/en/java/javase/25/docs/api/")
            links("https://guava.dev/releases/33.6.0-jre/api/docs/")
            links("https://jd.papermc.io/paper/26.2/")
            options.locale = "en_US"
            options.encoding = "UTF-8"
            (options as StandardJavadocDocletOptions).addBooleanOption(
                "keywords",
                true
            )
            (options as StandardJavadocDocletOptions).addStringOption(
                "Xdoclint:none",
                "-quiet"
            )
            (options as StandardJavadocDocletOptions).addBooleanOption(
                "html5",
                true
            )
            options.windowTitle = "$pluginName Javadoc"
        }


    }

    test {
        useJUnitPlatform()

        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

private fun getMcVersion(apiNameString: String, parts: Int = -1): String {
    val mcVersion = apiNameString.split('-')[0]
    val splitInput = mcVersion.split('.')
    return if (parts > 0 && parts < splitInput.size) {
        splitInput.take(parts).joinToString(".")
    } else {
        mcVersion
    }
}


