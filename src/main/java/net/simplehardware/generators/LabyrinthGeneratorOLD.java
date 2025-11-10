package net.simplehardware.generators;

import net.simplehardware.models.CellButton;
import net.simplehardware.MazeGrid;
import net.simplehardware.models.Mode;

import java.util.*;

public class LabyrinthGeneratorOLD {

    private static final Random RNG = new Random();

    public static void generateMaze(MazeGrid grid) {
        int n = grid.getGridSize();
        if (n < 5) return;

        CellButton[][] cells = grid.getCells();

        // --- Step 1: generate structural maze ---
        generateRecursiveBacktrackerMaze(cells);
    }

    // ------------------------------------------------
    // 1. Recursive Backtracking Maze Generator
    // ------------------------------------------------
    private static void generateRecursiveBacktrackerMaze(CellButton[][] cells) {
        int n = cells.length;
        // Fill with walls
        for (int y = 0; y < n; y++)
            for (CellButton[] cell : cells) cell[y].setMode(Mode.WALL, 0);

        boolean[][] visited = new boolean[n][n];
        int startX = (RNG.nextInt(n / 2)) * 2 + 1;
        int startY = (RNG.nextInt(n / 2)) * 2 + 1;

        dfsMaze(cells, visited, startX, startY);
    }

    private static void dfsMaze(CellButton[][] cells, boolean[][] visited, int x, int y) {
        int n = cells.length;
        visited[x][y] = true;
        cells[x][y].setMode(Mode.FLOOR, 0);

        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};
        Collections.shuffle(Arrays.asList(dirs), RNG);

        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx > 0 && ny > 0 && nx < n - 1 && ny < n - 1 && !visited[nx][ny]) {
                // carve wall between (x,y) and (nx,ny)
                cells[x + d[0] / 2][y + d[1] / 2].setMode(Mode.FLOOR, 0);
                dfsMaze(cells, visited, nx, ny);
            }
        }
    }

}
