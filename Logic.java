package sc.player2017.logic;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import com.thoughtworks.xstream.io.binary.Token.Value;

import sc.player2017.Starter;
import sc.player2017.logic.*;
import sc.plugin2017.Acceleration;
import sc.plugin2017.Action;
import sc.plugin2017.Advance;
import sc.plugin2017.Board;
import sc.plugin2017.Direction;
import sc.plugin2017.Field;
import sc.plugin2017.FieldType;
import sc.plugin2017.GameState;
import sc.plugin2017.IGameHandler;
import sc.plugin2017.Move;
import sc.plugin2017.Player;
import sc.plugin2017.PlayerColor;
import sc.plugin2017.Push;
import sc.plugin2017.Turn;
import sc.plugin2017.util.InvalidMoveException;
import sc.shared.GameResult;

/**
 * Das Herz des Simpleclients: Eine sehr simple Logik, die ihre Zuege zufaellig
 * waehlt, aber gueltige Zuege macht. Ausserdem werden zum Spielverlauf
 * Konsolenausgaben gemacht.
 */
public class Logic implements IGameHandler {

	private Starter client;
	private GameState gameState;
	private Player currentPlayer;
	private int startDepth = 2;
	private SimplifiedMove bestMove;
	private ArrayList<ExtendedMove> weightedMoves;

	private static final Logger log = LoggerFactory.getLogger(Logic.class);
	/*
	 * Klassenweit verfuegbarer Zufallsgenerator der beim Laden der klasse
	 * einmalig erzeugt wird und darn immer zur Verfuegung steht.
	 */
	private static final Random rand = new SecureRandom();

