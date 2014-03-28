import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

/*** 
 Utility class for PlayHangman and HangmanStrategy classes 
 Implemented exclusively through static class methods.

 Used to group static methods and state dealing with text display output, 
 timing, dictionary, hangman word collection, stumper state, 
 and batch processing state
***/

public class HangmanUtil
{
	/**
	 * An enum to handle display output state
	 * Options are specified during program startup
	 */
	public enum DisplayLevel
	{ 
		//Text print display flags
		NONE(0), TERSE(1), LESS_TERSE(2), VERBOSE(3), 
		MORE_VERBOSE(4), DEBUG(5);

    	private final int dlevel;

	    DisplayLevel(int level) 
	    {
	        this.dlevel = level;
	    }

	    //Method for determining if we can print out this display level
	    //For example: if the display level is verbose, we will print 
	    //out terse and less terse statements

	    //@return whether passed in display level is valid to display at 
	    public boolean isValidLevel(DisplayLevel tmp)
	    {
	        return (this.dlevel >= tmp.dlevel);
	    }
	}

	/**
	 * An enum to handle clock level display state
	 * Options are specified during program startup
	 */
	public enum ClockLevel
	{
		//Clock is for higher level timing
		//(e.g. start and end over the course of a program)
		//Clock2 is to be used at a more granular level

		NO_CLOCK(0), CLOCK(1), CLOCK2(2);

		private final int clevel;

		ClockLevel(int level) 
	    {
	        this.clevel = level;
	    }

	    /**
	     * @return boolean flag if passed in clock level is valid 
	     * @params ClockLevel with which to compare
	     */
	    public boolean isValidLevel(ClockLevel tmp)
	    {
	        return (this.clevel == tmp.clevel);
	    }
	}

	/* Static fields managing timing state, dictionary, hangman 
		word collection, stumper state, and batch processing state */

	//set default output to TERSE 
	private static DisplayLevel textDisplay = DisplayLevel.TERSE;

	//set default timing to NO_CLOCK
	private static ClockLevel clockDisplay = ClockLevel.NO_CLOCK;

	//fields to manage timing state
	//Note: Either clock is used or clock2 is used
	//		both can't be used at the same time
	//		so we have one set of fields
	private static int clockCounter = 1;
	private static long lastClockTime = -1;
	private static long startClockTime = -1;

	//data structure to hold dictionary file info
	private static File dictionary;

	//data structure to store hangman word(s)
	private static List<String> hangmanWords = new ArrayList<String>();

	//state (flags, data structures, ids) to handle stumper program logic
	private static final String STUMPERS_FILE_NAME = "stumpers.txt";
	private static boolean initStumpersMode = false;
	private static File stumpers;
	private static boolean useStumpers = false;

	private static Set<String> stumperWords = new HashSet<String>();

	//state (flags, data structures, ids) to handle batch file
	//program logic
	private static File batch;
	private static boolean useBatchWords = false;
	private static int batchWordsCount = 0;
	private static int batchScoreCount = 0;


	/**
	 * Determines if any hangman words left to process
	 * @return boolean if any words left
	 */
	public static boolean anyHangmanWords()
	{
		if(HangmanUtil.hangmanWords.size() > 0)
		{
			return true;
		}
	
		return false;
	}

	/**
	 * @return Returns the number of hangman words to play
	 */
	public static int getNumHangmans()
	{
		if(HangmanUtil.anyHangmanWords()) 
		{
			return HangmanUtil.hangmanWords.size();
		}

		return -1; 
	}

	/**
	 * Add a new hangman word to the collection
	 * @params String hangman word
	 */
	public static void addHangmanWord(String word)
	{
		if(word != null) HangmanUtil.hangmanWords.add(word);
	}

	/**
	 * Reset collection of hangman words
	 */
	public static void clearHangmanWords()
	{
		HangmanUtil.hangmanWords.clear();
	}

