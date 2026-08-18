plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.miapp"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_25
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		// Spring AI 2.0.0 GA (12 jun 2026) está alineado con Spring Boot 4.1.0, sin fricción de
		// versión. Verificado contra Maven Central antes de fijar la versión (ver specs/truper-cliente-mcp-plan.md).
		mavenBom("org.springframework.ai:spring-ai-bom:2.0.0")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	// Compatibilidad verificada: springdoc-openapi 3.1.0 fija su propia dependencia en Spring Boot 4.1.0,
	// versión exacta que usa este proyecto (ver api/build.gradle.kts). Release notes:
	// https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.0
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
	// MCP Server sobre Streamable HTTP (Spring AI). Único starter necesario: auto-configura
	// escaneo de anotaciones @McpTool, generación de JSON Schema y transporte.
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-restclient")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	// Cliente MCP real (Java SDK) para McpServerIT — nunca se mockea el protocolo.
	testImplementation("io.modelcontextprotocol.sdk:mcp:2.0.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

jacoco {
	toolVersion = "0.8.15"
}

// Clases sin lógica de negocio que no deben inflar ni castigar el número de cobertura:
// el punto de entrada de Spring Boot y la configuración declarativa sin ramas que probar.
// El manejador global de errores vive en el mismo paquete pero SÍ tiene lógica (arma el
// ProblemDetail), así que se lo excluye de la exclusión.
val jacocoRutasExcluidas = listOf("com/miapp/ApiApplication.class", "com/miapp/config/**")

fun ConfigurableFileCollection.excluirClasesSinLogica() {
	setFrom(files.map { dir ->
		fileTree(dir) { exclude(jacocoRutasExcluidas) } +
			fileTree(dir) { include("com/miapp/config/ManejadorErrores.class") }
	})
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	classDirectories.excluirClasesSinLogica()
	reports {
		xml.required = true
		html.required = true
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.excluirClasesSinLogica()
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.90".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}