	/**
	 * Erzeugt ein neues Strategieobjekt, das zufaellige Zuege taetigt.
	 *
	 * @param client
	 *            Der Zugrundeliegende Client der mit dem Spielserver
	 *            kommunizieren kann.
	 */
	public Logic(Starter client) {
		this.client = client;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
		log.info("Das Spiel ist beendet.");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRequestAction() {
		
		System.out.println("\n\n######## Runde: " + this.gameState.getRound() + " ########\n");

		// muss drin sein
		currentPlayer.setMovement(currentPlayer.getSpeed());
		currentPlayer.setFreeAcc(1);
		currentPlayer.setFreeTurns(gameState.isFreeTurn() ? 2 : 1);

//		System.out.println(gameState);

		long millis = System.currentTimeMillis();

		List<SimplifiedMove> s = SimplifiedMove.getAllBestMoves(gameState, currentPlayer, 1);
		int i = 0;
		int coalNeededQcoal = 0;
		GameState clone;
		// for (SimplifiedMove smove : s) {
		// System.out.print(smove);
		// try {
		// clone = gameState.clone();
		// smove.perform(clone, clone.getCurrentPlayer());
		// System.out.print("coal used: " + (6 -
		// clone.getCurrentPlayer().getCoal()) + ", needed: "
		// + smove.getCoalNeeded());
		// if (6 - clone.getCurrentPlayer().getCoal() == smove.getCoalNeeded())
		// {
		// coalNeededQcoal++;
		// }
		// System.out.println();
		// } catch (CloneNotSupportedException | InvalidMoveException e) {
		// System.out.println(e.getMessage());
		// i++;
		// }
		// }
		// System.out.println("\nsize:" + s.size());
		// System.out.println("CoalNeededQCoal: " + coalNeededQcoal);
		// System.out.println("not performed: " + i);
		this.bestMove = new SimplifiedMove(this.gameState, this.currentPlayer);

		if (Values.ALPHA_BETA) {
			alpha(Integer.MIN_VALUE, this.startDepth, this.startDepth, this.gameState);
			// alphabeta(Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
			// startDepth);
		} else {
			this.bestMove.actions.add(new Advance(1, 0));
		}
		try {
			this.bestMove.perform(gameState, currentPlayer);
			this.gameState.prepareNextTurn(bestMove);
			// System.out.println(rate());
		} catch (InvalidMoveException e) {
			e.printStackTrace();
		}
		// System.out.println(this.bestMove.actions);
		// System.out.println("bestMove possible: " + isMovePossible(bestMove));
		if (!isMovePossible(bestMove)) {
			System.out.println("#### s");
			this.bestMove = getBestMove(s);
		}
		if (!isMovePossible(bestMove)) {
			System.out.println("#### rand");
			this.bestMove = getRandomMove();
		}
		sendAction(bestMove.getEquivalent());
		System.out.println("Time needed: " + (System.currentTimeMillis() - millis) + "ms");
	}

	private SimplifiedMove getBestMove(List<SimplifiedMove> possibleMoves) {
		SimplifiedMove move = new SimplifiedMove(this.gameState, this.currentPlayer);
		// Finde Zug mit meisten Punkten
		int maxPoints = Integer.MIN_VALUE;
		TestGameState clone = null;
		for (SimplifiedMove possibleMove : possibleMoves) {
			// Klone gameState
			try {
				clone = new TestGameState(gameState);
			} catch (CloneNotSupportedException e) {
				log.error("Problem mit dem Klonen des GameState.", e);
			}
			performOnGameStateEasyOpp(clone, possibleMove, true);

			int points = rateAfterMove(clone);
			if (points > maxPoints) {
				maxPoints = points;
				move = possibleMove;
			}
			gameState = clone;
		}
		return move;
	}

	private SimplifiedMove getRandomMove() {
		SimplifiedMove move = new SimplifiedMove(this.gameState, this.currentPlayer);
		// Setze die für perform benötigen Attribute
		currentPlayer.setMovement(currentPlayer.getSpeed());
		currentPlayer.setFreeAcc(1);
		currentPlayer.setFreeTurns(gameState.isFreeTurn() ? 2 : 1);

		List<SimplifiedMove> possibleMoves = new ArrayList<SimplifiedMove>();

		System.out.println("Random");

		// Sandbank
		if (currentPlayer.getField(gameState.getBoard()).getType() == FieldType.SANDBANK) {
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
			log.info("Bin auf Sandbank, sende Zug {}", move);
			return move;
		}
		// Zuege in alle Richtungen durchprobieren
		for (Direction direction : Direction.values()) {
			// try moves for current speed
			for (Advance action : gameState.getPossibleMovesInDirection(currentPlayer, currentPlayer.getSpeed(),
					direction, currentPlayer.getCoal())) {
				SimplifiedMove newMove = new SimplifiedMove(this.gameState, this.currentPlayer);
				int index = 0;
				if (currentPlayer.getDirection() != direction) {
					newMove.actions.add(new Turn(currentPlayer.getDirection().turnToDir(direction), index++));
				}
				newMove.actions.add(new Advance(action.distance, index++));
				possibleMoves.add(newMove);
			}
			// try moves when accelerating or decelerating (depending if we are
			// currently at speed 1 or 2)
			int otherSpeed = currentPlayer.getSpeed() == 1 ? 2 : 1;
			for (Advance action : gameState.getPossibleMovesInDirection(currentPlayer, otherSpeed, direction,
					currentPlayer.getCoal())) {
				SimplifiedMove newMove = new SimplifiedMove(this.gameState, this.currentPlayer);
				int index = 0;
				newMove.actions.add(new Acceleration(otherSpeed - currentPlayer.getSpeed(), index++));
				if (currentPlayer.getDirection() != direction) {
					newMove.actions.add(new Turn(currentPlayer.getDirection().turnToDir(direction), index++));
				}
				newMove.actions.add(new Advance(action.distance, index++));
				possibleMoves.add(newMove);
			}
		}
		System.out.println("RandomPossibleSize" + possibleMoves.size());
		move = getBestMove(possibleMoves);
		if (move.actions.isEmpty()) {
			// move = possibleMoves.get(new
			// Random().nextInt(possibleMoves.size()));
		}
		return move;
	}

	private boolean isMovePossible(Move move) {
		SimplifiedMove eq = new SimplifiedMove(this.gameState, this.gameState.getCurrentPlayer());
		for (Action action : move.actions) {
			eq.add(action, gameState.getBoard());
		}
		return eq.isReallyPossible();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Player player, Player otherPlayer) {
		currentPlayer = player;
		log.info("Spielerwechsel: " + player.getPlayerColor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(GameState gameState) {
		this.gameState = gameState;
		currentPlayer = gameState.getCurrentPlayer();
		log.info("Das Spiel geht voran: Zug: {}", gameState.getTurn());
		log.info("Spieler: {}", currentPlayer.getPlayerColor());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendAction(Move move) {
		client.sendMove(move);
	}

	// alphabeta(IntegerMinValue+1 , Integer.Max_Value -1, startDepth)
	/**
	 * 
	 * @param alpha
	 *            IntegerMinValue+1
	 * @param beta
	 *            Integer.Max_Value -1
	 * @param deep
	 *            startDepth
	 * @return rate of move;
	 */
	private int alphabeta(int alpha, int beta, int deep) {

		List<SimplifiedMove> moves;
		GameState g = gameState;

		Prints.println("-alphabeta( " + alpha + ", " + beta + ", " + deep + ")", Prints.LOGIC_ALPHABETA_ON);
		if (deep == 0 || gameEnded()) {
			Prints.println("return/ Depth: " + deep + " ended: " + gameEnded(), Prints.LOGIC_ALPHABETA_ON);
			this.gameState = g;
			return rate();
		}
		moves = SimplifiedMove.getAllBestMoves(this.gameState, this.gameState.getCurrentPlayer(), 2);
		if (moves.isEmpty()) {
			Prints.println("return/ no possible moves", Prints.LOGIC_ALPHABETA_ON);
			this.gameState = g;
			return rate();
		}
		for (Move move : moves) {
			int value;
			try {
				g = this.gameState.clone();
				move.perform(gameState, gameState.getCurrentPlayer());
				gameState.prepareNextTurn(move);
				if (gameState.getCurrentPlayer().getPlayerColor() == g.getCurrentPlayer().getPlayerColor()) {
					value = alphabeta(alpha, beta, deep - 1);
				} else {
					value = -alphabeta(-beta, -alpha, deep - 1);
				}
				Prints.println(
						"Depth : " + (startDepth - deep) + " Value: " + value + "; Alpha: " + alpha + "; Beta: " + beta,
						Prints.LOGIC_ALPHABETA_ON);
				if (value >= beta) {
					Prints.println("return/ value > beta", Prints.LOGIC_ALPHABETA_ON);
					this.gameState = g;
					return beta;
				}
				if (value > alpha) {
					Prints.println("rate > alpha", Prints.LOGIC_ALPHABETA_ON);
					alpha = value;
					if (deep == startDepth) {
						Prints.println("deep == startDepth", Prints.LOGIC_ALPHABETA_ON);
						Prints.println(move.actions + ": " + value, Prints.LOGIC_ALPHABETA_ON);
						bestMove = (SimplifiedMove) move.clone();
					}
				}
				gameState = g;
			} catch (CloneNotSupportedException | InvalidMoveException ex) {
				Prints.println(ex.getClass().getSimpleName() + move, Prints.LOGIC_ALPHABETA_ON);

				gameState = g;
			} catch (IndexOutOfBoundsException e) {
				for (Action action : move.actions) {
					System.err.println(action.getClass().getName() + action.order);
				}
				throw e;
			} finally {
				gameState = g;
			}
		}
		Prints.println("return/ ende", Prints.LOGIC_ALPHABETA_ON);
		this.gameState = g;
		return alpha;
	}

	private int alpha(int alpha, int depth, int maxDepth, GameState gameState) {

		List<SimplifiedMove> moves;
		TestGameState g = null;
		int value;

		try {
			g = new TestGameState(gameState.clone());
		} catch (CloneNotSupportedException e1) {
		}
		if (depth == 0 || gameEnded()) {
			System.out.println("Normal");
			return rateAfterMove(g);
		}
		// if(depth != maxDepth){
		// g.getOtherTestPlayer().moveAway(g);
		// }
		moves = SimplifiedMove.getAllBestMoves(g, g.getCurrentPlayer(), 2);
		System.out.println("Moves size" + (2-depth) + ": "+ moves.size());
		if (moves.isEmpty()) {
			System.out.println("Empty");
			return rateAfterMove(g);
		}
		for (SimplifiedMove move : moves) {
			try {
				// g = new TestGameState(gameState.clone());
				if (performOnGameStateEasyOpp(g, move, depth != maxDepth)) {
					value = alpha(alpha, depth - 1, maxDepth, g);
					if (depth == maxDepth) {
						System.out.println("move" + move.actions + value);
					}
					if (value > alpha) {
						alpha = value;
						if (depth == maxDepth) {
							this.bestMove = move.clone();
							System.out.println("best" + move.actions + value);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return alpha;// alpha
	}

	private boolean performOnGameStateEasyOpp(TestGameState gameState, Move move, boolean moveOppAway) {

		// if(moveOppAway){
		// gameState.getOtherTestPlayer().moveAway(gameState);
		// }

//		System.out.println(gameState);
		if (performOnGameState(gameState, move)) {
			move = new Move();
			move.actions.add(new Advance(1, 1));
			gameState.getCurrentTestPlayer().moveAway(gameState);	//wegen seiten tausch
//			System.out.println(gameState);
//			 System.exit(0);
			if (performOnGameState(gameState, move)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private boolean performOnGameState(GameState gameState, Move move) {

		Player current = gameState.getCurrentPlayer();
		current.setMovement(current.getSpeed());
		current.setFreeAcc(1);
		current.setFreeTurns(gameState.isFreeTurn() ? 2 : 1);
		try {
			move.perform(gameState, current);
			gameState.prepareNextTurn(move);
			return true;
		} catch (InvalidMoveException e) {
			return false;
		}
	}

	private int rateAfterMove(GameState gameState) {

		int value = 0;
		Player opponent = gameState.getCurrentPlayer();
		Player current = gameState.getOtherPlayer();
		int ownPoints = current.getPoints();
		int oppPoints = opponent.getPoints();
		int round = gameState.getRound();

		value += ownPoints;
		// value -= oppPoints;
		value += current.getPassenger();
		System.out.println("value: " + value + ", points: " + ownPoints + ", passengers: " + current.getPassenger() + ", current:" + current);
		// value += current.getCoal();

		return value;
	}

	private int rate() {

		int value = 0;
		// da spielertausch bei prepareNextTurn
		Player opponent = this.gameState.getCurrentPlayer();
		Player current = this.gameState.getOtherPlayer();
		int ownPoints = current.getPoints();
		int oppPoints = opponent.getPoints();
		int round = this.gameState.getRound();

		Prints.println("-rate", Prints.LOGIC_RATE_ON);
		// aktuelle Punktzahl
		value += (ownPoints * Values.FACTOR_OWN_POINTS);
		// value -= (oppPoints * Values.FACTOR_OPP_POINTS);
		// // Anzahl Passagner
		value += (current.getPassenger() * Values.FACTOR_OWN_PASSANGERS);
		// value -= (opponent.getPassenger() * Values.FACTOR_OPP_PASSENGERS);
		// // Zielfeld
		if (ownPoints >= 49 && current.getSpeed() == 1) {
			value += Values.VALUE_OWN_GOAL;
		}
		// if (oppPoints >= 49 && opponent.getSpeed() == 1) {
		// value -= Values.VALUE_OPP_GOAL;
		// }
		// // aktuelle Kohle
		value += ((30 - round) * (49 - ownPoints) * (6 - current.getCoal()) * Values.FACTOR_OWN_COAL);
		// gegner
		value -= ((30 - round) * (49 - oppPoints) * (6 - opponent.getCoal()) * Values.FACTOR_OPP_COAL);

		Prints.println("Rate: " + value, Prints.LOGIC_RATE_ON);

		return value;
	}

	private boolean gameEnded() {

		Player current = this.gameState.getCurrentPlayer();
		Player opponet = this.gameState.getOtherPlayer();

		return (this.gameState.getTurn() == 60 || (current.getPoints() >= 49 && current.getSpeed() == 1)
				|| (opponet.getPoints() >= 49 && opponet.getSpeed() == 1));
	}

	private class TestGameState extends GameState {

		protected TestPlayer redTestPlayer;
		protected TestPlayer blueTestPlayer;

		public TestGameState(GameState stateToClone) throws CloneNotSupportedException {
			super(stateToClone);
			// change players to own implementation
			redTestPlayer = new TestPlayer(super.getRedPlayer());
			// setRedPlayer(redTestPlayer);
			blueTestPlayer = new TestPlayer(super.getBluePlayer());
			// setBluePlayer(blueTestPlayer);
//			System.out.println(this);
//			System.exit(0);
		}

		public TestPlayer getRedTestPlayer() {
			return redTestPlayer;
		}

		public TestPlayer getBlueTestPlayer() {
			return blueTestPlayer;
		}

		public TestPlayer getOtherTestPlayer() {
			return new TestPlayer(getOtherPlayer());
		}

		public TestPlayer getCurrentTestPlayer() {
			return new TestPlayer(getCurrentPlayer());
		}

		private void addField(Field field) {

			Board board = getBoard();
			for (int i = 0; i < board.getTiles().size(); i++) {
				if (board.getTiles().get(i).isVisible()) {
					board.getTiles().get(i).fields.add(field);
					break;
				}
			}
			setBoard(board);
		}

		public void setFieldsAroundPlayerWater(Player player) {

			Field playerField;

			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
				playerField = new Field(FieldType.WATER, player.getX() + i, player.getY() + j);
				addField(playerField);
				}
			}
			put(player.getX(), player.getY(), player);
		}

		public void setCurrentPlayer(PlayerColor current) {
			super.setCurrentPlayer(current);
		}
		
		public void setRedPlayer(Player red){
			super.setRedPlayer(red);
		}
		
		public void setBluePlayer(Player blue){
			super.setBluePlayer(blue);
		}

	}

	private class TestPlayer extends Player {

		public TestPlayer(Player playerToClone) {
			super(playerToClone);
			super.setPlayerColor(playerToClone.getPlayerColor());
		}

		public void moveAway(TestGameState gameState) {
			put(99, 99, 10);
			setSpeed(1);
			gameState.setFieldsAroundPlayerWater(this);
			if(this.getPlayerColor() == PlayerColor.RED){
				gameState.setRedPlayer(this);
				gameState.setCurrentPlayer(PlayerColor.RED);
			}else{
				gameState.setBluePlayer(this);
				gameState.setCurrentPlayer(PlayerColor.BLUE);
			}
		}
	}

}
