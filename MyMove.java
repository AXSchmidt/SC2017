package sc.player2017.logic;

import java.util.List;

import com.jogamp.opengl.util.Gamma;

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

public class MyMove extends Move {

	public static final int POSSIBLE_YES = 0;
	public static final int POSSIBLE_COAL = 1;
	public static final int POSSIBLE_SPEED = 2;
	public static final int POSSIBLE_PASSABLE = 3;

	private Field endField; // Zielfeld des Moves
	private Direction dir; // Ausrichtung des letzten Feldes
	private int countAdvance = 0; // Anzahl der Bewegungen nach vorne
	private int countTurn = 0; // Anzahl der Drehungen
	private int countTrunk = 0; // Anzahl der Baumstaemme die gerade ueberfahren
								// wurden
	private int countPush = 0; // Anzahl der Bewegungspunkte fuer Gegner

	public MyMove() {
		// super();
	}

	public MyMove(Player player, Board board) {
		super();
		this.endField = player.getField(board);
		this.dir = player.getDirection();
	}

	public MyMove(Field startField, Direction direction) {
		super();
		this.endField = startField;
		this.dir = direction;
	}

	public MyMove(List<Action> selectedActions, Field startField, Direction direction, int advance, int turn, int trunk,
			int push) {
		super(selectedActions);
		this.endField = startField;
		this.dir = direction;
		this.countAdvance = advance;
		this.countTurn = turn;
		this.countTrunk = trunk;
		this.countPush = push;
	}

	public static MyMove getMyMove(MyMove move) {
		return new MyMove(move.actions, move.getEndField(), move.getDirection(), move.getCountAdvance(),
				move.getCountTurn(), move.getCountTrunk(), move.getCountPush());
	}

	public String asString(Player player) {
		return this.actions.toString() + " P" + printField(this.endField) + " Coal: " + this.coalNeeded(player)
				+ ", Advance: " + this.getCountAdvance() + ", Turn " + this.getCountTurn() + ", Trunk "
				+ this.getCountTrunk() + ", Push " + this.getCountPush();
	}

	public String asStringCoal(Player player) {
		return printField(this.endField) + " Coal: " + this.coalNeeded(player) + ", Advance: " + this.getCountAdvance()
				+ ", Turn " + this.getCountTurn();
	}

	private String printField(Field field) {
		return "(" + addSpaces(field.getX(), 2) + "/" + addSpaces(field.getY(), 2) + ")";
	}

	private String addSpaces(int value, int len) {
		String result = String.valueOf(value);
		while (result.length() < len) {
			result = " " + result;
		}
		return result;
	}

	public Field getEndField() {
		return this.endField;
	}

	public Direction getDirection() {
		return this.dir;
	}

	public void setEndfield(Field field) {
		this.endField = field;
	}

	public void setDirection(Direction direction) {
		this.dir = direction;
	}

	public int getCountAdvance() {
		return this.countAdvance;
	}

	public int getCountTurn() {
		return this.countTurn;
	}

	public int getCountTrunk() {
		return this.countTrunk;
	}

	public int getCountPush() {
		return this.countPush;
	}

	public int getAcceleration(Player player) {
		return Math.max(0, this.countAdvance + this.countTrunk + this.countPush - player.getSpeed());
	}

	public void add(Action action) {
		// Advance
		if (action instanceof Advance) {
			this.countAdvance++;
			this.moveToDir(this.dir);
		}
		this.actions.add(action);
	}

	// Add Turn
	public void add(Direction direction) {
		if (this.dir != direction) {
			int turnSize = this.dir.turnToDir(direction);
			this.dir = direction;
			this.countTurn += Math.abs(turnSize);
			this.add(new Turn(turnSize, this.actions.size()));
		}
	}

	private void moveToDir(Direction direction) {
		int x = this.endField.getX();
		int y = this.endField.getY();
		switch (direction) {
		case DOWN_LEFT:
			if (y % 2 != 0) {
				x--;
			}
			y++;
			break;
		case DOWN_RIGHT:
			if (y % 2 == 0) {
				x++;
			}
			y++;
			break;
		case LEFT:
			x--;
			break;
		case RIGHT:
			x++;
			break;
		case UP_LEFT:
			if (y % 2 != 0) {
				x--;
			}
			y--;
			break;
		case UP_RIGHT:
			if (y % 2 == 0) {
				x++;
			}
			y--;
			break;
		}
		this.endField = new Field(this.endField.getType(), x, y);
	}

	public int coalNeeded(Player player) {
		int speed = player.getSpeed();

		// turn
		int coal = Math.max(0, this.countTurn - player.getFreeTurns());
		// accelerate
		if (this.countAdvance > (speed + player.getFreeAcc())) {
			coal += this.countAdvance - speed - player.getFreeAcc();
		}
		// brake
		if (this.countAdvance < (speed - player.getFreeAcc())) {
			coal += speed - player.getFreeAcc() - this.countAdvance;
		}
		// trunk
		coal += this.countTrunk;
		// push action
		coal += this.countPush;

		return coal;
	}

	public int isPossible(GameState gameState) {

		Board board = gameState.getBoard();
		Player player = gameState.getCurrentPlayer();
		Player opponent = gameState.getOtherPlayer();

		// Teste auf Passierbarkeit
		Field newField = board.getField(this.endField.getX(), this.endField.getY());
		if (newField == null || !newField.isPassable()) {
			return POSSIBLE_PASSABLE;
		}

		this.setEndfield(newField);

		// Boot ueberquert Baumstamm
		if (newField.getType() == FieldType.LOG) {
			this.countTrunk++;
		}

		// Gegner schubsen
		if (newField.equals(opponent.getField(board))) {
			this.countPush++;
			Push push = new Push();
			Field oppPushField = newField.clone();
			for (Direction direction : Direction.values()) {
				//if (direction != this.dir.getOpposite()) {
					oppPushField = this.endField.getFieldInDirection(direction, board);
					if (oppPushField != null && oppPushField.isPassable()) {
						push = new Push(direction, this.actions.size());
					}
				//}
			}
			this.add(push);
			if (oppPushField.getType() == FieldType.SANDBANK) {
				this.countPush++;
			}
		}

		// Update Acceleration
		this.actions.set(0, new Acceleration(this.getAcceleration(player)));

		// Teste auf MaxSpeed Ueberschreitung
		if (player.getSpeed() + this.getAcceleration(player) > 6) {
			return POSSIBLE_SPEED;
		}

		// Teste auf Kohleverbrauch
		if (this.coalNeeded(player) > player.getCoal()) {
			return POSSIBLE_COAL;
		}

		return POSSIBLE_YES;
	}

	public boolean needLessCoalThen(MyMove move, Player player) {
		return (this.coalNeeded(player) < move.coalNeeded(player));
	}

	public static MyMove getMoveCleaned(MyMove move) {
		MyMove result = move;
		if (((Acceleration) (move.getActions().get(0))).acc == 0) {
			// result = new MyMove(move.actions, move.endField);
			result = MyMove.getMyMove(move);
			result.actions.remove(0);
		}
		return result;
	}

}
