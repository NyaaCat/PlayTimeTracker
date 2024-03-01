plugins {
    `java-library`
    `maven-publish`
}

// = = =

val pluginName = "PlayerTimeTracker"
val majorVersion = 0
val minorVersion = 8

val paperApiName = "1.20.4-R0.1-SNAPSHOT"

// = = =

// for Jenkins CI
val buildNumber = System.getenv("BUILD_NUMBER") ?: "local"
val mavenDirectory: String =
    System.getenv("MAVEN_DIR")
        ?: layout.buildDirectory.dir("repo").get().asFile.absolutePath
val javaDocDirectory: String =
    System.getenv("JAVADOC_DIR")
        ?: layout.buildDirectory.dir("javadoc").get().asFile.absolutePath

// Version used for distribution. Different from maven repo
group = "cat.nyaa"
//archivesBaseName = "${pluginNameUpper}-mc$minecraftVersion"
version =
    "$majorVersion.$minorVersion.$buildNumber"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    } //paper
    maven { url = uri("https://libraries.minecraft.net") } // mojang
    maven { url = uri("https://repo.essentialsx.net/releases/") } // essentials
    maven { url = uri("https://ci.nyaacat.com/maven/") } // nyaacat
    maven {
        url =
            uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    } // placeholderapi
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiName")
    compileOnly("org.jetbrains:annotations:23.0.0")
    // soft dep
    compileOnly("net.essentialsx:EssentialsX:2.19.6")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.udojava:EvalEx:2.7")
    compileOnly("org.xerial:sqlite-jdbc:3.42.0.0")
    compileOnly("com.zaxxer:HikariCP:5.0.1")
    // other nyaa plugins
    compileOnly("cat.nyaa:nyaacore:9.3.4-1.20.4")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(getComponents()["java"])
            afterEvaluate {
                artifactId = pluginName.lowercase()
                groupId = "$group"
                version =
                    "$majorVersion.$minorVersion.$buildNumber-${
                        getMcVersion(
                            paperApiName
                        )
                    }"
            }
        }
    }
    repositories {
        maven {
            name = "nyaaMaven"
            url = uri(mavenDirectory)
        }
    }
}

/*
reobfJar {
     // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
     // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
     outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
}
*/


tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        with((options as StandardJavadocDocletOptions)) {
            options.encoding =
                Charsets.UTF_8.name() // We want UTF-8 for everything
            links("https://docs.oracle.com/en/java/javase/17/docs/api/")
            links("https://guava.dev/releases/21.0/api/docs/")
            links("https://ci.md-5.net/job/BungeeCord/ws/chat/target/apidocs/")
            links("https://jd.papermc.io/paper/1.20/")
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

}

private fun getMcVersion(apiNameString: String): String {
    return apiNameString.split('-')[0]
}
