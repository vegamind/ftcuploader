package si.vegvamind.ftcuploader;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Uploader implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		ModuleTaskExtension extension = project.getExtensions().create("moduleTask", ModuleTaskExtension.class);

		project.task("hello").doLast(task -> {
			Path srcPath = project.getProjectDir().toPath().resolve("src/main/java");
			try (Stream<Path> walk = Files.walk(srcPath)) {
				walk.filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java"))
						.map(Path::toFile)
						.collect(Collectors.toList())
						.forEach(file -> System.out.println(file.getAbsoluteFile()));
			} catch (IOException e) {
				throw new RuntimeException("Failed to read Java files", e);
			}
		});
	}
}