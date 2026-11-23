rootProject.name = "general-fullstack-dev"

// Keep each app as an independent Gradle build and add more here as they are created.
includeBuild("auth")
includeBuild("backend/fileStorage/standard")
includeBuild("backend/fileStorage/webFlux")
