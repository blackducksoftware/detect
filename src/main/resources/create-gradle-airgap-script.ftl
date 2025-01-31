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
}

task installDependencies(type: Copy) {
    from configurations.airGap
    include '*.jar'
    exclude '*jackson-core*.jar'
    exclude '*jackson-databind*.jar'
    exclude '*jackson-annotations*.jar'
    into "${gradleOutput}"
}
