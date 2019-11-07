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
import sc.plugin2017.PlayerColor;
import sc.plugin2017.Push;
import sc.plugin2017.Turn;

public class SimplifiedMove extends Move implements Cloneable, Comparable<SimplifiedMove> {

	private static List<SimplifiedMove> possibleBests = new ArrayList<>();

	private boolean possible;
	private boolean noNeedToSlowDown; // if Sandbank
	private int coalNeeded;
	private int freeTurns;
	private int maxCoalToUse;
	private int speedNeeded;
	private Direction currentDirection;
	private Field currentField;

	public SimplifiedMove(GameState gameState, Player player) {

		this(new ArrayList<Action>(), gameState, player);
	}

	public SimplifiedMove(List<Action> selectedActions, GameState gameState, Player player) {

		this(selectedActions, true, false, 0, 1, player.getCoal(), 0, player.getDirection(),
				player.getField(gameState.getBoard()));
		if (gameState.isFreeTurn()) {
			freeTurns += 1;
		}
	}

	public SimplifiedMove(List<Action> actions, boolean possible, boolean noNeedToSlowDown, int coalNeeded,
			int freeTurns, int maxCoalToUse, int speedNeeded, Direction currentDirection, Field currentField) {
		super(actions);
		this.possible = possible;
		this.noNeedToSlowDown = noNeedToSlowDown;
		this.coalNeeded = coalNeeded;
		this.freeTurns = freeTurns;
		this.maxCoalToUse = maxCoalToUse;
		this.speedNeeded = speedNeeded;
		this.currentDirection = currentDirection;
		this.currentField = currentField;
	}

	public int getFreeTurns() {
		return freeTurns;
	}

	public void setFreeTurns(int freeTurns) {
		this.freeTurns = freeTurns;
	}

	public int getMaxCoalToUse() {
		return maxCoalToUse;
	}

	public void setMaxCoalToUse(int maxCoalToUse) {
		this.maxCoalToUse = maxCoalToUse;
	}

	public Direction getCurrentDirection() {
		return currentDirection;
	}

	public void setCurrentDirection(Direction currentDirection) {
		this.currentDirection = currentDirection;
	}

	public Field getCurrentField() {
		return currentField;
	}

	public void setCurrentField(Field currentField) {
		this.currentField = currentField;
	}

	public boolean isNoNeedToSlowDown() {
		return noNeedToSlowDown;
	}

	public int getCoalNeeded() {
		return coalNeeded;
	}

	public int getSpeedNeeded() {
		return speedNeeded;
	}

	public SimplifiedMove clone() {

		SimplifiedMove clone;

		clone = new SimplifiedMove(copyActions(), this.possible, this.noNeedToSlowDown, this.coalNeeded, this.freeTurns,
				this.maxCoalToUse, this.speedNeeded, this.currentDirection, this.currentField.clone());
		return clone;
	}

	public List<Action> copyActions() {

		ArrayList<Action> actionsCopy = new ArrayList<>();

		for (Action a : this.actions) {
			if (a instanceof Acceleration) {
				Acceleration b = (Acceleration) a;
				actionsCopy.add(new Acceleration(b.acc, b.order));
			} else if (a instanceof Advance) {
				Advance b = (Advance) a;
				actionsCopy.add(new Advance(b.distance, b.order));
			} else if (a instanceof Turn) {
				Turn b = (Turn) a;
				actionsCopy.add(new Turn(b.direction, b.order));
			} else if (a instanceof Push) {
				Push b = (Push) a;
				actionsCopy.add(new Push(b.direction, b.order));
			}
		}
		return actionsCopy;
	}

	/**
	 * Differenz der benötigten Kohle ---positiv, wenn mehr benötigt wird
	 */
	@Override
	public int compareTo(SimplifiedMove arg0) {

		return this.coalNeeded - arg0.coalNeeded;
	}

	public void add(Action action, Board board) {

		int[] costs;

		if (this.currentField.getType() == FieldType.SANDBANK) {
			this.possible = false;
			this.noNeedToSlowDown = true;
		}
		if (action instanceof Turn) {
			this.currentDirection = this.currentDirection.getTurnedDirection(((Turn) action).direction);
		}
		costs = getCosts(action, board);
		this.actions.add(action);
		this.coalNeeded += costs[0];
		this.speedNeeded += costs[1];
	}

	public void add(int index, Action action, Board board) {

		int[] costs;

		if (this.currentField.getType() == FieldType.SANDBANK) {
			this.possible = false;
			this.noNeedToSlowDown = true;
		}
		if (action instanceof Turn) {
			this.currentDirection = this.currentDirection.getTurnedDirection(((Turn) action).direction);
		}
		costs = getCosts(action, board);
		this.actions.add(index, action);
		this.coalNeeded += costs[0];
		this.speedNeeded += costs[1];
	}

