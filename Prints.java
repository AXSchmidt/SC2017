package sc.player2017.logic;

public abstract class Prints {

	private static final boolean PRINTS_ON = true;
	
	//Logic
	public static final boolean LOGIC_ON_REQUEST_ON = true;
	public static final boolean LOGIC_WEIGHTED_MOVES_ON = false;
	public static final boolean LOGIC_ALPHABETA_ON = false;
	public static final boolean LOGIC_RATE_ON = false;
	
	//ExtendedMove
	public static final boolean EXTENDED_MOVE_GET_MOVE_CLEANED = false;
	public static final boolean EXTENDED_MOVE_SET_ACC = false;
	
	public static void print(Object toPrint, boolean on) {

		if (PRINTS_ON && on) {
			System.out.print(toPrint.toString());
		}
	}

	public static void println(Object toPrint, boolean on) {

		if (PRINTS_ON && on) {
			System.out.println(toPrint.toString());
		}
	}
}
