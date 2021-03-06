package bjoern.plugins.vsa.transformer;

import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bjoern.pluginlib.radare.emulation.esil.ESILKeyword;
import bjoern.pluginlib.radare.emulation.esil.ESILTokenEvaluator;
import bjoern.pluginlib.radare.emulation.esil.ESILTokenStream;
import bjoern.plugins.vsa.domain.AbstractEnvironment;
import bjoern.plugins.vsa.domain.ValueSet;
import bjoern.plugins.vsa.structures.Bool3;
import bjoern.plugins.vsa.structures.DataWidth;
import bjoern.plugins.vsa.structures.StridedInterval;
import bjoern.plugins.vsa.transformer.esil.ESILTransformationException;

public class ESILTransformer implements Transformer
{
	private Logger logger = LoggerFactory.getLogger(ESILTransformer.class);
	private AbstractEnvironment outEnv;
	private Deque<Object> esilStack;

	private ESILTokenStream tokenStream;
	private ESILTokenEvaluator esilParser = new ESILTokenEvaluator();

	public ESILTransformer() {}


	@Override
	public AbstractEnvironment transform(String esilCode, AbstractEnvironment env)
	{
		outEnv = new AbstractEnvironment(env);
		esilStack = new LinkedList<>();
		tokenStream = new ESILTokenStream(esilCode);

		logger.info("Transforming [" + esilCode + "]");

		if (esilCode.equals(""))
		{
			return outEnv;
		}

		while (tokenStream.hasNext())
		{
			String token = tokenStream.next();
			if (esilParser.isEsilKeyword(token))
			{
				executeEsilCommand(ESILKeyword.fromString(token));
			} else if (esilParser.isNumericConstant(token))
			{
				esilStack.push(ValueSet
						.newGlobal(StridedInterval.getSingletonSet(esilParser.parseNumericConstant(token), DataWidth.R64)));
			} else if (esilParser.isRegister(token))
			{
				esilStack.push(token);
			} else if (esilParser.isFlag(token))
			{
				esilStack.push(token);
			} else
			{
				throw new ESILTransformationException("Unknown ESIL token (" + token + ")");
			}
		}

		return outEnv;
	}

	private ValueSet getValueSetOfObject(Object obj)
	{
		if (obj instanceof ValueSet)
		{
			return (ValueSet) obj;
		} else if (obj instanceof Bool3)
		{
			return getValueSetOfBooleanValue((Bool3) obj);
		} else if (obj instanceof String && esilParser.isRegister((String) obj))
		{
			return outEnv.getValueSetOfRegister((String) obj);
		} else if (obj instanceof String && esilParser.isFlag((String) obj))
		{
			return getValueSetOfBooleanValue(outEnv.getValueOfFlag((String) obj));
		}
		throw new ESILTransformationException(
				"Object cannot be represented as value set: " + obj.getClass().getSimpleName());
	}

	private ValueSet getValueSetOfBooleanValue(Bool3 value)
	{
		if (value == Bool3.TRUE)
		{
			return ValueSet.newGlobal(StridedInterval.getSingletonSet(1, DataWidth.R64));
		} else if (value == Bool3.FALSE)
		{
			return ValueSet.newGlobal(StridedInterval.getSingletonSet(0, DataWidth.R64));
		} else
		{
			return ValueSet.newGlobal(StridedInterval.getInterval(0, 1, DataWidth.R64));
		}
	}

	private Bool3 getBooleanValueOfObject(Object obj)
	{
		if (obj instanceof Bool3)
		{
			return (Bool3) obj;
		} else if (obj instanceof String && esilParser.isFlag((String) obj))
		{
			return outEnv.getValueOfFlag((String) obj);
		} else if (obj instanceof ValueSet)
		{
			return getBooleanValueOfValueSet((ValueSet) obj);
		} else if (obj instanceof String && esilParser.isRegister((String) obj))
		{
			return getBooleanValueOfValueSet(outEnv.getValueSetOfRegister((String) obj));

		}
		throw new ESILTransformationException("Object cannot be represented as boolean value: " + obj);
	}

	private Bool3 getBooleanValueOfValueSet(ValueSet valueSet)
	{
		StridedInterval si = valueSet.getValueOfGlobalRegion();
		if (!si.isBottom())
		{
			if (si.isZero())
			{
				return Bool3.FALSE;
			} else if (!si.contains(0))
			{
				return Bool3.TRUE;
			} else
			{
				return Bool3.MAYBE;
			}
		}
		throw new ESILTransformationException("Value set cannot be represented as boolean value: " + valueSet);
	}

	private ValueSet popValueSet()
	{
		return getValueSetOfObject(esilStack.pop());
	}

	private Bool3 popBooleanValue()
	{
		return getBooleanValueOfObject(esilStack.pop());
	}