	/**
	 * Reset collection of stumper words
	 */
	public static void clearStumperWords()
	{
		HangmanUtil.stumperWords.clear();
	}

	/**
	 * Set display print text flag to NONE
	 */
	public static void clearPrintFlags()
	{
		HangmanUtil.textDisplay = DisplayLevel.NONE;
	}

	/**
	 * @return Indicate whether passed in display level is valid
	 * @param DisplayLevel to compare against
	 */
	public static boolean isValidLevel(DisplayLevel other)
	{
		return HangmanUtil.textDisplay.isValidLevel(other);
	}

	/**
	 * @return Indicate whether passed in clock level is valid
	 * @param ClockLevel to compare against
	 */
	public static boolean isValidLevel(ClockLevel other)
	{
		return HangmanUtil.clockDisplay.isValidLevel(other);
	}

	/**
	 * Mark that the mode is to initialize stumper words
	 */
	public static void setInitStumpers()
	{
		HangmanUtil.initStumpersMode = true;
	}

	/**
	 * @return The boolean flag if we are in initialize stumper mode
	 */
	public static boolean isInitStumpers()
	{
		return HangmanUtil.initStumpersMode;
	}

	/**
	 * @return If the word passed in is a stumper return true 
	 * @param String word to check for possible stumper quality
	 */
	public static boolean isStumper(String word)
	{
		boolean valid = false;

		if(null != word &&
			false == HangmanUtil.stumperWords.isEmpty() &&
			true == HangmanUtil.useStumpers)
		{
			valid = HangmanUtil.stumperWords.contains(word);
		}

		return valid;
	}

	/**
	 * @return true if we are indeed using stumpers 
	 */
	public static boolean useStumpers()
	{
		return HangmanUtil.useStumpers;
	}

	/**
	 * Exit game with an abort message
	 * called when there is unexpected or incorrect program behavior
	 */
	static void abort()
	{
		System.err.println("");
		System.err.println("[Aborting Hangman]");
		System.err.println("");
		System.exit(1);
	}

	/**
	 * Invoke scanner object from dictionary file object
	 * @return Scanner object of dictionary 
	 */
	public static Scanner getDictionary() throws IOException
	{
		return new Scanner(HangmanUtil.dictionary);
	}

	/**
	 * Initialize stumpers by playing hangman for all the words
	 * in the dictionary list and then store stumper words 
	 * into file STUMPERS_FILE_NAME
	 * @throws IllegalStateException, IOException
	 */
	public static void initStumpers() 
		throws IllegalStateException, IOException
	{
		Scanner input = null;

		try
		{
			HangmanUtil.textDisplay = DisplayLevel.NONE;
					
			input = HangmanUtil.getDictionary();

			File file = new File(STUMPERS_FILE_NAME);
				
			// creates file
			file.createNewFile();

			BufferedWriter bwriter = 
				new BufferedWriter(new FileWriter(file)); 

			int count = 0;
			
			while(input.hasNext())
			{
				String word = input.next();
				
				HangmanUtil.addHangmanWord(word);

				if(HangmanGame.Status.GAME_LOST == HangmanUtil.singleRun())
				{
					bwriter.write(word);
					bwriter.newLine();
				}

				HangmanUtil.clearHangmanWords();

				count++;

				if(count % 5000 == 0) bwriter.flush();
			}

			input.close();
			input = null;

			bwriter.flush();
			bwriter.close();
			bwriter = null;
		}
		catch(IllegalStateException ise)
		{
			throw ise;
		}
		catch(IOException ioe)
		{
			throw ioe;
		}
		catch(Exception e)
		{
			System.err.println(e);
		}
		finally
		{
			if(input != null) input.close();
		}
	}

