import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Queue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Scanner;


public class HangmanStrategy implements GuessingStrategy
{
	/*
		SimulatedMostWordsFilteredComparator NOT CURRENTLY USED
		The class implements the Comparator interface, by 
		looking at the leftover set of a correct letter guess
		
		FUTURE TODO: loop through each of the set words and simulate if they
		were the "correct" hangman secret - how many words would that remove
		e.g. if processing FACTUAL and the correct guess is the 
		letter 't' and it is the correct hangman word and the current 
		formation is -A----A-, we would simulate it is as -A--T-A- .
		Then we see the number of words it removes, do this for
		all the words and remaining 'letters to guess' combinations
		then average that for each letter and then compare letters
	*/
	private static class SimulatedMostWordsFilteredComparator
		implements Comparator<Character> 
	{

		private Set<String> clone = new HashSet<String>();

		private String hangman = null;
		private int wordLength = 0;

		// Initialize the instance through the constructor
		public SimulatedMostWordsFilteredComparator(Set<String> set,
			String _hangman, int _wordLength)
		{
			//copy words collection to clone
			for(String copy: set)
			{
				clone.add(copy);
			}

			hangman = _hangman;
			wordLength = _wordLength;
		}

		// Compare the two characters using criteria of which eliminates the most
		// words
		//
		// FUTURE TODO : loop through each of the words in the set as if they were 
		// 				 the actual hangman, and pass in a different hangman string
		//				 for each filterWordsCorrectGuess call
		public int compare(Character c1, Character c2) {
            
			int score1 = HangmanStrategy.filterWordsCorrectGuess(c1, clone, 
				hangman, wordLength, true);
			int score2 = HangmanStrategy.filterWordsCorrectGuess(c2, clone, 
				hangman, wordLength, true);
            
			HangmanUtil.debug("word filtered simulated letter " + c1 
				+ ", [score is: " + score1 + "]");
			HangmanUtil.debug("word filtered simulated letter " + c2 
				+ ", [score is: " + score2 + "]");

            if (score1 >= score2)
            {
           		return -1;
	        } 
	        else 
	        {
	            return 1;
	        }
        }
	}

	/*
		The class implements the Comparator interface, by looking at 
		leftover sets of letter guesses and seeing if they are interesting 
		e.g. as an outlier in some way as compared to the standard set 
		of english letter frequency distribution using standard deviations
	*/
	private static class SimulatedOutlierSetComparator
			implements Comparator<Character> 
	{

		private Set<String> clone = new HashSet<String>();

		// Initialize the instance through the constructor
		public SimulatedOutlierSetComparator(Set<String> set)
		{
			//copy words collection to clone
			for(String copy: set)
			{
				clone.add(copy);
			}

			HangmanUtil.debug("In constructor, clone is: " + clone);
		}

		// Compare these two characters based on their set outlier scores
		// set being the set returned after the characters have been guessed 
		// as wrong letter guesses
		// (This is one of many interesting types of statistics 
		// that can be inferred)
		public int compare(Character c1, Character c2) {
            
            Set<String> s1 = cloneSet(clone);
            Set<String> s2 = cloneSet(clone);

			HangmanStrategy.filterWordsWrongGuess(c1, s1, true);
			HangmanStrategy.filterWordsWrongGuess(c2, s2, true);

			double score1 = HangmanStrategy.outlierScore(s1);
            double score2 = HangmanStrategy.outlierScore(s2);

            s1.clear();
            s2.clear();

            s1 = null;
            s2 = null;

			HangmanUtil.debug("outlier simulated letter " + c1 
				+ ", [score is: " + score1 + "]");
			HangmanUtil.debug("outlier simulated letter " + c2 
				+ ", [score is: " + score2 + "]");

            if (score1 >= score2)
            {
           		return -1;
	        } 
	        else 
	        {
	            return 1;
	        }
        }

        // Create a new copy of the set
        private Set<String> cloneSet(Set<String> set)
        {
        	Set<String> cloned = new HashSet<String>();
        	for(String copy: set)
        	{
        		cloned.add(copy);
        	}

        	return cloned;
        }
	}



	/*
	  NOTE: Using the Java HashMap predominately, as it can easily handle 
	  millions of insertions.  Doing less than a million insertions 
	
	  The facts:
	  174k words ~ in words.txt
	  9.08 avg letters per word from : 
	  	awk '{print length}' words.txt | awk '{sum += $1} END {print sum}'

	  Since we are dealing with only 1 word length during each playhangman
	  game, the largest word set is length 8, with 28,558 words

	  28558 words * 9.1 avg letters per word ~ 260k letters 
	  260K puts during each game pass is less than a million insertions

	  Distribution follows:

		awk '{print length}' words.txt | sort -n | uniq -c
		// words length
			  96 2
			 978 3
			3919 4
			8672 5
			15290 6
			23208 7
			28558 8
			25011 9
			20404 10
			15581 11
			11382 12
			7835 13
			5134 14
			3198 15
			1938 16
			1125 17
			 594 18
			 328 19
			 159 20
			  62 21
			  29 22
			  13 23
			   9 24
			   2 25
			   2 27
			   1 28

	*/

	/* Static fields */

