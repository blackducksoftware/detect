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
    airGap 'com.fasterxml.jackson.core:jackson-databind:2.15.0'
}

task installDependencies(type: Copy) {
    from configurations.airGap
    include '*.jar'
    into "${gradleOutput}"
}
