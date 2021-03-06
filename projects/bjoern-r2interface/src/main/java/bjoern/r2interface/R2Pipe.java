package bjoern.r2interface;

// Adapted from:
// https://github.com/radare/radare2-bindings/blob/master/r2pipe/java/org/radare/r2pipe/R2Pipe.java

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class R2Pipe
{
	private static final Logger logger = LoggerFactory.getLogger(R2Pipe.class);

	public final String R2_LOC = "radare2";
	private final Process process;
	private OutputStream stdin;
	private InputStream stdout;
	private StreamGobbler errorGobbler;

	public R2Pipe(String filename) throws IOException
	{
		process = spawnR2Process(filename);
		connectProcessPipes();
		readUpToZeroByte();
	}

	private void connectProcessPipes()
	{
		stdin = process.getOutputStream();
		stdout = process.getInputStream();
		errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
		errorGobbler.start();
	}

	private Process spawnR2Process(String filename) throws IOException
	{
		try
		{
			return Runtime.getRuntime().exec(R2_LOC + " -q0 " + filename);
		}
		catch (IOException e)
		{
			throw new IOException("Cannot find `radare2` on path.");
		}
	}

	public String cmd(String cmd) throws IOException
	{
		cmdNoResponse(cmd);
		return readUpToZeroByte();
	}

	public void cmdNoResponse(String cmd) throws IOException
	{
		logger.info("r2 command: {}", cmd);
		cmd += "\n";

		stdin.write((cmd).getBytes());
		stdin.flush();
	}

	public String readUpToZeroByte() throws IOException
	{
		StringBuffer sb = new StringBuffer();
		byte[] b = new byte[1];
		while (stdout.read(b) == 1)
		{
			if (b[0] == '\0')
				break;
			sb.append((char) b[0]);
		}
		return sb.toString();
	}

	public String readNextLine() throws IOException
	{
		StringBuffer sb = new StringBuffer();
		byte[] b = new byte[1];
		while (stdout.read(b) == 1)
		{
			if (b[0] == '\n' || b[0] == '\0')
				break;
			sb.append((char) b[0]);
		}
		return sb.toString();
	}

	public void quit() throws Exception
	{
		cmd("q");
	}

}