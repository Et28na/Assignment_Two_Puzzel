//SudokuGame.java
// Sudoku
// Controls
//  Click a cell to select it
//  To input a number, type 1–9; to clear, type 0 / BACKSPACE / DELETE.
//  Press R to reset
//  Press H for hint (highlights an empty cell that can be solved)
//  Press ESC to clear
//  Grid flashes green when solved correctly

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class SudokuGame extends GameEngine {

    // Game Configuration Constants
    private static final int SUDOKU_GRID_SIZE = 9; // The total size of the grid
    private static final int SUBGRID_SIZE = 3; // One of the sections out of the nine grids
    private static final int CELL_SIZE_PIXELS = 50; // This makes each cell(box)-50 pixels tall and wide
    private static final int BOARD_PADDING = 50; // Adds  pixels from the board to the window (Might change would be good to get feedback)
    private static final int WINDOW_SIZE = CELL_SIZE_PIXELS * SUDOKU_GRID_SIZE + BOARD_PADDING * 2;
    // just sum to find the total window size (board padding times 2 as both sides of the board)

    private static final int GRID_LINE_THIN = 1; // for the inner grid
    private static final int GRID_LINE_THICK = 3; // the outer grid
    private static final int CELL_BORDER_OFFSET = 1; // To make sure when highlighting i.e hints or selection doesn't cover the lines
    private static final int TEXT_VERTICAL_OFFSET = 15; // Moves text down from the cell center.
    private static final int SOLVED_FLASH_INTERVAL = 300; // little flash when solve for seconds thinks it looks cool

    // Game State Variables
    private final int[][] currentBoard = new int[SUDOKU_GRID_SIZE][SUDOKU_GRID_SIZE];
    private final boolean[][] isOriginalClue = new boolean[SUDOKU_GRID_SIZE][SUDOKU_GRID_SIZE];
    private int selectedRow = -1;
    private int selectedColumn = -1;
    private boolean isPuzzleSolved = false;
    private boolean victoryPlayed = false; // Track victory sound has been played
    private int hintRow = -1;
    private int hintColumn = -1;
    private long hintStartTime = 0;
    private static final long HINT_DISPLAY_DURATION = 2000; // 2 seconds
    private enum GameState { MENU, PLAYING, HELP, QUIT }
    private GameState currentState = GameState.MENU;

    // Timer Variables
    private long startTime = 0;
    private long timeRemaining = 300000; // 5 minutes in milliseconds
    private static final long INITIAL_TIME = 300000; // 5 minutes
    private static final long CORRECT_BONUS = 15000; // +15 seconds for correct
    private static final long WRONG_PENALTY = 5000; // -5 seconds for wrong
    private boolean gameOver = false;

    // Color Scheme
    private final Color SELECTION_HIGHLIGHT = new Color(255, 255, 200); // Pale yellow
    private final Color SOLVED_FLASH_GREEN = new Color(200, 255, 200);
    private final Color HINT_HIGHLIGHT = new Color(255, 200, 255); // Light purple
    private final Color ERROR_RED = new Color(220, 20, 20);
    private final Color USER_INPUT_BLUE = new Color(20, 20, 180);

    //Audio
    private static AudioClip backGroundMusic;
    private static AudioClip selectSound;
    private static AudioClip errorSound;
    private static AudioClip victorySound;
    private static boolean musicPlaying = false;

    // Background Image
    private Image backgroundImage;
    private Image backgroundMenuImage;

    // Pen cursor
    private Image penCursor;
    private int mouseX = 0, mouseY = 0;
    private boolean penClickAnimating = false;
    private long penClickStartTime = 0;
    private final long PEN_ANIM_DURATION = 150; // in ms


    public static void main(String[] args) {
        GameEngine.createGame(new SudokuGame(), 60); // framerate
    }

    public SudokuGame() {
        super(WINDOW_SIZE, WINDOW_SIZE);
    }

    @Override
    public void init() {
        currentState = GameState.MENU;
        penCursor = loadImage("pen.png");
        if (backgroundMenuImage == null) {
            backgroundMenuImage = loadImage("backgroundMenuImage.png");
            if (backGroundMusic == null) {
                backGroundMusic = loadAudio("backroundMusic.wav");
            }if (!musicPlaying && backGroundMusic != null) {
                startAudioLoop(backGroundMusic, -12f);
                musicPlaying = true;
            }
        }

    }

    private void loadInitPuzzle() { // probably going to make a few games
        int[][] initialPuzzle = {
                // Starter puzzle 0 = empty
                {5,3,0, 0,7,0, 0,0,0}, //T - top row
                {6,0,0, 1,9,5, 0,0,0}, // M - middle row
                {0,9,8, 0,0,0, 0,6,0}, // B - Bottom row

                {8,0,0, 0,6,0, 0,0,3}, //T - top row
                {4,0,0, 8,0,3, 0,0,1}, // M - middle row
                {7,0,0, 0,2,0, 0,0,6}, // B - Bottom row

                {0,6,0, 0,0,0, 2,8,0}, //T - top row
                {0,0,0, 4,1,9, 0,0,5}, // M - middle row
                {0,0,0, 0,8,0, 0,7,9} // B - Bottom row
        };

        for (int row = 0; row < SUDOKU_GRID_SIZE; row++) {
            for (int col = 0; col < SUDOKU_GRID_SIZE; col++) {
                currentBoard[row][col] = initialPuzzle[row][col];
                isOriginalClue[row][col] = initialPuzzle[row][col] != 0;
            }
        }

        // Load background image
        if (backgroundImage == null) {
            backgroundImage = loadImage("background.png");
        }

        // Load audio clips once
        if (selectSound  == null) selectSound  = loadAudio("selectSound.wav");
        if (errorSound   == null) errorSound   = loadAudio("errorSound.wav");
        if (victorySound == null) victorySound = loadAudio("VictorySound.wav");

        if (backGroundMusic == null) {
            backGroundMusic = loadAudio("backroundMusic.wav");
        }if (!musicPlaying && backGroundMusic != null) {
            startAudioLoop(backGroundMusic, -12f);
            musicPlaying = true;
        }
    }

    private void resetGameState() {
        selectedRow = selectedColumn = -1;
        isPuzzleSolved = false;
        victoryPlayed = false; // Reset victory sound flag
        clearHint();
        startTime = getTime(); // Reset timer
        timeRemaining = INITIAL_TIME; // Reset to 5 minutes
        gameOver = false;
    }

    @Override
    public void update(double deltaTime) {
        updateSolvedFlashEffect();
        updateHintDisplay();

        // Update countdown timer only if game is still active
        if (!isPuzzleSolved && !gameOver) {
            long elapsed = getTime() - startTime;
            long newTimeRemaining = INITIAL_TIME - elapsed;

            // Check if time has run out
            if (newTimeRemaining <= 0) {
                timeRemaining = 0;
                gameOver = true;
            } else {
                timeRemaining = newTimeRemaining;
            }
        }
    }

    private void updateSolvedFlashEffect() {
        if (isPuzzleSolved) {
            boolean shouldFlashGreen = (getTime() / SOLVED_FLASH_INTERVAL) % 2 == 0;
            if (shouldFlashGreen) {
                changeBackgroundColor(SOLVED_FLASH_GREEN);
            } else {
                changeBackgroundColor(white);
            }
        }
    }

    private void updateHintDisplay() {
        if (hintRow != -1 && hintColumn != -1) {
            if (getTime() - hintStartTime > HINT_DISPLAY_DURATION) {
                clearHint();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void paintComponent() {
        clearBackground(width(), height());



        if (currentState == GameState.MENU) {
            drawMenu();
        } else if (currentState == GameState.HELP) {
            drawHelp();
        } else if (currentState == GameState.PLAYING) {
            drawGame();
        }

        if (penCursor != null) {
            int offsetY = 0;

            if (penClickAnimating) {
                long elapsed = getTime() - penClickStartTime;
                if (elapsed > PEN_ANIM_DURATION) {
                    penClickAnimating = false;
                } else {
                    offsetY = 4; // small downward shift on click
                }
            }

            drawImage(penCursor, mouseX - 16, mouseY - 16 + offsetY, 32, 32);
        }
    }

    private void drawGame() {
        // Draw background
        if (backgroundImage != null) {
            drawImage(backgroundImage, 0, 0, width(), height());
        }

        int boardOriginX = BOARD_PADDING;
        int boardOriginY = BOARD_PADDING;

        drawGridLines(boardOriginX, boardOriginY);
        drawCellHighlights(boardOriginX, boardOriginY);
        drawAllDigits(boardOriginX, boardOriginY);
        drawGameStatus();
    }

    private void drawMenu() {

        if (backgroundMenuImage != null) {
            drawImage(backgroundMenuImage, 0, 0, width(), height());
        }
        changeColor(black);
        drawBoldText(WINDOW_SIZE / 2 - 125, 150, "Sudoku Game", "Arial", 40);

        drawMenuOption("1. Play", 200);
        drawMenuOption("2. Help", 260);
        drawMenuOption("3. Quit", 320);
    }

    private void drawMenuOption(String text, int yPosition) {
        drawBoldText(WINDOW_SIZE / 2 - 50, yPosition, text, "Arial", 24);
    }

    private void drawHelp() {
        if (backgroundMenuImage != null) {
            drawImage(backgroundMenuImage, 0, 0, width(), height());
        }
        changeColor(Color.WHITE);
        drawBoldText(50, 100, "HELP", "Arial", 36);
        drawText(50, 150, "Click a cell to select it", "Arial", 20);
        drawText(50, 180, "Type 1–9 to input numbers", "Arial", 20);
        drawText(50, 210, "Type 0 / BACKSPACE / DELETE to clear", "Arial", 20);
        drawText(50, 240, "Press R to reset, H for hint (highlights solvable cell)", "Arial", 20);
        drawText(50, 270, "Press ESC to clear selection", "Arial", 20);
        drawText(50, 300, "Press M to toggle music", "Arial", 20);
        drawText(50, 330, "Grid will flash green when solved correctly", "Arial", 20);
        drawText(50, 380, "Press B to go back to menu", "Arial", 20);
    }

    private void drawGridLines(int originX, int originY) {
        changeColor(black);

        for (int lineIndex = 0; lineIndex <= SUDOKU_GRID_SIZE; lineIndex++) {
            int pixelPosition = lineIndex * CELL_SIZE_PIXELS;
            int lineThickness = (lineIndex % SUBGRID_SIZE == 0) ? GRID_LINE_THICK : GRID_LINE_THIN;

            // Vertical lines
            drawLine(originX + pixelPosition, originY,
                    originX + pixelPosition, originY + SUDOKU_GRID_SIZE * CELL_SIZE_PIXELS,
                    lineThickness);

            // Horizontal lines
            drawLine(originX, originY + pixelPosition,
                    originX + SUDOKU_GRID_SIZE * CELL_SIZE_PIXELS, originY + pixelPosition,
                    lineThickness);
        }
    }

    private void drawCellHighlights(int originX, int originY) {
        // Draw selection highlight
        if (isCellSelected()) {
            changeColor(SELECTION_HIGHLIGHT);
            drawCellHighlight(originX, originY, selectedRow, selectedColumn);
        }

        // Draw hint highlight
        if (isHintActive()) {
            changeColor(HINT_HIGHLIGHT);
            drawCellHighlight(originX, originY, hintRow, hintColumn);
        }
    }

    private void drawCellHighlight(int originX, int originY, int row, int col) {
        drawSolidRectangle(
                originX + col * CELL_SIZE_PIXELS + CELL_BORDER_OFFSET,
                originY + row * CELL_SIZE_PIXELS + CELL_BORDER_OFFSET,
                CELL_SIZE_PIXELS - 2 * CELL_BORDER_OFFSET,
                CELL_SIZE_PIXELS - 2 * CELL_BORDER_OFFSET
        );
    }

    private void drawAllDigits(int originX, int originY) {
        for (int row = 0; row < SUDOKU_GRID_SIZE; row++) {
            for (int col = 0; col < SUDOKU_GRID_SIZE; col++) {
                int cellValue = currentBoard[row][col];
                if (cellValue != 0) {
                    drawSingleDigit(originX, originY, row, col, cellValue);
                }
            }
        }
    }

    private void drawSingleDigit(int originX, int originY, int row, int col, int value) {
        int centerX = originX + col * CELL_SIZE_PIXELS + CELL_SIZE_PIXELS / 2;
        int centerY = originY + row * CELL_SIZE_PIXELS + CELL_SIZE_PIXELS / 2 + TEXT_VERTICAL_OFFSET;

        boolean isConflicted = hasRuleViolation(row, col, value);

        if (isOriginalClue[row][col]) {
            changeColor(black);
            drawBoldText(centerX - 10, centerY, String.valueOf(value), "Arial", 30);
        } else {
            changeColor(isConflicted ? ERROR_RED : USER_INPUT_BLUE);
            drawText(centerX - 8, centerY, String.valueOf(value), "Arial", 30);
        }
    }

    private void drawGameStatus() {
        // Draw timer
        long seconds = timeRemaining / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        // Change color based on time remaining
        if (timeRemaining < 60000) { // Less than 1 minute
            changeColor(red);
        } else if (timeRemaining < 120000) { // Less than 2 minutes
            changeColor(orange);
        } else {
            changeColor(black);
        }

        drawBoldText(BOARD_PADDING, 30, String.format("Time: %02d:%02d", minutes, seconds), "Arial", 20);

        // Draw game status messages
        if (isPuzzleSolved) {
            changeColor(green);
            drawBoldText(BOARD_PADDING, WINDOW_SIZE - 10, "Puzzle Solved!", "Arial", 30);
            changeColor(blue);
            long finalTime = (INITIAL_TIME - timeRemaining) / 1000;
            drawText(BOARD_PADDING + 250, WINDOW_SIZE - 10, "Completed in: " + (finalTime / 60) + "m " + (finalTime % 60) + "s", "Arial", 20);
        } else if (gameOver) {
            changeColor(red);
            drawBoldText(BOARD_PADDING, WINDOW_SIZE - 10, "Time's Up! Press R to restart", "Arial", 30);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        int clickX = mouseEvent.getX();
        int clickY = mouseEvent.getY();

        int clickedRow = getRowFromPixel(clickY);
        int clickedColumn = getColumnFromPixel(clickX);

        if (isValidGridPosition(clickedRow, clickedColumn)) {
            selectCell(clickedRow, clickedColumn);
            clearHint();
            penClickAnimating = true;
            penClickStartTime = getTime();
        }
    }

    private int getRowFromPixel(int pixelY) {
        return (pixelY - BOARD_PADDING) / CELL_SIZE_PIXELS;
    }

    private int getColumnFromPixel(int pixelX) {
        return (pixelX - BOARD_PADDING) / CELL_SIZE_PIXELS;
    }

    private boolean isValidGridPosition(int row, int col) {
        return row >= 0 && row < SUDOKU_GRID_SIZE && col >= 0 && col < SUDOKU_GRID_SIZE;
    }

    private void selectCell(int row, int col) {
        if (!isOriginalClue[row][col]) {
            selectedRow = row;
            selectedColumn = col;
        } else {
            clearSelection();
        }
    }

    private void clearSelection() {
        selectedRow = selectedColumn = -1;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (handleSpecialKeys(keyCode)) {
            return;
        }

        if (currentState == GameState.MENU) {
            if (keyCode == KeyEvent.VK_1) {
                currentState = GameState.PLAYING;
                loadInitPuzzle();
                resetGameState();
                startTime = getTime();
            } else if (keyCode == KeyEvent.VK_2) {
                currentState = GameState.HELP;
            } else if (keyCode == KeyEvent.VK_3 || keyCode == KeyEvent.VK_Q) {
                System.exit(0);
            }
            return;
        }

        if (currentState == GameState.HELP) {
            if (keyCode == KeyEvent.VK_B) {
                currentState = GameState.MENU;
            }
            return;
        }

        if (currentState != GameState.PLAYING) return;

        if (isCellSelected() && !gameOver) {
            int digitValue = mapKeyToDigit(keyCode);
            if (digitValue >= 0) {
                placeCellValue(digitValue);
            }
            else if (keyCode == KeyEvent.VK_M) {        // press M to toggle music
                if (musicPlaying) {
                    stopAudioLoop(backGroundMusic);
                } else {
                    startAudioLoop(backGroundMusic, -12f);
                }
                musicPlaying = !musicPlaying;
            }
        }
    }

    private boolean handleSpecialKeys(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_R:
                resetPuzzle();
                return true;
            case KeyEvent.VK_H:
                showHint();
                return true;
            case KeyEvent.VK_ESCAPE:
                clearSelection();
                clearHint();
                return true;
            default:
                return false;
        }
    }

    private int mapKeyToDigit(int keyCode) {
        if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
            return keyCode - KeyEvent.VK_0;
        } else if (keyCode >= KeyEvent.VK_NUMPAD1 && keyCode <= KeyEvent.VK_NUMPAD9) {
            return keyCode - KeyEvent.VK_NUMPAD0;
        } else if (keyCode == KeyEvent.VK_0 || keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE) {
            return 0;
        }
        return -1; // Invalid
    }

    private void placeCellValue(int value) {
        // Don't allow moves if game is over
        if (gameOver) {
            return;
        }

        int previousValue = currentBoard[selectedRow][selectedColumn];
        currentBoard[selectedRow][selectedColumn] = value;

        // Check if the new value violates rules
        if (value != 0 && hasRuleViolation(selectedRow, selectedColumn, value)) {
            // Play error sound for illegal move
            if (errorSound != null) {
                playAudio(errorSound);
            }
            // Subtract 5 seconds for wrong answer
            startTime = startTime - WRONG_PENALTY;
        } else if (value != 0 && previousValue == 0) {
            // Play select sound for valid placement
            if (selectSound != null) {
                playAudio(selectSound);
            }
            // Add 15 seconds for correct answer decrease start time to increase remaining time
            startTime = startTime + CORRECT_BONUS;
        } else if (value == 0) {
            // Just play sound for clearing, no time bonus/penalty
            if (selectSound != null) {
                playAudio(selectSound);
            }
        }

        // Check if puzzle is now solved
        boolean wasNotSolved = !isPuzzleSolved;
        isPuzzleSolved = checkIfPuzzleIsSolved();

        // Play victory sound if puzzle was just solved
        if (isPuzzleSolved && wasNotSolved && !victoryPlayed) {
            if (victorySound != null) {
                playAudio(victorySound);
                victoryPlayed = true;
            }
        }

        clearHint();
    }

    private void resetPuzzle() {
        init();
    }

    private void showHint() {// my addition searches all cells and find one that can defiantly be solved
        for (int row = 0; row < SUDOKU_GRID_SIZE; row++) {
            for (int col = 0; col < SUDOKU_GRID_SIZE; col++) {
                if (currentBoard[row][col] == 0 && canSolveCell(row, col)) {
                    hintRow = row;
                    hintColumn = col;
                    hintStartTime = getTime();
                    return;
                }
            }
        }
    }

    private boolean canSolveCell(int row, int col) {//Returns true only if exactly ONE number works
        int possibleValues = 0;
        for (int value = 1; value <= 9; value++) {
            if (!hasRuleViolation(row, col, value)) {
                possibleValues++;
                if (possibleValues > 1) {
                    return false; // More than one possibility
                }
            }
        }
        return possibleValues == 1;
    }

    private void clearHint() {
        hintRow = hintColumn = -1;
    }

    // Check if placing value at (row,col) violates Sudoku rules
    private boolean hasRuleViolation(int row, int col, int value) {
        if (value == 0) return false;

        return hasRowConflict(row, col, value) ||
                hasColumnConflict(row, col, value) ||
                hasSubgridConflict(row, col, value);
    }

    private boolean hasRowConflict(int row, int excludeCol, int value) {//excludeCol is the column  testing
        for (int col = 0; col < SUDOKU_GRID_SIZE; col++) {
            if (col != excludeCol && currentBoard[row][col] == value) {
                return true;
            }
        }
        return false;
    }

    private boolean hasColumnConflict(int excludeRow, int col, int value) {//checks the value and see if it exists already
        for (int row = 0; row < SUDOKU_GRID_SIZE; row++) {
            if (row != excludeRow && currentBoard[row][col] == value) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSubgridConflict(int row, int col, int value) {// finds the subgrid then contains the cell
        int subgridStartRow = (row / SUBGRID_SIZE) * SUBGRID_SIZE;
        int subgridStartCol = (col / SUBGRID_SIZE) * SUBGRID_SIZE;

        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                int checkRow = subgridStartRow + i;
                int checkCol = subgridStartCol + j;

                if ((checkRow != row || checkCol != col) &&
                        currentBoard[checkRow][checkCol] == value) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkIfPuzzleIsSolved() { // just checks all cells
        for (int row = 0; row < SUDOKU_GRID_SIZE; row++) {
            for (int col = 0; col < SUDOKU_GRID_SIZE; col++) {
                int cellValue = currentBoard[row][col];
                if (cellValue == 0 || hasRuleViolation(row, col, cellValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isCellSelected() {
        return selectedRow != -1 && selectedColumn != -1;
    }

    private boolean isHintActive() {
        return hintRow != -1 && hintColumn != -1;
    }
}