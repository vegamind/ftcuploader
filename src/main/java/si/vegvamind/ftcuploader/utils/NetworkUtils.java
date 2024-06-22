package si.vegvamind.ftcuploader.utils;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkUtils {
	private NetworkUtils() {
	}

	public static void upload(String robotIp, Path filePath) {
		try {
			OkHttpClient client = new OkHttpClient().newBuilder().build();

			/*
			Truly an ugly hack - for each file find its root folder (src/main/java) and remove it from file path;

			> why do such thing? - Well, Gradle's SourceSets are useless here, since TeamCode module
				lacks main, test, etc. SourceSets.
			> Oh but the folder structure does in fact contain src/main, how come the Gradle doesn't recognize it as a
				SourceSet??? Well, I don't know
			 */
			String relativeFilePath = filePath
					.toString()
					.replace(
							findMainPath(filePath).toString(),
							""
					);

			RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
					.addFormDataPart(
							"data",
							new String(Files.readAllBytes(filePath))
					)
					.build();

			Request request = new Request.Builder()
					.url("http://" + robotIp + ":8080/java/file/save?f=/src" + relativeFilePath)
					.method("POST", body)
					.build();

			client.newCall(request).execute().close();
			System.out.println("Uploaded " + filePath.getFileName());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Path findMainPath(Path path) {
		// Recurse till we find src/main/java <- (java folder)
		Path parent = path.getParent();

		if(parent.getFileName().endsWith("java")) {
			return parent;
		} else {
			return findMainPath(parent);
		}
	}
}
