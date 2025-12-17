plugins {
    `java-library`
}

dependencies {
    // spring
    api("org.springframework:spring-web")

    // lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
