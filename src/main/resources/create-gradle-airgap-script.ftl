repositories {
    maven {
        url 'https://repo.blackduck.com/bds-integration-public-cache/'
    }
}

configurations {
    airGap
}

dependencies {
    airGap 'com.synopsys.integration:integration-common:26.1.2'
    airGap 'com.fasterxml.jackson.core:jackson-databind:2.15.0'
}

task installDependencies(type: Copy) {
    from configurations.airGap
    include '*.jar'
    exclude '*jackson-core-2.14.0.jar'
    exclude '*jackson-databind-2.14.0.jar'
    into "${gradleOutput}"
}
