
/*** 
 PlayHangman encapsulates both HangmanGame and HangmanStrategy
 Provides the code to represent game playing and 
 allows game and strategy to interact 
***/

public class PlayHangman
{
	//game playing has a constant number of wrong guesses
	private static final int MAX_WRONG_GUESSES = 5;
	
	private static final int ERROR_SCORE = -1;

	//Using a conservative number to limit 
	//the number of PlayHangman objects created
	private static final int MAX_PH_OBJECTS = 1100;

	//Counter to keep track of number of object instances
	private static int instances = 0;
	
	//one game instance for each PlayHangman object
	private HangmanGame game;

	//one strategy instance for each PlayHangman object
	//NOTE: Would have used a generic Strategy type but it doesn't support
	//an explicit destructor method such as end()
	private HangmanStrategy strategy;

	/**
	 * Run various PlayHangman modes 
	 * (initalize stumpers, batch file, single game, multiple games)
	 * Processes various configuration options
	 * @param args is a string array of command line options
	 */
	public static void main(String[] args)
	{
		try
		{			
			HangmanUtil.config(args);

			if(HangmanUtil.isInitStumpers() == true)
			{
				HangmanUtil.initStumpers();
			}
			else if(HangmanUtil.useBatchWords() == true)
			{
				// Turn off print display options
				HangmanUtil.clearPrintFlags();
				HangmanUtil.processBatchWords();
			}

			else if(HangmanUtil.anyHangmanWords() == false)
			{
				System.err.println("No hangman word received!");
				HangmanUtil.abort();
			}
			
			// We have only one hangman word specified
			else if(HangmanUtil.getNumHangmans() == 1)
			{
				HangmanUtil.singleRun();
			}

			// We have multiple hangman words
			// (This is a little slower than the batch file)
			else if(HangmanUtil.getNumHangmans() > 1) 
			{
				// Turn off print display options
				HangmanUtil.clearPrintFlags();
				HangmanUtil.multipleHangmans();
			}

			// Clear state
			HangmanUtil.clearHangmanWords();
			HangmanUtil.clearStumperWords();
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Display PlayHangman command line usage options and scenarios
	 */
	public static void usage()
	{
		
		System.out.println("Usage: Please enter a hangman word and optionally '-v'\n" + 
			"if the verbose mode is desired or specify a list of hangman words");
		System.out.println("");
		System.out.println("<Simple>");
		System.out.println("");
		System.out.println("	java PlayHangman -h");
		System.out.println("	java PlayHangman -f dictionary.txt " + 
			"[-v] $word1");
		System.out.println("	java PlayHangman -f dictionary.txt $word1 " 
			+ "... [$wordN]");
		System.out.println("	java PlayHangman -f dictionary.txt -baseline");
		System.out.println("	java PlayHangman -f dictionary.txt -batch " 
			+ "large.txt");
		/*	args[]								 a0        a1       a2     a3	aN 	*/
		
		System.out.println("");
		System.out.println("<Advanced>");
		System.out.println("");
		System.out.println("	java PlayHangman -f dictionary.txt " + 
			"[[-v2][-v][-lv][-lt]] [-clk2] $word1");
		System.out.println("");
		System.out.println("		(v2 = more verbose output)");
		System.out.println("		(v  = verbose output)");
		System.out.println("		(lv = less verbose output)");
		System.out.println("		(lt = less terse output)");
		System.out.println("		(clk = high level timing output)");

		System.out.println("");
		System.out.println("	java PlayHangman -f dictionary.txt " + 
			"[[-clk][-clk2]] $word1 ... $wordN");
		System.out.println("");
		System.out.println("		(clk2 = lower level timing output)");

		/*	args[]								 a0        a1       a2     a3	 	*/
		
		System.out.println("");
		System.out.println("<Initialize stumper finding strategy>\n(for 175k words takes ~ 45 mins)");
		System.out.println("");
		System.out.println("	java PlayHangman -f dictionary.txt -init:stumpers");
	}


	/**
	 * Factory method to return PlayHangman instances
	 * Regulates number of instances to be under MAX_PH_OBJECTS 
	 * @throws IllegalStateException if maximum amount of instances has been surpassed
	 */
	public static PlayHangman newInstance() throws IllegalStateException
	{
		if(PlayHangman.instances <= MAX_PH_OBJECTS) 
		{
			return new PlayHangman();
		}
		else
		{
			throw new IllegalStateException("Reached max number of " + 
				"Play Hangman instances");
		}
	}
	

	/* Instance methods */

	/**
	 * Constructor.  
	 * Used as part of static factory method newInstance,
	 * in order to regulate how many PlayHangman instances are created 
	 */

	private PlayHangman()
	{
		PlayHangman.instances++;

		HangmanUtil.verbose(false, "!!Play Hangman!!");
	}

	/**
	 * override of object toString() for PlayHangman
	 * @return unique PlayHangman msg
	 */
	public String toString()
	{
		String msg;

		if(game != null) 
		{
			msg = game.toString();
		}
		else
		{
			msg = "Uninitialized PlayHangman object #" 
				+ PlayHangman.instances;
		}

		return msg;
	}

	/**
	 * Initialize single hangman game
	 * The clock2 option displays detailed timing information
	 * @param String secret (the hangman word)
	 */
	public void init(String secret)
	{
		try
		{
			HangmanUtil.clock2("Start PlayHangman init");

			HangmanUtil.verbose(false, "secret: " + secret);

			this.game = new HangmanGame(secret, MAX_WRONG_GUESSES); 

			HangmanUtil.clock2("Instantiated HangmanGame, now " 
				+ "creating strategy");

			//Initialize hangman strategy
			this.strategy = new HangmanStrategy(game);

			HangmanUtil.clock2("Instantiated Strategy");

			HangmanUtil.clock2("End PlayHangman init");
		}
		catch(Exception e)
		{
			e.printStackTrace(System.err);
			HangmanUtil.abort();
		}
	}

	/**
	 * Run single hangman game
	 * @return game score
	 */
	public int run()
	{
		return run(this.game, this.strategy);
	}

	/**
	 * Run hangman game along with provided guessing strategy.
	 * The clock2 option displays detailed timing information 
	 * @param HangmanGame
	 * @param GuessingStrategy
	 * @return game score
	 */
	public int run(HangmanGame game, GuessingStrategy strategy)
	{
		try
		{
			if(game == null && strategy == null)
			{
				throw new IllegalArgumentException("input parameter(s) " + 
					"can't be null");
			}

			HangmanUtil.clock2("Starting new game");

			//Java note: apparently enums are implicitly static
			while(status() == HangmanGame.Status.KEEP_GUESSING)
			{
				HangmanUtil.clock2("Deciding next guess");

				//Ask the strategy for the next guess
				Guess guess = strategy.nextGuess(game);

				HangmanUtil.clock2("Made next guess");
				
				//Apply the next guess to the game
				guess.makeGuess(game);

				HangmanUtil.clock2("Applied guess to game");
				
				HangmanUtil.terse(game.toString());
				HangmanUtil.terse();
				HangmanUtil.terse();
			}

			HangmanUtil.clock2("Finished game");
		}
		catch(Exception e)
		{
			System.err.println("Exception received: " + e);
			return PlayHangman.ERROR_SCORE;
		}

		return game.currentScore();
	}

	/**
	 * @return hangman game status
	 */
	public HangmanGame.Status status()
	{
		return game.gameStatus();
	}

	/**
	 * An explicit destructor for PlayHangman (rather than finalizer),
	 * destructs strategy object
	 * Attempts to clean up object state so gc can intervene
	 */
	public void end()
	{
		strategy.end();
		strategy = null;
		game = null;

		PlayHangman.instances--;
	}
}