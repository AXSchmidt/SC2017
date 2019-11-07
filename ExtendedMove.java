package sc.player2017.logic;

import java.util.ArrayList;
import java.util.List;

import sc.plugin2017.Acceleration;
import sc.plugin2017.Action;
import sc.plugin2017.Advance;
import sc.plugin2017.Board;
import sc.plugin2017.Direction;
import sc.plugin2017.Field;
import sc.plugin2017.FieldType;
import sc.plugin2017.GameState;
import sc.plugin2017.Move;
import sc.plugin2017.Player;
import sc.plugin2017.Push;
import sc.plugin2017.Turn;

public class ExtendedMove extends Move implements Comparable<ExtendedMove>, Cloneable {

	private boolean coalNeededComputed = false;
	private int coalNeeded = -1;
	private ExtendedField endField;
	private boolean possible = true;

	public ExtendedMove(ExtendedField startField) {

		super();
		this.endField = startField;
		this.coalNeededComputed = false;
	}

	public ExtendedMove(List<Action> selectedActions, ExtendedField startField) {

		super(selectedActions);
		this.endField = startField;
		this.coalNeededComputed = false;
	}

	public ExtendedMove(ArrayList<Action> actions, ExtendedField clone, int coalNeeded2, boolean possible2) {

		this.actions = actions;
		this.endField = clone;
		this.coalNeeded = coalNeeded2;
		this.possible = possible2;
		this.coalNeededComputed = true;
	}

	public int computeCoalNeeded(Player player, GameState gameState) {
		
		int coalNeeded = 0;
		int freeTurns = 1 + (gameState.isFreeTurn() ? 1 : 0);
		Board board = gameState.getBoard();
		Field playerField = player.getField(board).clone();
		Direction dir = player.getDirection();

		for (Action action : this.actions) {
			if (action.getClass() == Advance.class) {
				Advance adv = (Advance) action;
				try {
				if (playerField.getType() == FieldType.SANDBANK && adv.distance < 0) {
					coalNeeded += 1;
				}
				if (adv.distance <= 1) {
					playerField = playerField.getFieldInDirection(adv.distance == 1 ? dir : dir.getOpposite(), board);
				}} catch (NullPointerException e) {
//					System.out.println(this);
					coalNeeded = 4999;
					break;
				}
			} else if (action instanceof Acceleration) {
				Acceleration acc = (Acceleration) action;
				if (acc.acc > 1 || acc.acc < -1) {
					coalNeeded += (Math.abs(acc.acc) - 1);
				}
			} else if (action.getClass() == Turn.class) {
				
				Turn turn = (Turn) action;
				freeTurns -= Math.abs(turn.direction);
				//freeTurns -= (Math.abs(turn.direction) - ((freeTurns < 0) ? 0 : freeTurns));
				dir = dir.getTurnedDirection(turn.direction);
			}
		}
		coalNeeded += (freeTurns > 0) ? 0 : -freeTurns;
		this.coalNeeded = coalNeeded;
		this.coalNeededComputed = true;
		return coalNeeded;
	}
	
	public int speedNeeded(GameState gameState, Player player){

		int speedNeeded = 0;
		Board board = gameState.getBoard();
		Field field = player.getField(board);
		
		for (Action action : this.actions) {
            if (action instanceof Advance) {
                Advance o = (Advance)action;
                speedNeeded += o.distance;
                for (int i = 1; i <= o.distance; ++i) {
                    Field nextField = field.getFieldInDirection(player.getDirection(), board);
                    if (nextField.getType() == FieldType.LOG) {
                        speedNeeded += 1;
                    }
                    field = nextField;
                }
            } else if (action instanceof Push) {
                Push o = (Push)action;
                if (field.getFieldInDirection(o.direction, board).getType() == FieldType.LOG) {
                    speedNeeded += 2;
                } else {
                    speedNeeded += 1;
                }
            }
		}
		return speedNeeded;
	}

	public boolean isPossible(int coal, Player player, GameState gameState) {

		return this.possible && (((Acceleration) this.actions.get(0)).acc < -1 || this.getCoalNeeded(player, gameState) < coal);
	}
	
	/**
	 * @deprecated
	 * @param arg0
	 * @param player
	 * @param gameState
	 * @return
	 */
	public boolean isPossible(int coal){
			return this.possible && this.coalNeeded < coal;
		}

