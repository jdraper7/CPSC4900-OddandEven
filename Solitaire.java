import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class Solitaire
{
	// CONSTANTS
	public static final int TABLE_HEIGHT = Card.CARD_HEIGHT * 4+150;
	public static final int TABLE_WIDTH = (Card.CARD_WIDTH * 12) + 140;
	public static final int NUM_FINAL_DECKS = 4;
	public static final int NUM_TABLEAU_DECKS = 9;
	public static final int NUM_RESERVE_DECKS = 3;
	public static final Point DECK_POS = new Point(5, 5);
	public static final Point SHOW_POS = new Point(DECK_POS.x + Card.CARD_WIDTH + 5, DECK_POS.y);
	public static final Point FINAL_POS = new Point(SHOW_POS.x + Card.CARD_WIDTH + 265, DECK_POS.y);
	public static final Point PLAY_POS = new Point(DECK_POS.x, FINAL_POS.y + Card.CARD_HEIGHT + 255);

	// GAMEPLAY STRUCTURES
	private static CardStack deck; // populated with standard 52 card deck
	private static CardStack[] tableau; // Tableau stacks
	private static CardStack[] reserve; // Reserve stacks
	private static FinalStack[] foundationA;// FoundationA Stacks
	private static FinalStack[] foundationB;// FoundationB Stacks
	private static WasteStack waste;// waste card spot

	// GUI COMPONENTS (top level)
	private static final JFrame frame = new JFrame("Even and Odd Solitaire");
	protected static final JPanel table = new JPanel();
	// other components
	private static JEditorPane gameTitle = new JEditorPane("text/html", "");
	private static JButton showRulesButton = new JButton("Show Rules");
	private static JButton newGameButton = new JButton("New Game");
	private static JTextField scoreBox = new JTextField();// displays the score
	private static JTextField timeBox = new JTextField();// displays the time
	private static JButton toggleTimerButton = new JButton("Pause Timer");
	private static JTextField statusBox = new JTextField();// status messages
	private static final Card newCardButton = new Card();// reveal waste card

	// TIMER UTILITIES
	private static Timer timer;
	private static ScoreClock scoreClock = new ScoreClock();

	// MISC TRACKING VARIABLES
	private static boolean timeRunning = false;// timer running?
	private static int score = 0;// keep track of the score
	private static int time = 0;// keep track of seconds elapsed

	public static void main(String[] args)
	{

		Container contentPane;

		frame.setSize(TABLE_WIDTH, TABLE_HEIGHT);

		table.setLayout(null);
		table.setBackground(new Color(0, 180, 0));

		contentPane = frame.getContentPane();
		contentPane.add(table);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		playNewGame();

		table.addMouseListener(new CardMovementManager());
		table.addMouseMotionListener(new CardMovementManager());

		frame.setVisible(true);

	}

	private static void playNewGame()
	{
		deck = new CardStack(true); // deal 52 cards
		deck.shuffle();
		table.removeAll();
		// reset stacks if user starts a new game in the middle of one
		if (tableau != null && reserve != null && foundationA != null && foundationB != null)
		{
			for (int x = 0; x < NUM_TABLEAU_DECKS; x++)
			{
				tableau[x].makeEmpty();
			}
			for (int x = 0; x < NUM_RESERVE_DECKS; x++)
			{
				reserve[x].makeEmpty();
			}
			for (int x = 0; x < NUM_FINAL_DECKS; x++)
			{
				foundationA[x].makeEmpty();
			}
			for (int x = 0; x < NUM_FINAL_DECKS; x++)
			{
				foundationB[x].makeEmpty();
			}
		}
		// initialize & place final (foundation) decks/stacks
		foundationA = new FinalStack[NUM_FINAL_DECKS];
		foundationB = new FinalStack[NUM_FINAL_DECKS];
		for (int x = 0; x < NUM_FINAL_DECKS; x++)
		{
			foundationA[x] = new FinalStack();
			foundationA[x].setXY((FINAL_POS.x + (x * Card.CARD_WIDTH)) + 10, FINAL_POS.y);
			table.add(foundationA[x]);

			foundationB[x] = new FinalStack();
			foundationB[x].setXY((FINAL_POS.x + ((x+4) * Card.CARD_WIDTH)) + 40, FINAL_POS.y);
			table.add(foundationB[x]);

		}
		// place new card distribution button
		table.add(moveCard(newCardButton, DECK_POS.x, DECK_POS.y));
		// initialize & place play (tableau) decks/stacks
		reserve = new CardStack[NUM_RESERVE_DECKS];
		for (int x = 0; x < NUM_RESERVE_DECKS; x++)
		{
			reserve[x] = new CardStack(false);
			reserve[x].setXY((DECK_POS.x + (x * (Card.CARD_WIDTH + 10))), PLAY_POS.y);
			table.add(reserve[x]);
		}
		tableau = new CardStack[NUM_TABLEAU_DECKS];
		for (int x = 0; x < NUM_TABLEAU_DECKS; x++)
		{
			tableau[x] = new CardStack(false);
			tableau[x].setXY((DECK_POS.x + ((x+3) * (Card.CARD_WIDTH + 10))), PLAY_POS.y);
			table.add(tableau[x]);
		}
		waste = new WasteStack();
		waste.setXY(SHOW_POS.x, SHOW_POS.y);
		table.add(waste);

		// Dealing new game
		for (int x = 0; x < NUM_RESERVE_DECKS; x++)
		{
			for (int y = 0; y < 5; y++)
			{
				reserve[x].putFirst(deck.pop());
			}
			reserve[x].putFirst(deck.pop().setFaceup());
		}
		for (int x = 0; x < NUM_TABLEAU_DECKS; x++)
		{
			Card c = deck.pop().setFaceup();
			tableau[x].putFirst(c);
		}
		waste.push(deck.pop().setFaceup());

		// reset time
		time = 0;

		newGameButton.addActionListener(new NewGameListener());
		newGameButton.setBounds(0, TABLE_HEIGHT - 70, 120, 30);

		showRulesButton.addActionListener(new ShowRulesListener());
		showRulesButton.setBounds(120, TABLE_HEIGHT - 70, 120, 30);

		scoreBox.setBounds(240, TABLE_HEIGHT - 70, 120, 30);
		scoreBox.setText("Score: 0");
		scoreBox.setEditable(false);
		scoreBox.setOpaque(false);

		timeBox.setBounds(360, TABLE_HEIGHT - 70, 120, 30);
		timeBox.setText("Seconds: 0");
		timeBox.setEditable(false);
		timeBox.setOpaque(false);

		startTimer();

		toggleTimerButton.setBounds(480, TABLE_HEIGHT - 70, 125, 30);
		toggleTimerButton.addActionListener(new ToggleTimerListener());

		statusBox.setBounds(605, TABLE_HEIGHT - 70, 180, 30);
		statusBox.setEditable(false);
		statusBox.setOpaque(false);

		table.add(statusBox);
		table.add(toggleTimerButton);
		table.add(gameTitle);
		table.add(timeBox);
		table.add(newGameButton);
		table.add(showRulesButton);
		table.add(scoreBox);
		table.repaint();
	}

	// moves a card to abs location within a component
	protected static Card moveCard(Card c, int x, int y)
	{
		c.setBounds(new Rectangle(new Point(x, y), new Dimension(Card.CARD_WIDTH + 10, Card.CARD_HEIGHT + 10)));
		c.setXY(new Point(x, y));
		return c;
	}

	// add/subtract points based on gameplay actions
	protected static void setScore(int deltaScore)
	{
		Solitaire.score += deltaScore;
		String newScore = "Score: " + Solitaire.score;
		scoreBox.setText(newScore);
		scoreBox.repaint();
	}

	// GAME TIMER UTILITIES
	protected static void updateTimer()
	{
		Solitaire.time += 1;
		// every 10 seconds elapsed we take away 2 points
		if (Solitaire.time % 10 == 0)
		{
			setScore(-2);
		}
		String time = "Seconds: " + Solitaire.time;
		timeBox.setText(time);
		timeBox.repaint();
	}

	protected static void startTimer()
	{
		scoreClock = new ScoreClock();
		// set the timer to update every second
		timer = new Timer();
		timer.scheduleAtFixedRate(scoreClock, 1000, 1000);
		timeRunning = true;
	}

	// the pause timer button uses this
	protected static void toggleTimer()
	{
		if (timeRunning && scoreClock != null)
		{
			scoreClock.cancel();
			timeRunning = false;
		} else
		{
			startTimer();
		}
	}

	private static class ScoreClock extends TimerTask
	{
		@Override
		public void run()
		{
			updateTimer();
		}
	}

	// BUTTON LISTENERS
	private static class NewGameListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			timer.cancel();
			playNewGame();
		}
	}

	private static class ShowRulesListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JDialog ruleFrame = new JDialog(frame, true);
			ruleFrame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			ruleFrame.setSize(800, 600);
			JScrollPane scroll;
			JEditorPane rulesTextPane = new JEditorPane("text/html", "");
			rulesTextPane.setEditable(false);
			String rulesText = "<b>Even and Odd Solitaire Rules</b>"
					+ "<br><br> 1 deck. Easy. No redeal."
					+ "<br><br><b>Even and Odd Solitaire </b>uses one deck (52 cards). You have 9 tableau piles with one card in each pile and 3 reserve piles (with 6 cards in each pile). You also have 8 foundation piles."
					+ "<br><br><b>The object of the game</b>"
					+ "<br><ul><li>To build up the left four foundations in ascending sequence regardless of suit by twos starting with Ace (Ace,3,5,7,9,J,K) and</li>"
					+ "<li>To build up the right four foundations in ascending sequence regardless of suit by twos starting with 2 (2,4,6,8,10,Q)</li></ul>"
					+ "<b>The rules</b>"
					+ "<br>Each tableau pile may contain only one card. All cards in tableaus, top cards of reserve, stock and waste piles are available to play. Spaces in tableaus are filled from waste or stock piles. Empty reserve piles cannot be filled."
					+ "<br><br>When you have made all the moves initially available, begin turning over cards from the stock pile."
					+ "<br><br>There is no redeal.";
			rulesTextPane.setText(rulesText);
			ruleFrame.add(scroll = new JScrollPane(rulesTextPane));

			ruleFrame.setVisible(true);
		}
	}

	private static class ToggleTimerListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			toggleTimer();
			if (!timeRunning)
			{
				toggleTimerButton.setText("Start Timer");
			} else
			{
				toggleTimerButton.setText("Pause Timer");
			}
		}
	}

	/*
	 * This class handles all of the logic of moving the Card components as well
	 * as the game logic. This determines where Cards can be moved according to
	 * the rules of Even and Odd Solitaire
	 */
	private static class CardMovementManager extends MouseAdapter
	{
		private boolean checkForWin = false;// should we check if game is over?
		private boolean gameOver = true;// easier to negate this than affirm it
		private Point start = null;// where mouse was clicked
		private Card card = null; // card to be moved
		private CardStack source = null;
		private boolean sourceInTableau = false;
		private CardStack dest = null;

		private boolean validFinalStackMove(Card source, Card dest)
		{
			int s_val = source.getValue().ordinal();
			int d_val = dest.getValue().ordinal();
			if (s_val == (d_val + 2)) // destination must be two lower
				return true;
			else
				return false;
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			start = e.getPoint();
			boolean stopSearch = false;
			statusBox.setText("");

			for (int x = 0; x < (NUM_RESERVE_DECKS); x++)
			{
				if (stopSearch)
					break;
				source = reserve[x];
				// pinpointing exact card pressed
				for (Component ca : source.getComponents())
				{
					Card c = (Card) ca;
					if (c.contains(start) && source.contains(start) && c.getFaceStatus())
					{
						card = c;
						stopSearch = true;
						break;
					}
				}
			}
			if (card == null) 
			{
				for (int x = 0; x < (NUM_TABLEAU_DECKS); x++)
				{
					if (stopSearch)
						break;
					source = tableau[x];
					// pinpointing exact card pressed
					for (Component ca : source.getComponents())
					{
						Card c = (Card) ca;
						if (c.contains(start) && source.contains(start) && c.getFaceStatus())
						{
							card = c;
							stopSearch = true;
							sourceInTableau = true;
							break;
						}
					}
				}
			}
			if (card == null && waste.contains(start)) 
			{
				card = waste.pop();
			}

			// SHOW (WASTE) CARD OPERATIONS
			// display new show card
			if (newCardButton.contains(start) && deck.showSize() > 0)
			{
				Card c = deck.pop().setFaceup();
				waste.push(c);
				c.repaint();
				table.repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			// used for status bar updates
			boolean validMoveMade = false;
			
			// SHOW CARD MOVEMENTS
			if (waste.contains(start) && !newCardButton.contains(start))
			{
				// Moving from SHOW TO FINAL
				if (card.getValue().ordinal()%2==0) 
				{
					for (int x = 0; x < NUM_FINAL_DECKS; x++)
					{
						dest = foundationA[x];
						// only aces can go first
						if (dest.empty())
						{
							if (card.getValue() == Card.Value.ACE)
							{
								dest.push(card);
								dest.repaint();
								table.repaint();
								card = null;
								setScore(10);
								validMoveMade = true;
								break;
							}
						}
						else if (validFinalStackMove(card, dest.getLast()))
						{
							dest.push(card);
							dest.repaint();
							table.repaint();
							card = null;
							checkForWin = true;
							setScore(10);
							validMoveMade = true;
							break;
						}
					}
				}
				else 
				{
					for (int x = 0; x < NUM_FINAL_DECKS; x++)
					{
						dest = foundationB[x];
						// only twos can go first
						if (dest.empty())
						{
							if (card.getValue() == Card.Value.TWO)
							{
								dest.push(card);
								dest.repaint();
								table.repaint();
								card = null;
								setScore(10);
								validMoveMade = true;
								break;
							}
						}
						else if (validFinalStackMove(card, dest.getLast()))
						{
							dest.push(card);
							dest.repaint();
							table.repaint();
							card = null;
							checkForWin = true;
							setScore(10);
							validMoveMade = true;
							break;
						}
					}
				}
			}

			if (card != null && source != null && card.getFaceStatus()) 
			{
				if (card.getValue().ordinal()%2==0) 
				{
					for (int x = 0; x < NUM_FINAL_DECKS; x++)
					{
						dest = foundationA[x];

						// TO EMPTY STACK
						if (dest.empty())// empty final should only take an ACE
						{
							if (card.getValue() == Card.Value.ACE)
							{
								Card c = source.popFirst();
								c.repaint();
								if (source.getFirst() != null)
								{
									Card temp = source.getFirst().setFaceup();
									temp.repaint();
									source.repaint();
								}
								dest.setXY(dest.getXY().x, dest.getXY().y);
								dest.push(c);
								dest.repaint();
								table.repaint();
								dest.showSize();
								card = null;
								setScore(10);
								validMoveMade = true;
								break;
							}// TO POPULATED STACK
						} 
						else if (validFinalStackMove(card, dest.getLast()))
						{
							Card c = source.popFirst();
							c.repaint();
							if (source.getFirst() != null)
							{
								Card temp = source.getFirst().setFaceup();
								temp.repaint();
								source.repaint();
							}
							dest.setXY(dest.getXY().x, dest.getXY().y);
							dest.push(c);
							dest.repaint();
							table.repaint();
							dest.showSize();
							card = null;
							checkForWin = true;
							setScore(10);
							validMoveMade = true;
							break;
						}
					}
				}
				else
				{
					for (int x = 0; x < NUM_FINAL_DECKS; x++)
					{
						dest = foundationB[x];

						// TO EMPTY STACK
						if (dest.empty())// empty final should only take an ACE
						{
							if (card.getValue() == Card.Value.TWO)
							{
								Card c = source.popFirst();
								c.repaint();
								if (source.getFirst() != null)
								{
									Card temp = source.getFirst().setFaceup();
									temp.repaint();
									source.repaint();
								}
								dest.setXY(dest.getXY().x, dest.getXY().y);
								dest.push(c);
								dest.repaint();
								table.repaint();
								dest.showSize();
								card = null;
								setScore(10);
								validMoveMade = true;
								break;
							}// TO POPULATED STACK
						} 
						else if (validFinalStackMove(card, dest.getLast()))
						{
							Card c = source.popFirst();
							c.repaint();
							if (source.getFirst() != null)
							{
								Card temp = source.getFirst().setFaceup();
								temp.repaint();
								source.repaint();
							}
							dest.setXY(dest.getXY().x, dest.getXY().y);
							dest.push(c);
							dest.repaint();
							table.repaint();
							dest.showSize();
							card = null;
							checkForWin = true;
							setScore(10);
							validMoveMade = true;
							break;
						}
					}
				}
				if (sourceInTableau && validMoveMade) 
				{
					if (!waste.empty()) {
						source.push(waste.pop().setFaceup());
					}
					else if (!deck.empty()) {
						source.push(deck.pop().setFaceup());
					}
				}
			}

			// SHOWING STATUS MESSAGE IF MOVE INVALID
			if (!validMoveMade && dest != null && card != null)
			{
				statusBox.setText("That Is Not A Valid Move");
			}

			// CHECKING FOR WIN
			if (checkForWin)
			{
				boolean gameNotOver = false;
				// cycle through final decks, if they're all full then game over
				for (int x = 0; x < NUM_FINAL_DECKS; x++)
				{
					dest = foundationA[x];
					if (dest.showSize() != 7)
					{
						// one deck is not full, so game is not over
						gameNotOver = true;
						break;
					}
				}
				for (int x = 0; x < NUM_FINAL_DECKS; x++)
				{
					dest = foundationB[x];
					if (dest.showSize() != 6)
					{
						// one deck is not full, so game is not over
						gameNotOver = true;
						break;
					}
				}
				if (!gameNotOver)
					gameOver = true;
			}

			if (checkForWin && gameOver)
			{
				JOptionPane.showMessageDialog(table, "Congratulations! You've Won!");
				statusBox.setText("Game Over!");
			}

			if (waste.empty() && !deck.empty()) 
			{
				waste.push(deck.pop().setFaceup());
			}

			// RESET VARIABLES FOR NEXT EVENT
			start = null;
			source = null;
			dest = null;
			card = null;
			checkForWin = false;
			gameOver = false;
			sourceInTableau = false;
		}
	}
}