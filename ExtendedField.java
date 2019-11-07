package sc.player2017.logic;
import java.util.ArrayList;
import java.util.List;

import sc.plugin2017.Board;
import sc.plugin2017.Direction;
import sc.plugin2017.Field;
import sc.plugin2017.FieldType;

public class ExtendedField extends Field {

	public ExtendedField(Field field) {
		super(field.getType(), field.getX(), field.getY());
		// TODO Auto-generated constructor stub
	}
	
	public ExtendedField(FieldType type, int x, int y) {
		super(type, x, y);
		// TODO Auto-generated constructor stub
	}

	public ExtendedField(FieldType type, int x, int y, int points) {
		super(type, x, y, points);
		// TODO Auto-generated constructor stub
	}

	public ExtendedField alwaysGetFieldInDirection(Direction dir, Board board){
		
		return new ExtendedField(super.alwaysGetFieldInDirection(dir, board));
	}
	
	public List<Direction> getDirectionsToPassableBorderFields(Board board){
		
		List<Direction> res = new ArrayList<>();
		
		for(Direction dir: Direction.values()){
			try{
			if(alwaysGetFieldInDirection(dir, board).isPassable()){
				res.add(dir);
			}
			}catch(NullPointerException e){
				continue;
			}
		}
		return res;
	}
}
