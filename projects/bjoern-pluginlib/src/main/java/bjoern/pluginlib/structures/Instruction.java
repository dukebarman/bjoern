package bjoern.pluginlib.structures;

import com.tinkerpop.blueprints.Vertex;

import bjoern.nodeStore.NodeTypes;
import bjoern.structures.BjoernNodeProperties;
import octopus.lib.structures.Node;

public class Instruction extends Node implements Comparable<Instruction>
{

	public Instruction(Vertex vertex)
	{
		super(vertex, NodeTypes.INSTRUCTION);
	}

	public long getAddress()
	{
		return Long.parseLong(getNode().getProperty(BjoernNodeProperties.ADDR).toString());
	}

	public String getEsilCode()
	{
		return getNode().getProperty(BjoernNodeProperties.ESIL);
	}

	@Override
	public int compareTo(Instruction instruction)
	{
		if (this.getAddress() < instruction.getAddress())
		{
			return -1;
		} else if (this.getAddress() > instruction.getAddress())
		{
			return 1;
		} else
		{
			return 0;
		}
	}

	public String getCode()
	{
		return this.getNode().getProperty("repr");
	}
}