	private String popRegisterOrFlag()
	{
		Object obj = esilStack.pop();
		if (obj instanceof String)
		{
			String identifier = (String) obj;
			if (esilParser.isRegister(identifier) || esilParser.isFlag(identifier))
			{
				return identifier;
			}
		}
		throw new ESILTransformationException("Expected register of flag, found " + obj);
	}

	private void executeEsilCommand(ESILKeyword command)
	{
		logger.debug("Executing esil command: " + command);
		logger.debug("Stack content: " + esilStack);
		logger.debug("Environment: " + outEnv);
		switch (command)
		{
			case ASSIGNMENT:
				executeAssignment();
				break;
			case COMPARE:
				logger.warn("Operation (compare) not yet implemented");
				esilStack.pop();
				esilStack.pop();
				break;
			case SMALLER:
			case SMALLER_OR_EQUAL:
			case BIGGER:
			case BIGGER_OR_EQUAL:
				logger.warn("Operation (smaller*, bigger*) not yet implemented");
				esilStack.pop();
				esilStack.pop();
				esilStack.push(Bool3.MAYBE);
				break;
			case SHIFT_LEFT:
				executeShiftLeft();
				break;
			case SHIFT_RIGHT:
				executeShiftRight();
				break;
			case ROTATE_LEFT:
				executeRotateLeft();
				break;
			case ROTATE_RIGHT:
				executeRotateRight();
				break;
			case AND:
				executeBitAnd();
				break;
			case OR:
				executeBitOr();
				break;
			case XOR:
				executeBitXor();
				break;
			case ADD:
				executeAdd();
				break;
			case SUB:
				executeSub();
				break;
			case MUL:
				executeMul();
				break;
			case DIV:
				executeDiv();
				break;
			case MOD:
				executeMod();
				break;
			case NEG:
				executeLogicalNot();
				break;
			case INC:
				executeInc();
				break;
			case DEC:
				executeDec();
				break;
			case ADD_ASSIGN:
				executeAddAssign();
				break;
			case SUB_ASSIGN:
				executeSubAssign();
				break;
			case MUL_ASSIGN:
				executeMulAssign();
				break;
			case DIV_ASSIGN:
				executeDivAssign();
				break;
			case MOD_ASSIGN:
				executeModAssign();
				break;
			case SHIFT_LEFT_ASSIGN:
				executeShiftLeftAssign();
				break;
			case SHIFT_RIGHT_ASSIGN:
				executeShiftRightAssign();
				break;
			case AND_ASSIGN:
				executeAndAssign();
				break;
			case OR_ASSIGN:
				executeOrAssign();
				break;
			case XOR_ASSIGN:
				executeXorAssign();
				break;
			case INC_ASSIGN:
				executeIncAssign();
				break;
			case DEC_ASSIGN:
				executeDecAssign();
				break;
			case NEG_ASSIGN:
				executeNegAssign();
				break;
			case POKE:
			case POKE_AST:
			case POKE1:
			case POKE2:
			case POKE4:
			case POKE8:
				executePoke();
				break;
			case PEEK:
			case PEEK_AST:
			case PEEK1:
			case PEEK2:
			case PEEK4:
			case PEEK8:
				executePeek();
				break;
			case START_CONDITIONAL:
				executeConditional();
				break;
			case END_CONDITIONAL:
				break;
			default:
				break;
		}
	}

	private void executeMul()
	{
		esilStack.push(popValueSet().mul(popValueSet()));
	}