	public void set(int index, Acceleration action, Board board) {

		remove(index, board);
		add(index, action, board);
	}

	/**
	 * nur für Acc
	 * 
	 * @param index
	 * @param board
	 */
	private void remove(int index, Board board) {

		int[] costs;

		costs = getCosts(this.actions.get(index), board);
		this.actions.remove(index);
		this.coalNeeded -= costs[0];
		this.speedNeeded -= costs[1];
	}

	/**
	 * mit possible und Feld neu setzung
	 * 
	 * @return int[coal, speed]
	 */
	private int[] getCosts(Action action, Board board) {

		int[] cost = { 0, 0 };

		if (action instanceof Acceleration) {
			Acceleration acc = (Acceleration) action;
			cost[0] += (Math.abs(acc.acc) - 1);
			if (this.currentField.getType() == FieldType.SANDBANK) {
				this.possible = false;
			}
		} else if (action instanceof Advance) {
			Advance adv = (Advance) action;
			if (this.currentField.getType() == FieldType.SANDBANK) {
				if (Math.abs(adv.distance) != 1) {
					possible = false;
				} else if (adv.distance == -1) {
					cost[0] += 1;
				}
			}
			try {
				for (int i = 1; i <= adv.distance; ++i) {
					Field nextField = this.currentField.getFieldInDirection(this.currentDirection, board);
					if (!nextField.isPassable()) {
						possible = false;
					}
					if (nextField.getType() == FieldType.LOG) {
						cost[1] += 2;
					} else {
						cost[1] += 1;
					}
					this.currentField = nextField;
				}
			} catch (NullPointerException e) {
				this.possible = false;
			}
		} else if (action instanceof Turn) {
			Turn turn = (Turn) action;
			int turnWidth = Math.abs(turn.direction);
			if (this.freeTurns > 0) {
				int freeTurns = this.freeTurns - turnWidth;
				if (freeTurns >= 0) {
					this.freeTurns = freeTurns;
					turnWidth = 0;
				} else {
					this.freeTurns = 0;
					turnWidth = -freeTurns;
				}
			}
			cost[0] += turnWidth;
			if (this.currentField.getType() == FieldType.SANDBANK) {
				this.possible = false;
			}
		} else if (action instanceof Push) {
			Push push = (Push) action;
			try {
				if (this.currentField.getFieldInDirection(push.direction, board).getType() == FieldType.LOG) {
					cost[1] += 2;
				} else {
					cost[1] += 1;
				}
			} catch (NullPointerException e) {
				this.possible = false;
			}
		}
		return cost;
	}

	public boolean isPossible() {

		return this.possible && (((Acceleration) this.actions.get(0)).acc < -1 || this.coalNeeded < this.maxCoalToUse);
	}

	public boolean isReallyPossible() {

		System.out.println("possible: " + possible);
		System.out.println("coal: " + (this.coalNeeded < this.maxCoalToUse));
		System.out.println("not empty: " + !this.actions.isEmpty());

		return this.possible && (this.coalNeeded < this.maxCoalToUse) && !this.actions.isEmpty();
	}

	public boolean sameFieldAndDirection(SimplifiedMove move) {

		boolean better = false;

		better = (this.currentField.equals(move.currentField) && this.currentDirection == move.currentDirection);
		return better;
	}

	public boolean betterOrEqualCoal(SimplifiedMove move) {

		boolean better = false;

		better = this.coalNeeded <= move.coalNeeded;
		return better;
	}

	public boolean betterOrEqual(SimplifiedMove move) {

		boolean better = false;

		// if (!this.isPossible()) {
		// return false;
		// }
		better = (this.currentField.equals(move.currentField) && this.currentDirection == move.currentDirection
				&& this.coalNeeded <= move.coalNeeded);
		return better;
	}

	public static List<SimplifiedMove> getAllBestMoves(GameState gameState, Player player) {

		return getAllBestMoves(gameState, player, player.getCoal());
	}

