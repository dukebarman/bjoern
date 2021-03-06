package bjoern.pluginlib.structures;

import bjoern.nodeStore.NodeTypes;
import bjoern.structures.edges.EdgeTypes;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import octopus.lib.structures.OctopusNode;

public class Function extends OctopusNode
{

	public Function(Vertex vertex)
	{
		super(vertex, NodeTypes.FUNCTION);
	}

	public GremlinPipeline<?, BasicBlock> basicBlocks()
	{
		return new GremlinPipeline<>().start(this.getBaseVertex()).out(EdgeTypes.IS_FUNCTION_OF).transform(BasicBlock::new);
	}

}
