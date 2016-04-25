package bjoern.plugins.radareimporter;

import org.json.JSONObject;

import bjoern.input.radare.RadareExporter;
import bjoern.pluginlib.BjoernProject;
import bjoern.pluginlib.PluginAdapter;
import octopus.server.components.orientdbImporter.ImportCSVRunnable;
import octopus.server.components.orientdbImporter.ImportJob;
import octopus.server.components.projectmanager.OctopusProject;
import octopus.server.components.projectmanager.ProjectManager;

public class RadareImporterPlugin extends PluginAdapter {

	String projectName;
	private BjoernProject project;

	@Override
	public void configure(JSONObject settings)
	{
		projectName = settings.getString("projectName");
	}

	@Override
	public void execute() throws Exception
	{
		project = openProject();
		String pathToBinary = project.getPathToBinary();
		analyzeBinaryWithR2(pathToBinary);

	}

	private void analyzeBinaryWithR2(String pathToBinary)
	{
		String pathToProjectDir = project.getPathToProjectDir();
		String nodeFilename = project.getNodeFilename();
		String edgeFilename = project.getEdgeFilename();
		String dbName = project.getDatabaseName();

		RadareExporter radareExporter = new RadareExporter();
		radareExporter.tryToExport(pathToBinary, pathToProjectDir, null);

		ImportJob importJob = new ImportJob(nodeFilename, edgeFilename, dbName);
		(new ImportCSVRunnable(importJob)).run();

	}

	private BjoernProject openProject()
	{
		OctopusProject oProject = ProjectManager.getProjectByName(projectName);
		if(oProject == null)
			throw new RuntimeException("Error: project does not exist");

		return new BjoernProject(oProject);
	}

	@Override
	public void beforeExecution() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterExecution() throws Exception {
		// TODO Auto-generated method stub

	}

}
