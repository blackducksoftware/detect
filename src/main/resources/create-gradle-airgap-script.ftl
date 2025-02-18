repositories {
    maven {
        url 'https://repo.blackduck.com/bds-integration-public-cache/'
    }
}

configurations {
    airGap
}

dependencies {
    airGap 'org.slf4j:slf4j-api:2.0.7'
}

task installDependencies(type: Copy) {
    from configurations.airGap
    into "${gradleOutput}"
}
