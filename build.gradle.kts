dependencies {
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
}

plugins {
    id("org.sonarqube") version "3.3"
}

sonarqube {
    properties {
        property("sonar.projectKey", "framurpri_PAI5-ST-13")
    }
}