	private void executeMulAssign()
	{
		Object element = esilStack.peek();
		executeMul();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeDiv()
	{
		esilStack.push(popValueSet().div(popValueSet()));
	}

	private void executeDivAssign()
	{
		Object element = esilStack.peek();
		executeDiv();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeMod()
	{
		esilStack.push(popValueSet().mod(popValueSet()));
	}

	private void executeModAssign()
	{
		Object element = esilStack.peek();
		executeMod();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeShiftLeft()
	{
		esilStack.push(popValueSet().shiftLeft(popValueSet()));
	}

	private void executeShiftLeftAssign()
	{
		Object element = esilStack.peek();
		executeShiftLeft();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeShiftRight()
	{
		esilStack.push(popValueSet().shiftRight(popValueSet()));
	}

	private void executeShiftRightAssign()
	{
		Object element = esilStack.peek();
		executeShiftRight();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeRotateLeft()
	{
		esilStack.push(popValueSet().rotateLeft(popValueSet()));
	}

	private void executeRotateRight()
	{
		esilStack.push(popValueSet().rotateRight(popValueSet()));
	}

	private void executeNeg()
	{
		executeLogicalNot();
	}

	private void executeLogicalNot()
	{
		esilStack.push(popBooleanValue().not());
	}

	private void executeBitNeg()
	{
		esilStack.push(popValueSet().not());
	}

	private void executeNegAssign()
	{
		Object element = esilStack.peek();
		executeNeg();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeXor()
	{
		Object obj1 = esilStack.pop();
		Object obj2 = esilStack.pop();
		if (obj1 instanceof Bool3 || obj2 instanceof Bool3 || (obj1 instanceof String && esilParser.isFlag((String) obj1)) || (
				obj2 instanceof String && esilParser.isFlag((String) obj2)))
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeLogicalXor();
		} else
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeBitXor();
		}
	}


	private void executeXorAssign()
	{
		Object element = esilStack.peek();
		executeXor();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeLogicalXor()
	{
		esilStack.push(popBooleanValue().xor(popBooleanValue()));
	}

	private void executeBitXor()
	{
		esilStack.push(popValueSet().xor(popValueSet()));
	}

	private void executeOr()
	{
		Object obj1 = esilStack.pop();
		Object obj2 = esilStack.pop();
		if (obj1 instanceof Bool3 || obj2 instanceof Bool3 || (obj1 instanceof String && esilParser.isFlag((String) obj1)) || (
				obj2 instanceof String && esilParser.isFlag((String) obj2)))
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeLogicalOr();
		} else
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeBitOr();
		}
	}

	private void executeOrAssign()
	{
		Object element = esilStack.peek();
		executeOr();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeLogicalOr()
	{
		esilStack.push(popBooleanValue().or(popBooleanValue()));
	}

	private void executeBitOr()
	{
		esilStack.push(popValueSet().or(popValueSet()));
	}

	private void executeAnd()
	{
		Object obj1 = esilStack.pop();
		Object obj2 = esilStack.pop();

		if (obj1 instanceof Bool3 || obj2 instanceof Bool3 || (obj1 instanceof String && esilParser.isFlag((String) obj1)) || (
				obj2 instanceof String && esilParser.isFlag((String) obj2)))
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeLogicalAnd();
		} else
		{
			esilStack.push(obj2);
			esilStack.push(obj1);
			executeBitAnd();
		}
	}

	private void executeAndAssign()
	{
		Object element = esilStack.peek();
		executeAnd();
		esilStack.push(element);
		executeAssignment();
	}

	private void executeBitAnd()
	{
		esilStack.push(popValueSet().and(popValueSet()));
	}

	private void executeLogicalAnd()
	{
		esilStack.push(popBooleanValue().and(popBooleanValue()));
	}

	private void executeConditional()
	{
		Bool3 bool3 = popBooleanValue();
		if (bool3 == Bool3.FALSE)
		{
			tokenStream.skipUntilToken(ESILKeyword.END_CONDITIONAL.keyword);
		} else if (bool3 == Bool3.MAYBE)
		{
			StringBuilder builder = new StringBuilder();
			do
			{
				builder.append(tokenStream.next()).append(",");
			} while (tokenStream.hasNext());

			builder.setLength(builder.length() - 1);
			String esilCode = builder.toString();

			AbstractEnvironment amc = new ESILTransformer().transform(esilCode, outEnv);
			if (esilCode.indexOf("}") == esilCode.length() - 1)
			{
				this.outEnv = amc.union(this.outEnv);
			} else
			{
				this.outEnv = amc
						.union(new ESILTransformer().transform(esilCode.substring(esilCode.indexOf("}") + 2), outEnv));
			}
		}
	}

	private void executePeek()
	{
		logger.warn("Loading data from memory not yet supported");
		esilStack.pop();
		esilStack.push(ValueSet.newTop(DataWidth.R64));
	}

	private void executePoke()
	{
		logger.warn("Writing data to memory not yet supported");
		esilStack.pop();
		esilStack.pop();
	}

	private void executeInc()
	{
		esilStack.push(ValueSet.newGlobal(StridedInterval.getSingletonSet(1, DataWidth.R64)));
		executeAdd();
	}

	private void executeIncAssign()
	{
		Object obj = esilStack.peek();
		executeInc();
		esilStack.push(obj);
		executeAssignment();
	}

	private void executeDec()
	{
		esilStack.push(ValueSet.newGlobal(StridedInterval.getSingletonSet(1, DataWidth.R64)));
		executeSub();
	}

	private void executeDecAssign()
	{
		Object obj = esilStack.peek();
		executeDec();
		esilStack.push(obj);
		executeAssignment();
	}

	private void executeAdd()
	{
		esilStack.push(popValueSet().add(popValueSet()));
	}

	private void executeAddAssign()
	{
		Object obj = esilStack.peek();
		executeAdd();
		esilStack.push(obj);
		executeAssignment();
	}

	private void executeSub()
	{
		esilStack.push(popValueSet().sub(popValueSet()));
	}

	private void executeSubAssign()
	{
		Object obj = esilStack.peek();
		executeSub();
		esilStack.push(obj);
		executeAssignment();
	}

	private void executeAssignment()
	{
		String identifier = popRegisterOrFlag();
		if (esilParser.isRegister(identifier))
		{
			outEnv.setValueSetOfRegister(identifier, popValueSet());
		} else if (esilParser.isFlag(identifier))
		{
			outEnv.setValueOfFlag(identifier, popBooleanValue());
		}
	}

}