	/*
	  Relative percentage frequency of letters used in English language
	  From wikipedia http://en.wikipedia.org/wiki/Letter_frequency
	  Index 0 corresponds to 'a' which is 8.167%, index 1 to 'b' which 
	  is 1.492% etc..
	  Letter e is the highest with 12.702% letter frequency
	*/									
	private static final double[] relativeLetterFreqEnglishLang = 
		new double[]{8.167, 1.492, 2.782, 4.253, 12.702, 2.228, 2.015, 
			6.094, 6.966, 0.153, 0.772, 4.025, 2.406, 6.749, 7.507, 1.929, 
			0.095, 5.987, 6.327, 9.056, 2.758, 0.978, 2.360, 0.150, 1.974, 
			0.074};

	//Constants used in determining when to 
	//employ certain guessing strategies
	private static final int MEDIUM_WORD_SET_SIZE = 550;
	private static final int SMALL_WORD_SET_SIZE = 20;
	private static final int TINY_WORD_SET_SIZE = 5;
	private static final int MICRO_WORD_SET_SIZE = 2;

	private static final int TOP_N_THRESHOLD = 3;


	//This map contains the set of words keyed by their size
	private static Map<Integer, Set<String>> sizeWordMap = 
	  new HashMap<Integer, Set<String>>();

	//This map contains the counts of all the characters in each of 
	// the word length arranged word sets
	private static Map<Integer, Map<Character, Integer>> letterCountsMapSets = 
	  new HashMap<Integer, Map<Character, Integer>>(); 


	//This map contains the counts of all the characters in the dictionary
	private static Map<Character, Integer> letterCountsMapDictionary = 
		new HashMap<Character, Integer>();


	/* Instance member fields */	

	//Current character counts map of current word set
	private Map<Character, Integer> letterCountsMap = 
		new HashMap<Character, Integer>();

	//Current set of possible hangman solution words
	//Used to add and remove words frequently and quickly
	private Set<String> wordSet = new HashSet<String>();

	//Current set of already guessed letters
	//Used as a fast lookup for guessed letters
	private Set<Character> alreadyGuessedLetters = new HashSet<Character>();

	//Current queue of most frequent letters given current word set
	private Queue<Character> freqLetterQueue = 
		new ArrayDeque<Character>();

	//Game state
	private int answerLength = 0;
	private int numWrongGuessesRemaining = 0;
	private String hangman = null;

