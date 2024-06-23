package si.vegamind.ftcuploader.utils;

import okhttp3.*;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import si.vegamind.ftcuploader.FTCUploader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NetworkUtils {
	private static final Logger LOG = Logging.getLogger(FTCUploader.class);

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
			LOG.info("Uploaded {}", filePath.getFileName());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Method gets the last build error message
	 *
	 * @param robotIp Robot ip
	 * @return <br>
	 * {@code null} if build was successful <br>
	 * {@code ""} or {@code error message} if build failed
	 */
	public static String getErrorMessage(String robotIp) {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url("http://" + robotIp + ":8080/java/build/wait")
				.get()
				.build();

		try(Response res = client.newCall(request).execute()) {
			if(res.body() != null) {
				String body = res.body().string();
				return body.isEmpty() ? null : body;
			} else {
				return "";
			}
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