	/**
	 * Load stumper words from STUMPERS_FILE_NAME file and store
	 * into collection for easy lookup 
	 */
	private static void loadStumpers() 
		throws IOException
	{
		Scanner input = null;
		
		try
		{	
			if(true == HangmanUtil.useStumpers && 
				true == HangmanUtil.stumperWords.isEmpty())
			{

				HangmanUtil.stumpers = new File(STUMPERS_FILE_NAME);

				if(null == HangmanUtil.stumpers ||
					false == HangmanUtil.stumpers.exists())
				{
					throw new IOException("[Unable to load stumpers file\n" 
						+ "Please re-generate file for optimal results " +
						"(see usage with -h)]");
				}
				else
				{
					input = new Scanner(HangmanUtil.stumpers);

					if(null != input)
					{
						//Load words into hashset
						while(input.hasNext())
						{
							//minimize local variable scope
							String word = input.next();

							HangmanUtil.stumperWords.add(word);
						}

						input.close();
						input = null;
					}

					HangmanUtil.verbose2("Finished loading stumper words");
				}
			}
		}
		catch(IOException e)
		{
			throw e;
		}
		finally
		{
			if(null != input) input.close();
		}
	}

	/**
	 * @return The boolean flag indicating if we are in 
	 * batch words processing mode
	 */
	public static boolean useBatchWords()
	{
		return HangmanUtil.useBatchWords;
	}

