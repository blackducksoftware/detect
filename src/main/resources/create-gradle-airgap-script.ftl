repositories {
    maven {
        url 'https://sig-repo.synopsys.com/bds-integration-public-cache/'
    }
}

configurations {
    airGap
}

dependencies {
    airGap 'com.synopsys.integration:integration-common:27.0.1'
}

task installDependencies(type: Copy) {
    from configurations.airGap
    include '*.jar'
    exclude '*jackson-core-2.14.0.jar'
    exclude '*jackson-databind-2.14.0.jar'
    into "${gradleOutput}"
}
