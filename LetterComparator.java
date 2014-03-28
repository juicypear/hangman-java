import java.util.Map;
import java.util.Comparator;

/*** 
 Class to implement comparator interface to be used in sorting hashmaps 
***/

class LetterComparator implements Comparator<Character> 
{
	Map<Character, Integer> map;

	public LetterComparator(Map<Character, Integer> _map) {
    	this.map = _map;
	}

    //Higher map values should precede lower values
    public int compare(Character a, Character b) {
        
        if (map.get(a) >= map.get(b)) 
        {
            return -1;
        } 
        else 
        {
            return 1;
        }
    }
    
}