	public static List<SimplifiedMove> getAllBestMoves(GameState gameState, Player player, int coalToSpend) {

		SimplifiedMove startMove;
		SimplifiedMove clone;
		List<SimplifiedMove> result;
		Field oppField;

		possibleBests = new ArrayList<>();
		startMove = new SimplifiedMove(gameState, player);
		result = new ArrayList<>();
		startMove.add(new Acceleration(0, 0), gameState.getBoard());
		if (coalToSpend < player.getCoal())
			startMove.setMaxCoalToUse(coalToSpend);
		if (player.getPlayerColor() == PlayerColor.BLUE) {
			oppField = gameState.getRedPlayer().getField(gameState.getBoard());
		} else {
			oppField = gameState.getBluePlayer().getField(gameState.getBoard());
		}
		for (Direction direction : Direction.values()) {
			clone = startMove;
			if (direction != clone.currentDirection) {
				clone = startMove.clone();
				clone.add(new Turn(clone.currentDirection.turnToDir(direction), clone.actions.size()),
						gameState.getBoard());
			}
			getAllBestMoves(clone, gameState, oppField, player.getSpeed());
			// result.addAll(getAllBestMoves(clone, gameState, oppField,
			// player.getSpeed()););
		}
		result.addAll(possibleBests);
		return result;
	}

	private static List<SimplifiedMove> getAllBestMoves(SimplifiedMove move, GameState gameState, Field oppField,
			int playerSpeed) {

		List<SimplifiedMove> res = new ArrayList<>();
		SimplifiedMove newMove;
		List<Direction> pushDirs;
		SimplifiedMove newMove2;

		// neuer Zug
		newMove = move.clone();
		// einen Schritt vorwärts
		newMove.add(new Advance(1, newMove.actions.size()), gameState.getBoard());
		// Abdrängen, wenn auf Gegner
		if (newMove.currentField.equals(oppField)) {
			pushDirs = getPushDirections(gameState.getBoard(), newMove.currentDirection, newMove.currentField);
			for (Direction dir : pushDirs) {
				newMove2 = newMove.clone();
				newMove2.add(new Push(dir, newMove2.actions.size()), gameState.getBoard());
				res.addAll(addTurnSetAccAddMove(newMove2, gameState, oppField, playerSpeed));
			}
		} else {
			res.addAll(addTurnSetAccAddMove(newMove, gameState, oppField, playerSpeed));
		}

		return res;
	}

	private static List<SimplifiedMove> addTurnSetAccAddMove(SimplifiedMove move, GameState gameState,
			Field oppField, int playerSpeed) {

		List<SimplifiedMove> res = new ArrayList<>();
		int acceleration;
		boolean better = true;
		SimplifiedMove newMove;
		for (Direction direction : Direction.values()) {
			newMove = move.clone();
			// Drehen
			if (direction != newMove.currentDirection) {
				newMove.add(new Turn(newMove.currentDirection.turnToDir(direction), newMove.actions.size()),
						gameState.getBoard());
			}
			// Beschleunigung setzen
			acceleration = newMove.speedNeeded - playerSpeed;
			if (newMove.noNeedToSlowDown && acceleration < 0) {
				acceleration = 0;
			}
			newMove.set(0, new Acceleration(acceleration, 0), gameState.getBoard());
			if (newMove.isPossible()) {
				for (int i = 0; i < possibleBests.size(); i++) {
					if (possibleBests.get(i).sameFieldAndDirection(newMove)) {
						if (possibleBests.get(i).betterOrEqualCoal(newMove)) {
							better = false;
							break;
						} else {
							possibleBests.remove(i);
							break;
						}
					}
				}
				if (better) {
					possibleBests.add(getMoveCleaned(newMove));
					res.add(getMoveCleaned(newMove));
					res.addAll(getAllBestMoves(newMove, gameState, oppField, playerSpeed));
				}
			}
		}
		return res;
	}

	public static SimplifiedMove getMoveCleaned(SimplifiedMove move) {

		SimplifiedMove res = move;
		if (((Acceleration) (move.getActions().get(0))).acc == 0) {
			res = move.clone();
			res.actions.remove(0);
		}
		return res;
	}

	public static List<Direction> getPushDirections(Board board, Direction currentDirection, Field currentField) {

		List<Direction> res = new ArrayList<>();

		for (Direction dir : Direction.values()) {
			if (dir == currentDirection.getOpposite()) {
				continue;
			}
			try {
				if (currentField.getFieldInDirection(dir, board).isPassable()) {
					res.add(dir);
				}
			} catch (NullPointerException e) {
				continue;
			}
		}
		return res;
	}

	public Move getEquivalent() {

		Move res = new Move();

		res.actions = copyActions();
		return res;
	}

	@Override
	public String toString() {
		return "SimplifiedMove [possible=" + possible + ", noNeedToSlowDown=" + noNeedToSlowDown + ", coalNeeded="
				+ coalNeeded + ", freeTurns=" + freeTurns + ", maxCoalToUse=" + maxCoalToUse + ", speedNeeded="
				+ speedNeeded + ", currentDirection=" + currentDirection + ", currentField=" + currentField
				+ ", actions=" + actions + "]";
	}
}
