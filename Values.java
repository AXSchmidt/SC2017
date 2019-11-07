package sc.player2017.logic;

import java.util.ArrayList;
import java.util.List;

public class Values {

	private Values() {
	}
	
	public static final boolean ALPHA_BETA = true;

	public static int FACTOR_OWN_POINTS = 500;
	public static int FACTOR_OPP_POINTS = 100;

	public static int FACTOR_OWN_PASSANGERS = 2000000;
	public static int FACTOR_OPP_PASSENGERS = 500;

	public static int VALUE_OWN_GOAL = 9999999;
	public static int VALUE_OPP_GOAL = 1000;

	public static int VALUE_OWN_SANDBANK = 30;
	public static int VALUE_OPP_SANDBANK = 20;
	
	public static int FACTOR_OWN_COAL = 9;
	public static int FACTOR_OPP_COAL = 1;

	public static List<Integer> getAllValues() {

		List<Integer> res = new ArrayList<>();

		res.add(FACTOR_OWN_POINTS);
		res.add(FACTOR_OPP_POINTS);
		res.add(FACTOR_OWN_PASSANGERS);
		res.add(FACTOR_OPP_PASSENGERS);
		res.add(VALUE_OWN_GOAL);
		res.add(VALUE_OPP_GOAL);
		res.add(VALUE_OWN_SANDBANK);
		res.add(VALUE_OPP_SANDBANK);
		res.add(FACTOR_OWN_COAL);
		res.add(FACTOR_OPP_COAL);
		return res;
	}

	public static boolean setAllValues(List<Integer> values) {

		if (values.size() == 10) {
			FACTOR_OWN_POINTS = values.get(0);
			FACTOR_OPP_POINTS = values.get(1);
			FACTOR_OWN_PASSANGERS = values.get(2);
			FACTOR_OPP_PASSENGERS = values.get(3);
			VALUE_OWN_GOAL = values.get(4);
			VALUE_OPP_GOAL = values.get(5);
			VALUE_OWN_SANDBANK = values.get(6);
			VALUE_OPP_SANDBANK = values.get(7);
			FACTOR_OWN_COAL = values.get(8);
			FACTOR_OPP_COAL = values.get(9);
			return true;
		} else {
			return false;
		}
	}
}
