package si.vegvamind.ftcuploader;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;
import si.vegvamind.ftcuploader.utils.NetworkUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FTCUploader implements Plugin<Project> {
	private WebSocketClient webSocketClient;

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
						throw new RuntimeException("Failed to FTC project files", e);
					}
				});

		project.task("ftcCompile")
				.dependsOn(uploadTask)
				.doFirst(task -> {
					URI socketUri;
					try {
						socketUri = new URI("ws://" + extension.getRobotIp() + ":8081/");
					} catch(URISyntaxException e) {
						throw new RuntimeException(e);
					}

					webSocketClient = new WebSocketClient(socketUri) {
						@Override
						public void onOpen(ServerHandshake serverHandshake) {
						}

						@Override
						public void onMessage(String messageRaw) {
							System.out.println(messageRaw);

							JSONObject message = new JSONObject(messageRaw);
							String payloadRaw = message.getString("payload");
							JSONObject payload = new JSONObject(payloadRaw);

							switch(payload.getString("status")) {
								case "SUCCESSFUL":
									System.out.println("BUILD SUCCEEDED");
									webSocketClient.close();
									break;
								case "FAILED":
									System.err.println("BUILD FAILED");
									webSocketClient.close();
									break;
							}
						}

						@Override
						public void onClose(int i, String s, boolean b) {
						}

						@Override
						public void onError(Exception e) {
							throw new RuntimeException(e);
						}
					};

					webSocketClient.connect();
					webSocketClient.send("{\"namespace\":\"system\",\"type\":\"subscribeToNamespace\",\"payload\":\"ONBOTJAVA\"}");
					webSocketClient.send("{namespace: \"ONBOTJAVA\", type: \"build:launch\", payload: \"\"}");
				});
	}
}