	public boolean lessOrSameCoalNeeded(ExtendedMove arg0, Player player, GameState gameState) {

		return (arg0.getCoalNeeded(player, gameState) - this.getCoalNeeded(player, gameState) >= 0);
	}
	/**
	 * @deprecated
	 * @param arg0
	 * @param player
	 * @param gameState
	 * @return
	 */
	public boolean lessOrSameCoalNeeded(ExtendedMove arg0) {

		return (arg0.coalNeeded - this.coalNeeded >= 0);
	}

	/**
	 * @param gameState 
	 * @param player 
	 * @return Instanzvariable coalNeeded
	 */
	public int getCoalNeeded(Player player, GameState gameState) {
		//das weg acc + setAccs an wird 2
		if(!coalNeededComputed){
			computeCoalNeeded(player, gameState);
		}
		return this.coalNeeded;
	}

	public ExtendedField getEndField() {
		return endField;
	}

	/**
	 * bezieht Endfeldberechnung und possible mit ein
	 * 
	 * @param action
	 * @param playerDir
	 * @param board
	 */
	public void add(Advance action, Direction playerDir, Board board) {

		try {
			boolean hasAdvance = false;
			for(Action a: this.actions){
				if(a instanceof Advance){
					hasAdvance = true;
					break;
				}
			}
			//Bei Sandbank Zug beendet aber nicht wenn auf Sand gestartet
			if(this.endField.getType() == FieldType.SANDBANK && hasAdvance){
				possible = false;
			}
			for (int i = 0; i < action.distance; i++) {
//				this.endField = this.endField.alwaysGetFieldInDirection(playerDir, board);	move kann nicht auf not visible performed werden
				this.endField = new ExtendedField(this.endField.getFieldInDirection(playerDir, board));
				if (!this.endField.isPassable() || board.getField(this.endField.getX(), this.endField.getY())== null) {
					this.possible = false;
					return;
				}
			}
			this.actions.add(action);
			this.coalNeededComputed = false;
		} catch (Exception ex) {
			possible = false;
		}
	}
	
	public static ExtendedMove getMoveCleaned(ExtendedMove move){
		
		ExtendedMove res = move;
		if(((Acceleration)(move.getActions().get(0))).acc == 0){
			res = new ExtendedMove(move.actions, move.endField);
			res.actions.remove(0);
		}
		Prints.println(res, Prints.EXTENDED_MOVE_GET_MOVE_CLEANED);
		if(res.actions.get(0) instanceof Acceleration) Prints.println("getMoveCleaned Acc" + ((Acceleration) res.actions.get(0)).acc, Prints.EXTENDED_MOVE_GET_MOVE_CLEANED);
		return res;
	}

	@Override
	/**
	 * anhand punkte
	 */
	public int compareTo(ExtendedMove o) {

		return this.endField.getPoints() - o.endField.getPoints();
	}

	public static List<ExtendedMove> setAccs(List<ExtendedMove> res, Player player) {

		List<ExtendedMove> result = new ArrayList<>();
		for(ExtendedMove move: res){
			result.add(move.setAcc(player));
		}
		Prints.println(result, Prints.EXTENDED_MOVE_SET_ACC);
		return result;
	}

	private ExtendedMove setAcc(Player player) {

		int needsSpeed = 0;
		Action acc = this.actions.get(0);
		
		for(Action a: actions){
			if(a instanceof Advance){
				needsSpeed+=1;
			}
		}
		if(acc instanceof Acceleration){
			((Acceleration)acc).acc = needsSpeed - player.getSpeed();
			Prints.println(((Acceleration)acc).acc, Prints.EXTENDED_MOVE_SET_ACC);
		}
		Prints.println(this.actions, Prints.EXTENDED_MOVE_SET_ACC);
		Prints.println(needsSpeed - player.getSpeed(), Prints.EXTENDED_MOVE_SET_ACC);
		return this;
	}

	// public ExtendedMove clone() throws CloneNotSupportedException {
	//
	// ArrayList<Action> actions = new ArrayList<>();
	//
	// for(Action a: this.actions){
	// if(a instanceof Advance){
	// actions.add(((Advance)a).clone());
	// }else if(a instanceof Push){
	// actions.add(((Push)a).clone());
	// }else if(a instanceof Turn){
	// actions.add(((Turn)a).clone());
	// }else if(a instanceof
	// Acceleration)actions.add(((Acceleration)a).clone());
	// }
	// return new ExtendedMove(actions, (Field) this.endField.clone(),
	// this.coalNeeded, this.possible);
	// }
}