	/**
	 * Process the batch words listed in the batch file specified
	 * at startup.  Iterate through words while running hangman on 
	 * each of the words.
	 */
	public static void processBatchWords()
	{
		Scanner input = null;

		try
		{
			HangmanUtil.clockDisplay = HangmanUtil.ClockLevel.CLOCK;

			if(true == HangmanUtil.useBatchWords)
			{
				if(null == HangmanUtil.batch)
				{
					throw new IOException("[Unable to load batch file.");
				}
				else
				{
					input = new Scanner(HangmanUtil.batch);

					if(null != input)
					{
						HangmanUtil.clock("Starting Batch hangman games");

						//Load words into hashset
						while(input.hasNext())
						{
							String word = input.next();

							HangmanUtil.addHangmanWord(word);

							HangmanUtil.singleRun();

							HangmanUtil.clearHangmanWords();

							HangmanUtil.batchWordsCount++;
						}

						double timeElapsed = 
							HangmanUtil.clock("End batch games");

						input.close();
						input = null;
					
						int numGames = HangmanUtil.batchWordsCount;
						
						double avg = ((double) HangmanUtil.batchScoreCount)
										 / (double) numGames;
			
						double timePerGame = 
							timeElapsed / (double) numGames;

						System.out.println("Given " + numGames 
							+ " words, average word score is: " + avg);

						HangmanUtil.clock(false, "For " + numGames
							+ " games, total time was " + timeElapsed
							+ " ms, average time per game including " 
							+ "initializations was " + timePerGame + " ms");
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
		}
		finally
		{
			if(null != input) input.close();
		}
	}

	/**
	 * Run a single game of hangman.  If stumpers mode is on 
	 * use stumpers in guessing strategy.
	 * @return The game status of the single game
 	 */
	public static HangmanGame.Status singleRun()
	{
		HangmanGame.Status status = HangmanGame.Status.GAME_LOST;

		try
		{
			HangmanUtil.useStumpers = true;

			if(!HangmanUtil.isInitStumpers()) HangmanUtil.loadStumpers();

			String word = HangmanUtil.hangmanWords.get(0);

			status = singleRun(word);
		}
		catch(Exception e)
		{
			System.err.println(e);
		}

		return status;
	}

	/**
	 * Run a single game of hangman.  
	 * @return The game status of the single game
 	 */
	private static HangmanGame.Status singleRun(String secret)
	{
		HangmanGame.Status status = HangmanGame.Status.GAME_LOST;

		try
		{
			if(secret == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}

			PlayHangman play = PlayHangman.newInstance();

			play.init(secret);
			
			int score = play.run();
			
			status = play.status();
			
			play.end();

			play = null;

			if(true == HangmanUtil.isInitStumpers() || 
				true == HangmanUtil.useBatchWords())
			{
				HangmanUtil.bulk(secret.toUpperCase() + ": " + score);
				
				HangmanUtil.batchScoreCount += score;
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return status;		
	} 

	/**
	 * Run multiple games of hangman as specified by a word 
	 * list given from the command line.  
	 * If the clock option is specified, time the game plays
	 * NOTE: for larger sizes of hangman games, e.g. > a thousand,
	 * 		 use the batch option and a file of hangman words
 	 */
	public static void multipleHangmans()
	{
		try
		{
			List<PlayHangman> plays = new ArrayList<PlayHangman>();
			List<Integer> scores = new ArrayList<Integer>();

			HangmanUtil.clock("Multiple hangman games");
			HangmanUtil.clock("Start multiple inits");

			HangmanUtil.multipleInits(plays);
			
			HangmanUtil.clock("End multiple inits");
			HangmanUtil.clock("Start multiple runs");
			
			HangmanUtil.multipleRuns(plays, scores);
			plays.clear();
			
			HangmanUtil.clock("End multiple runs");
			
			HangmanUtil.multipleScores(HangmanUtil.hangmanWords, scores);
			scores.clear();
			
			double timeElapsed = HangmanUtil.clock("End multiple scoring");

			int numGames = HangmanUtil.getNumHangmans();
			double timePerGame = (double) timeElapsed / (double) numGames;

			HangmanUtil.clock(false, "For " + numGames 
				+ " games, total time was " + timeElapsed 
				+ " ms, average time per game including " 
				+ "initializations was " + timePerGame + " ms");
			
		}
		catch(IllegalStateException ise)
		{
			ise.printStackTrace(System.err);
		}
		catch(Exception e)
		{
			System.err.println(e);
		}
	}

	/**
	 * Run multiple initializations for multiple games.
 	 */
	private static void multipleInits(List<PlayHangman> plays) 
		throws IllegalStateException, IOException, IllegalArgumentException
	{
		if(plays == null)
		{
			throw new IllegalArgumentException("input parameter(s) " 
				+ "can't be null");
		}

		HangmanUtil.multipleInits(HangmanUtil.hangmanWords, plays);

		HangmanUtil.useStumpers = true;

		HangmanUtil.loadStumpers();
	}

	/**
	 * Run multiple initializations for multiple games given two parameters.
 	 */
	private static void multipleInits(List<String> words, 
									List<PlayHangman> plays) 
		throws IllegalStateException, IllegalArgumentException
	{
		try
		{
			if(words == null && plays == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}
			else
			{
				for(String secret: words)
				{
					PlayHangman play = PlayHangman.newInstance();
					play.init(secret);
					plays.add(play);
				}
			}
		}
		catch(IllegalStateException ise)
		{
			throw ise;
		}
	}

	/**
	 * Run multiple games from collection of play hangmans.  
	 * Records games scores in scores collection param
 	 */
	private static void multipleRuns(List<PlayHangman> plays, 
								List<Integer> scores) throws Exception
	{
		try
		{
			if(plays == null && scores == null)
			{
				throw new IllegalArgumentException("input parameter(s) " 
					+ "can't be null");
			}
			else
			{
				for(PlayHangman play : plays)
				{
					int score = play.run();

					scores.add(score);
					
					play.end();

					play = null;
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
			throw e;
		}
	}


	/**
	 * Output scores for multiple games 
	 * @param list of the hangman words
	 * @param pre-created collection of scores
 	 */
	private static void multipleScores(List<String> secrets, 
									   List<Integer> scores) 
	{
		try
		{
			if(secrets == null && scores == null)
			{
				throw new IllegalArgumentException("input parameter(s)" 
					+ " can't be null");
			}
			if(secrets.size() != scores.size())
			{
				throw new IllegalStateException("the two lists are " 
					+ "size mismatched, they are not equal in size");
			}
			else
			{
				Iterator<String> itr = secrets.iterator();

				int i = 0;
				int sum = 0;
				int count = scores.size();

				while(itr.hasNext())
				{
					String secret = (itr.next()).toUpperCase();

					int score = scores.get(i);

					sum += score;

					System.out.println(secret + ": " + score);

					i++;
				}

				double avg = ((double) sum / (double) count);

				System.out.println("Given " + count 
					+ " words, average word score is: " + avg);
				
				itr = null;
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}
	}

	/**
	 * Process command line arguments for PlayHangman program
	 * and set various program modes, clock levels, display text modes,
	 * batch file and so forth.
	 * @param args is a string array of command line options
 	 */
	public static void config(String[] args) 
	{
		try
		{
			if(args == null || args.length < 3 || 
				(args[0]).equals("-h") || (args[0]).equals("-help"))
			{
				PlayHangman.usage();
				HangmanUtil.abort();
			}
			//Check for java PlayHangman -f dictionary.txt -init:stumper
			else if(args.length == 3 && 
				(args[0]).equals("-f") &&
				(args[2]).equals("-init:stumpers"))
			{
				String dictionaryFileName = args[1];
				HangmanUtil.dictionary = new File(dictionaryFileName);

				HangmanUtil.setInitStumpers();
			}
			else if(args.length == 3 && 
				(args[0]).equals("-f") &&
				(args[2]).equals("-baseline"))
			{
				String dictionaryFileName = args[1];
				HangmanUtil.dictionary = new File(dictionaryFileName);

				HangmanUtil.addHangmanWord("comaker");
				HangmanUtil.addHangmanWord("cumulate");
				HangmanUtil.addHangmanWord("eruptive");
				HangmanUtil.addHangmanWord("factual");
				HangmanUtil.addHangmanWord("monadism");
				HangmanUtil.addHangmanWord("mus");
				HangmanUtil.addHangmanWord("nagging");
				HangmanUtil.addHangmanWord("oses");
				HangmanUtil.addHangmanWord("remembered");
				HangmanUtil.addHangmanWord("spodumenes");
				HangmanUtil.addHangmanWord("stereoisomers");
				HangmanUtil.addHangmanWord("toxics");
				HangmanUtil.addHangmanWord("trichromats");
				HangmanUtil.addHangmanWord("triose");
				HangmanUtil.addHangmanWord("uniformed");

				HangmanUtil.clockDisplay = ClockLevel.CLOCK;
			}
			//Check for java PlayHangman -f dictionary.txt -bulkfile
			else if(args.length == 4 && 
				(args[0]).equals("-f") &&
				(args[2]).equals("-batch"))
			{
				String dictionaryFileName = args[1];
				HangmanUtil.dictionary = new File(dictionaryFileName);

				String batchWordsFileName = args[3];
				HangmanUtil.batch = new File(batchWordsFileName);

				HangmanUtil.useBatchWords = true;
			}
			//Check for java PlayHangman -f dictionary.txt arg2 .... argN
			else if(args.length >= 3 && (args[0]).equals("-f"))
			{
				String dictionaryFileName = args[1];
				HangmanUtil.dictionary = new File(dictionaryFileName);
						
				HangmanUtil.processArgs(args, 2);
			}
			else
			{
				PlayHangman.usage();
				HangmanUtil.abort();
			}
		}
		catch(Exception e)
		{
			System.err.println(e);
			HangmanUtil.abort();
		}
	}

	/**
	 * Process command line arguments for PlayHangman program
	 * and set various program modes, clock levels, display text modes,
	 * batch file and so forth.
	 * @param args is a string array of command line options
	 * @param offset is the starting index into string array
 	 */
	private static void processArgs(String[] args, int offset) 
	{
		try
		{
			int i = offset;

			while(i < args.length)
			{
				if((args[i]).indexOf("-") == -1)
				{
					HangmanUtil.addHangmanWord(args[i]);
				}

				if((args[i]).equals("-v"))
				{
					HangmanUtil.textDisplay = DisplayLevel.VERBOSE;
				}

				if((args[i]).equals("-v2"))
				{
					HangmanUtil.textDisplay = DisplayLevel.MORE_VERBOSE;
				}

				if((args[i]).equals("-lt"))
				{
					HangmanUtil.textDisplay = DisplayLevel.LESS_TERSE;
				}

				if((args[i]).equals("-dbg") ||
					(args[i]).equals("-debug"))
				{
					HangmanUtil.textDisplay = DisplayLevel.DEBUG;
				}

				if((args[i]).equals("-clk"))
				{
					HangmanUtil.clockDisplay = ClockLevel.CLOCK;
				}

				if((args[i]).equals("-clk2"))
				{
					HangmanUtil.clockDisplay = ClockLevel.CLOCK2;
				}

				if((args[i]).equals("-clk2"))
				{
					HangmanUtil.clockDisplay = ClockLevel.CLOCK2;
				}

				if((args[i]).equals("-load:stumpers"))
				{
					HangmanUtil.useStumpers = true;
				}

				i++;
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}		
	}

	
	/**
	 * Output method to display terse comments given 
	 * the display output level is TERSE or more expressive
	 * @param String message to print
 	 */
	public static void terse(String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.TERSE)) 
		{
			System.out.println(s);
		}
	}

	/**
	 * Output method to display new line given 
	 * the display output level is TERSE or more expressive
 	 */
	public static void terse()
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.TERSE))
		{
			System.out.println("");
		}
	}
	
	/**
	 * Output method to display less terse comments given 
	 * the display output level is LESS_TERSE or more expressive
	 * @param String message to print
 	 */
	public static void lessterse(String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.LESS_TERSE))
		{
			System.out.println(s);
		}
	}

	/**
	 * Output method to display new line given 
	 * the display output level is VERBOSE or more expressive
 	 */
	public static void verbose()
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.VERBOSE)) 
		{
			System.out.println("");
		}
	}

	/**
	 * Output method to display verbose comments given 
	 * the display output level is VERBOSE or more expressive
	 * @param String message to print
 	 */
	public static void verbose(String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.VERBOSE))
		{
			System.out.println("[H_] " + s);
		}
	}

	/**
	 * Output method to display verbose comments given 
	 * the display output level is VERBOSE or more expressive
	 * @param boolean flag to indicate whether to print the standard header
	 * 					preceding the string message
	 * @param String message to print
 	 */
	public static void verbose(boolean header, String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.VERBOSE)) 
		{
			if(!header)
			{
				System.out.println(s);
			}
			else 
			{
				verbose(s);
			}
		}
	}

	/**
	 * Output method to display more verbose comments given 
	 * the display output level is MORE_VERBOSE or more expressive
	 * @param String message to print
 	 */
	public static void verbose2(String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.MORE_VERBOSE))
		{
			System.out.println("[H_] " + s);
		}
	}

	/**
	 * Output method to display debug comments given 
	 * the display output level is DEBUG or more expressive
	 * @param String message to print
 	 */
	public static void debug(String s)
	{
		if(HangmanUtil.isValidLevel(DisplayLevel.DEBUG)) 
		{
			System.out.println("[DBG] " + s);
		}
	}

	/**
	 * Output method to display bulk processing comments given 
	 * we are initializing stumpers or running a batch file of words
	 * @param String message to print
 	 */
	public static void bulk(String s)
	{
		if(true == HangmanUtil.isInitStumpers() || 
				true == HangmanUtil.useBatchWords()) 
		{
			System.out.println(s);
		}
	}			

	/**
	 * Output method to display new empty line given 
	 * the clock level is CLOCK
	 * @return long value of elapsed time from first time called
 	 */
	public static long clock()
	{
		return clock("");
	}

	/**
	 * Output method to capture timing info in milliseconds
	 * and also display clock comment given the clock level 
	 * is CLOCK. Clock information is output using the following format:
	 * [TIME][Counter][Now][Time Diff from Start][Time Diff from LastCalled]
	 *
	 * Start is the first time clock is called in the prgoram
	 * Time units are in milliseconds
	 * @param boolean flag to indicate whether to print the standard clock
	 * 					information header or just the string message
	 * @param String message to print
	 * @return long value of elapsed time from first time called
 	 */
	public static long clock(boolean normal, String msg) 
	{
		long value = 0;

		try
		{
			if(HangmanUtil.isValidLevel(ClockLevel.CLOCK))
			{
				if(normal == false)
				{
					System.out.println("[CLK] " + msg);
				}
				else
				{
					value = clock(msg);
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return value;
	}

	/**
	 * Output method to capture timing info in milliseconds
	 * and also display clock comment given the clock level 
	 * is CLOCK. Clock information is output using the following format:
	 * [TIME][Counter][Now][Time Diff from Start][Time Diff from LastCalled]
	 *
	 * Start is the first time clock is called in the prgoram
	 * Time units are in milliseconds
	 *
	 * @param String message to print
	 * @return long value of elapsed time from first time called
 	 */
	public static long clock(String msg) 
	{
		long diffFromStart = 0;

		try
		{
			if(HangmanUtil.isValidLevel(ClockLevel.CLOCK))
			{
				long now = System.currentTimeMillis();

				if(HangmanUtil.lastClockTime != -1)
				{
					long diff = now - HangmanUtil.lastClockTime;
					diffFromStart = now - HangmanUtil.startClockTime;

					System.out.println("[CLK][" + HangmanUtil.clockCounter++ 
						+ "][" + now + "][" + diffFromStart + "][" 
						+ diff + "] " + msg);
					HangmanUtil.lastClockTime = now;
				}
				else
				{
					HangmanUtil.startClockTime = now;
					System.out.println("[CLK][" + HangmanUtil.clockCounter++ 
						+ "][" + now + "] " + msg);
					HangmanUtil.lastClockTime = now;
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return diffFromStart;
	}


	/**
	 * Output method to display new empty line given 
	 * the clock level is CLOCK2
	 * @return long value of elapsed time from first time called	 
 	 */
	public static long clock2()
	{
		return clock2("");
	}

	/**
	 * Output method to capture timing info in milliseconds
	 * and also display clock comment given the clock level 
	 * is CLOCK2. Clock information is output using the following format:
	 * [TIME][Counter][Now][Time Diff from Start][Time Diff from LastCalled]
	 *
	 * Start is the first time clock2 is called in the program
	 * NOTE: clock2 should be used at a more granular than clock
	 * Time units are in milliseconds
	 *
	 * @param String message to print
	 * @return long value of elapsed time from first time called	 
 	 */
	public static long clock2(String msg) 
	{
		long diffFromStart = 0;
		
		try
		{
			if(HangmanUtil.isValidLevel(ClockLevel.CLOCK2))
			{
				long now = System.currentTimeMillis();
				if(HangmanUtil.lastClockTime != -1)
				{
					long diff = now - HangmanUtil.lastClockTime;
					diffFromStart = now - HangmanUtil.startClockTime;

					System.out.println("[CLK2][" + HangmanUtil.clockCounter++ 
						+ "][" + now + "][" + diffFromStart + "][" 
						+ diff + "] " + msg);
					HangmanUtil.lastClockTime = now;
				}
				else
				{
					HangmanUtil.startClockTime = now;
					System.out.println("[CLK2][" + HangmanUtil.clockCounter++ 
						+ "][" + now + "] " + msg);
					HangmanUtil.lastClockTime = now;
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("Exception: " + e);
		}

		return diffFromStart;
	}


	// Suppress default constructor for noninstantiability
	private HangmanUtil()
	{

	}
}