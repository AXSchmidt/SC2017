package sc.player2017.logic;

import java.util.ArrayList;
import java.util.List;

import sc.player2017.Starter;
import sc.plugin2017.Acceleration;
import sc.plugin2017.Action;
import sc.plugin2017.Advance;
import sc.plugin2017.Direction;
import sc.plugin2017.Field;
import sc.plugin2017.FieldType;
import sc.plugin2017.GameState;
import sc.plugin2017.IGameHandler;
import sc.plugin2017.Move;
import sc.plugin2017.Player;
import sc.plugin2017.PlayerColor;
import sc.plugin2017.util.InvalidMoveException;
import sc.shared.GameResult;

public class BabbaBot implements IGameHandler {

	private Starter client;
	private GameState gameState;
	private Player currentPlayer;
	private ArrayList<MyMove> allMoves;
	private Move bestMove;
	private int startDepth = 2;
	private boolean useAlphaBeta = true;

	// KONSOLEN AUSGABE
	private boolean print_getPoints_All = true; // Ausgabe in Methode getPoints();
	private boolean print_getPoints_Coal = false; // Ausgabe in Methode getPoints();

	public BabbaBot(Starter client) {
		this.client = client;
	}

	@Override
	public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
		System.out.println("Das Spiel ist beendet.");
	}

	@Override
	public void onRequestAction() {
		long starttime = System.currentTimeMillis();
		System.out.println("BabbaBot 0.9 startet");

		if (isSandbankMove()) {
			return;
		}

		if (useAlphaBeta) {
			this.bestMove = new Move();
			alphabeta(Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, startDepth);
			sendAction(this.bestMove);
		} else {
			List<MyMove> possibleMoves = getAllMoves(new MyMove(currentPlayer, gameState.getBoard()));

			// Finde Zug mit meisten Punkten
			MyMove move = getBestMove(possibleMoves);
			System.out.println("");
			System.out.println("possibleMoves: " + possibleMoves.size());
			System.out.println("BestMyMove " + move.asString(currentPlayer));

			sendAction(move);
		}

		System.out.println("Lauftzeit: " + (System.currentTimeMillis() - starttime) + "ms");
	}

	private boolean isSandbankMove() {
		if (currentPlayer.getField(gameState.getBoard()).getType() == FieldType.SANDBANK) {
			Move move = new Move();
			Field fieldInFront = currentPlayer.getField(gameState.getBoard())
					.getFieldInDirection(currentPlayer.getDirection(), gameState.getBoard());
			Field fieldBehind = currentPlayer.getField(gameState.getBoard())
					.getFieldInDirection(currentPlayer.getDirection().getOpposite(), gameState.getBoard());
			if (fieldInFront != null && fieldInFront.isPassable()) {
				move.actions.add(new Advance(1, 0));
			} else if (fieldBehind != null && fieldBehind.isPassable()) {
				move.actions.add(new Advance(-1, 0));
			} else {
				// Wir sind auf dieser Sandbank gefangen und können keinen
				// gültigen Zug mehr ausführen.
			}
			sendAction(move);
			return true;
		}
		return false;
	}

	// Finde Zug mit meisten Punkten
	private MyMove getBestMove(List<MyMove> possibleMoves) {
		preparePerform();
		MyMove move = new MyMove(currentPlayer, gameState.getBoard());
		int maxPoints = Integer.MIN_VALUE;
		for (MyMove possibleMove : possibleMoves) {
			int points = getPoints(possibleMove);
			if (points > maxPoints) {
				maxPoints = points;
				move = possibleMove;
			}

		}
		System.out.println("");
		System.out.println("Maxpoints: " + maxPoints);
		return move;
	}

	private void preparePerform() {
		// Setze die für perform benötigen Attribute
		currentPlayer.setMovement(currentPlayer.getSpeed());
		currentPlayer.setFreeAcc(1);
		currentPlayer.setFreeTurns(gameState.isFreeTurn() ? 2 : 1);
	}

	private int getPoints(MyMove move) {
		// NUR AUSGABE
		if (print_getPoints_All) {
			System.out.println("");
			System.out.println("Move: " + move.asString(currentPlayer));
			System.out.println("EndField: " + move.getEndField().toString());
		}
		if (print_getPoints_Coal) {
			System.out.println("Move: " + move.asStringCoal(currentPlayer));
		}
		int points = 0;
		Player current = this.gameState.getCurrentPlayer();
		GameState clone = null;
		try {
			clone = gameState.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Problem mit dem Klonen des GameState.");
		}
		try {
			// System.out.println("Test Move: " + m.actions.toString());
			int coal = clone.getCurrentPlayer().getCoal();
			int ownpass = current.getPassenger();

			move.perform(clone, clone.getCurrentPlayer());

			int coal2 = clone.getCurrentPlayer().getCoal();
			int ownpass2 = clone.getCurrentPlayer().getPassenger();
			// Rundenanzahl, Passagiere noch da sind!!!, Restkohle gerne
			// behalten
			if (gameState.getTurn() < 2) {
				if (coal - coal2 > 1) {
					points = points - 300;
				}
			}
			// an verbleibender Kohle orientieren
			// for gestufte
			if (coal2 < coal - 1) {
				points = points - 300;
			}

			if (coal < 4) {
				if (coal2 < coal) {
					points = points - 200;
				}
			}

			if (ownpass < 2) {
				if (ownpass2 == ownpass + 1) {
					points = points + 1000;
				}
			}

			points = points + 40 * clone.getPointsForPlayer(clone.getCurrentPlayerColor());

		} catch (InvalidMoveException e) {
			System.out.println(e.getMessage());
		}
		if (print_getPoints_All) {
			System.out.println("Points " + points);
		}
		return points;
	}

	private List<MyMove> getAllMoves(MyMove move) {

		List<MyMove> possibleMoves = new ArrayList<>();
		Direction curDir = currentPlayer.getDirection();

		// First Call
		if (move.actions.isEmpty()) {
			this.allMoves = new ArrayList<MyMove>();
			move.setDirection(curDir);
			move.setEndfield(currentPlayer.getField(gameState.getBoard()));
			move.add(new Acceleration(move.getAcceleration(currentPlayer), 0));
		}

		for (Direction direction : Direction.values()) {

			// Copy the current move
			MyMove newMove = MyMove.getMyMove(move);

			// Add Turn
			newMove.add(direction);

			// Add one Acceleration
			newMove.add(new Advance(1, newMove.actions.size()));

			// Teste auf Passierbarkeit und Kohleverbrauch
			if (newMove.isPossible(gameState) <= MyMove.POSSIBLE_COAL) {
				// ADD MOVE
				if (canMoveAdd(newMove)) {
					if (newMove.coalNeeded(currentPlayer) <= currentPlayer.getCoal()) {
						this.allMoves.add(MyMove.getMoveCleaned(newMove));
						possibleMoves.add(MyMove.getMoveCleaned(newMove));
					}
					if (newMove.getEndField().getType() != FieldType.SANDBANK) {
						possibleMoves.addAll(getAllMoves(newMove));
					}
				}
			}
		}
		return possibleMoves;
	}

	private boolean canMoveAdd(MyMove move) {
		int delCount = 0;
		for (int i = 0; i < this.allMoves.size(); i++) {
			MyMove existMove = this.allMoves.get(i);
			// Vergleiche Zielfelder
			if (existMove.getEndField().compareTo(move.getEndField()) == 0) {
				if (move.needLessCoalThen(existMove, currentPlayer)) {
					this.allMoves.remove(i - delCount);
					delCount++;
				} else {
					return false;
				}
			}
		}
		return true;
	}

	private int alphabeta(int alpha, int beta, int deep) {

		List<MyMove> moves;
		GameState g = gameState;

		if (deep == 0 || gameEnded()) {
			this.gameState = g;
			return rateAlphaBeta();
		}

		moves = getAllMoves(new MyMove(currentPlayer, gameState.getBoard()));
		if (moves.isEmpty()) {
			this.gameState = g;
			return rateAlphaBeta();
		}
		for (MyMove move : moves) {
			int value;
			try {
				System.err.println("Teste Zug " + move.asString(currentPlayer));
				g = this.gameState.clone();
				move.perform(gameState, gameState.getCurrentPlayer());
				System.err.println("Zug performed");
				gameState.prepareNextTurn(move);
				if (gameState.getCurrentPlayer().getPlayerColor() == g.getCurrentPlayer().getPlayerColor()) {
					value = alphabeta(alpha, beta, deep - 1);
				} else {
					value = -alphabeta(-beta, -alpha, deep - 1);
				}
				if (value >= beta) {
					this.gameState = g;
					return beta;
				}
				if (value > alpha) {
					alpha = value;
					if (deep == startDepth) {
						bestMove = new Move(move.actions);
					}
				}
				gameState = g;
			} catch (CloneNotSupportedException | InvalidMoveException ex) {
				System.out.println(ex.getClass().getSimpleName() + move);
				gameState = g;
			} catch (IndexOutOfBoundsException e) {
				for (Action action : move.actions) {
					//System.err.println(action.getClass().getName() + action.order);
				}
				throw e;
			} finally {
				gameState = g;
			}
		}
		this.gameState = g;
		return alpha;
	}

	private int rateAlphaBeta() {

		int value = 0;
		// da spielertausch bei prepareNextTurn
		Player opponent = this.gameState.getCurrentPlayer();
		Player current = this.gameState.getOtherPlayer();
		int ownPoints = current.getPoints();
		int oppPoints = opponent.getPoints();
		int round = this.gameState.getRound();

		// aktuelle Punktzahl
		value += (ownPoints * 2);
		value -= (oppPoints);
		// // Anzahl Passagner
		value += (current.getPassenger() * 3);
		value -= (opponent.getPassenger());
		// // Zielfeld
		if (ownPoints >= 49 && current.getSpeed() == 1) {
			value += 1000;
		}
		if (oppPoints >= 49 && opponent.getSpeed() == 1) {
			value -= 750;
		}
		// // aktuelle Kohle
		value += ((30 - round) * (49 - oppPoints) * (6 - opponent.getCoal()) * 2);

		return value;
	}

	private boolean gameEnded() {
		Player current = this.gameState.getCurrentPlayer();
		Player opponet = this.gameState.getOtherPlayer();

		return (this.gameState.getTurn() == 60 || (current.getPoints() >= 49 && current.getSpeed() == 1)
				|| (opponet.getPoints() >= 49 && opponet.getSpeed() == 1));
	}

	@Override
	public void onUpdate(Player player, Player otherPlayer) {
		currentPlayer = player;
	}

	@Override
	public void onUpdate(GameState gameState) {
		this.gameState = gameState;
		currentPlayer = gameState.getCurrentPlayer();
	}

	@Override
	public void sendAction(Move move) {
		// client.sendMove(move);
		client.sendMove(new Move(move.getActions()));
	}

}
