package si.vegvamind.ftcuploader;

import com.neovisionaries.ws.client.*;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.json.JSONObject;
import si.vegvamind.ftcuploader.utils.NetworkUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FTCUploader implements Plugin<Project> {
	private static final Logger LOG = Logging.getLogger(FTCUploader.class);

	@Override
	public void apply(Project project) {
		UploaderConfig extension = project.getExtensions().create("ftcUploader", UploaderConfig.class);

		Task uploadTask = project.task("ftcUpload")
				.doFirst(task -> {
					Path srcPath = project.getProjectDir().toPath().resolve("src/main/java");
					try(Stream<Path> walk = Files.walk(srcPath)) {
						walk.filter(Files::isRegularFile)
								.filter(path -> path.toString().endsWith(".java"))
								.forEach(path -> NetworkUtils.upload(extension.getRobotIp(), path));
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				});

		project.task("ftcCompile")
				.dependsOn(uploadTask)
				.doFirst(task -> {
					try {
						WebSocket ws = new WebSocketFactory()
								.setConnectionTimeout(3000)
								.createSocket("ws://192.168.49.1:8081/")
								.addListener(new WebSocketAdapter() {
									@Override
									public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
										LOG.info("Connected to OnBotJava!");
										websocket.sendText("{\"namespace\":\"system\",\"type\":\"subscribeToNamespace\",\"payload\":\"ONBOTJAVA\"}");
										websocket.sendText("{namespace: \"ONBOTJAVA\", type: \"build:launch\", payload: \"\"}");
										LOG.info("Started compilation!");
									}

									@Override
									public void onTextMessage(WebSocket websocket, String text) {
										JSONObject message = new JSONObject(text);

										if(message.getString("namespace").equals("ONBOTJAVA")) {
											JSONObject payload = new JSONObject(message.getString("payload"));
											String status = payload.getString("status");

											LOG.info("Status update: {}", status);

											if(status.equals("SUCCESSFUL") || status.equals("FAILED")) {
												websocket.sendClose();
												websocket.disconnect();
											}
										}
									}
								})
								.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
								.connect();
						while(ws.isOpen()) {
						}
					} catch(IOException | WebSocketException e) {
						throw new RuntimeException(e);
					}
				})
				.doLast(task -> {
					// Check if build was successful
					String error = NetworkUtils.getErrorMessage(extension.getRobotIp());
					if(error != null) {
						throw new GradleException("Build FAILED! \n" + error);
					}

					// Make sure warning is visible
					System.err.println("This plugin is meant for development and prototyping; do not use it to upload code for actual competition!");
					System.err.println("Before the competition make sure you are running code compiled in Android Studio!");
				});
	}
}