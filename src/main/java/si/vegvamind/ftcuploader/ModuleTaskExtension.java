package si.vegvamind.ftcuploader;

import org.gradle.api.tasks.SourceSet;

public class ModuleTaskExtension {
	private SourceSet sss;

	public SourceSet getSss() {
		return sss;
	}

	public void setSss(SourceSet sss) {
		this.sss = sss;
	}
}