	//State managing guesses
	private Character lastLetterGuessed = null;
	private String lastWordGuessed = null;
	private boolean lastGuessIsLetter = false;
	private boolean lastGuessIsWord = false;

	
	/**
	 * Create a letter frequency map from the hangman letters 
	 * used to compare if possible words match the hangman formation
	 */
	private static void createHangmanLetterMap(String hangman, 
							Map<Character, Integer> map, int validLength)
	{
		try
		{
			if(hangman == null || map == null)
			{
				throw new IllegalArgumentException("input parameter(s)" +
					 "can't be null");
			}

			int length = hangman.length();
			
			if(length != validLength)
			{
				throw new IllegalStateException("hangman length doesn't " +
					"equal assert length");
			}

			for(int i=0; i<length; i++)
			{
				Character letter = Character.toLowerCase(hangman.charAt(i));

				if(letter != HangmanGame.MYSTERY_LETTER)
				{
					if(map.get(letter) == null)
					{
						map.put(letter, 1);
					}
					else
					{
						int count = map.get(letter);
						map.put(letter, count + 1);
					}
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * (DEPRECATED)
	 * Checks if the word is valid given the new hangman formation 
	 * and correctly guessed letter.
	 *
	 * NOTE: This is the replacement of three checks in 
	 * filterWordsCorrectGuessHelper rolled into one.  Three checks: 
	 * HangmanUtil.hangmanLetterCountsMatch, 
	 * HangmanUtil.hangmanLetterPositionsMatch and 
	 * word.indexOf(guessedCorrectLetter) == -1
	 *
	 * Rather than doing 3 passes of the word, doing 1 pass with 
	 * all the 3 checks
	 */
	private static boolean isValidPossibleWord(Character guessedCorrectLetter, 
				String word, Map<Character, Integer> map, String hangman)
	{
		boolean match = false;

		boolean guessedCorrectLetterInWord = false;

		boolean hangmanPositionMatch = true;

		try
		{
			if(guessedCorrectLetter == null || word == null || 
					map == null || hangman == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			int length = hangman.length();

			if(word.length() != length)
			{
				throw new IllegalStateException("Word length doesn't" 
					+ " equal hangman length");
			}

			Map<Character, Integer> hangmanLetterCounts = 
				new HashMap<Character, Integer>();

			hangmanLetterCounts.putAll(map);
			
			for(int i=0; i<length; i++)
			{
				Character letter = word.charAt(i);

				HangmanUtil.debug("correct letter is: " 
					+ guessedCorrectLetter + ", hangman is: " 
					+ hangman + ", word is: "+ word + 
					", word[i] is: " + letter);

				if(guessedCorrectLetter.equals(letter))  
				{
					guessedCorrectLetterInWord = true;
				}

				//If we actually have some letters guessed correctly
				//in the hangman formation
				if(hangmanLetterCounts.keySet().size() >= 1)
				{
					//Only care if the letter is in the hangman Map
					if(hangmanLetterCounts.get(letter) != null)
					{
						int count = hangmanLetterCounts.get(letter);
						hangmanLetterCounts.put(letter, count - 1);
					}

					//Match correct positions of hangman non-mystery letters 
					//in word.  If no match, return match is false

					if(hangman.charAt(i) != HangmanGame.MYSTERY_LETTER)
					{
						//if the hangman letter doesn't match the letter 
						//in the word at the same position, can't be a 
						//possible word, return false
						if(hangman.charAt(i) 
								!= Character.toUpperCase(word.charAt(i)))
						{
							HangmanUtil.debug("not a valid possible " 
								+ "word (no pos match)");

						//	HangmanUtil.debug("hangman is: " + 
						//		hangman + ", word is: "
						//		+ word + ", hangman[i] is: " + 
						//		hangman.charAt(i) 
						//		+ ", word[i] is: " + word.charAt(i));

							hangmanPositionMatch = false;

							return false;
						}
					}
				}
			}

			//if there is no occurence of the correctly guessed letter 
			//in the word return
			if(guessedCorrectLetterInWord == false) 
			{
				HangmanUtil.debug("not a valid possible word (correct " 
					+ "letter not found)");

				return false; 
			}

			if(hangmanPositionMatch == true) match = true;

			//Now we've looked at all the characters in the word and 
			//reconciled them with the hangman map
			//Lets check to ensure the values are all 0, if not its 
			//not a match.
			Iterator<Integer> itr = hangmanLetterCounts.values().iterator();		

			while (itr.hasNext())
			{
    			int count = itr.next();
    			if(count != 0) 
    			{
    				//match = false;
    				//break;
    				HangmanUtil.debug("not a valid possible word " 
    					+ "(hangman letter counts don't match word)");

    				return false;
    			}
    			else 
    			{
    				match = true;
    			}
			}

			//if we are still in the function at this point, 
			//ensure that the hangman formation isn't just all blanks
			if(hangmanLetterCounts.keySet().size() == 0)
			{
				//They match because we don't know any letters of 
				//the hangman yet, so it could be anything
				match = true;
			}

			hangmanLetterCounts.clear();
			hangmanLetterCounts = null;

			itr = null;
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return match;
	}


	/**
	 * We traverse this hangman hashmap with the word. If it has the letters
	 * in the hangman then we update the hashmap accordingly by reducing
	 * the count of that letter by 1, so if they match then the hashmap
	 * values for its keys should end up being 0, if not they don't match
	 *
	 * NOTE: E.g Handles case where word has 3 a's and hangman has 2 a's
	 *			 Where the two hangman a's are in the same position
	 *			 as the word
	 */
	private static boolean hangmanLetterCountsMatch(String word, 
											Map<Character, Integer> map)
	{
		boolean match = false;

		try
		{
			if(word == null || map == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ "can't be null");
			}

			Map<Character, Integer> temp = new HashMap<Character, Integer>();

			temp.putAll(map);

			if(temp.keySet().size() >= 1)
			{
				int length = word.length();
				
				for(int i=0; i<length; i++)
				{
					Character letter = word.charAt(i);

					//Only care if the letter is in the hangman Map
					if(temp.get(letter) != null)
					{
						int count = temp.get(letter);
						temp.put(letter, count - 1);
					}
				}

				//Now we've looked at all the characters in the word and 
				//reconciled them with the hangman map
				//Lets check to ensure the values are all 0, if not its 
				//not a match.
				Iterator<Integer> itr = temp.values().iterator();		

				while (itr.hasNext())
				{
	    			int count = itr.next();
	    			if(count != 0) 
	    			{
	    				match = false;
	    				break;
	    			}
	    			else 
	    			{
	    				match = true;
	    			}
				}

				temp.clear();
				temp = null;

				itr = null;
			}
			else
			{
				//They match because we don't know any letters of 
				//the hangman yet, so it could be anything
				match = true;
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return match;
	}

	/**
	 * Match correct positions of hangman non-mystery letters in word
	 * If no match, return match is false
	 */
	private static boolean hangmanLetterPositionsMatch(String word, 
													   String hangman)
	{
		boolean match = true;

		try
		{
			if(word == null || hangman == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			int length = hangman.length();

			if(word.length() != length)
			{
				throw new IllegalStateException("Word length doesn't " 
					+ "equal hangman length");
			}
			else
			{
				for(int i=0; i<length; i++)
				{
					//Match correct positions of hangman non-mystery letters 
					//in word.  If no match, return match is false

					if(hangman.charAt(i) != HangmanGame.MYSTERY_LETTER)
					{
						if(hangman.charAt(i) != 
								Character.toUpperCase(word.charAt(i)))
						{
						/*	HangmanUtil.debug("hangman is: " 
								+ hangman + ", word is: "
								+ word + ", hangman[i] is: " 
								+ hangman.charAt(i) 
								+ ", word[i] is: " + word.charAt(i)); */

							return false;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return match;
	}

	/**
	 * Records the number of words containing various letters.  
	 * If an exclusion set is provided, ignore the letters found in the 
	 * exclusion set.  Takes in an empty queue.
	 * Returns a map with the letter count frequency and a sorted
	 * queue with the letters in increasing order of word frequency
	 */
	private static void tallyLetterWordFrequency(Set<String> set, 
			Set<Character> exclusion, Map<Character, Integer> map, 
			Queue<Character> sortedQueue) 
	{
		try
		{
			if(set == null || map == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't" + " be null");
			}

			for(String s: set)
			{
				if(exclusion != null)
				{
					HangmanStrategy.tallyUniqLetterFrequency(s, true, exclusion, map);
				}
				else
				{
					HangmanStrategy.tallyUniqLetterFrequency(s, false, null, map);
				}
			}

			if(sortedQueue != null)
			{
				HangmanStrategy.sortLetterMap(map, sortedQueue, null);
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * Given a word, tally the various letters in the word by uniqueness.  
	 * If there are two a's record only 1 a.  If an exclusion set is 
	 * provided, ignore the letters found in the exclusion set.  
	 * Returns a map with the letter count frequency.
	 */
	private static void tallyUniqLetterFrequency(String word, 
				boolean useExclusionSet, Set<Character> exclusion, 
				Map<Character, Integer> map) 
	{
		try
		{
			if(word == null || map == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			if(useExclusionSet == true && exclusion == null)
			{
				throw new IllegalArgumentException("specified exclusion " 
					+ "set intent, but exclusion set can't be null");
			}

			//We only want to record one entry for each letter in the word
			//If a word has two e's for example only mark e once
			//Thus the word letter frequency for e is '1' word

			Set<Character> processedLetters = new HashSet<Character>();

			for(int i=0; i<word.length(); i++)
			{
				char letter = word.charAt(i);

				boolean alreadyProcessed = processedLetters.contains(letter);

				boolean exclude = false;

				//check if we want to ignore certain letters
				if(true == useExclusionSet)
				{
					exclude = exclusion.contains(letter);
				}

				//We are going to tally only those letters 
				//that we haven't guessed already
				if(false == exclude && false == alreadyProcessed)
				{
					//check if letter key has a value
					if(map.get(letter) == null)
					{
						map.put(letter, 1);
					}
					else
					{
						int count = map.get(letter);
						map.put(letter, count + 1);
					}

					processedLetters.add(letter);
				}
			}

			processedLetters.clear();
			processedLetters = null;
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * Take the character frequency count hashmap and sort it using the 
	 * passed in comparator. If none is provided, the default one 
	 * will be applied sorting most frequent characters to the 
	 * head of the output queue.
	 *
	 * NOTE: Ignoring the case where there are multiple characters 
	 * with the same count	
	 */
	private static void sortLetterMap(Map<Character, Integer> map, 
			Queue<Character> queue, Comparator<Character> cmp) 
	{
		try
		{
			if(map == null || queue == null)
			{
				throw new IllegalArgumentException("input parameter(s) " 
					+ "can't be null");
			}

			boolean useDefaultComparator = false;

			if(cmp == null) 
			{
				cmp = new LetterComparator(map);
				useDefaultComparator = true;
			}

			//We sort a hashmap and construct a queue
	
			Map<Character, Integer> sorted_map = 
				new TreeMap<Character, Integer>(cmp);
			
			sorted_map.putAll(map);

			for (Character key : sorted_map.keySet())
			{
				queue.add(key);
			}
			
			HangmanUtil.verbose("sorted letter map is " 
				+ sorted_map.toString().toUpperCase());

			HangmanUtil.verbose("sorted letter queue is " 
				+ queue.toString().toUpperCase());

			if(true == useDefaultComparator) cmp = null;

			sorted_map.clear();
			sorted_map = null;
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * Remove possible words that do have the incorrect letter.
	 */
	private static int filterWordsWrongGuess(Character c, 
			Set<String> words, boolean verboseDisplayOff)
	{
		int count = 0;

		try
		{
			if(c == null || words == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			Set<String> removeWords = new HashSet<String>();

			for(String word: words) 
			{
				//If the word has the non-occuring character,
				//remove it from the wordset
				if(word.indexOf(c) != -1)
				{
					removeWords.add(word);
					count++;
				}
			}

			for(String rword: removeWords)
			{
				words.remove(rword);
			}

			removeWords.clear();

			if(false == verboseDisplayOff)
			{
				HangmanUtil.verbose("removing " + count 
					+ " words (wrong guess)");
				
				HangmanUtil.verbose("possible words set size is now " 
					+ words.size());

				HangmanUtil.verbose2("possible words set is " 
					+ words.toString().toUpperCase());
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return count;
	}

	/**
	 * @return number of words filtered from the words collection
	 * after having correctly guessed letter c.
	 * Updates words collection with new filtered set
	 */
	private static int filterWordsCorrectGuess(Character letter, 
					Set<String> words, String hangman, 
					int validLength, boolean simulate)
	{
	
		int wordsFiltered = 0;

		try
		{
			if(letter == null || words == null 
				|| hangman == null || validLength == 0)
			{
				throw new IllegalArgumentException("input parameter(s) " + 
					"can't be null or unspecified");
			}

			Map<Character, Integer> map = new HashMap<Character, Integer>();

			HangmanStrategy.createHangmanLetterMap(hangman, map, validLength);

			wordsFiltered 
				= HangmanStrategy.filterWordsCorrectGuessHelper(letter, 
								words, hangman, map, simulate);

	//		#Deprecated, runs slower
	//		wordsFiltered 
	//			= HangmanStrategy.filterWordsCorrectGuessRemove(letter, 
	//							words, hangman, map, simulate);
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return wordsFiltered;
	
	}

	

	/**
	 * (Deprecated)
	 * Remove possible words that either don't have the correct letter,
	 * correct hangman letter counts, or hangman letter positions.  
	 * Can also be used in simulation mode without making any 
	 * possible word set changes.
	 */
	private static int filterWordsCorrectGuessRemove(Character c, 
						Set<String> words, String hangman, 
						Map<Character, Integer> map, boolean simulate) 
	{
		int count = 0;

		try
		{
			if(c == null || words == null || hangman == null || map == null) 
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			Set<String> removeWords = new HashSet<String>();

			for(String word: words)
			{
				//Check if word is a possible valid word, if not remove it
				if(false == HangmanStrategy.isValidPossibleWord(c, word, map, hangman))
				{
					if(simulate == false) removeWords.add(word);
					count++;
					continue;
				}
			}

			if(simulate == false) 
			{
				for(String rword: removeWords)
				{
					words.remove(rword);
				}
			}

			removeWords.clear();
			removeWords = null;

			HangmanUtil.verbose("[Deprecated] removing " 
				+ count + " words (correct guess)");
			HangmanUtil.verbose("[Deprecated] possible words set size is " 
				+ words.size());
			HangmanUtil.verbose2("[Deprecated] possible words set is " 
				+ words.toString().toUpperCase());
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return count;
	}


	/**
	 * Removes possible words that either don't have the 
	 * correct letter, correct hangman letter positions, or hangman 
	 * letter counts.  Can also be used in simulation mode without 
	 * making any possible word set changes
	 */
	private static int filterWordsCorrectGuessHelper(Character c, 
							Set<String> words, String hangman, 
							Map<Character, Integer> map, boolean simulate) 
	{
		int count = 0;

		try
		{
			if(c == null || words == null || hangman == null || map == null) 
			{
				throw new IllegalArgumentException("[Deprecated] " 
					+ "input parameter(s) can't be null");
			}

			Set<String> removeWords = new HashSet<String>();

			for(String word: words)
			{
				//NOTE: Better Performance achieved when these three check
				//are run independently vs rolling them all into one, 
				//with one pass of the word.  As most of the words hit
				// the first check

				//If the word doesn't have the correctly guessed character
				//remove it from the wordset
				if(word.indexOf(c) == -1)
				{
					if(simulate == false) removeWords.add(word);
					count++;
					continue;
				}

				//If the hangman has only 2 a's showing 
				//and the word has 3 a's then remove word
				if(false == 
					HangmanStrategy.hangmanLetterCountsMatch(word, map))
				{
					if(simulate == false) removeWords.add(word);
					count++;
					continue;
				}

				//Ensure the exact position of the 
				//e.g. 2 a's match the hangman positions
				if(false == 
					HangmanStrategy.hangmanLetterPositionsMatch(word, hangman))
				{
					if(simulate == false) removeWords.add(word);
					count++;
					continue;
				}
			}

			if(simulate == false) 
			{
				for(String rword: removeWords)
				{
					words.remove(rword);
				}
			}

			removeWords.clear();
			removeWords = null;

			HangmanUtil.verbose("removing " + count 
				+ " words (correct guess)");
			HangmanUtil.verbose("possible words set size is " 
				+ words.size());
			HangmanUtil.verbose2("possible words set is " 
				+ words.toString().toUpperCase());
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return count;
	}

	
	/**
	 * @return the score after having guessed the letters in the list given 
	 * it was a correct choice in a simulated game. 
	 * The simulation does not change the game state.
	 */
	private static Character simulatedLetterStrength(List<Character> list, 
			Comparator<Character> simComparator) 
	{
		Character strongLetter = null;

		//Check the strength of the letters as determined by their
		//passed in comparator
		try
		{	
			if(list == null || simComparator == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			HangmanUtil.debug("Simulating letter guess " 
				+ "(not actually guessing right now)");

			Set<Character> sortedSet = 
				new TreeSet<Character>(simComparator);
			
			sortedSet.addAll(list);

			HangmanUtil.debug("Sorted letter strengths map is: " 
				+ sortedSet);

			Queue<Character> sortedQueue = new ArrayDeque<Character>();

			for (Character key : sortedSet)
			{
				sortedQueue.add(key);
			}

			HangmanUtil.debug("End simulating letter guess");

			HangmanUtil.debug("Strong simulated letter guess winner is: " 
				+ sortedQueue.peek());

			strongLetter = sortedQueue.remove();

			sortedSet.clear();
			sortedQueue.clear();

			sortedSet = null;
			sortedQueue = null;
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return strongLetter;
	}


	/*
	 * If this set follows traditional english letter frequency, score it 
	 * low if it doesn't, score it higher.  Reward abnormal, or outlier 
	 * sets to see if we can find any unique letter patterned sets.  
	 * Compute standard deviation to common occuring english letter 
	 * distributions found in the english language.

	 * NOTE: This could be a useful attribute when training a 
	 * 		 classifier algorithm e.g. naive bayes
	 * NOTE: Observation - the smaller the set the higher the standard
	 *		 deviation because there are less characters 
	 */
	private static double outlierScore(Set<String> set)
	{
		double standardDeviation = 0;

		try
		{	
			if(set == null)
			{
				throw new IllegalArgumentException("input parameter(s) " 
					+ "can't be null");
			}

			Map<Character, Integer> map = new HashMap<Character, Integer>();

			HangmanStrategy.tallyLetterWordFrequency(set, null, map, null);
			
			int size = set.size();
			
			int count = 0;
			double sumSqDiff = 0.0;

			/*
				Compute the standard deviation of letter frequency 
				across this word set as compared to the reference values
				of the relative letter frequency in english language

				http://www.mathsisfun.com/data/standard-deviation.html

				The Standard Deviation is a measure of how spread out 
				numbers are. It is the square root of the variance. 
				The variance is the average of the squared differences 
				from the mean
			*/

			for(Character key: map.keySet())
			{
				double letterFreqDist = 
					(double) map.get(key) / (double) size;

				double standard = 
					HangmanStrategy.relativeLetterFreqEnglishLang[ ((char) key - 'a') ];

				double diff = Math.abs(standard - letterFreqDist);

				double sqDiff = Math.pow(diff, 2);

				sumSqDiff += sqDiff;

				count++;
			}

			double variance = sumSqDiff / (double) count;

			standardDeviation = Math.sqrt((double) variance);
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return standardDeviation;
	}



	/** Instance methods **/

	/**
	 * Public constructor for HangmanStrategy
	 * Initializes recordkeeping strategy structures
	 * @params HangmanGame instance
	 */
	public HangmanStrategy(HangmanGame game) throws Exception
	{
		try
		{
			if(game == null)
			{
				throw new IllegalArgumentException("input parameter" 
					+ " can't be null");
			}

			this.answerLength = game.getSecretWordLength();
			this.numWrongGuessesRemaining = game.numWrongGuessesRemaining();
			this.hangman = game.getGuessedSoFar();

			initialize();
			
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * Explicit destructor for HangmanStrategy instance
	 */
	public void end()
	{
		this.letterCountsMap.clear();
		this.letterCountsMap = null;

		this.wordSet.clear();
		this.wordSet = null;

		this.alreadyGuessedLetters.clear();
		this.alreadyGuessedLetters = null;

		this.freqLetterQueue.clear();
		this.freqLetterQueue = null;

		this.lastLetterGuessed = null;
		this.lastWordGuessed = null;

		this.hangman = null;
		
		this.answerLength = -1;
		this.numWrongGuessesRemaining = -1;

		this.lastGuessIsLetter = false;
		this.lastGuessIsWord = false;
	}

	/**
	 * If this answer length's word set or letters count info is not 
	 * already stored, generate it and store it for this instance 
	 * and for subsequent games
	 */
	private void initialize()
	{
		Scanner input = null;

		try
		{
			Set<String> words = this.sizeWordMap.get(this.answerLength);

			Map<Character, Integer> letterCounts = 
				this.letterCountsMapSets.get(this.answerLength);

			//check if we already have the wordset pre-stored and the 
			//letter frequency pre-stored if so then just copy to
			// instance variables
			if(words != null && letterCounts != null)
			{
				for(String s: words)
				{
					this.wordSet.add(s);
				}

				for(Character key : letterCounts.keySet())
				{
					this.letterCountsMap.put(key, letterCounts.get(key));
				}
	  
				HangmanStrategy.sortLetterMap(this.letterCountsMap, 
					this.freqLetterQueue, null);
			}
			else //if we don't have the wordset pre-stored read in the dictionary words
			{
				input = HangmanUtil.getDictionary();

				if(input != null)
				{
					//Load words into hashset
					while(input.hasNext())
					{
						String word = input.next();

						//We are only interested in words of answer length size
						if(word.length() == this.answerLength)
						{
							this.wordSet.add(word);
						}
					}
					
					input.close();
					input = null;

					//Store our new wordSet into the HashMap of HashSets  
					//for future retrieval to avoid initializing again
					if(sizeWordMap.get(this.answerLength) == null)
	  				{
	   					Set<String> set = new HashSet<String>();

	   					for(String s: this.wordSet)
	   					{
		 					set.add(s);
						}

					   sizeWordMap.put(this.answerLength, set);
	  				}

					tallyLetterFrequency();

					//Extract the letters and counts and then put it into 
					//the HashMap of HashMap to avoid computing this again
					if(null == this.letterCountsMapSets.get(this.answerLength))
					{
						Map<Character, Integer> lCounts = 
							new HashMap<Character, Integer>();

						for(Character key : this.letterCountsMap.keySet())
						{
							lCounts.put(key, this.letterCountsMap.get(key));
						}

						this.letterCountsMapSets.put(this.answerLength, lCounts);
					}
				}
				else
				{
					throw new IllegalStateException("Expecting a non-null scanner object");
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
		finally
		{
			if(null != input) input.close();
		}
	}

	/**
	 * Tally the letter frequency given the various recordkeeping structures 
	 */
	private void tallyLetterFrequency()
	{
		try
		{
			this.letterCountsMap.clear();
			this.freqLetterQueue.clear();

			HangmanStrategy.tallyLetterWordFrequency(this.wordSet, 
				this.alreadyGuessedLetters, this.letterCountsMap, 
				this.freqLetterQueue);

		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}


	/**
	 * @param HangmanGame instance
	 * @return Guess, the strategized guess
	 * Given a hangman game instance, determine the next 
	 * guessing strategy.  Record information related to the last guess 
	 * and use that to prepare the strategy for the next guess.
	 */
	public Guess nextGuess(HangmanGame game)
	{
		Guess guess = null;

		try
		{
			if(game == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			//Before we produce next guess, determine success of
			//last guess and update accordingly
			processLastGuess(game);

			if(HangmanUtil.isValidLevel(HangmanUtil.DisplayLevel.VERBOSE))
			{
				Set<Character> guesses = game.getAllGuessedLetters();
				HangmanUtil.verbose("all guessed letters so far are " 
					+ guesses);
			}

			Character guessCharacter = getNextLetter();

			HangmanUtil.debug("guess character is: " + guessCharacter);

			//only 1 possible word left, we are done!
			if(1 == this.wordSet.size())
			{
				Iterator<String> iter = this.wordSet.iterator();
				String word = iter.next();

				guess = new GuessWord(word.toUpperCase());
				
				this.lastWordGuessed = word;
				this.lastGuessIsWord = true;
				this.lastGuessIsLetter = false;
			}
			else if(null != guessCharacter)
			{
				this.alreadyGuessedLetters.add(guessCharacter);

				//Letter c should be lowercase for processing and
				//matching purposes but uppercase for display 
				//as easier to read

				guess = 
					new GuessLetter(Character.toUpperCase(guessCharacter));

				this.lastLetterGuessed = guessCharacter;
				this.lastGuessIsLetter = true;
				this.lastGuessIsWord = false;
			}

			HangmanUtil.verbose();
			HangmanUtil.verbose();
			HangmanUtil.lessterse(guess.toString());
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
		}
		
		return guess;
	}

	/**
	 * Top level get next letter function, asks for the next letter to guess
	 */
	private Character getNextLetter()
	{
		Character letter = null;
		
		try
		{
			letter = getNextLetter(this.freqLetterQueue, this.wordSet, 
						this.letterCountsMap, this.alreadyGuessedLetters);
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return letter;
	}

	/**
	 * Using the frequent letter queues, the set of possible words, 
	 * the character counts determine the next letter to guess.
	 *
	 * EXTRA: varies the next letter to guess by looking at interesting 
	 * things like standard deviation of a word set after having guessed
	 * a letter, choosing the most frequent letter and also looking at which
	 * letter occurs most frequently in the stumper words
	 */
	private Character getNextLetter(Queue<Character> letterQ, 
					Set<String> words, Map<Character, Integer> lookup, 
					Set<Character> exclusion)
	{
		Character letter = null;
		
		try
		{
			if(letterQ == null || words == null || lookup == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			if(letterQ.isEmpty())
			{ 
				String bewildered = "Strange, no more letters in " 
				 + "frequent letter queue, this shouldn't be happening!\n" 
				 + "Is the dictionary and word to guess both in english?";

				throw new Exception(bewildered);
			}
			
			if(words.size() > 1)
			{
				while(false == letterQ.isEmpty() && 
					null != (letter = letterQ.peek()) )
				{
					int letterCounts = lookup.get(letter);
					int possibleWords = words.size();

					HangmanUtil.debug("letter is: " + letter 
						+ ", counts is: " + letterCounts 
						+ ", set size is: " + possibleWords);

					/*
					  For this range of possible words we are simply 
					  guessing the most frequent letter
					*/

					if(possibleWords > HangmanStrategy.MEDIUM_WORD_SET_SIZE) 
					{
						break;  
					}

					/*
					  Trying to optimize letter guessing strategy between 
					  this possible word set range by choosing a letter 
					  that is also "strong" or "interesting".  Strong meaning
					  it will eliminate the most possible words.
					  Interesting meaning unique set outlier score
					*/
					else if(possibleWords <= HangmanStrategy.MEDIUM_WORD_SET_SIZE && 
						possibleWords > HangmanStrategy.MICRO_WORD_SET_SIZE)
					{
						Character alternate = null;

						List<Character> letters 
							= new ArrayList<Character>(letterQ);

						List<Character> subList = null;

						//Only look at the TOP_N_THRESHOLD 
						//characters, if list is less than that
						//than use that
						if(letters.size() >= HangmanStrategy.TOP_N_THRESHOLD)
						{
							subList = letters.subList(0, HangmanStrategy.TOP_N_THRESHOLD);
						}
						else
						{
							subList = letters;
						}

						alternate = 
							getNextLetterFromStumperSet(words, exclusion);

						//If the stumper most freqent letter is within
						//the our most frequent letter sublist, run with it
						//If not, use another strategy through
						//the comparator interface below
						if(alternate != null && subList.contains(alternate)) 
						{
							letter = new Character(alternate);
							break;
						}


						HangmanUtil.debug("sublist is: " + subList);

						/*
							#Not fully baked yet

							Comparator<Character> simComparator = 
							new SimulatedMostWordsFilteredComparator(words, 
						 			this.hangman, this.answerLength);
						*/

						Comparator<Character> simComparator = 
							new SimulatedOutlierSetComparator(words);

						alternate = HangmanStrategy.simulatedLetterStrength(subList, 
															simComparator);

						HangmanUtil.debug("Picking alternate letter: " 
							+ alternate);
								
						letter = new Character(alternate);

						subList.clear();
						letters.clear();
						
						subList = null;
						letters = null;
						

						break;
					}
					
					/*
					  If we are down to a few possible words and most of 
					  the letters are in all of the words, then we want
					  to gain some new information. Pick a letter whose 
					  frequency is half or less than the size of the set
					  size. For a set size of 10 this is a letter count 
					  frequency of 5 or less versus choosing letters with 
					  letter count frequencies 10,9, etc.. 
					*/

					else if(letterCounts <= possibleWords / 2)
					{
						break; 
					}

					//at the end of the while loop, remove the "peeked" 
					//at letter, to advance to the next letter
					letterQ.remove();
				}

				/* 
				  There are no more characters that have counts less 
				  than or equal to half the set size, so just choose the 
				  last character where its letter frequency 
				  count is > possible words set size / 2
				*/

				if(null == letter && letterQ.size() > 0)
				{
					letter = letterQ.remove();
				}
				else 
				{
					if(null == letter)
					{
						String msg = "\nThere are no more next letters, " + 
									 "this shouldn't be happening";

						throw new Exception(msg);
					}
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
			e.printStackTrace(System.err);
		}

		return letter;
	}

	/**
	 * Get the next letter from the stumper words set that is a subset of 
	 * the possible words. The strategy is to shave off the stumpers by 
	 * guessing their frequent letters without having to guess a stumper 
	 * word itself
	 */
	private Character getNextLetterFromStumperSet(Set<String> words, 
												Set<Character> exclusion)
	{
		Character letter = null;
		
		try
		{
			if(words == null)
			{
				throw new IllegalArgumentException("input parameter(s) " 
					+ "can't be null");
			}

			int possibleWords = words.size();

			if(true == HangmanUtil.useStumpers()) 
			{
				Set<String> localStumpers = new HashSet<String>();

				Map<Character, Integer> map = 
					new HashMap<Character, Integer>();
				
				Queue<Character> sortedQ = new ArrayDeque<Character>();
				
				//First check if there is a stumper word in our wordSet
				for(String possibleStumper: words)
				{
					if(true == HangmanUtil.isStumper(possibleStumper))
					{
						localStumpers.add(possibleStumper);
					}
				}
				
				//If any stumpers are left
				if(localStumpers.size() > 0)
				{
					HangmanUtil.verbose2("Stumper word set: " + 
						localStumpers.toString().toUpperCase());

					HangmanStrategy.tallyLetterWordFrequency(localStumpers, 
						exclusion, map, sortedQ);

					//return most frequent letter
					letter = sortedQ.remove();
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
			e.printStackTrace(System.err);
		}

		return letter;
	}

	/**
	 * Capture whether the last guess was correct or wrong 
	 * and update the strategy state accordingly
	 */
	private void processLastGuess(HangmanGame game)
	{
		try
		{
			if(game == null)
			{
				throw new IllegalArgumentException("input parameter(s) " 
					+ "can't be null");
			}

			this.hangman = game.getGuessedSoFar();

			//We've had one guess already
			if(game.currentScore() > 0)
			{
				//If the last guess was correct, update accordingly
				if(this.numWrongGuessesRemaining == 
							game.numWrongGuessesRemaining())
				{
					//We got a correct guess
					HangmanUtil.verbose("<Correct guess>");
					madeCorrectGuess();
					
				}
				else //The last guess was incorrect, so update accordingly
				{
					this.numWrongGuessesRemaining =
						 game.numWrongGuessesRemaining();

					HangmanUtil.verbose("<Wrong guess>");
					madeWrongGuess();
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * Update set of possible words and letter frequency state related 
	 * to making a correct guess
	 */
	private void madeCorrectGuess()
	{
		try
		{
			//We only check if the last guess is a letter (vs a word), 
			//otherwise we would have won and would not be here
			if(true == this.lastGuessIsLetter)
			{
				HangmanStrategy.filterWordsCorrectGuess(this.lastLetterGuessed,
					this.wordSet, this.hangman, this.answerLength, false);

				//Perform the appropriate record keeping
				tallyLetterFrequency();
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}


	/**
	 * Update set of possible words and letter frequency state related 
	 * to making a wrong guess
	 */
	private void madeWrongGuess()
	{
		try
		{
			//If the last incorrect guess was a letter
			if(true == this.lastGuessIsLetter 
					&& false == this.lastGuessIsWord)
			{
				HangmanStrategy.filterWordsWrongGuess(this.lastLetterGuessed, 
													this.wordSet, false);
			}
			
			//If the last incorrect guess was a word
			if(true == this.lastGuessIsWord 
					&& false == this.lastGuessIsLetter)
			{
				//simply remove it from the word set
				this.wordSet.remove(this.lastWordGuessed);	

				HangmanUtil.verbose("removing 1 word (wrong guess)");
				HangmanUtil.verbose("possible words set size is now " + 
					this.wordSet.size());
				HangmanUtil.verbose2("possible words set is " + 
					this.wordSet.toString().toUpperCase());	
			}

			//Perform the appropriate record keeping
			tallyLetterFrequency();
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}		
	}	
}